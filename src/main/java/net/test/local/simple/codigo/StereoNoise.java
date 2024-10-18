/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.io.ByteArrayInputStream;
import java.util.Random;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 *
 * @author pc
 */
public class StereoNoise {

    public static void main(String[] args) throws LineUnavailableException {
        int sampleRate = 44100;
        int sampleSizeInBits = 16;
        int channels = 2;
        boolean signed = true;
        boolean bigEndian = true;
        AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);

        int bufferSize = 4096;
        byte[] buffer = new byte[bufferSize * format.getFrameSize()];
        AudioInputStream audioInputStream = new AudioInputStream(new ByteArrayInputStream(buffer), format, bufferSize);

        SourceDataLine line = AudioSystem.getSourceDataLine(format);
        line.open(format);
        line.start();

        Random random = new Random();
        double angle = 0.0;
        double increment = 2 * Math.PI * 440.0 / sampleRate;

        while (true) {
            for (int i = 0; i < bufferSize; i++) {
                //Canal izquierdo
                short leftSample = (short) (Math.sin(angle) * Short.MAX_VALUE); //Onda senoidal
                //Canal derecho
                short rightSample = (short) (random.nextGaussian() * Short.MAX_VALUE); // Ruido blanco
                angle += increment;

                // Interleave samples into the buffer
                int offset = i * format.getFrameSize();
                if (format.isBigEndian()) {
                    buffer[offset] = (byte) (leftSample >>> 8);
                    buffer[offset + 1] = (byte) leftSample;
                    buffer[offset + 2] = (byte) (rightSample >>> 8);
                    buffer[offset + 3] = (byte) rightSample;
                } else {
                    buffer[offset + 1] = (byte) (leftSample >>> 8);
                    buffer[offset] = (byte) leftSample;
                    buffer[offset + 3] = (byte) (rightSample >>> 8);
                    buffer[offset + 2] = (byte) rightSample;
                }
            }

            line.write(buffer, 0, buffer.length);
        }
    }
}
