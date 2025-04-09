package dev.tsvinc.fib;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;

public interface FibCalculatorLibrary extends Library {
    FibCalculatorLibrary INSTANCE = Native.load("fibcalculator", FibCalculatorLibrary.class);
    long calculate_sum_of_fibs(int count, int n);
}