/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author pc
 */
public class VisualBufferStarvationMonitor {

    private static volatile boolean running = true;
    private static final int BUFFER_SIZE = 4096;
    private static final int VIS_WIDTH = 960;
    private static final int VIS_HEIGHT = 400;
    private static final Color STARVATION_COLOR = new Color(255, 50, 50, 150);

    private static int[] samples = new int[VIS_WIDTH];
    private static int sampleIndex = 0;
    private static int starvationCount = 0;

    public static void main(String[] args) {
        // Create visualization frame
        JFrame frame = new JFrame("Audio Buffer Monitor");
        BufferedImage image = new BufferedImage(VIS_WIDTH, VIS_HEIGHT, BufferedImage.TYPE_INT_RGB);
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(image, 0, 0, null);
            }
        };
        panel.setPreferredSize(new Dimension(VIS_WIDTH, VIS_HEIGHT));
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Setup audio capture
        try {
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
            TargetDataLine line = AudioSystem.getTargetDataLine(format);
            line.open(format, BUFFER_SIZE * 4);
            line.start();

            // Start buffer monitoring thread
            new Thread(() -> {
                byte[] buffer = new byte[BUFFER_SIZE];
                Graphics2D g = image.createGraphics();
                g.setBackground(Color.BLACK);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                while (running) {
                    // Clear the image periodically
                    if (sampleIndex == 0) {
                        g.setColor(Color.BLACK);
                        g.fillRect(0, 0, VIS_WIDTH, VIS_HEIGHT);
                    }

                    int available = line.available();
                    if (available >= BUFFER_SIZE) {
                        line.read(buffer, 0, BUFFER_SIZE);
                        processAudioData(buffer, g);
                        starvationCount = 0;
                    } else {
                        starvationCount++;
                        if (starvationCount > 2) {
                            drawStarvationWarning(g);
                        }
                    }

                    panel.repaint();
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                line.close();
            }).start();

            // Run for 20 seconds
            Thread.sleep(20000);
            running = false;

        } catch (LineUnavailableException | InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void processAudioData(byte[] buffer, Graphics2D g) {
        // Convert 16-bit audio data to visualization points
        for (int i = 0; i < buffer.length; i += 2) {
            int sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
            samples[sampleIndex] = (sample * VIS_HEIGHT / 2) / 32768 + VIS_HEIGHT / 2;

            // Draw waveform
            if (sampleIndex > 0) {
                g.setColor(Color.GREEN);
                g.drawLine(sampleIndex - 1, samples[sampleIndex - 1],
                        sampleIndex, samples[sampleIndex]);
            }

            sampleIndex = (sampleIndex + 1) % VIS_WIDTH;
        }
    }

    private static void drawStarvationWarning(Graphics2D g) {
        // Draw red overlay
        g.setColor(STARVATION_COLOR);
        g.fillRect(0, 0, VIS_WIDTH, VIS_HEIGHT);

        // Draw warning text
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        String warning = "BUFFER STARVATION!";
        int textWidth = g.getFontMetrics().stringWidth(warning);
        g.drawString(warning, (VIS_WIDTH - textWidth) / 2, VIS_HEIGHT / 2);
    }
}
