/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Painter;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.border.EtchedBorder;

/**
 *
 * @author pc
 */
public class SliderSkinDemo2 {

    public JComponent makeUI() {
        UIDefaults d = new UIDefaults();
        d.put("Slider:SliderTrack[Enabled].backgroundPainter", (Painter<JSlider>) (Graphics2D g, JSlider c, int w, int h) -> {
            // Calculate thumb position based on value
            int thumbPos = (int) (c.getValue() * w / (c.getMaximum() + c.getMinimum()));

            g.setStroke(new BasicStroke(1.1f));
            g.setColor(Color.ORANGE.darker());
            g.fillRoundRect(0, 4, w, h - 8, 12, 14);
            if (c.getOrientation() == JSlider.HORIZONTAL) {

                // Horizontal slider: split by thumb position
                g.setColor(Color.red.darker());
                g.fill(new Rectangle2D.Double(4, 6, thumbPos, h - 12)); // Left side (or top for vertical)

                g.setColor(Color.MAGENTA);
                g.fill(new Rectangle2D.Double(thumbPos, 6, w - thumbPos, h - 12)); // Right side (or bottom for vertical)

            } else if (c.getOrientation() == JSlider.VERTICAL) {
                // Vertical slider: split by thumb position (rotation is done on this horizontal slide in another code)

                g.setColor(Color.GREEN);
                g.fill(new Rectangle2D.Double(w - thumbPos, 6, thumbPos, h - 12)); // Bottom side

                g.setColor(Color.BLUE);
                g.fill(new Rectangle2D.Double(4, 6, w - thumbPos, h - 12)); // Top side

            }
            g.setStroke(new BasicStroke(1.1f));
            g.setColor(Color.WHITE);
            g.drawRoundRect(0, 4, w, h - 6, 12, 14);
        });
        JSlider slider3 = new JSlider();
        slider3.putClientProperty("Slider.paintThumbArrowShape", Boolean.TRUE); // Enable ArrowShape

        JSlider slider = new JSlider();
        slider.putClientProperty("Nimbus.Overrides", d);
        slider.setOrientation(JSlider.HORIZONTAL);

        JSlider slider2 = new JSlider();
        slider2.putClientProperty("Nimbus.Overrides", d);
        slider2.putClientProperty("Slider.paintThumbArrowShape", Boolean.TRUE);
        slider2.setOrientation(JSlider.VERTICAL);

        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4, 5, 2, 4), BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)));
        p.setBackground(Color.gray.darker());

        p.add(new JSlider());

        p.add(slider);
        p.add(slider2);
        p.add(slider3);

        //p.add(Box.createRigidArea(new Dimension(262, 340)));
        return p;
    }

    public static void main(String... args) {
        EventQueue.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(laf.getName())) {
                        UIManager.setLookAndFeel(laf.getClassName());
                    }
                }
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
                System.out.println("Not ready L&F");
            }
            JFrame f = new JFrame();
            f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            f.getContentPane().add(new SliderSkinDemo2().makeUI());
            f.setSize(320, 360);

            f.setLocationRelativeTo(null);
            f.setVisible(true);

        });
    }
}
