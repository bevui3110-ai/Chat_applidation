package client.gui;

import client.network.ClientSocketHandler;
import common.MessagePacket;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;

public class RegisterForm extends JFrame {

    private String serverIP;
    private int serverPort;

    private JTextField txtRegUser;
    private JPasswordField txtRegPass;
    private JPasswordField txtRegConfirmPass;

    // Color Palette
    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);       // #2563EB
    private static final Color COLOR_PRIMARY_HOVER = new Color(29, 78, 216); // #1D4ED8
    private static final Color COLOR_BG_LIGHT = new Color(248, 250, 252);     // #F8FAFC
    private static final Color COLOR_CARD_BG = Color.WHITE;
    private static final Color COLOR_INPUT_BG = new Color(248, 250, 252);    // #F8FAFC
    private static final Color COLOR_INPUT_BORDER = new Color(226, 232, 240);// #E2E8F0
    private static final Color COLOR_TEXT_DARK = new Color(15, 23, 42);      // #0F172A
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);  // #64748B
    private static final Color COLOR_SECONDARY_BG = new Color(239, 246, 255); // #EFF6FF
    private static final Color COLOR_SECONDARY_HOVER = new Color(219, 234, 254); // #DBEAFE

    public RegisterForm(String ip, String portStr) {
        this.serverIP = (ip == null || ip.trim().isEmpty()) ? "127.0.0.1" : ip.trim();
        try {
            this.serverPort = Integer.parseInt(portStr.trim());
        } catch (Exception e) {
            this.serverPort = 12345;
        }

        setTitle("Create an Account - ChatChit");
        setSize(460, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel bgPanel = new JPanel(new GridBagLayout());
        bgPanel.setBackground(COLOR_BG_LIGHT);
        bgPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel registerCard = buildRegisterCard();
        bgPanel.add(registerCard);
        add(bgPanel);
    }

    private JPanel buildRegisterCard() {
        RoundedCardPanel card = new RoundedCardPanel(20, COLOR_CARD_BG);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(25, 28, 22, 28));

        // 1. Header (Icon + Badge + Title + Subtitle)
        card.add(createHeaderPanel("Create an Account", "Register to join the ChatChit TCP network", "GATEWAY PROTOCOL V1.0"));
        card.add(Box.createVerticalStrut(16));

        // 2. Server Connection Pill
        JPanel statusPill = new JPanel(new BorderLayout());
        statusPill.setOpaque(true);
        statusPill.setBackground(COLOR_SECONDARY_BG);
        statusPill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(219, 234, 254), 1, true),
                new EmptyBorder(6, 12, 6, 12)
        ));
        statusPill.setMaximumSize(new Dimension(380, 32));

        JLabel lblRegStatusIP = new JLabel("<html><font color='#10B981'>\u25CF</font> tcp://" + serverIP + ":" + serverPort + "</html>");
        lblRegStatusIP.setFont(new Font("Monospaced", Font.BOLD, 11));
        lblRegStatusIP.setForeground(new Color(30, 64, 175));

        JLabel lblReady = new JLabel("READY");
        lblReady.setFont(new Font("Monospaced", Font.BOLD, 10));
        lblReady.setForeground(new Color(30, 64, 175));

        statusPill.add(lblRegStatusIP, BorderLayout.WEST);
        statusPill.add(lblReady, BorderLayout.EAST);
        card.add(statusPill);
        card.add(Box.createVerticalStrut(14));

        // 3. Username Row
        JPanel userPanel = new JPanel();
        userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.Y_AXIS));
        userPanel.setOpaque(false);
        userPanel.setMaximumSize(new Dimension(380, 62));

        JPanel userLabelRow = new JPanel(new BorderLayout());
        userLabelRow.setOpaque(false);
        JLabel lblUser = new JLabel("Username");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUser.setForeground(COLOR_TEXT_DARK);
        JLabel lblMinUser = new JLabel("min 4 chars");
        lblMinUser.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lblMinUser.setForeground(COLOR_TEXT_MUTED);
        userLabelRow.add(lblUser, BorderLayout.WEST);
        userLabelRow.add(lblMinUser, BorderLayout.EAST);

        txtRegUser = new JTextField();
        JPanel userContainer = createStyledTextFieldContainer(txtRegUser, "Choose a username", "@");

        userPanel.add(userLabelRow);
        userPanel.add(Box.createVerticalStrut(4));
        userPanel.add(userContainer);
        card.add(userPanel);
        card.add(Box.createVerticalStrut(12));

        // 4. Password Row
        JPanel passPanel = new JPanel();
        passPanel.setLayout(new BoxLayout(passPanel, BoxLayout.Y_AXIS));
        passPanel.setOpaque(false);
        passPanel.setMaximumSize(new Dimension(380, 62));

        JPanel passLabelRow = new JPanel(new BorderLayout());
        passLabelRow.setOpaque(false);
        JLabel lblPass = new JLabel("Password");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPass.setForeground(COLOR_TEXT_DARK);
        JLabel lblMinPass = new JLabel("min 1 char");
        lblMinPass.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lblMinPass.setForeground(COLOR_TEXT_MUTED);
        passLabelRow.add(lblPass, BorderLayout.WEST);
        passLabelRow.add(lblMinPass, BorderLayout.EAST);

        txtRegPass = new JPasswordField();
        JPanel passContainer = createStyledPasswordFieldContainer(txtRegPass, "Create password", null);

        passPanel.add(passLabelRow);
        passPanel.add(Box.createVerticalStrut(4));
        passPanel.add(passContainer);
        card.add(passPanel);
        card.add(Box.createVerticalStrut(12));

        // 5. Confirm Password Row
        JPanel confirmPanel = new JPanel();
        confirmPanel.setLayout(new BoxLayout(confirmPanel, BoxLayout.Y_AXIS));
        confirmPanel.setOpaque(false);
        confirmPanel.setMaximumSize(new Dimension(380, 62));

        JPanel confirmLabelRow = new JPanel(new BorderLayout());
        confirmLabelRow.setOpaque(false);
        JLabel lblConfirm = new JLabel("Confirm Password");
        lblConfirm.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblConfirm.setForeground(COLOR_TEXT_DARK);
        JLabel lblMustMatch = new JLabel("must match");
        lblMustMatch.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lblMustMatch.setForeground(COLOR_TEXT_MUTED);
        confirmLabelRow.add(lblConfirm, BorderLayout.WEST);
        confirmLabelRow.add(lblMustMatch, BorderLayout.EAST);

        txtRegConfirmPass = new JPasswordField();
        JPanel confirmContainer = createStyledPasswordFieldContainer(txtRegConfirmPass, "Repeat password", null);

        confirmPanel.add(confirmLabelRow);
        confirmPanel.add(Box.createVerticalStrut(4));
        confirmPanel.add(confirmContainer);
        card.add(confirmPanel);
        card.add(Box.createVerticalStrut(14));

        // 6. Security Info Banner
        JPanel infoBox = new JPanel(new BorderLayout(8, 0));
        infoBox.setOpaque(true);
        infoBox.setBackground(COLOR_SECONDARY_BG);
        infoBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(191, 219, 254), 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));
        infoBox.setMaximumSize(new Dimension(380, 48));

        JLabel lblInfoIcon = new JLabel("i");
        lblInfoIcon.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblInfoIcon.setForeground(COLOR_PRIMARY);

        JTextArea txtInfo = new JTextArea("Client accounts authenticate securely against the TCP Auth broker before stream negotiation.");
        txtInfo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        txtInfo.setForeground(new Color(30, 64, 175));
        txtInfo.setLineWrap(true);
        txtInfo.setWrapStyleWord(true);
        txtInfo.setEditable(false);
        txtInfo.setOpaque(false);

        infoBox.add(lblInfoIcon, BorderLayout.WEST);
        infoBox.add(txtInfo, BorderLayout.CENTER);
        card.add(infoBox);
        card.add(Box.createVerticalStrut(16));

        // 7. Buttons
        ModernButton btnRegister = new ModernButton("\u2192  Register Account", COLOR_PRIMARY, COLOR_PRIMARY_HOVER, Color.WHITE);
        btnRegister.addActionListener(e -> processRegister());
        card.add(btnRegister);
        card.add(Box.createVerticalStrut(10));

        ModernButton btnBackLogin = new ModernButton("\u2190  Back to Login", COLOR_SECONDARY_BG, COLOR_SECONDARY_HOVER, COLOR_PRIMARY);
        btnBackLogin.addActionListener(e -> {
            LoginForm loginForm = new LoginForm(serverIP, String.valueOf(serverPort), txtRegUser.getText().trim());
            loginForm.setVisible(true);
            dispose();
        });
        card.add(btnBackLogin);
        card.add(Box.createVerticalStrut(16));

        // 8. Footer (Daemon / Port info)
        JPanel regFooter = new JPanel(new BorderLayout());
        regFooter.setOpaque(false);
        regFooter.setMaximumSize(new Dimension(380, 20));

        JLabel lblDaemon = new JLabel("Daemon: ONLINE");
        lblDaemon.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lblDaemon.setForeground(COLOR_TEXT_MUTED);

        JLabel lblPortInfo = new JLabel("Port " + serverPort + "/TCP");
        lblPortInfo.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lblPortInfo.setForeground(COLOR_TEXT_MUTED);

        regFooter.add(lblDaemon, BorderLayout.WEST);
        regFooter.add(lblPortInfo, BorderLayout.EAST);
        card.add(regFooter);

        return card;
    }

    private void processRegister() {
        String username = txtRegUser.getText().trim();
        String password = new String(txtRegPass.getPassword()).trim();
        String confirmPass = new String(txtRegConfirmPass.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty() || confirmPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng đi\u1EC1n đ\u1EA7y đ\u1EE7 các thông tin đăng ký!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (username.length() < 4) {
            JOptionPane.showMessageDialog(this, "Username ph\u1EA3i có ít nh\u1EA5t 4 ký t\u1EF1!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (password.length() < 1) {
            JOptionPane.showMessageDialog(this, "Vui lòng nh\u1EADp m\u1EADt kh\u1EA9u!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!password.equals(confirmPass)) {
            JOptionPane.showMessageDialog(this, "M\u1EADt kh\u1EA9u xác nh\u1EADn không kh\u1EDBp!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ClientSocketHandler handler = null;
        try {
            handler = new ClientSocketHandler(serverIP, serverPort);
            MessagePacket packet = new MessagePacket("REGISTER", username, "", password);
            handler.sendPacket(packet);

            MessagePacket response = handler.readPacket();
            if (response != null) {
                if ("REGISTER_SUCCESS".equals(response.getType())) {
                    JOptionPane.showMessageDialog(this, "T\u1EA1o tài kho\u1EA3n thành công! Hãy đăng nh\u1EADp.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    LoginForm loginForm = new LoginForm(serverIP, String.valueOf(serverPort), username);
                    loginForm.setVisible(true);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, response.getContent(), "Đăng ký th\u1EA5t b\u1EA1i", JOptionPane.ERROR_MESSAGE);
                }
            }
            handler.close();
        } catch (IOException | ClassNotFoundException ex) {
            JOptionPane.showMessageDialog(this, "Không th\u1EC3 k\u1EBFt n\u1ED1i đ\u1EBFn Server (" + serverIP + ":" + serverPort + ")!", "L\u1ED7i k\u1EBFt n\u1ED1i", JOptionPane.ERROR_MESSAGE);
            if (handler != null) {
                handler.close();
            }
        }
    }

    // ==========================================
    // UI HELPER METHODS
    // ==========================================
    private JPanel createHeaderPanel(String title, String subtitle, String topBadge) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel iconPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(COLOR_PRIMARY);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));

                g2.setColor(Color.WHITE);
                int bw = 24, bh = 18;
                int bx = (getWidth() - bw) / 2;
                int by = (getHeight() - bh) / 2 - 1;
                g2.fillRoundRect(bx, by, bw, bh, 8, 8);

                int[] px = {bx + 4, bx + 10, bx + 2};
                int[] py = {by + bh, by + bh, by + bh + 4};
                g2.fillPolygon(px, py, 3);

                g2.setColor(COLOR_PRIMARY);
                g2.fillOval(bx + 5, by + 7, 3, 3);
                g2.fillOval(bx + 10, by + 7, 3, 3);
                g2.fillOval(bx + 15, by + 7, 3, 3);

                g2.dispose();
            }
        };
        iconPanel.setPreferredSize(new Dimension(48, 48));
        iconPanel.setMaximumSize(new Dimension(48, 48));
        iconPanel.setOpaque(false);
        iconPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(iconPanel);
        panel.add(Box.createVerticalStrut(12));

        if (topBadge != null) {
            JLabel lblBadge = new JLabel(topBadge);
            lblBadge.setFont(new Font("Monospaced", Font.BOLD, 11));
            lblBadge.setForeground(COLOR_PRIMARY);
            lblBadge.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(lblBadge);
            panel.add(Box.createVerticalStrut(6));
        }

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(COLOR_TEXT_DARK);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(lblTitle);

        panel.add(Box.createVerticalStrut(4));

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(COLOR_TEXT_MUTED);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(lblSub);

        return panel;
    }

    private JPanel createStyledTextFieldContainer(JTextField field, String placeholder, String prefixIcon) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(COLOR_TEXT_DARK);
        field.setBackground(COLOR_INPUT_BG);
        field.setCaretColor(COLOR_PRIMARY);
        field.setOpaque(false);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        int leftPadding = 12;
        if (prefixIcon != null) {
            JLabel lblPrefix = new JLabel("  " + prefixIcon + "  ");
            lblPrefix.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblPrefix.setForeground(COLOR_TEXT_MUTED);
            wrapper.add(lblPrefix, BorderLayout.WEST);
            leftPadding = 4;
        }

        field.setBorder(new EmptyBorder(6, leftPadding, 6, 12));

        JPanel container = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(COLOR_INPUT_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));

                boolean focused = field.isFocusOwner();
                g2.setColor(focused ? COLOR_PRIMARY : COLOR_INPUT_BORDER);
                g2.setStroke(new BasicStroke(focused ? 1.5f : 1.0f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));

                g2.dispose();
            }
        };
        container.setOpaque(false);
        container.setMaximumSize(new Dimension(380, 38));
        container.setPreferredSize(new Dimension(380, 38));
        container.add(wrapper, BorderLayout.CENTER);
        wrapper.add(field, BorderLayout.CENTER);

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) { container.repaint(); }
            @Override
            public void focusLost(FocusEvent e) { container.repaint(); }
        });

        return container;
    }

    private JPanel createStyledPasswordFieldContainer(JPasswordField field, String placeholder, String prefixIcon) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(COLOR_TEXT_DARK);
        field.setBackground(COLOR_INPUT_BG);
        field.setCaretColor(COLOR_PRIMARY);
        field.setOpaque(false);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);

        int leftPadding = 12;
        if (prefixIcon != null) {
            JLabel lblPrefix = new JLabel("  " + prefixIcon + "  ");
            lblPrefix.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lblPrefix.setForeground(COLOR_TEXT_MUTED);
            wrapper.add(lblPrefix, BorderLayout.WEST);
            leftPadding = 4;
        }

        field.setBorder(new EmptyBorder(6, leftPadding, 6, 4));

        JLabel lblEye = new JLabel(new EyeIcon(false, COLOR_TEXT_MUTED));
        lblEye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblEye.setToolTipText("Nh\u1EA5p đ\u1EC3 \u1EA9n/hi\u1EC7n m\u1EADt kh\u1EA9u");
        lblEye.setBorder(new EmptyBorder(0, 4, 0, 8));

        char defaultEcho = field.getEchoChar();
        lblEye.addMouseListener(new MouseAdapter() {
            private boolean showing = false;
            @Override
            public void mouseClicked(MouseEvent e) {
                showing = !showing;
                field.setEchoChar(showing ? (char) 0 : defaultEcho);
                lblEye.setIcon(showing ? new EyeIcon(true, COLOR_PRIMARY) : new EyeIcon(false, COLOR_TEXT_MUTED));
            }
        });

        wrapper.add(lblEye, BorderLayout.EAST);

        JPanel container = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(COLOR_INPUT_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));

                boolean focused = field.isFocusOwner();
                g2.setColor(focused ? COLOR_PRIMARY : COLOR_INPUT_BORDER);
                g2.setStroke(new BasicStroke(focused ? 1.5f : 1.0f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));

                g2.dispose();
            }
        };
        container.setOpaque(false);
        container.setMaximumSize(new Dimension(380, 38));
        container.setPreferredSize(new Dimension(380, 38));
        container.add(wrapper, BorderLayout.CENTER);
        wrapper.add(field, BorderLayout.CENTER);

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) { container.repaint(); }
            @Override
            public void focusLost(FocusEvent e) { container.repaint(); }
        });

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

    private static class RoundedCardPanel extends JPanel {
        private final int cornerRadius;
        private final Color bgColor;

        public RoundedCardPanel(int radius, Color bg) {
            this.cornerRadius = radius;
            this.bgColor = bg;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(0, 0, 0, 8));
            g2.fillRoundRect(2, 4, getWidth() - 4, getHeight() - 4, cornerRadius, cornerRadius);

            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);

            g2.setColor(new Color(226, 232, 240));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);

            g2.dispose();
        }
    }

    private static class ModernButton extends JButton {
        private final Color bgNormal;
        private final Color bgHover;
        private boolean isHovered = false;

        public ModernButton(String text, Color bgNormal, Color bgHover, Color textColor) {
            super(text);
            this.bgNormal = bgNormal;
            this.bgHover = bgHover;

            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setForeground(textColor);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.CENTER_ALIGNMENT);
            setMaximumSize(new Dimension(380, 44));
            setPreferredSize(new Dimension(380, 44));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setColor(isHovered ? bgHover : bgNormal);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
