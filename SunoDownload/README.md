# SunoDownload

Lädt **alle Songs deiner eigenen Suno-Bibliothek** (https://suno.com/me) als MP3-Dateien herunter —
durchnummeriert, mit dem exakten Songtitel, sortiert vom **ältesten zum neuesten** Song.

```
001 - Erster Song.mp3
002 - Zweiter Song.mp3
003 - Dritter Song.mp3
...
```

Zielordner: **`C:\Sono Backup`**

---

## Warum zwei Schritte?

Google lässt eine Anmeldung in einem ferngesteuerten Browser nicht zu („Dieser Browser ist
möglicherweise nicht sicher"). Deshalb erledigt **dein eigener, ganz normaler Chrome** den Teil, der
eine Anmeldung braucht: Er liest nur die Songliste aus. Das Herunterladen selbst braucht keine
Anmeldung und macht das Programm dann allein.

---

## Schritt 1 — Songliste holen (einmalig, ca. 30 Sekunden)

1. In deinem normalen Chrome **https://suno.com/me** öffnen (du bist dort ja angemeldet).
2. **F12** drücken → oben den Reiter **„Console"** anklicken.
3. Den kompletten Inhalt von **`bibliothek-holen.js`** hineinkopieren und **Enter** drücken.
   - Chrome fragt beim ersten Mal nach: dann **`allow pasting`** eintippen, Enter, und nochmal einfügen.
4. Es erscheint: `✅ N Songs gefunden` — und die Datei **`suno-liste.json`** landet in deinem
   **Downloads**-Ordner.

Falls stattdessen der Hinweis auf den „Notweg" erscheint: die Bibliothek langsam bis ganz unten
scrollen und danach in der Konsole `sunoSpeichern()` eintippen.

## Schritt 2 — Songs herunterladen

Doppelklick auf **`Songs-laden.cmd`**. Fertig — das Programm findet die Liste im Downloads-Ordner
von allein und lädt alles nach `C:\Sono Backup`.

### Anderer Zielordner

```cmd
Songs-laden.cmd "D:\Musik\Meine Suno Songs"
```

### Bestimmte Liste verwenden

```cmd
Songs-laden.cmd "C:\Users\barwa\Downloads\suno-liste.json"
```

---

## Gut zu wissen

**Abbruch ist kein Problem.** Einfach neu starten: fertige Dateien werden erkannt und übersprungen,
nur der Rest wird geholt. Halb geladene Dateien heißen `.teil` und werden nie als fertige MP3 gewertet.

**Neue Songs später.** Schritt 1 nochmal machen (neue Liste), dann Schritt 2 — es werden nur die
neu hinzugekommenen Songs geladen. Achtung: Die Nummerierung verschiebt sich nicht, weil neue Songs
hinten angehängt werden.

**Einzelne Fehler.** Das Programm listet fehlgeschlagene Songs am Ende namentlich auf. Nochmal
starten genügt.

**Protokoll.** Jeder Lauf schreibt nach `logs/suno-download.jsonl` — dort steht bei einem Fehler die
genaue Ursache samt URL.

---

## Technisches

- **Node.js 24+** — führt TypeScript direkt aus, kein Build-Schritt nötig
- Keine Zugangsdaten im Code, kein gespeichertes Passwort, kein Token auf der Festplatte
- Das Konsolen-Skript liest ausschließlich, verändert nichts und sendet nichts an Dritte
- Die Audio-Dateien liegen auf dem Suno-CDN und brauchen für den Abruf keine Anmeldung

### Nicht im Repository

`node_modules/`, `logs/`, `.browser-profil/`, `suno-liste*.json` und alle MP3-Dateien sind per
`.gitignore` ausgeschlossen.

---

Version 1.1.0 (14.08.2026, 18:15 Uhr)
