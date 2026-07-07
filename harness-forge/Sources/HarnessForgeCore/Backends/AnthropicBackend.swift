// AnthropicBackend.swift
// LLMClient-Implementierung fuer Anthropics Claude Messages API.
// Endpoint: https://api.anthropic.com/v1/messages
// API-Version: 2023-06-01

import Foundation

public struct AnthropicBackend: LLMClient {
    public let identifier: String = "anthropic"
    public let modelName: String
    public let costPerMillionInputTokens: Double
    public let costPerMillionOutputTokens: Double
    public let maxContextTokens: Int = 200_000
    public let supportsTools: Bool = true

    private let apiKey: String
    private let baseURL: URL
    private let urlSession: URLSession
    private let apiVersion: String = "2023-06-01"

    public init(
        apiKey: String,
        modelName: String = "claude-opus-4-7",
        baseURL: URL = URL(string: "https://api.anthropic.com/v1/messages")!,
        urlSession: URLSession = .shared
    ) {
        self.apiKey = apiKey
        self.modelName = modelName
        self.baseURL = baseURL
        self.urlSession = urlSession
        // Claude Opus 4.7 Kosten-Schaetzung (USD pro 1M Tokens, Stand 2026)
        self.costPerMillionInputTokens = 15.0
        self.costPerMillionOutputTokens = 75.0
    }

    public func send(_ request: LLMRequest) async throws -> LLMResponse {
        let startTime = Date()

        // Anthropic-spezifisches Message-Format: system separat, keine system-role in messages
        let nonSystemMessages = request.messages.filter { $0.role != .system }
        let messagesArray: [[String: Any]] = nonSystemMessages.map { msg in
            [
                "role": msg.role == .assistant ? "assistant" : "user",
                "content": msg.content,
            ]
        }

        var body: [String: Any] = [
            "model": modelName,
            "messages": messagesArray,
            "max_tokens": request.maxTokens,
            "temperature": request.temperature,
        ]
        if let system = request.systemPrompt {
            body["system"] = system
        }

        guard JSONSerialization.isValidJSONObject(body) else {
            throw LLMError.decodingError("Request-Body ist kein valides JSON")
        }

        let bodyData = try JSONSerialization.data(withJSONObject: body)

        var urlRequest = URLRequest(url: baseURL)
        urlRequest.httpMethod = "POST"
        urlRequest.httpBody = bodyData
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        urlRequest.setValue(apiKey, forHTTPHeaderField: "x-api-key")
        urlRequest.setValue(apiVersion, forHTTPHeaderField: "anthropic-version")

        let (data, response): (Data, URLResponse)
        do {
            (data, response) = try await urlSession.data(for: urlRequest)
        } catch {
            throw LLMError.networkError(error.localizedDescription)
        }

        guard let http = response as? HTTPURLResponse else {
            throw LLMError.networkError("Keine HTTPURLResponse erhalten")
        }

        if http.statusCode == 429 {
            let retryAfter = http.value(forHTTPHeaderField: "retry-after").flatMap(TimeInterval.init)
            throw LLMError.rateLimitExceeded(retryAfter: retryAfter)
        }

        guard (200..<300).contains(http.statusCode) else {
            let bodyStr = String(data: data, encoding: .utf8) ?? "<binary>"
            throw LLMError.httpError(statusCode: http.statusCode, body: bodyStr)
        }

        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw LLMError.decodingError("Antwort ist kein JSON-Objekt")
        }

        // Content-Array verarbeiten (Text + optional Tool-Calls)
        let contentArray = json["content"] as? [[String: Any]] ?? []
        var textContent = ""
        var toolCalls: [ToolCall] = []

        for block in contentArray {
            switch block["type"] as? String {
            case "text":
                if let text = block["text"] as? String { textContent += text }
            case "tool_use":
                if let id = block["id"] as? String,
                   let name = block["name"] as? String,
                   let inputObj = block["input"] {
                    let argsData = (try? JSONSerialization.data(withJSONObject: inputObj)) ?? Data("{}".utf8)
                    let argsJSON = String(data: argsData, encoding: .utf8) ?? "{}"
                    toolCalls.append(ToolCall(id: id, name: name, argumentsJSON: argsJSON))
                }
            default:
                break
            }
        }

        // Usage-Metriken
        let usage = json["usage"] as? [String: Any] ?? [:]
        let inputTokens = usage["input_tokens"] as? Int ?? 0
        let outputTokens = usage["output_tokens"] as? Int ?? 0
        let cost = (Double(inputTokens) / 1_000_000) * costPerMillionInputTokens
                 + (Double(outputTokens) / 1_000_000) * costPerMillionOutputTokens

        // Stop-Reason mappen
        let stopReasonRaw = json["stop_reason"] as? String ?? "other"
        let stopReason: LLMResponse.StopReason = switch stopReasonRaw {
        case "end_turn": .endTurn
        case "max_tokens": .maxTokens
        case "tool_use": .toolUse
        case "stop_sequence": .stopSequence
        default: .other(stopReasonRaw)
        }

        let latencyMs = Int(Date().timeIntervalSince(startTime) * 1000)

        return LLMResponse(
            content: textContent,
            toolCalls: toolCalls,
            stopReason: stopReason,
            usage: .init(inputTokens: inputTokens, outputTokens: outputTokens, estimatedCostUSD: cost),
            metadata: .init(
                backendIdentifier: identifier,
                modelName: modelName,
                latencyMs: latencyMs,
                timestamp: Date()
            )
        )
    }
}
