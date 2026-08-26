import AppKit

/// Farbpalette und Design-Umschalter. 1:1-Port von Themes/DarkTheme.xaml und Themes/LightTheme.xaml
/// plus ThemeManager.cs. Die WPF-Farbwerte stehen als #AARRGGBB bzw. #RRGGBB und werden hier
/// unveraendert uebernommen, damit der Mac exakt dieselben Farben zeigt wie Windows.
enum AppTheme: String {
    case dark = "Dark"
    case light = "Light"
}

/// Ein kompletter Farbsatz. Die Schluessel entsprechen 1:1 den DynamicResource-Namen aus WPF.
struct ThemePalette {
    let desktopBgTop: NSColor
    let desktopBgMid: NSColor
    let desktopBgBottom: NSColor

    let windowBg: NSColor
    let titleBarBg: NSColor
    let cardBg: NSColor
    let surfaceBg: NSColor
    let hoverBg: NSColor
    let chipBg: NSColor

    let glassBorder: NSColor
    let glassBright: NSColor
    let accentGlow: NSColor
    let panelBg: NSColor
    let tableHeaderBg: NSColor

    let borderSoft: NSColor
    let borderStrong: NSColor
    let gridLine: NSColor

    let text: NSColor
    let muted: NSColor
    let dim: NSColor

    let accent: NSColor
    let accentGradientTop: NSColor
    let accentGradientBottom: NSColor
    let accentHover: NSColor
    let accentPressed: NSColor
    let accentSoftBg: NSColor
    let accentLine: NSColor

    let selectedBg: NSColor
    let selectedBorder: NSColor
    let rowSelectedBg: NSColor

    let statusOkBg: NSColor
    let statusOkFg: NSColor
    let statusWarnBg: NSColor
    let statusWarnFg: NSColor
    let statusBadBg: NSColor
    let statusBadFg: NSColor

    let closeHoverBg: NSColor
}

extension NSColor {
    /// WPF-Farbschreibweise: "#RRGGBB" oder "#AARRGGBB" (Alpha ZUERST - anders als bei CSS).
    static func wpf(_ hex: String) -> NSColor {
        var value = hex
        if value.hasPrefix("#") { value.removeFirst() }
        guard let raw = UInt32(value, radix: 16) else { return .magenta }

        let a, r, g, b: CGFloat
        if value.count == 8 {
            a = CGFloat((raw >> 24) & 0xFF) / 255.0
            r = CGFloat((raw >> 16) & 0xFF) / 255.0
            g = CGFloat((raw >> 8) & 0xFF) / 255.0
            b = CGFloat(raw & 0xFF) / 255.0
        } else {
            a = 1.0
            r = CGFloat((raw >> 16) & 0xFF) / 255.0
            g = CGFloat((raw >> 8) & 0xFF) / 255.0
            b = CGFloat(raw & 0xFF) / 255.0
        }
        return NSColor(srgbRed: r, green: g, blue: b, alpha: a)
    }

    /// Blendet die Farbe deckend ueber einen Untergrund. AppKit-Layer mischen halbtransparente
    /// Farben zwar korrekt, aber gestapelte Karten wuerden sich dabei sichtbar aufhellen -
    /// WPF rendert dieselbe Struktur flacher. Deshalb an den Stellen, wo es auf exakte Optik
    /// ankommt, vorab verrechnen.
    func flattened(over background: NSColor) -> NSColor {
        guard let top = usingColorSpace(.sRGB), let base = background.usingColorSpace(.sRGB) else { return self }
        let a = top.alphaComponent
        return NSColor(srgbRed: top.redComponent * a + base.redComponent * (1 - a),
                       green: top.greenComponent * a + base.greenComponent * (1 - a),
                       blue: top.blueComponent * a + base.blueComponent * (1 - a),
                       alpha: 1.0)
    }
}

enum ThemeManager {
    /// Wird nach jedem Wechsel gefeuert (Fenster passen Titelleiste und Farben an).
    static let themeChangedNotification = Notification.Name("OpenLauncher.ThemeChanged")

    private(set) static var current: AppTheme = .dark

    static var palette: ThemePalette { current == .dark ? darkPalette : lightPalette }

    static func apply(_ theme: AppTheme) {
        current = theme
        Logger.shared.info("ThemeManager", "apply", "Design gewechselt: \(theme.rawValue)")
        NotificationCenter.default.post(name: themeChangedNotification, object: nil)
    }

    static func toggle() {
        apply(current == .dark ? .light : .dark)
    }

    /// Untergrund, gegen den halbtransparente Flaechen verrechnet werden (der Fensterhintergrund).
    static var flattenBase: NSColor {
        current == .dark ? NSColor.wpf("#0A0810") : NSColor.wpf("#E9E7F8")
    }

    // ===== Dark (Themes/DarkTheme.xaml) =====
    private static let darkPalette = ThemePalette(
        desktopBgTop: .wpf("#14101F"),
        desktopBgMid: .wpf("#0A0810"),
        desktopBgBottom: .wpf("#07060C"),
        windowBg: .wpf("#8C12101C"),
        titleBarBg: .wpf("#00191826"),
        cardBg: .wpf("#6B1E1B2E"),
        surfaceBg: .wpf("#0DFFFFFF"),
        hoverBg: .wpf("#14FFFFFF"),
        chipBg: .wpf("#0FFFFFFF"),
        glassBorder: .wpf("#1CFFFFFF"),
        glassBright: .wpf("#24FFFFFF"),
        accentGlow: .wpf("#B37C6CF5"),
        panelBg: .wpf("#6B1E1B2E"),
        tableHeaderBg: .wpf("#B8181524"),
        borderSoft: .wpf("#14FFFFFF"),
        borderStrong: .wpf("#1CFFFFFF"),
        gridLine: .wpf("#14FFFFFF"),
        text: .wpf("#F1EFF9"),
        muted: .wpf("#A3F1EFF9"),
        dim: .wpf("#6BF1EFF9"),
        accent: .wpf("#A79CFF"),
        accentGradientTop: .wpf("#BDB6FF"),
        accentGradientBottom: .wpf("#7C6CF5"),
        accentHover: .wpf("#B9B0FF"),
        accentPressed: .wpf("#6455E0"),
        accentSoftBg: .wpf("#387C6CF5"),
        accentLine: .wpf("#7A7C6CF5"),
        selectedBg: .wpf("#427C6CF5"),
        selectedBorder: .wpf("#D6A79CFF"),
        rowSelectedBg: .wpf("#297C6CF5"),
        statusOkBg: .wpf("#294ADE80"),
        statusOkFg: .wpf("#5BF0A0"),
        statusWarnBg: .wpf("#29FBBF24"),
        statusWarnFg: .wpf("#FBBF24"),
        statusBadBg: .wpf("#29F87171"),
        statusBadFg: .wpf("#FF8F8F"),
        closeHoverBg: .wpf("#E5484D")
    )

    // ===== Light (Themes/LightTheme.xaml) =====
    private static let lightPalette = ThemePalette(
        desktopBgTop: .wpf("#F2F0FE"),
        desktopBgMid: .wpf("#E9E7F8"),
        desktopBgBottom: .wpf("#E2E0F2"),
        windowBg: .wpf("#85FFFFFF"),
        titleBarBg: .wpf("#00FFFFFF"),
        cardBg: .wpf("#8CFFFFFF"),
        surfaceBg: .wpf("#8CFFFFFF"),
        hoverBg: .wpf("#1A7C6CF5"),
        chipBg: .wpf("#1A7C6CF5"),
        glassBorder: .wpf("#BFFFFFFF"),
        glassBright: .wpf("#E6FFFFFF"),
        accentGlow: .wpf("#737C6CF5"),
        panelBg: .wpf("#8CFFFFFF"),
        tableHeaderBg: .wpf("#B8FFFFFF"),
        borderSoft: .wpf("#1A281E50"),
        borderStrong: .wpf("#BFFFFFFF"),
        gridLine: .wpf("#1A281E50"),
        text: .wpf("#211D33"),
        muted: .wpf("#9E1D1930"),
        dim: .wpf("#6B1D1930"),
        accent: .wpf("#6F60E8"),
        accentGradientTop: .wpf("#A79CFF"),
        accentGradientBottom: .wpf("#7C6CF5"),
        accentHover: .wpf("#6455E0"),
        accentPressed: .wpf("#5646C8"),
        accentSoftBg: .wpf("#D9EDEAFD"),
        accentLine: .wpf("#527C6CF5"),
        selectedBg: .wpf("#D9EEEBFC"),
        selectedBorder: .wpf("#7C6CF5"),
        rowSelectedBg: .wpf("#F1EEFC"),
        statusOkBg: .wpf("#DCFCE7"),
        statusOkFg: .wpf("#15803D"),
        statusWarnBg: .wpf("#FEF3C7"),
        statusWarnFg: .wpf("#B45309"),
        statusBadBg: .wpf("#FEE2E2"),
        statusBadFg: .wpf("#DC2626"),
        closeHoverBg: .wpf("#E5484D")
    )
}
