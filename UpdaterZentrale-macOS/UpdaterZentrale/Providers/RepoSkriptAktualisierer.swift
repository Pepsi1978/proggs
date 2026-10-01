import Foundation

/// Eigene Werkzeuge in ~/proggs. 1:1-Port von Providers/RepoSkriptAktualisierer.cs.
///
/// Das Aktualisieren baut die vorhandenen Skripte NIE nach, es ruft sie nur auf -- und niemals mit
/// einem Force-Schalter, weil jedes Skript seine eigene Rueckfrage stellen soll.
///
/// Zwei Unterschiede zur Windows-Fassung:
///
/// 1. **`bash` statt `pwsh`**, und die Skripte heissen anders: update-launcher.sh statt
///    update-launcher.ps1, rebuild-overlay.sh statt rebuild-overlay.ps1.
///
/// 2. **`rebuild-overlay.sh` hat auf dem Mac KEIN Ja/Nein-Fenster und gibt KEIN
///    `OVERLAY_UPDATE_STATUS=` aus** -- anders als seine Windows-Schwester. Es beendet die
///    Overlays und baut sofort. Damit trotzdem nichts ungefragt passiert, stellt die App die
///    Rueckfrage bei solchen Eintraegen (leeres `statusPraefix`) SELBST, bevor das Skript
///    ueberhaupt startet. Das Ergebnis wird dann aus Exit-Code und Schlusszeile gelesen.
///    Das Skript selbst bleibt unangetastet: es gehoert einem anderen Projekt.
struct RepoSkriptAktualisierer: Aktualisierer {
    let art = "reposkript"

    // MARK: - Pruefen

    func pruefen(_ eintrag: ProgrammEintrag, _ protokoll: Fortschritt) async -> PruefErgebnis {
        let installiert = installierteVersion(eintrag)
        let quelle = quellVersion(eintrag)

        // Merkmal 1: liegen auf der Gegenstelle Commits mit Quellcode fuer diesen Ordner, die hier fehlen?
        let hinterstand = await Self.hinterstand(eintrag.repoOrdner, protokoll)
        let hatRepoOrdner = !(eintrag.repoOrdner?.istLeer ?? true)

        // Merkmal 2: ist der Quellstand neuer als die gebaute Fassung? Numerisch verglichen -- ein
        // Praefixvergleich hielte "1.24.5" fuer aktuell, wenn aus "1.24.50" gebaut wurde.
        let quellstandNeuer = !quelle.istLeer && !installiert.istLeer
                              && Versionen.istNeuer(quelle, als: installiert)

        protokoll.berichte("gebaut: \(installiert) | Quelle: \(quelle)")

        if installiert.istLeer {
            return PruefErgebnis(zustand: .nichtInstalliert, verfuegbareVersion: quelle,
                                 meldung: "Noch nicht gebaut.")
        }

        if (hinterstand ?? 0) > 0 || quellstandNeuer {
            let grund = (hinterstand ?? 0) > 0
                ? "\(hinterstand!) neue Commit(s) mit Quellcode auf origin/main"
                : "Quellstand \(quelle) neuer als der gebaute Stand"
            return PruefErgebnis(zustand: .updateVerfuegbar, installierteVersion: installiert,
                                 verfuegbareVersion: quelle, meldung: grund + ".")
        }

        // Eine gescheiterte Git-Abfrage ist "unbekannt", nie "aktuell": die Karte behaelt dann ihre
        // Update-Schaltflaeche, statt einen offenen Build hinter einer gruenen Plakette zu verstecken.
        if hatRepoOrdner && hinterstand == nil {
            return PruefErgebnis(zustand: .unbekannt, installierteVersion: installiert, verfuegbareVersion: quelle,
                                 meldung: "Der Git-Stand ließ sich nicht abfragen – ob neue Commits vorliegen, ist unbekannt.")
        }

        return PruefErgebnis(zustand: .aktuell, installierteVersion: installiert,
                             verfuegbareVersion: quelle, meldung: "Gebauter Stand ist aktuell.")
    }

    /// Nur Dateien, die im Build landen, zaehlen. Die Ordner tragen auch Laufzeit- und Profildaten,
    /// die andere Rechner laufend committen; als "neue Commits" gezaehlt kuendigten sie ein Update
    /// an, das das Skript zu Recht nicht baute -- und die Karte bot dasselbe Update immer wieder an.
    static let bauQuellen = ["*.swift", "*.plist", "*.sh", "*.entitlements", "*.icns"]

    /// Commits auf origin/main, die Build-Quellen des Ordners beruehren; 0 ohne Ordner; nil, wenn
    /// git nicht antworten konnte -- nie eine stille 0, die als "aktuell" gelesen wuerde.
    static func hinterstand(_ ordner: String?, _ protokoll: Fortschritt) async -> Int? {
        guard let ordner, !ordner.istLeer else { return 0 }
        let holen = await Kommandozeile.ausfuehren("/usr/bin/git", ["fetch", "--quiet"],
                                                   zeitlimit: 180, arbeitsverzeichnis: Pfade.repoWurzel)
        if holen.abgelaufen || holen.exitCode != 0 {
            protokoll.berichte("git fetch fehlgeschlagen (\(holen.abgelaufen ? "Zeitlimit" : "Code \(holen.exitCode)")) "
                               + "– Remote-Stand unbekannt: \(holen.ausgabe)")
            return nil
        }
        let sauber = ordner.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        let muster = bauQuellen.map { ":(glob)\(sauber)/**/\($0)" }
        let zaehl = await Kommandozeile.ausfuehren("/usr/bin/git",
                                                   ["rev-list", "--count", "HEAD..origin/main", "--"] + muster,
                                                   zeitlimit: 120, arbeitsverzeichnis: Pfade.repoWurzel)
        let anzahl = !zaehl.abgelaufen && zaehl.exitCode == 0 ? Int(zaehl.ausgabe.trimmed) : nil
        protokoll.berichte("git rev-list HEAD..origin/main -- \(ordner) (Quellcode) -> "
                           + (anzahl.map(String.init) ?? "FEHLER Code \(zaehl.exitCode): \(zaehl.ausgabe)"))
        return anzahl
    }

    // MARK: - Aktualisieren

    func aktualisieren(_ eintrag: ProgrammEintrag, _ protokoll: Fortschritt) async -> PruefErgebnis {
        let skript = (Pfade.repoWurzel as NSString).appendingPathComponent(eintrag.skript ?? "")
        guard FileManager.default.fileExists(atPath: skript) else {
            return PruefErgebnis(zustand: .fehler, meldung: "Skript fehlt: \(skript)")
        }

        let hatEigeneRueckfrage = !(eintrag.statusPraefix?.istLeer ?? true)

        // Skripte ohne eigenes Ja/Nein-Fenster: hier wird gefragt, sonst gar nicht.
        if !hatEigeneRueckfrage {
            let frage = "\(eintrag.name) wird jetzt neu gebaut und neu gestartet.\n\n"
                      + "Das Skript beendet das laufende Programm dafür selbstständig; nicht "
                      + "gespeicherte Eingaben gehen verloren. Der Vorgang dauert ein bis zwei Minuten.\n\n"
                      + "Jetzt aktualisieren?"
            let freigegeben = await MainActor.run { Dialoge.fragen(frage, "\(eintrag.name) neu bauen?") }
            if !freigegeben {
                return PruefErgebnis(zustand: .abgebrochen,
                                     meldung: "Du hast abgelehnt – es wurde nichts geändert.")
            }
            protokoll.berichte("Update per Klick freigegeben.")
        }

        // Die Pruefung meldet ein Update, sobald origin/main Commits fuer diesen Ordner traegt -- die
        // Skripte bauen aber nur, was auf der Platte liegt. Ohne vorheriges Ziehen antworteten sie
        // "bereits aktuell", die naechste Pruefung faende dieselben Commits wieder, und die Karte
        // boete dasselbe Update endlos an. Nur Fast-Forward, wie der Launcher selbst.
        if let ordner = eintrag.repoOrdner, !ordner.istLeer,
           let neu = await Self.hinterstand(ordner, protokoll), neu > 0 {
            protokoll.berichte("git pull --ff-only (\(neu) neue Commit(s) für \(ordner))")
            let zug = await Kommandozeile.ausfuehren("/usr/bin/git", ["pull", "--ff-only", "--quiet"],
                                                     zeitlimit: 180, arbeitsverzeichnis: Pfade.repoWurzel)
            protokoll.berichte(zug.ausgabe)
            if zug.abgelaufen || zug.exitCode != 0 {
                return PruefErgebnis(zustand: .fehler,
                                     meldung: "git pull --ff-only ist fehlgeschlagen (lokale Änderungen oder abweichender Verlauf) – "
                                            + "das Repo muss erst von Hand abgeglichen werden. Das Update-Skript wurde nicht gestartet.",
                                     protokoll: zug.ausgabe)
            }
        }

        let exeVorher = await fingerabdruck(eintrag)
        var argumente = [skript]
        if let skriptArgumente = eintrag.skriptArgumente, !skriptArgumente.istLeer {
            argumente.append(contentsOf: Kommandozeile.zerlegen(skriptArgumente))
        }

        protokoll.berichte("bash " + argumente.joined(separator: " "))
        if hatEigeneRueckfrage {
            protokoll.berichte("Das Skript zeigt jetzt ein Ja/Nein-Fenster. Ohne Klick auf 'Ja' passiert nichts.")
        }

        // Die Skripte mit Dialog warten bis zu 240 s auf den Klick; das Zeitlimit muss klar
        // darueber liegen. Die ohne Dialog warten bis zu 300 s auf eine laufende Aufnahme.
        let zeitlimit = TimeInterval(max(eintrag.dialogWartezeitSekunden, 300) + eintrag.zeitlimitMinuten * 60)
        let lauf = await Kommandozeile.ausfuehren("/bin/bash", argumente,
                                                  zeitlimit: zeitlimit,
                                                  arbeitsverzeichnis: Pfade.repoWurzel)
        protokoll.berichte(lauf.ausgabe)

        // Wie der Prozess endete, wiegt schwerer als das, was er ausgab: ein "started" vor einem
        // Zeitlimit ist nie ein fertiges Update.
        if lauf.abgelaufen {
            return PruefErgebnis(zustand: .fehler, meldung: "Zeitlimit überschritten.", protokoll: lauf.ausgabe)
        }
        let exeNachher = await fingerabdruck(eintrag)
        let buildUnveraendert = !exeVorher.istLeer && exeVorher == exeNachher

        // Weg A: das Skript meldet seinen Status selbst (update-launcher.sh).
        if hatEigeneRueckfrage {
            let status = statusLesen(lauf.ausgabe, eintrag.statusPraefix)
            switch status {
            case "cancelled":
                return PruefErgebnis(zustand: .abgebrochen,
                                     meldung: "Du hast im Ja/Nein-Fenster auf 'Nein' geklickt – es wurde nichts geändert.",
                                     protokoll: lauf.ausgabe)
            case "no-answer":
                return PruefErgebnis(zustand: .abgebrochen,
                                     meldung: "Kein Klick im Zeitfenster – es wurde nichts geändert.",
                                     protokoll: lauf.ausgabe)
            case "already-current":
                return PruefErgebnis(zustand: .aktuell, meldung: "War bereits aktuell.",
                                     protokoll: lauf.ausgabe)
            case "started":
                // "started" allein beweist kein Update: das Skript sagt es auch, wenn der Build schon
                // aktuell war und es nur die vorhandene Fassung gestartet hat.
                return buildUnveraendert
                    ? PruefErgebnis(zustand: .aktuell, meldung: "Der Build war bereits aktuell – das Skript hat ihn nur gestartet.",
                                    protokoll: lauf.ausgabe)
                    : PruefErgebnis(zustand: .fertig, meldung: "Neue Version gebaut und gestartet.",
                                    protokoll: lauf.ausgabe)
            default:
                break
            }
        }

        // Weg B: kein Statusband -- Exit-Code und Schlusszeile entscheiden (rebuild-overlay.sh).
        if lauf.exitCode == 0 {
            let meldung = lauf.ausgabe.contains("Fertig — alle Ziele")
                       || lauf.ausgabe.contains("Fertig - alle Ziele")
                ? "Neue Version gebaut, gestartet und verifiziert."
                : "Skript erfolgreich durchgelaufen."
            return PruefErgebnis(zustand: .fertig, meldung: meldung, protokoll: lauf.ausgabe)
        }

        if lauf.ausgabe.contains("Fertig mit Problemen bei") {
            return PruefErgebnis(zustand: .fehler,
                                 meldung: "Das Skript konnte den Neustart nicht verifizieren – Einzelheiten im Protokoll.",
                                 protokoll: lauf.ausgabe)
        }

        return PruefErgebnis(zustand: .fehler, meldung: "Das Skript endete mit Code \(lauf.exitCode).",
                             protokoll: lauf.ausgabe)
    }

    // MARK: - Fingerabdruck

    /// Die Versionsnummer allein bewegt sich nur, wenn jemand sie hochzaehlt -- sie wuerde nach
    /// einem Neubau faelschlich einen Fehlschlag melden. Was sich hier wirklich aendert, ist die
    /// Schreibzeit der gebauten Programmdatei.
    func fingerabdruck(_ eintrag: ProgrammEintrag) async -> String {
        let exe = Pfade.aufloesen(eintrag.exePfadWirksam)
        guard !exe.isEmpty, FileManager.default.fileExists(atPath: exe) else { return "" }

        let zeit = Pfade.schreibZeit(exe)
        guard !zeit.isEmpty else { return "" }
        let version = installierteVersion(eintrag)
        return (version.istLeer ? "" : version + " vom ") + zeit
    }

    // MARK: - Intern

    private func statusLesen(_ ausgabe: String, _ praefix: String?) -> String {
        guard let praefix, !praefix.istLeer else { return "" }
        for zeile in ausgabe.split(separator: "\n", omittingEmptySubsequences: false) {
            let text = String(zeile)
            guard let stelle = text.range(of: praefix, options: .caseInsensitive) else { continue }
            let rest = String(text[stelle.upperBound...]).trimmed
            return rest.split(separator: " ").first.map(String.init) ?? ""
        }
        return ""
    }

    /// Die gebaute Fassung. Unter Windows war das FileVersionInfo der exe; auf dem Mac liegt die
    /// Version in der Info.plist des gebauten App-Buendels.
    private func installierteVersion(_ eintrag: ProgrammEintrag) -> String {
        // Bevorzugt das App-Buendel; sonst das Buendel um die Programmdatei herum ableiten
        // (<App>.app/Contents/MacOS/<Name> -> <App>.app).
        if let appPfad = eintrag.appPfad, !appPfad.istLeer {
            let pfad = Pfade.aufloesen(appPfad)
            if FileManager.default.fileExists(atPath: pfad) { return Pfade.appVersion(pfad) }
        }

        let exe = Pfade.aufloesen(eintrag.exePfadWirksam)
        guard !exe.isEmpty, FileManager.default.fileExists(atPath: exe) else { return "" }

        let macOsOrdner = (exe as NSString).deletingLastPathComponent          // …/Contents/MacOS
        let contents = (macOsOrdner as NSString).deletingLastPathComponent     // …/Contents
        let buendel = (contents as NSString).deletingLastPathComponent         // …/<App>.app
        guard buendel.hasSuffix(".app") else { return "" }
        return Pfade.appVersion(buendel)
    }

    /// Der Soll-Stand aus der Quelle. Unter Windows das `<Version>`-Element der csproj, auf dem
    /// Mac das CFBundleShortVersionString der Info.plist im Quellordner.
    private func quellVersion(_ eintrag: ProgrammEintrag) -> String {
        guard let projektDatei = eintrag.projektDatei, !projektDatei.istLeer else { return "" }
        let datei = (Pfade.repoWurzel as NSString).appendingPathComponent(projektDatei)
        guard FileManager.default.fileExists(atPath: datei) else { return "" }
        return Pfade.plistWert(datei, schluessel: "CFBundleShortVersionString")
    }
}
