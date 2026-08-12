# Zeigt, was in der App gerade schiefgeht - Abstuerze und Laufzeitfehler,
# lesbar aufbereitet statt als Logcat-Wand.
#
# Aufruf:  .\Zeig-Fehler.ps1                 # laufende App im Vordergrund
#          .\Zeig-Fehler.ps1 -Paket de.frank.entropyreducer.debug
#          .\Zeig-Fehler.ps1 -Geraet R3GL7073MLM   # echtes Handy statt Emulator
#          .\Zeig-Fehler.ps1 -Live            # mitlaufen und Fehler sofort melden
#          .\Zeig-Fehler.ps1 -Alles           # auch Fremd-/Systemmeldungen

param(
  [string]$Paket = "",
  [string]$Geraet = "",
  [switch]$Live,
  [switch]$Alles
)

$ErrorActionPreference = "Continue"
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) { Write-Host "adb nicht gefunden." -ForegroundColor Red; exit 1 }

# --- Geraet waehlen ------------------------------------------------------
if ($Geraet -eq "") {
  $zeilen = & $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\sdevice$" }
  if (-not $zeilen) { Write-Host "Kein Geraet verbunden." -ForegroundColor Red; exit 1 }
  $emu = $zeilen | Where-Object { $_ -match "^emulator-" } | Select-Object -First 1
  if (-not $emu) { $emu = @($zeilen)[0] }
  $Geraet = ($emu -split "\s+")[0]
}

# --- Paket ermitteln ------------------------------------------------------
# Reihenfolge ist wichtig: Nach einem Absturz steht die App NICHT mehr im
# Vordergrund - dort ist dann der Launcher. Wer nur nach vorn schaut, findet
# ausgerechnet im Fehlerfall nichts. Darum zuerst das Absturzprotokoll fragen.
if ($Paket -eq "") {
  $letzterCrash = & $adb -s $Geraet logcat -b crash -d 2>$null |
                  Where-Object { $_ -match "Process:\s*([a-zA-Z0-9_.]+)" } | Select-Object -Last 1
  if ($letzterCrash -match "Process:\s*([a-zA-Z0-9_.]+)") { $Paket = $Matches[1] }
}
if ($Paket -eq "") {
  $vorn = & $adb -s $Geraet shell "dumpsys activity activities | grep -m1 topResumedActivity" 2>$null
  if ($vorn -match "([a-zA-Z0-9_]+(?:\.[a-zA-Z0-9_]+)+)/") { $kandidat = $Matches[1] } else { $kandidat = "" }
  # Startbildschirm und Systemoberflaeche sind nie die gesuchte App
  if ($kandidat -notmatch "nexuslauncher|com\.android\.systemui|launcher") { $Paket = $kandidat }
}
if ($Paket -eq "") {
  Write-Host "Konnte die laufende App nicht bestimmen - bitte -Paket angeben." -ForegroundColor Yellow
} else {
  Write-Host "App: $Paket   Geraet: $Geraet" -ForegroundColor Cyan
}

# --- Abstuerze -----------------------------------------------------------
Write-Host ""
Write-Host "=== Abstuerze ===" -ForegroundColor White
$crash = & $adb -s $Geraet logcat -b crash -d 2>$null

# Native Systemabstuerze (Bluetooth, Grafiktreiber ...) gehoeren NICHT zur App.
# Sie mit Stacktrace anzuzeigen wuerde nur in die Irre fuehren.
$nativesRauschen = "F DEBUG|libbluetooth|libart\.so|libc\.so|BuildId:|pbtombstone|backtrace:|/apex/"
$systemCrashes = @($crash | Where-Object { $_ -match $nativesRauschen })

if ($Paket -ne "" -and -not $Alles) {
  $kurz = $Paket -replace '\.debug$',''
  $crashRelevant = @($crash | Where-Object { $_ -match [regex]::Escape($kurz) -and $_ -notmatch $nativesRauschen })
} else {
  $crashRelevant = @($crash | Where-Object { $_ -notmatch $nativesRauschen })
}

if ($crashRelevant.Count -eq 0) {
  Write-Host "  Keine Abstuerze der App." -ForegroundColor Green
  if ($systemCrashes.Count -gt 0) {
    Write-Host "  (Das Emulator-System selbst hat protokolliert - betrifft die App nicht.)" -ForegroundColor DarkGray
  }
} else {
  # Nur die aussagekraeftigen Zeilen: Fehlertyp, Ursache, eigene Codestellen
  $wichtig = $crashRelevant | Where-Object {
    $_ -match "FATAL EXCEPTION|Process:|[a-zA-Z]+(Exception|Error):|Caused by:|at (de|com|kotlin)\.[a-zA-Z0-9_.]+\("
  } | Select-Object -Last 25
  if ($wichtig) { $wichtig | ForEach-Object { Write-Host "  $_" -ForegroundColor Yellow } }
  else { $crashRelevant | Select-Object -Last 15 | ForEach-Object { Write-Host "  $_" -ForegroundColor DarkGray } }
}

# --- Laufende Fehlermeldungen -------------------------------------------
Write-Host ""
Write-Host "=== Fehlermeldungen der App ===" -ForegroundColor White
$fehler = & $adb -s $Geraet logcat -d -t 600 *:E 2>$null
if ($Paket -ne "" -and -not $Alles) {
  $kurz = $Paket -replace '\.debug$',''
  $fehler = $fehler | Where-Object { $_ -match [regex]::Escape($kurz) }
}
# Rauschen ausblenden, das jede Android-App erzeugt
$fehler = $fehler | Where-Object { $_ -notmatch "EGL_emulation|OpenGLRenderer|eglCodecCommon|HostConnection" }

if (-not $fehler) {
  Write-Host "  Keine Fehlermeldungen." -ForegroundColor Green
} else {
  $fehler | Select-Object -Last 20 | ForEach-Object { Write-Host "  $_" -ForegroundColor Yellow }
}

# --- Live mitlesen -------------------------------------------------------
if ($Live) {
  Write-Host ""
  Write-Host "=== Live (Abbruch mit Strg+C) ===" -ForegroundColor White
  & $adb -s $Geraet logcat -c 2>$null | Out-Null
  $kurz = if ($Paket -ne "") { $Paket -replace '\.debug$','' } else { "" }
  & $adb -s $Geraet logcat *:E 2>$null | ForEach-Object {
    if ($_ -match "EGL_emulation|OpenGLRenderer|eglCodecCommon|HostConnection") { return }
    if ($kurz -ne "" -and -not $Alles -and $_ -notmatch [regex]::Escape($kurz)) { return }
    Write-Host $_ -ForegroundColor Yellow
  }
}

Write-Host ""
