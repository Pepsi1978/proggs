# Regel-Groessenlimit: max 2 KB pro Regel-Datei (KRITISCH)

> Gilt fuer JEDE Datei in `~/.claude/rules/` — AUSSER die 3 Direktiven (`superintelligence.md`,
> `self-observation.md`, `resilient-bugfixing.md`), die unbegrenzt bleiben.

## Die Grenze

Jede Regel-Datei darf **maximal 2048 Byte (2 KB)** gross sein — darueber ist verboten. Grund: alle
`rules/*.md` werden bei JEDER Session voll geladen (fixer Token-Sockel); je mehr Instruktionen, desto
schlechter die Befolgung ALLER Regeln ("double the instructions, halve the compliance", Context-Rot).
Kleine, fokussierte Regeln = bessere Befolgung + weniger Kontext.

## Wie man unter 2 KB bleibt (verlustfrei)

- **Kern rein, Detail auslagern:** Grundregel + Verbote in die rules-Datei; Volltext, Beispiele, grosse
  Tabellen nach `claude-code-setup/docs/<name>-detail.md` (per `Read` nachladbar). Die Regel verweist
  per Pfad — verlustfrei (progressive disclosure, `lossless-context-principle.md`).
- **Prosa raffen:** Begruendung auf 1 Satz, keine Wiederholungen, keine langen Inline-Beispiele.
- **Nie Funktionalitaet wegwerfen** — nur auslagern.

## Durchsetzung

Der Hook `rule-size-guard` (PreToolUse Write/Edit) BLOCKIERT das Speichern einer `rules/*.md` >2048 Byte
(ausser den 3 Direktiven) — Poka-Yoke Stufe 2. Notaus bei Fehlalarm: leere Datei
`rule-size-guard-disable.flag` im TEMP.

## Was NIEMALS passieren darf

- Eine neue oder geaenderte Regel >2 KB speichern (ausser den 3 Direktiven)
- Funktionalitaet wegwerfen statt sie auszulagern, nur um unter 2 KB zu kommen
