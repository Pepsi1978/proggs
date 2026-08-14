# SunoDownload

Lädt **alle Songs deiner eigenen Suno-Bibliothek** (https://suno.com/me) als MP3-Dateien herunter —
durchnummeriert, mit dem exakten Songtitel, sortiert vom **ältesten zum neuesten** Song.

```
001 - Erster Song.mp3
002 - Zweiter Song.mp3
003 - Dritter Song.mp3
...
```

---

## So startest du es

1. Doppelklick auf **`Songs-laden.cmd`**.
2. Es öffnet sich ein Chrome-Fenster mit Suno. **Melde dich dort einmal an.**
3. Ab da läuft alles von allein: das Fenster einfach offen lassen und warten.
4. Die MP3s landen in **`C:\Users\barwa\Music\Suno`**.

Die Anmeldung wird in einem eigenen Browser-Profil gespeichert. Beim zweiten Start bist du
normalerweise sofort angemeldet und musst gar nichts mehr tun.

### Anderer Zielordner

```cmd
Songs-laden.cmd "D:\Musik\Meine Suno Songs"
```

### Aus der Kommandozeile

```cmd
node suno-download.ts
node suno-download.ts "D:\Musik\Meine Suno Songs"
```

---

## Was passiert im Hintergrund

| Schritt | Was gemacht wird |
|---------|------------------|
| 1. Browser | Echtes Chrome mit eigenem Profil (`.browser-profil/`), damit die Anmeldung erhalten bleibt |
| 2. Anmeldung | Das Skript wartet, bis eine gültige Sitzung existiert (max. 15 Minuten) |
| 3. Bibliothek lesen | Drei Wege gleichzeitig: mitlesen der Netzwerk-Antworten, Durchblättern der Suno-API, Scrollen der Seite für nachgeladene Einträge |
| 4. Sortieren | Nach Erstellungsdatum aufsteigend — Nummer `001` ist der älteste Song |
| 5. Laden | Jede Datei einzeln mit bis zu 3 Versuchen; erst nach vollständigem Download umbenannt |

### Abbruch ist kein Problem

Wenn du das Fenster schließt oder das Internet wegbricht: einfach neu starten. Bereits vollständig
geladene Dateien werden erkannt und übersprungen, es wird nur der Rest geholt.

Halb geladene Dateien liegen als `.teil` vor und werden nie als fertige MP3 gewertet.

---

## Wenn etwas nicht klappt

**„Keine Songs gefunden"**
Die Anmeldung hat nicht gegriffen oder die Bibliothek war noch nicht geladen. Erst im Chrome-Fenster
prüfen, ob unter https://suno.com/me wirklich Songs zu sehen sind, dann neu starten.

**Einzelne Songs mit `❗`**
Das Skript listet sie am Ende namentlich auf. Nochmal starten — fertige Dateien werden übersprungen,
nur die fehlenden werden erneut versucht.

**Protokoll**
Jeder Lauf schreibt eine Zeile pro Ereignis nach `logs/suno-download.jsonl` — dort steht bei einem
Fehler die genaue Ursache samt URL.

---

## Technisches

- **Node.js 24+** (führt TypeScript direkt aus, kein Build-Schritt nötig)
- **playwright-core** — steuert dein installiertes Chrome (kein eigener Browser-Download)
- Keine Zugangsdaten im Code: die Anmeldung passiert ausschließlich im Browser, das Skript sieht nur
  die Sitzung des laufenden Fensters

### Nicht im Repository

`node_modules/`, `.browser-profil/` (enthält die Sitzung!), `logs/` und alle MP3-Dateien sind
per `.gitignore` ausgeschlossen.

---

Version 1.0.0 (14.08.2026, 18:09 Uhr)
