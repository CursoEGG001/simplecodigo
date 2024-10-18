/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

/**
 *
 * @author pc
 */
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class TemporalExample {

    public static void main(String[] args) {
        // Get the start of the current year
        LocalDate startOfYear = LocalDate.now().withMonth(1).withDayOfMonth(1);
        System.out.println("This day: " + LocalDate.now() + " and origin day: " + startOfYear);

        // Calculate the number of days from the start of the year
        long daysSinceStartOfYear = ChronoUnit.DAYS.between(startOfYear, LocalDate.now());

        System.out.println("Days since the start of the year: " + daysSinceStartOfYear);
    }
}
