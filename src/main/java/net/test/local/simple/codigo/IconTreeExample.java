/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.Component;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

/**
 *
 * @author pc
 */
public class IconTreeExample {

    public static void main(String[] args) {
        // Create a JFrame to display the components
        JFrame frame = new JFrame("Icon Tree");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create a JTree with a root node
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Root");
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        JTree tree = new JTree(treeModel);
        JScrollPane scrollPane = new JScrollPane(tree);

        // Create custom cell renderer to display icons
        MyTreeCellRenderer renderer = new MyTreeCellRenderer();
        tree.setCellRenderer(renderer);

        // Add components to the frame
        frame.add(scrollPane);

        // Set the frame size and make it visible
        frame.setSize(300, 200);
        frame.setVisible(true);

        // Add nodes with icons
        DefaultMutableTreeNode node1 = new DefaultMutableTreeNode("Node 1");
        rootNode.add(node1);
        node1.add(new DefaultMutableTreeNode(UIManager.getIcon("FileChooser.newFolderIcon")));
        node1.add(new DefaultMutableTreeNode(UIManager.getIcon("FileChooser.homeFolderIcon")));
        node1.add(new DefaultMutableTreeNode(UIManager.getIcon("FileChooser.hardDriveIcon")));
        rootNode.add(new DefaultMutableTreeNode(UIManager.getIcon("FileChooser.hardDriveIcon")));

        // Expand the root node
        treeModel.nodeStructureChanged(rootNode);
    }

    private static class MyTreeCellRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Component component = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();
                if (userObject instanceof ImageIcon) {
                    setIcon((ImageIcon) userObject);
                }
            }

            return component;
        }
    }
}
