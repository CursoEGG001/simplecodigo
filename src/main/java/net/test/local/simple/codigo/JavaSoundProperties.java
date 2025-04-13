/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.CompoundControl;
import javax.sound.sampled.Control;
import javax.sound.sampled.EnumControl;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 *
 * @author pc
 */
public class JavaSoundProperties extends JFrame {

    private static final long serialVersionUID = 1L;

    private JTree tree;
    private JPanel detailsPanel;
    private final Map<DefaultMutableTreeNode, TreeNodeComponent> nodeComponentsMap;

    public JavaSoundProperties() {
        this.nodeComponentsMap = new HashMap<>();
        setTitle("Explorador de Audio Java Sound");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(800, 600));

        initializeUI();
        populateTree();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        JSplitPane separatedPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // Panel izquierdo con el árbol
        JPanel treePanel = new JPanel(new BorderLayout());
        tree = new JTree();
        tree.addTreeSelectionListener(e -> showDetails());
        tree.setShowsRootHandles(true);
        treePanel.add(new JScrollPane(tree), BorderLayout.CENTER);

        // Panel derecho con los detalles
        detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setBorder(BorderFactory.createTitledBorder("Detalles del Objeto"));

        separatedPane.setLeftComponent(treePanel);
        separatedPane.setRightComponent(detailsPanel);
        add(separatedPane);

    }

    private void populateTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Dispositivos de Audio");

        // Obtener información de todos los mezcladores
        Mixer.Info[] mixersInfo = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixersInfo) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            MixerNode mixerNode = new MixerNode(mixerInfo);
            DefaultMutableTreeNode mixerTreeNode = new DefaultMutableTreeNode(mixerNode);
            nodeComponentsMap.put(mixerTreeNode, mixerNode);

            // Agregar líneas de entrada
            DefaultMutableTreeNode sourceNode = new DefaultMutableTreeNode("Líneas de Entrada");
            for (Line.Info lineInfo : mixer.getSourceLineInfo()) {
                addLineToTree(sourceNode, lineInfo, mixer);
            }
            if (sourceNode.getChildCount() > 0) {
                mixerTreeNode.add(sourceNode);
            }

            // Agregar líneas de salida
            DefaultMutableTreeNode targetNode = new DefaultMutableTreeNode("Líneas de Salida");
            for (Line.Info lineInfo : mixer.getTargetLineInfo()) {
                addLineToTree(targetNode, lineInfo, mixer);
            }
            if (targetNode.getChildCount() > 0) {
                mixerTreeNode.add(targetNode);
            }

            root.add(mixerTreeNode);
        }

        tree.setModel(new DefaultTreeModel(root));
        expandAllNodes(tree);
    }

    private void addLineToTree(DefaultMutableTreeNode parentNode, Line.Info lineInfo, Mixer mixer) {
        LineNode lineNode = new LineNode(lineInfo);
        DefaultMutableTreeNode lineTreeNode = new DefaultMutableTreeNode(lineNode);
        nodeComponentsMap.put(lineTreeNode, lineNode);

        try {
            Line line = mixer.getLine(lineInfo);
            if (!line.isOpen() && !(line instanceof Clip)) {
                line.open();
            }

            // Agregar controles si están disponibles
            Control[] controls = line.getControls();
            if (controls.length > 0) {
                DefaultMutableTreeNode controlsNode = new DefaultMutableTreeNode("Controles");
                for (Control control : controls) {
                    switch (control) {
                        case BooleanControl bc -> {
                            DefaultMutableTreeNode booleanNode = new DefaultMutableTreeNode("BooleanControl: " + bc.getType());
                            nodeComponentsMap.put(booleanNode, new ControlNode(bc));
                            controlsNode.add(booleanNode);
                        }
                        case EnumControl ec -> {
                            DefaultMutableTreeNode enumNode = new DefaultMutableTreeNode("EnumControl: " + ec.getType());
                            nodeComponentsMap.put(enumNode, new ControlNode(ec));

                            for (Object value : ec.getValues()) {
                                DefaultMutableTreeNode valueNode = new DefaultMutableTreeNode(value.toString());
                                nodeComponentsMap.put(valueNode, new ControlNode(ec));
                                enumNode.add(valueNode);
                            }

                            controlsNode.add(enumNode);
                        }
                        case FloatControl fc -> {
                            DefaultMutableTreeNode floatNode = new DefaultMutableTreeNode("FloatControl: " + fc.getType());
                            nodeComponentsMap.put(floatNode, new ControlNode(fc));
                            controlsNode.add(floatNode);
                        }
                        case CompoundControl cc -> {
                            DefaultMutableTreeNode compoundNode = new DefaultMutableTreeNode("CompoundControl: " + cc.getType());
                            nodeComponentsMap.put(compoundNode, new ControlNode(cc));

                            for (Control childControl : cc.getMemberControls()) {
                                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(childControl.toString());
                                nodeComponentsMap.put(childNode, new ControlNode(childControl));
                                compoundNode.add(childNode);
                            }

                            controlsNode.add(compoundNode);
                        }
                        default -> {
                        }
                    }
                }
                lineTreeNode.add(controlsNode);
            }

            parentNode.add(lineTreeNode);
        } catch (LineUnavailableException e) {
            lineTreeNode.add(new DefaultMutableTreeNode("No disponible: " + e.getMessage()));
        }
    }

    private void expandAllNodes(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    private void showDetails() {
        detailsPanel.removeAll();
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

        if (selectedNode != null) {
            TreeNodeComponent nodeComponent = nodeComponentsMap.get(selectedNode);
            if (nodeComponent != null) {
                detailsPanel.add(nodeComponent.getDetailsPanel(), BorderLayout.CENTER);
            }
        }

        detailsPanel.revalidate();
        detailsPanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(JavaSoundProperties::new);
    }

    public abstract class TreeNodeComponent {

        public abstract JPanel getDetailsPanel();
    }

    public class ControlNode extends TreeNodeComponent {

        private final Control control;

        public ControlNode(Control control) {
            this.control = control;
        }

        @Override
        public JPanel getDetailsPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(5, 5, 5, 5);

            JLabel typeLabel = new JLabel("Tipo de Control: " + control.getType().toString() + " (" + control + ")");
            panel.add(typeLabel, gbc);

            buildControlPanel(panel, gbc, control);

            return panel;
        }

        private void buildControlPanel(JPanel panel, GridBagConstraints gbc, Control control) {
            switch (control) {
                case BooleanControl bc -> {
                    JCheckBox checkBox = new JCheckBox("Valor: " + bc.getValue());
                    checkBox.setEnabled(false);
                    gbc.gridy++;
                    panel.add(checkBox, gbc);
                }
                case FloatControl fc -> {
                    JLabel valueLabel = new JLabel("Valor: " + fc.getValue());
                    JLabel minLabel = new JLabel("Mínimo: " + fc.getMinimum());
                    JLabel maxLabel = new JLabel("Máximo: " + fc.getMaximum());
                    JLabel unitLabel = new JLabel("Unidad: " + fc.getUnits());
                    JSlider slider = new JSlider(
                            (int) (fc.getMinimum() * 100),
                            (int) (fc.getMaximum() * 100),
                            (int) (fc.getValue() * 100)
                    );
                    slider.setEnabled(false);

                    gbc.gridy++;
                    panel.add(valueLabel, gbc);
                    gbc.gridy++;
                    panel.add(minLabel, gbc);
                    gbc.gridy++;
                    panel.add(maxLabel, gbc);
                    gbc.gridy++;
                    panel.add(unitLabel, gbc);
                    gbc.gridy++;
                    panel.add(slider, gbc);
                }
                case EnumControl ec -> {
                    JLabel valueLabel = new JLabel("Valor: " + ec.getValue());
                    JLabel valuesLabel = new JLabel("Valores: " + Arrays.toString(ec.getValues()));

                    gbc.gridy++;
                    panel.add(valueLabel, gbc);
                    gbc.gridy++;
                    panel.add(valuesLabel, gbc);

                    // Manejar controles anidados en EnumControl
                    if (ec.getValue() instanceof Control nestedControl) {
                        JPanel nestedPanel = new JPanel(new GridBagLayout());
                        GridBagConstraints nestedGbc = new GridBagConstraints();
                        nestedGbc.gridx = 0;
                        nestedGbc.gridy = 0;
                        nestedGbc.weightx = 1;
                        nestedGbc.anchor = GridBagConstraints.WEST;
                        nestedGbc.insets = new Insets(5, 5, 5, 5);

                        Control nestedEnumControl = nestedControl;
                        buildControlPanel(nestedPanel, nestedGbc, nestedEnumControl);

                        gbc.gridy++;
                        panel.add(nestedPanel, gbc);
                    }
                }
                default -> {
                }
            }
        }

        @Override
        public String toString() {
            return control.getType().toString();
        }

    }

    public class LineNode extends TreeNodeComponent {

        private final Line.Info lineInfo;

        public LineNode(Line.Info lineInfo) {
            this.lineInfo = lineInfo;
        }

        @Override
        public JPanel getDetailsPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 0.5;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(5, 5, 5, 5);

            JLabel lineClassLabel = new JLabel("Clase de Línea: " + lineInfo.getLineClass().getName());
            JLabel portClassLabel = new JLabel("Puerto: "
                    + ((lineInfo instanceof Port.Info portInfo)
                            ? "Puerto " + portInfo
                            : "No hay info."));
            panel.add(portClassLabel, gbc);
            gbc.gridy++;
            panel.add(lineClassLabel, gbc);

            return panel;
        }

        @Override
        public String toString() {
            return lineInfo.toString();
        }
    }

    public class MixerNode extends TreeNodeComponent {

        private final Mixer.Info mixerInfo;

        public MixerNode(Mixer.Info mixerInfo) {
            this.mixerInfo = mixerInfo;
        }

        @Override
        public JPanel getDetailsPanel() {
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(5, 5, 5, 5);

            JLabel nameLabel = new JLabel("Nombre: " + mixerInfo.getName());
            JLabel descriptionLabel = new JLabel("Descripción: " + mixerInfo.getDescription());
            JLabel vendorLabel = new JLabel("Proveedor: " + mixerInfo.getVendor());
            JLabel versionLabel = new JLabel("Proveedor: " + mixerInfo.getVersion());

            panel.add(nameLabel, gbc);
            gbc.gridy++;
            panel.add(descriptionLabel, gbc);
            gbc.gridy++;
            panel.add(vendorLabel, gbc);
            gbc.gridx++;
            panel.add(versionLabel, gbc);

            return panel;
        }

        @Override
        public String toString() {
            return "Mezclador: " + mixerInfo.getName();
        }
    }
}
