// DecisionMatrix.swift
// Aus einer TaskDecomposition wird eine 5x6-Matrix: fuenf Harness-Typen
// gegen sechs Achsen. Jede Zelle ist eine Punktzahl 1..5. Die Zeile mit der
// hoechsten Gesamtpunktzahl ist die Empfehlung.

import Foundation

public struct DecisionMatrix: Sendable, Codable, Equatable {

    // MARK: - Sub-Strukturen

    public struct AxisScore: Sendable, Codable, Equatable {
        public let axisName: String
        public let value: String
        public let score: Int

        public init(axisName: String, value: String, score: Int) {
            self.axisName = axisName
            self.value = value
            self.score = score
        }
    }

    public struct Row: Sendable, Codable, Equatable {
        public let harnessType: HarnessType
        public let scores: [AxisScore]
        public let totalScore: Int

        public init(harnessType: HarnessType, scores: [AxisScore], totalScore: Int) {
            self.harnessType = harnessType
            self.scores = scores
            self.totalScore = totalScore
        }
    }

    // MARK: - State

    public let taskDescription: String
    public let rows: [Row]

    public init(taskDescription: String, rows: [Row]) {
        self.taskDescription = taskDescription
        self.rows = rows
    }

    // MARK: - Aufbau

    /// Erzeugt die komplette 5x6-Matrix aus einer Zerlegung.
    public static func build(
        for taskDescription: String,
        decomposition: TaskDecomposition
    ) -> DecisionMatrix {
        let rows = HarnessType.allCases.map { type -> Row in
            let axes = [
                AxisScore(
                    axisName: "Umgebung",
                    value: decomposition.targetEnvironment.rawValue,
                    score: type.score(for: decomposition.targetEnvironment)
                ),
                AxisScore(
                    axisName: "Interaktivitaet",
                    value: decomposition.interactivity.rawValue,
                    score: type.score(for: decomposition.interactivity)
                ),
                AxisScore(
                    axisName: "Verifizierbarkeit",
                    value: decomposition.verifiability.rawValue,
                    score: type.score(for: decomposition.verifiability)
                ),
                AxisScore(
                    axisName: "Datenquellen",
                    value: decomposition.dataSources.rawValue,
                    score: type.score(for: decomposition.dataSources)
                ),
                AxisScore(
                    axisName: "Zielnutzer",
                    value: decomposition.targetUsers.rawValue,
                    score: type.score(for: decomposition.targetUsers)
                ),
                AxisScore(
                    axisName: "Offline",
                    value: decomposition.offlineCapability.rawValue,
                    score: type.score(for: decomposition.offlineCapability)
                ),
            ]
            let total = axes.reduce(0) { $0 + $1.score }
            return Row(harnessType: type, scores: axes, totalScore: total)
        }
        return DecisionMatrix(taskDescription: taskDescription, rows: rows)
    }

    // MARK: - Auswertung

    public var sortedByScore: [Row] {
        rows.sorted { $0.totalScore > $1.totalScore }
    }

    public var recommended: HarnessType {
        sortedByScore.first?.harnessType ?? .purePrompt
    }

    /// Laesst eine menschen-lesbare Markdown-Tabelle entstehen.
    public func prettyPrint() -> String {
        let sorted = sortedByScore
        var lines: [String] = []
        lines.append("| Harness | Umgebung | Inter. | Verif. | Daten | Nutzer | Offline | **Total** |")
        lines.append("|---------|:--------:|:------:|:------:|:-----:|:------:|:-------:|:---------:|")
        for row in sorted {
            let scores = row.scores.map { "\($0.score)" }.joined(separator: " | ")
            lines.append("| \(row.harnessType.displayName) | \(scores) | **\(row.totalScore)** |")
        }
        return lines.joined(separator: "\n")
    }

    /// Baut eine kurze, gebrauchsfertige Begruendung aus Matrix + Zerlegung.
    /// Kein LLM-Call noetig — rein deterministisch aus den Scores berechnet.
    public func generateRationale(decomposition: TaskDecomposition) -> String {
        let sorted = sortedByScore
        guard let winner = sorted.first else { return "Keine Empfehlung moeglich." }

        var text = """
        Empfohlen: \(winner.harnessType.displayName)
        Score: \(winner.totalScore)/30

        Staerken dieser Empfehlung:
        """
        for axis in winner.scores where axis.score >= 4 {
            text += "\n- \(axis.axisName) (\(axis.value)): \(axis.score)/5"
        }

        if let runner = sorted.dropFirst().first {
            let delta = winner.totalScore - runner.totalScore
            text += """


            Alternative: \(runner.harnessType.displayName) (Score \(runner.totalScore)/30, \(delta) Punkte weniger).
            """
        }

        text += """


        Begruendung der Zerlegung: \(decomposition.rationale)
        """

        return text
    }
}
