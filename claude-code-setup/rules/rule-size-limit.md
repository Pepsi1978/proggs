# Regel-Groessenlimit: max 2 KB pro Regel-Datei (KRITISCH)

> Gilt AUSNAHMSLOS fuer JEDE Datei in `~/.claude/rules/` — auch die 3 Direktiven-Kerne. Die 1:1-Volltexte
> der Direktiven + grosser Regeln liegen in `claude-code-setup/docs/rules/` und sind dort unbegrenzt.

## Die Grenze

Jede Regel-Datei in `~/.claude/rules/` darf **maximal 2048 Byte (2 KB)** gross sein. Grund: alle
`rules/*.md` werden bei JEDER Session voll geladen (fixer Token-Sockel); je mehr Instruktionen, desto
schlechter die Befolgung ALLER Regeln ("double the instructions, halve the compliance", Context-Rot).
Kleine, fokussierte Regeln = bessere Befolgung + weniger Kontext.

## Wie man unter 2 KB bleibt (verlustfrei)

- **Kern rein, Detail auslagern:** Grundregel + Verbote in die rules-Datei; Volltext, Beispiele, grosse
  Tabellen nach `claude-code-setup/docs/rules/<name>.md` (per `Read` nachladbar). Die Regel verweist per
  Pfad — verlustfrei (progressive disclosure, `lossless-context-principle.md`).
- **Prosa raffen:** Begruendung auf 1 Satz, keine Wiederholungen, keine langen Inline-Beispiele.
- **Nie Funktionalitaet wegwerfen** — nur auslagern.

## Durchsetzung

Der Hook `rule-size-guard` (PreToolUse Write/Edit) BLOCKIERT das Speichern einer `rules/*.md` >2048 Byte
(via `permissionDecision=deny`; gemeinsame Logik `rule-size-guard.py`, Wrapper `.ps1`/`.sh`). Die
Volltexte in `docs/rules/` sind ausgenommen. Notaus bei Fehlalarm: leere Datei
`rule-size-guard-disable.flag` im TEMP.

## Was NIEMALS passieren darf

- Eine neue oder geaenderte Regel in `rules/` >2 KB speichern
- Funktionalitaet wegwerfen statt sie nach `docs/rules/` auszulagern, nur um unter 2 KB zu kommen
