/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import javax.sound.sampled.SourceDataLine;

/**
 *
 * @author pc
 */

public class DetectOutputLine {

    public static void main(String[] args) {
        // Obtener información de todos los mezcladores disponibles
        Mixer.Info[] mixersInfo = AudioSystem.getMixerInfo();

        for (Mixer.Info mixerInfo : mixersInfo) {
            System.out.println("Mezclador: " + mixerInfo.getName());

            try {
                // Obtener el mezclador
                Mixer mixer = AudioSystem.getMixer(mixerInfo);

                // Obtener las líneas de salida (target lines)
                Line.Info[] targetLines = mixer.getTargetLineInfo();

                System.out.println("  Líneas de salida:");
                for (Line.Info lineInfo : targetLines) {
                    System.out.println("    " + lineInfo);

                    // Verificar si es una línea de salida de tipo Port
                    if (lineInfo instanceof Port.Info) {
                        Port.Info portInfo = (Port.Info) lineInfo;
                        System.out.println("      Es un puerto de salida: " + portInfo.getName());
                    }

                    // Verificar si es una línea de salida de tipo SourceDataLine
                    if (SourceDataLine.class.isAssignableFrom(lineInfo.getLineClass())) {
                        System.out.println("      Es una línea de datos de salida");
                    }

                    // Verificar si es una línea de salida de tipo Clip
                    if (Clip.class.isAssignableFrom(lineInfo.getLineClass())) {
                        System.out.println("      Es una línea de clip de audio");
                    }
                }

            } catch (Exception e) {
                System.out.println("  Error al obtener información del mezclador: " + e.getMessage());
            }
        }
    }
}
