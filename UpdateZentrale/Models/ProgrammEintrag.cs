using System.Text.Json.Serialization;

namespace UpdateZentrale.Models;

/// <summary>
/// One catalog entry from programs.json. Adding a program means adding an object here -- never
/// touching code -- which is why every provider-specific field lives on this single type instead
/// of in a subclass hierarchy.
/// </summary>
public sealed class ProgrammEintrag
{
    public string Id { get; set; } = "";
    public string Name { get; set; } = "";
    public string Gruppe { get; set; } = "Weitere";
    public string Beschreibung { get; set; } = "";

    /// <summary>winget | msstore | cli | reposkript</summary>
    public string Art { get; set; } = "winget";

    public string? WingetId { get; set; }

    /// <summary>
    /// The winget source the package comes from. Naming it keeps winget from also asking the
    /// Store source for every query, which took 45 to 95 seconds per program. Empty = all sources.
    /// </summary>
    public string? WingetQuelle { get; set; } = "winget";

    /// <summary>
    /// Selects the installed package by display name instead of by id. winget only matches an id
    /// against packages it can correlate by id; one installed outside winget (PowerShell's MSI)
    /// answers "no installed package" to --id, yet shows up -- with that same id -- under --name.
    /// The row is still anchored on <see cref="WingetId"/>, so a loose name cannot pick a stranger.
    /// </summary>
    public string? WingetName { get; set; }

    /// <summary>
    /// Updates with "winget install --id … --exact" instead of "upgrade". "upgrade" only acts on
    /// packages winget correlates, and for PowerShell's MSI it answers "no installed package" even
    /// with --name. Installing the newer MSI performs the in-place major upgrade itself.
    /// </summary>
    public bool WingetUpdatePerInstall { get; set; }

    /// <summary>
    /// Pins the installer type of the update run ("wix" = MSI). Without it winget picks the
    /// manifest's preferred one -- for PowerShell the MSIX bundle, which landed beside the MSI
    /// install as a second PowerShell instead of replacing it.
    /// </summary>
    public string? WingetInstallerTyp { get; set; }
    public string? AppxName { get; set; }
    public string? PackageFamilyName { get; set; }

    /// <summary>Application id inside the MSIX package; almost always "App".</summary>
    public string? AppxAnwendungsId { get; set; }

    /// <summary>Store product id (e.g. 9PLM9XGG6VKS), used for the silent msstore upgrade.</summary>
    public string? StoreProduktId { get; set; }

    /// <summary>True when this entry is a packaged (MSIX) app rather than a plain exe.</summary>
    [JsonIgnore]
    public bool IstPaketApp => !string.IsNullOrWhiteSpace(PackageFamilyName);

    /// <summary>
    /// Publisher hash from the package family name. Every install folder of that package under
    /// WindowsApps carries it, which makes it a reliable way to tell a packaged app's processes
    /// apart from a same-named CLI.
    /// </summary>
    [JsonIgnore]
    public string? PaketKennung
    {
        get
        {
            if (string.IsNullOrWhiteSpace(PackageFamilyName)) return null;
            var teil = PackageFamilyName.Split('_').LastOrDefault();
            return string.IsNullOrWhiteSpace(teil) ? null : teil;
        }
    }

    public string ExePfad { get; set; } = "";

    /// <summary>
    /// Weitere Orte, an denen dasselbe Programm liegen kann. Dieselbe programs.json laeuft auf
    /// mehreren Rechnern, und dort unterscheiden sich die Installationswege: LM Studio liegt mal
    /// unter %LOCALAPPDATA%, mal unter "Program Files"; die CLIs kommen mal vom nativen Installer,
    /// mal als npm-Shim. Statt den Katalog je Rechner zu gabeln, gewinnt der erste Pfad, den es
    /// wirklich gibt.
    /// </summary>
    public List<string> ExePfadAlternativen { get; set; } = new();

    /// <summary>
    /// Der Katalog-Pfad, der auf diesem Rechner existiert -- unaufgeloest, damit die Aufrufer
    /// weiterhin selbst <see cref="Services.Pfade.Aufloesen"/> anwenden. Gibt es keinen, bleibt es
    /// bei <see cref="ExePfad"/>, damit die Fehlermeldung den erwarteten Ort nennt.
    /// </summary>
    [JsonIgnore]
    public string ExePfadWirksam
    {
        get
        {
            if (ExePfadAlternativen.Count == 0) return ExePfad;
            foreach (var kandidat in new[] { ExePfad }.Concat(ExePfadAlternativen))
            {
                if (string.IsNullOrWhiteSpace(kandidat)) continue;
                var aufgeloest = Services.Pfade.Aufloesen(kandidat);
                if (!string.IsNullOrEmpty(aufgeloest) && System.IO.File.Exists(aufgeloest)) return kandidat;
            }
            return ExePfad;
        }
    }

    /// <summary>
    /// Blendet den Eintrag aus, wenn auf diesem Rechner keiner der Pfadkandidaten existiert. Für
    /// Programme, die nur auf einem Teil der Rechner benutzt werden: derselbe Katalog bleibt für
    /// alle gültig, ohne auf einem Gerät eine Karte "Nicht installiert" stehen zu lassen.
    /// </summary>
    public bool AusblendenWennFehlt { get; set; }

    /// <summary>
    /// Presence marker for entries without a launchable exe (the OpenCode fix build: its exe sits
    /// in a versioned folder, current.json points to it). Counts for <see cref="AufDiesemRechnerVorhanden"/>
    /// only -- never started, never flagged as admin.
    /// </summary>
    public string? VorhandenPfad { get; set; }

    [JsonIgnore]
    public bool AufDiesemRechnerVorhanden
    {
        get
        {
            foreach (var kandidat in new[] { ExePfad, VorhandenPfad ?? "" }.Concat(ExePfadAlternativen))
            {
                if (string.IsNullOrWhiteSpace(kandidat)) continue;
                var aufgeloest = Services.Pfade.Aufloesen(kandidat);
                if (!string.IsNullOrEmpty(aufgeloest) && System.IO.File.Exists(aufgeloest)) return true;
            }
            return false;
        }
    }

    public string? VersionsArgumente { get; set; }
    public string? UpdateArgumente { get; set; }
    public string? PruefArgumente { get; set; }

    /// <summary>
    /// Exit codes of the check run that still mean "the answer is valid". rustup check ends with
    /// 100 exactly when updates are pending; without this, every pending update read as failure.
    /// </summary>
    public List<int> PruefExitCodes { get; set; } = new() { 0 };

    /// <summary>Only lines after the first line containing this marker count as planned updates.</summary>
    public string? PruefAbschnitt { get; set; }

    /// <summary>
    /// Regex for a planned-update line, replacing the default arrow test -- for tools that list
    /// updates as a table (sdkmanager: "emulator | 36.4.10 | 37.2.12").
    /// </summary>
    public string? PruefZeilenMuster { get; set; }

    /// <summary>
    /// A different program for the update run than for the version query: the npm packages report
    /// their version themselves (firebase.cmd --version) but are updated through npm.cmd.
    /// </summary>
    public string? UpdateExePfad { get; set; }

    public string? NpmPaket { get; set; }
    /// <summary>Plain-text URL whose body names the newest version (e.g. Kimi Code's CDN "latest").</summary>
    public string? VersionsUrl { get; set; }
    public string? StartArgumente { get; set; }

    /// <summary>Check output lists updates as "alt -> neu"; counting arrows tells us how many.</summary>
    public bool PfeilZaehlen { get; set; }

    public int ZeitlimitMinuten { get; set; } = 20;

    public string? RepoOrdner { get; set; }
    public string? Skript { get; set; }
    public string? SkriptArgumente { get; set; }
    public string? StatusPraefix { get; set; }
    public string? ProjektDatei { get; set; }
    public int DialogWartezeitSekunden { get; set; } = 300;

    public List<string> Prozesse { get; set; } = new();
    public List<string> HelferProzesse { get; set; } = new();
    public bool BeendenVorUpdate { get; set; }
    public bool NeuStartenNachUpdate { get; set; }

    public string? Hinweis { get; set; }

    /// <summary>Text of the "pending" band instead of the default "restart the program" wording.</summary>
    public string? UebernahmeText { get; set; }

    /// <summary>
    /// A jump to a new major version (TypeScript 6 → 7, FFmpeg 8 → 9) is shown, but "install all"
    /// leaves it out; it runs only on a deliberate click on the card.
    /// </summary>
    public bool HauptversionNurPerKlick { get; set; }

    /// <summary>
    /// Expected update duration in seconds until real runs were measured (verlauf.jsonl). Without
    /// it a quarter of the time limit stands in. Only orders the batch and the duration sort.
    /// </summary>
    public int? DauerSchaetzungSekunden { get; set; }

    /// <summary>
    /// Always the last update of "install all", whatever its duration: the App Installer replaces
    /// winget itself, and every winget call after it in the same batch would hit a half-swapped tool.
    /// </summary>
    public bool Zuletzt { get; set; }

    /// <summary>
    /// May run in the parallel lane of "install all": a self-updater that touches only its own
    /// folder and shares no lock, tree or runtime with another entry. Explicit on purpose -- guessed
    /// from the command line it put the Android SDK (needs the JDK being replaced) and the LM Studio
    /// runtimes (LM Studio itself is replaced beside them) into the parallel lane.
    /// </summary>
    public bool Parallel { get; set; }

    /// <summary>
    /// Ids that must be updated before this one when both are in the same run: sdkmanager needs
    /// the new JDK, the LM Studio runtimes belong to the new LM Studio build, the npm tools to
    /// the new Node. Only orders a run; it never pulls an update into one.
    /// </summary>
    public List<string> Nach { get; set; } = new();

    /// <summary>
    /// Regex whose group 1 is the installed version in the output of <see cref="VersionsArgumente"/>.
    /// Without it the first "x.y.z" wins -- for "gradle --version" that is Kotlin's version as soon
    /// as Gradle itself has only two parts ("9.7").
    /// </summary>
    public string? VersionsMuster { get; set; }

    /// <summary>Same for the body of <see cref="VersionsUrl"/> (Gradle's JSON names "version": "9.7").</summary>
    public string? VersionsUrlMuster { get; set; }

    /// <summary>
    /// Why this update can end a running CLI session (Claude Code, Codex, a terminal window).
    /// Set = the card is marked, and "install all" holds it back for a second step that runs
    /// only once the CLIs are closed.
    /// </summary>
    public string? CliRisiko { get; set; }

    [JsonIgnore]
    public bool KannCliBeenden => !string.IsNullOrWhiteSpace(CliRisiko);

    /// <summary>
    /// Vendor download for a winget package whose manifest ships the wrong installer (VS Code
    /// 1.142.0 pointed at the 1.141.2 setup: "Successfully installed", nothing changed, the same
    /// update offered forever). "{version}" is replaced with the version winget offers. Used only
    /// when the downloaded installer's version contradicts the offered one.
    /// </summary>
    public string? HerstellerInstallerUrl { get; set; }

    /// <summary>Silent switches for <see cref="HerstellerInstallerUrl"/>.</summary>
    public string? HerstellerInstallerArgumente { get; set; }

    public string Akzent { get; set; } = "#7C5CFF";

    /// <summary>Default for the admin toggle; the user override in settings.json wins.</summary>
    public bool AdminStandard { get; set; }

    [JsonIgnore]
    public IEnumerable<string> AlleProzesse => Prozesse.Concat(HelferProzesse);
}

public sealed class ProgrammKatalog
{
    public int Version { get; set; } = 1;
    public List<ProgrammEintrag> Programme { get; set; } = new();
}
