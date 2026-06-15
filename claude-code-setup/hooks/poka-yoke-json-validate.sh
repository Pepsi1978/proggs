#!/bin/bash
# Poka-Yoke: JSON-Validierung nach jedem Write/Edit auf JSON-Dateien
# Quelle: Superintelligenz Finding 8 — Fehler unmoeglich machen
# Event: PostToolUse (Edit|Write)
#
# FIXED: Shell-Injection via FILE_PATH beseitigt (Audit Finding #11)
# FIXED: Leerer stdin behandelt (Audit Finding #13)

set +e  # Nie crashen

INPUT=$(cat)
[[ -z "$INPUT" ]] && exit 0

# Sicher: Datei-Pfad via Python extrahieren, kein Shell-Interpolation
FILE_PATH=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_input',{}).get('file_path',''))" 2>/dev/null)

# Nur JSON-Dateien pruefen
[[ ! "$FILE_PATH" =~ \.json$ ]] && exit 0
[[ ! -f "$FILE_PATH" ]] && exit 0

# BOM-Schutz fuer Claude-Config-JSON (Poka-Yoke, Direktive #3): ein anderer PostToolUse-Hook
# kann nach einem Edit ein UTF-8-BOM (re-)einfuegen -> Claude Codes strikter JSON-Parser
# verwirft eine BOM-behaftete settings.json STILL (alle Hooks tot, claude-config.md 3.2).
# Root-Cause-unabhaengig automatisch entfernen.
case "$FILE_PATH" in
  */.claude/*|*/settings.json|*/settings.local.json|*/.mcp.json|*claude-code-setup/*)
    python3 -c "
import sys
p = sys.argv[1]
with open(p, 'rb') as f:
    b = f.read()
if b[:3] == b'\xef\xbb\xbf':
    with open(p, 'w', encoding='utf-8', newline='') as f:
        f.write(b[3:].decode('utf-8'))
    print('POKA-YOKE: UTF-8-BOM aus ' + p + ' entfernt (haette Claude Codes JSON-Parser still gebrochen).')
" "$FILE_PATH" 2>/dev/null
    ;;
esac

# Sicher: FILE_PATH als Argument, nie in Python-Code interpoliert
if python3 -c "import json,sys; json.load(open(sys.argv[1], encoding='utf-8'))" "$FILE_PATH" 2>/dev/null; then
    exit 0
else
    echo "POKA-YOKE WARNUNG: $(basename "$FILE_PATH") enthaelt KEIN valides JSON nach dem letzten Edit!"
    echo "Bitte die Datei sofort reparieren bevor weitergearbeitet wird."
    exit 0
fi

exit 0
