#!/usr/bin/env bash
# silent-corrector.sh — PreToolUse Hook: Blocks known dangerous patterns
# Intercepts Bash commands that violate project rules, with precise matching
# to avoid false positives on commands that merely MENTION the patterns.
#
# Corrections implemented:
#   1. cd into ~/Codex/ → BLOCK (most dangerous Codex operation)
#   2. sed as PRIMARY command on JSON → BLOCK (no-sed-on-json rule)
#
# Intentionally NOT implemented:
#   3. git push without fetch+rebase — handled by poka-yoke-git-push.sh
#   4. Deep string analysis — would be too fragile, rules cover edge cases
#
# Hook protocol (PreToolUse):
#   - Receives tool invocation as JSON on stdin
#   - exit 0 = allow unchanged
#   - exit 2 + JSON stdout = block with reason
#
# Defense in Depth (Directive #3):
#   - set +e: never abort on error
#   - Fail-open: if uncertain, ALLOW (rules catch edge cases)
#   - printf instead of echo for safe output
#   - ALWAYS exit 0 on unhandled paths
#   - Pattern matching via python3 for precision (no shell regex issues)
#
# Source: Direktiven-Recherche 2026-04-06, Erkenntnis 3
# Poka-Yoke Stufe 2: Prevents the most dangerous known patterns

set +e

LOG_FILE="/tmp/silent-corrector.log"
TS="$(date '+%Y-%m-%d %H:%M:%S' 2>/dev/null || echo 'unknown')"

log_event() {
    printf '[%s] %s: %s\n' "$TS" "$1" "$2" >> "$LOG_FILE" 2>/dev/null || true
}

# ─── Read stdin ──────────────────────────────────────────────────────
INPUT=""
INPUT=$(cat 2>/dev/null) || true
[ -z "$INPUT" ] && exit 0

# ─── Extract tool_name and command via python3 (robust JSON parsing) ─
# All extraction and checking in ONE python3 call to minimize overhead
# and avoid shell escaping issues.
RESULT=""
RESULT=$(printf '%s' "$INPUT" | python3 -c '
import sys, json, re

try:
    data = json.load(sys.stdin)
except Exception:
    print("PASS")
    sys.exit(0)

tool_name = data.get("tool_name", "")
if tool_name != "Bash":
    print("PASS")
    sys.exit(0)

cmd = data.get("tool_input", {}).get("command", "")
if not cmd:
    print("PASS")
    sys.exit(0)

# Split command into individual sub-commands (by && ; ||)
# Then check EACH sub-command for dangerous patterns
parts = re.split(r"\s*(?:&&|;|\|\|)\s*", cmd)

for part in parts:
    stripped = part.strip()
    if not stripped:
        continue

    # CHECK 1: cd into Codex directory
    # Only matches: cd ~/Codex, cd /Users/*/Codex, cd "$HOME/Codex"
    if re.match(r"cd\s+.*[~/]Codex(/|$|\s|\")", stripped):
        print("BLOCK_CODEX")
        sys.exit(0)

    # CHECK 2: Direct file operations on Codex paths
    # Matches: ls ~/Codex/, cat /Users/frank/Codex/file, rm ~/Codex/...
    file_ops = r"^(ls|cat|rm|mv|cp|touch|mkdir|chmod|head|tail|wc|stat|file|readlink)\s+"
    if re.match(file_ops, stripped) and re.search(r"[~/]Codex/", stripped):
        print("BLOCK_CODEX")
        sys.exit(0)

    # CHECK 3: sed as PRIMARY command targeting a .json file
    # Only matches when sed is the actual command, not mentioned in a string
    # Must be: sed [flags] [pattern] <file>.json
    if re.match(r"sed\s+", stripped) and re.search(r"\.json\b", stripped):
        print("BLOCK_SED_JSON")
        sys.exit(0)

print("PASS")
' 2>/dev/null) || true

# ─── Act on result ───────────────────────────────────────────────────
case "$RESULT" in
    BLOCK_CODEX)
        log_event "BLOCKED Codex-path" "command operated on ~/Codex/"
        printf '{"reason":"BLOCKIERT: ~/Codex/ ist gesperrt (codex-directory-forbidden.md). Verwende ~/proggs/ statt ~/Codex/."}\n'
        exit 2
        ;;
    BLOCK_SED_JSON)
        log_event "BLOCKED sed-on-JSON" "sed command targeting .json file"
        printf '{"reason":"BLOCKIERT: sed auf JSON verboten (no-sed-on-json.md). Benutze Edit-Tool oder python3 json.load/dump."}\n'
        exit 2
        ;;
    *)
        # PASS or any unexpected value — allow through (fail-open)
        exit 0
        ;;
esac

# Fallback — should never reach here, but if it does: allow
exit 0
