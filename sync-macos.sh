#!/bin/bash
# sync-macos.sh
#
# Zentrales One-Shot-Sync-Skript fuer Frank's Mac.
# Macht git pull und baut ALLE plattformuebergreifenden macOS-Apps neu.
# Idempotent — bei jedem Aufruf wieder ausfuehrbar, ohne Schaden.
#
# Aufruf: bash ~/proggs/sync-macos.sh
# Trigger-Phrase fuer Claude: "bringe dich auf den neuesten Stand"
#
# WICHTIG: Dieses Skript ist die EINZIGE zentrale Sync-Stelle. Wenn ein
# neues Mac-Projekt hinzukommt das gebaut werden muss, hier in der
# MAC_BUILD_TARGETS-Liste eintragen. Dann ist das Projekt automatisch
# Teil des "bringe dich auf den neuesten Stand"-Workflows.
#
# Was dieses Skript NICHT tut: /self-improve starten, Whiteboard-Cleanup,
# Researcher-Agents. Frank hat das explizit so gewollt — der Sync soll
# schlank und vorhersehbar bleiben.

set -e
cd "$HOME/proggs"

echo "================================================================"
echo "  macOS Sync — bringe alles auf den neuesten Stand"
echo "================================================================"

# Schritt 1: Repo aktualisieren
echo ""
echo "[1/3] git fetch + rebase"
echo "----------------------------------------------------------------"
git fetch origin
# Falls lokale Aenderungen liegen (uncommitted): autostash, dann pop
git rebase --autostash origin/main || {
    echo ""
    echo "FEHLER: git rebase fehlgeschlagen. Manuell aufloesen, dann nochmal."
    exit 1
}

# Schritt 2: Alle plattformuebergreifenden Mac-Build-Targets durchgehen
echo ""
echo "[2/3] Mac-Apps neu bauen"
echo "----------------------------------------------------------------"

# Liste aller Mac-Projekte mit build.sh. Bei neuen Projekten hier ergaenzen.
MAC_BUILD_TARGETS=(
    "TerminalVoiceOverlay-macOS"
    "ClaudeCodexVoiceOverlay-macOS"
)

for target in "${MAC_BUILD_TARGETS[@]}"; do
    if [ ! -d "$HOME/proggs/$target" ]; then
        echo "  [skip] $target — Verzeichnis fehlt"
        continue
    fi
    if [ ! -f "$HOME/proggs/$target/build.sh" ]; then
        echo "  [skip] $target — build.sh fehlt"
        continue
    fi
    echo ""
    echo "  >>> Baue $target ..."
    (cd "$HOME/proggs/$target" && bash build.sh) || {
        echo "  FEHLER beim Bauen von $target — naechster Build wird versucht"
        continue
    }
    echo "  >>> $target fertig"
done

# Schritt 3: Status-Zusammenfassung
echo ""
echo "[3/3] Fertig"
echo "----------------------------------------------------------------"
echo "Alle Mac-Apps sind auf dem neuesten Stand."
echo ""
echo "Voice-Overlay-Config in ~/SK/VoiceOverlays/:"
ls -la "$HOME/SK/VoiceOverlays/" 2>/dev/null || echo "  (Ordner noch nicht angelegt — wird beim ersten App-Start erzeugt)"
echo ""
echo "Naechste Schritte:"
echo "  open ~/proggs/TerminalVoiceOverlay-macOS/build/TerminalVoiceOverlay.app"
echo "  open ~/proggs/ClaudeCodexVoiceOverlay-macOS/build/ClaudeCodexVoiceOverlay.app"
echo ""
