/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author pc
 */
public class DynamicLookAndFeelExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Dynamic Look and Feel Example");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(600, 400);

            JPanel panel = new JPanel(new BorderLayout());

            // ComboBox to change Look and Feel
            JComboBox<UIManager.LookAndFeelInfo> lafComboBox = new JComboBox<>(UIManager.getInstalledLookAndFeels());
            
            lafComboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof UIManager.LookAndFeelInfo lfListado) {
                        setText(lfListado.getName());
                    }
                    return this;
                }
            });

            JProgressBar demoProgBar = new JProgressBar(SwingConstants.VERTICAL);
            
            demoProgBar.setMinimum(0);
            demoProgBar.setMaximum(100);
            demoProgBar.setValue(50);
            demoProgBar.setStringPainted(true);

            lafComboBox.addActionListener((var e) -> {
                var selectedLAF = (UIManager.LookAndFeelInfo) lafComboBox.getSelectedItem();
                try {
                    UIManager.setLookAndFeel(selectedLAF.getClassName());
                    SwingUtilities.updateComponentTreeUI(frame);
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                    System.out.println("Unable to set Look and Feel: " + ex.getMessage());
                }
            });

            // Panel to hold various components
            JPanel componentsPanel = new JPanel(new GridLayout(4, 1));
            componentsPanel.add(new JScrollPane(new JTextArea("Scrollable Text Area")));
            componentsPanel.add(new JSpinner());
            componentsPanel.add(new JSlider());
            componentsPanel.add(new JTextField("Text Field"));
            componentsPanel.add(demoProgBar);
            componentsPanel.add(new JList(
                    new String[]{
                        "uno", "dos", "tres", "cuatro"
                    }
            ));
            componentsPanel.add(new JLabel("Label of text"));

            panel.add(lafComboBox, BorderLayout.NORTH);
            panel.add(componentsPanel, BorderLayout.CENTER);

            frame.add(panel);
            frame.setVisible(true);
        });
    }
}
