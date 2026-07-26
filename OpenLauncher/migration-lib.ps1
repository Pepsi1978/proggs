# Hilfsfunktionen fuer den Umzug OpenCodeLauncher -> OpenLauncher.
#
# Bewusst als eigene Datei: release-und-verknuepfung.ps1 ENTFERNT am Ende einen Ordner. Diese
# Funktionen entscheiden, ob das gefahrlos ist, und werden darum von tests/profil-migration.tests.ps1
# gegen echte Ordner geprueft, statt nur im Ernstfall zu laufen.
#
# Junctions werden NIE verfolgt: Das Minimal-Profil blendet ~/.claude/skills als Verzeichnis-Junction
# ein (InstructionProfileService.EnsureSkillsJunction). Beim Kopieren entstuenden sonst echte Kopien
# des gesamten Skills-Bestands, beim Loeschen wuerde der Originalordner HINTER der Junction
# mitgeloescht -- ein Datenverlust ausserhalb des Launcher-Ordners.

Set-StrictMode -Version Latest

function Test-IstVerknuepfung {
    <#  Junction/Symlink? Wird an jeder Stelle geprueft, an der ein Verzeichnisbaum betreten wird. #>
    param([Parameter(Mandatory)][string]$Pfad)

    $item = Get-Item -LiteralPath $Pfad -Force -ErrorAction SilentlyContinue
    return $null -ne $item -and $item.Attributes.HasFlag([IO.FileAttributes]::ReparsePoint)
}

function Copy-FehlendeDateien {
    <#
      Ergaenzt im Ziel nur, was dort fehlt, und liefert die Anzahl kopierter Dateien.
      Vorhandenes bleibt unangetastet: Der Zielordner traegt nach einem Repo-Update bereits die
      versionierten Dateien -- die sind die neuere Wahrheit. Ueberschreiben wuerde eine frisch
      gezogene Konfiguration durch einen alten Stand ersetzen.
    #>
    param(
        [Parameter(Mandatory)][string]$Quelle,
        [Parameter(Mandatory)][string]$Ziel
    )

    if (-not (Test-Path -LiteralPath $Ziel)) { New-Item -ItemType Directory -Path $Ziel -Force | Out-Null }

    $kopiert = 0
    foreach ($datei in @(Get-ChildItem -LiteralPath $Quelle -File -Force)) {
        $zielDatei = Join-Path $Ziel $datei.Name
        if (-not (Test-Path -LiteralPath $zielDatei)) {
            Copy-Item -LiteralPath $datei.FullName -Destination $zielDatei -Force
            $kopiert++
        }
    }
    foreach ($ordner in @(Get-ChildItem -LiteralPath $Quelle -Directory -Force)) {
        if (Test-IstVerknuepfung $ordner.FullName) { continue }
        $kopiert += Copy-FehlendeDateien -Quelle $ordner.FullName -Ziel (Join-Path $Ziel $ordner.Name)
    }
    return $kopiert
}

function Get-FehlendeDateien {
    <#
      Verifikation vor dem Loeschen: liefert jede Datei der Quelle, die im Ziel NICHT existiert.
      Ein leeres Ergebnis ist die einzige Bedingung, unter der der alte Ordner entfernt werden darf.
    #>
    param(
        [Parameter(Mandatory)][string]$Quelle,
        [Parameter(Mandatory)][string]$Ziel
    )

    $fehlend = New-Object System.Collections.Generic.List[string]
    foreach ($datei in @(Get-ChildItem -LiteralPath $Quelle -File -Force)) {
        if (-not (Test-Path -LiteralPath (Join-Path $Ziel $datei.Name))) { $fehlend.Add($datei.FullName) }
    }
    foreach ($ordner in @(Get-ChildItem -LiteralPath $Quelle -Directory -Force)) {
        if (Test-IstVerknuepfung $ordner.FullName) { continue }
        foreach ($eintrag in @(Get-FehlendeDateien -Quelle $ordner.FullName -Ziel (Join-Path $Ziel $ordner.Name))) {
            $fehlend.Add($eintrag)
        }
    }
    return $fehlend.ToArray()
}

function Remove-BaumSicher {
    <#
      Loescht den Baum, loest Junctions aber nur als Verknuepfung auf, statt ihnen zu folgen.
      Remove-Item -Recurse allein ist hier nicht sicher genug: Es gab Windows-/PowerShell-Staende,
      in denen der Inhalt hinter einem Reparse-Point mitgeloescht wurde.
    #>
    param([Parameter(Mandatory)][string]$Pfad)

    foreach ($ordner in @(Get-ChildItem -LiteralPath $Pfad -Directory -Force)) {
        if (Test-IstVerknuepfung $ordner.FullName) {
            [IO.Directory]::Delete($ordner.FullName, $false)
        } else {
            Remove-BaumSicher -Pfad $ordner.FullName
        }
    }
    Remove-Item -LiteralPath $Pfad -Recurse -Force
}
