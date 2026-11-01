package client.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class UserProfileDialog {

    public interface UserProfileCallback {
        void onOpenDirectChat(String targetUser);
        void onNicknameChanged();
    }

    public static void showDialog(JFrame parent, String currentUsername, String targetUser, boolean isOnline, UserProfileCallback callback) {
        JDialog dialog = new JDialog(parent, "Thông tin người dùng", true);
        dialog.setSize(380, 360);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Avatar Panel (80x80) using AvatarUtils (Facebook silhouette default)
        JPanel avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                AvatarUtils.drawAvatar(g2, targetUser, getWidth(), getHeight());
                g2.dispose();
            }
        };
        avatarPanel.setPreferredSize(new Dimension(80, 80));
        avatarPanel.setMaximumSize(new Dimension(80, 80));
        avatarPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        avatarPanel.setOpaque(false);

        // Display Name (Nickname or Username)
        String nickname = NicknameDialog.getNickname(currentUsername, targetUser);
        String mainName = (nickname != null && !nickname.trim().isEmpty()) ? nickname.trim() : targetUser;

        JLabel lblName = new JLabel(mainName);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblName.setForeground(new Color(15, 23, 42));
        lblName.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Account Handle
        JLabel lblHandle = new JLabel("@" + targetUser);
        lblHandle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblHandle.setForeground(new Color(100, 116, 139));
        lblHandle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Status Badge
        JPanel statusBadge = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 2));
        statusBadge.setOpaque(false);
        statusBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblDot = new JLabel("●");
        lblDot.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDot.setForeground(isOnline ? new Color(16, 185, 129) : new Color(148, 163, 184));

        JLabel lblStatus = new JLabel(isOnline ? "Online" : "Offline");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setForeground(isOnline ? new Color(22, 101, 52) : new Color(71, 85, 105));

        statusBadge.add(lblDot);
        statusBadge.add(lblStatus);

        // Buttons Panel
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new BoxLayout(btnPanel, BoxLayout.Y_AXIS));
        btnPanel.setOpaque(false);
        btnPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnNickname = createButton("<html><span style='font-family:Segoe UI Emoji;'>✏️</span>  <span style='font-family:Segoe UI;'>Đặt biệt danh</span></html>", new Color(241, 245, 249), new Color(226, 232, 240), new Color(15, 23, 42));
        btnNickname.setMaximumSize(new Dimension(300, 38));
        btnNickname.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnNickname.addActionListener(e -> {
            dialog.dispose();
            NicknameDialog.showDialog(parent, currentUsername, targetUser, (u, n) -> {
                if (callback != null) callback.onNicknameChanged();
            });
        });

        JButton btnDirectChat = createButton("<html><span style='font-family:Segoe UI Emoji;'>💬</span>  <span style='font-family:Segoe UI;'>Nhắn tin riêng</span></html>", new Color(37, 99, 235), new Color(29, 78, 216), Color.WHITE);
        btnDirectChat.setMaximumSize(new Dimension(300, 38));
        btnDirectChat.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnDirectChat.addActionListener(e -> {
            dialog.dispose();
            if (callback != null) {
                callback.onOpenDirectChat(targetUser);
            }
        });

        // Hide direct chat button if viewing oneself
        if (currentUsername.equals(targetUser)) {
            btnDirectChat.setVisible(false);
        }

        btnPanel.add(btnNickname);
        btnPanel.add(Box.createVerticalStrut(8));
        btnPanel.add(btnDirectChat);

        mainPanel.add(avatarPanel);
        mainPanel.add(Box.createVerticalStrut(12));
        mainPanel.add(lblName);
        mainPanel.add(Box.createVerticalStrut(2));
        mainPanel.add(lblHandle);
        mainPanel.add(Box.createVerticalStrut(8));
        mainPanel.add(statusBadge);
        mainPanel.add(Box.createVerticalStrut(16));
        mainPanel.add(btnPanel);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private static JButton createButton(String text, Color bgNormal, Color bgHover, Color fgText) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
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
