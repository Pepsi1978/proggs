// ReadmeTemplate.swift
// README fuer generierte Pure-Prompt-Harnesses.

import Foundation

public enum ReadmeTemplate {

    public static func render(
        taskDescription: String,
        slug: String,
        decomposition: TaskDecomposition,
        date: Date = Date()
    ) -> String {
        """
        # \(slug)

        > **Automatisch generierter Pure-Prompt-Harness**
        > Erstellt von Harness Forge am \(BuilderHelpers.isoDate(date))

        ## Was ist das?

        Dieser Ordner enthaelt einen optimierten System-Prompt fuer folgende Aufgabe:

        > \(taskDescription)

        ## Benutzung

        1. Oeffne `SYSTEM_PROMPT.md`.
        2. Kopiere den gesamten Inhalt in deine KI-Anwendung (Claude, ChatGPT, Gemini, …)
           als System-Prompt.
        3. Fuehre deine Konversation.

        ## Einordnung

        Die Aufgabe wurde vom TaskAnalyzer entlang dieser 6 Achsen eingeordnet:

        - **Umgebung**: `\(decomposition.targetEnvironment.rawValue)`
        - **Interaktivitaet**: `\(decomposition.interactivity.rawValue)`
        - **Verifizierbarkeit**: `\(decomposition.verifiability.rawValue)`
        - **Datenquellen**: `\(decomposition.dataSources.rawValue)`
        - **Zielnutzer**: `\(decomposition.targetUsers.rawValue)`
        - **Offline**: `\(decomposition.offlineCapability.rawValue)`

        **Begruendung**: \(decomposition.rationale)

        ## Anpassungen

        Der Prompt ist ein deterministisch generierter Startpunkt. Du darfst ihn
        beliebig anpassen. Wenn du das System-Prompt ueber Harness-Forge-Versioning
        verfolgen moechtest, commite Aenderungen in `.forge/prompts.git/` (siehe
        Haupt-Projekt).

        ## Lizenz

        MIT (oder wie von dir angepasst).

        ---

        *Teil des Mono-Repos `Pepsi1978/proggs`.*

        """
    }
}
