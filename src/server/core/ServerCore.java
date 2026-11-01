package server.core;

import common.MessagePacket;
import server.manager.GroupManager;
import server.manager.RoomManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.*;

public class ServerCore {
    private static int port = common.Constants.PORT;
    private static ServerSocket serverSocket;
    private static ThreadPoolExecutor pool;
    private static volatile boolean running = false;
    private static long startTime = 0;
    private static final Set<ClientSession> activeSessions = ConcurrentHashMap.newKeySet();
    private static Thread serverThread;

    public static int getPort() { return port; }
    public static void setPort(int newPort) { port = newPort; }
    public static boolean isRunning() { return running; }
    public static long getStartTime() { return startTime; }
    public static long getUptimeSeconds() {
        return running ? (System.currentTimeMillis() - startTime) / 1000 : 0;
    }

    public static Set<ClientSession> getActiveSessions() {
        return activeSessions;
    }

    public static ThreadPoolExecutor getThreadPool() {
        return pool;
    }

    public void startServer() {
        startServer(port);
    }

    public static synchronized void startServer(int serverPort) {
        if (running) return;
        port = serverPort;
        running = true;
        startTime = System.currentTimeMillis();

        GroupManager.initDatabase();
        pool = (ThreadPoolExecutor) Executors.newCachedThreadPool();

        serverThread = new Thread(() -> {
            ServerLogger.info("ServerSocket", "ServerSocket initialized on wildcard interface 0.0.0.0:" + port);
            try {
                serverSocket = new ServerSocket(port);
                while (running && !serverSocket.isClosed()) {
                    Socket socket = serverSocket.accept();
                    ClientSession session = new ClientSession(socket);
                    activeSessions.add(session);
                    pool.execute(session);
                }
            } catch (IOException e) {
                if (running) {
                    ServerLogger.error("ServerSocket", "L\u1ED7i ServerSocket: " + e.getMessage());
                }
            } finally {
                running = false;
                ServerLogger.info("ServerSocket", "ServerSocket đã d\u1EEBng.");
            }
        }, "Server-Acceptor-Thread");
        serverThread.start();
    }

    public static synchronized void stopServer() {
        if (!running) return;
        running = false;
        ServerLogger.info("Server", "Đang d\u1EEBng server...");

        for (ClientSession session : new ArrayList<>(activeSessions)) {
            session.closeSession();
        }
        activeSessions.clear();

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            ServerLogger.error("Server", "L\u1ED7i đóng ServerSocket: " + e.getMessage());
        }

        if (pool != null) {
            pool.shutdownNow();
        }

        ServerLogger.info("Server", "Server đã d\u1EEBng hoàn toàn.");
    }

    public static void kickClient(String identifier) {
        if (identifier == null) return;
        ClientSession session = RoomManager.getClient(identifier);
        if (session != null) {
            ServerLogger.disconnect(identifier, "Admin ng\u1EAFt k\u1EBFt n\u1ED1i (Kick) user '" + identifier + "'.");
            session.sendPacket(new MessagePacket("KICKED", "Server", identifier, "B\u1EA1n đã b\u1ECB qu\u1EA3n tr\u1ECB viên ng\u1EAFt k\u1EBFt n\u1ED1i kh\u1ECFi server."));
            session.closeSession();
            RoomManager.removeClient(identifier);
            broadcastUserList();
        } else {
            for (ClientSession s : new ArrayList<>(activeSessions)) {
                if (identifier.equals(s.getUsername()) || identifier.equals(s.getRemoteAddress()) || identifier.equals(s.getThreadName())) {
                    ServerLogger.disconnect(s.getThreadName(), "Admin ng\u1EAFt k\u1EBFt n\u1ED1i socket (" + s.getRemoteAddress() + ").");
                    s.closeSession();
                    activeSessions.remove(s);
                    break;
                }
            }
        }
    }

    public static void broadcastSystemMessage(String messageText) {
        if (messageText == null || messageText.trim().isEmpty()) return;
        String content = "[THÔNG BÁO H\u1EC6 TH\u1ED0NG] " + messageText.trim();
        MessagePacket packet = new MessagePacket("CHAT_ALL", "H\u1EC6 TH\u1ED0NG", "C\u1ED9ng đ\u1ED3ng", content);
        
        ClientSession.saveMessage("H\u1EC6 TH\u1ED0NG", "C\u1ED9ng đ\u1ED3ng", content, "CHAT_ALL", null);
        
        for (ClientSession client : RoomManager.getOnlineClients().values()) {
            client.sendPacket(packet);
        }
        
        ServerLogger.broadcast("System", "Đã phát thông báo h\u1EC7 th\u1ED1ng: " + messageText);
    }

    public static void broadcast(MessagePacket packet, String senderName) {
        for (ClientSession client : RoomManager.getOnlineClients().values()) {
            if (!client.getUsername().equals(senderName)) {
                client.sendPacket(packet);
            }
        }
    }

    public static void sendPrivate(MessagePacket packet) {
        if (GroupManager.isGroup(packet.getReceiver())) {
            sendGroupMessage(packet);
            return;
        }
        ClientSession target = RoomManager.getClient(packet.getReceiver());
        if (target != null) {
            target.sendPacket(packet);
        }
    }

    public static void sendToUser(String targetUser, MessagePacket packet) {
        ClientSession target = RoomManager.getClient(targetUser);
        if (target != null) {
            target.sendPacket(packet);
        }
    }

    public static void sendGroupMessage(MessagePacket packet) {
        Set<String> members = GroupManager.getGroupMembers(packet.getReceiver());
        for (String member : members) {
            if (!member.equals(packet.getSender())) {
                ClientSession session = RoomManager.getClient(member);
                if (session != null) {
                    session.sendPacket(packet);
                }
            }
        }
    }

    public static void broadcastUserList() {
        Set<String> allUsers = getAllRegisteredUsers();
        Set<String> onlineUsers = RoomManager.getOnlineClients().keySet();
        allUsers.addAll(onlineUsers); // Đ\u1EA3m b\u1EA3o gom đ\u1EE7 t\u1EA5t c\u1EA3 các user

        for (ClientSession client : RoomManager.getOnlineClients().values()) {
            String username = client.getUsername();
            if (username == null) continue;

            List<String> items = new ArrayList<>();
            for (String u : allUsers) {
                if (!u.equals(username)) {
                    boolean isOnline = onlineUsers.contains(u);
                    items.add(u + ":" + (isOnline ? "ONLINE" : "OFFLINE"));
                }
            }

            // Lấy danh sách các nhóm mà user này tham gia và danh sách thành viên trong nhóm
            List<String> userGroups = GroupManager.getUserGroups(username);
            for (String g : userGroups) {
                Set<String> members = GroupManager.getGroupMembers(g);
                String creator = GroupManager.getGroupCreator(g);
                items.add("[GROUP]" + g + ":" + creator + ":" + String.join(";", members));
            }

            String contentStr = String.join(",", items);
            MessagePacket packet = new MessagePacket("UPDATE_USER", "Server", username, contentStr);
            client.sendPacket(packet);
        }
    }

    public static int getRegisteredUsersCount() {
        return getAllRegisteredUsers().size();
    }

    public static class RegisteredUserInfo {
        private final int id;
        private final String username;
        private final String status;
        private final String ipAddress;

        public RegisteredUserInfo(int id, String username, String status, String ipAddress) {
            this.id = id;
            this.username = username;
            this.status = status;
            this.ipAddress = ipAddress;
        }

        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getStatus() { return status; }
        public String getIpAddress() { return ipAddress; }
    }

    public static List<RegisteredUserInfo> getRegisteredUsersList() {
        List<RegisteredUserInfo> list = new ArrayList<>();
        Set<String> onlineUsers = RoomManager.getOnlineClients().keySet();
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                String sql = "SELECT username FROM users ORDER BY username ASC";
                try (PreparedStatement pstmt = conn.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {
                    int index = 1;
                    while (rs.next()) {
                        String user = rs.getString("username");
                        boolean isOnline = onlineUsers.contains(user);
                        ClientSession session = isOnline ? RoomManager.getClient(user) : null;
                        String ip = session != null ? session.getRemoteAddress() : "Offline";
                        list.add(new RegisteredUserInfo(index++, user, isOnline ? "ONLINE" : "OFFLINE", ip));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static Set<String> getAllRegisteredUsers() {
        Set<String> users = new HashSet<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                String sql = "SELECT username FROM users";
                try (PreparedStatement pstmt = conn.prepareStatement(sql);
                     ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        users.add(rs.getString("username"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }
}