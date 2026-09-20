#!/bin/bash
# Baut die Updater-Zentrale macOS als .app-Buendel.
#
# Gegenstueck zu UpdateZentrale.csproj unter Windows:
#   - swiftc statt MSBuild (kein Xcode-Projekt, kein SwiftPM - Projekt-Anker der macOS-Apps)
#   - der Build-Zeitstempel wird HIER gesetzt (Gegenstueck zum MSBuild-Target "SetBuildTimestamp"),
#     damit in der App nie ein von Hand getippter - und damit moeglicherweise falscher - Zeitpunkt steht
#   - Signieren zuletzt mit echtem Zertifikat, OHNE --deep (Best-Practice §G17 / Almanach §G6)
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$PROJECT_DIR/UpdaterZentrale"
BUILD_DIR="$PROJECT_DIR/build"
BINARY_NAME="UpdaterZentrale"
APP_NAME="Updater-Zentrale macOS"
APP_BUNDLE="$BUILD_DIR/$APP_NAME.app"

echo "=== Baue $APP_NAME ==="

# Zeigt `xcode-select -p` auf Xcode.app und ist dessen Lizenz nicht angenommen, bricht swiftc mit
# "You have not agreed to the Xcode license agreements" ab. Die Command Line Tools bauen ohne
# Lizenzabfrage - dauerhaft loesbar nur vom Benutzer: `sudo xcodebuild -license accept`.
if [ -x "/Library/Developer/CommandLineTools/usr/bin/swiftc" ]; then
    export DEVELOPER_DIR="/Library/Developer/CommandLineTools"
fi

rm -rf "$BUILD_DIR"
mkdir -p "$APP_BUNDLE/Contents/MacOS"
mkdir -p "$APP_BUNDLE/Contents/Resources"

# Info.plist mit echtem Build-Zeitstempel (dd.MM.yyyy, HH:mm).
BUILD_TIMESTAMP="$(date '+%d.%m.%Y, %H:%M')"
sed "s/__BUILD_TIMESTAMP__/$BUILD_TIMESTAMP/" "$SRC_DIR/Info.plist" > "$APP_BUNDLE/Contents/Info.plist"

# Reihenfolge: Modelle, Dienste, Anbieter, Ansichtsmodelle, Ansichten, Einstiegspunkt.
# main.swift MUSS zuletzt stehen - nur die Datei mit diesem Namen darf Code auf oberster Ebene haben.
SWIFT_FILES=(
    "$SRC_DIR/Models/Models.swift"
    "$SRC_DIR/Services/Pfade.swift"
    "$SRC_DIR/Services/Versionen.swift"
    "$SRC_DIR/Services/Kommandozeile.swift"
    "$SRC_DIR/Services/Einstellungen.swift"
    "$SRC_DIR/Services/Katalogdienst.swift"
    "$SRC_DIR/Services/Protokollierung.swift"
    "$SRC_DIR/Services/Prozessdienst.swift"
    "$SRC_DIR/Services/Systemdienst.swift"
    "$SRC_DIR/Services/Dialoge.swift"
    "$SRC_DIR/Services/Terminal.swift"
    "$SRC_DIR/Services/Darstellung.swift"
    "$SRC_DIR/Providers/Aktualisierer.swift"
    "$SRC_DIR/Providers/BrewAktualisierer.swift"
    "$SRC_DIR/Providers/CliAktualisierer.swift"
    "$SRC_DIR/Providers/RepoSkriptAktualisierer.swift"
    "$SRC_DIR/ViewModels/ProgrammViewModel.swift"
    "$SRC_DIR/ViewModels/HauptViewModel.swift"
    "$SRC_DIR/Views/Controls.swift"
    "$SRC_DIR/Views/ProgrammKarteView.swift"
    "$SRC_DIR/Views/HauptFensterController.swift"
    "$SRC_DIR/AppDelegate.swift"
    "$SRC_DIR/main.swift"
)

swiftc \
    -o "$APP_BUNDLE/Contents/MacOS/$BINARY_NAME" \
    -target arm64-apple-macos13.0 \
    -sdk "$(xcrun --show-sdk-path)" \
    -O \
    -framework AppKit \
    -framework Foundation \
    "${SWIFT_FILES[@]}"

cp "$SRC_DIR/UpdaterZentrale.entitlements" "$APP_BUNDLE/Contents/Resources/"

# Rueckfallebene fuer den Katalog: liegt das Repo nicht am erwarteten Ort, liest die App diese
# Kopie. Der gepflegte Katalog bleibt aber programs.json im Projektordner - ein neues Programm
# ist dort eine JSON-Aenderung, kein Neubau.
cp "$PROJECT_DIR/programs.json" "$APP_BUNDLE/Contents/Resources/"

# App-Symbol. Fehlt es, wird es erzeugt.
if [ ! -f "$SRC_DIR/Resources/AppIcon.icns" ]; then
    bash "$PROJECT_DIR/tools/make-icon.sh"
fi
cp "$SRC_DIR/Resources/AppIcon.icns" "$APP_BUNDLE/Contents/Resources/"

# Signieren ZULETZT (nach der finalen Info.plist - sonst verweigert TCC still, Almanach §H4).
# KEIN --deep: das Buendel hat keine verschachtelten Buendel, und --deep gilt als fehleranfaellig.
SIGNING_IDENTITY="Frank Local Dev"
if security find-identity -v -p codesigning | grep -q "$SIGNING_IDENTITY"; then
    codesign --force --sign "$SIGNING_IDENTITY" \
        --entitlements "$SRC_DIR/UpdaterZentrale.entitlements" "$APP_BUNDLE"
    echo "=== Signiert mit Zertifikat: $SIGNING_IDENTITY ==="
else
    echo "⚠ Zertifikat '$SIGNING_IDENTITY' nicht gefunden, Fallback auf Ad-hoc-Signierung"
    echo "  Berechtigungen (Apple Events) muessen dann nach jedem Rebuild neu erteilt werden!"
    codesign --force --sign - --entitlements "$SRC_DIR/UpdaterZentrale.entitlements" "$APP_BUNDLE"
fi

echo "=== Build erfolgreich: $APP_BUNDLE ($BUILD_TIMESTAMP) ==="
echo ""
echo "App starten:"
echo "  open \"$APP_BUNDLE\""
