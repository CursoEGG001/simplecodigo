/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author pc
 */

public class ResizableBufferStarvationMonitor {
    private static volatile boolean running = true;
    private static final int BUFFER_SIZE = 4096;
    private static final Color STARVATION_COLOR = new Color(255, 50, 50, 150);
    
    private static final int MAX_SAMPLES = 2000; // Keep last N samples for redrawing
    private static final Queue<Integer> sampleQueue = new LinkedList<>();
    private static int starvationCount = 0;
    private static Dimension currentSize = new Dimension(960, 400);

    public static void main(String[] args) {
        // Create visualization frame
        JFrame frame = new JFrame("Audio Buffer Monitor");
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawVisualization(g, getWidth(), getHeight());
            }
        };
        
        // Handle window resizing
        panel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                currentSize = panel.getSize();
                panel.repaint();
            }
        });

        frame.setPreferredSize(new Dimension(960, 400));
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
                
                while (running) {
                    int available = line.available();
                    if (available >= BUFFER_SIZE) {
                        line.read(buffer, 0, BUFFER_SIZE);
                        processAudioData(buffer);
                        starvationCount = 0;
                    } else {
                        starvationCount++;
                    }
                    
                    SwingUtilities.invokeLater(panel::repaint);
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
            e.printStackTrace();
        }
    }

    private static void processAudioData(byte[] buffer) {
        synchronized (sampleQueue) {
            for (int i = 0; i < buffer.length; i += 2) {
                int sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
                sampleQueue.add(sample);
                
                // Maintain queue size
                while (sampleQueue.size() > MAX_SAMPLES) {
                    sampleQueue.poll();
                }
            }
        }
    }

    private static void drawVisualization(Graphics g, int width, int height) {
        // Create temporary buffer for drawing
        BufferedImage bufferImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bufferImage.createGraphics();
        
        // Clear background
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, height);
        
        // Draw waveform
        drawWaveform(g2d, width, height);
        
        // Draw starvation warning if needed
        if (starvationCount > 2) {
            drawStarvationWarning(g2d, width, height);
        }
        
        // Draw the buffer image to screen
        g.drawImage(bufferImage, 0, 0, null);
        g2d.dispose();
    }

    private static void drawWaveform(Graphics2D g, int width, int height) {
        g.setColor(Color.GREEN);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        synchronized (sampleQueue) {
            if (sampleQueue.isEmpty()) return;
            
            int xStep = Math.max(1, width / sampleQueue.size());
            int prevX = 0;
            int prevY = height/2;
            int x = 0;
            
            for (int sample : sampleQueue) {
                int y = (sample * height/2) / 32768 + height/2;
                y = Math.max(0, Math.min(height-1, y));
                
                if (x > 0) {
                    g.drawLine(prevX, prevY, x, y);
                }
                
                prevX = x;
                prevY = y;
                x += xStep;
                
                if (x >= width) break;
            }
        }
    }

    private static void drawStarvationWarning(Graphics2D g, int width, int height) {
        // Draw red overlay
        g.setColor(STARVATION_COLOR);
        g.fillRect(0, 0, width, height);
        
        // Draw warning text
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        String warning = "BUFFER STARVATION!";
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(warning);
        int textHeight = fm.getHeight();
        
        // Center text
        int x = (width - textWidth) / 2;
        int y = (height - textHeight) / 2 + fm.getAscent();
        g.drawString(warning, x, y);
    }
}