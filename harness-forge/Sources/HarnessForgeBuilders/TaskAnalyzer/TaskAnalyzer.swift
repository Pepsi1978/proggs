// TaskAnalyzer.swift
// Herzstueck von Harness Forge.
//
// Pipeline (4 Stufen):
//   1) Zerlegung entlang der 6 Achsen  (1 LLM-Call, strikt parsebarer Prompt)
//   2) Optional: Web-Recherche          (0+ Tool-Calls an WebSearchTool)
//   3) Decision-Matrix                  (deterministisch, kein LLM)
//   4) Empfehlung + Begruendung         (deterministisch, kein LLM)
//
// Dadurch ist der Analyzer mit nur einem einzigen LLM-Call kostenguenstig —
// die Entscheidung selbst ist nachvollziehbar und nachrechenbar.

import Foundation
import HarnessForgeCore

public actor TaskAnalyzer {

    // MARK: - Ergebnis-Typen

    public struct AnalysisResult: Sendable, Equatable {
        public let taskDescription: String
        public let slug: String
        public let decomposition: TaskDecomposition
        public let researchFindings: [WebSearchResult]
        public let matrix: DecisionMatrix
        public let recommendation: HarnessType
        public let rationale: String
        public let totalCostUSD: Double
    }

    public enum AnalyzerError: Error, LocalizedError, Sendable {
        case invalidLLMResponse(String)

        public var errorDescription: String? {
            switch self {
            case .invalidLLMResponse(let text):
                "LLM-Antwort entspricht nicht dem erwarteten Format:\n\(text.prefix(200))"
            }
        }
    }

    // MARK: - Abhaengigkeiten

    private let client: any LLMClient
    private let webSearch: any WebSearchTool

    public init(
        client: any LLMClient,
        webSearch: any WebSearchTool = NoWebSearchTool()
    ) {
        self.client = client
        self.webSearch = webSearch
    }

    // MARK: - Haupt-API

    public func analyze(
        taskDescription: String,
        enableWebSearch: Bool = false,
        existingSlugs: Set<String> = []
    ) async throws -> AnalysisResult {
        let baseSlug = SlugGenerator.generate(from: taskDescription)
        let slug = SlugGenerator.ensureUnique(base: baseSlug, existing: existingSlugs)

        // Stage 1: LLM-basierte Zerlegung
        let decompResponse = try await runDecomposition(taskDescription: taskDescription)
        let decomposition = try Self.parseDecomposition(decompResponse.content)

        // Stage 2: Optional Web-Recherche
        var findings: [WebSearchResult] = []
        if enableWebSearch {
            let query = "harness agent patterns best practices \(decomposition.targetEnvironment.rawValue)"
            findings = (try? await webSearch.search(query: query, maxResults: 5)) ?? []
        }

        // Stage 3: Decision-Matrix (deterministisch)
        let matrix = DecisionMatrix.build(for: taskDescription, decomposition: decomposition)

        // Stage 4: Begruendung (deterministisch)
        let rationale = matrix.generateRationale(decomposition: decomposition)

        return AnalysisResult(
            taskDescription: taskDescription,
            slug: slug,
            decomposition: decomposition,
            researchFindings: findings,
            matrix: matrix,
            recommendation: matrix.recommended,
            rationale: rationale,
            totalCostUSD: decompResponse.usage.estimatedCostUSD
        )
    }

    // MARK: - Stage 1: Zerlegung

    private func runDecomposition(taskDescription: String) async throws -> LLMResponse {
        let systemPrompt = Self.decompositionSystemPrompt
        let userPrompt = Self.decompositionUserPrompt(for: taskDescription)

        return try await client.send(LLMRequest(
            messages: [.user(userPrompt)],
            systemPrompt: systemPrompt,
            maxTokens: 500,
            temperature: 0.1  // deterministisches Format
        ))
    }

    private static let decompositionSystemPrompt = """
    Du bist der TaskAnalyzer von Harness Forge. Dein einziger Job ist es, eine
    Aufgabenbeschreibung in 6 Achsen zu zerlegen und das Ergebnis in einem
    STRIKT vorgegebenen Format zurueckzugeben — ohne Prosa, ohne Erlaeuterung.
    Weicht nie vom Format ab.
    """

    private static func decompositionUserPrompt(for task: String) -> String {
        """
        Zerlege die folgende Aufgabe in 6 Achsen. Antworte EXAKT in diesem Format,
        jede Zeile beginnt mit dem Label, dann Doppelpunkt, dann der Wert.
        Verwende ausschliesslich die in den Klammern aufgefuehrten Werte.

        TARGET_ENV: <mobile|desktop|terminal|cloud|promptOnly|embedded>
        INTERACTIVITY: <oneShot|ongoing|background>
        VERIFIABILITY: <hardTests|softCriteria|subjective>
        DATA_SOURCES: <onlyLLM|localFiles|web|apis|sensors>
        TARGET_USERS: <developers|endUsers|claudeUsers>
        OFFLINE: <mustOffline|canBeOnline>
        RATIONALE: <ein kurzer Satz auf Deutsch>

        Aufgabe: \(task)
        """
    }

    /// Parst die Decomposition-Antwort. Oeffentlich, damit Tests sie ohne
    /// LLM-Call pruefen koennen.
    public static func parseDecomposition(_ text: String) throws -> TaskDecomposition {
        func value(for prefix: String) -> String? {
            for line in text.split(separator: "\n") {
                let trimmedLine = line.trimmingCharacters(in: .whitespaces)
                if trimmedLine.hasPrefix(prefix) {
                    return String(trimmedLine.dropFirst(prefix.count)).trimmingCharacters(in: .whitespaces)
                }
            }
            return nil
        }

        guard
            let envRaw = value(for: "TARGET_ENV:"),
            let env = TargetEnvironment(rawValue: envRaw),
            let interRaw = value(for: "INTERACTIVITY:"),
            let inter = Interactivity(rawValue: interRaw),
            let verRaw = value(for: "VERIFIABILITY:"),
            let ver = Verifiability(rawValue: verRaw),
            let dataRaw = value(for: "DATA_SOURCES:"),
            let data = DataSources(rawValue: dataRaw),
            let usersRaw = value(for: "TARGET_USERS:"),
            let users = TargetUsers(rawValue: usersRaw),
            let offlineRaw = value(for: "OFFLINE:"),
            let offline = OfflineCapability(rawValue: offlineRaw)
        else {
            throw AnalyzerError.invalidLLMResponse(text)
        }

        let rationale = value(for: "RATIONALE:") ?? ""

        return TaskDecomposition(
            targetEnvironment: env,
            interactivity: inter,
            verifiability: ver,
            dataSources: data,
            targetUsers: users,
            offlineCapability: offline,
            rationale: rationale
        )
    }
}
