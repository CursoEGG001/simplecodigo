/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package net.test.local.simple.codigo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author pc
 */
public class Simplecodigo {

    public static void main(String[] args) {

        Map<Integer, String> dataMap = new HashMap<>();

        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                Simplecodigo.class.getResourceAsStream(
                                        "/coding_qual_input.txt")
                        )
                )) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ", 2); // Split into number and string parts
                if (parts.length == 2) { // Ensure both parts are present
                    try {
                        int key = Integer.parseInt(parts[0]);
                        String value = parts[1];
                        dataMap.put(key, value);
                    } catch (NumberFormatException e) {
                        System.err.println("Formato de número en línea: " + line);
                    }
                } else {
                    System.err.println("Formato de línea inválido: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error leyendo archivo: " + e.getMessage());
        }

        TreeMap<Integer, String> ordenado = new TreeMap<>(dataMap);
        System.out.println(ordenado);

        System.out.println("HashMap resultado:");
        int k = 0, n = 0;

        for (Map.Entry<Integer, String> entry : ordenado.entrySet()) {
            System.out.print(entry.getValue() + "\t");
            if (k != n) {
                n++;
            } else {
                System.out.print("\n\"" + entry.getValue() + "\"\n");
                k++;
                n = 0;
            }

        }

        Map<Integer, String> componentes = new HashMap<>();
        componentes.put(3, "love");
        componentes.put(6, "computers");
        componentes.put(2, "dogs");
        componentes.put(4, "cats");
        componentes.put(1, "I");
        componentes.put(5, "you");
        componentes.put(7, "mean");
        componentes.put(8, "coral");
        componentes.put(9, "primal");

        k = 0;
        n = 0;
        for (Map.Entry<Integer, String> entry : componentes.entrySet()) {

            System.out.print(entry.getValue() + "[" + k + "](" + n + ")\t");
            if (k != n) {
                n++;
            } else {
                System.out.println("Encontrada: " + entry.getValue());
                k++;
                n = 0;
            }
        }
    }

}
