import Foundation
import Darwin
import CryptoKit

/// Kommandozeilen-Werkzeuge, die sich selbst aktualisieren ("claude update", "codex update"). 1:1-Port von Providers/CliAktualisierer.cs.
///
/// Woher die verfuegbare Version kommt, entscheidet der Katalog-Eintrag: entweder ein
/// Trockenlauf-Unterbefehl, oder die npm-Registry fuer die dort veroeffentlichten Werkzeuge.
///
/// Der einzige Unterschied zu Windows: der Windows-Schalter `alsAufrufer` (Umgebungsvariable
/// `__COMPAT_LAYER=RunAsInvoker`, die den Windows-Fehler 740 bei Programmen mit
/// "Als Administrator ausführen" umgeht) entfaellt -- macOS kennt weder das Flag noch den Fehler.
struct CliAktualisierer: Aktualisierer {
    let art = "cli"

    func pruefen(_ eintrag: ProgrammEintrag, _ protokoll: Fortschritt) async -> PruefErgebnis {
        let exe = Pfade.aufloesen(eintrag.exePfadWirksam)
        guard FileManager.default.isExecutableFile(atPath: exe) else {
            return PruefErgebnis(zustand: .nichtInstalliert, meldung: "Nicht gefunden: \(exe)")
        }

        var installiert = ""
        if let argumente = eintrag.versionsArgumente, !argumente.istLeer {
            let lauf = await Kommandozeile.ausfuehren(exe, argumente, zeitlimit: 120)
            installiert = Self.versionAus(lauf.ausgabe, eintrag.versionsMuster)
            protokoll.berichte("\((exe as NSString).lastPathComponent) \(argumente) -> \(lauf.ausgabe)")
        }

        // Weg 1: das Werkzeug sagt selbst, was es aktualisieren wuerde.
        if let pruefArgumente = eintrag.pruefArgumente, !pruefArgumente.istLeer {
            protokoll.berichte("\((exe as NSString).lastPathComponent) \(pruefArgumente)")
            let lauf = await Kommandozeile.ausfuehren(exe, pruefArgumente, zeitlimit: 360)
            protokoll.berichte(lauf.ausgabe)

            // Ein Trockenlauf, der scheiterte oder abbrach, listet nichts -- das hiesse "nichts
            // offen". Nur ein sauberer Lauf darf "aktuell" sagen.
            if lauf.abgelaufen || !Self.pruefCodeGueltig(eintrag, lauf.exitCode) {
                return PruefErgebnis(zustand: .fehler, installierteVersion: installiert,
                                     meldung: lauf.abgelaufen
                                        ? "Die Prüfung hat das Zeitlimit überschritten – der Stand ist unbekannt."
                                        : "Die Prüfung endete mit Code \(lauf.exitCode) – der Stand ist unbekannt.",
                                     protokoll: lauf.ausgabe)
            }

            let geplant = Self.geplanteZeilen(lauf.ausgabe, eintrag)
            if !geplant.isEmpty {
                return PruefErgebnis(zustand: .updateVerfuegbar,
                                     installierteVersion: installiert,
                                     verfuegbareVersion: "\(geplant.count) Paket(e)",
                                     meldung: geplant.count == 1 ? geplant[0] : "\(geplant.count) Aktualisierungen geplant.",
                                     protokoll: lauf.ausgabe)
            }
            return PruefErgebnis(zustand: .aktuell, installierteVersion: installiert,
                                 meldung: "Alles auf dem neuesten Stand.", protokoll: lauf.ausgabe)
        }

        // Weg 2: gegen die npm-Registry vergleichen, die dieselbe Versionszeile fuehrt.
        // Weg 3: eine Klartext-Adresse, die die neueste Version nennt (Kimi-CDN "latest").
        let quelle: (adresse: String, json: Bool)?
        if let paket = eintrag.npmPaket, !paket.istLeer {
            quelle = ("https://registry.npmjs.org/\(paket)/latest", true)
        } else if let url = eintrag.versionsUrl, !url.istLeer {
            quelle = (url, false)
        } else {
            quelle = nil
        }
        if let quelle {
            let abfrage = await Self.netzVersion(quelle.adresse, json: quelle.json, protokoll,
                                                 muster: quelle.json ? nil : eintrag.versionsUrlMuster)
            guard let verfuegbar = abfrage.version else {
                // Eine fehlgeschlagene Abfrage ist nie "aktuell" -- sonst verschwindet ein offenes
                // Update hinter einer gruenen Plakette.
                return PruefErgebnis(zustand: .fehler, installierteVersion: installiert,
                                     meldung: abfrage.problem ?? "Versionsabfrage fehlgeschlagen.")
            }
            let neuer = Versionen.istNeuer(verfuegbar, als: installiert)
            return PruefErgebnis(zustand: neuer ? .updateVerfuegbar : .aktuell,
                                 installierteVersion: installiert,
                                 verfuegbareVersion: verfuegbar,
                                 meldung: neuer ? "Neue Version \(verfuegbar) verfügbar." : "Auf dem neuesten Stand.")
        }

        return PruefErgebnis(zustand: .unbekannt, installierteVersion: installiert,
                             meldung: "Keine Prüfquelle hinterlegt – das Update lässt sich trotzdem auslösen.")
    }

    func aktualisieren(_ eintrag: ProgrammEintrag, _ protokoll: Fortschritt) async -> PruefErgebnis {
        let exe = Pfade.aufloesen(Self.updateExe(eintrag))
        guard FileManager.default.isExecutableFile(atPath: exe) else {
            return PruefErgebnis(zustand: .nichtInstalliert, meldung: "Nicht gefunden: \(exe)")
        }

        // Vor dem Lauf festgehalten: danach faellt exePfadWirksam womoeglich schon auf einen anderen Pfad zurueck.
        let programmDatei = Pfade.aufloesen(eintrag.exePfadWirksam)
        let npmGlobal = NpmGlobal.betrifft(eintrag, programmDatei)
        if npmGlobal, let vorab = await NpmGlobal.vorabLaden(eintrag, protokoll) { return vorab }

        let argumente = eintrag.updateArgumente ?? "update"
        protokoll.berichte("\((exe as NSString).lastPathComponent) \(argumente)")

        // npm sucht seine Module relativ zum Arbeitsverzeichnis (MODULE_NOT_FOUND); der eigene
        // Ordner des Programms ist immer richtig und fuer jedes andere Werkzeug harmlos.
        // npm-Werkzeuge: Ruhezeit statt festem Limit, falls der Lauf doch selbst laden muss.
        var lauf = await Kommandozeile.ausfuehren(exe, Kommandozeile.zerlegen(argumente),
                                                  zeitlimit: TimeInterval(eintrag.zeitlimitMinuten * 60),
                                                  arbeitsverzeichnis: (exe as NSString).deletingLastPathComponent,
                                                  lebenszeichen: npmGlobal ? NpmGlobal.downloadGroesse : nil)
        protokoll.berichte(lauf.ausgabe)

        // Das Werkzeug meldet, dass noch ein anderes Update laeuft. Nur fuer Werkzeuge, die sich
        // selbst aktualisieren: ihre verwaisten Reste sind ein haengengebliebener frueherer Lauf.
        var sperreZuJung = false
        if Self.istUpdateSperre(lauf), Self.gleicheDatei(exe, Pfade.aufloesen(eintrag.exePfadWirksam)) {
            let reste = await Self.verwaisteBeenden(exe, protokoll)
            sperreZuJung = reste.beendet == 0 && reste.zuJung > 0
            if reste.beendet > 0 {
                protokoll.berichte("Neuer Versuch nach dem Beenden des hängengebliebenen Laufs …")
                lauf = await Kommandozeile.ausfuehren(exe, Kommandozeile.zerlegen(argumente),
                                                      zeitlimit: TimeInterval(eintrag.zeitlimitMinuten * 60),
                                                      arbeitsverzeichnis: (exe as NSString).deletingLastPathComponent,
                                                      lebenszeichen: npmGlobal ? NpmGlobal.downloadGroesse : nil)
                protokoll.berichte(lauf.ausgabe)
            }
        }

        // Ein eben gestarteter frueherer Lauf haelt die Sperre: er wird von selbst fertig. Kein Code-Problem,
        // deshalb eigene Meldung ohne automatische Claude-Uebergabe.
        if sperreZuJung, Self.istUpdateSperre(lauf) {
            return PruefErgebnis(zustand: .fehler,
                                 meldung: Self.laeuftNochMeldung + "Ein eben gestartetes Update von \(eintrag.name) läuft noch. "
                                    + "In ein paar Minuten erneut versuchen.",
                                 protokoll: lauf.ausgabe)
        }

        // npm hat das alte Paket entfernt, und der Lauf endete, bevor das neue da war.
        // Liegt es jetzt an einem anderen Katalog-Pfad (Umzug auf den nativen Installer), ist nichts zerstoert.
        if npmGlobal, !FileManager.default.fileExists(atPath: programmDatei), !eintrag.aufDiesemRechnerVorhanden,
           await NpmGlobal.wiederherstellen(eintrag, programmDatei, protokoll) {
            return PruefErgebnis(zustand: .fertig, meldung: "Update abgeschlossen, nachdem npm die Installation unterbrochen hatte.",
                                 protokoll: lauf.ausgabe)
        }

        // Nur wenn das Werkzeug selbst aufgab, nicht wenn das Zeitlimit der App den Lauf beendete.
        let downloadFrist = !lauf.abgelaufen && Self.istDownloadFrist(lauf)
        let langsamMeldung = Self.langsamesNetzMeldung + eintrag.name + " hat seinen eigenen Download wegen Zeitüberschreitung abgebrochen. "
            + "Die Verbindung ist gerade zu langsam für die neue Version – in einem schnelleren Netz erneut versuchen. "
            + "Die installierte Version bleibt unverändert nutzbar."

        // Eigener Download gescheitert oder haengengeblieben: die Datei aus dem Manifest selbst laden
        // (IPv4 zuerst). Nicht, solange ein anderer Lauf des Werkzeugs seine Update-Sperre haelt.
        if eintrag.ersatzDownload, !npmGlobal, !Self.istUpdateSperre(lauf),
           AbbruchMarke.aktuell?.istAbgebrochen != true,
           lauf.abgelaufen || Self.istDownloadAbbruch(lauf) || downloadFrist,
           Self.gleicheDatei(exe, programmDatei),
           let ersatz = await Self.ersatzDownload(eintrag, programmDatei, protokoll) {
            // Half auch der Ersatz nicht, bleibt es bei der langsamen Leitung: keine Claude-Uebergabe dafuer.
            if ersatz.zustand == .fehler, downloadFrist, FileManager.default.fileExists(atPath: programmDatei) {
                return PruefErgebnis(zustand: .fehler, meldung: langsamMeldung + " " + ersatz.meldung,
                                     protokoll: lauf.ausgabe + "\n" + ersatz.protokoll)
            }
            return PruefErgebnis(zustand: ersatz.zustand, meldung: ersatz.meldung,
                                 protokoll: lauf.ausgabe + "\n" + ersatz.protokoll)
        }

        // Die eigene Gesamtfrist des Werkzeugs fuer den Download ist abgelaufen (Claude Code: ~10 min fuer
        // ~300 MB) und der Ersatzdownload half nicht. Kein Code-Problem: eigene Meldung, keine Claude-Uebergabe.
        // Nur solange es stimmt: "bleibt nutzbar" darf nicht ueber einem von npm entfernten Paket stehen.
        if downloadFrist, FileManager.default.fileExists(atPath: programmDatei) {
            return PruefErgebnis(zustand: .fehler, meldung: langsamMeldung, protokoll: lauf.ausgabe)
        }

        if lauf.abgelaufen {
            return PruefErgebnis(zustand: .fehler, meldung: "Zeitlimit überschritten.", protokoll: lauf.ausgabe)
        }
        if lauf.exitCode != 0 {
            return PruefErgebnis(zustand: .fehler, meldung: "Endete mit Code \(lauf.exitCode).",
                                 protokoll: lauf.ausgabe)
        }
        return PruefErgebnis(zustand: .fertig, meldung: "Update abgeschlossen.", protokoll: lauf.ausgabe)
    }

    /// Die Version, wo das Werkzeug eine meldet. Bei Eintraegen ohne Versionsabfrage ist der
    /// Fingerabdruck der Trockenlauf-Plan: nach einem erfolgreichen Update muss er leer sein. Eine
    /// gescheiterte Versionsabfrage ergibt "" (unbekannt) und faellt nicht auf den Plan zurueck:
    /// die beiden Werte waeren verschiedener Art und wuerden immer als Aenderung gelten.
    func fingerabdruck(_ eintrag: ProgrammEintrag) async -> String {
        let exe = Pfade.aufloesen(eintrag.exePfadWirksam)
        guard FileManager.default.isExecutableFile(atPath: exe) else { return "" }

        if let argumente = eintrag.versionsArgumente, !argumente.istLeer {
            let lauf = await Kommandozeile.ausfuehren(exe, argumente, zeitlimit: 120)
            if lauf.abgelaufen || lauf.exitCode != 0 { return "" }
            return Self.versionAus(lauf.ausgabe, eintrag.versionsMuster)
        }

        if let pruefArgumente = eintrag.pruefArgumente, !pruefArgumente.istLeer {
            let lauf = await Kommandozeile.ausfuehren(exe, pruefArgumente, zeitlimit: 480)
            if lauf.abgelaufen || !Self.pruefCodeGueltig(eintrag, lauf.exitCode) { return "" }
            let geplant = Self.geplanteZeilen(lauf.ausgabe, eintrag)
            return geplant.isEmpty ? "nichts offen" : geplant.joined(separator: " | ")
        }

        return ""
    }

    // MARK: - Intern

    /// Ein Lauf, der abgewiesen wurde, weil noch ein anderes Update desselben Werkzeugs laeuft.
    static func istUpdateSperre(_ lauf: BefehlErgebnis) -> Bool {
        !lauf.abgelaufen && lauf.exitCode != 0 && lauf.ausgabe.localizedCaseInsensitiveContains("already in progress")
    }

    static func gleicheDatei(_ a: String, _ b: String) -> Bool {
        !a.isEmpty && !b.isEmpty
            && (a as NSString).resolvingSymlinksInPath == (b as NSString).resolvingSymlinksInPath
    }

    /// Beendet Reste eines frueheren Update-Laufs derselben Programmdatei. Ein haengengebliebener
    /// Lauf (Antigravitys Download ohne Verbindung) ueberlebt die App, die ihn gestartet hat; er
    /// haelt seine Update-Sperre offen, und jedes spaetere Update endet sofort mit "update already
    /// in progress". Nur Waisen zaehlen (Elternprozess ist launchd), die mindestens zwei Minuten
    /// laufen: eine interaktive Sitzung hat ihr Terminal als Elternteil, ein gerade gestartetes
    /// Update darf fertig werden. Gerufen nur, nachdem das Werkzeug selbst eine laufende
    /// Aktualisierung gemeldet hat.
    /// - Returns: wie viele Reste wirklich beendet sind und wie viele als zu jung stehen blieben.
    static func verwaisteBeenden(_ exe: String, _ protokoll: Fortschritt) async -> (beendet: Int, zuJung: Int) {
        await withCheckedContinuation { fortsetzung in
            // Eigener Thread: Prozesstabelle, pgrep und das Warten blockieren.
            DispatchQueue.global(qos: .userInitiated).async {
                fortsetzung.resume(returning: verwaisteBeendenBlockierend(exe, protokoll))
            }
        }
    }

    private static func verwaisteBeendenBlockierend(_ exe: String, _ protokoll: Fortschritt) -> (beendet: Int, zuJung: Int) {
        let ziel = (exe as NSString).resolvingSymlinksInPath
        var pids = [pid_t](repeating: 0, count: 8192)
        let anzahl = Int(proc_listallpids(&pids, Int32(pids.count * MemoryLayout<pid_t>.size)))
        var beendet = 0
        var zuJung = 0
        // Ueber den Programmpfad statt den Namen: Claudes nativer Installer laeuft als "2.1.x".
        for pid in pids.prefix(max(0, anzahl)) where pid > 1 && pid != getpid() {
            guard (Prozessdienst.pfadVon(pid) as NSString).resolvingSymlinksInPath == ziel,
                  let info = bsdInfo(pid), info.pbi_ppid == 1 else { continue }
            let start = Date(timeIntervalSince1970: TimeInterval(info.pbi_start_tvsec))
            guard Date().timeIntervalSince(start) >= 120 else {
                protokoll.berichte("Verwaister \((exe as NSString).lastPathComponent)-Prozess \(pid) läuft erst seit "
                                   + Formate.uhrzeit(start) + " – bleibt bestehen, das Update bitte in ein paar Minuten erneut versuchen.")
                zuJung += 1
                continue
            }
            protokoll.berichte("Verwaister \((exe as NSString).lastPathComponent)-Prozess \(pid) (gestartet "
                               + Formate.zeitpunkt(start) + ") hält das Update auf – wird beendet.")
            // Auch die Nachfahren koennen die Sperre halten. Erst freundlich, dann hart.
            let baum = [pid] + Kommandozeile.prozessbaum(pid)
            func lebt() -> Bool { baum.contains { kill($0, 0) == 0 } }
            func warten(_ sekunden: TimeInterval) {
                let ende = Date().addingTimeInterval(sekunden)
                while Date() < ende && lebt() { Thread.sleep(forTimeInterval: 0.1) }
            }
            for p in baum { kill(p, SIGTERM) }
            warten(2)
            for p in baum { kill(p, SIGKILL) }
            warten(5)
            if lebt() {
                protokoll.berichte("Prozess \(pid) ließ sich nicht vollständig beenden.")
            } else {
                beendet += 1
            }
        }
        return (beendet, zuJung)
    }

    private static func bsdInfo(_ pid: pid_t) -> proc_bsdinfo? {
        var info = proc_bsdinfo()
        let groesse = Int32(MemoryLayout<proc_bsdinfo>.size)
        return proc_pidinfo(pid, PROC_PIDTBSDINFO, 0, &info, groesse) == groesse ? info : nil
    }


    /// Das Programm fuer den Update-Lauf: `updateExePfad`, sonst dasselbe wie fuer die Version.
    static func updateExe(_ eintrag: ProgrammEintrag) -> String {
        if let pfad = eintrag.updateExePfad, !pfad.istLeer { return pfad }
        return eintrag.exePfadWirksam
    }

    /// Exit-Codes, die trotzdem eine gueltige Antwort tragen (rustup check: 100 = Updates offen).
    static func pruefCodeGueltig(_ eintrag: ProgrammEintrag, _ exitCode: Int) -> Bool {
        exitCode == 0 || eintrag.pruefExitCodes.contains(exitCode)
    }

    /// Die geplanten Updates einer Pruefung: standardmaessig Zeilen mit Pfeil ("alt → neu"), oder --
    /// wenn der Katalog sie nennt -- Zeilen nach einer Abschnittsmarke, die einem Muster folgen.
    static func geplanteZeilen(_ ausgabe: String, _ eintrag: ProgrammEintrag) -> [String] {
        var zeilen = ausgabe
            .replacingOccurrences(of: "\r", with: "")
            .split(separator: "\n", omittingEmptySubsequences: false)
            .map(String.init)

        if let marke = eintrag.pruefAbschnitt, !marke.istLeer {
            // Kein Abschnitt heisst nichts offen -- sdkmanager laesst "Available Updates" dann weg.
            if let start = zeilen.firstIndex(where: { $0.localizedCaseInsensitiveContains(marke) }) {
                zeilen = Array(zeilen[(start + 1)...])
            } else {
                zeilen = []
            }
        }

        let passt: (String) -> Bool
        if let muster = eintrag.pruefZeilenMuster, !muster.istLeer,
           let regex = try? NSRegularExpression(pattern: muster) {
            passt = { regex.firstMatch(in: $0, range: NSRange($0.startIndex..., in: $0)) != nil }
        } else {
            passt = { $0.contains("→") || $0.contains("->") }
        }

        return zeilen.filter(passt).map { $0.trimmed }.filter { !$0.isEmpty }
    }

    /// Die Version in einem Text: Gruppe 1 des Katalog-Musters, wenn eines gesetzt ist (zweistellige
    /// Versionen wie Gradles "9.7"), sonst das erste "x.y.z". Ein ungueltiges Muster ergibt "" (unbekannt).
    static func versionAus(_ text: String, _ muster: String?) -> String {
        guard let muster, !muster.istLeer else { return Versionen.ausText(text) }
        guard let regex = try? NSRegularExpression(pattern: muster, options: [.anchorsMatchLines]),
              let treffer = regex.firstMatch(in: text, range: NSRange(text.startIndex..., in: text)) else { return "" }
        let bereich = treffer.numberOfRanges > 1 ? treffer.range(at: 1) : treffer.range
        guard let r = Range(bereich, in: text) else { return "" }
        return String(text[r])
    }

    /// Hinweise, dass der eigene Download des Werkzeugs abriss (Go: agy). Nur dann lohnt der Ersatzweg.
    static let downloadAbbruchHinweise = ["download interrupted", "while reading body", "wsarecv", "connection reset",
                                          "timed out", "timeout", "deadline exceeded", "network is unreachable"]

    /// Anfang der Fehlermeldung zu `istDownloadFrist`; haelt sie aus der automatischen Claude-Uebergabe.
    static let langsamesNetzMeldung = "Download zu langsam: "

    /// Anfang der Meldung, wenn ein eben gestarteter frueherer Lauf die Update-Sperre haelt; ebenfalls ohne Claude-Uebergabe.
    static let laeuftNochMeldung = "Update läuft noch: "

    /// Die eigene Gesamtfrist des Werkzeugs lief ab. Kein kurzer Aussetzer: auf derselben langsamen
    /// Leitung kostet jeder weitere Versuch wieder die volle Frist (Windows 1.3.51).
    static func istDownloadFrist(_ lauf: BefehlErgebnis) -> Bool {
        lauf.exitCode != 0
            && (lauf.ausgabe.localizedCaseInsensitiveContains("exceeded the total deadline")
                || lauf.ausgabe.localizedCaseInsensitiveContains("Download timed out"))
    }

    static func istDownloadAbbruch(_ lauf: BefehlErgebnis) -> Bool {
        lauf.exitCode != 0 && downloadAbbruchHinweise.contains { lauf.ausgabe.lowercased().contains($0) }
    }

    /// Version, Download-Adresse und Pruefsumme aus einem Auto-Updater-Manifest; nil, wenn eins fehlt.
    static func manifestAuswerten(_ daten: Data) -> (version: String, url: String, algorithmus: String, pruefsumme: String)? {
        guard let objekt = (try? JSONSerialization.jsonObject(with: daten)) as? [String: Any],
              let url = objekt["url"] as? String, url.lowercased().hasPrefix("https://"),
              let version = objekt["version"] as? String, !version.istLeer else { return nil }
        if let sha = objekt["sha512"] as? String, sha.count == 128 { return (version, url, "sha512", sha.lowercased()) }
        if let sha = objekt["sha256"] as? String, sha.count == 64 { return (version, url, "sha256", sha.lowercased()) }
        return nil   // nie eine Datei einsetzen, die sich nicht pruefen laesst
    }

    private static func pruefsumme(_ pfad: String, _ algorithmus: String) -> String? {
        guard let daten = try? Data(contentsOf: URL(fileURLWithPath: pfad), options: .mappedIfSafe) else { return nil }
        let summe: [UInt8] = algorithmus == "sha512" ? Array(SHA512.hash(data: daten)) : Array(SHA256.hash(data: daten))
        return summe.map { String(format: "%02x", $0) }.joined()
    }

    /// Laedt die Programmdatei aus dem Manifest per curl -4, prueft die Pruefsumme und setzt sie ein:
    /// die alte Datei wird umbenannt, die neue an ihren Platz gelegt, und "--version" muss antworten,
    /// sonst kommt die alte zurueck. Ein Symlink (nativer Installer) wird aufgeloest, ersetzt wird sein
    /// Ziel. nil, wenn es kein brauchbares Manifest oder keine neuere Version gibt.
    static func ersatzDownload(_ eintrag: ProgrammEintrag, _ programm: String,
                               _ protokoll: Fortschritt) async -> PruefErgebnis? {
        guard let adresse = eintrag.versionsUrl, !adresse.istLeer else { return nil }
        if AbbruchMarke.aktuell?.istAbgebrochen == true { return nil }
        let exe = (programm as NSString).resolvingSymlinksInPath
        protokoll.berichte("Eigener Download gescheitert – die UpdaterZentrale lädt die neue Version selbst (IPv4) …")

        // Auch das Manifest per curl -4: URLSession nimmt die IPv6-Route, an der der Download gerade riss.
        let abruf = await Kommandozeile.ausfuehren("/usr/bin/curl", ["-4", "-sS", "-fL", "--max-time", "30", adresse], zeitlimit: 60)
        if abruf.abgelaufen || abruf.exitCode != 0 {
            protokoll.berichte("Manifest nicht lesbar (curl Code \(abruf.exitCode)): \(abruf.ausgabe)")
            return nil
        }
        guard let m = manifestAuswerten(Data(abruf.ausgabe.utf8)) else {
            protokoll.berichte("Das Manifest nennt keine Version mit Download-Adresse und Prüfsumme.")
            return nil
        }

        // Nur echt Neueres: ein gecachtes Manifest darf nie eine aeltere Version einsetzen.
        let vorher = await Kommandozeile.ausfuehren(exe, eintrag.versionsArgumente ?? "--version", zeitlimit: 120)
        let installiert = versionAus(vorher.ausgabe, eintrag.versionsMuster)
        guard !installiert.istLeer, Versionen.istNeuer(m.version, als: installiert) else {
            protokoll.berichte("Manifest nennt \(m.version), installiert ist \(installiert.istLeer ? "unbekannt" : installiert) – kein Ersatzdownload.")
            return nil
        }

        let fm = FileManager.default
        let neu = exe + ".download", alt = exe + ".alt"
        defer { try? fm.removeItem(atPath: neu) }
        protokoll.berichte("Lade \(m.url)")
        let laden = await Kommandozeile.ausfuehren("/usr/bin/curl", ["-4", "-sS", "-fL", "--retry", "2", "-o", neu, m.url],
                                                   zeitlimit: TimeInterval(eintrag.zeitlimitMinuten * 60),
                                                   lebenszeichen: { (try? FileManager.default.attributesOfItem(atPath: neu)[.size] as? Int64) ?? 0 })
        if AbbruchMarke.aktuell?.istAbgebrochen == true {
            return PruefErgebnis(zustand: .fehler, meldung: "Abgebrochen.", protokoll: laden.ausgabe)
        }
        guard laden.exitCode == 0, !laden.abgelaufen, fm.fileExists(atPath: neu) else {
            return PruefErgebnis(zustand: .fehler,
                                 meldung: "Auch der Ersatzdownload per IPv4 scheiterte (curl Code \(laden.exitCode)).",
                                 protokoll: laden.ausgabe)
        }

        guard pruefsumme(neu, m.algorithmus) == m.pruefsumme else {
            return PruefErgebnis(zustand: .fehler,
                                 meldung: "Ersatzdownload verworfen: die \(m.algorithmus.uppercased())-Prüfsumme passt nicht zum Manifest.")
        }
        protokoll.berichte("\(m.algorithmus.uppercased())-Prüfsumme stimmt.")

        // Das macOS-Manifest liefert ein Archiv (cli_mac_arm64.tar.gz): auspacken und die Programmdatei
        // gleichen Namens nehmen, sonst die einzige Datei darin.
        let pfad = m.url.lowercased().split(separator: "?").first.map(String.init) ?? ""
        if pfad.hasSuffix(".tar.gz") || pfad.hasSuffix(".tgz") {
            let ordner = exe + ".entpackt"
            defer { try? fm.removeItem(atPath: ordner) }
            try? fm.removeItem(atPath: ordner)
            try? fm.createDirectory(atPath: ordner, withIntermediateDirectories: true)
            let entpacken = await Kommandozeile.ausfuehren("/usr/bin/tar", ["-xzf", neu, "-C", ordner], zeitlimit: 300)
            let dateien = (fm.enumerator(atPath: ordner)?.allObjects as? [String] ?? [])
                .filter { var istOrdner: ObjCBool = false
                          return fm.fileExists(atPath: ordner + "/" + $0, isDirectory: &istOrdner) && !istOrdner.boolValue }
            let namen = [(exe as NSString).lastPathComponent, (programm as NSString).lastPathComponent]
            guard entpacken.exitCode == 0,
                  let treffer = dateien.first(where: { namen.contains(($0 as NSString).lastPathComponent) })
                                ?? (dateien.count == 1 ? dateien[0] : nil) else {
                return PruefErgebnis(zustand: .fehler,
                                     meldung: "Ersatzdownload: im Archiv fand sich keine Datei \(namen[0]).",
                                     protokoll: entpacken.ausgabe + "\n" + dateien.joined(separator: "\n"))
            }
            try? fm.removeItem(atPath: neu)
            do { try fm.moveItem(atPath: ordner + "/" + treffer, toPath: neu) } catch {
                return PruefErgebnis(zustand: .fehler, meldung: "Ersatzdownload ließ sich nicht auspacken: \(error.localizedDescription)")
            }
        }

        do {
            try? fm.removeItem(atPath: alt)
            try fm.setAttributes([.posixPermissions: 0o755], ofItemAtPath: neu)
            try fm.moveItem(atPath: exe, toPath: alt)
            do { try fm.moveItem(atPath: neu, toPath: exe) } catch { try? fm.moveItem(atPath: alt, toPath: exe); throw error }
        } catch {
            return PruefErgebnis(zustand: .fehler, meldung: "Ersatzdownload ließ sich nicht einsetzen: \(error.localizedDescription)")
        }

        let probe = await Kommandozeile.ausfuehren(exe, eintrag.versionsArgumente ?? "--version", zeitlimit: 120)
        if probe.abgelaufen || probe.exitCode != 0 {
            try? fm.removeItem(atPath: exe)
            try? fm.moveItem(atPath: alt, toPath: exe)
            return PruefErgebnis(zustand: .fehler,
                                 meldung: "Die geladene Version startete nicht (Code \(probe.exitCode)) – die alte bleibt installiert.",
                                 protokoll: probe.ausgabe)
        }
        try? fm.removeItem(atPath: alt)
        protokoll.berichte("Neue Datei eingesetzt: \(probe.ausgabe.trimmingCharacters(in: .whitespacesAndNewlines))")
        return PruefErgebnis(zustand: .fertig, meldung: "Update per Ersatzdownload (IPv4) abgeschlossen.", protokoll: probe.ausgabe)
    }

    /// Fragt die Version direkt per HTTPS ab statt ueber "npm view": das braucht weder node noch
    /// ein bestimmtes Arbeitsverzeichnis (der npm-Shim sucht seine Module relativ zum
    /// Arbeitsverzeichnis und bricht sonst mit MODULE_NOT_FOUND ab). `json` liest das Feld "version"
    /// der npm-Registry, sonst zaehlt die erste Versionsnummer im Klartext.
    static func netzVersion(_ adresse: String, json: Bool, _ protokoll: Fortschritt,
                            muster: String? = nil) async -> (version: String?, problem: String?) {
        guard let url = URL(string: adresse) else {
            return (nil, "Ungültige Versionsadresse: \(adresse)")
        }
        var anfrage = URLRequest(url: url)
        anfrage.timeoutInterval = 30
        // Die GitHub-API (Releases) weist Anfragen ohne User-Agent ab.
        anfrage.setValue("UpdaterZentrale/1.0", forHTTPHeaderField: "User-Agent")
        let ergebnis: (version: String?, problem: String?)
        do {
            let (daten, antwort) = try await URLSession.shared.data(for: anfrage)
            let code = (antwort as? HTTPURLResponse)?.statusCode ?? 0
            if !(200...299).contains(code) {
                ergebnis = (nil, "Versionsabfrage fehlgeschlagen: HTTP \(code).")
            } else {
                var text = String(decoding: daten, as: UTF8.self)
                if json {
                    let objekt = (try? JSONSerialization.jsonObject(with: daten)) as? [String: Any]
                    text = objekt?["version"] as? String ?? ""
                }
                let version = versionAus(text, muster)
                ergebnis = version.istLeer ? (nil, "Die Versionsadresse lieferte keine Versionsnummer.") : (version, nil)
            }
        } catch {
            ergebnis = (nil, "Versionsabfrage fehlgeschlagen: \(error.localizedDescription)")
        }
        protokoll.berichte("\(adresse) -> \(ergebnis.version ?? ergebnis.problem ?? "")")
        return ergebnis
    }
}
