# Klappt das Fold 8 im Emulator auf oder zu - mit korrektem Format UND korrekter
# Groesse.
#
# WARUM NICHT DER FALT-KNOPF IM EMULATOR? Der aendert nur den Scharnierwinkel.
# Android rechnet das Layout dabei NICHT auf das andere Format um: die App wird
# weiter auf 1848x2448 gerendert und nur ausschnittweise angezeigt (siehe
# bugs/android/emulator-foldable.md #1). Ein echter Wechsel braucht die andere
# AVD - genau das macht dieses Skript, inklusive App und Groesse.
#
# Aufruf:  .\Klappen.ps1          -> in den jeweils anderen Zustand wechseln
#          .\Klappen.ps1 -Auf     -> aufgeklappt (Innendisplay, 7,6 Zoll)
#          .\Klappen.ps1 -Zu      -> zugeklappt (Cover-Display, 5,5 Zoll) - der Normalfall
#          .\Klappen.ps1 -Auf -Projekt EntropieReductor   -> App ausdruecklich vorgeben

param(
  [switch]$Auf,
  [switch]$Zu,
  [string]$Projekt = "",
  [double]$Zoom = 1.0
)

$ErrorActionPreference = "Continue"
$hier = Split-Path -Parent $MyInvocation.MyCommand.Path
$adb  = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

# --- Wohin? --------------------------------------------------------------
if ($Auf -and $Zu) {
  Write-Host "Bitte nur -Auf ODER -Zu angeben." -ForegroundColor Red
  exit 1
}
if (-not $Auf -and -not $Zu) {
  # Umschalten: das Gegenteil dessen, was gerade laeuft. Laeuft nichts, ist der
  # Normalfall zugeklappt.
  $laufende = ""
  if (Test-Path $adb) {
    $laufende = ((& $adb -s emulator-5554 emu avd name 2>$null) | Select-Object -First 1) -replace "\s",""
  }
  if ($laufende -match "Cover") { $Auf = $true } else { $Zu = $true }
}

# --- Welche App soll mitkommen? -----------------------------------------
# Ohne Angabe die zuletzt gestartete - so klappt der Wechsel mit jeder App,
# nicht nur mit einer bestimmten.
if ($Projekt -eq "") {
  $merker = Join-Path $env:TEMP "fold8-letztes-projekt.txt"
  if (Test-Path $merker) {
    $Projekt = (Get-Content $merker -ErrorAction SilentlyContinue | Select-Object -First 1)
  }
}

$ziel = if ($Auf) { "aufgeklappt (Innendisplay, 7,6 Zoll)" } else { "zugeklappt (Cover-Display, 5,5 Zoll)" }
Write-Host "Wechsle nach: $ziel" -ForegroundColor Cyan
if ($Projekt -ne "") { Write-Host "App: $(Split-Path $Projekt -Leaf)" -ForegroundColor DarkGray }

# --- Start-Fold8 macht den Rest: AVD wechseln, App mitnehmen, Groesse setzen
$start = Join-Path $hier "Start-Fold8.ps1"
if (-not (Test-Path $start)) {
  Write-Host "Start-Fold8.ps1 nicht gefunden neben diesem Skript." -ForegroundColor Red
  exit 1
}

$argumente = @("-ExecutionPolicy","Bypass","-File",$start,"-Zoom",$Zoom)
if ($Auf) { $argumente += "-Innen" }
if ($Projekt -ne "") { $argumente += @("-Projekt",$Projekt,"-OhneBauen") }
& powershell $argumente
