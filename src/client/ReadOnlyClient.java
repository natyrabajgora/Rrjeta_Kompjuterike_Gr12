package client;

import java.io.IOException;
import java.util.Scanner;

import static server.ServerConfig.*;

public class ReadOnlyClient extends BaseClient {

    public ReadOnlyClient(int clientId) {
        super(clientId);
    }

    @Override
    public void start() {
        try (Scanner sc = new Scanner(System.in)) {
            System.out.println("=== READ ONLY CLIENT (ID = " + clientId + ") ===");
            sendHello(Permission.READ_ONLY);
            System.out.println(receiveResponse());
            System.out.println("Komandat e lejuara: " + CMD_LIST + ", " +
                    CMD_READ + " <file>, " + CMD_SEARCH + " <keyword>");
            System.out.println(CMD_EXIT + " ose exit/quit për ta mbyllur");
            System.out.println("------------------------------------------------------------");

        while (true) {
            System.out.print("> ");
            if (!sc.hasNextLine()) {
                break;
            }
            String input = sc.nextLine().trim();
            if (input.isBlank()) {
                continue;
            }
            if (isExitCommand(input)) {
                System.out.println("Po e mbyll klientin read-only...");
                break;
            }
            if (input.equals(CMD_LIST)) {
                sendMessage(CMD_LIST);
                System.out.println(receiveResponse());
            } else if (input.startsWith(CMD_READ + " ")) {
                String file = input.substring(CMD_READ.length() + 1).trim();
                sendMessage(CMD_READ + " " + quoteIfNeeded(file));
                System.out.println(receiveResponse());
            } else if (input.startsWith(CMD_SEARCH + " ")) {
                String key = input.substring(CMD_SEARCH.length() + 1).trim();
                sendMessage(CMD_SEARCH + " " + quoteIfNeeded(key));
                System.out.println(receiveResponse());
            } else {
                System.out.println("Nuk ke autorizim për këtë komandë.");
            }
        }
        }finally{
            try {
                close();
            } catch (IOException ignored) {
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
