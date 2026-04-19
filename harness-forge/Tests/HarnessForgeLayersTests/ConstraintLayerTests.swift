// ConstraintLayerTests.swift

import Testing
@testable import HarnessForgeLayers

@Test("Constraint: leere Regel-Liste → keine Violations")
func emptyRules() {
    let layer = ConstraintLayer()
    #expect(layer.validate("irgendwas").isEmpty)
}

@Test("Constraint: nonEmpty-Regel erkennt leeren String")
func nonEmptyRule() {
    let layer = ConstraintLayer(rules: [ConstraintLayer.nonEmpty])
    #expect(layer.validate("  \n\n  ").count == 1)
    #expect(layer.validate("Hallo").isEmpty)
}

@Test("Constraint: maxLength warnt bei zu langem Input")
func maxLengthRule() {
    let layer = ConstraintLayer(rules: [ConstraintLayer.maxLength(5)])
    #expect(layer.validate("abc").isEmpty)
    let violations = layer.validate("abcdefgh")
    #expect(violations.count == 1)
    #expect(violations[0].severity == .warning)
}

@Test("Constraint: forbidsSubstrings erkennt verbotene Strings")
func forbiddenRule() {
    let layer = ConstraintLayer(rules: [
        ConstraintLayer.forbidsSubstrings(["secret", "password"])
    ])
    #expect(layer.validate("hallo welt").isEmpty)
    #expect(layer.validate("my password is 123").count == 1)
}

@Test("Constraint: validateJSONKeys erkennt fehlende Keys")
func jsonKeyValidation() {
    let layer = ConstraintLayer()
    let valid = #"{"name":"foo","version":"1.0"}"#
    #expect(layer.validateJSONKeys(valid, requiredKeys: ["name", "version"]).isEmpty)

    let invalid = #"{"name":"foo"}"#
    let violations = layer.validateJSONKeys(invalid, requiredKeys: ["name", "version"])
    #expect(violations.count == 1)
    #expect(violations[0].ruleID == "json.missing-key")
}

@Test("Constraint: validateJSONKeys erkennt ungueltiges JSON")
func invalidJSON() {
    let layer = ConstraintLayer()
    let violations = layer.validateJSONKeys("nicht{json}", requiredKeys: [])
    #expect(violations.count == 1)
    #expect(violations[0].ruleID == "json.parse")
}
