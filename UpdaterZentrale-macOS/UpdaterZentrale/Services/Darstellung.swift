import AppKit

/// Farbsaetze und Design-Umschalter. 1:1-Port von Themes/Dunkel.xaml, Themes/Hell.xaml und
/// Services/Darstellung.cs. Jeder Hexwert ist unveraendert aus der Windows-Fassung uebernommen,
/// damit beide Plattformen exakt dieselben Farben zeigen.
///
/// WPF tauschte zur Laufzeit ein ganzes ResourceDictionary aus und alle DynamicResource-Bindungen
/// zogen nach. AppKit hat das nicht: hier meldet sich jede Ansicht auf `gewechselt` und faerbt
/// sich beim Wechsel selbst neu.

extension NSColor {
    /// WPF-Farbschreibweise: "#RRGGBB" oder "#AARRGGBB" (Alpha ZUERST -- anders als bei CSS).
    static func wpf(_ hex: String) -> NSColor {
        var wert = hex
        if wert.hasPrefix("#") { wert.removeFirst() }
        guard let roh = UInt32(wert, radix: 16) else { return .magenta }

        if wert.count == 8 {
            return NSColor(srgbRed: CGFloat((roh >> 16) & 0xFF) / 255.0,
                           green: CGFloat((roh >> 8) & 0xFF) / 255.0,
                           blue: CGFloat(roh & 0xFF) / 255.0,
                           alpha: CGFloat((roh >> 24) & 0xFF) / 255.0)
        }
        return NSColor(srgbRed: CGFloat((roh >> 16) & 0xFF) / 255.0,
                       green: CGFloat((roh >> 8) & 0xFF) / 255.0,
                       blue: CGFloat(roh & 0xFF) / 255.0,
                       alpha: 1.0)
    }

    /// Blendet die Farbe deckend ueber einen Untergrund. AppKit stapelt halbtransparente Ebenen
    /// sichtbar heller als WPF dieselbe Struktur zeichnet; an den Stellen, wo die Optik zaehlt,
    /// wird deshalb vorab verrechnet.
    func verrechnet(ueber untergrund: NSColor) -> NSColor {
        guard let oben = usingColorSpace(.sRGB), let basis = untergrund.usingColorSpace(.sRGB) else { return self }
        let a = oben.alphaComponent
        return NSColor(srgbRed: oben.redComponent * a + basis.redComponent * (1 - a),
                       green: oben.greenComponent * a + basis.greenComponent * (1 - a),
                       blue: oben.blueComponent * a + basis.blueComponent * (1 - a),
                       alpha: 1.0)
    }
}

/// Ein vollstaendiger Farbsatz. Die Namen entsprechen 1:1 den Schluesseln aus Dunkel.xaml/Hell.xaml.
struct Farbsatz {
    let hintergrund: NSColor
    let flaeche: NSColor
    let flaecheHoch: NSColor
    let flaecheTief: NSColor
    let rand: NSColor
    let randHell: NSColor
    let text: NSColor
    let textLeise: NSColor
    let textSehrLeise: NSColor

    let akzent: NSColor
    let akzentRand: NSColor
    let aufAkzent: NSColor
    let aufKennzeichen: NSColor

    /// WPF zeichnet hier einen Verlauf (#7C5CFF → #4DA3FF). AppKit nimmt die beiden Endpunkte.
    let akzentVerlaufOben: NSColor
    let akzentVerlaufUnten: NSColor

    let karteOben: NSColor
    let karteUnten: NSColor
    let karteGewaehlt: NSColor

    let konsole: NSColor
    let konsoleRand: NSColor
    let konsoleText: NSColor

    let plaketteFlaeche: NSColor
    let plaketteRand: NSColor

    let laeuftFlaeche: NSColor
    let laeuftRand: NSColor
    let laeuftText: NSColor

    let autostartFlaeche: NSColor
    let autostartRand: NSColor
    let autostartText: NSColor

    let warnFlaeche: NSColor
    let warnRand: NSColor
    let warnText: NSColor

    let fehlerFlaeche: NSColor
    let fehlerRand: NSColor
    let fehlerText: NSColor

    let schalterBahn: NSColor
    let schalterRand: NSColor
    let schalterKnopf: NSColor
    let rollbalken: NSColor
}

enum Darstellung {
    /// Wird nach jedem Wechsel gefeuert; jede Ansicht faerbt sich daraufhin neu.
    static let gewechselt = Notification.Name("UpdaterZentrale.DarstellungGewechselt")

    private(set) static var istHell = false

    static var satz: Farbsatz { istHell ? hell : dunkel }

    /// Untergrund, gegen den halbtransparente Flaechen verrechnet werden.
    static var untergrund: NSColor { satz.hintergrund }

    static func anwenden(_ hell: Bool) {
        istHell = hell
        NotificationCenter.default.post(name: gewechselt, object: nil)
    }

    // ===== Dunkel (Themes/Dunkel.xaml) =====
    private static let dunkel = Farbsatz(
        hintergrund: .wpf("#050609"),
        flaeche: .wpf("#1A1E2A"),
        flaecheHoch: .wpf("#242939"),
        flaecheTief: .wpf("#161A25"),
        rand: .wpf("#333A4E"),
        randHell: .wpf("#4A5470"),
        text: .wpf("#EEF0F6"),
        textLeise: .wpf("#9AA1B4"),
        textSehrLeise: .wpf("#7E8699"),
        akzent: .wpf("#7C5CFF"),
        akzentRand: .wpf("#6B4BF0"),
        aufAkzent: .wpf("#FFFFFF"),
        aufKennzeichen: .wpf("#0B0C10"),
        akzentVerlaufOben: .wpf("#7C5CFF"),
        akzentVerlaufUnten: .wpf("#4DA3FF"),
        karteOben: .wpf("#1E2230"),
        karteUnten: .wpf("#171B26"),
        karteGewaehlt: .wpf("#232841"),
        konsole: .wpf("#0B0D14"),
        konsoleRand: .wpf("#2E3449"),
        konsoleText: .wpf("#9FB4C9"),
        plaketteFlaeche: .wpf("#2A3044"),
        plaketteRand: .wpf("#3B4258"),
        laeuftFlaeche: .wpf("#16281E"),
        laeuftRand: .wpf("#245437"),
        laeuftText: .wpf("#5FE29B"),
        autostartFlaeche: .wpf("#1B2436"),
        autostartRand: .wpf("#2C3B57"),
        autostartText: .wpf("#89B4FF"),
        warnFlaeche: .wpf("#3A2A16"),
        warnRand: .wpf("#6B4A1E"),
        warnText: .wpf("#FFCE8A"),
        fehlerFlaeche: .wpf("#3A1B22"),
        fehlerRand: .wpf("#7A2B38"),
        fehlerText: .wpf("#FFB4C0"),
        schalterBahn: .wpf("#353C51"),
        schalterRand: .wpf("#454D66"),
        schalterKnopf: .wpf("#B9C0D0"),
        rollbalken: .wpf("#454D66")
    )

    // ===== Hell (Themes/Hell.xaml) =====
    private static let hell = Farbsatz(
        hintergrund: .wpf("#DFE3ED"),
        flaeche: .wpf("#FFFFFF"),
        flaecheHoch: .wpf("#FFFFFF"),
        flaecheTief: .wpf("#FFFFFF"),
        rand: .wpf("#BFC6D8"),
        randHell: .wpf("#98A2BC"),
        text: .wpf("#161822"),
        textLeise: .wpf("#5C6479"),
        textSehrLeise: .wpf("#79819A"),
        akzent: .wpf("#6344E8"),
        akzentRand: .wpf("#5738DC"),
        aufAkzent: .wpf("#FFFFFF"),
        aufKennzeichen: .wpf("#FFFFFF"),
        akzentVerlaufOben: .wpf("#6C4CF0"),
        akzentVerlaufUnten: .wpf("#3C8DF5"),
        karteOben: .wpf("#FFFFFF"),
        karteUnten: .wpf("#FBFCFF"),
        karteGewaehlt: .wpf("#EAE4FF"),
        konsole: .wpf("#1B1E28"),
        konsoleRand: .wpf("#2A2F3D"),
        konsoleText: .wpf("#C3D3E2"),
        plaketteFlaeche: .wpf("#E7EAF4"),
        plaketteRand: .wpf("#C3CADC"),
        laeuftFlaeche: .wpf("#E4F7EC"),
        laeuftRand: .wpf("#A9DEC1"),
        laeuftText: .wpf("#127A45"),
        autostartFlaeche: .wpf("#E7EFFE"),
        autostartRand: .wpf("#B4CCF4"),
        autostartText: .wpf("#1D4FA8"),
        warnFlaeche: .wpf("#FDF1DE"),
        warnRand: .wpf("#EBC88A"),
        warnText: .wpf("#8A5A11"),
        fehlerFlaeche: .wpf("#FDE9ED"),
        fehlerRand: .wpf("#F2B8C4"),
        fehlerText: .wpf("#98203A"),
        schalterBahn: .wpf("#CAD1E1"),
        schalterRand: .wpf("#AEB7CD"),
        schalterKnopf: .wpf("#FFFFFF"),
        rollbalken: .wpf("#A7B0C6")
    )
}

/// Farben fuer den Zustand einer Karte. Gegenstueck zu ZustandZuPinselUmwandler.
enum Zustandsfarben {
    private static func roh(_ zustand: UpdateZustand) -> (Int, Int, Int) {
        switch zustand {
        case .aktuell: return (34, 197, 94)
        case .updateVerfuegbar: return (245, 158, 11)
        case .fertig: return (56, 189, 248)
        case .fehler: return (244, 63, 94)
        case .abgebrochen: return (148, 163, 184)
        case .nichtInstalliert: return (148, 163, 184)
        case .pruefe: return (124, 92, 255)
        case .unbekannt: return (148, 163, 184)
        }
    }

    /// Getoente Flaeche hinter dem Zustands-Kennzeichen.
    static func hintergrund(_ zustand: UpdateZustand) -> NSColor {
        let (r, g, b) = roh(zustand)
        let deckung: CGFloat = Darstellung.istHell ? 30.0 / 255.0 : 38.0 / 255.0
        return NSColor(srgbRed: CGFloat(r) / 255, green: CGFloat(g) / 255, blue: CGFloat(b) / 255,
                       alpha: deckung).verrechnet(ueber: Darstellung.satz.karteOben)
    }

    /// Gesaettigte Schrift darauf. Auf heller Flaeche waere der satte Ton zu blass -- er wird
    /// deshalb abgedunkelt, genau wie in der Windows-Fassung.
    static func vordergrund(_ zustand: UpdateZustand) -> NSColor {
        var (r, g, b) = roh(zustand)
        if Darstellung.istHell {
            r = Int(Double(r) * 0.72); g = Int(Double(g) * 0.72); b = Int(Double(b) * 0.72)
        }
        return NSColor(srgbRed: CGFloat(r) / 255, green: CGFloat(g) / 255, blue: CGFloat(b) / 255, alpha: 1)
    }

    /// Gegenstueck zu ZustandZuTextUmwandler.
    static func text(_ zustand: UpdateZustand) -> String {
        switch zustand {
        case .aktuell: return "Aktuell"
        case .updateVerfuegbar: return "Update verfügbar"
        case .fertig: return "Fertig"
        case .fehler: return "Fehler"
        case .abgebrochen: return "Abgebrochen"
        case .nichtInstalliert: return "Nicht installiert"
        case .pruefe: return "Prüft …"
        case .unbekannt: return "Noch nicht geprüft"
        }
    }
}
