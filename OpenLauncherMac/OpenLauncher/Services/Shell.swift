import Foundation

/// Ergebnis eines Kommandozeilen-Aufrufs.
struct ShellResult {
    let exitCode: Int32
    let stdout: String
    let stderr: String
    /// false, wenn der Prozess ins Timeout lief und abgebrochen werden musste.
    let finished: Bool
}

/// Duenne Huelle um Process. Ersetzt die Windows-Aufrufe mit ProcessStartInfo/RedirectStandardOutput.
enum Shell {
    /// Fuehrt ein Programm aus und wartet auf das Ergebnis. `timeout` in Sekunden; 0 = ohne Grenze.
    @discardableResult
    static func run(_ executable: String, _ arguments: [String] = [],
                    workingDirectory: String? = nil,
                    environment: [String: String]? = nil,
                    timeout: TimeInterval = 30) -> ShellResult {
        let process = Process()
        process.executableURL = URL(fileURLWithPath: executable)
        process.arguments = arguments
        if let workingDirectory { process.currentDirectoryURL = URL(fileURLWithPath: workingDirectory) }
        if let environment { process.environment = environment }

        let outPipe = Pipe()
        let errPipe = Pipe()
        process.standardOutput = outPipe
        process.standardError = errPipe

        // Ausgabe waehrend des Laufs mitlesen: ein volles Pipe-Puffer-Fenster (64 KB) wuerde den
        // Kindprozess sonst blockieren und den Aufruf haengen lassen.
        var outData = Data()
        var errData = Data()
        let ioQueue = DispatchQueue(label: "openlauncher.shell.io")
        outPipe.fileHandleForReading.readabilityHandler = { handle in
            let chunk = handle.availableData
            if !chunk.isEmpty { ioQueue.sync { outData.append(chunk) } }
        }
        errPipe.fileHandleForReading.readabilityHandler = { handle in
            let chunk = handle.availableData
            if !chunk.isEmpty { ioQueue.sync { errData.append(chunk) } }
        }

        do {
            try process.run()
        } catch {
            outPipe.fileHandleForReading.readabilityHandler = nil
            errPipe.fileHandleForReading.readabilityHandler = nil
            return ShellResult(exitCode: -1, stdout: "", stderr: error.localizedDescription, finished: false)
        }

        var finished = true
        if timeout > 0 {
            let deadline = Date().addingTimeInterval(timeout)
            while process.isRunning && Date() < deadline {
                Thread.sleep(forTimeInterval: 0.02)
            }
            if process.isRunning {
                process.terminate()
                finished = false
            }
        }
        process.waitUntilExit()

        outPipe.fileHandleForReading.readabilityHandler = nil
        errPipe.fileHandleForReading.readabilityHandler = nil
        // Reste nach dem Ende einsammeln.
        if let rest = try? outPipe.fileHandleForReading.readToEnd(), !rest.isEmpty { ioQueue.sync { outData.append(rest) } }
        if let rest = try? errPipe.fileHandleForReading.readToEnd(), !rest.isEmpty { ioQueue.sync { errData.append(rest) } }

        let out = ioQueue.sync { String(data: outData, encoding: .utf8) ?? "" }
        let err = ioQueue.sync { String(data: errData, encoding: .utf8) ?? "" }
        return ShellResult(exitCode: process.terminationStatus, stdout: out, stderr: err, finished: finished)
    }

    /// Startet ein Programm und wartet NICHT (fuer das Oeffnen von Terminal-Fenstern).
    @discardableResult
    static func launch(_ executable: String, _ arguments: [String] = [],
                       workingDirectory: String? = nil) -> Int32? {
        let process = Process()
        process.executableURL = URL(fileURLWithPath: executable)
        process.arguments = arguments
        if let workingDirectory { process.currentDirectoryURL = URL(fileURLWithPath: workingDirectory) }
        do {
            try process.run()
            return process.processIdentifier
        } catch {
            Logger.shared.error("Shell", "launch", "Start fehlgeschlagen: \(error.localizedDescription)",
                                ["executable": executable])
            return nil
        }
    }

    /// Sucht ein Programm im PATH - Ersatz fuer `where.exe` unter Windows.
    /// Zusaetzlich werden die typischen macOS-Installationsorte geprueft, weil eine per Finder
    /// gestartete .app einen minimalen PATH erbt (/usr/bin:/bin:/usr/sbin:/sbin) und Homebrew,
    /// bun & Co. darin NICHT enthalten sind.
    static func which(_ name: String) -> String? {
        let extraDirs = [
            "\(Paths.home)/.local/bin",
            "\(Paths.home)/.bun/bin",
            "\(Paths.home)/.opencode/bin",
            "\(Paths.home)/.cargo/bin",
            "/opt/homebrew/bin",
            "/usr/local/bin",
            "/usr/bin",
            "/bin"
        ]
        for dir in extraDirs {
            let candidate = (dir as NSString).appendingPathComponent(name)
            if FileManager.default.isExecutableFile(atPath: candidate) { return candidate }
        }

        let result = run("/usr/bin/env", ["which", name], timeout: 5)
        let line = result.stdout.split(separator: "\n").first.map(String.init)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        if let line, !line.isEmpty, FileManager.default.isExecutableFile(atPath: line) { return line }
        return nil
    }

    /// Maskiert einen Wert fuer die Verwendung in einfachen Anfuehrungszeichen in sh/zsh.
    /// Gegenstueck zu EscapePowerShellSingleQuotedValue im Windows-Launcher.
    static func singleQuoted(_ value: String) -> String {
        "'" + value.replacingOccurrences(of: "'", with: "'\\''") + "'"
    }
}
