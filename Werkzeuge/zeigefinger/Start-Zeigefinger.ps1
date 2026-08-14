param(
  [string]$Projekt = "$env:USERPROFILE\proggs\Experimente",
  [string]$Serial = "",
  [string]$Eingabefenster = "",
  [switch]$NurZwischenablage,
  [switch]$ImVordergrund
)

$ErrorActionPreference = "Stop"
$hier = Split-Path -Parent $MyInvocation.MyCommand.Path
$skript = Join-Path $hier "zeigefinger_overlay.py"

# Der VBS-Starter laeuft ohne Konsole: ein blosses throw waere unsichtbar.
function Abbruch([string]$meldung) {
  if (-not $ImVordergrund) {
    Add-Type -AssemblyName System.Windows.Forms | Out-Null
    [System.Windows.Forms.MessageBox]::Show($meldung, "Zeigefinger", "OK", "Error") | Out-Null
  }
  throw $meldung
}

if (-not (Test-Path -LiteralPath $Projekt)) {
  Abbruch "Projektordner nicht gefunden: $Projekt"
}
if (-not (Test-Path -LiteralPath $skript)) {
  Abbruch "Overlay-Skript nicht gefunden: $skript"
}
if (-not (Get-Command scrcpy -ErrorAction SilentlyContinue)) {
  Abbruch "scrcpy ist nicht im PATH."
}

$python = (Get-Command python -ErrorAction SilentlyContinue)
if (-not $python) { Abbruch "Python ist nicht im PATH." }
$python = $python.Source
$pythonw = Join-Path (Split-Path -Parent $python) "pythonw.exe"

# Ohne -Serial entscheidet das Werkzeug selbst: laeuft ein Emulator, wird der
# gespiegelt, sonst das angeschlossene Geraet (Fold 8).
$argumente = @($skript, "--projekt", $Projekt)
if ($Serial) { $argumente += @("--serial", $Serial) }
if ($Eingabefenster) { $argumente += @("--eingabefenster", $Eingabefenster) }
if ($NurZwischenablage) { $argumente += "--nur-zwischenablage" }

if ($ImVordergrund) {
  & $python @argumente
  exit $LASTEXITCODE
}

if (-not (Test-Path -LiteralPath $pythonw)) { $pythonw = $python }
$prozessArgumente = $argumente | ForEach-Object { '"' + ($_ -replace '"', '\"') + '"' }
Start-Process -FilePath $pythonw -ArgumentList $prozessArgumente -WorkingDirectory $hier
Write-Host "Zeigefinger gestartet. Die Leiste erscheint an der scrcpy-Spiegelung." -ForegroundColor Green
