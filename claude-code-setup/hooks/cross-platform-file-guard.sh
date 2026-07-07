#!/usr/bin/env bash
# cross-platform-file-guard.sh — macOS-Gegenstueck zu cross-platform-file-guard.ps1
# Event: PostToolUse (Edit|Write auf .ps1/.sh Dateien in ~/.claude/hooks/)
# Zweck: Poka-Yoke Stufe 1 — Warnt wenn Cross-Platform-Gegenstueck fehlt
#
# WICHTIG: Standalone-Hook. MUSS mit exit 0 enden. Warnt nur, blockiert nie.

set +e

LOG_DIR="$HOME/.claude/logs/hooks"
LOG_FILE="$LOG_DIR/cross-platform-file-guard-$(date +%Y-%m-%d).log"
mkdir -p "$LOG_DIR" 2>/dev/null

log_msg() {
    echo "[$(date +%H:%M:%S)] $1" >> "$LOG_FILE" 2>/dev/null
}

# stdin lesen
INPUT=$(cat 2>/dev/null)
if [ -z "$INPUT" ] || [ ${#INPUT} -lt 5 ]; then
    exit 0
fi

# Bearbeitete Datei identifizieren
FILE_PATH=""
if command -v jq &>/dev/null; then
    FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // .tool_input.path // ""' 2>/dev/null)
else
    FILE_PATH=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); ti=d.get('tool_input',{}); print(ti.get('file_path',ti.get('path','')))" 2>/dev/null)
fi

if [ -z "$FILE_PATH" ]; then
    exit 0
fi

# Nur Hook-Dateien in ~/.claude/hooks/ pruefen
if [[ "$FILE_PATH" != *".claude/hooks/"* ]]; then
    exit 0
fi

HOOKS_DIR="$HOME/.claude/hooks"
WARNINGS=""

BASENAME=$(basename "$FILE_PATH")
NAME_NOEXT="${BASENAME%.*}"
EXT="${BASENAME##*.}"

if [ "$EXT" = "ps1" ]; then
    # Windows-Hook — gibt es .sh?
    SH_PATH="$HOOKS_DIR/$NAME_NOEXT.sh"
    if [ ! -f "$SH_PATH" ]; then
        WARNINGS="CROSS-PLATFORM: Du hast $NAME_NOEXT.ps1 geaendert, aber es gibt KEIN $NAME_NOEXT.sh Gegenstueck fuer macOS. Soll ich es erstellen?"
    else
        # Gegenstueck existiert — ist es aelter?
        PS1_MOD=$(stat -f %m "$HOOKS_DIR/$BASENAME" 2>/dev/null || stat -c %Y "$HOOKS_DIR/$BASENAME" 2>/dev/null)
        SH_MOD=$(stat -f %m "$SH_PATH" 2>/dev/null || stat -c %Y "$SH_PATH" 2>/dev/null)
        if [ -n "$PS1_MOD" ] && [ -n "$SH_MOD" ]; then
            DIFF=$((PS1_MOD - SH_MOD))
            if [ $DIFF -gt 300 ]; then  # 5 Minuten Toleranz
                WARNINGS="CROSS-PLATFORM: $NAME_NOEXT.ps1 wurde geaendert, aber $NAME_NOEXT.sh ist aelter. Bitte auch die .sh-Version aktualisieren."
            fi
        fi
    fi
elif [ "$EXT" = "sh" ]; then
    # macOS-Hook — gibt es .ps1?
    PS1_PATH="$HOOKS_DIR/$NAME_NOEXT.ps1"
    if [ ! -f "$PS1_PATH" ]; then
        WARNINGS="CROSS-PLATFORM: Du hast $NAME_NOEXT.sh geaendert, aber es gibt KEIN $NAME_NOEXT.ps1 Gegenstueck fuer Windows. Soll ich es erstellen?"
    else
        SH_MOD=$(stat -f %m "$HOOKS_DIR/$BASENAME" 2>/dev/null || stat -c %Y "$HOOKS_DIR/$BASENAME" 2>/dev/null)
        PS1_MOD=$(stat -f %m "$PS1_PATH" 2>/dev/null || stat -c %Y "$PS1_PATH" 2>/dev/null)
        if [ -n "$SH_MOD" ] && [ -n "$PS1_MOD" ]; then
            DIFF=$((SH_MOD - PS1_MOD))
            if [ $DIFF -gt 300 ]; then
                WARNINGS="CROSS-PLATFORM: $NAME_NOEXT.sh wurde geaendert, aber $NAME_NOEXT.ps1 ist aelter. Bitte auch die .ps1-Version aktualisieren."
            fi
        fi
    fi
fi

# Warnung ausgeben
if [ -n "$WARNINGS" ]; then
    log_msg "WARN: $WARNINGS"

    python3 -c "
import json, sys
warn = sys.argv[1]
out = {'hookSpecificOutput': {'hookEventName': 'PostToolUse', 'additionalContext': warn}}
print(json.dumps(out))
" "$WARNINGS" 2>/dev/null
fi

exit 0
