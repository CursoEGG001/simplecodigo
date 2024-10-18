/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

/**
 *
 * @author pc
 */
import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.util.Random;

public class WhiteNoise extends Thread {

    private static WhiteNoise generatorThread;

    final static public int SAMPLE_SIZE = 2;
    final static public int PACKET_SIZE = 5000;

    SourceDataLine line;
    public boolean exitExecution = false;

    public static void main(String[] args) {
        try {
            generatorThread = new WhiteNoise();
            generatorThread.start();
            Thread.sleep(3000);
            generatorThread.exit();
        } catch (InterruptedException e) {
            System.out.println("Interrumpido el hilo");
        }
    }

    public void run() {

        try {
            AudioFormat format = new AudioFormat(44100, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format, PACKET_SIZE * 2);

            if (!AudioSystem.isLineSupported(info)) {
                throw new LineUnavailableException();
            }

            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
        } catch (LineUnavailableException e) {
            System.exit(-1);
        }

        ByteBuffer buffer = ByteBuffer.allocate(PACKET_SIZE);

        Random random = new Random();
        while (exitExecution == false) {
            buffer.clear();
            for (int i = 0; i < PACKET_SIZE / SAMPLE_SIZE; i++) {
                buffer.putShort((short) (random.nextGaussian() * Short.MAX_VALUE));
            }
            line.write(buffer.array(), 0, buffer.position());
        }

        line.drain();
        line.close();
    }

    public void exit() {
        exitExecution = true;
    }

}
