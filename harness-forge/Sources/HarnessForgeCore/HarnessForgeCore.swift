// HarnessForgeCore.swift
// Kern-Bibliothek von Harness Forge.
//
// In dieser Datei landen nur die oeffentlichen Einstiegspunkte der Core-
// Bibliothek. Alles Fachliche (LLMClient, Router, Persistenz) lebt in
// eigenen Unter-Verzeichnissen und wird ab Step 2 hinzugefuegt.
//
// Teil des Mono-Repos Pepsi1978/proggs — Lizenz: MIT.

import Foundation

/// Hauptnamespace der Harness-Forge-Core-Bibliothek.
///
/// Aktuell enthaelt dieser Namespace lediglich die Versionskonstante. Ab
/// Schritt 2 kommen `LLMClient`, `Router` und weitere Kernkomponenten hinzu.
public enum HarnessForgeCore {
    /// Aktuelle Versionsnummer der Bibliothek.
    ///
    /// Wir folgen Semantic Versioning (MAJOR.MINOR.PATCH). Solange das Projekt
    /// im Entstehen ist, bleibt der MAJOR bei 0.
    public static let version = "0.1.0"

    /// Projekt-Anzeigename — wird von allen Frontends (CLI, SwiftUI-App)
    /// konsistent verwendet.
    public static let displayName = "Harness Forge"

    /// Kurzes Identitaets-Motto, wird z.B. im CLI-Banner und im App-About-
    /// Fenster angezeigt.
    public static let tagline = "Eine Fabrik fuer KI-Harnesse."
}
