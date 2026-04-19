// RollbackCommand.swift
// `forge rollback <slug> <version>` — rollt System-Prompt auf eine
// frueher gespeicherte Version zurueck.

import ArgumentParser
import Foundation
import HarnessForgeCore

struct RollbackCommand: AsyncParsableCommand {
    static let configuration = CommandConfiguration(
        commandName: "rollback",
        abstract: "Rolle den Prompt eines Harness auf eine frueher gespeicherte Version zurueck"
    )

    @Argument(help: "Slug des Harness")
    var slug: String

    @Argument(help: "Version (Commit-Hash, 7+ Zeichen reichen). Ohne Angabe werden verfuegbare Versionen gelistet.")
    var version: String?

    @Option(name: .long, help: "Dateiname innerhalb des Harness (Default: SYSTEM_PROMPT.md)")
    var file: String = "SYSTEM_PROMPT.md"

    @Option(name: .long, help: "Basis-Verzeichnis (Default: ~/proggs)")
    var root: String = (NSHomeDirectory() as NSString).appendingPathComponent("proggs")

    mutating func run() async throws {
        let rootURL = URL(fileURLWithPath: (root as NSString).expandingTildeInPath)
        let repoURL = rootURL.appendingPathComponent(".harness-forge/prompts.git")
        let versioning = try await PromptVersioning(repoURL: repoURL)

        // Ohne version: Liste anzeigen
        guard let version else {
            let versions = try await versioning.listVersions(harnessSlug: slug, fileName: file)
            if versions.isEmpty {
                print("Keine Versionen fuer \(slug)/\(file) gefunden.")
                return
            }
            print("Verfuegbare Versionen fuer \(slug)/\(file):\n")
            for v in versions {
                print("  \(v.shortHash)  \(v.dateISO)  \(v.message)")
            }
            print("\nRollback mit: forge rollback \(slug) <hash>")
            return
        }

        let newHash = try await versioning.rollback(
            harnessSlug: slug,
            fileName: file,
            toVersion: version
        )

        // Datei auf Platte ebenfalls aktualisieren
        let content = try await versioning.getPrompt(
            harnessSlug: slug, fileName: file, atVersion: "HEAD"
        )
        let fileURL = rootURL.appendingPathComponent("\(slug)/\(file)")
        try content.write(to: fileURL, atomically: true, encoding: .utf8)

        print("Rollback auf \(String(version.prefix(7))) — neuer Commit: \(String(newHash.prefix(7)))")
        print("Datei aktualisiert: \(fileURL.path)")
    }
}
