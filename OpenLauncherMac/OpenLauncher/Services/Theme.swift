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
    // Kontrast-Ueberarbeitung: Flaechen und Raender waren nur 5-11 % deckendes Weiss ueber
    // fast schwarzem Grund - Karten, Knoepfe und Zeilen lagen damit alle im selben Dunkel und
    // die Raender verschwanden fast. Jetzt deckende, gestufte Toene: Karte heller als der
    // Hintergrund, schaltbare Flaeche heller als die Karte, Rand deutlich heller als beides.
    private static let darkPalette = ThemePalette(
        desktopBgTop: .wpf("#14101F"),
        desktopBgMid: .wpf("#0A0810"),
        desktopBgBottom: .wpf("#07060C"),
        windowBg: .wpf("#12101C"),
        titleBarBg: .wpf("#00191826"),
        cardBg: .wpf("#1A1726"),
        surfaceBg: .wpf("#262336"),
        hoverBg: .wpf("#363050"),
        chipBg: .wpf("#2A263C"),
        glassBorder: .wpf("#524C6B"),
        glassBright: .wpf("#6E6790"),
        accentGlow: .wpf("#B37C6CF5"),
        panelBg: .wpf("#1A1726"),
        tableHeaderBg: .wpf("#211E2E"),
        borderSoft: .wpf("#403A55"),
        borderStrong: .wpf("#6A6389"),
        gridLine: .wpf("#3A3550"),
        text: .wpf("#F3F1FB"),
        muted: .wpf("#CFCBDE"),
        dim: .wpf("#9B96AD"),
        accent: .wpf("#B0A6FF"),
        accentGradientTop: .wpf("#BDB6FF"),
        accentGradientBottom: .wpf("#7C6CF5"),
        accentHover: .wpf("#8E80FF"),
        accentPressed: .wpf("#6455E0"),
        accentSoftBg: .wpf("#2E2947"),
        accentLine: .wpf("#6F62D8"),
        selectedBg: .wpf("#3A3168"),
        selectedBorder: .wpf("#B0A6FF"),
        rowSelectedBg: .wpf("#2C2650"),
        statusOkBg: .wpf("#14301F"),
        statusOkFg: .wpf("#5BF0A0"),
        statusWarnBg: .wpf("#33280D"),
        statusWarnFg: .wpf("#FBBF24"),
        statusBadBg: .wpf("#351A1A"),
        statusBadFg: .wpf("#FF9A9A"),
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
