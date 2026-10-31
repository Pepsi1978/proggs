import SwiftUI

/// Einstellungen → Kontingente: Die Limits holt das Handy (`kontingente_lesen`), dort liegen auch die Anmeldungen. Der letzte Stand bleibt, wenn das Handy gerade nicht antwortet.
@MainActor @Observable
final class Kontingente {
    static let shared = Kontingente()
    private static let speicher = "kontingente.letzterStand"

    struct Limit: Identifiable {
        let name: String, genutzt: Double, uebrig: Double, reset: String, tempo: String, zuSchnell: Bool
        var id: String { name }
    }
    struct Anbieter: Identifiable {
        let id: String, titel: String, konto: String, hinweis: String, anmelden: Bool, stand: String, limits: [Limit]
    }

    var anbieter: [Anbieter] = []
    var fehler: String?
    var laedt = false

    init() {
        if let t = UserDefaults.standard.string(forKey: Self.speicher) { anbieter = Self.lies(t) ?? [] }
    }

    func laden(frisch: Bool = false) async {
        if laedt { return }
        laedt = true; defer { laedt = false }
        var argumente: JSONObjekt = ["json": true]
        if frisch { argumente["frisch"] = true }
        let r = await McpKlient.shared.rufe("kontingente_lesen", argumente, sekunden: 120)
        let t = McpKlient.ohneKontextHinweis(r.text)
        guard let neu = Self.lies(t) else {
            fehler = t.isEmpty ? "Das Handy antwortet gerade nicht." : t
            log("fehler", "Kontingente nicht abrufbar", wo: "Kontingente", fehler: fehler)
            return
        }
        fehler = nil
        anbieter = neu
        UserDefaults.standard.set(t, forKey: Self.speicher)
    }

    private static func lies(_ t: String) -> [Anbieter]? {
        guard let d = t.data(using: .utf8), let j = (try? JSONSerialization.jsonObject(with: d)) as? [String: Any], let liste = j["anbieter"] as? [[String: Any]] else { return nil }
        return liste.map { a in
            Anbieter(id: a["id"] as? String ?? "", titel: a["titel"] as? String ?? "", konto: a["konto"] as? String ?? "", hinweis: a["hinweis"] as? String ?? "",
                     anmelden: a["anmelden"] as? Bool ?? false, stand: a["stand"] as? String ?? "",
                     limits: (a["limits"] as? [[String: Any]] ?? []).map { l in
                         Limit(name: l["name"] as? String ?? "", genutzt: l["genutzt"] as? Double ?? 0, uebrig: l["uebrig"] as? Double ?? 0, reset: l["reset"] as? String ?? "",
                               tempo: l["tempo"] as? String ?? "", zuSchnell: l["zuSchnell"] as? Bool ?? false)
                     })
        }
    }
}

struct KontingenteAnsicht: View {
    @State private var k = Kontingente.shared

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("Wie viel deiner Abos noch übrig ist und wann es zurückgesetzt wird, alle Zeiten in deutscher Zeit. Jarvis behält das im Blick und meldet sich, wenn ein Limit vor dem Zurücksetzen leer wäre.")
                .font(.jarvis(13)).foregroundStyle(Farbe.text2).fixedSize(horizontal: false, vertical: true)
            if let f = k.fehler { Text(k.anbieter.isEmpty ? f : "Alter Stand. " + f).font(.jarvis(13)).foregroundStyle(Farbe.warnung).fixedSize(horizontal: false, vertical: true) }
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
                ForEach(a.limits) { l in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(l.name).font(.jarvis(12, .semibold)).foregroundStyle(Farbe.text2)
                        Text("\(zahl(l.uebrig)) % übrig").font(.jarvis(24, .bold)).foregroundStyle(l.zuSchnell ? Farbe.rot : Farbe.haupt)
                        balken(min(1, max(0, l.genutzt / 100)), rot: l.zuSchnell)
                        Text("Zurückgesetzt: " + l.reset).font(.jarvis(13)).foregroundStyle(Farbe.text)
                        if !l.tempo.isEmpty {
                            Text(l.zuSchnell ? "Zu schnell: " + l.tempo.replacingOccurrences(of: "ZU SCHNELL: ", with: "") : l.tempo)
                                .font(.jarvis(12, l.zuSchnell ? .semibold : .regular)).foregroundStyle(l.zuSchnell ? Farbe.rot : Farbe.text3)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                    .padding(.top, 4)
                }
                if !a.hinweis.isEmpty {
                    Text(a.hinweis + (a.anmelden ? " Anmelden auf dem Handy unter Einstellungen → Kontingente." : "")).font(.jarvis(12)).foregroundStyle(Farbe.warnung).fixedSize(horizontal: false, vertical: true)
                }
                if !a.stand.isEmpty { Text(a.stand).font(.jarvis(11)).foregroundStyle(Farbe.text3) }
            }
        }
    }

    private func balken(_ anteil: Double, rot: Bool) -> some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(Farbe.glas(0.08))
                if rot { Capsule().fill(Farbe.rot).frame(width: geo.size.width * anteil) } else { Capsule().fill(Farbe.akzent).frame(width: geo.size.width * anteil) }
            }
        }
        .frame(height: 8)
    }
}
