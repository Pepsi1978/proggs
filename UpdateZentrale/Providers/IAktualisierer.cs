using UpdateZentrale.Models;

namespace UpdateZentrale.Providers;

/// <summary>
/// One implementation per update mechanism. A new program in programs.json only needs an existing
/// "art" value -- code changes are reserved for genuinely new mechanisms.
/// </summary>
public interface IAktualisierer
{
    /// <summary>Value of the catalog field "art" this implementation serves.</summary>
    string Art { get; }

    Task<PruefErgebnis> PruefenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch);

    Task<PruefErgebnis> AktualisierenAsync(ProgrammEintrag eintrag, IProgress<string> protokoll, CancellationToken abbruch);

    /// <summary>
    /// Whatever must change when an update really arrived -- read before and after a run and then
    /// compared. An exit code of 0 is only the installer's own claim; this is the evidence.
    ///
    /// What that is depends on the mechanism: a version number where one exists, the write time of
    /// the built exe for the repo scripts, the pending list for the LM Studio runtimes. Returning
    /// an empty string means "cannot tell", and the run is then reported as unverified rather than
    /// as success.
    /// </summary>
    Task<string> FingerabdruckAsync(ProgrammEintrag eintrag, CancellationToken abbruch);
}
