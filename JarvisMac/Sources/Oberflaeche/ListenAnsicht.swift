import SwiftUI

/// Eigener Leistenreiter „Listen“ zwischen Ablage und Einstellungen, wie am Handy und Tablet.
struct ListenReiter: View {
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                BereichsKopf(symbol: "checklist", titel: "Listen",
                             untertitel: "Dieselben Listen wie auf Handy und Tablet, in beide Richtungen abgeglichen; die jüngere Änderung gilt.")
                ListenAnsicht()
            }
            .padding(36).frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

/// Die Listen, wie der Listen-Reiter am Handy (ui/ListenBildschirm.kt): Listen als Kacheln, darunter die gewählte Liste mit
/// Abhaken, Unterpunkten, Prioritäten, Tags, Abschnitten, Sortierung und Gruppierung. Alles geht über ListenSpeicher und ist sofort auf Handy und Tablet.
struct ListenAnsicht: View {
    @State private var speicher = ListenSpeicher.shared
    @MainActor private static var gewaehlt: Int64 = 0
    @State private var gewaehlt = ListenAnsicht.gewaehlt
    @State private var unterVon: Int64 = 0
    @State private var bearbeite: Int64 = 0
    @State private var bearbeiteText = ""
    @State private var loeschenFrage = false
    @State private var abschnittWahl = ""
    @State private var neuName = ""
    @State private var eingabe = ""
    @State private var nameText = ""
    /// Zu welcher Liste nameText und abschnitteText gehören: Ein Wechsel mit fokussiertem Feld benennt sonst die neue Liste um.
    @State private var kopfVon: Int64 = 0
    @State private var abschnitteText = ""
    @FocusState private var eingabeFokus: Bool
    @FocusState private var nameFokus: Bool
    @FocusState private var abschnitteFokus: Bool
    @FocusState private var titelFokus: Bool

    static func listenFarbe(_ l: Liste) -> Color {
        let farben = [Farbe.haupt, Farbe.gruen, Farbe.gold, Farbe.kupfer, Farbe.warnung, Farbe.rot, Farbe.text2, Farbe.haupt]
        return farben[abs(l.farbe) % farben.count]
    }
    static func prioFarbe(_ p: Int) -> Color { p == 3 ? Farbe.rot : p == 2 ? Farbe.gold : p == 1 ? Farbe.gruen : Farbe.text3 }

    private var aktuell: Liste? {
        let alle = speicher.alle
        return alle.first { $0.id == gewaehlt } ?? alle.first
    }

    var body: some View {
        let _ = speicher.aenderung
        let l = aktuell
        VStack(alignment: .leading, spacing: 16) {
            kacheln(l)
            status
            if let l {
                kopf(l)
                unterHinweis(l)
                HStack(spacing: 10) {
                    Image(systemName: "plus").font(.system(size: 13, weight: .semibold)).foregroundStyle(Farbe.haupt)
                    TextField("Eintrag hinzufügen … (Enter; !hoch, #tag)", text: $eingabe).textFieldStyle(.plain).font(.jarvis(14))
                        .focused($eingabeFokus).onSubmit { hinzufuegen(l) }
                }
                .padding(.horizontal, 12).padding(.vertical, 10)
                .background(RoundedRectangle(cornerRadius: 12).fill(Farbe.glas(0.05)))
                .overlay(RoundedRectangle(cornerRadius: 12).strokeBorder(Farbe.glas(0.08)))
                zeilen(l)
            } else {
                Text(speicher.bereit ? "Noch keine Listen. Leg oben eine an oder sag Jarvis „Setz Milch auf die Einkaufsliste“." : "")
                    .font(.jarvis(14)).foregroundStyle(Farbe.text2)
            }
        }
        .onAppear { if let l { gewaehlt = l.id; kopfVon = l.id; nameText = l.name; abschnitteText = l.abschnitte.joined(separator: ", ") } }
        .onChange(of: aktuell?.id) { _, _ in uebernehmeKopf() }
        // Tippt Frank gerade im Kopf, bleibt sein Text stehen: Sonst überschriebe eine Änderung vom Handy die halbe Eingabe.
        .onChange(of: speicher.aenderung) { _, _ in uebernehmeKopf() }
    }

    private func uebernehmeKopf() {
        guard let l = aktuell else { return }
        if gewaehlt != l.id { gewaehlt = l.id; Self.gewaehlt = l.id }
        let gewechselt = kopfVon != l.id
        kopfVon = l.id
        if gewechselt || !nameFokus { nameText = l.name }
        if gewechselt || !abschnitteFokus { abschnitteText = l.abschnitte.joined(separator: ", ") }
    }

    private func waehle(_ id: Int64) {
        guard gewaehlt != id else { return }
        // Offene Eingabe im Kopf gehört zur bisherigen Liste: vor dem Wechsel dort speichern.
        if let alt = aktuell, kopfVon == alt.id { namensAenderung(alt); abschnitteAenderung(alt) }
        gewaehlt = id; Self.gewaehlt = id
        if bearbeite != 0, let e = speicher.eintrag(mitId: bearbeite) { titelFertig(e) }
        unterVon = 0; bearbeite = 0; loeschenFrage = false; abschnittWahl = ""
        uebernehmeKopf()
    }

    // MARK: Kacheln und Stand

    private func kacheln(_ aktiv: Liste?) -> some View {
        LazyVGrid(columns: [GridItem(.adaptive(minimum: 170), spacing: 10)], spacing: 10) {
            ForEach(speicher.alle, id: \.id) { l in
                let an = aktiv?.id == l.id
                let farbe = Self.listenFarbe(l)
                Button { waehle(l.id) } label: {
                    HStack(spacing: 10) {
                        Text(ListenArten.zeichen(l.art)).font(.system(size: 16))
                            .frame(width: 34, height: 34).background(RoundedRectangle(cornerRadius: 10).fill(farbe.opacity(0.15)))
                        VStack(alignment: .leading, spacing: 1) {
                            Text(l.name).font(.jarvis(13, .semibold)).foregroundStyle(Farbe.text).lineLimit(1)
                            Text("\(speicher.offen(l.id)) offen").font(.jarvis(11)).foregroundStyle(Farbe.text3)
                        }
                        Spacer(minLength: 0)
                    }
                    .padding(10)
                    .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(Farbe.glas(an ? 0.13 : 0.04)))
                    .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).strokeBorder(an ? farbe.opacity(0.7) : Farbe.glas(0.06)))
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
            // Neue Liste: Name eintippen, Enter legt an.
            TextField("+ Neue Liste", text: $neuName).textFieldStyle(.plain).font(.jarvis(13))
                .padding(.horizontal, 12).padding(.vertical, 18)
                .background(RoundedRectangle(cornerRadius: 14, style: .continuous).fill(Farbe.glas(0.03)))
                .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).strokeBorder(Farbe.glas(0.08)))
                .onSubmit {
                    let name = neuName.trimmingCharacters(in: .whitespaces)
                    guard !name.isEmpty else { return }
                    let l = speicher.listeAnlegen(name, art: Self.artAusName(name))
                    neuName = ""
                    gewaehlt = 0
                    waehle(l.id)
                    log("listen", "Liste angelegt: \(name)", wo: "Listen")
                }
        }
    }

    @ViewBuilder private var status: some View {
        let zeilen: [(String, Color)] = [
            (!speicher.bereit && speicher.alle.isEmpty) ? ("Der Abgleich mit Handy und Tablet läuft …", Farbe.text2) : nil,
            speicher.ausstehend > 0 ? ("\(speicher.ausstehend) Änderung(en) gehen noch an Handy und Tablet …", Farbe.text3) : nil,
            speicher.meldung.map { ($0, Farbe.warnung) },
            speicher.fehler.map { ($0, Farbe.warnung) },
        ].compactMap { $0 }
        if !zeilen.isEmpty {
            VStack(alignment: .leading, spacing: 4) {
                ForEach(Array(zeilen.enumerated()), id: \.offset) { _, z in Text(z.0).font(.jarvis(12)).foregroundStyle(z.1) }
            }
        }
    }

    /// Wie `Listen.artAusName` am Handy: „Einkaufsliste“ → Einkauf.
    static func artAusName(_ name: String) -> String {
        let g = name.lowercased()
        if ["einkauf", "shopping", "supermarkt", "besorg"].contains(where: g.contains) { return "einkauf" }
        if ["pack", "koffer", "urlaub", "reise"].contains(where: g.contains) { return "packliste" }
        if g.contains("idee") { return "ideen" }
        if ["aufgabe", "todo", "to do", "erledig"].contains(where: g.contains) { return "aufgaben" }
        if g.contains("check") { return "checkliste" }
        return "sonstiges"
    }

    // MARK: Kopf der Liste

    private func namensAenderung(_ l: Liste) {
        guard kopfVon == l.id else { return }
        let n = nameText.trimmingCharacters(in: .whitespaces)
        if !n.isEmpty && n != l.name { speicher.listeAendern(l.id) { $0["name"] = n } }
    }

    private func abschnitteAenderung(_ l: Liste) {
        guard kopfVon == l.id else { return }
        var neu: [String] = []
        for a in abschnitteText.split(separator: ",").map({ $0.trimmingCharacters(in: .whitespaces) }) where !a.isEmpty && !neu.contains(a) { neu.append(a) }
        if neu != l.abschnitte { speicher.listeAendern(l.id) { $0["abschnitte"] = neu } }
    }

    private func wahl(_ l: Liste, _ feld: String, _ wert: String) -> Binding<String> {
        Binding(get: { wert }, set: { w in speicher.listeAendern(l.id) { $0[feld] = w } })
    }

    private func kopf(_ l: Liste) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 12) {
                Text(ListenArten.zeichen(l.art)).font(.system(size: 22))
                TextField("Name", text: $nameText).textFieldStyle(.plain).font(.jarvis(20, .bold))
                    .focused($nameFokus).onSubmit { namensAenderung(l) }
                    .onChange(of: nameFokus) { _, drin in if !drin { namensAenderung(l) } }
                RundKnopf(symbol: "checkmark.circle.fill", hilfe: "Erledigte Einträge löschen", groesse: 30) {
                    let n = speicher.erledigteLoeschen(l.id)
                    log("listen", "\(n) erledigte Einträge gelöscht", wo: "Listen")
                }
                if loeschenFrage {
                    Text("Liste wirklich löschen?").font(.jarvis(12, .semibold)).foregroundStyle(Farbe.rot)
                    KapselKnopf(symbol: "trash.fill", titel: "Löschen", hervorgehoben: true) {
                        loeschenFrage = false
                        speicher.listeLoeschen(l.id)
                        gewaehlt = 0; Self.gewaehlt = 0
                        log("listen", "Liste gelöscht: \(l.name)", wo: "Listen")
                    }
                    KapselKnopf(symbol: "xmark", titel: "Abbrechen") { loeschenFrage = false }
                } else {
                    RundKnopf(symbol: "trash", hilfe: "Liste löschen", groesse: 30) { loeschenFrage = true }
                }
            }
            HStack(spacing: 10) {
                Picker("", selection: wahl(l, "art", l.art)) { ForEach(ListenArten.alle, id: \.kennung) { Text($0.zeichen + " " + $0.anzeige).tag($0.kennung) } }
                    .labelsHidden().frame(width: 150)
                Picker("", selection: wahl(l, "sortierung", l.sortierung)) { ForEach(ListenArten.sortierungen, id: \.kennung) { Text($0.anzeige).tag($0.kennung) } }
                    .labelsHidden().frame(width: 180)
                Picker("", selection: wahl(l, "gruppierung", l.gruppierung)) { ForEach(ListenArten.gruppierungen, id: \.kennung) { Text("Gruppe: " + $0.anzeige).tag($0.kennung) } }
                    .labelsHidden().frame(width: 180)
                Spacer()
                Text("Erledigte zeigen").font(.jarvis(12)).foregroundStyle(Farbe.text2)
                Toggle("", isOn: Binding(get: { l.erledigteZeigen }, set: { an in speicher.listeAendern(l.id) { $0["erledigte_zeigen"] = an } }))
                    .toggleStyle(.switch).labelsHidden()
            }
            if l.gruppierung == "abschnitt" {
                TextField("Abschnitte, mit Komma getrennt (z. B. Obst, Drogerie)", text: $abschnitteText).textFieldStyle(.plain).font(.jarvis(12))
                    .focused($abschnitteFokus).onSubmit { abschnitteAenderung(l) }
                    .onChange(of: abschnitteFokus) { _, drin in if !drin { abschnitteAenderung(l) } }
                    .padding(.horizontal, 10).padding(.vertical, 8)
                    .background(RoundedRectangle(cornerRadius: 10).fill(Farbe.glas(0.04)))
            }
        }
    }

    @ViewBuilder private func unterHinweis(_ l: Liste) -> some View {
        if unterVon != 0, let oben = speicher.eintraegeVon(l.id).first(where: { $0.id == unterVon }) {
            HStack(spacing: 8) {
                Image(systemName: "arrow.turn.up.right").font(.system(size: 12)).foregroundStyle(Farbe.haupt)
                Text("Neue Einträge kommen unter „\(oben.titel)“").font(.jarvis(12)).foregroundStyle(Farbe.haupt)
                RundKnopf(symbol: "xmark", hilfe: "Wieder auf die oberste Ebene", groesse: 24) { unterVon = 0 }
                Spacer()
            }
        }
    }

    private func hinzufuegen(_ l: Liste) {
        let text = eingabe.trimmingCharacters(in: .whitespaces)
        guard !text.isEmpty else { return }
        let s = ListenArten.schnell(text)
        // In der Abschnittsansicht landet ein neuer Eintrag im letzten Abschnitt, den Frank angetippt hat (wie am Handy der Kopf „+“).
        let abschnitt = l.gruppierung == "abschnitt" && unterVon == 0 ? abschnittWahl : ""
        let oben = speicher.eintraegeVon(l.id).contains { $0.id == unterVon } ? unterVon : 0
        speicher.hinzufuegen(l.id, s.titel, eltern: oben, prioritaet: s.prio, tags: s.tags, abschnitt: abschnitt)
        eingabe = ""
        log("listen", "Eintrag hinzugefügt: \(s.titel)", wo: "Listen")
    }

    // MARK: Zeilen

    @ViewBuilder private func zeilen(_ l: Liste) -> some View {
        let ansicht = speicher.ansicht(l)
        if ansicht.isEmpty {
            Text("Die Liste ist leer.").font(.jarvis(13)).foregroundStyle(Farbe.text3).padding(.leading, 4).padding(.top, 8)
        } else {
            VStack(alignment: .leading, spacing: 0) {
                // Feste Kennungen: Eine Änderung vom Handy verschiebt sonst die Zeilen, und ein Titel, der gerade bearbeitet wird, verlöre den Fokus.
                ForEach(Array(ansicht.enumerated()), id: \.element.kennung) { _, z in
                    if let e = z.eintrag { eintragZeile(l, z, e) } else { kopfZeile(l, z) }
                }
            }
        }
    }

    @ViewBuilder private func kopfZeile(_ l: Liste, _ z: ListenZeile) -> some View {
        let name = z.kopf ?? ""
        let echt = name == "Ohne Abschnitt" ? "" : name
        let an = l.gruppierung == "abschnitt" && abschnittWahl == echt && name != "Erledigt"
        let inhalt = HStack(spacing: 8) {
            Text(name.uppercased()).font(.jarvis(11, .bold)).foregroundStyle(an ? Farbe.haupt : Farbe.text3)
            Text("\(z.kopfAnzahl)").font(.jarvis(11)).foregroundStyle(Farbe.text3)
            Spacer()
        }
        .padding(.leading, 4).padding(.top, 14).padding(.bottom, 4)
        if l.gruppierung == "abschnitt" && name != "Erledigt" {
            Button { abschnittWahl = abschnittWahl == echt ? "" : echt } label: { inhalt.contentShape(Rectangle()) }
                .buttonStyle(.plain).help("Antippen: neue Einträge kommen in diesen Abschnitt")
        } else {
            inhalt
        }
    }

    private func titelFertig(_ e: ListenEintrag) {
        guard bearbeite == e.id else { return }
        bearbeite = 0
        let t = bearbeiteText.trimmingCharacters(in: .whitespaces)
        if !t.isEmpty && t != e.titel { speicher.aendern(e.id) { $0["titel"] = t } }
    }

    private func eintragZeile(_ liste: Liste, _ z: ListenZeile, _ e: ListenEintrag) -> some View {
        let prio = Self.prioFarbe(e.prioritaet)
        let andere = speicher.alle.filter { $0.id != e.listeId }
        return ListenZeileRahmen(tiefe: z.tiefe) {
            Button {
                speicher.abhaken(e.id, !e.erledigt)
                log("listen", (e.erledigt ? "Wieder geöffnet: " : "Abgehakt: ") + e.titel, wo: "Listen")
            } label: {
                ZStack {
                    Circle().fill(e.erledigt ? Farbe.haupt : Color.clear)
                    Circle().strokeBorder(e.erledigt ? Farbe.haupt : (e.prioritaet > 0 ? prio : Farbe.text3), lineWidth: 1.6)
                    if e.erledigt { Image(systemName: "checkmark").font(.system(size: 10, weight: .bold)).foregroundStyle(Farbe.aufOrange) }
                }
                .frame(width: 20, height: 20).padding(.top, 2)
                .contentShape(Circle())
            }
            .buttonStyle(.plain).help(e.erledigt ? "Wieder öffnen" : "Abhaken")
        } titel: {
            if bearbeite == e.id {
                TextField("Titel", text: $bearbeiteText).textFieldStyle(.plain).font(.jarvis(14))
                    .focused($titelFokus)
                    .onSubmit { titelFertig(e) }
                    .onChange(of: titelFokus) { _, drin in if !drin { titelFertig(e) } }
                    .onExitCommand { bearbeite = 0 }
                    .onAppear { titelFokus = true }
            } else {
                VStack(alignment: .leading, spacing: 3) {
                    Text(e.titel).font(.jarvis(14, e.angeheftet ? .semibold : .regular)).foregroundStyle(e.erledigt ? Farbe.text3 : Farbe.text)
                        .strikethrough(e.erledigt)
                        .onTapGesture(count: 2) {
                            // Läuft schon eine Bearbeitung, erst sie speichern: sonst ginge der geänderte Titel still verloren.
                            if bearbeite != 0, bearbeite != e.id, let alt = speicher.eintrag(mitId: bearbeite) { titelFertig(alt) }
                            bearbeite = e.id; bearbeiteText = e.titel
                        }
                        .help("Doppelklick: Titel ändern")
                    let zusatz = !e.tags.isEmpty || e.angeheftet || !e.faellig.isEmpty || z.kinderGesamt > 0 || (!e.abschnitt.isEmpty && liste.gruppierung != "abschnitt")
                    if zusatz {
                        HStack(spacing: 8) {
                            if e.angeheftet { Image(systemName: "bookmark.fill").font(.system(size: 10)).foregroundStyle(Farbe.haupt) }
                            if !e.faellig.isEmpty { Text(ListenArten.datumKurz(e.faellig)).font(.jarvis(11, .semibold)).foregroundStyle(Farbe.gold) }
                            ForEach(e.tags, id: \.self) { Text("#" + $0).font(.jarvis(11)).foregroundStyle(Farbe.haupt) }
                            if z.kinderGesamt > 0 { Text("\(z.kinderErledigt)/\(z.kinderGesamt)").font(.jarvis(11)).foregroundStyle(Farbe.text3) }
                            if !e.abschnitt.isEmpty && liste.gruppierung != "abschnitt" { Text(e.abschnitt).font(.jarvis(11)).foregroundStyle(Farbe.text3) }
                        }
                    }
                    if !e.notiz.isEmpty { Text(e.notiz).font(.jarvis(12)).foregroundStyle(Farbe.text2).lineLimit(2) }
                }
            }
        } werkzeuge: {
            HStack(spacing: 4) {
                RundKnopf(symbol: "plus", hilfe: "Unterpunkt hinzufügen", groesse: 26) { unterVon = e.id; eingabeFokus = true }
                RundKnopf(symbol: "exclamationmark.circle.fill", hilfe: "Priorität: \(ListenArten.prioName(e.prioritaet)) (Klick: weiter)", groesse: 26) {
                    let p = (e.prioritaet + 1) % 4
                    speicher.aendern(e.id) { $0["prioritaet"] = p }
                }
                RundKnopf(symbol: "bookmark.fill", hilfe: e.angeheftet ? "Lösen" : "Oben anheften", aktiv: e.angeheftet, groesse: 26) {
                    speicher.aendern(e.id) { $0["angeheftet"] = !e.angeheftet }
                }
                if e.eltern != 0 {
                    RundKnopf(symbol: "arrow.uturn.backward", hilfe: "Auf die oberste Ebene", groesse: 26) { speicher.einhaengen(e.id, unter: 0) }
                }
                if !andere.isEmpty {
                    Menu {
                        ForEach(andere, id: \.id) { l in
                            Button(ListenArten.zeichen(l.art) + "  " + l.name) {
                                speicher.verschieben(e.id, nach: l.id)
                                log("listen", "Verschoben nach \(l.name): \(e.titel)", wo: "Listen")
                            }
                        }
                    } label: {
                        Image(systemName: "arrow.right").font(.system(size: 11, weight: .semibold)).foregroundStyle(Farbe.text2)
                            .frame(width: 26, height: 26).background(Circle().fill(Farbe.glas(0.06)))
                    }
                    .menuStyle(.borderlessButton).menuIndicator(.hidden).fixedSize().help("In eine andere Liste verschieben")
                }
                RundKnopf(symbol: "trash", hilfe: "Löschen (mit Unterpunkten)", groesse: 26) {
                    speicher.loeschen(e.id)
                    log("listen", "Gelöscht: " + e.titel, wo: "Listen")
                }
            }
        }
    }
}

/// Eine Eintragszeile: Haken, Titel, die Werkzeuge erscheinen beim Darüberfahren; eingerückt nach Tiefe.
private struct ListenZeileRahmen<Haken: View, Titel: View, Werkzeuge: View>: View {
    let tiefe: Int
    @ViewBuilder var haken: Haken
    @ViewBuilder var titel: Titel
    @ViewBuilder var werkzeuge: Werkzeuge
    @State private var schwebt = false

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            haken
            titel.frame(maxWidth: .infinity, alignment: .leading)
            werkzeuge.opacity(schwebt ? 1 : 0)
        }
        .padding(.leading, 10 + 24 * CGFloat(tiefe)).padding(.trailing, 8).padding(.vertical, 8)
        .background(RoundedRectangle(cornerRadius: 10).fill(schwebt ? Farbe.glas(0.05) : Color.clear))
        .onHover { schwebt = $0 }
    }
}
