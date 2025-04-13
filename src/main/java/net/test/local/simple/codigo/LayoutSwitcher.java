/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

/**
 *
 * @author pc
 */
public class LayoutSwitcher extends JFrame {

    private JPanel mainPanel;
    private JRadioButtonMenuItem verticalItem;
    private JRadioButtonMenuItem horizontalItem;

    public LayoutSwitcher() {
        initUI();
    }

    private void initUI() {
        setTitle("Switch Layout Example");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        createMenuBar();
        createMainPanel();

        add(mainPanel);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu layoutMenu = new JMenu("Layout");

        verticalItem = new JRadioButtonMenuItem("Vertical");
        horizontalItem = new JRadioButtonMenuItem("Horizontal");

        ButtonGroup group = new ButtonGroup();
        group.add(verticalItem);
        group.add(horizontalItem);
        verticalItem.setSelected(true);

        verticalItem.addActionListener(new LayoutActionListener());
        horizontalItem.addActionListener(new LayoutActionListener());

        layoutMenu.add(verticalItem);
        layoutMenu.add(horizontalItem);
        menuBar.add(layoutMenu);
        setJMenuBar(menuBar);
    }

    private void createMainPanel() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Agregamos componentes de ejemplo
        mainPanel.add(createButton("Botón 1"));
        mainPanel.add(createButton("Botón 2"));
        mainPanel.add(createButton("Botón 3"));
    }

    private JButton createButton(String text) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setAlignmentY(Component.CENTER_ALIGNMENT); // Alineación horizontal
        return button;
    }

    private class LayoutActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (verticalItem.isSelected()) {
                mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            } else {
                mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
            }

            // Forzar actualización del layout
            mainPanel.revalidate();
            mainPanel.repaint();
        }
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            LayoutSwitcher ex = new LayoutSwitcher();
            ex.setVisible(true);
        });
    }
}
