#!/bin/bash
# sherpa-onnx (C-API) fuer die Offline-Transkription der Voice-Overlays (Parakeet primeline).
# Wird von TerminalVoiceOverlay-macOS/build.sh und ClaudeCodexVoiceOverlay-macOS/build.sh eingebunden.
# Laedt die offizielle Release-Bibliothek einmal in ~/Library/Caches/sherpa-onnx (nicht ins Repo, 33 MB)
# und prueft sie per SHA-256. Danach setzt es SHERPA_LIB (dylibs) und SHERPA_HEADER (c-api.h).

SHERPA_VERSION="1.13.8"
SHERPA_NAME="sherpa-onnx-v${SHERPA_VERSION}-osx-arm64-shared-lib"
SHERPA_TAR_SHA="ae77050cdae565496059d96f5ab33d77b397a0864282a4e4a26e3b3b3effb948"
SHERPA_HEADER_SHA="2a1b95084be8fd1deb3228fcad2fd3f7f0258b64582f7402281ec174c7b7f4ce"

ensure_sherpa() {
    local cache="$HOME/Library/Caches/sherpa-onnx"
    SHERPA_LIB="$cache/$SHERPA_NAME/lib"
    SHERPA_HEADER="$cache/$SHERPA_NAME/include/c-api.h"
    if [ -f "$SHERPA_LIB/libsherpa-onnx-c-api.dylib" ] && [ -f "$SHERPA_LIB/libonnxruntime.dylib" ] \
        && [ -f "$SHERPA_HEADER" ] && [ "$(shasum -a 256 "$SHERPA_HEADER" | cut -d' ' -f1)" = "$SHERPA_HEADER_SHA" ]; then
        return 0
    fi
    echo "=== Lade sherpa-onnx $SHERPA_VERSION (einmalig) ==="
    mkdir -p "$cache"
    local tar="$cache/$SHERPA_NAME.tar.bz2"
    curl -fsSL -o "$tar" "https://github.com/k2-fsa/sherpa-onnx/releases/download/v${SHERPA_VERSION}/${SHERPA_NAME}.tar.bz2" || return 1
    [ "$(shasum -a 256 "$tar" | cut -d' ' -f1)" = "$SHERPA_TAR_SHA" ] || { echo "FEHLER: sherpa-onnx-Archiv hat falsche Pruefsumme"; return 1; }
    tar xjf "$tar" -C "$cache" || return 1
    mkdir -p "$cache/$SHERPA_NAME/include"
    curl -fsSL -o "$SHERPA_HEADER" "https://raw.githubusercontent.com/k2-fsa/sherpa-onnx/v${SHERPA_VERSION}/sherpa-onnx/c-api/c-api.h" || return 1
    [ "$(shasum -a 256 "$SHERPA_HEADER" | cut -d' ' -f1)" = "$SHERPA_HEADER_SHA" ] || { echo "FEHLER: c-api.h hat falsche Pruefsumme"; return 1; }
}
