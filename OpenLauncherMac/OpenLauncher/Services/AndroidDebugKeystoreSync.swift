import Foundation

/// Gegenstueck zu AndroidDebugKeystoreSync.cs: legt auf jedem Rechner denselben Android-Debug-Schluessel
/// nach ~/.android/debug.keystore. Das Android-Gradle-Plugin signiert jeden Debug-Build ohne eigene
/// signingConfig damit - ein pro Rechner zufaelliger Schluessel liess das Handy Updates nach einem
/// Rechnerwechsel verweigern (INSTALL_FAILED_UPDATE_INCOMPATIBLE), nur Deinstallieren (= Datenverlust) half.
/// Quelle ist ~/SK/Android/debug-shared.keystore (SHA-256 F7:82:13:1C...). Ein abweichender alter Schluessel
/// wird nie geloescht, sondern nach ~/SK/Android/alt gesichert: nur mit ihm lassen sich damit signierte Apps
/// spaeter per apksigner-Rotation ohne Deinstallation umziehen.
enum AndroidDebugKeystoreSync {
    static func run() {
        let log = Logger.shared
        let fm = FileManager.default
        let home = fm.homeDirectoryForCurrentUser
        let skDir = home.appendingPathComponent("SK/Android")
        let skKey = skDir.appendingPathComponent("debug-shared.keystore")

        guard let shared = try? Data(contentsOf: skKey) else {
            log.warn("AndroidKeystore", "run", "gemeinsamer Debug-Key fehlt (\(skKey.path)) - nichts geaendert")
            return
        }

        let androidDir = home.appendingPathComponent(".android")
        let target = androidDir.appendingPathComponent("debug.keystore")
        do {
            if let current = try? Data(contentsOf: target) {
                if current == shared { return }
                let altDir = skDir.appendingPathComponent("alt")
                try fm.createDirectory(at: altDir, withIntermediateDirectories: true)
                let stamp = DateFormatter()
                stamp.dateFormat = "yyyyMMdd-HHmmss"
                let host = Host.current().localizedName ?? "mac"
                let backup = altDir.appendingPathComponent("debug-\(host)-\(stamp.string(from: Date())).keystore")
                try current.write(to: backup)
                log.info("AndroidKeystore", "run", "abweichenden Debug-Key gesichert: \(backup.path)")
            }
            try fm.createDirectory(at: androidDir, withIntermediateDirectories: true)
            try shared.write(to: target)
            log.info("AndroidKeystore", "run", "gemeinsamer Debug-Key nach \(target.path) gesetzt")
        } catch {
            log.error("AndroidKeystore", "run", error.localizedDescription)
        }
    }
}
