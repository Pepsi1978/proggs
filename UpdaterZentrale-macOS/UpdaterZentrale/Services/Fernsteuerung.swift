import Foundation

/// Fernsteuerung aus Jarvis (Karte „Mac-Update-Zentrale“ am Handy und Tablet) über die Mac-Brücke
/// (`Jarvis/werkzeuge/pc-bruecke/jarvis_pc_bruecke.py`). Dateibasiert, damit die laufende Einzelinstanz erreichbar ist:
///
/// * Die Brücke legt je Auftrag `fern/auftraege/<kennung>.json` ab (`{"kennung", "art"}`, art = pruefen,
///   unkritisch oder kritisch). Die App liest den Ordner jede Sekunde, führt jede Kennung höchstens einmal aus und löscht die Datei.
/// * Die App schreibt `fern/stand.json`: Prüf- und Installationsstatus, offene Updates in zwei Gruppen (CLI-kritisch nach
///   `ProgrammEintrag.istCliKritisch`), offene Fenster am Mac (lokale Freigabe), nötige Neustarts und den Fernvorgang samt Ergebnis.
///   Nur kurze Texte, keine Protokolle und keine Zugangsdaten; Fehler gehen wie bisher selbst an Claude Code.
///
/// Doppelte Läufe verhindert die App selbst: Während ein Fernvorgang oder irgendein anderer Lauf (`Laufkoordination`) läuft,
/// wird ein neuer Installationsauftrag abgewiesen. Ein Prüfauftrag während einer laufenden Prüfung hängt sich an sie an.
/// Erfolg meldet der Vorgang erst nach dem Lauf aus den Berichten der Karten (Fingerabdruck geprüft: „Erfolgreich“).
@MainActor
final class Fernsteuerung {
    static let shared = Fernsteuerung()

    private let ordner = (Pfade.benutzerOrdner as NSString).appendingPathComponent("fern")
    private var auftraegeOrdner: String { (ordner as NSString).appendingPathComponent("auftraege") }
    private var standDatei: String { (ordner as NSString).appendingPathComponent("stand.json") }
    private var vorgangDatei: String { (ordner as NSString).appendingPathComponent("vorgang.json") }

    private weak var modell: HauptViewModel?
    private var takt: Timer?
    /// Der laufende oder letzte Fernvorgang (überlebt einen Neustart der App, etwa nach dem Selbst-Update).
    private var vorgang: [String: Any]?
    private var abgewiesen: [String: Any]?
    private var erledigt: [String] = []
    private var fortsetzungOffen = false
    private var zuletztGeschrieben = ""
    private var zuletztZeit = Date.distantPast

    func starten(_ modell: HauptViewModel) {
        self.modell = modell
        try? FileManager.default.createDirectory(atPath: auftraegeOrdner, withIntermediateDirectories: true)
        if let daten = FileManager.default.contents(atPath: vorgangDatei),
           let alt = (try? JSONSerialization.jsonObject(with: daten)) as? [String: Any] {
            erledigt = alt["erledigt"] as? [String] ?? []
            var v = alt["vorgang"] as? [String: Any]
            if v?["phase"] as? String == "laeuft" {
                // Die App wurde mitten im Vorgang beendet: Nur nach einem Selbst-Update setzt sie ihn fort.
                if (v?["ids"] as? [String])?.isEmpty == false { fortsetzungOffen = true } else {
                    v?["phase"] = "unklar"
                    v?["text"] = "Die Update-Zentrale wurde während des Vorgangs neu gestartet. Bitte neu prüfen."
                    v?["ende"] = Date().timeIntervalSince1970
                }
            }
            vorgang = v
        }
        // .common: Der Takt läuft auch, solange am Mac ein Ja/Nein-Fenster offen ist.
        let t = Timer(timeInterval: 1, repeats: true) { _ in MainActor.assumeIsolated { Fernsteuerung.shared.tick() } }
        RunLoop.main.add(t, forMode: .common)
        takt = t
        tick()
    }

    func sofortMelden() { standSchreiben(erzwingen: true) }

    private func tick() {
        auftraegeLesen()
        if fortsetzungOffen, let modell, modell.erstePruefungFertig, !modell.belegt {
            fortsetzungOffen = false
            abschliessen()
        }
        standSchreiben(erzwingen: false)
    }

    // MARK: - Aufträge

    private func auftraegeLesen() {
        guard let namen = try? FileManager.default.contentsOfDirectory(atPath: auftraegeOrdner) else { return }
        for name in namen.sorted() where name.hasSuffix(".json") {
            let pfad = (auftraegeOrdner as NSString).appendingPathComponent(name)
            let daten = FileManager.default.contents(atPath: pfad)
            try? FileManager.default.removeItem(atPath: pfad)
            guard let daten, let a = (try? JSONSerialization.jsonObject(with: daten)) as? [String: Any],
                  let kennung = a["kennung"] as? String, !kennung.isEmpty, let art = a["art"] as? String else { continue }
            guard !erledigt.contains(kennung) else { continue }
            erledigt.append(kennung)
            erledigt = Array(erledigt.suffix(50))
            Diagnose.ereignis(.info, "fern", "fern.auftrag", "Auftrag aus Jarvis: \(art) (\(kennung))")
            Task { await ausfuehren(kennung, art) }
        }
    }

    private var vorgangLaeuft: Bool { vorgang?["phase"] as? String == "laeuft" }

    private func abweisen(_ kennung: String, _ art: String, _ text: String) {
        abgewiesen = ["kennung": kennung, "art": art, "text": text, "zeit": Date().timeIntervalSince1970]
        Diagnose.ereignis(.warnung, "fern", "fern.abgewiesen", text)
        sichern()
        standSchreiben(erzwingen: true)
    }

    private func beginnen(_ kennung: String, _ art: String, _ text: String) {
        vorgang = ["kennung": kennung, "art": art, "phase": "laeuft", "text": text, "beginn": Date().timeIntervalSince1970]
        sichern()
        standSchreiben(erzwingen: true)
    }

    private func setzen(_ werte: [String: Any]) {
        for (k, v) in werte { vorgang?[k] = v }
        sichern()
        standSchreiben(erzwingen: true)
    }

    /// Wartet, bis kein Lauf mehr den Besitz hält und die Anfangsprüfung durch ist (höchstens `sekunden`).
    private func frei(_ sekunden: Double) async -> Bool {
        let ende = Date().addingTimeInterval(sekunden)
        while Date() < ende {
            if let m = modell, !m.belegt, m.erstePruefungFertig { return true }
            try? await Task.sleep(nanoseconds: 1_000_000_000)
        }
        return false
    }

    private func ausfuehren(_ kennung: String, _ art: String) async {
        guard let modell else { return }
        switch art {
        case "pruefen":
            if vorgangLaeuft && vorgang?["art"] as? String == "pruefen" {
                // Hängt sich an die laufende Prüfung; die Kennung steht im Stand, damit der Relay den Auftrag als angekommen sieht.
                return setzen(["angehaengt": kennung])
            }
            if vorgangLaeuft || modell.laufArt == "installation" {
                abweisen(kennung, art, "Am Mac läuft gerade eine Installation – geprüft wird danach von selbst.")
                return
            }
            let anhaengen = modell.laufArt == "pruefung" || !modell.erstePruefungFertig
            beginnen(kennung, art, modell.belegt && !anhaengen ? "Wartet auf das Ende des laufenden Vorgangs am Mac …" : "Prüft alle Programme …")
            let vorher = modell.pruefungEnde
            // Läuft schon eine Prüfung (etwa die Anfangsprüfung nach dem Start durch die Brücke), ist sie die frische;
            // hält ein anderer Vorgang (Update einer Karte) den Besitz, erst sein Ende abwarten.
            guard await frei(900) else { return setzen(["phase": "fehler", "text": "Am Mac lief nach 15 Minuten noch ein anderer Vorgang – nicht geprüft.", "ende": jetzt]) }
            if !anhaengen || modell.pruefungEnde == vorher {
                setzen(["text": "Prüft alle Programme …"])
                await modell.allePruefen()
            }
            guard let ende = modell.pruefungEnde, ende != vorher else {
                return setzen(["phase": "fehler", "text": "Die Prüfung kam am Mac nicht zustande: " + modell.kopfStatus, "ende": jetzt])
            }
            setzen(["phase": "fertig", "text": modell.kopfStatus, "ende": jetzt])
        case "unkritisch", "kritisch":
            let kritisch = art == "kritisch"
            if vorgangLaeuft || modell.laufArt == "installation" {
                abweisen(kennung, art, "Am Mac läuft bereits ein Vorgang – es wird nichts doppelt installiert.")
                return
            }
            beginnen(kennung, art, modell.belegt || !modell.erstePruefungFertig ? "Wartet auf das Ende der laufenden Prüfung …" : "Startet …")
            guard await frei(900) else {
                return setzen(["phase": "fehler", "text": "Am Mac lief nach 15 Minuten immer noch ein anderer Vorgang – nichts installiert.", "ende": jetzt])
            }
            let plan = modell.fernPlan(kritisch: kritisch)
            guard !plan.isEmpty else {
                return setzen(["phase": "fertig", "ids": [String](), "ergebnisse": [[String: Any]](),
                               "text": kritisch ? "Kein CLI-kritisches Update offen." : "Kein unkritisches Update offen.", "ende": jetzt])
            }
            setzen(["ids": plan.map(\.programm.eintrag.id), "text": "Installiert \(plan.count) Update\(plan.count == 1 ? "" : "s") …"])
            await modell.fernInstallieren(plan, kritisch: kritisch)
            abschliessen()
        default:
            abweisen(kennung, art, "Unbekannter Auftrag.")
        }
    }

    private var jetzt: Double { Date().timeIntervalSince1970 }

    /// Wertet die Berichte der Karten aus, die seit Beginn des Vorgangs geschrieben wurden.
    private func abschliessen() {
        guard let modell, let ids = vorgang?["ids"] as? [String] else { return }
        let beginn = Date(timeIntervalSince1970: vorgang?["beginn"] as? Double ?? 0)
        var ergebnisse: [[String: Any]] = []
        var ok = 0, neustart: [String] = []
        for id in ids {
            guard let p = modell.programme.first(where: { $0.eintrag.id == id }) else {
                ergebnisse.append(["name": id, "ok": false, "text": "nicht mehr im Katalog – Ergebnis unbekannt"])
                continue
            }
            var e: [String: Any] = ["name": p.name]
            if let b = p.letzterBericht, b.zeit >= beginn.addingTimeInterval(-5) {
                switch b.ergebnis {
                case .erfolgreich, .bereitsAktuell: e["ok"] = true; e["text"] = b.versionNachher.istLeer ? "aktualisiert" : "jetzt " + b.versionNachher; ok += 1
                case .ausstehend: e["ok"] = true; e["text"] = "installiert, wird nach einem Neustart des Programms aktiv"; neustart.append(p.name); ok += 1
                case .abgebrochen, .fehlgeschlagen, .nichtVerifiziert: e["ok"] = false; e["text"] = HauptViewModel.grund(b)
                }
            } else {
                e["ok"] = false; e["text"] = "nicht gelaufen – das Programm war beschäftigt oder das Update wurde nicht gestartet"
            }
            ergebnisse.append(e)
        }
        let phase = ok == ids.count ? "fertig" : (ok > 0 ? "teilweise" : "fehler")
        let text = phase == "fertig"
            ? (ids.count == 1 ? "Update bestätigt installiert." : "Alle \(ids.count) Updates bestätigt installiert.")
            : "\(ok) von \(ids.count) Updates installiert."
        setzen(["phase": phase, "text": text + (neustart.isEmpty ? "" : " Neustart nötig: " + neustart.joined(separator: ", ") + "."),
                "ergebnisse": ergebnisse, "neustart": neustart, "ende": jetzt])
    }

    private func sichern() {
        var d: [String: Any] = ["erledigt": erledigt]
        if let vorgang { d["vorgang"] = vorgang }
        schreiben(d, vorgangDatei)
    }

    // MARK: - Stand

    private func kurz(_ text: String, _ n: Int) -> String {
        let t = text.trimmingCharacters(in: .whitespacesAndNewlines)
        return t.count > n ? String(t.prefix(n - 1)) + "…" : t
    }

    private func freigabe() -> String {
        if let titel = Dialoge.offenerTitel { return "Fenster am Mac offen: „\(titel)“" }
        guard let modell else { return "" }
        for p in modell.programme where p.istBeschaeftigt {
            let text = p.protokoll.hasSuffix("\n") ? String(p.protokoll.dropLast()) : p.protokoll
            let letzte = text.range(of: "\n", options: .backwards).map { String(text[$0.upperBound...]) } ?? text
            if letzte.contains("Ja/Nein") { return "\(p.name): Ja/Nein-Fenster am Mac beantworten" }
            if letzte.localizedCaseInsensitiveContains("passwort") { return "\(p.name): Mac-Passwort am Mac eingeben" }
        }
        return ""
    }

    private func standSchreiben(erzwingen: Bool) {
        guard let modell else { return }
        let liste: [[String: Any]] = modell.programme.compactMap { p in
            let fehler = p.zustand == .fehler
            guard p.hatUpdate || fehler || p.istBeschaeftigt || p.uebernahmeOffen else { return nil }
            var e: [String: Any] = [
                "id": p.eintrag.id, "name": p.name, "inst": p.installierteVersion, "verf": p.verfuegbareVersion,
                "kritisch": p.eintrag.istCliKritisch, "update": p.hatUpdate, "fehler": fehler, "laeuft": p.istBeschaeftigt,
                "hauptversion": p.istHauptversionsSprung, "neustart": p.uebernahmeOffen,
                "status": kurz(p.statusText, 160)
            ]
            if let grund = p.eintrag.cliKritischGrund { e["grund"] = kurz(grund, 140) }
            return e
        }
        let f = modell.fortschritt
        var d: [String: Any] = [
            "v": 1, "pid": Int(ProcessInfo.processInfo.processIdentifier),
            "version": Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "?",
            "laeuft": modell.belegt, "art": modell.laufArt ?? "", "bereit": modell.erstePruefungFertig,
            "kopf": kurz(modell.kopfStatus, 220), "detail": kurz(modell.laufDetail, 220),
            "fortschritt": [f.fertig, f.gesamt], "freigabe": freigabe(),
            "anzahl": modell.programme.count, "programme": liste
        ]
        if let e = modell.pruefungEnde { d["geprueft"] = e.timeIntervalSince1970 }
        if let vorgang { d["vorgang"] = vorgang }
        if let abgewiesen { d["abgewiesen"] = abgewiesen }
        let vergleich = (try? JSONSerialization.data(withJSONObject: d, options: [.sortedKeys])).map { String(decoding: $0, as: UTF8.self) } ?? ""
        // Unverändert nur alle 5 s neu (Lebenszeichen für die Brücke), sonst sofort.
        guard erzwingen || vergleich != zuletztGeschrieben || Date().timeIntervalSince(zuletztZeit) >= 5 else { return }
        zuletztGeschrieben = vergleich
        zuletztZeit = Date()
        d["zeit"] = Date().timeIntervalSince1970
        schreiben(d, standDatei)
    }

    private func schreiben(_ d: [String: Any], _ pfad: String) {
        guard let daten = try? JSONSerialization.data(withJSONObject: d, options: [.sortedKeys]) else { return }
        let zwischen = pfad + ".neu"
        guard FileManager.default.createFile(atPath: zwischen, contents: daten) else { return }
        _ = try? FileManager.default.replaceItemAt(URL(fileURLWithPath: pfad), withItemAt: URL(fileURLWithPath: zwischen))
        if FileManager.default.fileExists(atPath: zwischen) { try? FileManager.default.moveItem(atPath: zwischen, toPath: pfad) }
    }
}
