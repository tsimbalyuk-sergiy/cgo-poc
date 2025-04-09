#!/bin/bash

set -e

echo "--- Starting Build and Run ---"

export JAVA_HOME=${JAVA_HOME:-$(readlink -f /usr/bin/javac | sed "s:/bin/javac::")}
echo "JAVA_HOME to use: $JAVA_HOME"
if [ ! -d "$JAVA_HOME/include" ]; then
  echo "JAVA_HOME/include directory not found"
  exit 1
fi

CC=gcc
CFLAGS="-fPIC -O3 -march=native"
LDFLAGS="-v -shared"

OS_NAME=$(uname -s)
SHARED_LIB_EXT=".so"
JNI_PLATFORM_DIR="linux"

if [ "$OS_NAME" == "Darwin" ]; then
  echo "Detected macOS"
  SHARED_LIB_EXT=".dylib"
  JNI_PLATFORM_DIR="darwin"
else
  echo "Detected Linux"
fi

JNI_INCLUDE_PATHS="-I\"$JAVA_HOME/include\" -I\"$JAVA_HOME/include/$JNI_PLATFORM_DIR\""

LIB_FIB_CALC="libfibcalculator${SHARED_LIB_EXT}"
LIB_JNI_GLUE="libfibjni${SHARED_LIB_EXT}"
JAVA_PACKAGE_PATH="dev/tsvinc/fib"
JAVA_CLASS_NAME="dev.tsvinc.fib.FibCalculatorJNI"
JAVA_SOURCE_FILE="${JAVA_PACKAGE_PATH}/FibCalculatorJNI.java"
JNI_HEADER_FILE="dev_tsvinc_fib_FibCalculatorJNI.h"

$CC $CFLAGS $LDFLAGS fibcalculator.c -o ${LIB_FIB_CALC}
# shellcheck disable=SC2181
if [ $? -ne 0 ]; then echo "Error compiling fibcalculator.c"; exit 1; fi

if [ ! -f "${JAVA_SOURCE_FILE}" ]; then
    echo "Error: Java source file not found at ${JAVA_SOURCE_FILE}"
    exit 1
fi
javac -h . -d . ${JAVA_SOURCE_FILE}
# shellcheck disable=SC2181
if [ $? -ne 0 ]; then echo "Error generating JNI header"; exit 1; fi
if [ ! -f "${JNI_HEADER_FILE}" ]; then
    echo "Error: JNI header file ${JNI_HEADER_FILE} not generated."
    exit 1
fi

eval $CC $CFLAGS $LDFLAGS $JNI_INCLUDE_PATHS fibcalculator_jni.c -L. -lfibcalculator -o ${LIB_JNI_GLUE}
# shellcheck disable=SC2181
if [ $? -ne 0 ]; then echo "Error compiling JNI glue code"; exit 1; fi

if [ ! -f "${LIB_JNI_GLUE}" ]; then
    echo "Error: JNI glue library ${LIB_JNI_GLUE} not found"
    exit 1
fi
java -Djava.library.path=. ${JAVA_CLASS_NAME}