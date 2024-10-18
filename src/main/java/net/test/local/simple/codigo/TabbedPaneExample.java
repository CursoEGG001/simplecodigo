/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/**
 *
 * @author pc
 */
public class TabbedPaneExample {

    public static void main(String[] args) {

        JFrame frame = new JFrame("Tabbed Pane with Custom Tab Selector");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setTabPlacement(JTabbedPane.LEFT);

        tabbedPane.setUI(new MyTabbedPaneUI());

        tabbedPane.addTab("Tab 1", new JPanel());
        tabbedPane.addTab("Tab 2", new JPanel());
        tabbedPane.addTab("Tab 3", new JPanel());

        frame.add(tabbedPane);
        frame.setSize(400, 300);
        frame.setVisible(true);
    }

    public static class MyTabbedPaneUI extends BasicTabbedPaneUI {

        @Override
        protected void paintTab(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect) {

            // Rotate the Graphics2D context for vertical drawing
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.translate(rects[tabIndex].x + 5, rects[tabIndex].y + 5); // Adjust translation for padding
            g2d.rotate(Math.toRadians(-90));  // Rotate 90 degrees counter-clockwise

            // Get the font metrics and adjust X position for centering
            FontMetrics metrics = g2d.getFontMetrics();
            int stringWidth = metrics.stringWidth(tabPane.getTitleAt(tabIndex));
            int x = (rects[tabIndex].height - stringWidth);

            // Set the text color and draw the rotated label
            g2d.drawString(tabPane.getTitleAt(tabIndex), x, 4);
            g2d.dispose();

        }

    }
}
