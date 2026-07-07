// TaskAnalyzerTests.swift
// Tests fuer den Analyzer. Wir setzen einen Fake-LLMClient ein, der genau das
// zurueckgibt, was die Tests erwarten — keine echten API-Calls.

import Testing
import Foundation
@testable import HarnessForgeBuilders
import HarnessForgeCore

// MARK: - Fake-Backend, das fest verdrahtete Antworten liefert

struct ScriptedBackend: LLMClient {
    let identifier: String = "scripted"
    let modelName: String = "scripted-1"
    let costPerMillionInputTokens: Double = 0.0
    let costPerMillionOutputTokens: Double = 0.0
    let maxContextTokens: Int = 100_000
    let supportsTools: Bool = false

    let responseContent: String

    func send(_ request: LLMRequest) async throws -> LLMResponse {
        LLMResponse(
            content: responseContent,
            stopReason: .endTurn,
            usage: .init(inputTokens: 50, outputTokens: 40, estimatedCostUSD: 0.001),
            metadata: .init(
                backendIdentifier: identifier,
                modelName: modelName,
                latencyMs: 1,
                timestamp: Date()
            )
        )
    }
}

// MARK: - Parser-Tests

@Test("TaskAnalyzer.parseDecomposition: Happy-Path")
func parseDecompositionHappy() throws {
    let text = """
    TARGET_ENV: mobile
    INTERACTIVITY: ongoing
    VERIFIABILITY: softCriteria
    DATA_SOURCES: sensors
    TARGET_USERS: endUsers
    OFFLINE: mustOffline
    RATIONALE: Mobile Offline-App mit Sensoren.
    """
    let d = try TaskAnalyzer.parseDecomposition(text)
    #expect(d.targetEnvironment == .mobile)
    #expect(d.interactivity == .ongoing)
    #expect(d.verifiability == .softCriteria)
    #expect(d.dataSources == .sensors)
    #expect(d.targetUsers == .endUsers)
    #expect(d.offlineCapability == .mustOffline)
    #expect(d.rationale.contains("Sensoren"))
}

@Test("TaskAnalyzer.parseDecomposition: fehlendes Feld wirft")
func parseDecompositionIncomplete() {
    let text = "TARGET_ENV: mobile\nINTERACTIVITY: ongoing"
    #expect(throws: TaskAnalyzer.AnalyzerError.self) {
        _ = try TaskAnalyzer.parseDecomposition(text)
    }
}

@Test("TaskAnalyzer.parseDecomposition: ungueltiger Wert wirft")
func parseDecompositionInvalidValue() {
    let text = """
    TARGET_ENV: marspod
    INTERACTIVITY: ongoing
    VERIFIABILITY: softCriteria
    DATA_SOURCES: sensors
    TARGET_USERS: endUsers
    OFFLINE: mustOffline
    RATIONALE: x
    """
    #expect(throws: TaskAnalyzer.AnalyzerError.self) {
        _ = try TaskAnalyzer.parseDecomposition(text)
    }
}

// MARK: - End-to-End mit ScriptedBackend

@Test("TaskAnalyzer.analyze: E2E mit skriptetem Backend liefert Empfehlung")
func analyzeEndToEnd() async throws {
    let scriptedResponse = """
    TARGET_ENV: mobile
    INTERACTIVITY: ongoing
    VERIFIABILITY: softCriteria
    DATA_SOURCES: sensors
    TARGET_USERS: endUsers
    OFFLINE: mustOffline
    RATIONALE: Offline-Wander-App mit GPS.
    """
    let backend = ScriptedBackend(responseContent: scriptedResponse)
    let analyzer = TaskAnalyzer(client: backend)

    let result = try await analyzer.analyze(
        taskDescription: "Baue eine Offline-Wander-App mit GPS"
    )

    #expect(result.slug == "baue-eine-offline-wander-app-mit-gps")
    #expect(result.recommendation == .androidKotlin)
    #expect(result.matrix.rows.count == HarnessType.allCases.count)
    #expect(result.totalCostUSD > 0)
    #expect(result.rationale.contains("Android"))
}

@Test("TaskAnalyzer.analyze: existingSlugs fuehrt zu eindeutigem Suffix")
func analyzeUniqueSlug() async throws {
    let scripted = """
    TARGET_ENV: terminal
    INTERACTIVITY: oneShot
    VERIFIABILITY: hardTests
    DATA_SOURCES: localFiles
    TARGET_USERS: developers
    OFFLINE: canBeOnline
    RATIONALE: CLI-Tool.
    """
    let backend = ScriptedBackend(responseContent: scripted)
    let analyzer = TaskAnalyzer(client: backend)

    let result = try await analyzer.analyze(
        taskDescription: "Log Parser",
        existingSlugs: ["log-parser"]
    )
    #expect(result.slug == "log-parser-2")
}

@Test("TaskAnalyzer.analyze: Web-Search wird nicht aufgerufen wenn deaktiviert")
func analyzeWebSearchOff() async throws {
    struct SpyWebSearch: WebSearchTool {
        let callCount: ManagedCounter

        func search(query: String, maxResults: Int) async throws -> [WebSearchResult] {
            await callCount.increment()
            return []
        }
    }

    actor ManagedCounter {
        var count = 0
        func increment() { count += 1 }
    }

    let spy = ManagedCounter()
    let scripted = """
    TARGET_ENV: promptOnly
    INTERACTIVITY: oneShot
    VERIFIABILITY: subjective
    DATA_SOURCES: onlyLLM
    TARGET_USERS: claudeUsers
    OFFLINE: canBeOnline
    RATIONALE: Brainstorming.
    """
    let analyzer = TaskAnalyzer(
        client: ScriptedBackend(responseContent: scripted),
        webSearch: SpyWebSearch(callCount: spy)
    )
    _ = try await analyzer.analyze(taskDescription: "t", enableWebSearch: false)
    let count = await spy.count
    #expect(count == 0)
}
