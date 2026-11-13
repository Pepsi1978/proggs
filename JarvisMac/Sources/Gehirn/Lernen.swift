import Foundation
import Observation

/// Die Regel fürs Live-Gedächtnis wie am Handy (`Merkdatei.KNAPP` in RegelnFaehigkeit.kt), Wortlaut unverändert: Jeder Prompt, der
/// Gedächtnistext schreibt (Lernen, Verdichten), setzt genau diese Konstante ein, nie eine eigene Formulierung.
enum Merkregel {
    static let knappText = "So knapp wie möglich, so vollständig wie nötig: Ein fremdes Modell muss den Text ohne Vorwissen richtig verstehen. " +
        "Behalte Namen, Zahlen, Daten, Fristen, Geltung, Auslöser, Ausnahmen und Vorbehalte („laut Frank“, „nicht verifiziert“). " +
        "Streiche Füllwörter, Wiederholungen, Beispiele, Begründungen ohne Einfluss auf das Verhalten und alles, was schon anderswo steht."
    static let knapp = knappText + " Ein Thema je Eintrag."
}

/// Lernen am Mac: Ruht ein Gespräch 10 Minuten, prüft ein kleines Modell, ob Frank darin neue dauerhafte Angaben
/// oder ausdrückliche Entscheidungen gemacht hat, die Jarvis noch nicht kennt – und gibt sie mit jarvis_merken an
/// das gemeinsame Gedächtnis zurück. So fließt, was am Mac passiert, ins Live-Gedächtnis aller Geräte.
@MainActor
final class Lernen {
    static let shared = Lernen()
    private var zeitgeber: [UUID: Task<Void, Never>] = [:]

    func merkeAktivitaet(_ g: Gespraech) {
        // Im Kanalgespräch spricht Jarvis am Handy, nicht Frank: Daraus wird nichts über Frank gelernt.
        guard Einstellungen.shared.lernenAn, g.titel != AgentenKanal.titel else { return }
        zeitgeber[g.id]?.cancel()
        zeitgeber[g.id] = Task { [weak self] in
            try? await Task.sleep(for: .seconds(600))
            guard !Task.isCancelled else { return }
            await self?.lerne(aus: g)
        }
    }

    private func lerne(aus g: Gespraech) async {
        // Läuft gerade etwas, später noch einmal versuchen statt diese Phase zu verlieren.
        if g.arbeitet { merkeAktivitaet(g); return }
        let neu = Array(g.nachrichten.dropFirst(min(g.gelerntBis, g.nachrichten.count)))
        guard neu.contains(where: { $0.rolle == .frank }) else { return }
        let stand = g.nachrichten.count
        let verlauf = neu.suffix(30).map { "\($0.rolle == .frank ? "Frank" : "Jarvis"): \($0.text.prefix(1500))" }.joined(separator: "\n")
        let bekannt = await McpKlient.shared.text("jarvis_gemerktes_lesen", [:]) ?? ""
        let prompt = """
        Gespräch zwischen Frank und Jarvis am Mac:
        \(verlauf)

        Bereits gemerkt:
        \(bekannt.prefix(30000))

        Finde nur NEUE dauerhafte Angaben über Frank (Fakten, Vorlieben, Ziele, Regeln für Jarvis) oder AUSDRÜCKLICHE \
        Entscheidungen, die Frank selbst gesagt hat und die oben noch nicht stehen. Keine Vermutungen, keine Vorschläge von Jarvis, \
        nichts Vorübergehendes, nichts, was Jarvis im Gespräch schon selbst gemerkt hat.
        Schreibregel für jeden Text: \(Merkregel.knapp)
        Antworte nur mit JSON:
        {"eintraege":[{"art":"info|regel|ziel|entscheidung|stil","text":"Eintrag in der dritten Person"}]}
        Ist nichts Neues dabei: {"eintraege":[]}
        """
        guard let antwort = await Kurzauftrag.frage(prompt, modell: "claude-sonnet-5-5") else { return }
        defer { g.gelerntBis = stand; g.speichern() }
        guard let a = antwort.firstIndex(of: "{"), let b = antwort.lastIndex(of: "}"), a < b,
              let o = JSON.objekt(String(antwort[a...b])), let liste = o["eintraege"] as? [JSONObjekt] else { return }
        for e in liste.prefix(5) {
            guard let text = e["text"] as? String, !text.isEmpty else { continue }
            let art = e["art"] as? String ?? "info"
            // „gelernt“ bleibt am Text erkennbar: Es ist nicht Franks ausdrückliche Vorgabe und begründet keine Rechte. Der Anlass steht nur beim Lesen und Pflegen.
            let r = await McpKlient.shared.rufe("jarvis_merken", ["art": art, "text": text + " (gelernt am Mac)", "anlass": String(("Gespräch am Mac: " + g.titel).prefix(120))])
            log("system", "Gelernt am Mac (\(art)): \(text) → \(r.fehler ? "Fehler" : "gemerkt")")
        }
        g.gelerntBis = stand
        g.speichern()
    }
}

/// Konsolidiert das Gemerkte nach der neuen Regel wie `jarvis_gemerktes_verdichten` am Handy: Ein Modell schlägt je Art knappere Fassungen und
/// zusammengelegte Dubletten vor; Frank sieht die Vorschläge und übernimmt sie mit einem Klick (das Werkzeug läuft nur auf seinen ausdrücklichen Wunsch).
/// Herkunft, Geltung, Frist und Anlass hält das Handy selbst fest.
@MainActor @Observable
final class Verdichter {
    static let shared = Verdichter()
    struct Neufassung: Hashable { let id: Int64; let alt: String; let neu: String }
    struct Zusammen: Hashable { let id: Int64; let in_: Int64; let alt: String }
    struct Vorschlag: Hashable, Identifiable {
        let id = UUID()
        let art: String
        let titel: String
        let neu: [Neufassung]
        let weg: [Zusammen]
    }

    private static let arten: [(art: String, kopf: String)] = [
        ("regel", "Regel (art regel):"), ("ziel", "Ziel (art ziel):"), ("info", "Notiz (art info):"), ("programmierregel", "Programmierregel (art programmierregel):"),
        ("stil", "Stilregel (art stil):"), ("entscheidung", "Entscheidung (art entscheidung):"),
    ]

    private(set) var laeuft = false
    /// Eigene Sperre fürs Übernehmen: Ein laufendes Vorschlagen verschluckt den Klick sonst still.
    private(set) var uebernimmt = false
    private(set) var meldung: String?
    private(set) var vorschlaege: [Vorschlag] = []

    /// Liest das Gemerkte und lässt je Art Vorschläge rechnen. Ändert noch nichts.
    func vorschlagen() async {
        guard !laeuft else { return }
        laeuft = true
        defer { laeuft = false }
        meldung = "Liest das Gemerkte und rechnet Vorschläge …"
        vorschlaege = []
        guard let text = await McpKlient.shared.text("jarvis_gemerktes_lesen", [:]) else { meldung = "Das Handy-Jarvis ist gerade nicht erreichbar."; return }
        let bloecke = Self.bloecke(McpKlient.ohneKontextHinweis(text)).filter { $0.eintraege.count > 1 }
        let ergebnis = await withTaskGroup(of: (Int, Vorschlag?).self) { g -> [Vorschlag] in
            for (i, b) in bloecke.enumerated() { g.addTask { (i, await Self.vorschlag(b)) } }
            var r: [(Int, Vorschlag)] = []
            for await (i, v) in g { if let v { r.append((i, v)) } }
            return r.sorted { $0.0 < $1.0 }.map(\.1)
        }
        vorschlaege = ergebnis
        meldung = ergebnis.isEmpty ? "Nichts zu verdichten: Das Gemerkte ist schon knapp."
            : "\(ergebnis.reduce(0) { $0 + $1.neu.count }) Neufassung(en), \(ergebnis.reduce(0) { $0 + $1.weg.count }) Zusammenlegung(en) vorgeschlagen. Erst „Übernehmen“ ändert etwas."
    }

    private static func vorschlag(_ b: Block) async -> Vorschlag? {
        let liste = b.eintraege.map { "[\($0.id)] \(ohneZusatz($0.text))" }.joined(separator: "\n")
        let prompt = """
        Gemerkte Einträge der Art „\(b.art)“ aus Jarvis' Gedächtnis:
        \(liste)

        Schreibe zu lang geratene Einträge knapper und lege echte Dubletten (gleiches Thema) zusammen. Schreibregel: \(Merkregel.knapp)
        Herkunft, Geltung, Erlaubnisse, Fristen und Anlass führt Jarvis selbst getrennt vom Text und bleiben erhalten: keine solchen Klammerzusätze in die Neufassung schreiben.
        Inhalt nie ändern, nichts hinzuerfinden, nichts weglassen, was das Verhalten beeinflusst. Unveränderte Einträge nicht nennen.
        Antworte nur mit JSON: {"eintraege":[{"id":1,"text":"Neufassung"}],"zusammenlegen":[{"id":5,"in":1}]}
        Bei zusammenlegen steht der Inhalt von id danach in der Neufassung von in (die dann in eintraege stehen muss). Ist nichts zu tun: {"eintraege":[],"zusammenlegen":[]}
        """
        guard let antwort = await Kurzauftrag.frage(prompt, modell: "claude-opus-5-5"),
              let a = antwort.firstIndex(of: "{"), let z = antwort.lastIndex(of: "}"), a < z,
              let o = JSON.objekt(String(antwort[a...z])) else { return nil }
        let alt = Dictionary(b.eintraege.map { ($0.id, $0.text) }, uniquingKeysWith: { x, _ in x })
        let neu: [Neufassung] = (o["eintraege"] as? [JSONObjekt] ?? []).compactMap { e in
            guard e["id"] != nil else { return nil }
            let id = ListenWert.ganz(e["id"])
            guard let vorher = alt[id], let t = (e["text"] as? String)?.trimmingCharacters(in: .whitespacesAndNewlines), !t.isEmpty else { return nil }
            let n = Neufassung(id: id, alt: vorher, neu: mitWoertlichemZusatz(vorher, t))
            return ohneZusatz(n.neu) != ohneZusatz(n.alt) ? n : nil
        }
        let weg: [Zusammen] = (o["zusammenlegen"] as? [JSONObjekt] ?? []).compactMap { e in
            guard e["id"] != nil, e["in"] != nil else { return nil }
            let id = ListenWert.ganz(e["id"]), ziel = ListenWert.ganz(e["in"])
            guard id != ziel, let vorher = alt[id], neu.contains(where: { $0.id == ziel }) else { return nil }
            return Zusammen(id: id, in_: ziel, alt: vorher)
        }
        return neu.isEmpty && weg.isEmpty ? nil : Vorschlag(art: b.art, titel: b.titel, neu: neu, weg: weg)
    }

    /// Übernimmt einen Vorschlag am Handy (jarvis_gemerktes_verdichten).
    func uebernehmen(_ v: Vorschlag) async {
        guard !uebernimmt else { meldung = "Eine Übernahme läuft noch, gleich noch einmal."; return }
        uebernimmt = true
        defer { uebernimmt = false }
        let r = await McpKlient.shared.rufe("jarvis_gemerktes_verdichten", [
            "art": v.art,
            "eintraege": v.neu.map { ["id": $0.id, "text": $0.neu] as JSONObjekt },
            "zusammenlegen": v.weg.map { ["id": $0.id, "in": $0.in_] as JSONObjekt },
        ])
        meldung = McpKlient.ohneKontextHinweis(r.text)
        log("gedaechtnis", "Gemerktes verdichtet (\(v.art))", wo: "Gedächtnis", fehler: r.fehler ? r.text : nil)
        if !r.fehler { vorschlaege.removeAll { $0.id == v.id } }
    }

    func verwerfen(_ v: Vorschlag) { vorschlaege.removeAll { $0.id == v.id } }

    /// „(gelernt am Laptop)“ und „(gelernt am Mac)“ stehen wörtlich im Text (die Rechner schreiben sie so): Sie bleiben an der Neufassung.
    private static func mitWoertlichemZusatz(_ alt: String, _ neu: String) -> String {
        guard let m = regexTreffer(#"\(gelernt am (Laptop|Mac)\)"#, alt)?[0] else { return neu }
        return neu.hasSuffix(m) ? neu : neu + " " + m
    }

    /// Text ohne die Zusätze, die Jarvis selbst führt (Herkunft, Geltung, Erlaubnis, Frist): hinten angehängte Klammern mit diesen Anfängen, auch mehrere.
    private static func ohneZusatz(_ t: String) -> String {
        var t = t
        let muster = #"\s*\((gelernt|gilt nur|Anlass|ERLAUBNIS|Erlaubnis|bis |abgelaufen)[^()]*(\([^()]*\)[^()]*)*\)\s*$"#
        while true {
            let vorher = t
            t = t.replacingOccurrences(of: muster, with: "", options: .regularExpression).trimmingCharacters(in: .whitespaces)
            if t == vorher { break }
        }
        return t.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private struct Block: Sendable { let art: String; let titel: String; var eintraege: [(id: Int64, text: String)] }

    private static func bloecke(_ text: String) -> [Block] {
        var aus: [Block] = []
        for roh in text.components(separatedBy: "\n") {
            let z = roh.trimmingCharacters(in: .whitespaces)
            if let kopf = arten.first(where: { $0.kopf == z }) {
                aus.append(Block(art: kopf.art, titel: kopf.kopf.components(separatedBy: " (")[0], eintraege: []))
                continue
            }
            if !aus.isEmpty, let m = regexTreffer(#"^\[(\d+)\]\s+(.+)$"#, z), let id = Int64(m[1]) { aus[aus.count - 1].eintraege.append((id, m[2])) }
        }
        return aus
    }
}
