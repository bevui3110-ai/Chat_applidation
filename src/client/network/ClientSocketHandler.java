package client.network;

import common.MessagePacket;
import java.io.*;
import java.net.Socket;

public class ClientSocketHandler {
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;

    public ClientSocketHandler(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        oos = new ObjectOutputStream(socket.getOutputStream());
        ois = new ObjectInputStream(socket.getInputStream());
    }

    public void sendPacket(MessagePacket packet) throws IOException {
        oos.writeObject(packet);
        oos.flush();
    }

    public MessagePacket readPacket() throws IOException, ClassNotFoundException {
        return (MessagePacket) ois.readObject();
    }

    public void close() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }

    public Socket getSocket() {
        return socket;
    }

    public String getServerIp() {
        if (socket != null && socket.getInetAddress() != null) {
            String ip = socket.getInetAddress().getHostAddress();
            if ("0:0:0:0:0:0:0:1".equals(ip)) return "127.0.0.1";
            return ip;
        }
        return "127.0.0.1";
    }
}
