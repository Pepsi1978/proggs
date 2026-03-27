param(
    [switch]$SkipInstall,
    [switch]$SkipValidate
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = (Resolve-Path (Join-Path $ScriptDir "..\..")).Path

if (-not (Test-Path (Join-Path $RepoRoot "codex-setup"))) {
    throw "bootstrap-codex-setup.ps1 must run inside a Codex workspace that contains codex-setup/."
}

Set-Location $RepoRoot

Write-Host "Codex bootstrap startet im Workspace $RepoRoot"

if (-not $SkipInstall) {
    Write-Host "[1/3] Installiere self-improve lokal nach ~/.codex/skills ..."
    & (Join-Path $ScriptDir "install-self-improve.ps1")
    Write-Host "[2/3] Konfiguriere Git-Hooks fuer den lokalen Pre-Commit-Scanner ..."
    & (Join-Path $ScriptDir "install-git-hooks.ps1")
}
else {
    Write-Host "[1/3] self-improve-Installation uebersprungen (--SkipInstall)."
    Write-Host "[2/3] Git-Hook-Installation uebersprungen (--SkipInstall)."
}

if (-not $SkipValidate) {
    Write-Host "[3/3] Validiere codex-setup ..."
    & (Join-Path $ScriptDir "validate-codex-setup.ps1")
}
else {
    Write-Host "[3/3] Validierung uebersprungen (--SkipValidate)."
}

Write-Host "Bootstrap abgeschlossen."
Write-Host "Naechste direkte Bruecken-Trigger:"
Write-Host "- Starte bitte die Bruecke zu Claude Code"
Write-Host "- Starte bitte die Bruecke zu Gemini CLI"
Write-Host "Optionaler Gesamtueberblick:"
Write-Host "- pwsh -NoProfile -File codex-setup/scripts/bootstrap-report.ps1"
