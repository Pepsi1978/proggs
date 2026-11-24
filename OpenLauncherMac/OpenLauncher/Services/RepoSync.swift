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
        if before.stdout != after.stdout { starteVorwaermen() }
        return Result(ok: true, message: message)
    }

    /// Neuer Stand vom Repo: den LaunchAgent de.frank.vorwaermen (Werkzeuge/vorwaermen/einrichten.sh) sofort anstoßen, statt bis
    /// zu 5 Minuten auf seinen Takt zu warten (wie RepoSync.cs ab 1.24.121). Er baut die geänderten Apps vor; der Sitzungsbau
    /// wartet per Hook darauf. Über launchd, damit kein Langläufer am Launcher hängt; ohne -k, ein laufender Lauf bleibt.
    private static func starteVorwaermen() {
        let plist = (Paths.home as NSString).appendingPathComponent("Library/LaunchAgents/de.frank.vorwaermen.plist")
        guard FileManager.default.fileExists(atPath: plist) else { return }
        let ziel = "gui/\(getuid())/de.frank.vorwaermen"
        let r = Shell.run("/bin/launchctl", ["kickstart", ziel], environment: ProcessInfo.processInfo.environment, timeout: 5)
        // Läuft der Agent gerade, lehnt launchctl den Anstoß ab: kein Fehler, der laufende Lauf sieht den neuen Stand ohnehin.
        if r.exitCode != 0 { Logger.shared.info("RepoSync", "starteVorwaermen", r.stderr) }
    }

    /// Committet genau eine Datei und pusht sie (vorher Rebase auf origin). Andere, noch nicht committete
    /// Aenderungen paralleler Sitzungen bleiben unangetastet (Commit nur mit Pfad, --autostash beim Rebase).
    /// Liest eine Datei so, wie sie auf GitHub (origin/main) liegt: nur `git fetch` plus `git show`, der
    /// Arbeitsbaum bleibt unberuehrt. nil, wenn offline oder die Datei dort fehlt. Blockiert: nie auf dem Main-Thread.
    static func readUpstreamFile(_ repoRelativePath: String) -> String? {
        let repo = (Paths.home as NSString).appendingPathComponent("proggs")
        guard FileManager.default.fileExists(atPath: (repo as NSString).appendingPathComponent(".git")) else { return nil }
        let git = Shell.which("git") ?? "/usr/bin/git"
        var env = ProcessInfo.processInfo.environment
        env["GIT_TERMINAL_PROMPT"] = "0"
        let fetch = Shell.run(git, ["-C", repo, "fetch", "--quiet"], environment: env, timeout: 20)
        guard fetch.finished, fetch.exitCode == 0 else { return nil }
        let show = Shell.run(git, ["-C", repo, "show", "@{u}:" + repoRelativePath], environment: env, timeout: 10)
        guard show.finished, show.exitCode == 0 else { return nil }
        return show.stdout
    }

    /// Pusht genau eine Datei als eigenen Commit direkt auf den GitHub-Stand (origin/main), ohne Rebase, ohne den
    /// lokalen Branch und ohne fremde Commits paralleler Sitzungen anzufassen (Windows: PushFileOnUpstreamAsync).
    /// Danach wird die Datei vorgemerkt, damit das naechste Vorspulen nicht an ihr scheitert. Blockiert.
    static func pushFileOnUpstream(_ repoRelativePath: String, message: String) -> Result {
        let repo = (Paths.home as NSString).appendingPathComponent("proggs")
        let git = Shell.which("git") ?? "/usr/bin/git"
        var env = ProcessInfo.processInfo.environment
        env["GIT_TERMINAL_PROMPT"] = "0"
        func run(_ args: [String], _ e: [String: String] = env, timeout: TimeInterval = 20) -> String {
            Shell.run(git, ["-C", repo] + args, environment: e, timeout: timeout).stdout.trimmingCharacters(in: .whitespacesAndNewlines)
        }
        let index = (NSTemporaryDirectory() as NSString).appendingPathComponent("openlauncher-index-" + UUID().uuidString)
        defer { try? FileManager.default.removeItem(atPath: index) }
        var indexEnv = env
        indexEnv["GIT_INDEX_FILE"] = index
        for attempt in 1...3 {
            let fetch = Shell.run(git, ["-C", repo, "fetch", "--quiet"], environment: env, timeout: 20)
            guard fetch.finished, fetch.exitCode == 0 else { return Result(ok: false, message: "Abgleich fehlgeschlagen") }
            let upstream = run(["rev-parse", "@{u}"])
            let blob = run(["hash-object", "-w", "--path=" + repoRelativePath, "--", repoRelativePath])
            guard !upstream.isEmpty, !blob.isEmpty else { return Result(ok: false, message: "Git-Stand nicht lesbar") }
            try? FileManager.default.removeItem(atPath: index)
            _ = run(["read-tree", upstream], indexEnv)
            _ = run(["update-index", "--add", "--cacheinfo", "100644,\(blob),\(repoRelativePath)"], indexEnv)
            let tree = run(["write-tree"], indexEnv)
            guard !tree.isEmpty else { return Result(ok: false, message: "Baum nicht erstellbar") }
            if tree == run(["rev-parse", upstream + "^{tree}"]) {
                _ = run(["add", "--", repoRelativePath])
                return Result(ok: true, message: "keine Änderung")
            }
            let commit = run(["commit-tree", tree, "-p", upstream, "-m", message])
            guard !commit.isEmpty else { return Result(ok: false, message: "Commit nicht erstellbar") }
            let push = Shell.run(git, ["-C", repo, "push", "--quiet", "origin", commit + ":refs/heads/main"], environment: env, timeout: 30)
            if push.finished, push.exitCode == 0 {
                _ = Shell.run(git, ["-C", repo, "fetch", "--quiet"], environment: env, timeout: 20)
                _ = run(["add", "--", repoRelativePath])
                Logger.shared.info("RepoSync", "pushFileOnUpstream", "Datei gepusht", ["path": repoRelativePath, "commit": commit, "attempt": "\(attempt)"])
                return Result(ok: true, message: "gepusht")
            }
            if !push.finished { return Result(ok: false, message: "Push: Zeitüberschreitung") }
        }
        return Result(ok: false, message: "Push dreimal abgewiesen (GitHub-Stand wechselte)")
    }

    /// Merkt eine Datei im Index vor (gleicher Inhalt wie GitHub: das Vorspulen scheitert dann nicht an ihr).
    static func stageFile(_ repoRelativePath: String) {
        let repo = (Paths.home as NSString).appendingPathComponent("proggs")
        let git = Shell.which("git") ?? "/usr/bin/git"
        _ = Shell.run(git, ["-C", repo, "add", "--", repoRelativePath], environment: ProcessInfo.processInfo.environment, timeout: 10)
    }

    static func commitAndPushFile(_ filePath: String, message commitMessage: String, localWins: Bool = false) -> Result {
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
        // Lief schon vorher ein Rebase (parallele Sitzung), gehoert er nicht uns: nie abbrechen.
        let gitDir = (repo as NSString).appendingPathComponent(".git")
        let rebaseVorher = FileManager.default.fileExists(atPath: (gitDir as NSString).appendingPathComponent("rebase-merge"))
            || FileManager.default.fileExists(atPath: (gitDir as NSString).appendingPathComponent("rebase-apply"))
        let pull = Shell.run(git, ["-C", repo, "pull", "--rebase", "--autostash", "--quiet"], environment: env, timeout: 0)
        guard pull.finished, pull.exitCode == 0 else {
            // Den eigenen Rebase nie halb stehen lassen: sonst blieben Konfliktmarker im Arbeitsbaum.
            if pull.finished && !rebaseVorher { _ = Shell.run(git, ["-C", repo, "rebase", "--abort"], environment: env, timeout: 20) }
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
