package client;
import server.ServerConfig;
import java.io.Closeable;
import java.io.IOException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import server.ServerConfig;
import java.io.Closeable;
import java.io.IOException;

public abstract class BaseClient implements Closeable {
    public static final int BUFFER_SIZE = ServerConfig.BUFFER_SIZE;
    private static final String CMD_EXIT = "/exit";
    protected final int clientId;
    protected final String clientIdentifier;
    protected final InetAddress serverAddress;
    protected final int serverPort;
    protected final DatagramSocket socket;
    private static final int SOCKET_TIMEOUT_MS = 5_000;

    protected enum Permission {
        ADMIN("ADMIN"),
        READ_ONLY("READ_ONLY");

        private final String keyword;

        Permission(String keyword) {
            this.keyword = keyword;
        }

        public String keyword() {
            return keyword;
        }

        public boolean isAdmin() {
            return this == ADMIN;
        }
    }

    public BaseClient(int clientId) {
        this.clientId = clientId;
        this.clientIdentifier = "client" + clientId;
        try {
            this.serverAddress = InetAddress.getByName(ServerConfig.resolveServerHost());
            this.serverPort = ServerConfig.resolveServerPort();
            this.socket = new DatagramSocket();
            this.socket.setSoTimeout(SOCKET_TIMEOUT_MS);
        } catch (Exception e) {
            throw new IllegalStateException("Nuk mund të inicializohet klienti: " + e.getMessage(), e);
        }
    }

    // e ndreq formatin e mesazhit që serveri e pret
    protected String getClientIdentifier(){
        return clientIdentifier;
    }

    protected boolean isExitCommand(String input) {
        if (input == null) {
            return false;
        }
        return CMD_EXIT.equalsIgnoreCase(input.trim()) ||
                "exit".equalsIgnoreCase(input.trim()) ||
                "quit".equalsIgnoreCase(input.trim());
    }

    // e qon porosin ne server
    protected void sendMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        try {
            byte[] data = message.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(
                    data, data.length, serverAddress, serverPort
            );

            socket.send(packet);

        } catch (Exception e) {
            System.out.println("Error sending packet: " + e.getMessage());
        }
    }
    protected void sendTextCommand(String text) throws IOException {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(
                data,
                data.length,
                serverAddress,
                serverPort
        );
        socket.send(packet);
    }
    protected void sendHello(String roleKeyword){
        sendMessage("HELLO " + getClientIdentifier() + " " + roleKeyword);
    }

    protected void sendPacket(String op, String payload) throws IOException {
        // Nderto mesazhin qe do te dergohet te serveri
        String message;
        if (payload == null || payload.isBlank()) {
            message = op;
        } else {
            message = op + " " + payload;
        }

        // Përdor sendMessage ekzistues për ta dërguar
        sendMessage(message);
    }

    // merr pergjigjen nga serveri
    protected String receiveResponse() {
        try {
            byte[] buffer = new byte[8192];
            DatagramPacket resp = new DatagramPacket(buffer, buffer.length);

            socket.receive(resp);
            return new String(resp.getData(), 0, resp.getLength(), java.nio.charset.StandardCharsets.UTF_8);

        } catch (Exception e) {
            return "Error receiving response!";
        }
    }

    // do te implementohet te AdminClient dhe ReadOnlyClient
    public abstract void start();
}
