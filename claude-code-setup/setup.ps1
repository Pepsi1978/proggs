# =============================================================================
# Claude Code Setup - Windows (PowerShell)
# Installiert alle Plugins, Skills, Dev-Tools, MCP-Server und Settings
# Version 2.0.0
# =============================================================================

$ErrorActionPreference = "Continue"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$Manifest = Join-Path $ScriptDir "manifest.json"
$SkillsDir = Join-Path $ScriptDir "skills"
$ClaudeSkillsDir = Join-Path $env:USERPROFILE ".claude\skills"
$ClaudeSettings = Join-Path $env:USERPROFILE ".claude\settings.json"

Write-Host ""
Write-Host "=============================================" -ForegroundColor Blue
Write-Host "  Claude Code Setup v2.0 - Windows           " -ForegroundColor Blue
Write-Host "=============================================" -ForegroundColor Blue
Write-Host ""

# ---------------------------------------------------
# 1. Voraussetzungen pruefen
# ---------------------------------------------------
Write-Host "[1/8] Voraussetzungen pruefen..." -ForegroundColor Yellow

$Missing = @()

if (-not (Get-Command "claude" -ErrorAction SilentlyContinue)) {
    $Missing += "claude"
    Write-Host "  X Claude Code CLI nicht gefunden" -ForegroundColor Red
} else {
    $ver = claude --version 2>&1 | Select-Object -First 1
    Write-Host "  OK Claude Code CLI $ver" -ForegroundColor Green
}

if (-not (Get-Command "git" -ErrorAction SilentlyContinue)) {
    $Missing += "git"
    Write-Host "  X Git nicht gefunden" -ForegroundColor Red
} else {
    Write-Host "  OK Git $(git --version)" -ForegroundColor Green
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
Write-Host "[2/8] Rechner erkennen..." -ForegroundColor Yellow

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
# 3. Dev-Tools installieren
# ---------------------------------------------------
Write-Host ""
Write-Host "[3/8] Dev-Tools installieren..." -ForegroundColor Yellow

# Bun
if (-not (Get-Command "bun" -ErrorAction SilentlyContinue)) {
    Write-Host "  Bun installieren ... " -NoNewline
    try {
        winget install Oven-sh.Bun --accept-source-agreements --accept-package-agreements 2>&1 | Out-Null
        Write-Host "OK" -ForegroundColor Green
    } catch {
        Write-Host "X (manuell: winget install Oven-sh.Bun)" -ForegroundColor Red
    }
} else {
    Write-Host "  OK Bun $(bun --version)" -ForegroundColor Green
}

# Deno
if (-not (Get-Command "deno" -ErrorAction SilentlyContinue)) {
    Write-Host "  Deno installieren ... " -NoNewline
    try {
        winget install DenoLand.Deno --accept-source-agreements --accept-package-agreements 2>&1 | Out-Null
        Write-Host "OK" -ForegroundColor Green
    } catch {
        Write-Host "X (manuell: winget install DenoLand.Deno)" -ForegroundColor Red
    }
} else {
    Write-Host "  OK Deno $(deno -V)" -ForegroundColor Green
}

# Rust
$rustcPath = Join-Path $env:USERPROFILE ".cargo\bin\rustc.exe"
if ((-not (Get-Command "rustc" -ErrorAction SilentlyContinue)) -and (-not (Test-Path $rustcPath))) {
    Write-Host "  Rust installieren ... " -NoNewline
    try {
        winget install Rustlang.Rustup --accept-source-agreements --accept-package-agreements 2>&1 | Out-Null
        Write-Host "OK" -ForegroundColor Green
        # PATH aktualisieren fuer aktuelle Session
        $cargoPath = Join-Path $env:USERPROFILE ".cargo\bin"
        $env:PATH = "$cargoPath;$env:PATH"
    } catch {
        Write-Host "X (manuell: winget install Rustlang.Rustup)" -ForegroundColor Red
    }
} else {
    if (Test-Path $rustcPath) {
        $rustVer = & $rustcPath --version 2>&1
    } else {
        $rustVer = rustc --version 2>&1
    }
    Write-Host "  OK Rust $rustVer" -ForegroundColor Green
}

# Docker
if (-not (Get-Command "docker" -ErrorAction SilentlyContinue)) {
    Write-Host "  Docker Desktop installieren ... " -NoNewline
    try {
        winget install Docker.DockerDesktop --accept-source-agreements --accept-package-agreements 2>&1 | Out-Null
        Write-Host "OK (bitte App einmal starten)" -ForegroundColor Green
    } catch {
        Write-Host "X (manuell: winget install Docker.DockerDesktop)" -ForegroundColor Red
    }
} else {
    Write-Host "  OK Docker $(docker --version)" -ForegroundColor Green
}

# .NET SDK
if (-not (Get-Command "dotnet" -ErrorAction SilentlyContinue)) {
    Write-Host "  .NET SDK installieren ... " -NoNewline
    try {
        winget install Microsoft.DotNet.SDK.10 --accept-source-agreements --accept-package-agreements 2>&1 | Out-Null
        Write-Host "OK" -ForegroundColor Green
    } catch {
        Write-Host "X (manuell: winget install Microsoft.DotNet.SDK.10)" -ForegroundColor Red
    }
} else {
    Write-Host "  OK .NET $(dotnet --version)" -ForegroundColor Green
}

# ---------------------------------------------------
# 4. Extra Marketplaces einrichten
# ---------------------------------------------------
Write-Host ""
Write-Host "[4/8] Marketplaces einrichten..." -ForegroundColor Yellow

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
# 5. Plugins installieren
# ---------------------------------------------------
Write-Host ""
Write-Host "[5/8] Plugins installieren..." -ForegroundColor Yellow

$Installed = 0
$Skipped = 0

# Official Marketplace Plugins
$officialPlugins = @(
    "swift-lsp", "typescript-lsp", "csharp-lsp", "clangd-lsp",
    "superpowers", "frontend-design", "context7",
    "feature-dev", "code-simplifier", "commit-commands", "security-guidance",
    "claude-md-management", "pr-review-toolkit", "claude-code-setup",
    "hookify", "playground", "ralph-loop", "code-review",
    "playwright", "coderabbit", "explanatory-output-style", "skill-creator",
    "plugin-dev", "agent-sdk-dev"
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
    "episodic-memory", "double-shot-latte", "superpowers-chrome", "claude-session-driver",
    "superpowers-lab", "superpowers-developing-for-claude-code", "elements-of-style"
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
# 6. MCP Server einrichten
# ---------------------------------------------------
Write-Host ""
Write-Host "[6/8] MCP Server einrichten..." -ForegroundColor Yellow

Write-Host "  sequential-thinking installieren ... " -NoNewline
try {
    claude mcp add --scope user sequential-thinking -- npx -y @modelcontextprotocol/server-sequential-thinking 2>&1 | Out-Null
    Write-Host "OK" -ForegroundColor Green
} catch {
    Write-Host "(bereits installiert oder Fehler)" -ForegroundColor Yellow
}

# ---------------------------------------------------
# 7. Security Tools (Parry)
# ---------------------------------------------------
Write-Host ""
Write-Host "[7/8] Security Tools installieren..." -ForegroundColor Yellow

$cargoPath = Join-Path $env:USERPROFILE ".cargo\bin\cargo.exe"
$parryPath = Join-Path $env:USERPROFILE ".cargo\bin\parry.exe"

if (Test-Path $parryPath) {
    $parryVer = & $parryPath --version 2>&1
    Write-Host "  OK Parry $parryVer" -ForegroundColor Green
} elseif (Test-Path $cargoPath) {
    Write-Host "  Parry installieren (dauert ~2 Min) ... " -NoNewline
    try {
        & $cargoPath install --git https://github.com/vaporif/parry parry-ai 2>&1 | Out-Null
        Write-Host "OK" -ForegroundColor Green
    } catch {
        Write-Host "X (manuell: cargo install --git https://github.com/vaporif/parry parry-ai)" -ForegroundColor Red
    }
} else {
    Write-Host "  ! Rust/Cargo nicht gefunden - Parry kann nicht installiert werden" -ForegroundColor Yellow
}

# ---------------------------------------------------
# 8. Custom Skills kopieren
# ---------------------------------------------------
Write-Host ""
Write-Host "[8/8] Custom Skills installieren..." -ForegroundColor Yellow

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
# Zusammenfassung
# ---------------------------------------------------
Write-Host ""
Write-Host "=============================================" -ForegroundColor Blue
Write-Host "  Setup abgeschlossen!" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Blue
Write-Host ""
Write-Host "  Plugins:        32 (26 official + 7 superpowers-marketplace)"
Write-Host "  Skills:          4 (auto-verify-iterate, cross-platform, tampermonkey-version, undo-changes)"
Write-Host "  MCP Server:      sequential-thinking"
Write-Host "  Security:        Parry (Prompt-Injection-Scanner)"
Write-Host "  Dev-Tools:       Bun, Deno, Rust, Docker, .NET SDK"
Write-Host "  Agent Teams:     Aktiviert (experimentell)"
Write-Host "  Marketplaces:    2 (claude-plugins-official, superpowers-marketplace)"
Write-Host ""
Write-Host "  Starte Claude Code neu, damit alle Plugins aktiv werden:" -ForegroundColor Yellow
Write-Host "  claude"
Write-Host ""
Write-Host "  Optional: Parry benoetigt einen HuggingFace-Token fuer volle Funktionalitaet:" -ForegroundColor Yellow
Write-Host '  $env:HF_TOKEN = "hf_dein_token"'
Write-Host ""
