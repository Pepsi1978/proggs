---
name: session-flaschenhals
description: >
  Wertet als allerletzter Schritt einer Programmiersitzung deren echten Verlauf aus (Wartezeiten, Fehler,
  Doppelarbeit) und setzt jede belegte, knappe Beschleunigung selbst um, auch nach einmaligen Fehlern, solange
  sie das System nicht durch zusätzlichen Kontext verlangsamt. Läuft ohne Rückfragen.
  Auch auf Zuruf: "Sitzung auswerten", "Flaschenhälse", "warum hat das so lange gedauert".
---

# Session-Flaschenhals

Messender Rückblick auf Basis des JSONL-Verlaufs. Ergänzt `selbstbeobachtung` (Erinnerung) um Zahlen.
Ziel: künftige ähnliche Aufgaben schneller, bei gleicher Qualität. Läuft **nach** allem anderen
(Abschlussmeldung, Review-Frage erledigt), **stellt keine Fragen**, ändert **keinen App-Code**.

Budget: Ziel unter 3 min und unter 15k Tokens. Nichts Neues gefunden und keine Sofort-Korrektur (Schritt 2)
→ nach Schritt 3 mit einer Zeile enden.

## 1. Messen

```
python -X utf8 "<skills>/session-flaschenhals/analyse.py" <sitzungs-id>     # diese Sitzung
python -X utf8 "<skills>/session-flaschenhals/analyse.py" --letzte=15       # Vergleich + Wirkung
```
Mac und Hostinger-Server: `python3` statt `python`. Ablauf überall gleich; das Skript findet den
Profilordner selbst (`CLAUDE_CONFIG_DIR`, Server `~/.openlauncher/claude/minimal`).
Sitzungs-ID = die UUID im Scratchpad-Pfad. Das Skript liest nur, braucht ~1 s und gibt < 1k Tokens aus.
Andere CLIs (Codex, OpenCode) ohne eigenen Claude-Verlauf: nur den Vergleich laufen lassen und die
eigene Sitzung aus dem Gedächtnis nach denselben Kriterien einordnen.

## 2. Einordnen

- Zeit kommt in drei Töpfe: **Werkzeug** (Befehle, Builds), **Modell** (Denken/Schreiben),
  **Frank/Ruhe** (Antwortzeit, Pausen, Hintergrund). Frank/Ruhe ist nie ein Befund gegen den Agenten.
- **Grundsatz (Frank, 08.11.2026): Netto-Zeitgewinn entscheidet, nicht die Häufigkeit.** Jeder Fehler,
  jeder vermeidbar langsame Befehl und jede Doppelarbeit, die Agent-Zeit gekostet hat, ist ein Kandidat —
  **auch einmalig**. Umsetzen, wenn eine knappe Maßnahme ihn künftig verhindert und das System dadurch
  nirgends langsamer wird. Normale Entwicklungsarbeit (roter Build, der gefixt wurde) ist kein Befund.
- **Kontext-Kosten sind die Grenze.** Was in jeder Sitzung mitgeladen wird (MEMORY.md, Hook-Ausgaben),
  kostet bei jeder Sitzung; was nur bei Bedarf gelesen wird (Skill-Anleitung, Almanach, Best Practice,
  Skript), kostet fast nichts. Daher: **einmalig** → nur Hebel 1 und 2 aus Schritt 4, knapp (Faustregel ≤ 3
  Zeilen je Stelle); **wiederkehrend** (≥ 2 Sitzungen, aktuelle mitgezählt; Vergleich lässt sie weg) →
  zusätzlich Hebel 3 erlaubt. Nie lange Abschnitte, nie neue Pflichtlektüre.
- **Sofort-Korrektur:** Fehlende oder falsche Angabe (Pfad, Datei- oder Parametername, „nicht gefunden“)
  in einer Bedarfsdatei → dort sofort berichtigen, nur die Tatsache, kein `massnahmen.json`-Eintrag,
  zählt nicht zum Limit aus Schritt 4. Commit-Nachricht `session-flaschenhals: <ort> berichtigt`.
- Vor jeder Maßnahme die Ursache belegen: Kommandos hinter dem Befund ansehen (`--json` liefert sie)
  und gegenprüfen, wie oft dasselbe Muster **ohne** Fehler lief. Den richtigen Wert belegen (Glob/Grep),
  nicht raten. Ohne klare Ursache: keine Maßnahme.

## 3. Bekannte Maßnahmen prüfen (`massnahmen.json`)

Der Vergleich zeigt je Maßnahme vorher/nachher (Sitzungen mit Treffer, Fehler, Aufrufe).
- ≥ 10 Sitzungen nachher, Fehler 0 → `status: wirkt`.
- Fehler kommen nachher weiter → `wirkungslos`: Ursache neu prüfen oder laut `ruecknahme` zurücknehmen.
- Muster läuft nachher wieder häufig **ohne** Fehler (Ursache upstream behoben) → `überholt`:
  Kontextzeile laut `ruecknahme` entfernen. So wächst die Pflichtlektüre nicht.

## 4. Umsetzen (höchstens 3 Maßnahmen je Lauf, je ≤ 3 Dateien; Sofort-Korrekturen zählen nicht mit)

Ursache nach Direktive 3 (`claude-code-setup/docs/rules/resilient-bugfixing.md`): 5×Warum, verwandte
Stellen, Funktionserhalt. Hebel in dieser Reihenfolge, der erste passende gewinnt:
1. **Ursache im Werkzeug/Skript beheben** (Skill, Hook, Werkzeuge/…) — kostet keinen Kontext.
2. **Vorhandene Stelle ergänzen**, die nur bei Bedarf gelesen wird: passender Skill, Almanach unter
   `bugs/`, Best Practice.
3. **Eine Zeile** im `MEMORY.md` des aktiven Profils (≤ 200 Zeichen, Detail in eigener Memory-Datei),
   nur bei wiederkehrenden Befunden und nur, wenn Hebel 1 und 2 nicht greifen.
Nie: neue Profilregeln, CLAUDE.md/AGENTS.md ändern, App-Code ändern, neue Pflicht-Lesedateien. Das wird
nur als Vorschlag berichtet.

Jede Maßnahme in `massnahmen.json` eintragen: `id`, `titel`, `datum` (Systemzeit per Befehl), `beleg`
(Zahlen), `ort`, `fehler_regex`/`aufruf_regex` (+ `werkzeug`, `min_zeilen`) für die Wirkungsprüfung,
`pruefung`, `ruecknahme`, `kosten`, `plattform` (nur wo die Ursache auftritt: `windows`/`mac`/`linux`;
weglassen = alle). Ohne prüfbare Regex keine Maßnahme. Jeder Rechner misst nur seine eigenen Verläufe.

## 5. Funktionserhalt und Abschluss

- Geänderte Skripte einmal ausführen (mindestens Syntaxprüfung), Diff lesen: nichts entfernt, nichts geschluckt.
- Nur eigene Dateien vormerken (parallele Sitzungen!). Hat eine Datei fremde Änderungen (oft `MEMORY.md`),
  nur den eigenen Teil vormerken (`git hash-object -w` + `git update-index --cacheinfo`) und dann
  **`git commit` ohne Pfadangabe** — mit Pfaden nimmt git die ganze Arbeitskopie der Datei mit.
  Vorher `git diff --cached --name-only` prüfen. Nachricht: `session-flaschenhals: <maßnahme>`.
  Kein Versions-Bump (Skills/Memory haben keine Version). `git pull --rebase`, dann push.
  Nur Versioniertes committen: Das Windows-Gedächtnis liegt im Repo, Mac- und Server-Gedächtnis nur lokal.

## 6. Bericht (höchstens 8 Zeilen)

Zeittöpfe dieser Sitzung · wiederkehrende Befunde · einmalige Befunde (kurz) · neue/geprüfte Maßnahmen
mit Ort · Eigenaufwand (Skriptlaufzeit, Gesamtdauer des Laufs) · Zeitgewinn: **„geschätzt, nicht
gemessen“**, bis `massnahmen.json` Nachher-Daten hat. Commit/Push ja/nein.
