// ForgeApp.swift
// Einstiegspunkt des CLI-Frontends.
//
// Aktuell ein absichtlich minimales Geruest: zeigt Version und Tagline, die
// eigentlichen Kommandos (new, list, edit, rollback) werden in Step 12
// hinzugefuegt. Bis dahin dient dieser Stub als Smoke-Test, dass das
// Executable-Target korrekt verdrahtet ist und `swift run forge --version`
// funktioniert.

import ArgumentParser
import HarnessForgeCore

@main
struct ForgeApp: ParsableCommand {
    static let configuration = CommandConfiguration(
        commandName: "forge",
        abstract: HarnessForgeCore.tagline,
        discussion: """
            Harness Forge ist ein Meta-Harness-Builder fuer macOS. Beschreibe
            eine Aufgabe in Alltagssprache, und das Werkzeug entscheidet,
            welche Harness-Form (App, CLI, Prompt, …) am besten passt — und
            baut sie vollstaendig.

            In diesem Step 1 ist das Geruest angelegt; die Kommandos selbst
            folgen ab Step 12.
            """,
        version: HarnessForgeCore.version
    )

    mutating func run() throws {
        let banner = """

        \(HarnessForgeCore.displayName) v\(HarnessForgeCore.version)
        \(HarnessForgeCore.tagline)

        Naechste Kommandos (folgen ab Step 12):
          forge new "<Aufgabe>"      Neuen Harness erzeugen
          forge list                 Alle erzeugten Harnesse auflisten
          forge edit <id>            Prompt eines Harnesses bearbeiten
          forge rollback <id> <ver>  Prompt-Version zuruecksetzen

        Vorerst nutzbar:
          forge --version            Version anzeigen
          forge --help               Diese Hilfe

        """
        print(banner)
    }
}
