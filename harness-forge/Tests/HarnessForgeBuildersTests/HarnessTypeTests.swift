// HarnessTypeTests.swift
// Testet die Scoring-Matrix — pro Achse ein paar Stichproben.

import Testing
@testable import HarnessForgeBuilders

@Test("HarnessType: Android gewinnt fuer mobile + sensors + endUsers + mustOffline")
func androidDominatesMobile() {
    let decomp = TaskDecomposition(
        targetEnvironment: .mobile,
        interactivity: .ongoing,
        verifiability: .softCriteria,
        dataSources: .sensors,
        targetUsers: .endUsers,
        offlineCapability: .mustOffline,
        rationale: "Mobile Offline-App"
    )
    let android = HarnessType.androidKotlin.totalScore(for: decomp)
    let prompt = HarnessType.purePrompt.totalScore(for: decomp)
    let cli = HarnessType.pythonCLI.totalScore(for: decomp)
    #expect(android > prompt)
    #expect(android > cli)
}

@Test("HarnessType: PurePrompt gewinnt bei promptOnly + subjective + onlyLLM")
func promptDominatesPromptOnly() {
    let decomp = TaskDecomposition(
        targetEnvironment: .promptOnly,
        interactivity: .oneShot,
        verifiability: .subjective,
        dataSources: .onlyLLM,
        targetUsers: .claudeUsers,
        offlineCapability: .canBeOnline,
        rationale: "Brainstorming-Aufgabe"
    )
    let prompt = HarnessType.purePrompt.totalScore(for: decomp)
    let desktop = HarnessType.tauriDesktop.totalScore(for: decomp)
    #expect(prompt > desktop)
}

@Test("HarnessType: PythonCLI gewinnt bei terminal + developers + hardTests")
func cliDominatesTerminal() {
    let decomp = TaskDecomposition(
        targetEnvironment: .terminal,
        interactivity: .oneShot,
        verifiability: .hardTests,
        dataSources: .localFiles,
        targetUsers: .developers,
        offlineCapability: .canBeOnline,
        rationale: "Skript"
    )
    let cli = HarnessType.pythonCLI.totalScore(for: decomp)
    let android = HarnessType.androidKotlin.totalScore(for: decomp)
    #expect(cli > android)
}

@Test("HarnessType: ClaudeSubagent gewinnt bei claudeUsers")
func subagentDominatesClaudeUsers() {
    let decomp = TaskDecomposition(
        targetEnvironment: .terminal,
        interactivity: .oneShot,
        verifiability: .softCriteria,
        dataSources: .localFiles,
        targetUsers: .claudeUsers,
        offlineCapability: .canBeOnline,
        rationale: "Spezialist fuer Claude Code"
    )
    let sub = HarnessType.claudeSubagent.totalScore(for: decomp)
    let android = HarnessType.androidKotlin.totalScore(for: decomp)
    #expect(sub > android)
}

@Test("HarnessType: alle Scores sind im Bereich 1..5")
func allScoresInRange() {
    for harness in HarnessType.allCases {
        for env in TargetEnvironment.allCases {
            let s = harness.score(for: env)
            #expect((1...5).contains(s), "\(harness) vs \(env) = \(s)")
        }
        for inter in Interactivity.allCases {
            let s = harness.score(for: inter)
            #expect((1...5).contains(s), "\(harness) vs \(inter) = \(s)")
        }
        for ver in Verifiability.allCases {
            let s = harness.score(for: ver)
            #expect((1...5).contains(s), "\(harness) vs \(ver) = \(s)")
        }
        for d in DataSources.allCases {
            let s = harness.score(for: d)
            #expect((1...5).contains(s), "\(harness) vs \(d) = \(s)")
        }
        for u in TargetUsers.allCases {
            let s = harness.score(for: u)
            #expect((1...5).contains(s), "\(harness) vs \(u) = \(s)")
        }
        for o in OfflineCapability.allCases {
            let s = harness.score(for: o)
            #expect((1...5).contains(s), "\(harness) vs \(o) = \(s)")
        }
    }
}
