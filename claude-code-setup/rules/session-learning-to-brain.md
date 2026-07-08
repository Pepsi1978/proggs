# Session-Mitlernen ins zweite Gehirn (Gruppe D) + Entscheidungs-Rueckfluss (KRITISCH)

## Das automatische System (laeuft von selbst — nur kennen)

| Baustein | Was passiert | Wo |
|----------|--------------|-----|
| **D27 Session-Protokoll** | SessionEnd-Hook `session-brain-summary` sammelt Franks Prompts + git-Commits + geaenderte Dateien (Secrets redaktiert), schickt sie an den Second-Brain-Agenten (`POST /session-log`, WireGuard 10.8.0.1:8002), der zu "gemacht/entschieden/gelernt" verdichtet | Eintrag `Session <CLI> <Projekt> — YYYY-MM-DD HH:MM` unter **Programmierung/Sessions** |
| **D28 Kern-Block** | pflegt automatisch "Woran Frank gerade baut" nach (neueste Arbeit zuerst) | Titel **"Kern-Block: Woran Frank gerade baut"** unter **Programmierung/Kern-Bloecke** |
| **D31 Projektstand-Recall** | "Woran habe ich zuletzt gearbeitet?"/"Stand beim X?" → Antwort aus neuesten Session-Protokollen + Kern-Block | sb-agent ab 0.53.0 |
| **D32 Episoden-Auszug** | jeder Session-Eintrag traegt Franks Prompts (verdichtet), durchsuchbar | Teil des Eintrags |
| **D30 Cross-CLI** | OpenCode speist im gleichen Format/Titel-Schema ein | Programmierung/Sessions |

Titel-Schema: `Session <CLI> <Projekt> — YYYY-MM-DD HH:MM` (echte Uhr). Hook-Log:
`~/.claude/logs/session-brain-summary.log`.

## D29 — Entscheidungs-Rueckfluss (manueller Baustein, PFLICHT fuer Claude)

Faellt eine echte Grundsatz-Entscheidung (Weichenstellung, die kuenftige Arbeit praegt — z.B. "eigener
Container statt Nacht-Thread"), schlage ich am ENDE der Aufgabe (nie mittendrin) vor:
> "Soll ich diese Entscheidung ins Gehirn merken? [Entscheidung 1 Satz + Begruendung 1 Satz]"

Bei Ja: `second-brain`-MCP `remember`, Titel `Entscheidung <Projekt/Bereich>: <Kurz> <YYYY-MM-DD>`,
Kategorie `Programmierung/Entscheidungen`, Inhalt = Entscheidung + verworfene Alternative + Begruendung.
**Niemals automatisch speichern** (anders als Bugfixes) — Entscheidungen sind wertend, Frank bestaetigt
jede. Mehrere → in EINEM Vorschlagsblock sammeln.

## Was NIEMALS passieren darf

- Den SessionEnd-Hook ohne Franks Auftrag deaktivieren · eine Grundsatz-Entscheidung erkennen und den
  Vorschlag am Aufgabenende weglassen · eine Entscheidung OHNE Franks Ja ins Gehirn schreiben
- Das Titel-/Kategorie-Schema abwandeln (bricht Chronologie + Projektstand-Recall)
