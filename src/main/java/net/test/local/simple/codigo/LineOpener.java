/**
 *
 * LineOpener - Selector Avanzado de Líneas de Audio
 *
 * Esta aplicación permite seleccionar y manipular diferentes tipos de líneas de audio
 * del sistema, incluyendo reproducción de archivos de audio y grabación desde
 * dispositivos de entrada.
 *
 * Características principales:
 * - Selección de mezcladores de audio del sistema
 * - Manejo de diferentes tipos de líneas: SourceDataLine, TargetDataLine y Ports
 * - Reproducción de archivos de audio con conversión automática de formatos
 * - Grabación de audio en archivos WAV con monitoreo en tiempo real
 * - Control de volumen y medidor de nivel de audio
 * - Configuración avanzada de parámetros de audio (sample rate, bits, canales)
 * - Detección automática de capacidades de dispositivos
 * - Interfaz gráfica intuitiva con indicadores visuales
 *
 * Uso:
 * 1. Seleccione un mezclador de audio del sistema
 * 2. Elija una línea específica (entrada, salida o puerto)
 * 3. Para reproducción: seleccione un archivo de audio y haga clic en "Abrir Línea"
 * 4. Para grabación: haga clic en "Abrir Línea" y seleccione ubicación de guardado
 * 5. Utilice el control deslizante para ajustar el volumen durante la operación
 * 6. Monitoree el nivel de audio en la barra de progreso
 *
 *
 * @author pc
 * @version 0.01b
 * @since 0.01b
 */
package net.test.local.simple.codigo;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Aplicación avanzada para selección y manipulación de líneas de audio del sistema.
 *
 * <p>
 * Esta clase proporciona una interfaz gráfica completa para trabajar con dispositivos de audio del sistema, permitiendo
 * reproducción de archivos y grabación de audio.</p>
 *
 * <p>
 * Ejemplo de uso básico:</p>
 * <pre>
 * {@code
 * public static void main(String[] args) {
 *     SwingUtilities.invokeLater(() -> {
 *         new LineOpener().setVisible(true);
 *     });
 * }
 * }
 * </pre>
 *
 */
public class LineOpener extends JFrame {

    /**
     * Logger para registro de eventos y errores
     */
    private static final Logger LOGGER = Logger.getLogger(LineOpener.class.getName());

    /**
     * Tamaño del buffer para operaciones de audio
     */
    private static final int BUFFER_SIZE = 8192;

    /**
     * Tasa de muestreo por defecto en Hz
     */
    private static final int DEFAULT_SAMPLE_RATE = 44100;

    /**
     * Tamaño de muestra por defecto en bits
     */
    private static final int DEFAULT_SAMPLE_SIZE = 16;

    /**
     * Número de canales por defecto
     */
    private static final int DEFAULT_CHANNELS = 2;

    // Componentes de la interfaz de usuario
    private JComboBox<Mixer.Info> mixerComboBox;
    private JComboBox<LineInfoWrapper> lineComboBox;
    private JButton openButton;
    private JButton fileButton;
    private JLabel statusLabel;
    private JSlider volumeSlider;
    private JProgressBar levelMeter;
    private JPanel formatPanel;
    private JComboBox<String> sampleRateCombo;
    private JComboBox<String> sampleSizeCombo;
    private JComboBox<String> channelsCombo;

    // Componentes de audio
    private File selectedFile;
    private volatile boolean playing = false;
    private SourceDataLine currentSourceLine;
    private TargetDataLine currentTargetLine;
    private FloatControl volumeControl;
    private final ExecutorService audioExecutor;
    private Future<?> currentAudioTask;

    // Monitoreo de nivel de audio
    private Timer levelUpdateTimer;
    private volatile float currentLevel = 0.0f;

    /**
     * Constructor principal que inicializa la aplicación. Configura la interfaz de usuario, carga los mezcladores de audio y
     * establece los manejadores de eventos.
     */
    public LineOpener() {
        super("LineOpener - Selector Avanzado de Líneas de Audio");
        audioExecutor = Executors.newSingleThreadExecutor();
        initializeUI();
        populateMixers();
        setupEventHandlers();

        LOGGER.info("Aplicación LineOpener iniciada correctamente");
    }

    /**
     * Inicializa todos los componentes de la interfaz de usuario. Configura el layout, crea los paneles y establece las
     * propiedades visuales.
     */
    private void initializeUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Panel principal con componentes organizados
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Selección de mezclador
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(new JLabel("Mezclador:", JLabel.RIGHT), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        mixerComboBox = new JComboBox<>();
        mixerComboBox.setToolTipText("Seleccione el dispositivo de audio");
        mainPanel.add(mixerComboBox, gbc);

        // Selección de línea
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Línea:", JLabel.RIGHT), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        lineComboBox = new JComboBox<>();
        lineComboBox.setToolTipText("Seleccione la línea de audio específica");
        mainPanel.add(lineComboBox, gbc);

        // Panel de formato de audio
        createFormatPanel();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        mainPanel.add(formatPanel, gbc);

        // Control de volumen
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Volumen:", JLabel.RIGHT), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        volumeSlider = new JSlider(0, 100, 75);
        volumeSlider.setMajorTickSpacing(25);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setPaintLabels(true);
        volumeSlider.setToolTipText("Ajustar volumen de reproducción/grabación");
        mainPanel.add(volumeSlider, gbc);

        // Medidor de nivel
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Nivel:", JLabel.RIGHT), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        levelMeter = new JProgressBar(0, 100);
        levelMeter.setStringPainted(true);
        levelMeter.setString("Silencio");
        levelMeter.setToolTipText("Nivel actual de audio");
        mainPanel.add(levelMeter, gbc);

        // Botones de control
        JPanel buttonPanel = new JPanel(new FlowLayout());
        openButton = new JButton("Abrir Línea");
        openButton.setPreferredSize(new Dimension(120, 30));
        fileButton = new JButton("Seleccionar Archivo");
        fileButton.setEnabled(false);
        fileButton.setPreferredSize(new Dimension(150, 30));

        buttonPanel.add(openButton);
        buttonPanel.add(fileButton);

        // Panel de estado
        statusLabel = new JLabel("Seleccione una línea para comenzar");
        statusLabel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        statusLabel.setHorizontalAlignment(JLabel.CENTER);

        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        add(statusLabel, BorderLayout.NORTH);

        // Configurar timer para actualización de niveles
        levelUpdateTimer = new Timer(50, e -> updateLevelMeter());

        setSize(600, 350);
        setLocationRelativeTo(null);
        setResizable(true);

        LOGGER.info("Interfaz de usuario inicializada");
    }

    /**
     * Crea el panel de configuración de formato de audio. Incluye controles para sample rate, tamaño de muestra y número de
     * canales.
     */
    private void createFormatPanel() {
        formatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        formatPanel.setBorder(BorderFactory.createTitledBorder("Formato de Audio"));

        formatPanel.add(new JLabel("Sample Rate:"));
        sampleRateCombo = new JComboBox<>(new String[]{"8000", "11025", "22050", "44100", "48000", "96000"});
        sampleRateCombo.setSelectedItem("44100");
        formatPanel.add(sampleRateCombo);

        formatPanel.add(new JLabel("Bits:"));
        sampleSizeCombo = new JComboBox<>(new String[]{"8", "16", "24"});
        sampleSizeCombo.setSelectedItem("16");
        formatPanel.add(sampleSizeCombo);

        formatPanel.add(new JLabel("Canales:"));
        channelsCombo = new JComboBox<>(new String[]{"1 (Mono)", "2 (Estéreo)"});
        channelsCombo.setSelectedItem("2 (Estéreo)");
        formatPanel.add(channelsCombo);
    }

    /**
     * Obtiene el formato de audio seleccionado en los controles de la interfaz.
     *
     * @return AudioFormat configurado según las selecciones del usuario
     */
    private AudioFormat getSelectedAudioFormat() {
        float sampleRate = Float.parseFloat((String) sampleRateCombo.getSelectedItem());
        int sampleSize = Integer.parseInt((String) sampleSizeCombo.getSelectedItem());
        int channels = channelsCombo.getSelectedIndex() + 1;

        return new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate,
                sampleSize,
                channels,
                (sampleSize / 8) * channels,
                sampleRate,
                false
        );
    }

    /**
     * Puebla el combo box de mezcladores con todos los mezcladores disponibles en el sistema.
     */
    private void populateMixers() {
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info info : mixerInfos) {
            mixerComboBox.addItem(info);
        }
        LOGGER.info(() -> "Cargados " + mixerInfos.length + " mezcladores de audio");
    }

    /**
     * Puebla el combo box de líneas con las líneas disponibles para un mezclador específico.
     *
     * @param mixerInfo Información del mezclador seleccionado
     */
    private void populateLines(Mixer.Info mixerInfo) {
        lineComboBox.removeAllItems();

        Mixer mixer = AudioSystem.getMixer(mixerInfo);
        // Líneas de salida (SourceDataLine)
        Line.Info[] sourceLineInfos = mixer.getSourceLineInfo();
        for (Line.Info info : sourceLineInfos) {
            lineComboBox.addItem(new LineInfoWrapper(info, LineType.SOURCE));
        }
        // Líneas de entrada (TargetDataLine)
        Line.Info[] targetLineInfos = mixer.getTargetLineInfo();
        for (Line.Info info : targetLineInfos) {
            lineComboBox.addItem(new LineInfoWrapper(info, LineType.TARGET));
        }
        // Puertos
        try {
            Line.Info[] portLineInfos = mixer.getSourceLineInfo(Port.Info.SPEAKER);
            for (Line.Info info : portLineInfos) {
                lineComboBox.addItem(new LineInfoWrapper(info, LineType.PORT));
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "No se encontraron puertos de altavoz", e);
        }
    }

    /**
     * Configura todos los manejadores de eventos para los componentes de la interfaz.
     */
    private void setupEventHandlers() {
        // Listener para cambio de mezclador
        mixerComboBox.addActionListener(e -> {
            Mixer.Info selectedMixer = (Mixer.Info) mixerComboBox.getSelectedItem();
            if (selectedMixer != null) {
                populateLines(selectedMixer);
            }
        });

        // Listener para cambio de línea
        lineComboBox.addActionListener(e -> {
            LineInfoWrapper selectedLine = (LineInfoWrapper) lineComboBox.getSelectedItem();
            boolean enableFileButton = selectedLine != null
                    && (selectedLine.type == LineType.SOURCE || selectedLine.type == LineType.TARGET);
            fileButton.setEnabled(enableFileButton);

            if (selectedLine != null) {
                updateStatus("Línea seleccionada: " + selectedLine.toString());
                // Actualizar automáticamente los parámetros según las capacidades de la línea
                updateFormatParametersForLine(selectedLine);
            }
        });

        // Listener para control de volumen
        volumeSlider.addChangeListener(e -> {
            if (volumeControl != null) {
                float volume = volumeSlider.getValue() / 100.0f;
                float gain = (float) (Math.log(volume == 0 ? 0.0001 : volume) / Math.log(10.0) * 20.0);
                volumeControl.setValue(gain);
            }
        });

        openButton.addActionListener(e -> openSelectedLine());
        fileButton.addActionListener(e -> selectAudioFile());

    }

    /**
     * Abre la línea de audio seleccionada por el usuario. Determina el tipo de línea y ejecuta la acción correspondiente.
     */
    private void openSelectedLine() {
        LineInfoWrapper selectedLineWrapper = (LineInfoWrapper) lineComboBox.getSelectedItem();
        if (selectedLineWrapper == null) {
            showError("No se ha seleccionado ninguna línea");
            return;
        }

        try {
            Mixer mixer = AudioSystem.getMixer((Mixer.Info) mixerComboBox.getSelectedItem());
            Line line = mixer.getLine(selectedLineWrapper.info);

            switch (selectedLineWrapper.type) {
                case PORT ->
                    handlePortLine(line);
                case SOURCE -> {
                    if (selectedLineWrapper.info instanceof DataLine.Info dli
                            && dli.getLineClass() == Clip.class) {
                        handleClipLine((Clip) mixer.getLine(selectedLineWrapper.info));
                    } else {
                        handleSourceDataLine((SourceDataLine) mixer.getLine(selectedLineWrapper.info));
                    }
                }
                case TARGET ->
                    handleTargetDataLine((TargetDataLine) line);
            }

        } catch (LineUnavailableException | IllegalArgumentException ex) {
            LOGGER.log(Level.WARNING, "Error al abrir línea", ex);
            showError("Error al abrir la línea: " + ex.getMessage());
        }
    }

    /**
     * Maneja una línea de tipo Port (puerto de audio).
     *
     * @param line Línea de tipo Port a abrir
     */
    private void handlePortLine(Line line) {
        try {
            line.open();
            updateStatus("Puerto abierto: " + line.getLineInfo());
            showInfo("Puerto abierto exitosamente");
            LOGGER.info(() -> "Puerto abierto: " + line.getLineInfo());
        } catch (LineUnavailableException e) {
            LOGGER.log(Level.WARNING, "Error al abrir puerto", e);
            showError("Error al abrir puerto: " + e.getMessage());
        }
    }

    /**
     * Maneja una línea de tipo SourceDataLine (salida de audio).
     *
     * @param line Línea de salida de audio
     */
    private void handleSourceDataLine(SourceDataLine line) {
        if (selectedFile == null) {
            showError("Seleccione primero un archivo de audio");
            return;
        }

        currentAudioTask = audioExecutor.submit(() -> {
            try {
                playAudioFile(line, selectedFile);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error en reproducción", e);
                SwingUtilities.invokeLater(() -> showError("Error en reproducción: " + e.getMessage()));
            }
        });
    }

    private void handleClipLine(Clip clip) {
        if (selectedFile == null) {
            showError("Select an audio file first");
            return;
        }

        currentAudioTask = audioExecutor.submit(() -> {
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(selectedFile)) {
                clip.open(ais);

                setupVolumeControl(clip);      // re-uses the same slider
                clip.start();

                SwingUtilities.invokeLater(() -> {
                    openButton.setText("Stop Clip");
                    updateStatus("Playing via Clip: " + selectedFile.getName());
                    levelUpdateTimer.start();
                });

                // Monitor level – Clip gives us no live stream, so we fake it
                while (clip.isRunning() && playing) {
                    // calculateAudioLevelDummy();   // optional fake meter
                    Thread.sleep(50);
                }

                clip.drain();
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Clip error", ex);
                SwingUtilities.invokeLater(() -> showError("Clip error: " + ex.getMessage()));
            }
        });
    }

    /**
     * Reproduce un archivo de audio a través de una línea de salida.
     *
     * @param line Línea de salida de audio
     * @param audioFile Archivo de audio a reproducir
     */
    private void playAudioFile(SourceDataLine line, File audioFile) {
        try {
            playing = true;
            currentSourceLine = line;

            SwingUtilities.invokeLater(() -> {
                openButton.setText("Detener Reproducción");
                updateStatus("Preparando reproducción...");
            });

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat fileFormat = audioStream.getFormat();
            AudioFormat playFormat = getCompatibleFormat(fileFormat, line);

            if (!fileFormat.equals(playFormat)) {
                audioStream = AudioSystem.getAudioInputStream(playFormat, audioStream);
            }

            line.open(playFormat);

            // Configurar control de volumen si está disponible
            setupVolumeControl(line);

            line.start();
            levelUpdateTimer.start();

            SwingUtilities.invokeLater(() -> updateStatus("Reproduciendo: " + audioFile.getName()));

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = audioStream.read(buffer)) != -1 && playing) {
                line.write(buffer, 0, bytesRead);

                // Calcular nivel de audio para el medidor
                calculateAudioLevel(buffer, bytesRead);
            }

            line.drain();
            line.stop();
            line.close();
            audioStream.close();

            SwingUtilities.invokeLater(() -> updateStatus("Reproducción completada"));
            LOGGER.info(() -> "Reproducción completada: " + audioFile.getName());

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            LOGGER.log(Level.SEVERE, "Error durante reproducción", e);
            SwingUtilities.invokeLater(() -> showError("Error durante reproducción: " + e.getMessage()));
        }
    }

    /**
     * Maneja una línea de tipo TargetDataLine (entrada de audio).
     *
     * @param line Línea de entrada de audio
     */
    private void handleTargetDataLine(TargetDataLine line) {
        File saveFile = selectSaveFile();
        if (saveFile == null) {
            return;
        }

        currentAudioTask = audioExecutor.submit(() -> {
            try {
                recordAudio(line, saveFile);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error en grabación", e);
                SwingUtilities.invokeLater(() -> showError("Error en grabación: " + e.getMessage()));
            }
        });
    }

    /**
     * Graba audio desde una línea de entrada a un archivo.
     *
     * @param line Línea de entrada de audio
     * @param saveFile Archivo donde se guardará la grabación
     */
    private void recordAudio(TargetDataLine line, File saveFile) {
        try {
            playing = true;
            currentTargetLine = line;

            SwingUtilities.invokeLater(() -> {
                openButton.setText("Detener Grabación");
                updateStatus("Iniciando grabación...");
            });

            AudioFormat format = getSelectedAudioFormat();
            line.open(format);
            line.start();
            levelUpdateTimer.start();

            AudioInputStream audioInputStream = new AudioInputStream(line);

            SwingUtilities.invokeLater(()
                    -> updateStatus("Grabando en: " + saveFile.getName())
            );

            // Escribir en un hilo separado para evitar bloqueos
            Thread writeThread = new Thread(() -> {
                try {
                    AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, saveFile);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error al escribir archivo de audio", e);
                    SwingUtilities.invokeLater(()
                            -> showError("Error al guardar archivo: " + e.getMessage())
                    );
                }
            });
            writeThread.start();

            // Monitorear niveles durante la grabación
            byte[] buffer = new byte[BUFFER_SIZE];
            while (playing) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    calculateAudioLevel(buffer, bytesRead);
                }
            }

            line.stop();
            line.close();
            audioInputStream.close();

            writeThread.join(2000);

            SwingUtilities.invokeLater(()
                    -> updateStatus("Grabación completada: " + saveFile.getName())
            );
            LOGGER.info(() -> "Grabación completada: " + saveFile.getName());

        } catch (LineUnavailableException | IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Error durante grabación", e);
            SwingUtilities.invokeLater(() -> showError("Error durante grabación: " + e.getMessage()));
        }
    }

    /**
     * Obtiene un formato de audio compatible entre el archivo y la línea de salida.
     *
     * @param fileFormat Formato del archivo de audio
     * @param line Línea de salida de audio
     * @return AudioFormat compatible
     */
    private AudioFormat getCompatibleFormat(AudioFormat fileFormat, SourceDataLine line) {

        // Si el formato del archivo es compatible, usarlo directamente
        if (((DataLine.Info) line.getLineInfo()).isFormatSupported(fileFormat)) {
            return fileFormat;
        }

        // Intentar crear un formato compatible
        return new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                fileFormat.getSampleRate(),
                16, // Bits por muestra estándar
                fileFormat.getChannels(),
                2 * fileFormat.getChannels(), // Frame size
                fileFormat.getFrameRate(),
                false // Little endian
        );
    }

    /**
     * Configura el control de volumen para una línea de audio si está disponible.
     *
     * @param line Línea de audio
     */
    private void setupVolumeControl(Line line) {
        try {
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                volumeControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            } else if (line.isControlSupported(FloatControl.Type.VOLUME)) {
                volumeControl = (FloatControl) line.getControl(FloatControl.Type.VOLUME);
            }

            if (volumeControl != null) {
                SwingUtilities.invokeLater(() -> volumeSlider.setEnabled(true));
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Control de volumen no disponible", e);
        }
    }

    /**
     * Calcula el nivel de audio a partir de los datos de audio.
     *
     * @param buffer Buffer de datos de audio
     * @param length Longitud de datos válidos en el buffer
     */
    private void calculateAudioLevel(byte[] buffer, int length) {
        long sum = 0;
        for (int i = 0; i < length; i += 2) {
            if (i + 1 < length) {
                short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
                sum += Math.abs(sample);
            }
        }

        if (length > 0) {
            currentLevel = (float) sum / (length / 2) / Short.MAX_VALUE;
        }
    }

    /**
     * Actualiza el medidor de nivel de audio en la interfaz.
     */
    private void updateLevelMeter() {
        int level = Math.min(100, (int) (currentLevel * 100));
        levelMeter.setValue(level);

        String levelText = level > 50 ? "Alto" : level > 20 ? "Medio" : level > 0 ? "Bajo" : "Silencio";
        levelMeter.setString(levelText + " (" + level + "%)");

        // Cambiar color según el nivel
        if (level > 80) {
            levelMeter.setForeground(Color.RED);
        } else if (level > 50) {
            levelMeter.setForeground(Color.ORANGE);
        } else {
            levelMeter.setForeground(Color.GREEN);
        }
    }

    /**
     * Muestra un diálogo para seleccionar la ubicación y nombre del archivo de grabación.
     *
     * @return File seleccionado por el usuario, o null si se cancela
     */
    private File selectSaveFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccionar ubicación para guardar la grabación");

        FileNameExtensionFilter[] filters = {
            new FileNameExtensionFilter("WAV Audio", "wav"),
            new FileNameExtensionFilter("AIFF Files", "aiff"),
            new FileNameExtensionFilter("AU Files", "au")
        };

        for (FileNameExtensionFilter filter : filters) {
            fileChooser.addChoosableFileFilter(filter);
        }

        fileChooser.setSelectedFile(new File("grabacion_" + System.currentTimeMillis() + ".wav"));

        int result = fileChooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File saveFile = fileChooser.getSelectedFile();

        // Asegurar extensión correcta
        if (!saveFile.getName().toLowerCase().matches(".*\\.(wav|aiff|au)$")) {
            saveFile = new File(saveFile.getAbsolutePath() + ".wav");
        }

        // Confirmar sobreescritura si existe
        if (saveFile.exists()) {
            int response = JOptionPane.showConfirmDialog(this,
                    "El archivo ya existe. ¿Desea sobreescribirlo?",
                    "Archivo existente",
                    JOptionPane.YES_NO_OPTION);
            if (response != JOptionPane.YES_OPTION) {
                return null;
            }
        }

        return saveFile;
    }

    /**
     * Actualiza automáticamente los parámetros de formato según las capacidades detectadas de la línea seleccionada.
     *
     * @param selectedLineWrapper Información de la línea seleccionada
     */
    private void updateFormatParametersForLine(LineInfoWrapper selectedLineWrapper) {
        if (selectedLineWrapper == null) {
            return;
        }

        try {
            Mixer mixer = AudioSystem.getMixer((Mixer.Info) mixerComboBox.getSelectedItem());

            if (selectedLineWrapper.info instanceof DataLine.Info dataLineInfo) {
                AudioFormat[] supportedFormats = dataLineInfo.getFormats();

                if (supportedFormats.length > 0) {
                    // Encontrar el mejor formato disponible
                    AudioFormat preferredFormat = findPreferredFormat(supportedFormats);
                    updateFormatControls(preferredFormat, supportedFormats);

                    // Actualizar estado con información de capacidades
                    updateStatus(String.format("Línea detectada: %d formatos soportados. Formato recomendado: %.0f Hz, %d bits, %d canales",
                            supportedFormats.length,
                            preferredFormat.getSampleRate(),
                            preferredFormat.getSampleSizeInBits(),
                            preferredFormat.getChannels()));

                    LOGGER.info(String.format("Capacidades detectadas para línea %s: %d formatos disponibles",
                            selectedLineWrapper.info.toString(), supportedFormats.length));
                } else {
                    updateStatus("Línea seleccionada - capacidades de formato limitadas");
                }

                // Verificar controles disponibles
                try {
                    Line line = mixer.getLine(selectedLineWrapper.info);
                    detectAvailableControls(line);
                } catch (LineUnavailableException e) {
                    LOGGER.log(Level.FINE, "No se pudo abrir línea para detectar controles", e);
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error al detectar capacidades de línea", e);
            updateStatus("Error al detectar capacidades: " + e.getMessage());
        }
    }

    /**
     * Encuentra el formato preferido entre los disponibles, priorizando calidad de audio estándar (44.1kHz, 16-bit,
     * estéreo).
     *
     * @param supportedFormats Formatos soportados por la línea
     * @return AudioFormat preferido
     */
    private AudioFormat findPreferredFormat(AudioFormat[] supportedFormats) {
        AudioFormat preferred = supportedFormats[0]; // Por defecto el primero

        for (AudioFormat format : supportedFormats) {
            // Priorizar PCM signed
            if (format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED) {
                // Priorizar 44.1 kHz
                if (format.getSampleRate() == 44100.0f || format.getSampleRate() == AudioSystem.NOT_SPECIFIED) {
                    // Priorizar 16 bits
                    if (format.getSampleSizeInBits() == 16 || format.getSampleSizeInBits() == AudioSystem.NOT_SPECIFIED) {
                        // Priorizar estéreo
                        if (format.getChannels() == 2 || format.getChannels() == AudioSystem.NOT_SPECIFIED) {
                            preferred = format;
                            break;
                        }
                        preferred = format;
                    }
                }
            }
        }

        return preferred;
    }

    /**
     * Actualiza los controles de formato basado en las capacidades detectadas.
     *
     * @param preferredFormat Formato preferido
     * @param allFormats Todos los formatos soportados
     */
    private void updateFormatControls(AudioFormat preferredFormat, AudioFormat[] allFormats) {
        SwingUtilities.invokeLater(() -> {
            // Recopilar todas las tasas de muestreo soportadas
            java.util.Set<String> supportedRates = new java.util.LinkedHashSet<>();
            java.util.Set<String> supportedBits = new java.util.LinkedHashSet<>();
            java.util.Set<String> supportedChannels = new java.util.LinkedHashSet<>();

            for (AudioFormat format : allFormats) {
                if (format.getSampleRate() != AudioSystem.NOT_SPECIFIED) {
                    supportedRates.add(String.valueOf((int) format.getSampleRate()));
                }
                if (format.getSampleSizeInBits() != AudioSystem.NOT_SPECIFIED) {
                    supportedBits.add(String.valueOf(format.getSampleSizeInBits()));
                }
                if (format.getChannels() != AudioSystem.NOT_SPECIFIED) {
                    String channelText = format.getChannels() == 1 ? "1 (Mono)"
                            : format.getChannels() == 2 ? "2 (Estéreo)"
                            : format.getChannels() + " canales";
                    supportedChannels.add(channelText);
                }
            }

            // Actualizar combo boxes con valores soportados
            updateComboBoxWithSupportedValues(sampleRateCombo, supportedRates,
                    preferredFormat.getSampleRate() != AudioSystem.NOT_SPECIFIED
                    ? String.valueOf((int) preferredFormat.getSampleRate()) : "44100");

            updateComboBoxWithSupportedValues(sampleSizeCombo, supportedBits,
                    preferredFormat.getSampleSizeInBits() != AudioSystem.NOT_SPECIFIED
                    ? String.valueOf(preferredFormat.getSampleSizeInBits()) : "16");

            updateComboBoxWithSupportedValues(channelsCombo, supportedChannels,
                    preferredFormat.getChannels() == 1 ? "1 (Mono)"
                    : preferredFormat.getChannels() == 2 ? "2 (Estéreo)" : "2 (Estéreo)");

            // Deshabilitar controles si solo hay una opción
            sampleRateCombo.setEnabled(supportedRates.size() > 1);
            sampleSizeCombo.setEnabled(supportedBits.size() > 1);
            channelsCombo.setEnabled(supportedChannels.size() > 1);
        });
    }

    /**
     * Actualiza un combo box con los valores soportados.
     *
     * @param comboBox Combo box a actualizar
     * @param supportedValues Valores soportados
     * @param preferredValue Valor preferido
     */
    private void updateComboBoxWithSupportedValues(JComboBox<String> comboBox,
            java.util.Set<String> supportedValues,
            String preferredValue) {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();

        // Si hay valores específicos soportados, usarlos
        if (!supportedValues.isEmpty()) {
            for (String value : supportedValues) {
                model.addElement(value);
            }
        } else {
            // Si no hay restricciones específicas, usar valores por defecto
            if (comboBox == sampleRateCombo) {
                model.addElement("8000");
                model.addElement("11025");
                model.addElement("22050");
                model.addElement("44100");
                model.addElement("48000");
                model.addElement("96000");
            } else if (comboBox == sampleSizeCombo) {
                model.addElement("8");
                model.addElement("16");
                model.addElement("24");
            } else if (comboBox == channelsCombo) {
                model.addElement("1 (Mono)");
                model.addElement("2 (Estéreo)");
            }
        }

        comboBox.setModel(model);

        // Seleccionar el valor preferido si está disponible
        for (int i = 0; i < model.getSize(); i++) {
            if (model.getElementAt(i).equals(preferredValue)) {
                comboBox.setSelectedIndex(i);
                break;
            }
        }
    }

    /**
     * Detecta los controles disponibles en una línea.
     *
     * @param line Línea de audio
     */
    private void detectAvailableControls(Line line) {
        try {
            try (line) {
                if (!(line instanceof Clip)) {
                    line.open();
                }

                Control[] controls = line.getControls();
                StringBuilder controlInfo = new StringBuilder("Controles disponibles: ");

                boolean hasVolumeControl = false;
                for (Control control : controls) {
                    controlInfo.append(control.getType().toString()).append(", ");

                    if (control instanceof FloatControl floatControl) {
                        if (floatControl.getType() == FloatControl.Type.MASTER_GAIN
                                || floatControl.getType() == FloatControl.Type.VOLUME) {
                            hasVolumeControl = true;
                        }
                    }
                }

                final boolean volumeAvailable = hasVolumeControl;
                SwingUtilities.invokeLater(() -> {
                    if (!volumeAvailable) {
                        volumeSlider.setEnabled(false);
                        volumeSlider.setToolTipText("Control de volumen no disponible para esta línea");
                    } else {
                        volumeSlider.setEnabled(true);
                        volumeSlider.setToolTipText("Ajustar volumen de reproducción/grabación");
                    }
                });

                if (controls.length > 0) {
                    String finalControlInfo = controlInfo.substring(0, controlInfo.length() - 2);
                    LOGGER.info(finalControlInfo);
                }
            }

        } catch (LineUnavailableException e) {
            LOGGER.log(Level.FINE, "No se pudieron detectar controles", e);
        }
    }

    /**
     * Muestra un diálogo para seleccionar un archivo de audio.
     */
    private void selectAudioFile() {
        LineInfoWrapper selectedLine = (LineInfoWrapper) lineComboBox.getSelectedItem();

        if (selectedLine != null && selectedLine.type == LineType.SOURCE) {
            JFileChooser fileChooser = new JFileChooser();

            AudioFileFormat.Type[] supportedTypes = AudioSystem.getAudioFileTypes();
            if (supportedTypes.length > 0) {
                String[] extensions = Arrays.stream(supportedTypes)
                        .map(AudioFileFormat.Type::getExtension)
                        .toArray(String[]::new);

                FileNameExtensionFilter filter = new FileNameExtensionFilter(
                        "Archivos de Audio (" + String.join(", ", extensions).toUpperCase() + ")",
                        extensions
                );
                fileChooser.setFileFilter(filter);
            }

            fileChooser.setDialogTitle("Seleccionar archivo de audio para reproducir");
            int result = fileChooser.showOpenDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                updateStatus("Archivo seleccionado: " + selectedFile.getName());
                LOGGER.info(() -> "Archivo seleccionado: " + selectedFile.getAbsolutePath());
            }
        } else if (selectedLine != null && selectedLine.type == LineType.TARGET) {
            updateStatus("Use 'Abrir Línea' para comenzar la grabación");
        }
    }

    /**
     * Actualiza el texto del label de estado.
     *
     * @param message Mensaje a mostrar
     */
    private void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

    /**
     * Muestra un diálogo de error.
     *
     * @param message Mensaje de error
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Muestra un diálogo de información.
     *
     * @param message Mensaje de información
     */
    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Información", JOptionPane.INFORMATION_MESSAGE);
    }

    // Enumeraciones y clases auxiliares
    /**
     * Tipos de líneas de audio disponibles.
     */
    private enum LineType {
        /**
         * Puerto de audio (altavoces, micrófonos, etc.)
         */
        PORT("Puerto"),
        /**
         * Línea de salida de audio (reproducción)
         */
        SOURCE("Salida"),
        /**
         * Línea de entrada de audio (grabación)
         */
        TARGET("Entrada"),
        /**
         * Clip (almacenamiento en memoria del audio)
         */
        CLIP("Clip");

        private final String displayName;

        LineType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * Wrapper para encapsular información de línea con su tipo.
     */
    private static class LineInfoWrapper {

        /**
         * Información de la línea de audio
         */
        final Line.Info info;

        /**
         * Tipo de línea
         */
        final LineType type;

        /**
         * Constructor.
         *
         * @param info Información de la línea
         * @param type Tipo de línea
         */
        LineInfoWrapper(Line.Info info, LineType type) {
            this.info = info;
            this.type = type;
        }

        @Override
        public String toString() {
            return String.format("%s (%s)", info.toString(), type.toString());
        }
    }

    /**
     * Método principal de la aplicación.
     *
     * @param args Argumentos de línea de comandos
     */
    public static void main(String[] args) {
        // Configurar look and feel del sistema
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
            LOGGER.log(Level.ALL, "No se pudo configurar el look and feel del sistema", e);
        }

        SwingUtilities.invokeLater(() -> {
            try {
                new LineOpener().setVisible(true);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error al inicializar la aplicación", e);
                JOptionPane.showMessageDialog(null,
                        "Error al inicializar la aplicación: " + e.getMessage(),
                        "Error Fatal",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }

}
