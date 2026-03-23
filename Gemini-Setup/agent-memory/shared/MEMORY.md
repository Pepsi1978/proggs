# Shared Knowledge Hub — Zentrales Whiteboard (Gemini CLI)

Das zentrale Gedaechtnis fuer das Gemini CLI System. Jede Komponente (Agents, Skills, Hooks) MUSS hier lesen und schreiben, um plattformuebergreifendes Lernen (Claude/Codex/Gemini) zu ermoeglichen.

## Oberste Direktive

- Gemini CLI verfolgt als oberstes Ziel, seine Programmierintelligenz und die Intelligenz seiner gesamten Programmierumgebung von Session zu Session zu steigern.
- Die Zielrichtung ist kumulative und exponentielle Hebelwirkung.
- Das langfristige Leitbild ist, in diesem Workspace die intelligenteste moegliche Programmierumgebung aufzubauen.
- Jede Session soll mindestens einen klaren Intelligenzhebel benennen oder einen `Intelligenzvorschlag` liefern.

---

## Offene Fehler & Probleme (Zentrale Fehler-Fix-Datenbank)
<!-- Diese Sektion wird von Gemini, Claude und Codex gelesen, um voneinander zu lernen. -->
<!-- PFLICHT-FORMAT fuer neue Eintraege (MUSS kontextunabhaengig fuer Claude/Codex erklaert sein): -->
<!--   ### DATUM — Quelle: [Komponententyp: Name] — Kurzbeschreibung -->
<!--   **Kontext:** Welches Ziel wurde verfolgt? (Wichtig fuer CLIs ohne Session-Historie!) -->
<!--   **Quelle:** Welche Komponente (Hook/Agent/Tool/Skill + Name) -->
<!--   **Symptom:** Was ist sichtbar schiefgegangen? (Fehlermeldungen, Verhalten) -->
<!--   **Ursache:** WARUM ist es passiert? (Root Cause, Pfad-Probleme, OS-Unterschiede) -->
<!--   **Betroffene Dateien:** Welche Dateien wurden geaendert? -->
<!--   **Reproduktion:** Wie kann man den Fehler nachstellen? -->
<!--   **Fix-Details & Begruendung:** Was wurde getan und WARUM? (Logik-Erklaerung fuer andere CLIs) -->
<!--   **Status:** OFFEN | GEFIXT (Datum) -->

<!-- GEFIXTE FEHLER ARCHIV (Format: <!-- ARCHIV (DATUM): [Komponente] Beschreibung — GEFIXT. --\> ) -->

---

## Gedaechtnisstand

- Initiales Setup fuer Gemini CLI durchgefuehrt.
- Regeln von Codex adaptiert und in `Gemini-Setup/rules/global.md` dokumentiert.
- Schreibberechtigungen fuer `Gemini-Setup` etabliert.
- Whisper-API (Spracheingabe) als Primaerquelle des Benutzers vermerkt.
- Workspace-Trennung: GeminiCLI (Schreibrechte), proggs/Codex (Nur-Lesezugriff).
- **Bruecken-Logik:** Struktur fuer Fehler-Fix-Datenbank analog zu Claude/Codex implementiert (2026-03-23).
- **Lern-Erfolg (Delta-Bruecke):** 8 Fixes von Claude Code analysiert und adaptiert (2026-03-23).
  - *Wichtigste Erkenntnisse:* Hooks duerfen Fehler nicht verschlucken; StopFailure-Hooks duerfen keine API-Abhaengigkeit haben (Zirkularitaet); .ps1 Hooks brauchen .sh Gegenstuecke fuer Cross-Plattform-Stabilitaet.

## Aktuelle Systemkonfiguration (Windows)

Folgende Tools sind auf diesem Windows-System installiert und verifiziert:
- **Node.js**: v24.14.0
- **npm**: 11.12.0
- **Git**: 2.53.0.windows.2
- **PowerShell (pwsh)**: 7.6.0 (installiert)
- **PowerShell (Standard)**: 5.1 (aktuelle Session)

*Hinweis: Docker und VS Code konnten nicht über die Standard-CLI-Befehle im aktuellen Pfad gefunden werden.*

