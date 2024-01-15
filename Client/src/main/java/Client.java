import java.io.*;
import java.net.Socket;
import java.util.LinkedList;

public class Client extends FileTransfer implements Runnable{
    private final LinkedList<String> receiveList;
    private final LinkedList<String> sendingList;
    private final String host;
    private final int port;

    public Client(String host, int port) {
        DIR = "/Users/andrejdubovik/Desktop/Client/";
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

    @Override
    public void receiveReady(String fileName) throws IOException {
        super.receiveReady(fileName);
        request(MessageType.RECEIVE_REQUEST, fileName);
        receiveReady = false;
        receiveList.removeFirst();
    }

    @Override
    public void sendingReady(String fileName) {
        super.sendingReady(fileName);
        if (sFile.exists()) {
            sendingReady = false;
            request(MessageType.SENDING_REQUEST, fileName);
        }
        sendingList.removeFirst();
    }
}