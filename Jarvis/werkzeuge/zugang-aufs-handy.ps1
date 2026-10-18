<#
.SYNOPSIS
    Bringt die Zugangsschlüssel von Jarvis aus ~/SK direkt in die App auf dem Handy (über adb, ohne APK).

.DESCRIPTION
    Schlüssel und Tokens stehen nicht mehr in der APK (wer sie hat, könnte sie auslesen). Dieses Skript baut aus ~/SK
    eine JSON-Datei und legt sie über run-as als files/zugang.json in den privaten Speicher von Jarvis. Die App liest
    sie beim nächsten Start ein, speichert die Werte verschlüsselt und löscht die Datei.

    Quellen (fehlt eine, bleibt der Wert in der App unverändert):
      server_token         ~/SK/Jarvis/handy-token.txt (eine Zeile; der Handy-Token des Relays)
      routine_token        ~/SK/Jarvis/routine.properties (token)
      such_schluessel      ~/SK/Tavily/tavily-api-key.txt
      maps_schluessel      ~/SK/Jarvis/maps-api-key.txt, sonst ~/SK/EntropieReductor/maps-api-key.txt
      drive_client_secret  ~/SK/rclone/google-oauth-client.txt (client_secret)

    Werte werden nie ausgegeben, nur die Namen der übertragenen Felder. Voraussetzung: die Debug-Fassung von Jarvis
    (run-as braucht eine debugfähige App; UpdateStation installiert Debug).

.PARAMETER Geraet
    adb-Seriennummer des Handys, z. B. 192.168.1.20:5555 (siehe "adb devices"; WLAN vor Kabel).

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File werkzeuge\zugang-aufs-handy.ps1 -Geraet 192.168.1.20:5555
#>
param([Parameter(Mandatory = $true)][string]$Geraet)

$ErrorActionPreference = 'Stop'
$adb = if ($env:ADB) { $env:ADB } else { (Get-Command adb -ErrorAction Stop).Source }
$sk = Join-Path $env:USERPROFILE 'SK'

# Paketname aus dem Bau lesen, nicht raten.
$gradle = Get-Content -Raw -Encoding utf8 -LiteralPath (Join-Path $PSScriptRoot '..\app\build.gradle.kts')
if ($gradle -notmatch 'applicationId\s*=\s*"([^"]+)"') { throw 'applicationId in app/build.gradle.kts nicht gefunden.' }
$paket = $Matches[1]

function Lies-Zeile([string]$pfad) {
    if (-not (Test-Path -LiteralPath $pfad -PathType Leaf)) { return $null }
    $wert = (Get-Content -Raw -Encoding utf8 -LiteralPath $pfad).Trim()
    if ($wert) { return $wert } else { return $null }
}

function Lies-Eigenschaft([string]$pfad, [string]$name) {
    if (-not (Test-Path -LiteralPath $pfad -PathType Leaf)) { return $null }
    foreach ($zeile in Get-Content -Encoding utf8 -LiteralPath $pfad) {
        # Wie java.util.Properties (das der Bau bisher benutzt hat): Trenner = oder :, Kommentare mit # oder !.
        if ($zeile -match '^\s*([^#!=:\s]+)\s*[=:]\s*(.*)$' -and $Matches[1] -eq $name) {
            $wert = $Matches[2].Trim()
            if ($wert) { return $wert }
        }
    }
    return $null
}

$werte = [ordered]@{}
$quellen = [ordered]@{
    server_token        = { Lies-Zeile (Join-Path $sk 'Jarvis\handy-token.txt') }
    routine_token       = { Lies-Eigenschaft (Join-Path $sk 'Jarvis\routine.properties') 'token' }
    such_schluessel     = { Lies-Zeile (Join-Path $sk 'Tavily\tavily-api-key.txt') }
    maps_schluessel     = { $m = Lies-Zeile (Join-Path $sk 'Jarvis\maps-api-key.txt'); if (-not $m) { $m = Lies-Zeile (Join-Path $sk 'EntropieReductor\maps-api-key.txt') }; $m }
    drive_client_secret = { Lies-Eigenschaft (Join-Path $sk 'rclone\google-oauth-client.txt') 'client_secret' }
}
foreach ($name in $quellen.Keys) {
    $wert = & $quellen[$name]
    if ($wert) { $werte[$name] = $wert } else { Write-Warning "$name fehlt in ~/SK, bleibt in der App unverändert." }
}
if ($werte.Count -eq 0) { throw 'Kein einziger Zugangswert in ~/SK gefunden, nichts zu übertragen.' }

$geraete = & $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '\tdevice$' } | ForEach-Object { ($_ -split '\t')[0] }
if ($geraete -notcontains $Geraet) { throw "Gerät $Geraet ist nicht verbunden (adb devices). Bei WLAN zuerst Werkzeuge\adb-wlan\adb-wlan.ps1 ausführen." }

$temp = Join-Path $env:TEMP ("jarvis-zugang-" + [guid]::NewGuid().ToString('N') + '.json')
try {
    # Zuerst leer anlegen und nur für den eigenen Benutzer lesbar machen, erst dann die Werte hineinschreiben.
    [System.IO.File]::WriteAllText($temp, '', [System.Text.UTF8Encoding]::new($false))
    & icacls.exe $temp /inheritance:r /grant:r "$($env:USERNAME):(R,W,D)" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Rechte der temporären Datei ließen sich nicht einschränken.' }
    [System.IO.File]::WriteAllText($temp, ($werte | ConvertTo-Json -Compress), [System.Text.UTF8Encoding]::new($false))
    $laenge = (Get-Item -LiteralPath $temp).Length

    # exec-in reicht stdin unverändert durch (kein Terminal, kein CRLF-Umbau). run-as schreibt als App-Benutzer.
    $befehl = "run-as $paket sh -c 'mkdir -p files && umask 077 && cat > files/zugang.json'"
    $p = Start-Process -FilePath $adb -ArgumentList @('-s', $Geraet, 'exec-in', "`"$befehl`"") -RedirectStandardInput $temp `
        -NoNewWindow -Wait -PassThru
    if ($p.ExitCode -ne 0) { throw "Übertragung fehlgeschlagen (adb Code $($p.ExitCode)). Ist die Debug-Fassung von $paket installiert?" }

    $angekommen = (& $adb -s $Geraet shell "run-as $paket stat -c %s files/zugang.json" 2>$null | Out-String).Trim()
    if ($angekommen -ne "$laenge") { throw "files/zugang.json fehlt oder ist unvollständig auf dem Handy (erwartet $laenge Byte, gefunden '$angekommen')." }
    Write-Output "Zugang übertragen ($($werte.Keys -join ', ')). Jarvis übernimmt ihn beim nächsten Start und löscht die Datei."
} finally {
    Remove-Item -LiteralPath $temp -Force -ErrorAction SilentlyContinue
}
