using System.Diagnostics;
using System.IO;
using System.Text;
using System.Text.Json;
using UpdateZentrale.Models;

namespace UpdateZentrale.Services;

/// <summary>
/// Durable logging. Everything an update run produces goes to a daily text file, and the outcome
/// of each run additionally to verlauf.jsonl -- so a failure can still be diagnosed after the
/// window has long been closed. Every variable text is masked and bounded before it is written;
/// the daily file is rotated by size into numbered parts with a bounded count per day.
/// </summary>
public static class Protokollierung
{
    private static readonly object Schloss = new();

    private static readonly JsonSerializerOptions Optionen = new()
    {
        WriteIndented = false,
        PropertyNameCaseInsensitive = true
    };

    /// <summary>Settable only for tests, so they never write into the real history.</summary>
    public static string Ordner { get; internal set; } = Path.Combine(Pfade.BenutzerOrdner, "logs");

    public static string VerlaufsDatei => Path.Combine(Ordner, "verlauf.jsonl");

    /// <summary>Size of one daily text part; test seam.</summary>
    internal static long MaxTextBytes { get; set; } = 10 * 1024 * 1024;

    /// <summary>Parts per day; beyond that lines are dropped (counted, one visible warning).</summary>
    internal static int MaxTextTeile { get; set; } = 10;

    /// <summary>Free room a run header wants in the current part, so header and footer stay together.</summary>
    internal static long KopfReserveBytes { get; set; } = 512 * 1024;

    public static int Verworfen { get; private set; }

    /// <summary>The base file of a day (part 1). Older reports point at it and stay valid.</summary>
    public static string TagesDatei(DateTime? zeit = null) => TagesTeil(zeit ?? DateTime.Now, 1);

    public static string TagesTeil(DateTime tag, int teil)
        => Path.Combine(Ordner, "updates-" + tag.ToString("yyyy-MM-dd") + (teil <= 1 ? "" : "." + teil) + ".log");

    /// <summary>The part currently written for that day (the last existing one).</summary>
    public static string AktuelleTagesDatei(DateTime? zeit = null)
    {
        var tag = zeit ?? DateTime.Now;
        for (var teil = MaxTextTeile; teil > 1; teil--)
            if (File.Exists(TagesTeil(tag, teil))) return TagesTeil(tag, teil);
        return TagesTeil(tag, 1);
    }

    public static void Schreiben(string programmId, string text)
    {
        if (string.IsNullOrWhiteSpace(text)) return;

        // Masked and bounded before anything reaches the disk; one runaway tool output (spinner
        // frames, a whole installer log) must not grow the daily file without limit.
        var zeilen = Bereinigung.Sicher(text, MaxBlockZeichen).Replace("\r\n", "\n").Split('\n');
        var stempel = DateTime.Now.ToString("HH:mm:ss");
        var kennung = Bereinigung.Sicher(programmId, 80);
        var puffer = new StringBuilder();

        foreach (var zeile in zeilen)
        {
            puffer.Append(stempel).Append("  [").Append(kennung).Append("]  ").AppendLine(zeile.TrimEnd());
        }

        TagesAnhaengen(DateTime.Now, puffer.ToString(), reserve: 0);
    }

    /// <summary>Marks the start of a run, so a log can never be mistaken for a different attempt.</summary>
    public static void LaufBeginnen(ProgrammEintrag eintrag, string befehl, string fingerabdruckVorher)
    {
        var kopf = new StringBuilder()
            .AppendLine(new string('=', 78))
            .AppendLine("Update-Lauf: " + Bereinigung.Sicher(eintrag.Name, 200) + "  (" + Bereinigung.Sicher(eintrag.Id, 100)
                        + ", Art " + Bereinigung.Sicher(eintrag.Art, 50) + ")")
            .AppendLine("Gestartet:   " + DateTime.Now.ToString("dd.MM.yyyy, HH:mm:ss"))
            .AppendLine("Rechte:      " + (Rechte.IstErhoeht ? "Administrator" : "Standardbenutzer"))
            .AppendLine("Befehl:      " + Bereinigung.Sicher(befehl, 500))
            .AppendLine("Stand vorher: " + (string.IsNullOrWhiteSpace(fingerabdruckVorher)
                ? "(unbekannt)" : Bereinigung.Sicher(fingerabdruckVorher, 300)))
            .AppendLine(new string('-', 78))
            .ToString();

        // A new part now rather than between header and footer.
        TagesAnhaengen(DateTime.Now, kopf, reserve: KopfReserveBytes);
    }

    /// <summary>Upper bound for one block written to the daily log.</summary>
    public const int MaxBlockZeichen = 6000;

    public static void LaufBeenden(UpdateBericht bericht)
    {
        bericht.ProgrammId = Bereinigung.Sicher(bericht.ProgrammId, 100);
        bericht.Name = Bereinigung.Sicher(bericht.Name, 200);
        bericht.Art = Bereinigung.Sicher(bericht.Art, 50);
        bericht.Meldung = Bereinigung.Sicher(bericht.Meldung, 2000);
        bericht.VersionVorher = Bereinigung.Sicher(bericht.VersionVorher, 300);
        bericht.VersionNachher = Bereinigung.Sicher(bericht.VersionNachher, 300);
        bericht.AusstehendeVersion = bericht.AusstehendeVersion is null ? null : Bereinigung.Sicher(bericht.AusstehendeVersion, 100);
        bericht.Befehl = Bereinigung.Sicher(bericht.Befehl, 500);

        var fuss = new StringBuilder()
            .AppendLine(new string('-', 78))
            .AppendLine("Ergebnis:      " + bericht.ErgebnisText)
            .AppendLine("Begründung:    " + bericht.Meldung)
            .AppendLine("Stand vorher:  " + bericht.VersionVorher)
            .AppendLine("Stand nachher: " + bericht.VersionNachher)
            .AppendLine("Exit-Code:     " + bericht.ExitCode)
            .AppendLine(new string('=', 78))
            .AppendLine()
            .ToString();

        // The report links to the part that really holds the footer.
        var geschrieben = TagesAnhaengen(bericht.Zeit, fuss, reserve: 0);
        bericht.ProtokollDatei = Bereinigung.Sicher(geschrieben ?? AktuelleTagesDatei(bericht.Zeit), 500);

        try
        {
            Anhaengen(VerlaufsDatei, JsonSerializer.Serialize(bericht, Optionen) + Environment.NewLine);
        }
        catch (Exception ex)
        {
            // A broken history entry must never break the update itself -- but it is recorded.
            Diagnose.Ausnahme(ex, "protokoll", "Verlaufseintrag schreiben", Schwere.Warnung);
        }
    }

    /// <summary>Latest report per program id, used to show "last run" on each card.</summary>
    public static Dictionary<string, UpdateBericht> LetzteBerichte()
    {
        var ergebnis = new Dictionary<string, UpdateBericht>(StringComparer.OrdinalIgnoreCase);
        var kaputt = 0;
        try
        {
            if (!File.Exists(VerlaufsDatei)) return ergebnis;

            foreach (var zeile in LiesAlleZeilen(VerlaufsDatei))
            {
                if (string.IsNullOrWhiteSpace(zeile)) continue;
                try
                {
                    var bericht = JsonSerializer.Deserialize<UpdateBericht>(zeile, Optionen);
                    if (bericht is null || string.IsNullOrWhiteSpace(bericht.ProgrammId)) continue;

                    // The file is append-only and chronological, so the last entry wins.
                    ergebnis[bericht.ProgrammId] = bericht;
                }
                catch
                {
                    // Skip a single malformed line instead of losing the whole history.
                    kaputt++;
                }
            }
            if (kaputt > 0)
                Diagnose.Ereignis(Schwere.Warnung, "protokoll", "verlauf.kaputte_zeilen",
                    kaputt + " unlesbare Zeile(n) in verlauf.jsonl übersprungen.");
        }
        catch (Exception ex)
        {
            Diagnose.Ausnahme(ex, "protokoll", "Verlauf lesen", Schwere.Warnung);
        }
        return ergebnis;
    }

    /// <returns>false when Explorer could not be started -- the reason is in the diagnostics.</returns>
    public static bool OrdnerOeffnen()
    {
        try
        {
            Directory.CreateDirectory(Ordner);
            using var _ = Process.Start(new ProcessStartInfo { FileName = Ordner, UseShellExecute = true });
            Diagnose.Ereignis(Schwere.Info, "ui", "diagnose.geoeffnet", "Diagnoseordner geöffnet.");
            return true;
        }
        catch (Exception ex)
        {
            Diagnose.Ausnahme(ex, "ui", "Diagnoseordner öffnen");
            return false;
        }
    }

    public static void DateiOeffnen(string? datei)
    {
        var ziel = string.IsNullOrWhiteSpace(datei) || !File.Exists(datei) ? AktuelleTagesDatei() : datei;
        try
        {
            if (!File.Exists(ziel)) { OrdnerOeffnen(); return; }
            using var _ = Process.Start(new ProcessStartInfo { FileName = ziel, UseShellExecute = true });
        }
        catch (Exception ex)
        {
            Diagnose.Ausnahme(ex, "ui", "Laufprotokoll öffnen", Schwere.Warnung);
            OrdnerOeffnen();
        }
    }

    /// <summary>
    /// Appends to the day's current part: a part that would exceed its size (or, for a run header,
    /// would not keep the reserve free) is closed and the next one opened. With every part of the
    /// day full, the text is dropped, counted, and one visible warning is raised.
    /// </summary>
    /// <returns>The file actually written, or null when nothing was written.</returns>
    private static string? TagesAnhaengen(DateTime tag, string text, long reserve)
    {
        var laenge = Encoding.UTF8.GetByteCount(text);
        lock (Schloss)
        {
            for (var teil = 1; teil <= MaxTextTeile; teil++)
            {
                var datei = TagesTeil(tag, teil);
                long vorhanden;
                try { vorhanden = File.Exists(datei) ? new FileInfo(datei).Length : 0; }
                catch { vorhanden = 0; }

                var naechsterBelegt = teil < MaxTextTeile && File.Exists(TagesTeil(tag, teil + 1));
                if (naechsterBelegt) continue;   // an older, already closed part
                if (vorhanden > 0 && vorhanden + laenge + reserve > MaxTextBytes && teil < MaxTextTeile) continue;
                if (vorhanden > 0 && vorhanden + laenge > MaxTextBytes) break;   // last part full

                return Anhaengen(datei, text) ? datei : null;
            }

            Verworfen++;
        }
        Diagnose.WarnungMelden("Das Tagesprotokoll hat sein Tageslimit erreicht (" + MaxTextTeile + " × "
                               + MaxTextBytes / (1024 * 1024) + " MB); weitere Zeilen werden verworfen.");
        return null;
    }

    /// <summary>
    /// Appends with sharing enabled and a short retry: a second instance of the app (or an editor
    /// holding the file open) must not cost a log line.
    /// </summary>
    private static bool Anhaengen(string datei, string text)
    {
        lock (Schloss)
        {
            for (var versuch = 0; versuch < 3; versuch++)
            {
                try
                {
                    Directory.CreateDirectory(Ordner);
                    using var strom = new FileStream(datei, FileMode.Append, FileAccess.Write, FileShare.ReadWrite);
                    using var schreiber = new StreamWriter(strom, Encoding.UTF8);
                    schreiber.Write(text);
                    return true;
                }
                catch (IOException ex)
                {
                    if (versuch == 2) Diagnose.Ausnahme(ex, "protokoll", "Tageslog schreiben", Schwere.Warnung);
                    Thread.Sleep(50);
                }
                catch (Exception ex)
                {
                    Diagnose.Ausnahme(ex, "protokoll", "Tageslog schreiben", Schwere.Warnung);
                    return false;
                }
            }
            return false;
        }
    }

    private static IEnumerable<string> LiesAlleZeilen(string datei)
    {
        using var strom = new FileStream(datei, FileMode.Open, FileAccess.Read, FileShare.ReadWrite);
        using var leser = new StreamReader(strom, Encoding.UTF8);
        while (leser.ReadLine() is { } zeile) yield return zeile;
    }
}
