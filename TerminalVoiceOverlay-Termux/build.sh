#!/data/data/com.termux/files/usr/bin/bash
# Build script for Terminal Voice Overlay (Termux)

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== Terminal Voice Overlay - Build ==="

# Check Node.js
if ! command -v node &>/dev/null; then
  echo "Fehler: Node.js nicht installiert. Bitte 'pkg install nodejs' ausfuehren."
  exit 1
fi

# Check TypeScript compiler
if ! command -v tsc &>/dev/null; then
  echo "TypeScript Compiler nicht gefunden. Installiere..."
  npm install -g typescript
fi

# Check termux-api
if ! command -v termux-microphone-record &>/dev/null; then
  echo "Warnung: termux-api nicht installiert. Bitte 'pkg install termux-api' ausfuehren"
  echo "und die Termux:API App aus F-Droid/Play Store installieren."
fi

# Check .env
if [ ! -f ".env" ]; then
  echo "Warnung: .env Datei fehlt. Bitte .env.example kopieren und API-Keys eintragen."
fi

# Install dev dependencies if needed
if [ ! -d "node_modules/@types/node" ]; then
  echo "Installiere Abhaengigkeiten..."
  npm install --save-dev @types/node
fi

# Compile TypeScript (use node directly to avoid shebang issue on Android)
echo "Kompiliere TypeScript..."
TSC_BIN="$(npm root -g)/typescript/bin/tsc"
if [ -f "$TSC_BIN" ]; then
  node "$TSC_BIN"
else
  echo "Fehler: tsc nicht gefunden. Bitte 'npm install -g typescript' ausfuehren."
  exit 1
fi

echo "Build erfolgreich! Starten mit: node dist/main.js"
