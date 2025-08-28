package net.test.local.simple.codigo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.CompoundControl;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import javax.swing.Timer;

import java.util.List;

/**
 * AudioSystemExplorer - A comprehensive GUI application that demonstrates advanced integration between Java Sound API and
 * Swing data models with dynamic event-driven updates for audio system exploration and control.
 */
public class AudioSystemExplorer extends JFrame {

    // Primary data models for the interface components
    private DefaultComboBoxModel<DeviceWrapper> deviceComboModel;
    private DefaultListModel<LineWrapper> lineListModel;
    private DefaultTreeModel controlTreeModel;
    private DefaultComboBoxModel<AudioFormatWrapper> formatComboModel;

    // UI Components
    private JComboBox<DeviceWrapper> deviceCombo;
    private JList<LineWrapper> lineList;
    private JTree controlTree;
    private JComboBox<AudioFormatWrapper> formatCombo;
    private JPanel controlPanel;
    private JTextArea infoArea;
    private JButton openLineButton;
    private JButton closeLineButton;
    private JButton testAudioButton;

    // Audio management objects
    private Line currentLine;
    private Mixer currentMixer;
    private Map<Line, List<Control>> lineControlsMap;
    private AudioPropertyChangeSupport propertySupport;

    // Status and monitoring
    private JLabel statusLabel;
    private JProgressBar levelMeter;
    private Timer statusUpdateTimer;

    public AudioSystemExplorer() {
        initializeModels();
        setupUserInterface();
        loadAudioSystemData();
        configureEventHandlers();
        initializePropertySupport();
        startStatusMonitoring();
    }

    /**
     * Initializes all data models used by the Swing components.
     */
    private void initializeModels() {
        deviceComboModel = new DefaultComboBoxModel<>();
        lineListModel = new DefaultListModel<>();
        formatComboModel = new DefaultComboBoxModel<>();
        lineControlsMap = new HashMap<>();
        propertySupport = new AudioPropertyChangeSupport(this);

        // Initialize tree model with root node
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Audio Controls");
        controlTreeModel = new DefaultTreeModel(rootNode);
    }

    /**
     * Sets up the complete user interface layout.
     */
    private void setupUserInterface() {
        setTitle("AudioSystemExplorer - Advanced Java Sound API Interface");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create main panels
        add(createSelectionPanel(), BorderLayout.NORTH);
        add(createMainDisplayPanel(), BorderLayout.CENTER);
        add(createControlActionPanel(), BorderLayout.SOUTH);

        setSize(900, 700);
        setLocationRelativeTo(null);
    }

    /**
     * Creates the device and format selection panel.
     */
    private JPanel createSelectionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        panel.setBorder(BorderFactory.createTitledBorder("AudioSystemExplorer - Device and Format Selection"));

        // Device selection
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        panel.add(new JLabel("Audio Device:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        deviceCombo = new JComboBox<>(deviceComboModel);
        deviceCombo.setPreferredSize(new Dimension(300, 25));
        panel.add(deviceCombo, gbc);

        // Format selection
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Audio Format:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formatCombo = new JComboBox<>(formatComboModel);
        formatCombo.setPreferredSize(new Dimension(300, 25));
        panel.add(formatCombo, gbc);

        // Refresh button
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JButton refreshButton = new JButton("Refresh System");
        refreshButton.addActionListener(e -> refreshAudioSystem());
        panel.add(refreshButton, gbc);

        return panel;
    }

    /**
     * Creates the main display panel with lines, controls, and information.
     */
    private JPanel createMainDisplayPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Create split pane for lines and controls
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setResizeWeight(0.3);

        // Left panel: Lines list
        JPanel leftPanel = createLinesPanel();
        mainSplitPane.setLeftComponent(leftPanel);

        // Right panel: Controls and info
        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplitPane.setResizeWeight(0.6);
        rightSplitPane.setTopComponent(createControlsPanel());
        rightSplitPane.setBottomComponent(createInfoPanel());

        mainSplitPane.setRightComponent(rightSplitPane);
        panel.add(mainSplitPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Creates the audio lines list panel.
     */
    private JPanel createLinesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Available Audio Lines"));

        lineList = new JList<>(lineListModel);
        lineList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lineList.setCellRenderer(new LineListCellRenderer());

        JScrollPane scrollPane = new JScrollPane(lineList);
        scrollPane.setPreferredSize(new Dimension(250, 200));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Creates the audio controls tree panel.
     */
    private JPanel createControlsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Audio Line Controls"));

        controlTree = new JTree(controlTreeModel);
        controlTree.setRootVisible(true);
        controlTree.setCellRenderer(new ControlTreeCellRenderer());

        // Create control manipulation panel
        controlPanel = new JPanel(new FlowLayout());

        JScrollPane treeScrollPane = new JScrollPane(controlTree);
        treeScrollPane.setPreferredSize(new Dimension(400, 200));

        panel.add(treeScrollPane, BorderLayout.CENTER);
        panel.add(controlPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Creates the information display panel.
     */
    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Line Information"));

        infoArea = new JTextArea(6, 40);
        infoArea.setEditable(false);
        infoArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(infoArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Creates the control action panel with buttons and status.
     */
    private JPanel createControlActionPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());

        openLineButton = new JButton("Open Line");
        closeLineButton = new JButton("Close Line");
        testAudioButton = new JButton("Test Audio");

        buttonPanel.add(openLineButton);
        buttonPanel.add(closeLineButton);
        buttonPanel.add(testAudioButton);

        // Status panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Ready - Select a device to begin");
        levelMeter = new JProgressBar(0, 100);
        levelMeter.setStringPainted(true);
        levelMeter.setString("Audio Level");

        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.add(levelMeter, BorderLayout.EAST);

        panel.add(buttonPanel, BorderLayout.CENTER);
        panel.add(statusPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Loads all available audio system data into the models.
     */
    private void loadAudioSystemData() {
        loadAudioDevices();
        loadStandardFormats();
    }

    /**
     * Loads all available audio devices into the device combo box model.
     */
    private void loadAudioDevices() {
        deviceComboModel.removeAllElements();

        // Add default mixer first
        try {
            Mixer defaultMixer = AudioSystem.getMixer(null);
            deviceComboModel.addElement(new DeviceWrapper(defaultMixer, "Default System Mixer"));
        } catch (Exception e) {
            System.err.println("Could not access default mixer");
        }

        // Add all available mixers
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixerInfos) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                deviceComboModel.addElement(new DeviceWrapper(mixer, mixerInfo.getName()));
                propertySupport.firePropertyChange("deviceAdded", null, mixer);
            } catch (Exception e) {
                System.err.println("Error loading mixer: " + mixerInfo.getName());
            }
        }
    }

    /**
     * Loads standard audio formats into the format combo box model. This method now attempts to derive compatible formats
     * based on the currently selected line's capabilities, falling back to standard formats if no line is selected or if
     * line-specific formats cannot be determined.
     */
    private void loadStandardFormats() {
        formatComboModel.removeAllElements();

        // Standard formats as fallback/default
        AudioFormat[] standardFormats = {
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0f, 16, 2, 4, 44100.0f, true),
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000.0f, 16, 2, 4, 48000.0f, false),
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 22050.0f, 16, 1, 2, 22050.0f, false),
            new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000.0f, 8, 1, 1, 16000.0f, true),
            new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, 8000.0f, 8, 1, 1, 8000.0f, false)
        };

        LineWrapper selectedLine = lineList.getSelectedValue();

        // If no line is selected, load standard formats as initial options
        if (selectedLine == null) {
            for (AudioFormat format : standardFormats) {
                formatComboModel.addElement(new AudioFormatWrapper(format));
            }
            return;
        }

        // If a line is selected, try to get its supported formats
        Line.Info lineInfo = selectedLine.getLineInfo();

        if (lineInfo instanceof DataLine.Info dataLineInfo) {
            AudioFormat[] supportedFormats = dataLineInfo.getFormats();

            // If the line reports specific supported formats, use/adapt them
            if (supportedFormats.length > 0) {
                Set<AudioFormatWrapper> compatibleFormats = new LinkedHashSet<>(); // Use Set to avoid duplicates

                for (AudioFormat supportedFormat : supportedFormats) {
                    // Add the format as reported by the system directly
                    compatibleFormats.add(new AudioFormatWrapper(supportedFormat));

                    // Try to "complete" formats with NOT_SPECIFIED values using standard formats as templates
                    if (supportedFormat.getSampleRate() == AudioSystem.NOT_SPECIFIED
                            || supportedFormat.getFrameRate() == AudioSystem.NOT_SPECIFIED) {

                        for (AudioFormat standard : standardFormats) {
                            // Check if the core properties match (encoding, bits, channels)
                            // This is a basic check; more sophisticated matching could be implemented
                            if (supportedFormat.getEncoding().equals(standard.getEncoding())
                                    && supportedFormat.getSampleSizeInBits() == standard.getSampleSizeInBits()
                                    && supportedFormat.getChannels() == standard.getChannels()) {

                                // Create a new format filling in the missing values from the standard format
                                float sampleRate = (supportedFormat.getSampleRate() == AudioSystem.NOT_SPECIFIED)
                                        ? standard.getSampleRate() : supportedFormat.getSampleRate();
                                float frameRate = (supportedFormat.getFrameRate() == AudioSystem.NOT_SPECIFIED)
                                        ? standard.getFrameRate() : supportedFormat.getFrameRate();

                                // Calculate frame size if needed (simplified calculation)
                                int frameSize = supportedFormat.getFrameSize();
                                if (frameSize == AudioSystem.NOT_SPECIFIED) {
                                    frameSize = (standard.getSampleSizeInBits() / 8) * standard.getChannels();
                                }

                                boolean bigEndian = supportedFormat.isBigEndian(); // Prefer line's endianness if specified

                                AudioFormat correctedFormat = new AudioFormat(
                                        supportedFormat.getEncoding(),
                                        sampleRate,
                                        supportedFormat.getSampleSizeInBits(),
                                        supportedFormat.getChannels(),
                                        frameSize,
                                        frameRate,
                                        bigEndian
                                );
                                compatibleFormats.add(new AudioFormatWrapper(correctedFormat));
                            }
                        }
                    }
                }

                // Add the derived/compatible formats to the model
                for (AudioFormatWrapper formatWrapper : compatibleFormats) {
                    formatComboModel.addElement(formatWrapper);
                }

                // If we derived specific formats, we might stop here.
                // Alternatively, add standard formats too if the list is empty or as additional options.
                // Adding them might be useful if the derived list is empty or if standards are also likely to work.
                // Let's add them if the derived list is empty as a fallback.
                if (compatibleFormats.isEmpty()) {
                    for (AudioFormat format : standardFormats) {
                        formatComboModel.addElement(new AudioFormatWrapper(format));
                    }
                }

            } else {
                // Line is a DataLine but reports no specific formats, fall back to standards
                for (AudioFormat format : standardFormats) {
                    formatComboModel.addElement(new AudioFormatWrapper(format));
                }
            }
        } else {
            // If the selected line is not a DataLine (e.g., Port, Clip), 
            // standard formats might still be relevant for *opening* other DataLines,
            // or for general reference. Load standard formats.
            for (AudioFormat format : standardFormats) {
                formatComboModel.addElement(new AudioFormatWrapper(format));
            }
        }
    }

    /**
     * Configures all event handlers for the interface components.
     */
    private void configureEventHandlers() {
        // Device selection handler
        deviceCombo.addActionListener(e -> handleDeviceSelection());

        // Line selection handler
        lineList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                handleLineSelection();
            }
        });

        // Control tree selection handler
        controlTree.addTreeSelectionListener(e -> handleControlSelection());

        // Format selection handler
        formatCombo.addActionListener(e -> handleFormatSelection());

        // Button handlers
        openLineButton.addActionListener(e -> openSelectedLine());
        closeLineButton.addActionListener(e -> closeCurrentLine());
        testAudioButton.addActionListener(e -> testAudioFunctionality());

        // Mouse handlers for interactive control manipulation
        controlTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleControlDoubleClick();
                }
            }
        });
    }

    /**
     * Initializes property change support for audio events.
     */
    private void initializePropertySupport() {
        propertySupport.addPropertyChangeListener("lineOpened", evt -> {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Line opened: " + evt.getNewValue());
                updateControlsForOpenLine();
            });
        });

        propertySupport.addPropertyChangeListener("lineClosed", evt -> {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Line closed: " + evt.getOldValue());
                clearControlsDisplay();
            });
        });

        propertySupport.addPropertyChangeListener("controlChanged", evt -> {
            SwingUtilities.invokeLater(() -> {
                updateInfoDisplay();
            });
        });
    }

    /**
     * Starts the status monitoring timer.
     */
    private void startStatusMonitoring() {
        statusUpdateTimer = new Timer(200, e -> updateStatusDisplay());
        statusUpdateTimer.start();
    }

    /**
     * Handles device selection and updates the lines list model.
     */
    private void handleDeviceSelection() {
        DeviceWrapper selectedDevice = (DeviceWrapper) deviceCombo.getSelectedItem();
        if (selectedDevice != null) {
            currentMixer = selectedDevice.getMixer();
            updateLinesForDevice(currentMixer);
            propertySupport.firePropertyChange("deviceSelected", null, currentMixer);
            statusLabel.setText("Device selected: " + selectedDevice.getName());
        }
    }

    /**
     * Updates the lines list model based on the selected device.
     */
    private void updateLinesForDevice(Mixer mixer) {
        lineListModel.clear();
        lineControlsMap.clear();

        if (mixer == null) {
            return;
        }

        try {
            // Add source lines (for playback)
            Line.Info[] sourceLineInfos = mixer.getSourceLineInfo();
            for (Line.Info lineInfo : sourceLineInfos) {
                switch (lineInfo) {
                    case DataLine.Info dataLineInfo -> {
                        LineWrapper wrapper = new LineWrapper(dataLineInfo, "Source", mixer);
                        lineListModel.addElement(wrapper);
                    }
                    case Port.Info portInfo -> {
                        LineWrapper wrapper = new LineWrapper(portInfo, "Source Port", mixer);
                        lineListModel.addElement(wrapper);
                    }
                    default -> {
                    }
                }
            }

            // Add target lines (for recording)
            Line.Info[] targetLineInfos = mixer.getTargetLineInfo();
            for (Line.Info lineInfo : targetLineInfos) {
                switch (lineInfo) {
                    case DataLine.Info dataLineInfo -> {
                        LineWrapper wrapper = new LineWrapper(dataLineInfo, "Target", mixer);
                        lineListModel.addElement(wrapper);
                    }
                    case Port.Info portInfo -> {
                        LineWrapper wrapper = new LineWrapper(portInfo, "Target Port", mixer);
                        lineListModel.addElement(wrapper);
                    }
                    default -> {
                    }
                }
            }

            // Add Clip lines separately (they don't appear in regular source/target queries)
            addClipLinesToModel(mixer);

        } catch (Exception e) {
            statusLabel.setText("Error loading lines: " + e.getMessage());
        }
    }

    /**
     * Adds Clip lines to the model by querying for Clip support.
     */
    private void addClipLinesToModel(Mixer mixer) {
        try {
            // Check if the mixer supports Clips
            Line.Info clipInfo = new Line.Info(Clip.class);
            if (mixer.isLineSupported(clipInfo)) {
                LineWrapper wrapper = new LineWrapper(clipInfo, "Clip", mixer);
                lineListModel.addElement(wrapper);
            }

            // Also check for DataLine.Info that might represent Clips
            AudioFormat[] commonFormats = {
                new AudioFormat(44100.0f, 16, 2, true, false),
                new AudioFormat(22050.0f, 16, 1, true, false)
            };

            for (AudioFormat format : commonFormats) {
                DataLine.Info clipDataInfo = new DataLine.Info(Clip.class, format);
                if (mixer.isLineSupported(clipDataInfo)) {
                    LineWrapper wrapper = new LineWrapper(clipDataInfo, "Clip (DataLine)", mixer);
                    lineListModel.addElement(wrapper);
                    break; // Only add one example to avoid duplicates
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking for Clip support: " + e.getMessage());
        }
    }

    /**
     * Handles line selection and updates the controls tree model.
     */
    private void handleLineSelection() {
        LineWrapper selectedLine = lineList.getSelectedValue();
        if (selectedLine != null) {
            updateControlsForLine(selectedLine);
            updateInfoDisplay();
            propertySupport.firePropertyChange("lineSelected", null, selectedLine);
        }
    }

    /**
     * Updates the controls tree model for the selected line.
     */
    private void updateControlsForLine(LineWrapper lineWrapper) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) controlTreeModel.getRoot();
        root.removeAllChildren();

        try {
            // Attempt to get or open the line to access its controls
            Line line = getOrOpenLine(lineWrapper);
            if (line != null) {
                Control[] controls = line.getControls();
                List<Control> controlList = Arrays.asList(controls);
                lineControlsMap.put(line, controlList);

                // Build tree structure for controls
                buildControlsTree(root, controls);
                controlTreeModel.reload();
                expandControlTree();
            }
        } catch (LineUnavailableException e) {
            statusLabel.setText("Cannot access line controls: " + e.getMessage());
        }
    }

    /**
     * Gets or opens a line to access its controls.
     */
    private Line getOrOpenLine(LineWrapper lineWrapper) throws LineUnavailableException {
        Line.Info lineInfo = lineWrapper.getLineInfo();

        // Handle Port lines - they don't need to be opened to access controls
        if (lineInfo instanceof Port.Info) {
            try {
                Port port = (Port) lineWrapper.getMixer().getLine(lineInfo);
                return port; // Ports don't need to be opened
            } catch (LineUnavailableException e) {
                throw new LineUnavailableException("Cannot access port: " + e.getMessage());
            }
        }

        // Handle Clip lines - create but don't open them
        if (lineInfo.getLineClass() == Clip.class) {
            try {
                Clip clip = (Clip) lineWrapper.getMixer().getLine(lineInfo);
                // DON'T open the clip - just return it to access controls
                return clip;
            } catch (LineUnavailableException e) {
                throw new LineUnavailableException("Cannot access clip: " + e.getMessage());
            }
        }

        // First try to get the line without opening it
        try {
            Line line = lineWrapper.getMixer().getLine(lineWrapper.getLineInfo());
            if (line.isOpen()) {
                return line;
            }
        } catch (LineUnavailableException e) {
            // Line might not be available yet
        }

        // Handle DataLine (SourceDataLine, TargetDataLine) - these need to be opened
        if (lineWrapper.getLineInfo() instanceof DataLine.Info dataLineInfo) {

            // Skip opening if it's a Clip DataLine.Info
            if (dataLineInfo.getLineClass() == Clip.class) {
                try {
                    return lineWrapper.getMixer().getLine(dataLineInfo);
                } catch (LineUnavailableException e) {
                    throw new LineUnavailableException("Cannot access clip data line: " + e.getMessage());
                }
            }

            AudioFormat[] supportedFormats = dataLineInfo.getFormats();

            if (supportedFormats.length > 0) {
                try {
                    DataLine dataLine = (DataLine) lineWrapper.getMixer().getLine(dataLineInfo);

                    // Find a compatible format
                    AudioFormat compatibleFormat = findCompatibleFormat(dataLineInfo);
                    if (compatibleFormat != null) {
                        dataLine.open();
                        return dataLine;
                    }
                } catch (LineUnavailableException e) {
                    System.err.println("Could not open data line: " + e.getMessage());
                }
            }
        }

        return null;
    }

    /**
     * Finds a compatible audio format for a data line.
     */
    private AudioFormat findCompatibleFormat(DataLine.Info dataLineInfo) {
        AudioFormat[] supportedFormats = dataLineInfo.getFormats();

        // First try the currently selected format from the combo box
        AudioFormatWrapper selectedWrapper = (AudioFormatWrapper) formatCombo.getSelectedItem();
        if (selectedWrapper != null) {
            AudioFormat selectedFormat = selectedWrapper.getFormat();
            for (AudioFormat supported : supportedFormats) {
                if (isFormatCompatible(selectedFormat, supported)) {
                    return selectedFormat;
                }
            }
        }

        // If no exact match, return the first supported format
        return supportedFormats.length > 0 ? supportedFormats[0] : null;
    }

    /**
     * Checks if two audio formats are compatible.
     */
    private boolean isFormatCompatible(AudioFormat format1, AudioFormat format2) {
        return format1.getSampleRate() == format2.getSampleRate()
                && format1.getSampleSizeInBits() == format2.getSampleSizeInBits()
                && format1.getChannels() == format2.getChannels()
                && format1.getEncoding().equals(format2.getEncoding());
    }

    /**
     * Builds the controls tree structure.
     */
    private void buildControlsTree(DefaultMutableTreeNode root, Control[] controls) {
        Map<Control.Type, DefaultMutableTreeNode> typeNodes = new HashMap<>();

        for (Control control : controls) {
            Control.Type type = control.getType();

            // Create type node if it doesn't exist
            DefaultMutableTreeNode typeNode = typeNodes.get(type);
            if (typeNode == null) {
                typeNode = new DefaultMutableTreeNode(type.toString());
                typeNodes.put(type, typeNode);
                root.add(typeNode);
            }

            // Create control node
            ControlNodeWrapper controlWrapper = new ControlNodeWrapper(control);
            DefaultMutableTreeNode controlNode = new DefaultMutableTreeNode(controlWrapper);
            typeNode.add(controlNode);

            // Add compound control members if applicable
            if (control instanceof CompoundControl compound) {
                for (Control member : compound.getMemberControls()) {
                    ControlNodeWrapper memberWrapper = new ControlNodeWrapper(member);
                    controlNode.add(new DefaultMutableTreeNode(memberWrapper));
                }
            }
        }
    }

    /**
     * Expands the control tree to show all nodes.
     */
    private void expandControlTree() {
        for (int i = 0; i < controlTree.getRowCount(); i++) {
            controlTree.expandRow(i);
        }
    }

    /**
     * Handles control tree selection and shows interactive controls.
     */
    private void handleControlSelection() {
        TreePath selectionPath = controlTree.getSelectionPath();
        if (selectionPath != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
            Object userObject = node.getUserObject();

            if (userObject instanceof ControlNodeWrapper wrapper) {
                showControlInterface(wrapper.getControl());
            }
        }
    }

    /**
     * Shows interactive interface for the selected control.
     */
    private void showControlInterface(Control control) {
        controlPanel.removeAll();

        switch (control) {
            case FloatControl floatControl ->
                createFloatControlInterface(floatControl);
            case BooleanControl booleanControl ->
                createBooleanControlInterface(booleanControl);
            case EnumControl enumControl ->
                createEnumControlInterface(enumControl);
            default -> {
            }
        }

        controlPanel.revalidate();
        controlPanel.repaint();
    }

    /**
     * Creates interface for float controls (volume, pan, etc.).
     */
    private void createFloatControlInterface(FloatControl floatControl) {
        JLabel label = new JLabel(floatControl.getType().toString() + ":");
        controlPanel.add(label);

        float min = floatControl.getMinimum();
        float max = floatControl.getMaximum();
        float current = floatControl.getValue();

        JSlider slider = new JSlider(0, 1000);
        slider.setValue((int) ((current - min) / (max - min) * 1000));

        JLabel valueLabel = new JLabel(String.format("%.2f", current));

        slider.addChangeListener(e -> {
            float value = min + (slider.getValue() / 1000.0f) * (max - min);
            floatControl.setValue(value);
            valueLabel.setText(String.format("%.2f", value));
            propertySupport.firePropertyChange("controlChanged", null, floatControl);
        });

        controlPanel.add(slider);
        controlPanel.add(valueLabel);
    }

    /**
     * Creates interface for boolean controls (mute, etc.).
     */
    private void createBooleanControlInterface(BooleanControl booleanControl) {
        JCheckBox checkBox = new JCheckBox(booleanControl.getType().toString());
        checkBox.setSelected(booleanControl.getValue());

        checkBox.addActionListener(e -> {
            booleanControl.setValue(checkBox.isSelected());
            propertySupport.firePropertyChange("controlChanged", null, booleanControl);
        });

        controlPanel.add(checkBox);
    }

    /**
     * Creates interface for enumeration controls.
     */
    private void createEnumControlInterface(EnumControl enumControl) {
        JLabel label = new JLabel(enumControl.getType().toString() + ":");
        controlPanel.add(label);

        JComboBox<Object> comboBox = new JComboBox<>(enumControl.getValues());
        comboBox.setSelectedItem(enumControl.getValue());

        comboBox.addActionListener(e -> {
            enumControl.setValue(comboBox.getSelectedItem());
            propertySupport.firePropertyChange("controlChanged", null, enumControl);
        });

        controlPanel.add(comboBox);
    }

    /**
     * Handles format selection and validates compatibility.
     */
    private void handleFormatSelection() {
        LineWrapper selectedLine = lineList.getSelectedValue();
        AudioFormatWrapper selectedFormat = (AudioFormatWrapper) formatCombo.getSelectedItem();

        if (selectedLine != null && selectedFormat != null) {
            validateFormatCompatibility(selectedLine, selectedFormat.getFormat());
        }
    }

    /**
     * Validates format compatibility with the selected line.
     */
    private void validateFormatCompatibility(LineWrapper lineWrapper, AudioFormat format) {
        try {
            DataLine.Info lineInfo = (DataLine.Info) lineWrapper.getLineInfo();
            boolean isSupported = AudioSystem.isLineSupported(lineInfo);

            // Check if format is specifically supported
            AudioFormat[] supportedFormats = lineInfo.getFormats();
            boolean formatSupported = false;

            for (AudioFormat supported : supportedFormats) {
                if (isFormatCompatible(format, supported)) {
                    formatSupported = true;
                    break;
                }
            }

            String status = String.format("Format compatibility - Line: %s, Format: %s",
                    isSupported ? "Supported" : "Not supported",
                    formatSupported ? "Compatible" : "Incompatible");
            statusLabel.setText(status);

        } catch (Exception e) {
            statusLabel.setText("Error validating format: " + e.getMessage());
        }
    }

    /**
     * Opens the selected audio line.
     */
    private void openSelectedLine() {
        LineWrapper selectedLine = lineList.getSelectedValue();

        if (selectedLine == null) {
            statusLabel.setText("Please select a line");
            return;
        }

        try {
            closeCurrentLine(); // Close any existing line first

            Line.Info lineInfo = selectedLine.getLineInfo();

            // Handle different line types appropriately
            if (lineInfo instanceof Port.Info info) {
                // Ports don't need to be opened
                Port port = (Port) selectedLine.getMixer().getLine(lineInfo);
                currentLine = port;
                propertySupport.firePropertyChange("lineOpened", null, port);
                statusLabel.setText("Port accessed: " + lineInfo);

            } else if (lineInfo.getLineClass() == Clip.class) {
                // Clips should not be opened without audio data
                Clip clip = (Clip) selectedLine.getMixer().getLine(lineInfo);
                currentLine = clip;
                propertySupport.firePropertyChange("lineOpened", null, clip);
                statusLabel.setText("Clip accessed (not opened): " + lineInfo);

            } else if (lineInfo instanceof DataLine.Info dataLineInfo) {

                // Don't open Clip DataLines
                if (dataLineInfo.getLineClass() == Clip.class) {
                    Clip clip = (Clip) selectedLine.getMixer().getLine(dataLineInfo);
                    currentLine = clip;
                    propertySupport.firePropertyChange("lineOpened", null, clip);
                    statusLabel.setText("Clip DataLine accessed (not opened): " + dataLineInfo);
                    return;
                }

                // Open regular DataLines (SourceDataLine, TargetDataLine)
                AudioFormatWrapper selectedFormat = (AudioFormatWrapper) formatCombo.getSelectedItem();
                if (selectedFormat == null) {
                    statusLabel.setText("Please select an audio format for DataLine");
                    return;
                }

                DataLine dataLine = (DataLine) selectedLine.getMixer().getLine(dataLineInfo);
                dataLine.open();

                currentLine = dataLine;
                propertySupport.firePropertyChange("lineOpened", null, dataLine);
                statusLabel.setText("DataLine opened: " + dataLineInfo.getLineClass().getSimpleName());

            } else {
                // Generic Line handling
                Line line = selectedLine.getMixer().getLine(lineInfo);
                currentLine = line;
                propertySupport.firePropertyChange("lineOpened", null, line);
                statusLabel.setText("Line accessed: " + lineInfo.getLineClass().getSimpleName());
            }

        } catch (LineUnavailableException e) {
            statusLabel.setText("Cannot access line: " + e.getMessage());
        } catch (Exception e) {
            statusLabel.setText("Error accessing line: " + e.getMessage());
        }
    }

    /**
     * Closes the current audio line.
     */
    private void closeCurrentLine() {
        if (currentLine != null) {
            Line oldLine = currentLine;

            // Only close lines that were actually opened
            if (currentLine.isOpen() && !(currentLine instanceof Port) && !(currentLine instanceof Clip)) {
                currentLine.close();
            }

            currentLine = null;
            propertySupport.firePropertyChange("lineClosed", oldLine, null);
        }
    }

    /**
     * Tests audio functionality with the current configuration.
     */
    private void testAudioFunctionality() {
        if (currentLine == null) {
            statusLabel.setText("No line is currently open");
            return;
        }

        try {
            switch (currentLine) {
                case SourceDataLine sourceDataLine ->
                    testPlayback(sourceDataLine);
                case TargetDataLine targetDataLine ->
                    testRecording(targetDataLine);
                default -> {
                }
            }
        } catch (Exception e) {
            statusLabel.setText("Test failed: " + e.getMessage());
        }
    }

    /**
     * Tests playback functionality.
     */
    private void testPlayback(SourceDataLine sourceLine) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                sourceLine.start();

                AudioFormat format = sourceLine.getFormat();
                int sampleRate = (int) format.getSampleRate();
                int duration = 1000; // 1 second
                int sampleSize = format.getSampleSizeInBits() / 8;
                int channels = format.getChannels();
                int frameSize = sampleSize * channels;

                byte[] buffer = new byte[sampleRate * frameSize];

                // Generate sine wave
                for (int i = 0; i < buffer.length; i += frameSize) {
                    double time = (double) (i / frameSize) / sampleRate;
                    double frequency = 440.0; // A note
                    short sample = (short) (Short.MAX_VALUE * 0.3
                            * Math.sin(2 * Math.PI * frequency * time));

                    // Convert sample to bytes based on format
                    for (int ch = 0; ch < channels; ch++) {
                        for (int b = 0; b < sampleSize; b++) {
                            buffer[i + ch * sampleSize + b]
                                    = (byte) ((sample >> (8 * b)) & 0xFF);
                        }
                    }
                }

                sourceLine.write(buffer, 0, buffer.length);
                sourceLine.drain();
                sourceLine.stop();
                return null;
            }
        };
        worker.execute();
    }

    /**
     * Tests recording functionality.
     */
    private void testRecording(TargetDataLine targetLine) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                targetLine.start();

                // Record for 2 seconds
                AudioFormat format = targetLine.getFormat();
                int bufferSize = (int) (format.getSampleRate() * format.getFrameSize());
                byte[] buffer = new byte[bufferSize];

                long recordingTime = 2000; // 2 seconds
                long endTime = System.currentTimeMillis() + recordingTime;

                while (System.currentTimeMillis() < endTime) {
                    int bytesRead = targetLine.read(buffer, 0, buffer.length);
                    levelMeter.setValue(bytesRead);
                    Thread.sleep(100);
                }

                targetLine.stop();
                return null;
            }

            @Override
            protected void done() {
                statusLabel.setText("Recording test completed");
            }
        };

        worker.execute();
        statusLabel.setText("Testing recording...");
    }

    /**
     * Handles double-click on control tree nodes for quick control access.
     */
    private void handleControlDoubleClick() {
        TreePath selectionPath = controlTree.getSelectionPath();
        if (selectionPath != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
            Object userObject = node.getUserObject();

            if (userObject instanceof ControlNodeWrapper wrapper) {
                Control control = wrapper.getControl();

                // Show detailed control dialog for complex controls
                showDetailedControlDialog(control);
            }
        }
    }

    /**
     * Shows a detailed dialog for complex control manipulation.
     */
    private void showDetailedControlDialog(Control control) {
        JDialog dialog = new JDialog(this, "Control Details: " + control.getType(), true);
        dialog.setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Control information
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        contentPanel.add(new JLabel(control.getType().toString()), gbc);

        // Control-specific interface
        switch (control) {
            case FloatControl floatControl ->
                addFloatControlToDialog(contentPanel, floatControl);
            case BooleanControl booleanControl ->
                addBooleanControlToDialog(contentPanel, booleanControl);
            case EnumControl enumControl ->
                addEnumControlToDialog(contentPanel, enumControl);
            default -> {
            }
        }

        dialog.add(contentPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Adds float control interface to dialog.
     */
    private void addFloatControlToDialog(JPanel panel, FloatControl floatControl) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        int currentRow = panel.getComponentCount() / 2;

        // Current value
        gbc.gridx = 0;
        gbc.gridy = currentRow + 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Current Value:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        JLabel valueLabel = new JLabel(String.format("%.3f %s",
                floatControl.getValue(), floatControl.getUnits()));
        panel.add(valueLabel, gbc);

        // Range
        gbc.gridx = 0;
        gbc.gridy = currentRow + 2;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Range:"), gbc);
        gbc.gridx = 1;
        panel.add(new JLabel(String.format("%.3f to %.3f %s",
                floatControl.getMinimum(), floatControl.getMaximum(), floatControl.getUnits())), gbc);

        // Precision
        gbc.gridx = 0;
        gbc.gridy = currentRow + 3;
        panel.add(new JLabel("Precision:"), gbc);
        gbc.gridx = 1;
        panel.add(new JLabel(String.format("%.6f", floatControl.getPrecision())), gbc);

        // Control slider
        gbc.gridx = 0;
        gbc.gridy = currentRow + 4;
        panel.add(new JLabel("Control:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JSlider slider = new JSlider(0, 1000);
        float min = floatControl.getMinimum();
        float max = floatControl.getMaximum();
        float current = floatControl.getValue();
        slider.setValue((int) ((current - min) / (max - min) * 1000));

        slider.addChangeListener(e -> {
            float value = min + (slider.getValue() / 1000.0f) * (max - min);
            floatControl.setValue(value);
            valueLabel.setText(String.format("%.3f %s", value, floatControl.getUnits()));
            propertySupport.firePropertyChange("controlChanged", current, value);
        });

        panel.add(slider, gbc);
    }

    /**
     * Adds boolean control interface to dialog.
     */
    private void addBooleanControlToDialog(JPanel panel, BooleanControl booleanControl) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        int currentRow = panel.getComponentCount() / 2;

        gbc.gridx = 0;
        gbc.gridy = currentRow + 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("State:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JCheckBox checkBox = new JCheckBox("Enabled");
        checkBox.setSelected(booleanControl.getValue());
        checkBox.addActionListener(e -> {
            boolean oldValue = booleanControl.getValue();
            booleanControl.setValue(checkBox.isSelected());
            propertySupport.firePropertyChange("controlChanged", oldValue, checkBox.isSelected());
        });

        panel.add(checkBox, gbc);

        // Show state labels if available
        gbc.gridx = 0;
        gbc.gridy = currentRow + 2;
        panel.add(new JLabel("True State:"), gbc);
        gbc.gridx = 1;
        panel.add(new JLabel(booleanControl.getStateLabel(true)), gbc);

        gbc.gridx = 0;
        gbc.gridy = currentRow + 3;
        panel.add(new JLabel("False State:"), gbc);
        gbc.gridx = 1;
        panel.add(new JLabel(booleanControl.getStateLabel(false)), gbc);
    }

    /**
     * Adds enum control interface to dialog.
     */
    private void addEnumControlToDialog(JPanel panel, EnumControl enumControl) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        int currentRow = panel.getComponentCount() / 2;

        gbc.gridx = 0;
        gbc.gridy = currentRow + 1;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Current:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JComboBox<Object> comboBox = new JComboBox<>(enumControl.getValues());
        comboBox.setSelectedItem(enumControl.getValue());
        comboBox.addActionListener(e -> {
            Object oldValue = enumControl.getValue();
            enumControl.setValue(comboBox.getSelectedItem());
            propertySupport.firePropertyChange("controlChanged", oldValue, comboBox.getSelectedItem());
        });

        panel.add(comboBox, gbc);
    }

    /**
     * Updates the controls display when a line is opened.
     */
    private void updateControlsForOpenLine() {
        if (currentLine != null) {
            // Force refresh of the controls tree
            LineWrapper selectedLine = lineList.getSelectedValue();
            if (selectedLine != null) {
                updateControlsForLine(selectedLine);
            }

            // Enable control interaction
            controlTree.setEnabled(true);
            openLineButton.setEnabled(false);
            closeLineButton.setEnabled(true);
            testAudioButton.setEnabled(true);
        }
    }

    /**
     * Clears the controls display when a line is closed.
     */
    private void clearControlsDisplay() {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) controlTreeModel.getRoot();
        root.removeAllChildren();
        controlTreeModel.reload();

        controlPanel.removeAll();
        controlPanel.revalidate();
        controlPanel.repaint();

        infoArea.setText("");

        // Update button states
        openLineButton.setEnabled(true);
        closeLineButton.setEnabled(false);
        testAudioButton.setEnabled(false);
    }

    /**
     * Updates the information display with current line and control details.
     */
    private void updateInfoDisplay() {
        StringBuilder info = new StringBuilder();

        LineWrapper selectedLine = lineList.getSelectedValue();
        if (selectedLine != null) {
            info.append("=== LINE INFORMATION ===\n");
            info.append("Type: ").append(selectedLine.getType()).append("\n");
            info.append("Class: ").append(selectedLine.getLineInfo().getLineClass().getSimpleName()).append("\n");

            if (selectedLine.getLineInfo() instanceof DataLine.Info dataLineInfo) {
                info.append("Buffer Size: ").append(dataLineInfo.getMinBufferSize())
                        .append(" - ").append(dataLineInfo.getMaxBufferSize()).append(" bytes\n");

                AudioFormat[] formats = dataLineInfo.getFormats();
                info.append("Supported Formats: ").append(formats.length).append("\n");

                for (int i = 0; i < Math.min(formats.length, 3); i++) {
                    info.append("  Format ").append(i + 1).append(": ")
                            .append(formatToString(formats[i])).append("\n");
                }
                if (formats.length > 3) {
                    info.append("  ... and ").append(formats.length - 3).append(" more\n");
                }
            } else if (selectedLine.getLineInfo() instanceof Port.Info portInfo) {
                info.append("Port Name: ").append(portInfo.getName()).append("\n");
                info.append("Is Source: ").append(portInfo.isSource()).append("\n");
            } else if (selectedLine.getLineInfo().getLineClass() == Clip.class) {
                info.append("Clip Type: Audio playback with positioning\n");
                info.append("Note: Clips require audio data to be opened\n");
            }
        }

        if (currentLine != null) {
            info.append("\n=== CURRENT LINE STATUS ===\n");
            info.append("Open: ").append(currentLine.isOpen()).append("\n");

            // Only show running/active for lines that support these states
            if (!(currentLine instanceof Port)) {

            }

            switch (currentLine) {

                case Clip clip -> {
                    info.append("Frame Length: ").append(clip.getFrameLength()).append("\n");
                    info.append("Microsecond Length: ").append(clip.getMicrosecondLength()).append("\n");
                    info.append("Frame Position: ").append(clip.getFramePosition()).append("\n");
                    info.append("Microsecond Position: ").append(clip.getMicrosecondPosition()).append("\n");

                    if (clip.getFormat() != null) {
                        AudioFormat format = clip.getFormat();
                        info.append("Format: ").append(formatToString(format)).append("\n");
                    }
                }
                case Port port -> {
                    info.append("Port Type: ")
                            .append(port.toString())
                            .append(" is ")
                            .append(port.isOpen() ? "open" : "closed");
                    info.append("\nAlways Active: Ports don't have open/close states\n");
                }
                case DataLine dataLine -> {
                    info.append("Level: ").append(String.format("%.2f", dataLine.getLevel())).append("\n");
                    info.append("Buffer Size: ").append(dataLine.getBufferSize()).append(" bytes\n");
                    info.append("Available: ").append(dataLine.available()).append(" bytes\n");
                    info.append("Frame Position: ").append(dataLine.getFramePosition()).append("\n");
                    info.append("Microsecond Position: ").append(dataLine.getMicrosecondPosition()).append("\n");

                    if (dataLine.getFormat() != null) {
                        AudioFormat format = dataLine.getFormat();
                        info.append("Format: ").append(formatToString(format)).append("\n");
                    }
                }
                default -> {
                }
            }

            // Control information
            Control[] controls = currentLine.getControls();
            if (controls.length > 0) {
                info.append("\n=== AVAILABLE CONTROLS ===\n");
                for (Control control : controls) {
                    info.append("• ").append(control.getType().toString());
                    switch (control) {
                        case FloatControl fc ->
                            info.append(" (").append(String.format("%.2f", fc.getValue())).append(")");
                        case BooleanControl bc ->
                            info.append(" (").append(bc.getValue()).append(")");
                        case EnumControl ec ->
                            info.append(" (").append(ec.getValue()).append(")");
                        default -> {
                        }
                    }
                    info.append("\n");
                }
            } else {
                info.append("\n=== NO CONTROLS AVAILABLE ===\n");
                info.append("This line type does not expose any controls\n");
            }
        }

        infoArea.setText(info.toString());
        infoArea.setCaretPosition(0);
    }

    /**
     * Converts AudioFormat to readable string.
     */
    private String formatToString(AudioFormat format) {
        return String.format("%.0f Hz, %d-bit, %d ch, %s",
                format.getSampleRate(),
                format.getSampleSizeInBits(),
                format.getChannels(),
                format.getEncoding());
    }

    /**
     * Updates status display with current system state.
     */
    private void updateStatusDisplay() {
        if (currentLine instanceof DataLine dataLine) {
            float level = dataLine.getLevel();

            // Convert level to percentage (level is typically 0.0 to 1.0)
            int percentage = (int) (Math.abs(level) * 100);
            levelMeter.setValue(Math.min(percentage, 100));
            levelMeter.setString("Level: " + percentage + "%");
        } else {
            // Simulate some activity when no line is active
            int randomLevel = (int) (Math.random() * 20);
            levelMeter.setValue(randomLevel);
            levelMeter.setIndeterminate(true);
            levelMeter.setString("System: " + randomLevel + "%");
        }
    }

    /**
     * Refreshes the entire audio system data.
     */
    private void refreshAudioSystem() {
        closeCurrentLine();
        loadAudioSystemData();
        clearControlsDisplay();
        statusLabel.setText("Audio system refreshed");
        propertySupport.firePropertyChange("systemRefreshed", null, "complete");
    }

    // ==================== WRAPPER CLASSES ====================
    /**
     * Wrapper class for audio devices (mixers).
     */
    private static class DeviceWrapper {

        private final Mixer mixer;
        private final String name;

        public DeviceWrapper(Mixer mixer, String name) {
            this.mixer = mixer;
            this.name = name;
        }

        public Mixer getMixer() {
            return mixer;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            try {
                Mixer.Info info = mixer.getMixerInfo();
                return name + " - " + info.getDescription();
            } catch (Exception e) {
                return name;
            }
        }
    }

    /**
     * Wrapper class for audio lines.
     */
    private static class LineWrapper {

        private final Line.Info lineInfo;
        private final String type;
        private final Mixer mixer;

        public LineWrapper(Line.Info lineInfo, String type, Mixer mixer) {
            this.lineInfo = lineInfo;
            this.type = type;
            this.mixer = mixer;
        }

        public Line.Info getLineInfo() {
            return lineInfo;
        }

        public String getType() {
            return type;
        }

        public Mixer getMixer() {
            return mixer;
        }

        @Override
        public String toString() {
            String className = lineInfo.getLineClass().getSimpleName();
            return type + " - " + className;
        }
    }

    /**
     * Wrapper class for audio formats.
     */
    private static class AudioFormatWrapper {

        private final AudioFormat format;

        public AudioFormatWrapper(AudioFormat format) {
            this.format = format;
        }

        public AudioFormat getFormat() {
            return format;
        }

        @Override
        public String toString() {
            return String.format("%.0f Hz, %d-bit, %d ch (%s)",
                    format.getSampleRate(),
                    format.getSampleSizeInBits(),
                    format.getChannels(),
                    format.getEncoding().toString());
        }
    }

    /**
     * Wrapper class for control tree nodes.
     */
    private static class ControlNodeWrapper {

        private final Control control;

        public ControlNodeWrapper(Control control) {
            this.control = control;
        }

        public Control getControl() {
            return control;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(control.getType().toString());

            switch (control) {
                case FloatControl fc ->
                    sb.append(" (").append(String.format("%.2f", fc.getValue())).append(")");
                case BooleanControl bc ->
                    sb.append(" (").append(bc.getValue() ? "ON" : "OFF").append(")");
                case EnumControl ec ->
                    sb.append(" (").append(ec.getValue()).append(")");
                default -> {
                }
            }

            return sb.toString();
        }
    }

    // ==================== CUSTOM RENDERERS ====================
    /**
     * Custom cell renderer for the lines list.
     */
    private static class LineListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {

            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof LineWrapper wrapper) {

                if (null != wrapper.getType()) // Set different icons for different line types
                {
                    switch (wrapper.getType()) {
                        case "Source" ->
                            setIcon(createColoredIcon(Color.BLUE));
                        case "Target" ->
                            setIcon(createColoredIcon(Color.RED));
                        case "Source Port" ->
                            setIcon(createColoredIcon(Color.CYAN));
                        case "Target Port" ->
                            setIcon(createColoredIcon(Color.MAGENTA));
                        case "Clip", "Clip (DataLine)" ->
                            setIcon(createColoredIcon(Color.GREEN));
                        default -> {
                        }
                    }
                }

                // Add tooltip with more information
                if (wrapper.getLineInfo() instanceof DataLine.Info info) {
                    setToolTipText(String.format("Buffer: %d-%d bytes, Formats: %d",
                            info.getMinBufferSize(), info.getMaxBufferSize(), info.getFormats().length));
                } else if (wrapper.getLineInfo() instanceof Port.Info portInfo) {
                    setToolTipText("Port: " + portInfo.getName() + " ("
                            + (portInfo.isSource() ? "Source" : "Target") + ")");
                } else if (wrapper.getLineInfo().getLineClass() == Clip.class) {
                    setToolTipText("Clip - Audio playback with positioning control");
                }
            }

            return this;
        }

        private Icon createColoredIcon(Color color) {
            return new Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    g.setColor(color);
                    g.fillOval(x, y, getIconWidth(), getIconHeight());
                    g.setColor(Color.BLACK);
                    g.drawOval(x, y, getIconWidth(), getIconHeight());
                }

                @Override
                public int getIconWidth() {
                    return 12;
                }

                @Override
                public int getIconHeight() {
                    return 12;
                }
            };
        }
    }

    /**
     * Custom cell renderer for the control tree.
     */
    private static class ControlTreeCellRenderer extends DefaultTreeCellRenderer {

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {

            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            if (value instanceof DefaultMutableTreeNode node) {
                Object userObject = node.getUserObject();

                if (userObject instanceof ControlNodeWrapper wrapper) {
                    Control control = wrapper.getControl();

                    // Set different icons for different control types
                    switch (control) {
                        case FloatControl floatControl ->
                            setIcon(createControlIcon("F", Color.GREEN));
                        case BooleanControl booleanControl ->
                            setIcon(createControlIcon("B", Color.ORANGE));
                        case EnumControl enumControl ->
                            setIcon(createControlIcon("E", Color.MAGENTA));
                        case CompoundControl compoundControl ->
                            setIcon(createControlIcon("C", Color.CYAN));
                        default -> {
                        }
                    }

                    // Add detailed tooltip
                    setToolTipText(createControlTooltip(control));
                }
            }

            return this;
        }

        private Icon createControlIcon(String letter, Color color) {
            return new Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    g.setColor(color);
                    g.fillRect(x, y, getIconWidth(), getIconHeight());
                    g.setColor(Color.BLACK);
                    g.drawRect(x, y, getIconWidth(), getIconHeight());
                    g.setColor(Color.WHITE);
                    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
                    FontMetrics fm = g.getFontMetrics();
                    int stringWidth = fm.stringWidth(letter);
                    int stringHeight = fm.getAscent();
                    g.drawString(letter, x + (getIconWidth() - stringWidth) / 2,
                            y + (getIconHeight() + stringHeight) / 2 - 2);
                }

                @Override
                public int getIconWidth() {
                    return 16;
                }

                @Override
                public int getIconHeight() {
                    return 16;
                }
            };
        }

        private String createControlTooltip(Control control) {
            StringBuilder tooltip = new StringBuilder();
            tooltip.append("<html>");
            tooltip.append("<b>Type:</b> ").append(control.getType()).append("<br>");

            switch (control) {
                case FloatControl fc -> {
                    tooltip.append("<b>Range:</b> ").append(fc.getMinimum()).append(" to ").append(fc.getMaximum()).append("<br>");
                    tooltip.append("<b>Current:</b> ").append(fc.getValue()).append("<br>");
                    tooltip.append("<b>Units:</b> ").append(fc.getUnits());
                }
                case BooleanControl bc -> {
                    tooltip.append("<b>State:</b> ").append(bc.getValue()).append("<br>");
                    tooltip.append("<b>True Label:</b> ").append(bc.getStateLabel(true)).append("<br>");
                    tooltip.append("<b>False Label:</b> ").append(bc.getStateLabel(false));
                }
                case EnumControl ec -> {
                    tooltip.append("<b>Current:</b> ").append(ec.getValue()).append("<br>");
                    tooltip.append("<b>Options:</b> ").append(ec.getValues().length);
                }
                default -> {
                }
            }

            tooltip.append("</html>");
            return tooltip.toString();
        }
    }

    // ==================== PROPERTY CHANGE SUPPORT ====================
    /**
     * Custom property change support for audio events.
     */
    private static class AudioPropertyChangeSupport {

        private final PropertyChangeSupport support;

        public AudioPropertyChangeSupport(Object source) {
            support = new PropertyChangeSupport(source);
        }

        public void addPropertyChangeListener(String propertyName,
                PropertyChangeListener listener) {
            support.addPropertyChangeListener(propertyName, listener);
        }

        public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
            support.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    // ==================== APPLICATION ENTRY POINT ====================
    /**
     * Application main method.
     */
    public static void main(String[] args) {
        // Set system look and feel
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
                System.err.println("Could not set system look and feel: " + e.getMessage());
            }

            // Create and show AudioSystemExplorer
            AudioSystemExplorer app = new AudioSystemExplorer();
            app.setVisible(true);
        });
    }

    /**
     * Clean up resources when the application is disposed.
     */
    @Override
    public void dispose() {
        if (statusUpdateTimer != null) {
            statusUpdateTimer.stop();
        }
        closeCurrentLine();
        super.dispose();
    }
}
