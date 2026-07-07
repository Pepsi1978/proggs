// RollbackEngine.swift
// Duenner Wrapper fuer Rollback — ermoeglicht UI-Buttons "Zurueck zu X".

import Foundation

public struct RollbackEngine: Sendable {
    private let versioning: PromptVersioning

    public init(versioning: PromptVersioning) {
        self.versioning = versioning
    }

    /// Rollt eine Datei auf eine frueher gespeicherte Version zurueck.
    /// Erzeugt einen neuen Commit ("Rollback auf abc1234"), damit die
    /// History erhalten bleibt.
    @discardableResult
    public func rollback(
        harnessSlug: String,
        fileName: String,
        to version: String
    ) async throws -> String {
        try await versioning.rollback(
            harnessSlug: harnessSlug,
            fileName: fileName,
            toVersion: version
        )
    }

    /// Zeigt alle verfuegbaren Rollback-Ziele (frueherer als HEAD).
    public func availableVersions(
        harnessSlug: String,
        fileName: String
    ) async throws -> [PromptVersioning.Version] {
        try await versioning.listVersions(harnessSlug: harnessSlug, fileName: fileName)
    }
}
