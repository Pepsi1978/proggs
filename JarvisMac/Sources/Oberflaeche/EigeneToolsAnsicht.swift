import SwiftUI

/// „Eigene Tools“: Inhaltsverzeichnis der Helfer dieses Macs mit Detailbereich, oben Pause und Recherche-Wahl.
struct EigeneToolsAnsicht: View {
    @State private var einst = Einstellungen.shared
    @State private var katalog = HelferKatalog.shared
    @State private var offen: String?
    @State private var loeschen: HelferEintrag?

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            if let k = offen, let e = katalog.finde(k), e.zustand != HelferKatalog.papierkorb { detail(e) } else { uebersicht }
        }
        .alert(loeschen.map { "\($0.name) endgültig löschen?" } ?? "", isPresented: Binding(get: { loeschen != nil }, set: { if !$0 { loeschen = nil } })) {
            Button("Abbrechen", role: .cancel) {}
            Button("Endgültig löschen", role: .destructive) { if let e = loeschen { katalog.endgueltigLoeschen(e.kennung) } }
        } message: {
            if let e = loeschen {
                Text("Gelöscht werden Programm, frühere Fassung, eigener Speicher und Ergebnisdateien dieses Helfers." + (e.konten.isEmpty ? "" : " Die Konten \(e.konten.joined(separator: ", ")) bleiben erhalten.") + " Das lässt sich nicht rückgängig machen.")
            }
        }
    }

    private var uebersicht: some View {
        VStack(alignment: .leading, spacing: 18) {
            GlasKarte {
                VStack(alignment: .leading, spacing: 10) {
                    KartenTitel(symbol: "wrench.and.screwdriver.fill", titel: "Eigene Tools")
                    Text("Helfer, die Jarvis für deine Aufgaben bauen lässt, prüft und selbst benutzt.").font(.jarvis(12)).foregroundStyle(Farbe.text2)
                    Toggle(isOn: $einst.helferAutomatik) { Text("Helfer selbst bauen lassen").font(.jarvis(14, .semibold)) }
                        .toggleStyle(.switch).tint(Farbe.haupt)
                    HStack(spacing: 14) {
                        Text("Recherche").font(.jarvis(14, .semibold))
                        Spacer()
                        Picker("", selection: $einst.rechercheModell) { ForEach(ModellWahl.fuerRecherche) { Text($0.name).tag($0.id) } }.labelsHidden().frame(maxWidth: 190)
                        Picker("", selection: $einst.rechercheEffort) {
                            Text("Mittel").tag("medium"); Text("High").tag("high"); Text("Sehr hoch").tag("xhigh"); Text("Maximal").tag("max")
                        }.labelsHidden().frame(maxWidth: 120)
                    }
                }
            }
            GlasKarte {
                VStack(alignment: .leading, spacing: 4) {
                    KartenTitel(symbol: "list.bullet", titel: "Inhalt")
                    if katalog.sortiert.isEmpty { Text("Noch keine Helfer. Jarvis legt hier an, was deine Aufgaben brauchen.").font(.jarvis(13)).foregroundStyle(Farbe.text2).padding(.top, 4) }
                    ForEach(katalog.sortiert) { e in
                        HStack(spacing: 10) {
                            Circle().fill(farbe(e)).frame(width: 8, height: 8)
                            VStack(alignment: .leading, spacing: 1) {
                                Text(e.name).font(.jarvis(14, .semibold))
                                Text(e.text("zweck")).font(.jarvis(12)).foregroundStyle(Farbe.text2).lineLimit(1)
                            }
                            Spacer(minLength: 8)
                            Text(zustandText(e)).font(.jarvis(12)).foregroundStyle(Farbe.text2)
                            Image(systemName: "chevron.right").font(.system(size: 11)).foregroundStyle(Farbe.text3)
                        }
                        .padding(.vertical, 7)
                        .contentShape(Rectangle())
                        .onTapGesture { offen = e.kennung }
                    }
                }
            }
            if !katalog.imPapierkorb.isEmpty {
                GlasKarte {
                    VStack(alignment: .leading, spacing: 8) {
                        KartenTitel(symbol: "trash", titel: "Papierkorb", farbe: Farbe.text2)
                        ForEach(katalog.imPapierkorb) { e in
                            HStack {
                                Text(e.name).font(.jarvis(13, .medium))
                                Spacer()
                                KapselKnopf(symbol: "arrow.uturn.backward", titel: "Wiederherstellen") { _ = katalog.verwalten(e.kennung, aktion: "wiederherstellen") }
                                KapselKnopf(symbol: "trash", titel: "Löschen") { loeschen = e }
                            }
                        }
                    }
                }
            }
        }
    }

    private func detail(_ e: HelferEintrag) -> some View {
        VStack(alignment: .leading, spacing: 18) {
            KapselKnopf(symbol: "chevron.left", titel: "Eigene Tools") { offen = nil }
            GlasKarte {
                VStack(alignment: .leading, spacing: 10) {
                    KartenTitel(symbol: "wrench.adjustable.fill", titel: e.name)
                    HStack(spacing: 8) {
                        Circle().fill(farbe(e)).frame(width: 8, height: 8)
                        Text(zustandText(e)).font(.jarvis(13, .semibold))
                        Text("Fassung \(e.text("version")) · am Mac").font(.jarvis(12)).foregroundStyle(Farbe.text2)
                    }
                    angabe("Wofür", e.text("zweck"))
                    angabe("Kann", e.liste("faehigkeiten").joined(separator: ", "))
                    angabe("Aufruf", "Jarvis ruft mac_helfer_ausfuehren mit der Kennung \(e.kennung); vom Handy aus helfer_ausfuehren.")
                    angabe("Braucht", eingabenText(e))
                    angabe("Liefert", e.text("ausgabe"))
                    angabe("Voraussetzungen", e.liste("voraussetzungen").joined(separator: ", "))
                    angabe("Rechte", rechteText(e))
                    angabe("Grenzen", e.liste("grenzen").joined(separator: ", "))
                    if e.manifest["braucht_app_update"] as? Bool == true { angabe("Hinweis", "Braucht ein Update von Jarvis.") }
                    angabe("Letzte Prüfung", pruefungText(e))
                    HStack(spacing: 8) {
                        if e.zustand == HelferKatalog.wartet { KapselKnopf(symbol: "checkmark.seal.fill", titel: "Rechte freigeben", hervorgehoben: true) { katalog.freigeben(e.kennung) } }
                        KapselKnopf(symbol: e.aktiv ? "pause.fill" : "play.fill", titel: e.aktiv ? "Deaktivieren" : "Aktivieren") { _ = katalog.verwalten(e.kennung, aktion: e.aktiv ? "deaktivieren" : "aktivieren") }
                        if katalog.hatVorherigeFassung(e.kennung) { KapselKnopf(symbol: "arrow.uturn.backward", titel: "Letzte Fassung") { _ = katalog.verwalten(e.kennung, aktion: "rueckrollen") } }
                        KapselKnopf(symbol: "arrow.up", titel: "Höher") { katalog.verschieben(e.kennung, um: -1) }
                        KapselKnopf(symbol: "arrow.down", titel: "Tiefer") { katalog.verschieben(e.kennung, um: 1) }
                        KapselKnopf(symbol: "trash", titel: "Papierkorb") { _ = katalog.verwalten(e.kennung, aktion: "papierkorb"); offen = nil }
                    }
                }
            }
        }
    }

    @ViewBuilder private func angabe(_ titel: String, _ text: String) -> some View {
        if !text.isEmpty {
            HStack(alignment: .top, spacing: 10) {
                Text(titel).font(.jarvis(12, .semibold)).foregroundStyle(Farbe.text2).frame(width: 110, alignment: .leading)
                Text(text).font(.jarvis(13)).fixedSize(horizontal: false, vertical: true).textSelection(.enabled)
            }
        }
    }

    private func eingabenText(_ e: HelferEintrag) -> String {
        let s = e.manifest["eingabe"] as? JSONObjekt ?? [:]
        let pflicht = s["required"] as? [String] ?? []
        return (s["properties"] as? JSONObjekt ?? [:]).keys.sorted().map { pflicht.contains($0) ? "\($0) (nötig)" : $0 }.joined(separator: ", ")
    }

    private func rechteText(_ e: HelferEintrag) -> String {
        var t: [String] = []
        if !e.konten.isEmpty { t.append("Konten: " + e.konten.joined(separator: ", ")) }
        if let n = e.rechte["netz"] as? [String], !n.isEmpty { t.append("Netz: " + n.joined(separator: ", ")) }
        if e.rechte["extern"] as? Bool == true { t.append("wirkt nach außen") }
        return t.isEmpty ? "Keine besonderen" : t.joined(separator: " · ")
    }

    private func pruefungText(_ e: HelferEintrag) -> String {
        guard e.pruefungZeit > 0 else { return e.pruefungErgebnis }
        let f = DateFormatter(); f.dateFormat = "dd.MM.yyyy, HH:mm 'Uhr'"
        return f.string(from: Date(timeIntervalSince1970: e.pruefungZeit)) + ": " + e.pruefungErgebnis
    }

    private func zustandText(_ e: HelferEintrag) -> String {
        if !e.aktiv { return "Deaktiviert" }
        switch e.zustand {
        case HelferKatalog.bereit: return "Einsatzbereit"
        case HelferKatalog.inPruefung: return "In Prüfung"
        case HelferKatalog.wartet: return "Wartet auf Freigabe"
        case HelferKatalog.fehlerhaft: return "Fehlerhaft"
        default: return "Im Papierkorb"
        }
    }

    private func farbe(_ e: HelferEintrag) -> Color {
        if !e.aktiv { return Farbe.text3 }
        switch e.zustand {
        case HelferKatalog.bereit: return Farbe.gruen
        case HelferKatalog.fehlerhaft: return Farbe.rot
        default: return Farbe.warnung
        }
    }
}

/// Alle Schlüssel an einer Stelle, jeder mit zwei kurzen Sätzen. Die Werte bleiben, wo sie sind; nur die Ansicht ist zentral.
struct KontenListe: View {
    @State private var einst = Einstellungen.shared
    @State private var stimmen = VorleseEinstellungen.shared
    @State private var katalog = HelferKatalog.shared
    @State private var sperre = TresorSperre.shared
    @State private var neuTitel = ""
    @State private var neuBeschreibung = ""
    @State private var neuDienst = ""
    @State private var entfernen: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            eintrag("anthropic", "Anthropic (Claude)", "Meldet Jarvis bei den Claude-Modellen an. Wird für Chat und Lernen mit Claude verwendet.", "Jarvis' Kopf", $einst.zugang.anthropic)
            eintrag("openai", "OpenAI (Live, GPT)", "Meldet Jarvis bei der OpenAI-Plattform an. Wird für das Live-Gespräch und GPT ohne ChatGPT-Anmeldung verwendet.", "Live-Gespräch, Jarvis' Kopf", $einst.zugang.openai)
            eintrag("gemini", "Gemini (Live)", "Meldet Jarvis bei Google AI Studio an. Wird für das Live-Gespräch mit Gemini verwendet.", "Live-Gespräch", $einst.zugang.gemini)
            eintrag("groq", "Groq (Whisper)", "Meldet Jarvis bei Groq an. Wird für die Umwandlung von Sprache in Text verwendet.", "Spracheingabe", $einst.zugang.groq)
            eintrag("google", "Google Cloud (Chirp 3 HD)", "Meldet Jarvis bei Google Cloud an. Wird für die Vorlesestimmen Chirp 3 HD verwendet.", "Vorlesen", $stimmen.zugang.google)
            eintrag("gemini_tts_frei", "Gemini TTS kostenlos", "Schlüssel eines Projekts ohne Abrechnung. Wird zuerst für die Gemini-Vorlesestimmen verwendet.", "Vorlesen", $stimmen.zugang.geminiFrei)
            eintrag("gemini_tts_bezahlt", "Gemini TTS bezahlt", "Schlüssel eines Projekts mit Abrechnung. Wird für die Gemini-Stimmen verwendet, wenn das Kontingent erschöpft ist.", "Vorlesen", $stimmen.zugang.geminiBezahlt)
            eintrag("azure", "Azure Speech", "Meldet Jarvis bei Azure Speech an. Wird für die Azure-Vorlesestimmen verwendet.", "Vorlesen", $stimmen.zugang.azure)
            eintrag("qwen", "Alibaba DashScope", "Meldet Jarvis bei Alibaba an. Wird für die Qwen-Stimmen und die eigene Stimme verwendet.", "Vorlesen", $stimmen.zugang.qwen)
            ForEach(einst.zugang.weitere.keys.sorted(), id: \.self) { k in weiterer(k) }
            if sperre.entsperrt { neuForm } else { Text("Zum Hinzufügen oder Ändern mit Touch ID entsperren.").font(.jarvis(11)).foregroundStyle(Farbe.text3) }
        }
        .alert("Schlüssel entfernen?", isPresented: Binding(get: { entfernen != nil }, set: { if !$0 { entfernen = nil } })) {
            Button("Abbrechen", role: .cancel) {}
            Button("Entfernen", role: .destructive) { if let k = entfernen { einst.zugang.weitere.removeValue(forKey: k) } }
        } message: {
            let nutzer = entfernen.map { katalog.nutzer(konto: $0) } ?? []
            Text(nutzer.isEmpty ? "Kein Helfer benutzt ihn." : "Diese Helfer funktionieren danach nicht mehr: \(nutzer.joined(separator: ", ")).")
        }
    }

    private func verwendet(_ fest: String, _ id: String) -> String {
        let helfer = id.isEmpty ? [] : katalog.nutzer(konto: id)
        return "Verwendet von: " + ([fest] + helfer).filter { !$0.isEmpty }.joined(separator: ", ")
    }

    private func eintrag(_ id: String, _ titel: String, _ zweiSaetze: String, _ fest: String, _ wert: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            TresorFeld(titel: titel, wert: wert)
            Text(zweiSaetze).font(.jarvis(12)).foregroundStyle(Farbe.text2)
            HStack {
                Text(verwendet(fest, id)).font(.jarvis(11)).foregroundStyle(Farbe.text3)
                Spacer()
                if !id.isEmpty {
                    let gesperrt = einst.zugang.gesperrtFuerHelfer.contains(id)
                    Button(gesperrt ? "Für Helfer entsperren" : "Für Helfer sperren") {
                        if gesperrt { einst.zugang.gesperrtFuerHelfer.removeAll { $0 == id } } else { einst.zugang.gesperrtFuerHelfer.append(id) }
                    }.buttonStyle(.plain).font(.jarvis(11)).foregroundStyle(gesperrt ? Farbe.warnung : Farbe.text2)
                }
            }
        }
    }

    private func weiterer(_ k: String) -> some View {
        let wert = Binding(get: { einst.zugang.weitere[k]?.wert ?? "" }, set: { einst.zugang.weitere[k]?.wert = $0 })
        let beschreibung = Binding(get: { einst.zugang.weitere[k]?.beschreibung ?? "" }, set: { einst.zugang.weitere[k]?.beschreibung = $0 })
        let k2 = einst.zugang.weitere[k] ?? WeiteresKonto()
        return VStack(alignment: .leading, spacing: 3) {
            TresorFeld(titel: k2.titel, wert: wert)
            if sperre.entsperrt {
                TextField("Zwei Sätze: was macht er, wofür?", text: beschreibung).textFieldStyle(.roundedBorder).font(.jarvis(12))
            } else { Text(k2.beschreibung).font(.jarvis(12)).foregroundStyle(Farbe.text2) }
            HStack {
                Text(verwendet(k2.dienst, k)).font(.jarvis(11)).foregroundStyle(Farbe.text3)
                Spacer()
                Button(k2.gesperrt ? "Entsperren" : "Sperren") { einst.zugang.weitere[k]?.gesperrt.toggle() }
                    .buttonStyle(.plain).font(.jarvis(11)).foregroundStyle(k2.gesperrt ? Farbe.warnung : Farbe.text2)
                if sperre.entsperrt { Button("Entfernen") { entfernen = k }.buttonStyle(.plain).font(.jarvis(11)).foregroundStyle(Farbe.text2) }
            }
        }
    }

    private var neuForm: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Neuer Schlüssel").font(.jarvis(13, .semibold))
            TextField("Name, z. B. YouTube", text: $neuTitel).textFieldStyle(.roundedBorder).font(.jarvis(12))
            TextField("Zwei Sätze: was macht er, wofür?", text: $neuBeschreibung).textFieldStyle(.roundedBorder).font(.jarvis(12))
            TextField("Dienst, z. B. YouTube Data API", text: $neuDienst).textFieldStyle(.roundedBorder).font(.jarvis(12))
            KapselKnopf(symbol: "plus", titel: "Hinzufügen") {
                let titel = neuTitel.trimmingCharacters(in: .whitespaces)
                let k = titel.lowercased().map { $0.isLetter || $0.isNumber ? String($0) : "-" }.joined()
                guard !k.isEmpty, einst.zugang.weitere[k] == nil else { return }
                einst.zugang.weitere[k] = WeiteresKonto(titel: titel, beschreibung: neuBeschreibung, dienst: neuDienst)
                neuTitel = ""; neuBeschreibung = ""; neuDienst = ""
            }
        }
    }
}
