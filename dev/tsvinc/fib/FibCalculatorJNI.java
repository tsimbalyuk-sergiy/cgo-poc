package dev.tsvinc.fib;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class FibCalculatorJNI {

    private static native long calculateSumOfFibsNative(int count, int n);

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

    public static long calculateSumOfFibsJava(int count, int n) {
        long totalSum = 0;
        for (int i = 0; i < count; i++) {
            totalSum += fibJava(n);
        }
        return totalSum;
    }

    @FunctionalInterface
    interface CalculationTask {
        long calculate(int count, int n);
    }

    public static List<Long> runWithThreads(int numWorkers, int count, int n, CalculationTask task) {
        ExecutorService executor = Executors.newFixedThreadPool(numWorkers);
        List<Future<Long>> futures = new ArrayList<>(numWorkers);
        List<Long> results = new ArrayList<>(numWorkers);

        for (int i = 0; i < numWorkers; i++) {
            Future<Long> future = executor.submit(() -> task.calculate(count, n));
            futures.add(future);
        }

        for (Future<Long> future : futures) {
            try {
                results.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Error executing task: " + e);
                results.add(-1L);
                Thread.currentThread().interrupt();
            }
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        return results;
    }

    public static List<Long> runWithVirtualThreads(int numWorkers, int count, int n, CalculationTask task) {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Long>> futures = new ArrayList<>(numWorkers);

            for (int i = 0; i < numWorkers; i++) {
                Future<Long> future = executor.submit(() -> task.calculate(count, n));
                futures.add(future);
            }

            List<Long> results = new ArrayList<>(numWorkers);
            for (Future<Long> future : futures) {
                try {
                    results.add(future.get());
                } catch (InterruptedException | ExecutionException e) {
                    System.err.println("Error executing task (Virtual Thread): " + e);
                    results.add(-1L);
                }
            }
            return results;
        }
    }

    public static List<Long> runWithManualVirtualThreads(int numWorkers, int count, int n, CalculationTask task) {
        List<FutureTask<Long>> futureTasks = new ArrayList<>(numWorkers);
        List<Thread> threads = new ArrayList<>(numWorkers);

        for (int i = 0; i < numWorkers; i++) {
            final int currentCount = count;
            final int currentN = n;
            Callable<Long> callable = () -> task.calculate(currentCount, currentN);

            FutureTask<Long> futureTask = new FutureTask<>(callable);
            futureTasks.add(futureTask);

            Thread virtualThread = Thread.startVirtualThread(futureTask);
            threads.add(virtualThread);
        }

        List<Long> results = new ArrayList<>(numWorkers);
        for (FutureTask<Long> ft : futureTasks) {
            try {
                results.add(ft.get());
            } catch (ExecutionException e) {
                results.add(-1L);
            } catch (InterruptedException e) {
                 results.add(-1L);
                 Thread.currentThread().interrupt();
            }
        }

        return results;
    }

    public static long sumResults(List<Long> results) {
        long total = 0;
        for (long r : results) {
            total += r;
        }
        return total;
    }


    public static void main(String[] args) {
        try {
             System.loadLibrary("fibjni");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Failed to load library. Google for -Djava.library.path=.");
            System.err.println(e);
            System.exit(1);
        }

        int numThreads = Runtime.getRuntime().availableProcessors();
        int iterationsPerThread = 10000000;
        int fibN = 92;

        System.out.printf("Number of threads: %d%n", numThreads);
        String delimiter = "----------------------------------------";

        long startTimeJNI = System.nanoTime();
        List<Long> resultsJNI = runWithThreads(numThreads, iterationsPerThread, fibN,
                                               FibCalculatorJNI::calculateSumOfFibsNative);
        long durationJNI = System.nanoTime() - startTimeJNI;
        System.out.printf("JNI time: \t\t%d ms%n", TimeUnit.NANOSECONDS.toMillis(durationJNI));
        System.out.printf("JNI total sum: \t\t%d%n", sumResults(resultsJNI));
        System.out.println(delimiter);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }


        long startTimeJava = System.nanoTime();
        List<Long> resultsJava = runWithVirtualThreads(numThreads, iterationsPerThread, fibN,
                                        FibCalculatorJNI::calculateSumOfFibsJava);
        long durationJava = System.nanoTime() - startTimeJava;
        System.out.printf("Java time: \t\t%d ms%n", TimeUnit.NANOSECONDS.toMillis(durationJava));
        System.out.printf("Java total sum: \t%d%n", sumResults(resultsJava));
        System.out.println(delimiter);

        System.out.printf("JNI time:\t\t%d ms%nJava time:\t\t%d ms%n",
                          TimeUnit.NANOSECONDS.toMillis(durationJNI),
                          TimeUnit.NANOSECONDS.toMillis(durationJava));
        System.out.println(delimiter);

        if (durationJNI < durationJava) {
            System.out.printf("JNI wins with diff: \t%d ms%n",
                              TimeUnit.NANOSECONDS.toMillis(durationJava - durationJNI));
        } else {
            System.out.printf("Java wins with diff: \t%d ms%n",
                              TimeUnit.NANOSECONDS.toMillis(durationJNI - durationJava));
        }
    }
}