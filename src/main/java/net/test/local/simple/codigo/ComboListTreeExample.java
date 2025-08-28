/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 *
 * @author pc
 */
public class ComboListTreeExample extends JFrame {

    private JComboBox<String> categoryCombo;
    private JList<String> itemList;
    private JTree itemTree;

    // Datos de ejemplo: Categorías -> Items -> Subelementos para el árbol
    private final Map<String, List<String>> categoryItems = new HashMap<>();
    private final Map<String, List<String>> itemDetails = new HashMap<>();

    public ComboListTreeExample() {
        initializeData();
        setupUI();
        populateInitialList();
    }

    private void initializeData() {
        // Configuración de categorías y sus elementos
        categoryItems.put("Frutas", Arrays.asList("Manzana", "Plátano", "Cereza"));
        categoryItems.put("Colores", Arrays.asList("Rojo", "Verde", "Azul"));
        categoryItems.put("Animales", Arrays.asList("Perro", "Gato", "Pájaro"));

        // Configuración de subelementos para el árbol
        itemDetails.put("Manzana", Arrays.asList("Gala", "Fuji", "Honeycrisp"));
        itemDetails.put("Plátano", Arrays.asList("Cavendish", "Plátano Macho"));
        itemDetails.put("Cereza", Arrays.asList("Bing", "Rainier"));

        itemDetails.put("Rojo", Arrays.asList("Carmesí", "Escarlata", "Rubí"));
        itemDetails.put("Verde", Arrays.asList("Esmeralda", "Lima", "Jade"));
        itemDetails.put("Azul", Arrays.asList("Cobalto", "Azur", "Añil"));

        itemDetails.put("Perro", Arrays.asList("Labrador", "Poodle", "Bulldog"));
        itemDetails.put("Gato", Arrays.asList("Siamés", "Persa", "Maine Coon"));
        itemDetails.put("Pájaro", Arrays.asList("Águila", "Gorrión", "Loro"));
    }

    private void setupUI() {
        setTitle("Ejemplo Combo - Lista - Árbol");
        setSize(700, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(Color.WHITE);

        // Panel superior con ComboBox
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Seleccione categoría: "));
        categoryCombo = new JComboBox<>(categoryItems.keySet().toArray(new String[0]));
        categoryCombo.setPreferredSize(new Dimension(200, 30));
        topPanel.add(categoryCombo);
        add(topPanel, BorderLayout.NORTH);

        // Panel central con lista de elementos
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createTitledBorder("Elementos disponibles"));
        itemList = new JList<>();
        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        itemList.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane listScrollPane = new JScrollPane(itemList);
        centerPanel.add(listScrollPane, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        // Panel derecho con árbol de detalles
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Detalles del elemento"));
        itemTree = new JTree();
        itemTree.setFont(new Font("Arial", Font.PLAIN, 12));
        JScrollPane treeScrollPane = new JScrollPane(itemTree);
        rightPanel.add(treeScrollPane, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        // Listeners
        categoryCombo.addActionListener(e -> updateItemList());
        itemList.addListSelectionListener(this::updateTree);
    }

    private void populateInitialList() {
        updateItemList();
        if (itemList.getModel().getSize() > 0) {
            itemList.setSelectedIndex(0);
        }
    }

    private void updateItemList() {
        String selectedCategory = (String) categoryCombo.getSelectedItem();
        DefaultListModel<String> listModel = new DefaultListModel<>();

        if (selectedCategory != null) {
            categoryItems.get(selectedCategory).forEach(listModel::addElement);
        }

        itemList.setModel(listModel);

        // Limpiar árbol cuando cambia la categoría
        itemTree.setModel(null);
    }

    private void updateTree(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }

        String selectedItem = itemList.getSelectedValue();
        if (selectedItem == null) {
            return;
        }

        // Crear modelo de árbol
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(selectedItem);
        List<String> details = itemDetails.get(selectedItem);

        if (details != null) {
            details.forEach(detail -> root.add(new DefaultMutableTreeNode(detail)));
        }

        itemTree.setModel(new DefaultTreeModel(root));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ComboListTreeExample example = new ComboListTreeExample();
            example.setVisible(true);
        });
    }
}
