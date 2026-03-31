#!/bin/bash
# Immunologisches Gedaechtnis: Bash-Befehle gegen bekannte Fehlermuster pruefen
# Quelle: Superintelligenz Finding 10 — Error-Antigen-System
# Event: PreToolUse (Bash)
#
# FIXED: Shell-Injection via Pattern beseitigt (Audit Finding #18)
# FIXED: 45 Python-Prozesse → 1 einziger (Audit Finding #24)
# FIXED: Leerer stdin behandelt (Audit Finding #13)
# FIXED: IMMUNE_CMD env var Bug — Befehl jetzt via stdin uebergeben (Audit Re-Test)

set +e  # Nie crashen

INPUT=$(cat)
[[ -z "$INPUT" ]] && exit 0

COMMAND=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_input',{}).get('command',''))" 2>/dev/null)
[[ -z "$COMMAND" ]] && exit 0

ANTIGEN_FILE="$HOME/proggs/.claude/agent-memory/shared/error-antigens.jsonl"
[[ ! -f "$ANTIGEN_FILE" ]] && exit 0

# Ein einziger Python-Aufruf: Befehl UND Antigen-Pfad als Argumente, kein Shell-Interpolation
python3 - "$COMMAND" "$ANTIGEN_FILE" << 'PYEOF'
import json, re, sys

cmd = sys.argv[1] if len(sys.argv) > 1 else ''
antigen_file = sys.argv[2] if len(sys.argv) > 2 else ''
if not cmd or not antigen_file:
    sys.exit(0)

try:
    with open(antigen_file, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                obj = json.loads(line)
                pattern = obj.get('pattern', '')
                if pattern and re.search(pattern, cmd):
                    aid = obj.get('id', '?')
                    sev = obj.get('severity', '?')
                    cm = obj.get('countermeasure', '?')
                    print(f'IMMUN-CHECK [{aid}] ({sev}): {cm}')
            except (json.JSONDecodeError, re.error):
                continue
except FileNotFoundError:
    pass
PYEOF

exit 0
