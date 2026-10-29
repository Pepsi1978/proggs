import SwiftUI

struct HauptFenster: View {
    @State private var steuerung = AppSteuerung.shared

    var body: some View {
        HStack(spacing: 0) {
            Seitenleiste()
                .frame(width: 272)
            Rectangle().fill(Farbe.linie).frame(width: 1)
            ZStack {
                switch steuerung.bereich {
                case .zentrale: ZentraleAnsicht()
                case .gespraech:
                    if let g = steuerung.gespraech { ChatAnsicht(gespraech: g).id(g.id) } else { ZentraleAnsicht() }
                case .live: LiveAnsicht()
                case .ablage: AblageAnsicht()
                case .gedaechtnis: GedaechtnisAnsicht()
                case .aktivitaet: AktivitaetAnsicht()
                case .einstellungen: EinstellungenAnsicht()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .transition(.opacity)
            .animation(.easeInOut(duration: 0.18), value: steuerung.bereich)
        }
        .background(JarvisHintergrund())
        .foregroundStyle(Farbe.text)
        .background(TastenKuerzel())
    }
}

/// Fenster-Tastenkürzel: ⌘N neues Gespräch, ⌘L Live, ⌘D Sprechen, ⌘, Einstellungen, ⌘1 Zentrale.
private struct TastenKuerzel: View {
    var body: some View {
        ZStack {
            Button("") { AppSteuerung.shared.neuesGespraech() }.keyboardShortcut("n", modifiers: .command)
            Button("") { AppSteuerung.shared.live() }.keyboardShortcut("l", modifiers: .command)
            Button("") { AppSteuerung.shared.sprechen() }.keyboardShortcut("d", modifiers: .command)
            Button("") { AppSteuerung.shared.bereich = .einstellungen }.keyboardShortcut(",", modifiers: .command)
            Button("") { AppSteuerung.shared.bereich = .zentrale }.keyboardShortcut("1", modifiers: .command)
        }
        .opacity(0).frame(width: 0, height: 0)
    }
}

struct Seitenleiste: View {
    @State private var steuerung = AppSteuerung.shared
    @State private var speicher = GespraechsSpeicher.shared
    @State private var suche = ""

    private var gefiltert: [Gespraech] {
        let s = suche.trimmingCharacters(in: .whitespaces).lowercased()
        guard !s.isEmpty else { return speicher.alle }
        return speicher.alle.filter { $0.titel.lowercased().contains(s) || $0.nachrichten.contains { $0.text.lowercased().contains(s) } }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 12) {
                JarvisKugel(pegel: 0, zustand: .ruhe, groesse: 38)
                VStack(alignment: .leading, spacing: 2) {
                    Text("JARVIS").font(.jarvis(17, .heavy)).tracking(4)
                    HStack(spacing: 6) {
                        Circle().fill(steuerung.handyVerbunden ? Farbe.gruen : Farbe.orange).frame(width: 7, height: 7)
                            .shadow(color: steuerung.handyVerbunden ? Farbe.gruen : Farbe.orange, radius: 4)
                        Text(steuerung.handyVerbunden ? "Mit dem Handy verbunden" : "Handy nicht erreichbar")
                            .font(.jarvis(11, .medium)).foregroundStyle(Farbe.text2)
                    }
                }
            }
            .padding(.top, 34)
            .padding(.horizontal, 6)

            Button { steuerung.neuesGespraech() } label: {
                HStack {
                    Image(systemName: "plus.bubble.fill")
                    Text("Neues Gespräch").font(.jarvis(14, .semibold))
                    Spacer()
                    Text("⌘N").font(.jarvis(11, .medium)).foregroundStyle(Color.black.opacity(0.55))
                }
                .foregroundStyle(Color.black)
                .padding(.horizontal, 14).padding(.vertical, 11)
                .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(Farbe.akzent))
            }
            .buttonStyle(.plain)

            VStack(spacing: 2) {
                ForEach([Bereich.zentrale, .live, .ablage, .gedaechtnis, .aktivitaet, .einstellungen]) { b in
                    LeistenEintrag(symbol: b.symbol, titel: b.titel, aktiv: steuerung.bereich == b) { steuerung.bereich = b }
                }
            }

            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass").foregroundStyle(Farbe.text3)
                TextField("Gespräche durchsuchen", text: $suche).textFieldStyle(.plain).font(.jarvis(13))
            }
            .padding(.horizontal, 12).padding(.vertical, 9)
            .background(RoundedRectangle(cornerRadius: 12).fill(Color.white.opacity(0.05)))

            Text("GESPRÄCHE").font(.jarvis(10, .bold)).tracking(2).foregroundStyle(Farbe.text3).padding(.leading, 6)

            ScrollView {
                LazyVStack(alignment: .leading, spacing: 2) {
                    ForEach(gefiltert) { g in
                        GespraechsEintrag(g: g, aktiv: steuerung.bereich == .gespraech && steuerung.gespraech?.id == g.id)
                    }
                }
            }
            .scrollIndicators(.never)

            HStack {
                Text("Jarvis für macOS \(AppInfo.version)").font(.jarvis(10)).foregroundStyle(Farbe.text3)
                Spacer()
            }
            .padding(.bottom, 12).padding(.leading, 6)
        }
        .padding(.horizontal, 14)
        .background(Color.black.opacity(0.22))
    }
}

struct LeistenEintrag: View {
    let symbol: String
    let titel: String
    let aktiv: Bool
    let aktion: () -> Void
    @State private var schwebt = false
    var body: some View {
        Button(action: aktion) {
            HStack(spacing: 12) {
                Image(systemName: symbol).font(.system(size: 13, weight: .semibold)).frame(width: 20)
                    .foregroundStyle(aktiv ? AnyShapeStyle(Farbe.akzent) : AnyShapeStyle(Farbe.text2))
                Text(titel).font(.jarvis(14, aktiv ? .semibold : .medium)).foregroundStyle(aktiv ? Farbe.text : Farbe.text2)
                Spacer()
            }
            .padding(.horizontal, 12).padding(.vertical, 9)
            .background(RoundedRectangle(cornerRadius: 11, style: .continuous).fill(Color.white.opacity(aktiv ? 0.09 : (schwebt ? 0.05 : 0))))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .onHover { schwebt = $0 }
    }
}

struct GespraechsEintrag: View {
    let g: Gespraech
    let aktiv: Bool
    @State private var schwebt = false
    var body: some View {
        Button { AppSteuerung.shared.oeffne(g) } label: {
            HStack(spacing: 10) {
                if g.arbeitet {
                    ProgressView().controlSize(.mini).frame(width: 14)
                } else {
                    Image(systemName: g.titel == "Sprachgespräch" ? "waveform" : "bubble.left.fill")
                        .font(.system(size: 10)).foregroundStyle(Farbe.text3).frame(width: 14)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(g.titel).font(.jarvis(13, aktiv ? .semibold : .regular)).lineLimit(1)
                        .foregroundStyle(aktiv ? Farbe.text : Farbe.text2)
                    Text(g.geaendert.formatted(.relative(presentation: .named))).font(.jarvis(10)).foregroundStyle(Farbe.text3)
                }
                Spacer(minLength: 0)
                if schwebt {
                    Button { GespraechsSpeicher.shared.loeschen(g); if aktiv { AppSteuerung.shared.bereich = .zentrale } } label: {
                        Image(systemName: "trash").font(.system(size: 11)).foregroundStyle(Farbe.text3)
                    }.buttonStyle(.plain).help("Gespräch löschen")
                }
            }
            .padding(.horizontal, 10).padding(.vertical, 7)
            .background(RoundedRectangle(cornerRadius: 10, style: .continuous).fill(Color.white.opacity(aktiv ? 0.09 : (schwebt ? 0.04 : 0))))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .onHover { schwebt = $0 }
    }
}
