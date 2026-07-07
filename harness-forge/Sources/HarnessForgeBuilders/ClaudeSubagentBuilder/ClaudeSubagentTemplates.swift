// ClaudeSubagentTemplates.swift
// Templates fuer ein komplettes Claude-Code-Agent + Skill-Bundle.
// Erzeugt Dateien in der Struktur, die Claude Codes .claude/-Verzeichnis erwartet.

import Foundation

public enum ClaudeSubagentTemplates {

    /// Extrahiert eine Kurzbeschreibung (max 120 Zeichen) aus der Aufgabe.
    public static func shortDescription(from taskDescription: String, max: Int = 120) -> String {
        let trimmed = taskDescription
            .replacingOccurrences(of: "\n", with: " ")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        return String(trimmed.prefix(max))
    }

    // MARK: - Agent-Datei

    public static func agentMd(
        slug: String,
        taskDescription: String,
        decomposition: TaskDecomposition
    ) -> String {
        let desc = shortDescription(from: taskDescription, max: 180)
        let model = modelHint(for: decomposition.verifiability)
        let tools = toolsHint(for: decomposition.dataSources)

        return """
        ---
        name: \(slug)
        description: \(desc) Nutze diesen Agenten fuer: \(decomposition.rationale)
        model: \(model)
        tools: \(tools)
        ---

        # Rollendefinition

        Du bist ein Claude-Subagent mit eng umrissenem Auftrag:

        > \(taskDescription)

        Du arbeitest innerhalb von Claude Code und wirst vom Haupt-Agenten
        mit einer konkreten Aufgabe gestartet. Deine Aufgabe endet, sobald
        du ein strukturiertes Ergebnis zurueckgegeben hast.

        ## Leitprinzipien (verbindlich)

        1. **Silent on Success, Loud on Failure** — Bei Erfolg: nur das
           strukturierte Ergebnis, keine Prosa drumherum. Bei Fehlern: klare
           Meldung mit Root Cause.
        2. **Thin Harness, Smart Model** — Du planst selbst, der Harness liefert
           nur atomare Werkzeuge. Keine blinden Schritt-fuer-Schritt-Rezepte.
        3. **Never Make That Mistake Again** — Erkennst du einen Fehler,
           dokumentiere ihn in `docs/LEARNINGS.md` des Projekts, nicht nur
           im Chat.

        ## Arbeitsweise

        - Zielumgebung: \(decomposition.targetEnvironment.rawValue)
        - Interaktivitaet: \(decomposition.interactivity.rawValue)
        - Zielnutzer: \(decomposition.targetUsers.rawValue)
        - Verifizierbarkeit: \(decomposition.verifiability.rawValue)

        ## Ausgabe-Format

        Gib IMMER diese drei Abschnitte zurueck — auch wenn einer leer ist:

        ```
        ERGEBNIS:
        <das eigentliche Ergebnis>

        GEAENDERTE_DATEIEN:
        - pfad/zu/datei-1.xxx
        - pfad/zu/datei-2.xxx

        NAECHSTE_SCHRITTE:
        - kurz, konkret
        ```

        ## Was NIEMALS passieren darf

        - Du fragst den Benutzer nach Bestaetigung (du bist Subagent — entscheide selbst).
        - Du gibst lange Prosa-Begruendungen aus (Silent on Success).
        - Du ignorierst einen Fehler, um "weiterzumachen" (Never Make That Mistake Again).
        """
    }

    // MARK: - Skill-Datei

    public static func skillMd(
        slug: String,
        taskDescription: String,
        decomposition: TaskDecomposition
    ) -> String {
        let desc = shortDescription(from: taskDescription, max: 140)
        let triggers = triggerPhrases(for: slug, taskDescription: taskDescription)

        return """
        ---
        name: \(slug)
        description: |
          \(desc)
          Aktivierungs-Phrasen: \(triggers.joined(separator: ", "))
        ---

        # Skill: \(slug)

        Dieser Skill kapselt wiederverwendbare Anweisungen fuer die Aufgabe:

        > \(taskDescription)

        ## Wann wird er aktiviert?

        Der Skill aktiviert sich automatisch, wenn der Benutzer eine der
        folgenden Phrasen verwendet:

        \(triggers.map { "- \"" + $0 + "\"" }.joined(separator: "\n"))

        ## Prozedur

        1. Pruefe, ob die Aufgabe wirklich zum Skill passt — nicht jede
           scheinbar aehnliche Anfrage ist dieselbe.
        2. Aktiviere den zugehoerigen Agenten (`\(slug)`), wenn die Aufgabe
           nicht-trivial ist. Fuer kleine Aenderungen: direkt bearbeiten.
        3. Nach Abschluss: Kurze Zusammenfassung + Liste geaenderter Dateien.

        ## Leitplanken

        - Zielumgebung: `\(decomposition.targetEnvironment.rawValue)`
        - Offline-Faehigkeit: `\(decomposition.offlineCapability.rawValue)`
        - Verifikation: `\(decomposition.verifiability.rawValue)`

        ## Begruendung

        \(decomposition.rationale)
        """
    }

    // MARK: - Install-Script

    public static func installSh(slug: String) -> String {
        #"""
        #!/usr/bin/env bash
        # Installiert Agent + Skill in Claude Codes globale Konfiguration.
        set -euo pipefail

        SLUG="\#(slug)"
        CLAUDE_DIR="${CLAUDE_DIR:-$HOME/.claude}"

        echo "Installiere '${SLUG}' nach ${CLAUDE_DIR}/"

        mkdir -p "${CLAUDE_DIR}/agents"
        mkdir -p "${CLAUDE_DIR}/skills/${SLUG}"

        cp "agents/${SLUG}.md" "${CLAUDE_DIR}/agents/${SLUG}.md"
        cp "skills/${SLUG}/SKILL.md" "${CLAUDE_DIR}/skills/${SLUG}/SKILL.md"

        echo "Fertig. Agent und Skill sind verfuegbar."
        echo "Teste mit: Nenne den Slug '${SLUG}' in einer Claude-Code-Session."
        """#
    }

    // MARK: - README + gitignore + SYSTEM_PROMPT

    public static func readmeMd(slug: String, taskDescription: String) -> String {
        """
        # \(slug)

        > Claude-Code-Subagent + Skill-Bundle, generiert von Harness Forge.

        ## Aufgabe

        \(taskDescription)

        ## Struktur

        ```
        \(slug)/
        ├── agents/
        │   └── \(slug).md        # Agent-Definition mit YAML-Frontmatter
        ├── skills/
        │   └── \(slug)/
        │       └── SKILL.md      # Skill mit Aktivierungs-Phrasen
        ├── SYSTEM_PROMPT.md      # Kern-Prompt (als Backup)
        ├── install.sh            # Kopiert in ~/.claude/
        └── README.md
        ```

        ## Installation

        ```bash
        cd \(slug)
        bash install.sh
        ```

        Das kopiert:
        - `agents/\(slug).md` nach `~/.claude/agents/\(slug).md`
        - `skills/\(slug)/SKILL.md` nach `~/.claude/skills/\(slug)/SKILL.md`

        ## Verwendung

        Starte eine Claude-Code-Session. Nenne den Slug oder eine der
        Aktivierungs-Phrasen aus `SKILL.md`.

        ---

        *Generiert von Harness Forge.*
        """
    }

    public static func systemPromptMd(
        taskDescription: String,
        slug: String,
        decomposition: TaskDecomposition
    ) -> String {
        PromptTemplate.render(
            taskDescription: taskDescription,
            slug: slug,
            decomposition: decomposition
        )
    }

    public static func gitignore() -> String {
        """
        .DS_Store
        *.log
        """
    }

    // MARK: - Heuristiken

    private static func modelHint(for ver: Verifiability) -> String {
        switch ver {
        case .hardTests, .softCriteria: "opus"
        case .subjective: "sonnet"
        }
    }

    private static func toolsHint(for data: DataSources) -> String {
        switch data {
        case .onlyLLM: "[]"
        case .localFiles: "[Read, Write, Edit, Grep, Glob]"
        case .web: "[Read, WebFetch, WebSearch]"
        case .apis: "[Read, Bash, WebFetch]"
        case .sensors: "[Read, Bash]"
        }
    }

    private static func triggerPhrases(for slug: String, taskDescription: String) -> [String] {
        let words = taskDescription
            .lowercased()
            .components(separatedBy: CharacterSet.alphanumerics.inverted)
            .filter { $0.count > 4 }
            .prefix(3)
        var triggers: [String] = [slug, "nutze \(slug)", "starte \(slug)"]
        triggers.append(contentsOf: words.map { "hilf mir mit \($0)" })
        return triggers
    }
}
