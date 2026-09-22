using System.Text;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using UpdateZentrale.Models;
using UpdateZentrale.Providers;
using UpdateZentrale.Services;

namespace UpdateZentrale.ViewModels;

/// <summary>One card in the list: the catalog entry plus everything the UI shows about it.</summary>
public sealed partial class ProgrammViewModel : ObservableObject
{
    private readonly IAktualisierer? _aktualisierer;
    private readonly Einstellungen _einstellungen;
    private readonly StringBuilder _protokoll = new();
    private readonly Laufkoordination _koordination;

    public ProgrammViewModel(ProgrammEintrag eintrag, IAktualisierer? aktualisierer, Einstellungen einstellungen,
                             Laufkoordination koordination)
    {
        Eintrag = eintrag;
        _aktualisierer = aktualisierer;
        _einstellungen = einstellungen;
        _koordination = koordination;
        _koordination.Geaendert += AufKoordinationGeaendert;

        var gespeichert = einstellungen.Fuer(eintrag.Id);
        // The registry is the truth for "runs as admin"; settings.json only keeps the wish for
        // programs whose exe was not found when the switch was flipped.
        _alsAdministrator = Systemdienst.AdminModusLesen(eintrag)
                            || (gespeichert.AlsAdministrator && string.IsNullOrWhiteSpace(eintrag.ExePfad));

        if (_aktualisierer is null)
        {
            Zustand = UpdateZustand.Fehler;
            StatusText = "Unbekannte Update-Art: " + eintrag.Art;
        }

        ZustandAktualisieren();
    }

    public ProgrammEintrag Eintrag { get; }

    public string Name => Eintrag.Name;
    public string Gruppe => Eintrag.Gruppe;
    public string Beschreibung => Eintrag.Beschreibung;
    public string Akzent => Eintrag.Akzent;

    /// <summary>Up to two initials for the tile; brackets and symbols are skipped.</summary>
    public string Kuerzel => new string(Eintrag.Name
        .Split(new[] { ' ', '-', '/' }, StringSplitOptions.RemoveEmptyEntries)
        .Where(t => char.IsLetterOrDigit(t[0]))
        .Take(2)
        .Select(t => char.ToUpperInvariant(t[0]))
        .ToArray());

    public string ArtText => Eintrag.Art switch
    {
        "winget" => "winget",
        "store" => "Microsoft Store",
        "msstore" => "Microsoft Store",
        "cli" => "Selbst-Update",
        "reposkript" => "Eigenes Skript",
        _ => Eintrag.Art
    };

    public string? Hinweis => Eintrag.Hinweis;
    public bool HatHinweis => !string.IsNullOrWhiteSpace(Eintrag.Hinweis);
    /// <summary>A packaged app is launched through the apps folder, so it needs no exe path.</summary>
    public bool KannStarten => !string.IsNullOrWhiteSpace(Eintrag.ExePfad) || Eintrag.IstPaketApp;

    /// <summary>
    /// Windows refuses to run packaged (MSIX/Store) apps elevated at all -- there is no
    /// compatibility flag for them. Showing a switch that cannot work would be a lie, so those
    /// cards get an explanation instead.
    /// </summary>
    public bool AdminSchalterMoeglich => !Eintrag.IstPaketApp && !string.IsNullOrWhiteSpace(Eintrag.ExePfad);

    public bool AdminNichtMoeglich => Eintrag.IstPaketApp;

    [ObservableProperty] private UpdateZustand _zustand = UpdateZustand.Unbekannt;
    [ObservableProperty] private string _statusText = "Noch nicht geprüft";
    [ObservableProperty] private string _installierteVersion = "";
    [ObservableProperty] private string _verfuegbareVersion = "";
    [ObservableProperty] private bool _istBeschaeftigt;
    [ObservableProperty] private bool _laeuft;
    [ObservableProperty] private bool _imAutostart;
    [ObservableProperty] private bool _adminWarnung;

    private bool _alsAdministrator;

    /// <summary>
    /// Writes the AppCompatFlags layer, so Windows itself elevates the program on every start --
    /// the same switch as "Run this program as an administrator" in the file properties.
    /// </summary>
    public bool AlsAdministrator
    {
        get => _alsAdministrator;
        set
        {
            if (_alsAdministrator == value) return;
            _alsAdministrator = value;

            var erfolg = Systemdienst.AdminModusSetzen(Eintrag, value);
            _einstellungen.Fuer(Eintrag.Id).AlsAdministrator = value;
            _einstellungen.Speichern();

            if (!erfolg && value)
            {
                _alsAdministrator = false;
                Melde("Der Administratormodus ließ sich nicht setzen – die Programmdatei wurde nicht gefunden.");
            }

            ZustandAktualisieren();
            OnPropertyChanged();
        }
    }

    public string Protokoll => _protokoll.ToString();

    public bool HatUpdate => Zustand == UpdateZustand.UpdateVerfuegbar;

    /// <summary>
    /// The update button only invites action when there really is a newer version. Once a check
    /// found nothing, the button says "Aktuell" and is disabled instead of suggesting work.
    /// </summary>
    public bool AktionMoeglich => _aktualisierer is not null
                                  && !IstBeschaeftigt
                                  && !_koordination.Belegt
                                  && Zustand != UpdateZustand.Aktuell
                                  && !UebernahmeOffen;

    public const string BelegtText = "Es läuft bereits ein anderer Vorgang – erst danach ist das möglich.";

    /// <summary>The buttons follow the app-wide lock at once, from whichever thread released it.</summary>
    private void AufKoordinationGeaendert(object? sender, EventArgs e)
    {
        void Melden()
        {
            OnPropertyChanged(nameof(AktionMoeglich));
            OnPropertyChanged(nameof(KannPruefen));
        }

        var dispatcher = System.Windows.Application.Current?.Dispatcher;
        if (dispatcher is null || dispatcher.CheckAccess()) Melden();
        else dispatcher.BeginInvoke(Melden);
    }

    /// <summary>Called when the card is thrown away (catalog reload), so it stops listening.</summary>
    public void Abmelden() => _koordination.Geaendert -= AufKoordinationGeaendert;

    /// <summary>
    /// A batch run passes its ownership down; a click has to acquire its own. Losing that race
    /// means another operation is running -- then nothing starts.
    /// </summary>
    private bool Erwerben(Laufbesitz? sammel, out Laufbesitz? eigen)
    {
        eigen = null;
        if (sammel is not null)
        {
            // A batch token is only honoured while it really owns this coordination -- a
            // foreign, released or stale token must not slip a run past the lock.
            if (_koordination.IstAktiverSammelbesitz(sammel)) return true;
            StatusText = BelegtText;
            return false;
        }

        eigen = _koordination.EinzelBeginnen();
        if (eigen is not null) return true;

        StatusText = BelegtText;
        return false;
    }

    public string AktionsText => UebernahmeOffen
        ? "Neustart nötig"
        : Zustand switch
        {
            UpdateZustand.Aktuell => "Aktuell",
            UpdateZustand.UpdateVerfuegbar => "Aktualisieren",
            UpdateZustand.Pruefe => "Prüft …",
            UpdateZustand.NichtInstalliert => "Nicht installiert",
            _ => "Aktualisieren"
        };

    /// <summary>Only a real update gets the accent button; everything else stays calm.</summary>
    public bool AktionBetont => Zustand == UpdateZustand.UpdateVerfuegbar && !UebernahmeOffen;

    public bool KannPruefen => _aktualisierer is not null && !IstBeschaeftigt && !_koordination.Belegt;

    public string VersionsText => string.IsNullOrWhiteSpace(InstallierteVersion)
        ? "–"
        : string.IsNullOrWhiteSpace(VerfuegbareVersion) || VerfuegbareVersion == InstallierteVersion
            ? InstallierteVersion
            : InstallierteVersion + "   →   " + VerfuegbareVersion;

    public event EventHandler<string>? Meldung;

    [ObservableProperty] private UpdateBericht? _letzterBericht;

    public bool HatBericht => LetzterBericht is not null;

    /// <summary>
    /// Das rote Band. Es verschwindet erst, wenn es weggeklickt wurde -- und nur für genau diesen
    /// Lauf: Ein spaeterer Fehler hat einen spaeteren Zeitstempel und wird wieder gezeigt.
    /// </summary>
    public bool BerichtIstFehler => LetzterBericht is { IstFehler: true } bericht
                                    && !(_einstellungen.Fuer(Eintrag.Id).FehlerQuittiertBis is { } marke
                                         && marke >= bericht.Zeit);
    public string BerichtKurz => LetzterBericht?.Kurzfassung ?? "";
    public string BerichtGrund => LetzterBericht?.Meldung ?? "";

    /// <summary>
    /// The update is downloaded and installed, but the program has not picked it up yet. Without
    /// saying so, a new check finds the same update again and it looks as if nothing happened --
    /// which is exactly how this shows up in practice.
    /// </summary>
    public bool UebernahmeOffen => LetzterBericht?.Ergebnis == LaufErgebnis.Ausstehend;

    public string UebernahmeText => "Das Update ist bereits heruntergeladen und installiert. Es wird aktiv, "
                                    + "sobald " + Name + " einmal neu gestartet wurde – bis dahin meldet die Prüfung "
                                    + "weiterhin die alte Version.";

    partial void OnLetzterBerichtChanged(UpdateBericht? value)
    {
        OnPropertyChanged(nameof(HatBericht));
        OnPropertyChanged(nameof(BerichtIstFehler));
        OnPropertyChanged(nameof(BerichtKurz));
        OnPropertyChanged(nameof(BerichtGrund));
        OnPropertyChanged(nameof(UebernahmeOffen));
        OnPropertyChanged(nameof(AktionMoeglich));
        OnPropertyChanged(nameof(AktionsText));
        OnPropertyChanged(nameof(AktionBetont));
    }

    /// <summary>Opens the log of the run that is shown on the card.</summary>
    [RelayCommand]
    private void ProtokollOeffnen() => Protokollierung.DateiOeffnen(LetzterBericht?.ProtokollDatei);

    /// <summary>
    /// Nimmt das rote Band von der Karte. Gemeldet bleibt der Lauf trotzdem: im Tagesprotokoll und
    /// in verlauf.jsonl steht er unveraendert, und die graue "Zuletzt:"-Zeile nennt ihn weiter.
    /// Weggeklickt wird nur der Alarm, nicht die Tatsache.
    /// </summary>
    [RelayCommand]
    private void FehlerQuittieren()
    {
        if (LetzterBericht is not { IstFehler: true } bericht) return;

        _einstellungen.Fuer(Eintrag.Id).FehlerQuittiertBis = bericht.Zeit;
        _einstellungen.Speichern();

        // LetzterBericht selbst aendert sich nicht, also meldet sich hier nichts von allein.
        OnPropertyChanged(nameof(BerichtIstFehler));
    }

    /// <summary>
    /// A staged update (Claude Desktop) only becomes real on the program's next start. On a later
    /// launch of the UpdateZentrale this confirms it retroactively -- or flags that it never
    /// arrived, which is exactly the case that would otherwise go unnoticed.
    /// </summary>
    public async Task AusstehendesPruefenAsync()
    {
        if (_aktualisierer is null || LetzterBericht is not { Ergebnis: LaufErgebnis.Ausstehend } offen) return;

        // Confirmed only with the expected target reached AND a real current check -- any other
        // fingerprint change is not proof when the same or another update is still offered.
        var jetzt = await _aktualisierer.FingerabdruckAsync(Eintrag, CancellationToken.None);
        var pruefung = await _aktualisierer.PruefenAsync(Eintrag, new Progress<string>(_ => { }), CancellationToken.None);
        if (UpdateKette.StagedUrteil(offen, jetzt, pruefung, DateTime.Now) is not { } urteil) return;

        var nachtrag = new UpdateBericht
        {
            Zeit = DateTime.Now,
            ProgrammId = Eintrag.Id,
            Name = Name,
            Art = Eintrag.Art,
            Ergebnis = urteil.Ergebnis,
            VersionVorher = offen.VersionNachher,
            VersionNachher = jetzt,
            Befehl = offen.Befehl,
            Erhoeht = Rechte.IstErhoeht,
            Meldung = urteil.Meldung
        };
        Protokollierung.LaufBeenden(nachtrag);
        LetzterBericht = nachtrag;
    }

    [ObservableProperty] private bool _autostartAlsAufgabe;

    /// <summary>
    /// Only worth showing where the conflict actually exists: the program starts elevated and has
    /// (or had) an autostart entry that Windows would now skip.
    /// </summary>
    public bool AutostartUmstellbar => KannStarten && (ImAutostart || AutostartAlsAufgabe) && AlsAdministrator;

    /// <summary>
    /// Kommandozeilen-Werkzeuge werden von anderen Programmen im Hintergrund gestartet (der
    /// OpenLauncher ruft lms.exe, jedes Terminal ruft claude.exe/codex.exe) — und zwar per
    /// CreateProcess ohne Shell. Für so einen Start kann Windows keinen UAC-Dialog zeigen: steht
    /// die exe auf "Als Administrator ausführen", bricht der Start mit Fehler 740 ab. Die
    /// Zentrale selbst umgeht das für ihre eigenen Aufrufe, fremde Programme können das nicht.
    /// </summary>
    public bool KonsolenAdminWarnung =>
        _alsAdministrator && Eintrag.Art.Equals("cli", StringComparison.OrdinalIgnoreCase);

    public void ZustandAktualisieren()
    {
        Laeuft = Prozessdienst.Laeuft(Eintrag);
        ImAutostart = Systemdienst.ImAutostart(Eintrag);
        AdminWarnung = Systemdienst.AdminBrichtAutostart(Eintrag, _alsAdministrator) && !AutostartAlsAufgabe;
        OnPropertyChanged(nameof(AutostartUmstellbar));
        OnPropertyChanged(nameof(KonsolenAdminWarnung));
    }

    /// <summary>Checks once whether the elevated logon task for this program exists.</summary>
    public async Task AufgabenZustandLesenAsync()
    {
        AutostartAlsAufgabe = await Aufgabenplanung.ExistiertAsync(Eintrag.Id);
        ZustandAktualisieren();
    }

    /// <summary>
    /// Moves the autostart from the Run key into a scheduled task with highest privileges -- and
    /// back again. Creating such a task needs an elevated UpdateZentrale.
    /// </summary>
    [RelayCommand]
    private async Task AutostartUmstellenAsync()
    {
        if (!Rechte.IstErhoeht)
        {
            Melde("Dafür muss die UpdateZentrale selbst mit Administratorrechten laufen.\n\n"
                  + "Oben auf „Als Administrator neu starten“ klicken und es danach erneut versuchen.");
            return;
        }

        var einstellung = _einstellungen.Fuer(Eintrag.Id);

        if (AutostartAlsAufgabe)
        {
            var (weg, ausgabe) = await Aufgabenplanung.EntfernenAsync(Eintrag.Id);
            if (!weg)
            {
                Melde("Die geplante Aufgabe ließ sich nicht entfernen:\n" + ausgabe);
                return;
            }

            // Put the original Run entry back exactly as it was.
            if (!string.IsNullOrWhiteSpace(einstellung.GesicherterRunName)
                && !string.IsNullOrWhiteSpace(einstellung.GesicherterRunWert))
            {
                Systemdienst.RunEintragSchreiben(einstellung.GesicherterRunName!, einstellung.GesicherterRunWert!);
                einstellung.GesicherterRunName = null;
                einstellung.GesicherterRunWert = null;
                _einstellungen.Speichern();
            }

            AutostartAlsAufgabe = false;
            StatusText = "Autostart läuft wieder über den normalen Windows-Autostart.";
        }
        else
        {
            var frage = "Der Autostart von " + Name + " wird auf eine geplante Aufgabe umgestellt.\n\n"
                      + "Damit startet das Programm bei der Anmeldung mit Administratorrechten und ohne Rückfrage "
                      + "der Benutzerkontensteuerung. Der bisherige Autostart-Eintrag wird gesichert und entfernt; "
                      + "beim Zurückstellen wird er wiederhergestellt.\n\nJetzt umstellen?";

            if (!Dialoge.Fragen(frage, "Autostart mit Administratorrechten?")) return;

            // Read the existing entry FIRST and reuse its exact command line -- wrappers like
            // wscript.exe with a watcher script must survive the move to the task.
            var vorhanden = Systemdienst.RunEintrag(Eintrag);
            var befehlszeile = vorhanden?.Wert;
            if (string.IsNullOrWhiteSpace(befehlszeile))
            {
                befehlszeile = "\"" + Pfade.Aufloesen(Eintrag.ExePfadWirksam) + "\"";
                if (!string.IsNullOrWhiteSpace(Eintrag.StartArgumente))
                    befehlszeile += " " + Eintrag.StartArgumente;
            }

            var (erfolg, ausgabe) = await Aufgabenplanung.AnlegenAsync(Eintrag.Id, befehlszeile!);

            if (!erfolg)
            {
                Melde("Die geplante Aufgabe ließ sich nicht anlegen:\n" + ausgabe);
                return;
            }

            if (vorhanden is not null)
            {
                einstellung.GesicherterRunName = vorhanden.Value.Name;
                einstellung.GesicherterRunWert = vorhanden.Value.Wert;
                _einstellungen.Speichern();
                Systemdienst.RunEintragEntfernen(vorhanden.Value.Name);
            }

            AutostartAlsAufgabe = true;
            StatusText = "Autostart läuft jetzt als geplante Aufgabe mit Administratorrechten.";
        }

        ZustandAktualisieren();
    }

    /// <summary>Forces the state converters to run again after a light/dark switch.</summary>
    public void DarstellungAuffrischen() => OnPropertyChanged(nameof(Zustand));

    partial void OnZustandChanged(UpdateZustand value)
    {
        OnPropertyChanged(nameof(HatUpdate));
        OnPropertyChanged(nameof(AktionMoeglich));
        OnPropertyChanged(nameof(AktionsText));
        OnPropertyChanged(nameof(AktionBetont));
    }

    partial void OnIstBeschaeftigtChanged(bool value)
    {
        OnPropertyChanged(nameof(AktionMoeglich));
        OnPropertyChanged(nameof(KannPruefen));
    }

    [RelayCommand]
    private Task PruefenAsync() => PruefenKernAsync(null);

    /// <summary>A check driven by the batch run, inside the batch's exclusive ownership.</summary>
    internal Task PruefenImSammelAsync(Laufbesitz sammel) => PruefenKernAsync(sammel);

    private async Task PruefenKernAsync(Laufbesitz? sammel)
    {
        if (_aktualisierer is null || IstBeschaeftigt) return;
        if (!Erwerben(sammel, out var eigen)) return;
        try
        {
            await LaufAsync(async (fortschritt, abbruch) =>
            {
                StatusText = "Wird geprüft …";
                Zustand = UpdateZustand.Pruefe;
                return await _aktualisierer.PruefenAsync(Eintrag, fortschritt, abbruch);
            });
        }
        finally
        {
            eigen?.Dispose();
        }
    }

    [RelayCommand]
    private Task AktualisierenAsync() => AktualisierenKernAsync(null);

    /// <summary>An update driven by "install all", inside the batch's exclusive ownership.</summary>
    internal Task AktualisierenImSammelAsync(Laufbesitz sammel) => AktualisierenKernAsync(sammel);

    private async Task AktualisierenKernAsync(Laufbesitz? sammel)
    {
        // Busy already: LaufAsync would drop this request anyway -- but only after the user had
        // agreed to close the program. Ownership is taken before that question for the same reason.
        if (_aktualisierer is null || IstBeschaeftigt) return;
        if (!Erwerben(sammel, out var eigen)) return;
        try
        {
            await AktualisierenMitBesitzAsync();
        }
        finally
        {
            eigen?.Dispose();
        }
    }

    /// <summary>Test seam: replaces the settle pauses of the update chain (never used by the app).</summary>
    internal Func<TimeSpan, CancellationToken, Task>? KettenWarten { get; set; }

    /// <summary>
    /// One click = one bounded update chain (see UpdateKette), inside the ownership taken by the
    /// caller. Exactly one log header and one footer; the report is written from the FINAL verdict
    /// only, never before the real provider re-check.
    /// </summary>
    private async Task AktualisierenMitBesitzAsync()
    {
        if (_aktualisierer is null) return;

        await LaufAsync(async (fortschritt, abbruch) =>
        {
            var kette = new UpdateKette(_aktualisierer, Eintrag, fortschritt, text => StatusText = text, KettenWarten);

            var (vorher, vorPruefung) = await kette.VorpruefenAsync(abbruch);
            Protokollierung.LaufBeginnen(Eintrag, BefehlsBeschreibung(), vorher);

            var urteil = UpdateKette.BereitsAktuell(vorher, vorPruefung);
            var liefVorher = false;

            if (urteil is null)
            {
                liefVorher = Prozessdienst.Laeuft(Eintrag);

                // Electron/NSIS installers hang silently while the app is running, so the helper
                // processes go down too -- but only after the user agreed, and only once per chain.
                if (Eintrag.BeendenVorUpdate && liefVorher)
                {
                    var laufende = Prozessdienst.Laufende(Eintrag).Count;
                    var frage = Name + " läuft gerade (" + laufende + " Prozess(e) einschließlich Helferprogramme).\n\n"
                              + "Zum Aktualisieren muss das Programm beendet werden."
                              + (Eintrag.NeuStartenNachUpdate ? " Danach wird es automatisch neu gestartet." : "")
                              + "\n\nJetzt beenden und aktualisieren?";

                    if (!Dialoge.Fragen(frage, "Programm beenden?"))
                    {
                        urteil = new KettenUrteil(LaufErgebnis.Abgebrochen, "Abgebrochen – das Programm läuft weiter.",
                            vorher, vorher, vorPruefung with { Zustand = UpdateZustand.Abgebrochen }, 0, false);
                    }
                    else
                    {
                        StatusText = "Beendet das Programm …";
                        fortschritt.Report("Beende " + string.Join(", ", Eintrag.AlleProzesse));
                        var beendet = await Prozessdienst.BeendenAsync(Eintrag, abbruch);
                        if (beendet.Problem is not null) fortschritt.Report(beendet.Problem);

                        // An installer next to a still running target hangs or half-installs.
                        if (!beendet.Erfolgreich)
                        {
                            var meldung = Name + " ließ sich nicht vollständig beenden – das Update wurde nicht gestartet. "
                                          + beendet.Problem;
                            urteil = new KettenUrteil(LaufErgebnis.Fehlgeschlagen, meldung, vorher, vorher,
                                vorPruefung with { Zustand = UpdateZustand.Fehler }, 0, false);
                        }
                    }
                }

                urteil ??= await kette.AusfuehrenAsync(vorher, vorPruefung, abbruch);
            }

            // Restart once, after the whole chain -- never between passes, where a running target
            // would block the next installer.
            if (Eintrag.NeuStartenNachUpdate && liefVorher && urteil.UpdateLiefDurch)
            {
                fortschritt.Report("Startet " + Name + " neu.");
                Prozessdienst.Starten(Eintrag, AlsAdministrator);
            }

            var bericht = new UpdateBericht
            {
                ProgrammId = Eintrag.Id,
                Name = Name,
                Art = Eintrag.Art,
                Ergebnis = urteil.Ergebnis,
                Meldung = urteil.Meldung,
                VersionVorher = urteil.FingerabdruckVorher,
                VersionNachher = urteil.FingerabdruckNachher,
                AusstehendeVersion = urteil.AusstehendeVersion,
                Befehl = BefehlsBeschreibung(),
                Erhoeht = Rechte.IstErhoeht,
                ExitCode = urteil.Ergebnis is LaufErgebnis.Fehlgeschlagen or LaufErgebnis.NichtVerifiziert ? 1 : 0
            };

            fortschritt.Report(AbschlussZeile(bericht));
            Protokollierung.LaufBeenden(bericht);
            LetzterBericht = bericht;

            return urteil.FuerKarte;
        });
    }

    /// <summary>
    /// A change is only proven when BOTH readings exist and differ. One empty side is not a
    /// change but a failed reading -- "1.2.3" -> "" used to count as success.
    /// </summary>
    internal static (LaufErgebnis Ergebnis, string Meldung) FingerabdruckUrteil(string vorher, string nachher)
    {
        var ohneVorher = string.IsNullOrWhiteSpace(vorher);
        var ohneNachher = string.IsNullOrWhiteSpace(nachher);

        if (ohneVorher && ohneNachher)
            return (LaufErgebnis.NichtVerifiziert, "Das Update meldete Erfolg, der Stand ließ sich aber weder vorher noch "
                                                   + "nachher ermitteln – es ist nicht überprüfbar.");
        if (ohneVorher || ohneNachher)
            return (LaufErgebnis.NichtVerifiziert, "Das Update meldete Erfolg, der Stand ließ sich aber "
                                                   + (ohneVorher ? "vorher" : "nachher") + " nicht ermitteln ("
                                                   + Beschreibe(vorher) + " → " + Beschreibe(nachher)
                                                   + ") – es ist nicht überprüfbar.");
        if (vorher == nachher)
            return (LaufErgebnis.NichtVerifiziert, "Das Update meldete Erfolg, der Stand ist aber unverändert ("
                                                   + Beschreibe(nachher) + "). Einzelheiten stehen im Protokoll.");

        return (LaufErgebnis.Erfolgreich, "Verifiziert: " + Beschreibe(vorher) + " → " + Beschreibe(nachher));
    }

    private static string Beschreibe(string fingerabdruck)
        => string.IsNullOrWhiteSpace(fingerabdruck) ? "(unbekannt)" : fingerabdruck;

    private static string AbschlussZeile(UpdateBericht bericht) => bericht.Ergebnis switch
    {
        LaufErgebnis.Erfolgreich or LaufErgebnis.BereitsAktuell => "✔ " + bericht.Meldung,
        LaufErgebnis.Ausstehend => "⏳ " + bericht.Meldung,
        LaufErgebnis.Abgebrochen => "– " + bericht.Meldung,
        _ => "✘ " + bericht.Meldung
    };

    /// <summary>Human-readable description of what this card actually runs, for the log header.</summary>
    private string BefehlsBeschreibung() => Eintrag.Art switch
    {
        "winget" => "winget upgrade --id " + Eintrag.WingetId + " --exact --silent",
        "store" or "msstore" => "winget upgrade --id " + Eintrag.StoreProduktId + " --source msstore --silent",
        "cli" => Pfade.Aufloesen(Eintrag.ExePfadWirksam) + " " + (Eintrag.UpdateArgumente ?? "update"),
        "reposkript" => "pwsh -File " + Eintrag.Skript + " " + Eintrag.SkriptArgumente,
        _ => Eintrag.Art
    };

    [RelayCommand]
    private void Starten()
    {
        if (!Prozessdienst.Starten(Eintrag, AlsAdministrator))
        {
            Melde("Konnte nicht gestartet werden: " + Pfade.Aufloesen(Eintrag.ExePfadWirksam));
        }
        ZustandAktualisieren();
    }

    private async Task LaufAsync(Func<IProgress<string>, CancellationToken, Task<PruefErgebnis>> arbeit)
    {
        if (IstBeschaeftigt) return;
        IstBeschaeftigt = true;

        var fortschritt = new Progress<string>(zeile =>
        {
            if (string.IsNullOrWhiteSpace(zeile)) return;
            _protokoll.AppendLine(zeile.Trim());
            Protokollierung.Schreiben(Eintrag.Id, zeile);
            OnPropertyChanged(nameof(Protokoll));
        });

        try
        {
            var ergebnis = await arbeit(fortschritt, CancellationToken.None);
            Zustand = ergebnis.Zustand;
            if (!string.IsNullOrWhiteSpace(ergebnis.InstallierteVersion)) InstallierteVersion = ergebnis.InstallierteVersion;
            VerfuegbareVersion = ergebnis.VerfuegbareVersion;
            StatusText = string.IsNullOrWhiteSpace(ergebnis.Meldung) ? AktionsText : ergebnis.Meldung;
        }
        catch (Exception ex)
        {
            Zustand = UpdateZustand.Fehler;
            StatusText = ex.Message;
            _protokoll.AppendLine(ex.ToString());
            Protokollierung.Schreiben(Eintrag.Id, "[Ausnahme] " + ex);
        }
        finally
        {
            IstBeschaeftigt = false;
            ZustandAktualisieren();
            OnPropertyChanged(nameof(VersionsText));
            OnPropertyChanged(nameof(Protokoll));
        }
    }

    private void Melde(string text) => Meldung?.Invoke(this, text);
}
