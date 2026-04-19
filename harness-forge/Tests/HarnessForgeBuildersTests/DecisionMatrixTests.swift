// DecisionMatrixTests.swift

import Testing
@testable import HarnessForgeBuilders

private let mobileDecomp = TaskDecomposition(
    targetEnvironment: .mobile,
    interactivity: .ongoing,
    verifiability: .softCriteria,
    dataSources: .sensors,
    targetUsers: .endUsers,
    offlineCapability: .mustOffline,
    rationale: "Wander-App"
)

@Test("DecisionMatrix: build erzeugt eine Zeile pro HarnessType")
func buildGeneratesAllRows() {
    let m = DecisionMatrix.build(for: "Wander-Tagebuch", decomposition: mobileDecomp)
    #expect(m.rows.count == HarnessType.allCases.count)
}

@Test("DecisionMatrix: sortedByScore ist absteigend")
func sortedDescending() {
    let m = DecisionMatrix.build(for: "t", decomposition: mobileDecomp)
    let scores = m.sortedByScore.map(\.totalScore)
    #expect(scores == scores.sorted(by: >))
}

@Test("DecisionMatrix: recommended == best of sortedByScore")
func recommendedMatchesTop() {
    let m = DecisionMatrix.build(for: "t", decomposition: mobileDecomp)
    #expect(m.recommended == m.sortedByScore.first?.harnessType)
}

@Test("DecisionMatrix: Mobile-Task empfiehlt Android")
func mobileRecommendsAndroid() {
    let m = DecisionMatrix.build(for: "Wander-Tagebuch", decomposition: mobileDecomp)
    #expect(m.recommended == .androidKotlin)
}

@Test("DecisionMatrix: prettyPrint liefert Markdown mit Header und 5 Zeilen")
func prettyPrintContainsAllRows() {
    let m = DecisionMatrix.build(for: "t", decomposition: mobileDecomp)
    let out = m.prettyPrint()
    #expect(out.contains("| Harness "))
    #expect(out.contains("Android (Kotlin + Compose)"))
    #expect(out.contains("Python CLI"))
}

@Test("DecisionMatrix: generateRationale erwaehnt Empfehlung und Alternative")
func rationaleMentionsBoth() {
    let m = DecisionMatrix.build(for: "t", decomposition: mobileDecomp)
    let text = m.generateRationale(decomposition: mobileDecomp)
    #expect(text.contains("Android"))
    #expect(text.contains("Alternative"))
    #expect(text.contains("Wander-App"))
}
