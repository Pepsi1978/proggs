---
name: almanach-trigger-auswertung
description: "Wertet die Almanach-Trigger-Sonde (~/.claude/state/bug-almanac-triggers.jsonl) aus und schlaegt unnoetige Almanach-/Best-Practices-Unterbrechungen zum Ausschluss vor (Direktive #2 Selbstbeobachtung). Nutze diesen Skill IMMER wenn der Benutzer sagt: 'werte die Almanach-Trigger aus', 'Almanach-Auswertung', 'Almanach-Trigger-Statistik', 'analysiere die Almanach-Sonde', 'welche Almanach-Unterbrechungen waren unnoetig', 'welche Almanach-Blocks kann ich ausschliessen', 'wie oft hat der Almanach geblockt', 'Almanach-Watcher auswerten'. Liest und schlaegt vor — aendert NIE den Guard selbst."
---

# Almanach-Trigger-Auswertung

## Wozu dieser Skill

Der `bug-almanac-guard` (PreToolUse-Hook) unterbricht die Arbeit, bis fuer den betroffenen
Bereich der Bug-Almanach (+ Best-Practices) gelesen wurde. Eine **Sonde** im Guard zeichnet
jede Unterbrechung (`block`) UND jede Freigabe (`pass`) als JSON-Zeile auf nach:

```
~/.claude/state/bug-almanac-triggers.jsonl
```

Manche dieser Unterbrechungen sind **unnoetig** — das klassische Beispiel ist ein reiner
**Version-Bump** (`versionName`/`versionCode` um eins erhoehen), der den Gradle-Almanach
ausloest, obwohl dafuer kein Bug-Wissen noetig ist. Dieser Skill liest die Aufzeichnung,
zeigt die Muster und benennt **verdaechtig unnoetige Auslöser** mit konkretem
Ausschluss-Vorschlag. So entsteht die Datengrundlage fuer den dritten Schritt
(messen → auswerten → **datengetrieben ausschliessen**).

**Wichtig:** Dieser Skill liest und schlaegt vor. Er aendert NIE den Guard selbst — der
Ausschluss unnoetiger Auslöser ist eine bewusste, separate Entscheidung von Frank.

## Datenformat (eine JSON-Zeile pro Ereignis)

| Feld | Bedeutung |
|------|-----------|
| `ts` | Zeitstempel |
| `event` | `block` (Unterbrechung) oder `pass` (Freigabe) |
| `block_type` | `almanach-ungelesen` · `volltext-c` · `best-practices` · `kein-almanach` · `already-read` · `disabled` · `ack` |
| `slug` / `area` | Bereich technisch + Klarname |
| `tool` | Edit / Write / MultiEdit |
| `file` | betroffene Datei |
| `change_excerpt` | kurzer Auszug der Aenderung (Secrets maskiert) — der Schluessel zum Erkennen trivialer Auslöser |
| `high_risk` | Stufe-C-Bereich? |
| `session` | Session-ID |

## Ablauf

### Schritt 1 — Aggregieren (verlustfrei, ohne die ganze Datei in den Kontext zu laden)

Das mitgelieferte Script liest die `.jsonl` (inkl. rotierter `.jsonl.1`) und gibt eine
kompakte Auswertung aus. Die Rohdatei wird NICHT in den Kontext geladen (Lossless-Prinzip —
die Details bleiben per Pfad erreichbar):

```bash
python3 ~/.claude/skills/almanach-trigger-auswertung/scripts/aggregate.py
```

Gibt es keine Daten, meldet das Script das und der Skill endet mit einem Hinweis, dass die
Sonde noch nichts aufgezeichnet hat.

### Schritt 2 — Verdachtsanalyse (KI-Schritt)

Aus der Aggregation die `change_excerpt`-Auszuege der `block`-Ereignisse pro Bereich
durchgehen und einschaetzen, ob die Aenderung **trivial** war (kein Bug-Wissen noetig).
Heuristik fuer „Verdacht unnoetig":

- nur `versionName` / `versionCode` / `version =` / `"version":` geaendert → **Version-Bump**
- nur eine String-Ressource (`<string …>`, `getString`, reine Text-Konstante) → **reine Lokalisierung**
- nur Kommentar / Whitespace / Doku-Zeile
- nur ein Import / eine Formatierung ohne Logikaenderung

Ein Block ist KEIN Verdacht, wenn der Auszug echte Logik zeigt (Funktionsruempfe,
Bedingungen, neue API-Aufrufe, Annotationen) — dort ist die Almanach-Pflicht berechtigt.

### Schritt 3 — Bericht (festes Format)

```
# Almanach-Trigger-Auswertung

## Verhaeltnis
- Unterbrechungen (block): N   |   Freigaben (pass): M
- Top-Bereiche nach Blocks: <slug> (n), <slug> (n), …

## Verdacht: unnoetige Auslöser
- <slug> (<area>): <n> Blocks, davon <m> Verdacht <Grund>
  Beispiele: "<change_excerpt>", "<change_excerpt>"
  → Vorschlag: <Grund> im Guard von der Pflicht ausnehmen.

## Berechtigte Unterbrechungen (kein Handlungsbedarf)
- <slug>: <n> Blocks mit echter Logik-Aenderung.

## Naechster Schritt
Der Ausschluss ist eine separate Aenderung am Guard (bug-almanac-guard.ps1/.sh).
Sag Bescheid, welche Vorschlaege umgesetzt werden sollen.
```

### Schritt 4 — Auswertung vermerken (PFLICHT am Ende)

Nach dem Bericht den Zeitpunkt + die aktuelle Block-Zahl festhalten, damit der Session-Start-Reminder
(im `bug-almanac-index`-Hook) kuenftig „neue seit letzter Auswertung" statt „seit Beginn" zeigt:

```bash
python3 - <<'PY'
import datetime
from pathlib import Path
state = Path.home() / ".claude" / "state"
total = 0
for fn in ("bug-almanac-triggers.jsonl.1", "bug-almanac-triggers.jsonl"):
    p = state / fn
    if p.exists():
        with open(p, encoding="utf-8") as f:
            total += sum(1 for line in f if '"event":"block"' in line)
state.mkdir(parents=True, exist_ok=True)
# Zeile 1 = Datum (yyyy-mm-dd), Zeile 2 = Block-Zahl zum Auswertungszeitpunkt.
with open(state / "almanach-last-review.txt", "w", encoding="utf-8", newline="\n") as f:
    f.write(datetime.date.today().isoformat() + "\n" + str(total) + "\n")
print(f"Auswertung vermerkt: {datetime.date.today().isoformat()}, {total} Blocks gesamt")
PY
```

## Grenzen (bewusst)

- Der Skill **aendert nie** den Guard — er liefert nur die Entscheidungsgrundlage.
- Er bewertet `change_excerpt` (max ~300 Zeichen), nicht die komplette Datei — das reicht
  fuer die Trivial-Erkennung, kann aber im Grenzfall einen Block falsch einschaetzen. Im
  Zweifel als „berechtigt" einstufen (lieber eine Unterbrechung zu viel als ein verpasster Bug).
