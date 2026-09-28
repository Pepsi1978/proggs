#!/usr/bin/env bash
# Wrapper um die Flags von build-android-arm64-v8a.sh (v1.13.8), aber mit Ninja statt make
# mit TTS NUR fuer Supertonic + Pocket (Patch tts-ohne-espeak.patch, kein espeak-ng/piper).
set -ex
SDK=/c/Users/barwa/AppData/Local/Android/Sdk
export ANDROID_NDK=$SDK/ndk/28.2.13676358
export PATH=$SDK/cmake/3.31.6/bin:$PATH
ROOT=/c/Users/barwa/build/sherpa-onnx
export SHERPA_ONNXRUNTIME_LIB_DIR=$ROOT/ort/jni/arm64-v8a
export SHERPA_ONNXRUNTIME_INCLUDE_DIR=$ROOT/ort/headers
dir=$ROOT/src/build-android-arm64-v8a-tts
mkdir -p $dir && cd $dir
cmake -G Ninja -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK/build/cmake/android.toolchain.cmake" \
    -DSHERPA_ONNX_ENABLE_TTS=ON -DSHERPA_ONNX_TTS_OHNE_ESPEAK=ON \
    -DSHERPA_ONNX_ENABLE_SPEAKER_DIARIZATION=OFF \
    -DSHERPA_ONNX_ENABLE_BINARY=OFF \
    -DBUILD_PIPER_PHONMIZE_EXE=OFF \
    -DBUILD_PIPER_PHONMIZE_TESTS=OFF \
    -DBUILD_ESPEAK_NG_EXE=OFF \
    -DBUILD_ESPEAK_NG_TESTS=OFF \
    -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_SHARED_LIBS=ON \
    -DSHERPA_ONNX_ENABLE_PYTHON=OFF \
    -DSHERPA_ONNX_ENABLE_TESTS=OFF \
    -DSHERPA_ONNX_ENABLE_CHECK=OFF \
    -DSHERPA_ONNX_ENABLE_PORTAUDIO=OFF \
    -DSHERPA_ONNX_ENABLE_JNI=ON \
    -DSHERPA_ONNX_LINK_LIBSTDCPP_STATICALLY=OFF \
    -DSHERPA_ONNX_ENABLE_C_API=OFF \
    -DCMAKE_INSTALL_PREFIX=./install \
    -DSHERPA_ONNX_ENABLE_RKNN=OFF \
    -DSHERPA_ONNX_ENABLE_QNN=OFF \
    -DANDROID_ABI="arm64-v8a" \
    -DANDROID_PLATFORM=android-26 ..
cmake --build . -j 8
cmake --install . --strip
cp -fv "$SHERPA_ONNXRUNTIME_LIB_DIR/libonnxruntime.so" install/lib
