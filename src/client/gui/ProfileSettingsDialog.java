package client.gui;

import client.network.ClientSocketHandler;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.function.Consumer;

public class ProfileSettingsDialog {

    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);
    private static final Color COLOR_PRIMARY_HOVER = new Color(29, 78, 216);
    private static final Color COLOR_BG_LIGHT = new Color(248, 250, 252);
    private static final Color COLOR_CARD_BG = Color.WHITE;
    private static final Color COLOR_TEXT_DARK = new Color(15, 23, 42);
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);
    private static final Color COLOR_BORDER = new Color(226, 232, 240);
    private static final Color COLOR_INPUT_BG = new Color(241, 245, 249);

    private static final File AVATAR_DIR = new File("user_data/avatars");

    public static BufferedImage getCustomAvatarImage(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        try {
            File file = new File(AVATAR_DIR, username + ".png");
            if (file.exists()) {
                return ImageIO.read(file);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static void saveAvatarToDisk(String username, BufferedImage img) {
        try {
            if (!AVATAR_DIR.exists()) {
                AVATAR_DIR.mkdirs();
            }
            File file = new File(AVATAR_DIR, username + ".png");
            ImageIO.write(img, "png", file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showDialog(JFrame parent, String username, ClientSocketHandler socketHandler, Consumer<BufferedImage> avatarUpdateCallback) {

        JDialog dialog = new JDialog(parent, "Profile & Settings", true);
        dialog.setSize(760, 560);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(COLOR_BG_LIGHT);

        // TOP HEADER BAR
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(COLOR_CARD_BG);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));
        headerPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel lblTitle = new JLabel("⚙️ Profile & Settings");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(COLOR_TEXT_DARK);

        JLabel lblSub = new JLabel("Manage your account credentials, display preferences, and active socket session state.");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(COLOR_TEXT_MUTED);

        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setOpaque(false);
        headerText.add(lblTitle);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(lblSub);

        headerPanel.add(headerText, BorderLayout.WEST);

        // CENTER BODY (LEFT SIDEBAR + RIGHT FORM)
        JPanel bodyPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        bodyPanel.setOpaque(false);
        bodyPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // LEFT COLUMN: CLIENT IDENTITY
        JPanel leftCol = new JPanel();
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
        leftCol.setOpaque(false);

        JLabel lblClientIdentity = new JLabel("CLIENT IDENTITY");
        lblClientIdentity.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblClientIdentity.setForeground(COLOR_TEXT_MUTED);

        // Avatar Card Box
        JPanel avatarCard = new JPanel();
        avatarCard.setLayout(new BoxLayout(avatarCard, BoxLayout.Y_AXIS));
        avatarCard.setBackground(COLOR_CARD_BG);
        avatarCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));

        // Avatar Viewport
        JPanel avatarView = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                AvatarUtils.drawAvatar(g2, username, getWidth(), getHeight());
                g2.dispose();
            }
        };
        avatarView.setPreferredSize(new Dimension(90, 90));
        avatarView.setMaximumSize(new Dimension(90, 90));
        avatarView.setAlignmentX(Component.CENTER_ALIGNMENT);
        avatarView.setOpaque(false);

        JLabel lblName = new JLabel(username);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblName.setForeground(COLOR_TEXT_DARK);
        lblName.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblHandle = new JLabel("@" + username);
        lblHandle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblHandle.setForeground(COLOR_TEXT_MUTED);
        lblHandle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnChangeAvatar = new JButton("\uD83D\uDCF7 Change Avatar");
        btnChangeAvatar.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnChangeAvatar.setForeground(COLOR_PRIMARY);
        btnChangeAvatar.setBackground(new Color(239, 246, 255));
        btnChangeAvatar.setFocusPainted(false);
        btnChangeAvatar.setBorder(BorderFactory.createLineBorder(new Color(191, 219, 254), 1, true));
        btnChangeAvatar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnChangeAvatar.setMaximumSize(new Dimension(200, 34));
        btnChangeAvatar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnChangeAvatar.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Ch\u1ECDn \u1EA3nh đ\u1EA1i di\u1EC7n t\u1EEB thư vi\u1EC7n máy tính");
            int res = chooser.showOpenDialog(dialog);
            if (res == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if (file != null && file.exists()) {
                    try {
                        BufferedImage img = ImageIO.read(file);
                        if (img != null) {
                            saveAvatarToDisk(username, img);
                            avatarView.repaint();
                            if (avatarUpdateCallback != null) {
                                avatarUpdateCallback.accept(img);
                            }
                            JOptionPane.showMessageDialog(dialog, "Đã lưu \u1EA3nh đ\u1EA1i di\u1EC7n c\u1ED1 đ\u1ECBnh thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(dialog, "Không th\u1EC3 đ\u1ECDc t\u1EC7p \u1EA3nh!", "L\u1ED7i", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(dialog, "L\u1ED7i đ\u1ECDc t\u1EC7p \u1EA3nh!", "L\u1ED7i", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        avatarCard.add(avatarView);
        avatarCard.add(Box.createVerticalStrut(10));
        avatarCard.add(lblName);
        avatarCard.add(Box.createVerticalStrut(2));
        avatarCard.add(lblHandle);
        avatarCard.add(Box.createVerticalStrut(12));
        avatarCard.add(btnChangeAvatar);

        // Stats Card
        JPanel statsCard = new JPanel(new GridLayout(3, 2, 4, 6));
        statsCard.setBackground(COLOR_CARD_BG);
        statsCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));

        statsCard.add(createStatLabel("Socket Endpoint", true));
        statsCard.add(createStatLabel("127.0.0.1:52140", false));
        statsCard.add(createStatLabel("Protocol Handshake", true));
        statsCard.add(createStatLabel("SECURE_AES256", false));
        statsCard.add(createStatLabel("Local Storage Quota", true));
        statsCard.add(createStatLabel("18.4 MB / 512 MB", false));

        // Socket status pill at bottom
        JLabel lblTcpStatus = new JLabel("\uD83D\uDFE2 TCP Channel Open");
        lblTcpStatus.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTcpStatus.setForeground(new Color(22, 101, 52));
        lblTcpStatus.setBackground(new Color(220, 252, 231));
        lblTcpStatus.setOpaque(true);
        lblTcpStatus.setBorder(new EmptyBorder(6, 10, 6, 10));

        leftCol.add(lblClientIdentity);
        leftCol.add(Box.createVerticalStrut(6));
        leftCol.add(avatarCard);
        leftCol.add(Box.createVerticalStrut(10));
        leftCol.add(statsCard);
        leftCol.add(Box.createVerticalStrut(10));
        leftCol.add(lblTcpStatus);

        // RIGHT COLUMN: ACCOUNT CREDENTIALS & CHANGE PASSWORD (NO DISPLAY NAME)
        JPanel rightCol = new JPanel();
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
        rightCol.setOpaque(false);

        // Account Credentials Section
        JLabel lblCredsTitle = new JLabel("\uD83D\uDC64 Account Credentials");
        lblCredsTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblCredsTitle.setForeground(COLOR_PRIMARY);

        JLabel lblUsername = new JLabel("Current Username (Read-Only)");
        lblUsername.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblUsername.setForeground(COLOR_TEXT_MUTED);

        JTextField txtUsername = new JTextField("@" + username);
        txtUsername.setEditable(false);
        txtUsername.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtUsername.setBackground(COLOR_INPUT_BG);
        txtUsername.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));
        txtUsername.setMaximumSize(new Dimension(340, 36));

        // Change Password Section
        JLabel lblPassTitle = new JLabel("\uD83D\uDD11 Change Password");
        lblPassTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPassTitle.setForeground(COLOR_PRIMARY);

        JPasswordField txtCurrentPass = new JPasswordField();
        JPanel pnlCurrentPass = createPasswordFieldWithEye(txtCurrentPass);

        JPasswordField txtNewPass = new JPasswordField();
        JPanel pnlNewPass = createPasswordFieldWithEye(txtNewPass);

        JPasswordField txtConfirmPass = new JPasswordField();
        JPanel pnlConfirmPass = createPasswordFieldWithEye(txtConfirmPass);

        JLabel lblPassHint = new JLabel("\u24D8 M\u1EADt kh\u1EA9u m\u1EDBi c\u1EA7n t\u1ED1i thi\u1EC3u 8 ký t\u1EF1.");
        lblPassHint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblPassHint.setForeground(COLOR_TEXT_MUTED);

        rightCol.add(lblCredsTitle);
        rightCol.add(Box.createVerticalStrut(4));
        rightCol.add(lblUsername);
        rightCol.add(Box.createVerticalStrut(4));
        rightCol.add(txtUsername);
        rightCol.add(Box.createVerticalStrut(14));

        rightCol.add(lblPassTitle);
        rightCol.add(Box.createVerticalStrut(4));
        rightCol.add(new JLabel("M\u1EADt kh\u1EA9u hi\u1EC7n t\u1EA1i:"));
        rightCol.add(pnlCurrentPass);
        rightCol.add(Box.createVerticalStrut(4));
        rightCol.add(new JLabel("M\u1EADt kh\u1EA9u m\u1EDBi:"));
        rightCol.add(pnlNewPass);
        rightCol.add(Box.createVerticalStrut(4));
        rightCol.add(new JLabel("Xác nh\u1EADn m\u1EADt kh\u1EA9u m\u1EDBi:"));
        rightCol.add(pnlConfirmPass);
        rightCol.add(Box.createVerticalStrut(6));
        rightCol.add(lblPassHint);

        bodyPanel.add(leftCol);
        bodyPanel.add(rightCol);

        // FOOTER ACTION BUTTONS
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(COLOR_CARD_BG);
        footerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BORDER),
                new EmptyBorder(12, 20, 12, 20)
        ));

        JButton btnLogout = new JButton("\uD83D\uDEAA Logout & Terminate Socket");
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLogout.setForeground(new Color(225, 29, 72));
        btnLogout.setBackground(new Color(254, 242, 242));
        btnLogout.setFocusPainted(false);
        btnLogout.setBorder(BorderFactory.createLineBorder(new Color(254, 202, 202), 1, true));
        btnLogout.setPreferredSize(new Dimension(210, 38));
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnLogout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(dialog, "B\u1EA1n có ch\u1EAFc mu\u1ED1n đ\u0103ng xu\u1EA5t?", "Đ\u0103ng xu\u1EA5t", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                dialog.dispose();
                if (parent instanceof ChatForm) {
                    ((ChatForm) parent).performLogout();
                } else {
                    parent.dispose();
                    new LoginForm().setVisible(true);
                }
            }
        });

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightButtons.setOpaque(false);

        JButton btnDiscard = new JButton("Discard Changes");
        btnDiscard.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnDiscard.setForeground(COLOR_TEXT_DARK);
        btnDiscard.setBackground(COLOR_CARD_BG);
        btnDiscard.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1, true));
        btnDiscard.setPreferredSize(new Dimension(135, 38));
        btnDiscard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnDiscard.addActionListener(e -> dialog.dispose());

        JButton btnSave = new JButton("\uD83D\uDCBE Save Changes");
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSave.setForeground(Color.WHITE);
        btnSave.setBackground(COLOR_PRIMARY);
        btnSave.setFocusPainted(false);
        btnSave.setBorderPainted(false);
        btnSave.setPreferredSize(new Dimension(145, 38));
        btnSave.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnSave.addActionListener(e -> {
            String currentPass = new String(txtCurrentPass.getPassword()).trim();
            String newPass = new String(txtNewPass.getPassword()).trim();
            String confirmPass = new String(txtConfirmPass.getPassword()).trim();

            if (!newPass.isEmpty()) {
                if (currentPass.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Vui lòng nh\u1EADp m\u1EADt kh\u1EA9u hi\u1EC7n t\u1EA1i!", "L\u1ED7i", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (newPass.length() < 8) {
                    JOptionPane.showMessageDialog(dialog, "M\u1EADt kh\u1EA9u m\u1EDBi ph\u1EA3i t\u1EEB 8 ký t\u1EF1 tr\u1EDF lên!", "L\u1ED7i", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (!newPass.equals(confirmPass)) {
                    JOptionPane.showMessageDialog(dialog, "M\u1EADt kh\u1EA9u m\u1EDBi và m\u1EADt kh\u1EA9u xác nh\u1EADn không trùng kh\u1EDBp!", "L\u1ED7i", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (socketHandler != null) {
                    try {
                        common.MessagePacket packet = new common.MessagePacket("CHANGE_PASSWORD", username, "", currentPass + ":" + newPass);
                        socketHandler.sendPacket(packet);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(dialog, "Không th\u1EC3 g\u1EEDi yêu c\u1EA7u đ\u1ED5i m\u1EADt kh\u1EA9u!", "L\u1ED7i", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            } else {
                JOptionPane.showMessageDialog(dialog, "Đã lưu cài đ\u1EB7t!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            }
        });

        rightButtons.add(btnDiscard);
        rightButtons.add(btnSave);

        footerPanel.add(btnLogout, BorderLayout.WEST);
        footerPanel.add(rightButtons, BorderLayout.EAST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(bodyPanel, BorderLayout.CENTER);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private static JLabel createStatLabel(String text, boolean isHeader) {
        JLabel label = new JLabel(text);
        if (isHeader) {
            label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            label.setForeground(COLOR_TEXT_MUTED);
        } else {
            label.setFont(new Font("Segoe UI", Font.BOLD, 11));
            label.setForeground(COLOR_TEXT_DARK);
        }
        return label;
    }

    private static JPanel createPasswordFieldWithEye(JPasswordField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBackground(COLOR_INPUT_BG);
        field.setBorder(new EmptyBorder(6, 10, 6, 4));
        field.setOpaque(false);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(340, 36));
        wrapper.setPreferredSize(new Dimension(340, 36));

        JLabel lblEye = new JLabel(new EyeIcon(false, COLOR_TEXT_MUTED));
        lblEye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblEye.setToolTipText("Nh\u1EA5p đ\u1EC3 \u1EA9n/hi\u1EC7n m\u1EADt kh\u1EA9u");
        lblEye.setBorder(new EmptyBorder(0, 4, 0, 8));

        char defaultEcho = field.getEchoChar();
        lblEye.addMouseListener(new java.awt.event.MouseAdapter() {
            private boolean showing = false;
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showing = !showing;
                field.setEchoChar(showing ? (char) 0 : defaultEcho);
                lblEye.setIcon(showing ? new EyeIcon(true, COLOR_PRIMARY) : new EyeIcon(false, COLOR_TEXT_MUTED));
            }
        });

        wrapper.add(field, BorderLayout.CENTER);
        wrapper.add(lblEye, BorderLayout.EAST);

        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(true);
        container.setBackground(COLOR_INPUT_BG);
        container.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1, true));
        container.setMaximumSize(new Dimension(340, 36));
        container.setPreferredSize(new Dimension(340, 36));
        container.add(wrapper, BorderLayout.CENTER);

        return container;
    }

    private static class EyeIcon implements Icon {
        private final boolean visible;
        private final Color color;

        public EyeIcon(boolean visible, Color color) {
            this.visible = visible;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int w = 16;
            int h = 10;
            int cx = x + 2;
            int cy = y + 4;

            Path2D eye = new Path2D.Float();
            eye.moveTo(cx, cy + h / 2.0f);
            eye.quadTo(cx + w / 2.0f, cy - 2, cx + w, cy + h / 2.0f);
            eye.quadTo(cx + w / 2.0f, cy + h + 2, cx, cy + h / 2.0f);
            eye.closePath();
            g2.draw(eye);

            if (visible) {
                g2.fillOval(cx + w / 2 - 3, cy + h / 2 - 3, 6, 6);
            } else {
                g2.fillOval(cx + w / 2 - 2, cy + h / 2 - 2, 4, 4);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx + 1, cy + h + 1, cx + w - 1, cy - 1);
            }

            g2.dispose();
        }

        @Override
        public int getIconWidth() { return 20; }

        @Override
        public int getIconHeight() { return 18; }
    }
}
