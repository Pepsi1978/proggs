#!/bin/bash
# Hält Node.js in ~/.local/node auf der neuesten LTS: geprüfter Download, Austausch erst danach. Ein Abbruch vor dem Tausch lässt den alten Stand unberührt.
# Nach einem Wechsel der Hauptversion wird Kimi neu installiert, denn sein node-pty ist für die alte Fassung gebaut. Der Merker sorgt dafür, dass das auch nachgeholt wird.
set -euo pipefail
ZIEL="$HOME/.local/node"
KIMI_OFFEN="$HOME/.local/node.kimi-offen"
mkdir -p "$HOME/.local"
rm -rf "$HOME"/.local/node-neu.*   # Reste eines hart beendeten Laufs; sein trap läuft dann nicht.
[ -d "$ZIEL" ] || [ ! -d "$ZIEL.alt" ] || mv "$ZIEL.alt" "$ZIEL"

JETZT=""
[ ! -x "$ZIEL/bin/node" ] || JETZT=$("$ZIEL/bin/node" --version)
V=$(curl -fsSL https://nodejs.org/dist/index.json | jq -r '[.[] | select(.lts)][0].version // empty')
[ -n "$V" ] || { echo "Keine LTS-Version gefunden"; exit 1; }

if [ "$V" != "$JETZT" ]; then
  T=$(mktemp -d "$HOME/.local/node-neu.XXXXXX")
  trap 'rm -rf "$T"' EXIT
  cd "$T"
  D="node-$V-linux-x64.tar.xz"
  curl -fsSLO "https://nodejs.org/dist/$V/$D"
  curl -fsSLO "https://nodejs.org/dist/$V/SHASUMS256.txt"
  grep " $D\$" SHASUMS256.txt | sha256sum -c --quiet -
  mkdir neu
  tar -xJf "$D" --strip-components=1 -C neu
  [ -x neu/bin/node ]
  if [ -d "$ZIEL" ]; then
    # Was jemand ohne NPM_CONFIG_PREFIX direkt in Node installiert hat, zieht mit um; die neuen Dateien von Node und npm bleiben.
    cp -an "$ZIEL/lib/node_modules/." neu/lib/node_modules/
    cp -an "$ZIEL/bin/." neu/bin/
  fi
  if [ -n "$JETZT" ] && [ "${JETZT%%.*}" != "${V%%.*}" ]; then
    touch "$KIMI_OFFEN"
  fi
  rm -rf "$ZIEL.alt"
  [ ! -d "$ZIEL" ] || mv "$ZIEL" "$ZIEL.alt"
  mv neu "$ZIEL"
  rm -rf "$ZIEL.alt"
fi

if [ -e "$KIMI_OFFEN" ]; then
  PATH="$ZIEL/bin:$PATH" npm install -g --silent --allow-scripts=@moonshot-ai/kimi-code,node-pty @moonshot-ai/kimi-code@latest
  rm -f "$KIMI_OFFEN"
fi
