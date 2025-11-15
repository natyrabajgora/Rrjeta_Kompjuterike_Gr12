package server;

import java.net.SocketAddress;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class ClientSession {

    // ======================
    //  ROLI I KLIENTIT
    // ======================
    public enum Permission {
        ADMIN,
        READ_ONLY
    }

    // ======================
    //  ATRIBUTET
    // ======================
    private final SocketAddress address;     // IP + PORT i klientit
    private String clientId;                 // Vendoset me HELLO clientX ADMIN/READ
    private Permission permission;           // ADMIN ose READ_ONLY

    private volatile long lastActive;        // Timestamp i mesazhit të fundit

    // Statistika për STATS
    private final AtomicLong messagesCount;  // numri i mesazheve të pranuara
    private final AtomicLong bytesReceived;  // bytes që ka dërgu klienti
    private final AtomicLong bytesSent;      // bytes që i ka kthyer serveri

    // ======================
    //  KONSTRUKTORI
    // ======================
    public ClientSession(SocketAddress address) {
        this.address = address;
        this.clientId = "UNKNOWN"; // derisa të vijë HELLO
        this.permission = Permission.READ_ONLY;

        this.lastActive = System.currentTimeMillis();

        this.messagesCount = new AtomicLong(0);
        this.bytesReceived = new AtomicLong(0);
        this.bytesSent = new AtomicLong(0);
    }

    // ======================
    //  GETTERS
    // ======================
    public SocketAddress getAddress() {
        return address;
    }

    public String getClientId() {
        return clientId;
    }

    public Permission getPermission() {
        return permission;
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

    // ======================
    //  SETTERS
    // ======================
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    // ======================
    //  AKTIVITETI I KLIENTIT
    // ======================
    public void touch() {
        this.lastActive = System.currentTimeMillis(); // për timeout
    }

    public void incrementMessages() {
        messagesCount.incrementAndGet();
    }

    public void addBytesReceived(long bytes) {
        bytesReceived.addAndGet(bytes);
    }

    public void addBytesSent(long bytes) {
        bytesSent.addAndGet(bytes);
    }

    // ======================
    //  DEBUG / PRINTIM
    // ======================
    @Override
    public String toString() {
        return "\nClientSession {" +
                "\n  Address        = " + address +
                "\n  Client ID      = " + clientId +
                "\n  Permission     = " + permission +
                "\n  Last Active    = " + Instant.ofEpochMilli(lastActive) +
                "\n  Messages Count = " + messagesCount.get() +
                "\n  Bytes Received = " + bytesReceived.get() +
                "\n  Bytes Sent     = " + bytesSent.get() +
                "\n}";

    }
}
