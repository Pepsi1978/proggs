// VerificationLayerTests.swift

import Testing
@testable import HarnessForgeLayers

@Test("Verification: keine Hooks → keine Failures")
func noHooks() async {
    let layer = VerificationLayer()
    let failures = await layer.verify("irgendwas")
    #expect(failures.isEmpty)
}

@Test("Verification: MinimumLengthHook failed bei kurzem Output")
func minLengthFails() async {
    let layer = VerificationLayer(hooks: [MinimumLengthHook(minimumLength: 20)])
    let failures = await layer.verify("kurz")
    #expect(failures.count == 1)
    #expect(failures[0].hookName == "min-length")
}

@Test("Verification: MinimumLengthHook passed bei ausreichendem Output")
func minLengthPasses() async {
    let layer = VerificationLayer(hooks: [MinimumLengthHook(minimumLength: 5)])
    let failures = await layer.verify("dies ist definitiv lang genug")
    #expect(failures.isEmpty)
}

@Test("Verification: ValidJSONHook erkennt gueltiges und ungueltiges JSON")
func jsonHook() async {
    let layer = VerificationLayer(hooks: [ValidJSONHook()])
    let goodFailures = await layer.verify(#"{"a":1}"#)
    #expect(goodFailures.isEmpty)

    let badFailures = await layer.verify("nicht{json}")
    #expect(badFailures.count == 1)
    #expect(badFailures[0].hookName == "valid-json")
}

@Test("Verification: ForbiddenSubstringsHook fail bei Treffer")
func forbiddenHook() async {
    let layer = VerificationLayer(hooks: [
        ForbiddenSubstringsHook(forbidden: ["geheim", "password"])
    ])
    let failures = await layer.verify("hier ist das password")
    #expect(failures.count == 1)
}

@Test("Verification: mehrere Hooks laufen parallel, alle Failures kommen zurueck")
func multipleHooks() async {
    let layer = VerificationLayer(hooks: [
        MinimumLengthHook(minimumLength: 100),
        ValidJSONHook(),
    ])
    // kurz UND kein JSON → beide schlagen fehl
    let failures = await layer.verify("abc")
    #expect(failures.count == 2)
    let names = Set(failures.map(\.hookName))
    #expect(names == Set(["min-length", "valid-json"]))
}
