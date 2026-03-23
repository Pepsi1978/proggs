$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir "..\..")).Path
Set-Location $RepoRoot

function Test-CodexMcpServer {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    $output = & codex mcp list 2>&1
    if ($LASTEXITCODE -ne 0) {
        return $false
    }

    return (($output -join "`n") -match [regex]::Escape($Name))
}

$RequiredFiles = @(
    "AGENTS.md",
    "codex-setup\rules\global.md",
    "codex-setup\rules\self-observation.md",
    "codex-setup\rules\resilient-bugfixing.md",
    "codex-setup\rules\german-trigger-routing.md",
    "codex-setup\rules\claude-delta-sync.md",
    "codex-setup\rules\gemini-delta-sync.md",
    "codex-setup\agent-memory\shared\MEMORY.md",
    "codex-setup\state\claude-delta-state.json",
    "codex-setup\state\gemini-delta-state.json",
    "codex-setup\state\environment-fixes.json",
    "codex-setup\state\implemented-intelligence-suggestions.json",
    "codex-setup\bridges\cloud-code-delta-bridge.md",
    "codex-setup\bridges\cloud-code-delta-bridge.json",
    "codex-setup\bridges\gemini-cli-delta-bridge.md",
    "codex-setup\bridges\gemini-cli-delta-bridge.json",
    "codex-setup\bridges\environment-fix-exchange-bridge.md",
    "codex-setup\bridges\environment-fix-exchange-bridge.json",
    "codex-setup\bridges\intelligence-suggestion-exchange-bridge.md",
    "codex-setup\bridges\intelligence-suggestion-exchange-bridge.json",
    "codex-setup\scripts\whiteboard-bridge.mjs",
    "codex-setup\scripts\whiteboard-insert.sh",
    "codex-setup\scripts\whiteboard-insert.ps1",
    "codex-setup\scripts\writeback-enforcer.sh",
    "codex-setup\scripts\writeback-enforcer.ps1",
    "codex-setup\scripts\install-self-improve.sh",
    "codex-setup\scripts\install-self-improve.ps1",
    "codex-setup\scripts\check-openai-docs-mcp.mjs",
    "codex-setup\scripts\check-openai-docs-mcp.sh",
    "codex-setup\scripts\check-openai-docs-mcp.ps1",
    "codex-setup\scripts\code-search-mcp-client.mjs",
    "codex-setup\scripts\code-search-mcp-client.sh",
    "codex-setup\scripts\code-search-mcp-client.ps1",
    "codex-setup\scripts\check-code-search-mcp-client.mjs",
    "codex-setup\scripts\check-code-search-health.mjs",
    "codex-setup\scripts\check-code-search-health.sh",
    "codex-setup\scripts\check-code-search-health.ps1",
    "codex-setup\scripts\audit-claude-delta.mjs",
    "codex-setup\scripts\audit-claude-delta.sh",
    "codex-setup\scripts\audit-claude-delta.ps1",
    "codex-setup\scripts\audit-gemini-delta.mjs",
    "codex-setup\scripts\audit-gemini-delta.sh",
    "codex-setup\scripts\audit-gemini-delta.ps1",
    "codex-setup\scripts\register-environment-fix.mjs",
    "codex-setup\scripts\register-environment-fix.sh",
    "codex-setup\scripts\register-environment-fix.ps1",
    "codex-setup\scripts\new-environment-fix.mjs",
    "codex-setup\scripts\new-environment-fix.sh",
    "codex-setup\scripts\new-environment-fix.ps1",
    "codex-setup\scripts\register-intelligence-suggestion.mjs",
    "codex-setup\scripts\register-intelligence-suggestion.sh",
    "codex-setup\scripts\register-intelligence-suggestion.ps1",
    "codex-setup\skills\self-improve\references\claude-delta-sync.md",
    "codex-setup\skills\self-improve\references\gemini-delta-sync.md",
    "codex-setup\skills\self-improve\references\resilient-bugfixing.md",
    "codex-setup\skills\self-improve\references\agents\gemini-delta-scanner.md",
    "codex-setup\skills\self-improve\SKILL.md"
)

foreach ($file in $RequiredFiles) {
    if (-not (Test-Path $file)) {
        throw "Missing required file: $file"
    }
}

$ExpectedMemory = Join-Path $RepoRoot "codex-setup\agent-memory\shared\MEMORY.md"
$ResolvedMemory = (node "codex-setup/scripts/whiteboard-bridge.mjs" resolve-memory --workspace $RepoRoot | Select-Object -Last 1).Trim()
if ($ResolvedMemory -ne $ExpectedMemory) {
    throw "whiteboard-bridge resolved unexpected memory path: $ResolvedMemory"
}

$OutsideDir = Join-Path ([System.IO.Path]::GetTempPath()) ("codex-setup-validate-" + [guid]::NewGuid().ToString())
$TempWorkspace = Join-Path ([System.IO.Path]::GetTempPath()) ("codex-setup-memory-" + [guid]::NewGuid().ToString())
New-Item -ItemType Directory -Force -Path $OutsideDir | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $TempWorkspace "codex-setup\agent-memory\shared") | Out-Null
try {
    Push-Location $OutsideDir
    $null = node (Join-Path $RepoRoot "codex-setup\scripts\whiteboard-bridge.mjs") resolve-memory 2>$null
    if ($LASTEXITCODE -eq 0) {
        throw "whiteboard-bridge must fail outside a Codex workspace without --workspace."
    }
} finally {
    Pop-Location
    Remove-Item -Recurse -Force $OutsideDir -ErrorAction SilentlyContinue
}

$null = Copy-Item "codex-setup\agent-memory\shared\MEMORY.md" -Destination (Join-Path $TempWorkspace "codex-setup\agent-memory\shared\MEMORY.md") -Force

try {
    $null = node "codex-setup/scripts/whiteboard-bridge.mjs" resolve-memory --workspace $RepoRoot --memory (Join-Path $RepoRoot "codex-setup\README.md") 2>$null
    if ($LASTEXITCODE -eq 0) {
        throw "whiteboard-bridge must reject non-authoritative --memory paths."
    }

    $IntegrityOutput = (node "codex-setup/scripts/whiteboard-bridge.mjs" check-integrity --workspace $RepoRoot | Select-Object -Last 1).Trim()
    if ($IntegrityOutput -notmatch "^OK [0-9a-f]{64}$") {
        throw "whiteboard-bridge integrity check failed: $IntegrityOutput"
    }
} finally {
    # no-op, temp workspace cleaned later
}

$Sections = @(
    "## Oberste Direktive",
    "## Offene Fehler & Probleme",
    "## Systemzustand",
    "## Erkenntnisse aus Code Reviews",
    "## Erkenntnisse aus Tests",
    "## Architektur-Entscheidungen",
    "## Debugging-Muster",
    "## Performance & Optimierung",
    "## UI/UX-Patterns",
    "## Forschung & Intelligence",
    "## Regeln & Konventionen"
)

$Memory = Get-Content "codex-setup/agent-memory/shared/MEMORY.md" -Raw
foreach ($section in $Sections) {
    if ($Memory -notmatch [regex]::Escape($section)) {
        throw "Missing section in MEMORY.md: $section"
    }
}

$DirectiveFiles = @(
    "AGENTS.md",
    "codex-setup\README.md",
    "codex-setup\rules\global.md",
    "codex-setup\rules\self-observation.md",
    "codex-setup\rules\resilient-bugfixing.md",
    "codex-setup\rules\german-trigger-routing.md",
    "codex-setup\rules\claude-delta-sync.md",
    "codex-setup\rules\gemini-delta-sync.md",
    "codex-setup\agent-memory\shared\MEMORY.md",
    "codex-setup\skills\self-improve\SKILL.md",
    "codex-setup\skills\self-improve\references\claude-delta-sync.md",
    "codex-setup\skills\self-improve\references\gemini-delta-sync.md",
    "codex-setup\skills\self-improve\references\resilient-bugfixing.md",
    "codex-setup\skills\self-improve\references\report-and-creative.md",
    "codex-setup\skills\self-improve\references\whiteboard-bridge.md",
    "codex-setup\skills\self-improve\references\workspace-scan.md"
)

foreach ($file in $DirectiveFiles) {
    if ((Get-Content $file -Raw) -notmatch "Oberste Direktive") {
        throw "Missing Oberste Direktive marker in $file"
    }
}

if ((Get-Content "AGENTS.md" -Raw) -notmatch "OpenAI developer documentation MCP server") {
    throw "AGENTS.md must instruct Codex to use the OpenAI Docs MCP server."
}

if ((Get-Content "AGENTS.md" -Raw) -notmatch "GeminiCLI") {
    throw "AGENTS.md must mark Gemini comparison paths as read-only."
}

if ((Get-Content "AGENTS.md" -Raw) -notmatch "proposal-only") {
    throw "AGENTS.md must mark bridge findings as proposal-only."
}

if ((Get-Content "AGENTS.md" -Raw) -notmatch "8 intelligence dimensions") {
    throw "AGENTS.md must keep the 8 intelligence dimensions in scope."
}

if ((Get-Content "AGENTS.md" -Raw) -notmatch "Intelligenz-Vorschlag:") {
    throw "AGENTS.md must define the intelligence proposal format."
}

if ((Get-Content "AGENTS.md" -Raw) -notmatch "implemented-intelligence-suggestions.json") {
    throw "AGENTS.md must define the implemented intelligence suggestion ledger."
}

if ((Get-Content "AGENTS.md" -Raw) -notmatch "every agent, skill, plugin, hook, and process") {
    throw "AGENTS.md must apply self-observation to every agent, skill, plugin, hook, and process."
}

if ((Get-Content "AGENTS.md" -Raw) -notmatch "resilient bugfixing as directive 3") {
    throw "AGENTS.md must include resilient bugfixing as directive 3."
}

if ((Get-Content "AGENTS.md" -Raw) -notmatch [regex]::Escape('automatically create a focused commit and push it to `origin/main`')) {
    throw "AGENTS.md must instruct Codex to auto-commit and push validated codex-setup changes."
}

if ((Get-Content "AGENTS.md" -Raw) -notmatch [regex]::Escape('always start with `Committed.`')) {
    throw "AGENTS.md must require the final response to start git status reporting with Committed."
}

if ((Get-Content "AGENTS.md" -Raw) -notmatch [regex]::Escape('add a second final line `Gepusht in <path>, plattformuebergreifend.`')) {
    throw "AGENTS.md must define the optional pushed git status line."
}

if ((Get-Content "codex-setup\rules\global.md" -Raw) -notmatch [regex]::Escape('nach erfolgreicher lokaler Validierung eigenstaendig committen und nach `origin/main` pushen')) {
    throw "global.md must instruct Codex to auto-push validated codex-setup changes."
}

if ((Get-Content "codex-setup\rules\global.md" -Raw) -notmatch [regex]::Escape('zuerst `Committed.` sobald der lokale Commit existiert')) {
    throw "global.md must require Committed as the first git status line."
}

if ((Get-Content "codex-setup\rules\global.md" -Raw) -notmatch [regex]::Escape('als zweite Abschlusszeile `Gepusht in <path>, plattformuebergreifend.`')) {
    throw "global.md must define the pushed git status line."
}

if ((Get-Content "codex-setup\README.md" -Raw) -notmatch [regex]::Escape('nach erfolgreicher lokaler Validierung eigenstaendig committen und nach `origin/main` pushen soll')) {
    throw "README.md must document the codex-setup auto-push rule."
}

if ((Get-Content "codex-setup\README.md" -Raw) -notmatch [regex]::Escape('check-code-search-health.sh')) {
    throw "README.md must document the code-search healthcheck script."
}

if ((Get-Content "codex-setup\README.md" -Raw) -notmatch [regex]::Escape('code-search-mcp-client.sh')) {
    throw "README.md must document the direct code-search MCP client."
}

if ((Get-Content "codex-setup\README.md" -Raw) -notmatch [regex]::Escape('Last write mode')) {
    throw "README.md must document the last-write status fields."
}

if ((Get-Content "codex-setup\rules\global.md" -Raw) -notmatch "neue Tools, Plugins oder Agenten") {
    throw "global.md must require future-proof fixes across new tools and agents."
}

if ((Get-Content "codex-setup\rules\global.md" -Raw) -notmatch "semantischer Suche, Indexierung, Hintergrund-Reindex") {
    throw "global.md must route semantic-search questions through the code-search healthcheck."
}

if ((Get-Content "codex-setup\rules\global.md" -Raw) -notmatch "Read-Only Fremd-Workspaces") {
    throw "global.md must document read-only foreign workspaces."
}

if ((Get-Content "codex-setup\rules\global.md" -Raw) -notmatch "GeminiCLI") {
    throw "global.md must mark Gemini comparison paths as read-only."
}

if ((Get-Content "codex-setup\rules\global.md" -Raw) -notmatch "Die 8 Intelligenz-Dimensionen") {
    throw "global.md must define the 8 intelligence dimensions."
}

if ((Get-Content "codex-setup\rules\global.md" -Raw) -notmatch "Cross-Tool-Lernen") {
    throw "global.md must describe cross-tool learning."
}

if ((Get-Content "codex-setup\rules\global.md" -Raw) -notmatch "Intelligenz-Vorschlag:") {
    throw "global.md must define the intelligence proposal format."
}

if ((Get-Content "codex-setup\rules\global.md" -Raw) -notmatch "implemented-intelligence-suggestions.json") {
    throw "global.md must define the implemented intelligence suggestion ledger."
}

if ((Get-Content "codex-setup\rules\global.md" -Raw) -notmatch "Direktive 3: Resilient Bugfixing") {
    throw "global.md must define resilient bugfixing as directive 3."
}

if ((Get-Content "codex-setup\rules\resilient-bugfixing.md" -Raw) -notmatch '3x `Warum\?`') {
    throw "resilient-bugfixing.md must require 3x Warum."
}

if ((Get-Content "codex-setup\rules\resilient-bugfixing.md" -Raw) -notmatch "Fix-Induced-Failure-Pruefung") {
    throw "resilient-bugfixing.md must define the fix-induced-failure review."
}

if ((Get-Content "codex-setup\rules\resilient-bugfixing.md" -Raw) -notmatch "Defense in Depth") {
    throw "resilient-bugfixing.md must define defense in depth."
}

if ((Get-Content "codex-setup\rules\resilient-bugfixing.md" -Raw) -notmatch 'Jeder Umgebungsfix gehoert in `codex-setup/state/environment-fixes.json`') {
    throw "resilient-bugfixing.md must require fix-ledger documentation."
}

if ((Get-Content "codex-setup\rules\self-observation.md" -Raw) -notmatch "Die 6 Beobachtungskategorien") {
    throw "self-observation.md must define the 6 observation categories."
}

if ((Get-Content "codex-setup\rules\self-observation.md" -Raw) -notmatch "### 4. Wissensluecken") {
    throw "self-observation.md must include Wissensluecken."
}

if ((Get-Content "codex-setup\rules\self-observation.md" -Raw) -notmatch "### 6. Benutzer-Hinweise") {
    throw "self-observation.md must include Benutzer-Hinweise."
}

if ((Get-Content "codex-setup\rules\self-observation.md" -Raw) -notmatch "Rueckmeldung: immer am Ende der Aufgabe und nach der Status-Meldung") {
    throw "self-observation.md must require end-of-task reporting after the status message."
}

if ((Get-Content "codex-setup\rules\self-observation.md" -Raw) -notmatch "jeden Agenten, jeden Skill, jedes Plugin, jeden Hook und jeden Prozess") {
    throw "self-observation.md must apply to all working components."
}

if ((Get-Content "codex-setup\README.md" -Raw) -notmatch "audit-claude-delta.mjs") {
    throw "README.md must document the Claude delta audit."
}

if ((Get-Content "codex-setup\README.md" -Raw) -notmatch "audit-gemini-delta.mjs") {
    throw "README.md must document the Gemini delta audit."
}

if ((Get-Content "codex-setup\README.md" -Raw) -notmatch "cloud-code-delta-bridge") {
    throw "README.md must document the reusable Cloud Code bridge."
}

if ((Get-Content "codex-setup\README.md" -Raw) -notmatch "environment-fixes.json") {
    throw "README.md must document the environment-fix ledger."
}

if ((Get-Content "codex-setup\README.md" -Raw) -notmatch "register-environment-fix.mjs") {
    throw "README.md must document the environment-fix registration script."
}

if ((Get-Content "codex-setup\README.md" -Raw) -notmatch "implemented-intelligence-suggestions.json") {
    throw "README.md must document the implemented intelligence suggestion ledger."
}

if ((Get-Content "codex-setup\README.md" -Raw) -notmatch "register-intelligence-suggestion.mjs") {
    throw "README.md must document the intelligence-suggestion registration script."
}

if ((Get-Content "codex-setup\README.md" -Raw) -notmatch "new-environment-fix.ps1") {
    throw "README.md must document the guided environment-fix helper."
}

if ((Get-Content "codex-setup\README.md" -Raw) -notmatch "intelligence-suggestion-exchange-bridge") {
    throw "README.md must document the intelligence-suggestion exchange bridge."
}

if ((Get-Content "codex-setup\README.md" -Raw) -notmatch "Starte bitte die Bruecke zu Cloud Code") {
    throw "README.md must document the direct Cloud Code bridge trigger."
}

if ((Get-Content "codex-setup\README.md" -Raw) -notmatch "Starte bitte die Bruecke zu Gemini CLI") {
    throw "README.md must document the direct Gemini bridge trigger."
}

if ((Get-Content "codex-setup\README.md" -Raw) -notmatch "GeminiCLI") {
    throw "README.md must mark Gemini comparison paths as read-only."
}

if ((Get-Content "codex-setup\README.md" -Raw) -notmatch "8 Intelligenz-Dimensionen") {
    throw "README.md must document the 8 intelligence dimensions."
}

if ((Get-Content "codex-setup\README.md" -Raw) -notmatch "resilient-bugfixing") {
    throw "README.md must document resilient bugfixing."
}

$ClaudeDeltaState = Get-Content "codex-setup\state\claude-delta-state.json" -Raw | ConvertFrom-Json
if ($ClaudeDeltaState.scope -ne "claude-environment-only") {
    throw "claude-delta-state.json must track only Claude environment deltas."
}
if (-not $ClaudeDeltaState.replace_requires_confirmation) {
    throw "claude-delta-state.json must require approval for replacements."
}
if ($ClaudeDeltaState.tracked_paths -notcontains "CLAUDE.md") {
    throw "claude-delta-state.json must track CLAUDE.md."
}

$EnvironmentFixes = Get-Content "codex-setup\state\environment-fixes.json" -Raw | ConvertFrom-Json
if ($EnvironmentFixes.scope -ne "programming-environment-only") {
    throw "environment-fixes.json must track programming-environment fixes only."
}
if ($EnvironmentFixes.entries.Count -lt 1) {
    throw "environment-fixes.json must contain at least one fix entry."
}
foreach ($entry in $EnvironmentFixes.entries) {
    if (
        [string]::IsNullOrWhiteSpace($entry.id) -or
        [string]::IsNullOrWhiteSpace($entry.context_for_other_clis) -or
        [string]::IsNullOrWhiteSpace($entry.symptom_before_fix) -or
        [string]::IsNullOrWhiteSpace($entry.root_cause) -or
        [string]::IsNullOrWhiteSpace($entry.what_was_fixed) -or
        [string]::IsNullOrWhiteSpace($entry.why_it_was_fixed) -or
        [string]::IsNullOrWhiteSpace($entry.exact_error_text) -or
        [string]::IsNullOrWhiteSpace($entry.root_cause_why_chain) -or
        [string]::IsNullOrWhiteSpace($entry.related_error_sources_checked) -or
        [string]::IsNullOrWhiteSpace($entry.wrong_pattern_example) -or
        [string]::IsNullOrWhiteSpace($entry.right_pattern_example) -or
        [string]::IsNullOrWhiteSpace($entry.avoidance_rule) -or
        [string]::IsNullOrWhiteSpace($entry.resilience_summary) -or
        [string]::IsNullOrWhiteSpace($entry.fix_induced_failure_review) -or
        [string]::IsNullOrWhiteSpace($entry.verification) -or
        [string]::IsNullOrWhiteSpace($entry.portability_notes)
    ) {
        throw "environment-fixes.json contains an invalid fix entry."
    }
    if ($entry.context_for_other_clis.Length -lt 40 -or $entry.symptom_before_fix.Length -lt 40 -or $entry.root_cause.Length -lt 30 -or $entry.exact_error_text.Length -lt 20 -or $entry.root_cause_why_chain.Length -lt 40 -or $entry.related_error_sources_checked.Length -lt 40 -or $entry.avoidance_rule.Length -lt 25 -or $entry.resilience_summary.Length -lt 40 -or $entry.fix_induced_failure_review.Length -lt 40 -or $entry.verification.Length -lt 30 -or $entry.portability_notes.Length -lt 30) {
        throw "environment-fixes.json fix entries must contain detailed cross-CLI context."
    }
}

$ImplementedSuggestions = Get-Content "codex-setup\state\implemented-intelligence-suggestions.json" -Raw | ConvertFrom-Json
if ($ImplementedSuggestions.scope -ne "programming-environment-only") {
    throw "implemented-intelligence-suggestions.json must stay environment-only."
}
if ($ImplementedSuggestions.ledger_kind -ne "implemented-intelligence-suggestions") {
    throw "implemented-intelligence-suggestions.json must identify its ledger kind."
}
if (-not $ImplementedSuggestions.github_url) {
    throw "implemented-intelligence-suggestions.json must expose its GitHub URL."
}
if (-not $ImplementedSuggestions.peer_ledgers.Codex) {
    throw "implemented-intelligence-suggestions.json must expose the Codex ledger address."
}
if (-not $ImplementedSuggestions.peer_ledgers.'Cloud Code') {
    throw "implemented-intelligence-suggestions.json must expose the Cloud Code ledger address."
}
if (-not $ImplementedSuggestions.peer_ledgers.'Gemini CLI') {
    throw "implemented-intelligence-suggestions.json must expose the Gemini CLI ledger address."
}
if ($ImplementedSuggestions.entries.Count -lt 1) {
    throw "implemented-intelligence-suggestions.json must contain at least one entry."
}
foreach ($entry in $ImplementedSuggestions.entries) {
    if (
        [string]::IsNullOrWhiteSpace($entry.id) -or
        [string]::IsNullOrWhiteSpace($entry.summary) -or
        [string]::IsNullOrWhiteSpace($entry.proposal_text) -or
        [string]::IsNullOrWhiteSpace($entry.context_for_other_clis) -or
        [string]::IsNullOrWhiteSpace($entry.why_it_was_suggested) -or
        [string]::IsNullOrWhiteSpace($entry.why_it_was_implemented) -or
        [string]::IsNullOrWhiteSpace($entry.how_it_was_implemented) -or
        [string]::IsNullOrWhiteSpace($entry.bridge_value) -or
        [string]::IsNullOrWhiteSpace($entry.adoption_guidance)
    ) {
        throw "implemented-intelligence-suggestions.json contains an invalid entry."
    }
    if ($entry.context_for_other_clis.Length -lt 40 -or $entry.why_it_was_suggested.Length -lt 30 -or $entry.why_it_was_implemented.Length -lt 30 -or $entry.how_it_was_implemented.Length -lt 40 -or $entry.bridge_value.Length -lt 30 -or $entry.adoption_guidance.Length -lt 30) {
        throw "implemented-intelligence-suggestions.json entries must contain detailed cross-CLI context."
    }
}

if ((Get-Content "codex-setup\agent-memory\shared\MEMORY.md" -Raw) -notmatch "Die 8 Intelligenz-Dimensionen") {
    throw "MEMORY.md must define the 8 intelligence dimensions."
}

if ((Get-Content "codex-setup\agent-memory\shared\MEMORY.md" -Raw) -notmatch "Cross-Tool-Lernen") {
    throw "MEMORY.md must describe cross-tool learning."
}

if ((Get-Content "codex-setup\agent-memory\shared\MEMORY.md" -Raw) -notmatch "Intelligenz-Vorschlag:") {
    throw "MEMORY.md must define the intelligence proposal format."
}

if ((Get-Content "codex-setup\agent-memory\shared\MEMORY.md" -Raw) -notmatch "Effizienzverluste, Wissensluecken, wiederkehrenden Muster") {
    throw "MEMORY.md must summarize the stronger self-observation categories."
}

$CloudCodeBridge = Get-Content "codex-setup\bridges\cloud-code-delta-bridge.json" -Raw | ConvertFrom-Json
if ($CloudCodeBridge.source_label -ne "Cloud Code") {
    throw "cloud-code-delta-bridge.json must identify Cloud Code as the source label."
}
if (-not $CloudCodeBridge.proposal_only) {
    throw "cloud-code-delta-bridge.json must be proposal-only."
}
if (-not $CloudCodeBridge.replacement_requires_confirmation) {
    throw "cloud-code-delta-bridge.json must require approval for replacements."
}
if ($CloudCodeBridge.trigger_phrases.Count -lt 3) {
    throw "cloud-code-delta-bridge.json must define multiple trigger phrases."
}
if ($CloudCodeBridge.trigger_phrases -notcontains "Starte bitte die Bruecke zu Cloud Code") {
    throw "cloud-code-delta-bridge.json must include the direct Cloud Code bridge trigger."
}
if (-not $CloudCodeBridge.include_implemented_intelligence_suggestions) {
    throw "cloud-code-delta-bridge.json must include implemented intelligence suggestions."
}
if (-not $CloudCodeBridge.exchange_ledgers.implemented_intelligence_suggestions.codex.repo_path) {
    throw "cloud-code-delta-bridge.json must expose implemented-suggestion ledger addresses."
}

$EnvironmentFixBridge = Get-Content "codex-setup\bridges\environment-fix-exchange-bridge.json" -Raw | ConvertFrom-Json
if ($EnvironmentFixBridge.source_label -ne "CLI Environment Fixes") {
    throw "environment-fix-exchange-bridge.json must describe CLI environment fixes."
}
if ($EnvironmentFixBridge.scope -ne "programming-environment-only") {
    throw "environment-fix-exchange-bridge.json must stay environment-only."
}
if ($EnvironmentFixBridge.trigger_phrases.Count -lt 3) {
    throw "environment-fix-exchange-bridge.json must define multiple trigger phrases."
}
if (-not $EnvironmentFixBridge.requires_full_context) {
    throw "environment-fix-exchange-bridge.json must require full context."
}

if (-not $EnvironmentFixBridge.requires_resilient_bugfix_fields) {
    throw "environment-fix-exchange-bridge.json must require resilient bugfix fields."
}

$IntelligenceBridge = Get-Content "codex-setup\bridges\intelligence-suggestion-exchange-bridge.json" -Raw | ConvertFrom-Json
if ($IntelligenceBridge.source_label -ne "Implemented Intelligence Suggestions") {
    throw "intelligence-suggestion-exchange-bridge.json must describe implemented intelligence suggestions."
}
if ($IntelligenceBridge.scope -ne "programming-environment-only") {
    throw "intelligence-suggestion-exchange-bridge.json must stay environment-only."
}
if (-not $IntelligenceBridge.requires_full_context) {
    throw "intelligence-suggestion-exchange-bridge.json must require full context."
}
if (-not $IntelligenceBridge.peer_registration_required) {
    throw "intelligence-suggestion-exchange-bridge.json must require peer registration."
}
if ($IntelligenceBridge.trigger_phrases.Count -lt 3) {
    throw "intelligence-suggestion-exchange-bridge.json must define multiple trigger phrases."
}

$GeminiDeltaState = Get-Content "codex-setup\state\gemini-delta-state.json" -Raw | ConvertFrom-Json
if ($GeminiDeltaState.scope -ne "gemini-environment-only") {
    throw "gemini-delta-state.json must track only Gemini environment deltas."
}
if (-not $GeminiDeltaState.replace_requires_confirmation) {
    throw "gemini-delta-state.json must require approval for replacements."
}
if ($GeminiDeltaState.tracked_paths -notcontains "Gemini-Setup/**") {
    throw "gemini-delta-state.json must track Gemini-Setup/**."
}

$GeminiBridge = Get-Content "codex-setup\bridges\gemini-cli-delta-bridge.json" -Raw | ConvertFrom-Json
if ($GeminiBridge.source_label -ne "Gemini CLI") {
    throw "gemini-cli-delta-bridge.json must identify Gemini CLI as the source label."
}
if (-not $GeminiBridge.proposal_only) {
    throw "gemini-cli-delta-bridge.json must be proposal-only."
}
if (-not $GeminiBridge.replacement_requires_confirmation) {
    throw "gemini-cli-delta-bridge.json must require approval for replacements."
}
if ($GeminiBridge.trigger_phrases -notcontains "Starte bitte die Bruecke zu Gemini CLI") {
    throw "gemini-cli-delta-bridge.json must include the direct Gemini bridge trigger."
}
if (-not $GeminiBridge.include_implemented_intelligence_suggestions) {
    throw "gemini-cli-delta-bridge.json must include implemented intelligence suggestions."
}
if (-not $GeminiBridge.exchange_ledgers.implemented_intelligence_suggestions.codex.repo_path) {
    throw "gemini-cli-delta-bridge.json must expose implemented-suggestion ledger addresses."
}

Get-ChildItem "codex-setup\skills\self-improve\references\agents" -Filter "*.md" | ForEach-Object {
    if ((Get-Content $_.FullName -Raw) -notmatch "Oberste Direktive") {
        throw "Missing Oberste Direktive marker in $($_.FullName)"
    }
}

$DirectiveText = ((node "codex-setup/scripts/whiteboard-bridge.mjs" print-directive --workspace $RepoRoot) -join "`n")
if ($DirectiveText -notmatch "Oberste Direktive") {
    throw "Failed to read Oberste Direktive via whiteboard bridge."
}

$OperationalScripts = Get-ChildItem "codex-setup/scripts" -Recurse -File |
    Where-Object { $_.Name -notin @("validate-codex-setup.sh", "validate-codex-setup.ps1") }

$Forbidden = $OperationalScripts |
    Select-String -Pattern "Pepsi1978/proggs|~/proggs/(claude-code-setup|\.claude)|/Users/frank/proggs/(claude-code-setup|\.claude)|C:\\Users\\barwa\\proggs|~/.claude/settings.json"

if ($Forbidden) {
    throw "Forbidden operational Claude/proggs reference found in codex-setup scripts."
}

Get-ChildItem "codex-setup/scripts" -Filter "*.ps1" | ForEach-Object {
    [void][scriptblock]::Create((Get-Content $_.FullName -Raw))
}

$TempToken = (node (Join-Path $RepoRoot "codex-setup\scripts\whiteboard-bridge.mjs") directive-token --workspace $TempWorkspace | Select-Object -Last 1).Trim()
node (Join-Path $RepoRoot "codex-setup\scripts\whiteboard-bridge.mjs") insert --workspace $TempWorkspace --directive-token $TempToken --section "Systemzustand" --entry "- **[2099-01-01 00:00] validator**: bridge mutation smoke test" | Out-Null

$TempMemory = Get-Content (Join-Path $TempWorkspace "codex-setup\agent-memory\shared\MEMORY.md") -Raw
if ($TempMemory -notmatch "bridge mutation smoke test") {
    throw "whiteboard-bridge did not write the smoke-test entry."
}

$TempIntegrity = (node (Join-Path $RepoRoot "codex-setup\scripts\whiteboard-bridge.mjs") check-integrity --workspace $TempWorkspace | Select-Object -Last 1).Trim()
if ($TempIntegrity -notmatch "^OK [0-9a-f]{64}$") {
    throw "whiteboard-bridge integrity check failed on the temporary workspace."
}

$null = node (Join-Path $RepoRoot "codex-setup\scripts\whiteboard-bridge.mjs") insert --workspace $TempWorkspace --directive-token "invalid-token" --section "Systemzustand" --entry "- **[2099-01-01 00:01] validator**: should fail" 2>$null
if ($LASTEXITCODE -eq 0) {
    throw "whiteboard-bridge accepted an invalid directive token."
}

$TempMemoryAfterFailure = Get-Content (Join-Path $TempWorkspace "codex-setup\agent-memory\shared\MEMORY.md") -Raw
if ($TempMemoryAfterFailure -match "should fail") {
    throw "whiteboard-bridge wrote data despite an invalid directive token."
}

Remove-Item -Recurse -Force $TempWorkspace -ErrorAction SilentlyContinue

if (Test-CodexMcpServer "openaiDeveloperDocs") {
    & pwsh -NoProfile -File "codex-setup\scripts\check-openai-docs-mcp.ps1" | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "openaiDeveloperDocs MCP smoke test failed."
    }
} else {
    Write-Host "Skipping openaiDeveloperDocs MCP smoke test: server not configured in this Codex runtime."
}

$ClaudeAuditJson = node "codex-setup/scripts/audit-claude-delta.mjs" --json
if ($LASTEXITCODE -ne 0) {
    throw "Claude delta audit failed."
}
$ClaudeAudit = ($ClaudeAuditJson -join "`n") | ConvertFrom-Json
if (-not $ClaudeAudit.latest_relevant_commit) {
    throw "Claude delta audit did not report a latest relevant commit."
}

$TempAuditState = Join-Path ([System.IO.Path]::GetTempPath()) ("claude-delta-state-" + [guid]::NewGuid().ToString() + ".json")
node "codex-setup/scripts/audit-claude-delta.mjs" mark-reviewed --state $TempAuditState --commit $ClaudeAudit.latest_relevant_commit | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Claude delta audit could not write a temporary reviewed state."
}
$TempAudit = Get-Content $TempAuditState -Raw | ConvertFrom-Json
if ($TempAudit.last_reviewed_commit -ne $ClaudeAudit.latest_relevant_commit) {
    throw "Claude delta audit stored the wrong reviewed commit."
}
Remove-Item $TempAuditState -ErrorAction SilentlyContinue

$TempFixLedger = Join-Path ([System.IO.Path]::GetTempPath()) ("environment-fixes-" + [guid]::NewGuid().ToString() + ".json")
node "codex-setup/scripts/register-environment-fix.mjs" add --state $TempFixLedger --id "validator-temp-fix" --category "validator" --summary "temporary validator entry" --context "This temporary validator entry exists only to prove that the environment-fix writer accepts full cross-CLI context." --symptom "Without this smoke test the validator would only know that the file exists, not that the writer can create a detailed entry end to end." --root-cause "The validator needs a write-path proof so schema changes cannot silently break the fix ledger tooling." --what "temporary write path" --why "prove the environment-fix ledger writer works" --exact-error "Validator smoke path would otherwise miss schema-level regressions." --why-chain "Why 1: a file can exist while the writer is still broken. Why 2: schema evolution can silently desynchronize the writer and validator. Why 3: without an end-to-end write test the regression would only surface later in a real session." --related-checks "Checked the write path, JSON serialization path, required-field enforcement, and post-write readback path because all of them belong to the same fix-ledger workflow." --wrong-pattern "Only validate that the ledger file exists or can be parsed." --right-pattern "Create a temporary ledger entry end to end and verify that the persisted id matches the expected smoke-test value." --avoidance-rule "If a fix ledger schema changes, then always run an end-to-end temporary write test instead of validating only file existence or JSON syntax." --resilience-summary "This smoke test is defensive because it checks the workflow instead of one file, update-resilient because it runs after future schema changes, and documented because the validator itself carries the expectation forward." --failure-review "Dependencies: writer and validator must stay aligned. Failure scenario: if the writer breaks the validator must fail loudly. State change: only a temporary file is touched. Race risk is negligible in the temp path. Compatibility remains intact because the smoke uses the same CLI. Platform effects are covered by PowerShell and Bash validators. Update resistance comes from rerunning the same smoke after future schema changes. Graceful degradation is preserved because failure only aborts validation, not the workspace." --verification "The validator reads the temporary ledger back immediately and checks that the written entry id matches the expected smoke-test value." --portability-notes "Other CLIs should keep the same smoke test so a fix ledger is validated as a workflow, not just as a static JSON file." | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Environment-fix registration script could not write a temporary entry."
}
$TempFixes = Get-Content $TempFixLedger -Raw | ConvertFrom-Json
if ($TempFixes.entries[0].id -ne "validator-temp-fix") {
    throw "Environment-fix registration stored the wrong entry."
}
Remove-Item $TempFixLedger -ErrorAction SilentlyContinue

$TempNewFixLedger = Join-Path ([System.IO.Path]::GetTempPath()) ("environment-fixes-guided-" + [guid]::NewGuid().ToString() + ".json")
node "codex-setup/scripts/new-environment-fix.mjs" --state $TempNewFixLedger --id "validator-guided-fix" --category "validator" --summary "guided validator entry" --context "This guided validator entry proves that the higher-level helper can forward a complete environment-fix payload without manual JSON editing." --symptom "Without this smoke test the guided helper could drift away from register-environment-fix.mjs while the core writer still looks healthy." --root-cause "The helper is a second entry path for the same ledger, so it needs its own end-to-end validation." --what "guided helper write path" --why "prove the guided environment-fix helper stays aligned with the core writer" --exact-error "The regression would otherwise stay invisible until someone relies on the guided helper in a real session." --why-chain "Why 1: a helper can break independently from the core writer. Why 2: it owns argument collection and forwarding. Why 3: without an end-to-end smoke test a drift would only appear during a real logging run." --related-checks "Checked the guided helper argument path, its forwarding logic, the core writer path, and the resulting persisted ledger entry because they form one workflow." --wrong-pattern "Only validate the core writer and assume the guided wrapper still forwards every required field correctly." --right-pattern "Run the guided helper end to end with a temporary ledger and verify that the expected entry id is persisted." --avoidance-rule "If a second helper writes into a validated ledger, then always smoke-test the helper workflow itself instead of validating only the underlying writer." --resilience-summary "This smoke test is defensive because it validates the higher-level entry path, update-resilient because it reruns after future helper changes, and safe because it touches only a temporary file." --failure-review "Dependencies: helper argument collection and the core writer must stay aligned. Failure scenario: if the helper drifts the validator fails loudly. State change: only a temporary file is written. Race risk is negligible. Backward compatibility remains because the helper only forwards data. Platform effects are covered by the shared Node path plus PowerShell and Bash wrappers. Update resistance comes from rerunning this test after future helper changes. Graceful degradation is preserved because helper failure aborts validation instead of corrupting the workspace." --verification "The validator reads the temporary ledger back immediately and checks that the guided helper persisted the expected entry id." --portability-notes "Other CLIs should smoke-test guided logging helpers end to end so wrapper drift does not silently weaken a shared ledger workflow." | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Guided environment-fix helper could not write a temporary entry."
}
$TempGuidedFixes = Get-Content $TempNewFixLedger -Raw | ConvertFrom-Json
if ($TempGuidedFixes.entries[0].id -ne "validator-guided-fix") {
    throw "Guided environment-fix helper stored the wrong entry."
}
Remove-Item $TempNewFixLedger -ErrorAction SilentlyContinue

$TempSuggestionLedger = Join-Path ([System.IO.Path]::GetTempPath()) ("implemented-intelligence-suggestions-" + [guid]::NewGuid().ToString() + ".json")
node "codex-setup/scripts/register-intelligence-suggestion.mjs" add --state $TempSuggestionLedger --id "validator-intelligence-suggestion" --summary "temporary intelligence suggestion entry" --proposal "💡 Intelligenz-Vorschlag: A durable helper should exist for rich ledger writes so future sessions do not retype long schemas manually." --context "This temporary validator entry proves that the implemented-intelligence-suggestion ledger can store a full cross-CLI explanation, not just a short headline." --suggested-because "The richer Codex ledgers are valuable only if their entries remain understandable for other CLIs without access to this session." --implemented-because "The validator needs an end-to-end write proof so the ledger schema and registration script cannot silently drift apart." --implementation "The validator writes a temporary intelligence-suggestion entry through the dedicated registration script and then reads the ledger back to confirm the persisted id." --bridge-value "Other CLIs can trust that the Codex implemented-suggestion ledger is validated as a workflow and not merely as a static JSON file." --adoption-guidance "If another CLI creates the same ledger, it should keep an end-to-end validator smoke test so schema drift is caught before a real session depends on the file." --portable-to "Cloud Code,Gemini CLI" --artifacts "codex-setup/scripts/register-intelligence-suggestion.mjs,codex-setup/state/implemented-intelligence-suggestions.json" | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Intelligence-suggestion registration script could not write a temporary entry."
}
$TempSuggestions = Get-Content $TempSuggestionLedger -Raw | ConvertFrom-Json
if ($TempSuggestions.entries[0].id -ne "validator-intelligence-suggestion") {
    throw "Intelligence-suggestion registration stored the wrong entry."
}
Remove-Item $TempSuggestionLedger -ErrorAction SilentlyContinue

& pwsh -NoProfile -File "codex-setup\scripts\audit-claude-delta.ps1" --json | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Claude delta audit PowerShell wrapper failed."
}

$GeminiAuditJson = node "codex-setup/scripts/audit-gemini-delta.mjs" --json
if ($LASTEXITCODE -ne 0) {
    throw "Gemini delta audit failed."
}
$GeminiAudit = ($GeminiAuditJson -join "`n") | ConvertFrom-Json
if (-not $GeminiAudit.latest_relevant_commit) {
    throw "Gemini delta audit did not report a latest relevant commit."
}

$TempGeminiState = Join-Path ([System.IO.Path]::GetTempPath()) ("gemini-delta-state-" + [guid]::NewGuid().ToString() + ".json")
node "codex-setup/scripts/audit-gemini-delta.mjs" mark-reviewed --state $TempGeminiState --commit $GeminiAudit.latest_relevant_commit | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Gemini delta audit could not write a temporary reviewed state."
}
$TempGeminiAudit = Get-Content $TempGeminiState -Raw | ConvertFrom-Json
if ($TempGeminiAudit.last_reviewed_commit -ne $GeminiAudit.latest_relevant_commit) {
    throw "Gemini delta audit stored the wrong reviewed commit."
}
Remove-Item $TempGeminiState -ErrorAction SilentlyContinue

& pwsh -NoProfile -File "codex-setup\scripts\audit-gemini-delta.ps1" --json | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Gemini delta audit PowerShell wrapper failed."
}

& node "codex-setup/scripts/check-code-search-mcp-client.mjs" | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "direct code-search MCP client self-test failed."
}

if (Test-CodexMcpServer "code-search") {
    & pwsh -NoProfile -File "codex-setup\scripts\code-search-mcp-client.ps1" tools | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "direct code-search MCP client failed."
    }

    & pwsh -NoProfile -File "codex-setup\scripts\check-code-search-health.ps1" --json | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "code-search MCP smoke test failed."
    }
} else {
    Write-Host "Skipping code-search MCP runtime smoke tests: server not configured in this Codex runtime."
}

Write-Host "codex-setup validation passed"
