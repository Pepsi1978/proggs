# apk-update.ps1 - baut die Update-APK eines Android-Projekts und legt sie mit update.json
# in den Google-Drive-Ordner "Meine Ablage\Dokumente\Updates\<Projekt>".
# Die Handy-App UpdateStation liest update.json und vergleicht versionCode + Signatur.
#
# Aufruf: pwsh -File apk-update.ps1 -Projekt FisetinBegleiter [-OhneBuild]
# Ausgabe-Zeilen mit Präfix APK_UPDATE_ sind für den Skill maschinenlesbar.

param(
    [Parameter(Mandatory = $true)][string]$Projekt,
    [switch]$OhneBuild
)

$ErrorActionPreference = 'Stop'
$utf8 = New-Object System.Text.UTF8Encoding($false)

function Fehler([string]$text) {
    Write-Host "APK_UPDATE_STATUS=fehler"
    Write-Host "APK_UPDATE_FEHLER=$text"
    exit 1
}

$skillDir   = Split-Path -Parent $PSScriptRoot
$proggs     = Join-Path $env:USERPROFILE 'proggs'
$updatesDir = Join-Path $env:USERPROFILE 'Meine Ablage\Dokumente\Updates'
$keystore   = Join-Path $env:USERPROFILE 'SK\Android\debug-shared.keystore'

# --- Konfiguration --------------------------------------------------------------------------
$cfg = Get-Content (Join-Path $skillDir 'projekte.json') -Raw -Encoding utf8 | ConvertFrom-Json
$eintrag = $cfg.projekte.$Projekt
$gradleTask = if ($eintrag.gradleTask) { $eintrag.gradleTask } else { $cfg.standard.gradleTask }
$apkOrdner  = if ($eintrag.apkOrdner)  { $eintrag.apkOrdner }  else { $cfg.standard.apkOrdner }

$projektDir = Join-Path $proggs $Projekt
$gradleFile = Join-Path $projektDir 'app\build.gradle.kts'
if (-not (Test-Path $gradleFile)) { Fehler "Kein Android-Projekt: $gradleFile fehlt." }

$gradleText = [IO.File]::ReadAllText($gradleFile)
$erwartetesPaket = $eintrag.paket
if (-not $erwartetesPaket) {
    $m = [regex]::Match($gradleText, 'applicationId\s*=\s*"([^"]+)"')
    if (-not $m.Success) { Fehler "applicationId in build.gradle.kts nicht gefunden." }
    $erwartetesPaket = $m.Groups[1].Value
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
$zielDir = Join-Path $updatesDir $Projekt
New-Item -ItemType Directory -Force -Path $zielDir | Out-Null
$manifestPfad = Join-Path $zielDir 'update.json'
$letzterCode = 0
if (Test-Path $manifestPfad) {
    try { $letzterCode = [int]((Get-Content $manifestPfad -Raw -Encoding utf8 | ConvertFrom-Json).versionCode) } catch { $letzterCode = 0 }
}

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
$mindest = [Math]::Max($letzterCode, $installiertCode)

# --- Versionslog: einzige Quelle für versionCode, versionName und Stand ----------------------
$logRel = "$Projekt/app/src/main/assets/versionslog.json"
$logPfad = Join-Path $projektDir 'app\src\main\assets\versionslog.json'
if (-not (Test-Path $logPfad)) { Fehler "Kein Versionslog: $logPfad fehlt. Projekt zuerst auf den Versionslog umstellen (SKILL.md, Abschnitt Versionslog)." }
$versionslog = Get-Content $logPfad -Raw -Encoding utf8 | ConvertFrom-Json
$eintraege = [System.Collections.Generic.List[object]]::new()
foreach ($e in @($versionslog.eintraege)) { $eintraege.Add($e) }
if ($eintraege.Count -eq 0) { Fehler "Versionslog $logPfad hat keine Einträge." }
$versionCode = [int]$eintraege[-1].versionCode

function J([string]$s) { '"' + ($s -replace '\\', '\\' -replace '"', '\"' -replace "`r?`n", ' ') + '"' }
function Schreibe-Versionslog {
    $nl = "`n"
    $zeilen = $eintraege | ForEach-Object {
        "    { `"versionCode`": $([int]$_.versionCode), `"versionName`": $(J $_.versionName), `"stand`": $(J $_.stand), `"notiz`": $(J $_.notiz) }"
    }
    $text = "{$nl  `"format`": 1,$nl  `"app`": $(J $Projekt),$nl  `"eintraege`": [$nl" + ($zeilen -join ",$nl") + "$nl  ]$nl}$nl"
    [IO.File]::WriteAllText($logPfad, $text, $utf8)
}

# versionCode muss über der zuletzt veröffentlichten UND der installierten liegen. Erstes Update
# ohne Handy-Verbindung: die aktuelle Nummer ist vermutlich schon per Kabel installiert.
if ($letzterCode -eq 0 -and $installiertCode -eq 0) { $mindest = $versionCode }
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
}

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
$temp = Join-Path ([IO.Path]::GetTempPath()) "apk-update-$Projekt.apk"
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

# --- Ablegen: erst APK, dann update.json (die Handy-App liest nur, was im Manifest steht) ---
$sicherName = ($apkName -replace '[^\w\.\-]', '_')
$apkDatei = "$Projekt-$sicherName-vc$apkCode.apk"
$zielApk = Join-Path $zielDir $apkDatei
Copy-Item $temp $zielApk -Force
Remove-Item $temp -ErrorAction SilentlyContinue
# Die 5 neuesten APKs (nach versionCode) bleiben liegen, ältere werden gelöscht.
$behalten = 5
Get-ChildItem $zielDir -Filter *.apk |
    Sort-Object { $m = [regex]::Match($_.Name, '-vc(\d+)\.apk$'); if ($m.Success) { [int]$m.Groups[1].Value } else { 0 } } -Descending |
    Select-Object -Skip $behalten | Remove-Item -Force

$sha256 = (Get-FileHash $zielApk -Algorithm SHA256).Hash.ToLower()
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
    groesse        = (Get-Item $zielApk).Length
    sha256         = $sha256
    signaturSha256 = $signatur
    variante       = $apkOrdner
    erstelltAm     = $jetzt.ToString('dd.MM.yyyy HH:mm')
    erstelltAmIso  = $jetzt.ToString('yyyy-MM-ddTHH:mm:sszzz')
    commit         = "$commit"
    versionslog    = $verlauf
}
[IO.File]::WriteAllText($manifestPfad, ($manifest | ConvertTo-Json -Depth 5), $utf8)

Write-Host "APK_UPDATE_STATUS=ok"
Write-Host "APK_UPDATE_PAKET=$paket"
Write-Host "APK_UPDATE_VERSION=$apkName (versionCode $apkCode, vorher veröffentlicht: $letzterCode, am Handy: $installiertCode)"
Write-Host "APK_UPDATE_SIGNATUR=$signiert, SHA-256 $($signatur.Substring(0,16))..."
Write-Host "APK_UPDATE_DATEI=$zielApk"
