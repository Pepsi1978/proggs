// ClaudeSubagentBuilderTests.swift

import Testing
import Foundation
@testable import HarnessForgeBuilders

private func makeTmpParent() -> URL {
    let url = FileManager.default.temporaryDirectory
        .appendingPathComponent("harness-forge-subagent-test-\(UUID().uuidString)")
    try? FileManager.default.createDirectory(at: url, withIntermediateDirectories: true)
    return url
}
private func cleanup(_ url: URL) { try? FileManager.default.removeItem(at: url) }

private let subDecomp = TaskDecomposition(
    targetEnvironment: .terminal,
    interactivity: .ongoing,
    verifiability: .softCriteria,
    dataSources: .localFiles,
    targetUsers: .claudeUsers,
    offlineCapability: .canBeOnline,
    rationale: "Spezialist innerhalb Claude Code."
)

@Test("Subagent: erzeugt alle 6 Dateien in korrekter Struktur")
func subagentBuildsAllFiles() async throws {
    let parent = makeTmpParent(); defer { cleanup(parent) }
    let outcome = try await ClaudeSubagentBuilder().build(
        taskDescription: "Schreibe hilfreiche Commit-Messages",
        slug: "commit-helper",
        decomposition: subDecomp,
        outputParentDirectory: parent
    )
    #expect(outcome.createdFiles.count == 6)
    for f in outcome.createdFiles {
        #expect(FileManager.default.fileExists(atPath: f.path))
    }
}

@Test("Subagent: Agent-Datei hat gueltiges YAML-Frontmatter")
func agentFrontmatter() async throws {
    let parent = makeTmpParent(); defer { cleanup(parent) }
    let outcome = try await ClaudeSubagentBuilder().build(
        taskDescription: "Code-Reviewer fuer Swift",
        slug: "swift-reviewer",
        decomposition: subDecomp,
        outputParentDirectory: parent
    )
    let agent = outcome.createdFiles.first { $0.lastPathComponent == "swift-reviewer.md" }!
    let text = try String(contentsOf: agent, encoding: .utf8)
    #expect(text.hasPrefix("---\n"))
    #expect(text.contains("name: swift-reviewer"))
    #expect(text.contains("description:"))
    #expect(text.contains("model:"))
    #expect(text.contains("tools:"))
    #expect(text.contains("Code-Reviewer fuer Swift"))
}

@Test("Subagent: SKILL.md hat Aktivierungs-Phrasen")
func skillHasTriggers() async throws {
    let parent = makeTmpParent(); defer { cleanup(parent) }
    let outcome = try await ClaudeSubagentBuilder().build(
        taskDescription: "Spezialist fuer SQL-Abfragen-Optimierung",
        slug: "sql-optimizer",
        decomposition: subDecomp,
        outputParentDirectory: parent
    )
    let skill = outcome.createdFiles.first { $0.path.contains("/skills/sql-optimizer/SKILL.md") }!
    let text = try String(contentsOf: skill, encoding: .utf8)
    #expect(text.contains("Aktivierungs-Phrasen"))
    #expect(text.contains("sql-optimizer"))
    #expect(text.contains("nutze sql-optimizer"))
}

@Test("Subagent: install.sh ist ausfuehrbar und referenziert ~/.claude/")
func installScript() async throws {
    let parent = makeTmpParent(); defer { cleanup(parent) }
    let outcome = try await ClaudeSubagentBuilder().build(
        taskDescription: "t", slug: "install-test",
        decomposition: subDecomp, outputParentDirectory: parent
    )
    let install = outcome.createdFiles.first { $0.lastPathComponent == "install.sh" }!
    let text = try String(contentsOf: install, encoding: .utf8)
    #expect(text.contains("$HOME/.claude"))
    #expect(text.contains("SLUG=\"install-test\""))
    #expect(text.contains("agents/${SLUG}.md"))
    #expect(text.contains("skills/${SLUG}/SKILL.md"))

    // POSIX-Permissions: 0o755 (rwxr-xr-x)
    let attrs = try FileManager.default.attributesOfItem(atPath: install.path)
    let perms = attrs[.posixPermissions] as? Int
    #expect(perms == 0o755)
}

@Test("Subagent: tools-Hint variiert nach Datenquellen")
func toolsHintVariation() async throws {
    let parent = makeTmpParent(); defer { cleanup(parent) }

    let webDecomp = TaskDecomposition(
        targetEnvironment: .terminal, interactivity: .ongoing,
        verifiability: .softCriteria, dataSources: .web,
        targetUsers: .claudeUsers, offlineCapability: .canBeOnline,
        rationale: "Web-Research-Bot"
    )

    let outcome = try await ClaudeSubagentBuilder().build(
        taskDescription: "t", slug: "web-test",
        decomposition: webDecomp, outputParentDirectory: parent
    )
    let agent = outcome.createdFiles.first { $0.lastPathComponent == "web-test.md" }!
    let text = try String(contentsOf: agent, encoding: .utf8)
    #expect(text.contains("WebFetch"))
    #expect(text.contains("WebSearch"))
}

@Test("Subagent: shortDescription kuerzt auf max-Laenge")
func shortDescFunction() {
    let long = String(repeating: "a", count: 500)
    let short = ClaudeSubagentTemplates.shortDescription(from: long, max: 50)
    #expect(short.count <= 50)
}
