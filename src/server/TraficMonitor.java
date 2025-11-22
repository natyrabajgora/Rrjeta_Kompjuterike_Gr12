package server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class TraficMonitor {

    public static class Constants {
        public static final String LOGS_DIR = "logs";
        public static final String STATS_LOG_FILE = LOGS_DIR + "/server_stats.txt";
    }

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

    public String buildStats(Map<?, ClientSession> sessions) {
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
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(Constants.STATS_LOG_FILE, true))) {

            writer.write(stats);
            writer.write("\n");
        } catch (IOException e) {
            System.err.println("Failed to write stats: " + e.getMessage());
        }
    }

    private void ensureLogsDir() {
        File dir = new File(Constants.LOGS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}
