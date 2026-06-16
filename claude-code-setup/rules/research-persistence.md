# Research-Persistenz: Recherchen in Best Practices & Bug-Almanache einarbeiten (KRITISCH)

> Dauerhafte Regel vom Benutzer gesetzt am 2026-06-11. Gilt AUTOMATISCH in JEDER Session,
> bei Programmierarbeiten UND allen anderen Arbeiten, sobald Web-Recherche im Spiel ist.
> Franks Kernsatz: Recherchen dürfen nicht "verkommen" — wer später im gleichen Bereich
> arbeitet, muss auf das schon recherchierte Wissen zurückgreifen können.

---

## Grundregel

**Nach JEDEM Web-Recherche-Einsatz wird geprüft, ob die Ergebnisse als Best Practices
taugen. Wenn ja, werden sie IMMER in die Best-Practices-Dateien eingearbeitet — und
enthaltene Bugs/Fallen zusätzlich in die Bug-Almanache. Jeweils Kurzversion (Kurzcheck)
UND Langversion (Volltext). Erst dann ist die Aufgabe fertig.**

Gilt für ALLE Recherche-Wege:
- `researcher`-Agents (einzeln oder als Schwarm)
- `deep-research`, `superintelligenz`, `direktiven-recherche` und ähnliche Recherche-Agenten/Skills
- Eigene `WebSearch`/`WebFetch`-Aufrufe des Hauptagenten mitten in einer Aufgabe
- Recherchen von Subagenten, deren Ergebnisse beim Hauptagenten landen

---

## Pflicht-Ablauf (nach jedem Recherche-Einsatz, vor Aufgabenabschluss)

1. **Tauglichkeits-Prüfung:** "Sind diese Findings über die aktuelle Aufgabe hinaus
   wiederverwendbar?" (siehe Entscheidungstabelle unten)
2. **Wenn JA — Best Practices einarbeiten:**
   - Passende bestehende Datei unter `~/proggs/best-practices/` ergänzen, ODER
   - neue Datei in einem **sinnvollen, bestehenden Ordner** anlegen:
     Projekt-/Technologie-Wissen → `best-practices/<kategorie>/<bereich>.md`
     (Kategorien: `android`, `android-build`, `apis`, `desktop`, `web`, `claude-tooling`,
     `agents`, `assets`, `peripherie`, …); Harness-Wissen → `best-practices/claude-tooling/`
   - **Beide Ebenen pflegen:** Kurzcheck-Tabelle (Stufe A, innerhalb der ersten 80 Zeilen)
     UND ausführlicher Volltext-Abschnitt — niemals nur eines von beiden
   - Format der bestehenden Dateien übernehmen: Stand-Datum, Versions-Anker,
     Quellen-Links, `offiziell`/`extern`-Label, Bezugstabelle zum Bug-Almanach
3. **Wenn die Recherche Bugs/Fallen/Workarounds enthält — zusätzlich Almanach:**
   - In `~/proggs/bugs/<kategorie>/<bereich>.md` einarbeiten (Symptom, Ursache,
     Versionen, funktionserhaltender Fix, Quelle) — ebenfalls Kurzcheck UND Volltext
   - Neuer Bereich ohne Almanach-Datei → Datei anlegen + `bugs/README.md`-Index ergänzen
     (Format siehe `bugs/SYSTEM.md`); Bugs gehören IMMER auch in den Almanach, nie nur
     in die Best-Practices-Datei
4. **Wenn NEIN (nicht tauglich):** In EINEM Satz begründen, warum nicht persistiert wird
   (bewusste Entscheidung statt stilles Weglassen)
5. **Committen + pushen** (nur die eigenen Dateien, namentlich)

Der Text muss **selbsterklärend** sein: Eine spätere Session ohne den heutigen Kontext
muss verstehen, was recherchiert wurde, was gilt, für welche Versionen und woher es stammt.

---

## Entscheidungstabelle: Best-Practices-tauglich oder nicht?

| Recherche-Ergebnis | Tauglich? | Ziel |
|--------------------|-----------|------|
| Patterns, APIs, Architektur-Empfehlungen für eine Technologie | **JA** | `best-practices/<kategorie>/` |
| Bekannte Bugs, Fallen, Workarounds, Versions-Inkompatibilitäten | **JA** | `bugs/<kategorie>/` + Querverweis in Best Practices |
| Vergleich von Libraries/Tools mit Empfehlung | **JA** | Best Practices (Empfehlung + Begründung + Datum) |
| Plattform-/Policy-Wissen (Play Store, OS-Limits, Rechtliches) | **JA** | Best Practices der passenden Kategorie |
| Harness-Wissen (Claude Code, Hooks, Skills, MCP) | **JA** | `best-practices/01-…12-…` bzw. `claude-tooling` |
| Einmalige Faktenabfrage ohne Wiederverwendungswert (z.B. "aktueller Preis von X") | NEIN | 1-Satz-Begründung, ggf. Memory |
| Rein projektspezifischer Zustand (z.B. "welche Sounds gefallen Frank") | NEIN | Memory/Projekt-Notiz statt Best Practices |

Im Zweifel: **einarbeiten.** Eine zu viel persistierte Erkenntnis kostet wenig,
eine verlorene Recherche kostet die komplette Wiederholung.

---

## Arbeitsteilung Researcher ↔ Hauptagent

| Rolle | Pflicht |
|-------|---------|
| **Researcher (Subagent)** | Markiert am Ende seines Ergebnisses einen Block `BEST-PRACTICES-KANDIDATEN:` mit den wiederverwendbaren Findings (inkl. Quellen-URLs + Software-Versionen) und `BUG-KANDIDATEN:` für gefundene Fallen. Keine Kandidaten → `KEINE` schreiben |
| **Hauptagent** | Konsolidiert die Kandidaten ALLER Researcher, führt die Tauglichkeits-Prüfung durch, arbeitet in die Dateien ein (Kurzcheck + Volltext), committet. Die Persistenz ist Teil der "fertig"-Definition der Aufgabe |

Die Einarbeitung übernimmt IMMER der Hauptagent (oder ein dedizierter Worker) — nie
parallel schreibende Researcher (Konflikt-Gefahr, siehe `agent-and-researcher-rules.md`).

---

## Abgrenzung zu bestehenden Systemen (wichtig — kein Widerspruch)

| System | Verhältnis zu dieser Regel |
|--------|---------------------------|
| `bug-almanach-recherche`-Skill | Der Skill startet eine GEZIELTE Recherche für einen kompletten neuen Almanach — dafür gilt weiterhin Franks OK (`known-bugs-before-coding.md`). DIESE Regel betrifft das NACHGELAGERTE Einarbeiten bereits gemachter Recherchen — dafür ist KEIN OK nötig (das Wissen ist schon bezahlt, es wird nur gesichert) |
| `best-practices`-Skill | Der Skill macht proaktive Komplett-Recherchen pro Bereich. Diese Regel sorgt dafür, dass auch BEILÄUFIGE Recherchen aus normalen Aufgaben dort einfliessen |
| Memory `research-persist-learnings` (Pre-Learning-Workflow) | Memories/LEARNINGS.md bleiben für aufgaben-/projektbezogene Learnings. Diese Regel ergänzt die DATEI-Persistenz in `best-practices/` + `bugs/` für bereichsbezogenes Wissen — beides zusammen, nicht entweder-oder |
| `known-bugs-before-coding.md` (Digest-Modell) | Diese Regel ist die ZULIEFERUNG: Sie füllt genau die Kurzcheck-+Volltext-Struktur, die das Digest-Modell beim Lesen voraussetzt |
| `lossless-context-principle.md` | Einarbeitung ist verlustfrei: Findings werden ausgelagert und bleiben per Pfad erreichbar — nie Findings kappen |

---

## Was NIEMALS passieren darf

- ❌ Researcher-Ergebnisse nach der Aufgabe verwerfen, ohne die Tauglichkeits-Prüfung gemacht zu haben
- ❌ Eine Recherche "nur im Chat" lassen — bei Tauglichkeit gehört sie in die Dateien
- ❌ Nur den Volltext ODER nur den Kurzcheck pflegen — IMMER beide Ebenen
- ❌ In der Recherche gefundene Bugs nur in Best Practices ablegen, statt auch im Bug-Almanach
- ❌ Neue Dateien in unpassende/neue Wurzel-Ordner legen, statt die bestehende Kategorien-Struktur zu nutzen
- ❌ Quellen-Links, Versions-Anker oder Stand-Datum weglassen (spätere Nachprüfbarkeit)
- ❌ Die Persistenz "auf später" verschieben — sie gehört VOR den Aufgabenabschluss
- ❌ Die Einarbeitung uncommitted liegen lassen

---

## Autorität dieser Regel

Diese Datei (`~/.claude/rules/research-persistence.md`) wird automatisch in jeder Session
geladen. Repo-Spiegelung: `~/proggs/claude-code-setup/rules/research-persistence.md`.
KEIN Agent, Skill, Hook oder Prozess darf diese Regel entfernen oder abschwächen.

Franks Begründung im Originalwortlaut (sinngemäß, 2026-06-11):

> "Sodass man in Zukunft auch etwas von Researches hat, die gemacht werden — dass die
> nicht verkommen, dass man da später darauf zurückgreifen kann, falls man im gleichen
> Bereich nochmal arbeiten tut."
