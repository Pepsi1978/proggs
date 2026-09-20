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
        let appPfad = Pfade.aufloesen(eintrag.appPfad)
        let installiert = appPfad.isEmpty ? "" : Pfade.appVersion(appPfad)

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
            let stand = Pfade.appVersion(Pfade.aufloesen(eintrag.appPfad))
            return PruefErgebnis(zustand: .aktuell, installierteVersion: stand,
                                 meldung: "War bereits aktuell.", protokoll: lauf.ausgabe)
        }

        if lauf.exitCode != 0 {
            return PruefErgebnis(zustand: .fehler, meldung: fehlergrund(lauf.ausgabe, exitCode: lauf.exitCode),
                                 protokoll: lauf.ausgabe)
        }

        let neu = Pfade.appVersion(Pfade.aufloesen(eintrag.appPfad))
        return PruefErgebnis(zustand: .fertig, installierteVersion: neu,
                             meldung: "Update installiert.", protokoll: lauf.ausgabe)
    }

    // MARK: - Fingerabdruck

    /// Die Version aus der Info.plist -- direkt vom Programm gelesen, ohne Netz und ohne brew.
    func fingerabdruck(_ eintrag: ProgrammEintrag) async -> String {
        let appPfad = Pfade.aufloesen(eintrag.appPfad)
        guard !appPfad.isEmpty else { return "" }
        return Pfade.appVersion(appPfad)
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
        if ausgabe.localizedCaseInsensitiveContains("sudo")
            || ausgabe.localizedCaseInsensitiveContains("password")
            || ausgabe.localizedCaseInsensitiveContains("Passwort") {
            return "Homebrew wollte nach dem Passwort fragen – das geht hier nicht. "
                 + "Den Befehl einmal im Terminal ausführen (Schaltfläche „Fenster“ rechts unten)."
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
