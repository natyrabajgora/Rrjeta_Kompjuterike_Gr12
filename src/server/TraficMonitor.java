package server;

import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;

public class TraficMonitor {

    private final HashMap<InetAddress, Integer> messageCount = new HashMap<>();
    private final HashMap<InetAddress, Long> trafficBytes = new HashMap<>();
    private final HashSet<InetAddress> activeClients = new HashSet<>();

    private final String logFile = "logs/server_stats.txt";


    public synchronized void registerPacket(InetAddress ip, int packetSize) {
        activeClients.add(ip);
        messageCount.put(ip, messageCount.getOrDefault(ip, 0) + 1);
        trafficBytes.put(ip, trafficBytes.getOrDefault(ip, 0L) + packetSize);
    }


    public synchronized void removeClient(InetAddress ip) {
        activeClients.remove(ip);
    }

    public synchronized String generateStats() {
        StringBuilder sb = new StringBuilder();
        sb.append("------ SERVER STATS ------\n");
        sb.append("Active Clients: ").append(activeClients.size()).append("\n\n");

        long totalTraffic = 0;

        for (InetAddress ip : activeClients) {
            int msg = messageCount.getOrDefault(ip, 0);
            long bytes = trafficBytes.getOrDefault(ip, 0L);
            totalTraffic += bytes;

            sb.append("Client: ").append(ip.getHostAddress()).append("\n");
            sb.append("   Messages: ").append(msg).append("\n");
            sb.append("   Traffic: ").append(bytes).append(" bytes\n\n");
        }

        sb.append("Total Traffic: ").append(totalTraffic).append(" bytes\n");
        sb.append("----------------------------\n");

        return sb.toString();
    }


    public synchronized void writeStatsToFile() {
        try (FileWriter writer = new FileWriter(logFile)) {
            writer.write(generateStats());
        } catch (IOException e) {
            System.out.println("Error writing stats file: " + e.getMessage());
        }
    }


    public void startLoggingThread(int intervalSeconds) {
        new Thread(() -> {
            while (true) {
                writeStatsToFile();
                try {
                    Thread.sleep(intervalSeconds * 1000L);
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }
}
