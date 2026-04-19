// OpenAIBackend.swift
// LLMClient-Implementierung fuer OpenAIs Chat Completions API.
// Endpoint: https://api.openai.com/v1/chat/completions

import Foundation

public struct OpenAIBackend: LLMClient {
    public let identifier: String
    public let modelName: String
    public let costPerMillionInputTokens: Double
    public let costPerMillionOutputTokens: Double
    public let maxContextTokens: Int
    public let supportsTools: Bool = true

    private let apiKey: String
    private let baseURL: URL
    private let urlSession: URLSession

    public init(
        apiKey: String,
        modelName: String = "gpt-4.1",
        baseURL: URL = URL(string: "https://api.openai.com/v1/chat/completions")!,
        urlSession: URLSession = .shared,
        identifier: String = "openai",
        costPerMillionInputTokens: Double = 2.50,
        costPerMillionOutputTokens: Double = 10.00,
        maxContextTokens: Int = 128_000
    ) {
        self.apiKey = apiKey
        self.modelName = modelName
        self.baseURL = baseURL
        self.urlSession = urlSession
        self.identifier = identifier
        self.costPerMillionInputTokens = costPerMillionInputTokens
        self.costPerMillionOutputTokens = costPerMillionOutputTokens
        self.maxContextTokens = maxContextTokens
    }

    public func send(_ request: LLMRequest) async throws -> LLMResponse {
        let startTime = Date()

        // OpenAI-Message-Format: system als normale role in messages
        var messagesArray: [[String: Any]] = []
        if let system = request.systemPrompt {
            messagesArray.append(["role": "system", "content": system])
        }
        for msg in request.messages {
            messagesArray.append(["role": msg.role.rawValue, "content": msg.content])
        }

        let body: [String: Any] = [
            "model": modelName,
            "messages": messagesArray,
            "max_tokens": request.maxTokens,
            "temperature": request.temperature,
        ]

        guard JSONSerialization.isValidJSONObject(body) else {
            throw LLMError.decodingError("Request-Body ist kein valides JSON")
        }

        let bodyData = try JSONSerialization.data(withJSONObject: body)

        var urlRequest = URLRequest(url: baseURL)
        urlRequest.httpMethod = "POST"
        urlRequest.httpBody = bodyData
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        urlRequest.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")

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

        let choices = json["choices"] as? [[String: Any]] ?? []
        let firstChoice = choices.first ?? [:]
        let message = firstChoice["message"] as? [String: Any] ?? [:]
        let content = message["content"] as? String ?? ""

        // Tool-Calls parsen
        var toolCalls: [ToolCall] = []
        if let tcs = message["tool_calls"] as? [[String: Any]] {
            for tc in tcs {
                if let id = tc["id"] as? String,
                   let function = tc["function"] as? [String: Any],
                   let name = function["name"] as? String,
                   let args = function["arguments"] as? String {
                    toolCalls.append(ToolCall(id: id, name: name, argumentsJSON: args))
                }
            }
        }

        let usage = json["usage"] as? [String: Any] ?? [:]
        let inputTokens = usage["prompt_tokens"] as? Int ?? 0
        let outputTokens = usage["completion_tokens"] as? Int ?? 0
        let cost = (Double(inputTokens) / 1_000_000) * costPerMillionInputTokens
                 + (Double(outputTokens) / 1_000_000) * costPerMillionOutputTokens

        let finishReason = firstChoice["finish_reason"] as? String ?? "other"
        let stopReason: LLMResponse.StopReason = switch finishReason {
        case "stop": .endTurn
        case "length": .maxTokens
        case "tool_calls": .toolUse
        default: .other(finishReason)
        }

        let latencyMs = Int(Date().timeIntervalSince(startTime) * 1000)

        return LLMResponse(
            content: content,
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
