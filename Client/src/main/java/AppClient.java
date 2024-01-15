import java.io.IOException;

public class AppClient {
    private static final String HOST = "localhost";
    private static final int PORT = 65500;

    public static void main(String[] args) throws IOException {
        Client client = new Client(HOST, PORT);
        new Thread(client).start();
        new ClientGui(client);
    }
}