import AppKit

/// Weiss, welche Prozesse eines Programms gerade leben. 1:1-Port von Services/Prozessdienst.cs.
///
/// Unter Windows ging das ueber Process.GetProcessesByName plus Pfadvergleich. Auf dem Mac gibt es
/// zwei Sorten Prozesse, und sie brauchen zwei Wege:
///   * GUI-Programme (.app) findet NSWorkspace ueber ihre Bundle-Kennung bzw. ihren Bundle-Pfad.
///   * Kommandozeilen-Werkzeuge (lms, claude, codex) kennt NSWorkspace gar nicht -- die findet nur
///     `pgrep`, und dort ist der Pfadvergleich genauso noetig wie unter Windows.
enum Prozessdienst {

    /// Ein gefundener Prozess -- entweder ein GUI-Programm oder ein Kommandozeilen-Werkzeug.
    struct Treffer {
        let pid: pid_t
        let app: NSRunningApplication?
    }

    @MainActor
    static func laufende(_ eintrag: ProgrammEintrag) -> [Treffer] {
        var treffer: [Treffer] = []
        treffer.append(contentsOf: laufendeApps(eintrag))
        treffer.append(contentsOf: laufendeWerkzeuge(eintrag))
        return treffer
    }

    @MainActor
    static func laeuft(_ eintrag: ProgrammEintrag) -> Bool { !laufende(eintrag).isEmpty }

    // MARK: - GUI-Programme

    @MainActor
    private static func laufendeApps(_ eintrag: ProgrammEintrag) -> [Treffer] {
        let appPfad = eintrag.appPfad.map { Pfade.aufloesen($0) } ?? ""
        guard !appPfad.isEmpty || !(eintrag.bundleId?.istLeer ?? true) else { return [] }

        return NSWorkspace.shared.runningApplications.compactMap { app in
            // Bundle-Kennung ist das genaueste Merkmal -- sie ueberlebt auch ein Update, das den
            // Ordner austauscht.
            if let kennung = eintrag.bundleId, !kennung.istLeer,
               app.bundleIdentifier?.caseInsensitiveCompare(kennung) == .orderedSame {
                return Treffer(pid: app.processIdentifier, app: app)
            }
            // Sonst ueber den Bundle-Pfad. Genau wie unter Windows gilt: ein unlesbarer Pfad ist
            // NIE ein Treffer -- lieber kein Merkmal als ein falsches.
            guard !appPfad.isEmpty, let bundle = app.bundleURL?.path else { return nil }
            let normalisiert = (bundle as NSString).standardizingPath
            return normalisiert.caseInsensitiveCompare(appPfad) == .orderedSame
                ? Treffer(pid: app.processIdentifier, app: app)
                : nil
        }
    }

    // MARK: - Kommandozeilen-Werkzeuge

    /// Prozessnamen kollidieren zwischen Eintraegen -- "claude" ist sowohl die CLI als auch (unter
    /// anderem Pfad) Teil der Desktop-App. Ohne Pfadpruefung wuerde ein Desktop-Update anbieten,
    /// laufende CLI-Sitzungen zu beenden. Ein bekannter Installationsordner schlaegt deshalb
    /// immer den nackten Namen.
    @MainActor
    private static func laufendeWerkzeuge(_ eintrag: ProgrammEintrag) -> [Treffer] {
        guard !eintrag.alleProzesse.isEmpty else { return [] }

        let exe = Pfade.aufloesen(eintrag.exePfad)
        let erwarteterOrdner = exe.isEmpty ? nil : (exe as NSString).deletingLastPathComponent

        var treffer: [Treffer] = []
        for name in eintrag.alleProzesse {
            for pid in pidsFuer(name) {
                guard pid != ProcessInfo.processInfo.processIdentifier else { continue }
                // Eine schon als GUI-App erfasste PID nicht doppelt zaehlen.
                if treffer.contains(where: { $0.pid == pid }) { continue }

                if let erwarteterOrdner {
                    let pfad = pfadVon(pid)
                    if pfad.isEmpty { continue }   // Unlesbar: nie als Treffer annehmen.
                    guard pfad.hasPrefix(erwarteterOrdner) else { continue }
                }
                treffer.append(Treffer(pid: pid, app: nil))
            }
        }
        return treffer
    }

    /// `pgrep -x` findet exakt benannte Prozesse -- das Gegenstueck zu GetProcessesByName.
    private static func pidsFuer(_ name: String) -> [pid_t] {
        let rohr = Pipe()
        let prozess = Process()
        prozess.executableURL = URL(fileURLWithPath: "/usr/bin/pgrep")
        prozess.arguments = ["-x", name]
        prozess.standardOutput = rohr
        prozess.standardError = FileHandle.nullDevice
        guard (try? prozess.run()) != nil else { return [] }
        let daten = rohr.fileHandleForReading.readDataToEndOfFile()
        prozess.waitUntilExit()
        let text = String(data: daten, encoding: .utf8) ?? ""
        return text.split(separator: "\n").compactMap { pid_t(String($0).trimmed) }
    }

    /// Programmpfad einer PID. Gegenstueck zu Process.MainModule.FileName.
    private static func pfadVon(_ pid: pid_t) -> String {
        var puffer = [CChar](repeating: 0, count: Int(4 * MAXPATHLEN))
        let laenge = proc_pidpath(pid, &puffer, UInt32(puffer.count))
        guard laenge > 0 else { return "" }   // Kein Leserecht: gilt als "unbekannter Pfad".
        return String(cString: puffer)
    }

    // MARK: - Beenden und Starten

    /// Schliesst freundlich, erzwingt nur, was sich innerhalb der Gnadenfrist nicht beenden laesst.
    @MainActor
    @discardableResult
    static func beenden(_ eintrag: ProgrammEintrag) async -> Int {
        let prozesse = laufende(eintrag)

        for treffer in prozesse {
            if let app = treffer.app {
                app.terminate()
            } else {
                kill(treffer.pid, SIGTERM)
            }
        }

        try? await Task.sleep(nanoseconds: 2_500_000_000)

        for treffer in laufende(eintrag) {
            if let app = treffer.app {
                app.forceTerminate()
            } else {
                kill(treffer.pid, SIGKILL)
            }
        }

        try? await Task.sleep(nanoseconds: 800_000_000)
        return prozesse.count
    }

    /// Startet das Programm. Ein App-Buendel geht ueber NSWorkspace (so wie ein Doppelklick im
    /// Finder), ein Kommandozeilen-Werkzeug direkt.
    @MainActor
    @discardableResult
    static func starten(_ eintrag: ProgrammEintrag) -> Bool {
        if eintrag.istAppBuendel {
            let pfad = Pfade.aufloesen(eintrag.appPfad)
            guard FileManager.default.fileExists(atPath: pfad) else { return false }

            let einstellung = NSWorkspace.OpenConfiguration()
            einstellung.activates = true
            if let argumente = eintrag.startArgumente, !argumente.istLeer {
                einstellung.arguments = Kommandozeile.zerlegen(argumente)
            }
            NSWorkspace.shared.openApplication(at: URL(fileURLWithPath: pfad),
                                               configuration: einstellung,
                                               completionHandler: nil)
            return true
        }

        let exe = Pfade.aufloesen(eintrag.exePfad)
        guard FileManager.default.isExecutableFile(atPath: exe) else { return false }
        let argumente = eintrag.startArgumente.map(Kommandozeile.zerlegen) ?? []
        return Kommandozeile.starten(exe, argumente,
                                     arbeitsverzeichnis: (exe as NSString).deletingLastPathComponent)
    }
}
