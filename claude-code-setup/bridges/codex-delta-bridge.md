<!-- DEPRECATED: 2026-03-25 — Replaced by Universal Mirror Bridge (mirror-ledger.md + export/import agents) -->
<!-- This file is kept for reference only. New cross-platform sync uses mirror-ledger.md -->

# Codex Delta Bridge (fuer Claude Code)

Dies ist Claude Codes Bruecken-Spezifikation, um sinnvolle Verbesserungen aus Codex CLI
fuer die eigene Programmierumgebung zu erfassen.

## Scope

Read-only Quellen (Claude Code liest, schreibt NIEMALS dort):

- `codex-setup/**` — Regeln, Skills, Scripts, State, Bridges
- `codex-setup/state/environment-fixes.json` — Codex-Fehlerfixes
- `codex-setup/agent-memory/shared/MEMORY.md` — Codex-Whiteboard
- `AGENTS.md` — Codex-Agenten-Definitionen

Nicht Teil dieses Syncs:
- Normaler Projektcode, App-Features, Projektlogik
- Claude Code schreibt NIEMALS in Codex-Verzeichnisse

## Pflichtablauf

1. `codex-delta-state.json` lesen um den letzten geprüften Commit zu ermitteln
2. `git log --oneline <last_commit>..HEAD -- codex-setup/ AGENTS.md` ausfuehren
3. Geaenderte Dateien lesen und nur umgebungsbezogene Aenderungen betrachten
4. `codex-setup/state/environment-fixes.json` auf neue Eintraege pruefen
5. `codex-setup/agent-memory/shared/MEMORY.md` auf neue Erkenntnisse pruefen
6. Port-Kandidaten fuer Claude Code klassifizieren und als Liste praesentieren
7. Nach Benutzer-Approval: `codex-delta-state.json` aktualisieren

## Klassifikation

Jeder Delta-Kandidat wird als genau eine dieser Klassen berichtet:

- `ADD`: neue, additive Idee die Claude Code bisher nicht hat
- `ADAPT`: sinnvoll, aber muss fuer Claude Code uebersetzt/angepasst werden
- `REPLACE`: wuerde bestehende Claude Code Regeln oder Verhalten ersetzen

## Approval-Regel

- `ADD`: kann als sichere Empfehlung vorgeschlagen werden
- `ADAPT`: als Claude-Code-spezifische Portierung erklaeren
- `REPLACE`: vor der Umsetzung ausdruecklich warnen und Freigabe holen

Wenn alte und neue Logik beide nuetzlich sind, ist additive Integration der Standard.

## Ausgabeformat

Die menschenlesbare Liste ist deutsch und nach diesen Gruppen sortiert:

- `A1`, `A2`, ... Regeln und Prompts
- `B1`, `B2`, ... Agents, Skills und Arbeitsprozesse
- `C1`, `C2`, ... Skripte, Hooks und Validierung
- `D1`, `D2`, ... Runtime und Konfiguration
- `E1`, `E2`, ... Fehlerfixes aus environment-fixes.json

Zu jedem Punkt gehoeren mindestens:
- Klasse: `ADD`, `ADAPT` oder `REPLACE`
- Quelle (Datei in codex-setup/)
- Zielhinweis (wo in claude-code-setup/ uebernehmen)
- Kurzer Grund
- Klare Empfehlung
- Bugfix-/Haertungssignal: ja/nein

## Triggerwoerter

Diese Bruecke soll bei folgenden Formulierungen aktiviert werden:

- "starte die Bruecke zu Codex CLI"
- "was hat Codex CLI verbessert"
- "hol dir die neuesten Informationen von Codex"
- "welche Verbesserungen gibt es bei Codex"
- "welche Regeln sind bei Codex neuer"
- "welche Fehler hat Codex gefixt"
- "was kann Claude Code von Codex lernen"
- "Codex-Delta pruefen"
- "synchronisiere mit Codex"

## State

- `claude-code-setup/state/codex-delta-state.json` trackt den letzten geprüften Commit
- Wird nur nach Benutzer-Approval aktualisiert
- Ist keine operative Memory-Quelle; das Whiteboard bleibt die Wahrheit

## Sicherheitsregeln

- Claude Code liest Codex-Dateien NUR read-only
- Claude Code schreibt NIEMALS in codex-setup/
- Portierte Regeln gelten erst nach 5 realen Anwendungen als robust (Bewaehrungsphase)
