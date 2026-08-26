import Foundation

/// Strukturiertes JSON-Lines-Logging (Observability-Standard, Direktive Observability-First).
/// Eintrag pro Zeile: ts, level, module, fn, msg, ctx. Der Log-Pfad wird beim Start EINMAL
/// ausgegeben. 1:1-Port von Services/Logger.cs.
final class Logger: @unchecked Sendable {
    static let shared = Logger()

    let logPath: String
    private let lock = NSLock()

    private init() {
        let dir = (Paths.appSupport as NSString).appendingPathComponent("logs")
        Paths.ensureDirectory(dir)
        let stamp = DateFormatter()
        stamp.dateFormat = "yyyyMMdd"
        logPath = (dir as NSString).appendingPathComponent("launcher_\(stamp.string(from: Date())).jsonl")
        FileHandle.standardError.write("Log: \(logPath)\n".data(using: .utf8)!)
    }

    func info(_ module: String, _ fn: String, _ msg: String, _ ctx: [String: Any]? = nil) {
        write("INFO", module, fn, msg, ctx)
    }

    func warn(_ module: String, _ fn: String, _ msg: String, _ ctx: [String: Any]? = nil) {
        write("WARN", module, fn, msg, ctx)
    }

    func error(_ module: String, _ fn: String, _ msg: String, _ ctx: [String: Any]? = nil) {
        write("ERROR", module, fn, msg, ctx)
    }

    func debug(_ module: String, _ fn: String, _ msg: String, _ ctx: [String: Any]? = nil) {
        write("DEBUG", module, fn, msg, ctx)
    }

    /// Schreibt einen ausfuehrlichen Fehlerbericht als eigene Textdatei und gibt deren Pfad zurueck.
    func writeErrorReport(area: String, details: String) -> String {
        let dir = ((logPath as NSString).deletingLastPathComponent as NSString).appendingPathComponent("errors")
        Paths.ensureDirectory(dir)
        let safeArea = String(area.map { $0.isLetter || $0.isNumber ? $0 : "_" })
        let stamp = DateFormatter()
        stamp.dateFormat = "yyyyMMdd_HHmmss_SSS"
        let path = (dir as NSString).appendingPathComponent("\(stamp.string(from: Date()))_\(safeArea).txt")
        try? details.write(toFile: path, atomically: true, encoding: .utf8)
        return path
    }

    private func write(_ level: String, _ module: String, _ fn: String, _ msg: String, _ ctx: [String: Any]?) {
        let stamp = DateFormatter()
        stamp.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS"
        var entry: [String: Any] = [
            "ts": stamp.string(from: Date()),
            "level": level,
            "module": module,
            "fn": fn,
            "msg": msg
        ]
        if let ctx { entry["ctx"] = sanitize(ctx) }

        guard let data = try? JSONSerialization.data(withJSONObject: entry, options: [.sortedKeys]),
              var line = String(data: data, encoding: .utf8) else { return }
        line += "\n"

        lock.lock()
        defer { lock.unlock() }
        if let handle = FileHandle(forWritingAtPath: logPath) {
            handle.seekToEndOfFile()
            handle.write(line.data(using: .utf8)!)
            try? handle.close()
        } else {
            // Logging darf die App nie killen: fehlschlagendes Anlegen wird still hingenommen.
            try? line.write(toFile: logPath, atomically: false, encoding: .utf8)
        }
    }

    /// JSONSerialization akzeptiert nur JSON-faehige Werte. Alles andere wird zur Beschreibung -
    /// so kann ein exotischer Kontextwert das Logging nicht zum Stolpern bringen.
    private func sanitize(_ value: Any) -> Any {
        switch value {
        case let dict as [String: Any]:
            return dict.mapValues { sanitize($0) }
        case let array as [Any]:
            return array.map { sanitize($0) }
        case is String, is Int, is Double, is Bool:
            return value
        case let optional as Any?:
            if optional == nil { return NSNull() }
            return String(describing: value)
        default:
            return String(describing: value)
        }
    }
}
