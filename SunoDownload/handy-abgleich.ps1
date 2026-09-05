#Requires -Version 5.1
<#
    Gleicht die MP3-Dateien aus C:\Suno Backup mit dem Ordner "Suno Backup" im
    internen Speicher des Handys ab und kopiert alles hinüber, was dort fehlt.

    Einbahnstraße: PC → Handy. Auf dem Handy wird nie etwas gelöscht oder
    überschrieben, außer eine Datei ist dort nachweislich unvollständig (andere
    Größe als auf dem PC) — dann ist sie ein Rest einer abgebrochenen Übertragung
    und wird erneut kopiert.

    Nur *.mp3 wird übertragen. Die Verwaltungsdateien des Downloaders
    (_bestand.json, _downloader-log.txt und dergleichen) bleiben auf dem PC.
#>

param(
    # Ohne Nachfrage am Ende — für Testläufe und den Aufruf aus anderen Skripten.
    [switch]$Still
)

# adb schreibt seinen Fortschritt auf die Fehlerausgabe ("1 file pushed …"). Mit
# ErrorActionPreference = Stop würde PowerShell daraus einen Abbruch machen — jeder
# erfolgreiche Push hätte das Skript getötet. Fehler werden hier deshalb am
# Rückgabewert erkannt, nicht am Kanal.
$ErrorActionPreference = 'Continue'

$QUELLE = 'C:\Suno Backup'
$ZIEL   = '/sdcard/Suno Backup'

# Umlaute in Dateinamen gehen sonst auf dem Weg zu adb kaputt.
try {
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
    $OutputEncoding = [System.Text.Encoding]::UTF8
} catch { }

$Host.UI.RawUI.WindowTitle = 'Suno Backup - Abgleich mit dem Handy'

function Schreib([string]$Text, [string]$Farbe = 'Gray') { Write-Host $Text -ForegroundColor $Farbe }

Write-Host ''
Schreib '  ════════════════════════════════════════════════════════════════' Cyan
Schreib '   Suno Backup — Abgleich mit dem Handy' Cyan
Schreib '  ════════════════════════════════════════════════════════════════' Cyan
Write-Host ''

function Beende([int]$Code) {
    Write-Host ''
    if (-not $Still) { Read-Host '  Enter zum Schließen' | Out-Null }
    exit $Code
}

# ---------------------------------------------------------------- adb finden

function Finde-Adb {
    $treffer = Get-Command adb.exe -ErrorAction SilentlyContinue
    if ($treffer) { return $treffer.Source }
    $orte = @(
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
        "$env:USERPROFILE\AppData\Local\Android\Sdk\platform-tools\adb.exe",
        "$env:ProgramFiles\Android\platform-tools\adb.exe",
        "${env:ProgramFiles(x86)}\Android\android-sdk\platform-tools\adb.exe",
        'C:\platform-tools\adb.exe'
    )
    foreach ($o in $orte) { if (Test-Path $o) { return $o } }
    return $null
}

$adb = Finde-Adb
if (-not $adb) {
    Schreib '  ❗ adb.exe wurde nicht gefunden.' Red
    Schreib '     Erwartet unter %LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe' DarkGray
    Beende 1
}

<#
    Ruft adb auf und sammelt beide Ausgabekanäle als Text ein. Erfolg oder
    Misserfolg steht ausschließlich im Rückgabewert — was adb auf die
    Fehlerausgabe schreibt, ist bei diesem Werkzeug meist eine Erfolgsmeldung.
#>
function Ruf-Adb {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Argumente)
    $alle = @()
    if ($script:serie) { $alle += @('-s', $script:serie) }
    $alle += $Argumente
    $zeilen = @(& $script:adb @alle 2>&1 | ForEach-Object { "$_" })
    return [pscustomobject]@{ Code = $LASTEXITCODE; Zeilen = $zeilen; Text = ($zeilen -join ' ').Trim() }
}

# ---------------------------------------------------------------- Quelle prüfen

if (-not (Test-Path -LiteralPath $QUELLE)) {
    Schreib "  ❗ Der Ordner $QUELLE gibt es nicht." Red
    # Ein umbenannter Ordner ist der wahrscheinlichste Grund — dann steht das Ziel daneben.
    $eltern = Split-Path $QUELLE -Parent
    $nah = Get-ChildItem -LiteralPath $eltern -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match 'backup|suno|sono' } | Select-Object -First 5
    if ($nah) {
        Schreib '     Diese Ordner daneben könnten gemeint sein:' DarkGray
        foreach ($n in $nah) { Schreib "       $($n.FullName)" DarkGray }
    }
    Beende 1
}

# ---------------------------------------------------------------- Handy suchen

Schreib '  Handy suchen …' DarkGray
Ruf-Adb start-server | Out-Null

function Hole-Geraete {
    $zeilen = (Ruf-Adb devices).Zeilen
    $liste = @()
    foreach ($z in $zeilen) {
        $t = "$z".Trim()
        if ($t -match '^(\S+)\s+(device|unauthorized|offline)$') {
            $liste += [pscustomobject]@{ Seriennummer = $Matches[1]; Zustand = $Matches[2] }
        }
    }
    return $liste
}

$geraete = @(Hole-Geraete)
$bereit  = @($geraete | Where-Object { $_.Zustand -eq 'device' })

if ($bereit.Count -eq 0) {
    if ($geraete | Where-Object { $_.Zustand -eq 'unauthorized' }) {
        Schreib '  ❗ Das Handy ist angeschlossen, hat den Rechner aber noch nicht bestätigt.' Red
        Schreib '     Auf dem Handy-Bildschirm "USB-Debugging zulassen" antippen und neu starten.' DarkGray
    } else {
        Schreib '  ❗ Kein Handy gefunden.' Red
        Schreib '     Handy per USB anschließen, entsperren und USB-Debugging einschalten.' DarkGray
        Schreib '     (Einstellungen → Entwickleroptionen → USB-Debugging)' DarkGray
    }
    Beende 1
}
if ($bereit.Count -gt 1) {
    Schreib "  ❗ Es sind $($bereit.Count) Geräte angeschlossen — bitte nur eines anschließen:" Red
    foreach ($g in $bereit) { Schreib "       $($g.Seriennummer)" DarkGray }
    Beende 1
}

$serie = $bereit[0].Seriennummer
$name  = ((Ruf-Adb shell getprop ro.product.model).Zeilen | Select-Object -First 1)
Schreib "  Handy: $("$name".Trim()) ($serie)" Green

# ---------------------------------------------------------------- Listen holen

# Zielordner anlegen, falls er auf dem Handy noch nicht da ist.
Ruf-Adb shell "mkdir -p '$ZIEL'" | Out-Null

Schreib "  MP3-Dateien in $QUELLE zählen …" DarkGray
$lokal = @(Get-ChildItem -LiteralPath $QUELLE -Filter *.mp3 -File -ErrorAction SilentlyContinue)
if ($lokal.Count -eq 0) {
    Schreib '  ❗ Im Quellordner liegt keine einzige MP3-Datei.' Red
    Beende 1
}

Schreib "  MP3-Dateien auf dem Handy zählen …" DarkGray
# stat liefert Größe und Name in einem Rutsch; das ist der einzige Weg, eine
# abgebrochene Übertragung zu erkennen. Kann das Handy kein stat, wird nur nach
# dem Namen verglichen — dann bleibt eine halbe Datei eben liegen.
$roh = (Ruf-Adb shell "cd '$ZIEL' 2>/dev/null && stat -c '%s|%n' *.mp3 2>/dev/null").Zeilen
$aufHandy = @{}
$mitGroesse = $true
foreach ($z in $roh) {
    $t = "$z".Trim()
    if ($t -match '^(\d+)\|(.+\.mp3)$') { $aufHandy[$Matches[2].ToLower()] = [int64]$Matches[1] }
}
if ($aufHandy.Count -eq 0) {
    $mitGroesse = $false
    $roh = (Ruf-Adb shell "cd '$ZIEL' 2>/dev/null && ls -1 *.mp3 2>/dev/null").Zeilen
    foreach ($z in $roh) {
        $t = "$z".Trim()
        if ($t -like '*.mp3') { $aufHandy[$t.ToLower()] = -1 }
    }
}

Write-Host ''
Schreib "  Auf dem Rechner : $($lokal.Count) MP3-Dateien" Gray
Schreib "  Auf dem Handy   : $($aufHandy.Count) MP3-Dateien" Gray
if (-not $mitGroesse -and $aufHandy.Count -gt 0) {
    Schreib '  (Das Handy meldet keine Dateigrößen — es wird nur nach Namen verglichen.)' DarkYellow
}

# ---------------------------------------------------------------- Vergleich

$zuKopieren = @()
$unvollstaendig = 0
foreach ($f in $lokal) {
    $schluessel = $f.Name.ToLower()
    if (-not $aufHandy.ContainsKey($schluessel)) {
        $zuKopieren += $f
    } elseif ($mitGroesse -and $aufHandy[$schluessel] -ne $f.Length) {
        # Andere Größe = Rest einer abgebrochenen Übertragung, wird ersetzt.
        $zuKopieren += $f
        $unvollstaendig++
    }
}

# Nur zur Information — gelöscht wird auf dem Handy nichts.
$nurAufHandy = 0
$lokalNamen = @{}
foreach ($f in $lokal) { $lokalNamen[$f.Name.ToLower()] = $true }
foreach ($k in $aufHandy.Keys) { if (-not $lokalNamen.ContainsKey($k)) { $nurAufHandy++ } }
if ($nurAufHandy -gt 0) {
    Schreib "  Nur auf dem Handy: $nurAufHandy Dateien — die bleiben unangetastet." DarkGray
}

Write-Host ''
if ($zuKopieren.Count -eq 0) {
    Schreib '  ✅ Das Handy ist auf dem neuesten Stand — nichts zu kopieren.' Green
    Beende 0
}

$mbGesamt = [math]::Round((($zuKopieren | Measure-Object -Property Length -Sum).Sum) / 1MB, 1)
Schreib "  $($zuKopieren.Count) Dateien werden kopiert ($mbGesamt MB)." Cyan
if ($unvollstaendig -gt 0) {
    Schreib "  Davon $unvollstaendig unvollständige, die erneut übertragen werden." DarkYellow
}
Write-Host ''

# ---------------------------------------------------------------- Kopieren

$start = Get-Date
$fertig = 0
$fehler = @()

foreach ($f in $zuKopieren) {
    $nr = $fertig + $fehler.Count + 1
    $anzeige = if ($f.Name.Length -gt 46) { $f.Name.Substring(0, 45) + '…' } else { $f.Name }
    Write-Host ("`r  {0,5} / {1}  {2}" -f $nr, $zuKopieren.Count, $anzeige.PadRight(47)) -NoNewline

    $ergebnis = Ruf-Adb push $f.FullName "$ZIEL/$($f.Name)"
    # adb meldet Erfolg auch auf der Fehlerausgabe — der Rückgabewert entscheidet.
    if ($ergebnis.Code -eq 0) {
        $fertig++
    } else {
        $fehler += [pscustomobject]@{ Datei = $f.Name; Grund = ($ergebnis.Text -replace '\s+', ' ') }
    }
}
Write-Host ''

# Der Medienspeicher kennt die neuen Dateien sonst erst nach einem Neustart —
# dann fehlen sie in der Musik-App im Auto. Beide Wege still versuchen.
if ($fertig -gt 0) {
    Schreib '  Medienverzeichnis auffrischen …' DarkGray
    Ruf-Adb shell "content call --uri content://media --method scan_volume --arg external_primary" | Out-Null
    foreach ($f in ($zuKopieren | Select-Object -First 60)) {
        $pfad = "$ZIEL/$($f.Name)"
        Ruf-Adb shell "am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d 'file://$pfad'" | Out-Null
    }
}

# ---------------------------------------------------------------- Bilanz

$dauer = [math]::Round(((Get-Date) - $start).TotalSeconds)
$tempo = if ($dauer -gt 0) { [math]::Round($mbGesamt / $dauer, 1) } else { $mbGesamt }

Write-Host ''
Schreib '  ════════════════════════════════════════════════════════════════' Cyan
if ($fehler.Count -eq 0) {
    Schreib "   Fertig: $fertig Dateien kopiert · $mbGesamt MB · $dauer s ($tempo MB/s)" Green
} else {
    Schreib "   Fertig: $fertig kopiert · $($fehler.Count) fehlgeschlagen · $dauer s" Yellow
}
Schreib "   Auf dem Handy liegen jetzt $($aufHandy.Count + $fertig) MP3-Dateien." Gray
Schreib '  ════════════════════════════════════════════════════════════════' Cyan

if ($fehler.Count -gt 0) {
    Write-Host ''
    Schreib '  Nicht geklappt hat:' Yellow
    foreach ($e in ($fehler | Select-Object -First 15)) {
        Schreib "   ❗ $($e.Datei) — $($e.Grund)" Yellow
    }
    if ($fehler.Count -gt 15) { Schreib "   … und $($fehler.Count - 15) weitere." Yellow }
    Schreib '  Einfach noch einmal starten — Fertiges wird übersprungen.' DarkGray
    Beende 1
}

Beende 0
