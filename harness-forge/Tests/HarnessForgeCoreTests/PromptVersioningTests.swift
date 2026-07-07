// PromptVersioningTests.swift
// Integrationstests mit echtem `git`-Binary im tmp-Ordner.

import Testing
import Foundation
@testable import HarnessForgeCore

private func tmpRepoURL() -> URL {
    FileManager.default.temporaryDirectory
        .appendingPathComponent("forge-prompts-\(UUID().uuidString)")
}

@Test("Versioning: init erzeugt .git und initialen Commit")
func initCreatesGit() async throws {
    let url = tmpRepoURL()
    defer { try? FileManager.default.removeItem(at: url) }

    let v = try await PromptVersioning(repoURL: url)
    let gitDir = url.appendingPathComponent(".git")
    #expect(FileManager.default.fileExists(atPath: gitDir.path))

    let head = try await v.currentHead()
    #expect(head.count > 10, "HEAD muss ein gueltiger Commit-Hash sein")
}

@Test("Versioning: savePrompt erzeugt Datei + Commit")
func saveCreatesCommit() async throws {
    let url = tmpRepoURL()
    defer { try? FileManager.default.removeItem(at: url) }

    let v = try await PromptVersioning(repoURL: url)
    let hash = try await v.savePrompt(
        harnessSlug: "test",
        fileName: "SYSTEM_PROMPT.md",
        content: "# Version 1",
        message: "first version"
    )

    #expect(hash.count > 10)
    let fileURL = url.appendingPathComponent("test/SYSTEM_PROMPT.md")
    #expect(FileManager.default.fileExists(atPath: fileURL.path))
}

@Test("Versioning: listVersions gibt alle Commits zurueck")
func listVersions() async throws {
    let url = tmpRepoURL()
    defer { try? FileManager.default.removeItem(at: url) }

    let v = try await PromptVersioning(repoURL: url)
    _ = try await v.savePrompt(
        harnessSlug: "app", fileName: "P.md",
        content: "Version A", message: "v1"
    )
    try await Task.sleep(for: .milliseconds(1100)) // git-Zeitaufloesung: 1s
    _ = try await v.savePrompt(
        harnessSlug: "app", fileName: "P.md",
        content: "Version B", message: "v2"
    )

    let versions = try await v.listVersions(harnessSlug: "app", fileName: "P.md")
    #expect(versions.count == 2)
    #expect(versions[0].message == "v2", "neueste zuerst")
    #expect(versions[1].message == "v1")
    #expect(versions[0].shortHash.count == 7)
}

@Test("Versioning: getPrompt bei beliebigen Versionen")
func getPromptAtVersion() async throws {
    let url = tmpRepoURL()
    defer { try? FileManager.default.removeItem(at: url) }

    let v = try await PromptVersioning(repoURL: url)
    _ = try await v.savePrompt(
        harnessSlug: "x", fileName: "f.md",
        content: "ALPHA", message: "a"
    )
    try await Task.sleep(for: .milliseconds(1100))
    _ = try await v.savePrompt(
        harnessSlug: "x", fileName: "f.md",
        content: "BETA", message: "b"
    )

    let versions = try await v.listVersions(harnessSlug: "x", fileName: "f.md")
    let first = try await v.getPrompt(harnessSlug: "x", fileName: "f.md", atVersion: versions[1].hash)
    let second = try await v.getPrompt(harnessSlug: "x", fileName: "f.md", atVersion: versions[0].hash)
    #expect(first == "ALPHA")
    #expect(second == "BETA")
}

@Test("Versioning: diff zeigt Aenderungen zwischen Versionen")
func diffBetweenVersions() async throws {
    let url = tmpRepoURL()
    defer { try? FileManager.default.removeItem(at: url) }

    let v = try await PromptVersioning(repoURL: url)
    _ = try await v.savePrompt(
        harnessSlug: "d", fileName: "p.md",
        content: "hello world\n", message: "first"
    )
    try await Task.sleep(for: .milliseconds(1100))
    _ = try await v.savePrompt(
        harnessSlug: "d", fileName: "p.md",
        content: "hello universe\n", message: "second"
    )

    let versions = try await v.listVersions(harnessSlug: "d", fileName: "p.md")
    let diff = try await v.diff(
        harnessSlug: "d", fileName: "p.md",
        fromVersion: versions[1].hash,
        toVersion: versions[0].hash
    )
    #expect(diff.contains("-hello world"))
    #expect(diff.contains("+hello universe"))
}

@Test("Versioning: rollback stellt alten Inhalt wieder her und erzeugt neuen Commit")
func rollbackCreatesNewCommit() async throws {
    let url = tmpRepoURL()
    defer { try? FileManager.default.removeItem(at: url) }

    let v = try await PromptVersioning(repoURL: url)
    _ = try await v.savePrompt(harnessSlug: "r", fileName: "p.md", content: "ORIGINAL", message: "v1")
    try await Task.sleep(for: .milliseconds(1100))
    _ = try await v.savePrompt(harnessSlug: "r", fileName: "p.md", content: "CHANGED", message: "v2")
    try await Task.sleep(for: .milliseconds(1100))

    let versions = try await v.listVersions(harnessSlug: "r", fileName: "p.md")
    let original = versions[1].hash

    _ = try await v.rollback(harnessSlug: "r", fileName: "p.md", toVersion: original)

    let after = try await v.getPrompt(harnessSlug: "r", fileName: "p.md", atVersion: "HEAD")
    #expect(after == "ORIGINAL")

    let finalVersions = try await v.listVersions(harnessSlug: "r", fileName: "p.md")
    #expect(finalVersions.count == 3, "Rollback-Commit muss History erweitern, nicht verkuerzen")
    #expect(finalVersions[0].message.contains("Rollback"))
}

@Test("DiffViewer: diffWithPrevious liefert nil bei <2 Versionen")
func diffWithPreviousEdgeCase() async throws {
    let url = tmpRepoURL()
    defer { try? FileManager.default.removeItem(at: url) }

    let v = try await PromptVersioning(repoURL: url)
    let viewer = DiffViewer(versioning: v)
    _ = try await v.savePrompt(harnessSlug: "q", fileName: "p.md", content: "one", message: "v1")

    let d = try await viewer.diffWithPrevious(harnessSlug: "q", fileName: "p.md")
    #expect(d == nil)
}

@Test("RollbackEngine: availableVersions liefert alle Commits")
func rollbackEngineListsVersions() async throws {
    let url = tmpRepoURL()
    defer { try? FileManager.default.removeItem(at: url) }

    let v = try await PromptVersioning(repoURL: url)
    let engine = RollbackEngine(versioning: v)
    _ = try await v.savePrompt(harnessSlug: "e", fileName: "p.md", content: "a", message: "v1")
    try await Task.sleep(for: .milliseconds(1100))
    _ = try await v.savePrompt(harnessSlug: "e", fileName: "p.md", content: "b", message: "v2")

    let versions = try await engine.availableVersions(harnessSlug: "e", fileName: "p.md")
    #expect(versions.count == 2)
}
