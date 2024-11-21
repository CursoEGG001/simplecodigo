/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.BorderLayout;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 *
 * @author pc
 */
public class MicLevelMeter extends JFrame {

    private JProgressBar levelMeter;
    private TargetDataLine micLine;

    public MicLevelMeter() {
        setTitle("VUmetro del Mic");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(124, 400);
        levelMeter = new JProgressBar(0, 100);
        levelMeter.setOrientation(SwingConstants.VERTICAL);
        levelMeter.setStringPainted(true);
        levelMeter.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createRaisedBevelBorder(),
                        BorderFactory.createTitledBorder("Mic Level VUmeter")
                )
        );
        add(levelMeter, BorderLayout.CENTER);
        setVisible(true);

        // Start microphone capture and level meter in a new thread
        new Thread(this::captureMicrophoneLevel).start();
    }

    private void captureMicrophoneLevel() {
        try {
            // Set audio format for microphone capture
            AudioFormat format = new AudioFormat(44100.0f, 16, 1, true, true); // Mono, 16-bit, 44.1kHz
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            // Check if the system supports the microphone input
            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("Sin Micrófono");
                return;
            }

            // Open and start the microphone input line
            micLine = (TargetDataLine) AudioSystem.getLine(info);
            micLine.open(format);
            micLine.start();

            byte[] buffer = new byte[4096]; // Buffer for capturing audio data
            int bytesRead;

            while (true) {
                // System.out.println("Nivel: " + (micLine.getLevel() < 0 ? "No se especifíca" : micLine.getLevel()));
                // Read data from the microphone into the buffer
                bytesRead = micLine.read(buffer, 0, buffer.length);

                if (bytesRead > 0) {
                    // Calculate the RMS level of the audio data (Root Mean Square)
                    double rms = calculateRMSLevel(buffer, bytesRead);

                    // Convert RMS to a percentage scale for the level meter (0-100)
                    int level = (int) (rms * 100);
                    SwingUtilities.invokeLater(() -> levelMeter.setValue(level));
                }
            }
        } catch (LineUnavailableException e) {
            System.out.println("Inhabilitada: " + e.getMessage());
        }
    }

    // Method to calculate RMS level (volume) from the audio buffer
    private double calculateRMSLevel(byte[] audioData, int bytesRead) {
        long sum = 0;
        for (int i = 0; i < bytesRead; i += 2) {
            // Convert two bytes to a 16-bit value
            int sample = (audioData[i + 1] << 8) | (audioData[i] & 0xFF);
            sum += sample * sample;
        }
        double mean = (double) sum / (bytesRead / 2);
        return Math.sqrt(mean) / 32768.0; // Normalize to range 0-1
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MicLevelMeter::new);
    }
}
