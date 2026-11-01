package server.gui;

import javax.swing.*;
import java.awt.*;

public class ModernButton extends JButton {
    private Color normalBg;
    private Color textColor;
    private Color borderColor;
    private int cornerRadius = 8;

    public ModernButton(String text, Color bg, Color textColor) {
        this(text, bg, textColor, null);
    }

    public ModernButton(String text, Color bg, Color textColor, Color borderColor) {
        super(text);
        this.normalBg = bg;
        this.textColor = textColor;
        this.borderColor = borderColor;
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setFont(new Font("Segoe UI", Font.BOLD, 12));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public void setCustomColors(Color bg, Color textColor) {
        setCustomColors(bg, textColor, this.borderColor);
    }

    public void setCustomColors(Color bg, Color textColor, Color borderColor) {
        this.normalBg = bg;
        this.textColor = textColor;
        this.borderColor = borderColor;
        repaint();
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        if (!isEnabled()) {
            g2.setColor(new Color(241, 245, 249));
            g2.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);
            if (borderColor != null) {
                g2.setColor(new Color(226, 232, 240));
                g2.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);
            }
            g2.setColor(new Color(148, 163, 184));
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = (width - fm.stringWidth(getText())) / 2;
            int y = (height - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), x, y);
            g2.dispose();
            return;
        }

        Color bg = normalBg;
        if (getModel().isPressed()) {
            bg = normalBg.darker();
        } else if (getModel().isRollover()) {
            bg = normalBg.brighter();
        }

        g2.setColor(bg);
        g2.fillRoundRect(0, 0, width, height, cornerRadius, cornerRadius);

        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, width - 1, height - 1, cornerRadius, cornerRadius);
        }

        g2.setColor(textColor);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int x = (width - fm.stringWidth(getText())) / 2;
        int y = (height - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(getText(), x, y);

        g2.dispose();
    }
}
