# Jarvis Windows HP (JarvisWindows) – Projektregeln

Native .NET-10-WPF-App ohne XAML, Port von `JarvisMac` nach `PORT-VERTRAG.md`: gleiche Datei- und Typnamen, gleiche Texte, Windows-Gegenstücke. Eine neue Funktion in JarvisMac gehört auch hierher (Feature-Gleichstand) und umgekehrt. Dauerhafte Regel: Jede neue Funktion von JarvisWindows, der PC-Brücke oder des Vorwärmens bekommt sofort, im selben Auftrag, auch ihr Mac-Gegenstück (JarvisMac, `Jarvis/werkzeuge/pc-bruecke/jarvis_pc_bruecke.py`, `Werkzeuge/vorwaermen/einrichten.sh`).

## Version (Regel 6)
`<Version>` in `JarvisWindows.csproj`, Form `1.0.0`, pro Commit genau einmal die letzte Stelle erhöhen. Den Zeitstempel setzt der Bau selbst. Die App zeigt beides unten links und unter Einstellungen → Über.

## Bauen und installieren
`pwsh -File publish.ps1` baut, installiert nach `%LOCALAPPDATA%\Programs\JarvisWindowsHP`, richtet die Aufgabe `JarvisWindowsWecker` ein und startet die App.

## Anbindung ans Handy
Eigener Kanal beim Relay neben dem des Macs: `/j/<Geheimnis>/hp/kanal` (App), `/pc/hp-befehl` (Wecker `tools/hp-wecker.ps1`), `/geraet/hp/...` (Handy, `WindowsHpJarvisFaehigkeit`). Der Mac-Kanal bleibt unberührt.

## Schlüssel
Nur im Tresor (`Sources/Kern/Tresor.cs`, DPAPI), beim ersten Start aus `~/SK` übernommen. Nie ins Git, nie im Klartext.

## Farben, Live, Funktionsdatei
Wie in `JarvisMac/CLAUDE.md`: Farben nur über `Farbe`, `funktionen.md` nach jeder Änderung nachziehen. Der Live-Knopf startet die Gesprächsschleife von „Hey Jarvis“ (`AppSteuerung.Live`) wie am Handy seit 1.8.633; das Echtzeit-Live-Gespräch (`Sources/Live`) ist entfernt.
