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
        // Every provider -- built in, new, or a test fake -- is wrapped here once, so all of them
        // report their calls to the diagnostics without any code of their own.
        _aktualisierer = aktualisierer is null ? null : DiagnoseAktualisierer.Umhuellen(aktualisierer);
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
        "opencodefix" => "Windows-Fix-Build",
        "opencodesitzung" => "OpenLauncher-Sitzung",
        _ => Eintrag.Art
    };

    public string? Hinweis => Eintrag.Hinweis;
    public bool HatHinweis => !string.IsNullOrWhiteSpace(Eintrag.Hinweis);

    public string? CliRisiko => Eintrag.CliRisiko is { } r ? "Kann CLI-Sitzungen beenden: " + r : null;
    public bool KannCliBeenden => Eintrag.KannCliBeenden;

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

    /// <summary>An update already handed to a CLI session is not offered again: neither here nor to a batch run.</summary>
    public bool HatUpdate => Zustand == UpdateZustand.UpdateVerfuegbar && !UpdateUebergeben;

    /// <summary>A session has this update; after this time it counts as ended, the same as an OpenCode session.</summary>
    public bool UpdateUebergeben => _einstellungen.Fuer(Eintrag.Id).UpdateUebergebenAm is { } am
                                    && DateTime.Now - am < Providers.OpenCodeSitzungAktualisierer.Sperrzeit;

    /// <summary>Not for the OpenCode card: its normal update already is such a session, with its own lock.</summary>
    public bool KannAnCliUebergeben => AktionBetont && !Eintrag.Art.Equals("opencodesitzung", StringComparison.OrdinalIgnoreCase);

    private void UebergabeMerken(DateTime? zeitpunkt)
    {
        _einstellungen.Fuer(Eintrag.Id).UpdateUebergebenAm = zeitpunkt;
        _einstellungen.Speichern();
        OnPropertyChanged(nameof(HatUpdate));
        OnPropertyChanged(nameof(AktionsText));
        OnPropertyChanged(nameof(AktionBetont));
        OnPropertyChanged(nameof(KannAnCliUebergeben));
    }

    /// <summary>
    /// The update button only invites action when there really is a newer version. Once a check
    /// found nothing, the button says "Aktuell" and is disabled instead of suggesting work.
    /// </summary>
    public bool AktionMoeglich => _aktualisierer is not null
                                  && !IstBeschaeftigt
                                  && !_koordination.Belegt
                                  && Zustand != UpdateZustand.Aktuell
                                  && !UebernahmeOffen;

    /// <summary>The source of the running update's cancel button; null while no update runs.</summary>
    private CancellationTokenSource? _updateAbbruch;

    /// <summary>The user cancelled this card's last update; a new update clears it.</summary>
    private bool _vomNutzerAbgebrochen;

    /// <summary>Set by the batch for a card it is past; asks whether a session may start right now.</summary>
    private Func<bool>? _uebergabeImSammel;

    /// <summary>
    /// Not for repo scripts and session starts: a script killed after closing its program leaves it
    /// closed or half built, and a session the launcher already started would not stop anyway.
    /// </summary>
    private bool AbbruchMoeglich => !Eintrag.Art.Equals("reposkript", StringComparison.OrdinalIgnoreCase)
                                    && !Eintrag.Art.Equals("opencodesitzung", StringComparison.OrdinalIgnoreCase);

    public bool KannAbbrechen => AbbruchMoeglich && _updateAbbruch is { IsCancellationRequested: false };

    /// <summary>
    /// A card cancelled inside the running batch can go to a session while that batch still holds the
    /// lock: the batch is past it. Only once no phase ahead would close the session's terminal.
    /// </summary>
    private bool UebergabeNachAbbruch => _vomNutzerAbgebrochen && !IstBeschaeftigt
                                         && LetzterBericht is { Ergebnis: LaufErgebnis.Abgebrochen }
                                         && _uebergabeImSammel?.Invoke() == true;

    public bool KannCliKnopf => AktionMoeglich || UebergabeNachAbbruch;

    internal void ImSammelFreigeben(Func<bool>? erlaubt)
    {
        _uebergabeImSammel = erlaubt;
        AktionsstandMelden();
    }

    /// <summary>The two properties behind the update and hand-over buttons always change together.</summary>
    internal void AktionsstandMelden()
    {
        OnPropertyChanged(nameof(AktionMoeglich));
        OnPropertyChanged(nameof(KannCliKnopf));
    }

    /// <summary>
    /// Ends the running update of this card; afterwards the real state is read back (see
    /// <see cref="NachAbbruchAsync"/>). Asks first, because a killed installer can leave a program half replaced.
    /// </summary>
    [RelayCommand]
    private void UpdateAbbrechen()
    {
        if (!KannAbbrechen) return;
        if (!Dialoge.Fragen("Das Update von " + Name + " jetzt abbrechen?\n\nDie laufende Installation wird beendet. "
                            + "Das kann ein halb ersetztes Programm hinterlassen; die UpdateZentrale prüft das danach und meldet es.",
                            "Update abbrechen?"))
            return;

        // The run may have ended while the question was open.
        if (_updateAbbruch is not { IsCancellationRequested: false } abbruch) return;
        _vomNutzerAbgebrochen = true;
        StatusText = "Bricht das Update ab …";
        Diagnose.Ereignis(Schwere.Info, "karte", "update.abbruch", Name + ": Update vom Nutzer abgebrochen.", "update",
                          new Dictionary<string, object?> { ["programm"] = Eintrag.Id });
        // Not Cancel(): the click must not wait for what the cancellation sets in motion.
        _ = abbruch.CancelAsync();
        OnPropertyChanged(nameof(KannAbbrechen));
    }

    public const string BelegtText = "Es läuft bereits ein anderer Vorgang – erst danach ist das möglich.";

    /// <summary>The buttons follow the app-wide lock at once, from whichever thread released it.</summary>
    private void AufKoordinationGeaendert(object? sender, EventArgs e)
    {
        void Melden()
        {
            AktionsstandMelden();
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
    private bool Erwerben(Laufbesitz? sammel, out Laufbesitz? eigen, string art)
    {
        eigen = null;
        if (sammel is not null)
        {
            // A batch token is only honoured while it really owns this coordination -- a
            // foreign, released or stale token must not slip a run past the lock.
            if (_koordination.IstAktiverSammelbesitz(sammel)) return true;
            Abgewiesen(art, "Sammelbesitz ungültig oder nicht mehr aktiv.");
            return false;
        }

        eigen = _koordination.EinzelBeginnen();
        if (eigen is not null) return true;

        Abgewiesen(art, "Es läuft bereits ein anderer Vorgang.");
        return false;
    }

    /// <summary>A refused or doubled start is recorded, with the program and what was refused.</summary>
    private void Abgewiesen(string art, string grund)
    {
        StatusText = BelegtText;
        Diagnose.Ereignis(Schwere.Warnung, "karte", "start.abgewiesen", art + " für " + Name + " abgewiesen: " + grund, art,
            new Dictionary<string, object?> { ["programm"] = Eintrag.Id, ["art"] = art });
    }

    public string AktionsText => UebernahmeOffen
        ? (Eintrag.Art.Equals("opencodesitzung", StringComparison.OrdinalIgnoreCase) ? "Sitzung baut" : "Neustart nötig")
        : Zustand switch
        {
            UpdateZustand.Aktuell => "Aktuell",
            UpdateZustand.UpdateVerfuegbar => UpdateUebergeben ? "Sitzung läuft" : "Aktualisieren",
            UpdateZustand.Pruefe => "Prüft …",
            UpdateZustand.NichtInstalliert => "Nicht installiert",
            _ => "Aktualisieren"
        };

    /// <summary>Only a real update gets the accent button; everything else stays calm.</summary>
    public bool AktionBetont => HatUpdate && !UebernahmeOffen;

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

    public string UebernahmeText => !string.IsNullOrWhiteSpace(Eintrag.UebernahmeText)
        ? Eintrag.UebernahmeText
        : "Das Update ist bereits heruntergeladen und installiert. Es wird aktiv, "
          + "sobald " + Name + " einmal neu gestartet wurde – bis dahin meldet die Prüfung "
          + "weiterhin die alte Version.";

    /// <summary>"Start and take over" only where there is something to start (not for the OpenCode session).</summary>
    /// <summary>This card is the app itself and its script just answered "started": the helper waits for the app to end.</summary>
    public bool IstSelbstupdateNeustart => Eintrag.Id.Equals(Sammelplan.SelbstId, StringComparison.OrdinalIgnoreCase)
                                           && LetzterBericht is { Ergebnis: LaufErgebnis.Ausstehend } b
                                           && DateTime.Now - b.Zeit < TimeSpan.FromMinutes(5);

    public bool UebernahmeStartbar => KannStarten && !Eintrag.Id.Equals(Sammelplan.SelbstId, StringComparison.OrdinalIgnoreCase);

    partial void OnLetzterBerichtChanged(UpdateBericht? value)
    {
        OnPropertyChanged(nameof(HatBericht));
        OnPropertyChanged(nameof(BerichtIstFehler));
        OnPropertyChanged(nameof(BerichtKurz));
        OnPropertyChanged(nameof(BerichtGrund));
        OnPropertyChanged(nameof(UebernahmeOffen));
        AktionsstandMelden();
        OnPropertyChanged(nameof(AktionsText));
        OnPropertyChanged(nameof(AktionBetont));
        OnPropertyChanged(nameof(KannAnCliUebergeben));
    }

    /// <summary>Opens the log of the run that is shown on the card.</summary>
    [RelayCommand]
    private void ProtokollOeffnen() => Protokollierung.DateiOeffnen(LetzterBericht?.ProtokollDatei);

    /// <summary>
    /// Sends the failed run with its log to a new Claude Code session to analyse and fix it.
    /// An async command is disabled while it runs, so a double click starts one session, not two.
    /// </summary>
    [RelayCommand]
    private async Task AnClaudeUebergebenAsync()
    {
        if (_uebergabeLaeuft || LetzterBericht is not { IstFehler: true } bericht) return;
        _uebergabeLaeuft = true;
        try
        {
            Melde("Übergebe das Problem mit " + Name + " an Claude Code …");
            var problem = await ClaudeUebergabe.StartenAsync(Eintrag, bericht);
            if (problem is null)
            {
                // The running session covers an automatic hand-over of the same failure too.
                _einstellungen.Fuer(Eintrag.Id).AutomatischUebergebenAm = DateTime.Now;
                _einstellungen.Speichern();
            }
            Melde(problem ?? "Problem mit " + Name + " an Claude Code übergeben – die Sitzung läuft im Terminal.");
        }
        finally
        {
            _uebergabeLaeuft = false;
        }
    }

    /// <summary>The state read before the current update; null until its log header is written.</summary>
    private string? _standVorLauf;

    /// <summary>Der vor dem Lauf wirksame Programmpfad, damit die Vollstaendigkeitspruefung nach dem Lauf denselben Ort prueft.</summary>
    private string? _pfadVorLauf;

    /// <summary>The handover buttons and the automatic handover must not start two sessions for one program.</summary>
    private bool _uebergabeLaeuft;

    /// <summary>
    /// Sends the whole pending update as a task to a new Claude Code session instead of running it here.
    /// The session works without asking, so everything the normal run would ask is asked here first.
    /// The app-wide lock is held while the launcher starts it, which can take up to two minutes.
    /// </summary>
    [RelayCommand]
    private async Task UpdateAnCliUebergebenAsync()
    {
        if (_uebergabeLaeuft || !KannAnCliUebergeben) return;

        if (KannCliBeenden && !Dialoge.Fragen(
                Name + " kann laufende CLI-Sitzungen beenden:\n\n" + Eintrag.CliRisiko
                + "\n\nSchließe vorher Claude Code, Codex und alle Terminalfenster. Jetzt trotzdem an eine CLI-Sitzung übergeben?",
                "CLI-Sitzungen in Gefahr"))
            return;

        var beendenNoetig = Eintrag.BeendenVorUpdate && Prozessdienst.Laeuft(Eintrag);
        if (beendenNoetig && !Dialoge.Fragen(Name + " läuft gerade und muss zum Aktualisieren beendet werden. "
                                             + "Das übernimmt die CLI-Sitzung.\n\nJetzt übergeben?", "Programm beenden?"))
            return;

        // Without a status prefix the script has no question of its own: the app has to ask.
        var skriptFragen = Eintrag.Art.Equals("reposkript", StringComparison.OrdinalIgnoreCase) && string.IsNullOrWhiteSpace(Eintrag.StatusPraefix);
        if (skriptFragen && !Dialoge.Fragen(Name + " wird neu gebaut und neu gestartet.\n\nJetzt an eine CLI-Sitzung übergeben?", Name + " aktualisieren?"))
            return;

        Laufbesitz? eigen = null;
        var ohneSperre = UebergabeNachAbbruch && _koordination.Belegt;
        if (!ohneSperre && !Erwerben(null, out eigen, "uebergabe")) return;
        // Without the lock the batch may end during the launcher start: mark first, so nothing offers this update again.
        if (ohneSperre) UebergabeMerken(DateTime.Now);
        _uebergabeLaeuft = true;
        try
        {
            Melde("Übergebe das Update von " + Name + " an Claude Code …");
            var (problem, vielleicht) = await ClaudeUebergabe.UpdateStartenAsync(Eintrag, InstallierteVersion, VerfuegbareVersion,
                                                                                 BefehlsBeschreibung(), beendenNoetig, skriptFragen);
            // An unanswered launcher usually started the session anyway: a second click must not start another.
            if (problem is null || vielleicht) UebergabeMerken(DateTime.Now);
            else if (ohneSperre) UebergabeMerken(null);
            Diagnose.Ereignis(problem is null ? Schwere.Info : Schwere.Warnung, "uebergabe", "update.uebergeben",
                              problem ?? Name + " an eine CLI-Sitzung übergeben.", null,
                              new Dictionary<string, object?> { ["programm"] = Eintrag.Id, ["ziel"] = VerfuegbareVersion });
            Melde(problem ?? "Update von " + Name + " an Claude Code übergeben – die Sitzung läuft im Terminal. Danach bestätigt „Prüfen“ den neuen Stand.");
        }
        finally
        {
            _uebergabeLaeuft = false;
            eigen?.Dispose();
        }
    }

    /// <summary>
    /// Right after a failure, without a click: at most one session per program within
    /// <see cref="ClaudeUebergabe.Sperrfrist"/>, so two sessions do not repair the same failure.
    /// </summary>
    private async Task AutomatischUebergebenAsync(UpdateBericht bericht)
    {
        // Leave the update run first: a dialog in here would keep the card and the app-wide lock busy.
        await Task.Yield();
        if (_uebergabeLaeuft) return;
        _uebergabeLaeuft = true;
        try
        {
            var ergebnis = await ClaudeUebergabe.AutomatischStartenAsync(new[] { (Eintrag, bericht) }, _einstellungen);
            if (ergebnis.Problem is not null) Melde(ergebnis.Problem);
            else
                StatusText = ergebnis.Gestartet
                    ? "Fehler automatisch an Claude Code übergeben – die Sitzung läuft im Terminal und beugt dem Fehler künftig vor."
                    : ClaudeUebergabe.SperrHinweis;
        }
        catch (Exception diagAusnahme)
        {
            Diagnose.Gefangen(diagAusnahme, "uebergabe", Schwere.Warnung);
        }
        finally
        {
            _uebergabeLaeuft = false;
        }
    }

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
    /// <param name="pruefung">A check that just ran -- reused instead of asking the provider again.</param>
    public async Task AusstehendesPruefenAsync(PruefErgebnis? pruefung = null)
    {
        if (_aktualisierer is null || LetzterBericht is not { Ergebnis: LaufErgebnis.Ausstehend } offen) return;

        // Confirmed only with the expected target reached AND a real current check -- any other
        // fingerprint change is not proof when the same or another update is still offered.
        using var vorgang = Diagnose.VorgangBeginnen("nachpruefung-ausstehend", Eintrag,
            "Ausstehendes Update vom " + offen.Zeit.ToString("dd.MM.yyyy") + " prüfen");
        var jetzt = await _aktualisierer.FingerabdruckAsync(Eintrag, CancellationToken.None);
        pruefung ??= await _aktualisierer.PruefenAsync(Eintrag, new Progress<string>(_ => { }), CancellationToken.None);
        if (UpdateKette.StagedUrteil(offen, jetzt, pruefung, DateTime.Now) is not { } urteil)
        {
            vorgang.Beenden("weiter-ausstehend", "Ziel noch nicht nachweislich übernommen.");
            return;
        }
        vorgang.Beenden(urteil.Ergebnis.ToString(), urteil.Meldung,
            urteil.Ergebnis == LaufErgebnis.Erfolgreich ? Schwere.Info : Schwere.Warnung);

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
    /// Kommandozeilen-Werkzeuge werden von anderen Programmen im Hintergrund gestartet (jedes
    /// Terminal ruft claude.exe/codex.exe) — und zwar per
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

    /// <summary>
    /// Shows the answer of an earlier check (usually the hidden one at Windows logon) right away.
    /// A pending takeover or an unknown mechanism keeps its own display.
    /// </summary>
    public void StandUebernehmen(PruefstandEintrag stand)
    {
        if (_aktualisierer is null || IstBeschaeftigt) return;
        Zustand = stand.Zustand;
        InstallierteVersion = stand.Installiert;
        VerfuegbareVersion = stand.Verfuegbar;
        StatusText = string.IsNullOrWhiteSpace(stand.Meldung) ? AktionsText : stand.Meldung;
        OnPropertyChanged(nameof(VersionsText));
    }

    /// <summary>Forces the state converters to run again after a light/dark switch.</summary>
    public void DarstellungAuffrischen() => OnPropertyChanged(nameof(Zustand));

    partial void OnZustandChanged(UpdateZustand value)
    {
        Diagnose.Ereignis(Schwere.Debug, "karte", "karte.zustand", Name + ": " + value, value.ToString(),
            new Dictionary<string, object?> { ["programm"] = Eintrag.Id });
        OnPropertyChanged(nameof(HatUpdate));
        AktionsstandMelden();
        OnPropertyChanged(nameof(AktionsText));
        OnPropertyChanged(nameof(AktionBetont));
        OnPropertyChanged(nameof(KannAnCliUebergeben));
        if (value == UpdateZustand.Aktuell && _einstellungen.Fuer(Eintrag.Id).UpdateUebergebenAm is not null) UebergabeMerken(null);
    }

    partial void OnIstBeschaeftigtChanged(bool value)
    {
        AktionsstandMelden();
        OnPropertyChanged(nameof(KannPruefen));
    }

    [RelayCommand]
    private Task PruefenAsync() => PruefenKernAsync(null);

    /// <summary>A check driven by the batch run, inside the batch's exclusive ownership.</summary>
    internal Task PruefenImSammelAsync(Laufbesitz sammel) => PruefenKernAsync(sammel);

    private async Task PruefenKernAsync(Laufbesitz? sammel)
    {
        if (_aktualisierer is null) return;
        if (IstBeschaeftigt) { Doppelt("pruefung"); return; }
        if (!Erwerben(sammel, out var eigen, "pruefung")) return;
        try
        {
            await LaufAsync("pruefung", async (fortschritt, abbruch) =>
            {
                StatusText = "Wird geprüft …";
                Zustand = UpdateZustand.Pruefe;
                return await _aktualisierer.PruefenAsync(Eintrag, fortschritt, abbruch);
            });

            // A pending update (staged installer, the OpenCode session) is confirmed by the very
            // check that sees it arrive -- not only at the next start of the app.
            if (UebernahmeOffen && _letztePruefung is { Zustand: UpdateZustand.Aktuell } frisch)
                await AusstehendesPruefenAsync(frisch);
        }
        finally
        {
            eigen?.Dispose();
        }
    }

    [RelayCommand]
    private Task AktualisierenAsync()
    {
        // A single click on a risky card gets the same warning the batch gives by holding it back.
        if (KannCliBeenden && !Dialoge.Fragen(
                Name + " kann laufende CLI-Sitzungen beenden:\n\n" + Eintrag.CliRisiko
                + "\n\nSchließe vorher Claude Code, Codex und alle Terminalfenster. Jetzt trotzdem aktualisieren?",
                "CLI-Sitzungen in Gefahr"))
        {
            return Task.CompletedTask;
        }
        if (UpdateUebergeben)
        {
            // The session may have ended early, so this stays possible -- but only on purpose.
            if (!Dialoge.Fragen("Dieses Update ging schon an eine CLI-Sitzung. Läuft sie noch, kämen sich beide Updates in die Quere."
                                + "\n\nJetzt trotzdem hier aktualisieren?", "CLI-Sitzung läuft"))
                return Task.CompletedTask;
            UebergabeMerken(null);
        }
        return AktualisierenKernAsync(null);
    }

    /// <summary>An update driven by "install all", inside the batch's exclusive ownership.</summary>
    internal async Task<UpdateBericht?> AktualisierenImSammelAsync(Laufbesitz sammel, SammelKontext? kontext = null)
    {
        _sammelKontext = kontext;
        var vorher = LetzterBericht;
        try
        {
            await AktualisierenKernAsync(sammel);
            // Only a report THIS run wrote: a refused start leaves yesterday's verdict in place, and
            // that must not count as this batch's result. An exception writes its own failed report.
            return ReferenceEquals(LetzterBericht, vorher) ? null : LetzterBericht;
        }
        finally
        {
            _sammelKontext = null;
        }
    }

    private SammelKontext? _sammelKontext;

    /// <summary>The answer of the check that just ran (null after an update or an exception).</summary>
    private PruefErgebnis? _letztePruefung;

    // ---------------- Dauer und Reihenfolge ----------------

    /// <summary>Position in programs.json; the order of the catalog view.</summary>
    public int KatalogIndex { get; set; }

    /// <summary>Position of the card's group in the catalog -- the first sort key, so groups stay whole.</summary>
    public int GruppenRang { get; set; }

    private List<double> _dauern = new();

    /// <summary>Measured durations of real updates, oldest first (from verlauf.jsonl).</summary>
    public void DauernSetzen(IEnumerable<double>? dauern)
    {
        _dauern = dauern?.ToList() ?? new List<double>();
        OnPropertyChanged(nameof(ErwarteteDauerSekunden));
        OnPropertyChanged(nameof(DauerText));
        OnPropertyChanged(nameof(DauerGemessen));
    }

    public bool DauerGemessen => _dauern.Count > 0;

    /// <summary>
    /// Median of the last three measured updates; without any, the catalog estimate, else a
    /// quarter of the time limit (VS 120 min → 30, a CLI 15 min → 4) -- crude, but it puts the
    /// big installers at the end until real numbers exist.
    /// </summary>
    public double ErwarteteDauerSekunden
    {
        get
        {
            if (_dauern.Count > 0)
            {
                var letzte = _dauern.TakeLast(3).OrderBy(d => d).ToList();
                return letzte[letzte.Count / 2];
            }
            return Eintrag.DauerSchaetzungSekunden ?? Math.Max(1, Eintrag.ZeitlimitMinuten) * 15.0;
        }
    }

    public string DauerText => DauerGemessen
        ? "⏱ Update zuletzt " + Protokollierung.DauerText(ErwarteteDauerSekunden)
          + (_dauern.Count > 1 ? " (Median aus " + Math.Min(_dauern.Count, 3) + ")" : "")
        : "⏱ Update ≈ " + Protokollierung.DauerText(ErwarteteDauerSekunden) + " (geschätzt)";

    /// <summary>"Install all" leaves a major version jump to a deliberate click when the catalog says so.</summary>
    public bool IstHauptversionsSprung
    {
        get
        {
            if (!Eintrag.HauptversionNurPerKlick) return false;
            static string Haupt(string v) => new string(v.TrimStart('<', '>', ' ', 'v').TakeWhile(char.IsDigit).ToArray());
            var alt = Haupt(InstallierteVersion);
            var neu = Haupt(VerfuegbareVersion);
            return alt.Length > 0 && neu.Length > 0 && alt != neu;
        }
    }

    private async Task AktualisierenKernAsync(Laufbesitz? sammel)
    {
        // Busy already: LaufAsync would drop this request anyway -- but only after the user had
        // agreed to close the program. Ownership is taken before that question for the same reason.
        if (_aktualisierer is null) return;
        if (IstBeschaeftigt) { Doppelt("update"); return; }
        if (!Erwerben(sammel, out var eigen, "update")) return;
        var berichtVorher = LetzterBericht;
        try
        {
            await AktualisierenMitBesitzAsync();
        }
        finally
        {
            eigen?.Dispose();
        }

        // A single click on the own card: the run is fully written now (report, card state), so
        // the app ends itself and the script's helper copies the new build in. Inside a batch the
        // batch does this after its own log line.
        if (sammel is null && !ReferenceEquals(LetzterBericht, berichtVorher) && IstSelbstupdateNeustart)
            await Selbstupdate.AppBeendenAsync();
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

        await LaufAsync("update", async (fortschritt, abbruch) =>
        {
            var uhr = System.Diagnostics.Stopwatch.StartNew();
            var kontext = _sammelKontext;
            var kette = new UpdateKette(_aktualisierer, Eintrag, fortschritt, text => StatusText = text, KettenWarten);
            _standVorLauf = null;
            _pfadVorLauf = Pfade.Aufloesen(Eintrag.ExePfadWirksam);
            var angebotVorher = VerfuegbareVersion;

            var vorher = "";
            PruefErgebnis? vorPruefung = null;
            KettenUrteil? urteil = null;
            var liefVorher = false;
            var vomUpdateGeschlossen = false;

            try
            {
                var (stand, pruefung) = await kette.VorpruefenAsync(abbruch);
                vorher = stand;
                vorPruefung = pruefung;
                Protokollierung.LaufBeginnen(Eintrag, BefehlsBeschreibung(), vorher);
                _standVorLauf = vorher;
                abbruch.ThrowIfCancellationRequested();

                urteil = UpdateKette.BereitsAktuell(vorher, pruefung);

                if (urteil is null)
                {
                    // Before the kill question: the program must not stay closed for the whole wait.
                    if (await kette.SperreAbwartenAsync(abbruch) is { } gesperrt)
                        urteil = new KettenUrteil(LaufErgebnis.Fehlgeschlagen, gesperrt, vorher, vorher,
                            pruefung with { Zustand = UpdateZustand.Fehler, Meldung = gesperrt }, 0, false);

                    liefVorher = Prozessdienst.Laeuft(Eintrag);

                    // Electron/NSIS installers hang silently while the app is running, so the helper
                    // processes go down too -- but only after the user agreed, and only once per chain.
                    if (urteil is null && Eintrag.BeendenVorUpdate && liefVorher)
                    {
                        var laufende = Prozessdienst.Laufende(Eintrag).Count;
                        var frage = Name + " läuft gerade (" + laufende + " Prozess(e) einschließlich Helferprogramme).\n\n"
                                  + "Zum Aktualisieren muss das Programm beendet werden."
                                  + (Eintrag.NeuStartenNachUpdate ? " Danach wird es automatisch neu gestartet." : "")
                                  + "\n\nJetzt beenden und aktualisieren?";

                        if (!Dialoge.Fragen(frage, "Programm beenden?"))
                        {
                            urteil = new KettenUrteil(LaufErgebnis.Abgebrochen, "Abgebrochen – das Programm läuft weiter.",
                                vorher, vorher, pruefung with { Zustand = UpdateZustand.Abgebrochen }, 0, false);
                        }
                        else
                        {
                            StatusText = "Beendet das Programm …";
                            fortschritt.Report("Beende " + string.Join(", ", Eintrag.AlleProzesse));
                            // Without the cancel token: it would cut the grace period and kill unsaved work at once.
                            var beendet = await Prozessdienst.BeendenAsync(Eintrag, CancellationToken.None);
                            if (beendet.Problem is not null) fortschritt.Report(beendet.Problem);
                            vomUpdateGeschlossen = beendet.Erfolgreich;

                            // An installer next to a still running target hangs or half-installs.
                            if (!beendet.Erfolgreich)
                            {
                                var meldung = Name + " ließ sich nicht vollständig beenden – das Update wurde nicht gestartet. "
                                              + beendet.Problem;
                                urteil = new KettenUrteil(LaufErgebnis.Fehlgeschlagen, meldung, vorher, vorher,
                                    pruefung with { Zustand = UpdateZustand.Fehler }, 0, false);
                            }
                        }
                    }

                    urteil ??= await kette.AusfuehrenAsync(vorher, pruefung, abbruch);
                }
            }
            catch (OperationCanceledException) when (abbruch.IsCancellationRequested)
            {
                // Cancelled between two commands (a wait, the winget queue): judged below like any other cancel.
                urteil = null;
            }

            // The one place a cancel is judged, whatever a provider made of its killed command: only a
            // result the update really reached survives it. The state is read back, not assumed.
            var nutzerAbbruch = abbruch.IsCancellationRequested
                                && urteil is not { Ergebnis: LaufErgebnis.Erfolgreich or LaufErgebnis.Ausstehend or LaufErgebnis.BereitsAktuell };
            if (_standVorLauf is null) Protokollierung.LaufBeginnen(Eintrag, BefehlsBeschreibung(), "");
            urteil = nutzerAbbruch ? await NachAbbruchAsync(urteil, vorher, vorPruefung, angebotVorher, fortschritt) : urteil!;

            // Ein abgebrochenes oder gescheitertes Update kann die Programmdatei halb ersetzt zuruecklassen.
            // Das muss als eigener Befund sichtbar werden, nicht nur als "Zeitlimit".
            // Ein Installer ausserhalb des beendeten Prozessbaums (msiexec im Windows-Installer-Dienst)
            // kopiert womoeglich noch: bis zu einer Minute nachpruefen, bevor "fehlt" gilt.
            var fehlt = urteil.Ergebnis == LaufErgebnis.Erfolgreich ? null : Programmpruefung.Vollstaendig(Eintrag, _pfadVorLauf);
            for (var versuch = 0; fehlt is not null && versuch < 12; versuch++)
            {
                await Task.Delay(TimeSpan.FromSeconds(5));
                fehlt = Programmpruefung.Vollstaendig(Eintrag, _pfadVorLauf);
            }
            if (fehlt is not null)
            {
                fortschritt.Report("⚠ " + fehlt);
                urteil = urteil with
                {
                    Ergebnis = LaufErgebnis.Fehlgeschlagen,
                    Meldung = fehlt + " Vorheriger Befund: " + urteil.Meldung,
                    FuerKarte = urteil.FuerKarte with { Zustand = UpdateZustand.Fehler },
                    UpdateLiefDurch = false
                };
            }

            // Restart once, after the whole chain -- never between passes, where a running target
            // would block the next installer.
            // After a cancel too, if this update closed the program: it should not stay closed.
            if (Eintrag.NeuStartenNachUpdate && liefVorher
                && (urteil.UpdateLiefDurch || (nutzerAbbruch && vomUpdateGeschlossen && !Prozessdienst.Laeuft(Eintrag))))
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
                ExitCode = urteil.Ergebnis is LaufErgebnis.Fehlgeschlagen or LaufErgebnis.NichtVerifiziert ? 1 : 0,
                DauerSekunden = Math.Round(uhr.Elapsed.TotalSeconds, 1),
                ArbeitSekunden = urteil.ArbeitSekunden,
                UpdateAufrufe = urteil.UpdateAufrufe,
                SammelId = kontext?.Id,
                SammelPhase = kontext?.Phase,
                SammelPosition = kontext?.Position,
                // After a cancel only a program left incomplete goes to a session on its own.
                OhneAutoUebergabe = nutzerAbbruch && fehlt is null
            };

            fortschritt.Report(AbschlussZeile(bericht) + "  (Dauer " + Protokollierung.DauerText(uhr.Elapsed.TotalSeconds) + ")");
            Protokollierung.LaufBeenden(bericht);
            LetzterBericht = bericht;
            // In a batch the batch hands the failure over as soon as no CLI-critical step is ahead.
            if (kontext is null && ClaudeUebergabe.AutomatischRelevant(bericht)) _ = AutomatischUebergebenAsync(bericht);
            if (bericht.Ergebnis is LaufErgebnis.Erfolgreich or LaufErgebnis.Ausstehend && Protokollierung.Messwert(bericht) is { } messwert)
                DauernSetzen(_dauern.Append(messwert));

            return urteil.FuerKarte;
        });
    }

    /// <summary>
    /// What a cancelled update really left behind, read back without the fired token: an installer
    /// that had finished counts as done, and the card shows the check's offer, not the cancel.
    /// </summary>
    /// <param name="bisher">The chain's verdict; null when the cancel threw between two commands.</param>
    private async Task<KettenUrteil> NachAbbruchAsync(KettenUrteil? bisher, string vorher, PruefErgebnis? vorPruefung,
                                                      string angebotVorher, IProgress<string> fortschritt)
    {
        StatusText = "Abgebrochen – prüft den Stand …";
        var stand = "";
        PruefErgebnis? jetzt = null;
        try
        {
            // Bounded: the winget queue may be held by another card of the batch for a long time.
            using var frist = new CancellationTokenSource(TimeSpan.FromMinutes(1));
            stand = await _aktualisierer!.FingerabdruckAsync(Eintrag, frist.Token);
            jetzt = await _aktualisierer.PruefenAsync(Eintrag, fortschritt, frist.Token);
        }
        catch (Exception diagAusnahme)
        {
            Diagnose.Gefangen(diagAusnahme, "karte", Schwere.Info);
        }

        var nachher = string.IsNullOrWhiteSpace(stand) ? UpdateKette.NichtLesbar : stand;
        var aufrufe = bisher?.UpdateAufrufe ?? 0;
        var arbeit = bisher?.ArbeitSekunden ?? 0;

        // A process survived the kill: a failure, and it may still be installing.
        if (bisher is not null && bisher.Meldung.Contains(Kommandozeile.BaumBleibtText, StringComparison.Ordinal))
            return bisher with { Ergebnis = LaufErgebnis.Fehlgeschlagen, FingerabdruckNachher = nachher };

        if (jetzt is { Zustand: UpdateZustand.Aktuell })
        {
            var bewegt = !string.IsNullOrWhiteSpace(stand) && !string.IsNullOrWhiteSpace(vorher) && stand != vorher;
            return new KettenUrteil(bewegt ? LaufErgebnis.Erfolgreich : LaufErgebnis.BereitsAktuell,
                bewegt ? "Das Update war beim Abbruch schon durch: " + vorher + " → " + stand
                       : "Abgebrochen, die Prüfung meldet aber den aktuellen Stand.",
                vorher, nachher, jetzt with { Meldung = "Auf dem neuesten Stand." }, aufrufe, bewegt) { ArbeitSekunden = arbeit };
        }

        var angebot = new[] { jetzt?.VerfuegbareVersion, vorPruefung?.VerfuegbareVersion, angebotVorher }
                          .FirstOrDefault(v => !string.IsNullOrWhiteSpace(v)) ?? "";
        var karte = (jetzt ?? vorPruefung ?? new PruefErgebnis(UpdateZustand.Abgebrochen)) with
        {
            Zustand = angebot.Length > 0 ? UpdateZustand.UpdateVerfuegbar : UpdateZustand.Abgebrochen,
            VerfuegbareVersion = angebot,
            Meldung = "Abgebrochen – das Update ist weiter offen."
        };
        return new KettenUrteil(LaufErgebnis.Abgebrochen, "Vom Nutzer abgebrochen.", vorher, nachher, karte, aufrufe, false) { ArbeitSekunden = arbeit };
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
        "winget" => "winget " + Providers.WingetAktualisierer.UpdateBefehl(Eintrag) + " --silent",
        "store" or "msstore" => "winget upgrade --id " + Eintrag.StoreProduktId + " --source msstore --silent",
        "cli" => Pfade.Aufloesen(string.IsNullOrWhiteSpace(Eintrag.UpdateExePfad) ? Eintrag.ExePfadWirksam : Eintrag.UpdateExePfad)
                 + " " + (Eintrag.UpdateArgumente ?? "update"),
        "reposkript" => "pwsh -File " + Eintrag.Skript + " " + Eintrag.SkriptArgumente,
        "opencodefix" => Providers.OpenCodeFixAktualisierer.Befehlsbeschreibung,
        "opencodesitzung" => Providers.OpenCodeSitzungAktualisierer.Befehlsbeschreibung,
        _ => Eintrag.Art
    };

    [RelayCommand]
    private async Task StartenAsync()
    {
        // A staged package update is registered only once every process of the app has ended. A
        // new window of an app that still runs changes nothing -- Windows Terminal hosts all its
        // windows in one process, so "start" alone left the card pending forever. The takeover
        // therefore closes the app, registers the staged package and starts it fresh.
        var uebernehmen = UebernahmeOffen && Eintrag.IstPaketApp;
        if (uebernehmen)
        {
            if (!Dialoge.Fragen("Zum Übernehmen werden alle Fenster von " + Name + " geschlossen – auch darin "
                                + "laufende Claude- oder Codex-Sitzungen. Danach startet " + Name
                                + " in der neuen Version.\n\nJetzt schließen und übernehmen?", "Update übernehmen"))
                return;

            // -ForceApplicationShutdown does not close the app for -RegisterByFamilyName (0x80073D02,
            // "in use"), so its processes -- everything running from its WindowsApps folder -- are
            // ended here first. If registering the staged package still fails, winget installs it
            // again now that nothing holds it.
            var lauf = await Kommandozeile.AusfuehrenAsync("powershell.exe",
                "-NoProfile -NonInteractive -Command \""
                + "$p = Get-Process | Where-Object { $_.Path -like '*\\WindowsApps\\" + Eintrag.AppxName + "_*' }; "
                + "$p | Stop-Process -Force -ErrorAction SilentlyContinue; "
                + "$p | Wait-Process -Timeout 20 -ErrorAction SilentlyContinue; Start-Sleep -Seconds 2; "
                + "try { Add-AppxPackage -RegisterByFamilyName -MainPackage '" + Eintrag.PackageFamilyName + "' -ErrorAction Stop } "
                + "catch { & '" + Pfade.Winget + "' upgrade --id " + Eintrag.WingetId + " --exact --silent "
                + "--disable-interactivity --accept-source-agreements --accept-package-agreements; "
                + "if ($LASTEXITCODE -ne 0) { Write-Output $_; exit 1 } }\"",
                TimeSpan.FromMinutes(10));
            if (lauf.ExitCode != 0)
                Melde("Das Update ließ sich nicht übernehmen:\n" + lauf.Ausgabe);
        }

        if (!Prozessdienst.Starten(Eintrag, AlsAdministrator))
        {
            Melde("Konnte nicht gestartet werden: " + Pfade.Aufloesen(Eintrag.ExePfadWirksam));
        }
        ZustandAktualisieren();
        if (uebernehmen) await AusstehendesPruefenAsync();
    }

    private void Doppelt(string art)
        => Diagnose.Ereignis(Schwere.Warnung, "karte", "start.doppelt", art + " für " + Name + " ignoriert: die Karte arbeitet bereits.", art,
            new Dictionary<string, object?> { ["programm"] = Eintrag.Id, ["art"] = art });

    /// <summary>What the card keeps in memory for the detail pane -- bounded, newest text wins.</summary>
    private const int MaxProtokollZeichen = 200_000;

    private void ProtokollAnhaengen(string text)
    {
        _protokoll.AppendLine(text);
        if (_protokoll.Length > MaxProtokollZeichen)
            _protokoll.Remove(0, _protokoll.Length - MaxProtokollZeichen).Insert(0, "…[ältere Zeilen gekürzt]…" + Environment.NewLine);
    }

    /// <summary>
    /// Every check and update of every card runs through here: one diagnostics operation with
    /// begin, end, duration and verdict, the program as context for everything below it, and a
    /// recorded exception instead of a message only on screen.
    /// </summary>
    private async Task LaufAsync(string art, Func<IProgress<string>, CancellationToken, Task<PruefErgebnis>> arbeit)
    {
        if (IstBeschaeftigt) { Doppelt(art); return; }
        IstBeschaeftigt = true;
        var berichtBeiStart = LetzterBericht;
        if (art == "update")
        {
            _vomNutzerAbgebrochen = false;
            _updateAbbruch = new CancellationTokenSource();
            OnPropertyChanged(nameof(KannAbbrechen));
        }
        var abbruch = _updateAbbruch;

        using var vorgang = Diagnose.VorgangBeginnen(art, Eintrag, art + " " + Name);
        var fortschritt = new Progress<string>(zeile =>
        {
            if (string.IsNullOrWhiteSpace(zeile)) return;
            ProtokollAnhaengen(zeile.Trim());
            Protokollierung.Schreiben(Eintrag.Id, zeile);
            OnPropertyChanged(nameof(Protokoll));
        });

        try
        {
            _letztePruefung = null;
            var ergebnis = await arbeit(fortschritt, abbruch?.Token ?? CancellationToken.None);
            if (art == "pruefung") _letztePruefung = ergebnis;
            Zustand = ergebnis.Zustand;
            if (!string.IsNullOrWhiteSpace(ergebnis.InstallierteVersion)) InstallierteVersion = ergebnis.InstallierteVersion;
            VerfuegbareVersion = ergebnis.VerfuegbareVersion;
            StatusText = string.IsNullOrWhiteSpace(ergebnis.Meldung) ? AktionsText : ergebnis.Meldung;

            // A check's answer is kept for the next start; an update makes the kept answer void.
            if (art == "pruefung") Pruefstand.Merken(Eintrag.Id, ergebnis with { InstallierteVersion = InstallierteVersion });
            else Pruefstand.Vergessen(Eintrag.Id);
            vorgang.Beenden(ergebnis.Zustand.ToString(), ergebnis.Meldung, ergebnis.Zustand switch
            {
                UpdateZustand.Fehler => Schwere.Fehler,
                UpdateZustand.Unbekannt or UpdateZustand.Abgebrochen => Schwere.Warnung,
                _ => Schwere.Info
            });
        }
        catch (Exception ex)
        {
            Zustand = UpdateZustand.Fehler;
            StatusText = ex.Message;
            if (art != "pruefung") Pruefstand.Vergessen(Eintrag.Id);
            ProtokollAnhaengen(ex.ToString());
            Protokollierung.Schreiben(Eintrag.Id, "[Ausnahme] " + ex);
            Diagnose.Ausnahme(ex, "karte", art + " " + Eintrag.Id);
            vorgang.Beenden("Ausnahme", ex.GetType().Name + ": " + ex.Message, Schwere.Fehler);
            if (art == "update" && ReferenceEquals(LetzterBericht, berichtBeiStart)) AusnahmeBerichten(ex);
        }
        finally
        {
            if (abbruch is not null)
            {
                _updateAbbruch = null;
                abbruch.Dispose();
                OnPropertyChanged(nameof(KannAbbrechen));
            }
            IstBeschaeftigt = false;
            ZustandAktualisieren();
            OnPropertyChanged(nameof(VersionsText));
            OnPropertyChanged(nameof(Protokoll));
        }
    }

    /// <summary>
    /// An update that ended in an exception is a failed update too: without this report it reached
    /// neither the history nor the automatic hand-over to a session.
    /// </summary>
    private void AusnahmeBerichten(Exception ausnahme)
    {
        // The app is closing: what breaks now is the shutdown, not the update.
        if (System.Windows.Application.Current?.Dispatcher.HasShutdownStarted == true) return;
        try
        {
            var kontext = _sammelKontext;
            var befehl = "";
            try { befehl = BefehlsBeschreibung(); }
            catch (Exception diagAusnahme) { Diagnose.Gefangen(diagAusnahme, "uebergabe", Schwere.Debug); }

            // Thrown by the pre-check: the log needs its header too, or the footer stands alone.
            if (_standVorLauf is null) Protokollierung.LaufBeginnen(Eintrag, befehl, "");
            var fehlt = Programmpruefung.Vollstaendig(Eintrag, _pfadVorLauf);

            var bericht = new UpdateBericht
            {
                ProgrammId = Eintrag.Id,
                Name = Name,
                Art = Eintrag.Art,
                Ergebnis = LaufErgebnis.Fehlgeschlagen,
                Meldung = (fehlt is null ? "" : fehlt + " ") + "Das Update brach mit einer Ausnahme ab: " + ausnahme.GetType().Name + ": " + ausnahme.Message,
                VersionVorher = _standVorLauf ?? "",
                VersionNachher = UpdateKette.NichtLesbar,
                Befehl = befehl,
                Erhoeht = Rechte.IstErhoeht,
                ExitCode = 1,
                SammelId = kontext?.Id,
                SammelPhase = kontext?.Phase,
                SammelPosition = kontext?.Position
            };
            Protokollierung.LaufBeenden(bericht);
            LetzterBericht = bericht;
            // In a batch the batch hands the failure over as soon as no CLI-critical step is ahead.
            if (kontext is null && ClaudeUebergabe.AutomatischRelevant(bericht)) _ = AutomatischUebergebenAsync(bericht);
        }
        catch (Exception diagAusnahme)
        {
            Diagnose.Gefangen(diagAusnahme, "uebergabe", Schwere.Warnung);
        }
    }

    private void Melde(string text) => Meldung?.Invoke(this, text);
}

/// <summary>Which batch, phase and position an update ran in -- written into its report.</summary>
public sealed record SammelKontext(string Id, int Phase, int Position);
