package server.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class StatCardPanel extends JPanel {
    private final JLabel lblIcon;
    private final JLabel lblTitle;
    private final JLabel lblValue;
    private final JLabel lblSubtext;
    private final JPanel badgePanel;

    public StatCardPanel(String iconText, String title, String initialValue, String initialSubtext, Color valueColor) {
        setLayout(new BorderLayout(10, 8));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(14, 16, 14, 16)
        ));

        // Top Row: Title + Icon
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        lblTitle = new JLabel(title.toUpperCase());
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitle.setForeground(new Color(100, 116, 139));
        topPanel.add(lblTitle, BorderLayout.WEST);

        lblIcon = new JLabel(iconText);
        lblIcon.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        topPanel.add(lblIcon, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Center: Value
        lblValue = new JLabel(initialValue);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblValue.setForeground(valueColor != null ? valueColor : new Color(15, 23, 42));
        add(lblValue, BorderLayout.CENTER);

        // Bottom: Subtext & Badge
        badgePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        badgePanel.setOpaque(false);

        lblSubtext = new JLabel(initialSubtext);
        lblSubtext.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSubtext.setForeground(new Color(100, 116, 139));
        badgePanel.add(lblSubtext);

        add(badgePanel, BorderLayout.SOUTH);
    }

    public void updateValue(String value, Color color) {
        lblValue.setText(value);
        if (color != null) {
            lblValue.setForeground(color);
        }
    }

    public void updateSubtext(String subtext) {
        lblSubtext.setText(subtext);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g2.dispose();
    }
}
