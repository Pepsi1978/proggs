// HarnessForgeApp.swift
// @main-Einstiegspunkt der SwiftUI-App. Ersetzt den Konsolen-Stub aus Step 1.

import SwiftUI

@main
struct HarnessForgeApp: App {
    @State private var state = AppState()

    var body: some Scene {
        WindowGroup("Harness Forge") {
            RootView()
                .environment(state)
                .frame(minWidth: 900, minHeight: 620)
                .task { await state.refresh() }
        }
        .windowResizability(.contentSize)

        Settings {
            SettingsView()
                .environment(state)
                .frame(width: 520, height: 380)
                .padding()
        }
    }
}
