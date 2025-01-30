/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.io.IOException;
import java.util.Scanner;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

/**
 *
 * @author pc
 */
public class SineWavePlayerCLI {

    private static final float SAMPLE_RATE = 44100.0f;
    private static final int BUFFER_SIZE = 1024;

    public static void main(String[] args) throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);

        // 1: Lista dispositivos de audio disponibles
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        System.out.println("Available Playback Devices:");
        for (int i = 0; i < mixers.length; i++) {
            System.out.println((i + 1) + ". " + mixers[i].getName());
        }

        // 2: Pregunta por dispositivo a usar
        System.out.print("Select a playback device (1-" + mixers.length + "): ");
        int deviceIndex = scanner.nextInt() - 1;
        scanner.nextLine(); // Consume newline

        if (deviceIndex < 0 || deviceIndex >= mixers.length) {
            System.out.println("Invalid selection. Exiting...");
            return;
        }

        Mixer selectedMixer = AudioSystem.getMixer(mixers[deviceIndex]);
        System.out.println("\nSelected Device: " + mixers[deviceIndex].getName());

        // 3: Define formato de audio a usar
        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                SAMPLE_RATE, 16, 1, 2, SAMPLE_RATE, false);
        System.out.println("Playback Format: " + format);

        // 4: Abre un SourceDataLine en el dispositivo elegido
        DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, format);
        if (!selectedMixer.isLineSupported(lineInfo)) {
            System.out.println("Error: Selected device does not support the required format.");
            return;
        }

        try (SourceDataLine line = (SourceDataLine) selectedMixer.getLine(lineInfo)) {
            line.open(format);
            line.start();
            System.out.println("Playing sine wave (440Hz, amplitude 0.5)... Press Enter to stop.");

            // 5: Inicia reproducción en un hilo
            Thread playbackThread = new Thread(() -> generateSineWave(line, 440.0f, 0.5f));
            playbackThread.start();

            // Espera al usuario para detener la reproducción.
            do {
                if (0 != System.in.available()) {
                    playbackThread.interrupt();
                }
            } while (playbackThread.isAlive());

            line.drain();
        } catch (LineUnavailableException e) {
            System.err.println("Error: Unable to open line - " + e.getMessage());
        }

        System.out.println("Playback stopped.");
        scanner.close();
    }

    /**
     * Generates and plays a sine wave with the given frequency and amplitude.
     */
    private static void generateSineWave(SourceDataLine line, float frequency, float amplitude) {
        byte[] buffer = new byte[BUFFER_SIZE];
        double phase = 0.0;
        double phaseIncrement = 2.0 * Math.PI * frequency / SAMPLE_RATE;

        while (!Thread.currentThread().isInterrupted()) {
            for (int i = 0; i < BUFFER_SIZE / 2; i++) {
                short sample = (short) (amplitude * Math.sin(phase) * Short.MAX_VALUE);
                buffer[2 * i] = (byte) (sample & 0x00ff);
                buffer[2 * i + 1] = (byte) ((sample & 0xff00) >>> 8);
                phase += phaseIncrement;
                if (phase >= 2.0 * Math.PI) {
                    phase -= 2.0 * Math.PI;
                }
            }
            line.write(buffer, 0, BUFFER_SIZE);
        }
    }
}
