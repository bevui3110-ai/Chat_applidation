package client.gui;

import common.MessagePacket;
import client.network.ClientSocketHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class CreateGroupDialog {

    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);
    private static final Color COLOR_PRIMARY_HOVER = new Color(29, 78, 216);
    private static final Color COLOR_BG_LIGHT = new Color(248, 250, 252);
    private static final Color COLOR_CARD_BG = Color.WHITE;
    private static final Color COLOR_TEXT_DARK = new Color(15, 23, 42);
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);
    private static final Color COLOR_BORDER = new Color(226, 232, 240);
    private static final Color COLOR_INPUT_BG = new Color(241, 245, 249);

    public static void showDialog(JFrame parent, String username, List<String> rawItemList, ClientSocketHandler socketHandler, Function<String, String> nameCleaner) {
        JDialog dialog = new JDialog(parent, "T\u1EA1o nhóm chat m\u1EDBi", true);
        dialog.setSize(420, 530);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(COLOR_BG_LIGHT);

        // HEADER BANNER
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(COLOR_PRIMARY);
        headerPanel.setBorder(new EmptyBorder(16, 18, 16, 18));

        JLabel lblTitle = new JLabel("\uD83D\uDC65 T\u1EA1o nhóm chat m\u1EDBi");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Ch\u1ECDn b\u1EA1n bè đ\u1EC3 thành l\u1EADp nhóm h\u1ED9i tho\u1EA1i");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(219, 234, 254));

        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setOpaque(false);
        headerText.add(lblTitle);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(lblSub);

        headerPanel.add(headerText, BorderLayout.WEST);

        // FORM & MEMBERS CONTAINER
        JPanel centerContent = new JPanel(new BorderLayout(0, 10));
        centerContent.setOpaque(false);
        centerContent.setBorder(new EmptyBorder(12, 14, 10, 14));

        // Group Name Input Box
        JPanel inputCard = new JPanel(new BorderLayout(8, 0));
        inputCard.setOpaque(false);

        JLabel lblGroupName = new JLabel("Tên nhóm chat:");
        lblGroupName.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblGroupName.setForeground(COLOR_TEXT_DARK);

        JTextField txtGroupName = new JTextField();
        txtGroupName.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtGroupName.setBackground(COLOR_CARD_BG);
        txtGroupName.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));

        JPanel nameBox = new JPanel(new BorderLayout(0, 4));
        nameBox.setOpaque(false);
        nameBox.add(lblGroupName, BorderLayout.NORTH);
        nameBox.add(txtGroupName, BorderLayout.CENTER);

        centerContent.add(nameBox, BorderLayout.NORTH);

        // Members list
        JPanel membersPanel = new JPanel();
        membersPanel.setLayout(new BoxLayout(membersPanel, BoxLayout.Y_AXIS));
        membersPanel.setBackground(COLOR_BG_LIGHT);

        List<JCheckBox> checkBoxes = new ArrayList<>();
        for (String item : rawItemList) {
            if (!item.equals("C\u1ED9ng đ\u1ED3ng") && !item.startsWith("[GROUP]")) {
                String cleanName = nameCleaner.apply(item);
                if (!cleanName.equals(username)) {
                    boolean isOnline = item.contains(":ONLINE");

                    JPanel userCard = new JPanel(new BorderLayout(10, 0));
                    userCard.setBackground(COLOR_CARD_BG);
                    userCard.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
                            new EmptyBorder(8, 10, 8, 10)
                    ));
                    userCard.setMaximumSize(new Dimension(370, 50));
                    userCard.setPreferredSize(new Dimension(370, 50));

                    JPanel avatar = new JPanel() {
                        @Override
                        protected void paintComponent(Graphics g) {
                            super.paintComponent(g);
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(isOnline ? COLOR_PRIMARY : new Color(148, 163, 184));
                            g2.fillOval(0, 0, 32, 32);
                            g2.setColor(Color.WHITE);
                            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                            String init = cleanName.length() > 0 ? cleanName.substring(0, 1).toUpperCase() : "U";
                            FontMetrics fm = g2.getFontMetrics();
                            g2.drawString(init, (32 - fm.stringWidth(init)) / 2, 21);
                            g2.dispose();
                        }
                    };
                    avatar.setPreferredSize(new Dimension(32, 32));
                    avatar.setOpaque(false);

                    JPanel nameCol = new JPanel();
                    nameCol.setLayout(new BoxLayout(nameCol, BoxLayout.Y_AXIS));
                    nameCol.setOpaque(false);

                    JLabel lblName = new JLabel(cleanName);
                    lblName.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    lblName.setForeground(COLOR_TEXT_DARK);

                    JLabel lblStatus = new JLabel(isOnline ? "\uD83D\uDFE2 Online" : "\u26AA Offline");
                    lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                    lblStatus.setForeground(isOnline ? new Color(16, 185, 129) : COLOR_TEXT_MUTED);

                    nameCol.add(lblName);
                    nameCol.add(lblStatus);

                    JCheckBox cb = new JCheckBox();
                    cb.setOpaque(false);

                    userCard.add(avatar, BorderLayout.WEST);
                    userCard.add(nameCol, BorderLayout.CENTER);
                    userCard.add(cb, BorderLayout.EAST);

                    checkBoxes.add(cb);
                    cb.putClientProperty("username", cleanName);

                    membersPanel.add(userCard);
                    membersPanel.add(Box.createVerticalStrut(6));
                }
            }
        }

        JScrollPane scroll = new JScrollPane(membersPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        centerContent.add(scroll, BorderLayout.CENTER);

        // BOTTOM ACTION BUTTONS
        JPanel bottomBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        bottomBtnPanel.setBackground(COLOR_CARD_BG);
        bottomBtnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BORDER));

        JButton btnConfirm = createStyledButton("T\u1EA1o nhóm", COLOR_PRIMARY, COLOR_PRIMARY_HOVER, Color.WHITE);
        btnConfirm.setPreferredSize(new Dimension(110, 38));

        JButton btnCancel = createStyledButton("H\u1EE7y", COLOR_CARD_BG, COLOR_BG_LIGHT, COLOR_TEXT_DARK);
        btnCancel.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1, true));
        btnCancel.setPreferredSize(new Dimension(80, 38));

        btnConfirm.addActionListener(e -> {
            String gName = txtGroupName.getText().trim();
            if (gName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng nh\u1EADp tên nhóm chat!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            List<String> selectedUsers = new ArrayList<>();
            for (JCheckBox cb : checkBoxes) {
                if (cb.isSelected()) {
                    selectedUsers.add((String) cb.getClientProperty("username"));
                }
            }
            if (selectedUsers.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng ch\u1ECDn ít nh\u1EA5t 1 thành viên!", "Thông báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String packetContent = gName + ":" + String.join(",", selectedUsers);
            MessagePacket packet = new MessagePacket("CREATE_GROUP", username, "", packetContent);
            try {
                socketHandler.sendPacket(packet);
                JOptionPane.showMessageDialog(dialog, "Đã g\u1EEDi yêu c\u1EA7u t\u1EA1o nhóm chat thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Không th\u1EC3 t\u1EA1o nhóm!", "L\u1ED7i", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnCancel.addActionListener(e -> dialog.dispose());

        bottomBtnPanel.add(btnCancel);
        bottomBtnPanel.add(btnConfirm);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(centerContent, BorderLayout.CENTER);
        mainPanel.add(bottomBtnPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private static JButton createStyledButton(String text, Color bgNormal, Color bgHover, Color fgText) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(fgText);
        button.setBackground(bgNormal);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { button.setBackground(bgHover); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { button.setBackground(bgNormal); }
        });
        return button;
    }
}
