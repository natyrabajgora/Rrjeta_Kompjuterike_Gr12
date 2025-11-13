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
        System.out.println("Komandat:");
        System.out.println("/list");
        System.out.println("/read <filename>");
        System.out.println("/upload <filename>");
        System.out.println("/download <filename>");
        System.out.println("/delete <filename>");
        System.out.println("/search <keyword>");
        System.out.println("/info <filename>");
        System.out.println("--------------------------------------");

        while (true) {
            System.out.print("> ");
            String input = sc.nextLine();
            executeCommand(input);
        }
    }

    private void executeCommand(String input) {
        try {
            if (input.equals("/list")) {
                sendPacket("LIST", "");
                System.out.println(receiveResponse());
            }

            else if (input.startsWith("/read ")) {
                String file = input.substring(6);
                sendPacket("READ", file);
                System.out.println(receiveResponse());
            }

            else if (input.startsWith("/delete ")) {
                String file = input.substring(8);
                sendPacket("DELETE", file);
                System.out.println(receiveResponse());
            }

            else if (input.startsWith("/search ")) {
                String key = input.substring(8);
                sendPacket("SEARCH", key);
                System.out.println(receiveResponse());
            }

            else if (input.startsWith("/info ")) {
                String file = input.substring(6);
                sendPacket("INFO", file);
                System.out.println(receiveResponse());
            }

            else if (input.startsWith("/upload ")) {
                uploadFile(input.substring(8));
            }

            else if (input.startsWith("/download ")) {
                String file = input.substring(10);
                sendPacket("DOWNLOAD", file);
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

            sendPacket("UPLOAD", filename + "::" + base64);
            System.out.println(receiveResponse());

        } catch (Exception e) {
            System.out.println("Gabim ne upload!");
        }
    }
}
