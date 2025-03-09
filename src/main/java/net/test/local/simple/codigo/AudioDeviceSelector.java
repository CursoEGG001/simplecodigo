/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.EnumControl;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.event.TreeSelectionEvent;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import javax.swing.BorderFactory;

public class AudioDeviceSelector extends JFrame implements LineListener {

    private JSplitPane splitPane;
    private JTree deviceTree;
    private JPanel infoPanel;
    private final Map<Object, InfoPanel> panelCache = new HashMap<>();
    private Line currentLine;

    public AudioDeviceSelector() {
        super("Audio Device Explorer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1024, 768);

        initComponents();
        buildDeviceTree();

        setVisible(true);
    }

    private void initComponents() {

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(300);
        splitPane.setDividerSize(6);
        splitPane.setBorder(BorderFactory.createRaisedSoftBevelBorder());

        deviceTree = new JTree();
        deviceTree.addTreeSelectionListener(this::treeSelectionChanged);
        deviceTree.setBorder(BorderFactory.createRaisedSoftBevelBorder());

        infoPanel = new JPanel(new CardLayout());

        splitPane.setLeftComponent(new JScrollPane(deviceTree));
        splitPane.setRightComponent(new JScrollPane(infoPanel));

        add(splitPane);
    }

    private void buildDeviceTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Audio System");

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            DefaultMutableTreeNode mixerNode = new DefaultMutableTreeNode(mixerInfo);
            Mixer mixer = AudioSystem.getMixer(mixerInfo);

            // Add target and source lines
            for (Line.Info lineInfo : mixer.getTargetLineInfo()) {
                mixerNode.add(createLineNode(lineInfo));
            }
            for (Line.Info lineInfo : mixer.getSourceLineInfo()) {
                mixerNode.add(createLineNode(lineInfo));
            }

            root.add(mixerNode);
        }

        deviceTree.setModel(new DefaultTreeModel(root));
        expandAllNodes();
    }

    private DefaultMutableTreeNode createLineNode(Line.Info lineInfo) {
        DefaultMutableTreeNode lineNode = new DefaultMutableTreeNode(lineInfo);
        try {
            Line line = AudioSystem.getLine(lineInfo);
            if (line instanceof DataLine dataLine) {
                lineNode.add(new DefaultMutableTreeNode(dataLine.getFormat()));
            }
        } catch (LineUnavailableException e) {
            System.err.println("Line unavailable: " + e.getMessage());
        }
        return lineNode;
    }

    private void expandAllNodes() {
        for (int i = 0; i < deviceTree.getRowCount(); i++) {
            deviceTree.expandRow(i);
        }
    }

    private void treeSelectionChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) deviceTree.getLastSelectedPathComponent();
        if (node == null) {
            return;
        }

        Object userObject = node.getUserObject();
        InfoPanel panel = panelCache.computeIfAbsent(userObject, this::createInfoPanel);

        showInfoPanel(panel, userObject);
    }

    private InfoPanel createInfoPanel(Object context) {
        if (context instanceof Mixer.Info info) {
            return new MixerInfoPanel(info);
        } else if (context instanceof Line.Info info) {
            return new LineInfoPanel(info, this); // Pass AudioDeviceSelector instance
        } else if (context instanceof AudioFormat) {
            return new FormatInfoPanel();
        }
        return new DefaultInfoPanel();
    }

    private void showInfoPanel(InfoPanel panel, Object context) {
        infoPanel.removeAll();
        panel.update(context);
        infoPanel.add(panel);
        infoPanel.revalidate();
        infoPanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AudioDeviceSelector::new);
    }

    private abstract static class InfoPanel extends JPanel {

        public abstract void update(Object context);
    }

    private static class MixerInfoPanel extends InfoPanel {

        private final JTextArea infoArea = new JTextArea(10, 30);

        public MixerInfoPanel(Mixer.Info mixerInfo) {
            setLayout(new BorderLayout());
            add(new JScrollPane(infoArea), BorderLayout.CENTER);
        }

        @Override
        public void update(Object context) {
            if (context instanceof Mixer.Info info) {
                infoArea.setText(String.format(
                        "Mixer: %s\nVendor: %s\nVersion: %s\nDescription: %s",
                        info.getName(), info.getVendor(), info.getVersion(), info.getDescription()
                ));
            }
        }
    }

    private static final class LineInfoPanel extends InfoPanel {

        private final JTabbedPane tabs = new JTabbedPane();
        private final JPanel controlsPanel = new JPanel();
        private final AudioDeviceSelector parent; // Reference to parent

        public LineInfoPanel(Line.Info lineInfo, AudioDeviceSelector parent) {
            this.parent = parent; // Store parent reference
            setLayout(new BorderLayout());
            add(tabs, BorderLayout.CENTER);
            add(controlsPanel, BorderLayout.SOUTH);
            update(lineInfo);
        }

        @Override
        public void update(Object context) {
            tabs.removeAll();
            controlsPanel.removeAll();

            if (context instanceof Line.Info info) {
                tabs.addTab("General", new JLabel("Propiedades generales de la línea"));

                try {
                    Line line = AudioSystem.getLine(info);
                    parent.currentLine = line; // Store current line in parent
                    line.addLineListener(parent); // Add listener from parent
                    line.open(); // **ABRIR LA LÍNEA AQUÍ**

                    addControls(line.getControls(), line); // Generalized controls display
                    tabs.addTab("Controles", new JScrollPane(controlsPanel)); // Add controls panel as tab

                } catch (LineUnavailableException e) {
                    controlsPanel.add(new JLabel("Línea no disponible: " + e.getMessage()));
                }
            }
        }

        private void addControls(Control[] controls, Line line) {
            JPanel apiControlPanel = new JPanel(new GridLayout(0, 2));
            if (controls != null) { // Check if controls array is not null
                for (Control control : controls) {
                    switch (control) {
                        case FloatControl floatControl -> {
                            JLabel controlLabel = new JLabel(floatControl.getType().toString());

                            JSlider controlSlider = new JSlider(
                                    (int) floatControl.getMinimum(),
                                    (int) floatControl.getMaximum(),
                                    (int) floatControl.getValue()
                            );
                            controlSlider.setEnabled(false);

                            controlSlider.setPaintLabels(true);
                            controlSlider.setPaintTicks(true);
                            controlSlider.setMajorTickSpacing((int) ((floatControl.getMaximum() - floatControl.getMinimum()) / 2));
                            controlSlider.setMinorTickSpacing((int) ((floatControl.getMaximum() - floatControl.getMinimum()) / 10));

                            Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
                            labelTable.put((int) floatControl.getMinimum(), new JLabel(floatControl.getMinLabel()));
                            labelTable.put((int) (floatControl.getMinimum() + (floatControl.getMaximum() - floatControl.getMinimum()) / 2), new JLabel(floatControl.getMidLabel()));
                            labelTable.put((int) floatControl.getMaximum(), new JLabel(floatControl.getMaxLabel()));
                            controlSlider.setLabelTable(labelTable);

                            String units = floatControl.getUnits();
                            float precision = floatControl.getPrecision();
                            JLabel unitsLabel = new JLabel("Unidades: " + units + ", Precisión: " + precision);
                            controlSlider.setToolTipText("Valor actual: " + floatControl.getValue());

                            apiControlPanel.add(controlLabel);
                            apiControlPanel.add(controlSlider);
                            apiControlPanel.add(unitsLabel);

                        }
                        case BooleanControl booleanControl -> {
                            JCheckBox controlCheckBox = new JCheckBox(
                                    booleanControl.getType().toString(),
                                    booleanControl.getValue()
                            );
                            controlCheckBox.setEnabled(false);
                            controlCheckBox.setToolTipText("Estado actual: " + booleanControl.getValue());
                            apiControlPanel.add(controlCheckBox);

                        }
                        case EnumControl enumControl -> {
                            JComboBox<Object> controlComboBox = new JComboBox<>(enumControl.getValues());
                            controlComboBox.setSelectedItem(enumControl.getValue());
                            controlComboBox.setEnabled(false);
                            controlComboBox.setToolTipText("Valor seleccionado: " + enumControl.getValue());
                            apiControlPanel.add(new JLabel(enumControl.getType().toString()));
                            apiControlPanel.add(controlComboBox);
                        }
                        default -> {
                            apiControlPanel.add(new JLabel(control.getType().toString())); // Default label for unknown controls
                            apiControlPanel.add(new JLabel(control.toString())); // Default value display
                        }
                    }
                }
            } else {
                apiControlPanel.add(new JLabel("No hay controles disponibles para esta línea."));
            }

            if (apiControlPanel.getComponentCount() > 0) {
                controlsPanel.add(new JScrollPane(apiControlPanel));
            } else {
                controlsPanel.add(new JLabel("No se encontraron controles API estándar."));
            }
        }
    }

    private static class FormatInfoPanel extends InfoPanel {

        private final JTextArea formatDetails = new JTextArea(10, 30);

        public FormatInfoPanel() {
            setLayout(new BorderLayout());
            formatDetails.setEditable(false);
            add(new JScrollPane(formatDetails), BorderLayout.CENTER);
        }

        @Override
        public void update(Object context) {
            if (context instanceof AudioFormat format) {
                String details = String.format("""
                                            Formato de Audio:
                                            Tipo de Codificación: %s
                                            Tasa de Muestreo: %.1f Hz
                                            Bits por Muestra: %d
                                            Canales: %d
                                            Tamaño de Trama: %d bytes
                                            Tasa de Trama: %.1f frames/s
                                            Big-Endian: %b""",
                        format.getEncoding(),
                        format.getSampleRate(),
                        format.getSampleSizeInBits(),
                        format.getChannels(),
                        format.getFrameSize(),
                        format.getFrameRate(),
                        format.isBigEndian()
                );
                formatDetails.setText(details);
            }
        }
    }

    private static class DefaultInfoPanel extends InfoPanel {

        private final JLabel defaultLabel = new JLabel("Seleccione un elemento para ver detalles");

        public DefaultInfoPanel() {
            setLayout(new BorderLayout());
            defaultLabel.setHorizontalAlignment(SwingConstants.CENTER);
            add(defaultLabel, BorderLayout.CENTER);
        }

        @Override
        public void update(Object context) {
            // No implementation needed
        }
    }

    @Override
    public void update(LineEvent event) {
        SwingUtilities.invokeLater(() -> {
            Line line = event.getLine();
            Line.Info info = line.getLineInfo();

            if (line == currentLine) {
                InfoPanel panel = panelCache.get(info);
                if (panel instanceof LineInfoPanel lineInfoPanel) {
                    lineInfoPanel.update(info); // Refresh the line info panel on event, if needed.
                    showInfoPanel(panel, info); // Re-show, although update might be enough.
                }
            }

            String message = String.format(
                    "Evento: %s\nTiempo: %d\nEstado: %s",
                    event.getType(),
                    event.getFramePosition(),
                    line.isOpen() ? "Abierto" : "Cerrado"
            );

            JOptionPane.showMessageDialog(this, message, "Evento de Línea", JOptionPane.INFORMATION_MESSAGE);
        });
    }
}
