/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

/**
 *
 * @author pc
 */
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Aplicación didáctica para demostrar el uso de Java Sound API Implementa el patrón MVC para enseñar reproducción y
 * grabación de audio
 */
public class JavaSoundTutorialApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
                System.err.println(e.getMessage());
            }

            AudioSystemModel model = new AudioSystemModel();
            JavaSoundView view = new JavaSoundView();
            JavaSoundController controller = new JavaSoundController(model, view);

            view.setVisible(true);
        });
    }
}

/**
 * Modelo que encapsula toda la información del sistema de audio
 */
class AudioSystemModel {

    private List<AudioSystemListener> listeners = new ArrayList<>();
    private Mixer.Info[] mixerInfos;
    private Mixer selectedMixer;
    private Line.Info[] lineInfos;
    private Line selectedLine;
    private Control[] controls;

    public AudioSystemModel() {
        refreshAudioSystem();
    }

    public void addListener(AudioSystemListener listener) {
        listeners.add(listener);
    }

    public void refreshAudioSystem() {
        mixerInfos = AudioSystem.getMixerInfo();
        notifyMixersChanged();
    }

    public Mixer.Info[] getMixerInfos() {
        return mixerInfos;
    }

    public void setSelectedMixer(Mixer.Info mixerInfo) {
        if (selectedMixer != null) {
            selectedMixer.close();
        }

        selectedMixer = AudioSystem.getMixer(mixerInfo);
        try {
            selectedMixer.open();
            lineInfos = selectedMixer.getSourceLineInfo();
            Line.Info[] targetLines = selectedMixer.getTargetLineInfo();

            // Combinar líneas fuente y destino
            Line.Info[] allLines = new Line.Info[lineInfos.length + targetLines.length];
            System.arraycopy(lineInfos, 0, allLines, 0, lineInfos.length);
            System.arraycopy(targetLines, 0, allLines, lineInfos.length, targetLines.length);
            lineInfos = allLines;

            notifyLinesChanged();
        } catch (LineUnavailableException e) {
            System.err.println(e.getMessage());
        }
    }

    public Line.Info[] getLineInfos() {
        return lineInfos != null ? lineInfos : new Line.Info[0];
    }

    public void setSelectedLine(Line.Info lineInfo) {
        if (selectedLine != null) {
            selectedLine.close();
        }

        try {
            selectedLine = selectedMixer.getLine(lineInfo);
            selectedLine.open();

            if (selectedLine instanceof SourceDataLine || selectedLine instanceof TargetDataLine) {
                controls = selectedLine.getControls();
                notifyControlsChanged();
                notifyFormatsChanged();
            }

        } catch (LineUnavailableException e) {
            System.err.println(e.getMessage());
        }
    }

    public Line getSelectedLine() {
        return selectedLine;
    }

    public Control[] getControls() {
        return controls != null ? controls : new Control[0];
    }

    public AudioFormat[] getSupportedFormats() {
        if (selectedLine instanceof DataLine) {
            DataLine.Info info = (DataLine.Info) selectedLine.getLineInfo();
            return info.getFormats();
        }
        return new AudioFormat[0];
    }

    private void notifyMixersChanged() {
        for (AudioSystemListener listener : listeners) {
            listener.mixersChanged();
        }
    }

    private void notifyLinesChanged() {
        for (AudioSystemListener listener : listeners) {
            listener.linesChanged();
        }
    }

    private void notifyControlsChanged() {
        for (AudioSystemListener listener : listeners) {
            listener.controlsChanged();
        }
    }

    private void notifyFormatsChanged() {
        for (AudioSystemListener listener : listeners) {
            listener.formatsChanged();
        }
    }
}

/**
 * Interface para escuchar cambios en el modelo del sistema de audio
 */
interface AudioSystemListener {

    void mixersChanged();

    void linesChanged();

    void controlsChanged();

    void formatsChanged();
}

/**
 * Vista principal de la aplicación usando Swing
 */
class JavaSoundView extends JFrame implements AudioSystemListener {

    private JComboBox<Mixer.Info> mixerComboBox;
    private JList<Line.Info> lineList;
    private JTree controlTree;
    private JList<AudioFormat> formatList;
    private JTextArea infoArea;
    private JButton playButton, recordButton, stopButton;
    private JPanel operationPanel;
    private DefaultListModel<Line.Info> lineModel;
    private DefaultTreeModel controlTreeModel;
    private DefaultListModel<AudioFormat> formatModel;

    public JavaSoundView() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
    }

    private void initializeComponents() {
        setTitle("Tutorial Java Sound API - Reproducción y Grabación");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);

        // Componentes principales
        mixerComboBox = new JComboBox<>();
        mixerComboBox.setRenderer(new MixerInfoRenderer());

        lineModel = new DefaultListModel<>();
        lineList = new JList<>(lineModel);
        lineList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lineList.setCellRenderer(new LineInfoRenderer());

        controlTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode("Controles"));
        controlTree = new JTree(controlTreeModel);

        formatModel = new DefaultListModel<>();
        formatList = new JList<>(formatModel);
        formatList.setCellRenderer(new AudioFormatRenderer());

        infoArea = new JTextArea(10, 30);
        infoArea.setEditable(false);
        infoArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        // Botones de operación
        playButton = new JButton("Reproducir");
        recordButton = new JButton("Grabar");
        stopButton = new JButton("Detener");
        stopButton.setEnabled(false);

        operationPanel = new JPanel();
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // Panel superior con selector de mezclador
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(BorderFactory.createTitledBorder("Sistema de Audio"));
        topPanel.add(new JLabel("Mezclador:"));
        topPanel.add(mixerComboBox);
        add(topPanel, BorderLayout.NORTH);

        // Panel central dividido
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // Panel izquierdo con listas
        JPanel leftPanel = new JPanel(new BorderLayout());

        JSplitPane leftSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // Lista de líneas
        JPanel linePanel = new JPanel(new BorderLayout());
        linePanel.setBorder(BorderFactory.createTitledBorder("Líneas de Audio"));
        linePanel.add(new JScrollPane(lineList));
        leftSplit.setTopComponent(linePanel);

        // Árbol de controles
        JPanel controlPanel = new JPanel(new BorderLayout());
        controlPanel.setBorder(BorderFactory.createTitledBorder("Controles"));
        controlPanel.add(new JScrollPane(controlTree));
        leftSplit.setBottomComponent(controlPanel);

        leftPanel.add(leftSplit);
        mainSplit.setLeftComponent(leftPanel);

        // Panel derecho
        JPanel rightPanel = new JPanel(new BorderLayout());

        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // Lista de formatos
        JPanel formatPanel = new JPanel(new BorderLayout());
        formatPanel.setBorder(BorderFactory.createTitledBorder("Formatos de Audio"));
        formatPanel.add(new JScrollPane(formatList));
        rightSplit.setTopComponent(formatPanel);

        // Área de información
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Información y Tutorial"));
        infoPanel.add(new JScrollPane(infoArea));
        rightSplit.setBottomComponent(infoPanel);

        rightPanel.add(rightSplit);
        mainSplit.setRightComponent(rightPanel);

        add(mainSplit, BorderLayout.CENTER);

        // Panel inferior con controles de operación
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createTitledBorder("Operaciones"));

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(playButton);
        buttonPanel.add(recordButton);
        buttonPanel.add(stopButton);
        bottomPanel.add(buttonPanel, BorderLayout.NORTH);
        bottomPanel.add(operationPanel, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);

        // Configurar divisores
        mainSplit.setDividerLocation(300);
        leftSplit.setDividerLocation(200);
        rightSplit.setDividerLocation(150);
    }

    private void setupEventHandlers() {
        mixerComboBox.addActionListener(e -> {
            if (mixerSelectionListener != null) {
                Mixer.Info selected = (Mixer.Info) mixerComboBox.getSelectedItem();
                if (selected != null) {
                    mixerSelectionListener.mixerSelected(selected);
                }
            }
        });

        lineList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && lineSelectionListener != null) {
                Line.Info selected = lineList.getSelectedValue();
                if (selected != null) {
                    lineSelectionListener.lineSelected(selected);
                }
            }
        });
    }

    // Listeners para comunicación con el controlador
    private MixerSelectionListener mixerSelectionListener;
    private LineSelectionListener lineSelectionListener;

    public void setMixerSelectionListener(MixerSelectionListener listener) {
        this.mixerSelectionListener = listener;
    }

    public void setLineSelectionListener(LineSelectionListener listener) {
        this.lineSelectionListener = listener;
    }

    public void setOperationListener(OperationListener listener) {
        playButton.addActionListener(e -> listener.playRequested());
        recordButton.addActionListener(e -> listener.recordRequested());
        stopButton.addActionListener(e -> listener.stopRequested());
    }

    // Implementación de AudioSystemListener
    @Override
    public void mixersChanged() {
        SwingUtilities.invokeLater(() -> {
            mixerComboBox.removeAllItems();
            // Aquí el controlador proporcionará los datos
        });
    }

    @Override
    public void linesChanged() {
        SwingUtilities.invokeLater(() -> {
            lineModel.clear();
            // Aquí el controlador proporcionará los datos
        });
    }

    @Override
    public void controlsChanged() {
        SwingUtilities.invokeLater(() -> {
            controlTreeModel.setRoot(new DefaultMutableTreeNode("Controles"));
            // Aquí el controlador proporcionará los datos
        });
    }

    @Override
    public void formatsChanged() {
        SwingUtilities.invokeLater(() -> {
            formatModel.clear();
            // Aquí el controlador proporcionará los datos
        });
    }

    // Métodos para actualizar la vista
    public void updateMixers(Mixer.Info[] mixers) {
        SwingUtilities.invokeLater(() -> {
            mixerComboBox.removeAllItems();
            for (Mixer.Info mixer : mixers) {
                mixerComboBox.addItem(mixer);
            }
        });
    }

    public void updateLines(Line.Info[] lines) {
        SwingUtilities.invokeLater(() -> {
            lineModel.clear();
            for (Line.Info line : lines) {
                lineModel.addElement(line);
            }
        });
    }

    public void updateControls(Control[] controls) {
        SwingUtilities.invokeLater(() -> {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Controles");
            for (Control control : controls) {
                DefaultMutableTreeNode controlNode = new DefaultMutableTreeNode(control);
                root.add(controlNode);

                // Si es un control compuesto, agregar subcontroles
                if (control instanceof CompoundControl compound) {
                    for (Control member : compound.getMemberControls()) {
                        controlNode.add(new DefaultMutableTreeNode(member));
                    }
                }
            }
            controlTreeModel.setRoot(root);
            controlTree.expandRow(0);
        });
    }

    public void updateFormats(AudioFormat[] formats) {
        SwingUtilities.invokeLater(() -> {
            formatModel.clear();
            for (AudioFormat format : formats) {
                formatModel.addElement(format);
            }
        });
    }

    public void updateInfo(String info) {
        SwingUtilities.invokeLater(() -> {
            infoArea.setText(info);
            infoArea.setCaretPosition(0);
        });
    }

    public void setOperationButtonsEnabled(boolean play, boolean record, boolean stop) {
        SwingUtilities.invokeLater(() -> {
            playButton.setEnabled(play);
            recordButton.setEnabled(record);
            stopButton.setEnabled(stop);
        });
    }
}

/**
 * Controlador que maneja la lógica de la aplicación
 */
class JavaSoundController implements MixerSelectionListener, LineSelectionListener, OperationListener {

    private AudioSystemModel model;
    private JavaSoundView view;
    private AudioPlayer audioPlayer;
    private AudioRecorder audioRecorder;

    public JavaSoundController(AudioSystemModel model, JavaSoundView view) {
        this.model = model;
        this.view = view;

        // Configurar listeners
        model.addListener(view);
        view.setMixerSelectionListener(this);
        view.setLineSelectionListener(this);
        view.setOperationListener(this);

        // Inicializar vista con datos del modelo
        updateViewWithModelData();

        // Mostrar información tutorial inicial
        showTutorialInfo();
    }

    private void updateViewWithModelData() {
        view.updateMixers(model.getMixerInfos());
    }

    @Override
    public void mixerSelected(Mixer.Info mixerInfo) {
        model.setSelectedMixer(mixerInfo);
        view.updateLines(model.getLineInfos());

        // Mostrar información del mezclador
        StringBuilder info = new StringBuilder();
        info.append("MEZCLADOR SELECCIONADO:\n");
        info.append("Nombre: ").append(mixerInfo.getName()).append("\n");
        info.append("Descripción: ").append(mixerInfo.getDescription()).append("\n");
        info.append("Proveedor: ").append(mixerInfo.getVendor()).append("\n");
        info.append("Versión: ").append(mixerInfo.getVersion()).append("\n\n");

        info.append("TUTORIAL - Mezcladores:\n");
        info.append("Los mezcladores (Mixer) son dispositivos que pueden reproducir, grabar,\n");
        info.append("o procesar datos de audio. Cada mezclador tiene líneas de origen\n");
        info.append("(SourceDataLine) para reproducción y líneas de destino (TargetDataLine)\n");
        info.append("para grabación.\n\n");
        info.append("Selecciona una línea para ver sus controles y formatos soportados.");

        view.updateInfo(info.toString());
    }

    @Override
    public void lineSelected(Line.Info lineInfo) {
        model.setSelectedLine(lineInfo);
        view.updateControls(model.getControls());
        view.updateFormats(model.getSupportedFormats());

        // Mostrar información de la línea
        StringBuilder info = new StringBuilder();
        info.append("LÍNEA SELECCIONADA:\n");
        info.append("Clase: ").append(lineInfo.getLineClass().getSimpleName()).append("\n");

        if (lineInfo instanceof DataLine.Info dataLineInfo) {
            info.append("Formatos soportados: ").append(dataLineInfo.getFormats().length).append("\n");
            info.append("Buffer mínimo: ").append(dataLineInfo.getMinBufferSize()).append(" bytes\n");
            info.append("Buffer máximo: ").append(dataLineInfo.getMaxBufferSize()).append(" bytes\n");
        }

        info.append("\nTUTORIAL - Tipos de Líneas:\n");

        if (lineInfo.getLineClass() == SourceDataLine.class) {
            info.append("SourceDataLine: Para reproducción de audio.\n");
            info.append("- Escribe datos de audio al dispositivo de salida\n");
            info.append("- Usa write() para enviar datos\n");
            info.append("- Controla volumen, balance, etc.\n");
            view.setOperationButtonsEnabled(true, false, false);
        } else if (lineInfo.getLineClass() == TargetDataLine.class) {
            info.append("TargetDataLine: Para grabación de audio.\n");
            info.append("- Lee datos de audio desde dispositivo de entrada\n");
            info.append("- Usa read() para obtener datos\n");
            info.append("- Controla ganancia de entrada\n");
            view.setOperationButtonsEnabled(false, true, false);
        } else if (lineInfo.getLineClass() == Clip.class) {
            info.append("Clip: Para reproducir archivos de audio completos.\n");
            info.append("- Carga todo el audio en memoria\n");
            info.append("- Permite bucles y posicionamiento\n");
            info.append("- Ideal para sonidos cortos\n");
            view.setOperationButtonsEnabled(true, false, false);
        } else if (lineInfo.getLineClass() == Port.class) {
            info.append("Port: Representa conexiones físicas.\n");
            info.append("- Entrada de micrófono, línea de entrada\n");
            info.append("- Salida de altavoces, auriculares\n");
            info.append("- No maneja datos directamente\n");
            view.setOperationButtonsEnabled(false, false, false);
        }

        view.updateInfo(info.toString());
    }

    @Override
    public void playRequested() {
        Line selectedLine = model.getSelectedLine();
        switch (selectedLine) {
            case SourceDataLine sourceDataLine -> {
                if (audioPlayer != null) {
                    audioPlayer.interrupt();
                }
                audioPlayer = new AudioPlayer(sourceDataLine);
                audioPlayer.start();
                view.setOperationButtonsEnabled(false, false, true);
            }
            case Clip clip -> // Implementar reproducción con Clip
                playWithClip(clip);
            default -> {
            }
        }
    }

    @Override
    public void recordRequested() {
        Line selectedLine = model.getSelectedLine();
        if (selectedLine instanceof TargetDataLine targetDataLine) {
            if (audioRecorder != null) {
                audioRecorder.interrupt();
            }
            audioRecorder = new AudioRecorder(targetDataLine);
            audioRecorder.start();
            view.setOperationButtonsEnabled(false, false, true);
        }
    }

    @Override
    public void stopRequested() {
        if (audioPlayer != null) {
            audioPlayer.interrupt();
            audioPlayer = null;
        }
        if (audioRecorder != null) {
            audioRecorder.interrupt();
            audioRecorder = null;
        }
        view.setOperationButtonsEnabled(true, true, false);
    }

    private void playWithClip(Clip clip) {
        // Implementar reproducción básica con Clip
        try {
            // Generar un tono simple para demostración
            AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
            byte[] audioData = generateTone(440, 2.0, format);

            AudioInputStream audioStream = new AudioInputStream(
                    new ByteArrayInputStream(audioData), format, audioData.length / format.getFrameSize());

            clip.open(audioStream);
            clip.start();

            view.setOperationButtonsEnabled(false, false, true);

            // Auto-stop después de que termine
            new Timer(3000, e -> {
                try (clip) {
                    clip.stop();
                }
                view.setOperationButtonsEnabled(true, false, false);
            }).start();

        } catch (IOException | LineUnavailableException e) {
            System.err.println(e.getMessage());
        }
    }

    private byte[] generateTone(double frequency, double duration, AudioFormat format) {
        int sampleRate = (int) format.getSampleRate();
        int samples = (int) (sampleRate * duration);
        byte[] output = new byte[samples * format.getFrameSize()];

        for (int i = 0; i < samples; i++) {
            double time = i / (double) sampleRate;
            short sample = (short) (Short.MAX_VALUE * 0.3 * Math.sin(2.0 * Math.PI * frequency * time));

            // Stereo: mismo sample en ambos canales
            output[i * 4] = (byte) (sample & 0xFF);
            output[i * 4 + 1] = (byte) ((sample >> 8) & 0xFF);
            output[i * 4 + 2] = (byte) (sample & 0xFF);
            output[i * 4 + 3] = (byte) ((sample >> 8) & 0xFF);
        }

        return output;
    }

    private void showTutorialInfo() {
        String tutorial = """
                          TUTORIAL JAVA SOUND API
                          ========================
                          
                          Esta aplicaci\u00f3n demuestra el uso completo de Java Sound API:
                          
                          1. AudioSystem: Punto de entrada principal
                             - getMixerInfo(): Obtiene mezcladores disponibles
                             - getMixer(): Obtiene instancia de mezclador
                          
                          2. Mixer: Dispositivo de audio (tarjeta de sonido)
                             - getSourceLineInfo(): L\u00edneas para reproducci\u00f3n
                             - getTargetLineInfo(): L\u00edneas para grabaci\u00f3n
                          
                          3. Line: Conexi\u00f3n de audio
                             - SourceDataLine: Reproducir datos
                             - TargetDataLine: Grabar datos
                             - Clip: Reproducir archivos
                             - Port: Conexiones f\u00edsicas
                          
                          4. Control: Ajustes de l\u00ednea
                             - FloatControl: Volumen, balance
                             - BooleanControl: Mute, selecci\u00f3n
                             - CompoundControl: Controles agrupados
                          
                          5. AudioFormat: Especificaci\u00f3n de audio
                             - Sample rate, bit depth, canales, etc.
                          
                          INSTRUCCIONES:
                          - Selecciona un mezclador del combo superior
                          - Elige una l\u00ednea de la lista
                          - Observa sus controles y formatos
                          - Usa los botones para probar reproducci\u00f3n/grabaci\u00f3n""";

        view.updateInfo(tutorial);
    }
}

// Clases para reproducción y grabación
class AudioPlayer extends Thread {

    private SourceDataLine line;
    private volatile boolean running = true;

    public AudioPlayer(SourceDataLine line) {
        this.line = line;
    }

    @Override
    public void run() {
        try {
            if (!line.isOpen()) {
                line.open();
            }
            line.start();

            // Generar y reproducir un tono de prueba
            AudioFormat format = line.getFormat();
            byte[] buffer = generateTestTone(format);

            int bytesPerFrame = format.getFrameSize();
            int bufferSize = line.getBufferSize();
            int chunkSize = bufferSize / 4;

            int offset = 0;
            while (running && offset < buffer.length) {
                int bytesToWrite = Math.min(chunkSize, buffer.length - offset);
                line.write(buffer, offset, bytesToWrite);
                offset += bytesToWrite;

                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    break;
                }
            }

            line.drain();

        } catch (LineUnavailableException e) {
            System.err.println(e.getMessage());
        } finally {
            line.stop();
        }
    }

    public void stopPlayback() {
        running = false;
        interrupt();
    }

    private byte[] generateTestTone(AudioFormat format) {
        int sampleRate = (int) format.getSampleRate();
        int duration = 3; // seconds
        int samples = sampleRate * duration;

        if (format.getSampleSizeInBits() == 16) {
            byte[] buffer = new byte[samples * format.getFrameSize()];
            for (int i = 0; i < samples; i++) {
                double time = i / (double) sampleRate;
                short sample = (short) (Short.MAX_VALUE * 0.3 * Math.sin(2.0 * Math.PI * 440 * time));

                int framePos = i * format.getFrameSize();
                buffer[framePos] = (byte) (sample & 0xFF);
                buffer[framePos + 1] = (byte) ((sample >> 8) & 0xFF);

                if (format.getChannels() == 2) {
                    buffer[framePos + 2] = buffer[framePos];
                    buffer[framePos + 3] = buffer[framePos + 1];
                }
            }
            return buffer;
        }

        return new byte[0];
    }
}

class AudioRecorder extends Thread {

    private TargetDataLine line;
    private volatile boolean recording = true;
    private ByteArrayOutputStream recordedData;

    public AudioRecorder(TargetDataLine line) {
        this.line = line;
        this.recordedData = new ByteArrayOutputStream();
    }

    @Override
    public void run() {
        try {
            if (!line.isOpen()) {
                line.open();
            }
            line.start();

            byte[] buffer = new byte[line.getBufferSize() / 4];

            while (recording) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                recordedData.write(buffer, 0, bytesRead);

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    break;
                }
            }

            line.stop();
            System.out.println("Grabación completada: " + recordedData.size() + " bytes");

        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void stopRecording() {
        recording = false;
        interrupt();
    }

    public byte[] getRecordedData() {
        return recordedData.toByteArray();
    }
}

// Interfaces para comunicación entre vista y controlador
interface MixerSelectionListener {

    void mixerSelected(Mixer.Info mixerInfo);
}

interface LineSelectionListener {

    void lineSelected(Line.Info lineInfo);
}

interface OperationListener {

    void playRequested();

    void recordRequested();

    void stopRequested();
}

// Renderers personalizados para mejorar la visualización
class MixerInfoRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof Mixer.Info info) {
            setText(info.getName() + " (" + info.getVendor() + ")");
        }

        return this;
    }
}

class LineInfoRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof Line.Info info) {
            String type = info.getLineClass().getSimpleName();
            String icon = "🔊";

            if (info.getLineClass() == SourceDataLine.class) {
                icon = "🔊"; // Reproducción
            } else if (info.getLineClass() == TargetDataLine.class) {
                icon = "🎤"; // Grabación
            } else if (info.getLineClass() == Clip.class) {
                icon = "🎵"; // Clip
            } else if (info.getLineClass() == Port.class) {
                icon = "🔌"; // Puerto
            }

            setText(icon + " " + type + " - " + info.toString());
        }

        return this;
    }
}

class AudioFormatRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof AudioFormat format) {
            StringBuilder sb = new StringBuilder();
            sb.append("🎼 ");
            sb.append((int) format.getSampleRate()).append(" Hz, ");
            sb.append(format.getSampleSizeInBits()).append(" bit, ");
            sb.append(format.getChannels() == 1 ? "Mono" : "Stereo");
            sb.append(format.isBigEndian() ? ", Big Endian" : ", Little Endian");
            setText(sb.toString());
        }

        return this;
    }
}

/**
 * Clase auxiliar para demostrar el trabajo con archivos de audio
 */
class AudioFileHandler {

    /**
     * Demuestra cómo obtener información de formatos de archivo soportados
     */
    public static void demonstrateAudioFileFormats() {
        System.out.println("=== FORMATOS DE ARCHIVO SOPORTADOS ===");

        // Obtener tipos de archivo soportados para lectura
        AudioFileFormat.Type[] readTypes = AudioSystem.getAudioFileTypes();
        System.out.println("Formatos de lectura soportados:");
        for (AudioFileFormat.Type type : readTypes) {
            System.out.println("  - " + type.toString() + " (." + type.getExtension() + ")");
        }

        // Demostrar cómo leer información de un archivo
        try {
            // Crear un archivo temporal para demostración
            File tempFile = createSampleAudioFile();
            if (tempFile != null && tempFile.exists()) {
                AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(tempFile);

                System.out.println("\nInformación del archivo:");
                System.out.println("  Tipo: " + fileFormat.getType());
                System.out.println("  Duración: " + fileFormat.getFrameLength() + " frames");
                System.out.println("  Formato: " + fileFormat.getFormat());

                tempFile.delete(); // Limpiar
            }
        } catch (IOException | UnsupportedAudioFileException e) {
            System.out.println("Error al crear archivo de demostración: " + e.getMessage());
        }
    }

    /**
     * Crea un archivo de audio de muestra para demostración
     */
    private static File createSampleAudioFile() {
        try {
            // Crear formato de audio estándar
            AudioFormat format = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED, // encoding
                    44100, // sample rate
                    16, // bits per sample
                    2, // channels
                    4, // frame size
                    44100, // frame rate
                    false // big endian
            );

            // Generar datos de audio (tono simple)
            int duration = 1; // 1 segundo
            int samples = (int) (format.getSampleRate() * duration);
            byte[] audioData = new byte[samples * format.getFrameSize()];

            for (int i = 0; i < samples; i++) {
                double time = i / format.getSampleRate();
                short sample = (short) (Short.MAX_VALUE * 0.3 * Math.sin(2.0 * Math.PI * 440 * time));

                int pos = i * format.getFrameSize();
                audioData[pos] = (byte) (sample & 0xFF);
                audioData[pos + 1] = (byte) ((sample >> 8) & 0xFF);
                audioData[pos + 2] = audioData[pos];     // Canal derecho igual que izquierdo
                audioData[pos + 3] = audioData[pos + 1];
            }

            // Crear stream de audio
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream audioStream = new AudioInputStream(bais, format, samples);

            // Escribir a archivo temporal
            File tempFile = File.createTempFile("java_sound_demo", ".wav");
            AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, tempFile);

            return tempFile;

        } catch (IOException e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    /**
     * Demuestra la conversión entre formatos de audio
     */
    public static void demonstrateFormatConversion() {
        System.out.println("\n=== CONVERSIÓN DE FORMATOS ===");

        try {
            // Formato origen
            AudioFormat sourceFormat = new AudioFormat(44100, 16, 2, true, false);

            // Formatos destino posibles
            AudioFormat[] targetFormats = {
                new AudioFormat(22050, 16, 2, true, false), // Reducir sample rate
                new AudioFormat(44100, 8, 2, true, false), // Reducir bits
                new AudioFormat(44100, 16, 1, true, false) // Mono
            };

            System.out.println("Formato origen: " + formatToString(sourceFormat));
            System.out.println("Conversiones soportadas:");

            for (AudioFormat target : targetFormats) {
                boolean supported = AudioSystem.isConversionSupported(target, sourceFormat);
                System.out.println("  -> " + formatToString(target) + ": "
                        + (supported ? "✓ Soportada" : "✗ No soportada"));
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static String formatToString(AudioFormat format) {
        return String.format("%.0f Hz, %d bit, %s",
                format.getSampleRate(),
                format.getSampleSizeInBits(),
                format.getChannels() == 1 ? "Mono" : "Stereo");
    }
}

/**
 * Clase para demostrar diferentes técnicas de control de líneas
 */
class LineControlDemo {

    /**
     * Demuestra el uso de controles en una línea de audio
     */
    public static void demonstrateLineControls(Line line) {
        if (line == null) {
            return;
        }

        System.out.println("=== CONTROLES DE LÍNEA ===");
        System.out.println("Línea: " + line.getLineInfo().getLineClass().getSimpleName());

        Control[] controls = line.getControls();
        System.out.println("Controles disponibles: " + controls.length);

        for (Control control : controls) {
            demonstrateControl(control);
        }
    }

    private static void demonstrateControl(Control control) {
        System.out.println("\n  Control: " + control.getType());

        switch (control) {
            case FloatControl floatControl -> {
                System.out.println("    Tipo: Control flotante");
                System.out.println("    Valor actual: " + floatControl.getValue());
                System.out.println("    Rango: " + floatControl.getMinimum() + " a " + floatControl.getMaximum());
                System.out.println("    Unidades: " + floatControl.getUnits());
                System.out.println("    Precisión: " + floatControl.getPrecision());

                // Ejemplo de uso: ajustar al 50%
                float range = floatControl.getMaximum() - floatControl.getMinimum();
                float middleValue = floatControl.getMinimum() + (range * 0.5f);
                System.out.println("    Configurando al 50%: " + middleValue);

            }
            case BooleanControl boolControl -> {
                System.out.println("    Tipo: Control booleano");
                System.out.println("    Valor actual: " + boolControl.getValue());
                System.out.println("    Etiqueta verdadero: " + boolControl.getStateLabel(true));
                System.out.println("    Etiqueta falso: " + boolControl.getStateLabel(false));

            }
            case EnumControl enumControl -> {
                System.out.println("    Tipo: Control de enumeración");
                System.out.println("    Valor actual: " + enumControl.getValue());
                System.out.print("    Valores posibles: ");
                Object[] values = enumControl.getValues();
                for (int i = 0; i < values.length; i++) {
                    System.out.print(values[i]);
                    if (i < values.length - 1) {
                        System.out.print(", ");
                    }
                }
                System.out.println();

            }
            case CompoundControl compound -> {
                System.out.println("    Tipo: Control compuesto");
                System.out.println("    Controles miembro:");
                for (Control member : compound.getMemberControls()) {
                    System.out.println("      - " + member.getType());
                }
            }
            default -> {
            }
        }
    }
}

/**
 * Extensión del controlador con funcionalidades avanzadas
 */
class AdvancedAudioOperations {

    /**
     * Demuestra grabación con diferentes formatos
     */
    public static void demonstrateAdvancedRecording(TargetDataLine line) {
        if (line == null || !line.isOpen()) {
            return;
        }

        System.out.println("=== GRABACIÓN AVANZADA ===");

        try {
            AudioFormat format = line.getFormat();
            System.out.println("Formato de grabación: " + format);

            // Crear buffer para grabación
            int bufferSize = (int) (format.getSampleRate() * format.getFrameSize());
            byte[] buffer = new byte[bufferSize];

            // Inicializar análisis de audio
            AudioAnalyzer analyzer = new AudioAnalyzer(format);

            line.start();

            // Grabar por 3 segundos con análisis en tiempo real
            long endTime = System.currentTimeMillis() + 3000;
            while (System.currentTimeMillis() < endTime) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    analyzer.processAudioData(buffer, bytesRead);
                }
            }

            line.stop();

            // Mostrar resultados del análisis
            analyzer.printAnalysis();

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Demuestra reproducción con efectos en tiempo real
     */
    public static void demonstrateEffectsPlayback(SourceDataLine line) {
        if (line == null || !line.isOpen()) {
            return;
        }

        System.out.println("=== REPRODUCCIÓN CON EFECTOS ===");

        try {
            AudioFormat format = line.getFormat();
            AudioEffectProcessor processor = new AudioEffectProcessor(format);

            // Generar audio base
            byte[] audioData = generateComplexTone(format, 3.0);

            // Aplicar efectos
            byte[] processedData = processor.applyReverb(audioData);

            line.start();

            // Reproducir con control de volumen dinámico
            int chunkSize = (int) (format.getSampleRate() * format.getFrameSize() * 0.1); // 100ms chunks
            for (int offset = 0; offset < processedData.length; offset += chunkSize) {
                int bytesToWrite = Math.min(chunkSize, processedData.length - offset);
                line.write(processedData, offset, bytesToWrite);

                // Simular cambio de volumen
                adjustLineVolume(line, 0.3f + 0.4f * (float) Math.sin(offset * 0.001));

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
            }

            line.drain();
            line.stop();

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private static byte[] generateComplexTone(AudioFormat format, double duration) {
        int sampleRate = (int) format.getSampleRate();
        int samples = (int) (sampleRate * duration);
        byte[] buffer = new byte[samples * format.getFrameSize()];

        // Generar acorde (múltiples frecuencias)
        double[] frequencies = {261.63, 329.63, 392.00}; // C Major chord

        for (int i = 0; i < samples; i++) {
            double time = i / (double) sampleRate;
            double sample = 0;

            // Sumar múltiples frecuencias
            for (double freq : frequencies) {
                sample += Math.sin(2.0 * Math.PI * freq * time) / frequencies.length;
            }

            // Aplicar envelope (fade in/out)
            double envelope = Math.min(time * 2, Math.min(1.0, (duration - time) * 2));
            sample *= envelope * 0.3;

            short shortSample = (short) (sample * Short.MAX_VALUE);

            // Escribir sample (estéreo)
            int pos = i * format.getFrameSize();
            buffer[pos] = (byte) (shortSample & 0xFF);
            buffer[pos + 1] = (byte) ((shortSample >> 8) & 0xFF);
            if (format.getChannels() == 2) {
                buffer[pos + 2] = buffer[pos];
                buffer[pos + 3] = buffer[pos + 1];
            }
        }

        return buffer;
    }

    private static void adjustLineVolume(SourceDataLine line, float volume) {
        try {
            FloatControl volumeControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float) (Math.log(Math.max(0.0001, volume)) / Math.log(10.0) * 20.0);
            volumeControl.setValue(Math.max(volumeControl.getMinimum(),
                    Math.min(volumeControl.getMaximum(), dB)));
        } catch (Exception e) {
            // Control no disponible
        }
    }
}

/**
 * Analizador de audio en tiempo real
 */
class AudioAnalyzer {

    private AudioFormat format;
    private double totalEnergy;
    private double maxAmplitude;
    private int sampleCount;

    public AudioAnalyzer(AudioFormat format) {
        this.format = format;
        this.totalEnergy = 0;
        this.maxAmplitude = 0;
        this.sampleCount = 0;
    }

    public void processAudioData(byte[] data, int length) {
        if (format.getSampleSizeInBits() == 16) {
            for (int i = 0; i < length - 1; i += 2) {
                short sample = (short) ((data[i + 1] << 8) | (data[i] & 0xFF));
                double amplitude = Math.abs(sample) / (double) Short.MAX_VALUE;

                totalEnergy += amplitude * amplitude;
                maxAmplitude = Math.max(maxAmplitude, amplitude);
                sampleCount++;
            }
        }
    }

    public void printAnalysis() {
        if (sampleCount > 0) {
            double rmsAmplitude = Math.sqrt(totalEnergy / sampleCount);
            double avgdB = 20 * Math.log10(Math.max(0.0001, rmsAmplitude));
            double peakdB = 20 * Math.log10(Math.max(0.0001, maxAmplitude));

            System.out.println("Análisis de audio:");
            System.out.println("  RMS: " + String.format("%.2f dB", avgdB));
            System.out.println("  Pico: " + String.format("%.2f dB", peakdB));
            System.out.println("  Muestras analizadas: " + sampleCount);
        }
    }
}

/**
 * Procesador de efectos de audio
 */
class AudioEffectProcessor {

    private AudioFormat format;
    private double[] delayBuffer;
    private int delayBufferSize;
    private int delayIndex;

    public AudioEffectProcessor(AudioFormat format) {
        this.format = format;
        // Buffer para reverb simple (250ms)
        this.delayBufferSize = (int) (format.getSampleRate() * 0.25);
        this.delayBuffer = new double[delayBufferSize];
        this.delayIndex = 0;
    }

    public byte[] applyReverb(byte[] inputData) {
        if (format.getSampleSizeInBits() != 16) {
            return inputData;
        }

        byte[] outputData = new byte[inputData.length];

        for (int i = 0; i < inputData.length - 1; i += 2) {
            // Leer sample
            short inputSample = (short) ((inputData[i + 1] << 8) | (inputData[i] & 0xFF));
            double input = inputSample / (double) Short.MAX_VALUE;

            // Obtener sample con delay
            double delayed = delayBuffer[delayIndex];

            // Mezclar con feedback
            double output = input + delayed * 0.3;

            // Guardar en buffer de delay
            delayBuffer[delayIndex] = input + delayed * 0.1;
            delayIndex = (delayIndex + 1) % delayBufferSize;

            // Convertir de vuelta a bytes
            short outputSample = (short) (output * Short.MAX_VALUE * 0.7);
            outputData[i] = (byte) (outputSample & 0xFF);
            outputData[i + 1] = (byte) ((outputSample >> 8) & 0xFF);
        }

        return outputData;
    }
}
