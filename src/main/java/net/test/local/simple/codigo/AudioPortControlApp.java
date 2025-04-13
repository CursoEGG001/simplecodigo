/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class AudioPortControlApp extends JFrame {

    private SourceDataLine audioLine;
    private JSlider volumeSlider;
    private boolean isPlaying;
    private Thread playbackThread;
    private JComboBox<Port.Info> portComboBox;
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(44100, 16, 2, true, false);

    public AudioPortControlApp() {
        setTitle("Control de Puerto de Audio");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(640, 300));
        setMinimumSize(new Dimension(600, 240));
        setSize(720, 400);
        setLocationRelativeTo(null);

        initializeUI();
        populatePorts();
        pack();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Selecciona un puerto:"));

        portComboBox = new JComboBox<>();
        // Añadir listener para cambios en la selección del puerto
        portComboBox.addActionListener(e -> updateVolumeFromSelectedPort());
        topPanel.add(portComboBox);
        topPanel.add(portComboBox);

        JButton refreshButton = new JButton("Refrescar");
        refreshButton.addActionListener(e -> populatePorts());
        topPanel.add(refreshButton);

        JButton playButton = new JButton("Reproducir");
        playButton.addActionListener(e -> togglePlayback());
        topPanel.add(playButton);

        add(topPanel, BorderLayout.NORTH);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.TRAILING, 2, 2));

        volumeSlider = new JSlider(0, 100, 50);
        volumeSlider.setMajorTickSpacing(20);
        volumeSlider.setMinorTickSpacing(10);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setPaintLabels(true);
        volumeSlider.addChangeListener(e -> {
            if (audioLine != null && audioLine.isOpen() && audioLine.isControlSupported(FloatControl.Type.VOLUME)) {
                FloatControl volumeControl = (FloatControl) audioLine.getControl(FloatControl.Type.VOLUME);
                float value = volumeSlider.getValue() / 100f * (volumeControl.getMaximum() - volumeControl.getMinimum()) + volumeControl.getMinimum();
                volumeControl.setValue(value);
            }
        });

        controlPanel.add(new JLabel("Volumen:"));
        controlPanel.add(volumeSlider);
        add(controlPanel, BorderLayout.CENTER);

        JTextArea statusArea = new JTextArea(5, 30);
        statusArea.setEditable(false);
        add(new JScrollPane(statusArea), BorderLayout.SOUTH);
    }

    private void updateVolumeFromSelectedPort() {
        Port.Info portInfo = (Port.Info) portComboBox.getSelectedItem();
        if (portInfo == null) {
            return;
        }

        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixers) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            if (mixer.isLineSupported(portInfo)) {
                DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, AUDIO_FORMAT);
                if (mixer.isLineSupported(dataLineInfo)) {
                    try (SourceDataLine tempLine = (SourceDataLine) mixer.getLine(dataLineInfo)) {
                        tempLine.open(AUDIO_FORMAT);

                        if (tempLine.isControlSupported(FloatControl.Type.VOLUME)) {
                            FloatControl volumeControl = (FloatControl) tempLine.getControl(FloatControl.Type.VOLUME);
                            float currentValue = volumeControl.getValue();
                            // Convertir el valor del volumen a un porcentaje para el slider
                            float min = volumeControl.getMinimum();
                            float max = volumeControl.getMaximum();
                            int sliderValue = (int) ((currentValue - min) / (max - min) * 100);
                            SwingUtilities.invokeLater(() -> volumeSlider.setValue(sliderValue));
                        }
                    } catch (LineUnavailableException ex) {
                        Logger.getLogger(getClass().getName()).log(Level.WARNING, "No se pudo leer el volumen del puerto", ex);
                    }
                    break;
                }
            }
        }
    }

    private void populatePorts() {
        portComboBox.removeAllItems();
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixerInfos) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            Line.Info[] sourceLines = mixer.getSourceLineInfo();
            for (Line.Info lineInfo : sourceLines) {
                if (lineInfo instanceof Port.Info portInfo) {
                    portComboBox.addItem(portInfo);
                }
            }
            Line.Info[] targetLines = mixer.getTargetLineInfo();
            for (Line.Info lineInfo : targetLines) {
                if (lineInfo instanceof Port.Info portInfo) {
                    portComboBox.addItem(portInfo);
                }
            }
        }
    }

    private void togglePlayback() {
        if (isPlaying) {
            stopPlayback();
        } else {
            startPlayback();
        }
    }

    private void startPlayback() {
        isPlaying = true;
        playbackThread = new Thread(this::generateAndPlayTone);
        playbackThread.start();
    }

    private void stopPlayback() {
        isPlaying = false;
        if (audioLine != null) {
            audioLine.stop();
            audioLine.close();
        }
        if (playbackThread != null) {
            try {
                playbackThread.join();
            } catch (InterruptedException e) {
                Logger.getLogger(AudioPortControlApp.class.getName()).log(Level.SEVERE, null, e);
            }
        }
    }

    private void generateAndPlayTone() {
        try {
            AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
            Port.Info portInfo = (Port.Info) portComboBox.getSelectedItem();

            if (portInfo == null) {
                JOptionPane.showMessageDialog(this, "Selecciona un puerto válido.");
                return;
            }

            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            for (Mixer.Info mixerInfo : mixers) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(portInfo)) {
                    DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
                    if (mixer.isLineSupported(dataLineInfo)) {
                        try {
                            audioLine = (SourceDataLine) mixer.getLine(dataLineInfo);
                            audioLine.open(format);

                            if (audioLine.isControlSupported(FloatControl.Type.VOLUME)) {
                                FloatControl volumeControl = (FloatControl) audioLine.getControl(FloatControl.Type.VOLUME);
                                SwingUtilities.invokeLater(() -> {
                                    volumeSlider.setValue((int) ((volumeControl.getValue() - volumeControl.getMinimum()) / (volumeControl.getMaximum() - volumeControl.getMinimum()) * 100));
                                });
                            }

                            audioLine.start();

                            byte[] buffer = new byte[4096];
                            double angle = 0.0;
                            double frequency = 440.0;
                            int sampleRate = (int) format.getSampleRate();

                            while (isPlaying) {
                                for (int i = 0; i < buffer.length;) {
                                    double sine = Math.sin(angle);
                                    angle += 2 * Math.PI * frequency / sampleRate;
                                    int sample = (int) (sine * 32767);
                                    for (int c = 0; c < format.getChannels(); c++) {
                                        buffer[i++] = (byte) (sample);
                                        buffer[i++] = (byte) (sample >> 8);
                                    }
                                }
                                audioLine.write(buffer, 0, buffer.length);
                            }

                            audioLine.drain();
                            audioLine.close();
                            return;
                        } catch (LineUnavailableException e) {
                            Logger.getLogger(AudioPortControlApp.class.getName()).log(Level.SEVERE, "Formato no soportado por el puerto.", e);
                        }
                    }
                }
            }
            JOptionPane.showMessageDialog(this, "No se pudo reproducir en el puerto seleccionado.");
        } catch (Exception e) {
            Logger.getLogger(AudioPortControlApp.class.getName()).log(Level.SEVERE, "Error al reproducir.", e);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AudioPortControlApp().setVisible(true);
        });
    }
}
