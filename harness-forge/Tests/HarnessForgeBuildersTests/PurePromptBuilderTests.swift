// PurePromptBuilderTests.swift
// E2E-Tests fuer den PurePromptBuilder. Nutzt tmp-Ordner, cleans sich selbst auf.

import Testing
import Foundation
@testable import HarnessForgeBuilders

private func makeTmpParent() -> URL {
    let url = FileManager.default.temporaryDirectory
        .appendingPathComponent("harness-forge-test-\(UUID().uuidString)")
    try? FileManager.default.createDirectory(at: url, withIntermediateDirectories: true)
    return url
}

private func cleanup(_ url: URL) {
    try? FileManager.default.removeItem(at: url)
}

private let sampleDecomp = TaskDecomposition(
    targetEnvironment: .promptOnly,
    interactivity: .oneShot,
    verifiability: .subjective,
    dataSources: .onlyLLM,
    targetUsers: .endUsers,
    offlineCapability: .canBeOnline,
    rationale: "Einmalige Brainstorming-Aufgabe."
)

@Test("PurePromptBuilder: erzeugt Projekt-Ordner + 2 Dateien")
func buildCreatesFiles() async throws {
    let parent = makeTmpParent()
    defer { cleanup(parent) }

    let builder = PurePromptBuilder()
    let outcome = try await builder.build(
        taskDescription: "Brainstorme Markennamen fuer einen Kaffeeladen",
        slug: "kaffee-marken-brainstorming",
        decomposition: sampleDecomp,
        outputParentDirectory: parent
    )

    #expect(outcome.harnessType == .purePrompt)
    #expect(outcome.createdFiles.count == 2)
    #expect(FileManager.default.fileExists(atPath: outcome.projectDirectory.path))
    for file in outcome.createdFiles {
        #expect(FileManager.default.fileExists(atPath: file.path))
    }
}

@Test("PurePromptBuilder: Prompt-Inhalt enthaelt Task-Beschreibung und alle 6 Achsen")
func promptContentIsComplete() async throws {
    let parent = makeTmpParent()
    defer { cleanup(parent) }

    let task = "Hilf mir Limericks zu schreiben"
    let outcome = try await PurePromptBuilder().build(
        taskDescription: task,
        slug: "limerick-helfer",
        decomposition: sampleDecomp,
        outputParentDirectory: parent
    )

    let promptURL = outcome.createdFiles.first { $0.lastPathComponent == "SYSTEM_PROMPT.md" }!
    let prompt = try String(contentsOf: promptURL, encoding: .utf8)

    #expect(prompt.contains(task))
    #expect(prompt.contains("promptOnly"))
    #expect(prompt.contains("oneShot"))
    #expect(prompt.contains("subjective"))
    #expect(prompt.contains("onlyLLM"))
    #expect(prompt.contains("endUsers"))
    #expect(prompt.contains("canBeOnline"))
    #expect(prompt.contains("Leitprinzipien"))
}

@Test("PurePromptBuilder: README enthaelt Slug und Anleitung")
func readmeContent() async throws {
    let parent = makeTmpParent()
    defer { cleanup(parent) }

    let outcome = try await PurePromptBuilder().build(
        taskDescription: "Test-Task",
        slug: "test-slug",
        decomposition: sampleDecomp,
        outputParentDirectory: parent
    )

    let readmeURL = outcome.createdFiles.first { $0.lastPathComponent == "README.md" }!
    let readme = try String(contentsOf: readmeURL, encoding: .utf8)

    #expect(readme.contains("test-slug"))
    #expect(readme.contains("SYSTEM_PROMPT.md"))
    #expect(readme.contains("Benutzung"))
}

@Test("PurePromptBuilder: wirft bei existierendem Ordner")
func throwsOnExistingDirectory() async throws {
    let parent = makeTmpParent()
    defer { cleanup(parent) }

    let existing = parent.appendingPathComponent("already-here")
    try FileManager.default.createDirectory(at: existing, withIntermediateDirectories: true)

    await #expect(throws: BuilderError.self) {
        _ = try await PurePromptBuilder().build(
            taskDescription: "x",
            slug: "already-here",
            decomposition: sampleDecomp,
            outputParentDirectory: parent
        )
    }
}

@Test("PurePromptBuilder: rollenStatement variiert nach Zielnutzer")
func roleVariesByTargetUsers() async throws {
    let parent = makeTmpParent()
    defer { cleanup(parent) }

    let forDevs = TaskDecomposition(
        targetEnvironment: .promptOnly, interactivity: .oneShot,
        verifiability: .hardTests, dataSources: .onlyLLM,
        targetUsers: .developers, offlineCapability: .canBeOnline,
        rationale: "Entwickler-Tool"
    )
    let forEndUsers = TaskDecomposition(
        targetEnvironment: .promptOnly, interactivity: .oneShot,
        verifiability: .subjective, dataSources: .onlyLLM,
        targetUsers: .endUsers, offlineCapability: .canBeOnline,
        rationale: "Konsumenten-Tool"
    )

    let o1 = try await PurePromptBuilder().build(
        taskDescription: "t",
        slug: "dev-\(UUID().uuidString.prefix(8))",
        decomposition: forDevs,
        outputParentDirectory: parent
    )
    let o2 = try await PurePromptBuilder().build(
        taskDescription: "t",
        slug: "user-\(UUID().uuidString.prefix(8))",
        decomposition: forEndUsers,
        outputParentDirectory: parent
    )

    let p1 = try String(contentsOf: o1.createdFiles[0], encoding: .utf8)
    let p2 = try String(contentsOf: o2.createdFiles[0], encoding: .utf8)

    #expect(p1.contains("Software-Ingenieur"))
    #expect(p2.contains("freundlicher, geduldiger Assistent"))
}
