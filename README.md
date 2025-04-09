# CGO-JNI POC

1. [prepare](#prepare-env)
2. [build](#build)
3. [run](#run)
4. [java](#java)

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

## java
```shell
chmod +x build_jni.sh && ./build_jni.sh
```