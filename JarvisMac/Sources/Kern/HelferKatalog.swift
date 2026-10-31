import Foundation

/// Ein Eintrag des Katalogs. Der Steckbrief bleibt Text: So darf der Eintrag zwischen den Ausführungsfäden wandern.
struct HelferEintrag: Identifiable, Codable, Sendable {
    var kennung: String
    var manifestText: String
    var zustand: String
    var aktiv = true
    var pruefungZeit: Double = 0
    var pruefungErgebnis = ""
    var position = 0
    /// Die zuletzt von Frank freigegebenen Rechte; nur die Oberfläche setzt das.
    var freigabe = ""
    /// Zustand vor dem Papierkorb.
    var vorPapierkorb = ""
    var id: String { kennung }

    var manifest: JSONObjekt { JSON.objekt(manifestText) ?? [:] }
    func text(_ k: String) -> String { manifest[k] as? String ?? "" }
    func liste(_ k: String) -> [String] { manifest[k] as? [String] ?? [] }
    var rechte: JSONObjekt { manifest["rechte"] as? JSONObjekt ?? [:] }
    var konten: [String] { (rechte["konten"] as? [JSONObjekt] ?? []).compactMap { $0["id"] as? String } }
    var name: String { text("name").isEmpty ? kennung : text("name") }
    var nutzbar: Bool { zustand == HelferKatalog.bereit && aktiv }
}

/// Die eigenen Helfer dieses Macs: Katalog, Prüfung, Ausführung. Ablage: Helfer/katalog.json und je Helfer ein Ordner mit
/// aktueller Fassung (aktuell), letzter geprüfter Fassung (.vorher) und eigenem Speicher (speicher). Regeln: JarvisHelfer/README.md.
/// Mac-Programme laufen unter Franks Benutzer und sind nicht abgeschottet; Netz- und Dateigrenzen setzt hier das Review, nicht die Laufzeit.
@MainActor @Observable
final class HelferKatalog {
    static let shared = HelferKatalog()
    nonisolated static let bereit = "bereit", inPruefung = "in_pruefung", wartet = "wartet_auf_freigabe", fehlerhaft = "fehlerhaft", papierkorb = "papierkorb"
    nonisolated private static let frist = 600.0
    nonisolated private static let gesperrteWerkzeuge = try! NSRegularExpression(pattern: "^(programmier.*|helfer_.*|mac_helfer_.*|agent_starten|mail_senden|.*loeschen|.*einstellungen.*|mac_.*|openlauncher_sitzung|terminal_befehl)$")

    private(set) var eintraege: [HelferEintrag] = []
    private var installiertGerade: Set<String> = []
    private var gestartet = false

    private let wurzel: URL = {
        let u = Pfade.unterstuetzung.appendingPathComponent("Helfer", isDirectory: true)
        try? FileManager.default.createDirectory(at: u, withIntermediateDirectories: true)
        return u
    }()
    private var katalogDatei: URL { wurzel.appendingPathComponent("katalog.json") }
    private var eingang: URL { wurzel.appendingPathComponent("eingang", isDirectory: true) }
    private func ordner(_ k: String) -> URL { wurzel.appendingPathComponent(k, isDirectory: true) }

    private init() {
        if let d = try? Data(contentsOf: katalogDatei), let l = try? JSONDecoder().decode([HelferEintrag].self, from: d) { eintraege = l }
        // Mitten in der Prüfung beendet: Ohne bestandene Prüfung gilt der Helfer nicht als verfügbar.
        for i in eintraege.indices where eintraege[i].zustand == Self.inPruefung {
            eintraege[i].zustand = Self.fehlerhaft
            eintraege[i].pruefungErgebnis = "Prüfung wurde unterbrochen."
        }
    }

    func starten() {
        guard !gestartet else { return }
        gestartet = true
        try? FileManager.default.createDirectory(at: eingang, withIntermediateDirectories: true)
        AgentenKanal.shared.katalogSenden()
        Task { while true { await eingangPruefen(); try? await Task.sleep(for: .seconds(30)) } }
    }

    private func sichern() {
        if let d = try? JSONEncoder().encode(eintraege) { try? d.write(to: katalogDatei, options: .atomic) }
        AgentenKanal.shared.katalogSenden()
    }

    var sortiert: [HelferEintrag] { eintraege.filter { $0.zustand != Self.papierkorb }.sorted { ($0.position, $0.kennung) < ($1.position, $1.kennung) } }
    var imPapierkorb: [HelferEintrag] { eintraege.filter { $0.zustand == Self.papierkorb } }
    func finde(_ k: String) -> HelferEintrag? { eintraege.first { $0.kennung == k } }
    func nutzer(konto: String) -> [String] { eintraege.filter { $0.konten.contains(konto) }.map(\.name) }
    func hatVorherigeFassung(_ k: String) -> Bool { FileManager.default.fileExists(atPath: ordner(k).appendingPathComponent(".vorher/helfer.json").path) }

    private func aendere(_ k: String, _ f: (inout HelferEintrag) -> Void) {
        guard let i = eintraege.firstIndex(where: { $0.kennung == k }) else { return }
        f(&eintraege[i])
        sichern()
    }

    func kanalListe() -> [JSONObjekt] { sortiert.map(Self.katalogZeile) }

    nonisolated private static func katalogZeile(_ e: HelferEintrag) -> JSONObjekt {
        var m = e.manifest
        for weg in ["tests", "datei", "rolle"] { m.removeValue(forKey: weg) }
        m["kennung"] = e.kennung
        m["ort"] = "mac"
        m["zustand"] = e.zustand
        m["aktiv"] = e.aktiv
        m["position"] = e.position
        m["pruefung"] = ["zeit": e.pruefungZeit, "ergebnis": e.pruefungErgebnis] as JSONObjekt
        return m
    }

    // MARK: Suchen

    func suche(_ wort: String) -> [HelferEintrag] {
        let woerter = wort.lowercased().split(whereSeparator: { !$0.isLetter && !$0.isNumber }).map(String.init).filter { $0.count > 1 }
        guard !woerter.isEmpty else { return sortiert }
        func punkte(_ e: HelferEintrag) -> Int {
            let hoch = ([e.kennung, e.name] + e.liste("schlagworte")).joined(separator: " ").lowercased()
            let rest = ([e.text("zweck")] + e.liste("faehigkeiten")).joined(separator: " ").lowercased()
            return woerter.reduce(0) { $0 + (hoch.contains($1) ? 3 : 0) + (rest.contains($1) ? 1 : 0) }
        }
        return sortiert.map { ($0, punkte($0)) }.filter { $0.1 > 0 }.sorted { $0.1 > $1.1 }.map(\.0)
    }

    // MARK: Verwalten

    /// Alles, was Modell und Handy dürfen. Freigeben und endgültig Löschen gibt es bewusst nur in der Oberfläche.
    func verwalten(_ k: String, aktion: String) -> (ok: Bool, text: String) {
        guard let e = finde(k) else { return (false, "Keinen Helfer „\(k)“ gefunden.") }
        switch aktion {
        case "aktivieren": aendere(k) { $0.aktiv = true }; return (true, "\(e.name) ist aktiv.")
        case "deaktivieren": aendere(k) { $0.aktiv = false }; return (true, "\(e.name) ist deaktiviert.")
        case "papierkorb":
            guard e.zustand != Self.papierkorb else { return (true, "\(e.name) liegt schon im Papierkorb.") }
            aendere(k) { $0.vorPapierkorb = $0.zustand; $0.zustand = Self.papierkorb }
            return (true, "\(e.name) liegt im Papierkorb. Konten und Schlüssel bleiben unberührt.")
        case "wiederherstellen":
            guard e.zustand == Self.papierkorb else { return (false, "\(e.name) liegt nicht im Papierkorb.") }
            aendere(k) { $0.zustand = $0.vorPapierkorb.isEmpty ? Self.fehlerhaft : $0.vorPapierkorb; $0.vorPapierkorb = "" }
            return (true, "\(e.name) ist wiederhergestellt.")
        case "rueckrollen":
            return rueckrollen(k)
        default:
            return (false, "Unbekannte aktion.")
        }
    }

    private func rueckrollen(_ k: String) -> (ok: Bool, text: String) {
        let fm = FileManager.default
        let vorher = ordner(k).appendingPathComponent(".vorher", isDirectory: true), aktuell = ordner(k).appendingPathComponent("aktuell", isDirectory: true)
        guard let d = try? Data(contentsOf: vorher.appendingPathComponent("helfer.json")), let text = String(data: d, encoding: .utf8) else {
            return (false, "Es gibt keine frühere geprüfte Fassung.")
        }
        try? fm.removeItem(at: aktuell)
        do { try fm.copyItem(at: vorher, to: aktuell) } catch { return (false, "Zurücksetzen gescheitert: \(error.localizedDescription)") }
        aendere(k) { e in
            e.manifestText = text
            e.zustand = Self.brauchtFreigabe(e.manifest) && e.freigabe != Self.signatur(e.manifest) ? Self.wartet : Self.bereit
            e.pruefungZeit = Date().timeIntervalSince1970
            e.pruefungErgebnis = "Auf die letzte geprüfte Fassung \(e.text("version")) zurückgesetzt."
        }
        return (true, "Die letzte geprüfte Fassung gilt wieder.")
    }

    func freigeben(_ k: String) {
        aendere(k) { e in
            e.freigabe = Self.signatur(e.manifest)
            if e.zustand == Self.wartet { e.zustand = Self.bereit }
        }
        log("werkzeug", "Helfer \(k): Rechte von Frank freigegeben", wo: "Eigene Tools")
    }

    func verschieben(_ k: String, um schritt: Int) {
        var l = sortiert.map(\.kennung)
        guard let i = l.firstIndex(of: k), l.indices.contains(i + schritt) else { return }
        l.swapAt(i, i + schritt)
        for (n, kennung) in l.enumerated() { if let j = eintraege.firstIndex(where: { $0.kennung == kennung }) { eintraege[j].position = n } }
        sichern()
    }

    func endgueltigLoeschen(_ k: String) {
        guard finde(k)?.zustand == Self.papierkorb else { return }
        try? FileManager.default.removeItem(at: ordner(k))
        eintraege.removeAll { $0.kennung == k }
        sichern()
        log("werkzeug", "Helfer \(k) endgültig gelöscht", wo: "Eigene Tools")
    }

    // MARK: Installieren

    nonisolated private static func brauchtFreigabe(_ m: JSONObjekt) -> Bool {
        let r = m["rechte"] as? JSONObjekt ?? [:]
        // Mac-Programme rufen keine Jarvis-Werkzeuge auf; erklärt einer trotzdem welche, entscheidet Frank.
        return !(r["konten"] as? [Any] ?? []).isEmpty || !(r["werkzeuge"] as? [Any] ?? []).isEmpty || r["extern"] as? Bool == true
    }

    nonisolated private static func signatur(_ m: JSONObjekt) -> String { JSON.text(m["rechte"] as? JSONObjekt ?? [:]) }

    nonisolated private static func pruefeSteckbrief(_ m: JSONObjekt, dateien: [String: String], review: JSONObjekt) -> String? {
        let k = m["kennung"] as? String ?? ""
        guard k.count <= 40, k.range(of: "^[a-z0-9][a-z0-9-]*$", options: .regularExpression) != nil else { return "Die Kennung muss klein aus a-z, 0-9 und - bestehen, höchstens 40 Zeichen." }
        guard m["ort"] as? String == "mac" else { return "Dieser Helfer ist nicht für den Mac bestimmt (ort)." }
        let art = m["art"] as? String ?? ""
        if art == "skript" {
            let d = m["datei"] as? String ?? ""
            guard d == "main.py" || d == "main.sh", dateien[d] != nil else { return "datei muss main.py oder main.sh sein und mitgeliefert werden." }
        } else if art == "agent" {
            guard !(m["rolle"] as? String ?? "").isEmpty else { return "Ein Agent braucht eine rolle." }
        } else { return "art muss skript oder agent sein." }
        for name in dateien.keys where name.hasPrefix("/") || name.split(separator: "/").contains("..") || name.hasPrefix(".") {
            return "Unzulässiger Dateiname \(name)."
        }
        let tests = m["tests"] as? [JSONObjekt] ?? []
        guard tests.contains(where: { ($0["erwartet"] as? JSONObjekt)?["ok"] as? Bool == true }), tests.contains(where: { ($0["erwartet"] as? JSONObjekt)?["ok"] as? Bool == false }) else {
            return "Mindestens ein Normal- und ein Fehlerfall gehören in tests."
        }
        for w in ((m["rechte"] as? JSONObjekt)?["werkzeuge"] as? [String] ?? []) {
            if gesperrteWerkzeuge.firstMatch(in: w, range: NSRange(w.startIndex..., in: w)) != nil { return "Das Werkzeug \(w) ist für Helfer gesperrt." }
        }
        guard (review["kritisch_offen"] as? NSNumber)?.intValue == 0, !(review["zusammenfassung"] as? String ?? "").isEmpty else {
            return "Ohne abgeschlossenes Review (kritisch_offen = 0, zusammenfassung) wird nichts registriert."
        }
        return nil
    }

    /// Prüft die neue Fassung auf diesem Mac. Bestanden: Sie gilt, die bisherige wird zur letzten geprüften. Durchgefallen: Die bisherige bleibt.
    func installieren(manifest m: JSONObjekt, dateien: [String: String], review: JSONObjekt) async -> JSONObjekt {
        func nein(_ t: String) -> JSONObjekt { ["ok": false, "fehler": t] }
        if let f = Self.pruefeSteckbrief(m, dateien: dateien, review: review) { return nein(f) }
        let k = m["kennung"] as? String ?? ""
        guard installiertGerade.insert(k).inserted else { return nein("Dieser Helfer wird gerade schon geprüft.") }
        defer { installiertGerade.remove(k) }
        let fm = FileManager.default
        let neu = ordner(k).appendingPathComponent("neu", isDirectory: true), aktuell = ordner(k).appendingPathComponent("aktuell", isDirectory: true), vorher = ordner(k).appendingPathComponent(".vorher", isDirectory: true)
        try? fm.removeItem(at: neu)
        do {
            try fm.createDirectory(at: neu, withIntermediateDirectories: true)
            for (name, inhalt) in dateien {
                let ziel = neu.appendingPathComponent(name)
                try fm.createDirectory(at: ziel.deletingLastPathComponent(), withIntermediateDirectories: true)
                try inhalt.write(to: ziel, atomically: true, encoding: .utf8)
            }
            try Data(JSON.text(m, schoen: true).utf8).write(to: neu.appendingPathComponent("helfer.json"))
        } catch { return nein("Paket nicht gespeichert: \(error.localizedDescription)") }

        let manifestText = JSON.text(m)
        let alt = finde(k)
        let bewaehrt = alt.map { $0.zustand == Self.bereit || $0.zustand == Self.wartet } ?? false
        if !bewaehrt {
            if alt == nil {
                let position = (eintraege.map(\.position).max() ?? -1) + 1
                eintraege.append(HelferEintrag(kennung: k, manifestText: manifestText, zustand: Self.inPruefung, position: position))
            } else { aendere(k) { $0.manifestText = manifestText; $0.zustand = Self.inPruefung } }
            sichern()
        }
        let fehlgeschlagen = await testen(m, ordner: neu, kennung: k)
        let jetzt = Date().timeIntervalSince1970
        let version = m["version"] as? String ?? ""
        if let fehlgeschlagen {
            try? fm.removeItem(at: neu)
            let ergebnis = "Prüfung von \(version) durchgefallen: \(fehlgeschlagen)"
            aendere(k) { e in
                if !bewaehrt { e.zustand = Self.fehlerhaft }
                e.pruefungZeit = jetzt
                e.pruefungErgebnis = bewaehrt ? ergebnis + " Die bisherige Fassung gilt weiter." : ergebnis
            }
            log("fehler", "Helfer \(k): \(ergebnis)", wo: "Eigene Tools")
            return ["ok": false, "fehler": ergebnis, "zustand": finde(k)?.zustand ?? Self.fehlerhaft]
        }
        if bewaehrt, fm.fileExists(atPath: aktuell.path) {
            try? fm.removeItem(at: vorher)
            try? fm.moveItem(at: aktuell, to: vorher)
        }
        try? fm.removeItem(at: aktuell)
        do { try fm.moveItem(at: neu, to: aktuell) } catch { return nein("Fassung nicht übernommen: \(error.localizedDescription)") }
        let anzahl = (m["tests"] as? [Any])?.count ?? 0
        let ergebnis = "\(anzahl) Tests bestanden auf diesem Mac; Review: \((review["befunde"] as? NSNumber)?.intValue ?? 0) Befunde, keine kritischen offen."
        aendere(k) { e in
            e.manifestText = manifestText
            e.zustand = Self.brauchtFreigabe(m) && e.freigabe != Self.signatur(m) ? Self.wartet : Self.bereit
            e.pruefungZeit = jetzt
            e.pruefungErgebnis = ergebnis
        }
        let zustand = finde(k)?.zustand ?? Self.bereit
        log("werkzeug", "Helfer \(k) \(version) registriert: \(zustand)", wo: "Eigene Tools", ergebnis: ergebnis)
        return ["ok": true, "zustand": zustand, "pruefung": ["ergebnis": ergebnis, "zeit": jetzt] as JSONObjekt]
    }

    /// nil = alle Tests bestanden. Tests bekommen nie einen Schlüssel.
    private func testen(_ m: JSONObjekt, ordner neu: URL, kennung: String) async -> String? {
        let tests = m["tests"] as? [JSONObjekt] ?? []
        for t in tests {
            let name = t["name"] as? String ?? "Test"
            let erwartet = t["erwartet"] as? JSONObjekt ?? [:]
            let eingabe = t["eingabe"] as? JSONObjekt ?? [:]
            let r = await lauf(m, ordner: neu, kennung: kennung, eingabe: JSON.text(eingabe), konten: [:], frist: 120)
            let sollOk = erwartet["ok"] as? Bool ?? true
            if r.ok != sollOk { return "„\(name)“: \(r.ok ? "lief durch, obwohl ein Fehler erwartet war" : "Fehler: \(String(r.fehler.prefix(300)))")" }
            let gesamt = r.text + " " + r.datenText + " " + r.fehler
            for teil in erwartet["enthaelt"] as? [String] ?? [] where !gesamt.contains(teil) { return "„\(name)“: Ergebnis enthält „\(teil)“ nicht." }
        }
        return nil
    }

    func eingangPruefen() async {
        let fm = FileManager.default
        guard let liste = try? fm.contentsOfDirectory(at: eingang, includingPropertiesForKeys: nil) else { return }
        for paket in liste where !paket.lastPathComponent.hasPrefix(".") {
            let kennung = paket.lastPathComponent
            guard let d = try? Data(contentsOf: paket.appendingPathComponent("registrierung.json")), let reg = JSON.objekt(d) else { continue }
            var ergebnis: JSONObjekt = ["ok": false, "fehler": "Unbekannter Bauschlüssel."]
            let schluessel = reg["bau_schluessel"] as? String ?? ""
            if HelferWerkzeuge.bauSchluesselBekannt(schluessel) {
                if let md = try? Data(contentsOf: paket.appendingPathComponent("helfer.json")), let m = JSON.objekt(md) {
                    var dateien: [String: String] = [:]
                    if let e = fm.enumerator(at: paket, includingPropertiesForKeys: [.isRegularFileKey]) {
                        for case let u as URL in e.allObjects {
                            guard (try? u.resourceValues(forKeys: [.isRegularFileKey]).isRegularFile) == true else { continue }
                            let rel = String(u.standardizedFileURL.path.dropFirst(paket.standardizedFileURL.path.count + 1))
                            if rel == "helfer.json" || rel == "registrierung.json" { continue }
                            if let t = try? String(contentsOf: u, encoding: .utf8) { dateien[rel] = t }
                        }
                    }
                    ergebnis = await installieren(manifest: m, dateien: dateien, review: reg["review"] as? JSONObjekt ?? [:])
                    if ergebnis["ok"] as? Bool == true { HelferWerkzeuge.bauSchluesselVerbrauchen(schluessel) }
                } else { ergebnis = ["ok": false, "fehler": "helfer.json fehlt oder ist ungültig."] }
            }
            try? fm.removeItem(at: paket)
            let ok = ergebnis["ok"] as? Bool == true
            let text = ok ? "Helfer \(kennung): \(ergebnis["zustand"] as? String ?? "")" : "Helfer \(kennung) abgelehnt: \(ergebnis["fehler"] as? String ?? "")"
            log(ok ? "werkzeug" : "fehler", text, wo: "Eigene Tools")
            Task { _ = await Shell.mitteilung("Eigene Tools", String(text.prefix(180))) }
        }
    }

    // MARK: Ausführen

    struct Lauf: Sendable { var ok = false; var text = ""; var datenText = "{}"; var fehler = ""; var dateien: [(name: String, pfad: String)] = []; var dauerMs = 0 }

    /// Ruft einen freigegebenen Helfer auf; Frank kann ihn deaktivieren, das Modell kann Rechte nicht erweitern.
    func ausfuehren(_ k: String, eingabe: JSONObjekt) async -> Lauf {
        guard let e = finde(k) else { return Lauf(fehler: "Keinen Helfer „\(k)“ gefunden.") }
        guard e.aktiv else { return Lauf(fehler: "\(e.name) ist deaktiviert.") }
        guard e.zustand == Self.bereit else {
            return Lauf(fehler: e.zustand == Self.wartet ? "\(e.name) wartet auf Franks Freigabe in Eigene Tools." : "\(e.name) ist nicht einsatzbereit (\(e.zustand)).")
        }
        var konten: [String: String] = [:]
        for id in e.konten {
            guard let wert = Self.kontoWert(id) else { return Lauf(fehler: "Das Konto „\(id)“ fehlt oder ist gesperrt. Es steht unter Konten und APIs.") }
            konten["KONTO_" + id.uppercased().replacingOccurrences(of: "-", with: "_")] = wert
        }
        let beginn = Date()
        var r = await lauf(e.manifest, ordner: ordner(k).appendingPathComponent("aktuell", isDirectory: true), kennung: k, eingabe: JSON.text(eingabe), konten: konten, frist: Self.frist)
        r.dauerMs = Int(Date().timeIntervalSince(beginn) * 1000)
        log("werkzeug", "Helfer \(k): \(r.ok ? "ok" : "Fehler")", wo: "Eigene Tools", werkzeug: k, fehler: r.ok ? nil : r.fehler, dauerMs: r.dauerMs)
        return r
    }

    private static func kontoWert(_ id: String) -> String? {
        let z = Einstellungen.shared.zugang
        if let w = z.weitere[id] { return w.gesperrt || w.wert.isEmpty ? nil : w.wert }
        guard !z.gesperrtFuerHelfer.contains(id) else { return nil }
        let wert: String
        switch id {
        case "anthropic": wert = z.anthropic
        case "openai": wert = z.openai
        case "gemini": wert = z.gemini
        case "google": wert = z.google
        case "groq": wert = z.groq
        default: return nil
        }
        return wert.isEmpty ? nil : wert
    }

    private func lauf(_ m: JSONObjekt, ordner: URL, kennung: String, eingabe: String, konten: [String: String], frist: Double) async -> Lauf {
        if m["art"] as? String == "agent" { return await agentLauf(rolle: m["rolle"] as? String ?? "", name: kennung, eingabe: eingabe, frist: frist) }
        let basis = self.ordner(kennung)
        return await Self.programmLauf(datei: ordner.appendingPathComponent(m["datei"] as? String ?? "main.sh"), basis: basis, eingabe: eingabe, konten: konten, frist: frist)
    }

    private func agentLauf(rolle: String, name: String, eingabe: String, frist: Double) async -> Lauf {
        let g = GespraechsSpeicher.shared.neu()
        g.titel = "Helfer \(name)"
        let auftrag = "[EIGENES TOOL \(name) – Lauf ohne Frank]\n\(rolle)\n\nEingabe als JSON:\n\(eingabe)\n\nAntworte nur mit dem Ergebnis. Fehlt etwas Nötiges, beginne mit FEHLER:"
        let gesperrt: Set<String> = ["jarvis_merken", "jarvis_vergessen", "jarvis_langzeitkontext_ersetzen", "mail_senden", "programmieren", "programmier_nachtrag", "programmier_antwort", "programmierer_sprechen", "agent_starten", "mac_dialog", "mac_helfer_bauen", "helfer_bauen", "mac_helfer_verwalten", "helfer_verwalten", "helfer_registrieren", "mac_einstellungen", "einstellungen", "mac_befehl", "mac_applescript", "mac_bedienen", "mac_tastatur", "terminal_befehl", "openlauncher_sitzung"]
        let t = Task { @MainActor in await AgentenKanal.$imLauf.withValue(true) { await JarvisAgent.shared.frage(auftrag, in: g, ohneWerkzeuge: gesperrt) } }
        let fertig = await mitFrist(frist) { _ = await t.value }
        if !fertig { t.cancel(); GespraechsSpeicher.shared.loeschen(g); return Lauf(fehler: "Keine Antwort innerhalb der Frist.") }
        let antwort = Geheimnisse.schwaerzen(await t.value).trimmingCharacters(in: .whitespacesAndNewlines)
        GespraechsSpeicher.shared.loeschen(g)
        if antwort.hasPrefix("FEHLER:") || antwort.isEmpty || (antwort.components(separatedBy: "\n\n").last ?? "").hasPrefix("⚠️") {
            return Lauf(fehler: antwort.isEmpty ? "Keine Antwort." : String(antwort.prefix(1000)))
        }
        return Lauf(ok: true, text: antwort)
    }

    /// Eigener Prozess statt Shell.zsh: nur die festgelegte Umgebung, Eingabe auf stdin.
    nonisolated private static func programmLauf(datei: URL, basis: URL, eingabe: String, konten: [String: String], frist: Double) async -> Lauf {
        let fm = FileManager.default
        let kennung = UUID().uuidString
        let arbeit = fm.temporaryDirectory.appendingPathComponent("helfer-\(kennung)", isDirectory: true)
        let ausgabe = basis.appendingPathComponent("ausgabe/\(kennung)", isDirectory: true)
        let speicher = basis.appendingPathComponent("speicher", isDirectory: true)
        for u in [arbeit, ausgabe, speicher] { try? fm.createDirectory(at: u, withIntermediateDirectories: true) }
        defer { try? fm.removeItem(at: arbeit) }
        aufraeumen(basis.appendingPathComponent("ausgabe", isDirectory: true))
        let ein = arbeit.appendingPathComponent(".eingabe"), aus = arbeit.appendingPathComponent(".aus"), fehl = arbeit.appendingPathComponent(".fehler")
        try? Data(eingabe.utf8).write(to: ein)
        fm.createFile(atPath: aus.path, contents: nil); fm.createFile(atPath: fehl.path, contents: nil)
        let p = Process()
        if datei.pathExtension == "py" {
            // Nicht zuerst /usr/bin/python3: Dieser Starter scheitert hier an der nicht akzeptierten Xcode-Lizenz.
            let python = ["/opt/homebrew/bin/python3", "/usr/local/bin/python3", "/Library/Developer/CommandLineTools/usr/bin/python3", "/usr/bin/python3"].first { fm.isExecutableFile(atPath: $0) } ?? "/usr/bin/python3"
            p.executableURL = URL(fileURLWithPath: python)
        } else { p.executableURL = URL(fileURLWithPath: "/bin/zsh") }
        p.arguments = [datei.path]
        p.currentDirectoryURL = arbeit
        var umgebung = ["PATH": "/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin", "HOME": Pfade.heim.path, "HELFER_AUSGABE": ausgabe.path, "HELFER_SPEICHER": speicher.path]
        konten.forEach { umgebung[$0.key] = $0.value }
        p.environment = umgebung
        guard let inH = try? FileHandle(forReadingFrom: ein), let ausH = try? FileHandle(forWritingTo: aus), let fehlH = try? FileHandle(forWritingTo: fehl) else { return Lauf(fehler: "Der Helfer ließ sich nicht vorbereiten.") }
        defer { try? inH.close(); try? ausH.close(); try? fehlH.close() }
        p.standardInput = inH; p.standardOutput = ausH; p.standardError = fehlH
        let beendet = HelferEndeSignal()
        p.terminationHandler = { _ in beendet.setzen() }
        do { try p.run() } catch { return Lauf(fehler: "Der Helfer startet nicht: \(error.localizedDescription)") }
        let wachhund = Task.detached {
            try? await Task.sleep(for: .seconds(frist))
            if p.isRunning { p.terminate(); try? await Task.sleep(for: .seconds(3)); if p.isRunning { kill(p.processIdentifier, SIGKILL) } }
        }
        await beendet.warten()
        wachhund.cancel()
        let stdout = Geheimnisse.schwaerzen(String(decoding: (try? Data(contentsOf: aus)) ?? Data(), as: UTF8.self))
        let stderr = Geheimnisse.schwaerzen(String(decoding: (try? Data(contentsOf: fehl)) ?? Data(), as: UTF8.self))
        guard p.terminationReason == .exit, p.terminationStatus == 0 else {
            let grund = stderr.trimmingCharacters(in: .whitespacesAndNewlines)
            return Lauf(fehler: p.terminationReason == .uncaughtSignal ? "Der Helfer wurde abgebrochen (Frist oder Absturz)." : (grund.isEmpty ? "Exit-Code \(p.terminationStatus)." : String(grund.suffix(1500))))
        }
        guard let j = JSON.objekt(stdout.trimmingCharacters(in: .whitespacesAndNewlines)) else { return Lauf(fehler: "Der Helfer lieferte kein JSON.") }
        var dateien: [(name: String, pfad: String)] = []
        for d in j["dateien"] as? [JSONObjekt] ?? [] {
            let name = (d["name"] as? String).map { URL(fileURLWithPath: $0).lastPathComponent } ?? ""
            let roh = d["pfad"] as? String ?? name
            let voll = (roh.hasPrefix("/") ? URL(fileURLWithPath: roh) : ausgabe.appendingPathComponent(roh)).standardizedFileURL.path
            // Nur aus dem eigenen Ausgabeordner: Sonst gäbe ein Helfer beliebige Dateien des Macs weiter.
            guard !name.isEmpty, voll.hasPrefix(ausgabe.standardizedFileURL.path + "/"), fm.fileExists(atPath: voll) else { return Lauf(fehler: "Die gemeldete Datei „\(name)“ liegt nicht im Ausgabeordner oder fehlt.") }
            dateien.append((name, voll))
        }
        return Lauf(ok: true, text: j["text"] as? String ?? "", datenText: JSON.text(j["daten"] as? JSONObjekt ?? [:]), dateien: dateien)
    }

    /// Ergebnisdateien bleiben sieben Tage.
    nonisolated private static func aufraeumen(_ ordner: URL) {
        let grenze = Date().addingTimeInterval(-7 * 86_400)
        for u in (try? FileManager.default.contentsOfDirectory(at: ordner, includingPropertiesForKeys: [.contentModificationDateKey])) ?? [] {
            if let d = try? u.resourceValues(forKeys: [.contentModificationDateKey]).contentModificationDate, d < grenze { try? FileManager.default.removeItem(at: u) }
        }
    }

    // MARK: Kanal zum Handy

    func kanalNachricht(art: String, text: String) async -> String {
        let j = JSON.objekt(text) ?? [:]
        switch art {
        case "helfer_liste":
            return JSON.text(["helfer": kanalListe()] as JSONObjekt)
        case "helfer_aufruf":
            let r = await ausfuehren(j["helfer"] as? String ?? "", eingabe: j["eingabe"] as? JSONObjekt ?? [:])
            guard r.ok else { return JSON.text(["ok": false, "fehler": r.fehler] as JSONObjekt) }
            var rest = 6_000_000
            var dateien: [JSONObjekt] = []
            for d in r.dateien {
                let inhalt = (try? Data(contentsOf: URL(fileURLWithPath: d.pfad))) ?? Data()
                if inhalt.count > rest { dateien.append(["name": d.name, "zu_gross": true, "pfad": d.pfad]); continue }
                rest -= inhalt.count
                dateien.append(["name": d.name, "base64": inhalt.base64EncodedString()])
            }
            return JSON.text(["ok": true, "text": r.text, "daten": JSON.objekt(r.datenText) ?? [:], "dateien": dateien, "dauer_ms": r.dauerMs] as JSONObjekt)
        case "helfer_installieren":
            var dateien: [String: String] = [:]
            (j["dateien"] as? JSONObjekt ?? [:]).forEach { if let t = $0.value as? String { dateien[$0.key] = t } }
            return JSON.text(await installieren(manifest: j["manifest"] as? JSONObjekt ?? [:], dateien: dateien, review: j["review"] as? JSONObjekt ?? [:]))
        case "helfer_verwalten":
            let r = verwalten(j["helfer"] as? String ?? "", aktion: j["aktion"] as? String ?? "")
            return JSON.text(["ok": r.ok, r.ok ? "text" : "fehler": r.text] as JSONObjekt)
        case "cli_recherche":
            return await HelferWerkzeuge.cliRecherche(frage: j["frage"] as? String ?? "", anzahl: (j["anzahl"] as? NSNumber)?.intValue ?? 3, modell: j["modell"] as? String ?? "")
        default:
            return JSON.text(["ok": false, "fehler": "Unbekannte Art \(art)."] as JSONObjekt)
        }
    }
}

/// Einmaliges Signal aus dem terminationHandler an einen wartenden Task.
private final class HelferEndeSignal: @unchecked Sendable {
    private let lock = NSLock()
    private var gesetzt = false
    private var wartend: CheckedContinuation<Void, Never>?
    func setzen() {
        lock.lock(); gesetzt = true; let w = wartend; wartend = nil; lock.unlock()
        w?.resume()
    }
    func warten() async {
        await withCheckedContinuation { w in
            lock.lock()
            if gesetzt { lock.unlock(); w.resume() } else { wartend = w; lock.unlock() }
        }
    }
}
