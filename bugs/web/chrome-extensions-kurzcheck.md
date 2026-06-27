# Chrome-Erweiterungen (Manifest V3) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Dieser Kurzcheck ist die Vorab-Pflichtlektuere
> (Stufe A, `Read` mit `limit=80`). Der Volltext darunter ist Pflicht bei JEDEM Fehler in
> diesem Bereich (Stufe B). Der Kurzcheck ersetzt den Volltext nicht.

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | Erweiterung verschwindet/instabil nach Neustart | ZUERST Ordner-Pfad wechseln, NICHT ID/Code | #1 |
| 2 | Laedt nicht / instabil | Keine `_`-Dateien im Ordner (ausser `_locales`) | #2 |
| 3 | State (Login/Abo/Tab) ploetzlich weg | Nie globale Vars → `chrome.storage`; SW stirbt nach 30s | #3 |
| 4 | Event feuert nach SW-Neustart nicht | Alle Listener synchron im Top-Level registrieren | #5 |
| 5 | Timer feuert nie | `chrome.alarms` statt `setTimeout`/`setInterval` (min 30s) | #6, #7 |
| 6 | „message port closed before response" | Async `onMessage`: `return true` (oder Promise ab 148) | #14 |
| 7 | Erweiterung reagiert nicht in offenen Tabs | Per `executeScript` nachinjizieren + Doppel-Guard | #17 |
| 8 | Seiten-JS-Variable ist `undefined` | Content-Script im Isolated World; `world:"MAIN"` noetig | #18 |
| 9 | „Extension context invalidated" | `chrome.runtime?.id` pruefen / try-catch + Cleanup | #19 |
| 10 | Mikrofon/Audio geht nicht | Nicht im SW → Offscreen-Doc/Content-Script | #48 |
| 11 | Button wird rot, aber kein Ton | ZUERST Chrome-Mic-Auswahl pruefen, NICHT den Code | #54 |
| 12 | WebSocket-Handshake schlaegt fehl | Host als `wss://` in `host_permissions` (MV3) | #53 |
| 13 | `storage.sync.set` QUOTA-Fehler | 8 KB/Item: pro Datensatz EIN Key | #28, #37 |
| 14 | Erweiterung nach Update deaktiviert | Neue Permission als `optional_permissions` | #40 |
| 15 | Sicherheit / Store | `sender` validieren, keine Secrets im Bundle; MV2 ist tot (≥139) | #55, #58, #64 |
| 16 | STT-Live-Vorschau springt im Feld / wird statt finaler Fassung gesendet | Vorschau ins schwebende Overlay (NICHT ins `contenteditable`); `previewActive`-Riegel: nur finale Whisper-Fassung ins Feld | #74 |
