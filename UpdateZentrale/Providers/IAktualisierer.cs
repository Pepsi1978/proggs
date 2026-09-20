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
}
