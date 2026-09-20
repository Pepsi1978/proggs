import Foundation

/// Ergebnis eines Kommandozeilen-Aufrufs. 1:1 zu `BefehlErgebnis` aus Kommandozeile.cs.
struct BefehlErgebnis: Sendable {
    let exitCode: Int
    let ausgabe: String
    let abgelaufen: Bool
}

/// Fuehrt Konsolenwerkzeuge aus und gibt deren gesamte Ausgabe zurueck.
///
/// brew, npm und die Update-Skripte malen ihren Fortschritt mit Steuerzeichen -- die werden
/// entfernt, damit das Protokoll lesbar bleibt (genau wie bei winget unter Windows).
enum Kommandozeile {

    /// Steuerzeichen und ANSI-Sequenzen. Identisches Muster wie in der Windows-Fassung.
    private static let steuerzeichen = try! NSRegularExpression(
        pattern: "\\x1B\\[[0-9;?]*[ -/]*[@-~]|[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]")

    /// Umgebung fuer jeden Aufruf.
    ///
    /// Eine aus dem Finder gestartete .app erbt einen minimalen PATH. Homebrew, npm und die
    /// eigenen Werkzeuge liegen darin NICHT -- deshalb wird der PATH hier ergaenzt, statt sich
    /// auf den geerbten zu verlassen. `HOMEBREW_NO_AUTO_UPDATE` haelt jede Pruefung kurz: ohne das
    /// laedt brew bei jedem zweiten Aufruf seinen Katalog neu und jede Karte braucht 30 Sekunden.
    static var umgebung: [String: String] {
        var umwelt = ProcessInfo.processInfo.environment
        let zusatz = [
            "/opt/homebrew/bin", "/opt/homebrew/sbin", "/usr/local/bin",
            (Pfade.heim as NSString).appendingPathComponent(".local/bin"),
            (Pfade.heim as NSString).appendingPathComponent(".lmstudio/bin"),
            "/usr/bin", "/bin", "/usr/sbin", "/sbin"
        ]
        let vorhanden = (umwelt["PATH"] ?? "").split(separator: ":").map(String.init)
        var pfade = vorhanden
        for eintrag in zusatz where !pfade.contains(eintrag) { pfade.append(eintrag) }
        umwelt["PATH"] = pfade.joined(separator: ":")
        umwelt["HOMEBREW_NO_AUTO_UPDATE"] = "1"
        umwelt["HOMEBREW_NO_ENV_HINTS"] = "1"

        // Die eigenen Update-Skripte (update-launcher.sh, rebuild-overlay.sh) bauen mit swiftc.
        // Zeigt `xcode-select -p` auf Xcode.app und ist dessen Lizenz nicht angenommen, bricht
        // JEDER dieser Builds mit "You have not agreed to the Xcode license agreements" ab -- und
        // der Launcher bliebe nach dem Beenden ungebaut zurueck. Die Command Line Tools bauen ohne
        // Lizenzabfrage; sie sind hier deshalb fest gesetzt.
        // Dauerhaft loesbar nur vom Benutzer selbst: `sudo xcodebuild -license accept`.
        if FileManager.default.fileExists(atPath: "/Library/Developer/CommandLineTools/usr/bin/swiftc") {
            umwelt["DEVELOPER_DIR"] = "/Library/Developer/CommandLineTools"
        }

        // Ohne Terminal keine Fortschrittsbalken -- die machen das Protokoll unlesbar.
        umwelt["TERM"] = "dumb"
        umwelt["NO_COLOR"] = "1"
        return umwelt
    }

    /// Fuehrt ein Programm aus und wartet auf das Ergebnis.
    /// - Parameter argumente: als EINE Zeile wie unter Windows; wird nach Shell-Regeln zerlegt.
    static func ausfuehren(_ datei: String,
                           _ argumente: String,
                           zeitlimit: TimeInterval,
                           arbeitsverzeichnis: String? = nil) async -> BefehlErgebnis {
        await ausfuehren(datei, zerlegen(argumente), zeitlimit: zeitlimit,
                         arbeitsverzeichnis: arbeitsverzeichnis)
    }

    /// Fuehrt ein Programm mit bereits zerlegten Argumenten aus -- der sichere Weg, wenn ein
    /// Argument Leerzeichen enthaelt (Pfade wie "/Applications/LM Studio.app").
    static func ausfuehren(_ datei: String,
                           _ argumente: [String],
                           zeitlimit: TimeInterval,
                           arbeitsverzeichnis: String? = nil) async -> BefehlErgebnis {
        let umwelt = umgebung
        return await withCheckedContinuation { fortsetzung in
            // Eigener Thread: Process blockiert, und der MainActor darf dabei nie stehenbleiben.
            DispatchQueue.global(qos: .userInitiated).async {
                let ergebnis = ausfuehrenBlockierend(datei, argumente, zeitlimit: zeitlimit,
                                                     arbeitsverzeichnis: arbeitsverzeichnis,
                                                     umwelt: umwelt)
                fortsetzung.resume(returning: ergebnis)
            }
        }
    }

    private static func ausfuehrenBlockierend(_ datei: String,
                                              _ argumente: [String],
                                              zeitlimit: TimeInterval,
                                              arbeitsverzeichnis: String?,
                                              umwelt: [String: String]) -> BefehlErgebnis {
        guard FileManager.default.isExecutableFile(atPath: datei) else {
            return BefehlErgebnis(exitCode: -1,
                                  ausgabe: "Start fehlgeschlagen: \((datei as NSString).lastPathComponent) wurde nicht gefunden (\(datei)).",
                                  abgelaufen: false)
        }

        let prozess = Process()
        prozess.executableURL = URL(fileURLWithPath: datei)
        prozess.arguments = argumente
        prozess.environment = umwelt
        if let arbeitsverzeichnis, !arbeitsverzeichnis.isEmpty {
            prozess.currentDirectoryURL = URL(fileURLWithPath: arbeitsverzeichnis)
        }

        let ausgabeRohr = Pipe()
        let fehlerRohr = Pipe()
        prozess.standardOutput = ausgabeRohr
        prozess.standardError = fehlerRohr
        // Kein Terminal an stdin: sonst wartet ein Werkzeug, das nachfragen will, endlos.
        prozess.standardInput = FileHandle.nullDevice

        // Waehrend des Laufs mitlesen: ein volles Pipe-Puffer-Fenster (64 KB) wuerde den
        // Kindprozess sonst blockieren und den Aufruf haengen lassen.
        var ausgabeDaten = Data()
        var fehlerDaten = Data()
        let schleuse = DispatchQueue(label: "updaterzentrale.kommandozeile.io")
        ausgabeRohr.fileHandleForReading.readabilityHandler = { griff in
            let stueck = griff.availableData
            if !stueck.isEmpty { schleuse.sync { ausgabeDaten.append(stueck) } }
        }
        fehlerRohr.fileHandleForReading.readabilityHandler = { griff in
            let stueck = griff.availableData
            if !stueck.isEmpty { schleuse.sync { fehlerDaten.append(stueck) } }
        }

        do {
            try prozess.run()
        } catch {
            ausgabeRohr.fileHandleForReading.readabilityHandler = nil
            fehlerRohr.fileHandleForReading.readabilityHandler = nil
            return BefehlErgebnis(exitCode: -1,
                                  ausgabe: "Start fehlgeschlagen: \(error.localizedDescription)",
                                  abgelaufen: false)
        }

        var abgelaufen = false
        if zeitlimit > 0 {
            let ende = Date().addingTimeInterval(zeitlimit)
            while prozess.isRunning && Date() < ende {
                Thread.sleep(forTimeInterval: 0.05)
            }
            if prozess.isRunning {
                // Erst freundlich, dann hart -- ein haengender Installer soll die App nicht binden.
                prozess.terminate()
                let gnadenfrist = Date().addingTimeInterval(5)
                while prozess.isRunning && Date() < gnadenfrist { Thread.sleep(forTimeInterval: 0.05) }
                if prozess.isRunning { kill(prozess.processIdentifier, SIGKILL) }
                abgelaufen = true
            }
        }
        prozess.waitUntilExit()

        ausgabeRohr.fileHandleForReading.readabilityHandler = nil
        fehlerRohr.fileHandleForReading.readabilityHandler = nil
        if let rest = try? ausgabeRohr.fileHandleForReading.readToEnd(), !rest.isEmpty {
            schleuse.sync { ausgabeDaten.append(rest) }
        }
        if let rest = try? fehlerRohr.fileHandleForReading.readToEnd(), !rest.isEmpty {
            schleuse.sync { fehlerDaten.append(rest) }
        }

        let ausgabe = schleuse.sync { String(data: ausgabeDaten, encoding: .utf8) ?? "" }
        let fehler = schleuse.sync { String(data: fehlerDaten, encoding: .utf8) ?? "" }

        var text = ausgabe
        if !fehler.istLeer { text += "\n" + fehler }

        return BefehlErgebnis(exitCode: Int(prozess.terminationStatus),
                              ausgabe: saeubern(text),
                              abgelaufen: abgelaufen)
    }

    /// Startet ein Programm und wartet NICHT.
    @discardableResult
    static func starten(_ datei: String, _ argumente: [String] = [],
                        arbeitsverzeichnis: String? = nil) -> Bool {
        guard FileManager.default.isExecutableFile(atPath: datei) else { return false }
        let prozess = Process()
        prozess.executableURL = URL(fileURLWithPath: datei)
        prozess.arguments = argumente
        prozess.environment = umgebung
        if let arbeitsverzeichnis, !arbeitsverzeichnis.isEmpty {
            prozess.currentDirectoryURL = URL(fileURLWithPath: arbeitsverzeichnis)
        }
        do { try prozess.run(); return true } catch { return false }
    }

    /// Zerlegt eine Argumentzeile nach Shell-Regeln (Anfuehrungszeichen halten zusammen).
    /// Unter Windows uebernimmt das der Prozessstarter; auf macOS braucht Process ein Feld.
    static func zerlegen(_ zeile: String) -> [String] {
        var teile: [String] = []
        var aktuell = ""
        var inAnfuehrung: Character? = nil
        var hatInhalt = false

        for zeichen in zeile {
            if let anfuehrung = inAnfuehrung {
                if zeichen == anfuehrung { inAnfuehrung = nil } else { aktuell.append(zeichen) }
            } else if zeichen == "\"" || zeichen == "'" {
                inAnfuehrung = zeichen
                hatInhalt = true
            } else if zeichen == " " || zeichen == "\t" {
                if !aktuell.isEmpty || hatInhalt { teile.append(aktuell); aktuell = ""; hatInhalt = false }
            } else {
                aktuell.append(zeichen)
            }
        }
        if !aktuell.isEmpty || hatInhalt { teile.append(aktuell) }
        return teile
    }

    /// Maskiert einen Wert fuer einfache Anfuehrungszeichen in sh/zsh.
    static func einfachZitiert(_ wert: String) -> String {
        "'" + wert.replacingOccurrences(of: "'", with: "'\\''") + "'"
    }

    /// Entfernt Steuerzeichen und vereinheitlicht die Zeilenenden.
    ///
    /// Werkzeuge malen ihre Fortschrittszeile mit nackten Wagenruecklaeufen neu. Die muessen zu
    /// Zeilenumbruechen werden, sonst klebt die naechste Zeile hinter Fortschrittsresten und
    /// kein Parser findet sie wieder.
    private static func saeubern(_ text: String) -> String {
        let bereich = NSRange(text.startIndex..., in: text)
        let ohneSteuerung = steuerzeichen.stringByReplacingMatches(in: text, options: [],
                                                                   range: bereich, withTemplate: "")
        return ohneSteuerung
            .replacingOccurrences(of: "\r\n", with: "\n")
            .replacingOccurrences(of: "\r", with: "\n")
            .trimmed
    }
}
