// ContextLayerTests.swift

import Testing
import Foundation
@testable import HarnessForgeLayers

@Test("Context: parseMarkdown zerlegt nach Headings")
func parseMarkdown() {
    let md = """
    # Hauptkapitel
    Einleitungstext.

    ## Unterkapitel A
    Inhalt A.
    Noch mehr Inhalt A.

    ## Unterkapitel B
    Inhalt B.
    """
    let ctx = ContextLayer.parseMarkdown(md)
    #expect(ctx.chunks.count == 3)
    #expect(ctx.chunks[0].heading == "Hauptkapitel")
    #expect(ctx.chunks[0].level == 1)
    #expect(ctx.chunks[1].heading == "Unterkapitel A")
    #expect(ctx.chunks[1].level == 2)
    #expect(ctx.chunks[1].content.contains("Inhalt A"))
}

@Test("Context: leerer Markdown liefert leere chunks")
func emptyMarkdown() {
    let ctx = ContextLayer.parseMarkdown("")
    #expect(ctx.chunks.isEmpty)
}

@Test("Context: relevantChunks findet keyword-ueberlappende Chunks")
func relevantChunks() {
    let md = """
    # Swift-Kapitel
    Es geht um Swift und Concurrency.

    # Android-Kapitel
    Hier geht es um Kotlin und Android.

    # Python-Kapitel
    Python und Django sind das Thema.
    """
    let ctx = ContextLayer.parseMarkdown(md)
    let hits = ctx.relevantChunks(for: "swift concurrency modell")
    #expect(!hits.isEmpty)
    #expect(hits[0].heading == "Swift-Kapitel")
}

@Test("Context: relevantChunks mit unpassender Query → leer")
func relevantChunksNoMatch() {
    let ctx = ContextLayer(chunks: [
        .init(heading: "Android", content: "kotlin compose", level: 1),
    ])
    #expect(ctx.relevantChunks(for: "Fahrrad Reparatur").isEmpty)
}

@Test("Context: assembleContext setzt Headings mit ## voran")
func assembleContext() {
    let ctx = ContextLayer(chunks: [
        .init(heading: "Test", content: "body", level: 1),
    ])
    let out = ctx.assembleContext(for: "test body")
    #expect(out.contains("## Test"))
    #expect(out.contains("body"))
}

@Test("Context: loadMarkdown wirft bei fehlender Datei")
func loadMissingFile() {
    let url = URL(fileURLWithPath: "/tmp/does-not-exist-\(UUID().uuidString).md")
    #expect(throws: ContextLayer.LoadError.self) {
        _ = try ContextLayer.loadMarkdown(from: url)
    }
}

@Test("Context: loadMarkdown laedt echte Datei und parst")
func loadRealFile() throws {
    let tmp = FileManager.default.temporaryDirectory.appendingPathComponent("ctx-\(UUID().uuidString).md")
    try "# Titel\nInhalt".write(to: tmp, atomically: true, encoding: .utf8)
    defer { try? FileManager.default.removeItem(at: tmp) }

    let ctx = try ContextLayer.loadMarkdown(from: tmp)
    #expect(ctx.chunks.count == 1)
    #expect(ctx.chunks[0].heading == "Titel")
}
