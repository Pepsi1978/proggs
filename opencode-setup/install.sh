#!/usr/bin/env bash
#
# install.sh — OpenCode-Umgebung aus dem plattformuebergreifenden Speicher installieren (macOS / Linux).
#
# Kopiert ALLES aus opencode-setup/ an seinen Platz unter ~/.config/opencode/, sodass OpenCode
# auf einem frischen Rechner 1:1 dieselbe Umgebung hat: globale Config, globale Regeln (AGENTS.md),
# globale Agents, lokale Plugins, TUI-Plugin-Dependencies, Notifier-Sounds. Danach folgt ein Voraussetzungs-Check
# (SK-Keys, OPENROUTER_API_KEY, WireGuard/Second-Brain, opencode-Auth) mit klarer TODO-Liste.
#
# Voraussetzung: OpenCode-CLI ist bereits installiert (siehe README, Schritt 1).
# Aufruf:        bash ~/proggs/opencode-setup/install.sh
#
# Idempotent: vorhandene Dateien werden vor dem Ueberschreiben nach .backup-<zeit>/ gesichert.
# Die plattformspezifische "shell"-Zeile wird auf "bash" gesetzt; opencode-notifier.json wird mit
# den korrekten lokalen Sound-Pfaden NEU erzeugt (NICHT die Windows-Pfade aus dem Repo uebernehmen).

set -euo pipefail

SRC="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"   # opencode-setup/
DST="${OPENCODE_SETUP_DST:-$HOME/.config/opencode}"   # Zielort (per Env ueberschreibbar, z.B. zum Testen)

green() { printf '\033[32m%s\033[0m\n' "$1"; }
yellow() { printf '\033[33m%s\033[0m\n' "$1"; }
red() { printf '\033[31m%s\033[0m\n' "$1"; }

echo "== OpenCode-Setup -> $DST =="

# --- 0) OpenCode installiert? (nur Hinweis, kein Abbruch) ---
if command -v opencode >/dev/null 2>&1; then
  green "OK  OpenCode-CLI gefunden: $(opencode --version 2>/dev/null || echo '?')"
else
  red "!!  OpenCode-CLI NICHT gefunden. Zuerst installieren (siehe README, Schritt 1), z.B.:"
  echo "      brew install anomalyco/tap/opencode      # macOS (empfohlen)"
  echo "      curl -fsSL https://opencode.ai/install | bash"
fi

# --- 1) Zielverzeichnisse + Backup ---
mkdir -p "$DST/agents" "$DST/plugins" "$DST/sounds" "$DST/skill"
backup() {  # $1 = Zielpfad: sichern, falls vorhanden
  if [ -e "$1" ]; then
    local bdir
    bdir="$DST/.backup-$(date +%Y%m%d-%H%M%S 2>/dev/null || echo manual)"
    mkdir -p "$bdir"
    cp -R "$1" "$bdir/" 2>/dev/null || true
  fi
}

# --- 2) Globale Config (shell -> bash fuer macOS/Linux) ---
backup "$DST/opencode.jsonc"
sed 's/"shell": "pwsh"/"shell": "bash"/' "$SRC/opencode.jsonc" > "$DST/opencode.jsonc"
green "OK  opencode.jsonc (shell=bash)"

backup "$DST/tui.json"
cp "$SRC/tui.json" "$DST/tui.json"
green "OK  tui.json (TUI-Plugins)"

# --- 3) Globale Regeln, Agents, Plugins ---
backup "$DST/AGENTS.md"
cp "$SRC/AGENTS-global.md" "$DST/AGENTS.md"
green "OK  AGENTS.md (globale Regeln)"

if cp "$SRC/agents/"*.md "$DST/agents/" 2>/dev/null; then green "OK  agents/"; else yellow "--  keine agents/*.md"; fi
if find "$SRC/plugins" -mindepth 1 -maxdepth 1 -type f \( -name '*.js' -o -name '*.mjs' \) ! -name '*.test.mjs' -exec cp {} "$DST/plugins/" \; 2>/dev/null; then green "OK  plugins/ (inkl. Notifier-Vertrag, ohne Tests)"; else yellow "--  keine produktiven Plugins"; fi
# Nur plattformneutrale Plugin-Pakete: plugins/windows/ enthaelt ausschliesslich
# Windows-Plugins (terminal-task-title.js prueft selbst auf win32 und tut hier nichts) --
# sie haben unter macOS/Linux nichts im Plugin-Ordner zu suchen.
if find "$SRC/plugins" -mindepth 1 -maxdepth 1 -type d ! -name windows -exec cp -R {} "$DST/plugins/" \; 2>/dev/null; then green "OK  plugins/*/ (TUI-Plugin-Pakete, ohne windows/)"; else yellow "--  keine plugins/*/"; fi
rm -rf "$DST/plugins/windows" 2>/dev/null || true

# Entfernt die mit Plugin 1.1.0 ausgelieferten Theme-Duplikate. Seit 1.2.0 nutzt das Dropdown
# ausschliesslich die eingebauten OpenCode-Themes.
rm -f \
  "$DST/themes/frank-dark.json" \
  "$DST/themes/frank-light.json" \
  "$DST/plugins/token-cost-sidebar/themes/frank-dark.json" \
  "$DST/plugins/token-cost-sidebar/themes/frank-light.json"
rmdir "$DST/plugins/token-cost-sidebar/themes" 2>/dev/null || true

if command -v npm >/dev/null 2>&1; then
  notifier_version="0.2.8"
  installed_notifier_package="$DST/node_modules/@mohak34/opencode-notifier/package.json"
  if [ -f "$installed_notifier_package" ] && command -v node >/dev/null 2>&1; then
    detected_notifier_version="$(node -p "try { require(process.argv[1]).version } catch { '' }" "$installed_notifier_package" 2>/dev/null || true)"
    case "$detected_notifier_version" in
      [0-9]*.[0-9]*.[0-9]*) notifier_version="$detected_notifier_version" ;;
    esac
  fi
  if (cd "$DST" && npm install --silent --save-exact '@opencode-ai/plugin@1.17.15' '@opentui/core@0.4.3' '@opentui/solid@0.4.3' 'solid-js@1.9.12' "@mohak34/opencode-notifier@$notifier_version"); then
    green "OK  TUI-Plugin-Dependencies (npm)"
  else
    yellow "--  TUI-Plugin-Dependencies konnten nicht installiert werden"
  fi
else
  yellow "--  npm nicht gefunden -> TUI-Plugin-Dependencies manuell in ~/.config/opencode installieren"
fi
if cp -R "$SRC/skill/"* "$DST/skill/" 2>/dev/null; then green "OK  skill/ (OpenCode-Skills, z.B. session-opencode)"; else yellow "--  keine skill/*"; fi

# --- 4) Notifier-Sounds + Config (Pfade lokal erzeugen, NICHT die Windows-Pfade kopieren) ---
if cp "$SRC/sounds/"*.wav "$DST/sounds/" 2>/dev/null; then green "OK  sounds/"; else yellow "--  keine sounds/*.wav"; fi
backup "$DST/opencode-notifier.json"
cat > "$DST/opencode-notifier.json" <<EOF
{
  "sound": true,
  "notification": true,
  "showSessionTitle": true,
  "suppressWhenFocused": false,
  "minDuration": 0,
  "events": {
    "permission": false,
    "complete": true,
    "subagent_complete": false,
    "error": false,
    "question": true,
    "interrupted": false,
    "user_cancelled": false,
    "plan_exit": false,
    "session_started": false,
    "user_message": false,
    "client_connected": false
  },
  "messages": {
    "permission": "OpenCode benötigt eine Freigabe: {sessionTitle}",
    "complete": "Arbeit abgeschlossen: {sessionTitle}",
    "subagent_complete": "Unteraufgabe abgeschlossen: {sessionTitle}",
    "error": "Fehler in der Session: {sessionTitle}",
    "question": "OpenCode hat eine Frage: {sessionTitle}",
    "interrupted": "Session wurde unterbrochen: {sessionTitle}",
    "user_cancelled": "Session wurde abgebrochen: {sessionTitle}",
    "plan_exit": "Plan ist zur Prüfung bereit: {sessionTitle}"
  },
  "sounds": {
    "permission": "$DST/sounds/permission.wav",
    "complete": "$DST/sounds/complete.wav",
    "subagent_complete": "$DST/sounds/complete.wav",
    "error": "$DST/sounds/error.wav",
    "question": "$DST/sounds/permission.wav",
    "interrupted": "$DST/sounds/error.wav",
    "user_cancelled": "$DST/sounds/error.wav",
    "plan_exit": "$DST/sounds/permission.wav"
  }
}
EOF
green "OK  opencode-notifier.json (lokale Sound-Pfade)"

if command -v node >/dev/null 2>&1 && [ -f "$DST/plugins/notifier-auto-updater.mjs" ]; then
  if node "$DST/plugins/notifier-auto-updater.mjs" --config-dir "$DST" --force --verbose; then
    green "OK  Notifier taeglich geprueft und Vertragsregeln verifiziert"
  else
    yellow "--  Notifier-Update verworfen -> letzte funktionierende Version bleibt aktiv"
  fi
else
  yellow "--  Node oder Notifier-Updater fehlt -> taegliche Pruefung startet beim naechsten OpenCode-Start erneut"
fi

# --- 5) macOS-Stabilitaetsbuild (Gegenstueck zum Windows-Fix in install.ps1) ---
# Baut das gepatchte OpenCode-Binary: TuiModel-API (Effort-Waehler der Seitenleiste),
# Full-Repaint-Recovery, TUI-Fehler-Handler, Cache-Telemetrie. Der Launcher startet danach dieses
# Binary. Ueberspringbar mit OPENCODE_SKIP_MACFIX=1 (z.B. auf Testzielen ohne bun/git).
if [ "${OPENCODE_SKIP_MACFIX:-0}" != "1" ]; then
  if bash "$SRC/build-install-macos-tuifix.sh"; then
    green "OK  OpenCode-macOS-Fix (TuiModel-API, Full-Repaint, Fehler-Handler, Cache-Telemetrie)"
  else
    yellow "--  macOS-Stabilitaetsbuild fehlgeschlagen -> bisherige OpenCode-Fassung bleibt aktiv"
  fi
else
  yellow "--  macOS-Stabilitaetsbuild uebersprungen (OPENCODE_SKIP_MACFIX=1)"
fi

# --- 5) Voraussetzungs-Check (nur Hinweise; das Setup selbst ist fertig) ---
echo ""
echo "== Voraussetzungs-Check (was noch fehlt, manuell erledigen) =="
todo=0
check_file() {  # $1 = Pfad, $2 = Beschreibung
  if [ -s "$1" ]; then green "OK  $2"; else red "!!  FEHLT: $2 ($1)"; todo=$((todo + 1)); fi
}
check_file "$HOME/SK/OpenCode/firecrawl-api-key.txt" "Firecrawl-Key (Recherche Engine A)"
check_file "$HOME/SK/OpenCode/go-api-key.txt" "OpenCode-Go-Key (MiniMax-Recherche)"
check_file "$HOME/SK/ClaudeCodeOpenRouter/openrouter.key" "OpenRouter-Key (Engine B)"

if [ -n "${OPENROUTER_API_KEY:-}" ]; then
  green "OK  OPENROUTER_API_KEY gesetzt (Owl-Provider)"
else
  red "!!  OPENROUTER_API_KEY NICHT gesetzt -> Owl-Provider unbenutzbar. In die Shell-Rc aufnehmen."
  todo=$((todo + 1))
fi

# WireGuard / Second-Brain erreichbar? (Tunnel-Gateway 10.8.0.1)
if ping -c 1 -W 2 10.8.0.1 >/dev/null 2>&1; then
  green "OK  WireGuard-Tunnel aktiv (10.8.0.1 erreichbar) -> Second-Brain-MCP nutzbar"
else
  yellow "--  10.8.0.1 nicht erreichbar -> WireGuard-Tunnel starten, sonst kein Second-Brain (bugs/server/wireguard.md)"
  todo=$((todo + 1))
fi

echo ""
yellow "Noch manuell (interaktiv, nicht skriptbar):"
echo "  - opencode auth login   (bzw. /connect in der TUI) fuer das Go-Abo (opencode-go/MiniMax + Plan)"
echo "  - Beim ERSTEN Prompt muss OpenCode melden: 'N Regeln aus dem zweiten Gehirn eingelesen.'"
echo "  - @plannotator/opencode installiert OpenCode beim Start; der Notifier wurde lokal als Guard-Abhaengigkeit installiert."

echo ""
if [ "$todo" -eq 0 ]; then
  green "== Fertig. Alle Voraussetzungen erfuellt -> 'opencode' starten. =="
else
  yellow "== Dateien installiert. Noch $todo Voraussetzung(en) offen (siehe oben). =="
fi
