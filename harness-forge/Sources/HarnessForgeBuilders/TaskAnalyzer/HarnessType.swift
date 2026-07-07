// HarnessType.swift
// Die 5 moeglichen Harness-Formen + Eignungs-Bewertung (1..5) pro Achsen-Wert.
// Die Scoring-Matrix ist bewusst tabellarisch — so lassen sich Praeferenzen
// zentral und nachvollziehbar einstellen, ohne verstreuten Heuristik-Code.

import Foundation

public enum HarnessType: String, Sendable, Codable, CaseIterable {
    case androidKotlin = "android_kotlin"
    case tauriDesktop = "tauri_desktop"
    case pythonCLI = "python_cli"
    case purePrompt = "pure_prompt"
    case claudeSubagent = "claude_subagent"

    public var displayName: String {
        switch self {
        case .androidKotlin: "Android (Kotlin + Compose)"
        case .tauriDesktop: "Desktop (Tauri v2)"
        case .pythonCLI: "Python CLI"
        case .purePrompt: "Reiner System-Prompt"
        case .claudeSubagent: "Claude-Subagent"
        }
    }

    public var shortDescription: String {
        switch self {
        case .androidKotlin: "Handy-App fuer unterwegs, Sensoren, Offline-Nutzung"
        case .tauriDesktop: "Desktop-App mit grosser Oberflaeche, Dateisystem, lokale Modelle"
        case .pythonCLI: "Terminal-Tool fuer Automatisierung und Entwickler-Workflows"
        case .purePrompt: "Reiner Markdown-Prompt fuer Denkaufgaben ohne Tools"
        case .claudeSubagent: "Spezialist innerhalb von Claude Code"
        }
    }

    // MARK: - Scoring

    public func score(for env: TargetEnvironment) -> Int {
        switch (self, env) {
        case (.androidKotlin, .mobile): 5
        case (.androidKotlin, .embedded): 3
        case (.androidKotlin, .cloud): 2
        case (.androidKotlin, _): 1

        case (.tauriDesktop, .desktop): 5
        case (.tauriDesktop, .cloud), (.tauriDesktop, .embedded): 2
        case (.tauriDesktop, .terminal): 2
        case (.tauriDesktop, _): 1

        case (.pythonCLI, .terminal): 5
        case (.pythonCLI, .cloud): 3
        case (.pythonCLI, .desktop): 2
        case (.pythonCLI, _): 1

        case (.purePrompt, .promptOnly): 5
        case (.purePrompt, _): 2

        case (.claudeSubagent, .terminal): 5
        case (.claudeSubagent, .cloud), (.claudeSubagent, .promptOnly): 3
        case (.claudeSubagent, _): 1
        }
    }

    public func score(for inter: Interactivity) -> Int {
        switch (self, inter) {
        case (.androidKotlin, .ongoing): 5
        case (.androidKotlin, .background): 4
        case (.androidKotlin, .oneShot): 2

        case (.tauriDesktop, .ongoing): 5
        case (.tauriDesktop, .background): 4
        case (.tauriDesktop, .oneShot): 3

        case (.pythonCLI, .oneShot): 5
        case (.pythonCLI, .background): 4
        case (.pythonCLI, .ongoing): 3

        case (.purePrompt, .oneShot): 5
        case (.purePrompt, .ongoing): 2
        case (.purePrompt, .background): 1

        case (.claudeSubagent, .oneShot), (.claudeSubagent, .ongoing): 4
        case (.claudeSubagent, .background): 3
        }
    }

    public func score(for ver: Verifiability) -> Int {
        switch (self, ver) {
        case (.androidKotlin, .hardTests), (.androidKotlin, .softCriteria): 4
        case (.androidKotlin, .subjective): 3

        case (.tauriDesktop, .hardTests), (.tauriDesktop, .softCriteria): 4
        case (.tauriDesktop, .subjective): 3

        case (.pythonCLI, .hardTests): 5
        case (.pythonCLI, .softCriteria): 4
        case (.pythonCLI, .subjective): 3

        case (.purePrompt, .hardTests): 1
        case (.purePrompt, .softCriteria): 3
        case (.purePrompt, .subjective): 5

        case (.claudeSubagent, .hardTests): 3
        case (.claudeSubagent, .softCriteria): 4
        case (.claudeSubagent, .subjective): 3
        }
    }

    public func score(for data: DataSources) -> Int {
        switch (self, data) {
        case (.androidKotlin, .sensors): 5
        case (.androidKotlin, .apis), (.androidKotlin, .web): 4
        case (.androidKotlin, .localFiles): 3
        case (.androidKotlin, .onlyLLM): 2

        case (.tauriDesktop, .localFiles), (.tauriDesktop, .web), (.tauriDesktop, .apis): 5
        case (.tauriDesktop, .onlyLLM): 2
        case (.tauriDesktop, .sensors): 2

        case (.pythonCLI, .localFiles), (.pythonCLI, .apis): 5
        case (.pythonCLI, .web): 4
        case (.pythonCLI, .onlyLLM): 3
        case (.pythonCLI, .sensors): 1

        case (.purePrompt, .onlyLLM): 5
        case (.purePrompt, _): 1

        case (.claudeSubagent, .localFiles): 4
        case (.claudeSubagent, .onlyLLM), (.claudeSubagent, .web), (.claudeSubagent, .apis): 3
        case (.claudeSubagent, .sensors): 1
        }
    }

    public func score(for users: TargetUsers) -> Int {
        switch (self, users) {
        case (.androidKotlin, .endUsers): 5
        case (.androidKotlin, .developers): 3
        case (.androidKotlin, .claudeUsers): 1

        case (.tauriDesktop, .endUsers): 5
        case (.tauriDesktop, .developers): 4
        case (.tauriDesktop, .claudeUsers): 1

        case (.pythonCLI, .developers): 5
        case (.pythonCLI, .claudeUsers): 3
        case (.pythonCLI, .endUsers): 2

        case (.purePrompt, .developers), (.purePrompt, .endUsers): 3
        case (.purePrompt, .claudeUsers): 4

        case (.claudeSubagent, .claudeUsers): 5
        case (.claudeSubagent, .developers): 3
        case (.claudeSubagent, .endUsers): 1
        }
    }

    public func score(for offline: OfflineCapability) -> Int {
        switch (self, offline) {
        case (.androidKotlin, .mustOffline): 5
        case (.androidKotlin, .canBeOnline): 4

        case (.tauriDesktop, .mustOffline): 4
        case (.tauriDesktop, .canBeOnline): 5

        case (.pythonCLI, .mustOffline): 3
        case (.pythonCLI, .canBeOnline): 5

        case (.purePrompt, .mustOffline): 2
        case (.purePrompt, .canBeOnline): 3

        case (.claudeSubagent, .mustOffline): 1
        case (.claudeSubagent, .canBeOnline): 4
        }
    }

    // MARK: - Gesamt-Score

    public func totalScore(for decomp: TaskDecomposition) -> Int {
        score(for: decomp.targetEnvironment)
            + score(for: decomp.interactivity)
            + score(for: decomp.verifiability)
            + score(for: decomp.dataSources)
            + score(for: decomp.targetUsers)
            + score(for: decomp.offlineCapability)
    }
}
