// RouterTests.swift
// Tests fuer die Backend-Registrierung und Auswahl-Heuristik.

import Testing
@testable import HarnessForgeCore

/// Fake-Backend, das keine echten API-Calls macht.
struct FakeBackend: LLMClient {
    let identifier: String
    let modelName: String
    let costPerMillionInputTokens: Double
    let costPerMillionOutputTokens: Double
    let maxContextTokens: Int = 100_000
    let supportsTools: Bool = true

    func send(_ request: LLMRequest) async throws -> LLMResponse {
        LLMResponse(
            content: "fake",
            stopReason: .endTurn,
            usage: .zero,
            metadata: .init(
                backendIdentifier: identifier,
                modelName: modelName,
                latencyMs: 0,
                timestamp: .init()
            )
        )
    }
}

@Test("Router: speichert und findet Backends")
func routerRegisterAndLookup() async throws {
    let router = Router()
    let fake = FakeBackend(
        identifier: "anthropic",
        modelName: "claude-test",
        costPerMillionInputTokens: 15,
        costPerMillionOutputTokens: 75
    )
    await router.register(fake)

    let list = await router.listBackends()
    #expect(list == ["anthropic"])

    let found = try await router.backend(identifier: "anthropic")
    #expect(found.identifier == "anthropic")
}

@Test("Router: wirft bei unbekanntem Backend")
func routerUnknownThrows() async {
    let router = Router()
    await #expect(throws: LLMError.self) {
        _ = try await router.backend(identifier: "unbekannt")
    }
}

@Test("Router: liefert nil wenn leer")
func routerEmptyReturnsNil() async {
    let router = Router()
    let chosen = await router.chooseBackend(for: .reasoning)
    #expect(chosen == nil)
}

@Test("Router: bevorzugt Anthropic fuer Reasoning")
func routerReasoningPrefersAnthropic() async {
    let router = Router()
    await router.register(FakeBackend(
        identifier: "openai", modelName: "gpt", costPerMillionInputTokens: 2, costPerMillionOutputTokens: 10))
    await router.register(FakeBackend(
        identifier: "anthropic", modelName: "claude", costPerMillionInputTokens: 15, costPerMillionOutputTokens: 75))

    let chosen = await router.chooseBackend(for: .reasoning)
    #expect(chosen?.identifier == "anthropic")
}

@Test("Router: preferLocal waehlt LM Studio")
func routerPrefersLocal() async {
    let router = Router()
    await router.register(FakeBackend(
        identifier: "anthropic", modelName: "claude", costPerMillionInputTokens: 15, costPerMillionOutputTokens: 75))
    await router.register(FakeBackend(
        identifier: "lmstudio", modelName: "local", costPerMillionInputTokens: 0, costPerMillionOutputTokens: 0))

    let chosen = await router.chooseBackend(
        for: .reasoning,
        budget: Router.CostBudget(maxTotalUSD: 10, preferLocal: true)
    )
    #expect(chosen?.identifier == "lmstudio")
}

@Test("Router: summarization waehlt guenstigstes")
func routerSummarizationPicksCheapest() async {
    let router = Router()
    await router.register(FakeBackend(
        identifier: "anthropic", modelName: "claude", costPerMillionInputTokens: 15, costPerMillionOutputTokens: 75))
    await router.register(FakeBackend(
        identifier: "gemini", modelName: "gem", costPerMillionInputTokens: 1.25, costPerMillionOutputTokens: 5))

    let chosen = await router.chooseBackend(for: .summarization)
    #expect(chosen?.identifier == "gemini")
}

@Test("Router: unregister entfernt Backend")
func routerUnregister() async {
    let router = Router()
    await router.register(FakeBackend(
        identifier: "x", modelName: "y", costPerMillionInputTokens: 1, costPerMillionOutputTokens: 1))
    await router.unregister(identifier: "x")
    let list = await router.listBackends()
    #expect(list.isEmpty)
}
