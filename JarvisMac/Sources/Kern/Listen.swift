import AppKit
import CryptoKit
import Foundation
import Observation

/// Eine Liste wie am Handy (listen/Listen.kt): Name, Art, Farbe und eigene Ansicht. Liest sich aus dem rohen JSON der Datei,
/// damit Felder einer neueren Handy-Fassung beim Zurückschreiben erhalten bleiben.
struct Liste {
    let roh: JSONObjekt
    var id: Int64 { ListenWert.ganz(roh["id"]) }
    var name: String { roh["name"] as? String ?? "" }
    var art: String { roh["art"] as? String ?? "sonstiges" }
    var farbe: Int { Int(ListenWert.ganz(roh["farbe"])) }
    var sortierung: String { roh["sortierung"] as? String ?? "manuell" }
    var gruppierung: String { roh["gruppierung"] as? String ?? "keine" }
    var erledigteZeigen: Bool { ListenWert.wahr(roh["erledigte_zeigen"], true) }
    var pos: Double { ListenWert.zahl(roh["pos"]) }
    var geaendert: Int64 { ListenWert.ganz(roh["geaendert"]) }
    var geloescht: Bool { ListenWert.wahr(roh["geloescht"], false) }
    var abschnitte: [String] { roh["abschnitte"] as? [String] ?? [] }
}

/// Ein Eintrag; eltern = 0 heißt oberste Ebene. Priorität 0 keine, 1 niedrig, 2 mittel, 3 hoch.
struct ListenEintrag {
    let roh: JSONObjekt
    var id: Int64 { ListenWert.ganz(roh["id"]) }
    var listeId: Int64 { ListenWert.ganz(roh["liste"]) }
    var titel: String { roh["titel"] as? String ?? "" }
    var eltern: Int64 { ListenWert.ganz(roh["eltern"]) }
    var notiz: String { roh["notiz"] as? String ?? "" }
    var prioritaet: Int { Int(ListenWert.ganz(roh["prioritaet"])) }
    var faellig: String { roh["faellig"] as? String ?? "" }
    var tags: [String] { roh["tags"] as? [String] ?? [] }
    var erledigt: Bool { ListenWert.wahr(roh["erledigt"], false) }
    var erledigtAm: Int64 { ListenWert.ganz(roh["erledigt_am"]) }
    var pos: Double { ListenWert.zahl(roh["pos"]) }
    var erstellt: Int64 { ListenWert.ganz(roh["erstellt"]) }
    var geaendert: Int64 { ListenWert.ganz(roh["geaendert"]) }
    var geloescht: Bool { ListenWert.wahr(roh["geloescht"], false) }
    var angeheftet: Bool { ListenWert.wahr(roh["angeheftet"], false) }
    var abschnitt: String { roh["abschnitt"] as? String ?? "" }
}

enum ListenWert {
    static func ganz(_ x: Any?) -> Int64 { AblageAbgleich.zahl(x) }
    static func zahl(_ x: Any?) -> Double { (x as? NSNumber)?.doubleValue ?? 0 }
    static func wahr(_ x: Any?, _ vorgabe: Bool) -> Bool { (x as? Bool) ?? (x as? NSNumber)?.boolValue ?? vorgabe }
}

/// Eine Zeile der Ansicht: Gruppenkopf oder Eintrag mit Einrücktiefe und Fortschritt seiner Unterpunkte.
struct ListenZeile {
    var eintrag: ListenEintrag? = nil
    var tiefe = 0
    var kinderErledigt = 0
    var kinderGesamt = 0
    var kopf: String? = nil
    var kopfAnzahl = 0
    var kennung: String { eintrag.map { "e:\($0.id)" } ?? "kopf:" + (kopf ?? "") }
}

enum ListenArten {
    static let alle: [(kennung: String, anzeige: String, zeichen: String)] = [
        ("einkauf", "Einkauf", "🛒"), ("aufgaben", "Aufgaben", "✅"), ("checkliste", "Checkliste", "📋"), ("packliste", "Packliste", "🎒"), ("ideen", "Ideen", "💡"), ("sonstiges", "Sonstiges", "📝"),
    ]
    static func zeichen(_ art: String) -> String { alle.first { $0.kennung == art }?.zeichen ?? "📝" }
    static let sortierungen: [(kennung: String, anzeige: String)] = [
        ("manuell", "Eigene Reihenfolge"), ("faellig", "Fälligkeit"), ("prioritaet", "Priorität"), ("titel", "Titel A–Z"), ("tag", "Tag"), ("erstellt", "Neueste zuerst"), ("aelteste", "Älteste zuerst"), ("geaendert", "Zuletzt geändert"),
    ]
    static let gruppierungen: [(kennung: String, anzeige: String)] = [
        ("keine", "Keine"), ("abschnitt", "Abschnitte"), ("prioritaet", "Priorität"), ("faellig", "Fälligkeit"), ("tag", "Tag"), ("erstellt", "Erstellt am"),
    ]
    static let prioritaeten = ["keine", "niedrig", "mittel", "hoch"]
    static func prioName(_ p: Int) -> String { (0...3).contains(p) ? prioritaeten[p] : "keine" }

    /// Schnelleingabe wie am Handy: „Milch !hoch #kühl“ → Titel „Milch“, Priorität hoch, Tag „kühl“.
    static func schnell(_ text: String) -> (titel: String, prio: Int, tags: [String]) {
        var prio = 0
        var tags: [String] = []
        var rest: [String] = []
        for w in text.trimmingCharacters(in: .whitespaces).split(whereSeparator: \.isWhitespace).map(String.init) {
            if w.count > 1, w.hasPrefix("!") {
                let p = String(w.dropFirst()).lowercased()
                var wert: Int?
                if let z = Int(p), (0...3).contains(z) { wert = z } else {
                    switch p {
                    case "keine", "ohne": wert = 0
                    case "niedrig", "gering", "low": wert = 1
                    case "mittel", "normal", "medium": wert = 2
                    case "hoch", "wichtig", "dringend", "high": wert = 3
                    default: wert = nil
                    }
                }
                if let x = wert { prio = x; continue }
            } else if w.count > 1, w.hasPrefix("#") { tags.append(String(w.dropFirst())); continue }
            rest.append(w)
        }
        let titel = rest.joined(separator: " ").trimmingCharacters(in: .whitespaces)
        return (titel.isEmpty ? text.trimmingCharacters(in: .whitespaces) : titel, prio, tags)
    }

    /// Einmal angelegt: tag() läuft je Zeile und beim Sortieren mehrfach je Eintrag.
    private static let isoTag: DateFormatter = { let f = DateFormatter(); f.locale = Locale(identifier: "en_US_POSIX"); f.dateFormat = "yyyy-MM-dd"; return f }()

    static func tag(_ iso: String) -> Date? {
        return isoTag.date(from: iso).map { Calendar.current.startOfDay(for: $0) }
    }

    /// Fälligkeit kurz: heute, morgen, gestern oder „12.11.“.
    static func datumKurz(_ iso: String) -> String {
        guard let d = tag(iso) else { return iso }
        let k = Calendar.current
        let heute = k.startOfDay(for: Date())
        if d == heute { return "heute" }
        if d == k.date(byAdding: .day, value: 1, to: heute) { return "morgen" }
        if d == k.date(byAdding: .day, value: -1, to: heute) { return "gestern" }
        return "\(k.component(.day, from: d)).\(k.component(.month, from: d))."
    }
}

private struct ListenFehler: LocalizedError {
    let text: String
    var errorDescription: String? { text }
}

/// Franks Listen am Mac, Eins-zu-eins mit Handy und Tablet: je Liste und Eintrag eine JSON-Datei `listen/l_<id>.json` bzw.
/// `listen/e_<id>.json`, abgeglichen über den Geräteabgleich-Speicher des Servers (`/j/<Geheimnis>/abgleich`, Art „gedaechtnis“) wie
/// `GedaechtnisAbgleich.kt`. Der Mac hält eine Kopie unter Application Support/JarvisMac/Listen; Lesen geht ohne Netz, Änderungen gehen sofort hoch
/// und bei Netzausfall später. Gelöscht wird nur als Grabstein in der Datei (geloescht = true), nie die Datei oder das Dokument.
/// Regeln wie am Handy: Haben beide Seiten geändert, gewinnt die jüngere Änderung; was der Server ablehnt (jüngere Fassung drüben), wird ersetzt.
@MainActor @Observable
final class ListenSpeicher {
    static let shared = ListenSpeicher()
    static let ordnerName = "listen"
    private static let maxTiefe = 4

    @ObservationIgnored private var listen: [Int64: Liste] = [:]
    @ObservationIgnored private var eintraege: [Int64: ListenEintrag] = [:]
    /// Was der Server je Pfad zuletzt meldete: Prüfsumme und Zeit der Fassung.
    @ObservationIgnored private var fern: [String: (sha: String, geaendert: Int64)] = [:]
    /// Hier geändert und noch nicht vom Server angenommen.
    @ObservationIgnored private var offen: Set<String> = []
    @ObservationIgnored private var stand: Int64 = 0
    @ObservationIgnored private var instanz = ""
    @ObservationIgnored private var laeuft = false
    @ObservationIgnored private var hochladenLaeuft = false
    @ObservationIgnored private var speicherPlan: Task<Void, Never>?
    @ObservationIgnored private var abruf: Task<Int, Error>?
    @ObservationIgnored private var letzteId: Int64 = 0
    /// Prüfsumme je Pfad, mitgeführt bei jedem Schreiben: Der Abgleich liest dafür nicht jede Datei neu.
    @ObservationIgnored private var shaMerker: [String: String] = [:]

    /// Zählt jede Änderung des Bestands (Oberfläche baut neu).
    private(set) var aenderung = 0
    private(set) var fehler: String?
    /// Der erste Abgleich mit dem Server ist durch.
    private(set) var bereit = false
    /// Änderungen vom Mac, die noch nicht beim Server sind.
    private(set) var ausstehend = 0
    private(set) var zuletzt: Date?
    /// Hinweis für die Oberfläche (etwa nach einer Kollision), verschwindet mit der nächsten Änderung von Frank.
    var meldung: String?

    private let wurzel: URL = Pfade.unterstuetzung.appendingPathComponent("Listen", isDirectory: true)
    private var dateien: URL { wurzel.appendingPathComponent(Self.ordnerName, isDirectory: true) }
    private var zustandDatei: URL { wurzel.appendingPathComponent("zustand.json") }

    private init() { try? FileManager.default.createDirectory(at: dateien, withIntermediateDirectories: true) }

    private var basis: String? {
        let a = Einstellungen.shared.zugang.relay.trimmingCharacters(in: .whitespacesAndNewlines)
        guard a.hasSuffix("/mcp") else { return nil }
        return String(a.dropLast(4)) + "/abgleich"
    }

    // MARK: Lesen

    // Bestand und Abgleich sind von der Beobachtung ausgenommen: Wer liest, beobachtet stattdessen den Zähler aenderung.
    var alle: [Liste] {
        _ = aenderung
        return listen.values.filter { !$0.geloescht }.sorted { $0.pos != $1.pos ? $0.pos < $1.pos : $0.id < $1.id }
    }
    func listeZu(_ id: Int64) -> Liste? { listen[id].flatMap { $0.geloescht ? nil : $0 } }
    func eintraegeVon(_ liste: Int64) -> [ListenEintrag] { _ = aenderung; return eintraege.values.filter { $0.listeId == liste && !$0.geloescht } }
    func offen(_ liste: Int64) -> Int { _ = aenderung; return eintraege.values.filter { $0.listeId == liste && !$0.geloescht && !$0.erledigt }.count }
    var offenGesamt: Int { alle.reduce(0) { $0 + offen($1.id) } }

    // MARK: Hochfahren und Abgleich

    func hochfahren() {
        guard !laeuft else { return }
        laeuft = true
        // Nach dem Ruhezustand hängt der Long-Poll an einer toten Verbindung: abbrechen statt bis zum Zeitlimit warten.
        NSWorkspace.shared.notificationCenter.addObserver(forName: NSWorkspace.didWakeNotification, object: nil, queue: .main) { _ in
            MainActor.assumeIsolated { ListenSpeicher.shared.abruf?.cancel() }
        }
        Task {
            await ladeLokal()
            await schleife()
        }
    }

    private func ladeLokal() async {
        let zielDateien = dateien, zielZustand = zustandDatei
        let (z, objekte) = await Task.detached { () -> (JSONKiste, [JSONKiste]) in
            let zust = (try? Data(contentsOf: zielZustand)).flatMap { JSON.objekt($0) }
            var liste: [JSONKiste] = []
            let urls = (try? FileManager.default.contentsOfDirectory(at: zielDateien, includingPropertiesForKeys: nil)) ?? []
            for u in urls where u.pathExtension == "json" {
                if let d = try? Data(contentsOf: u), let o = JSON.objekt(d) { liste.append(JSONKiste(wert: o)) }
            }
            return (JSONKiste(wert: zust), liste)
        }.value
        if let z = z.wert {
            stand = ListenWert.ganz(z["stand"])
            instanz = z["instanz"] as? String ?? ""
            for (pfad, wert) in z["fern"] as? JSONObjekt ?? [:] {
                if let w = wert as? JSONObjekt { fern[pfad] = (w["sha"] as? String ?? "", ListenWert.ganz(w["geaendert"])) }
            }
            for p in z["offen"] as? [String] ?? [] { offen.insert(p) }
            bereit = stand > 0
        }
        for o in objekte { if let w = o.wert { uebernimm(w) } }
        ausstehend = offen.count
        aenderung += 1
    }

    /// Ein Objekt in den Bestand: Liste, wenn es kein Feld „liste“ hat, sonst Eintrag.
    private func uebernimm(_ o: JSONObjekt) {
        guard o["id"] != nil else { return }
        let id = ListenWert.ganz(o["id"])
        if o["liste"] != nil { eintraege[id] = ListenEintrag(roh: o) } else { listen[id] = Liste(roh: o) }
    }

    private func schleife() async {
        var fehlerFolge = 0
        let pausen: [Double] = [0, 2, 5, 15, 30]
        while true {
            let beginn = Date()
            if !Einstellungen.shared.listenAbgleichAn {
                // Ohne Abgleich gilt die Kopie am Mac als bereit: Sonst stünde bei einer leeren Kopie für immer „Abgleich läuft“.
                if !bereit { bereit = true }
                fehler = "Der Listen-Abgleich ist ausgeschaltet (Einstellungen → Zentrale & Bereiche); Änderungen bleiben am Mac, bis er wieder an ist."
                let t = Task { () async throws -> Int in try await Task.sleep(for: .seconds(30)); return 0 }
                abruf = t
                _ = try? await t.value
                continue
            }
            let t = Task { () async throws -> Int in
                // Erst den Stand des Servers sehen: Was dort inzwischen jünger ist, gewinnt, bevor Offenes hochgeht.
                if !self.offen.isEmpty {
                    _ = try await self.holen(warten: 0)
                    // Eigene Aufgabe: „Jetzt abgleichen“ bricht den Long-Poll ab, nie ein halbes Hochladen.
                    try await Task { try await self.hochladen() }.value
                }
                return try await self.holen(warten: 25)
            }
            abruf = t
            do {
                let n = try await t.value
                fehlerFolge = 0
                fehler = nil
                zuletzt = Date()
                if n == 0 && Date().timeIntervalSince(beginn) < 1 { try? await Task.sleep(for: .seconds(2)) }
            } catch is CancellationError {
                continue
            } catch let e as URLError where e.code == .cancelled {
                continue
            } catch {
                fehlerFolge += 1
                if fehlerFolge >= 2 {
                    fehler = basis == nil ? "Keine Relay-Adresse eingetragen; die Listen zeigen den letzten Stand." : "Server nicht erreichbar, du siehst den letzten Stand: \(error.localizedDescription)"
                    if fehlerFolge == 2 { log("listen", "Listen-Abgleich unterbrochen", wo: "Listen", fehler: error.localizedDescription) }
                }
                try? await Task.sleep(for: .seconds(pausen[min(fehlerFolge, pausen.count - 1)]))
            }
        }
    }

    /// Sofort abgleichen (Knopf „Abgleichen“, nach jeder Änderung).
    func jetztAbgleichen() { abruf?.cancel() }

    private func anfrage(_ url: URL, _ methode: String = "GET", sekunden: Double = 30) -> URLRequest {
        var r = URLRequest(url: url, timeoutInterval: sekunden)
        r.httpMethod = methode
        r.setValue("JarvisMac/\(AppInfo.version)", forHTTPHeaderField: "User-Agent")
        return r
    }

    private static func code(_ a: URLResponse) -> Int { (a as? HTTPURLResponse)?.statusCode ?? 0 }

    private func holen(warten: Int) async throws -> Int {
        guard let basis else { throw ListenFehler(text: "Keine Relay-Adresse eingetragen.") }
        let geraet = AblageAbgleich.quelle.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""
        var gesamt = 0
        var runden = 0
        while runden < 200 {
            let w = runden == 0 ? warten : 0
            runden += 1
            guard let url = URL(string: "\(basis)/aenderungen?seit=\(stand)&warten=\(w)&geraet=\(geraet)") else { throw ListenFehler(text: "Ungültige Relay-Adresse.") }
            let (daten, antwort) = try await URLSession.shared.data(for: anfrage(url, sekunden: Double(w) + 30))
            let c = Self.code(antwort)
            guard c == 200 else { throw ListenFehler(text: "Der Server antwortet mit \(c).") }
            guard let o = await Task.detached(operation: { JSONKiste(wert: JSON.objekt(daten)) }).value.wert else { throw ListenFehler(text: "Unlesbare Antwort vom Server.") }
            let i = o["instanz"] as? String ?? ""
            // Neu aufgesetzter Speicher: von vorn lesen; was hier geändert ist, geht danach wieder hoch.
            if ((o["neuaufbau"] as? Bool) == true || (!instanz.isEmpty && !i.isEmpty && i != instanz)) && stand != 0 {
                log("listen", "Abgleich-Speicher neu aufgesetzt, lese von vorn", wo: "Listen")
                instanz = i
                stand = 0
                // Das Handy lädt als Erstquelle seinen Stand neu hoch; der Mac drückt seine Kopie nicht mit frischer Zeit darüber.
                fern = [:]
                continue
            }
            if !i.isEmpty { instanz = i }
            var neu: [(pfad: String, sha: String, geaendert: Int64)] = []
            for d in o["docs"] as? [JSONObjekt] ?? [] {
                guard d["art"] as? String == "gedaechtnis", (d["geloescht"] as? Bool) != true,
                      let w = d["daten"] as? JSONObjekt, let pfad = w["pfad"] as? String, Self.pfadOk(pfad) else { continue }
                let sha = w["text_sha"] as? String ?? ""
                let geaendert = ListenWert.ganz(d["geaendert"])
                guard !sha.isEmpty else { continue }
                fern[pfad] = (sha, geaendert)
                neu.append((pfad, sha, geaendert))
            }
            for n in neu {
                if lokalSha(n.pfad) == n.sha { offen.remove(n.pfad); continue }
                // Hier geändert und noch nicht oben: Die jüngere Änderung gewinnt.
                if offen.contains(n.pfad) && lokalGeaendert(n.pfad) > n.geaendert { continue }
                if try await holeDatei(basis, n.pfad, n.sha, n.geaendert) { offen.remove(n.pfad); gesamt += 1 }
            }
            let weiter = o["stand"] != nil ? ListenWert.ganz(o["stand"]) : stand
            let mehr = (o["mehr"] as? Bool) == true && weiter != stand
            stand = weiter
            if !mehr { break }
        }
        if !bereit { bereit = true }
        ausstehend = offen.count
        zustandSpeichern()
        if gesamt > 0 { aenderung += 1; log("listen", "\(gesamt) Listen-Änderung(en) vom Handy", wo: "Listen") }
        return gesamt
    }

    private static func pfadOk(_ pfad: String) -> Bool { pfad.range(of: #"^listen/[le]_\d{1,20}\.json$"#, options: .regularExpression) != nil }

    private func lokalDatei(_ pfad: String) -> URL { wurzel.appendingPathComponent(pfad) }

    private static func sha(_ d: Data) -> String { SHA256.hash(data: d).map { String(format: "%02x", $0) }.joined() }

    private func lokalSha(_ pfad: String) -> String {
        if let s = shaMerker[pfad] { return s }
        guard let d = try? Data(contentsOf: lokalDatei(pfad)) else { return "" }
        let s = Self.sha(d)
        shaMerker[pfad] = s
        return s
    }

    private func lokalGeaendert(_ pfad: String) -> Int64 {
        guard let r = pfad.range(of: #"\d+"#, options: .regularExpression), let id = Int64(pfad[r]) else { return 0 }
        return pfad.contains("/l_") ? (listen[id]?.geaendert ?? 0) : (eintraege[id]?.geaendert ?? 0)
    }

    /// fernGeaendert: Zeit der fremden Fassung. Hat Frank hier während des Ladens jünger geändert, bleibt seine Fassung.
    private func holeDatei(_ basis: String, _ pfad: String, _ sha: String, _ fernGeaendert: Int64) async throws -> Bool {
        guard let url = URL(string: "\(basis)/blob/\(sha)") else { return false }
        let (daten, antwort) = try await URLSession.shared.data(for: anfrage(url))
        let c = Self.code(antwort)
        if c == 404 { return false }   // abgelöst und schon aufgeräumt: die nächste Fassung kommt mit dem Abgleich
        guard c == 200 else { throw ListenFehler(text: "Datei holen: Server \(c)") }
        guard Self.sha(daten) == sha else { log("fehler", "\(pfad) kam beschädigt an", wo: "Listen"); return false }
        guard let o = JSON.objekt(daten) else { return false }
        // Ab hier ohne await bis zum Übernehmen: Eine Änderung von Frank dazwischen ist nicht mehr möglich, eine davor wird hier erkannt.
        if offen.contains(pfad) && lokalGeaendert(pfad) > fernGeaendert { return false }
        try daten.write(to: lokalDatei(pfad), options: .atomic)
        shaMerker[pfad] = sha
        uebernimm(o)
        return true
    }

    /// Kennung des Dokuments am Server wie `GedaechtnisAbgleich.kennung`: nur Buchstaben, Ziffern, _ und -.
    static func kennung(_ pfad: String) -> String {
        let glatt = pfad.replacingOccurrences(of: "[^A-Za-z0-9_-]", with: "_", options: .regularExpression)
        let eindeutig = pfad.allSatisfy { ($0.isASCII && ($0.isLetter || $0.isNumber)) || "_-./".contains($0) }
        return eindeutig && glatt.count <= 64 ? glatt : String(glatt.prefix(40)) + "_" + String(sha(Data(pfad.utf8)).prefix(23))
    }

    /// Lädt alle hier geänderten Dateien hoch. Abgelehnt heißt: drüben liegt eine jüngere Fassung, sie gilt und kommt gleich mit.
    private func hochladen() async throws {
        guard !hochladenLaeuft, Einstellungen.shared.listenAbgleichAn, let basis else { return }
        hochladenLaeuft = true
        defer {
            hochladenLaeuft = false
            ausstehend = offen.count
            zustandSpeichern()
        }
        for pfad in Array(offen) {
            guard let daten = try? Data(contentsOf: lokalDatei(pfad)) else { offen.remove(pfad); continue }
            let sha = Self.sha(daten)
            guard let blobURL = URL(string: "\(basis)/blob/\(sha)"), let docURL = URL(string: "\(basis)/doc/gedaechtnis/\(Self.kennung(pfad))") else { continue }
            var breq = anfrage(blobURL, "PUT")
            breq.setValue("application/octet-stream", forHTTPHeaderField: "Content-Type")
            breq.httpBody = daten
            let (_, bant) = try await URLSession.shared.data(for: breq)
            let bc = Self.code(bant)
            guard (200..<300).contains(bc) else { throw ListenFehler(text: "Listen-Datei hochladen: Server \(bc)") }
            let vorher = fern[pfad] ?? ("", 0)
            // Zeit der Änderung, nicht des Hochladens: Eine offline gemachte ältere Änderung darf eine jüngere vom Handy nicht schlagen.
            var geaendert = lokalGeaendert(pfad)
            if vorher.geaendert >= geaendert && vorher.sha != sha {
                // Drüben liegt eine jüngere Fassung als Franks Änderung hier: Sie gilt, wie am Handy.
                offen.remove(pfad)
                if !vorher.sha.isEmpty { _ = try await holeDatei(basis, pfad, vorher.sha, vorher.geaendert) }
                continue
            }
            geaendert = max(geaendert, vorher.geaendert + 1)
            let rumpf: JSONObjekt = [
                "daten": ["pfad": pfad, "text_sha": sha, "groesse": daten.count] as JSONObjekt,
                "geaendert": geaendert, "geloescht": false, "von": AblageAbgleich.quelle,
            ]
            var dreq = anfrage(docURL, "PUT")
            dreq.setValue("application/json", forHTTPHeaderField: "Content-Type")
            dreq.httpBody = JSON.daten(rumpf)
            let (antwortDaten, dant) = try await URLSession.shared.data(for: dreq)
            let c = Self.code(dant)
            if c == 409 { continue }   // Datei am Server noch nicht vollständig: nächste Runde
            guard (200..<300).contains(c) else { throw ListenFehler(text: "Listen-Fassung melden: Server \(c)") }
            let antwort = JSON.objekt(antwortDaten) ?? [:]
            // Hat Frank während des Hochladens noch einmal geändert, bleibt der Pfad offen: Die neue Fassung geht in der nächsten Runde hoch.
            let unveraendert = lokalSha(pfad) == sha
            if (antwort["uebernommen"] as? Bool) == true {
                fern[pfad] = (sha, geaendert)
                if unveraendert { offen.remove(pfad) }
                continue
            }
            // Drüben ist eine jüngere Fassung: Sie gilt (wie am Handy), die hiesige Änderung weicht.
            if let d = antwort["doc"] as? JSONObjekt, let w = d["daten"] as? JSONObjekt, let fs = w["text_sha"] as? String, !fs.isEmpty {
                let fg = ListenWert.ganz(d["geaendert"])
                fern[pfad] = (fs, fg)
                if unveraendert { offen.remove(pfad) }
                if try await holeDatei(basis, pfad, fs, fg) {
                    meldung = "Eine Änderung kam zeitgleich von einem anderen Gerät; dessen jüngere Fassung gilt."
                    aenderung += 1
                }
            } else {
                // Abgelehnt ohne verwertbare Fassung: Franks Änderung bleibt offen (nie still verwerfen), der Mac liest den Server neu.
                log("fehler", "Listen-Änderung vom Server abgelehnt, bleibt offen: \(pfad)", wo: "Listen")
                fern[pfad] = nil
                stand = 0
            }
        }
    }

    private func zustandSpeichern() {
        guard speicherPlan == nil else { return }
        speicherPlan = Task {
            try? await Task.sleep(for: .milliseconds(800))
            speicherPlan = nil
            var f: JSONObjekt = [:]
            for (k, v) in fern { f[k] = ["sha": v.sha, "geaendert": v.geaendert] as JSONObjekt }
            let z: JSONObjekt = ["stand": stand, "instanz": instanz, "fern": f, "offen": Array(offen)]
            let daten = JSON.daten(z), ziel = zustandDatei
            await Task.detached { try? daten.write(to: ziel, options: .atomic) }.value
        }
    }

    // MARK: Schreiben (wie Listen.schreibe am Handy: alles in einem Zug, nur echte Änderungen)

    @MainActor
    final class Schreiber {
        private let s: ListenSpeicher
        fileprivate var listenNeu: [Int64: JSONObjekt] = [:]
        fileprivate var eintraegeNeu: [Int64: JSONObjekt] = [:]
        fileprivate init(_ s: ListenSpeicher) { self.s = s }

        func liste(_ id: Int64) -> Liste? { listenNeu[id].map { Liste(roh: $0) } ?? s.listeZu(id) }
        func eintrag(_ id: Int64) -> ListenEintrag? {
            if let n = eintraegeNeu[id] { return ListenEintrag(roh: n) }
            return s.eintraege[id].flatMap { $0.geloescht ? nil : $0 }
        }
        func alleEintraege(_ liste: Int64) -> [ListenEintrag] {
            (s.eintraege.values.filter { $0.listeId == liste && eintraegeNeu[$0.id] == nil }
                + eintraegeNeu.values.map { ListenEintrag(roh: $0) }.filter { $0.listeId == liste }).filter { !$0.geloescht }
        }

        /// Eine Kopie mit geänderten Feldern vormerken; unverändert wird nichts geschrieben (sonst schlüge eine neue Zeit jüngere Änderungen drüben).
        func setzeListe(_ l: Liste, _ aendern: (inout JSONObjekt) -> Void) { Self.setze(l.roh, l.id, &listenNeu, s.listen[l.id]?.roh, aendern) }
        func setzeEintrag(_ e: ListenEintrag, _ aendern: (inout JSONObjekt) -> Void) { Self.setze(e.roh, e.id, &eintraegeNeu, s.eintraege[e.id]?.roh, aendern) }
        func neuListe(_ o: JSONObjekt) { listenNeu[ListenWert.ganz(o["id"])] = o }
        func neuEintrag(_ o: JSONObjekt) { eintraegeNeu[ListenWert.ganz(o["id"])] = o }

        private static func setze(_ basis: JSONObjekt, _ id: Int64, _ ziel: inout [Int64: JSONObjekt], _ gespeichert: JSONObjekt?, _ aendern: (inout JSONObjekt) -> Void) {
            var kopie = ziel[id] ?? basis
            aendern(&kopie)
            if let gespeichert, gleichOhneZeit(gespeichert, kopie) { ziel[id] = nil; return }
            ziel[id] = kopie
        }

        private static func gleichOhneZeit(_ a: JSONObjekt, _ b: JSONObjekt) -> Bool {
            var x = a, y = b
            x["geaendert"] = 0; y["geaendert"] = 0
            return JSON.text(x) == JSON.text(y)
        }

        private func kinderNachEltern(_ liste: Int64) -> [Int64: [ListenEintrag]] { Dictionary(grouping: alleEintraege(liste), by: \.eltern) }

        func nachkommen(_ id: Int64, _ liste: Int64) -> [ListenEintrag] {
            let alle = kinderNachEltern(liste)
            var aus: [ListenEintrag] = []
            var gesehen: Set<Int64> = [id]
            func sammle(_ v: Int64) { for e in alle[v] ?? [] where gesehen.insert(e.id).inserted { aus.append(e); sammle(e.id) } }
            sammle(id)
            return aus
        }

        func vorfahren(_ e: ListenEintrag) -> [ListenEintrag] {
            var aus: [ListenEintrag] = []
            var gesehen: Set<Int64> = [e.id]
            var v = e.eltern
            while v != 0, gesehen.insert(v).inserted {
                guard let p = eintrag(v), p.listeId == e.listeId else { break }
                aus.append(p)
                v = p.eltern
            }
            return aus
        }

        func tiefe(_ e: ListenEintrag) -> Int { vorfahren(e).count }
        func hoehe(_ id: Int64, _ liste: Int64) -> Int {
            let alle = kinderNachEltern(liste)
            var gesehen: Set<Int64> = []
            func h(_ v: Int64) -> Int {
                guard gesehen.insert(v).inserted, let k = alle[v], !k.isEmpty else { return 0 }
                return k.map { 1 + h($0.id) }.max() ?? 0
            }
            return h(id)
        }
    }

    private static func jetzt() -> Int64 { Int64(Date().timeIntervalSince1970 * 1000) }

    /// Wie `Listen.schreibe`: Datei, Bestand und Abgleich in einem Zug.
    @discardableResult
    func schreibe<T>(_ block: (Schreiber) -> T) -> T {
        let s = Schreiber(self)
        let ergebnis = block(s)
        guard !s.listenNeu.isEmpty || !s.eintraegeNeu.isEmpty else { return ergebnis }
        let jetzt = Self.jetzt()
        for (id, o) in s.listenNeu { ablegen("\(Self.ordnerName)/l_\(id).json", o, jetzt) }
        for (id, o) in s.eintraegeNeu { ablegen("\(Self.ordnerName)/e_\(id).json", o, jetzt) }
        meldung = nil
        ausstehend = offen.count
        aenderung += 1
        zustandSpeichern()
        // Gleich abgleichen, nicht erst nach dem nächsten Long-Poll: Die Schleife holt zuerst den Stand des Servers und lädt dann hoch.
        jetztAbgleichen()
        return ergebnis
    }

    private func ablegen(_ pfad: String, _ o: JSONObjekt, _ jetzt: Int64) {
        var o = o
        // Nie älter als der Stand, auf dem die Änderung aufbaut: Geht die Uhr eines anderen Geräts vor, verlöre Franks Änderung sonst.
        o["geaendert"] = max(jetzt, (fern[pfad]?.geaendert ?? 0) + 1)
        let daten = JSON.daten(o)
        do { try daten.write(to: lokalDatei(pfad), options: .atomic) } catch { log("fehler", "Liste nicht gespeichert: \(error.localizedDescription)", wo: "Listen") }
        shaMerker[pfad] = Self.sha(daten)
        uebernimm(o)
        offen.insert(pfad)
        zustandSofort()   // „offen“ sofort sichern: Endet die App gleich danach, überschriebe der Server die Änderung sonst
    }

    /// Schreibt den Abgleichstand sofort (klein, synchron), ohne die 800-ms-Bündelung von [zustandSpeichern].
    private func zustandSofort() {
        var f: JSONObjekt = [:]
        for (k, v) in fern { f[k] = ["sha": v.sha, "geaendert": v.geaendert] as JSONObjekt }
        let z: JSONObjekt = ["stand": stand, "instanz": instanz, "fern": f, "offen": Array(offen)]
        try? JSON.daten(z).write(to: zustandDatei, options: .atomic)
    }

    func neueId() -> Int64 {
        let vorschlag = Self.jetzt() * 1000 + Int64.random(in: 0..<1000)
        letzteId = max(letzteId + 1, vorschlag)
        return letzteId
    }

    // MARK: Listen

    @discardableResult
    func listeAnlegen(_ name: String, art: String = "sonstiges") -> Liste {
        schreibe { s in
            let jetzt = Self.jetzt()
            let vorhanden = alle
            let o: JSONObjekt = [
                "id": neueId(), "name": name.trimmingCharacters(in: .whitespaces), "art": ListenArten.alle.contains { $0.kennung == art } ? art : "sonstiges",
                "farbe": vorhanden.count % 8, "sortierung": "manuell", "gruppierung": "keine", "erledigte_zeigen": true,
                "pos": (vorhanden.map(\.pos).max() ?? 0) + 1.0, "erstellt": jetzt, "geaendert": jetzt, "geloescht": false, "abschnitte": [String](),
            ]
            s.neuListe(o)
            return Liste(roh: o)
        }
    }

    func listeAendern(_ id: Int64, _ aendern: @escaping (inout JSONObjekt) -> Void) {
        schreibe { s in if let l = s.liste(id) { s.setzeListe(l, aendern) } }
    }

    /// Liste samt allen Einträgen als gelöscht markieren.
    func listeLoeschen(_ id: Int64) {
        schreibe { s in
            guard let l = s.liste(id) else { return }
            s.setzeListe(l) { $0["geloescht"] = true }
            for e in s.alleEintraege(id) { s.setzeEintrag(e) { $0["geloescht"] = true } }
        }
    }

    // MARK: Einträge

    @discardableResult
    func hinzufuegen(_ liste: Int64, _ titel: String, eltern: Int64 = 0, prioritaet: Int = 0, tags: [String] = [], abschnitt: String = "", faellig: String = "", notiz: String = "") -> ListenEintrag? {
        schreibe { s -> ListenEintrag? in
            let abschnitt = abschnitt.trimmingCharacters(in: .whitespaces)
            if !abschnitt.isEmpty, let l0 = s.liste(liste), !l0.abschnitte.contains(abschnitt) {
                s.setzeListe(l0) { $0["abschnitte"] = l0.abschnitte + [abschnitt] }
            }
            guard s.liste(liste) != nil else { return nil }
            var oben = eltern != 0 ? s.eintrag(eltern) : nil
            if let o = oben, o.listeId != liste { oben = nil }
            let vater: Int64 = oben.map { s.tiefe($0) + 1 > Self.maxTiefe ? $0.eltern : $0.id } ?? 0
            let geschwister = s.alleEintraege(liste).filter { $0.eltern == vater }
            let jetzt = Self.jetzt()
            let o: JSONObjekt = [
                "id": neueId(), "liste": liste, "titel": titel.trimmingCharacters(in: .whitespaces), "eltern": vater, "notiz": notiz.trimmingCharacters(in: .whitespaces),
                "prioritaet": min(max(prioritaet, 0), 3), "faellig": faellig,
                "tags": tags.map { String($0.trimmingCharacters(in: .whitespaces).drop { $0 == "#" }) }.filter { !$0.isEmpty },
                "erledigt": false, "erledigt_am": 0, "pos": (geschwister.map(\.pos).max() ?? 0) + 1.0, "erstellt": jetzt, "geaendert": jetzt,
                "geloescht": false, "angeheftet": false, "abschnitt": vater == 0 ? abschnitt : "",
            ]
            s.neuEintrag(o)
            return ListenEintrag(roh: o)
        }
    }

    func aendern(_ id: Int64, _ aendern: @escaping (inout JSONObjekt) -> Void) {
        schreibe { s in if let e = s.eintrag(id) { s.setzeEintrag(e, aendern) } }
    }

    /// Abhaken wie am Handy: nimmt alle Unterpunkte mit; sind alle Geschwister erledigt, auch den Oberpunkt. Wieder öffnen öffnet erledigte Oberpunkte.
    func abhaken(_ id: Int64, _ erledigt: Bool) {
        schreibe { s in
            guard let e = s.eintrag(id) else { return }
            let jetzt = Self.jetzt()
            @MainActor func setze(_ x: ListenEintrag, _ an: Bool) { s.setzeEintrag(x) { $0["erledigt"] = an; $0["erledigt_am"] = an ? jetzt : Int64(0) } }
            setze(e, erledigt)
            if erledigt {
                for k in s.nachkommen(id, e.listeId) where !k.erledigt { setze(k, true) }
                for p in s.vorfahren(e) {
                    if p.erledigt || s.alleEintraege(e.listeId).contains(where: { $0.eltern == p.id && !$0.erledigt }) { break }
                    setze(p, true)
                }
            } else {
                for p in s.vorfahren(e) where p.erledigt { setze(p, false) }
            }
        }
    }

    func loeschen(_ id: Int64) {
        schreibe { s in
            guard let e = s.eintrag(id) else { return }
            s.setzeEintrag(e) { $0["geloescht"] = true }
            for k in s.nachkommen(id, e.listeId) { s.setzeEintrag(k) { $0["geloescht"] = true } }
        }
    }

    @discardableResult
    func erledigteLoeschen(_ liste: Int64) -> Int {
        schreibe { s in
            let weg = s.alleEintraege(liste).filter(\.erledigt)
            for e in weg {
                s.setzeEintrag(e) { $0["geloescht"] = true }
                for k in s.nachkommen(e.id, liste) { s.setzeEintrag(k) { $0["geloescht"] = true } }
            }
            return weg.count
        }
    }

    /// In eine andere Liste verschieben, mit allen Unterpunkten; dort auf der obersten Ebene.
    func verschieben(_ id: Int64, nach ziel: Int64) {
        schreibe { s in
            guard let e = s.eintrag(id), s.liste(ziel) != nil else { return }
            let kinder = s.nachkommen(id, e.listeId)
            let pos = (s.alleEintraege(ziel).filter { $0.eltern == 0 }.map(\.pos).max() ?? 0) + 1.0
            s.setzeEintrag(e) { $0["liste"] = ziel; $0["eltern"] = Int64(0); $0["pos"] = pos }
            for k in kinder { s.setzeEintrag(k) { $0["liste"] = ziel } }
        }
    }

    /// Unterpunkt eines anderen Eintrags derselben Liste machen (0 = oberste Ebene). False, wenn das einen Kreis oder mehr als fünf Ebenen ergäbe.
    @discardableResult
    func einhaengen(_ id: Int64, unter eltern: Int64) -> Bool {
        schreibe { s in
            guard let e = s.eintrag(id) else { return false }
            if eltern == id || (eltern != 0 && s.nachkommen(id, e.listeId).contains { $0.id == eltern }) { return false }
            if eltern != 0 {
                guard let p = s.eintrag(eltern), p.listeId == e.listeId else { return false }
                if s.tiefe(p) + 1 + s.hoehe(id, e.listeId) > Self.maxTiefe { return false }
            }
            let pos = (s.alleEintraege(e.listeId).filter { $0.eltern == eltern }.map(\.pos).max() ?? 0) + 1.0
            s.setzeEintrag(e) { $0["eltern"] = eltern; $0["pos"] = pos }
            return true
        }
    }

    // MARK: Ansicht (wie Listen.ansicht am Handy)

    private static let deutsch = Locale(identifier: "de_DE")
    private static func kollator(_ a: String, _ b: String) -> Int {
        switch a.compare(b, options: [.caseInsensitive], range: nil, locale: deutsch) {
        case .orderedAscending: return -1
        case .orderedDescending: return 1
        default: return 0
        }
    }
    private static func vgl<T: Comparable>(_ a: T, _ b: T) -> Int { a < b ? -1 : a > b ? 1 : 0 }
    private static func folge(_ werte: Int...) -> Int { werte.first { $0 != 0 } ?? 0 }
    private static func manuell(_ a: ListenEintrag, _ b: ListenEintrag) -> Int { folge(vgl(a.pos, b.pos), vgl(a.erstellt, b.erstellt)) }
    private static func datum(_ a: ListenEintrag, _ b: ListenEintrag) -> Int {
        a.faellig == b.faellig ? 0 : a.faellig.isEmpty ? 1 : b.faellig.isEmpty ? -1 : vgl(a.faellig, b.faellig)
    }
    private static func prio(_ a: ListenEintrag, _ b: ListenEintrag) -> Int { vgl(b.prioritaet, a.prioritaet) }
    private static func ersterTag(_ e: ListenEintrag) -> String? { e.tags.sorted { kollator($0, $1) < 0 }.first }

    private static func vergleicher(_ sortierung: String) -> (ListenEintrag, ListenEintrag) -> Int {
        let v: (ListenEintrag, ListenEintrag) -> Int
        switch sortierung {
        case "faellig": v = { folge(datum($0, $1), prio($0, $1), manuell($0, $1)) }
        case "prioritaet": v = { folge(prio($0, $1), datum($0, $1), manuell($0, $1)) }
        case "titel": v = { folge(kollator($0.titel, $1.titel), manuell($0, $1)) }
        case "tag": v = { a, b in
            let x = ersterTag(a), y = ersterTag(b)
            let t = x == y ? 0 : x == nil ? 1 : y == nil ? -1 : kollator(x!, y!)
            return folge(t, prio(a, b), manuell(a, b))
        }
        case "erstellt": v = { vgl($1.erstellt, $0.erstellt) }
        case "aelteste": v = { vgl($0.erstellt, $1.erstellt) }
        case "geaendert": v = { vgl($1.geaendert, $0.geaendert) }
        default: v = manuell
        }
        return { a, b in folge(vgl(b.angeheftet ? 1 : 0, a.angeheftet ? 1 : 0), v(a, b)) }
    }

    private static func faelligGruppe(_ f: String) -> (rang: Int, name: String) {
        guard let d = ListenArten.tag(f) else { return (9, "Ohne Datum") }
        let k = Calendar.current
        let heute = k.startOfDay(for: Date())
        if d < heute { return (0, "Überfällig") }
        if d == heute { return (1, "Heute") }
        if d == k.date(byAdding: .day, value: 1, to: heute) { return (2, "Morgen") }
        if let w = k.date(byAdding: .day, value: 7, to: heute), d <= w { return (3, "Nächste 7 Tage") }
        return (4, "Später")
    }

    /// Gruppen in der Reihenfolge des ersten Auftretens; die Einträge behalten ihre sortierte Folge.
    private static func gruppiere<K: Hashable>(_ l: [ListenEintrag], _ schluessel: (ListenEintrag) -> K) -> [(K, [ListenEintrag])] {
        var reihe: [K] = []
        var gruppen: [K: [ListenEintrag]] = [:]
        for e in l {
            let k = schluessel(e)
            if gruppen[k] == nil { reihe.append(k) }
            gruppen[k, default: []].append(e)
        }
        return reihe.map { ($0, gruppen[$0] ?? []) }
    }

    /// Die Einträge einer Liste so, wie Frank sie am Handy sieht: offene oben (sortiert und gruppiert wie eingestellt),
    /// Unterpunkte eingerückt unter ihrem Oberpunkt, erledigte Oberpunkte unten unter „Erledigt“.
    func ansicht(_ liste: Liste, erledigteZeigen: Bool? = nil) -> [ListenZeile] {
        let alle = eintraegeVon(liste.id)
        let nachId = Dictionary(alle.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })
        func erreichtOben(_ e: ListenEintrag) -> Bool {
            var gesehen: Set<Int64> = [e.id]
            var v = e.eltern
            while v != 0 {
                guard gesehen.insert(v).inserted, let p = nachId[v] else { return false }
                v = p.eltern
            }
            return true
        }
        let kinder = Dictionary(grouping: alle) { e -> Int64 in e.eltern != 0 && nachId[e.eltern] != nil && erreichtOben(e) ? e.eltern : 0 }
        let v = Self.vergleicher(liste.sortierung)
        let zeigen = erledigteZeigen ?? liste.erledigteZeigen
        var aus: [ListenZeile] = []
        var gezeigt: Set<Int64> = []
        func sortiert(_ l: [ListenEintrag]) -> [ListenEintrag] { l.sorted { v($0, $1) < 0 } }
        func zeile(_ e: ListenEintrag, _ tiefe: Int) {
            guard gezeigt.insert(e.id).inserted else { return }
            let k = kinder[e.id] ?? []
            aus.append(ListenZeile(eintrag: e, tiefe: tiefe, kinderErledigt: k.filter(\.erledigt).count, kinderGesamt: k.count))
            for x in sortiert(k.filter { !$0.erledigt }) { zeile(x, tiefe + 1) }
            if zeigen || e.erledigt { for x in sortiert(k.filter(\.erledigt)) { zeile(x, tiefe + 1) } }
        }
        func gruppe(_ kopf: String, _ l: [ListenEintrag]) {
            aus.append(ListenZeile(kopf: kopf, kopfAnzahl: l.count))
            for e in l { zeile(e, 0) }
        }
        let oben = kinder[0] ?? []
        let offenOben = sortiert(oben.filter { !$0.erledigt })
        switch liste.gruppierung {
        case "abschnitt":
            let extra = Array(Set(offenOben.map(\.abschnitt).filter { !$0.isEmpty && !liste.abschnitte.contains($0) })).sorted { Self.kollator($0, $1) < 0 }
            let reihe = liste.abschnitte + extra
            for (k, l) in Self.gruppiere(offenOben, \.abschnitt).sorted(by: { ($0.0.isEmpty ? -1 : reihe.firstIndex(of: $0.0) ?? Int.max) < ($1.0.isEmpty ? -1 : reihe.firstIndex(of: $1.0) ?? Int.max) }) {
                gruppe(k.isEmpty ? "Ohne Abschnitt" : k, l)
            }
        case "erstellt":
            let f = DateFormatter(); f.locale = Self.deutsch; f.dateFormat = "d. MMMM yyyy"
            let tag = { (e: ListenEintrag) in Calendar.current.startOfDay(for: Date(timeIntervalSince1970: Double(e.erstellt) / 1000)) }
            for (k, l) in Self.gruppiere(offenOben, tag).sorted(by: { $0.0 > $1.0 }) { gruppe("Erstellt " + f.string(from: k), l) }
        case "prioritaet":
            for (k, l) in Self.gruppiere(offenOben, \.prioritaet).sorted(by: { $0.0 > $1.0 }) { gruppe(k == 0 ? "Ohne Priorität" : "Priorität " + ListenArten.prioName(k), l) }
        case "faellig":
            for (k, l) in Self.gruppiere(offenOben, { Self.faelligGruppe($0.faellig).rang }).sorted(by: { $0.0 < $1.0 }) { gruppe(Self.faelligGruppe(l[0].faellig).name, l) }
        case "tag":
            let gruppen = Self.gruppiere(offenOben) { Self.ersterTag($0) ?? "" }.sorted { a, b in
                if a.0.isEmpty != b.0.isEmpty { return !a.0.isEmpty }
                return Self.kollator(a.0, b.0) < 0
            }
            for (k, l) in gruppen { gruppe(k.isEmpty ? "Ohne Tag" : "#" + k, l) }
        default:
            for e in offenOben { zeile(e, 0) }
        }
        let fertig = oben.filter(\.erledigt).sorted { $0.erledigtAm > $1.erledigtAm }
        if zeigen && !fertig.isEmpty { gruppe("Erledigt", fertig) }
        return aus
    }
}
