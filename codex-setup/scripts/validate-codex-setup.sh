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

required_files=(
  "AGENTS.md"
  "codex-setup/rules/global.md"
  "codex-setup/agent-memory/shared/MEMORY.md"
  "codex-setup/scripts/whiteboard-bridge.mjs"
  "codex-setup/scripts/whiteboard-insert.sh"
  "codex-setup/scripts/whiteboard-insert.ps1"
  "codex-setup/scripts/writeback-enforcer.sh"
  "codex-setup/scripts/writeback-enforcer.ps1"
  "codex-setup/scripts/install-self-improve.sh"
  "codex-setup/scripts/install-self-improve.ps1"
  "codex-setup/scripts/code-search-mcp-client.mjs"
  "codex-setup/scripts/code-search-mcp-client.sh"
  "codex-setup/scripts/code-search-mcp-client.ps1"
  "codex-setup/scripts/check-code-search-mcp-client.mjs"
  "codex-setup/scripts/check-code-search-health.mjs"
  "codex-setup/scripts/check-code-search-health.sh"
  "codex-setup/scripts/check-code-search-health.ps1"
  "codex-setup/skills/self-improve/SKILL.md"
)

for file in "${required_files[@]}"; do
  [[ -f "$file" ]] || { echo "Missing required file: $file" >&2; exit 1; }
done

expected_memory="$REPO_ROOT/codex-setup/agent-memory/shared/MEMORY.md"
resolved_memory="$(node "codex-setup/scripts/whiteboard-bridge.mjs" resolve-memory --workspace "$REPO_ROOT")"
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
  "codex-setup/agent-memory/shared/MEMORY.md" \
  "codex-setup/skills/self-improve/SKILL.md" \
  "codex-setup/skills/self-improve/references/report-and-creative.md" \
  "codex-setup/skills/self-improve/references/whiteboard-bridge.md" \
  "codex-setup/skills/self-improve/references/workspace-scan.md"; do
  search_fixed "Oberste Direktive" "$file"
done

search_fixed "OpenAI developer documentation MCP server" "AGENTS.md"
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
search_fixed "neue Tools, Plugins oder Agenten" "codex-setup/rules/global.md"
search_fixed "semantischer Suche, Indexierung, Hintergrund-Reindex" "codex-setup/rules/global.md"

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

bash "codex-setup/scripts/check-openai-docs-mcp.sh" >/dev/null
node "codex-setup/scripts/check-code-search-mcp-client.mjs" >/dev/null
bash "codex-setup/scripts/code-search-mcp-client.sh" tools >/dev/null
bash "codex-setup/scripts/check-code-search-health.sh" --json >/dev/null

echo "codex-setup validation passed"
