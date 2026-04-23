# Produces a release drop of PromptBoard.
# Default target: a portable self-contained ZIP for win-x64 that needs no
# install step — the user unzips and double-clicks PromptBoard.App.exe.
#
# Usage (from the PromptBoard repo root):
#   pwsh -File scripts/build-installer.ps1
#   pwsh -File scripts/build-installer.ps1 -Version 1.2.3
#   pwsh -File scripts/build-installer.ps1 -Runtime win-arm64
#
# Note on MSIX: the csproj uses WindowsPackageType=None (unpackaged), so
# we ship a ZIP. Turning this into an MSIX needs a code-signing certificate
# and a WindowsPackageType switch — out of scope for this shipping step.

param(
    [string]$Version = "1.0.0",
    [ValidateSet("win-x64", "win-x86", "win-arm64")]
    [string]$Runtime = "win-x64",
    [string]$Configuration = "Release"
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$app = Join-Path $root "PromptBoard.App\PromptBoard.App.csproj"
$publishRoot = Join-Path $root "publish"
$stageDir = Join-Path $publishRoot "PromptBoard-$Version-$Runtime"
$zipPath = Join-Path $publishRoot "PromptBoard-$Version-$Runtime.zip"

if (-not (Test-Path $app)) {
    throw "PromptBoard.App.csproj not found at $app"
}

Write-Host "==> Publishing $Runtime $Configuration (version $Version)"

if (Test-Path $stageDir) {
    Remove-Item -Recurse -Force $stageDir
}
if (Test-Path $zipPath) {
    Remove-Item -Force $zipPath
}

dotnet publish $app `
    -c $Configuration `
    -r $Runtime `
    --self-contained true `
    -p:PublishSingleFile=false `
    -p:PublishReadyToRun=true `
    -p:Platform=$(switch ($Runtime) { "win-x64" { "x64" } "win-x86" { "x86" } "win-arm64" { "ARM64" } }) `
    -o $stageDir `
    --nologo
if ($LASTEXITCODE -ne 0) {
    throw "dotnet publish failed with exit code $LASTEXITCODE"
}

# Drop a README next to the binaries so the end user knows what to do.
$readme = @"
PromptBoard $Version ($Runtime)
===============================

So startest du PromptBoard:

1. Entpacke diese ZIP in einen dauerhaften Ordner (z.B. `C:\Programme\PromptBoard\`).
2. Doppelklick auf `PromptBoard.App.exe`.
3. Beim ersten Start legt die App eine kleine Datenbank im Ordner an — die
   Prompts und Einstellungen bleiben also neben der exe liegen und lassen
   sich einfach mitsichern.

Hinweise:

* Das Programm ist unsigniert (kein teures Code-Signing-Zertifikat). Windows
  fragt beim ersten Start eventuell nach. "Trotzdem ausfuehren" waehlen.
* Zum Deinstallieren: einfach den Ordner loeschen.
* Konfiguration, API-Keys und Prompts findest du in der Datei `promptboard.db`
  im selben Ordner. Zur Sicherheit regelmaessig diese Datei zusaetzlich sichern.

Online: https://github.com/Pepsi1978/proggs
"@
Set-Content -Path (Join-Path $stageDir "README.txt") -Value $readme -Encoding UTF8

Write-Host "==> Creating ZIP $zipPath"
Compress-Archive -Path "$stageDir\*" -DestinationPath $zipPath -Force

$size = [math]::Round((Get-Item $zipPath).Length / 1MB, 1)
Write-Host ""
Write-Host "Done."
Write-Host "  Stage folder: $stageDir"
Write-Host "  ZIP release:  $zipPath ($size MB)"
Write-Host ""
Write-Host "Smoke test: start `"$stageDir\PromptBoard.App.exe`""
