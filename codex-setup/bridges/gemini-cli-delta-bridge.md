# Gemini CLI Delta Bridge

Dies ist die Codex-Bruecke fuer Verbesserungen aus Gemini CLI.

Ziel:

- neue Regeln, Setup-Ideen, Arbeitsprozesse und Umgebungsverbesserungen aus `Gemini CLI`
- umgebungsbezogene Fehlerfixes, Memory-Muster und Haertungen aus `Gemini CLI`
- spaeter auch bereits umgesetzte `Intelligenzvorschlaege` aus einem optionalen Gemini-Ledger
- sauber, deutsch und approval-faehig auflisten
- nur die Programmierumgebung betrachten, nicht normalen Projektcode

## Quellseite

Read-only Quellen:

- `Gemini-Setup/**`
- `Gemini-Setup/agent-memory/shared/MEMORY.md`
- `%USERPROFILE%\GeminiCLI\agent-memory\shared\MEMORY.md`
- optional spaeter: `Gemini-Setup/state/implemented-intelligence-suggestions.json`
- optional lokal spaeter: `%USERPROFILE%\GeminiCLI\state\implemented-intelligence-suggestions.json`

## Zielseite

Codex schreibt nur in seine eigene Setup-Basis:

- `codex-setup/`
- `AGENTS.md`
- Codex-Validierung

## Exchange-Ledger-Adressen

- Codex-Ledger fuer umgesetzte Vorschlaege:
  `codex-setup/state/implemented-intelligence-suggestions.json`
- Erwartete Gemini-Repo-Adresse:
  `Gemini-Setup/state/implemented-intelligence-suggestions.json`
- Erwartete Gemini-Workspace-Adresse:
  `%USERPROFILE%\GeminiCLI\state\implemented-intelligence-suggestions.json`
- Erwartete Cloud-Code-Adresse fuer spaetere Fremd-Ports:
  `claude-code-setup/state/implemented-intelligence-suggestions.json`

Andere CLIs sollen dieselbe Dateiart im eigenen Setup anlegen und ihre Adresse in der eigenen Bruecke mit veroeffentlichen.

## Bootstrap fuer frische Rechner

- Codex-Referenz:
  `codex-setup/scripts/bootstrap-codex-setup.sh`
  und
  `codex-setup/scripts/bootstrap-codex-setup.ps1`
- Erwartete Gemini-Repo-Adresse:
  `Gemini-Setup/scripts/bootstrap-gemini-setup.sh`
  und
  `Gemini-Setup/scripts/bootstrap-gemini-setup.ps1`
- Erwartete Gemini-Workspace-Adresse:
  `%USERPROFILE%\GeminiCLI\scripts/bootstrap-gemini-setup.sh`
  und
  `%USERPROFILE%\GeminiCLI\scripts/bootstrap-gemini-setup.ps1`
- Erwartete Cloud-Code-Adresse:
  `claude-code-setup/scripts/bootstrap-claude-code-setup.sh`
  und
  `claude-code-setup/scripts/bootstrap-claude-code-setup.ps1`

Andere CLIs sollen fuer frische Rechner eigene Bootstrap-Skripte anlegen und deren genaue Adressen sowohl in der Registry als auch in der Delta-Bruecke mit veroeffentlichen.

## Bridge-Registry

- Zentrale Codex-Registry:
  `codex-setup/bridges/bridge-registry.json`
- GitHub:
  `https://github.com/Pepsi1978/proggs/blob/main/codex-setup/bridges/bridge-registry.json`
- Erwartete Gemini-Repo-Registry:
  `Gemini-Setup/bridges/bridge-registry.json`
- Erwartete Gemini-Workspace-Registry:
  `%USERPROFILE%\GeminiCLI\bridges\bridge-registry.json`
- Erwartete Cloud-Code-Registry:
  `claude-code-setup/bridges/bridge-registry.json`

Hinweis fuer andere Bruecken:
- Cloud Code und Gemini CLI sollen diese Registry-Dateiart ebenfalls uebernehmen und dort ihre exakten Ledger- und Bridge-Adressen veroeffentlichen.

## Pflichtklassifikation

Jeder Delta-Kandidat wird als genau eine dieser Klassen berichtet:

- `ADD`
- `ADAPT`
- `REPLACE`

## Approval-Regel

- `ADD`: sichere additive Empfehlung
- `ADAPT`: fuer Codex uebersetzen
- `REPLACE`: vor Umsetzung warnen und Freigabe holen

- Alle drei Klassen sind zunaechst nur Vorschlaege.
- Auch `ADD` und `ADAPT` duerfen niemals autonom umgesetzt werden.
- Erst nach ausdruecklicher Benutzerfreigabe auf Basis der A1/B1/C1/D1-Liste darf Codex die Vorschlaege uebernehmen.

## Ausgabeformat

Die menschenlesbare Liste soll deutsch sein und nach diesen Gruppen sortiert werden:

- `A1`, `A2`, ... Regeln und Prompts
- `B1`, `B2`, ... Agenten, Skills und Arbeitsprozesse
- `C1`, `C2`, ... Skripte, Memory-Muster und Validierung
- `D1`, `D2`, ... Runtime und Konfiguration

## Triggerwoerter

- "Starte bitte die Bruecke zu Gemini CLI"
- "starte die Bruecke zu Gemini CLI"
- "was wurde bei Gemini CLI erneuert"
- "welche Verbesserungen gibt es bei Gemini CLI"
- "welche Regeln sind bei Gemini CLI neuer"
- "welche Fehler wurden bei Gemini CLI gefixt"
- "was davon ist fuer Codex sinnvoll"

## Referenzimplementierung

- `codex-setup/scripts/audit-gemini-delta.mjs`
- `codex-setup/scripts/audit-gemini-delta.sh`
- `codex-setup/scripts/audit-gemini-delta.ps1`

Der Satz `Starte bitte die Bruecke zu Gemini CLI` soll bei Codex immer direkt diese Referenzimplementierung ausloesen.
