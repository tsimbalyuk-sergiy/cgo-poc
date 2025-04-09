package dev.tsvinc.fib;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class FibCalculatorJNA {

    public static long fibJava(int n) {
        if (n <= 0) {
            return 0;
        }
        if (n == 1) {
            return 1;
        }
        long a = 0;
        long b = 1;
        for (int i = 2; i <= n; i++) {
            long temp = a + b;
            a = b;
            b = temp;
        }
        return b;
    }

    @FunctionalInterface
    interface CalculationTask {
        long calculate(int count, int n);
    }

    public static long calculateSumOfFibsJava(int count, int n) {
        long totalSum = 0;
        for (int i = 0; i < count; i++) {
            totalSum += fibJava(n);
        }
        return totalSum;
    }

    public static List<Long> runWithPlatformThreads(int numWorkers, int count, int n, CalculationTask task) {
        ExecutorService executor = Executors.newFixedThreadPool(numWorkers);
        List<Future<Long>> futures = new ArrayList<>(numWorkers);
        try {
            for (int i = 0; i < numWorkers; i++) {
                futures.add(executor.submit(() -> task.calculate(count, n)));
            }
            List<Long> results = new ArrayList<>(numWorkers);
            for (Future<Long> future : futures) {
                try {
                    results.add(future.get());
                } catch (ExecutionException e) {
                    System.err.println(e);
                    results.add(-1L);
                } catch (InterruptedException e) {
                    System.err.println(e);
                    results.add(-1L);
                    Thread.currentThread().interrupt();
                }
            }
            return results;
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException ie) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public static long sumResults(List<Long> results) {
        long total = 0;
        for (long r : results) {
            if (r != -1L) {
                 total += r;
            }
        }
        return total;
    }

    public static void main(String[] args) {
        int numThreads = Runtime.getRuntime().availableProcessors();
        int iterationsPerThread = 10000000;
        int fibN = 92;
        String delimiter = "----------------------------------------";

        System.out.println("Starting JNA vs Java Benchmark...");
        System.out.printf("Number of workers (threads): %d%n", numThreads);
        System.out.printf("Iterations per worker: %d%n", iterationsPerThread);
        System.out.printf("Fibonacci number N: %d%n", fibN);
        System.out.println(delimiter);

        long startTimeJNA = System.nanoTime();
        List<Long> resultsJNA = runWithPlatformThreads(numThreads, iterationsPerThread, fibN,
                (count, n) -> FibCalculatorLibrary.INSTANCE.calculate_sum_of_fibs(count, n)
        );
        long durationJNA = System.nanoTime() - startTimeJNA;
        System.out.printf("JNA time: \t\t%d ms%n", TimeUnit.NANOSECONDS.toMillis(durationJNA));
        System.out.printf("JNA total sum: \t\t%d%n", sumResults(resultsJNA));
        System.out.println(delimiter);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long startTimeJavaPlatform = System.nanoTime();
        List<Long> resultsJavaPlatform = runWithPlatformThreads(numThreads, iterationsPerThread, fibN,
                                            FibCalculatorJNA::calculateSumOfFibsJava); // Non-optimized Java
        long durationJavaPlatform = System.nanoTime() - startTimeJavaPlatform;
        System.out.printf("Java time: \t%d ms%n", TimeUnit.NANOSECONDS.toMillis(durationJavaPlatform));
        System.out.printf("Java total sum: \t%d%n", sumResults(resultsJavaPlatform));
        System.out.println(delimiter);

        System.out.printf("JNA time:\t\t%d ms%nJava time:\t%d ms%n",
                          TimeUnit.NANOSECONDS.toMillis(durationJNA),
                          TimeUnit.NANOSECONDS.toMillis(durationJavaPlatform));
        System.out.println(delimiter);

        if (durationJNA < durationJavaPlatform) {
            System.out.printf("JNA wins with diff: \t%d ms%n",
                              TimeUnit.NANOSECONDS.toMillis(durationJavaPlatform - durationJNA));
        } else {
            System.out.printf("Java wins with diff: \t%d ms%n",
                              TimeUnit.NANOSECONDS.toMillis(durationJNA - durationJavaPlatform));
        }
         System.out.println("Benchmark finished.");
    }
}