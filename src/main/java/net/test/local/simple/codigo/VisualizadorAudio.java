/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author pc
 */
public class VisualizadorAudio extends JFrame {

    // Componente personalizado para visualización
    private final PanelVisualizacion panelVisualizacion;

    // Queue para buffers de audio entrantes
    private final Queue<AudioBuffer> colaAudio;

    // Deque para histórico de muestras (buffer circular)
    private final Deque<Double> historicoMuestras;
    private final int MAX_MUESTRAS = 400; // Muestras visibles

    // Imagen buffer para dibujo eficiente
    private BufferedImage bufferImagen;
    private Graphics2D bufferGraphics;

    // Componentes de captura de audio
    private TargetDataLine targetDataLine;
    private AudioFormat formatoAudio;
    private Thread hiloCaptura;
    private volatile boolean ejecutando = false;

    // Para control de volumen y detección
    private double volumenMaximo = 0.0;

    public VisualizadorAudio() {
        setTitle("Visualizador de Audio en Tiempo Real - Captura Real");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 300);
        setLocationRelativeTo(null);

        // Inicializar estructuras de datos
        this.colaAudio = new LinkedBlockingQueue<>();
        this.historicoMuestras = new ArrayDeque<>();

        // Crear panel de visualización
        panelVisualizacion = new PanelVisualizacion();
        add(panelVisualizacion);

        // Inicializar buffer de imagen
        inicializarBufferImagen();

        // Iniciar captura real de audio
        if (iniciarCaptura()) {
            // Iniciar hilo de visualización
            iniciarVisualizacion();
        } else {
            JOptionPane.showMessageDialog(null,
                    "No se encontró ningún dispositivo de entrada de audio disponible.",
                    "Error de Captura",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void inicializarBufferImagen() {
        bufferImagen = new BufferedImage(800, 300, BufferedImage.TYPE_INT_RGB);
        bufferGraphics = bufferImagen.createGraphics();
        bufferGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private boolean iniciarCaptura() {
        try {
            // Obtener todos los mixers disponibles
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            TargetDataLine lineaSeleccionada = null;

            System.out.println("Buscando dispositivos de entrada de audio...");

            // Buscar un mixer con línea de captura
            for (Mixer.Info mixerInfo : mixers) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                Line.Info[] lineInfos = mixer.getTargetLineInfo();

                if (lineInfos.length > 0) {
                    System.out.println("Encontrado: " + mixerInfo.getName()
                            + " - " + mixerInfo.getDescription());

                    try {
                        // Intentar obtener la línea de captura
                        lineaSeleccionada = (TargetDataLine) mixer.getLine(lineInfos[0]);
                        break; // Tomar el primero disponible
                    } catch (LineUnavailableException e) {
                        System.out.println("No se puede usar: " + mixerInfo.getName());
                    }
                }
            }

            if (lineaSeleccionada == null) {
                System.out.println("No se encontraron dispositivos de entrada de audio.");
                return false;
            }

            // Configurar formato de audio (16-bit, mono, 44.1kHz)
            formatoAudio = new AudioFormat(44100.0f, 16, 1, true, false);

            // Verificar si el formato es soportado
            DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, formatoAudio);
            if (!AudioSystem.isLineSupported(dataLineInfo)) {
                System.out.println("Formato no soportado, intentando con formato alternativo...");
                // Intentar con formato más básico
                formatoAudio = new AudioFormat(22050.0f, 16, 1, true, false);
                dataLineInfo = new DataLine.Info(TargetDataLine.class, formatoAudio);

                if (!AudioSystem.isLineSupported(dataLineInfo)) {
                    System.out.println("Formato alternativo tampoco soportado.");
                    return false;
                }
            }

            // Abrir la línea de captura
            targetDataLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
            targetDataLine.open(formatoAudio);

            System.out.println("Captura iniciada con éxito:");
            System.out.println("Formato: " + formatoAudio);
            System.out.println("Buffer size: " + targetDataLine.getBufferSize());

            // Iniciar hilo de captura
            ejecutando = true;
            hiloCaptura = new Thread(this::capturarAudio);
            hiloCaptura.setDaemon(true);
            hiloCaptura.start();

            return true;

        } catch (LineUnavailableException e) {
            System.err.println("Error al iniciar captura: " + e.getMessage());
            return false;
        }
    }

    private void capturarAudio() {
        try {
            // Iniciar captura
            targetDataLine.start();

            // Buffer para leer datos de audio
            int bufferSize = (int) (formatoAudio.getSampleRate() * formatoAudio.getFrameSize() * 0.1); // 100ms de audio
            byte[] buffer = new byte[bufferSize];

            System.out.println("Iniciando captura de audio...");

            while (ejecutando && !Thread.currentThread().isInterrupted()) {
                try {
                    // Leer datos de audio
                    int bytesRead = targetDataLine.read(buffer, 0, buffer.length);

                    if (bytesRead > 0) {
                        // Convertir bytes a muestras
                        double[] muestras = convertirBytesAMuestras(buffer, bytesRead);

                        // Crear buffer de audio y añadir a la cola
                        AudioBuffer audioBuffer = new AudioBuffer(muestras, 1);
                        colaAudio.offer(audioBuffer);
                    }

                } catch (Exception e) {
                    if (ejecutando) {
                        System.err.println("Error durante captura: " + e.getMessage());
                    }
                    break;
                }
            }

        } finally {
            // Limpiar recursos
            if (targetDataLine != null) {
                targetDataLine.stop();
                targetDataLine.close();
                System.out.println("Captura detenida.");
            }
        }
    }

    private double[] convertirBytesAMuestras(byte[] audioBytes, int length) {
        // Convertir bytes a muestras de 16-bit signed
        int bytesPerSample = formatoAudio.getSampleSizeInBits() / 8;
        int numSamples = length / bytesPerSample;
        double[] samples = new double[numSamples];

        for (int i = 0; i < numSamples; i++) {
            if (bytesPerSample == 2) {
                // 16-bit signed little-endian
                int sample = (audioBytes[i * 2 + 1] << 8) | (audioBytes[i * 2] & 0xFF);
                samples[i] = sample / 32768.0; // Normalizar a -1.0 a 1.0
            } else if (bytesPerSample == 1) {
                // 8-bit unsigned
                samples[i] = ((audioBytes[i] & 0xFF) - 128) / 128.0;
            }
        }

        return samples;
    }

    private void iniciarVisualizacion() {
        // Hilo para procesar y visualizar
        Thread hiloVisualizacion = new Thread(() -> {
            while (ejecutando) {
                try {
                    // Procesar buffers disponibles
                    procesarBuffersAudio();

                    // Actualizar visualización
                    actualizarVisualizacion();

                    Thread.sleep(30); // ~33 FPS
                } catch (InterruptedException e) {
                    if (ejecutando) {
                        System.out.println("Actualización en curso...");
                    }
                }
            }
        });

        hiloVisualizacion.setDaemon(true);
        hiloVisualizacion.start();
    }

    private void procesarBuffersAudio() {
        // Procesar todos los buffers disponibles en la cola (máximo 5 para no saturar)
        int buffersProcesados = 0;
        while (!colaAudio.isEmpty() && buffersProcesados < 5) {
            AudioBuffer buffer = colaAudio.poll();

            // Procesar todas las muestras del buffer
            double[] muestras = buffer.getMuestras();
            for (double muestra : muestras) {
                // Añadir al histórico usando Deque
                if (historicoMuestras.size() >= MAX_MUESTRAS) {
                    historicoMuestras.pollFirst(); // Eliminar la más antigua
                }
                historicoMuestras.offerLast(muestra); // Añadir la nueva

                // Actualizar volumen máximo para normalización
                double valorAbsoluto = Math.abs(muestra);
                if (valorAbsoluto > volumenMaximo) {
                    volumenMaximo = valorAbsoluto;
                }
            }

            buffersProcesados++;
        }

        // Decaer volumen máximo lentamente para mejor visualización
        volumenMaximo *= 0.995;
        if (volumenMaximo < 0.01) {
            volumenMaximo = 0.01;
        }
    }

    private void actualizarVisualizacion() {
        // Limpiar buffer
        bufferGraphics.setColor(Color.BLACK);
        bufferGraphics.fillRect(0, 0, getWidth(), getHeight());

        // Dibujar cuadrícula
        dibujarCuadricula();

        // Dibujar forma de onda usando el histórico
        dibujarFormaOnda();

        // Dibujar información
        dibujarInformacion();

        // Actualizar panel (solo se redibuja el buffer)
        panelVisualizacion.repaint();
    }

    private void dibujarCuadricula() {
        bufferGraphics.setColor(new Color(30, 30, 30));
        int ancho = getWidth();
        int altura = getHeight();

        // Líneas verticales
        for (int x = 0; x < ancho; x += 25) {
            bufferGraphics.drawLine(x, 0, x, altura);
        }

        // Líneas horizontales
        for (int y = 0; y < altura; y += 25) {
            bufferGraphics.drawLine(0, y, ancho, y);
        }

        // Líneas centrales más visibles
        bufferGraphics.setColor(new Color(60, 60, 60));
        bufferGraphics.drawLine(0, altura / 2, ancho, altura / 2);
        bufferGraphics.drawLine(ancho / 2, 0, ancho / 2, altura);
    }

    private void dibujarFormaOnda() {
        if (historicoMuestras.isEmpty()) {
            return;
        }

        int ancho = getWidth();
        int altura = getHeight();
        int centroY = altura / 2;

        // Convertir Deque a array para acceso eficiente
        Double[] muestras = historicoMuestras.toArray(Double[]::new);
        int numMuestras = muestras.length;

        // Dibujar línea de forma de onda
        bufferGraphics.setColor(Color.CYAN);
        bufferGraphics.setStroke(new BasicStroke(2));

        int ultimoX = 0;
        int ultimoY = centroY;

        for (int i = 0; i < numMuestras; i++) {
            // Calcular posición X (de izquierda a derecha)
            int x = (int) ((double) i / MAX_MUESTRAS * ancho);

            // Calcular posición Y basada en la muestra (normalizada)
            double muestraNormalizada = muestras[i] / volumenMaximo;
            int y = centroY - (int) (muestraNormalizada * (altura / 2 - 15));

            // Limitar Y dentro de los límites de la ventana
            y = Math.max(10, Math.min(altura - 10, y));

            // Dibujar línea desde el punto anterior
            if (i > 0) {
                bufferGraphics.drawLine(ultimoX, ultimoY, x, y);
            }

            ultimoX = x;
            ultimoY = y;
        }

        // Dibujar punto actual (última muestra) en rojo brillante
        if (numMuestras > 0) {
            bufferGraphics.setColor(Color.RED);
            bufferGraphics.fillOval(ultimoX - 4, ultimoY - 4, 8, 8);
            bufferGraphics.setColor(Color.WHITE);
            bufferGraphics.drawOval(ultimoX - 4, ultimoY - 4, 8, 8);
        }
    }

    private void dibujarInformacion() {
        bufferGraphics.setColor(Color.GREEN);
        bufferGraphics.setFont(new Font("Monospaced", Font.BOLD, 12));

        String info = String.format("Muestras: %d | Volumen Max: %.2f | Freq: %.0f Hz",
                historicoMuestras.size(),
                volumenMaximo,
                formatoAudio.getSampleRate());
        bufferGraphics.drawString(info, 10, 20);

        // Mostrar dispositivos disponibles
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        int dispositivos = 0;
        for (Mixer.Info infoMixer : mixers) {
            Mixer mixer = AudioSystem.getMixer(infoMixer);
            if (mixer.getTargetLineInfo().length > 0) {
                dispositivos++;
            }
        }
        bufferGraphics.drawString("Dispositivos de entrada: " + dispositivos, 10, 35);
    }

    // Panel personalizado que solo muestra el buffer
    private class PanelVisualizacion extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            // Solo dibujar el buffer - esto es muy eficiente
            if (bufferImagen != null) {
                g.drawImage(bufferImagen, 0, 0, null);
            }
        }
    }

    public void detener() {
        ejecutando = false;
        if (hiloCaptura != null) {
            hiloCaptura.interrupt();
        }
        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
                System.out.println("Fallo al inicializar interface: " + e.getLocalizedMessage());
            }

            VisualizadorAudio visualizador = new VisualizadorAudio();
            visualizador.setVisible(true);

            // Detener al cerrar
            visualizador.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    visualizador.detener();
                }
            });
        });
    }
}

// Clase auxiliar para buffers de audio
class AudioBuffer {

    private final double[] muestras;
    private final int numCanales;
    private final long timestamp;

    public AudioBuffer(double[] muestras, int numCanales) {
        this.muestras = muestras;
        this.numCanales = numCanales;
        this.timestamp = System.currentTimeMillis();
    }

    public double[] getMuestras() {
        return muestras;
    }

    public int getNumCanales() {
        return numCanales;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
