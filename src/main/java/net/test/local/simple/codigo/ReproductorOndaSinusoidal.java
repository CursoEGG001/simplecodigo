/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author pc
 */
public class ReproductorOndaSinusoidal extends JFrame {

    // Constantes para la generación de audio
    private static final float SAMPLE_RATE = 44100;
    private static final int SAMPLE_SIZE = 16;
    private static final int CHANNELS = 2;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = true;

    // Componentes de audio
    private SourceDataLine line;
    private Thread playThread;
    private volatile boolean isPlaying = false;
    private final AtomicInteger estadoActual = new AtomicInteger(0);

    // Componentes de la interfaz
    private JButton startButton;
    private JButton stopButton;
    private JLabel estadoLabel;
    private JLabel eventoLabel;

    public ReproductorOndaSinusoidal() {
        super("Reproductor de Onda Sinusoidal");

        // Configurar la interfaz gráfica
        configurarInterfaz();

        // Inicializar el sistema de audio
        inicializarAudio();

        // Mostrar la ventana
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void configurarInterfaz() {
        setLayout(new BorderLayout());

        // Panel principal
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Etiquetas de información
        estadoLabel = new JLabel("Estado: Detenido");
        eventoLabel = new JLabel("Evento: Ninguno");

        // Botones de control
        startButton = new JButton("Iniciar");
        stopButton = new JButton("Detener");
        stopButton.setEnabled(false);

        // Añadir acciones a los botones
        startButton.addActionListener((ActionEvent e) -> {
            iniciarReproduccion();
        });

        stopButton.addActionListener((ActionEvent e) -> {
            detenerReproduccion();
        });

        // Añadir componentes al panel
        panel.add(estadoLabel);
        panel.add(eventoLabel);
        panel.add(startButton);
        panel.add(stopButton);

        // Añadir panel al frame
        add(panel, BorderLayout.CENTER);

        // Panel de información
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new GridLayout(3, 1));
        infoPanel.setBorder(BorderFactory.createTitledBorder("Estados"));

        infoPanel.add(new JLabel("Estado 0: Sonido en ambos canales"));
        infoPanel.add(new JLabel("Estado 1: Sonido solo en canal izquierdo"));
        infoPanel.add(new JLabel("Estado 2: Sonido solo en canal derecho"));

        add(infoPanel, BorderLayout.SOUTH);
    }

    private void inicializarAudio() {
        try {
            AudioFormat format = new AudioFormat(
                    SAMPLE_RATE, // Sample rate
                    SAMPLE_SIZE, // Sample size in bits
                    CHANNELS, // Channels
                    SIGNED, // Signed
                    BIG_ENDIAN // Big-endian
            );

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                JOptionPane.showMessageDialog(this, "Formato de audio no soportado", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }

            line = (SourceDataLine) AudioSystem.getLine(info);

            // Agregar LineListener para manejar eventos
            line.addLineListener((LineEvent event) -> {
                if (event.getType() == LineEvent.Type.OPEN) {
                    SwingUtilities.invokeLater(() -> eventoLabel.setText("Evento: OPEN"));
                } else if (event.getType() == LineEvent.Type.START) {
                    SwingUtilities.invokeLater(() -> {
                        eventoLabel.setText("Evento: START");
                        estadoLabel.setText("Estado: " + obtenerNombreEstado(estadoActual.get()));
                        startButton.setEnabled(false);
                        stopButton.setEnabled(true);
                    });
                } else if (event.getType() == LineEvent.Type.STOP) {
                    SwingUtilities.invokeLater(() -> {
                        eventoLabel.setText("Evento: STOP");
                        // Cambiar al siguiente estado cuando se detiene
                        int nuevoEstado = (estadoActual.get() + 1) % 3;
                        estadoActual.set(nuevoEstado);
                        estadoLabel.setText("Estado: " + obtenerNombreEstado(nuevoEstado));
                        startButton.setEnabled(true);
                        stopButton.setEnabled(false);
                    });
                } else if (event.getType() == LineEvent.Type.CLOSE) {
                    SwingUtilities.invokeLater(() -> eventoLabel.setText("Evento: CLOSE"));
                }
            });

            line.open(format);
        } catch (LineUnavailableException e) {
            JOptionPane.showMessageDialog(this, "Error al inicializar el audio: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private String obtenerNombreEstado(int estado) {
        return switch (estado) {
            case 0 ->
                "Ambos canales";
            case 1 ->
                "Canal izquierdo";
            case 2 ->
                "Canal derecho";
            default ->
                "Desconocido";
        };
    }

    private void iniciarReproduccion() {
        if (isPlaying) {
            return;
        }

        isPlaying = true;

        playThread = new Thread(() -> {
            try {
                line.start();

                // Generar y reproducir onda sinusoidal
                generarOndaSinusoidal();

                line.drain();
                line.stop();
            } catch (Exception e) {
                System.err.println("Error durante la reproducción: " + e.getMessage());
            } finally {
                isPlaying = false;
            }
        });

        playThread.start();
    }

    private void detenerReproduccion() {
        isPlaying = false;
        if (line != null) {
            line.drain();
            line.stop();
        }
    }

    private void generarOndaSinusoidal() {
        // Parámetros de la onda
        final double frecuencia = 440.0; // Nota A (La)
        final double amplitud = 0.7;     // 70% de la amplitud máxima

        // Buffer para los datos de audio
        byte[] buffer = new byte[1024];

        // Estado actual
        final int estado = estadoActual.get();

        // Generar y reproducir la onda
        double angulo = 0;
        final double incrementoAngulo = 2.0 * Math.PI * frecuencia / SAMPLE_RATE;

        while (isPlaying) {
            // Llenar el buffer con datos de onda sinusoidal
            for (int i = 0; i < buffer.length; i += 4) { // 4 bytes por sample (16 bits * 2 canales)
                double sinValor = Math.sin(angulo) * amplitud;
                angulo += incrementoAngulo;

                // Convertir el valor a bytes (16 bits, big-endian)
                short valorAmplitud = (short) (sinValor * Short.MAX_VALUE);

                // Canal izquierdo
                if (estado == 0 || estado == 1) { // Ambos canales o solo izquierdo
                    buffer[i] = (byte) (valorAmplitud >> 8);
                    buffer[i + 1] = (byte) (valorAmplitud & 0xFF);
                } else { // Canal derecho solo
                    buffer[i] = 0;
                    buffer[i + 1] = 0;
                }

                // Canal derecho
                if (estado == 0 || estado == 2) { // Ambos canales o solo derecho
                    buffer[i + 2] = (byte) (valorAmplitud >> 8);
                    buffer[i + 3] = (byte) (valorAmplitud & 0xFF);
                } else { // Canal izquierdo solo
                    buffer[i + 2] = 0;
                    buffer[i + 3] = 0;
                }
            }

            // Escribir datos al line
            line.write(buffer, 0, buffer.length);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ReproductorOndaSinusoidal::new);
    }
}
