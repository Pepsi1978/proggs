import Foundation

/// Einstellungen von Jarvis am Mac per Sprache lesen und ändern (Gegenstück zum EinstellungsKatalog der Handy-App).
/// Zugangsschlüssel, Passwörter und Server-Adressen gehören bewusst nicht hinein.
@MainActor
enum MacEinstellungsKatalog {
    struct Eintrag {
        let kennung: String
        let bereich: String
        let titel: String
        let beschreibung: String
        let werte: String
        let lesen: () -> String
        let setzen: (String) -> String?   // nil = ok, sonst Fehlermeldung
    }

    static func wahr(_ s: String) -> Bool? {
        switch s.lowercased() {
        case "true", "an", "ein", "ja", "1", "on": return true
        case "false", "aus", "nein", "0", "off": return false
        default: return nil
        }
    }

    private static func stillezeit(_ kennung: String, _ titel: String, _ beschreibung: String, _ pfad: ReferenceWritableKeyPath<Einstellungen, Double>) -> Eintrag {
        let e = Einstellungen.shared
        return Eintrag(kennung: kennung, bereich: "Sprache", titel: titel, beschreibung: beschreibung,
                       werte: "1 bis 15 Sekunden, auch mit Komma, z. B. 2,5", lesen: { Einstellungen.stilleText(e[keyPath: pfad]) }) { v in
            guard let s = Einstellungen.stilleAus(v) else { return "Erlaubt: 1 bis 15 Sekunden, auch mit Komma" }
            e[keyPath: pfad] = s; return nil
        }
    }

    /// Kennungen aus „a, b, c“, klein geschrieben.
    private static func liste(_ w: String) -> [String] {
        w.split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces).lowercased() }.filter { !$0.isEmpty }
    }

    private static func ohneDoppel(_ ids: [String]) -> [String] {
        var gesehen = Set<String>()
        return ids.filter { gesehen.insert($0).inserted }
    }

    static var alle: [Eintrag] {
        let e = Einstellungen.shared
        let v = VorleseEinstellungen.shared
        return [
            Eintrag(kennung: "modell", bereich: "Jarvis' Kopf", titel: "Modell", beschreibung: "Welches Modell am Mac denkt.",
                    werte: ModellWahl.alle.map(\.id).joined(separator: " | "), lesen: { e.modell }) { v in
                guard let m = ModellWahl.alle.first(where: { $0.id == v || $0.name.lowercased() == v.lowercased() }) else { return "Unbekanntes Modell" }
                e.modell = m.id; return nil
            },
            Eintrag(kennung: "denkstufe", bereich: "Jarvis' Kopf", titel: "Denkstufe", beschreibung: "Wie gründlich das Modell nachdenkt.",
                    werte: "low | medium | high | xhigh | max", lesen: { e.effort }) { v in
                guard ["low", "medium", "high", "xhigh", "max"].contains(v) else { return "Erlaubt: low, medium, high, xhigh, max" }
                e.effort = v; return nil
            },
            Eintrag(kennung: "sprach_denkstufe", bereich: "Jarvis' Kopf", titel: "Denkstufe beim Sprechen", beschreibung: "Wie gründlich das Modell bei gesprochenen Fragen nachdenkt; niedriger heißt schnellere Antwort.",
                    werte: "low | medium | high | xhigh | max", lesen: { e.sprachEffort }) { v in
                guard ["low", "medium", "high", "xhigh", "max"].contains(v) else { return "Erlaubt: low, medium, high, xhigh, max" }
                e.sprachEffort = v; return nil
            },
            Eintrag(kennung: "schrittlimit", bereich: "Jarvis' Kopf", titel: "Schrittlimit", beschreibung: "Werkzeugschritte je Anfrage, 0 = unbegrenzt.",
                    werte: "0–200", lesen: { "\(e.schrittLimit)" }) { v in
                guard let n = Int(v), (0...200).contains(n) else { return "Zahl 0–200" }
                e.schrittLimit = n; return nil
            },
            Eintrag(kennung: "helfer_automatik", bereich: "Eigene Tools", titel: "Helfer selbst bauen lassen", beschreibung: "Aus = Pause: Jarvis beauftragt keine neuen Helfer, vorhandene laufen weiter.",
                    werte: "an | aus", lesen: { e.helferAutomatik ? "an" : "aus" }) { v in
                guard let b = wahr(v) else { return "an oder aus" }; e.helferAutomatik = b; return nil
            },
            Eintrag(kennung: "recherche_modell", bereich: "Eigene Tools", titel: "Recherche-Modell", beschreibung: "Modell für gründliche Recherchen beim Helferbau; keine kleinen Modelle.",
                    werte: ModellWahl.fuerRecherche.map(\.id).joined(separator: " | "), lesen: { e.rechercheModell }) { v in
                guard let m = ModellWahl.fuerRecherche.first(where: { $0.id == v || $0.name.lowercased() == v.lowercased() }) else { return "Für Recherchen nicht wählbar" }
                e.rechercheModell = m.id; return nil
            },
            Eintrag(kennung: "recherche_effort", bereich: "Eigene Tools", titel: "Recherche-Effort", beschreibung: "Wie gründlich die Recherche-Agenten denken.",
                    werte: "medium | high | xhigh | max", lesen: { e.rechercheEffort }) { v in
                guard ["medium", "high", "xhigh", "max"].contains(v) else { return "Erlaubt: medium, high, xhigh, max" }
                e.rechercheEffort = v; return nil
            },
            Eintrag(kennung: "lernen", bereich: "Jarvis' Kopf", titel: "Lernen am Mac", beschreibung: "Neue Angaben aus ruhenden Mac-Gesprächen ins gemeinsame Gedächtnis.",
                    werte: "an | aus", lesen: { e.lernenAn ? "an" : "aus" }) { v in
                guard let b = wahr(v) else { return "an oder aus" }; e.lernenAn = b; return nil
            },
            Eintrag(kennung: "vorlesen", bereich: "Stimme", titel: "Antworten vorlesen", beschreibung: "Im Sprachgespräch die Antworten vorlesen.",
                    werte: "an | aus", lesen: { e.vorlesen ? "an" : "aus" }) { v in
                guard let b = wahr(v) else { return "an oder aus" }; e.vorlesen = b; return nil
            },
            Eintrag(kennung: "vorlese_anbieter", bereich: "Stimme", titel: "Vorlese-Anbieter", beschreibung: "Wer vorliest.",
                    werte: VorleseAnbieter.allCases.map(\.rawValue).joined(separator: " | "), lesen: { v.anbieter.rawValue }) { w in
                guard let a = VorleseAnbieter.allCases.first(where: { $0.rawValue == w || $0.titel.lowercased().contains(w.lowercased()) }) else { return "Unbekannter Anbieter" }
                v.anbieter = a; return nil
            },
            Eintrag(kennung: "tempo", bereich: "Stimme", titel: "Sprechtempo", beschreibung: "Tempo des gewählten Vorlese-Anbieters.",
                    werte: "\(VorleseEinstellungen.tempoMin)–\(VorleseEinstellungen.tempoMax)", lesen: { String(format: "%.2f", v.tempo(v.anbieter)) }) { w in
                guard let x = Double(w.replacingOccurrences(of: ",", with: ".")),
                      (VorleseEinstellungen.tempoMin...VorleseEinstellungen.tempoMax).contains(x) else { return "Zahl außerhalb des Bereichs" }
                v.setzeTempo(x, fuer: v.anbieter); return nil
            },
            Eintrag(kennung: "stimme", bereich: "Stimme", titel: "Stimme", beschreibung: "Stimme des gewählten Vorlese-Anbieters.",
                    werte: v.stimmenListe(v.anbieter).prefix(40).map(\.id).joined(separator: " | "), lesen: { v.stimme(v.anbieter) }) { w in
                guard let s = v.stimmenListe(v.anbieter).first(where: { $0.id == w || $0.name.lowercased().contains(w.lowercased()) }) else { return "Unbekannte Stimme" }
                v.setzeStimme(s.id, fuer: v.anbieter); return nil
            },
            Eintrag(kennung: "weckwort_an", bereich: "Sprache", titel: "Weckwort", beschreibung: "Auf das Weckwort lauschen.",
                    werte: "an | aus", lesen: { e.weckwortAn ? "an" : "aus" }) { v in
                guard let b = wahr(v) else { return "an oder aus" }
                e.weckwortAn = b; b ? Weckwort.shared.starten() : Weckwort.shared.stoppen(); return nil
            },
            stillezeit("weckwort_stille_app", "Stillezeit in der App", "Nach so viel Stille seit Franks letztem Wort schließt das Mikrofon, wenn das Gespräch bei Jarvis vorn begann und er noch vorn ist, per Weckwort, ⌥⌘J oder Live-Knopf. Sagt Frank nur „die Stillezeit“, ist diese gemeint.", \.stilleInAppSek),
            stillezeit("weckwort_stille_aussen", "Stillezeit außerhalb der App", "Nach so viel Stille seit Franks letztem Wort schließt das Mikrofon in allen anderen Fällen (Gespräch im Hintergrund begonnen, Jarvis nicht mehr vorn oder Bildschirm gesperrt), per Weckwort, ⌥⌘J oder Live-Knopf.", \.stilleAussenSek),
            Eintrag(kennung: "weckwort", bereich: "Sprache", titel: "Weckwort-Phrase", beschreibung: "z. B. „Hey Jarvis“ oder „Okay Computer“.",
                    werte: "Text", lesen: { e.weckwort }) { v in
                guard !v.isEmpty else { return "Leer geht nicht" }
                e.weckwort = v; Weckwort.shared.stoppen(); if e.weckwortAn { Weckwort.shared.starten() }; return nil
            },
            Eintrag(kennung: "transkription", bereich: "Sprache", titel: "Transkription", beschreibung: "Welche Spracherkennung das Diktat nutzt.",
                    werte: "parakeet | groq | apple", lesen: { e.transkription }) { v in
                let w = v.lowercased()
                guard ["apple", "groq", "parakeet"].contains(w) else { return "Erlaubt: parakeet, groq, apple" }
                e.transkription = w; return nil
            },
            Eintrag(kennung: "parakeet_woerterbuch_an", bereich: "Sprache", titel: "Parakeet-Wörterbuch", beschreibung: "Eigene Begriffe bei der Offline-Transkription mit Parakeet bevorzugen.",
                    werte: "an | aus", lesen: { TranskriptionsEinstellungen.shared.parakeetWoerterbuchAn ? "an" : "aus" }) { v in
                guard let b = wahr(v) else { return "an oder aus" }
                TranskriptionsEinstellungen.shared.parakeetWoerterbuchAn = b; return nil
            },
            Eintrag(kennung: "parakeet_begriffe", bereich: "Sprache", titel: "Parakeet-Begriffe", beschreibung: "Begriffe des Parakeet-Wörterbuchs. Ersetzt die ganze Liste.",
                    werte: "Begriffe durch Komma oder Zeilenumbruch getrennt, höchstens 100", lesen: { TranskriptionsEinstellungen.shared.parakeetWoerterbuch }) { v in
                TranskriptionsEinstellungen.shared.parakeetWoerterbuch = v; return nil
            },
            Eintrag(kennung: "tastenkuerzel", bereich: "Sprache", titel: "Globale Tastenkürzel", beschreibung: "⌥⌘J sprechen, ⌥⌘K Fenster.",
                    werte: "an | aus", lesen: { e.globalesKuerzel ? "an" : "aus" }) { v in
                guard let b = wahr(v) else { return "an oder aus" }
                e.globalesKuerzel = b; b ? AppSteuerung.shared.kuerzelRegistrieren() : AppSteuerung.shared.kuerzelAbmelden(); return nil
            },
            Eintrag(kennung: "zentrale_reihenfolge", bereich: "Zentrale & Bereiche", titel: "Reihenfolge der Zentrale", beschreibung: "Reihenfolge der Karten in der Zentrale (per Drag & Drop oder hier); „zuruecksetzen“ stellt die Werks-Reihenfolge her.",
                    werte: BereicheEinstellungen.zentraleKarten.map(\.id).joined(separator: ", ") + " (mit Komma) oder zuruecksetzen",
                    lesen: { ZentraleAnsicht.reihenfolge(BereicheEinstellungen.zentraleKarten.map(\.id)).joined(separator: ", ") }) { w in
                if ["zuruecksetzen", "zurücksetzen", "werk"].contains(w.trimmingCharacters(in: .whitespaces).lowercased()) { e.zentraleReihenfolge = ""; return nil }
                let ids = liste(w)
                let fremd = ids.filter { id in !BereicheEinstellungen.zentraleKarten.contains { $0.id == id } }
                if !fremd.isEmpty { return "Unbekannte Karte: " + fremd.joined(separator: ", ") + ". Erlaubt: " + BereicheEinstellungen.zentraleKarten.map(\.id).joined(separator: ", ") }
                e.zentraleReihenfolge = ohneDoppel(ids).joined(separator: ","); return nil
            },
            Eintrag(kennung: "zentrale_ausgeblendet", bereich: "Zentrale & Bereiche", titel: "Ausgeblendete Karten der Zentrale", beschreibung: "Diese Karten erscheinen nicht in der Zentrale; leer bzw. „keine“ zeigt alle.",
                    werte: BereicheEinstellungen.zentraleKarten.map(\.id).joined(separator: ", ") + " (mit Komma) oder keine",
                    lesen: { e.zentraleAusgeblendet.isEmpty ? "keine" : e.zentraleAusgeblendet }) { w in
                let t = w.trimmingCharacters(in: .whitespaces).lowercased()
                let ids = t == "keine" || t.isEmpty ? [] : liste(w)
                let fremd = ids.filter { id in !BereicheEinstellungen.zentraleKarten.contains { $0.id == id } }
                if !fremd.isEmpty { return "Unbekannte Karte: " + fremd.joined(separator: ", ") }
                e.zentraleAusgeblendet = ohneDoppel(ids).joined(separator: ","); return nil
            },
            Eintrag(kennung: "listen_abgleich", bereich: "Zentrale & Bereiche", titel: "Listen mit Handy und Tablet abgleichen", beschreibung: "Ablage → Listen gleichen sich mit Handy und Tablet ab; aus = nur die Kopie am Mac.",
                    werte: "an | aus", lesen: { e.listenAbgleichAn ? "an" : "aus" }) { v in
                guard let b = wahr(v) else { return "an oder aus" }
                e.listenAbgleichAn = b; ListenSpeicher.shared.jetztAbgleichen(); return nil
            },
            Eintrag(kennung: "biomarker_zeitraum", bereich: "Zentrale & Bereiche", titel: "Biomarker-Zeitraum", beschreibung: "Vergleich und Verlauf im Biomarker-Bereich in Tagen.",
                    werte: "7 | 30 | 90", lesen: { String(e.biomarkerTage) }) { v in
                guard let t = Int(v.trimmingCharacters(in: .whitespaces)), [7, 30, 90].contains(t) else { return "Erlaubt: 7, 30 oder 90" }
                e.biomarkerTage = t
                Task { await BioDaten.shared.laden(tage: t) }
                return nil
            },
            Eintrag(kennung: "biomarker_beim_start", bereich: "Zentrale & Bereiche", titel: "Biomarker beim Öffnen der Zentrale lesen", beschreibung: "Liest die Biomarker vom Handy, wenn die Zentrale aufgeht (höchstens alle fünf Minuten, nie direkt bei den Quellen).",
                    werte: "an | aus", lesen: { e.biomarkerBeimStart ? "an" : "aus" }) { v in
                guard let b = wahr(v) else { return "an oder aus" }; e.biomarkerBeimStart = b; return nil
            },
            Eintrag(kennung: "solar_beim_start", bereich: "Zentrale & Bereiche", titel: "Solarbatterie beim Start lesen", beschreibung: "Liest die Solarbatterie einmal je App-Start, sobald die Zentrale aufgeht.",
                    werte: "an | aus", lesen: { e.solarBeimStart ? "an" : "aus" }) { v in
                guard let b = wahr(v) else { return "an oder aus" }; e.solarBeimStart = b; return nil
            },
            Eintrag(kennung: "solar_takt", bereich: "Zentrale & Bereiche", titel: "Solarbatterie regelmäßig lesen", beschreibung: "Zusätzlich alle so vielen Minuten, solange Jarvis läuft; 0 = nur beim Start und per Knopf.",
                    werte: "0 | 5 | 15 | 30 | 60", lesen: { String(e.solarTaktMinuten) }) { v in
                guard let m = Int(v.trimmingCharacters(in: .whitespaces)), [0, 5, 15, 30, 60].contains(m) else { return "Erlaubt: 0, 5, 15, 30 oder 60" }
                e.solarTaktMinuten = m; return nil
            },
            Eintrag(kennung: "darstellung", bereich: "Darstellung", titel: "Darstellung", beschreibung: "Hell, dunkel oder automatisch nach Sonnenauf- und -untergang in Berlin.",
                    werte: "hell | automatik | dunkel", lesen: { DesignStand.shared.modus.rawValue }) { v in
                guard let m = DesignStand.Modus(rawValue: v.lowercased()) else { return "hell, automatik oder dunkel" }
                DesignStand.shared.modus = m; return nil
            },
            Eintrag(kennung: "vollzugriff", bereich: "Mac", titel: "Vollzugriff", beschreibung: "Jarvis darf am Mac alles ohne Rückfrage.",
                    werte: "nur lesen", lesen: { e.vollzugriff ? "an" : "aus" }) { _ in
                // Bewusst nicht per Werkzeug änderbar: sonst könnte das Modell eine ausgeschaltete Sperre selbst aufheben.
                "Den Vollzugriff schaltet nur Frank selbst um (Einstellungen → Mac & Rechte)."
            },
        ]
    }
}

extension MacWerkzeuge {
    static let weitere: [LokalesWerkzeug] = [
        LokalesWerkzeug(definition: WerkzeugDefinition(
            name: "mac_einstellungen",
            beschreibung: "Auch die Darstellung (Kennung darstellung: hell, automatik, dunkel). Jarvis am Mac: eigene Einstellungen lesen (aktion lesen, optional bereich) oder ändern (aktion aendern mit kennung und wert): Modell, Denkstufe, Denkstufe beim Sprechen, Schrittlimit, Lernen, Vorlesen, Tempo, Stimme, Weckwort, Transkription, Parakeet-Wörterbuch, Tastenkürzel, Zentrale (Reihenfolge, ausgeblendete Karten), Listen-Abgleich, Biomarker (Zeitraum, beim Start), Solarbatterie (beim Start, Takt), Vollzugriff. Für die Einstellungen des Handys bleibt das Werkzeug einstellungen.",
            schema: ["type": "object", "properties": [
                "aktion": ["type": "string", "enum": ["lesen", "aendern"]],
                "bereich": ["type": "string", "description": "optional: nur dieser Bereich"],
                "kennung": ["type": "string"], "wert": ["type": "string"],
            ] as JSONObjekt, "required": ["aktion"]])) { a in
            await MainActor.run {
                let katalog = MacEinstellungsKatalog.alle
                if (a["aktion"] as? String) == "aendern" {
                    let k = a["kennung"] as? String ?? ""
                    guard let e = katalog.first(where: { $0.kennung == k }) else { return WerkzeugErgebnis(text: "Unbekannte Kennung \(k)", fehler: true) }
                    if let f = e.setzen((a["wert"] as? String ?? "").trimmingCharacters(in: .whitespaces)) {
                        return WerkzeugErgebnis(text: "\(e.titel): \(f). Mögliche Werte: \(e.werte)", fehler: true)
                    }
                    return WerkzeugErgebnis(text: "\(e.titel) steht jetzt auf \(e.lesen()).")
                }
                let b = (a["bereich"] as? String ?? "").lowercased()
                return WerkzeugErgebnis(text: katalog.filter { b.isEmpty || $0.bereich.lowercased().contains(b) }.map {
                    "\($0.kennung) [\($0.bereich)] \($0.titel) = \($0.lesen())\($0.beschreibung.isEmpty ? "" : " – \($0.beschreibung)") (Werte: \($0.werte))"
                }.joined(separator: "\n"))
            }
        },
        LokalesWerkzeug(definition: WerkzeugDefinition(
            name: "mac_funktionen_lesen",
            beschreibung: "Jarvis am Mac: Übersicht, was die Mac-Version kann, dazu die Reiter der Handy-App, die Zusammenarbeit mit dem CLI und die Regeln für ChatGPT Live (nur bei Bedarf, z. B. „Kannst du am Mac …?“). Für die vollständigen Funktionen des Handys funktionen_lesen.",
            schema: ["type": "object", "properties": [:] as JSONObjekt])) { _ in
            WerkzeugErgebnis(text: Bundle.main.mitgeliefert("funktionen") + "\n\n" + Bundle.main.mitgeliefert("jarvis-wissen"))
        },
    ]

    static var alleLokal: [LokalesWerkzeug] { alle + weitere + MacDialoge.werkzeuge + RepoWerkzeuge.alle + AgentenKanal.werkzeuge + HelferWerkzeuge.alle }
}
