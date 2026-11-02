#!/bin/bash
# Hält Node.js in ~/.local/node auf der neuesten LTS: geprüfter Download, Austausch erst danach. Ein Abbruch lässt den alten Stand unberührt.
# Nach einem Wechsel der Hauptversion wird Kimi neu installiert, denn sein node-pty ist für die alte Fassung gebaut.
set -euo pipefail
ZIEL="$HOME/.local/node"
JETZT=""
[ ! -x "$ZIEL/bin/node" ] || JETZT=$("$ZIEL/bin/node" --version)
V=$(curl -fsSL https://nodejs.org/dist/index.json | jq -r '[.[] | select(.lts)][0].version // empty')
[ -n "$V" ] || { echo "Keine LTS-Version gefunden"; exit 1; }
[ "$V" != "$JETZT" ] || exit 0

mkdir -p "$HOME/.local"
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
rm -rf "$ZIEL.alt"
[ ! -d "$ZIEL" ] || mv "$ZIEL" "$ZIEL.alt"
mv neu "$ZIEL"
rm -rf "$ZIEL.alt"
if [ -n "$JETZT" ] && [ "${JETZT%%.*}" != "${V%%.*}" ]; then
  npm install -g --silent --allow-scripts=@moonshot-ai/kimi-code,node-pty @moonshot-ai/kimi-code@latest
fi
