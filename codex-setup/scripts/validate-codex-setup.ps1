$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir "..\..")).Path
Set-Location $RepoRoot

$RequiredFiles = @(
    "AGENTS.md",
    "codex-setup\rules\global.md",
    "codex-setup\agent-memory\shared\MEMORY.md",
    "codex-setup\scripts\whiteboard-bridge.mjs",
    "codex-setup\scripts\whiteboard-insert.sh",
    "codex-setup\scripts\whiteboard-insert.ps1",
    "codex-setup\scripts\writeback-enforcer.sh",
    "codex-setup\scripts\writeback-enforcer.ps1",
    "codex-setup\scripts\install-self-improve.sh",
    "codex-setup\scripts\install-self-improve.ps1",
    "codex-setup\scripts\code-search-mcp-client.mjs",
    "codex-setup\scripts\code-search-mcp-client.sh",
    "codex-setup\scripts\code-search-mcp-client.ps1",
    "codex-setup\scripts\check-code-search-health.mjs",
    "codex-setup\scripts\check-code-search-health.sh",
    "codex-setup\scripts\check-code-search-health.ps1",
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
    "codex-setup\agent-memory\shared\MEMORY.md",
    "codex-setup\skills\self-improve\SKILL.md",
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

& pwsh -NoProfile -File "codex-setup\scripts\check-openai-docs-mcp.ps1" | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "openaiDeveloperDocs MCP smoke test failed."
}

& pwsh -NoProfile -File "codex-setup\scripts\code-search-mcp-client.ps1" tools | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "direct code-search MCP client failed."
}

& pwsh -NoProfile -File "codex-setup\scripts\check-code-search-health.ps1" --json | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "code-search MCP smoke test failed."
}

Write-Host "codex-setup validation passed"
