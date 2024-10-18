/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author pc
 */
public class IconSliderComboBox {

    public static void main(String[] args) {
        JFrame frame = new JFrame("Icon Combo Slider Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create a JComboBox with items
        String[] items = {"Item 1", "Item 2", "Item 3"};
        JComboBox<String> comboBox = new JComboBox<>(items);

        // Create custom icon renderer
        IconComboRenderer renderer = new IconComboRenderer();
        comboBox.setRenderer(renderer);

        // Create a slider to control the icon proportion
        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 100, 50);
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int value = slider.getValue();
                String selectedItem = (String) comboBox.getSelectedItem();
                renderer.updateProportion(selectedItem, value);
                comboBox.repaint();
            }
        });

        // Add an ActionListener to the JComboBox to set the selected item's icon
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedItem = (String) comboBox.getSelectedItem();
                slider.setValue(renderer.getProportion(selectedItem));
            }
        });

        // Create a panel to hold the combo box and slider
        JPanel panel = new JPanel();
        panel.add(comboBox);
        panel.add(slider);

        frame.getContentPane().add(panel);
        frame.pack();
        frame.setVisible(true);
    }

    static class IconComboRenderer extends DefaultListCellRenderer {

        private Map<String, MyIcon> iconMap = new HashMap<>();

        public IconComboRenderer() {
            iconMap.put("Item 1", new MyIcon(50, Color.RED, Color.RED.darker()));
            iconMap.put("Item 2", new MyIcon(50, Color.GREEN, Color.GREEN.darker()));
            iconMap.put("Item 3", new MyIcon(50, Color.MAGENTA, Color.MAGENTA.darker()));
        }

        public void updateProportion(String item, int proportion) {
            MyIcon icon = iconMap.get(item);
            if (icon != null) {
                icon.setProportion(proportion);
            }
        }

        public int getProportion(String item) {
            MyIcon icon = iconMap.get(item);
            return icon != null ? icon.getProportion() : 50;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            // Set the icon for the current item
            MyIcon icon = iconMap.get(value.toString());
            if (icon != null) {
                label.setIcon(icon);
            }

            return label;
        }
    }

    static class MyIcon implements Icon {

        private int proportion;
        private Color color1;
        private Color color2;

        public MyIcon(int proportion, Color color1, Color color2) {
            this.proportion = proportion;
            this.color1 = color1;
            this.color2 = color2;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            // Draw the first color
            g.setColor(color1);
            g.fillRect(x, y, (int) (getIconWidth() * proportion / 100), getIconHeight());

            // Draw the second color
            g.setColor(color2);
            g.fillRect(x + (int) (getIconWidth() * proportion / 100), y, getIconWidth() - (int) (getIconWidth() * proportion / 100), getIconHeight());
        }

        public void setProportion(int proportion) {
            this.proportion = proportion;
        }

        public int getProportion() {
            return proportion;
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }
    }
}
