// Trajectory.swift
// SwiftData-Model fuer eine vollstaendige Aufgabe (von Eingabe bis Abschluss).
// Eine Trajectory enthaelt N Interactions (Tool-Calls, LLM-Aufrufe).

import Foundation
import SwiftData

/// Eine abgeschlossene oder laufende Aufgabe, fuer die Harness Forge einen
/// oder mehrere KI-Aufrufe gemacht hat. Dient spaeter als Input fuer
/// Reflection-Loops und Fine-Tuning-Datasets.
@Model
public final class Trajectory {
    @Attribute(.unique) public var id: UUID
    public var createdAt: Date
    public var updatedAt: Date
    public var taskDescription: String
    /// Harness-Typ: "pure_prompt", "python_cli", "tauri_desktop",
    /// "android_kotlin", "claude_subagent".
    public var harnessType: String
    /// Generierter slug-style Name, z.B. "swedish-packrafting-planner".
    public var harnessSlug: String
    public var totalCostUSD: Double
    public var totalInputTokens: Int
    public var totalOutputTokens: Int
    /// Erfolgs-Score 1..5, 0 = unbewertet.
    public var successScore: Int

    @Relationship(deleteRule: .cascade, inverse: \Interaction.trajectory)
    public var interactions: [Interaction] = []

    public init(
        id: UUID = UUID(),
        taskDescription: String,
        harnessType: String,
        harnessSlug: String
    ) {
        self.id = id
        self.createdAt = Date()
        self.updatedAt = Date()
        self.taskDescription = taskDescription
        self.harnessType = harnessType
        self.harnessSlug = harnessSlug
        self.totalCostUSD = 0
        self.totalInputTokens = 0
        self.totalOutputTokens = 0
        self.successScore = 0
    }
}
