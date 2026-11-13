import AppKit
import SwiftUI

/// Auftrag an das Modell für „Mit KI verbessern“, wörtlich wie ERINNERUNG_KI am Handy.
private let erinnerungKi = "Du überarbeitest eine von Frank eingesprochene oder eingetippte Erinnerung. Antworte nur mit JSON: {\"text\":\"…\",\"kurz\":\"…\",\"zeile\":\"…\"}. " +
    "text: der vollständige Inhalt in gutem Deutsch; behalte jeden Punkt, jeden Namen, jede Zahl, füge nichts hinzu, berichtige Hörfehler, lass Füllwörter weg. " +
    "kurz: Kurzfassung zum Vorlesen, höchstens 35 Wörter, für sich allein gut verständlich, ohne Anrede und ohne Datum. " +
    "zeile: etwa sechs Wörter, die man in einer Liste sofort erkennt, ohne Punkt und ohne Datum. Führe nichts aus."

/// Was der Editor bearbeitet; kennung nil = neue Erinnerung.
struct ErinnerungEntwurf: Identifiable, Equatable {
    let id: UUID
    var kennung: String?
    var allgemein: Bool
    var text = ""
    var kurz = ""
    var zeile = ""
    var datum: String?
    var uhrzeit: String?
    var form = Erinnerung.formMeldung

    init(allgemein: Bool) {
        id = UUID()
        self.allgemein = allgemein
    }

    init(_ e: Erinnerung) {
        id = UUID()
        kennung = e.id
        allgemein = e.allgemein
        text = e.text; kurz = e.kurz; zeile = e.zeile
        datum = e.datum; uhrzeit = e.uhrzeit; form = e.form
    }
}

/// Auswahlfeld wie die Chips am Handy, nur für den Reiter Erinnerung.
private struct WahlChip: View {
    let titel: String
    let aktiv: Bool
    let aktion: () -> Void
    var body: some View {
        Button(action: aktion) {
            Text(titel).font(.jarvis(12, .semibold)).foregroundStyle(aktiv ? Farbe.haupt : Farbe.text2)
                .padding(.horizontal, 12).padding(.vertical, 7)
                .background(Capsule().fill(aktiv ? Farbe.haupt.opacity(0.15) : Farbe.glas(0.06)))
                .overlay(Capsule().strokeBorder(aktiv ? Farbe.haupt.opacity(0.6) : Farbe.glas(0.10)))
                .contentShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Liste

/// Bereich „Erinnerung“ der Ablage: Unterreiter „Mit Datum“ (das Nächste oben, das Älteste unten) und „Allgemein“.
struct ErinnerungenAnsicht: View {
    @State private var abgleich = AblageAbgleich.shared
    @State private var steuerung = AppSteuerung.shared
    @State private var allgemein = false

    var body: some View {
        let jetzt = Int64(Date().timeIntervalSince1970 * 1000)
        let liste = allgemein ? Erinnerung.allgemeine(abgleich.erinnerungen) : Erinnerung.datiert(abgleich.erinnerungen, jetzt: jetzt)
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 8) {
                WahlChip(titel: "Mit Datum", aktiv: !allgemein) { allgemein = false }
                WahlChip(titel: "Allgemein", aktiv: allgemein) { allgemein = true }
                Spacer()
                RundKnopf(symbol: "plus", hilfe: "Neue Erinnerung", aktiv: true) { steuerung.erinnerungEntwurf = ErinnerungEntwurf(allgemein: allgemein) }
            }
            if liste.isEmpty {
                Text(allgemein ? "Keine allgemeinen Erinnerungen. Sag Jarvis: „Erinnere mich später an …“" : "Keine Erinnerungen mit Datum. Klick auf +, oder sag Jarvis: „Erinnere mich am 12. August daran, …“")
                    .font(.jarvis(14)).foregroundStyle(Farbe.text2)
            }
            LazyVStack(alignment: .leading, spacing: 10) {
                ForEach(liste) { e in zeile(e, jetzt: jetzt) }
            }
        }
    }

    private func zeile(_ e: Erinnerung, jetzt: Int64) -> some View {
        let vorbei = !allgemein && (e.erledigt != 0 || (e.termin ?? 0) < jetzt)
        let oben = allgemein ? (e.erledigt != 0 ? "Erledigt" : "Ohne festen Termin") : e.datumAnzeige + " · " + (e.wecker ? "Wecker" : "Erinnerung") + (e.erledigt != 0 ? " · erledigt" : "")
        return Button { steuerung.erinnerungOffen = e.id } label: {
            GlasKarte(abstand: 14) {
                VStack(alignment: .leading, spacing: 3) {
                    Text(oben).font(.jarvis(12, .semibold)).foregroundStyle(vorbei ? Farbe.text3 : Farbe.haupt).lineLimit(1)
                    Text(e.zeile).font(.jarvis(15)).foregroundStyle(vorbei ? Farbe.text3 : Farbe.text).lineLimit(2)
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Ebenen über dem Fenster

struct ErinnerungEbenen: View {
    @State private var steuerung = AppSteuerung.shared
    var body: some View {
        ZStack {
            if let id = steuerung.erinnerungOffen { ErinnerungDetailEbene(id: id).id(id).foregroundStyle(Farbe.text).tint(Farbe.haupt) }
            if let d = steuerung.erinnerungEntwurf { ErinnerungEditorVollbild(entwurf: d).id(d.id).foregroundStyle(Farbe.text).tint(Farbe.haupt) }
        }
    }
}

/// Fällige Erinnerungen über allem, bis sie bestätigt sind; beim Wecker heißt der Knopf „Aus“.
struct ErinnerungBanner: View {
    @State private var planer = ErinnerungPlaner.shared
    @State private var steuerung = AppSteuerung.shared

    var body: some View {
        VStack(spacing: 8) {
            ForEach(planer.anzeige.prefix(4)) { f in
                HStack(spacing: 12) {
                    Image(systemName: f.erinnerung.wecker ? "alarm.fill" : "bell.fill").font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(f.erinnerung.wecker ? Farbe.rot : Farbe.haupt)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(f.erinnerung.zeile).font(.jarvis(14, .semibold)).foregroundStyle(Farbe.text).lineLimit(1)
                        Text(f.erinnerung.kurz).font(.jarvis(12)).foregroundStyle(Farbe.text2).lineLimit(2)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .contentShape(Rectangle())
                    .onTapGesture { steuerung.erinnerungOffen = f.erinnerung.id }
                    KapselKnopf(symbol: f.erinnerung.wecker ? "alarm.waves.left.and.right" : "checkmark", titel: f.erinnerung.wecker ? "Aus" : "Bestätigen", hervorgehoben: true) { planer.bestaetige(f) }
                }
                .padding(.horizontal, 16).padding(.vertical, 12)
                .background(RoundedRectangle(cornerRadius: 16, style: .continuous).fill(Farbe.grund2))
                .overlay(RoundedRectangle(cornerRadius: 16, style: .continuous).strokeBorder((f.erinnerung.wecker ? Farbe.rot : Farbe.haupt).opacity(0.8), lineWidth: 1.5))
            }
            if planer.anzeige.count > 4 {
                Text("und \(planer.anzeige.count - 4) weitere").font(.jarvis(12)).foregroundStyle(Farbe.text2)
            }
        }
        .frame(maxWidth: 680)
        .padding(.top, planer.anzeige.isEmpty ? 0 : 14)
        .padding(.horizontal, 24)
    }
}

// MARK: - Detail

/// Die vollständige Ansicht einer Erinnerung, eine Ebene über dem Fenster.
private struct ErinnerungDetailEbene: View {
    let id: String
    @State private var abgleich = AblageAbgleich.shared
    @State private var planer = ErinnerungPlaner.shared
    @State private var steuerung = AppSteuerung.shared
    @State private var plusOffen = false
    @State private var loeschFrage = false
    @State private var fehler = ""

    var body: some View {
        if let e = abgleich.erinnerungen.first(where: { $0.id == id }) {
            inhalt(e)
        } else {
            Color.clear.onAppear { steuerung.erinnerungOffen = nil }
        }
    }

    private func schliessen() { steuerung.erinnerungOffen = nil }

    private func inhalt(_ e: Erinnerung) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 12) {
                Text(e.allgemein ? "Ohne festen Termin" : e.datumAnzeige + " · " + (e.wecker ? "Wecker" : "Erinnerung")).font(.jarvis(15, .semibold)).foregroundStyle(Farbe.haupt)
                Spacer()
                KapselKnopf(symbol: "xmark", titel: "Schließen") { schliessen() }
            }
            ScrollView {
                VStack(alignment: .leading, spacing: 10) {
                    Text(e.zeile).font(.jarvis(24, .bold)).textSelection(.enabled)
                    ueberschrift("Kurzfassung")
                    Text(e.kurz).font(.jarvis(15)).textSelection(.enabled)
                    ueberschrift("Ausführlich")
                    Text(e.text).font(.jarvis(15)).textSelection(.enabled)
                    if !e.allgemein {
                        ueberschrift("Form")
                        HStack(spacing: 8) {
                            WahlChip(titel: "Benachrichtigung", aktiv: !e.wecker) { aendern(e.id) { $0.form = Erinnerung.formMeldung } }
                            WahlChip(titel: "Wecker", aktiv: e.wecker) { aendern(e.id) { $0.form = Erinnerung.formWecker } }
                        }
                    }
                    ueberschrift(e.allgemein ? "Hinweise" : "Erinnerungszeitpunkte")
                    if e.allgemein && e.hinweisUm == 0 {
                        Text("Jarvis meldet sich von selbst, wenn es zu Dienst, Schlaf und Tag passt.").font(.jarvis(13)).foregroundStyle(Farbe.text2)
                    }
                    // Der Stand ändert sich mit der Uhr, auch ohne dass neue Daten kommen.
                    TimelineView(.periodic(from: Date(), by: 20)) { kontext in
                        zeitpunkte(e, jetzt: Int64(kontext.date.timeIntervalSince1970 * 1000))
                    }
                    if !e.allgemein {
                        RundKnopf(symbol: "plus", hilfe: "Erinnerungszeitpunkt hinzufügen", aktiv: true) { plusOffen = true }
                    }
                    Text("Von \(e.quelle.isEmpty ? "Jarvis Handy" : e.quelle)\(ErinnerungZeit.gespeichert(e.erstellt))").font(.jarvis(11)).foregroundStyle(Farbe.text3).padding(.top, 8)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            if !fehler.isEmpty { Text(fehler).font(.jarvis(13)).foregroundStyle(Farbe.warnung) }
            HStack(spacing: 10) {
                KapselKnopf(symbol: "trash", titel: "Löschen") { loeschFrage = true }
                Spacer()
                KapselKnopf(symbol: "checkmark", titel: e.erledigt != 0 ? "Wieder offen" : "Erledigt") {
                    let neu = e.erledigt != 0 ? Int64(0) : Int64(Date().timeIntervalSince1970 * 1000)
                    aendern(e.id) { $0.erledigt = neu }
                }
                KapselKnopf(symbol: "square.and.pencil", titel: "Bearbeiten", hervorgehoben: true) { steuerung.erinnerungEntwurf = ErinnerungEntwurf(e) }
            }
        }
        .padding(.horizontal, 36).padding(.top, 40).padding(.bottom, 28)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        .background(Farbe.grund)
        .confirmationDialog("Diese Erinnerung auf allen Jarvis-Versionen löschen?", isPresented: $loeschFrage) {
            Button("Löschen", role: .destructive) {
                Task { do { try await abgleich.erinnerungLoeschen(e.id) } catch { fehler = error.localizedDescription } }
            }
        }
        .sheet(isPresented: $plusOffen) {
            AusloesungWahl(erinnerung: e) { a in
                guard let z = e.zeitVon(a), z > Int64(Date().timeIntervalSince1970 * 1000) else { return "Dieser Zeitpunkt ist schon vorbei." }
                if e.doppelt(a) { return "Dieser Zeitpunkt (\(ErinnerungZeit.zeitText(z))) ist schon eingestellt." }
                plusOffen = false
                aendern(e.id) { $0.ausloesungen.append(a) }
                return nil
            }
        }
    }

    private func ueberschrift(_ t: String) -> some View {
        Text(t).font(.jarvis(12)).foregroundStyle(Farbe.text3).padding(.top, 8)
    }

    private func zeitpunkte(_ e: Erinnerung, jetzt: Int64) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(e.termine()) { t in
                let q = planer.quittung(t.quittung)
                HStack(spacing: 10) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(t.anzeige).font(.jarvis(14, .semibold))
                        Text(ErinnerungZeit.zeitText(t.zeit) + " · " + standText(t, quittung: q, jetzt: jetzt)).font(.jarvis(12)).foregroundStyle(q != nil ? Farbe.gruen : Farbe.text2)
                    }
                    Spacer()
                    if q == nil && t.zeit <= jetzt {
                        KapselKnopf(symbol: "checkmark", titel: "Bestätigen") { planer.bestaetige(t) }
                    }
                    if let a = t.ausloesung {
                        KleinKnopf(symbol: "xmark", hilfe: "Zeitpunkt entfernen") { aendern(e.id) { alt in alt.ausloesungen.removeAll { x in x.id == a.id } } }
                    }
                }
            }
        }
    }

    private func standText(_ t: Faellig, quittung q: ErinnerungQuittung?, jetzt: Int64) -> String {
        if let q { return "bestätigt auf \(q.von)" }
        if t.zeit > jetzt { return "geplant" }
        return t.meldbar(jetzt) ? "fällig" : "verpasst"
    }

    private func aendern(_ id: String, _ f: @escaping (inout Erinnerung) -> Void) {
        Task {
            do {
                try await abgleich.erinnerungAendern(id) { alt in
                    var n = alt
                    f(&n)
                    return n
                }
                fehler = ""
            } catch { fehler = error.localizedDescription }
        }
    }
}

/// Auswahl hinter jedem Plus: wie viele Tage oder Stunden vor dem Termin; bei reinem Datum wahlweise mit eigener Uhrzeit.
private struct AusloesungWahl: View {
    let erinnerung: Erinnerung
    /// Gibt zurück, warum der Zeitpunkt nicht geht; nil = übernommen.
    let fertig: (Ausloesung) -> String?
    @Environment(\.dismiss) private var schliessen
    @State private var anzahl = ""
    @State private var inStunden: Bool
    @State private var eigeneUhr = false
    @State private var uhr: Date
    @State private var meldung = ""

    init(erinnerung: Erinnerung, fertig: @escaping (Ausloesung) -> String?) {
        self.erinnerung = erinnerung
        self.fertig = fertig
        _inStunden = State(initialValue: erinnerung.uhrzeit != nil)
        _uhr = State(initialValue: ErinnerungZeit.uhrDatum(erinnerung.ohneUhrzeitUm))
    }

    private var vorgaben: [(String, Int)] {
        let vorn: [(String, Int)] = erinnerung.uhrzeit != nil ? [("Zum Termin", 0), ("1 Stunde", 60), ("3 Stunden", 180)] : [("Am Termintag", 0)]
        let rest: [(String, Int)] = [("1 Tag", 1440), ("2 Tage", 2880), ("1 Woche", 10080), ("2 Wochen", 20160), ("1 Monat", 43200)]
        return vorn + rest
    }

    private func nimm(_ minuten: Int) {
        let u = erinnerung.uhrzeit == nil && eigeneUhr ? ErinnerungZeit.uhrText(uhr) : nil
        meldung = fertig(Erinnerung.neueAusloesung(minuten, uhrzeit: u)) ?? ""
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Wann erinnern?").font(.jarvis(18, .bold))
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 104), spacing: 8)], alignment: .leading, spacing: 8) {
                ForEach(Array(vorgaben.enumerated()), id: \.offset) { _, v in
                    WahlChip(titel: v.0, aktiv: false) { nimm(v.1) }
                }
            }
            HStack(spacing: 8) {
                TextField("Anzahl", text: $anzahl).textFieldStyle(.plain).font(.jarvis(14))
                    .padding(.horizontal, 12).padding(.vertical, 7)
                    .background(RoundedRectangle(cornerRadius: 10).fill(Farbe.glas(0.05)))
                    .onChange(of: anzahl) { _, neu in
                        let sauber = String(neu.filter(\.isNumber).prefix(4))
                        if sauber != neu { anzahl = sauber }
                    }
                if erinnerung.uhrzeit != nil { WahlChip(titel: "Stunden", aktiv: inStunden) { inStunden = true } }
                WahlChip(titel: "Tage", aktiv: !inStunden) { inStunden = false }
            }
            if erinnerung.uhrzeit == nil {
                HStack(spacing: 10) {
                    Toggle(isOn: $eigeneUhr) { Text(eigeneUhr ? "Uhrzeit" : "Uhrzeit: \(erinnerung.ohneUhrzeitUm) (Vorgabe)").font(.jarvis(13)).foregroundStyle(Farbe.text2) }
                    if eigeneUhr {
                        DatePicker("", selection: $uhr, displayedComponents: .hourAndMinute).labelsHidden()
                            .environment(\.locale, Locale(identifier: "de_DE"))
                    }
                }
            }
            if !meldung.isEmpty { Text(meldung).font(.jarvis(13)).foregroundStyle(Farbe.warnung) }
            HStack {
                KapselKnopf(symbol: "xmark", titel: "Abbrechen") { schliessen() }
                Spacer()
                KapselKnopf(symbol: "plus", titel: "Hinzufügen", hervorgehoben: true) {
                    guard let n = Int(anzahl) else { return }
                    nimm(inStunden ? n * 60 : n * 1440)
                }
            }
        }
        .padding(24)
        .frame(width: 460)
        .background(Farbe.grund)
        .foregroundStyle(Farbe.text)
        .tint(Farbe.haupt)
    }
}

// MARK: - Bearbeiten

/// Eine Erinnerung schreiben oder bearbeiten, wie der Prompt-Editor über das ganze Fenster im Vollbildmodus von macOS. Bearbeiten erzeugt nie eine Quittung.
private struct ErinnerungEditorVollbild: View {
    let ausgang: ErinnerungEntwurf
    @State private var d: ErinnerungEntwurf
    @State private var steuerung = AppSteuerung.shared
    @State private var fehler = ""
    @State private var meldung = ""
    @State private var speichert = false
    @State private var arbeitet = false
    @State private var frageVerwerfen = false
    @State private var vollbildGeschaltet = false
    @State private var vorKi: ErinnerungEntwurf?
    @State private var nachKi: ErinnerungEntwurf?
    @State private var datumOffen = false
    @State private var uhrOffen = false
    @State private var uhrEntwurf = Date()
    @FocusState private var fokus: Bool

    init(entwurf: ErinnerungEntwurf) {
        ausgang = entwurf
        _d = State(initialValue: entwurf)
    }

    /// Der Diktat-Knopf hängt für Prompts immer den Aufgabentrenner an. Erinnerungen kennen keine Aufgaben: Hier wird das Gesagte mit einem Leerzeichen angefügt.
    private var diktatZiel: Binding<String> {
        Binding(get: { "" }, set: { neu in
            var t = neu.trimmingCharacters(in: .whitespacesAndNewlines)
            while t.hasSuffix(";") { t = String(t.dropLast()).trimmingCharacters(in: .whitespacesAndNewlines) }
            guard !t.isEmpty else { return }
            let vorn = d.text.trimmingCharacters(in: .whitespacesAndNewlines)
            d.text = vorn.isEmpty ? t : vorn + " " + t
        })
    }

    private var datumWahl: Binding<Date> {
        Binding(get: { ErinnerungZeit.datum(d.datum) ?? Date() }, set: { d.datum = ErinnerungZeit.datumText($0); datumOffen = false })
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                Text(d.kennung != nil ? "Erinnerung bearbeiten" : "Neue Erinnerung").font(.jarvis(24, .bold))
                Spacer()
                KapselKnopf(symbol: "xmark", titel: "Schließen") { if d != ausgang { frageVerwerfen = true } else { schliessen() } }
                KapselKnopf(symbol: "square.and.arrow.down.fill", titel: speichert ? "Speichert …" : "Speichern", hervorgehoben: true) { speichern() }
                    .keyboardShortcut("s", modifiers: .command)
            }
            HStack(spacing: 8) {
                WahlChip(titel: "Mit Datum", aktiv: !d.allgemein) { d.allgemein = false }
                WahlChip(titel: "Allgemein", aktiv: d.allgemein) { d.allgemein = true }
                if !d.allgemein { terminFelder }
                Spacer()
            }
            HStack(spacing: 10) {
                DiktatKnopf(text: diktatZiel)
                KapselKnopf(symbol: "sparkles", titel: arbeitet ? "Jarvis verbessert …" : "Mit KI verbessern") { verbessern() }
                if arbeitet { ProgressView().controlSize(.small) }
                if let v = vorKi, nachKi == d {
                    KapselKnopf(symbol: "arrow.uturn.backward", titel: "Rückgängig") { d = v; vorKi = nil; nachKi = nil }
                }
                if !meldung.isEmpty { Text(meldung).font(.jarvis(12)).foregroundStyle(Farbe.warnung) }
            }
            if !fehler.isEmpty { Text(fehler).font(.jarvis(13)).foregroundStyle(Farbe.warnung) }
            TextEditor(text: $d.text).font(.jarvis(17)).scrollContentBackground(.hidden).focused($fokus)
                .padding(14)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(RoundedRectangle(cornerRadius: 16).fill(Farbe.glas(0.05)))
            feld("Kurzfassung zum Vorlesen (etwa 35 Wörter)", $d.kurz, zeilen: 3)
            feld("Listenzeile (etwa sechs Wörter)", $d.zeile, zeilen: 1)
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
        .confirmationDialog("Deine Änderungen an dieser Erinnerung verwerfen?", isPresented: $frageVerwerfen) {
            Button("Verwerfen", role: .destructive) { schliessen() }
        }
    }

    @ViewBuilder private var terminFelder: some View {
        WahlChip(titel: d.datum.flatMap { ErinnerungZeit.tag($0) }.map { ErinnerungZeit.anzeige($0) } ?? "Datum wählen", aktiv: d.datum != nil) { datumOffen = true }
            .popover(isPresented: $datumOffen) {
                DatePicker("", selection: datumWahl, displayedComponents: .date).datePickerStyle(.graphical).labelsHidden().padding(12)
                    .environment(\.locale, Locale(identifier: "de_DE"))
            }
        WahlChip(titel: d.uhrzeit.map { "\($0) Uhr" } ?? "Ohne Uhrzeit", aktiv: d.uhrzeit != nil) {
            uhrEntwurf = ErinnerungZeit.uhrDatum(d.uhrzeit ?? AblageAbgleich.shared.erinnerungOhneUhrzeit)
            uhrOffen = true
        }
        .popover(isPresented: $uhrOffen) {
            HStack(spacing: 10) {
                DatePicker("", selection: $uhrEntwurf, displayedComponents: .hourAndMinute).labelsHidden()
                    .environment(\.locale, Locale(identifier: "de_DE"))
                KapselKnopf(symbol: "checkmark", titel: "Übernehmen") { d.uhrzeit = ErinnerungZeit.uhrText(uhrEntwurf); uhrOffen = false }
            }
            .padding(12)
        }
        if d.uhrzeit != nil { KleinKnopf(symbol: "xmark", hilfe: "Uhrzeit entfernen") { d.uhrzeit = nil } }
        WahlChip(titel: d.form == Erinnerung.formWecker ? "Wecker" : "Benachrichtigung", aktiv: d.form == Erinnerung.formWecker) {
            d.form = d.form == Erinnerung.formWecker ? Erinnerung.formMeldung : Erinnerung.formWecker
        }
    }

    private func feld(_ hinweis: String, _ text: Binding<String>, zeilen: Int) -> some View {
        TextField(hinweis, text: text, axis: .vertical).textFieldStyle(.plain).font(.jarvis(14)).lineLimit(1...zeilen)
            .padding(.horizontal, 12).padding(.vertical, 9)
            .background(RoundedRectangle(cornerRadius: 12).fill(Farbe.glas(0.05)))
    }

    private func schliessen() { steuerung.erinnerungEntwurf = nil }

    private func verbessern() {
        let alt = d
        guard !arbeitet, !alt.text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        arbeitet = true; meldung = ""
        Task {
            let roh = await Kurzauftrag.frage(alt.text, system: erinnerungKi)
            arbeitet = false
            guard d == alt else { meldung = "Der Text wurde inzwischen geändert. Die KI-Fassung ist verworfen."; return }
            guard let roh, let anfang = roh.firstIndex(of: "{"), let ende = roh.lastIndex(of: "}"), anfang < ende, let j = JSON.objekt(String(roh[anfang...ende])) else {
                meldung = "Die KI hat nichts Brauchbares geliefert. Der Text bleibt, wie er ist."
                return
            }
            func wert(_ k: String) -> String { (j[k] as? String ?? "").trimmingCharacters(in: .whitespacesAndNewlines) }
            guard wert("text").count >= 3 else { meldung = "Die KI hat nichts Brauchbares geliefert. Der Text bleibt, wie er ist."; return }
            var neu = alt
            neu.text = wert("text")
            if !wert("kurz").isEmpty { neu.kurz = wert("kurz") }
            if !wert("zeile").isEmpty { neu.zeile = wert("zeile") }
            d = neu; vorKi = alt; nachKi = neu
            log("ablage", "Erinnerung verbessert", wo: "Ablage/Erinnerung")
        }
    }

    private func speichern() {
        guard !speichert, !arbeitet, !Diktat.shared.aktiv else { return }
        let e = d
        let text = e.text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { fehler = AblageFehler.leer.localizedDescription; return }
        guard e.allgemein || e.datum != nil else { fehler = "Bitte ein Datum wählen."; return }
        let datum = e.allgemein ? nil : e.datum, uhrzeit = e.allgemein ? nil : e.uhrzeit
        let kurz = e.kurz.trimmingCharacters(in: .whitespacesAndNewlines), zeile = e.zeile.trimmingCharacters(in: .whitespacesAndNewlines)
        speichert = true; fehler = ""
        Task {
            defer { speichert = false }
            let abgleich = AblageAbgleich.shared
            do {
                if let kennung = e.kennung {
                    try await abgleich.erinnerungAendern(kennung) { alt in
                        var n = alt
                        n.text = text
                        n.kurz = kurz.isEmpty ? Erinnerung.kurzAus(text) : kurz
                        n.zeile = zeile.isEmpty ? Erinnerung.zeileAus(text) : zeile
                        n.datum = datum; n.uhrzeit = uhrzeit; n.form = e.form; n.allgemein = e.allgemein
                        // Nur beim Wechsel von allgemein zu datiert: Bewusst entfernte Zeitpunkte bleiben entfernt.
                        if !e.allgemein && alt.allgemein { n.ausloesungen = [Erinnerung.neueAusloesung(0)] }
                        return n
                    }
                } else {
                    try await abgleich.erinnerungAnlegen(text: text, zeile: zeile, kurz: kurz, datum: datum, uhrzeit: uhrzeit, form: e.form, vorab: [])
                }
            } catch { fehler = error.localizedDescription; return }
            schliessen()
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
