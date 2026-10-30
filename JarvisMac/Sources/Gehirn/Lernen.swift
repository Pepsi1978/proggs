import Foundation

/// Lernen am Mac: Ruht ein Gespräch 10 Minuten, prüft ein kleines Modell, ob Frank darin neue dauerhafte Angaben
/// oder ausdrückliche Entscheidungen gemacht hat, die Jarvis noch nicht kennt – und gibt sie mit jarvis_merken an
/// das gemeinsame Gedächtnis zurück. So fließt, was am Mac passiert, ins Live-Gedächtnis aller Geräte.
@MainActor
final class Lernen {
    static let shared = Lernen()
    private var zeitgeber: [UUID: Task<Void, Never>] = [:]

    func merkeAktivitaet(_ g: Gespraech) {
        // Im Kanalgespräch spricht Jarvis am Handy, nicht Frank: Daraus wird nichts über Frank gelernt.
        guard Einstellungen.shared.lernenAn, g.titel != AgentenKanal.titel else { return }
        zeitgeber[g.id]?.cancel()
        zeitgeber[g.id] = Task { [weak self] in
            try? await Task.sleep(for: .seconds(600))
            guard !Task.isCancelled else { return }
            await self?.lerne(aus: g)
        }
    }

    private func lerne(aus g: Gespraech) async {
        // Läuft gerade etwas, später noch einmal versuchen statt diese Phase zu verlieren.
        if g.arbeitet { merkeAktivitaet(g); return }
        let neu = Array(g.nachrichten.dropFirst(min(g.gelerntBis, g.nachrichten.count)))
        guard neu.contains(where: { $0.rolle == .frank }) else { return }
        let stand = g.nachrichten.count
        let verlauf = neu.suffix(30).map { "\($0.rolle == .frank ? "Frank" : "Jarvis"): \($0.text.prefix(1500))" }.joined(separator: "\n")
        let bekannt = await McpKlient.shared.text("jarvis_gemerktes_lesen", [:]) ?? ""
        let prompt = """
        Gespräch zwischen Frank und Jarvis am Mac:
        \(verlauf)

        Bereits gemerkt:
        \(bekannt.prefix(30000))

        Finde nur NEUE dauerhafte Angaben über Frank (Fakten, Vorlieben, Ziele, Regeln für Jarvis) oder AUSDRÜCKLICHE \
        Entscheidungen, die Frank selbst gesagt hat und die oben noch nicht stehen. Keine Vermutungen, keine Vorschläge von Jarvis, \
        nichts Vorübergehendes, nichts, was Jarvis im Gespräch schon selbst gemerkt hat. Antworte nur mit JSON:
        {"eintraege":[{"art":"info|regel|ziel|entscheidung|stil","text":"ein vollständiger Satz in der dritten Person"}]}
        Ist nichts Neues dabei: {"eintraege":[]}
        """
        guard let antwort = await Kurzauftrag.frage(prompt, modell: "claude-sonnet-5-5") else { return }
        defer { g.gelerntBis = stand; g.speichern() }
        guard let a = antwort.firstIndex(of: "{"), let b = antwort.lastIndex(of: "}"), a < b,
              let o = JSON.objekt(String(antwort[a...b])), let liste = o["eintraege"] as? [JSONObjekt] else { return }
        for e in liste.prefix(5) {
            guard let text = e["text"] as? String, !text.isEmpty else { continue }
            let art = e["art"] as? String ?? "info"
            let r = await McpKlient.shared.rufe("jarvis_merken", ["art": art, "text": text + " (gelernt am Mac)"])
            log("system", "Gelernt am Mac (\(art)): \(text) → \(r.fehler ? "Fehler" : "gemerkt")")
        }
        g.gelerntBis = stand
        g.speichern()
    }
}
