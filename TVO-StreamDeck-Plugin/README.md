# TVO Auto-Enter Stream Deck Plugin

Spiegelt den orange/grau Auto-Enter-Status des **Terminal Voice Overlays** live
auf eine Stream-Deck-Taste — und schaltet ihn beidseitig synchron um.

## Wie es funktioniert

```
Voice Terminal Overlay  ◀──────  GET  /autoenter/status  ──────  Stream Deck Plugin
        (laeuft eh)               POST /autoenter/toggle                (Polling 500ms)
                                  127.0.0.1:5723
```

- **Maus-Klick im Overlay** → naechster Poll-Tick (max 500 ms) faerbt die Taste um.
- **Stream-Deck-Tastendruck** → `POST /autoenter/toggle` → Overlay-Button-Farbe wechselt + Taste folgt sofort.

Keine externe Erreichbarkeit, alles loopback (127.0.0.1) — kein Firewall-Eintrag noetig.

## Installation

1. Stream-Deck-Software starten (falls nicht eh laufend).
2. Doppelklick auf `dist/com.tvo.autoenter.streamDeckPlugin` (oder die Kopie auf dem Desktop).
3. Stream-Deck-Software fragt **"Plugin installieren?"** → **Ja**.
4. Stream-Deck-Editor oeffnen, rechts in der Aktionsliste den Eintrag **"TVO Auto-Enter Toggle"** auf eine freie Taste ziehen.
5. Fertig — die Taste folgt jetzt dem Overlay-Status.

## Erneut bauen nach Code-Aenderung

```powershell
pwsh -File build-plugin.ps1
```

Erzeugt die `.streamDeckPlugin`-Datei neu und kopiert sie auf den Desktop.

## Ordner-Struktur

```
TVO-StreamDeck-Plugin/
├── README.md                              # diese Datei
├── build-plugin.ps1                       # Build-Skript
├── com.tvo.autoenter.sdPlugin/            # eigentlicher Plugin-Quellcode
│   ├── manifest.json                      # Plugin-Manifest (Name, UUID, Actions)
│   ├── plugin.html                        # Bootstrap-Seite (laedt code.js)
│   ├── code.js                            # Plugin-Logik (WebSocket + Polling)
│   ├── inspector.html                     # Konfigurations-Panel (minimal)
│   └── icons/                             # 6 PNG-Icons (off/on, 1x/2x, plus Plugin-Kategorie)
└── dist/
    └── com.tvo.autoenter.streamDeckPlugin # Build-Artefakt (ZIP mit anderer Endung)
```

## Troubleshooting

- **Taste zeigt "offline"** — TVO laeuft nicht oder Port 5723 ist belegt. TVO starten / pruefen ob ein anderer Prozess Port 5723 hat (`netstat -ano | findstr 5723`).
- **Aenderungen am Plugin-Code zeigen sich nicht** — Stream Deck cached Plugins. Im Stream Deck Editor das Plugin einmal entfernen und neu installieren, oder Stream-Deck-Software komplett neu starten.
- **Taste flackert orange/grau** — Polling-Intervall (500 ms) ist normal sichtbar. Falls stoerend: in `code.js` `POLL_INTERVAL_MS` auf 1000 erhoehen.
