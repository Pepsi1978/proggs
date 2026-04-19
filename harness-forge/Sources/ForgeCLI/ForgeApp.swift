// ForgeApp.swift
// Root-Command der CLI. Delegiert an die Subcommands in Commands/.

import ArgumentParser
import HarnessForgeCore

@main
struct ForgeApp: AsyncParsableCommand {
    static let configuration = CommandConfiguration(
        commandName: "forge",
        abstract: HarnessForgeCore.tagline,
        discussion: """
            Harness Forge ist ein Meta-Harness-Builder fuer macOS. Beschreibe
            eine Aufgabe in Alltagssprache, und das Werkzeug entscheidet,
            welche Harness-Form (App, CLI, Prompt, Subagent, Desktop) am
            besten passt — und baut sie vollstaendig.
            """,
        version: HarnessForgeCore.version,
        subcommands: [
            NewCommand.self,
            AnalyzeCommand.self,
            ListCommand.self,
            EditCommand.self,
            RollbackCommand.self,
        ],
        defaultSubcommand: nil
    )
}
