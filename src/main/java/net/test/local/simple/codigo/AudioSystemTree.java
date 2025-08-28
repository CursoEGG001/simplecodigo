/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.BorderLayout;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author pc
 */


public class AudioSystemTree extends JFrame {

    public AudioSystemTree() {
        setTitle("Árbol del Sistema de Audio");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Sistema de Audio");

        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixers) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            DefaultMutableTreeNode mixerNode = new DefaultMutableTreeNode(mixerInfo.getName());

            // Líneas de entrada
            try {
                Line.Info[] targetLines = mixer.getTargetLineInfo();
                if (targetLines.length > 0) {
                    DefaultMutableTreeNode targetNode = new DefaultMutableTreeNode("Líneas de Entrada");
                    for (Line.Info lineInfo : targetLines) {
                        DefaultMutableTreeNode lineNode = new DefaultMutableTreeNode(lineInfo.toString());
                        addLineDetails(lineNode, mixer, lineInfo);
                        targetNode.add(lineNode);
                    }
                    mixerNode.add(targetNode);
                }
            } catch (Exception e) {
                // Ignorar errores
            }

            // Líneas de salida
            try {
                Line.Info[] sourceLines = mixer.getSourceLineInfo();
                if (sourceLines.length > 0) {
                    DefaultMutableTreeNode sourceNode = new DefaultMutableTreeNode("Líneas de Salida");
                    for (Line.Info lineInfo : sourceLines) {
                        DefaultMutableTreeNode lineNode = new DefaultMutableTreeNode(lineInfo.toString());
                        addLineDetails(lineNode, mixer, lineInfo);
                        sourceNode.add(lineNode);
                    }
                    mixerNode.add(sourceNode);
                }
            } catch (Exception e) {
                // Ignorar errores
            }

            root.add(mixerNode);
        }

        JTree tree = new JTree(root);
        JScrollPane scrollPane = new JScrollPane(tree);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void addLineDetails(DefaultMutableTreeNode lineNode, Mixer mixer, Line.Info lineInfo) {
        try {
            try (Line line = mixer.getLine(lineInfo)) {
                line.open();
                
                // Controles
                Control[] controls = line.getControls();
                if (controls.length > 0) {
                    DefaultMutableTreeNode controlsNode = new DefaultMutableTreeNode("Controles");
                    for (Control control : controls) {
                        controlsNode.add(new DefaultMutableTreeNode(control.toString()));
                    }
                    lineNode.add(controlsNode);
                }
                
                // Formatos de audio soportados
                if (line instanceof DataLine dataLine) {
                    AudioFormat[] formats = dataLine.getFormat().getClass().getDeclaredAnnotation(Deprecated.class) == null ?
                            new AudioFormat[]{dataLine.getFormat()} : new AudioFormat[0];
                    
                    DefaultMutableTreeNode formatsNode = new DefaultMutableTreeNode("Formatos de Audio");
                    for (AudioFormat format : formats) {
                        formatsNode.add(new DefaultMutableTreeNode(format.toString()));
                    }
                    lineNode.add(formatsNode);
                }
            }
        } catch (LineUnavailableException e) {
            lineNode.add(new DefaultMutableTreeNode("Error al abrir línea: " + e.getMessage()));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
                System.out.println("No se encontraron Look & feel");
            }
            new AudioSystemTree().setVisible(true);
        });
    }
}