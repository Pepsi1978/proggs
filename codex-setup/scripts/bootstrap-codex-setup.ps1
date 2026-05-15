param(
    [switch]$SkipValidate
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Resolve-Path (Join-Path $ScriptDir "..\..")

if (-not (Test-Path (Join-Path $RepoRoot "codex-setup"))) {
    throw "bootstrap-codex-setup.ps1 must run inside a workspace that contains codex-setup/."
}

Set-Location $RepoRoot
Write-Host "Codex bootstrap startet im Workspace $RepoRoot"

if (-not $SkipValidate) {
    Write-Host "[1/2] Synchronisiere Codex-Session-Start ..."
    $sync = Join-Path $ScriptDir "session-start-sync.ps1"
    if (Test-Path $sync) {
        & powershell -ExecutionPolicy Bypass -File $sync
    } else {
        Write-Host "  session-start-sync.ps1 nicht gefunden, übersprungen."
    }

    Write-Host "[2/2] Validiere codex-setup ..."
    & node (Join-Path $ScriptDir "validate-codex-setup.mjs")
} else {
    Write-Host "[1/1] Validierung übersprungen (--skip-validate)."
}

Write-Host "Bootstrap abgeschlossen."
Write-Host "Nächste direkte Brücken-Trigger:"
Write-Host "- Starte die Claude-Delta-Prüfung"
Write-Host "- Starte die Gemini-Delta-Prüfung"
Write-Host "Optionaler Gesamtüberblick:"
Write-Host "- powershell -ExecutionPolicy Bypass -File codex-setup/scripts/bootstrap-report.ps1"
