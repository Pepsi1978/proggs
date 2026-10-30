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
            Eintrag(kennung: "schrittlimit", bereich: "Jarvis' Kopf", titel: "Schrittlimit", beschreibung: "Werkzeugschritte je Anfrage, 0 = unbegrenzt.",
                    werte: "0–200", lesen: { "\(e.schrittLimit)" }) { v in
                guard let n = Int(v), (0...200).contains(n) else { return "Zahl 0–200" }
                e.schrittLimit = n; return nil
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
            Eintrag(kennung: "tastenkuerzel", bereich: "Sprache", titel: "Globale Tastenkürzel", beschreibung: "⌥⌘J sprechen, ⌥⌘K Fenster.",
                    werte: "an | aus", lesen: { e.globalesKuerzel ? "an" : "aus" }) { v in
                guard let b = wahr(v) else { return "an oder aus" }
                e.globalesKuerzel = b; b ? AppSteuerung.shared.kuerzelRegistrieren() : AppSteuerung.shared.kuerzelAbmelden(); return nil
            },
            Eintrag(kennung: "live_anbieter", bereich: "Live-Gespräch", titel: "Live-Anbieter", beschreibung: "OpenAI oder Gemini.",
                    werte: "openai | gemini", lesen: { e.liveAnbieter }) { v in
                guard ["openai", "gemini"].contains(v) else { return "openai oder gemini" }; e.liveAnbieter = v; return nil
            },
            Eintrag(kennung: "live_modell_openai", bereich: "Live-Gespräch", titel: "OpenAI-Live-Modell", beschreibung: "",
                    werte: LiveOptionen.openAIModelle.map(\.id).joined(separator: " | "), lesen: { e.liveModellOpenAI }) { v in e.liveModellOpenAI = v; return nil },
            Eintrag(kennung: "live_stimme_openai", bereich: "Live-Gespräch", titel: "OpenAI-Live-Stimme", beschreibung: "",
                    werte: LiveOptionen.liveStimmen.map(\.id).joined(separator: " | "), lesen: { e.liveStimmeOpenAI }) { v in e.liveStimmeOpenAI = v; return nil },
            Eintrag(kennung: "live_modell_gemini", bereich: "Live-Gespräch", titel: "Gemini-Live-Modell", beschreibung: "",
                    werte: LiveOptionen.geminiModelle.map(\.id).joined(separator: " | "), lesen: { e.liveModellGemini }) { v in e.liveModellGemini = v; return nil },
            Eintrag(kennung: "live_stimme_gemini", bereich: "Live-Gespräch", titel: "Gemini-Live-Stimme", beschreibung: "",
                    werte: LiveOptionen.geminiStimmen.map(\.id).joined(separator: " | "), lesen: { e.liveStimmeGemini }) { v in e.liveStimmeGemini = v; return nil },
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
            beschreibung: "Jarvis am Mac: eigene Einstellungen lesen (aktion lesen, optional bereich) oder ändern (aktion aendern mit kennung und wert): Modell, Denkstufe, Schrittlimit, Lernen, Vorlesen, Tempo, Stimme, Weckwort, Transkription, Tastenkürzel, Live-Anbieter/-Modell/-Stimme, Vollzugriff. Für die Einstellungen des Handys bleibt das Werkzeug einstellungen.",
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
            let t = ["funktionen", "jarvis-wissen"].map { name in
                Bundle.main.url(forResource: name, withExtension: "md").flatMap { try? String(contentsOf: $0, encoding: .utf8) } ?? "\(name).md fehlt im App-Bundle."
            }
            return WerkzeugErgebnis(text: t.joined(separator: "\n\n"))
        },
    ]

    static var alleLokal: [LokalesWerkzeug] { alle + weitere + RepoWerkzeuge.alle }
}
