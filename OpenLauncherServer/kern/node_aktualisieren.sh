#!/bin/bash
# Hält Node.js in ~/.local/node aktuell: geprüfter Download, Austausch erst danach. Ein Abbruch lässt den alten Stand unberührt.
# Automatisch nur innerhalb der Hauptversion, denn node-pty von Kimi hängt daran. Fehlt Node ganz, kommt die neueste LTS.
set -euo pipefail
ZIEL="$HOME/.local/node"
LISTE=$(curl -fsSL https://nodejs.org/dist/index.json)
JETZT=""
if [ -x "$ZIEL/bin/node" ]; then
  JETZT=$("$ZIEL/bin/node" --version)
  V=$(jq -r --arg h "${JETZT%%.*}." '[.[] | select(.version | startswith($h))][0].version // empty' <<<"$LISTE")
else
  V=$(jq -r '[.[] | select(.lts)][0].version // empty' <<<"$LISTE")
fi
[ -n "$V" ] || { echo "Keine passende Node-Version gefunden"; exit 1; }
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
