# Schliesst den Umzug von OpenCodeLauncher auf OpenLauncher ab, baut die Release-Version und legt
# die Desktop-Verknuepfung an.
#
#   Rechtsklick auf diese Datei -> "Mit PowerShell ausfuehren"
#
# Ablauf (die Reihenfolge ist wesentlich):
#   1. Profile aus dem alten Ordner uebernehmen -- dort liegen die Laufzeitdaten der CLIs, darunter
#      der Login-Token (.credentials.json), der bewusst nie im Repo liegt.
#   2. Uebernahme pruefen: JEDE Datei des alten Ordners muss im neuen angekommen sein.
#   3. Erst dann den alten Ordner entfernen. Vor Schritt 2 zu loeschen wuerde den Login vernichten.
#   4. Release bauen, Desktop-Verknuepfung ueber create_shortcut.ps1 anlegen (Zielpfad + Icon werden
#      nur dort gepflegt).

[CmdletBinding()]
param(
    # Ueberspringt die Sicherheitsabfrage vor dem Entfernen des alten Ordners.
    [switch]$AltenOrdnerOhneRueckfrageEntfernen
)

$ErrorActionPreference = 'Stop'

# Uebernahme- und Loeschlogik liegt in migration-lib.ps1, damit sie von
# tests/profil-migration.tests.ps1 geprueft wird und nicht erst im Ernstfall laeuft.
. (Join-Path $PSScriptRoot 'migration-lib.ps1')

$projectRoot = $PSScriptRoot
$projectFile = Join-Path $projectRoot 'OpenLauncher.csproj'
$shortcutScript = Join-Path $projectRoot 'create_shortcut.ps1'
$exe = Join-Path $projectRoot 'bin\Release\net8.0-windows10.0.19041.0\win-x64\OpenLauncher.exe'
$legacyRoot = Join-Path (Split-Path -Parent $projectRoot) 'OpenCodeLauncher'

if (-not (Test-Path -LiteralPath $projectFile)) {
    throw "Projektdatei nicht gefunden: $projectFile"
}
if (-not (Get-Command dotnet -ErrorAction SilentlyContinue)) {
    throw "Das .NET-8-SDK wurde nicht gefunden. Bitte installieren (siehe SETUP.md, Abschnitt 5) und erneut ausfuehren."
}

# Ein laufender Launcher haelt die .exe gesperrt -> der Build scheitert sonst mit einem
# Datei-in-Benutzung-Fehler, dessen Ursache aus der dotnet-Ausgabe nicht zu erkennen ist.
$running = @(Get-Process -Name 'OpenLauncher' -ErrorAction SilentlyContinue)
if ($running.Count -gt 0) {
    throw "Der OpenLauncher laeuft noch und wuerde den Build blockieren. Bitte schliessen und erneut ausfuehren."
}

# ---------------------------------------------------------------- 1-3: Umzug abschliessen
if (Test-Path -LiteralPath $legacyRoot) {
    Write-Output "Uebernehme die restlichen Daten aus dem alten Ordner ($legacyRoot) ..."
    $kopiert = Copy-FehlendeDateien -Quelle $legacyRoot -Ziel $projectRoot
    Write-Output "  -> $kopiert Datei(en) uebernommen."

    $fehlend = @(Get-FehlendeDateien -Quelle $legacyRoot -Ziel $projectRoot)
    if ($fehlend.Count -gt 0) {
        Write-Warning "Der alte Ordner wird NICHT entfernt: $($fehlend.Count) Datei(en) fehlen im neuen Ordner."
        $fehlend | Select-Object -First 10 | ForEach-Object { Write-Warning "  - $_" }
        throw "Die Uebernahme ist unvollstaendig. Der alte Ordner bleibt als Sicherheitsnetz bestehen."
    }

    Write-Output '  -> Vollstaendig uebernommen (jede Datei im neuen Ordner vorhanden).'
    $entfernen = $AltenOrdnerOhneRueckfrageEntfernen
    if (-not $entfernen) {
        $antwort = Read-Host "Alten Ordner '$legacyRoot' jetzt endgueltig entfernen? [j/N]"
        $entfernen = $antwort -match '^(j|ja|y|yes)$'
    }
    if ($entfernen) {
        Remove-BaumSicher -Pfad $legacyRoot
        Write-Output "  -> Alter Ordner entfernt: $legacyRoot"
    } else {
        Write-Output '  -> Alter Ordner bleibt vorerst bestehen (kann jederzeit von Hand geloescht werden).'
    }
} else {
    Write-Output 'Kein alter OpenCodeLauncher-Ordner vorhanden - nichts zu uebernehmen.'
}

# ---------------------------------------------------------------- 4: Bauen + Verknuepfung
Write-Output ''
Write-Output 'Baue die Release-Version ...'
$build = Start-Process -FilePath 'dotnet' -ArgumentList @('build', $projectFile, '-c', 'Release') `
    -WorkingDirectory $projectRoot -Wait -PassThru -NoNewWindow
if ($build.ExitCode -ne 0) {
    throw "Der Release-Build ist fehlgeschlagen (Exit-Code $($build.ExitCode))."
}
if (-not (Test-Path -LiteralPath $exe)) {
    throw "Der Build meldete Erfolg, aber die Datei fehlt: $exe"
}

Write-Output 'Lege die Desktop-Verknuepfung an ...'
& $shortcutScript

Write-Output ''
Write-Output 'Fertig. Der OpenLauncher startet ueber die Verknuepfung "OpenLauncher" auf dem Desktop.'
Write-Output "  -> Programm: $exe"
