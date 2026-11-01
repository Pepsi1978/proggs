using System.Text;

namespace JarvisWindows;

/// <summary>Die Werkzeuge zu den eigenen Helfern dieses PCs. Die Handy-Helfer und der Gesamtkatalog kommen über helfer_* des Handys.</summary>
public static class HelferWerkzeuge
{
    private static readonly object Schloss = new();
    private const int TagesGrenze = 5;

    /// <summary>Einträge „Schlüssel|Kennung“; ohne Kennung (neuer Helfer) passt der Schlüssel nur zu einer noch nicht vergebenen.</summary>
    private static List<string> Geladen() => Tresor.Lade<List<string>>("helfer-bau") ?? new();

    public static bool BauSchluesselPasst(string s, string kennung, bool vorhanden)
    {
        lock (Schloss)
        {
            if (s.Length == 0 || s.Contains('|')) return false;
            var eintrag = Geladen().FirstOrDefault(e => e == s || e.StartsWith(s + "|"));
            if (eintrag == null) return false;
            var gebunden = eintrag.Split('|').Skip(1).FirstOrDefault() ?? "";
            return gebunden.Length == 0 ? !vorhanden : gebunden == kennung;
        }
    }

    public static void BauSchluesselVerbrauchen(string s)
    {
        lock (Schloss) Tresor.Sichere(Geladen().Where(e => e != s && !e.StartsWith(s + "|")).ToList(), "helfer-bau");
    }

    private static string NeuerBauSchluessel(string kennung)
    {
        var s = Guid.NewGuid().ToString("N").ToUpperInvariant();
        lock (Schloss)
        {
            var neu = Geladen().Append(s + "|" + kennung).ToList();
            Tresor.Sichere(neu.Skip(Math.Max(0, neu.Count - 20)).ToList(), "helfer-bau");
        }
        return s;
    }

    /// <summary>Wie Finde-Claude der PC-Brücke. Eine .cmd-Hülle nur zuletzt: Durch cmd.exe sollen keine Texte gehen, deshalb kommt die Frage ohnehin über stdin.</summary>
    private static string? Claude() => Shell.Finde("claude",
        Path.Combine(Pfade.Heim, ".local", "bin", "claude.exe"),
        Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "npm", "node_modules", "@anthropic-ai", "claude-code", "bin", "claude.exe"));

    /// <summary>claude -p ohne Fenster, Text über stdin, nur die genannten Werkzeuge, ohne Hooks und ohne MCP.</summary>
    internal static async Task<Shell.Ergebnis> ClaudeImHintergrund(string text, string ordner, double sekunden, string modell, string werkzeuge, string? erlaubt, IEnumerable<string>? verboten)
    {
        if (Claude() is not { } exe) return new Shell.Ergebnis("", "Keine Claude-Code-Installation gefunden.", -1, false);
        var profil = Path.Combine(Pfade.Proggs, "OpenLauncher", "Profiles", "ClaudeCode", "minimal");
        // --setting-sources project läse die Hooks aus proggs\.claude\settings.json mit: Sie gelten hier ausdrücklich nicht.
        var einstellungen = Path.Combine(Pfade.Temp, $"cli-{Guid.NewGuid():N}.json");
        try
        {
            await File.WriteAllTextAsync(einstellungen, "{\"disableAllHooks\":true}", new UTF8Encoding(false));
            var args = new List<string> { "-p", "--output-format", "text", "--model", modell, "--setting-sources", "project", "--settings", einstellungen,
                "--strict-mcp-config", "--permission-mode", "dontAsk", "--tools", werkzeuge };
            if (erlaubt != null) args.AddRange(new[] { "--allowedTools", erlaubt });
            if (verboten != null) { args.Add("--disallowedTools"); args.AddRange(verboten); }
            return await Shell.Prozess(exe, args, ordner, sekunden, text, new Dictionary<string, string> { ["CLAUDE_CONFIG_DIR"] = profil });
        }
        finally { try { File.Delete(einstellungen); } catch { } }
    }

    /// <summary>Recherche über das CLI, parallel über Unteragenten. Kein Zugriff auf ~/SK, keine Änderungen, nur mit Vollzugriff.</summary>
    public static async Task<string> CliRecherche(string frage, int anzahl, string modell)
    {
        static string Nein(string t) => JSON.Text(new JSONObjekt { ["ok"] = false, ["fehler"] = t });
        if (!Einstellungen.Shared.Vollzugriff) return Nein("Der Vollzugriff ist am PC ausgeschaltet.");
        if (frage.Length == 0) return Nein("Die Frage fehlt.");
        if (Tresor.Sperre(frage) != null) return Nein("Die Frage berührt gesperrte Zugangsdaten.");
        var zahl = Math.Min(Math.Max(anzahl, 1), 7);
        var cli = new[] { "sonnet", "opus", "fable" }.FirstOrDefault(m => m == modell) ?? "sonnet";
        var voll = $"Recherchiere gründlich: {frage}\n\nTeile die Arbeit in getrennte Fragen und nutze dafür bei Bedarf bis zu {zahl} parallele Unteragenten mit dem Modell sonnet; nicht mehr als nötig und keine doppelten Fragen. Antworte auf Deutsch mit dem, was für die Umsetzung zählt, und mit den Quellen.";
        // Nur Web, kein Lesen von Dateien, außerhalb des Repos: Eine manipulierte Webseite soll hier nichts finden, das sie ausleiten kann.
        var r = await ClaudeImHintergrund(voll, Pfade.Temp, 1200, cli, "WebSearch,WebFetch,Task", "WebSearch,WebFetch,Task", null);
        if (r.ZeitAus) return Nein("Die Recherche wurde nach 20 Minuten abgebrochen.");
        if (r.Code != 0)
        {
            var t = r.Fehler.Length == 0 ? r.Ausgabe : r.Fehler;
            return Nein(t.Length > 600 ? t[^600..] : t);
        }
        return JSON.Text(new JSONObjekt { ["ok"] = true, ["text"] = Geheimnisse.Schwaerzen(r.Ausgabe) });
    }

    private static string Zeile(HelferEintrag e)
    {
        var stand = e.Aktiv ? e.Zustand : "deaktiviert";
        return $"- {e.Kennung} ({e.Name}, {e.Text("version")}, {stand}): {e.Text("zweck")}";
    }

    private static string Detail(HelferEintrag e)
    {
        var t = $"{e.Name} [{e.Kennung}], Fassung {e.Text("version")}, PC, {(e.Aktiv ? e.Zustand : "deaktiviert")}\nZweck: {e.Text("zweck")}";
        t += $"\nFähigkeiten: {string.Join("; ", e.Liste("faehigkeiten"))}";
        t += $"\nEingabe (JSON-Schema): {JSON.Text(e.Manifest.Obj("eingabe") ?? new JSONObjekt())}\nErgebnis: {e.Text("ausgabe")}";
        t += $"\nVoraussetzungen: {string.Join("; ", e.Liste("voraussetzungen"))}\nGrenzen: {string.Join("; ", e.Liste("grenzen"))}";
        t += $"\nRechte: {JSON.Text(e.Rechte)}\nLetzte Prüfung: {e.PruefungErgebnis}";
        return t;
    }

    private static int TagesZahl() => Vorgaben.Text("helferBauTag") == DateTime.Now.ToString("yyyy-MM-dd") ? Vorgaben.Ganz("helferBauZahl", 0) : 0;

    /// <summary>Zählt erst nach dem Start: Ein gescheiterter Start kostet keinen der fünf Aufträge des Tages.</summary>
    private static void TagesZaehlerErhoehen()
    {
        var n = TagesZahl();
        Vorgaben.Setze("helferBauTag", DateTime.Now.ToString("yyyy-MM-dd"));
        Vorgaben.Setze("helferBauZahl", n + 1);
    }

    private static string Auftrag(string aufgabe, string faehigkeit, string kennung, string schluessel, string modell, string effort)
    {
        var eingang = Path.Combine(Pfade.Unterstuetzung, "Helfer", "eingang");
        return $$$"""
            Bau für {{{AppInfo.Name}}} einen Helfer (oder verbessere den vorhandenen „{{{kennung}}}“) nach JarvisHelfer/README.md. Lies die Datei zuerst.
            Ziel: {{{faehigkeit}}}
            Franks ursprüngliche Aufgabe, für die er gebraucht wird: {{{aufgabe}}}
            Ort: {{{AppInfo.Geraet}}} (dieser Windows-PC). Quellstand nach JarvisHelfer/<kennung>/, committen und pushen.
            Vorgehen: kleinste tragfähige Lösung, praktische Tests für Normal- und Fehlerfälle im Steckbrief, danach gründliches Review mit /code-review max --max-findings 100, kritische Befunde beheben.
            Recherche nur bei echten Lücken; dabei bevorzugt {{{modell}}} mit Effort {{{effort}}}, keine kleinen Modelle, höchstens sieben parallele Unteragenten.
            Abschluss: Paket nach {{{eingang}}}\<kennung>\ kopieren (helfer.json, Programmdatei) und zuletzt registrierung.json dazu: {"bau_schluessel":"{{{schluessel}}}","review":{"befunde":n,"kritisch_offen":0,"zusammenfassung":"..."}}. Jarvis prüft es dann selbst.
            Nichts anlegen, das Kosten, neue Konten, Veröffentlichungen oder zusätzliche Rechte ohne Franks Ja bedeutet. REVIEW: ja, sehr ausführlich.
            """;
    }

    private static JSONObjekt Str(string? beschreibung = null)
    {
        var o = new JSONObjekt { ["type"] = "string" };
        if (beschreibung != null) o["description"] = beschreibung;
        return o;
    }

    public static readonly IReadOnlyList<LokalesWerkzeug> Alle = new[]
    {
        new LokalesWerkzeug(new WerkzeugDefinition(
            "windows_helfer_suchen",
            "Jarvis am PC: durchsucht den Katalog der eigenen PC-Helfer nach einer Fähigkeit („Word-Datei“, „YouTube“). Ohne Suchwort kommt die Übersicht. Nutze es, bevor du sagst, dass etwas fehlt.",
            new JSONObjekt { ["type"] = "object", ["properties"] = new JSONObjekt { ["suche"] = Str("Fähigkeit oder Stichworte") } }),
            async a =>
            {
                var wort = a.Text("suche") ?? "";
                var treffer = Haupt.Wert(() => HelferKatalog.Shared.Suche(wort).ToList());
                return new WerkzeugErgebnis(treffer.Count == 0 ? "Kein passender PC-Helfer." : string.Join("\n", treffer.Select(Zeile)));
            }),
        new LokalesWerkzeug(new WerkzeugDefinition(
            "windows_helfer_info",
            "Jarvis am PC: alle Angaben zu einem PC-Helfer: Zweck, Fähigkeiten, Eingaben, Ergebnisse, Voraussetzungen, Rechte, Grenzen, Zustand, letzte Prüfung.",
            new JSONObjekt { ["type"] = "object", ["properties"] = new JSONObjekt { ["kennung"] = Str() }, ["required"] = new List<string> { "kennung" } }),
            async a =>
            {
                var e = Haupt.Wert(() => HelferKatalog.Shared.Finde(a.Text("kennung") ?? ""));
                return e == null ? new WerkzeugErgebnis("Keinen solchen Helfer gefunden.", Fehler: true) : new WerkzeugErgebnis(Detail(e));
            }),
        new LokalesWerkzeug(new WerkzeugDefinition(
            "windows_helfer_ausfuehren",
            "Jarvis am PC: führt einen einsatzbereiten PC-Helfer aus und liefert sein Ergebnis samt Dateipfaden. Prüfe das Ergebnis, bevor du Erfolg meldest.",
            new JSONObjekt
            {
                ["type"] = "object",
                ["properties"] = new JSONObjekt
                {
                    ["kennung"] = Str(),
                    ["eingabe"] = new JSONObjekt { ["type"] = "object", ["description"] = "Eingabe nach dem Schema des Helfers" },
                },
                ["required"] = new List<string> { "kennung" },
            }),
            async a =>
            {
                var k = a.Text("kennung") ?? "";
                var eingabe = a.Obj("eingabe") ?? new JSONObjekt();
                var r = await HelferKatalog.Shared.Ausfuehren(k, eingabe: eingabe);
                if (!r.Ok) return new WerkzeugErgebnis(r.Fehler, Fehler: true);
                var t = r.Text;
                if (r.DatenText != "{}") t += $"\nDaten: {r.DatenText}";
                if (r.Dateien.Count != 0) t += "\nDateien: " + string.Join(", ", r.Dateien.Select(d => $"{d.name} → {d.pfad}"));
                return new WerkzeugErgebnis(t + $"\nDauer: {r.DauerMs} ms");
            }),
        new LokalesWerkzeug(new WerkzeugDefinition(
            "windows_helfer_verwalten",
            "Jarvis am PC: aktiviert oder deaktiviert einen PC-Helfer, legt ihn in den Papierkorb, holt ihn zurück oder setzt ihn auf die letzte geprüfte Fassung. Freigaben und endgültiges Löschen macht nur Frank in den Einstellungen.",
            new JSONObjekt
            {
                ["type"] = "object",
                ["properties"] = new JSONObjekt
                {
                    ["kennung"] = Str(),
                    ["aktion"] = new JSONObjekt { ["type"] = "string", ["enum"] = new List<string> { "aktivieren", "deaktivieren", "papierkorb", "wiederherstellen", "rueckrollen" } },
                },
                ["required"] = new List<string> { "kennung", "aktion" },
            }),
            async a =>
            {
                var k = a.Text("kennung") ?? "";
                var aktion = a.Text("aktion") ?? "";
                var r = Haupt.Wert(() => HelferKatalog.Shared.Verwalten(k, aktion: aktion));
                return new WerkzeugErgebnis(r.text, Fehler: !r.ok);
            }),
        new LokalesWerkzeug(new WerkzeugDefinition(
            "windows_helfer_bauen",
            "Jarvis am PC: lässt für eine konkrete Aufgabe einen PC-Helfer bauen oder verbessern, wenn Suche und vorhandene Funktionen nicht reichen. Sag Frank kurz, dass du dafür einen Helfer bauen lässt, und mach danach seine eigentliche Aufgabe fertig. Nicht ohne konkreten Anlass.",
            new JSONObjekt
            {
                ["type"] = "object",
                ["properties"] = new JSONObjekt
                {
                    ["aufgabe"] = Str("Franks ursprüngliche Aufgabe"),
                    ["faehigkeit"] = Str("Was der Helfer können muss, mit überprüfbaren Erfolgskriterien"),
                    ["kennung"] = Str("Nur beim Verbessern: Kennung des vorhandenen Helfers"),
                },
                ["required"] = new List<string> { "aufgabe", "faehigkeit" },
            }),
            async a =>
            {
                var e = Einstellungen.Shared;
                if (!e.HelferAutomatik) return new WerkzeugErgebnis("Der Helferbau ist pausiert (Einstellungen → Eigene Tools). Sag Frank das.", Fehler: true);
                var aufgabe = a.Text("aufgabe") ?? "";
                var faehigkeit = a.Text("faehigkeit") ?? "";
                var kennung = a.Text("kennung") ?? "";
                if (faehigkeit.Length < 10 || aufgabe.Length == 0) return new WerkzeugErgebnis("Aufgabe und Fähigkeit vollständig angeben.", Fehler: true);
                // Das Handy kennt bei helfer_bauen nur die Orte handy und mac; mac liefe auf dem falschen Rechner. Windows-Helfer entstehen deshalb direkt hier.
                if (TagesZahl() >= TagesGrenze) return new WerkzeugErgebnis($"Heute wurden schon {TagesGrenze} Helfer direkt am PC beauftragt; das ist die Grenze.", Fehler: true);
                if (WinWerkzeuge.Alle.FirstOrDefault(w => w.Definition.Name == "openlauncher_sitzung") is not { } start) return new WerkzeugErgebnis("OpenLauncher-Weg fehlt.", Fehler: true);
                var text = Auftrag(aufgabe, faehigkeit, kennung, NeuerBauSchluessel(kennung), e.RechercheModell, e.RechercheEffort);
                var ergebnis = await start.Ausfuehren(new JSONObjekt
                {
                    ["auftrag"] = text, ["name"] = "Jarvis: Helfer", ["cli"] = "claude", ["modell"] = "claude-opus-5-5[1m]", ["profil"] = "minimal", ["modus"] = "schnell", ["effort"] = "high",
                });
                if (ergebnis.Fehler) return ergebnis;
                TagesZaehlerErhoehen();
                return new WerkzeugErgebnis("Der Helferbau läuft direkt am PC mit Claude Opus 5.5. Fertig ist er erst, wenn er nach bestandener Prüfung im Katalog als bereit steht (windows_helfer_suchen).");
            }),
    };
}
