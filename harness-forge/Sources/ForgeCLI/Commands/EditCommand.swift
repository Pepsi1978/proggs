// EditCommand.swift
// `forge edit <slug>` — oeffnet die SYSTEM_PROMPT.md im $EDITOR und
// committet Aenderungen in die Prompt-Versionierung.

import ArgumentParser
import Foundation
import HarnessForgeCore

struct EditCommand: AsyncParsableCommand {
    static let configuration = CommandConfiguration(
        commandName: "edit",
        abstract: "Bearbeite den System-Prompt eines Harness und versioniere die Aenderung"
    )

    @Argument(help: "Slug des Harness, z.B. 'mein-harness'")
    var slug: String

    @Option(name: .long, help: "Dateiname innerhalb des Harness (Default: SYSTEM_PROMPT.md)")
    var file: String = "SYSTEM_PROMPT.md"

    @Option(name: .long, help: "Basis-Verzeichnis (Default: ~/proggs)")
    var root: String = (NSHomeDirectory() as NSString).appendingPathComponent("proggs")

    mutating func run() async throws {
        let rootURL = URL(fileURLWithPath: (root as NSString).expandingTildeInPath)
        let fileURL = rootURL.appendingPathComponent("\(slug)/\(file)")

        guard FileManager.default.fileExists(atPath: fileURL.path) else {
            throw CleanExit.message("Datei nicht gefunden: \(fileURL.path)")
        }

        let before = try String(contentsOf: fileURL, encoding: .utf8)

        // $EDITOR auswaehlen (nano ist safer default als vim)
        let editor = ProcessInfo.processInfo.environment["EDITOR"] ?? "nano"

        let proc = Process()
        proc.executableURL = URL(fileURLWithPath: "/usr/bin/env")
        proc.arguments = [editor, fileURL.path]
        proc.standardInput = FileHandle.standardInput
        proc.standardOutput = FileHandle.standardOutput
        proc.standardError = FileHandle.standardError
        try proc.run()
        proc.waitUntilExit()

        guard proc.terminationStatus == 0 else {
            throw CleanExit.message("Editor '\(editor)' beendete sich mit Status \(proc.terminationStatus)")
        }

        let after = try String(contentsOf: fileURL, encoding: .utf8)
        if before == after {
            print("Keine Aenderung — nichts committed.")
            return
        }

        // In Prompt-Versioning ablegen
        let repoURL = rootURL.appendingPathComponent(".harness-forge/prompts.git")
        let versioning = try await PromptVersioning(repoURL: repoURL)
        let hash = try await versioning.savePrompt(
            harnessSlug: slug,
            fileName: file,
            content: after,
            message: "Edit via CLI (\(file))"
        )
        print("Gespeichert als Version \(String(hash.prefix(7)))")
    }
}
