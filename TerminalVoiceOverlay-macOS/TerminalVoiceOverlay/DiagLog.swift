import Foundation

/// Minimaler, fehlertoleranter JSON-Lines-Datei-Logger fuer Laufzeit-Diagnose
/// (Observability-First). 1:1-Portierung von
/// TerminalVoiceOverlay-Windows/Services/DiagLog.cs.
///
/// Grund fuer die eigene Datei: eine gestartete .app hat KEINE sichtbare Konsole.
/// `NSLog` landet zwar im Unified Log, ist dort aber zwischen Systemmeldungen kaum
/// zu finden und laesst sich nicht einfach mitlesen. Diese Datei dagegen live:
///
///     tail -f ~/Library/Application\ Support/TerminalVoiceOverlay/diag.log
///
/// Format: {"ts":"2026-...","ctx":"Groq","msg":"...", <weitere k/v>}
///
/// Grundsatz wie in Windows: **Logging darf NIE den Ablauf stoeren** — jede Methode
/// schluckt ihre Fehler. Geschrieben wird auf einer eigenen seriellen Queue, damit
/// weder der Audio- noch der UI-Thread auf die Platte wartet.
enum DiagLog {

    /// Ab dieser Groesse wird rotiert (Windows: 1_000_000).
    private static let maxFileBytes: UInt64 = 1_000_000

    private static let queue = DispatchQueue(label: "tvo.diaglog")
    private static var announced = false

    /// Pfad der Log-Datei. Faellt bei Problemen auf das Temp-Verzeichnis zurueck.
    static let filePath: String = resolvePath()

    private static func resolvePath() -> String {
        let fm = FileManager.default
        if let support = fm.urls(for: .applicationSupportDirectory, in: .userDomainMask).first {
            let dir = support.appendingPathComponent("TerminalVoiceOverlay")
            do {
                try fm.createDirectory(at: dir, withIntermediateDirectories: true)
                return dir.appendingPathComponent("diag.log").path
            } catch {
                // faellt unten auf Temp zurueck
            }
        }
        return (NSTemporaryDirectory() as NSString)
            .appendingPathComponent("TerminalVoiceOverlay-diag.log")
    }

    // MARK: - Oeffentliche API (Namen 1:1 wie Windows)

    static func info(_ ctx: String, _ msg: String, _ extra: [(String, Any?)] = []) {
        write(ctx, msg, extra)
    }

    static func warn(_ ctx: String, _ msg: String, _ extra: [(String, Any?)] = []) {
        write(ctx, msg, [("level", "WARN")] + extra)
    }

    static func error(_ ctx: String, _ msg: String, _ err: Error, _ extra: [(String, Any?)] = []) {
        write(ctx, msg, [("level", "ERROR"),
                         ("err", err.localizedDescription),
                         ("type", String(describing: type(of: err)))] + extra)
    }

    /// Laufzeit-Messung. `since` ist ein Startzeitpunkt aus `DiagLog.now()`.
    static func perf(_ ctx: String, _ step: String, since start: CFAbsoluteTime,
                     _ extra: [(String, Any?)] = []) {
        let ms = Int((CFAbsoluteTimeGetCurrent() - start) * 1000)
        write(ctx, "perf", [("step", step), ("ms", ms)] + extra)
    }

    /// Startzeitpunkt fuer `perf` (Windows-Pendant: `Stopwatch.StartNew()`).
    static func now() -> CFAbsoluteTime { CFAbsoluteTimeGetCurrent() }

    /// Schreibt eine JSON-Zeile. Niemals werfend.
    static func write(_ ctx: String, _ msg: String, _ extra: [(String, Any?)] = []) {
        let stamp = timestamp()
        var line = "{"
        line += kv("ts", stamp)
        line += "," + kv("ctx", ctx)
        line += "," + kv("msg", msg)
        for (key, value) in extra {
            let text: String
            if let value = value { text = String(describing: value) } else { text = "null" }
            line += "," + kv(key, text)
        }
        line += "}"

        queue.async {
            if !announced {
                announced = true
                append("{\"ts\":\"\(stamp)\",\"ctx\":\"DiagLog\",\"msg\":\"=== session start ===\"}")
            }
            append(line)
        }
    }

    // MARK: - Intern

    private static func append(_ line: String) {
        rotateIfBig()
        guard let data = (line + "\n").data(using: .utf8) else { return }
        let fm = FileManager.default
        if let handle = FileHandle(forWritingAtPath: filePath) {
            defer { try? handle.close() }
            do {
                try handle.seekToEnd()
                try handle.write(contentsOf: data)
            } catch {
                // Logging darf nie stoeren
            }
        } else {
            fm.createFile(atPath: filePath, contents: data)
        }
    }

    private static func rotateIfBig() {
        let fm = FileManager.default
        guard let attrs = try? fm.attributesOfItem(atPath: filePath),
              let size = attrs[.size] as? UInt64, size > maxFileBytes else { return }
        let backup = filePath + ".1"
        try? fm.removeItem(atPath: backup)
        try? fm.moveItem(atPath: filePath, toPath: backup)
    }

    private static func timestamp() -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS"
        return f.string(from: Date())
    }

    private static func kv(_ key: String, _ value: String) -> String {
        "\"\(escape(key))\":\"\(escape(value))\""
    }

    private static func escape(_ s: String) -> String {
        s.replacingOccurrences(of: "\\", with: "\\\\")
         .replacingOccurrences(of: "\"", with: "\\\"")
         .replacingOccurrences(of: "\r", with: " ")
         .replacingOccurrences(of: "\n", with: " ")
         .replacingOccurrences(of: "\t", with: " ")
    }
}
