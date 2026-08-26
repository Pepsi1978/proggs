import Foundation

struct OpenCodeUpdateResult {
    let status: String
    let message: String
    let exitCode: Int32
}

/// Taegliche OpenCode-Updatepruefung beim Start des Launchers.
/// 1:1-Port von Services/OpenCodeUpdateService.cs - mit einem bewussten Plattform-Unterschied:
///
/// Windows startet `opencode-setup/build-install-windows-mousefix.ps1`. Dieses Skript baut einen
/// gepatchten opencode-Build gegen einen **Windows-spezifischen Maus-Fehler**; auf macOS gibt es
/// weder den Fehler noch den Build. Der Mac sucht deshalb ein macOS-Update-Skript
/// (`opencode-setup/update-macos.sh`) und meldet, wenn es fehlt, denselben Zustand wie Windows bei
/// fehlendem Skript: "unavailable". Dieser Zustand veraendert die Statuszeile nicht - der Launcher
/// startet also unveraendert, statt eine Fehlermeldung zu zeigen.
///
/// Bewusst wird NICHT `opencode-setup/install.sh` aufgerufen: das ist ein vollstaendiger
/// Einrichtungslauf, der ~/.config/opencode ueberschreibt - kein Update-Check.
final class OpenCodeUpdateService {
    func check() async -> OpenCodeUpdateResult {
        let script = (Paths.home as NSString).appendingPathComponent("proggs/opencode-setup/update-macos.sh")
        guard Paths.fileExists(script) else {
            return OpenCodeUpdateResult(status: "unavailable", message: "Update-Skript nicht gefunden.", exitCode: 0)
        }

        let workingDirectory = (script as NSString).deletingLastPathComponent
        let result = await Task.detached {
            Shell.run("/bin/bash", [script], workingDirectory: workingDirectory, timeout: 120)
        }.value

        let stdout = result.stdout.trimmingCharacters(in: .whitespacesAndNewlines)
        let stderr = result.stderr.trimmingCharacters(in: .whitespacesAndNewlines)
        let state = readState()

        let status: String
        if stdout.hasPrefix("OPENCODE_UPDATE_STATUS=deferred") {
            status = "deferred"
        } else if stdout.hasPrefix("OPENCODE_UPDATE_STATUS=busy") {
            status = "busy"
        } else {
            status = state.status ?? (result.exitCode == 0 ? "completed" : "failed")
        }

        let message: String
        switch status {
        case "deferred": message = "Die tägliche OpenCode-Updateprüfung ist noch nicht fällig."
        case "busy": message = "Eine OpenCode-Updateprüfung läuft bereits."
        default: message = state.message ?? (result.exitCode == 0 ? stdout : stderr)
        }

        Logger.shared.info("OpenCodeUpdateService", "check", "OpenCode-Updatepruefung abgeschlossen",
                           ["status": status, "message": message, "exitCode": Int(result.exitCode)])
        return OpenCodeUpdateResult(status: status, message: message, exitCode: result.exitCode)
    }

    private func readState() -> (status: String?, message: String?) {
        let path = (Paths.home as NSString).appendingPathComponent(".local/share/opencode-mousefix/update-state.json")
        guard let data = FileManager.default.contents(atPath: path),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return (nil, nil)
        }
        return (root["status"] as? String, root["message"] as? String)
    }
}
