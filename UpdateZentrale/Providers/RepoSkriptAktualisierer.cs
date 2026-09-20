using System.IO;
using System.Text.RegularExpressions;
using UpdateZentrale.Models;
using UpdateZentrale.Services;

namespace UpdateZentrale.Providers;

/// <summary>
/// Own tools in ~/proggs. Updating never reimplements the existing PowerShell scripts, it only
/// calls them -- and never with -Force, because each script opens its own yes/no window that the
/// user has to answer.
/// </summary>
public sealed class RepoSkriptAktualisierer : IAktualisierer
{
    public string Art => "reposkript";

    public async Task<PruefErgebnis> PruefenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        var installiert = InstallierteVersion(eintrag);
        var quelle = QuellVersion(eintrag);

        // Signal 1: does the remote carry commits for this folder that are not here yet?
        var hinterstand = 0;
        if (!string.IsNullOrWhiteSpace(eintrag.RepoOrdner))
        {
            await Kommandozeile.AusfuehrenAsync("git", "fetch --quiet", TimeSpan.FromMinutes(3), Pfade.RepoWurzel, abbruch);
            var zaehl = await Kommandozeile.AusfuehrenAsync("git",
                "rev-list --count HEAD..origin/main -- \"" + eintrag.RepoOrdner + "\"",
                TimeSpan.FromMinutes(2), Pfade.RepoWurzel, abbruch);
            int.TryParse(zaehl.Ausgabe.Trim(), out hinterstand);
            protokoll.Report("git rev-list HEAD..origin/main -- " + eintrag.RepoOrdner + " -> " + hinterstand);
        }

        // Signal 2: is the source version newer than the built exe?
        var quellstandNeuer = !string.IsNullOrWhiteSpace(quelle)
                              && !string.IsNullOrWhiteSpace(installiert)
                              && !installiert.StartsWith(quelle, StringComparison.Ordinal);

        protokoll.Report("gebaut: " + installiert + " | Quelle: " + quelle);

        if (string.IsNullOrWhiteSpace(installiert))
            return new PruefErgebnis(UpdateZustand.NichtInstalliert, "", quelle, "Noch nicht gebaut.");

        if (hinterstand > 0 || quellstandNeuer)
        {
            var grund = hinterstand > 0
                ? hinterstand + " neue Commit(s) auf origin/main"
                : "Quellstand " + quelle + " neuer als der gebaute Stand";
            return new PruefErgebnis(UpdateZustand.UpdateVerfuegbar, installiert, quelle, grund + ".");
        }

        return new PruefErgebnis(UpdateZustand.Aktuell, installiert, quelle, "Gebauter Stand ist aktuell.");
    }

    public async Task<PruefErgebnis> AktualisierenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        var skript = Path.Combine(Pfade.RepoWurzel, (eintrag.Skript ?? "").Replace('/', Path.DirectorySeparatorChar));
        if (!File.Exists(skript))
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: "Skript fehlt: " + skript);

        var args = ("-NoProfile -ExecutionPolicy Bypass -File \"" + skript + "\" " + eintrag.SkriptArgumente).TrimEnd();
        protokoll.Report("pwsh " + args);
        protokoll.Report("Das Skript zeigt jetzt ein Ja/Nein-Fenster. Ohne Klick auf 'Ja' passiert nichts.");

        // The scripts wait up to 240 s for the click, so the timeout must sit clearly above that.
        var zeitlimit = TimeSpan.FromSeconds(Math.Max(eintrag.DialogWartezeitSekunden, 300) + eintrag.ZeitlimitMinuten * 60);
        var lauf = await Kommandozeile.AusfuehrenAsync(PwshPfad(), args, zeitlimit, Pfade.RepoWurzel, abbruch);
        protokoll.Report(lauf.Ausgabe);

        var status = StatusLesen(lauf.Ausgabe, eintrag.StatusPraefix);
        if (status == "cancelled")
            return new PruefErgebnis(UpdateZustand.Abgebrochen,
                Meldung: "Du hast im Ja/Nein-Fenster auf 'Nein' geklickt - es wurde nichts geaendert.", Protokoll: lauf.Ausgabe);
        if (status == "no-answer")
            return new PruefErgebnis(UpdateZustand.Abgebrochen,
                Meldung: "Kein Klick im Zeitfenster - es wurde nichts geaendert.", Protokoll: lauf.Ausgabe);
        if (status == "already-current")
            return new PruefErgebnis(UpdateZustand.Aktuell, Meldung: "War bereits aktuell.", Protokoll: lauf.Ausgabe);
        if (status == "started")
            return new PruefErgebnis(UpdateZustand.Fertig, Meldung: "Neue Version gebaut und gestartet.", Protokoll: lauf.Ausgabe);
        if (lauf.Abgelaufen)
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: "Zeitlimit ueberschritten.", Protokoll: lauf.Ausgabe);
        if (lauf.ExitCode == 0)
            return new PruefErgebnis(UpdateZustand.Fertig, Meldung: "Skript erfolgreich durchgelaufen.", Protokoll: lauf.Ausgabe);

        return new PruefErgebnis(UpdateZustand.Fehler,
            Meldung: "Skript endete mit Code " + lauf.ExitCode + ".", Protokoll: lauf.Ausgabe);
    }

    /// <summary>rebuild-overlay.ps1 requires PowerShell 7; fall back only if pwsh is missing.</summary>
    private static string PwshPfad()
    {
        var kandidaten = new[]
        {
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "PowerShell", "7", "pwsh.exe"),
            Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Microsoft", "WindowsApps", "pwsh.exe")
        };
        return kandidaten.FirstOrDefault(File.Exists) ?? "powershell.exe";
    }

    private static string StatusLesen(string ausgabe, string? praefix)
    {
        if (string.IsNullOrWhiteSpace(praefix)) return "";
        foreach (var zeile in ausgabe.Split('\n'))
        {
            var index = zeile.IndexOf(praefix, StringComparison.OrdinalIgnoreCase);
            if (index < 0) continue;
            var rest = zeile.Substring(index + praefix.Length).Trim();
            return rest.Split(' ', StringSplitOptions.RemoveEmptyEntries).FirstOrDefault() ?? "";
        }
        return "";
    }

    private static string InstallierteVersion(ProgrammEintrag eintrag)
    {
        var exe = Pfade.Aufloesen(eintrag.ExePfad);
        if (string.IsNullOrWhiteSpace(exe) || !File.Exists(exe)) return "";
        try
        {
            var info = System.Diagnostics.FileVersionInfo.GetVersionInfo(exe);
            return info.FileVersion ?? info.ProductVersion ?? "";
        }
        catch
        {
            return "";
        }
    }

    /// <summary>Reads the csproj Version element -- the single source of truth for these tools.</summary>
    private static string QuellVersion(ProgrammEintrag eintrag)
    {
        if (string.IsNullOrWhiteSpace(eintrag.ProjektDatei)) return "";
        var datei = Path.Combine(Pfade.RepoWurzel, eintrag.ProjektDatei.Replace('/', Path.DirectorySeparatorChar));
        if (!File.Exists(datei)) return "";
        try
        {
            var treffer = Regex.Match(File.ReadAllText(datei), @"<Version>\s*([^<\s]+)\s*</Version>");
            return treffer.Success ? treffer.Groups[1].Value : "";
        }
        catch
        {
            return "";
        }
    }
}
