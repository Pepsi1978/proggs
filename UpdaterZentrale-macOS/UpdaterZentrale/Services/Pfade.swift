import Foundation

/// Die einzige Stelle, die weiss, wo was liegt -- 1:1-Port von Services/Pfade.cs.
///
/// | Windows                                  | macOS                                                |
/// |------------------------------------------|------------------------------------------------------|
/// | %LOCALAPPDATA%\UpdateZentrale            | ~/Library/Application Support/UpdaterZentrale        |
/// | %LOCALAPPDATA%\UpdateZentrale\logs       | ~/Library/Application Support/UpdaterZentrale/logs    |
/// | %USERPROFILE%                            | $HOME                                                |
/// | %LOCALAPPDATA%\Microsoft\WindowsApps\winget.exe | /opt/homebrew/bin/brew                        |
enum Pfade {
    static var heim: String { NSHomeDirectory() }

    /// Repo-Wurzel (~/proggs). Ermittelt wie unter Windows: vom Programm aus nach oben laufen,
    /// bis der Ordner dieses Projekts mit seinem Katalog gefunden ist.
    static let repoWurzel: String = ermitteln()

    static var projektOrdner: String {
        (repoWurzel as NSString).appendingPathComponent("UpdaterZentrale-macOS")
    }

    static var katalogDatei: String {
        (projektOrdner as NSString).appendingPathComponent("programs.json")
    }

    static let benutzerOrdner: String =
        (NSHomeDirectory() as NSString).appendingPathComponent("Library/Application Support/UpdaterZentrale")

    static var einstellungsDatei: String {
        (benutzerOrdner as NSString).appendingPathComponent("settings.json")
    }

    /// Homebrew. Eine aus dem Finder gestartete .app erbt einen minimalen PATH
    /// (/usr/bin:/bin:/usr/sbin:/sbin) -- /opt/homebrew/bin ist darin NICHT enthalten.
    /// Deshalb wird der Pfad absolut ermittelt, nie ueber `brew` im PATH.
    static let brew: String = {
        for kandidat in ["/opt/homebrew/bin/brew", "/usr/local/bin/brew"] {
            if FileManager.default.isExecutableFile(atPath: kandidat) { return kandidat }
        }
        return "/opt/homebrew/bin/brew"
    }()

    /// npm -- aus demselben Grund absolut.
    static let npm: String = {
        for kandidat in ["/opt/homebrew/bin/npm", "/usr/local/bin/npm",
                         (NSHomeDirectory() as NSString).appendingPathComponent(".local/bin/npm")] {
            if FileManager.default.isExecutableFile(atPath: kandidat) { return kandidat }
        }
        return "/opt/homebrew/bin/npm"
    }()

    /// Loest `~`, `$HOME` und `%VARIABLEN%` auf und gibt einen absoluten Pfad zurueck.
    ///
    /// Die `%VAR%`-Schreibweise wird mit uebersetzt, damit ein Eintrag aus dem Windows-Katalog
    /// hier nicht stumm auf einen unsinnigen Pfad zeigt: %USERPROFILE% und %LOCALAPPDATA% sind
    /// die beiden, die im Katalog vorkommen.
    static func aufloesen(_ pfad: String?) -> String {
        guard let pfad, !pfad.istLeer else { return "" }

        var text = pfad
            .replacingOccurrences(of: "%USERPROFILE%", with: heim)
            .replacingOccurrences(of: "%HOME%", with: heim)
            .replacingOccurrences(of: "%LOCALAPPDATA%",
                                  with: (heim as NSString).appendingPathComponent("Library/Application Support"))
            .replacingOccurrences(of: "$HOME", with: heim)

        if text.hasPrefix("~") {
            text = (text as NSString).expandingTildeInPath
        }
        // Der Katalog schreibt Pfade mit Schraegstrichen -- unter macOS ohnehin die richtige Form.
        return (text as NSString).standardizingPath
    }

    /// Liest einen Wert aus der Info.plist eines App-Buendels bzw. einer Info.plist-Datei.
    /// Ersetzt FileVersionInfo.GetVersionInfo aus der Windows-Fassung.
    static func plistWert(_ pfad: String, schluessel: String) -> String {
        let datei = pfad.hasSuffix(".plist")
            ? pfad
            : (pfad as NSString).appendingPathComponent("Contents/Info.plist")
        guard FileManager.default.fileExists(atPath: datei),
              let daten = FileManager.default.contents(atPath: datei),
              let plist = try? PropertyListSerialization.propertyList(from: daten, options: [], format: nil),
              let woerterbuch = plist as? [String: Any] else { return "" }
        return (woerterbuch[schluessel] as? String) ?? ""
    }

    /// Die Anzeigeversion eines App-Buendels -- auf dem Mac die Wahrheit ueber "was ist installiert",
    /// auch wenn die App gar nicht ueber brew kam.
    static func appVersion(_ appPfad: String) -> String {
        let kurz = plistWert(appPfad, schluessel: "CFBundleShortVersionString")
        return kurz.istLeer ? plistWert(appPfad, schluessel: "CFBundleVersion") : kurz
    }

    static func existiert(_ pfad: String) -> Bool {
        !pfad.isEmpty && FileManager.default.fileExists(atPath: pfad)
    }

    static func ordnerAnlegen(_ pfad: String) {
        try? FileManager.default.createDirectory(atPath: pfad, withIntermediateDirectories: true)
    }

    /// Aenderungszeit einer Datei als "yyyy-MM-dd HH:mm:ss" -- Teil des Fingerabdrucks der
    /// eigenen Werkzeuge (die Versionsnummer allein aendert sich beim Neubau nicht).
    static func schreibZeit(_ pfad: String) -> String {
        guard let attribute = try? FileManager.default.attributesOfItem(atPath: pfad),
              let datum = attribute[.modificationDate] as? Date else { return "" }
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd HH:mm:ss"
        f.timeZone = TimeZone(identifier: "UTC")
        return f.string(from: datum)
    }

    /// Die App laeuft aus einem .app-Buendel (in /Applications oder im build/-Ordner); von dort aus
    /// nach oben laufen, bis der Projektordner mit seinem Katalog gefunden ist.
    /// Faellt auf ~/proggs zurueck -- dort liegt das Repo auf diesem Rechner.
    private static func ermitteln() -> String {
        var ordner = Bundle.main.bundlePath
        for _ in 0..<10 {
            let kandidat = (ordner as NSString).appendingPathComponent("UpdaterZentrale-macOS/programs.json")
            if FileManager.default.fileExists(atPath: kandidat) { return ordner }
            let eltern = (ordner as NSString).deletingLastPathComponent
            if eltern == ordner || eltern.isEmpty { break }
            ordner = eltern
        }
        return (NSHomeDirectory() as NSString).appendingPathComponent("proggs")
    }
}
