/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.BorderLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.AffineTransform;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;

/**
 *
 * @author pc
 */
public class RotatingLabelsDemo extends JFrame {

    private JSlider slider;
    private RotatingLabel[] labels;

    public RotatingLabelsDemo() {
        setTitle("Rotating Labels Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Create labels
        labels = new RotatingLabel[8];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = new RotatingLabel("Label " + (i + 1));
        }

        // Create slider
        slider = new JSlider(0, 360, 0);
        slider.addChangeListener((ChangeEvent e) -> {
            int rotationAngle = slider.getValue();
            for (int i = 0; i < labels.length; i++) {
                labels[i].setRotationAngle((i < 4 ? 45 : 0) + rotationAngle + (i < 4 ? i * 45 : -i * 45));
                labels[i].repaint();
            }
        });

        // Set layout
        setLayout(new BorderLayout());
        JPanel labelPanel = new JPanel(new GridLayout(3, 3));
        for (int i = 0; i < labels.length; i++) {
            if (i < 4) {
                RotatingLabel label = labels[i];
                labelPanel.add(label);
            } else {
                if (i != 4) {
                    RotatingLabel label = labels[i];
                    labelPanel.add(label);
                } else {
                    labelPanel.add(slider);
                    RotatingLabel label = labels[i];
                    labelPanel.add(label);
                }
            }
        }
        add(labelPanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RotatingLabelsDemo frame = new RotatingLabelsDemo();
            frame.setVisible(true);
        });
    }

    private static class RotatingLabel extends JLabel {

        private int rotationAngle;

        public RotatingLabel(String text) {
            super(text);
        }

        public void setRotationAngle(int rotationAngle) {
            this.rotationAngle = rotationAngle;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            // Get the text width and height
            FontMetrics metrics = g2d.getFontMetrics();
            int textWidth = metrics.stringWidth(getText());
            int textHeight = metrics.getHeight();

            // Calculate the center point of the text within the label
            int centerX = (int) 2 * textWidth * textHeight / (textHeight + textWidth);
            int centerY = (int) ((1 / textHeight) + (1 / textWidth)) / 2;

            g2d.translate(centerX, -centerY);
            // Set the rotation point to the center of the label
            AffineTransform at = AffineTransform.getRotateInstance(
                    Math.toRadians(rotationAngle), centerX, centerY);

            g2d.setTransform(at);
            g2d.translate(-centerX, centerY);
            super.paintComponent(g2d);
        }
    }
}
