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
/** 
 * The AudioControllerApp class serves as the main entry point for the audio control application. 
 * It initializes the GUI interface using SwingUtilities to ensure thread safety, sets up the 
 * communication component, and launches the audio processing thread to handle background operations. 
 * This class orchestrates the integration of the GUI, communication, and audio thread components 
 * to provide a functional audio control system. 
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

/** 
 * <p>GUI class for audio control application, extending JFrame to provide a graphical interface for audio playback, volume adjustment, and frequency setting.</p>
 * <p>Manages UI components including a play button, volume slider, and frequency input field, and communicates with a {@link Communicator} to send commands.</p>
 * <p>Provides methods to update the play button state and frequency display based on external input.</p>
 */
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

/**
 * AudioThread is a thread that manages audio playback, allowing dynamic control of volume and frequency. It processes
 * commands from a Communicator to toggle playback, adjust volume, and change frequency. The thread uses a SourceDataLine
 * for audio output and ensures thread-safe updates to volume and frequency using atomic references.
 */
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

    /** 
     * Generates an audio tone by filling a buffer with PCM samples based on the current frequency, volume, and audio
     * format settings. 
     * This method handles both signed and unsigned PCM encoding, supports 8-bit and 16-bit sample sizes, and accounts
     * for endianness. 
     * The generated buffer is written to the audio line for playback. 
     * <p>This method runs in a loop while the tone is playing ({@code isPlaying} is true) and updates the phase to generate
     * a continuous sine wave. 
     * It relies on internal state for audio format, line, and playback control. 
     */
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

    /** 
     * Processes incoming audio commands from the communicator, updating internal state 
     * and sending corresponding events. Handles volume, frequency, and playback toggle commands.
     * 
     * <p>Commands are parsed from the communicator's audio command queue. Valid commands:
     * <ul>
     *   <li>"VOLUME:XX" - Sets volume to XX% (0.0 to 1.0)</li>
     *   <li>"FREQUENCY:XX.XX" - Sets frequency to XX.XX Hz</li>
     *   <li>"TOGGLE_PLAYBACK" - Toggles playback state and sends "PLAYING"/"STOPPED" event</li>
     * </ul>
     * 
     * <p>Invalid frequency commands result in an error message being printed to stderr.
     * 
     * @param none 
     * @return void 
     * @throws none 
     */
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

    /** 
     * Stops the audio playback by setting the running and playing flags to false. 
     * This method is used to gracefully terminate the audio processing task. 
     * 
     * @return void 
     */
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

    /** 
     * Processes audio events asynchronously, updating the GUI state based on the event type. 
     * Handles "PLAYING" and "STOPPED" events by invoking Swing updates on the Event Dispatch Thread (EDT) 
     * to ensure thread safety. 
     * 
     * @param audioEvents A queue of audio events to process. 
     * @see SwingUtilities.invokeLater 
     */
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
