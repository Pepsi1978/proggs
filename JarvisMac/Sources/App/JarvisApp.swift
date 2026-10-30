import AppKit
import SwiftUI

@main
struct JarvisApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        Window("Jarvis", id: "haupt") {
            HauptFenster()
                .frame(minWidth: 980, minHeight: 640)
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
        // Alle Schlüssel gleich beim Start in den Tresor übernehmen und für das Schwärzen bekannt machen, nicht erst bei der ersten Nutzung.
        try? FileManager.default.setAttributes([.posixPermissions: 0o700], ofItemAtPath: Pfade.unterstuetzung.path)
        try? FileManager.default.setAttributes([.posixPermissions: 0o700], ofItemAtPath: Pfade.gespraeche.path)
        _ = Einstellungen.shared
        _ = VorleseEinstellungen.shared
        Task { _ = await CodexAnmeldung.shared.angemeldet }
        Task.detached(priority: .utility) { AblageLader.aufraeumen() }
        Task { _ = await McpKlient.shared.werkzeuge(frisch: true) }
        AppSteuerung.shared.hochfahren()
    }
    func applicationWillTerminate(_ notification: Notification) {
        TresorSperre.shared.sperren()
        Einstellungen.shared.zugangSichern()
        VorleseEinstellungen.shared.zugangSichern()
        Logbuch.shared.schreib("system", "Jarvis beendet")
        Logbuch.schreiber.sync {}
        GespraechsSpeicher.abschliessen()
    }
    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool { false }
}
