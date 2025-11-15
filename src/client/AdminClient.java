package client;

import java.io.File;
import java.io.FileInputStream;
import java.util.Scanner;

public class AdminClient extends BaseClient {

    public AdminClient(int clientId) {
        super(clientId);
    }

    @Override
    public void start() {
        Scanner sc = new Scanner(System.in);

        System.out.println("=== ADMIN CLIENT (ID = " + clientId + ") ===");
        sendHello("ADMIN");
        System.out.println(receiveResponse());
        System.out.println("Komandat:");
        System.out.println("/list");
        System.out.println("/read <filename>");
        System.out.println("/upload <filename>");
        System.out.println("/download <filename>");
        System.out.println("/delete <filename>");
        System.out.println("/search <keyword>");
        System.out.println("/info <filename>");
        System.out.println("stats (komande pa slash per statistikat e serverit)");
        System.out.println("--------------------------------------");

        while (true) {
            System.out.print("> ");
            String input = sc.nextLine();
            if(input.equalsIgnoreCase("stats")){
                sendMessage("STATS");
                System.out.println(receiveResponse());
                continue;
            }
            executeCommand(input);
        }
    }

    private void executeCommand(String input) {
        try {
            if (input.equals("/list")) {
                sendMessage("/list");
                System.out.println(receiveResponse());
            }

            else if (input.startsWith("/read ")) {
                String file = input.substring(6);
                sendMessage("/read" + file);
                System.out.println(receiveResponse());
            }

            else if (input.startsWith("/delete ")) {
                String file = input.substring(8);
                sendMessage("/delete" + file);
                System.out.println(receiveResponse());
            }

            else if (input.startsWith("/search ")) {
                String key = input.substring(8);
                sendMessage("/search" + key);
                System.out.println(receiveResponse());
            }

            else if (input.startsWith("/info ")) {
                String file = input.substring(6);
                sendMessage("/info" + file);
                System.out.println(receiveResponse());
            }

            else if (input.startsWith("/upload ")) {
                uploadFile(input.substring(8));
            }

            else if (input.startsWith("/download ")) {
                String file = input.substring(10);
                sendMessage("/download" + file);
                System.out.println(receiveResponse());
            }

            else {
                System.out.println("Komandë e panjohur!");
            }

        } catch (Exception e) {
            System.out.println("Gabim në ekzekutim!");
        }
    }

    private void uploadFile(String filename) {
        try {
            File file = new File(filename);

            if (!file.exists()) {
                System.out.println("File nuk ekziston!");
                return;
            }

            FileInputStream fis = new FileInputStream(file);
            byte[] bytes = fis.readAllBytes();
            fis.close();

            String base64 = java.util.Base64.getEncoder().encodeToString(bytes);

            sendMessage("/upload" + file.getName() + " " + base64);
            System.out.println(receiveResponse());

        } catch (Exception e) {
            System.out.println("Gabim ne upload!");
        }
    }
    public static void main(String[] args) {
        int clientId = 1;

        if (args.length > 0) {
            try {
                clientId = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("ID i klientit duhet të jetë numër.");
                return;
            }
        }

        new AdminClient(clientId).start();
    }
}
