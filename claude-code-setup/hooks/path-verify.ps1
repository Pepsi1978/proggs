# path-verify.ps1 — Robuste PATH-Verifikation nach Shell-Updates
# Event: Manuell nach Shell-Updates ausfuehren ODER als Post-Update-Check
# Platform: Windows (PowerShell 7+)
#
# ZWECK: Prueft ob alle kritischen Tools nach einem Shell-Update noch im PATH sind.
# Repariert fehlende Eintraege automatisch wo moeglich.
#
# ANWENDUNG:
#   pwsh ~/.claude/hooks/path-verify.ps1          # Pruefung
#   pwsh ~/.claude/hooks/path-verify.ps1 -Fix     # Pruefung + Reparatur

param([switch]$Fix)

$missing = @()
$warnings = @()
$fixed = @()

function Test-CommandExists($cmd, $desc, $hint) {
    if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
        $script:missing += "$desc ($cmd) — $hint"
        return $false
    }
    return $true
}

function Test-PathDir($dir, $desc) {
    if ((Test-Path $dir) -and ($env:PATH -notlike "*$dir*")) {
        $script:warnings += "$desc`: $dir existiert aber ist NICHT im PATH"
        if ($Fix) {
            # User PATH persistent reparieren
            $userPath = [Environment]::GetEnvironmentVariable("PATH", "User")
            if ($userPath -notlike "*$dir*") {
                [Environment]::SetEnvironmentVariable("PATH", "$dir;$userPath", "User")
                $env:PATH = "$dir;$env:PATH"
                $script:fixed += "$dir zum User-PATH hinzugefuegt (persistent)"
            }
        }
        return $false
    }
    return $true
}

Write-Host "=== PATH-Verifikation (Windows) ==="
Write-Host ""

# --- Plattform-uebergreifend ---
Test-CommandExists "git" "Git" "winget install Git.Git"
Test-CommandExists "node" "Node.js" "winget install OpenJS.NodeJS"
Test-CommandExists "npm" "npm" "kommt mit Node.js"
Test-CommandExists "python3" "Python 3" "winget install Python.Python.3"
Test-CommandExists "rustc" "Rust Compiler" "rustup.rs"
Test-CommandExists "cargo" "Cargo" "rustup.rs"
Test-CommandExists "go" "Go" "winget install GoLang.Go"
Test-CommandExists "claude" "Claude Code CLI" "npm install -g @anthropic-ai/claude-code"
Test-CommandExists "bun" "Bun" "npm install -g bun"

# --- Windows-spezifische Pfade (REFERENZLISTE aus CLAUDE.md) ---
$requiredPaths = @(
    @{ Path = "$env:USERPROFILE\bin";                                    Desc = "Python/python3 Wrapper" },
    @{ Path = "$env:USERPROFILE\.local\bin";                             Desc = "uvx, pipx" },
    @{ Path = "$env:USERPROFILE\.bun\bin";                               Desc = "Bun" },
    @{ Path = "$env:USERPROFILE\.cargo\bin";                             Desc = "Rust/Cargo" },
    @{ Path = "$env:USERPROFILE\AppData\Roaming\npm";                    Desc = "npm global" },
    @{ Path = "$env:USERPROFILE\go\bin";                                 Desc = "Go bin" },
    @{ Path = "$env:LOCALAPPDATA\Android\Sdk\platform-tools";            Desc = "ADB/Fastboot" },
    @{ Path = "$env:LOCALAPPDATA\Android\Sdk\cmdline-tools\latest\bin";  Desc = "Android SDK" },
    @{ Path = "$env:LOCALAPPDATA\Android\Sdk\emulator";                  Desc = "Android Emulator" }
)

# Gradle und Kotlin: dynamisch finden (Versionen aendern sich)
$gradleDir = Get-ChildItem "C:\Gradle\gradle-*\bin" -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
if ($gradleDir) { $requiredPaths += @{ Path = $gradleDir.FullName; Desc = "Gradle" } }

$kotlinDir = "C:\Kotlin\kotlinc\bin"
if (Test-Path $kotlinDir) { $requiredPaths += @{ Path = $kotlinDir; Desc = "Kotlin" } }

foreach ($entry in $requiredPaths) {
    Test-PathDir $entry.Path $entry.Desc
}

# --- Environment-Variablen ---
$envMissing = @()
if (-not $env:JAVA_HOME) {
    $javaHome = (Get-Command java -ErrorAction SilentlyContinue)?.Source | Split-Path | Split-Path
    if ($javaHome) { $envMissing += "JAVA_HOME nicht gesetzt — sollte sein: $javaHome" }
}
if (-not $env:ANDROID_HOME -and (Test-Path "$env:LOCALAPPDATA\Android\Sdk")) {
    $envMissing += "ANDROID_HOME nicht gesetzt — sollte sein: $env:LOCALAPPDATA\Android\Sdk"
}
if (-not $env:GOPATH) {
    $envMissing += "GOPATH nicht gesetzt — sollte sein: $env:USERPROFILE\go"
}

# --- Report ---
Write-Host ""
$totalIssues = $missing.Count + $warnings.Count + $envMissing.Count

if ($totalIssues -eq 0) {
    Write-Host "PATH-Verifikation: Alle Checks bestanden."
} else {
    Write-Host "PATH-Verifikation: $totalIssues Problem(e) gefunden:"
    Write-Host ""

    if ($missing.Count -gt 0) {
        Write-Host "FEHLENDE TOOLS:"
        $missing | ForEach-Object { Write-Host "  - $_" }
        Write-Host ""
    }
    if ($warnings.Count -gt 0) {
        Write-Host "PATH-WARNUNGEN:"
        $warnings | ForEach-Object { Write-Host "  - $_" }
        Write-Host ""
    }
    if ($envMissing.Count -gt 0) {
        Write-Host "FEHLENDE ENV-VARIABLEN:"
        $envMissing | ForEach-Object { Write-Host "  - $_" }
        Write-Host ""
    }
    if ($fixed.Count -gt 0) {
        Write-Host "AUTOMATISCH REPARIERT:"
        $fixed | ForEach-Object { Write-Host "  - $_" }
        Write-Host ""
    }
}

exit 0
