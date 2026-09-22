using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;
using System.Text;
using System.Text.Json;
using UpdateZentrale.Models;

namespace UpdateZentrale.Services;

public enum Schwere { Debug, Info, Warnung, Fehler }

/// <summary>
/// One user or batch action. Created with <see cref="Diagnose.VorgangBeginnen"/>; while it is
/// open it is the ambient context (AsyncLocal), so every event written below it -- provider
/// calls, external commands, chain phases -- carries its id without passing anything around.
/// A new provider or catalog program therefore gets full correlation for free.
/// </summary>
public sealed class Vorgang : IDisposable
{
    private readonly Stopwatch _uhr = Stopwatch.StartNew();
    private readonly Vorgang? _vorher;
    private int _beendet;

    internal Vorgang(string art, ProgrammEintrag? programm, Vorgang? eltern)
    {
        Id = Guid.NewGuid().ToString("N")[..16];
        Art = art;
        ElternId = eltern?.Id;
        ProgrammId = programm?.Id ?? eltern?.ProgrammId;
        ProgrammName = programm?.Name ?? eltern?.ProgrammName;
        ProviderArt = programm?.Art ?? eltern?.ProviderArt;
        _vorher = eltern;
    }

    public string Id { get; }
    public string? ElternId { get; }
    public string Art { get; }
    public string? ProgrammId { get; }
    public string? ProgrammName { get; }
    public string? ProviderArt { get; }
    public TimeSpan Dauer => _uhr.Elapsed;

    /// <summary>Writes the end event exactly once, with duration and verdict.</summary>
    public void Beenden(string ergebnis, string? meldung = null, Schwere schwere = Schwere.Info)
    {
        if (Interlocked.Exchange(ref _beendet, 1) != 0) return;
        Diagnose.Schreiben(schwere, "vorgang", "vorgang.ende", meldung ?? ergebnis, phase: Art, vorgang: this,
            dauerMs: (long)_uhr.Elapsed.TotalMilliseconds, ergebnis: ergebnis);
    }

    public void Dispose()
    {
        Beenden("unbeendet", "Vorgang ohne ausdrückliches Ergebnis verlassen.", Schwere.Warnung);
        Diagnose.Zuruecksetzen(this, _vorher);
    }
}

/// <summary>
/// Central, structured and fail-safe diagnostics: one JSONL event per line, schema versioned,
/// correlated by session, operation and parent operation. Next to it the human-readable daily
/// log stays as it was (Protokollierung). Nothing here may ever break the app or an update --
/// a write that fails goes to a bounded fallback file and raises one visible warning.
/// </summary>
public static class Diagnose
{
    public const int SchemaVersion = 1;

    /// <summary>A new id per app start.</summary>
    public static string SitzungId { get; } = Guid.NewGuid().ToString("N")[..16];

    private static readonly AsyncLocal<Vorgang?> Aktuell = new();

    public static Vorgang? AktuellerVorgang => Aktuell.Value;

    /// <summary>Test seam: the writer; the app uses the log folder of Protokollierung.</summary>
    internal static DiagnoseSchreiber Schreiber { get; set; } = new(() => Protokollierung.Ordner);

    /// <summary>Raised once per session when the primary diagnostics file cannot be written.</summary>
    public static event Action<string>? Warnung;
    public static string? LetzteWarnung { get; private set; }

    internal static void WarnungMelden(string text)
    {
        if (LetzteWarnung is not null) return;
        LetzteWarnung = text;
        try { Warnung?.Invoke(text); } catch { /* a UI handler must not break logging */ }
    }

    /// <summary>
    /// Subscribes AND replays a warning raised before the subscription (e.g. during startup,
    /// before the main view model existed), so no warning stays invisible.
    /// </summary>
    public static void WarnungAbonnieren(Action<string> ziel)
    {
        Warnung += ziel;
        if (LetzteWarnung is { } schon) ziel(schon);
    }

    /// <summary>Test seam: a fresh session state for the one-time warning.</summary>
    internal static void WarnungZuruecksetzen() => LetzteWarnung = null;

    // ------------------------------------------------------------------ operations

    /// <summary>Opens an operation below the current one (if any) and makes it ambient.</summary>
    public static Vorgang VorgangBeginnen(string art, ProgrammEintrag? programm = null, string? meldung = null)
    {
        var vorgang = new Vorgang(art, programm, Aktuell.Value);
        Aktuell.Value = vorgang;
        Schreiben(Schwere.Info, "vorgang", "vorgang.beginn", meldung ?? art, phase: art, vorgang: vorgang);
        return vorgang;
    }

    internal static void Zuruecksetzen(Vorgang vorgang, Vorgang? vorher)
    {
        if (ReferenceEquals(Aktuell.Value, vorgang)) Aktuell.Value = vorher;
    }

    // ------------------------------------------------------------------ events

    public static void Ereignis(Schwere schwere, string komponente, string typ, string meldung, string? phase = null,
                                IReadOnlyDictionary<string, object?>? daten = null)
        => Schreiben(schwere, komponente, typ, meldung, phase, Aktuell.Value, null, null, daten);

    private static readonly ConditionalWeakTable<Exception, object> Gemeldet = new();

    /// <summary>Type, message, stack and inner exceptions -- once per exception object.</summary>
    public static void Ausnahme(Exception? ausnahme, string komponente, string kontext, Schwere schwere = Schwere.Fehler)
    {
        if (ausnahme is null) return;
        lock (Gemeldet)
        {
            if (Gemeldet.TryGetValue(ausnahme, out _)) return;
            Gemeldet.Add(ausnahme, new object());
        }

        var daten = new Dictionary<string, object?>
        {
            ["ausnahmeTyp"] = ausnahme.GetType().FullName,
            ["ausnahmeMeldung"] = Bereinigung.Sicher(ausnahme.Message, 2000),
            ["stack"] = Bereinigung.Sicher(ausnahme.StackTrace, 8000),
            ["hResult"] = ausnahme.HResult,
            ["inner"] = InnereKette(ausnahme)
        };
        Schreiben(schwere, komponente, "ausnahme", kontext + ": " + ausnahme.Message, null, Aktuell.Value, null, null, daten);
    }

    /// <summary>
    /// For best-effort catch blocks that keep the app going: the fallback stays, but the reason
    /// is recorded with the method it happened in.
    /// </summary>
    public static void Gefangen(Exception ausnahme, string komponente, Schwere schwere = Schwere.Warnung,
                                [CallerMemberName] string kontext = "")
        => Ausnahme(ausnahme, komponente, kontext, schwere);

    public static bool SchonProtokolliert(Exception ausnahme)
    {
        lock (Gemeldet) return Gemeldet.TryGetValue(ausnahme, out _);
    }

    private static string InnereKette(Exception ausnahme)
    {
        var teile = new List<string>();
        var innere = ausnahme is AggregateException agg ? agg.InnerExceptions.ToList() : new List<Exception>();
        if (ausnahme.InnerException is { } eine && !innere.Contains(eine)) innere.Add(eine);
        foreach (var i in innere.Take(5))
            teile.Add(i.GetType().FullName + ": " + i.Message + (i.StackTrace is null ? "" : "\n" + i.StackTrace));
        return Bereinigung.Sicher(string.Join("\n---\n", teile), 8000);
    }

    /// <summary>
    /// The three unhandled-exception sources end up here. Nothing is swallowed: the event is
    /// written even if the same object was reported elsewhere before (then as a short reference).
    /// </summary>
    public static void UnbehandelteAusnahme(string quelle, Exception? ausnahme, bool beendetApp)
    {
        if (ausnahme is null)
        {
            Ereignis(Schwere.Fehler, "app", "ausnahme.unbehandelt", "Unbehandelter Fehler ohne Ausnahmeobjekt (" + quelle + ").");
            return;
        }
        if (SchonProtokolliert(ausnahme))
        {
            Ereignis(Schwere.Fehler, "app", "ausnahme.unbehandelt.wiederholt",
                quelle + ": dieselbe Ausnahme wurde bereits protokolliert (" + ausnahme.GetType().Name + ").",
                daten: new Dictionary<string, object?> { ["quelle"] = quelle, ["appBeendet"] = beendetApp });
            return;
        }
        Ausnahme(ausnahme, "app", "Unbehandelt (" + quelle + (beendetApp ? ", App endet" : "") + ")");
    }

    // ------------------------------------------------------------------ app lifecycle

    public static Dictionary<string, object?> Laufzeitinfo()
    {
        var assembly = Assembly.GetExecutingAssembly();
        return new Dictionary<string, object?>
        {
            ["version"] = assembly.GetName().Version?.ToString(3),
            ["build"] = assembly.GetCustomAttributes<AssemblyMetadataAttribute>().FirstOrDefault(a => a.Key == "BuildTimestamp")?.Value,
            ["pid"] = Environment.ProcessId,
            ["architektur"] = RuntimeInformation.ProcessArchitecture.ToString(),
            ["betriebssystem"] = RuntimeInformation.OSDescription,
            ["dotnet"] = RuntimeInformation.FrameworkDescription,
            ["erhoeht"] = Rechte.IstErhoeht,
            ["sitzung"] = SitzungId
        };
    }

    public static void AppGestartet() => Ereignis(Schwere.Info, "app", "app.start", "UpdateZentrale gestartet.", daten: Laufzeitinfo());

    public static void AppBeendet(int exitCode)
    {
        int laufzeit;
        using (var ich = Process.GetCurrentProcess()) laufzeit = (int)(DateTime.Now - ich.StartTime).TotalSeconds;
        Ereignis(Schwere.Info, "app", "app.ende", "UpdateZentrale beendet.",
            daten: new Dictionary<string, object?> { ["exitCode"] = exitCode, ["laufzeitSek"] = laufzeit });
    }

    // ------------------------------------------------------------------ retention

    public const int AufbewahrungTage = 30;

    /// <summary>
    /// Deletes diagnostics and daily logs older than the retention period -- judged by BOTH the
    /// date in the name and the last write time, so an active or recently written file is never
    /// touched. verlauf.jsonl is history and stays.
    /// </summary>
    public static int Aufraeumen(string ordner, DateTime jetzt, int tage = AufbewahrungTage)
    {
        var geloescht = 0;
        if (!Directory.Exists(ordner)) return 0;
        var grenze = jetzt.Date.AddDays(-tage);
        foreach (var datei in Directory.EnumerateFiles(ordner, "*.*")
                     .Where(d => Path.GetFileName(d) is var n
                                 && (n.StartsWith("diagnose-", StringComparison.OrdinalIgnoreCase) && n.EndsWith(".jsonl", StringComparison.OrdinalIgnoreCase)
                                     || n.StartsWith("updates-", StringComparison.OrdinalIgnoreCase) && n.EndsWith(".log", StringComparison.OrdinalIgnoreCase))))
        {
            try
            {
                var name = Path.GetFileName(datei);
                var datumsText = name.Split('-', 2)[1].Split('.')[0];
                if (!DateTime.TryParseExact(datumsText, "yyyy-MM-dd", null, System.Globalization.DateTimeStyles.None, out var datum)) continue;
                if (datum >= grenze || File.GetLastWriteTime(datei) >= grenze) continue;
                File.Delete(datei);
                geloescht++;
            }
            catch (Exception ex)
            {
                Ausnahme(ex, "diagnose", "Aufräumen von " + Path.GetFileName(datei), Schwere.Warnung);
            }
        }
        return geloescht;
    }

    // ------------------------------------------------------------------ the writer

    [ThreadStatic] private static bool _schreibtGerade;

    internal static void Schreiben(Schwere schwere, string komponente, string typ, string meldung, string? phase,
                                   Vorgang? vorgang, long? dauerMs = null, string? ergebnis = null,
                                   IReadOnlyDictionary<string, object?>? daten = null)
    {
        if (_schreibtGerade) return;   // no recursion from inside the writer
        _schreibtGerade = true;
        try
        {
            Schreiber.Schreiben(Zeile(schwere, komponente, typ, meldung, phase, vorgang, dauerMs, ergebnis, daten));
        }
        catch
        {
            // Serialising a strange value must never cost the caller anything.
        }
        finally
        {
            _schreibtGerade = false;
        }
    }

    /// <summary>One event as one JSON line, fields in a fixed order.</summary>
    internal static string Zeile(Schwere schwere, string komponente, string typ, string meldung, string? phase,
                                 Vorgang? vorgang, long? dauerMs, string? ergebnis, IReadOnlyDictionary<string, object?>? daten)
    {
        using var puffer = new MemoryStream();
        // Relaxed escaping keeps umlauts readable in the file; it is still valid JSON (never HTML).
        using (var w = new Utf8JsonWriter(puffer, new JsonWriterOptions { Encoder = System.Text.Encodings.Web.JavaScriptEncoder.UnsafeRelaxedJsonEscaping }))
        {
            w.WriteStartObject();
            w.WriteNumber("schema", SchemaVersion);
            w.WriteString("zeit", DateTimeOffset.UtcNow.ToString("yyyy-MM-dd'T'HH:mm:ss.fff'Z'"));
            w.WriteString("schwere", schwere.ToString().ToLowerInvariant());
            w.WriteString("komponente", Bereinigung.Sicher(komponente, 60));
            w.WriteString("typ", Bereinigung.Sicher(typ, 100));
            w.WriteString("sitzung", SitzungId);
            if (vorgang is not null)
            {
                w.WriteString("vorgang", vorgang.Id);
                if (vorgang.ElternId is not null) w.WriteString("eltern", vorgang.ElternId);
                w.WriteString("vorgangsart", Bereinigung.Sicher(vorgang.Art, 60));
                if (vorgang.ProgrammId is not null) w.WriteString("programm", Bereinigung.Sicher(vorgang.ProgrammId, 100));
                if (vorgang.ProgrammName is not null) w.WriteString("programmName", Bereinigung.Sicher(vorgang.ProgrammName, 200));
                if (vorgang.ProviderArt is not null) w.WriteString("art", Bereinigung.Sicher(vorgang.ProviderArt, 50));
            }
            if (phase is not null) w.WriteString("phase", Bereinigung.Sicher(phase, 60));
            w.WriteString("meldung", Bereinigung.Sicher(meldung, 2000));
            if (dauerMs is not null) w.WriteNumber("dauerMs", dauerMs.Value);
            if (ergebnis is not null) w.WriteString("ergebnis", Bereinigung.Sicher(ergebnis, 200));
            if (daten is { Count: > 0 })
            {
                w.WriteStartObject("daten");
                foreach (var (rohSchluessel, wert) in daten)
                {
                    var schluessel = Bereinigung.Sicher(rohSchluessel, 60);
                    switch (wert)
                    {
                        case null: w.WriteNull(schluessel); break;
                        case bool b: w.WriteBoolean(schluessel, b); break;
                        case int i: w.WriteNumber(schluessel, i); break;
                        case long l: w.WriteNumber(schluessel, l); break;
                        case double d: w.WriteNumber(schluessel, d); break;
                        default: w.WriteString(schluessel, Bereinigung.Sicher(wert.ToString(), 8000)); break;
                    }
                }
                w.WriteEndObject();
            }
            w.WriteEndObject();
        }
        return Encoding.UTF8.GetString(puffer.ToArray());
    }
}

/// <summary>
/// Appends whole lines -- one write call per line, under an in-process lock AND a machine-wide
/// mutex, so neither threads nor a second app instance can interleave half lines. Daily file
/// with size parts; a failing primary file goes to a bounded fallback in %TEMP%.
/// </summary>
public sealed class DiagnoseSchreiber
{
    private readonly Func<string> _ordner;
    private readonly object _schloss = new();
    private readonly string _mutexName;

    public long MaxDateiBytes { get; init; } = 10 * 1024 * 1024;
    public int MaxTeileProTag { get; init; } = 20;
    public long MaxFallbackBytes { get; init; } = 1024 * 1024;
    public string FallbackDatei { get; init; } = Path.Combine(Path.GetTempPath(), "UpdateZentrale", "diagnose-fallback.jsonl");

    public int Verworfen { get; private set; }

    /// <summary>How long to wait for the machine-wide lock before falling back (test seam).</summary>
    public TimeSpan MutexWartezeit { get; init; } = TimeSpan.FromSeconds(2);

    public DiagnoseSchreiber(Func<string> ordner, string mutexName = @"Local\UpdateZentrale.Diagnose")
    {
        _ordner = ordner;
        _mutexName = mutexName;
    }

    public string Datei(DateTime tag, int teil)
        => Path.Combine(_ordner(), "diagnose-" + tag.ToString("yyyy-MM-dd") + (teil <= 1 ? "" : "." + teil) + ".jsonl");

    public void Schreiben(string zeile)
    {
        var bytes = Encoding.UTF8.GetBytes(zeile.Replace("\r", "").Replace("\n", " ") + "\n");
        lock (_schloss)
        {
            Mutex? mutex = null;
            var gehalten = false;
            try
            {
                try
                {
                    mutex = new Mutex(false, _mutexName);
                    gehalten = mutex.WaitOne(MutexWartezeit);
                }
                catch (AbandonedMutexException) { gehalten = true; }
                catch { gehalten = false; }

                // Without the machine-wide lock another process could interleave half lines:
                // never write the primary file unguarded -- the bounded fallback takes it.
                if (!gehalten || !Primaer(bytes)) Fallback(bytes, gehalten ? "nicht beschreibbar" : "gesperrt (Mutex nicht erhalten)");
            }
            finally
            {
                if (gehalten) try { mutex!.ReleaseMutex(); } catch { }
                mutex?.Dispose();
            }
        }
    }

    private bool Primaer(byte[] bytes)
    {
        var heute = DateTime.Now;
        for (var versuch = 0; versuch < 3; versuch++)
        {
            try
            {
                Directory.CreateDirectory(_ordner());
                for (var teil = 1; teil <= MaxTeileProTag; teil++)
                {
                    var datei = Datei(heute, teil);
                    using var strom = new FileStream(datei, FileMode.Append, FileAccess.Write, FileShare.ReadWrite | FileShare.Delete);
                    if (strom.Length > 0 && strom.Length + bytes.Length > MaxDateiBytes)
                    {
                        if (teil < MaxTeileProTag) continue;
                        Verworfen++;           // day budget spent: drop instead of growing without bound
                        Diagnose.WarnungMelden("Das Diagnoseprotokoll hat sein Tageslimit erreicht (" + MaxTeileProTag
                                               + " Teile); weitere Ereignisse werden verworfen.");
                        return true;
                    }
                    strom.Write(bytes, 0, bytes.Length);
                    return true;
                }
                return true;
            }
            catch (IOException)
            {
                Thread.Sleep(30);
            }
            catch
            {
                return false;
            }
        }
        return false;
    }

    private void Fallback(byte[] bytes, string grund)
    {
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(FallbackDatei)!);
            var laenge = File.Exists(FallbackDatei) ? new FileInfo(FallbackDatei).Length : 0;
            if (laenge + bytes.Length <= MaxFallbackBytes)
            {
                using var strom = new FileStream(FallbackDatei, FileMode.Append, FileAccess.Write, FileShare.ReadWrite | FileShare.Delete);
                strom.Write(bytes, 0, bytes.Length);
            }
            else
            {
                Verworfen++;
            }
        }
        catch
        {
            Verworfen++;
        }
        Diagnose.WarnungMelden("Das Diagnoseprotokoll ist " + grund + " (" + _ordner() + "). Ersatzdatei: " + FallbackDatei);
    }
}
