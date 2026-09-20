import AppKit

/// Zusammenbau der App. Gegenstueck zu App.xaml.cs.
///
/// Der Einstiegspunkt selbst liegt in main.swift -- `@main` waere hier falsch: das daraus erzeugte
/// NSApplicationMain sucht seinen Delegate in einer NIB. Diese App hat keine (reiner Code-Aufbau),
/// also bliebe der Delegate ungesetzt und applicationDidFinishLaunching wuerde nie feuern.
@MainActor
final class AppDelegate: NSObject, NSApplicationDelegate {
    private var hauptFenster: HauptFensterController?

    func applicationDidFinishLaunching(_ notification: Notification) {
        // Nur ein Fenster: zwei Instanzen wuerden dieselbe settings.json und dieselben
        // Protokolldateien schreiben, und zwei Update-Laeufe koennten sich am selben Installer
        // begegnen. Gegenstueck zum Einzelstart-Mutex unter Windows -- auf macOS reicht dafuer
        // die Bundle-Kennung.
        if let laufende = andereInstanz() {
            if #available(macOS 14.0, *) {
                laufende.activate()
            } else {
                laufende.activate(options: [.activateIgnoringOtherApps])
            }
            NSApp.terminate(nil)
            return
        }

        menueBauen()

        let modell = HauptViewModel()
        let fenster = HauptFensterController(modell: modell)
        hauptFenster = fenster
        fenster.showWindow(nil)
        fenster.window?.makeKeyAndOrderFront(nil)

        if #available(macOS 14.0, *) {
            NSApp.activate()
        } else {
            NSApp.activate(ignoringOtherApps: true)
        }

        // Die Liste ist erst nuetzlich, wenn jede Karte ihren Zustand kennt -- die erste Pruefung
        // laeuft deshalb von selbst, sobald das Fenster steht.
        fenster.erstePruefungStarten()
    }

    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool { true }

    /// Klick aufs Dock-Symbol bei laufender App: Fenster wieder zeigen.
    func applicationShouldHandleReopen(_ sender: NSApplication, hasVisibleWindows flag: Bool) -> Bool {
        if !flag { hauptFenster?.window?.makeKeyAndOrderFront(nil) }
        return true
    }

    private func andereInstanz() -> NSRunningApplication? {
        guard let kennung = Bundle.main.bundleIdentifier else { return nil }
        return NSWorkspace.shared.runningApplications.first {
            $0.bundleIdentifier == kennung
                && $0.processIdentifier != ProcessInfo.processInfo.processIdentifier
        }
    }

    /// Ohne Menueleiste gibt es auf macOS weder ⌘Q noch ⌘W, und Kopieren/Einfuegen in der
    /// Befehlszeile bliebe ohne Tastenkuerzel.
    private func menueBauen() {
        let hauptMenue = NSMenu()

        let appEintrag = NSMenuItem()
        let appMenue = NSMenu()
        appMenue.addItem(withTitle: "Über Updater-Zentrale macOS",
                         action: #selector(NSApplication.orderFrontStandardAboutPanel(_:)), keyEquivalent: "")
        appMenue.addItem(.separator())
        appMenue.addItem(withTitle: "Updater-Zentrale ausblenden",
                         action: #selector(NSApplication.hide(_:)), keyEquivalent: "h")
        appMenue.addItem(withTitle: "Andere ausblenden",
                         action: #selector(NSApplication.hideOtherApplications(_:)), keyEquivalent: "H")
        appMenue.addItem(.separator())
        appMenue.addItem(withTitle: "Updater-Zentrale beenden",
                         action: #selector(NSApplication.terminate(_:)), keyEquivalent: "q")
        appEintrag.submenu = appMenue
        hauptMenue.addItem(appEintrag)

        let bearbeitenEintrag = NSMenuItem()
        let bearbeitenMenue = NSMenu(title: "Bearbeiten")
        bearbeitenMenue.addItem(withTitle: "Widerrufen", action: Selector(("undo:")), keyEquivalent: "z")
        bearbeitenMenue.addItem(withTitle: "Wiederholen", action: Selector(("redo:")), keyEquivalent: "Z")
        bearbeitenMenue.addItem(.separator())
        bearbeitenMenue.addItem(withTitle: "Ausschneiden", action: #selector(NSText.cut(_:)), keyEquivalent: "x")
        bearbeitenMenue.addItem(withTitle: "Kopieren", action: #selector(NSText.copy(_:)), keyEquivalent: "c")
        bearbeitenMenue.addItem(withTitle: "Einsetzen", action: #selector(NSText.paste(_:)), keyEquivalent: "v")
        bearbeitenMenue.addItem(withTitle: "Alles auswählen", action: #selector(NSText.selectAll(_:)), keyEquivalent: "a")
        bearbeitenEintrag.submenu = bearbeitenMenue
        hauptMenue.addItem(bearbeitenEintrag)

        let fensterEintrag = NSMenuItem()
        let fensterMenue = NSMenu(title: "Fenster")
        fensterMenue.addItem(withTitle: "Im Dock ablegen",
                             action: #selector(NSWindow.performMiniaturize(_:)), keyEquivalent: "m")
        fensterMenue.addItem(withTitle: "Zoomen", action: #selector(NSWindow.performZoom(_:)), keyEquivalent: "")
        fensterMenue.addItem(.separator())
        fensterMenue.addItem(withTitle: "Schließen", action: #selector(NSWindow.performClose(_:)), keyEquivalent: "w")
        fensterEintrag.submenu = fensterMenue
        hauptMenue.addItem(fensterEintrag)
        NSApp.windowsMenu = fensterMenue

        NSApp.mainMenu = hauptMenue
    }
}
