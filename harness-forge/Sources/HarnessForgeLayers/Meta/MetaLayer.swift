// MetaLayer.swift
// Meta — Hyperagent: Drift-Monitoring, Skill-Extraktion.
// Nur reine statische Funktionen — kein Zustand, deshalb kein Actor noetig.

import Foundation

public enum MetaLayer {

    // MARK: - Drift-Erkennung

    public struct DriftReport: Sendable, Equatable {
        public let originalGoal: String
        public let currentFocus: String
        public let driftScore: Double

        public var isDrifting: Bool {
            driftScore > 0.6
        }

        public init(originalGoal: String, currentFocus: String, driftScore: Double) {
            self.originalGoal = originalGoal
            self.currentFocus = currentFocus
            self.driftScore = driftScore
        }
    }

    /// Einfache Token-Ueberlappung zwischen Ziel und aktuellem Fokus.
    /// 0.0 = identisch, 1.0 = komplett unterschiedlich.
    public static func detectDrift(originalGoal: String, currentFocus: String) -> DriftReport {
        let goalTokens = Self.tokens(from: originalGoal)
        let focusTokens = Self.tokens(from: currentFocus)

        guard !goalTokens.isEmpty && !focusTokens.isEmpty else {
            return DriftReport(originalGoal: originalGoal, currentFocus: currentFocus, driftScore: 0)
        }

        let intersection = goalTokens.intersection(focusTokens).count
        let union = goalTokens.union(focusTokens).count
        let similarity = union > 0 ? Double(intersection) / Double(union) : 1.0
        return DriftReport(
            originalGoal: originalGoal,
            currentFocus: currentFocus,
            driftScore: 1.0 - similarity
        )
    }

    // MARK: - Skill-Kandidaten

    public struct SkillCandidate: Sendable, Equatable {
        public let sequence: [String]
        public let occurrences: Int

        public init(sequence: [String], occurrences: Int) {
            self.sequence = sequence
            self.occurrences = occurrences
        }
    }

    /// Findet wiederkehrende Tool-Call-Sequenzen der Laenge `length`.
    /// Sequenzen, die mindestens `minOccurrences` mal auftauchen, werden
    /// als Skill-Kandidaten zurueckgegeben.
    public static func extractSkillCandidates(
        from toolCallSequences: [[String]],
        length: Int = 3,
        minOccurrences: Int = 2
    ) -> [SkillCandidate] {
        guard length > 0 else { return [] }

        var counts: [[String]: Int] = [:]
        for seq in toolCallSequences where seq.count >= length {
            for start in 0...(seq.count - length) {
                let subseq = Array(seq[start..<(start + length)])
                counts[subseq, default: 0] += 1
            }
        }

        return counts
            .filter { $0.value >= minOccurrences }
            .map { SkillCandidate(sequence: $0.key, occurrences: $0.value) }
            .sorted { $0.occurrences > $1.occurrences }
    }

    // MARK: - Helfer

    private static func tokens(from text: String) -> Set<String> {
        Set(text.lowercased()
            .components(separatedBy: CharacterSet.alphanumerics.inverted)
            .filter { $0.count >= 4 })
    }
}
