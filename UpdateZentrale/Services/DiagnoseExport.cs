using System.IO;
using System.IO.Compression;
using System.Text;
using System.Text.Json;
using UpdateZentrale.Models;

namespace UpdateZentrale.Services;

/// <summary>
/// Builds a local ZIP for a later Codex/Claude session: recent structured and readable logs,
/// runtime metadata, a catalog summary and a README -- every text masked and size-bounded again
/// on the way in. No dumps, no registry, no environment, no settings file.
/// </summary>
public static class DiagnoseExport
{
    public const long MaxDateiBytes = 4 * 1024 * 1024;
    public const long MaxGesamtBytes = 20 * 1024 * 1024;
    public const int Tage = 7;

    public static string ExportOrdner => Path.Combine(Pfade.BenutzerOrdner, "diagnose-exporte");

    public static string Erstellen(string logOrdner, string zielOrdner, DateTime jetzt, IEnumerable<ProgrammEintrag> katalog,
                                   IReadOnlyDictionary<string, object?> laufzeit)
        => Erstellen(logOrdner, zielOrdner, jetzt, katalog, laufzeit, MaxDateiBytes, MaxGesamtBytes);

    /// <param name="maxGesamt">Hard limit for the SUM of all uncompressed entries.</param>
    internal static string Erstellen(string logOrdner, string zielOrdner, DateTime jetzt, IEnumerable<ProgrammEintrag> katalog,
                                     IReadOnlyDictionary<string, object?> laufzeit, long maxDatei, long maxGesamt)
    {
        Directory.CreateDirectory(zielOrdner);
        var ziel = Path.Combine(zielOrdner, "UpdateZentrale-Diagnose-" + jetzt.ToString("yyyyMMdd-HHmmss") + ".zip");
        // Unique per call: a failure removes exactly this file and never a foreign one.
        var temp = ziel + "." + Guid.NewGuid().ToString("N") + ".tmp";
        long gesamt = 0;

        try
        {
            using (var zip = ZipFile.Open(temp, ZipArchiveMode.Create))
            {
                // Small, essential entries first; the logs fill what is left, newest first.
                Eintragen(zip, "README.md", Anleitung(jetzt), maxGesamt, ref gesamt);
                var metadaten = laufzeit.ToDictionary(p => Bereinigung.Sicher(p.Key, 60),
                    p => p.Value is string s ? Bereinigung.Sicher(s, 500) : p.Value);
                metadaten["erstellt"] = jetzt.ToString("o");
                metadaten["schema"] = Diagnose.SchemaVersion;
                metadaten["tage"] = Tage;
                Eintragen(zip, "metadaten.json", JsonSerializer.Serialize(metadaten, new JsonSerializerOptions { WriteIndented = true }),
                    maxGesamt, ref gesamt);
                Eintragen(zip, "katalog.json", KatalogZusammenfassung(katalog), maxGesamt, ref gesamt);

                if (Directory.Exists(logOrdner))
                {
                    var grenze = jetzt.Date.AddDays(-Tage);
                    var dateien = Directory.EnumerateFiles(logOrdner)
                        .Where(d => Path.GetFileName(d) is var n
                                    && (n.StartsWith("diagnose-") && n.EndsWith(".jsonl") || n.StartsWith("updates-") && n.EndsWith(".log") || n == "verlauf.jsonl"))
                        .Where(d => File.GetLastWriteTime(d) >= grenze)
                        .OrderByDescending(File.GetLastWriteTime);

                    foreach (var datei in dateien)
                    {
                        if (!Eintragen(zip, "logs/" + Path.GetFileName(datei), Lesen(datei, maxDatei), maxGesamt, ref gesamt)) break;
                    }
                }
            }

            File.Move(temp, ziel, overwrite: true);
            return ziel;
        }
        catch
        {
            try { if (File.Exists(temp)) File.Delete(temp); } catch { /* best effort; the original error matters */ }
            throw;
        }
    }

    /// <summary>Tail of the file (newest lines), masked line by line, within the per-file limit.</summary>
    private static string Lesen(string datei, long maxDatei)
    {
        string text;
        using (var strom = new FileStream(datei, FileMode.Open, FileAccess.Read, FileShare.ReadWrite | FileShare.Delete))
        {
            var start = Math.Max(0, strom.Length - maxDatei);
            strom.Seek(start, SeekOrigin.Begin);
            using var leser = new StreamReader(strom, Encoding.UTF8);
            text = leser.ReadToEnd();
            if (start > 0)
            {
                var ersteZeile = text.IndexOf('\n');
                text = "…[gekürzt: ältere " + start + " Bytes nicht im Paket]…\n" + (ersteZeile >= 0 ? text[(ersteZeile + 1)..] : text);
            }
        }
        return string.Join("\n", text.Split('\n').Select(z => Bereinigung.Sicher(z, 20000)));
    }

    /// <summary>
    /// Adds an entry within the remaining byte budget -- never beyond it. Oversized content is cut
    /// on a UTF-8 character boundary (with a marker when there is room for it).
    /// </summary>
    /// <returns>false when the budget is spent and no entry was added.</returns>
    internal static bool Eintragen(ZipArchive zip, string name, string inhalt, long maxGesamt, ref long gesamt)
    {
        var rest = maxGesamt - gesamt;
        if (rest <= 0) return false;

        var bytes = Encoding.UTF8.GetBytes(inhalt);
        if (bytes.Length > rest)
        {
            var marke = "\n…[gekürzt: Diagnosepaket-Grenze erreicht, " + bytes.Length + " Bytes im Original]…\n";
            var markenBytes = Encoding.UTF8.GetByteCount(marke);
            bytes = rest > markenBytes
                ? Encoding.UTF8.GetBytes(Utf8Praefix(inhalt, (int)(rest - markenBytes)) + marke)
                : Encoding.UTF8.GetBytes(Utf8Praefix(inhalt, (int)rest));
        }

        var eintrag = zip.CreateEntry(name, CompressionLevel.Optimal);
        using (var strom = eintrag.Open()) strom.Write(bytes, 0, bytes.Length);
        gesamt += bytes.Length;
        return true;
    }

    /// <summary>The longest prefix whose UTF-8 form fits into maxBytes, never splitting a character.</summary>
    internal static string Utf8Praefix(string text, int maxBytes)
    {
        if (maxBytes <= 0) return "";
        if (Encoding.UTF8.GetByteCount(text) <= maxBytes) return text;
        int unten = 0, oben = text.Length;
        while (unten < oben)
        {
            var mitte = (unten + oben + 1) / 2;
            var laenge = mitte;
            if (laenge > 0 && char.IsHighSurrogate(text[laenge - 1])) laenge--;
            if (Encoding.UTF8.GetByteCount(text.AsSpan(0, laenge)) <= maxBytes) unten = mitte; else oben = mitte - 1;
        }
        var ende = unten;
        if (ende > 0 && char.IsHighSurrogate(text[ende - 1])) ende--;
        return text[..ende];
    }

    /// <summary>What the app manages, without arguments that could carry secrets verbatim.</summary>
    private static string KatalogZusammenfassung(IEnumerable<ProgrammEintrag> katalog)
        => JsonSerializer.Serialize(katalog.Select(e => new
        {
            Id = Bereinigung.Sicher(e.Id, 100),
            Name = Bereinigung.Sicher(e.Name, 200),
            Art = Bereinigung.Sicher(e.Art, 50),
            Gruppe = Bereinigung.Sicher(e.Gruppe, 100),
            WingetId = Bereinigung.Sicher(e.WingetId, 200),
            StoreProduktId = Bereinigung.Sicher(e.StoreProduktId, 100),
            RepoOrdner = Bereinigung.Sicher(e.RepoOrdner, 200),
            Skript = Bereinigung.Sicher(e.Skript, 300),
            SkriptArgumente = Bereinigung.Sicher(e.SkriptArgumente, 200),
            UpdateArgumente = Bereinigung.Sicher(e.UpdateArgumente, 200),
            PruefArgumente = Bereinigung.Sicher(e.PruefArgumente, 200),
            Exe = Bereinigung.Sicher(e.ExePfad, 300),
            e.BeendenVorUpdate,
            e.ZeitlimitMinuten
        }), new JsonSerializerOptions { WriteIndented = true });

    private static string Anleitung(DateTime jetzt) =>
        "# UpdateZentrale – Diagnosepaket\n\n"
        + "Erstellt: " + jetzt.ToString("dd.MM.yyyy HH:mm:ss") + "\n\n"
        + "## Inhalt\n"
        + "- `metadaten.json`: Version, Build, PID, Architektur, Betriebssystem, .NET, Rechte, Sitzungs-ID.\n"
        + "- `katalog.json`: verwaltete Programme (bereinigt).\n"
        + "- `logs/diagnose-*.jsonl`: strukturierte Ereignisse, eine JSON-Zeile je Ereignis (Schema " + Diagnose.SchemaVersion + ").\n"
        + "- `logs/updates-*.log`: lesbare Tagesprotokolle; `logs/verlauf.jsonl`: Ergebnis je Update-Lauf.\n\n"
        + "## Auswertung (Codex/Claude)\n"
        + "1. `metadaten.json` lesen (Version, Sitzung).\n"
        + "2. In `diagnose-*.jsonl` nach `schwere` = `fehler`/`warnung` und `typ` = `ausnahme` filtern.\n"
        + "3. Zu einem Fehler alle Zeilen mit derselben `vorgang`-ID holen; `eltern` verbindet Sammelläufe mit Einzelvorgängen.\n"
        + "4. `befehl.beginn`/`befehl.ende` (Komponente `kommando`) zeigen externe Befehle mit Zeitlimit, Exit-Code, Dauer,\n"
        + "   Abbruch/Zeitlimit/Pipe/Beenden-Problem und bereinigter, gekürzter Ausgabe.\n"
        + "5. `vorgang.ende` trägt `dauerMs` und `ergebnis`; `kette.*` zeigt die Update-Kette Durchlauf für Durchlauf.\n\n"
        + "Alle Texte sind bereinigt (Kennwörter, Tokens, Schlüssel, Autorisierung maskiert als `***`) und gekürzt\n"
        + "(sichtbare Marke `…[gekürzt: …]…`). Das Paket enthält keine Umgebungsvariablen, Registry oder Speicherabbilder.\n";
}
