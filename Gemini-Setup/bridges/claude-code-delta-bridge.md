# Claude Code Delta Bridge (fuer Gemini CLI)

Dies ist Gemini CLIs Bruecken-Spezifikation, um sinnvolle Verbesserungen aus Claude Code
fuer die eigene Programmierumgebung zu erfassen.

## Scope

Read-only Quellen (Gemini CLI liest, schreibt NIEMALS dort):

- `claude-code-setup/**` ÔÇö Regeln, Agents, Skills, Hooks, Scripts, Bridges, State
- `claude-code-setup/environment-fixes.md` ÔÇö Gemeinsame Fehlerdatenbank
- `CLAUDE.md` ÔÇö Claude Code Projektregeln

Nicht Teil dieses Syncs:
- Normaler Projektcode, App-Features, Projektlogik
- Gemini CLI schreibt NIEMALS in claude-code-setup/

## Pflichtablauf

1. `Gemini-Setup/state/claude-code-delta-state.json` lesen um den letzten geprueften Commit zu ermitteln
2. `git log --oneline <last_commit>..HEAD -- claude-code-setup/ CLAUDE.md` ausfuehren
3. Wenn keine neuen Commits: "Kein Delta seit letztem Check." melden und beenden
4. Geaenderte Dateien lesen und nur umgebungsbezogene Aenderungen betrachten
5. `claude-code-setup/environment-fixes.md` auf neue Eintraege pruefen
6. `claude-code-setup/bridges/` auf neue Bridges oder universelle Direktiven pruefen
7. Port-Kandidaten fuer Gemini CLI klassifizieren und als Liste praesentieren
8. Nach Benutzer-Approval: `claude-code-delta-state.json` aktualisieren

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
- `E1`, `E2`, ... Fehlerfixes aus environment-fixes.md

Zu jedem Punkt gehoeren mindestens:
- Klasse: `ADD`, `ADAPT` oder `REPLACE`
- Quelle (Datei in claude-code-setup/)
- Zielhinweis (wo in Gemini-Setup/ uebernehmen)
- Kurzer Grund
- Klare Empfehlung
- Bugfix-/Haertungssignal: ja/nein

## Triggerwoerter

Diese Bruecke soll bei folgenden Formulierungen aktiviert werden:

- "starte die Bruecke zu Claude Code"
- "was hat Claude Code verbessert"
- "hol dir die neuesten Informationen von Claude Code"
- "welche Verbesserungen gibt es bei Claude Code"
- "welche Regeln sind bei Claude Code neuer"
- "welche Fehler hat Claude Code gefixt"
- "was kann Gemini von Claude Code lernen"
- "Claude-Code-Delta pruefen"
- "synchronisiere mit Claude Code"

## State

- `Gemini-Setup/state/claude-code-delta-state.json` trackt den letzten geprueften Commit
- Wird nur nach Benutzer-Approval aktualisiert
- Ist keine operative Memory-Quelle; das Whiteboard bleibt die Wahrheit

## Sicherheitsregeln

- Gemini CLI liest Claude-Code-Dateien NUR read-only
- Gemini CLI schreibt NIEMALS in claude-code-setup/
- Portierte Regeln gelten erst nach 5 realen Anwendungen als robust (Bewaehrungsphase)
