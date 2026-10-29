import Foundation

/// Modelle, Stimmen und Anweisungen des Live-Gesprächs – Werte wie in der Handy-App (Jarvis/live/LiveOptionen.kt, Stand 28.10.2026).
enum LiveOptionen {
    struct Wahl: Identifiable, Hashable { let id: String; let name: String }

    static let openAIModelle: [Wahl] = [
        .init(id: "gpt-live-1", name: "GPT-Live 1 (neuestes in der API)"),
        .init(id: "gpt-realtime-2.1", name: "Realtime 2.1"),
        .init(id: "gpt-realtime-2.1-mini", name: "Realtime 2.1 mini"),
    ]
    static let geminiModelle: [Wahl] = [
        .init(id: "gemini-3.8-live", name: "Gemini 3.8 Live"),
        .init(id: "gemini-3.8-live-extended-thinking", name: "Gemini 3.8 Live Extended Thinking"),
        .init(id: "gemini-3.1-flash-live-preview", name: "Gemini 3.1 Flash Live"),
    ]

    /// GPT-Live-Stimmen (API-Referenz, 28.10.2026); Nuri, Juni und Flitz sind die deutschen.
    static let liveStimmen: [Wahl] = [
        ("nuri", "Nuri (Deutsch, weiblich)"), ("juni", "Juni (Deutsch, weiblich)"), ("flitz", "Flitz (Deutsch, männlich)"),
        ("marin", "Marin (Standard)"), ("cedar", "Cedar"), ("alloy", "Alloy"), ("ash", "Ash"), ("ballad", "Ballad"), ("coral", "Coral"),
        ("echo", "Echo"), ("sage", "Sage"), ("shimmer", "Shimmer"), ("verse", "Verse"),
    ].map { Wahl(id: $0.0, name: $0.1) }
    /// Realtime-Stimmen.
    static let realtimeStimmen: [Wahl] = [
        ("marin", "Marin"), ("cedar", "Cedar"), ("alloy", "Alloy"), ("ash", "Ash"), ("ballad", "Ballad"),
        ("coral", "Coral"), ("echo", "Echo"), ("sage", "Sage"), ("shimmer", "Shimmer"), ("verse", "Verse"),
    ].map { Wahl(id: $0.0, name: $0.1) }
    /// Die 30 Stimmen der Gemini Live API.
    static let geminiStimmen: [Wahl] = [
        ("Charon", "sachlich, männlich"), ("Puck", "munter, männlich"), ("Kore", "bestimmt, weiblich"), ("Zephyr", "hell, weiblich"),
        ("Fenrir", "aufgeweckt, männlich"), ("Leda", "jugendlich, weiblich"), ("Orus", "bestimmt, männlich"), ("Aoede", "locker, weiblich"),
        ("Callirrhoe", "gelassen, weiblich"), ("Autonoe", "klar, weiblich"), ("Enceladus", "hauchig, männlich"), ("Iapetus", "deutlich, männlich"),
        ("Umbriel", "gelassen, männlich"), ("Algieba", "weich, männlich"), ("Despina", "weich, weiblich"), ("Erinome", "deutlich, weiblich"),
        ("Algenib", "rau, männlich"), ("Rasalgethi", "informativ, männlich"), ("Laomedeia", "beschwingt, weiblich"), ("Achernar", "sanft, weiblich"),
        ("Alnilam", "fest, männlich"), ("Schedar", "ausgeglichen, männlich"), ("Gacrux", "reif, weiblich"), ("Pulcherrima", "direkt, weiblich"),
        ("Achird", "freundlich, männlich"), ("Zubenelgenubi", "ungezwungen, männlich"), ("Vindemiatrix", "behutsam, weiblich"), ("Sadachbia", "lebhaft, männlich"),
        ("Sadaltager", "kenntnisreich, männlich"), ("Sulafat", "warm, weiblich"),
    ].map { Wahl(id: $0.0, name: "\($0.0) · \($0.1)") }

    /// Gültiges OpenAI-Modell (unbekannte Werte → GPT-Live 1).
    static func openAIModell(_ wert: String) -> String { openAIModelle.contains { $0.id == wert } ? wert : "gpt-live-1" }
    /// Gültiges Gemini-Modell (unbekannte Werte → Gemini 3.8 Live, wie das Handy-Profil „Gemini 3.8 Live“).
    static func geminiModell(_ wert: String) -> String { geminiModelle.contains { $0.id == wert } ? wert : "gemini-3.8-live" }

    static func openAIStimme(_ wert: String, modell: String) -> String {
        let liste = modell.hasPrefix("gpt-live") ? liveStimmen : realtimeStimmen
        return liste.contains { $0.id == wert } ? wert : (modell.hasPrefix("gpt-live") ? "flitz" : "cedar")
    }
    static func geminiStimme(_ wert: String) -> String { geminiStimmen.contains { $0.id == wert } ? wert : "Charon" }

    /// Gemini: Denkstufen je Modell (Handy: geminiStufen).
    static func geminiDenkstufe(_ modell: String) -> String? {
        switch modell {
        case "gemini-3.8-live-extended-thinking": return "LOW"
        case "gemini-3.1-flash-live-preview": return "MINIMAL"
        default: return nil
        }
    }

    static let begruessung = "Begrüße Frank kurz auf Deutsch, höchstens ein Satz, dann hör zu."
    static let dauertNoch = "Das dauert noch etwas. Ich sage dir die Antwort, sobald sie da ist, auch wenn unser Gespräch bis dahin endet. Sie steht dann auch im Chat."

    /// Neutrale Signale, die Gemini sagen darf, während Jarvis arbeitet.
    static let signale = ["Mhm", "Okay", "Alles klar", "Moment", "Einen Moment", "Augenblick", "Ich schau nach", "Lass mich kurz nachsehen"]

    static func jetzt() -> String {
        let f = DateFormatter(); f.locale = Locale(identifier: "de_DE"); f.dateFormat = "EEEE, d. MMMM yyyy, HH:mm"
        return f.string(from: Date()) + " Uhr."
    }

    private static let bereiche = "Termine, Dienstplan, Aufgaben, Wecker, Timer, Biodaten, Schlaf, Ideen, Tagebuch, Mails, Wetter, Fahrzeit, Gedächtnis, " +
        "Ablage, Programmieraufträge, Recherchen und alles, was am Mac passieren soll (Programme öffnen, Befehle, Dateien, Terminal, OpenLauncher, Updates, Bildschirm)"

    /// Anweisung fürs Sprachmodell. [gptLive]: delegiert (Client-Delegation), sonst Funktion jarvis_fragen. [nurStimme]: Gemini „Jarvis direkt“.
    static func anweisung(gptLive: Bool, nurStimme: Bool) -> String {
        if nurStimme {
            return "# Rolle\nDu bist die Stimme von Jarvis, Franks persönlichem Assistenten auf seinem Mac, im Live-Sprachgespräch. " +
                "Inhaltlich antwortet immer Jarvis: Die App gibt jeden Beitrag von Frank an Jarvis weiter. Hörbar sind Jarvis' Antworten in deinen Worten " +
                "und kurze neutrale Signale; eigene Sachantworten bleiben stumm.\n" +
                "# Sprache\nSprich immer Deutsch. Natürlich, warm und flüssig: kurze Sätze, keine Aufzählungszeichen, keine Formatierung, keine Links und keine Kennungen vorlesen.\n" +
                "# Franks Beiträge\nAuf alles, was Frank sagt, auch Smalltalk, ein Danke oder eine bloße Mitteilung, rufst du sofort jarvis_fragen mit seinen Worten auf. " +
                "Antworte nie selbst inhaltlich, auch nicht, wenn du die Antwort zu wissen glaubst. Davor darfst du höchstens ein kurzes neutrales Signal sagen, nur eines von: " +
                signale.map { "„\($0)“" }.joined(separator: ", ") + ". Oft passt gar keins. Nie zweimal dasselbe hintereinander, " +
                "kein Ja oder Nein, keine Wartefloskeln wiederholen und nichts von der Antwort vorwegnehmen.\n" +
                "# Antworten von Jarvis\nDas Ergebnis von jarvis_fragen und jede Nachricht, die mit „" + JarvisWeiche.marke.trimmingCharacters(in: .whitespaces) + "“ beginnt, ist Jarvis' Antwort an Frank. " +
                "Sprich sie sofort, ohne die Markierung und ohne Einleitung wie „Jarvis sagt“. Du musst sie nicht wörtlich vorlesen: Formuliere ihren Kern natürlich " +
                "wie im Gespräch und verbinde ihn gern mit einem kurzen Übergang wie „Also“ oder „Schau mal“. Alle Fakten, Zahlen, Namen, Zeiten, Einschränkungen, Unsicherheiten, " +
                "Rückfragen und Bestätigungen bleiben dabei genau erhalten. Füge keine eigenen Fakten, Ratschläge oder Bewertungen hinzu, und bestätige nur, was Jarvis bestätigt. " +
                "Auf eine solche Nachricht rufst du nie jarvis_fragen auf.\n" +
                "# Ausnahme\nNur die kurze Begrüßung zu Beginn sprichst du selbst.\n" +
                "# Unterbrechen\nFrank kann dich jederzeit unterbrechen; dann hörst du sofort auf. Eine unterbrochene Antwort setzt du nicht von dir aus fort; will Frank den Rest hören, kommt er von Jarvis.\n" +
                "# Regeln\nErfinde nie Daten. Nenne nie Quellen oder Webadressen. Behaupte nie selbst, etwas erledigt oder das Gespräch beendet zu haben; das sagt nur Jarvis.\n" +
                "Jetzt ist " + jetzt()
        }
        return "# Rolle\nDu bist die Stimme von Jarvis, Franks persönlichem Assistenten, hier auf seinem Mac im Live-Sprachgespräch. " +
            "Du selbst weißt nichts über Franks Daten und kannst am Mac nichts tun; das macht Jarvis.\n" +
            "# Sprache\nSprich immer Deutsch, auch wenn Frank kurz Englisch spricht. Natürlich, warm und knapp wie ein guter Gesprächspartner: kurze Sätze, " +
            "keine Aufzählungszeichen, keine Formatierung, keine Links, keine Quellen und keine Kennungen vorlesen.\n" +
            "# Unterbrechen\nFrank kann dich jederzeit unterbrechen; dann hörst du sofort auf und gehst auf ihn ein. Lass ihm Denkpausen.\n" +
            (gptLive
                ? "# Nachschlagen\nFür alles, was Franks Daten, Apps oder Aktionen betrifft (\(bereiche)) und für aktuelle Fragen aus dem Internet delegierst du, bevor du antwortest. " +
                  "Sag dabei genau einen kurzen Satz wie „Moment, ich schaue nach“. Gib danach den Kern von Jarvis' Antwort natürlich wieder, ohne Fakten zu verändern.\n"
                : "# Werkzeug\nFür alles, was Franks Daten, Apps oder Aktionen betrifft (\(bereiche)) und für aktuelle Fragen aus dem Internet rufst du jarvis_fragen mit dem vollständigen " +
                  "Auftrag in Franks Worten auf, bevor du antwortest. Sag dabei genau einen kurzen Satz wie „Moment, ich schaue nach“.\n") +
            "# Beenden\nWill Frank das Gespräch beenden, etwa „Gespräch beenden“ oder „Tschüss Jarvis“, sag höchstens „Bis später“; die App schließt die Verbindung.\n" +
            "# Regeln\nErfinde nie Daten. Melde etwas erst als erledigt, wenn das Ergebnis von Jarvis da ist.\n" +
            "Jetzt ist " + jetzt()
    }

    static func jarvisWerkzeugBeschreibung() -> String {
        "Fragt Jarvis mit allen Werkzeugen und Franks Daten (Handy-Apps und Mac): " + bereiche +
            ". Gib den Auftrag vollständig in Franks Worten weiter."
    }

    /// Sätze, mit denen Frank das Live-Gespräch beendet.
    static func istBeenden(_ text: String) -> Bool {
        let t = text.lowercased().trimmingCharacters(in: .whitespacesAndNewlines.union(.punctuationCharacters))
        let muster = ["gespräch beenden", "beende das gespräch", "beende das live-gespräch", "live-gespräch beenden", "leg auf",
                      "tschüss jarvis", "tschüs jarvis", "ciao jarvis", "bis später jarvis", "auf wiedersehen jarvis", "sprachsession beenden"]
        return muster.contains { t.contains($0) } || t == "tschüss" || t == "tschüs"
    }
}
