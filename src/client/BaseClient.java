package client;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public abstract class BaseClient {

    protected int clientId;
    protected InetAddress serverAddress;
    protected int serverPort = 9000;
    protected DatagramSocket socket;

    public BaseClient(int clientId) {
        try {
            this.clientId = clientId;
            this.socket = new DatagramSocket();
            this.serverAddress = InetAddress.getByName("127.0.0.1");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // e ndreq formatin e mesazhit që serveri e pret
    protected String buildPacket(String command, String payload) {
        return clientId + "|" + command + "|" + payload;
    }

    // e qon porosin ne server
    protected void sendPacket(String command, String payload) {
        try {
            String msg = buildPacket(command, payload);
            byte[] data = msg.getBytes();

            DatagramPacket packet = new DatagramPacket(
                    data, data.length, serverAddress, serverPort
            );

            socket.send(packet);

        } catch (Exception e) {
            System.out.println("Error sending packet!");
        }
    }

    // merr pergjigjen nga serveri
    protected String receiveResponse() {
        try {
            byte[] buffer = new byte[8192];
            DatagramPacket resp = new DatagramPacket(buffer, buffer.length);

            socket.receive(resp);
            return new String(resp.getData(), 0, resp.getLength());

        } catch (Exception e) {
            return "Error receiving response!";
        }
    }

    // do te implementohet te AdminClient dhe ReadOnlyClient
    public abstract void start();
}
