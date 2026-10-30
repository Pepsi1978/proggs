import Foundation

/// Erkennt, ob ein gesprochener Satz die Sprachsession beenden soll, wie am Handy (SprachBefehle.kt). Läuft vor dem Modell:
/// Was hier sicher erkannt wird, beendet sofort. Frei Formuliertes geht ans Modell, das dafür `sprachsession_beenden` hat.
enum SprachBefehle {
    private static let deutsch = Locale(identifier: "de_DE")

    /// „Ende“, „Stopp“, „Danke, das war's“ …: nur wenn der ganze Satz ein solcher Befehl ist.
    static func istAusbefehl(_ text: String) -> Bool { ausbefehle.contains(satz(text)) }

    /// Kleingeschrieben, ohne Satzzeichen und Füllwörter, als ein Satz.
    private static func satz(_ text: String) -> String {
        text.lowercased(with: deutsch).replacingOccurrences(of: "’", with: "'")
            .replacingOccurrences(of: "[^a-zäöüß' ]", with: " ", options: .regularExpression)
            .split(separator: " ").map(String.init).filter { !fuellworte.contains($0) }
            .joined(separator: " ").replacingOccurrences(of: "'", with: "")
    }

    /// `imGespraechsmodus` = die Sprachschleife läuft: Dann genügt auch ein bloßes „Beenden“ oder „Session beenden“.
    /// Sonst muss ausdrücklich die Sprachsession, der Sprachmodus oder das Mikrofon genannt sein.
    static func istSessionEnde(_ text: String, imGespraechsmodus: Bool) -> Bool {
        if imGespraechsmodus && istAusbefehl(text) { return true }
        let worte = normiert(text)
        if worte.isEmpty || worte.count > 8 { return false }
        // Fragen und Verneinungen beenden nie: „Wie beende ich die Sprachsession?“, „Beende die Sprachsession nicht“.
        if worte.contains(where: sperrworte.contains) { return false }
        let rest = worte.filter { !fuellworte.contains($0) && !artikel.contains($0) && !helfer.contains($0) }
        if rest.isEmpty || rest.contains(where: { !endeVerben.contains($0) && !nomenImmer.contains($0) && !nomenImModus.contains($0) }) { return false }
        if !rest.contains(where: endeVerben.contains) { return false }
        if rest.contains(where: nomenImmer.contains) { return true }
        if !imGespraechsmodus { return false }
        // In der Sprachschleife: „Session beenden“ oder ein bloßes „Bitte beenden jetzt“, aber nie ein einzelnes „zu“ oder „ab“.
        return rest.contains(where: nomenImModus.contains) || rest.contains(where: kernVerben.contains) || (rest.contains("hör") && rest.contains("auf"))
    }

    /// Kleinschreibung, ohne Satzzeichen; „Sprach-Session“, „Sprach Session“ und „Voice Session“ werden zu „sprachsession“.
    private static func normiert(_ text: String) -> [String] {
        var s = " " + text.lowercased(with: deutsch).replacingOccurrences(of: "[^a-zäöüß ]", with: " ", options: .regularExpression)
            .replacingOccurrences(of: " +", with: " ", options: .regularExpression).trimmingCharacters(in: .whitespaces) + " "
        for (alt, neu) in zusammen { s = s.replacingOccurrences(of: " \(alt) ", with: " \(neu) ") }
        return s.split(separator: " ").map(String.init)
    }

    private static let fuellworte: Set<String> = ["jarvis", "okay", "ok", "bitte", "hey", "also", "gut", "dann", "ja", "nein", "nee", "so"]

    private static let helfer: Set<String> = [
        "jetzt", "mal", "doch", "einfach", "endlich", "sofort", "gleich", "nun", "erstmal", "danke", "vielen", "dank", "gerne", "gern",
        "kannst", "könntest", "würdest", "du", "wir", "können", "kann", "ich", "will", "möchte", "möchtest", "sollst", "lass", "lasst",
        "uns", "mach", "mache", "machen", "schalt", "schalte", "hiermit", "wieder", "komplett", "ganz", "für", "heute", "hier",
    ]

    private static let artikel: Set<String> = [
        "die", "den", "das", "der", "dem", "diese", "diesen", "dieses", "unsere", "meine", "deine", "dein", "mein",
        "aktuelle", "aktive", "laufende", "aktuellen", "aktiven", "laufenden",
    ]

    private static let endeVerben: Set<String> = [
        "beenden", "beende", "beendet", "beend", "stoppen", "stoppe", "stopp", "stop", "aus", "ausschalten", "ausmachen", "abschalten",
        "abstellen", "schließen", "schließe", "schließ", "zu", "zumachen", "ende", "schluss", "aufhören", "hör", "höre", "auf",
        "abbrechen", "brich", "ab", "zuzuhören",
    ]

    /// Ohne Nomen zählen nur diese eindeutigen Verben.
    private static let kernVerben: Set<String> = ["beenden", "beende", "beendet", "stoppen", "stoppe", "stopp", "stop", "aufhören", "abbrechen", "schluss", "ende"]

    private static let nomenImmer: Set<String> = [
        "sprachsession", "sprachsitzung", "sprachmodus", "sprachschleife", "gesprächsmodus", "sprachdialog", "sprachsteuerung", "mikrofon", "mikro",
    ]

    private static let nomenImModus: Set<String> = ["session", "sitzung", "gespräch", "unterhaltung"]

    private static let sperrworte: Set<String> = [
        "nicht", "kein", "keine", "nie", "wie", "warum", "wieso", "weshalb", "was", "wann", "wo", "ob", "erklär", "erkläre",
        "programmier", "programmiersession", "programmierauftrag", "auftrag", "app",
    ]

    private static let zusammen: [(String, String)] = [
        ("sprach session", "sprachsession"), ("voice session", "sprachsession"), ("sprach sitzung", "sprachsitzung"),
        ("sprach modus", "sprachmodus"), ("gesprächs modus", "gesprächsmodus"), ("sprach schleife", "sprachschleife"),
    ]

    private static let hoeflich: Set<String> = [
        "danke", "danke schön", "dankeschön", "vielen dank", "tschüss", "tschüs", "ciao", "fertig", "das wars", "danke das wars", "das wars danke",
        "das ist alles", "danke das ist alles", "das reicht", "mehr nicht", "nichts mehr", "nichts weiter", "sonst nichts", "danke sonst nichts",
    ]

    private static let ausbefehle: Set<String> = hoeflich.union([
        "ende", "stopp", "stop", "schluss", "aus",
        "sprachsitzung beenden", "sprachsitzung beendet", "sitzung beenden", "gespräch beenden", "beenden", "beende", "aufhören", "hör auf",
        "ende der sitzung", "session beenden", "schluss für jetzt", "erstmal nicht", "erst mal nicht",
    ])
}
