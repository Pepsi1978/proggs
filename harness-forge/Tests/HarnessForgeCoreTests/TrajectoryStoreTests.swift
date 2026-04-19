// TrajectoryStoreTests.swift
// Tests fuer die SwiftData-basierte Trajektorien-Persistenz.
// Jeder Test laeuft auf MainActor mit einer In-Memory-DB — keine Dateien, keine Leaks.

import Testing
import Foundation
@testable import HarnessForgeCore

@MainActor
@Suite("TrajectoryStore")
struct TrajectoryStoreTests {
    @Test("Frisch: leere Liste, count = 0")
    func emptyInitial() throws {
        let store = try TrajectoryStore(inMemory: true)
        #expect(try store.listTrajectories().isEmpty)
        #expect(try store.count() == 0)
    }

    @Test("Create fuegt Trajectory hinzu und gibt ID zurueck")
    func createTrajectory() throws {
        let store = try TrajectoryStore(inMemory: true)
        let id = try store.createTrajectory(
            taskDescription: "Test-Aufgabe",
            harnessType: "pure_prompt",
            harnessSlug: "test-slug"
        )
        let list = try store.listTrajectories()
        #expect(list.count == 1)
        #expect(list[0].id == id)
        #expect(list[0].taskDescription == "Test-Aufgabe")
        #expect(list[0].harnessType == "pure_prompt")
        #expect(list[0].interactionCount == 0)
        #expect(list[0].successScore == 0)
    }

    @Test("recordInteraction aktualisiert Totals")
    func recordInteraction() throws {
        let store = try TrajectoryStore(inMemory: true)
        let id = try store.createTrajectory(taskDescription: "t", harnessType: "cli", harnessSlug: "x")

        try store.recordInteraction(
            trajectoryID: id,
            backend: "anthropic",
            model: "claude",
            prompt: "hi",
            response: "ok",
            inputTokens: 100,
            outputTokens: 50,
            costUSD: 0.01,
            latencyMs: 123,
            success: true
        )

        try store.recordInteraction(
            trajectoryID: id,
            backend: "anthropic",
            model: "claude",
            prompt: "mehr",
            response: "klar",
            inputTokens: 80,
            outputTokens: 20,
            costUSD: 0.005,
            latencyMs: 99,
            success: true
        )

        let list = try store.listTrajectories()
        #expect(list.count == 1)
        #expect(list[0].interactionCount == 2)
        #expect(list[0].totalInputTokens == 180)
        #expect(list[0].totalOutputTokens == 70)
        #expect(abs(list[0].totalCostUSD - 0.015) < 1e-6)
    }

    @Test("recordInteraction fuer unbekannte ID wirft")
    func recordForUnknownThrows() throws {
        let store = try TrajectoryStore(inMemory: true)
        #expect(throws: TrajectoryStore.StoreError.self) {
            try store.recordInteraction(
                trajectoryID: UUID(),
                backend: "x", model: "y", prompt: "p", response: "r",
                inputTokens: 0, outputTokens: 0, costUSD: 0, latencyMs: 0, success: true
            )
        }
    }

    @Test("setScore aktualisiert den Score")
    func setScore() throws {
        let store = try TrajectoryStore(inMemory: true)
        let id = try store.createTrajectory(taskDescription: "t", harnessType: "x", harnessSlug: "s")
        try store.setScore(trajectoryID: id, score: 5)
        let list = try store.listTrajectories()
        #expect(list[0].successScore == 5)
    }

    @Test("Mehrere Trajektorien sind nach createdAt absteigend sortiert")
    func sortOrder() async throws {
        let store = try TrajectoryStore(inMemory: true)
        _ = try store.createTrajectory(taskDescription: "first", harnessType: "x", harnessSlug: "a")
        try await Task.sleep(for: .milliseconds(15))
        _ = try store.createTrajectory(taskDescription: "second", harnessType: "x", harnessSlug: "b")

        let list = try store.listTrajectories()
        #expect(list.count == 2)
        #expect(list[0].taskDescription == "second", "neueste zuerst")
        #expect(list[1].taskDescription == "first")
    }
}
