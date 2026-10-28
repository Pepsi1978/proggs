import Foundation
import Darwin

/// Kommandozeilen-Werkzeuge, die sich selbst aktualisieren ("claude update", "codex update",
/// "lms runtime update"). 1:1-Port von Providers/CliAktualisierer.cs.
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

        let argumente = eintrag.updateArgumente ?? "update"
        protokoll.berichte("\((exe as NSString).lastPathComponent) \(argumente)")

        // npm sucht seine Module relativ zum Arbeitsverzeichnis (MODULE_NOT_FOUND); der eigene
        // Ordner des Programms ist immer richtig und fuer jedes andere Werkzeug harmlos.
        var lauf = await Kommandozeile.ausfuehren(exe, argumente,
                                                  zeitlimit: TimeInterval(eintrag.zeitlimitMinuten * 60),
                                                  arbeitsverzeichnis: (exe as NSString).deletingLastPathComponent)
        protokoll.berichte(lauf.ausgabe)

        // Das Werkzeug meldet, dass noch ein anderes Update laeuft. Nur fuer Werkzeuge, die sich
        // selbst aktualisieren: ihre verwaisten Reste sind ein haengengebliebener frueherer Lauf.
        if Self.istUpdateSperre(lauf),
           Self.gleicheDatei(exe, Pfade.aufloesen(eintrag.exePfadWirksam)),
           await Self.verwaisteBeenden(exe, protokoll) > 0 {
            protokoll.berichte("Neuer Versuch nach dem Beenden des hängengebliebenen Laufs …")
            lauf = await Kommandozeile.ausfuehren(exe, argumente,
                                                  zeitlimit: TimeInterval(eintrag.zeitlimitMinuten * 60),
                                                  arbeitsverzeichnis: (exe as NSString).deletingLastPathComponent)
            protokoll.berichte(lauf.ausgabe)
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

    /// Die Version, wo das Werkzeug eine meldet. Die LM-Studio-Runtimes haben keine -- ihr
    /// Fingerabdruck ist stattdessen der Trockenlauf-Plan: nach einem erfolgreichen Update muss
    /// er leer sein.
    func fingerabdruck(_ eintrag: ProgrammEintrag) async -> String {
        let exe = Pfade.aufloesen(eintrag.exePfadWirksam)
        guard FileManager.default.isExecutableFile(atPath: exe) else { return "" }

        if let argumente = eintrag.versionsArgumente, !argumente.istLeer {
            let lauf = await Kommandozeile.ausfuehren(exe, argumente, zeitlimit: 120)
            let version = Self.versionAus(lauf.ausgabe, eintrag.versionsMuster)
            if !version.istLeer { return version }
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
    /// - Returns: wie viele Reste wirklich beendet sind.
    static func verwaisteBeenden(_ exe: String, _ protokoll: Fortschritt) async -> Int {
        await withCheckedContinuation { fortsetzung in
            // Eigener Thread: Prozesstabelle, pgrep und das Warten blockieren.
            DispatchQueue.global(qos: .userInitiated).async {
                fortsetzung.resume(returning: verwaisteBeendenBlockierend(exe, protokoll))
            }
        }
    }

    private static func verwaisteBeendenBlockierend(_ exe: String, _ protokoll: Fortschritt) -> Int {
        let ziel = (exe as NSString).resolvingSymlinksInPath
        var pids = [pid_t](repeating: 0, count: 8192)
        let anzahl = Int(proc_listallpids(&pids, Int32(pids.count * MemoryLayout<pid_t>.size)))
        var beendet = 0
        // Ueber den Programmpfad statt den Namen: Claudes nativer Installer laeuft als "2.1.x".
        for pid in pids.prefix(max(0, anzahl)) where pid > 1 && pid != getpid() {
            guard (Prozessdienst.pfadVon(pid) as NSString).resolvingSymlinksInPath == ziel,
                  let info = bsdInfo(pid), info.pbi_ppid == 1 else { continue }
            let start = Date(timeIntervalSince1970: TimeInterval(info.pbi_start_tvsec))
            guard Date().timeIntervalSince(start) >= 120 else { continue }
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
                protokoll.berichte("Prozess \(pid) ließ sich nicht beenden.")
            } else {
                beendet += 1
            }
        }
        return beendet
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
