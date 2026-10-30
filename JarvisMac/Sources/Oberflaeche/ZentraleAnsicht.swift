import SwiftUI

@MainActor @Observable
final class ZentraleDaten {
    static let shared = ZentraleDaten()
    var auswertung: String?
    var aufgaben: String?
    var termine: String?
    var wetter: String?
    var laedt = false
    var zuletzt: Date?

    func laden(erzwingen: Bool = false) async {
        if laedt { return }
        if !erzwingen, let z = zuletzt, Date().timeIntervalSince(z) < 300 { return }
        laedt = true
        defer { laedt = false }
        let f = DateFormatter(); f.dateFormat = "yyyy-MM-dd"
        let bis = f.string(from: Date().addingTimeInterval(2 * 86400))
        async let a = McpKlient.shared.text("tagesauswertung_lesen", [:])
        async let b = McpKlient.shared.text("aufgaben_lesen", ["bereich": "heute"])
        async let c = McpKlient.shared.text("kalender_lesen", ["von": "heute", "bis": bis])
        async let d = McpKlient.shared.text("wetter_lesen", ["datum": "heute", "tage": 2])
        let (ra, rb, rc, rd) = await (a, b, c, d)
        // Was gerade nicht kam, bleibt stehen; nur ein Erfolg sperrt das erneute Laden für fünf Minuten.
        auswertung = ra.map(Self.kuerzeAuswertung) ?? auswertung
        aufgaben = rb ?? aufgaben; termine = rc ?? termine; wetter = rd ?? wetter
        if ra != nil || rb != nil || rc != nil || rd != nil { zuletzt = Date() }
    }

    /// Nur der Teil für Frank (ohne den angehängten Regel- und Gedächtnisblock).
    static func kuerzeAuswertung(_ t: String) -> String {
        var s = t
        for marke in ["\nFRANKS REGELN", "\n--- SEIT DIESER", "\nGESAMTBILD FEHLT"] {
            if let r = s.range(of: marke) { s = String(s[..<r.lowerBound]) }
        }
        return s.trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

struct ZentraleAnsicht: View {
    @State private var daten = ZentraleDaten.shared
    @State private var steuerung = AppSteuerung.shared
    @State private var anhang: Data?
    @State private var auswertungOffen = false
    @State private var neuLaeuft = false
    @State private var neuFehler = false

    static var gruss: String {
        let h = Calendar.current.component(.hour, from: Date())
        switch h {
        case 5..<11: return "Guten Morgen, Frank."
        case 11..<17: return "Hallo Frank."
        case 17..<22: return "Guten Abend, Frank."
        default: return "Noch wach, Frank?"
        }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                ZugangsHinweis()
                macKarte
                VStack(spacing: 10) {
                    held
                    schnellVerknuepfungen
                }
                .frame(maxWidth: 640)
                HStack(alignment: .top, spacing: 18) {
                    auswertungsKarte.frame(maxWidth: .infinity)
                    listenKarte(symbol: "checklist", titel: "Aufgaben heute", farbe: Farbe.gruen, text: daten.aufgaben)
                        .frame(width: 360)
                }
                HStack(alignment: .top, spacing: 18) {
                    listenKarte(symbol: "calendar", titel: "Termine", farbe: Farbe.gold, text: daten.termine)
                        .frame(maxWidth: .infinity)
                    listenKarte(symbol: "cloud.sun.fill", titel: "Wetter", farbe: Farbe.warnung, text: daten.wetter)
                        .frame(width: 360)
                }
            }
            .padding(.horizontal, 28)
            .padding(.vertical, 26)
            .frame(maxWidth: .infinity)
        }
        .scrollIndicators(.never)
        .task {
            await daten.laden()
        }
    }

    /// Kompakte Kopfzeile: Kugel links neben der Begrüßung, darunter das Eingabefeld.
    private var held: some View {
        VStack(spacing: 14) {
            HStack(spacing: 22) {
                Button { steuerung.kuerzelSprechen() } label: {
                    KugelLive(groesse: 96)
                }
                .buttonStyle(.plain)
                .help("Antippen zum Sprechen (⌥⌘J überall)")
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 10) {
                        LivePunkt()
                        Text(Self.gruss).font(.jarvis(30, .bold))
                        .foregroundStyle(LinearGradient(colors: [Farbe.text, Farbe.haupt], startPoint: .leading, endPoint: .trailing))
                    }
                    Text(Self.datumZeile).font(.jarvis(14, .medium)).foregroundStyle(Farbe.text2)
                }
            }
            EingabeBlase(gespraech: nil, anhang: $anhang, zweizeilig: true)
        }
    }

    private var schnellVerknuepfungen: some View {
        VStack(spacing: 8) {
            ForEach(Self.vorschlaege, id: \.self) { v in
                KapselKnopf(symbol: "arrow.turn.down.right", titel: v, voll: true) { steuerung.gespraech = nil; steuerung.sende(v) }
            }
        }
    }

    static let vorschlaege = ["Wie ist mein Tag?", "Was steht heute an?", "Wie habe ich geschlafen?", "Prüfe meine Updates"]

    static var datumZeile: String {
        let f = DateFormatter(); f.locale = Locale(identifier: "de_DE"); f.dateFormat = "EEEE, d. MMMM yyyy"
        return f.string(from: Date())
    }

    private var auswertungsKarte: some View {
        GlasKarte {
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    KartenTitel(symbol: "sun.horizon.fill", titel: "Tagesauswertung", farbe: Farbe.haupt)
                    Spacer()
                    if daten.laedt { ProgressView().controlSize(.small) }
                    RundKnopf(symbol: "speaker.wave.2.fill", hilfe: "Vorlesen", groesse: 30) {
                        if let a = daten.auswertung { Task { await Stimme.shared.sprich(a) } }
                    }
                    RundKnopf(symbol: "arrow.clockwise", hilfe: "Neu laden", groesse: 30) { Task { await daten.laden(erzwingen: true) } }
                    RundKnopf(symbol: "sparkles", hilfe: "Neue Auswertung erstellen lassen (1–3 Minuten)", aktiv: neuLaeuft, groesse: 30) {
                        guard !neuLaeuft else { return }
                        neuLaeuft = true
                        Task {
                            let t = await McpKlient.shared.text("tagesauswertung_lesen", ["neu_erstellen": true], sekunden: 240)
                            if let t { daten.auswertung = ZentraleDaten.kuerzeAuswertung(t) } else { neuFehler = true }
                            neuLaeuft = false
                        }
                    }
                }
                if neuLaeuft { Text("Jarvis schreibt eine neue Auswertung – das dauert ein bis drei Minuten …").font(.jarvis(12)).foregroundStyle(Farbe.haupt) }
                if neuFehler { Text("Die neue Auswertung kam nicht an – das Handy war nicht erreichbar oder brauchte zu lange.").font(.jarvis(12)).foregroundStyle(Farbe.warnung) }
                if let a = daten.auswertung {
                    MarkdownText(text: auswertungOffen ? a : Self.anfang(a), groesse: 14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Button(auswertungOffen ? "Weniger anzeigen" : "Ganze Auswertung lesen") {
                        withAnimation(.spring(duration: 0.35)) { auswertungOffen.toggle() }
                    }
                    .buttonStyle(.plain).font(.jarvis(13, .semibold)).foregroundStyle(Farbe.haupt)
                } else if !daten.laedt {
                    Text("Das Handy-Jarvis ist gerade nicht erreichbar – sobald es online ist, steht hier deine Tagesauswertung.")
                        .font(.jarvis(14)).foregroundStyle(Farbe.text2)
                } else {
                    Text("Lade deine Tagesauswertung …").font(.jarvis(14)).foregroundStyle(Farbe.text2)
                }
            }
        }
    }

    static func anfang(_ t: String) -> String {
        let absaetze = t.components(separatedBy: "\n\n")
        return absaetze.prefix(3).joined(separator: "\n\n")
    }

    private func listenKarte(symbol: String, titel: String, farbe: Color, text: String?) -> some View {
        GlasKarte {
            VStack(alignment: .leading, spacing: 12) {
                KartenTitel(symbol: symbol, titel: titel, farbe: farbe)
                if let text {
                    MarkdownText(text: Self.aufbereiten(text), groesse: 13)
                } else {
                    Text(daten.laedt ? "Lade …" : "Nicht verfügbar").font(.jarvis(13)).foregroundStyle(Farbe.text3)
                }
            }
        }
    }

    /// Werkzeugtexte etwas lesbarer: interne Kennungen raus.
    static func aufbereiten(_ t: String, grenze: Int = 1600) -> String {
        var s = t.replacingOccurrences(of: #"\[id \d+\] "#, with: "", options: .regularExpression)
        s = McpKlient.ohneKontextHinweis(s)
        if s.count > grenze { s = String(s.prefix(grenze)) + " …" }
        return s
    }

    private var macKarte: some View {
        GlasKarte {
            VStack(alignment: .leading, spacing: 14) {
                KartenTitel(symbol: "macbook", titel: "Am Mac", farbe: Farbe.kupfer)
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 190), spacing: 10)], spacing: 10) {
                    MacKachel(symbol: "terminal.fill", titel: "Programmier-Session", farbe: Farbe.haupt) {
                        steuerung.neuesGespraech(); steuerung.entwurf = "Starte eine OpenLauncher-Session mit Opus 5.5 für folgende Aufgabe: "
                    }
                    MacKachel(symbol: "arrow.triangle.2.circlepath", titel: "Updates prüfen", farbe: Farbe.gruen) {
                        steuerung.gespraech = nil; steuerung.sende("Prüfe in der Updater-Zentrale, welche Programme auf dem Mac ein Update brauchen, und sag mir das Ergebnis.")
                    }
                    MacKachel(symbol: "eye.fill", titel: "Bildschirm ansehen", farbe: Farbe.gold) {
                        steuerung.gespraech = nil; steuerung.sende("Schau dir meinen Bildschirm an und sag mir kurz, was du siehst.")
                    }
                    MacKachel(symbol: "gauge.with.dots.needle.67percent", titel: "Mac-Status", farbe: Farbe.warnung) {
                        steuerung.gespraech = nil; steuerung.sende("Wie geht es meinem Mac? Kurzer Status.")
                    }
                    MacKachel(symbol: "bubble.left.and.bubble.right.fill", titel: "ChatGPT öffnen", farbe: Farbe.gruen) {
                        Task { _ = await Shell.zsh("open -a ChatGPT") }
                    }
                    MacKachel(symbol: "square.stack.3d.up.fill", titel: "OpenLauncher", farbe: Farbe.haupt) {
                        Task { _ = await Shell.zsh("open -a OpenLauncher") }
                    }
                }
            }
        }
    }
}

/// Fehlt Jarvis jeder Zugang zu einem Modell, sagt das der erste Bildschirm.
struct ZugangsHinweis: View {
    @State private var zugangDa = true
    var body: some View {
        Group {
            if !zugangDa {
                GlasKarte {
                    HStack(spacing: 14) {
                        Image(systemName: "key.horizontal.fill").font(.system(size: 20)).foregroundStyle(Farbe.warnung)
                        VStack(alignment: .leading, spacing: 3) {
                            Text("Noch ein Schritt: Jarvis braucht einen Kopf").font(.jarvis(15, .semibold))
                            Text("Melde dich einmal mit deinem ChatGPT-Konto an (läuft über dein Abo) oder trage einen Anthropic-Schlüssel ein.")
                                .font(.jarvis(12)).foregroundStyle(Farbe.text2)
                        }
                        Spacer()
                        KapselKnopf(symbol: "arrow.right", titel: "Zu den Konten", hervorgehoben: true) {
                            EinstellungenAnsicht.startReiter = "konten"; AppSteuerung.shared.bereich = .einstellungen
                        }
                    }
                }
            }
        }
        .task { zugangDa = await Einstellungen.shared.gehirnZugang() }
    }
}

struct MacKachel: View {
    let symbol: String
    let titel: String
    let farbe: Color
    let aktion: () -> Void
    @State private var schwebt = false
    var body: some View {
        Button { log("oberflaeche", "Funktionsboard: \(titel) gestartet", wo: "Zentrale/Am Mac"); aktion() } label: {
            HStack(spacing: 10) {
                Image(systemName: symbol).font(.system(size: 15, weight: .semibold)).foregroundStyle(farbe)
                    .frame(width: 34, height: 34).background(RoundedRectangle(cornerRadius: 10).fill(farbe.opacity(0.13)))
                Text(titel).font(.jarvis(13, .semibold)).foregroundStyle(Farbe.text).lineLimit(1)
                Spacer(minLength: 0)
            }
            .padding(10)
            .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(Farbe.glas(schwebt ? 0.09 : 0.04)))
            .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).strokeBorder(Farbe.glas(schwebt ? 0.14 : 0.06)))
            .scaleEffect(schwebt ? 1.015 : 1)
        }
        .buttonStyle(.plain)
        .onHover { schwebt = $0 }
        .animation(.spring(duration: 0.25), value: schwebt)
    }
}
