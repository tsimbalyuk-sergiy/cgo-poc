#include <jni.h>
#include <stdint.h>
#include "dev_tsvinc_fib_FibCalculatorJNI.h"

extern long long calculate_sum_of_fibs(int count, int n);

JNIEXPORT jlong JNICALL Java_dev_tsvinc_fib_FibCalculatorJNI_calculateSumOfFibsNative
  (JNIEnv *env, jclass cls, jint count, jint n)
{
    long long result = calculate_sum_of_fibs((int)count, (int)n);

    return (jlong)result;
}