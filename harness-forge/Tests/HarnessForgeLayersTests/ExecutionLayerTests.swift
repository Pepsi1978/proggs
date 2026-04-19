// ExecutionLayerTests.swift

import Testing
import Foundation
@testable import HarnessForgeLayers

@Test("Execution: Tool registrieren und auflisten")
func registerAndList() async {
    let layer = ExecutionLayer()
    await layer.register(ExecutionLayer.Tool(
        name: "echo",
        description: "Gibt den Input zurueck",
        execute: { $0 }
    ))

    let tools = await layer.listTools()
    #expect(tools.count == 1)
    #expect(tools[0].name == "echo")

    let has = await layer.hasTool(name: "echo")
    #expect(has == true)
}

@Test("Execution: invoke ruft das Tool auf")
func invokeTool() async throws {
    let layer = ExecutionLayer()
    await layer.register(ExecutionLayer.Tool(
        name: "uppercase",
        description: "Macht Gross",
        execute: { $0.uppercased() }
    ))

    let result = try await layer.invoke(name: "uppercase", argumentsJSON: "hallo")
    #expect(result == "HALLO")
}

@Test("Execution: unbekanntes Tool wirft")
func unknownToolThrows() async {
    let layer = ExecutionLayer()
    await #expect(throws: ExecutionLayer.ExecutionError.self) {
        _ = try await layer.invoke(name: "ghost", argumentsJSON: "{}")
    }
}

@Test("Execution: Aufrufe werden gezaehlt")
func invocationCounting() async throws {
    let layer = ExecutionLayer()
    await layer.register(ExecutionLayer.Tool(
        name: "noop",
        description: "x",
        execute: { _ in "" }
    ))

    _ = try await layer.invoke(name: "noop", argumentsJSON: "{}")
    _ = try await layer.invoke(name: "noop", argumentsJSON: "{}")
    _ = try await layer.invoke(name: "noop", argumentsJSON: "{}")

    let count = await layer.invocations(for: "noop")
    #expect(count == 3)
    let total = await layer.totalInvocations()
    #expect(total == 3)
}

@Test("Execution: unregister entfernt das Tool")
func unregister() async {
    let layer = ExecutionLayer()
    await layer.register(ExecutionLayer.Tool(name: "tmp", description: "x", execute: { _ in "" }))
    await layer.unregister(name: "tmp")
    let has = await layer.hasTool(name: "tmp")
    #expect(has == false)
}

@Test("Execution: Fehler im Tool werden zu ExecutionError")
func toolErrorWrapped() async {
    struct CustomError: Error, LocalizedError {
        var errorDescription: String? { "intern" }
    }

    let layer = ExecutionLayer()
    await layer.register(ExecutionLayer.Tool(
        name: "bad",
        description: "wirft immer",
        execute: { _ in throw CustomError() }
    ))

    await #expect(throws: ExecutionLayer.ExecutionError.self) {
        _ = try await layer.invoke(name: "bad", argumentsJSON: "{}")
    }
}
