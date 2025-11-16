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


    private String handleUpload(String cmd) throws IOException {
        // args pritet me qenë:
        // "tessst.txt SGVsbG8AAA..."  (file + base64)

        UploadPayload payload = parseUpload(cmd);
        if (payload == null) {
            return "ERR Usage: " + ServerConfig.CMD_UPLOAD + " <filename> <content>";
        }
        Path serverFile = resolveWithin(serverDir, payload.fileName());
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(payload.base64());
        } catch (IllegalArgumentException e) {
            return "ERR Invalid upload payload (expected Base64)";
        }

        Files.write(serverFile, decoded);
        Path uploadedCopy = resolveWithin(uploadDir, serverFile.getFileName().toString());
        Files.write(uploadedCopy, decoded);

        return "OK Uploaded " + serverFile.getFileName() + " (" + decoded.length + " bytes)";
    }




    private String handleDownload(String cmd) throws IOException {
        String fileName = extractSingleArgument(cmd, ServerConfig.CMD_DOWNLOAD);
        if (fileName == null) {
            return "ERR Usage: /download <filename>";
        }

        Path file = resolveWithin(serverDir, fileName);
        if (!Files.isRegularFile(file)) {
            return "ERR File not found";
        }
        byte[] bytes = Files.readAllBytes(file);
        Path copy = resolveWithin(downloadDir, file.getFileName().toString());
        Files.write(copy, bytes);
        String payload = Base64.getEncoder().encodeToString(bytes);

        return buildBase64Response(file, bytes, payload);
    }

    // ============================
    //   HELPER METHODS
    // ============================

    private String getSecondArg(String cmd) {
        String[] parts = cmd.trim().split("\\s+");
        if (parts.length < 2) return null;
        return parts[1];
    }
    private String extractSingleArgument(String cmd, String keyword) {
        String trimmed = cmd.trim();
        if (!trimmed.startsWith(keyword)) {
            return null;
        }
        String remainder = trimmed.substring(keyword.length()).trim();
        if (remainder.isEmpty()) {
            return null;
        }
        return unquote(remainder);
    }
    private UploadPayload parseUpload(String cmd) {
        String trimmed = cmd.trim();
        if (!trimmed.startsWith(ServerConfig.CMD_UPLOAD)) {
            return null;
        }
        int cursor = ServerConfig.CMD_UPLOAD.length();
        cursor = skipWhitespace(trimmed, cursor);
        if (cursor >= trimmed.length()) {
            return null;
        }
        ParseResult fileResult = parseToken(trimmed, cursor);
        if (fileResult == null) {
            return null;
        }
        cursor = skipWhitespace(trimmed, fileResult.nextIndex());
        if (cursor >= trimmed.length()) {
            return null;
        }
        String payload = trimmed.substring(cursor);
        return new UploadPayload(fileResult.token(), payload);
    }
    private ParseResult parseToken(String source, int start) {
        if (source.charAt(start) == '"') {
            int end = source.indexOf('"', start + 1);
            if (end <= start) {
                return null;
            }
            return new ParseResult(source.substring(start + 1, end), end + 1);
        }
        int end = start;
        while (end < source.length() && !Character.isWhitespace(source.charAt(end))) {
            end++;
        }
        if (end == start) {
            return null;
        }
        return new ParseResult(source.substring(start, end), end);
    }
    private int skipWhitespace(String source, int index) {
        int cursor = index;
        while (cursor < source.length() && Character.isWhitespace(source.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }
    private String unquote(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
    private Path resolveWithin(Path root, String requested) throws IOException {
        if (requested == null || requested.isBlank()) {
            throw new SecurityException("Missing filename");
        }
        Path target = root.resolve(requested).normalize();
        if (!target.startsWith(root)) {
            throw new SecurityException("Path escapes the allowed directory");
        }
        return target;
    }
    private Path ensureDir(String path) throws IOException {
        Path dir = Paths.get(path).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        return dir;
    }
    private boolean isProbablyText(byte[] data) {
        for (byte b : data) {
            int value = b & 0xFF;
            if (value == 0) {
                return false;
            }
            if (value < 0x08) {
                return false;
            }
        }
        return true;
    }
    private String buildBase64Response(Path file, byte[] bytes, String payload) {
        String safeName = escapeHeaderValue(file.getFileName().toString());
        return "DATA_BASE64\nfilename=" + safeName + "\nsize=" + bytes.length + "\n" + payload;
    }
    private String escapeHeaderValue(String value) {
        return value.replace('\n', '_').replace('\r', '_');
    }
    private record UploadPayload(String fileName, String base64) {
    }
    private record ParseResult(String token, int nextIndex) {
    }

}
