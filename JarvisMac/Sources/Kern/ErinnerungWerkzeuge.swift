import Foundation

/// erinnerung_speichern und erinnerungen_lesen laufen am Mac direkt auf der gemeinsamen Ablage, mit denselben Aktionen und Antworten wie ErinnerungenFaehigkeit am Handy. Die Schemata kommen vom Handy.
@MainActor
enum ErinnerungWerkzeuge {
    private static var abgleich: AblageAbgleich { AblageAbgleich.shared }
    private static var planer: ErinnerungPlaner { ErinnerungPlaner.shared }
    private static func jetzt() -> Int64 { Int64(Date().timeIntervalSince1970 * 1000) }

    private static func wert(_ a: JSONObjekt, _ k: String) -> String {
        if let s = a[k] as? String { return s }
        if let n = a[k] as? NSNumber { return n.stringValue }
        return ""
    }
    private static func gesetzt(_ a: JSONObjekt, _ k: String) -> Bool { !wert(a, k).trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
    private static func sauber(_ a: JSONObjekt, _ k: String) -> String { wert(a, k).trimmingCharacters(in: .whitespacesAndNewlines) }
    private static func fehler(_ t: String) -> WerkzeugErgebnis { WerkzeugErgebnis(text: t, fehler: true) }

    private static func finde(_ a: JSONObjekt) -> (Erinnerung?, String?) {
        let alle = abgleich.erinnerungen
        if gesetzt(a, "id") {
            let k = wert(a, "id")
            if let e = alle.first(where: { $0.id == k }) { return (e, nil) }
            return (nil, "Diese Erinnerung gibt es nicht.")
        }
        let wort = sauber(a, "suche").lowercased()
        if wort.isEmpty { return (nil, "Es fehlt id oder suche.") }
        let treffer = alle.filter { $0.zeile.lowercased().contains(wort) || $0.kurz.lowercased().contains(wort) || $0.text.lowercased().contains(wort) }
        if treffer.count == 1 { return (treffer[0], nil) }
        if treffer.isEmpty { return (nil, "Keine Erinnerung passt zu „\(wort)“.") }
        return (nil, "Mehrere passen, bitte genauer: " + treffer.prefix(8).map(\.zeile).joined(separator: "; "))
    }

    private static let datumMuster = try? NSRegularExpression(pattern: "(\\d{1,2})\\.(\\d{1,2})\\.(\\d{2,4})")
    private static let uhrMuster = try? NSRegularExpression(pattern: "^\\s*(2[0-3]|[01]?\\d)(?:[:.]?([0-5]\\d))?(?!\\d)")

    /// JJJJ-MM-TT oder T.M.JJ(JJ) irgendwo im Text.
    private static func datumAus(_ t: String) -> String? {
        let s = t.trimmingCharacters(in: .whitespacesAndNewlines)
        if let d = ErinnerungZeit.tag(s) { return d.text }
        let ns = s as NSString
        guard let m = datumMuster?.firstMatch(in: s, range: NSRange(location: 0, length: ns.length)),
              let tag = Int(ns.substring(with: m.range(at: 1))), let monat = Int(ns.substring(with: m.range(at: 2))) else { return nil }
        let j = ns.substring(with: m.range(at: 3))
        guard let jahr = Int(j) else { return nil }
        return ErinnerungZeit.gueltig(jahr: j.count == 2 ? 2000 + jahr : jahr, monat: monat, tag: tag)?.text
    }

    private static func uhrzeitAus(_ t: String) -> String? {
        guard !t.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return nil }
        let ns = t as NSString
        guard let m = uhrMuster?.firstMatch(in: t, range: NSRange(location: 0, length: ns.length)), let h = Int(ns.substring(with: m.range(at: 1))) else { return nil }
        let r = m.range(at: 2)
        let mi = r.location == NSNotFound ? 0 : Int(ns.substring(with: r)) ?? 0
        return String(format: "%02d:%02d", h, mi)
    }

    private static func vorabAus(_ t: String) -> [Int] {
        var r: [Int] = []
        for teil in t.split(whereSeparator: { $0 == "," || $0 == ";" }) {
            if let m = Int(teil.trimmingCharacters(in: .whitespaces)), m >= 0, !r.contains(m) { r.append(m) }
        }
        return r
    }

    private static func kopf(_ e: Erinnerung) -> String {
        "\(e.datumAnzeige): \(e.zeile) [id \(e.id)]" + (e.wecker ? " (Wecker)" : "") + (e.erledigt != 0 ? " (erledigt)" : "")
    }

    private static func stand(_ f: Faellig, _ jetzt: Int64) -> String {
        let s: String
        if let q = planer.quittung(f.quittung) { s = "bestätigt auf \(q.von)" }
        else if f.zeit > jetzt { s = "geplant" }
        else if !f.meldbar(jetzt) { s = "verpasst oder beim Einstellen schon vorbei" }
        else { s = "fällig, noch nicht bestätigt" }
        return "- \(f.anzeige): \(ErinnerungZeit.zeitText(f.zeit)) — \(s)" + (f.ausloesung.map { " [ausloesung_id \($0.id)]" } ?? "")
    }

    private static func voll(_ e: Erinnerung) -> String {
        let jetzt = Self.jetzt()
        var t = kopf(e) + "\nKurzfassung: \(e.kurz)\n\n\(e.text)"
        if e.allgemein {
            t += "\n\nAllgemeine Erinnerung ohne Termin."
            if e.zuletztErwaehnt > 0 { t += " Zuletzt erwähnt: \(ErinnerungZeit.zeitText(e.zuletztErwaehnt))." }
        }
        let termine = e.termine()
        if !termine.isEmpty { t += "\n\nZeitpunkte:\n" + termine.map { stand($0, jetzt) }.joined(separator: "\n") }
        return t
    }

    static func ausfuehren(_ name: String, _ a: JSONObjekt) async throws -> WerkzeugErgebnis {
        if name == "erinnerungen_lesen" { return lesen(a) }
        return try await speichern(a)
    }

    private static func lesen(_ a: JSONObjekt) -> WerkzeugErgebnis {
        if gesetzt(a, "id") || gesetzt(a, "suche") {
            let (e, f) = finde(a)
            if let e { return WerkzeugErgebnis(text: voll(e)) }
            return fehler(f ?? "")
        }
        let art = gesetzt(a, "art") ? wert(a, "art") : "alle"
        let jetzt = Self.jetzt()
        let mit = a["mit_erledigten"] as? Bool == true || wert(a, "mit_erledigten") == "true"
        let alle = abgleich.erinnerungen
        let datiert = art == "allgemein" ? [] : Erinnerung.datiert(alle, jetzt: jetzt).filter { e in
            mit || (e.erledigt == 0 && (e.termin ?? 0) >= jetzt) || e.termine().contains { t in planer.quittung(t.quittung) == nil && t.zeit <= jetzt && t.meldbar(jetzt) }
        }
        let allgemein = art == "datiert" ? [] : Erinnerung.allgemeine(alle).filter { mit || $0.erledigt == 0 }
        if datiert.isEmpty && allgemein.isEmpty { return WerkzeugErgebnis(text: "Keine Erinnerungen.") }
        var t = ""
        if !datiert.isEmpty { t += "Mit Datum:\n" + datiert.map { "- " + kopf($0) }.joined(separator: "\n") + "\n" }
        if !allgemein.isEmpty { t += "Allgemein:\n" + allgemein.map { "- \($0.zeile) [id \($0.id)]" + ($0.erledigt != 0 ? " (erledigt)" : "") }.joined(separator: "\n") }
        return WerkzeugErgebnis(text: t.trimmingCharacters(in: .whitespacesAndNewlines))
    }

    private static func speichern(_ a: JSONObjekt) async throws -> WerkzeugErgebnis {
        let aktion = wert(a, "aktion")
        if aktion == "anlegen" { return try await anlegen(a) }
        let (gefunden, f) = finde(a)
        guard let e = gefunden else { return fehler(f ?? "") }
        switch aktion {
        case "aendern":
            var datum = e.datum
            if gesetzt(a, "datum") {
                guard let d = datumAus(wert(a, "datum")) else { return fehler("„\(wert(a, "datum"))“ ist kein Datum.") }
                datum = d
            }
            var uhrzeit = e.uhrzeit
            if gesetzt(a, "uhrzeit") {
                if sauber(a, "uhrzeit").lowercased() == "keine" { uhrzeit = nil } else {
                    guard let u = uhrzeitAus(wert(a, "uhrzeit")) else { return fehler("„\(wert(a, "uhrzeit"))“ ist keine Uhrzeit.") }
                    uhrzeit = u
                }
            }
            let form = [Erinnerung.formWecker, Erinnerung.formMeldung].contains(wert(a, "form")) ? wert(a, "form") : e.form
            let text = sauber(a, "text"), kurz = sauber(a, "kurz"), zeile = sauber(a, "zeile")
            let neu = try await abgleich.erinnerungAendern(e.id) { alt in
                var n = alt
                if !text.isEmpty { n.text = text }
                if !kurz.isEmpty { n.kurz = kurz }
                if !zeile.isEmpty { n.zeile = zeile }
                n.datum = datum; n.uhrzeit = uhrzeit; n.form = form; n.allgemein = datum == nil
                // Nur beim Wechsel von allgemein zu datiert: Bewusst entfernte Zeitpunkte bleiben entfernt.
                if datum != nil && alt.allgemein { n.ausloesungen = [Erinnerung.neueAusloesung(0)] }
                return n
            }
            return WerkzeugErgebnis(text: "Geändert: \(neu.datumAnzeige), \(neu.zeile), Form \(neu.wecker ? "Wecker" : "Benachrichtigung"). Das ist keine Bestätigung." + (neu.ausloesungen.isEmpty ? "" : vorbeiHinweis(neu)))
        case "loeschen":
            try await abgleich.erinnerungLoeschen(e.id)
            return WerkzeugErgebnis(text: "Gelöscht: \(e.zeile).")
        case "ausloesung_hinzufuegen":
            if e.allgemein { return fehler("Das ist eine allgemeine Erinnerung ohne Termin; dafür gibt es keine Zeitpunkte. Gib ihr erst ein Datum (aendern).") }
            var u: String?
            if gesetzt(a, "ausloesung_uhrzeit") {
                guard let v = uhrzeitAus(wert(a, "ausloesung_uhrzeit")) else { return fehler("Keine Uhrzeit: \(wert(a, "ausloesung_uhrzeit"))") }
                u = v
            }
            if a["minuten"] == nil || a["minuten"] is NSNull { return fehler("Es fehlt minuten: wie lange vor dem Termin.") }
            let neu = Erinnerung.neueAusloesung(max(0, Int(AblageAbgleich.zahl(a["minuten"]))), uhrzeit: u)
            guard let z = e.zeitVon(neu) else { return fehler("Der Termin ist nicht lesbar.") }
            if z <= jetzt() { return fehler("Dieser Zeitpunkt (\(ErinnerungZeit.zeitText(z))) ist schon vorbei, nichts eingestellt.") }
            if e.doppelt(neu) { return WerkzeugErgebnis(text: "Dieser Zeitpunkt (\(ErinnerungZeit.zeitText(z))) ist schon eingestellt.") }
            try await abgleich.erinnerungAendern(e.id) { alt in var n = alt; n.ausloesungen.append(neu); return n }
            return WerkzeugErgebnis(text: "Zusätzlich erinnere ich dich \(e.vorlaufText(neu)): \(ErinnerungZeit.zeitText(z)).")
        case "ausloesung_entfernen":
            let id = wert(a, "ausloesung_id")
            guard e.ausloesungen.contains(where: { $0.id == id }) else { return fehler("Diesen Zeitpunkt gibt es nicht.") }
            try await abgleich.erinnerungAendern(e.id) { alt in var n = alt; n.ausloesungen.removeAll { $0.id == id }; return n }
            return WerkzeugErgebnis(text: "Zeitpunkt entfernt.")
        case "bestaetigen":
            let jetzt = Self.jetzt()
            let offen = e.termine().filter { planer.quittung($0.quittung) == nil }
            let ziel: Faellig
            if gesetzt(a, "ausloesung_id") {
                let k = wert(a, "ausloesung_id")
                guard let t = offen.first(where: { $0.ausloesung?.id == k }) else { return fehler("Dieser Zeitpunkt ist nicht offen.") }
                ziel = t
            } else {
                // Ohne Angabe nur, was gerade wirklich fällig ist: Nie eine tote oder eine künftige Auslösung statt des klingelnden Weckers.
                guard let t = offen.first(where: { $0.zeit <= jetzt && $0.meldbar(jetzt) }) else {
                    return WerkzeugErgebnis(text: "Bei dieser Erinnerung ist gerade nichts fällig. Einen künftigen Zeitpunkt bestätigst du nur mit ausloesung_id.")
                }
                ziel = t
            }
            planer.bestaetige(ziel)
            let rest = offen.filter { $0.zeit > jetzt }.count
            return WerkzeugErgebnis(text: "Bestätigt: \(ziel.anzeige). " + (rest > 0 ? "\(rest) weitere Zeitpunkte bleiben geplant." : "Weitere Zeitpunkte sind nicht geplant."))
        case "erledigt":
            let zeit = jetzt()
            try await abgleich.erinnerungAendern(e.id) { alt in var n = alt; n.erledigt = zeit; return n }
            return WerkzeugErgebnis(text: "Als erledigt markiert: \(e.zeile).")
        case "wieder_offen":
            try await abgleich.erinnerungAendern(e.id) { alt in var n = alt; n.erledigt = 0; return n }
            return WerkzeugErgebnis(text: "Wieder offen: \(e.zeile). Schon vergangene Zeitpunkte melden sich nicht nachträglich.")
        case "erwaehnt":
            let zeit = jetzt()
            try await abgleich.erinnerungAendern(e.id) { alt in var n = alt; n.zuletztErwaehnt = zeit; return n }
            return WerkzeugErgebnis(text: "Vermerkt.")
        default:
            return fehler("Unbekannte aktion „\(aktion)“.")
        }
    }

    private static func anlegen(_ a: JSONObjekt) async throws -> WerkzeugErgebnis {
        let text = sauber(a, "text")
        if text.isEmpty { return fehler("Der Inhalt fehlt.") }
        let form = wert(a, "form")
        guard form == Erinnerung.formMeldung || form == Erinnerung.formWecker else { return fehler("Frag Frank ausdrücklich „Benachrichtigung oder Wecker?“ und übergib seine Auswahl in form.") }
        var datum: String?
        if gesetzt(a, "datum") {
            guard let d = datumAus(wert(a, "datum")) else { return fehler("„\(wert(a, "datum"))“ ist kein Datum.") }
            datum = d
        }
        var uhrzeit: String?
        if datum != nil, gesetzt(a, "uhrzeit") {
            guard let u = uhrzeitAus(wert(a, "uhrzeit")) else { return fehler("„\(wert(a, "uhrzeit"))“ ist keine Uhrzeit.") }
            uhrzeit = u
        }
        let vorab = vorabAus(wert(a, "vorab_minuten")).map { Erinnerung.neueAusloesung($0) }
        let e = try await abgleich.erinnerungAnlegen(text: text, zeile: wert(a, "zeile"), kurz: wert(a, "kurz"), datum: datum, uhrzeit: uhrzeit, form: form, vorab: vorab)
        if e.allgemein { return WerkzeugErgebnis(text: "Als allgemeine Erinnerung gespeichert: \(e.zeile). Jarvis meldet sich, wenn es passt.") }
        let zeiten = e.ausloesungen.map { e.vorlaufText($0) }.joined(separator: ", ")
        return WerkzeugErgebnis(text: "Gespeichert für \(e.datumAnzeige): \(e.zeile). Form: \(e.wecker ? "Wecker" : "Benachrichtigung"). Zeitpunkte: \(zeiten)." + vorbeiHinweis(e))
    }

    private static func vorbeiHinweis(_ e: Erinnerung) -> String {
        e.meldetSichNoch(jetzt()) ? "" : " ACHTUNG: Alle Zeitpunkte sind schon vorbei, von selbst meldet sie sich nicht. Sag das Frank und frag nach einer Uhrzeit."
    }
}
