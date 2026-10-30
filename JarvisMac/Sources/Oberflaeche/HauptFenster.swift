import SwiftUI

struct HauptFenster: View {
    @State private var steuerung = AppSteuerung.shared
    @State private var guthaben = Guthaben.shared

    var body: some View {
        HStack(spacing: 0) {
            Seitenleiste()
                .frame(width: 272)
            Rectangle().fill(Farbe.linie).frame(width: 1)
            ZStack {
                switch steuerung.bereich {
                case .start: StartAnsicht()
                case .zentrale: ZentraleAnsicht()
                case .gespraech:
                    if let g = steuerung.gespraech { ChatAnsicht(gespraech: g).id(g.id) } else { StartAnsicht() }
                case .live: LiveAnsicht()
                case .einstellungen: EinstellungenAnsicht()
                }
            }
            .frame(minWidth: 0, maxWidth: .infinity, maxHeight: .infinity)
            .clipped()
            .transition(.opacity)
            .animation(.easeInOut(duration: 0.18), value: steuerung.bereich)
        }
        .background(JarvisHintergrund())
        .foregroundStyle(Farbe.text)
        .tint(Farbe.haupt)
        .background(TastenKuerzel())
        // Prompt bearbeiten deckt alles ab, auch die Seitenleiste.
        .overlay {
            if let p = steuerung.promptBearbeiten { PromptEditorVollbild(prompt: p).id(p.id).foregroundStyle(Farbe.text).tint(Farbe.haupt) }
        }
        .sheet(isPresented: $guthaben.anmeldungOffen) { ConsoleAnmeldungAnsicht() }
    }
}

/// Fenster-Tastenkürzel: ⌘N neues Gespräch, ⌘L Live, ⌘D Sprechen, ⌘, Einstellungen, ⌘1 Start, ⌘2 Zentrale.
private struct TastenKuerzel: View {
    @State private var steuerung = AppSteuerung.shared
    var body: some View {
        ZStack {
            Button("") { AppSteuerung.shared.neuesGespraech() }.keyboardShortcut("n", modifiers: .command)
            Button("") { AppSteuerung.shared.live() }.keyboardShortcut("l", modifiers: .command)
            Button("") { AppSteuerung.shared.sprechen() }.keyboardShortcut("d", modifiers: .command)
            Button("") { AppSteuerung.shared.bereich = .einstellungen }.keyboardShortcut(",", modifiers: .command)
            Button("") { AppSteuerung.shared.bereich = .start }.keyboardShortcut("1", modifiers: .command)
            Button("") { AppSteuerung.shared.bereich = .zentrale }.keyboardShortcut("2", modifiers: .command)
        }
        // Im Prompt-Editor würden sie den Bereich unsichtbar darunter wechseln.
        .disabled(steuerung.promptBearbeiten != nil)
        .opacity(0).frame(width: 0, height: 0)
    }
}

struct Seitenleiste: View {
    @State private var steuerung = AppSteuerung.shared
    @State private var speicher = GespraechsSpeicher.shared
    @State private var suche = ""
    @State private var reiter = 0          // 0 Gespräche, 1 Programmierung, 2 Projekte
    @State private var projekte = Projekte.shared
    @State private var neuesProjekt = ""
    @State private var design = DesignStand.shared

    private var gefiltert: [Gespraech] {
        let basis: [Gespraech]
        switch reiter {
        case 1: basis = speicher.alle.filter { $0.istProgrammierung }
        case 2: basis = steuerung.projekt.map { p in speicher.alle.filter { $0.projekt == p } } ?? []
        default: basis = speicher.alle.filter { $0.projekt == nil && !$0.istProgrammierung }
        }
        let s = suche.trimmingCharacters(in: .whitespaces).lowercased()
        guard !s.isEmpty else { return basis }
        return basis.filter { $0.titel.lowercased().contains(s) || $0.nachrichten.contains { $0.text.lowercased().contains(s) } }
    }

    /// Welche Reiter gerade arbeiten – nur über die (wenigen) arbeitenden Gespräche, einmal je Darstellung.
    private var arbeitet: [Bool] {
        let aktiv = speicher.alle.filter(\.arbeitet)
        guard !aktiv.isEmpty else { return [false, false, false] }
        let prog = aktiv.map(\.istProgrammierung)
        return [zip(aktiv, prog).contains { $0.projekt == nil && !$1 }, prog.contains(true), aktiv.contains { $0.projekt != nil }]
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 12) {
                KugelLive(groesse: 34)
                VStack(alignment: .leading, spacing: 2) {
                    Text("JARVIS").font(.jarvis(17, .heavy)).tracking(4)
                    HStack(spacing: 6) {
                        LivePunkt(groesse: 7)
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
                ForEach([Bereich.start, .zentrale, .einstellungen]) { b in
                    LeistenEintrag(symbol: b.symbol, titel: b.titel, aktiv: steuerung.bereich == b) { steuerung.bereich = b }
                }
            }

            HStack(spacing: 8) {
                Image(systemName: "magnifyingglass").foregroundStyle(Farbe.text3)
                TextField("Gespräche durchsuchen", text: $suche).textFieldStyle(.plain).font(.jarvis(13))
            }
            .padding(.horizontal, 12).padding(.vertical, 9)
            .background(RoundedRectangle(cornerRadius: 12).fill(Farbe.glas(0.05)))

            let status = arbeitet
            HStack(spacing: 4) {
                ForEach(Array(["Gespräche", "Programmierung", "Projekte"].enumerated()), id: \.offset) { i, name in
                    let laeuft = status[i]
                    Button { reiter = i; if i != 2 { steuerung.projekt = nil } } label: {
                        HStack(spacing: 4) {
                            Text(name).font(.jarvis(11, reiter == i ? .bold : .medium)).lineLimit(1).minimumScaleFactor(0.8)
                            if laeuft { Circle().fill(Farbe.warnung).frame(width: 6, height: 6).shadow(color: Farbe.warnung, radius: 3) }
                        }
                        .foregroundStyle(reiter == i ? Farbe.text : Farbe.text3)
                        .padding(.horizontal, 8).padding(.vertical, 6).frame(maxWidth: .infinity)
                        .background(RoundedRectangle(cornerRadius: 8).fill(Farbe.glas(reiter == i ? 0.09 : 0)))
                    }
                    .buttonStyle(.plain)
                }
            }
            if reiter == 2 {
                if let p = steuerung.projekt {
                    HStack {
                        Button { steuerung.projekt = nil } label: { Image(systemName: "chevron.left") }.buttonStyle(.plain)
                        Text(p).font(.jarvis(13, .bold)).lineLimit(1)
                        Spacer()
                        Button { steuerung.neuesGespraech(imProjekt: true) } label: { Image(systemName: "plus.bubble") }.buttonStyle(.plain).help("Neues Gespräch im Projekt")
                    }
                    .padding(.horizontal, 6)
                } else {
                    HStack {
                        TextField("Neues Projekt", text: $neuesProjekt).textFieldStyle(.plain).font(.jarvis(12))
                            .onSubmit { projekte.neu(neuesProjekt); neuesProjekt = "" }
                        Button { projekte.neu(neuesProjekt); neuesProjekt = "" } label: { Image(systemName: "folder.badge.plus") }.buttonStyle(.plain)
                    }
                    .padding(.horizontal, 10).padding(.vertical, 7)
                    .background(RoundedRectangle(cornerRadius: 10).fill(Farbe.glas(0.05)))
                    ScrollView {
                        VStack(alignment: .leading, spacing: 2) {
                            ForEach(projekte.namen, id: \.self) { p in
                                LeistenEintrag(symbol: "folder.fill", titel: p, aktiv: false) { steuerung.projekt = p }
                            }
                        }
                    }
                    .scrollIndicators(.never)
                }
            }

            ScrollView {
                LazyVStack(alignment: .leading, spacing: 2) {
                    ForEach(gefiltert) { g in
                        GespraechsEintrag(g: g, aktiv: steuerung.bereich == .gespraech && steuerung.gespraech?.id == g.id)
                    }
                }
            }
            .scrollIndicators(.never)

            HStack(spacing: 10) {
                RundKnopf(symbol: design.modus.symbol, hilfe: design.hilfe, groesse: 30) { design.weiter() }
                Text("Jarvis für macOS \(AppInfo.version)").font(.jarvis(10)).foregroundStyle(Farbe.text3)
                Spacer()
            }
            .padding(.bottom, 12).padding(.leading, 2)
        }
        .padding(.horizontal, 14)
        .background(Farbe.tiefe(0.22))
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
            .background(RoundedRectangle(cornerRadius: 11, style: .continuous).fill(Farbe.glas(aktiv ? 0.09 : (schwebt ? 0.05 : 0))))
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
                    Button {
                        GespraechsSpeicher.shared.loeschen(g)
                        if AppSteuerung.shared.gespraech?.id == g.id { AppSteuerung.shared.gespraech = nil }
                        if aktiv { AppSteuerung.shared.bereich = .start }
                    } label: {
                        Image(systemName: "trash").font(.system(size: 11)).foregroundStyle(Farbe.text3)
                    }.buttonStyle(.plain).help("Gespräch löschen")
                }
            }
            .padding(.horizontal, 10).padding(.vertical, 7)
            .background(RoundedRectangle(cornerRadius: 10, style: .continuous).fill(Farbe.glas(aktiv ? 0.09 : (schwebt ? 0.04 : 0))))
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .onHover { schwebt = $0 }
    }
}
