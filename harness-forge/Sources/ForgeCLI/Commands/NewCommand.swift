// NewCommand.swift
// `forge new "<task>"` — die volle Pipeline:
//   Analyse → Entscheidung → Bau → Pfad-Ausgabe.

import ArgumentParser
import Foundation
import HarnessForgeCore
import HarnessForgeBuilders

struct NewCommand: AsyncParsableCommand {
    static let configuration = CommandConfiguration(
        commandName: "new",
        abstract: "Erzeuge einen neuen KI-Harness aus einer Aufgabenbeschreibung"
    )

    @Argument(help: "Aufgabe in Alltagssprache — ein Satz genuegt")
    var task: String

    @Option(name: .long, help: "Erzwinge einen bestimmten Harness-Typ (ueberschreibt die Empfehlung)")
    var type: HarnessType?

    @Option(name: .long, help: "Ausgabe-Verzeichnis (Default: ~/proggs)")
    var output: String = (NSHomeDirectory() as NSString).appendingPathComponent("proggs")

    @Flag(name: .long, help: "Nur Analyse anzeigen, nicht bauen")
    var analyzeOnly: Bool = false

    mutating func run() async throws {
        let (client, backendLabel) = ClientFactory.make()
        print("Backend: \(backendLabel)")
        print("Analysiere Aufgabe ...\n")

        let analyzer = TaskAnalyzer(client: client)
        let result = try await analyzer.analyze(taskDescription: task)

        printMatrix(result)

        guard !analyzeOnly else { return }

        let chosenType = type ?? result.recommendation
        let builder = selectBuilder(for: chosenType)

        let outputURL = URL(fileURLWithPath: (output as NSString).expandingTildeInPath)
        print("\nBaue \(chosenType.displayName) unter \(outputURL.path)/\(result.slug) ...")

        let outcome = try await builder.build(
            taskDescription: result.taskDescription,
            slug: result.slug,
            decomposition: result.decomposition,
            outputParentDirectory: outputURL
        )

        print("\nFertig.\n")
        print(outcome.summary)
        print("\nProjektpfad: \(outcome.projectDirectory.path)")
    }

    // MARK: - Ausgabe-Helfer

    private func printMatrix(_ result: TaskAnalyzer.AnalysisResult) {
        print("Slug: \(result.slug)")
        print("Empfehlung: \(result.recommendation.displayName)")
        print("Kosten der Analyse: $\(String(format: "%.4f", result.totalCostUSD))")
        print("")
        print(result.matrix.prettyPrint())
        print("")
        print(result.rationale)
    }

    // MARK: - Builder-Auswahl

    private func selectBuilder(for type: HarnessType) -> any HarnessBuilder {
        switch type {
        case .purePrompt: PurePromptBuilder()
        case .pythonCLI: PythonCLIBuilder()
        case .tauriDesktop: TauriDesktopBuilder()
        case .androidKotlin: AndroidKotlinBuilder()
        case .claudeSubagent: ClaudeSubagentBuilder()
        }
    }
}
