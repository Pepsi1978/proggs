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
        "reposkript": RepoSkriptAktualisierer(),
        "opencodesitzung": OpenCodeSitzungAktualisierer()
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

    /// Zweite Fusszeile waehrend eines Sammellaufs: was gerade laeuft, an welcher Position, seit wann.
    private(set) var laufDetail = "" { didSet { beiAenderung?() } }

    /// Wie weit Sammelpruefung bzw. Sammellauf sind: x von n fertig (Fortschrittsbalken der Fusszeile).
    private(set) var fortschrittFertig = 0
    private(set) var fortschrittGesamt = 1
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

    /// Die Karten nach Gruppe gebuendelt, die Gruppen in der Reihenfolge des Katalogs. Innerhalb
    /// jeder Gruppe: Katalogreihenfolge, oder -- mit "⏱ nach Dauer" -- das kuerzeste erwartete
    /// Update zuerst. Ersetzt die CollectionViewSource-Gruppierung und -Sortierung aus WPF.
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
        return reihenfolge.map { name in
            var liste = inhalt[name] ?? []
            if nachDauerSortieren {
                liste.sort { a, b in
                    a.erwarteteDauerSekunden != b.erwarteteDauerSekunden
                        ? a.erwarteteDauerSekunden < b.erwarteteDauerSekunden
                        : a.katalogIndex < b.katalogIndex
                }
            }
            return (name, liste)
        }
    }

    // MARK: - Sortierung

    /// Innerhalb jeder Gruppe: Katalogreihenfolge, oder das kuerzeste erwartete Update zuerst.
    var nachDauerSortieren: Bool {
        get { einstellungen.nachDauerSortieren }
        set {
            guard einstellungen.nachDauerSortieren != newValue else { return }
            einstellungen.nachDauerSortieren = newValue
            einstellungen.speichern()
            sortierungAnwenden()
        }
    }

    /// Ordnet die Karten neu -- nur, wenn gerade nichts laeuft; sonst nach dem Lauf.
    private func sortierungAnwenden() {
        guard !laeuftSammelvorgang else { return }
        beiListenWechsel?()
        beiAenderung?()
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

    /// Gruen nur, wenn jede Karte geprueft ist und weder ein Update noch ein Fehler offen steht.
    var allesAktuell: Bool {
        !programme.isEmpty && programme.allSatisfy {
            switch $0.zustand {
            case .unbekannt, .pruefe, .updateVerfuegbar, .fehler: return false
            default: return true
            }
        }
    }

    /// Beide Teilplaene aus EINEM Plan, getrennt nach dem CLI-Risiko der Karte -- nicht nach der
    /// Phase: ein Repo-Skript oder ein "zuletzt"-Eintrag mit cliRisiko hat eine andere Phase und
    /// gehoert trotzdem zum CLI-Knopf.
    private func plaeneBerechnen() -> (sicher: [Sammelplan.Schritt], cli: [Sammelplan.Schritt]) {
        let alles = Sammelplan.planen(programme.filter(\.hatUpdate))
        return (alles.filter { !$0.programm.kannCliBeenden }, alles.filter { $0.programm.kannCliBeenden })
    }

    /// Beschriftung des Knopfs fuer alles, was nicht CLI-kritisch ist.
    var sicherText: String {
        let n = plaeneBerechnen().sicher.count
        let nurKlick = programme.filter { $0.hatUpdate && $0.istHauptversionsSprung && !$0.kannCliBeenden }.count
        if n == 0 {
            if nurKlick > 0 { return "\(nurKlick) Hauptversion\(nurKlick == 1 ? "" : "en") nur per Klick auf der Karte" }
            return allesAktuell ? "✔ Alles aktuell" : "Keine unkritischen Updates"
        }
        return (n == 1 ? "1 Update installieren" : "\(n) Updates installieren") + "  ·  nicht CLI-kritisch"
            + (nurKlick > 0 ? "  ·  \(nurKlick) nur per Klick" : "")
    }

    /// Beschriftung des Knopfs fuer die CLI-kritischen Updates.
    var cliText: String {
        let n = plaeneBerechnen().cli.count
        return n == 0 ? "Keine CLI-kritischen Updates"
            : (n == 1 ? "1 CLI-kritisches Update" : "\(n) CLI-kritische Updates") + " installieren"
    }

    var hatSichereUpdates: Bool { !plaeneBerechnen().sicher.isEmpty }
    var hatCliUpdates: Bool { !plaeneBerechnen().cli.isEmpty }

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
        let (berichte, dauern) = Protokollierung.verlaufLesen()

        for (index, eintrag) in katalog.programme.enumerated() {
            let dienst = aktualisierer[eintrag.art.lowercased()]
            let karte = ProgrammViewModel(eintrag: eintrag, aktualisierer: dienst, einstellungen: einstellungen,
                                          koordination: koordination)
            karte.katalogIndex = index
            if let gemessen = dauern[eintrag.id] { karte.dauernSetzen(gemessen) }
            karte.beiAenderung = { [weak self] in self?.beiAenderung?() }
            karte.beiMeldung = { text in Dialoge.hinweis(text) }

            // Der Verlauf ueberlebt Neustarts -- jede Karte kann zeigen, wie ihr letzter Lauf ging.
            if let bericht = berichte[eintrag.id] { karte.letzterBericht = bericht }

            // Ebenso die Antwort der letzten Pruefung -- die Liste ist aussagekraeftig, bevor etwas laeuft.
            if let stand = Pruefstand.lesen(eintrag.id) { karte.standUebernehmen(stand) }

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
        await erstePruefungKern()

        // Ein vom Selbst-Update unterbrochener Sammellauf geht jetzt weiter.
        if einstellungen.sammelFortsetzen != nil { await sammelFortsetzen() }
    }

    private func erstePruefungKern() async {
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

        // Neu geprueft wird nur, was keine frische Antwort hat. Die eigenen Werkzeuge immer: das
        // kostet einen gemeinsamen git pull und ist der einzige Weg, wie eine gerade im Repo
        // angekommene Version ohne Klick erscheint.
        let grenze = Date().addingTimeInterval(-Self.standGueltig)
        let faellig = programme.filter { p in
            guard let zeit = p.standVom else { return true }
            return zeit < grenze || p.eintrag.art == "reposkript"
        }
        if faellig.isEmpty {
            let aeltester = programme.compactMap(\.standVom).min()
            kopfStatus = "Stand der letzten Prüfung von " + (aeltester.map(Formate.uhrzeit) ?? "–") + " Uhr – "
                       + updateZusammenfassung + ". „Alle prüfen“ fragt neu ab."
            vorgang.beenden("uebernommen", kopfStatus)
            return
        }

        await allePruefenMitBesitz(besitz, nur: faellig)
        vorgang.beenden("abgeschlossen", kopfStatus)
    }

    /// Wie lange eine gemerkte Antwort beim naechsten Start die Pruefung erspart.
    private static let standGueltig: TimeInterval = 60 * 60

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

    private func allePruefenMitBesitz(_ besitz: Laufbesitz, nur: [ProgrammViewModel]? = nil) async {
        // Eine Momentaufnahme: die Liste darf sich unter dem Lauf nicht verschieben.
        let liste = nur ?? programme

        // Erst den Homebrew-Katalog auffrischen, DANN pruefen. Ohne das liest `brew info` den
        // lokalen Tap, und der kann Tage alt sein -- die Pruefung meldete dann "Aktuell", obwohl
        // ein Cask laengst neuer ist. `winget list` unter Windows hat dieses Problem nicht, weil es
        // seine Quelle selbst aktuell haelt.
        if liste.contains(where: { $0.eintrag.art == "brew" }) {
            kopfStatus = "Homebrew-Katalog wird aufgefrischt …"
            _ = await Kommandozeile.ausfuehren(Pfade.brew, ["update", "--quiet"], zeitlimit: 300)
        }

        // Die eigenen Werkzeuge werden aus ~/proggs gebaut -- also zuerst den Checkout abgleichen,
        // einmal fuer alle und bevor eine Karte daraus eine Version liest.
        var repoHinweis: String?
        if liste.contains(where: { $0.eintrag.art == "reposkript" }) {
            kopfStatus = "Gleicht das Repo mit GitHub ab (git pull) …"
            let abgleich = await RepoAbgleich.shared.abgleichen(erzwingen: true)
            if !abgleich.gezogenOk { repoHinweis = abgleich.meldung }
        }

        // Alle gleichzeitig: die Pruefungen warten auf verschiedene Dinge (brew, die Registries,
        // git, lms). Nacheinander dauerte derselbe Durchgang Minuten; jetzt bestimmt die
        // langsamste einzelne Pruefung das Tempo.
        var offen = liste.map(\.name)
        var fertig = 0
        fortschrittGesamt = max(liste.count, 1)
        func anzeigen() {
            fortschrittFertig = fertig
            kopfStatus = "Prüft … \(fertig) von \(liste.count) fertig"
                + (offen.isEmpty ? "" : " – läuft noch: " + offen.prefix(4).joined(separator: ", ")
                                        + (offen.count > 4 ? " …" : ""))
        }
        anzeigen()
        await withTaskGroup(of: String.self) { gruppe in
            for programm in liste {
                gruppe.addTask {
                    await programm.pruefen(sammel: besitz)
                    return await programm.name
                }
            }
            for await name in gruppe {
                if let stelle = offen.firstIndex(of: name) { offen.remove(at: stelle) }
                fertig += 1
                anzeigen()
            }
        }

        kopfStatus = "Prüfung abgeschlossen – " + updateZusammenfassung + "."
                   + (repoHinweis.map { "  ·  " + $0 } ?? "")
    }

    /// Zwei unabhaengige Knoepfe: alles, was keine CLI-Sitzung beruehren kann, und die
    /// CLI-kritischen Updates. Jeder darf zuerst -- keiner wartet auf den anderen.
    func sichereAktualisieren() async { await sammelStarten(cli: false) }
    func cliAktualisieren() async { await sammelStarten(cli: true) }

    private func sammelStarten(cli: Bool) async {
        if koordination.belegt {
            kopfStatus = "Updates gehen erst, wenn der laufende Vorgang fertig ist."
            sammelAbgewiesen("sammelupdate")
            return
        }

        let plaene = plaeneBerechnen()
        let plan = cli ? plaene.cli : plaene.sicher
        guard !plan.isEmpty else {
            Dialoge.hinweis(cli
                ? "Es ist kein CLI-kritisches Update offen."
                : "Es ist kein unkritisches Update offen, das gesammelt installiert werden kann. Prüfe zuerst, oder "
                  + "aktualisiere einzelne Programme gezielt (Hauptversionssprünge nur per Klick auf der Karte).")
            return
        }

        let nurKlick = programme.filter { $0.hatUpdate && $0.istHauptversionsSprung }
        let liste = plan.enumerated().map { i, s in
            "  \(i + 1). \(s.programm.name)  (\(Sammelplan.phasenName(s.phase)), "
            + Protokollierung.dauerText(s.programm.erwarteteDauerSekunden) + ")"
        }.joined(separator: "\n")
        let frage = cli
            ? "Diese Updates können laufende CLI-Sitzungen beenden:\n\n"
              + plan.enumerated().map { i, s in "  \(i + 1). \(s.programm.name) – \(s.programm.eintrag.cliRisiko ?? "")" }
                  .joined(separator: "\n")
              + "\n\nSchließe vorher Claude Code, Codex, OpenCode und alle Terminalfenster, dann „Ja“."
              + "\n\nBei laufenden Programmen wird vorher nachgefragt."
            : "Reihenfolge der Updates (keines davon ist CLI-kritisch):\n\n" + liste
              + Self.abhaengigkeiten(plan, plaene.cli)
              + (nurKlick.isEmpty ? "" : "\n\nNur per Klick auf der Karte (Hauptversion): " + nurKlick.map(\.name).joined(separator: ", "))
              + "\n\nBei laufenden Programmen wird vorher nachgefragt."
        guard Dialoge.fragen(frage, cli ? "CLI-kritische Updates installieren?" : "Unkritische Updates installieren?") else { return }

        let stempel = DateFormatter()
        stempel.dateFormat = "yyyyMMdd-HHmmss"
        // Die Frage des CLI-Knopfs war schon "jetzt die CLIs schließen" -- im Lauf keine zweite.
        await sammelAusfuehren(plan, sammelId: "sammel-" + (cli ? "cli-" : "") + stempel.string(from: Date()), cliBestaetigt: cli)
    }

    /// Mit zwei Knoepfen bestimmt der Benutzer die Reihenfolge -- manche unkritischen Updates bauen
    /// aber auf einem CLI-kritischen auf (npm-Werkzeuge auf Node, Gradle/Kotlin auf dem JDK). Die
    /// Frage nennt das, damit die Wahl eine bewusste ist.
    private static func abhaengigkeiten(_ plan: [Sammelplan.Schritt], _ cliOffen: [Sammelplan.Schritt]) -> String {
        var offen: [String: String] = [:]
        for s in cliOffen { offen[s.programm.eintrag.id.lowercased()] = s.programm.name }
        var paare: [(vorher: String, danach: [String])] = []
        for s in plan {
            for v in s.programm.eintrag.nach {
                guard let name = offen[v.lowercased()] else { continue }
                if let i = paare.firstIndex(where: { $0.vorher == name }) {
                    paare[i].danach.append(s.programm.name)
                } else {
                    paare.append((name, [s.programm.name]))
                }
            }
        }
        guard !paare.isEmpty else { return "" }
        return "\n\nHinweis: Diese Updates bauen auf CLI-kritischen auf, die noch offen sind. Am besten zuerst den Knopf "
            + "„⚠ CLI-kritische Updates“:\n" + paare.map { "  • \($0.vorher) vor " + $0.danach.joined(separator: ", ") }.joined(separator: "\n")
    }

    /// Ein Programm eines Sammellaufs, wie es in sammellaeufe.jsonl steht.
    private struct SammelLauf {
        let position: Int, id: String, phase: Int, parallel: Bool
        let beginn: Date, ende: Date, sekunden: Double, ergebnis: String, erwartetSekunden: Double

        var json: [String: Any] {
            let f = ISO8601DateFormatter()
            return ["position": position, "id": id, "phase": phase, "parallel": parallel,
                    "beginn": f.string(from: beginn), "ende": f.string(from: ende), "sekunden": sekunden,
                    "ergebnis": ergebnis, "erwartetSekunden": erwartetSekunden]
        }
    }

    /// Fuehrt einen Sammelplan in EINEM exklusiven Besitz aus. Die Updater-Zentrale selbst geht
    /// zuerst und allein: ihr Skript startet die App neu, also wird der Rest gesichert und von der
    /// neuen Fassung fortgesetzt. Ein Plan vom CLI-Knopf wurde mit dessen Frage bestaetigt; jeder
    /// andere Plan, der doch CLI-kritische Schritte enthaelt, haelt davor einmal an und fragt. Die
    /// Phasen laufen nacheinander; innerhalb einer Phase laufen "parallel"-Eintraege in einer
    /// zweiten Bahn (hoechstens drei) neben der seriellen (brew, npm, Skripte).
    private func sammelAusfuehren(_ plan: [Sammelplan.Schritt], sammelId: String, versatz: Int = 0,
                                  planGroesse: Int = 0, cliBestaetigt: Bool = false) async {
        // Erst jetzt erworben: die Frage davor ist modal, und was inzwischen startete, gewinnt.
        guard let besitz = koordination.sammelBeginnen() else {
            kopfStatus = "Updates gehen erst, wenn der laufende Vorgang fertig ist."
            sammelAbgewiesen("sammelupdate")
            return
        }
        defer { besitz.freigeben() }

        let gesamtZahl = max(planGroesse, plan.count + versatz)
        let laufName = cliBestaetigt ? "CLI-kritische Updates" : "Updates"
        laeuftSammelvorgang = true
        let gesamtBeginn = Date()
        var laeufe: [SammelLauf] = []
        var laufend: [(name: String, zeile: String)] = []
        var fertig = 0
        var cliHinweis = ""
        fortschrittGesamt = max(plan.count, 1)
        fortschrittFertig = 0
        let vorgang = Diagnose.vorgangBeginnen("sammelupdate", nil, laufName + ": " + plan.map(\.programm.eintrag.id).joined(separator: ", "))
        Protokollierung.sammellaufSchreiben([
            "sammelId": sammelId, "ereignis": versatz == 0 ? "plan" : "fortsetzung", "versatz": versatz, "planGroesse": gesamtZahl,
            "plan": plan.enumerated().map { i, s in
                ["position": versatz + i + 1, "id": s.programm.eintrag.id, "phase": s.phase, "parallel": s.parallel,
                 "erwartetSekunden": s.programm.erwarteteDauerSekunden.rounded(), "gemessen": s.programm.dauerGemessen] as [String: Any]
            }
        ])

        func anzeigen() {
            fortschrittFertig = fertig
            kopfStatus = laufName + ": \(versatz + fertig) von \(gesamtZahl) fertig, \(laufend.count) laufen gerade"
                + " – seit " + Protokollierung.dauerText(Date().timeIntervalSince(gesamtBeginn)) + "." + cliHinweis
            laufDetail = laufend.map(\.zeile).joined(separator: "\n")
        }

        func abschluss(_ ereignis: String) {
            Protokollierung.sammellaufSchreiben([
                "sammelId": sammelId, "ereignis": ereignis,
                "gesamtSekunden": (Date().timeIntervalSince(gesamtBeginn) * 10).rounded() / 10,
                "laeufe": laeufe.map(\.json)
            ])
        }

        func einer(_ s: Sammelplan.Schritt) async -> UpdateBericht? {
            let position = versatz + (plan.firstIndex { $0.programm === s.programm } ?? 0) + 1
            laufend.append((s.programm.name, "▶ " + s.programm.name + "  ·  Position \(position) von \(gesamtZahl)"
                            + "  ·  " + Sammelplan.phasenName(s.phase) + (s.parallel ? " (parallel)" : "")
                            + "  ·  gestartet " + Formate.uhrzeitMitSekunden(Date())
                            + "  ·  erwartet ≈ " + Protokollierung.dauerText(s.programm.erwarteteDauerSekunden)))
            anzeigen()
            let beginn = Date()
            let bericht = await s.programm.aktualisierenImSammel(besitz, SammelKontext(id: sammelId, phase: s.phase, position: position))
            laufend.removeAll { $0.name == s.programm.name }
            fertig += 1
            anzeigen()
            laeufe.append(SammelLauf(position: position, id: s.programm.eintrag.id, phase: s.phase, parallel: s.parallel,
                                     beginn: beginn, ende: Date(),
                                     sekunden: (Date().timeIntervalSince(beginn) * 10).rounded() / 10,
                                     ergebnis: bericht?.ergebnisText ?? "ohne Bericht",
                                     erwartetSekunden: s.programm.erwarteteDauerSekunden.rounded()))
            return bericht
        }

        var ende = "abgeschlossen"
        var rest = plan
        if let selbst = plan.first(where: { Sammelplan.istSelbst($0.programm) }) {
            rest = plan.filter { !Sammelplan.istSelbst($0.programm) }
            // VOR dem Lauf gesichert: der Helfer des Skripts uebernimmt, sobald die App endet.
            if rest.isEmpty {
                einstellungen.sammelFortsetzen = nil
            } else {
                let f = SammelFortsetzung()
                f.sammelId = sammelId
                f.zeit = Date()
                f.versatz = versatz + 1
                f.planGroesse = gesamtZahl
                f.programmIds = rest.map(\.programm.eintrag.id)
                f.cliBestaetigt = cliBestaetigt
                einstellungen.sammelFortsetzen = f
            }
            einstellungen.speichern()
            ausgewaehltesProgramm = selbst.programm
            let bericht = await einer(selbst)

            // Nur ein echtes "gebaut" startet die App neu: das Selbst-Update ist ausstehend (die neue
            // Fassung kommt erst nach dem Ende dieses Prozesses). Alles andere -- Nein, Fehler, nichts
            // zu tun -- geht hier sofort weiter.
            if bericht?.ergebnis == .ausstehend {
                ende = "selbstupdate"
                abschluss("selbstupdate-neustart")   // jetzt geschrieben: dieser Prozess erreicht das Ende nicht
                kopfStatus = rest.isEmpty
                    ? "Die Updater-Zentrale startet in der neuen Fassung neu …"
                    : "Die Updater-Zentrale startet neu und setzt die übrigen \(rest.count) Updates danach fort …"
                laufDetail = ""
                // Alles dieses Laufs ist geschrieben: die App beendet sich, der Helfer wartet genau darauf.
                await ProgrammViewModel.appBeenden()
                try? await Task.sleep(nanoseconds: 90_000_000_000)
                // Noch am Leben: der Neustart kam nicht zustande -- diese Fassung macht selbst weiter.
                ende = "abgeschlossen"
                Diagnose.ereignis(.warnung, "sammel", "sammel.kein_neustart",
                                  "Kein Neustart nach dem Selbst-Update – der Sammellauf geht hier weiter.")
            }
            einstellungen.sammelFortsetzen = nil
            einstellungen.speichern()
        }

        let phasen = Array(Set(rest.map(\.phase))).sorted()
        for phase in phasen {
            let schritte = rest.filter { $0.phase == phase }

            // Der eine Moment, in dem die CLIs zu sein muessen: direkt vor den Updates, die ihre
            // Sitzungen beenden koennen. "Nein" laesst sie fuer einen spaeteren Klick offen.
            if phase == Sammelplan.phaseCli && !cliBestaetigt {
                laufDetail = ""
                let frage = "Jetzt kommen die CLI-kritischen Updates:\n\n"
                    + schritte.map { "  • \($0.programm.name) – \($0.programm.eintrag.cliRisiko ?? "")" }.joined(separator: "\n")
                    + "\n\nSchließe jetzt Claude Code, Codex, OpenCode und alle Terminalfenster und klicke dann auf „Ja“."
                    + "\n„Nein“ überspringt sie (sie bleiben offen); die übrigen Updates laufen weiter."
                if !Dialoge.fragen(frage, "CLI-kritische Updates jetzt installieren?") {
                    Diagnose.ereignis(.info, "sammel", "sammel.cli_uebersprungen", "\(schritte.count) CLI-kritische Updates übersprungen.")
                    Protokollierung.sammellaufSchreiben(["sammelId": sammelId, "ereignis": "cli-uebersprungen",
                                                         "ids": schritte.map(\.programm.eintrag.id)])
                    fertig += schritte.count
                    cliHinweis = "  ·  CLI-kritische Updates übersprungen – sie bleiben offen."
                    anzeigen()
                    continue
                }
            }

            let seriell = schritte.filter { !$0.parallel }
            let parallel = schritte.filter(\.parallel)

            await withTaskGroup(of: Void.self) { gruppe in
                gruppe.addTask { @MainActor in
                    for s in seriell {
                        self.ausgewaehltesProgramm = s.programm
                        _ = await einer(s)
                    }
                }
                // Parallele Bahn: hoechstens drei zugleich.
                gruppe.addTask { @MainActor in
                    var index = 0
                    await withTaskGroup(of: Void.self) { bahn in
                        func naechster() {
                            guard index < parallel.count else { return }
                            let s = parallel[index]
                            index += 1
                            bahn.addTask { @MainActor in _ = await einer(s) }
                        }
                        for _ in 0..<3 { naechster() }
                        for await _ in bahn { naechster() }
                    }
                }
            }

            if phase == Sammelplan.phaseCli && rest.contains(where: { $0.phase > Sammelplan.phaseCli }) {
                cliHinweis = "  ✔ CLI-kritische Updates sind durch – du kannst im CLI weiterarbeiten."
                anzeigen()
                Diagnose.ereignis(.info, "sammel", "sammel.cli_fertig", kopfStatus)
            }
        }

        let fehler = laeufe.filter { ["Fehlgeschlagen", "Nicht verifiziert", "ohne Bericht"].contains($0.ergebnis) }.count
        kopfStatus = laufName + " sind durchgelaufen (" + Protokollierung.dauerText(Date().timeIntervalSince(gesamtBeginn)) + ")"
            + (fehler > 0 ? " – \(fehler) mit Problemen, siehe rote Karten." : ".")
            + (cliHinweis.contains("übersprungen") ? cliHinweis : "")
            + (cliBestaetigt ? "  ✔ Du kannst im CLI weiterarbeiten." : "")
        vorgang.beenden("abgeschlossen", kopfStatus)
        abschluss(ende)
        laeuftSammelvorgang = false

        // Erst am Ende und in EINER Sitzung: mitten im Lauf koennten spaetere Schritte (Terminal,
        // CLI, Launcher) die Sitzung wieder beenden, und mehrere Sitzungen kaemen sich auf main in die Quere.
        let faelle = plan.compactMap { s -> (ProgrammEintrag, UpdateBericht)? in
            guard let b = s.programm.letzterBericht, b.sammelId == sammelId else { return nil }
            return (s.programm.eintrag, b)
        }
        if faelle.contains(where: { ClaudeUebergabe.automatischRelevant($0.1) }) {
            if let problem = await ClaudeUebergabe.automatischStarten(faelle, einstellungen) {
                kopfStatus += "  ·  " + problem
            } else if faelle.contains(where: { $0.1.ergebnis == .fehlgeschlagen
                && Date().timeIntervalSince(einstellungen.fuer($0.0.id).automatischUebergebenAm ?? .distantPast) < 60 }) {
                kopfStatus += "  ·  Fehler automatisch an Claude Code übergeben."
            }
            anzeigen()
        }
        laufDetail = ""
        if nachDauerSortieren { sortierungAnwenden() }
    }

    /// Nach dem Selbst-Update in einem Sammellauf setzt die neue Fassung den gesicherten Rest fort --
    /// einmal, und nur, wenn er frisch ist. Dem ganzen Lauf hat der Benutzer schon zugestimmt; die
    /// CLI-kritische Phase stellt trotzdem ihre eigene Frage (ausser der Lauf kam vom CLI-Knopf).
    private func sammelFortsetzen() async {
        guard let offen = einstellungen.sammelFortsetzen else { return }
        einstellungen.sammelFortsetzen = nil
        einstellungen.speichern()
        guard Date().timeIntervalSince(offen.zeit) <= 30 * 60 else { return }

        let ids = Set(offen.programmIds.map { $0.lowercased() })
        let plan = Sammelplan.planen(programme.filter {
            $0.hatUpdate && ids.contains($0.eintrag.id.lowercased()) && !Sammelplan.istSelbst($0)
        })
        Diagnose.ereignis(.info, "sammel", "sammel.fortsetzung", "\(plan.count) Updates nach dem Selbst-Update fortgesetzt.")
        if !plan.isEmpty {
            await sammelAusfuehren(plan, sammelId: offen.sammelId, versatz: offen.versatz, planGroesse: offen.planGroesse,
                                   cliBestaetigt: offen.cliBestaetigt)
        }
    }
}
