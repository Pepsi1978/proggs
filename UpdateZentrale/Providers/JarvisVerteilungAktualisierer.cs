using System.IO;
using System.Text.Json;
using UpdateZentrale.Models;
using UpdateZentrale.Services;

namespace UpdateZentrale.Providers;

/// <summary>
/// Jarvis auf HP-Laptop, Power-PC und Mac: dünne Karte über der gemeinsamen Verteil-Logik
/// <c>Werkzeuge/jarvis-verteilung/jarvis_verteilung.py</c> (dieselbe Datei nutzt die Mac-Zentrale).
/// Prüfen = Repo holen, Portierungsbedarf bestimmen und nötige Portierungssitzungen im Hintergrund starten (nie bei einem
/// gewöhnlichen Update ohne Portierungsbedarf), Aktualisieren = nur geprüfte, freigegebene Stände mit Sicherung und
/// Wiederherstellung installieren. Beim ersten Prüfen richtet sie den Hintergrundlauf (Aufgabe „JarvisVerteilung“, alle
/// 30 Minuten, ohne erhöhte Rechte) ein; darüber holt ein Rechner, der offline war, alles nach.
/// </summary>
public sealed class JarvisVerteilungAktualisierer : IAktualisierer
{
    public string Art => "jarvisverteilung";

    private static bool _eingerichtet;

    internal static string Skript => Path.Combine(Pfade.RepoWurzel, "Werkzeuge", "jarvis-verteilung", "jarvis_verteilung.py");

    /// <summary>
    /// Ein echtes python.exe, ohne sich auf einen Starter im PATH zu verlassen. Gesucht wird in dieser Reihenfolge:
    /// klassischer Installer (%LOCALAPPDATA%\Programs\Python\Python3xx), Python Install Manager
    /// (%LOCALAPPDATA%\Python\pythoncore-3.xx-64, dort gibt es kein „py“, wenn der Launcher fehlt),
    /// systemweite Installation (Program Files), dann PATH ohne WindowsApps (dort liegt der Store-Platzhalter,
    /// der „Python wurde nicht gefunden“ meldet). Null, wenn nichts da ist.
    /// </summary>
    internal static string? Python()
    {
        // Ein Treffer bleibt gültig, solange die Datei existiert: spart die Suche bei jedem Kartenabgleich.
        if (_python is { } bekannt && File.Exists(bekannt)) return bekannt;
        return _python = PythonSuchen();
    }

    private static string? _python;

    internal const string MusterKlassisch = @"^Python3(\d{1,3})(-64|-arm64|-32)?$";
    internal const string MusterInstallManager = @"^pythoncore-3\.(\d{1,3})(-64|-arm64|-32)?$";

    private static string? PythonSuchen()
    {
        var appData = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
        var programme = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles);
        // Über alle Wurzeln gemeinsam nach Version sortiert: ein altes Python38 schlägt kein neueres pythoncore-3.14.
        var exe = Versionsordner([(Path.Combine(appData, "Programs", "Python"), MusterKlassisch),
                                  (Path.Combine(appData, "Python"), MusterInstallManager),
                                  (programme, MusterKlassisch)])
                  .Select(d => Path.Combine(d, "python.exe"))
                  .Append(Path.Combine(appData, "Python", "bin", "python.exe"))
                  .FirstOrDefault(File.Exists);
        if (exe is not null) return exe;

        // PATH: zuerst der offizielle Starter „py“ (wählt selbst das neueste Python 3), auch als Alias in WindowsApps.
        // Danach python.exe/python3.exe nur nach einem Probelauf: im PATH kann ein Python 2, ein MSYS-Python oder
        // der Store-Platzhalter in WindowsApps stehen.
        var ordner = (Environment.GetEnvironmentVariable("PATH") ?? "")
            .Split(';', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
            .Select(o => Environment.ExpandEnvironmentVariables(o.Trim('"')))
            .Where(o => o.Length > 0).ToList();
        foreach (var name in new[] { "py.exe", "python3.exe", "python.exe" })
            foreach (var o in ordner)
            {
                string p;
                try { p = Path.Combine(o, name); } catch (ArgumentException) { continue; }
                if (!File.Exists(p) && !IstAlias(p)) continue;
                if (name == "py.exe" || TaugtAlsPython(p)) return p;
            }
        return null;
    }

    /// <summary>App-Ausführungsaliase in WindowsApps sind Reparse-Punkte; File.Exists sieht sie nicht immer.</summary>
    private static bool IstAlias(string pfad)
    {
        try { return new FileInfo(pfad).Attributes.HasFlag(FileAttributes.ReparsePoint); }
        catch (Exception e) when (e is IOException or UnauthorizedAccessException or ArgumentException) { return false; }
    }

    /// <summary>Echtes Windows-Python ab 3.8 (kein Platzhalter, kein Python 2, kein MSYS)? Höchstens 10 s.</summary>
    private static bool TaugtAlsPython(string pfad)
    {
        try
        {
            using var p = System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(pfad,
                "-c \"import sys;print(sys.version_info>=(3,8) and sys.platform=='win32')\"")
                { RedirectStandardOutput = true, RedirectStandardError = true, UseShellExecute = false, CreateNoWindow = true });
            if (p is null) return false;
            var ausgabe = p.StandardOutput.ReadToEndAsync();
            _ = p.StandardError.ReadToEndAsync();
            if (!p.WaitForExit(10_000)) { try { p.Kill(true); } catch (Exception) { } return false; }
            return p.ExitCode == 0 && ausgabe.Wait(1_000) && ausgabe.Result.Trim() == "True";
        }
        catch (Exception) { return false; }
    }

    /// <summary>
    /// Unterordner aller Wurzeln, deren Name zum Muster passt, nach Nebenversion absteigend (Python39 verliert gegen
    /// Python313); bei gleicher Version zuerst 64 Bit ohne Zusatz, dann -64, -arm64, -32. Unlesbare Wurzeln zählen als leer.
    /// </summary>
    internal static IEnumerable<string> Versionsordner(IEnumerable<(string Wurzel, string Muster)> wurzeln)
    {
        var treffer = new List<(string Ordner, int Version, int Rang)>();
        foreach (var (wurzel, muster) in wurzeln)
        {
            string[] ordner;
            try { ordner = Directory.Exists(wurzel) ? Directory.GetDirectories(wurzel) : []; }
            catch (Exception e) when (e is IOException or UnauthorizedAccessException) { continue; }
            foreach (var d in ordner)
            {
                var m = System.Text.RegularExpressions.Regex.Match(Path.GetFileName(d), muster, System.Text.RegularExpressions.RegexOptions.IgnoreCase);
                if (!m.Success || !int.TryParse(m.Groups[1].Value, out var version)) continue;
                var rang = m.Groups[2].Value.ToLowerInvariant() switch { "" => 0, "-64" => 1, "-arm64" => 2, _ => 3 };
                treffer.Add((d, version, rang));
            }
        }
        return treffer.OrderByDescending(t => t.Version).ThenBy(t => t.Rang).Select(t => t.Ordner).ToList();
    }

    internal static IEnumerable<string> Versionsordner(string wurzel, string muster) => Versionsordner([(wurzel, muster)]);

    private const string OhnePython = "Python nicht gefunden (gesucht: %LOCALAPPDATA%\\Programs\\Python, %LOCALAPPDATA%\\Python\\pythoncore-*, "
                                    + "Program Files, PATH ohne Store-Platzhalter). Python über die UpdateZentrale oder winget installieren.";

    private static async Task<(BefehlErgebnis Lauf, JsonElement? Daten)> LaufAsync(string befehl, TimeSpan zeit, CancellationToken abbruch)
    {
        var json = Path.Combine(Path.GetTempPath(), "jarvis-verteilung-" + Guid.NewGuid().ToString("N") + ".json");
        try
        {
            if (Python() is not { } python) return (new BefehlErgebnis(-1, OhnePython, false), null);
            var lauf = await Kommandozeile.AusfuehrenAsync(python, "-X utf8 \"" + Skript + "\" " + befehl, zeit, Pfade.RepoWurzel, abbruch,
                                                           zusatzUmgebung: new Dictionary<string, string> { ["JARVIS_VERTEILUNG_JSON"] = json });
            JsonElement? daten = null;
            if (File.Exists(json))
                using (var doc = JsonDocument.Parse(await File.ReadAllTextAsync(json, abbruch))) daten = doc.RootElement.Clone();
            return (lauf, daten);
        }
        finally
        {
            try { File.Delete(json); } catch (IOException) { }
        }
    }

    public async Task<PruefErgebnis> PruefenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        if (!File.Exists(Skript)) return new PruefErgebnis(UpdateZustand.Fehler, Meldung: "Verteil-Skript fehlt: " + Skript);
        if (Python() is not { } python) return new PruefErgebnis(UpdateZustand.Fehler, Meldung: OhnePython);
        if (!_eingerichtet)
        {
            var e = await Kommandozeile.AusfuehrenAsync(python, "-X utf8 \"" + Skript + "\" einrichten", TimeSpan.FromMinutes(1), Pfade.RepoWurzel, abbruch);
            protokoll.Report(e.Ausgabe);
            _eingerichtet = e.ExitCode == 0;
        }
        var (lauf, daten) = await LaufAsync("pruefen", TimeSpan.FromMinutes(6), abbruch);
        protokoll.Report(lauf.Ausgabe);
        return Einordnen(daten, lauf, nachInstallation: false);
    }

    public async Task<PruefErgebnis> AktualisierenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        protokoll.Report("Installiert nur geprüfte, freigegebene Stände; vorher Sicherung, bei Fehlschlag wird die letzte Version wiederhergestellt.");
        var (lauf, daten) = await LaufAsync("installieren", TimeSpan.FromMinutes(35), abbruch);
        protokoll.Report(lauf.Ausgabe);
        return Einordnen(daten, lauf, nachInstallation: true);
    }

    public async Task<string> FingerabdruckAsync(ProgrammEintrag eintrag, CancellationToken abbruch)
    {
        var (_, daten) = await LaufAsync("status --ohne-holen", TimeSpan.FromMinutes(2), abbruch);
        return daten is { } d && Lokal(d) is { } z ? z.GetProperty("installiert").GetString() ?? "" : "";
    }

    private static JsonElement? Lokal(JsonElement d)
    {
        if (!d.TryGetProperty("ziele", out var ziele)) return null;
        foreach (var z in ziele.EnumerateArray())
            if (z.TryGetProperty("lokal", out var l) && l.GetBoolean()) return z;
        return null;
    }

    internal static PruefErgebnis Einordnen(JsonElement? daten, BefehlErgebnis lauf, bool nachInstallation)
    {
        if (daten is not { } d || Lokal(d) is not { } z)
        {
            var letzte = lauf.Ausgabe.Split('\n').Select(s => s.Trim()).LastOrDefault(s => s.Length > 0) ?? "";
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: "Jarvis-Verteilung ohne Ergebnis: " + letzte, Protokoll: lauf.Ausgabe);
        }
        var text = d.TryGetProperty("text", out var t) ? t.GetString() ?? "" : "";
        var kurz = d.TryGetProperty("kurz", out var k) ? k.GetString() ?? "" : "";
        var installiert = z.GetProperty("installiert").GetString() ?? "";
        var bereit = z.GetProperty("bereitgestellt").GetString() ?? "";
        var zustand = z.GetProperty("zustand").GetString() ?? "";
        if (nachInstallation && kurz == "fehlgeschlagen")
            return new PruefErgebnis(UpdateZustand.Fehler, installiert, bereit, text, lauf.Ausgabe);
        var uz = zustand switch
        {
            "aktuell" => UpdateZustand.Aktuell,
            "bereitgestellt" => UpdateZustand.UpdateVerfuegbar,
            "wartet" => UpdateZustand.UpdateVerfuegbar,
            "nicht_installiert" => UpdateZustand.NichtInstalliert,
            // Blockiert ist kein Fehler der Zentrale (die letzte funktionierende Version bleibt), aber auch nicht „aktuell“:
            // ein neuerer Stand wartet, der Grund steht auf der Karte.
            "blockiert" => UpdateZustand.UpdateVerfuegbar,
            _ => UpdateZustand.Unbekannt
        };
        if (nachInstallation && kurz == "installiert") uz = UpdateZustand.Fertig;
        // Nichts installiert (wartet auf Live-Gespräch oder Freigabe, blockiert, an die Hintergrundaufgabe übergeben, schon aktuell):
        // kein Fehler und keine Übergabe an Claude Code, die letzte funktionierende Version bleibt; der Grund steht auf der Karte.
        else if (nachInstallation) uz = UpdateZustand.Abgebrochen;
        return new PruefErgebnis(uz, installiert, bereit, text, lauf.Ausgabe);
    }
}
