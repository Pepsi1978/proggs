// LifecycleLayerTests.swift

import Testing
@testable import HarnessForgeLayers

@Test("Lifecycle: Kosten unter Cap sind OK")
func costBelowCap() async throws {
    let layer = LifecycleLayer(costCapUSD: 1.0)
    try await layer.trackCost(0.1)
    try await layer.trackCost(0.2)
    try await layer.trackCost(0.3)

    let stats = await layer.currentStats()
    #expect(abs(stats.totalCostUSD - 0.6) < 1e-6)
    #expect(stats.callCount == 3)
}

@Test("Lifecycle: Kosten ueber Cap werfen")
func costOverCap() async {
    let layer = LifecycleLayer(costCapUSD: 0.5)
    try? await layer.trackCost(0.3)
    await #expect(throws: LifecycleLayer.LifecycleError.self) {
        try await layer.trackCost(0.4)
    }
}

@Test("Lifecycle: reset() nullt Kosten und Historie")
func resetClearsState() async throws {
    let layer = LifecycleLayer()
    try await layer.trackCost(1.0)
    await layer.reset()
    let stats = await layer.currentStats()
    #expect(stats.totalCostUSD == 0)
    #expect(stats.callCount == 0)
}

@Test("Lifecycle: identische Outputs loesen Loop-Detection aus")
func loopDetected() async throws {
    let layer = LifecycleLayer(loopWindow: 5, loopSimilarityThreshold: 0.7)
    let sameOutput = "immer wieder die gleiche antwort"
    try await layer.checkLoop(with: sameOutput)
    try await layer.checkLoop(with: sameOutput)
    await #expect(throws: LifecycleLayer.LifecycleError.self) {
        try await layer.checkLoop(with: sameOutput)
    }
}

@Test("Lifecycle: unterschiedliche Outputs loesen keine Loop aus")
func noLoopOnDifferentOutputs() async throws {
    let layer = LifecycleLayer(loopSimilarityThreshold: 0.9)
    try await layer.checkLoop(with: "Alpha Beta Gamma")
    try await layer.checkLoop(with: "Delta Epsilon Zeta")
    try await layer.checkLoop(with: "Eta Theta Iota")
    // nichts geworfen
}

@Test("Lifecycle: tokenize und jaccard arbeiten symmetrisch")
func tokenizeAndJaccard() {
    let a = LifecycleLayer.tokenize("hallo welt")
    let b = LifecycleLayer.tokenize("hallo welt")
    #expect(LifecycleLayer.jaccard(a, b) == 1.0)

    let c = LifecycleLayer.tokenize("komplett anders")
    #expect(LifecycleLayer.jaccard(a, c) == 0.0)
}
