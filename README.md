# CGO-JNI-JNA POC

1. [prepare](#prepare-env)
2. [build](#build)
3. [run](#run)
4. [java-jni](#java-jni)
5. [java-jna](#java-jna)

## prepare env

### ubuntu
```shell
sudo apt update
sudo apt install build-essential golang-go
```

### fedora
```shell
sudo dnf check-update
sudo dnf group install development-tools
sudo dnf install golang
```

### macos
```shell
xcode-select --install
```

check env
verify both have some yield
```shell
gcc --version
go version
```

## build
```shell
chmod +x ./build.sh
./build.sh
```

## run
```shell
./fib_main
```

## java jni
```shell
chmod +x build_jni.sh && ./build_jni.sh
```

## java jna
```shell
chmod +x build_jna.sh && ./build_jna.sh
```