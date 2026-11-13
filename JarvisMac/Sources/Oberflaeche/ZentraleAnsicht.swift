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
    /// Gesetzt, wenn der letzte Abruf gar nichts brachte; ein Erfolg löscht es.
    var letzterFehlschlag: Date?

    /// Lädt unabhängig von der Ansicht, damit ein Wechsel den Abruf nicht abbricht. Nach einem Fehlschlag frühestens nach einer Minute wieder.
    func vorladen() {
        if let f = letzterFehlschlag, Date().timeIntervalSince(f) < 60 { return }
        Task { await laden() }
    }

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
        if ra != nil || rb != nil || rc != nil || rd != nil {
            zuletzt = Date()
            letzterFehlschlag = nil
        } else {
            letzterFehlschlag = Date()
        }
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

    @State private var einst = Einstellungen.shared
    @State private var reihe: [String] = []
    @State private var ziehen = ZiehZustand()
    @State private var kachelReihe: [String] = []
    @State private var kachelZiehen = ZiehZustand()

    /// Die Werks-Reihenfolge wie am Handy oben die Lage, darunter Biomarker und Solarbatterie; neue Karten kommen hinten dazu.
    static let werk = ["mac", "auswertung", "biomarker", "solar", "aufgaben", "termine", "wetter"]

    static func reihenfolge(_ vorhanden: [String]) -> [String] {
        var gespeichert: [String] = []
        for id in Einstellungen.shared.zentraleReihenfolge.split(separator: ",").map({ $0.trimmingCharacters(in: .whitespaces) })
            where vorhanden.contains(id) && !gespeichert.contains(id) { gespeichert.append(id) }
        return gespeichert + werk.filter { vorhanden.contains($0) && !gespeichert.contains($0) } + vorhanden.filter { !gespeichert.contains($0) && !werk.contains($0) }
    }

    private var ausgeblendet: [String] {
        einst.zentraleAusgeblendet.split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
    }

    private func reiheAbgleichen() {
        let aus = ausgeblendet
        reihe = Self.reihenfolge(Self.werk).filter { !aus.contains($0) }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                ZugangsHinweis()
                // Wie die Startbereiche am Handy (Modul M1.2): jede Karte an der ganzen Fläche greifen und verschieben; die Reihenfolge bleibt gespeichert.
                VStack(spacing: 20) {
                    ForEach(reihe, id: \.self) { id in
                        karte(id).ziehbar(id, reihe: $reihe, zustand: ziehen)
                    }
                }
                .ziehStapel(ziehen) { ids in
                    // Ausgeblendete Karten behalten ihren Platz: an ihrer bisherigen Stelle wieder einsetzen.
                    let alt = Self.reihenfolge(Self.werk)
                    var neu = ids
                    for (i, id) in alt.enumerated() where !neu.contains(id) { neu.insert(id, at: min(i, neu.count)) }
                    einst.zentraleReihenfolge = neu.joined(separator: ",")
                    log("oberflaeche", "Zentrale umsortiert: " + ids.joined(separator: ", "), wo: "Zentrale")
                }
                Text("Karten lassen sich per Drag & Drop verschieben.").font(.jarvis(11)).foregroundStyle(Farbe.text3)
            }
            .padding(.horizontal, 28)
            .padding(.vertical, 26)
            .frame(maxWidth: .infinity)
        }
        .scrollIndicators(.never)
        .onAppear {
            reiheAbgleichen()
            daten.vorladen()
            let aus = ausgeblendet
            if einst.biomarkerBeimStart && !aus.contains("biomarker") { Task { await BioDaten.shared.laden() } }
            // Wie am Handy einmal je App-Start von selbst, danach per Knopf oder im eingestellten Takt.
            if einst.solarBeimStart && !aus.contains("solar") && !SolarDaten.shared.automatischGelesen {
                SolarDaten.shared.automatischGelesen = true
                Task { await SolarDaten.shared.lesen() }
            }
        }
        .onChange(of: einst.zentraleReihenfolge) { _, _ in if ziehen.gezogen == nil { reiheAbgleichen() } }
        .onChange(of: einst.zentraleAusgeblendet) { _, _ in reiheAbgleichen() }
    }

    @ViewBuilder private func karte(_ id: String) -> some View {
        switch id {
        case "mac": macKarte
        case "auswertung": auswertungsKarte
        case "biomarker": BiomarkerKarte()
        case "solar": SolarKarte()
        case "aufgaben": listenKarte(symbol: "checklist", titel: "Aufgaben heute", farbe: Farbe.gruen, text: daten.aufgaben)
        case "termine": listenKarte(symbol: "calendar", titel: "Termine", farbe: Farbe.gold, text: daten.termine)
        default: listenKarte(symbol: "cloud.sun.fill", titel: "Wetter", farbe: Farbe.warnung, text: daten.wetter)
        }
    }

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
                    MarkdownText(text: Self.aufbereiten(text), groesse: 13).frame(maxWidth: .infinity, alignment: .leading)
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

    private static let kacheln: [(symbol: String, titel: String)] = [
        ("terminal.fill", "Programmier-Session"), ("arrow.triangle.2.circlepath", "Updates prüfen"), ("eye.fill", "Bildschirm ansehen"),
        ("gauge.with.dots.needle.67percent", "Mac-Status"), ("bubble.left.and.bubble.right.fill", "ChatGPT öffnen"), ("square.stack.3d.up.fill", "OpenLauncher"),
    ]

    @ViewBuilder private func kachel(_ titel: String) -> some View {
        let symbol = Self.kacheln.first { $0.titel == titel }?.symbol ?? "square"
        switch titel {
        case "Programmier-Session":
            MacKachel(symbol: symbol, titel: titel, farbe: Farbe.haupt) {
                steuerung.neuesGespraech(); steuerung.entwurf = "Starte eine OpenLauncher-Session mit Opus 5.5 für folgende Aufgabe: "
            }
        case "Updates prüfen":
            MacKachel(symbol: symbol, titel: titel, farbe: Farbe.gruen) {
                steuerung.gespraech = nil; steuerung.sende("Prüfe in der Updater-Zentrale, welche Programme auf dem Mac ein Update brauchen, und sag mir das Ergebnis.")
            }
        case "Bildschirm ansehen":
            MacKachel(symbol: symbol, titel: titel, farbe: Farbe.gold) {
                steuerung.gespraech = nil; steuerung.sende("Schau dir meinen Bildschirm an und sag mir kurz, was du siehst.")
            }
        case "Mac-Status":
            MacKachel(symbol: symbol, titel: titel, farbe: Farbe.warnung) {
                steuerung.gespraech = nil; steuerung.sende("Wie geht es meinem Mac? Kurzer Status.")
            }
        case "ChatGPT öffnen":
            MacKachel(symbol: symbol, titel: titel, farbe: Farbe.gruen) { Task { _ = await Shell.zsh("open -a ChatGPT") } }
        default:
            MacKachel(symbol: symbol, titel: titel, farbe: Farbe.haupt) { Task { _ = await Shell.zsh("open -a OpenLauncher") } }
        }
    }

    private var macKarte: some View {
        GlasKarte {
            VStack(alignment: .leading, spacing: 14) {
                KartenTitel(symbol: "macbook", titel: "Am Mac", farbe: Farbe.kupfer)
                // Auch die Kacheln lassen sich per Drag & Drop umsortieren; neue kommen hinten dazu.
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 190), spacing: 10)], spacing: 10) {
                    ForEach(kachelReihe, id: \.self) { t in kachel(t).ziehbar(t, reihe: $kachelReihe, zustand: kachelZiehen, art: .url) }
                }
                .ziehStapel(kachelZiehen, art: .url) { ids in UserDefaults.standard.set(ids.joined(separator: "|"), forKey: "zentrale.mac.reihenfolge") }
            }
        }
        .onAppear {
            let gespeichert = (UserDefaults.standard.string(forKey: "zentrale.mac.reihenfolge") ?? "").split(separator: "|").map(String.init)
            let alle = Self.kacheln.map(\.titel)
            kachelReihe = gespeichert.filter { alle.contains($0) } + alle.filter { !gespeichert.contains($0) }
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
        // Kein Button: Er schluckte das Drücken, und die Kachel ließe sich nicht per Drag & Drop verschieben.
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
        .contentShape(Rectangle())
        .onTapGesture { log("oberflaeche", "Funktionsboard: \(titel) gestartet", wo: "Zentrale/Am Mac"); aktion() }
        // Kein Button (der schluckte das Ziehen), für Tastatur und VoiceOver aber weiter als Knopf erkennbar und auslösbar.
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isButton)
        .accessibilityAction { log("oberflaeche", "Funktionsboard: \(titel) gestartet", wo: "Zentrale/Am Mac"); aktion() }
        .onHover { schwebt = $0 }
        .animation(.spring(duration: 0.25), value: schwebt)
    }
}
