#!/usr/bin/env bash
# research-approval: Blockt Web-Recherche-Aufrufe bis eine Freigabe-Flag-Datei existiert.
# Setzt research-strategy.md durch (Frage 1 A/B/C/D muss Frank beantworten, bevor mm/or-research
# oder die Firecrawl-MCP laufen). Verbraucht sonst still Firecrawl-Credits / teure Tokens.
# Runs as PreToolUse hook (matcher: Bash | mcp__.*firecrawl.*)
# stdout -> AI context (nur DENY-JSON), stderr -> user terminal. Platform: macOS/Linux

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
. "$SCRIPT_DIR/hook-log.sh"
# FAIL-OPEN: jeder unerwartete Fehler -> exit 0 (Guard blockiert die Session nie versehentlich)
trap 'hook_log_warn "research-approval: error at line $LINENO"; exit 0' ERR

raw="$(cat || true)"
if [ -z "$raw" ]; then exit 0; fi   # kein Input -> normaler Flow (FAIL-OPEN)

raw_lower="$(printf '%s' "$raw" | tr '[:upper:]' '[:lower:]')"

# --- Research-Erkennung: NUR echte AUSFUEHRUNG, nicht blosse Erwaehnung ---
# (sonst wuerden git add / grep / cat / py_compile auf den Dateinamen faelschlich blockiert).
# KEIN jq (umgeht §16.2-Control-Char-Bypass). POSIX-ERE ohne \b (BSD-libc-portabel).
is_research=0
if [[ "$raw_lower" =~ python[0-9.]*[[:space:]]+(-[a-z][^[:space:]]*[[:space:]]+)*[^[:space:]]*(mm|or)-research\.py ]]; then
  is_research=1   # python ... <pfad>mm/or-research.py  (NICHT 'python -m py_compile datei.py')
elif [[ "$raw_lower" =~ (^|[[:space:]])(bash|sh)[[:space:]]+[^[:space:]]*(mm|or)-research\.sh ]] \
  || [[ "$raw_lower" =~ (^|[[:space:]])\./[^[:space:]]*(mm|or)-research\.sh ]]; then
  is_research=1   # bash/sh/./ ... mm/or-research.sh
fi
if [ "$is_research" -eq 0 ]; then
  # Firecrawl-MCP-Tool? tool_name via python3 (NIE jq, §16.2) + raw-Fallback
  tn="$(printf '%s' "$raw" | python3 -c 'import sys,json
try:
    print((json.loads(sys.stdin.read()).get("tool_name") or "").lower())
except Exception:
    print("")' 2>/dev/null || true)"
  case "$tn" in *firecrawl*) is_research=1 ;; esac
  if [ "$is_research" -eq 0 ]; then
    case "$raw_lower" in *mcp__*firecrawl*) is_research=1 ;; esac
  fi
fi

if [ "$is_research" -eq 0 ]; then exit 0; fi   # kein Research-Aufruf -> normaler Flow

# --- Freigabe-Flag pruefen (Existenz + TTL 30 Min) ---
tmp="${TMPDIR:-/tmp}"
flag="${tmp%/}/research-approved.flag"
ttl=1800
if [ -f "$flag" ]; then
  now=$(date +%s)
  mt=$(date -r "$flag" +%s 2>/dev/null || stat -c %Y "$flag" 2>/dev/null || echo 0)
  age=$(( now - mt ))
  if [ "$age" -ge 0 ] && [ "$age" -lt "$ttl" ]; then exit 0; fi   # Freigabe aktiv -> normaler Flow
fi

# --- Keine Freigabe -> DENY (spec-konform JSON via python3, exit 0; §16.1, §1.6) ---
reason="RESEARCH-FREIGABE FEHLT (Regel research-strategy.md). Vor jeder Web-Recherche MUSS Frank per AskUserQuestion gefragt werden -- Frage 1: A=Firecrawl+MiniMax M3 (max Thinking), B=MiniMax+parallel (max Thinking), C=Opus-Schwarm, D=Freitext. Nach Wahl A oder B die Freigabe setzen: touch '$flag' (gilt 30 Min), dann den Aufruf erneut starten. Bei C laeuft KEIN mm/or-research (Opus-Researcher stattdessen)."
printf '%s' "$reason" | python3 -c 'import sys,json
reason=sys.stdin.read()
print(json.dumps({"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":reason}}))'
exit 0
