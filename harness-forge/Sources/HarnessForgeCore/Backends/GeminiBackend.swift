// GeminiBackend.swift
// LLMClient-Implementierung fuer Google Gemini (GenerateContent API).
// Endpoint: https://generativelanguage.googleapis.com/v1beta/models/<model>:generateContent

import Foundation

public struct GeminiBackend: LLMClient {
    public let identifier: String = "gemini"
    public let modelName: String
    public let costPerMillionInputTokens: Double
    public let costPerMillionOutputTokens: Double
    public let maxContextTokens: Int = 2_000_000
    public let supportsTools: Bool = true

    private let apiKey: String
    private let baseHost: URL
    private let urlSession: URLSession

    public init(
        apiKey: String,
        modelName: String = "gemini-2.5-pro",
        baseHost: URL = URL(string: "https://generativelanguage.googleapis.com")!,
        urlSession: URLSession = .shared
    ) {
        self.apiKey = apiKey
        self.modelName = modelName
        self.baseHost = baseHost
        self.urlSession = urlSession
        // Gemini 2.5 Pro Kosten-Schaetzung (USD pro 1M Tokens, Stand 2026)
        self.costPerMillionInputTokens = 1.25
        self.costPerMillionOutputTokens = 5.00
    }

    public func send(_ request: LLMRequest) async throws -> LLMResponse {
        let startTime = Date()

        // Gemini-spezifisches Format: contents mit parts
        var contents: [[String: Any]] = []
        for msg in request.messages where msg.role != .system {
            let role = msg.role == .assistant ? "model" : "user"
            contents.append([
                "role": role,
                "parts": [["text": msg.content]],
            ])
        }

        var body: [String: Any] = [
            "contents": contents,
            "generationConfig": [
                "maxOutputTokens": request.maxTokens,
                "temperature": request.temperature,
            ],
        ]

        if let system = request.systemPrompt {
            body["systemInstruction"] = [
                "parts": [["text": system]],
            ]
        }

        guard JSONSerialization.isValidJSONObject(body) else {
            throw LLMError.decodingError("Request-Body ist kein valides JSON")
        }

        let bodyData = try JSONSerialization.data(withJSONObject: body)

        // URL mit Key als Query-Parameter bauen
        let urlString = "\(baseHost.absoluteString)/v1beta/models/\(modelName):generateContent?key=\(apiKey)"
        guard let url = URL(string: urlString) else {
            throw LLMError.invalidURL(urlString)
        }

        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "POST"
        urlRequest.httpBody = bodyData
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")

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
            throw LLMError.rateLimitExceeded(retryAfter: nil)
        }

        guard (200..<300).contains(http.statusCode) else {
            let bodyStr = String(data: data, encoding: .utf8) ?? "<binary>"
            throw LLMError.httpError(statusCode: http.statusCode, body: bodyStr)
        }

        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw LLMError.decodingError("Antwort ist kein JSON-Objekt")
        }

        let candidates = json["candidates"] as? [[String: Any]] ?? []
        let firstCand = candidates.first ?? [:]
        let contentObj = firstCand["content"] as? [String: Any] ?? [:]
        let parts = contentObj["parts"] as? [[String: Any]] ?? []
        let textContent = parts.compactMap { $0["text"] as? String }.joined()

        let usage = json["usageMetadata"] as? [String: Any] ?? [:]
        let inputTokens = usage["promptTokenCount"] as? Int ?? 0
        let outputTokens = usage["candidatesTokenCount"] as? Int ?? 0
        let cost = (Double(inputTokens) / 1_000_000) * costPerMillionInputTokens
                 + (Double(outputTokens) / 1_000_000) * costPerMillionOutputTokens

        let finishReason = firstCand["finishReason"] as? String ?? "OTHER"
        let stopReason: LLMResponse.StopReason = switch finishReason {
        case "STOP": .endTurn
        case "MAX_TOKENS": .maxTokens
        case "SAFETY", "RECITATION": .other(finishReason)
        default: .other(finishReason)
        }

        let latencyMs = Int(Date().timeIntervalSince(startTime) * 1000)

        return LLMResponse(
            content: textContent,
            toolCalls: [],
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
