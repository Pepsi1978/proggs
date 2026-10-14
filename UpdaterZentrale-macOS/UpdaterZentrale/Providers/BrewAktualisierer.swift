import Foundation

/// Grafische Programme, die auf dem Mac ueber ein Homebrew-Cask kommen.
/// Gegenstueck zu Providers/WingetAktualisierer.cs UND Providers/StoreAktualisierer.cs -- auf dem
/// Mac gibt es fuer beide nur diesen einen Weg (siehe PORTING.md).
///
/// Drei Dinge laufen hier bewusst anders als unter Windows:
///
/// 1. **Die installierte Version kommt NIE von brew, sondern aus der Info.plist des Programms.**
///    Keine der verwalteten Apps wurde ueber brew installiert (`installed` ist bei allen null) --
///    sie bringen ihren eigenen Updater mit. Wuerde hier `brew list` befragt, stuende bei jeder
///    Karte "nicht installiert", obwohl die App da ist.
///
/// 2. **Ein Cask kann der App HINTERHERLAUFEN.** winget kennt nur "Upgrade steht an"; ein Cask ist
///    dagegen eine unabhaengige Rezeptdatei, die aelter sein kann als das, was der eingebaute
///    Updater der App schon installiert hat. Deshalb wird numerisch verglichen (Versionen.istNeuer)
///    statt auf Ungleichheit geprueft -- sonst waere ein Downgrade als "Update" angeboten worden.
///
/// 3. **Die Cask-Version traegt ein Komma-Anhaengsel** ("2.2553.1,c38127e2…"), das die Info.plist
///    nicht kennt. Ohne das Abschneiden waere jede Karte dauerhaft falsch als veraltet markiert.
struct BrewAktualisierer: Aktualisierer {
    let art = "brew"

    // MARK: - Pruefen

    func pruefen(_ eintrag: ProgrammEintrag, _ protokoll: Fortschritt) async -> PruefErgebnis {
        if let formel = eintrag.formel, !formel.istLeer {
            return await formelPruefen(formel, protokoll)
        }

        let appPfad = Pfade.aufloesen(eintrag.appPfad)
        let installiert = await installierteVersion(eintrag)

        if installiert.istLeer {
            return PruefErgebnis(zustand: .nichtInstalliert,
                                 meldung: appPfad.isEmpty
                                    ? "Kein Programmpfad hinterlegt."
                                    : "Nicht installiert: \(appPfad)")
        }
        protokoll.berichte("Installiert: \(installiert)  (\(appPfad))")

        guard let cask = eintrag.cask, !cask.istLeer else {
            return PruefErgebnis(zustand: .unbekannt, installierteVersion: installiert,
                                 meldung: "Kein Homebrew-Cask hinterlegt – das Update lässt sich trotzdem nicht prüfen.")
        }
        guard FileManager.default.isExecutableFile(atPath: Pfade.brew) else {
            return PruefErgebnis(zustand: .unbekannt, installierteVersion: installiert,
                                 meldung: "Homebrew wurde nicht gefunden (\(Pfade.brew)).")
        }

        protokoll.berichte("brew info --cask \(cask) --json=v2")
        let lauf = await Kommandozeile.ausfuehren(
            Pfade.brew, ["info", "--cask", cask, "--json=v2"], zeitlimit: 180)

        if lauf.abgelaufen {
            return PruefErgebnis(zustand: .unbekannt, installierteVersion: installiert,
                                 meldung: "Zeitlimit beim Abfragen von Homebrew überschritten.",
                                 protokoll: lauf.ausgabe)
        }

        guard let info = caskInfo(lauf.ausgabe) else {
            // Der Cask-Name stimmt nicht oder die Ausgabe hat eine andere Form. "Aktuell" zu melden
            // waere hier eine Luege -- genau wie in der Windows-Fassung.
            return PruefErgebnis(zustand: .unbekannt, installierteVersion: installiert,
                                 meldung: "Homebrew kennt das Cask „\(cask)“ nicht oder lieferte keine lesbare Antwort.",
                                 protokoll: lauf.ausgabe)
        }

        let verfuegbar = Versionen.caskBereinigen(info.version)
        protokoll.berichte("Cask \(cask): \(info.version)"
                           + (info.version == verfuegbar ? "" : "  → verglichen wird \(verfuegbar)")
                           + (info.installiert == nil ? "  (nicht über Homebrew installiert)" : "  (über Homebrew installiert)"))

        if verfuegbar.istLeer {
            return PruefErgebnis(zustand: .unbekannt, installierteVersion: installiert,
                                 meldung: "Homebrew nennt für „\(cask)“ keine Version.",
                                 protokoll: lauf.ausgabe)
        }

        // Android Studio traegt in seiner Info.plist nur "2026.2", das Cask heisst "2026.2.1.8":
        // eine verkuerzte installierte Version ist die angebotene, kein ewig offenes Update
        // (Windows-Gegenstueck: `verfuegbar.StartsWith(installiert + ".")`, 1.3.22).
        if Versionen.istVerkuerzteForm(installiert, von: verfuegbar) {
            return PruefErgebnis(zustand: .aktuell, installierteVersion: installiert,
                                 meldung: "Auf dem neuesten Stand (\(verfuegbar)).", protokoll: lauf.ausgabe)
        }

        // Der Downgrade-Schutz: nur eine echt neuere Version ist ein Update.
        if Versionen.istNeuer(verfuegbar, als: installiert) {
            return PruefErgebnis(zustand: .updateVerfuegbar,
                                 installierteVersion: installiert,
                                 verfuegbareVersion: verfuegbar,
                                 meldung: "Neue Version \(verfuegbar) verfügbar.",
                                 protokoll: lauf.ausgabe)
        }

        // Cask ist aelter als das Installierte: die App hat sich selbst schon weiter aktualisiert.
        // Das ist kein Fehler, sondern der Normalfall bei Apps mit eigenem Updater.
        if Versionen.istNeuer(installiert, als: verfuegbar) {
            return PruefErgebnis(zustand: .aktuell,
                                 installierteVersion: installiert,
                                 meldung: "Auf dem neuesten Stand – neuer als das Homebrew-Rezept (\(verfuegbar)).",
                                 protokoll: lauf.ausgabe)
        }

        return PruefErgebnis(zustand: .aktuell, installierteVersion: installiert,
                             meldung: "Auf dem neuesten Stand.", protokoll: lauf.ausgabe)
    }

    // MARK: - Aktualisieren

    func aktualisieren(_ eintrag: ProgrammEintrag, _ protokoll: Fortschritt) async -> PruefErgebnis {
        if let formel = eintrag.formel, !formel.istLeer {
            return await formelAktualisieren(eintrag, formel, protokoll)
        }
        guard let cask = eintrag.cask, !cask.istLeer else {
            return PruefErgebnis(zustand: .fehler, meldung: "Kein Homebrew-Cask hinterlegt.")
        }
        guard FileManager.default.isExecutableFile(atPath: Pfade.brew) else {
            return PruefErgebnis(zustand: .fehler, meldung: "Homebrew wurde nicht gefunden (\(Pfade.brew)).")
        }

        // Vor dem Installieren den Katalog frisch holen: ohne das kennt brew ein gerade
        // veroeffentlichtes Update noch gar nicht (Gegenstueck zum `git fetch` der Repo-Skripte).
        protokoll.berichte("brew update --quiet")
        let auffrischen = await Kommandozeile.ausfuehren(Pfade.brew, ["update", "--quiet"], zeitlimit: 300)
        if !auffrischen.ausgabe.istLeer { protokoll.berichte(auffrischen.ausgabe) }

        // Ueber brew installierte Apps werden aktualisiert; alle anderen ersetzt `--force`. `--adopt`
        // waere der saubere Weg, scheitert aber, sobald die installierte Version von der des Casks
        // abweicht -- und genau das ist hier immer der Fall.
        let ueberBrew = await istUeberBrewInstalliert(cask)
        let argumente = ueberBrew
            ? ["upgrade", "--cask", cask]
            : ["install", "--cask", "--force", cask]

        protokoll.berichte("brew " + argumente.joined(separator: " "))
        let lauf = await Kommandozeile.ausfuehren(
            Pfade.brew, argumente, zeitlimit: TimeInterval(eintrag.zeitlimitMinuten * 60))
        protokoll.berichte(lauf.ausgabe)

        if lauf.abgelaufen {
            return PruefErgebnis(zustand: .fehler, meldung: "Zeitlimit überschritten.", protokoll: lauf.ausgabe)
        }

        if istBereitsAktuell(lauf.ausgabe) {
            let stand = await fingerabdruck(eintrag)
            return PruefErgebnis(zustand: .aktuell, installierteVersion: stand,
                                 meldung: "War bereits aktuell.", protokoll: lauf.ausgabe)
        }

        if lauf.exitCode != 0 {
            return PruefErgebnis(zustand: .fehler, meldung: fehlergrund(lauf.ausgabe, exitCode: lauf.exitCode),
                                 protokoll: lauf.ausgabe)
        }

        let neu = await fingerabdruck(eintrag)
        return PruefErgebnis(zustand: .fertig, installierteVersion: neu,
                             meldung: "Update installiert.", protokoll: lauf.ausgabe)
    }

    // MARK: - Fingerabdruck

    /// Die Version aus der Info.plist -- direkt vom Programm gelesen, ohne Netz und ohne brew.
    /// Formeln und Casks ohne App-Buendel: die installierte Version laut brew (ebenfalls ohne Netz).
    func fingerabdruck(_ eintrag: ProgrammEintrag) async -> String {
        if let formel = eintrag.formel, !formel.istLeer {
            return formelInfo(await infoLauf(["info", "--json=v2", formel]).ausgabe)?.installiert ?? ""
        }
        return await installierteVersion(eintrag)
    }

    /// Die installierte Version eines Casks -- fuer Pruefung UND Fingerabdruck dieselbe Lesart.
    ///
    /// Grundlage ist die Info.plist des Programms. Hat brew das Cask selbst installiert und kennt
    /// es eine LAENGERE Fassung derselben Version (Android Studio: Info.plist "2026.2", brew
    /// "2026.2.1.8"), zaehlt die laengere: sie unterscheidet Patch-Staende, die die Info.plist
    /// verschluckt, und bewegt sich bei jedem brew-Update -- sonst saehe ein Patch-Update
    /// (2026.2.1.8 → 2026.2.1.9) wie "nichts passiert" aus. Weicht brew ab, weil die App sich
    /// selbst weiter aktualisiert hat, bleibt die Info.plist die Wahrheit.
    /// Casks ohne App-Buendel (dotnet-sdk): die installierte Version kennt nur brew selbst.
    private func installierteVersion(_ eintrag: ProgrammEintrag) async -> String {
        let appPfad = Pfade.aufloesen(eintrag.appPfad)
        guard let cask = eintrag.cask, !cask.istLeer else {
            return appPfad.isEmpty ? "" : Pfade.appVersion(appPfad)
        }
        if appPfad.isEmpty { return await caskInstalliert(cask) }
        let plist = Pfade.appVersion(appPfad)
        if plist.istLeer { return "" }
        let brewStand = await caskInstalliert(cask)
        return Versionen.istVerkuerzteForm(plist, von: brewStand) ? brewStand : plist
    }

    // MARK: - Formeln

    /// Kommandozeilen-Werkzeuge aus Homebrew (git, node, go …). Anders als bei den Casks ist brew
    /// hier die Wahrheit fuer beide Seiten: es hat sie installiert, und es kennt die neue Version.
    private func formelPruefen(_ formel: String, _ protokoll: Fortschritt) async -> PruefErgebnis {
        guard FileManager.default.isExecutableFile(atPath: Pfade.brew) else {
            return PruefErgebnis(zustand: .unbekannt, meldung: "Homebrew wurde nicht gefunden (\(Pfade.brew)).")
        }
        protokoll.berichte("brew info --json=v2 \(formel)")
        let lauf = await infoLauf(["info", "--json=v2", formel])
        if lauf.abgelaufen {
            return PruefErgebnis(zustand: .unbekannt, meldung: "Zeitlimit beim Abfragen von Homebrew überschritten.",
                                 protokoll: lauf.ausgabe)
        }
        guard let info = formelInfo(lauf.ausgabe) else {
            return PruefErgebnis(zustand: .unbekannt,
                                 meldung: "Homebrew kennt die Formel „\(formel)“ nicht oder lieferte keine lesbare Antwort.",
                                 protokoll: lauf.ausgabe)
        }
        protokoll.berichte("Formel \(formel): installiert \(info.installiert.istLeer ? "–" : info.installiert), "
                           + "verfügbar \(info.verfuegbar)\(info.veraltet ? "  (veraltet)" : "")")

        if info.installiert.istLeer {
            return PruefErgebnis(zustand: .nichtInstalliert, verfuegbareVersion: info.verfuegbar,
                                 meldung: "Nicht über Homebrew installiert.", protokoll: lauf.ausgabe)
        }
        // brew selbst entscheidet "veraltet" (es kennt Revisionen wie 2.56.0_1); der Zahlenvergleich
        // faengt nur ab, wenn das Feld fehlt.
        if info.veraltet || Versionen.istNeuer(info.verfuegbar, als: info.installiert) {
            return PruefErgebnis(zustand: .updateVerfuegbar, installierteVersion: info.installiert,
                                 verfuegbareVersion: info.verfuegbar,
                                 meldung: "Neue Version \(info.verfuegbar) verfügbar.", protokoll: lauf.ausgabe)
        }
        return PruefErgebnis(zustand: .aktuell, installierteVersion: info.installiert,
                             meldung: "Auf dem neuesten Stand.", protokoll: lauf.ausgabe)
    }

    private func formelAktualisieren(_ eintrag: ProgrammEintrag, _ formel: String,
                                     _ protokoll: Fortschritt) async -> PruefErgebnis {
        guard FileManager.default.isExecutableFile(atPath: Pfade.brew) else {
            return PruefErgebnis(zustand: .fehler, meldung: "Homebrew wurde nicht gefunden (\(Pfade.brew)).")
        }
        protokoll.berichte("brew update --quiet")
        let auffrischen = await Kommandozeile.ausfuehren(Pfade.brew, ["update", "--quiet"], zeitlimit: 300)
        if !auffrischen.ausgabe.istLeer { protokoll.berichte(auffrischen.ausgabe) }

        protokoll.berichte("brew upgrade \(formel)")
        let lauf = await Kommandozeile.ausfuehren(Pfade.brew, ["upgrade", formel],
                                                  zeitlimit: TimeInterval(eintrag.zeitlimitMinuten * 60))
        protokoll.berichte(lauf.ausgabe)

        if lauf.abgelaufen {
            return PruefErgebnis(zustand: .fehler, meldung: "Zeitlimit überschritten.", protokoll: lauf.ausgabe)
        }
        let neu = await fingerabdruck(eintrag)
        if lauf.exitCode != 0 {
            if istBereitsAktuell(lauf.ausgabe) {
                return PruefErgebnis(zustand: .aktuell, installierteVersion: neu,
                                     meldung: "War bereits aktuell.", protokoll: lauf.ausgabe)
            }
            return PruefErgebnis(zustand: .fehler, meldung: fehlergrund(lauf.ausgabe, exitCode: lauf.exitCode),
                                 protokoll: lauf.ausgabe)
        }
        return PruefErgebnis(zustand: .fertig, installierteVersion: neu,
                             meldung: "Update installiert.", protokoll: lauf.ausgabe)
    }

    private struct FormelInfo {
        let installiert: String
        let verfuegbar: String
        let veraltet: Bool
    }

    /// `versions.stable` ist die verfuegbare, der juengste `installed`-Eintrag die installierte Version.
    private func formelInfo(_ json: String) -> FormelInfo? {
        guard let daten = json.data(using: .utf8),
              let wurzel = try? JSONSerialization.jsonObject(with: daten) as? [String: Any],
              let formeln = wurzel["formulae"] as? [[String: Any]],
              let erste = formeln.first else { return nil }
        let versionen = erste["versions"] as? [String: Any]
        let stabil = (versionen?["stable"] as? String) ?? ""
        let revision = (erste["revision"] as? Int) ?? 0
        let installiert = ((erste["installed"] as? [[String: Any]]) ?? [])
            .compactMap { $0["version"] as? String }
            .max { Versionen.istNeuer($1, als: $0) } ?? ""
        return FormelInfo(installiert: installiert,
                          verfuegbar: revision > 0 ? "\(stabil)_\(revision)" : stabil,
                          veraltet: (erste["outdated"] as? Bool) ?? false)
    }

    private func infoLauf(_ argumente: [String]) async -> BefehlErgebnis {
        await Kommandozeile.ausfuehren(Pfade.brew, argumente, zeitlimit: 180)
    }

    /// Die ueber brew installierte Cask-Version, ohne Komma-Anhaengsel; leer, wenn nicht ueber brew.
    private func caskInstalliert(_ cask: String) async -> String {
        let lauf = await infoLauf(["info", "--cask", cask, "--json=v2"])
        return Versionen.caskBereinigen(caskInfo(lauf.ausgabe)?.installiert ?? "")
    }

    // MARK: - Intern

    private struct CaskInfo {
        let version: String
        let installiert: String?
    }

    /// Liest `version` und `installed` aus der JSON-Antwort von `brew info --json=v2`.
    private func caskInfo(_ json: String) -> CaskInfo? {
        guard let daten = json.data(using: .utf8),
              let wurzel = try? JSONSerialization.jsonObject(with: daten) as? [String: Any],
              let casks = wurzel["casks"] as? [[String: Any]],
              let erstes = casks.first else { return nil }

        let version = (erstes["version"] as? String) ?? ""
        let installiert = erstes["installed"] as? String
        return CaskInfo(version: version, installiert: installiert)
    }

    private func istUeberBrewInstalliert(_ cask: String) async -> Bool {
        let lauf = await Kommandozeile.ausfuehren(Pfade.brew, ["info", "--cask", cask, "--json=v2"],
                                                  zeitlimit: 120)
        return caskInfo(lauf.ausgabe)?.installiert != nil
    }

    private func istBereitsAktuell(_ ausgabe: String) -> Bool {
        let hinweise = ["already installed", "is already up-to-date", "nothing to upgrade",
                        "no cask to upgrade", "No outdated"]
        return hinweise.contains { ausgabe.localizedCaseInsensitiveContains($0) }
    }

    /// Uebersetzt die haeufigen brew-Fehlschlaege in einen Satz, mit dem der Benutzer etwas
    /// anfangen kann. Das Gegenstueck zu `BrauchtRechte` unter Windows ist hier der Fall, dass
    /// brew nach einem Passwort fragen will -- ohne Terminal kann es das nicht.
    static func fehlergrundText(_ ausgabe: String, exitCode: Int) -> String {
        // sudo fragt ueber den Askpass-Helfer der App (Kommandozeile.umgebung) nach dem Passwort.
        // Die drei Ausgaenge des Dialogs lesen sich in der sudo-Ausgabe so:
        if ausgabe.localizedCaseInsensitiveContains("no password was provided")
            || ausgabe.localizedCaseInsensitiveContains("User canceled")
            || ausgabe.localizedCaseInsensitiveContains("gave up") {
            return "Die Passwort-Abfrage wurde abgebrochen – ohne Mac-Passwort kann Homebrew dieses Update nicht einspielen."
        }
        if ausgabe.localizedCaseInsensitiveContains("incorrect password")
            || ausgabe.localizedCaseInsensitiveContains("Sorry, try again")
            || ausgabe.localizedCaseInsensitiveContains("incorrect password attempt") {
            return "Das Mac-Passwort war falsch – das Update erneut starten und das Passwort noch einmal eingeben."
        }
        if ausgabe.localizedCaseInsensitiveContains("a terminal is required")
            || ausgabe.localizedCaseInsensitiveContains("askpass") {
            return "Homebrew wollte nach dem Passwort fragen, der Passwort-Dialog der App kam aber nicht zustande. "
                 + "Den Befehl einmal im Terminal ausführen (Schaltfläche „Fenster“ rechts unten)."
        }
        if ausgabe.localizedCaseInsensitiveContains("sudo")
            || ausgabe.localizedCaseInsensitiveContains("password")
            || ausgabe.localizedCaseInsensitiveContains("Passwort") {
            return "Homebrew ist an der Passwort-Abfrage (sudo) gescheitert – Einzelheiten stehen im Protokoll."
        }
        if ausgabe.localizedCaseInsensitiveContains("It seems the App source")
            || ausgabe.localizedCaseInsensitiveContains("is already an App") {
            return "Das Programm liegt bereits dort, wo Homebrew es hinlegen will, "
                 + "stammt aber nicht von Homebrew. Einzelheiten stehen im Protokoll."
        }
        if ausgabe.localizedCaseInsensitiveContains("No available formula")
            || ausgabe.localizedCaseInsensitiveContains("No available cask") {
            return "Homebrew kennt dieses Cask nicht (mehr)."
        }
        if ausgabe.localizedCaseInsensitiveContains("still running")
            || ausgabe.localizedCaseInsensitiveContains("quit the application") {
            return "Das Programm läuft noch und muss für das Update beendet werden."
        }
        return "Homebrew endete mit Code \(exitCode)."
    }

    private func fehlergrund(_ ausgabe: String, exitCode: Int) -> String {
        BrewAktualisierer.fehlergrundText(ausgabe, exitCode: exitCode)
    }
}
