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
        sendHello("READ");
        System.out.println(receiveResponse());
        System.out.println("Komandat e lejuara: /list, /read <file>, /search <keyword>");
        System.out.println("------------------------------------------------------------");

        while (true) {
            System.out.print("> ");
            String input = sc.nextLine();

            if (input.equals("/list")) {
                sendMessage("/list");
                System.out.println(receiveResponse());
            }

            else if (input.startsWith("/read ")) {
                String file = input.substring(6);
                sendMessage("/read" + file);
                System.out.println(receiveResponse());
            }

            else if (input.startsWith("/search ")) {
                String key = input.substring(8);
               sendMessage("/seach" + key);
                System.out.println(receiveResponse());
            }

            else {
                System.out.println("Nuk ke autorizim për këtë komandë.");
            }
        }
    }
        public static void main(String[] args){
        int clientId = 1;

        if(args.length > 0){
            try {
                clientId = Integer.parseInt(args[0]);
            }catch (NumberFormatException e){
                System.out.println("ID i klientit duhet te jete numer.");
                return;
            }
        }
        new ReadOnlyClient(clientId).start();
        }
}
