import AppKit

/// Nachbau der WPF-Styles aus Theme.xaml als AppKit-Bausteine.
/// WPF beschreibt Aussehen deklarativ (ControlTemplate + DynamicResource); AppKit braucht dafuer
/// eigene Ansichten mit CALayer-Eigenschaften. Jede Klasse hier entspricht genau einem XAML-Style.

// MARK: - Grundlage: faerbt sich beim Design-Wechsel selbst neu

class ThemedView: NSView {
    init() {
        super.init(frame: .zero)
        wantsLayer = true
        translatesAutoresizingMaskIntoConstraints = false
        NotificationCenter.default.addObserver(self, selector: #selector(farbenAnwenden),
                                               name: Darstellung.gewechselt, object: nil)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }
    deinit { NotificationCenter.default.removeObserver(self) }

    @objc func farbenAnwenden() {}
}

/// Beschriftung, die ihre Farbe beim Design-Wechsel nachzieht.
final class ThemedLabel: NSTextField {
    enum Rolle { case normal, leise, sehrLeise, akzent, fest }

    var rolle: Rolle = .normal { didSet { farbenAnwenden() } }
    /// Fuer Rolle `.fest`: eine Farbe, die nicht aus dem Farbsatz kommt (Zustands-Kennzeichen).
    var festeFarbe: NSColor? { didSet { farbenAnwenden() } }

    override init(frame frameRect: NSRect) {
        super.init(frame: frameRect)
        NotificationCenter.default.addObserver(self, selector: #selector(farbenAnwenden),
                                               name: Darstellung.gewechselt, object: nil)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }
    deinit { NotificationCenter.default.removeObserver(self) }

    /// Beschriftungen nehmen keine Klicks an.
    ///
    /// Eine Karte ist fast vollstaendig von Beschriftungen bedeckt. NSTextField verschluckt
    /// `mouseDown` auch als reines Label -- ein Klick auf den Beschreibungstext waehlte die Karte
    /// dann NICHT aus, obwohl er unter Windows genau das tut. `hitTest` auf nil reicht den Klick
    /// an die darunterliegende Ansicht weiter; Schaltflaechen und Schiebeschalter sind eigene
    /// Steuerelemente und davon nicht betroffen.
    override func hitTest(_ point: NSPoint) -> NSView? { nil }

    /// Mehrzeilige Beschriftungen muessen UMBRECHEN statt das Fenster breiter zu machen.
    ///
    /// AppKit fragt ein Label ohne `preferredMaxLayoutWidth` nach seiner Breite fuer EINE Zeile.
    /// In einem NSStackView im NSScrollView reicht Autolayout diesen Wunsch bis zum Fenster durch:
    /// eine lange Beschreibung machte das Fenster dann breiter als der Bildschirm, statt sich auf
    /// zwei Zeilen zu verteilen. Die zugewiesene Breite hier zurueckzumelden loest genau das.
    override func layout() {
        super.layout()
        if maximumNumberOfLines != 1, abs(preferredMaxLayoutWidth - bounds.width) > 0.5 {
            preferredMaxLayoutWidth = bounds.width
            invalidateIntrinsicContentSize()
        }
    }

    @objc func farbenAnwenden() {
        let satz = Darstellung.satz
        switch rolle {
        case .normal: textColor = satz.text
        case .leise: textColor = satz.textLeise
        case .sehrLeise: textColor = satz.textSehrLeise
        case .akzent: textColor = satz.akzent
        case .fest: textColor = festeFarbe ?? satz.text
        }
    }
}

enum UI {
    /// Beschriftung wie die TextBlock-Styles aus Theme.xaml.
    /// - Parameter einzeilig: `true` fuer kurze Angaben, die NIE umbrechen duerfen -- Versionen,
    ///   Zustands-Kennzeichen, Plaketten. Sie behalten ihre volle Breite.
    ///   `false` (Vorgabe) fuer Fliesstext: er bricht um, statt seinen Container aufzuspreizen.
    ///
    /// Die Unterscheidung ist wichtiger, als sie aussieht. Gibt man ALLEN Beschriftungen nach,
    /// quetscht Autolayout auch eine Versionsnummer auf ein Zeichen Breite -- "26.915.31945" stand
    /// dann als Ziffernsaeule untereinander. Gibt man KEINER nach, macht eine lange Beschreibung
    /// das Fenster breiter als den Bildschirm. Beides ist hier schon passiert.
    static func beschriftung(_ text: String, groesse: CGFloat = 12,
                             gewicht: NSFont.Weight = .regular,
                             rolle: ThemedLabel.Rolle = .normal,
                             mono: Bool = false,
                             kursiv: Bool = false,
                             einzeilig: Bool = false) -> ThemedLabel {
        let feld = ThemedLabel(labelWithString: text)
        var schrift = mono
            ? NSFont.monospacedSystemFont(ofSize: groesse, weight: gewicht)
            : NSFont.systemFont(ofSize: groesse, weight: gewicht)
        if kursiv {
            schrift = NSFontManager.shared.convert(schrift, toHaveTrait: .italicFontMask)
        }
        feld.font = schrift
        feld.rolle = rolle
        feld.translatesAutoresizingMaskIntoConstraints = false

        if einzeilig {
            feld.lineBreakMode = .byTruncatingTail
            feld.maximumNumberOfLines = 1
            feld.setContentCompressionResistancePriority(.required, for: .horizontal)
            feld.setContentHuggingPriority(.required, for: .horizontal)
        } else {
            feld.lineBreakMode = .byWordWrapping
            feld.maximumNumberOfLines = 0
            feld.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
            feld.setContentHuggingPriority(.defaultLow, for: .horizontal)
        }
        feld.farbenAnwenden()
        return feld
    }

    static func platzhalter() -> NSView {
        let ansicht = NSView()
        ansicht.translatesAutoresizingMaskIntoConstraints = false
        return ansicht
    }
}

// MARK: - Karte (ListBoxItem-Template aus HauptFenster.xaml)

/// Eine Programmkarte: abgerundete Flaeche mit Rand, die auf Auswahl und Mauszeiger reagiert.
class KartenView: ThemedView {
    var eckenRadius: CGFloat = 14 { didSet { farbenAnwenden() } }
    var istAusgewaehlt = false { didSet { farbenAnwenden() } }
    var beiKlick: (() -> Void)?

    private var beobachtung: NSTrackingArea?
    private var unterMaus = false { didSet { farbenAnwenden() } }

    override init() {
        super.init()
        farbenAnwenden()
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }

    override func updateTrackingAreas() {
        super.updateTrackingAreas()
        if let beobachtung { removeTrackingArea(beobachtung) }
        let bereich = NSTrackingArea(rect: bounds, options: [.mouseEnteredAndExited, .activeInActiveApp],
                                     owner: self)
        addTrackingArea(bereich)
        beobachtung = bereich
    }

    override func mouseEntered(with event: NSEvent) { unterMaus = true }
    override func mouseExited(with event: NSEvent) { unterMaus = false }
    override func mouseDown(with event: NSEvent) { beiKlick?() }

    override func farbenAnwenden() {
        let satz = Darstellung.satz
        layer?.cornerRadius = eckenRadius
        layer?.borderWidth = 1
        // WPF zeichnet hier einen sanften senkrechten Verlauf; die obere Farbe traegt die Wirkung.
        layer?.backgroundColor = (istAusgewaehlt ? satz.karteGewaehlt : satz.karteOben).cgColor
        if istAusgewaehlt {
            layer?.borderColor = satz.akzentRand.cgColor
        } else {
            layer?.borderColor = (unterMaus ? satz.randHell : satz.rand).cgColor
        }
    }
}

/// Eine schlichte, abgerundete Flaeche -- in XAML die Border mit FlaecheHoch/FlaecheTief.
class FlaechenView: ThemedView {
    enum Ton { case hoch, tief, konsole, plakette, laeuft, autostart, warnung, fehler }

    var ton: Ton = .hoch { didSet { farbenAnwenden() } }
    var eckenRadius: CGFloat = 9 { didSet { farbenAnwenden() } }
    var zeigtRand = true { didSet { farbenAnwenden() } }

    init(ton: Ton = .hoch, eckenRadius: CGFloat = 9) {
        self.ton = ton
        self.eckenRadius = eckenRadius
        super.init()
        layer?.masksToBounds = true
        farbenAnwenden()
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }

    override func farbenAnwenden() {
        let satz = Darstellung.satz
        layer?.cornerRadius = eckenRadius
        layer?.borderWidth = zeigtRand ? 1 : 0

        let (flaeche, rand): (NSColor, NSColor)
        switch ton {
        case .hoch: (flaeche, rand) = (satz.flaecheHoch, satz.rand)
        case .tief: (flaeche, rand) = (satz.flaecheTief, satz.rand)
        case .konsole: (flaeche, rand) = (satz.konsole, satz.konsoleRand)
        case .plakette: (flaeche, rand) = (satz.plaketteFlaeche, satz.plaketteRand)
        case .laeuft: (flaeche, rand) = (satz.laeuftFlaeche, satz.laeuftRand)
        case .autostart: (flaeche, rand) = (satz.autostartFlaeche, satz.autostartRand)
        case .warnung: (flaeche, rand) = (satz.warnFlaeche, satz.warnRand)
        case .fehler: (flaeche, rand) = (satz.fehlerFlaeche, satz.fehlerRand)
        }
        layer?.backgroundColor = flaeche.cgColor
        layer?.borderColor = rand.cgColor
    }
}

/// Kleines Merkmal-Kennzeichen ("winget", "läuft gerade", "Autostart") -- Flaeche plus Text in einem.
final class PlakettenView: FlaechenView {
    private let beschriftung: ThemedLabel

    init(text: String, ton: Ton, textRolle: ThemedLabel.Rolle = .leise,
         textFarbe: NSColor? = nil, groesse: CGFloat = 10.5) {
        beschriftung = UI.beschriftung(text, groesse: groesse, rolle: textFarbe == nil ? textRolle : .fest, einzeilig: true)
        beschriftung.festeFarbe = textFarbe
        super.init(ton: ton, eckenRadius: 6)
        addSubview(beschriftung)
        NSLayoutConstraint.activate([
            beschriftung.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 7),
            beschriftung.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -7),
            beschriftung.topAnchor.constraint(equalTo: topAnchor, constant: 2),
            beschriftung.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -2)
        ])
        setContentHuggingPriority(.required, for: .horizontal)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }

    var text: String {
        get { beschriftung.stringValue }
        set { beschriftung.stringValue = newValue }
    }

    func setzeTextFarbe(_ farbe: NSColor?) {
        beschriftung.festeFarbe = farbe
        beschriftung.rolle = farbe == nil ? .leise : .fest
    }

    override func farbenAnwenden() {
        super.farbenAnwenden()
        let satz = Darstellung.satz
        switch ton {
        case .laeuft: setzeTextFarbe(satz.laeuftText)
        case .autostart: setzeTextFarbe(satz.autostartText)
        case .warnung: setzeTextFarbe(satz.warnText)
        case .fehler: setzeTextFarbe(satz.fehlerText)
        default: break
        }
    }
}

/// Das farbige Kennzeichen mit den Initialen -- die Farbe kommt aus dem Katalog.
final class KennzeichenView: ThemedView {
    private let beschriftung: ThemedLabel

    init(kuerzel: String, akzent: String, groesse: CGFloat = 46) {
        beschriftung = UI.beschriftung(kuerzel, groesse: groesse * 0.35, gewicht: .bold, rolle: .fest)
        super.init()
        layer?.cornerRadius = groesse * 0.28
        layer?.backgroundColor = NSColor.wpf(akzent).cgColor

        beschriftung.alignment = .center
        addSubview(beschriftung)
        NSLayoutConstraint.activate([
            beschriftung.centerXAnchor.constraint(equalTo: centerXAnchor),
            beschriftung.centerYAnchor.constraint(equalTo: centerYAnchor),
            widthAnchor.constraint(equalToConstant: groesse),
            heightAnchor.constraint(equalToConstant: groesse)
        ])
        farbenAnwenden()
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }

    override func farbenAnwenden() {
        beschriftung.festeFarbe = Darstellung.satz.aufKennzeichen
    }
}

// MARK: - Schaltflaechen (SchalterHaupt / SchalterZweit / SchalterLeise / SchalterRuhig)

final class Schalter: NSControl {
    enum Auspraegung { case haupt, zweit, leise, ruhig }

    private let beschriftung = ThemedLabel(labelWithString: "")
    private var beobachtung: NSTrackingArea?
    private var unterMaus = false { didSet { farbenAnwenden() } }
    private var gedrueckt = false { didSet { farbenAnwenden() } }

    /// Einzelne Schaltflaechen bekommen bei einem echten Update die Akzentfarbe (DataTrigger
    /// `AktionBetont` in HauptFenster.xaml).
    var betont = false { didSet { farbenAnwenden() } }

    private(set) var auspraegung: Auspraegung
    var waagerechterRand: CGFloat = 16 { didSet { invalidateIntrinsicContentSize() } }
    var senkrechterRand: CGFloat = 9 { didSet { invalidateIntrinsicContentSize() } }

    var titel: String {
        get { beschriftung.stringValue }
        set {
            beschriftung.stringValue = newValue
            setAccessibilityLabel(newValue)
            invalidateIntrinsicContentSize()
        }
    }

    var schriftgroesse: CGFloat = 13 {
        didSet {
            beschriftung.font = .systemFont(ofSize: schriftgroesse,
                                            weight: auspraegung == .leise ? .regular : .semibold)
            invalidateIntrinsicContentSize()
        }
    }

    override var isEnabled: Bool { didSet { farbenAnwenden() } }

    init(_ auspraegung: Auspraegung, _ titel: String) {
        self.auspraegung = auspraegung
        super.init(frame: .zero)
        wantsLayer = true
        translatesAutoresizingMaskIntoConstraints = false

        if auspraegung == .leise { waagerechterRand = 12; senkrechterRand = 7 }

        beschriftung.translatesAutoresizingMaskIntoConstraints = false
        beschriftung.alignment = .center
        beschriftung.lineBreakMode = .byTruncatingTail
        beschriftung.maximumNumberOfLines = 1
        beschriftung.font = .systemFont(ofSize: schriftgroesse,
                                        weight: auspraegung == .leise ? .regular : .semibold)
        beschriftung.rolle = .fest
        addSubview(beschriftung)

        NSLayoutConstraint.activate([
            beschriftung.centerXAnchor.constraint(equalTo: centerXAnchor),
            beschriftung.centerYAnchor.constraint(equalTo: centerYAnchor),
            beschriftung.leadingAnchor.constraint(greaterThanOrEqualTo: leadingAnchor, constant: 4),
            beschriftung.trailingAnchor.constraint(lessThanOrEqualTo: trailingAnchor, constant: -4)
        ])

        setAccessibilityRole(.button)
        self.titel = titel
        // Waagerecht nachgeben: lieber eine Beschriftung mit Auslassungspunkten als ein Fenster,
        // das breiter wird als der Bildschirm.
        setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        NotificationCenter.default.addObserver(self, selector: #selector(farbenAnwenden),
                                               name: Darstellung.gewechselt, object: nil)
        farbenAnwenden()
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }
    deinit { NotificationCenter.default.removeObserver(self) }

    override var intrinsicContentSize: NSSize {
        let groesse = beschriftung.intrinsicContentSize
        return NSSize(width: groesse.width + waagerechterRand * 2,
                      height: groesse.height + senkrechterRand * 2)
    }

    override func updateTrackingAreas() {
        super.updateTrackingAreas()
        if let beobachtung { removeTrackingArea(beobachtung) }
        let bereich = NSTrackingArea(rect: bounds, options: [.mouseEnteredAndExited, .activeInActiveApp],
                                     owner: self)
        addTrackingArea(bereich)
        beobachtung = bereich
    }

    override func mouseEntered(with event: NSEvent) { if isEnabled { unterMaus = true } }
    override func mouseExited(with event: NSEvent) { unterMaus = false; gedrueckt = false }
    override func mouseDown(with event: NSEvent) { if isEnabled { gedrueckt = true } }

    override func mouseUp(with event: NSEvent) {
        let warGedrueckt = gedrueckt
        gedrueckt = false
        guard isEnabled, warGedrueckt,
              bounds.contains(convert(event.locationInWindow, from: nil)) else { return }
        ausloesen()
    }

    private func ausloesen() {
        if let action, let target { NSApp.sendAction(action, to: target, from: self) }
    }

    override func accessibilityPerformPress() -> Bool {
        guard isEnabled else { return false }
        ausloesen()
        return true
    }

    override func resetCursorRects() {
        addCursorRect(bounds, cursor: isEnabled ? .pointingHand : .arrow)
    }

    @objc func farbenAnwenden() {
        let satz = Darstellung.satz
        layer?.cornerRadius = 9
        layer?.borderWidth = 1

        // Die Deckung ersetzt die Opacity-Trigger aus dem WPF-ControlTemplate.
        let deckung: CGFloat = !isEnabled ? 0.4 : (gedrueckt ? 0.7 : (unterMaus ? 0.86 : 1.0))
        alphaValue = deckung

        if betont {
            layer?.backgroundColor = satz.akzentVerlaufOben.cgColor
            layer?.borderColor = satz.akzentRand.cgColor
            beschriftung.festeFarbe = satz.aufAkzent
            window?.invalidateCursorRects(for: self)
            return
        }

        switch auspraegung {
        case .haupt:
            layer?.backgroundColor = satz.akzentVerlaufOben.cgColor
            layer?.borderColor = satz.akzentRand.cgColor
            beschriftung.festeFarbe = satz.aufAkzent
        case .zweit:
            layer?.backgroundColor = satz.flaecheHoch.cgColor
            layer?.borderColor = satz.rand.cgColor
            beschriftung.festeFarbe = satz.text
        case .leise:
            layer?.backgroundColor = NSColor.clear.cgColor
            layer?.borderColor = satz.rand.cgColor
            beschriftung.festeFarbe = satz.textLeise
        case .ruhig:
            layer?.backgroundColor = satz.plaketteFlaeche.cgColor
            layer?.borderColor = satz.plaketteRand.cgColor
            beschriftung.festeFarbe = satz.textLeise
        }
        window?.invalidateCursorRects(for: self)
    }
}

// MARK: - Schiebeschalter (Style "Schiebeschalter")

final class Schiebeschalter: NSControl {
    private let bahn = ThemedView()
    private let knopf = ThemedView()
    private var knopfLinks: NSLayoutConstraint!
    private var knopfRechts: NSLayoutConstraint!

    var istAn = false {
        didSet {
            guard istAn != oldValue else { return }
            stellungAnwenden()
            farbenAnwenden()
        }
    }

    var beiWechsel: ((Bool) -> Void)?

    override init(frame frameRect: NSRect) {
        super.init(frame: frameRect)
        wantsLayer = true
        translatesAutoresizingMaskIntoConstraints = false

        bahn.layer?.cornerRadius = 11.5
        bahn.layer?.borderWidth = 1
        addSubview(bahn)

        knopf.layer?.cornerRadius = 8.5
        addSubview(knopf)

        knopfLinks = knopf.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 3)
        knopfRechts = knopf.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -3)

        NSLayoutConstraint.activate([
            widthAnchor.constraint(equalToConstant: 42),
            heightAnchor.constraint(equalToConstant: 23),
            bahn.leadingAnchor.constraint(equalTo: leadingAnchor),
            bahn.trailingAnchor.constraint(equalTo: trailingAnchor),
            bahn.topAnchor.constraint(equalTo: topAnchor),
            bahn.bottomAnchor.constraint(equalTo: bottomAnchor),
            knopf.widthAnchor.constraint(equalToConstant: 17),
            knopf.heightAnchor.constraint(equalToConstant: 17),
            knopf.centerYAnchor.constraint(equalTo: centerYAnchor),
            knopfLinks
        ])

        setAccessibilityRole(.checkBox)
        NotificationCenter.default.addObserver(self, selector: #selector(farbenAnwenden),
                                               name: Darstellung.gewechselt, object: nil)
        farbenAnwenden()
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }
    deinit { NotificationCenter.default.removeObserver(self) }

    override func mouseDown(with event: NSEvent) {
        istAn.toggle()
        beiWechsel?(istAn)
    }

    override func accessibilityPerformPress() -> Bool {
        istAn.toggle()
        beiWechsel?(istAn)
        return true
    }

    override func resetCursorRects() { addCursorRect(bounds, cursor: .pointingHand) }

    private func stellungAnwenden() {
        knopfLinks.isActive = !istAn
        knopfRechts.isActive = istAn
        setAccessibilityValue(istAn ? "ein" : "aus")
    }

    @objc func farbenAnwenden() {
        let satz = Darstellung.satz
        bahn.layer?.backgroundColor = (istAn ? satz.akzentVerlaufOben : satz.schalterBahn).cgColor
        bahn.layer?.borderColor = (istAn ? satz.akzentRand : satz.schalterRand).cgColor
        knopf.layer?.backgroundColor = (istAn ? NSColor.white : satz.schalterKnopf).cgColor
    }
}

// MARK: - Bildlaufleiste (Style ScrollBar)

/// Schlanker Griff ohne Rinne -- sonst rahmt macOS die Karten mit einem langen grauen Strich.
final class SchlankerRollbalken: NSScroller {
    override class var isCompatibleWithOverlayScrollers: Bool { true }

    override class func scrollerWidth(for controlSize: NSControl.ControlSize,
                                      scrollerStyle: NSScroller.Style) -> CGFloat { 11 }

    override func drawKnobSlot(in slotRect: NSRect, highlight flag: Bool) { }

    override func drawKnob() {
        let griff = rect(for: .knob)
        guard griff.width > 0, griff.height > 0 else { return }
        let balken = griff.insetBy(dx: 3, dy: 2)
        let radius = balken.width / 2
        Darstellung.satz.rollbalken.setFill()
        NSBezierPath(roundedRect: balken, xRadius: radius, yRadius: radius).fill()
    }
}

enum Rollen {
    static func anwenden(_ ansicht: NSScrollView) {
        ansicht.hasVerticalScroller = true
        ansicht.autohidesScrollers = true
        ansicht.drawsBackground = false
        ansicht.scrollerStyle = .overlay
        ansicht.verticalScroller = SchlankerRollbalken()
        ansicht.contentView.drawsBackground = false
        ansicht.backgroundColor = .clear
    }
}

// MARK: - Eingabezeile des Terminals (Style "BefehlsZeile")

final class BefehlsZeile: NSTextField {
    init() {
        super.init(frame: .zero)
        isBordered = false
        drawsBackground = false
        focusRingType = .none
        wantsLayer = true
        translatesAutoresizingMaskIntoConstraints = false
        font = .monospacedSystemFont(ofSize: 12, weight: .regular)
        placeholderString = "Befehl eingeben und mit der Eingabetaste ausführen"
        NotificationCenter.default.addObserver(self, selector: #selector(farbenAnwenden),
                                               name: Darstellung.gewechselt, object: nil)
        farbenAnwenden()
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }
    deinit { NotificationCenter.default.removeObserver(self) }

    /// Innenabstand wie `Padding="9,7"` in WPF.
    override var intrinsicContentSize: NSSize {
        var groesse = super.intrinsicContentSize
        groesse.height += 14
        return groesse
    }

    override func drawFocusRingMask() { }

    private final class GepolsterteZelle: NSTextFieldCell {
        private let abstand = NSSize(width: 9, height: 7)

        override func drawingRect(forBounds rect: NSRect) -> NSRect {
            super.drawingRect(forBounds: rect.insetBy(dx: abstand.width, dy: abstand.height))
        }

        override func select(withFrame rect: NSRect, in controlView: NSView, editor: NSText,
                             delegate: Any?, start: Int, length: Int) {
            super.select(withFrame: drawingRect(forBounds: rect), in: controlView, editor: editor,
                         delegate: delegate, start: start, length: length)
        }

        override func edit(withFrame rect: NSRect, in controlView: NSView, editor: NSText,
                           delegate: Any?, event: NSEvent?) {
            super.edit(withFrame: drawingRect(forBounds: rect), in: controlView, editor: editor,
                       delegate: delegate, event: event)
        }
    }

    override class var cellClass: AnyClass? {
        get { GepolsterteZelle.self }
        set { super.cellClass = newValue }
    }

    @objc func farbenAnwenden() {
        let satz = Darstellung.satz
        layer?.cornerRadius = 8
        layer?.borderWidth = 1
        layer?.borderColor = satz.konsoleRand.cgColor
        layer?.backgroundColor = satz.konsole.cgColor
        textColor = satz.konsoleText
    }
}

// MARK: - Konsolenbereich (Protokoll und Terminal-Ausgabe)

/// Ein rollbarer Textbereich in Konsolenfarben -- Gegenstueck zur Border mit PinselKonsole.
final class KonsolenView: FlaechenView {
    private let rollbereich = NSScrollView()
    private let textAnsicht = NSTextView()

    init() {
        super.init(ton: .konsole, eckenRadius: 9)

        rollbereich.translatesAutoresizingMaskIntoConstraints = false
        rollbereich.hasVerticalScroller = true
        rollbereich.documentView = textAnsicht
        Rollen.anwenden(rollbereich)

        textAnsicht.isEditable = false
        textAnsicht.isSelectable = true
        textAnsicht.drawsBackground = false
        textAnsicht.font = .monospacedSystemFont(ofSize: 11.5, weight: .regular)
        textAnsicht.textContainerInset = NSSize(width: 10, height: 8)
        textAnsicht.isVerticallyResizable = true
        textAnsicht.isHorizontallyResizable = false
        textAnsicht.autoresizingMask = [.width]
        textAnsicht.textContainer?.widthTracksTextView = true

        addSubview(rollbereich)
        NSLayoutConstraint.activate([
            rollbereich.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 1),
            rollbereich.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -1),
            rollbereich.topAnchor.constraint(equalTo: topAnchor, constant: 1),
            rollbereich.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -1)
        ])
        farbenAnwenden()
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }

    var text: String {
        get { textAnsicht.string }
        set {
            guard textAnsicht.string != newValue else { return }
            let warAmEnde = istAmEnde
            textAnsicht.string = newValue
            textAnsicht.textColor = Darstellung.satz.konsoleText
            // Mitlaufen, solange der Benutzer nicht selbst hochgescrollt hat.
            if warAmEnde { textAnsicht.scrollToEndOfDocument(nil) }
        }
    }

    private var istAmEnde: Bool {
        guard let dokument = rollbereich.documentView else { return true }
        let sichtbar = rollbereich.contentView.bounds
        return sichtbar.maxY >= dokument.frame.height - 24
    }

    override func farbenAnwenden() {
        super.farbenAnwenden()
        textAnsicht.textColor = Darstellung.satz.konsoleText
        textAnsicht.insertionPointColor = Darstellung.satz.konsoleText
    }
}

// MARK: - Fortschrittsbalken

/// Der schmale Balken unter den Bedienelementen, solange eine Karte arbeitet.
final class Fortschrittsbalken: NSProgressIndicator {
    init() {
        super.init(frame: .zero)
        translatesAutoresizingMaskIntoConstraints = false
        style = .bar
        isIndeterminate = true
        controlSize = .small
        heightAnchor.constraint(equalToConstant: 6).isActive = true
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }

    var laeuft: Bool = false {
        didSet {
            guard laeuft != oldValue else { return }
            isHidden = !laeuft
            if laeuft { startAnimation(nil) } else { stopAnimation(nil) }
        }
    }
}

/// Dokument-Ansicht fuer NSScrollView. AppKit rechnet sonst von UNTEN nach oben -- die Liste
/// begaenne am unteren Rand und waere beim Start ans Ende gescrollt.
final class GedrehteView: NSView {
    override var isFlipped: Bool { true }
}
