// ListCommand.swift
// `forge list` — zeigt alle Ordner in ~/proggs/, die einen SYSTEM_PROMPT.md
// enthalten (unser Harness-Forge-Marker).

import ArgumentParser
import Foundation

struct ListCommand: AsyncParsableCommand {
    static let configuration = CommandConfiguration(
        commandName: "list",
        abstract: "Liste alle Harness-Forge-generierten Projekte in ~/proggs/"
    )

    @Option(name: .long, help: "Basis-Verzeichnis (Default: ~/proggs)")
    var root: String = (NSHomeDirectory() as NSString).appendingPathComponent("proggs")

    mutating func run() throws {
        let rootURL = URL(fileURLWithPath: (root as NSString).expandingTildeInPath)
        let contents = (try? FileManager.default.contentsOfDirectory(
            at: rootURL,
            includingPropertiesForKeys: [.isDirectoryKey]
        )) ?? []

        let harnessDirs = contents.filter { url in
            let marker = url.appendingPathComponent("SYSTEM_PROMPT.md")
            return FileManager.default.fileExists(atPath: marker.path)
        }.sorted { $0.lastPathComponent < $1.lastPathComponent }

        if harnessDirs.isEmpty {
            print("Keine Harness-Forge-Projekte in \(rootURL.path) gefunden.")
            print("Erzeuge deinen ersten mit: forge new \"<deine Aufgabe>\"")
            return
        }

        print("Gefundene Harness-Forge-Projekte in \(rootURL.path):\n")
        for dir in harnessDirs {
            let name = dir.lastPathComponent
            print("  - \(name)")
        }
        print("\nNutze: forge edit <name>   oder   forge rollback <name> <version>")
    }
}
