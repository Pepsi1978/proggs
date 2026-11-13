import Foundation
import Observation

/// Eine Messgröße des Biomarker-Bereichs mit Kennung des Handy-Werkzeugs, Anzeige und Gruppe.
struct BioGroesse {
    let id: String
    let titel: String
    let gruppe: String
}

/// Eine Zeile aus biomarker_auswertung: aktueller Wert, Vergleich, Einordnung.
struct BioWert {
    let name: String
    let quelle: String
    let wert: String
    let einordnung: String
    let schnittDavor: String?
    let schnitte: String?
    let richtung: String
    let datum: String?
}

/// Ein Verlauf aus biomarker_verlauf: Tageswerte mit Einheit, Schnitt und Spanne.
struct BioVerlauf {
    let name: String
    let einheit: String
    let quelle: String
    let schnitt: Double?
    let punkte: [(tag: Date, wert: Double)]
}

/// Erste Fundstelle eines Musters mit allen Gruppen (fehlende Gruppen leer).
func regexTreffer(_ muster: String, _ text: String, optionen: NSRegularExpression.Options = []) -> [String]? {
    guard let r = try? NSRegularExpression(pattern: muster, options: optionen),
          let m = r.firstMatch(in: text, range: NSRange(text.startIndex..., in: text)) else { return nil }
    return (0..<m.numberOfRanges).map { i in Range(m.range(at: i), in: text).map { String(text[$0]) } ?? "" }
}

func regexAlle(_ muster: String, _ text: String, optionen: NSRegularExpression.Options = []) -> [[String]] {
    guard let r = try? NSRegularExpression(pattern: muster, options: optionen) else { return [] }
    return r.matches(in: text, range: NSRange(text.startIndex..., in: text)).map { m in
        (0..<m.numberOfRanges).map { i in Range(m.range(at: i), in: text).map { String(text[$0]) } ?? "" }
    }
}

/// Der Biomarker-Bereich am Mac. Wie das Tablet ein reiner Spiegel des Handys: Das Handy ist der einzige Abrufer bei Whoop, Oura, Waage
/// und Health Connect (Whoop verträgt nur einen); der Mac liest dessen Auswertung und Verläufe über die Handy-Werkzeuge und hält den letzten Stand
/// auf der Platte, damit der Bereich auch ohne Handy etwas zeigt. „Frische Werte holen“ bittet das Handy um einen Abruf (biomarker_aktualisieren).
@MainActor @Observable
final class BioDaten {
    static let shared = BioDaten()

    static let groessen: [BioGroesse] = [
        .init(id: "erholung", titel: "Erholung", gruppe: "Erholung"), .init(id: "hrv", titel: "HRV", gruppe: "Erholung"), .init(id: "ruhepuls", titel: "Ruhepuls", gruppe: "Erholung"), .init(id: "vo2max", titel: "VO2max", gruppe: "Erholung"),
        .init(id: "schlafdauer", titel: "Schlafzeit", gruppe: "Schlaf"), .init(id: "schlafperformance", titel: "Schlafperformance", gruppe: "Schlaf"), .init(id: "tiefschlaf_prozent", titel: "Tiefschlaf %", gruppe: "Schlaf"), .init(id: "rem_prozent", titel: "REM %", gruppe: "Schlaf"),
        .init(id: "wach_min", titel: "Wachzeit im Bett", gruppe: "Schlaf"), .init(id: "erholsamer_schlaf_prozent", titel: "Erholsamer Schlaf", gruppe: "Schlaf"), .init(id: "schlafeffizienz", titel: "Schlafeffizienz", gruppe: "Schlaf"),
        .init(id: "schlafkonsistenz", titel: "Schlafregelmäßigkeit", gruppe: "Schlaf"), .init(id: "schlafdefizit_min", titel: "Schlafdefizit", gruppe: "Schlaf"),
        .init(id: "tagesbelastung", titel: "Tagesbelastung", gruppe: "Aktivität"), .init(id: "energie_kcal", titel: "Energie", gruppe: "Aktivität"), .init(id: "schritte", titel: "Schritte", gruppe: "Aktivität"),
        .init(id: "atemfrequenz", titel: "Atemfrequenz", gruppe: "Körper"), .init(id: "hauttemperatur", titel: "Hauttemperatur", gruppe: "Körper"),
        .init(id: "oura_readiness", titel: "Readiness (Oura)", gruppe: "Oura"), .init(id: "oura_schlafscore", titel: "Schlafscore (Oura)", gruppe: "Oura"), .init(id: "oura_resilienz", titel: "Resilienz (Oura)", gruppe: "Oura"), .init(id: "oura_aktivitaet", titel: "Aktivität (Oura)", gruppe: "Oura"),
        .init(id: "gewicht", titel: "Gewicht", gruppe: "Waage"), .init(id: "koerperfett", titel: "Körperfett", gruppe: "Waage"), .init(id: "muskelmasse", titel: "Muskelmasse", gruppe: "Waage"), .init(id: "viszeralfett", titel: "Viszeralfett", gruppe: "Waage"), .init(id: "bmi", titel: "BMI", gruppe: "Waage"), .init(id: "wasseranteil", titel: "Wasseranteil", gruppe: "Waage"),
    ]

    /// Die Kerngrößen der Startkarte, wie BiomarkerStartKarte am Handy.
    static let kern = ["erholung", "hrv", "ruhepuls", "tiefschlaf_prozent", "schlafdauer", "vo2max"]

    private(set) var auswertungText: String?
    /// Alle Werte eines Tages (biomarker_tag), für „Tageswerte“.
    private(set) var tagText: String?
    private(set) var tagDatum = ""
    private(set) var werte: [BioWert] = []
    private(set) var verlaeufe: [String: BioVerlauf] = [:]
    /// Kopfzeilen der Auswertung: Whoop-Stand, letzter Abruf je Quelle, Hinweise.
    private(set) var kopf: [String] = []
    private(set) var laedt = false
    private(set) var holtFrisch = false
    private(set) var zuletzt: Date?
    private(set) var fehler: String?
    private(set) var abrufMeldung: String?
    private(set) var stand = 0
    private(set) var tage: Int = [7, 30, 90].contains(Einstellungen.shared.biomarkerTage) ? Einstellungen.shared.biomarkerTage : 30

    @ObservationIgnored private let datei = Pfade.unterstuetzung.appendingPathComponent("biomarker.json")
    @ObservationIgnored private var verlaufRoh: [String: String] = [:]
    /// Kennung → genauer Name laut Katalog des Handys.
    @ObservationIgnored private var katalogNamen: [String: String] = [:]
    @ObservationIgnored private var nochmal = false
    @ObservationIgnored private var speicherAufgabe: Task<Void, Never>?

    private init() { ladeSpeicher() }

    private func ladeSpeicher() {
        guard let d = try? Data(contentsOf: datei), let o = JSON.objekt(d) else { return }
        if o["zeit"] != nil { zuletzt = Date(timeIntervalSince1970: Double(ListenWert.ganz(o["zeit"])) / 1000) }
        for (k, w) in o["verlauf"] as? JSONObjekt ?? [:] { if let s = w as? String { verlaufRoh[k] = s } }
        for (k, w) in o["katalog"] as? JSONObjekt ?? [:] { if let s = w as? String { katalogNamen[k] = s } }
        uebernimm(o["auswertung"] as? String)
        tagText = o["tag"] as? String
        tagDatum = o["tag_datum"] as? String ?? ""
    }

    private func speichern() {
        var o: JSONObjekt = ["tage": tage, "zeit": Int64(Date().timeIntervalSince1970 * 1000), "tag_datum": tagDatum, "verlauf": verlaufRoh, "katalog": katalogNamen]
        if let a = auswertungText { o["auswertung"] = a }
        if let t = tagText { o["tag"] = t }
        let daten = JSON.daten(o), ziel = datei
        // Nacheinander: Zwei Speicherungen kurz hintereinander (Tag und Auswertung) schreiben sonst gleichzeitig.
        let vorher = speicherAufgabe
        speicherAufgabe = Task.detached { await vorher?.value; try? daten.write(to: ziel, options: .atomic) }
    }

    /// Liest Auswertung und Verläufe vom Handy. Ohne erzwingen höchstens alle fünf Minuten (Öffnen des Bereichs).
    func laden(erzwingen: Bool = false, tage neueTage: Int? = nil) async {
        var erzwingen = erzwingen
        // Der Zeitraum gilt sofort, auch wenn gerade geladen wird: Dann folgt nach dem laufenden Abruf einer mit dem neuen Zeitraum.
        if let t = neueTage, t != tage {
            tage = t; erzwingen = true
            if Einstellungen.shared.biomarkerTage != t { Einstellungen.shared.biomarkerTage = t }
            stand += 1
        }
        if laedt { if erzwingen { nochmal = true }; return }
        if !erzwingen, let z = zuletzt, Date().timeIntervalSince(z) < 300, !werte.isEmpty { return }
        laedt = true
        defer {
            laedt = false
            if nochmal { nochmal = false; Task { await laden(erzwingen: true) } }
        }
        let f = DateFormatter(); f.locale = Locale(identifier: "en_US_POSIX"); f.dateFormat = "yyyy-MM-dd"
        let bis = Date()
        let von = f.string(from: Calendar.current.date(byAdding: .day, value: -(tage - 1), to: bis) ?? bis)
        let ids = Self.groessen.map(\.id)
        let stuecke = stride(from: 0, to: ids.count, by: 5).map { Array(ids[$0..<min($0 + 5, ids.count)]) }
        let tageJetzt = tage
        let mitKatalog = katalogNamen.isEmpty
        async let auswertungL = McpKlient.shared.text("biomarker_auswertung", ["tage": tageJetzt, "metriken": ids])
        // Der Katalog nennt je Kennung den genauen Namen, mit dem Auswertung und Verlauf die Größe schreiben.
        async let katalogL: String? = mitKatalog ? McpKlient.shared.text("biomarker_verlauf", [:]) : nil
        let bisText = f.string(from: bis)
        let teile = await withTaskGroup(of: (Int, String?).self) { g -> [Int: String] in
            for (i, teil) in stuecke.enumerated() {
                g.addTask { (i, await McpKlient.shared.text("biomarker_verlauf", ["metriken": teil, "von": von, "bis": bisText, "aufloesung": "tag"])) }
            }
            var r: [Int: String] = [:]
            for await (i, t) in g { if let t { r[i] = t } }
            return r
        }
        let a = await auswertungL
        if let kt = await katalogL {
            for m in regexAlle(#"^- ([a-z0-9_]+): (.+?) \["#, kt, optionen: .anchorsMatchLines) { katalogNamen[m[1]] = m[2].trimmingCharacters(in: .whitespaces) }
        }
        if a == nil && teile.isEmpty {
            fehler = "Das Handy-Jarvis ist gerade nicht erreichbar, du siehst den letzten Stand."
            return
        }
        fehler = nil
        for (i, teil) in stuecke.enumerated() { if let r = teile[i] { for id in teil { verlaufRoh[id] = r } } }
        if let a { uebernimm(a) } else { baueVerlaeufe() }
        zuletzt = Date()
        speichern()
    }

    func tagLaden(_ datum: String) async {
        guard let t = await McpKlient.shared.text("biomarker_tag", ["datum": datum]) else {
            if tagText == nil { tagText = "Das Handy-Jarvis ist gerade nicht erreichbar." }
            return
        }
        tagDatum = datum
        tagText = McpKlient.ohneKontextHinweis(t)
        speichern()
    }

    /// Bittet das Handy, jetzt bei den Quellen abzurufen (dauert bis zu einer Minute), und liest danach neu.
    func frischHolen() async {
        guard !holtFrisch else { return }
        holtFrisch = true
        abrufMeldung = nil
        defer { holtFrisch = false }
        let t = await McpKlient.shared.text("biomarker_aktualisieren", [:], sekunden: 120)
        abrufMeldung = t.map(McpKlient.ohneKontextHinweis) ?? "Das Handy hat nicht geantwortet."
        log("biomarker", "Frische Biomarker beim Handy angefordert", wo: "Biomarker", fehler: t == nil ? "keine Antwort" : nil)
        await laden(erzwingen: true)
    }

    private func uebernimm(_ roh: String?) {
        guard let roh else { baueVerlaeufe(); return }
        let text = McpKlient.ohneKontextHinweis(roh)
        auswertungText = text
        var kopfNeu: [String] = []
        var werteNeu: [BioWert] = []
        var imKopf = true
        for zeile in text.components(separatedBy: "\n") {
            let z = zeile.trimmingCharacters(in: .whitespaces)
            if z.isEmpty { continue }
            if z.hasPrefix("Biomarker-Auswertung") { imKopf = false; continue }
            if imKopf { kopfNeu.append(z); continue }
            guard let m = regexTreffer(#"^- (.+) \(([^()]+)\): ([^;]+);(.*)$"#, z) else { continue }
            let rest = m[4]
            var wert = m[3].trimmingCharacters(in: .whitespaces)
            var datum: String?
            if let dm = regexTreffer(#"^(.*?)\s*\(Messung vom (\d{4}-\d{2}-\d{2}), [^)]*\)$"#, wert) { wert = dm[1]; datum = dm[2] }
            werteNeu.append(BioWert(
                name: m[1].trimmingCharacters(in: .whitespaces), quelle: m[2].trimmingCharacters(in: .whitespaces), wert: wert,
                einordnung: (regexTreffer(#"→\s*([^;]+)"#, rest)?[1] ?? "").trimmingCharacters(in: .whitespaces),
                schnittDavor: regexTreffer(#"Schnitt davor (-?\d[\d.]*(?:,\d+)?)"#, rest)?[1].trimmingCharacters(in: .whitespaces),
                schnitte: regexTreffer(#"Schnitte 7/30/90 Tage: ([^;]+)"#, rest)?[1].trimmingCharacters(in: .whitespaces),
                richtung: regexTreffer(#"\[höher ist (besser|schlechter|neutral)\]"#, rest)?[1] ?? "neutral", datum: datum))
        }
        kopf = kopfNeu
        werte = werteNeu
        baueVerlaeufe()
    }

    private func baueVerlaeufe() {
        var v: [String: BioVerlauf] = [:]
        for text in Set(verlaufRoh.values) { for b in Self.liesVerlauf(text) { v[b.name] = b } }
        verlaeufe = v
        stand += 1
    }

    /// Blöcke „Name [Einheit, Quelle], N Tage mit Daten, Schnitt X, Spanne a–b:“ und darunter „JJJJ-MM-TT Wert; …“.
    private static func liesVerlauf(_ text: String) -> [BioVerlauf] {
        let zeilen = text.components(separatedBy: "\n").map { $0.trimmingCharacters(in: .whitespaces) }
        var aus: [BioVerlauf] = []
        guard zeilen.count > 1 else { return aus }
        for i in 0..<(zeilen.count - 1) {
            guard let m = regexTreffer(#"^(.+?) \[([^,\]]*), ([^\]]+)\](.*):$"#, zeilen[i]) else { continue }
            let schnitt = regexTreffer(#"Schnitt (-?\d[\d.]*(?:,\d+)?)"#, m[4]).flatMap { zahl($0[1]) }
            var punkte: [(tag: Date, wert: Double)] = []
            for teil in zeilen[i + 1].components(separatedBy: ";") {
                let t = teil.trimmingCharacters(in: .whitespaces)
                guard let p = regexTreffer(#"^(\d{4}-\d{2}-\d{2})(?:\s*\S*)?\s+(-?[\d.,]+)$"#, t) ?? regexTreffer(#"^(\d{4}-\d{2}-\d{2})\S*\s+(-?[\d.,]+)"#, t),
                      let tag = ListenArten.tag(p[1]), let w = zahl(p[2]) else { continue }
                punkte.append((tag, w))
            }
            aus.append(BioVerlauf(name: m[1].trimmingCharacters(in: .whitespaces), einheit: m[2].trimmingCharacters(in: .whitespaces),
                                  quelle: m[3].trimmingCharacters(in: .whitespaces), schnitt: schnitt, punkte: punkte))
        }
        return aus
    }

    private static func zahl(_ t: String) -> Double? { Double(t.replacingOccurrences(of: ".", with: "").replacingOccurrences(of: ",", with: ".")) }

    /// Wert und Verlauf zu einer Kennung: über den Anzeigenamen, den das Handy für beide Werkzeuge gleich schreibt.
    func zu(_ g: BioGroesse) -> (wert: BioWert?, verlauf: BioVerlauf?) {
        // Der Verlauf wird über den Namen gefunden; den Namen kennt das Handy, nicht der Mac: erst der Katalog, sonst bekannte Namen.
        let genau = katalogNamen[g.id]
        func passt(_ name: String) -> Bool { genau.map { name.caseInsensitiveCompare($0) == .orderedSame } ?? Self.passt(name, g) }
        let wert = werte.first { passt($0.name) }
        var verlauf = wert.flatMap { verlaeufe[$0.name] }
        if verlauf == nil { verlauf = verlaeufe.values.first { passt($0.name) } }
        return (wert, verlauf)
    }

    private static let namen: [String: [String]] = [
        "erholung": ["Erholung (Recovery)", "Erholung"], "hrv": ["HRV"], "ruhepuls": ["Ruhepuls"], "vo2max": ["VO2max"],
        "schlafdauer": ["Schlafzeit", "Schlafdauer"], "schlafperformance": ["Schlafperformance"], "tiefschlaf_prozent": ["Tiefschlaf-Anteil"],
        "rem_prozent": ["REM-Anteil"], "wach_min": ["Wachzeit im Bett"], "erholsamer_schlaf_prozent": ["Erholsamer Schlaf (Tief + REM)", "Erholsamer Schlaf"],
        "schlafeffizienz": ["Schlafeffizienz"], "schlafkonsistenz": ["Schlafregelmäßigkeit"], "schlafdefizit_min": ["Schlafdefizit"],
        "tagesbelastung": ["Tagesbelastung (Strain)", "Tagesbelastung"], "energie_kcal": ["Energieumsatz"], "schritte": ["Schritte"],
        "atemfrequenz": ["Atemfrequenz"], "hauttemperatur": ["Hauttemperatur"],
        "oura_readiness": ["Readiness"], "oura_schlafscore": ["Schlafscore"], "oura_resilienz": ["Resilienz"], "oura_aktivitaet": ["Aktivitätsscore", "Aktivität"],
        "gewicht": ["Gewicht"], "koerperfett": ["Körperfett"], "muskelmasse": ["Muskelmasse"], "viszeralfett": ["Viszerales Fett"], "bmi": ["BMI"], "wasseranteil": ["Wasseranteil", "Körperwasser"],
    ]

    private static func passt(_ name: String, _ g: BioGroesse) -> Bool {
        guard let kandidaten = namen[g.id] else { return name.caseInsensitiveCompare(g.titel) == .orderedSame }
        let klein = name.lowercased()
        return kandidaten.contains { klein == $0.lowercased() || klein.hasPrefix($0.lowercased() + " (") }
    }

    /// Reihenfolge der Karten im Bereich: eigene Reihenfolge am Mac, neue Größen hinten.
    static func reihenfolge() -> [String] {
        let gespeichert = (UserDefaults.standard.string(forKey: "biomarker.reihenfolge") ?? "").split(separator: ",").map(String.init)
            .filter { id in groessen.contains { $0.id == id } }
        return gespeichert + groessen.map(\.id).filter { !gespeichert.contains($0) }
    }
    static func reihenfolgeSpeichern(_ ids: [String]) { UserDefaults.standard.set(ids.joined(separator: ","), forKey: "biomarker.reihenfolge") }
}

/// Victron-Solarbatterie wie die Startkarte am Handy (VictronStartKarte.kt): BMV-712 und SmartSolar-Laderegler, gelesen über das Handy-Werkzeug
/// batterie_lesen (direkt per Bluetooth am Handy, unterwegs über den Rechner zu Hause). Einmal je App-Start von selbst, sonst per Knopf oder im eingestellten Takt.
@MainActor @Observable
final class SolarDaten {
    static let shared = SolarDaten()
    @ObservationIgnored private let datei = Pfade.unterstuetzung.appendingPathComponent("solarbatterie.json")
    private(set) var stand: JSONObjekt?
    private(set) var laeuft = false
    private(set) var fehler: String?
    @ObservationIgnored var automatischGelesen = false

    private init() { stand = (try? Data(contentsOf: datei)).flatMap { JSON.objekt($0) } }

    func lesen() async {
        guard !laeuft else { return }
        laeuft = true
        defer { laeuft = false }
        let t = await McpKlient.shared.text("batterie_lesen", [:], sekunden: 60)
        let roh = t.map(McpKlient.ohneKontextHinweis)
        var o: JSONObjekt?
        if let roh, let a = roh.firstIndex(of: "{"), let b = roh.lastIndex(of: "}"), a < b { o = JSON.objekt(String(roh[a...b])) }
        guard var o, !(o["geraete"] as? [JSONObjekt] ?? []).isEmpty else {
            fehler = t == nil ? "Das Handy-Jarvis ist gerade nicht erreichbar." : ((roh ?? "").isEmpty ? "Die Anlage hat keine Werte geliefert." : String(roh!.prefix(200)))
            return
        }
        o["gelesen"] = Int64(Date().timeIntervalSince1970 * 1000)
        fehler = nil
        stand = o
        let daten = JSON.daten(o), ziel = datei
        Task.detached { try? daten.write(to: ziel, options: .atomic) }
    }

    func geraet(_ art: String) -> JSONObjekt? { (stand?["geraete"] as? [JSONObjekt] ?? []).first { $0["art"] as? String == art } }

    private static func z(_ d: Double, _ stellen: Int = 1) -> String {
        let f = NumberFormatter(); f.locale = Locale(identifier: "de_DE"); f.numberStyle = .decimal
        f.minimumFractionDigits = stellen; f.maximumFractionDigits = stellen
        return f.string(from: NSNumber(value: d)) ?? String(d)
    }
    private static func zahl(_ o: JSONObjekt, _ k: String) -> Double? { (o[k] as? NSNumber)?.doubleValue }

    /// Zeilen (Name, Wert) der Batterie, wie VictronFaehigkeit.batterieZeilen.
    static func batterieZeilen(_ b: JSONObjekt) -> [(String, String)] {
        var r: [(String, String)] = []
        if let soc = zahl(b, "ladezustand_prozent") { r.append(("Ladezustand", z(soc) + " %")) }
        if let v = zahl(b, "spannung_v") { r.append(("Spannung", z(v, 2) + " V")) }
        if let a = zahl(b, "strom_a") { r.append(("Strom", a > 0.05 ? "lädt mit \(z(a)) A" : a < -0.05 ? "gibt \(z(-a)) A ab" : "kein nennenswerter Strom")) }
        if let rl = zahl(b, "restlaufzeit_min") { let m = Int(rl); r.append(("Restlaufzeit", m >= 1440 ? "etwa \(m / 1440) Tage" : "\(m / 60) h \(m % 60) min")) }
        if let ah = zahl(b, "verbraucht_ah") { r.append(("Entnommen", z(-ah) + " Ah")) }
        return r
    }

    /// Zeilen des Ladereglers, wie VictronFaehigkeit.reglerZeilen.
    static func reglerZeilen(_ l: JSONObjekt) -> [(String, String)] {
        var r: [(String, String)] = []
        if let w = zahl(l, "solarleistung_w") { r.append(("Solarleistung", "\(Int(w)) W")) }
        if let a = zahl(l, "ladestrom_a") { r.append(("Ladestrom", z(a) + " A")) }
        if let k = zahl(l, "ertrag_heute_kwh") { r.append(("Ertrag heute", z(k, 2) + " kWh")) }
        if let v = zahl(l, "spannung_v") { r.append(("Batteriespannung", z(v, 2) + " V")) }
        if let s = l["zustand"] as? String, !s.isEmpty { r.append(("Ladephase", s)) }
        return r
    }

    /// „Stand 14:05 Uhr · direkt per Bluetooth (Handy)“ wie alterKurz am Handy.
    var standText: String {
        guard let stand else { return fehler ?? "Noch kein Stand" }
        // Wie am Handy der älteste Stand der Geräte: Ein veralteter Laderegler darf sich nicht hinter einem frischen Batteriewert verstecken.
        let geraete: [JSONObjekt] = stand["geraete"] as? [JSONObjekt] ?? []
        let zeiten: [Int64] = geraete.map { ListenWert.ganz($0["zeit"]) }.filter { $0 > 0 }
        let zeit: Int64 = zeiten.min() ?? 0
        let quelle = stand["quelle"] as? String ?? ""
        guard zeit > 0 else { return quelle }
        let uhr = Date(timeIntervalSince1970: Double(zeit))
        let f = DateFormatter(); f.locale = Locale(identifier: "de_DE")
        let text: String
        if Date().timeIntervalSince(uhr) < 120 { text = "gerade eben" }
        else if Calendar.current.isDateInToday(uhr) { f.dateFormat = "HH:mm"; text = f.string(from: uhr) + " Uhr" }
        else { f.dateFormat = "dd.MM. HH:mm"; text = f.string(from: uhr) + " Uhr" }
        return "Stand " + text + (quelle.isEmpty ? "" : " · " + quelle)
    }
}
