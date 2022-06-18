import java.io.*;
import java.net.Socket;
import java.nio.file.Files;

public class FileTransfer {
    public String DIR;
    public Socket socket;
    public ObjectInputStream inputObject;
    public ObjectOutputStream outputObject;
    public OutputStream outputFile;
    public boolean receiveReady = true;
    public boolean sendingReady = true;
    public File rFile;
    public File sFile;
    public int receiveNum;
    public long totalWrite;

    public Socket getSocket() {
        return socket;
    }

    public void request(MessageType messageType, String fileName) {
        ServiceMessage msg = new ServiceMessage(messageType, fileName);
        try {
            outputObject.writeObject(msg);
            outputObject.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receiveReady(String fileName) throws IOException {
        rFile = new File(DIR, fileName);
        if (rFile.exists()) System.out.println("File will be rewriting");
        outputFile = Files.newOutputStream(rFile.toPath());
        receiveNum = 0;
        totalWrite = 0;
    }

    public void sendingReady(String fileName) {
        sFile = new File(DIR, fileName);
        if (!sFile.exists()) System.out.println("File not found!");
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
            System.out.println("Receiving file completed, transferred " + totalWrite + " bytes");
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
                System.out.println("Sending file completed, transferred " + totalRead + " bytes");
                sendingReady = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long splitSize() {
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
