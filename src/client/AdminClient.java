package client;

import java.io.File;
import java.io.FileInputStream;
import java.util.Scanner;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class AdminClient extends BaseClient {

    public AdminClient(int clientId) {
        super(clientId);
    }

    @Override
    public void start() {
        try (Scanner sc = new Scanner(System.in)) {
            System.out.println("=== ADMIN CLIENT (ID = " + clientId + ") ===");
            sendHello(Permission.ADMIN);
            System.out.println(receiveResponse());
            printMenu();

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
                    System.out.println("Po dal nga klienti...");
                    break;
                }
                if (input.equalsIgnoreCase(CMD_STATS)) {
                    sendMessage(CMD_STATS);
                    System.out.println(receiveResponse());
                    continue;
                }
                executeCommand(input);
            }
        } finally {
            try {
                close();
            } catch (IOException ignored) {
            }
        }
    }
    public void printMenu(){
        System.out.println("Komandat:");
        System.out.println("/list");
        System.out.println("/read <filename>");
        System.out.println("/upload <filename>");
        System.out.println("/download <filename>");
        System.out.println("/delete <filename>");
        System.out.println("/search <keyword>");
        System.out.println("/info <filename>");
        System.out.println("stats (komande pa slash per statistikat e serverit)");
        System.out.println(CMD_EXIT + " ose exit për ta mbyllur klientin"); // duhet me konfiguru cmd_exit ne serverconfig hala spodi cka me vendos
        System.out.println("--------------------------------------");
    }

    private void executeCommand(String input) {
        try {
            if (input.equals("/list")) {
                sendTextCommand(input);
                System.out.println(receiveResponse());
            }

            else if (input.startsWith("/read ")) {
                sendTextCommand(input);
                System.out.println(receiveResponse());
            }

            else if (input.startsWith("/delete ")) {
                sendTextCommand(input);
                System.out.println(receiveResponse());
            }

            else if (input.startsWith("/search ")) {
                sendTextCommand(input);
                System.out.println(receiveResponse());
            }

            else if (input.startsWith("/info ")) {
                sendTextCommand(input);
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
            File file = new File("data/uploads/" + filename);

            System.out.println("Looking for: " + file.getAbsolutePath());

            if (!file.exists()) {
                System.out.println("File nuk ekziston!");
                return;
            }

            FileInputStream fis = new FileInputStream(file);
            byte[] bytes = fis.readAllBytes();
            fis.close();

            String base64 = java.util.Base64.getEncoder().encodeToString(bytes);

            // KY ËSHTË FORMAT IQE PRET SERVERI
            sendPacket("UPLOAD", file.getName() + " " + base64);

            System.out.println(receiveResponse());

        } catch (Exception e) {
            System.out.println("Gabim ne upload!");
            e.printStackTrace();
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
