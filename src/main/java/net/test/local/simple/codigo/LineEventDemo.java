/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 *
 * @author pc
 */
public class LineEventDemo extends JFrame {

    private final JComboBox<Mixer.Info> mixerComboBox;
    private final JComboBox<AudioFormat> formatComboBox;
    private final JTextArea logArea = new JTextArea(10, 40);
    private final JButton playButton = new JButton("Play");
    private final JButton stopButton = new JButton("Stop");

    private Mixer selectedMixer;
    private AudioFormat selectedFormat;
    private SourceDataLine audioLine;
    private volatile boolean playing = false;

    public LineEventDemo() {
        this.mixerComboBox = new JComboBox<>();
        this.formatComboBox = new JComboBox<>();
        configureUI();
        loadAudioDevices();
        setupListeners();
    }

    private void configureUI() {
        setLayout(new BorderLayout(5, 5));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Panel de configuración
        JPanel configPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        configPanel.add(new JLabel("Dispositivo de audio:"));
        configPanel.add(mixerComboBox);
        configPanel.add(new JLabel("Formato de audio:"));
        configPanel.add(formatComboBox);

        // Panel de controles
        JPanel controlPanel = new JPanel();
        controlPanel.add(playButton);
        controlPanel.add(stopButton);
        stopButton.setEnabled(false);

        add(configPanel, BorderLayout.NORTH);
        add(new JScrollPane(logArea), BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private void loadAudioDevices() {
        mixerComboBox.removeAllItems();
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            mixerComboBox.addItem(info);
        }
    }

    private void setupListeners() {
        mixerComboBox.addActionListener(e -> updateFormatList());
        formatComboBox.addActionListener(e -> updateSelectedFormat());

        playButton.addActionListener(e -> startPlayback());
        stopButton.addActionListener(e -> stopPlayback());
    }

    private void updateFormatList() {
        formatComboBox.removeAllItems();
        Mixer.Info info = (Mixer.Info) mixerComboBox.getSelectedItem();
        selectedMixer = AudioSystem.getMixer(info);

        // Obtener formatos soportados para líneas de salida
        Line.Info lineInfo = new DataLine.Info(SourceDataLine.class, null);
        try {
            for (Line.Info supportedLine : selectedMixer.getSourceLineInfo(lineInfo)) {
                DataLine.Info dataLineInfo = (DataLine.Info) supportedLine;
                for (AudioFormat format : dataLineInfo.getFormats()) {
                    formatComboBox.addItem(format);
                }
            }
        } catch (Exception ex) {
            log("Error al cargar formatos: " + ex.getMessage());
        }
    }

    private void updateSelectedFormat() {
        selectedFormat = (AudioFormat) formatComboBox.getSelectedItem();
    }

    private void startPlayback() {
        if (selectedMixer == null || selectedFormat == null) {
            JOptionPane.showMessageDialog(this, "Seleccione dispositivo fuente y formato", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, selectedFormat);
            if (!selectedMixer.isLineSupported(info)) {
                JOptionPane.showMessageDialog(this, "La línea no es compatible con el formato de audio", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            audioLine = (SourceDataLine) selectedMixer.getLine(info);

            audioLine.addLineListener(event
                    -> SwingUtilities.invokeLater(()
                            -> log("Evento de línea: " + event.getType())
                    )
            );

            audioLine.open(
                    new AudioFormat(
                            (selectedFormat.getSampleRate() > 0)
                            ? selectedFormat.getSampleRate()
                            : 44000.0f,
                            selectedFormat.getSampleSizeInBits(),
                            selectedFormat.getChannels(),
                            selectedFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED),
                            selectedFormat.isBigEndian()
                    )
            );
            audioLine.start();

            playing = true;
            new Thread(this::generateTone).start();

            playButton.setEnabled(false);
            stopButton.setEnabled(true);
            log("Reproducción iniciada");
        } catch (LineUnavailableException ex) {
            log("Error: Línea no disponible - " + ex.getMessage());
        }
    }

    private void generateTone() {
        int bufferSize = 4096; // Tamaño del buffer en muestras por canal
        int sampleRate = (selectedFormat.getSampleRate() > 0)
                ? (int) selectedFormat.getSampleRate()
                : 44000;
        int sampleSizeInBits = selectedFormat.getSampleSizeInBits();
        int channels = selectedFormat.getChannels();
        boolean signed = selectedFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED);
        boolean bigEndian = selectedFormat.isBigEndian();

        // Tamaño del buffer en bytes (considerando canales)
        int bufferSizeInBytes = bufferSize * channels * (sampleSizeInBits / 8);
        byte[] buffer = new byte[bufferSizeInBytes];

        double freq = 440.0; // Frecuencia en Hz
        double amp = 0.5;     // Amplitud

        double maxAmplitude = amp * (signed ? (Math.pow(2, sampleSizeInBits - 1) - 1) : (Math.pow(2, sampleSizeInBits) - 1));

        while (playing) {
            for (int i = 0; i < bufferSize; i++) {
                double angle = i / (sampleRate / freq) * 2.0 * Math.PI;
                double sample = maxAmplitude * Math.sin(angle);

                // Generar muestras para cada canal
                for (int channel = 0; channel < channels; channel++) {
                    int index = i * channels * (sampleSizeInBits / 8) + channel * (sampleSizeInBits / 8);

                    // Almacenar la muestra según el formato
                    if (sampleSizeInBits == 8) {
                        buffer[index] = (byte) sample;
                    } else if (sampleSizeInBits == 16) {
                        short s = (short) sample;
                        if (bigEndian) {
                            buffer[index] = (byte) (s >> 8);
                            buffer[index + 1] = (byte) s;
                        } else {
                            buffer[index] = (byte) s;
                            buffer[index + 1] = (byte) (s >> 8);
                        }
                    }
                }
            }

            audioLine.write(buffer, 0, bufferSizeInBytes);
        }
    }

    private void stopPlayback() {
        playing = false;
        if (audioLine != null) {
            audioLine.stop();
            audioLine.close();
        }
        playButton.setEnabled(true);
        stopButton.setEnabled(false);
        log("Reproducción detenida");
    }

    private void log(String message) {
        logArea.append(message + "\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(()
                -> new LineEventDemo().setVisible(true)
        );
    }

}
