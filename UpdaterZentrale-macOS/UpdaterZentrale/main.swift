import AppKit

/// Haelt den Delegate am Leben (NSApplication.delegate ist eine schwache Referenz).
nonisolated(unsafe) var gehaltenerDelegate: AppDelegate?

// Einstiegspunkt. Bewusst von Hand verdrahtet statt per `@main`/`@NSApplicationMain`:
// Diese App wird komplett im Code aufgebaut und hat keine NIB. NSApplicationMain wuerde seinen
// Delegate aber genau dort suchen - ohne NIB bliebe er nil und applicationDidFinishLaunching
// wuerde nie aufgerufen (die App startete, zeigte aber nichts an).
let anwendung = NSApplication.shared
MainActor.assumeIsolated {
    let delegate = AppDelegate()
    anwendung.delegate = delegate
    // Referenz halten: NSApplication.delegate ist `weak`, ohne diesen Anker waere der Delegate
    // sofort wieder freigegeben und keine einzige Rueckmeldung kaeme an.
    gehaltenerDelegate = delegate
}
// .regular: ein normales Fensterprogramm mit Dock-Symbol und Menueleiste.
anwendung.setActivationPolicy(.regular)
anwendung.run()
