// EndToEndDogfoodingTests.swift
// Der finale Integrations-Test: laeuft die komplette Harness-Forge-Pipeline
// von einer natuerlichsprachigen Aufgabe bis zu fertigen Projekt-Dateien durch.
// Nutzt ScriptedBackend (aus TaskAnalyzerTests), so ist der Test deterministisch
// und braucht keine API-Keys.

import Testing
import Foundation
@testable import HarnessForgeBuilders
import HarnessForgeCore

// MARK: - Helfer

private func makeTmpParent() -> URL {
    let url = FileManager.default.temporaryDirectory
        .appendingPathComponent("harness-forge-e2e-\(UUID().uuidString)")
    try? FileManager.default.createDirectory(at: url, withIntermediateDirectories: true)
    return url
}
private func cleanup(_ url: URL) { try? FileManager.default.removeItem(at: url) }

private func selectBuilder(for type: HarnessType) -> any HarnessBuilder {
    switch type {
    case .purePrompt: PurePromptBuilder()
    case .pythonCLI: PythonCLIBuilder()
    case .tauriDesktop: TauriDesktopBuilder()
    case .androidKotlin: AndroidKotlinBuilder()
    case .claudeSubagent: ClaudeSubagentBuilder()
    }
}

// MARK: - Szenario 1: Mobile Offline-App → Android

@Test("E2E: Packrafting-Trip-Planner fuehrt zu Android-Harness")
func e2ePackraftingToAndroid() async throws {
    let parent = makeTmpParent(); defer { cleanup(parent) }

    // Der Analyzer wuerde das so einordnen — gescripteter LLM-Output:
    let scriptedResponse = """
    TARGET_ENV: mobile
    INTERACTIVITY: ongoing
    VERIFIABILITY: softCriteria
    DATA_SOURCES: sensors
    TARGET_USERS: endUsers
    OFFLINE: mustOffline
    RATIONALE: Unterwegs-Tool mit GPS und Offline-Bedarf.
    """
    let backend = ScriptedBackend(responseContent: scriptedResponse)
    let analyzer = TaskAnalyzer(client: backend)

    // Phase 1+2+3+4: Zerlegung + Matrix + Empfehlung
    let analysis = try await analyzer.analyze(
        taskDescription: "Baue mir einen Begleiter fuer Packrafting-Touren in Schweden mit GPS und Tagesplan"
    )

    #expect(analysis.recommendation == .androidKotlin)
    #expect(analysis.slug.hasPrefix("baue-mir-einen-begleiter"))
    #expect(analysis.slug.count <= 40)
    #expect(analysis.matrix.rows.count == 5)

    // Phase 5: Bauen
    let builder = selectBuilder(for: analysis.recommendation)
    let outcome = try await builder.build(
        taskDescription: analysis.taskDescription,
        slug: analysis.slug,
        decomposition: analysis.decomposition,
        outputParentDirectory: parent
    )

    #expect(outcome.harnessType == .androidKotlin)
    #expect(outcome.createdFiles.count >= 13)

    // Verifikation der erzeugten Struktur
    let projectDir = outcome.projectDirectory
    #expect(FileManager.default.fileExists(
        atPath: projectDir.appendingPathComponent("app/build.gradle.kts").path))
    #expect(FileManager.default.fileExists(
        atPath: projectDir.appendingPathComponent("README.md").path))
    #expect(FileManager.default.fileExists(
        atPath: projectDir.appendingPathComponent("SYSTEM_PROMPT.md").path))

    // SYSTEM_PROMPT enthaelt die urspruengliche Aufgabe
    let prompt = try String(
        contentsOf: projectDir.appendingPathComponent("SYSTEM_PROMPT.md"),
        encoding: .utf8
    )
    #expect(prompt.contains("Packrafting-Touren"))
}

// MARK: - Szenario 2: Terminal-Tool → Python CLI

@Test("E2E: Log-Parser-Aufgabe fuehrt zu Python-CLI-Harness")
func e2eLogParserToPython() async throws {
    let parent = makeTmpParent(); defer { cleanup(parent) }

    let scriptedResponse = """
    TARGET_ENV: terminal
    INTERACTIVITY: oneShot
    VERIFIABILITY: hardTests
    DATA_SOURCES: localFiles
    TARGET_USERS: developers
    OFFLINE: canBeOnline
    RATIONALE: Einmaliges Terminal-Tool fuer Entwickler.
    """
    let backend = ScriptedBackend(responseContent: scriptedResponse)
    let analyzer = TaskAnalyzer(client: backend)

    let analysis = try await analyzer.analyze(
        taskDescription: "Parse nginx access-logs und finde die 10 langsamsten Requests pro Tag"
    )

    #expect(analysis.recommendation == .pythonCLI)

    let builder = selectBuilder(for: analysis.recommendation)
    let outcome = try await builder.build(
        taskDescription: analysis.taskDescription,
        slug: analysis.slug,
        decomposition: analysis.decomposition,
        outputParentDirectory: parent
    )

    #expect(outcome.harnessType == .pythonCLI)
    let pyproject = outcome.createdFiles.first { $0.lastPathComponent == "pyproject.toml" }!
    let content = try String(contentsOf: pyproject, encoding: .utf8)
    #expect(content.contains("typer>="))
}

// MARK: - Szenario 3: Brainstorming → Pure Prompt

@Test("E2E: Brainstorming-Aufgabe fuehrt zu Pure-Prompt-Harness")
func e2eBrainstormingToPurePrompt() async throws {
    let parent = makeTmpParent(); defer { cleanup(parent) }

    let scriptedResponse = """
    TARGET_ENV: promptOnly
    INTERACTIVITY: oneShot
    VERIFIABILITY: subjective
    DATA_SOURCES: onlyLLM
    TARGET_USERS: endUsers
    OFFLINE: canBeOnline
    RATIONALE: Einmalige Ideenfindung ohne externe Daten.
    """
    let backend = ScriptedBackend(responseContent: scriptedResponse)
    let analyzer = TaskAnalyzer(client: backend)

    let analysis = try await analyzer.analyze(
        taskDescription: "Brainstorme 30 Markennamen fuer einen Rostocker Kaffeeladen"
    )

    #expect(analysis.recommendation == .purePrompt)

    let builder = selectBuilder(for: analysis.recommendation)
    let outcome = try await builder.build(
        taskDescription: analysis.taskDescription,
        slug: analysis.slug,
        decomposition: analysis.decomposition,
        outputParentDirectory: parent
    )

    #expect(outcome.harnessType == .purePrompt)
    #expect(outcome.createdFiles.count == 2) // SYSTEM_PROMPT.md + README.md
}

// MARK: - Verifikation der 6-Schicht-Pipeline auf einen realen Flow

@Test("E2E: Alle 5 Builder koennen hintereinander gebaut werden")
func e2eAllBuildersWork() async throws {
    let parent = makeTmpParent(); defer { cleanup(parent) }

    let decomp = TaskDecomposition(
        targetEnvironment: .desktop,
        interactivity: .ongoing,
        verifiability: .softCriteria,
        dataSources: .localFiles,
        targetUsers: .developers,
        offlineCapability: .canBeOnline,
        rationale: "E2E-Test"
    )

    for (i, harnessType) in HarnessType.allCases.enumerated() {
        let slug = "e2e-test-\(i)"
        let builder = selectBuilder(for: harnessType)
        let outcome = try await builder.build(
            taskDescription: "E2E-Testaufgabe \(harnessType.rawValue)",
            slug: slug,
            decomposition: decomp,
            outputParentDirectory: parent
        )
        #expect(outcome.harnessType == harnessType)
        #expect(!outcome.createdFiles.isEmpty)
        #expect(FileManager.default.fileExists(atPath: outcome.projectDirectory.path))
    }
}

// MARK: - Auto-Commit-Simulation

@Test("E2E: erzeugter Harness ist ein valides Git-Repo-Unterprojekt")
func e2eGeneratedProjectIsFlat() async throws {
    let parent = makeTmpParent(); defer { cleanup(parent) }
    let scripted = """
    TARGET_ENV: terminal
    INTERACTIVITY: oneShot
    VERIFIABILITY: hardTests
    DATA_SOURCES: localFiles
    TARGET_USERS: developers
    OFFLINE: canBeOnline
    RATIONALE: Testprojekt.
    """
    let analyzer = TaskAnalyzer(client: ScriptedBackend(responseContent: scripted))
    let analysis = try await analyzer.analyze(taskDescription: "Git-Repo-Test-Aufgabe")
    let outcome = try await selectBuilder(for: analysis.recommendation).build(
        taskDescription: analysis.taskDescription,
        slug: analysis.slug,
        decomposition: analysis.decomposition,
        outputParentDirectory: parent
    )

    // Harnesses sollten KEIN eigenes .git haben — sie werden Teil des proggs-Mono-Repos
    let gitDir = outcome.projectDirectory.appendingPathComponent(".git")
    #expect(!FileManager.default.fileExists(atPath: gitDir.path),
            "Generierter Harness darf KEIN .git haben (er gehoert ins Mono-Repo)")

    // Aber ein .gitignore fuer Build-Artefakte
    let gitignore = outcome.projectDirectory.appendingPathComponent(".gitignore")
    #expect(FileManager.default.fileExists(atPath: gitignore.path))
}
