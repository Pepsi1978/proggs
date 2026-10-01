import AppKit

/// Eine Karte in der Liste: der Katalog-Eintrag plus alles, was die Oberflaeche darueber zeigt.
/// 1:1-Port von ViewModels/ProgrammViewModel.cs.
///
/// WPF meldete Aenderungen ueber INotifyPropertyChanged; AppKit hat das nicht. An seine Stelle
/// tritt `beiAenderung` -- die Kartenansicht traegt sich dort ein und zeichnet sich neu.
///
/// Nicht portiert (kein macOS-Gegenstueck, siehe PORTING.md): `AlsAdministrator`,
/// `AutostartUmstellen`, `AdminWarnung`, `KonsolenAdminWarnung`, `AutostartAlsAufgabe`.
@MainActor
final class ProgrammViewModel {

    private let aktualisierer: Aktualisierer?
    private let einstellungen: Einstellungen
    private let koordination: Laufkoordination
    private var protokollPuffer = ""

    static let belegtText = "Es läuft bereits ein anderer Vorgang – erst danach ist das möglich."

    /// Was die Karte fuer den Detailbereich im Speicher haelt -- begrenzt, der neueste Text gewinnt.
    private static let maxProtokollZeichen = 200_000

    /// Wird nach jeder sichtbaren Aenderung aufgerufen.
    var beiAenderung: (() -> Void)?
    /// Wird fuer Meldungen aufgerufen, die ein Hinweisfenster verdienen.
    var beiMeldung: ((String) -> Void)?

    let eintrag: ProgrammEintrag

    init(eintrag: ProgrammEintrag, aktualisierer: Aktualisierer?, einstellungen: Einstellungen,
         koordination: Laufkoordination) {
        self.eintrag = eintrag
        self.aktualisierer = aktualisierer
        self.einstellungen = einstellungen
        self.koordination = koordination

        if aktualisierer == nil {
            zustand = .fehler
            statusText = "Unbekannte Update-Art: \(eintrag.art)"
        }
        zustandAktualisieren()
    }

    // MARK: - Unveraenderliche Anzeige

    var name: String { eintrag.name }
    var gruppe: String { eintrag.gruppe }
    var beschreibung: String { eintrag.beschreibung }
    var akzent: String { eintrag.akzent }
    var hinweis: String? { eintrag.hinweis }
    var hatHinweis: Bool { !(eintrag.hinweis?.istLeer ?? true) }

    /// Bis zu zwei Anfangsbuchstaben fuer die Kachel; Klammern und Zeichen werden uebersprungen.
    var kuerzel: String {
        let teile = eintrag.name
            .split(whereSeparator: { $0 == " " || $0 == "-" || $0 == "/" })
            .filter { $0.first?.isLetter == true || $0.first?.isNumber == true }
            .prefix(2)
        return String(teile.compactMap { $0.first?.uppercased().first })
    }

    var artText: String {
        switch eintrag.art {
        case "brew": return "Homebrew"
        case "cli": return "Selbst-Update"
        case "reposkript": return "Eigenes Skript"
        default: return eintrag.art
        }
    }

    /// Ein App-Buendel laeuft ueber den Finder-Weg, ein Werkzeug ueber seinen Pfad.
    var kannStarten: Bool { eintrag.istAppBuendel || !eintrag.exePfadWirksam.istLeer }

    // MARK: - Veraenderlicher Zustand

    private(set) var zustand: UpdateZustand = .unbekannt {
        didSet { melden() }
    }
    private(set) var statusText = "Noch nicht geprüft" { didSet { melden() } }
    private(set) var installierteVersion = "" { didSet { melden() } }
    private(set) var verfuegbareVersion = "" { didSet { melden() } }
    private(set) var istBeschaeftigt = false { didSet { melden() } }
    private(set) var laeuft = false { didSet { melden() } }
    private(set) var imAutostart = false { didSet { melden() } }

    var letzterBericht: UpdateBericht? { didSet { melden() } }

    var protokoll: String { protokollPuffer }

    var hatUpdate: Bool { zustand == .updateVerfuegbar }

    /// Die Update-Schaltflaeche laedt nur dann zum Klick ein, wenn wirklich eine neuere Version
    /// vorliegt. Hat eine Pruefung nichts gefunden, steht dort "Aktuell" und sie ist abgeschaltet,
    /// statt Arbeit vorzutaeuschen.
    var aktionMoeglich: Bool {
        aktualisierer != nil && !istBeschaeftigt && !koordination.belegt && zustand != .aktuell && !uebernahmeOffen
    }

    var aktionsText: String {
        if uebernahmeOffen { return "Neustart nötig" }
        switch zustand {
        case .aktuell: return "Aktuell"
        case .updateVerfuegbar: return "Aktualisieren"
        case .pruefe: return "Prüft …"
        case .nichtInstalliert: return "Nicht installiert"
        default: return "Aktualisieren"
        }
    }

    /// Nur ein echtes Update bekommt die Akzentfarbe; alles andere bleibt ruhig.
    var aktionBetont: Bool { zustand == .updateVerfuegbar && !uebernahmeOffen }

    var kannPruefen: Bool { aktualisierer != nil && !istBeschaeftigt && !koordination.belegt }

    var versionsText: String {
        if installierteVersion.istLeer { return "–" }
        if verfuegbareVersion.istLeer || verfuegbareVersion == installierteVersion {
            return installierteVersion
        }
        return installierteVersion + "   →   " + verfuegbareVersion
    }

    var hatBericht: Bool { letzterBericht != nil }
    /// Das rote Band. Es verschwindet erst, wenn es weggeklickt wurde -- und nur fuer genau diesen
    /// Lauf: ein spaeterer Fehler hat einen spaeteren Zeitstempel und wird wieder gezeigt.
    var berichtIstFehler: Bool {
        guard let bericht = letzterBericht, bericht.istFehler else { return false }
        if let marke = einstellungen.fuer(eintrag.id).fehlerQuittiertBis, marke >= bericht.zeit { return false }
        return true
    }
    var berichtKurz: String { letzterBericht?.kurzfassung ?? "" }
    var berichtGrund: String { letzterBericht?.meldung ?? "" }

    /// Das Update ist heruntergeladen und installiert, das Programm hat es nur noch nicht
    /// uebernommen. Ohne diesen Hinweis findet eine neue Pruefung dasselbe Update wieder und es
    /// sieht aus, als waere nichts passiert -- genau so faellt es in der Praxis auf.
    var uebernahmeOffen: Bool { letzterBericht?.ergebnis == .ausstehend }

    var uebernahmeText: String {
        "Das Update ist bereits heruntergeladen und installiert. Es wird aktiv, sobald \(name) "
        + "einmal neu gestartet wurde – bis dahin meldet die Prüfung weiterhin die alte Version."
    }

    /// Wird von der Fensterlogik gerufen, wenn sich die app-weite Sperre aendert.
    func sperreGeaendert() { melden() }

    private func melden() { beiAenderung?() }

    // MARK: - Zustand einlesen

    func zustandAktualisieren() {
        laeuft = Prozessdienst.laeuft(eintrag)
        imAutostart = Systemdienst.imAutostart(eintrag)
    }

    /// Frischt die Darstellung nach einem Hell/Dunkel-Wechsel auf.
    func darstellungAuffrischen() { melden() }

    // MARK: - Ausstehende Uebernahme

    /// Ein abgelegtes Update (Claude Desktop macht das) wird erst beim naechsten Start des
    /// Programms echt. Bei einem spaeteren Start der Updater-Zentrale wird es hier rueckwirkend
    /// bestaetigt -- oder als nie angekommen ausgewiesen, was sonst unbemerkt bliebe.
    func ausstehendesPruefen() async {
        guard let aktualisierer,
              let offen = letzterBericht, offen.ergebnis == .ausstehend else { return }

        // Bestaetigt nur mit erreichtem Ziel UND echter "aktuell"-Pruefung -- eine andere
        // Aenderung des Fingerabdrucks ist kein Beweis, solange ein Update angeboten wird.
        let jetzt = await aktualisierer.fingerabdruck(eintrag)
        let pruefung = await aktualisierer.pruefen(eintrag, Fortschritt { _ in })
        guard let urteil = UpdateKette.stagedUrteil(offen, jetzt: jetzt, pruefung: pruefung, zeitpunkt: Date()) else { return }

        var nachtrag = UpdateBericht()
        nachtrag.zeit = Date()
        nachtrag.programmId = eintrag.id
        nachtrag.name = name
        nachtrag.art = eintrag.art
        nachtrag.ergebnis = urteil.ergebnis
        nachtrag.versionVorher = offen.versionNachher
        nachtrag.versionNachher = jetzt
        nachtrag.befehl = offen.befehl
        nachtrag.meldung = urteil.meldung
        Protokollierung.laufBeenden(&nachtrag)
        letzterBericht = nachtrag
        Diagnose.ereignis(urteil.ergebnis == .erfolgreich ? .info : .warnung, "karte", "ausstehend.urteil",
                          urteil.meldung, ["programm": eintrag.id])
    }

    /// Nimmt das rote Band von der Karte. Gemeldet bleibt der Lauf trotzdem: im Tagesprotokoll und
    /// in verlauf.jsonl steht er unveraendert, und die graue "Zuletzt:"-Zeile nennt ihn weiter.
    func fehlerQuittieren() {
        guard let bericht = letzterBericht, bericht.istFehler else { return }
        einstellungen.fuer(eintrag.id).fehlerQuittiertBis = bericht.zeit
        einstellungen.speichern()
        melden()
    }

    // MARK: - Befehle

    func protokollOeffnen() {
        Protokollierung.dateiOeffnen(letzterBericht?.protokollDatei)
    }

    func starten() {
        if !Prozessdienst.starten(eintrag) {
            beiMeldung?("Konnte nicht gestartet werden: \(eintrag.zielPfad)")
        }
        zustandAktualisieren()
    }

    func pruefen(sammel: Laufbesitz? = nil) async {
        guard let aktualisierer else { return }
        if istBeschaeftigt { doppelt("pruefung"); return }
        guard let eigen = erwerben(sammel, "pruefung") else { return }
        defer { eigen.freigeben() }
        await lauf("pruefung") { fortschritt in
            self.statusText = "Wird geprüft …"
            self.zustand = .pruefe
            return await aktualisierer.pruefen(self.eintrag, fortschritt)
        }
    }

    /// Ein Klick = eine begrenzte Update-Kette (siehe UpdateKette), innerhalb des Besitzes.
    /// Genau ein Protokollkopf und ein -fuss; der Bericht entsteht erst aus dem ENDURTEIL.
    func aktualisieren(sammel: Laufbesitz? = nil) async {
        guard let aktualisierer else { return }
        // Schon beschaeftigt: der Lauf wuerde verworfen -- aber erst, nachdem der Benutzer dem
        // Beenden zugestimmt hat. Deshalb wird der Besitz VOR dieser Frage genommen.
        if istBeschaeftigt { doppelt("update"); return }
        guard let eigen = erwerben(sammel, "update") else { return }
        defer { eigen.freigeben() }

        await lauf("update") { fortschritt in
            let kette = UpdateKette(aktualisierer, self.eintrag, fortschritt) { self.statusText = $0 }
            let (vorher, vorPruefung) = await kette.vorpruefen()
            Protokollierung.laufBeginnen(self.eintrag, befehl: self.befehlsBeschreibung(),
                                         fingerabdruckVorher: vorher)

            var urteil = UpdateKette.bereitsAktuell(vorher, vorPruefung)
            var liefVorher = false

            if urteil == nil {
                liefVorher = Prozessdienst.laeuft(self.eintrag)

                // Installer haengen still, solange das Programm laeuft -- deshalb gehen die Helfer
                // mit herunter, aber erst nach Zustimmung und nur einmal je Kette.
                if self.eintrag.beendenVorUpdate && liefVorher {
                    let laufende = Prozessdienst.laufende(self.eintrag).count
                    let frage = "\(self.name) läuft gerade (\(laufende) Prozess(e) einschließlich Helferprogramme).\n\n"
                              + "Zum Aktualisieren muss das Programm beendet werden."
                              + (self.eintrag.neuStartenNachUpdate ? " Danach wird es automatisch neu gestartet." : "")
                              + "\n\nJetzt beenden und aktualisieren?"

                    var karte = vorPruefung
                    if !Dialoge.fragen(frage, "Programm beenden?") {
                        karte.zustand = .abgebrochen
                        urteil = KettenUrteil(ergebnis: .abgebrochen, meldung: "Abgebrochen – das Programm läuft weiter.",
                                              fingerabdruckVorher: vorher, fingerabdruckNachher: vorher,
                                              fuerKarte: karte, updateAufrufe: 0, updateLiefDurch: false)
                    } else {
                        self.statusText = "Beendet das Programm …"
                        fortschritt.berichte("Beende " + self.eintrag.alleProzesse.joined(separator: ", ")
                                             + (self.eintrag.alleProzesse.isEmpty ? self.name : ""))
                        let beendet = await Prozessdienst.beenden(self.eintrag)
                        if let problem = beendet.problem { fortschritt.berichte(problem) }
                        if !beendet.erfolgreich {
                            karte.zustand = .fehler
                            urteil = KettenUrteil(ergebnis: .fehlgeschlagen,
                                                  meldung: "\(self.name) ließ sich nicht vollständig beenden – das Update wurde nicht gestartet. "
                                                         + (beendet.problem ?? ""),
                                                  fingerabdruckVorher: vorher, fingerabdruckNachher: vorher,
                                                  fuerKarte: karte, updateAufrufe: 0, updateLiefDurch: false)
                        }
                    }
                }

                if urteil == nil { urteil = await kette.ausfuehren(vorher, vorPruefung) }
            }
            let ende = urteil!

            // Einmal neu starten, nach der ganzen Kette -- nie zwischen Durchlaeufen, wo ein
            // laufendes Ziel den naechsten Installer blockieren wuerde.
            if self.eintrag.neuStartenNachUpdate && liefVorher && ende.updateLiefDurch {
                fortschritt.berichte("Startet \(self.name) neu.")
                Prozessdienst.starten(self.eintrag)
            }

            var bericht = UpdateBericht()
            bericht.programmId = self.eintrag.id
            bericht.name = self.name
            bericht.art = self.eintrag.art
            bericht.ergebnis = ende.ergebnis
            bericht.meldung = ende.meldung
            bericht.versionVorher = ende.fingerabdruckVorher
            bericht.versionNachher = ende.fingerabdruckNachher
            bericht.ausstehendeVersion = ende.ausstehendeVersion
            bericht.befehl = self.befehlsBeschreibung()
            bericht.exitCode = ende.ergebnis == .fehlgeschlagen || ende.ergebnis == .nichtVerifiziert ? 1 : 0

            fortschritt.berichte(Self.abschlussZeile(bericht))
            Protokollierung.laufBeenden(&bericht)
            self.letzterBericht = bericht
            return ende.fuerKarte
        }
    }

    // MARK: - Sperre

    /// Ein Sammellauf reicht seinen Besitz herunter; ein Klick muss seinen eigenen erwerben.
    /// Wer dieses Rennen verliert, startet nichts.
    /// - Returns: ein Objekt zum Freigeben (beim Sammelbesitz ein leerer Platzhalter), oder nil.
    private func erwerben(_ sammel: Laufbesitz?, _ art: String) -> Freigabe? {
        if let sammel {
            if koordination.istAktiverSammelbesitz(sammel) { return Freigabe(nil) }
            abgewiesen(art, "Sammelbesitz ungültig oder nicht mehr aktiv.")
            return nil
        }
        if let eigen = koordination.einzelBeginnen() { return Freigabe(eigen) }
        abgewiesen(art, "Es läuft bereits ein anderer Vorgang.")
        return nil
    }

    /// Kapselt "nur freigeben, was man selbst erworben hat".
    @MainActor final class Freigabe {
        private let besitz: Laufbesitz?
        init(_ besitz: Laufbesitz?) { self.besitz = besitz }
        func freigeben() { besitz?.freigeben() }
    }

    private func abgewiesen(_ art: String, _ grund: String) {
        statusText = Self.belegtText
        Diagnose.ereignis(.warnung, "karte", "start.abgewiesen", "\(art) für \(name) abgewiesen: \(grund)",
                          ["programm": eintrag.id, "art": art])
    }

    private func doppelt(_ art: String) {
        Diagnose.ereignis(.warnung, "karte", "start.doppelt", "\(art) für \(name) ignoriert: die Karte arbeitet bereits.",
                          ["programm": eintrag.id, "art": art])
    }

    private static func beschreibe(_ fingerabdruck: String) -> String {
        fingerabdruck.istLeer ? "(unbekannt)" : fingerabdruck
    }

    private static func abschlussZeile(_ bericht: UpdateBericht) -> String {
        switch bericht.ergebnis {
        case .erfolgreich, .bereitsAktuell: return "✔ " + bericht.meldung
        case .ausstehend: return "⏳ " + bericht.meldung
        case .abgebrochen: return "– " + bericht.meldung
        default: return "✘ " + bericht.meldung
        }
    }

    /// Verstaendliche Beschreibung dessen, was diese Karte wirklich ausfuehrt -- fuer den
    /// Protokollkopf.
    private func befehlsBeschreibung() -> String {
        switch eintrag.art {
        case "brew":
            return "brew install --cask --force " + (eintrag.cask ?? "?")
        case "cli":
            return Pfade.aufloesen(eintrag.exePfadWirksam) + " " + (eintrag.updateArgumente ?? "update")
        case "reposkript":
            return "bash " + (eintrag.skript ?? "") + " " + (eintrag.skriptArgumente ?? "")
        default:
            return eintrag.art
        }
    }

    // MARK: - Lauf-Rahmen

    /// Jede Pruefung und jedes Update jeder Karte laeuft hier durch: ein Diagnose-Vorgang mit
    /// Beginn, Ende, Dauer und Urteil.
    private func lauf(_ art: String, _ arbeit: @escaping (Fortschritt) async -> PruefErgebnis) async {
        if istBeschaeftigt { doppelt(art); return }
        istBeschaeftigt = true
        let vorgang = Diagnose.vorgangBeginnen(art, eintrag.id, "\(art) \(name)")

        let id = eintrag.id
        let fortschritt = Fortschritt { [weak self] zeile in
            Protokollierung.schreiben(id, zeile)
            Task { @MainActor in
                guard let self else { return }
                self.protokollAnhaengen(zeile.trimmed)
                self.melden()
            }
        }

        let ergebnis = await arbeit(fortschritt)

        zustand = ergebnis.zustand
        if !ergebnis.installierteVersion.istLeer { installierteVersion = ergebnis.installierteVersion }
        verfuegbareVersion = ergebnis.verfuegbareVersion
        statusText = ergebnis.meldung.istLeer ? aktionsText : ergebnis.meldung
        let schwere: Schwere
        switch ergebnis.zustand {
        case .fehler: schwere = .fehler
        case .unbekannt, .abgebrochen: schwere = .warnung
        default: schwere = .info
        }
        vorgang.beenden("\(ergebnis.zustand)", ergebnis.meldung, schwere)

        istBeschaeftigt = false
        zustandAktualisieren()
        melden()
    }

    private func protokollAnhaengen(_ text: String) {
        protokollPuffer += text + "\n"
        if protokollPuffer.count > Self.maxProtokollZeichen {
            protokollPuffer = "…[ältere Zeilen gekürzt]…\n" + String(protokollPuffer.suffix(Self.maxProtokollZeichen))
        }
    }
}
