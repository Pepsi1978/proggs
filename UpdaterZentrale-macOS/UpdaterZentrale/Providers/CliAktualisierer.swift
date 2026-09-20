import Foundation

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
        let exe = Pfade.aufloesen(eintrag.exePfad)
        guard FileManager.default.isExecutableFile(atPath: exe) else {
            return PruefErgebnis(zustand: .nichtInstalliert, meldung: "Nicht gefunden: \(exe)")
        }

        var installiert = ""
        if let argumente = eintrag.versionsArgumente, !argumente.istLeer {
            let lauf = await Kommandozeile.ausfuehren(exe, argumente, zeitlimit: 120)
            installiert = Versionen.ausText(lauf.ausgabe)
            protokoll.berichte("\((exe as NSString).lastPathComponent) \(argumente) -> \(lauf.ausgabe)")
        }

        // Weg 1: das Werkzeug sagt selbst, was es aktualisieren wuerde.
        if let pruefArgumente = eintrag.pruefArgumente, !pruefArgumente.istLeer {
            protokoll.berichte("\((exe as NSString).lastPathComponent) \(pruefArgumente)")
            let lauf = await Kommandozeile.ausfuehren(exe, pruefArgumente, zeitlimit: 360)
            protokoll.berichte(lauf.ausgabe)

            let geplant = geplanteZeilen(lauf.ausgabe)
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
        if let paket = eintrag.npmPaket, !paket.istLeer {
            let verfuegbar = await npmVersion(paket, protokoll)
            if !verfuegbar.istLeer {
                let neuer = Versionen.istNeuer(verfuegbar, als: installiert)
                return PruefErgebnis(zustand: neuer ? .updateVerfuegbar : .aktuell,
                                     installierteVersion: installiert,
                                     verfuegbareVersion: verfuegbar,
                                     meldung: neuer ? "Neue Version \(verfuegbar) verfügbar." : "Auf dem neuesten Stand.")
            }
        }

        return PruefErgebnis(zustand: .unbekannt, installierteVersion: installiert,
                             meldung: "Keine Prüfquelle hinterlegt – das Update lässt sich trotzdem auslösen.")
    }

    func aktualisieren(_ eintrag: ProgrammEintrag, _ protokoll: Fortschritt) async -> PruefErgebnis {
        let exe = Pfade.aufloesen(eintrag.exePfad)
        guard FileManager.default.isExecutableFile(atPath: exe) else {
            return PruefErgebnis(zustand: .nichtInstalliert, meldung: "Nicht gefunden: \(exe)")
        }

        let argumente = eintrag.updateArgumente ?? "update"
        protokoll.berichte("\((exe as NSString).lastPathComponent) \(argumente)")

        let lauf = await Kommandozeile.ausfuehren(exe, argumente,
                                                  zeitlimit: TimeInterval(eintrag.zeitlimitMinuten * 60))
        protokoll.berichte(lauf.ausgabe)

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
        let exe = Pfade.aufloesen(eintrag.exePfad)
        guard FileManager.default.isExecutableFile(atPath: exe) else { return "" }

        if let argumente = eintrag.versionsArgumente, !argumente.istLeer {
            let lauf = await Kommandozeile.ausfuehren(exe, argumente, zeitlimit: 120)
            let version = Versionen.ausText(lauf.ausgabe)
            if !version.istLeer { return version }
        }

        if let pruefArgumente = eintrag.pruefArgumente, !pruefArgumente.istLeer {
            let lauf = await Kommandozeile.ausfuehren(exe, pruefArgumente, zeitlimit: 480)
            let geplant = geplanteZeilen(lauf.ausgabe)
            return geplant.isEmpty ? "nichts offen" : geplant.joined(separator: " | ")
        }

        return ""
    }

    // MARK: - Intern

    /// Zeilen, die eine geplante Aktualisierung als "alt → neu" ausweisen.
    private func geplanteZeilen(_ ausgabe: String) -> [String] {
        ausgabe
            .split(separator: "\n", omittingEmptySubsequences: false)
            .map(String.init)
            .filter { $0.contains("→") || $0.contains("->") }
            .map { $0.trimmed }
            .filter { !$0.isEmpty }
    }

    private func npmVersion(_ paket: String, _ protokoll: Fortschritt) async -> String {
        guard FileManager.default.isExecutableFile(atPath: Pfade.npm) else {
            protokoll.berichte("npm wurde nicht gefunden (\(Pfade.npm)) – npm-Prüfung übersprungen.")
            return ""
        }
        let lauf = await Kommandozeile.ausfuehren(Pfade.npm, ["view", paket, "version"], zeitlimit: 120)
        protokoll.berichte("npm view \(paket) version -> \(lauf.ausgabe)")
        return Versionen.ausText(lauf.ausgabe)
    }
}
