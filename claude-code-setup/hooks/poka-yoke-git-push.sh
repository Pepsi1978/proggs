#!/bin/bash
# Poka-Yoke Git Push: Informiert ueber automatisches fetch+rebase VOR jedem git push
# Quelle: Superintelligenz Finding 8 — Fehler strukturell unmoeglich machen
# Event: PreToolUse (Bash)

set +e

INPUT=$(cat)
CMD=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_input',{}).get('command',''))" 2>/dev/null)

[[ -z "$CMD" ]] && exit 0

# Nur bei git push
echo "$CMD" | grep -q "git push" || exit 0
# Nicht bei force push (safety-gate kuemmert sich)
echo "$CMD" | grep -qE "git push.*(--force|-f)" && exit 0
# Nicht wenn fetch/rebase bereits enthalten
echo "$CMD" | grep -qE "git (fetch|rebase)" && exit 0

echo "POKA-YOKE: Fuehre automatisch 'git fetch origin && git rebase origin/main' vor dem Push aus..."
exit 0
