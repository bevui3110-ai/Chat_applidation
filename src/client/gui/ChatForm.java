package client.gui;

import common.MessagePacket;
import client.network.ClientSocketHandler;
import client.network.FileTransferClient;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.function.Function;

public class ChatForm extends JFrame implements Runnable {

    private static final Map<String, Integer> unreadMap = new HashMap<>();
    private static final Map<String, String> snippetMap = new HashMap<>();
    private static final Map<String, String> timeMap = new HashMap<>();
    private static final Map<String, Clip> activeClipsMap = new HashMap<>();
    private static final File TEMP_DIR = new File(System.getProperty("java.io.tmpdir"), "ChatApp_TempFiles");

    private ClientSocketHandler socketHandler;
    private String username;
    private boolean running = true;
    private String currentChatTarget = "Cộng đồng";

    // Call Handler Manager
    private final CallDialog callDialogHandler = new CallDialog();

    // GUI Components
    private JLabel lblTargetTitle;
    private JLabel lblTargetSub;
    private JButton btnAddGroupMember;
    private JButton btnLeaveGroup;

    private DefaultListModel<String> listModel;
    private JList<String> userList;
    private JTextField txtSearch;

    private CardLayout cardLayout = new CardLayout();
    private JPanel chatCardPanel = new JPanel(cardLayout);
    private JTextField txtMessage;

    private Map<String, JTextPane> chatPanesMap = new HashMap<>();
    private Map<String, HTMLDocument> docMap = new HashMap<>();
    private Map<String, HTMLEditorKit> kitMap = new HashMap<>();
    private Map<String, BackgroundPanel> bgPanelMap = new HashMap<>();
    private List<String> rawItemList = new ArrayList<>();

    // Color Palette
    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);       // #2563EB
    private static final Color COLOR_PRIMARY_HOVER = new Color(29, 78, 216); // #1D4ED8
    private static final Color COLOR_BG_LIGHT = new Color(248, 250, 252);     // #F8FAFC
    private static final Color COLOR_CARD_BG = Color.WHITE;                   // #FFFFFF
    private static final Color COLOR_SIDEBAR_BG = new Color(241, 245, 249);  // #F1F5F9
    private static final Color COLOR_TEXT_DARK = new Color(15, 23, 42);       // #0F172A
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);   // #64748B
    private static final Color COLOR_BORDER = new Color(226, 232, 240);       // #E2E8F0
    private static final Color COLOR_INPUT_BG = new Color(241, 245, 249);     // #F1F5F9
    private static final Color COLOR_HEADER_BG = Color.WHITE;                  // #FFFFFF

    public ChatForm(ClientSocketHandler socketHandler, String username) {
        this.socketHandler = socketHandler;
        this.username = username;
        NicknameDialog.setSocketHandler(socketHandler);

        initUI();

        new Thread(this).start();
    }

    private void initUI() {
        setTitle("ChatChit v1.0 - " + username);
        setSize(1080, 720);
        setMinimumSize(new Dimension(880, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                running = false;
                callDialogHandler.stopCallTimers();
                if (socketHandler != null) {
                    socketHandler.close();
                }
            }
        });

        JPanel mainLayout = new JPanel(new BorderLayout());
        mainLayout.setBackground(COLOR_BG_LIGHT);

        // TOP HEADER BAR
        mainLayout.add(createAppHeaderBar(), BorderLayout.NORTH);

        // MAIN BODY CONTAINER (Khởi tạo RightChatPanel trước để toàn bộ header & viewport sẵn sàng)
        JPanel bodyPanel = new JPanel(new BorderLayout());
        bodyPanel.setOpaque(false);
        JPanel rightChat = createRightChatPanel();
        JPanel leftSidebar = createLeftSidebarPanel();
        bodyPanel.add(leftSidebar, BorderLayout.WEST);
        bodyPanel.add(rightChat, BorderLayout.CENTER);

        mainLayout.add(bodyPanel, BorderLayout.CENTER);
        setContentPane(mainLayout);

        // Khởi tạo tab mặc định & chọn an toàn sau khi toàn bộ GUI đã sẵn sàng
        createChatTab("Cộng đồng");
        if (userList != null && listModel != null && listModel.size() > 0) {
            userList.setSelectedIndex(0);
        }
    }

    // ==========================================
    // TOP NAVIGATION HEADER BAR
    // ==========================================
    private JPanel createAppHeaderBar() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(COLOR_HEADER_BG);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));
        header.setPreferredSize(new Dimension(0, 56));

        JPanel leftNav = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        leftNav.setOpaque(false);

        // Blue ChatChit Brand Badge
        JLabel lblLogo = new JLabel("<html><span style='font-family:Segoe UI Emoji; color:#2563EB;'>💬</span> <span style='font-family:Segoe UI; font-weight:bold; color:#2563EB;'>ChatChit</span></html>");
        lblLogo.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblLogo.setForeground(COLOR_PRIMARY);

        // Green Socket Active Pill Badge
        JLabel lblStatusPill = new JLabel("<html><font color='#10B981'>●</font> Socket Active</html>");
        lblStatusPill.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblStatusPill.setForeground(new Color(22, 101, 52));
        lblStatusPill.setBackground(new Color(220, 252, 231));
        lblStatusPill.setOpaque(true);
        lblStatusPill.setBorder(new EmptyBorder(3, 8, 3, 8));

        leftNav.add(lblLogo);
        leftNav.add(lblStatusPill);

        JPanel rightNav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        rightNav.setOpaque(false);

        JButton btnMainChat = createStyledButton("Main Chat", COLOR_PRIMARY, COLOR_PRIMARY_HOVER, Color.WHITE);
        btnMainChat.setPreferredSize(new Dimension(105, 34));

        JButton btnLogout = createHeaderIconButton("🚪", "Đăng xuất khỏi tài khoản");
        btnLogout.setPreferredSize(new Dimension(36, 34));
        btnLogout.setBackground(new Color(254, 242, 242));
        btnLogout.setForeground(new Color(225, 29, 72));
        btnLogout.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { btnLogout.setBackground(new Color(254, 202, 202)); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { btnLogout.setBackground(new Color(254, 242, 242)); }
        });
        btnLogout.addActionListener(e -> performLogout());

        rightNav.add(btnMainChat);
        rightNav.add(btnLogout);

        header.add(leftNav, BorderLayout.WEST);
        header.add(rightNav, BorderLayout.EAST);
        return header;
    }

    // ==========================================
    // LEFT SIDEBAR
    // ==========================================
    private JPanel createLeftSidebarPanel() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(290, 0));
        sidebar.setBackground(COLOR_SIDEBAR_BG);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, COLOR_BORDER));

        // TOP CONTAINER: PROFILE CARD + SEARCH
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);
        topContainer.setBorder(new EmptyBorder(12, 12, 8, 12));

        // Profile Card Panel
        JPanel profileCard = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        profileCard.setOpaque(false);
        profileCard.setBorder(new EmptyBorder(10, 12, 10, 12));
        profileCard.setPreferredSize(new Dimension(266, 60));
        profileCard.setMaximumSize(new Dimension(266, 60));
        profileCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        profileCard.setToolTipText("Bấm để xem Profile & Settings");

        profileCard.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                ProfileSettingsDialog.showDialog(ChatForm.this, username, socketHandler, img -> repaint());
            }
        });

        // Avatar Circle using Facebook Silhouette Default
        JPanel avatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                AvatarUtils.drawAvatar(g2, username, getWidth(), getHeight());
                g2.dispose();
            }
        };
        avatarPanel.setPreferredSize(new Dimension(38, 38));
        avatarPanel.setOpaque(false);

        // User Info Column
        JPanel infoCol = new JPanel();
        infoCol.setLayout(new BoxLayout(infoCol, BoxLayout.Y_AXIS));
        infoCol.setOpaque(false);

        JLabel lblName = new JLabel("You (" + username + ")");
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblName.setForeground(COLOR_TEXT_DARK);

        JLabel lblHandle = new JLabel("@" + username);
        lblHandle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblHandle.setForeground(COLOR_TEXT_MUTED);

        infoCol.add(lblName);
        infoCol.add(Box.createVerticalStrut(2));
        infoCol.add(lblHandle);

        // Online Pill Badge
        JLabel lblOnlineBadge = new JLabel("Online");
        lblOnlineBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblOnlineBadge.setForeground(new Color(22, 101, 52));
        lblOnlineBadge.setBackground(new Color(220, 252, 231));
        lblOnlineBadge.setOpaque(true);
        lblOnlineBadge.setBorder(new EmptyBorder(2, 6, 2, 6));

        profileCard.add(avatarPanel, BorderLayout.WEST);
        profileCard.add(infoCol, BorderLayout.CENTER);
        profileCard.add(lblOnlineBadge, BorderLayout.EAST);

        // Search Field
        txtSearch = new JTextField();
        txtSearch.setToolTipText("Tìm kiếm...");
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtSearch.setBackground(COLOR_CARD_BG);
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));

        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyReleased(java.awt.event.KeyEvent e) { filterList(); }
        });

        topContainer.add(profileCard);
        topContainer.add(Box.createVerticalStrut(10));
        topContainer.add(txtSearch);

        // LIST MODEL
        listModel = new DefaultListModel<>();
        listModel.addElement("Cộng đồng");

        userList = new JList<>(listModel);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userList.setCellRenderer(new ModernUserListRenderer(unreadMap, snippetMap, timeMap, this::getCleanTargetName, username));
        userList.setBackground(COLOR_CARD_BG);
        userList.setSelectionBackground(new Color(239, 246, 255));
        userList.setSelectionForeground(COLOR_TEXT_DARK);
        userList.setFixedCellHeight(60);

        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = userList.getSelectedValue();
                if (selected != null) {
                    currentChatTarget = getCleanTargetName(selected);
                    unreadMap.put(currentChatTarget, 0);
                    userList.repaint();

                    if (!chatPanesMap.containsKey(currentChatTarget)) {
                        createChatTab(currentChatTarget);
                    }
                    if (cardLayout != null && chatCardPanel != null) {
                        cardLayout.show(chatCardPanel, currentChatTarget);
                    }
                    updateTargetTitleHeader(selected);
                    BackgroundPanel bp = bgPanelMap.get(currentChatTarget);
                    if (bp != null) {
                        bp.setGroup(isGroup(currentChatTarget));
                        bp.refreshBackground();
                    }
                }
            }
        });

        userList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                String selected = userList.getSelectedValue();
                if (selected != null) {
                    String clean = getCleanTargetName(selected);
                    if (unreadMap.getOrDefault(clean, 0) > 0) {
                        unreadMap.put(clean, 0);
                        userList.repaint();
                    }
                }
            }
        });

        JScrollPane scrollList = new JScrollPane(userList);
        scrollList.setBorder(BorderFactory.createEmptyBorder());
        scrollList.setOpaque(false);
        scrollList.getViewport().setOpaque(false);
        scrollList.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Bottom Create Group Button Panel
        JPanel bottomBtnPanel = new JPanel(new BorderLayout());
        bottomBtnPanel.setOpaque(false);
        bottomBtnPanel.setBorder(new EmptyBorder(8, 12, 12, 12));

        JButton btnBottomCreateGroup = createStyledButton("+ Create Group", Color.WHITE, COLOR_SIDEBAR_BG, COLOR_PRIMARY);
        btnBottomCreateGroup.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBottomCreateGroup.setBorder(BorderFactory.createLineBorder(new Color(191, 219, 254), 1, true));
        btnBottomCreateGroup.setPreferredSize(new Dimension(0, 36));
        btnBottomCreateGroup.addActionListener(e -> showCreateGroupDialog());

        bottomBtnPanel.add(btnBottomCreateGroup, BorderLayout.CENTER);

        sidebar.add(topContainer, BorderLayout.NORTH);
        sidebar.add(scrollList, BorderLayout.CENTER);
        sidebar.add(bottomBtnPanel, BorderLayout.SOUTH);
        return sidebar;
    }

    private void updateTargetTitleHeader(String selected) {
        if (lblTargetTitle == null || lblTargetSub == null) return;
        lblTargetSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        if ("Cộng đồng".equals(currentChatTarget)) {
            lblTargetTitle.setText("General Chat (# Cộng đồng)");
            lblTargetSub.setText("<html><font color='#2563EB'>●</font> Public TCP Network Stream</html>");
            if (btnAddGroupMember != null) btnAddGroupMember.setVisible(false);
            if (btnLeaveGroup != null) btnLeaveGroup.setVisible(false);
        } else if (selected != null && selected.startsWith("[GROUP]")) {
            lblTargetTitle.setText(currentChatTarget + " [Group]");
            lblTargetSub.setText("<html><font color='#2563EB'>●</font> TCP Group Channel</html>");
            if (btnAddGroupMember != null) btnAddGroupMember.setVisible(false);
            if (btnLeaveGroup != null) btnLeaveGroup.setVisible(false);
        } else {
            boolean isOnline = isUserOnline(currentChatTarget);
            String dispName = NicknameDialog.getDisplayName(username, currentChatTarget);
            lblTargetTitle.setText(dispName);
            if (isOnline) {
                lblTargetSub.setText("<html><font color='#10B981'>●</font> <b>Online</b> - Direct Message</html>");
            } else {
                lblTargetSub.setText("<html><font color='#94A3B8'>○</font> <i>Offline</i> - Direct Message</html>");
            }
            if (btnAddGroupMember != null) btnAddGroupMember.setVisible(false);
            if (btnLeaveGroup != null) btnLeaveGroup.setVisible(false);
        }
    }

    private boolean isUserOnline(String user) {
        for (String item : rawItemList) {
            if (item.startsWith(user + ":ONLINE")) return true;
        }
        return false;
    }

    private void openDirectChatWithUser(String user) {
        if (user.equals(username)) return;
        for (int i = 0; i < listModel.getSize(); i++) {
            String item = listModel.getElementAt(i);
            String cleanName = getCleanTargetName(item);
            if (cleanName.equals(user)) {
                userList.setSelectedIndex(i);
                return;
            }
        }
        String item = user + (isUserOnline(user) ? ":ONLINE" : ":OFFLINE");
        listModel.addElement(item);
        userList.setSelectedValue(item, true);
    }

    // ==========================================
    // RIGHT CHAT MAIN PANEL
    // ==========================================
    private JPanel createRightChatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBackground(COLOR_CARD_BG);

        // HEADER
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(COLOR_CARD_BG);
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));
        headerPanel.setPreferredSize(new Dimension(0, 58));

        JPanel targetInfo = new JPanel();
        targetInfo.setLayout(new BoxLayout(targetInfo, BoxLayout.Y_AXIS));
        targetInfo.setOpaque(false);
        targetInfo.setBorder(new EmptyBorder(10, 16, 10, 0));

        lblTargetTitle = new JLabel("General Chat (# Cộng đồng)");
        lblTargetTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTargetTitle.setForeground(COLOR_TEXT_DARK);
        lblTargetTitle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblTargetTitle.setToolTipText("Bấm để xem thông tin người dùng / nhóm");

        lblTargetTitle.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                String selected = userList.getSelectedValue();
                if ("Cộng đồng".equals(currentChatTarget) || (selected != null && selected.startsWith("[GROUP]"))) {
                    GroupInfoDialog.showDialog(ChatForm.this, username, currentChatTarget, rawItemList, socketHandler, () -> {
                        updateTargetTitleHeader(userList.getSelectedValue());
                        userList.repaint();
                    });
                } else {
                    boolean isOnline = isUserOnline(currentChatTarget);
                    UserProfileDialog.showDialog(ChatForm.this, username, currentChatTarget, isOnline, new UserProfileDialog.UserProfileCallback() {
                        @Override public void onOpenDirectChat(String targetUser) { openDirectChatWithUser(targetUser); }
                        @Override public void onNicknameChanged() { updateTargetTitleHeader(userList.getSelectedValue()); userList.repaint(); }
                    });
                }
            }
        });

        lblTargetSub = new JLabel("<html><font color='#2563EB'>●</font> Public TCP Network Stream</html>");
        lblTargetSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTargetSub.setForeground(COLOR_TEXT_MUTED);

        targetInfo.add(lblTargetTitle);
        targetInfo.add(lblTargetSub);

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 10));
        headerActions.setOpaque(false);

        btnAddGroupMember = createHeaderIconButton("➕", "Mời thành viên vào nhóm");
        btnAddGroupMember.setVisible(false);
        btnAddGroupMember.addActionListener(e -> showAddGroupMemberDialog());

        btnLeaveGroup = createHeaderIconButton("🚪", "Thoát khỏi nhóm chat");
        btnLeaveGroup.setVisible(false);
        btnLeaveGroup.addActionListener(e -> leaveGroup());

        JButton btnOptions = createHeaderIconButton("•••", "Thông tin & Tùy chọn nhóm");
        btnOptions.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnOptions.addActionListener(e -> {
            String selected = userList.getSelectedValue();
            if ("Cộng đồng".equals(currentChatTarget) || (selected != null && selected.startsWith("[GROUP]"))) {
                GroupInfoDialog.showDialog(ChatForm.this, username, currentChatTarget, rawItemList, socketHandler, () -> {
                    updateTargetTitleHeader(userList.getSelectedValue());
                    userList.repaint();
                });
            } else {
                NicknameDialog.showDialog(this, username, currentChatTarget, (u, n) -> {
                    updateTargetTitleHeader(userList.getSelectedValue());
                    userList.repaint();
                });
            }
        });

        JButton btnBg = createHeaderIconButton("🖼️", "Đổi hình nền chat");
        btnBg.addActionListener(e -> {
            ChatBackgroundDialog.showDialog(this, username, currentChatTarget, isGroup(currentChatTarget), socketHandler, () -> {
                BackgroundPanel bp = bgPanelMap.get(currentChatTarget);
                if (bp != null) bp.refreshBackground();
            });
        });

        JButton btnVoiceCall = createHeaderIconButton("📞", "Gọi thoại (Voice Call)");
        JButton btnVideoCall = createHeaderIconButton("📹", "Gọi Video (Video Call)");

        btnVoiceCall.addActionListener(e -> startVoiceCall());
        btnVideoCall.addActionListener(e -> startVideoCall());

        headerActions.add(btnOptions);
        headerActions.add(btnBg);
        headerActions.add(btnVoiceCall);
        headerActions.add(btnVideoCall);

        headerPanel.add(targetInfo, BorderLayout.WEST);
        headerPanel.add(headerActions, BorderLayout.EAST);

        // CENTER CHAT CARD PANEL
        if (cardLayout == null) cardLayout = new CardLayout();
        if (chatCardPanel == null) chatCardPanel = new JPanel(cardLayout);
        chatCardPanel.setBackground(COLOR_BG_LIGHT);

        // FOOTER INPUT PANEL
        JPanel footerPanel = new JPanel(new BorderLayout(8, 0));
        footerPanel.setBackground(COLOR_CARD_BG);
        footerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, COLOR_BORDER),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JPanel leftAttachmentGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        leftAttachmentGroup.setOpaque(false);

        JButton btnEmoji = createFooterIconButton("😊", "Biểu tượng cảm xúc (Emoji)");
        JButton btnSticker = createFooterIconButton("🎨", "Nhãn dán (Sticker Picker & Create)");
        JButton btnPaperclip = createFooterIconButton("📎", "Đính kèm tệp / ảnh");
        JButton btnMicRecord = createFooterIconButton("🎙️", "Ghi âm giọng nói thực tế");

        btnPaperclip.addActionListener(e -> openDirectAttachmentChooser());
        btnMicRecord.addActionListener(e -> showRealVoiceRecorderDialog());
        btnEmoji.addActionListener(e -> showGridEmojiPickerPopup(btnEmoji));
        btnSticker.addActionListener(e -> showStickerPickerPopup(btnSticker));

        leftAttachmentGroup.add(btnEmoji);
        leftAttachmentGroup.add(btnSticker);
        leftAttachmentGroup.add(btnPaperclip);
        leftAttachmentGroup.add(btnMicRecord);

        txtMessage = new JTextField();
        txtMessage.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        txtMessage.setBackground(COLOR_INPUT_BG);
        txtMessage.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(8, 14, 8, 14)
        ));
        txtMessage.addActionListener(e -> sendMessage());

        JPanel rightSendGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightSendGroup.setOpaque(false);

        JButton btnSchedule = createFooterIconButton("⏱️", "Hẹn giờ gửi tin nhắn (Scheduled Message)");
        btnSchedule.addActionListener(e -> showScheduledMessageDialog());

        JButton btnSend = createStyledButton("Gửi", COLOR_PRIMARY, COLOR_PRIMARY_HOVER, Color.WHITE);
        btnSend.setPreferredSize(new Dimension(68, 38));
        btnSend.addActionListener(e -> sendMessage());

        rightSendGroup.add(btnSchedule);
        rightSendGroup.add(btnSend);

        footerPanel.add(leftAttachmentGroup, BorderLayout.WEST);
        footerPanel.add(txtMessage, BorderLayout.CENTER);
        footerPanel.add(rightSendGroup, BorderLayout.EAST);

        chatPanel.add(headerPanel, BorderLayout.NORTH);
        chatPanel.add(chatCardPanel, BorderLayout.CENTER);
        chatPanel.add(footerPanel, BorderLayout.SOUTH);

        return chatPanel;
    }

    private JButton createHeaderIconButton(String iconText, String tooltip) {
        JButton btn = new JButton(iconText);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
        btn.setForeground(COLOR_TEXT_DARK);
        btn.setBackground(COLOR_SIDEBAR_BG);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setToolTipText(tooltip);
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(36, 36));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(COLOR_BORDER); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { btn.setBackground(COLOR_SIDEBAR_BG); }
        });
        return btn;
    }

    private JButton createFooterIconButton(String iconText, String tooltip) {
        JButton btn = new JButton(iconText);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 17));
        btn.setForeground(COLOR_TEXT_MUTED);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setToolTipText(tooltip);
        btn.setMargin(new Insets(0, 0, 0, 0));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(36, 36));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { btn.setForeground(COLOR_PRIMARY); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { btn.setForeground(COLOR_TEXT_MUTED); }
        });
        return btn;
    }

    private JButton createStyledButton(String text, Color bgNormal, Color bgHover, Color fgText) {
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

    // ==========================================
    // STICKER & SCHEDULED DIALOG CALLS
    // ==========================================
    private void showStickerPickerPopup(Component invoker) {
        StickerPickerPopup.showPopup(invoker, username, (stickerName, stickerData) -> {
            sendSticker(stickerName, stickerData);
        });
    }

    private void showScheduledMessageDialog() {
        ScheduledMessageDialog.showDialog(this, currentChatTarget, txtMessage.getText().trim(), socketHandler, username, (target, content, timestamp, msgId) -> {
            txtMessage.setText("");
            String displayTime = formatMessageTime(timestamp);
            String bubble = "<div id='msg-" + msgId + "' title='" + username + "' style='text-align:right; margin:6px;'>" +
                    "<span style='background-color:#0084ff; color:white; padding:8px 14px; border-radius:18px; display:inline-block; text-align:left; max-width:70%; word-break:break-word;'>" +
                    content + "</span><br>" +
                    "<span style='font-size:10px; color:#999; margin-right:4px;'>" + displayTime + " ⏱️</span></div>";
            appendMessageToChat(target, bubble);
            updateConversationPreview(target, "Bạn: " + content, displayTime, false);
        });
    }

    private void sendSticker(String stickerName, byte[] stickerData) {
        String target = currentChatTarget;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentDbTime = sdf.format(new Date());
        String displayTime = formatMessageTime(currentDbTime);
        String msgId = UUID.randomUUID().toString();

        try {
            MessagePacket packet = new MessagePacket("STICKER", username, target, "[Sticker]", msgId);
            packet.setFileName(stickerName);
            packet.setFileData(stickerData);
            packet.setTimestamp(currentDbTime);

            socketHandler.sendPacket(packet);

            File imgFile = getOrSaveTempFile(stickerName, stickerData);
            String fileUrl = (imgFile != null && imgFile.exists()) ? imgFile.toURI().toURL().toString() : "";

            String bubble = "<div id='msg-" + msgId + "' title='" + username + "' style='text-align:right; margin:6px;'>" +
                    "<img src='" + fileUrl + "' width='120' style='border:0;'/><br>" +
                    "<span style='font-size:10px; color:#999; margin-right:4px;'>" + displayTime + "</span></div>";

            appendMessageToChat(target, bubble);
            updateConversationPreview(target, "Bạn: [Nhãn dán]", displayTime, false);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi gửi nhãn dán!");
        }
    }

    // ==========================================
    // DIRECT ATTACHMENT CHOOSER
    // ==========================================
    private void openDirectAttachmentChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn Ảnh hoặc File để gửi");

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            if (selectedFile != null && selectedFile.exists()) {
                String fileName = selectedFile.getName().toLowerCase();
                if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".gif")) {
                    sendImageFile(selectedFile);
                } else {
                    sendDocumentFile(selectedFile);
                }
            }
        }
    }

    private void sendImageFile(File file) {
        try {
            byte[] fileData = FileTransferClient.readFileToBytes(file);
            String target = currentChatTarget;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentDbTime = sdf.format(new Date());
            String displayTime = formatMessageTime(currentDbTime);
            String msgId = UUID.randomUUID().toString();

            MessagePacket packet = new MessagePacket("IMAGE", username, target, file.getName(), msgId);
            packet.setFileName(file.getName());
            packet.setFileData(fileData);
            packet.setTimestamp(currentDbTime);

            socketHandler.sendPacket(packet);

            String fileUrl = file.toURI().toURL().toString();
            String bubble = "<div id='msg-" + msgId + "' title='" + username + "' style='text-align:right; margin:6px;'>" +
                    "<span style='background-color:#0084ff; padding:6px; border-radius:14px; display:inline-block;'>" +
                    "<a href='" + fileUrl + "'><img src='" + fileUrl + "' width='180' style='border-radius:12px; display:block; border:0;'/></a></span><br>" +
                    "<span style='font-size:10px; color:#999; margin-right:4px;'>" + displayTime + "</span></div>";

            appendMessageToChat(target, bubble);
            updateConversationPreview(target, "Bạn: [Hình ảnh]", displayTime, false);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi gửi ảnh!");
        }
    }

    private void sendDocumentFile(File file) {
        try {
            byte[] fileData = FileTransferClient.readFileToBytes(file);
            String target = currentChatTarget;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentDbTime = sdf.format(new Date());
            String displayTime = formatMessageTime(currentDbTime);
            String msgId = UUID.randomUUID().toString();

            MessagePacket packet = new MessagePacket("FILE", username, target, file.getName(), msgId);
            packet.setFileName(file.getName());
            packet.setFileData(fileData);
            packet.setTimestamp(currentDbTime);

            socketHandler.sendPacket(packet);

            String fileUrl = file.toURI().toURL().toString();
            String bubble = "<div id='msg-" + msgId + "' title='" + username + "' style='text-align:right; margin:6px;'>" +
                    "<span style='background-color:#0084ff; color:white; padding:10px 14px; border-radius:18px; display:inline-block;'>" +
                    "📁 <b>Tài liệu:</b> <a href='" + fileUrl + "' style='color:white; text-decoration:underline;'>" + file.getName() + "</a></span><br>" +
                    "<span style='font-size:10px; color:#999; margin-right:4px;'>" + displayTime + "</span></div>";

            appendMessageToChat(target, bubble);
            String fileLabel = (file.getName().toLowerCase().endsWith(".wav")) ? "[Tin nhắn thoại]" : "[Tệp đính kèm]";
            updateConversationPreview(target, "Bạn: " + fileLabel, displayTime, false);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi gửi tệp!");
        }
    }

    private void showRealVoiceRecorderDialog() {
        VoiceRecorderDialog.showDialog(this, username, currentChatTarget, (wavBytes, durationStr, fileName) -> {
            String target = currentChatTarget;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentDbTime = sdf.format(new Date());
            String displayTime = formatMessageTime(currentDbTime);
            String msgId = UUID.randomUUID().toString();

            try {
                MessagePacket packet = new MessagePacket("FILE", username, target, fileName, msgId);
                packet.setFileName(fileName);
                packet.setFileData(wavBytes);
                packet.setTimestamp(currentDbTime);

                socketHandler.sendPacket(packet);

                File tempWavFile = getOrSaveTempFile(fileName, wavBytes);
                String fileUrl = (tempWavFile != null && tempWavFile.exists()) ? tempWavFile.toURI().toURL().toString() : "";

                String bubble = "<div id='msg-" + msgId + "' title='" + username + "' style='text-align:right; margin:6px;'>" +
                        "<span style='background-color:#0084ff; color:white; padding:10px 14px; border-radius:18px; display:inline-block;'>" +
                        "🎙️ <b>Ghi âm thoại (" + durationStr + "):</b> " +
                        "<a href='" + fileUrl + "' style='color:#FFFF00; font-weight:bold; text-decoration:underline;'>▶ PHÁT ÂM THANH</a>" +
                        "</span><br><span style='font-size:10px; color:#999; margin-right:4px;'>" + displayTime + "</span></div>";

                appendMessageToChat(target, bubble);
                updateConversationPreview(target, "Bạn: [Tin nhắn thoại]", displayTime, false);
            } catch (Exception ex) {
                appendMessageToChat(target, "<div style='color:red;'>Lỗi gửi bản ghi âm!</div>");
            }
        });
    }

    private void showGridEmojiPickerPopup(Component invoker) {
        EmojiPickerPopup.showPopup(invoker, emoji -> {
            int pos = txtMessage.getCaretPosition();
            String currentText = txtMessage.getText();
            if (pos < 0 || pos > currentText.length()) pos = currentText.length();
            String newText = currentText.substring(0, pos) + emoji + currentText.substring(pos);
            txtMessage.setText(newText);
            txtMessage.setCaretPosition(pos + emoji.length());
            txtMessage.requestFocus();
        });
    }

    public void performLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc chắn muốn đăng xuất khỏi tài khoản?",
                "Xác nhận đăng xuất",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            running = false;
            callDialogHandler.stopCallTimers();
            if (socketHandler != null) {
                try {
                    socketHandler.sendPacket(new MessagePacket("LOGOUT", username, "", ""));
                } catch (Exception ignored) {}
                socketHandler.close();
            }
            dispose();
            SwingUtilities.invokeLater(() -> {
                LoginForm loginForm = new LoginForm();
                loginForm.setVisible(true);
            });
        }
    }

    private void startVoiceCall() {
        if ("Cộng đồng".equals(currentChatTarget)) {
            JOptionPane.showMessageDialog(this, "Tính năng gọi thoại hiện áp dụng cho chat riêng tư 1-1!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        sendCallRequest(currentChatTarget, false);
    }

    private void startVideoCall() {
        if ("Cộng đồng".equals(currentChatTarget)) {
            JOptionPane.showMessageDialog(this, "Tính năng gọi Video hiện áp dụng cho chat riêng tư 1-1!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        sendCallRequest(currentChatTarget, true);
    }

    private void sendCallRequest(String targetUser, boolean isVideo) {
        try {
            int port = callDialogHandler.prepareOutgoingCall(username, targetUser, isVideo);
            String myIp = "127.0.0.1";
            try {
                if (socketHandler != null && socketHandler.getSocket() != null && socketHandler.getSocket().getLocalAddress() != null) {
                    myIp = socketHandler.getSocket().getLocalAddress().getHostAddress();
                    if ("0:0:0:0:0:0:0:1".equals(myIp)) {
                        myIp = "127.0.0.1";
                    }
                }
            } catch (Exception ignored) {}

            MessagePacket packet = new MessagePacket("CALL_REQUEST", username, targetUser, 
                (isVideo ? "VIDEO:" : "VOICE:") + port + ":" + myIp);
            socketHandler.sendPacket(packet);
            callDialogHandler.showCallerWindow(this, username, targetUser, isVideo, socketHandler);
        } catch (Exception ex) {
            callDialogHandler.cleanupCall();
            JOptionPane.showMessageDialog(this, "Không thể gửi yêu cầu cuộc gọi!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendMessage() {
        String msg = txtMessage.getText().trim();
        if (msg.isEmpty()) return;

        String target = currentChatTarget;
        String type = "Cộng đồng".equals(target) ? "CHAT_ALL" : "CHAT_PRIVATE";

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentDbTime = sdf.format(new Date());
        String displayTime = formatMessageTime(currentDbTime);
        String msgId = UUID.randomUUID().toString();

        MessagePacket packet = new MessagePacket(type, username, target, msg, msgId);
        packet.setTimestamp(currentDbTime);

        try {
            socketHandler.sendPacket(packet);
            String bubble = "<div id='msg-" + msgId + "' title='" + username + "' style='text-align:right; margin:6px;'>" +
                    "<span style='background-color:#0084ff; color:white; padding:8px 14px; border-radius:18px; display:inline-block; text-align:left; max-width:70%; word-break:break-word;'>" +
                    msg + "</span><br>" +
                    "<span style='font-size:10px; color:#999; margin-right:4px;'>" + displayTime + "</span></div>";
            appendMessageToChat(target, bubble);
            updateConversationPreview(target, "Bạn: " + msg, displayTime, false);
            txtMessage.setText("");
        } catch (Exception ex) {
            appendMessageToChat(target, "<div style='text-align:center; color:red;'>Lỗi gửi tin nhắn!</div>");
        }
    }

    private String getCleanTargetName(String item) {
        if (item == null) return "Cộng đồng";
        if (item.equals("Cộng đồng")) return "Cộng đồng";
        if (item.startsWith("[GROUP]")) {
            String groupContent = item.substring(7);
            return groupContent.split(":")[0];
        }
        if (item.contains(":ONLINE") || item.contains(":OFFLINE")) {
            return item.split(":")[0];
        }
        return item;
    }

    private void filterList() {
        String query = txtSearch.getText().trim().toLowerCase();
        String selected = userList.getSelectedValue();
        listModel.clear();
        for (String item : rawItemList) {
            String cleanName = getCleanTargetName(item).toLowerCase();
            String dispName = NicknameDialog.getDisplayName(username, getCleanTargetName(item)).toLowerCase();
            if (query.isEmpty() || cleanName.contains(query) || dispName.contains(query) || item.toLowerCase().contains(query)) {
                listModel.addElement(item);
            }
        }
        if (selected != null) {
            userList.setSelectedValue(selected, true);
        }
    }

    private void showCreateGroupDialog() {
        CreateGroupDialog.showDialog(this, username, rawItemList, socketHandler, this::getCleanTargetName);
    }

    private void showAddGroupMemberDialog() {
        AddGroupMemberDialog.showDialog(this, username, currentChatTarget, rawItemList, socketHandler, this::getCleanTargetName);
    }

    private void leaveGroup() {
        String selected = userList.getSelectedValue();
        if (selected == null || !selected.startsWith("[GROUP]")) {
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc chắn muốn rời khỏi nhóm '" + currentChatTarget + "'?",
                "Xác nhận thoát nhóm",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.YES_OPTION) {
            MessagePacket packet = new MessagePacket("LEAVE_GROUP", username, "", currentChatTarget);
            try {
                socketHandler.sendPacket(packet);
                JOptionPane.showMessageDialog(this, "Bạn đã rời khỏi nhóm " + currentChatTarget, "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                userList.setSelectedIndex(0);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không thể gửi yêu cầu rời nhóm!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String formatMessageTime(String dbTimestamp) {
        if (dbTimestamp == null || dbTimestamp.isEmpty()) {
            SimpleDateFormat tf = new SimpleDateFormat("HH:mm");
            return tf.format(new Date());
        }
        try {
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date msgDate = dbFormat.parse(dbTimestamp);
            Date currentDate = new Date();

            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
            SimpleDateFormat dayMonthFormat = new SimpleDateFormat("dd/MM");
            SimpleDateFormat fullDateFormat = new SimpleDateFormat("dd/MM/yyyy");
            SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");

            String msgDayStr = fullDateFormat.format(msgDate);
            String currentDayStr = fullDateFormat.format(currentDate);

            if (msgDayStr.equals(currentDayStr)) {
                return timeFormat.format(msgDate);
            }

            long diffTime = currentDate.getTime() - msgDate.getTime();
            long diffDays = diffTime / (24 * 60 * 60 * 1000);

            if (diffDays == 1 || (diffDays == 0 && !msgDayStr.equals(currentDayStr))) {
                return "Hôm qua " + timeFormat.format(msgDate);
            }

            String msgYear = yearFormat.format(msgDate);
            String currentYear = yearFormat.format(currentDate);
            if (!msgYear.equals(currentYear)) {
                return fullDateFormat.format(msgDate) + " " + timeFormat.format(msgDate);
            }

            return dayMonthFormat.format(msgDate);

        } catch (Exception e) {
            return dbTimestamp;
        }
    }

    private void updateConversationPreview(String target, String snippet, String time, boolean markUnread) {
        if (target == null || target.isEmpty()) return;
        String cleanSnippet = formatPreviewSnippet(snippet);
        snippetMap.put(target, cleanSnippet);
        if (time != null && !time.isEmpty()) {
            timeMap.put(target, time);
        }
        if (markUnread && !target.equals(currentChatTarget)) {
            unreadMap.put(target, unreadMap.getOrDefault(target, 0) + 1);
        } else if (target.equals(currentChatTarget)) {
            unreadMap.put(target, 0);
        }
        userList.repaint();
    }

    private String formatPreviewSnippet(String text) {
        if (text == null || text.trim().isEmpty()) return "";
        String clean = text.replaceAll("<[^>]*>", "").replaceAll("\\s+", " ").trim();
        if (clean.length() > 30) {
            clean = clean.substring(0, 28) + "...";
        }
        return clean;
    }

    // Custom Background Panel
    private static class BackgroundPanel extends JPanel {
        private String username;
        private String targetName;
        private boolean isGroup;
        private BufferedImage cachedImg;
        private String cachedImgPath;

        public BackgroundPanel(String username, String targetName, boolean isGroup, LayoutManager layout) {
            super(layout);
            this.username = username;
            this.targetName = targetName;
            this.isGroup = isGroup;
        }

        public BackgroundPanel(String username, String targetName, LayoutManager layout) {
            this(username, targetName, false, layout);
        }

        public void setGroup(boolean isGroup) {
            this.isGroup = isGroup;
        }

        public void refreshBackground() {
            this.cachedImg = null;
            this.cachedImgPath = null;
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            String hexColor = ChatBackgroundDialog.getBgColorHex(username, targetName, isGroup);
            String imgPath = ChatBackgroundDialog.getBgImagePath(username, targetName, isGroup);

            // 1. Nếu có hình nền tùy chỉnh
            if (imgPath != null && !imgPath.isEmpty()) {
                if (!imgPath.equals(cachedImgPath)) {
                    cachedImgPath = imgPath;
                    cachedImg = null;
                    File imgFile = new File(imgPath);
                    if (imgFile.exists()) {
                        try {
                            cachedImg = ImageIO.read(imgFile);
                        } catch (Exception ignored) {}
                    }
                }

                if (cachedImg != null) {
                    // Lấy khung nhìn thực tế của người dùng (Viewport rectangle)
                    // visibleRect luôn cố định kích thước khung chat nhìn thấy, không bị giãn nở theo số lượng tin nhắn
                    Rectangle viewRect = getVisibleRect();
                    int viewW = viewRect.width > 0 ? viewRect.width : getWidth();
                    int viewH = viewRect.height > 0 ? viewRect.height : getHeight();
                    int imgW = cachedImg.getWidth();
                    int imgH = cachedImg.getHeight();

                    // Chế độ Cover: Giữ nguyên tỷ lệ ảnh gốc (Aspect Ratio), phóng to vừa đủ bao phủ toàn bộ khung nhìn
                    double scale = Math.max((double) viewW / imgW, (double) viewH / imgH);
                    int drawW = (int) Math.round(imgW * scale);
                    int drawH = (int) Math.round(imgH * scale);

                    // Căn giữa ảnh trong khung nhìn hiện tại
                    int drawX = viewRect.x + (viewW - drawW) / 2;
                    int drawY = viewRect.y + (viewH - drawH) / 2;

                    // Khử răng cưa và render chất lượng cao để ảnh luôn mịn đẹp, không méo hay vỡ hạt
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    Shape oldClip = g2.getClip();
                    g2.clipRect(viewRect.x, viewRect.y, viewW, viewH);
                    g2.drawImage(cachedImg, drawX, drawY, drawW, drawH, null);
                    g2.setClip(oldClip);

                    g2.dispose();
                    return;
                }
            } else {
                cachedImg = null;
                cachedImgPath = null;
            }

            // 2. Nếu có màu nền tùy chỉnh
            if (hexColor != null && !hexColor.isEmpty()) {
                try {
                    Color c = Color.decode(hexColor);
                    g2.setColor(c);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                    return;
                } catch (Exception ignored) {}
            }

            // 3. Nền mặc định
            g2.setColor(COLOR_BG_LIGHT);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    private void createChatTab(String targetName) {
        JTextPane txtPane = new JTextPane();
        txtPane.setEditable(false);
        txtPane.setContentType("text/html");
        txtPane.setOpaque(false);
        txtPane.setText("<html><body style=\"font-family:'Segoe UI', Arial, sans-serif; font-size:13px; background-color:transparent;\"></body></html>");

        txtPane.addHyperlinkListener(e -> {
            if (javax.swing.event.HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                try {
                    String urlStr = e.getURL() != null ? e.getURL().toString() : (e.getDescription() != null ? e.getDescription() : "");
                    if (urlStr.startsWith("user://")) {
                        String targetUser = urlStr.substring(7);
                        boolean isOnline = isUserOnline(targetUser);
                        UserProfileDialog.showDialog(ChatForm.this, username, targetUser, isOnline, new UserProfileDialog.UserProfileCallback() {
                            @Override public void onOpenDirectChat(String user) { openDirectChatWithUser(user); }
                            @Override public void onNicknameChanged() { updateTargetTitleHeader(userList.getSelectedValue()); userList.repaint(); }
                        });
                    } else if (urlStr.endsWith(".wav") || urlStr.contains("Voice_")) {
                        File wavFile = new File(e.getURL().toURI());
                        if (wavFile.exists()) {
                            Clip oldClip = activeClipsMap.get(urlStr);
                            if (oldClip != null && oldClip.isRunning()) {
                                oldClip.stop();
                                oldClip.close();
                            }
                            AudioInputStream ais = AudioSystem.getAudioInputStream(wavFile);
                            Clip clip = AudioSystem.getClip();
                            clip.open(ais);
                            clip.start();
                            activeClipsMap.put(urlStr, clip);
                        } else {
                            JOptionPane.showMessageDialog(this, "Không tìm thấy tệp âm thanh WAV!");
                        }
                    } else {
                        File file = new File(e.getURL().toURI());
                        if (file.exists()) {
                            String[] options = {"Xem trực tiếp (Mở lớn)", "Tải về máy"};
                            int choice = JOptionPane.showOptionDialog(
                                    this,
                                    "Thao tác với tệp: " + file.getName(),
                                    "Quản lý tệp & ảnh",
                                    JOptionPane.DEFAULT_OPTION,
                                    JOptionPane.INFORMATION_MESSAGE,
                                    null, options, options[0]
                            );

                            if (choice == 0) {
                                Desktop.getDesktop().open(file);
                            } else if (choice == 1) {
                                JFileChooser saveChooser = new JFileChooser();
                                saveChooser.setSelectedFile(new File(file.getName()));
                                int saveOpt = saveChooser.showSaveDialog(this);
                                if (saveOpt == JFileChooser.APPROVE_OPTION) {
                                    File dest = saveChooser.getSelectedFile();
                                    Files.copy(file.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                                    JOptionPane.showMessageDialog(this, "Đã tải về thành công!");
                                }
                            }
                        } else {
                            JOptionPane.showMessageDialog(this, "Không tìm thấy tệp thực tế trên ổ đĩa!");
                        }
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Không thể thao tác với tệp này!");
                }
            }
        });

        // Context menu listener for Message Recall, Delete For Me, User Info & Nickname
        txtPane.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showChatContextMenu(txtPane, e.getPoint(), targetName);
                }
            }
        });

        BackgroundPanel bgContainer = new BackgroundPanel(username, targetName, isGroup(targetName), new BorderLayout());
        bgContainer.add(txtPane, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(bgContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

        chatCardPanel.add(scrollPane, targetName);

        chatPanesMap.put(targetName, txtPane);
        docMap.put(targetName, (HTMLDocument) txtPane.getDocument());
        kitMap.put(targetName, (HTMLEditorKit) txtPane.getEditorKit());
        bgPanelMap.put(targetName, bgContainer);
    }

    private void showChatContextMenu(JTextPane txtPane, Point pt, String targetName) {
        int pos = txtPane.viewToModel2D(pt);
        if (pos < 0) return;

        HTMLDocument doc = (HTMLDocument) txtPane.getDocument();
        Element elem = doc.getCharacterElement(pos);

        Element msgElem = null;
        Element curr = elem;
        while (curr != null) {
            Object idAttr = curr.getAttributes().getAttribute(HTML.Attribute.ID);
            if (idAttr != null && idAttr.toString().startsWith("msg-")) {
                msgElem = curr;
                break;
            }
            curr = curr.getParentElement();
        }

        JPopupMenu popup = new JPopupMenu();

        if (msgElem != null) {
            String elementId = msgElem.getAttributes().getAttribute(HTML.Attribute.ID).toString();
            String msgId = elementId.substring(4);

            Object senderAttr = msgElem.getAttributes().getAttribute(HTML.Attribute.TITLE);
            String msgSender = senderAttr != null ? senderAttr.toString() : "";

            final Element targetElem = msgElem;
            if (username.equals(msgSender)) {
                JMenuItem itemRecall = new JMenuItem("↩️ Thu hồi tin nhắn (Recall for Everyone)");
                itemRecall.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                itemRecall.addActionListener(ev -> recallMessage(msgId, targetName));
                popup.add(itemRecall);

                JMenuItem itemEdit = new JMenuItem("✏️ Chỉnh sửa tin nhắn (Edit Message)");
                itemEdit.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                itemEdit.addActionListener(ev -> editMessage(msgId, targetName, targetElem));
                popup.add(itemEdit);
            } else if (!msgSender.isEmpty()) {
                boolean inGroup = isGroupTarget(currentChatTarget);
                String dispName = inGroup ? NicknameDialog.getGroupDisplayName(currentChatTarget, msgSender) : NicknameDialog.getPrivateDisplayName(username, msgSender);
                JMenuItem itemProfile = new JMenuItem("👤 Xem thông tin (" + dispName + ")");
                itemProfile.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                itemProfile.addActionListener(ev -> {
                    boolean isOnline = isUserOnline(msgSender);
                    UserProfileDialog.showDialog(ChatForm.this, username, msgSender, isOnline, new UserProfileDialog.UserProfileCallback() {
                        @Override public void onOpenDirectChat(String targetUser) { openDirectChatWithUser(targetUser); }
                        @Override public void onNicknameChanged() { updateTargetTitleHeader(userList.getSelectedValue()); userList.repaint(); }
                    });
                });
                popup.add(itemProfile);

                JMenuItem itemNick = new JMenuItem("✏️ Đặt biệt danh cho " + dispName);
                itemNick.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                itemNick.addActionListener(ev -> {
                    if (inGroup) {
                        NicknameDialog.showGroupNicknameDialog(ChatForm.this, currentChatTarget, msgSender, (u, n) -> {
                            updateTargetTitleHeader(userList.getSelectedValue());
                            userList.repaint();
                        });
                    } else {
                        NicknameDialog.showDialog(ChatForm.this, username, msgSender, (u, n) -> {
                            updateTargetTitleHeader(userList.getSelectedValue());
                            userList.repaint();
                        });
                    }
                });
                popup.add(itemNick);

                JMenuItem itemDirect = new JMenuItem("💬 Nhắn tin riêng");
                itemDirect.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                itemDirect.addActionListener(ev -> openDirectChatWithUser(msgSender));
                popup.add(itemDirect);
            }

            JMenuItem itemForward = new JMenuItem("➡️ Chuyển tiếp tin nhắn (Forward)");
            itemForward.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            itemForward.addActionListener(ev -> forwardMessage(targetElem));
            popup.add(itemForward);

            JMenuItem itemDeleteLocal = new JMenuItem("🗑️ Hủy / Xóa phía tôi (Delete for Me)");
            itemDeleteLocal.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            itemDeleteLocal.addActionListener(ev -> deleteMessageForMe(msgId, targetName));
            popup.add(itemDeleteLocal);
        } else {
            JMenuItem itemBg = new JMenuItem("🖼️ Thay đổi nền Chat");
            itemBg.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            itemBg.addActionListener(ev -> ChatBackgroundDialog.showDialog(this, username, targetName, isGroup(targetName), socketHandler, () -> {
                BackgroundPanel bp = bgPanelMap.get(targetName);
                if (bp != null) bp.refreshBackground();
            }));
            popup.add(itemBg);

            if (!"Cộng đồng".equals(targetName) && !targetName.startsWith("[GROUP]")) {
                JMenuItem itemNick = new JMenuItem("✏️ Đặt biệt danh cho " + NicknameDialog.getDisplayName(username, targetName));
                itemNick.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                itemNick.addActionListener(ev -> NicknameDialog.showDialog(this, username, targetName, (u, n) -> {
                    updateTargetTitleHeader(userList.getSelectedValue());
                    userList.repaint();
                }));
                popup.add(itemNick);
            }
        }

        popup.show(txtPane, pt.x, pt.y);
    }

    private void recallMessage(String msgId, String targetName) {
        try {
            MessagePacket packet = new MessagePacket("RECALL_MSG", username, targetName, msgId);
            socketHandler.sendPacket(packet);

            String recalledHtml = "<div id='msg-" + msgId + "' style='text-align:right; margin:6px;'>" +
                    "<span style='background-color:#f3f4f6; color:#9ca3af; padding:6px 12px; border-radius:12px; display:inline-block; font-style:italic; font-size:12px;'>🚫 Tin nhắn đã được thu hồi</span></div>";
            replaceHtmlElement(targetName, msgId, recalledHtml);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi gửi yêu cầu thu hồi tin nhắn!");
        }
    }

    private void deleteMessageForMe(String msgId, String targetName) {
        String blankHtml = "<div id='msg-" + msgId + "' style='display:none;'></div>";
        replaceHtmlElement(targetName, msgId, blankHtml);
    }

    private void editMessage(String msgId, String targetName, Element msgElem) {
        String currentText = "";
        try {
            int start = msgElem.getStartOffset();
            int end = msgElem.getEndOffset();
            currentText = msgElem.getDocument().getText(start, end - start).trim();
            if (currentText.startsWith("[") && currentText.contains("] ")) {
                currentText = currentText.substring(currentText.indexOf("] ") + 2);
            }
        } catch (Exception ignored) {}

        String newContent = JOptionPane.showInputDialog(this, "Chỉnh sửa nội dung tin nhắn:", currentText);
        if (newContent != null && !newContent.trim().isEmpty() && !newContent.trim().equals(currentText)) {
            newContent = newContent.trim();
            try {
                MessagePacket editPacket = new MessagePacket("EDIT_MSG", username, targetName, newContent, msgId);
                socketHandler.sendPacket(editPacket);
            } catch (Exception e) {
                e.printStackTrace();
            }

            String updatedHtml = "<div id='msg-" + msgId + "' title='" + username + "' style='text-align:right; margin:6px;'>" +
                    "<span style='background-color:#0084ff; color:white; padding:8px 14px; border-radius:18px; display:inline-block; text-align:left; max-width:70%; word-break:break-word;'>" +
                    newContent + "</span><br>" +
                    "<span style='font-size:10px; color:#999; margin-right:4px;'>Đã chỉnh sửa • " + formatMessageTime(null) + "</span></div>";
            replaceHtmlElement(targetName, msgId, updatedHtml);
        }
    }

    private void forwardMessage(Element msgElem) {
        String contentToForward = "";
        try {
            int start = msgElem.getStartOffset();
            int end = msgElem.getEndOffset();
            contentToForward = msgElem.getDocument().getText(start, end - start).trim();
            if (contentToForward.startsWith("[") && contentToForward.contains("] ")) {
                contentToForward = contentToForward.substring(contentToForward.indexOf("] ") + 2);
            }
        } catch (Exception ignored) {}

        if (contentToForward.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy nội dung để chuyển tiếp!");
            return;
        }

        List<String> destinations = new ArrayList<>();
        destinations.add("Cộng đồng");
        for (String item : rawItemList) {
            String clean = getCleanTargetName(item);
            if (!clean.equals("Cộng đồng") && !clean.equals(username) && !clean.equals(currentChatTarget)) {
                destinations.add(clean);
            }
        }

        if (destinations.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có cuộc trò chuyện nào khác để chuyển tiếp!");
            return;
        }

        String chosen = (String) JOptionPane.showInputDialog(
                this,
                "Chọn người nhận hoặc nhóm nhận tin nhắn:",
                "Chuyển tiếp tin nhắn",
                JOptionPane.PLAIN_MESSAGE,
                null,
                destinations.toArray(new String[0]),
                destinations.get(0)
        );

        if (chosen != null && !chosen.isEmpty()) {
            String forwardedText = "[Chuyển tiếp]: " + contentToForward;
            boolean isGroup = chosen.equals("Cộng đồng") || isGroupTarget(chosen);
            try {
                MessagePacket fwdPacket = new MessagePacket(isGroup ? "CHAT_ALL" : "CHAT_PRIVATE", username, chosen, forwardedText);
                socketHandler.sendPacket(fwdPacket);

                String fwdHtml = "<div id='msg-" + fwdPacket.getMsgId() + "' title='" + username + "' style='text-align:right; margin:6px;'>" +
                        "<span style='background-color:#0084ff; color:white; padding:8px 14px; border-radius:18px; display:inline-block; text-align:left; max-width:70%; word-break:break-word;'>" +
                        "<i>↪ Chuyển tiếp:</i><br>" + contentToForward + "</span><br>" +
                        "<span style='font-size:10px; color:#999; margin-right:4px;'>" + formatMessageTime(null) + "</span></div>";
                appendMessageToChat(chosen, fwdHtml);

                JOptionPane.showMessageDialog(this, "Đã chuyển tiếp tin nhắn tới: " + chosen);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private boolean isGroup(String name) {
        return isGroupTarget(name);
    }

    private boolean isGroupTarget(String name) {
        if (name == null || name.trim().isEmpty() || "Cộng đồng".equalsIgnoreCase(name.trim())) return false;
        String trimmed = name.trim();
        if (trimmed.startsWith("[GROUP]") || trimmed.startsWith("[group]") || trimmed.endsWith("[Group]")) return true;
        String cleanParam = getCleanTargetName(trimmed);
        for (String item : rawItemList) {
            if (item != null && item.startsWith("[GROUP]") && getCleanTargetName(item).equalsIgnoreCase(cleanParam)) {
                return true;
            }
        }
        return false;
    }

    private void replaceHtmlElement(String targetUser, String msgId, String newHtml) {
        SwingUtilities.invokeLater(() -> {
            try {
                HTMLDocument doc = docMap.get(targetUser);
                if (doc != null) {
                    Element elem = doc.getElement("msg-" + msgId);
                    if (elem != null) {
                        doc.setOuterHTML(elem, newHtml);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void appendMessageToChat(String targetUser, String htmlContent) {
        SwingUtilities.invokeLater(() -> {
            try {
                if (!chatPanesMap.containsKey(targetUser)) {
                    createChatTab(targetUser);
                }
                HTMLDocument doc = docMap.get(targetUser);
                HTMLEditorKit kit = kitMap.get(targetUser);
                JTextPane pane = chatPanesMap.get(targetUser);

                kit.insertHTML(doc, doc.getLength(), htmlContent, 0, 0, null);
                pane.setCaretPosition(doc.getLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private File getOrSaveTempFile(String fileName, byte[] data) {
        try {
            if (!TEMP_DIR.exists()) {
                TEMP_DIR.mkdirs();
            }
            File tempFile = new File(TEMP_DIR, fileName);
            if (data != null && data.length > 0) {
                FileTransferClient.saveBytesToFile(data, tempFile.getAbsolutePath());
            }
            tempFile.deleteOnExit();
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ==========================================
    // CLIENT SOCKET RECEIVE LOOP (RUNNABLE)
    // ==========================================
    @Override
    public void run() {
        while (running) {
            try {
                MessagePacket packet = socketHandler.readPacket();
                if (packet == null) break;

                handlePacket(packet);

            } catch (Exception e) {
                if (running) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Mất kết nối tới Server!", "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
                    });
                }
                break;
            }
        }
    }

    private void handlePacket(MessagePacket packet) {
        String dbTimestamp = packet.getTimestamp();
        String displayTime = formatMessageTime(dbTimestamp);
        String msgId = packet.getMsgId();

        SwingUtilities.invokeLater(() -> {
            if ("UPDATE_USER".equals(packet.getType()) || "USER_LIST".equals(packet.getType())) {
                String content = packet.getContent();
                if (content != null && !content.isEmpty()) {
                    String[] items = content.split(",");
                    rawItemList.clear();
                    rawItemList.add("Cộng đồng");

                    for (String item : items) {
                        if (item.startsWith("[GROUP]")) {
                            rawItemList.add(item);
                        } else {
                            String uName = item.split(":")[0];
                            if (!uName.equals(username)) {
                                rawItemList.add(item);
                            }
                        }
                    }

                    String currentSelected = userList.getSelectedValue();
                    filterList();

                    if (currentSelected != null) {
                        userList.setSelectedValue(currentSelected, true);
                    }
                    if (userList.getSelectedIndex() == -1 && listModel.getSize() > 0) {
                        userList.setSelectedIndex(0);
                    }
                }

            } else if ("RECALL_MSG".equals(packet.getType())) {
                String recalledId = packet.getContent();
                boolean isMe = packet.getSender().equals(username);
                String targetTab = isMe ? packet.getReceiver() : (packet.getReceiver().equals(username) ? packet.getSender() : packet.getReceiver());
                if (targetTab == null || targetTab.isEmpty()) targetTab = "Cộng đồng";

                String recalledHtml = "<div id='msg-" + recalledId + "' style='text-align:" + (isMe ? "right" : "left") + "; margin:6px;'>" +
                        "<span style='background-color:#f3f4f6; color:#9ca3af; padding:6px 12px; border-radius:12px; display:inline-block; font-style:italic; font-size:12px;'>🚫 Tin nhắn đã được thu hồi</span></div>";
                replaceHtmlElement(targetTab, recalledId, recalledHtml);
                updateConversationPreview(targetTab, "🚫 Tin nhắn đã được thu hồi", displayTime, false);

            } else if ("EDIT_MSG".equals(packet.getType())) {
                boolean isMe = packet.getSender().equals(username);
                String targetTab = isMe ? packet.getReceiver() : (packet.getReceiver().equals(username) ? packet.getSender() : packet.getReceiver());
                if (targetTab == null || targetTab.isEmpty()) targetTab = "Cộng đồng";

                String editedHtml;
                if (isMe) {
                    editedHtml = "<div id='msg-" + msgId + "' title='" + username + "' style='text-align:right; margin:6px;'>" +
                            "<span style='background-color:#0084ff; color:white; padding:8px 14px; border-radius:18px; display:inline-block; text-align:left; max-width:70%; word-break:break-word;'>" +
                            packet.getContent() + "</span><br>" +
                            "<span style='font-size:10px; color:#999; margin-right:4px;'>Đã chỉnh sửa • " + displayTime + "</span></div>";
                } else {
                    String senderDisp = NicknameDialog.getDisplayName(username, packet.getSender());
                    editedHtml = "<div id='msg-" + msgId + "' title='" + packet.getSender() + "' style='text-align:left; margin:6px;'>" +
                            "<a href='user://" + packet.getSender() + "' style='font-size:11px; color:#65676B; font-weight:bold; text-decoration:none;'>" + senderDisp + "</a><br>" +
                            "<span style='background-color:#E4E6EB; color:black; padding:8px 14px; border-radius:18px; display:inline-block; text-align:left; max-width:70%; word-break:break-word;'>" +
                            packet.getContent() + "</span><br>" +
                            "<span style='font-size:10px; color:#999; margin-left:4px;'>" + displayTime + " • Đã chỉnh sửa</span></div>";
                }
                replaceHtmlElement(targetTab, msgId, editedHtml);

            } else if ("RECALLED".equals(packet.getType())) {
                boolean isMe = packet.getSender().equals(username);
                String targetTab = isMe ? packet.getReceiver() : (packet.getReceiver().equals(username) ? packet.getSender() : packet.getReceiver());
                if (targetTab == null || targetTab.isEmpty()) targetTab = "Cộng đồng";

                String recalledHtml = "<div id='msg-" + msgId + "' style='text-align:" + (isMe ? "right" : "left") + "; margin:6px;'>" +
                        "<span style='background-color:#f3f4f6; color:#9ca3af; padding:6px 12px; border-radius:12px; display:inline-block; font-style:italic; font-size:12px;'>🚫 Tin nhắn đã được thu hồi</span></div>";
                appendMessageToChat(targetTab, recalledHtml);

            } else if ("CALL_REQUEST".equals(packet.getType())) {
                callDialogHandler.handleIncomingCallRequest(this, username, packet.getSender(), packet.getContent(), socketHandler);

            } else if ("CALL_ACCEPT".equals(packet.getType())) {
                callDialogHandler.handleCallAccept(packet.getContent());

            } else if ("CALL_REJECT".equals(packet.getType())) {
                callDialogHandler.handleCallReject(packet.getSender());

            } else if ("CALL_END".equals(packet.getType())) {
                callDialogHandler.handleCallEnd();

            } else if ("CHANGE_PASSWORD_SUCCESS".equals(packet.getType())) {
                JOptionPane.showMessageDialog(this, packet.getContent(), "Thành công", JOptionPane.INFORMATION_MESSAGE);

            } else if ("CHANGE_PASSWORD_FAIL".equals(packet.getType())) {
                JOptionPane.showMessageDialog(this, packet.getContent(), "Lỗi đổi mật khẩu", JOptionPane.ERROR_MESSAGE);

            } else if ("SET_BG".equals(packet.getType()) || "BG_SYNC".equals(packet.getType())) {
                String sender = packet.getSender();
                String receiver = packet.getReceiver();
                String contentPacket = packet.getContent();
                String fileName = packet.getFileName();
                byte[] fileData = packet.getFileData();

                String convId = "";
                String color = "";

                if (contentPacket != null && contentPacket.contains("|")) {
                    String[] parts = contentPacket.split("\\|", -1);
                    convId = parts[0];
                    color = parts.length > 1 ? parts[1] : "";
                } else {
                    String targetRef = "SET_BG".equals(packet.getType()) ? (receiver.equals(username) ? sender : receiver) : receiver;
                    convId = ChatBackgroundDialog.getCanonicalConvId(username, targetRef, isGroup(targetRef));
                    color = (contentPacket != null) ? contentPacket : "";
                }

                String localImagePath = "";
                if (fileData != null && fileData.length > 0 && fileName != null && !fileName.isEmpty()) {
                    try {
                        File bgDir = new File("user_data/backgrounds");
                        if (!bgDir.exists()) bgDir.mkdirs();
                        File bgFile = new File(bgDir, fileName);
                        java.nio.file.Files.write(bgFile.toPath(), fileData);
                        localImagePath = bgFile.getAbsolutePath();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                if ("RESET".equals(color)) {
                    ChatBackgroundDialog.applySyncedBackground(convId, "", "");
                } else if (!localImagePath.isEmpty()) {
                    ChatBackgroundDialog.applySyncedBackground(convId, "", localImagePath);
                } else if (fileName != null && !fileName.isEmpty()) {
                    File fLocal = new File("user_data/backgrounds", fileName);
                    if (fLocal.exists()) {
                        ChatBackgroundDialog.applySyncedBackground(convId, "", fLocal.getAbsolutePath());
                    } else {
                        File fServer = new File("server_storage", fileName);
                        if (fServer.exists()) {
                            ChatBackgroundDialog.applySyncedBackground(convId, "", fServer.getAbsolutePath());
                        }
                    }
                }

                String targetTab = null;
                if ("Cộng đồng".equals(convId)) {
                    targetTab = "Cộng đồng";
                } else if (convId.contains(":")) {
                    String[] users = convId.split(":");
                    targetTab = users[0].equals(username) ? users[1] : users[0];
                } else {
                    targetTab = convId;
                }

                if (targetTab != null) {
                    final String fTab = targetTab;
                    SwingUtilities.invokeLater(() -> {
                        String cleanTab = fTab.startsWith("[GROUP]") ? fTab.substring(7).split(":")[0].trim() : fTab;
                        BackgroundPanel bp = bgPanelMap.get(cleanTab);
                        if (bp == null) bp = bgPanelMap.get(fTab);
                        if (bp != null) {
                            bp.setGroup(isGroup(cleanTab));
                            bp.refreshBackground();
                        }
                    });
                }

            } else if ("NICKNAME_LIST".equals(packet.getType())) {
                NicknameDialog.loadNicknamesFromData(packet.getContent());
                SwingUtilities.invokeLater(() -> {
                    updateTargetTitleHeader(userList.getSelectedValue());
                    userList.repaint();
                });

            } else if ("GROUP_NICKNAME_UPDATE".equals(packet.getType())) {
                NicknameDialog.setGroupNickname(packet.getSender(), packet.getReceiver(), packet.getContent());
                SwingUtilities.invokeLater(() -> {
                    updateTargetTitleHeader(userList.getSelectedValue());
                    userList.repaint();
                });

            } else if ("CHAT_ALL".equals(packet.getType())) {
                boolean isMe = packet.getSender().equals(username);
                String senderDisp = isMe ? "Bạn" : NicknameDialog.getPrivateDisplayName(username, packet.getSender());
                String snippet = isMe ? ("Bạn: " + packet.getContent()) : (senderDisp + ": " + packet.getContent());
                updateConversationPreview("Cộng đồng", snippet, displayTime, !isMe);

                String bubble;
                if (isMe) {
                    bubble = "<div id='msg-" + msgId + "' title='" + username + "' style='text-align:right; margin:6px;'>" +
                            "<span style='background-color:#0084ff; color:white; padding:8px 14px; border-radius:18px; display:inline-block; text-align:left; max-width:70%; word-break:break-word;'>" +
                            packet.getContent() + "</span><br>" +
                            "<span style='font-size:10px; color:#999; margin-right:4px;'>" + displayTime + "</span></div>";
                } else {
                    bubble = "<div id='msg-" + msgId + "' title='" + packet.getSender() + "' style='text-align:left; margin:6px;'>" +
                            "<a href='user://" + packet.getSender() + "' style='font-size:11px; color:#65676B; font-weight:bold; text-decoration:none;'>" + senderDisp + "</a><br>" +
                            "<span style='background-color:#E4E6EB; color:black; padding:8px 14px; border-radius:18px; display:inline-block; text-align:left; max-width:70%; word-break:break-word;'>" +
                            packet.getContent() + "</span><br>" +
                            "<span style='font-size:10px; color:#999; margin-left:4px;'>" + displayTime + "</span></div>";
                }
                appendMessageToChat("Cộng đồng", bubble);

            } else if ("CHAT_PRIVATE".equals(packet.getType())) {
                boolean isMe = packet.getSender().equals(username);
                String targetTab = isMe ? packet.getReceiver() : (packet.getReceiver().equals(username) ? packet.getSender() : packet.getReceiver());
                boolean inGroup = isGroupTarget(targetTab);
                String senderDisp = inGroup ?
                        NicknameDialog.getGroupDisplayName(targetTab, packet.getSender()) :
                        NicknameDialog.getPrivateDisplayName(username, packet.getSender());

                String snippet = isMe ? ("Bạn: " + packet.getContent()) : (inGroup ? (senderDisp + ": " + packet.getContent()) : packet.getContent());
                updateConversationPreview(targetTab, snippet, displayTime, !isMe);

                String bubble;
                if (isMe) {
                    bubble = "<div id='msg-" + msgId + "' title='" + username + "' style='text-align:right; margin:6px;'>" +
                            "<span style='background-color:#0084ff; color:white; padding:8px 14px; border-radius:18px; display:inline-block; text-align:left; max-width:70%; word-break:break-word;'>" +
                            packet.getContent() + "</span><br>" +
                            "<span style='font-size:10px; color:#999; margin-right:4px;'>" + displayTime + "</span></div>";
                } else {
                    bubble = "<div id='msg-" + msgId + "' title='" + packet.getSender() + "' style='text-align:left; margin:6px;'>" +
                            "<a href='user://" + packet.getSender() + "' style='font-size:11px; color:#2563EB; font-weight:bold; text-decoration:none;'>" + senderDisp + "</a><br>" +
                            "<span style='background-color:#E4E6EB; color:black; padding:8px 14px; border-radius:18px; display:inline-block; text-align:left; max-width:70%; word-break:break-word;'>" +
                            packet.getContent() + "</span><br>" +
                            "<span style='font-size:10px; color:#999; margin-left:4px;'>" + displayTime + "</span></div>";
                }
                appendMessageToChat(targetTab, bubble);

            } else if ("STICKER".equals(packet.getType())) {
                boolean isMe = packet.getSender().equals(username);
                String receiver = packet.getReceiver();
                String targetTab = (receiver == null || receiver.isEmpty() || "Cộng đồng".equals(receiver)) ? "Cộng đồng" : (isMe ? receiver : (receiver.equals(username) ? packet.getSender() : receiver));
                boolean inGroup = isGroupTarget(targetTab);
                String senderDisp = inGroup ?
                        NicknameDialog.getGroupDisplayName(targetTab, packet.getSender()) :
                        NicknameDialog.getPrivateDisplayName(username, packet.getSender());

                String snippet = isMe ? "Bạn: [Nhãn dán]" : (inGroup || "Cộng đồng".equals(targetTab) ? (senderDisp + ": [Nhãn dán]") : "[Nhãn dán]");
                updateConversationPreview(targetTab, snippet, displayTime, !isMe);

                String fileUrl = "";
                File imgFile = getOrSaveTempFile(packet.getFileName(), packet.getFileData());
                if (imgFile != null && imgFile.exists()) {
                    try {
                        fileUrl = imgFile.toURI().toURL().toString();
                    } catch (Exception ignored) {}
                }

                String bubble;
                String stickerTag = "<img src='" + fileUrl + "' width='120' style='border:0;'/>";
                if (isMe) {
                    bubble = "<div id='msg-" + msgId + "' title='" + username + "' style='text-align:right; margin:6px;'>" +
                            stickerTag + "<br>" +
                            "<span style='font-size:10px; color:#999; margin-right:4px;'>" + displayTime + "</span></div>";
                } else {
                    bubble = "<div id='msg-" + msgId + "' title='" + packet.getSender() + "' style='text-align:left; margin:6px;'>" +
                            "<a href='user://" + packet.getSender() + "' style='font-size:11px; color:#65676B; font-weight:bold; text-decoration:none;'>" + senderDisp + " đã gửi nhãn dán:</a><br>" +
                            stickerTag + "<br>" +
                            "<span style='font-size:10px; color:#999; margin-left:4px;'>" + displayTime + "</span></div>";
                }
                appendMessageToChat(targetTab, bubble);

            } else if ("IMAGE".equals(packet.getType())) {
                boolean isMe = packet.getSender().equals(username);
                String receiver = packet.getReceiver();
                String targetTab = (receiver == null || receiver.isEmpty() || "Cộng đồng".equals(receiver)) ? "Cộng đồng" : (isMe ? receiver : (receiver.equals(username) ? packet.getSender() : receiver));
                boolean inGroup = isGroupTarget(targetTab);
                String senderDisp = inGroup ?
                        NicknameDialog.getGroupDisplayName(targetTab, packet.getSender()) :
                        NicknameDialog.getPrivateDisplayName(username, packet.getSender());

                String snippet = isMe ? "Bạn: [Hình ảnh]" : (inGroup || "Cộng đồng".equals(targetTab) ? (senderDisp + ": [Hình ảnh]") : "[Hình ảnh]");
                updateConversationPreview(targetTab, snippet, displayTime, !isMe);

                String fileUrl = "";
                File imgFile = getOrSaveTempFile(packet.getFileName(), packet.getFileData());
                if (imgFile != null && imgFile.exists()) {
                    try {
                        fileUrl = imgFile.toURI().toURL().toString();
                    } catch (Exception ignored) {}
                }

                String bubble;
                String imgTag = "<a href='" + fileUrl + "'><img src='" + fileUrl + "' width='180' style='border-radius:12px; display:block; border:0;'/></a>";
                if (isMe) {
                    bubble = "<div id='msg-" + msgId + "' title='" + username + "' style='text-align:right; margin:6px;'>" +
                            "<span style='background-color:#0084ff; padding:6px; border-radius:14px; display:inline-block;'>" +
                            imgTag + "</span><br>" +
                            "<span style='font-size:10px; color:#999; margin-right:4px;'>" + displayTime + "</span></div>";
                } else {
                    bubble = "<div id='msg-" + msgId + "' title='" + packet.getSender() + "' style='text-align:left; margin:6px;'>" +
                            "<a href='user://" + packet.getSender() + "' style='font-size:11px; color:#65676B; font-weight:bold; text-decoration:none;'>" + senderDisp + " đã gửi ảnh:</a><br>" +
                            "<span style='background-color:#E4E6EB; padding:6px; border-radius:14px; display:inline-block;'>" +
                            imgTag + "</span><br>" +
                            "<span style='font-size:10px; color:#999; margin-left:4px;'>" + displayTime + "</span></div>";
                }
                appendMessageToChat(targetTab, bubble);

            } else if ("FILE".equals(packet.getType())) {
                boolean isMe = packet.getSender().equals(username);
                String receiver = packet.getReceiver();
                String targetTab = (receiver == null || receiver.isEmpty() || "Cộng đồng".equals(receiver)) ? "Cộng đồng" : (isMe ? receiver : (receiver.equals(username) ? packet.getSender() : receiver));
                boolean inGroup = isGroupTarget(targetTab);
                String senderDisp = inGroup ?
                        NicknameDialog.getGroupDisplayName(targetTab, packet.getSender()) :
                        NicknameDialog.getPrivateDisplayName(username, packet.getSender());

                boolean isWavVoice = packet.getFileName() != null && packet.getFileName().toLowerCase().endsWith(".wav");
                String fileLabel = isWavVoice ? "[Tin nhắn thoại]" : "[Tệp đính kèm]";
                String snippet = isMe ? ("Bạn: " + fileLabel) : (inGroup || "Cộng đồng".equals(targetTab) ? (senderDisp + ": " + fileLabel) : fileLabel);
                updateConversationPreview(targetTab, snippet, displayTime, !isMe);

                String fileUrl = "";
                File file = getOrSaveTempFile(packet.getFileName(), packet.getFileData());
                if (file != null && file.exists()) {
                    try {
                        fileUrl = file.toURI().toURL().toString();
                    } catch (Exception ignored) {}
                }

                String bubble;
                if (isMe) {
                    bubble = "<div id='msg-" + msgId + "' title='" + username + "' style='text-align:right; margin:6px;'>" +
                            "<span style='background-color:#0084ff; color:white; padding:10px 14px; border-radius:18px; display:inline-block;'>" +
                            (isWavVoice ? "🎙️ <b>Ghi âm thoại:</b> <a href='" + fileUrl + "' style='color:#FFFF00; font-weight:bold; text-decoration:underline;'>▶ PHÁT ÂM THANH</a>" :
                                          "📁 <b>Tài liệu:</b> <a href='" + (fileUrl.isEmpty() ? "#" : fileUrl) + "' style='color:white; text-decoration:underline;'>" + packet.getFileName() + "</a>") +
                            "</span><br>" +
                            "<span style='font-size:10px; color:#999; margin-right:4px;'>" + displayTime + "</span></div>";
                } else {
                    bubble = "<div id='msg-" + msgId + "' title='" + packet.getSender() + "' style='text-align:left; margin:6px;'>" +
                            "<a href='user://" + packet.getSender() + "' style='font-size:11px; color:#65676B; font-weight:bold; text-decoration:none;'>" + senderDisp + " đã gửi tệp:</a><br>" +
                            "<span style='background-color:#E4E6EB; color:black; padding:10px 14px; border-radius:18px; display:inline-block;'>" +
                            (isWavVoice ? "🎙️ <b>Ghi âm thoại:</b> <a href='" + fileUrl + "' style='color:#0084ff; font-weight:bold; text-decoration:underline;'>▶ PHÁT ÂM THANH</a>" :
                                          "📁 <b>Tài liệu:</b> <a href='" + (fileUrl.isEmpty() ? "#" : fileUrl) + "' style='color:#0084ff; text-decoration:underline;'>" + packet.getFileName() + "</a>") +
                            "</span><br>" +
                            "<span style='font-size:10px; color:#999; margin-left:4px;'>" + displayTime + "</span></div>";
                }
                appendMessageToChat(targetTab, bubble);
            }
        });
    }
}
