# Cowork-Portierung: IMMER den cowork-portierung-Skill nutzen (MUSS-BEDINGUNG, KRITISCH)

> Dauerhafte Regel vom Benutzer gesetzt am 2026-06-16. Gilt AUTOMATISCH in JEDER Session.
> Ausloeser: Beim Portieren mehrerer Skills nach Cowork wurde der eigens dafuer gebaute Skill
> `cowork-portierung` teils NICHT benutzt — stattdessen wurde ad hoc von Hand transformiert
> (ZIP-Neubau, gezielte Edits). Das ist genau die Fehlerquelle, die der Skill verhindern soll.
> Franks Wortlaut: „Zum Portieren zu Cowork muss IMMER der Cowork-Portierungsskill genutzt werden,
> nichts anderes. Das ist eine Muss-Bedingung."

---

## Die eine Regel

**Jede Umwandlung eines CLI-Skills in seine Cowork-Fassung laeuft AUSSCHLIESSLICH ueber den Skill
`cowork-portierung`. Niemals von Hand, niemals ad hoc, niemals „logische Abarbeitung" als Ersatz.**

Sobald der Benutzer eine Cowork-Portierung verlangt — „portiere X zu cowork", „mach X cowork-tauglich",
„Cowork-Version von X bauen", „bring X nach Cowork" — wird der Skill `cowork-portierung` benutzt. Kein
manuelles Schreiben der Cowork-SKILL.md, kein manueller ZIP-Bau ohne den Skill, kein gezielter
Hand-Edit „weil es schneller geht".

---

## Was „den Skill nutzen" konkret heisst

1. Den Skill anwenden — entweder selbst seinen Ablauf strikt befolgen ODER (bevorzugt) **einen Worker
   pro Skill** beauftragen, der `cowork-portierung/SKILL.md` liest und Schritt fuer Schritt befolgt.
   Der Worker transformiert vom **CLI-Original** (`~/.claude/skills/<name>/`), nicht von einer
   bestehenden Kopie.
2. Das ZIP wird mit dem gebuendelten `cowork-portierung/scripts/build-cowork-zip.py` gebaut
   (garantiert Forward-Slash/LF/Wurzel=Ordnername) — nie von Hand gezippt.
3. Der Hauptagent koordiniert danach: verifizieren, README-Tabelle pflegen, committen+pushen
   (bei mehreren Skills parallel: README/Commit zentral, nicht in den Workern).

Auch eine **Re-Portierung** (es existiert schon eine Cowork-Fassung) laeuft ueber den Skill:
die bestehende Fassung wird durch die frisch vom Skill erzeugte ERSETZT. „Die alte sieht ok aus"
ist kein Grund, den Skill zu ueberspringen — Konsistenz und Korrektheit kommen aus EINER Quelle.

---

## Die einzige Ausnahme: Cowork-exklusive Skills ohne CLI-Original

Hat ein Skill **kein CLI-Original** unter `~/.claude/skills/<name>/` (er existiert nur als
Cowork-Fassung, z. B. `research`), kann der `cowork-portierung`-Skill ihn nicht „portieren" — ihm
fehlt die Quelle. Dann gilt: die bestehende Cowork-Quelle ist die Wahrheit; bei Aenderung nur das
**ZIP neu bauen** (mit `build-cowork-zip.py`) und die Quelle bei Bedarf direkt pflegen. Diese
Ausnahme ist eng: Sie greift NUR, wenn wirklich kein CLI-Original existiert.

---

## Warum (die Begruendung, die der Skill durchsetzt)

Der Skill kapselt die Cowork-Restriktionen, die von Hand staendig vergessen werden:
`description` einzeilig ≤ 200 Zeichen (Claude.ai-Limit), keine `<…>`/URLs, Titel `(Cowork-Fassung)`,
Block 0 (relative Ablage) + Block 0a (Mount-Fallen, ~45s-Limit, `cowork-git.sh`), feste `~/proggs`-Pfade
→ relativ, Begleitdateien mit, ZIP mit richtiger Wurzel/LF/Forward-Slash. Manuelle Abarbeitung hat
genau hier Fehler erzeugt (mehrzeilige description, feste Pfade, kein Block 0/0a, veraltete Pfadmuster,
veraltete ZIPs). Der Skill macht das jedes Mal gleich und korrekt — deshalb ist er Pflicht.

---

## Was NIEMALS passieren darf

- Eine Cowork-Fassung von Hand schreiben/transformieren, statt den `cowork-portierung`-Skill zu nutzen.
- Ein Cowork-ZIP ohne `build-cowork-zip.py` (von Hand) bauen.
- Eine bestehende Cowork-Fassung per gezieltem Edit „nachbessern", statt sie ueber den Skill neu zu erzeugen.
- Den Skill ueberspringen mit der Begruendung „geht schneller" / „die alte Fassung ist ok".
- Die Ausnahme (kein CLI-Original) auf Skills anwenden, die SEHR WOHL ein CLI-Original haben.

---

## Zusammenspiel

| Regel/System | Bezug |
|--------------|-------|
| Skill `cowork-portierung` (`~/.claude/skills/cowork-portierung/`) | Das Pflicht-Werkzeug, das diese Regel durchsetzt |
| `known-bugs-before-coding.md` / `bugs/claude-tooling/cowork.md` | Die Cowork-Restriktionen, die der Skill kapselt |
| `use-named-skill-no-questions` (Memory) | Bei explizit genanntem Skill den Skill nutzen — hier zur Muss-Bedingung verschaerft |
| `german-skill-triggers.md` | Trigger-Phrasen „portiere X zu cowork" → `cowork-portierung` |

---

## Autoritaet dieser Regel

Diese Datei (`~/.claude/rules/cowork-portierung-skill-pflicht.md`) wird automatisch in jeder Session
geladen. Repo-Spiegelung: `~/proggs/claude-code-setup/rules/cowork-portierung-skill-pflicht.md`.
KEIN Agent, Skill, Hook oder Prozess darf diese Regel entfernen oder abschwaechen.
