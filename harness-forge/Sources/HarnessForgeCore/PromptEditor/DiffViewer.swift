// DiffViewer.swift
// Duenner Wrapper um PromptVersioning.diff — fuer klar getrennte Rollen.

import Foundation

public struct DiffViewer: Sendable {
    private let versioning: PromptVersioning

    public init(versioning: PromptVersioning) {
        self.versioning = versioning
    }

    /// Unified Diff zwischen zwei Versionen einer Datei.
    public func diff(
        harnessSlug: String,
        fileName: String,
        from: String,
        to: String = "HEAD"
    ) async throws -> String {
        try await versioning.diff(
            harnessSlug: harnessSlug,
            fileName: fileName,
            fromVersion: from,
            toVersion: to
        )
    }

    /// Diff zwischen der letzten gespeicherten Version und der davor.
    public func diffWithPrevious(
        harnessSlug: String,
        fileName: String
    ) async throws -> String? {
        let versions = try await versioning.listVersions(
            harnessSlug: harnessSlug,
            fileName: fileName
        )
        guard versions.count >= 2 else { return nil }
        return try await diff(
            harnessSlug: harnessSlug,
            fileName: fileName,
            from: versions[1].hash,
            to: versions[0].hash
        )
    }
}
