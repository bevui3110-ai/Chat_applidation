package server.gui;

import server.core.ServerCore;
import server.core.ServerLogger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class ServerLogsPanel extends JPanel implements ServerLogger.LogListener {
    private final JTextPane logTextPane;
    private final StyledDocument doc;
    private final JCheckBox chkAutoScroll;
    private final JLabel lblSocketState;
    private final JLabel lblBufferedLines;
    private final JLabel lblWorkerThreads;
    private final JLabel lblThroughput;
    private final JTextField txtCommandInput;
    private String currentLevelFilter = "ALL";

    // Style attributes
    private SimpleAttributeSet styleDefault;
    private SimpleAttributeSet styleTime;
    private SimpleAttributeSet styleInfo;
    private SimpleAttributeSet styleAuth;
    private SimpleAttributeSet styleMsg;
    private SimpleAttributeSet styleBroadcast;
    private SimpleAttributeSet styleDisconnect;
    private SimpleAttributeSet styleFile;
    private SimpleAttributeSet styleError;

    public ServerLogsPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(new Color(248, 250, 252));
        setBorder(new EmptyBorder(14, 16, 14, 16));

        // Top Metrics Bar
        JPanel topBar = new JPanel(new BorderLayout(10, 10));
        topBar.setOpaque(false);

        JPanel metricsGrid = new JPanel(new GridLayout(1, 4, 10, 0));
        metricsGrid.setOpaque(false);

        lblSocketState = createMetricCard("SOCKET STATE", "BOUND :" + ServerCore.getPort(), new Color(22, 101, 52));
        lblBufferedLines = createMetricCard("BUFFERED LINES", "0 entries", new Color(30, 41, 59));
        lblWorkerThreads = createMetricCard("WORKER THREADS", "0 active / 0 idle", new Color(30, 41, 59));
        lblThroughput = createMetricCard("THROUGHPUT", "4.2 KB / 240 pkt", new Color(37, 99, 235));

        metricsGrid.add(lblSocketState.getParent());
        metricsGrid.add(lblBufferedLines.getParent());
        metricsGrid.add(lblWorkerThreads.getParent());
        metricsGrid.add(lblThroughput.getParent());

        topBar.add(metricsGrid, BorderLayout.CENTER);

        // Action Controls (Auto-scroll, Refresh, Clear)
        JPanel ctrlBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        ctrlBox.setOpaque(false);

        chkAutoScroll = new JCheckBox("Auto-scroll to bottom", true);
        chkAutoScroll.setOpaque(false);
        chkAutoScroll.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        chkAutoScroll.setForeground(new Color(51, 65, 85));

        ModernButton btnRefresh = new ModernButton("Refresh", new Color(37, 99, 235), Color.WHITE);
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefresh.setPreferredSize(new Dimension(85, 30));
        btnRefresh.addActionListener(e -> reloadLogs());

        ModernButton btnClear = new ModernButton("Clear Logs", new Color(241, 245, 249), new Color(51, 65, 85), new Color(203, 213, 225));
        btnClear.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClear.setPreferredSize(new Dimension(95, 30));
        btnClear.addActionListener(e -> ServerLogger.clearLogs());

        ctrlBox.add(chkAutoScroll);
        ctrlBox.add(btnRefresh);
        ctrlBox.add(btnClear);

        topBar.add(ctrlBox, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Terminal Container (Dark Theme)
        JPanel terminalPanel = new JPanel(new BorderLayout());
        terminalPanel.setBackground(new Color(15, 23, 42));
        terminalPanel.setBorder(BorderFactory.createLineBorder(new Color(30, 41, 59), 1, true));

        // Terminal Window Bar (Mac style buttons)
        JPanel termHeader = new JPanel(new BorderLayout());
        termHeader.setBackground(new Color(30, 41, 59));
        termHeader.setBorder(new EmptyBorder(6, 12, 6, 12));

        JPanel dotsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        dotsPanel.setOpaque(false);
        dotsPanel.add(createDot(new Color(239, 68, 68)));
        dotsPanel.add(createDot(new Color(245, 158, 11)));
        dotsPanel.add(createDot(new Color(34, 197, 94)));

        JLabel lblTermTitle = new JLabel("java.net.ServerSocket stdout stream [PID: " + ProcessHandle.current().pid() + "]");
        lblTermTitle.setFont(new Font("Consolas", Font.PLAIN, 12));
        lblTermTitle.setForeground(new Color(148, 163, 184));
        dotsPanel.add(Box.createHorizontalStrut(10));
        dotsPanel.add(lblTermTitle);

        termHeader.add(dotsPanel, BorderLayout.WEST);

        JLabel lblLive = new JLabel("UTF-8 / LF  \u25CF LIVE");
        lblLive.setFont(new Font("Consolas", Font.BOLD, 11));
        lblLive.setForeground(new Color(34, 197, 94));
        termHeader.add(lblLive, BorderLayout.EAST);

        terminalPanel.add(termHeader, BorderLayout.NORTH);

        // Log Text Pane
        logTextPane = new JTextPane();
        logTextPane.setEditable(false);
        logTextPane.setBackground(new Color(15, 23, 42));
        logTextPane.setForeground(new Color(226, 232, 240));
        logTextPane.setFont(new Font("Consolas", Font.PLAIN, 12));
        logTextPane.setMargin(new Insets(10, 12, 10, 12));
        doc = logTextPane.getStyledDocument();

        initStyles();

        JScrollPane logScroll = new JScrollPane(logTextPane);
        logScroll.setBorder(null);
        logScroll.getViewport().setBackground(new Color(15, 23, 42));

        terminalPanel.add(logScroll, BorderLayout.CENTER);

        // Command Prompt Input at bottom of terminal
        JPanel cmdPanel = new JPanel(new BorderLayout(8, 0));
        cmdPanel.setBackground(new Color(15, 23, 42));
        cmdPanel.setBorder(new EmptyBorder(6, 12, 8, 12));

        JLabel lblPrompt = new JLabel("$");
        lblPrompt.setFont(new Font("Consolas", Font.BOLD, 14));
        lblPrompt.setForeground(new Color(34, 197, 94));
        cmdPanel.add(lblPrompt, BorderLayout.WEST);

        txtCommandInput = new JTextField();
        txtCommandInput.setBackground(new Color(15, 23, 42));
        txtCommandInput.setForeground(new Color(241, 245, 249));
        txtCommandInput.setCaretColor(Color.WHITE);
        txtCommandInput.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtCommandInput.setBorder(null);
        txtCommandInput.setToolTipText("Type socket management command (e.g., :status, :kick <name>, :broadcast <msg>, :clear)");

        txtCommandInput.addActionListener(this::handleCommandInput);
        cmdPanel.add(txtCommandInput, BorderLayout.CENTER);

        JLabel lblEnter = new JLabel("Press ENTER");
        lblEnter.setFont(new Font("Consolas", Font.PLAIN, 10));
        lblEnter.setForeground(new Color(100, 116, 139));
        cmdPanel.add(lblEnter, BorderLayout.EAST);

        terminalPanel.add(cmdPanel, BorderLayout.SOUTH);
        add(terminalPanel, BorderLayout.CENTER);

        // Bottom Level Filter Buttons Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filterPanel.setOpaque(false);

        JLabel lblFilterTitle = new JLabel("LEVEL FILTER:");
        lblFilterTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblFilterTitle.setForeground(new Color(100, 116, 139));
        filterPanel.add(lblFilterTitle);

        for (String filter : new String[]{"ALL", "INFO", "AUTH", "MESSAGES", "ERRORS"}) {
            boolean isActive = filter.equals(currentLevelFilter);
            ModernButton btnFilter = new ModernButton(
                    filter,
                    isActive ? new Color(37, 99, 235) : new Color(241, 245, 249),
                    isActive ? Color.WHITE : new Color(71, 85, 105),
                    new Color(203, 213, 225)
            );
            btnFilter.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btnFilter.setPreferredSize(new Dimension(80, 26));

            btnFilter.addActionListener(e -> {
                currentLevelFilter = filter;
                reloadLogs();
                for (Component c : filterPanel.getComponents()) {
                    if (c instanceof ModernButton) {
                        ModernButton b = (ModernButton) c;
                        boolean active = b.getText().equals(currentLevelFilter);
                        b.setCustomColors(
                                active ? new Color(37, 99, 235) : new Color(241, 245, 249),
                                active ? Color.WHITE : new Color(71, 85, 105),
                                new Color(203, 213, 225)
                        );
                    }
                }
            });
            filterPanel.add(btnFilter);
        }

        bottomBar.add(filterPanel, BorderLayout.WEST);

        JLabel lblFooterStatus = new JLabel("\u25CF Socket daemon listening on port " + ServerCore.getPort() + " | Logging level: DEBUG");
        lblFooterStatus.setFont(new Font("Consolas", Font.PLAIN, 11));
        lblFooterStatus.setForeground(new Color(100, 116, 139));
        bottomBar.add(lblFooterStatus, BorderLayout.EAST);

        add(bottomBar, BorderLayout.SOUTH);

        ServerLogger.addListener(this);
        reloadLogs();
    }

    private JLabel createMetricCard(String title, String value, Color valueColor) {
        JPanel card = new JPanel(new BorderLayout(0, 2));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel lblT = new JLabel(title);
        lblT.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblT.setForeground(new Color(100, 116, 139));

        JLabel lblV = new JLabel(value);
        lblV.setFont(new Font("Consolas", Font.BOLD, 13));
        lblV.setForeground(valueColor);

        card.add(lblT, BorderLayout.NORTH);
        card.add(lblV, BorderLayout.CENTER);

        return lblV;
    }

    private JLabel createDot(Color color) {
        JLabel dot = new JLabel("\u25CF");
        dot.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        dot.setForeground(color);
        return dot;
    }

    private void initStyles() {
        styleDefault = new SimpleAttributeSet();
        StyleConstants.setForeground(styleDefault, new Color(226, 232, 240));

        styleTime = new SimpleAttributeSet();
        StyleConstants.setForeground(styleTime, new Color(100, 116, 139));

        styleInfo = new SimpleAttributeSet();
        StyleConstants.setForeground(styleInfo, new Color(59, 130, 246));
        StyleConstants.setBold(styleInfo, true);

        styleAuth = new SimpleAttributeSet();
        StyleConstants.setForeground(styleAuth, new Color(139, 92, 246));
        StyleConstants.setBold(styleAuth, true);

        styleMsg = new SimpleAttributeSet();
        StyleConstants.setForeground(styleMsg, new Color(6, 182, 212));
        StyleConstants.setBold(styleMsg, true);

        styleBroadcast = new SimpleAttributeSet();
        StyleConstants.setForeground(styleBroadcast, new Color(245, 158, 11));
        StyleConstants.setBold(styleBroadcast, true);

        styleDisconnect = new SimpleAttributeSet();
        StyleConstants.setForeground(styleDisconnect, new Color(239, 68, 68));
        StyleConstants.setBold(styleDisconnect, true);

        styleFile = new SimpleAttributeSet();
        StyleConstants.setForeground(styleFile, new Color(236, 72, 153));
        StyleConstants.setBold(styleFile, true);

        styleError = new SimpleAttributeSet();
        StyleConstants.setForeground(styleError, new Color(220, 38, 38));
        StyleConstants.setBold(styleError, true);
    }

    public synchronized void reloadLogs() {
        try {
            doc.remove(0, doc.getLength());
            List<ServerLogger.LogEntry> logs = ServerLogger.getLogs();
            for (ServerLogger.LogEntry entry : logs) {
                appendLogEntry(entry);
            }
            updateMetrics(logs.size());
        } catch (BadLocationException ignored) {}
    }

    private void appendLogEntry(ServerLogger.LogEntry entry) {
        String level = entry.getLevel();
        if (!shouldShow(level)) return;

        try {
            doc.insertString(doc.getLength(), "[" + entry.getTime() + "] ", styleTime);

            SimpleAttributeSet levelStyle = getStyleForLevel(level);
            doc.insertString(doc.getLength(), String.format("[%-9s] ", level), levelStyle);

            doc.insertString(doc.getLength(), "[" + entry.getTag() + "] ", styleTime);
            doc.insertString(doc.getLength(), entry.getMessage() + "\n", styleDefault);

            if (chkAutoScroll.isSelected()) {
                logTextPane.setCaretPosition(doc.getLength());
            }
        } catch (BadLocationException ignored) {}
    }

    private boolean shouldShow(String level) {
        if ("ALL".equalsIgnoreCase(currentLevelFilter)) return true;
        if ("INFO".equalsIgnoreCase(currentLevelFilter)) return "INFO".equalsIgnoreCase(level);
        if ("AUTH".equalsIgnoreCase(currentLevelFilter)) return "AUTH".equalsIgnoreCase(level);
        if ("MESSAGES".equalsIgnoreCase(currentLevelFilter)) return "MSG".equalsIgnoreCase(level) || "BROADCAST".equalsIgnoreCase(level);
        if ("ERRORS".equalsIgnoreCase(currentLevelFilter)) return "ERROR".equalsIgnoreCase(level);
        return true;
    }

    private SimpleAttributeSet getStyleForLevel(String level) {
        switch (level.toUpperCase()) {
            case "INFO": return styleInfo;
            case "AUTH": return styleAuth;
            case "MSG": return styleMsg;
            case "BROADCAST": return styleBroadcast;
            case "DISCONNECT": return styleDisconnect;
            case "FILE": return styleFile;
            case "ERROR": return styleError;
            default: return styleDefault;
        }
    }

    private void updateMetrics(int logCount) {
        lblSocketState.setText(ServerCore.isRunning() ? "BOUND :" + ServerCore.getPort() : "STOPPED");
        lblBufferedLines.setText(logCount + " entries");

        int activeThreads = ServerCore.getThreadPool() != null ? ServerCore.getThreadPool().getActiveCount() : 0;
        int activeClients = server.manager.RoomManager.getOnlineClients().size();
        lblWorkerThreads.setText(activeThreads + " active / " + (ServerCore.getActiveSessions().size() - activeThreads) + " idle");
        lblThroughput.setText(activeClients + " online clients");
    }

    private void handleCommandInput(ActionEvent e) {
        String input = txtCommandInput.getText().trim();
        if (input.isEmpty()) return;

        txtCommandInput.setText("");
        ServerLogger.info("Terminal", "$ " + input);

        if (input.equalsIgnoreCase(":clear")) {
            ServerLogger.clearLogs();
        } else if (input.equalsIgnoreCase(":status")) {
            ServerLogger.info("Status", "Running: " + ServerCore.isRunning() + " | Port: " + ServerCore.getPort() +
                    " | Connected Clients: " + server.manager.RoomManager.getOnlineClients().size());
        } else if (input.toLowerCase().startsWith(":kick ")) {
            String name = input.substring(6).trim();
            ServerCore.kickClient(name);
        } else if (input.toLowerCase().startsWith(":broadcast ")) {
            String msg = input.substring(11).trim();
            ServerCore.broadcastSystemMessage(msg);
        } else if (input.equalsIgnoreCase(":help")) {
            ServerLogger.info("Help", "Available commands: :status, :kick <username>, :broadcast <message>, :clear, :help");
        } else {
            ServerLogger.error("Terminal", "Unknown command: '" + input + "'. Type :help for commands.");
        }
    }

    @Override
    public void onLogAdded(ServerLogger.LogEntry entry) {
        SwingUtilities.invokeLater(() -> {
            appendLogEntry(entry);
            updateMetrics(ServerLogger.getLogs().size());
        });
    }

    @Override
    public void onLogsCleared() {
        SwingUtilities.invokeLater(() -> {
            try {
                doc.remove(0, doc.getLength());
                updateMetrics(0);
            } catch (BadLocationException ignored) {}
        });
    }
}
