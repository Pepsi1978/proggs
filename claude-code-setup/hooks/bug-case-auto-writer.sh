#!/usr/bin/env bash
# bug-case-auto-writer.sh — macOS-Gegenstueck zu bug-case-auto-writer.ps1
# Event: PostToolUseFailure
# Zweck: Automatischer Bug-Cases-Writer + Pattern-Matcher (Luecke 2 + 6)
#
# Was dieses Skript tut:
# 1. Liest die Fehlermeldung aus dem PostToolUseFailure-Event
# 2. Prueft ob ein aehnlicher Fall in bug-cases.jsonl existiert (Pattern-Match)
# 3. Wenn JA: Gibt den alten Fix als Kontext an Claude zurueck (RAG-Effekt)
# 4. Wenn NEIN: Schreibt einen neuen Roh-Eintrag in bug-cases.jsonl
#
# Poka-Yoke-Stufe: 3 (Eliminierung) — Vergessen ist strukturell unmoeglich
# WICHTIG: Standalone-Hook. MUSS mit exit 0 enden.

set +e  # Nie abbrechen bei Fehlern

# --- Pfade ---
BUG_CASES="$HOME/proggs/.claude/agent-memory/shared/bug-cases.jsonl"
LOG_DIR="$HOME/.claude/logs/hooks"
LOG_FILE="$LOG_DIR/bug-case-auto-writer-$(date +%Y-%m-%d).log"

mkdir -p "$LOG_DIR" 2>/dev/null

log_msg() {
    echo "[$(date +%H:%M:%S)] $1" >> "$LOG_FILE" 2>/dev/null
}

# --- stdin lesen ---
INPUT=$(cat 2>/dev/null)
if [ -z "$INPUT" ] || [ ${#INPUT} -lt 5 ]; then
    exit 0
fi

# --- Fehlerdaten extrahieren (jq oder Python) ---
if command -v jq &>/dev/null; then
    TOOL_NAME=$(echo "$INPUT" | jq -r '.tool_name // ""' 2>/dev/null)
    ERROR_TEXT=$(echo "$INPUT" | jq -r '.error // ""' 2>/dev/null)
    COMMAND_TEXT=$(echo "$INPUT" | jq -r '.tool_input.command // .tool_input.file_path // ""' 2>/dev/null)
else
    # Fallback: Python
    TOOL_NAME=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('tool_name',''))" 2>/dev/null)
    ERROR_TEXT=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('error',''))" 2>/dev/null)
    COMMAND_TEXT=$(echo "$INPUT" | python3 -c "import sys,json; d=json.load(sys.stdin); ti=d.get('tool_input',{}); print(ti.get('command',ti.get('file_path','')))" 2>/dev/null)
fi

# Triviale Fehler ignorieren (unter 20 Zeichen)
if [ ${#ERROR_TEXT} -lt 20 ]; then
    log_msg "SKIP: Fehlermeldung zu kurz (${#ERROR_TEXT} Zeichen)"
    exit 0
fi

# --- LUECKE 6: Pattern-Matching gegen bestehende Bug-Cases ---
MATCHED_CASE=""
MATCHED_SCORE=0
MATCHED_SYMPTOM=""
MATCHED_ROOT=""
MATCHED_FIX=""
MATCHED_PREVENTION=""

if [ -f "$BUG_CASES" ]; then
    # Python fuer zuverlaessiges JSON-Parsing und Wort-Matching
    MATCH_RESULT=$(python3 -c "
import json, sys, re, os

error_text = sys.argv[1]
bug_path = sys.argv[2]

best_score = 0
best_case = None

try:
    with open(bug_path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                case = json.loads(line)
            except:
                continue

            symptom = case.get('symptom', '')
            if not symptom:
                continue

            # Symptom-Pattern-Match
            words = [w for w in symptom.split() if len(w) > 3]
            if not words:
                continue
            match_count = sum(1 for w in words if w.lower() in error_text.lower())
            score = round(match_count / len(words), 2)

            if score > best_score:
                best_score = score
                best_case = case

            # Exaktes symptom_pattern Match
            sp = case.get('symptom_pattern', '')
            if sp:
                try:
                    if re.search(sp, error_text):
                        best_score = 1.0
                        best_case = case
                        break
                except:
                    pass

    if best_case and best_score >= 0.5:
        print(f'MATCH|{best_score}|{best_case.get(\"symptom\",\"\")}|{best_case.get(\"root_cause\",\"\")}|{best_case.get(\"fix\",\"\")}|{best_case.get(\"prevention\",best_case.get(\"prevention_rule\",\"\"))}')
    else:
        print('NOMATCH')
except Exception as e:
    print('NOMATCH')
" "$ERROR_TEXT" "$BUG_CASES" 2>/dev/null)

    if [[ "$MATCH_RESULT" == MATCH* ]]; then
        IFS='|' read -r _ MATCHED_SCORE MATCHED_SYMPTOM MATCHED_ROOT MATCHED_FIX MATCHED_PREVENTION <<< "$MATCH_RESULT"
    fi
fi

# --- Entscheidung ---
if [ -n "$MATCHED_SYMPTOM" ] && [ "$MATCHED_SCORE" != "0" ]; then
    # Bekannten Fix zurueckgeben
    SCORE_PCT=$(python3 -c "print(int(float('$MATCHED_SCORE') * 100))" 2>/dev/null || echo "50")

    FIX_ADVICE="BEKANNTER FEHLER (Aehnlichkeit: ${SCORE_PCT}%)\nSymptom: $MATCHED_SYMPTOM\nRoot Cause: $MATCHED_ROOT\nFix: $MATCHED_FIX"
    if [ -n "$MATCHED_PREVENTION" ]; then
        FIX_ADVICE="$FIX_ADVICE\nPraevention: $MATCHED_PREVENTION"
    fi
    FIX_ADVICE="$FIX_ADVICE\nWENDE DIESEN FIX ZUERST AN bevor du eine neue Hypothese versuchst."

    # JSON-Output fuer Claude
    python3 -c "
import json, sys
advice = sys.argv[1]
out = {'hookSpecificOutput': {'hookEventName': 'PostToolUseFailure', 'additionalContext': advice}}
print(json.dumps(out))
" "$FIX_ADVICE" 2>/dev/null

    log_msg "MATCH: Score=$MATCHED_SCORE, Symptom='$MATCHED_SYMPTOM', Tool=$TOOL_NAME"

else
    # Neuen Eintrag schreiben
    # Duplikat-Check: Gleicher Fehler in letzten 10 Zeilen?
    SHORT_ERROR="${ERROR_TEXT:0:80}"
    IS_DUPLICATE=false

    if [ -f "$BUG_CASES" ]; then
        if tail -10 "$BUG_CASES" | grep -qF "$SHORT_ERROR" 2>/dev/null; then
            IS_DUPLICATE=true
        fi
    fi

    if [ "$IS_DUPLICATE" = false ]; then
        # Neuen Eintrag erstellen
        SYMPTOM_SHORT="${ERROR_TEXT:0:200}"
        TODAY=$(date +%Y-%m-%d)
        ERROR_HASH=$(echo "${ERROR_TEXT:0:100}" | md5sum 2>/dev/null | cut -d' ' -f1 || echo "nohash")

        python3 -c "
import json, sys, os
entry = {
    'date': sys.argv[1],
    'symptom': sys.argv[2],
    'root_cause': 'AUTO-ERFASST — Root Cause noch nicht analysiert',
    'fix': 'AUTO-ERFASST — Fix noch nicht dokumentiert',
    'files': [sys.argv[3]],
    'tags': [sys.argv[4].lower(), 'auto-captured'],
    'severity': 'unbekannt',
    'error_hash': sys.argv[5],
    'auto_captured': True
}
path = sys.argv[6]
with open(path, 'a', encoding='utf-8') as f:
    f.write(json.dumps(entry, ensure_ascii=False) + '\n')
" "$TODAY" "$SYMPTOM_SHORT" "$COMMAND_TEXT" "$TOOL_NAME" "$ERROR_HASH" "$BUG_CASES" 2>/dev/null

        log_msg "WRITE: Neuer Bug-Case auto-erfasst, Tool=$TOOL_NAME, Hash=$ERROR_HASH"

        # Claude informieren
        python3 -c "
import json
out = {'hookSpecificOutput': {'hookEventName': 'PostToolUseFailure', 'additionalContext': 'Bug-Case automatisch erfasst (Tool: $TOOL_NAME). Nach dem Fix: Bitte Root Cause und Fix in bug-cases.jsonl nachtragen (Feld auto_captured: true suchen).'}}
print(json.dumps(out))
" 2>/dev/null
    else
        log_msg "SKIP: Duplikat erkannt"
    fi
fi

exit 0
