#!/bin/bash

set -e

echo "--- Starting Build and Run ---"

export JAVA_HOME=${JAVA_HOME:-$(readlink -f /usr/bin/javac | sed "s:/bin/javac::")}
echo "JAVA_HOME to use: $JAVA_HOME"
if [ ! -d "$JAVA_HOME/include" ]; then
  echo "JAVA_HOME/include directory not found"
  exit 1
fi

CC="gcc"
CFLAGS="-fPIC -O3 -march=native"
LDFLAGS="-shared"
JAVAC="$JAVA_HOME/bin/javac"
JAVA="$JAVA_HOME/bin/java"

JNA_VERSION="5.14.0"
JNA_JAR_NAME="jna-${JNA_VERSION}.jar"
JNA_DOWNLOAD_URL="https://repo1.maven.org/maven2/net/java/dev/jna/jna/${JNA_VERSION}/${JNA_JAR_NAME}"
JNA_TARGET_JAR_PATH="./${JNA_JAR_NAME}"
JAVA_PACKAGE_PATH="dev/tsvinc/fib"
JAVA_MAIN_CLASS="dev.tsvinc.fib.FibCalculatorJNA"
JAVA_SOURCE_FILES=("${JAVA_PACKAGE_PATH}/FibCalculatorJNA.java" "${JAVA_PACKAGE_PATH}/FibCalculatorLibrary.java")
NATIVE_LIB_C_SOURCE="fibcalculator.c"

OS_NAME=$(uname -s)
SHARED_LIB_EXT=".so"
if [ "$OS_NAME" == "Darwin" ]; then
  echo "Detected macOS"
  SHARED_LIB_EXT=".dylib"
else
  echo "Detected Linux"
fi

NATIVE_LIB_NAME="libfibcalculator${SHARED_LIB_EXT}"
NATIVE_LIB_TARGET_PATH="./${NATIVE_LIB_NAME}"

rm -f "${NATIVE_LIB_NAME}" "${JNA_JAR_NAME}"
find dev -type f -name '*.class' -delete 2>/dev/null || true
if [ -f "${JNA_TARGET_JAR_PATH}" ]; then
    echo "[DEBUG] ${JNA_JAR_NAME} already exists"
else
    DOWNLOAD_CMD=""
    if command -v curl &> /dev/null; then
        DOWNLOAD_CMD="curl -LfsS -o \"${JNA_TARGET_JAR_PATH}\" \"${JNA_DOWNLOAD_URL}\""
    elif command -v wget &> /dev/null; then
        DOWNLOAD_CMD="wget -q -O \"${JNA_TARGET_JAR_PATH}\" \"${JNA_DOWNLOAD_URL}\""
    else
        echo "curl or wget required"
        exit 1
    fi
    eval ${DOWNLOAD_CMD}
    if [ ! -f "${JNA_TARGET_JAR_PATH}" ] || [ ! -s "${JNA_TARGET_JAR_PATH}" ]; then
         echo "[ERROR] Failed to download ${JNA_JAR_NAME}." >&2
         rm -f "${JNA_TARGET_JAR_PATH}"
         exit 1
    fi
fi


if [ ! -f "${NATIVE_LIB_C_SOURCE}" ]; then
    echo "[ERROR] Native C source file not found: ${NATIVE_LIB_C_SOURCE}" >&2
    exit 1
fi
${CC} ${CFLAGS} ${LDFLAGS} "${NATIVE_LIB_C_SOURCE}" -o "${NATIVE_LIB_TARGET_PATH}"

if [ $? -ne 0 ]; then echo "Error compiling native library" >&2; exit 1; fi


for file in "${JAVA_SOURCE_FILES[@]}"; do
    echo "[DEBUG] Checking file existence for: '$file'"
    if [ ! -f "$file" ]; then
        echo "[ERROR] Java source file not found: $file" >&2
        exit 1
    fi
done

mkdir -p "${JAVA_PACKAGE_PATH}"

CLASSPATH_SEP=":"
COMPILE_CLASSPATH=".${CLASSPATH_SEP}${JNA_TARGET_JAR_PATH}"

echo "[DEBUG] Classpath: ${COMPILE_CLASSPATH}"

${JAVAC} -cp "${COMPILE_CLASSPATH}" -d . "${JAVA_SOURCE_FILES[@]}"

if [ $? -ne 0 ]; then echo "Error compiling Java" >&2; exit 1; fi

RUNTIME_CLASSPATH=".${CLASSPATH_SEP}${JNA_TARGET_JAR_PATH}"
JAVA_OPTS="-Djava.library.path=."
echo "[DEBUG] Java options: ${JAVA_OPTS}"
echo "[DEBUG] cmd: ${JAVA} ${JAVA_OPTS} -cp \"${RUNTIME_CLASSPATH}\" ${JAVA_MAIN_CLASS}"

${JAVA} ${JAVA_OPTS} -cp "${RUNTIME_CLASSPATH}" ${JAVA_MAIN_CLASS}