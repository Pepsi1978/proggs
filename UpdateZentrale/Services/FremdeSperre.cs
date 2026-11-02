using System.Diagnostics;
using UpdateZentrale.Models;

namespace UpdateZentrale.Services;

/// <summary>
/// Foreign processes that hold an update's files and may not be ended: a SYSTEM task cannot be
/// killed without admin rights, and it usually does useful work. Visual Studio's scheduled
/// BackgroundDownload runs from the installer folder the bootstrapper has to rename, and it runs
/// longest exactly when an update is pending. So an update waits for them instead of colliding.
/// See bugs/bug-cases/2026-11-02-updatezentrale-vs-installer-backgrounddownload-5002.md.
/// </summary>
public static class FremdeSperre
{
    private static readonly TimeSpan Takt = TimeSpan.FromSeconds(30);

    /// <summary>
    /// The listed names that are running now. By name only: a SYSTEM process hides its path from a
    /// normal user, so the path filter of <see cref="Prozessdienst"/> would never match it.
    /// </summary>
    public static IReadOnlyList<string> Laufende(IEnumerable<string> namen)
        => namen.Where(LaeuftProzess).ToList();

    private static bool LaeuftProzess(string name)
    {
        try
        {
            var treffer = Process.GetProcessesByName(name);
            foreach (var prozess in treffer) prozess.Dispose();
            return treffer.Length > 0;
        }
        catch (Exception ausnahme)
        {
            Diagnose.Gefangen(ausnahme, "prozess", Schwere.Warnung);
            return false;
        }
    }

    /// <summary>
    /// Waits until none of the entry's blocking processes runs any more.
    /// Returns null when the way is free, otherwise the message for the failed update.
    /// </summary>
    public static async Task<string?> AbwartenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, Action<string> status,
                                                    CancellationToken abbruch, Func<IReadOnlyList<string>>? laufende = null,
                                                    TimeSpan? takt = null)
    {
        laufende ??= () => Laufende(eintrag.WarteAufProzesse);
        var uhr = Stopwatch.StartNew();
        var limit = TimeSpan.FromMinutes(eintrag.WarteMinuten);
        var gemeldet = false;

        while (true)
        {
            var noch = laufende();
            if (noch.Count == 0)
            {
                if (gemeldet) protokoll.Report("Sperre frei nach " + Dauer(uhr.Elapsed) + ".");
                return null;
            }

            var namen = string.Join(", ", noch);
            if (uhr.Elapsed >= limit)
                return eintrag.Name + " wartet seit " + Dauer(uhr.Elapsed) + " auf " + namen
                       + ". Der Prozess hält die Installer-Dateien und wird nicht beendet – später erneut aktualisieren.";

            if (!gemeldet)
            {
                gemeldet = true;
                protokoll.Report("Warte auf " + namen + " (hält die Installer-Dateien, höchstens " + eintrag.WarteMinuten + " min) …");
                status("Wartet auf " + namen + " …");
            }

            await Task.Delay(takt ?? Takt, abbruch);
        }
    }

    private static string Dauer(TimeSpan dauer) => (int)Math.Ceiling(dauer.TotalMinutes) + " min";
}
