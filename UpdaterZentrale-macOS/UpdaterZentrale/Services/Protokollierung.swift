import AppKit

/// Dauerhafte Protokollierung. 1:1-Port von Services/Protokollierung.cs.
///
/// Alles, was ein Update-Lauf ausgibt, geht in eine Tagesdatei; das Ergebnis jedes Laufs
/// zusaetzlich nach verlauf.jsonl -- damit ein Fehlschlag auch noch nachvollziehbar ist, wenn das
/// Fenster laengst geschlossen wurde.
enum Protokollierung {
    private static let schloss = NSLock()

    static let ordner: String = (Pfade.benutzerOrdner as NSString).appendingPathComponent("logs")

    static var verlaufsDatei: String {
        (ordner as NSString).appendingPathComponent("verlauf.jsonl")
    }

    static func tagesDatei(_ zeit: Date = Date()) -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        return (ordner as NSString).appendingPathComponent("updates-\(f.string(from: zeit)).log")
    }

    private static var kodierer: JSONEncoder {
        let k = JSONEncoder()
        k.outputFormatting = [.sortedKeys]
        k.dateEncodingStrategy = .iso8601
        return k
    }

    private static var dekodierer: JSONDecoder {
        let d = JSONDecoder()
        d.dateDecodingStrategy = .iso8601
        return d
    }

    static func schreiben(_ programmId: String, _ text: String) {
        guard !text.istLeer else { return }

        let stempel = Formate.uhrzeitMitSekunden(Date())
        var puffer = ""
        for zeile in text.replacingOccurrences(of: "\r\n", with: "\n").split(separator: "\n",
                                                                             omittingEmptySubsequences: false) {
            puffer += "\(stempel)  [\(programmId)]  \(String(zeile).trimmingCharacters(in: .whitespaces))\n"
        }
        anhaengen(tagesDatei(), puffer)
    }

    /// Markiert den Beginn eines Laufs, damit ein Protokoll nie einem anderen Versuch zugeordnet wird.
    static func laufBeginnen(_ eintrag: ProgrammEintrag, befehl: String, fingerabdruckVorher: String) {
        let linie = String(repeating: "=", count: 78)
        let strich = String(repeating: "-", count: 78)
        let kopf = """
        \(linie)
        Update-Lauf: \(eintrag.name)  (\(eintrag.id), Art \(eintrag.art))
        Gestartet:   \(Formate.langerZeitpunkt(Date()))
        Rechte:      Standardbenutzer (macOS kennt keine Rechte-Erhöhung für Programme)
        Befehl:      \(befehl)
        Stand vorher: \(fingerabdruckVorher.istLeer ? "(unbekannt)" : fingerabdruckVorher)
        \(strich)

        """
        anhaengen(tagesDatei(), kopf)
    }

    static func laufBeenden(_ bericht: inout UpdateBericht) {
        bericht.protokollDatei = tagesDatei(bericht.zeit)

        let linie = String(repeating: "=", count: 78)
        let strich = String(repeating: "-", count: 78)
        let fuss = """
        \(strich)
        Ergebnis:      \(bericht.ergebnisText)
        Begründung:    \(bericht.meldung)
        Stand vorher:  \(bericht.versionVorher)
        Stand nachher: \(bericht.versionNachher)
        Exit-Code:     \(bericht.exitCode)
        \(linie)

        """
        anhaengen(tagesDatei(bericht.zeit), fuss)

        // Ein kaputter Verlaufseintrag darf das Update selbst nie kaputt machen.
        if let daten = try? kodierer.encode(bericht), let zeile = String(data: daten, encoding: .utf8) {
            anhaengen(verlaufsDatei, zeile + "\n")
        }
    }

    /// Der jeweils letzte Bericht je Programm-Id -- daraus zeigt jede Karte ihren "Zuletzt"-Stand.
    static func letzteBerichte() -> [String: UpdateBericht] {
        var ergebnis: [String: UpdateBericht] = [:]
        guard let inhalt = try? String(contentsOfFile: verlaufsDatei, encoding: .utf8) else { return ergebnis }

        for zeile in inhalt.split(separator: "\n") {
            let text = String(zeile).trimmed
            guard !text.isEmpty, let daten = text.data(using: .utf8) else { continue }
            // Eine einzelne kaputte Zeile ueberspringen, statt den ganzen Verlauf zu verlieren.
            guard let bericht = try? dekodierer.decode(UpdateBericht.self, from: daten),
                  !bericht.programmId.istLeer else { continue }
            // Die Datei wird nur angehaengt und ist chronologisch -- der letzte Eintrag gewinnt.
            ergebnis[bericht.programmId] = bericht
        }
        return ergebnis
    }

    @MainActor
    static func ordnerOeffnen() {
        Pfade.ordnerAnlegen(ordner)
        NSWorkspace.shared.open(URL(fileURLWithPath: ordner))
    }

    @MainActor
    static func dateiOeffnen(_ datei: String?) {
        let ziel = (datei?.istLeer ?? true) || !FileManager.default.fileExists(atPath: datei!)
            ? tagesDatei()
            : datei!
        guard FileManager.default.fileExists(atPath: ziel) else { ordnerOeffnen(); return }
        NSWorkspace.shared.open(URL(fileURLWithPath: ziel))
    }

    /// Haengt an, mit kurzem Wiederholen: ein Editor, der die Datei offen haelt, darf keine
    /// Protokollzeile kosten.
    private static func anhaengen(_ datei: String, _ text: String) {
        guard !text.isEmpty, let daten = text.data(using: .utf8) else { return }

        schloss.lock()
        defer { schloss.unlock() }

        for _ in 0..<3 {
            Pfade.ordnerAnlegen(ordner)
            if !FileManager.default.fileExists(atPath: datei) {
                FileManager.default.createFile(atPath: datei, contents: nil)
            }
            if let griff = FileHandle(forWritingAtPath: datei) {
                griff.seekToEndOfFile()
                griff.write(daten)
                try? griff.close()
                return
            }
            Thread.sleep(forTimeInterval: 0.05)
        }
    }
}
