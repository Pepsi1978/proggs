# Startet den Fold-8-Emulator in Originalgroesse - fertig zum Arbeiten.
#
# Aufruf:  .\Start-Fold8.ps1                 -> Emulator, aufgeklappt, Originalgroesse
#          .\Start-Fold8.ps1 -Cover          -> Cover-Display-Emulator
#          .\Start-Fold8.ps1 -MitGeraet      -> zusaetzlich das echte Fold live daneben
#          .\Start-Fold8.ps1 -Zoom 1.5       -> groesser als das echte Handy
#          .\Start-Fold8.ps1 -Apk pfad.apk   -> APK gleich mitinstallieren

param(
  [switch]$Cover,
  [switch]$MitGeraet,
  [double]$Zoom = 1.0,
  [string]$Apk = "",
  [switch]$Kaltstart
)

$ErrorActionPreference = "Continue"
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$adb = "$sdk\platform-tools\adb.exe"
$emulator = "$sdk\emulator\emulator.exe"
$hier = Split-Path -Parent $MyInvocation.MyCommand.Path

# Agenten-Umgebung nicht an die App vererben
Get-ChildItem env: | Where-Object { $_.Name -like "CLAUDE*" -or $_.Name -eq "NO_COLOR" -or $_.Name -like "ANTHROPIC*" } |
  ForEach-Object { Remove-Item "env:$($_.Name)" -ErrorAction SilentlyContinue }
$env:ANDROID_HOME = $sdk

$avd = if ($Cover) { "Fold8_Cover" } else { "Fold8" }

# --- Laeuft schon einer? -------------------------------------------------
$laeuft = & $adb devices | Where-Object { $_ -match "^emulator-\d+\s+device" }
if ($laeuft) {
  Write-Host "Emulator laeuft bereits." -ForegroundColor DarkGray
} else {
  Write-Host "Starte $avd ..." -ForegroundColor Cyan
  $argumente = @("-avd", $avd, "-gpu", "host")
  if ($Kaltstart) { $argumente += "-no-snapshot-load" }
  Start-Process -FilePath $emulator -ArgumentList $argumente -WindowStyle Normal

  Write-Host "Warte auf den Systemstart..." -NoNewline
  $fertig = $false
  for ($i = 0; $i -lt 90; $i++) {
    Start-Sleep -Seconds 4
    $b = (& $adb -s emulator-5554 shell getprop sys.boot_completed 2>$null) -replace "\s",""
    if ($b -eq "1") { $fertig = $true; break }
    Write-Host "." -NoNewline
  }
  Write-Host ""
  if (-not $fertig) { Write-Host "Systemstart nicht bestaetigt - bitte selbst nachsehen." -ForegroundColor Yellow }
  else { Write-Host "Bereit." -ForegroundColor Green }
}

# --- An das echte Geraet angleichen -------------------------------------
# Frank hat die Schrift auf dem Fold 8 auf 90 Prozent gestellt
& $adb -s emulator-5554 shell "settings put system font_scale 0.9" 2>$null | Out-Null

# --- APK installieren ----------------------------------------------------
if ($Apk -ne "") {
  if (Test-Path $Apk) {
    Write-Host "Installiere $Apk ..." -ForegroundColor Cyan
    & $adb -s emulator-5554 install -r $Apk 2>&1 | Select-Object -Last 1
  } else {
    Write-Host "APK nicht gefunden: $Apk" -ForegroundColor Red
  }
}

# --- Originalgroesse setzen ----------------------------------------------
$skript = Join-Path $hier "Set-Originalgroesse.ps1"
if (Test-Path $skript) {
  if ($Cover) { & powershell -ExecutionPolicy Bypass -File $skript -Zoom $Zoom -Cover }
  else        { & powershell -ExecutionPolicy Bypass -File $skript -Zoom $Zoom }
}

# --- Echtes Geraet daneben spiegeln --------------------------------------
if ($MitGeraet) {
  $echte = & $adb devices | Where-Object { $_ -match "^\w+\s+device" -and $_ -notmatch "^emulator-" }
  if ($echte) {
    $seriennr = (($echte | Select-Object -First 1) -split "\s+")[0]
    $scrcpy = Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages" -Recurse -Filter "scrcpy.exe" -ErrorAction SilentlyContinue |
              Select-Object -First 1 -ExpandProperty FullName
    if ($scrcpy) {
      Write-Host "Spiegle das echte Geraet ($seriennr) ..." -ForegroundColor Cyan
      Start-Process -FilePath $scrcpy -ArgumentList "--serial", $seriennr, "--window-title", "Fold8-echt", "--max-size", "1000"
    } else {
      Write-Host "scrcpy nicht gefunden. Installieren mit: winget install Genymobile.scrcpy" -ForegroundColor Yellow
    }
  } else {
    Write-Host "Kein echtes Geraet angeschlossen." -ForegroundColor Yellow
  }
}

Write-Host ""
Write-Host "  Zuklappen:   adb -s emulator-5554 emu sensor set hinge-angle0 0" -ForegroundColor DarkGray
Write-Host "  Aufklappen:  adb -s emulator-5554 emu sensor set hinge-angle0 180" -ForegroundColor DarkGray
Write-Host "  Elemente:    .\Zeig-Elemente.ps1" -ForegroundColor DarkGray
Write-Host ""
