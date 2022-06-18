import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;

public class Server {
    private final String DIR = "/Users/andrejdubovik/Desktop/Server/";
//    private final String DIR = "../FileTransferIO/ServerIO/src/main/resources/";
    private Socket socket = null;
    private ObjectInputStream inputObject;
    private ObjectOutputStream outputObject;
    private OutputStream outputFile;
    private File rFile;
    private File sFile;
    private int receiveNum;
    private long totalWrite;

    public Server (int port) {
        System.out.println("Server started...");
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            socket = serverSocket.accept();
            System.out.println("Client connected");
            // Для нескольких клиентов завести тредпул и завернуть всё в бесконечный цикл
            outputObject = new ObjectOutputStream(socket.getOutputStream());
            inputObject = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (!socket.isClosed()) {
            try {
                ServiceMessage msg = (ServiceMessage) inputObject.readObject();
                switch (msg.getMessageType()) {
                    case CONNECTION_CLOSE: {
                        closeConnection();
                        break;
                    }
                    case SENDING_REQUEST: // подготовка к приёму и команда на отправку
                    {
                        receiveReady(msg.getFileName());
                        break;
                    }
                    case RECEIVE_REQUEST: // подготовка к передаче и отправка размера файла
                    {
                        sendingReady(msg.getFileName());
                        break;
                    }
                    case RECEIVE_FILE: // получение файла с клиента и запись на сервере
                    {
                        receiveFile(msg);
                        break;
                    }
                }
            } catch (IOException e) {
                closeConnection();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
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

        request(MessageType.SENDING_FILE, fileName);
    }
    private void sendingReady(String fileName) {
        sFile = new File(DIR, fileName);
        if (!sFile.exists()) System.out.println("File not found!");

        if (sFile.exists()) new Thread(this::sendingFile).start();
        else request(MessageType.SENDING_ERROR, fileName);
    }

    public void receiveFile(ServiceMessage msg) {
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
             //если не равны номера части которая должна быть и которая пришла - дать команду вернуться к предыдущей
        }
        if (totalWrite == msg.getFileSize()) {
            System.out.println("File transfer from client completed, transferred " + totalWrite + " bytes");
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
                System.out.println("File transfer to client completed, transferred " + totalRead + " bytes");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int splitSize() {
        return 8 * 1024;
    }
    private void closeConnection() {
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