import java.io.IOException;
import java.util.Scanner;

public class ClientGui {

    public ClientGui(Client client) throws IOException {

        Scanner in = new Scanner(System.in);

        while (client.getSocket().isConnected()) {
            System.out.print("Input command: (s)end / (r)eceive / (e)xit => ");
            String com = in.nextLine();
            if (com.equalsIgnoreCase("e") || com.equalsIgnoreCase("exit")) {
                client.closeConnection();
                break;
            }
            if (com.equalsIgnoreCase("s") || com.equalsIgnoreCase("r")) {
                System.out.println("Input name file");
                String fileName = in.nextLine();
                if (com.equalsIgnoreCase("s")) client.sending(fileName);
                if (com.equalsIgnoreCase("r")) client.receive(fileName);
            }
            client.fileTransfer();
        }
    }
}