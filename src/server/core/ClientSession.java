package server.core;

import common.MessagePacket;
import server.manager.GroupManager;
import server.manager.RoomManager;
import java.io.*;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class ClientSession implements Runnable {
    private Socket socket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private String username;
    private String threadName = "Thread-1";
    private String clientState = "Idle";

    public ClientSession(Socket socket) {
        this.socket = socket;
    }

    public String getUsername() { return username; }
    public Socket getSocket() { return socket; }
    public String getThreadName() { return threadName; }
    public String getClientState() { return clientState; }
    public void setClientState(String state) { this.clientState = state; }

    public String getRemoteAddress() {
        try {
            if (socket != null && socket.getRemoteSocketAddress() != null) {
                return socket.getRemoteSocketAddress().toString().replace("/", "");
            }
        } catch (Exception ignored) {}
        return "127.0.0.1:0";
    }

    public void closeSession() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }

    @Override
    public void run() {
        this.threadName = Thread.currentThread().getName();
        ServerLogger.info(threadName, "Inbound socket accepted from " + getRemoteAddress());
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());

            while (true) {
                MessagePacket packet = (MessagePacket) ois.readObject();
                if (packet == null) break;

                this.clientState = "Transmitting";

                if ("REGISTER".equals(packet.getType())) {
                    boolean success = handleRegister(packet.getSender(), packet.getContent());
                    if (success) {
                        sendPacket(new MessagePacket("REGISTER_SUCCESS", "Server", "", "Đăng ký thành công! Hãy đăng nh\u1EADp."));
                        ServerLogger.auth(threadName, "Đăng ký tài kho\u1EA3n m\u1EDBi thành công: '" + packet.getSender() + "'");
                    } else {
                        sendPacket(new MessagePacket("REGISTER_FAIL", "Server", "", "Tài kho\u1EA3n đã t\u1ED3n t\u1EA1i ho\u1EB7c l\u1ED7i cơ s\u1EDF d\u1EEF li\u1EC7u!"));
                        ServerLogger.auth(threadName, "Đăng ký th\u1EA5t b\u1EA1i cho tài kho\u1EA3n: '" + packet.getSender() + "'");
                    }
                } else if ("LOGIN".equals(packet.getType())) {
                    int loginResult = handleLogin(packet.getSender(), packet.getContent());
                    if (loginResult == 1) {
                        this.username = packet.getSender();
                        RoomManager.addClient(this.username, this);
                        sendPacket(new MessagePacket("LOGIN_SUCCESS", "Server", "", "Đăng nh\u1EADp thành công!"));
                        ServerCore.broadcastUserList();
                        
                        // T\u1EA3i và g\u1EEDi toàn b\u1ED9 l\u1ECBch s\u1EED chat v\u1EC1 cho client ngay khi đăng nh\u1EADp thành công
                        sendChatHistory();
                        // Tai danh sach biet danh tu MySQL (group_nicknames) gui ve cho client
                        java.util.Map<String, String> userNicknames = GroupManager.getNicknamesForUser(this.username);
                        if (!userNicknames.isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            for (java.util.Map.Entry<String, String> entry : userNicknames.entrySet()) {
                                if (sb.length() > 0) sb.append(";");
                                sb.append(entry.getKey()).append("=").append(entry.getValue());
                            }
                            sendPacket(new MessagePacket("NICKNAME_LIST", "Server", this.username, sb.toString()));
                        }
                        // Tai danh sach biet danh nhom ma user tham gia
                        List<String> userGroups = GroupManager.getUserGroups(this.username);
                        for (String g : userGroups) {
                            Map<String, String> grpNicknames = GroupManager.getNicknamesForGroup(g);
                            for (Map.Entry<String, String> entry : grpNicknames.entrySet()) {
                                sendPacket(new MessagePacket("GROUP_NICKNAME_UPDATE", g, entry.getKey(), entry.getValue()));
                            }
                        }
                        // Dong bo anh nen va mau nen cac cuoc tro chuyen da luu tu MySQL (chat_settings)
                        java.util.Map<String, String[]> allBgs = GroupManager.getAllChatBackgrounds();
                        for (java.util.Map.Entry<String, String[]> bgEntry : allBgs.entrySet()) {
                            String convId = bgEntry.getKey();
                            String[] vals = bgEntry.getValue();
                            String bgColor = vals[0];
                            String bgImage = vals[1];

                            boolean isRelevant = false;
                            if ("Cộng đồng".equals(convId)) {
                                isRelevant = true;
                            } else if (GroupManager.isGroup(convId)) {
                                isRelevant = GroupManager.isMemberOfGroup(convId, this.username);
                            } else if (convId.contains(":")) {
                                String[] users = convId.split(":");
                                isRelevant = users[0].equals(this.username) || users[1].equals(this.username);
                            }

                            if (isRelevant) {
                                MessagePacket bgPacket = new MessagePacket("BG_SYNC", "Server", this.username, convId + "|" + (bgColor != null ? bgColor : ""));
                                bgPacket.setFileName(bgImage);
                                if (bgImage != null && !bgImage.isEmpty()) {
                                    bgPacket.setFileData(readServerFile(bgImage));
                                }
                                sendPacket(bgPacket);
                            }
                        }
                        ServerLogger.auth(threadName, "Client '" + username + "' đã đăng nh\u1EADp thành công t\u1EEB " + getRemoteAddress());
                    } else if (loginResult == 0) {
                        sendPacket(new MessagePacket("LOGIN_FAIL", "Server", "", "Sai m\u1EADt kh\u1EA9u!"));
                        ServerLogger.auth(threadName, "Đăng nh\u1EADp th\u1EA5t b\u1EA1i (sai mk) cho user: '" + packet.getSender() + "'");
                    } else {
                        sendPacket(new MessagePacket("LOGIN_FAIL", "Server", "", "Tài kho\u1EA3n không t\u1ED3n t\u1EA1i!"));
                        ServerLogger.auth(threadName, "Đăng nh\u1EADp th\u1EA5t b\u1EA1i (tài kho\u1EA3n không t\u1ED3n t\u1EA1i): '" + packet.getSender() + "'");
                    }
                } else if ("CHAT_ALL".equals(packet.getType())) {
                    saveMessage(username, "Cộng đồng", packet.getContent(), "CHAT_ALL", null, packet.getMsgId());
                    ServerCore.broadcast(packet, username);
                    ServerLogger.msg(threadName, "Broadcasting [" + username + " -> Cộng đồng]: " + packet.getContent());
                } else if ("CHAT_PRIVATE".equals(packet.getType())) {
                    saveMessage(username, packet.getReceiver(), packet.getContent(), "CHAT_PRIVATE", null, packet.getMsgId());
                    ServerCore.sendPrivate(packet);
                    ServerLogger.msg(threadName, "Private msg [" + username + " -> " + packet.getReceiver() + "]: " + packet.getContent());
                } else if ("STICKER".equals(packet.getType())) {
                    String target = (packet.getReceiver() == null || packet.getReceiver().isEmpty()) ? "Cộng đồng" : packet.getReceiver();
                    saveMessage(username, target, packet.getContent(), "STICKER", packet.getFileName(), packet.getMsgId());
                    if (packet.getFileData() != null && packet.getFileData().length > 0) {
                        saveServerFile(packet.getFileName(), packet.getFileData());
                    }
                    if ("Cộng đồng".equals(target)) {
                        ServerCore.broadcast(packet, username);
                    } else {
                        ServerCore.sendPrivate(packet);
                    }
                    ServerLogger.msg(threadName, "Sticker [" + username + " -> " + target + "]: " + packet.getFileName());
                } else if ("RECALL_MSG".equals(packet.getType())) {
                    String msgIdToRecall = packet.getContent();
                    recallMessageInDB(msgIdToRecall);
                    String target = packet.getReceiver();
                    if (target == null || target.isEmpty() || "Cộng đồng".equals(target)) {
                        ServerCore.broadcast(packet, username);
                    } else {
                        ServerCore.sendPrivate(packet);
                    }
                    ServerLogger.info(threadName, "Recalled message ID " + msgIdToRecall + " by " + username);
                } else if ("SET_NICKNAME".equals(packet.getType())) {
                    String targetUser = packet.getReceiver();
                    String newNickname = packet.getContent();
                    GroupManager.saveNickname(username, targetUser, newNickname);
                    ServerLogger.info(threadName, username + " da dat biet danh cho " + targetUser + ": '" + newNickname + "'");
                } else if ("SET_GROUP_NICKNAME".equals(packet.getType())) {
                    String groupName = packet.getSender();
                    String targetUser = packet.getReceiver();
                    String newNickname = packet.getContent();
                    GroupManager.saveGroupNickname(groupName, targetUser, newNickname);
                    MessagePacket syncPkt = new MessagePacket("GROUP_NICKNAME_UPDATE", groupName, targetUser, newNickname);
                    Set<String> members = GroupManager.getGroupMembers(groupName);
                    for (String m : members) {
                        ServerCore.sendToUser(m, syncPkt);
                    }
                    ServerLogger.info(threadName, username + " da dat biet danh nhom [" + groupName + "] cho " + targetUser + ": '" + newNickname + "'");
                } else if ("SET_BG".equals(packet.getType())) {
                    String sender = packet.getSender();
                    String target = packet.getReceiver();
                    String convId = GroupManager.getCanonicalConvId(sender, target);
                    String content = packet.getContent(); // hexColor, or "RESET", or ""
                    String fileName = packet.getFileName();
                    byte[] fileData = packet.getFileData();

                    String bgColor = null;
                    String bgImage = null;

                    if ("RESET".equals(content)) {
                        GroupManager.saveChatBackground(convId, null, null);
                    } else if (fileData != null && fileData.length > 0 && fileName != null && !fileName.isEmpty()) {
                        saveServerFile(fileName, fileData);
                        bgImage = fileName;
                        GroupManager.saveChatBackground(convId, null, bgImage);
                    } else if (content != null && content.startsWith("#")) {
                        bgColor = content;
                        GroupManager.saveChatBackground(convId, bgColor, null);
                    }

                    MessagePacket forwardPacket = new MessagePacket("SET_BG", sender, target, convId + "|" + (bgColor != null ? bgColor : ( "RESET".equals(content) ? "RESET" : "" )));
                    forwardPacket.setFileName(bgImage);
                    forwardPacket.setFileData(fileData);

                    if ("Cộng đồng".equals(convId)) {
                        ServerCore.broadcast(forwardPacket, "");
                    } else if (GroupManager.isGroup(convId)) {
                        Set<String> members = GroupManager.getGroupMembers(convId);
                        for (String m : members) {
                            ServerCore.sendToUser(m, forwardPacket);
                        }
                    } else {
                        ServerCore.sendToUser(target, forwardPacket);
                        ServerCore.sendToUser(sender, forwardPacket);
                    }
                    ServerLogger.info(threadName, username + " da cap nhat nen cho cuoc tro chuyen '" + convId + "'");
                } else if ("CREATE_GROUP".equals(packet.getType())) {
                    String content = packet.getContent();
                    if (content != null && content.contains(":")) {
                        String[] parts = content.split(":", 2);
                        String groupName = parts[0];
                        String[] membersArray = parts[1].split(",");
                        Set<String> members = new HashSet<>(Arrays.asList(membersArray));
                        GroupManager.createGroup(groupName, username, members);
                        ServerCore.broadcastUserList();
                        ServerLogger.info(threadName, "T\u1EA1o nhóm chat m\u1EDBi '" + groupName + "' b\u1EDFi " + username);
                    }
                } else if ("ADD_GROUP_MEMBER".equals(packet.getType())) {
                    String content = packet.getContent();
                    if (content != null && content.contains(":")) {
                        String[] parts = content.split(":", 2);
                        String groupName = parts[0];
                        String[] newMembers = parts[1].split(",");
                        GroupManager.addMembersToGroup(groupName, newMembers);
                        ServerCore.broadcastUserList();

                        for (String m : newMembers) {
                            if (m != null && !m.trim().isEmpty()) {
                                ClientSession session = RoomManager.getClient(m.trim());
                                if (session != null) {
                                    session.sendChatHistory();
                                }
                            }
                        }
                        ServerLogger.info(threadName, "Thêm thành viên vào nhóm '" + groupName + "'");
                    }
                } else if ("EDIT_MSG".equals(packet.getType())) {
                    String msgIdToEdit = packet.getMsgId();
                    String newContent = packet.getContent();
                    editMessageInDB(msgIdToEdit, newContent);
                    String target = packet.getReceiver();
                    if (target == null || target.isEmpty() || "Cộng đồng".equals(target) || GroupManager.isGroup(target)) {
                        ServerCore.broadcast(packet, username);
                    } else {
                        ServerCore.sendPrivate(packet);
                    }
                    ServerLogger.info(threadName, username + " da chinh sua tin nhan ID " + msgIdToEdit);
                } else if ("DELETE_GROUP".equals(packet.getType())) {
                    String groupName = packet.getContent();
                    if (groupName != null && !groupName.trim().isEmpty()) {
                        String cleanGroup = groupName.trim();
                        String creator = GroupManager.getGroupCreator(cleanGroup);
                        if (username.equals(creator)) {
                            GroupManager.deleteGroup(cleanGroup);
                            ServerCore.broadcastUserList();
                            MessagePacket notify = new MessagePacket("CHAT_ALL", "Server", cleanGroup, "📢 Nhóm '" + cleanGroup + "' đã bị giải tán bởi Trưởng nhóm " + username);
                            ServerCore.broadcast(notify, null);
                            ServerLogger.info(threadName, username + " (Truong nhom) da xoa nhom '" + cleanGroup + "'");
                        }
                    }
                } else if ("LEAVE_GROUP".equals(packet.getType())) {
                    String groupName = packet.getContent();
                    if (groupName != null && !groupName.trim().isEmpty()) {
                        String cleanGroup = groupName.trim();
                        String newLeader = GroupManager.removeMemberFromGroup(cleanGroup, username);
                        ServerCore.broadcastUserList();
                        if (newLeader != null && !newLeader.isEmpty()) {
                            MessagePacket notify = new MessagePacket("CHAT_ALL", "Server", cleanGroup, "📢 " + username + " đã rời nhóm. " + newLeader + " hiện là Trưởng nhóm mới.");
                            ServerCore.broadcast(notify, null);
                        }
                        ServerLogger.info(threadName, username + " da roi nhom '" + cleanGroup + "'");
                    }
                } else if ("CHANGE_PASSWORD".equals(packet.getType())) {
                    String content = packet.getContent();
                    if (content != null && content.contains(":")) {
                        String[] parts = content.split(":", 2);
                        String currentPass = parts[0];
                        String newPass = parts[1];

                        int checkResult = handleLogin(username, currentPass);
                        if (checkResult == 1) {
                            boolean updated = updatePasswordInDB(username, newPass);
                            if (updated) {
                                sendPacket(new MessagePacket("CHANGE_PASSWORD_SUCCESS", "Server", username, "Đ\u1ED5i m\u1EADt kh\u1EA9u thành công!"));
                                ServerLogger.auth(threadName, username + " đã đ\u1ED5i m\u1EADt kh\u1EA9u thành công.");
                            } else {
                                sendPacket(new MessagePacket("CHANGE_PASSWORD_FAIL", "Server", username, "L\u1ED7i c\u1EADp nh\u1EADt CSDL m\u1EADt kh\u1EA9u!"));
                            }
                        } else {
                            sendPacket(new MessagePacket("CHANGE_PASSWORD_FAIL", "Server", username, "M\u1EADt kh\u1EA9u hi\u1EC7n t\u1EA1i không đúng!"));
                        }
                    }
                } else if ("CALL_REQUEST".equals(packet.getType()) || "CALL_ACCEPT".equals(packet.getType()) ||
                           "CALL_REJECT".equals(packet.getType()) || "CALL_END".equals(packet.getType())) {
                    if (("CALL_REQUEST".equals(packet.getType()) || "CALL_ACCEPT".equals(packet.getType())) && packet.getContent() != null) {
                        String senderIp = socket.getInetAddress().getHostAddress();
                        if ("0:0:0:0:0:0:0:1".equals(senderIp)) senderIp = "127.0.0.1";
                        String currentContent = packet.getContent();
                        String[] parts = currentContent.split(":");
                        if (parts.length == 2) {
                            packet = new MessagePacket(packet.getType(), packet.getSender(), packet.getReceiver(), currentContent + ":" + senderIp, packet.getMsgId());
                        }
                    }
                    ServerCore.sendPrivate(packet);
                    ServerLogger.info(threadName, "Call packet " + packet.getType() + " t\u1EEB " + username + " t\u1EDBi " + packet.getReceiver());
                } else if ("FILE".equals(packet.getType()) || "IMAGE".equals(packet.getType())) {
                    String target = (packet.getReceiver() == null || packet.getReceiver().isEmpty()) ? "C\u1ED9ng đ\u1ED3ng" : packet.getReceiver();
                    saveMessage(username, target, packet.getContent(), packet.getType(), packet.getFileName(), packet.getMsgId());
                    saveServerFile(packet.getFileName(), packet.getFileData());
                    
                    if (target.equals("C\u1ED9ng đ\u1ED3ng")) {
                        ServerCore.broadcast(packet, username);
                    } else {
                        ServerCore.sendPrivate(packet);
                    }
                    ServerLogger.file(threadName, "File/Image '" + packet.getFileName() + "' g\u1EEDi b\u1EDFi " + username + " t\u1EDBi " + target);
                }
                
                this.clientState = "Idle";
            }
        } catch (Exception e) {
            ServerLogger.disconnect(threadName, "Client connection reset / exception: " + (username != null ? username : getRemoteAddress()));
        } finally {
            ServerCore.getActiveSessions().remove(this);
            if (username != null) {
                RoomManager.removeClient(username);
                ServerCore.broadcastUserList();
                ServerLogger.disconnect(threadName, "Socket closed by client '" + username + "' (" + getRemoteAddress() + "). Worker thread recycled.");
            } else {
                ServerLogger.disconnect(threadName, "Unauthenticated socket closed (" + getRemoteAddress() + ").");
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private static synchronized void saveServerFile(String fileName, byte[] data) {
        if (fileName == null || data == null || data.length == 0) return;
        try {
            File dir = new File("server_storage");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static synchronized byte[] readServerFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) return null;
        try {
            File file = new File("server_storage", fileName);
            if (file.exists()) {
                return java.nio.file.Files.readAllBytes(file.toPath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private synchronized boolean handleRegister(String user, String pass) {
        String query = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, user);
            pstmt.setString(2, pass);
            pstmt.executeUpdate();
            return true;
        } catch (Exception e) {
            return false; // Trùng tên tài kho\u1EA3n ho\u1EB7c l\u1ED7i DB
        }
    }

    private synchronized int handleLogin(String user, String pass) {
        String query = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, user);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String dbPassword = rs.getString("password");
                    if (dbPassword.equals(pass)) {
                        return 1; // Thành công
                    } else {
                        return 0; // Sai m\u1EADt kh\u1EA9u
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1; // Không t\u1ED3n t\u1EA1i user
    }

    public static synchronized void saveMessage(String sender, String receiver, String content, String type, String fileName) {
        saveMessage(sender, receiver, content, type, fileName, null);
    }

    public static synchronized void saveMessage(String sender, String receiver, String content, String type, String fileName, String msgId) {
        String query = "INSERT INTO messages (msg_id, sender, receiver, content, type, file_name) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, msgId);
            pstmt.setString(2, sender);
            pstmt.setString(3, receiver);
            pstmt.setString(4, content);
            pstmt.setString(5, type);
            pstmt.setString(6, fileName);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void recallMessageInDB(String msgId) {
        if (msgId == null || msgId.isEmpty()) return;
        String query = "UPDATE messages SET content = '*Tin nhắn đã được thu hồi*', type = 'RECALLED' WHERE msg_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, msgId);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void editMessageInDB(String msgId, String newContent) {
        if (msgId == null || msgId.isEmpty()) return;
        String query = "UPDATE messages SET content = ? WHERE msg_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, newContent);
            pstmt.setString(2, msgId);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendChatHistory() {
        List<String> userGroups = GroupManager.getUserGroups(this.username);
        StringBuilder sql = new StringBuilder("SELECT * FROM messages WHERE receiver = 'C\u1ED9ng đ\u1ED3ng' OR sender = ? OR receiver = ?");
        for (int i = 0; i < userGroups.size(); i++) {
            sql.append(" OR receiver = ?");
        }
        sql.append(" ORDER BY created_at ASC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            pstmt.setString(1, this.username);
            pstmt.setString(2, this.username);
            for (int i = 0; i < userGroups.size(); i++) {
                pstmt.setString(3 + i, userGroups.get(i));
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String sender = rs.getString("sender");
                    String receiver = rs.getString("receiver");
                    String content = rs.getString("content");
                    String type = rs.getString("type");
                    String fileName = rs.getString("file_name");
                    String dbTime = rs.getString("created_at");
                    String msgId = null;
                    try {
                        msgId = rs.getString("msg_id");
                    } catch (Exception ignored) {}

                    MessagePacket historyPacket = new MessagePacket(type, sender, receiver, content, msgId);
                    historyPacket.setFileName(fileName);
                    
                    if ("IMAGE".equals(type) || "FILE".equals(type) || "STICKER".equals(type)) {
                        byte[] fileData = readServerFile(fileName);
                        historyPacket.setFileData(fileData);
                    }

                    historyPacket.setTimestamp(dbTime);
                    
                    if (!"C\u1ED9ng đ\u1ED3ng".equals(receiver) && !GroupManager.isGroup(receiver)) {
                        if (sender.equals(this.username)) {
                            historyPacket.setReceiver(receiver);
                        } else {
                            historyPacket.setReceiver(sender);
                        }
                    }

                    sendPacket(historyPacket);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized boolean updatePasswordInDB(String user, String newPass) {
        String query = "UPDATE users SET password = ? WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, newPass);
            pstmt.setString(2, user);
            pstmt.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void sendPacket(MessagePacket packet) {
        try {
            oos.writeObject(packet);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
