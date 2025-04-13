/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

/**
 *
 * @author pc
 */
import java.lang.reflect.Method;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import static net.test.local.simple.codigo.CheckPortMethods.CheckPortMethods;

public class FiltrarPortInfo {

    public static void main(String[] args) {
        CheckPortMethods();
        Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
        for (Mixer.Info info : mixerInfo) {
            System.out.println("Mezclador: " + info);
            Mixer mixer = AudioSystem.getMixer(info);
            System.out.println("Usando Source");
            Line.Info[] linesInSource = mixer.getSourceLineInfo();

            for (Line.Info lineInfo : linesInSource) {
                if (lineInfo instanceof Port.Info) {
                    System.out.println("-- Port.Info encontrado: " + lineInfo);
                    // Aquí puedes realizar otras acciones con la instancia de Port.Info
                }
            }
            System.out.println("Usando Target");
            Line.Info[] linesInTarget = mixer.getTargetLineInfo();
            for (Line.Info lineInfo : linesInTarget) {

                if (lineInfo instanceof Port.Info) {
                    System.out.println("-- Port.Info encontrado: " + lineInfo);
                    // Aquí puedes realizar otras acciones con la instancia de Port.Info
                }
            }
        }
    }
}

class CheckPortMethods {

    public static void CheckPortMethods() {
        Class<Port> portClass = Port.class;
        Method[] methods = portClass.getMethods();

        System.out.println("Métodos de la clase Port:");
        for (Method method : methods) {
            System.out.println(method.getName());
        }

        boolean hasWriteMethod = false;
        for (Method method : methods) {
            if (method.getName().equals("write")) {
                hasWriteMethod = true;
                break;
            }
        }

        System.out.println("\n¿El método write existe en Port? " + hasWriteMethod);
    }
}
