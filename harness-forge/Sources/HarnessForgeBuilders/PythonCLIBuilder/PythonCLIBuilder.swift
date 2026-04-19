// PythonCLIBuilder.swift
// Generiert ein vollstaendiges Python-CLI-Projekt mit Typer + Rich + httpx.

import Foundation

public struct PythonCLIBuilder: HarnessBuilder {

    public let type: HarnessType = .pythonCLI

    public init() {}

    public func build(
        taskDescription: String,
        slug: String,
        decomposition: TaskDecomposition,
        outputParentDirectory: URL
    ) async throws -> BuildOutcome {
        let projectDir = try BuilderHelpers.createProjectDirectory(
            parent: outputParentDirectory,
            slug: slug
        )

        let pyPackage = PythonCLITemplates.pythonPackageName(from: slug)
        let srcDir = projectDir.appendingPathComponent("src/\(pyPackage)")

        do {
            try FileManager.default.createDirectory(
                at: srcDir,
                withIntermediateDirectories: true
            )
        } catch {
            throw BuilderError.cannotCreateDirectory(srcDir, underlyingError: error.localizedDescription)
        }

        // Alle Dateien zusammenstellen
        let files: [(URL, String)] = [
            (projectDir.appendingPathComponent("pyproject.toml"),
             PythonCLITemplates.pyprojectToml(slug: slug, pyPackage: pyPackage, taskDescription: taskDescription)),

            (projectDir.appendingPathComponent("README.md"),
             PythonCLITemplates.readmeMd(slug: slug, taskDescription: taskDescription, decomposition: decomposition)),

            (projectDir.appendingPathComponent("SYSTEM_PROMPT.md"),
             PythonCLITemplates.systemPromptMd(taskDescription: taskDescription, slug: slug, decomposition: decomposition)),

            (projectDir.appendingPathComponent(".gitignore"),
             PythonCLITemplates.gitignore()),

            (srcDir.appendingPathComponent("__init__.py"),
             PythonCLITemplates.initPy(slug: slug)),

            (srcDir.appendingPathComponent("__main__.py"),
             PythonCLITemplates.mainPy()),

            (srcDir.appendingPathComponent("cli.py"),
             PythonCLITemplates.cliPy(slug: slug, pyPackage: pyPackage, taskDescription: taskDescription)),

            (srcDir.appendingPathComponent("llm_client.py"),
             PythonCLITemplates.llmClientPy()),
        ]

        for (url, content) in files {
            try BuilderHelpers.writeFile(at: url, content: content)
        }

        let totalBytes = files.reduce(0) { $0 + $1.1.utf8.count }

        return BuildOutcome(
            harnessType: type,
            slug: slug,
            projectDirectory: projectDir,
            createdFiles: files.map(\.0),
            summary: """
            Python-CLI-Harness '\(slug)' erzeugt.
            - Python-Paket: \(pyPackage)
            - 8 Dateien, \(totalBytes) Bytes total
            - Installation: `cd \(slug) && pip install -e .`
            - Nutzung: `\(slug) ask "Deine Frage"`
            """
        )
    }
}
