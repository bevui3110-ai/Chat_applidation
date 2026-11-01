package server.manager;

import server.core.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GroupManager {
    private static final Map<String, Set<String>> groupMap = new ConcurrentHashMap<>();
    private static final Map<String, String> groupCreatorMap = new ConcurrentHashMap<>();

    public static synchronized void initDatabase() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return;
            // 1. Tạo các bảng cơ bản nếu chưa tồn tại
            try (PreparedStatement pstmt1 = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS chat_groups (id INT AUTO_INCREMENT PRIMARY KEY, group_name VARCHAR(100) UNIQUE NOT NULL, created_by VARCHAR(50), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
                 PreparedStatement pstmt2 = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS group_members (id INT AUTO_INCREMENT PRIMARY KEY, group_name VARCHAR(100) NOT NULL, username VARCHAR(50) NOT NULL)");
                 PreparedStatement pstmt3 = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS messages (id INT AUTO_INCREMENT PRIMARY KEY, msg_id VARCHAR(50), sender VARCHAR(50), receiver VARCHAR(50), content TEXT, type VARCHAR(50), file_name VARCHAR(255), is_read BOOLEAN DEFAULT FALSE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
                 PreparedStatement pstmt4 = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS group_nicknames (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) NOT NULL, target_user VARCHAR(50) NOT NULL, nickname VARCHAR(100) NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, UNIQUE KEY unique_user_target (username, target_user))");
                 PreparedStatement pstmt5 = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS user_preferences (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) NOT NULL, target VARCHAR(50) NOT NULL, bg_color VARCHAR(20) DEFAULT NULL, bg_image VARCHAR(255) DEFAULT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, UNIQUE KEY unique_user_target_pref (username, target))");
                 PreparedStatement pstmt6 = conn.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS chat_settings (id INT AUTO_INCREMENT PRIMARY KEY, conv_id VARCHAR(100) NOT NULL UNIQUE, bg_color VARCHAR(20) DEFAULT NULL, bg_image VARCHAR(255) DEFAULT NULL, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)")) {
                pstmt1.executeUpdate();
                pstmt2.executeUpdate();
                pstmt3.executeUpdate();
                pstmt4.executeUpdate();
                pstmt5.executeUpdate();
                pstmt6.executeUpdate();
            }

            // 2. Tự động đồng bộ / nâng cấp các cột còn thiếu trong các bảng (Phần 3)
            String[] alterQueries = {
                "ALTER TABLE messages ADD COLUMN msg_id VARCHAR(50)",
                "ALTER TABLE messages ADD COLUMN is_read BOOLEAN DEFAULT FALSE",
                "ALTER TABLE users ADD COLUMN avatar VARCHAR(255) DEFAULT NULL",
                "ALTER TABLE users ADD COLUMN status VARCHAR(20) DEFAULT 'OFFLINE'",
                "ALTER TABLE users ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP",
                "ALTER TABLE chat_groups ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP",
                "ALTER TABLE group_members ADD UNIQUE KEY unique_group_member (group_name, username)"
            };

            for (String alterSql : alterQueries) {
                try (PreparedStatement pstmtAlter = conn.prepareStatement(alterSql)) {
                    pstmtAlter.executeUpdate();
                } catch (Exception ignored) {
                    // Cột hoặc index đã tồn tại thì bỏ qua an toàn
                }
            }

            loadGroupsFromDB();
            migrateLocalBackgroundsToDB();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void saveNickname(String username, String targetUser, String nickname) {
        if (username == null || targetUser == null) return;
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return;
            if (nickname == null || nickname.trim().isEmpty()) {
                String sql = "DELETE FROM group_nicknames WHERE username = ? AND target_user = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, username);
                    pstmt.setString(2, targetUser);
                    pstmt.executeUpdate();
                }
            } else {
                String sql = "INSERT INTO group_nicknames (username, target_user, nickname) VALUES (?, ?, ?) " +
                             "ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), updated_at = CURRENT_TIMESTAMP";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, username);
                    pstmt.setString(2, targetUser);
                    pstmt.setString(3, nickname.trim());
                    pstmt.executeUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized Map<String, String> getNicknamesForUser(String username) {
        Map<String, String> map = new HashMap<>();
        if (username == null) return map;
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return map;
            String sql = "SELECT target_user, nickname FROM group_nicknames WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        map.put(rs.getString("target_user"), rs.getString("nickname"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public static synchronized void saveGroupNickname(String groupName, String targetUser, String nickname) {
        saveNickname(groupName, targetUser, nickname);
    }

    public static synchronized Map<String, String> getNicknamesForGroup(String groupName) {
        Map<String, String> map = new HashMap<>();
        if (groupName == null) return map;
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return map;
            String sql = "SELECT target_user, nickname FROM group_nicknames WHERE username = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, groupName);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        map.put(rs.getString("target_user"), rs.getString("nickname"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public static synchronized void loadGroupsFromDB() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return;

            // Nạp người tạo nhóm từ bảng chat_groups
            String sqlGroups = "SELECT group_name, created_by FROM chat_groups";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlGroups);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String g = rs.getString("group_name");
                    String creator = rs.getString("created_by");
                    if (g != null) groupCreatorMap.put(g, creator != null ? creator : "");
                }
            }

            // Nạp thành viên nhóm từ bảng group_members
            String sql = "SELECT group_name, username FROM group_members";
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String group = rs.getString("group_name");
                    String user = rs.getString("username");
                    groupMap.computeIfAbsent(group, k -> Collections.synchronizedSet(new HashSet<>())).add(user);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void createGroup(String groupName, String creator, Set<String> members) {
        Set<String> allMembers = new HashSet<>(members);
        allMembers.add(creator);
        groupMap.put(groupName, allMembers);
        groupCreatorMap.put(groupName, creator != null ? creator : "");

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                String sqlGroup = "INSERT INTO chat_groups (group_name, created_by) VALUES (?, ?) ON DUPLICATE KEY UPDATE created_by=VALUES(created_by)";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlGroup)) {
                    pstmt.setString(1, groupName);
                    pstmt.setString(2, creator);
                    pstmt.executeUpdate();
                }

                String sqlMember = "INSERT INTO group_members (group_name, username) VALUES (?, ?)";
                for (String m : allMembers) {
                    try (PreparedStatement pstmt = conn.prepareStatement(sqlMember)) {
                        pstmt.setString(1, groupName);
                        pstmt.setString(2, m);
                        pstmt.executeUpdate();
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getGroupCreator(String groupName) {
        return groupCreatorMap.getOrDefault(groupName, "");
    }

    public static synchronized void deleteGroup(String groupName) {
        groupMap.remove(groupName);
        groupCreatorMap.remove(groupName);
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                try (PreparedStatement pstmt1 = conn.prepareStatement("DELETE FROM group_members WHERE group_name = ?");
                     PreparedStatement pstmt2 = conn.prepareStatement("DELETE FROM chat_groups WHERE group_name = ?")) {
                    pstmt1.setString(1, groupName);
                    pstmt1.executeUpdate();
                    pstmt2.setString(1, groupName);
                    pstmt2.executeUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isGroup(String name) {
        return groupMap.containsKey(name);
    }

    public static Map<String, Set<String>> getGroupMap() {
        return groupMap;
    }

    public static Set<String> getAllGroups() {
        return groupMap.keySet();
    }

    public static Set<String> getGroupMembers(String groupName) {
        return groupMap.getOrDefault(groupName, Collections.emptySet());
    }

    public static List<String> getUserGroups(String username) {
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : groupMap.entrySet()) {
            if (entry.getValue().contains(username)) {
                list.add(entry.getKey());
            }
        }
        return list;
    }

    public static synchronized void addMembersToGroup(String groupName, String[] newMembers) {
        Set<String> members = groupMap.computeIfAbsent(groupName, k -> Collections.synchronizedSet(new HashSet<>()));
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sqlMember = "INSERT INTO group_members (group_name, username) VALUES (?, ?)";
            for (String m : newMembers) {
                if (m != null && !m.trim().isEmpty()) {
                    String cleanUser = m.trim();
                    members.add(cleanUser);
                    if (conn != null) {
                        try (PreparedStatement pstmt = conn.prepareStatement(sqlMember)) {
                            pstmt.setString(1, groupName);
                            pstmt.setString(2, cleanUser);
                            pstmt.executeUpdate();
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized String removeMemberFromGroup(String groupName, String username) {
        String newLeader = null;
        Set<String> members = groupMap.get(groupName);
        if (members != null) {
            members.remove(username);
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                String sql = "DELETE FROM group_members WHERE group_name = ? AND username = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, groupName);
                    pstmt.setString(2, username);
                    pstmt.executeUpdate();
                }

                // Nếu người rời nhóm là Trưởng nhóm -> chuyển giao chức vụ hoặc giải tán
                String currentCreator = groupCreatorMap.get(groupName);
                if (username.equals(currentCreator)) {
                    if (members == null || members.isEmpty()) {
                        // Không còn ai trong nhóm -> giải tán nhóm luôn
                        deleteGroup(groupName);
                    } else {
                        // Chọn thành viên kế tiếp làm Trưởng nhóm mới
                        newLeader = members.iterator().next();
                        groupCreatorMap.put(groupName, newLeader);
                        String sqlUpdate = "UPDATE chat_groups SET created_by = ? WHERE group_name = ?";
                        try (PreparedStatement pstmtUp = conn.prepareStatement(sqlUpdate)) {
                            pstmtUp.setString(1, newLeader);
                            pstmtUp.setString(2, groupName);
                            pstmtUp.executeUpdate();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newLeader;
    }

    public static boolean isMemberOfGroup(String groupName, String username) {
        Set<String> members = groupMap.get(groupName);
        return members != null && members.contains(username);
    }

    public static String cleanGroupName(String name) {
        if (name == null) return "";
        if (name.startsWith("[GROUP]")) {
            return name.substring(7).split(":")[0].trim();
        }
        return name.trim();
    }

    public static String getCanonicalConvId(String user1, String user2) {
        if ("Cộng đồng".equals(user2) || "Cộng đồng".equals(user1)) {
            return "Cộng đồng";
        }
        String cleanTarget = cleanGroupName(user2);
        if (isGroup(cleanTarget)) {
            return cleanTarget;
        }
        String cleanU1 = cleanGroupName(user1);
        if (isGroup(cleanU1)) {
            return cleanU1;
        }
        if (user1.compareTo(user2) < 0) {
            return user1 + ":" + user2;
        } else {
            return user2 + ":" + user1;
        }
    }

    public static synchronized void saveChatBackground(String convId, String bgColor, String bgImage) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return;

            if ((bgColor == null || bgColor.isEmpty()) && (bgImage == null || bgImage.isEmpty())) {
                try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM chat_settings WHERE conv_id = ?")) {
                    pstmt.setString(1, convId);
                    pstmt.executeUpdate();
                } catch (Exception ignored) {}

                try {
                    if (convId.contains(":")) {
                        String[] u = convId.split(":");
                        try (PreparedStatement p = conn.prepareStatement("DELETE FROM user_preferences WHERE (username = ? AND target = ?) OR (username = ? AND target = ?)")) {
                            p.setString(1, u[0]);
                            p.setString(2, u[1]);
                            p.setString(3, u[1]);
                            p.setString(4, u[0]);
                            p.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement p = conn.prepareStatement("DELETE FROM user_preferences WHERE target = ?")) {
                            p.setString(1, convId);
                            p.executeUpdate();
                        }
                    }
                } catch (Exception ignored) {}
                return;
            }

            // 1. Lưu vào bảng chat_settings
            try {
                String query = "INSERT INTO chat_settings (conv_id, bg_color, bg_image) VALUES (?, ?, ?) " +
                               "ON DUPLICATE KEY UPDATE bg_color = VALUES(bg_color), bg_image = VALUES(bg_image), updated_at = CURRENT_TIMESTAMP";
                try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, convId);
                    pstmt.setString(2, (bgColor != null && !bgColor.isEmpty()) ? bgColor : null);
                    pstmt.setString(3, (bgImage != null && !bgImage.isEmpty()) ? bgImage : null);
                    pstmt.executeUpdate();
                }
            } catch (Exception ignored) {}

            // 2. Lưu vào bảng user_preferences để hiển thị đầy đủ trong phpMyAdmin
            try {
                String sqlPref = "INSERT INTO user_preferences (username, target, bg_color, bg_image) VALUES (?, ?, ?, ?) " +
                                 "ON DUPLICATE KEY UPDATE bg_color = VALUES(bg_color), bg_image = VALUES(bg_image), updated_at = CURRENT_TIMESTAMP";
                if (convId.contains(":")) {
                    String[] u = convId.split(":");
                    try (PreparedStatement p1 = conn.prepareStatement(sqlPref);
                         PreparedStatement p2 = conn.prepareStatement(sqlPref)) {
                        p1.setString(1, u[0]);
                        p1.setString(2, u[1]);
                        p1.setString(3, (bgColor != null && !bgColor.isEmpty()) ? bgColor : null);
                        p1.setString(4, (bgImage != null && !bgImage.isEmpty()) ? bgImage : null);
                        p1.executeUpdate();

                        p2.setString(1, u[1]);
                        p2.setString(2, u[0]);
                        p2.setString(3, (bgColor != null && !bgColor.isEmpty()) ? bgColor : null);
                        p2.setString(4, (bgImage != null && !bgImage.isEmpty()) ? bgImage : null);
                        p2.executeUpdate();
                    }
                } else {
                    try (PreparedStatement p = conn.prepareStatement(sqlPref)) {
                        p.setString(1, "ALL");
                        p.setString(2, convId);
                        p.setString(3, (bgColor != null && !bgColor.isEmpty()) ? bgColor : null);
                        p.setString(4, (bgImage != null && !bgImage.isEmpty()) ? bgImage : null);
                        p.executeUpdate();
                    }
                }
            } catch (Exception ignored) {}

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized Map<String, String[]> getAllChatBackgrounds() {
        Map<String, String[]> map = new HashMap<>();
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                // Ưu tiên đọc từ chat_settings
                try (PreparedStatement pstmt = conn.prepareStatement("SELECT conv_id, bg_color, bg_image FROM chat_settings");
                     ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String convId = rs.getString("conv_id");
                        String bgColor = rs.getString("bg_color");
                        String bgImage = rs.getString("bg_image");
                        map.put(convId, new String[]{ bgColor, bgImage });
                    }
                } catch (Exception ignored) {}

                // Nếu chat_settings chưa có, đọc từ user_preferences
                if (map.isEmpty()) {
                    try (PreparedStatement pstmt = conn.prepareStatement("SELECT username, target, bg_color, bg_image FROM user_preferences");
                         ResultSet rs = pstmt.executeQuery()) {
                        while (rs.next()) {
                            String u = rs.getString("username");
                            String t = rs.getString("target");
                            String bgColor = rs.getString("bg_color");
                            String bgImage = rs.getString("bg_image");
                            String convId = "ALL".equals(u) ? t : getCanonicalConvId(u, t);
                            if (!map.containsKey(convId)) {
                                map.put(convId, new String[]{ bgColor, bgImage });
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public static synchronized void migrateLocalBackgroundsToDB() {
        java.io.File prefsFile = new java.io.File("user_data/bg_prefs.properties");
        if (!prefsFile.exists()) return;

        java.util.Properties props = new java.util.Properties();
        try (java.io.FileInputStream fis = new java.io.FileInputStream(prefsFile)) {
            props.load(fis);
        } catch (Exception e) {
            return;
        }

        java.io.File serverDir = new java.io.File("server_storage");
        if (!serverDir.exists()) serverDir.mkdirs();

        for (String key : props.stringPropertyNames()) {
            String val = props.getProperty(key);
            if (val == null || val.trim().isEmpty()) continue;

            if (key.endsWith("_image")) {
                String prefix = key.substring(0, key.length() - 6);
                String convId;
                if (prefix.startsWith("CONV_")) {
                    convId = prefix.substring(5);
                } else if (prefix.contains("_")) {
                    String[] parts = prefix.split("_", 2);
                    convId = getCanonicalConvId(parts[0], parts[1]);
                } else {
                    convId = prefix;
                }

                java.io.File imgFile = new java.io.File(val);
                if (imgFile.exists()) {
                    try {
                        String safeConv = convId.replaceAll("[^a-zA-Z0-9_]", "_");
                        String serverFileName = "bg_" + safeConv + ".jpg";
                        java.io.File destFile = new java.io.File(serverDir, serverFileName);
                        if (!destFile.exists() || destFile.length() == 0) {
                            java.nio.file.Files.copy(imgFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }
                        saveChatBackground(convId, null, serverFileName);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            } else if (key.endsWith("_color")) {
                String prefix = key.substring(0, key.length() - 6);
                String convId;
                if (prefix.startsWith("CONV_")) {
                    convId = prefix.substring(5);
                } else if (prefix.contains("_")) {
                    String[] parts = prefix.split("_", 2);
                    convId = getCanonicalConvId(parts[0], parts[1]);
                } else {
                    convId = prefix;
                }
                saveChatBackground(convId, val, null);
            }
        }
    }
}
