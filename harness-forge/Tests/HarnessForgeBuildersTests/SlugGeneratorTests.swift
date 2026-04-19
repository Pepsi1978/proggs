// SlugGeneratorTests.swift

import Testing
@testable import HarnessForgeBuilders

@Test("Slug: einfache Aufgabe wird zu kebab-case")
func simpleSlug() {
    let s = SlugGenerator.generate(from: "Schweden Packrafting Trip Planner")
    #expect(s == "schweden-packrafting-trip-planner")
}

@Test("Slug: Umlaute werden korrekt gefaltet")
func umlautsFolded() {
    let s = SlugGenerator.generate(
        from: "Fröhliches Ärger-Gespräch über Bücher",
        maxLength: 100
    )
    #expect(s == "froehliches-aerger-gespraech-ueber-buecher")
}

@Test("Slug: wird auf maxLength gekuerzt")
func truncation() {
    let long = String(repeating: "abcdefghij ", count: 20)  // ~200 chars
    let s = SlugGenerator.generate(from: long, maxLength: 15)
    #expect(s.count <= 15)
    #expect(!s.hasSuffix("-"))
}

@Test("Slug: leere Eingabe liefert unnamed-harness")
func emptyInput() {
    #expect(SlugGenerator.generate(from: "") == "unnamed-harness")
    #expect(SlugGenerator.generate(from: "   !!!   ") == "unnamed-harness")
}

@Test("Slug: ensureUnique passthrough bei fehlender Kollision")
func ensureUniqueNoCollision() {
    let s = SlugGenerator.ensureUnique(base: "meine-app", existing: ["andere", "nochwas"])
    #expect(s == "meine-app")
}

@Test("Slug: ensureUnique haengt -2 an bei Kollision")
func ensureUniqueCollision() {
    let s = SlugGenerator.ensureUnique(base: "meine-app", existing: ["meine-app"])
    #expect(s == "meine-app-2")
}

@Test("Slug: ensureUnique zaehlt hoch bis frei")
func ensureUniqueMultipleCollisions() {
    let existing: Set<String> = ["x", "x-2", "x-3"]
    let s = SlugGenerator.ensureUnique(base: "x", existing: existing)
    #expect(s == "x-4")
}

@Test("Slug: Sonderzeichen und Emojis werden gefiltert")
func specialCharsStripped() {
    let s = SlugGenerator.generate(from: "Hallo 😀 Welt !!! #rocks")
    #expect(s == "hallo-welt-rocks")
}
