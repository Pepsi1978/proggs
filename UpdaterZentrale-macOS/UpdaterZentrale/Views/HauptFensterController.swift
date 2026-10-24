import AppKit

/// Hauptfenster. Nachbau von HauptFenster.xaml:
///   Kopfzeile | Kartenliste ‖ Protokoll und Terminal | Fußzeile.
///
/// Nicht uebernommen (kein macOS-Gegenstueck, siehe PORTING.md): die Plakette "Administrator" und
/// die Schaltflaeche "Als Administrator neu starten" in der Kopfzeile sowie der Schiebeschalter
/// "Immer als Administrator starten" in der Fußzeile.
@MainActor
final class HauptFensterController: NSWindowController, NSWindowDelegate {

    private let modell: HauptViewModel

    // Kopfzeile
    private let zusammenfassungPlakette: PlakettenView
    private let designSchalter = Schiebeschalter()
    private let dauerSchalter = Schiebeschalter()
    private let allePruefenSchalter = Schalter(.zweit, "Alle prüfen")
    private let neuLadenSchalter = Schalter(.leise, "Neu laden")
    private let diagnoseSchalter = Schalter(.leise, "Diagnose exportieren")
    /// Zwei unabhaengige Sammelknoepfe: die Reihenfolge waehlt der Benutzer selbst.
    private let cliUpdatesSchalter = Schalter(.zweit, "CLI-kritische Updates installieren")
    private let sichereUpdatesSchalter = Schalter(.haupt, "Updates installieren")
    private let katalogFehlerBand = FlaechenView(ton: .fehler, eckenRadius: 9)
    private let katalogFehlerText: ThemedLabel

    // Liste
    private let listenRollbereich = NSScrollView()
    private let listenInhalt = GedrehteView()
    private var listenStapel = NSStackView()
    private var karten: [ProgrammKarteView] = []

    // Rechte Spalte
    private let rechtesPanel = FlaechenView(ton: .tief, eckenRadius: 14)
    private let protokollUeberschrift: ThemedLabel
    private let gewaehltesProgramm: ThemedLabel
    private let statusFlaeche = FlaechenView(ton: .hoch, eckenRadius: 9)
    private let statusText: ThemedLabel
    private let statusVersion: ThemedLabel
    private let protokollKonsole = KonsolenView()
    private let terminalUeberschrift: ThemedLabel
    private let terminalKonsole = KonsolenView()
    private let befehlsZeile = BefehlsZeile()
    private let ausfuehrenSchalter = Schalter(.zweit, "Ausführen")

    // Fußzeile
    private let fussStatus: ThemedLabel
    private let fussDetail: ThemedLabel
    private let fussBalken = NSProgressIndicator()
    private let versionsAnzeige: ThemedLabel

    private let hintergrund = ThemedView()

    init(modell: HauptViewModel) {
        self.modell = modell

        zusammenfassungPlakette = PlakettenView(text: modell.updateZusammenfassung, ton: .plakette,
                                                groesse: 12)
        katalogFehlerText = UI.beschriftung("", groesse: 12.5, rolle: .fest)
        protokollUeberschrift = UI.beschriftung("Protokoll", groesse: 14, gewicht: .semibold, einzeilig: true)
        gewaehltesProgramm = UI.beschriftung("", rolle: .leise, einzeilig: true)
        statusText = UI.beschriftung("", groesse: 12)
        statusVersion = UI.beschriftung("", groesse: 11.5, rolle: .leise, mono: true, einzeilig: true)
        terminalUeberschrift = UI.beschriftung(modell.terminalKopf, groesse: 14, gewicht: .semibold, einzeilig: true)
        // Der Status steht vollstaendig da (umbrechend statt gekuerzt), darunter waehrend eines
        // Sammellaufs je laufendem Update eine Zeile mit Position, Phase, Startzeit und Erwartung.
        fussStatus = UI.beschriftung(modell.kopfStatus, rolle: .leise)
        fussDetail = UI.beschriftung("", groesse: 11.5, rolle: .leise, mono: true)
        versionsAnzeige = UI.beschriftung(modell.anwendungsVersion, rolle: .leise, einzeilig: true)

        // Fenstergroesse nach macOS-Verhaeltnissen, NICHT nach den Windows-Maßen.
        //
        // Die Windows-Fassung startet mit 1340 x 880 bei einer Mindestgroesse von 1060 x 620. Auf
        // einem MacBook-Display sind das aber nur 1280 x 832 Punkte, und davon gehen Menueleiste
        // und Dock noch ab: ein 1340 Punkte breites Fenster ragte links aus dem Bild, und die
        // Mindestbreite haette sich nicht einmal mehr kleiner ziehen lassen.
        //
        // Deshalb richtet sich die Groesse hier nach dem wirklich nutzbaren Bereich: rund neun
        // Zehntel davon, gedeckelt auf eine angenehme Groesse, zentriert wie jedes Mac-Fenster.
        let sichtbar = NSScreen.main?.visibleFrame ?? NSRect(x: 0, y: 0, width: 1280, height: 720)
        let breite = min(1180, sichtbar.width * 0.94)
        let hoehe = min(800, sichtbar.height * 0.94)

        let fenster = NSWindow(
            contentRect: NSRect(x: 0, y: 0, width: breite, height: hoehe),
            styleMask: [.titled, .closable, .miniaturizable, .resizable],
            backing: .buffered, defer: false)
        fenster.title = "Updater-Zentrale macOS"
        // Die Mindestgroesse darf den Bildschirm nie ueberschreiten, sonst laesst sich das Fenster
        // nicht mehr auf eine brauchbare Groesse ziehen.
        fenster.minSize = NSSize(width: min(880, breite), height: min(540, hoehe))
        // Zweite Sicherung gegen ein aufgeblaehtes Fenster: mehr als der sichtbare Bereich darf es
        // nie werden, auch wenn irgendein Inhalt doch einmal mehr Platz verlangt.
        fenster.maxSize = NSSize(width: sichtbar.width, height: sichtbar.height)
        fenster.center()

        super.init(window: fenster)
        fenster.delegate = self

        aufbauen()
        designAnwenden()
        aktualisieren()
        kartenNeuBauen()

        modell.beiAenderung = { [weak self] in self?.aktualisieren() }
        modell.beiListenWechsel = { [weak self] in self?.kartenNeuBauen() }

        NotificationCenter.default.addObserver(self, selector: #selector(designAnwenden),
                                               name: Darstellung.gewechselt, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(bildschirmeGeaendert),
                                               name: NSApplication.didChangeScreenParametersNotification,
                                               object: nil)
    }

    required init?(coder: NSCoder) { fatalError("init(coder:) wird nicht verwendet") }
    deinit { NotificationCenter.default.removeObserver(self) }

    // MARK: - Aufbau

    private func aufbauen() {
        guard let inhalt = window?.contentView else { return }

        hintergrund.translatesAutoresizingMaskIntoConstraints = false
        inhalt.addSubview(hintergrund)
        NSLayoutConstraint.activate([
            hintergrund.leadingAnchor.constraint(equalTo: inhalt.leadingAnchor),
            hintergrund.trailingAnchor.constraint(equalTo: inhalt.trailingAnchor),
            hintergrund.topAnchor.constraint(equalTo: inhalt.topAnchor),
            hintergrund.bottomAnchor.constraint(equalTo: inhalt.bottomAnchor)
        ])

        let kopf = kopfzeileBauen()
        let mitte = hauptbereichBauen()
        let fuss = fusszeileBauen()

        for ansicht in [kopf, katalogFehlerBand, mitte, fuss] {
            ansicht.translatesAutoresizingMaskIntoConstraints = false
            inhalt.addSubview(ansicht)
        }

        katalogFehlerBand.isHidden = true
        // Ausgeblendet belegte das Band samt Abstaenden sonst rund 60 Punkte Hoehe.
        fehlerBandOben = katalogFehlerBand.topAnchor.constraint(equalTo: kopf.bottomAnchor, constant: 10)
        fehlerBandZu = katalogFehlerBand.heightAnchor.constraint(equalToConstant: 0)
        fehlerBandZu.isActive = true
        katalogFehlerText.isHidden = true

        NSLayoutConstraint.activate([
            kopf.leadingAnchor.constraint(equalTo: inhalt.leadingAnchor, constant: 20),
            kopf.trailingAnchor.constraint(equalTo: inhalt.trailingAnchor, constant: -20),
            kopf.topAnchor.constraint(equalTo: inhalt.topAnchor, constant: 16),

            katalogFehlerBand.leadingAnchor.constraint(equalTo: kopf.leadingAnchor),
            katalogFehlerBand.trailingAnchor.constraint(equalTo: kopf.trailingAnchor),
            fehlerBandOben,

            mitte.leadingAnchor.constraint(equalTo: kopf.leadingAnchor),
            mitte.trailingAnchor.constraint(equalTo: kopf.trailingAnchor),
            mitte.topAnchor.constraint(equalTo: katalogFehlerBand.bottomAnchor, constant: 12),
            mitte.bottomAnchor.constraint(equalTo: fuss.topAnchor, constant: -12),

            fuss.leadingAnchor.constraint(equalTo: kopf.leadingAnchor),
            fuss.trailingAnchor.constraint(equalTo: kopf.trailingAnchor),
            fuss.bottomAnchor.constraint(equalTo: inhalt.bottomAnchor, constant: -14)
        ])
    }

    private func kopfzeileBauen() -> NSView {
        // Logo-Kachel mit dem Aufwaertspfeil aus der Windows-Fassung.
        let kachel = ThemedView()
        kachel.layer?.cornerRadius = 10
        let pfeil = UI.beschriftung("↑", groesse: 16, gewicht: .bold, rolle: .fest, einzeilig: true)
        pfeil.alignment = .center
        kachel.addSubview(pfeil)
        NSLayoutConstraint.activate([
            kachel.widthAnchor.constraint(equalToConstant: 28),
            kachel.heightAnchor.constraint(equalToConstant: 28),
            pfeil.centerXAnchor.constraint(equalTo: kachel.centerXAnchor),
            pfeil.centerYAnchor.constraint(equalTo: kachel.centerYAnchor)
        ])
        logoKachel = kachel
        logoPfeil = pfeil

        let titel = UI.beschriftung("Updater-Zentrale macOS", groesse: 18, gewicht: .semibold, einzeilig: true)

        let titelReihe = NSStackView(views: [kachel, titel, zusammenfassungPlakette, UI.platzhalter()])
        titelReihe.orientation = .horizontal
        titelReihe.alignment = .centerY
        titelReihe.spacing = 12
        titelReihe.translatesAutoresizingMaskIntoConstraints = false

        let untertitel = UI.beschriftung("Alle Werkzeuge an einem Ort prüfen und aktualisieren.", rolle: .leise, einzeilig: true)

        // Hell/Dunkel-Umschalter im gerahmten Kaestchen, wie unter Windows.
        designSchalter.istAn = modell.hellModus
        designSchalter.beiWechsel = { [weak self] an in self?.modell.hellModus = an }
        let designRahmen = gerahmt([UI.beschriftung("🌙", groesse: 13, einzeilig: true), designSchalter,
                                    UI.beschriftung("☀", groesse: 13, einzeilig: true)])

        // Sortierung innerhalb der Gruppen: Katalog oder kuerzestes Update zuerst.
        dauerSchalter.istAn = modell.nachDauerSortieren
        dauerSchalter.beiWechsel = { [weak self] an in self?.modell.nachDauerSortieren = an }
        let dauerRahmen = gerahmt([UI.beschriftung("⏱ nach Dauer", groesse: 12, einzeilig: true), dauerSchalter])
        dauerRahmen.toolTip = "Sortiert die Karten innerhalb jeder Gruppe nach der gemessenen bzw. geschätzten Update-Dauer, die kürzesten oben."

        let katalogOeffnen = Schalter(.leise, "Katalog öffnen")
        katalogOeffnen.target = self
        katalogOeffnen.action = #selector(katalogOeffnenGeklickt)
        katalogOeffnen.toolTip = "programs.json öffnen – dort werden neue Programme eingetragen."

        let neuLaden = neuLadenSchalter
        neuLaden.target = self
        neuLaden.action = #selector(neuLadenGeklickt)

        allePruefenSchalter.target = self
        allePruefenSchalter.action = #selector(allePruefenGeklickt)
        cliUpdatesSchalter.target = self
        cliUpdatesSchalter.action = #selector(cliUpdatesGeklickt)
        cliUpdatesSchalter.toolTip = "Updates, die laufende CLI-Sitzungen (Claude Code, Codex, Terminals) beenden können. Vorher die CLIs schließen."
        sichereUpdatesSchalter.target = self
        sichereUpdatesSchalter.action = #selector(sichereUpdatesGeklickt)
        sichereUpdatesSchalter.toolTip = "Alle Updates, die keine CLI-Sitzung unterbrechen – eigene Werkzeuge zuerst, dann die kürzesten."

        // Drei Zeilen statt zwei Spalten (wie die Windows-Fassung seit 1.3.32): nebeneinander
        // drueckte die breite Bedienleiste den Titelblock zusammen. AppKit hat kein WrapPanel, also
        // verteilt sich die Bedienleiste fest auf zwei Zeilen, die beide in das schmalste Fenster passen.
        // Hoehe ist auf dem MacBook knapp (832 Punkte samt Menueleiste und Dock): deshalb nur ZWEI
        // Zeilen. Oben Titel und die beiden Schiebeschalter, darunter alle Schaltflaechen.
        titelReihe.addArrangedSubview(designRahmen)
        titelReihe.addArrangedSubview(dauerRahmen)
        titelReihe.setCustomSpacing(8, after: designRahmen)

        let zeileZwei = NSStackView(views: [untertitel, UI.platzhalter(), katalogOeffnen, neuLaden, allePruefenSchalter,
                                            cliUpdatesSchalter, sichereUpdatesSchalter])
        zeileZwei.orientation = .horizontal
        zeileZwei.alignment = .centerY
        zeileZwei.spacing = 8
        zeileZwei.setCustomSpacing(16, after: untertitel)
        zeileZwei.detachesHiddenViews = true
        zeileZwei.translatesAutoresizingMaskIntoConstraints = false
        // Wird es eng, kuerzt zuerst der Untertitel.
        untertitel.setContentCompressionResistancePriority(.init(rawValue: 250), for: .horizontal)

        for knopf in [katalogOeffnen, neuLaden, allePruefenSchalter, cliUpdatesSchalter, sichereUpdatesSchalter] {
            knopf.setContentCompressionResistancePriority(UI.schalterFestigkeit, for: .horizontal)
        }

        let reihe = NSStackView(views: [titelReihe, zeileZwei])
        reihe.orientation = .vertical
        reihe.alignment = .leading
        reihe.spacing = 8
        reihe.translatesAutoresizingMaskIntoConstraints = false
        for zeile in [titelReihe, zeileZwei] {
            zeile.widthAnchor.constraint(equalTo: reihe.widthAnchor).isActive = true
        }

        // Katalogfehler-Band vorbereiten.
        katalogFehlerBand.addSubview(katalogFehlerText)
        NSLayoutConstraint.activate([
            katalogFehlerText.leadingAnchor.constraint(equalTo: katalogFehlerBand.leadingAnchor, constant: 14),
            katalogFehlerText.trailingAnchor.constraint(equalTo: katalogFehlerBand.trailingAnchor, constant: -14),
            katalogFehlerText.topAnchor.constraint(equalTo: katalogFehlerBand.topAnchor, constant: 10),
            katalogFehlerText.bottomAnchor.constraint(equalTo: katalogFehlerBand.bottomAnchor, constant: -10)
        ])

        return reihe
    }

    /// Ein gerahmtes Kaestchen mit Beschriftung und Schiebeschalter, wie in der Windows-Kopfzeile.
    private func gerahmt(_ ansichten: [NSView]) -> FlaechenView {
        let reihe = NSStackView(views: ansichten)
        reihe.orientation = .horizontal
        reihe.alignment = .centerY
        reihe.spacing = 7
        reihe.translatesAutoresizingMaskIntoConstraints = false
        let rahmen = FlaechenView(ton: .hoch, eckenRadius: 9)
        rahmen.addSubview(reihe)
        NSLayoutConstraint.activate([
            reihe.leadingAnchor.constraint(equalTo: rahmen.leadingAnchor, constant: 11),
            reihe.trailingAnchor.constraint(equalTo: rahmen.trailingAnchor, constant: -11),
            reihe.topAnchor.constraint(equalTo: rahmen.topAnchor, constant: 6),
            reihe.bottomAnchor.constraint(equalTo: rahmen.bottomAnchor, constant: -6)
        ])
        return rahmen
    }

    private var fehlerBandOben: NSLayoutConstraint!
    private var fehlerBandZu: NSLayoutConstraint!

    private var logoKachel: ThemedView?
    private var logoPfeil: ThemedLabel?

    private func hauptbereichBauen() -> NSView {
        // --- Liste ---
        listenStapel.orientation = .vertical
        listenStapel.alignment = .leading
        listenStapel.spacing = 10
        listenStapel.translatesAutoresizingMaskIntoConstraints = false

        listenInhalt.translatesAutoresizingMaskIntoConstraints = false
        listenInhalt.addSubview(listenStapel)
        NSLayoutConstraint.activate([
            listenStapel.leadingAnchor.constraint(equalTo: listenInhalt.leadingAnchor),
            listenStapel.trailingAnchor.constraint(equalTo: listenInhalt.trailingAnchor, constant: -10),
            listenStapel.topAnchor.constraint(equalTo: listenInhalt.topAnchor),
            listenStapel.bottomAnchor.constraint(equalTo: listenInhalt.bottomAnchor)
        ])

        listenRollbereich.translatesAutoresizingMaskIntoConstraints = false
        listenRollbereich.documentView = listenInhalt
        listenRollbereich.hasHorizontalScroller = false
        Rollen.anwenden(listenRollbereich)
        listenInhalt.widthAnchor.constraint(equalTo: listenRollbereich.widthAnchor).isActive = true

        // --- Rechte Spalte ---
        rechtesPanelBauen()

        let teiler = NSSplitView()
        teiler.isVertical = true
        teiler.dividerStyle = .thin
        teiler.translatesAutoresizingMaskIntoConstraints = false
        teiler.addArrangedSubview(listenRollbereich)
        teiler.addArrangedSubview(rechtesPanel)
        teiler.setHoldingPriority(.defaultLow, forSubviewAt: 0)
        teiler.setHoldingPriority(.defaultHigh, forSubviewAt: 1)
        rechtesPanel.widthAnchor.constraint(greaterThanOrEqualToConstant: 300).isActive = true

        // Startbreite: rechte Spalte rund 360 pt, hoechstens die halbe Fensterbreite.
        DispatchQueue.main.async { [weak self, weak teiler] in
            guard let teiler, teiler.frame.width > 0 else { return }
            self?.anBildschirmAnpassen()
            teiler.layoutSubtreeIfNeeded()
            teiler.setPosition(max(teiler.frame.width - 360, teiler.frame.width * 0.5), ofDividerAt: 0)
        }

        return teiler
    }

    private func rechtesPanelBauen() {
        let kopf = NSStackView(views: [protokollUeberschrift, gewaehltesProgramm])
        kopf.orientation = .vertical
        kopf.alignment = .leading
        kopf.spacing = 2
        kopf.translatesAutoresizingMaskIntoConstraints = false

        let statusStapel = NSStackView(views: [statusText, statusVersion])
        statusStapel.orientation = .vertical
        statusStapel.alignment = .leading
        statusStapel.spacing = 5
        statusStapel.translatesAutoresizingMaskIntoConstraints = false
        statusFlaeche.addSubview(statusStapel)
        // Ohne feste Breite bliebe der umbrechende Statustext auf der schmalen Breite seines
        // ersten Layouts haengen ("Auf dem / neuesten / Stand").
        statusText.widthAnchor.constraint(equalTo: statusStapel.widthAnchor).isActive = true
        NSLayoutConstraint.activate([
            statusStapel.leadingAnchor.constraint(equalTo: statusFlaeche.leadingAnchor, constant: 11),
            statusStapel.trailingAnchor.constraint(equalTo: statusFlaeche.trailingAnchor, constant: -11),
            statusStapel.topAnchor.constraint(equalTo: statusFlaeche.topAnchor, constant: 9),
            statusStapel.bottomAnchor.constraint(equalTo: statusFlaeche.bottomAnchor, constant: -9)
        ])

        let terminalFenster = Schalter(.leise, "Fenster")
        terminalFenster.schriftgroesse = 11
        terminalFenster.waagerechterRand = 9
        terminalFenster.senkrechterRand = 4
        terminalFenster.setContentCompressionResistancePriority(UI.schalterFestigkeit, for: .horizontal)
        terminalFenster.target = self
        terminalFenster.action = #selector(terminalFensterGeklickt)

        let terminalLeeren = Schalter(.leise, "Leeren")
        terminalLeeren.schriftgroesse = 11
        terminalLeeren.waagerechterRand = 9
        terminalLeeren.senkrechterRand = 4
        terminalLeeren.setContentCompressionResistancePriority(UI.schalterFestigkeit, for: .horizontal)
        terminalLeeren.target = self
        terminalLeeren.action = #selector(terminalLeerenGeklickt)

        let terminalKopf = NSStackView(views: [terminalUeberschrift, UI.platzhalter(),
                                               terminalFenster, terminalLeeren])
        terminalKopf.orientation = .horizontal
        terminalKopf.alignment = .centerY
        terminalKopf.spacing = 6
        terminalKopf.translatesAutoresizingMaskIntoConstraints = false

        befehlsZeile.target = self
        befehlsZeile.action = #selector(befehlAusfuehrenGeklickt)
        ausfuehrenSchalter.target = self
        ausfuehrenSchalter.action = #selector(befehlAusfuehrenGeklickt)
        ausfuehrenSchalter.waagerechterRand = 13
        ausfuehrenSchalter.senkrechterRand = 7
        ausfuehrenSchalter.setContentCompressionResistancePriority(UI.schalterFestigkeit, for: .horizontal)

        let eingabeReihe = NSStackView(views: [befehlsZeile, ausfuehrenSchalter])
        eingabeReihe.orientation = .horizontal
        eingabeReihe.alignment = .centerY
        eingabeReihe.spacing = 8
        eingabeReihe.translatesAutoresizingMaskIntoConstraints = false

        let stapel = NSStackView(views: [kopf, statusFlaeche, protokollKonsole,
                                         terminalKopf, terminalKonsole, eingabeReihe])
        stapel.orientation = .vertical
        stapel.alignment = .leading
        stapel.spacing = 10
        stapel.setCustomSpacing(16, after: protokollKonsole)
        stapel.translatesAutoresizingMaskIntoConstraints = false

        rechtesPanel.addSubview(stapel)
        NSLayoutConstraint.activate([
            stapel.leadingAnchor.constraint(equalTo: rechtesPanel.leadingAnchor, constant: 16),
            stapel.trailingAnchor.constraint(equalTo: rechtesPanel.trailingAnchor, constant: -16),
            stapel.topAnchor.constraint(equalTo: rechtesPanel.topAnchor, constant: 14),
            stapel.bottomAnchor.constraint(equalTo: rechtesPanel.bottomAnchor, constant: -14)
        ])

        for ansicht in [kopf, statusFlaeche, protokollKonsole, terminalKopf, terminalKonsole, eingabeReihe] {
            ansicht.widthAnchor.constraint(equalTo: stapel.widthAnchor).isActive = true
        }

        // Verhaeltnis wie unter Windows: Protokoll 2 Teile, Terminal 1,3 Teile.
        protokollKonsole.heightAnchor.constraint(greaterThanOrEqualToConstant: 70).isActive = true
        terminalKonsole.heightAnchor.constraint(greaterThanOrEqualToConstant: 60).isActive = true
        protokollKonsole.heightAnchor.constraint(equalTo: terminalKonsole.heightAnchor,
                                                 multiplier: 2.0 / 1.3).isActive = true
    }

    private func fusszeileBauen() -> NSView {
        let protokolle = Schalter(.leise, "Protokolle")
        protokolle.target = self
        protokolle.action = #selector(protokolleGeklickt)

        diagnoseSchalter.target = self
        diagnoseSchalter.action = #selector(diagnoseGeklickt)
        diagnoseSchalter.toolTip = "Protokolle, Diagnose und Verlauf der letzten 14 Tage maskiert als ZIP nach ~/Downloads."

        let auffrischen = Schalter(.leise, "Auffrischen")
        auffrischen.target = self
        auffrischen.action = #selector(auffrischenGeklickt)

        // Wie weit die Sammelpruefung bzw. der Sammellauf ist: x von n Programmen fertig.
        fussBalken.style = .bar
        fussBalken.isIndeterminate = false
        fussBalken.controlSize = .small
        fussBalken.translatesAutoresizingMaskIntoConstraints = false
        fussBalken.widthAnchor.constraint(equalToConstant: 110).isActive = true
        fussBalken.isHidden = true

        // Der Status bekommt die volle Breite (sonst blieb er auf der schmalen Breite seines ersten
        // Layouts haengen, brach Wort fuer Wort um und schob das Fenster unten aus dem Bildschirm) und
        // hoechstens drei Zeilen; der volle Text steht im Tooltip. Senkrecht gibt er nach, statt das
        // Fenster hoeher als den Bildschirm zu machen.
        for (feld, zeilen) in [(fussStatus, 3), (fussDetail, 4)] {
            feld.maximumNumberOfLines = zeilen
            feld.lineBreakMode = .byWordWrapping
            feld.cell?.truncatesLastVisibleLine = true
            feld.setContentCompressionResistancePriority(.init(rawValue: 250), for: .vertical)
        }

        let schalterReihe = NSStackView(views: [fussBalken, UI.platzhalter(), protokolle, diagnoseSchalter, auffrischen, versionsAnzeige])
        schalterReihe.orientation = .horizontal
        schalterReihe.alignment = .centerY
        schalterReihe.spacing = 10
        schalterReihe.translatesAutoresizingMaskIntoConstraints = false

        // Drei Zeilen: der Status vollstaendig, darunter was gerade laeuft, darunter die Schalter.
        let stapel = NSStackView(views: [fussStatus, fussDetail, schalterReihe])
        stapel.orientation = .vertical
        stapel.alignment = .leading
        stapel.spacing = 6
        stapel.detachesHiddenViews = true
        stapel.translatesAutoresizingMaskIntoConstraints = false
        for zeile in [fussStatus, fussDetail, schalterReihe] {
            zeile.widthAnchor.constraint(equalTo: stapel.widthAnchor).isActive = true
        }
        fussDetail.isHidden = true

        let rahmen = FlaechenView(ton: .tief, eckenRadius: 11)
        rahmen.addSubview(stapel)
        NSLayoutConstraint.activate([
            stapel.leadingAnchor.constraint(equalTo: rahmen.leadingAnchor, constant: 16),
            stapel.trailingAnchor.constraint(equalTo: rahmen.trailingAnchor, constant: -16),
            stapel.topAnchor.constraint(equalTo: rahmen.topAnchor, constant: 10),
            stapel.bottomAnchor.constraint(equalTo: rahmen.bottomAnchor, constant: -10)
        ])
        return rahmen
    }

    // MARK: - Karten

    private func kartenNeuBauen() {
        for ansicht in listenStapel.arrangedSubviews {
            listenStapel.removeArrangedSubview(ansicht)
            ansicht.removeFromSuperview()
        }
        karten.removeAll()

        for gruppe in modell.gruppen {
            let ueberschrift = UI.beschriftung(gruppe.name, groesse: 11.5, gewicht: .semibold, rolle: .leise)
            let huelle = NSView()
            huelle.translatesAutoresizingMaskIntoConstraints = false
            huelle.addSubview(ueberschrift)
            NSLayoutConstraint.activate([
                ueberschrift.leadingAnchor.constraint(equalTo: huelle.leadingAnchor, constant: 4),
                ueberschrift.trailingAnchor.constraint(lessThanOrEqualTo: huelle.trailingAnchor),
                ueberschrift.topAnchor.constraint(equalTo: huelle.topAnchor, constant: 8),
                ueberschrift.bottomAnchor.constraint(equalTo: huelle.bottomAnchor)
            ])
            listenStapel.addArrangedSubview(huelle)
            huelle.widthAnchor.constraint(equalTo: listenStapel.widthAnchor).isActive = true

            for programm in gruppe.programme {
                let karte = ProgrammKarteView(modell: programm)
                karte.beiKlick = { [weak self] in
                    self?.modell.ausgewaehltesProgramm = programm
                }
                listenStapel.addArrangedSubview(karte)
                karte.widthAnchor.constraint(equalTo: listenStapel.widthAnchor).isActive = true
                karten.append(karte)
            }
        }
        aktualisieren()
    }

    // MARK: - Anzeige nachziehen

    private func aktualisieren() {
        zusammenfassungPlakette.text = modell.updateZusammenfassung
        fussStatus.stringValue = modell.kopfStatus
        fussStatus.toolTip = modell.kopfStatus
        fussDetail.stringValue = modell.laufDetail
        fussDetail.isHidden = modell.laufDetail.istLeer
        fussBalken.isHidden = !modell.laeuftSammelvorgang
        fussBalken.maxValue = Double(modell.fortschrittGesamt)
        fussBalken.doubleValue = Double(modell.fortschrittFertig)
        versionsAnzeige.stringValue = modell.anwendungsVersion

        let frei = !modell.laeuftSammelvorgang && modell.sammelMoeglich
        allePruefenSchalter.isEnabled = frei
        cliUpdatesSchalter.isHidden = !modell.hatCliUpdates
        cliUpdatesSchalter.isEnabled = frei
        cliUpdatesSchalter.titel = "⚠  " + modell.cliText
        sichereUpdatesSchalter.isEnabled = frei
        sichereUpdatesSchalter.titel = modell.sicherText
        sichereUpdatesSchalter.auspraegung = modell.hatSichereUpdates ? .haupt : .zweit
        sichereUpdatesSchalter.erledigt = modell.allesAktuell
        dauerSchalter.istAn = modell.nachDauerSortieren
        neuLadenSchalter.isEnabled = modell.sammelMoeglich
        diagnoseSchalter.isEnabled = !modell.exportLaeuft

        if let fehler = modell.katalogFehler, !fehler.istLeer {
            katalogFehlerBand.isHidden = false
            katalogFehlerText.isHidden = false
            fehlerBandZu.isActive = false
            fehlerBandOben.constant = 10
            katalogFehlerText.stringValue = fehler
            katalogFehlerText.festeFarbe = Darstellung.satz.fehlerText
        } else {
            katalogFehlerBand.isHidden = true
            katalogFehlerText.isHidden = true
            fehlerBandZu.isActive = true
            fehlerBandOben.constant = 0
        }

        let gewaehlt = modell.ausgewaehltesProgramm
        gewaehltesProgramm.stringValue = gewaehlt?.name ?? ""
        statusText.stringValue = gewaehlt?.statusText ?? ""
        statusVersion.stringValue = gewaehlt?.versionsText ?? ""
        protokollKonsole.text = gewaehlt?.protokoll ?? ""

        terminalKonsole.text = modell.terminalAusgabe
        befehlsZeile.isEnabled = !modell.terminalLaeuft
        ausfuehrenSchalter.isEnabled = !modell.terminalLaeuft

        for karte in karten { karte.aktualisieren() }
        markierungAnwenden()
        ueberlaufPruefenVormerken()
    }

    private func markierungAnwenden() {
        // Die Karte selbst kennt ihr Modell; welche ausgewaehlt ist, weiss nur das Fenster.
        var index = 0
        for gruppe in modell.gruppen {
            for programm in gruppe.programme {
                guard index < karten.count else { break }
                karten[index].istAusgewaehlt = (programm === modell.ausgewaehltesProgramm)
                index += 1
            }
        }
    }

    @objc private func designAnwenden() {
        let satz = Darstellung.satz
        hintergrund.layer?.backgroundColor = satz.hintergrund.cgColor
        logoKachel?.layer?.backgroundColor = satz.akzentVerlaufOben.cgColor
        logoPfeil?.festeFarbe = satz.aufAkzent
        designSchalter.istAn = modell.hellModus

        // Die Systemtitelleiste zieht mit -- Gegenstueck zu DwmSetWindowAttribute unter Windows.
        window?.appearance = NSAppearance(named: Darstellung.istHell ? .aqua : .darkAqua)
    }

    // MARK: - Fenster immer im sichtbaren Bereich

    /// Zweite Schicht neben `UI.nieBreiterAlsFenster`: Groessengrenzen am Bildschirm ausrichten,
    /// auf dem das Fenster WIRKLICH steht (nicht `NSScreen.main`), und ein Fenster, das groesser
    /// geworden ist als dessen sichtbarer Bereich, wieder hineinziehen -- nach einem Monitorwechsel
    /// oder einer geaenderten Aufloesung. macOS selbst korrigiert nur die Hoehe, nie die Breite.
    /// Die Position bleibt sonst unangetastet: ein bewusst ueber zwei Schirme gestelltes Fenster
    /// springt nicht zurueck, und waehrend die Maus zieht, werden nur die Grenzen nachgezogen.
    private var passtGeradeAn = false
    private var ueberlaufVorgemerkt = false
    private var breiteGemeldet = false

    private func anBildschirmAnpassen() {
        guard !passtGeradeAn, let fenster = window,
              let sichtbar = (fenster.screen ?? NSScreen.main)?.visibleFrame else { return }
        passtGeradeAn = true
        defer { passtGeradeAn = false }

        fenster.maxSize = NSSize(width: sichtbar.width, height: sichtbar.height)
        fenster.minSize = NSSize(width: min(880, sichtbar.width), height: min(540, sichtbar.height))
        guard NSEvent.pressedMouseButtons == 0, !fenster.inLiveResize else { return }

        let vorher = fenster.frame
        guard vorher.width > sichtbar.width + 0.5 || vorher.height > sichtbar.height + 0.5 else { return }
        var rahmen = vorher
        rahmen.size.width = min(rahmen.width, sichtbar.width)
        rahmen.size.height = min(rahmen.height, sichtbar.height)
        rahmen.origin.x = min(max(rahmen.minX, sichtbar.minX), sichtbar.maxX - rahmen.width)
        rahmen.origin.y = min(max(rahmen.minY, sichtbar.minY), sichtbar.maxY - rahmen.height)
        fenster.setFrame(rahmen, display: true)
        Diagnose.ereignis(.warnung, "fenster", "an-bildschirm-angepasst",
                          "Fenster war größer als der sichtbare Bereich und wurde hineingezogen.",
                          ["vorher": NSStringFromRect(vorher), "nachher": NSStringFromRect(rahmen)])
    }

    /// Fruehwarnung: ragt nach dem Layout ein Bereich ueber den Fensterinhalt hinaus, hat irgendwo
    /// wieder etwas eine waagerechte Festigkeit ueber 500. Gemessen an den echten Rahmen, nicht an
    /// `fittingSize` (das drueckt nur mit Prioritaet 50 und meldete deshalb fast immer).
    private func ueberlaufPruefenVormerken() {
        guard !breiteGemeldet, !ueberlaufVorgemerkt else { return }
        ueberlaufVorgemerkt = true
        DispatchQueue.main.async { [weak self] in
            guard let self else { return }
            self.ueberlaufVorgemerkt = false
            guard !self.breiteGemeldet, let inhalt = self.window?.contentView else { return }
            inhalt.layoutSubtreeIfNeeded()
            let rechterRand = inhalt.subviews.filter { !$0.isHidden }.map(\.frame.maxX).max() ?? 0
            guard rechterRand > inhalt.bounds.maxX + 1 else { return }
            self.breiteGemeldet = true
            Diagnose.ereignis(.warnung, "fenster", "inhalt-zu-breit",
                              "Inhalt ragt rechts aus dem Fenster.",
                              ["inhalt": rechterRand, "fenster": inhalt.bounds.width])
        }
    }

    func windowDidChangeScreen(_ notification: Notification) { anBildschirmAnpassen() }
    func windowDidEndLiveResize(_ notification: Notification) { ueberlaufPruefenVormerken() }

    /// Erst im naechsten Durchlauf: macOS setzt das Fenster nach dem Abstecken eines Monitors
    /// selbst um, danach stimmt `window.screen`.
    @objc private func bildschirmeGeaendert() {
        DispatchQueue.main.async { [weak self] in
            self?.anBildschirmAnpassen()
            self?.ueberlaufPruefenVormerken()
        }
    }

    // MARK: - Aktionen

    @objc private func allePruefenGeklickt() { Task { await modell.allePruefen() } }
    @objc private func sichereUpdatesGeklickt() { Task { await modell.sichereAktualisieren() } }
    @objc private func cliUpdatesGeklickt() { Task { await modell.cliAktualisieren() } }
    @objc private func neuLadenGeklickt() { modell.katalogNeuLaden() }
    @objc private func katalogOeffnenGeklickt() { modell.katalogOeffnen() }
    @objc private func protokolleGeklickt() { modell.protokolleOeffnen() }
    @objc private func diagnoseGeklickt() { Task { await modell.diagnoseExportieren() } }
    @objc private func auffrischenGeklickt() { modell.zustaendeAuffrischen() }
    @objc private func terminalLeerenGeklickt() { modell.terminalLeeren() }
    @objc private func terminalFensterGeklickt() { modell.terminalFensterOeffnen() }

    @objc private func befehlAusfuehrenGeklickt() {
        let befehl = befehlsZeile.stringValue
        guard !befehl.istLeer else { return }
        befehlsZeile.stringValue = ""
        Task { await modell.befehlAusfuehren(befehl) }
    }

    /// Laeuft, sobald das Fenster steht: die Liste ist erst mit Zustaenden aussagekraeftig.
    func erstePruefungStarten() {
        Task { await modell.erstePruefung() }
    }
}
