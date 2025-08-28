/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

public class LineOpener extends JFrame {

    private JComboBox<Mixer.Info> mixerComboBox;
    private JComboBox<LineInfoWrapper> lineComboBox;
    private JButton openButton;
    private JButton fileButton;
    private JLabel statusLabel;
    private File selectedFile;
    private volatile boolean playing = false;
    private SourceDataLine currentSourceLine;
    private TargetDataLine currentTargetLine;

    public LineOpener() {
        super("LineOpener - Selector de Líneas de Audio");
        initializeUI();
        populateMixers();
        setupEventHandlers();
    }

    private void initializeUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(4, 2, 2, 2));

        add(new JLabel("Mezclador:", JLabel.RIGHT));
        mixerComboBox = new JComboBox<>();
        add(mixerComboBox);

        add(new JLabel("Línea:", JLabel.RIGHT));
        lineComboBox = new JComboBox<>();
        add(lineComboBox);

        openButton = new JButton("Abrir Línea");
        add(openButton);

        fileButton = new JButton("Seleccionar Archivo");
        fileButton.setEnabled(false);
        add(fileButton);

        statusLabel = new JLabel("Seleccione una línea");
        statusLabel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        add(statusLabel);

        setSize(460, 210);
        setLocationRelativeTo(null);
    }

    private void populateMixers() {
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info info : mixerInfos) {
            mixerComboBox.addItem(info);
        }
    }

    private void populateLines(Mixer.Info mixerInfo) {
        lineComboBox.removeAllItems();
        Mixer mixer = AudioSystem.getMixer(mixerInfo);

        Line.Info[] sourceLineInfos = mixer.getSourceLineInfo();
        for (Line.Info info : sourceLineInfos) {
            lineComboBox.addItem(new LineInfoWrapper(info, LineType.SOURCE));
        }

        Line.Info[] targetLineInfos = mixer.getTargetLineInfo();
        for (Line.Info info : targetLineInfos) {
            lineComboBox.addItem(new LineInfoWrapper(info, LineType.TARGET));
        }

        Line.Info[] portLineInfos = mixer.getSourceLineInfo(Port.Info.SPEAKER);
        for (Line.Info info : portLineInfos) {
            lineComboBox.addItem(new LineInfoWrapper(info, LineType.PORT));
        }
    }

    private void setupEventHandlers() {
        mixerComboBox.addActionListener(e -> {
            Mixer.Info selectedMixer = (Mixer.Info) mixerComboBox.getSelectedItem();
            if (selectedMixer != null) {
                populateLines(selectedMixer);
            }
        });

        lineComboBox.addActionListener(e -> {
            LineInfoWrapper selectedLine = (LineInfoWrapper) lineComboBox.getSelectedItem();
            fileButton.setEnabled(selectedLine != null
                    && (selectedLine.type == LineType.SOURCE || selectedLine.type == LineType.TARGET));
        });

        openButton.addActionListener(e -> openSelectedLine());
        fileButton.addActionListener(e -> selectAudioFile());
    }

    private void openSelectedLine() {
        LineInfoWrapper selectedLineWrapper = (LineInfoWrapper) lineComboBox.getSelectedItem();
        if (selectedLineWrapper == null) {
            return;
        }

        // Si ya está reproduciendo, detener la reproducción
        if (playing) {
            stopPlayback();
            return;
        }

        try {
            Mixer mixer = AudioSystem.getMixer((Mixer.Info) mixerComboBox.getSelectedItem());
            Line line = mixer.getLine(selectedLineWrapper.info);

            switch (selectedLineWrapper.type) {
                case PORT -> {
                    line.open();
                    statusLabel.setText("Puerto abierto: " + line.getLineInfo());
                    JOptionPane.showMessageDialog(this, "Puerto abierto exitosamente");
                }
                case SOURCE ->
                    handleSourceDataLine((SourceDataLine) line);
                case TARGET ->
                    handleTargetDataLine((TargetDataLine) line);
            }

        } catch (LineUnavailableException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Error al abrir la línea: " + ex.getMessage());
        }
    }

    private void handleSourceDataLine(SourceDataLine line) {
        if (selectedFile == null) {
            JOptionPane.showMessageDialog(this, "Seleccione primero un archivo de audio");
            return;
        }

        if (playing) {
            JOptionPane.showMessageDialog(this, "Ya se está reproduciendo un archivo.");
            return;
        }

        // Ejecutar en hilo secundario
        new Thread(() -> {
            try {
                playing = true;
                currentSourceLine = line; // Guardar referencia a la línea actual
                updateUI("Iniciando reproducción...");
                SwingUtilities.invokeLater(() -> openButton.setText("Detener Línea"));

                AudioInputStream audioStream = prepareAudioStream(selectedFile, line);
                if (audioStream == null) {
                    playing = false;
                    SwingUtilities.invokeLater(() -> openButton.setText("Abrir Línea"));
                    return;
                }

                line.open(audioStream.getFormat());
                line.start();

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = audioStream.read(buffer)) != -1 && playing) {
                    line.write(buffer, 0, bytesRead);
                }

                line.drain();
                line.stop();
                line.close();
                audioStream.close();

                updateUI("Reproducción completada");
            } catch (IOException | LineUnavailableException | UnsupportedAudioFileException ex) {
                SwingUtilities.invokeLater(()
                        -> JOptionPane.showMessageDialog(LineOpener.this, "Error: " + ex.getMessage()));
            } finally {
                playing = false;
                currentSourceLine = null;
                SwingUtilities.invokeLater(() -> {
                    openButton.setText("Abrir Línea");
                    statusLabel.setText("Reproducción detenida");
                });
            }
        }).start();
    }

    private void stopPlayback() {
        playing = false;
        if (currentSourceLine != null) {
            currentSourceLine.stop();
            currentSourceLine.close();
        }
        if (currentTargetLine != null) {
            currentTargetLine.stop();
            currentTargetLine.close();
        }
        openButton.setText("Abrir Línea");
        statusLabel.setText("Operación detenida manualmente");
    }

    private AudioInputStream prepareAudioStream(File file, SourceDataLine line)
            throws UnsupportedAudioFileException, IOException {

        AudioInputStream originalStream = AudioSystem.getAudioInputStream(file);
        AudioFormat fileFormat = originalStream.getFormat();
        AudioFormat lineFormat = line.getFormat();

        if (!isCompatibleFormat(fileFormat, lineFormat)) {
            int response = JOptionPane.showConfirmDialog(this,
                    "El formato del archivo no es compatible. ¿Intentar convertir?",
                    "Formato incompatible", JOptionPane.YES_NO_OPTION);

            if (response == JOptionPane.YES_OPTION) {
                AudioFormat convertedFormat = new AudioFormat(
                        lineFormat.getEncoding(),
                        fileFormat.getSampleRate(),
                        lineFormat.getSampleSizeInBits(),
                        lineFormat.getChannels(),
                        lineFormat.getFrameSize(),
                        fileFormat.getFrameRate(),
                        lineFormat.isBigEndian());

                return AudioSystem.getAudioInputStream(convertedFormat, originalStream);
            } else {
                originalStream.close();
                return null;
            }
        }

        return originalStream;
    }

    private void handleTargetDataLine(TargetDataLine line) {
        // Crear un JFileChooser para seleccionar dónde guardar el archivo
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccionar ubicación para guardar la grabación");
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Audio Files", new String[]{"wav", "aiff", "au"}));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("WAV Audio", "wav"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("AIFF Files", "aiff"));
        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("AU Files", new String[]{"au"}));
        fileChooser.setSelectedFile(new File("grabacion.wav"));

        int result = fileChooser.showSaveDialog(this);

        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File saveFile = fileChooser.getSelectedFile();

        // Asegurar que el archivo tenga extensión .wav
        if (!saveFile.getName().toLowerCase().endsWith(".wav")) {
            saveFile = new File(saveFile.getAbsolutePath() + ".wav");
        }

        // Verificar si el archivo ya existe
        if (saveFile.exists()) {
            int response = JOptionPane.showConfirmDialog(this,
                    "El archivo ya existe. ¿Desea sobreescribirlo?",
                    "Archivo existente",
                    JOptionPane.YES_NO_OPTION);
            if (response != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // Crear variables finales para usar en el hilo
        final File finalSaveFile = saveFile;

        // Ejecutar la grabación en un hilo secundario
        new Thread(() -> {
            try {
                playing = true;
                currentTargetLine = line;
                updateUI("Iniciando grabación...");
                SwingUtilities.invokeLater(() -> openButton.setText("Detener Grabación"));

                // Abrir la línea de entrada
                line.open(line.getFormat());
                line.start();

                // Usar AudioSystem para escribir directamente a un archivo WAV
                AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;

                // Crear un AudioInputStream desde la TargetDataLine
                AudioInputStream audioInputStream = new AudioInputStream(line);

                // Escribir directamente al archivo usando AudioSystem en un hilo separado
                // para no bloquear el hilo de grabación
                Thread writeThread = new Thread(() -> {
                    try {
                        AudioSystem.write(audioInputStream, fileType, finalSaveFile);
                    } catch (IOException ex) {
                        SwingUtilities.invokeLater(()
                                -> JOptionPane.showMessageDialog(LineOpener.this, "Error al guardar archivo: " + ex.getMessage()));
                    }
                });
                writeThread.start();

                // El bucle de grabación continúa hasta que se detenga
                while (playing) {
                    Thread.sleep(100); // Pequeña pausa para no consumir mucho CPU
                }

                // Detener y cerrar la línea
                line.stop();
                line.close();
                audioInputStream.close();

                // Esperar a que termine el hilo de escritura
                try {
                    writeThread.join(1000); // Esperar máximo 1 segundo
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

                updateUI("Grabación completada: " + finalSaveFile.getName());

            } catch (LineUnavailableException | IOException | InterruptedException ex) {
                SwingUtilities.invokeLater(()
                        -> JOptionPane.showMessageDialog(LineOpener.this, "Error en grabación: " + ex.getMessage()));
            } finally {
                playing = false;
                currentTargetLine = null;
                SwingUtilities.invokeLater(() -> {
                    openButton.setText("Abrir Línea");
                    statusLabel.setText("Grabación detenida");
                });
            }
        }).start();
    }

    private void selectAudioFile() {
        LineInfoWrapper selectedLine = (LineInfoWrapper) lineComboBox.getSelectedItem();

        if (selectedLine != null && selectedLine.type == LineType.SOURCE) {
            JFileChooser fileChooser = new JFileChooser();

            // Get supported audio file types from AudioSystem
            AudioFileFormat.Type[] supportedTypes = AudioSystem.getAudioFileTypes();

            // Create file filter based on supported types
            if (supportedTypes.length > 0) {
                String[] extensions = Arrays.stream(supportedTypes)
                        .map(AudioFileFormat.Type::getExtension)
                        .toArray(String[]::new);
                FileNameExtensionFilter filter = new FileNameExtensionFilter(
                        "Audio Files (" + String.join(", ", extensions).toUpperCase() + ")",
                        extensions
                );
                fileChooser.setFileFilter(filter);
            } else {
                JOptionPane.showMessageDialog(this, "No audio file types are supported by the system.");
                return;
            }

            fileChooser.setDialogTitle("Seleccionar archivo de audio para reproducir");
            int result = fileChooser.showOpenDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                statusLabel.setText("Archivo seleccionado: " + selectedFile.getName());
            }
        } else if (selectedLine != null && selectedLine.type == LineType.TARGET) {
            statusLabel.setText("Seleccione 'Abrir Línea' para comenzar la grabación");
        }
    }

    private boolean isCompatibleFormat(AudioFormat format1, AudioFormat format2) {
        return format1.getEncoding().equals(format2.getEncoding())
                && format1.getSampleRate() == format2.getSampleRate()
                && format1.getSampleSizeInBits() == format2.getSampleSizeInBits()
                && format1.getChannels() == format2.getChannels()
                && format1.getFrameRate() == format2.getFrameRate()
                && format1.isBigEndian() == format2.isBigEndian();
    }

    private void updateUI(String message) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(message));
    }

    // Clases auxiliares
    private enum LineType {
        PORT, SOURCE, TARGET
    }

    private static class LineInfoWrapper {

        final Line.Info info;
        final LineType type;

        LineInfoWrapper(Line.Info info, LineType type) {
            this.info = info;
            this.type = type;
        }

        @Override
        public String toString() {
            return info.toString() + " (" + type.name() + ")";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LineOpener().setVisible(true);
        });
    }
}
