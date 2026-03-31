# Cascade Format Hook: Lint-Pruefung nach jedem Edit/Write auf Code-Dateien
# Quelle: Superintelligenz Finding 3 — Cascade Hooks Pattern (Windsurf-inspiriert)
# Event: PostToolUse (Edit|Write)
# Zweck: Fuehrt Linting/Formatting auf geaenderten Code-Dateien aus und meldet Fehler.
#
# UNTERSCHIED zu auto-format.ps1:
#   auto-format.ps1 = Formatierung (Einrueckung, Whitespace) — ASYNC, stil Fehler ignoriert
#   cascade-format.ps1 = LINT-Pruefung (Syntaxfehler, fehlende Imports) — SYNC, Fehler werden gemeldet
#
# ROBUSTNESS: Gesamtes Skript in try/catch. Jeder Fehler → exit 0 (nie blockieren).

$ErrorActionPreference = 'SilentlyContinue'
try { . "$PSScriptRoot/hook-log.ps1" } catch { }

try {
    $hookInput = [Console]::In.ReadToEnd()
    $data = $hookInput | ConvertFrom-Json -ErrorAction Stop
} catch { exit 0 }

if (-not $data) { exit 0 }

$filePath = $data.tool_input.file_path
if (-not $filePath -or -not (Test-Path $filePath)) { exit 0 }

$ext = [System.IO.Path]::GetExtension($filePath).TrimStart('.').ToLower()
$lintResult = $null
$lintCmd = ""
$lintFailed = $false

# Tampermonkey .user.js: ueberspringen — nutzt eslint per CLAUDE.md
if ($filePath -match '\.user\.js$') { exit 0 }

switch ($ext) {
    # Kotlin: ktlint wenn installiert, sonst skip (kein --relative — entfernt in ktlint v1.x)
    { $_ -in 'kt', 'kts' } {
        $ktlint = Get-Command ktlint -ErrorAction SilentlyContinue
        if ($ktlint) {
            $lintCmd = "ktlint"
            $lintResult = & ktlint "$filePath" 2>&1
            if ($LASTEXITCODE -ne 0) { $lintFailed = $true }
        }
    }
    # JavaScript/TypeScript: biome (installiert!) → schneller als eslint
    { $_ -in 'js', 'jsx', 'ts', 'tsx', 'mjs', 'mts' } {
        $biome = Get-Command biome -ErrorAction SilentlyContinue
        if ($biome) {
            $lintCmd = "biome lint"
            $lintResult = & biome lint "$filePath" 2>&1
            if ($LASTEXITCODE -ne 0) { $lintFailed = $true }
        }
    }
    # JSON: Validierung (doppelt mit poka-yoke, aber hier als Lint — detailliertere Meldung)
    'json' {
        try {
            $content = Get-Content -Path $filePath -Raw -Encoding UTF8
            $null = $content | ConvertFrom-Json -ErrorAction Stop
        } catch {
            $lintCmd = "json-validate"
            $lintResult = $_.Exception.Message
            $lintFailed = $true
        }
    }
    # Python: py_compile
    'py' {
        $python = Get-Command python3 -ErrorAction SilentlyContinue
        if (-not $python) { $python = Get-Command python -ErrorAction SilentlyContinue }
        if ($python) {
            $lintCmd = "py_compile"
            $lintResult = & $python.Source -m py_compile "$filePath" 2>&1
            if ($LASTEXITCODE -ne 0) { $lintFailed = $true }
        }
    }
    # Go: go vet
    { $_ -eq 'go' } {
        $go = Get-Command go -ErrorAction SilentlyContinue
        if ($go) {
            $dir = [System.IO.Path]::GetDirectoryName($filePath)
            $lintCmd = "go vet"
            $lintResult = & go vet "$dir/..." 2>&1
            if ($LASTEXITCODE -ne 0) { $lintFailed = $true }
        }
    }
    # Rust: cargo check (nur wenn Cargo.toml existiert)
    'rs' {
        $dir = [System.IO.Path]::GetDirectoryName($filePath)
        $cargo = Get-ChildItem -Path $dir -Filter "Cargo.toml" -Recurse -Depth 3 -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($cargo) {
            $lintCmd = "cargo check"
            $lintResult = & cargo check --manifest-path $cargo.FullName 2>&1 | Select-Object -First 10
            if ($LASTEXITCODE -ne 0) { $lintFailed = $true }
        }
    }
}

if ($lintFailed -and $lintCmd) {
    # Nur die ersten 5 Zeilen der Lint-Ausgabe — nicht die ganze Ausgabe fluten
    $shortResult = ($lintResult | Select-Object -First 5) -join "`n"
    $msg = "CASCADE-LINT [$lintCmd]: Fehler in $([System.IO.Path]::GetFileName($filePath))"
    Write-Output $msg
    if ($shortResult) {
        Write-Output $shortResult
    }
    Write-Output "Bitte den Lint-Fehler fixen bevor weitergearbeitet wird."
    Hook-Log "LINT FAILED: $lintCmd on $filePath"
}

# Nie blockieren — exit 0 immer
exit 0
