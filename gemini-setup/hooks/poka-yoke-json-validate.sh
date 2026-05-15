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

# Sicher: FILE_PATH als Argument, nie in Python-Code interpoliert
if python3 -c "import json,sys; json.load(open(sys.argv[1], encoding='utf-8'))" -- "$FILE_PATH" 2>/dev/null; then
    exit 0
else
    echo "POKA-YOKE WARNUNG: $(basename "$FILE_PATH") enthaelt KEIN valides JSON nach dem letzten Edit!"
    echo "Bitte die Datei sofort reparieren bevor weitergearbeitet wird."
    exit 0
fi
