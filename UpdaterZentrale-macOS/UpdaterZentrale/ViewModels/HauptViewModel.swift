import AppKit

/// Fensterlogik. 1:1-Port von ViewModels/HauptViewModel.cs.
///
/// Nicht portiert (kein macOS-Gegenstueck, siehe PORTING.md): `ImmerAlsAdmin`,
/// `AlsAdminNeuStarten`, `IstErhoeht`, `RechteText` -- macOS kennt keine Rechte-Erhoehung fuer
/// Programme, und Homebrew verweigert den Betrieb als root sogar ausdruecklich.
@MainActor
final class HauptViewModel {

    private let einstellungen = Einstellungen.laden()

    /// App-weit: hoechstens ein externer Vorgang (Pruefung, Update, Sammellauf) zur Zeit.
    private let koordination = Laufkoordination()

    /// Alle bekannten Mechanismen, ueber das Katalogfeld "art" erreichbar. Ein neues Programm ist
    /// ein JSON-Eintrag; nur ein wirklich neuer Mechanismus braucht hier einen Eintrag.
    private let aktualisierer: [String: Aktualisierer] = [
        "brew": BrewAktualisierer(),
        "cli": CliAktualisierer(),
        "reposkript": RepoSkriptAktualisierer()
    ]

    /// Wird nach jeder Aenderung gefeuert, die das Fenster neu zeichnen muss.
    var beiAenderung: (() -> Void)?
    /// Wird gefeuert, wenn die Kartenliste ausgetauscht wurde (Katalog neu geladen).
    var beiListenWechsel: (() -> Void)?

    private(set) var programme: [ProgrammViewModel] = []

    var ausgewaehltesProgramm: ProgrammViewModel? {
        didSet { beiAenderung?() }
    }

    private(set) var kopfStatus = "Bereit." { didSet { beiAenderung?() } }
    private(set) var laeuftSammelvorgang = false { didSet { beiAenderung?() } }
    private(set) var katalogFehler: String? { didSet { beiAenderung?() } }

    init() {
        hellModusIntern = einstellungen.hellModus
        Darstellung.anwenden(hellModusIntern)
        koordination.beiAenderung.append { [weak self] in
            guard let self else { return }
            for programm in self.programme { programm.sperreGeaendert() }
            self.beiAenderung?()
        }
        katalogLaden()
    }

    // MARK: - Gruppierung

    /// Die Karten in der Reihenfolge des Katalogs, nach Gruppe gebuendelt.
    /// Ersetzt die CollectionViewSource-Gruppierung aus WPF.
    var gruppen: [(name: String, programme: [ProgrammViewModel])] {
        var reihenfolge: [String] = []
        var inhalt: [String: [ProgrammViewModel]] = [:]
        for programm in programme {
            if inhalt[programm.gruppe] == nil {
                inhalt[programm.gruppe] = []
                reihenfolge.append(programm.gruppe)
            }
            inhalt[programm.gruppe]?.append(programm)
        }
        return reihenfolge.map { ($0, inhalt[$0] ?? []) }
    }

    // MARK: - Darstellung

    private var hellModusIntern: Bool

    /// Hell oder dunkel; die Wahl wird in settings.json gemerkt.
    var hellModus: Bool {
        get { hellModusIntern }
        set {
            guard hellModusIntern != newValue else { return }
            hellModusIntern = newValue

            Darstellung.anwenden(newValue)
            einstellungen.hellModus = newValue
            einstellungen.speichern()

            for programm in programme { programm.darstellungAuffrischen() }
            beiAenderung?()
        }
    }

    var darstellungsText: String { hellModus ? "Heller Modus" : "Dunkler Modus" }

    // MARK: - Terminal

    private(set) var terminalAusgabe = "" { didSet { beiAenderung?() } }
    private(set) var terminalLaeuft = false { didSet { beiAenderung?() } }

    /// Kurz gehalten: daneben stehen noch zwei Schaltflaechen, und die rechte Spalte ist schmal.
    var terminalKopf: String { "Terminal · zsh" }

    func befehlAusfuehren(_ eingabe: String) async {
        let befehl = eingabe.trimmed
        guard !befehl.isEmpty, !terminalLaeuft else { return }

        terminalLaeuft = true
        terminalAusgabe += (terminalAusgabe.isEmpty ? "" : "\n\n") + "$ " + befehl + "\n"

        let vorgang = Diagnose.vorgangBeginnen("terminal", nil, "Terminalbefehl: " + String(befehl.prefix(300)))
        let ausgabe = await Terminal.ausfuehren(befehl, arbeitsverzeichnis: Terminal.arbeitsverzeichnis)
        vorgang.beenden("abgeschlossen")
        terminalAusgabe += ausgabe
        terminalLaeuft = false
    }

    func terminalLeeren() { terminalAusgabe = "" }

    func terminalFensterOeffnen() {
        if !Terminal.fensterOeffnen() {
            Dialoge.hinweis("Es ließ sich kein Terminalfenster öffnen.")
        }
    }

    // MARK: - Kopf- und Fusszeile

    var anwendungsVersion: String {
        let info = Bundle.main.infoDictionary
        let version = (info?["CFBundleShortVersionString"] as? String) ?? "?"
        let stempel = info?["BuildTimestamp"] as? String
        let text = "Version " + version
        return stempel == nil ? text : text + "  ·  Build " + stempel!
    }

    var anzahlUpdates: Int { programme.filter(\.hatUpdate).count }

    var updateZusammenfassung: String {
        switch anzahlUpdates {
        case 0: return "Keine Updates offen"
        case 1: return "1 Update verfügbar"
        default: return "\(anzahlUpdates) Updates verfügbar"
        }
    }

    // MARK: - Katalog

    private func katalogLaden() {
        programme.removeAll()

        let (katalog, fehler) = Katalogdienst.laden()
        katalogFehler = fehler
        Diagnose.ereignis(fehler == nil ? .info : .fehler, "katalog", "katalog.geladen",
                          fehler ?? "\(katalog.programme.count) Programme geladen.",
                          ["programme": katalog.programme.map { $0.id + ":" + $0.art }.joined(separator: ",")])
        let berichte = Protokollierung.letzteBerichte()

        for eintrag in katalog.programme {
            let dienst = aktualisierer[eintrag.art.lowercased()]
            let karte = ProgrammViewModel(eintrag: eintrag, aktualisierer: dienst, einstellungen: einstellungen,
                                          koordination: koordination)
            karte.beiAenderung = { [weak self] in self?.beiAenderung?() }
            karte.beiMeldung = { text in Dialoge.hinweis(text) }

            // Der Verlauf ueberlebt Neustarts -- jede Karte kann zeigen, wie ihr letzter Lauf ging.
            if let bericht = berichte[eintrag.id] { karte.letzterBericht = bericht }

            programme.append(karte)
        }

        ausgewaehltesProgramm = programme.first
        beiListenWechsel?()
        beiAenderung?()
    }

    /// Sammelschalter und "Neu laden" gehen nur, solange gar nichts laeuft.
    var sammelMoeglich: Bool { !koordination.belegt }

    func katalogNeuLaden() {
        // Neu laden wirft die Karten weg. Eine Karte mitten im Update verschwaende samt Ergebnis,
        // und von der frischen Karte waere ein zweiter Lauf desselben Installers moeglich.
        if koordination.belegt || laeuftSammelvorgang || programme.contains(where: \.istBeschaeftigt) {
            kopfStatus = "Neu laden geht erst, wenn alle laufenden Prüfungen und Updates fertig sind."
            sammelAbgewiesen("katalog-neu-laden")
            return
        }
        katalogLaden()
        kopfStatus = "Katalog neu geladen – \(programme.count) Programme."
    }

    /// Oeffnet programs.json, damit ein neues Programm ohne Eingriff in die App dazukommen kann.
    func katalogOeffnen() {
        let datei = Pfade.katalogDatei
        guard FileManager.default.fileExists(atPath: datei) else {
            Dialoge.hinweis("Der Katalog wurde nicht gefunden: \(datei)")
            return
        }
        NSWorkspace.shared.open(URL(fileURLWithPath: datei))
    }

    /// Oeffnet den Protokollordner; dort stehen Tagesprotokolle, Diagnose und Verlauf.
    func protokolleOeffnen() { Protokollierung.ordnerOeffnen() }

    private(set) var exportLaeuft = false { didSet { beiAenderung?() } }

    /// Baut das maskierte Diagnose-ZIP abseits der Oberflaeche und zeigt es im Finder.
    func diagnoseExportieren() async {
        guard !exportLaeuft else { return }
        exportLaeuft = true
        defer { exportLaeuft = false }
        let vorgang = Diagnose.vorgangBeginnen("diagnose-export", nil, "Diagnosepaket erstellen")
        let katalog = programme.map(\.eintrag)
        let ergebnis: Result<String, Error> = await Task.detached { Result { try Diagnose.exportieren(katalog: katalog) } }.value
        switch ergebnis {
        case .success(let pfad):
            vorgang.beenden("erstellt", pfad)
            kopfStatus = "Diagnosepaket erstellt: " + pfad
            NSWorkspace.shared.activateFileViewerSelecting([URL(fileURLWithPath: pfad)])
        case .failure(let fehler):
            vorgang.beenden("Ausnahme", fehler.localizedDescription, .fehler)
            Dialoge.hinweis("Das Diagnosepaket ließ sich nicht erstellen:\n" + fehler.localizedDescription)
        }
    }

    private func sammelAbgewiesen(_ art: String) {
        Diagnose.ereignis(.warnung, "sammel", "start.abgewiesen", "\(art) abgewiesen: es läuft bereits ein Vorgang.")
    }

    func zustaendeAuffrischen() {
        Systemdienst.zwischenspeicherLeeren()
        for programm in programme { programm.zustandAktualisieren() }
        kopfStatus = "Laufende Programme und Autostart wurden neu eingelesen."
    }

    // MARK: - Sammelvorgaenge

    /// Laeuft einmal, nachdem das Fenster steht -- so ist die Liste ohne ersten Klick aussagekraeftig,
    /// und die Update-Schaltflaechen zeigen ueberall dort "Aktuell", wo nichts ansteht.
    func erstePruefung() async {
        try? await Task.sleep(nanoseconds: 400_000_000)

        // Die Anfangspruefung fragt brew, git und die Werkzeuge ab wie ein Sammellauf und haelt
        // denselben exklusiven Besitz -- ein Klick waehrenddessen wartet, statt daneben zu laufen.
        guard let besitz = koordination.sammelBeginnen() else {
            kopfStatus = ProgrammViewModel.belegtText
            sammelAbgewiesen("startpruefung")
            return
        }
        defer { besitz.freigeben() }
        let vorgang = Diagnose.vorgangBeginnen("startpruefung", nil, "Anfangsprüfung aller Programme")
        laeuftSammelvorgang = true
        defer { laeuftSammelvorgang = false }

        // Ist ein zuvor abgelegtes Update inzwischen angekommen -- oder haengt es noch?
        for programm in programme { await programm.ausstehendesPruefen() }

        await allePruefenMitBesitz(besitz)
        vorgang.beenden("abgeschlossen", kopfStatus)
    }

    func allePruefen() async {
        guard let besitz = koordination.sammelBeginnen() else {
            kopfStatus = "Alle prüfen geht erst, wenn der laufende Vorgang fertig ist."
            sammelAbgewiesen("sammelpruefung")
            return
        }
        defer { besitz.freigeben() }
        laeuftSammelvorgang = true
        defer { laeuftSammelvorgang = false }
        let vorgang = Diagnose.vorgangBeginnen("sammelpruefung", nil, "Alle prüfen")
        await allePruefenMitBesitz(besitz)
        vorgang.beenden("abgeschlossen", kopfStatus)
    }

    private func allePruefenMitBesitz(_ besitz: Laufbesitz) async {

        // Erst den Homebrew-Katalog auffrischen, DANN pruefen. Ohne das liest `brew info` den
        // lokalen Tap, und der kann Tage alt sein -- die Pruefung meldete dann "Aktuell", obwohl
        // ein Cask laengst neuer ist. `winget list` unter Windows hat dieses Problem nicht, weil es
        // seine Quelle selbst aktuell haelt.
        if programme.contains(where: { $0.eintrag.art == "brew" }) {
            kopfStatus = "Homebrew-Katalog wird aufgefrischt …"
            _ = await Kommandozeile.ausfuehren(Pfade.brew, ["update", "--quiet"], zeitlimit: 300)
        }

        // Bewusst nacheinander: brew greift ohnehin seriell auf seinen Katalog zu, und ein
        // paralleler Schwung macht das Protokoll unlesbar. Eine Momentaufnahme: die Liste darf
        // sich unter der Schleife nicht verschieben.
        let liste = programme
        let gesamt = liste.count
        for (i, programm) in liste.enumerated() {
            kopfStatus = "Prüft \(programm.name) (\(i + 1) von \(gesamt)) …"
            await programm.pruefen(sammel: besitz)
        }
        kopfStatus = "Prüfung abgeschlossen – " + updateZusammenfassung + "."
    }

    func alleAktualisieren() async {
        if koordination.belegt {
            kopfStatus = "Alle Updates gehen erst, wenn der laufende Vorgang fertig ist."
            sammelAbgewiesen("sammelupdate")
            return
        }

        let offen = programme.filter(\.hatUpdate)
        guard !offen.isEmpty else {
            Dialoge.hinweis("Es ist kein Update offen. Prüfe zuerst, oder aktualisiere einzelne Programme gezielt.")
            return
        }

        let liste = offen.map { "  • " + $0.name }.joined(separator: "\n")
        guard Dialoge.fragen("Diese Programme werden jetzt aktualisiert:\n\n\(liste)"
                             + "\n\nBei laufenden Programmen wird vorher nachgefragt.",
                             "Alle Updates installieren?") else { return }

        // Erst jetzt erworben: die Frage oben ist modal, und was inzwischen startete, gewinnt.
        guard let besitz = koordination.sammelBeginnen() else {
            kopfStatus = "Alle Updates gehen erst, wenn der laufende Vorgang fertig ist."
            sammelAbgewiesen("sammelupdate")
            return
        }
        defer { besitz.freigeben() }
        laeuftSammelvorgang = true
        defer { laeuftSammelvorgang = false }
        let vorgang = Diagnose.vorgangBeginnen("sammelupdate", nil, "Alle Updates: " + offen.map(\.eintrag.id).joined(separator: ", "))

        for (i, programm) in offen.enumerated() {
            kopfStatus = "Aktualisiert \(programm.name) (\(i + 1) von \(offen.count)) …"
            ausgewaehltesProgramm = programm
            await programm.aktualisieren(sammel: besitz)
        }
        kopfStatus = "Alle Updates sind durchgelaufen."
        vorgang.beenden("abgeschlossen", kopfStatus)
    }
}
