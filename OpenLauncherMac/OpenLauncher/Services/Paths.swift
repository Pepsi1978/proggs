import Foundation

/// Zentrale Pfad-Abbildung Windows -> macOS. Alle Dienste holen ihre Ablageorte hier, damit die
/// Uebersetzung an EINER Stelle steht und nicht ueber die Services verstreut ist.
///
/// | Windows                              | macOS                                            |
/// |--------------------------------------|--------------------------------------------------|
/// | %APPDATA%\OpenLauncher               | ~/Library/Application Support/OpenLauncher       |
/// | %LOCALAPPDATA%\OpenLauncher\sessions | ~/Library/Application Support/OpenLauncher/sessions |
/// | %USERPROFILE%                        | $HOME                                            |
///
/// Bewusst NICHT umgebogen wird `~/proggs/OpenLauncher/` (models.json, model-defaults.json,
/// Profiles/): diese Dateien liegen im Repo und sind auf beiden Plattformen dieselben.
enum Paths {
    static var home: String {
        NSHomeDirectory()
    }

    /// Anwendungsdaten (Logs, Layout, Tab-Farb-Zustand, TPS-Cache).
    static var appSupport: String {
        (home as NSString).appendingPathComponent("Library/Application Support/OpenLauncher")
    }

    /// Sitzungs-Configs fuer OpenCode (kurzlebig, werden nach 14 Tagen aufgeraeumt).
    static var sessionsRoot: String {
        (appSupport as NSString).appendingPathComponent("sessions")
    }

    /// Repo-Wurzel des Launchers - Modell-Liste, Modell-Standards und Profile.
    /// Identisch mit dem Windows-Pfad, damit beide Plattformen dieselbe versionierte Liste lesen.
    static var repoRoot: String {
        (home as NSString).appendingPathComponent("proggs/OpenLauncher")
    }

    /// Repo-Wurzel der macOS-Profile. Die Windows-Profile enthalten PowerShell-Hooks und
    /// C:\-Pfade; macOS bekommt deshalb eigene, gleich aufgebaute Profil-Ordner.
    static var macProfilesRoot: String {
        (repoRoot as NSString).appendingPathComponent("Profiles")
    }

    static var openCodeConfigDir: String {
        (home as NSString).appendingPathComponent(".config/opencode")
    }

    static var claudeHome: String {
        (home as NSString).appendingPathComponent(".claude")
    }

    static var tempDir: String {
        NSTemporaryDirectory()
    }

    static func ensureDirectory(_ path: String) {
        try? FileManager.default.createDirectory(atPath: path, withIntermediateDirectories: true)
    }

    /// Schreibt atomar (Temp-Datei + Move), damit ein Absturz mitten im Schreiben keine
    /// abgeschnittene Datei hinterlaesst - dieselbe Absicherung wie im Windows-Launcher.
    @discardableResult
    static func writeAtomic(_ text: String, to path: String) -> Bool {
        ensureDirectory((path as NSString).deletingLastPathComponent)
        let tmp = path + ".tmp"
        do {
            try text.write(toFile: tmp, atomically: false, encoding: .utf8)
            if FileManager.default.fileExists(atPath: path) {
                _ = try? FileManager.default.replaceItemAt(URL(fileURLWithPath: path),
                                                          withItemAt: URL(fileURLWithPath: tmp))
                if FileManager.default.fileExists(atPath: tmp) {
                    try? FileManager.default.removeItem(atPath: path)
                    try FileManager.default.moveItem(atPath: tmp, toPath: path)
                }
            } else {
                try FileManager.default.moveItem(atPath: tmp, toPath: path)
            }
            return true
        } catch {
            try? FileManager.default.removeItem(atPath: tmp)
            return false
        }
    }

    static func readText(_ path: String) -> String {
        (try? String(contentsOfFile: path, encoding: .utf8)) ?? ""
    }

    static func fileExists(_ path: String) -> Bool {
        FileManager.default.fileExists(atPath: path)
    }

    static func directoryExists(_ path: String) -> Bool {
        var isDir: ObjCBool = false
        let exists = FileManager.default.fileExists(atPath: path, isDirectory: &isDir)
        return exists && isDir.boolValue
    }
}
