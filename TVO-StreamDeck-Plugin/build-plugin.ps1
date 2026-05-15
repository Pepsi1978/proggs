# ============================================================================
# Build-Skript: packt den .sdPlugin-Ordner zu einer .streamDeckPlugin-Datei
# die per Doppelklick in der Stream Deck Software installiert wird.
#
# Aufruf:
#   pwsh -File build-plugin.ps1
#
# Ergebnis:
#   ./dist/com.tvo.autoenter.streamDeckPlugin
#   plus eine Kopie auf dem Desktop, damit Frank sofort doppelklicken kann.
# ============================================================================

$ErrorActionPreference = "Stop"

$pluginRoot = Join-Path $PSScriptRoot 'com.tvo.autoenter.sdPlugin'
$distDir    = Join-Path $PSScriptRoot 'dist'
$outZip     = Join-Path $distDir 'com.tvo.autoenter.zip'
$outPlugin  = Join-Path $distDir 'com.tvo.autoenter.streamDeckPlugin'
$desktopOut = Join-Path ([Environment]::GetFolderPath('Desktop')) 'com.tvo.autoenter.streamDeckPlugin'

if (-not (Test-Path $pluginRoot)) {
    throw "Plugin-Ordner nicht gefunden: $pluginRoot"
}

New-Item -ItemType Directory -Force -Path $distDir | Out-Null
if (Test-Path $outZip)     { Remove-Item $outZip -Force }
if (Test-Path $outPlugin)  { Remove-Item $outPlugin -Force }
if (Test-Path $desktopOut) { Remove-Item $desktopOut -Force }

Write-Host "Pack $pluginRoot ..." -ForegroundColor Cyan

# Compress-Archive packt den ORDNER selbst NICHT mit ein wenn wir den Pfad mit \*
# uebergeben — wir brauchen aber den Ordner-Namen im ZIP-Root (Stream-Deck-
# Konvention: das ZIP enthaelt einen Top-Level-Ordner com.tvo.autoenter.sdPlugin).
Compress-Archive -Path $pluginRoot -DestinationPath $outZip -CompressionLevel Optimal -Force

# Stream Deck akzeptiert beliebige Endungen, fuer Doppelklick-Installation
# muss es .streamDeckPlugin heissen.
Rename-Item -Path $outZip -NewName 'com.tvo.autoenter.streamDeckPlugin' -Force

Copy-Item -Path $outPlugin -Destination $desktopOut -Force

Write-Host ""
Write-Host "Fertig:" -ForegroundColor Green
Write-Host "  Plugin-Datei:    $outPlugin"
Write-Host "  Auf dem Desktop: $desktopOut"
Write-Host ""
Write-Host "Installation:" -ForegroundColor Yellow
Write-Host "  1. Stream Deck Software sollte laufen."
Write-Host "  2. Doppelklick auf die Datei auf dem Desktop."
Write-Host "  3. Stream Deck fragt 'Plugin installieren?' -> Ja."
Write-Host "  4. Im Stream Deck Editor rechts in der Aktionsliste taucht"
Write-Host "     'TVO Auto-Enter Toggle' auf -> auf eine Taste ziehen."
