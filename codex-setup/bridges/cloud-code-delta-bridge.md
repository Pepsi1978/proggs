# Cloud Code Delta Bridge

Dies ist die generische Bruecken-Spezifikation fuer andere CLI-Programmierumgebungen.

Ziel:

- neue Regeln, Agents, Skills, Hooks, Arbeitsprozesse und Setup-Ideen aus `Cloud Code`
- umgebungsbezogene Fehlerfixes, Haertungen und zukunftsfaehige Fixlogik aus `Cloud Code`
- spaeter auch bereits umgesetzte `Intelligenzvorschlaege` aus einem optionalen Cloud-Code-Ledger
- sauber, deutsch und approval-faehig auflisten
- nur die Programmierumgebung betrachten, nicht normalen Projektcode
- in den jeweils eigenen Ziel-Workspace portieren, ohne die Quellumgebung zu beschreiben

Fehlerfixe sind nur dann Teil der Bruecke, wenn sie sich auf Regeln, Setup, Hooks, Agenten, Skills,
Validierung oder die Programmierumgebung beziehen.

## Quellseite

Fuer Codex ist `Cloud Code` gleich `Claude Code`.

Read-only Quellen:

- `CLAUDE.md`
- `claude-code-setup/**`
- optional spaeter: `claude-code-setup/state/implemented-intelligence-suggestions.json`

Andere CLIs duerfen dieselbe Bruecke nutzen, muessen diese Quellen aber ebenfalls read-only behandeln.

## Zielseite

Der CLI-Agent, der diese Bruecke nachbaut, schreibt nur in seine eigene Setup-Basis und seinen eigenen Workspace.

Beispiele:

- Codex schreibt nur in `codex-setup/`, `AGENTS.md` und eigene Validierung
- Gemini CLI soll spaeter nur in seine eigene Setup-Basis und seinen eigenen Workspace schreiben

## Exchange-Ledger-Adressen

- Codex-Ledger fuer umgesetzte Vorschlaege:
  `codex-setup/state/implemented-intelligence-suggestions.json`
- Erwartete Cloud-Code-Adresse:
  `claude-code-setup/state/implemented-intelligence-suggestions.json`
- Erwartete Gemini-Adresse fuer spaetere Fremd-Ports:
  `Gemini-Setup/state/implemented-intelligence-suggestions.json`

Andere CLIs sollen dieselbe Dateiart im eigenen Setup anlegen und ihre Adresse in der eigenen Bruecke mit veroeffentlichen.

## Bootstrap fuer frische Rechner

- Codex-Referenz:
  `codex-setup/scripts/bootstrap-codex-setup.sh`
  und
  `codex-setup/scripts/bootstrap-codex-setup.ps1`
- Erwartete Cloud-Code-Adresse:
  `claude-code-setup/scripts/bootstrap-claude-code-setup.sh`
  und
  `claude-code-setup/scripts/bootstrap-claude-code-setup.ps1`
- Erwartete Gemini-Adresse:
  `Gemini-Setup/scripts/bootstrap-gemini-setup.sh`
  und
  `Gemini-Setup/scripts/bootstrap-gemini-setup.ps1`

Andere CLIs sollen fuer frische Rechner eigene Bootstrap-Skripte anlegen und deren genaue Adressen sowohl in der Registry als auch in der Delta-Bruecke mit veroeffentlichen.

## Bridge-Registry

- Zentrale Codex-Registry:
  `codex-setup/bridges/bridge-registry.json`
- GitHub:
  `https://github.com/Pepsi1978/proggs/blob/main/codex-setup/bridges/bridge-registry.json`
- Erwartete Cloud-Code-Registry:
  `claude-code-setup/bridges/bridge-registry.json`
- Erwartete Gemini-Registry:
  `Gemini-Setup/bridges/bridge-registry.json`

Hinweis fuer andere Bruecken:
- Cloud Code und Gemini CLI sollen diese Registry-Dateiart ebenfalls uebernehmen und dort ihre exakten Ledger- und Bridge-Adressen veroeffentlichen.

## Pflichtklassifikation

Jeder Delta-Kandidat wird als genau eine dieser Klassen berichtet:

- `ADD`: neue, additive Idee
- `ADAPT`: sinnvoll, aber fuer die Zielumgebung zu uebersetzen
- `REPLACE`: wuerde bestehende Regeln, Prompts oder Verhalten der Zielumgebung ersetzen

## Approval-Regel

- `ADD`: kann als sichere Standardempfehlung vorgeschlagen werden
- `ADAPT`: als zielsystem-spezifische Portierung erklaeren
- `REPLACE`: vor der Umsetzung ausdruecklich warnen und Freigabe holen

- Alle drei Klassen sind zunaechst nur Vorschlaege.
- Auch `ADD` und `ADAPT` duerfen niemals autonom umgesetzt werden.
- Erst nach ausdruecklicher Benutzerfreigabe auf Basis der A1/B1/C1/D1-Liste darf der Zielagent die Vorschlaege uebernehmen.

Wenn alte und neue Logik beide nuetzlich sind, ist additive Integration der Standard.

## Ausgabeformat

Die menschenlesbare Liste soll deutsch sein und nach diesen Gruppen sortiert werden:

- `A1`, `A2`, ... Regeln und Prompts
- `B1`, `B2`, ... Agents, Skills und Arbeitsprozesse
- `C1`, `C2`, ... Skripte, Hooks und Validierung
- `D1`, `D2`, ... Runtime und Konfiguration

Zu jedem Punkt gehoeren mindestens:

- Klasse: `ADD`, `ADAPT` oder `REPLACE`
- Quelle
- Zielhinweis
- kurzer Grund
- klare Empfehlung

Wenn erkennbar, zusaetzlich:

- `Bugfix-/Haertungssignal: ja/nein`
- kurzer Hinweis, ob Fixlogik oder Regression-Schutz aus der Quelle uebernommen werden sollte

## Triggerwoerter

Die Bruecke soll bei deutschen Formulierungen wie diesen aktiviert werden:

- "hol dir die neuesten Informationen von Cloud Code"
- "Starte bitte die Bruecke zu Cloud Code"
- "was wurde bei Cloud Code erneuert"
- "welche Verbesserungen gibt es bei Cloud Code"
- "welche Regeln sind dort neuer"
- "welche Agenten oder Arbeitsprozesse sind dort neuer"
- "welche Fehler wurden bei Cloud Code gefixt"
- "welche robuste Fixlogik gibt es bei Cloud Code"
- "was davon ist fuer [Ziel-CLI] sinnvoll"
- "schau im Codex Setup Ordner nach dieser Bruecke"
- "baue diese Bruecke auch fuer [Ziel-CLI]"

## Referenzimplementierung

Fuer Codex ist die Referenzimplementierung:

- `codex-setup/scripts/audit-claude-delta.mjs`
- `codex-setup/scripts/audit-claude-delta.sh`
- `codex-setup/scripts/audit-claude-delta.ps1`

Der Satz `Starte bitte die Bruecke zu Cloud Code` soll bei Codex immer direkt diese Referenzimplementierung ausloesen.

Der dazugehoerige State liegt unter:

- `codex-setup/state/claude-delta-state.json`
