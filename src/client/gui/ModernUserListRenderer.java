package client.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;
import java.util.function.Function;

public class ModernUserListRenderer extends JPanel implements ListCellRenderer<Object> {
    private final Map<String, Integer> unreadMap;
    private final Map<String, String> snippetMap;
    private final Map<String, String> timeMap;
    private final Function<String, String> nameCleaner;
    private final String currentUsername;

    private final JLabel lblIcon = new JLabel();
    private final JLabel lblName = new JLabel();
    private final JLabel lblTime = new JLabel();
    private final JLabel lblSnippet = new JLabel();
    private boolean showDot = false;

    private final JComponent dotComponent = new JComponent() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (showDot) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(37, 99, 235)); // Messenger Blue Dot
                int size = 8;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                g2.fillOval(x, y, size, size);
                g2.dispose();
            }
        }
    };

    public ModernUserListRenderer(Map<String, Integer> unreadMap, Map<String, String> snippetMap, Map<String, String> timeMap, Function<String, String> nameCleaner, String currentUsername) {
        this.unreadMap = unreadMap;
        this.snippetMap = snippetMap;
        this.timeMap = timeMap;
        this.nameCleaner = nameCleaner;
        this.currentUsername = currentUsername;

        setOpaque(true);
        setLayout(new GridLayout(2, 1, 0, 2));
        setBorder(new EmptyBorder(6, 12, 6, 12));

        // Dòng 1: Icon (Trái) + Tên (Giữa) + Thời gian (Phải)
        JPanel row1 = new JPanel(new BorderLayout(6, 0));
        row1.setOpaque(false);

        lblIcon.setOpaque(false);
        lblName.setOpaque(false);
        lblTime.setOpaque(false);
        lblTime.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel titleBox = new JPanel(new BorderLayout(6, 0));
        titleBox.setOpaque(false);
        titleBox.add(lblIcon, BorderLayout.WEST);
        titleBox.add(lblName, BorderLayout.CENTER);

        row1.add(titleBox, BorderLayout.CENTER);
        row1.add(lblTime, BorderLayout.EAST);

        // Dòng 2: Nội dung tóm tắt (Trái) & Chấm xanh (Phải)
        JPanel row2 = new JPanel(new BorderLayout(6, 0));
        row2.setOpaque(false);
        lblSnippet.setOpaque(false);
        dotComponent.setOpaque(false);
        dotComponent.setPreferredSize(new Dimension(16, 16));
        row2.add(lblSnippet, BorderLayout.CENTER);
        row2.add(dotComponent, BorderLayout.EAST);

        add(row1);
        add(row2);
    }

    public ModernUserListRenderer(Map<String, Integer> unreadMap, Function<String, String> nameCleaner, String currentUsername) {
        this(unreadMap, null, null, nameCleaner, currentUsername);
    }

    public ModernUserListRenderer(Map<String, Integer> unreadMap, Function<String, String> nameCleaner) {
        this(unreadMap, null, null, nameCleaner, "");
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        text = text.trim();
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 1) + "…";
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (isSelected) {
            setBackground(list.getSelectionBackground());
        } else {
            setBackground(list.getBackground());
        }

        if (value == null) {
            lblIcon.setText("");
            lblName.setText("");
            lblTime.setText("");
            lblSnippet.setText("");
            showDot = false;
            return this;
        }

        String valStr = value.toString();
        String cleanName = nameCleaner != null ? nameCleaner.apply(valStr) : valStr;

        int unread = (unreadMap != null) ? unreadMap.getOrDefault(cleanName, 0) : 0;
        boolean isUnread = unread > 0;
        this.showDot = isUnread;

        // Thời gian tin nhắn cuối (10px, góc phải trên)
        String timeStr = (timeMap != null) ? timeMap.getOrDefault(cleanName, "") : "";
        lblTime.setText(timeStr);
        lblTime.setFont(new Font("Segoe UI", isUnread ? Font.BOLD : Font.PLAIN, 10));
        lblTime.setForeground(isUnread ? new Color(37, 99, 235) : new Color(148, 163, 184));

        // Tóm tắt tin nhắn cuối (11px, dòng 2)
        String snippet = (snippetMap != null) ? snippetMap.getOrDefault(cleanName, "") : "";
        if (snippet.isEmpty()) {
            if ("Cộng đồng".equals(cleanName)) {
                snippet = "Phòng chat chung mọi người";
            } else if (valStr.startsWith("[GROUP]")) {
                snippet = "Nhóm trò chuyện";
            } else {
                snippet = "Bắt đầu cuộc trò chuyện";
            }
        }
        lblSnippet.setText(truncate(snippet, 26));
        lblSnippet.setFont(new Font("Segoe UI", isUnread ? Font.BOLD : Font.PLAIN, 11));
        lblSnippet.setForeground(isUnread ? new Color(15, 23, 42) : new Color(100, 116, 139));

        // Dòng 1: Hoàn toàn dùng Plain Text, TUYỆT ĐỐI KHÔNG DÙNG HTML (Tránh rớt dòng trên Swing)
        if (valStr.equals("Cộng đồng")) {
            lblIcon.setText("🌐");
            lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
            lblIcon.setForeground(new Color(37, 99, 235));
            lblName.setText("General Chat");
            lblName.setFont(new Font("Segoe UI", isUnread ? Font.BOLD : Font.PLAIN, 12));
            lblName.setForeground(new Color(37, 99, 235));
        } else if (valStr.startsWith("[GROUP]")) {
            lblIcon.setText("👥");
            lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
            lblIcon.setForeground(new Color(124, 58, 237));
            lblName.setText(cleanName);
            lblName.setFont(new Font("Segoe UI", isUnread ? Font.BOLD : Font.PLAIN, 12));
            lblName.setForeground(new Color(124, 58, 237));
        } else if (valStr.contains(":ONLINE")) {
            String uName = valStr.split(":")[0];
            String dispName = NicknameDialog.getPrivateDisplayName(currentUsername, uName);
            lblIcon.setText("●");
            lblIcon.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblIcon.setForeground(new Color(16, 185, 129));
            lblName.setText(dispName);
            lblName.setFont(new Font("Segoe UI", isUnread ? Font.BOLD : Font.PLAIN, 12));
            lblName.setForeground(isUnread ? new Color(15, 23, 42) : new Color(30, 41, 59));
        } else if (valStr.contains(":OFFLINE")) {
            String uName = valStr.split(":")[0];
            String dispName = NicknameDialog.getPrivateDisplayName(currentUsername, uName);
            lblIcon.setText("○");
            lblIcon.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lblIcon.setForeground(new Color(148, 163, 184));
            lblName.setText(dispName);
            lblName.setFont(new Font("Segoe UI", isUnread ? Font.BOLD : Font.PLAIN, 12));
            lblName.setForeground(isUnread ? new Color(15, 23, 42) : new Color(100, 116, 139));
        } else {
            String dispName = NicknameDialog.getPrivateDisplayName(currentUsername, cleanName);
            lblIcon.setText("");
            lblName.setText(dispName);
            lblName.setFont(new Font("Segoe UI", isUnread ? Font.BOLD : Font.PLAIN, 12));
            lblName.setForeground(isUnread ? new Color(15, 23, 42) : new Color(30, 41, 59));
        }

        return this;
    }
}
