# Codex Whiteboard

Dieses Whiteboard ist das zentrale Nervensystem fuer Codex in diesem Workspace.
Es ist die einzige autoritative operative Memory fuer den Codex-Self-Improve-Workflow.

## Oberste Direktive

### Direktive 1: Superintelligenz
- Dieses Codex-Setup soll von Session zu Session intelligenter werden.
- Bevorzugt werden Verbesserungen mit Compound-Effekt statt lokaler Bequemlichkeit.
- Die 8 Intelligenz-Dimensionen muessen gemeinsam wachsen: Wissensbreite, Wissenstiefe, Geschwindigkeit, Qualitaet, Autonomie, Voraussicht, Kreativitaet, Meta-Intelligenz.

### Direktive 2: Selbstbeobachtung
- Waehrend der Arbeit aktiv beobachten, aber Vorschlaege erst am Ende melden.
- Die 6 Beobachtungskategorien sind: Fehler, Umwege, Effizienz, Wissensluecken, Muster, Benutzer-Hinweise.
- Benutzer-Hinweise sofort dauerhaft sichern.
- Wiederholte Fehler oder Wiederholungen derselben Korrektur sind Alarm-Signale auf Klassenebene.

### Direktive 3: Resilient Bugfixing
- Jeder Umgebungsfehler bekommt 3x Warum, Related-Surface-Review und einen zukunftsfesten Fix.
- Vor einem Abschluss pruefen: 8-Punkte-Fix-Review, Defense in Depth und Plattform-Dauerhaftigkeit.
- Kein Fix darf nur lokal funktionieren und spaeter beim naechsten Update wieder brechen.

## Offene Fehler & Probleme

- 2026-04-22: `codex-setup/` wurde in diesem Workspace erst jetzt bootstrappt. Vorher war der lokal installierte `self-improve`-Skill ohne repo-native Quelle und ohne autoritatives Codex-Whiteboard.

## Systemzustand

2026-04-22: Codex-Setup-Bootstrap abgeschlossen. Repo-native Whiteboard, State, Bridge-Skripte und Self-Improve-Repoquelle sind jetzt vorhanden.
- Stand: 2026-04-22
- Workspace: `C:\Users\barwa\Codex`
- Repo-native Codex-Steuerzentrale: `codex-setup/`
- Whiteboard-Bridge: minimal vorhanden
- Self-Improve-Repoquelle: wird aus `~/.codex/skills/self-improve/` gespiegelt

## Erkenntnisse aus Code Reviews

- Noch keine Eintraege.

## Erkenntnisse aus Tests

- Noch keine Eintraege.

## Architektur-Entscheidungen

- Codex nutzt ein eigenes Whiteboard in `codex-setup/` statt `claude-code-setup/` oder `.claude/`.
- `claude-code-setup/` bleibt eine read-only Vergleichsquelle.

## Debugging-Muster

- Fehlende repo-lokale Kontrollstruktur fuer Codex fuehrt zu Drift zwischen lokal installiertem Skill und Workspace.

## Performance & Optimierung

- Compound-Gewinne gehen vor lokaler Bequemlichkeit.

## UI/UX-Patterns

- Nicht belegt.

## Forschung & Intelligence

- Nicht belegt.

## Regeln & Konventionen

- Whiteboard-Schreibzugriffe laufen ueber `codex-setup/scripts/whiteboard-bridge.mjs` oder die Wrapper.
- Direkte Append-Muster an das Whiteboard sind verboten.
- Codex-spezifische Setup-Aenderungen gehoeren nach `codex-setup/`.
