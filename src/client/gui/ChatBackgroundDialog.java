package client.gui;

import client.network.ClientSocketHandler;
import common.MessagePacket;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public class ChatBackgroundDialog {

    public interface BackgroundChangeCallback {
        void onBackgroundChanged();
    }

    private static final String PREFS_FILE = "user_data/bg_prefs.properties";

    public static String getCanonicalConvId(String user1, String user2) {
        return getCanonicalConvId(user1, user2, false);
    }

    public static String getCanonicalConvId(String user1, String user2, boolean isGroup) {
        if (user2 == null || user2.isEmpty() || "Cộng đồng".equals(user2) || "Cộng đồng".equals(user1)) {
            return "Cộng đồng";
        }
        if (isGroup || user2.startsWith("[GROUP]")) {
            String cleanTarget = user2;
            if (cleanTarget.startsWith("[GROUP]")) {
                cleanTarget = cleanTarget.substring(7).split(":")[0].trim();
            }
            return cleanTarget.trim();
        }
        String cleanTarget = user2;
        if (user1.compareTo(cleanTarget) < 0) {
            return user1 + ":" + cleanTarget;
        } else {
            return cleanTarget + ":" + user1;
        }
    }

    public static void showDialog(Component parent, String username, String currentTarget, BackgroundChangeCallback callback) {
        showDialog(parent, username, currentTarget, false, null, callback);
    }

    public static void showDialog(Component parent, String username, String currentTarget, ClientSocketHandler socketHandler, BackgroundChangeCallback callback) {
        showDialog(parent, username, currentTarget, false, socketHandler, callback);
    }

    public static void showDialog(Component parent, String username, String currentTarget, boolean isGroup, ClientSocketHandler socketHandler, BackgroundChangeCallback callback) {
        Window parentWindow = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(parentWindow, "Thay đổi hình nền đoạn Chat", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(440, 260);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(22, 26, 22, 26));
        panel.setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("Cài đặt hình nền: " + currentTarget);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(new Color(37, 99, 235));

        JLabel lblSub = new JLabel("<html><span style='color:#64748B;'>Hình nền sẽ được đồng bộ tức thì cho cả hai người trong đoạn chat</span></html>");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JCheckBox chkApplyGlobal = new JCheckBox("Áp dụng riêng cho tất cả đoạn chat của tôi");
        chkApplyGlobal.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chkApplyGlobal.setForeground(new Color(15, 23, 42));
        chkApplyGlobal.setOpaque(false);

        // Nút Chọn Ảnh Nền
        JButton btnChooseImage = createButton("Chọn Ảnh Nền (.PNG, .JPG)", new Color(37, 99, 235), new Color(29, 78, 216), Color.WHITE);
        btnChooseImage.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        // Nút Khôi phục mặc định
        JButton btnResetDefault = createButton("Khôi phục mặc định", new Color(254, 242, 242), new Color(254, 202, 202), new Color(225, 29, 72));
        btnResetDefault.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        // 1. Thao tác Chọn hình ảnh nền
        btnChooseImage.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Chọn ảnh làm nền Chat");
            chooser.setFileFilter(new FileNameExtensionFilter("Hình ảnh (*.png, *.jpg, *.jpeg)", "png", "jpg", "jpeg"));

            int res = chooser.showOpenDialog(dialog);
            if (res == JFileChooser.APPROVE_OPTION) {
                File selFile = chooser.getSelectedFile();
                if (selFile != null && selFile.exists()) {
                    try {
                        byte[] fileData = java.nio.file.Files.readAllBytes(selFile.toPath());
                        String convId = getCanonicalConvId(username, currentTarget, isGroup);
                        String safeConv = convId.replaceAll("[^a-zA-Z0-9_]", "_");
                        String fileName = "bg_" + safeConv + "_" + System.currentTimeMillis() + ".jpg";

                        // Lưu bản sao vào thư mục user_data/backgrounds/
                        File bgDir = new File("user_data/backgrounds");
                        if (!bgDir.exists()) bgDir.mkdirs();
                        File localBgFile = new File(bgDir, fileName);
                        java.nio.file.Files.write(localBgFile.toPath(), fileData);

                        if (chkApplyGlobal.isSelected()) {
                            savePref(username, "GLOBAL", "image", localBgFile.getAbsolutePath());
                            savePref(username, "GLOBAL", "color", "");
                        } else {
                            applySyncedBackground(convId, "", localBgFile.getAbsolutePath());
                            if (socketHandler != null) {
                                MessagePacket packet = new MessagePacket("SET_BG", username, currentTarget, "IMAGE");
                                packet.setFileName(fileName);
                                packet.setFileData(fileData);
                                socketHandler.sendPacket(packet);
                            }
                        }

                        JOptionPane.showMessageDialog(dialog, "Đã cập nhật và đồng bộ ảnh nền cho đoạn chat!");
                        if (callback != null) callback.onBackgroundChanged();
                        dialog.dispose();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(dialog, "Lỗi đọc file ảnh: " + ex.getMessage());
                    }
                }
            }
        });

        // 2. Thao tác Khôi phục mặc định
        btnResetDefault.addActionListener(e -> {
            String convId = getCanonicalConvId(username, currentTarget, isGroup);
            if (chkApplyGlobal.isSelected()) {
                savePref(username, "GLOBAL", "color", "");
                savePref(username, "GLOBAL", "image", "");
            }
            applySyncedBackground(convId, "", "");
            if (socketHandler != null) {
                try {
                    MessagePacket packet = new MessagePacket("SET_BG", username, currentTarget, "RESET");
                    socketHandler.sendPacket(packet);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            JOptionPane.showMessageDialog(dialog, "Đã khôi phục cài đặt mặc định!");
            if (callback != null) callback.onBackgroundChanged();
            dialog.dispose();
        });

        panel.add(lblTitle);
        panel.add(Box.createVerticalStrut(4));
        panel.add(lblSub);
        panel.add(Box.createVerticalStrut(10));
        panel.add(chkApplyGlobal);
        panel.add(Box.createVerticalStrut(14));
        panel.add(btnChooseImage);
        panel.add(Box.createVerticalStrut(10));
        panel.add(btnResetDefault);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    public static synchronized void applySyncedBackground(String convId, String color, String imagePath) {
        Properties props = loadPrefs();
        String keyColor = "CONV_" + convId + "_color";
        String keyImage = "CONV_" + convId + "_image";

        if (color != null && !color.isEmpty()) {
            props.setProperty(keyColor, color);
        } else {
            props.remove(keyColor);
        }

        if (imagePath != null && !imagePath.isEmpty()) {
            props.setProperty(keyImage, imagePath);
        } else {
            props.remove(keyImage);
        }

        // Lưu cả 2 chiều k1_k2 và k2_k1 để bất kỳ ai mở chat đều nhận đúng ngay lập tức
        if (convId.contains(":")) {
            String[] uParts = convId.split(":");
            String k1 = uParts[0] + "_" + uParts[1];
            String k2 = uParts[1] + "_" + uParts[0];
            if (imagePath != null && !imagePath.isEmpty()) {
                props.setProperty(k1 + "_image", imagePath);
                props.setProperty(k2 + "_image", imagePath);
            } else {
                props.remove(k1 + "_image");
                props.remove(k2 + "_image");
            }
            props.remove(k1 + "_color");
            props.remove(k2 + "_color");
        } else {
            String clean = convId.startsWith("[GROUP]") ? convId.substring(7).split(":")[0].trim() : convId.trim();
            if (imagePath != null && !imagePath.isEmpty()) {
                props.setProperty(convId + "_image", imagePath);
                props.setProperty(clean + "_image", imagePath);
                props.setProperty("CONV_" + clean + "_image", imagePath);
            } else {
                props.remove(convId + "_image");
                props.remove(clean + "_image");
                props.remove("CONV_" + convId + "_image");
                props.remove("CONV_" + clean + "_image");
            }
            props.remove(convId + "_color");
            props.remove(clean + "_color");
            props.remove("CONV_" + convId + "_color");
            props.remove("CONV_" + clean + "_color");
        }

        File dir = new File("user_data");
        if (!dir.exists()) dir.mkdirs();
        try (FileOutputStream fos = new FileOutputStream(PREFS_FILE)) {
            props.store(fos, "Chat Background Preferences");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getBgColorHex(String username, String target) {
        return getBgColorHex(username, target, false);
    }

    public static String getBgColorHex(String username, String target, boolean isGroup) {
        if (target == null || target.isEmpty()) return "";
        Properties props = loadPrefs();
        String convId = getCanonicalConvId(username, target, isGroup);
        String color = props.getProperty("CONV_" + convId + "_color", "");

        if (color.isEmpty()) {
            String clean = target.startsWith("[GROUP]") ? target.substring(7).split(":")[0].trim() : target.trim();
            color = props.getProperty("CONV_" + clean + "_color", "");
            if (color.isEmpty()) {
                color = props.getProperty(clean + "_color", "");
            }
        }

        if (color.isEmpty() && convId.contains(":")) {
            String[] u = convId.split(":");
            color = props.getProperty(u[0] + "_" + u[1] + "_color", "");
            if (color.isEmpty()) color = props.getProperty(u[1] + "_" + u[0] + "_color", "");
        }
        return color;
    }

    public static String getBgImagePath(String username, String target) {
        return getBgImagePath(username, target, false);
    }

    public static String getBgImagePath(String username, String target, boolean isGroup) {
        if (target == null || target.isEmpty()) return "";
        Properties props = loadPrefs();
        String convId = getCanonicalConvId(username, target, isGroup);
        String path = props.getProperty("CONV_" + convId + "_image", "");

        if (path.isEmpty()) {
            String clean = target.startsWith("[GROUP]") ? target.substring(7).split(":")[0].trim() : target.trim();
            path = props.getProperty("CONV_" + clean + "_image", "");
            if (path.isEmpty()) {
                path = props.getProperty(clean + "_image", "");
            }
        }

        if (path.isEmpty() && convId.contains(":")) {
            String[] u = convId.split(":");
            path = props.getProperty(u[0] + "_" + u[1] + "_image", "");
            if (path.isEmpty()) path = props.getProperty(u[1] + "_" + u[0] + "_image", "");
        }

        if (!path.isEmpty()) {
            File f = new File(path);
            if (f.exists()) return f.getAbsolutePath();
            File fLocal = new File("user_data/backgrounds", path);
            if (fLocal.exists()) return fLocal.getAbsolutePath();
            File fServer = new File("server_storage", path);
            if (fServer.exists()) return fServer.getAbsolutePath();
        }
        return path;
    }

    private static Properties loadPrefs() {
        Properties props = new Properties();
        File file = new File(PREFS_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            } catch (Exception ignored) {}
        }
        return props;
    }

    private static void savePref(String username, String target, String type, String value) {
        Properties props = loadPrefs();
        File dir = new File("user_data");
        if (!dir.exists()) dir.mkdirs();

        String key = username + "_" + target + "_" + type;
        if (value == null || value.isEmpty()) {
            props.remove(key);
        } else {
            props.setProperty(key, value);
        }

        try (FileOutputStream fos = new FileOutputStream(PREFS_FILE)) {
            props.store(fos, "Chat Background Preferences");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JButton createButton(String text, Color bgNormal, Color bgHover, Color fgText) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(fgText);
        btn.setBackground(bgNormal);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(10, 16, 10, 16)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(bgHover); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { btn.setBackground(bgNormal); }
        });
        return btn;
    }
}
