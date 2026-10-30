# Installiert eine Jarvis-APK per adb (Tunnel, WLAN, notfalls Kabel), aber erst nach Rückfrage in Jarvis:
# Das Fenster bietet „Sofort“ oder „In 1 bis 5 Minuten“, damit eine laufende Aufnahme nicht abbricht.
# Aufruf: jarvis-install.ps1 <apk> [-Geraet <gerät>]
# Letzte Zeile JARVIS_INSTALL_STATUS: installiert, abgebrochen, keine-antwort oder fehler.
param([Parameter(Mandatory)][string]$Apk, [string]$Geraet = "")

$paket = "de.frank.jarvis"
$fenster = "$paket/.ui.InstallFrageActivity"
$antwort = "/sdcard/Android/data/$paket/files/install-antwort.txt"
$warteS = 240
$wlanSkript = Join-Path (Split-Path $PSScriptRoot) "adb-wlan\adb-wlan.ps1"

function Ende([string]$status, [string]$text = "") {
    if ($text) { "JARVIS_INSTALL_STATUS=$status ($text)" } else { "JARVIS_INSTALL_STATUS=$status" }
    if ($status -eq "fehler") { exit 1 }
    exit 0
}

if (-not (Test-Path $Apk)) { Ende fehler "APK nicht gefunden: $Apk" }
$adb = @($env:ADB, "$env:ANDROID_HOME\platform-tools\adb.exe", "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe", (Get-Command adb -ErrorAction SilentlyContinue).Source) |
    Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1
if (-not $adb) { Ende fehler "adb nicht gefunden" }

# WLAN oder Tunnel (IP:Port) vor Kabel; ohne Verbindung einmal das WLAN-Werkzeug fragen.
function Waehle-Geraet {
    $bereit = & $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\sdevice$" } | ForEach-Object { ($_ -split "\s+")[0] }
    $netz = $bereit | Where-Object { $_ -match ":\d+$" } | Select-Object -First 1
    if ($netz) { return $netz }
    return $bereit | Select-Object -First 1
}
function Verbinde {
    if ($script:Geraet -and (& $adb -s $script:Geraet get-state 2>$null) -eq "device") { return $true }
    $script:Geraet = Waehle-Geraet
    if ($script:Geraet) { return $true }
    & pwsh -NoProfile -File $wlanSkript *> $null
    $script:Geraet = Waehle-Geraet
    return [bool]$script:Geraet
}
function Installiere {
    if (-not (Verbinde)) { Ende fehler "Handy nicht erreichbar" }
    "Installiere über $script:Geraet ..."
    $ausgabe = & $adb -s $script:Geraet install -r $Apk 2>&1 | Out-String
    $ausgabe.Trim()
    if ($ausgabe -match "(?m)^Success") { Ende installiert $script:Geraet }
    Ende fehler "adb install gescheitert: $($ausgabe.Trim() -split "`n" | Select-Object -Last 1)"
}

if (-not (Verbinde)) { Ende fehler "Handy nicht erreichbar" }

$kennung = "$([DateTimeOffset]::Now.ToUnixTimeSeconds())-$PID"
& $adb -s $Geraet shell rm -f $antwort *> $null
$start = & $adb -s $Geraet shell am start -n $fenster --es kennung $kennung 2>&1 | Out-String
if ($start -match "does not exist") {
    # Die installierte Version kennt das Fenster noch nicht: nur dann ohne Rückfrage.
    "Installierte Jarvis-Version hat das Fenster noch nicht, installiere ohne Rückfrage."
    Installiere
}
if ($start -match "(?m)^Error.*") { Ende fehler "Fenster ließ sich nicht öffnen: $($Matches[0])" }
"Fenster in Jarvis geöffnet, warte auf Antwort (höchstens $warteS s) ..."

$bis = (Get-Date).AddSeconds($warteS)
$wahl = ""
while ((Get-Date) -lt $bis) {
    $zeile = (& $adb -s $Geraet shell cat $antwort 2>$null | Out-String).Trim()
    if ($zeile.StartsWith("$kennung ")) { $wahl = $zeile.Substring($kennung.Length + 1); break }
    Start-Sleep -Seconds 2
}
& $adb -s $Geraet shell rm -f $antwort *> $null

switch -Regex ($wahl) {
    "^$" { Ende keine-antwort "nichts geändert" }
    "^abbruch$" { Ende abgebrochen "nichts geändert" }
    "^0$" { "Gewählt: Sofort"; break }
    "^[1-5]$" { "Gewählt: in $wahl Minute(n), Installation um $((Get-Date).AddMinutes([int]$wahl).ToString('HH:mm')) Uhr"; Start-Sleep -Seconds ([int]$wahl * 60); break }
    default { Ende fehler "unbekannte Antwort: $wahl" }
}
Installiere
