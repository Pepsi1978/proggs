import AppKit

/// Composition-Root (Best-Practice §E13). Gegenstueck zu App.xaml.cs.
/// Der Einstiegspunkt selbst liegt in main.swift - `@main` waere hier falsch: das daraus erzeugte
/// NSApplicationMain sucht seinen Delegate in einer NIB. Diese App hat keine (reiner Code-Aufbau),
/// also bliebe der Delegate ungesetzt und applicationDidFinishLaunching wuerde nie feuern.
@MainActor
final class AppDelegate: NSObject, NSApplicationDelegate {
    private var mainWindowController: MainWindowController?

    func applicationDidFinishLaunching(_ notification: Notification) {
        // Einzelinstanz: laeuft schon ein OpenLauncher, wird dieser in den Vordergrund geholt und
        // die zweite Instanz beendet sich. Gegenstueck zum Single-Instance-Mutex unter Windows.
        // macOS hat kein AllowSetForegroundWindow - `activate()` ohne Parameter ist der korrekte Weg
        // (Bug-Almanach swift-appkit §A4: `activate(ignoringOtherApps:)` ist seit macOS 14 wirkungslos).
        if let running = otherRunningInstance() {
            Logger.shared.info("AppDelegate", "applicationDidFinishLaunching",
                               "laufende OpenLauncher-Instanz aktiviert")
            // Deployment-Target ist macOS 13; das parameterlose activate() gibt es erst ab 14.
            // Auf 13 bleibt nur die alte, dort noch wirksame Fassung (Almanach §A4).
            if #available(macOS 14.0, *) {
                running.activate()
            } else {
                running.activate(options: [.activateIgnoringOtherApps])
            }
            NSApp.terminate(nil)
            return
        }

        Logger.shared.info("AppDelegate", "applicationDidFinishLaunching", "OpenLauncher gestartet")

        // Gespeichertes Design anwenden, BEVOR das Hauptfenster gezeichnet wird (kein Umschalt-Flackern).
        let layout = LayoutSettings.load()
        let theme: AppTheme = layout.theme.caseInsensitiveCompare("Light") == .orderedSame ? .light : .dark
        if theme != ThemeManager.current { ThemeManager.apply(theme) }

        buildMenu()

        let viewModel = MainViewModel()
        let controller = MainWindowController(viewModel: viewModel, layoutSettings: layout)
        mainWindowController = controller
        controller.showWindow(nil)
        controller.window?.makeKeyAndOrderFront(nil)
        if #available(macOS 14.0, *) {
            NSApp.activate()
        } else {
            NSApp.activate(ignoringOtherApps: true)
        }
    }

    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool { true }

    /// Klick aufs Dock-Symbol bei bereits laufender App: Fenster wieder zeigen.
    func applicationShouldHandleReopen(_ sender: NSApplication, hasVisibleWindows flag: Bool) -> Bool {
        if !flag { mainWindowController?.window?.makeKeyAndOrderFront(nil) }
        return true
    }

    private func otherRunningInstance() -> NSRunningApplication? {
        guard let bundleId = Bundle.main.bundleIdentifier else { return nil }
        return NSWorkspace.shared.runningApplications.first {
            $0.bundleIdentifier == bundleId && $0.processIdentifier != ProcessInfo.processInfo.processIdentifier
        }
    }

    /// Minimales Menue. Ohne Menueleiste funktionieren auf macOS weder ⌘Q noch ⌘W, und
    /// Kopieren/Einfuegen in den Textfeldern des Launchers bliebe ohne Tastenkuerzel.
    private func buildMenu() {
        let mainMenu = NSMenu()

        let appMenuItem = NSMenuItem()
        let appMenu = NSMenu()
        appMenu.addItem(withTitle: "Über OpenLauncher", action: #selector(NSApplication.orderFrontStandardAboutPanel(_:)), keyEquivalent: "")
        appMenu.addItem(.separator())
        appMenu.addItem(withTitle: "OpenLauncher ausblenden", action: #selector(NSApplication.hide(_:)), keyEquivalent: "h")
        appMenu.addItem(withTitle: "Andere ausblenden", action: #selector(NSApplication.hideOtherApplications(_:)), keyEquivalent: "H")
        appMenu.addItem(.separator())
        appMenu.addItem(withTitle: "OpenLauncher beenden", action: #selector(NSApplication.terminate(_:)), keyEquivalent: "q")
        appMenuItem.submenu = appMenu
        mainMenu.addItem(appMenuItem)

        let editMenuItem = NSMenuItem()
        let editMenu = NSMenu(title: "Bearbeiten")
        editMenu.addItem(withTitle: "Widerrufen", action: Selector(("undo:")), keyEquivalent: "z")
        editMenu.addItem(withTitle: "Wiederholen", action: Selector(("redo:")), keyEquivalent: "Z")
        editMenu.addItem(.separator())
        editMenu.addItem(withTitle: "Ausschneiden", action: #selector(NSText.cut(_:)), keyEquivalent: "x")
        editMenu.addItem(withTitle: "Kopieren", action: #selector(NSText.copy(_:)), keyEquivalent: "c")
        editMenu.addItem(withTitle: "Einsetzen", action: #selector(NSText.paste(_:)), keyEquivalent: "v")
        editMenu.addItem(withTitle: "Alles auswählen", action: #selector(NSText.selectAll(_:)), keyEquivalent: "a")
        editMenuItem.submenu = editMenu
        mainMenu.addItem(editMenuItem)

        let windowMenuItem = NSMenuItem()
        let windowMenu = NSMenu(title: "Fenster")
        windowMenu.addItem(withTitle: "Im Dock ablegen", action: #selector(NSWindow.performMiniaturize(_:)), keyEquivalent: "m")
        windowMenu.addItem(withTitle: "Zoomen", action: #selector(NSWindow.performZoom(_:)), keyEquivalent: "")
        windowMenu.addItem(.separator())
        windowMenu.addItem(withTitle: "Schließen", action: #selector(NSWindow.performClose(_:)), keyEquivalent: "w")
        windowMenuItem.submenu = windowMenu
        mainMenu.addItem(windowMenuItem)
        NSApp.windowsMenu = windowMenu

        NSApp.mainMenu = mainMenu
    }
}
