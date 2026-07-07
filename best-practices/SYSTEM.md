# Best-Practices-System — Funktionsweise & Design

> Stand: 2026-06-16 (v1). Gegenstück zu [`bugs/SYSTEM.md`](../bugs/SYSTEM.md). Entstanden aus
> dem Umbau, der die Best-Practices strukturell **1:1 wie den Bug-Almanach** aufgestellt hat
> (flach, präfixlos, gleiche Kategorien; Commits #46839 ff.). Dieses Dokument beschreibt, wie
> die „richtige Seite der Medaille" arbeitet — es ist die Referenz für künftige Verbesserungen.

---

## 1. Zweck (in einem Satz)

**Bevor an einem technischen Bereich gearbeitet wird, liegen nicht nur dessen bekannte Bugs
(`bugs/`), sondern auch die bewährten Best-Practices bereits auf dem Tisch — *wie man es von
vornherein richtig macht, damit der Bug gar nicht erst entsteht*.** Das ist die proaktive
Prävention: der Bug-Almanach immunisiert gegen bekannte Fehler, die Best-Practices verhindern,
dass man sie überhaupt erzeugt.

---

## 2. Zwei Seiten einer Medaille

| | `bugs/<kat>/<bereich>.md` (Almanach) | `best-practices/<kat>/<bereich>.md` (diese Seite) |
|---|---|---|
| Frage | *Was geht schief, wie löse ich es?* | *Wie mache ich es von vornherein richtig?* |
| Quelle | öffentlich bekannte Bugs/Fallen + funktionserhaltende Fixes | offizielle Hersteller-Doku/Changelog + bewährte Patterns |
| Kopplung | Bezugs-Tabelle → Best-Practice | Bezugs-Tabelle → Almanach |
| Lese-Reihenfolge (Stufe A) | **zuerst** | **danach**, vor dem Coden |

Beide Dateien tragen eine wechselseitige **Bezugs-Tabelle**. `bugs/check-coupling.py` prüft,
dass jedes Paar wechselseitig verlinkt ist (sonst „Drift"). Eine Best-Practice **ohne**
Bug-Pendant ist erlaubt und wird als `[INFO]` (ungepaart) geführt — das gilt für rein
präventives Wissen ohne dokumentierten Failure-Mode (z. B. das generische Harness-Wissen
und `apis/multi-provider`).

---

## 3. Ordnerstruktur (seit 2026-06-16: flach, 1:1 wie `bugs/`)

```
best-practices/
├── README.md                  ← Inhaltsverzeichnis (nach Kategorie), Kategorie-Übersicht
├── SYSTEM.md                  ← dieses Dokument
├── _state.json                ← Stand des Harness-Wissens (last_version / last_checked)
├── _changelog-archiv.md       ← Master-Zeitleiste aller Best-Practices-Änderungen
├── android/ · android-build/ · desktop/ · web/ · apis/ · peripherie/ · assets/ · agents/
└── claude-tooling/            ← Harness (Bug-gepaart + generisches Harness-Wissen)
```

Dateiname-Schema: `<bereich>.md` (kein `best-practices-`-Präfix). Die Kategorien sind
deckungsgleich mit `bugs/<kategorie>/`. Hooks und `check-coupling.py` finden die Dateien
**rekursiv** über den Bereichs-Namen — ein Kategorie-Wechsel ist unkritisch.

### Doppelnatur von `claude-tooling/`
- **Bug-gepaarte Digests:** `claude-hooks.md`, `mcp-server.md`, `claude-config.md`,
  `cowork*.md`, `python-windows.md`, `claude-code-desktop-vs-cli.md`,
  `agent-knowledge-system.md` — Gegenstücke zu `bugs/claude-tooling/`.
- **Generisches Harness-Wissen** (früher `01-hooks`…`12-neues`): `hooks.md`, `skills.md`,
  `agents.md`, `plugins.md`, `mcp.md`, `commands.md`, `settings.md`, `kontext.md`,
  `token-effizienz.md`, `arbeitsweise.md`, `researcher.md`, `neues.md` — ausführliches
  „wie benutzt man die Claude-Code-Werkzeuge am besten". Spezifische Digests zeigen auf
  diese Volltexte (z. B. `claude-hooks.md` → `hooks.md`).

---

## 4. Digest-Modell (3 Lese-Stufen — wie bei `bugs/`)

Jede Best-Practice-Datei trägt oben einen **Kurzcheck** (Tabelle, in den ersten ~80 Zeilen).

| Stufe | Wann | Was lesen |
|-------|------|-----------|
| **A** | vor jeder Arbeit im Bereich | NUR den Kurzcheck (`Read` mit `limit=80`) — **nach** dem Almanach-Kurzcheck |
| **B** | ab dem ersten Fehler im Bereich | Volltext der zugehörigen Best-Practice + Almanach |
| **C** | Hochrisiko-Bereich (r8, firebase-billing, claude-hooks, claude-config) | Volltext schon vorab |

Der `bug-almanac-guard`-Hook erzwingt die Reihenfolge: bereichstypische Edits werden
blockiert, bis **zuerst** der Almanach- **und dann** der Best-Practice-Kurzcheck gelesen
wurde (1×/Bereich/Session).

---

## 5. Pflege & Wachstum

- **Quellen-Rangordnung:** Offiziell (Hersteller) = Grundwahrheit, extern = gelabelte
  Alternative. Jeder Eintrag: Quelle (URL) + Datum/Version + `offiziell`/`extern`.
- **Versions-Anker:** live ermittelte installierte Version; `check-version-anchor.py` prüft Drift.
- **Wer schreibt:** `bug-almanach-recherche`-Skill (Prävention pro Bug), `best-practices`-Skill
  (gezielter Lauf pro Software/Harness-Thema). Nie parallel schreibende Researcher — der
  Hauptagent/ein Worker arbeitet die Kandidaten konsolidiert ein (siehe `research-persistence`-Regel).
- **Selbst-wachsend:** jeder neue Bereich fügt eine Best-Practice hinzu, jede Recherche
  verdichtet eine bestehende. Die Persistenz ist Teil der „fertig"-Definition jeder Aufgabe
  mit Web-Recherche.

---

## 6. Health-Checks

- `python bugs/check-coupling.py` — wechselseitige Almanach↔Best-Practice-Verlinkung (Drift-Check).
- `python bugs/check-version-anchor.py` — Versions-Anker-Drift.
- `python bugs/health.py` — Gesamt-Selbsttest (ruft die obigen + Stand-Verfall auf).

---

## 7. Was NIEMALS passieren darf

- ❌ Eine Best-Practice-Datei mit `best-practices-`-Präfix oder unter `projekt-code/` neu anlegen (alte Struktur).
- ❌ Eine Bezugs-Tabelle einseitig setzen (erzeugt Drift im check-coupling).
- ❌ Best-Practices und Almanach in unterschiedliche Kategorien legen (Symmetrie bricht).
- ❌ Findings nur im Chat lassen statt sie hier zu persistieren (siehe `research-persistence`-Regel).
- ❌ Quelle, Versions-Anker oder Stand-Datum weglassen (spätere Nachprüfbarkeit).

---

## 8. Änderungs-Historie des Best-Practices-Systems

> Die System-Struktur-Historie steht HIER (persistent). Nicht in `_changelog-archiv.md` —
> das ist das **wortwörtliche Claude-Code-Changelog** (Recherche-Quelle), das der
> `best-practices`-Skill bei jedem Lauf neu von GitHub holt und dabei überschreibt.

| Datum | Änderung |
|-------|----------|
| **2026-06-16** | **Großer Strukturumbau:** `best-practices/` auf **flach 1:1 wie `bugs/`** umgestellt (Commits #46833–#46852). (1) `projekt-code/<kat>/best-practices-<x>.md` → `best-practices/<kat>/<x>.md` (62 Dateien, präfixlos). (2) Generisches Harness-Wissen `01-hooks`…`12-neues` → `claude-tooling/<thema>.md` (12 Dateien). (3) Neue `README.md` (Kategorie-Index) + diese `SYSTEM.md`; alte Ordner `projekt-code/` + `01-12` entfernt. (4) Alle Pfad-Verweise in Almanachen, Rules (+ Repo-Spiegel) korrigiert. (5) Neuer `bugs/check-dead-paths.py` Validator (Backtick-Pfade + Links) in `health.py` gebündelt. Werkzeuge (`bug-almanac-guard`, `check-coupling.py`) wurden in Phase 1 abwärtskompatibel gemacht und erkennen beide Strukturen — die Straffung (Alt-Pfad-Code raus) folgt nach dem Skill-Umbau. |
| vor 2026-06-16 | Struktur: `projekt-code/<kat>/best-practices-<software>.md` (Projekt-Code, seit 2026-06-03 nach Kategorie gruppiert) + nummerierte Harness-Ordner `01-hooks`…`12-neues`. |
