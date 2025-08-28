/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.test.local.simple.codigo;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author pc
 */
class BrainSynapse {

    // Configuración de la sinapsis
    private static final int MAX_QUEUE_SIZE = 1023;
    private static final int NEURONS = 17;
    private static final Random RANDOM = new Random();

    // Estado compartido
    private final Queue<Double>[] neurotransmitters;
    private final Lock lock = new ReentrantLock(true); // Lock justo
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    // Contadores atómicos para detección de bloqueos
    private final AtomicInteger deadlockCounter = new AtomicInteger(0);
    private final AtomicInteger livelockCounter = new AtomicInteger(0);

    // Perceptrón simple para ajustar colas
    private final Perceptron perceptron;

    public BrainSynapse() {
        neurotransmitters = new Queue[NEURONS];
        for (int i = 0; i < NEURONS; i++) {
            neurotransmitters[i] = new LinkedList<>();
        }
        perceptron = new Perceptron(NEURONS);
    }

    // Perceptrón para ajustar prioridades
    static class Perceptron {

        private final double[] weights;

        public Perceptron(int size) {
            weights = new double[size];
            for (int i = 0; i < size; i++) {
                weights[i] = 1.0; // Pesos iniciales
            }
        }

        public void adjustWeights(int neuronId, double error) {
            weights[neuronId] += 0.1 * error; // Ajuste simple
        }

        public double getWeight(int neuronId) {
            return weights[neuronId];
        }
    }

    // Neurona productora
    public void produce(int neuronId, double signal) throws InterruptedException {
        lock.lock();
        try {
            // Verificar deadlock/livelock
            if (deadlockCounter.get() > 10 || livelockCounter.get() > 20) {
                System.out.println("⬅ NEURONA " + neuronId + " CAMBIA DE HILO POR BLOQUEO");
                Thread.yield(); // Cambio de contexto
                deadlockCounter.set(0);
                livelockCounter.set(0);
            }

            // Esperar si la cola está llena (con timeout para evitar livelock)
            while (neurotransmitters[neuronId].size() >= MAX_QUEUE_SIZE) {
                livelockCounter.incrementAndGet();
                if (!notFull.await(100 + RANDOM.nextInt(100), TimeUnit.MILLISECONDS)) {
                    perceptron.adjustWeights(neuronId, -0.1); // Reducir prioridad
                    return;
                }
            }

            // Añadir señal usando el perceptrón para priorizar
            double priority = perceptron.getWeight(neuronId);
            neurotransmitters[neuronId].offer(signal * priority);
            System.out.println("NEURONA ⚡ " + neuronId + " produce: " + signal + " (Pri: " + priority + ")");

            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    // Neurona consumidora
    public double consume(int neuronId) throws InterruptedException {
        lock.lock();
        try {
            // Detectar posibles deadlocks
            if (deadlockCounter.incrementAndGet() > 1000) {
                System.out.println("🚨 DETECTADO DEADLOCK! Liberando recursos...");
                neurotransmitters[neuronId].clear();
                deadlockCounter.set(0);
                return 0;
            }

            // Esperar con timeout para evitar bloqueos
            while (neurotransmitters[neuronId].isEmpty()) {
                if (!notEmpty.await(200, TimeUnit.MILLISECONDS)) {
                    livelockCounter.incrementAndGet();
                    perceptron.adjustWeights(neuronId, 0.2); // Aumentar prioridad
                    return 0;
                }
            }

            // Procesar señal
            double signal = neurotransmitters[neuronId].poll();
            System.out.println("🔄 NEURONA " + neuronId + " consume: " + signal);

            notFull.signalAll();
            deadlockCounter.decrementAndGet();
            return signal;
        } finally {
            lock.unlock();
        }
    }
}

public class NeuralConcurrency {

    private static final Random RANDOM = new Random();

    public static void main(String[] args) throws InterruptedException {
        final BrainSynapse synapse = new BrainSynapse();
        final ExecutorService executor = Executors.newFixedThreadPool(18);

        // Crear tareas para cada neurona
        for (int i = 0; i < 18; i++) {
            final int neuronId = i;
            executor.submit(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        // Patrón variable para simular bloqueos
                        if (Math.random() > 0.22) {
                            synapse.produce(neuronId, RANDOM.nextDouble());
                        } else {
                            synapse.consume(neuronId);
                        }
                        TimeUnit.MILLISECONDS.sleep(89 + RANDOM.nextInt(89));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Apagar después de 50 segundos
        executor.awaitTermination(50, TimeUnit.SECONDS);
        executor.shutdownNow();
        System.out.println("Simulación finalizada");

    }
}
