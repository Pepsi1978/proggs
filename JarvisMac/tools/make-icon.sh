#!/bin/bash
# Erzeugt Resources/AppIcon.icns aus tools/make-icon.swift.
set -e
export DEVELOPER_DIR=/Library/Developer/CommandLineTools
DIR="$(cd "$(dirname "$0")/.." && pwd)"
TMP="$(mktemp -d)"
swiftc -o "$TMP/icon" "$DIR/tools/make-icon.swift" -framework AppKit
"$TMP/icon" "$TMP/icon.png"
SET="$TMP/AppIcon.iconset"; mkdir -p "$SET"
for s in 16 32 128 256 512; do
  sips -z $s $s "$TMP/icon.png" --out "$SET/icon_${s}x${s}.png" >/dev/null
  d=$((s*2)); sips -z $d $d "$TMP/icon.png" --out "$SET/icon_${s}x${s}@2x.png" >/dev/null
done
iconutil -c icns "$SET" -o "$DIR/Resources/AppIcon.icns"
rm -rf "$TMP"
echo "AppIcon.icns erzeugt"
