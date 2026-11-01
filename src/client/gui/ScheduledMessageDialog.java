package client.gui;

import common.MessagePacket;
import client.network.ClientSocketHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Timer;

public class ScheduledMessageDialog {

    public static class ScheduledTaskItem {
        public String id;
        public String target;
        public String content;
        public long executeTime;
        public Timer timer;

        public ScheduledTaskItem(String id, String target, String content, long executeTime, Timer timer) {
            this.id = id;
            this.target = target;
            this.content = content;
            this.executeTime = executeTime;
            this.timer = timer;
        }
    }

    private static final List<ScheduledTaskItem> pendingTasks = Collections.synchronizedList(new ArrayList<>());

    public interface ScheduledMessageCallback {
        void onScheduledMessageSent(String target, String content, String timestamp, String msgId);
    }

    public static List<ScheduledTaskItem> getPendingTasks() {
        return pendingTasks;
    }

    public static void showDialog(JFrame parent, String currentTarget, String draftContent, ClientSocketHandler socketHandler, String username, ScheduledMessageCallback callback) {
        JDialog dialog = new JDialog(parent, "Hẹn giờ gửi tin nhắn", true);
        dialog.setSize(440, 380);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);
        dialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));
        panel.setBackground(Color.WHITE);

        JLabel lblTarget = new JLabel("Gửi tới: " + currentTarget);
        lblTarget.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTarget.setForeground(new Color(37, 99, 235));

        JLabel lblContent = new JLabel("Nội dung tin nhắn:");
        lblContent.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblContent.setForeground(new Color(15, 23, 42));

        JTextArea txtContentArea = new JTextArea(draftContent != null ? draftContent : "");
        txtContentArea.setRows(3);
        txtContentArea.setLineWrap(true);
        txtContentArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtContentArea.setBackground(new Color(248, 250, 252));
        JScrollPane scrollArea = new JScrollPane(txtContentArea);
        scrollArea.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));

        JLabel lblTime = new JLabel("Thời gian gửi (sau bao nhiêu phút):");
        lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTime.setForeground(new Color(15, 23, 42));

        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, 1440, 1);
        JSpinner spinnerMinutes = new JSpinner(spinnerModel);
        spinnerMinutes.setFont(new Font("Segoe UI", Font.BOLD, 13));
        spinnerMinutes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnViewPending = createButton("Danh sách hẹn giờ (" + pendingTasks.size() + ")", new Color(241, 245, 249), new Color(226, 232, 240), new Color(15, 23, 42));
        JButton btnSchedule = createButton("Lên lịch gửi", new Color(37, 99, 235), new Color(29, 78, 216), Color.WHITE);

        btnViewPending.addActionListener(e -> showPendingListDialog(parent));

        btnSchedule.addActionListener(e -> {
            String text = txtContentArea.getText().trim();
            if (text.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng nhập nội dung tin nhắn!");
                return;
            }

            int mins = (Integer) spinnerMinutes.getValue();
            long delayMs = mins * 60 * 1000L;
            long targetTimeMs = System.currentTimeMillis() + delayMs;
            String taskId = UUID.randomUUID().toString();

            Timer timer = new Timer();
            ScheduledTaskItem taskItem = new ScheduledTaskItem(taskId, currentTarget, text, targetTimeMs, timer);

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String currentDbTime = sdf.format(new Date());

                        String packetType = "Cộng đồng".equals(currentTarget) ? "CHAT_ALL" : "CHAT_PRIVATE";
                        MessagePacket packet = new MessagePacket(packetType, username, currentTarget, text, taskId);
                        packet.setTimestamp(currentDbTime);

                        socketHandler.sendPacket(packet);
                        pendingTasks.remove(taskItem);

                        if (callback != null) {
                            SwingUtilities.invokeLater(() -> callback.onScheduledMessageSent(currentTarget, text, currentDbTime, taskId));
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }, delayMs);

            pendingTasks.add(taskItem);
            JOptionPane.showMessageDialog(dialog, "Đã hẹn giờ gửi tin nhắn sau " + mins + " phút!");
            dialog.dispose();
        });

        btnPanel.add(btnViewPending);
        btnPanel.add(btnSchedule);

        panel.add(lblTarget);
        panel.add(Box.createVerticalStrut(10));
        panel.add(lblContent);
        panel.add(Box.createVerticalStrut(4));
        panel.add(scrollArea);
        panel.add(Box.createVerticalStrut(10));
        panel.add(lblTime);
        panel.add(Box.createVerticalStrut(4));
        panel.add(spinnerMinutes);
        panel.add(Box.createVerticalStrut(16));
        panel.add(btnPanel);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    public static void showPendingListDialog(JFrame parent) {
        JDialog dialog = new JDialog(parent, "Danh sách tin nhắn chờ gửi", true);
        dialog.setSize(520, 420);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 12));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        // HEADER
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("<html><span style='font-family:Segoe UI Emoji;'>⏱️</span> <span style='font-family:Segoe UI; font-weight:bold;'>Tin nhắn đang chờ gửi</span></html>");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(new Color(15, 23, 42));

        JLabel lblSub = new JLabel("Các tin nhắn sẽ tự động gửi khi đếm ngược kết thúc");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(100, 116, 139));

        headerPanel.add(lblTitle);
        headerPanel.add(Box.createVerticalStrut(2));
        headerPanel.add(lblSub);

        // LIST MODEL & CARDS
        DefaultListModel<ScheduledTaskItem> model = new DefaultListModel<>();
        synchronized (pendingTasks) {
            for (ScheduledTaskItem item : pendingTasks) {
                model.addElement(item);
            }
        }

        JList<ScheduledTaskItem> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(Color.WHITE);
        list.setFixedCellHeight(68);

        list.setCellRenderer((l, item, index, isSelected, cellHasFocus) -> {
            JPanel card = new JPanel(new BorderLayout(0, 4)) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(isSelected ? new Color(239, 246, 255) : new Color(248, 250, 252));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight() - 6, 8, 8);
                    g2.setColor(isSelected ? new Color(37, 99, 235) : new Color(226, 232, 240));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 7, 8, 8);
                    g2.dispose();
                }
            };
            card.setOpaque(false);
            card.setBorder(new EmptyBorder(8, 12, 10, 12));

            if (item != null) {
                long remainingSecs = Math.max(0, (item.executeTime - System.currentTimeMillis()) / 1000);

                JPanel topRow = new JPanel(new BorderLayout());
                topRow.setOpaque(false);

                JLabel lblTarget = new JLabel("Gửi tới: @" + item.target);
                lblTarget.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lblTarget.setForeground(new Color(37, 99, 235));

                JLabel lblBadge = new JLabel("⏱ Còn " + remainingSecs + "s");
                lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
                lblBadge.setForeground(new Color(217, 119, 6));
                lblBadge.setBackground(new Color(254, 243, 199));
                lblBadge.setOpaque(true);
                lblBadge.setBorder(new EmptyBorder(2, 6, 2, 6));

                topRow.add(lblTarget, BorderLayout.WEST);
                topRow.add(lblBadge, BorderLayout.EAST);

                String previewText = item.content;
                if (previewText != null && previewText.length() > 50) {
                    previewText = previewText.substring(0, 50) + "...";
                }
                JLabel lblContent = new JLabel("<html><i>" + (previewText != null ? previewText : "") + "</i></html>");
                lblContent.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                lblContent.setForeground(new Color(51, 65, 85));

                card.add(topRow, BorderLayout.NORTH);
                card.add(lblContent, BorderLayout.CENTER);
            }
            return card;
        });

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(226, 232, 240)));

        // EMPTY STATE PANEL
        JPanel emptyPanel = new JPanel(new GridBagLayout());
        emptyPanel.setBackground(new Color(248, 250, 252));
        JLabel lblEmpty = new JLabel("(Hiện không có tin nhắn nào đang chờ gửi)");
        lblEmpty.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblEmpty.setForeground(new Color(100, 116, 139));
        emptyPanel.add(lblEmpty);

        JPanel centerContainer = new JPanel(new CardLayout());
        centerContainer.add(scrollPane, "LIST");
        centerContainer.add(emptyPanel, "EMPTY");
        CardLayout centerCard = (CardLayout) centerContainer.getLayout();

        if (model.isEmpty()) {
            centerCard.show(centerContainer, "EMPTY");
        } else {
            centerCard.show(centerContainer, "LIST");
        }

        // FOOTER BUTTONS
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footerPanel.setOpaque(false);

        JButton btnClose = createButton("Đóng", new Color(241, 245, 249), new Color(226, 232, 240), new Color(51, 65, 85));

        JButton btnCancelTask = createButton("Hủy tin nhắn đã chọn", new Color(239, 68, 68), new Color(220, 38, 38), Color.WHITE);
        btnCancelTask.addActionListener(e -> {
            int idx = list.getSelectedIndex();
            if (idx >= 0 && idx < pendingTasks.size()) {
                ScheduledTaskItem item = pendingTasks.remove(idx);
                if (item.timer != null) item.timer.cancel();
                model.remove(idx);
                JOptionPane.showMessageDialog(dialog, "Đã hủy gửi tin nhắn hẹn giờ!");
                if (model.isEmpty()) {
                    centerCard.show(centerContainer, "EMPTY");
                }
            } else if (!model.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng chọn tin nhắn muốn hủy!");
            }
        });

        btnClose.addActionListener(e -> dialog.dispose());

        footerPanel.add(btnClose);
        footerPanel.add(btnCancelTask);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(centerContainer, BorderLayout.CENTER);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private static JButton createButton(String text, Color bgNormal, Color bgHover, Color fgText) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(fgText);
        btn.setBackground(bgNormal);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(8, 16, 8, 16)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(bgHover); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { btn.setBackground(bgNormal); }
        });
        return btn;
    }
}
