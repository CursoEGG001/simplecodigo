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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.CompoundControl;
import javax.sound.sampled.Control;
import javax.sound.sampled.EnumControl;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import javax.sound.sampled.SourceDataLine;
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

    private JComboBox<PortWrapper> portComboBox;
    private final JTextArea controlTextArea;
    private final JButton refreshButton;
    private final JButton playButton;
    private final JPanel controlsContainer;
    private SourceDataLine line;
    private volatile boolean isPlaying;

    // Wrapper class to store Port.Info and its associated Mixer
    private static class PortWrapper {

        final Port.Info portInfo;
        final Mixer mixer;
        final boolean isOutput; // Now directly reflects portInfo.isSource()

        PortWrapper(Port.Info portInfo, Mixer mixer) { // Removed isOutput parameter
            this.portInfo = portInfo;
            this.mixer = mixer;
            this.isOutput = portInfo.isSource(); // Use built-in check
        }

        @Override
        public String toString() {
            return portInfo.getName() + " (" + mixer.getMixerInfo().getName() + ")"
                    + (isOutput ? " [Output]" : " [Input]");
        }
    }

    public PortInfoSwingApp() {
        setTitle("Java Sound API Port Selector");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        portComboBox = new JComboBox<>();
        controlTextArea = new JTextArea(10, 30);
        controlTextArea.setEditable(false);
        refreshButton = new JButton("Refresh Ports");
        playButton = new JButton("Play Test Tone");
        controlsContainer = new JPanel();

        setupUI();
        setupListeners();
        populatePorts();
        pack();
    }

    private void setupUI() {
        setLayout(new BorderLayout());
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Select Port:"));
        topPanel.add(portComboBox);
        topPanel.add(refreshButton);
        topPanel.add(playButton);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(controlTextArea), BorderLayout.CENTER);
        add(controlsContainer, BorderLayout.SOUTH);
    }

    private void setupListeners() {
        portComboBox.addActionListener(e -> {
            PortWrapper wrapper = (PortWrapper) portComboBox.getSelectedItem();
            if (wrapper != null) {
                displayPortControls(wrapper);
                updatePlayButtonState(wrapper);
            }
        });

        playButton.addActionListener(e -> {
            if (!isPlaying) {
                startPlayback();
                playButton.setText("Stop Test Tone");
            } else {
                stopPlayback();
                playButton.setText("Play Test Tone");
            }
        });

        refreshButton.addActionListener(e -> populatePorts());
    }

    private void populatePorts() {
        portComboBox.removeAllItems();
        List<PortWrapper> ports = new ArrayList<>();

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);

            // Process ports
            processLines(mixer.getSourceLineInfo(), mixer, ports); // No isOutput parameter
            processLines(mixer.getTargetLineInfo(), mixer, ports); // No isOutput parameter
        }

        if (!ports.isEmpty()) {
            ports.forEach(portComboBox::addItem);
            portComboBox.setSelectedIndex(0);
        } else {
            controlTextArea.setText("No audio ports found in the system.");
            playButton.setEnabled(false);
        }
    }

    private void processLines(Line.Info[] lines, Mixer mixer, List<PortWrapper> ports) { // Removed isOutput parameter
        for (Line.Info lineInfo : lines) {
            if (lineInfo instanceof Port.Info portInfo) {
                try {
                    Line testLine = AudioSystem.getMixer(mixer.getMixerInfo()).getLine(portInfo);
                    if (testLine != null) {
                        ports.add(new PortWrapper(portInfo, mixer)); // No manual isOutput
                        testLine.close();
                    }
                } catch (LineUnavailableException e) {
                    Logger.getLogger(PortInfoSwingApp.class.getName())
                            .log(Level.FINE, "Port unavailable: " + portInfo.getName(), e);
                }
            }
        }
    }

    private void startPlayback() {
        Thread playThread = new Thread(() -> {
            PortWrapper wrapper = (PortWrapper) portComboBox.getSelectedItem();
            if (wrapper == null || !wrapper.isOutput) {
                Logger.getLogger(PortInfoSwingApp.class.getName())
                        .log(Level.WARNING, "Invalid port selected for playback");
                return;
            }

            try {
                line = (SourceDataLine) AudioSystem.getMixer(wrapper.mixer.getMixerInfo()).getLine(wrapper.portInfo);
                line.open(); // Open with the line's default format
                line.start();

                AudioFormat format = line.getFormat();
                int sampleRate = (int) format.getSampleRate();
                int channels = format.getChannels();
                int sampleSizeInBytes = format.getSampleSizeInBits() / 8;
                boolean isBigEndian = format.isBigEndian();

                isPlaying = true;
                int bufferSize = 4096;
                byte[] buffer = new byte[bufferSize * channels * sampleSizeInBytes];
                double angle = 0;
                double frequency = 440; // A4 note

                while (isPlaying) {
                    for (int i = 0; i < buffer.length;) {
                        double sine = Math.sin(angle);
                        angle += 2 * Math.PI * frequency / sampleRate;
                        if (angle > 2 * Math.PI) {
                            angle -= 2 * Math.PI;
                        }
                        int sample = (int) (sine * 32767); // 16-bit range

                        // Write sample to all channels
                        for (int c = 0; c < channels; c++) {
                            if (sampleSizeInBytes == 2) {
                                if (isBigEndian) {
                                    buffer[i++] = (byte) (sample >>> 8);
                                    buffer[i++] = (byte) sample;
                                } else {
                                    buffer[i++] = (byte) sample;
                                    buffer[i++] = (byte) (sample >>> 8);
                                }
                            } else {
                                // Handle other sample sizes (e.g., 8-bit, 24-bit) if necessary
                                // This example assumes 16-bit for simplicity
                            }
                        }
                    }
                    line.write(buffer, 0, buffer.length);
                }

                line.drain();
                line.stop();
                line.close();
            } catch (LineUnavailableException ex) {
                Logger.getLogger(PortInfoSwingApp.class.getName())
                        .log(Level.SEVERE, "Error accessing audio line", ex);
            }
        });
        playThread.start();
    }

    private void updatePlayButtonState(PortWrapper wrapper) {
        boolean canPlay = wrapper != null && wrapper.isOutput;
        playButton.setEnabled(canPlay);
        playButton.setToolTipText(canPlay
                ? "Play test tone through this output"
                : "Test tone not available for input ports");
    }

    private void stopPlayback() {
        isPlaying = false;
        if (line != null) {
            line.stop();
            line.close();
        }
    }

    // Method to display port controls in the text area
    private void displayPortControls(PortWrapper portInfo) {
        controlTextArea.setText(""); // Clear previous text
        try {
            try (Port port = (Port) AudioSystem.getMixer(portInfo.mixer.getMixerInfo()).getLine(portInfo.portInfo)) {
                port.open();

                controlTextArea.append("Controls for: " + portInfo + "\n\n");

                for (Control control : port.getControls()) {
                    controlTextArea.append("Control: " + control + "\n");
                }
                updateControlsPanel(portInfo.portInfo);
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
