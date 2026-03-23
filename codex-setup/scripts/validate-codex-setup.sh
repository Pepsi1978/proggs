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
  "codex-setup/rules/resilient-bugfixing.md"
  "codex-setup/rules/german-trigger-routing.md"
  "codex-setup/rules/claude-delta-sync.md"
  "codex-setup/rules/gemini-delta-sync.md"
  "codex-setup/agent-memory/shared/MEMORY.md"
  "codex-setup/state/claude-delta-state.json"
  "codex-setup/state/gemini-delta-state.json"
  "codex-setup/state/environment-fixes.json"
  "codex-setup/state/implemented-intelligence-suggestions.json"
  "codex-setup/bridges/cloud-code-delta-bridge.md"
  "codex-setup/bridges/cloud-code-delta-bridge.json"
  "codex-setup/bridges/gemini-cli-delta-bridge.md"
  "codex-setup/bridges/gemini-cli-delta-bridge.json"
  "codex-setup/bridges/bridge-registry.json"
  "codex-setup/bridges/environment-fix-exchange-bridge.md"
  "codex-setup/bridges/environment-fix-exchange-bridge.json"
  "codex-setup/bridges/intelligence-suggestion-exchange-bridge.md"
  "codex-setup/bridges/intelligence-suggestion-exchange-bridge.json"
  "codex-setup/scripts/bridge-registry.mjs"
  "codex-setup/scripts/whiteboard-bridge.mjs"
  "codex-setup/scripts/whiteboard-insert.sh"
  "codex-setup/scripts/whiteboard-insert.ps1"
  "codex-setup/scripts/writeback-enforcer.sh"
  "codex-setup/scripts/writeback-enforcer.ps1"
  "codex-setup/scripts/install-self-improve.sh"
  "codex-setup/scripts/install-self-improve.ps1"
  "codex-setup/scripts/bootstrap-codex-setup.sh"
  "codex-setup/scripts/bootstrap-codex-setup.ps1"
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
  "codex-setup/scripts/audit-gemini-delta.mjs"
  "codex-setup/scripts/audit-gemini-delta.sh"
  "codex-setup/scripts/audit-gemini-delta.ps1"
  "codex-setup/scripts/register-environment-fix.mjs"
  "codex-setup/scripts/register-environment-fix.sh"
  "codex-setup/scripts/register-environment-fix.ps1"
  "codex-setup/scripts/new-environment-fix.mjs"
  "codex-setup/scripts/new-environment-fix.sh"
  "codex-setup/scripts/new-environment-fix.ps1"
  "codex-setup/scripts/register-intelligence-suggestion.mjs"
  "codex-setup/scripts/register-intelligence-suggestion.sh"
  "codex-setup/scripts/register-intelligence-suggestion.ps1"
  "codex-setup/skills/self-improve/references/claude-delta-sync.md"
  "codex-setup/skills/self-improve/references/gemini-delta-sync.md"
  "codex-setup/skills/self-improve/references/resilient-bugfixing.md"
  "codex-setup/skills/self-improve/references/agents/gemini-delta-scanner.md"
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
  "codex-setup/rules/resilient-bugfixing.md" \
  "codex-setup/rules/german-trigger-routing.md" \
  "codex-setup/rules/claude-delta-sync.md" \
  "codex-setup/rules/gemini-delta-sync.md" \
  "codex-setup/agent-memory/shared/MEMORY.md" \
  "codex-setup/skills/self-improve/SKILL.md" \
  "codex-setup/skills/self-improve/references/claude-delta-sync.md" \
  "codex-setup/skills/self-improve/references/gemini-delta-sync.md" \
  "codex-setup/skills/self-improve/references/resilient-bugfixing.md" \
  "codex-setup/skills/self-improve/references/report-and-creative.md" \
  "codex-setup/skills/self-improve/references/whiteboard-bridge.md" \
  "codex-setup/skills/self-improve/references/workspace-scan.md"; do
  search_fixed "Oberste Direktive" "$file"
done

search_fixed "OpenAI developer documentation MCP server" "AGENTS.md"
search_fixed "GeminiCLI" "AGENTS.md"
search_fixed "proposal-only" "AGENTS.md"
search_fixed "8 intelligence dimensions" "AGENTS.md"
search_fixed "💡 Intelligenz-Vorschlag:" "AGENTS.md"
search_fixed "implemented-intelligence-suggestions.json" "AGENTS.md"
search_fixed "bridge-registry.json" "AGENTS.md"
search_fixed "every agent, skill, plugin, hook, and process" "AGENTS.md"
search_fixed "resilient bugfixing as directive 3" "AGENTS.md"
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
search_fixed "audit-gemini-delta.mjs" "codex-setup/README.md"
search_fixed "cloud-code-delta-bridge" "codex-setup/README.md"
search_fixed "environment-fixes.json" "codex-setup/README.md"
search_fixed "register-environment-fix.mjs" "codex-setup/README.md"
search_fixed "implemented-intelligence-suggestions.json" "codex-setup/README.md"
search_fixed "register-intelligence-suggestion.mjs" "codex-setup/README.md"
search_fixed "new-environment-fix.ps1" "codex-setup/README.md"
search_fixed "intelligence-suggestion-exchange-bridge" "codex-setup/README.md"
search_fixed "bootstrap-codex-setup.sh" "codex-setup/README.md"
search_fixed "bootstrap-codex-setup.ps1" "codex-setup/README.md"
search_fixed "Starte bitte die Bruecke zu Cloud Code" "codex-setup/README.md"
search_fixed "Starte bitte die Bruecke zu Gemini CLI" "codex-setup/README.md"
search_fixed "GeminiCLI" "codex-setup/README.md"
search_fixed "8 Intelligenz-Dimensionen" "codex-setup/README.md"
search_fixed "resilient-bugfixing" "codex-setup/README.md"
search_fixed "bridge-registry.json" "codex-setup/README.md"
search_fixed "neue Tools, Plugins oder Agenten" "codex-setup/rules/global.md"
search_fixed "semantischer Suche, Indexierung, Hintergrund-Reindex" "codex-setup/rules/global.md"
search_fixed "Read-Only Fremd-Workspaces" "codex-setup/rules/global.md"
search_fixed "GeminiCLI" "codex-setup/rules/global.md"
search_fixed "Die 8 Intelligenz-Dimensionen" "codex-setup/rules/global.md"
search_fixed "Cross-Tool-Lernen" "codex-setup/rules/global.md"
search_fixed "💡 Intelligenz-Vorschlag:" "codex-setup/rules/global.md"
search_fixed "implemented-intelligence-suggestions.json" "codex-setup/rules/global.md"
search_fixed "bridge-registry.json" "codex-setup/rules/global.md"
search_fixed "Direktive 3: Resilient Bugfixing" "codex-setup/rules/global.md"
search_fixed "Die 6 Beobachtungskategorien" "codex-setup/rules/self-observation.md"
search_fixed "### 4. Wissensluecken" "codex-setup/rules/self-observation.md"
search_fixed "### 6. Benutzer-Hinweise" "codex-setup/rules/self-observation.md"
search_fixed "Rueckmeldung: immer am Ende der Aufgabe und nach der Status-Meldung" "codex-setup/rules/self-observation.md"
search_fixed "jeden Agenten, jeden Skill, jedes Plugin, jeden Hook und jeden Prozess" "codex-setup/rules/self-observation.md"
search_fixed '3x `Warum?`' "codex-setup/rules/resilient-bugfixing.md"
search_fixed "Fix-Induced-Failure-Pruefung" "codex-setup/rules/resilient-bugfixing.md"
search_fixed "Defense in Depth" "codex-setup/rules/resilient-bugfixing.md"
search_fixed "Jeder Umgebungsfix gehoert in \`codex-setup/state/environment-fixes.json\`" "codex-setup/rules/resilient-bugfixing.md"

node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('codex-setup/state/claude-delta-state.json','utf8')); if(data.scope!=='claude-environment-only') process.exit(1); if(!data.replace_requires_confirmation) process.exit(1); if(!data.tracked_paths.includes('CLAUDE.md')) process.exit(1);" || {
  echo "claude-delta-state.json is invalid." >&2
  exit 1
}

node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('codex-setup/state/environment-fixes.json','utf8')); if(data.scope!=='programming-environment-only') process.exit(1); if(!Array.isArray(data.entries) || data.entries.length<1) process.exit(1); if(data.entries.some(entry=>!entry.id||!entry.context_for_other_clis||entry.context_for_other_clis.length<40||!entry.symptom_before_fix||entry.symptom_before_fix.length<40||!entry.root_cause||entry.root_cause.length<30||!entry.what_was_fixed||!entry.why_it_was_fixed||!entry.exact_error_text||entry.exact_error_text.length<20||!entry.root_cause_why_chain||entry.root_cause_why_chain.length<40||!entry.related_error_sources_checked||entry.related_error_sources_checked.length<40||!entry.wrong_pattern_example||!entry.right_pattern_example||!entry.avoidance_rule||entry.avoidance_rule.length<25||!entry.resilience_summary||entry.resilience_summary.length<40||!entry.fix_induced_failure_review||entry.fix_induced_failure_review.length<40||!entry.verification||entry.verification.length<30||!entry.portability_notes||entry.portability_notes.length<30)) process.exit(1);" || {
  echo "environment-fixes.json is invalid." >&2
  exit 1
}

node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('codex-setup/bridges/bridge-registry.json','utf8')); if(data.registry_name!=='codex-bridge-registry') process.exit(1); if(data.proposal_prefix!=='💡 Intelligenz-Vorschlag:') process.exit(1); if(!data.global_policies || !data.global_policies.proposal_only || !data.global_policies.replace_requires_confirmation || !data.global_policies.foreign_sources_read_only || !data.global_policies.implemented_intelligence_must_be_resilient || !data.global_policies.implemented_intelligence_resilience_rule) process.exit(1); if(!data.peer_registry_targets || !data.peer_registry_targets.Codex || !data.peer_registry_targets['Cloud Code'] || !data.peer_registry_targets['Gemini CLI']) process.exit(1); if(!data.bootstrap_artifacts || !data.bootstrap_artifacts.Codex || !Array.isArray(data.bootstrap_artifacts.Codex.repo_scripts) || data.bootstrap_artifacts.Codex.repo_scripts.length<2 || !data.bootstrap_artifacts['Cloud Code'] || !data.bootstrap_artifacts['Gemini CLI']) process.exit(1); if(!data.bridges || !data.bridges['cloud-code-delta'] || !data.bridges['gemini-cli-delta'] || !data.bridges['environment-fix-exchange'] || !data.bridges['implemented-intelligence-suggestion-exchange']) process.exit(1);" || {
  echo "bridge-registry.json is invalid." >&2
  exit 1
}

node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('codex-setup/bridges/bridge-registry.json','utf8')); if(data.bridges['cloud-code-delta'].state_scope!=='claude-environment-only') process.exit(1); if(data.bridges['gemini-cli-delta'].state_scope!=='gemini-environment-only') process.exit(1); if(!Array.isArray(data.bridges['cloud-code-delta'].audit_git_paths) || data.bridges['cloud-code-delta'].audit_git_paths.length<2) process.exit(1); if(!Array.isArray(data.bridges['gemini-cli-delta'].audit_git_paths) || data.bridges['gemini-cli-delta'].audit_git_paths.length<1) process.exit(1); if(!data.bridges['cloud-code-delta'].audit_title || !data.bridges['gemini-cli-delta'].audit_title) process.exit(1);" || {
  echo "bridge-registry.json must define audit scope, titles, and git paths for the delta bridges." >&2
  exit 1
}

node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('codex-setup/state/implemented-intelligence-suggestions.json','utf8')); if(data.scope!=='programming-environment-only') process.exit(1); if(data.ledger_kind!=='implemented-intelligence-suggestions') process.exit(1); if(data.proposal_prefix!=='💡 Intelligenz-Vorschlag:') process.exit(1); if(data.bridge_registry_path!=='codex-setup/bridges/bridge-registry.json') process.exit(1); if(!data.github_url || !data.bridge_registry_github_url || !data.resilience_rule || data.resilience_rule.length<40) process.exit(1); if(!Array.isArray(data.required_resilience_fields) || !data.required_resilience_fields.includes('resilience_summary') || !data.required_resilience_fields.includes('future_failure_review')) process.exit(1); if(!data.peer_ledgers || !data.peer_ledgers.Codex || !data.peer_ledgers['Cloud Code'] || !data.peer_ledgers['Gemini CLI']) process.exit(1); if(!Array.isArray(data.entries) || data.entries.length<1) process.exit(1); if(data.entries.some(entry=>!entry.id||!entry.summary||!entry.proposal_text||!String(entry.proposal_text).startsWith('💡 Intelligenz-Vorschlag:')||!entry.context_for_other_clis||entry.context_for_other_clis.length<40||!entry.why_it_was_suggested||entry.why_it_was_suggested.length<30||!entry.why_it_was_implemented||entry.why_it_was_implemented.length<30||!entry.how_it_was_implemented||entry.how_it_was_implemented.length<40||!entry.bridge_value||entry.bridge_value.length<30||!entry.adoption_guidance||entry.adoption_guidance.length<30||!entry.resilience_summary||entry.resilience_summary.length<40||!entry.future_failure_review||entry.future_failure_review.length<40)) process.exit(1);" || {
  echo "implemented-intelligence-suggestions.json is invalid." >&2
  exit 1
}

search_fixed "Die 8 Intelligenz-Dimensionen" "codex-setup/agent-memory/shared/MEMORY.md"
search_fixed "Cross-Tool-Lernen" "codex-setup/agent-memory/shared/MEMORY.md"
search_fixed "💡 Intelligenz-Vorschlag:" "codex-setup/agent-memory/shared/MEMORY.md"
search_fixed "Effizienzverluste, Wissensluecken, wiederkehrenden Muster" "codex-setup/agent-memory/shared/MEMORY.md"
search_fixed "Direktive 3: Resilient Bugfixing" "codex-setup/agent-memory/shared/MEMORY.md"
search_fixed "💡 Intelligenz-Vorschlag:" "codex-setup/skills/self-improve/references/report-and-creative.md"

node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('codex-setup/bridges/cloud-code-delta-bridge.json','utf8')); if(data.source_label!=='Cloud Code') process.exit(1); if(!data.replacement_requires_confirmation) process.exit(1); if(!Array.isArray(data.trigger_phrases) || data.trigger_phrases.length<3) process.exit(1); if(!data.trigger_phrases.includes('Starte bitte die Bruecke zu Cloud Code')) process.exit(1);" || {
  echo "cloud-code-delta-bridge.json is invalid." >&2
  exit 1
}

node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('codex-setup/bridges/cloud-code-delta-bridge.json','utf8')); if(!data.proposal_only) process.exit(1);" || {
  echo "cloud-code-delta-bridge.json must be proposal-only." >&2
  exit 1
}

node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('codex-setup/bridges/cloud-code-delta-bridge.json','utf8')); if(!data.include_implemented_intelligence_suggestions) process.exit(1); if(!data.exchange_ledgers || !data.exchange_ledgers.implemented_intelligence_suggestions || !data.exchange_ledgers.implemented_intelligence_suggestions.codex || !data.exchange_ledgers.implemented_intelligence_suggestions.codex.repo_path) process.exit(1); if(!data.exchange_ledgers.bootstrap_setup || !data.exchange_ledgers.bootstrap_setup.codex || !Array.isArray(data.exchange_ledgers.bootstrap_setup.codex.repo_scripts) || data.exchange_ledgers.bootstrap_setup.codex.repo_scripts.length<2 || !data.exchange_ledgers.bootstrap_setup.cloud_code_expected || !data.exchange_ledgers.bootstrap_setup.gemini_expected) process.exit(1);" || {
  echo "cloud-code-delta-bridge.json must expose implemented-suggestion ledger addresses." >&2
  exit 1
}

node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('codex-setup/bridges/cloud-code-delta-bridge.json','utf8')); if(!data.bridge_registry || data.bridge_registry.registry_path!=='codex-setup/bridges/bridge-registry.json' || data.bridge_registry.bridge_id!=='cloud-code-delta' || !data.bridge_registry.peer_expected_registries || !data.bridge_registry.peer_expected_registries['Cloud Code'] || !data.bridge_registry.peer_expected_registries['Gemini CLI']) process.exit(1);" || {
  echo "cloud-code-delta-bridge.json must define bridge-registry metadata." >&2
  exit 1
}

node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('codex-setup/bridges/environment-fix-exchange-bridge.json','utf8')); if(data.source_label!=='CLI Environment Fixes') process.exit(1); if(data.scope!=='programming-environment-only') process.exit(1); if(!data.requires_full_context) process.exit(1); if(!Array.isArray(data.trigger_phrases) || data.trigger_phrases.length<3) process.exit(1);" || {
  echo "environment-fix-exchange-bridge.json is invalid." >&2
  exit 1
}

node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('codex-setup/bridges/environment-fix-exchange-bridge.json','utf8')); if(!data.requires_resilient_bugfix_fields) process.exit(1);" || {
  echo "environment-fix-exchange-bridge.json must require resilient bugfix fields." >&2
  exit 1
}

node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('codex-setup/bridges/environment-fix-exchange-bridge.json','utf8')); if(!data.bridge_registry || data.bridge_registry.registry_path!=='codex-setup/bridges/bridge-registry.json' || data.bridge_registry.bridge_id!=='environment-fix-exchange') process.exit(1);" || {
  echo "environment-fix-exchange-bridge.json must define bridge-registry metadata." >&2
  exit 1
}

node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('codex-setup/bridges/intelligence-suggestion-exchange-bridge.json','utf8')); if(data.source_label!=='Implemented Intelligence Suggestions') process.exit(1); if(data.scope!=='programming-environment-only') process.exit(1); if(!data.requires_full_context) process.exit(1); if(!data.peer_registration_required) process.exit(1); if(!Array.isArray(data.trigger_phrases) || data.trigger_phrases.length<3) process.exit(1);" || {
  echo "intelligence-suggestion-exchange-bridge.json is invalid." >&2
  exit 1
}

node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('codex-setup/bridges/intelligence-suggestion-exchange-bridge.json','utf8')); if(data.proposal_prefix!=='💡 Intelligenz-Vorschlag:') process.exit(1); if(!data.requires_resilience_fields) process.exit(1); if(!Array.isArray(data.entry_schema) || !data.entry_schema.includes('resilience_summary') || !data.entry_schema.includes('future_failure_review')) process.exit(1); if(!data.bridge_registry || data.bridge_registry.registry_path!=='codex-setup/bridges/bridge-registry.json' || data.bridge_registry.bridge_id!=='implemented-intelligence-suggestion-exchange') process.exit(1); if(!data.bootstrap_setup || !data.bootstrap_setup.codex || !Array.isArray(data.bootstrap_setup.codex.repo_scripts) || data.bootstrap_setup.codex.repo_scripts.length<2 || !data.bootstrap_setup['Cloud Code'] || !data.bootstrap_setup['Gemini CLI']) process.exit(1);" || {
  echo "intelligence-suggestion-exchange-bridge.json must define proposal-prefix, resilience, and bridge-registry metadata." >&2
  exit 1
}

node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('codex-setup/state/gemini-delta-state.json','utf8')); if(data.scope!=='gemini-environment-only') process.exit(1); if(!data.replace_requires_confirmation) process.exit(1); if(!data.tracked_paths.includes('Gemini-Setup/**')) process.exit(1);" || {
  echo "gemini-delta-state.json is invalid." >&2
  exit 1
}

node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('codex-setup/bridges/gemini-cli-delta-bridge.json','utf8')); if(data.source_label!=='Gemini CLI') process.exit(1); if(!data.proposal_only) process.exit(1); if(!data.replacement_requires_confirmation) process.exit(1); if(!Array.isArray(data.trigger_phrases) || !data.trigger_phrases.includes('Starte bitte die Bruecke zu Gemini CLI')) process.exit(1);" || {
  echo "gemini-cli-delta-bridge.json is invalid." >&2
  exit 1
}

node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('codex-setup/bridges/gemini-cli-delta-bridge.json','utf8')); if(!data.include_implemented_intelligence_suggestions) process.exit(1); if(!data.exchange_ledgers || !data.exchange_ledgers.implemented_intelligence_suggestions || !data.exchange_ledgers.implemented_intelligence_suggestions.codex || !data.exchange_ledgers.implemented_intelligence_suggestions.codex.repo_path) process.exit(1); if(!data.exchange_ledgers.bootstrap_setup || !data.exchange_ledgers.bootstrap_setup.codex || !Array.isArray(data.exchange_ledgers.bootstrap_setup.codex.repo_scripts) || data.exchange_ledgers.bootstrap_setup.codex.repo_scripts.length<2 || !data.exchange_ledgers.bootstrap_setup.gemini_expected || !data.exchange_ledgers.bootstrap_setup.cloud_code_expected) process.exit(1);" || {
  echo "gemini-cli-delta-bridge.json must expose implemented-suggestion ledger addresses." >&2
  exit 1
}

node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync('codex-setup/bridges/gemini-cli-delta-bridge.json','utf8')); if(!data.bridge_registry || data.bridge_registry.registry_path!=='codex-setup/bridges/bridge-registry.json' || data.bridge_registry.bridge_id!=='gemini-cli-delta' || !data.bridge_registry.peer_expected_registries || !data.bridge_registry.peer_expected_registries['Cloud Code'] || !data.bridge_registry.peer_expected_registries['Gemini CLI']) process.exit(1);" || {
  echo "gemini-cli-delta-bridge.json must define bridge-registry metadata." >&2
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

bash "codex-setup/scripts/bootstrap-codex-setup.sh" --skip-validate >/dev/null

if has_mcp_server "openaiDeveloperDocs"; then
  bash "codex-setup/scripts/check-openai-docs-mcp.sh" >/dev/null
else
  echo "Skipping openaiDeveloperDocs MCP smoke test: server not configured in this Codex runtime."
fi
audit_json="$(node "codex-setup/scripts/audit-claude-delta.mjs" --json)"
node -e "const data=JSON.parse(process.argv[1]); if(data.bridge_id!=='cloud-code-delta') process.exit(1); if(data.registry_path!=='codex-setup/bridges/bridge-registry.json') process.exit(1); if(!Array.isArray(data.tracked_git_paths) || data.tracked_git_paths.length<2) process.exit(1); if(!Array.isArray(data.trigger_phrases) || !data.trigger_phrases.includes('Starte bitte die Bruecke zu Cloud Code')) process.exit(1); if(!data.exchange_ledgers || !data.exchange_ledgers.implemented_intelligence_suggestions || !data.exchange_ledgers.implemented_intelligence_suggestions.codex || !data.exchange_ledgers.implemented_intelligence_suggestions.codex.repo_path || !data.exchange_ledgers.bootstrap_setup || !data.exchange_ledgers.bootstrap_setup.codex || !Array.isArray(data.exchange_ledgers.bootstrap_setup.codex.repo_scripts) || data.exchange_ledgers.bootstrap_setup.codex.repo_scripts.length<2) process.exit(1);" "$audit_json" || {
  echo "Claude delta audit must expose registry-driven bridge metadata." >&2
  exit 1
}
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
node "codex-setup/scripts/register-environment-fix.mjs" add --state "$temp_fix_ledger" --id "validator-temp-fix" --category "validator" --summary "temporary validator entry" --context "This temporary validator entry exists only to prove that the environment-fix writer accepts full cross-CLI context." --symptom "Without this smoke test the validator would only know that the file exists, not that the writer can create a detailed entry end to end." --root-cause "The validator needs a write-path proof so schema changes cannot silently break the fix ledger tooling." --what "temporary write path" --why "prove the environment-fix ledger writer works" --exact-error "Validator smoke path would otherwise miss schema-level regressions." --why-chain "Why 1: a file can exist while the writer is still broken. Why 2: schema evolution can silently desynchronize the writer and validator. Why 3: without an end-to-end write test the regression would only surface later in a real session." --related-checks "Checked the write path, JSON serialization path, required-field enforcement, and post-write readback path because all of them belong to the same fix-ledger workflow." --wrong-pattern "Only validate that the ledger file exists or can be parsed." --right-pattern "Create a temporary ledger entry end to end and verify that the persisted id matches the expected smoke-test value." --avoidance-rule "If a fix ledger schema changes, then always run an end-to-end temporary write test instead of validating only file existence or JSON syntax." --resilience-summary "This smoke test is defensive because it checks the workflow instead of one file, update-resilient because it runs after future schema changes, and documented because the validator itself carries the expectation forward." --failure-review "Dependencies: writer and validator must stay aligned. Failure scenario: if the writer breaks the validator must fail loudly. State change: only a temporary file is touched. Race risk is negligible in the temp path. Compatibility remains intact because the smoke uses the same CLI. Platform effects are covered by PowerShell and Bash validators. Update resistance comes from rerunning the same smoke after future schema changes. Graceful degradation is preserved because failure only aborts validation, not the workspace." --verification "The validator reads the temporary ledger back immediately and checks that the written entry id matches the expected smoke-test value." --portability-notes "Other CLIs should keep the same smoke test so a fix ledger is validated as a workflow, not just as a static JSON file." >/dev/null
node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync(process.argv[1],'utf8')); if(!data.entries || data.entries[0].id!=='validator-temp-fix') process.exit(1);" "$temp_fix_ledger" || {
  echo "Environment-fix registration failed." >&2
  exit 1
}
temp_guided_fix_ledger="$(mktemp)"
rm -f "$temp_guided_fix_ledger"
node "codex-setup/scripts/new-environment-fix.mjs" --state "$temp_guided_fix_ledger" --id "validator-guided-fix" --category "validator" --summary "guided validator entry" --context "This guided validator entry proves that the higher-level helper can forward a complete environment-fix payload without manual JSON editing." --symptom "Without this smoke test the guided helper could drift away from register-environment-fix.mjs while the core writer still looks healthy." --root-cause "The helper is a second entry path for the same ledger, so it needs its own end-to-end validation." --what "guided helper write path" --why "prove the guided environment-fix helper stays aligned with the core writer" --exact-error "The regression would otherwise stay invisible until someone relies on the guided helper in a real session." --why-chain "Why 1: a helper can break independently from the core writer. Why 2: it owns argument collection and forwarding. Why 3: without an end-to-end smoke test a drift would only appear during a real logging run." --related-checks "Checked the guided helper argument path, its forwarding logic, the core writer path, and the resulting persisted ledger entry because they form one workflow." --wrong-pattern "Only validate the core writer and assume the guided wrapper still forwards every required field correctly." --right-pattern "Run the guided helper end to end with a temporary ledger and verify that the expected entry id is persisted." --avoidance-rule "If a second helper writes into a validated ledger, then always smoke-test the helper workflow itself instead of validating only the underlying writer." --resilience-summary "This smoke test is defensive because it validates the higher-level entry path, update-resilient because it reruns after future helper changes, and safe because it touches only a temporary file." --failure-review "Dependencies: helper argument collection and the core writer must stay aligned. Failure scenario: if the helper drifts the validator fails loudly. State change: only a temporary file is written. Race risk is negligible. Backward compatibility remains because the helper only forwards data. Platform effects are covered by the shared Node path plus PowerShell and Bash wrappers. Update resistance comes from rerunning this test after future helper changes. Graceful degradation is preserved because helper failure aborts validation instead of corrupting the workspace." --verification "The validator reads the temporary ledger back immediately and checks that the guided helper persisted the expected entry id." --portability-notes "Other CLIs should smoke-test guided logging helpers end to end so wrapper drift does not silently weaken a shared ledger workflow." >/dev/null
node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync(process.argv[1],'utf8')); if(!data.entries || data.entries[0].id!=='validator-guided-fix') process.exit(1);" "$temp_guided_fix_ledger" || {
  echo "Guided environment-fix helper failed." >&2
  exit 1
}
temp_suggestion_ledger="$(mktemp)"
rm -f "$temp_suggestion_ledger"
node "codex-setup/scripts/register-intelligence-suggestion.mjs" add --state "$temp_suggestion_ledger" --id "validator-intelligence-suggestion" --summary "temporary intelligence suggestion entry" --proposal "💡 Intelligenz-Vorschlag: A durable helper should exist for rich ledger writes so future sessions do not retype long schemas manually." --context "This temporary validator entry proves that the implemented-intelligence-suggestion ledger can store a full cross-CLI explanation, not just a short headline." --suggested-because "The richer Codex ledgers are valuable only if their entries remain understandable for other CLIs without access to this session." --implemented-because "The validator needs an end-to-end write proof so the ledger schema and registration script cannot silently drift apart." --implementation "The validator writes a temporary intelligence-suggestion entry through the dedicated registration script and then reads the ledger back to confirm the persisted id." --bridge-value "Other CLIs can trust that the Codex implemented-suggestion ledger is validated as a workflow and not merely as a static JSON file." --adoption-guidance "If another CLI creates the same ledger, it should keep an end-to-end validator smoke test so schema drift is caught before a real session depends on the file." --resilience-summary "This smoke test is resilient because it validates the full implemented-intelligence workflow, not just a static file, and it keeps proposal formatting plus future-proof metadata under active verification." --failure-review "Dependencies: registry defaults, the registration script, and the ledger schema must stay aligned. Failure scenario: if one layer drifts the validator must fail loudly. State change: only a temporary file is written. Race risk is negligible in the temp path. Backward compatibility remains because the smoke uses the same script entrypoint. Platform effects are covered by PowerShell and Bash validators. Update resistance comes from rerunning the smoke after future schema or bridge-registry changes. Graceful degradation is preserved because failure only aborts validation." --portable-to "Cloud Code,Gemini CLI" --artifacts "codex-setup/scripts/register-intelligence-suggestion.mjs,codex-setup/state/implemented-intelligence-suggestions.json" >/dev/null
node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync(process.argv[1],'utf8')); if(!data.entries || data.entries[0].id!=='validator-intelligence-suggestion') process.exit(1);" "$temp_suggestion_ledger" || {
  echo "Intelligence-suggestion registration failed." >&2
  exit 1
}
bash "codex-setup/scripts/audit-claude-delta.sh" --json >/dev/null
gemini_audit_json="$(node "codex-setup/scripts/audit-gemini-delta.mjs" --json)"
node -e "const data=JSON.parse(process.argv[1]); if(data.bridge_id!=='gemini-cli-delta') process.exit(1); if(data.registry_path!=='codex-setup/bridges/bridge-registry.json') process.exit(1); if(!Array.isArray(data.tracked_git_paths) || data.tracked_git_paths.length<1) process.exit(1); if(!Array.isArray(data.trigger_phrases) || !data.trigger_phrases.includes('Starte bitte die Bruecke zu Gemini CLI')) process.exit(1); if(!data.exchange_ledgers || !data.exchange_ledgers.implemented_intelligence_suggestions || !data.exchange_ledgers.implemented_intelligence_suggestions.codex || !data.exchange_ledgers.implemented_intelligence_suggestions.codex.repo_path || !data.exchange_ledgers.bootstrap_setup || !data.exchange_ledgers.bootstrap_setup.codex || !Array.isArray(data.exchange_ledgers.bootstrap_setup.codex.repo_scripts) || data.exchange_ledgers.bootstrap_setup.codex.repo_scripts.length<2) process.exit(1);" "$gemini_audit_json" || {
  echo "Gemini delta audit must expose registry-driven bridge metadata." >&2
  exit 1
}
gemini_audit_latest="$(node -e "const data=JSON.parse(process.argv[1]); if(!data.latest_relevant_commit) process.exit(1); console.log(data.latest_relevant_commit);" "$gemini_audit_json")"
temp_gemini_state="$(mktemp)"
rm -f "$temp_gemini_state"
node "codex-setup/scripts/audit-gemini-delta.mjs" mark-reviewed --state "$temp_gemini_state" --commit "$gemini_audit_latest" >/dev/null
node -e "const fs=require('fs'); const data=JSON.parse(fs.readFileSync(process.argv[1],'utf8')); if(data.last_reviewed_commit!==process.argv[2]) process.exit(1);" "$temp_gemini_state" "$gemini_audit_latest" || {
  echo "Gemini delta audit failed to store a reviewed commit." >&2
  exit 1
}
bash "codex-setup/scripts/audit-gemini-delta.sh" --json >/dev/null
node "codex-setup/scripts/check-code-search-mcp-client.mjs" >/dev/null
if has_mcp_server "code-search"; then
  bash "codex-setup/scripts/code-search-mcp-client.sh" tools >/dev/null
  bash "codex-setup/scripts/check-code-search-health.sh" --json >/dev/null
else
  echo "Skipping code-search MCP runtime smoke tests: server not configured in this Codex runtime."
fi

echo "codex-setup validation passed"
