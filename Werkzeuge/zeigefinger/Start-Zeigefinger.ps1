param(
  [string]$Projekt = "$env:USERPROFILE\proggs\Experimente",
  [string]$Serial = "emulator-5554",
  [string]$Eingabefenster = "",
  [switch]$NurZwischenablage,
  [switch]$ImVordergrund
)

$ErrorActionPreference = "Stop"
$hier = Split-Path -Parent $MyInvocation.MyCommand.Path
$skript = Join-Path $hier "zeigefinger_overlay.py"

if (-not (Test-Path -LiteralPath $Projekt)) {
  throw "Projektordner nicht gefunden: $Projekt"
}
if (-not (Test-Path -LiteralPath $skript)) {
  throw "Overlay-Skript nicht gefunden: $skript"
}
if (-not (Get-Command scrcpy -ErrorAction SilentlyContinue)) {
  throw "scrcpy ist nicht im PATH."
}

$python = (Get-Command python -ErrorAction Stop).Source
$pythonw = Join-Path (Split-Path -Parent $python) "pythonw.exe"
$argumente = @($skript, "--projekt", $Projekt, "--serial", $Serial)
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
