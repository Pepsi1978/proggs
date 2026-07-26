# Test fuer die Umzugs-Hilfsfunktionen (migration-lib.ps1).
#
# Warum dieser Test existiert: release-und-verknuepfung.ps1 ENTFERNT am Ende den alten
# OpenCodeLauncher-Ordner. Zwei Fehler waeren dabei nicht wiedergutzumachen:
#   1. Loeschen, obwohl Dateien (allen voran der Login-Token .credentials.json) noch nicht
#      uebernommen sind -> der Login ist weg.
#   2. Einer Junction folgen -> geloescht wuerde der ECHTE ~/.claude/skills-Bestand ausserhalb
#      des Launcher-Ordners.
# Beide Faelle werden hier gegen echte Ordner geprueft, nicht nur gegen Code gelesen.

[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
. (Join-Path (Split-Path -Parent $PSScriptRoot) 'migration-lib.ps1')

# Pfade aus Einzelsegmenten zusammensetzen: 'a\b' waere auf macOS/Linux ein Dateiname mit
# Backslash statt zweier Ebenen - der Test wuerde dort eine andere Struktur pruefen als auf Windows.
function Pfad {
    param([Parameter(Mandatory)][string]$Basis, [Parameter(ValueFromRemainingArguments)][string[]]$Teile)
    $ergebnis = $Basis
    foreach ($teil in $Teile) { $ergebnis = Join-Path $ergebnis $teil }
    return $ergebnis
}

$failures = New-Object System.Collections.Generic.List[string]
function Assert($bedingung, [string]$text) {
    if (-not $bedingung) { $script:failures.Add($text) }
}

$wurzel = Join-Path ([IO.Path]::GetTempPath()) ("openlauncher-migrationstest-" + [Guid]::NewGuid().ToString('N'))
$alt = Join-Path $wurzel 'OpenCodeLauncher'
$neu = Join-Path $wurzel 'OpenLauncher'
$extern = Join-Path $wurzel 'echte-skills'

try {
    # --- Ausgangslage aufbauen -------------------------------------------------------------
    New-Item -ItemType Directory -Path (Pfad $alt 'Profiles' 'ClaudeCode' 'standard') -Force | Out-Null
    New-Item -ItemType Directory -Path (Pfad $alt 'Profiles' 'ClaudeCode' 'minimal') -Force | Out-Null
    New-Item -ItemType Directory -Path $extern -Force | Out-Null
    New-Item -ItemType Directory -Path (Pfad $neu 'Profiles' 'ClaudeCode' 'standard') -Force | Out-Null

    # Login-Token: liegt NUR im alten Ordner und muss ankommen.
    Set-Content -LiteralPath (Pfad $alt 'Profiles' 'ClaudeCode' 'standard' '.credentials.json') -Value '{"token":"geheim"}'
    Set-Content -LiteralPath (Pfad $alt 'Profiles' 'ClaudeCode' 'standard' 'settings.json') -Value 'ALT'
    Set-Content -LiteralPath (Pfad $alt 'Profiles' 'ClaudeCode' 'minimal' '.claude.json') -Value '{"account":"frank"}'
    # Gleichnamige Datei im Ziel: der neuere Repo-Stand darf NICHT ueberschrieben werden.
    Set-Content -LiteralPath (Pfad $neu 'Profiles' 'ClaudeCode' 'standard' 'settings.json') -Value 'NEU'
    # Die echten Skills liegen ausserhalb; im alten Profil haengt nur eine Verknuepfung darauf.
    Set-Content -LiteralPath (Join-Path $extern 'wichtiger-skill.md') -Value 'darf niemals verschwinden'
    New-Item -ItemType SymbolicLink -Path (Pfad $alt 'Profiles' 'ClaudeCode' 'minimal' 'skills') -Target $extern | Out-Null

    # --- 1. Uebernahme ---------------------------------------------------------------------
    $kopiert = Copy-FehlendeDateien -Quelle $alt -Ziel $neu

    Assert (Test-Path -LiteralPath (Pfad $neu 'Profiles' 'ClaudeCode' 'standard' '.credentials.json')) `
        'Der Login-Token (.credentials.json) wurde nicht uebernommen - genau dieser Verlust erzwingt einen neuen Login.'
    Assert (Test-Path -LiteralPath (Pfad $neu 'Profiles' 'ClaudeCode' 'minimal' '.claude.json')) `
        'Die versteckte .claude.json wurde nicht uebernommen (Get-ChildItem ohne -Force?).'
    Assert ((Get-Content -LiteralPath (Pfad $neu 'Profiles' 'ClaudeCode' 'standard' 'settings.json') -Raw).Trim() -eq 'NEU') `
        'Eine im Ziel vorhandene Datei wurde ueberschrieben - der neuere Repo-Stand geht damit verloren.'
    Assert ($kopiert -ge 2) "Die Anzahl kopierter Dateien ist unplausibel: $kopiert"
    Assert (-not (Test-Path -LiteralPath (Pfad $neu 'Profiles' 'ClaudeCode' 'minimal' 'skills' 'wichtiger-skill.md'))) `
        'Der Inhalt hinter der Junction wurde als echte Kopie materialisiert - die Junction wurde verfolgt.'

    # --- 2. Verifikation -------------------------------------------------------------------
    $fehlend = @(Get-FehlendeDateien -Quelle $alt -Ziel $neu)
    Assert ($fehlend.Count -eq 0) "Nach vollstaendiger Uebernahme werden faelschlich $($fehlend.Count) Datei(en) als fehlend gemeldet: $($fehlend -join ', ')"

    # Gegenprobe: fehlt etwas, MUSS es erkannt werden - sonst loescht das Skript zu frueh.
    Remove-Item -LiteralPath (Pfad $neu 'Profiles' 'ClaudeCode' 'standard' '.credentials.json') -Force
    $fehlendJetzt = @(Get-FehlendeDateien -Quelle $alt -Ziel $neu)
    Assert ($fehlendJetzt.Count -eq 1) 'Eine im Ziel fehlende Datei wurde NICHT erkannt - der alte Ordner wuerde trotz Datenverlust geloescht.'
    Copy-FehlendeDateien -Quelle $alt -Ziel $neu | Out-Null

    # --- 3. Loeschen -----------------------------------------------------------------------
    Remove-BaumSicher -Pfad $alt

    Assert (-not (Test-Path -LiteralPath $alt)) 'Der alte Ordner wurde nicht entfernt.'
    Assert (Test-Path -LiteralPath (Join-Path $extern 'wichtiger-skill.md')) `
        'SCHWERWIEGEND: Der Inhalt hinter der Junction wurde mitgeloescht - das Loeschen folgte dem Reparse-Point.'
    Assert (Test-Path -LiteralPath (Pfad $neu 'Profiles' 'ClaudeCode' 'standard' '.credentials.json')) `
        'Der uebernommene Login-Token ist nach dem Loeschen nicht mehr vorhanden.'
}
finally {
    Remove-Item -LiteralPath $wurzel -Recurse -Force -ErrorAction SilentlyContinue
}

# --- 4. Quellcode-Guard ---------------------------------------------------------------------
# Warum zusaetzlich zum Laufzeittest: Der gefaehrlichste Fall -- beim Loeschen einer Junction zu
# folgen und damit ~/.claude/skills zu vernichten -- ist WINDOWS-spezifisch und laesst sich auf
# macOS/Linux nicht ausloesen: Remove-Item entfernt dort nur den Link. Mit entferntem Schutz besteht
# der Laufzeittest hier also weiterhin (nachgewiesen). Ein bestandener Test wuerde die Regression
# damit durchwinken -- exakt das Muster aus dem Bug-Case vom 24.07.2026, wo der Regressionstest
# einen Pfad nachstellte, den der echte Fall nie nimmt. Der Guard prueft darum die Struktur.
$libPfad = Join-Path (Split-Path -Parent $PSScriptRoot) 'migration-lib.ps1'
$lib = Get-Content -LiteralPath $libPfad -Raw

function Get-FunktionsBlock {
    # Liefert den Text einer Funktion bis zur naechsten Funktionsdefinition (bzw. Dateiende).
    param([string]$Text, [string]$Name)
    $start = $Text.IndexOf("function $Name")
    if ($start -lt 0) { return '' }
    $next = $Text.IndexOf("`nfunction ", $start + 1)
    if ($next -lt 0) { return $Text.Substring($start) }
    return $Text.Substring($start, $next - $start)
}

$loeschBlock = Get-FunktionsBlock -Text $lib -Name 'Remove-BaumSicher'
Assert ($loeschBlock -match 'Test-IstVerknuepfung') `
    'Remove-BaumSicher prueft nicht auf Junctions. Auf Windows kann damit der Inhalt HINTER der Junction (~/.claude/skills) mitgeloescht werden.'
Assert ($loeschBlock -match '\[IO\.Directory\]::Delete\([^)]*\$false\)') `
    'Remove-BaumSicher loest Junctions nicht per Directory.Delete(pfad, $false) auf. Remove-Item -Recurse allein ist auf Windows nicht sicher.'

foreach ($name in @('Copy-FehlendeDateien', 'Get-FehlendeDateien')) {
    $block = Get-FunktionsBlock -Text $lib -Name $name
    Assert ($block -match 'Test-IstVerknuepfung[^\r\n]*\)\s*\{\s*continue') `
        "$name ueberspringt Junctions nicht. Der Skills-Bestand wuerde als echte Kopie materialisiert bzw. als fehlend gemeldet."
}

# Get-ChildItem ohne -Force uebersieht versteckte Dateien -- und damit .credentials.json/.claude.json.
Assert (($lib | Select-String -Pattern 'Get-ChildItem' -AllMatches).Matches.Count -eq
        ($lib | Select-String -Pattern 'Get-ChildItem[^\r\n]*-Force' -AllMatches).Matches.Count) `
    'Es gibt Get-ChildItem-Aufrufe ohne -Force. Versteckte Dateien wie der Login-Token wuerden uebersehen.'

if ($failures.Count -gt 0) {
    Write-Host 'Migrationstest FEHLGESCHLAGEN:' -ForegroundColor Red
    foreach ($f in $failures) { Write-Host "  - $f" -ForegroundColor Red }
    throw "Profil-Migrationstest: $($failures.Count) Verstoss/Verstoesse."
}

Write-Output 'Profil-Migrationstest bestanden (9 Laufzeitpruefungen + 5 Quellcode-Guards).'
