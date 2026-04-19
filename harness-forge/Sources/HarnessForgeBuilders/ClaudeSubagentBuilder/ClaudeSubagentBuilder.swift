// ClaudeSubagentBuilder.swift
// Erzeugt ein Claude-Code-Subagent + Skill-Bundle zum Installieren in ~/.claude/.

import Foundation

public struct ClaudeSubagentBuilder: HarnessBuilder {

    public let type: HarnessType = .claudeSubagent

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
        let agentsDir = projectDir.appendingPathComponent("agents")
        let skillDir = projectDir.appendingPathComponent("skills/\(slug)")

        for dir in [agentsDir, skillDir] {
            do {
                try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
            } catch {
                throw BuilderError.cannotCreateDirectory(dir, underlyingError: error.localizedDescription)
            }
        }

        let agentURL = agentsDir.appendingPathComponent("\(slug).md")
        let skillURL = skillDir.appendingPathComponent("SKILL.md")
        let installURL = projectDir.appendingPathComponent("install.sh")

        let files: [(URL, String)] = [
            (agentURL,
             ClaudeSubagentTemplates.agentMd(slug: slug, taskDescription: taskDescription, decomposition: decomposition)),
            (skillURL,
             ClaudeSubagentTemplates.skillMd(slug: slug, taskDescription: taskDescription, decomposition: decomposition)),
            (installURL,
             ClaudeSubagentTemplates.installSh(slug: slug)),
            (projectDir.appendingPathComponent("README.md"),
             ClaudeSubagentTemplates.readmeMd(slug: slug, taskDescription: taskDescription)),
            (projectDir.appendingPathComponent("SYSTEM_PROMPT.md"),
             ClaudeSubagentTemplates.systemPromptMd(taskDescription: taskDescription, slug: slug, decomposition: decomposition)),
            (projectDir.appendingPathComponent(".gitignore"),
             ClaudeSubagentTemplates.gitignore()),
        ]

        for (url, content) in files {
            try BuilderHelpers.writeFile(at: url, content: content)
        }

        // install.sh ausfuehrbar machen
        let attrs: [FileAttributeKey: Any] = [.posixPermissions: 0o755]
        try? FileManager.default.setAttributes(attrs, ofItemAtPath: installURL.path)

        return BuildOutcome(
            harnessType: type,
            slug: slug,
            projectDirectory: projectDir,
            createdFiles: files.map(\.0),
            summary: """
            Claude-Subagent-Bundle '\(slug)' erzeugt.
            - agents/\(slug).md (Agent-Definition)
            - skills/\(slug)/SKILL.md (Aktivierungs-Skill)
            - install.sh (installiert nach ~/.claude/)
            - Nutzung: `cd \(slug) && bash install.sh`
            """
        )
    }
}
