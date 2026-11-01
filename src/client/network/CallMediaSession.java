package client.network;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;

public class CallMediaSession {

    public static final byte PACKET_TYPE_AUDIO = 1;
    public static final byte PACKET_TYPE_VIDEO = 2;
    public static final byte PACKET_TYPE_CAM_STATE = 3;
    public static final byte PACKET_TYPE_MIC_STATE = 4;

    private final boolean isVideo;
    private final String selfUsername;
    private final String peerUsername;

    private ServerSocket serverSocket;
    private Socket mediaSocket;
    private DataOutputStream dos;
    private DataInputStream dis;

    private final Object writeLock = new Object();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean videoStarted = new AtomicBoolean(false);

    // Audio
    private TargetDataLine targetLine;
    private SourceDataLine sourceLine;
    private volatile boolean isMicOn = true;
    private volatile boolean isSpeakerOn = true;
    private volatile double currentRms = 0.0;
    private volatile double smoothedRms = 0.0;

    // Video
    private Webcam webcam;
    private volatile boolean isCamOn = true;
    private volatile BufferedImage localVideoFrame;
    private volatile BufferedImage remoteVideoFrame;

    private Runnable onFrameUpdateListener;

    public CallMediaSession(boolean isVideo, String selfUsername, String peerUsername) {
        this.isVideo = isVideo;
        this.selfUsername = selfUsername;
        this.peerUsername = peerUsername;
    }

    public void setOnFrameUpdateListener(Runnable listener) {
        this.onFrameUpdateListener = listener;
    }

    public int startListening() throws IOException {
        running.set(true);
        serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();

        Thread acceptThread = new Thread(() -> {
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    Socket client = serverSocket.accept();
                    setupConnectedSocket(client);
                }
            } catch (IOException ignored) {
            }
        }, "CallMedia-Accept-" + selfUsername);
        acceptThread.setDaemon(true);
        acceptThread.start();

        return port;
    }

    public void connectToPeer(String host, int port) {
        if (connected.get() || !running.get()) {
            return;
        }

        final String rawHost = (host != null) ? host.trim() : "";
        final String targetHost = ("0:0:0:0:0:0:0:1".equals(rawHost) || "localhost".equalsIgnoreCase(rawHost) || rawHost.isEmpty()) ? "127.0.0.1" : rawHost;
        Thread connectThread = new Thread(() -> {
            try {
                Socket socket = new Socket(targetHost, port);
                setupConnectedSocket(socket);
            } catch (Exception e) {
                System.err.println("Direct connect to peer (" + targetHost + ":" + port + ") failed: " + e.getMessage());
            }
        }, "CallMedia-Connect-" + selfUsername);
        connectThread.setDaemon(true);
        connectThread.start();
    }

    private synchronized void setupConnectedSocket(Socket socket) {
        if (connected.get()) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            return;
        }

        try {
            this.mediaSocket = socket;
            this.mediaSocket.setTcpNoDelay(true);
            this.dos = new DataOutputStream(new BufferedOutputStream(mediaSocket.getOutputStream(), 65536));
            this.dis = new DataInputStream(new BufferedInputStream(mediaSocket.getInputStream(), 65536));
            this.connected.set(true);

            // Start media hardware & transport threads
            startAudioSubsystem();
            if (isVideo) {
                startVideoSubsystem();
            }
            startReceiverThread();

            // Send initial camera & mic states
            sendCamState(isCamOn);
            sendMicState(isMicOn);

        } catch (IOException e) {
            System.err.println("Setup connected socket failed: " + e.getMessage());
            close();
        }
    }

    public void startMediaLocally() {
        if (!running.get()) {
            running.set(true);
        }
        startAudioSubsystem();
        if (isVideo) {
            startVideoSubsystem();
        }
    }

    private void startAudioSubsystem() {
        if (targetLine != null || sourceLine != null) {
            return;
        }

        Thread audioInitThread = new Thread(() -> {
            try {
                float[] sampleRates = {16000.0f, 8000.0f, 44100.0f};
                AudioFormat chosenFormat = new AudioFormat(16000.0f, 16, 1, true, false);

                // Setup Microphone
                for (float rate : sampleRates) {
                    AudioFormat fmt = new AudioFormat(rate, 16, 1, true, false);
                    DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, fmt);
                    if (AudioSystem.isLineSupported(targetInfo)) {
                        try {
                            targetLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
                            targetLine.open(fmt);
                            targetLine.start();
                            chosenFormat = fmt;
                            break;
                        } catch (Throwable ignored) {
                        }
                    }
                }

                if (targetLine != null) {
                    Thread micThread = new Thread(this::recordAudioLoop, "CallMedia-AudioRec-" + selfUsername);
                    micThread.setDaemon(true);
                    micThread.start();
                }

                // Setup Speaker
                DataLine.Info sourceInfo = new DataLine.Info(SourceDataLine.class, chosenFormat);
                if (AudioSystem.isLineSupported(sourceInfo)) {
                    try {
                        sourceLine = (SourceDataLine) AudioSystem.getLine(sourceInfo);
                        sourceLine.open(chosenFormat);
                        sourceLine.start();
                    } catch (Throwable ignored) {
                    }
                }

            } catch (Throwable t) {
                System.err.println("Audio subsystem init warning: " + t.getMessage());
            }
        }, "CallMedia-AudioInit-" + selfUsername);
        audioInitThread.setDaemon(true);
        audioInitThread.start();
    }

    private void recordAudioLoop() {
        byte[] buffer = new byte[1280]; // 40ms of 16kHz 16-bit mono
        while (running.get() && targetLine != null && targetLine.isOpen()) {
            try {
                int bytesRead = targetLine.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    if (isMicOn) {
                        double rms = calculateRMS(buffer, bytesRead);
                        this.currentRms = rms;
                        double norm = Math.min(1.0, rms / 5000.0);
                        this.smoothedRms = (this.smoothedRms * 0.6) + (norm * 0.4);

                        if (connected.get()) {
                            sendPacket(PACKET_TYPE_AUDIO, buffer, 0, bytesRead);
                        }
                    } else {
                        this.currentRms = 0.0;
                        this.smoothedRms = 0.0;
                    }
                }
            } catch (Exception e) {
                break;
            }
        }
    }

    private double calculateRMS(byte[] audioData, int length) {
        long sum = 0;
        int sampleCount = length / 2;
        for (int i = 0; i < length - 1; i += 2) {
            short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
            sum += (long) sample * sample;
        }
        double mean = (double) sum / Math.max(1, sampleCount);
        return Math.sqrt(mean);
    }

    private void startVideoSubsystem() {
        if (webcam != null || !videoStarted.compareAndSet(false, true)) {
            return;
        }
        Thread videoInitThread = new Thread(() -> {
            try {
                // Ensure OpenIMAJGrabber.dll is available in project root for BridJ / Eclipse
                File dllFile = new File("OpenIMAJGrabber.dll");
                if (!dllFile.exists()) {
                    try (InputStream is = getClass().getResourceAsStream("/com/github/sarxos/webcam/ds/buildin/lib/win64/OpenIMAJGrabber.dll")) {
                        if (is != null) {
                            try (FileOutputStream fos = new FileOutputStream(dllFile)) {
                                byte[] buf = new byte[8192];
                                int r;
                                while ((r = is.read(buf)) != -1) {
                                    fos.write(buf, 0, r);
                                }
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("Could not auto-extract OpenIMAJGrabber.dll: " + ex.getMessage());
                    }
                }

                // Tắt cơ chế file-lock của Sarxos
                try {
                    java.lang.reflect.Method m = Webcam.class.getMethod("getWebcamLock");
                    Object lock = m.invoke(null);
                    if (lock != null) {
                        lock.getClass().getMethod("disable").invoke(lock);
                    }
                } catch (Throwable ignored) {
                }

                Webcam defaultCam = Webcam.getDefault();
                if (defaultCam != null) {
                    this.webcam = defaultCam;

                    try {
                        defaultCam.getLock().disable();
                    } catch (Throwable ignored) {
                    }

                    // 1. Đảm bảo camera được đóng trước khi thay đổi độ phân giải
                    try {
                        if (defaultCam.isOpen()) {
                            defaultCam.close();
                        }
                    } catch (Throwable ignored) {
                    }

                    // 2. Thử đặt độ phân giải an toàn (nếu không hỗ trợ thì giữ nguyên mặc định)
                    try {
                        Dimension qvga = WebcamResolution.QVGA.getSize();
                        for (Dimension d : defaultCam.getViewSizes()) {
                            if (d.width == qvga.width && d.height == qvga.height) {
                                defaultCam.setViewSize(qvga);
                                break;
                            }
                        }
                    } catch (Throwable ignored) {
                    }

                    // 3. Mở camera an toàn có bọc Fallback
                    try {
                        if (!defaultCam.isOpen()) {
                            defaultCam.open(true);
                        }
                        // Bắt đầu luồng ghi hình khi mở thành công
                        Thread captureThread = new Thread(this::captureVideoLoop, "CallMedia-VideoCap-" + selfUsername);
                        captureThread.setDaemon(true);
                        captureThread.start();
                    } catch (Throwable t) {
                        // Bắt trường hợp camera đang bận (test 2 client cùng 1 laptop):
                        // Không crash chương trình, chuyển sang chế độ chờ để cuộc gọi vẫn tiếp tục
                        System.out.println("Camera dang ban hoac dang duoc client khac su dung: " + t.getMessage());
                    }
                } else {
                    System.err.println("No webcam device detected on system.");
                }
            } catch (Throwable t) {
                System.out.println("Camera dang ban hoac dang duoc client khac su dung: " + t.getMessage());
            }
        }, "CallMedia-VideoInit-" + selfUsername);
        videoInitThread.setDaemon(true);
        videoInitThread.start();
    }

    private void captureVideoLoop() {
        while (running.get()) {
            try {
                if (isCamOn && webcam != null && webcam.isOpen()) {
                    BufferedImage img = webcam.getImage();
                    if (img != null) {
                        this.localVideoFrame = img;
                        if (onFrameUpdateListener != null) {
                            onFrameUpdateListener.run();
                        }

                        if (connected.get()) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ImageIO.write(img, "jpg", baos);
                            byte[] jpegData = baos.toByteArray();
                            sendPacket(PACKET_TYPE_VIDEO, jpegData, 0, jpegData.length);
                        }
                    }
                } else {
                    this.localVideoFrame = null;
                }
                Thread.sleep(40); // ~25 FPS
            } catch (InterruptedException ie) {
                break;
            } catch (Exception e) {
                // Ignore transient frame errors
            }
        }
    }

    private void startReceiverThread() {
        Thread receiver = new Thread(() -> {
            try {
                while (running.get() && connected.get() && dis != null) {
                    byte type = dis.readByte();
                    int len = dis.readInt();
                    if (len < 0 || len > 2000000) { // Safety sanity limit 2MB
                        break;
                    }
                    byte[] data = new byte[len];
                    dis.readFully(data);

                    handleReceivedPacket(type, data);
                }
            } catch (IOException e) {
                // Connection closed or broken
            } finally {
                connected.set(false);
            }
        }, "CallMedia-Receiver-" + selfUsername);
        receiver.setDaemon(true);
        receiver.start();
    }

    private void handleReceivedPacket(byte type, byte[] data) {
        switch (type) {
            case PACKET_TYPE_AUDIO:
                if (isSpeakerOn && sourceLine != null && sourceLine.isOpen()) {
                    sourceLine.write(data, 0, data.length);
                }
                break;

            case PACKET_TYPE_VIDEO:
                try {
                    ByteArrayInputStream bais = new ByteArrayInputStream(data);
                    BufferedImage frame = ImageIO.read(bais);
                    if (frame != null) {
                        this.remoteVideoFrame = frame;
                        if (onFrameUpdateListener != null) {
                            onFrameUpdateListener.run();
                        }
                    }
                } catch (Exception ignored) {
                }
                break;

            case PACKET_TYPE_CAM_STATE:
                boolean peerCam = data.length > 0 && data[0] == 1;
                if (!peerCam) {
                    this.remoteVideoFrame = null;
                }
                if (onFrameUpdateListener != null) {
                    onFrameUpdateListener.run();
                }
                break;

            case PACKET_TYPE_MIC_STATE:
                // Peer mic state if needed
                break;
        }
    }

    private void sendPacket(byte type, byte[] data, int offset, int length) {
        if (!connected.get() || dos == null) {
            return;
        }
        synchronized (writeLock) {
            try {
                dos.writeByte(type);
                dos.writeInt(length);
                dos.write(data, offset, length);
                dos.flush();
            } catch (IOException e) {
                connected.set(false);
            }
        }
    }

    public void setMicOn(boolean on) {
        this.isMicOn = on;
        if (!on) {
            this.currentRms = 0.0;
            this.smoothedRms = 0.0;
        }
        sendMicState(on);
    }

    public boolean isMicOn() {
        return isMicOn;
    }

    public void setSpeakerOn(boolean on) {
        this.isSpeakerOn = on;
    }

    public boolean isSpeakerOn() {
        return isSpeakerOn;
    }

    public void setCamOn(boolean on) {
        this.isCamOn = on;
        if (!on) {
            this.localVideoFrame = null;
            if (webcam != null) {
                try {
                    if (webcam.isOpen()) {
                        webcam.close();
                    }
                } catch (Throwable ignored) {
                }
            }
        } else {
            if (webcam != null) {
                new Thread(() -> {
                    try {
                        if (!webcam.isOpen()) {
                            webcam.open(true);
                        }
                    } catch (Throwable t) {
                        System.out.println("Camera dang ban hoac dang duoc client khac su dung: " + t.getMessage());
                    }
                }, "CallMedia-WebcamReopen-" + selfUsername).start();
            }
        }
        sendCamState(on);
    }

    public boolean isCamOn() {
        return isCamOn;
    }

    private void sendCamState(boolean on) {
        if (connected.get()) {
            sendPacket(PACKET_TYPE_CAM_STATE, new byte[]{(byte) (on ? 1 : 0)}, 0, 1);
        }
    }

    private void sendMicState(boolean on) {
        if (connected.get()) {
            sendPacket(PACKET_TYPE_MIC_STATE, new byte[]{(byte) (on ? 1 : 0)}, 0, 1);
        }
    }

    public BufferedImage getLocalVideoFrame() {
        return localVideoFrame;
    }

    public BufferedImage getRemoteVideoFrame() {
        return remoteVideoFrame;
    }

    public double getSmoothedRms() {
        return isMicOn ? smoothedRms : 0.0;
    }

    public double getCurrentRms() {
        return isMicOn ? currentRms : 0.0;
    }

    public boolean isConnected() {
        return connected.get();
    }

    public synchronized void close() {
        running.set(false);
        connected.set(false);

        // 1. Close webcam
        videoStarted.set(false);
        if (webcam != null) {
            try {
                if (webcam.isOpen()) {
                    webcam.close();
                }
            } catch (Throwable ignored) {
            }
            webcam = null;
        }
        localVideoFrame = null;
        remoteVideoFrame = null;

        // 2. Close audio lines
        if (targetLine != null) {
            try {
                targetLine.stop();
                targetLine.close();
            } catch (Throwable ignored) {
            }
            targetLine = null;
        }
        if (sourceLine != null) {
            try {
                sourceLine.stop();
                sourceLine.close();
            } catch (Throwable ignored) {
            }
            sourceLine = null;
        }

        // 3. Close network sockets
        try {
            if (dos != null) {
                dos.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (dis != null) {
                dis.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (mediaSocket != null && !mediaSocket.isClosed()) {
                mediaSocket.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
    }
}
