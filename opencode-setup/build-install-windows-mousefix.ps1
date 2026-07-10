param(
    [string]$Version = "1.17.18",
    [string]$PatchRevision = "6"
)

$ErrorActionPreference = "Stop"
$workDir = Join-Path $env:TEMP "opencode-mousefix-$Version"
$patchPath = Join-Path $PSScriptRoot "patches\opencode-1.17.18-windows-mouse.patch"
$installDir = Join-Path $HOME ".local\share\opencode-mousefix"
$target = Join-Path $installDir "opencode.exe"

if (-not (Test-Path -LiteralPath $patchPath)) {
    throw "Patch nicht gefunden: $patchPath"
}

if (Test-Path -LiteralPath $workDir) {
    Remove-Item -LiteralPath $workDir -Recurse -Force
}

git clone --depth 1 --branch "v$Version" https://github.com/anomalyco/opencode.git $workDir
if ($LASTEXITCODE -ne 0) { throw "OpenCode v$Version konnte nicht geklont werden." }

Push-Location $workDir
try {
    git apply --check --ignore-space-change $patchPath
    if ($LASTEXITCODE -ne 0) { throw "Der Windows-Stabilitätspatch passt nicht auf OpenCode v$Version." }
    git apply --ignore-space-change $patchPath
    if ($LASTEXITCODE -ne 0) { throw "Der Windows-Stabilitätspatch konnte nicht angewendet werden." }

    bun install --ignore-scripts
    if ($LASTEXITCODE -ne 0) { throw "Bun-Abhängigkeiten konnten nicht installiert werden." }

    $openTuiDir = Join-Path $workDir "packages\tui\node_modules\@opentui\core"
    $openTuiPackage = Join-Path $openTuiDir "package.json"
    $openTuiBundle = Join-Path $openTuiDir "index-xt9f071j.js"
    if (-not (Test-Path -LiteralPath $openTuiPackage) -or -not (Test-Path -LiteralPath $openTuiBundle)) {
        throw "Die erwartete OpenTUI-Core-Abhängigkeit fehlt."
    }
    $openTuiVersion = (Get-Content -LiteralPath $openTuiPackage -Raw | ConvertFrom-Json).version
    if ($openTuiVersion -ne "0.4.3") {
        throw "Unerwartete OpenTUI-Core-Version: $openTuiVersion"
    }
    $rendererSource = [IO.File]::ReadAllText($openTuiBundle)
    $newline = if ($rendererSource.Contains("`r`n")) { "`r`n" } else { "`n" }
    $rendererNeedle = "  reportNativeRenderFailure() {${newline}    console.error(`"[CliRenderer] Native frame render failed; waiting for the next render request to force repaint`");"
    $rendererReplacement = "  reportNativeRenderFailure() {${newline}    this.forceFullRepaintRequested = true;${newline}    console.error(`"[CliRenderer] Native frame render failed; waiting for the next render request to force repaint`");"
    if (-not $rendererSource.Contains($rendererNeedle)) {
        throw "Der OpenTUI-Recovery-Patch passt nicht auf Core $openTuiVersion."
    }
    $rendererSource = $rendererSource.Replace($rendererNeedle, $rendererReplacement)
    [IO.File]::WriteAllText($openTuiBundle, $rendererSource, [Text.UTF8Encoding]::new($false))

    $env:OPENCODE_VERSION = "$Version-windowsfix.$PatchRevision"
    bun run --cwd packages/opencode script/build.ts --single --skip-install --skip-embed-web-ui
    if ($LASTEXITCODE -ne 0) { throw "Das gepatchte OpenCode-Binary konnte nicht gebaut werden." }

    $built = Join-Path $workDir "packages\opencode\dist\opencode-windows-x64\bin\opencode.exe"
    if (-not (Test-Path -LiteralPath $built)) { throw "Build-Artefakt fehlt: $built" }

    New-Item -ItemType Directory -Path $installDir -Force | Out-Null
    $staged = "$target.new"
    Copy-Item -LiteralPath $built -Destination $staged -Force
    Move-Item -LiteralPath $staged -Destination $target -Force

    $actual = (& $target --version).Trim()
    if ($actual -ne $env:OPENCODE_VERSION) {
        throw "Versionsprüfung fehlgeschlagen: erwartet $($env:OPENCODE_VERSION), erhalten $actual"
    }
    "Installiert: $target ($actual)"
}
finally {
    Pop-Location
}
