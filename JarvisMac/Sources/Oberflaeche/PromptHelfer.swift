import AppKit
import SwiftUI

/// Mehrere Aufgaben in einem Text: Trenner ist genau „ ; “ (Leerzeichen, Semikolon, Leerzeichen). Andere Semikolons gehören zum Text.
@MainActor
enum AufgabenText {
    static let trenner = " ; "

    /// Hängt eine neue Einsprechung an: Zwischen den Einsprechungen und am Ende steht immer der Trenner.
    static func anhaengen(_ basis: String, _ neu: String) -> String {
        let n = ohneEndTrenner(neu)
        guard !n.isEmpty else { return basis }
        let b = ohneEndTrenner(basis)
        return (b.isEmpty ? "" : b + trenner) + n + trenner
    }

    private static func ohneEndTrenner(_ t: String) -> String {
        var s = t.trimmingCharacters(in: .whitespacesAndNewlines)
        while s == ";" || s.hasSuffix(" ;") { s = String(s.dropLast()).trimmingCharacters(in: .whitespacesAndNewlines) }
        return s
    }

    /// Verbessert jede Aufgabe einzeln und setzt die Trenner wieder dazwischen, auch am Ende, wenn er dort stand.
    /// Misslingt eine Aufgabe, bleibt ihr Text unverändert; zurück kommt, wie viele das waren.
    static func verbessern(_ text: String) async -> (text: String, fehlgeschlagen: Int) {
        let getrimmt = text.trimmingCharacters(in: .whitespacesAndNewlines)
        let hatEndTrenner = getrimmt == ";" || getrimmt.hasSuffix(" ;")
        let teile = ohneEndTrenner(getrimmt).components(separatedBy: trenner)
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty }
        guard !teile.isEmpty else { return (text, 0) }
        var fertig = teile
        var fehlgeschlagen = 0
        // Höchstens vier gleichzeitig: Ein Prompt mit dreißig Aufgaben liefe sonst in die Ratenbegrenzung.
        for start in stride(from: 0, to: teile.count, by: 4) {
            await withTaskGroup(of: (Int, String?).self) { gruppe in
                for i in start..<min(start + 4, teile.count) {
                    let teil = teile[i]
                    gruppe.addTask { @MainActor in (i, await Kurzauftrag.frage(teil, system: anweisung)) }
                }
                for await (i, antwort) in gruppe {
                    // Das Modell darf aus einer Aufgabe nie zwei machen: ein versehentlicher Trenner im Ergebnis wird zum gewöhnlichen Semikolon.
                    let a = ohneEndTrenner(antwort ?? "").replacingOccurrences(of: trenner, with: "; ")
                    if a.isEmpty { fehlgeschlagen += 1 } else { fertig[i] = a }
                }
            }
        }
        return (fertig.joined(separator: trenner) + (hatEndTrenner ? trenner : ""), fehlgeschlagen)
    }

    private static let anweisung = """
    Du bringst genau eine Aufgabe aus einem Prompt von Frank in gutes Deutsch. Ein Prompt ist ein vorformulierter Auftrag, den Frank später an seinen Assistenten Jarvis \
    (oft weiter an einen Programmierer) schickt. Frank hat ihn eingesprochen oder eingetippt. Erfasse den Inhalt sinngemäß und schreibe ihn als klaren, vollständigen, \
    für sich allein verständlichen Auftrag. Behalte jeden inhaltlichen Punkt, jeden Namen, jede Zahl und jede Anforderung, füge nichts hinzu und deute nichts um. \
    Lass Füllwörter, Wiederholungen und Selbstkorrekturen weg und berichtige Hörfehler der Spracherkennung. Absätze oder Aufzählungen nur, wenn sie den Auftrag klarer machen. \
    Führe den Auftrag nicht aus. Die übrigen Aufgaben des Prompts werden getrennt bearbeitet: Gib genau diese eine Aufgabe zurück und verwende nie die Zeichenfolge Leerzeichen, Semikolon, Leerzeichen. \
    Antworte nur mit dem fertigen Text, ohne Anführungszeichen, ohne Einleitung und ohne Erklärung.
    """
}

/// Mikrofon für ein Textfeld: Jede Einsprechung endet mit dem Trenner, damit die nächste folgen kann.
struct DiktatKnopf: View {
    @Binding var text: String
    /// Pegel und erkannter Text neben dem Knopf; die Eingabeblase zeigt sie selbst darüber.
    var vorschau = true
    var meldeLaeuft: (Bool) -> Void = { _ in }
    var meldeFehler: (String) -> Void = { _ in }
    @State private var diktat = Diktat.shared
    @State private var laeuft = false
    @State private var meldung = ""

    var body: some View {
        HStack(spacing: 8) {
            RundKnopf(symbol: laeuft && diktat.aktiv ? "stop.fill" : "mic.fill", hilfe: "Einsprechen", aktiv: laeuft && diktat.aktiv, farbe: Farbe.gruen) { druecken() }
            if vorschau && laeuft && diktat.aktiv {
                PegelBalken(pegel: diktat.pegel)
                Text(diktat.vorschau.isEmpty ? "Ich höre zu …" : diktat.vorschau).font(.jarvis(12)).foregroundStyle(Farbe.text2).lineLimit(1)
            }
            if vorschau && !meldung.isEmpty { Text(meldung).font(.jarvis(12)).foregroundStyle(Farbe.warnung).lineLimit(2) }
        }
        .onDisappear { if laeuft { diktat.stoppen() } }
    }

    private func druecken() {
        if laeuft { diktat.stoppen(); return }
        // Ein Diktat, das jemand anderes gestartet hat (Sprachgespräch, Chat), bleibt unangetastet.
        guard !diktat.aktiv else { melde("Das Mikrofon wird gerade schon benutzt."); return }
        laeuft = true; melde(""); meldeLaeuft(true)
        Task {
            if let erkannt = await diktat.hoereBisStille(maxSekunden: 120) {
                text = AufgabenText.anhaengen(text, erkannt)
            } else if let f = diktat.fehler {
                melde(f)
            }
            laeuft = false; meldeLaeuft(false)
        }
    }

    private func melde(_ t: String) { meldung = t; meldeFehler(t) }
}

/// Mikrofon, KI-Verbesserung je Aufgabe und Rückgängig für einen Prompt.
struct PromptWerkzeuge: View {
    @Binding var text: String
    @State private var vorher: String?
    @State private var eigenerStand: String?
    @State private var arbeitet = false
    @State private var meldung = ""

    var body: some View {
        HStack(spacing: 10) {
            DiktatKnopf(text: $text)
            KapselKnopf(symbol: "sparkles", titel: arbeitet ? "Jarvis verbessert …" : "Mit KI verbessern") {
                guard !arbeitet, !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
                let alt = text
                arbeitet = true; meldung = ""
                Task {
                    let (neu, fehlgeschlagen) = await AufgabenText.verbessern(alt)
                    arbeitet = false
                    if fehlgeschlagen > 0 { meldung = "\(fehlgeschlagen) Aufgabe(n) konnten nicht verbessert werden und blieben unverändert." }
                    // Hat Frank währenddessen weitergeschrieben oder diktiert, bleibt sein Text stehen.
                    if text != alt {
                        meldung = "Du hast währenddessen weitergeschrieben, deshalb bleibt dein Text unverändert."
                        log("ablage", "Prompt-Verbesserung verworfen, Text wurde währenddessen geändert", wo: "Ablage/Prompts")
                    } else if neu != alt {
                        vorher = alt; eigenerStand = neu; text = neu
                        log("ablage", "Prompt verbessert (\(fehlgeschlagen) Aufgaben unverändert)", wo: "Ablage/Prompts")
                    }
                }
            }
            if arbeitet { ProgressView().controlSize(.small) }
            if let v = vorher {
                KapselKnopf(symbol: "arrow.uturn.backward", titel: "Rückgängig") { text = v; vorher = nil }
            }
            if !meldung.isEmpty { Text(meldung).font(.jarvis(12)).foregroundStyle(Farbe.warnung) }
        }
        // Rückgängig gilt nur für den Stand direkt nach der KI; danach würde es spätere Änderungen löschen.
        .onChange(of: text) { _, neu in if neu != eigenerStand { vorher = nil } }
    }
}

/// Einen Prompt bearbeiten: deckt das ganze Fenster ab und schaltet es für diese Zeit in den Vollbildmodus von macOS.
struct PromptEditorVollbild: View {
    let prompt: PromptEintrag
    @State private var steuerung = AppSteuerung.shared
    @State private var text: String
    @State private var fehler = ""
    @State private var speichert = false
    @State private var frageVerwerfen = false
    @State private var vollbildGeschaltet = false
    /// Der Prompt wurde inzwischen geändert oder gelöscht: Der nächste Klick auf Speichern legt ihn neu an.
    @State private var alsNeu = false
    @FocusState private var fokus: Bool

    init(prompt: PromptEintrag) {
        self.prompt = prompt
        _text = State(initialValue: prompt.text)
    }

    private var geaendert: Bool { prompt.nummer > 0 && text != prompt.text }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 12) {
                Text(prompt.nummer > 0 ? "Prompt \(prompt.nummer) bearbeiten" : "Neuer Prompt").font(.jarvis(24, .bold))
                Spacer()
                Text("\(text.count) Zeichen").font(.jarvis(12)).foregroundStyle(Farbe.text3)
                KapselKnopf(symbol: "xmark", titel: "Schließen") { if geaendert { frageVerwerfen = true } else { schliessen() } }
                KapselKnopf(symbol: "square.and.arrow.down.fill", titel: speichert ? "Speichert …" : "Speichern", hervorgehoben: true) { speichern() }
                    .keyboardShortcut("s", modifiers: .command)
            }
            PromptWerkzeuge(text: $text)
            if !fehler.isEmpty { Text(fehler).font(.jarvis(13)).foregroundStyle(Farbe.warnung) }
            TextEditor(text: $text).font(.jarvis(17)).scrollContentBackground(.hidden).focused($fokus)
                .padding(14)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(RoundedRectangle(cornerRadius: 16).fill(Farbe.glas(0.05)))
        }
        .padding(.horizontal, 36).padding(.top, 40).padding(.bottom, 28)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Farbe.grund)
        .onAppear { fokus = true; vollbildGeschaltet = Self.vollbild(an: true) }
        .onDisappear {
            guard vollbildGeschaltet else { return }
            _ = Self.vollbild(an: false)
            // Während der Einblend-Animation ignoriert macOS das Umschalten: dann noch einmal, sobald sie durch ist.
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) { MainActor.assumeIsolated { _ = Self.vollbild(an: false) } }
        }
        .confirmationDialog("Deine Änderungen an diesem Prompt verwerfen?", isPresented: $frageVerwerfen) {
            Button("Verwerfen", role: .destructive) { schliessen() }
        }
    }

    /// Ein neuer Prompt geht ohne Speichern zurück ins Feld unter „Neuer Prompt“, nichts davon ist verloren.
    private func schliessen(gespeichert: Bool = false) {
        if prompt.nummer == 0 { steuerung.promptEntwurf = gespeichert ? "" : text }
        steuerung.promptBearbeiten = nil
    }

    private func speichern() {
        guard !speichert else { return }
        speichert = true; fehler = ""
        Task {
            defer { speichert = false }
            var a: JSONObjekt = ["aktion": "speichern", "text": text]
            if prompt.nummer > 0 && !alsNeu {
                // Die Nummer ist nur der Platz in der Liste: Nach dem Löschen eines anderen Prompts rückt alles auf. Deshalb am Text wiederfinden.
                guard let liste = await McpKlient.shared.text("prompts_lesen") else { fehler = "Das Handy ist gerade nicht erreichbar, nichts wurde gespeichert."; return }
                guard let aktuell = PromptEintrag.zerlege(liste).first(where: { $0.text == prompt.text }) else {
                    fehler = "Dieser Prompt wurde inzwischen geändert oder gelöscht. Speichern legt deine Fassung als neuen Prompt an."
                    alsNeu = true
                    return
                }
                a = ["aktion": "aendern", "text": text, "nummer": aktuell.nummer]
            }
            let r = await McpKlient.shared.rufe("prompt_speichern", a)
            if r.fehler { fehler = r.text; return }
            steuerung.promptStand += 1
            schliessen(gespeichert: true)
        }
    }

    /// true = der Zustand wurde tatsächlich umgeschaltet; nur dann stellt das Schließen ihn wieder her.
    private static func vollbild(an: Bool) -> Bool {
        guard let w = NSApp.windows.first(where: { $0.title == "Jarvis" && $0.canBecomeMain }) ?? NSApp.mainWindow else { return false }
        guard w.styleMask.contains(.fullScreen) != an else { return false }
        w.collectionBehavior.insert(.fullScreenPrimary)
        w.toggleFullScreen(nil)
        return true
    }
}
