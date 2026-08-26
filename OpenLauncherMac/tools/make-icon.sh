#!/bin/bash
# Erzeugt AppIcon.icns aus dem Windows-Symbol OpenLauncher/app.ico.
# Damit tragen beide Fassungen exakt dasselbe Bild - kein zweites, abweichendes Symbol.
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
SOURCE_ICO="$DIR/../../OpenLauncher/app.ico"
OUT="$DIR/../OpenLauncher/Resources/AppIcon.icns"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

if [ ! -f "$SOURCE_ICO" ]; then
    echo "❌ Quelle nicht gefunden: $SOURCE_ICO" >&2
    exit 1
fi

# Groesste Ebene der .ico als PNG herausziehen (sips waehlt die hoechste Aufloesung).
sips -s format png "$SOURCE_ICO" --out "$WORK/base.png" >/dev/null

ICONSET="$WORK/AppIcon.iconset"
mkdir -p "$ICONSET"
# Die von macOS erwarteten Groessen (jeweils einfach und in doppelter Aufloesung).
for spec in "16 icon_16x16" "32 icon_16x16@2x" "32 icon_32x32" "64 icon_32x32@2x" \
            "128 icon_128x128" "256 icon_128x128@2x" "256 icon_256x256" \
            "512 icon_256x256@2x" "512 icon_512x512" "1024 icon_512x512@2x"; do
    set -- $spec
    sips -z "$1" "$1" "$WORK/base.png" --out "$ICONSET/$2.png" >/dev/null
done

mkdir -p "$(dirname "$OUT")"
iconutil -c icns "$ICONSET" -o "$OUT"
echo "✅ Symbol erzeugt: $OUT"
