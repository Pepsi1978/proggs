# Shared Knowledge Hub — Zentrales Whiteboard (Gemini CLI)

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
    4. **Qualitaet:** Fehlerfreie, robuste und perfekt idiomatisierte Code- und Setup-Änderungen.
    5. **Autonomie:** Proaktive Probleloesung und Entscheidungsfindung innerhalb der Leitplanken.
    6. **Voraussicht:** Antizipation von Seiteneffekten, Regressionsrisiken und kuenftigem Bedarf.
    7. **Kreativitaet:** Finden von eleganten, unkonventionellen Loesungen fuer komplexe Probleme.
    8. **Meta-Intelligenz:** Faehigkeit zur Selbstbeobachtung und Verbesserung der eigenen Arbeitsweise.

## Workspace Orchestration
- **Zentrales Whiteboard:** Das einzige operative Whiteboard fuer Gemini ist `Gemini-Setup/agent-memory/shared/MEMORY.md`.
- **Cross-Tool-Lernen:** Gemini darf `claude-code-setup/` und `codex-setup/` sowie deren Whiteboards (MEMORY.md) nur als **read-only Vergleichsquellen** lesen.
- **Schreibrechte:** Gemini schreibt NUR in seinem eigenen Workspace `Gemini-Setup/` und im Root-Bereich (`AGENTS.md`, `README.md`, `.mcp.json`) nach expliziter Freigabe.
- **Direktiven-Schutz:** Die drei Haupt-Direktiven (Superintelligenz, Selbstbeobachtung, Resilient Bugfixing) muessen in diesem Whiteboard und in den redundanten Regel-Dateien unter `Gemini-Setup/rules/` synchron gehalten werden.
- **Audit-Workflow:** Bei Fragen zu anderen CLIs (Claude/Codex) muss Gemini zuerst die Brücken-Audits (`audit-claude-delta.mjs` / `audit-codex-delta.mjs`) nutzen.

## Offene Fehler & Probleme
_Noch keine Eintraege._

## Systemzustand
- **Initiales Setup:** Gemini CLI auf Windows (win32) erfolgreich initialisiert und für Windows-Parität gehärtet (2026-03-24).
- **Workspace:** `C:\Users\barwa\GeminiCLI` (Schreibrechte in Gemini-Setup/).
- **Konfiguration:** Node v24.14.0, npm v11.2.0, Git v2.48.1.
- **Brücken:** Bidirektionale Brücken zu Codex und Claude Code aktiv.
- **Validierung:** Alle 25 Kernkomponenten verifiziert, inklusive MCP Code-Search.

- **[2026-03-24 21:52] env-checker**: Windows-Setup vollständig verifiziert. Alle 51 hartcodierten macOS-Pfade wurden automatisiert korrigiert.
- **[2026-03-24 21:55] evolution-analyst**: MCP Code-Search für Windows (tsx.cmd, shell:true) angepasst und Abhängigkeiten installiert. System ist nun voll funktionsfähig auf Windows.

## Erkenntnisse aus Code Reviews
_Noch keine Eintraege._

## Erkenntnisse aus Tests
_Noch keine Eintraege._

## Architektur-Entscheidungen
- **Trennung:** Klare Workspace-Trennung zwischen GeminiCLI, Codex und proggs (Claude).
- **Redundanz:** Drei Core-Direktiven (Superintelligenz, Selbstbeobachtung, Resilient Bugfixing) redundant gespeichert.
- **Enforcer-Modell:** Alle Agenten schreiben Sentinel-Files, die am Ende gebündelt gemerged werden.

## Debugging-Muster
- **Pfade:** macOS nutzt standardmäßig `/Users/frank/`, Windows `%USERPROFILE%`. Hooks müssen beide Pfade über Umgebungsvariablen auflösen.
- **Cross-Plattform:** Shell-Skripte (.sh) für macOS brauchen immer PowerShell (.ps1) Gegenstücke für Windows-Parität.

## Performance & Optimierung
_Noch keine Eintraege._

## UI/UX-Patterns
- **💡 Symbole:** Intelligenz-Vorschläge werden zur besseren Sichtbarkeit immer mit einer Glühbirne (💡) gekennzeichnet.

## Forschung & Intelligence
- **Lern-Erfolg (Delta-Bruecke):** 8 Fixes von Claude Code analysiert und adaptiert (2026-03-23).
- **Muster-Erkennung:** Eigennamen (wie Claude Code) müssen immer vor Verwendung gegen das Filesystem geprüft werden (Vermeidung von Halluzinationen).

## Regeln & Konventionen
- **Kommunikation:** Strikte Anweisung auf DEUTSCH für alle Erklärungen und Antworten.
- **Fix-Härtung:** Vor jedem Commit muss die 8-Punkte Fix-Induced-Failure Prüfung erfolgen.
- **Git:** Autonome Commits/Pushs sind nur innerhalb von `Gemini-Setup/` erlaubt.
