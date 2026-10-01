import AppKit

/// Zentrales Diagnoseprotokoll. Gegenstueck zu Services/Diagnose.cs und DiagnoseExport.cs.
///
/// Jeder Vorgang (Pruefung, Update, Sammellauf, Terminalbefehl), jede abgewiesene Doppel-Anfrage
/// und jede Ausnahme landet als eine JSON-Zeile in logs/diagnose-JJJJ-MM-TT.jsonl -- daneben die
/// Tagesprotokolle und verlauf.jsonl. "Diagnose exportieren" packt diesen Ordner maskiert in ein
/// ZIP, damit ein Fehler auch ohne Bildschirmfoto nachvollziehbar weitergegeben werden kann.
enum Schwere: String {
    case debug = "Debug"
    case info = "Info"
    case warnung = "Warnung"
    case fehler = "Fehler"
}

enum Diagnose {
    private static let schloss = NSLock()

    static func tagesDatei(_ zeit: Date = Date()) -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        return (Protokollierung.ordner as NSString).appendingPathComponent("diagnose-\(f.string(from: zeit)).jsonl")
    }

    /// Ein Ereignis. Der Text wird maskiert, bevor er auf die Platte geht.
    static func ereignis(_ schwere: Schwere, _ bereich: String, _ art: String, _ text: String,
                         _ kontext: [String: Any] = [:]) {
        var zeile: [String: Any] = [
            "Zeit": ISO8601DateFormatter().string(from: Date()),
            "Schwere": schwere.rawValue,
            "Bereich": bereich,
            "Art": art,
            "Text": maskieren(text)
        ]
        if !kontext.isEmpty {
            zeile["Kontext"] = kontext.mapValues { wert -> Any in
                if let text = wert as? String { return maskieren(text) }
                return wert
            }
        }
        guard let daten = try? JSONSerialization.data(withJSONObject: zeile, options: [.sortedKeys]),
              let text = String(data: daten, encoding: .utf8) else { return }

        schloss.lock()
        defer { schloss.unlock() }
        Pfade.ordnerAnlegen(Protokollierung.ordner)
        let datei = tagesDatei()
        if !FileManager.default.fileExists(atPath: datei) {
            FileManager.default.createFile(atPath: datei, contents: nil)
        }
        guard let griff = FileHandle(forWritingAtPath: datei) else { return }
        defer { try? griff.close() }
        griff.seekToEndOfFile()
        griff.write((text + "\n").data(using: .utf8)!)
    }

    /// Beginn und Ende eines Vorgangs mit Dauer und Urteil.
    final class Vorgang {
        private let art: String
        private let programm: String?
        private let beginn = Date()
        private var beendet = false

        fileprivate init(art: String, programm: String?) {
            self.art = art
            self.programm = programm
        }

        func beenden(_ urteil: String, _ text: String = "", _ schwere: Schwere = .info) {
            guard !beendet else { return }
            beendet = true
            var kontext: [String: Any] = ["urteil": urteil, "dauerMs": Int(Date().timeIntervalSince(beginn) * 1000)]
            if let programm { kontext["programm"] = programm }
            Diagnose.ereignis(schwere, "vorgang", art + ".ende", text.istLeer ? urteil : text, kontext)
        }

        deinit {
            // Ein Vorgang, der nie beendet wurde, ist selbst ein Befund.
            if !beendet { Diagnose.ereignis(.warnung, "vorgang", art + ".ende", "Vorgang ohne Abschluss verlassen.",
                                            programm.map { ["programm": $0] } ?? [:]) }
        }
    }

    static func vorgangBeginnen(_ art: String, _ programm: String?, _ text: String) -> Vorgang {
        ereignis(.info, "vorgang", art + ".beginn", text, programm.map { ["programm": $0] } ?? [:])
        return Vorgang(art: art, programm: programm)
    }

    // MARK: - Maskierung

    /// Haeufige Geheimnisformen: Schluessel mit Praefix, Bearer-Token, Passwort-Zuweisungen.
    private static let geheimnisse: [NSRegularExpression] = [
        "(sk|pk|rk)-[A-Za-z0-9_-]{16,}",
        "gh[pousr]_[A-Za-z0-9]{20,}",
        "github_pat_[A-Za-z0-9_]{20,}",
        "xox[abpr]-[A-Za-z0-9-]{10,}",
        "(?i)bearer\\s+[A-Za-z0-9._~+/=-]{12,}",
        "(?i)(password|passwort|token|secret|api[_-]?key)\\s*[=:]\\s*\\S+"
    ].compactMap { try? NSRegularExpression(pattern: $0) }

    static func maskieren(_ text: String) -> String {
        var ergebnis = text
        for muster in geheimnisse {
            ergebnis = muster.stringByReplacingMatches(in: ergebnis, range: NSRange(ergebnis.startIndex..., in: ergebnis),
                                                       withTemplate: "[maskiert]")
        }
        // Der Benutzername steckt in jedem Pfad; fuer die Weitergabe ist er unnoetig.
        return ergebnis.replacingOccurrences(of: Pfade.heim, with: "~")
    }

    // MARK: - Export

    /// Packt Protokollordner und Katalog maskiert als ZIP nach ~/Downloads und gibt den Pfad zurueck.
    static func exportieren(katalog: [ProgrammEintrag]) throws -> String {
        let fm = FileManager.default
        let stempel: String = {
            let f = DateFormatter()
            f.dateFormat = "yyyyMMdd-HHmmss"
            return f.string(from: Date())
        }()
        let name = "UpdaterZentrale-Diagnose-\(stempel)"
        let arbeit = (NSTemporaryDirectory() as NSString).appendingPathComponent(name)
        try? fm.removeItem(atPath: arbeit)
        try fm.createDirectory(atPath: arbeit, withIntermediateDirectories: true)
        defer { try? fm.removeItem(atPath: arbeit) }

        // Nur die letzten 14 Tage, maskiert.
        let grenze = Date().addingTimeInterval(-14 * 24 * 60 * 60)
        for datei in (try? fm.contentsOfDirectory(atPath: Protokollierung.ordner)) ?? [] {
            let quelle = (Protokollierung.ordner as NSString).appendingPathComponent(datei)
            let geaendert = (try? fm.attributesOfItem(atPath: quelle)[.modificationDate] as? Date) ?? Date()
            guard geaendert >= grenze, let text = try? String(contentsOfFile: quelle, encoding: .utf8) else { continue }
            try maskieren(text).write(toFile: (arbeit as NSString).appendingPathComponent(datei), atomically: true, encoding: .utf8)
        }

        let info = Bundle.main.infoDictionary
        var laufzeit = "Updater-Zentrale macOS \(info?["CFBundleShortVersionString"] as? String ?? "?")"
        laufzeit += " (Build \(info?["BuildTimestamp"] as? String ?? "?"))\n"
        laufzeit += "macOS \(ProcessInfo.processInfo.operatingSystemVersionString)\n"
        laufzeit += "Erstellt: \(Formate.langerZeitpunkt(Date()))\n\nKatalog:\n"
        laufzeit += katalog.map { "  \($0.id)  [\($0.art)]  \(maskieren($0.zielPfad))" }.joined(separator: "\n")
        try laufzeit.write(toFile: (arbeit as NSString).appendingPathComponent("laufzeit.txt"), atomically: true, encoding: .utf8)

        let zielOrdner = (Pfade.heim as NSString).appendingPathComponent("Downloads")
        Pfade.ordnerAnlegen(zielOrdner)
        let ziel = (zielOrdner as NSString).appendingPathComponent(name + ".zip")
        let packen = Process()
        packen.executableURL = URL(fileURLWithPath: "/usr/bin/ditto")
        packen.arguments = ["-c", "-k", "--keepParent", arbeit, ziel]
        try packen.run()
        packen.waitUntilExit()
        guard packen.terminationStatus == 0, fm.fileExists(atPath: ziel) else {
            throw NSError(domain: "Diagnose", code: Int(packen.terminationStatus),
                          userInfo: [NSLocalizedDescriptionKey: "ditto endete mit Code \(packen.terminationStatus)."])
        }
        return ziel
    }
}
