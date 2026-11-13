import Foundation

/// Macht Text vorlesbar, bevor er an eine Stimme geht (Gegenstück zu SprechText.kt der Android-App).
/// Der angezeigte Text ändert sich nicht – nur der Weg zur Synthese wird aufbereitet.
enum SprechText {

    private static let abkuerzungen: [(String, String)] = [
        ("z. B.", "zum Beispiel"), ("z.B.", "zum Beispiel"), ("u. a.", "unter anderem"), ("u.a.", "unter anderem"),
        ("d. h.", "das heißt"), ("d.h.", "das heißt"), ("bzw.", "beziehungsweise"), ("ca.", "circa"),
        ("ggf.", "gegebenenfalls"), ("evtl.", "eventuell"), ("inkl.", "inklusive"), ("exkl.", "exklusive"),
        ("usw.", "und so weiter"), ("etc.", "und so weiter"), ("vgl.", "vergleiche"), ("Nr.", "Nummer"),
        ("Abb.", "Abbildung"), ("Kap.", "Kapitel"), ("Mio.", "Millionen"), ("Mrd.", "Milliarden"),
        ("Std.", "Stunden"), ("Sek.", "Sekunden"), ("Tsd.", "Tausend"), ("max.", "maximal"),
        ("s. o.", "siehe oben"), ("s. u.", "siehe unten"),
    ]

    private static let ersatz: [(String, String)] = [
        ("&", " und "), ("%", " Prozent "), ("€", " Euro "), ("$", " Dollar "), ("£", " Pfund "),
        ("+", " plus "), ("=", " gleich "), ("<", " kleiner als "), (">", " größer als "),
        ("→", " führt zu "), ("←", " kommt von "), ("@", " at "), ("/", " oder "), ("|", ". "),
        ("•", ". "), ("·", ". "), ("…", ". "), ("„", ""), ("“", ""), ("”", ""), ("\"", ""), ("«", ""), ("»", ""),
        ("–", " - "), ("—", " - "), ("\\", " "), ("^", " "), ("~", " "), ("©", ""), ("®", ""), ("™", ""),
    ]

    private static let einheiten: [String: String] = [
        "km": "Kilometer", "cm": "Zentimeter", "mm": "Millimeter", "m": "Meter", "l": "Liter", "ml": "Milliliter",
        "h": "Stunden", "min": "Minuten", "kWh": "Kilowattstunden", "°C": "Grad", "kg": "Kilogramm", "g": "Gramm",
        "ms": "Millisekunden", "GB": "Gigabyte", "MB": "Megabyte", "W": "Watt",
    ]

    private static let monate = ["Januar", "Februar", "März", "April", "Mai", "Juni",
                                 "Juli", "August", "September", "Oktober", "November", "Dezember"]

    private static let ordnung = ["erster", "zweiter", "dritter", "vierter", "fünfter", "sechster", "siebter", "achter",
                                  "neunter", "zehnter", "elfter", "zwölfter", "dreizehnter", "vierzehnter", "fünfzehnter",
                                  "sechzehnter", "siebzehnter", "achtzehnter", "neunzehnter", "zwanzigster",
                                  "einundzwanzigster", "zweiundzwanzigster", "dreiundzwanzigster", "vierundzwanzigster",
                                  "fünfundzwanzigster", "sechsundzwanzigster", "siebenundzwanzigster", "achtundzwanzigster",
                                  "neunundzwanzigster", "dreißigster", "einunddreißigster"]

    private static func ersetze(_ text: String, _ muster: String, _ f: (_ gruppen: [String]) -> String?) -> String {
        guard let re = try? NSRegularExpression(pattern: muster, options: []) else { return text }
        let ns = text as NSString
        var ergebnis = ""
        var letzte = 0
        for m in re.matches(in: text, range: NSRange(location: 0, length: ns.length)) {
            var gruppen: [String] = []
            for i in 0..<m.numberOfRanges {
                let r = m.range(at: i)
                gruppen.append(r.location == NSNotFound ? "" : ns.substring(with: r))
            }
            ergebnis += ns.substring(with: NSRange(location: letzte, length: m.range.location - letzte))
            ergebnis += f(gruppen) ?? gruppen[0]
            letzte = m.range.location + m.range.length
        }
        ergebnis += ns.substring(from: letzte)
        return ergebnis
    }

    private static func re(_ text: String, _ muster: String, _ vorlage: String, _ opt: NSRegularExpression.Options = []) -> String {
        guard let r = try? NSRegularExpression(pattern: muster, options: opt) else { return text }
        return r.stringByReplacingMatches(in: text, range: NSRange(location: 0, length: (text as NSString).length), withTemplate: vorlage)
    }

    /// Bereinigt Text für die Sprachausgabe: Markdown, Code, Webadressen, Quellen, Emojis raus, Zahlen lesbar.
    static func fuerStimme(_ roh: String) -> String {
        var t = roh
        if t.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return "" }
        t = re(t, "```[\\s\\S]*?```", " Codebeispiel. ")
        t = re(t, "`([^`]*)`", "$1")
        t = re(t, "\\[([^\\]]+)\\]\\([^)]*\\)", "$1")
        t = re(t, "(?i)\\b(?:https?://|www\\.)\\S+", " Link ")
        t = re(t, "\\b[\\w.-]+@[\\w.-]+\\.\\w{2,}\\b", " E-Mail-Adresse ")
        t = re(t, "(?<![\\w])(?:~|/)[\\w.-]*[/][\\w./-]+", " Dateipfad ")
        t = re(t, "\\((?:vgl\\.|siehe|vergleiche|Quelle:?|S\\.|Seite|Abb\\.|Kap\\.)[^)]*\\)|\\[[^\\]]{0,80}\\]", " ", .caseInsensitive)
        t = re(t, "(?m)^\\s{0,6}(?:[-*•·+]|\\d{1,2}[.)](?!\\s*(?:Januar|Februar|März|April|Mai|Juni|Juli|August|September|Oktober|November|Dezember)\\b))\\s+", "")
        t = re(t, "(?m)^\\|?[\\s:|-]+\\|[\\s:|-]*$", "")          // Tabellen-Trennzeilen
        t = re(t, "[*_#]|~~|(?m:^[ \\t]*>+)", "")
        t = re(t, "[(){}\\[\\]]", ", ")
        for (kurz, lang) in abkuerzungen {
            t = re(t, "(?<!\\p{L})" + NSRegularExpression.escapedPattern(for: kurz), NSRegularExpression.escapedTemplate(for: lang))
        }
        t = zahlen(t)
        for (z, w) in ersatz { t = t.replacingOccurrences(of: z, with: w) }
        // Emojis und sonstige Symbole entfernen
        t = String(t.unicodeScalars.filter { s in
            if s.properties.isEmojiPresentation || (s.properties.isEmoji && s.value > 0x238C) { return false }
            if s.value == 0xFE0F || s.value == 0x200D { return false }
            return true
        }.map(Character.init))
        t = re(t, "[^\\p{L}\\p{N} .,!?;:\\-'\\n]", " ")
        t = re(t, "[ \\t]+", " ")
        t = re(t, "([!?.])\\1+", "$1")
        t = re(t, " ?([.,!?;:]) ?", "$1 ")
        t = re(t, "([.,!?;:])(?:\\s*\\1)+", "$1")
        t = re(t, ",\\s*\\.", ".")
        t = re(t, "\\n{3,}", "\n\n")
        return t.split(separator: "\n", omittingEmptySubsequences: false)
            .map { $0.trimmingCharacters(in: .whitespaces) }.joined(separator: "\n")
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private static func zahlen(_ roh: String) -> String {
        var t = roh
        t = ersetze(t, "\\b(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4})?") { g in
            guard let tag = Int(g[1]), let monat = Int(g[2]), (1...31).contains(tag), (1...12).contains(monat) else { return nil }
            return ordnung[tag - 1] + " " + monate[monat - 1] + (g[3].isEmpty ? "" : " " + g[3])
        }
        t = ersetze(t, "\\b(\\d{1,2}):(\\d{2})\\b") { g in
            guard let h = Int(g[1]), let m = Int(g[2]), (0...23).contains(h), (0...59).contains(m) else { return nil }
            return m == 0 ? "\(h) Uhr" : "\(h) Uhr \(m)"
        }
        t = ersetze(t, "\\b(\\d+),(\\d+)\\s*([a-zA-Z°]{1,4})?") { g in
            let zahl = "\(g[1]) Komma \(g[2])"
            if let e = einheiten[g[3]] { return zahl + " " + e }
            return zahl + (g[3].isEmpty ? "" : " " + g[3])
        }
        t = ersetze(t, "\\b(\\d+)\\s*([a-zA-Z°]{1,4})\\b") { g in
            guard let e = einheiten[g[2]] else { return nil }
            return "\(g[1]) \(e)"
        }
        return t
    }

    /// Zerlegt den Text in Stücke für die Synthese: erstes Stück kurz (schneller Start), danach bis ~350 Zeichen.
    static func stuecke(_ text: String) -> [String] {
        var saetze: [String] = []
        var aktuell = ""
        let zeichen = Array(text)
        for (i, c) in zeichen.enumerated() {
            aktuell.append(c)
            let ende = c == "\n" || ((c == "." || c == "!" || c == "?") && (i + 1 >= zeichen.count || zeichen[i + 1] == " " || zeichen[i + 1] == "\n"))
            if ende {
                let s = aktuell.trimmingCharacters(in: .whitespacesAndNewlines)
                if !s.isEmpty { saetze.append(s) }
                aktuell = ""
            }
        }
        let rest = aktuell.trimmingCharacters(in: .whitespacesAndNewlines)
        if !rest.isEmpty { saetze.append(rest) }

        var stuecke: [String] = []
        var puffer = ""
        for s in saetze {
            let grenze = stuecke.isEmpty ? 140 : 350
            if !puffer.isEmpty && puffer.count + s.count + 1 > grenze {
                stuecke.append(puffer); puffer = ""
            }
            puffer += (puffer.isEmpty ? "" : " ") + s
            if stuecke.isEmpty && puffer.count >= 60 { stuecke.append(puffer); puffer = "" }
        }
        if !puffer.isEmpty { stuecke.append(puffer) }
        // Überlange Einzelsätze (ohne Satzzeichen) hart an Wortgrenzen teilen.
        return stuecke.flatMap { s -> [String] in
            guard s.count > 800 else { return [s] }
            var teile: [String] = []; var p = ""
            for w in s.split(separator: " ") {
                if p.count + w.count > 600 { teile.append(p); p = "" }
                p += (p.isEmpty ? "" : " ") + w
            }
            if !p.isEmpty { teile.append(p) }
            return teile
        }
    }
}

/// Zerlegt einen wachsenden Antworttext in vorlesefertige Stücke: Das erste kommt nach dem ersten Satz, die weiteren sammeln kurze Sätze,
/// damit nicht jedes „Ja.“ eine eigene Anfrage an die Stimme wird. Ein Punkt gilt erst als Satzende, wenn das nächste Zeichen da ist.
final class SatzStrom {
    static let ersteGrenze = 140
    private static let grenze = 350, minErstes = 25, minWeitere = 80
    private static let kuerzel: Set<String> = [
        "bzw", "ca", "ggf", "evtl", "inkl", "exkl", "usw", "etc", "vgl", "nr", "abb", "kap", "mio", "mrd", "std", "sek", "tsd", "max", "min",
        "dr", "prof", "hr", "fr", "st", "str", "tel", "bspw", "sog", "ggü",
    ]

    private var puffer: [Character] = []
    private var gesammelt = ""
    private var erstes = true

    func hinzu(_ text: String) -> [String] {
        puffer.append(contentsOf: text)
        return nimm(ende: false)
    }

    func ende() -> [String] { nimm(ende: true) }

    /// Stelle zum Trennen bis `grenze`: am liebsten hinter einem Komma, sonst an einer Wortgrenze.
    static func schnittstelle(_ t: [Character], _ grenze: Int) -> Int {
        let bis = min(grenze, t.count - 1)
        guard bis > 0 else { return bis }
        var komma = -1
        for i in stride(from: min(bis, t.count - 2), through: 0, by: -1) where t[i] == "," && t[i + 1] == " " { komma = i; break }
        if komma >= grenze / 3 { return komma + 1 }
        for i in stride(from: bis, to: 0, by: -1) where t[i] == " " { return i }
        return bis
    }

    private static func istKuerzel(_ t: [Character], _ start: Int, _ punkt: Int) -> Bool {
        var a = punkt
        while a > start && !t[a - 1].isWhitespace { a -= 1 }
        let wort = String(t[a..<punkt]).drop { "([„\"*_".contains($0) }
        if wort.isEmpty { return false }
        if wort.count == 1 { return true }                                   // „z.“, „B.“
        if wort.allSatisfy(\.isNumber) { return wort.count <= 2 }            // „3. Platz“, Listen; eine Jahreszahl beendet den Satz
        return wort.contains(".") || kuerzel.contains(wort.lowercased())
    }

    private func nimm(ende: Bool) -> [String] {
        var aus: [String] = []
        let t = puffer
        var start = 0
        var imCode = false
        var i = 0
        while i < t.count {
            if i + 2 < t.count && t[i] == "`" && t[i + 1] == "`" && t[i + 2] == "`" { imCode.toggle(); i += 3; continue }
            if imCode { i += 1; continue }
            let c = t[i]
            let folgt = i + 1 < t.count
            let satzende = c == "\n" || ((c == "." || c == "!" || c == "?") && folgt && (t[i + 1] == " " || t[i + 1] == "\n") && (c != "." || !Self.istKuerzel(t, start, i)))
            if satzende {
                satz(String(t[start...i]), &aus)
                start = i + 1
            }
            i += 1
        }
        // Ein langer Satz ohne Punkt soll nicht bis zu seinem Ende warten.
        let obergrenze = erstes ? Self.ersteGrenze : Self.grenze
        if !ende && !imCode && t.count - start > obergrenze {
            let rest = Array(t[start...])
            let schnitt = Self.schnittstelle(rest, obergrenze)
            if schnitt > 0 { satz(String(rest[..<schnitt]), &aus, sofort: true); start += schnitt }
        }
        puffer.removeFirst(min(start, puffer.count))
        if ende {
            satz(String(puffer), &aus, sofort: true)
            puffer = []
            if !gesammelt.isEmpty { abgeben(&aus) }
        }
        return aus
    }

    private func satz(_ s: String, _ aus: inout [String], sofort: Bool = false) {
        // Zeilenumbrüche bleiben: Markdown-Listen und -Überschriften brauchen sie beim Bereinigen.
        if !s.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { gesammelt += s }
        let laenge = gesammelt.trimmingCharacters(in: .whitespacesAndNewlines).count
        if laenge > 0 && (sofort || laenge >= (erstes ? Self.minErstes : Self.minWeitere)) { abgeben(&aus) }
    }

    private func abgeben(_ aus: inout [String]) {
        let sauber = SprechText.fuerStimme(gesammelt)
        gesammelt = ""
        guard !sauber.isEmpty else { return }
        aus.append(sauber)
        erstes = false
    }
}

/// Eine laufende Sprachausgabe, die mit der Modellantwort wächst. `text` nimmt die gestreamten Stücke, `ende` den fertigen Antworttext
/// und liest nur, was im Strom noch nicht kam (etwa eine Fehlermeldung), damit nichts doppelt klingt.
@MainActor
final class VorleseStrom {
    private let saetze = SatzStrom()
    private var gefuettert = ""
    private var beendet = false
    let stuecke: AsyncStream<String>
    private let zufluss: AsyncStream<String>.Continuation
    var lauf: Task<Void, Never>?
    private let stopp: () -> Void

    init(stopp: @escaping () -> Void) {
        (stuecke, zufluss) = AsyncStream.makeStream(of: String.self)
        self.stopp = stopp
    }

    func text(_ stueck: String) {
        guard !beendet else { return }
        gefuettert += stueck
        for s in saetze.hinzu(stueck) { zufluss.yield(s) }
    }

    /// Das Modell ist fertig. Kehrt zurück, wenn alles gesprochen oder gestoppt ist.
    func ende(_ endtext: String) async {
        if !beendet {
            beendet = true
            let rest = Self.nachzutragen(gefuettert, endtext)
            if !rest.isEmpty { for s in saetze.hinzu(rest) { zufluss.yield(s) } }
            for s in saetze.ende() { zufluss.yield(s) }
            zufluss.finish()
        }
        await lauf?.value
    }

    func abbrechen() {
        beendet = true
        zufluss.finish()
        stopp()
    }

    /// Was vom Endtext hinter dem schon Gestreamten liegt. Leerraum zählt nicht: Die Runden werden im Endtext durch Absätze getrennt.
    /// Passt der Anfang nicht, wird nichts nachgetragen: Lieber ein fehlender Satz als eine doppelte Antwort.
    nonisolated static func nachzutragen(_ gestreamt: String, _ endtext: String) -> String {
        let g = Array(gestreamt), e = Array(endtext)
        var i = 0, j = 0
        while true {
            while i < g.count && g[i].isWhitespace { i += 1 }
            if i >= g.count { break }
            while j < e.count && e[j].isWhitespace { j += 1 }
            if j >= e.count || e[j] != g[i] { return "" }
            i += 1; j += 1
        }
        return String(e[j...])
    }
}
