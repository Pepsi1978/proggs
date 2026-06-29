#!/usr/bin/env bash
#
# setup-keystores-macos.sh
# ------------------------------------------------------------------------------
# Holt alle App-Keystores + zugehoerige Secrets vom Laufwerk Y (WireGuard-Freigabe
# \\10.8.0.1\daten, auf macOS gemountet als /Volumes/daten) nach ~/SK/<App>/.
#
# Hintergrund: Alle Android-Apps nutzen denselben gemeinsamen Debug-Keystore
# (debug-shared.keystore). So ist die Debug-Signatur auf Windows UND macOS gleich
# und Apps lassen sich ueber Rechner-Grenzen hinweg installieren.
#
# Benutzung auf dem Mac (einmalig, bzw. nach jeder Keystore-Aenderung):
#   1. Laufwerk Y mounten (Finder -> Gehe zu -> Mit Server verbinden ->
#      smb://10.8.0.1/daten), oder per Terminal:
#        mkdir -p /Volumes/daten && mount_smbfs //10.8.0.1/daten /Volumes/daten
#   2. Dieses Skript ausfuehren:
#        bash ~/proggs/keystore-sync/setup-keystores-macos.sh
#      Optional mit eigenem Quellpfad:
#        bash ~/proggs/keystore-sync/setup-keystores-macos.sh /pfad/zu/Keystore
# ------------------------------------------------------------------------------
set -euo pipefail

# Quelle: 1. Argument, sonst typischer macOS-SMB-Mount.
SRC="${1:-/Volumes/daten/Keystore}"
DST="$HOME/SK"

APPS=(BestJournalAndroid BestJournalFrank EntropieReductor CortexAndroid NEMS VoiceKey)

echo "=== Keystore-Setup (macOS) ==="
echo "Quelle:  $SRC"
echo "Ziel:    $DST"
echo ""

if [ ! -d "$SRC" ]; then
  echo "FEHLER: Quell-Ordner '$SRC' nicht gefunden."
  echo "  -> Ist Laufwerk Y gemountet? (Finder: smb://10.8.0.1/daten)"
  echo "  -> Oder Quellpfad als Argument uebergeben:"
  echo "       bash $0 /Volumes/<name>/Keystore"
  exit 1
fi

for app in "${APPS[@]}"; do
  if [ -d "$SRC/$app" ]; then
    mkdir -p "$DST/$app"
    cp -f "$SRC/$app/"* "$DST/$app/" 2>/dev/null || true
    echo "  [OK] $app  <- $(ls "$SRC/$app" | tr '\n' ' ')"
  else
    echo "  [--] $app  (kein Ordner in der Quelle, uebersprungen)"
  fi
done

echo ""
echo "=== Verifikation: alle debug-shared.keystore muessen identisch sein ==="
if command -v shasum >/dev/null 2>&1; then
  shasum -a 256 "$DST"/*/debug-shared.keystore 2>/dev/null | sed "s|$HOME/||"
else
  sha256sum "$DST"/*/debug-shared.keystore 2>/dev/null | sed "s|$HOME/||"
fi

echo ""
echo "Fertig. Du kannst jetzt auf macOS bauen:"
echo "  cd ~/proggs/<App> && ./gradlew assembleDebug"
echo ""
echo "Hinweis: BestJournalAndroid-Testversion und EntropieReductor wurden auf den"
echo "gemeinsamen Debug-Keystore umgestellt. Falls auf einem Geraet die Meldung"
echo "'INSTALL_FAILED_UPDATE_INCOMPATIBLE' / 'Signatures do not match' kommt, die"
echo "betroffene App dort EINMAL deinstallieren und neu installieren."
