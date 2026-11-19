# PreToolUse-Wächter: erzwingt den vorgeschriebenen Deploy-Weg für den OpenLauncher.
#
# Hintergrund: Vor Einführung von update-launcher.ps1 wurde mehrfach die ALTE EXE gestartet und
# als neue Version gemeldet — Frank sah seine Verbesserungen nicht, der Agent behauptete "das ist
# die neue". Das Skript schließt das aus: es prüft den Build-Exit-Code, prüft dass überhaupt eine
# neue Datei entstand und erkennt einen sofortigen Absturz der neuen Version. Von Hand
# (Stop-Process + dotnet build + Start-Process) fehlen genau diese Prüfungen.
#
# Der Wächter blockiert deshalb Bauen, Beenden und Starten des Launchers von Hand und verweist auf
# update-launcher.ps1. Nur-Lese-Befehle (Get-Process, Get-Item, Test-Path …) bleiben erlaubt, damit
# die Versionsprüfung nach dem Deploy weiterhin möglich ist.
#
# Notausstieg: enthält der Befehl den Marker "guard-aus", läuft er durch. Der Marker muss bewusst
# hingeschrieben werden und steht sichtbar im Befehl — ein versehentliches Umgehen ist damit
# ausgeschlossen, ein absichtliches nachvollziehbar.

$ErrorActionPreference = 'Stop'

try {
    $raw = [Console]::In.ReadToEnd()
    if ([string]::IsNullOrWhiteSpace($raw)) { exit 0 }
    $payload = $raw | ConvertFrom-Json
} catch {
    # Unlesbare Eingabe darf niemals die Arbeit blockieren.
    exit 0
}

$command = $payload.tool_input.command
if ([string]::IsNullOrWhiteSpace($command)) { exit 0 }

# 1. Der vorgeschriebene Weg selbst und der bewusste Notausstieg laufen immer durch.
#    update-launcher zählt nur als echter Skriptaufruf (Befehlsanfang, -File, &, pwsh/bash), nicht als Text irgendwo im Befehl.
$skriptAufruf = '(?:^|[;&|\n(]\s*|-File\s+|&\s*|\b(?:pwsh|powershell|bash|sh|zsh)(?:\s+-\S+)*\s+)["'']?[^\s;&|"'']*update-launcher\.(?:ps1|sh)\b'
if ($command -match $skriptAufruf -or $command -match 'guard-aus') { exit 0 }

# 2. Nur Befehle, die den Launcher überhaupt betreffen.
#    Groß-/Kleinschreibung beachten: der Zustandsordner ~/.openlauncher (Vorwärmen, Repo-Status) ist nicht der Launcher.
#    Seit dem Review ohne Rücksicht auf die Schreibweise (taskkill /IM openlauncher.exe), dafür vorher entfernt: der
#    Zustandsordner .openlauncher/, die Profile unter OpenLauncher/Profiles (Skills, Memory, HTML) und OpenLauncherServer.
$bezug = $command -replace '(?i)\.openlauncher[\\/]', '' -replace '(?i)OpenLauncher[\\/]+Profiles\b', '' -replace '(?i)OpenLauncherServer', ''
if ($bezug -notmatch 'OpenLauncher') { exit 0 }

# Text in Anführungszeichen (Commit-Nachrichten, grep-Muster, echo) ist kein Handgriff: für die Suche ausblenden.
# Pfade in Anführungszeichen bleiben für Schritt 2 sichtbar (oben geprüft), ausgeführter Text (-Command "…", -c "…") auch.
$ohneText = [regex]::Replace($command, '"[^"]*"|''[^'']*''', {
    param($m)
    $davor = $command.Substring(0, $m.Index)
    if ($davor -match '(?:\s-c|\s-Command|\beval)\s*$') { return $m.Value }
    return '""'
})

# 3. Die drei Handgriffe, die update-launcher.ps1 ersetzt. Lesende Cmdlets stehen bewusst NICHT
#    hier: Get-Process/Get-Item auf die EXE bleiben erlaubt (Versionsprüfung nach dem Deploy).
$verbote = @(
    @{ Muster = 'dotnet\s+(build|publish|msbuild)'; Handgriff = 'Bauen' }
    @{ Muster = 'Stop-Process|taskkill|CloseMainWindow|\.Kill\(\)'; Handgriff = 'Beenden' }
    @{ Muster = 'Start-Process|Invoke-Item'; Handgriff = 'Starten' }
)

$treffer = @($verbote | Where-Object { $ohneText -match $_.Muster } | ForEach-Object { $_.Handgriff })
if ($treffer.Count -eq 0) { exit 0 }

$grund = @"
Blockiert: $($treffer -join ' + ') des OpenLaunchers von Hand.

Nimm stattdessen den vorgeschriebenen Weg:
    pwsh C:\Users\barwa\proggs\OpenLauncher\update-launcher.ps1

Warum: Vor diesem Skript wurde mehrfach die ALTE EXE gestartet und als neue Version gemeldet.
Das Skript prueft den Build-Exit-Code, prueft dass eine neue Datei entstand und erkennt einen
sofortigen Absturz der neuen Version. Es fragt Frank per Dialog und schliesst den Launcher
sauber statt ihn abzuschiessen. Siehe OpenLauncher/SETUP.md, Schritt 5 und 6.

Nach dem Deploy die laufende Version auslesen und die Nummer nennen (Get-Process ist erlaubt).
"@

$antwort = @{
    hookSpecificOutput = @{
        hookEventName            = 'PreToolUse'
        permissionDecision       = 'deny'
        permissionDecisionReason = $grund
    }
}
$antwort | ConvertTo-Json -Depth 5 -Compress
exit 0
