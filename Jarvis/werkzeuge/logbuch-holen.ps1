<#
.SYNOPSIS
    Holt das Logbuch von Jarvis vom Handy auf den PC (über adb, bevorzugt per WLAN).

.DESCRIPTION
    Jarvis schreibt je Tag eine Datei filesDir/logbuch/JJJJ-MM-TT.jsonl (eine JSON-Zeile je Ereignis:
    z = Zeit, k = Art, e = Ereignis, d = Einzelheiten, ms = Dauer, v = Version). Dieses Skript kopiert sie nach
    Jarvis/logs/<Zeitstempel>/ und legt dazu das Android-Log (logcat) der Jarvis-Teile und eine kurze Zusammenfassung ab.
    Danach kann eine KI die Dateien lesen und die App gezielt verbessern ("schau dir die Logs an").

    Voraussetzung: die Debug-Fassung von Jarvis (run-as braucht eine debugfähige App; UpdateStation installiert Debug).

.PARAMETER Tage
    Nur die letzten N Tage holen (Vorgabe: alle vorhandenen, höchstens 14).
#>
param([int]$Tage = 14)

$ErrorActionPreference = 'Stop'
$paket = 'de.frank.jarvis'
$adb = if ($env:ADB) { $env:ADB } else { 'adb' }

# Gerät wählen: WLAN (IP:5555) vor Kabel.
$geraete = & $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '\tdevice$' } | ForEach-Object { ($_ -split '\t')[0] }
if (-not $geraete) {
    Write-Host 'Kein Handy verbunden. Versuche WLAN-Verbindung ...'
    powershell -ExecutionPolicy Bypass -File "$env:USERPROFILE\proggs\Werkzeuge\adb-wlan\adb-wlan.ps1" | Out-Null
    $geraete = & $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '\tdevice$' } | ForEach-Object { ($_ -split '\t')[0] }
}
$geraet = ($geraete | Where-Object { $_ -match ':\d+$' } | Select-Object -First 1)
if (-not $geraet) { $geraet = $geraete | Select-Object -First 1 }
if (-not $geraet) { throw 'Kein Handy erreichbar (weder WLAN noch Kabel).' }

$ziel = Join-Path $PSScriptRoot ("..\logs\" + (Get-Date -Format 'yyyy-MM-dd_HHmm'))
New-Item -ItemType Directory -Force $ziel | Out-Null
$ziel = (Resolve-Path $ziel).Path

$dateien = & $adb -s $geraet shell "run-as $paket ls files/logbuch" 2>$null | ForEach-Object { $_.Trim() } | Where-Object { $_ -match '^\d{4}-\d{2}-\d{2}\.jsonl$' } | Sort-Object -Descending | Select-Object -First $Tage
if (-not $dateien) { Write-Warning 'Kein Logbuch gefunden (ältere Jarvis-Fassung oder keine Debug-Fassung installiert).' }
foreach ($d in $dateien) {
    # exec-out liefert die Bytes unverändert (kein CRLF-Umbau wie bei "shell").
    $aus = Join-Path $ziel $d
    $p = Start-Process -FilePath $adb -ArgumentList @('-s', $geraet, 'exec-out', "run-as $paket cat files/logbuch/$d") -RedirectStandardOutput $aus -NoNewWindow -PassThru -Wait
    if ($p.ExitCode -ne 0) { Write-Warning "$d ließ sich nicht holen." }
}

# Android-Log der Jarvis-Teile (alle Tags beginnen mit "Jarvis" oder stammen aus dem Prozess).
$prozess = (& $adb -s $geraet shell pidof $paket 2>$null).Trim()
if ($prozess) { & $adb -s $geraet logcat -d --pid=$prozess | Out-File (Join-Path $ziel 'logcat-jarvis.txt') -Encoding utf8 }
& $adb -s $geraet logcat -d -b crash | Out-File (Join-Path $ziel 'logcat-abstuerze.txt') -Encoding utf8

# Kurze Zusammenfassung für den schnellen Überblick.
$zeilen = Get-ChildItem $ziel -Filter '*.jsonl' | ForEach-Object { Get-Content $_.FullName -Encoding utf8 } | Where-Object { $_ } | ForEach-Object { try { $_ | ConvertFrom-Json } catch { } }
$text = @("LOGBUCH JARVIS, geholt $(Get-Date -Format 'dd.MM.yyyy HH:mm') von $geraet", "Einträge: $($zeilen.Count)", '')
$text += 'Je Art:'
$text += $zeilen | Group-Object k | Sort-Object Count -Descending | ForEach-Object { "  $($_.Name): $($_.Count)" }
$text += '', 'Häufigste Fehler:'
$text += $zeilen | Where-Object { $_.k -eq 'fehler' } | Group-Object e | Sort-Object Count -Descending | Select-Object -First 15 | ForEach-Object { "  $($_.Count)x $($_.Name)" }
$text += '', 'Langsamste Abläufe (Schnitt ms / max ms / Anzahl):'
$text += $zeilen | Where-Object { $null -ne $_.ms } | Group-Object e | ForEach-Object {
    $m = $_.Group | Measure-Object ms -Average -Maximum
    [pscustomobject]@{ e = $_.Name; schnitt = [int]$m.Average; max = [int]$m.Maximum; n = $m.Count }
} | Sort-Object schnitt -Descending | Select-Object -First 20 | ForEach-Object { "  $($_.schnitt) / $($_.max) / $($_.n)x  $($_.e)" }
$text | Out-File (Join-Path $ziel 'zusammenfassung.txt') -Encoding utf8

Write-Host "Logbuch liegt in $ziel"
Get-Content (Join-Path $ziel 'zusammenfassung.txt') -Encoding utf8
