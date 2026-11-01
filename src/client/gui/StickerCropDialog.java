package client.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class StickerCropDialog {

    public interface StickerCropCallback {
        void onStickerCreated();
    }

    public static void showDialog(Component invoker, String username, File sourceFile, StickerCropCallback callback) {
        Window parentWindow = SwingUtilities.getWindowAncestor(invoker);
        JDialog dialog = new JDialog(parentWindow, "Tạo & Xem trước Sticker", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(380, 420);
        dialog.setLocationRelativeTo(invoker);
        dialog.setResizable(false);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel lblTitle = new JLabel("Xem trước Nhãn dán (Tỉ lệ 1:1 vuông)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(new Color(15, 23, 42));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Process Image preview
        BufferedImage croppedImg = null;
        boolean isGif = sourceFile.getName().toLowerCase().endsWith(".gif");

        try {
            if (!isGif) {
                BufferedImage original = ImageIO.read(sourceFile);
                if (original != null) {
                    int w = original.getWidth();
                    int h = original.getHeight();
                    int minDim = Math.min(w, h);
                    int x = (w - minDim) / 2;
                    int y = (h - minDim) / 2;

                    BufferedImage cropped = original.getSubimage(x, y, minDim, minDim);

                    int targetDim = 120;
                    croppedImg = new BufferedImage(targetDim, targetDim, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = croppedImg.createGraphics();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int pad = 8;
                    int innerSize = targetDim - (pad * 2); // 104x104

                    // 1. Drop shadow
                    g2.setColor(new Color(0, 0, 0, 35));
                    g2.fillRoundRect(pad + 2, pad + 3, innerSize, innerSize, 24, 24);

                    // 2. Thick white sticker die-cut background border
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(pad - 3, pad - 3, innerSize + 6, innerSize + 6, 28, 28);

                    // 3. Cropped image rounded clip
                    g2.setClip(new java.awt.geom.RoundRectangle2D.Float(pad, pad, innerSize, innerSize, 20, 20));
                    g2.drawImage(cropped, pad, pad, innerSize, innerSize, null);
                    g2.setClip(null);

                    // 4. White stroke border
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(2.5f));
                    g2.drawRoundRect(pad, pad, innerSize, innerSize, 20, 20);

                    g2.dispose();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        final BufferedImage finalCroppedImg = croppedImg;

        // Preview Panel
        JPanel previewPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(241, 245, 249));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(226, 232, 240));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);

                if (finalCroppedImg != null) {
                    int drawX = (getWidth() - finalCroppedImg.getWidth()) / 2;
                    int drawY = (getHeight() - finalCroppedImg.getHeight()) / 2;
                    g2.drawImage(finalCroppedImg, drawX, drawY, null);
                } else if (isGif) {
                    ImageIcon icon = new ImageIcon(sourceFile.getAbsolutePath());
                    Image img = icon.getImage().getScaledInstance(140, 140, Image.SCALE_DEFAULT);
                    ImageIcon scaledIcon = new ImageIcon(img);
                    int drawX = (getWidth() - 140) / 2;
                    int drawY = (getHeight() - 140) / 2;
                    scaledIcon.paintIcon(this, g2, drawX, drawY);
                }
                g2.dispose();
            }
        };
        previewPanel.setPreferredSize(new Dimension(200, 200));
        previewPanel.setMaximumSize(new Dimension(200, 200));
        previewPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        previewPanel.setOpaque(false);

        JLabel lblTip = new JLabel("Ảnh sẽ tự động cắt khung vuông chuẩn không dãn bẹp");
        lblTip.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblTip.setForeground(new Color(100, 116, 139));
        lblTip.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setOpaque(false);
        btnPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnCancel = createButton("Hủy", new Color(241, 245, 249), new Color(226, 232, 240), new Color(15, 23, 42));
        JButton btnConfirm = createButton("Xác nhận tạo Sticker", new Color(37, 99, 235), new Color(29, 78, 216), Color.WHITE);

        btnCancel.addActionListener(e -> dialog.dispose());

        btnConfirm.addActionListener(e -> {
            try {
                File userDir = new File("user_data/" + username + "/stickers");
                if (!userDir.exists()) userDir.mkdirs();

                String extension = isGif ? "gif" : "png";
                String destName = "custom_" + System.currentTimeMillis() + "." + extension;
                File destFile = new File(userDir, destName);

                if (isGif) {
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else if (finalCroppedImg != null) {
                    ImageIO.write(finalCroppedImg, "png", destFile);
                }

                JOptionPane.showMessageDialog(dialog, "Đã tạo Sticker thành công!");
                dialog.dispose();
                if (callback != null) callback.onStickerCreated();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Lỗi khi lưu Sticker mới!");
            }
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnConfirm);

        mainPanel.add(lblTitle);
        mainPanel.add(Box.createVerticalStrut(16));
        mainPanel.add(previewPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(lblTip);
        mainPanel.add(Box.createVerticalStrut(16));
        mainPanel.add(btnPanel);

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
