// MetaLayerTests.swift

import Testing
@testable import HarnessForgeLayers

@Test("Meta: detectDrift — gleiches Ziel = 0 Drift")
func noDrift() {
    let report = MetaLayer.detectDrift(
        originalGoal: "Baue einen Reiseplaner fuer Schweden",
        currentFocus: "Baue einen Reiseplaner fuer Schweden"
    )
    #expect(report.driftScore == 0.0)
    #expect(report.isDrifting == false)
}

@Test("Meta: detectDrift — komplett unterschiedlich = hoher Drift")
func fullDrift() {
    let report = MetaLayer.detectDrift(
        originalGoal: "Reiseplaner fuer Schweden",
        currentFocus: "Fotografie-Workshop in Tokyo"
    )
    #expect(report.driftScore > 0.6)
    #expect(report.isDrifting == true)
}

@Test("Meta: detectDrift — teilweise Ueberlappung = mittlerer Drift")
func partialDrift() {
    let report = MetaLayer.detectDrift(
        originalGoal: "Swift iOS App fuer Fotografie",
        currentFocus: "Swift iOS App fuer Video"
    )
    #expect(report.driftScore > 0)
    #expect(report.driftScore < 0.8)
}

@Test("Meta: extractSkillCandidates findet wiederkehrende Sequenzen")
func extractSkills() {
    let sequences: [[String]] = [
        ["read_file", "analyze", "write_file"],
        ["read_file", "analyze", "write_file"],
        ["read_file", "analyze", "format"],
    ]
    let skills = MetaLayer.extractSkillCandidates(
        from: sequences,
        length: 2,
        minOccurrences: 3
    )
    // "read_file, analyze" kommt 3x vor
    let topSequence = skills.first?.sequence
    #expect(topSequence == ["read_file", "analyze"])
    #expect(skills.first?.occurrences == 3)
}

@Test("Meta: extractSkillCandidates mit leerem Input → leer")
func extractEmpty() {
    let skills = MetaLayer.extractSkillCandidates(from: [])
    #expect(skills.isEmpty)
}

@Test("Meta: extractSkillCandidates ignoriert einzelne Vorkommen")
func extractSingletonsIgnored() {
    let sequences: [[String]] = [
        ["a", "b", "c"],
        ["d", "e", "f"],
    ]
    let skills = MetaLayer.extractSkillCandidates(
        from: sequences,
        length: 2,
        minOccurrences: 2
    )
    #expect(skills.isEmpty)
}
