#!/usr/bin/env bash
# antigen-matcher.sh — PreToolUse Hook: Warns before risky tool calls (L1, bash version)
#
# Immunsystem-Muster: Bekannte Fehler aus bug-cases.jsonl als "Antigen-Fingerprints".
# PreToolUse matcht geplanten Call gegen bekannte Fehlermuster.
# Bei Match: Warnung auf stderr (non-blocking).
#
# Direktive #3 Conformance: Graceful Degradation, Non-blocking, Timeout-Schutz.

# Failsafe: Bei jedem Fehler still beenden
set +e
trap 'exit 0' ERR

# Read input
stdin_input=$(cat 2>/dev/null || echo "")
if [ -z "$stdin_input" ]; then exit 0; fi

# Only for risky tools
tool_name=$(echo "$stdin_input" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    print(d.get('tool_name',''))
except:
    print('')
" 2>/dev/null)

case "$tool_name" in
    Bash|Edit|Write|NotebookEdit) ;;
    *) exit 0 ;;
esac

bug_cases="$HOME/proggs/.claude/agent-memory/shared/bug-cases.jsonl"
if [ ! -f "$bug_cases" ]; then exit 0; fi

# Run the match in Python (faster + safer parsing), with 2s timeout
result=$(timeout 2 python3 <<PYEOF 2>/dev/null
import json, sys, re

stdin_input = '''$stdin_input'''
try:
    data = json.loads(stdin_input)
except Exception:
    sys.exit(0)

tool_name = data.get('tool_name', '')
tool_input = data.get('tool_input', {})

if tool_name == 'Bash':
    haystack = str(tool_input.get('command', ''))
elif tool_name in ('Edit', 'Write'):
    haystack = ' '.join(str(tool_input.get(k, '')) for k in ('file_path', 'new_string', 'content'))
else:
    sys.exit(0)

if not haystack.strip():
    sys.exit(0)

matches = []
try:
    with open('$bug_cases', 'r', encoding='utf-8') as f:
        bugs = [json.loads(l) for l in list(f)[:100] if l.strip()]
except Exception:
    sys.exit(0)

for bug in bugs:
    hits = 0
    total = 0
    for tag in bug.get('tags', []):
        total += 2
        if re.search(re.escape(tag), haystack, re.IGNORECASE):
            hits += 2
    symptom = bug.get('symptom', '')
    words = [w for w in symptom.split() if len(w) > 4 and re.match(r'^[A-Za-z_-]+$', w)][:3]
    for w in words:
        total += 1
        if re.search(re.escape(w), haystack, re.IGNORECASE):
            hits += 1
    if total >= 3 and (hits / total) >= 0.6:
        matches.append({
            'symptom': symptom,
            'fix': bug.get('fix', ''),
            'severity': bug.get('severity', '?'),
            'score': round((hits/total)*100)
        })

if matches:
    top = max(matches, key=lambda m: m['score'])
    print(f"[antigen-matcher] Moeglicher Treffer ({top['score']}% Match, Severity: {top['severity']}): {top['symptom']}", file=sys.stderr)
    print(f"  Bekannter Fix: {top['fix']}", file=sys.stderr)
PYEOF
)

exit 0
