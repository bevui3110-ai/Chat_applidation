package client.gui;

import client.network.ClientSocketHandler;
import common.MessagePacket;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class NicknameDialog {

    public interface NicknameCallback {
        void onNicknameChanged(String targetUser, String newNickname);
    }

    private static final String NICKNAMES_FILE = "user_data/nicknames.properties";
    private static ClientSocketHandler socketHandler;

    // Phân tách hoàn toàn: Bộ nhớ biệt danh riêng tư 1-1 vs Biệt danh nhóm
    private static final Map<String, String> memoryPrivateNicknames = new ConcurrentHashMap<>();
    private static final Map<String, String> memoryGroupNicknames = new ConcurrentHashMap<>();

    public static void setSocketHandler(ClientSocketHandler handler) {
        socketHandler = handler;
    }

    public static void loadNicknamesFromData(String data) {
        if (data == null || data.trim().isEmpty()) return;
        String[] pairs = data.split(";");
        for (String pair : pairs) {
            if (pair.contains("=")) {
                String[] kv = pair.split("=", 2);
                String target = kv[0].trim();
                String nick = kv[1].trim();
                if (!target.isEmpty()) {
                    if (target.contains("_")) {
                        memoryGroupNicknames.put(target, nick);
                    } else {
                        memoryPrivateNicknames.put(target, nick);
                    }
                }
            }
        }
    }

    public static void setGroupNickname(String groupName, String targetUser, String nickname) {
        if (groupName == null || targetUser == null) return;
        String key = groupName + "_" + targetUser;
        if (nickname == null || nickname.trim().isEmpty()) {
            memoryGroupNicknames.remove(key);
        } else {
            memoryGroupNicknames.put(key, nickname.trim());
        }
    }

    // ==========================================
    // 1. BIỆT DANH TRONG NHÓM (GROUP NICKNAME)
    // ==========================================
    public static String getGroupNickname(String groupName, String targetUser) {
        if (groupName == null || targetUser == null || targetUser.isEmpty()) return "";
        String key = groupName + "_" + targetUser;
        if (memoryGroupNicknames.containsKey(key)) {
            return memoryGroupNicknames.get(key);
        }
        Properties props = loadNicknames();
        return props.getProperty(key, "");
    }

    public static String getGroupDisplayName(String groupName, String targetUser) {
        String nick = getGroupNickname(groupName, targetUser);
        if (nick != null && !nick.trim().isEmpty()) {
            return nick.trim();
        }
        return targetUser;
    }

    public static void showGroupNicknameDialog(Component parent, String groupName, String targetUser, NicknameCallback callback) {
        Window parentWindow = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(parentWindow, "Đặt biệt danh trong nhóm", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(420, 220);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));
        panel.setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("Đặt biệt danh cho @" + targetUser + " trong nhóm " + groupName);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(new Color(124, 58, 237));

        String currentNickname = getGroupNickname(groupName, targetUser);

        JTextField txtNickname = new JTextField(currentNickname);
        txtNickname.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtNickname.setBackground(new Color(248, 250, 252));
        txtNickname.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        txtNickname.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnReset = createButton("Gỡ biệt danh", new Color(241, 245, 249), new Color(226, 232, 240), new Color(15, 23, 42));
        JButton btnSave = createButton("Lưu biệt danh", new Color(124, 58, 237), new Color(109, 40, 217), Color.WHITE);

        btnReset.addActionListener(e -> {
            saveGroupNickname(groupName, targetUser, "");
            JOptionPane.showMessageDialog(dialog, "Đã xóa biệt danh nhóm!");
            if (callback != null) callback.onNicknameChanged(targetUser, "");
            dialog.dispose();
        });

        btnSave.addActionListener(e -> {
            String newNick = txtNickname.getText().trim();
            saveGroupNickname(groupName, targetUser, newNick);
            JOptionPane.showMessageDialog(dialog, "Đã cập nhật biệt danh nhóm mới!");
            if (callback != null) callback.onNicknameChanged(targetUser, newNick);
            dialog.dispose();
        });

        btnPanel.add(btnReset);
        btnPanel.add(btnSave);

        panel.add(lblTitle);
        panel.add(Box.createVerticalStrut(12));
        panel.add(txtNickname);
        panel.add(Box.createVerticalStrut(20));
        panel.add(btnPanel);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    public static void saveGroupNickname(String groupName, String targetUser, String nickname) {
        if (groupName == null || targetUser == null) return;
        String key = groupName + "_" + targetUser;
        if (nickname == null || nickname.trim().isEmpty()) {
            memoryGroupNicknames.remove(key);
        } else {
            memoryGroupNicknames.put(key, nickname.trim());
        }

        if (socketHandler != null) {
            try {
                MessagePacket packet = new MessagePacket("SET_GROUP_NICKNAME", groupName, targetUser, nickname != null ? nickname.trim() : "");
                socketHandler.sendPacket(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Properties props = loadNicknames();
        File dir = new File("user_data");
        if (!dir.exists()) dir.mkdirs();

        if (nickname == null || nickname.trim().isEmpty()) {
            props.remove(key);
        } else {
            props.setProperty(key, nickname.trim());
        }

        try (FileOutputStream fos = new FileOutputStream(NICKNAMES_FILE)) {
            props.store(fos, "User Nicknames");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // 2. BIỆT DANH CHAT RIÊNG TƯ (1-1 PRIVATE)
    // ==========================================
    public static String getPrivateNickname(String username, String targetUser) {
        if (targetUser == null || targetUser.isEmpty()) return "";
        String key = (username != null && !username.isEmpty()) ? username + "_" + targetUser : targetUser;
        if (memoryPrivateNicknames.containsKey(key)) {
            return memoryPrivateNicknames.get(key);
        }
        if (memoryPrivateNicknames.containsKey(targetUser)) {
            return memoryPrivateNicknames.get(targetUser);
        }
        Properties props = loadNicknames();
        return props.getProperty(key, props.getProperty(targetUser, ""));
    }

    public static String getPrivateDisplayName(String username, String targetUser) {
        String nick = getPrivateNickname(username, targetUser);
        if (nick != null && !nick.trim().isEmpty()) {
            return nick.trim();
        }
        return targetUser;
    }

    // Giữ nguyên các hàm cũ trỏ về phạm vi Private để tương thích 100%
    public static String getNickname(String username, String targetUser) {
        return getPrivateNickname(username, targetUser);
    }

    public static String getDisplayName(String username, String targetUser) {
        return getPrivateDisplayName(username, targetUser);
    }

    public static String getShortDisplayName(String username, String targetUser) {
        return getPrivateDisplayName(username, targetUser);
    }

    public static void showDialog(Component parent, String username, String targetUser, NicknameCallback callback) {
        Window parentWindow = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(parentWindow, "Đặt biệt danh", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(400, 220);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));
        panel.setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("Đặt biệt danh cho: " + targetUser);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(new Color(37, 99, 235));

        String currentNickname = getPrivateNickname(username, targetUser);

        JTextField txtNickname = new JTextField(currentNickname);
        txtNickname.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtNickname.setBackground(new Color(248, 250, 252));
        txtNickname.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        txtNickname.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnReset = createButton("Gỡ biệt danh", new Color(241, 245, 249), new Color(226, 232, 240), new Color(15, 23, 42));
        JButton btnSave = createButton("Lưu biệt danh", new Color(37, 99, 235), new Color(29, 78, 216), Color.WHITE);

        btnReset.addActionListener(e -> {
            savePrivateNickname(username, targetUser, "");
            JOptionPane.showMessageDialog(dialog, "Đã xóa biệt danh!");
            if (callback != null) callback.onNicknameChanged(targetUser, "");
            dialog.dispose();
        });

        btnSave.addActionListener(e -> {
            String newNick = txtNickname.getText().trim();
            savePrivateNickname(username, targetUser, newNick);
            JOptionPane.showMessageDialog(dialog, "Đã cập nhật biệt danh mới!");
            if (callback != null) callback.onNicknameChanged(targetUser, newNick);
            dialog.dispose();
        });

        btnPanel.add(btnReset);
        btnPanel.add(btnSave);

        panel.add(lblTitle);
        panel.add(Box.createVerticalStrut(12));
        panel.add(txtNickname);
        panel.add(Box.createVerticalStrut(20));
        panel.add(btnPanel);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    public static void savePrivateNickname(String username, String targetUser, String nickname) {
        if (targetUser == null) return;
        String key = (username != null && !username.isEmpty()) ? username + "_" + targetUser : targetUser;
        if (nickname == null || nickname.trim().isEmpty()) {
            memoryPrivateNicknames.remove(key);
            memoryPrivateNicknames.remove(targetUser);
        } else {
            memoryPrivateNicknames.put(key, nickname.trim());
            memoryPrivateNicknames.put(targetUser, nickname.trim());
        }

        if (socketHandler != null) {
            try {
                MessagePacket packet = new MessagePacket("SET_NICKNAME", username, targetUser, nickname != null ? nickname.trim() : "");
                socketHandler.sendPacket(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Properties props = loadNicknames();
        File dir = new File("user_data");
        if (!dir.exists()) dir.mkdirs();

        String fileKey = (username != null && !username.isEmpty()) ? username + "_" + targetUser : targetUser;
        if (nickname == null || nickname.trim().isEmpty()) {
            props.remove(fileKey);
            props.remove(targetUser);
        } else {
            props.setProperty(fileKey, nickname.trim());
        }

        try (FileOutputStream fos = new FileOutputStream(NICKNAMES_FILE)) {
            props.store(fos, "User Nicknames");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Properties loadNicknames() {
        Properties props = new Properties();
        File file = new File(NICKNAMES_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            } catch (Exception ignored) {}
        }
        return props;
    }

    private static JButton createButton(String text, Color bgNormal, Color bgHover, Color fgText) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(fgText);
        btn.setBackground(bgNormal);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(8, 16, 8, 16)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(bgHover); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { btn.setBackground(bgNormal); }
        });
        return btn;
    }
}
