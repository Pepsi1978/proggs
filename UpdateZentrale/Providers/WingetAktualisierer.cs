using System.IO;
using UpdateZentrale.Models;
using UpdateZentrale.Services;

namespace UpdateZentrale.Providers;

/// <summary>
/// winget packages. The CLI has no JSON output for "list", so the table is parsed by the column
/// positions of its header row -- splitting on whitespace breaks on names like "LM Studio 0.4.24+1".
/// </summary>
public sealed class WingetAktualisierer : IAktualisierer
{
    public string Art => "winget";

    public async Task<PruefErgebnis> PruefenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        if (!File.Exists(Pfade.Winget))
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: "winget wurde nicht gefunden.");

        var lauf = await ListeAsync(eintrag, protokoll, abbruch);
        return ListeAuswerten(lauf, eintrag.WingetId ?? "", HierVorhanden(eintrag));
    }

    /// <summary>
    /// How winget is told which package: by id, or -- for packages winget only correlates by
    /// display name -- by name. See <see cref="ProgrammEintrag.WingetName"/>.
    /// </summary>
    internal static string Auswahl(ProgrammEintrag eintrag)
        => string.IsNullOrWhiteSpace(eintrag.WingetName)
            ? $"--id {eintrag.WingetId} --exact"
            : $"--name \"{eintrag.WingetName.Trim()}\"";

    /// <summary>Verb and package selection of the update run -- see <see cref="ProgrammEintrag.WingetUpdatePerInstall"/>.</summary>
    internal static string UpdateBefehl(ProgrammEintrag eintrag)
        => (eintrag.WingetUpdatePerInstall
               ? $"install --id {eintrag.WingetId} --exact --force"
               : "upgrade " + Auswahl(eintrag))
           + (string.IsNullOrWhiteSpace(eintrag.WingetInstallerTyp) ? "" : " --installer-type " + eintrag.WingetInstallerTyp.Trim());

    /// <summary>Something on this machine says the program is there, whatever winget answers.</summary>
    private static bool HierVorhanden(ProgrammEintrag eintrag)
        => eintrag.IstPaketApp || (!string.IsNullOrWhiteSpace(eintrag.ExePfadWirksam)
                                   && File.Exists(Pfade.Aufloesen(eintrag.ExePfadWirksam)));

    /// <summary>
    /// winget keeps one lock on its sources; several runs at once only queue up inside it and
    /// can time out there. All checks share this one lane, everything else runs beside it.
    /// </summary>
    internal static readonly SemaphoreSlim Reihe = new(1, 1);

    private static string QuellArgument(ProgrammEintrag eintrag)
        => string.IsNullOrWhiteSpace(eintrag.WingetQuelle) ? "" : " --source " + eintrag.WingetQuelle.Trim();

    /// <summary>
    /// "winget list" for one package, limited to its source. Without a source winget also asks
    /// the Store for every query (measured: 46 s against 1 s). Should the package not be found
    /// in the named source after all, the slow query over all sources decides -- the fast path
    /// must never turn an installed program into "not installed".
    /// </summary>
    private static async Task<BefehlErgebnis> ListeAsync(ProgrammEintrag eintrag, IProgress<string>? protokoll, CancellationToken abbruch)
    {
        var basis = $"list {Auswahl(eintrag)} --disable-interactivity --accept-source-agreements";
        var quelle = QuellArgument(eintrag);

        await Reihe.WaitAsync(abbruch);
        try
        {
            protokoll?.Report("winget " + basis + quelle);
            var lauf = await Kommandozeile.AusfuehrenAsync(Pfade.Winget, basis + quelle, TimeSpan.FromMinutes(2), abbruch: abbruch);
            protokoll?.Report(lauf.Ausgabe);

            var gefunden = !lauf.Abgelaufen && lauf.ExitCode == 0 && !MeldetKeinPaket(lauf.Ausgabe)
                           && TabellenZeile(lauf.Ausgabe, eintrag.WingetId ?? "") is not null;
            if (quelle.Length == 0 || gefunden) return lauf;

            // A failed run is a failed run -- asking again more slowly does not repair it. And
            // "not found" is believed when nothing on this machine says otherwise.
            var keinPaket = lauf.ExitCode == KeinPaketGefunden || (lauf.ExitCode == 0 && !lauf.Abgelaufen);
            if (!keinPaket || !HierVorhanden(eintrag)) return lauf;

            protokoll?.Report("In der Quelle „" + eintrag.WingetQuelle + "“ nicht eindeutig – Abfrage über alle Quellen: winget " + basis);
            lauf = await Kommandozeile.AusfuehrenAsync(Pfade.Winget, basis, TimeSpan.FromMinutes(3), abbruch: abbruch);
            protokoll?.Report(lauf.Ausgabe);
            return lauf;
        }
        finally
        {
            Reihe.Release();
        }
    }

    /// <summary>winget's exit code for "no installed package matches" (measured with v1.29).</summary>
    internal const int KeinPaketGefunden = unchecked((int)0x8A150014);

    /// <summary>winget's exit code for "installed, no applicable upgrade".</summary>
    internal const int KeinAnwendbaresUpgrade = unchecked((int)0x8A15002B);

    /// <summary>
    /// Process failures first: a timed-out or failed run has no package row either, and used to
    /// read as "not installed". Only winget's own "no package found" answer means that.
    /// </summary>
    internal static PruefErgebnis ListeAuswerten(BefehlErgebnis lauf, string id, bool hierVorhanden = false)
    {
        if (lauf.Abgelaufen)
            return new PruefErgebnis(UpdateZustand.Fehler,
                Meldung: "winget hat das Zeitlimit überschritten – der Stand ist unbekannt.", Protokoll: lauf.Ausgabe);

        var keinPaket = lauf.ExitCode == KeinPaketGefunden || (lauf.ExitCode == 0 && MeldetKeinPaket(lauf.Ausgabe));

        // The program file is right there: winget merely cannot correlate it. "Not installed"
        // would be a plain lie on the card -- exactly what the PowerShell card used to say.
        if (keinPaket && hierVorhanden)
            return new PruefErgebnis(UpdateZustand.Unbekannt,
                Meldung: "Installiert, aber winget ordnet es dem Paket nicht zu – im Katalog „wingetName“ setzen.",
                Protokoll: lauf.Ausgabe);

        if (keinPaket)
            return new PruefErgebnis(UpdateZustand.NichtInstalliert,
                Meldung: "Nicht über winget installiert.", Protokoll: lauf.Ausgabe);

        if (lauf.ExitCode != 0)
            return new PruefErgebnis(UpdateZustand.Fehler,
                Meldung: "winget endete mit Code " + Code(lauf.ExitCode) + " – der Stand ist unbekannt.", Protokoll: lauf.Ausgabe);

        var zeile = TabellenZeile(lauf.Ausgabe, id);
        if (zeile is null)
            // A clean run without the package row means the output shape changed, not that the
            // program is missing -- saying "not installed" there would be a lie.
            return new PruefErgebnis(UpdateZustand.Unbekannt,
                Meldung: "winget-Ausgabe war nicht lesbar – siehe Protokoll.", Protokoll: lauf.Ausgabe);

        var (installiert, verfuegbar) = zeile.Value;
        if (string.IsNullOrWhiteSpace(installiert))
            return new PruefErgebnis(UpdateZustand.Unbekannt,
                Meldung: "winget nannte keine installierte Version – siehe Protokoll.", Protokoll: lauf.Ausgabe);

        // Android Studio registers "2026.2" while winget offers "2026.2.1.8" for the same build:
        // a shortened installed version is the offered one, not an endless pending update.
        var zustand = string.IsNullOrWhiteSpace(verfuegbar) || verfuegbar == installiert
                      || verfuegbar.StartsWith(installiert + ".", StringComparison.Ordinal)
            ? UpdateZustand.Aktuell
            : UpdateZustand.UpdateVerfuegbar;

        return new PruefErgebnis(zustand, installiert, verfuegbar,
            zustand == UpdateZustand.Aktuell ? "Auf dem neuesten Stand." : $"Neue Version {verfuegbar} verfügbar.",
            lauf.Ausgabe);
    }

    public async Task<PruefErgebnis> AktualisierenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch)
    {
        if (!File.Exists(Pfade.Winget))
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: "winget wurde nicht gefunden.");

        var args = UpdateBefehl(eintrag) + " --silent --disable-interactivity "
                 + "--accept-source-agreements --accept-package-agreements" + QuellArgument(eintrag);
        protokoll.Report($"winget {args}");

        var lauf = await Kommandozeile.AusfuehrenAsync(
            Pfade.Winget, args, TimeSpan.FromMinutes(eintrag.ZeitlimitMinuten), abbruch: abbruch);
        protokoll.Report(lauf.Ausgabe);

        if (Kommandozeile.UnsauberesEnde(lauf) is { } unsauber) return unsauber;

        // Nothing to do is not a failure: the card must say "current", not show a red band.
        if (IstNichtsZuTun(lauf))
            return new PruefErgebnis(UpdateZustand.Aktuell, Meldung: "War bereits aktuell – kein Upgrade nötig.", Protokoll: lauf.Ausgabe);

        // Files in use (pwsh, node): the installer replaced what it could and finishes on reboot.
        // That is a pending update, not a failure.
        if (BrauchtNeustart(lauf))
        {
            return new PruefErgebnis(UpdateZustand.Fertig,
                Meldung: "Installiert – wird nach dem nächsten Neustart des PCs vollständig übernommen.",
                Protokoll: lauf.Ausgabe, ErstNachNeustart: true);
        }

        // Some manifests forbid upgrades through winget (Android Studio). Saying so beats a hex code.
        if (NurUeberHersteller(lauf))
            return new PruefErgebnis(UpdateZustand.Fehler,
                Meldung: eintrag.Name + " lässt sich nicht über winget aktualisieren – das Update im Programm selbst starten.",
                Protokoll: lauf.Ausgabe);

        if (lauf.ExitCode is InstallationInBenutzung or DateiInBenutzung)
            return new PruefErgebnis(UpdateZustand.Fehler,
                Meldung: eintrag.Name + " ist gerade in Benutzung – schließen und erneut aktualisieren.",
                Protokoll: lauf.Ausgabe);

        if (lauf.ExitCode != 0)
        {
            var grund = StoreAktualisierer.BrauchtRechte(lauf.Ausgabe)
                ? "Dafür fehlen Administratorrechte – oben auf „Als Administrator neu starten“ klicken."
                : "winget endete mit Code " + Code(lauf.ExitCode) + ".";
            return new PruefErgebnis(UpdateZustand.Fehler, Meldung: grund, Protokoll: lauf.Ausgabe);
        }

        // Some installers (Claude Desktop) only stage the update and swap it in on next launch.
        if (lauf.Ausgabe.Contains("Restart the application", StringComparison.OrdinalIgnoreCase)
            || lauf.Ausgabe.Contains("Starten Sie die Anwendung neu", StringComparison.OrdinalIgnoreCase))
        {
            return new PruefErgebnis(UpdateZustand.Fertig,
                Meldung: "Installiert – wird beim nächsten Start von " + eintrag.Name + " übernommen.",
                Protokoll: lauf.Ausgabe, ErstNachNeustart: true);
        }

        return new PruefErgebnis(UpdateZustand.Fertig, Meldung: "Update installiert.", Protokoll: lauf.Ausgabe);
    }

    /// <summary>
    /// The installed version. For packaged apps Get-AppxPackage is both faster and more truthful
    /// than the winget table, because it reads the registration rather than a correlation.
    /// </summary>
    public async Task<string> FingerabdruckAsync(ProgrammEintrag eintrag, CancellationToken abbruch)
    {
        if (!string.IsNullOrWhiteSpace(eintrag.AppxName))
        {
            var appx = StoreAktualisierer.AppxAuswerten(
                await Kommandozeile.AusfuehrenAsync("powershell.exe", StoreAktualisierer.AppxBefehl(eintrag.AppxName),
                    TimeSpan.FromMinutes(2), abbruch: abbruch));
            if (!appx.Erfolg) Protokollierung.Schreiben(eintrag.Id, "Fingerabdruck unbekannt: " + appx.Problem);
            return appx.Version;
        }

        if (!File.Exists(Pfade.Winget)) return "";

        var lauf = await ListeAsync(eintrag, null, abbruch);
        var (wert, problem) = ListenFingerabdruck(lauf, eintrag.WingetId ?? "");
        if (problem is not null) Protokollierung.Schreiben(eintrag.Id, "Fingerabdruck unbekannt: " + problem);
        return wert;
    }

    /// <summary>The installed version from a list run -- only from a clean run, never a guess.</summary>
    internal static (string Fingerabdruck, string? Problem) ListenFingerabdruck(BefehlErgebnis lauf, string id)
    {
        if (lauf.Abgelaufen) return ("", "winget list hat das Zeitlimit überschritten.");
        if (lauf.ExitCode == KeinPaketGefunden) return ("", "winget findet das Paket nicht.");
        if (lauf.ExitCode != 0) return ("", "winget list endete mit Code " + Code(lauf.ExitCode) + ".");
        var installiert = TabellenZeile(lauf.Ausgabe, id)?.Installiert ?? "";
        return string.IsNullOrWhiteSpace(installiert) ? ("", "winget list lieferte keine lesbare Version.") : (installiert, null);
    }

    /// <summary>winget: the install finished but needs a reboot to complete (MSI 3010).</summary>
    internal const int NeustartZumAbschluss = unchecked((int)0x8A150109);

    /// <summary>winget: the package or one of its files is in use.</summary>
    internal const int InstallationInBenutzung = unchecked((int)0x8A150101);
    internal const int DateiInBenutzung = unchecked((int)0x8A150103);

    internal static bool BrauchtNeustart(BefehlErgebnis lauf)
        => !lauf.Abgelaufen && !lauf.Abgebrochen && lauf.BeendenProblem is null
           && (lauf.ExitCode is NeustartZumAbschluss or 3010
               || lauf.Ausgabe.Contains("Restart your PC to finish installation", StringComparison.OrdinalIgnoreCase)
               || lauf.Ausgabe.Contains("um die Installation abzuschließen", StringComparison.OrdinalIgnoreCase));

    /// <summary>The manifest denies upgrades through winget; only the publisher's own updater may.</summary>
    internal static bool NurUeberHersteller(BefehlErgebnis lauf)
        => lauf.Ausgabe.Contains("cannot be upgraded using WinGet", StringComparison.OrdinalIgnoreCase)
           || lauf.Ausgabe.Contains("nicht mit WinGet aktualisiert werden", StringComparison.OrdinalIgnoreCase);

    /// <summary>winget's "installed, nothing newer" answer to an upgrade, in code or in words.</summary>
    internal static bool IstNichtsZuTun(BefehlErgebnis lauf)
        => !lauf.Abgelaufen && !lauf.Abgebrochen && lauf.BeendenProblem is null
           && (lauf.ExitCode == KeinAnwendbaresUpgrade
               || lauf.Ausgabe.Contains("No applicable upgrade", StringComparison.OrdinalIgnoreCase)
               || lauf.Ausgabe.Contains("No available upgrade", StringComparison.OrdinalIgnoreCase)
               || lauf.Ausgabe.Contains("Kein anwendbares Upgrade", StringComparison.OrdinalIgnoreCase));

    private static bool MeldetKeinPaket(string ausgabe)
        => ausgabe.Contains("No installed package found", StringComparison.OrdinalIgnoreCase)
           || ausgabe.Contains("Es wurde kein installiertes Paket", StringComparison.OrdinalIgnoreCase);

    /// <summary>winget codes are HRESULTs; hex is what its documentation and issues use.</summary>
    internal static string Code(int exitCode) => exitCode < 0 ? "0x" + exitCode.ToString("X8") : exitCode.ToString();

    /// <summary>
    /// Finds the package row and returns (installed, available).
    ///
    /// Header column offsets are not usable: winget pads columns by display width, and a name
    /// that is itself version-like ("LM Studio 0.4.24+1") pushes the row out of alignment.
    /// Anchoring on the exact package id and reading the fields behind it survives that, and
    /// works regardless of the display language.
    /// </summary>
    internal static (string Installiert, string Verfuegbar)? TabellenZeile(string ausgabe, string id)
    {
        if (string.IsNullOrWhiteSpace(id)) return null;

        // Side-by-side installs (.NET SDK 10.0.204 next to 10.0.402) give one row each. The newest
        // install is the one that matters -- the first row would keep an updated SDK "outdated"
        // and its unchanged version would fail the before/after verification.
        (string Installiert, string Verfuegbar)? beste = null;
        foreach (var zeile in ausgabe.Split('\n'))
        {
            var kandidat = ZeileLesen(zeile, id);
            if (kandidat is null) continue;
            if (beste is null || CliAktualisierer.Vergleiche(OhneOperator(kandidat.Value.Installiert),
                                                             OhneOperator(beste.Value.Installiert)) > 0)
                beste = kandidat;
        }
        return beste;
    }

    private static string OhneOperator(string version) => version.TrimStart('<', '>', ' ');

    private static (string Installiert, string Verfuegbar)? ZeileLesen(string zeile, string id)
    {
        {
            var stelle = zeile.IndexOf(id, StringComparison.OrdinalIgnoreCase);
            if (stelle < 0) return null;
            if (zeile.TrimStart().StartsWith("Name", StringComparison.OrdinalIgnoreCase)) return null;

            // The id must stand alone: "Microsoft.PowerShell" also occurs in "Microsoft.PowerShell.Preview".
            var ende = stelle + id.Length;
            if (ende < zeile.Length && !char.IsWhiteSpace(zeile[ende])) return null;

            var rest = zeile[ende..]
                .Split(' ', StringSplitOptions.RemoveEmptyEntries)
                .ToList();

            // winget writes an unknown exact version as "< 3.14.7" or "> 17.14.41" -- operator and
            // number are one field, not an installed "<" and an available "3.14.7".
            for (var i = 0; i < rest.Count - 1; i++)
            {
                if (rest[i] is "<" or ">")
                {
                    rest[i] = rest[i] + " " + rest[i + 1];
                    rest.RemoveAt(i + 1);
                }
            }

            // Trailing source column ("winget", "msstore", "MSIX\..."): not a version.
            while (rest.Count > 0 && !SiehtNachVersionAus(rest[^1])) rest.RemoveAt(rest.Count - 1);

            return rest.Count switch
            {
                0 => ("", ""),
                1 => (rest[0], ""),
                _ => (rest[0], rest[1])
            };
        }
    }

    /// <summary>A version field always starts with a digit; "winget" and "Unknown" do not.</summary>
    private static bool SiehtNachVersionAus(string feld)
        => feld.Length > 0 && (char.IsDigit(feld[0]) || feld.StartsWith("< ") || feld.StartsWith("> "));
}
