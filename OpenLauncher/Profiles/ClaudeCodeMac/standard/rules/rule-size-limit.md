# Regel-Groessenlimit: max 1,5 KB pro Regel-Datei (KRITISCH)

> Gilt AUSNAHMSLOS fuer JEDE Datei in `~/.claude/rules/` — auch die 3 Direktiven-Kerne. Die 1:1-Volltexte
> der Direktiven + grosser Regeln liegen in `claude-code-setup/docs/rules/` und sind dort unbegrenzt.

## Die Grenze
Jede Datei in `~/.claude/rules/` darf **max 1536 Byte (1,5 KB)** gross sein. Grund: alle `rules/*.md`
werden bei JEDER Session voll geladen (fixer Token-Sockel); je mehr Instruktionen, desto schlechter die
Befolgung ALLER Regeln (Context-Rot). Kleine, fokussierte Regeln = bessere Befolgung + weniger Kontext.

## Unter 1,5 KB bleiben (verlustfrei)
- **Kern rein, Detail auslagern:** Grundregel + Verbote in die rules-Datei; Volltext/Beispiele/Tabellen
  nach `claude-code-setup/docs/rules/<name>.md` (per `Read` nachladbar). Verweis per Pfad — verlustfrei.
- **Prosa raffen:** Begruendung auf 1 Satz, keine Wiederholungen, keine langen Inline-Beispiele.
- **Nie Funktionalitaet wegwerfen** — nur auslagern.

## Durchsetzung
Der Hook `rule-size-guard` (PreToolUse Write/Edit) BLOCKIERT `rules/*.md` >1536 Byte (via
`permissionDecision=deny`; Logik `rule-size-guard.py`, Wrapper `.ps1`/`.sh`). Volltexte in `docs/rules/`
ausgenommen. Notaus: leere Datei `rule-size-guard-disable.flag` im TEMP.

## Was NIEMALS
- Eine Regel in `rules/` >1,5 KB speichern · Funktionalitaet wegwerfen statt nach `docs/rules/` auszulagern.
