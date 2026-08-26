#!/bin/bash
# Legt ein Startsymbol fuer den OpenLauncher auf den Schreibtisch - OHNE Verknuepfungspfeil.
#
# Warum kein Alias und kein Symlink? Beide bekommen von Finder das kleine Pfeil-Abzeichen
# aufgestempelt. Ein winziges eigenes Programm dagegen ist ein ganz normales Programm - es traegt
# kein Abzeichen und laesst sich frei benennen. Es enthaelt nur eine Zeile: "oeffne den echten
# OpenLauncher". Dadurch bleibt es auch nach jedem Neubau aktuell, statt zu veralten.
set -e

DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$DIR/.." && pwd)"
TARGET_APP="$PROJECT_DIR/build/OpenLauncher.app"
ICON="$PROJECT_DIR/OpenLauncher/Resources/AppIcon.icns"
DESKTOP_APP="$HOME/Desktop/OpenLauncher.app"

if [ ! -d "$TARGET_APP" ]; then
    echo "❌ Der Launcher ist noch nicht gebaut: $TARGET_APP" >&2
    echo "   Zuerst 'bash $PROJECT_DIR/build.sh' ausfuehren." >&2
    exit 1
fi

rm -rf "$DESKTOP_APP"
mkdir -p "$DESKTOP_APP/Contents/MacOS" "$DESKTOP_APP/Contents/Resources"

cat > "$DESKTOP_APP/Contents/Info.plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleName</key>
    <string>OpenLauncher</string>
    <key>CFBundleIdentifier</key>
    <string>de.frank.OpenLauncher.desktop</string>
    <key>CFBundleVersion</key>
    <string>1.0</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleExecutable</key>
    <string>start</string>
    <key>CFBundleIconFile</key>
    <string>AppIcon</string>
    <!-- Kein eigenes Fenster, kein Dock-Symbol: das Startprogramm oeffnet nur den echten Launcher. -->
    <key>LSUIElement</key>
    <true/>
</dict>
</plist>
PLIST

cat > "$DESKTOP_APP/Contents/MacOS/start" <<START
#!/bin/bash
exec /usr/bin/open "$TARGET_APP"
START
chmod +x "$DESKTOP_APP/Contents/MacOS/start"

[ -f "$ICON" ] && cp "$ICON" "$DESKTOP_APP/Contents/Resources/AppIcon.icns"

# Signieren, damit macOS das Programm ohne Warnung startet.
SIGNING_IDENTITY="Frank Local Dev"
if security find-identity -v -p codesigning | grep -q "$SIGNING_IDENTITY"; then
    codesign --force --sign "$SIGNING_IDENTITY" "$DESKTOP_APP" >/dev/null 2>&1 || true
else
    codesign --force --sign - "$DESKTOP_APP" >/dev/null 2>&1 || true
fi

# Finder das neue Symbol zeigen lassen (sonst bleibt manchmal das Platzhaltersymbol stehen).
touch "$DESKTOP_APP"
echo "✅ Schreibtisch-Symbol angelegt: $DESKTOP_APP (ohne Verknuepfungspfeil)"
