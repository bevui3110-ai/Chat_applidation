package client.gui;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class VoiceRecorderDialog {

    public interface VoiceRecorderCallback {
        void onSendVoiceRecord(byte[] wavBytes, String durationStr, String fileName);
    }

    private static final Color COLOR_CARD_BG = Color.WHITE;
    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);
    private static final Color COLOR_PRIMARY_HOVER = new Color(29, 78, 216);
    private static final Color COLOR_INPUT_BG = new Color(241, 245, 249);
    private static final Color COLOR_TEXT_DARK = new Color(15, 23, 42);
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);

    public static void showDialog(JFrame parent, String username, String currentChatTarget, VoiceRecorderCallback callback) {
        JDialog recordDialog = new JDialog(parent, "Ghi âm gi\u1ECDng nói th\u1EF1c t\u1EBF", true);
        recordDialog.setSize(360, 250);
        recordDialog.setLocationRelativeTo(parent);
        recordDialog.setResizable(false);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(COLOR_CARD_BG);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel lblTitle = new JLabel("\uD83C\uDF99\uFE0F GHI ÂM GI\u1ECCNG NÓI TH\u1EF0C T\u1EBE");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(COLOR_TEXT_DARK);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTimer = new JLabel("\uD83D\uDD34 00:00");
        lblTimer.setFont(new Font("Monospaced", Font.BOLD, 24));
        lblTimer.setForeground(new Color(225, 29, 72));
        lblTimer.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblStatus = new JLabel("Đang k\u1EBFt n\u1ED1i Micro...");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStatus.setForeground(COLOR_TEXT_MUTED);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
        ByteArrayOutputStream recordedBytes = new ByteArrayOutputStream();
        final TargetDataLine[] targetLineRef = new TargetDataLine[1];
        final boolean[] isRecording = {true};

        new Thread(() -> {
            try {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                if (!AudioSystem.isLineSupported(info)) {
                    SwingUtilities.invokeLater(() -> lblStatus.setText("\u274C Không tìm th\u1EA5y Micro!"));
                    return;
                }
                TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();
                targetLineRef[0] = line;

                SwingUtilities.invokeLater(() -> lblStatus.setText("\uD83D\uDD34 Đang ghi âm t\u1EEB Micro... (Nói vào micro c\u1EE7a b\u1EA1n)"));

                byte[] buffer = new byte[1024];
                while (isRecording[0]) {
                    int read = line.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        recordedBytes.write(buffer, 0, read);
                    }
                }
                line.stop();
                line.close();
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> lblStatus.setText("\u274C L\u1ED7i m\u1EDF Micro: " + ex.getMessage()));
            }
        }).start();

        final int[] seconds = {0};
        Timer timer = new Timer(1000, e -> {
            seconds[0]++;
            lblTimer.setText(String.format("\uD83D\uDD34 %02d:%02d", seconds[0] / 60, seconds[0] % 60));
        });
        timer.start();

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnSendAudio = createStyledButton("G\u1EEDi tin nh\u1EAFn tho\u1EA1i", COLOR_PRIMARY, COLOR_PRIMARY_HOVER, Color.WHITE);
        btnSendAudio.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSendAudio.setPreferredSize(new Dimension(150, 36));

        JButton btnCancel = createStyledButton("H\u1EE7y", Color.WHITE, COLOR_INPUT_BG, COLOR_TEXT_DARK);
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCancel.setPreferredSize(new Dimension(70, 36));

        btnSendAudio.addActionListener(e -> {
            isRecording[0] = false;
            timer.stop();

            if (targetLineRef[0] != null) {
                targetLineRef[0].stop();
            }

            byte[] pcmData = recordedBytes.toByteArray();
            byte[] wavBytes = convertPcmToWav(pcmData, format);

            int durationSec = seconds[0] == 0 ? 1 : seconds[0];
            String durationStr = String.format("%02d:%02d", durationSec / 60, durationSec % 60);

            String fileName = "Voice_" + System.currentTimeMillis() + ".wav";

            if (callback != null) {
                callback.onSendVoiceRecord(wavBytes, durationStr, fileName);
            }
            recordDialog.dispose();
        });

        btnCancel.addActionListener(e -> {
            isRecording[0] = false;
            timer.stop();
            if (targetLineRef[0] != null) targetLineRef[0].stop();
            recordDialog.dispose();
        });

        btnPanel.add(btnSendAudio);
        btnPanel.add(btnCancel);

        panel.add(lblTitle);
        panel.add(Box.createVerticalStrut(10));
        panel.add(lblTimer);
        panel.add(Box.createVerticalStrut(4));
        panel.add(lblStatus);
        panel.add(Box.createVerticalStrut(18));
        panel.add(btnPanel);

        recordDialog.add(panel);
        recordDialog.setVisible(true);
    }

    private static byte[] convertPcmToWav(byte[] pcmData, AudioFormat format) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(pcmData);
            AudioInputStream ais = new AudioInputStream(bais, format, pcmData.length / format.getFrameSize());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return pcmData;
        }
    }

    private static JButton createStyledButton(String text, Color bgNormal, Color bgHover, Color fgText) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(fgText);
        button.setBackground(bgNormal);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { button.setBackground(bgHover); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { button.setBackground(bgNormal); }
        });
        return button;
    }
}
