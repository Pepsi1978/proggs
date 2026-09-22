namespace UpdateZentrale.Models;

public enum LaufErgebnis
{
    /// <summary>Fingerprint changed as expected -- the update demonstrably arrived.</summary>
    Erfolgreich,

    /// <summary>Installer staged the update; it becomes active when the program next starts.</summary>
    Ausstehend,

    /// <summary>User said no, or nothing was pending.</summary>
    Abgebrochen,

    /// <summary>The tool itself reported a failure.</summary>
    Fehlgeschlagen,

    /// <summary>
    /// The worst case and the reason this exists: the tool reported success, but nothing actually
    /// changed. Silently trusting the exit code would hide exactly this.
    /// </summary>
    NichtVerifiziert,

    /// <summary>
    /// Nothing needed installing: the real check (before or right after the call) confirmed the
    /// current state. Appended last -- verlauf.jsonl stores the number, older entries keep theirs.
    /// </summary>
    BereitsAktuell
}

/// <summary>
/// One update run, written to verlauf.jsonl so a failure can still be diagnosed days later.
/// </summary>
public sealed class UpdateBericht
{
    public DateTime Zeit { get; set; } = DateTime.Now;
    public string ProgrammId { get; set; } = "";
    public string Name { get; set; } = "";
    public string Art { get; set; } = "";

    public LaufErgebnis Ergebnis { get; set; }
    public string Meldung { get; set; } = "";

    public string VersionVorher { get; set; } = "";
    public string VersionNachher { get; set; } = "";

    /// <summary>Set with <see cref="LaufErgebnis.Ausstehend"/>: the version that should appear later.</summary>
    public string? AusstehendeVersion { get; set; }

    public string Befehl { get; set; } = "";
    public int ExitCode { get; set; }
    public bool Erhoeht { get; set; }
    public string ProtokollDatei { get; set; } = "";

    public string ErgebnisText => Ergebnis switch
    {
        LaufErgebnis.Erfolgreich => "Erfolgreich",
        LaufErgebnis.Ausstehend => "Ausstehend",
        LaufErgebnis.Abgebrochen => "Abgebrochen",
        LaufErgebnis.Fehlgeschlagen => "Fehlgeschlagen",
        LaufErgebnis.NichtVerifiziert => "Nicht verifiziert",
        LaufErgebnis.BereitsAktuell => "Bereits aktuell",
        _ => "Unbekannt"
    };

    /// <summary>Short line for the card: when, how it went, and which versions were involved.</summary>
    public string Kurzfassung
    {
        get
        {
            var versionen = string.IsNullOrWhiteSpace(VersionVorher) && string.IsNullOrWhiteSpace(VersionNachher)
                ? ""
                : VersionVorher == VersionNachher || string.IsNullOrWhiteSpace(VersionNachher)
                    ? "  " + VersionVorher
                    : "  " + VersionVorher + " → " + VersionNachher;

            return "Zuletzt: " + Zeit.ToString("dd.MM.yyyy, HH:mm") + " – " + ErgebnisText + versionen;
        }
    }

    public bool IstFehler => Ergebnis is LaufErgebnis.Fehlgeschlagen or LaufErgebnis.NichtVerifiziert;
}
