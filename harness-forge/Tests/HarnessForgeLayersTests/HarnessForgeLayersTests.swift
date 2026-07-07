// HarnessForgeLayersTests.swift
// Smoke-Test fuer das Layers-Modul.

import Testing
@testable import HarnessForgeLayers

@Test("Layers-Modul laedt und Versionsnummer ist konsistent")
func layersModuleSmokeTest() {
    #expect(HarnessForgeLayers.version == "0.1.0")
    #expect(!HarnessForgeLayers.version.isEmpty)
}
