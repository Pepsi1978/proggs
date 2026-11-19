# Richtet die Jarvis-PC-Brücke ein: Aufgabe „JarvisPcBruecke“ startet sie bei jeder Anmeldung unsichtbar und startet
# sie jede Minute neu, falls sie nicht mehr läuft. Läuft ohne Administratorrechte (nur für den angemeldeten Benutzer). Mehrfach ausführbar.
# Power-PC: einrichten.ps1 -Rang power schreibt %LOCALAPPDATA%\JarvisPcBruecke\rechner.json ({"rang":"power","sol":true}).
# -Rang haupt entfernt die Datei wieder (Laptop: kein Rang, Platz haupt). Ohne -Rang bleibt eine vorhandene Datei unverändert.
# Hat sich der Rang geändert, wird die laufende Brücke beendet und neu gestartet (sonst hielte sie die Sperre mit dem alten Rang).
# Nie nur Stop-ScheduledTask: Das beendet nur conhost, pwsh liefe verwaist weiter und jeder curl-Aufruf scheiterte.
param([ValidateSet('haupt', 'power')][string]$Rang)
$ErrorActionPreference = 'Stop'
$rechnerDatei = Join-Path $env:LOCALAPPDATA 'JarvisPcBruecke\rechner.json'
function Lies-Rang {
    if (-not (Test-Path -LiteralPath $rechnerDatei)) { return 'haupt' }
    try { $r = "$((Get-Content -Raw -Encoding utf8 -LiteralPath $rechnerDatei | ConvertFrom-Json).rang)".Trim().ToLowerInvariant() } catch { return '?' }
    if ($r) { return $r } else { return 'haupt' }
}
function Bruecken-Prozesse {
    @(Get-CimInstance Win32_Process -Filter "Name='pwsh.exe' OR Name='powershell.exe'" |
        Where-Object { $_.ProcessId -ne $PID -and "$($_.CommandLine)" -like '*jarvis-pc-bruecke.ps1*' })
}
$alterRang = Lies-Rang
if ($Rang -eq 'power') {
    New-Item -ItemType Directory -Force -Path (Split-Path $rechnerDatei) | Out-Null
    [System.IO.File]::WriteAllText($rechnerDatei, '{"rang":"power","sol":true}', [System.Text.UTF8Encoding]::new($false))
    Write-Output "Rang power gespeichert: $rechnerDatei"
} elseif ($Rang -eq 'haupt') {
    Remove-Item -LiteralPath $rechnerDatei -Force -ErrorAction SilentlyContinue
    Write-Output 'Rang haupt (Standard): keine lokale Einstellung.'
}
$skript = Join-Path $PSScriptRoot 'jarvis-pc-bruecke.ps1'
$pwsh = (Get-Command pwsh.exe -ErrorAction SilentlyContinue).Source
if (-not $pwsh) { $pwsh = (Get-Command powershell.exe).Source }

# conhost --headless: kein aufblitzendes Konsolenfenster, auch nicht beim Wächter-Start jede Minute.
# Zugleich hängt die Brücke so an keinem Terminal-Fenster: Unter Windows 11 landete die versteckte Konsole sonst in
# Windows Terminal und wurde mit dessen Fenster beendet (0xC000013A, 15.10.2026).
$conhost = Join-Path $env:SystemRoot 'System32\conhost.exe'
# Victron-Solarbatterie per Bluetooth (jarvis_victron.py) braucht bleak und cryptography.
& python -m pip install --quiet --disable-pip-version-check bleak cryptography
if ($LASTEXITCODE -ne 0) { Write-Warning 'bleak/cryptography nicht installiert: Die Solarbatterie wird vom Rechner nicht gemeldet.' }
$aktion = New-ScheduledTaskAction -Execute $conhost -Argument "--headless `"$pwsh`" -NoProfile -ExecutionPolicy Bypass -File `"$skript`""
$ausloeser = New-ScheduledTaskTrigger -AtLogOn -User "$env:USERDOMAIN\$env:USERNAME"
# Wächter: jede Minute erneut starten. Läuft die Brücke schon, beendet sich die neue sofort (Sperre im Skript).
# Nötig, weil Windows ein Beenden von außen (z. B. 0xC000013A) nicht als Fehler wertet und dann nicht neu startet.
$waechter = New-ScheduledTaskTrigger -Once -At (Get-Date).AddMinutes(1) -RepetitionInterval (New-TimeSpan -Minutes 1)
$ausloeser = @($ausloeser, $waechter)
$einstellungen = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable `
    -ExecutionTimeLimit ([TimeSpan]::Zero) -RestartCount 999 -RestartInterval (New-TimeSpan -Minutes 1) -MultipleInstances IgnoreNew
$haupt = New-ScheduledTaskPrincipal -UserId "$env:USERDOMAIN\$env:USERNAME" -LogonType Interactive -RunLevel Limited
Register-ScheduledTask -TaskName 'JarvisPcBruecke' -Action $aktion -Trigger $ausloeser -Settings $einstellungen -Principal $haupt -Force | Out-Null
$neuerRang = Lies-Rang
if ($neuerRang -ne $alterRang) {
    $laufend = Bruecken-Prozesse
    if ($laufend.Count) {
        Write-Output "Rang $alterRang -> ${neuerRang}: laufende Brücke wird neu gestartet."
        $laufend | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
        for ($i = 0; $i -lt 20 -and (Bruecken-Prozesse).Count; $i++) { Start-Sleep -Milliseconds 250 }
        if ((Bruecken-Prozesse).Count) { throw 'Die alte Brücke ließ sich nicht beenden; der neue Rang gilt noch nicht.' }
    }
}
Start-ScheduledTask -TaskName 'JarvisPcBruecke'
$laeuft = $false
for ($i = 0; $i -lt 40 -and -not $laeuft; $i++) { Start-Sleep -Milliseconds 250; $laeuft = [bool](Bruecken-Prozesse).Count }
if ($laeuft) {
    Write-Output "JarvisPcBruecke eingerichtet und gestartet (Rang $neuerRang). Protokoll: %LOCALAPPDATA%\JarvisPcBruecke\bruecke.log"
} else {
    Write-Warning 'JarvisPcBruecke eingerichtet, aber die Brücke läuft nicht. Protokoll prüfen: %LOCALAPPDATA%\JarvisPcBruecke\bruecke.log'
    exit 1
}
