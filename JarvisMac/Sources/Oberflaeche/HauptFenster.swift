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
                case .ablage: AblageAnsicht()
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
            // Wie am Handy frei über dem Inhalt, nur dort, wo die Blase steht: Sie kosten keinen Platz.
            .overlay(alignment: .bottomTrailing) {
                if steuerung.bereich == .start || steuerung.bereich == .gespraech {
                    SchwebeKnoepfe().padding(.trailing, 14).padding(.bottom, 146)
                }
            }
        }
        .background(JarvisHintergrund())
        .foregroundStyle(Farbe.text)
        .tint(Farbe.haupt)
        .background(TastenKuerzel())
        // Prompt bearbeiten deckt alles ab, auch die Seitenleiste.
        .overlay {
            if let p = steuerung.promptBearbeiten { PromptEditorVollbild(prompt: p).id(p.id).foregroundStyle(Farbe.text).tint(Farbe.haupt) }
        }
        .sheet(isPresented: $guthaben.anmeldungOffen, onDismiss: { Task { await guthaben.laden(frisch: true) } }) { ConsoleAnmeldungAnsicht() }
        // Hier und nicht in der Prompt-Liste: Das Senden verlässt die Ablage. Abbrechen gilt als Nein, der Prompt bleibt.
        .alert("Soll der Prompt gelöscht werden?", isPresented: Binding(get: { steuerung.promptLoeschenFrage != nil }, set: { if !$0 { steuerung.promptLoeschenFrage = nil } }), presenting: steuerung.promptLoeschenFrage) { p in
            Button("Ja", role: .destructive) { steuerung.gesendetenPromptLoeschen(p) }
            Button("Nein", role: .cancel) {}
        } message: { _ in
            Text("Der Text in der Eingabe bleibt erhalten.")
        }
        .alert("Prompt nicht gelöscht", isPresented: Binding(get: { steuerung.promptLoeschenFehler != nil }, set: { if !$0 { steuerung.promptLoeschenFehler = nil } }), presenting: steuerung.promptLoeschenFehler) { _ in
            Button("OK", role: .cancel) {}
        } message: { fehler in
            Text(fehler)
        }
    }
}

/// Fenster-Tastenkürzel: ⌘N und ⌘1 Start mit neuem Gespräch, ⌘L Live, ⌘D Sprechen, ⌘, Einstellungen, ⌘2 Zentrale, ⌘3 Ablage.
private struct TastenKuerzel: View {
    @State private var steuerung = AppSteuerung.shared
    var body: some View {
        ZStack {
            Button("") { AppSteuerung.shared.neuesGespraech() }.keyboardShortcut("n", modifiers: .command)
            Button("") { AppSteuerung.shared.live() }.keyboardShortcut("l", modifiers: .command)
            Button("") { AppSteuerung.shared.sprechen() }.keyboardShortcut("d", modifiers: .command)
            Button("") { AppSteuerung.shared.bereich = .einstellungen }.keyboardShortcut(",", modifiers: .command)
            Button("") { AppSteuerung.shared.startOeffnen() }.keyboardShortcut("1", modifiers: .command)
            Button("") { AppSteuerung.shared.bereich = .zentrale }.keyboardShortcut("2", modifiers: .command)
            Button("") { AppSteuerung.shared.bereich = .ablage }.keyboardShortcut("3", modifiers: .command)
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
    @State private var live = LiveGespraech.shared

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

            VStack(spacing: 2) {
                // „Start“ beginnt ein neues Gespräch und öffnet unten die Sprechblase.
                Button { steuerung.startOeffnen() } label: {
                    HStack(spacing: 12) {
                        Image(systemName: Bereich.start.symbol).font(.system(size: 13, weight: .semibold)).frame(width: 20)
                        Text(Bereich.start.titel).font(.jarvis(14, .semibold))
                        Spacer()
                        Text("⌘N").font(.jarvis(11, .medium)).foregroundStyle(Color.black.opacity(0.55))
                    }
                    .foregroundStyle(Color.black)
                    .padding(.horizontal, 12).padding(.vertical, 10)
                    .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(Farbe.akzent))
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .padding(.bottom, 4)
                ForEach([Bereich.zentrale, .ablage, .einstellungen]) { b in
                    LeistenEintrag(symbol: b.symbol, titel: b.titel, aktiv: steuerung.bereich == b) { steuerung.bereich = b }
                }
                // Der Rückweg zu einem laufenden Gespräch; gestartet wird Live aus der Startblase.
                if live.aktiv {
                    LeistenEintrag(symbol: Bereich.live.symbol, titel: "Live läuft", aktiv: steuerung.bereich == .live) { steuerung.bereich = .live }
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
                VStack(alignment: .leading, spacing: 1) {
                    Text("Jarvis für macOS \(AppInfo.version)")
                    Text("Build \(AppInfo.stand) Uhr")
                }
                .font(.jarvis(10)).foregroundStyle(Farbe.text3)
                Spacer()
            }
            .padding(.bottom, 12).padding(.leading, 2)
        }
        .padding(.horizontal, 14)
        .background(Farbe.tiefe(0.22))
    }
}

/// Die schwebenden Knöpfe nach dem Stapel des Handys: Neue Session, Papierkorb, ein freier Platz, Nachtrag, Programmierung. Ohne eigene Fläche, Papierkorb und Nachtrag nur, wenn sie etwas tun können.
struct SchwebeKnoepfe: View {
    @State private var steuerung = AppSteuerung.shared
    @State private var loeschenZiel: Gespraech?
    @State private var sessionBlitz = false

    var body: some View {
        VStack(spacing: 10) {
            SchwebeKnopf(symbol: "plus.bubble.fill", hilfe: "Neue Session", leuchtet: sessionBlitz) {
                steuerung.neueSession()
                sessionBlitz = true
                Task { try? await Task.sleep(for: .milliseconds(450)); sessionBlitz = false }
            }
            if let g = steuerung.gespraech, !g.nachrichten.isEmpty {
                SchwebeKnopf(symbol: "trash.fill", hilfe: "Gespräch löschen") { loeschenZiel = g }
            }
            Color.clear.frame(width: 38, height: 14)
            if steuerung.nachtragMoeglich {
                SchwebeKnopf(symbol: "plus", hilfe: "Nachtrag in die Programmier-Sitzung dieses Gesprächs", leuchtet: steuerung.nachtragAktiv) {
                    steuerung.nachtragUmschalten()
                }
            }
            SchwebeKnopf(symbol: "chevron.left.forwardslash.chevron.right", hilfe: "Neuer Programmierauftrag", leuchtet: steuerung.programmiererAktiv) {
                steuerung.programmiererUmschalten()
            }
        }
        .alert("Gespräch löschen?", isPresented: Binding(get: { loeschenZiel != nil }, set: { if !$0 { loeschenZiel = nil } }), presenting: loeschenZiel) { g in
            Button("Löschen", role: .destructive) {
                // Das Ziel steht seit dem Klick fest: Hat das Gespräch inzwischen gewechselt, bleibt die offene Ansicht stehen.
                GespraechsSpeicher.shared.loeschen(g)
                if steuerung.gespraech === g {
                    steuerung.gespraech = nil
                    if steuerung.bereich == .gespraech { steuerung.bereich = .start }
                }
            }
            Button("Abbrechen", role: .cancel) {}
        } message: { _ in
            Text("Möchtest du dieses Gespräch wirklich löschen? Es wird endgültig entfernt und erscheint nicht mehr unter Gespräche.")
        }
    }
}

private struct SchwebeKnopf: View {
    let symbol: String
    let hilfe: String
    var leuchtet = false
    let aktion: () -> Void
    @State private var schwebt = false

    var body: some View {
        Button(action: aktion) {
            Image(systemName: symbol).font(.system(size: 14, weight: .semibold))
                .foregroundStyle(leuchtet ? Color.black : Farbe.text2)
                .frame(width: 38, height: 38)
                .background(Circle().fill(leuchtet ? AnyShapeStyle(Farbe.gruen) : AnyShapeStyle(Farbe.glas(schwebt ? 0.13 : 0.05))))
                .glassEffect(.regular, in: .circle)
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .help(hilfe)
        .onHover { schwebt = $0 }
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
