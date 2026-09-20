# Updater-Zentrale macOS — Projektregeln

## Version (Regel 6 der globalen CLAUDE.md)

Die Version steht in `UpdaterZentrale/Info.plist`, in **zwei** Schlüsseln, die immer gleich
bleiben müssen:

```xml
<key>CFBundleVersion</key>          <string>1.0.0</string>
<key>CFBundleShortVersionString</key> <string>1.0.0</string>
```

Form: `1.0.0` (drei Zahlen, wie bei OpenLauncherMac).

**Den Zeitstempel nie von Hand eintragen.** `build.sh` setzt `BuildTimestamp` beim Bauen aus der
echten Systemzeit; in der App steht er unten rechts neben der Version. Der Platzhalter
`__BUILD_TIMESTAMP__` in der Quell-Info.plist bleibt unverändert stehen.

## Bauen und installieren

```bash
cd ~/proggs/UpdaterZentrale-macOS
bash build.sh
pkill -x UpdaterZentrale 2>/dev/null
rm -rf "/Applications/Updater-Zentrale macOS.app"
ditto "build/Updater-Zentrale macOS.app" "/Applications/Updater-Zentrale macOS.app"
open "/Applications/Updater-Zentrale macOS.app"
```

`ditto` statt `cp -R`: nur ditto überträgt erweiterte Attribute und die Signatur vollständig.

## Fallstricke, die hier schon zugeschlagen haben

1. **Xcode-Lizenz.** `swiftc` bricht auf diesem Rechner ab, solange `xcode-select -p` auf
   Xcode.app zeigt. `build.sh` setzt deshalb `DEVELOPER_DIR=/Library/Developer/CommandLineTools`,
   und `Services/Kommandozeile.swift` gibt dasselbe an alle Kindprozesse weiter — sonst scheitern
   `update-launcher.sh` und `rebuild-overlay.sh`, wenn die App sie aufruft.

2. **Autolayout und Beschriftungen.** In `Views/Controls.swift` unterscheidet
   `UI.beschriftung(…, einzeilig:)` zwei Sorten:
   * `einzeilig: true` für Versionen, Zustände, Plaketten — sie dürfen **nicht** schrumpfen,
     sonst bricht eine Versionsnummer zeichenweise als Ziffernsäule um.
   * `einzeilig: false` (Vorgabe) für Fließtext — er muss nachgeben, sonst macht eine lange
     Beschreibung das Fenster breiter als den Bildschirm.

   Beides ist schon passiert. Bei neuen Beschriftungen bewusst entscheiden.

3. **Fenstergröße.** Nicht die Windows-Maße (1340 × 880) übernehmen — dieses MacBook hat nur
   1280 × 832 Punkte. `HauptFensterController` rechnet aus `NSScreen.visibleFrame`.

4. **`main.swift` muss in `build.sh` zuletzt stehen.** Nur die Datei mit diesem Namen darf Code
   auf oberster Ebene haben.

5. **PATH.** Eine aus dem Finder gestartete App erbt nur `/usr/bin:/bin:/usr/sbin:/sbin`.
   Homebrew und npm werden deshalb über absolute Pfade aus `Services/Pfade.swift` aufgerufen, und
   `Kommandozeile.umgebung` ergänzt den PATH. Nie `brew` oder `npm` blank aufrufen.

## Katalog

`programs.json` wird zur Laufzeit gelesen. **Ein neues Programm ist eine JSON-Änderung, kein
Neubau** — das ist der Kern des Entwurfs und gilt auf beiden Plattformen.

Das Schema ist absichtlich identisch zu `~/proggs/UpdateZentrale/programs.json` (Windows).
Wird dort ein Feld ergänzt, hier mitziehen.

## Die Skripte anderer Projekte nie anfassen

`OpenLauncherMac/update-launcher.sh` und `rebuild-overlay.sh` gehören ihren eigenen Projekten.
Die Updater-Zentrale **ruft sie nur auf**, nie mit einem Force-Schalter, und beendet deren
Prozesse niemals selbst. Fehlt einem Skript eine Rückfrage, stellt die App sie selbst
(leeres `statusPraefix`) — das Skript wird dafür nicht geändert.
