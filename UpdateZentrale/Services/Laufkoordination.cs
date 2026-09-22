namespace UpdateZentrale.Services;

/// <summary>
/// One external operation at a time, app-wide. Per-card busy flags only kept a card from running
/// twice; a check of card A could still run next to an update of card B or next to "check all",
/// which put two winget, git or pwsh runs -- or two installers -- on the machine at once.
///
/// The state is the current owner itself, swapped with Interlocked.CompareExchange: a double
/// click or two nearly simultaneous starts can never both win. Because ownership is a reference
/// and not just a kind, an old token of the same kind can neither pass itself off as the current
/// owner nor release it. A batch run holds ownership for its whole duration and passes its token
/// down to the card operations it drives; those validate it before running.
/// </summary>
public sealed class Laufkoordination
{
    private Laufbesitz? _besitzer;

    /// <summary>Raised after every acquire and release, so the UI can re-evaluate its buttons.</summary>
    public event EventHandler? Geaendert;

    public bool Belegt => Volatile.Read(ref _besitzer) is not null;
    public bool SammelLaeuft => Volatile.Read(ref _besitzer) is { IstSammel: true };

    /// <returns>null when any other operation is running -- the caller must not start.</returns>
    public Laufbesitz? EinzelBeginnen() => Beginnen(istSammel: false);

    /// <returns>null when any operation (single or batch) is running.</returns>
    public Laufbesitz? SammelBeginnen() => Beginnen(istSammel: true);

    /// <summary>
    /// True only for the batch token that currently owns THIS coordination: a token of another
    /// coordination, a released one, or an old one from an earlier batch run is rejected.
    /// </summary>
    public bool IstAktiverSammelbesitz(Laufbesitz? besitz)
        => besitz is { IstSammel: true } && ReferenceEquals(Volatile.Read(ref _besitzer), besitz);

    private Laufbesitz? Beginnen(bool istSammel)
    {
        var neu = new Laufbesitz(this, istSammel);
        if (Interlocked.CompareExchange(ref _besitzer, neu, null) is not null) return null;
        Geaendert?.Invoke(this, EventArgs.Empty);
        return neu;
    }

    internal void Freigeben(Laufbesitz besitz)
    {
        // Clears only if this very token is the owner; a stray release cannot free someone else.
        if (ReferenceEquals(Interlocked.CompareExchange(ref _besitzer, null, besitz), besitz))
            Geaendert?.Invoke(this, EventArgs.Empty);
    }
}

/// <summary>Proof of ownership. Dispose releases exactly once, however often it is called.</summary>
public sealed class Laufbesitz : IDisposable
{
    private readonly Laufkoordination _koordination;
    private int _freigegeben;

    internal Laufbesitz(Laufkoordination koordination, bool istSammel)
    {
        _koordination = koordination;
        IstSammel = istSammel;
    }

    public bool IstSammel { get; }

    public string Art => IstSammel ? "Sammel" : "Einzel";

    public void Dispose()
    {
        if (Interlocked.Exchange(ref _freigegeben, 1) == 0) _koordination.Freigeben(this);
    }
}
