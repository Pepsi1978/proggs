import CryptoKit
import Foundation
import Observation
import UniformTypeIdentifiers

/// Die gemeinsame Ablage aller Jarvis-Versionen auf dem Server (Relay, /j/<Geheimnis>/ablage).
/// Der Mac hält die komplette Ablage als Kopie unter Application Support/JarvisMac/Ablage: Dokumente in docs.json, Inhalte nach Prüfsumme unter blobs.
/// Lesen geht also auch ohne Netz und ohne Handy; Schreiben braucht den Server und gilt erst, wenn er es angenommen hat.
/// Neues von anderen Versionen kommt per Long-Polling sofort an; eigene Änderungen wecken den Mac nicht.
@MainActor @Observable
final class AblageAbgleich {
    static let shared = AblageAbgleich()
    static let quelle = "Jarvis Mac OS"

    private(set) var eintraege: [AblageEintrag] = []
    private(set) var prompts: [PromptEintrag] = []
    private(set) var fehler: String?
    private(set) var verbunden = false

    @ObservationIgnored private var docs: [String: JSONObjekt] = [:]
    @ObservationIgnored private var stand = 0
    @ObservationIgnored private var laeuft = false
    @ObservationIgnored private var ladeAufgabe: Task<Void, Never>?
    @ObservationIgnored private var nochmalLaden = false
    @ObservationIgnored private var laufendeLadungen: [String: Task<URL, Error>] = [:]

    private let ordner: URL = {
        let u = Pfade.unterstuetzung.appendingPathComponent("Ablage", isDirectory: true)
        try? FileManager.default.createDirectory(at: u.appendingPathComponent("blobs", isDirectory: true), withIntermediateDirectories: true)
        return u
    }()
    private var blobs: URL { ordner.appendingPathComponent("blobs", isDirectory: true) }
    private var docsDatei: URL { ordner.appendingPathComponent("docs.json") }

    private var basis: String? {
        let a = Einstellungen.shared.zugang.relay.trimmingCharacters(in: .whitespacesAndNewlines)
        guard a.hasSuffix("/mcp") else { return nil }
        return String(a.dropLast(4)) + "/ablage"
    }

    // MARK: Abgleich

    func hochfahren() {
        guard !laeuft else { return }
        laeuft = true
        ladeDocs()
        neuAufbauen()
        allesLaden()
        Task { await schleife() }
    }

    private func schleife() async {
        while true {
            do {
                try await holen(warten: 25)
                if fehler != nil { fehler = nil }
                verbunden = true
            } catch {
                verbunden = false
                fehler = "Server nicht erreichbar, du siehst den letzten Stand: \(error.localizedDescription)"
                log("ablage", "Abgleich unterbrochen", wo: "Ablage", fehler: error.localizedDescription)
                try? await Task.sleep(for: .seconds(15))
            }
        }
    }

    /// Einmal sofort abgleichen (Knopf „Neu laden“).
    func jetztAbgleichen() async {
        do { try await holen(warten: 0); fehler = nil } catch { fehler = "Server nicht erreichbar, du siehst den letzten Stand: \(error.localizedDescription)" }
    }

    private func holen(warten: Int) async throws {
        guard let basis else { throw URLError(.badURL); }
        guard let url = URL(string: "\(basis)/aenderungen?seit=\(stand)&warten=\(warten)&geraet=\(Self.quelle.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? "")") else { throw URLError(.badURL) }
        let (daten, antwort) = try await URLSession.shared.data(for: URLRequest(url: url, timeoutInterval: Double(warten) + 30))
        guard (antwort as? HTTPURLResponse)?.statusCode == 200, let o = JSON.objekt(daten), let liste = o["docs"] as? [JSONObjekt] else { throw URLError(.badServerResponse) }
        let neu = Self.zahl(o["stand"])
        // Nur der Server weiß sicher, dass er neu aufgesetzt wurde. Ein kleinerer Stand allein kann auch eine überholte Antwort sein („Neu laden“ neben dem Long-Polling).
        let neuaufbau = o["neuaufbau"] as? Bool == true
        if neuaufbau { docs = [:] } else if neu < Int64(stand) { return }
        for d in liste {
            guard let art = d["art"] as? String, let id = d["id"] as? String else { continue }
            // Eine Antwort von vor dem eigenen Speichern darf die gerade gespeicherte Fassung nicht zurückdrehen.
            if let da = docs["\(art):\(id)"], Self.zahl(da["stand"]) > Self.zahl(d["stand"]) { continue }
            docs["\(art):\(id)"] = d
        }
        guard neu != Int64(stand) || !liste.isEmpty else { return }
        stand = max(Int(neu), neuaufbau ? 0 : stand)
        speichereDocs()
        neuAufbauen()
        if !liste.isEmpty { log("ablage", "\(liste.count) Änderung(en) vom Server", wo: "Ablage") }
        allesLaden()
    }

    private func ladeDocs() {
        guard let d = try? Data(contentsOf: docsDatei), let o = JSON.objekt(d) else { return }
        stand = Int(Self.zahl(o["stand"]))
        docs = o["docs"] as? [String: JSONObjekt] ?? [:]
    }

    private func speichereDocs() {
        try? JSON.daten(["stand": stand, "docs": docs] as JSONObjekt).write(to: docsDatei, options: .atomic)
    }

    /// Lädt im Hintergrund alles, was hier noch fehlt, und räumt Inhalte weg, die kein Eintrag mehr braucht.
    private func allesLaden() {
        guard ladeAufgabe == nil else { nochmalLaden = true; return }
        ladeAufgabe = Task {
            var fehlgeschlagen = false
            for sha in benutzteInhalte() where !FileManager.default.fileExists(atPath: pfad(sha).path) {
                do { _ = try await inhalt(sha) } catch { fehlgeschlagen = true }
            }
            // Erst jetzt und frisch berechnet; was gerade erst entstand, gehört vermutlich zu einem laufenden Speichern und bleibt einen Tag liegen.
            let benutzt = benutzteInhalte()
            let grenze = Date().addingTimeInterval(-86_400)
            let vorhanden = (try? FileManager.default.contentsOfDirectory(at: blobs, includingPropertiesForKeys: [.contentModificationDateKey])) ?? []
            for u in vorhanden where !benutzt.contains(u.lastPathComponent) {
                if let d = try? u.resourceValues(forKeys: [.contentModificationDateKey]).contentModificationDate, d < grenze { try? FileManager.default.removeItem(at: u) }
            }
            ladeAufgabe = nil
            if nochmalLaden { nochmalLaden = false; allesLaden() } else if fehlgeschlagen {
                try? await Task.sleep(for: .seconds(300))
                allesLaden()
            }
        }
    }

    private func benutzteInhalte() -> Set<String> {
        var r = Set<String>()
        for d in docs.values where d["art"] as? String == "eintrag" && d["geloescht"] as? Bool != true {
            guard let w = d["daten"] as? JSONObjekt else { continue }
            if let t = w["text_sha"] as? String, !t.isEmpty { r.insert(t) }
            for a in w["anhaenge"] as? [JSONObjekt] ?? [] { if let s = a["sha256"] as? String, !s.isEmpty { r.insert(s) } }
        }
        return r
    }

    // MARK: Inhalte

    private func pfad(_ sha: String) -> URL { blobs.appendingPathComponent(sha) }

    /// Der Inhalt zu einer Prüfsumme, notfalls jetzt vom Server. Ein Inhalt wird nur einmal gleichzeitig geladen.
    func inhalt(_ sha: String) async throws -> URL {
        let ziel = pfad(sha)
        if FileManager.default.fileExists(atPath: ziel.path) { return ziel }
        if let laufend = laufendeLadungen[sha] { return try await laufend.value }
        guard let basis, let url = URL(string: "\(basis)/blob/\(sha)") else { throw URLError(.badURL) }
        let t = Task<URL, Error> {
            let (tmp, antwort) = try await URLSession.shared.download(for: URLRequest(url: url, timeoutInterval: 600))
            defer { try? FileManager.default.removeItem(at: tmp) }
            guard (antwort as? HTTPURLResponse)?.statusCode == 200 else { throw URLError(.fileDoesNotExist) }
            guard try await Self.pruefsumme(tmp) == sha else { throw URLError(.cannotDecodeContentData) }
            try? FileManager.default.removeItem(at: ziel)
            try FileManager.default.moveItem(at: tmp, to: ziel)
            return ziel
        }
        laufendeLadungen[sha] = t
        defer { laufendeLadungen[sha] = nil }
        return try await t.value
    }

    nonisolated static func pruefsumme(_ url: URL) async throws -> String {
        try await Task.detached(priority: .utility) {
            let h = try FileHandle(forReadingFrom: url)
            defer { try? h.close() }
            var rechner = SHA256()
            while let stueck = try h.read(upToCount: 1 << 20), !stueck.isEmpty { rechner.update(data: stueck) }
            return rechner.finalize().map { String(format: "%02x", $0) }.joined()
        }.value
    }

    func text(_ e: AblageEintrag) async throws -> String {
        guard let sha = e.textSha else { return "" }
        let u = try await inhalt(sha)
        return (try? String(contentsOf: u, encoding: .utf8)) ?? ""
    }

    /// Volltextsuche: Titel, Dateinamen, Beschreibungen der Dateien und Texte; ein noch fehlender Text wird dafür geholt.
    func suche(_ wort: String) async -> Set<String> {
        let w = wort.lowercased()
        var r = Set<String>()
        for e in eintraege {
            if e.titel.lowercased().contains(w) || e.dateien.contains(where: { $0.name.lowercased().contains(w) || $0.beschreibung.lowercased().contains(w) }) { r.insert(e.id); continue }
            guard let sha = e.textSha, let u = try? await inhalt(sha) else { continue }
            let treffer = await Task.detached(priority: .utility) { ((try? String(contentsOf: u, encoding: .utf8)) ?? "").lowercased().contains(w) }.value
            if treffer { r.insert(e.id) }
        }
        return r
    }

    // MARK: Schreiben

    /// Schickt ein Dokument zum Server; fehlende Inhalte kommen zuerst hoch. Erst danach gilt es auch hier.
    private func senden(art: String, id: String, daten: JSONObjekt, geaendert: Int64, geloescht: Bool = false, inhalte: [String: URL] = [:]) async throws {
        guard let basis, let url = URL(string: "\(basis)/doc/\(art)/\(id)") else { throw AblageFehler.keineAdresse }
        var req = URLRequest(url: url, timeoutInterval: 60)
        req.httpMethod = "PUT"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.httpBody = JSON.daten(["daten": daten, "geaendert": geaendert, "geloescht": geloescht, "von": Self.quelle] as JSONObjekt)
        var (antwort, kopf) = try await URLSession.shared.data(for: req)
        if (kopf as? HTTPURLResponse)?.statusCode == 409 {
            for sha in JSON.objekt(antwort)?["fehlend"] as? [String] ?? [] {
                guard let datei = inhalte[sha], let hoch = URL(string: "\(basis)/blob/\(sha)") else { throw AblageFehler.inhaltFehlt }
                var r = URLRequest(url: hoch, timeoutInterval: 900)
                r.httpMethod = "PUT"
                let (_, k) = try await URLSession.shared.upload(for: r, fromFile: datei)
                guard (k as? HTTPURLResponse)?.statusCode == 200 else { throw AblageFehler.hochladen }
            }
            (antwort, kopf) = try await URLSession.shared.data(for: req)
        }
        guard (kopf as? HTTPURLResponse)?.statusCode == 200 else { throw AblageFehler.server }
        guard let ok = JSON.objekt(antwort), ok["uebernommen"] as? Bool == true else { throw AblageFehler.neuereFassung }
        docs["\(art):\(id)"] = ["art": art, "id": id, "daten": geloescht ? [:] : daten, "geaendert": geaendert, "geloescht": geloescht, "von": Self.quelle, "stand": Self.zahl(ok["stand"])] as JSONObjekt
        Task { await McpKlient.shared.kontextVerwerfen() }
        speichereDocs()
        neuAufbauen()
    }

    private static func jetzt() -> Int64 { Int64(Date().timeIntervalSince1970 * 1000) }

    /// Geht diese Uhr nach, gälte die eigene Änderung sonst als älter als der Stand, auf dem sie aufbaut.
    private func zeit(_ art: String, _ id: String) -> Int64 { max(Self.jetzt(), Self.zahl(docs["\(art):\(id)"]?["geaendert"]) + 1) }
    private static func neueId() -> String { String(UUID().uuidString.replacingOccurrences(of: "-", with: "").lowercased().prefix(20)) }

    /// Legt einen Inhalt in den eigenen Speicher, damit er nicht noch einmal vom Server geholt werden muss.
    private func merke(_ daten: Data) throws -> (String, URL) {
        let sha = SHA256.hash(data: daten).map { String(format: "%02x", $0) }.joined()
        let u = pfad(sha)
        if !FileManager.default.fileExists(atPath: u.path) { try daten.write(to: u, options: .atomic) }
        return (sha, u)
    }

    private func eintragDaten(_ id: String) -> JSONObjekt? {
        guard let d = docs["eintrag:\(id)"], d["geloescht"] as? Bool != true else { return nil }
        return d["daten"] as? JSONObjekt
    }

    /// Neuer Text oder Ersatz bei genau gleichem Titel (wie am Handy); mit anhaengen wird ergänzt.
    func textSpeichern(titel: String, inhalt: String, anhaengen: Bool = false) async throws {
        let name = Self.titelSauber(titel)
        var volltext = inhalt.trimmingCharacters(in: .whitespacesAndNewlines) + "\n"
        // Wie am Handy: Ersetzt wird nur bei genau gleichem Titel, ein eindeutiger Teil des Titels genügt nur zum Anhängen.
        var vorhanden = eintraege.first { Self.titelSauber($0.titel).lowercased() == name.lowercased() }
        if vorhanden == nil && anhaengen {
            let teil = eintraege.filter { Self.titelSauber($0.titel).lowercased().contains(name.lowercased()) }
            if teil.count == 1 { vorhanden = teil[0] }
        }
        if anhaengen, let v = vorhanden, v.textSha != nil { volltext = (try await text(v)) + "\n\n" + volltext }
        let (sha, u) = try merke(Data(volltext.utf8))
        let id = vorhanden?.id ?? Self.neueId()
        let zeit = self.zeit("eintrag", id)
        var w = vorhanden.flatMap { eintragDaten($0.id) } ?? ["id": id, "titel": name, "erstellt": zeit, "herkunft": Self.quelle, "quelle": Self.quelle, "beschreibung": "", "anhaenge": [JSONObjekt]()]
        w["text_sha"] = sha
        w["text_groesse"] = volltext.utf8.count
        w["geaendert"] = zeit
        w["recherche"] = name.lowercased().contains("recherche") || volltext.hasPrefix("# Recherche")
        try await senden(art: "eintrag", id: id, daten: w, geaendert: zeit, inhalte: [sha: u])
        log("ablage", "Text gespeichert: \(name)", wo: "Ablage")
    }

    /// Eine Datei vom Mac in die Ablage: ein eigener Eintrag mit dem Dateinamen als Titel.
    func dateiHinzufuegen(_ datei: URL) async throws {
        let sha = try await Self.pruefsumme(datei)
        let u = pfad(sha)
        // Abseits des Hauptfadens: Eine große Datei legte sonst die ganze App still. Über eine Zwischendatei, das Datum zählt für das Aufräumen.
        try await Task.detached(priority: .userInitiated) {
            guard !FileManager.default.fileExists(atPath: u.path) else { return }
            let teil = u.appendingPathExtension("teil")
            try? FileManager.default.removeItem(at: teil)
            try FileManager.default.copyItem(at: datei, to: teil)
            try FileManager.default.setAttributes([.modificationDate: Date()], ofItemAtPath: teil.path)
            try FileManager.default.moveItem(at: teil, to: u)
        }.value
        let groesse = (try? FileManager.default.attributesOfItem(atPath: u.path)[.size] as? Int) ?? 0
        let endung = datei.pathExtension.lowercased()
        let typ = Dateiart.bestimme(endung)
        let zeit = Self.jetzt()
        let titel = datei.deletingPathExtension().lastPathComponent
        let id = Self.neueId()
        let anhang: JSONObjekt = ["id": Self.neueId(), "titel": titel, "name": datei.lastPathComponent, "endung": endung.isEmpty ? "bin" : endung, "mime": typ.mime, "groesse": groesse,
                                  "erstellt": zeit, "herkunft": Self.quelle, "sha256": sha, "beschreibung": "", "art": typ.art, "kategorie": typ.kategorie]
        let w: JSONObjekt = ["id": id, "titel": titel, "text_sha": NSNull(), "text_groesse": 0, "erstellt": zeit, "geaendert": zeit, "herkunft": Self.quelle, "quelle": Self.quelle,
                             "beschreibung": "", "recherche": false, "anhaenge": [anhang]]
        try await senden(art: "eintrag", id: id, daten: w, geaendert: zeit, inhalte: [sha: u])
        log("ablage", "Datei hinzugefügt: \(datei.lastPathComponent)", wo: "Ablage")
    }

    /// Löscht den Eintrag oder nur eine seiner Dateien, auf allen Jarvis-Versionen.
    func loeschen(eintragId: String, dateiId: String? = nil) async throws {
        let zeit = self.zeit("eintrag", eintragId)
        if let dateiId, var w = eintragDaten(eintragId) {
            let rest = (w["anhaenge"] as? [JSONObjekt] ?? []).filter { $0["id"] as? String != dateiId }
            let hatText = (w["text_sha"] as? String)?.isEmpty == false
            if !rest.isEmpty || hatText {
                w["anhaenge"] = rest
                w["geaendert"] = zeit
                try await senden(art: "eintrag", id: eintragId, daten: w, geaendert: zeit)
                return
            }
        }
        try await senden(art: "eintrag", id: eintragId, daten: [:], geaendert: zeit, geloescht: true)
    }

    // MARK: Prompts

    /// Wie am Handy: Der gespeicherte Text endet immer mit dem Aufgabentrenner, damit die nächste Einsprache dort ansetzt.
    private static func promptText(_ t: String) -> String {
        AufgabenText.anhaengen("", t)
    }

    @discardableResult
    func promptSpeichern(_ text: String) async throws -> PromptEintrag {
        let sauber = Self.promptText(text)
        guard !sauber.isEmpty else { throw AblageFehler.leer }
        let zeit = Self.jetzt()
        let w: JSONObjekt = ["text": sauber, "titel": "", "erstellt": zeit, "quelle": Self.quelle]
        try await senden(art: "prompt", id: String(zeit), daten: w, geaendert: zeit)
        return prompts.first { $0.erstellt == zeit } ?? PromptEintrag(nummer: prompts.count, titel: "", text: sauber, erstellt: zeit, quelle: Self.quelle)
    }

    func promptAendern(erstellt: Int64, text: String) async throws {
        guard var w = docs["prompt:\(erstellt)"]?["daten"] as? JSONObjekt else { throw AblageFehler.nichtMehrDa }
        let sauber = Self.promptText(text)
        guard !sauber.isEmpty else { throw AblageFehler.leer }
        if w["text"] as? String != sauber { w["titel"] = "" }
        w["text"] = sauber
        try await senden(art: "prompt", id: String(erstellt), daten: w, geaendert: zeit("prompt", String(erstellt)))
    }

    func promptLoeschen(erstellt: Int64) async throws {
        try await senden(art: "prompt", id: String(erstellt), daten: [:], geaendert: zeit("prompt", String(erstellt)), geloescht: true)
    }

    // MARK: Werkzeuge des Modells

    /// Was der Mac selbst am Server erledigt. Alles andere geht weiter ans Handy; dort erzeugte Einträge bekommen die Quelle des Mac über _quelle.
    func werkzeug(_ name: String, _ a: JSONObjekt) async -> WerkzeugErgebnis? {
        do {
            switch name {
            case "ablage_schreiben":
                guard a["als_pdf"] as? Bool != true else { return nil }
                let titel = a["titel"] as? String ?? "", inhalt = a["inhalt"] as? String ?? ""
                guard !inhalt.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return WerkzeugErgebnis(text: "Der Inhalt ist leer.", fehler: true) }
                try await textSpeichern(titel: titel, inhalt: inhalt, anhaengen: a["anhaengen"] as? Bool == true)
                return WerkzeugErgebnis(text: "Abgelegt als „\(titel)“.")
            case "ablage_lesen":
                // Ohne Titel die Liste vom Handy (mit Vorläufigem), Unbekanntes kann ein vorläufiges Ergebnis dort sein.
                guard let titel = (a["titel"] as? String).flatMap({ $0.trimmingCharacters(in: .whitespaces).isEmpty ? nil : $0 }), let e = finde(titel) else { return nil }
                var t = "Eintrag „\(e.titel)“ von \(e.quelle) (Stand \(e.geaendert.formatted(date: .abbreviated, time: .shortened))):\n\n"
                if e.textSha != nil { t += String(try await text(e).prefix(60_000)) + "\n" }
                if !e.dateien.isEmpty { t += "\nDATEIEN (in der Ablage ansehen):\n" + e.dateien.map { "- \($0.name) (\($0.art), \(AblageAnsicht.groesse($0.groesse)))" }.joined(separator: "\n") }
                return WerkzeugErgebnis(text: t)
            case "ablage_loeschen":
                // Direkte Aufrufe der App kannten nur die Kennung; Unbekanntes kann ein vorläufiges Ergebnis am Handy sein.
                let kennung = a["eintrag_id"] as? String
                guard let e = kennung.map({ k in eintraege.first { $0.id == k } }) ?? finde(a["titel"] as? String ?? "") else { return nil }
                let wunsch = (a["datei"] as? String)?.trimmingCharacters(in: .whitespaces).lowercased() ?? ""
                if let id = a["datei_id"] as? String {
                    guard let d = e.dateien.first(where: { $0.id == id }) else { return WerkzeugErgebnis(text: "Diese Datei gibt es nicht mehr.", fehler: true) }
                    try await loeschen(eintragId: e.id, dateiId: d.id)
                    return WerkzeugErgebnis(text: "Gelöscht: „\(d.name)“ aus „\(e.titel)“.")
                }
                if !wunsch.isEmpty {
                    let teil = e.dateien.filter { $0.name.lowercased().contains(wunsch) }
                    guard let d = e.dateien.first(where: { $0.name.lowercased() == wunsch }) ?? (teil.count == 1 ? teil[0] : nil) else { return WerkzeugErgebnis(text: "Keine eindeutige Datei „\(wunsch)“ in „\(e.titel)“.", fehler: true) }
                    try await loeschen(eintragId: e.id, dateiId: d.id)
                    return WerkzeugErgebnis(text: "Gelöscht: „\(d.name)“ aus „\(e.titel)“.")
                }
                try await loeschen(eintragId: e.id)
                return WerkzeugErgebnis(text: "Gelöscht: „\(e.titel)“.")
            case "prompts_lesen":
                let kopf = { (p: PromptEintrag) in "Prompt \(p.nummer)\(p.titel.isEmpty ? "" : " (\(p.titel))") [id \(p.erstellt)]:\n\(p.text)" }
                let n = Self.zahl(a["nummer"])
                if n > 0 {
                    guard let p = prompts.first(where: { Int64($0.nummer) == n }) else { return WerkzeugErgebnis(text: "Einen Prompt mit der Nummer \(n) gibt es nicht.", fehler: true) }
                    return WerkzeugErgebnis(text: kopf(p))
                }
                if prompts.isEmpty { return WerkzeugErgebnis(text: "Noch keine Prompts gespeichert.") }
                return WerkzeugErgebnis(text: prompts.map(kopf).joined(separator: "\n\n"))
            case "prompt_speichern":
                let aktion = a["aktion"] as? String ?? "speichern", text = a["text"] as? String ?? ""
                let erstellt = Self.zahl(a["erstellt"]), nummer = Self.zahl(a["nummer"])
                let ziel = prompts.first { erstellt != 0 ? $0.erstellt == erstellt : Int64($0.nummer) == nummer }
                switch aktion {
                case "loeschen":
                    guard let ziel else { return WerkzeugErgebnis(text: "Diesen Prompt gibt es nicht.", fehler: true) }
                    if erstellt != 0, !text.isEmpty, ziel.text.trimmingCharacters(in: .whitespacesAndNewlines) != text.trimmingCharacters(in: .whitespacesAndNewlines) {
                        return WerkzeugErgebnis(text: "Der Prompt wurde inzwischen geändert und bleibt stehen.", fehler: true)
                    }
                    try await promptLoeschen(erstellt: ziel.erstellt)
                    return WerkzeugErgebnis(text: "Prompt \(ziel.nummer) gelöscht. Die folgenden Prompts sind eine Nummer aufgerückt.")
                case "aendern":
                    guard let ziel else { return WerkzeugErgebnis(text: "Diesen Prompt gibt es nicht.", fehler: true) }
                    try await promptAendern(erstellt: ziel.erstellt, text: text)
                    return WerkzeugErgebnis(text: "Prompt \(ziel.nummer) geändert.")
                default:
                    let p = try await promptSpeichern(text)
                    return WerkzeugErgebnis(text: "Als Prompt \(p.nummer) im Reiter Prompts der Ablage gespeichert.")
                }
            default:
                return nil
            }
        } catch {
            return WerkzeugErgebnis(text: error.localizedDescription, fehler: true)
        }
    }

    /// Genauer Titel oder ein eindeutiger Teil davon, wie am Handy.
    private func finde(_ titel: String) -> AblageEintrag? {
        let g = Self.titelSauber(titel).lowercased()
        if let e = eintraege.first(where: { Self.titelSauber($0.titel).lowercased() == g }) { return e }
        let teil = eintraege.filter { Self.titelSauber($0.titel).lowercased().contains(g) }
        return teil.count == 1 ? teil[0] : nil
    }

    /// Die Liste aus der eigenen Kopie, wenn das Handy nicht antwortet.
    func liste() -> WerkzeugErgebnis {
        if eintraege.isEmpty { return WerkzeugErgebnis(text: "Die Ablage ist leer.") }
        let zeilen = eintraege.prefix(80).map { e in
            "- \(e.titel) (\(e.quelle), \(e.geaendert.formatted(date: .abbreviated, time: .omitted)))" + (e.dateien.isEmpty ? "" : ": " + e.dateien.map(\.name).joined(separator: ", "))
        }
        return WerkzeugErgebnis(text: "\(eintraege.count) Eintrag/Einträge in der Ablage (Handy nicht erreichbar, Vorläufiges fehlt hier):\n" + zeilen.joined(separator: "\n"))
    }

    /// Wie AblageSpeicher.dateiname am Handy: ohne Pfadzeichen und .md, höchstens 80 Zeichen, sonst entstünden Doppel.
    private static func titelSauber(_ titel: String) -> String {
        var t = titel.trimmingCharacters(in: .whitespacesAndNewlines)
        if t.lowercased().hasSuffix(".md") { t = String(t.dropLast(3)) }
        t = t.replacingOccurrences(of: #"[\\/:*?"<>|[:cntrl:]]"#, with: " ", options: .regularExpression)
        t = t.replacingOccurrences(of: #"\s+"#, with: " ", options: .regularExpression)
        t = String(t.prefix(80)).trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? "Notiz" : t
    }

    // MARK: Ansicht

    /// Das Modell schickt Zahlen mitunter als Text.
    nonisolated static func zahl(_ x: Any?) -> Int64 { (x as? NSNumber)?.int64Value ?? (x as? String).flatMap { Int64($0.trimmingCharacters(in: .whitespaces)) } ?? 0 }

    private func neuAufbauen() {
        var neu: [AblageEintrag] = []
        var ps: [PromptEintrag] = []
        for d in docs.values where d["geloescht"] as? Bool != true {
            guard let w = d["daten"] as? JSONObjekt, let id = d["id"] as? String else { continue }
            switch d["art"] as? String {
            case "eintrag":
                let dateien = (w["anhaenge"] as? [JSONObjekt] ?? []).map { a in
                    AblageDatei(id: a["id"] as? String ?? "", name: a["name"] as? String ?? "", art: a["art"] as? String ?? "SONSTIGE", kategorie: a["kategorie"] as? String ?? "SONSTIGE",
                                endung: a["endung"] as? String ?? "", groesse: Int(Self.zahl(a["groesse"])), sha: a["sha256"] as? String ?? "", beschreibung: a["beschreibung"] as? String ?? "")
                }
                let sha = (w["text_sha"] as? String).flatMap { $0.isEmpty ? nil : $0 }
                neu.append(AblageEintrag(id: id, titel: w["titel"] as? String ?? "", geaendert: Date(timeIntervalSince1970: Double(Self.zahl(d["geaendert"])) / 1000), hatText: sha != nil,
                                         recherche: w["recherche"] as? Bool ?? false, dateien: dateien, textSha: sha, textGroesse: Int(Self.zahl(w["text_groesse"])), quelle: Self.quellenName(w["quelle"])))
            case "prompt":
                ps.append(PromptEintrag(nummer: 0, titel: w["titel"] as? String ?? "", text: w["text"] as? String ?? "", erstellt: Int64(id) ?? 0, quelle: Self.quellenName(w["quelle"])))
            default: break
            }
        }
        // Die gezogene Reihenfolge des Handys; was sie noch nicht kennt, steht unten, das Älteste zuerst.
        let ordnung = ((docs["ordnung:prompts"]?["daten"] as? JSONObjekt)?["reihenfolge"] as? [Any] ?? []).map { Self.zahl($0) }
        var platz: [Int64: Int] = [:]
        for (i, e) in ordnung.enumerated() where platz[e] == nil { platz[e] = i }
        ps.sort { a, b in
            switch (platz[a.erstellt], platz[b.erstellt]) {
            case let (x?, y?): return x < y
            case (_?, nil): return true
            case (nil, _?): return false
            default: return a.erstellt < b.erstellt
            }
        }
        for i in ps.indices { ps[i].nummer = i + 1 }
        eintraege = neu.sorted { $0.geaendert > $1.geaendert }
        prompts = ps
    }

    private static func quellenName(_ x: Any?) -> String {
        let s = (x as? String ?? "").trimmingCharacters(in: .whitespaces)
        return s.isEmpty ? "Jarvis Handy" : s
    }
}

enum AblageFehler: LocalizedError {
    case keineAdresse, inhaltFehlt, hochladen, server, neuereFassung, leer, nichtMehrDa
    var errorDescription: String? {
        switch self {
        case .keineAdresse: return "Keine Relay-Adresse eingetragen (Einstellungen → Konten)."
        case .inhaltFehlt: return "Der Inhalt fehlt hier, nichts wurde gespeichert."
        case .hochladen: return "Die Datei ließ sich nicht hochladen, nichts wurde gespeichert."
        case .server: return "Der Server hat das nicht angenommen, nichts wurde gespeichert."
        case .neuereFassung: return "Auf dem Server liegt schon eine neuere Fassung. Sie erscheint gleich hier."
        case .leer: return "Der Text ist leer."
        case .nichtMehrDa: return "Das gibt es inzwischen nicht mehr."
        }
    }
}

/// Dieselbe Einteilung wie Dateityp.art auf dem Handy; die Android-App berechnet sie aus der Endung selbst neu.
enum Dateiart {
    static func bestimme(_ e: String) -> (art: String, kategorie: String, mime: String) {
        let typ = UTType(filenameExtension: e)
        let mime = typ?.preferredMIMEType ?? "application/octet-stream"
        let code: Set<String> = ["kt", "kts", "java", "py", "js", "ts", "css", "sh", "ps1", "c", "h", "cpp", "cs", "swift", "go", "rs", "sql", "gradle", "bat", "rb", "php"]
        let daten: Set<String> = ["json", "xml", "yaml", "yml", "toml", "ini", "conf", "properties"]
        switch e {
        case "md", "markdown": return ("MARKDOWN", "TEXTE", "text/markdown")
        case "csv", "tsv": return ("TABELLE_TEXT", "TABELLEN", mime)
        case "svg": return ("SVG", "BILDER", "image/svg+xml")
        case "gif": return ("ANIMATION", "BILDER", "image/gif")
        case "html", "htm": return ("HTML", "TEXTE", "text/html")
        case _ where code.contains(e): return ("CODE", "TEXTE", mime)
        case _ where daten.contains(e): return ("DATEN", "TEXTE", mime)
        case "pdf": return ("PDF", "DOKUMENTE", "application/pdf")
        case "doc", "docx", "odt", "rtf", "epub": return ("DOKUMENT", "DOKUMENTE", mime)
        case "xls", "xlsx", "ods": return ("TABELLE", "TABELLEN", mime)
        case "ppt", "pptx", "odp", "key": return ("PRAESENTATION", "PRAESENTATIONEN", mime)
        case "zip", "7z", "rar", "gz", "tar": return ("ARCHIV", "SONSTIGE", mime)
        default:
            if mime.hasPrefix("image/") { return ("BILD", "BILDER", mime) }
            if mime.hasPrefix("audio/") { return ("AUDIO", "AUDIO", mime) }
            if mime.hasPrefix("video/") { return ("VIDEO", "VIDEO", mime) }
            if e == "txt" || e == "log" || mime.hasPrefix("text/") { return ("TEXT", "TEXTE", mime) }
            return ("SONSTIGE", "SONSTIGE", mime)
        }
    }
}
