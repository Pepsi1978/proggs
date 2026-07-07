# Design-Spec: Almanach-Trigger-Sonde + Auswertung

- **Datum:** 2026-06-16
- **Status:** Freigegeben (Brainstorming), bereit fuer Implementierungsplanung
- **Bereich:** Claude-Harness (Hook `bug-almanac-guard` + neuer Auswertungs-Skill)
- **Direktive:** #2 Selbstbeobachtung (das System beobachtet seine eigenen Unterbrechungen und lernt daraus)
- **Verwandte Regeln:** `observability-first.md`, `known-bugs-before-coding.md`, `resilient-bugfixing.md` (Funktionserhalt), `lossless-context-principle.md`

---

## 1. Problem

Das Bug-Almanach-System unterbricht Claude per `bug-almanac-guard` (PreToolUse), bis fuer den
betroffenen Bereich der Almanach (+ Best-Practices) gelesen wurde. Das ist gewollt — aber manche
Unterbrechungen sind **unnoetig**. Beispiel: ein reiner **Version-Bump** (`versionName`/`versionCode`
um eins erhoehen) loest den Gradle-Almanach aus, obwohl dafuer kein Bug-Wissen noetig ist.

Heute gibt es nur ein **dünnes** Block-Log (`~/.claude/state/bug-almanac-blocks.log`): pro Block eine
Zeile mit `Datum + slug` (z.B. `2026-06-16 13:40 gradle`), optional `(best-practices)` /
`(kein-almanach)` / `(stufe-c-volltext)`. Stand heute: 185 Eintraege. Was **fehlt**, ist der **Grund**:
welche Datei, welches Tool, **was genau geaendert wurde**. Ohne diese Information laesst sich nicht
erkennen, welche Trigger unnoetig waren.

## 2. Ziel

Eine **Sonde** zeichnet jede Almanach-/Best-Practices-Unterbrechung **und** jede Freigabe ausfuehrlich
und maschinen-/KI-lesbar auf. Ein **Auswertungs-Skill** liest diese Aufzeichnung, zeigt Muster und
markiert **verdaechtig unnoetige Auslöser** mit konkreten Ausschluss-Vorschlägen. Der eigentliche
Ausschluss (z.B. Version-Bumps nicht mehr blocken) erfolgt **spaeter, datengetrieben** — nicht in
diesem Schritt.

Dreistufig: **messen → auswerten → datengetrieben ausschliessen.**

## 3. Nicht-Ziele (YAGNI)

- Kein Live-Dashboard, keine Datenbank, kein Webserver.
- Keine automatische Selbst-Anpassung des Guards (die Ausschluss-Phase ist bewusst getrennt und kommt spaeter).
- Keine Aenderung an der bestehenden Block-Entscheidung des Guards — die Sonde ist **rein beobachtend**.
- Der bestehende `bug-almanac-blocks.log` wird **nicht** abgeschafft (Abwaertskompatibilitaet).

## 4. Architektur — drei Bausteine

### Baustein 1 — Die Sonde (im bestehenden Guard)

Der `bug-almanac-guard` (`.ps1` **und** `.sh`) bekommt an jeder Entscheidungsstelle einen
Sonden-Aufruf, der **eine JSON-Zeile** pro Ereignis anhaengt nach:

```
~/.claude/state/bug-almanac-triggers.jsonl
```

JSON-Lines (ein Objekt pro Zeile), wie `observability-first` es verlangt — durchsuchbar per
`grep`/`jq`/Python, ideal fuer KI-Auswertung. Der alte `blocks.log` bleibt unveraendert daneben.

**Felder pro Zeile:**

| Feld | Inhalt | Beispiel |
|------|--------|----------|
| `ts` | ISO-Zeitstempel | `2026-06-16T14:05:00` |
| `event` | `block` oder `pass` | `block` |
| `block_type` | Grund-Klasse (siehe unten) | `almanach-ungelesen` |
| `slug` | Bereich technisch | `gradle` |
| `area` | Bereich Klarname | `Build - Gradle (AGP/R8)` |
| `tool` | ausloesendes Tool | `Edit` / `Write` / `MultiEdit` |
| `file` | betroffene Datei | `.../app/build.gradle.kts` |
| `change_excerpt` | **kurzer Auszug der Aenderung (~300 Zeichen, Secrets maskiert)** | `versionName = "0.11.1"` |
| `high_risk` | Stufe-C-Bereich? | `true`/`false` |
| `session` | Session-ID (Gruppierung), falls vorhanden | `abc123` |

**`event`/`block_type`-Matrix** (an welcher Stelle im Guard welcher Eintrag entsteht):

| Stelle im Guard | `event` | `block_type` |
|-----------------|---------|--------------|
| Almanach existiert, aber read-Marker fehlt | `block` | `almanach-ungelesen` |
| Hochrisiko-Bereich, full-Marker fehlt (Stufe C) | `block` | `volltext-c` |
| Best-Practices-Datei noch nicht gelesen | `block` | `best-practices` |
| Kein Almanach fuer den Bereich, keine Quittung | `block` | `kein-almanach` |
| Almanach (+ BP) gelesen → freigegeben | `pass` | `already-read` |
| Notaus aktiv (`bug-almanac-disable.flag`) | `pass` | `disabled` |
| Quittung gesetzt (`bug-almanac-ack-<slug>.flag`) | `pass` | `ack` |

**Logging-Frequenz (gegen Aufblaehung):**
- `block`-Eintraege: bei **jedem** Block schreiben (jeder Block ist eine echte Unterbrechung).
- `pass`-Eintraege: **einmal pro Bereich + Session** schreiben — gekoppelt an den bestehenden
  `seenMarker` (der ohnehin die einmalige sanfte Bestaetigung steuert). So ergibt sich ein sauberes
  Verhaeltnis „geblockt vs. glatt durch" pro Bereich, ohne dass jeder Folge-Edit eine Zeile erzeugt.

**`change_excerpt`-Gewinnung:** aus dem bereits vorhandenen Tool-Input
(`tool_input.content` / `new_string` / `edits[].new_string`), auf ~300 Zeichen gekuerzt.

**Sicherheit (`observability-first` §8):** Vor dem Schreiben werden Secrets im Auszug maskiert
(Muster: `gho_…`, `ghp_…`, `sk-…`, `AIza…` sowie generische `token|key|secret|password = "…"`).
Die `.jsonl` liegt in `~/.claude/state/` — **ausserhalb** jedes Repos, also kein `.gitignore`-Bedarf.

**Robustheit (Direktive #3, FAIL-OPEN):** Die Sonde laeuft in eigenem `try/catch`. Jeder Fehler beim
Schreiben wird verschluckt (max. `Hook-LogWarn`) und beeinflusst **nie** die Block-Entscheidung des
Guards. Die Sonde darf den Guard niemals verlangsamen oder blockieren.

**Rotation:** Wenn die `.jsonl` eine grosszuegige Grenze ueberschreitet (Richtwert ~5 MB), wird sie
einmalig nach `…triggers.jsonl.1` umbenannt und frisch begonnen (genau eine Vorgaengerdatei). So
waechst sie nicht unbegrenzt, Langzeit-Muster bleiben aber erhalten.

### Baustein 2 — Der Auswertungs-Skill (neu)

Ein deutscher Skill (Arbeitstitel **`almanach-trigger-auswertung`**, finaler Name siehe Abschnitt 8),
erstellt **zwingend via `skill-creator`** (CLAUDE.md-Pflicht). Per Zuruf gestartet liest er die
`.jsonl` und liefert:

- **Verhaeltnis** `block` vs. `pass` (wie oft genervt vs. glatt durch) — gesamt und pro Bereich.
- **Haeufigste Auslöser** gruppiert nach `slug` + `block_type`.
- **Verdachtsliste unnoetiger Auslöser:** Bereiche/Faelle, deren `change_excerpt` nach trivialer
  Aenderung aussieht (z.B. reiner Version-Bump, reine String-/Doku-Aenderung) — mit konkretem
  Ausschluss-Vorschlag, z.B.: „Bereich `gradle` 6× geblockt, 5× nur Version-Bump → Vorschlag:
  Version-Bump-Erkennung in den Guard aufnehmen."

Der Skill **aendert nichts** — er liest, wertet aus und schlaegt vor. Entscheidung bleibt bei Frank.

### Baustein 3 — Spaeterer Ausschluss (NICHT in diesem Schritt)

Erst wenn echte Daten zeigen, welche Gruende verlaesslich unnoetig sind, wird die Ausschluss-Logik in
den Guard eingebaut (z.B. „nur `versionName`/`versionCode` geaendert → nicht blocken"). Bewusst
getrennt, damit nicht versehentlich echte Bug-relevante Aenderungen durchgewunken werden.

## 5. Datenfluss

```
Edit/Write/MultiEdit
        │
        ▼
  bug-almanac-guard  ──(Block-Entscheidung wie bisher, UNVERAENDERT)──► Claude
        │
        └──(zusaetzlich, rein beobachtend)──► Sonde schreibt 1 JSON-Zeile
                                                     │
                                                     ▼
                                  ~/.claude/state/bug-almanac-triggers.jsonl
                                                     │
                          (auf Zuruf, getrennt) ─────┘
                                                     ▼
                              almanach-trigger-auswertung (Skill) ──► Bericht + Ausschluss-Vorschlaege
```

## 6. Cross-Platform

Beide Guard-Varianten bekommen die identische Sonde:
- `~/.claude/hooks/bug-almanac-guard.ps1` (PowerShell)
- `~/.claude/hooks/bug-almanac-guard.sh` (Bash; JSON-Schreiben per Python, **kein `jq`** — siehe `claude-hooks.md`)

Beide schreiben in **dieselbe** `.jsonl` mit identischem Schema, sodass die Auswertung
plattformunabhaengig funktioniert. Der Skill ist Markdown → plattformunabhaengig.

## 7. Pflicht-Auflagen (aus den Regeln)

1. **Hochrisiko-Bereich `claudehooks` (Stufe C):** Bevor der Guard editiert wird, MUSS
   `bugs/claude-tooling/claude-hooks.md` im **Volltext** + die zugehoerige Best-Practices-Datei
   gelesen werden (`known-bugs-before-coding`). Der Guard erzwingt das ohnehin selbst.
2. **Neuer Skill nur via `skill-creator`** (CLAUDE.md).
3. **Funktionserhalt (Direktive #3):** Die Block-Logik des Guards bleibt Byte-fuer-Byte gleich in
   ihrem Verhalten; die Sonde ist additiv. Regressionscheck: alle bestehenden Block-/Pass-Faelle
   verhalten sich wie vorher (nur eine zusaetzliche Logzeile entsteht).
4. **Verlustfrei:** Findings werden ausgelagert (`.jsonl` + Skill), nicht in den Kontext geladen
   (`lossless-context-principle`).

## 8. Offene Detailfrage

- **Finaler Skill-Name.** Arbeitstitel `almanach-trigger-auswertung`. Alternativen denkbar:
  `almanach-watcher`, `almanach-statistik`. Wird vor dem `skill-creator`-Lauf mit Frank festgelegt.

## 9. Testplan (Kurz)

1. Provozierter Block (Edit an Bereichsdatei ohne gelesenen Almanach) → genau eine `block`-Zeile mit
   korrektem `block_type`, `file`, `change_excerpt`.
2. Nach Lesen des Almanachs ein weiterer Edit → genau eine `pass`-Zeile (`already-read`), keine zweite
   bei weiteren Edits derselben Session/Bereich.
3. Secret im `new_string` → im `change_excerpt` maskiert.
4. Fehler beim Schreiben (z.B. State-Ordner schreibgeschuetzt) → Guard blockt/laesst trotzdem normal
   durch (FAIL-OPEN), keine Stoerung.
5. `.ps1`- und `.sh`-Lauf erzeugen schema-gleiche Zeilen.
6. Auswertungs-Skill auf einer Beispiel-`.jsonl` → korrektes Verhaeltnis + plausible Verdachtsliste.
