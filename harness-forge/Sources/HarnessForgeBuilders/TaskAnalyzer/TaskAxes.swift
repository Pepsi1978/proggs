// TaskAxes.swift
// Die 6 Achsen, entlang derer jede Aufgabe zerlegt wird. Jede Achse ist ein
// Enum mit klar definierten Werten — das erzwingt saubere Strukturen beim
// LLM-Output und macht die nachgelagerte Scoring-Matrix deterministisch.

import Foundation

// MARK: - Achse 1: Zielumgebung

public enum TargetEnvironment: String, Sendable, Codable, CaseIterable {
    case mobile
    case desktop
    case terminal
    case cloud
    case promptOnly
    case embedded
}

// MARK: - Achse 2: Interaktivitaet

public enum Interactivity: String, Sendable, Codable, CaseIterable {
    case oneShot
    case ongoing
    case background
}

// MARK: - Achse 3: Verifizierbarkeit

public enum Verifiability: String, Sendable, Codable, CaseIterable {
    case hardTests
    case softCriteria
    case subjective
}

// MARK: - Achse 4: Datenquellen

public enum DataSources: String, Sendable, Codable, CaseIterable {
    case onlyLLM
    case localFiles
    case web
    case apis
    case sensors
}

// MARK: - Achse 5: Zielnutzer

public enum TargetUsers: String, Sendable, Codable, CaseIterable {
    case developers
    case endUsers
    case claudeUsers
}

// MARK: - Achse 6: Offline-Faehigkeit

public enum OfflineCapability: String, Sendable, Codable, CaseIterable {
    case mustOffline
    case canBeOnline
}

// MARK: - Zerlegung

/// Das Ergebnis der Stage-1-Analyse: die Aufgabe entlang aller 6 Achsen eingeordnet,
/// plus eine menschenlesbare Begruendung.
public struct TaskDecomposition: Sendable, Codable, Equatable {
    public let targetEnvironment: TargetEnvironment
    public let interactivity: Interactivity
    public let verifiability: Verifiability
    public let dataSources: DataSources
    public let targetUsers: TargetUsers
    public let offlineCapability: OfflineCapability
    public let rationale: String

    public init(
        targetEnvironment: TargetEnvironment,
        interactivity: Interactivity,
        verifiability: Verifiability,
        dataSources: DataSources,
        targetUsers: TargetUsers,
        offlineCapability: OfflineCapability,
        rationale: String
    ) {
        self.targetEnvironment = targetEnvironment
        self.interactivity = interactivity
        self.verifiability = verifiability
        self.dataSources = dataSources
        self.targetUsers = targetUsers
        self.offlineCapability = offlineCapability
        self.rationale = rationale
    }
}
