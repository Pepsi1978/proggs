# path-verify.ps1 -- Robuste PATH-Verifikation nach Shell-Updates
# Event: Manuell nach Shell-Updates ausfuehren ODER als Post-Update-Check
# Platform: Windows (PowerShell 7+)
#
# ZWECK: Prueft ob alle kritischen Tools nach einem Shell-Update noch im PATH sind.
# Repariert fehlende Eintraege automatisch wo moeglich.
# Erkennt verwaiste Python-Installationen (pip ohne python.exe).
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
        $script:missing += "$desc ($cmd) -- $hint"
        return $false
    }
    return $true
}

function Test-PathDir($dir, $desc) {
    if ((Test-Path $dir) -and ($env:PATH -notlike "*$dir*")) {
        $script:warnings += "${desc}: $dir existiert aber ist NICHT im PATH"
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
$null = Test-CommandExists "git" "Git" "winget install Git.Git"
$null = Test-CommandExists "node" "Node.js" "winget install OpenJS.NodeJS"
$null = Test-CommandExists "npm" "npm" "kommt mit Node.js"
$null = Test-CommandExists "python3" "Python 3" "winget install Python.Python.3"
$null = Test-CommandExists "rustc" "Rust Compiler" "rustup.rs"
$null = Test-CommandExists "cargo" "Cargo" "rustup.rs"
$null = Test-CommandExists "go" "Go" "winget install GoLang.Go"
$null = Test-CommandExists "claude" "Claude Code CLI" "npm install -g @anthropic-ai/claude-code"
$null = Test-CommandExists "bun" "Bun" "npm install -g bun"

# --- Dev-Analyse-Tools (Tier 1+2, installiert 2026-04-04) ---
$null = Test-CommandExists "bat" "bat (cat mit Highlighting)" "winget install sharkdp.bat"
$null = Test-CommandExists "fd" "fd (schnelles find)" "winget install sharkdp.fd"
$null = Test-CommandExists "fzf" "fzf (Fuzzy-Finder)" "winget install junegunn.fzf"
$null = Test-CommandExists "delta" "delta (Git-Diff-Viewer)" "winget install dandavison.delta"
$null = Test-CommandExists "tokei" "tokei (Code-Statistiken)" "cargo install tokei"
$null = Test-CommandExists "shellcheck" "shellcheck (Shell-Linter)" "winget install koalaman.shellcheck"
$null = Test-CommandExists "hyperfine" "hyperfine (Benchmarking)" "cargo install hyperfine"
$null = Test-CommandExists "dust" "dust (Disk-Usage)" "cargo install du-dust"
$null = Test-CommandExists "duf" "duf (Disk-Free)" "winget install muesli.duf"
$null = Test-CommandExists "lazygit" "lazygit (Git-TUI)" "winget install JesseDuffield.lazygit"
$null = Test-CommandExists "shfmt" "shfmt (Shell-Formatter)" "go install mvdan.cc/sh/v3/cmd/shfmt@latest"
$null = Test-CommandExists "trivy" "trivy (Security-Scanner)" "winget install AquaSecurity.Trivy"
$null = Test-CommandExists "glow" "glow (Markdown-Renderer)" "winget install charmbracelet.glow"

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
    $null = Test-PathDir $entry.Path $entry.Desc
}

# --- Environment-Variablen ---
$envMissing = @()
if (-not $env:JAVA_HOME) {
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    $javaHome = if ($javaCmd) { $javaCmd.Source | Split-Path | Split-Path } else { $null }
    if ($javaHome) { $envMissing += "JAVA_HOME nicht gesetzt -- sollte sein: $javaHome" }
}
if (-not $env:ANDROID_HOME -and (Test-Path "$env:LOCALAPPDATA\Android\Sdk")) {
    $envMissing += "ANDROID_HOME nicht gesetzt -- sollte sein: $env:LOCALAPPDATA\Android\Sdk"
}
if (-not $env:GOPATH) {
    $envMissing += "GOPATH nicht gesetzt -- sollte sein: $env:USERPROFILE\go"
}

# ===========================================================
# ORPHANED PYTHON INSTALLATIONS (Poka-Yoke Stufe 1+2)
# ===========================================================
# Root Cause (2026-04-03): When switching Python managers (official installer -> uv),
# the old installation may be partially removed, leaving pip.exe without python.exe.
# The pip launcher hardcodes the python.exe path and fails with:
#   "Fatal error in launcher: Unable to create process using python.exe"
# This check detects such orphans BEFORE the user hits the error.
$orphanedPython = @()

# Check 1: PATH entries with pip.exe but no python.exe in parent
$allPathDirs = ($env:PATH -split ';') | Where-Object { $_ -ne '' }
foreach ($dir in $allPathDirs) {
    $dirExists = try { $dir -and (Test-Path $dir -ErrorAction Stop) } catch { $false }
    if ($dirExists) {
        $hasPip = Test-Path (Join-Path $dir 'pip.exe')
        if ($hasPip -and $dir -match 'Python.*Scripts') {
            $parentDir = Split-Path $dir -Parent
            $hasPython = Test-Path (Join-Path $parentDir 'python.exe')
            if (-not $hasPython) {
                $orphanedPython += "ORPHANED pip: $dir hat pip.exe aber $parentDir hat KEIN python.exe"
                if ($Fix) {
                    $userPath = [Environment]::GetEnvironmentVariable("PATH", "User")
                    if ($userPath -like "*$dir*" -or $userPath -like "*$parentDir*") {
                        $cleanEntries = $userPath -split ';' | Where-Object {
                            $_ -ne $dir -and $_ -ne $parentDir -and $_ -ne ''
                        }
                        $cleanUserPath = $cleanEntries -join ';'
                        [Environment]::SetEnvironmentVariable("PATH", $cleanUserPath, "User")
                        $script:fixed += "Orphaned Python aus User-PATH entfernt: $dir"
                    } else {
                        $script:warnings += "Orphaned Python in SYSTEM-PATH (braucht Admin): $dir"
                    }
                }
            }
        }
    }
}

# Check 2: PATH entries pointing to non-existent tool directories
$ghostDirs = @()
foreach ($dir in $allPathDirs) {
    $dirMissing = try { $dir -and $dir -notmatch '^\$|^%' -and -not (Test-Path $dir -ErrorAction Stop) } catch { $false }
    if ($dirMissing) {
        if ($dir -match 'Python|node|npm|cargo|rustup|[Gg]o\\|gradle|Kotlin|Android|bun') {
            $ghostDirs += "GHOST-PATH: $dir existiert nicht mehr"
        }
    }
}

if ($orphanedPython.Count -gt 0) { $warnings += $orphanedPython }
if ($ghostDirs.Count -gt 0) { $warnings += $ghostDirs }

# --- Report ---
Write-Host ""
$totalIssues = $missing.Count + $warnings.Count + $envMissing.Count

if ($totalIssues -eq 0) {
    Write-Host "PATH-Verifikation: Alle Checks bestanden." -ForegroundColor Green
} else {
    Write-Host "PATH-Verifikation: $totalIssues Problem(e) gefunden:" -ForegroundColor Yellow
    Write-Host ""

    if ($missing.Count -gt 0) {
        Write-Host "FEHLENDE TOOLS:" -ForegroundColor Red
        $missing | ForEach-Object { Write-Host "  - $_" }
        Write-Host ""
    }
    if ($warnings.Count -gt 0) {
        Write-Host "PATH-WARNUNGEN:" -ForegroundColor Yellow
        $warnings | ForEach-Object { Write-Host "  - $_" }
        Write-Host ""
    }
    if ($envMissing.Count -gt 0) {
        Write-Host "FEHLENDE ENV-VARIABLEN:" -ForegroundColor Yellow
        $envMissing | ForEach-Object { Write-Host "  - $_" }
        Write-Host ""
    }
    if ($fixed.Count -gt 0) {
        Write-Host "AUTOMATISCH REPARIERT:" -ForegroundColor Green
        $fixed | ForEach-Object { Write-Host "  - $_" }
        Write-Host ""
    }
}

exit 0
