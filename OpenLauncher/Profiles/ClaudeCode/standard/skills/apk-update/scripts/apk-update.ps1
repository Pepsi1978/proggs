# apk-update.ps1 - baut die Update-APK eines Android-Projekts und legt sie mit update.json
# in den Google-Drive-Ordner "Meine Ablage\Dokumente\Updates\<Projekt>".
# Die Handy-App UpdateStation liest update.json und vergleicht versionCode + Signatur.
#
# Aufruf: pwsh -File apk-update.ps1 -Projekt FisetinBegleiter [-OhneBuild]
# Ausgabe-Zeilen mit Präfix APK_UPDATE_ sind für den Skill maschinenlesbar.
# Exit 0 = veröffentlicht (lokal bereit), 1 = Fehler, 2 = Versionslog vorbereitet, erst committen.

param(
    [Parameter(Mandatory = $true)][string]$Projekt,
    [switch]$OhneBuild,
    # Nur für Tests: andere Wurzeln statt ~/proggs und des Drive-Ordners.
    [string]$ProggsWurzel = (Join-Path $env:USERPROFILE 'proggs'),
    [string]$UpdatesWurzel = (Join-Path $env:USERPROFILE 'Meine Ablage\Dokumente\Updates')
)

$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding($false)

$script:aufraeumen = [System.Collections.Generic.List[string]]::new()
$script:sperre = $null
function Aufraeumen {
    foreach ($f in $script:aufraeumen) { Remove-Item -LiteralPath $f -Force -ErrorAction SilentlyContinue }
    if ($script:sperre) { $script:sperre.Dispose(); $script:sperre = $null }
}
function Fehler([string]$text) {
    Aufraeumen
    Write-Host "APK_UPDATE_STATUS=fehler"
    Write-Host "APK_UPDATE_FEHLER=$text"
    exit 1
}
# Jeder unerwartete Fehler (Datei-, Git-, JSON-Operationen; ErrorActionPreference=Stop) endet
# maschinenlesbar mit genau einer Statuszeile und gibt Sperre und Temp-Dateien frei.
trap {
    Fehler "Unerwarteter Fehler: $($_.Exception.Message) (Zeile $($_.InvocationInfo.ScriptLineNumber))"
}

$skillDir   = Split-Path -Parent $PSScriptRoot
$proggs     = [IO.Path]::GetFullPath($ProggsWurzel).TrimEnd('\')
$updatesDir = [IO.Path]::GetFullPath($UpdatesWurzel).TrimEnd('\')
$keystore   = Join-Path $env:USERPROFILE 'SK\Android\debug-shared.keystore'

# --- Projekt sicher bestimmen ---------------------------------------------------------------
# Der Name wird Ordnername in Drive: nur einfache Zeichen, kein Pfad, direkt unter ~/proggs.
if ($Projekt -notmatch '^[A-Za-z0-9][A-Za-z0-9_.\-]{0,63}$' -or $Projekt.Contains('..')) {
    Fehler "Ungültiger Projektname '$Projekt' (erlaubt: Buchstaben, Ziffern, _ . -; kein Pfad)."
}
$projektDir = [IO.Path]::GetFullPath((Join-Path $proggs $Projekt))
if ((Split-Path $projektDir -Parent) -ne $proggs -or -not (Test-Path $projektDir -PathType Container)) {
    Fehler "Projektordner $projektDir fehlt oder liegt nicht direkt unter $proggs."
}
# Schreibweise vom Datenträger übernehmen, damit Drive-Ordner und Projekt gleich heißen.
$Projekt = (Get-Item $projektDir).Name

# --- Exklusiv pro Projekt: parallele Läufe (andere Sitzungen) dürfen sich nicht überschreiben ---
# Die Sperrdatei verschwindet beim Schließen; endet der Prozess, gibt Windows sie ebenfalls frei.
$lauf = [guid]::NewGuid().ToString('N').Substring(0, 8)
$sperrDir = Join-Path ([IO.Path]::GetTempPath()) 'apk-update-sperren'
New-Item -ItemType Directory -Force -Path $sperrDir | Out-Null
try {
    $script:sperre = [IO.FileStream]::new((Join-Path $sperrDir "$($Projekt.ToLowerInvariant()).lock"),
        [IO.FileMode]::OpenOrCreate, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None, 4096, [IO.FileOptions]::DeleteOnClose)
} catch {
    Fehler "$Projekt wird gerade von einem anderen apk-update-Lauf gebaut oder veröffentlicht. Warten und erneut starten."
}

$gradleFile = Join-Path $projektDir 'app\build.gradle.kts'
if (-not (Test-Path $gradleFile)) {
    if (Test-Path (Join-Path $projektDir 'app\build.gradle')) {
        Fehler "Groovy-Buildskript (app/build.gradle) wird nicht automatisch umgestellt. Versionslog von Hand einbinden (SKILL.md, Abschnitt Versionslog)."
    }
    Fehler "Kein Android-Projekt: $gradleFile fehlt."
}

# --- Konfiguration --------------------------------------------------------------------------
$cfg = Get-Content (Join-Path $skillDir 'projekte.json') -Raw -Encoding utf8 | ConvertFrom-Json
$eintrag = $cfg.projekte.$Projekt
$gradleTask = if ($eintrag.gradleTask) { $eintrag.gradleTask } else { $cfg.standard.gradleTask }
$apkOrdner  = if ($eintrag.apkOrdner)  { $eintrag.apkOrdner }  else { $cfg.standard.apkOrdner }

$gradleText = [IO.File]::ReadAllText($gradleFile)
$erwartetesPaket = $eintrag.paket
if (-not $erwartetesPaket) {
    $ids = [regex]::Matches($gradleText, 'applicationId\s*=\s*"([^"]+)"')
    if ($ids.Count -ne 1) { Fehler "applicationId in build.gradle.kts nicht eindeutig ($($ids.Count) Treffer). Paket in projekte.json eintragen." }
    $erwartetesPaket = $ids[0].Groups[1].Value
    Write-Host "APK_UPDATE_NEUES_PROJEKT=$Projekt (nicht in projekte.json; Standard $gradleTask, Paket $erwartetesPaket)"
}

# --- Build-Tools ----------------------------------------------------------------------------
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$bt = Get-ChildItem (Join-Path $sdk 'build-tools') -Directory |
    Where-Object { Test-Path (Join-Path $_.FullName 'apksigner.bat') } |
    Sort-Object { [version]($_.Name -replace '[^\d\.].*$', '') } | Select-Object -Last 1
if (-not $bt) { Fehler "Keine Android build-tools mit apksigner gefunden unter $sdk." }
$apksigner = Join-Path $bt.FullName 'apksigner.bat'
$aapt2     = Join-Path $bt.FullName 'aapt2.exe'

# --- Zuletzt veröffentlichte Version --------------------------------------------------------
# update.json zählt, abgesichert durch die höchste -vcN.apk im Ordner. Eine kaputte update.json
# darf nie zu "noch nichts veröffentlicht" (0) werden.
$zielDir = Join-Path $updatesDir $Projekt
$manifestPfad = Join-Path $zielDir 'update.json'
function VcNummer([string]$name) { $m = [regex]::Match($name, '-vc(\d+)\.apk$'); if ($m.Success) { [int]$m.Groups[1].Value } else { 0 } }
$hoechsteApk = 0
if (Test-Path $zielDir) {
    $hoechsteApk = (@(Get-ChildItem $zielDir -Filter *.apk | ForEach-Object { VcNummer $_.Name }) + 0 | Measure-Object -Maximum).Maximum
}
$letzterCode = 0
$alteApk = $null
if (Test-Path $manifestPfad) {
    try {
        $alt = Get-Content $manifestPfad -Raw -Encoding utf8 | ConvertFrom-Json
        $letzterCode = [int]$alt.versionCode
        $alteApk = "$($alt.apk)"
        if ($letzterCode -le 0) { throw "versionCode fehlt" }
    } catch {
        if ($hoechsteApk -le 0) { Fehler "update.json in $zielDir ist unlesbar und keine APK liegt daneben. Datei prüfen, nicht blind überschreiben." }
        Write-Host "WARNUNG: update.json unlesbar, nutze höchste APK-Nummer $hoechsteApk als veröffentlichte Version."
        $letzterCode = 0
    }
}
$letzterCode = [Math]::Max($letzterCode, $hoechsteApk)

# --- Am Handy installierte Version (falls per adb erreichbar, WLAN-Gerät bevorzugt) ---------
$installiertCode = 0
$adb = Join-Path $sdk 'platform-tools\adb.exe'
if (-not (Test-Path $adb)) { $adb = 'adb' }
try {
    $geraete = @(& $adb devices 2>$null | Select-String '^(\S+)\s+device$' | ForEach-Object { $_.Matches[0].Groups[1].Value })
    $geraet = ($geraete | Where-Object { $_ -match ':' } | Select-Object -First 1)
    if (-not $geraet) { $geraet = $geraete | Select-Object -First 1 }
    if ($geraet) {
        $d = & $adb -s $geraet shell dumpsys package $erwartetesPaket 2>$null | Select-String 'versionCode=(\d+)' | Select-Object -First 1
        if ($d) { $installiertCode = [int]$d.Matches[0].Groups[1].Value }
        Write-Host "Am Handy ($geraet) installiert: versionCode $installiertCode"
    }
} catch { }
# Neu: kein Zwangs-Bump beim ersten Update. Strikt höher als veröffentlicht UND installiert.
$mindest = [Math]::Max($letzterCode, $installiertCode)

# --- Versionslog: einzige Quelle für versionCode, versionName und Stand ----------------------
$logRel = "$Projekt/app/src/main/assets/versionslog.json"
$gradleRel = "$Projekt/app/build.gradle.kts"
$logPfad = Join-Path $projektDir 'app\src\main\assets\versionslog.json'
$eintraege = [System.Collections.Generic.List[object]]::new()
# Vom Skript geänderte Projektdateien: dann wird NICHT veröffentlicht, erst committen und pushen.
$vorbereitet = [System.Collections.Generic.List[string]]::new()

function J([string]$s) { '"' + ($s -replace '\\', '\\' -replace '"', '\"' -replace "`r?`n", ' ') + '"' }
function Schreibe-Versionslog {
    $nl = "`n"
    $zeilen = $eintraege | ForEach-Object {
        "    { `"versionCode`": $([int]$_.versionCode), `"versionName`": $(J $_.versionName), `"stand`": $(J $_.stand), `"notiz`": $(J $_.notiz) }"
    }
    $text = "{$nl  `"format`": 1,$nl  `"app`": $(J $Projekt),$nl  `"eintraege`": [$nl" + ($zeilen -join ",$nl") + "$nl  ]$nl}$nl"
    New-Item -ItemType Directory -Force -Path (Split-Path $logPfad -Parent) | Out-Null
    [IO.File]::WriteAllText($logPfad, $text, $utf8)
}

if (-not (Test-Path $logPfad)) {
    # Bootstrap für neue Projekte: nur das eindeutige Standardmuster wird umgestellt, sonst Anleitung.
    $anleitung = "Versionslog von Hand einbinden: Block 'versionslogAktuell' aus UpdateStation/app/build.gradle.kts übernehmen und $logRel nach Vorlage UpdateStation/app/src/main/assets/versionslog.json anlegen."
    if ($gradleText -match 'versionslogAktuell') { Fehler "build.gradle.kts liest schon einen Versionslog, aber $logRel fehlt. $anleitung" }
    $mc = [regex]::Matches($gradleText, '(?m)^([ \t]*)versionCode\s*=\s*(\d+)[ \t]*\r?$')
    $mn = [regex]::Matches($gradleText, '(?m)^([ \t]*)versionName\s*=\s*"([^"\r\n]+)"[ \t]*\r?$')
    $ma = [regex]::Matches($gradleText, '(?m)^android\s*\{')
    $mb = [regex]::Matches($gradleText, '(?m)^([ \t]*)buildConfigField\("String",\s*"VERSION_BUMPED_AT",.*$')
    $eindeutig = $mc.Count -eq 1 -and $mn.Count -eq 1 -and $ma.Count -eq 1 -and $mb.Count -le 1 -and
        ([regex]::Matches($gradleText, 'versionCode')).Count -eq 1 -and ([regex]::Matches($gradleText, 'versionName')).Count -eq 1
    if (-not $eindeutig) { Fehler "Kein Versionslog und kein eindeutiges Versionsmuster (genau eine Zeile versionCode = <Zahl> und versionName = `"x.y.z`"). $anleitung" }

    $nlG = if ($gradleText.Contains("`r`n")) { "`r`n" } else { "`n" }
    $block = @(
        '// Version kommt aus dem Versionslog (app/src/main/assets/versionslog.json, neuester Eintrag unten).'
        '// Die Datei liegt als Asset in der APK, damit UpdateStation Verlauf und Neuerungen anzeigen kann.'
        '@Suppress("UNCHECKED_CAST")'
        'val versionslogAktuell = ((groovy.json.JsonSlurper().parse(file("src/main/assets/versionslog.json"), "UTF-8") as Map<String, Any>)["eintraege"] as List<Map<String, Any>>).last()'
        ''
        ''
    ) -join $nlG
    $neu = $gradleText
    $neu = $neu.Remove($mc[0].Index, $mc[0].Length).Insert($mc[0].Index, $mc[0].Groups[1].Value + 'versionCode = (versionslogAktuell["versionCode"] as Number).toInt()' + $(if ($mc[0].Value.EndsWith("`r")) { "`r" } else { '' }))
    $mn = [regex]::Matches($neu, '(?m)^([ \t]*)versionName\s*=\s*"([^"\r\n]+)"[ \t]*\r?$')
    $neu = $neu.Remove($mn[0].Index, $mn[0].Length).Insert($mn[0].Index, $mn[0].Groups[1].Value + 'versionName = versionslogAktuell["versionName"] as String' + $(if ($mn[0].Value.EndsWith("`r")) { "`r" } else { '' }))
    $mb = [regex]::Matches($neu, '(?m)^([ \t]*)buildConfigField\("String",\s*"VERSION_BUMPED_AT",.*?(\r?)$')
    if ($mb.Count -eq 1) {
        $neu = $neu.Remove($mb[0].Index, $mb[0].Length).Insert($mb[0].Index, $mb[0].Groups[1].Value + 'buildConfigField("String", "VERSION_BUMPED_AT", "\"${versionslogAktuell["stand"]}\"")' + $mb[0].Groups[2].Value)
    }
    $ma = [regex]::Match($neu, '(?m)^android\s*\{')
    $neu = $neu.Insert($ma.Index, $block)

    $startCode = [int]$mc[0].Groups[2].Value
    $startName = $mn[0].Groups[2].Value
    $eintraege.Add([pscustomobject]@{
        versionCode = $startCode
        versionName = $startName
        stand       = (Get-Date -Format 'dd.MM.yyyy, HH:mm') + ' Uhr'
        notiz       = 'Versionslog eingerichtet (erste Veröffentlichung über UpdateStation)'
    })
    Schreibe-Versionslog
    [IO.File]::WriteAllText($gradleFile, $neu, $utf8)
    Write-Host "APK_UPDATE_VERSIONSLOG_EINGERICHTET=vc$startCode ($logRel, $gradleRel)"
    $vorbereitet.Add($logRel); $vorbereitet.Add($gradleRel)
}

$versionslog = Get-Content $logPfad -Raw -Encoding utf8 | ConvertFrom-Json
$eintraege.Clear()
foreach ($e in @($versionslog.eintraege)) { $eintraege.Add($e) }
if ($eintraege.Count -eq 0) { Fehler "Versionslog $logPfad hat keine Einträge." }
$versionCode = [int]$eintraege[-1].versionCode

if ($versionCode -le $mindest) {
    $neu = $mindest + 1
    $teile = "$($eintraege[-1].versionName)".Split('.')
    $teile[-1] = [string]([int]($teile[-1] -replace '\D.*$', '') + 1)
    # Notiz = Commits am Projekt seit der letzten Änderung am Versionslog.
    $seit = git -C $proggs log -1 --format=%H -- $logRel 2>$null
    $betreffe = if ($seit) { @(git -C $proggs log --format=%s "$seit..HEAD" -- $Projekt 2>$null) } else { @() }
    $notiz = ($betreffe | ForEach-Object { $_ -replace "^$([regex]::Escape($Projekt)):\s*", '' } | Select-Object -First 8) -join '; '
    if (-not $notiz) { $notiz = 'Update bereitgestellt' }
    $eintraege.Add([pscustomobject]@{
        versionCode = $neu
        versionName = $teile -join '.'
        stand       = (Get-Date -Format 'dd.MM.yyyy, HH:mm') + ' Uhr'
        notiz       = $notiz
    })
    Schreibe-Versionslog
    Write-Host "APK_UPDATE_VERSIONSLOG_ERGAENZT=$versionCode->$neu ($logRel)"
    $versionCode = $neu
    if (-not $vorbereitet.Contains($logRel)) { $vorbereitet.Add($logRel) }
}

if ($vorbereitet.Count -gt 0) {
    Aufraeumen
    Write-Host "APK_UPDATE_STATUS=vorbereitet"
    Write-Host "APK_UPDATE_COMMIT_NOETIG=$($vorbereitet -join ' ')"
    Write-Host "APK_UPDATE_HINWEIS=Nichts veröffentlicht. Diese Pfade mit der App-Änderung committen (nur mit Pfaden) und pushen, dann das Skript erneut starten."
    exit 2
}

# --- Nur committeten, gepushten Stand veröffentlichen --------------------------------------
# Sonst nennt update.json einen Commit, aus dem die APK nicht gebaut wurde.
$offen = @(git -C $proggs status --porcelain -- $Projekt 2>&1)
if ($LASTEXITCODE -ne 0) { Fehler "git status für $Projekt fehlgeschlagen: $($offen -join ' ')" }
if ($offen.Count -gt 0) { Fehler "Offene Änderungen in $Projekt ($(($offen | Select-Object -First 5) -join '; ')). Erst committen und pushen, dann veröffentlichen." }
# Ohne erfolgreichen Abgleich ist nicht belegt, dass der Stand gepusht und aktuell ist: abbrechen.
git -C $proggs fetch --quiet 2>$null
if ($LASTEXITCODE -ne 0) { Fehler "git fetch fehlgeschlagen – ohne Abgleich mit origin wird nicht veröffentlicht." }
$hinter = git -C $proggs rev-list --count 'HEAD..@{u}' 2>$null
if ($LASTEXITCODE -ne 0) { Fehler "Kein Upstream-Branch für den Abgleich mit origin." }
$vorn = git -C $proggs rev-list --count '@{u}..HEAD' -- $Projekt 2>$null
if ($LASTEXITCODE -ne 0) { Fehler "Abgleich der Projekt-Commits mit origin fehlgeschlagen." }
if ([int]$hinter -gt 0) { Fehler "Lokaler Stand liegt $hinter Commits hinter origin. Erst git pull --rebase --autostash." }
if ([int]$vorn -gt 0) { Fehler "$vorn Commits an $Projekt sind noch nicht gepusht. Erst pushen, dann veröffentlichen." }

# --- Bauen ----------------------------------------------------------------------------------
if (-not $OhneBuild) {
    Write-Host "Baue $Projekt mit :app:$gradleTask ..."
    Push-Location $projektDir
    try {
        & .\gradlew.bat ":app:$gradleTask" --console=plain
        $exit = $LASTEXITCODE
    } finally { Pop-Location }
    if ($exit -ne 0) { Fehler "Gradle-Build :app:$gradleTask ist fehlgeschlagen (Exit $exit)." }
}

$apkQuelle = Get-ChildItem (Join-Path $projektDir "app\build\outputs\apk\$apkOrdner") -Filter *.apk -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime | Select-Object -Last 1
if (-not $apkQuelle) { Fehler "Keine APK unter app\build\outputs\apk\$apkOrdner gefunden." }

# --- Signieren (nur unsignierte APKs, mit dem gemeinsamen Debug-Key) ------------------------
$temp = Join-Path ([IO.Path]::GetTempPath()) "apk-update-$Projekt-$lauf.apk"
$script:aufraeumen.Add($temp); $script:aufraeumen.Add("$temp.idsig")
if ($apkQuelle.Name -match 'unsigned') {
    if (-not (Test-Path $keystore)) { Fehler "Gemeinsamer Debug-Key fehlt: $keystore" }
    & $apksigner sign --ks $keystore --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out $temp $apkQuelle.FullName
    if ($LASTEXITCODE -ne 0) { Fehler "apksigner sign ist fehlgeschlagen." }
    Remove-Item "$temp.idsig" -ErrorAction SilentlyContinue
    $signiert = 'gemeinsamer Debug-Key (vom Skill signiert)'
} else {
    Copy-Item $apkQuelle.FullName $temp -Force
    $signiert = 'vom Build signiert'
}

# --- Fakten aus der fertigen APK, nicht aus Gradle ------------------------------------------
$badging = (& $aapt2 dump badging $temp 2>$null | Select-Object -First 1)
$pm = [regex]::Match($badging, "name='([^']+)' versionCode='(\d+)' versionName='([^']*)'")
if (-not $pm.Success) { Fehler "aapt2 konnte Paket und Version nicht aus der APK lesen." }
$paket = $pm.Groups[1].Value
$apkCode = [int]$pm.Groups[2].Value
$apkName = $pm.Groups[3].Value

$certs = & $apksigner verify --print-certs $temp 2>&1
if ($LASTEXITCODE -ne 0) { Fehler "apksigner verify: APK ist nicht gültig signiert." }
$cm = [regex]::Match(($certs -join "`n"), 'certificate SHA-256 digest:\s*([0-9a-fA-F]+)')
if (-not $cm.Success) { Fehler "Signatur-Fingerabdruck nicht lesbar." }
$signatur = $cm.Groups[1].Value.ToLower()

if ($paket -ne $erwartetesPaket) { Fehler "Paket in der APK ist '$paket', erwartet '$erwartetesPaket'. Variante prüfen (projekte.json)." }
if ($apkCode -ne $versionCode) { Fehler "versionCode der APK ($apkCode) passt nicht zum Versionslog ($versionCode). Liest build.gradle.kts den Versionslog? Build veraltet?" }
if ($apkCode -le $mindest) { Fehler "versionCode $apkCode ist nicht höher als veröffentlicht ($letzterCode) bzw. installiert ($installiertCode)." }

# --- Ablegen: erst APK vollständig, dann update.json, zuletzt aufräumen ---------------------
# Beide Dateien entstehen außerhalb des Drive-Ordners und kommen per Umbenennen hinein: lokal
# ist so nie eine halb geschriebene Datei sichtbar. Wann und in welcher Reihenfolge Drive für
# Desktop hochlädt, sichert das nicht – das fängt UpdateStation als Sync-Zwischenstand ab.
New-Item -ItemType Directory -Force -Path $zielDir | Out-Null
$sicherName = ($apkName -replace '[^\w\.\-]', '_')
$apkDatei = "$Projekt-$sicherName-vc$apkCode.apk"
$zielApk = Join-Path $zielDir $apkDatei
$sha256 = (Get-FileHash $temp -Algorithm SHA256).Hash.ToLower()
$groesse = (Get-Item $temp).Length
[IO.File]::Move($temp, $zielApk, $true)
if ((Get-FileHash $zielApk -Algorithm SHA256).Hash.ToLower() -ne $sha256) { Fehler "APK im Zielordner weicht nach dem Ablegen ab ($zielApk)." }

$jetzt = Get-Date
$commit = (git -C $proggs rev-parse --short HEAD 2>$null)
# Die letzten 15 Einträge des Versionslogs gehen mit, damit UpdateStation "Neu in dieser Version" zeigt.
$verlauf = @($eintraege | Select-Object -Last 15 | ForEach-Object {
    [ordered]@{ versionCode = [int]$_.versionCode; versionName = "$($_.versionName)"; stand = "$($_.stand)"; notiz = "$($_.notiz)" }
})
$manifest = [ordered]@{
    format         = 1
    projekt        = $Projekt
    paket          = $paket
    versionCode    = $apkCode
    versionName    = $apkName
    versionStand   = "$($eintraege[-1].stand)"
    apk            = $apkDatei
    groesse        = $groesse
    sha256         = $sha256
    signaturSha256 = $signatur
    variante       = $apkOrdner
    erstelltAm     = $jetzt.ToString('dd.MM.yyyy HH:mm')
    erstelltAmIso  = $jetzt.ToString('yyyy-MM-ddTHH:mm:sszzz')
    commit         = "$commit"
    versionslog    = $verlauf
}
$manifestTemp = Join-Path ([IO.Path]::GetTempPath()) "apk-update-$Projekt-$lauf-update.json"
$script:aufraeumen.Add($manifestTemp)
[IO.File]::WriteAllText($manifestTemp, ($manifest | ConvertTo-Json -Depth 5), $utf8)
[IO.File]::Move($manifestTemp, $manifestPfad, $true)
$kontrolle = Get-Content $manifestPfad -Raw -Encoding utf8 | ConvertFrom-Json
if ([int]$kontrolle.versionCode -ne $apkCode -or "$($kontrolle.sha256)" -ne $sha256) { Fehler "update.json nach dem Schreiben nicht stimmig ($manifestPfad)." }

# Die 5 neuesten APKs (nach versionCode) bleiben liegen, ältere werden gelöscht – nie die neue
# und nie die bisher referenzierte (Handys mit altem Manifest-Stand laden sie evtl. noch).
$behalten = 5
Get-ChildItem $zielDir -Filter *.apk |
    Sort-Object { VcNummer $_.Name } -Descending |
    Select-Object -Skip $behalten |
    Where-Object { $_.Name -ne $apkDatei -and $_.Name -ne $alteApk } |
    Remove-Item -Force

Write-Host "APK_UPDATE_STATUS=ok"
Write-Host "APK_UPDATE_BEREIT=lokal (liegt im Drive-Ordner; den Upload übernimmt Google Drive für Desktop, das Skript prüft ihn nicht)"
Write-Host "APK_UPDATE_PAKET=$paket"
Write-Host "APK_UPDATE_VERSION=$apkName (versionCode $apkCode, vorher veröffentlicht: $letzterCode, am Handy: $installiertCode)"
Write-Host "APK_UPDATE_SIGNATUR=$signiert, SHA-256 $($signatur.Substring(0,16))..."
Write-Host "APK_UPDATE_DATEI=$zielApk"
Aufraeumen
