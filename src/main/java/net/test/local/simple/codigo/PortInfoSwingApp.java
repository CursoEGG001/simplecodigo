/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.Hashtable;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.CompoundControl;
import javax.sound.sampled.Control;
import javax.sound.sampled.EnumControl;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Port;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;

/**
 *
 * @author pc
 */
public class PortInfoSwingApp extends JFrame {

    private JComboBox<Port.Info> portComboBox;
    private final JTextArea controlTextArea;
    private final JButton refreshButton;
    private final JPanel controlsContainer;

    public PortInfoSwingApp() {
        setTitle("Java Sound API Port Selector");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        // Initialize components
        portComboBox = new JComboBox<>();
        portComboBox.setEditable(true); // Make the combo box editable
        controlTextArea = new JTextArea(10, 30);
        controlTextArea.setEditable(false); // Non-editable text area
        refreshButton = new JButton("Refresh Ports");
        controlsContainer = new JPanel();

        // Set up the layout
        setLayout(new BorderLayout());
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Select Port:"));
        topPanel.add(portComboBox);
        topPanel.add(refreshButton);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(controlTextArea), BorderLayout.CENTER);

        // Add action listener for combo box selection
        portComboBox.addActionListener((ActionEvent e) -> {
            Port.Info selectedPort = (Port.Info) portComboBox.getSelectedItem();
            if (selectedPort != null) {
                displayPortControls(selectedPort);
            }
        });

        // Add action listener for the refresh button
        refreshButton.addActionListener((ActionEvent e) -> {
            populatePorts();
        });

        // Populate ports at startup
        populatePorts();
    }

    // Method to populate the combo box with available ports
    private void populatePorts() {
        portComboBox.removeAllItems(); // Clear existing items

        Port.Info[] portInfos = {
            Port.Info.MICROPHONE,
            Port.Info.SPEAKER,
            Port.Info.LINE_IN,
            Port.Info.LINE_OUT,
            Port.Info.HEADPHONE,
            Port.Info.COMPACT_DISC
        };

        for (Port.Info portInfo : portInfos) {
            if (AudioSystem.isLineSupported(portInfo)) {
                portComboBox.addItem(portInfo);
            }
        }
    }

    // Method to display port controls in the text area
    private void displayPortControls(Port.Info portInfo) {
        controlTextArea.setText(""); // Clear previous text
        try {
            try (Port port = (Port) AudioSystem.getLine(portInfo)) {
                port.open();

                controlTextArea.append("Controls for: " + portInfo + "\n\n");

                for (Control control : port.getControls()) {
                    controlTextArea.append("Control: " + control + "\n");
                }
                updateControlsPanel(portInfo);
            }
        } catch (LineUnavailableException e) {
            controlTextArea.append("Error accessing " + portInfo + ": " + e.getMessage());
        }
    }

    private void updateControlsPanel(Port.Info portInfo) throws LineUnavailableException {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy = GridBagConstraints.LAST_LINE_END;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(3, 2, 2, 3); // Padding

        if (controlsContainer.getComponentCount() > 0) {
            controlsContainer.removeAll();
        }
        if (portInfo != null) {
            Control[] controls = AudioSystem.getLine(portInfo).getControls();

            for (Control control : controls) {
                addControlToPanel(control, controlPanel, gbc);
            }

            controlPanel.add(new JLabel("Line unavailable or no controls"), gbc);

            controlsContainer.add(controlPanel, BorderLayout.SOUTH);
            controlsContainer.revalidate();
            controlsContainer.repaint();
            add(controlsContainer, BorderLayout.SOUTH);
        }
    }

    // Helper method to add control components to the panel
    private void addControlToPanel(Control control, JPanel panel, GridBagConstraints gbc) {
        switch (control) {
            case BooleanControl booleanControl -> {
                JCheckBox checkBox = new JCheckBox(
                        "true".equals(
                                booleanControl.getStateLabel(booleanControl.getValue())
                        ) ? "Unmute" : "Mute",
                        booleanControl.getValue()
                );
                checkBox.addItemListener(e -> {
                    booleanControl.setValue(checkBox.isSelected());
                    checkBox.setText(booleanControl.getValue() ? "Unmute" : "Mute");
                    checkBox.revalidate();
                    checkBox.repaint();
                });
                checkBox.setBorder(
                        BorderFactory.createCompoundBorder(
                                BorderFactory.createTitledBorder(control.getType().toString()),
                                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)
                        )
                );
                checkBox.revalidate();
                checkBox.repaint();
                panel.add(checkBox, gbc);
            }
            case FloatControl floatControl -> {
                float floatMin = floatControl.getMinimum();
                float floatMax = floatControl.getMaximum();

                int intMin = 0;
                int intMax = Integer.MAX_VALUE;
                int initialIntValue = (int) ((floatControl.getValue() - floatMin) / (floatMax - floatMin) * (intMax - intMin) + intMin);
                JSlider slider = new JSlider(intMin, intMax, initialIntValue);

                // Create labels for slider using min, mid, and max labels from FloatControl
                Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
                labelTable.put(intMin, new JLabel(floatControl.getMinLabel()));
                labelTable.put(intMax, new JLabel(floatControl.getMaxLabel()));
                labelTable.put((intMax + intMin) / 2, new JLabel(floatControl.getMidLabel()));

                slider.setLabelTable(labelTable);
                slider.setPaintLabels(true);

                slider.addChangeListener((ChangeEvent e) -> {
                    float value = ((float) (slider.getValue() - intMin) / (intMax - intMin)) * (floatMax - floatMin) + floatMin;
                    floatControl.setValue(value);
                });
                slider.setBorder(
                        BorderFactory.createCompoundBorder(
                                BorderFactory.createTitledBorder(control.getType().toString()),
                                BorderFactory.createEmptyBorder(15, 2, 1, 2)
                        )
                );
                panel.add(slider, gbc);
            }
            case EnumControl enumControl -> {
                JComboBox<?> comboBox = new JComboBox<>(enumControl.getValues());
                comboBox.setSelectedItem(enumControl.getValue());
                comboBox.addActionListener(e -> enumControl.setValue(comboBox.getSelectedItem()));
                comboBox.setBorder(
                        BorderFactory.createCompoundBorder(
                                BorderFactory.createTitledBorder(control.getType().toString()),
                                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)
                        )
                );
                panel.add(new JLabel(enumControl.getType().toString()), gbc);
                panel.add(comboBox, gbc);
            }
            case CompoundControl compoundControl -> {
                JPanel compoundPanel = new JPanel();
                compoundPanel.setLayout(new GridLayout(0, 1));
                compoundPanel.setBorder(
                        BorderFactory.createCompoundBorder(
                                BorderFactory.createTitledBorder(control.getType().toString()),
                                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)
                        )
                );

                for (Control subControl : compoundControl.getMemberControls()) {
                    addControlToPanel(subControl, compoundPanel, gbc); // Recursive call for sub-controls
                }
                panel.add(compoundPanel, gbc);
            }
            default -> {
                panel.add(new JLabel("Unsupported control: " + control.getType().toString()), gbc);
            }
        }
    }

    public static void main(String[] args) {
        // Run the application on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            new PortInfoSwingApp().setVisible(true);
        });
    }
}
