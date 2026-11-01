import Foundation

/// Rechnet wie java.time am Handy in lokaler Wanduhrzeit: Die Kennung einer Auslösung enthält die Zeit und muss auf allen Geräten gleich herauskommen.
/// Darum bewusst ohne Calendar-Arithmetik: Bei Zeitumstellungen löst Foundation nicht überall so auf wie java.time.
enum ErinnerungZeit {
    struct Tag: Hashable, Sendable {
        let jahr: Int, monat: Int, tag: Int
        var text: String { String(format: "%04d-%02d-%02d", jahr, monat, tag) }
    }

    private static let monate = ["Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember"]
    private static let wochentage = ["So", "Mo", "Di", "Mi", "Do", "Fr", "Sa"]

    /// Streng JJJJ-MM-TT wie LocalDate.parse; ein Datum, das es nicht gibt, ist nil.
    static func tag(_ s: String) -> Tag? {
        let t = s.split(separator: "-", omittingEmptySubsequences: false)
        guard t.count == 3, t[0].count == 4, t[1].count == 2, t[2].count == 2, let j = Int(t[0]), let m = Int(t[1]), let d = Int(t[2]) else { return nil }
        return gueltig(jahr: j, monat: m, tag: d)
    }

    static func gueltig(jahr: Int, monat: Int, tag: Int) -> Tag? {
        guard (1...12).contains(monat), (1...31).contains(tag) else { return nil }
        let t = Tag(jahr: jahr, monat: monat, tag: tag)
        return ausTagen(tage(t)) == t ? t : nil
    }

    /// Tage seit dem 1.1.1970 im gregorianischen Kalender.
    static func tage(_ t: Tag) -> Int {
        let y = t.jahr - (t.monat <= 2 ? 1 : 0)
        let era = (y >= 0 ? y : y - 399) / 400
        let yoe = y - era * 400
        let doy = (153 * (t.monat + (t.monat > 2 ? -3 : 9)) + 2) / 5 + t.tag - 1
        let doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146_097 + doe - 719_468
    }

    static func ausTagen(_ tage: Int) -> Tag {
        let z = tage + 719_468
        let era = (z >= 0 ? z : z - 146_096) / 146_097
        let doe = z - era * 146_097
        let yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365
        let doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        let mp = (5 * doy + 2) / 153
        let d = doy - (153 * mp + 2) / 5 + 1
        let m = mp < 10 ? mp + 3 : mp - 9
        return Tag(jahr: yoe + era * 400 + (m <= 2 ? 1 : 0), monat: m, tag: d)
    }

    /// Minuten seit Mitternacht; was keine Uhrzeit ist, gilt wie am Handy als 10:00.
    static func uhr(_ s: String) -> Int {
        let t = s.split(separator: ":", omittingEmptySubsequences: false)
        guard t.count >= 2, t[0].count == 2, t[1].count == 2, let h = Int(t[0]), let m = Int(t[1]), (0...23).contains(h), (0...59).contains(m) else { return 600 }
        return h * 60 + m
    }

    /// Wanduhrzeit (Minuten seit 1.1.1970 00:00, als ob es keine Zeitzone gäbe) nach ms, aufgelöst wie LocalDateTime.atZone:
    /// Doppelte Stunde = die frühere, ausgefallene Stunde = um deren Länge nach vorn.
    static func ms(wandMinuten: Int) -> Int64 {
        let naiv = Int64(wandMinuten) * 60
        let zone = TimeZone.current
        func versatz(_ sek: Int64) -> Int64 { Int64(zone.secondsFromGMT(for: Date(timeIntervalSince1970: Double(sek)))) }
        let vor = versatz(naiv - 86_400), nach = versatz(naiv + 86_400)
        let a = naiv - vor, b = naiv - nach
        let passtA = versatz(a) == vor, passtB = versatz(b) == nach
        let sek: Int64
        if passtA && passtB { sek = min(a, b) } else if passtB { sek = b } else { sek = a }
        return sek * 1000
    }

    /// „Mo, 12. August 2027“
    static func anzeige(_ t: Tag) -> String {
        let w = ((tage(t) + 4) % 7 + 7) % 7
        return "\(wochentage[w]), \(t.tag). \(monate[t.monat - 1]) \(t.jahr)"
    }

    private static var kalender: Calendar {
        var c = Calendar(identifier: .gregorian)
        c.timeZone = TimeZone.current
        return c
    }

    /// „Mo, 12. August 2027, 10:00 Uhr“
    static func zeitText(_ ms: Int64) -> String {
        let k = kalender.dateComponents([.year, .month, .day, .hour, .minute], from: Date(timeIntervalSince1970: Double(ms) / 1000))
        let t = Tag(jahr: k.year ?? 1970, monat: k.month ?? 1, tag: k.day ?? 1)
        return anzeige(t) + String(format: ", %02d:%02d Uhr", k.hour ?? 0, k.minute ?? 0)
    }

    // Für die Auswahlfelder der Oberfläche.

    static func datum(_ s: String?) -> Date? {
        guard let s, let t = tag(s) else { return nil }
        return kalender.date(from: DateComponents(year: t.jahr, month: t.monat, day: t.tag, hour: 12))
    }

    static func datumText(_ d: Date) -> String {
        let k = kalender.dateComponents([.year, .month, .day], from: d)
        return Tag(jahr: k.year ?? 1970, monat: k.month ?? 1, tag: k.day ?? 1).text
    }

    static func uhrDatum(_ s: String) -> Date {
        let m = uhr(s)
        return kalender.date(bySettingHour: m / 60, minute: m % 60, second: 0, of: Date()) ?? Date()
    }

    static func uhrText(_ d: Date) -> String {
        let k = kalender.dateComponents([.hour, .minute], from: d)
        return String(format: "%02d:%02d", k.hour ?? 0, k.minute ?? 0)
    }
}

/// Ein eingestellter Erinnerungszeitpunkt: minuten vor dem Termin; mit uhrzeit (nur Termine ohne Uhrzeit) minuten / 1440 Tage vorher um diese Zeit.
struct Ausloesung: Identifiable, Hashable, Sendable {
    let id: String
    let minuten: Int
    var uhrzeit: String? = nil
    var angelegt: Int64 = 0
}

/// Wie Erinnerung am Handy (Erinnerungen.kt): zeile = Listenzeile, kurz = Vorlesefassung, text = voller Inhalt.
/// uhrzeit nil: Frank hat keine genannt; sie löst um ohneUhrzeitUm aus, angezeigt wird nur das Datum.
struct Erinnerung: Identifiable, Hashable, Sendable {
    static let formMeldung = "meldung"
    static let formWecker = "wecker"
    /// Älteres, nie Bestätigtes meldet sich nicht mehr von selbst.
    static let verpasstNachMs: Int64 = 7 * 24 * 3_600_000

    let id: String
    var allgemein: Bool
    var zeile: String
    var kurz: String
    var text: String
    var datum: String? = nil
    var uhrzeit: String? = nil
    var ohneUhrzeitUm = "10:00"
    var form = "meldung"
    var ausloesungen: [Ausloesung] = []
    var erledigt: Int64 = 0
    var hinweisUm: Int64 = 0
    var zuletztErwaehnt: Int64 = 0
    var quelle = ""
    var erstellt: Int64 = 0
    var geaendert: Int64 = 0

    var wecker: Bool { form == Self.formWecker }
    var tag: ErinnerungZeit.Tag? { datum.flatMap { ErinnerungZeit.tag($0) } }

    /// Der Termin selbst, zum Ordnen und Rechnen; bei reinem Datum die Uhrzeit, zu der er auslöst.
    var termin: Int64? {
        guard let t = tag else { return nil }
        return ErinnerungZeit.ms(wandMinuten: ErinnerungZeit.tage(t) * 1440 + ErinnerungZeit.uhr(uhrzeit ?? ohneUhrzeitUm))
    }

    func zeitVon(_ a: Ausloesung) -> Int64? {
        guard let t = tag else { return nil }
        let tage = ErinnerungZeit.tage(t)
        if let u = a.uhrzeit { return ErinnerungZeit.ms(wandMinuten: (tage - a.minuten / 1440) * 1440 + ErinnerungZeit.uhr(u)) }
        return ErinnerungZeit.ms(wandMinuten: tage * 1440 + ErinnerungZeit.uhr(uhrzeit ?? ohneUhrzeitUm) - a.minuten)
    }

    /// Alle Auslösungen. Die Kennung trägt die Zeit: Wird der Termin verschoben, ist es eine neue Auslösung; bloßes Bearbeiten des Textes lässt Bestätigtes bestätigt.
    func termine() -> [Faellig] {
        if allgemein {
            return hinweisUm > 0 ? [Faellig(erinnerung: self, quittung: "\(id)_h_\(hinweisUm / 60_000)", zeit: hinweisUm, ausloesung: nil, anzeige: "Hinweis von Jarvis")] : []
        }
        return ausloesungen.compactMap { a in
            zeitVon(a).map { z in Faellig(erinnerung: self, quittung: "\(id)_\(a.id)_\(z / 60_000)", zeit: z, ausloesung: a, anzeige: vorlaufText(a)) }
        }.sorted { $0.zeit < $1.zeit }
    }

    func vorlaufText(_ a: Ausloesung) -> String {
        func tageText(_ n: Int) -> String { n == 1 ? "1 Tag" : "\(n) Tage" }
        func stundenText(_ n: Int) -> String { n == 1 ? "1 Stunde" : "\(n) Stunden" }
        if let u = a.uhrzeit { return (a.minuten == 0 ? "Am Termintag" : tageText(a.minuten / 1440) + " vorher") + " um \(u) Uhr" }
        if a.minuten == 0 { return uhrzeit != nil ? "Zum Termin" : "Am Termintag" }
        if a.minuten % 1440 == 0 { return tageText(a.minuten / 1440) + " vorher" }
        if a.minuten % 60 == 0 { return stundenText(a.minuten / 60) + " vorher" }
        return "\(a.minuten) Minuten vorher"
    }

    /// Erste Zeile des Eintrags: Datum und nur eine ausdrücklich genannte Uhrzeit.
    var datumAnzeige: String {
        guard let t = tag else { return "Ohne festen Termin" }
        return ErinnerungZeit.anzeige(t) + (uhrzeit.map { " · \($0) Uhr" } ?? "")
    }

    // MARK: Dokument (Schlüssel exakt wie am Handy)

    func alsJson() -> JSONObjekt {
        func nullbar(_ s: String?) -> Any { if let s { return s }; return NSNull() }
        let liste: [JSONObjekt] = ausloesungen.map { ["id": $0.id, "minuten": $0.minuten, "uhrzeit": nullbar($0.uhrzeit), "angelegt": $0.angelegt] }
        return ["allgemein": allgemein, "zeile": zeile, "kurz": kurz, "text": text, "datum": nullbar(datum), "uhrzeit": nullbar(uhrzeit), "ohne_uhrzeit_um": ohneUhrzeitUm,
                "form": form, "ausloesungen": liste, "erledigt": erledigt, "hinweis_um": hinweisUm, "zuletzt_erwaehnt": zuletztErwaehnt,
                "quelle": quelle.isEmpty ? "Jarvis Handy" : quelle, "erstellt": erstellt]
    }

    static func aus(id: String, _ w: JSONObjekt, geaendert: Int64) -> Erinnerung {
        func nullbar(_ x: Any?) -> String? { (x as? String).flatMap { $0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : $0 } }
        let liste = (w["ausloesungen"] as? [JSONObjekt] ?? []).compactMap { a -> Ausloesung? in
            guard let k = a["id"] as? String else { return nil }
            return Ausloesung(id: k, minuten: Int(AblageAbgleich.zahl(a["minuten"])), uhrzeit: nullbar(a["uhrzeit"]), angelegt: AblageAbgleich.zahl(a["angelegt"]))
        }
        return Erinnerung(id: id, allgemein: w["allgemein"] as? Bool ?? false, zeile: w["zeile"] as? String ?? "", kurz: w["kurz"] as? String ?? "", text: w["text"] as? String ?? "",
                          datum: nullbar(w["datum"]), uhrzeit: nullbar(w["uhrzeit"]), ohneUhrzeitUm: w["ohne_uhrzeit_um"] as? String ?? "10:00", form: w["form"] as? String ?? formMeldung,
                          ausloesungen: liste, erledigt: AblageAbgleich.zahl(w["erledigt"]), hinweisUm: AblageAbgleich.zahl(w["hinweis_um"]), zuletztErwaehnt: AblageAbgleich.zahl(w["zuletzt_erwaehnt"]),
                          quelle: w["quelle"] as? String ?? "", erstellt: AblageAbgleich.zahl(w["erstellt"]), geaendert: geaendert)
    }

    // MARK: Helfer

    static func neueId() -> String { String(UUID().uuidString.replacingOccurrences(of: "-", with: "").lowercased().prefix(20)) }

    static func neueAusloesung(_ minuten: Int, uhrzeit: String? = nil) -> Ausloesung {
        Ausloesung(id: String(UUID().uuidString.replacingOccurrences(of: "-", with: "").lowercased().prefix(8)), minuten: minuten, uhrzeit: uhrzeit, angelegt: Int64(Date().timeIntervalSince1970 * 1000))
    }

    /// Listenzeile aus dem Text, wenn keine angegeben ist: die ersten sechs Wörter.
    static func zeileAus(_ text: String) -> String {
        var z = text.split(whereSeparator: \.isWhitespace).prefix(6).joined(separator: " ")
        while let l = z.last, ",;:.".contains(l) { z.removeLast() }
        return z
    }

    static func kurzAus(_ text: String) -> String {
        let w = text.split(whereSeparator: \.isWhitespace)
        return w.prefix(35).joined(separator: " ") + (w.count > 35 ? " …" : "")
    }

    /// Datierte: das als Nächstes Kommende oben, danach das Vergangene, das Älteste ganz unten. Maßgeblich ist der Termin selbst.
    static func datiert(_ alle: [Erinnerung], jetzt: Int64) -> [Erinnerung] {
        let d = alle.filter { !$0.allgemein }
        let kommend = d.filter { ($0.termin ?? 0) >= jetzt && $0.erledigt == 0 }
        let vorbei = d.filter { !(($0.termin ?? 0) >= jetzt && $0.erledigt == 0) }
        return kommend.sorted { ($0.termin ?? 0) < ($1.termin ?? 0) } + vorbei.sorted { ($0.termin ?? 0) > ($1.termin ?? 0) }
    }

    static func allgemeine(_ alle: [Erinnerung]) -> [Erinnerung] {
        alle.filter(\.allgemein).sorted { a, b in
            if (a.erledigt != 0) != (b.erledigt != 0) { return a.erledigt == 0 }
            return a.erstellt > b.erstellt
        }
    }
}

/// Eine einzelne Auslösung einer Erinnerung; quittung ist ihre Kennung auf allen Geräten.
struct Faellig: Identifiable, Hashable, Sendable {
    let erinnerung: Erinnerung
    let quittung: String
    let zeit: Int64
    let ausloesung: Ausloesung?
    let anzeige: String
    var id: String { quittung }

    /// Wird diese Auslösung noch von selbst gemeldet? Vor dem Anlegen vergangene Zeitpunkte nicht: Sie waren beim Einstellen schon vorbei.
    func meldbar(_ jetzt: Int64) -> Bool {
        guard erinnerung.erledigt == 0, jetzt - zeit < Erinnerung.verpasstNachMs else { return false }
        if let a = ausloesung { return zeit >= a.angelegt - 60_000 }
        return true
    }
}

/// Eine Bestätigung kommt nur dazu und wird nie gelöscht: Keine Bearbeitung auf einem anderen Gerät kann sie überschreiben.
struct ErinnerungQuittung: Hashable, Sendable {
    let id: String
    let zeit: Int64
    let von: String
    let erinnerung: String

    func alsJson() -> JSONObjekt { ["zeit": zeit, "von": von, "erinnerung": erinnerung] }
    static func aus(id: String, _ w: JSONObjekt) -> ErinnerungQuittung {
        ErinnerungQuittung(id: id, zeit: AblageAbgleich.zahl(w["zeit"]), von: w["von"] as? String ?? "", erinnerung: w["erinnerung"] as? String ?? "")
    }
}
