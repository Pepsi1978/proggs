import SwiftUI

/// Jarvis-Design: Schwarz mit Orange im Dunkelmodus, warmes Hell mit Orange im Hellmodus. Umgeschaltet über [DesignStand].
enum Farbe {
    private static var d: Bool { DesignStand.shared.dunkel }
    private static func c(_ r: Double, _ g: Double, _ b: Double) -> Color { Color(red: r, green: g, blue: b) }
    static var grund: Color { d ? .black : c(1.0, 0.975, 0.94) }
    static var grund2: Color { d ? c(0.065, 0.06, 0.055) : c(1.0, 0.95, 0.885) }
    static var haupt: Color { d ? c(1.0, 0.55, 0.10) : c(0.92, 0.43, 0.03) }
    static var gold: Color { d ? c(1.0, 0.73, 0.28) : c(0.82, 0.52, 0.02) }
    static var kupfer: Color { d ? c(1.0, 0.38, 0.16) : c(0.80, 0.29, 0.07) }
    static var gruen: Color { d ? c(0.30, 0.92, 0.58) : c(0.09, 0.58, 0.30) }
    static var warnung: Color { d ? c(1.0, 0.82, 0.28) : c(0.70, 0.42, 0.0) }
    static var rot: Color { d ? c(1.0, 0.36, 0.40) : c(0.80, 0.15, 0.17) }
    static var text: Color { d ? Color.white.opacity(0.94) : Color.black.opacity(0.88) }
    static var text2: Color { d ? Color.white.opacity(0.62) : Color.black.opacity(0.62) }
    static var text3: Color { d ? Color.white.opacity(0.38) : Color.black.opacity(0.42) }
    static var linie: Color { d ? Color.white.opacity(0.10) : Color.black.opacity(0.10) }
    /// Flächen und Ränder: heller als der Grund im Dunkelmodus, dunkler im Hellmodus.
    static func glas(_ a: Double) -> Color { d ? Color.white.opacity(a) : Color.black.opacity(a * 0.8) }
    /// Vertiefte Felder wie Texteditoren.
    static func tiefe(_ a: Double) -> Color { d ? Color.black.opacity(a) : Color.white.opacity(min(1, a * 2.4)) }
    static var akzent: LinearGradient { LinearGradient(colors: [haupt, gold], startPoint: .topLeading, endPoint: .bottomTrailing) }
    static var akzentWarm: LinearGradient { LinearGradient(colors: [kupfer, haupt], startPoint: .topLeading, endPoint: .bottomTrailing) }
}

extension Font {
    static func jarvis(_ groesse: CGFloat, _ gewicht: Font.Weight = .regular) -> Font {
        .system(size: groesse, weight: gewicht, design: .rounded)
    }
}

/// Hintergrund mit langsam wandernden Lichtflecken.
struct JarvisHintergrund: View {
    var body: some View {
        // Vor dem Canvas lesen: Lesevorgänge in seinem Zeichenblock lösen beim Umschalten kein Neuzeichnen aus.
        let grund = Farbe.grund
        let flecken: [(Color, Double, Double, Double)] = [
            (Farbe.haupt, 0.10, 0.07, 0.0), (Farbe.gold, 0.07, 0.05, 2.1), (Farbe.kupfer, 0.06, 0.04, 4.2),
        ]
        // Die Flecken wandern langsam: Zehn Bilder je Sekunde genügen. Im Hintergrund und während Sprache und Live steht der Verlauf ganz still.
        let bewegt = AppSteuerung.shared.appVorn && SprachSchleife.shared.zustand == .aus && !LiveGespraech.shared.aktiv
        return TimelineView(.animation(minimumInterval: 1.0 / 10, paused: !bewegt)) { tl in
            let t = tl.date.timeIntervalSinceReferenceDate
            Canvas { ctx, size in
                ctx.fill(Path(CGRect(origin: .zero, size: size)), with: .color(grund))
                for (farbe, staerke, tempo, phase) in flecken {
                    let x = size.width * (0.5 + 0.38 * cos(t * tempo + phase))
                    let y = size.height * (0.5 + 0.34 * sin(t * tempo * 1.3 + phase))
                    let r = max(size.width, size.height) * 0.55
                    ctx.fill(Path(ellipseIn: CGRect(x: x - r, y: y - r, width: r * 2, height: r * 2)),
                             with: .radialGradient(Gradient(colors: [farbe.opacity(staerke), .clear]),
                                                   center: CGPoint(x: x, y: y), startRadius: 0, endRadius: r))
                }
            }
        }
        .ignoresSafeArea()
    }
}

/// Glas-Karte im Liquid-Glass-Stil.
struct GlasKarte<Inhalt: View>: View {
    var ecke: CGFloat = 22
    var abstand: CGFloat = 18
    @ViewBuilder var inhalt: Inhalt
    var body: some View {
        inhalt
            .padding(abstand)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: ecke, style: .continuous)
                    .fill(Farbe.glas(0.035))
            )
            .glassEffect(.regular.tint(Farbe.glas(0.02)), in: .rect(cornerRadius: ecke))
            .overlay(
                RoundedRectangle(cornerRadius: ecke, style: .continuous)
                    .strokeBorder(LinearGradient(colors: [Farbe.glas(0.16), Farbe.glas(0.03)],
                                                 startPoint: .top, endPoint: .bottom), lineWidth: 1)
            )
    }
}

struct KartenTitel: View {
    let symbol: String
    let titel: String
    var farbe: Color = Farbe.haupt
    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: symbol)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(farbe)
                .frame(width: 30, height: 30)
                .background(Circle().fill(farbe.opacity(0.14)))
            Text(titel).font(.jarvis(15, .semibold)).foregroundStyle(Farbe.text)
        }
    }
}

/// Die Jarvis-Kugel: runde, seitlich schimmernde Lichtkugel ohne Rahmen. Vibriert beim Sprechen, zeigt beim Zuhören Wellen und ein Mikrofon.
struct JarvisKugel: View {
    var pegel: Float = 0
    var zustand: KugelZustand = .ruhe
    var groesse: CGFloat = 220
    /// Steht fest und rechnet keine Bilder, etwa neben fertigen Antworten.
    var still = false

    enum KugelZustand { case ruhe, hoert, denkt, spricht }

    var body: some View {
        // In Ruhe genügen wenige Bilder je Sekunde, sonst 30; verdeckt oder im Hintergrund gar keine. Das schont Akku und Grafikchip,
        // und Mikrofon und Live bekommen ihre Rechenzeit.
        TimelineView(.animation(minimumInterval: zustand == .ruhe ? 1.0 / 15 : 1.0 / 30, paused: still || !AppSteuerung.shared.appVorn)) { tl in
            let t = tl.date.timeIntervalSinceReferenceDate
            let p = Double(min(max(pegel, 0), 1))
            let tempo: Double = zustand == .denkt ? 2.4 : (zustand == .ruhe ? 0.5 : 1.2)
            let atmen = 1 + 0.03 * sin(t * 1.6) + p * 0.22
            // Sprechen: sichtbares Vibrieren, auch wenn die Stimme gerade keinen Pegel liefert.
            let beben = zustand == .spricht ? max(p, 0.4) : 0
            let farben: [Color] = {
                switch zustand {
                case .ruhe: return [Farbe.haupt, Farbe.gold, Farbe.haupt]
                case .hoert: return [Farbe.gruen, Farbe.haupt, Farbe.gruen]
                case .denkt: return [Farbe.gold, Farbe.kupfer, Farbe.haupt, Farbe.gold]
                case .spricht: return [Farbe.haupt, Farbe.kupfer, Farbe.gold, Farbe.haupt]
                }
            }()
            ZStack {
                Circle()
                    .fill(RadialGradient(colors: [farben[0].opacity(0.45), .clear], center: .center, startRadius: 0, endRadius: groesse * 0.75))
                    .frame(width: groesse * 1.6, height: groesse * 1.6)
                    .blur(radius: groesse * 0.1)
                    .scaleEffect(atmen)
                if zustand == .hoert { zuhoerWellen(t: t, p: p) }
                ForEach(0..<3, id: \.self) { i in
                    let w = Double(i)
                    Circle()
                        .fill(AngularGradient(colors: farben, center: .center, angle: .degrees(t * 40 * tempo * (w + 1) + w * 70)))
                        .frame(width: groesse * (0.82 - 0.14 * w), height: groesse * (0.82 - 0.14 * w))
                        .blur(radius: groesse * (0.07 - w * 0.015))
                        .opacity(0.75 - w * 0.12)
                        .offset(x: cos(t * tempo + w * 2) * groesse * 0.035 * (1 + p * 2),
                                y: sin(t * tempo * 1.2 + w) * groesse * 0.035 * (1 + p * 2))
                        .blendMode(.screen)
                }
                Circle()
                    .fill(RadialGradient(colors: [Color.white.opacity(0.85), Color.white.opacity(0.0)], center: .init(x: 0.42, y: 0.38),
                                         startRadius: 0, endRadius: groesse * 0.32))
                    .frame(width: groesse * 0.62, height: groesse * 0.62)
                    .blendMode(.plusLighter)
                    .opacity(0.55 + p * 0.4)
                // Seitlicher Schimmer: zwei weiche Lichtbögen links und rechts statt eines Rings.
                ForEach(0..<2, id: \.self) { i in
                    let seite = Double(i) * 180
                    Circle()
                        .trim(from: 0, to: 0.2)
                        .stroke(i == 0 ? Color.white.opacity(0.75) : farben[0].opacity(0.85),
                                style: StrokeStyle(lineWidth: groesse * 0.035, lineCap: .round))
                        .frame(width: groesse * 0.78, height: groesse * 0.78)
                        .blur(radius: groesse * 0.025)
                        .rotationEffect(.degrees(seite - 36 + 22 * sin(t * 0.9 + seite)))
                        .opacity(0.5 + 0.35 * sin(t * 1.7 + seite))
                        .blendMode(.plusLighter)
                }
                if zustand == .hoert && groesse >= 80 {
                    Image(systemName: "mic.fill")
                        .font(.system(size: groesse * 0.16, weight: .semibold))
                        .foregroundStyle(Color.white.opacity(0.95))
                        .shadow(color: Farbe.gruen, radius: groesse * 0.03)
                        .scaleEffect(1 + p * 0.35)
                }
            }
            .scaleEffect(x: 1 + 0.07 * beben * sin(t * 33), y: 1 + 0.07 * beben * cos(t * 41))
            .offset(x: groesse * 0.018 * beben * sin(t * 47), y: groesse * 0.018 * beben * cos(t * 53))
            // Malfläche größer als der Lichthof: sonst schneidet drawingGroup ihn als Viereck ab.
            .frame(width: groesse * 2.3, height: groesse * 2.3)
            .drawingGroup()
            .frame(width: groesse, height: groesse)
        }
        .contentShape(Circle())
    }

    /// Eigene Zuhör-Animation: Wellen laufen vom Rand der Kugel nach außen, lauter gesprochen = kräftiger.
    private func zuhoerWellen(t: Double, p: Double) -> some View {
        ForEach(0..<3, id: \.self) { i in
            let phase = (t * 0.55 + Double(i) / 3).truncatingRemainder(dividingBy: 1)
            Circle()
                .stroke(Farbe.gruen.opacity((1 - phase) * (0.45 + p * 0.5)), lineWidth: groesse * 0.012)
                .frame(width: groesse * (0.8 + 0.75 * phase), height: groesse * (0.8 + 0.75 * phase))
        }
    }
}

/// Kugel, die selbst auf Sprechen und Zuhören reagiert. Eigene Ansicht: Pegeländerungen zeichnen nur sie neu, nicht die ganze Seite.
struct KugelLive: View {
    var groesse: CGFloat
    var body: some View { JarvisKugel(pegel: KugelStand.pegel, zustand: KugelStand.zustand, groesse: groesse) }
}

/// Zustand und Pegel der Kugel aus allem, was gerade spricht oder zuhört (Sprachschleife, Live, reines Vorlesen).
@MainActor
enum KugelStand {
    static var zustand: JarvisKugel.KugelZustand {
        let schleife = SprachSchleife.shared.zustand, live = LiveGespraech.shared.zustand
        if Stimme.shared.spricht || schleife == .spricht || live == .spricht { return .spricht }
        if schleife == .denkt || live == .denkt { return .denkt }
        if schleife == .hoert || live == .hoert { return .hoert }
        return .ruhe
    }
    static var pegel: Float {
        switch zustand {
        case .spricht: return max(Stimme.shared.pegel, LiveGespraech.shared.pegelAus)
        case .hoert: return max(Diktat.shared.pegel, LiveGespraech.shared.pegelEin)
        default: return 0
        }
    }
}

struct RundKnopf: View {
    let symbol: String
    var hilfe: String = ""
    var aktiv = false
    var farbe: Color = Farbe.haupt
    var groesse: CGFloat = 34
    let aktion: () -> Void
    @State private var schwebt = false
    var body: some View {
        Button(action: aktion) {
            Image(systemName: symbol)
                .font(.system(size: groesse * 0.42, weight: .semibold))
                .foregroundStyle(aktiv ? Color.black : Farbe.text)
                .frame(width: groesse, height: groesse)
                .background(Circle().fill(aktiv ? AnyShapeStyle(farbe) : AnyShapeStyle(Farbe.glas(schwebt ? 0.14 : 0.07))))
                .overlay(Circle().strokeBorder(Farbe.glas(0.10)))
        }
        .buttonStyle(.plain)
        .help(hilfe)
        .onHover { schwebt = $0 }
        .animation(.easeOut(duration: 0.15), value: schwebt)
    }
}

struct KapselKnopf: View {
    let symbol: String
    let titel: String
    var hervorgehoben = false
    /// Volle Breite, Inhalt linksbündig (Schnellverknüpfungen).
    var voll = false
    let aktion: () -> Void
    @State private var schwebt = false
    var body: some View {
        Button(action: aktion) {
            HStack(spacing: 8) {
                Image(systemName: symbol).font(.system(size: 13, weight: .semibold))
                Text(titel).font(.jarvis(13, .semibold))
            }
            .frame(maxWidth: voll ? .infinity : nil, alignment: .leading)
            .foregroundStyle(hervorgehoben ? Color.black : Farbe.text)
            .padding(.horizontal, 16).padding(.vertical, 10)
            .background(Capsule().fill(hervorgehoben ? AnyShapeStyle(Farbe.akzent) : AnyShapeStyle(Farbe.glas(schwebt ? 0.13 : 0.07))))
            .overlay(Capsule().strokeBorder(Farbe.glas(0.10)))
            .contentShape(Capsule())
            .scaleEffect(schwebt && !voll ? 1.02 : 1)
        }
        .buttonStyle(.plain)
        .onHover { schwebt = $0 }
        .animation(.spring(duration: 0.25), value: schwebt)
    }
}

/// Einfache Markdown-Darstellung (Überschriften, Listen, Codeblöcke, Fett/Kursiv/Links inline).
struct MarkdownText: View {
    let text: String
    var groesse: CGFloat = 14

    private enum Block: Hashable { case absatz(String), ueberschrift(String, Int), punkt(String, String), code(String), linie }

    private var bloecke: [Block] {
        var r: [Block] = []
        var code: [String]? = nil
        var absatz: [String] = []
        func absatzFertig() { if !absatz.isEmpty { r.append(.absatz(absatz.joined(separator: "\n"))); absatz = [] } }
        for zeile in text.components(separatedBy: "\n") {
            let z = zeile.trimmingCharacters(in: .whitespaces)
            if z.hasPrefix("```") {
                if let c = code { r.append(.code(c.joined(separator: "\n"))); code = nil } else { absatzFertig(); code = [] }
                continue
            }
            if code != nil { code!.append(zeile); continue }
            if z.hasPrefix("<!--") && z.hasSuffix("-->") { continue }
            if z.isEmpty { absatzFertig(); continue }
            if z.hasPrefix("#") {
                absatzFertig()
                let stufe = z.prefix { $0 == "#" }.count
                r.append(.ueberschrift(String(z.dropFirst(stufe)).trimmingCharacters(in: .whitespaces), stufe)); continue
            }
            if z == "---" || z == "***" { absatzFertig(); r.append(.linie); continue }
            if z.hasPrefix("- ") || z.hasPrefix("* ") || z.hasPrefix("• ") {
                absatzFertig(); r.append(.punkt("•", String(z.dropFirst(2)))); continue
            }
            if let p = z.firstIndex(of: "."), z[..<p].allSatisfy(\.isNumber), !z[..<p].isEmpty, z[z.index(after: p)...].hasPrefix(" ") {
                absatzFertig(); r.append(.punkt(String(z[...p]), String(z[z.index(p, offsetBy: 2)...]))); continue
            }
            absatz.append(zeile)
        }
        if let c = code { r.append(.code(c.joined(separator: "\n"))) }
        absatzFertig()
        return r
    }

    private final class Inline { let wert: AttributedString; init(_ w: AttributedString) { wert = w } }
    /// Beim Streamen ändert sich nur der letzte Absatz: Die übrigen kommen aus dem Vorrat, statt bei jedem Bild neu geparst zu werden.
    @MainActor private static let vorrat: NSCache<NSString, Inline> = { let c = NSCache<NSString, Inline>(); c.totalCostLimit = 8_000_000; return c }()

    /// Der letzte Block wächst beim Streamen noch: Seine Zwischenstände kommen nicht in den Vorrat und verdrängen dort nichts.
    private func inline(_ s: String, merken: Bool = true) -> AttributedString {
        if let da = Self.vorrat.object(forKey: s as NSString) { return da.wert }
        let neu = (try? AttributedString(markdown: s, options: .init(interpretedSyntax: .inlineOnlyPreservingWhitespace))) ?? AttributedString(s)
        if merken { Self.vorrat.setObject(Inline(neu), forKey: s as NSString, cost: s.utf8.count) }
        return neu
    }

    var body: some View {
        let alle = bloecke
        VStack(alignment: .leading, spacing: 8) {
            ForEach(Array(alle.enumerated()), id: \.offset) { i, b in
                switch b {
                case .absatz(let s):
                    Text(inline(s, merken: i < alle.count - 1)).font(.system(size: groesse)).foregroundStyle(Farbe.text).lineSpacing(3)
                case .ueberschrift(let s, let stufe):
                    Text(inline(s)).font(.jarvis(stufe <= 1 ? groesse + 6 : (stufe == 2 ? groesse + 3 : groesse + 1), .bold))
                        .foregroundStyle(Farbe.text).padding(.top, 4)
                case .punkt(let zeichen, let s):
                    HStack(alignment: .firstTextBaseline, spacing: 8) {
                        Text(zeichen).font(.system(size: groesse, weight: .bold)).foregroundStyle(Farbe.haupt)
                        Text(inline(s, merken: i < alle.count - 1)).font(.system(size: groesse)).foregroundStyle(Farbe.text)
                    }
                case .code(let s):
                    ScrollView(.horizontal, showsIndicators: false) {
                        Text(s).font(.system(size: groesse - 1, design: .monospaced)).foregroundStyle(Farbe.text).padding(12)
                    }
                    .background(RoundedRectangle(cornerRadius: 10).fill(Farbe.tiefe(0.35)))
                case .linie:
                    Rectangle().fill(Farbe.linie).frame(height: 1)
                }
            }
        }
        .textSelection(.enabled)
        .tint(Farbe.haupt)
    }
}
