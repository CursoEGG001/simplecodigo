/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class OscilloscopeAnalyzer extends JFrame {
    private static final int BUFFER_SIZE = 2048;
    private static final int MAX_POINTS = 1024;
    private final LinkedList<Short> waveformBuffer;
    private final JComboBox<String> inputLineComboBox;
    private final JProgressBar volumeMeter;
    private final JLabel frequencyLabel;
    private TargetDataLine micLine;
    private final ExecutorService audioExecutor;
    private volatile boolean running = true;
    private Canvas canvas;
    private BufferStrategy bufferStrategy;
    private final ScheduledExecutorService renderExecutor;
    private final ConcurrentLinkedQueue<Short> audioQueue;

    public OscilloscopeAnalyzer() {
        setTitle("Audio Oscilloscope");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());

        // Initialize buffers
        waveformBuffer = new LinkedList<>();
        audioQueue = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < MAX_POINTS; i++) {
            waveformBuffer.add((short) 0);
        }

        // Create Canvas for hardware-accelerated rendering
        canvas = new Canvas();
        canvas.setPreferredSize(new Dimension(800, 400));
        canvas.setIgnoreRepaint(true);
        add(canvas, BorderLayout.CENTER);

        // Control Panel
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));

        // Input Selection
        List<String> availableLines = getAvailableInputLines();
        inputLineComboBox = new JComboBox<>(availableLines.toArray(String[]::new));
        inputLineComboBox.addActionListener(e -> restartAudioCapture());
        controlPanel.add(new JLabel("Select Input:"));
        controlPanel.add(inputLineComboBox);

        // Volume Meter
        volumeMeter = new JProgressBar(0, 100);
        volumeMeter.setStringPainted(true);
        volumeMeter.setBorder(BorderFactory.createTitledBorder("Volume"));
        controlPanel.add(volumeMeter);

        // Frequency Display
        frequencyLabel = new JLabel("Dominant Frequency: ? Hz");
        controlPanel.add(frequencyLabel);

        add(controlPanel, BorderLayout.SOUTH);

        // Initialize executors
        audioExecutor = Executors.newSingleThreadExecutor();
        renderExecutor = Executors.newSingleThreadScheduledExecutor();

        // Setup rendering surface
        pack();
        canvas.createBufferStrategy(2);
        bufferStrategy = canvas.getBufferStrategy();

        setLocationRelativeTo(null);
        setVisible(true);

        // Start processing
        startAudioCapture();
        startRenderLoop();
    }

    private void startRenderLoop() {
        // Get refresh rate of the default screen
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        DisplayMode dm = gd.getDisplayMode();
        int refreshRate = dm.getRefreshRate();
        if (refreshRate <= 0) refreshRate = 60; // Fallback to 60Hz if unable to detect

        // Schedule render updates at screen refresh rate
        long periodNanos = (long)(1_000_000_000.0 / refreshRate);
        renderExecutor.scheduleAtFixedRate(this::render, 0, periodNanos, TimeUnit.NANOSECONDS);
    }

    private void render() {
        if (!running || bufferStrategy == null) return;

        // Process any new audio data
        Short value;
        while ((value = audioQueue.poll()) != null) {
            waveformBuffer.add(value);
            while (waveformBuffer.size() > MAX_POINTS) {
                waveformBuffer.removeFirst();
            }
        }

        // Get rendering context
        Graphics2D g = null;
        try {
            g = (Graphics2D) bufferStrategy.getDrawGraphics();
            drawWaveform(g);
        } finally {
            if (g != null) g.dispose();
        }

        // Show the rendered frame
        if (!bufferStrategy.contentsLost()) {
            bufferStrategy.show();
        }

        // Sync with monitor's refresh rate
        Toolkit.getDefaultToolkit().sync();
    }

    private void drawWaveform(Graphics2D g) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        int centerY = height / 2;

        // Clear background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);

        // Configure rendering quality
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(1.5f));

        // Draw grid
        drawGrid(g, width, height, centerY);

        // Draw waveform
        if (!waveformBuffer.isEmpty()) {
            g.setColor(Color.GREEN);
            double xScale = (double) width / (MAX_POINTS - 1);
            double yScale = height * 0.45 / Short.MAX_VALUE;

            int prevX = 0;
            int prevY = centerY - (int) (waveformBuffer.get(0) * yScale);

            for (int i = 1; i < waveformBuffer.size(); i++) {
                int x = (int) (i * xScale);
                int y = centerY - (int) (waveformBuffer.get(i) * yScale);
                g.drawLine(prevX, prevY, x, y);
                prevX = x;
                prevY = y;
            }
        }
    }

    private void drawGrid(Graphics2D g, int width, int height, int centerY) {
        // Draw grid lines
        g.setColor(new Color(0, 50, 0));
        int gridSpacingX = width / 16;
        int gridSpacingY = height / 8;

        for (int i = 0; i <= width; i += gridSpacingX) {
            g.drawLine(i, 0, i, height);
        }
        for (int i = 0; i <= height; i += gridSpacingY) {
            g.drawLine(0, i, width, i);
        }

        // Draw center line
        g.setColor(new Color(0, 100, 0));
        g.drawLine(0, centerY, width, centerY);
    }

    private List<String> getAvailableInputLines() {
        List<String> lineNames = new ArrayList<>();
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            for (Line.Info lineInfo : mixer.getTargetLineInfo()) {
                if (lineInfo.getLineClass() == TargetDataLine.class) {
                    lineNames.add(mixerInfo.getName());
                    break;
                }
            }
        }
        return lineNames;
    }

    private void startAudioCapture() {
        try {
            String selectedMixerName = (String) inputLineComboBox.getSelectedItem();
            Mixer selectedMixer = null;
            
            for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
                if (mixerInfo.getName().equals(selectedMixerName)) {
                    selectedMixer = AudioSystem.getMixer(mixerInfo);
                    break;
                }
            }

            if (selectedMixer == null) {
                throw new LineUnavailableException("No mixer found");
            }

            AudioFormat format = new AudioFormat(44100.0f, 16, 2, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            micLine = (TargetDataLine) selectedMixer.getLine(info);
            micLine.open(format);
            micLine.start();

            audioExecutor.submit(this::processAudioStream);

        } catch (LineUnavailableException e) {
            JOptionPane.showMessageDialog(this,
                    "Audio capture error: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void processAudioStream() {
        byte[] buffer = new byte[BUFFER_SIZE];

        while (running && !Thread.interrupted()) {
            int bytesRead = micLine.read(buffer, 0, buffer.length);
            if (bytesRead > 0) {
                short[] audioData = convertByteToShortArray(buffer, bytesRead);
                
                // Update audio metrics
                double rms = calculateRMSLevel(audioData);
                double freq = estimateDominantFrequency(audioData);
                
                // Update UI components on EDT
                SwingUtilities.invokeLater(() -> {
                    volumeMeter.setValue((int) (rms * 100));
                    frequencyLabel.setText(String.format("Dominant Frequency: %.2f Hz", freq));
                });

                // Add data to the queue for rendering
                for (short value : audioData) {
                    audioQueue.offer(value);
                }
            }
        }
    }

    private void restartAudioCapture() {
        if (micLine != null) {
            micLine.stop();
            micLine.close();
        }
        startAudioCapture();
    }

    private short[] convertByteToShortArray(byte[] audioData, int bytesRead) {
        short[] shortData = new short[bytesRead / 2];
        for (int i = 0; i < shortData.length; i++) {
            shortData[i] = (short) ((audioData[2 * i + 1] << 8) | (audioData[2 * i] & 0xFF));
        }
        return shortData;
    }

    private double calculateRMSLevel(short[] audioData) {
        long sum = 0;
        for (short sample : audioData) {
            sum += sample * sample;
        }
        return Math.sqrt((double) sum / audioData.length) / Short.MAX_VALUE;
    }

    private double estimateDominantFrequency(short[] audioData) {
        int zeroCrossings = 0;
        boolean wasMax = false;
        boolean wasMin = false;

        for (int i = 1; i < audioData.length - 1; i++) {
            boolean isMax = (audioData[i] > audioData[i - 1] && audioData[i] > audioData[i + 1]);
            boolean isMin = (audioData[i] < audioData[i - 1] && audioData[i] < audioData[i + 1]);

            if (wasMax && isMin || wasMin && isMax) {
                zeroCrossings++;
            }

            wasMax = isMax;
            wasMin = isMin;
        }

        return zeroCrossings * 44100.0 / (2.0 * audioData.length);
    }

    public void shutdown() {
        running = false;
        if (audioExecutor != null) {
            audioExecutor.shutdownNow();
        }
        if (renderExecutor != null) {
            renderExecutor.shutdownNow();
        }
        if (micLine != null && micLine.isOpen()) {
            micLine.stop();
            micLine.close();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            OscilloscopeAnalyzer analyzer = new OscilloscopeAnalyzer();
            Runtime.getRuntime().addShutdownHook(new Thread(analyzer::shutdown));
        });
    }
}