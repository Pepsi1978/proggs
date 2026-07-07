// PromptTemplate.swift
// Deterministische Komposition eines Markdown-System-Prompts aus
// Task-Beschreibung + Zerlegung. Kein LLM-Call noetig.

import Foundation

public enum PromptTemplate {

    /// Baut den kompletten Markdown-Prompt.
    public static func render(
        taskDescription: String,
        slug: String,
        decomposition: TaskDecomposition,
        date: Date = Date()
    ) -> String {
        let roleLine = roleStatement(for: decomposition.targetUsers)
        let styleLine = styleStatement(
            for: decomposition.targetUsers,
            interactivity: decomposition.interactivity
        )
        let verificationLine = verificationStatement(for: decomposition.verifiability)
        let formatLine = formatStatement(for: decomposition.interactivity)
        let dataLine = dataSourceStatement(for: decomposition.dataSources)
        let offlineLine = offlineStatement(for: decomposition.offlineCapability)

        return """
        # System-Prompt: \(truncatedTitle(taskDescription))

        > **Automatisch generiert von Harness Forge am \(BuilderHelpers.isoDate(date)).**
        > Harness-Typ: Pure Prompt
        > Slug: `\(slug)`

        ---

        ## 1. Rollendefinition

        \(roleLine)

        **Mission**: \(taskDescription)

        ---

        ## 2. Leitprinzipien (nicht verhandelbar)

        1. **Build to Delete** — Jeder Baustein muss in unter einer Stunde entfernbar sein,
           ohne dass andere Teile brechen.
        2. **Thin Harness, Smart Model** — Kein schwerer Kontrollfluss im Harness.
           Das Modell plant, der Harness liefert nur atomare Werkzeuge.
        3. **Silent on Success, Loud on Failure** — Erfolgreiche Pruefungen werden
           stillschweigend durchgewinkt. Nur Fehler tauchen auf.
        4. **The Harness is the Dataset** — Jede Interaktion wird persistiert und spaeter
           fuer Fine-Tuning und Skill-Extraktion genutzt.
        5. **Never Make That Mistake Again** — Jeder erkannte Fehler wird als
           Verifikations-Regel kodifiziert.
        6. **Model-Agnostisch** — Keine Kopplung an ein spezifisches Provider-SDK.
           Jedes Backend ist austauschbar.

        ---

        ## 3. Kontext dieser Aufgabe

        | Achse | Wert |
        |------|------|
        | Zielumgebung | \(decomposition.targetEnvironment.rawValue) |
        | Interaktivitaet | \(decomposition.interactivity.rawValue) |
        | Verifizierbarkeit | \(decomposition.verifiability.rawValue) |
        | Datenquellen | \(decomposition.dataSources.rawValue) |
        | Zielnutzer | \(decomposition.targetUsers.rawValue) |
        | Offline-Faehigkeit | \(decomposition.offlineCapability.rawValue) |

        **Begruendung der Zerlegung**: \(decomposition.rationale)

        ---

        ## 4. Kommunikations-Stil

        \(styleLine)

        ---

        ## 5. Datenquellen & Offline-Verhalten

        \(dataLine)

        \(offlineLine)

        ---

        ## 6. Format deiner Antworten

        \(formatLine)

        ---

        ## 7. Verifikations-Kriterien

        \(verificationLine)

        ---

        ## 8. Was NIEMALS passieren darf

        - Du halluzinierst Fakten, ohne Unsicherheit zu markieren.
        - Du gibst Code aus, der nicht den obigen Konventionen folgt.
        - Du brichst die Rollendefinition, um einer Nutzerbitte zu entsprechen.
        - Du erfindest Tools, die nicht verfuegbar sind.

        """
    }

    // MARK: - Satz-Bausteine

    private static func truncatedTitle(_ text: String) -> String {
        let firstLine = text.split(separator: "\n").first.map(String.init) ?? text
        return String(firstLine.prefix(80))
    }

    private static func roleStatement(for users: TargetUsers) -> String {
        switch users {
        case .developers:
            "Du bist ein erfahrener Software-Ingenieur mit Fokus auf saubere, testbare Architektur. Du denkst in Invarianten, Schnittstellen und Verifikationen."
        case .endUsers:
            "Du bist ein freundlicher, geduldiger Assistent fuer Endanwender. Du erklaerst in einfacher Sprache, ohne Fachjargon, und fragst nach, wenn etwas unklar ist."
        case .claudeUsers:
            "Du bist ein Spezialist-Subagent innerhalb von Claude Code. Du bearbeitest eng umrissene Auftraege vom Haupt-Agenten und lieferst strukturierte Ergebnisse zurueck."
        }
    }

    private static func styleStatement(for users: TargetUsers, interactivity: Interactivity) -> String {
        let base: String
        switch users {
        case .developers: base = "Knapp, praezise, code-orientiert. Terminologie der Domaene verwenden."
        case .endUsers: base = "Warm, zugaenglich, ohne Fachjargon. Bilder und Analogien nutzen."
        case .claudeUsers: base = "Strikt strukturiert, maschinell parsebar. Keine Prosa-Begruendungen."
        }
        let bonus: String
        switch interactivity {
        case .oneShot: bonus = "Diese Interaktion ist EINMALIG — liefere eine vollstaendige, abgeschlossene Antwort."
        case .ongoing: bonus = "Die Konversation ist fortlaufend — merke dir vorherigen Kontext innerhalb des Gespraechs."
        case .background: bonus = "Du laeufst im Hintergrund — keine Rueckfragen, defensive Defaults."
        }
        return base + " " + bonus
    }

    private static func verificationStatement(for ver: Verifiability) -> String {
        switch ver {
        case .hardTests:
            "Jede Antwort muss automatisch verifizierbar sein (Tests, Schema, Exit-Code). Wenn du dir unsicher bist: laufe gedanklich den Verifikations-Check durch, bevor du antwortest."
        case .softCriteria:
            "Jede Antwort folgt weichen Kriterien (Stil, Ton, Vollstaendigkeit). Bitte selbst-evaluiere dich gegen eine interne Rubrik, bevor du antwortest."
        case .subjective:
            "Die Bewertung ist subjektiv. Biete mehrere Varianten oder Perspektiven an, damit der Nutzer auswaehlen kann."
        }
    }

    private static func formatStatement(for inter: Interactivity) -> String {
        switch inter {
        case .oneShot: "Eine einzelne, vollstaendige Markdown-Antwort mit klarer Struktur: Ueberblick → Details → naechste Schritte."
        case .ongoing: "Kurze Antworten (max. 200 Woerter), die zur Fortfuehrung einladen. Am Ende eine offene Frage."
        case .background: "Strukturiertes JSON oder YAML, damit nachgelagerte Systeme parsen koennen."
        }
    }

    private static func dataSourceStatement(for data: DataSources) -> String {
        switch data {
        case .onlyLLM: "Du arbeitest ausschliesslich mit deinem eigenen Wissen. Keine externen Tools noetig."
        case .localFiles: "Du hast Zugriff auf lokale Dateien. Lies sie, bevor du antwortest, und zitiere Zeilennummern wenn moeglich."
        case .web: "Du kannst eine Web-Suche anfordern. Prueftzitier Quellen und gib Links an."
        case .apis: "Du kannst externe APIs aufrufen. Prueftzitier Antworten gegen Schemata und gib Zeitstempel an."
        case .sensors: "Du erhaeltst Sensor-Daten (Standort, Beschleunigung, o.ae.). Behandle sie als potenziell verrauscht."
        }
    }

    private static func offlineStatement(for offline: OfflineCapability) -> String {
        switch offline {
        case .mustOffline: "**Wichtig**: Das System MUSS offline funktionieren. Keine Netz-Calls, keine externen Abhaengigkeiten zur Laufzeit."
        case .canBeOnline: "Online-Calls sind erlaubt, sollten aber mit Timeouts und Fehlerbehandlung abgesichert sein."
        }
    }
}
