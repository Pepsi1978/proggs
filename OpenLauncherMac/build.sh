#!/bin/bash
# Baut OpenLauncher (macOS) als .app-Bundle.
#
# Gegenstueck zu OpenLauncher.csproj + update-launcher.ps1 unter Windows:
#   - swiftc statt MSBuild (kein Xcode-Projekt, kein SwiftPM - Projekt-Anker der macOS-Apps)
#   - der Build-Zeitstempel wird HIER gesetzt (Gegenstueck zum MSBuild-Target "SetBuildTimestamp"),
#     damit in der App nie ein von Hand getippter - und damit moeglicherweise falscher - Zeitpunkt steht
#   - Signieren zuletzt mit echtem Zertifikat, OHNE --deep (Best-Practice §G17 / Almanach §G6)
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$PROJECT_DIR/OpenLauncher"
BUILD_DIR="$PROJECT_DIR/build"
APP_NAME="OpenLauncher"
APP_BUNDLE="$BUILD_DIR/$APP_NAME.app"

echo "=== Baue $APP_NAME ==="

rm -rf "$BUILD_DIR"
mkdir -p "$APP_BUNDLE/Contents/MacOS"
mkdir -p "$APP_BUNDLE/Contents/Resources"

# Info.plist mit echtem Build-Zeitstempel (deutsche Schreibweise wie unter Windows: dd.MM.yyyy, HH.mm)
BUILD_TIMESTAMP="$(date '+%d.%m.%Y, %H.%M')"
sed "s/__BUILD_TIMESTAMP__/$BUILD_TIMESTAMP/" "$SRC_DIR/Info.plist" > "$APP_BUNDLE/Contents/Info.plist"

SWIFT_FILES=(
    "$SRC_DIR/Models/Models.swift"
    "$SRC_DIR/Services/Paths.swift"
    "$SRC_DIR/Services/Logger.swift"
    "$SRC_DIR/Services/Shell.swift"
    "$SRC_DIR/Services/JSONValue.swift"
    "$SRC_DIR/Services/Theme.swift"
    "$SRC_DIR/Services/LayoutSettings.swift"
    "$SRC_DIR/Services/OpenCodeVariantCatalog.swift"
    "$SRC_DIR/Services/LmStudioService.swift"
    "$SRC_DIR/Services/ModelRegistry.swift"
    "$SRC_DIR/Services/ModelDefaultsService.swift"
    "$SRC_DIR/Services/InstructionProfileService.swift"
    "$SRC_DIR/Services/OpenRouterService.swift"
    "$SRC_DIR/Services/OpenCodeUpdateService.swift"
    "$SRC_DIR/Services/TerminalLauncher.swift"
    "$SRC_DIR/Services/OpenLauncherService.swift"
    "$SRC_DIR/ViewModels/MainViewModel.swift"
    "$SRC_DIR/Views/Controls.swift"
    "$SRC_DIR/Views/ModelListView.swift"
    "$SRC_DIR/Views/ProviderTableView.swift"
    "$SRC_DIR/Views/PanelViews.swift"
    "$SRC_DIR/Views/Dialogs.swift"
    "$SRC_DIR/Views/MainWindowController.swift"
    "$SRC_DIR/AppDelegate.swift"
    "$SRC_DIR/main.swift"
)

swiftc \
    -o "$APP_BUNDLE/Contents/MacOS/$APP_NAME" \
    -target arm64-apple-macos13.0 \
    -sdk "$(xcrun --show-sdk-path)" \
    -O \
    -framework AppKit \
    -framework Foundation \
    "${SWIFT_FILES[@]}"

cp "$SRC_DIR/OpenLauncher.entitlements" "$APP_BUNDLE/Contents/Resources/"

# Signieren ZULETZT (nach der finalen Info.plist - sonst verweigert TCC still, Almanach §H4).
# KEIN --deep: das Bundle hat keine verschachtelten Bundles, und --deep gilt als fehleranfaellig.
SIGNING_IDENTITY="Frank Local Dev"
if security find-identity -v -p codesigning | grep -q "$SIGNING_IDENTITY"; then
    codesign --force --sign "$SIGNING_IDENTITY" \
        --entitlements "$SRC_DIR/OpenLauncher.entitlements" "$APP_BUNDLE"
    echo "=== Signiert mit Zertifikat: $SIGNING_IDENTITY ==="
else
    echo "⚠ Zertifikat '$SIGNING_IDENTITY' nicht gefunden, Fallback auf Ad-hoc-Signierung"
    echo "  Berechtigungen (Apple Events) muessen dann nach jedem Rebuild neu erteilt werden!"
    codesign --force --sign - --entitlements "$SRC_DIR/OpenLauncher.entitlements" "$APP_BUNDLE"
fi

echo "=== Build erfolgreich: $APP_BUNDLE ($BUILD_TIMESTAMP) ==="
echo ""
echo "App starten:"
echo "  open $APP_BUNDLE"
