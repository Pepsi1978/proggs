---
name: command--gemini-bridge
description: Converted from command gemini-bridge
---

# Gemini Delta Bridge

Pruefe was Gemini CLI seit dem letzten Checkpoint verbessert hat und portiere sinnvolle
Aenderungen in die Codex Umgebung.

## Ablauf

1. Lies `codex-setup/state/gemini-delta-state.json` fuer den letzten geprueften Commit
2. Fuehre aus: `cd ~/prompts:proggs && git log --oneline <last_commit>..HEAD -- gemini-setup/`
3. Wenn keine neuen Commits: "Kein Delta seit letztem Check." melden und beenden
4. Geaenderte Dateien lesen — NUR umgebungsbezogene Aenderungen betrachten
5. `gemini-setup/shared/MEMORY.md` auf neue Erkenntnisse und Fehler-Fixes pruefen
6. `gemini-setup/rules/*.md` und `gemini-setup/archive/rules/*.md` auf neue Regeln pruefen, falls diese Pfade existieren
7. Jeden Kandidaten klassifizieren und als strukturierte Liste praesentieren

## Klassifikation

- `ADD`: Neue Idee die Codex bisher nicht hat → als Empfehlung vorschlagen
- `ADAPT`: Sinnvoll, muss aber fuer Codex angepasst werden → Portierung erklaeren
- `REPLACE`: Wuerde Bestehendes ersetzen → VOR Umsetzung Freigabe holen

## Ausgabeformat (deutsch, nach Gruppen sortiert)

- `A1-An`: Regeln und Prompts
- `B1-Bn`: Agents, Skills und Arbeitsprozesse
- `C1-Cn`: Skripte, Hooks und Validierung
- `D1-Dn`: Runtime und Konfiguration
- `E1-En`: Fehlerfixes

Pro Punkt: Klasse, Quelle, Ziel, Grund, Empfehlung, Bugfix-Signal (ja/nein)

## Sicherheitsregeln

- NUR READ-ONLY Zugriff auf `gemini-setup/` — NIEMALS dort schreiben
- Portierte Regeln gelten erst nach 5 Anwendungen als robust
- Nach Benutzer-Approval: `codex-setup/state/gemini-delta-state.json` aktualisieren
