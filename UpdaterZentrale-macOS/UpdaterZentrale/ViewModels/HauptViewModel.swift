import AppKit

/// Fensterlogik. 1:1-Port von ViewModels/HauptViewModel.cs.
///
/// Nicht portiert (kein macOS-Gegenstueck, siehe PORTING.md): `ImmerAlsAdmin`,
/// `AlsAdminNeuStarten`, `IstErhoeht`, `RechteText` -- macOS kennt keine Rechte-Erhoehung fuer
/// Programme, und Homebrew verweigert den Betrieb als root sogar ausdruecklich.
@MainActor
final class HauptViewModel {

    private let einstellungen = Einstellungen.laden()

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

        let ausgabe = await Terminal.ausfuehren(befehl, arbeitsverzeichnis: Terminal.arbeitsverzeichnis)
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
        let berichte = Protokollierung.letzteBerichte()

        for eintrag in katalog.programme {
            let dienst = aktualisierer[eintrag.art.lowercased()]
            let karte = ProgrammViewModel(eintrag: eintrag, aktualisierer: dienst, einstellungen: einstellungen)
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

    func katalogNeuLaden() {
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

    /// Oeffnet den Protokollordner; dort steht jeder Lauf, Tag fuer Tag.
    func protokolleOeffnen() { Protokollierung.ordnerOeffnen() }

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

        // Ist ein zuvor abgelegtes Update inzwischen angekommen -- oder haengt es noch?
        for programm in programme { await programm.ausstehendesPruefen() }

        await allePruefen()
    }

    func allePruefen() async {
        guard !laeuftSammelvorgang else { return }
        laeuftSammelvorgang = true
        defer { laeuftSammelvorgang = false }

        // Erst den Homebrew-Katalog auffrischen, DANN pruefen. Ohne das liest `brew info` den
        // lokalen Tap, und der kann Tage alt sein -- die Pruefung meldete dann "Aktuell", obwohl
        // ein Cask laengst neuer ist. `winget list` unter Windows hat dieses Problem nicht, weil es
        // seine Quelle selbst aktuell haelt.
        if programme.contains(where: { $0.eintrag.art == "brew" }) {
            kopfStatus = "Homebrew-Katalog wird aufgefrischt …"
            _ = await Kommandozeile.ausfuehren(Pfade.brew, ["update", "--quiet"], zeitlimit: 300)
        }

        // Bewusst nacheinander: brew greift ohnehin seriell auf seinen Katalog zu, und ein
        // paralleler Schwung macht das Protokoll unlesbar.
        let gesamt = programme.count
        for (i, programm) in programme.enumerated() {
            kopfStatus = "Prüft \(programm.name) (\(i + 1) von \(gesamt)) …"
            await programm.pruefen()
        }
        kopfStatus = "Prüfung abgeschlossen – " + updateZusammenfassung + "."
    }

    func alleAktualisieren() async {
        guard !laeuftSammelvorgang else { return }

        let offen = programme.filter(\.hatUpdate)
        guard !offen.isEmpty else {
            Dialoge.hinweis("Es ist kein Update offen. Prüfe zuerst, oder aktualisiere einzelne Programme gezielt.")
            return
        }

        let liste = offen.map { "  • " + $0.name }.joined(separator: "\n")
        guard Dialoge.fragen("Diese Programme werden jetzt aktualisiert:\n\n\(liste)"
                             + "\n\nBei laufenden Programmen wird vorher nachgefragt.",
                             "Alle Updates installieren?") else { return }

        laeuftSammelvorgang = true
        defer { laeuftSammelvorgang = false }

        for (i, programm) in offen.enumerated() {
            kopfStatus = "Aktualisiert \(programm.name) (\(i + 1) von \(offen.count)) …"
            ausgewaehltesProgramm = programm
            await programm.aktualisieren()
        }
        kopfStatus = "Alle Updates sind durchgelaufen."
    }
}
