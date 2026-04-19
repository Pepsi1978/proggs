// LessonsDBTests.swift
// Tests fuer LessonsDB + ReflectionLoop-Parser.

import Testing
import Foundation
@testable import HarnessForgeCore

@MainActor
@Suite("LessonsDB")
struct LessonsDBTests {
    @Test("Frisch: count = 0, leere Liste")
    func emptyInitial() throws {
        let db = try LessonsDB(inMemory: true)
        #expect(try db.count() == 0)
        #expect(try db.allLessons().isEmpty)
    }

    @Test("addLesson erhoeht count und liefert die Lesson zurueck")
    func addLesson() throws {
        let db = try LessonsDB(inMemory: true)
        let id = try db.addLesson(
            title: "Gemini 400 bei grossen Bildern",
            context: "Test",
            symptom: "HTTP 400",
            rootCause: "Image > 20MB",
            mitigation: "Resize to 15MB",
            tags: ["gemini", "image"]
        )
        #expect(try db.count() == 1)

        let all = try db.allLessons()
        #expect(all.count == 1)
        #expect(all[0].id == id)
        #expect(all[0].title == "Gemini 400 bei grossen Bildern")
        #expect(all[0].tags == ["gemini", "image"])
    }

    @Test("findLessons(matchingTag:) filtert korrekt")
    func findByTag() throws {
        let db = try LessonsDB(inMemory: true)
        try db.addLesson(title: "A", context: "c", symptom: "s", rootCause: "r", mitigation: "m", tags: ["gemini"])
        try db.addLesson(title: "B", context: "c", symptom: "s", rootCause: "r", mitigation: "m", tags: ["openai"])
        try db.addLesson(title: "C", context: "c", symptom: "s", rootCause: "r", mitigation: "m", tags: ["gemini", "image"])

        let geminiOnes = try db.findLessons(matchingTag: "gemini")
        #expect(geminiOnes.count == 2)
        #expect(Set(geminiOnes.map(\.title)) == Set(["A", "C"]))
    }

    @Test("allLessons ist absteigend nach discoveredAt sortiert")
    func sorting() async throws {
        let db = try LessonsDB(inMemory: true)
        try db.addLesson(title: "First", context: "c", symptom: "s", rootCause: "r", mitigation: "m")
        try await Task.sleep(for: .milliseconds(15))
        try db.addLesson(title: "Second", context: "c", symptom: "s", rootCause: "r", mitigation: "m")

        let all = try db.allLessons()
        #expect(all[0].title == "Second")
        #expect(all[1].title == "First")
    }

    @Test("deleteAll leert die DB")
    func deleteAll() throws {
        let db = try LessonsDB(inMemory: true)
        try db.addLesson(title: "x", context: "c", symptom: "s", rootCause: "r", mitigation: "m")
        try db.deleteAll()
        #expect(try db.count() == 0)
    }
}

// MARK: - ReflectionLoop-Parser-Tests (synchron, kein MainActor noetig)

@Test("ReflectionLoop-Parser: parst einzelnen Block korrekt")
func parseSingle() {
    let input = """
    TITEL: Token-Explosion bei langen Konversationen
    SYMPTOM: Kosten steigen nach 20+ Turns exponentiell
    URSACHE: Keine Kontext-Komprimierung aktiv
    ABHILFE: Nach 15 Turns Summary-Pass einbauen
    TAGS: kosten, kontext, optimierung
    ---
    """
    let result = ReflectionLoop.parseLessons(from: input)
    #expect(result.count == 1)
    #expect(result[0].title == "Token-Explosion bei langen Konversationen")
    #expect(result[0].tags == ["kosten", "kontext", "optimierung"])
}

@Test("ReflectionLoop-Parser: parst mehrere Bloecke")
func parseMultiple() {
    let input = """
    TITEL: A
    SYMPTOM: s1
    URSACHE: r1
    ABHILFE: m1
    TAGS: t1
    ---
    TITEL: B
    SYMPTOM: s2
    URSACHE: r2
    ABHILFE: m2
    TAGS:
    ---
    """
    let result = ReflectionLoop.parseLessons(from: input)
    #expect(result.count == 2)
    #expect(result[0].title == "A")
    #expect(result[1].title == "B")
    #expect(result[1].tags.isEmpty)
}

@Test("ReflectionLoop-Parser: unvollstaendiger Block wird verworfen")
func parseIncomplete() {
    let input = """
    TITEL: nur Titel, sonst nichts
    ---
    """
    let result = ReflectionLoop.parseLessons(from: input)
    #expect(result.isEmpty)
}

@Test("ReflectionLoop-Parser: leerer Input liefert leer zurueck")
func parseEmpty() {
    #expect(ReflectionLoop.parseLessons(from: "").isEmpty)
    #expect(ReflectionLoop.parseLessons(from: "   \n  ").isEmpty)
}
