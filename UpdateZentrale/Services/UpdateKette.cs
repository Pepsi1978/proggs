using System.Text.RegularExpressions;
using UpdateZentrale.Models;
using UpdateZentrale.Providers;

namespace UpdateZentrale.Services;

/// <summary>The final verdict of one user action, plus what the card should show.</summary>
public sealed record KettenUrteil(
    LaufErgebnis Ergebnis,
    string Meldung,
    string FingerabdruckVorher,
    string FingerabdruckNachher,
    PruefErgebnis FuerKarte,
    int UpdateAufrufe,
    bool UpdateLiefDurch,
    string? AusstehendeVersion = null,
    IReadOnlyList<string>? Uebergaenge = null);

/// <summary>
/// Drives one "Aktualisieren" click to its REAL end state instead of trusting one exit code.
///
/// The old flow wrote "Erfolgreich" as soon as any fingerprint changed and only then asked the
/// provider -- whose "update still available" merely nudged the card. A partial update (one of
/// two runtimes, a staged first step) was logged as success and offered again on the next check.
///
/// Rules:
/// <list type="number">
/// <item>Before anything: fingerprint and a real provider check. Already current → no update call.</item>
/// <item>A pass = update call, bounded settle (fingerprint polled with short injected pauses, so
/// an installer child that finishes after the parent returned is waited for), then a real check.</item>
/// <item>Success only if the call did not fail, the fingerprint provably moved (both readable,
/// different) and the check says current.</item>
/// <item>Still offered: a next pass ("Folgeupdate 2 von N", hard limit N) only for a DIFFERENT,
/// readable follow-up offer together with proven progress. The same offer again, or an unreadable
/// one, ends the chain after this pass -- the same update command is never repeated.</item>
/// <item>No progress → stop at once, the same command is not repeated. Failure, cancel and a
/// staged update (ErstNachNeustart) end the chain immediately. A check that fails or is unknown
/// is never success.</item>
/// </list>
/// The component writes no log file; the caller writes exactly one header and one footer.
/// </summary>
public sealed class UpdateKette
{
    private readonly IAktualisierer _aktualisierer;
    private readonly ProgrammEintrag _eintrag;
    private readonly IProgress<string> _protokoll;
    private readonly Action<string> _status;
    private readonly Func<TimeSpan, CancellationToken, Task> _warten;

    public int MaxDurchlaeufe { get; init; } = 3;
    public int MaxNachpruefungen { get; init; } = 4;
    public TimeSpan Pause { get; init; } = TimeSpan.FromSeconds(3);

    public UpdateKette(IAktualisierer aktualisierer, ProgrammEintrag eintrag, IProgress<string> protokoll,
                       Action<string>? status = null, Func<TimeSpan, CancellationToken, Task>? warten = null)
    {
        _aktualisierer = aktualisierer;
        _eintrag = eintrag;
        _protokoll = protokoll;
        _status = status ?? (_ => { });
        _warten = warten ?? ((dauer, abbruch) => Task.Delay(dauer, abbruch));
    }

    /// <summary>The pre-check (step 0). Split off so the caller can ask its kill question in between.</summary>
    public async Task<(string Fingerabdruck, PruefErgebnis Pruefung)> VorpruefenAsync(CancellationToken abbruch)
    {
        _status("Ermittelt den Stand …");
        var fingerabdruck = await _aktualisierer.FingerabdruckAsync(_eintrag, abbruch);
        var pruefung = await _aktualisierer.PruefenAsync(_eintrag, _protokoll, abbruch);
        _protokoll.Report("Vorprüfung: " + Beschreibe(pruefung) + " | Stand " + Anzeigen(fingerabdruck));
        Diagnose.Ereignis(Schwere.Info, "kette", "kette.vorpruefung", "Vorprüfung: " + Beschreibe(pruefung), "vorpruefung",
            new Dictionary<string, object?> { ["stand"] = fingerabdruck, ["zustand"] = pruefung.Zustand.ToString(), ["angebot"] = pruefung.VerfuegbareVersion });
        return (fingerabdruck, pruefung);
    }

    /// <summary>A current pre-check ends the action without any update call.</summary>
    public static KettenUrteil? BereitsAktuell(string fingerabdruck, PruefErgebnis vor)
        => vor.Zustand == UpdateZustand.Aktuell
            ? new KettenUrteil(LaufErgebnis.BereitsAktuell,
                "Bereits auf dem neuesten Stand – die Anzeige war veraltet, es wurde nichts installiert.",
                fingerabdruck, fingerabdruck, vor with { Meldung = "Auf dem neuesten Stand." }, 0, false)
            : null;

    public async Task<KettenUrteil> AusfuehrenAsync(string fingerabdruckStart, PruefErgebnis vor, CancellationToken abbruch)
    {
        var uebergaenge = new List<string>();
        var angebot = vor.VerfuegbareVersion;
        var standVor = fingerabdruckStart;
        var aufrufe = 0;
        var liefDurch = false;
        var letztePruefung = vor;
        var standNach = fingerabdruckStart;

        for (var durchlauf = 1; durchlauf <= MaxDurchlaeufe; durchlauf++)
        {
            if (durchlauf > 1)
            {
                var hinweis = "Folgeupdate " + durchlauf + " von " + MaxDurchlaeufe
                              + (string.Equals(_eintrag.Art, "reposkript", StringComparison.OrdinalIgnoreCase)
                                  ? " – das Skript fragt erneut nach Ja/Nein." : ".");
                _protokoll.Report(hinweis);
                _status(hinweis);
            }
            else
            {
                _status("Aktualisiert …");
            }

            aufrufe++;
            Diagnose.Ereignis(Schwere.Info, "kette", "kette.durchlauf", "Durchlauf " + durchlauf + " von " + MaxDurchlaeufe, "durchlauf",
                new Dictionary<string, object?> { ["durchlauf"] = durchlauf, ["angebot"] = angebot, ["standVor"] = standVor });
            var ergebnis = await _aktualisierer.AktualisierenAsync(_eintrag, _protokoll, abbruch);

            if (ergebnis.Zustand == UpdateZustand.Abgebrochen)
                return Ende(LaufErgebnis.Abgebrochen, Text(ergebnis.Meldung, "Abgebrochen."), ergebnis with { Zustand = UpdateZustand.Abgebrochen });
            if (ergebnis.Zustand is UpdateZustand.Fehler or UpdateZustand.NichtInstalliert)
                return Ende(LaufErgebnis.Fehlgeschlagen, Text(ergebnis.Meldung, "Das Update ist fehlgeschlagen."),
                    ergebnis with { Zustand = UpdateZustand.Fehler });

            liefDurch = true;

            if (ergebnis.ErstNachNeustart)
            {
                // Staged: becomes real on the program's next start. Re-checking now would show the
                // old version, and repeating would reinstall the same package.
                return Ende(LaufErgebnis.Ausstehend, Text(ergebnis.Meldung, "Installiert – wird beim nächsten Start übernommen."),
                    ergebnis with { Zustand = UpdateZustand.Fertig }, string.IsNullOrWhiteSpace(angebot) ? null : angebot);
            }

            // Settle: wait, bounded, for the real state -- an installer child may still be working.
            _status("Prüft das Ergebnis …");
            PruefErgebnis? nach = null;
            standNach = "";
            for (var versuch = 1; versuch <= MaxNachpruefungen; versuch++)
            {
                if (versuch > 1) await _warten(Pause, abbruch);
                standNach = await _aktualisierer.FingerabdruckAsync(_eintrag, abbruch);
                var bewegt = Fortschritt(standVor, standNach);
                Diagnose.Ereignis(Schwere.Debug, "kette", "kette.nachpruefung", "Nachprüfung " + versuch + " von " + MaxNachpruefungen
                    + (bewegt ? ": Stand bewegt" : ": Stand unverändert"), "nachpruefung",
                    new Dictionary<string, object?> { ["versuch"] = versuch, ["stand"] = standNach, ["bewegt"] = bewegt });
                var letzterVersuch = versuch == MaxNachpruefungen;

                if (!bewegt && !letzterVersuch && ergebnis.Zustand != UpdateZustand.Aktuell) continue;   // not arrived yet

                nach = await _aktualisierer.PruefenAsync(_eintrag, _protokoll, abbruch);
                if (nach.Zustand == UpdateZustand.Aktuell) break;
                if (nach.Zustand == UpdateZustand.UpdateVerfuegbar && bewegt
                    && nach.VerfuegbareVersion == angebot && !letzterVersuch) continue;   // provider cache may lag once
                break;
            }
            letztePruefung = nach!;

            var uebergang = "Durchlauf " + durchlauf + ": " + Anzeigen(standVor) + " → " + Anzeigen(standNach)
                            + " | Prüfung: " + Beschreibe(letztePruefung);
            uebergaenge.Add(uebergang);
            _protokoll.Report(uebergang);
            Diagnose.Ereignis(Schwere.Info, "kette", "kette.uebergang", uebergang, "uebergang",
                new Dictionary<string, object?> { ["durchlauf"] = durchlauf, ["zustand"] = letztePruefung.Zustand.ToString(), ["angebot"] = letztePruefung.VerfuegbareVersion });

            if (string.IsNullOrWhiteSpace(standNach))
                return Ende(LaufErgebnis.NichtVerifiziert,
                    "Der Stand ließ sich nach dem Update nicht ermitteln – es ist nicht überprüfbar.", KarteAus(letztePruefung));

            if (letztePruefung.Zustand is UpdateZustand.Fehler or UpdateZustand.Unbekannt or UpdateZustand.NichtInstalliert)
                return Ende(LaufErgebnis.NichtVerifiziert,
                    "Die Nachprüfung ergab keinen belastbaren Stand (" + Beschreibe(letztePruefung) + ") – nicht als Erfolg gewertet.",
                    KarteAus(letztePruefung));

            var fortschritt = Fortschritt(standVor, standNach);

            if (letztePruefung.Zustand == UpdateZustand.Aktuell)
            {
                if (fortschritt || Fortschritt(fingerabdruckStart, standNach))
                    return Ende(LaufErgebnis.Erfolgreich,
                        "Verifiziert: " + Anzeigen(fingerabdruckStart) + " → " + Anzeigen(standNach)
                        + (durchlauf > 1 ? " (in " + durchlauf + " Durchläufen)" : ""),
                        letztePruefung with { Meldung = "Auf dem neuesten Stand." });

                if (ergebnis.Zustand == UpdateZustand.Aktuell && durchlauf == 1)
                    return Ende(LaufErgebnis.BereitsAktuell,
                        Text(ergebnis.Meldung, "War bereits aktuell.") + " Die Nachprüfung bestätigt den aktuellen Stand.",
                        letztePruefung with { Meldung = "Auf dem neuesten Stand." });

                return Ende(LaufErgebnis.NichtVerifiziert,
                    "Die Prüfung meldet jetzt „aktuell“, der Stand hat sich aber nicht verändert ("
                    + Anzeigen(standNach) + ") – nicht als Erfolg gewertet.", KarteAus(letztePruefung));
            }

            // Still offered. A follow-up pass needs a DIFFERENT concrete offer: the same target again
            // means this update did not take, whatever else moved (a rebuilt exe of the same version
            // changes only a timestamp in the repo fingerprint). An empty offer proves nothing either.
            var folgeangebot = letztePruefung.VerfuegbareVersion;
            if (string.IsNullOrWhiteSpace(folgeangebot))
                return Ende(LaufErgebnis.NichtVerifiziert,
                    "Nach dem Update ist weiter ein Update offen, das Angebot ist aber nicht lesbar (" + Beschreibe(letztePruefung)
                    + ") – kein erneuter Versuch.", KarteAus(letztePruefung));

            if (folgeangebot == angebot)
                return Ende(LaufErgebnis.NichtVerifiziert,
                    "Dasselbe Update (" + folgeangebot + ") wird weiter angeboten"
                    + (fortschritt ? ", obwohl sich der Stand geändert hat (" + Anzeigen(standVor) + " → " + Anzeigen(standNach) + ")" : ", der Stand ist unverändert")
                    + " – kein erneuter Versuch.", KarteAus(letztePruefung));

            if (!fortschritt)
                return Ende(LaufErgebnis.NichtVerifiziert,
                    "Der Stand ist unverändert (" + Anzeigen(standNach) + "), das Angebot wechselte auf " + folgeangebot
                    + " – kein erneuter Versuch.", KarteAus(letztePruefung));

            if (durchlauf == MaxDurchlaeufe)
                return Ende(LaufErgebnis.NichtVerifiziert,
                    "Nach " + MaxDurchlaeufe + " Durchläufen ist weiter ein Update offen (" + Beschreibe(letztePruefung)
                    + "). Übergänge: " + string.Join(" / ", uebergaenge), KarteAus(letztePruefung));

            standVor = standNach;
            angebot = letztePruefung.VerfuegbareVersion;
        }

        // Unreachable by construction; kept fail-closed.
        return Ende(LaufErgebnis.NichtVerifiziert, "Die Update-Kette endete ohne Urteil.", KarteAus(letztePruefung));

        KettenUrteil Ende(LaufErgebnis urteil, string meldung, PruefErgebnis karte, string? ausstehend = null)
        {
            Diagnose.Ereignis(urteil is LaufErgebnis.Fehlgeschlagen or LaufErgebnis.NichtVerifiziert ? Schwere.Fehler
                    : urteil == LaufErgebnis.Abgebrochen ? Schwere.Warnung : Schwere.Info,
                "kette", "kette.urteil", meldung, "urteil",
                new Dictionary<string, object?> { ["ergebnis"] = urteil.ToString(), ["aufrufe"] = aufrufe, ["standVorher"] = fingerabdruckStart, ["standNachher"] = standNach });
            return new(urteil, meldung, fingerabdruckStart, standNach,
                karte with { Meldung = meldung }, aufrufe, liefDurch, ausstehend, uebergaenge);
        }
    }

    /// <summary>Card state for a non-success: the provider's truth, shown as an error.</summary>
    private static PruefErgebnis KarteAus(PruefErgebnis pruefung) => pruefung with { Zustand = UpdateZustand.Fehler };

    /// <summary>Proven movement: both readings exist and differ.</summary>
    internal static bool Fortschritt(string vorher, string nachher)
        => !string.IsNullOrWhiteSpace(vorher) && !string.IsNullOrWhiteSpace(nachher) && vorher != nachher;

    private static readonly Regex VersionsTeil = new(@"\d+(\.\d+)+", RegexOptions.Compiled);

    /// <summary>
    /// A staged update, re-checked later. Confirmed only when the expected target is reached (if
    /// known) AND the provider says current; any other fingerprint change is not enough.
    /// </summary>
    /// <returns>null: keep waiting. Otherwise the verdict to record.</returns>
    public static (LaufErgebnis Ergebnis, string Meldung)? StagedUrteil(UpdateBericht offen, string jetzt,
                                                                       PruefErgebnis pruefung, DateTime zeitpunkt)
    {
        var zielErreicht = true;
        if (!string.IsNullOrWhiteSpace(offen.AusstehendeVersion))
        {
            var ziel = VersionsTeil.Match(offen.AusstehendeVersion).Value;
            var ist = VersionsTeil.Match(jetzt ?? "").Value;
            zielErreicht = ziel.Length > 0 && ist.Length > 0 && CliAktualisierer.Vergleiche(ist, ziel) >= 0;
        }

        if (pruefung.Zustand == UpdateZustand.Aktuell && zielErreicht && Fortschritt(offen.VersionNachher, jetzt ?? ""))
            return (LaufErgebnis.Erfolgreich, "Nachträglich bestätigt: das Update vom " + offen.Zeit.ToString("dd.MM.yyyy")
                                              + " ist inzwischen aktiv (" + jetzt + ").");

        if ((zeitpunkt - offen.Zeit).TotalDays >= 7)
            return (LaufErgebnis.NichtVerifiziert, "Das Update vom " + offen.Zeit.ToString("dd.MM.yyyy")
                                                   + " ist bis heute nicht nachweislich übernommen ("
                                                   + (string.IsNullOrWhiteSpace(jetzt) ? "Stand unbekannt" : "Stand " + jetzt)
                                                   + ", Prüfung: " + Beschreibe(pruefung) + ").");
        return null;
    }

    private static string Text(string meldung, string ersatz) => string.IsNullOrWhiteSpace(meldung) ? ersatz : meldung;

    private static string Anzeigen(string fingerabdruck) => string.IsNullOrWhiteSpace(fingerabdruck) ? "(unbekannt)" : fingerabdruck;

    private static string Beschreibe(PruefErgebnis p) => p.Zustand switch
    {
        UpdateZustand.Aktuell => "aktuell",
        UpdateZustand.UpdateVerfuegbar => "Update offen" + (string.IsNullOrWhiteSpace(p.VerfuegbareVersion) ? "" : " (" + p.VerfuegbareVersion + ")"),
        UpdateZustand.Fehler => "Fehler" + (string.IsNullOrWhiteSpace(p.Meldung) ? "" : ": " + p.Meldung),
        UpdateZustand.Unbekannt => "unbekannt" + (string.IsNullOrWhiteSpace(p.Meldung) ? "" : ": " + p.Meldung),
        UpdateZustand.NichtInstalliert => "nicht installiert",
        _ => p.Zustand.ToString()
    };
}
