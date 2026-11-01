package client.gui;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

public class AvatarUtils {

    /**
     * Render avatar into graphics context:
     * - If custom avatar image exists for targetUser, draw it clipped to circle.
     * - Otherwise draw Facebook silhouette default avatar.
     */
    public static void drawAvatar(Graphics2D g2, String targetUser, int width, int height) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        BufferedImage customImg = ProfileSettingsDialog.getCustomAvatarImage(targetUser);
        if (customImg != null) {
            int imgW = customImg.getWidth();
            int imgH = customImg.getHeight();
            int minDim = Math.min(imgW, imgH);
            int sx = (imgW - minDim) / 2;
            int sy = (imgH - minDim) / 2;

            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setClip(new Ellipse2D.Float(0, 0, width, height));
            g2.drawImage(customImg, 0, 0, width, height, sx, sy, sx + minDim, sy + minDim, null);
        } else {
            drawFacebookDefaultAvatar(g2, width, height);
        }
    }

    /**
     * Draw Facebook/Zalo style default silhouette avatar.
     * - Light gray background circle (#E4E6EB).
     * - Darker neutral gray silhouette head & body (#8A8D91).
     */
    public static void drawFacebookDefaultAvatar(Graphics2D g2, int w, int h) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Clip to circular boundary
        Shape circleClip = new Ellipse2D.Float(0, 0, w, h);
        g2.setClip(circleClip);

        // 1. Light gray background (#E4E6EB)
        g2.setColor(new Color(228, 230, 235));
        g2.fill(circleClip);

        // 2. Silhouette head & shoulder/body (#8A8D91)
        g2.setColor(new Color(138, 141, 145));

        // Head: circle centered at upper portion
        int headDiameter = (int) (w * 0.38);
        int headX = (w - headDiameter) / 2;
        int headY = (int) (h * 0.18);
        g2.fillOval(headX, headY, headDiameter, headDiameter);

        // Body / Shoulders: filled arc/ellipse hugging bottom edge
        int bodyWidth = (int) (w * 0.78);
        int bodyHeight = (int) (h * 0.54);
        int bodyX = (w - bodyWidth) / 2;
        int bodyY = (int) (h * 0.52);

        g2.fillArc(bodyX, bodyY, bodyWidth, bodyHeight, 0, 180);
    }
}
