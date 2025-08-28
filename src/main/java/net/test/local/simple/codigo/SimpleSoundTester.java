/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
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
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 *
 * @author pc
 */
/**
 * Gestiona el estado de los dispositivos de audio y notifica cambios a la vista. Utiliza PropertyChangeSupport para mantener
 * sincronización en el patrón MVC.
 *
 * @see javax.sound.sampled.AudioSystem
 */
class SoundModel {

    // Propiedades que el modelo puede cambiar y notificar a los observadores
    public static final String SELECTED_MIXER_PROPERTY = "selectedMixer";
    public static final String AVAILABLE_LINES_PROPERTY = "availableLines";
    public static final String AVAILABLE_FORMATS_PROPERTY = "availableFormats";
    public static final String STATUS_MESSAGE_PROPERTY = "statusMessage";

    private Mixer selectedMixer;
    private List<Line.Info> availableLines;
    private List<AudioFormat> availableFormats;
    private String statusMessage;

    // Soporte para notificar a los listeners sobre cambios en las propiedades
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    public SoundModel() {
        availableLines = new ArrayList<>();
        availableFormats = new ArrayList<>();
        statusMessage = "Seleccione un mezclador, línea y formato para comenzar.";
    }

    // Métodos para añadir/eliminar listeners
    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        support.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        support.removePropertyChangeListener(pcl);
    }

    // --- Getters ---
    public Mixer getSelectedMixer() {
        return selectedMixer;
    }

    public List<Line.Info> getAvailableLines() {
        return availableLines;
    }

    public List<AudioFormat> getAvailableFormats() {
        return availableFormats;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    // --- Setters (llamados por el controlador para actualizar el estado) ---
    public void setSelectedMixer(Mixer mixer) {
        Mixer oldMixer = this.selectedMixer;
        this.selectedMixer = mixer;
        support.firePropertyChange(SELECTED_MIXER_PROPERTY, oldMixer, this.selectedMixer);
    }

    public void setAvailableLines(List<Line.Info> lines) {
        List<Line.Info> oldLines = new ArrayList<>(this.availableLines);
        this.availableLines = lines;
        support.firePropertyChange(AVAILABLE_LINES_PROPERTY, oldLines, new ArrayList<>(this.availableLines));
    }

    public void setAvailableFormats(List<AudioFormat> formats) {
        List<AudioFormat> oldFormats = new ArrayList<>(this.availableFormats);
        this.availableFormats = formats;
        support.firePropertyChange(AVAILABLE_FORMATS_PROPERTY, oldFormats, new ArrayList<>(this.availableFormats));
    }

    public void setStatusMessage(String message) {
        String oldMessage = this.statusMessage;
        this.statusMessage = message;
        // Disparar en el hilo de eventos de Swing para seguridad
        SwingUtilities.invokeLater(() -> support.firePropertyChange(STATUS_MESSAGE_PROPERTY, oldMessage, this.statusMessage));
    }

    // Método de lógica de negocio simple que permanece en el modelo (obtener datos iniciales)
    public Mixer.Info[] getMixerInfos() {
        return AudioSystem.getMixerInfo();
    }
}

/**
 * Interfaz gráfica que permite seleccionar dispositivos y formatos de audio. Incluye validación visual de compatibilidad
 * (ej.: deshabilita botón si faltan parámetros).
 *
 * @see javax.swing.JTree
 */
class SoundView extends JFrame {

    private final JTree mixerTree;
    private final JComboBox<Line.Info> lineComboBox;
    private final JList<AudioFormat> formatList;
    private final JButton playButton;
    private final JLabel statusLabel;

    // Getters para los componentes de la vista (para que el controlador añada listeners)
    public JTree getMixerTree() {
        return mixerTree;
    }

    public JComboBox<Line.Info> getLineComboBox() {
        return lineComboBox;
    }

    public JList<AudioFormat> getFormatList() {
        return formatList;
    }

    public JButton getPlayButton() {
        return playButton;
    }

    public JLabel getStatusLabel() {
        return statusLabel;
    }

    public SoundView() {
        super("Java Sound Configuración de reproducción.");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- Initialize Components ---
        mixerTree = new JTree();
        lineComboBox = new JComboBox<>();
        formatList = new JList<>();
        playButton = new JButton("Reproducir Tono");
        statusLabel = new JLabel("Inicializando...");

        // Prepara la apariencia
        mixerTree.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLoweredSoftBevelBorder(),
                        BorderFactory.createTitledBorder("Mezclador a usar:")
                )
        );
        // Configure list and combo box selection modes
        formatList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        formatList.setBorder(BorderFactory.createTitledBorder("Formatos:"));
        lineComboBox.setRenderer(new LineInfoRenderer()); // Custom renderer for better display
        lineComboBox.setBorder(BorderFactory.createTitledBorder("Líneas:"));

        // --- Layout Components ---
        JPanel selectionPanel = new JPanel(new GridLayout(1, 3));
        selectionPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createEtchedBorder(),
                        BorderFactory.createTitledBorder("Configurar Java Sound")
                )
        );
        selectionPanel.add(new JScrollPane(mixerTree));
        selectionPanel.add(new JScrollPane(lineComboBox));
        selectionPanel.add(new JScrollPane(formatList));

        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.add(playButton);
        controlPanel.add(statusLabel);

        add(selectionPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        // --- Final Setup ---
        setSize(800, 600);

        setLocationRelativeTo(null); // Center the window
        setVisible(true);

        // Set initial state
        playButton.setEnabled(false);
        playButton.setToolTipText("Genera y reproduce un tono de 440 Hz durante 1 segundo.");
        formatList.setModel(new DefaultListModel<>()); // Initialize list model
    }

    // Métodos para actualizar la vista basados en el modelo (llamados por el controlador)
    public void updateMixerTree(Mixer.Info[] mixerInfos) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Mezcladores Disponibles");
        if (mixerInfos.length == 0) {
            root.add(new DefaultMutableTreeNode("No se encontraron mezcladores."));
        } else {
            for (Mixer.Info info : mixerInfos) {
                root.add(new DefaultMutableTreeNode(info));
            }
        }
        mixerTree.setModel(new DefaultTreeModel(root));
    }

    public void updateLineComboBox(List<Line.Info> lineInfos) {
        lineComboBox.removeAllItems();
        if (lineInfos.isEmpty()) {
            lineComboBox.addItem(new Line.Info(Line.class) { // Add a placeholder
                @Override
                public boolean matches(Line.Info info) {
                    return false;
                }

                @Override
                public String toString() {
                    return "No SourceDataLines disponibles";
                }
            });
            lineComboBox.setEnabled(false);
        } else {
            lineComboBox.setEnabled(true);
            for (Line.Info info : lineInfos) {
                lineComboBox.addItem(info);
            }
        }
        // Asegurarse de que si hay ítems, el primero esté seleccionado por defecto
        if (lineComboBox.getItemCount() > 0) {
            lineComboBox.setSelectedIndex(0);
        } else {
            // Si no hay items, disparar el evento de cambio para limpiar la lista de formatos
            // Esto es un poco hacky, pero asegura que la lista de formatos se actualice correctamente.
            // Una alternativa sería que el controlador maneje explícitamente la actualización de formatos
            // cuando la lista de líneas cambia a vacía.
            lineComboBox.setSelectedItem(null); // Esto debería disparar el ActionListener
        }
    }

    public void updateFormatList(List<AudioFormat> formats) {
        DefaultListModel<AudioFormat> model = (DefaultListModel<AudioFormat>) formatList.getModel();
        model.clear();
        if (formats.isEmpty()) {
            model.addElement(new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, -1.0F, -1, -1, -1, -1.0F, false) {
                @Override
                public String toString() {
                    return "No se encontraron formatos compatibles.";
                }
            });
            formatList.setEnabled(false);
        } else {
            for (AudioFormat format : formats) {
                model.addElement(format);
            }
            formatList.setEnabled(true);
            // Seleccionar el primer formato por defecto si la lista no está vacía
            formatList.setSelectedIndex(0);
        }
    }

    public void updateStatusLabel(String message) {
        statusLabel.setText(message);
    }

    public void setPlayButtonEnabled(boolean enabled) {
        playButton.setEnabled(enabled);
    }

    /**
     * Custom renderer for Line.Info in JComboBox for better display.
     */
    private static class LineInfoRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Line.Info info) {
                label.setText(info.toString()); // Display the line info string
            }
            return label;
        }
    }
}

/**
 * Coordina la lógica de generación y reproducción de tonos. Implementa hilos para evitar bloqueos de la GUI durante la
 * reproducción.
 *
 * @see #playTone(Mixer, Line.Info, AudioFormat)
 */
class SoundController {

    private final SoundModel model;
    private final SoundView view;

    // Parameters for tone generation
    private static final int TONE_DURATION_SECONDS = 1; // Duration of the tone
    private static final double TONE_FREQUENCY = 440.0; // Frequency in Hz (A4 note)
    private static final double TONE_AMPLITUDE = 0.5; // Amplitude (0.0 to 1.0)

    // Frecuencias usuales a comprobar si getFormats() no es útil
    private static final float[] USUAL_SAMPLE_RATES = {
        8000.0F, 11025.0F, 16000.0F, 22050.0F, 44100.0F, 48000.0F, 88200.0F, 96000.0F
    };

    public SoundController(SoundModel model, SoundView view) {
        this.model = model;
        this.view = view;

        Initialize();
    }

    private void Initialize() {
        // --- Add Listeners to View Components (Manejo de Eventos de UI) ---
        view.getMixerTree().addTreeSelectionListener((TreeSelectionEvent e) -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) view.getMixerTree().getLastSelectedPathComponent();
            if (node != null && node.getUserObject() instanceof Mixer.Info) {
                Mixer selectedMixer = AudioSystem.getMixer((Mixer.Info) node.getUserObject());
                model.setSelectedMixer(selectedMixer); // Actualiza el modelo
                // Actualiza el modelo
                updateAvailableLines(selectedMixer); // Lógica en el controlador
            } else {
                model.setSelectedMixer(null); // Actualiza el modelo
                // Actualiza el modelo
                updateAvailableLines(null); // Lógica en el controlador
            }
            // Deshabilitar botón y limpiar formatos al cambiar de mezclador
            view.setPlayButtonEnabled(false);
            model.setAvailableFormats(new ArrayList<>());
        });
        view.getLineComboBox().addActionListener((ActionEvent e) -> {
            Line.Info selectedLineInfo = (Line.Info) view.getLineComboBox().getSelectedItem();
            // No necesitamos almacenar la línea seleccionada en el modelo,
            // la vista la gestiona. Solo necesitamos actualizar los formatos.
            updateAvailableFormats(selectedLineInfo); // Lógica en el controlador
            // Deshabilitar botón hasta que se seleccione un formato válido
            view.setPlayButtonEnabled(false);
        });
        view.getFormatList().addListSelectionListener((ListSelectionEvent e) -> {
            if (!e.getValueIsAdjusting()) {
                AudioFormat selectedFormat = view.getFormatList().getSelectedValue();
                // No necesitamos almacenar el formato seleccionado en el modelo, la vista lo gestiona.
                // Solo habilitamos el botón de reproducir si todo está seleccionado.
                view.setPlayButtonEnabled(model.getSelectedMixer() != null && view.getLineComboBox().getSelectedItem() instanceof Line.Info && selectedFormat != null && selectedFormat.getSampleRate() > 0
                        && // Asegurar que el formato es usable para tono
                        selectedFormat.getSampleSizeInBits() > 0 && selectedFormat.getChannels() > 0 && (selectedFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) || selectedFormat.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED)));
                model.setStatusMessage("Formato seleccionado: " + (selectedFormat != null ? selectedFormat.toString() : "Ninguno"));
            }
        });
        view.getPlayButton().addActionListener((ActionEvent e) -> {
            Mixer mixer = model.getSelectedMixer();
            Line.Info lineInfo = (Line.Info) view.getLineComboBox().getSelectedItem();
            AudioFormat format = view.getFormatList().getSelectedValue();
            if (mixer != null && lineInfo instanceof Line.Info && format != null) {
                // Ejecutar la reproducción en un hilo separado para no bloquear la GUI
                // La lógica de reproducción ahora está en el controlador
                new Thread(() -> {
                    try {
                        playTone(mixer, lineInfo, format);
                    } catch (LineUnavailableException ex) {
                        Logger.getLogger(SoundController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }).start();
            } else {
                model.setStatusMessage("Por favor, complete la selección antes de reproducir.");
            }
        });
        // --- Add Listeners to Model Properties (Vista observa al Modelo) ---
        // Estos listeners actualizan la vista cuando el modelo cambia
        model.addPropertyChangeListener(evt -> {
            switch (evt.getPropertyName()) {
                case SoundModel.SELECTED_MIXER_PROPERTY -> {
                }
                case SoundModel.AVAILABLE_LINES_PROPERTY -> // La vista actualiza la caja de líneas cuando el modelo le notifica
                    SwingUtilities.invokeLater(() -> view.updateLineComboBox((List<Line.Info>) evt.getNewValue()));
                case SoundModel.AVAILABLE_FORMATS_PROPERTY -> // La vista actualiza la lista de formatos cuando el modelo le notifica
                    SwingUtilities.invokeLater(() -> view.updateFormatList((List<AudioFormat>) evt.getNewValue()));
                case SoundModel.STATUS_MESSAGE_PROPERTY -> // La vista actualiza la etiqueta de estado cuando el modelo le notifica
                    SwingUtilities.invokeLater(() -> view.updateStatusLabel((String) evt.getNewValue()));
            }
            // La vista del árbol ya se actualiza por la selección del usuario,
            // no es necesario hacer nada aquí para el árbol.
        });
        // --- Inicializar Vista con Datos del Modelo ---
        view.updateMixerTree(model.getMixerInfos());
        model.setStatusMessage("Seleccione un mezclador."); // Mensaje de estado inicial
        // Mensaje de estado inicial
    }

    // --- Lógica de Manejo de Líneas y Formatos (Movida del Modelo al Controlador) ---
    private void updateAvailableLines(Mixer mixer) {
        List<Line.Info> availableLines = new ArrayList<>();
        if (mixer != null) {
            Line.Info[] sourceLineInfos = mixer.getSourceLineInfo();
            for (Line.Info info : sourceLineInfos) {
                if (info.getLineClass().equals(SourceDataLine.class)) {
                    availableLines.add(info);
                }
            }
        }
        model.setAvailableLines(availableLines); // Actualiza el modelo con las líneas encontradas
    }

    private void updateAvailableFormats(Line.Info lineInfo) {
        List<AudioFormat> availableFormats = new ArrayList<>();
        boolean foundUsableFormatFromLine = false;

        if (lineInfo instanceof DataLine.Info dataLineInfo) {
            AudioFormat[] formatsFromLine = dataLineInfo.getFormats();

            if (formatsFromLine != null && formatsFromLine.length > 0) {
                // Primero, intenta usar los formatos reportados directamente por la línea
                for (AudioFormat format : formatsFromLine) {
                    // Consideramos 'usable' si es PCM y tiene una tasa de muestreo positiva
                    if ((format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) || format.getEncoding().equals(AudioFormat.Encoding.PCM_UNSIGNED))
                            && format.getSampleRate() > 0
                            && format.getSampleSizeInBits() > 0
                            && format.getChannels() > 0) {
                        availableFormats.add(format);
                        foundUsableFormatFromLine = true;
                    }
                }
            }
        }

        // Si no se encontraron formatos PCM utilizables directamente de la línea, busca formatos usuales soportados por el mezclador
        if (!foundUsableFormatFromLine && model.getSelectedMixer() != null) {
            model.setStatusMessage("No se encontraron formatos PCM usuales para la línea. Buscando formatos comunes soportados por el mezclador...");
            List<AudioFormat> commonSupported = findSupportedUsualFormats(model.getSelectedMixer());
            if (!commonSupported.isEmpty()) {
                availableFormats.addAll(commonSupported);
                model.setStatusMessage("Encontrados formatos comunes soportados por el mezclador.");
            } else {
                model.setStatusMessage("No se encontraron formatos compatibles.");
            }
        } else if (availableFormats.isEmpty() && model.getSelectedMixer() != null) {
            // Si foundUsableFormatFromLine era true pero availableFormats terminó vacía (ej. solo había formatos no PCM)
            // Esto es para asegurar que si getFormats() no dio nada útil para el tono, busquemos usuales.
            List<AudioFormat> commonSupported = findSupportedUsualFormats(model.getSelectedMixer());
            if (!commonSupported.isEmpty()) {
                availableFormats.addAll(commonSupported);
                model.setStatusMessage("La línea reportó formatos, pero ninguno útil para tono. Buscando formatos comunes soportados...");
            } else {
                model.setStatusMessage("No se encontraron formatos compatibles.");
            }

        } else if (availableFormats.isEmpty()) {
            // Si lineInfo no era DataLine.Info o formatsFromLine era null/empty y no hay mezclador seleccionado
            model.setStatusMessage("No se encontraron formatos compatibles.");
        }

        model.setAvailableFormats(availableFormats); // Actualiza el modelo con los formatos encontrados
    }

    /**
     * Intenta encontrar AudioFormats comunes soportados por el mezclador dado utilizando Mixer.isLineSupported().
     *
     * @param mixer El mezclador a comprobar.
     * @return Una lista de AudioFormats comunes soportados.
     */
    private List<AudioFormat> findSupportedUsualFormats(Mixer mixer) {
        List<AudioFormat> supported = new ArrayList<>();
        if (mixer == null) {
            return supported; // No hay mezclador para comprobar
        }

        // Define algunos parámetros de formato comunes a probar
        int[] sampleSizes = {8, 16, 24, 32}; // Bits por muestra
        int[] channelsOptions = {1, 2}; // Mono, Stereo
        boolean[] endianOptions = {false, true}; // Little endian, Big endian (importante para 16+ bits)
        AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;

        for (float rate : USUAL_SAMPLE_RATES) {
            for (int size : sampleSizes) {
                for (int ch : channelsOptions) {
                    for (boolean endian : endianOptions) {
                        try {
                            // Construye el formato de prueba
                            AudioFormat testFormat = new AudioFormat(
                                    encoding,
                                    rate,
                                    size,
                                    ch,
                                    (size / 8) * ch, // Frame size
                                    rate, // Frame rate
                                    endian
                            );

                            // Crea un DataLine.Info para verificar el soporte
                            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, testFormat);

                            // Pregunta al mezclador si soporta una línea con este DataLine.Info
                            if (mixer.isLineSupported(dataLineInfo)) {
                                // Añade el formato si es soportado y aún no está en la lista
                                if (!supported.contains(testFormat)) {
                                    supported.add(testFormat);
                                }
                            }
                        } catch (IllegalArgumentException e) {
                            // Esto puede ocurrir si los parámetros combinados no son válidos
                            System.err.println("Error construyendo formato de prueba: " + e.getMessage());
                        }
                    }
                }
            }
        }
        return supported;
    }

    /**
     * Genera una onda senoidal en formato PCM según parámetros especificados.
     *
     * @param frequency Frecuencia en Hz (ej.: 440 para nota A4).
     * @param durationSeconds Duración en segundos.
     * @param format Formato de audio (PCM_SIGNED/PCM_UNSIGNED, tasa de muestreo, etc.).
     * @return Datos de audio en bytes listos para reproducción.
     * @throws IllegalArgumentException Si el formato no es compatible.
     *
     * @see AudioFormat
     */
    private byte[] generateSineWaveTone(double frequency, int durationSeconds, AudioFormat format) {
        // Validaciones esenciales para la generación de tono
        if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED && format.getEncoding() != AudioFormat.Encoding.PCM_UNSIGNED) {
            throw new IllegalArgumentException("Tone generation only supports PCM encoding.");
        }
        if (format.getSampleRate() <= 0) {
            throw new IllegalArgumentException("Invalid sample rate in AudioFormat: " + format.getSampleRate());
        }
        int sampleSizeInBits = format.getSampleSizeInBits();
        if (sampleSizeInBits <= 0 || (sampleSizeInBits % 8 != 0)) {
            throw new IllegalArgumentException("Invalid sample size in bits in AudioFormat: " + sampleSizeInBits);
        }
        int numChannels = format.getChannels();
        if (numChannels <= 0) {
            throw new IllegalArgumentException("Invalid number of channels in AudioFormat: " + numChannels);
        }

        int numSamples = (int) (durationSeconds * format.getSampleRate());
        int sampleSizeInBytes = sampleSizeInBits / 8;
        byte[] audioData = new byte[numSamples * sampleSizeInBytes * numChannels];

        double sampleRate = format.getSampleRate();
        boolean isSigned = format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED);
        boolean isBigEndian = format.isBigEndian();

        for (int i = 0; i < numSamples; i++) {
            double angle = 2.0 * Math.PI * frequency * i / sampleRate;
            double sample = TONE_AMPLITUDE * Math.sin(angle);

            // Convierte muestra double a byte(s) basado en format
            for (int channel = 0; channel < numChannels; channel++) {
                int byteIndex = (i * numChannels + channel) * sampleSizeInBytes;

                // Maneja diferentes tamaños de muestra y endianness
                switch (sampleSizeInBytes) {
                    case 1 -> {
                        // 8-bit
                        byte byteSample;
                        if (isSigned) {
                            byteSample = (byte) (sample * Byte.MAX_VALUE);
                        } else {
                            byteSample = (byte) (sample * 127.0 + 128.0); // 8-bit sin signo, rango 0 a 255
                        }
                        audioData[byteIndex] = byteSample;
                    }
                    case 2 -> {
                        // 16-bit
                        short shortSample = (short) (sample * Short.MAX_VALUE);
                        if (isBigEndian) {
                            audioData[byteIndex] = (byte) ((shortSample >>> 8) & 0xFF);
                            audioData[byteIndex + 1] = (byte) (shortSample & 0xFF);
                        } else {
                            audioData[byteIndex] = (byte) (shortSample & 0xFF);
                            audioData[byteIndex + 1] = (byte) ((shortSample >>> 8) & 0xFF);
                        }
                    }
                    case 3 -> {
                        // 24-bit (guarda como 3 bytes)
                        int intSample = (int) (sample * 8388607.0); // valor maximo para 24-bit con signo
                        if (isBigEndian) {
                            audioData[byteIndex] = (byte) ((intSample >>> 16) & 0xFF);
                            audioData[byteIndex + 1] = (byte) ((intSample >>> 8) & 0xFF);
                            audioData[byteIndex + 2] = (byte) (intSample & 0xFF);
                        } else {
                            audioData[byteIndex] = (byte) (intSample & 0xFF);
                            audioData[byteIndex + 1] = (byte) ((intSample >>> 8) & 0xFF);
                            audioData[byteIndex + 2] = (byte) ((intSample >>> 16) & 0xFF);
                        }
                    }
                    case 4 -> {
                        // 32-bit (int)
                        int intSample = (int) (sample * Integer.MAX_VALUE);
                        if (isBigEndian) {
                            audioData[byteIndex] = (byte) ((intSample >>> 24) & 0xFF);
                            audioData[byteIndex + 1] = (byte) ((intSample >>> 16) & 0xFF);
                            audioData[byteIndex + 2] = (byte) ((intSample >>> 8) & 0xFF);
                            audioData[byteIndex + 3] = (byte) (intSample & 0xFF);
                        } else {
                            audioData[byteIndex] = (byte) (intSample & 0xFF);
                            audioData[byteIndex + 1] = (byte) ((intSample >>> 8) & 0xFF);
                            audioData[byteIndex + 2] = (byte) ((intSample >>> 16) & 0xFF);
                            audioData[byteIndex + 3] = (byte) ((intSample >>> 24) & 0xFF);
                        }
                    }
                    default -> {
                    }
                }
                // Add more formats if needed (e.g., 64-bit float)
            }
        }
        return audioData;
    }

    /**
     * Plays the generated tone using the selected mixer, line, and format. This method is now in the Controller and called
     * in a separate thread.
     *
     * @param mixer The selected Mixer.
     * @param lineInfo The selected Line.Info.
     * @param format The selected AudioFormat.
     */
    private void playTone(Mixer mixer, Line.Info lineInfo, AudioFormat format) throws LineUnavailableException {
        model.setStatusMessage("Generando y reproduciendo tono...");

        SourceDataLine line = null;
        try {
            // Importante: Usar el DataLine.Info basado en el formato FINAL seleccionado.
            // Esto es necesario para abrir la línea específicamente con ese formato.
            DataLine.Info dataLineInfoForOpen = new DataLine.Info(lineInfo.getLineClass(), format);

            if (!mixer.isLineSupported(dataLineInfoForOpen)) {
                model.setStatusMessage("Error: El mezclador no soporta una línea de SourceDataLine con este formato específico: " + format.toString());
                return;
            }

            line = (SourceDataLine) mixer.getLine(dataLineInfoForOpen);
            line.open(format); // Abrir la línea con el formato seleccionado
            line.start();

            byte[] audioData = generateSineWaveTone(TONE_FREQUENCY, TONE_DURATION_SECONDS, format);
            line.write(audioData, 0, audioData.length);

            line.drain();
            line.stop();
            line.close();

            model.setStatusMessage("Reproducción de tono finalizada.");

        } catch (IllegalArgumentException e) {
            model.setStatusMessage("Error: Argumento inválido - " + e.getMessage());
        } catch (SecurityException e) {
            model.setStatusMessage("Error: Problema de seguridad - " + e.getMessage());
        }
        // Capturar otras excepciones inesperadas durante la reproducción

    }
}

// --- Main Application Class ---
public class SimpleSoundTester {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SoundModel model = new SoundModel();
            SoundView view = new SoundView();
            new SoundController(model, view);
        });
    }
}
