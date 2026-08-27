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
        current == .dark ? NSColor.wpf("#0A0810") : NSColor.wpf("#E4E1F5")
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
    // Kontrast-Ueberarbeitung: Im Hellmodus waren Raender frueher WEISS (#BFFFFFFF) bzw. nur
    // 10 % deckend - auf hellem Grund also praktisch unsichtbar; schaltbare Flaechen (Ghost-Knoepfe,
    // Zeilen, Eingabefelder) verschwanden im Hintergrund. Jetzt sind alle Raender dunkle,
    // deckende Lila-Grautoene und die Flaechen sind gegenueber den (fast weissen) Karten
    // getoent, damit man sofort sieht, was anklickbar ist.
    private static let lightPalette = ThemePalette(
        desktopBgTop: .wpf("#EFEDFB"),
        desktopBgMid: .wpf("#E4E1F5"),
        desktopBgBottom: .wpf("#D9D5EE"),
        windowBg: .wpf("#F8F7FD"),
        titleBarBg: .wpf("#00FFFFFF"),
        cardBg: .wpf("#FDFDFF"),
        surfaceBg: .wpf("#F0EEFA"),
        hoverBg: .wpf("#DFDAF9"),
        chipBg: .wpf("#E6E2FA"),
        glassBorder: .wpf("#9C96B4"),
        glassBright: .wpf("#B9B3CC"),
        accentGlow: .wpf("#737C6CF5"),
        panelBg: .wpf("#FDFDFF"),
        tableHeaderBg: .wpf("#E7E4F6"),
        borderSoft: .wpf("#AAA4C0"),
        borderStrong: .wpf("#6F6890"),
        gridLine: .wpf("#BDB8CE"),
        text: .wpf("#1B1729"),
        muted: .wpf("#4A4459"),
        dim: .wpf("#6E6880"),
        accent: .wpf("#5D4EDB"),
        accentGradientTop: .wpf("#8375F0"),
        accentGradientBottom: .wpf("#6455E0"),
        accentHover: .wpf("#5646C8"),
        accentPressed: .wpf("#4A3BB0"),
        accentSoftBg: .wpf("#E4E0FB"),
        accentLine: .wpf("#9086F2"),
        selectedBg: .wpf("#DCD7F9"),
        selectedBorder: .wpf("#5D4EDB"),
        rowSelectedBg: .wpf("#DFDAFA"),
        statusOkBg: .wpf("#CDF5DC"),
        statusOkFg: .wpf("#136B36"),
        statusWarnBg: .wpf("#FCEBB4"),
        statusWarnFg: .wpf("#8A4008"),
        statusBadBg: .wpf("#FBD6D6"),
        statusBadFg: .wpf("#B91C1C"),
        closeHoverBg: .wpf("#D93A3F")
    )
}
