/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import static net.test.local.simple.codigo.AudioControllerApp.gui;

/**
 *
 * @author pc
 */
public class AudioControllerApp {

    static GUI gui;

    public static void main(String[] args) {
        Communicator communicator = new Communicator();

        SwingUtilities.invokeLater(() -> {
            gui = new GUI(communicator);
        });
        new AudioThread(communicator).start();
    }
}

class GUI extends JFrame {

    private final Communicator communicator;
    private JButton playButton;
    private JSlider volumeSlider;
    private JTextField frequencyField;

    public GUI(Communicator communicator) {
        this.communicator = communicator;
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Control de Audio");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel de control
        JPanel controlPanel = new JPanel(new GridLayout(3, 1));

        playButton = new JButton("Reproducir");
        playButton.addActionListener(e -> communicator.sendGuiCommand("TOGGLE_PLAYBACK"));

        volumeSlider = new JSlider(0, 100, 50);
        volumeSlider.setBorder(BorderFactory.createTitledBorder("Volume:"));
        volumeSlider.addChangeListener(e -> communicator.sendGuiCommand("VOLUME:" + volumeSlider.getValue()));

        frequencyField = new JTextField("440");
        frequencyField.setBorder(BorderFactory.createTitledBorder("Frequency:"));
        frequencyField.addActionListener(e -> communicator.sendGuiCommand("FREQUENCY:" + frequencyField.getText()));

        controlPanel.add(playButton);
        controlPanel.add(volumeSlider);
        controlPanel.add(frequencyField);

        add(controlPanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public void updatePlayButton(boolean playing) {
        SwingUtilities.invokeLater(()
                -> playButton.setText(playing ? "Detener" : "Reproducir"));
    }

    public void updateFrequency(double frequency) {
        SwingUtilities.invokeLater(() -> frequencyField.setText(String.valueOf(frequency)));
    }
}

class AudioThread extends Thread {

    private double phase = 0.0;
    private final Communicator communicator;
    private volatile boolean isRunning;
    private volatile boolean isPlaying;
    private final AtomicReference<Float> volume;
    private final AtomicReference<Double> frequency;
    private SourceDataLine line;

    public AudioThread(Communicator communicator) {
        this.frequency = new AtomicReference<>(440.0);
        this.volume = new AtomicReference<>(0.5f);
        this.isRunning = true;
        this.isPlaying = false;
        this.communicator = communicator;
    }

    @Override
    public void run() {
        try {
            AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            while (isRunning) {
                processCommands();
                if (isPlaying) {
                    generateTone();
                } else {
                    Thread.sleep(50);
                }
            }
        } catch (LineUnavailableException | InterruptedException e) {
        } finally {
            if (line != null) {
                line.close();
            }
        }
    }

    private void generateTone() {
        byte[] buffer = new byte[4096];
        AudioFormat format = line.getFormat();
        double sampleRate = format.getSampleRate();
        int sampleSizeInBits = format.getSampleSizeInBits();
        int channels = format.getChannels();
        boolean isSigned = format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED);
        boolean isBigEndian = format.isBigEndian();

        for (int i = 0; i < buffer.length && isPlaying;) {
            double angle = phase * 2 * Math.PI;
            double amplitude = volume.get();
            double sample = Math.sin(angle) * amplitude;

            if (isSigned) {
                sample *= 32767.0;
            } else {
                sample = sample * 32767.0 + 32767.0;
            }

            int intSample = (int) sample;

            for (int c = 0; c < channels; c++) {
                if (sampleSizeInBits == 16) {
                    if (isBigEndian) {
                        buffer[i++] = (byte) (intSample >>> 8);
                        buffer[i++] = (byte) intSample;
                    } else {
                        buffer[i++] = (byte) intSample;
                        buffer[i++] = (byte) (intSample >>> 8);
                    }
                } else if (sampleSizeInBits == 8) {
                    buffer[i++] = (byte) intSample;
                }
            }

            phase += frequency.get() / sampleRate;
            if (phase >= 1.0) {
                phase -= 1.0;
            }
        }

        line.write(buffer, 0, buffer.length);
    }

    private void processCommands() {
        String command = communicator.pollAudioCommand();
        if (command != null) {
            if (command.startsWith("VOLUME:")) {
                float vol = Integer.parseInt(command.substring(7)) / 100f;
                volume.set(vol);
            } else if (command.startsWith("FREQUENCY:")) {
                try {
                    double freq = Double.parseDouble(command.substring(10));
                    frequency.set(freq);
                    System.out.println("Frecuencia actualizada a: " + freq + " Hz");
                } catch (NumberFormatException e) {
                    System.err.println("Frecuencia inválida");
                }
            } else if (command.equals("TOGGLE_PLAYBACK")) {
                isPlaying = !isPlaying;
                communicator.sendAudioEvent(isPlaying ? "PLAYING" : "STOPPED");
            }
        }
    }

    public void stopAudio() {
        isRunning = false;
        isPlaying = false;
    }
}

class Communicator {

    private final BlockingQueue<String> guiCommands = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> audioEvents = new LinkedBlockingQueue<>();

    public void sendGuiCommand(String command) {
        guiCommands.offer(command);
    }

    public String pollAudioCommand() {
        return guiCommands.poll();
    }

    public void sendAudioEvent(String event) {
        audioEvents.offer(event);
        processEvents();
    }

    private void processEvents() {
        String event;
        while ((event = audioEvents.poll()) != null) {
            switch (event) {
                case "PLAYING" -> {
                    SwingUtilities.invokeLater(() -> {
                        gui.setTitle("Reproduciendo...");
                        gui.updatePlayButton(true);  // Actualizar botón
                    });
                }
                case "STOPPED" -> {
                    SwingUtilities.invokeLater(() -> {
                        gui.setTitle("Detenido");
                        gui.updatePlayButton(false);  // Actualizar botón
                    });
                }
            }
        }
    }

}
