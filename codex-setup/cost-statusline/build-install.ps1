param(
    [string]$Source = (Join-Path $env:USERPROFILE '.cache\codex-cost-source-0.153.2'),
    [switch]$SkipBuild
)
$ErrorActionPreference = 'Stop'
$metadata = Get-Content -Raw (Join-Path $PSScriptRoot 'package.json') | ConvertFrom-Json
if (-not (Test-Path -LiteralPath (Join-Path $Source 'codex-rs\Cargo.toml'))) {
    & git clone --depth 1 --branch rust-v0.153.2 https://github.com/openai/codex.git $Source
    if ($LASTEXITCODE -ne 0) { throw 'Codex-Quellen konnten nicht geladen werden.' }
}
if (-not $SkipBuild) {
    & node (Join-Path $PSScriptRoot 'apply.mjs') $Source
    if ($LASTEXITCODE -ne 0) { throw 'Codex-Patch fehlgeschlagen.' }
    Push-Location (Join-Path $Source 'codex-rs')
    try {
        & cargo +stable build --profile dev-small -p codex-cli --bin codex
        if ($LASTEXITCODE -ne 0) { throw 'Codex-Build fehlgeschlagen; Installation bleibt unverändert.' }
    } finally { Pop-Location }
}
$codexHome = if ($env:CODEX_HOME) { $env:CODEX_HOME } else { Join-Path $env:USERPROFILE '.codex' }
$target = Join-Path $codexHome "cost-cli\$($metadata.version)"
$binary = Join-Path $Source 'codex-rs\target\dev-small\codex.exe'
if (-not (Test-Path -LiteralPath $binary)) { throw "Build fehlt: $binary" }
$npmRoot = (& npm root -g).Trim()
if ($LASTEXITCODE -ne 0) { throw 'npm-Verzeichnis konnte nicht ermittelt werden.' }
$launcher = Join-Path $npmRoot '@openai\codex\bin\codex.js'
$launcherText = Get-Content -Raw -LiteralPath $launcher
$original = 'const binaryPath = findCodexExecutable();'
$marker = '// codex-cost-statusline: lokale CLI'
if (-not $launcherText.Contains($original) -and -not $launcherText.Contains($marker)) {
    throw 'Unbekannter Codex-Launcher. Automatische Installation abgebrochen.'
}
New-Item -ItemType Directory -Path $target -Force | Out-Null
$vendorBin = Join-Path $npmRoot '@openai\codex\node_modules\@openai\codex-win32-x64\vendor\x86_64-pc-windows-msvc\bin'
if (-not (Test-Path -LiteralPath $vendorBin)) { throw 'Originale Codex-Hilfsprogramme fehlen.' }
Get-ChildItem -LiteralPath $vendorBin -File | Where-Object Name -ne 'codex.exe' | ForEach-Object {
    Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $target $_.Name) -Force
}
Copy-Item -LiteralPath $binary -Destination (Join-Path $target 'codex.exe') -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'prices.json') -Destination (Join-Path $codexHome 'cost-prices.json') -Force
$backup = "$launcher.before-cost-statusline"
if (-not (Test-Path -LiteralPath $backup)) { Copy-Item -LiteralPath $launcher -Destination $backup }
$binaryJs = (Join-Path $target 'codex.exe') | ConvertTo-Json -Compress
$replacement = "$marker`nconst binaryPath = $binaryJs;"
if ($launcherText.Contains($marker)) {
    $launcherText = [regex]::Replace($launcherText, '(?m)// codex-cost-statusline: lokale CLI\r?\nconst binaryPath = .*?;', [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $replacement })
} else { $launcherText = $launcherText.Replace($original, $replacement) }
[IO.File]::WriteAllText($launcher, $launcherText, [Text.UTF8Encoding]::new($false))
Write-Host "Codex-Kostenanzeige $($metadata.version) installiert: $target"
Write-Host 'Die neue CLI wird beim nächsten vollständigen Codex-Start verwendet.'
