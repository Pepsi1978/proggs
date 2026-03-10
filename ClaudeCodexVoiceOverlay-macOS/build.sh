#!/bin/bash
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$PROJECT_DIR/ClaudeCodexVoiceOverlay"
BUILD_DIR="$PROJECT_DIR/build"
APP_NAME="ClaudeCodexVoiceOverlay"
APP_BUNDLE="$BUILD_DIR/$APP_NAME.app"

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

# Copy entitlements and sign the app so macOS keeps permissions across rebuilds
cp "$SRC_DIR/$APP_NAME.entitlements" "$APP_BUNDLE/Contents/Resources/"
codesign --force --sign - --entitlements "$SRC_DIR/$APP_NAME.entitlements" --deep "$APP_BUNDLE"

echo "=== Build erfolgreich: $APP_BUNDLE ==="
echo ""
echo "App starten:"
echo "  open $APP_BUNDLE"
echo ""
echo "Oder direkt:"
echo "  $APP_BUNDLE/Contents/MacOS/$APP_NAME"
