#!/bin/bash
# Immunologisches Gedaechtnis: Bash-Befehle gegen bekannte Fehlermuster pruefen
# Quelle: Superintelligenz Finding 10 — Error-Antigen-System
# Event: PreToolUse (Bash)
# Zweck: Prueft ob ein Bash-Befehl ein bekanntes Fehlermuster enthaelt

set +e  # Nie crashen

# Input lesen
INPUT=$(cat)

# Befehl extrahieren
COMMAND=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_input',{}).get('command',''))" 2>/dev/null)

if [[ -z "$COMMAND" ]]; then
    exit 0
fi

# Antigen-Datei
ANTIGEN_FILE="$HOME/proggs/.claude/agent-memory/shared/error-antigens.jsonl"
if [[ ! -f "$ANTIGEN_FILE" ]]; then
    exit 0
fi

# Jede Zeile der Antigen-Datei pruefen
while IFS= read -r line; do
    [[ -z "$line" ]] && continue

    PATTERN=$(echo "$line" | python3 -c "import sys,json; print(json.load(sys.stdin).get('pattern',''))" 2>/dev/null)
    [[ -z "$PATTERN" ]] && continue

    if echo "$COMMAND" | python3 -c "import sys,re; cmd=sys.stdin.read(); exit(0 if re.search('$PATTERN', cmd) else 1)" 2>/dev/null; then
        ID=$(echo "$line" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))" 2>/dev/null)
        SEV=$(echo "$line" | python3 -c "import sys,json; print(json.load(sys.stdin).get('severity',''))" 2>/dev/null)
        CM=$(echo "$line" | python3 -c "import sys,json; print(json.load(sys.stdin).get('countermeasure',''))" 2>/dev/null)
        echo "IMMUN-CHECK [$ID] ($SEV): $CM"
    fi
done < "$ANTIGEN_FILE"

# Nie blockieren — nur warnen
exit 0
