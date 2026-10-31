import SwiftUI

/// Einstellungen → Kontingente: Die Limits holt das Handy (`kontingente_lesen`), dort liegen auch die Anmeldungen. Der letzte Stand bleibt, wenn das Handy gerade nicht antwortet.
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

    init() {
        if let t = UserDefaults.standard.string(forKey: Self.speicher), let alt = Self.lies(t) { anbieter = alt; ausSpeicher = true }
    }

    func laden(frisch: Bool = false) async {
        // Ein ausdrücklicher Wunsch wartet die laufende Abfrage ab, statt verloren zu gehen.
        if laedt && !frisch { return }
        while laedt { try? await Task.sleep(for: .milliseconds(300)) }
        laedt = true; defer { laedt = false }
        var argumente: JSONObjekt = ["json": true]
        if frisch { argumente["frisch"] = true }
        let r = await McpKlient.shared.rufe("kontingente_lesen", argumente, sekunden: 85)
        let t = McpKlient.ohneKontextHinweis(r.text)
        guard let neu = Self.lies(t) else {
            fehler = t.isEmpty ? "Das Handy antwortet gerade nicht." : t
            log("fehler", "Kontingente nicht abrufbar", wo: "Kontingente", fehler: fehler)
            return
        }
        fehler = nil
        anbieter = neu
        ausSpeicher = false
        UserDefaults.standard.set(t, forKey: Self.speicher)
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
            Text("Wie viel deiner Abos verbraucht ist und wann es zurückgesetzt wird, alle Zeiten in deutscher Zeit. Jarvis behält das im Blick und meldet sich, wenn ein Limit vor dem Zurücksetzen leer wäre.")
                .font(.jarvis(13)).foregroundStyle(Farbe.text2).fixedSize(horizontal: false, vertical: true)
            if let f = k.fehler { Text(k.anbieter.isEmpty ? f : "Alter Stand. " + f).font(.jarvis(13)).foregroundStyle(Farbe.warnung).fixedSize(horizontal: false, vertical: true) }
            else if k.ausSpeicher { Text("Letzter Stand, die neue Abfrage läuft.").font(.jarvis(13)).foregroundStyle(Farbe.warnung) }
            ForEach(k.anbieter) { karte($0) }
            HStack(spacing: 10) {
                KapselKnopf(symbol: "arrow.clockwise", titel: "Jetzt neu abfragen", hervorgehoben: true) { Task { await k.laden(frisch: true) } }
                if k.laedt { ProgressView().controlSize(.small) }
            }
            Text("Die Anmeldungen liegen auf dem Handy: Einstellungen → Kontingente.").font(.jarvis(12)).foregroundStyle(Farbe.text3)
        }
        .task { await k.laden() }
    }

    private func zahl(_ x: Double) -> String { x.formatted(.number.precision(.fractionLength(0...1)).locale(Locale(identifier: "de_DE"))) }

    private func karte(_ a: Kontingente.Anbieter) -> some View {
        GlasKarte {
            VStack(alignment: .leading, spacing: 10) {
                KartenTitel(symbol: "gauge.with.dots.needle.67percent", titel: a.titel)
                if !a.konto.isEmpty { Text(a.konto).font(.jarvis(12)).foregroundStyle(Farbe.text3) }
                ForEach(a.limits) { limit($0) }
                if !a.hinweis.isEmpty {
                    Text(a.hinweis + (a.anmelden ? " Anmelden auf dem Handy unter Einstellungen → Kontingente." : "")).font(.jarvis(12)).foregroundStyle(Farbe.warnung).fixedSize(horizontal: false, vertical: true)
                }
                if !a.stand.isEmpty { Text(a.stand).font(.jarvis(11)).foregroundStyle(Farbe.text3) }
            }
        }
    }

    /// Aus dem Speicher: festes Datum statt „heute“, kein Tempo; ein inzwischen abgelaufenes Fenster gilt wieder als frei.
    private func limit(_ l: Kontingente.Limit) -> some View {
        let abgelaufen = l.resetSek > 0 && l.resetSek <= Date().timeIntervalSince1970
        let schnell = l.zuSchnell && !k.ausSpeicher && !abgelaufen
        let genutzt = min(100, max(0, abgelaufen ? 0 : l.genutzt))
        let reset = abgelaufen ? "Inzwischen zurückgesetzt" : (k.ausSpeicher && !l.resetAm.isEmpty ? "Zurückgesetzt: " + l.resetAm : l.resetSatz)
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
