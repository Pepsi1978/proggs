// TrajectoryStore.swift
// Facade ueber SwiftData fuer Trajectories + Interactions.
// @MainActor: SwiftData-@Models sind nicht Sendable zwischen Actors, deshalb
// serialisieren wir alle Zugriffe auf den MainActor.

import Foundation
import SwiftData

@MainActor
public final class TrajectoryStore {

    // MARK: - Public API (Sendable DTOs)

    public struct Summary: Sendable, Equatable {
        public let id: UUID
        public let createdAt: Date
        public let updatedAt: Date
        public let taskDescription: String
        public let harnessType: String
        public let harnessSlug: String
        public let totalCostUSD: Double
        public let totalInputTokens: Int
        public let totalOutputTokens: Int
        public let interactionCount: Int
        public let successScore: Int
    }

    public enum StoreError: Error, LocalizedError, Sendable {
        case trajectoryNotFound(UUID)
        case persistenceFailed(String)

        public var errorDescription: String? {
            switch self {
            case .trajectoryNotFound(let id): "Trajectory nicht gefunden: \(id.uuidString)"
            case .persistenceFailed(let msg): "SwiftData-Fehler: \(msg)"
            }
        }
    }

    // MARK: - Internals

    private let container: ModelContainer
    private var context: ModelContext { container.mainContext }

    public init(inMemory: Bool = false, url: URL? = nil) throws {
        let schema = Schema([Trajectory.self, Interaction.self])
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
            throw StoreError.persistenceFailed(error.localizedDescription)
        }
    }

    // MARK: - Trajectories

    /// Legt eine neue Trajectory an und gibt deren ID zurueck.
    @discardableResult
    public func createTrajectory(
        taskDescription: String,
        harnessType: String,
        harnessSlug: String
    ) throws -> UUID {
        let t = Trajectory(
            taskDescription: taskDescription,
            harnessType: harnessType,
            harnessSlug: harnessSlug
        )
        context.insert(t)
        do {
            try context.save()
        } catch {
            throw StoreError.persistenceFailed(error.localizedDescription)
        }
        return t.id
    }

    /// Fuegt eine Interaction zu einer existierenden Trajectory hinzu und
    /// aktualisiert die aggregierten Totals.
    public func recordInteraction(
        trajectoryID: UUID,
        backend: String,
        model: String,
        prompt: String,
        response: String,
        inputTokens: Int,
        outputTokens: Int,
        costUSD: Double,
        latencyMs: Int,
        success: Bool,
        errorMessage: String? = nil
    ) throws {
        guard let t = try fetchTrajectory(id: trajectoryID) else {
            throw StoreError.trajectoryNotFound(trajectoryID)
        }
        let interaction = Interaction(
            backendIdentifier: backend,
            modelName: model,
            promptContent: prompt,
            responseContent: response,
            inputTokens: inputTokens,
            outputTokens: outputTokens,
            costUSD: costUSD,
            latencyMs: latencyMs,
            success: success,
            errorMessage: errorMessage
        )
        interaction.trajectory = t
        context.insert(interaction)
        t.totalInputTokens += inputTokens
        t.totalOutputTokens += outputTokens
        t.totalCostUSD += costUSD
        t.updatedAt = Date()
        do {
            try context.save()
        } catch {
            throw StoreError.persistenceFailed(error.localizedDescription)
        }
    }

    public func setScore(trajectoryID: UUID, score: Int) throws {
        guard let t = try fetchTrajectory(id: trajectoryID) else {
            throw StoreError.trajectoryNotFound(trajectoryID)
        }
        t.successScore = score
        t.updatedAt = Date()
        do {
            try context.save()
        } catch {
            throw StoreError.persistenceFailed(error.localizedDescription)
        }
    }

    public func listTrajectories() throws -> [Summary] {
        let descriptor = FetchDescriptor<Trajectory>(
            sortBy: [SortDescriptor(\.createdAt, order: .reverse)]
        )
        do {
            return try context.fetch(descriptor).map(Self.summarize)
        } catch {
            throw StoreError.persistenceFailed(error.localizedDescription)
        }
    }

    public func count() throws -> Int {
        do {
            return try context.fetchCount(FetchDescriptor<Trajectory>())
        } catch {
            throw StoreError.persistenceFailed(error.localizedDescription)
        }
    }

    // MARK: - Privates

    private func fetchTrajectory(id: UUID) throws -> Trajectory? {
        let descriptor = FetchDescriptor<Trajectory>(
            predicate: #Predicate<Trajectory> { $0.id == id }
        )
        do {
            return try context.fetch(descriptor).first
        } catch {
            throw StoreError.persistenceFailed(error.localizedDescription)
        }
    }

    private static func summarize(_ t: Trajectory) -> Summary {
        Summary(
            id: t.id,
            createdAt: t.createdAt,
            updatedAt: t.updatedAt,
            taskDescription: t.taskDescription,
            harnessType: t.harnessType,
            harnessSlug: t.harnessSlug,
            totalCostUSD: t.totalCostUSD,
            totalInputTokens: t.totalInputTokens,
            totalOutputTokens: t.totalOutputTokens,
            interactionCount: t.interactions.count,
            successScore: t.successScore
        )
    }
}
