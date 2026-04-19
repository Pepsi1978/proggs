// AnalyzeCommand.swift
// `forge analyze "<task>"` — Trockenlauf, nur Analyse + Matrix ausgeben.

import ArgumentParser
import Foundation
import HarnessForgeCore
import HarnessForgeBuilders

struct AnalyzeCommand: AsyncParsableCommand {
    static let configuration = CommandConfiguration(
        commandName: "analyze",
        abstract: "Nur Analyse + Entscheidungs-Matrix anzeigen, ohne zu bauen"
    )

    @Argument(help: "Aufgabe in Alltagssprache")
    var task: String

    mutating func run() async throws {
        let (client, label) = ClientFactory.make()
        print("Backend: \(label)\n")

        let analyzer = TaskAnalyzer(client: client)
        let result = try await analyzer.analyze(taskDescription: task)

        print("Slug: \(result.slug)")
        print("Empfehlung: \(result.recommendation.displayName)")
        print("Kosten: $\(String(format: "%.4f", result.totalCostUSD))\n")
        print(result.matrix.prettyPrint())
        print("")
        print(result.rationale)
    }
}
