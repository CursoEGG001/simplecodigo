/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.CompoundControl;
import javax.sound.sampled.Control;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;

/**
 *
 * @author pc
 */
public class AudioMixerWithPortControls {

    public static void main(String[] args) {
        // Get the available mixers
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();

        // Convert the array to a stream using Spliterator
        Spliterator<Mixer.Info> spliterator = Spliterators.spliterator(mixerInfos, Spliterator.SIZED);
        Iterator<Mixer.Info> iterator = StreamSupport.stream(spliterator, false).iterator();

        // Iterate over the mixers to find those that support port controls
        while (iterator.hasNext()) {
            Mixer.Info mixerInfo = iterator.next();
            Mixer mixer = AudioSystem.getMixer(mixerInfo);

            // Check if the mixer supports port controls
            if (mixer.isLineSupported(Port.Info.SPEAKER)) {
                System.out.println("Mixer with port controls supported: " + mixerInfo.getName());

                // Optionally, you can open and use the mixer here
                // For example, open a port and perform some operations
                try {
                    try (Port port = (Port) mixer.getLine(Port.Info.SPEAKER)) {
                        port.open();
                        System.out.println("Port opened for mixer: " + mixerInfo.getName());
                        for (Control control : port.getControls()) {
                            if (control instanceof CompoundControl subControles) {
                                for (Control memberControl : subControles.getMemberControls()) {
                                    System.out.println("¬ Control: " + memberControl.getType() + "\n\t" + memberControl + "\n\n");
                                }
                            } else {
                                System.out.println(control.getType() + " :" + control);
                            }
                        }
                    }
                    System.out.println("Port closed for mixer: " + mixerInfo.getName());
                } catch (LineUnavailableException e) {
                    System.out.println("Port disabled");
                }
            }
        }
    }
}
