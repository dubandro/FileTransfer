import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.LinkedList;

public class Client implements Runnable{
    private final String DIR = "/Users/andrejdubovik/Desktop/Client/";
//    private static final String DIR = "../FileTransferIO/ClientIO/src/main/resources/";
    private Socket socket;
    private ObjectInputStream inputObject;
    private ObjectOutputStream outputObject;
    private OutputStream outputFile;
    private boolean receiveReady = true;
    private boolean sendingReady = true;
    private final LinkedList<String> receiveList;
    private final LinkedList<String> sendingList;
    private File rFile;
    private File sFile;
    private int receiveNum;
    private long totalWrite;
    private final String host;
    private final int port;

    public Socket getSocket() {
        return socket;
    }

    public Client(String host, int port) {
        receiveList = new LinkedList<>();
        sendingList = new LinkedList<>();
        this.host = host;
        this.port = port;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(host, port);
            outputObject = new ObjectOutputStream(socket.getOutputStream());
            inputObject = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (!socket.isClosed()) {
            try {
                ServiceMessage msg = (ServiceMessage) inputObject.readObject();
                switch (msg.getMessageType()) {
                    case SENDING_ERROR: {
                        System.out.println("Error sending file " + msg.getFileName());
                        receiveReady = true;
                        break;
                    }
                    case RECEIVE_FILE: // получение файла с сервера и запись на клиенте
                    {
                        receiveFile(msg);
                        break;
                    }
                    case SENDING_FILE: // чтение файла на клиенте и передача серверу
                    {
                        new Thread(this::sendingFile).start();
                        break;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Socket closed");
            }
        }
    }

    public void fileTransfer() throws IOException {
        // если свободен и очередь не пустая делаем запрос
        if (receiveReady && !receiveList.isEmpty()) receiveReady(receiveList.getFirst());
        if (sendingReady && !sendingList.isEmpty()) sendingReady(sendingList.getFirst());
    }

    public void sending(String fileName) {
        sendingList.addLast(fileName);
    }
    public void receive(String fileName) {
        receiveList.addLast(fileName);
    }

    private void request(MessageType messageType, String fileName) {
        ServiceMessage msg = new ServiceMessage(messageType, fileName);
        try {
            outputObject.writeObject(msg);
            outputObject.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void receiveReady(String fileName) throws IOException {
        rFile = new File(DIR, fileName);
        if (rFile.exists()) System.out.println("File will be rewriting");
        outputFile = Files.newOutputStream(rFile.toPath());
        receiveNum = 0;
        totalWrite = 0;

        request(MessageType.RECEIVE_REQUEST, fileName);
        receiveReady = false;
        receiveList.removeFirst();
    }
    private void sendingReady(String fileName) {
        sFile = new File(DIR, fileName);
        if (!sFile.exists()) System.out.println("File not found!");

        if (sFile.exists()) {
            sendingReady = false;
            request(MessageType.SENDING_REQUEST, fileName);
        }
        sendingList.removeFirst();
    }

    public void receiveFile(ServiceMessage msg) throws IOException, ClassNotFoundException {
        if (receiveNum == msg.getCurrentPart()) {
            try {
                outputFile.write(msg.getBody());
                outputFile.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            receiveNum++;
            totalWrite += msg.getBody().length;
        }
        else {
            // если не равны номера части которая должна быть и которая пришла - дать команду вернуться к предыдущей
        }
        if (totalWrite == msg.getFileSize()) {
            System.out.println("File transfer from server completed, transferred " + totalWrite + " bytes");
            receiveReady = true;
        }
    }

    public void sendingFile() {
        try (DataInputStream readData = new DataInputStream(Files.newInputStream(sFile.toPath()))) {
            long splitSize = splitSize();
            long fileSize = sFile.length();
            int totalParts = (int) Math.ceil(fileSize / splitSize);
            long remainBytes = fileSize % splitSize;
            long currentLength;
            int currentRead;
            long totalRead = 0;
            int currentPart = 0;
            do {
                currentLength = currentPart < totalParts ? splitSize : remainBytes;
                byte[] arrayData = new byte[(int) currentLength];
                currentRead = readData.read(arrayData, 0, (int) currentLength);
                if (currentRead != -1) {
                    ServiceMessage msg = new ServiceMessage(MessageType.RECEIVE_FILE, fileSize, currentPart, arrayData);
                    outputObject.writeObject(msg);
                    outputObject.flush();
                    totalRead += currentRead;
                    currentPart++;
                }
            } while (currentPart <= totalParts);
            if (totalRead == fileSize) {
                System.out.println("File transfer to server completed, transferred " + totalRead + " bytes");
                sendingReady = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private long splitSize() {
        return 8 * 1024;
    }
    public void closeConnection() {
        if (!socket.isClosed()) {
            try {
                outputObject.writeObject(new ServiceMessage(MessageType.CONNECTION_CLOSE));
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputObject.close();
                outputObject.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                socket.close();
                if (socket.isClosed()) System.out.println("Socket close, client disconnected");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}