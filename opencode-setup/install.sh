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
if cp "$SRC/plugins/"*.js "$DST/plugins/" 2>/dev/null; then green "OK  plugins/ (inkl. tool-first-guard)"; else yellow "--  keine plugins/*.js"; fi
if find "$SRC/plugins" -mindepth 1 -maxdepth 1 -type d -exec cp -R {} "$DST/plugins/" \; 2>/dev/null; then green "OK  plugins/*/ (TUI-Plugin-Pakete)"; else yellow "--  keine plugins/*/"; fi

# Entfernt die mit Plugin 1.1.0 ausgelieferten Theme-Duplikate. Seit 1.2.0 nutzt das Dropdown
# ausschliesslich die eingebauten OpenCode-Themes.
rm -f \
  "$DST/themes/frank-dark.json" \
  "$DST/themes/frank-light.json" \
  "$DST/plugins/token-cost-sidebar/themes/frank-dark.json" \
  "$DST/plugins/token-cost-sidebar/themes/frank-light.json"
rmdir "$DST/plugins/token-cost-sidebar/themes" 2>/dev/null || true

if command -v npm >/dev/null 2>&1; then
  if (cd "$DST" && npm install --silent '@opencode-ai/plugin@1.17.7' '@opentui/core@0.3.4' '@opentui/solid@0.4.0' 'solid-js@1.9.12'); then
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
  "sounds": {
    "permission": "$DST/sounds/permission.wav",
    "complete": "$DST/sounds/complete.wav",
    "error": "$DST/sounds/error.wav"
  }
}
EOF
green "OK  opencode-notifier.json (lokale Sound-Pfade)"

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
echo "  - npm-Plugins (@mohak34/opencode-notifier, @plannotator/opencode) installiert OpenCode selbst beim Start."

echo ""
if [ "$todo" -eq 0 ]; then
  green "== Fertig. Alle Voraussetzungen erfuellt -> 'opencode' starten. =="
else
  yellow "== Dateien installiert. Noch $todo Voraussetzung(en) offen (siehe oben). =="
fi
