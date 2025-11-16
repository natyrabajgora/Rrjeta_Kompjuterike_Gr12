package server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
public class FileCommandHandler {

    private final Path serverDir;
    private final Path uploadDir;
    private final Path downloadDir;

    public FileCommandHandler() {
        this("data/server_files", "data/uploads", "data/downloads");
    }

    public FileCommandHandler(String serverPath, String uploadPath, String downloadPath) {
        try {
            this.serverDir = ensureDir(serverPath);
            this.uploadDir = ensureDir(uploadPath);
            this.downloadDir = ensureDir(downloadPath);
        } catch (IOException e) {
            throw new IllegalStateException("Nuk mund të krijohen direktoriumet e serverit", e);
        }
    }


    public String handle(String commandLine, String role) {
        if (commandLine == null || commandLine.isEmpty()) {
            return "ERR Empty command";
        }


        String cmd = commandLine.trim();

        try {
            if (cmd.startsWith("/list")) {
                return handleList();
            }

            if (cmd.startsWith("/read")) {
                return handleRead(cmd);
            }

            if (cmd.startsWith("/info")) {
                return handleInfo(cmd);
            }

            if (cmd.startsWith("/search")) {
                return handleSearch(cmd);
            }


            if (!"admin".equalsIgnoreCase(role)) {
                // nëse nuk është admin e provon njërën prej këtyre:
                if (cmd.startsWith("/upload") ||
                        cmd.startsWith("/download") ||
                        cmd.startsWith("/delete")) {
                    return "ERR Permission denied (admin only)";
                }
            }

            if (cmd.startsWith("/delete")) {
                return handleDelete(cmd);
            }

            if (cmd.startsWith("/upload")) {
                return handleUpload(cmd);
            }

            if (cmd.startsWith("/download")) {
                return handleDownload(cmd);
            }

            return "ERR Unknown command";

        } catch (Exception e) {
            return "ERR " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    // ============================
    //   KOMANDAT READ-ONLY
    // ============================

    // /list
    private String handleList() {
        StringBuilder sb = new StringBuilder();
        File[] files = serverDir.listFiles();
        if (files == null || files.length == 0) {
            return "DATA\n(no files)";
        }

        for (File f : files) {
            if (f.isFile()) {
                sb.append(f.getName()).append("\n");
            }
        }
        return "DATA\n" + sb;
    }

    // /read filename
    private String handleRead(String cmd) throws IOException {
        String fileName = getSecondArg(cmd);
        if (fileName == null) {
            return "ERR Usage: /read <filename>";
        }

        File file = new File(serverDir, fileName);
        if (!file.exists() || !file.isFile()) {
            return "ERR File not found";
        }

        String content = Files.readString(file.toPath());
        return "DATA\n" + content;
    }

    // /info filename
    private String handleInfo(String cmd) {
        String fileName = getSecondArg(cmd);
        if (fileName == null) {
            return "ERR Usage: /info <filename>";
        }

        File file = new File(serverDir, fileName);
        if (!file.exists() || !file.isFile()) {
            return "ERR File not found";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(file.getName()).append("\n");
        sb.append("Size: ").append(file.length()).append(" bytes\n");
        sb.append("LastModified: ").append(file.lastModified()).append("\n");

        return "DATA\n" + sb;
    }

    // /search keyword
    private String handleSearch(String cmd) {
        String keyword = getSecondArg(cmd);
        if (keyword == null) {
            return "ERR Usage: /search <keyword>";
        }

        keyword = keyword.toLowerCase();
        StringBuilder sb = new StringBuilder();

        File[] files = serverDir.listFiles();
        if (files == null || files.length == 0) {
            return "DATA\n(no files)";
        }

        for (File f : files) {
            if (f.isFile() && f.getName().toLowerCase().contains(keyword)) {
                sb.append(f.getName()).append("\n");
            }
        }

        if (sb.length() == 0) {
            return "DATA\n(no matches)";
        }

        return "DATA\n" + sb;
    }

    // ============================
    //   KOMANDAT ADMIN
    // ============================

    // /delete filename
    private String handleDelete(String cmd) {
        String fileName = getSecondArg(cmd);
        if (fileName == null) {
            return "ERR Usage: /delete <filename>";
        }

        File file = new File(serverDir, fileName);
        if (!file.exists() || !file.isFile()) {
            return "ERR File not found";
        }

        boolean ok = file.delete();
        if (ok) {
            return "OK File deleted";
        } else {
            return "ERR Could not delete file";
        }
    }


    private String handleUpload(String args) throws IOException {
        // args pritet me qenë:
        // "tessst.txt SGVsbG8AAA..."  (file + base64)

        String[] parts = args.split(" ", 2);  // VETËM 2 pjesë: emri + base64

        if (parts.length < 2) {
            return "ERR Usage: /upload <filename>";
        }

        String fileName = parts[0];
        String content  = parts[1];

        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(content);
        } catch (IllegalArgumentException e) {
            return "ERR Invalid upload payload (expected Base64)";
        }

        File serverFile = new File(serverDir, fileName);
        Files.write(serverFile.toPath(), decoded);

        File uploadedCopy = new File(uploadDir, fileName);
        Files.write(uploadedCopy.toPath(), decoded);

        return "OK Uploaded " + fileName + " (" + decoded.length + " bytes)";
    }




    private String handleDownload(String cmd) throws IOException {
        String fileName = getSecondArg(cmd);
        if (fileName == null) {
            return "ERR Usage: /download <filename>";
        }

        File file = new File(serverDir, fileName);
        if (!file.exists() || !file.isFile()) {
            return "ERR File not found";
        }

        String content = Files.readString(file.toPath());

        // opsionale: e ruan edhe lokal si kopje
        File copy = new File(downloadDir, fileName);
        Files.writeString(copy.toPath(), content);

        return "DATA\n" + content;
    }

    // ============================
    //   HELPER METHODS
    // ============================

    private String getSecondArg(String cmd) {
        String[] parts = cmd.trim().split("\\s+");
        if (parts.length < 2) return null;
        return parts[1];
    }
}
