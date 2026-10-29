#!/bin/bash
# Baut Jarvis (macOS) als .app-Bundle – swiftc ohne Xcode-Projekt, wie OpenLauncherMac.
# Dateiliste per find: neue Swift-Dateien unter Sources/ werden automatisch mitgebaut.
# Aufruf: bash build.sh            → nur bauen (build/Jarvis.app)
#         bash build.sh install    → bauen, nach /Applications/Jarvis.app installieren und starten
#         bash build.sh check      → nur Typprüfung, kein Bundle
set -e
export DEVELOPER_DIR=/Library/Developer/CommandLineTools

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$PROJECT_DIR/build"
APP_BUNDLE="$BUILD_DIR/Jarvis.app"
SDK="$(xcrun --show-sdk-path)"
SWIFT_FILES=()
while IFS= read -r f; do SWIFT_FILES+=("$f"); done < <(find "$PROJECT_DIR/Sources" -name '*.swift' | sort)

PLUGINS="/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/usr/lib/swift/host/plugins"
# SwiftUI-Makros (@State, @Entry …) liegen nur in Xcode; die Command Line Tools laden sie von dort (ohne Lizenzprüfung).
FLAGS=(-target arm64-apple-macos26.0 -sdk "$SDK" -swift-version 5 -parse-as-library -plugin-path "$PLUGINS"
       -framework AppKit -framework SwiftUI -framework AVFoundation -framework Speech
       -framework CoreAudio -framework AudioToolbox -framework Carbon -framework UserNotifications)

if [ "$1" = "check" ]; then
    swiftc -typecheck "${FLAGS[@]}" "${SWIFT_FILES[@]}"
    echo "=== Typprüfung ok ==="
    exit 0
fi

echo "=== Baue Jarvis ==="
rm -rf "$BUILD_DIR"
mkdir -p "$APP_BUNDLE/Contents/MacOS" "$APP_BUNDLE/Contents/Resources"
BUILD_TIMESTAMP="$(date '+%d.%m.%Y, %H:%M')"
sed "s/__BUILD_TIMESTAMP__/$BUILD_TIMESTAMP/" "$PROJECT_DIR/Info.plist" > "$APP_BUNDLE/Contents/Info.plist"

swiftc -o "$APP_BUNDLE/Contents/MacOS/Jarvis" -O "${FLAGS[@]}" "${SWIFT_FILES[@]}"

if [ ! -f "$PROJECT_DIR/Resources/AppIcon.icns" ]; then
    bash "$PROJECT_DIR/tools/make-icon.sh" || true
fi
[ -f "$PROJECT_DIR/Resources/AppIcon.icns" ] && cp "$PROJECT_DIR/Resources/AppIcon.icns" "$APP_BUNDLE/Contents/Resources/"

# Signieren zuletzt (nach der finalen Info.plist, sonst verweigert TCC still). Kein --deep.
SIGNING_IDENTITY="Frank Local Dev"
if security find-identity -v -p codesigning | grep -q "$SIGNING_IDENTITY"; then
    codesign --force --sign "$SIGNING_IDENTITY" --entitlements "$PROJECT_DIR/Jarvis.entitlements" "$APP_BUNDLE"
    echo "=== Signiert mit: $SIGNING_IDENTITY ==="
else
    echo "⚠ Zertifikat '$SIGNING_IDENTITY' fehlt – Ad-hoc-Signierung, Berechtigungen nach jedem Build neu erteilen"
    codesign --force --sign - --entitlements "$PROJECT_DIR/Jarvis.entitlements" "$APP_BUNDLE"
fi
echo "=== Build erfolgreich: $APP_BUNDLE ($BUILD_TIMESTAMP) ==="

if [ "$1" = "install" ]; then
    pkill -x Jarvis 2>/dev/null || true
    sleep 1
    rm -rf /Applications/Jarvis.app
    ditto "$APP_BUNDLE" /Applications/Jarvis.app
    open /Applications/Jarvis.app
    echo "=== Installiert und gestartet: /Applications/Jarvis.app ==="
fi
