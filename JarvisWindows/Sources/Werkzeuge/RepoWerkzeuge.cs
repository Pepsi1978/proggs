using System.Text;

namespace JarvisWindows;

/// <summary>Das Repo liegt am PC: Jarvis liest es direkt und fragt für größere Prüfungen das CLI im Hintergrund, ohne die Grenzen der GitHub-Anbindung des Handys.</summary>
public static class RepoWerkzeuge
{
    private static readonly string Ordner = Pfade.Proggs;
    private const int Zeilen = 400;

    private static WerkzeugErgebnis Fehler(string t) => new(t, Fehler: true);

    private static WerkzeugErgebnis Text(Shell.Ergebnis r, string leer)
    {
        if (r.Code != 0)
        {
            var t = string.Join("\n", new[] { r.Ausgabe, r.Fehler }.Where(x => x.Length > 0));
            return Fehler(t.Length == 0 ? $"Befehl fehlgeschlagen ({r.Code})." : t);
        }
        return new WerkzeugErgebnis(r.Ausgabe.Length == 0 ? leer : r.Ausgabe);
    }

    /// <summary>Kürzen geschieht hier statt mit „| head“: Eine Pipe würde Fehler von git verschlucken, und das Modell soll sehen, dass etwas fehlt.</summary>
    private static WerkzeugErgebnis Begrenzt(Shell.Ergebnis r, string leer, int hoechstens, int breite = 300)
    {
        var e = Text(r, leer);
        if (e.Fehler || r.Ausgabe.Length == 0) return e;
        var liste = r.Ausgabe.Split('\n');
        var t = string.Join("\n", liste.Take(hoechstens).Select(z => z.Schnitt(breite)));
        if (liste.Length > hoechstens) t += $"\n… gekürzt: {liste.Length - hoechstens} weitere Zeilen, enger eingrenzen.";
        return e with { Text = t };
    }

    private static WerkzeugErgebnis Ausschnitt(string inhalt, int von)
    {
        var liste = inhalt.Split('\n');
        if (von > liste.Length) return Fehler($"Die Datei hat nur {liste.Length} Zeilen.");
        var bis = Math.Min(liste.Length, von + Zeilen - 1);
        var t = string.Join("\n", liste[(von - 1)..bis]);
        if (bis < liste.Length) t += $"\n… Zeile {von} bis {bis} von {liste.Length}. Weiter mit von_zeile {bis + 1}.";
        return new WerkzeugErgebnis(t);
    }

    /// <summary>git direkt, ohne Shell: Eingaben können so nie zu Befehlen werden, und Exit-Codes bleiben genau (git grep meldet „nichts gefunden“ mit 1).</summary>
    private static Task<Shell.Ergebnis> Git(double sekunden, params string[] argumente)
        => Shell.Prozess("git", new[] { "-c", "core.quotepath=off" }.Concat(argumente), Ordner, sekunden);

    private static string? Echt(string pfad)
    {
        var aktuell = new FileInfo(pfad);
        var ziel = aktuell.ResolveLinkTarget(true);
        var voll = ziel?.FullName ?? aktuell.FullName;
        // Auch die Ordner auf dem Weg können Verknüpfungen sein.
        var teile = voll.Substring(Path.GetPathRoot(voll)!.Length).Split('\\', StringSplitOptions.RemoveEmptyEntries);
        var weg = Path.GetPathRoot(voll)!;
        foreach (var t in teile)
        {
            weg = Path.Combine(weg, t);
            FileSystemInfo i = Directory.Exists(weg) ? new DirectoryInfo(weg) : new FileInfo(weg);
            if (i.ResolveLinkTarget(true) is { } z) weg = z.FullName;
        }
        return weg;
    }

    /// <summary>Nur versionierte Dateien, die wirklich im Repo liegen: keine Symlinks nach außen, keine lokalen .env-Dateien.</summary>
    private static async Task<string?> DateiImRepo(string pfad)
    {
        string? wurzel, echt;
        try
        {
            wurzel = Echt(Ordner);
            echt = Echt(Path.GetFullPath(Path.Combine(Ordner, pfad.Replace('/', '\\'))));
        }
        catch { return null; }
        if (wurzel == null || echt == null || !echt.StartsWith(wurzel + "\\", StringComparison.OrdinalIgnoreCase)) return null;
        var r = await Git(20, "ls-files", "--error-unmatch", "--", pfad);
        return r.Code == 0 ? echt : null;
    }

    public static readonly IReadOnlyList<LokalesWerkzeug> Alle = new[]
    {
        new LokalesWerkzeug(new WerkzeugDefinition(
            "repo_lesen",
            $"Jarvis am PC: liest Franks Repo direkt aus ~/proggs, ohne die Grenzen der GitHub-Anbindung. aktion=projekte: oberste Ebene (jeder Ordner ist meist ein Projekt). dateien: alle Dateien eines Ordners (pfad), höchstens 800. datei: Inhalt einer versionierten Datei (pfad; je Aufruf {Zeilen} Zeilen, mit von_zeile weiterlesen). suche: Code-Suche im ganzen Repo oder unter pfad (anfrage), höchstens 200 Treffer. verlauf: letzte Commits, auf Wunsch nur für pfad, mit dem Stand gegenüber GitHub. prs: offene Pull Requests. Mit zweig liest du einen anderen Stand. Gekürzte Ergebnisse sind als gekürzt markiert. Nur lesen.",
            new JSONObjekt
            {
                ["type"] = "object",
                ["properties"] = new JSONObjekt
                {
                    ["aktion"] = new JSONObjekt { ["type"] = "string", ["enum"] = new List<string> { "projekte", "dateien", "datei", "suche", "verlauf", "prs" } },
                    ["pfad"] = new JSONObjekt { ["type"] = "string", ["description"] = "Pfad im Repo, zum Beispiel Jarvis oder Jarvis/app/build.gradle.kts" },
                    ["anfrage"] = new JSONObjekt { ["type"] = "string", ["description"] = "Für suche: Suchbegriff" },
                    ["von_zeile"] = new JSONObjekt { ["type"] = "integer", ["description"] = "Für datei: erste Zeile, Vorgabe 1" },
                    ["zweig"] = new JSONObjekt { ["type"] = "string", ["description"] = "Anderer Zweig oder Commit, Vorgabe: aktueller Stand am PC" },
                },
                ["required"] = new List<string> { "aktion" },
            }),
            async a =>
            {
                // Ohne Vollzugriff bleibt der Weg über das Handy.
                if (!Einstellungen.Shared.Vollzugriff) return await McpKlient.Shared.Rufe("repo_lesen", a);
                var pfad = (a.Text("pfad") ?? "").Replace('\\', '/');
                var anfrage = a.Text("anfrage") ?? "";
                var zweig = a.Text("zweig") ?? "";
                if (pfad.StartsWith('/') || pfad.StartsWith('~') || pfad.StartsWith('-') || pfad.Contains(':') || pfad.Split('/').Contains(".."))
                    return Fehler("pfad muss relativ zum Repo sein.");
                if (Tresor.Sperre(pfad, anfrage, zweig) is { } g) return g;
                // zweig wird Argument von git: erst prüfen, sonst ginge eine Option wie --output durch.
                if (zweig.Length > 0)
                {
                    var geprueft = await Git(20, "rev-parse", "--verify", "--quiet", "--end-of-options", zweig + "^{commit}");
                    if (zweig.StartsWith('-') || geprueft.Code != 0) return Fehler($"Zweig oder Commit „{zweig}“ gibt es nicht.");
                }
                var p = pfad.Length == 0 ? "." : pfad;
                var mitZweig = zweig.Length > 0;
                switch (a.Text("aktion") ?? "")
                {
                    case "projekte":
                        if (mitZweig) return Text(await Git(20, "ls-tree", "--name-only", zweig), "Leer.");
                        var oben = Directory.EnumerateDirectories(Ordner).Select(d => Path.GetFileName(d) + "/").Concat(Directory.EnumerateFiles(Ordner).Select(Path.GetFileName)).OrderBy(x => x, StringComparer.Ordinal);
                        return new WerkzeugErgebnis(string.Join("\n", oben) is { Length: > 0 } s ? s : "Leer.");
                    case "dateien":
                        return Begrenzt(mitZweig ? await Git(30, "ls-tree", "-r", "--name-only", zweig, "--", p) : await Git(30, "ls-files", "--", p), $"Keine Dateien unter {pfad}.", 800);
                    case "datei":
                        if (pfad.Length == 0) return Fehler("pfad fehlt.");
                        var von = (int)Math.Max(1, a.Ganz("von_zeile") ?? 1);
                        if (mitZweig)
                        {
                            var r = await Git(30, "show", zweig + ":" + pfad);
                            return r.Code == 0 ? Ausschnitt(r.Ausgabe, von) : Text(r, "");
                        }
                        if (await DateiImRepo(pfad) is not { } echt) return Fehler($"„{pfad}“ ist keine versionierte Datei im Repo.");
                        try { return Ausschnitt(await File.ReadAllTextAsync(echt, new UTF8Encoding(false, true)), von); }
                        catch { return Fehler($"„{pfad}“ ist nicht als Text lesbar."); }
                    case "suche":
                        if (anfrage.Length == 0) return Fehler("anfrage fehlt.");
                        var argumente = new List<string> { "grep", "-n", "-I", "-e", anfrage };
                        if (mitZweig) argumente.Add(zweig);
                        argumente.AddRange(new[] { "--", p });
                        var treffer = await Git(60, argumente.ToArray());
                        // git grep endet mit 1, wenn es nichts findet; das ist kein Fehler.
                        if (treffer.Code == 1 && treffer.Fehler.Length == 0) return new WerkzeugErgebnis("Nichts gefunden.");
                        return Begrenzt(treffer, "Nichts gefunden.", 200, 260);
                    case "verlauf":
                        var log = new List<string> { "log", "-n", "30", "--date=short", "--format=%h %ad %s" };
                        if (mitZweig) log.Add(zweig);
                        log.AddRange(new[] { "--", p });
                        var e = Text(await Git(30, log.ToArray()), "Kein Verlauf.");
                        if (!e.Fehler && !mitZweig)
                        {
                            var stand = await Git(20, "status", "-sb");
                            e = e with { Text = stand.Ausgabe.Split('\n')[0] + "\n" + e.Text };
                        }
                        return e;
                    case "prs":
                        return Text(await Shell.Prozess("gh", new[] { "pr", "list", "--limit", "30" }, Ordner, 40), "Keine offenen Pull Requests.");
                    default:
                        return Fehler("Unbekannte aktion.");
                }
            }),
        new LokalesWerkzeug(new WerkzeugDefinition(
            "cli_frage",
            "Jarvis am PC: fragt im Hintergrund das Claude-Code-CLI in ~/proggs, ohne Fenster. Es kann nur Dateien lesen und suchen, nichts ändern und nichts ausführen; ~/SK ist gesperrt. Für Fragen zum Repo und Prüfungen, die mehrere Dateien brauchen: „Wie funktioniert …?“, „Ist … schon eingebaut?“, „Wo steckt der Fehler?“. Dauert bis zu zehn Minuten und kostet Abo-Kontingent. Einzelne Dateien liest du schneller mit repo_lesen. Zum Ändern von Code eine Sitzung mit openlauncher_sitzung starten oder programmieren.",
            new JSONObjekt
            {
                ["type"] = "object",
                ["properties"] = new JSONObjekt
                {
                    ["frage"] = new JSONObjekt { ["type"] = "string", ["description"] = "Die vollständige, für sich allein verständliche Frage" },
                    ["modell"] = new JSONObjekt { ["type"] = "string", ["description"] = "sonnet (Vorgabe), opus oder haiku" },
                },
                ["required"] = new List<string> { "frage" },
            }),
            async a =>
            {
                if (!Einstellungen.Shared.Vollzugriff) return Fehler("Vollzugriff ist in den Einstellungen ausgeschaltet. Frank muss ihn einschalten (Einstellungen → Windows & Rechte).");
                var frage = a.Text("frage") ?? "";
                if (frage.Length == 0) return Fehler("frage fehlt.");
                if (Tresor.Sperre(frage) is { } g) return g;
                var modell = new[] { "sonnet", "opus", "haiku" }.FirstOrDefault(m => m == a.Text("modell")) ?? "sonnet";
                var heimPosix = "//" + Pfade.Heim.Replace(":", "").Replace('\\', '/').ToLowerInvariant()[..1] + Pfade.Heim.Replace(":", "").Replace('\\', '/')[1..];
                // Dieselbe Anmeldung wie die Sitzungen aus dem OpenLauncher, aber ohne dessen Einstellungen: Dort stünde der Modus „Rechte umgehen“,
                // dazu kämen Hooks und MCP-Server. Die Sperre gilt erst durch --tools und dontAsk, der Fragetext allein schützt nichts.
                var r = await HelferWerkzeuge.ClaudeImHintergrund(frage + "\n\nAntworte auf Deutsch und knapp.", Ordner, 600, modell, "Read,Grep,Glob", null,
                    new[] { $"Read({heimPosix}/SK/**)", "Read(~/SK/**)" });
                if (r.ZeitAus) return Fehler("Das CLI hat in zehn Minuten nicht geantwortet. Für so große Fragen openlauncher_sitzung nehmen.");
                return Text(r, "Das CLI hat nichts geantwortet.");
            }),
    };
}
