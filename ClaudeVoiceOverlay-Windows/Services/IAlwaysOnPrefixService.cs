using System.Threading;
using System.Threading.Tasks;

namespace ClaudeVoiceOverlay.Services;

/// <summary>
/// Assembles always-on prefix and suffix strings from every AlwaysOn
/// prompt in the shared PromptBoard database. Returns empty when no
/// prompts match. The Pre/Post split (#1820) means each prompt can be
/// front-only, back-only, or both.
/// </summary>
public interface IAlwaysOnPrefixService
{
    /// <summary>Back-compat alias for BuildPreAsync — same signature
    /// and semantics as before the Pre/Post split.</summary>
    Task<string> BuildAsync(CancellationToken ct = default);

    /// <summary>Prompts that should appear BEFORE the dictated text.</summary>
    Task<string> BuildPreAsync(CancellationToken ct = default);

    /// <summary>Prompts that should appear AFTER the dictated text.</summary>
    Task<string> BuildPostAsync(CancellationToken ct = default);

    /// <summary>
    /// Liefert Pre und Post in einem Aufruf, mit nur EINEM DB-Roundtrip statt
    /// zweien. Der Default ruft BuildPreAsync + BuildPostAsync sequentiell —
    /// das behaelt das alte Verhalten bei (zwei Roundtrips), bricht aber keinen
    /// bestehenden Implementer. Die Standard-Implementierung
    /// AlwaysOnPrefixService ueberschreibt die Default-Variante und nutzt einen
    /// einzigen DB-Roundtrip; alle Aufrufer im Voice-Submit-Pfad sollten diese
    /// Methode bevorzugen.
    /// </summary>
    async Task<(string Pre, string Post)> BuildBothAsync(CancellationToken ct = default)
    {
        string pre  = await BuildPreAsync(ct).ConfigureAwait(false);
        string post = await BuildPostAsync(ct).ConfigureAwait(false);
        return (pre, post);
    }
}
