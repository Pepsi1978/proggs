import Foundation

/// Fenstergroesse, -position, Breite der Modell-Spalte und gewaehltes Design.
/// 1:1-Port von Services/LayoutSettings.cs (Ablage: ~/Library/Application Support/OpenLauncher).
final class LayoutSettings: Codable {
    private static let defaultModelPaneWidth: Double = 300
    private static let minModelPaneWidth: Double = 240
    private static let maxModelPaneWidth: Double = 760
    private static let defaultWindowWidth: Double = 1360
    private static let defaultWindowHeight: Double = 860
    static let minWindowWidth: Double = 960
    static let minWindowHeight: Double = 600

    private static var filePath: String {
        (Paths.appSupport as NSString).appendingPathComponent("layout.json")
    }

    var modelPaneWidth: Double = LayoutSettings.defaultModelPaneWidth

    // NaN = noch nie gespeichert. Ein numerischer Sentinel wie -1 kollidiert mit echten negativen
    // Fensterkoordinaten (Monitor links/oberhalb des Hauptbildschirms) und wuerde deren
    // Wiederherstellung verhindern.
    var windowLeft: Double = .nan
    var windowTop: Double = .nan
    var windowWidth: Double = LayoutSettings.defaultWindowWidth
    var windowHeight: Double = LayoutSettings.defaultWindowHeight
    var windowState: String = "Normal"

    /// Gewaehltes Design: "Dark" oder "Light". Wird beim App-Start angewendet.
    var theme: String = "Dark"

    enum CodingKeys: String, CodingKey {
        case modelPaneWidth = "ModelPaneWidth"
        case windowLeft = "WindowLeft"
        case windowTop = "WindowTop"
        case windowWidth = "WindowWidth"
        case windowHeight = "WindowHeight"
        case windowState = "WindowState"
        case theme = "Theme"
    }

    init() {}

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        modelPaneWidth = LayoutSettings.decodeDouble(c, .modelPaneWidth) ?? LayoutSettings.defaultModelPaneWidth
        windowLeft = LayoutSettings.decodeDouble(c, .windowLeft) ?? .nan
        windowTop = LayoutSettings.decodeDouble(c, .windowTop) ?? .nan
        windowWidth = LayoutSettings.decodeDouble(c, .windowWidth) ?? LayoutSettings.defaultWindowWidth
        windowHeight = LayoutSettings.decodeDouble(c, .windowHeight) ?? LayoutSettings.defaultWindowHeight
        windowState = (try? c.decode(String.self, forKey: .windowState)) ?? "Normal"
        theme = (try? c.decode(String.self, forKey: .theme)) ?? "Dark"
    }

    /// .NET schreibt NaN als String "NaN" (AllowNamedFloatingPointLiterals). Beide Formen lesen,
    /// damit eine unter Windows geschriebene layout.json den Mac nicht aus dem Tritt bringt.
    private static func decodeDouble(_ c: KeyedDecodingContainer<CodingKeys>, _ key: CodingKeys) -> Double? {
        if let value = try? c.decode(Double.self, forKey: key) { return value }
        if let text = try? c.decode(String.self, forKey: key) {
            if text.caseInsensitiveCompare("NaN") == .orderedSame { return Double.nan }
            return Double(text)
        }
        return nil
    }

    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        try c.encode(modelPaneWidth, forKey: .modelPaneWidth)
        // NaN ist in reinem JSON nicht darstellbar - wie .NET als benannter String schreiben.
        if windowLeft.isNaN { try c.encode("NaN", forKey: .windowLeft) } else { try c.encode(windowLeft, forKey: .windowLeft) }
        if windowTop.isNaN { try c.encode("NaN", forKey: .windowTop) } else { try c.encode(windowTop, forKey: .windowTop) }
        try c.encode(windowWidth, forKey: .windowWidth)
        try c.encode(windowHeight, forKey: .windowHeight)
        try c.encode(windowState, forKey: .windowState)
        try c.encode(theme, forKey: .theme)
    }

    static func load() -> LayoutSettings {
        guard Paths.fileExists(filePath),
              let data = FileManager.default.contents(atPath: filePath) else { return LayoutSettings() }
        do {
            let settings = try JSONDecoder().decode(LayoutSettings.self, from: data)
            settings.modelPaneWidth = clamp(settings.modelPaneWidth)
            settings.normalizeWindowBounds()
            return settings
        } catch {
            Logger.shared.warn("LayoutSettings", "load", "Layout-Settings ignoriert, Defaults: \(error.localizedDescription)")
            return LayoutSettings()
        }
    }

    func save() {
        modelPaneWidth = LayoutSettings.clamp(modelPaneWidth)
        normalizeWindowBounds()
        Paths.ensureDirectory(Paths.appSupport)
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        guard let data = try? encoder.encode(self), let json = String(data: data, encoding: .utf8) else {
            Logger.shared.error("LayoutSettings", "save", "Layout konnte nicht serialisiert werden")
            return
        }
        if !Paths.writeAtomic(json, to: LayoutSettings.filePath) {
            Logger.shared.error("LayoutSettings", "save", "Layout konnte nicht geschrieben werden")
        }
    }

    private static func clamp(_ value: Double) -> Double {
        if value.isNaN || value.isInfinite { return defaultModelPaneWidth }
        return Swift.min(Swift.max(value, minModelPaneWidth), maxModelPaneWidth)
    }

    private func normalizeWindowBounds() {
        if windowWidth.isNaN || windowWidth.isInfinite { windowWidth = LayoutSettings.defaultWindowWidth }
        if windowHeight.isNaN || windowHeight.isInfinite { windowHeight = LayoutSettings.defaultWindowHeight }
        windowWidth = Swift.max(windowWidth, LayoutSettings.minWindowWidth)
        windowHeight = Swift.max(windowHeight, LayoutSettings.minWindowHeight)
        if windowState != "Maximized" { windowState = "Normal" }
    }
}
