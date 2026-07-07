// PurePromptBuilder.swift
// Der einfachste Harness-Builder. Erzeugt einen Ordner mit zwei Dateien:
// SYSTEM_PROMPT.md und README.md. Keine LLM-Calls, rein deterministisch.

import Foundation

public struct PurePromptBuilder: HarnessBuilder {

    public let type: HarnessType = .purePrompt

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

        let promptURL = projectDir.appendingPathComponent("SYSTEM_PROMPT.md")
        let readmeURL = projectDir.appendingPathComponent("README.md")

        let promptContent = PromptTemplate.render(
            taskDescription: taskDescription,
            slug: slug,
            decomposition: decomposition
        )
        let readmeContent = ReadmeTemplate.render(
            taskDescription: taskDescription,
            slug: slug,
            decomposition: decomposition
        )

        try BuilderHelpers.writeFile(at: promptURL, content: promptContent)
        try BuilderHelpers.writeFile(at: readmeURL, content: readmeContent)

        let summary = """
        Pure-Prompt-Harness '\(slug)' erzeugt.
        - SYSTEM_PROMPT.md (\(promptContent.count) Zeichen)
        - README.md (\(readmeContent.count) Zeichen)
        """

        return BuildOutcome(
            harnessType: type,
            slug: slug,
            projectDirectory: projectDir,
            createdFiles: [promptURL, readmeURL],
            summary: summary
        )
    }
}
