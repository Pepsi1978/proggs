#!/usr/bin/env bash
# antigen-matcher.sh — PreToolUse Hook: Warns before risky tool calls (L1, bash version)
#
# Immunsystem-Muster: Bekannte Fehler aus bug-cases.jsonl als "Antigen-Fingerprints".
# PreToolUse matcht geplanten Call gegen bekannte Fehlermuster.
# Bei Match: Warnung auf stderr (non-blocking).
#
# Direktive #3 Conformance: Graceful Degradation, Non-blocking, Timeout-Schutz.
#
# Hinweis zur Cross-Platform-Paritaet (Audit 2026-04-20):
# Das PowerShell-Gegenstueck hatte einen Bug wegen der Automatic-Variable $input.
# Bash hat keine solche Automatic-Variable — wir parsen hier in Python und nennen
# die Variable 'data'. Kein Fix noetig, nur dokumentiert.

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
    # NotebookEdit absichtlich NICHT: dessen Input-Shape passt nicht zum Haystack-Bau
    Bash|Edit|Write) ;;
    *) exit 0 ;;
esac

bug_cases="$HOME/proggs/.Gemini/agent-memory/shared/bug-cases.jsonl"
if [ ! -f "$bug_cases" ]; then exit 0; fi

# Run the match in Python (faster + safer parsing), with 2s timeout.
# Python writes warnings directly to stderr; we don't capture output.
#
# WICHTIG: <<'PYEOF' (quoted heredoc) — KEINE Shell-Expansion im Python-Code.
# Daten kommen via Env-Variablen (sicher gegen Quoting-Angriffe) und via stdin.
# Bug aus Loop 2 Audit 2026-04-20 gefixt: Vorher unquoted heredoc mit
# '''$stdin_input''' interpolation -- brach bei Backslashes/Newlines im JSON.
export STDIN_INPUT="$stdin_input"
export BUG_CASES_PATH="$bug_cases"
# WICHTIG: Kein '2>/dev/null' — das wuerde unsere deliberaten stderr-Warnungen
# unterdruecken. Python-Code ist durch try/except abgesichert, sollte nie
# selbst crashen. Timeout-Kill-Meldungen sind akzeptabler Noise.
# Bug aus Loop 2 Audit 2026-04-20 gefixt.
timeout 2 python3 <<'PYEOF' || true
import os, json, sys, re

stdin_input = os.environ.get('STDIN_INPUT', '')
bug_cases_path = os.environ.get('BUG_CASES_PATH', '')

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
    with open(bug_cases_path, 'r', encoding='utf-8') as f:
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

exit 0

