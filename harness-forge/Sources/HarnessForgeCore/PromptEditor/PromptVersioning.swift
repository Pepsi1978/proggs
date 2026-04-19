// PromptVersioning.swift
// Versioniert Prompts via eigenem Git-Repo (`.forge/prompts.git`).
// Ruft `git` ueber Process auf — kein SwiftGit2-Binding noetig, funktioniert
// mit jeder git-Installation (macOS Command-Line-Tools reichen).

import Foundation

public actor PromptVersioning {

    // MARK: - Public Types

    public struct Version: Sendable, Equatable {
        public let hash: String
        public let shortHash: String
        public let dateISO: String
        public let message: String

        public init(hash: String, dateISO: String, message: String) {
            self.hash = hash
            self.shortHash = String(hash.prefix(7))
            self.dateISO = dateISO
            self.message = message
        }
    }

    public enum VersioningError: Error, LocalizedError, Sendable {
        case gitNotFound
        case initFailed(String)
        case gitFailed(command: String, stderr: String, exitCode: Int32)
        case versionNotFound(String)

        public var errorDescription: String? {
            switch self {
            case .gitNotFound:
                "Git-Binary nicht gefunden"
            case .initFailed(let msg):
                "Konnte Prompt-Repo nicht initialisieren: \(msg)"
            case .gitFailed(let cmd, let err, let code):
                "git \(cmd) schlug fehl (exit \(code)): \(err.prefix(200))"
            case .versionNotFound(let v):
                "Version nicht gefunden: \(v)"
            }
        }
    }

    // MARK: - State

    public let repoURL: URL

    public init(repoURL: URL) async throws {
        self.repoURL = repoURL
        try await ensureInitialized()
    }

    // MARK: - Initialisierung

    private func ensureInitialized() async throws {
        let gitDir = repoURL.appendingPathComponent(".git")
        if FileManager.default.fileExists(atPath: gitDir.path) {
            return
        }
        do {
            try FileManager.default.createDirectory(at: repoURL, withIntermediateDirectories: true)
        } catch {
            throw VersioningError.initFailed(error.localizedDescription)
        }
        _ = try runGit(["init", "-q"])
        _ = try runGit(["config", "user.email", "harness-forge@local"])
        _ = try runGit(["config", "user.name", "Harness Forge"])
        // Initialer Commit, damit rev-parse HEAD spaeter funktioniert
        let readmeURL = repoURL.appendingPathComponent("README.md")
        try "# Harness Forge Prompt-Repo\n".write(to: readmeURL, atomically: true, encoding: .utf8)
        _ = try runGit(["add", "README.md"])
        _ = try runGit(["commit", "-q", "-m", "Initial commit"])
    }

    // MARK: - Speichern

    /// Speichert einen Prompt und erzeugt einen Commit. Liefert die Commit-Hash zurueck.
    @discardableResult
    public func savePrompt(
        harnessSlug: String,
        fileName: String,
        content: String,
        message: String
    ) async throws -> String {
        let harnessDir = repoURL.appendingPathComponent(harnessSlug)
        try FileManager.default.createDirectory(
            at: harnessDir,
            withIntermediateDirectories: true
        )
        let fileURL = harnessDir.appendingPathComponent(fileName)
        try content.write(to: fileURL, atomically: true, encoding: .utf8)

        let relPath = "\(harnessSlug)/\(fileName)"
        _ = try runGit(["add", relPath])

        // `commit --allow-empty` nicht noetig — wenn sich nichts geaendert hat,
        // ignoriert git den Call ohne Fehler in neueren Versionen; wir fangen
        // den "nothing to commit"-Fehler weich ab.
        let result = tryCommit(message: message)
        switch result {
        case .committed(let hash): return hash
        case .noChanges:
            // Aktuellen HEAD zurueckgeben
            return (try runGit(["rev-parse", "HEAD"])).trimmingCharacters(in: .whitespacesAndNewlines)
        }
    }

    private enum CommitResult { case committed(String), noChanges }

    private func tryCommit(message: String) -> CommitResult {
        do {
            _ = try runGit(["commit", "-q", "-m", message])
            let hash = try runGit(["rev-parse", "HEAD"]).trimmingCharacters(in: .whitespacesAndNewlines)
            return .committed(hash)
        } catch VersioningError.gitFailed(_, let stderr, _) where stderr.contains("nothing to commit") {
            return .noChanges
        } catch {
            return .noChanges
        }
    }

    // MARK: - Lesen

    public func listVersions(harnessSlug: String, fileName: String) async throws -> [Version] {
        let relPath = "\(harnessSlug)/\(fileName)"
        let output = try runGit([
            "log",
            "--pretty=format:%H|%cI|%s",
            "--follow",
            "--",
            relPath,
        ])
        return output
            .split(separator: "\n", omittingEmptySubsequences: true)
            .compactMap { line in
                let parts = line.split(separator: "|", maxSplits: 2, omittingEmptySubsequences: false)
                guard parts.count == 3 else { return nil }
                return Version(
                    hash: String(parts[0]),
                    dateISO: String(parts[1]),
                    message: String(parts[2])
                )
            }
    }

    public func getPrompt(harnessSlug: String, fileName: String, atVersion: String) async throws -> String {
        let spec = "\(atVersion):\(harnessSlug)/\(fileName)"
        do {
            return try runGit(["show", spec])
        } catch {
            throw VersioningError.versionNotFound(spec)
        }
    }

    public func currentHead() async throws -> String {
        try runGit(["rev-parse", "HEAD"]).trimmingCharacters(in: .whitespacesAndNewlines)
    }

    // MARK: - Diff

    public func diff(
        harnessSlug: String,
        fileName: String,
        fromVersion: String,
        toVersion: String = "HEAD"
    ) async throws -> String {
        let relPath = "\(harnessSlug)/\(fileName)"
        return try runGit(["diff", fromVersion, toVersion, "--", relPath])
    }

    // MARK: - Rollback

    /// Ueberschreibt die aktuelle Datei mit dem Inhalt aus `version` und
    /// erzeugt einen "Rollback"-Commit. So bleibt die History erhalten.
    public func rollback(
        harnessSlug: String,
        fileName: String,
        toVersion version: String
    ) async throws -> String {
        let content = try await getPrompt(
            harnessSlug: harnessSlug,
            fileName: fileName,
            atVersion: version
        )
        return try await savePrompt(
            harnessSlug: harnessSlug,
            fileName: fileName,
            content: content,
            message: "Rollback auf \(String(version.prefix(7)))"
        )
    }

    // MARK: - Git-Runner

    @discardableResult
    private func runGit(_ args: [String]) throws -> String {
        let process = Process()
        process.executableURL = URL(fileURLWithPath: "/usr/bin/env")
        process.arguments = ["git"] + args
        process.currentDirectoryURL = repoURL

        let stdout = Pipe()
        let stderr = Pipe()
        process.standardOutput = stdout
        process.standardError = stderr

        do {
            try process.run()
        } catch {
            throw VersioningError.gitNotFound
        }
        process.waitUntilExit()

        let outData = stdout.fileHandleForReading.readDataToEndOfFile()
        let errData = stderr.fileHandleForReading.readDataToEndOfFile()
        let outString = String(data: outData, encoding: .utf8) ?? ""
        let errString = String(data: errData, encoding: .utf8) ?? ""

        if process.terminationStatus != 0 {
            throw VersioningError.gitFailed(
                command: args.joined(separator: " "),
                stderr: errString,
                exitCode: process.terminationStatus
            )
        }
        return outString
    }
}
