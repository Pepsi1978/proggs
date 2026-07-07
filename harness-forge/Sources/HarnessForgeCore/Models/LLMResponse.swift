// LLMResponse.swift
// Model fuer die Antwort eines LLM-Backends, inklusive Metriken.

import Foundation

/// Antwort eines LLM-Backends.
public struct LLMResponse: Sendable, Equatable {
    public let content: String
    public let toolCalls: [ToolCall]
    public let stopReason: StopReason
    public let usage: TokenUsage
    public let metadata: Metadata

    public init(
        content: String,
        toolCalls: [ToolCall] = [],
        stopReason: StopReason,
        usage: TokenUsage,
        metadata: Metadata
    ) {
        self.content = content
        self.toolCalls = toolCalls
        self.stopReason = stopReason
        self.usage = usage
        self.metadata = metadata
    }

    /// Grund, warum das Modell die Antwort beendet hat.
    public enum StopReason: Sendable, Equatable {
        case endTurn
        case maxTokens
        case toolUse
        case stopSequence
        case other(String)
    }

    /// Token-Verbrauch und geschaetzte Kosten in USD.
    public struct TokenUsage: Sendable, Equatable {
        public let inputTokens: Int
        public let outputTokens: Int
        public let estimatedCostUSD: Double

        public init(inputTokens: Int, outputTokens: Int, estimatedCostUSD: Double) {
            self.inputTokens = inputTokens
            self.outputTokens = outputTokens
            self.estimatedCostUSD = estimatedCostUSD
        }

        public static let zero = TokenUsage(inputTokens: 0, outputTokens: 0, estimatedCostUSD: 0)
    }

    /// Metadaten ueber den Aufruf selbst (wann, wo, wie lange).
    public struct Metadata: Sendable, Equatable {
        public let backendIdentifier: String
        public let modelName: String
        public let latencyMs: Int
        public let timestamp: Date

        public init(backendIdentifier: String, modelName: String, latencyMs: Int, timestamp: Date) {
            self.backendIdentifier = backendIdentifier
            self.modelName = modelName
            self.latencyMs = latencyMs
            self.timestamp = timestamp
        }
    }
}
