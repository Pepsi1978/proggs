// HarnessForgeApp.swift
// Einstiegspunkt der SwiftUI-Desktop-App.
//
// Aktuell ein Konsolen-Platzhalter — die SwiftUI-Oberflaeche (Sidebar,
// Task-Eingabe, Reasoning-Stream, Decision-Matrix, Settings) wird in
// Step 13 hinzugefuegt. Der Zweck dieses Stubs ist, dass das Executable-
// Target bereits im Step-1-Build mit uebersetzt wird und so strukturelle
// Fehler (z.B. Target-Verdrahtung in Package.swift) frueh auffallen.

import Foundation
import HarnessForgeCore

@main
enum HarnessForgeAppEntry {
    static func main() {
        let message = """

        \(HarnessForgeCore.displayName) v\(HarnessForgeCore.version)

        Die SwiftUI-Oberflaeche wird in Step 13 hinzugefuegt.
        Aktuell laeuft dieses Target als Konsolen-Programm und dient
        lediglich als Build-Verdrahtungs-Test.

        Fuer das CLI benutze bitte:
          swift run forge --help

        """
        print(message)
    }
}
