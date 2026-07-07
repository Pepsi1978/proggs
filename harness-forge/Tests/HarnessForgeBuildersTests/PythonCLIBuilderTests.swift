// PythonCLIBuilderTests.swift

import Testing
import Foundation
@testable import HarnessForgeBuilders

private func makeTmpParent() -> URL {
    let url = FileManager.default.temporaryDirectory
        .appendingPathComponent("harness-forge-pycli-test-\(UUID().uuidString)")
    try? FileManager.default.createDirectory(at: url, withIntermediateDirectories: true)
    return url
}

private func cleanup(_ url: URL) {
    try? FileManager.default.removeItem(at: url)
}

private let pythonDecomp = TaskDecomposition(
    targetEnvironment: .terminal,
    interactivity: .oneShot,
    verifiability: .hardTests,
    dataSources: .localFiles,
    targetUsers: .developers,
    offlineCapability: .canBeOnline,
    rationale: "Typisches CLI-Tool."
)

@Test("PythonCLIBuilder: erzeugt alle 8 erwarteten Dateien")
func buildsAllFiles() async throws {
    let parent = makeTmpParent()
    defer { cleanup(parent) }

    let outcome = try await PythonCLIBuilder().build(
        taskDescription: "Parse und fasse Server-Logs zusammen",
        slug: "log-summary",
        decomposition: pythonDecomp,
        outputParentDirectory: parent
    )

    #expect(outcome.createdFiles.count == 8)
    for file in outcome.createdFiles {
        #expect(FileManager.default.fileExists(atPath: file.path), "fehlt: \(file.lastPathComponent)")
    }

    // Erwartete Namen
    let names = Set(outcome.createdFiles.map(\.lastPathComponent))
    #expect(names == Set([
        "pyproject.toml",
        "README.md",
        "SYSTEM_PROMPT.md",
        ".gitignore",
        "__init__.py",
        "__main__.py",
        "cli.py",
        "llm_client.py",
    ]))
}

@Test("PythonCLIBuilder: pyproject.toml enthaelt Projekt-Name, Entry-Point und Abhaengigkeiten")
func pyprojectContent() async throws {
    let parent = makeTmpParent()
    defer { cleanup(parent) }

    let outcome = try await PythonCLIBuilder().build(
        taskDescription: "Test",
        slug: "my-cool-tool",
        decomposition: pythonDecomp,
        outputParentDirectory: parent
    )

    let pyproject = outcome.createdFiles.first { $0.lastPathComponent == "pyproject.toml" }!
    let text = try String(contentsOf: pyproject, encoding: .utf8)
    #expect(text.contains(#"name = "my-cool-tool""#))
    #expect(text.contains("typer>="))
    #expect(text.contains("rich>="))
    #expect(text.contains("httpx>="))
    #expect(text.contains("my-cool-tool = \"my_cool_tool.cli:app\""))
    #expect(text.contains("packages = [\"src/my_cool_tool\"]"))
}

@Test("PythonCLIBuilder: Hyphen-Slug wird zu Underscore-Python-Package")
func pythonPackageNaming() async throws {
    let parent = makeTmpParent()
    defer { cleanup(parent) }

    let outcome = try await PythonCLIBuilder().build(
        taskDescription: "t",
        slug: "mehr-als-ein-bindestrich",
        decomposition: pythonDecomp,
        outputParentDirectory: parent
    )

    let initPy = outcome.createdFiles.first {
        $0.path.contains("/src/mehr_als_ein_bindestrich/__init__.py")
    }
    #expect(initPy != nil, "Python-Package-Ordner mit Underscores nicht gefunden")
}

@Test("PythonCLIBuilder: cli.py enthaelt Typer-App und alle 3 Backends")
func cliContent() async throws {
    let parent = makeTmpParent()
    defer { cleanup(parent) }

    let outcome = try await PythonCLIBuilder().build(
        taskDescription: "Test",
        slug: "cli-test",
        decomposition: pythonDecomp,
        outputParentDirectory: parent
    )

    let cli = outcome.createdFiles.first { $0.lastPathComponent == "cli.py" }!
    let text = try String(contentsOf: cli, encoding: .utf8)
    #expect(text.contains("typer.Typer"))
    #expect(text.contains("@app.command()"))
    #expect(text.contains("def ask("))
    #expect(text.contains("def info()"))
    #expect(text.contains("from .llm_client import"))

    let llm = outcome.createdFiles.first { $0.lastPathComponent == "llm_client.py" }!
    let llmText = try String(contentsOf: llm, encoding: .utf8)
    #expect(llmText.contains("ANTHROPIC_API_KEY"))
    #expect(llmText.contains("OPENAI_API_KEY"))
    #expect(llmText.contains("GEMINI_API_KEY"))
}

@Test("PythonCLIBuilder: SYSTEM_PROMPT.md enthaelt Task und alle 6 Achsen")
func promptContent() async throws {
    let parent = makeTmpParent()
    defer { cleanup(parent) }

    let outcome = try await PythonCLIBuilder().build(
        taskDescription: "Meine spezifische Aufgabe 42",
        slug: "prompt-test",
        decomposition: pythonDecomp,
        outputParentDirectory: parent
    )

    let prompt = outcome.createdFiles.first { $0.lastPathComponent == "SYSTEM_PROMPT.md" }!
    let text = try String(contentsOf: prompt, encoding: .utf8)
    #expect(text.contains("Meine spezifische Aufgabe 42"))
    #expect(text.contains("terminal"))
    #expect(text.contains("hardTests"))
    #expect(text.contains("developers"))
}

@Test("PythonCLIBuilder: pythonPackageName faltet Hyphens zu Underscores")
func pythonPackageNameFunction() {
    #expect(PythonCLITemplates.pythonPackageName(from: "abc") == "abc")
    #expect(PythonCLITemplates.pythonPackageName(from: "a-b-c") == "a_b_c")
    #expect(PythonCLITemplates.pythonPackageName(from: "kein_underscore") == "kein_underscore")
}
