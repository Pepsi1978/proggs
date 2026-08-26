import Foundation

/// Eine Terminal-Tab-Farbe. Windows uebergibt sie an `wt --tabColor`.
struct TerminalTabColor {
    let name: String
    let hex: String

    /// Faerbt den Tab ein - aber NUR, wenn das Terminal die Sequenz auch versteht.
    ///
    /// Terminal.app (das Standard-Terminal von macOS) kennt keine per-Tab-Farbe: Farben kommen dort
    /// aus dem gewaehlten Profil und gelten fuer alle Fenster. Die Sequenz wird deshalb zur Laufzeit
    /// an `$TERM_PROGRAM` geknuepft - in Terminal.app passiert schlicht nichts, statt dass
    /// unverstandene Steuerzeichen im Fenster landen. Der Farbname bleibt trotzdem sichtbar: er
    /// steht im Tab-Titel, und bei Claude Code faerbt `/color <name>` zusaetzlich die Oberflaeche.
    var tabColorScript: String {
        let (r, g, b) = rgb
        return """
        if [ "$TERM_PROGRAM" = "iTerm.app" ]; then
            printf '\\033]6;1;bg;red;brightness;\(r)\\a'
            printf '\\033]6;1;bg;green;brightness;\(g)\\a'
            printf '\\033]6;1;bg;blue;brightness;\(b)\\a'
        fi
        """
    }

    var rgb: (Int, Int, Int) {
        var value = hex
        if value.hasPrefix("#") { value.removeFirst() }
        guard let raw = UInt32(value, radix: 16), value.count == 6 else { return (128, 128, 128) }
        return (Int((raw >> 16) & 0xFF), Int((raw >> 8) & 0xFF), Int(raw & 0xFF))
    }
}

private struct TabColorState: Codable {
    var lastColor: String?
    var remaining: [String] = []

    enum CodingKeys: String, CodingKey {
        case lastColor = "LastColor"
        case remaining = "Remaining"
    }
}

/// Oeffnet ein neues Terminal-Fenster/-Tab und laesst darin ein Startskript laufen.
///
/// Windows nutzt dafuer Windows Terminal (`wt new-tab --tabColor ... pwsh -File script.ps1`) mit
/// einem PowerShell-Retry-Wrapper. Auf macOS ist das Gegenstueck **Terminal.app**, das
/// Standard-Terminal des Systems: `open -a Terminal <skript>` startet das Skript in einem neuen
/// Fenster bzw. Tab (je nach Systemeinstellung "Tabs bevorzugen"). Der Retry-Wrapper entfaellt - er
/// umgeht einen Fehler der Windows-Terminal-Kommandozeile, den es hier nicht gibt.
enum TerminalLauncher {
    /// Startet das Skript in Terminal.app. Gibt den verwendeten Terminal-Namen zurueck.
    @discardableResult
    static func openScript(_ scriptPath: String, workDir: String) -> String {
        // Ohne Ausfuehrungsrecht wuerde Terminal.app das Skript nur im Editor zeigen statt es zu starten.
        try? FileManager.default.setAttributes([.posixPermissions: 0o755], ofItemAtPath: scriptPath)

        let result = Shell.run("/usr/bin/open", ["-a", "Terminal", scriptPath],
                               workingDirectory: workDir, timeout: 20)
        if result.exitCode != 0 {
            Logger.shared.error("TerminalLauncher", "openScript",
                                "Terminal.app konnte nicht geoeffnet werden: \(result.stderr.trimmingCharacters(in: .whitespacesAndNewlines))",
                                ["script": scriptPath])
        }
        return "Terminal.app"
    }

    // ===================== Tab-Farben =====================

    /// Palette fuer OpenCode-Sitzungen (1:1 die Windows-Terminal-Farbnamen und -Hexwerte).
    static let openCodeColors: [TerminalTabColor] = [
        TerminalTabColor(name: "black", hex: "#0C0C0C"),
        TerminalTabColor(name: "red", hex: "#C50F1F"),
        TerminalTabColor(name: "green", hex: "#13A10E"),
        TerminalTabColor(name: "yellow", hex: "#C19C00"),
        TerminalTabColor(name: "blue", hex: "#0037DA"),
        TerminalTabColor(name: "purple", hex: "#881798"),
        TerminalTabColor(name: "cyan", hex: "#3A96DD"),
        TerminalTabColor(name: "white", hex: "#CCCCCC"),
        TerminalTabColor(name: "bright-black", hex: "#767676"),
        TerminalTabColor(name: "bright-red", hex: "#E74856"),
        TerminalTabColor(name: "bright-green", hex: "#16C60C"),
        TerminalTabColor(name: "bright-yellow", hex: "#F9F1A5"),
        TerminalTabColor(name: "bright-blue", hex: "#3B78FF"),
        TerminalTabColor(name: "bright-purple", hex: "#B4009E"),
        TerminalTabColor(name: "bright-cyan", hex: "#61D6D6"),
        TerminalTabColor(name: "bright-white", hex: "#F2F2F2")
    ]

    /// Palette fuer Claude-Code-Sitzungen. Die Namen sind zugleich die Argumente fuer `/color`.
    static let claudeColors: [TerminalTabColor] = [
        TerminalTabColor(name: "red", hex: "#FF3B3B"),
        TerminalTabColor(name: "orange", hex: "#FF8C42"),
        TerminalTabColor(name: "yellow", hex: "#FFD93D"),
        TerminalTabColor(name: "green", hex: "#4CAF50"),
        TerminalTabColor(name: "cyan", hex: "#00BCD4"),
        TerminalTabColor(name: "blue", hex: "#2196F3"),
        TerminalTabColor(name: "purple", hex: "#9C27B0"),
        TerminalTabColor(name: "pink", hex: "#FF1493")
    ]

    static func pickOpenCodeColor() -> TerminalTabColor {
        pickRotating(openCodeColors, statePath: (Paths.appSupport as NSString).appendingPathComponent("tab-color-state.json"))
    }

    static func pickClaudeColor() -> TerminalTabColor {
        pickRotating(claudeColors, statePath: (Paths.appSupport as NSString).appendingPathComponent("claude-tab-color-state.json"))
    }

    /// Zieht eine Farbe aus der Palette, ohne die zuletzt benutzte zu wiederholen, und arbeitet den
    /// Vorrat durch, bevor er neu aufgefuellt wird. Jede Palette hat ihre eigene Zustandsdatei.
    private static func pickRotating(_ palette: [TerminalTabColor], statePath: String) -> TerminalTabColor {
        var colorByName: [String: TerminalTabColor] = [:]
        for color in palette { colorByName[color.name.lowercased()] = color }

        let state = readTabColorState(statePath)
        var remaining = state.remaining
            .filter { colorByName[$0.lowercased()] != nil && $0.caseInsensitiveCompare(state.lastColor ?? "") != .orderedSame }
        var seen = Set<String>()
        remaining = remaining.filter { seen.insert($0.lowercased()).inserted }

        if remaining.isEmpty {
            remaining = palette.map(\.name).filter { $0.caseInsensitiveCompare(state.lastColor ?? "") != .orderedSame }
        }
        guard !remaining.isEmpty else { return palette.randomElement() ?? palette[0] }

        let index = Int.random(in: 0..<remaining.count)
        let pickedName = remaining[index]
        remaining.remove(at: index)
        writeTabColorState(statePath, TabColorState(lastColor: pickedName, remaining: remaining))
        return colorByName[pickedName.lowercased()] ?? palette[0]
    }

    private static func readTabColorState(_ path: String) -> TabColorState {
        guard Paths.fileExists(path), let data = FileManager.default.contents(atPath: path),
              let state = try? JSONDecoder().decode(TabColorState.self, from: data) else { return TabColorState() }
        return state
    }

    private static func writeTabColorState(_ path: String, _ state: TabColorState) {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        guard let data = try? encoder.encode(state), let json = String(data: data, encoding: .utf8) else { return }
        Paths.writeAtomic(json, to: path)
    }
}
