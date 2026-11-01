package server.gui;

import server.core.ClientSession;
import server.core.ServerCore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.EventObject;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

public class ActiveSocketTablePanel extends JPanel {
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JLabel lblHandlerCount;
    private final JLabel lblExecutorInfo;

    public ActiveSocketTablePanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        // Top Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleBox.setOpaque(false);

        JLabel lblTitle = new JLabel("\u26A1 Active Socket Pool");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(new Color(15, 23, 42));
        titleBox.add(lblTitle);

        lblHandlerCount = new JLabel("0 Handlers");
        lblHandlerCount.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblHandlerCount.setForeground(new Color(37, 99, 235));
        lblHandlerCount.setOpaque(true);
        lblHandlerCount.setBackground(new Color(239, 246, 255));
        lblHandlerCount.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(219, 234, 254), 1, true),
                new EmptyBorder(2, 8, 2, 8)
        ));
        titleBox.add(lblHandlerCount);

        topBar.add(titleBox, BorderLayout.WEST);

        ModernButton btnRescan = new ModernButton("Re-scan Sockets", new Color(241, 245, 249), new Color(51, 65, 85), new Color(203, 213, 225));
        btnRescan.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRescan.setPreferredSize(new Dimension(130, 28));
        btnRescan.addActionListener(e -> refreshTableData());
        topBar.add(btnRescan, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // Table Setup
        String[] columns = {"THREAD ID", "IP ADDRESS / SOCKET", "CLIENT IDENTITY", "STATE", "ACTIONS"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4; // Only ACTIONS column is interactive
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(38);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(241, 245, 249));
        table.setSelectionForeground(new Color(15, 23, 42));

        // Header Styling
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.getTableHeader().setForeground(new Color(71, 85, 105));
        table.getTableHeader().setPreferredSize(new Dimension(0, 32));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(160);
        table.getColumnModel().getColumn(3).setPreferredWidth(130);
        table.getColumnModel().getColumn(4).setPreferredWidth(110);

        // Custom Renderers
        table.getColumnModel().getColumn(0).setCellRenderer(new ThreadIdRenderer());
        table.getColumnModel().getColumn(2).setCellRenderer(new IdentityRenderer());
        table.getColumnModel().getColumn(3).setCellRenderer(new StateBadgeRenderer());

        DisconnectButtonRenderer disconnectRenderer = new DisconnectButtonRenderer();
        table.getColumnModel().getColumn(4).setCellRenderer(disconnectRenderer);
        table.getColumnModel().getColumn(4).setCellEditor(disconnectRenderer);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setPreferredSize(new Dimension(500, 220));

        add(scrollPane, BorderLayout.CENTER);

        // Footer Info
        lblExecutorInfo = new JLabel("Executor: java.util.concurrent.ThreadPoolExecutor | Available Workers: 100 / 100");
        lblExecutorInfo.setFont(new Font("Consolas", Font.PLAIN, 11));
        lblExecutorInfo.setForeground(new Color(100, 116, 139));
        add(lblExecutorInfo, BorderLayout.SOUTH);

        refreshTableData();
    }

    public void refreshTableData() {
        tableModel.setRowCount(0);
        Set<ClientSession> sessions = ServerCore.getActiveSessions();
        int activeCount = sessions.size();
        lblHandlerCount.setText(activeCount + " Handlers");

        for (ClientSession session : sessions) {
            String threadId = session.getThreadName();
            String ipSocket = session.getRemoteAddress();
            String identity = session.getUsername() != null ? session.getUsername() : "(Connecting)";
            String state = session.getClientState();

            tableModel.addRow(new Object[]{threadId, ipSocket, identity, state, "Disconnect"});
        }

        ThreadPoolExecutor pool = ServerCore.getThreadPool();
        if (pool != null) {
            int activeThreads = pool.getActiveCount();
            int poolSize = pool.getPoolSize();
            lblExecutorInfo.setText(String.format("Executor: java.util.concurrent.ThreadPoolExecutor | Active Workers: %d | Pool Size: %d", activeThreads, poolSize));
        } else {
            lblExecutorInfo.setText("Executor: Idle (Server Stopped)");
        }
    }

    private static class ThreadIdRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setFont(new Font("Consolas", Font.BOLD, 12));
            label.setForeground(new Color(22, 101, 52));
            label.setBorder(new EmptyBorder(0, 10, 0, 0));
            return label;
        }
    }

    private static class IdentityRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 7));
            panel.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);

            String text = value != null ? value.toString() : "";
            String initial = text.length() > 0 && !text.startsWith("(") ? text.substring(0, 1).toUpperCase() : "?";

            JLabel avatar = new JLabel(initial, SwingConstants.CENTER);
            avatar.setPreferredSize(new Dimension(24, 24));
            avatar.setFont(new Font("Segoe UI", Font.BOLD, 12));
            avatar.setOpaque(true);
            avatar.setBackground(new Color(219, 234, 254));
            avatar.setForeground(new Color(29, 78, 216));
            avatar.setBorder(BorderFactory.createLineBorder(new Color(191, 219, 254), 1, true));

            JLabel lblText = new JLabel(text);
            lblText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lblText.setForeground(new Color(15, 23, 42));

            panel.add(avatar);
            panel.add(lblText);
            return panel;
        }
    }

    private static class StateBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 8));
            panel.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);

            String stateStr = value != null ? value.toString() : "Online";
            JLabel badge = new JLabel("\u25CF " + stateStr);
            badge.setFont(new Font("Segoe UI", Font.BOLD, 11));

            if ("Transmitting".equalsIgnoreCase(stateStr)) {
                badge.setForeground(new Color(22, 101, 52));
                badge.setBackground(new Color(220, 252, 231));
            } else if ("Idle".equalsIgnoreCase(stateStr)) {
                badge.setForeground(new Color(71, 85, 105));
                badge.setBackground(new Color(241, 245, 249));
            } else {
                badge.setForeground(new Color(29, 78, 216));
                badge.setBackground(new Color(239, 246, 255));
            }

            badge.setOpaque(true);
            badge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
                    new EmptyBorder(2, 8, 2, 8)
            ));

            panel.add(badge);
            return panel;
        }
    }

    private class DisconnectButtonRenderer extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {
        private final JPanel panel;
        private final JButton btnDisconnect;
        private String currentIdentifier;

        public DisconnectButtonRenderer() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
            panel.setOpaque(true);

            btnDisconnect = new JButton("Disconnect");
            btnDisconnect.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btnDisconnect.setFocusPainted(false);
            btnDisconnect.setBackground(new Color(254, 226, 226));
            btnDisconnect.setForeground(new Color(220, 38, 38));
            btnDisconnect.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(252, 165, 165), 1, true),
                    new EmptyBorder(3, 10, 3, 10)
            ));

            btnDisconnect.addActionListener(e -> {
                if (currentIdentifier != null) {
                    ServerCore.kickClient(currentIdentifier);
                    fireEditingStopped();
                    refreshTableData();
                }
            });

            panel.add(btnDisconnect);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            panel.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
            return panel;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            panel.setBackground(table.getSelectionBackground());
            String identity = table.getValueAt(row, 2).toString();
            String threadId = table.getValueAt(row, 0).toString();
            String ipSocket = table.getValueAt(row, 1).toString();

            if (identity != null && !identity.equals("(Connecting)")) {
                currentIdentifier = identity;
            } else if (ipSocket != null && !ipSocket.isEmpty()) {
                currentIdentifier = ipSocket;
            } else {
                currentIdentifier = threadId;
            }

            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "Disconnect";
        }

        @Override
        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }
    }
}
