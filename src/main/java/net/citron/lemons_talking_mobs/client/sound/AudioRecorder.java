/*
package net.citron.lemons_talking_mobs.client.sound;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class AudioRecorder {

    // Standardní zvukový formát: 44.1 kHz, 16-bit, mono, signed, little-endian
    private static final AudioFormat FORMAT = new AudioFormat(44100.0f, 16, 1, true, false);
    private static TargetDataLine targetLine;
    private static ByteArrayOutputStream recordStream;
    private static Thread recordingThread;
    private static volatile boolean isRecording = false;

    public static void startRecording() {
        if (isRecording) return;
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("Mikrofon s pozadovanym formatem neni podporovan.");
                return;
            }

            targetLine = (TargetDataLine) AudioSystem.getLine(info);
            targetLine.open(FORMAT);
            targetLine.start();
            isRecording = true;
            recordStream = new ByteArrayOutputStream();

            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[1024];
                while (isRecording) {
                    int bytesRead = targetLine.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        recordStream.write(buffer, 0, bytesRead);
                    }
                }
            }, "MobSound-Recorder-Thread");
            recordingThread.start();

        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public static byte[] stopRecording() {
        if (!isRecording) return new byte[0];
        isRecording = false;

        try {
            if (targetLine != null) {
                targetLine.stop();
                targetLine.close();
            }
            if (recordingThread != null) {
                recordingThread.join(500);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return recordStream != null ? recordStream.toByteArray() : new byte[0];
    }

    public static void playPreview(byte[] pcmData) {
        if (pcmData == null || pcmData.length == 0) return;

        new Thread(() -> {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(pcmData);
                AudioInputStream audioStream = new AudioInputStream(bais, FORMAT, pcmData.length / FORMAT.getFrameSize());
                DataLine.Info info = new DataLine.Info(Clip.class, FORMAT);
                Clip clip = (Clip) AudioSystem.getLine(info);
                clip.open(audioStream);
                clip.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "MobSound-Preview-Thread").start();
    }
}

 */
