// HarnessBuilder.swift
// Gemeinsames Protokoll aller 5 Harness-Builder (Steps 6-10).
// Input: Task + Zerlegung + Zielordner. Output: erstellter Projekt-Pfad +
// Liste aller angelegten Dateien + menschenlesbare Zusammenfassung.

import Foundation

public struct BuildOutcome: Sendable, Equatable {
    public let harnessType: HarnessType
    public let slug: String
    public let projectDirectory: URL
    public let createdFiles: [URL]
    public let summary: String

    public init(
        harnessType: HarnessType,
        slug: String,
        projectDirectory: URL,
        createdFiles: [URL],
        summary: String
    ) {
        self.harnessType = harnessType
        self.slug = slug
        self.projectDirectory = projectDirectory
        self.createdFiles = createdFiles
        self.summary = summary
    }
}

public enum BuilderError: Error, LocalizedError, Sendable {
    case directoryAlreadyExists(URL)
    case cannotCreateDirectory(URL, underlyingError: String)
    case cannotWriteFile(URL, underlyingError: String)

    public var errorDescription: String? {
        switch self {
        case .directoryAlreadyExists(let url):
            "Projekt-Ordner existiert bereits: \(url.path)"
        case .cannotCreateDirectory(let url, let err):
            "Kann Ordner nicht anlegen (\(url.path)): \(err)"
        case .cannotWriteFile(let url, let err):
            "Kann Datei nicht schreiben (\(url.path)): \(err)"
        }
    }
}

public protocol HarnessBuilder: Sendable {
    var type: HarnessType { get }

    func build(
        taskDescription: String,
        slug: String,
        decomposition: TaskDecomposition,
        outputParentDirectory: URL
    ) async throws -> BuildOutcome
}

// MARK: - Gemeinsame Helfer

public enum BuilderHelpers {
    /// Erstellt den Projekt-Ordner unter `parent/<slug>/`. Wirft, wenn er bereits existiert
    /// — Builder sollen nicht versehentlich vorhandene Arbeit ueberschreiben.
    public static func createProjectDirectory(parent: URL, slug: String) throws -> URL {
        let projectDir = parent.appendingPathComponent(slug)
        if FileManager.default.fileExists(atPath: projectDir.path) {
            throw BuilderError.directoryAlreadyExists(projectDir)
        }
        do {
            try FileManager.default.createDirectory(
                at: projectDir,
                withIntermediateDirectories: true
            )
        } catch {
            throw BuilderError.cannotCreateDirectory(projectDir, underlyingError: error.localizedDescription)
        }
        return projectDir
    }

    /// Schreibt Inhalt UTF-8 kodiert. Wirft mit BuilderError, falls FS-Fehler.
    public static func writeFile(at url: URL, content: String) throws {
        do {
            try content.write(to: url, atomically: true, encoding: .utf8)
        } catch {
            throw BuilderError.cannotWriteFile(url, underlyingError: error.localizedDescription)
        }
    }

    /// Menschlich lesbares Datum fuer Dokumentation.
    public static func isoDate(_ date: Date = Date()) -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withFullDate]
        return formatter.string(from: date)
    }
}
