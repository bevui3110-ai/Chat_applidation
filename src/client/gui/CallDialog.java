package client.gui;

import common.MessagePacket;
import client.network.ClientSocketHandler;
import client.network.CallMediaSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public class CallDialog {
    private JDialog activeCallDialog;
    private JLabel activeCallStatusLabel;
    private Timer activeCallTimer;
    private Timer activeCallViewportTimer;
    private int activeCallSeconds = 0;
    private int callAnimFrame = 0;

    private boolean isCamOn = true;
    private boolean isMicOn = true;

    private CallMediaSession callMediaSession;
    private String currentUsername;
    private String currentTargetUser;
    private ClientSocketHandler currentSocketHandler;
    private JPanel currentViewport;

    private static final Color COLOR_PRIMARY = new Color(37, 99, 235);

    public synchronized int prepareOutgoingCall(String username, String targetUser, boolean isVideo) {
        cleanupCall();
        this.currentUsername = username;
        this.currentTargetUser = targetUser;
        this.isCamOn = true;
        this.isMicOn = true;

        callMediaSession = new CallMediaSession(isVideo, username, targetUser);
        callMediaSession.setOnFrameUpdateListener(() -> {
            if (currentViewport != null) {
                currentViewport.repaint();
            }
        });

        int port = 0;
        try {
            port = callMediaSession.startListening();
        } catch (Exception e) {
            System.err.println("Failed to start media listening: " + e.getMessage());
        }

        callMediaSession.startMediaLocally();
        return port;
    }

    public void playCallConnectedChime() {
        new Thread(() -> {
            try {
                Toolkit.getDefaultToolkit().beep();
                Thread.sleep(140);
                Toolkit.getDefaultToolkit().beep();
            } catch (Exception ignored) {}
        }).start();
    }

    public void startCallConnectedTimer() {
        if (activeCallStatusLabel != null) {
            activeCallStatusLabel.setText("\uD83D\uDFE2 Đã kết nối  (00:00)");
            activeCallStatusLabel.setForeground(new Color(34, 197, 94));
        }
        if (activeCallTimer != null) {
            activeCallTimer.stop();
        }
        activeCallSeconds = 0;
        activeCallTimer = new Timer(1000, e -> {
            activeCallSeconds++;
            if (activeCallStatusLabel != null) {
                activeCallStatusLabel.setText(String.format("\uD83D\uDFE2 Đã kết nối  (%02d:%02d)", activeCallSeconds / 60, activeCallSeconds % 60));
            }
        });
        activeCallTimer.start();
    }

    public void stopCallTimers() {
        if (activeCallTimer != null) {
            activeCallTimer.stop();
            activeCallTimer = null;
        }
        if (activeCallViewportTimer != null) {
            activeCallViewportTimer.stop();
            activeCallViewportTimer = null;
        }
    }

    public synchronized void cleanupCall() {
        stopCallTimers();

        if (callMediaSession != null) {
            callMediaSession.close();
            callMediaSession = null;
        }

        if (activeCallDialog != null) {
            activeCallDialog.dispose();
            activeCallDialog = null;
        }
        currentViewport = null;
    }

    public void handleCallAccept() {
        handleCallAccept(null);
    }

    public void handleCallAccept(String content) {
        playCallConnectedChime();
        startCallConnectedTimer();

        if (content != null && content.startsWith("ACCEPT:") && callMediaSession != null) {
            String[] parts = content.split(":");
            if (parts.length >= 2) {
                try {
                    int peerPort = Integer.parseInt(parts[1]);
                    String peerIp = (parts.length >= 3 && !parts[2].trim().isEmpty()) ? parts[2].trim() : "127.0.0.1";
                    if ("0:0:0:0:0:0:0:1".equals(peerIp) || "localhost".equalsIgnoreCase(peerIp)) {
                        peerIp = "127.0.0.1";
                    }
                    callMediaSession.connectToPeer(peerIp, peerPort);
                } catch (Exception ignored) {}
            }
        }
    }

    public void handleCallReject(String sender) {
        cleanupCall();
        JOptionPane.showMessageDialog(null, sender + " đã từ chối cuộc gọi.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }

    public void handleCallEnd() {
        cleanupCall();
    }

    public void showCallerWindow(JFrame parent, String username, String targetUser, boolean isVideo, ClientSocketHandler socketHandler) {
        stopCallTimers();
        this.currentUsername = username;
        this.currentTargetUser = targetUser;
        this.currentSocketHandler = socketHandler;
        isCamOn = true;
        isMicOn = true;

        if (callMediaSession == null) {
            callMediaSession = new CallMediaSession(isVideo, username, targetUser);
            callMediaSession.startMediaLocally();
        }

        activeCallDialog = new JDialog(parent, (isVideo ? "Video Call HD - " : "Voice Call HD - ") + targetUser, false);
        activeCallDialog.setSize(480, 560);
        activeCallDialog.setLocationRelativeTo(parent);
        activeCallDialog.setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(15, 23, 42));

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(16, 16, 12, 16));

        JLabel lblCallType = new JLabel(isVideo ? "\uD83D\uDCF9 VIDEO CALL HD (Camera Live)" : "\uD83D\uDCDE VOICE CALL HD (Voice Audio)");
        lblCallType.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCallType.setForeground(COLOR_PRIMARY);
        lblCallType.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblName = new JLabel(targetUser);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblName.setForeground(Color.WHITE);
        lblName.setAlignmentX(Component.CENTER_ALIGNMENT);

        activeCallStatusLabel = new JLabel("Đang đổ chuông ở máy đối phương...");
        activeCallStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        activeCallStatusLabel.setForeground(new Color(148, 163, 184));
        activeCallStatusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // REALISTIC MESSENGER WEBCAM VIDEO VIEWPORT PANEL
        JPanel cameraViewport = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                int w = getWidth();
                int h = getHeight();

                if (isVideo) {
                    if (isCamOn) {
                        BufferedImage remoteImg = (callMediaSession != null) ? callMediaSession.getRemoteVideoFrame() : null;

                        if (remoteImg != null) {
                            // Render real remote video stream
                            Shape oldClip = g2.getClip();
                            g2.clip(new RoundRectangle2D.Float(0, 0, w, h, 16, 16));

                            int rw = remoteImg.getWidth();
                            int rh = remoteImg.getHeight();
                            double scale = Math.max((double) w / rw, (double) h / rh);
                            int dw = (int) (rw * scale);
                            int dh = (int) (rh * scale);
                            int dx = (w - dw) / 2;
                            int dy = (h - dh) / 2;
                            g2.drawImage(remoteImg, dx, dy, dw, dh, null);

                            g2.setClip(oldClip);

                            // Subtle bottom gradient bar for remote participant name
                            g2.setPaint(new GradientPaint(0, h - 36, new Color(0, 0, 0, 0), 0, h, new Color(0, 0, 0, 160)));
                            g2.fillRoundRect(0, h - 36, w, 36, 16, 16);

                            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                            g2.setColor(Color.WHITE);
                            g2.drawString(targetUser + " (Camera)", 16, h - 14);

                        } else {
                            // Dynamic Gradient Camera Feed Background (Connecting / Waiting state)
                            Color c1 = new Color(15, 23, 42);
                            Color c2 = new Color(24, 38, 70);
                            GradientPaint gp = new GradientPaint(0, 0, c1, w, h, c2);
                            g2.setPaint(gp);
                            g2.fillRoundRect(0, 0, w, h, 16, 16);

                            // Central Participant Video Frame Avatar
                            int avatarSize = 94;
                            int ax = (w - avatarSize) / 2;
                            int ay = (h - avatarSize) / 2 - 10;

                            // Pulsing Aura Ring around camera user
                            int pulse = (int) (Math.abs(Math.sin(callAnimFrame * 0.1)) * 12);
                            g2.setColor(new Color(37, 99, 235, 60));
                            g2.fillOval(ax - pulse / 2, ay - pulse / 2, avatarSize + pulse, avatarSize + pulse);

                            g2.setColor(COLOR_PRIMARY);
                            g2.fillOval(ax, ay, avatarSize, avatarSize);

                            g2.setColor(Color.WHITE);
                            g2.setFont(new Font("Segoe UI", Font.BOLD, 36));
                            String initial = targetUser.length() > 0 ? targetUser.substring(0, 1).toUpperCase() : "V";
                            FontMetrics fm = g2.getFontMetrics();
                            g2.drawString(initial, ax + (avatarSize - fm.stringWidth(initial)) / 2, ay + 58);

                            // Camera Stream Label
                            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                            g2.setColor(new Color(147, 197, 253));
                            String camLabel = "CAMERA: " + targetUser + " (1080p HD)";
                            g2.drawString(camLabel, (w - g2.getFontMetrics().stringWidth(camLabel)) / 2, ay + avatarSize + 22);
                        }

                        // Subtle Video Scanline Scan Effect
                        g2.setColor(new Color(59, 130, 246, 25));
                        int scanY = (callAnimFrame * 2) % h;
                        g2.fillRect(0, scanY, w, 3);

                        // Viewfinder Corner Brackets
                        g2.setColor(new Color(59, 130, 246, 180));
                        g2.setStroke(new BasicStroke(2.0f));
                        g2.drawLine(15, 15, 35, 15);
                        g2.drawLine(15, 15, 15, 35);
                        g2.drawLine(w - 35, 15, w - 15, 15);
                        g2.drawLine(w - 15, 15, w - 15, 35);
                        g2.drawLine(15, h - 35, 15, h - 15);
                        g2.drawLine(15, h - 15, 35, h - 15);
                        g2.drawLine(w - 35, h - 15, w - 15, h - 15);
                        g2.drawLine(w - 15, h - 35, w - 15, h - 15);

                        // LIVE STREAM BADGE AT TOP LEFT
                        g2.setColor(new Color(225, 29, 72));
                        g2.fillRoundRect(12, 12, 85, 22, 6, 6);
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                        g2.drawString("\u25CF LIVE 1080p", 16, 27);

                        // Muted Mic Overlay Badge (if mic is off)
                        if (!isMicOn) {
                            g2.setColor(new Color(239, 68, 68));
                            g2.fillRoundRect(w / 2 - 50, 12, 100, 22, 6, 6);
                            g2.setColor(Color.WHITE);
                            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                            g2.drawString("TẮT MICRO", w / 2 - 32, 27);
                        }

                        // PIP LOCAL VIDEO CAMERA PREVIEW BOX (Bottom Right, 105x70)
                        int pipW = 105, pipH = 70;
                        int pipX = w - pipW - 12;
                        int pipY = h - pipH - 12;

                        BufferedImage localImg = (callMediaSession != null) ? callMediaSession.getLocalVideoFrame() : null;

                        if (localImg != null) {
                            // Render real local webcam feed into PIP
                            Shape oldClip = g2.getClip();
                            RoundRectangle2D pipClip = new RoundRectangle2D.Float(pipX, pipY, pipW, pipH, 8, 8);
                            g2.clip(pipClip);

                            int lw = localImg.getWidth();
                            int lh = localImg.getHeight();
                            double lScale = Math.max((double) pipW / lw, (double) pipH / lh);
                            int ldw = (int) (lw * lScale);
                            int ldh = (int) (lh * lScale);
                            int ldx = pipX + (pipW - ldw) / 2;
                            int ldy = pipY + (pipH - ldh) / 2;

                            // Mirror horizontally so local camera feels like a natural mirror
                            g2.drawImage(localImg, ldx + ldw, ldy, -ldw, ldh, null);

                            // Dark overlay at top of PIP for label contrast
                            g2.setPaint(new GradientPaint(pipX, pipY, new Color(0, 0, 0, 170), pipX, pipY + 22, new Color(0, 0, 0, 0)));
                            g2.fillRect(pipX, pipY, pipW, 22);

                            g2.setClip(oldClip);

                            // Border
                            g2.setColor(new Color(59, 130, 246));
                            g2.setStroke(new BasicStroke(1.5f));
                            g2.drawRoundRect(pipX, pipY, pipW - 1, pipH - 1, 8, 8);

                            // Status dot and "BẠN (Cam)" label
                            g2.setColor(new Color(34, 197, 94));
                            g2.fillOval(pipX + 8, pipY + 8, 7, 7);

                            g2.setColor(Color.WHITE);
                            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                            g2.drawString("BẠN (Cam)", pipX + 20, pipY + 15);

                        } else {
                            // Stylized PIP box placeholder
                            g2.setColor(new Color(30, 41, 59));
                            g2.fillRoundRect(pipX, pipY, pipW, pipH, 8, 8);
                            g2.setColor(new Color(59, 130, 246));
                            g2.drawRoundRect(pipX, pipY, pipW - 1, pipH - 1, 8, 8);

                            // Local Camera user initial
                            g2.setColor(new Color(34, 197, 94));
                            g2.fillOval(pipX + 8, pipY + 8, 8, 8);

                            g2.setColor(Color.WHITE);
                            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                            g2.drawString("BẠN (Cam)", pipX + 22, pipY + 16);

                            g2.setColor(new Color(37, 99, 235));
                            g2.fillOval(pipX + 12, pipY + 28, 30, 30);
                            g2.setColor(Color.WHITE);
                            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                            String selfInit = username.length() > 0 ? username.substring(0, 1).toUpperCase() : "U";
                            g2.drawString(selfInit, pipX + 22, pipY + 49);

                            g2.setColor(new Color(148, 163, 184));
                            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                            g2.drawString("720p HD", pipX + 50, pipY + 46);
                        }

                    } else {
                        // CAMERA OFF STATE
                        g2.setColor(new Color(15, 23, 42));
                        g2.fillRoundRect(0, 0, w, h, 16, 16);

                        g2.setColor(new Color(71, 85, 105));
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                        String offMsg = "\uD83D\uDCF7 CAMERA ĐÃ TẮT";
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(offMsg, (w - fm.stringWidth(offMsg)) / 2, h / 2 - 10);

                        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                        g2.setColor(new Color(148, 163, 184));
                        String subMsg = "Bấm nút Camera để bật lại video stream";
                        g2.drawString(subMsg, (w - g2.getFontMetrics().stringWidth(subMsg)) / 2, h / 2 + 16);
                    }

                } else {
                    // VOICE CALL REAL-TIME VISUALIZER
                    g2.setColor(new Color(15, 23, 42));
                    g2.fillRoundRect(0, 0, w, h, 16, 16);

                    double voiceRms = (callMediaSession != null && isMicOn) ? callMediaSession.getSmoothedRms() : 0.0;
                    int voicePulse = (int) (voiceRms * 28.0);
                    int pulse = ((callAnimFrame * 2) % 30) + voicePulse;

                    g2.setColor(new Color(37, 99, 235, Math.max(10, 60 - pulse)));
                    g2.fillOval((w - 140 - pulse) / 2, (h - 140 - pulse) / 2, 140 + pulse, 140 + pulse);

                    // 16 Voice Amplitude Bars driven by real microphone RMS
                    int numBars = 16;
                    int barW = 8;
                    int gap = 6;
                    int totalW = numBars * barW + (numBars - 1) * gap;
                    int startX = (w - totalW) / 2;
                    int baseY = h - 25;

                    g2.setColor(new Color(59, 130, 246, 200));
                    for (int i = 0; i < numBars; i++) {
                        double bell = Math.sin((i + 1) * Math.PI / (numBars + 1));
                        int dynamicH = (int) (voiceRms * 38.0 * (0.35 + 0.65 * bell) + Math.abs(Math.sin((callAnimFrame * 0.15) + i)) * 3 * voiceRms);
                        int barH = Math.max(6, Math.min(48, 6 + dynamicH));
                        g2.fillRoundRect(startX + i * (barW + gap), baseY - barH, barW, barH, 4, 4);
                    }

                    int avatarSize = 90;
                    int ax = (w - avatarSize) / 2;
                    int ay = (h - avatarSize) / 2 - 20;

                    g2.setColor(COLOR_PRIMARY);
                    g2.fillOval(ax, ay, avatarSize, avatarSize);

                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 36));
                    String initial = targetUser.length() > 0 ? targetUser.substring(0, 1).toUpperCase() : "C";
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(initial, ax + (avatarSize - fm.stringWidth(initial)) / 2, ay + 56);

                    // Mic muted indicator in voice mode
                    if (!isMicOn) {
                        g2.setColor(new Color(239, 68, 68));
                        g2.fillRoundRect(w / 2 - 50, 14, 100, 22, 6, 6);
                        g2.setColor(Color.WHITE);
                        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                        g2.drawString("TẮT MICRO", w / 2 - 32, 29);
                    }

                    g2.setColor(new Color(34, 197, 94));
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    String voiceLabel = "HD Audio (16kHz Mono Live)";
                    g2.drawString(voiceLabel, (w - g2.getFontMetrics().stringWidth(voiceLabel)) / 2, ay + avatarSize + 20);
                }

                g2.dispose();
            }
        };
        this.currentViewport = cameraViewport;
        callMediaSession.setOnFrameUpdateListener(() -> cameraViewport.repaint());

        cameraViewport.setPreferredSize(new Dimension(380, isVideo ? 270 : 210));
        cameraViewport.setMaximumSize(new Dimension(380, isVideo ? 270 : 210));
        cameraViewport.setOpaque(false);
        cameraViewport.setAlignmentX(Component.CENTER_ALIGNMENT);

        activeCallViewportTimer = new Timer(50, e -> {
            callAnimFrame++;
            cameraViewport.repaint();
        });
        activeCallViewportTimer.start();

        contentPanel.add(lblCallType);
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(lblName);
        contentPanel.add(Box.createVerticalStrut(4));
        contentPanel.add(activeCallStatusLabel);
        contentPanel.add(Box.createVerticalStrut(14));
        contentPanel.add(cameraViewport);

        // Bottom Controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        controls.setOpaque(false);

        JButton btnMic = createCallControlButton("Micro: ON", "Tắt/Mở Micro", new Color(51, 65, 85));
        JButton btnCam = createCallControlButton(isVideo ? "Camera: ON" : "Loa ngoài", "Tắt/Mở Camera/Loa", new Color(51, 65, 85));
        JButton btnEndCall = createCallControlButton("Kết thúc", "Kết thúc cuộc gọi", new Color(225, 29, 72));

        btnMic.addActionListener(e -> {
            isMicOn = !isMicOn;
            btnMic.setText(isMicOn ? "Micro: ON" : "Micro: OFF");
            btnMic.setBackground(isMicOn ? new Color(51, 65, 85) : new Color(225, 29, 72));
            if (callMediaSession != null) {
                callMediaSession.setMicOn(isMicOn);
            }
            cameraViewport.repaint();
        });

        btnCam.addActionListener(e -> {
            if (isVideo) {
                isCamOn = !isCamOn;
                btnCam.setText(isCamOn ? "Camera: ON" : "Camera: OFF");
                btnCam.setBackground(isCamOn ? new Color(51, 65, 85) : new Color(225, 29, 72));
                if (callMediaSession != null) {
                    callMediaSession.setCamOn(isCamOn);
                }
            } else {
                boolean isOn = btnCam.getText().contains("Loa ngoài");
                btnCam.setText(isOn ? "Mute Loa" : "Loa ngoài");
                btnCam.setBackground(isOn ? new Color(225, 29, 72) : new Color(51, 65, 85));
                if (callMediaSession != null) {
                    callMediaSession.setSpeakerOn(!isOn);
                }
            }
            cameraViewport.repaint();
        });

        btnEndCall.addActionListener(e -> {
            cleanupCall();
            try {
                socketHandler.sendPacket(new MessagePacket("CALL_END", username, targetUser, "END"));
            } catch (Exception ignored) {}
        });

        activeCallDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanupCall();
                try {
                    socketHandler.sendPacket(new MessagePacket("CALL_END", username, targetUser, "END"));
                } catch (Exception ignored) {}
            }
        });

        controls.add(btnMic);
        controls.add(btnCam);
        controls.add(btnEndCall);

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(controls, BorderLayout.SOUTH);

        Toolkit.getDefaultToolkit().beep();

        activeCallDialog.add(mainPanel);
        activeCallDialog.setVisible(true);
    }

    public void handleIncomingCallRequest(JFrame parent, String username, String caller, String content, ClientSocketHandler socketHandler) {
        SwingUtilities.invokeLater(() -> {
            boolean isVideo = content != null && content.startsWith("VIDEO");
            String title = (isVideo ? "Cuộc gọi Video" : "Cuộc gọi thoại") + " từ " + caller;

            // Parse caller's IP & port from content e.g. "VIDEO:51234:127.0.0.1"
            int callerPort = 0;
            String callerIp = "127.0.0.1";
            if (content != null && content.contains(":")) {
                String[] parts = content.split(":");
                if (parts.length >= 2) {
                    try { callerPort = Integer.parseInt(parts[1]); } catch (Exception ignored) {}
                }
                if (parts.length >= 3 && !parts[2].trim().isEmpty()) {
                    callerIp = parts[2].trim();
                }
            }
            if ("0:0:0:0:0:0:0:1".equals(callerIp) || "localhost".equalsIgnoreCase(callerIp)) {
                callerIp = "127.0.0.1";
            }
            final int fCallerPort = callerPort;
            final String fCallerIp = callerIp;

            Toolkit.getDefaultToolkit().beep();

            JDialog incomingDialog = new JDialog(parent, title, true);
            incomingDialog.setSize(380, 270);
            incomingDialog.setLocationRelativeTo(parent);
            incomingDialog.setResizable(false);

            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBackground(new Color(15, 23, 42));
            panel.setBorder(new EmptyBorder(25, 20, 20, 20));

            JLabel lblHeading = new JLabel(isVideo ? "\uD83D\uDCF9 CUỘC GỌI VIDEO ĐẾN" : "\uD83D\uDCDE CUỘC GỌI THOẠI ĐẾN");
            lblHeading.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblHeading.setForeground(COLOR_PRIMARY);
            lblHeading.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel lblCallerName = new JLabel(caller);
            lblCallerName.setFont(new Font("Segoe UI", Font.BOLD, 24));
            lblCallerName.setForeground(Color.WHITE);
            lblCallerName.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel lblInfo = new JLabel("đang gọi cho bạn...");
            lblInfo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lblInfo.setForeground(new Color(148, 163, 184));
            lblInfo.setAlignmentX(Component.CENTER_ALIGNMENT);

            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
            btnPanel.setOpaque(false);

            JButton btnAccept = createStyledButton("Chấp nhận", new Color(16, 185, 129), new Color(5, 150, 105), Color.WHITE);
            btnAccept.setPreferredSize(new Dimension(135, 42));

            JButton btnReject = createStyledButton("Từ chối", new Color(225, 29, 72), new Color(190, 18, 60), Color.WHITE);
            btnReject.setPreferredSize(new Dimension(115, 42));

            btnAccept.addActionListener(e -> {
                incomingDialog.dispose();

                // Start local media session
                cleanupCall();
                callMediaSession = new CallMediaSession(isVideo, username, caller);
                callMediaSession.startMediaLocally(); // Kích hoạt camera và micro cho người nghe

                int calleePort = 0;
                try {
                    calleePort = callMediaSession.startListening();
                } catch (Exception ex) {
                    System.err.println("Failed to start callee media listener: " + ex.getMessage());
                }

                String myIp = "127.0.0.1";
                try {
                    if (socketHandler != null && socketHandler.getSocket() != null && socketHandler.getSocket().getLocalAddress() != null) {
                        myIp = socketHandler.getSocket().getLocalAddress().getHostAddress();
                        if ("0:0:0:0:0:0:0:1".equals(myIp)) {
                            myIp = "127.0.0.1";
                        }
                    }
                } catch (Exception ignored) {}

                try {
                    socketHandler.sendPacket(new MessagePacket("CALL_ACCEPT", username, caller, 
                        "ACCEPT:" + calleePort + ":" + myIp));
                } catch (Exception ignored) {}

                if (fCallerPort > 0) {
                    callMediaSession.connectToPeer(fCallerIp, fCallerPort);
                }

                playCallConnectedChime();
                showCallerWindow(parent, username, caller, isVideo, socketHandler);
                startCallConnectedTimer();
            });

            btnReject.addActionListener(e -> {
                incomingDialog.dispose();
                try {
                    socketHandler.sendPacket(new MessagePacket("CALL_REJECT", username, caller, "REJECT"));
                } catch (Exception ignored) {}
            });

            btnPanel.add(btnAccept);
            btnPanel.add(btnReject);

            panel.add(lblHeading);
            panel.add(Box.createVerticalStrut(6));
            panel.add(lblCallerName);
            panel.add(Box.createVerticalStrut(4));
            panel.add(lblInfo);
            panel.add(Box.createVerticalStrut(25));
            panel.add(btnPanel);

            incomingDialog.add(panel);
            incomingDialog.setVisible(true);
        });
    }

    private JButton createCallControlButton(String text, String tooltip, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setToolTipText(tooltip);
        btn.setMargin(new Insets(2, 4, 2, 4));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(135, 42));
        return btn;
    }

    private JButton createStyledButton(String text, Color bgNormal, Color bgHover, Color fgText) {
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
