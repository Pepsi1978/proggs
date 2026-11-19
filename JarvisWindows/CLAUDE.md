# Jarvis Windows HP (JarvisWindows) – Projektregeln

Native .NET-10-WPF-App ohne XAML, Port von `JarvisMac` nach `PORT-VERTRAG.md`: gleiche Datei- und Typnamen, gleiche Texte, Windows-Gegenstücke. Eine neue Funktion in JarvisMac gehört auch hierher (Feature-Gleichstand) und umgekehrt. Dauerhafte Regel: Jede neue Funktion von JarvisWindows, der PC-Brücke oder des Vorwärmens bekommt sofort, im selben Auftrag, auch ihr Mac-Gegenstück (JarvisMac, `Jarvis/werkzeuge/pc-bruecke/jarvis_pc_bruecke.py`, `Werkzeuge/vorwaermen/einrichten.sh`).

## Version (Regel 6)
`<Version>` in `JarvisWindows.csproj`, Form `1.0.0`, pro Commit genau einmal die letzte Stelle erhöhen. Den Zeitstempel setzt der Bau selbst. Die App zeigt beides unten links und unter Einstellungen → Über.

## Bauen und installieren
`pwsh -File publish.ps1` baut, installiert nach `%LOCALAPPDATA%\Programs\JarvisWindowsHP`, richtet die Aufgabe `JarvisWindowsWecker` ein und startet die App.
Power-PC: `pwsh -File publish.ps1 -Geraet power` → Ordner `JarvisWindowsPower`, Aufgabe `JarvisWindowsWeckerPower`, Verknüpfung „Jarvis Windows Power-PC.lnk“, `geraet.json` neben der EXE. Eine HP-Installation auf demselben Rechner wird dabei beendet und entfernt (auf dem Power-PC läuft nur die Power-Identität). Aufräumen, Einzelinstanz-Mutex (`Local\JarvisWindows-Einzelinstanz`, Power mit `-power`) und Fensterbotschaften gelten je Identität; Texte an Modell und Kanal kommen aus `AppInfo` (`Kurz`, `Geschwister`), nie fest „Laptop“.

## Anbindung ans Handy
Eigener Kanal beim Relay neben dem des Macs: `/j/<Geheimnis>/hp/kanal` (App), `/pc/hp-befehl` (Wecker `tools/hp-wecker.ps1`), `/geraet/hp/...` (Handy, `WindowsHpJarvisFaehigkeit`, Werkzeuge `jarvis_windows_hp_*`). Power-PC entsprechend: `/j/<Geheimnis>/power/kanal`, `/pc/power-befehl`, `/geraet/power/...`, Werkzeuge `jarvis_windows_power_*`. Der Mac-Kanal bleibt unberührt.

## Schlüssel
Nur im Tresor (`Sources/Kern/Tresor.cs`, DPAPI), beim ersten Start aus `~/SK` übernommen. Nie ins Git, nie im Klartext.

## Farben, Live, Funktionsdatei
Wie in `JarvisMac/CLAUDE.md`: Farben nur über `Farbe`, `funktionen.md` nach jeder Änderung nachziehen. Der Live-Knopf startet die Gesprächsschleife von „Hey Jarvis“ (`AppSteuerung.Live`) wie am Handy seit 1.8.633; das Echtzeit-Live-Gespräch (`Sources/Live`) ist entfernt.
