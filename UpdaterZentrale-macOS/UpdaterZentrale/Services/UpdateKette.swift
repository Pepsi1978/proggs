import Foundation

/// Das Endurteil eines Klicks, plus was die Karte zeigen soll.
struct KettenUrteil {
    var ergebnis: LaufErgebnis
    var meldung: String
    var fingerabdruckVorher: String
    var fingerabdruckNachher: String
    var fuerKarte: PruefErgebnis
    var updateAufrufe: Int
    var updateLiefDurch: Bool
    var ausstehendeVersion: String? = nil
    var uebergaenge: [String] = []
}

/// Fuehrt einen Klick auf "Aktualisieren" bis zum ECHTEN Endstand, statt einem Exit-Code zu
/// glauben. Port von Services/UpdateKette.cs.
///
/// Der alte Ablauf schrieb "Erfolgreich", sobald sich irgendein Fingerabdruck bewegte, und fragte
/// erst danach den Anbieter. Ein Teil-Update (eine von zwei Runtimes, ein abgelegter erster
/// Schritt) wurde als Erfolg protokolliert und bei der naechsten Pruefung erneut angeboten.
///
/// Regeln:
/// 1. Vorher: Fingerabdruck und eine echte Pruefung. Schon aktuell → kein Update-Aufruf.
/// 2. Ein Durchlauf = Update-Aufruf, begrenztes Nachwarten (Fingerabdruck in kurzen Pausen, damit
///    ein Installer-Kind, das nach dem Elternprozess fertig wird, abgewartet wird), dann echte Pruefung.
/// 3. Erfolg nur, wenn der Aufruf nicht scheiterte, der Fingerabdruck sich nachweislich bewegte
///    und die Pruefung "aktuell" sagt.
/// 4. Weiter offen: ein Folgedurchlauf nur fuer ein ANDERES, lesbares Folgeangebot zusammen mit
///    nachgewiesenem Fortschritt. Dasselbe Angebot wieder beendet die Kette -- derselbe
///    Update-Befehl wird nie wiederholt.
/// 5. Kein Fortschritt → sofort Schluss. Fehler, Abbruch und ein abgelegtes Update
///    (erstNachNeustart) beenden die Kette sofort. Eine Pruefung mit Fehler oder "unbekannt" ist
///    nie ein Erfolg.
@MainActor
final class UpdateKette {
    private let aktualisierer: Aktualisierer
    private let eintrag: ProgrammEintrag
    private let protokoll: Fortschritt
    private let status: (String) -> Void

    var maxDurchlaeufe = 3
    var maxNachpruefungen = 4
    var pause: TimeInterval = 3

    init(_ aktualisierer: Aktualisierer, _ eintrag: ProgrammEintrag, _ protokoll: Fortschritt,
         status: @escaping (String) -> Void) {
        self.aktualisierer = aktualisierer
        self.eintrag = eintrag
        self.protokoll = protokoll
        self.status = status
    }

    /// Schritt 0, abgetrennt, damit der Aufrufer dazwischen seine Beenden-Frage stellen kann.
    func vorpruefen() async -> (fingerabdruck: String, pruefung: PruefErgebnis) {
        status("Ermittelt den Stand …")
        let fingerabdruck = await aktualisierer.fingerabdruck(eintrag)
        let pruefung = await aktualisierer.pruefen(eintrag, protokoll)
        protokoll.berichte("Vorprüfung: " + Self.beschreibe(pruefung) + " | Stand " + Self.anzeigen(fingerabdruck))
        Diagnose.ereignis(.info, "kette", "kette.vorpruefung", "Vorprüfung: " + Self.beschreibe(pruefung),
                          ["programm": eintrag.id, "stand": fingerabdruck, "angebot": pruefung.verfuegbareVersion])
        return (fingerabdruck, pruefung)
    }

    /// Eine aktuelle Vorpruefung beendet die Aktion ohne jeden Update-Aufruf.
    static func bereitsAktuell(_ fingerabdruck: String, _ vor: PruefErgebnis) -> KettenUrteil? {
        guard vor.zustand == .aktuell else { return nil }
        var karte = vor
        karte.meldung = "Auf dem neuesten Stand."
        return KettenUrteil(ergebnis: .bereitsAktuell,
                            meldung: "Bereits auf dem neuesten Stand – die Anzeige war veraltet, es wurde nichts installiert.",
                            fingerabdruckVorher: fingerabdruck, fingerabdruckNachher: fingerabdruck,
                            fuerKarte: karte, updateAufrufe: 0, updateLiefDurch: false)
    }

    func ausfuehren(_ fingerabdruckStart: String, _ vor: PruefErgebnis) async -> KettenUrteil {
        var uebergaenge: [String] = []
        var angebot = vor.verfuegbareVersion
        var standVor = fingerabdruckStart
        var aufrufe = 0
        var liefDurch = false
        var letztePruefung = vor
        var standNach = fingerabdruckStart

        func ende(_ urteil: LaufErgebnis, _ meldung: String, _ karte: PruefErgebnis, _ ausstehend: String? = nil) -> KettenUrteil {
            let schwere: Schwere = urteil == .fehlgeschlagen || urteil == .nichtVerifiziert ? .fehler
                : urteil == .abgebrochen ? .warnung : .info
            Diagnose.ereignis(schwere, "kette", "kette.urteil", meldung,
                              ["programm": eintrag.id, "ergebnis": urteil.rawValue, "aufrufe": aufrufe,
                               "standVorher": fingerabdruckStart, "standNachher": standNach])
            var k = karte
            k.meldung = meldung
            return KettenUrteil(ergebnis: urteil, meldung: meldung, fingerabdruckVorher: fingerabdruckStart,
                                fingerabdruckNachher: standNach, fuerKarte: k, updateAufrufe: aufrufe,
                                updateLiefDurch: liefDurch, ausstehendeVersion: ausstehend, uebergaenge: uebergaenge)
        }

        func alsFehler(_ p: PruefErgebnis) -> PruefErgebnis {
            var k = p
            k.zustand = .fehler
            return k
        }

        for durchlauf in 1...maxDurchlaeufe {
            if durchlauf > 1 {
                let hinweis = "Folgeupdate \(durchlauf) von \(maxDurchlaeufe)"
                    + (eintrag.art.lowercased() == "reposkript" ? " – das Skript fragt erneut nach Ja/Nein." : ".")
                protokoll.berichte(hinweis)
                status(hinweis)
            } else {
                status("Aktualisiert …")
            }

            aufrufe += 1
            Diagnose.ereignis(.info, "kette", "kette.durchlauf", "Durchlauf \(durchlauf) von \(maxDurchlaeufe)",
                              ["programm": eintrag.id, "angebot": angebot, "standVor": standVor])
            let ergebnis = await aktualisierer.aktualisieren(eintrag, protokoll)

            if ergebnis.zustand == .abgebrochen {
                var k = ergebnis
                k.zustand = .abgebrochen
                return ende(.abgebrochen, Self.text(ergebnis.meldung, "Abgebrochen."), k)
            }
            if ergebnis.zustand == .fehler || ergebnis.zustand == .nichtInstalliert {
                return ende(.fehlgeschlagen, Self.text(ergebnis.meldung, "Das Update ist fehlgeschlagen."), alsFehler(ergebnis))
            }

            liefDurch = true

            if ergebnis.erstNachNeustart {
                // Abgelegt: wird beim naechsten Start echt. Jetzt nachzupruefen zeigte die alte
                // Version, und ein Wiederholen installierte dasselbe Paket erneut.
                var k = ergebnis
                k.zustand = .fertig
                return ende(.ausstehend, Self.text(ergebnis.meldung, "Installiert – wird beim nächsten Start übernommen."),
                            k, angebot.istLeer ? nil : angebot)
            }

            // Nachwarten: begrenzt auf den echten Stand warten -- ein Installer-Kind arbeitet evtl. noch.
            status("Prüft das Ergebnis …")
            var nach: PruefErgebnis?
            standNach = ""
            for versuch in 1...maxNachpruefungen {
                if versuch > 1 { try? await Task.sleep(nanoseconds: UInt64(pause * 1_000_000_000)) }
                standNach = await aktualisierer.fingerabdruck(eintrag)
                let bewegt = Self.fortschritt(standVor, standNach)
                let letzterVersuch = versuch == maxNachpruefungen

                if !bewegt && !letzterVersuch && ergebnis.zustand != .aktuell { continue }   // noch nicht angekommen

                let pruefung = await aktualisierer.pruefen(eintrag, protokoll)
                nach = pruefung
                if pruefung.zustand == .aktuell { break }
                if pruefung.zustand == .updateVerfuegbar && bewegt
                    && pruefung.verfuegbareVersion == angebot && !letzterVersuch { continue }   // Anbieter-Zwischenspeicher hinkt evtl. einmal
                break
            }
            letztePruefung = nach ?? letztePruefung

            let uebergang = "Durchlauf \(durchlauf): " + Self.anzeigen(standVor) + " → " + Self.anzeigen(standNach)
                          + " | Prüfung: " + Self.beschreibe(letztePruefung)
            uebergaenge.append(uebergang)
            protokoll.berichte(uebergang)

            if standNach.istLeer {
                return ende(.nichtVerifiziert, "Der Stand ließ sich nach dem Update nicht ermitteln – es ist nicht überprüfbar.",
                            alsFehler(letztePruefung))
            }

            if [.fehler, .unbekannt, .nichtInstalliert].contains(letztePruefung.zustand) {
                return ende(.nichtVerifiziert,
                            "Die Nachprüfung ergab keinen belastbaren Stand (" + Self.beschreibe(letztePruefung)
                            + ") – nicht als Erfolg gewertet.", alsFehler(letztePruefung))
            }

            let bewegt = Self.fortschritt(standVor, standNach)

            if letztePruefung.zustand == .aktuell {
                var karte = letztePruefung
                karte.meldung = "Auf dem neuesten Stand."
                if bewegt || Self.fortschritt(fingerabdruckStart, standNach) {
                    return ende(.erfolgreich,
                                "Verifiziert: " + Self.anzeigen(fingerabdruckStart) + " → " + Self.anzeigen(standNach)
                                + (durchlauf > 1 ? " (in \(durchlauf) Durchläufen)" : ""), karte)
                }
                if ergebnis.zustand == .aktuell && durchlauf == 1 {
                    return ende(.bereitsAktuell,
                                Self.text(ergebnis.meldung, "War bereits aktuell.") + " Die Nachprüfung bestätigt den aktuellen Stand.",
                                karte)
                }
                return ende(.nichtVerifiziert,
                            "Die Prüfung meldet jetzt „aktuell“, der Stand hat sich aber nicht verändert ("
                            + Self.anzeigen(standNach) + ") – nicht als Erfolg gewertet.", alsFehler(letztePruefung))
            }

            // Weiter offen. Ein Folgedurchlauf braucht ein ANDERES konkretes Angebot.
            let folgeangebot = letztePruefung.verfuegbareVersion
            if folgeangebot.istLeer {
                return ende(.nichtVerifiziert,
                            "Nach dem Update ist weiter ein Update offen, das Angebot ist aber nicht lesbar ("
                            + Self.beschreibe(letztePruefung) + ") – kein erneuter Versuch.", alsFehler(letztePruefung))
            }
            if folgeangebot == angebot {
                return ende(.nichtVerifiziert,
                            "Dasselbe Update (\(folgeangebot)) wird weiter angeboten"
                            + (bewegt ? ", obwohl sich der Stand geändert hat (" + Self.anzeigen(standVor) + " → "
                               + Self.anzeigen(standNach) + ")" : ", der Stand ist unverändert")
                            + " – kein erneuter Versuch.", alsFehler(letztePruefung))
            }
            if !bewegt {
                return ende(.nichtVerifiziert,
                            "Der Stand ist unverändert (" + Self.anzeigen(standNach) + "), das Angebot wechselte auf "
                            + folgeangebot + " – kein erneuter Versuch.", alsFehler(letztePruefung))
            }
            if durchlauf == maxDurchlaeufe {
                return ende(.nichtVerifiziert,
                            "Nach \(maxDurchlaeufe) Durchläufen ist weiter ein Update offen (" + Self.beschreibe(letztePruefung)
                            + "). Übergänge: " + uebergaenge.joined(separator: " / "), alsFehler(letztePruefung))
            }

            standVor = standNach
            angebot = folgeangebot
        }

        // Konstruktionsbedingt unerreichbar; bleibt geschlossen.
        return ende(.nichtVerifiziert, "Die Update-Kette endete ohne Urteil.", alsFehler(letztePruefung))
    }

    /// Nachgewiesene Bewegung: beide Lesungen vorhanden und verschieden.
    static func fortschritt(_ vorher: String, _ nachher: String) -> Bool {
        !vorher.istLeer && !nachher.istLeer && vorher != nachher
    }

    /// Ein abgelegtes Update, spaeter erneut geprueft. Bestaetigt nur, wenn das erwartete Ziel
    /// erreicht ist (falls bekannt) UND der Anbieter "aktuell" sagt.
    /// - Returns: nil = weiter warten, sonst das festzuhaltende Urteil.
    static func stagedUrteil(_ offen: UpdateBericht, jetzt: String, pruefung: PruefErgebnis,
                             zeitpunkt: Date) -> (ergebnis: LaufErgebnis, meldung: String)? {
        var zielErreicht = true
        if let ziel = offen.ausstehendeVersion, !ziel.istLeer {
            let soll = Versionen.ausText(ziel)
            let ist = Versionen.ausText(jetzt)
            zielErreicht = !soll.istLeer && !ist.istLeer && Versionen.vergleiche(ist, soll) >= 0
        }

        if pruefung.zustand == .aktuell && zielErreicht && fortschritt(offen.versionNachher, jetzt) {
            return (.erfolgreich, "Nachträglich bestätigt: das Update vom \(Formate.datum(offen.zeit)) "
                                + "ist inzwischen aktiv (\(jetzt)).")
        }
        if zeitpunkt.timeIntervalSince(offen.zeit) >= 7 * 24 * 60 * 60 {
            return (.nichtVerifiziert, "Das Update vom \(Formate.datum(offen.zeit)) ist bis heute nicht nachweislich übernommen ("
                                     + (jetzt.istLeer ? "Stand unbekannt" : "Stand " + jetzt)
                                     + ", Prüfung: " + beschreibe(pruefung) + ").")
        }
        return nil
    }

    private static func text(_ meldung: String, _ ersatz: String) -> String { meldung.istLeer ? ersatz : meldung }

    static func anzeigen(_ fingerabdruck: String) -> String { fingerabdruck.istLeer ? "(unbekannt)" : fingerabdruck }

    static func beschreibe(_ p: PruefErgebnis) -> String {
        switch p.zustand {
        case .aktuell: return "aktuell"
        case .updateVerfuegbar: return "Update offen" + (p.verfuegbareVersion.istLeer ? "" : " (\(p.verfuegbareVersion))")
        case .fehler: return "Fehler" + (p.meldung.istLeer ? "" : ": " + p.meldung)
        case .unbekannt: return "unbekannt" + (p.meldung.istLeer ? "" : ": " + p.meldung)
        case .nichtInstalliert: return "nicht installiert"
        default: return "\(p.zustand)"
        }
    }
}
