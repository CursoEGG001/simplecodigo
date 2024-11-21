/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Control;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;

/**
 *
 * @author pc
 */
public class SawtoothPlayer extends JFrame {

    private SourceDataLine line;
    private final int SAMPLE_RATE = 44100;
    private final int SAMPLE_SIZE_IN_BITS = 16;
    private final int CHANNELS = 1;
    private final boolean SIGNED = true;
    private final boolean BIG_ENDIAN = false;
    private byte[] buffer;
    private float volume = 1.0f;
    private FloatControl volumeControl;

    public SawtoothPlayer() {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            // Check for and store volume control
            for (Control control : line.getControls()) {
                if (control instanceof FloatControl
                        && line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    volumeControl = (FloatControl) control;
                    break;
                }
            }

            // Create a slider for volume control
            JSlider volumeSlider = new JSlider(0, 100, 100);
            volumeSlider.setBorder(BorderFactory.createTitledBorder("Amplitud"));
            volumeSlider.addChangeListener((ChangeEvent e) -> {
                volume = (float) volumeSlider.getValue() / 100f;
                if (volumeControl != null) {
                    volumeControl.setValue(
                            (volumeControl.getMaximum() - volumeControl.getMinimum()) * volume + volumeControl.getMinimum()
                    );
                }
            });

            add(volumeSlider);
            setSize(300, 100);
            setVisible(true);

            SwingWorker audioTask = new SwingWorker<Object, Object>() {
                @Override
                protected Object doInBackground() throws Exception {
                    buffer = new byte[line.getBufferSize()];
                    generateSawtooth(buffer);
                    playLoop();
                    return null;
                }
            };
            audioTask.execute();

        } catch (LineUnavailableException e) {
        }
    }

    private void generateSawtooth(byte[] buffer) {
        int sampleSizeInBytes = SAMPLE_SIZE_IN_BITS / 8;
        int samplesPerFrame = CHANNELS * sampleSizeInBytes;
        int numSamples = buffer.length / samplesPerFrame;

        double phase = 0.0;
        double increment = 2.0 * Math.PI * 440.0 / SAMPLE_RATE;

        for (int i = 0; i < numSamples; i++) {
            short sample = (short) (Short.MAX_VALUE * (phase / Math.PI - 1));
            sample *= line.isControlSupported(volumeControl.getType()) ? 1 : volume;

            byte lowByte = (byte) (sample & 0xFF);
            byte highByte = (byte) ((sample >> 8) & 0xFF);

            buffer[i * samplesPerFrame] = lowByte;
            buffer[i * samplesPerFrame + 1] = highByte;

            phase += increment;
            if (phase >= 2 * Math.PI) {
                phase -= 2 * Math.PI;
            }
        }
    }

    private void playLoop() {
        while (true) {
            int count = line.write(buffer, 0, buffer.length);
            if (count < buffer.length) {
                generateSawtooth(buffer);
            }
        }
    }

    public static void main(String[] args) {
        SawtoothPlayer frameVolume = new SawtoothPlayer();
        frameVolume.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frameVolume.setTitle("Sawtooth Volume Control");
        frameVolume.setVisible(true);
    }
}
