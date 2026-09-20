import AppKit

/// Eine Programmkarte. Nachbau des `ListBox.ItemTemplate` aus HauptFenster.xaml:
/// Kennzeichen | Name, Beschreibung, Merkmale, Zustand, Bänder | Bedienelemente.
///
/// Nicht uebernommen (kein macOS-Gegenstueck, siehe PORTING.md): der Schiebeschalter
/// "Als Administrator bei jedem Start", das Band "Kein Administratorstart" fuer Paket-Apps, das
/// Warnband zum Autostart mit Administratorrechten und die Warnung zu Windows-Fehler 740.
@MainActor
final class ProgrammKarteView: KartenView {

    private let modell: ProgrammViewModel

    // Kopfzeile
    private let nameBeschriftung: ThemedLabel
    private let artPlakette: PlakettenView
    private let laeuftPlakette: PlakettenView
    private let autostartPlakette: PlakettenView

    private let beschreibungBeschriftung: ThemedLabel
    private let hinweisBeschriftung: ThemedLabel

    // Zustand und Version
    private let zustandFlaeche = FlaechenView(ton: .plakette, eckenRadius: 7)
    private let zustandBeschriftung: ThemedLabel
    private let versionBeschriftung: ThemedLabel
    private let statusBeschriftung: ThemedLabel
    private let berichtBeschriftung: ThemedLabel

    // Bänder
    private let uebernahmeBand = FlaechenView(ton: .autostart, eckenRadius: 7)
    private let uebernahmeText: ThemedLabel
    private let fehlerBand = FlaechenView(ton: .fehler, eckenRadius: 7)
    private let fehlerText: ThemedLabel

    // Bedienelemente
    private let aktionsSchalter = Schalter(.ruhig, "Aktualisieren")
    private let pruefenSchalter = Schalter(.zweit, "Prüfen")
    private let startenSchalter = Schalter(.leise, "Starten")
    private let balken = Fortschrittsbalken()

    init(modell: ProgrammViewModel) {
        self.modell = modell

        nameBeschriftung = UI.beschriftung(modell.name, groesse: 14, gewicht: .semibold, einzeilig: true)
        artPlakette = PlakettenView(text: modell.artText, ton: .plakette)
        laeuftPlakette = PlakettenView(text: "läuft gerade", ton: .laeuft)
        autostartPlakette = PlakettenView(text: "Autostart", ton: .autostart)

        beschreibungBeschriftung = UI.beschriftung(modell.beschreibung, rolle: .leise)
        hinweisBeschriftung = UI.beschriftung(modell.hinweis ?? "", rolle: .sehrLeise, kursiv: true)

        zustandBeschriftung = UI.beschriftung("", groesse: 11, gewicht: .semibold, rolle: .fest, einzeilig: true)
        versionBeschriftung = UI.beschriftung("–", groesse: 12, rolle: .leise, mono: true, einzeilig: true)
        statusBeschriftung = UI.beschriftung("", rolle: .leise)
        berichtBeschriftung = UI.beschriftung("", groesse: 11, rolle: .sehrLeise)

        uebernahmeText = UI.beschriftung("", groesse: 11, rolle: .fest)
        fehlerText = UI.beschriftung("", groesse: 11, rolle: .fest)

        super.init()

        aufbauen()
        aktualisieren()

        modell.beiAenderung = { [weak self] in self?.aktualisieren() }
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }

    // MARK: - Aufbau

    private func aufbauen() {
        let kennzeichen = KennzeichenView(kuerzel: modell.kuerzel, akzent: modell.akzent, groesse: 40)
        addSubview(kennzeichen)

        // --- Mitte ---
        let plakettenReihe = NSStackView(views: [nameBeschriftung, artPlakette, laeuftPlakette, autostartPlakette])
        plakettenReihe.orientation = .horizontal
        plakettenReihe.alignment = .centerY
        plakettenReihe.spacing = 7
        plakettenReihe.translatesAutoresizingMaskIntoConstraints = false
        // Wird es in der Kopfzeile eng, kuerzt der Name -- die Plaketten daneben tragen jeweils
        // nur ein Wort und saehen abgeschnitten sinnlos aus.
        nameBeschriftung.setContentCompressionResistancePriority(.defaultHigh, for: .horizontal)

        // Zustands-Kennzeichen plus Versionszeile.
        zustandFlaeche.addSubview(zustandBeschriftung)
        NSLayoutConstraint.activate([
            zustandBeschriftung.leadingAnchor.constraint(equalTo: zustandFlaeche.leadingAnchor, constant: 9),
            zustandBeschriftung.trailingAnchor.constraint(equalTo: zustandFlaeche.trailingAnchor, constant: -9),
            zustandBeschriftung.topAnchor.constraint(equalTo: zustandFlaeche.topAnchor, constant: 4),
            zustandBeschriftung.bottomAnchor.constraint(equalTo: zustandFlaeche.bottomAnchor, constant: -4)
        ])
        zustandFlaeche.zeigtRand = false
        zustandFlaeche.setContentHuggingPriority(.required, for: .horizontal)

        let zustandReihe = NSStackView(views: [zustandFlaeche, versionBeschriftung, UI.platzhalter()])
        zustandReihe.orientation = .horizontal
        zustandReihe.alignment = .centerY
        zustandReihe.spacing = 10
        zustandReihe.translatesAutoresizingMaskIntoConstraints = false

        bandAufbauen(uebernahmeBand, text: uebernahmeText, schalter: [
            ("Jetzt starten und übernehmen", #selector(startenGeklickt)),
            ("Protokoll öffnen", #selector(protokollGeklickt))
        ])
        bandAufbauen(fehlerBand, text: fehlerText, schalter: [
            ("Protokoll öffnen", #selector(protokollGeklickt))
        ])

        let mitte = NSStackView(views: [
            plakettenReihe, beschreibungBeschriftung, hinweisBeschriftung,
            zustandReihe, statusBeschriftung, berichtBeschriftung,
            uebernahmeBand, fehlerBand
        ])
        mitte.orientation = .vertical
        mitte.alignment = .leading
        mitte.spacing = 5
        mitte.setCustomSpacing(9, after: hinweisBeschriftung)
        mitte.setCustomSpacing(8, after: berichtBeschriftung)
        mitte.translatesAutoresizingMaskIntoConstraints = false
        addSubview(mitte)

        // --- Bedienelemente rechts ---
        aktionsSchalter.target = self
        aktionsSchalter.action = #selector(aktualisierenGeklickt)
        pruefenSchalter.target = self
        pruefenSchalter.action = #selector(pruefenGeklickt)
        startenSchalter.target = self
        startenSchalter.action = #selector(startenGeklickt)

        let kleineReihe = NSStackView(views: [pruefenSchalter, startenSchalter])
        kleineReihe.orientation = .horizontal
        kleineReihe.distribution = .fillEqually
        kleineReihe.spacing = 8
        kleineReihe.translatesAutoresizingMaskIntoConstraints = false

        balken.isHidden = true

        let rechts = NSStackView(views: [aktionsSchalter, kleineReihe, balken])
        rechts.orientation = .vertical
        rechts.alignment = .centerX
        rechts.spacing = 8
        rechts.translatesAutoresizingMaskIntoConstraints = false
        addSubview(rechts)

        NSLayoutConstraint.activate([
            kennzeichen.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 14),
            kennzeichen.topAnchor.constraint(equalTo: topAnchor, constant: 16),

            mitte.leadingAnchor.constraint(equalTo: kennzeichen.trailingAnchor, constant: 14),
            mitte.topAnchor.constraint(equalTo: topAnchor, constant: 14),
            mitte.bottomAnchor.constraint(lessThanOrEqualTo: bottomAnchor, constant: -14),
            mitte.trailingAnchor.constraint(equalTo: rechts.leadingAnchor, constant: -14),

            rechts.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -14),
            rechts.centerYAnchor.constraint(equalTo: centerYAnchor),
            rechts.topAnchor.constraint(greaterThanOrEqualTo: topAnchor, constant: 14),
            rechts.widthAnchor.constraint(equalToConstant: 148),

            aktionsSchalter.widthAnchor.constraint(equalTo: rechts.widthAnchor),
            kleineReihe.widthAnchor.constraint(equalTo: rechts.widthAnchor),
            balken.widthAnchor.constraint(equalTo: rechts.widthAnchor),

            heightAnchor.constraint(greaterThanOrEqualToConstant: 104)
        ])

        // Die Mitte darf waagerecht nicht ueber ihren Platz hinauswachsen, sonst schiebt ein
        // langer Beschreibungstext die Bedienelemente aus dem Fenster.
        for beschriftung in [beschreibungBeschriftung, hinweisBeschriftung, statusBeschriftung,
                             berichtBeschriftung] {
            beschriftung.widthAnchor.constraint(equalTo: mitte.widthAnchor).isActive = true
        }
        plakettenReihe.widthAnchor.constraint(lessThanOrEqualTo: mitte.widthAnchor).isActive = true
        uebernahmeBand.widthAnchor.constraint(equalTo: mitte.widthAnchor).isActive = true
        fehlerBand.widthAnchor.constraint(equalTo: mitte.widthAnchor).isActive = true
    }

    /// Ein farbiges Hinweisband mit Text und ein bis zwei Schaltflaechen darunter.
    private func bandAufbauen(_ band: FlaechenView, text: ThemedLabel,
                              schalter: [(String, Selector)]) {
        let knoepfe = schalter.map { titel, aktion -> Schalter in
            let knopf = Schalter(.leise, titel)
            knopf.schriftgroesse = 11
            knopf.waagerechterRand = 9
            knopf.senkrechterRand = 4
            knopf.target = self
            knopf.action = aktion
            return knopf
        }

        let knopfReihe = NSStackView(views: knoepfe + [UI.platzhalter()])
        knopfReihe.orientation = .horizontal
        knopfReihe.spacing = 6
        knopfReihe.translatesAutoresizingMaskIntoConstraints = false

        let inhalt = NSStackView(views: [text, knopfReihe])
        inhalt.orientation = .vertical
        inhalt.alignment = .leading
        inhalt.spacing = 7
        inhalt.translatesAutoresizingMaskIntoConstraints = false

        band.addSubview(inhalt)
        NSLayoutConstraint.activate([
            inhalt.leadingAnchor.constraint(equalTo: band.leadingAnchor, constant: 9),
            inhalt.trailingAnchor.constraint(equalTo: band.trailingAnchor, constant: -9),
            inhalt.topAnchor.constraint(equalTo: band.topAnchor, constant: 6),
            inhalt.bottomAnchor.constraint(equalTo: band.bottomAnchor, constant: -6),
            text.widthAnchor.constraint(equalTo: inhalt.widthAnchor)
        ])
        band.isHidden = true
    }

    // MARK: - Anzeige nachziehen

    func aktualisieren() {
        let satz = Darstellung.satz

        laeuftPlakette.isHidden = !modell.laeuft
        autostartPlakette.isHidden = !modell.imAutostart
        hinweisBeschriftung.isHidden = !modell.hatHinweis
        hinweisBeschriftung.stringValue = modell.hinweis ?? ""

        zustandBeschriftung.stringValue = Zustandsfarben.text(modell.zustand)
        zustandBeschriftung.festeFarbe = Zustandsfarben.vordergrund(modell.zustand)
        zustandFlaeche.layer?.backgroundColor = Zustandsfarben.hintergrund(modell.zustand).cgColor

        versionBeschriftung.stringValue = modell.versionsText
        statusBeschriftung.stringValue = modell.statusText

        berichtBeschriftung.stringValue = modell.berichtKurz
        berichtBeschriftung.isHidden = !modell.hatBericht

        uebernahmeBand.isHidden = !modell.uebernahmeOffen
        uebernahmeText.stringValue = modell.uebernahmeText
        uebernahmeText.festeFarbe = satz.autostartText

        fehlerBand.isHidden = !modell.berichtIstFehler
        fehlerText.stringValue = modell.berichtGrund
        fehlerText.festeFarbe = satz.fehlerText

        aktionsSchalter.titel = modell.aktionsText
        aktionsSchalter.isEnabled = modell.aktionMoeglich
        aktionsSchalter.betont = modell.aktionBetont

        pruefenSchalter.isEnabled = modell.kannPruefen
        startenSchalter.isEnabled = modell.kannStarten

        balken.laeuft = modell.istBeschaeftigt

        // Die Zustandsfarben haengen am Hell/Dunkel-Modus und muessen beim Wechsel mit.
        farbenAnwenden()
    }

    override func farbenAnwenden() {
        super.farbenAnwenden()
        zustandFlaeche.layer?.backgroundColor = Zustandsfarben.hintergrund(modell.zustand).cgColor
        zustandBeschriftung.festeFarbe = Zustandsfarben.vordergrund(modell.zustand)
    }

    // MARK: - Aktionen

    @objc private func aktualisierenGeklickt() {
        Task { await modell.aktualisieren() }
    }

    @objc private func pruefenGeklickt() {
        Task { await modell.pruefen() }
    }

    @objc private func startenGeklickt() {
        modell.starten()
    }

    @objc private func protokollGeklickt() {
        modell.protokollOeffnen()
    }
}
