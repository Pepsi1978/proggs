param(
    [string]$Version = "",
    [string]$PatchRevision = "6",
    [switch]$Force,
    [string]$InstallRoot = ""
)

$ErrorActionPreference = "Stop"
$sourceVersion = $Version
$runId = [Guid]::NewGuid().ToString("N")
$patchPath = Join-Path $PSScriptRoot "patches\opencode-1.17.18-windows-mouse.patch"
if (-not $InstallRoot) { $InstallRoot = Join-Path $HOME ".local\share\opencode-mousefix" }
$versionsDir = Join-Path $InstallRoot "versions"
$stagingDir = Join-Path $InstallRoot "staging"
$pointerPath = Join-Path $InstallRoot "current.json"
$statePath = Join-Path $InstallRoot "update-state.json"
$lockPath = Join-Path $InstallRoot "update.lock"
$legacyTarget = Join-Path $InstallRoot "opencode.exe"
$lock = $null
$candidate = $null

function Write-Utf8NoBom([string]$Path, [string]$Content) {
    [IO.File]::WriteAllText($Path, $Content, [Text.UTF8Encoding]::new($false))
}

function Write-AtomicJson([string]$Path, [object]$Value) {
    $temp = "$Path.$runId.tmp"
    Write-Utf8NoBom $temp ($Value | ConvertTo-Json -Depth 12)
    if (Test-Path -LiteralPath $Path) {
        [IO.File]::Replace($temp, $Path, "$Path.bak", $true)
    } else {
        [IO.File]::Move($temp, $Path)
    }
}

function Read-Json([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) { return $null }
    try { return [IO.File]::ReadAllText($Path) | ConvertFrom-Json } catch { return $null }
}

function Set-UpdateState([string]$Status, [string]$Message, [int]$RetryHours = 24) {
    Write-AtomicJson $statePath ([ordered]@{
        schemaVersion = 1
        status = $Status
        message = $Message
        candidateVersion = $candidate
        lastAttemptUtc = [DateTime]::UtcNow.ToString("o")
        nextCheckNotBeforeUtc = [DateTime]::UtcNow.AddHours($RetryHours).ToString("o")
        runId = $runId
    })
}

function Resolve-PointerExe($Entry) {
    if ($null -eq $Entry -or -not $Entry.relativeExe) { return $null }
    $root = [IO.Path]::GetFullPath($InstallRoot).TrimEnd('\') + '\'
    $path = [IO.Path]::GetFullPath((Join-Path $InstallRoot ([string]$Entry.relativeExe)))
    if (-not $path.StartsWith($root, [StringComparison]::OrdinalIgnoreCase)) { return $null }
    if (Test-Path -LiteralPath $path) { return $path }
    return $null
}

function Publish-Candidate([string]$Built, [string]$UpstreamVersion, [string]$CustomVersion, [string]$SourceCommit) {
    $finalDir = Join-Path $versionsDir $CustomVersion
    $finalExe = Join-Path $finalDir "opencode.exe"
    if (-not (Test-Path -LiteralPath $finalExe)) {
        $stage = Join-Path $stagingDir "$CustomVersion-$runId"
        New-Item -ItemType Directory -Path $stage -Force | Out-Null
        Copy-Item -LiteralPath $Built -Destination (Join-Path $stage "opencode.exe") -Force
        $stagedVersion = (& (Join-Path $stage "opencode.exe") --version).Trim()
        if ($stagedVersion -ne $CustomVersion) {
            throw "Versionsprüfung fehlgeschlagen: erwartet $CustomVersion, erhalten $stagedVersion"
        }
        Move-Item -LiteralPath $stage -Destination $finalDir
    }

    $actual = (& $finalExe --version).Trim()
    if ($actual -ne $CustomVersion) { throw "Installiertes Binary meldet unerwartet $actual" }
    $sha256 = (Get-FileHash -LiteralPath $finalExe -Algorithm SHA256).Hash.ToLowerInvariant()
    $current = Read-Json $pointerPath
    $pointer = [ordered]@{
        schemaVersion = 1
        active = [ordered]@{
            upstreamVersion = $UpstreamVersion
            customVersion = $CustomVersion
            relativeExe = "versions/$CustomVersion/opencode.exe"
            sha256 = $sha256
            sourceTag = "v$UpstreamVersion"
            sourceCommit = $SourceCommit
            capabilities = [ordered]@{
                windowsMouseHybrid = "present"
                fullRepaintRecovery = "present"
                tuiVariant = "present"
            }
        }
        previous = $current.active
        activatedAtUtc = [DateTime]::UtcNow.ToString("o")
    }
    Write-AtomicJson $pointerPath $pointer
    Set-UpdateState "installed" "OpenCode $CustomVersion ist für neue Sitzungen aktiv."
    "Installiert: $finalExe ($actual)"
}

New-Item -ItemType Directory -Path $InstallRoot, $versionsDir, $stagingDir -Force | Out-Null
try {
    try {
        $lock = [IO.File]::Open($lockPath, [IO.FileMode]::OpenOrCreate, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None)
    } catch [IO.IOException] {
        "Update läuft bereits."
        return
    }

    $state = Read-Json $statePath
    if (-not $Force -and -not $sourceVersion -and $state.nextCheckNotBeforeUtc) {
        $nextValue = $state.nextCheckNotBeforeUtc
        $next = if ($nextValue -is [DateTime]) {
            $nextValue.ToUniversalTime()
        } else {
            [DateTime]::Parse(
                [string]$nextValue,
                [Globalization.CultureInfo]::InvariantCulture,
                [Globalization.DateTimeStyles]::RoundtripKind
            ).ToUniversalTime()
        }
        if ($next -gt [DateTime]::UtcNow) {
            "Updateprüfung erst wieder ab $($next.ToString('o'))."
            return
        }
    }

    if (-not $Version) {
        $npm = Get-Command npm.cmd -ErrorAction SilentlyContinue
        if (-not $npm) { $npm = Get-Command npm -ErrorAction Stop }
        $raw = (& $npm.Source view opencode-ai dist-tags.latest --json 2>$null) -join ""
        if ($LASTEXITCODE -ne 0 -or -not $raw) { throw "Neueste OpenCode-Version konnte nicht abgefragt werden." }
        $Version = [string]($raw | ConvertFrom-Json)
    }
    if ($Version -notmatch '^\d+\.\d+\.\d+$') { throw "Keine stabile OpenCode-Version: $Version" }
    $candidate = $Version
    $customVersion = "$Version-windowsfix.$PatchRevision"

    $current = Read-Json $pointerPath
    $activeExe = Resolve-PointerExe $current.active
    if ($activeExe -and $current.active.upstreamVersion -eq $Version -and -not $Force) {
        Set-UpdateState "up-to-date" "OpenCode $customVersion ist bereits aktiv."
        "Bereits aktuell: $activeExe"
        return
    }

    if (-not $current -and (Test-Path -LiteralPath $legacyTarget)) {
        $legacyVersion = (& $legacyTarget --version).Trim()
        if ($legacyVersion -eq $customVersion) {
            Publish-Candidate $legacyTarget $Version $customVersion "legacy-migration"
            return
        }
    }

    if (-not (Test-Path -LiteralPath $patchPath)) { throw "Patch nicht gefunden: $patchPath" }
    foreach ($command in @("git", "bun")) {
        if (-not (Get-Command $command -ErrorAction SilentlyContinue)) { throw "Voraussetzung fehlt: $command" }
    }

    Set-UpdateState "building" "OpenCode $customVersion wird geprüft und gebaut." 1
    $workDir = Join-Path $env:TEMP "opencode-windowsfix-$Version-$runId"
    git clone --depth 1 --branch "v$Version" https://github.com/anomalyco/opencode.git $workDir
    if ($LASTEXITCODE -ne 0) { throw "OpenCode v$Version konnte nicht geklont werden." }

    Push-Location $workDir
    try {
        git apply --check --ignore-space-change $patchPath
        if ($LASTEXITCODE -ne 0) { throw "Windows-Funktionen sind mit OpenCode v$Version noch nicht kompatibel." }
        git apply --ignore-space-change $patchPath
        if ($LASTEXITCODE -ne 0) { throw "Windows-Stabilitätspatch konnte nicht angewendet werden." }

        bun install --ignore-scripts
        if ($LASTEXITCODE -ne 0) { throw "Bun-Abhängigkeiten konnten nicht installiert werden." }

        $openTuiDir = Join-Path $workDir "packages\tui\node_modules\@opentui\core"
        $rendererBundles = @(Get-ChildItem -LiteralPath $openTuiDir -Filter "index-*.js" | Where-Object {
            [IO.File]::ReadAllText($_.FullName).Contains("reportNativeRenderFailure()")
        })
        if ($rendererBundles.Count -ne 1) { throw "OpenTUI-Renderer-Bundle konnte nicht eindeutig bestimmt werden." }
        $openTuiBundle = $rendererBundles[0].FullName
        $rendererSource = [IO.File]::ReadAllText($openTuiBundle)
        $newline = if ($rendererSource.Contains("`r`n")) { "`r`n" } else { "`n" }
        $needle = "  reportNativeRenderFailure() {${newline}    console.error(`"[CliRenderer] Native frame render failed; waiting for the next render request to force repaint`");"
        $replacement = "  reportNativeRenderFailure() {${newline}    this.forceFullRepaintRequested = true;${newline}    console.error(`"[CliRenderer] Native frame render failed; waiting for the next render request to force repaint`");"
        if ($rendererSource.Contains($needle)) {
            Write-Utf8NoBom $openTuiBundle ($rendererSource.Replace($needle, $replacement))
        } elseif (-not $rendererSource.Contains($replacement)) {
            throw "OpenTUI-Full-Repaint-Recovery ist weder vorhanden noch sicher patchbar."
        }

        foreach ($package in @("core", "plugin", "tui", "opencode")) {
            bun run --cwd "packages/$package" typecheck
            if ($LASTEXITCODE -ne 0) { throw "Typecheck fehlgeschlagen: packages/$package" }
        }
        Push-Location "packages\tui"
        try {
            bun test "test/context/local.test.ts" "test/clipboard.test.ts"
            if ($LASTEXITCODE -ne 0) { throw "TUI-Regressionstests fehlgeschlagen." }
        } finally { Pop-Location }
        Push-Location "packages\opencode"
        try {
            bun test "test/cli/tui/thread.test.ts"
            if ($LASTEXITCODE -ne 0) { throw "CLI-Regressionstests fehlgeschlagen." }
        } finally { Pop-Location }

        $env:OPENCODE_VERSION = $customVersion
        bun run --cwd packages/opencode script/build.ts --single --skip-install --skip-embed-web-ui
        if ($LASTEXITCODE -ne 0) { throw "Custom-Binary konnte nicht gebaut werden." }
        $built = Join-Path $workDir "packages\opencode\dist\opencode-windows-x64\bin\opencode.exe"
        if (-not (Test-Path -LiteralPath $built)) { throw "Build-Artefakt fehlt: $built" }
        $sourceCommit = (git rev-parse HEAD).Trim()
        Publish-Candidate $built $Version $customVersion $sourceCommit
    } finally {
        Pop-Location
    }
}
catch {
    if ($lock) { Set-UpdateState "failed" $_.Exception.Message 1 }
    throw
}
finally {
    if ($null -ne $lock) { $lock.Dispose() }
}
