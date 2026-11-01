package server.gui;

import server.core.ServerCore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class PortConfigPanel extends JPanel {
    private final JTextField txtPort;
    private final JLabel lblStatusBadge;
    private final JButton btnBroadcast;
    private final JButton btnEditProps;
    private final JCheckBox chkTcpNoDelay;
    private final JCheckBox chkHeartbeat;
    private final JCheckBox chkVerbose;

    public PortConfigPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Port Configuration");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(new Color(15, 23, 42));
        headerPanel.add(lblTitle, BorderLayout.WEST);

        lblStatusBadge = new JLabel("Editable (Stopped)");
        lblStatusBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblStatusBadge.setOpaque(true);
        lblStatusBadge.setBackground(new Color(241, 245, 249));
        lblStatusBadge.setForeground(new Color(71, 85, 105));
        lblStatusBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(2, 8, 2, 8)
        ));
        headerPanel.add(lblStatusBadge, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Fields Container
        JPanel fieldsBox = new JPanel();
        fieldsBox.setLayout(new BoxLayout(fieldsBox, BoxLayout.Y_AXIS));
        fieldsBox.setOpaque(false);

        // Port Field
        JPanel portBox = new JPanel(new BorderLayout(0, 4));
        portBox.setOpaque(false);

        JPanel lblRow1 = new JPanel(new BorderLayout());
        lblRow1.setOpaque(false);
        JLabel lblPortName = new JLabel("Binding Port");
        lblPortName.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblPortName.setForeground(new Color(71, 85, 105));
        JLabel lblTcpIp = new JLabel("TCP / IP");
        lblTcpIp.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblTcpIp.setForeground(new Color(148, 163, 184));
        lblRow1.add(lblPortName, BorderLayout.WEST);
        lblRow1.add(lblTcpIp, BorderLayout.EAST);
        portBox.add(lblRow1, BorderLayout.NORTH);

        txtPort = new JTextField(String.valueOf(ServerCore.getPort()));
        txtPort.setFont(new Font("Consolas", Font.BOLD, 14));
        txtPort.setForeground(new Color(15, 23, 42));
        txtPort.setBackground(new Color(248, 250, 252));
        txtPort.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        portBox.add(txtPort, BorderLayout.CENTER);

        JLabel lblPortHelp = new JLabel("Assigned server listening socket socket.bind(port)");
        lblPortHelp.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblPortHelp.setForeground(new Color(148, 163, 184));
        portBox.add(lblPortHelp, BorderLayout.SOUTH);

        fieldsBox.add(portBox);
        fieldsBox.add(Box.createVerticalStrut(10));

        // Max Connections Field
        JPanel maxConnBox = new JPanel(new BorderLayout(0, 4));
        maxConnBox.setOpaque(false);

        JPanel lblRow2 = new JPanel(new BorderLayout());
        lblRow2.setOpaque(false);
        JLabel lblMaxConn = new JLabel("Max Connections");
        lblMaxConn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblMaxConn.setForeground(new Color(71, 85, 105));
        JLabel lblBacklog = new JLabel("Backlog: 50");
        lblBacklog.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblBacklog.setForeground(new Color(148, 163, 184));
        lblRow2.add(lblMaxConn, BorderLayout.WEST);
        lblRow2.add(lblBacklog, BorderLayout.EAST);
        maxConnBox.add(lblRow2, BorderLayout.NORTH);

        JTextField txtMax = new JTextField("100");
        txtMax.setEditable(false);
        txtMax.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtMax.setForeground(new Color(71, 85, 105));
        txtMax.setBackground(new Color(248, 250, 252));
        txtMax.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        maxConnBox.add(txtMax, BorderLayout.CENTER);

        fieldsBox.add(maxConnBox);
        fieldsBox.add(Box.createVerticalStrut(10));

        // Socket Options Checkboxes
        chkTcpNoDelay = new JCheckBox("TCP No Delay (Nagle bypass)", true);
        chkHeartbeat = new JCheckBox("Auto Heartbeat (Keep-Alive)", true);
        chkVerbose = new JCheckBox("Verbose Raw Packet Log", false);

        for (JCheckBox cb : new JCheckBox[]{chkTcpNoDelay, chkHeartbeat, chkVerbose}) {
            cb.setOpaque(false);
            cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            cb.setForeground(new Color(51, 65, 85));
            cb.setFocusPainted(false);
            fieldsBox.add(cb);
            fieldsBox.add(Box.createVerticalStrut(2));
        }

        add(fieldsBox, BorderLayout.CENTER);

        // Buttons Panel
        JPanel btnBox = new JPanel(new GridLayout(2, 1, 0, 8));
        btnBox.setOpaque(false);

        btnEditProps = new ModernButton("Edit Socket Properties", new Color(239, 246, 255), new Color(37, 99, 235), new Color(191, 219, 254));
        btnEditProps.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnEditProps.setPreferredSize(new Dimension(200, 32));
        btnEditProps.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Socket properties are dynamically managed by TCP acceptor.", "Socket Properties", JOptionPane.INFORMATION_MESSAGE));

        btnBroadcast = new ModernButton("Broadcast System Message", new Color(37, 99, 235), Color.WHITE);
        btnBroadcast.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnBroadcast.setPreferredSize(new Dimension(200, 34));
        btnBroadcast.addActionListener(e -> openBroadcastDialog());

        btnBox.add(btnEditProps);
        btnBox.add(btnBroadcast);

        add(btnBox, BorderLayout.SOUTH);
        updateState();
    }

    public int getEnteredPort() {
        try {
            return Integer.parseInt(txtPort.getText().trim());
        } catch (Exception e) {
            return ServerCore.getPort();
        }
    }

    public void updateState() {
        boolean isRunning = ServerCore.isRunning();
        txtPort.setEditable(!isRunning);
        txtPort.setEnabled(!isRunning);

        if (isRunning) {
            lblStatusBadge.setText("Locked (Running)");
            lblStatusBadge.setBackground(new Color(220, 252, 231));
            lblStatusBadge.setForeground(new Color(22, 101, 52));
        } else {
            lblStatusBadge.setText("Editable (Stopped)");
            lblStatusBadge.setBackground(new Color(241, 245, 249));
            lblStatusBadge.setForeground(new Color(71, 85, 105));
        }
    }

    private void openBroadcastDialog() {
        String msg = JOptionPane.showInputDialog(this,
                "Nh\u1EADp n\u1ED9i dung th\u00F4ng b\u00E1o h\u1EC7 th\u1ED1ng mu\u1ED1n g\u1EDFi t\u1EDBi to\u00E0n b\u1ED9 client online:",
                "Broadcast System Message",
                JOptionPane.PLAIN_MESSAGE);

        if (msg != null && !msg.trim().isEmpty()) {
            ServerCore.broadcastSystemMessage(msg.trim());
            JOptionPane.showMessageDialog(this, "\u0110\u00E3 ph\u00E1t th\u00F4ng b\u00E1o h\u1EC7 th\u1ED1ng th\u00E0nh c\u00F4ng!", "Broadcast Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
