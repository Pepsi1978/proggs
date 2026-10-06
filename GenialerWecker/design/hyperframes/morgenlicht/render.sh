#!/usr/bin/env bash
# Rendert die Morgenlicht-Schleife (dunkel und hell) mit HyperFrames und legt sie als
# kleine, stumme MP4 in die App. Aufruf aus diesem Ordner: bash render.sh
set -euo pipefail
cd "$(dirname "$0")"
ZIEL=../../../app/src/main/res/raw
ROH=$(mktemp -d)
mkdir -p "$ZIEL"
npx --yes hyperframes@0.8.138 render -o "$ROH/dunkel.mp4"
npx --yes hyperframes@0.8.138 render -c compositions/hell.html -o "$ROH/hell.mp4"
for v in dunkel hell; do
  # 720×1440, H.264 Main, ohne Tonspur (-an): Ein Video mit Ton dürfte dem Weckton nicht in die Quere kommen.
  ffmpeg -y -v error -i "$ROH/$v.mp4" -an -c:v libx264 -profile:v main -pix_fmt yuv420p \
    -preset slow -crf 26 -g 60 -movflags +faststart "$ZIEL/morgenlicht_$v.mp4"
done
rm -rf "$ROH"
ls -la "$ZIEL"/morgenlicht_*.mp4
