// Interaction.swift
// SwiftData-Model fuer einen einzelnen LLM-Aufruf innerhalb einer Trajectory.

import Foundation
import SwiftData

/// Ein einzelner LLM-Aufruf: welche Backend, welcher Prompt, welche Antwort,
/// wie viele Tokens, Kosten, Latenz, Erfolg.
@Model
public final class Interaction {
    @Attribute(.unique) public var id: UUID
    public var timestamp: Date
    public var backendIdentifier: String
    public var modelName: String
    /// Prompt-Inhalt (kann bei Bedarf gekuerzt oder gehasht werden).
    public var promptContent: String
    public var responseContent: String
    public var inputTokens: Int
    public var outputTokens: Int
    public var costUSD: Double
    public var latencyMs: Int
    public var success: Bool
    public var errorMessage: String?

    public var trajectory: Trajectory?

    public init(
        id: UUID = UUID(),
        backendIdentifier: String,
        modelName: String,
        promptContent: String,
        responseContent: String,
        inputTokens: Int,
        outputTokens: Int,
        costUSD: Double,
        latencyMs: Int,
        success: Bool,
        errorMessage: String? = nil
    ) {
        self.id = id
        self.timestamp = Date()
        self.backendIdentifier = backendIdentifier
        self.modelName = modelName
        self.promptContent = promptContent
        self.responseContent = responseContent
        self.inputTokens = inputTokens
        self.outputTokens = outputTokens
        self.costUSD = costUSD
        self.latencyMs = latencyMs
        self.success = success
        self.errorMessage = errorMessage
    }
}
