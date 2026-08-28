# cortex-menu-install.ps1 - Traegt "Cortex: Hier schnell einfuegen" ins Explorer-Kontextmenue ein.
#
# Danach ist der Ablauf genau wie gewohnt, nur schnell:
#   1. Dateien/Ordner markieren, Strg+C (kopieren) oder Strg+X (verschieben)
#   2. Zielordner oeffnen - lokal oder auf Y:/Z:
#   3. Rechtsklick ins Leere -> "Cortex: Hier schnell einfuegen"
#
# Unter Windows 11 sitzt der Eintrag im erweiterten Menue: Rechtsklick -> "Weitere Optionen
# anzeigen" (oder direkt Umschalt+F10). Das ist so gewollt - das schlanke Win11-Menue nimmt
# nur Eintraege von signierten Shell-Erweiterungen auf, ein Skript kommt dort nicht hinein.
#
# BENUTZUNG (normales Fenster, KEIN Administrator noetig - alles unter HKCU):
#   .\cortex-menu-install.ps1
#   .\cortex-menu-install.ps1 -Entfernen
#
# WICHTIG: Der Eintrag zeigt auf den Pfad, an dem cortex-paste.ps1 JETZT liegt. Wird das
# Repo verschoben, das Skript einfach erneut ausfuehren.

param(
    [switch]$Entfernen
)

$ErrorActionPreference = "Stop"

$Skript = Join-Path $PSScriptRoot "cortex-paste.ps1"
$Beschriftung = "Cortex: Hier schnell einfuegen"

# Zwei Orte, damit es sich anfuehlt wie das eingebaute "Einfuegen":
#   Directory\Background = Rechtsklick in den leeren Bereich eines geoeffneten Ordners (%V)
#   Directory            = Rechtsklick direkt auf einen Ordner (%1)
#   Drive                = Rechtsklick auf das Laufwerk selbst, z.B. Y: in "Dieser PC" (%1)
$Ziele = @(
    @{ Pfad = "HKCU:\Software\Classes\Directory\Background\shell\CortexPaste"; Arg = "%V" },
    @{ Pfad = "HKCU:\Software\Classes\Directory\shell\CortexPaste";            Arg = "%1" },
    @{ Pfad = "HKCU:\Software\Classes\Drive\shell\CortexPaste";                Arg = "%1" }
)

if ($Entfernen) {
    foreach ($z in $Ziele) {
        if (Test-Path $z.Pfad) {
            Remove-Item -Path $z.Pfad -Recurse -Force
            Write-Host "Entfernt: $($z.Pfad)"
        }
    }
    Write-Host ""
    Write-Host "[OK] Kontextmenue-Eintrag entfernt." -ForegroundColor Green
    exit 0
}

if (-not (Test-Path $Skript)) {
    Write-Host "[FEHLER] cortex-paste.ps1 liegt nicht neben diesem Skript ($PSScriptRoot)." -ForegroundColor Red
    exit 1
}

foreach ($z in $Ziele) {
    $cmdPfad = Join-Path $z.Pfad "command"
    New-Item -Path $cmdPfad -Force | Out-Null

    Set-ItemProperty -Path $z.Pfad -Name "(default)" -Value $Beschriftung
    # Icon aus der Shell32 (Ordner mit Pfeil) - rein kosmetisch, macht den Eintrag auffindbar.
    Set-ItemProperty -Path $z.Pfad -Name "Icon" -Value "%SystemRoot%\System32\shell32.dll,45"

    # WICHTIG: powershell.exe (5.1) mit -STA. Die Zwischenablage ist COM und braucht ein
    # Single-Threaded-Apartment; pwsh 7 laeuft ab Werk als MTA und wuerde beim Clipboard-Zugriff
    # scheitern. -NoProfile haelt den Start schnell, -ExecutionPolicy Bypass umgeht die Richtlinie
    # nur fuer diesen einen Aufruf.
    $befehl = "powershell.exe -STA -NoProfile -ExecutionPolicy Bypass -File `"$Skript`" -Ziel `"$($z.Arg)`""
    Set-ItemProperty -Path $cmdPfad -Name "(default)" -Value $befehl

    Write-Host "Eingetragen: $($z.Pfad)"
}

Write-Host ""
Write-Host "[OK] Kontextmenue-Eintrag angelegt." -ForegroundColor Green
Write-Host ""
Write-Host "So benutzt du ihn:"
Write-Host "  1. Dateien markieren, Strg+C (oder Strg+X zum Verschieben)"
Write-Host "  2. Zielordner oeffnen"
Write-Host "  3. Rechtsklick ins Leere -> 'Weitere Optionen anzeigen' -> '$Beschriftung'"
Write-Host ""
Write-Host "Tipp: Umschalt+F10 oeffnet das alte Menue direkt, ohne den Zwischenschritt."
Write-Host "Entfernen jederzeit mit:  .\cortex-menu-install.ps1 -Entfernen"
