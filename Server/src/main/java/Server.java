import java.io.*;
import java.net.ServerSocket;

public class Server extends FileTransfer {

    public Server (int port) {
        DIR = "/Users/andrejdubovik/Desktop/Server/";
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

    @Override
    public void receiveReady(String fileName) throws IOException {
        super.receiveReady(fileName);
        request(MessageType.SENDING_FILE, fileName);
    }

    @Override
    public void sendingReady(String fileName) {
        super.sendingReady(fileName);
        if (sFile.exists()) new Thread(this::sendingFile).start();
        else request(MessageType.SENDING_ERROR, fileName);
    }
}