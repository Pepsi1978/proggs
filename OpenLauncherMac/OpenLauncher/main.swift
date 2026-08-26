import AppKit

/// Haelt den Delegate am Leben (NSApplication.delegate ist eine schwache Referenz).
nonisolated(unsafe) var retainedDelegate: AppDelegate?

// Einstiegspunkt. Bewusst von Hand verdrahtet statt per `@main`/`@NSApplicationMain`:
// Diese App wird komplett im Code aufgebaut und hat keine NIB. NSApplicationMain wuerde seinen
// Delegate aber genau dort suchen - ohne NIB bliebe er nil und applicationDidFinishLaunching
// wuerde nie aufgerufen (die App startete, zeigte aber nichts an).
// main.swift laeuft ausserhalb des MainActors; der Aufbau selbst gehoert aber dorthin
// (Best-Practice §B6: MainActor-Isolation bewusst setzen statt zu umgehen).
let application = NSApplication.shared
MainActor.assumeIsolated {
    let delegate = AppDelegate()
    application.delegate = delegate
    // Referenz halten: NSApplication.delegate ist `weak`, ohne diesen Anker waere der Delegate
    // sofort wieder freigegeben und keine einzige Rueckmeldung kaeme an.
    retainedDelegate = delegate
}
// .regular: der Launcher ist ein normales Fensterprogramm mit Dock-Symbol und Menueleiste
// (kein Menuebar-/Accessory-Programm wie die Voice-Overlays).
application.setActivationPolicy(.regular)
application.run()
