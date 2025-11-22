package server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import server.ClientSession.Permission;
import java.util.concurrent.atomic.AtomicInteger;
import static server.ClientSession.Permission.*;
import server.TraficMonitor;


public class UDPServer {

    // ================== KONSTANTAT BAZË ==================
    public static class Constants {
        public static final int SERVER_PORT = 5000;              // ndryshoje nëse don
        public static final int BUFFER_SIZE = 4096;              // madhësia e paketës UDP
        public static final int MAX_CLIENTS = 10;                // max lidhje aktive
        public static final long CLIENT_TIMEOUT_MS = 20_000;     // 20 sekonda inaktivitet
        public static final String LOGS_DIR = "logs";
        public static final String STATS_LOG_FILE = LOGS_DIR + "/server_stats.txt";
        public static final String MSG_LOG_FILE = LOGS_DIR + "/messages.log";
        public static final String DATA_DIR = "data";
        public static final String SERVER_FILES_DIR = DATA_DIR + "/server_files";
        public static final String UPLOADS_DIR = DATA_DIR + "/uploads";
        public static final String DOWNLOADS_DIR = DATA_DIR + "/downloads";

    }

    // ================== FUSHAT E SERVERIT ==================
    private DatagramSocket socket;
    private final Map<SocketAddress, ClientSession> sessions = new ConcurrentHashMap<>();
    private final TraficMonitor trafficMonitor = new TraficMonitor();
    private final FileCommandHandler fileCommandHandler = new FileCommandHandler(Constants.SERVER_FILES_DIR, Constants.UPLOADS_DIR, Constants.DOWNLOADS_DIR);
    private final ExecutorService workerPool = Executors.newFixedThreadPool(8);
    private final AtomicInteger activeClientCount = new AtomicInteger(0);
    private volatile boolean running = false;

    // ================== METODA START ==================
    public void start() throws SocketException {
        int port = ServerConfig.resolveServerPort();
        socket = new DatagramSocket(port);
        running = true;
        System.out.println("UDP Server started on port " + port + " (host " + ServerConfig.resolveServerHost() + ")");

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
            int newCount = activeClientCount.incrementAndGet();
            if (newCount > ServerConfig.MAX_CLIENTS) { // refuzo lidhje te reja
                activeClientCount.decrementAndGet();
                return null;
            }
            ClientSession newSession = new ClientSession(addr);
            System.out.println("New client registered: " + newSession);
            return newSession;
        });

        if (session == null) {
            sendString("SERVER BUSY: Too many active clients.", clientAddress);
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
        if (!session.isAuthenticated()) {
            sendString("ERR Ju lutem identifikohuni me HELLO <clientId> <ADMIN|READ>", clientAddress);
            return;
        }


        // Komanda STATS
        if (ServerConfig.CMD_STATS.equalsIgnoreCase(message)) {
            if (!session.getPermission().equals(ADMIN)) {
                sendString("ERR Permission denied (admin only)", clientAddress);
                return;
            }
            handleStatsCommand(session);
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
        sendString("ERR Unknown command. Përdor komandat që fillojnë me '/'", clientAddress);
    }

    // ================== HELLO / AUTH ==================
    private void handleHello(ClientSession session, String message, SocketAddress address) {
        HelloPayload payload = parseHello(message);
        if (payload == null) {
            sendString("Usage: " + ServerConfig.CMD_HELLO + " <clientId> <ADMIN|READ>", address);
            return;
        }
        session.setClientId(payload.clientId());
        session.setPermission(payload.role());
        session.markAuthenticated();
        String reply = ServerConfig.CMD_HELLO + " " + payload.clientId() + ", role set to " + payload.role();
        sendString(reply, address);
    }

    private HelloPayload parseHello(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        // Prishet mesazhi sipas hapësirave
        String[] parts = message.trim().split("\\s+");

        // Format i pritshëm: HELLO <clientId> <ADMIN|READ>
        if (parts.length != 3) {
            return null;  // format i gabuar
        }

        // Kontrollo komandën
        if (!parts[0].equalsIgnoreCase("HELLO")) {
            return null;
        }

        String clientId = parts[1];
        String roleString = parts[2].toUpperCase();

        ClientSession.Permission role;

        try {
            // ADMIN ose READ
            role = ClientSession.Permission.valueOf(roleString);
        } catch (IllegalArgumentException e) {
            return null; // role i gabuar
        }

        // Kthe payload-in e strukturuar
        return new HelloPayload(clientId, role);
    }

    // ================== STATS KOMANDA ==================
    private void handleStatsCommand(ClientSession requester) {
        String stats = trafficMonitor.buildStats(sessions);
        System.out.println(stats);
        trafficMonitor.appendStatsToFile(stats);
        sendString(stats, requester.getAddress());
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
            if (session.getPermission() != ADMIN) {
                return "ERROR: You do not have permission to execute this command.";
            }
        }

        String role = session.getPermission().name();
        return fileCommandHandler.handle(commandLine, role);
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
                            removeSession(entry.getKey());
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
    private void removeSession(SocketAddress address) {
        ClientSession removed = sessions.remove(address);
        if (removed != null) {
            activeClientCount.decrementAndGet();
        }
    }
    private record HelloPayload(String clientId, ClientSession.Permission role) { }
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
