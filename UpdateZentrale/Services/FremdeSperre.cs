using System.Diagnostics;
using System.Text.RegularExpressions;
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

    /// <summary>A vendor task still working is no defect: this verdict starts no repair session.</summary>
    internal const string LimitMeldungPraefix = "Noch gesperrt: ";

    /// <summary>
    /// The listed names that are running now. By name only: a SYSTEM process hides its path from a
    /// normal user, so the path filter of <see cref="Prozessdienst"/> would never match it.
    /// </summary>
    public static IReadOnlyList<string> Laufende(IEnumerable<string> namen)
        => namen.Where(Prozessdienst.LaeuftNachName).ToList();

    /// <summary>
    /// Waits until none of the entry's blocking processes runs any more.
    /// Returns null when the way is free, otherwise the message for the failed update.
    /// </summary>
    /// <param name="rest">What is left of the entry's waiting budget; a whole update chain shares one.</param>
    public static async Task<string?> AbwartenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, Action<string> status,
                                                    TimeSpan rest, Func<IReadOnlyList<string>> laufende,
                                                    Func<TimeSpan, CancellationToken, Task> warten, CancellationToken abbruch)
    {
        var uhr = Stopwatch.StartNew();
        var gemeldet = false;

        while (true)
        {
            var noch = laufende();
            if (noch.Count == 0)
            {
                if (gemeldet) protokoll.Report("Sperre frei nach " + (int)uhr.Elapsed.TotalMinutes + " min.");
                return null;
            }

            var namen = string.Join(", ", noch);
            if (uhr.Elapsed >= rest)
                return LimitMeldungPraefix + namen + " sperrt das Update von " + eintrag.Name + " auch nach "
                       + eintrag.WarteMinuten + " min Wartezeit. Der Prozess wird nicht beendet – später erneut aktualisieren.";

            if (!gemeldet)
            {
                gemeldet = true;
                protokoll.Report("Warte auf " + namen + " (sperrt das Update, höchstens " + (int)Math.Ceiling(rest.TotalMinutes) + " min) …");
                status("Wartet auf " + namen + " …");
            }

            await warten(Takt, abbruch);
        }
    }

    /// <summary>The failed run names the blocker's collision (catalog: sperrFehlerMuster), whether or not it still runs.</summary>
    internal static bool IstSperrFehler(ProgrammEintrag eintrag, PruefErgebnis ergebnis)
        => ergebnis.Zustand == UpdateZustand.Fehler
           && !string.IsNullOrWhiteSpace(eintrag.SperrFehlerMuster)
           && Regex.IsMatch(ergebnis.Protokoll ?? "", eintrag.SperrFehlerMuster);
}
