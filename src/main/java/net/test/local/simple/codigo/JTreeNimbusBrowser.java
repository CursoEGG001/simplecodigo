/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

/**
 *
 * @author pc
 */
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.border.Border;

public class JTreeNimbusBrowser {

    private static final Set<String> NIMBUS_PRIMARY_COLORS = new HashSet<>(Arrays.asList(
            "text", "control", "nimbusBase", "nimbusOrange", "nimbusGreen", "nimbusRed", "nimbusInfoBlue",
            "nimbusAlertYellow", "nimbusFocus", "nimbusSelectedText", "nimbusSelectionBackground",
            "nimbusDisabledText", "nimbusLightBackground", "info"));
    private static final Set<String> NIMBUS_SECONDARY_COLORS = new HashSet<>(Arrays.asList(
            "textForeground", "textBackground", "background",
            "nimbusBlueGrey", "nimbusBorder", "nimbusSelection", "infoText", "menuText", "menu", "scrollbar",
            "controlText", "controlHighlight", "controlLHighlight", "controlShadow", "controlDkShadow", "textHighlight",
            "textHighlightText", "textInactiveText", "desktop", "activeCaption", "inactiveCaption"));
    private static final String[] NIMBUS_COMPONENTS = new String[]{
        "ArrowButton", "Button", "ToggleButton", "RadioButton", "CheckBox", "ColorChooser", "ComboBox",
        "\"ComboBox.scrollPane\"", "FileChooser", "InternalFrameTitlePane", "InternalFrame", "DesktopIcon",
        "DesktopPane", "Label", "List", "MenuBar", "MenuItem", "RadioButtonMenuItem", "CheckBoxMenuItem", "Menu",
        "PopupMenu", "PopupMenuSeparator", "OptionPane", "Panel", "ProgressBar", "Separator", "ScrollBar",
        "ScrollPane", "Viewport", "Slider", "Spinner", "SplitPane", "TabbedPane", "Table", "TableHeader",
        "\"Table.editor\"", "\"Tree.cellEditor\"", "TextField", "FormattedTextField", "PasswordField", "TextArea",
        "TextPane", "EditorPane", "ToolBar", "ToolBarSeparator", "ToolTip", "Tree", "RootPane"};

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            for (UIManager.LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(laf.getName())) {
                    try {
                        UIManager.setLookAndFeel(laf.getClassName());
                    } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
                        System.out.println("Look and Feel not ready: " + e.getMessage());
                    }
                }
            }

            // Get defaults
            UIDefaults defaults = UIManager.getLookAndFeelDefaults();

            // Split components
            Map<String, Map<String, Object>> componentDefaults = new HashMap<>();
            Map<String, Object> others = new HashMap<>();
            for (var keyObj : defaults.keySet()) {
                String key = keyObj.toString();
                boolean matchesComponent = false;
                componentloop:
                for (String componentName : NIMBUS_COMPONENTS) {
                    if (key.startsWith(componentName + ".") || key.startsWith(componentName + ":")
                            || key.startsWith(componentName + "[")) {
                        Map<String, Object> keys = componentDefaults.get(componentName);
                        if (keys == null) {
                            keys = new HashMap<>();
                            componentDefaults.put(componentName, keys);
                        }
                        keys.put(key, defaults.get(key));
                        matchesComponent = true;
                        break componentloop;
                    }
                }
                if (!matchesComponent) {
                    others.put(key, defaults.get(key));
                }
            }

            // Split out primary, secondary colors
            Map<String, Object> primaryColors = new HashMap<>();
            Map<String, Object> secondaryColors = new HashMap<>();
            for (Map.Entry<String, Object> entry : others.entrySet()) {
                if (NIMBUS_PRIMARY_COLORS.contains(entry.getKey())) {
                    primaryColors.put(entry.getKey(), (Color) entry.getValue());
                }
                if (NIMBUS_SECONDARY_COLORS.contains(entry.getKey())) {
                    secondaryColors.put(entry.getKey(), (Color) entry.getValue());
                }
            }
            for (String key : NIMBUS_PRIMARY_COLORS) {
                others.remove(key);
            }
            for (String key : NIMBUS_SECONDARY_COLORS) {
                others.remove(key);
            }

            // Split out UIs
            Map<String, Object> uiClasses = new HashMap<>();
            for (Map.Entry<String, Object> entry : others.entrySet()) {
                if (entry.getKey().endsWith("UI")) {
                    uiClasses.put(entry.getKey(), entry.getValue());
                }
            }
            for (String key : uiClasses.keySet()) {
                others.remove(key);
            }

            // Create JTree
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Nimbus Look and Feel Defaults");
            addToTree(root, "Primary Colors", primaryColors);
            addToTree(root, "Secondary Colors", secondaryColors);
            addToTree(root, "Components", componentDefaults);
            addToTree(root, "Others", others);
            addToTree(root, "UI Classes", uiClasses);

            JTree tree = new JTree(new DefaultTreeModel(root));
            JScrollPane scrollPane = new JScrollPane(tree);

            JFrame frame = new JFrame("Nimbus Look and Feel Browser");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(scrollPane, BorderLayout.CENTER);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    static <T> void addToTree(DefaultMutableTreeNode parent, String name, Map<String, T> map) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(name);
        parent.add(node);
        List<String> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(key + ": " + map.get(key));
            node.add(child);
            if (map.get(key) instanceof Map) {
                addToTree(child, key, (Map<String, T>) map.get(key));
            } else {
                addCustomNode(child, key, map.get(key));
            }
        }
    }

    static void addCustomNode(DefaultMutableTreeNode parent, String key, Object value) {
        if (value != null) {

            switch (value) {
                case Color color ->
                    parent.add(new DefaultMutableTreeNode(new ColorNode(key, color)));
                case Font font ->
                    parent.add(new DefaultMutableTreeNode(new FontNode(key, font)));
                case Dimension dimension ->
                    parent.add(new DefaultMutableTreeNode(new DimensionNode(key, dimension)));
                case Insets insets ->
                    parent.add(new DefaultMutableTreeNode(new InsetsNode(key, insets)));
                case Border border ->
                    parent.add(new DefaultMutableTreeNode(new BorderNode(key, border)));
                case Painter painter ->
                    parent.add(new DefaultMutableTreeNode(new PainterNode(key, painter)));
                case Icon icon ->
                    parent.add(new DefaultMutableTreeNode(new IconNode(key, icon)));
                default ->
                    parent.add(new DefaultMutableTreeNode(key + ": " + value));
            }

        } else {
            parent.add(new DefaultMutableTreeNode("Nothing to add..."));
        }
    }

    static class ColorNode {

        private final String key;
        private final Color color;

        public ColorNode(String key, Color color) {
            this.key = key;
            this.color = color;
        }

        @Override
        public String toString() {
            return key + ": #" + getWebColor(color) + " (R: " + color.getRed() + ",G: " + color.getGreen() + ",B: " + color.getBlue() + ")";
        }
    }

    static class FontNode {

        private final String key;
        private final Font font;

        public FontNode(String key, Font font) {
            this.key = key;
            this.font = font;
        }

        @Override
        public String toString() {
            String style = "";
            if (font.isBold() && font.isItalic()) {
                style = "Bold & Italic";
            } else if (font.isBold()) {
                style = "Bold";
            } else if (font.isItalic()) {
                style = "Italic";
            }
            return key + ": Font \"" + font.getFamily() + " " + font.getSize() + " " + style + "\"";
        }
    }

    static class DimensionNode {

        private final String key;
        private final Dimension dimension;

        public DimensionNode(String key, Dimension dimension) {
            this.key = key;
            this.dimension = dimension;
        }

        @Override
        public String toString() {
            return key + ": Dimension box (width:" + dimension.width + ",height:" + dimension.height + ")";
        }
    }

    static class InsetsNode {

        private final String key;
        private final Insets insets;

        public InsetsNode(String key, Insets insets) {
            this.key = key;
            this.insets = insets;
        }

        @Override
        public String toString() {
            return key + ": Insets (" + insets.top + "," + insets.left + "," + insets.bottom + "," + insets.right + ")";
        }
    }

    static class BorderNode {

        private final String key;
        private final Border border;

        public BorderNode(String key, Border border) {
            this.key = key;
            this.border = border;
        }

        @Override
        public String toString() {
            Insets insets = border.getBorderInsets(null);
            return key + ": Border Insets(" + insets.top + "," + insets.left + "," + insets.bottom + "," + insets.right + ")";
        }
    }

    static class PainterNode {

        private final String key;
        private final Painter painter;

        public PainterNode(String key, Painter painter) {
            this.key = key;
            this.painter = painter;
        }

        @Override
        public String toString() {
            return key + ": Painter -> " + painter.toString();
        }
    }

    static class IconNode {

        private final String key;
        private final Icon icon;

        public IconNode(String key, Icon icon) {
            this.key = key;
            this.icon = icon;
        }

        @Override
        public String toString() {
            return key + ": Icon " + icon.getIconWidth() + " x " + icon.getIconHeight();
        }
    }

    static String getWebColor(Color color) {
        String result = "";
        String num;
        num = Integer.toHexString(color.getRed());
        if (num.length() == 1) {
            num = "0" + num;
        }
        result += num;
        num = Integer.toHexString(color.getGreen());
        if (num.length() == 1) {
            num = "0" + num;
        }
        result += num;
        num = Integer.toHexString(color.getBlue());
        if (num.length() == 1) {
            num = "0" + num;
        }
        result += num;
        return result;
    }
}
