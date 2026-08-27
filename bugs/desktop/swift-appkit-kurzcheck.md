# macOS-Desktop — Swift / AppKit (Overlay-Apps) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektüre
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Permission weg nach jedem Rebuild (Mic/AX) | Stabile Bundle-ID + echtes Zertifikat, fester Pfad | §H1 |
| 2 | App crasht beim ersten Mic-Zugriff | `NSMicrophoneUsageDescription` in Info.plist setzen | §E1 |
| 3 | `swiftc`-Binary zeigt nie Mic-Prompt | Info.plist mit Bundle-ID einbetten; `.app` per `open` starten | §E2 |
| 4 | Textfeld im Overlay nicht fokussierbar | `canBecomeKey` UND `canBecomeMain` in NSPanel `true` | §A1 |
| 5 | Panel zeigt Fokus, tippt aber nicht | `.nonactivatingPanel` im Init setzen, nie per `setStyleMask` | §A2 |
| 6 | App kommt nicht in den Vordergrund | Kein `activate(ignoringOtherApps:)` — parameterlos `activate()` | §A4 |
| 7 | Hotkeys/Tap sterben nach Stunden/Sleep | Im Callback `tapDisabledByTimeout` abfangen + `tapEnable` | §D4 |
| 8 | CGEventTap-Callbacks feuern nie | Input Monitoring (nicht Accessibility) anfordern | §D5 |
| 9 | ⌥/⌥⇧-only Hotkey scheitert (Fehler -9868) | Auf 15.0/15.1 ⌘ oder ⌃ dazunehmen (gefixt ab 15.2) | §D1 |
| 10 | Overlay verschwindet bei Space/Fullscreen | Hohes Level + `[.canJoinAllSpaces, .stationary]` | §B2 |
| 11 | Overlay ragt aus Monitor / springt raus | Position immer auf `visibleFrame` klemmen (nicht löschen) | §B5 |
| 12 | TCC verweigert nach Info.plist-Edit still | Reihenfolge: bauen → plist final → DANN signieren | §H4 |
| 13 | AVAudioEngine-Tap crasht (Sample-Rate) | Tap-Format `inputNode.outputFormat(forBus:0)` nehmen | §E4 |
| 14 | Aufnahme tot nach AirPods/USB-Wechsel | `AVAudioEngineConfigurationChange` behandeln, Tap neu | §E5 |
| 15 | `codesign --deep` zum Signieren | Top-Bundle ohne `--deep`, Nested einzeln innen→außen | §G6 |
| 16 | `… is inaccessible due to 'fileprivate'` | `private extension` auf Dateiebene = fileprivate — geteilte Paletten intern lassen | §L1 |
| 17 | `ambiguous use of 'greatestFiniteMagnitude'` | Typ hinschreiben: `CGFloat.greatestFiniteMagnitude` | §L2 |
| 18 | `requires explicit use of 'self'` in Log-Aufruf | Parameter ist `@autoclosure @escaping` → `self.` schreiben, `@escaping` NICHT entfernen | §L3 |
| 19 | Dialog schließen killt laufende Overlay-Aufnahme | Besitz-Flag: nur die EIGENE Aufnahme stoppen | §M1 |
| 20 | Zweiter Recorder-Start, erste Aufnahme leer | `start()` muss bei laufender Aufnahme werfen, nicht überschreiben | §M2 |
| 21 | Zweite Spracheingabe löscht die erste | Im Eingabefeld anhängen statt `setText` | §M3 |
| 22 | Test schreibt in `~/Library/Application Support` | Vorher sichern + Hash-Vergleich; nie fremde Einträge löschen | §M4 |

> **Vor jedem „plattformübergreifend"-Commit:** die andere Plattform WIRKLICH übersetzen.
> Ein reiner Compile-Check (`swiftc -o /tmp/x <alle Dateien>`) dauert Sekunden und braucht weder
> Signierung noch Deploy-Guard. Ohne Xcode-Projekt kompiliert keine IDE beim Tippen mit —
> ein Fehler fällt sonst erst Wochen später auf (§L1).
