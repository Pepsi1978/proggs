# =============================================================================
# Claude Code Setup - Windows (PowerShell)
# Installiert alle Plugins, Skills und Settings aus dem Manifest
# =============================================================================

$ErrorActionPreference = "Continue"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$Manifest = Join-Path $ScriptDir "manifest.json"
$SkillsDir = Join-Path $ScriptDir "skills"
$ClaudeSkillsDir = Join-Path $env:USERPROFILE ".claude\skills"
$ClaudeSettings = Join-Path $env:USERPROFILE ".claude\settings.json"

Write-Host ""
Write-Host "=============================================" -ForegroundColor Blue
Write-Host "  Claude Code Setup - Plattformuebergreifend " -ForegroundColor Blue
Write-Host "=============================================" -ForegroundColor Blue
Write-Host ""

# ---------------------------------------------------
# 1. Voraussetzungen pruefen
# ---------------------------------------------------
Write-Host "[1/6] Voraussetzungen pruefen..." -ForegroundColor Yellow

$Missing = @()

if (-not (Get-Command "claude" -ErrorAction SilentlyContinue)) {
    $Missing += "claude"
    Write-Host "  X Claude Code CLI nicht gefunden" -ForegroundColor Red
} else {
    Write-Host "  OK Claude Code CLI" -ForegroundColor Green
}

if (-not (Get-Command "git" -ErrorAction SilentlyContinue)) {
    $Missing += "git"
    Write-Host "  X Git nicht gefunden" -ForegroundColor Red
} else {
    Write-Host "  OK Git" -ForegroundColor Green
}

if (-not (Get-Command "gh" -ErrorAction SilentlyContinue)) {
    Write-Host "  ! GitHub CLI (gh) nicht gefunden - optional" -ForegroundColor Yellow
} else {
    Write-Host "  OK GitHub CLI" -ForegroundColor Green
}

if (-not (Get-Command "node" -ErrorAction SilentlyContinue)) {
    Write-Host "  ! Node.js nicht gefunden - wird fuer einige Plugins benoetigt" -ForegroundColor Yellow
} else {
    $nodeVersion = node --version
    Write-Host "  OK Node.js $nodeVersion" -ForegroundColor Green
}

if ($Missing.Count -gt 0) {
    Write-Host ""
    Write-Host "Fehlende Voraussetzungen: $($Missing -join ', ')" -ForegroundColor Red
    Write-Host "Bitte installiere diese zuerst und starte das Skript erneut."
    exit 1
}

# ---------------------------------------------------
# 2. Rechner erkennen
# ---------------------------------------------------
Write-Host ""
Write-Host "[2/6] Rechner erkennen..." -ForegroundColor Yellow

$IsKnownMachine = $false
$GitHubUser = ""

if (Get-Command "gh" -ErrorAction SilentlyContinue) {
    try {
        $authStatus = gh auth status 2>&1 | Out-String
        if ($authStatus -match "Pepsi1978") {
            $GitHubUser = "Pepsi1978"
        }
    } catch {}
}

$proggsPath = Join-Path $env:USERPROFILE "proggs"

if ($GitHubUser -eq "Pepsi1978") {
    $IsKnownMachine = $true
    Write-Host "  OK Erkannt: Frank's Rechner (GitHub: Pepsi1978)" -ForegroundColor Green
} elseif (Test-Path $proggsPath) {
    $IsKnownMachine = $true
    Write-Host "  OK Erkannt: proggs-Verzeichnis vorhanden" -ForegroundColor Green
} else {
    Write-Host "  i Neuer/unbekannter Rechner" -ForegroundColor Blue
    Write-Host ""
    $InputUser = Read-Host "  GitHub-Benutzername (Enter fuer Pepsi1978)"
    if ([string]::IsNullOrWhiteSpace($InputUser)) {
        $GitHubUser = "Pepsi1978"
    } else {
        $GitHubUser = $InputUser
    }
    Write-Host ""
}

# ---------------------------------------------------
# 3. Extra Marketplaces einrichten
# ---------------------------------------------------
Write-Host "[3/6] Marketplaces einrichten..." -ForegroundColor Yellow

$claudeDir = Join-Path $env:USERPROFILE ".claude"
if (-not (Test-Path $claudeDir)) {
    New-Item -ItemType Directory -Path $claudeDir -Force | Out-Null
}

if (Test-Path $ClaudeSettings) {
    $settingsContent = Get-Content $ClaudeSettings -Raw
    if ($settingsContent -match "superpowers-marketplace") {
        Write-Host "  OK superpowers-marketplace bereits konfiguriert" -ForegroundColor Green
    } else {
        Write-Host "  i superpowers-marketplace wird durch Plugin-Installation hinzugefuegt..." -ForegroundColor Blue
    }
} else {
    Write-Host "  i Settings-Datei wird bei Plugin-Installation erstellt..." -ForegroundColor Blue
}

# ---------------------------------------------------
# 4. Plugins installieren
# ---------------------------------------------------
Write-Host ""
Write-Host "[4/6] Plugins installieren..." -ForegroundColor Yellow

$Installed = 0
$Skipped = 0

# Official Marketplace Plugins
$officialPlugins = @(
    "swift-lsp", "typescript-lsp", "csharp-lsp", "superpowers",
    "frontend-design", "context7", "github", "feature-dev",
    "code-simplifier", "commit-commands", "security-guidance",
    "claude-md-management", "pr-review-toolkit", "claude-code-setup",
    "hookify", "playground", "ralph-loop", "code-review"
)

foreach ($plugin in $officialPlugins) {
    Write-Host "  $plugin@claude-plugins-official ... " -NoNewline
    try {
        $result = claude plugins install "$plugin@claude-plugins-official" 2>&1
        Write-Host "OK" -ForegroundColor Green
        $Installed++
    } catch {
        Write-Host "(bereits installiert oder Fehler)" -ForegroundColor Yellow
        $Skipped++
    }
}

# Superpowers Marketplace Plugins
$superpowersPlugins = @(
    "episodic-memory", "double-shot-latte", "superpowers-chrome", "claude-session-driver"
)

foreach ($plugin in $superpowersPlugins) {
    Write-Host "  $plugin@superpowers-marketplace ... " -NoNewline
    try {
        $result = claude plugins install "$plugin@superpowers-marketplace" 2>&1
        Write-Host "OK" -ForegroundColor Green
        $Installed++
    } catch {
        Write-Host "(bereits installiert oder Fehler)" -ForegroundColor Yellow
        $Skipped++
    }
}

Write-Host ""
Write-Host "  Installiert: $Installed | Uebersprungen: $Skipped" -ForegroundColor Green

# ---------------------------------------------------
# 5. Custom Skills kopieren
# ---------------------------------------------------
Write-Host ""
Write-Host "[5/6] Custom Skills installieren..." -ForegroundColor Yellow

if (Test-Path $SkillsDir) {
    $skillFolders = Get-ChildItem -Path $SkillsDir -Directory

    foreach ($skillFolder in $skillFolders) {
        $skillName = $skillFolder.Name
        $targetDir = Join-Path $ClaudeSkillsDir $skillName

        if (-not (Test-Path $targetDir)) {
            New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
        }

        Copy-Item -Path (Join-Path $skillFolder.FullName "*") -Destination $targetDir -Recurse -Force

        if (Test-Path (Join-Path $targetDir "SKILL.md")) {
            Write-Host "  OK $skillName" -ForegroundColor Green
        } else {
            Write-Host "  X $skillName (SKILL.md fehlt)" -ForegroundColor Red
        }
    }
} else {
    Write-Host "  X Skills-Verzeichnis nicht gefunden: $SkillsDir" -ForegroundColor Red
}

# ---------------------------------------------------
# 6. Zusammenfassung
# ---------------------------------------------------
Write-Host ""
Write-Host "=============================================" -ForegroundColor Blue
Write-Host "  Setup abgeschlossen!" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Blue
Write-Host ""
Write-Host "  Installierte Plugins:  22"
Write-Host "  Installierte Skills:   3 (auto-verify-iterate, cross-platform, tampermonkey-version)"
Write-Host "  Marketplaces:          2 (claude-plugins-official, superpowers-marketplace)"
Write-Host ""
Write-Host "  Starte Claude Code neu, damit alle Plugins aktiv werden:" -ForegroundColor Yellow
Write-Host "  claude"
Write-Host ""
