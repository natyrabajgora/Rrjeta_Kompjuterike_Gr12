package client;

import java.util.Scanner;

public class ReadOnlyClient extends BaseClient {

    public ReadOnlyClient(int clientId) {
        super(clientId);
    }

    @Override
    public void start() {
        Scanner sc = new Scanner(System.in);

        System.out.println("=== READ ONLY CLIENT (ID = " + clientId + ") ===");
        System.out.println("Komandat e lejuara: /list, /read <file>, /search <keyword>");
        System.out.println("------------------------------------------------------------");

        while (true) {
            System.out.print("> ");
            String input = sc.nextLine();

            if (input.equals("/list")) {
                sendPacket("LIST", "");
                System.out.println(receiveResponse());
            }

            else if (input.startsWith("/read ")) {
                String file = input.substring(6);
                sendPacket("READ", file);
                System.out.println(receiveResponse());
            }

            else if (input.startsWith("/search ")) {
                String key = input.substring(8);
                sendPacket("SEARCH", key);
                System.out.println(receiveResponse());
            }

            else {
                System.out.println("Nuk ke autorizim për këtë komandë.");
            }
        }
    }
}
