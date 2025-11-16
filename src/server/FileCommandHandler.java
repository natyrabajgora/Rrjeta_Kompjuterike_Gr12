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
        if (commandLine == null || commandLine.isBlank()) {
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
        } catch (SecurityException e) {
            return "ERR " + e.getMessage();
        } catch (Exception e) {
            return "ERR " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    // ============================
    //   KOMANDAT READ-ONLY
    // ============================

    // /list
    private String handleList() throws IOException {
        try (var stream = Files.list(serverDir)) {
            List<String> files = stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.toList());
            if (files.isEmpty()) {
                return "DATA\n(no files)";
            }
            return "DATA\n" + String.join("\n", files);
        }
    }

    // /read filename
    private String handleRead(String cmd) throws IOException {
        String fileName = extractSingleArgument(cmd, ServerConfig.CMD_READ);
        if (fileName == null) {
            return "ERR Usage: /read <filename>";
        }

        Path file = resolveWithin(serverDir, fileName);
        if (!Files.isRegularFile(file)) {
            return "ERR File not found";
        }

        byte[] bytes = Files.readAllBytes(file);
        if (isProbablyText(bytes)) {
            return "DATA\n" + new String(bytes, StandardCharsets.UTF_8);
        }
        String base64 = Base64.getEncoder().encodeToString(bytes);
        return buildBase64Response(file, bytes, base64);
    }

    // /info filename
    private String handleInfo(String cmd) throws IOException {
        String fileName = extractSingleArgument(cmd, ServerConfig.CMD_INFO);
        if (fileName == null) {
            return "ERR Usage: /info <filename>";
        }

        Path file = resolveWithin(serverDir, fileName);
        if (!Files.isRegularFile(file)) {
            return "ERR File not found";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(file.getFileName()).append("\n");
        sb.append("Size: ").append(Files.size(file)).append(" bytes\n");
        sb.append("LastModified: ").append(Files.getLastModifiedTime(file)).append("\n");;

        return "DATA\n" + sb;
    }

    // /search keyword
    private String handleSearch(String cmd) throws IOException {
        String keyword = extractSingleArgument(cmd, ServerConfig.CMD_SEARCH);
        if (keyword == null) {
            return "ERR Usage: /search <keyword>";
        }

        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        try (var stream = Files.list(serverDir)) {
            List<String> matches = stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.toLowerCase(Locale.ROOT).contains(lowerKeyword))
                    .sorted()
                    .collect(Collectors.toList());
            if (matches.isEmpty()) {
                return "DATA\n(no matches)";
            }
            return "DATA\n" + String.join("\n", matches);
        }
    }

    // ============================
    //   KOMANDAT ADMIN
    // ============================

    // /delete filename
    private String handleDelete(String cmd) throws IOException {
        String fileName = extractSingleArgument(cmd, ServerConfig.CMD_DELETE);
        if (fileName == null) {
            return "ERR Usage: /delete <filename>";
        }

        Path file = resolveWithin(serverDir, fileName);
        if (!Files.isRegularFile(file)) {
            return "ERR File not found";
        }

        Files.delete(file);
        return "OK File deleted";
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
