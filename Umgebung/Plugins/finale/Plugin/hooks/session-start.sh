#!/usr/bin/env bash
# session-start.sh
#
# SessionStart-Hook des finale-Plugins. Prueft NICHT-BLOCKIEREND ob die vier
# Skill-Symlinks aufloesbar sind und gibt bei Defekt konkrete Reparatur-Befehle
# nach stderr aus (Post-Install-Wizard). Diese Diagnose erscheint im Session-Start-
# Bereich von Claude Code als Info-Block.
#
# Verbesserungen vom 2026-05-21 (Direktive #3):
# - set -eu statt nur set -u  (set -e fuer fail-fast, set -u fuer unbound-var-Schutz)
# - Logikfehler &&  ->  || behoben (vorher: Warnung nur wenn weder lesbar noch ausfuehrbar;
#   korrekt: Warnung wenn nicht lesbar)
# - Post-Install-Wizard: Bei Defekt JSON parsen und konkrete `ln -sfn`-Befehle
#   mit aufgeloesten Pfaden ausgeben — der Nutzer kann sie direkt copy-pasten
# - JSON-Output von verify-skills.sh wird in TMP gespeichert (vorher verworfen)
#
# Verhalten:
# - Tote Symlinks -> Warnung + konkrete Reparatur-Befehle, aber Exit 0 (nicht blockieren)
# - Alle OK -> kurze Erfolgsmeldung
# - Skript-Fehler -> stille Schluck-Logik, niemals die Session blockieren

set -eu

PLUGIN_ROOT="${CLAUDE_PLUGIN_ROOT:-}"

# Input-Guard: kein Plugin-Root gesetzt -> still durchwinken
if [ -z "$PLUGIN_ROOT" ] || [ ! -d "$PLUGIN_ROOT" ]; then
  exit 0
fi

VERIFY="$PLUGIN_ROOT/scripts/verify-skills.sh"

# Korrigierte Logik: || statt &&. Vorher: Warnung nur wenn weder lesbar noch ausfuehrbar
# (also Datei ist gar nicht da). Aber `bash script.sh` braucht nur Read-Berechtigung —
# x-Bit ist nicht noetig. Korrekte Pruefung: Datei muss EXISTIEREN und LESBAR sein.
if [ ! -f "$VERIFY" ] || [ ! -r "$VERIFY" ]; then
  echo "[finale] Hinweis: verify-skills.sh fehlt oder nicht lesbar — Plugin koennte beschaedigt sein." >&2
  exit 0
fi

# Temp-Dateien fuer JSON-Output und Stderr — beide werden am Ende geloescht
TMP_DIR="${TMPDIR:-/tmp}"
JSON_FILE="$TMP_DIR/.finale-verify-$$.json"
ERR_FILE="$TMP_DIR/.finale-verify-$$.err"

# Cleanup-Trap: Temp-Dateien werden IMMER geloescht, auch bei Skript-Fehler
trap 'rm -f "$JSON_FILE" "$ERR_FILE" 2>/dev/null || true' EXIT

# verify-skills.sh ausfuehren. Stdout (JSON) wird gefangen, Stderr separat.
# Der `if`-Block faengt den Exit-Code ab (set -e wird durch den `if`-Kontext deaktiviert).
if bash "$VERIFY" >"$JSON_FILE" 2>"$ERR_FILE"; then
  echo "[finale] Alle vier Skill-Symlinks OK." >&2
  exit 0
fi

# Skill-Verifikation fehlgeschlagen — Post-Install-Wizard zeigen
echo "[finale] WARNUNG: mindestens ein Skill-Symlink ist defekt." >&2

# JSON parsen und konkrete Reparatur-Befehle ausgeben.
# Verwendet expanduser fuer plattform-neutrale Pfad-Aufloesung.
python3 - "$JSON_FILE" "$PLUGIN_ROOT" <<'PY' >&2 2>/dev/null || true
import json, os, sys

json_path, plugin_root = sys.argv[1], sys.argv[2]
try:
    with open(json_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
except Exception:
    sys.exit(0)

home = os.path.expanduser('~')
broken_skills = []

for skill in data.get('skills', []):
    if skill.get('status') == 'error':
        broken_skills.append({
            'name': skill.get('name', '?'),
            'problem': skill.get('problem', '?'),
            'link': skill.get('linkPath', '?'),
            'target': skill.get('expectedTarget', '?'),
        })

if not broken_skills:
    sys.exit(0)

print('[finale] Reparatur-Befehle (direkt copy-pasten):', flush=True)
print('[finale]', flush=True)

# Plattform-spezifischer Hinweis fuer Git Bash auf Windows
ostype = os.environ.get('OSTYPE', '').lower()
msystem = os.environ.get('MSYSTEM', '')
if 'msys' in ostype or 'cygwin' in ostype or msystem:
    print('[finale]   # Wichtig auf Windows Git Bash: vorher Symlink-Modus setzen', flush=True)
    print('[finale]   export MSYS=winsymlinks:nativestrict', flush=True)
    print('[finale]', flush=True)

for s in broken_skills:
    # Pfade fuer Anzeige minimal normalisieren
    print(f'[finale]   # {s["name"]}: {s["problem"]}', flush=True)
    print(f'[finale]   ln -sfn "{s["target"]}" "{s["link"]}"', flush=True)

print('[finale]', flush=True)
print('[finale] Danach erneut bash scripts/verify-skills.sh ausfuehren — Erwartung: {"ok": true}.', flush=True)
print('[finale] Voraussetzung: die echten Skills muessen unter ~/.claude/skills/ liegen.', flush=True)
PY

# Fallback: Stderr aus verify-skills.sh anzeigen falls Python-Parser fehlschlug
if [ -s "$ERR_FILE" ]; then
  echo "[finale] Details aus verify-skills.sh:" >&2
  sed -n '1,10p' "$ERR_FILE" >&2 2>/dev/null || true
fi

exit 0
