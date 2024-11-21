package net.test.local.simple.codigo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import javax.sound.sampled.*;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import javax.swing.UIManager.LookAndFeelInfo;

public class AudioVolumeControlDemo {

    private static final int SLIDER_MIN = 0;
    private static final int SLIDER_MAX = 100;

    /**
     * Audio volumen slider demo.
     *
     * @param args
     * @throws LineUnavailableException
     * @throws UnsupportedAudioFileException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws UnsupportedLookAndFeelException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws
            LineUnavailableException,
            UnsupportedAudioFileException,
            IOException,
            ClassNotFoundException,
            IllegalAccessException,
            InstantiationException,
            UnsupportedLookAndFeelException,
            InterruptedException {

        // Set Nimbus Look and Feel with custom colors (optional)
        UIManager.put("nimbusBase", Color.YELLOW.darker());
        UIManager.put("nimbusBlueGrey", Color.ORANGE.darker().darker());
        UIManager.put("control", Color.ORANGE.darker());

        for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                UIManager.setLookAndFeel(info.getClassName());
                break;
            }
        }

        // Load audio file
        File audioFile = new File("C:\\Users\\pc\\Music\\Hybrid.wav"); // Replace with your audio file path

        // Create a Clip for playback
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
        AudioFormat format = audioInputStream.getFormat();
        DataLine.Info info = new DataLine.Info(Clip.class, format);
        try (Clip clip = (Clip) AudioSystem.getLine(info)) {
            clip.open(audioInputStream);

            // Create labels
            JLabel sliderLabel = new JLabel("Volume Slider") {
                @Override
                protected void paintComponent(Graphics g) {
                    var g2d = (Graphics2D) g.create();
                    double w = getWidth() / 2;
                    double h = getHeight() / 2;
                    double r = Math.PI / 2;

                    AffineTransform at = AffineTransform.getRotateInstance(r, w, h);

                    g2d.setTransform(at);
                    super.paintComponent(g2d);
                    g2d.dispose();
                }
            };
            sliderLabel.setHorizontalTextPosition(JLabel.CENTER);
            sliderLabel.setVerticalAlignment(JLabel.TOP);

            JLabel valueLabel = new JLabel(SLIDER_MAX + "%");
            JLabel valueLabel2 = new JLabel("");

            // Get the Master Gain control
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

            float minGain = gainControl.getMinimum();
            float maxGain = gainControl.getMaximum();

            String gainUnit = gainControl.getUnits(); // Get the gain unit (e.g., dB)

            // Create labels for gain info
            JLabel gainMinLabel = new JLabel(String.format("%.2f", minGain) + gainUnit);
            JLabel gainMaxLabel = new JLabel(String.format("%.2f", maxGain) + gainUnit);

            // Create a panel for the slider and gain info
            JPanel sliderAndGainPanel = new JPanel(new BorderLayout());

            // Create a JSlider for volume control
            JSlider volumeSlider = new JSlider(JSlider.VERTICAL, SLIDER_MIN, SLIDER_MAX, SLIDER_MAX);
            int defValue = (int) (((gainControl.getValue() - minGain) * SLIDER_MAX) / (maxGain - minGain));
            System.out.println(defValue + " " + SLIDER_MAX);
            volumeSlider.setValue(defValue);
            valueLabel.setText(defValue + "%");
            volumeSlider.addChangeListener(e -> {
                int value = volumeSlider.getValue();
                float gain = minGain + (maxGain - minGain) * value / SLIDER_MAX;
                gainControl.setValue(gain);
                valueLabel.setText(value + "%"); // Update the value label
            });
            volumeSlider.setMajorTickSpacing(25);
            volumeSlider.setMinorTickSpacing(5);
            volumeSlider.setPaintTicks(true);
            volumeSlider.setPaintTrack(true);
            volumeSlider.setPaintLabels(true);

            sliderAndGainPanel.add(volumeSlider, BorderLayout.CENTER);
            sliderAndGainPanel.add(gainMaxLabel, BorderLayout.NORTH);
            sliderAndGainPanel.add(gainMinLabel, BorderLayout.SOUTH);

            // Create a panel for the components with padding
            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16)); // Adjust padding as needed

            contentPanel.add(sliderLabel, BorderLayout.EAST);
            contentPanel.add(sliderAndGainPanel, BorderLayout.CENTER); // Add the combined slider & gain info panel
            contentPanel.add(valueLabel, BorderLayout.WEST);
            contentPanel.add(valueLabel2, BorderLayout.SOUTH);

            // Create a JFrame
            JFrame frame = new JFrame("Audio Volume Control");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(220, 260); // Adjust size to accommodate additional label

            // Add the panel to the frame
            frame.add(contentPanel);
            frame.pack();

            frame.setVisible(true);
            frame.setLocationRelativeTo(null);

            // Play audio
            clip.start();

            while (clip.getFramePosition() != clip.getFrameLength()) {
                valueLabel2.setText("Clip: " + clip.getFramePosition() + " de " + clip.getFrameLength());
            }
        }
    }
}
