#!/usr/bin/env bash
# config-guard-preemptive.sh — macOS-Gegenstueck zu config-guard-preemptive.ps1
# Event: ConfigChange (blockierbar mit exit 2)
# Zweck: Poka-Yoke Stufe 3 — Blockiert Config-Aenderungen BEVOR sie wirksam werden
#
# Defense in Depth:
#   Schicht 1 (praeventiv): Dieser Hook blockiert VOR dem Speichern
#   Schicht 2 (reaktiv): config-guard.sh repariert NACH dem Speichern
#
# WICHTIG: Standalone-Hook. exit 0 am Ende (ausser bei Block → exit 2).

set +e

LOG_DIR="$HOME/.claude/logs/hooks"
LOG_FILE="$LOG_DIR/config-guard-preemptive-$(date +%Y-%m-%d).log"
mkdir -p "$LOG_DIR" 2>/dev/null

log_msg() {
    echo "[$(date +%H:%M:%S)] $1" >> "$LOG_FILE" 2>/dev/null
}

# stdin lesen
INPUT=$(cat 2>/dev/null)
if [ -z "$INPUT" ] || [ ${#INPUT} -lt 5 ]; then
    exit 0
fi

# Geschuetzte Einstellungen pruefen
VIOLATIONS=""

check_settings_file() {
    local SETTINGS_PATH="$1"
    local FILENAME=$(basename "$SETTINGS_PATH")

    if [ ! -f "$SETTINGS_PATH" ]; then
        return
    fi

    # Python fuer zuverlaessiges JSON-Parsing
    local RESULT=$(python3 -c "
import json, sys, os

violations = []
path = sys.argv[1]
filename = sys.argv[2]

try:
    with open(path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    perms = data.get('permissions', {})
    mode = perms.get('defaultMode', '')
    if mode and mode != 'bypassPermissions':
        violations.append(f\"defaultMode='{mode}' in {filename} (MUSS 'bypassPermissions' sein)\")

    if 'allow' in perms:
        violations.append(f'allow-Liste in {filename} erkannt (blockiert bypassPermissions)')

    env = data.get('env', {})
    acp = env.get('CLAUDE_AUTOCOMPACT_PCT_OVERRIDE', '')
    if acp and int(acp) < 95:
        violations.append(f'AUTOCOMPACT={acp} (Minimum: 95)')

    sub = env.get('CLAUDE_CODE_SUBAGENT_MODEL', '')
    if sub and sub != 'sonnet':
        violations.append(f\"SUBAGENT_MODEL='{sub}' (erwartet: sonnet)\")

except Exception as e:
    pass

if violations:
    print('|'.join(violations))
else:
    print('')
" "$SETTINGS_PATH" "$FILENAME" 2>/dev/null)

    if [ -n "$RESULT" ]; then
        if [ -n "$VIOLATIONS" ]; then
            VIOLATIONS="$VIOLATIONS; $RESULT"
        else
            VIOLATIONS="$RESULT"
        fi
    fi
}

check_settings_file "$HOME/.claude/settings.json"
check_settings_file "$HOME/.claude/settings.local.json"

# Entscheidung
if [ -n "$VIOLATIONS" ]; then
    log_msg "BLOCKED: $VIOLATIONS"

    python3 -c "
import json, sys
msg = sys.argv[1]
out = {'error': f'CONFIG-GUARD PRAEVENTIV: BLOCKIERT — {msg}. Diese Aenderung wurde verhindert BEVOR sie wirksam wurde.'}
print(json.dumps(out))
" "$VIOLATIONS" 2>/dev/null

    exit 2
fi

exit 0
