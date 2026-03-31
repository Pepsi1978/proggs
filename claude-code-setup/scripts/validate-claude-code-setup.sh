#!/usr/bin/env bash
# validate-claude-code-setup.sh
# Validates that the claude-code-setup directory is complete and correct.
# Inspired by codex-setup/scripts/validate-codex-setup.sh — adapted for Claude Code.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SETUP_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$SETUP_DIR/.." && pwd)"

# ── counters ────────────────────────────────────────────────────────────────
PASS=0
FAIL=0
WARN=0

pass() { echo "  PASS  $*"; PASS=$(( PASS + 1 )); }
fail() { echo "  FAIL  $*"; FAIL=$(( FAIL + 1 )); }
warn() { echo "  WARN  $*"; WARN=$(( WARN + 1 )); }
section() { echo; echo "── $* ──────────────────────────────────────────────"; }

# ── helpers ─────────────────────────────────────────────────────────────────
check_dir() {
  local rel="$1"
  local full="$SETUP_DIR/$rel"
  if [[ -d "$full" ]]; then
    pass "Directory exists: claude-code-setup/$rel"
  else
    fail "Directory missing: claude-code-setup/$rel"
  fi
}

check_file() {
  local rel="$1"
  local full="$SETUP_DIR/$rel"
  if [[ -f "$full" ]]; then
    pass "File exists: claude-code-setup/$rel"
  else
    fail "File missing: claude-code-setup/$rel"
  fi
}

check_file_repo() {
  # Check a file relative to repo root
  local rel="$1"
  local full="$REPO_ROOT/$rel"
  if [[ -f "$full" ]]; then
    pass "File exists: $rel"
  else
    fail "File missing: $rel"
  fi
}

check_executable() {
  local rel="$1"
  local full="$SETUP_DIR/$rel"
  if [[ ! -f "$full" ]]; then
    fail "Script missing (cannot check +x): claude-code-setup/$rel"
    return
  fi
  if [[ -x "$full" ]]; then
    pass "Script is executable: claude-code-setup/$rel"
  else
    fail "Script not executable (chmod +x needed): claude-code-setup/$rel"
  fi
}

check_json_valid() {
  local rel="$1"
  local full="$SETUP_DIR/$rel"
  if [[ ! -f "$full" ]]; then
    fail "JSON file missing (cannot validate): claude-code-setup/$rel"
    return
  fi
  if node -e "JSON.parse(require('fs').readFileSync('$full','utf8'))" 2>/dev/null; then
    pass "Valid JSON: claude-code-setup/$rel"
  else
    fail "Invalid JSON: claude-code-setup/$rel"
  fi
}

check_json_valid_repo() {
  local rel="$1"
  local full="$REPO_ROOT/$rel"
  if [[ ! -f "$full" ]]; then
    fail "JSON file missing (cannot validate): $rel"
    return
  fi
  if node -e "JSON.parse(require('fs').readFileSync('$full','utf8'))" 2>/dev/null; then
    pass "Valid JSON: $rel"
  else
    fail "Invalid JSON: $rel"
  fi
}

check_content() {
  local file="$SETUP_DIR/$1"
  local pattern="$2"
  local description="$3"
  if [[ ! -f "$file" ]]; then
    fail "Cannot check content (file missing): claude-code-setup/$1"
    return
  fi
  if grep -qF -- "$pattern" "$file" 2>/dev/null; then
    pass "Content found [$description]: claude-code-setup/$1"
  else
    fail "Content missing [$description]: claude-code-setup/$1"
  fi
}

check_syntax_sh() {
  local full="$1"
  local rel="${full#$REPO_ROOT/}"
  if bash -n "$full" 2>/dev/null; then
    pass "Shell syntax OK: $rel"
  else
    fail "Shell syntax error: $rel"
  fi
}

# ════════════════════════════════════════════════════════════════════════════
echo "=== Claude Code Setup Validation ==="
echo "Setup dir : $SETUP_DIR"
echo "Repo root : $REPO_ROOT"
echo "Date      : $(date '+%Y-%m-%d %H:%M:%S')"

# ── 1. Required directories ─────────────────────────────────────────────────
section "1. Required Directories"
for d in rules agents commands hooks bridges state scripts tools; do
  check_dir "$d"
done
# agent-memory/shared is a non-standard path — check separately
if [[ -d "$SETUP_DIR/agent-memory/shared" ]]; then
  pass "Directory exists: claude-code-setup/agent-memory/shared"
else
  fail "Directory missing: claude-code-setup/agent-memory"
fi

# ── 2. Mandatory top-level files ────────────────────────────────────────────
section "2. Mandatory Files"
check_file "bridges/bridge-registry.json"
check_file "state/codex-delta-state.json"
check_file "state/gemini-delta-state.json"
check_file "state/implemented-intelligence-suggestions.json"

# ── 3. Three directives ─────────────────────────────────────────────────────
section "3. Three Directives"
check_file "rules/superintelligence.md"
check_file "rules/self-observation.md"
check_file "rules/resilient-bugfixing.md"

# ── 4. Bridge specs ─────────────────────────────────────────────────────────
section "4. Bridge Specs"
for f in \
  "bridges/codex-delta-bridge.md" \
  "bridges/codex-delta-bridge.json" \
  "bridges/gemini-delta-bridge.md" \
  "bridges/gemini-delta-bridge.json" \
  "bridges/environment-fix-exchange-bridge.md" \
  "bridges/environment-fix-exchange-bridge.json" \
  "bridges/intelligence-suggestion-exchange-bridge.md" \
  "bridges/intelligence-suggestion-exchange-bridge.json" \
  "bridges/directive-1-superintelligence-universal.md"
do
  check_file "$f"
done

# directive-2 may use either naming convention — check both
if [[ -f "$SETUP_DIR/bridges/directive-2-self-observation-universal.md" ]]; then
  pass "File exists: claude-code-setup/bridges/directive-2-self-observation-universal.md"
else
  warn "File missing: claude-code-setup/bridges/directive-2-self-observation-universal.md (optional but recommended)"
fi

# ── 5. JSON validation ───────────────────────────────────────────────────────
section "5. JSON Validation"
for f in \
  "bridges/bridge-registry.json" \
  "bridges/codex-delta-bridge.json" \
  "bridges/gemini-delta-bridge.json" \
  "bridges/environment-fix-exchange-bridge.json" \
  "bridges/intelligence-suggestion-exchange-bridge.json" \
  "state/codex-delta-state.json" \
  "state/gemini-delta-state.json" \
  "state/implemented-intelligence-suggestions.json"
do
  check_json_valid "$f"
done

# ── 6. Mandatory agents ──────────────────────────────────────────────────────
section "6. Mandatory Agents"
for f in \
  "agents/architect.md" \
  "agents/code-reviewer.md" \
  "agents/debugger.md" \
  "agents/tester.md" \
  "agents/quality-gate.md"
do
  check_file "$f"
done

# ── 7. Mandatory commands ────────────────────────────────────────────────────
section "7. Mandatory Commands"
for f in \
  "commands/self-improve.md" \
  "commands/codex-bridge.md" \
  "commands/gemini-bridge.md"
do
  check_file "$f"
done

# ── 8. Scripts executable ────────────────────────────────────────────────────
section "8. Scripts Executable (+x)"
while IFS= read -r -d '' script; do
  rel="${script#$SETUP_DIR/}"
  if [[ -x "$script" ]]; then
    pass "Executable: claude-code-setup/$rel"
  else
    fail "Not executable: claude-code-setup/$rel"
  fi
done < <(find "$SETUP_DIR/scripts" -maxdepth 2 -type f -name "*.sh" -print0 2>/dev/null)

# ── 9. Whiteboard ────────────────────────────────────────────────────────────
section "9. Whiteboard"
if [[ -f "$SETUP_DIR/agent-memory/shared/MEMORY.md" ]]; then
  pass "Whiteboard exists: claude-code-setup/agent-memory/shared/MEMORY.md"
else
  fail "Whiteboard missing: claude-code-setup/agent-memory/shared/MEMORY.md"
fi

# ── 10. CLAUDE.md in repo root ───────────────────────────────────────────────
section "10. CLAUDE.md"
if [[ -f "$REPO_ROOT/CLAUDE.md" ]]; then
  pass "CLAUDE.md exists in repo root"
else
  fail "CLAUDE.md missing from repo root"
fi

# ── 11. Directive content in key files ──────────────────────────────────────
section "11. Directive Content Checks"
check_content "rules/superintelligence.md" "Superintelligenz" "superintelligence keyword"
check_content "rules/self-observation.md" "Selbstbeobachtung" "self-observation keyword"
check_content "rules/resilient-bugfixing.md" "Resilient" "resilient bugfixing keyword"
check_content "bridges/directive-1-superintelligence-universal.md" "Superintelligenz" "directive 1 content"
check_content "agent-memory/shared/MEMORY.md" "MEMORY" "whiteboard header"

# ── 12. bridge-registry.json structural check ───────────────────────────────
section "12. bridge-registry.json Structure"
registry="$SETUP_DIR/bridges/bridge-registry.json"
if [[ -f "$registry" ]]; then
  if node -e \
"const fs=require('fs');
const data=JSON.parse(fs.readFileSync(process.argv[1],'utf8'));
const all=Object.assign({},data.bridges||{},data.exchange_bridges||{});
const req=['codex-delta','gemini-delta','environment-fix-exchange','intelligence-suggestion-exchange'];
const m=req.filter(k=>!all[k]);
if(m.length){process.stderr.write('Missing: '+m.join(', ')+'\n');process.exit(1);}
" "$registry" 2>/dev/null; then
    pass "bridge-registry.json has all required bridge entries"
  else
    fail "bridge-registry.json is missing required bridge entries"
  fi
else
  fail "bridge-registry.json missing — cannot check structure"
fi

# ── 13. Shell syntax check ───────────────────────────────────────────────────
section "13. Shell Syntax"
while IFS= read -r -d '' script; do
  check_syntax_sh "$script"
done < <(find "$SETUP_DIR/scripts" -maxdepth 2 -type f -name "*.sh" \
         ! -name "validate-claude-code-setup.sh" -print0 2>/dev/null)

# ── 14. Additional recommended files (WARN only) ─────────────────────────────
section "14. Recommended Files (WARN if missing)"
for f in \
  "README.md" \
  "manifest.json" \
  "settings.json"
do
  if [[ -f "$SETUP_DIR/$f" ]]; then
    pass "Recommended file exists: claude-code-setup/$f"
  else
    warn "Recommended file missing: claude-code-setup/$f"
  fi
done

# ── Summary ──────────────────────────────────────────────────────────────────
echo
echo "══════════════════════════════════════════"
echo "=== Claude Code Setup Validation ==="
printf "PASS: %-4s  FAIL: %-4s  WARN: %s\n" "$PASS" "$FAIL" "$WARN"
echo "══════════════════════════════════════════"

if [ "$FAIL" -gt 0 ]; then
  echo "Result: FAIL — $FAIL check(s) failed. Fix the issues above before using this setup."
  exit 1
else
  echo "Result: PASS — All required checks passed."
  exit 0
fi
