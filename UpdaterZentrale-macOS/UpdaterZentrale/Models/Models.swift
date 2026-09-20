import Foundation

/// 1:1-Port von Models/ProgrammEintrag.cs, Models/PruefErgebnis.cs und Models/UpdateBericht.cs.
/// Die Feldnamen bleiben deutsch und identisch zur Windows-Fassung, damit programs.json auf beiden
/// Plattformen dasselbe Schema hat und ein Eintrag von Hand uebertragbar bleibt.

// MARK: - Katalog-Eintrag

/// Ein Katalog-Eintrag aus programs.json. Ein neues Programm ist ein weiteres Objekt hier -- nie
/// eine Code-Aenderung -- weshalb jedes anbieter-spezifische Feld auf diesem EINEN Typ liegt statt
/// in einer Klassenhierarchie.
struct ProgrammEintrag: Codable {
    var id: String = ""
    var name: String = ""
    var gruppe: String = "Weitere"
    var beschreibung: String = ""

    /// brew | cli | reposkript
    var art: String = "brew"

    // ---- brew (Gegenstueck zu winget/store unter Windows) ----

    /// Name des Homebrew-Casks, z. B. "lm-studio".
    var cask: String?

    /// Das App-Buendel, dessen Info.plist die installierte Version traegt.
    /// Ersetzt den Windows-Weg ueber `winget list` bzw. `Get-AppxPackage`: auf dem Mac ist die
    /// Info.plist die Wahrheit, auch wenn die App gar nicht ueber brew installiert wurde.
    var appPfad: String?

    /// Bundle-Kennung, z. B. "com.anthropic.claudefordesktop". Fuer Starten und Beenden.
    var bundleId: String?

    // ---- cli ----

    var exePfad: String = ""
    var versionsArgumente: String?
    var updateArgumente: String?
    var pruefArgumente: String?
    var npmPaket: String?
    var startArgumente: String?

    /// Die Pruefausgabe listet ihre Updates als "alt → neu"; die Pfeile zu zaehlen sagt, wie viele.
    var pfeilZaehlen: Bool = false

    var zeitlimitMinuten: Int = 20

    // ---- reposkript ----

    var repoOrdner: String?
    var skript: String?
    var skriptArgumente: String?

    /// Praefix der Statuszeile des Skripts, z. B. "LAUNCHER_UPDATE_STATUS=".
    /// LEER lassen, wenn das Skript keine solche Zeile ausgibt (rebuild-overlay.sh auf dem Mac):
    /// dann fragt die App selbst vor dem Lauf nach und wertet Exit-Code und Schlusszeile aus.
    var statusPraefix: String?

    /// Quelle der Soll-Version: unter Windows die csproj, auf dem Mac die Info.plist der Quelle.
    var projektDatei: String?

    var dialogWartezeitSekunden: Int = 300

    // ---- Prozesse ----

    var prozesse: [String] = []
    var helferProzesse: [String] = []
    var beendenVorUpdate: Bool = false
    var neuStartenNachUpdate: Bool = false

    var hinweis: String?
    var akzent: String = "#7C5CFF"

    /// Alle Prozessnamen -- Haupt- und Helferprogramme.
    var alleProzesse: [String] { prozesse + helferProzesse }

    /// True, wenn der Eintrag ein App-Buendel meint statt einer nackten Programmdatei.
    var istAppBuendel: Bool {
        guard let appPfad, !appPfad.isEmpty else { return false }
        return true
    }

    /// Auf Nachfrage aufgeloester Pfad des App-Buendels bzw. der Programmdatei.
    var zielPfad: String {
        if let appPfad, !appPfad.isEmpty { return Pfade.aufloesen(appPfad) }
        return Pfade.aufloesen(exePfad)
    }

    enum CodingKeys: String, CodingKey {
        case id, name, gruppe, beschreibung, art
        case cask, appPfad, bundleId
        case exePfad, versionsArgumente, updateArgumente, pruefArgumente, npmPaket, startArgumente
        case pfeilZaehlen, zeitlimitMinuten
        case repoOrdner, skript, skriptArgumente, statusPraefix, projektDatei, dialogWartezeitSekunden
        case prozesse, helferProzesse, beendenVorUpdate, neuStartenNachUpdate
        case hinweis, akzent
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        // Jedes Feld einzeln mit Standardwert: ein fehlendes Feld in programs.json darf den
        // gesamten Katalog nicht unlesbar machen (genau das leistet PropertyNameCaseInsensitive
        // plus Standardwerte unter System.Text.Json).
        id = (try? c.decode(String.self, forKey: .id)) ?? ""
        name = (try? c.decode(String.self, forKey: .name)) ?? ""
        gruppe = (try? c.decode(String.self, forKey: .gruppe)) ?? "Weitere"
        beschreibung = (try? c.decode(String.self, forKey: .beschreibung)) ?? ""
        art = (try? c.decode(String.self, forKey: .art)) ?? "brew"
        cask = try? c.decode(String.self, forKey: .cask)
        appPfad = try? c.decode(String.self, forKey: .appPfad)
        bundleId = try? c.decode(String.self, forKey: .bundleId)
        exePfad = (try? c.decode(String.self, forKey: .exePfad)) ?? ""
        versionsArgumente = try? c.decode(String.self, forKey: .versionsArgumente)
        updateArgumente = try? c.decode(String.self, forKey: .updateArgumente)
        pruefArgumente = try? c.decode(String.self, forKey: .pruefArgumente)
        npmPaket = try? c.decode(String.self, forKey: .npmPaket)
        startArgumente = try? c.decode(String.self, forKey: .startArgumente)
        pfeilZaehlen = (try? c.decode(Bool.self, forKey: .pfeilZaehlen)) ?? false
        zeitlimitMinuten = (try? c.decode(Int.self, forKey: .zeitlimitMinuten)) ?? 20
        repoOrdner = try? c.decode(String.self, forKey: .repoOrdner)
        skript = try? c.decode(String.self, forKey: .skript)
        skriptArgumente = try? c.decode(String.self, forKey: .skriptArgumente)
        statusPraefix = try? c.decode(String.self, forKey: .statusPraefix)
        projektDatei = try? c.decode(String.self, forKey: .projektDatei)
        dialogWartezeitSekunden = (try? c.decode(Int.self, forKey: .dialogWartezeitSekunden)) ?? 300
        prozesse = (try? c.decode([String].self, forKey: .prozesse)) ?? []
        helferProzesse = (try? c.decode([String].self, forKey: .helferProzesse)) ?? []
        beendenVorUpdate = (try? c.decode(Bool.self, forKey: .beendenVorUpdate)) ?? false
        neuStartenNachUpdate = (try? c.decode(Bool.self, forKey: .neuStartenNachUpdate)) ?? false
        hinweis = try? c.decode(String.self, forKey: .hinweis)
        akzent = (try? c.decode(String.self, forKey: .akzent)) ?? "#7C5CFF"
    }
}

struct ProgrammKatalog: Codable {
    var version: Int = 1
    var programme: [ProgrammEintrag] = []

    enum CodingKeys: String, CodingKey { case version, programme }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        version = (try? c.decode(Int.self, forKey: .version)) ?? 1
        programme = (try? c.decode([ProgrammEintrag].self, forKey: .programme)) ?? []
    }

    init() {}
}

// MARK: - Pruefergebnis

enum UpdateZustand {
    case unbekannt
    case pruefe
    case aktuell
    case updateVerfuegbar
    case nichtInstalliert
    case fehler
    case fertig
    case abgebrochen
}

/// Ergebnis einer Pruefung oder eines Update-Laufs.
struct PruefErgebnis {
    var zustand: UpdateZustand
    /// Leer, wenn das Programm nicht installiert ist.
    var installierteVersion: String = ""
    /// Leer, wenn nichts Neueres bekannt ist.
    var verfuegbareVersion: String = ""
    /// Kurze Zeile fuer die Karte; das lange Protokoll geht in den Detailbereich.
    var meldung: String = ""
    var protokoll: String = ""
    /// Der Installer hat das Update abgelegt und schliesst es beim naechsten Start des Programms ab.
    /// Sofort nachzupruefen wuerde die alte Version melden und wie ein Fehlschlag aussehen.
    var erstNachNeustart: Bool = false
}

// MARK: - Update-Bericht

enum LaufErgebnis: String, Codable {
    /// Der Fingerabdruck hat sich wie erwartet geaendert -- das Update ist nachweislich angekommen.
    case erfolgreich = "Erfolgreich"
    /// Der Installer hat das Update abgelegt; es wird beim naechsten Start des Programms aktiv.
    case ausstehend = "Ausstehend"
    /// Es war nichts offen, oder der Benutzer hat abgelehnt.
    case abgebrochen = "Abgebrochen"
    /// Das Werkzeug selbst hat einen Fehler gemeldet.
    case fehlgeschlagen = "Fehlgeschlagen"
    /// Der schlimmste Fall und der Grund, warum es diesen Typ gibt: Erfolg gemeldet, aber nichts
    /// hat sich geaendert. Dem Exit-Code blind zu glauben wuerde genau das verstecken.
    case nichtVerifiziert = "NichtVerifiziert"
}

/// Ein Update-Lauf, geschrieben nach verlauf.jsonl -- damit ein Fehlschlag auch Tage spaeter noch
/// nachvollziehbar ist.
struct UpdateBericht: Codable {
    var zeit: Date = Date()
    var programmId: String = ""
    var name: String = ""
    var art: String = ""

    var ergebnis: LaufErgebnis = .abgebrochen
    var meldung: String = ""

    var versionVorher: String = ""
    var versionNachher: String = ""

    /// Nur bei `ausstehend` gesetzt: die Version, die spaeter erscheinen soll.
    var ausstehendeVersion: String?

    var befehl: String = ""
    var exitCode: Int = 0

    /// Unter Windows: lief der Lauf erhoeht? Auf macOS gibt es keine Rechte-Erhoehung fuer
    /// GUI-Programme; das Feld bleibt fuer die Formatgleichheit der Verlaufsdatei erhalten.
    var erhoeht: Bool = false

    var protokollDatei: String = ""

    var ergebnisText: String {
        switch ergebnis {
        case .erfolgreich: return "Erfolgreich"
        case .ausstehend: return "Ausstehend"
        case .abgebrochen: return "Abgebrochen"
        case .fehlgeschlagen: return "Fehlgeschlagen"
        case .nichtVerifiziert: return "Nicht verifiziert"
        }
    }

    /// Kurze Zeile fuer die Karte: wann, wie es lief, und welche Staende beteiligt waren.
    var kurzfassung: String {
        let versionen: String
        if versionVorher.trimmed.isEmpty && versionNachher.trimmed.isEmpty {
            versionen = ""
        } else if versionVorher == versionNachher || versionNachher.trimmed.isEmpty {
            versionen = "  " + versionVorher
        } else {
            versionen = "  " + versionVorher + " → " + versionNachher
        }
        return "Zuletzt: " + Formate.zeitpunkt(zeit) + " – " + ergebnisText + versionen
    }

    var istFehler: Bool { ergebnis == .fehlgeschlagen || ergebnis == .nichtVerifiziert }

    enum CodingKeys: String, CodingKey {
        case zeit = "Zeit"
        case programmId = "ProgrammId"
        case name = "Name"
        case art = "Art"
        case ergebnis = "Ergebnis"
        case meldung = "Meldung"
        case versionVorher = "VersionVorher"
        case versionNachher = "VersionNachher"
        case ausstehendeVersion = "AusstehendeVersion"
        case befehl = "Befehl"
        case exitCode = "ExitCode"
        case erhoeht = "Erhoeht"
        case protokollDatei = "ProtokollDatei"
    }

    init() {}

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        zeit = (try? c.decode(Date.self, forKey: .zeit)) ?? Date()
        programmId = (try? c.decode(String.self, forKey: .programmId)) ?? ""
        name = (try? c.decode(String.self, forKey: .name)) ?? ""
        art = (try? c.decode(String.self, forKey: .art)) ?? ""
        ergebnis = (try? c.decode(LaufErgebnis.self, forKey: .ergebnis)) ?? .abgebrochen
        meldung = (try? c.decode(String.self, forKey: .meldung)) ?? ""
        versionVorher = (try? c.decode(String.self, forKey: .versionVorher)) ?? ""
        versionNachher = (try? c.decode(String.self, forKey: .versionNachher)) ?? ""
        ausstehendeVersion = try? c.decode(String.self, forKey: .ausstehendeVersion)
        befehl = (try? c.decode(String.self, forKey: .befehl)) ?? ""
        exitCode = (try? c.decode(Int.self, forKey: .exitCode)) ?? 0
        erhoeht = (try? c.decode(Bool.self, forKey: .erhoeht)) ?? false
        protokollDatei = (try? c.decode(String.self, forKey: .protokollDatei)) ?? ""
    }
}

// MARK: - Kleine Helfer

extension String {
    var trimmed: String { trimmingCharacters(in: .whitespacesAndNewlines) }
    var istLeer: Bool { trimmed.isEmpty }
}

enum Formate {
    /// "dd.MM.yyyy, HH:mm" -- dieselbe Schreibweise wie unter Windows.
    static func zeitpunkt(_ datum: Date) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "de_DE")
        f.dateFormat = "dd.MM.yyyy, HH:mm"
        return f.string(from: datum)
    }

    static func datum(_ datum: Date) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "de_DE")
        f.dateFormat = "dd.MM.yyyy"
        return f.string(from: datum)
    }

    static func uhrzeitMitSekunden(_ datum: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "HH:mm:ss"
        return f.string(from: datum)
    }

    static func langerZeitpunkt(_ datum: Date) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "de_DE")
        f.dateFormat = "dd.MM.yyyy, HH:mm:ss"
        return f.string(from: datum)
    }
}
