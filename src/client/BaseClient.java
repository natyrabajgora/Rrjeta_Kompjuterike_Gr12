package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public abstract class BaseClient {

    protected final int clientId;
    protected final String clientIdentifier;
    protected InetAddress serverAddress;
    protected int serverPort = 5000;
    protected DatagramSocket socket;

    public BaseClient(int clientId) {
        this.clientId = clientId;
        this.clientIdentifier = "client" + clientId;
        try {
            this.socket = new DatagramSocket();
            this.serverAddress = InetAddress.getByName("127.0.0.1");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // e ndreq formatin e mesazhit që serveri e pret
    protected String getClientIdentifier(){
        return clientIdentifier;
    }

    // e qon porosin ne server
    protected void sendMessage(String message) {
        try {

            byte[] data = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            DatagramPacket packet = new DatagramPacket(
                    data, data.length, serverAddress, serverPort
            );

            socket.send(packet);

        } catch (Exception e) {
            System.out.println("Error sending packet!");
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
