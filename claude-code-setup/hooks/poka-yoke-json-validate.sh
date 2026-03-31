#!/bin/bash
# Poka-Yoke: JSON-Validierung nach jedem Write/Edit auf JSON-Dateien
# Quelle: Superintelligenz Finding 8 — Fehler unmoeglich machen
# Event: PostToolUse (Edit|Write)
# Zweck: Prueft ob geschriebene JSON-Dateien valide sind

set +e  # Nie crashen

# Input lesen
INPUT=$(cat)

# Datei-Pfad extrahieren
FILE_PATH=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_input',{}).get('file_path',''))" 2>/dev/null)

# Nur JSON-Dateien pruefen
if [[ ! "$FILE_PATH" =~ \.json$ ]]; then
    exit 0
fi

# Datei existiert?
if [[ ! -f "$FILE_PATH" ]]; then
    exit 0
fi

# JSON validieren
if python3 -c "import json; json.load(open('$FILE_PATH', encoding='utf-8'))" 2>/dev/null; then
    exit 0
else
    echo "POKA-YOKE WARNUNG: $FILE_PATH enthaelt KEIN valides JSON nach dem letzten Edit!"
    echo "Bitte die Datei sofort reparieren bevor weitergearbeitet wird."
    exit 0  # Nicht blockieren, nur warnen
fi
