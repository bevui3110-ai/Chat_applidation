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

public class LoginForm extends JFrame {

    private JTextField txtLoginIP;
    private JTextField txtLoginPort;
    private JTextField txtLoginUser;
    private JPasswordField txtLoginPass;
    private JCheckBox chkRemember;

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

    public LoginForm() {
        this("127.0.0.1", "12345", "");
    }

    public LoginForm(String ip, String portStr, String defaultUser) {
        setTitle("ChatChit Client");
        setSize(460, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel bgPanel = new JPanel(new GridBagLayout());
        bgPanel.setBackground(COLOR_BG_LIGHT);
        bgPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel loginCard = buildLoginCard(ip, portStr, defaultUser);
        bgPanel.add(loginCard);
        add(bgPanel);
    }

    private JPanel buildLoginCard(String defaultIP, String defaultPort, String defaultUser) {
        RoundedCardPanel card = new RoundedCardPanel(20, COLOR_CARD_BG);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(30, 28, 25, 28));

        // 1. Header (Icon + Title + Subtitle)
        card.add(createHeaderPanel("ChatChit Client", "Connect to TCP/IP Chat Server"));
        card.add(Box.createVerticalStrut(20));

        // 2. Server IP & Port Row (PRE-FILLED DEFAULTS, FULLY EDITABLE FOR LAN/REMOTE)
        JPanel ipPortPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        ipPortPanel.setOpaque(false);
        ipPortPanel.setMaximumSize(new Dimension(380, 65));

        // Server IP Column
        JPanel ipCol = new JPanel();
        ipCol.setLayout(new BoxLayout(ipCol, BoxLayout.Y_AXIS));
        ipCol.setOpaque(false);
        JPanel ipLabelRow = new JPanel(new BorderLayout());
        ipLabelRow.setOpaque(false);
        JLabel lblIP = new JLabel("Server IP");
        lblIP.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblIP.setForeground(COLOR_TEXT_DARK);
        JLabel lblV4 = new JLabel("v4/v6");
        lblV4.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lblV4.setForeground(COLOR_TEXT_MUTED);
        ipLabelRow.add(lblIP, BorderLayout.WEST);
        ipLabelRow.add(lblV4, BorderLayout.EAST);

        txtLoginIP = new JTextField(defaultIP != null ? defaultIP : "127.0.0.1");
        JPanel ipContainer = createStyledTextFieldContainer(txtLoginIP, "127.0.0.1", null);
        ipCol.add(ipLabelRow);
        ipCol.add(Box.createVerticalStrut(4));
        ipCol.add(ipContainer);

        // Port Column
        JPanel portCol = new JPanel();
        portCol.setLayout(new BoxLayout(portCol, BoxLayout.Y_AXIS));
        portCol.setOpaque(false);
        JPanel portLabelRow = new JPanel(new BorderLayout());
        portLabelRow.setOpaque(false);
        JLabel lblPort = new JLabel("Port");
        lblPort.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPort.setForeground(COLOR_TEXT_DARK);
        JLabel lblPortType = new JLabel("TCP");
        lblPortType.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lblPortType.setForeground(COLOR_TEXT_MUTED);
        portLabelRow.add(lblPort, BorderLayout.WEST);
        portLabelRow.add(lblPortType, BorderLayout.EAST);

        txtLoginPort = new JTextField(defaultPort != null ? defaultPort : "12345");
        JPanel portContainer = createStyledTextFieldContainer(txtLoginPort, "12345", null);
        portCol.add(portLabelRow);
        portCol.add(Box.createVerticalStrut(4));
        portCol.add(portContainer);

        ipPortPanel.add(ipCol);
        ipPortPanel.add(portCol);
        card.add(ipPortPanel);
        card.add(Box.createVerticalStrut(14));

        // 3. Username Row
        JPanel userPanel = new JPanel();
        userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.Y_AXIS));
        userPanel.setOpaque(false);
        userPanel.setMaximumSize(new Dimension(380, 65));
        JPanel userLabelRow = new JPanel(new BorderLayout());
        userLabelRow.setOpaque(false);
        JLabel lblUser = new JLabel("Username");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUser.setForeground(COLOR_TEXT_DARK);
        JLabel lblUserReq = new JLabel("Required");
        lblUserReq.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblUserReq.setForeground(COLOR_TEXT_MUTED);
        userLabelRow.add(lblUser, BorderLayout.WEST);
        userLabelRow.add(lblUserReq, BorderLayout.EAST);

        txtLoginUser = new JTextField(defaultUser != null ? defaultUser : "");
        JPanel userContainer = createStyledTextFieldContainer(txtLoginUser, "Enter username", null);

        userPanel.add(userLabelRow);
        userPanel.add(Box.createVerticalStrut(4));
        userPanel.add(userContainer);
        card.add(userPanel);
        card.add(Box.createVerticalStrut(14));

        // 4. Password Row
        JPanel passPanel = new JPanel();
        passPanel.setLayout(new BoxLayout(passPanel, BoxLayout.Y_AXIS));
        passPanel.setOpaque(false);
        passPanel.setMaximumSize(new Dimension(380, 65));

        JPanel passLabelRow = new JPanel(new BorderLayout());
        passLabelRow.setOpaque(false);
        JLabel lblPass = new JLabel("Password");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPass.setForeground(COLOR_TEXT_DARK);
        JLabel lblHelp = new JLabel("Need help?");
        lblHelp.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblHelp.setForeground(COLOR_PRIMARY);
        lblHelp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        passLabelRow.add(lblPass, BorderLayout.WEST);
        passLabelRow.add(lblHelp, BorderLayout.EAST);

        txtLoginPass = new JPasswordField();
        JPanel passContainer = createStyledPasswordFieldContainer(txtLoginPass, "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022", null);

        passPanel.add(passLabelRow);
        passPanel.add(Box.createVerticalStrut(4));
        passPanel.add(passContainer);
        card.add(passPanel);
        card.add(Box.createVerticalStrut(14));

        // 5. Options Row (Remember Host + TCP/NIO pill)
        JPanel optRow = new JPanel(new BorderLayout());
        optRow.setOpaque(false);
        optRow.setMaximumSize(new Dimension(380, 30));

        chkRemember = new JCheckBox("Remember host node");
        chkRemember.setSelected(true);
        chkRemember.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chkRemember.setForeground(COLOR_TEXT_DARK);
        chkRemember.setOpaque(false);
        chkRemember.setFocusPainted(false);

        JLabel lblPill = new JLabel(" TCP/NIO ");
        lblPill.setOpaque(true);
        lblPill.setBackground(COLOR_SECONDARY_BG);
        lblPill.setForeground(COLOR_PRIMARY);
        lblPill.setFont(new Font("Monospaced", Font.BOLD, 10));
        lblPill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(219, 234, 254), 1, true),
                new EmptyBorder(2, 6, 2, 6)
        ));

        optRow.add(chkRemember, BorderLayout.WEST);
        optRow.add(lblPill, BorderLayout.EAST);
        card.add(optRow);
        card.add(Box.createVerticalStrut(20));

        // 6. Action Buttons
        ModernButton btnLogin = new ModernButton("\u2192  Connect & Login", COLOR_PRIMARY, COLOR_PRIMARY_HOVER, Color.WHITE);
        btnLogin.addActionListener(e -> processLogin());
        card.add(btnLogin);
        card.add(Box.createVerticalStrut(10));

        ModernButton btnGoRegister = new ModernButton("Register New Account", COLOR_SECONDARY_BG, COLOR_SECONDARY_HOVER, COLOR_PRIMARY);
        btnGoRegister.addActionListener(e -> {
            RegisterForm registerForm = new RegisterForm(txtLoginIP.getText().trim(), txtLoginPort.getText().trim());
            registerForm.setVisible(true);
            dispose();
        });
        card.add(btnGoRegister);
        card.add(Box.createVerticalStrut(20));

        // 7. Status Footer
        JPanel statusFooter = new JPanel(new BorderLayout(8, 0));
        statusFooter.setOpaque(true);
        statusFooter.setBackground(COLOR_SECONDARY_BG);
        statusFooter.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(219, 234, 254), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        statusFooter.setMaximumSize(new Dimension(380, 36));

        JLabel lblStatusLeft = new JLabel("<html><font color='#10B981'>●</font> <b>Server Status:</b> Ready</html>");
        lblStatusLeft.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatusLeft.setForeground(COLOR_TEXT_DARK);

        JLabel lblStatusRight = new JLabel("0ms RTT");
        lblStatusRight.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lblStatusRight.setForeground(COLOR_TEXT_MUTED);

        statusFooter.add(lblStatusLeft, BorderLayout.WEST);
        statusFooter.add(lblStatusRight, BorderLayout.EAST);
        card.add(statusFooter);

        return card;
    }

    private void processLogin() {
        String ip = txtLoginIP.getText().trim();
        if (ip.isEmpty()) ip = "127.0.0.1";

        int port;
        try {
            String portStr = txtLoginPort.getText().trim();
            port = Integer.parseInt(portStr.isEmpty() ? "12345" : portStr);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Port ph\u1EA3i là m\u1ED9t s\u1ED1 nguyên h\u1EE3p l\u1EC7!", "L\u1ED7i k\u1EBFt n\u1ED1i", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String username = txtLoginUser.getText().trim();
        String password = new String(txtLoginPass.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nh\u1EADp đ\u1EA7y đ\u1EE7 Username và Password!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ClientSocketHandler handler = null;
        try {
            handler = new ClientSocketHandler(ip, port);
            MessagePacket packet = new MessagePacket("LOGIN", username, "", password);
            handler.sendPacket(packet);

            MessagePacket response = handler.readPacket();
            if (response != null) {
                if ("LOGIN_SUCCESS".equals(response.getType())) {
                    JOptionPane.showMessageDialog(this, "Đăng nh\u1EADp thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                    try {
                        ChatForm chatForm = new ChatForm(handler, username);
                        chatForm.setVisible(true);
                        dispose();
                    } catch (Throwable t) {
                        t.printStackTrace();
                        JOptionPane.showMessageDialog(this, "Lỗi khi mở giao diện Chat: " + t.getMessage(), "Lỗi khởi chạy", JOptionPane.ERROR_MESSAGE);
                        if (handler != null) handler.close();
                    }
                } else {
                    JOptionPane.showMessageDialog(this, response.getContent(), "Đăng nh\u1EADp th\u1EA5t b\u1EA1i", JOptionPane.ERROR_MESSAGE);
                    handler.close();
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Không th\u1EC3 k\u1EBFt n\u1ED1i đ\u1EBFn Server (" + ip + ":" + port + ")!", "L\u1ED7i k\u1EBFt n\u1ED1i", JOptionPane.ERROR_MESSAGE);
            if (handler != null) {
                handler.close();
            }
        }
    }

    // ==========================================
    // UI HELPER METHODS
    // ==========================================
    private JPanel createHeaderPanel(String title, String subtitle) {
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