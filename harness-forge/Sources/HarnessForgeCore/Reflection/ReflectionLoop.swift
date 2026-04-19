// ReflectionLoop.swift
// Periodische Selbstreflektion ueber die letzten N Trajektorien.
// Nach jedem Schwellenwert ruft die Loop ein LLM mit einer Zusammenfassung
// der letzten Interaktionen auf und bittet um verallgemeinerbare Lessons.

import Foundation

@MainActor
public final class ReflectionLoop {
    public let threshold: Int

    private let store: TrajectoryStore
    private let lessons: LessonsDB
    private let client: any LLMClient

    public init(
        store: TrajectoryStore,
        lessons: LessonsDB,
        client: any LLMClient,
        threshold: Int = 10
    ) {
        self.store = store
        self.lessons = lessons
        self.client = client
        self.threshold = threshold
    }

    // MARK: - Public API

    public struct Result: Sendable, Equatable {
        public let newLessons: Int
        public let costUSD: Double
        public let rawResponse: String
    }

    /// Analysiert die letzten `threshold` Trajektorien und extrahiert neue Lessons.
    public func reflect() async throws -> Result {
        let recent = Array(try store.listTrajectories().prefix(threshold))
        guard !recent.isEmpty else {
            return Result(newLessons: 0, costUSD: 0, rawResponse: "")
        }

        let summary = recent.map { t in
            let scoreLabel = t.successScore == 0 ? "?" : "\(t.successScore)"
            let cost = String(format: "%.4f", t.totalCostUSD)
            return "- [\(t.harnessType)] \(t.taskDescription) | $\(cost) | \(t.interactionCount) Calls | Score: \(scoreLabel)"
        }.joined(separator: "\n")

        let systemPrompt = """
        Du bist die Reflektions-Instanz von Harness Forge. Du analysierst
        frueherer Trajektorien und extrahierst NUR echte, verallgemeinerbare
        Muster. Einzelfaelle ignorierst du. Antworte knapp und praezise
        auf Deutsch.
        """

        let userPrompt = """
        Hier sind die letzten \(recent.count) Trajektorien:

        \(summary)

        Formuliere 1 bis 3 verallgemeinerbare Lessons im exakten Format:

        TITEL: <kurz>
        SYMPTOM: <was aufgefallen ist>
        URSACHE: <warum>
        ABHILFE: <was aendern>
        TAGS: <kommaseparierte Tags>
        ---

        Wenn nichts Verallgemeinerbares erkennbar ist, antworte mit:
        KEINE_LESSONS
        """

        let response = try await client.send(LLMRequest(
            messages: [.user(userPrompt)],
            systemPrompt: systemPrompt,
            maxTokens: 2000,
            temperature: 0.3
        ))

        if response.content.contains("KEINE_LESSONS") {
            return Result(
                newLessons: 0,
                costUSD: response.usage.estimatedCostUSD,
                rawResponse: response.content
            )
        }

        let parsed = Self.parseLessons(from: response.content)
        for p in parsed {
            try lessons.addLesson(
                title: p.title,
                context: "Auto-Reflektion (Schwellenwert \(threshold))",
                symptom: p.symptom,
                rootCause: p.rootCause,
                mitigation: p.mitigation,
                tags: p.tags
            )
        }

        return Result(
            newLessons: parsed.count,
            costUSD: response.usage.estimatedCostUSD,
            rawResponse: response.content
        )
    }

    // MARK: - Parsing (public nur fuer Tests)

    public struct ParsedLesson: Sendable, Equatable {
        public let title: String
        public let symptom: String
        public let rootCause: String
        public let mitigation: String
        public let tags: [String]
    }

    /// Parst die LLM-Antwort in strukturierte Lessons. Robust gegenueber
    /// Leerzeilen und fehlenden Feldern. `nonisolated`, weil reine pure-function
    /// ohne State — soll aus beliebigen Actor-Contexts aufrufbar sein.
    public nonisolated static func parseLessons(from text: String) -> [ParsedLesson] {
        let blocks = text.components(separatedBy: "---")
        var result: [ParsedLesson] = []

        for block in blocks {
            let trimmed = block.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !trimmed.isEmpty else { continue }

            let lines = trimmed.split(separator: "\n", omittingEmptySubsequences: true)

            func value(for prefix: String) -> String? {
                for line in lines where line.hasPrefix(prefix) {
                    return String(line.dropFirst(prefix.count)).trimmingCharacters(in: .whitespaces)
                }
                return nil
            }

            guard let title = value(for: "TITEL:"),
                  let symptom = value(for: "SYMPTOM:"),
                  let rootCause = value(for: "URSACHE:"),
                  let mitigation = value(for: "ABHILFE:") else {
                continue
            }

            let tags = (value(for: "TAGS:") ?? "")
                .split(separator: ",")
                .map { $0.trimmingCharacters(in: .whitespaces) }
                .filter { !$0.isEmpty }

            result.append(ParsedLesson(
                title: title,
                symptom: symptom,
                rootCause: rootCause,
                mitigation: mitigation,
                tags: tags
            ))
        }

        return result
    }
}
