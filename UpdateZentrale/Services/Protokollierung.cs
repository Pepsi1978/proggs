using System.Diagnostics;
using System.IO;
using System.Text;
using System.Text.Json;
using UpdateZentrale.Models;

namespace UpdateZentrale.Services;

/// <summary>
/// Durable logging. Everything an update run produces goes to a daily text file, and the outcome
/// of each run additionally to verlauf.jsonl -- so a failure can still be diagnosed after the
/// window has long been closed.
/// </summary>
public static class Protokollierung
{
    private static readonly object Schloss = new();

    private static readonly JsonSerializerOptions Optionen = new()
    {
        WriteIndented = false,
        PropertyNameCaseInsensitive = true
    };

    public static string Ordner { get; } = Path.Combine(Pfade.BenutzerOrdner, "logs");

    public static string VerlaufsDatei => Path.Combine(Ordner, "verlauf.jsonl");

    public static string TagesDatei(DateTime? zeit = null)
        => Path.Combine(Ordner, "updates-" + (zeit ?? DateTime.Now).ToString("yyyy-MM-dd") + ".log");

    public static void Schreiben(string programmId, string text)
    {
        if (string.IsNullOrWhiteSpace(text)) return;

        var zeilen = text.Replace("\r\n", "\n").Split('\n');
        var stempel = DateTime.Now.ToString("HH:mm:ss");
        var puffer = new StringBuilder();

        foreach (var zeile in zeilen)
        {
            puffer.Append(stempel).Append("  [").Append(programmId).Append("]  ").AppendLine(zeile.TrimEnd());
        }

        Anhaengen(TagesDatei(), puffer.ToString());
    }

    /// <summary>Marks the start of a run, so a log can never be mistaken for a different attempt.</summary>
    public static void LaufBeginnen(ProgrammEintrag eintrag, string befehl, string fingerabdruckVorher)
    {
        var kopf = new StringBuilder()
            .AppendLine(new string('=', 78))
            .AppendLine("Update-Lauf: " + eintrag.Name + "  (" + eintrag.Id + ", Art " + eintrag.Art + ")")
            .AppendLine("Gestartet:   " + DateTime.Now.ToString("dd.MM.yyyy, HH:mm:ss"))
            .AppendLine("Rechte:      " + (Rechte.IstErhoeht ? "Administrator" : "Standardbenutzer"))
            .AppendLine("Befehl:      " + befehl)
            .AppendLine("Stand vorher: " + (string.IsNullOrWhiteSpace(fingerabdruckVorher) ? "(unbekannt)" : fingerabdruckVorher))
            .AppendLine(new string('-', 78))
            .ToString();

        Anhaengen(TagesDatei(), kopf);
    }

    public static void LaufBeenden(UpdateBericht bericht)
    {
        bericht.ProtokollDatei = TagesDatei(bericht.Zeit);

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

        Anhaengen(TagesDatei(bericht.Zeit), fuss);

        try
        {
            Anhaengen(VerlaufsDatei, JsonSerializer.Serialize(bericht, Optionen) + Environment.NewLine);
        }
        catch
        {
            // A broken history entry must never break the update itself.
        }
    }

    /// <summary>Latest report per program id, used to show "last run" on each card.</summary>
    public static Dictionary<string, UpdateBericht> LetzteBerichte()
    {
        var ergebnis = new Dictionary<string, UpdateBericht>(StringComparer.OrdinalIgnoreCase);
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
                }
            }
        }
        catch
        {
        }
        return ergebnis;
    }

    public static void OrdnerOeffnen()
    {
        try
        {
            Directory.CreateDirectory(Ordner);
            Process.Start(new ProcessStartInfo { FileName = Ordner, UseShellExecute = true });
        }
        catch
        {
        }
    }

    public static void DateiOeffnen(string? datei)
    {
        var ziel = string.IsNullOrWhiteSpace(datei) || !File.Exists(datei) ? TagesDatei() : datei;
        try
        {
            if (!File.Exists(ziel)) { OrdnerOeffnen(); return; }
            Process.Start(new ProcessStartInfo { FileName = ziel, UseShellExecute = true });
        }
        catch
        {
            OrdnerOeffnen();
        }
    }

    /// <summary>
    /// Appends with sharing enabled and a short retry: a second instance of the app (or an editor
    /// holding the file open) must not cost a log line.
    /// </summary>
    private static void Anhaengen(string datei, string text)
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
                    return;
                }
                catch (IOException)
                {
                    Thread.Sleep(50);
                }
                catch
                {
                    return;
                }
            }
        }
    }

    private static IEnumerable<string> LiesAlleZeilen(string datei)
    {
        using var strom = new FileStream(datei, FileMode.Open, FileAccess.Read, FileShare.ReadWrite);
        using var leser = new StreamReader(strom, Encoding.UTF8);
        while (leser.ReadLine() is { } zeile) yield return zeile;
    }
}
