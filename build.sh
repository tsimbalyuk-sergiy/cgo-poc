#!/bin/bash

set -e

C_SOURCE="fibcalculator.c"
C_LIB_NAME="libfibcalculator.so"
GO_SOURCE="main.go"
GO_EXE_NAME="fib_main"

rm -f "$C_LIB_NAME" "$GO_EXE_NAME"

gcc -v -shared -fPIC -O3 -march=native -o "$C_LIB_NAME" "$C_SOURCE"
go build -ldflags="-s -w" -o "$GO_EXE_NAME" "$GO_SOURCE"
./$GO_EXE_NAME