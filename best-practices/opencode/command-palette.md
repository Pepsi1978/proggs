# OpenCode CLI — Befehlsliste / Command Palette (Strg+P)

> **Stand: 2026-06-19.** Quellen: offizielle Doku (`opencode.ai/docs`, `opencode.school`) **und**
> direkter OpenCode-Quellcode (`sst/opencode` @ Commit `5d2dc8`, plus `anomalyco/opencode` v1.17.8 für
> die schlanke Variante). Alle Befehlsnamen/Verhalten sind **offiziell** (aus dem Quellcode der
> TUI-Befehlsregistrierung verifiziert, nicht geraten/übersetzt).
>
> **Worum es geht:** Mit `Strg+P` öffnet die OpenCode-TUI die **Befehlsliste (Command Palette)** — ein
> Suchfenster mit allen Aktionen, gruppiert in Bereiche. Diese Datei dokumentiert die vollständige Liste
> 1:1 (englische Original-Labels), die Gruppen-Reihenfolge und die deutschen Erklärungen.

---

## Kern-Erkenntnisse

1. **Öffnen:** `Strg+P` (Keybind `command_list`). **Leader-Taste** = `Strg+X` (erst drücken, loslassen,
   dann Buchstabe; z. B. `Strg+X N` = New Session).
2. **Gruppen-Reihenfolge** in der interaktiven TUI: **Suggested · Session · Prompt · Agent · Provider ·
   System · VCS**.
3. **„Suggested"** ist **dynamisch**: Einträge mit `suggested: true` erscheinen oben in „Suggested" UND
   nochmal in ihrer eigenen Gruppe (z. B. Switch/New Session, Switch Model, Share Session, Connect Provider).
   Welche oben stehen, hängt vom Kontext ab.
4. **Toggle-Befehle:** Der Name zeigt die **nächste** mögliche Aktion (z. B. „Hide Sidebar" wenn die Leiste
   sichtbar ist → danach heißt der Eintrag „Show Sidebar"). Gilt für alle Hide/Show-, Enable/Disable-,
   Collapse/Expand-, Lock/Unlock- und „Switch to … Mode"-Einträge.
5. **Zwei Varianten-Befehle (Agent):** `Variant Cycle` (springt mit `Strg+T` direkt zur nächsten
   Denk-Stufe) **und** `Switch Model Variant` (öffnet die Auswahlliste). Beide sind getrennte Einträge.
6. **VCS = Git:** `Open Diff Viewer` zeigt die Datei-Änderungen der Sitzung; `Undo`/`Redo` (in dieser
   Version unter „Session") nutzen Git → Projekt muss ein Git-Repo sein.
7. **Move Session = Workspaces:** verschiebt die Sitzung in einen *Workspace* — eine isolierte Projekt-Kopie
   auf eigenem Git-Zweig (Git-Worktree `opencode/<name>`), Desktop-Feature für parallele Aufgaben.

---

## ⚠️ Versions-Nuance (wichtig für Re-Recherche)

Es gibt **zwei** TUI-Oberflächen im OpenCode-Code:

| Oberfläche | Pfad | Befehlsliste |
|-----------|------|--------------|
| **Interaktive TUI** (`tui`-Route) | `packages/opencode/src/cli/cmd/tui/…` | **reiche** 7-Gruppen-Palette (Suggested…VCS) — **das ist Franks Anzeige** |
| **Schlanke „run"-Variante** | `packages/opencode/src/cli/cmd/run/footer.command.tsx` | nur Session/Prompt/Agent/System, deutlich weniger Einträge |

In **v1.17.8** (anomalyco, Juni 2026) ist im Repo nur noch die schlanke `run`-Variante zu finden; die
reiche `tui`-Route existierte im Stand `5d2dc8` (sst/opencode, März 2026) und in Franks laufender Version.
**Bei künftiger Prüfung:** zuerst klären, welche der beiden Oberflächen der Nutzer offen hat.

### Quell-Dateien, in denen die Befehle registriert werden (`command.register([...])`)

| Datei | Welche Befehle |
|-------|----------------|
| `…/tui/component/dialog-command.tsx` | Palette-Container, „Commands"-Titel, **Suggested**-Logik (`suggested:true`) |
| `…/tui/app.tsx` | Switch/New Session, Switch Model, Switch Agent, Toggle MCPs, Variant-Befehle, Connect Provider, **alle System-Einträge** |
| `…/tui/routes/session/index.tsx` | alle **Session**-Befehle (Share, Rename, Jump, Fork, Compact, Undo/Redo, Toggles, Copy/Export …) |
| `…/tui/component/prompt/index.tsx` | **Prompt**-Gruppe: Open Editor, Skills, Stash prompt/pop/list, Interrupt |

Befehle ohne festes Tastenkürzel/Slash erscheinen nur in der Liste. Keybind-IDs offiziell unter
`opencode.ai/docs/keybinds`, Slash-Befehle unter `opencode.ai/docs/tui`. `offiziell`.

---

## Vollständige Befehlsliste — 1:1 in Anzeige-Reihenfolge

> Stand aus Franks geöffneter Session (Strg+P) am 2026-06-19, gegen den Quellcode verifiziert.
> Format: **Englischer Befehl** · `Tastenkürzel / Slash` — deutsche Erklärung.

### SUGGESTED (Vorgeschlagen — dynamisch, oben; stehen unten nochmal in ihrer Gruppe)
- **Share Session** · `/share` — Internet-Link zum Gespräch erstellen (bzw. kopieren, wenn schon geteilt).
- **Switch Session** · `Strg+X L · /sessions` — Liste der bisherigen Gespräche öffnen und zu einem wechseln.
- **New Session** · `Strg+X N · /new` — neues, leeres Gespräch ohne alten Verlauf starten.
- **Switch Model** · `Strg+X M · /models` — KI-Modell wechseln.

### SESSION (ein einzelnes Gespräch)
- **Open Editor** · `Strg+X E · /editor` — externes Schreibprogramm für längere Nachrichten öffnen.
- **Move Session** — Sitzung in einen anderen Workspace (isolierte Git-Zweig-Kopie) verschieben.
- **Share Session** · `/share` — Internet-Link zum Gespräch erstellen/kopieren.
- **Rename Session** · `Strg+R · /rename` — dem Gespräch einen eigenen Namen geben.
- **Jump to Message** · `Strg+X G · /timeline` — Zeitleiste; zu einer früheren Nachricht zurückspringen.
- **Fork Session** · `/fork` — ab einer Nachricht eine Abzweigung (Kopie) erstellen.
- **Compact Session** · `Strg+X C · /compact` — langes Gespräch zusammenfassen, Kontext-Platz freimachen.
- **Undo Previous Message** · `Strg+X U · /undo` — letzte Nachricht, KI-Antwort + Dateiänderungen zurücknehmen (Git).
- **Hide Sidebar** · `Strg+X B` — Seitenleiste aus-/einblenden (Umschalter).
- **Disable Code Concealment** — Verbergen von Code-Stellen aus-/einschalten (Umschalter).
- **Show Timestamps** — Uhrzeit pro Nachricht ein-/ausblenden (Umschalter).
- **Collapse Thinking** · `/thinking` — Gedanken (Reasoning) zu-/aufklappen (Umschalter).
- **Hide Tool Details** — ausführliche Werkzeug-Schritte aus-/einblenden (Umschalter).
- **Toggle Session Scrollbar** — Bildlaufleiste ein-/ausblenden.
- **Show Generic Tool Output** — allgemeine Werkzeug-Ausgabe ausführlich/knapp (Umschalter).
- **Copy Last Assistant Message** · `Strg+X Y` — letzte KI-Antwort in die Zwischenablage kopieren.
- **Copy Session Transcript** · `/copy` — gesamtes Gespräch als Text kopieren.
- **Export Session Transcript** · `Strg+X X · /export` — Gespräch als Markdown speichern + im Editor öffnen.
- **Switch Session** · `Strg+X L · /sessions` — (wie oben) Gespräch wechseln.
- **New Session** · `Strg+X N · /new` — (wie oben) neues Gespräch.

### PROMPT (rund um das Eingabefeld)
- **Skills** · `/skills` — Liste der „Skills" (vorbereitete Spezial-Fähigkeiten) öffnen.

### AGENT (Modell & Arbeitsweise der KI)
- **Switch Model** · `Strg+X M · /models` — aktives KI-Modell wechseln.
- **Switch Agent** · `Strg+X A · /agents` — KI-Rolle/Modus wechseln (z. B. Build ↔ Plan).
- **Toggle MCPs** · `/mcps` — angebundene Zusatz-Werkzeuge (MCP) einzeln an-/ausschalten.
- **Variant Cycle** · `Strg+T` — mit einem Tastendruck direkt zur nächsten Denk-Stufe (Variante/Reasoning).
- **Switch Model Variant** — Auswahlliste der Denk-Stufen öffnen und gezielt eine wählen.

### PROVIDER (Anbieter)
- **Connect Provider** · `/connect` — neuen Anbieter samt Zugangsschlüssel (API-Key) hinzufügen.

### SYSTEM (Programm & Ansicht)
- **View Status** · `Strg+X S · /status` — Zustand: Modell, Verbindung, Verbrauch.
- **Switch Theme** · `Strg+X T · /themes` — Farb-Design wählen.
- **Switch to Light Mode** — von dunkel auf hell wechseln (im Hellmodus: „Switch to Dark Mode").
- **Unlock Theme Mode** — feste Hell/Dunkel-Einstellung freigeben (Gegenstück: „Lock Theme Mode").
- **Help** · `/help` — Hilfe-Fenster.
- **Open Docs** — Online-Handbuch im Browser öffnen.
- **Exit the App** · `Strg+X Q · /exit` — OpenCode komplett schließen.
- **Toggle Debug Panel** — technisches Debug-Fenster ein-/ausblenden.
- **Toggle Console** — Protokollfenster (Konsole/Log) ein-/ausblenden.
- **Write Heap Snapshot** — Speicher-Abzug der App schreiben (Entwickler-Diagnose).
- **Disable Terminal Title** — Fenstertitel mit Sitzungsnamen an-/ausschalten (Umschalter).
- **Disable Animations** — bewegte Effekte an-/ausschalten (Umschalter).
- **Disable File Context** — Anzeige der mitgegebenen Hintergrund-Dateien an-/ausschalten (Umschalter).
- **Disable Diff Wrapping** — Zeilenumbruch in der Änderungs-Ansicht (Diff) an-/ausschalten.
- **Disable Paste Summary** — beim Einfügen großer Texte Kurzfassung statt Volltext an-/ausschalten (Umschalter).
- **Disable Session Directory Filtering** — Filter „nur Gespräche dieses Projektordners" an-/ausschalten.
- **Plugins** — Verwaltung der Erweiterungen (Plugins) öffnen.
- **Install Plugin** — neue Erweiterung (Plugin) hinzufügen/einrichten.

### VCS (Versionsverwaltung / Git)
- **Open Diff Viewer** — Ansicht aller Datei-Änderungen der Sitzung (Diff „vorher/nachher"; nutzt Git).

---

## Quellen

- **offiziell** — OpenCode-Doku: [Keybinds](https://opencode.ai/docs/keybinds/), [TUI & Slash-Commands](https://opencode.ai/docs/tui/)
- **offiziell** — [OpenCode School – Workspaces](https://opencode.school/lessons/workspaces/) (Move Session / Workspaces = Git-Worktree-Kopie)
- **offiziell** — OpenCode-Quellcode (TUI-Befehlsregistrierung): `app.tsx`, `routes/session/index.tsx`,
  `component/prompt/index.tsx`, `component/dialog-command.tsx` (sst/opencode @5d2dc8); schlanke Variante:
  `cmd/run/footer.command.tsx` (anomalyco @v1.17.8)
- **extern** — Cheatsheet/Keybind-Referenzen (opencode.school/cheatsheet, johnlindquist-Gist, Jerry's Tech Blog) — als Sekundärquellen gegengeprüft.

> **Querverweis:** Bedien-Grundlagen & Tastenkürzel auch in `grundlagen-installation.md` dieser Sammlung.
> Wenn künftig Bugs/Fallen zur TUI auftauchen → Almanach `bugs/opencode/…` anlegen und hier verlinken.
