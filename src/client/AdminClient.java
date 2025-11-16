package client;

import java.io.File;
import java.io.FileInputStream;
import java.util.Scanner;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static server.ServerConfig.*;

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
            if (input.equals(CMD_LIST)) {
                sendTextCommand(CMD_LIST);
                System.out.println(receiveResponse());
            }

            else if (input.startsWith(CMD_READ + " ")) {
                String file = input.substring(CMD_READ.length() + 1).trim();
                sendMessage(CMD_READ + " " + quoteIfNeeded(file));
                System.out.println(receiveResponse());
            }

            else if (input.startsWith(CMD_DELETE + " "))  {
                String file = input.substring(CMD_DELETE.length() + 1).trim();
                sendMessage(CMD_DELETE + " " + quoteIfNeeded(file));
                System.out.println(receiveResponse());
            }

            else if (input.startsWith(CMD_SEARCH + " ")) {
                String key = input.substring(CMD_SEARCH.length() + 1).trim();
                sendMessage(CMD_SEARCH + " " + quoteIfNeeded(key));
                System.out.println(receiveResponse());
            }

            else if (input.startsWith(CMD_INFO + " ")) {
                String file = input.substring(CMD_INFO.length() + 1).trim();
                sendMessage(CMD_INFO + " " + quoteIfNeeded(file));
                System.out.println(receiveResponse());
            }

            else if (input.startsWith(CMD_UPLOAD + " ")) {
                uploadFile(input.substring(CMD_UPLOAD.length() + 1).trim());
            }

            else if (input.startsWith(CMD_DOWNLOAD + " "))  {
                String file = input.substring(CMD_DOWNLOAD.length() + 1).trim();
                handleDownload(file);
            }

            else {
                System.out.println("Komandë e panjohur!");
            }

        } catch (Exception e) {
            System.out.println("Gabim në ekzekutim!");
        }
    }

    private void uploadFile(String filename) {
        File file = new File(filename);
        if (!file.exists() || !file.isFile()) {
            System.out.println("File nuk ekziston!");
            return;
        }
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = fis.readAllBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String payload = CMD_UPLOAD + " " + quoteIfNeeded(file.getName()) + " " + base64;
            if (payload.getBytes(StandardCharsets.UTF_8).length > BUFFER_SIZE) {
                System.out.println("File është shumë i madh për t'u dërguar në një paketë UDP. Kufizohet në " + BUFFER_SIZE + " bajte.");
                return;
            }
            sendMessage(payload);
            System.out.println(receiveResponse());
        }catch(Exception e){
            System.out.println("Gabim ne upload: " + e.getMessage());
        }
    }
    private void handleDownload(String file) {
        sendMessage(CMD_DOWNLOAD + " " + quoteIfNeeded(file));
        String response = receiveResponse();
        if (response.startsWith("DATA_BASE64")) {
            persistDownloadedFile(response);
        } else {
            System.out.println(response);
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
