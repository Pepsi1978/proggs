import SwiftUI

/// Festes, dunkles Jarvis-Design (Frank will kein Hell/Dunkel-Umschalten).
enum Farbe {
    static let grund = Color(red: 0.027, green: 0.035, blue: 0.060)
    static let grund2 = Color(red: 0.050, green: 0.062, blue: 0.105)
    static let cyan = Color(red: 0.22, green: 0.88, blue: 1.0)
    static let violett = Color(red: 0.55, green: 0.36, blue: 0.96)
    static let rosa = Color(red: 0.96, green: 0.36, blue: 0.72)
    static let gruen = Color(red: 0.30, green: 0.92, blue: 0.58)
    static let orange = Color(red: 1.0, green: 0.62, blue: 0.24)
    static let rot = Color(red: 1.0, green: 0.36, blue: 0.40)
    static let text = Color.white.opacity(0.94)
    static let text2 = Color.white.opacity(0.62)
    static let text3 = Color.white.opacity(0.38)
    static let linie = Color.white.opacity(0.08)
    static let akzent = LinearGradient(colors: [cyan, violett], startPoint: .topLeading, endPoint: .bottomTrailing)
    static let akzentWarm = LinearGradient(colors: [violett, rosa], startPoint: .topLeading, endPoint: .bottomTrailing)
}

extension Font {
    static func jarvis(_ groesse: CGFloat, _ gewicht: Font.Weight = .regular) -> Font {
        .system(size: groesse, weight: gewicht, design: .rounded)
    }
}

/// Hintergrund mit langsam wandernden Lichtflecken.
struct JarvisHintergrund: View {
    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 20)) { tl in
            let t = tl.date.timeIntervalSinceReferenceDate
            Canvas { ctx, size in
                ctx.fill(Path(CGRect(origin: .zero, size: size)), with: .color(Farbe.grund))
                let flecken: [(Color, Double, Double, Double)] = [
                    (Farbe.cyan, 0.18, 0.07, 0.0), (Farbe.violett, 0.22, 0.05, 2.1), (Farbe.rosa, 0.10, 0.04, 4.2),
                ]
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
                    .fill(Color.white.opacity(0.035))
            )
            .glassEffect(.regular.tint(Color.white.opacity(0.02)), in: .rect(cornerRadius: ecke))
            .overlay(
                RoundedRectangle(cornerRadius: ecke, style: .continuous)
                    .strokeBorder(LinearGradient(colors: [Color.white.opacity(0.16), Color.white.opacity(0.03)],
                                                 startPoint: .top, endPoint: .bottom), lineWidth: 1)
            )
    }
}

struct KartenTitel: View {
    let symbol: String
    let titel: String
    var farbe: Color = Farbe.cyan
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

    enum KugelZustand { case ruhe, hoert, denkt, spricht }

    var body: some View {
        // In Ruhe genügen wenige Bilder je Sekunde, das schont Akku und Grafikchip.
        TimelineView(.animation(minimumInterval: zustand == .ruhe ? 1.0 / 15 : nil)) { tl in
            let t = tl.date.timeIntervalSinceReferenceDate
            let p = Double(min(max(pegel, 0), 1))
            let tempo: Double = zustand == .denkt ? 2.4 : (zustand == .ruhe ? 0.5 : 1.2)
            let atmen = 1 + 0.03 * sin(t * 1.6) + p * 0.22
            // Sprechen: sichtbares Vibrieren, auch wenn die Stimme gerade keinen Pegel liefert.
            let beben = zustand == .spricht ? max(p, 0.4) : 0
            let farben: [Color] = {
                switch zustand {
                case .ruhe: return [Farbe.cyan, Farbe.violett, Farbe.cyan]
                case .hoert: return [Farbe.gruen, Farbe.cyan, Farbe.gruen]
                case .denkt: return [Farbe.violett, Farbe.rosa, Farbe.cyan, Farbe.violett]
                case .spricht: return [Farbe.cyan, Farbe.rosa, Farbe.violett, Farbe.cyan]
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
    var farbe: Color = Farbe.cyan
    var groesse: CGFloat = 34
    let aktion: () -> Void
    @State private var schwebt = false
    var body: some View {
        Button(action: aktion) {
            Image(systemName: symbol)
                .font(.system(size: groesse * 0.42, weight: .semibold))
                .foregroundStyle(aktiv ? Color.black : Farbe.text)
                .frame(width: groesse, height: groesse)
                .background(Circle().fill(aktiv ? AnyShapeStyle(farbe) : AnyShapeStyle(Color.white.opacity(schwebt ? 0.14 : 0.07))))
                .overlay(Circle().strokeBorder(Color.white.opacity(0.10)))
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
    let aktion: () -> Void
    @State private var schwebt = false
    var body: some View {
        Button(action: aktion) {
            HStack(spacing: 8) {
                Image(systemName: symbol).font(.system(size: 13, weight: .semibold))
                Text(titel).font(.jarvis(13, .semibold))
            }
            .foregroundStyle(hervorgehoben ? Color.black : Farbe.text)
            .padding(.horizontal, 16).padding(.vertical, 10)
            .background(Capsule().fill(hervorgehoben ? AnyShapeStyle(Farbe.akzent) : AnyShapeStyle(Color.white.opacity(schwebt ? 0.13 : 0.07))))
            .overlay(Capsule().strokeBorder(Color.white.opacity(0.10)))
            .scaleEffect(schwebt ? 1.02 : 1)
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

    private func inline(_ s: String) -> AttributedString {
        (try? AttributedString(markdown: s, options: .init(interpretedSyntax: .inlineOnlyPreservingWhitespace))) ?? AttributedString(s)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(Array(bloecke.enumerated()), id: \.offset) { _, b in
                switch b {
                case .absatz(let s):
                    Text(inline(s)).font(.system(size: groesse)).foregroundStyle(Farbe.text).lineSpacing(3)
                case .ueberschrift(let s, let stufe):
                    Text(inline(s)).font(.jarvis(stufe <= 1 ? groesse + 6 : (stufe == 2 ? groesse + 3 : groesse + 1), .bold))
                        .foregroundStyle(Farbe.text).padding(.top, 4)
                case .punkt(let zeichen, let s):
                    HStack(alignment: .firstTextBaseline, spacing: 8) {
                        Text(zeichen).font(.system(size: groesse, weight: .bold)).foregroundStyle(Farbe.cyan)
                        Text(inline(s)).font(.system(size: groesse)).foregroundStyle(Farbe.text)
                    }
                case .code(let s):
                    ScrollView(.horizontal, showsIndicators: false) {
                        Text(s).font(.system(size: groesse - 1, design: .monospaced)).foregroundStyle(Farbe.text).padding(12)
                    }
                    .background(RoundedRectangle(cornerRadius: 10).fill(Color.black.opacity(0.35)))
                case .linie:
                    Rectangle().fill(Farbe.linie).frame(height: 1)
                }
            }
        }
        .textSelection(.enabled)
        .tint(Farbe.cyan)
    }
}
