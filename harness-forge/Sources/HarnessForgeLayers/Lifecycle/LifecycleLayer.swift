// LifecycleLayer.swift
// L5 — Lifecycle: Kosten-Tracking, Loop-Detection, HITL-Gates.
// Als Actor: mutable State (Kosten, Historie) konkurrenzsicher serialisiert.

import Foundation

public actor LifecycleLayer {

    // MARK: - Public Types

    public enum LifecycleError: Error, LocalizedError, Sendable {
        case costCapExceeded(current: Double, cap: Double)
        case loopDetected(similarityScore: Double)

        public var errorDescription: String? {
            switch self {
            case .costCapExceeded(let cur, let cap):
                String(format: "Kosten-Cap ueberschritten: $%.4f > $%.4f", cur, cap)
            case .loopDetected(let score):
                String(format: "Loop erkannt — Aehnlichkeit: %.2f", score)
            }
        }
    }

    public struct Stats: Sendable, Equatable {
        public let totalCostUSD: Double
        public let callCount: Int
        public let recentOutputSamples: Int
    }

    // MARK: - Config

    public let costCapUSD: Double
    public let loopWindow: Int
    public let loopSimilarityThreshold: Double

    // MARK: - State

    private(set) var totalCostUSD: Double = 0
    private(set) var callCount: Int = 0
    private var recentOutputs: [String] = []

    public init(
        costCapUSD: Double = 10.0,
        loopWindow: Int = 5,
        loopSimilarityThreshold: Double = 0.85
    ) {
        self.costCapUSD = costCapUSD
        self.loopWindow = loopWindow
        self.loopSimilarityThreshold = loopSimilarityThreshold
    }

    // MARK: - Kosten

    /// Addiert Delta, wirft wenn das Cap ueberschritten wird.
    public func trackCost(_ delta: Double) throws {
        totalCostUSD += delta
        callCount += 1
        if totalCostUSD > costCapUSD {
            throw LifecycleError.costCapExceeded(current: totalCostUSD, cap: costCapUSD)
        }
    }

    public func currentStats() -> Stats {
        Stats(
            totalCostUSD: totalCostUSD,
            callCount: callCount,
            recentOutputSamples: recentOutputs.count
        )
    }

    public func reset() {
        totalCostUSD = 0
        callCount = 0
        recentOutputs = []
    }

    // MARK: - Loop-Detection

    /// Fuegt Output ins Fenster und prueft, ob zu viele aehnliche Outputs in Folge kamen.
    /// Wirft `loopDetected`, wenn mindestens zwei Outputs im Fenster eine Jaccard-Aehnlichkeit
    /// >= `loopSimilarityThreshold` zum neuen Output haben.
    public func checkLoop(with newOutput: String) throws {
        defer {
            recentOutputs.append(newOutput)
            if recentOutputs.count > loopWindow {
                recentOutputs.removeFirst(recentOutputs.count - loopWindow)
            }
        }

        let newTokens = Self.tokenize(newOutput)
        guard !newTokens.isEmpty else { return }

        var similarCount = 0
        var maxScore: Double = 0
        for old in recentOutputs {
            let oldTokens = Self.tokenize(old)
            let score = Self.jaccard(newTokens, oldTokens)
            maxScore = max(maxScore, score)
            if score >= loopSimilarityThreshold {
                similarCount += 1
            }
        }

        if similarCount >= 2 {
            throw LifecycleError.loopDetected(similarityScore: maxScore)
        }
    }

    // MARK: - Helfer

    nonisolated static func tokenize(_ s: String) -> Set<String> {
        Set(s.lowercased()
            .components(separatedBy: CharacterSet.alphanumerics.inverted)
            .filter { !$0.isEmpty })
    }

    nonisolated static func jaccard(_ a: Set<String>, _ b: Set<String>) -> Double {
        let intersection = a.intersection(b).count
        let union = a.union(b).count
        return union == 0 ? 0 : Double(intersection) / Double(union)
    }
}
