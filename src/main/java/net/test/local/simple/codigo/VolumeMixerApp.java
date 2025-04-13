/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Port;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

/**
 *
 * @author pc
 */
public class VolumeMixerApp {

    private Port speakerPort;
    private FloatControl volumeControl;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new VolumeMixerApp()::createAndShowGUI);
    }

    private void createAndShowGUI() {
        JFrame frame = new JFrame("Control de Volumen via Port");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // 1. Obtener y abrir el puerto de altavoz
        try {
            speakerPort = (Port) AudioSystem.getLine(Port.Info.SPEAKER);
            speakerPort.open();
        } catch (LineUnavailableException e) {
            showError(frame, "No se puede acceder al puerto de altavoces");
            return;
        }

        // 2. Buscar el control de volumen
        volumeControl = findVolumeControl(speakerPort, frame);
        if (volumeControl == null) {
            return;
        }

        // 3. Crear slider con mapeo correcto
        JSlider slider = createVolumeSlider(volumeControl);
        frame.add(slider, BorderLayout.CENTER);

        // 4. Configuración final
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // 5. Cerrar el puerto al salir
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (speakerPort != null && speakerPort.isOpen()) {
                    speakerPort.close();
                }
            }
        });
    }

    private FloatControl findVolumeControl(Port port, JFrame frame) {
        FloatControl control = null;
        if (port.isControlSupported(FloatControl.Type.VOLUME)) {
            control = (FloatControl) port.getControl(FloatControl.Type.VOLUME);
        } else if (port.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            control = (FloatControl) port.getControl(FloatControl.Type.MASTER_GAIN);
        }

        if (control == null) {
            showError(frame, "Este puerto no soporta control de volumen");
        }
        return control;
    }

    private JSlider createVolumeSlider(FloatControl control) {
        // Configuración de rangos
        float min = control.getMinimum();
        float max = control.getMaximum();
        float initialValue = control.getValue();

        // Mapeo lineal a escala 0-100
        JSlider slider = new JSlider(0, 100, convertToSliderValue(min, max, initialValue));
        slider.setMajorTickSpacing(20);
        slider.setMinorTickSpacing(5);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);

        // Actualización bidireccional
        slider.addChangeListener(e -> {
            float normalizedValue = slider.getValue() / 100f;
            float controlValue = min + (max - min) * normalizedValue;
            control.setValue(controlValue);
        });

        // Mostrar información del control
        String info = String.format("%s %s [%s]", speakerPort.getLineInfo().toString(),
                control.getType(),
                (control.getUnits().isEmpty() ? "unidades" : control.getUnits()));
        slider.setBorder(BorderFactory.createTitledBorder(info));

        return slider;
    }

    private void showError(JFrame frame, String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    private int convertToSliderValue(float min, float max, float value) {
        return Math.round((value - min) / (max - min) * 100);
    }
}
