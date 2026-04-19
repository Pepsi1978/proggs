// LLMClientMockTests.swift
// Tests fuer die vier Backend-Implementierungen mit URLProtocol-Mocks.

import Testing
import Foundation
@testable import HarnessForgeCore

// MARK: - Mock-URLProtocol

/// URLProtocol-Subklasse, die ALLE Requests abfaengt und vordefinierte Antworten
/// liefert — so testen wir Backends ohne echte API-Keys.
final class MockURLProtocol: URLProtocol, @unchecked Sendable {
    // swiftlint:disable:next nesting
    nonisolated(unsafe) static var mockHandler: (@Sendable (URLRequest) throws -> (HTTPURLResponse, Data))?

    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        guard let handler = MockURLProtocol.mockHandler else {
            client?.urlProtocol(self, didFailWithError: URLError(.badURL))
            return
        }
        do {
            let (response, data) = try handler(request)
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: data)
            client?.urlProtocolDidFinishLoading(self)
        } catch {
            client?.urlProtocol(self, didFailWithError: error)
        }
    }

    override func stopLoading() {}
}

private func mockedSession() -> URLSession {
    let config = URLSessionConfiguration.ephemeral
    config.protocolClasses = [MockURLProtocol.self]
    return URLSession(configuration: config)
}

private func httpResponse(for request: URLRequest, statusCode: Int) -> HTTPURLResponse {
    HTTPURLResponse(
        url: request.url!,
        statusCode: statusCode,
        httpVersion: "HTTP/1.1",
        headerFields: ["Content-Type": "application/json"]
    )!
}

// MARK: - Mock-abhaengige Tests (.serialized, weil globaler Handler geteilt wird)

/// Alle Tests in diesem Suite muessen sequentiell laufen, weil sie sich den
/// globalen `MockURLProtocol.mockHandler` teilen. Ohne `.serialized` wuerde
/// Swift Testing sie parallel starten und die Handler wuerden sich ueberschreiben.
@Suite("LLM-Backends gegen URLProtocol-Mocks", .serialized)
struct LLMBackendMockTests {
    @Test("Anthropic: parst erfolgreiche Antwort korrekt")
    func anthropicHappyPath() async throws {
        let responseJSON = """
        {
            "content": [{"type": "text", "text": "Hallo Welt"}],
            "stop_reason": "end_turn",
            "usage": {"input_tokens": 10, "output_tokens": 5}
        }
        """
        MockURLProtocol.mockHandler = { req in
            (httpResponse(for: req, statusCode: 200), Data(responseJSON.utf8))
        }

        let backend = AnthropicBackend(apiKey: "test-key", urlSession: mockedSession())
        let response = try await backend.send(LLMRequest(messages: [.user("Hallo")]))

        #expect(response.content == "Hallo Welt")
        #expect(response.usage.inputTokens == 10)
        #expect(response.usage.outputTokens == 5)
        #expect(response.stopReason == .endTurn)
        #expect(response.metadata.backendIdentifier == "anthropic")
        #expect(response.usage.estimatedCostUSD > 0)
    }

    @Test("Anthropic: wirft LLMError bei HTTP 401")
    func anthropicUnauthorized() async throws {
        MockURLProtocol.mockHandler = { req in
            (httpResponse(for: req, statusCode: 401), Data("unauthorized".utf8))
        }
        let backend = AnthropicBackend(apiKey: "bad-key", urlSession: mockedSession())

        await #expect(throws: LLMError.self) {
            _ = try await backend.send(LLMRequest(messages: [.user("Hallo")]))
        }
    }

    @Test("Anthropic: Rate-Limit wird erkannt")
    func anthropicRateLimit() async throws {
        MockURLProtocol.mockHandler = { req in
            (httpResponse(for: req, statusCode: 429), Data("rate limit".utf8))
        }
        let backend = AnthropicBackend(apiKey: "key", urlSession: mockedSession())

        await #expect(throws: LLMError.self) {
            _ = try await backend.send(LLMRequest(messages: [.user("x")]))
        }
    }

    @Test("OpenAI: parst Chat-Completions-Antwort")
    func openAIHappyPath() async throws {
        let responseJSON = """
        {
            "choices": [{
                "message": {"role": "assistant", "content": "Hi"},
                "finish_reason": "stop"
            }],
            "usage": {"prompt_tokens": 8, "completion_tokens": 2}
        }
        """
        MockURLProtocol.mockHandler = { req in
            (httpResponse(for: req, statusCode: 200), Data(responseJSON.utf8))
        }

        let backend = OpenAIBackend(apiKey: "sk-test", urlSession: mockedSession())
        let response = try await backend.send(LLMRequest(messages: [.user("Hi")]))

        #expect(response.content == "Hi")
        #expect(response.usage.inputTokens == 8)
        #expect(response.usage.outputTokens == 2)
        #expect(response.stopReason == .endTurn)
    }

    @Test("Gemini: parst GenerateContent-Antwort")
    func geminiHappyPath() async throws {
        let responseJSON = """
        {
            "candidates": [{
                "content": {"parts": [{"text": "Grüezi"}]},
                "finishReason": "STOP"
            }],
            "usageMetadata": {"promptTokenCount": 5, "candidatesTokenCount": 1}
        }
        """
        MockURLProtocol.mockHandler = { req in
            (httpResponse(for: req, statusCode: 200), Data(responseJSON.utf8))
        }

        let backend = GeminiBackend(apiKey: "gem-test", urlSession: mockedSession())
        let response = try await backend.send(LLMRequest(messages: [.user("Hallo")]))

        #expect(response.content == "Grüezi")
        #expect(response.usage.inputTokens == 5)
        #expect(response.stopReason == .endTurn)
    }

    @Test("LM Studio: delegiert an OpenAIBackend mit 0 Kosten")
    func lmStudioZeroCost() async throws {
        let responseJSON = """
        {
            "choices": [{"message": {"content": "lokal"}, "finish_reason": "stop"}],
            "usage": {"prompt_tokens": 100, "completion_tokens": 200}
        }
        """
        MockURLProtocol.mockHandler = { req in
            (httpResponse(for: req, statusCode: 200), Data(responseJSON.utf8))
        }

        let backend = LMStudioBackend(urlSession: mockedSession())
        let response = try await backend.send(LLMRequest(messages: [.user("x")]))

        #expect(response.content == "lokal")
        #expect(response.usage.estimatedCostUSD == 0.0, "Lokale Modelle kosten nichts")
        #expect(backend.identifier == "lmstudio")
    }
}

// MARK: - Mock-unabhaengige Tests (parallel OK)

@Test("ChatMessage: Convenience-Initializer setzen die richtige Role")
func chatMessageConvenience() {
    #expect(ChatMessage.system("x").role == .system)
    #expect(ChatMessage.user("x").role == .user)
    #expect(ChatMessage.assistant("x").role == .assistant)
    #expect(ChatMessage.tool(callID: "id", content: "x").role == .tool)
    #expect(ChatMessage.tool(callID: "id", content: "x").toolCallID == "id")
}
