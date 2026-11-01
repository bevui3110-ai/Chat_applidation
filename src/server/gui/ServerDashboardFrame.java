package server.gui;

import server.core.ServerCore;
import server.core.ServerLogger;
import server.manager.GroupManager;
import server.manager.RoomManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

public class ServerDashboardFrame extends JFrame {
    private final CardLayout cardLayout;
    private final JPanel mainContentPanel;
    private JLabel lblStatusBadge;
    private JLabel lblSubtitle;
    private JLabel lblUptime;
    private ModernButton btnStart;
    private ModernButton btnStop;
    private ModernButton btnTabDashboard;
    private ModernButton btnTabUsers;
    private ModernButton btnTabLogs;

    // 5 Stat Cards
    private final StatCardPanel cardState;
    private final StatCardPanel cardClients;
    private final StatCardPanel cardThreads;
    private final StatCardPanel cardAccounts;
    private final StatCardPanel cardRooms;

    // Sub panels
    private final ActiveSocketTablePanel tablePanel;
    private final PortConfigPanel portConfigPanel;
    private final RegisteredUsersPanel usersPanel;
    private final ServerLogsPanel fullLogsPanel;
    private final JPanel channelsListBox;

    private final Timer timer;

    public ServerDashboardFrame() {
        setTitle("ChatChit Server Control Center");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 850);
        setMinimumSize(new Dimension(1150, 750));
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(248, 250, 252));

        setLayout(new BorderLayout(0, 0));

        // 1. Top Header Bar (Fixed 2-row layout)
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // 2. Main Content Stack (CardLayout)
        cardLayout = new CardLayout();
        mainContentPanel = new JPanel(cardLayout);
        mainContentPanel.setOpaque(false);

        // Sub Panels
        tablePanel = new ActiveSocketTablePanel();
        portConfigPanel = new PortConfigPanel();
        usersPanel = new RegisteredUsersPanel();
        fullLogsPanel = new ServerLogsPanel();

        // 5 Stat Cards
        cardState = new StatCardPanel("\uD83D\uDCE1", "GATEWAY STATE", "ACTIVE", "0.0.0.0:" + ServerCore.getPort() + "  TCP Bound", new Color(22, 101, 52));
        cardClients = new StatCardPanel("\uD83D\uDC65", "CONNECTED CLIENTS", "0 / 100", "Capacity: 0%", new Color(30, 41, 59));
        cardThreads = new StatCardPanel("\uD83D\uDCBB", "THREAD POOL", "0 Threads", "WorkerThreads allocated \u25CF", new Color(30, 41, 59));
        cardAccounts = new StatCardPanel("\uD83D\uDD11", "REGISTERED USERS", "0 Accounts", "In MySQL Database", new Color(37, 99, 235));
        cardRooms = new StatCardPanel("\uD83D\uDCAC", "ACTIVE ROOMS", "0 Groups", "In-Memory Chat Routers", new Color(30, 41, 59));

        channelsListBox = new JPanel();
        channelsListBox.setLayout(new BoxLayout(channelsListBox, BoxLayout.Y_AXIS));
        channelsListBox.setOpaque(false);

        JPanel dashboardView = createDashboardView();

        mainContentPanel.add(dashboardView, "DASHBOARD");
        mainContentPanel.add(usersPanel, "USERS");
        mainContentPanel.add(fullLogsPanel, "LOGS");

        add(mainContentPanel, BorderLayout.CENTER);

        // UI Tabs & Header Action Handlers
        btnTabDashboard.addActionListener(e -> switchTab("DASHBOARD"));
        btnTabUsers.addActionListener(e -> switchTab("USERS"));
        btnTabLogs.addActionListener(e -> switchTab("LOGS"));

        // Timer for periodic refresh (1 second interval)
        timer = new Timer(1000, e -> updateDashboardUI());
        timer.start();

        // Auto-start server on launch if not running
        if (!ServerCore.isRunning()) {
            ServerCore.startServer(ServerCore.getPort());
        }

        updateDashboardUI();
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(0, 10));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                new EmptyBorder(12, 20, 10, 20)
        ));

        // ROW 1: Title + Subtitle (Left) and Uptime + Start/Stop Buttons (Right)
        JPanel row1 = new JPanel(new BorderLayout(15, 0));
        row1.setOpaque(false);

        // Left Box: Title & Status
        JPanel leftBox = new JPanel(new BorderLayout(0, 3));
        leftBox.setOpaque(false);

        JPanel titleLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleLine.setOpaque(false);

        JLabel lblTitle = new JLabel("ChatChit Server Control Center");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(15, 23, 42));
        titleLine.add(lblTitle);

        lblStatusBadge = new JLabel("\u25CF RUNNING");
        lblStatusBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblStatusBadge.setOpaque(true);
        lblStatusBadge.setBackground(new Color(220, 252, 231));
        lblStatusBadge.setForeground(new Color(22, 101, 52));
        lblStatusBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(187, 247, 208), 1, true),
                new EmptyBorder(3, 8, 3, 8)
        ));
        titleLine.add(lblStatusBadge);

        leftBox.add(titleLine, BorderLayout.NORTH);

        lblSubtitle = new JLabel("Multi-threaded TCP/IP Socket Gateway [Port " + ServerCore.getPort() + "] - HotSpot(TM) 64-Bit Tiered Compilers");
        lblSubtitle.setFont(new Font("Consolas", Font.PLAIN, 12));
        lblSubtitle.setForeground(new Color(100, 116, 139));
        leftBox.add(lblSubtitle, BorderLayout.SOUTH);

        row1.add(leftBox, BorderLayout.WEST);

        // Right Box: Uptime + Controls
        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightBox.setOpaque(false);

        lblUptime = new JLabel("\u23F1 Uptime: 00:00:00");
        lblUptime.setFont(new Font("Consolas", Font.BOLD, 13));
        lblUptime.setForeground(new Color(29, 78, 216));
        lblUptime.setOpaque(true);
        lblUptime.setBackground(new Color(239, 246, 255));
        lblUptime.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(219, 234, 254), 1, true),
                new EmptyBorder(6, 12, 6, 12)
        ));
        rightBox.add(lblUptime);

        // Start Server Button (Solid Emerald Green with White Text)
        btnStart = new ModernButton("\u25B6 Start Server", new Color(22, 101, 52), Color.WHITE);
        btnStart.addActionListener(e -> {
            int port = portConfigPanel.getEnteredPort();
            ServerCore.startServer(port);
            updateDashboardUI();
        });
        rightBox.add(btnStart);

        // Stop Server Button (Solid Crimson Red with Crisp White Text)
        btnStop = new ModernButton("\u25A0 Stop Server", new Color(220, 38, 38), Color.WHITE);
        btnStop.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(this,
                    "B\u1EA1n c\u00F3 ch\u1EAFc ch\u1EAFn mu\u1ED1n ng\u1EAFt to\u00E0n b\u1ED9 k\u1EBFt n\u1ED1i v\u00E0 D\u1EEAng Server?",
                    "X\u00E1c nh\u1EADn D\u1EEAng Server", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                ServerCore.stopServer();
                updateDashboardUI();
            }
        });
        rightBox.add(btnStop);

        row1.add(rightBox, BorderLayout.EAST);
        header.add(row1, BorderLayout.NORTH);

        // ROW 2: Navigation Tabs Bar
        JPanel navBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        navBar.setOpaque(false);

        btnTabDashboard = createTabButton("Server Dashboard", true);
        btnTabUsers = createTabButton("Users & Accounts", false);
        btnTabLogs = createTabButton("Server Logs", false);

        navBar.add(btnTabDashboard);
        navBar.add(btnTabUsers);
        navBar.add(btnTabLogs);

        header.add(navBar, BorderLayout.SOUTH);

        return header;
    }

    private ModernButton createTabButton(String text, boolean active) {
        ModernButton btn;
        if (active) {
            btn = new ModernButton(text, new Color(37, 99, 235), Color.WHITE);
        } else {
            btn = new ModernButton(text, new Color(241, 245, 249), new Color(71, 85, 105));
        }
        btn.setPreferredSize(new Dimension(170, 36));
        return btn;
    }

    private void switchTab(String cardName) {
        cardLayout.show(mainContentPanel, cardName);
        boolean isDash = "DASHBOARD".equals(cardName);
        boolean isUsers = "USERS".equals(cardName);
        boolean isLogs = "LOGS".equals(cardName);

        updateTabStyle(btnTabDashboard, isDash);
        updateTabStyle(btnTabUsers, isUsers);
        updateTabStyle(btnTabLogs, isLogs);

        if (isUsers) {
            usersPanel.refreshUserTable();
        }
    }

    private void updateTabStyle(ModernButton btn, boolean active) {
        if (active) {
            btn.setCustomColors(new Color(37, 99, 235), Color.WHITE);
        } else {
            btn.setCustomColors(new Color(241, 245, 249), new Color(71, 85, 105));
        }
    }

    private JPanel createDashboardView() {
        JPanel container = new JPanel(new BorderLayout(0, 14));
        container.setOpaque(false);
        container.setBorder(new EmptyBorder(14, 20, 14, 20));

        // Top 5 Stat Cards Row
        JPanel cardsRow = new JPanel(new GridLayout(1, 5, 12, 0));
        cardsRow.setOpaque(false);
        cardsRow.add(cardState);
        cardsRow.add(cardClients);
        cardsRow.add(cardThreads);
        cardsRow.add(cardAccounts);
        cardsRow.add(cardRooms);

        container.add(cardsRow, BorderLayout.NORTH);

        // Main 2 Column Grid
        JPanel mainGrid = new JPanel(new GridBagLayout());
        mainGrid.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // Left Column (Active Socket Table + Mini Console Stream)
        JPanel leftCol = new JPanel(new BorderLayout(0, 14));
        leftCol.setOpaque(false);
        leftCol.add(tablePanel, BorderLayout.CENTER);

        JPanel miniConsole = createMiniConsolePanel();
        leftCol.add(miniConsole, BorderLayout.SOUTH);

        gbc.gridx = 0;
        gbc.weightx = 0.65;
        mainGrid.add(leftCol, gbc);

        // Right Column (Port Config + Chat Channels)
        JPanel rightCol = new JPanel(new BorderLayout(0, 14));
        rightCol.setOpaque(false);

        rightCol.add(portConfigPanel, BorderLayout.NORTH);

        JPanel channelsCard = createChannelsCardPanel();
        rightCol.add(channelsCard, BorderLayout.CENTER);

        gbc.gridx = 1;
        gbc.weightx = 0.35;
        gbc.insets = new Insets(0, 14, 0, 0);
        mainGrid.add(rightCol, gbc);

        container.add(mainGrid, BorderLayout.CENTER);

        return container;
    }

    private JPanel createMiniConsolePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel lblTitle = new JLabel("\uD83D\uDCDF Console Stream");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(new Color(15, 23, 42));
        header.add(lblTitle, BorderLayout.WEST);

        JLabel lblSub = new JLabel("tail -f server.log");
        lblSub.setFont(new Font("Consolas", Font.PLAIN, 11));
        lblSub.setForeground(new Color(100, 116, 139));
        header.add(lblSub, BorderLayout.EAST);

        panel.add(header, BorderLayout.NORTH);

        JTextArea txtLogPreview = new JTextArea();
        txtLogPreview.setEditable(false);
        txtLogPreview.setFont(new Font("Consolas", Font.PLAIN, 11));
        txtLogPreview.setBackground(new Color(15, 23, 42));
        txtLogPreview.setForeground(new Color(226, 232, 240));
        txtLogPreview.setRows(5);

        ServerLogger.addListener(new ServerLogger.LogListener() {
            @Override
            public void onLogAdded(ServerLogger.LogEntry entry) {
                SwingUtilities.invokeLater(() -> {
                    txtLogPreview.append(entry.toFormattedString() + "\n");
                    txtLogPreview.setCaretPosition(txtLogPreview.getDocument().getLength());
                });
            }

            @Override
            public void onLogsCleared() {
                SwingUtilities.invokeLater(() -> txtLogPreview.setText(""));
            }
        });

        JScrollPane scroll = new JScrollPane(txtLogPreview);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(30, 41, 59), 1));
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createChannelsCardPanel() {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel lblTitle = new JLabel("\uD83D\uDCAC Chat Room Channels");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(new Color(15, 23, 42));
        header.add(lblTitle, BorderLayout.WEST);

        JLabel lblRouter = new JLabel("In-Memory Router");
        lblRouter.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblRouter.setForeground(new Color(100, 116, 139));
        header.add(lblRouter, BorderLayout.EAST);

        card.add(header, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(channelsListBox);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setPreferredSize(new Dimension(300, 140));

        card.add(scroll, BorderLayout.CENTER);

        ModernButton btnManage = new ModernButton("Manage Group Permissions ->", new Color(248, 250, 252), new Color(37, 99, 235), new Color(226, 232, 240));
        btnManage.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnManage.setPreferredSize(new Dimension(280, 32));
        btnManage.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Danh s\u00E1ch c\u00E1c nh\u00F3m chat hi\u1EC7n \u0111\u01B0\u1EE3c \u0111\u1ED3ng b\u1ED9 h\u00F3a t\u1EF1 \u0111\u1ED9ng v\u1EDBi Database MySQL.",
                "Group Channels", JOptionPane.INFORMATION_MESSAGE));

        card.add(btnManage, BorderLayout.SOUTH);

        return card;
    }

    private void updateDashboardUI() {
        boolean isRunning = ServerCore.isRunning();
        int port = ServerCore.getPort();

        if (isRunning) {
            lblStatusBadge.setText("\u25CF RUNNING");
            lblStatusBadge.setBackground(new Color(220, 252, 231));
            lblStatusBadge.setForeground(new Color(22, 101, 52));

            long secs = ServerCore.getUptimeSeconds();
            long hours = secs / 3600;
            long mins = (secs % 3600) / 60;
            long s = secs % 60;
            lblUptime.setText(String.format("\u23F1 Uptime: %02d:%02d:%02d", hours, mins, s));

            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
            btnStop.setCustomColors(new Color(220, 38, 38), Color.WHITE);
        } else {
            lblStatusBadge.setText("\u25CF STOPPED");
            lblStatusBadge.setBackground(new Color(254, 226, 226));
            lblStatusBadge.setForeground(new Color(220, 38, 38));

            lblUptime.setText("\u23F1 Uptime: 00:00:00");

            btnStart.setEnabled(true);
            btnStart.setCustomColors(new Color(22, 101, 52), Color.WHITE);
            btnStop.setEnabled(false);
        }

        lblSubtitle.setText("Multi-threaded TCP/IP Socket Gateway [Port " + port + "] - HotSpot(TM) 64-Bit Tiered Compilers");
        portConfigPanel.updateState();

        if (isRunning) {
            cardState.updateValue("ACTIVE", new Color(22, 101, 52));
            cardState.updateSubtext("0.0.0.0:" + port + "  TCP Bound");
        } else {
            cardState.updateValue("OFFLINE", new Color(220, 38, 38));
            cardState.updateSubtext("Server Unbound");
        }

        int onlineCount = RoomManager.getOnlineClients().size();
        cardClients.updateValue(onlineCount + " / 100", new Color(15, 23, 42));
        cardClients.updateSubtext("Capacity: " + (onlineCount * 100 / 100) + "%");

        ThreadPoolExecutor pool = ServerCore.getThreadPool();
        int threads = pool != null ? pool.getActiveCount() : 0;
        cardThreads.updateValue(threads + " Threads", new Color(15, 23, 42));
        cardThreads.updateSubtext("WorkerThreads allocated \u25CF");

        int registeredUsers = ServerCore.getRegisteredUsersCount();
        cardAccounts.updateValue(registeredUsers + " Users", new Color(37, 99, 235));
        cardAccounts.updateSubtext("In MySQL Database");

        Map<String, Set<String>> groupMap = GroupManager.getGroupMap();
        int groupsCount = groupMap.size();
        cardRooms.updateValue(groupsCount + " Groups", new Color(15, 23, 42));
        cardRooms.updateSubtext("General, In-Memory Chat");

        tablePanel.refreshTableData();

        channelsListBox.removeAll();
        if (groupMap.isEmpty()) {
            JLabel lblEmpty = new JLabel("Ch\u01B0a c\u00F3 nh\u00F3m chat n\u00E0o");
            lblEmpty.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            lblEmpty.setForeground(new Color(148, 163, 184));
            channelsListBox.add(lblEmpty);
        } else {
            for (Map.Entry<String, Set<String>> entry : groupMap.entrySet()) {
                JPanel item = new JPanel(new BorderLayout());
                item.setBackground(new Color(248, 250, 252));
                item.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                        new EmptyBorder(6, 10, 6, 10)
                ));

                JLabel lblName = new JLabel("# " + entry.getKey());
                lblName.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lblName.setForeground(new Color(15, 23, 42));
                item.add(lblName, BorderLayout.WEST);

                JLabel lblUsers = new JLabel(entry.getValue().size() + " Users");
                lblUsers.setFont(new Font("Segoe UI", Font.BOLD, 10));
                lblUsers.setOpaque(true);
                lblUsers.setBackground(new Color(239, 246, 255));
                lblUsers.setForeground(new Color(37, 99, 235));
                lblUsers.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(219, 234, 254), 1, true),
                        new EmptyBorder(2, 6, 2, 6)
                ));
                item.add(lblUsers, BorderLayout.EAST);

                channelsListBox.add(item);
                channelsListBox.add(Box.createVerticalStrut(6));
            }
        }
        channelsListBox.revalidate();
        channelsListBox.repaint();
    }
}
