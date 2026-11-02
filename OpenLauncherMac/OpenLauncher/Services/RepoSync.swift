import Foundation

/// Gegenstueck zu RepoSync.cs: gleicht vor jedem CLI-Start das Repo ~/proggs mit GitHub ab. Profile, Regeln
/// und Skills kommen aus dem Repo; ohne Abgleich startete eine Sitzung mit dem veralteten Stand dieses Rechners.
/// Bewusst nur Fast-Forward (`git fetch` plus `git merge --ff-only`): nie ein echter Merge, nie ein Konflikt, und noch nicht committete
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
        let fetch = Shell.run(git, ["-C", repo, "fetch", "--quiet"], environment: env, timeout: 20)
        guard fetch.finished else {
            log.warn("RepoSync", "pull", "git fetch nach 20 s abgebrochen")
            return Result(ok: false, message: "Zeitüberschreitung nach 20 s (offline?)")
        }
        guard fetch.exitCode == 0 else {
            let reason = fetch.stderr.split(separator: "\n").first.map { String($0).trimmingCharacters(in: .whitespaces) }
                ?? "unbekannter Fehler"
            log.warn("RepoSync", "pull", "git fetch fehlgeschlagen: \(reason)")
            return Result(ok: false, message: reason)
        }
        // Der Fetch darf sterben, das Vorspulen nicht: git schreibt erst die Dateien und setzt HEAD zuletzt.
        // Ein Abbruch dazwischen hinterlaesst eine verwaiste index.lock und tausende Scheinaenderungen.
        // Timeout 0: Bei grossem Rueckstand wartet der CLI-Start, statt auf einem halben Stand loszulaufen.
        let pull = Shell.run(git, ["-C", repo, "merge", "--ff-only", "--quiet", "@{u}"],
                             environment: env, timeout: 0)
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

    /// Committet genau eine Datei und pusht sie (vorher Rebase auf origin). Andere, noch nicht committete
    /// Aenderungen paralleler Sitzungen bleiben unangetastet (Commit nur mit Pfad, --autostash beim Rebase).
    static func commitAndPushFile(_ filePath: String, message commitMessage: String) -> Result {
        let log = Logger.shared
        let repo = (Paths.home as NSString).appendingPathComponent("proggs")
        let git = Shell.which("git") ?? "/usr/bin/git"
        var env = ProcessInfo.processInfo.environment
        env["GIT_TERMINAL_PROMPT"] = "0"
        func firstLine(_ text: String) -> String {
            text.split(separator: "\n").first.map { String($0).trimmingCharacters(in: .whitespaces) } ?? "unbekannter Fehler"
        }
        let add = Shell.run(git, ["-C", repo, "add", "--", filePath], environment: env, timeout: 10)
        guard add.exitCode == 0 else { return Result(ok: false, message: firstLine(add.stderr)) }
        let diff = Shell.run(git, ["-C", repo, "diff", "--cached", "--quiet", "--", filePath], environment: env, timeout: 10)
        if diff.exitCode == 0 { return Result(ok: true, message: "keine Änderung") }
        let commit = Shell.run(git, ["-C", repo, "commit", "--quiet", "-m", commitMessage, "--", filePath],
                               environment: env, timeout: 20)
        guard commit.exitCode == 0 else { return Result(ok: false, message: firstLine(commit.stderr)) }
        // Timeout 0: Ein abgebrochener Rebase mit Autostash hinterliesse das Repo mitten in der Arbeit.
        let pull = Shell.run(git, ["-C", repo, "pull", "--rebase", "--autostash", "--quiet"], environment: env, timeout: 0)
        guard pull.finished, pull.exitCode == 0 else {
            return Result(ok: false, message: "committet, aber Abgleich fehlgeschlagen: " + firstLine(pull.stderr))
        }
        let push = Shell.run(git, ["-C", repo, "push", "--quiet", "origin", "HEAD:main"], environment: env, timeout: 30)
        guard push.finished, push.exitCode == 0 else {
            return Result(ok: false, message: "committet, aber Push fehlgeschlagen: " + firstLine(push.stderr))
        }
        log.info("RepoSync", "commitAndPushFile", "Datei committet und gepusht", ["filePath": filePath])
        return Result(ok: true, message: "committet und gepusht")
    }
}
