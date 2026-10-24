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
        Programm:      \(bericht.name)  (\(bericht.programmId))
        Ergebnis:      \(bericht.ergebnisText)
        Begründung:    \(bericht.meldung)
        Stand vorher:  \(bericht.versionVorher)
        Stand nachher: \(bericht.versionNachher)
        Exit-Code:     \(bericht.exitCode)
        Dauer:         \(dauerZeile(bericht))
        Sammellauf:    \(bericht.sammelId.map { "\($0), Phase \(bericht.sammelPhase ?? 0), Position \(bericht.sammelPosition ?? 0)" } ?? "–")
        \(linie)

        """
        anhaengen(tagesDatei(bericht.zeit), fuss)

        // Ein kaputter Verlaufseintrag darf das Update selbst nie kaputt machen.
        if let daten = try? kodierer.encode(bericht), let zeile = String(data: daten, encoding: .utf8) {
            anhaengen(verlaufsDatei, zeile + "\n")
        }
    }

    private static func dauerZeile(_ bericht: UpdateBericht) -> String {
        guard let s = bericht.dauerSekunden else { return "(nicht gemessen)" }
        var text = dauerText(s) + " (" + String(format: "%.1f", locale: Locale(identifier: "en_US_POSIX"), s) + " s)"
        if let a = bericht.arbeitSekunden {
            text += ", davon Update-Aufrufe " + dauerText(a) + " (\(bericht.updateAufrufe ?? 0) Aufruf(e))"
        }
        return text
    }

    /// "0:42 min" / "1:05:10 h" -- dieselbe Schreibweise auf der Karte und im Protokoll.
    static func dauerText(_ sekunden: Double) -> String {
        let gesamt = Int(max(0, sekunden).rounded(.down))
        let h = gesamt / 3600, m = (gesamt % 3600) / 60, s = gesamt % 60
        return h >= 1 ? String(format: "%d:%02d:%02d h", h, m, s) : String(format: "%d:%02d min", m, s)
    }

    /// Der Messwert eines Berichts: die Anbieterzeit, wenn gemessen, sonst der ganze Klick.
    static func messwert(_ bericht: UpdateBericht) -> Double? {
        if let a = bericht.arbeitSekunden, a > 0 { return a }
        if let d = bericht.dauerSekunden, d > 0 { return d }
        return nil
    }

    static var sammelDatei: String {
        (ordner as NSString).appendingPathComponent("sammellaeufe.jsonl")
    }

    /// Eine Zeile je Ereignis eines Sammellaufs: geplante Reihenfolge mit Phasen und erwarteten
    /// Dauern, dann je Programm Beginn, Ende, Dauer und Urteil -- fuer die spaetere Auswertung.
    static func sammellaufSchreiben(_ eintrag: [String: Any]) {
        var sauber = eintrag
        sauber["zeit"] = ISO8601DateFormatter().string(from: Date())
        guard JSONSerialization.isValidJSONObject(sauber),
              let daten = try? JSONSerialization.data(withJSONObject: sauber, options: [.sortedKeys]),
              let zeile = String(data: daten, encoding: .utf8) else { return }
        anhaengen(sammelDatei, zeile + "\n")
    }

    /// Der jeweils letzte Bericht je Programm-Id -- daraus zeigt jede Karte ihren "Zuletzt"-Stand.
    static func letzteBerichte() -> [String: UpdateBericht] { verlaufLesen().berichte }

    /// Ein Durchgang durch verlauf.jsonl fuer den Start: der letzte Bericht je Programm UND die
    /// gemessenen Update-Dauern (aelteste zuerst). Nur Laeufe, die wirklich etwas installiert
    /// haben (Erfolg oder abgelegt), zaehlen als Dauer.
    static func verlaufLesen() -> (berichte: [String: UpdateBericht], dauern: [String: [Double]]) {
        var ergebnis: [String: UpdateBericht] = [:]
        var dauern: [String: [Double]] = [:]
        guard let inhalt = try? String(contentsOfFile: verlaufsDatei, encoding: .utf8) else { return (ergebnis, dauern) }

        for zeile in inhalt.split(separator: "\n") {
            let text = String(zeile).trimmed
            guard !text.isEmpty, let daten = text.data(using: .utf8) else { continue }
            // Eine einzelne kaputte Zeile ueberspringen, statt den ganzen Verlauf zu verlieren.
            guard let bericht = try? dekodierer.decode(UpdateBericht.self, from: daten),
                  !bericht.programmId.istLeer else { continue }
            // Die Datei wird nur angehaengt und ist chronologisch -- der letzte Eintrag gewinnt.
            ergebnis[bericht.programmId] = bericht
            if bericht.ergebnis == .erfolgreich || bericht.ergebnis == .ausstehend, let s = messwert(bericht) {
                dauern[bericht.programmId, default: []].append(s)
            }
        }
        return (ergebnis, dauern)
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
