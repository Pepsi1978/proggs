#!/bin/bash
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$PROJECT_DIR/ClaudeCodexVoiceOverlay"
BUILD_DIR="$PROJECT_DIR/build"
APP_NAME="ClaudeCodexVoiceOverlay"
APP_BUNDLE="$BUILD_DIR/$APP_NAME.app"

# Voice-Overlay-Config-Datei in $HOME/SK/VoiceOverlays/ installieren falls
# noch nicht vorhanden. Idempotent — vorhandene User-Anpassungen werden NIE
# ueberschrieben. Das macht "git pull && bash build.sh" zu einem one-shot
# Setup auf einer frischen Mac-Installation.
# Hinweis: voice-prompt.txt wird nicht mehr installiert. Whisper bekommt das
# Audio ohne Prompt-Hint; themenspezifische Profile passieren in Gemini.
SK_VOICE_DIR="$HOME/SK/VoiceOverlays"
TEMPLATE_DIR="$PROJECT_DIR/../voice-overlay-templates"
mkdir -p "$SK_VOICE_DIR"
for f in gemini-correction-prompt.txt; do
    if [ ! -f "$SK_VOICE_DIR/$f" ] && [ -f "$TEMPLATE_DIR/$f" ]; then
        cp "$TEMPLATE_DIR/$f" "$SK_VOICE_DIR/$f"
        echo "Installed: $SK_VOICE_DIR/$f"
    fi
done

echo "=== Building $APP_NAME ==="

# Clean
rm -rf "$BUILD_DIR"
mkdir -p "$APP_BUNDLE/Contents/MacOS"
mkdir -p "$APP_BUNDLE/Contents/Resources"

# Copy Info.plist
cp "$SRC_DIR/Info.plist" "$APP_BUNDLE/Contents/Info.plist"

# Compile
SWIFT_FILES=(
    "$SRC_DIR/Config.swift"
    "$SRC_DIR/AppWatcher.swift"
    "$SRC_DIR/AudioRecorder.swift"
    "$SRC_DIR/GroqWhisperClient.swift"
    "$SRC_DIR/GeminiClient.swift"
    "$SRC_DIR/InputController.swift"
    "$SRC_DIR/OverlayPanel.swift"
    "$SRC_DIR/ErrorDescriptions.swift"
    "$SRC_DIR/AppDelegate.swift"
    "$SRC_DIR/main.swift"
)

swiftc \
    -o "$APP_BUNDLE/Contents/MacOS/$APP_NAME" \
    -target arm64-apple-macos13.0 \
    -sdk "$(xcrun --show-sdk-path)" \
    -framework AppKit \
    -framework AVFoundation \
    -framework CoreGraphics \
    "${SWIFT_FILES[@]}"

# Copy entitlements and sign with persistent certificate (not ad-hoc)
# This ensures TCC permissions survive rebuilds, because macOS identifies
# the app by certificate identity instead of binary hash.
cp "$SRC_DIR/$APP_NAME.entitlements" "$APP_BUNDLE/Contents/Resources/"

SIGNING_IDENTITY="Frank Local Dev"
if security find-identity -v -p codesigning | grep -q "$SIGNING_IDENTITY"; then
    codesign --force --sign "$SIGNING_IDENTITY" --entitlements "$SRC_DIR/$APP_NAME.entitlements" --deep "$APP_BUNDLE"
    echo "=== Signiert mit Zertifikat: $SIGNING_IDENTITY ==="
else
    echo "⚠ Zertifikat '$SIGNING_IDENTITY' nicht gefunden, fallback auf ad-hoc Signierung"
    echo "  TCC-Berechtigungen gehen bei Rebuild verloren!"
    codesign --force --sign - --entitlements "$SRC_DIR/$APP_NAME.entitlements" --deep "$APP_BUNDLE"
fi

echo "=== Build erfolgreich: $APP_BUNDLE ==="
echo ""
echo "App starten:"
echo "  open $APP_BUNDLE"
echo ""
echo "Oder direkt:"
echo "  $APP_BUNDLE/Contents/MacOS/$APP_NAME"
