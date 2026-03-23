#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$REPO_ROOT"

if command -v rg >/dev/null 2>&1; then
  search_fixed() {
    local pattern="$1"
    shift
    rg -F -- "$pattern" "$@" >/dev/null
  }

  search_regex() {
    local pattern="$1"
    shift
    rg -- "$pattern" "$@" >/dev/null
  }
else
  search_fixed() {
    local pattern="$1"
    shift
    grep -F -- "$pattern" "$@" >/dev/null
  }

  search_regex() {
    local pattern="$1"
    shift
    grep -E -- "$pattern" "$@" >/dev/null
  }
fi

normalize_native_path() {
  node -e "let value=(process.argv[1]||'').replace(/\\\\/g,'/'); value=value.replace(/^([A-Za-z]):/, (_,drive)=>'/' + drive.toLowerCase()); console.log(value);" "$1"
}

has_mcp_server() {
  local name="$1"
  local output
  if ! output="$(codex mcp list 2>&1)"; then
    return 1
  fi
  grep -F -- "$name" <<<"$output" >/dev/null
}

required_files=(
  "AGENTS.md"
  "codex-setup/rules/global.md"
  "codex-setup/rules/self-observation.md"
  "codex-setup/rules/german-trigger-routing.md"
  "codex-setup/rules/claude-delta-sync.md"
  "codex-setup/agent-memory/shared/MEMORY.md"
  "codex-setup/state/claude-delta-state.json"
  "codex-setup/state/environment-fixes.json"
  "codex-setup/bridges/cloud-code-delta-bridge.md"
  "codex-setup/bridges/cloud-code-delta-bridge.json"
  "codex-setup/bridges/environment-fix-exchange-bridge.md"
  "codex-setup/bridges/environment-fix-exchange-bridge.json"
  "codex-setup/scripts/whiteboard-bridge.mjs"
  "codex-setup/scripts/whiteboard-insert.sh"
  "codex-setup/scripts/whiteboard-insert.ps1"
  "codex-setup/scripts/writeback-enforcer.sh"
  "codex-setup/scripts/writeback-enforcer.ps1"
  "codex-setup/scripts/install-self-improve.sh"
  "codex-setup/scripts/install-self-improve.ps1"
  "codex-setup/scripts/check-openai-docs-mcp.mjs"
  "codex-setup/scripts/check-openai-docs-mcp.sh"
  "codex-setup/scripts/check-openai-docs-mcp.ps1"
  "codex-setup/scripts/code-search-mcp-client.mjs"
  "codex-setup/scripts/code-search-mcp-client.sh"
  "codex-setup/scripts/code-search-mcp-client.ps1"
  "codex-setup/scripts/check-code-search-mcp-client.mjs"
  "codex-setup/scripts/check-code-search-health.mjs"
  "codex-setup/scripts/check-code-search-health.sh"
  "codex-setup/scripts/check-code-search-health.ps1"
  "codex-setup/scripts/audit-claude-delta.mjs"
  "codex-setup/scripts/audit-claude-delta.sh"
  "codex-setup/scripts/audit-claude-delta.ps1"
  "codex-setup/scripts/register-environment-fix.mjs"
  "codex-setup/scripts/register-environment-fix.sh"
  "codex-setup/scripts/register-environment-fix.ps1"
  "codex-setup/skills/self-improve/references/claude-delta-sync.md"
  "codex-setup/skills/self-improve/SKILL.md"
)

for file in "${required_files[@]}"; do
  [[ -f "$file" ]] || { echo "Missing required file: $file" >&2; exit 1; }
done

expected_memory="$(normalize_native_path "$REPO_ROOT/codex-setup/agent-memory/shared/MEMORY.md")"
resolved_memory="$(normalize_native_path "$(node "codex-setup/scripts/whiteboard-bridge.mjs" resolve-memory --workspace "$REPO_ROOT")")"
[[ "$resolved_memory" == "$expected_memory" ]] || {
  echo "whiteboard-bridge resolved unexpected memory path: $resolved_memory" >&2
  exit 1
}

outside_dir="$(mktemp -d)"
temp_workspace="$(mktemp -d)"
cleanup() {
  rm -rf "$outside_dir" "$temp_workspace"
}
trap cleanup EXIT

if (cd "$outside_dir" && node "$REPO_ROOT/codex-setup/scripts/whiteboard-bridge.mjs" resolve-memory >/dev/null 2>&1); then
  echo "whiteboard-bridge must fail outside a Codex workspace without --workspace." >&2
  exit 1
fi

if node "codex-setup/scripts/whiteboard-bridge.mjs" resolve-memory --workspace "$REPO_ROOT" --memory "$REPO_ROOT/codex-setup/README.md" >/dev/null 2>&1; then
  echo "whiteboard-bridge must reject non-authoritative --memory paths." >&2
  exit 1
fi

integrity_output="$(node "codex-setup/scripts/whiteboard-bridge.mjs" check-integrity --workspace "$REPO_ROOT")"
[[ "$integrity_output" == OK\ * ]] || {
  echo "whiteboard-bridge integrity check failed: $integrity_output" >&2
  exit 1
}

for section in \
  "## Oberste Direktive" \
  "## Offene Fehler & Probleme" \
  "## Systemzustand" \
  "## Erkenntnisse aus Code Reviews" \
  "## Erkenntnisse aus Tests" \
  "## Architektur-Entscheidungen" \
  "## Debugging-Muster" \
  "## Performance & Optimierung" \
  "## UI/UX-Patterns" \
  "## Forschung & Intelligence" \
  "## Regeln & Konventionen"; do
  search_fixed "$section" "codex-setup/agent-memory/shared/MEMORY.md"
done

for file in \
  "AGENTS.md" \
  "codex-setup/README.md" \
  "codex-setup/rules/global.md" \
  "codex-setup/rules/self-observation.md" \
  "codex-setup/rules/german-trigger-routing.md" \
  "codex-setup/rules/claude-delta-sync.md" \
  "codex-setup/agent-memory/shared/MEMORY.md" \
  "codex-setup/skills/self-improve/SKILL.md" \
  "codex-setup/skills/self-improve/references/claude-delta-sync.md" \
  "codex-setup/skills/self-improve/references/report-and-creative.md" \
  "codex-setup/skills/self-improve/references/whiteboard-bridge.md" \
  "codex-setup/skills/self-improve/references/workspace-scan.md"; do
  search_fixed "Oberste Direktive" "$file"
done

search_fixed "OpenAI developer documentation MCP server" "AGENTS.md"
search_fixed "GeminiCLI" "AGENTS.md"
search_fixed "automatically create a focused commit and push it to \`origin/main\`" "AGENTS.md"
search_fixed "always start with \`Committed.\`" "AGENTS.md"
search_fixed "add a second final line \`Gepusht in <path>, plattformuebergreifend.\`" "AGENTS.md"
search_fixed "nach erfolgreicher lokaler Validierung eigenstaendig committen und nach \`origin/main\` pushen" "codex-setup/rules/global.md"
search_fixed "zuerst \`Committed.\` sobald der lokale Commit existiert" "codex-setup/rules/global.md"
search_fixed "als zweite Abschlusszeile \`Gepusht in <path>, plattformuebergreifend.\`" "codex-setup/rules/global.md"
search_fixed "nach erfolgreicher lokaler Validierung eigenstaendig committen und nach \`origin/main\` pushen soll" "codex-setup/README.md"
search_fixed "check-code-search-health.sh" "codex-setup/README.md"
search_fixed "code-search-mcp-client.sh" "codex-setup/README.md"
search_fixed "Last write mode" "codex-setup/README.md"
search_fixed "audit-claude-delta.mjs" "codex-setup/README.md"
search_fixed "cloud-code-delta-bridge" "codex-setup/README.md"
search_fixed "environment-fixes.json" "codex-setup/README.md"
search_fixed "register-environment-fix.mjs" "codex-setup/README.md"
search_fixed "Starte bitte die Bruecke zu Cloud Code" "codex-setup/README.md"
search_fixed "GeminiCLI" "codex-setup/README.md"
search_fixed "neue Tools, Plugins oder Agenten" "codex-setup/rules/global.md"
search_fixed "semantischer Suche, Indexierung, Hintergrund-Reindex" "codex-setup/rules/global.md"
search_fixed "Read-Only Fremd-Workspaces" "codex-setup/rules/global.md"
search_fixed "GeminiCLI" "codex-setup/rules/global.md"

node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('codex-setup/state/claude-delta-state.json','utf8')); if(data.scope!=='claude-environment-only') process.exit(1); if(!data.replace_requires_confirmation) process.exit(1); if(!data.tracked_paths.includes('CLAUDE.md')) process.exit(1);" || {
  echo "claude-delta-state.json is invalid." >&2
  exit 1
}

node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('codex-setup/state/environment-fixes.json','utf8')); if(data.scope!=='programming-environment-only') process.exit(1); if(!Array.isArray(data.entries) || data.entries.length<1) process.exit(1); if(data.entries.some(entry=>!entry.id||!entry.context_for_other_clis||entry.context_for_other_clis.length<40||!entry.symptom_before_fix||entry.symptom_before_fix.length<40||!entry.root_cause||entry.root_cause.length<30||!entry.what_was_fixed||!entry.why_it_was_fixed||!entry.verification||entry.verification.length<30||!entry.portability_notes||entry.portability_notes.length<30)) process.exit(1);" || {
  echo "environment-fixes.json is invalid." >&2
  exit 1
}

node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('codex-setup/bridges/cloud-code-delta-bridge.json','utf8')); if(data.source_label!=='Cloud Code') process.exit(1); if(!data.replacement_requires_confirmation) process.exit(1); if(!Array.isArray(data.trigger_phrases) || data.trigger_phrases.length<3) process.exit(1); if(!data.trigger_phrases.includes('Starte bitte die Bruecke zu Cloud Code')) process.exit(1);" || {
  echo "cloud-code-delta-bridge.json is invalid." >&2
  exit 1
}

node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('codex-setup/bridges/environment-fix-exchange-bridge.json','utf8')); if(data.source_label!=='CLI Environment Fixes') process.exit(1); if(data.scope!=='programming-environment-only') process.exit(1); if(!data.requires_full_context) process.exit(1); if(!Array.isArray(data.trigger_phrases) || data.trigger_phrases.length<3) process.exit(1);" || {
  echo "environment-fix-exchange-bridge.json is invalid." >&2
  exit 1
}

while IFS= read -r -d '' file; do
  search_fixed "Oberste Direktive" "$file"
done < <(find "codex-setup/skills/self-improve/references/agents" -name "*.md" -print0)

node "codex-setup/scripts/whiteboard-bridge.mjs" print-directive --workspace "$REPO_ROOT" | grep -F "Oberste Direktive" >/dev/null

script_files=()
while IFS= read -r -d '' file; do
  script_files+=("$file")
done < <(find "codex-setup/scripts" -type f \( -name "*.sh" -o -name "*.ps1" -o -name "*.mjs" \) ! -name "validate-codex-setup.sh" ! -name "validate-codex-setup.ps1" -print0)

if ((${#script_files[@]} > 0)) && search_regex \
  "Pepsi1978/proggs|~/proggs/(claude-code-setup|\\.claude)|/Users/frank/proggs/(claude-code-setup|\\.claude)|C:\\\\Users\\\\barwa\\\\proggs|~/.claude/settings.json" \
  "${script_files[@]}"; then
  echo "Forbidden operational Claude/proggs reference found in codex-setup scripts." >&2
  exit 1
fi

while IFS= read -r -d '' file; do
  bash -n "$file"
done < <(find "codex-setup/scripts" -name "*.sh" -print0)

while IFS= read -r -d '' file; do
  node --check "$file"
done < <(find "codex-setup/scripts" -name "*.mjs" -print0)

mkdir -p "$temp_workspace/codex-setup/agent-memory/shared"
cp "codex-setup/agent-memory/shared/MEMORY.md" "$temp_workspace/codex-setup/agent-memory/shared/MEMORY.md"

temp_token="$(node "$REPO_ROOT/codex-setup/scripts/whiteboard-bridge.mjs" directive-token --workspace "$temp_workspace")"
node "$REPO_ROOT/codex-setup/scripts/whiteboard-bridge.mjs" insert \
  --workspace "$temp_workspace" \
  --directive-token "$temp_token" \
  --section "Systemzustand" \
  --entry "- **[2099-01-01 00:00] validator**: bridge mutation smoke test" >/dev/null
search_fixed "bridge mutation smoke test" "$temp_workspace/codex-setup/agent-memory/shared/MEMORY.md"
node "$REPO_ROOT/codex-setup/scripts/whiteboard-bridge.mjs" check-integrity --workspace "$temp_workspace" >/dev/null

if node "$REPO_ROOT/codex-setup/scripts/whiteboard-bridge.mjs" insert \
  --workspace "$temp_workspace" \
  --directive-token "invalid-token" \
  --section "Systemzustand" \
  --entry "- **[2099-01-01 00:01] validator**: should fail" >/dev/null 2>&1; then
  echo "whiteboard-bridge accepted an invalid directive token." >&2
  exit 1
fi
if search_fixed "should fail" "$temp_workspace/codex-setup/agent-memory/shared/MEMORY.md"; then
  echo "whiteboard-bridge wrote data despite an invalid directive token." >&2
  exit 1
fi

if command -v pwsh >/dev/null 2>&1; then
  while IFS= read -r -d '' file; do
    pwsh -NoProfile -Command "[void][scriptblock]::Create((Get-Content -Raw '$file'))"
  done < <(find "codex-setup/scripts" -name "*.ps1" -print0)
fi

if has_mcp_server "openaiDeveloperDocs"; then
  bash "codex-setup/scripts/check-openai-docs-mcp.sh" >/dev/null
else
  echo "Skipping openaiDeveloperDocs MCP smoke test: server not configured in this Codex runtime."
fi
audit_json="$(node "codex-setup/scripts/audit-claude-delta.mjs" --json)"
audit_latest="$(node -e "const data=JSON.parse(process.argv[1]); if(!data.latest_relevant_commit) process.exit(1); console.log(data.latest_relevant_commit);" "$audit_json")"
temp_audit_state="$(mktemp)"
rm -f "$temp_audit_state"
node "codex-setup/scripts/audit-claude-delta.mjs" mark-reviewed --state "$temp_audit_state" --commit "$audit_latest" >/dev/null
node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync(process.argv[1],'utf8')); if(data.last_reviewed_commit!==process.argv[2]) process.exit(1);" "$temp_audit_state" "$audit_latest" || {
  echo "Claude delta audit failed to store a reviewed commit." >&2
  exit 1
}
temp_fix_ledger="$(mktemp)"
rm -f "$temp_fix_ledger"
node "codex-setup/scripts/register-environment-fix.mjs" add --state "$temp_fix_ledger" --id "validator-temp-fix" --category "validator" --summary "temporary validator entry" --context "This temporary validator entry exists only to prove that the environment-fix writer accepts full cross-CLI context." --symptom "Without this smoke test the validator would only know that the file exists, not that the writer can create a detailed entry end to end." --root-cause "The validator needs a write-path proof so schema changes cannot silently break the fix ledger tooling." --what "temporary write path" --why "prove the environment-fix ledger writer works" --verification "The validator reads the temporary ledger back immediately and checks that the written entry id matches the expected smoke-test value." --portability-notes "Other CLIs should keep the same smoke test so a fix ledger is validated as a workflow, not just as a static JSON file." >/dev/null
node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync(process.argv[1],'utf8')); if(!data.entries || data.entries[0].id!=='validator-temp-fix') process.exit(1);" "$temp_fix_ledger" || {
  echo "Environment-fix registration failed." >&2
  exit 1
}
bash "codex-setup/scripts/audit-claude-delta.sh" --json >/dev/null
node "codex-setup/scripts/check-code-search-mcp-client.mjs" >/dev/null
if has_mcp_server "code-search"; then
  bash "codex-setup/scripts/code-search-mcp-client.sh" tools >/dev/null
  bash "codex-setup/scripts/check-code-search-health.sh" --json >/dev/null
else
  echo "Skipping code-search MCP runtime smoke tests: server not configured in this Codex runtime."
fi

echo "codex-setup validation passed"
