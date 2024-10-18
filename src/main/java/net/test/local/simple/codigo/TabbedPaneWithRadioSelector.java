/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

/**
 *
 * @author pc
 */
public class TabbedPaneWithRadioSelector extends JFrame {

    private JTabbedPane tabbedPane;
    private final JPanel tabSelectorPanel;
    private final JRadioButton topButton;
    private final JRadioButton leftButton;
    private final JRadioButton rightButton;
    private final JRadioButton bottomButton;
    private JTextField tab2LabelField;
    private final JPanel tab3Panel;
    private final JCheckBox enableIconsCheckBox;
    private final JComboBox<Icon> iconComboBox;
    private final JComboBox<String> tabLabelComboBox;

    public TabbedPaneWithRadioSelector() {
        super("Panel de pestañas con Selector Radio");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create tab selector panel with radio buttons
        tabSelectorPanel = new JPanel(new GridLayout(4, 1));
        topButton = new JRadioButton("Top(Arriba)");
        leftButton = new JRadioButton("Left(Izquierda)");
        rightButton = new JRadioButton("Right(Derecha)");
        bottomButton = new JRadioButton("Bottom(Abajo)");
        ButtonGroup group = new ButtonGroup();
        group.add(topButton);
        group.add(leftButton);
        group.add(rightButton);
        group.add(bottomButton);
        tabSelectorPanel.add(topButton);
        tabSelectorPanel.add(leftButton);
        tabSelectorPanel.add(rightButton);
        tabSelectorPanel.add(bottomButton);

        // Create Tab 2 with label field and combobox
        JPanel tab2Panel = new JPanel();
        JLabel label = new JLabel("Tab 2 Etiqueta:");
        tab2LabelField = new JTextField(10);
        JButton updateButton = new JButton("Actualizar Etiqueta");
        tabLabelComboBox = new JComboBox<>(); // New combobox for tab selection

        tab2Panel.add(label);
        tab2Panel.add(tabLabelComboBox);
        tab2Panel.add(tab2LabelField);
        tab2Panel.add(updateButton);

        updateButton.addActionListener(e -> {
            String newLabel = tab2LabelField.getText().trim();
            if (!newLabel.isEmpty()) {
                int selectedTabIndex = tabLabelComboBox.getSelectedIndex();
                if (selectedTabIndex >= 0) {
                    tabbedPane.setTitleAt(selectedTabIndex, newLabel);
                    tabLabelComboBox.setSelectedItem(newLabel); // Update combobox selection
                }
                tab2LabelField.setText("");
            }
        });

        // Create a BufferedImage for the X icon
        int iconSize = 24;
        BufferedImage xIconImage = new BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = xIconImage.createGraphics();
        g2d.setColor(Color.ORANGE.darker());
        g2d.drawLine(0, 0, iconSize - 1, iconSize - 1);
        g2d.drawLine(iconSize - 1, 0, 0, iconSize - 1);
        g2d.dispose();

        // Create a BufferedImage for the thick line square icon
        BufferedImage squareIconImage = new BufferedImage(iconSize, iconSize, BufferedImage.TYPE_INT_ARGB);
        g2d = squareIconImage.createGraphics();
        g2d.setColor(Color.BLUE);
        g2d.fillRect(0, 0, iconSize, iconSize);
        g2d.setStroke(new BasicStroke(3)); // Set the stroke width to 5
        g2d.clearRect(4, 3, iconSize - 4, iconSize - 3);
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawRect(4, 3, iconSize, iconSize);
        g2d.dispose();

        // Create Tab 3 with checkbox and combobox
        tab3Panel = new JPanel();
        enableIconsCheckBox = new JCheckBox("Habilitar Iconos Pestaña");
        iconComboBox = new JComboBox<>();
        iconComboBox.addItem(null); // Default no icon
        iconComboBox.addItem(new ImageIcon(xIconImage)); // Add your icons here
        iconComboBox.addItem(new ImageIcon(squareIconImage));
        iconComboBox.addActionListener(e -> updateTabIcons());
        tab3Panel.add(enableIconsCheckBox);
        tab3Panel.add(iconComboBox);

        // Create tabbed pane
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Tab 1", tabSelectorPanel);
        tabbedPane.addTab("Tab 2", tab2Panel);
        tabbedPane.addTab("Tab 3", tab3Panel);
        for (int i = 0; i < tabbedPane.getTabCount(); i++) { // Add tab labels to combobox on tab3
            tabLabelComboBox.addItem(tabbedPane.getTitleAt(i));
        }

        // Update tab icons on checkbox change
        enableIconsCheckBox.addActionListener(e -> updateTabIcons());

        // Add tabbed pane and tab selector panel to the frame
        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);

        // Add action listeners to radio buttons
        topButton.addActionListener(
                (var e) -> tabbedPane.setTabPlacement(JTabbedPane.TOP)
        );
        leftButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tabbedPane.setTabPlacement(JTabbedPane.LEFT);
            }
        });
        rightButton.addActionListener(
                e -> {
                    tabbedPane.setTabPlacement(JTabbedPane.RIGHT);
                }
        );
        bottomButton.addActionListener(
                (ActionEvent e) -> tabbedPane.setTabPlacement(JTabbedPane.BOTTOM)
        );

        pack();
        setVisible(true);
    }

    private void updateTabIcons() {

        if (enableIconsCheckBox.isSelected()) {
            Icon selectedIcon = (Icon) iconComboBox.getSelectedItem();
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                tabbedPane.setIconAt(i, selectedIcon);
            }
        } else {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                tabbedPane.setIconAt(i, null);
            }
        }
    }

    public static void main(String[] args) {
        new TabbedPaneWithRadioSelector();
    }
}
