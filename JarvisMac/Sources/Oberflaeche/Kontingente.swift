import SwiftUI

/// Einstellungen → Kontingente: Mit eigener Anmeldung (Einstellungen → Konsolen) liest der Mac die Limits selbst, sonst holt sie das Handy (`kontingente_lesen`). Der letzte Stand bleibt, wenn eine Quelle gerade nicht antwortet.
@MainActor @Observable
final class Kontingente {
    static let shared = Kontingente()
    private static let speicher = "kontingente.letzterStand"

    struct Limit: Identifiable {
        let name: String, genutzt: Double, resetSatz: String, resetAm: String, resetSek: Double, tempo: String, zuSchnell: Bool
        var id: String { name }
    }
    struct Anbieter: Identifiable {
        let id: String, titel: String, konto: String, hinweis: String, anmelden: Bool, stand: String, limits: [Limit]
    }

    var anbieter: [Anbieter] = []
    var fehler: String?
    var laedt = false
    /// Der Stand stammt vom letzten Programmlauf: „heute“ und das Tempo galten nur damals.
    var ausSpeicher = false
    /// Diese Dienste stehen gerade vom Handy in der Liste.
    var vomHandy: Set<String> = []

    /// Einzelabruf aus dem Kopf einer Karte: läuft gerade oder ging so aus.
    struct Lauf {
        let laeuft: Bool
        var erfolg = false
        var text = ""
    }
    var laeufe: [String: Lauf] = [:]
    private var einzeln = 0

    init() {
        if let t = UserDefaults.standard.string(forKey: Self.speicher), let alt = Self.lies(t) { anbieter = alt; ausSpeicher = true }
    }

    func laden(frisch: Bool = false) async {
        // Ein ausdrücklicher Wunsch wartet die laufende Abfrage ab, statt verloren zu gehen.
        if laedt && !frisch { return }
        // Auch laufende Einzelabrufe abwarten: Der Gesamtabruf arbeitet auf seiner eigenen Kopie und würde sie sonst überschreiben.
        while laedt || einzeln > 0 { try? await Task.sleep(for: .milliseconds(300)) }
        laedt = true; defer { laedt = false }
        // Ein neuer Gesamtabruf macht das Ergebnis eines früheren Einzelabrufs hinfällig.
        if frisch { laeufe = [:] }
        var stand =Dictionary(anbieter.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })
        var hier: [(id: String, titel: String)] = []
        for d in KontingentQuellen.alle {
            if await KontingentQuellen.hierMoeglich(d.id) { hier.append(d) }
        }
        var handyIds = Set(KontingentQuellen.alle.map(\.id)).subtracting(hier.map(\.id))
        // Abgemeldet und vom Handy ohne Zahlen: Der alte Stand darf nicht bleiben.
        for id in stand.keys where handyIds.contains(id) && !vomHandy.contains(id) { stand.removeValue(forKey: id) }
        func zeige() {
            anbieter = KontingentQuellen.alle.compactMap { stand[$0.id] }
            vomHandy = handyIds
        }
        // Der Mac zuerst und nebeneinander: Das Handy kann bis zu 85 Sekunden brauchen und hält hier niemanden auf.
        var handy: Task<(liste: [Anbieter]?, text: String), Never>? = handyIds.isEmpty ? nil : Task { await Self.vomHandyHolen(frisch: frisch) }
        var abgelehnt: [String] = []
        await withTaskGroup(of: Anbieter.self) { gruppe in
            for d in hier { gruppe.addTask { await KontingentQuellen.lies(d.id, titel: d.titel, frisch: frisch) } }
            for await a in gruppe {
                stand[a.id] = a
                if a.anmelden { abgelehnt.append(a.id) }
                zeige()
            }
        }
        // Abgelehnte Anmeldung am Mac: dann doch vom Handy, falls dort eine liegt.
        if handy == nil && !abgelehnt.isEmpty { handy = Task { await Self.vomHandyHolen(frisch: frisch) } }
        fehler = nil
        if let handy {
            let (liste, text) = await handy.value
            if let liste {
                for a in liste where handyIds.contains(a.id) || (abgelehnt.contains(a.id) && !a.limits.isEmpty) {
                    stand[a.id] = a
                    handyIds.insert(a.id)
                }
            } else {
                if hier.isEmpty { fehler = text.isEmpty ? "Das Handy antwortet gerade nicht." : text }
                log("fehler", "Kontingente vom Handy nicht abrufbar", wo: "Kontingente", fehler: text)
            }
        }
        zeige()
        ausSpeicher = false
        sichere()
    }

    /// Fragt nur diesen Anbieter frisch ab und ändert nur seine Karte. Als Erfolg gilt nur ein frischer Stand ohne Hinweis.
    func aktualisieren(_ id: String) async {
        if laeufe[id]?.laeuft == true { return }
        laeufe[id] = Lauf(laeuft: true)
        einzeln += 1
        while laedt { try? await Task.sleep(for: .milliseconds(300)) }
        let fehler = await einzelnHolen(id)
        einzeln -= 1
        let f = DateFormatter(); f.dateFormat = "HH:mm"
        laeufe[id] = Lauf(laeuft: false, erfolg: fehler == nil, text: fehler ?? "Aktualisiert: heute, " + f.string(from: Date()) + " Uhr")
    }

    private func einzelnHolen(_ id: String) async -> String? {
        var neu: Anbieter?
        var fehler: String?
        var ueberHandy = await !KontingentQuellen.hierMoeglich(id)
        if !ueberHandy {
            let titel = KontingentQuellen.alle.first { $0.id == id }?.titel ?? id
            let a = await KontingentQuellen.lies(id, titel: titel, frisch: true)
            neu = a
            fehler = Self.fehlertext(a)
            // Abgelehnte Anmeldung am Mac: dann doch vom Handy, falls dort eine liegt.
            ueberHandy = a.anmelden
        }
        var ausHandy = false
        if ueberHandy {
            let (liste, text) = await Self.vomHandyHolen(frisch: true, ziel: id)
            if let a = liste?.first(where: { $0.id == id }) {
                if neu == nil || !a.limits.isEmpty {
                    neu = a
                    ausHandy = true
                    // Nach Fristende schickt das Handy den letzten Stand: Der darf nicht als frisch gelten.
                    fehler = (JSON.objekt(text)?["aktualisiert"] as? Bool) == true ? Self.fehlertext(a) : "Aktualisierung fehlgeschlagen: Das Handy hat nicht rechtzeitig frisch geantwortet."
                }
            } else {
                if neu == nil { fehler = "Aktualisierung fehlgeschlagen: " + (liste != nil || text.isEmpty ? "Das Handy antwortet gerade nicht." : text) }
                log("fehler", "Kontingent vom Handy nicht abrufbar", wo: "Kontingente", fehler: text)
            }
        }
        if let neu {
            var stand = Dictionary(anbieter.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })
            stand[id] = neu
            if ausHandy { vomHandy.insert(id) } else { vomHandy.remove(id) }
            anbieter = KontingentQuellen.alle.compactMap { stand[$0.id] }
            sichere()
        }
        return fehler
    }

    private static func fehlertext(_ a: Anbieter) -> String? {
        if a.hinweis.hasPrefix("Ein Limit") { return "Nicht vollständig aktualisiert: " + a.hinweis }
        if !a.hinweis.isEmpty { return "Aktualisierung fehlgeschlagen: " + a.hinweis.replacingOccurrences(of: "Letzter Stand. ", with: "") }
        return a.limits.isEmpty ? "Aktualisierung fehlgeschlagen: Der Anbieter nennt keine Limits." : nil
    }

    private func sichere() {
        let speicherbar: [JSONObjekt] = anbieter.map { a in
            ["id": a.id, "titel": a.titel, "konto": a.konto, "hinweis": a.hinweis, "anmelden": a.anmelden, "stand": a.stand,
             "limits": a.limits.map { l in ["name": l.name, "genutzt": l.genutzt, "resetSatz": l.resetSatz, "resetAm": l.resetAm, "resetSek": l.resetSek, "tempo": l.tempo, "zuSchnell": l.zuSchnell] as JSONObjekt }]
        }
        UserDefaults.standard.set(JSON.text(["anbieter": speicherbar] as JSONObjekt), forKey: Self.speicher)
    }

    private static func vomHandyHolen(frisch: Bool, ziel: String? = nil) async -> (liste: [Anbieter]?, text: String) {
        var argumente: JSONObjekt = ["json": true]
        if frisch { argumente["frisch"] = true }
        if let ziel { argumente["anbieter"] = ziel }
        let r = await McpKlient.shared.rufe("kontingente_lesen", argumente, sekunden: 85)
        let t = McpKlient.ohneKontextHinweis(r.text)
        return (lies(t), t)
    }

    private static func lies(_ t: String) -> [Anbieter]? {
        guard let d = t.data(using: .utf8), let j = (try? JSONSerialization.jsonObject(with: d)) as? [String: Any], let liste = j["anbieter"] as? [[String: Any]] else { return nil }
        return liste.map { a in
            Anbieter(id: a["id"] as? String ?? "", titel: a["titel"] as? String ?? "", konto: a["konto"] as? String ?? "", hinweis: a["hinweis"] as? String ?? "",
                     anmelden: a["anmelden"] as? Bool ?? false, stand: a["stand"] as? String ?? "",
                     limits: (a["limits"] as? [[String: Any]] ?? []).map { l in
                         Limit(name: l["name"] as? String ?? "", genutzt: (l["genutzt"] as? NSNumber)?.doubleValue ?? 0,
                               resetSatz: l["resetSatz"] as? String ?? "", resetAm: l["resetAm"] as? String ?? "", resetSek: (l["resetSek"] as? NSNumber)?.doubleValue ?? 0,
                               tempo: l["tempo"] as? String ?? "", zuSchnell: l["zuSchnell"] as? Bool ?? false)
                     })
        }
    }
}

struct KontingenteAnsicht: View {
    @State private var k = Kontingente.shared

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack(spacing: 10) {
                KapselKnopf(symbol: "arrow.clockwise", titel: "Jetzt neu abfragen", hervorgehoben: true) { Task { await k.laden(frisch: true) } }
                if k.laedt { ProgressView().controlSize(.small) }
            }
            if let f = k.fehler { Text(k.anbieter.isEmpty ? f : "Alter Stand. " + f).font(.jarvis(13)).foregroundStyle(Farbe.warnung).fixedSize(horizontal: false, vertical: true) }
            else if k.ausSpeicher { Text("Letzter Stand, die neue Abfrage läuft.").font(.jarvis(13)).foregroundStyle(Farbe.warnung) }
            ForEach(k.anbieter) { karte($0) }
            Text("Anmelden unter Einstellungen → Konsolen. Ohne Anmeldung hier am Mac kommt der Stand vom Handy.").font(.jarvis(12)).foregroundStyle(Farbe.text3)
        }
        .task { await k.laden() }
    }

    private func zahl(_ x: Double) -> String { x.formatted(.number.precision(.fractionLength(0...1)).locale(Locale(identifier: "de_DE"))) }

    private func karte(_ a: Kontingente.Anbieter) -> some View {
        GlasKarte {
            VStack(alignment: .leading, spacing: 10) {
                let lauf = k.laeufe[a.id]
                HStack(spacing: 10) {
                    KartenTitel(symbol: "gauge.with.dots.needle.67percent", titel: a.titel)
                    Spacer()
                    aktualisierenKnopf(a, lauf)
                }
                if let lauf {
                    if lauf.laeuft { Text("Wird aktualisiert …").font(.jarvis(13)).foregroundStyle(Farbe.text2) }
                    else { Text(lauf.text).font(.jarvis(13, .semibold)).foregroundStyle(lauf.erfolg ? Farbe.haupt : Farbe.rot).fixedSize(horizontal: false, vertical: true) }
                }
                if !a.konto.isEmpty { Text(a.konto).font(.jarvis(12)).foregroundStyle(Farbe.text3) }
                ForEach(a.limits) { limit($0) }
                // Nach einem gescheiterten Einzelabruf steht der Grund schon oben; der Weg zur Anmeldung bleibt.
                if !a.hinweis.isEmpty && (a.anmelden || lauf == nil || lauf?.laeuft == true || lauf?.erfolg == true) {
                    Text(a.hinweis + (a.anmelden ? (k.vomHandy.contains(a.id) ? " Anmelden unter Einstellungen → Konsolen oder auf dem Handy." : " Anmelden unter Einstellungen → Konsolen.") : "")).font(.jarvis(12)).foregroundStyle(Farbe.warnung).fixedSize(horizontal: false, vertical: true)
                }
                if !a.stand.isEmpty { Text(a.stand).font(.jarvis(11)).foregroundStyle(Farbe.text3) }
            }
        }
    }

    @ViewBuilder
    private func aktualisierenKnopf(_ a: Kontingente.Anbieter, _ lauf: Kontingente.Lauf?) -> some View {
        if lauf?.laeuft == true {
            ProgressView().controlSize(.small).frame(width: 26, height: 24)
        } else {
            KleinKnopf(symbol: lauf == nil ? "arrow.clockwise" : (lauf?.erfolg == true ? "checkmark" : "exclamationmark.triangle.fill"), hilfe: a.titel + " aktualisieren",
                       farbe: lauf == nil ? Farbe.text2 : (lauf?.erfolg == true ? Farbe.haupt : Farbe.rot)) { Task { await k.aktualisieren(a.id) } }
        }
    }

    /// Aus dem Speicher: festes Datum statt „heute“, kein Tempo; ein inzwischen abgelaufenes Fenster gilt wieder als frei.
    private func limit(_ l: Kontingente.Limit) -> some View {
        let abgelaufen = l.resetSek > 0 && l.resetSek <= Date().timeIntervalSince1970
        let schnell = l.zuSchnell && !k.ausSpeicher && !abgelaufen
        let genutzt = min(100, max(0, abgelaufen ? 0 : l.genutzt))
        let reset = abgelaufen ? "Zurückgesetzt, neuer Verbrauch erst beim nächsten Abruf" : (k.ausSpeicher && !l.resetAm.isEmpty ? "Zurückgesetzt: " + l.resetAm : l.resetSatz)
        return VStack(alignment: .leading, spacing: 4) {
            Text(l.name).font(.jarvis(12, .semibold)).foregroundStyle(Farbe.text2)
            Text("\(zahl(genutzt)) % verbraucht").font(.jarvis(24, .bold)).foregroundStyle(schnell ? Farbe.rot : Farbe.haupt)
            balken(genutzt / 100, rot: schnell)
            Text(reset).font(.jarvis(13)).foregroundStyle(Farbe.text)
            if !l.tempo.isEmpty && !k.ausSpeicher && !abgelaufen {
                Text(schnell ? "Zu schnell: " + l.tempo : l.tempo)
                    .font(.jarvis(12, schnell ? .semibold : .regular)).foregroundStyle(schnell ? Farbe.rot : Farbe.text3)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .padding(.top, 4)
    }

    private func balken(_ anteil: Double, rot: Bool) -> some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(Farbe.glas(0.08))
                Capsule().fill(rot ? AnyShapeStyle(Farbe.rot) : AnyShapeStyle(Farbe.akzent)).frame(width: max(0, geo.size.width * min(1, max(0, anteil))))
            }
        }
        .frame(height: 8)
    }
}
