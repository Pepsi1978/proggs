import AppKit

/// Duenne Huelle, damit Ansichtsmodelle eine Ja/Nein-Frage stellen koennen, ohne ein Fenster zu
/// kennen. 1:1-Port von Services/Dialoge.cs -- MessageBox wird zu NSAlert.
@MainActor
enum Dialoge {
    /// Titel des gerade offenen Fensters: die Fernsteuerung meldet es als "wartet auf Freigabe am Mac".
    /// Bei verschachtelten Fenstern zählt das innerste; schließt es, gilt wieder das äußere.
    private static var offeneTitel: [String] = []
    static var offenerTitel: String? { offeneTitel.last }

    private static func zeigen(_ fenster: NSAlert) -> NSApplication.ModalResponse {
        offeneTitel.append(fenster.messageText)
        Fernsteuerung.shared.sofortMelden()
        defer { offeneTitel.removeLast(); Fernsteuerung.shared.sofortMelden() }
        return fenster.runModal()
    }
    /// Fragt nach und gibt true bei "Ja" zurueck. "Nein" ist wie unter Windows die Vorgabe:
    /// eine versehentlich bestaetigte Frage darf nie ein Update ausloesen.
    static func fragen(_ text: String, _ titel: String) -> Bool {
        let hinweis = NSAlert()
        hinweis.messageText = titel
        hinweis.informativeText = text
        hinweis.alertStyle = .informational
        // Die erste Taste ist die vorgewaehlte; deshalb steht "Nein" vorn.
        hinweis.addButton(withTitle: "Nein")
        hinweis.addButton(withTitle: "Ja")
        return zeigen(hinweis) == .alertSecondButtonReturn
    }

    static func hinweis(_ text: String, _ titel: String = "Updater-Zentrale macOS") {
        let fenster = NSAlert()
        fenster.messageText = titel
        fenster.informativeText = text
        fenster.alertStyle = .informational
        fenster.addButton(withTitle: "OK")
        _ = zeigen(fenster)
    }
}
