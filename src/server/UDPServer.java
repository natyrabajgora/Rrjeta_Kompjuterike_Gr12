package server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class UDPServer {

    // ================== KONSTANTAT BAZË ==================
    public static class Constants {
        public static final int SERVER_PORT = 5000;              // ndryshoje nëse don
        public static final int BUFFER_SIZE = 4096;              // madhësia e paketës UDP
        public static final int MAX_CLIENTS = 10;                // max lidhje aktive
        public static final long CLIENT_TIMEOUT_MS = 20_000;     // 20 sekonda inaktivitet
        public static final String LOGS_DIR = "logs";
        public static final String STATS_LOG_FILE = "logs/server_stats.txt";
        public static final String MSG_LOG_FILE = "logs/messages.log";
    }

    // ================== ENUM PER PRIVILEGJET ==================
    public enum Permission {
        ADMIN,
        READ_ONLY
    }

    // ================== TE DHENAT PER NJE KLIENT ==================
    public static class ClientSession {
        private final SocketAddress address;
        private String clientId;
        private Permission permission;
        private volatile long lastActive;
        private final AtomicLong messagesCount = new AtomicLong(0);
        private final AtomicLong bytesReceived = new AtomicLong(0);
        private final AtomicLong bytesSent = new AtomicLong(0);

        public ClientSession(SocketAddress address) {
            this.address = address;
            this.clientId = address.toString();
            this.permission = Permission.READ_ONLY;
            touch();
        }

        public SocketAddress getAddress() {
            return address;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public Permission getPermission() {
            return permission;
        }

        public void setPermission(Permission permission) {
            this.permission = permission;
        }

        public long getLastActive() {
            return lastActive;
        }

        public long getMessagesCount() {
            return messagesCount.get();
        }

        public long getBytesReceived() {
            return bytesReceived.get();
        }

        public long getBytesSent() {
            return bytesSent.get();
        }

        public void touch() {
            this.lastActive = System.currentTimeMillis();
        }

        public void incrementMessages() {
            messagesCount.incrementAndGet();
        }

        public void addBytesReceived(int bytes) {
            bytesReceived.addAndGet(bytes);
        }

        public void addBytesSent(int bytes) {
            bytesSent.addAndGet(bytes);
        }

        @Override
        public String toString() {
            return "ClientSession{" +
                    "address=" + address +
                    ", clientId='" + clientId + '\'' +
                    ", permission=" + permission +
                    ", lastActive=" + Instant.ofEpochMilli(lastActive) +
                    ", messagesCount=" + messagesCount +
                    ", bytesReceived=" + bytesReceived +
                    ", bytesSent=" + bytesSent +
                    '}';
        }
    }

    // ================== MONITORIMI I TRAFIKUT ==================
    public static class TrafficMonitor {
        private final AtomicLong totalBytesReceived = new AtomicLong(0);
        private final AtomicLong totalBytesSent = new AtomicLong(0);

        public void addBytesReceived(int bytes) {
            totalBytesReceived.addAndGet(bytes);
        }

        public void addBytesSent(int bytes) {
            totalBytesSent.addAndGet(bytes);
        }

        public long getTotalBytesReceived() {
            return totalBytesReceived.get();
        }

        public long getTotalBytesSent() {
            return totalBytesSent.get();
        }

        public String buildStats(Map<SocketAddress, ClientSession> sessions) {
            StringBuilder sb = new StringBuilder();
            sb.append("==== SERVER STATS ====\n");
            sb.append("Timestamp: ").append(Instant.now()).append("\n");
            sb.append("Active connections: ").append(sessions.size()).append("\n");
            sb.append("Total bytes received: ").append(getTotalBytesReceived()).append("\n");
            sb.append("Total bytes sent: ").append(getTotalBytesSent()).append("\n\n");

            for (ClientSession session : sessions.values()) {
                sb.append("Client: ").append(session.getClientId()).append("\n");
                sb.append("  Address: ").append(session.getAddress()).append("\n");
                sb.append("  Permission: ").append(session.getPermission()).append("\n");
                sb.append("  Last active: ").append(Instant.ofEpochMilli(session.getLastActive())).append("\n");
                sb.append("  Messages: ").append(session.getMessagesCount()).append("\n");
                sb.append("  Bytes received: ").append(session.getBytesReceived()).append("\n");
                sb.append("  Bytes sent: ").append(session.getBytesSent()).append("\n\n");
            }

            return sb.toString();
        }

        public void appendStatsToFile(String stats) {
            ensureLogsDir();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(Constants.STATS_LOG_FILE, true))) {
                writer.write(stats);
                writer.write("\n");
            } catch (IOException e) {
                System.err.println("Failed to write stats to file: " + e.getMessage());
            }
        }

        private void ensureLogsDir() {
            File dir = new File(Constants.LOGS_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
    }

    // ================== FUSHAT E SERVERIT ==================
    private DatagramSocket socket;
    private final Map<SocketAddress, ClientSession> sessions = new ConcurrentHashMap<>();
    private final TrafficMonitor trafficMonitor = new TrafficMonitor();
    private final ExecutorService workerPool = Executors.newFixedThreadPool(8);
    private volatile boolean running = false;

    // ================== METODA START ==================
    public void start() throws SocketException {
        socket = new DatagramSocket(Constants.SERVER_PORT);
        running = true;
        System.out.println("UDP Server started on port " + Constants.SERVER_PORT);

        startIdleChecker();
        startConsoleHint();

        while (running) {
            try {
                byte[] buffer = new byte[Constants.BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                workerPool.submit(() -> handlePacket(packet));
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error receiving packet: " + e.getMessage());
                }
            }
        }

        workerPool.shutdown();
        socket.close();
        System.out.println("Server stopped.");
    }

    // ================== HANDLER PER PAKETA ==================
    private void handlePacket(DatagramPacket packet) {
        SocketAddress clientAddress = packet.getSocketAddress();
        int length = packet.getLength();

        trafficMonitor.addBytesReceived(length);

        String message = new String(packet.getData(), 0, length, StandardCharsets.UTF_8).trim();
        System.out.println("Received from " + clientAddress + ": " + message);

        ClientSession session = sessions.compute(clientAddress, (addr, existing) -> {
            if (existing != null) {
                return existing;
            }
            // klient i ri
            if (sessions.size() >= Constants.MAX_CLIENTS) {
                // refuzo lidhje të reja
                sendString("SERVER BUSY: Too many active clients.", clientAddress);
                return null;
            }
            ClientSession newSession = new ClientSession(addr);
            System.out.println("New client registered: " + newSession);
            return newSession;
        });

        if (session == null) {
            // ose ishte i refuzuar
            return;
        }

        session.touch();
        session.incrementMessages();
        session.addBytesReceived(length);

        logMessage(session, message);

        // Protokoll i thjeshtë për identifikim & role:
        // HELLO <clientId> <role>
        // p.sh.: HELLO client1 ADMIN   ose   HELLO client2 READ
        if (message.toUpperCase().startsWith("HELLO")) {
            handleHello(session, message, clientAddress);
            return;
        }

        // Komanda STATS
        if (message.equalsIgnoreCase("STATS")) {
            handleStatsCommand(clientAddress);
            return;
        }

        // Këtu duhen trajtuar komandat si /list, /read, /upload, etj.
        // Këtë pjesë mund ta implementojë FileCommandHandler.
        if (message.startsWith("/")) {
            String response = handleCommandFromClient(session, message);
            sendString(response, clientAddress);
            return;
        }

        // Nëse nuk është komandë, vetëm echo + info
        String echo = "Server received (" + session.getPermission() + "): " + message;
        sendString(echo, clientAddress);
    }

    // ================== HELLO / AUTH ==================
    private void handleHello(ClientSession session, String message, SocketAddress address) {
        try {
            String[] parts = message.split("\\s+");
            if (parts.length >= 3) {
                String clientId = parts[1];
                String roleStr = parts[2].toUpperCase();

                session.setClientId(clientId);
                Permission perm = roleStr.equals("ADMIN") ? Permission.ADMIN : Permission.READ_ONLY;
                session.setPermission(perm);
                String reply = "HELLO " + clientId + ", role set to " + perm;
                sendString(reply, address);
            } else {
                sendString("Usage: HELLO <clientId> <ADMIN|READ>", address);
            }
        } catch (Exception e) {
            sendString("Error in HELLO command: " + e.getMessage(), address);
        }
    }

    // ================== STATS KOMANDA ==================
    private void handleStatsCommand(SocketAddress requester) {
        String stats = trafficMonitor.buildStats(sessions);
        System.out.println(stats);
        trafficMonitor.appendStatsToFile(stats);
        sendString(stats, requester);
    }

    // ================== TRAJTIMI I KOMANDAVE /list, /read, ... ==================
    private String handleCommandFromClient(ClientSession session, String commandLine) {
        // Këtu mund ta thërrasësh FileCommandHandler
        // p.sh.: return FileCommandHandler.handle(session, commandLine);

        // Për momentin, thjesht demonstro:
        if (commandLine.equalsIgnoreCase("/role")) {
            return "Your role is: " + session.getPermission();
        }

        // Shembull kontrolli për admin only:
        if (commandLine.startsWith("/delete") || commandLine.startsWith("/upload")) {
            if (session.getPermission() != Permission.ADMIN) {
                return "ERROR: You do not have permission to execute this command.";
            }
        }

        // TODO: Integro FileCommandHandler këtu
        return "Command received (but not yet implemented): " + commandLine;
    }

    // ================== LOG I MESAZHEVE ==================
    private void logMessage(ClientSession session, String message) {
        File dir = new File(Constants.LOGS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(Constants.MSG_LOG_FILE, true))) {
            writer.write(Instant.now() + " [" + session.getClientId() + "@" + session.getAddress() + "]: " + message);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Failed to log message: " + e.getMessage());
        }
    }

    // ================== DERGIMI I PERGJIGJEVE ==================
    private void sendString(String response, SocketAddress address) {
        byte[] data = response.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, (InetSocketAddress) address);
        try {
            socket.send(packet);
            trafficMonitor.addBytesSent(data.length);
            ClientSession session = sessions.get(address);
            if (session != null) {
                session.addBytesSent(data.length);
            }
        } catch (IOException e) {
            System.err.println("Error sending response to " + address + ": " + e.getMessage());
        }
    }

    // ================== THREAD PER TIMEOUT ==================
    private void startIdleChecker() {
        Thread t = new Thread(() -> {
            while (running) {
                try {
                    long now = System.currentTimeMillis();
                    for (Map.Entry<SocketAddress, ClientSession> entry : sessions.entrySet()) {
                        ClientSession session = entry.getValue();
                        if (now - session.getLastActive() > Constants.CLIENT_TIMEOUT_MS) {
                            System.out.println("Client timed out and removed: " + session);
                            sessions.remove(entry.getKey());
                        }
                    }

                    Thread.sleep(5000); // kontrollo çdo 5 sekonda
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ================== HINT PER STATS NGA KONZOLA ==================
    private void startConsoleHint() {
        // thjesht për me tregu se ekziston komanda STATS
        System.out.println("Tip: Clients can send 'STATS' to receive server statistics.");
    }

    // ================== MAIN ==================
    public static void main(String[] args) {
        UDPServer server = new UDPServer();
        try {
            server.start();
        } catch (SocketException e) {
            System.err.println("Failed to start UDP server: " + e.getMessage());
        }
    }
}
