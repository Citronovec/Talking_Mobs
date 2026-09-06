package net.citron.lemons_talking_mobs.client;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;

public class MicLevelTracker {

    private static TargetDataLine line;
    private static Thread listenerThread;
    private static volatile float currentLevel = 0.0F;
    private static volatile boolean running = false;

    // Buffer for recording
    private static volatile boolean recording = false;
    private static final ByteArrayOutputStream recordBuffer = new ByteArrayOutputStream();

    public static synchronized void start() {
        if (running) return;

        try {
            AudioFormat format = new AudioFormat(48000.0F, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                return;
            }

            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format, 4096);
            line.start();
            running = true;

            listenerThread = new Thread(() -> {
                byte[] buffer = new byte[2048];
                while (running) {
                    int count = line.read(buffer, 0, buffer.length);
                    if (count > 0) {

                        if (recording) {
                            synchronized (recordBuffer) {
                                recordBuffer.write(buffer, 0, count);
                            }
                        }

                        // counter for VU meter
                        double sum = 0.0;
                        int samples = count / 2;
                        for (int i = 0; i < count - 1; i += 2) {
                            short sample = (short) ((buffer[i] & 0xFF) | (buffer[i + 1] << 8));
                            sum += sample * sample;
                        }
                        double rms = Math.sqrt(sum / samples);
                        float normalized = (float) (rms / 6000.0);

                        currentLevel = (currentLevel * 0.3F) + (Math.min(1.0F, normalized) * 0.7F);
                    }
                }
            }, "TalkingMobs-MicLevelThread");
            listenerThread.setDaemon(true);
            listenerThread.start();

        } catch (Exception ignored) {
            running = false;
        }
    }

    public static void startRecording() {
        synchronized (recordBuffer) {
            recordBuffer.reset();
            recording = true;
        }
    }

    public static byte[] stopRecording() {
        synchronized (recordBuffer) {
            recording = false;
            return recordBuffer.toByteArray();
        }
    }

    public static synchronized void stop() {
        running = false;
        recording = false;
        if (line != null) {
            line.stop();
            line.close();
            line = null;
        }
        listenerThread = null;
        currentLevel = 0.0F;
    }

    public static float getCurrentLevel() {
        return currentLevel;
    }
}