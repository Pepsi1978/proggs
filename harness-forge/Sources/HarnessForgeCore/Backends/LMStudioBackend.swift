// LMStudioBackend.swift
// LLMClient-Implementierung fuer lokale Modelle via LM Studio.
// LM Studio bietet eine OpenAI-kompatible API — wir delegieren an OpenAIBackend
// mit angepasster Base-URL und ohne echten API-Key.

import Foundation

/// Lokales Backend via LM Studio (OpenAI-kompatibel, Default-Port 1234).
///
/// LM Studio laeuft als Desktop-App auf dem selben Rechner und stellt eine
/// OpenAI-kompatible HTTP-Schnittstelle zur Verfuegung. Dieser Wrapper nutzt
/// intern `OpenAIBackend`, da das Protokoll identisch ist — die Unterschiede
/// sind nur Base-URL, fehlender API-Key und null Kosten.
public struct LMStudioBackend: LLMClient {
    public let identifier: String = "lmstudio"
    public var modelName: String { inner.modelName }
    public var costPerMillionInputTokens: Double { 0.0 }
    public var costPerMillionOutputTokens: Double { 0.0 }
    public var maxContextTokens: Int { inner.maxContextTokens }
    public var supportsTools: Bool { inner.supportsTools }

    private let inner: OpenAIBackend

    public init(
        modelName: String = "local-model",
        baseURL: URL = URL(string: "http://localhost:1234/v1/chat/completions")!,
        maxContextTokens: Int = 32_000,
        urlSession: URLSession = .shared
    ) {
        self.inner = OpenAIBackend(
            apiKey: "lm-studio", // LM Studio akzeptiert jeden Key
            modelName: modelName,
            baseURL: baseURL,
            urlSession: urlSession,
            identifier: "lmstudio",
            costPerMillionInputTokens: 0.0,
            costPerMillionOutputTokens: 0.0,
            maxContextTokens: maxContextTokens
        )
    }

    public func send(_ request: LLMRequest) async throws -> LLMResponse {
        try await inner.send(request)
    }
}
