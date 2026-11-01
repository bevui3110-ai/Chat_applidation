package client.gui;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class StickerPickerPopup {

    public interface StickerSelectionCallback {
        void onStickerSelected(String stickerName, byte[] stickerData);
    }

    public static void showPopup(Component invoker, String username, StickerSelectionCallback callback) {
        JPopupMenu stickerPopup = new JPopupMenu();
        stickerPopup.setBackground(new Color(30, 32, 36));
        stickerPopup.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true));

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(30, 32, 36));
        mainPanel.setPreferredSize(new Dimension(340, 320));

        // Header Title & Create Button
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 26, 30));
        headerPanel.setBorder(new EmptyBorder(8, 12, 8, 12));

        JLabel lblTitle = new JLabel("🎨  Nhãn dán (Stickers)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(Color.WHITE);

        JButton btnCreate = new JButton("+ Tạo Sticker");
        btnCreate.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnCreate.setForeground(Color.WHITE);
        btnCreate.setBackground(new Color(37, 99, 235));
        btnCreate.setFocusPainted(false);
        btnCreate.setBorderPainted(false);
        btnCreate.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        headerPanel.add(lblTitle, BorderLayout.WEST);
        headerPanel.add(btnCreate, BorderLayout.EAST);

        // Content Area with Scroll
        JPanel gridPanel = new JPanel(new GridLayout(0, 4, 8, 8));
        gridPanel.setBackground(new Color(30, 32, 36));
        gridPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(10);
        scrollPane.getViewport().setBackground(new Color(30, 32, 36));

        // Load stickers runnable
        Runnable loadStickers = () -> {
            gridPanel.removeAll();
            List<StickerItem> stickers = getStickerList(username);
            for (StickerItem item : stickers) {
                JPanel stickerCard = new JPanel(new BorderLayout());
                stickerCard.setBackground(new Color(42, 45, 52));
                stickerCard.setPreferredSize(new Dimension(68, 68));

                JButton btnSticker = new JButton();
                btnSticker.setBackground(new Color(42, 45, 52));
                btnSticker.setFocusPainted(false);
                btnSticker.setBorderPainted(false);
                btnSticker.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                if (item.icon != null) {
                    btnSticker.setIcon(item.icon);
                } else {
                    btnSticker.setText(item.name);
                    btnSticker.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
                    btnSticker.setForeground(Color.WHITE);
                }

                btnSticker.addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { btnSticker.setBackground(new Color(51, 65, 85)); }
                    @Override public void mouseExited(MouseEvent e) { btnSticker.setBackground(new Color(42, 45, 52)); }
                    @Override public void mousePressed(MouseEvent e) {
                        if (SwingUtilities.isRightMouseButton(e) && item.isCustom) {
                            JPopupMenu deleteMenu = new JPopupMenu();
                            JMenuItem itemDelete = new JMenuItem("❌ Xóa nhãn dán này");
                            itemDelete.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                            itemDelete.addActionListener(ev -> {
                                int confirm = JOptionPane.showConfirmDialog(
                                        invoker,
                                        "Bạn có chắc chắn muốn xóa nhãn dán này?",
                                        "Xác nhận xóa Sticker",
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.WARNING_MESSAGE
                                );
                                if (confirm == JOptionPane.YES_OPTION) {
                                    deleteCustomSticker(username, item.fileName);
                                    stickerPopup.setVisible(false);
                                }
                            });
                            deleteMenu.add(itemDelete);
                            deleteMenu.show(btnSticker, e.getX(), e.getY());
                        }
                    }
                });

                btnSticker.addActionListener(e -> {
                    if (callback != null) {
                        callback.onStickerSelected(item.fileName, item.data);
                    }
                    stickerPopup.setVisible(false);
                });

                stickerCard.add(btnSticker, BorderLayout.CENTER);
                gridPanel.add(stickerCard);
            }
            gridPanel.revalidate();
            gridPanel.repaint();
        };

        loadStickers.run();

        // Create Sticker Handler (via StickerCropDialog)
        btnCreate.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Chọn ảnh làm Nhãn dán (PNG, JPG, GIF)");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Hình ảnh", "png", "jpg", "jpeg", "gif"));

            int res = chooser.showOpenDialog(invoker);
            if (res == JFileChooser.APPROVE_OPTION) {
                File selFile = chooser.getSelectedFile();
                if (selFile != null && selFile.exists()) {
                    stickerPopup.setVisible(false);
                    StickerCropDialog.showDialog(invoker, username, selFile, () -> {
                        // Sticker created
                    });
                }
            }
        });

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        stickerPopup.add(mainPanel);
        stickerPopup.show(invoker, 0, -mainPanel.getPreferredSize().height - 5);
    }

    private static class StickerItem {
        String name;
        String fileName;
        ImageIcon icon;
        byte[] data;
        boolean isCustom;

        StickerItem(String name, String fileName, ImageIcon icon, byte[] data, boolean isCustom) {
            this.name = name;
            this.fileName = fileName;
            this.icon = icon;
            this.data = data;
            this.isCustom = isCustom;
        }
    }

    private static List<StickerItem> getStickerList(String username) {
        List<StickerItem> list = new ArrayList<>();

        File userDir = new File("user_data/" + username + "/stickers");
        if (!userDir.exists()) userDir.mkdirs();

        String[] builtInStickers = {"🔥", "❤️", "👍", "🎉", "💩", "⭐", "🚀", "🥳"};
        Color[] stickerColors = {
            new Color(255, 69, 0),   // 🔥 Fire (orange-red)
            new Color(239, 68, 68),  // ❤️ Heart (red)
            new Color(37, 99, 235),  // 👍 Like (blue)
            new Color(168, 85, 247), // 🎉 Party (purple)
            new Color(120, 53, 15),  // 💩 Poo (brown)
            new Color(245, 158, 11), // ⭐ Star (gold)
            new Color(99, 102, 241), // 🚀 Rocket (indigo)
            new Color(236, 72, 153)  // 🥳 Party face (pink)
        };

        for (int i = 0; i < builtInStickers.length; i++) {
            String s = builtInStickers[i];
            Color color = stickerColors[i];
            try {
                BufferedImage bimg = new BufferedImage(140, 140, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = bimg.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                g2.setColor(color);
                Font font = new Font("Segoe UI Emoji", Font.PLAIN, 85);
                g2.setFont(font);
                FontMetrics fm = g2.getFontMetrics(font);
                int textWidth = fm.stringWidth(s);
                int x = Math.max(0, (140 - textWidth) / 2);
                int y = (140 - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(s, x, y);
                g2.dispose();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bimg, "png", baos);
                byte[] bytes = baos.toByteArray();
                ImageIcon icon = new ImageIcon(bimg.getScaledInstance(56, 56, Image.SCALE_SMOOTH));
                list.add(new StickerItem(s, "sticker_" + s.hashCode() + ".png", icon, bytes, false));
            } catch (Exception ignored) {}
        }

        File[] files = userDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif");
        });

        if (files != null) {
            for (File f : files) {
                try {
                    byte[] bytes = Files.readAllBytes(f.toPath());
                    ImageIcon origIcon = new ImageIcon(bytes);
                    Image scaled = origIcon.getImage().getScaledInstance(56, 56, Image.SCALE_SMOOTH);
                    ImageIcon icon = new ImageIcon(scaled);
                    list.add(new StickerItem(f.getName(), f.getName(), icon, bytes, true));
                } catch (Exception ignored) {}
            }
        }

        return list;
    }

    private static void deleteCustomSticker(String username, String fileName) {
        try {
            File file = new File("user_data/" + username + "/stickers", fileName);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
