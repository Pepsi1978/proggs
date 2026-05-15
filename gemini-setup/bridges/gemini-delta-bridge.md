<!-- DEPRECATED: 2026-03-25 — Replaced by Universal Mirror Bridge (mirror-ledger.md + export/import agents) -->
<!-- This file is kept for reference only. New cross-platform sync uses mirror-ledger.md -->

# Gemini Delta Bridge (fuer Gemini CLI)

Dies ist Gemini CLIs Bruecken-Spezifikation, um sinnvolle Verbesserungen aus Gemini CLI
fuer die eigene Programmierumgebung zu erfassen.

## Scope

Read-only Quellen (Gemini CLI liest, schreibt NIEMALS dort):

- `gemini-setup/**` — Regeln, Whiteboard, alles was Gemini CLI dort ablegt
- `gemini-setup/agent-memory/shared/MEMORY.md` — Gemini-Whiteboard (Fehler-Fixes, Erkenntnisse, Regeln)
- `gemini-setup/rules/global.md` — Gemini-Regeln

Nicht Teil dieses Syncs:
- Normaler Projektcode, App-Features, Projektlogik
- Gemini CLI schreibt NIEMALS in gemini-setup/ oder GeminiCLI/

## Pflichtablauf

1. `gemini-delta-state.json` lesen um den letzten geprüften Commit zu ermitteln
2. `git log --oneline <last_commit>..HEAD -- gemini-setup/` ausfuehren
3. Geaenderte Dateien lesen und nur umgebungsbezogene Aenderungen betrachten
4. `gemini-setup/agent-memory/shared/MEMORY.md` auf neue Erkenntnisse und Fehler-Fixes pruefen
5. `gemini-setup/rules/global.md` auf neue Regeln pruefen
6. Port-Kandidaten fuer Gemini CLI klassifizieren und als Liste praesentieren
7. Nach Benutzer-Approval: `gemini-delta-state.json` aktualisieren

## Klassifikation

Jeder Delta-Kandidat wird als genau eine dieser Klassen berichtet:

- `ADD`: neue, additive Idee die Gemini CLI bisher nicht hat
- `ADAPT`: sinnvoll, aber muss fuer Gemini CLI uebersetzt/angepasst werden
- `REPLACE`: wuerde bestehende Gemini CLI Regeln oder Verhalten ersetzen

## Approval-Regel

- `ADD`: kann als sichere Empfehlung vorgeschlagen werden
- `ADAPT`: als gemini-setup-spezifische Portierung erklaeren
- `REPLACE`: vor der Umsetzung ausdruecklich warnen und Freigabe holen

Wenn alte und neue Logik beide nuetzlich sind, ist additive Integration der Standard.

## Ausgabeformat

Die menschenlesbare Liste ist deutsch und nach diesen Gruppen sortiert:

- `A1`, `A2`, ... Regeln und Prompts
- `B1`, `B2`, ... Agents, Skills und Arbeitsprozesse
- `C1`, `C2`, ... Skripte, Hooks und Validierung
- `D1`, `D2`, ... Runtime und Konfiguration
- `E1`, `E2`, ... Fehlerfixes aus dem Whiteboard

Zu jedem Punkt gehoeren mindestens:
- Klasse: `ADD`, `ADAPT` oder `REPLACE`
- Quelle (Datei in gemini-setup/)
- Zielhinweis (wo in gemini-setup/ uebernehmen)
- Kurzer Grund
- Klare Empfehlung
- Bugfix-/Haertungssignal: ja/nein

## Triggerwoerter

Diese Bruecke soll bei folgenden Formulierungen aktiviert werden:

- "starte die Bruecke zu Gemini CLI"
- "was hat Gemini CLI verbessert"
- "hol dir die neuesten Informationen von Gemini"
- "welche Verbesserungen gibt es bei Gemini"
- "welche Regeln sind bei Gemini neuer"
- "welche Fehler hat Gemini gefixt"
- "was kann Gemini CLI von Gemini lernen"
- "Gemini-Delta pruefen"
- "synchronisiere mit Gemini"

## State

- `gemini-setup/state/gemini-delta-state.json` trackt den letzten geprüften Commit
- Wird nur nach Benutzer-Approval aktualisiert
- Ist keine operative Memory-Quelle; das Whiteboard bleibt die Wahrheit

## Sicherheitsregeln

- Gemini CLI liest Gemini-Dateien NUR read-only
- Gemini CLI schreibt NIEMALS in gemini-setup/ oder GeminiCLI/
- Portierte Regeln gelten erst nach 5 realen Anwendungen als robust (Bewaehrungsphase)

## Hinweis

Gemini CLI ist noch neu eingerichtet (seit 2026-03-23). Die Datenbasis wird ueber die
naechsten Sessions wachsen. Die Bruecke ist trotzdem schon funktionsfaehig und wird
automatisch mehr Inhalte finden sobald Gemini CLI seine eigenen Fixes und Regeln aufbaut.

