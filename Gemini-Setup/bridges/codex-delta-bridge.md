# Codex Delta Bridge (fuer Gemini CLI)

Dies ist Gemini CLIs Bruecken-Spezifikation, um sinnvolle Verbesserungen aus Codex CLI
fuer die eigene Programmierumgebung zu erfassen.

## Scope

Read-only Quellen (Gemini CLI liest, schreibt NIEMALS dort):

- `codex-setup/**` ÔÇö Regeln, Skills, Scripts, State, Bridges
- `codex-setup/state/environment-fixes.json` ÔÇö Codex-Fehlerdatenbank
- `codex-setup/agent-memory/shared/MEMORY.md` ÔÇö Codex-Whiteboard
- `codex-setup/bridges/bridge-registry.json` ÔÇö Codex Bridge-Registry
- `AGENTS.md` ÔÇö Codex-Agenten-Definitionen

Nicht Teil dieses Syncs:
- Normaler Projektcode, App-Features, Projektlogik
- Gemini CLI schreibt NIEMALS in codex-setup/

## Pflichtablauf

1. `Gemini-Setup/state/codex-delta-state.json` lesen um den letzten geprueften Commit zu ermitteln
2. `git log --oneline <last_commit>..HEAD -- codex-setup/ AGENTS.md` ausfuehren
3. Wenn keine neuen Commits: "Kein Delta seit letztem Check." melden und beenden
4. Geaenderte Dateien lesen und nur umgebungsbezogene Aenderungen betrachten
5. `codex-setup/state/environment-fixes.json` auf neue Eintraege pruefen
6. `codex-setup/agent-memory/shared/MEMORY.md` auf neue Erkenntnisse pruefen
7. `codex-setup/bridges/bridge-registry.json` auf neue Bridges pruefen
8. Port-Kandidaten fuer Gemini CLI klassifizieren und als Liste praesentieren
9. Nach Benutzer-Approval: `codex-delta-state.json` aktualisieren

## Klassifikation

Jeder Delta-Kandidat wird als genau eine dieser Klassen berichtet:

- `ADD`: neue, additive Idee die Gemini CLI bisher nicht hat
- `ADAPT`: sinnvoll, aber muss fuer Gemini CLI uebersetzt/angepasst werden
- `REPLACE`: wuerde bestehende Gemini CLI Regeln oder Verhalten ersetzen

## Approval-Regel

- `ADD`: kann als sichere Empfehlung vorgeschlagen werden
- `ADAPT`: als Gemini-CLI-spezifische Portierung erklaeren
- `REPLACE`: vor der Umsetzung ausdruecklich warnen und Freigabe holen
- KEINE autonome Umsetzung ÔÇö immer erst Benutzerfreigabe einholen

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
- Zielhinweis (wo in Gemini-Setup/ uebernehmen)
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
- "was kann Gemini von Codex lernen"
- "Codex-Delta pruefen"
- "synchronisiere mit Codex"

## State

- `Gemini-Setup/state/codex-delta-state.json` trackt den letzten geprueften Commit
- Wird nur nach Benutzer-Approval aktualisiert
- Ist keine operative Memory-Quelle; das Whiteboard bleibt die Wahrheit

## Sicherheitsregeln

- Gemini CLI liest Codex-Dateien NUR read-only
- Gemini CLI schreibt NIEMALS in codex-setup/
- Portierte Regeln gelten erst nach 5 realen Anwendungen als robust (Bewaehrungsphase)
