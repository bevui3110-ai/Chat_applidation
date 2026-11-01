package client.gui;

import common.MessagePacket;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;

public class GroupInfoDialog {

    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);
    private static final Color COLOR_BG_LIGHT = new Color(248, 250, 252);
    private static final Color COLOR_CARD_BG = Color.WHITE;
    private static final Color COLOR_TEXT_DARK = new Color(15, 23, 42);
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);
    private static final Color COLOR_BORDER = new Color(226, 232, 240);

    // Kích thước chuẩn đồng đều 100% cho tất cả các nút dưới đáy (không bị cái to cái nhỏ)
    private static final int BTN_WIDTH = 120;
    private static final int BTN_HEIGHT = 38;

    public static void showDialog(JFrame parent, String username, String groupName, List<String> rawItemList, client.network.ClientSocketHandler socketHandler) {
        showDialog(parent, username, groupName, rawItemList, socketHandler, null);
    }

    public static void showDialog(JFrame parent, String username, String groupName, List<String> rawItemList, client.network.ClientSocketHandler socketHandler, Runnable onUpdateCallback) {
        boolean isCommunity = "Cộng đồng".equals(groupName);
        JDialog dialog = new JDialog(parent, isCommunity ? "Cộng đồng" : "Thông tin nhóm: " + groupName, true);
        dialog.setSize(580, 640);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);
        dialog.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(COLOR_BG_LIGHT);

        // Lấy thông tin người tạo nhóm và danh sách thành viên từ rawItemList
        String groupCreator = "";
        Set<String> groupMemberSet = new HashSet<>();
        if (!isCommunity) {
            for (String item : rawItemList) {
                if (item.startsWith("[GROUP]")) {
                    String content = item.substring(7);
                    String[] parts = content.split(":");
                    if (parts[0].equals(groupName) && parts.length > 1) {
                        if (parts.length > 2) {
                            groupCreator = parts[1];
                            String[] mArr = parts[2].split(";");
                            for (String m : mArr) {
                                if (!m.trim().isEmpty()) groupMemberSet.add(m.trim());
                            }
                        } else {
                            String[] mArr = parts[1].split(";");
                            for (String m : mArr) {
                                if (!m.trim().isEmpty()) groupMemberSet.add(m.trim());
                            }
                        }
                    }
                }
            }
        }

        // HEADER BANNER - Không dùng emoji gây lỗi font hình ô vuông
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(COLOR_PRIMARY);
        headerPanel.setBorder(new EmptyBorder(18, 22, 18, 22));

        String titleStr = isCommunity ? "Phòng Cộng đồng" : groupName;
        JLabel lblTitle = new JLabel(titleStr);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 19));
        lblTitle.setForeground(Color.WHITE);

        String subText;
        if (isCommunity) {
            subText = "Danh sách thành viên";
        } else if (!groupCreator.isEmpty()) {
            subText = "<html><b>Người tạo nhóm:</b> " + groupCreator + " &bull; " + groupMemberSet.size() + " thành viên</html>";
        } else {
            subText = "Thông tin nhóm & Danh sách thành viên";
        }
        JLabel lblSub = new JLabel(subText);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(new Color(219, 234, 254));

        JPanel headerText = new JPanel();
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.setOpaque(false);
        headerText.add(lblTitle);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(lblSub);

        headerPanel.add(headerText, BorderLayout.WEST);

        // SEARCH & MEMBERS PANEL
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(14, 18, 10, 18));

        JTextField txtSearch = new JTextField();
        txtSearch.setToolTipText("Tìm kiếm thành viên...");
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearch.setBackground(COLOR_CARD_BG);
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        centerPanel.add(txtSearch, BorderLayout.NORTH);

        JPanel membersContainer = new JPanel();
        membersContainer.setLayout(new BoxLayout(membersContainer, BoxLayout.Y_AXIS));
        membersContainer.setBackground(COLOR_BG_LIGHT);

        // Danh sách thành viên: Đặt chính mình (Bạn) lên đầu, tiếp đến Trưởng nhóm (nếu có), rồi đến các thành viên khác
        List<String> candidateUsers = new ArrayList<>();
        if (isCommunity) {
            candidateUsers.add(username);
            for (String item : rawItemList) {
                if (!item.equals("Cộng đồng") && !item.startsWith("[GROUP]")) {
                    String uName = item.split(":")[0];
                    if (!uName.equals(username) && !candidateUsers.contains(uName)) {
                        candidateUsers.add(uName);
                    }
                }
            }
        } else {
            if (groupMemberSet.contains(username)) {
                candidateUsers.add(username);
            }
            if (!groupCreator.isEmpty() && !groupCreator.equals(username) && groupMemberSet.contains(groupCreator)) {
                candidateUsers.add(groupCreator);
            }
            for (String m : groupMemberSet) {
                if (!candidateUsers.contains(m)) {
                    candidateUsers.add(m);
                }
            }
        }

        final String finalCreator = groupCreator;
        final Runnable[] refreshRef = new Runnable[1];
        refreshRef[0] = () -> {
            membersContainer.removeAll();
            String query = txtSearch.getText().trim().toLowerCase();

            for (String uName : candidateUsers) {
                boolean isSelf = uName.equals(username);
                boolean isLeader = uName.equals(finalCreator);

                String nickname = isCommunity ? NicknameDialog.getPrivateNickname(username, uName) : NicknameDialog.getGroupNickname(groupName, uName);
                String dispName = (nickname != null && !nickname.trim().isEmpty()) ? nickname.trim() : uName;
                String searchKey = (uName + " " + dispName).toLowerCase();

                if (query.isEmpty() || searchKey.contains(query)) {
                    // Card thành viên với kích thước chuẩn đồng nhất (chiều cao 62px)
                    JPanel userCard = new JPanel(new BorderLayout(14, 0));
                    userCard.setBackground(COLOR_CARD_BG);
                    userCard.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
                            new EmptyBorder(10, 16, 10, 16)
                    ));
                    userCard.setPreferredSize(new Dimension(520, 62));
                    userCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
                    userCard.setAlignmentX(Component.CENTER_ALIGNMENT);

                    // Avatar tròn 40x40
                    JPanel avatar = new JPanel() {
                        @Override
                        protected void paintComponent(Graphics g) {
                            super.paintComponent(g);
                            Graphics2D g2 = (Graphics2D) g.create();
                            AvatarUtils.drawAvatar(g2, uName, getWidth(), getHeight());
                            g2.dispose();
                        }
                    };
                    avatar.setPreferredSize(new Dimension(40, 40));
                    avatar.setOpaque(false);

                    // Khối Tên & Vai trò:
                    // Dòng 1: Tên hiển thị (DispName) + (Bạn) nếu là chính mình
                    // Dòng 2: Trưởng nhóm (màu xanh nổi bật) hoặc @username
                    // TUYỆT ĐỐI XÓA BỎ CHỮ ONLINE / OFFLINE ĐỂ DANH SÁCH ĐỒNG ĐỀU, CÂN ĐỐI 100%
                    JPanel nameCol = new JPanel();
                    nameCol.setLayout(new BoxLayout(nameCol, BoxLayout.Y_AXIS));
                    nameCol.setOpaque(false);

                    String mainTitle = isSelf ? (dispName + " (Bạn)") : dispName;
                    JLabel lblName = new JLabel(mainTitle);
                    lblName.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    lblName.setForeground(COLOR_TEXT_DARK);

                    StringBuilder subHtml = new StringBuilder("<html>");
                    if (isLeader) {
                        subHtml.append("<b style='color:#2563EB;'>Trưởng nhóm</b>");
                        if (nickname != null && !nickname.trim().isEmpty()) {
                            subHtml.append(" &bull; <span style='color:#64748B;'>@").append(uName).append("</span>");
                        }
                    } else {
                        if (nickname != null && !nickname.trim().isEmpty()) {
                            subHtml.append("<span style='color:#64748B;'>@").append(uName).append("</span>");
                        } else {
                            subHtml.append("<span style='color:#94A3B8;'>@").append(uName).append("</span>");
                        }
                    }
                    subHtml.append("</html>");

                    JLabel lblDetail = new JLabel(subHtml.toString());
                    lblDetail.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    lblDetail.setForeground(COLOR_TEXT_MUTED);

                    nameCol.add(Box.createVerticalGlue());
                    nameCol.add(lblName);
                    nameCol.add(Box.createVerticalStrut(2));
                    nameCol.add(lblDetail);
                    nameCol.add(Box.createVerticalGlue());

                    userCard.add(avatar, BorderLayout.WEST);
                    userCard.add(nameCol, BorderLayout.CENTER);

                    // Nút Đặt biệt danh (nếu không phải là chính mình)
                    if (!isSelf) {
                        JButton btnSetNick = new JButton("Biệt danh");
                        btnSetNick.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                        btnSetNick.setForeground(COLOR_TEXT_DARK);
                        btnSetNick.setBackground(new Color(241, 245, 249));
                        btnSetNick.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1, true));
                        btnSetNick.setFocusPainted(false);
                        btnSetNick.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        btnSetNick.setPreferredSize(new Dimension(84, 30));

                        btnSetNick.addActionListener(ev -> {
                            if (isCommunity) {
                                NicknameDialog.showDialog(dialog, username, uName, (targetUser, newNick) -> {
                                    if (refreshRef[0] != null) refreshRef[0].run();
                                    if (onUpdateCallback != null) onUpdateCallback.run();
                                });
                            } else {
                                NicknameDialog.showGroupNicknameDialog(dialog, groupName, uName, (targetUser, newNick) -> {
                                    if (refreshRef[0] != null) refreshRef[0].run();
                                    if (onUpdateCallback != null) onUpdateCallback.run();
                                });
                            }
                        });

                        JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 5));
                        btnWrapper.setOpaque(false);
                        btnWrapper.add(btnSetNick);
                        userCard.add(btnWrapper, BorderLayout.EAST);
                    }

                    membersContainer.add(userCard);
                    membersContainer.add(Box.createVerticalStrut(8));
                }
            }
            membersContainer.revalidate();
            membersContainer.repaint();
        };

        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                if (refreshRef[0] != null) refreshRef[0].run();
            }
        });

        if (refreshRef[0] != null) refreshRef[0].run();

        // JScrollPane: Đặt HORIZONTAL_SCROLLBAR_NEVER để không bao giờ có thanh cuộn ngang lố lăng
        JScrollPane scroll = new JScrollPane(membersContainer);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        centerPanel.add(scroll, BorderLayout.CENTER);

        // BOTTOM ACTION PANEL: TẤT CẢ CÁC NÚT ĐỀU CÓ KÍCH THƯỚC CHUẨN ĐỒNG NHẤT (120px x 38px), CÙNG MỘ HÀNG
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        bottomPanel.setBackground(COLOR_CARD_BG);
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BORDER));

        if (!isCommunity) {
            // Nút "Thêm thành viên" (Kích thước chuẩn: 120 x 38)
            JButton btnAddMember = createUniformButton("Thêm thành viên", COLOR_PRIMARY, Color.WHITE);
            btnAddMember.addActionListener(e -> {
                dialog.dispose();
                AddGroupMemberDialog.showDialog(parent, username, groupName, rawItemList, socketHandler, str -> {
                    if (str.startsWith("[GROUP]")) return str.substring(7).split(":")[0];
                    if (str.contains(":")) return str.split(":")[0];
                    return str;
                });
            });
            bottomPanel.add(btnAddMember);

            // Nút "Rời nhóm" (Kích thước chuẩn: 120 x 38)
            JButton btnLeave = createUniformButton("Rời nhóm", new Color(254, 243, 199), new Color(180, 83, 9));
            btnLeave.setBorder(BorderFactory.createLineBorder(new Color(251, 191, 36), 1, true));
            btnLeave.addActionListener(e -> {
                boolean isLeader = username.equals(finalCreator);
                String msg = isLeader
                        ? "Bạn là Trưởng nhóm. Nếu bạn rời nhóm, quyền Trưởng nhóm sẽ được tự động chuyển giao cho thành viên kế tiếp.\nBạn có chắc chắn muốn rời nhóm?"
                        : "Bạn có chắc chắn muốn rời khỏi nhóm '" + groupName + "' không?";
                int ans = JOptionPane.showConfirmDialog(dialog, msg, "Xác nhận rời nhóm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (ans == JOptionPane.YES_OPTION) {
                    try {
                        socketHandler.sendPacket(new MessagePacket("LEAVE_GROUP", username, "", groupName));
                        dialog.dispose();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            bottomPanel.add(btnLeave);

            // Nút "Xóa nhóm" (Kích thước chuẩn: 120 x 38 - Chỉ hiển thị cho Trưởng nhóm)
            if (username.equals(finalCreator)) {
                JButton btnDelete = createUniformButton("Xóa nhóm", new Color(220, 38, 38), Color.WHITE);
                btnDelete.addActionListener(e -> {
                    int ans = JOptionPane.showConfirmDialog(dialog,
                            "CẢNH BÁO: Bạn có chắc chắn muốn giải tán và XÓA VĨNH VIỄN nhóm '" + groupName + "' không?\nToàn bộ dữ liệu thành viên sẽ bị xóa!",
                            "Xác nhận xóa nhóm", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
                    if (ans == JOptionPane.YES_OPTION) {
                        try {
                            socketHandler.sendPacket(new MessagePacket("DELETE_GROUP", username, "", groupName));
                            dialog.dispose();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
                bottomPanel.add(btnDelete);
            }
        }

        // Nút "Đóng" (Kích thước chuẩn: 120 x 38)
        JButton btnClose = createUniformButton("Đóng", COLOR_CARD_BG, COLOR_TEXT_DARK);
        btnClose.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1, true));
        btnClose.addActionListener(e -> dialog.dispose());
        bottomPanel.add(btnClose);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    /**
     * Tạo nút với kích thước chuẩn đồng nhất 100% (120 x 38), chữ nằm trên 1 hàng không ngắt dòng
     */
    private static JButton createUniformButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        Dimension dim = new Dimension(BTN_WIDTH, BTN_HEIGHT);
        btn.setPreferredSize(dim);
        btn.setMinimumSize(dim);
        btn.setMaximumSize(dim);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        return btn;
    }
}
