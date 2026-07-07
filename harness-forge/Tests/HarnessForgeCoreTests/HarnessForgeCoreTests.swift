// HarnessForgeCoreTests.swift
// Initialer Smoke-Test fuer die Core-Bibliothek.
//
// Wir nutzen "Swift Testing" (neues Framework seit Swift 6 / Xcode 16),
// nicht XCTest. Erkennbar an `import Testing`, `@Test` und `#expect`.

import Testing
@testable import HarnessForgeCore

/// Pruefen, dass die oeffentlichen Identitaetskonstanten der Core-Bibliothek
/// tatsaechlich gesetzt und nicht leer sind. Das ist bewusst trivial — wir
/// wollen hier ausschliesslich validieren, dass der Swift-Package-Build
/// ueberhaupt durchlaeuft und das Target korrekt verdrahtet ist.
@Test("Versionsnummer ist nicht leer und matcht dem aktuellen Release")
func versionIsSet() {
    #expect(HarnessForgeCore.version == "0.1.0")
    #expect(!HarnessForgeCore.version.isEmpty)
}

@Test("Anzeigename ist der Projekt-Name")
func displayNameIsCorrect() {
    #expect(HarnessForgeCore.displayName == "Harness Forge")
}

@Test("Tagline ist gesetzt und beschreibt die Mission")
func taglineIsSet() {
    #expect(!HarnessForgeCore.tagline.isEmpty)
    #expect(HarnessForgeCore.tagline.contains("Fabrik"))
}
