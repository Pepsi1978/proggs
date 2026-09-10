import Foundation

/// Gegenstueck zu RepoSync.cs: gleicht vor jedem CLI-Start das Repo ~/proggs mit GitHub ab. Profile, Regeln
/// und Skills kommen aus dem Repo; ohne Abgleich startete eine Sitzung mit dem veralteten Stand dieses Rechners.
/// Bewusst nur Fast-Forward (`git pull --ff-only`): nie ein Merge, nie ein Konflikt, und noch nicht committete
/// Aenderungen paralleler Sitzungen bleiben unangetastet. Geht das nicht, wird trotzdem gestartet.
enum RepoSync {
    struct Result {
        let ok: Bool
        let message: String
    }

    static func pull() -> Result {
        let log = Logger.shared
        let repo = (Paths.home as NSString).appendingPathComponent("proggs")
        guard FileManager.default.fileExists(atPath: (repo as NSString).appendingPathComponent(".git")) else {
            return Result(ok: true, message: "kein Repo unter ~/proggs")
        }
        let git = Shell.which("git") ?? "/usr/bin/git"
        var env = ProcessInfo.processInfo.environment
        // Nie auf eine Anmeldung warten: der Launcher hat kein Terminal.
        env["GIT_TERMINAL_PROMPT"] = "0"

        let before = Shell.run(git, ["-C", repo, "rev-parse", "HEAD"], environment: env, timeout: 5)
        let pull = Shell.run(git, ["-C", repo, "pull", "--ff-only", "--no-rebase", "--quiet"],
                             environment: env, timeout: 20)
        guard pull.finished else {
            log.warn("RepoSync", "pull", "git pull nach 20 s abgebrochen")
            return Result(ok: false, message: "Zeitüberschreitung nach 20 s (offline?)")
        }
        guard pull.exitCode == 0 else {
            let reason = pull.stderr.split(separator: "\n").first.map { String($0).trimmingCharacters(in: .whitespaces) }
                ?? "unbekannter Fehler"
            log.warn("RepoSync", "pull", "git pull fehlgeschlagen: \(reason)")
            return Result(ok: false, message: reason.localizedCaseInsensitiveContains("fast-forward")
                          ? "lokale Commits oder Änderungen verhindern den Abgleich" : reason)
        }
        let after = Shell.run(git, ["-C", repo, "rev-parse", "HEAD"], environment: env, timeout: 5)
        let message = before.stdout == after.stdout ? "Repo war aktuell" : "Repo aktualisiert"
        log.info("RepoSync", "pull", message)
        return Result(ok: true, message: message)
    }
}
