/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

public class RotatingLabelApp extends JFrame {

    private JLabel rotatingLabel;
    private JSlider rotationSlider;

    public RotatingLabelApp() {
        setTitle("Rotating Label App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create the rotating label
        rotatingLabel = new JLabel("Rotate Me!") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                int w = getWidth();
                int h = getHeight();

                // Set the rotation point to the center of the label
                AffineTransform at = AffineTransform.getRotateInstance(
                        Math.toRadians(rotationSlider.getValue()), w / 2.0, h / 2.0);

                g2d.setTransform(at);
                super.paintComponent(g2d);
                g2d.dispose();
            }
        };
        rotatingLabel.setHorizontalAlignment(JLabel.CENTER);
        rotatingLabel.setPreferredSize(new Dimension(200, 200));

        // Create the slider
        rotationSlider = new JSlider(JSlider.HORIZONTAL, 0, 90, 0);
        rotationSlider.setInverted(true);
        rotationSlider.setMajorTickSpacing(15);
        rotationSlider.setMinorTickSpacing(5);
        rotationSlider.setPaintTicks(true);
        rotationSlider.setPaintLabels(true);

        // Add change listener to the slider
        rotationSlider.addChangeListener(e -> rotatingLabel.repaint());

        // Add components to the frame
        add(rotatingLabel, BorderLayout.CENTER);
        add(rotationSlider, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new RotatingLabelApp().setVisible(true);
        });
    }
}
