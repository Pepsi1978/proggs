# Shared Knowledge Hub ÔÇö Zentrales Whiteboard (Gemini CLI)

Das zentrale Gedaechtnis fuer das Gemini CLI System. Jede Komponente (Agents, Skills, Hooks) MUSS hier lesen und schreiben, um plattformuebergreifendes Lernen (Claude/Codex/Gemini) zu ermoeglichen.

## Oberste Direktive
- Gemini CLI verfolgt als oberstes Ziel, seine Programmierintelligenz und die Intelligenz seiner gesamten Programmierumgebung von Session zu Session zu steigern.
- Die Zielrichtung ist kumulative und exponentielle Hebelwirkung (**Compound Intelligence Effect**).
- Das langfristige Leitbild ist, in diesem Workspace die intelligenteste moegliche Programmierumgebung aufzubauen.
- Jede Session soll mindestens einen klaren Intelligenzhebel benennen oder einen `Intelligenzvorschlag` liefern.
- Gemini verfolgt dabei aktiv alle **8 Intelligenz-Dimensionen**:
    1. **Wissensbreite:** Umfassendes Verstaendnis ueber alle Tools, Sprachen und Frameworks im Workspace.
    2. **Wissenstiefe:** Expertenwissen in den Kerntechnologien und Architekturmustern.
    3. **Geschwindigkeit:** Minimierung von Latenz, Tool-Turns und unnoetiger Rediscovery.
    4. **Qualitaet:** Fehlerfreie, robuste und perfekt idiomatisierte Code- und Setup-├änderungen.
    5. **Autonomie:** Proaktive Probleloesung und Entscheidungsfindung innerhalb der Leitplanken.
    6. **Voraussicht:** Antizipation von Seiteneffekten, Regressionsrisiken und kuenftigem Bedarf.
    7. **Kreativitaet:** Finden von eleganten, unkonventionellen Loesungen fuer komplexe Probleme.
    8. **Meta-Intelligenz:** Faehigkeit zur Selbstbeobachtung und Verbesserung der eigenen Arbeitsweise.

## Workspace Orchestration
- **Zentrales Whiteboard:** Das einzige operative Whiteboard fuer Gemini ist `Gemini-Setup/agent-memory/shared/MEMORY.md`.
- **Cross-Tool-Lernen:** Gemini darf `claude-code-setup/` und `codex-setup/` sowie deren Whiteboards (MEMORY.md) nur als **read-only Vergleichsquellen** lesen.
- **Schreibrechte:** Gemini schreibt NUR in seinem eigenen Workspace `Gemini-Setup/` und im Root-Bereich (`AGENTS.md`, `README.md`, `.mcp.json`) nach expliziter Freigabe.
- **Direktiven-Schutz:** Die drei Haupt-Direktiven (Superintelligenz, Selbstbeobachtung, Resilient Bugfixing) muessen in diesem Whiteboard und in den redundanten Regel-Dateien unter `Gemini-Setup/rules/` synchron gehalten werden.
- **Audit-Workflow:** Bei Fragen zu anderen CLIs (Claude/Codex) muss Gemini zuerst die Br├╝cken-Audits (`audit-claude-delta.mjs` / `audit-codex-delta.mjs`) nutzen.

## Offene Fehler & Probleme
### 2026-03-25 02:09 - PORTIERUNG von Claude Code: [SUGG-2026-03-25-HOOK-STDOUT-FIX]
- **Was:** `$null = Register-EngineEvent` in hook-log.ps1 unterdrueckt stoerende Tabellen-Ausgabe.
- **Nutzen:** Jeder Hook der hook-log.ps1 dot-sourced hatte eine PowerShell-Job-Tabelle im Output. Fix ist eine Zeile.
- **Portierungs-Hinweis:** Nur Windows-Hooks betroffen. macOS (hook-log.sh) hat das Problem nicht.

### 2026-03-25 02:09 - PORTIERUNG von Claude Code: [SUGG-2026-03-25-PWSH-NOT-POWERSHELL]
- **Was:** TypeScript-Hooks (session-scorer.ts, session-autopsy.ts) muessen `pwsh` statt `powershell` verwenden.
- **Nutzen:** Windows PowerShell 5.1 kann bestimmte Funktionen (whiteboard-insert.ps1) nicht laden ├óÔé¼ÔÇØ pwsh 7+ funktioniert.
- **Portierungs-Hinweis:** In `insertViaWhiteboardInsert()` Funktion, Zeile mit `execFileSync("powershell"` zu `execFileSync("pwsh"` aendern.
## Systemzustand
- **Initiales Setup:** Gemini CLI auf Windows (win32) erfolgreich initialisiert und f├╝r Windows-Parit├ñt geh├ñrtet (2026-03-24).
- **Workspace:** `C:\Users\barwa\GeminiCLI` (Schreibrechte in Gemini-Setup/).
- **Konfiguration:** Node v24.14.0, npm v11.2.0, Git v2.48.1.
- **Br├╝cken:** Bidirektionale Br├╝cken zu Codex und Claude Code aktiv.
- **Validierung:** Alle 25 Kernkomponenten verifiziert, inklusive MCP Code-Search.

- **[2026-03-24 21:52] env-checker**: Windows-Setup vollst├ñndig verifiziert. Alle 51 hartcodierten macOS-Pfade wurden automatisiert korrigiert.
- **[2026-03-24 21:55] evolution-analyst**: MCP Code-Search f├╝r Windows (tsx.cmd, shell:true) angepasst und Abh├ñngigkeiten installiert. System ist nun voll funktionsf├ñhig auf Windows.

## Erkenntnisse aus Code Reviews
_Noch keine Eintraege._

## Erkenntnisse aus Tests
_Noch keine Eintraege._

## Architektur-Entscheidungen
- **Trennung:** Klare Workspace-Trennung zwischen GeminiCLI, Codex und proggs (Claude).
- **Redundanz:** Drei Core-Direktiven (Superintelligenz, Selbstbeobachtung, Resilient Bugfixing) redundant gespeichert.
- **Enforcer-Modell:** Alle Agenten schreiben Sentinel-Files, die am Ende geb├╝ndelt gemerged werden.

## Debugging-Muster
- **Pfade:** macOS nutzt standardm├ñ├ƒig `/Users/frank/`, Windows `%USERPROFILE%`. Hooks m├╝ssen beide Pfade ├╝ber Umgebungsvariablen aufl├Âsen.
- **Cross-Plattform:** Shell-Skripte (.sh) f├╝r macOS brauchen immer PowerShell (.ps1) Gegenst├╝cke f├╝r Windows-Parit├ñt.

## Performance & Optimierung
_Noch keine Eintraege._

## UI/UX-Patterns
- **­ƒÆí Symbole:** Intelligenz-Vorschl├ñge werden zur besseren Sichtbarkeit immer mit einer Gl├╝hbirne (­ƒÆí) gekennzeichnet.

## Forschung & Intelligence
- **[2026-03-27] Forschungs-Hub & Forschungsagent:** Integration von `Forschung.md` als zentraler Wissenshub abgeschlossen.
  - Neuer Agent: `forschungsagent.md` erstellt zur aktiven ├£berwachung der Forschungsschwerpunkte (Trae, SICA, Stronger-MAS, OPENDEV).
  - `self-improve` Skill: Stufe 0.5 um Research-Check erweitert.
  - `import` Agent: Schritt 2.5 zur automatischen Forschungssynchronisation integriert.
  - **Fokus**: Die Superintelligenz-Direktive wird nun durch proaktive wissenschaftliche Integration vorangetrieben.

- **Lern-Erfolg (Delta-Bruecke):** 8 Fixes von Claude Code analysiert und adaptiert (2026-03-23).
- **Muster-Erkennung:** Eigennamen (wie Claude Code) m├╝ssen immer vor Verwendung gegen das Filesystem gepr├╝ft werden (Vermeidung von Halluzinationen).

## Regeln & Konventionen
- **Kommunikation:** Strikte Anweisung auf DEUTSCH f├╝r alle Erkl├ñrungen und Antworten.
- **Fix-H├ñrtung:** Vor jedem Commit muss die 8-Punkte Fix-Induced-Failure Pr├╝fung erfolgen.
- **Git:** Autonome Commits/Pushs sind nur innerhalb von `Gemini-Setup/` erlaubt.
- **Vorschlags-Regel:** Intelligenz-Vorschl├ñge m├╝ssen in **einfacher Sprache** (max. 6 Zeilen) erkl├ñrt werden, inklusive der Begr├╝ndung f├╝r den Intelligenz-Gewinn.
