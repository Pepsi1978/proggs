# selbstupdate-helfer.ps1 -- abgekoppelter Teil des Selbst-Updates (Stand 23.10.2026)
#
# Gestartet von update-zentrale.ps1, nachdem der neue Build im Zwischenordner liegt. Der Helfer muss
# warten, bis keine UpdateZentrale mehr laeuft (die laufende exe ist gesperrt), kopiert dann den Build
# nach bin\Release und startet ihn.
#
# Kam das Update aus der App, uebergibt sie ihre Prozessnummer (-AppPid): sie schreibt ihr Protokoll
# fertig und beendet sich danach SELBST -- der Helfer wartet nur auf das Ende genau dieses Prozesses.
# Keine Marker-Dateien, die liegenbleiben koennten, keine feste Wartezeit. Kam es von aussen
# (-AppPid 0), schliesst der Helfer nach einer Sekunde jede laufende UpdateZentrale selbst.
# Jeder Schritt steht in -Log.
param(
    [Parameter(Mandatory)][string]$Zwischen,
    [Parameter(Mandatory)][string]$Ziel,
    [Parameter(Mandatory)][string]$Exe,
    [Parameter(Mandatory)][string]$Log,
    [int]$AppPid = 0
)

function L([string]$Text) {
    try { Add-Content -LiteralPath $Log -Value ((Get-Date -Format 'dd.MM.yyyy HH:mm:ss') + '  ' + $Text) -Encoding utf8 } catch { }
}

L ("Helfer gestartet (PID $PID, App-PID $AppPid).")
$start = Get-Date

if ($AppPid -gt 0 -and (Get-Process -Id $AppPid -ErrorAction SilentlyContinue)) {
    L 'Update kam aus der App: warte, bis sie ihr Protokoll geschrieben und sich selbst beendet hat (hoechstens 60 s).'
    Wait-Process -Id $AppPid -Timeout 60 -ErrorAction SilentlyContinue
    L ('App ' + $(if (Get-Process -Id $AppPid -ErrorAction SilentlyContinue) { 'nach 60 s noch offen' } else { 'beendet nach ' + [int]((Get-Date) - $start).TotalSeconds + ' s' }) + '.')
}
else {
    Start-Sleep -Seconds 1
}

# Was jetzt noch laeuft (Start von aussen, eine zweite Instanz, eine haengende App), wird geschlossen.
$laufend = @(Get-Process UpdateZentrale -ErrorAction SilentlyContinue)
if ($laufend.Count -gt 0) {
    L ('Schliesse ' + $laufend.Count + ' laufende UpdateZentrale.')
    $laufend | ForEach-Object { $null = $_.CloseMainWindow() }
    for ($i = 0; $i -lt 60 -and (Get-Process UpdateZentrale -ErrorAction SilentlyContinue); $i++) { Start-Sleep -Milliseconds 500 }
    if (Get-Process UpdateZentrale -ErrorAction SilentlyContinue) {
        L 'Nach 30 s noch offen - wird beendet.'
        Get-Process UpdateZentrale -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
    }
}

# Ein gerade beendeter Prozess haelt seine Dateien oft noch einen Moment (Virenscanner, Entladen).
Start-Sleep -Seconds 1
# /R:5 /W:1 statt robocopys Vorgabe (1 Million Versuche je 30 s): eine Sperre kostet so Sekunden, nicht Stunden.
robocopy $Zwischen $Ziel /E /R:5 /W:1 /NFL /NDL /NJH /NJS | Out-Null
$code = $LASTEXITCODE
if ($code -ge 8) {
    L "FEHLER: Kopieren fehlgeschlagen (robocopy Code $code) - die bisherige Fassung bleibt und wird wieder gestartet."
    Start-Process -FilePath $Exe
    L ('Bisherige Fassung wieder gestartet: ' + (Get-Item -LiteralPath $Exe).VersionInfo.FileVersion)
    exit 1
}

L "Kopiert (robocopy Code $code)."
Start-Process -FilePath $Exe
L ('Neue Fassung gestartet: ' + (Get-Item -LiteralPath $Exe).VersionInfo.FileVersion + ' (' + [int]((Get-Date) - $start).TotalSeconds + ' s nach dem Helferstart).')
