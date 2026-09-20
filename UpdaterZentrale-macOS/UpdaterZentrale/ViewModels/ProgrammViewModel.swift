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
    private var protokollPuffer = ""

    /// Wird nach jeder sichtbaren Aenderung aufgerufen.
    var beiAenderung: (() -> Void)?
    /// Wird fuer Meldungen aufgerufen, die ein Hinweisfenster verdienen.
    var beiMeldung: ((String) -> Void)?

    let eintrag: ProgrammEintrag

    init(eintrag: ProgrammEintrag, aktualisierer: Aktualisierer?, einstellungen: Einstellungen) {
        self.eintrag = eintrag
        self.aktualisierer = aktualisierer
        self.einstellungen = einstellungen

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
    var kannStarten: Bool { eintrag.istAppBuendel || !eintrag.exePfad.istLeer }

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
        aktualisierer != nil && !istBeschaeftigt && zustand != .aktuell && !uebernahmeOffen
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

    var kannPruefen: Bool { aktualisierer != nil && !istBeschaeftigt }

    var versionsText: String {
        if installierteVersion.istLeer { return "–" }
        if verfuegbareVersion.istLeer || verfuegbareVersion == installierteVersion {
            return installierteVersion
        }
        return installierteVersion + "   →   " + verfuegbareVersion
    }

    var hatBericht: Bool { letzterBericht != nil }
    var berichtIstFehler: Bool { letzterBericht?.istFehler == true }
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

        let jetzt = await aktualisierer.fingerabdruck(eintrag)

        if jetzt.istLeer || jetzt == offen.versionNachher {
            // Nach sieben Tagen ist "wird beim naechsten Start aktiv" keine Erklaerung mehr.
            if Date().timeIntervalSince(offen.zeit) >= 7 * 24 * 60 * 60 {
                var haengt = UpdateBericht()
                haengt.zeit = Date()
                haengt.programmId = eintrag.id
                haengt.name = name
                haengt.art = eintrag.art
                haengt.ergebnis = .nichtVerifiziert
                haengt.versionVorher = offen.versionNachher
                haengt.versionNachher = jetzt
                haengt.befehl = offen.befehl
                haengt.meldung = "Das Update vom \(Formate.datum(offen.zeit)) wurde bis heute nicht "
                               + "übernommen – das Programm wurde offenbar nie neu gestartet."
                Protokollierung.laufBeenden(&haengt)
                letzterBericht = haengt
            }
            return
        }

        var bestaetigt = UpdateBericht()
        bestaetigt.zeit = Date()
        bestaetigt.programmId = eintrag.id
        bestaetigt.name = name
        bestaetigt.art = eintrag.art
        bestaetigt.ergebnis = .erfolgreich
        bestaetigt.versionVorher = offen.versionNachher
        bestaetigt.versionNachher = jetzt
        bestaetigt.befehl = offen.befehl
        bestaetigt.meldung = "Nachträglich bestätigt: das Update vom \(Formate.datum(offen.zeit)) "
                           + "ist inzwischen aktiv (\(jetzt))."
        Protokollierung.laufBeenden(&bestaetigt)
        letzterBericht = bestaetigt
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

    func pruefen() async {
        guard let aktualisierer else { return }
        await lauf { fortschritt in
            self.statusText = "Wird geprüft …"
            self.zustand = .pruefe
            return await aktualisierer.pruefen(self.eintrag, fortschritt)
        }
    }

    func aktualisieren() async {
        guard let aktualisierer else { return }

        // Installer haengen still, solange das Programm laeuft -- deshalb gehen die Helfer mit
        // herunter, aber erst nachdem der Benutzer zugestimmt hat.
        if eintrag.beendenVorUpdate && Prozessdienst.laeuft(eintrag) {
            let laufende = Prozessdienst.laufende(eintrag).count
            let frage = "\(name) läuft gerade (\(laufende) Prozess(e) einschließlich Helferprogramme).\n\n"
                      + "Zum Aktualisieren muss das Programm beendet werden."
                      + (eintrag.neuStartenNachUpdate ? " Danach wird es automatisch neu gestartet." : "")
                      + "\n\nJetzt beenden und aktualisieren?"

            if !Dialoge.fragen(frage, "Programm beenden?") {
                statusText = "Abgebrochen – das Programm läuft weiter."
                zustand = .abgebrochen
                return
            }
        }

        await lauf { fortschritt in
            // Beweis vor dem Lauf: was sich aendern MUSS, wenn das Update wirklich ankommt.
            self.statusText = "Ermittelt den Stand …"
            let vorher = await aktualisierer.fingerabdruck(self.eintrag)

            Protokollierung.laufBeginnen(self.eintrag, befehl: self.befehlsBeschreibung(),
                                         fingerabdruckVorher: vorher)
            let liefVorher = Prozessdienst.laeuft(self.eintrag)

            if self.eintrag.beendenVorUpdate && liefVorher {
                self.statusText = "Beendet das Programm …"
                fortschritt.berichte("Beende " + self.eintrag.alleProzesse.joined(separator: ", ")
                                     + (self.eintrag.alleProzesse.isEmpty ? self.name : ""))
                await Prozessdienst.beenden(self.eintrag)
            }

            self.statusText = "Aktualisiert …"
            let ergebnis = await aktualisierer.aktualisieren(self.eintrag, fortschritt)

            if self.eintrag.neuStartenNachUpdate && liefVorher && ergebnis.zustand == .fertig {
                fortschritt.berichte("Startet \(self.name) neu.")
                Prozessdienst.starten(self.eintrag)
            }

            // Beweis nach dem Lauf, und das Urteil aus dem Vergleich beider.
            self.statusText = "Prüft das Ergebnis …"
            let nachher = await aktualisierer.fingerabdruck(self.eintrag)
            var bericht = self.bewerten(ergebnis, vorher: vorher, nachher: nachher)

            fortschritt.berichte(Self.abschlussZeile(bericht))
            Protokollierung.laufBeenden(&bericht)
            self.letzterBericht = bericht

            // Die angezeigten Versionen nachziehen -- aber nicht, wenn der Installer das Update
            // nur abgelegt hat: dort ist die alte Version weiter die Wahrheit, und eine neue
            // Pruefung wuerde faelschlich wieder "Update verfügbar" zeigen.
            if bericht.ergebnis == .erfolgreich {
                let frisch = await aktualisierer.pruefen(self.eintrag, fortschritt)
                var angepasst = ergebnis
                angepasst.zustand = frisch.zustand == .aktuell ? .aktuell : ergebnis.zustand
                angepasst.installierteVersion = frisch.installierteVersion
                angepasst.verfuegbareVersion = frisch.verfuegbareVersion
                angepasst.meldung = bericht.meldung
                return angepasst
            }

            var angepasst = ergebnis
            switch bericht.ergebnis {
            case .abgebrochen: angepasst.zustand = .abgebrochen
            case .fehlgeschlagen, .nichtVerifiziert: angepasst.zustand = .fehler
            default: break
            }
            angepasst.meldung = bericht.meldung
            return angepasst
        }
    }

    // MARK: - Bewertung

    /// Macht aus "das Werkzeug endete mit 0" eine Aussage ueber die Wirklichkeit: hat sich der
    /// Fingerabdruck, der sich aendern musste, wirklich geaendert? Alles andere wird als Problem
    /// ausgewiesen, mit Grund.
    private func bewerten(_ ergebnis: PruefErgebnis, vorher: String, nachher: String) -> UpdateBericht {
        var bericht = UpdateBericht()
        bericht.programmId = eintrag.id
        bericht.name = name
        bericht.art = eintrag.art
        bericht.versionVorher = vorher
        bericht.versionNachher = nachher
        bericht.befehl = befehlsBeschreibung()
        bericht.exitCode = ergebnis.zustand == .fehler ? 1 : 0

        switch ergebnis.zustand {
        case .abgebrochen:
            bericht.ergebnis = .abgebrochen
            bericht.meldung = ergebnis.meldung

        case .fehler:
            bericht.ergebnis = .fehlgeschlagen
            bericht.meldung = ergebnis.meldung

        case .aktuell:
            bericht.ergebnis = .abgebrochen
            bericht.meldung = ergebnis.meldung.istLeer ? "Es war nichts offen." : ergebnis.meldung

        default:
            if ergebnis.erstNachNeustart {
                bericht.ergebnis = .ausstehend
                bericht.ausstehendeVersion = verfuegbareVersion.istLeer ? nil : verfuegbareVersion
                bericht.meldung = ergebnis.meldung
            } else if vorher.istLeer && nachher.istLeer {
                bericht.ergebnis = .nichtVerifiziert
                bericht.meldung = "Das Update meldete Erfolg, der Stand ließ sich aber weder vorher "
                                + "noch nachher ermitteln – es ist nicht überprüfbar."
            } else if vorher == nachher {
                bericht.ergebnis = .nichtVerifiziert
                bericht.meldung = "Das Update meldete Erfolg, der Stand ist aber unverändert ("
                                + Self.beschreibe(nachher) + "). Einzelheiten stehen im Protokoll."
            } else {
                bericht.ergebnis = .erfolgreich
                bericht.meldung = "Verifiziert: " + Self.beschreibe(vorher) + " → " + Self.beschreibe(nachher)
            }
        }

        return bericht
    }

    private static func beschreibe(_ fingerabdruck: String) -> String {
        fingerabdruck.istLeer ? "(unbekannt)" : fingerabdruck
    }

    private static func abschlussZeile(_ bericht: UpdateBericht) -> String {
        switch bericht.ergebnis {
        case .erfolgreich: return "✔ " + bericht.meldung
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
            return Pfade.aufloesen(eintrag.exePfad) + " " + (eintrag.updateArgumente ?? "update")
        case "reposkript":
            return "bash " + (eintrag.skript ?? "") + " " + (eintrag.skriptArgumente ?? "")
        default:
            return eintrag.art
        }
    }

    // MARK: - Lauf-Rahmen

    private func lauf(_ arbeit: @escaping (Fortschritt) async -> PruefErgebnis) async {
        guard !istBeschaeftigt else { return }
        istBeschaeftigt = true

        let id = eintrag.id
        let fortschritt = Fortschritt { [weak self] zeile in
            Protokollierung.schreiben(id, zeile)
            Task { @MainActor in
                guard let self else { return }
                self.protokollPuffer += zeile.trimmed + "\n"
                self.melden()
            }
        }

        let ergebnis = await arbeit(fortschritt)

        zustand = ergebnis.zustand
        if !ergebnis.installierteVersion.istLeer { installierteVersion = ergebnis.installierteVersion }
        verfuegbareVersion = ergebnis.verfuegbareVersion
        statusText = ergebnis.meldung.istLeer ? aktionsText : ergebnis.meldung

        istBeschaeftigt = false
        zustandAktualisieren()
        melden()
    }
}
