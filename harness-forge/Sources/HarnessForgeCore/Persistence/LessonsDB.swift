// LessonsDB.swift
// Facade fuer Lessons-Persistenz. "Never make that mistake again" —
// jede gelernte Regel wird hier abgelegt und bei kuenftigen Anfragen
// als Kontext beruecksichtigt.

import Foundation
import SwiftData

@MainActor
public final class LessonsDB {

    // MARK: - Public DTOs

    public struct Summary: Sendable, Equatable {
        public let id: UUID
        public let title: String
        public let discoveredAt: Date
        public let context: String
        public let symptom: String
        public let rootCause: String
        public let mitigation: String
        public let tags: [String]
    }

    public enum LessonsError: Error, LocalizedError, Sendable {
        case persistenceFailed(String)

        public var errorDescription: String? {
            switch self {
            case .persistenceFailed(let msg): "LessonsDB-Fehler: \(msg)"
            }
        }
    }

    // MARK: - Internals

    private let container: ModelContainer
    private var context: ModelContext { container.mainContext }

    public init(inMemory: Bool = false, url: URL? = nil) throws {
        let schema = Schema([Lesson.self])
        let config: ModelConfiguration
        if inMemory {
            config = ModelConfiguration(isStoredInMemoryOnly: true)
        } else if let url {
            config = ModelConfiguration(url: url)
        } else {
            config = ModelConfiguration()
        }
        do {
            self.container = try ModelContainer(for: schema, configurations: config)
        } catch {
            throw LessonsError.persistenceFailed(error.localizedDescription)
        }
    }

    // MARK: - CRUD

    @discardableResult
    public func addLesson(
        title: String,
        context: String,
        symptom: String,
        rootCause: String,
        mitigation: String,
        tags: [String] = []
    ) throws -> UUID {
        let lesson = Lesson(
            title: title,
            context: context,
            symptom: symptom,
            rootCause: rootCause,
            mitigation: mitigation,
            tags: tags
        )
        self.context.insert(lesson)
        do {
            try self.context.save()
        } catch {
            throw LessonsError.persistenceFailed(error.localizedDescription)
        }
        return lesson.id
    }

    public func allLessons() throws -> [Summary] {
        let descriptor = FetchDescriptor<Lesson>(
            sortBy: [SortDescriptor(\.discoveredAt, order: .reverse)]
        )
        do {
            return try context.fetch(descriptor).map(Self.summarize)
        } catch {
            throw LessonsError.persistenceFailed(error.localizedDescription)
        }
    }

    /// Findet alle Lessons, deren Tags das gegebene Tag enthalten.
    /// Arrays sind in #Predicate eingeschraenkt — wir filtern in Swift nach dem Fetch.
    public func findLessons(matchingTag tag: String) throws -> [Summary] {
        try allLessons().filter { $0.tags.contains(tag) }
    }

    public func count() throws -> Int {
        do {
            return try context.fetchCount(FetchDescriptor<Lesson>())
        } catch {
            throw LessonsError.persistenceFailed(error.localizedDescription)
        }
    }

    public func deleteAll() throws {
        do {
            try context.delete(model: Lesson.self)
            try context.save()
        } catch {
            throw LessonsError.persistenceFailed(error.localizedDescription)
        }
    }

    // MARK: - Privates

    private static func summarize(_ l: Lesson) -> Summary {
        Summary(
            id: l.id,
            title: l.title,
            discoveredAt: l.discoveredAt,
            context: l.context,
            symptom: l.symptom,
            rootCause: l.rootCause,
            mitigation: l.mitigation,
            tags: l.tags
        )
    }
}
