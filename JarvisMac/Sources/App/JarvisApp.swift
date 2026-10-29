import AppKit
import SwiftUI

@main
struct JarvisApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        Window("Jarvis", id: "haupt") {
            HauptFenster()
                .frame(minWidth: 980, minHeight: 640)
                .preferredColorScheme(.dark)
        }
        .windowStyle(.hiddenTitleBar)
        .defaultSize(width: 1280, height: 820)

        MenuBarExtra("Jarvis", systemImage: "waveform.circle.fill") {
            MenueLeiste()
        }
        .menuBarExtraStyle(.window)
    }
}

@MainActor
final class AppDelegate: NSObject, NSApplicationDelegate {
    func applicationDidFinishLaunching(_ notification: Notification) {
        Logbuch.shared.schreib("system", "Jarvis \(AppInfo.version) gestartet")
        Task { _ = await McpKlient.shared.werkzeuge(frisch: true) }
        AppSteuerung.shared.hochfahren()
    }
    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool { false }
}
