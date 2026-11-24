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

    /// <summary>Python aus der Standardinstallation; sonst der Name aus dem PATH.</summary>
    internal static string Python()
    {
        var lokal = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Programs", "Python");
        if (Directory.Exists(lokal))
        {
            var exe = Directory.GetDirectories(lokal, "Python3*").OrderByDescending(d => d, StringComparer.OrdinalIgnoreCase)
                               .Select(d => Path.Combine(d, "python.exe")).FirstOrDefault(File.Exists);
            if (exe is not null) return exe;
        }
        return "python";
    }

    private static async Task<(BefehlErgebnis Lauf, JsonElement? Daten)> LaufAsync(string befehl, TimeSpan zeit, CancellationToken abbruch)
    {
        var json = Path.Combine(Path.GetTempPath(), "jarvis-verteilung-" + Guid.NewGuid().ToString("N") + ".json");
        try
        {
            var lauf = await Kommandozeile.AusfuehrenAsync(Python(), "-X utf8 \"" + Skript + "\" " + befehl, zeit, Pfade.RepoWurzel, abbruch,
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
        if (!_eingerichtet)
        {
            var e = await Kommandozeile.AusfuehrenAsync(Python(), "-X utf8 \"" + Skript + "\" einrichten", TimeSpan.FromMinutes(1), Pfade.RepoWurzel, abbruch);
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
            // Blockiert ist kein Fehler der Zentrale: die letzte funktionierende Version bleibt, der Grund steht auf der Karte.
            "blockiert" => UpdateZustand.Aktuell,
            _ => UpdateZustand.Unbekannt
        };
        if (nachInstallation && kurz == "installiert") uz = UpdateZustand.Fertig;
        return new PruefErgebnis(uz, installiert, bereit, text, lauf.Ausgabe);
    }
}
