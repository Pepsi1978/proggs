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
