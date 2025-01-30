/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

/**
 *
 * @author pc
 */
public class AudioFormatExplorer extends JFrame {
    
    private final DefaultTreeModel treeModel;
    private final JTree tree;
    private final Set<AudioFormat> compatibleFormats = new HashSet<>();
    private final Map<AudioFormat, Integer> formatSupportCount = new HashMap<>(); // NEW

    public AudioFormatExplorer() {
        super("Audio Format Compatibility Explorer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 800);

        // Setup tree model
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Audio System");
        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);
        tree.setBorder(BorderFactory.createTitledBorder("Java Sound Report"));
        tree.setCellRenderer(new FormatCellRenderer());
        tree.setToolTipText("");

        // Build audio hierarchy and analyze formats
        buildAudioHierarchy(root);
        analyzeFormatCompatibility(); // NEW

        add(new JScrollPane(tree));
        setVisible(true);
    }

    // NEW METHOD: Analyze format compatibility across devices
    private void analyzeFormatCompatibility() {
        // Cast root node to DefaultMutableTreeNode
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();

        // Now we can use breadthFirstEnumeration()
        Enumeration<?> nodes = root.breadthFirstEnumeration();

        // Rest of the method remains the same...
        while (nodes.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
            if (node.getUserObject() instanceof AudioFormat format) {
                formatSupportCount.put(format, formatSupportCount.getOrDefault(format, 0) + 1);
            }
        }
        
        formatSupportCount.forEach((format, count) -> {
            if (count > 1) {
                compatibleFormats.add(format);
            }
        });
    }
    
    private void buildAudioHierarchy(DefaultMutableTreeNode root) {
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            DefaultMutableTreeNode mixerNode = createMixerNode(mixer);
            if (mixerNode.getChildCount() > 0) {
                root.add(mixerNode);
            }
        }
    }
    
    private DefaultMutableTreeNode createMixerNode(Mixer mixer) {
        DefaultMutableTreeNode mixerNode = new DefaultMutableTreeNode(
                mixer.getMixerInfo().getName()
        );
        processLines(mixerNode, mixer.getSourceLineInfo(), "Playback");
        processLines(mixerNode, mixer.getTargetLineInfo(), "Capture");
        return mixerNode;
    }
    
    private void processLines(DefaultMutableTreeNode parent, Line.Info[] lineInfos, String type) {
        DefaultMutableTreeNode typeNode = new DefaultMutableTreeNode(type);
        for (Line.Info lineInfo : lineInfos) {
            if (lineInfo instanceof DataLine.Info dataLineInfo) {
                typeNode.add(createLineNode(dataLineInfo));
            }
        }
        if (typeNode.getChildCount() > 0) {
            parent.add(typeNode);
        }
    }
    
    private DefaultMutableTreeNode createLineNode(DataLine.Info lineInfo) {
        DefaultMutableTreeNode lineNode = new DefaultMutableTreeNode(
                lineInfo.getLineClass().getSimpleName()
        );
        for (AudioFormat format : lineInfo.getFormats()) {
            lineNode.add(new DefaultMutableTreeNode(format));
        }
        return lineNode;
    }
    
    class FormatCellRenderer extends DefaultTreeCellRenderer {
        
        private final Color COMPATIBLE_COLOR = new Color(0, 128, 0);
        private final Font BOLD_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);
        
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            
            if (node.getUserObject() instanceof AudioFormat format) {
                if (compatibleFormats.contains(format)) {
                    setForeground(COMPATIBLE_COLOR);
                    setFont(BOLD_FONT);
                    setToolTipText("Supported by "
                            + formatSupportCount.get(format) + " devices");
                } else {
                    setForeground(Color.BLACK);
                    setFont(getFont().deriveFont(Font.PLAIN));
                    setToolTipText("Unique to this device");
                }
                
            }
            return this;
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AudioFormatExplorer());
    }
}
