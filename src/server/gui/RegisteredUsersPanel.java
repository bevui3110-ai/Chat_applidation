package server.gui;

import server.core.ServerCore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

public class RegisteredUsersPanel extends JPanel {
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JLabel lblTotalCount;
    private final JLabel lblOnlineCount;
    private final JLabel lblOfflineCount;
    private final JTextField txtSearch;

    public RegisteredUsersPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(new Color(248, 250, 252));
        setBorder(new EmptyBorder(14, 20, 14, 20));

        // Top Metrics Row
        JPanel topGrid = new JPanel(new GridLayout(1, 3, 14, 0));
        topGrid.setOpaque(false);

        lblTotalCount = createMetricCard("TOTAL REGISTERED ACCOUNTS", "0 Accounts", new Color(37, 99, 235));
        lblOnlineCount = createMetricCard("CURRENTLY ONLINE", "0 Online", new Color(22, 101, 52));
        lblOfflineCount = createMetricCard("OFFLINE ACCOUNTS", "0 Offline", new Color(100, 116, 139));

        topGrid.add(lblTotalCount.getParent());
        topGrid.add(lblOnlineCount.getParent());
        topGrid.add(lblOfflineCount.getParent());

        add(topGrid, BorderLayout.NORTH);

        // Center Panel (Search + Table)
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setBackground(Color.WHITE);
        centerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        // Search Bar
        JPanel searchBar = new JPanel(new BorderLayout(10, 0));
        searchBar.setOpaque(false);

        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchBox.setOpaque(false);

        JLabel lblSearchIcon = new JLabel("Search Username:");
        lblSearchIcon.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSearchIcon.setForeground(new Color(71, 85, 105));
        searchBox.add(lblSearchIcon);

        txtSearch = new JTextField();
        txtSearch.setPreferredSize(new Dimension(240, 32));
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshUserTable(); }
            public void removeUpdate(DocumentEvent e) { refreshUserTable(); }
            public void changedUpdate(DocumentEvent e) { refreshUserTable(); }
        });

        searchBox.add(txtSearch);
        searchBar.add(searchBox, BorderLayout.WEST);

        ModernButton btnRefresh = new ModernButton("Refresh Database", new Color(241, 245, 249), new Color(51, 65, 85), new Color(203, 213, 225));
        btnRefresh.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefresh.setPreferredSize(new Dimension(140, 28));
        btnRefresh.addActionListener(e -> refreshUserTable());

        searchBar.add(btnRefresh, BorderLayout.EAST);
        centerPanel.add(searchBar, BorderLayout.NORTH);

        // Registered Users Table
        String[] columns = {"#", "USERNAME", "ACCOUNT TYPE", "STATUS", "LAST CONNECTED IP", "ACTIONS"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(38);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(241, 245, 249));
        table.setSelectionForeground(new Color(15, 23, 42));

        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setBackground(new Color(248, 250, 252));
        table.getTableHeader().setForeground(new Color(71, 85, 105));
        table.getTableHeader().setPreferredSize(new Dimension(0, 32));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)));

        table.getColumnModel().getColumn(0).setPreferredWidth(50);  // ID
        table.getColumnModel().getColumn(1).setPreferredWidth(180); // Username
        table.getColumnModel().getColumn(2).setPreferredWidth(140); // Type
        table.getColumnModel().getColumn(3).setPreferredWidth(130); // Status
        table.getColumnModel().getColumn(4).setPreferredWidth(180); // IP
        table.getColumnModel().getColumn(5).setPreferredWidth(120); // Actions

        table.getColumnModel().getColumn(1).setCellRenderer(new IdentityRenderer());
        table.getColumnModel().getColumn(3).setCellRenderer(new UserStatusBadgeRenderer());

        ActionCellRenderer actionRenderer = new ActionCellRenderer();
        table.getColumnModel().getColumn(5).setCellRenderer(actionRenderer);
        table.getColumnModel().getColumn(5).setCellEditor(actionRenderer);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240), 1));
        scrollPane.getViewport().setBackground(Color.WHITE);

        centerPanel.add(scrollPane, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        refreshUserTable();
    }

    private JLabel createMetricCard(String title, String value, Color valueColor) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(12, 16, 12, 16)
        ));

        JLabel lblT = new JLabel(title);
        lblT.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblT.setForeground(new Color(100, 116, 139));

        JLabel lblV = new JLabel(value);
        lblV.setFont(new Font("Consolas", Font.BOLD, 18));
        lblV.setForeground(valueColor);

        card.add(lblT, BorderLayout.NORTH);
        card.add(lblV, BorderLayout.CENTER);

        return lblV;
    }

    public void refreshUserTable() {
        tableModel.setRowCount(0);
        List<ServerCore.RegisteredUserInfo> allUsers = ServerCore.getRegisteredUsersList();
        String filter = txtSearch.getText().trim().toLowerCase();

        int total = allUsers.size();
        int online = 0;
        int offline = 0;

        for (ServerCore.RegisteredUserInfo u : allUsers) {
            if ("ONLINE".equalsIgnoreCase(u.getStatus())) {
                online++;
            } else {
                offline++;
            }

            if (filter.isEmpty() || u.getUsername().toLowerCase().contains(filter)) {
                tableModel.addRow(new Object[]{
                        u.getId(),
                        u.getUsername(),
                        "Standard Member",
                        u.getStatus(),
                        u.getIpAddress(),
                        "ONLINE".equalsIgnoreCase(u.getStatus()) ? "Kick User" : "Registered"
                });
            }
        }

        lblTotalCount.setText(total + " Accounts");
        lblOnlineCount.setText(online + " Online");
        lblOfflineCount.setText(offline + " Offline");
    }

    private static class IdentityRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 7));
            panel.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);

            String text = value != null ? value.toString() : "";
            String initial = text.length() > 0 ? text.substring(0, 1).toUpperCase() : "?";

            JLabel avatar = new JLabel(initial, SwingConstants.CENTER);
            avatar.setPreferredSize(new Dimension(24, 24));
            avatar.setFont(new Font("Segoe UI", Font.BOLD, 12));
            avatar.setOpaque(true);
            avatar.setBackground(new Color(239, 246, 255));
            avatar.setForeground(new Color(37, 99, 235));
            avatar.setBorder(BorderFactory.createLineBorder(new Color(219, 234, 254), 1, true));

            JLabel lblText = new JLabel(text);
            lblText.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblText.setForeground(new Color(15, 23, 42));

            panel.add(avatar);
            panel.add(lblText);
            return panel;
        }
    }

    private static class UserStatusBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 8));
            panel.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);

            String statusStr = value != null ? value.toString() : "OFFLINE";
            JLabel badge = new JLabel("\u25CF " + statusStr);
            badge.setFont(new Font("Segoe UI", Font.BOLD, 11));

            if ("ONLINE".equalsIgnoreCase(statusStr)) {
                badge.setForeground(new Color(22, 101, 52));
                badge.setBackground(new Color(220, 252, 231));
            } else {
                badge.setForeground(new Color(100, 116, 139));
                badge.setBackground(new Color(241, 245, 249));
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

    private class ActionCellRenderer extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {
        private final JPanel panel;
        private final JButton btnAction;
        private String targetUser;
        private String actionText;

        public ActionCellRenderer() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
            panel.setOpaque(true);

            btnAction = new JButton("Kick User");
            btnAction.setFont(new Font("Segoe UI", Font.BOLD, 11));
            btnAction.setFocusPainted(false);

            btnAction.addActionListener(e -> {
                if ("Kick User".equals(actionText) && targetUser != null) {
                    ServerCore.kickClient(targetUser);
                    fireEditingStopped();
                    refreshUserTable();
                }
            });

            panel.add(btnAction);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            panel.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
            actionText = value != null ? value.toString() : "Registered";
            btnAction.setText(actionText);

            if ("Kick User".equals(actionText)) {
                btnAction.setEnabled(true);
                btnAction.setBackground(new Color(254, 226, 226));
                btnAction.setForeground(new Color(220, 38, 38));
                btnAction.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(252, 165, 165), 1, true),
                        new EmptyBorder(3, 10, 3, 10)
                ));
            } else {
                btnAction.setEnabled(false);
                btnAction.setBackground(new Color(241, 245, 249));
                btnAction.setForeground(new Color(148, 163, 184));
                btnAction.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                        new EmptyBorder(3, 10, 3, 10)
                ));
            }
            return panel;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            panel.setBackground(table.getSelectionBackground());
            targetUser = table.getValueAt(row, 1).toString();
            actionText = value != null ? value.toString() : "Registered";
            btnAction.setText(actionText);
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return actionText;
        }

        @Override
        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }
    }
}
