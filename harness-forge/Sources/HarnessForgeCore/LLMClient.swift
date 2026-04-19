// LLMClient.swift
// Einheitliche Schnittstelle zu allen KI-Backends (Anthropic, OpenAI, Gemini, LM Studio).
// Alle Backends implementieren dieses Protokoll. Der Router waehlt das passende.

import Foundation

// MARK: - Haupt-Protokoll

/// Einheitliche Schnittstelle zu einem KI-Backend.
///
/// Jedes Backend (Anthropic, OpenAI, Gemini, LM Studio, …) implementiert dieses
/// Protokoll. Der `Router` kennt alle registrierten Backends und waehlt das
/// passende fuer eine gegebene Aufgabe aus.
public protocol LLMClient: Sendable {
    /// Eindeutiger Identifier, z.B. "anthropic", "openai", "gemini", "lmstudio".
    var identifier: String { get }

    /// Modellname innerhalb des Backends, z.B. "claude-opus-4-7".
    var modelName: String { get }

    /// Ungefaehre Kosten pro 1 Mio Input-Tokens in USD.
    var costPerMillionInputTokens: Double { get }

    /// Ungefaehre Kosten pro 1 Mio Output-Tokens in USD.
    var costPerMillionOutputTokens: Double { get }

    /// Maximale Kontext-Laenge in Tokens.
    var maxContextTokens: Int { get }

    /// Unterstuetzt dieses Backend Tool-Use (Function-Calling)?
    var supportsTools: Bool { get }

    /// Sendet eine Konversation und liefert die Antwort zurueck.
    func send(_ request: LLMRequest) async throws -> LLMResponse
}

// MARK: - Request-Modell

/// Eine abgeschlossene Anfrage an ein LLM-Backend.
public struct LLMRequest: Sendable {
    public let messages: [ChatMessage]
    public let systemPrompt: String?
    public let maxTokens: Int
    public let temperature: Double
    public let tools: [ToolDefinition]

    public init(
        messages: [ChatMessage],
        systemPrompt: String? = nil,
        maxTokens: Int = 4096,
        temperature: Double = 0.7,
        tools: [ToolDefinition] = []
    ) {
        self.messages = messages
        self.systemPrompt = systemPrompt
        self.maxTokens = maxTokens
        self.temperature = temperature
        self.tools = tools
    }
}

// MARK: - Tool-Definition

/// Beschreibung eines Werkzeugs, das das Modell aufrufen kann.
public struct ToolDefinition: Sendable, Codable, Equatable {
    public let name: String
    public let description: String
    /// JSON-Schema der Parameter, als String (wird pro Backend serialisiert).
    public let parametersJSONSchema: String

    public init(name: String, description: String, parametersJSONSchema: String) {
        self.name = name
        self.description = description
        self.parametersJSONSchema = parametersJSONSchema
    }
}

// MARK: - Fehler

/// Alle moeglichen Fehler, die beim LLM-Aufruf auftreten koennen.
public enum LLMError: Error, LocalizedError, Sendable {
    case missingAPIKey(backend: String)
    case invalidURL(String)
    case httpError(statusCode: Int, body: String)
    case decodingError(String)
    case networkError(String)
    case rateLimitExceeded(retryAfter: TimeInterval?)
    case contextTooLong(limit: Int)
    case unknownBackend(identifier: String)

    public var errorDescription: String? {
        switch self {
        case .missingAPIKey(let backend):
            return "Kein API-Key im Keychain fuer Backend '\(backend)' hinterlegt."
        case .invalidURL(let url):
            return "Ungueltige URL: \(url)"
        case .httpError(let code, let body):
            return "HTTP \(code): \(body.prefix(200))"
        case .decodingError(let msg):
            return "Antwort konnte nicht geparst werden: \(msg)"
        case .networkError(let msg):
            return "Netzwerk-Fehler: \(msg)"
        case .rateLimitExceeded(let retry):
            if let retry {
                return "Rate-Limit erreicht, bitte in \(Int(retry))s erneut versuchen."
            }
            return "Rate-Limit erreicht."
        case .contextTooLong(let limit):
            return "Anfrage ueberschreitet Kontext-Limit von \(limit) Tokens."
        case .unknownBackend(let id):
            return "Unbekanntes Backend: \(id)"
        }
    }
}
