// HarnessForgeBuildersTests.swift
// Smoke-Test fuer das Builders-Modul.

import Testing
@testable import HarnessForgeBuilders

@Test("Builders-Modul laedt und Versionsnummer ist konsistent")
func buildersModuleSmokeTest() {
    #expect(HarnessForgeBuilders.version == "0.1.0")
    #expect(!HarnessForgeBuilders.version.isEmpty)
}
