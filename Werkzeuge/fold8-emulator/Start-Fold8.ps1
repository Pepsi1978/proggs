# Startet den Fold-8-Emulator in Originalgroesse - fertig zum Arbeiten.
#
# Aufruf:  .\Start-Fold8.ps1                 -> Emulator, aufgeklappt, Originalgroesse
#          .\Start-Fold8.ps1 -Cover          -> Cover-Display-Emulator
#          .\Start-Fold8.ps1 -MitGeraet      -> zusaetzlich das echte Fold live daneben
#          .\Start-Fold8.ps1 -Zoom 1.5       -> groesser als das echte Handy
#          .\Start-Fold8.ps1 -Apk pfad.apk   -> APK gleich mitinstallieren

param(
  [switch]$Cover,
  [switch]$Innen,
  [switch]$MitGeraet,
  [double]$Zoom = 1.0,
  [string]$Apk = "",
  [string]$Projekt = "",
  [switch]$OhneBauen,
  [switch]$Kaltstart
)

$ErrorActionPreference = "Continue"
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$adb = "$sdk\platform-tools\adb.exe"
$emulator = "$sdk\emulator\emulator.exe"
$hier = Split-Path -Parent $MyInvocation.MyCommand.Path

# Agenten-Umgebung nicht an die App vererben
Get-ChildItem env: | Where-Object { $_.Name -like "CLAUDE*" -or $_.Name -eq "NO_COLOR" -or $_.Name -like "ANTHROPIC*" } |
  ForEach-Object { Remove-Item "env:$($_.Name)" -ErrorAction SilentlyContinue }
$env:ANDROID_HOME = $sdk

# --- Welches Display? Dem echten Geraet folgen ---------------------------
# Frank vergleicht immer mit dem Handy in seiner Hand. Ist es zugeklappt, sieht
# er das Cover-Display (5,5 Zoll); aufgeklappt das Innendisplay (7,6 Zoll). Der
# Emulator waehlt darum genau das, was das angeschlossene Geraet gerade zeigt -
# so kann das falsche Display gar nicht erst gezeigt werden.
if (-not $Cover -and -not $Innen) {
  $echt = & $adb devices | Where-Object { $_ -match "^\w+\s+device" -and $_ -notmatch "^emulator-" } | Select-Object -First 1
  if ($echt) {
    $seriennr = ($echt -split "\s+")[0]
    $groesse = (& $adb -s $seriennr shell wm size 2>$null) -join " "
    if ($groesse -match "1248x1972") {
      $Cover = $true
      Write-Host "Dein Fold ist zugeklappt - Emulator startet im Cover-Format (5,5 Zoll)." -ForegroundColor DarkGray
    } elseif ($groesse -match "1848x2448|2448x1848") {
      Write-Host "Dein Fold ist aufgeklappt - Emulator startet im Innenformat (7,6 Zoll)." -ForegroundColor DarkGray
    }
  }
}

$avd = if ($Cover) { "Fold8_Cover" } else { "Fold8" }

# --- Laeuft schon einer? -------------------------------------------------
$laeuft = & $adb devices | Where-Object { $_ -match "^emulator-\d+\s+device" }
if ($laeuft) {
  Write-Host "Emulator laeuft bereits." -ForegroundColor DarkGray
} else {
  Write-Host "Starte $avd ..." -ForegroundColor Cyan
  $argumente = @("-avd", $avd, "-gpu", "host", "-no-boot-anim")
  if ($Kaltstart) { $argumente += "-no-snapshot-load" }
  Start-Process -FilePath $emulator -ArgumentList $argumente -WindowStyle Normal

  # Fenstergroesse setzen, SOBALD das Fenster da ist - also lange vor dem
  # fertigen Systemstart. Der Emulator kennt keine Startgroesse (siehe Kopf von
  # Set-Originalgroesse.ps1) und erscheint zunaechst bildschirmfuellend; je
  # frueher korrigiert wird, desto weniger sieht man davon.
  $setzen = Join-Path $hier "Set-Originalgroesse.ps1"
  if (Test-Path $setzen) {
    $argListe = @("-ExecutionPolicy","Bypass","-File",$setzen,"-Warten","60","-Zoom",$Zoom,"-Leise")
    if ($Cover) { $argListe += "-Cover" }
    & powershell $argListe | Out-Null
  }

  Write-Host "Warte auf den Systemstart..." -NoNewline
  $fertig = $false
  for ($i = 0; $i -lt 90; $i++) {
    Start-Sleep -Seconds 4
    $b = (& $adb -s emulator-5554 shell getprop sys.boot_completed 2>$null) -replace "\s",""
    if ($b -eq "1") { $fertig = $true; break }
    Write-Host "." -NoNewline
  }
  Write-Host ""
  if (-not $fertig) { Write-Host "Systemstart nicht bestaetigt - bitte selbst nachsehen." -ForegroundColor Yellow }
  else { Write-Host "Bereit." -ForegroundColor Green }
}

# --- An das echte Geraet angleichen -------------------------------------
# Frank hat die Schrift auf dem Fold 8 auf 90 Prozent gestellt
& $adb -s emulator-5554 shell "settings put system font_scale 0.9" 2>$null | Out-Null
# Hochformat festhalten: nicht selbsttaetig drehen, nur auf Zuruf
& $adb -s emulator-5554 shell "settings put system accelerometer_rotation 0" 2>$null | Out-Null
& $adb -s emulator-5554 shell "settings put system user_rotation 0" 2>$null | Out-Null

# WICHTIG: aufgeklappt erzwingen. Der Emulator sichert seinen Zustand ueber
# Sitzungen hinweg - blieb er einmal zugeklappt, zeigt er beim naechsten Start
# nur den Cover-Ausschnitt des grossen Panels. Die App wird dann weiterhin auf
# 1848x2448 gerendert, aber links/rechts/unten abgeschnitten dargestellt.
if (-not $Cover) {
  & $adb -s emulator-5554 emu sensor set hinge-angle0 180 2>$null | Out-Null
  Start-Sleep -Milliseconds 1500
  $zustand = (& $adb -s emulator-5554 shell "dumpsys device_state | grep mCommittedState" 2>$null) -join ""
  if ($zustand -notmatch "OPENED") {
    Write-Host "Hinweis: Emulator meldet nicht OPENED - Ansicht koennte beschnitten sein." -ForegroundColor Yellow
  }
}

# --- Projekt bauen, installieren und starten ------------------------------
if ($Projekt -ne "") {
  $wurzel = if (Test-Path $Projekt) { (Resolve-Path $Projekt).Path }
            else { Join-Path (Join-Path $env:USERPROFILE "proggs") $Projekt }

  if (-not (Test-Path (Join-Path $wurzel "gradlew.bat"))) {
    Write-Host "Kein Gradle-Projekt unter $wurzel" -ForegroundColor Red
  } else {
    if (-not $OhneBauen) {
      Write-Host "Baue $(Split-Path $wurzel -Leaf) ..." -ForegroundColor Cyan
      Push-Location $wurzel
      $bauAusgabe = & ".\gradlew.bat" assembleDebug --console=plain 2>&1
      $bauOk = ($LASTEXITCODE -eq 0)
      Pop-Location
      if (-not $bauOk) {
        Write-Host ""
        Write-Host "  BUILD FEHLGESCHLAGEN - es wird nichts installiert," -ForegroundColor Red
        Write-Host "  damit die alte Version nicht faelschlich fuer die neue gehalten wird." -ForegroundColor Red
        Write-Host ""
        # Die eigentlichen Fehlerzeilen zeigen, nicht das Gradle-Rauschen
        $fehlerZeilen = $bauAusgabe | Where-Object {
          $_ -match "^e: |error:|FAILURE:|Caused by:|Unresolved reference|Compilation error|> Task .* FAILED"
        } | Select-Object -First 15
        if ($fehlerZeilen) {
          Write-Host "  Fehler im Code:" -ForegroundColor Yellow
          $fehlerZeilen | ForEach-Object { Write-Host "    $_" -ForegroundColor Yellow }
        } else {
          $bauAusgabe | Select-Object -Last 12 | ForEach-Object { Write-Host "    $_" -ForegroundColor DarkGray }
        }
        Write-Host ""
        $Projekt = ""
      }
    }
  }

  if ($Projekt -ne "") {
    $apkDatei = Get-ChildItem (Join-Path $wurzel "app\build\outputs\apk\debug") -Filter "*.apk" -ErrorAction SilentlyContinue |
                Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $apkDatei) {
      $apkDatei = Get-ChildItem $wurzel -Recurse -Filter "*-debug.apk" -ErrorAction SilentlyContinue |
                  Sort-Object LastWriteTime -Descending | Select-Object -First 1
    }
    if ($apkDatei) { $Apk = $apkDatei.FullName }
    else { Write-Host "Keine Debug-APK gefunden." -ForegroundColor Red }
  }
}

if ($Apk -ne "") {
  if (Test-Path $Apk) {
    Write-Host "Installiere $(Split-Path $Apk -Leaf) ..." -ForegroundColor Cyan
    # -r = ersetzen. Die App-Daten (API-Schluessel, Einstellungen, Datenbanken)
    # bleiben dabei erhalten - nachgewiesen am 12.08.2026. NIEMALS uninstall
    # verwenden, das wuerde alle eingetragenen Zugangsdaten loeschen.
    $meldung = & $adb -s emulator-5554 install -r $Apk 2>&1
    $installOk = ($meldung -match "Success")

    if (-not $installOk) {
      Write-Host ""
      Write-Host "  INSTALLATION FEHLGESCHLAGEN:" -ForegroundColor Red
      $meldung | Select-Object -Last 4 | ForEach-Object { Write-Host "    $_" -ForegroundColor Yellow }
      if ($meldung -match "UPDATE_INCOMPATIBLE|signatures do not match") {
        Write-Host ""
        Write-Host "  Ursache: Die neue Version ist anders signiert als die installierte." -ForegroundColor Yellow
        Write-Host "  Ein Drueberinstallieren ist dann nicht moeglich." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "  ACHTUNG: Eine Deinstallation wuerde alle App-Daten loeschen -" -ForegroundColor Red
        Write-Host "  auch die eingetragenen API-Schluessel. Darum wird hier NICHT" -ForegroundColor Red
        Write-Host "  automatisch deinstalliert. Vorher sichern:  .\Sichere-Appdaten.ps1" -ForegroundColor Red
      }
      Write-Host ""
      $Apk = ""
    } else {
      $meldung | Select-Object -Last 1
    }
  } else {
    Write-Host "APK nicht gefunden: $Apk" -ForegroundColor Red
    $Apk = ""
  }

  if ($Apk -ne "") {
    # Paketnamen aus der APK lesen und die App gleich starten
    $aapt = Get-ChildItem "$sdk\build-tools" -Recurse -Filter "aapt2.exe" -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending | Select-Object -First 1
    $paket = ""
    if ($aapt) {
      $info = & $aapt.FullName dump packagename $Apk 2>$null
      if ($info) { $paket = ($info | Select-Object -First 1).Trim() }
    }
    if ($paket -ne "") {
      Write-Host "Starte $paket ..." -ForegroundColor Cyan
      # Absturzprotokoll leeren, damit nur NEUE Abstuerze gemeldet werden
      & $adb -s emulator-5554 logcat -b crash -c 2>$null | Out-Null
      & $adb -s emulator-5554 shell "monkey -p $paket -c android.intent.category.LAUNCHER 1" 2>&1 | Out-Null

      # --- Laeuft die App ueberhaupt noch? -------------------------------
      # Ein Kaltstart dauert auf dem Emulator laenger als eine feste Wartezeit:
      # bis zu 20 s auf den Prozess warten, statt vorschnell "abgestuerzt" zu melden.
      $pid1 = ""
      for ($i = 0; $i -lt 20; $i++) {
        Start-Sleep -Seconds 1
        $pid1 = (& $adb -s emulator-5554 shell "pidof $paket" 2>$null) -replace "\s",""
        if ($pid1 -ne "") { break }
      }

      # Der crash-Puffer sammelt ALLE Prozesse des Systems - auch Bluetooth-Tombstones,
      # die mit dieser App nichts zu tun haben. Nur Abstuerze zaehlen, die das eigene
      # Paket nennen ("Process: <paket>" bei Java, ">>> <paket> <<<" bei nativen).
      $absturz = @(& $adb -s emulator-5554 logcat -b crash -d 2>$null)
      $paketRegex = [regex]::Escape($paket)
      $eigenerCrash = @($absturz | Where-Object { $_ -match $paketRegex }).Count -gt 0

      if ($pid1 -eq "" -or $eigenerCrash) {
        Write-Host ""
        if ($eigenerCrash) {
          Write-Host "  DIE APP IST ABGESTUERZT." -ForegroundColor Red
          # Ab der ersten eigenen Zeile den zusammenhaengenden Stacktrace zeigen
          $start = 0
          for ($j = 0; $j -lt $absturz.Count; $j++) {
            if ($absturz[$j] -match $paketRegex) { $start = [Math]::Max(0, $j - 2); break }
          }
          $ende = [Math]::Min($absturz.Count - 1, $start + 14)
          Write-Host ""
          $absturz[$start..$ende] | ForEach-Object { Write-Host "    $_" -ForegroundColor Yellow }
        } else {
          Write-Host "  DIE APP LAEUFT NICHT." -ForegroundColor Red
          Write-Host "  Kein Absturz fuer $paket im Protokoll - sie wurde vermutlich gar nicht erst gestartet." -ForegroundColor DarkGray
        }
        Write-Host ""
        Write-Host "  Vollstaendiges Protokoll:  .\Zeig-Fehler.ps1" -ForegroundColor DarkGray
      } else {
        Write-Host "Laeuft (PID $pid1)." -ForegroundColor Green
        # Laufzeitfehler, die die App NICHT beenden, trotzdem melden
        $meckern = & $adb -s emulator-5554 logcat -d -t 400 *:E 2>$null |
                   Where-Object { $_ -match [regex]::Escape(($paket -replace '\.debug$','')) } |
                   Select-Object -First 6
        if ($meckern) {
          Write-Host ""
          Write-Host "  Die App laeuft, meldet aber Fehler:" -ForegroundColor Yellow
          $meckern | ForEach-Object { Write-Host "    $_" -ForegroundColor Yellow }
        }
      }
    } else {
      Write-Host "Paketname nicht ermittelbar - App bitte selbst antippen." -ForegroundColor Yellow
    }
  }
}

# --- Originalgroesse setzen ----------------------------------------------
$skript = Join-Path $hier "Set-Originalgroesse.ps1"
if (Test-Path $skript) {
  if ($Cover) { & powershell -ExecutionPolicy Bypass -File $skript -Zoom $Zoom -Cover }
  else        { & powershell -ExecutionPolicy Bypass -File $skript -Zoom $Zoom }
}

# --- Echtes Geraet daneben spiegeln --------------------------------------
if ($MitGeraet) {
  $echte = & $adb devices | Where-Object { $_ -match "^\w+\s+device" -and $_ -notmatch "^emulator-" }
  if ($echte) {
    $seriennr = (($echte | Select-Object -First 1) -split "\s+")[0]
    $scrcpy = Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages" -Recurse -Filter "scrcpy.exe" -ErrorAction SilentlyContinue |
              Select-Object -First 1 -ExpandProperty FullName
    if ($scrcpy) {
      Write-Host "Spiegle das echte Geraet ($seriennr) ..." -ForegroundColor Cyan
      # scrcpy kann seine Fenstergroesse - anders als der Emulator - direkt beim
      # Start bekommen. Damit steht die Spiegelung sofort im gleichen Massstab
      # daneben und muss nie nachjustiert werden.
      $gr = (& $adb -s $seriennr shell wm size 2>$null) -join " "
      $breite = 0; $hoehe = 0
      if ($gr -match "(\d+)x(\d+)") {
        $gB = [int]$Matches[1]; $gH = [int]$Matches[2]
        $gPpi = if ([math]::Max($gB,$gH) -eq 1972) { 424.3 } else { 403.6 }
        $breite = [int][math]::Round($gB * (242.9 / $gPpi) * $Zoom)
        $hoehe  = [int][math]::Round($gH * (242.9 / $gPpi) * $Zoom)
      }
      $scArgs = @("--serial", $seriennr, "--window-title", "Fold8-echt")
      if ($breite -gt 0) { $scArgs += @("--window-width", "$breite", "--window-height", "$hoehe", "--window-x", "60", "--window-y", "80") }
      else { $scArgs += @("--max-size", "1000") }
      Start-Process -FilePath $scrcpy -ArgumentList $scArgs
    } else {
      Write-Host "scrcpy nicht gefunden. Installieren mit: winget install Genymobile.scrcpy" -ForegroundColor Yellow
    }
  } else {
    Write-Host "Kein echtes Geraet angeschlossen." -ForegroundColor Yellow
  }
}

Write-Host ""
Write-Host "  Zuklappen:   adb -s emulator-5554 emu sensor set hinge-angle0 0" -ForegroundColor DarkGray
Write-Host "  Aufklappen:  adb -s emulator-5554 emu sensor set hinge-angle0 180" -ForegroundColor DarkGray
Write-Host "  Elemente:    .\Zeig-Elemente.ps1" -ForegroundColor DarkGray
Write-Host ""
