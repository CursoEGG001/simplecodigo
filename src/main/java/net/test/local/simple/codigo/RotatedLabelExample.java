/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author pc
 */
public class RotatedLabelExample extends JPanel {

    private String text;
    private int rotationAngle = Math.floorMod((int) Math.random() * Integer.MAX_VALUE - 1, 90); // Adjust rotation angle as needed

    public RotatedLabelExample(String text) {
        this.text = text;
        setPreferredSize(new Dimension(100, 100)); // Adjust initial size as needed
        setLayout(new BorderLayout());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.rotate(Math.toRadians(rotationAngle), getWidth() / 2, getHeight() / 2);

        Font font = new Font("Arial", Font.PLAIN, 12); // Adjust font as needed
        g2d.setFont(font);

        FontMetrics metrics = g2d.getFontMetrics(font);
        Rectangle2D bounds = metrics.getStringBounds(text, g2d);

        int labelWidth = (int) bounds.getHeight();
        int labelHeight = (int) bounds.getWidth();

        setPreferredSize(new Dimension(labelWidth, labelHeight));
        revalidate();

        g2d.setColor(Color.BLACK);
        g2d.drawString(text, (getWidth() - labelWidth) / 2, (getHeight() - labelHeight) / 2 + metrics.getAscent());

        g2d.dispose();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Rotated Label Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        RotatedLabelExample panel = new RotatedLabelExample("My Rotated Label");
        frame.add(panel);

        frame.pack();
        frame.setVisible(true);
    }
}
