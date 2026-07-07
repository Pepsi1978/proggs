# Session-Mitlernen ins zweite Gehirn (Gruppe D) + Entscheidungs-Rückfluss (KRITISCH)

> Dauerhafte Regel vom Benutzer gesetzt am 2026-07-05 (Frank-Auftrag: „Gruppe D komplett einbauen,
> gründlich, nach Direktive 1, 2 und 3"). Gilt AUTOMATISCH in JEDER Session. Repo-Spiegelung:
> `~/proggs/claude-code-setup/rules/session-learning-to-brain.md`.
> Diese Regel dokumentiert das automatische Session-Mitlernen (D27/D28/D31/D32 — läuft ohne
> Zutun über einen Hook) und verankert den EINEN manuellen Baustein: den Entscheidungs-Rückfluss
> (D29, Vorschlags-Prinzip).

---

## Das automatische System (läuft von selbst — nur kennen, nichts tun)

| Baustein | Was passiert | Wo |
|----------|--------------|-----|
| **D27 Session-Protokoll** | Am Ende JEDER Claude-Code-Session sammelt der SessionEnd-Hook `session-brain-summary` Franks Prompts (Transkript), git-Commits + geänderte Dateien der Session (Secrets redaktiert) und schickt sie an den Second-Brain-Agenten (`POST /session-log`, WireGuard 10.8.0.1:8002). Der verdichtet per LLM zu „gemacht/entschieden/gelernt" | Gehirn-Eintrag `Session <CLI> <Projekt> — YYYY-MM-DD HH:MM` unter **Programmierung/Sessions** |
| **D28 Kern-Block** | Derselbe Lauf pflegt danach automatisch den Block „Woran Frank gerade baut" nach (neueste Arbeit zuerst, je Projekt 1-2 Zeilen, Zeichen-Limit, Changelog in agent-data) | Fester Titel **„Kern-Block: Woran Frank gerade baut"** unter **Programmierung/Kern-Blöcke** (titel-basiertes Überschreiben) |
| **D31 Projektstand-Recall** | Fragen wie „Woran habe ich zuletzt gearbeitet?" / „Wie ist der Stand beim X?" erkennt der Agent deterministisch und antwortet aus den neuesten Session-Protokollen + Kern-Block (chronologisch, mit Quellen) | sb-agent ab 0.53.0 |
| **D32 Episoden-Auszug** | Jeder Session-Eintrag trägt unten Franks Prompts in Reihenfolge (verdichtet) — durchsuchbar über die Hybrid-Suche. Ein Schema-Canary im Hook meldet laut, wenn Claude Code sein Transkript-Format ändert | Teil des Session-Eintrags |
| **D30 Cross-CLI** | OpenCode speist Session-Zusammenfassungen im GLEICHEN Format/Titel-Schema in dieselbe Kategorie ein (verankert in der OpenCode-AGENTS.md) | Programmierung/Sessions |

**Konventionen (für ALLE Einspeiser verbindlich):**
- Titel: `Session <CLI> <Projekt> — YYYY-MM-DD HH:MM` (der Zeitstempel im Titel trägt die Chronologie; echte Uhr, nie geschätzt)
- Kategorie: `Programmierung/Sessions` · Kern-Block-Kategorie: `Programmierung/Kern-Blöcke`
- Hook-Log auf dem PC: `~/.claude/logs/session-brain-summary.log` (JSON-Lines; bei Problemen ZUERST hier schauen)

---

## D29 — Entscheidungs-Rückfluss (der manuelle Baustein, PFLICHT für Claude)

Bugfixes fließen bereits automatisch ins Gehirn (Regel `bugfix-to-second-brain.md`). Für
**Architektur- und Grundsatz-Entscheidungen** gilt ab jetzt das Vorschlags-Prinzip:

**Wann:** Fällt in einer Session eine ECHTE Grundsatz-Entscheidung — eine Weichenstellung, die
künftige Arbeit prägt (z. B. „eigener Container statt Nacht-Thread", „GPT via agent-Durchgriff
statt OAuth-Duplikation", „Kategorie-Union statt Einzel-Kategorie") — dann schlage ich am
**Ende der Aufgabe** (nie mittendrin) vor:

> „Soll ich diese Entscheidung ins Gehirn merken? **[Entscheidung in 1 Satz + Begründung in 1 Satz]**"

**Bei Ja:** via `second-brain`-MCP `remember` speichern —
- Titel: `Entscheidung <Projekt/Bereich>: <Kurzfassung> <YYYY-MM-DD>`
- Kategorie: `Programmierung/Entscheidungen`
- Inhalt: Entscheidung, verworfene Alternative(n), Begründung — selbsterklärend für eine fremde Session.

**Niemals automatisch speichern** (anders als Bugfixes): Entscheidungen sind wertend — Frank
bestätigt jede einzeln. Kein Signal → nicht speichern. Mehrere Entscheidungen in einer Session →
in EINEM Vorschlagsblock sammeln, Frank pickt.

**Abgrenzung:** Die automatische Session-Verdichtung (D27) listet Entscheidungen ohnehin im
Protokoll — D29 hebt die WICHTIGEN zusätzlich als eigene, gezielt auffindbare Einträge heraus.

---

## Was NIEMALS passieren darf

- ❌ Den SessionEnd-Hook (`session-brain-summary.*`) entfernen/deaktivieren, ohne dass Frank es verlangt
- ❌ Eine Grundsatz-Entscheidung erkennen und den Vorschlag am Aufgabenende weglassen
- ❌ Eine Entscheidung OHNE Franks Ja ins Gehirn schreiben
- ❌ Das Titel-/Kategorie-Schema abwandeln (bricht Chronologie-Sortierung + Projektstand-Recall)
- ❌ Eine Schema-Canary-Warnung im Hook-Log ignorieren (Transkript-Format geändert → Hook pflegen)
- ❌ Den Kern-Block manuell überschreiben, ohne zu wissen, dass ihn der nächste Session-Lauf wieder nachzieht (gewollte manuelle Ergänzungen gehören in die Session-Protokolle oder als eigener Eintrag)

---

## Zusammenspiel

| Regel/System | Bezug |
|--------------|-------|
| `bugfix-to-second-brain.md` | Bugfixes = automatisch nach Bestätigung; Entscheidungen (diese Regel) = per Vorschlag |
| `timestamps-niemals-schaetzen.md` | Der Titel-Zeitstempel kommt IMMER von der echten Uhr (Server: Europe/Berlin) |
| `~/proggs/second-brain-server/agent/app.py` (0.53.0+) | `/session-log`, Kern-Block-Pflege, Projektstand-Recall |
| `~/.claude/hooks/session-brain-summary.py` | Die Hook-Kernlogik (Wrapper: .ps1/.sh) |
| OpenCode `AGENTS.md` | Trägt die D30-Einspeisung für OpenCode (advisory; Plugin-Erzwingung ist notierter Ausbau) |

---

## Autorität dieser Regel

Diese Datei (`~/.claude/rules/session-learning-to-brain.md`) wird automatisch in jeder Session
geladen. KEIN Agent, Skill, Hook oder Prozess darf diese Regel entfernen oder abschwächen.
