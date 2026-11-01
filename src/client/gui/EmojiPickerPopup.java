package client.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class EmojiPickerPopup {

    public interface EmojiSelectionCallback {
        void onEmojiSelected(String emoji);
    }

    public static void showPopup(Component invoker, EmojiSelectionCallback callback) {
        JPopupMenu emojiPopup = new JPopupMenu();
        emojiPopup.setBackground(new Color(30, 32, 36));
        emojiPopup.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85), 1, true));

        JPanel gridPanel = new JPanel(new GridLayout(6, 8, 4, 4));
        gridPanel.setBackground(new Color(30, 32, 36));
        gridPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] emojis = {
                "\uD83D\uDE00", "\uD83D\uDE03", "\uD83D\uDE04", "\uD83D\uDE01", "\uD83D\uDE06", "\uD83D\uDE0A", "\uD83D\uDE02", "\uD83E\uDD23",
                "\u263A\uFE0F", "\uD83D\uDE07", "\uD83D\uDE42", "\uD83D\uDE43", "\uD83D\uDE09", "\uD83D\uDE0C", "\uD83D\uDE0D", "\uD83E\uDD70",
                "\uD83D\uDE18", "\uD83D\uDE17", "\uD83D\uDE19", "\uD83D\uDE1A", "\uD83D\uDE0B", "\uD83D\uDE1B", "\uD83D\uDE1D", "\uD83D\uDE1C",
                "\uD83E\uDD2A", "\uD83E\uDD28", "\uD83E\uDDD0", "\uD83E\uDD13", "\uD83D\uDE0E", "\uD83E\uDD73", "\uD83D\uDE0F", "\uD83D\uDE12",
                "\uD83D\uDE1E", "\uD83D\uDE14", "\uD83D\uDE1F", "\uD83D\uDE15", "\uD83D\uDE41", "\uD83E\uDD7A", "\uD83D\uDE22", "\uD83D\uDE2D",
                "\uD83D\uDE24", "\uD83D\uDE20", "\uD83D\uDE21", "\uD83D\uDC4D", "\uD83D\uDC4E", "\u2764\uFE0F", "\uD83D\uDD25", "\uD83C\uDF89"
        };

        for (String emoji : emojis) {
            JButton btnEmoji = new JButton(emoji);
            btnEmoji.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            btnEmoji.setForeground(Color.WHITE);
            btnEmoji.setBackground(new Color(30, 32, 36));
            btnEmoji.setFocusPainted(false);
            btnEmoji.setBorderPainted(false);
            btnEmoji.setMargin(new Insets(0, 0, 0, 0));
            btnEmoji.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnEmoji.setPreferredSize(new Dimension(38, 38));

            btnEmoji.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { btnEmoji.setBackground(new Color(51, 65, 85)); }
                @Override public void mouseExited(MouseEvent e) { btnEmoji.setBackground(new Color(30, 32, 36)); }
            });

            btnEmoji.addActionListener(e -> {
                if (callback != null) {
                    callback.onEmojiSelected(emoji);
                }
                emojiPopup.setVisible(false);
            });

            gridPanel.add(btnEmoji);
        }

        emojiPopup.add(gridPanel);
        emojiPopup.show(invoker, 0, -gridPanel.getPreferredSize().height - 5);
    }
}
