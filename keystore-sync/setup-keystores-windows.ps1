# setup-keystores-windows.ps1
# ------------------------------------------------------------------------------
# Holt alle App-Keystores + zugehoerige Secrets vom Laufwerk Y (WireGuard-Freigabe
# \\10.8.0.1\daten) nach %USERPROFILE%\SK\<App>\ — fuer einen WEITEREN Windows-Rechner.
#
# Hintergrund: Alle Android-Apps nutzen denselben gemeinsamen Debug-Keystore
# (debug-shared.keystore). So ist die Debug-Signatur auf allen Rechnern gleich und
# Apps lassen sich ueber Rechner-Grenzen hinweg installieren.
#
# Benutzung (PowerShell) auf dem anderen Windows-Rechner:
#   1. Laufwerk Y verbinden (WireGuard aktiv), z.B.:
#        net use Y: \\10.8.0.1\daten
#   2. Dieses Skript ausfuehren:
#        powershell -ExecutionPolicy Bypass -File Y:\Keystore\setup-keystores-windows.ps1
#      Optional mit eigenem Quellpfad:
#        powershell -ExecutionPolicy Bypass -File .\setup-keystores-windows.ps1 -Source "Y:\Keystore"
# ------------------------------------------------------------------------------
param(
    [string]$Source = "Y:\Keystore"
)

$ErrorActionPreference = "Stop"
$Dest = Join-Path $env:USERPROFILE "SK"
$Apps = @("BestJournalAndroid","BestJournalFrank","EntropieReductor","CortexAndroid","NEMS","VoiceKey")

Write-Host "=== Keystore-Setup (Windows) ==="
Write-Host "Quelle:  $Source"
Write-Host "Ziel:    $Dest"
Write-Host ""

if (-not (Test-Path $Source)) {
    Write-Host "FEHLER: Quell-Ordner '$Source' nicht gefunden."
    Write-Host "  -> Ist Laufwerk Y verbunden? (WireGuard aktiv?)  net use Y: \\10.8.0.1\daten"
    Write-Host "  -> Oder Quellpfad uebergeben:  -Source <Pfad>\Keystore"
    exit 1
}

foreach ($app in $Apps) {
    $src = Join-Path $Source $app
    if (Test-Path $src) {
        $dst = Join-Path $Dest $app
        New-Item -ItemType Directory -Force -Path $dst | Out-Null
        Copy-Item -Path (Join-Path $src "*") -Destination $dst -Force -ErrorAction SilentlyContinue
        $files = (Get-ChildItem $src -File | ForEach-Object { $_.Name }) -join " "
        Write-Host "  [OK] $app  <- $files"
    } else {
        Write-Host "  [--] $app  (kein Ordner in der Quelle, uebersprungen)"
    }
}

Write-Host ""
Write-Host "=== Verifikation: alle debug-shared.keystore muessen identisch sein ==="
Get-ChildItem (Join-Path $Dest "*\debug-shared.keystore") -ErrorAction SilentlyContinue | ForEach-Object {
    $hash = (Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLower()
    Write-Host ("  {0}  {1}" -f $hash, ($_.FullName -replace [regex]::Escape($env:USERPROFILE + "\"), ""))
}

Write-Host ""
Write-Host "Fertig. Du kannst jetzt bauen:"
Write-Host "  cd `$HOME\proggs\<App>; .\gradlew assembleDebug"
Write-Host ""
Write-Host "Hinweis: BestJournalAndroid-Testversion und EntropieReductor wurden auf den"
Write-Host "gemeinsamen Debug-Keystore umgestellt. Falls auf einem Geraet 'Signatures do not"
Write-Host "match' / INSTALL_FAILED_UPDATE_INCOMPATIBLE kommt, die App dort EINMAL"
Write-Host "deinstallieren und neu installieren."
