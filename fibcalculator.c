#include <stdio.h>

long long fib(int n) {
    if (n <= 0) return 0;
    if (n == 1) return 1;

    long long a = 0, b = 1;
    for (int i = 2; i <= n; i++) {
        long long temp = a + b;
        a = b;
        b = temp;
    }
    return b;
}

long long calculate_sum_of_fibs(int count, int n) {
    long long total_sum = 0;
    for (int i = 0; i < count; i++) {
        total_sum += fib(n);
    }
    return total_sum;
}