using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Diagnostics;
using System.Reflection;
using System.Windows.Data;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using UpdateZentrale.Models;
using UpdateZentrale.Providers;
using UpdateZentrale.Services;

namespace UpdateZentrale.ViewModels;

public sealed partial class HauptViewModel : ObservableObject
{
    private readonly Einstellungen _einstellungen = Einstellungen.Laden();

    /// <summary>App-wide: at most one external operation (check, update, batch) at a time.</summary>
    private readonly Laufkoordination _koordination = new();

    /// <summary>
    /// All known mechanisms, keyed by the catalog field "art". A new program is a JSON entry;
    /// only a genuinely new mechanism needs a new entry here.
    /// </summary>
    private readonly Dictionary<string, IAktualisierer> _aktualisierer =
        new(StringComparer.OrdinalIgnoreCase)
        {
            ["winget"] = new WingetAktualisierer(),
            ["cli"] = new CliAktualisierer(),
            ["store"] = new StoreAktualisierer(),
            ["msstore"] = new StoreAktualisierer(),   // Altname aus früheren Katalogfassungen
            ["reposkript"] = new RepoSkriptAktualisierer(),
            ["opencodefix"] = new OpenCodeFixAktualisierer(),
            ["opencodesitzung"] = new OpenCodeSitzungAktualisierer()
        };

    public HauptViewModel()
    {
        Programme = new ObservableCollection<ProgrammViewModel>();
        Ansicht = CollectionViewSource.GetDefaultView(Programme);
        // A card whose measured duration changes moves at once while "⏱ nach Dauer" is on.
        if (Ansicht is ICollectionViewLiveShaping live && live.CanChangeLiveSorting)
        {
            live.LiveSortingProperties.Add(nameof(ProgrammViewModel.ErwarteteDauerSekunden));
            live.IsLiveSorting = true;
        }
        Ansicht.GroupDescriptions.Add(new PropertyGroupDescription(nameof(ProgrammViewModel.Gruppe)));

        _hellModus = _einstellungen.HellModus;
        Darstellung.Anwenden(_hellModus);

        // The updater is meant to be able to install anything, so it marks itself to always run
        // elevated on the first launch. The switch in the footer turns that off again.
        // Marked as done only when the registry write really succeeded -- otherwise the next start
        // tries again instead of the app believing in a setting Windows never got.
        if (!_einstellungen.AdminStartGesetzt && Rechte.ImmerAlsAdminSetzen(true))
        {
            _einstellungen.AdminStartGesetzt = true;
            _einstellungen.Speichern();
        }
        _immerAlsAdmin = Rechte.ImmerAlsAdmin;

        _koordination.Geaendert += AufKoordinationGeaendert;
        Diagnose.WarnungAbonnieren(AufDiagnoseWarnung);   // also shows a warning raised before this point
        KatalogLaden();
    }

    public ObservableCollection<ProgrammViewModel> Programme { get; }

    public ICollectionView Ansicht { get; }

    [ObservableProperty] private ProgrammViewModel? _ausgewaehltesProgramm;
    [ObservableProperty] private string _kopfStatus = "Bereit.";

    /// <summary>Second footer line during "install all": what runs right now, at which position, since when.</summary>
    [ObservableProperty] private string _laufDetail = "";
    [ObservableProperty] private bool _laeuftSammelvorgang;
    [ObservableProperty] private string? _katalogFehler;

    private bool _hellModus;

    /// <summary>Light or dark; the choice is remembered in settings.json.</summary>
    public bool HellModus
    {
        get => _hellModus;
        set
        {
            if (_hellModus == value) return;
            _hellModus = value;

            Darstellung.Anwenden(value);
            _einstellungen.HellModus = value;
            _einstellungen.Speichern();

            // The state pills are drawn by converters that read the mode, so they need a nudge.
            foreach (var p in Programme) p.DarstellungAuffrischen();

            OnPropertyChanged();
            OnPropertyChanged(nameof(DarstellungsText));
        }
    }

    public string DarstellungsText => HellModus ? "Heller Modus" : "Dunkler Modus";

    // ---------------- Rechte ----------------

    private bool _immerAlsAdmin;

    public bool IstErhoeht => Rechte.IstErhoeht;
    public bool NichtErhoeht => !Rechte.IstErhoeht;

    public string RechteText => Rechte.IstErhoeht
        ? "Läuft mit Administratorrechten"
        : "Läuft ohne Administratorrechte";

    /// <summary>Marks the exe itself with the RUNASADMIN compatibility flag.</summary>
    public bool ImmerAlsAdmin
    {
        get => _immerAlsAdmin;
        set
        {
            if (_immerAlsAdmin == value) return;
            _immerAlsAdmin = value;

            if (!Rechte.ImmerAlsAdminSetzen(value))
            {
                _immerAlsAdmin = !value;
                Dialoge.Hinweis("Die Einstellung ließ sich nicht schreiben.");
            }
            OnPropertyChanged();
        }
    }

    [RelayCommand]
    private void AlsAdminNeuStarten()
    {
        if (Rechte.IstErhoeht)
        {
            Dialoge.Hinweis("Die UpdateZentrale läuft bereits mit Administratorrechten.");
            return;
        }

        if (Rechte.NeuStartenAlsAdmin())
        {
            System.Windows.Application.Current?.Shutdown();
        }
        else
        {
            KopfStatus = "Der Neustart mit Administratorrechten wurde abgebrochen.";
        }
    }

    // ---------------- Terminal ----------------

    [ObservableProperty] private string _befehl = "";
    [ObservableProperty] private string _terminalAusgabe = "";
    [ObservableProperty] private bool _terminalLaeuft;

    public string TerminalKopf => "Terminal · " + (Rechte.IstErhoeht ? "Administrator" : "Standardrechte");

    [RelayCommand]
    private async Task BefehlAusfuehrenAsync()
    {
        var befehl = Befehl.Trim();
        if (string.IsNullOrWhiteSpace(befehl) || TerminalLaeuft) return;

        TerminalLaeuft = true;
        TerminalAusgabe += (TerminalAusgabe.Length == 0 ? "" : "\n\n") + "PS> " + befehl + "\n";
        Befehl = "";

        using var vorgang = Diagnose.VorgangBeginnen("terminal", null, "Terminalbefehl: " + Bereinigung.Sicher(befehl, 300));
        try
        {
            var ausgabe = await Terminal.AusfuehrenAsync(befehl);
            TerminalAusgabe += ausgabe;
            vorgang.Beenden("abgeschlossen");
        }
        catch (Exception ex)
        {
            TerminalAusgabe += "[Fehler] " + ex.Message;
            Diagnose.Ausnahme(ex, "terminal", "Terminalbefehl");
            vorgang.Beenden("Ausnahme", ex.Message, Schwere.Fehler);
        }
        finally
        {
            TerminalLaeuft = false;
        }
    }

    [RelayCommand]
    private void TerminalLeeren() => TerminalAusgabe = "";

    [RelayCommand]
    private void TerminalFensterOeffnen()
    {
        if (!Terminal.FensterOeffnen())
        {
            Dialoge.Hinweis("Es ließ sich kein Terminalfenster öffnen.");
        }
    }

    public string AnwendungsVersion
    {
        get
        {
            var version = Assembly.GetExecutingAssembly().GetName().Version;
            var stempel = Assembly.GetExecutingAssembly()
                .GetCustomAttributes<AssemblyMetadataAttribute>()
                .FirstOrDefault(a => a.Key == "BuildTimestamp")?.Value;

            var text = "Version " + (version is null ? "?" : version.ToString(3));
            return stempel is null ? text : text + "  ·  Build " + stempel;
        }
    }

    public int AnzahlUpdates => Programme.Count(p => p.HatUpdate);

    /// <summary>Green only when every card was checked without an open update or a failed check.</summary>
    public bool AllesAktuell => Programme.Count > 0 && Programme.All(p =>
        p.Zustand is not (UpdateZustand.Unbekannt or UpdateZustand.Pruefe or UpdateZustand.UpdateVerfuegbar
            or UpdateZustand.Laeuft or UpdateZustand.Fehler));

    public bool HatUpdates => AnzahlUpdates > 0;

    private void SammelAnzeigeMelden()
    {
        OnPropertyChanged(nameof(AnzahlUpdates));
        OnPropertyChanged(nameof(UpdateZusammenfassung));
        OnPropertyChanged(nameof(AllesAktuell));
        OnPropertyChanged(nameof(HatUpdates));
        OnPropertyChanged(nameof(SicherText));
        OnPropertyChanged(nameof(CliText));
        OnPropertyChanged(nameof(HatSichereUpdates));
        OnPropertyChanged(nameof(HatCliUpdates));
    }

    public string UpdateZusammenfassung => AnzahlUpdates switch
    {
        0 => "Keine Updates offen",
        1 => "1 Update verfügbar",
        _ => AnzahlUpdates + " Updates verfügbar"
    };

    private void KatalogLaden()
    {
        foreach (var alt in Programme)
        {
            alt.PropertyChanged -= AufProgrammGeaendert;
            alt.Abmelden();
        }
        Programme.Clear();

        var (katalog, fehler) = Katalogdienst.Laden();
        KatalogFehler = fehler;
        Diagnose.Ereignis(fehler is null ? Schwere.Info : Schwere.Fehler, "katalog", "katalog.geladen",
            fehler ?? katalog.Programme.Count + " Programme geladen.", null,
            new Dictionary<string, object?>
            {
                ["anzahl"] = katalog.Programme.Count,
                ["programme"] = string.Join(",", katalog.Programme.Select(p => p.Id + ":" + p.Art)),
                ["ohneProvider"] = string.Join(",", katalog.Programme.Where(p => !_aktualisierer.ContainsKey(p.Art)).Select(p => p.Id))
            });
        var (berichte, dauern) = Protokollierung.VerlaufLesen();
        var gruppen = katalog.Programme.Select(p => p.Gruppe).Distinct(StringComparer.Ordinal).ToList();
        var index = 0;

        foreach (var eintrag in katalog.Programme)
        {
            _aktualisierer.TryGetValue(eintrag.Art, out var dienst);
            var vm = new ProgrammViewModel(eintrag, dienst, _einstellungen, _koordination)
            {
                KatalogIndex = index++,
                GruppenRang = gruppen.IndexOf(eintrag.Gruppe)
            };
            if (dauern.TryGetValue(eintrag.Id, out var gemessen)) vm.DauernSetzen(gemessen);
            vm.PropertyChanged += AufProgrammGeaendert;
            vm.Meldung += (_, text) => Dialoge.Hinweis(text);

            // The history survives restarts, so each card can show how its last run went.
            if (berichte.TryGetValue(eintrag.Id, out var bericht)) vm.LetzterBericht = bericht;

            // So does the answer of the last check -- the list is meaningful before anything runs.
            if (Pruefstand.Lesen(eintrag.Id) is { } stand) vm.StandUebernehmen(stand);

            Programme.Add(vm);
        }

        SortierungAnwenden();
        AusgewaehltesProgramm = Programme.FirstOrDefault();
        SammelAnzeigeMelden();
    }

    // ---------------- Sortierung ----------------

    /// <summary>Within each group: catalog order, or the shortest expected update first.</summary>
    public bool NachDauerSortieren
    {
        get => _einstellungen.NachDauerSortieren;
        set
        {
            if (_einstellungen.NachDauerSortieren == value) return;
            _einstellungen.NachDauerSortieren = value;
            _einstellungen.Speichern();
            SortierungAnwenden();
            OnPropertyChanged();
        }
    }

    /// <summary>The group rank comes first, otherwise WPF would split the groups apart.</summary>
    private void SortierungAnwenden()
    {
        using (Ansicht.DeferRefresh())
        {
            Ansicht.SortDescriptions.Clear();
            Ansicht.SortDescriptions.Add(new SortDescription(nameof(ProgrammViewModel.GruppenRang), ListSortDirection.Ascending));
            if (NachDauerSortieren)
                Ansicht.SortDescriptions.Add(new SortDescription(nameof(ProgrammViewModel.ErwarteteDauerSekunden), ListSortDirection.Ascending));
            Ansicht.SortDescriptions.Add(new SortDescription(nameof(ProgrammViewModel.KatalogIndex), ListSortDirection.Ascending));
        }
    }

    private void AufProgrammGeaendert(object? sender, PropertyChangedEventArgs e)
    {
        if (e.PropertyName is nameof(ProgrammViewModel.Zustand) or nameof(ProgrammViewModel.HatUpdate))
        {
            SammelAnzeigeMelden();
        }
    }

    /// <summary>
    /// Runs once after the window is up, so the list is meaningful without a first click -- and
    /// so the update buttons show "Aktuell" wherever nothing is pending.
    /// </summary>
    /// <param name="alles">
    /// The hidden start at Windows logon checks everything. A window opened by hand only checks
    /// what has no fresh answer -- after the logon check that is nothing, and the list is simply there.
    /// </param>
    public async Task ErstePruefungAsync(bool alles = false)
    {
        await ErstePruefungKernAsync(alles);

        // A batch interrupted by the self-update goes on now -- never from the hidden logon start.
        if (!alles && _einstellungen.SammelFortsetzen is not null)
        {
            try
            {
                await SammelFortsetzenAsync();
            }
            catch (Exception ex)
            {
                Diagnose.Ausnahme(ex, "sammel", "Sammellauf fortsetzen");
            }
        }
    }

    private async Task ErstePruefungKernAsync(bool alles)
    {
        await Task.Delay(400);

        // One operation for the whole startup pass -- also the scheduled-task queries below, so no
        // external command of the start runs without correlation.
        using var vorgang = Diagnose.VorgangBeginnen("startpruefung", null, "Anfangsprüfung aller Programme");

        // Which programs already start elevated through a scheduled task? Independent queries.
        await Task.WhenAll(Programme.Select(p => p.AufgabenZustandLesenAsync()));

        var grenze = DateTime.Now - StandGueltig;
        // The own tools are always asked again: that costs one shared git pull, and it is the
        // only way a version that just arrived in the repo shows up without a click.
        var faellig = alles
            ? Programme.ToList()
            : Programme.Where(p => p.StandVom is not { } zeit || zeit < grenze
                                   || p.Eintrag.Art.Equals("reposkript", StringComparison.OrdinalIgnoreCase)).ToList();
        if (faellig.Count == 0 && !Programme.Any(p => p.UebernahmeOffen))
        {
            var aeltester = Programme.Min(p => p.StandVom);
            KopfStatus = "Stand der letzten Prüfung von " + (aeltester?.ToString("HH:mm") ?? "–") + " Uhr – "
                         + UpdateZusammenfassung + ". „Alle prüfen“ fragt neu ab.";
            vorgang.Beenden("uebernommen", KopfStatus);
            _ = HintergrundSicherstellenAsync();
            return;
        }

        // The startup pass queries winget, git and the Appx registry just like a batch run, so it
        // holds the same exclusive ownership -- a click during it waits instead of running beside it.
        using var besitz = _koordination.SammelBeginnen();
        if (besitz is null)
        {
            KopfStatus = ProgrammViewModel.BelegtText;
            SammelAbgewiesen("startpruefung");
            vorgang.Beenden("abgewiesen", ProgrammViewModel.BelegtText, Schwere.Warnung);
            return;
        }

        LaeuftSammelvorgang = true;
        try
        {
            // Did a previously staged update arrive in the meantime -- or is it still hanging?
            foreach (var p in Programme.ToList()) await p.AusstehendesPruefenAsync();

            await AllePruefenMitBesitzAsync(besitz, faellig);
            vorgang.Beenden("abgeschlossen", KopfStatus);
        }
        catch (Exception ex)
        {
            Diagnose.Ausnahme(ex, "sammel", "Anfangsprüfung");
            vorgang.Beenden("Ausnahme", ex.Message, Schwere.Fehler);
            throw;
        }
        finally
        {
            LaeuftSammelvorgang = false;
            _ = HintergrundSicherstellenAsync();
        }
    }

    /// <summary>How long a stored answer spares the check at the next start.</summary>
    private static readonly TimeSpan StandGueltig = TimeSpan.FromMinutes(60);

    [ObservableProperty] private int _pruefGesamt = 1;
    [ObservableProperty] private int _pruefFertig;

    // ---------------- Prüfung beim Windows-Start ----------------

    /// <summary>Check hidden at Windows logon, so the window opened later already knows everything.</summary>
    public bool BeimStartPruefen
    {
        get => _einstellungen.HintergrundPruefung;
        set
        {
            if (_einstellungen.HintergrundPruefung == value) return;
            _einstellungen.HintergrundPruefung = value;
            _einstellungen.Speichern();
            OnPropertyChanged();
            _ = value ? HintergrundSicherstellenAsync(melden: true) : HintergrundEntfernenAsync();
        }
    }

    private const string HintergrundRunName = "UpdateZentrale-Hintergrund";

    /// <summary>
    /// Makes sure the logon start exists and points at this exe -- on every start, so it repairs
    /// itself after a moved folder or a removed task. Elevated: a scheduled task (the only way
    /// Windows starts a program marked "as administrator" at logon). Not elevated and not marked:
    /// the ordinary autostart entry.
    /// </summary>
    private async Task HintergrundSicherstellenAsync(bool melden = false)
    {
        try
        {
            if (!_einstellungen.HintergrundPruefung) return;
            var exe = Rechte.EigeneExe;
            if (string.IsNullOrWhiteSpace(exe)) return;

            if (Rechte.IstErhoeht)
            {
                if (_einstellungen.HintergrundEingerichtetFuer == "aufgabe:" + exe
                    && await Aufgabenplanung.ExistiertAsync(Aufgabenplanung.HintergrundId)) return;

                var (erfolg, ausgabe) = await Aufgabenplanung.HintergrundAnlegenAsync(exe);
                if (!erfolg)
                {
                    Diagnose.Ereignis(Schwere.Warnung, "hintergrund", "hintergrund.aufgabe", "Aufgabe nicht angelegt: " + ausgabe);
                    if (melden) Dialoge.Hinweis("Die Prüfung beim Windows-Start ließ sich nicht einrichten:\n" + ausgabe);
                    return;
                }
                Systemdienst.RunEintragEntfernen(HintergrundRunName);
                Merken("aufgabe:" + exe);
            }
            else if (!Rechte.ImmerAlsAdmin)
            {
                if (_einstellungen.HintergrundEingerichtetFuer == "run:" + exe) return;
                if (Systemdienst.RunEintragSchreiben(HintergrundRunName, "\"" + exe + "\" " + App.HintergrundSchalter))
                    Merken("run:" + exe);
            }
            else if (_einstellungen.HintergrundEingerichtetFuer != "aufgabe:" + exe)
            {
                // Marked "always as administrator" but started without: neither way is open now.
                const string text = "Die Prüfung beim Windows-Start wird eingerichtet, sobald die UpdateZentrale einmal "
                                    + "mit Administratorrechten läuft (oben: „Als Administrator neu starten“).";
                if (melden) Dialoge.Hinweis(text);
                else KopfStatus += "  ·  Hinweis: " + text;
            }
        }
        catch (Exception ex)
        {
            Diagnose.Ausnahme(ex, "hintergrund", "Prüfung beim Windows-Start einrichten", Schwere.Warnung);
        }

        void Merken(string wert)
        {
            _einstellungen.HintergrundEingerichtetFuer = wert;
            _einstellungen.Speichern();
            Diagnose.Ereignis(Schwere.Info, "hintergrund", "hintergrund.eingerichtet", wert);
        }
    }

    private async Task HintergrundEntfernenAsync()
    {
        try
        {
            Systemdienst.RunEintragEntfernen(HintergrundRunName);
            if (await Aufgabenplanung.ExistiertAsync(Aufgabenplanung.HintergrundId))
            {
                var (weg, ausgabe) = await Aufgabenplanung.EntfernenAsync(Aufgabenplanung.HintergrundId);
                if (!weg)
                {
                    Dialoge.Hinweis("Die geplante Aufgabe ließ sich nicht entfernen – dafür muss die UpdateZentrale "
                                    + "mit Administratorrechten laufen.\n" + ausgabe);
                    return;
                }
            }
            _einstellungen.HintergrundEingerichtetFuer = null;
            _einstellungen.Speichern();
        }
        catch (Exception ex)
        {
            Diagnose.Ausnahme(ex, "hintergrund", "Prüfung beim Windows-Start entfernen", Schwere.Warnung);
        }
    }

    [RelayCommand]
    private async Task AllePruefenAsync()
    {
        using var besitz = _koordination.SammelBeginnen();
        if (besitz is null)
        {
            KopfStatus = "Alle prüfen geht erst, wenn der laufende Vorgang fertig ist.";
            SammelAbgewiesen("sammelpruefung");
            return;
        }

        LaeuftSammelvorgang = true;
        using var vorgang = Diagnose.VorgangBeginnen("sammelpruefung", null, "Alle prüfen");
        try
        {
            await AllePruefenMitBesitzAsync(besitz);
            vorgang.Beenden("abgeschlossen", KopfStatus);
        }
        finally
        {
            LaeuftSammelvorgang = false;
        }
    }

    private async Task AllePruefenMitBesitzAsync(Laufbesitz besitz, IReadOnlyList<ProgrammViewModel>? nur = null)
    {
        // A snapshot: the list must not shift under the run (reload is blocked meanwhile, this is
        // the second layer).
        var liste = (nur ?? Programme).ToList();
        PruefGesamt = Math.Max(liste.Count, 1);
        PruefFertig = 0;

        // The own tools are built from ~/proggs, so the checkout is brought up to date first --
        // once for all of them, and before any card reads a version from it.
        string? repoHinweis = null;
        if (liste.Any(p => p.Eintrag.Art.Equals("reposkript", StringComparison.OrdinalIgnoreCase)))
        {
            KopfStatus = "Gleicht das Repo mit GitHub ab (git pull) …";
            var abgleich = await RepoAbgleich.AbgleichenAsync(Pfade.RepoWurzel, erzwingen: true);
            if (!abgleich.GezogenOk) repoHinweis = abgleich.Meldung;
        }

        // All at once: the checks wait on different things (winget, the registries, git, lms).
        // Only winget itself is queued, inside its provider. One after the other the same pass
        // took five minutes; the slowest single check now sets the pace.
        var offen = liste.Select(p => p.Name).ToList();
        void Anzeigen() => KopfStatus = "Prüft … " + PruefFertig + " von " + liste.Count + " fertig"
                                        + (offen.Count == 0 ? "" : " – läuft noch: " + string.Join(", ", offen.Take(4))
                                                                   + (offen.Count > 4 ? " …" : ""));
        async Task EinerAsync(ProgrammViewModel p)
        {
            try
            {
                await p.PruefenImSammelAsync(besitz);
            }
            finally
            {
                offen.Remove(p.Name);
                PruefFertig++;
                Anzeigen();
            }
        }

        Anzeigen();
        await Task.WhenAll(liste.Select(EinerAsync));

        KopfStatus = "Prüfung abgeschlossen – " + UpdateZusammenfassung + "."
                     + (repoHinweis is null ? "" : "  ·  " + repoHinweis);
    }

    /// <summary>Open updates that cannot end a CLI session, in batch order (own tools first).</summary>
    private List<Sammelplan.Schritt> SichererPlan
        => Sammelplan.Planen(Programme.Where(p => p.HatUpdate)).Where(s => s.Phase != Sammelplan.PhaseCli).ToList();

    /// <summary>Open updates that can end a running CLI session.</summary>
    private List<Sammelplan.Schritt> CliPlan
        => Sammelplan.Planen(Programme.Where(p => p.HatUpdate)).Where(s => s.Phase == Sammelplan.PhaseCli).ToList();

    /// <summary>Label of the button for everything that is not CLI-critical.</summary>
    public string SicherText
    {
        get
        {
            var n = SichererPlan.Count;
            if (n == 0) return AllesAktuell ? "✔ Alles aktuell" : "Keine unkritischen Updates";
            var nurKlick = Programme.Count(p => p.HatUpdate && p.IstHauptversionsSprung);
            return (n == 1 ? "1 Update installieren" : $"{n} Updates installieren") + "  ·  nicht CLI-kritisch"
                   + (nurKlick > 0 ? $"  ·  {nurKlick} nur per Klick" : "");
        }
    }

    /// <summary>Label of the button for the CLI-critical updates.</summary>
    public string CliText
    {
        get
        {
            var n = CliPlan.Count;
            return n == 0 ? "Keine CLI-kritischen Updates" : (n == 1 ? "1 CLI-kritisches Update" : $"{n} CLI-kritische Updates") + " installieren";
        }
    }

    public bool HatSichereUpdates => SichererPlan.Count > 0;
    public bool HatCliUpdates => CliPlan.Count > 0;

    /// <summary>
    /// Two independent buttons: everything that cannot touch a CLI session, and the CLI-critical
    /// updates. Either may go first -- neither waits for the other.
    /// </summary>
    [RelayCommand]
    private Task SichereAktualisierenAsync() => SammelStartenAsync(cli: false);

    [RelayCommand]
    private Task CliAktualisierenAsync() => SammelStartenAsync(cli: true);

    private async Task SammelStartenAsync(bool cli)
    {
        if (_koordination.Belegt)
        {
            KopfStatus = "Updates gehen erst, wenn der laufende Vorgang fertig ist.";
            SammelAbgewiesen("sammelupdate");
            return;
        }

        var plan = cli ? CliPlan : SichererPlan;
        if (plan.Count == 0)
        {
            Dialoge.Hinweis(cli
                ? "Es ist kein CLI-kritisches Update offen."
                : "Es ist kein unkritisches Update offen, das gesammelt installiert werden kann. Prüfe zuerst, oder "
                  + "aktualisiere einzelne Programme gezielt (Hauptversionssprünge nur per Klick auf der Karte).");
            return;
        }

        var nurKlick = Programme.Where(p => p.HatUpdate && p.IstHauptversionsSprung).ToList();
        var liste = string.Join("\n", plan.Select((s, i) => $"  {i + 1}. {s.Programm.Name}  ({Sammelplan.PhasenName(s.Phase)}, "
                                                           + Protokollierung.DauerText(s.Programm.ErwarteteDauerSekunden) + ")"));
        var frage = cli
            ? "Diese Updates können laufende CLI-Sitzungen beenden:\n\n"
              + string.Join("\n", plan.Select((s, i) => $"  {i + 1}. {s.Programm.Name} – {s.Programm.Eintrag.CliRisiko}"))
              + "\n\nSchließe vorher Claude Code, Codex, OpenCode und alle Terminalfenster, dann „Ja“."
              + "\n\nBei laufenden Programmen wird vorher nachgefragt."
            : "Reihenfolge der Updates (keines davon ist CLI-kritisch):\n\n" + liste
              + (nurKlick.Count > 0
                  ? "\n\nNur per Klick auf der Karte (Hauptversion): " + string.Join(", ", nurKlick.Select(p => p.Name))
                  : "")
              + "\n\nBei laufenden Programmen wird vorher nachgefragt.";
        if (!Dialoge.Fragen(frage, cli ? "CLI-kritische Updates installieren?" : "Unkritische Updates installieren?")) return;

        // The CLI button's question already was "close the CLIs now" -- no second one inside the run.
        await SammelAusfuehrenAsync(plan, "sammel-" + (cli ? "cli-" : "") + DateTime.Now.ToString("yyyyMMdd-HHmmss"), cliBestaetigt: cli);
    }

    /// <summary>
    /// Runs a batch plan inside one exclusive ownership. The UpdateZentrale itself goes first and
    /// alone: its script restarts the app, so the rest is stored and continued by the new instance.
    /// Before the CLI-critical phase the batch stops once and asks -- the one moment the CLIs must
    /// be closed. The phases run one after the other; within a phase, entries marked "parallel"
    /// run in a second lane beside the serial one (winget, npm, scripts).
    /// </summary>
    /// <param name="versatz">Steps already done before a restart: positions continue the original plan.</param>
    /// <param name="planGroesse">Size of the original plan (0 = this plan).</param>
    /// <param name="cliBestaetigt">The user just confirmed the CLI-critical updates -- the phase does not ask again.</param>
    private async Task SammelAusfuehrenAsync(List<Sammelplan.Schritt> plan, string sammelId, int versatz = 0, int planGroesse = 0,
                                             bool cliBestaetigt = false)
    {
        // Acquired only now: the question before is modal, and whatever started meanwhile wins.
        using var besitz = _koordination.SammelBeginnen();
        if (besitz is null)
        {
            KopfStatus = "Alle Updates gehen erst, wenn der laufende Vorgang fertig ist.";
            SammelAbgewiesen("sammelupdate");
            return;
        }

        var gesamtZahl = Math.Max(planGroesse, plan.Count + versatz);
        LaeuftSammelvorgang = true;
        var gesamt = System.Diagnostics.Stopwatch.StartNew();
        var laeufe = new List<SammelLauf>();
        // All continuations run on the UI thread (WPF context), so these lists need no locks.
        var laufend = new List<(string Name, string Zeile)>();
        var fertig = 0;
        var cliHinweis = "";
        PruefGesamt = Math.Max(plan.Count, 1);
        PruefFertig = 0;
        using var vorgang = Diagnose.VorgangBeginnen("sammelupdate", null, "Alle Updates: " + string.Join(", ", plan.Select(s => s.Programm.Eintrag.Id)));
        Protokollierung.SammellaufSchreiben(new
        {
            sammelId, ereignis = versatz == 0 ? "plan" : "fortsetzung", zeit = DateTime.Now, versatz, planGroesse = gesamtZahl,
            plan = plan.Select((s, i) => new { position = versatz + i + 1, id = s.Programm.Eintrag.Id, phase = s.Phase, parallel = s.Parallel,
                                               erwartetSekunden = Math.Round(s.Programm.ErwarteteDauerSekunden), gemessen = s.Programm.DauerGemessen })
        });

        void Anzeigen()
        {
            PruefFertig = fertig;
            KopfStatus = "Alle Updates: " + (versatz + fertig) + " von " + gesamtZahl + " fertig, " + laufend.Count + " laufen gerade"
                         + " – seit " + Protokollierung.DauerText(gesamt.Elapsed.TotalSeconds) + "." + cliHinweis;
            LaufDetail = string.Join("\n", laufend.Select(l => l.Zeile));
        }

        void Abschluss(string ereignis) => Protokollierung.SammellaufSchreiben(new
        {
            sammelId, ereignis, zeit = DateTime.Now, gesamtSekunden = Math.Round(gesamt.Elapsed.TotalSeconds, 1), laeufe = laeufe.ToList()
        });

        async Task<UpdateBericht?> EinerAsync(Sammelplan.Schritt s)
        {
            var position = versatz + plan.IndexOf(s) + 1;
            laufend.Add((s.Programm.Name, "▶ " + s.Programm.Name + "  ·  Position " + position + " von " + gesamtZahl
                                          + "  ·  " + Sammelplan.PhasenName(s.Phase) + (s.Parallel ? " (parallel)" : "")
                                          + "  ·  gestartet " + DateTime.Now.ToString("HH:mm:ss")
                                          + "  ·  erwartet ≈ " + Protokollierung.DauerText(s.Programm.ErwarteteDauerSekunden)));
            Anzeigen();
            var uhr = System.Diagnostics.Stopwatch.StartNew();
            var beginn = DateTime.Now;
            UpdateBericht? bericht = null;
            try
            {
                bericht = await s.Programm.AktualisierenImSammelAsync(besitz, new SammelKontext(sammelId, s.Phase, position));
                return bericht;
            }
            finally
            {
                laufend.RemoveAll(l => l.Name == s.Programm.Name);
                fertig++;
                Anzeigen();
                laeufe.Add(new SammelLauf(position, s.Programm.Eintrag.Id, s.Phase, s.Parallel, beginn, DateTime.Now,
                                          Math.Round(uhr.Elapsed.TotalSeconds, 1), bericht?.ErgebnisText ?? "ohne Bericht",
                                          Math.Round(s.Programm.ErwarteteDauerSekunden)));
            }
        }

        var ende = "abgeschlossen";
        try
        {
            var rest = plan;
            var selbst = plan.FirstOrDefault(s => s.Programm.Eintrag.Id.Equals(Sammelplan.SelbstId, StringComparison.OrdinalIgnoreCase));
            if (selbst is not null)
            {
                rest = plan.Where(s => s != selbst).ToList();
                // Stored BEFORE the run: the script's helper ends this process shortly after "started".
                _einstellungen.SammelFortsetzen = rest.Count == 0 ? null
                    : new SammelFortsetzung
                    {
                        SammelId = sammelId, Zeit = DateTime.Now, Versatz = versatz + 1, PlanGroesse = gesamtZahl,
                        ProgrammIds = rest.Select(s => s.Programm.Eintrag.Id).ToList()
                    };
                _einstellungen.Speichern();
                AusgewaehltesProgramm = selbst.Programm;
                var bericht = await EinerAsync(selbst);

                // Only a real "started" restarts the app: the self-update is reported as pending
                // (the new build is copied in after this process ends). Anything else -- no, no
                // answer, failure, nothing to do -- carries on here at once.
                if (bericht is { Ergebnis: LaufErgebnis.Ausstehend })
                {
                    ende = "selbstupdate";
                    Abschluss("selbstupdate-neustart");   // written now: this process will not reach the finally
                    KopfStatus = rest.Count == 0
                        ? "Die UpdateZentrale startet in der neuen Fassung neu …"
                        : "Die UpdateZentrale startet neu und setzt die übrigen " + rest.Count + " Updates danach fort …";
                    LaufDetail = "";
                    await Task.Delay(TimeSpan.FromSeconds(90));
                    // Still alive: the restart did not happen -- this instance carries on itself.
                    ende = "abgeschlossen";
                    Diagnose.Ereignis(Schwere.Warnung, "sammel", "sammel.kein_neustart", "Kein Neustart nach dem Selbst-Update – der Sammellauf geht hier weiter.", sammelId);
                }
                _einstellungen.SammelFortsetzen = null;
                _einstellungen.Speichern();
            }

            foreach (var phase in rest.GroupBy(s => s.Phase).OrderBy(g => g.Key))
            {
                var schritte = phase.ToList();

                // The one moment the CLIs have to be closed: right before the updates that can end
                // their sessions. "No" leaves them open for a later click; the rest goes on.
                if (phase.Key == Sammelplan.PhaseCli && !cliBestaetigt)
                {
                    LaufDetail = "";
                    var frage = "Jetzt kommen die CLI-kritischen Updates:\n\n"
                                + string.Join("\n", schritte.Select(s => "  • " + s.Programm.Name + " – " + s.Programm.Eintrag.CliRisiko))
                                + "\n\nSchließe jetzt Claude Code, Codex, OpenCode und alle Terminalfenster und klicke dann auf „Ja“."
                                + "\n„Nein“ überspringt sie (sie bleiben offen); die übrigen Updates laufen weiter.";
                    if (!Dialoge.Fragen(frage, "CLI-kritische Updates jetzt installieren?"))
                    {
                        Diagnose.Ereignis(Schwere.Info, "sammel", "sammel.cli_uebersprungen", schritte.Count + " CLI-kritische Updates übersprungen.", sammelId);
                        Protokollierung.SammellaufSchreiben(new { sammelId, ereignis = "cli-uebersprungen", zeit = DateTime.Now, ids = schritte.Select(s => s.Programm.Eintrag.Id) });
                        fertig += schritte.Count;
                        cliHinweis = "  ·  CLI-kritische Updates übersprungen – sie bleiben offen.";
                        Anzeigen();
                        continue;
                    }
                }

                var seriell = schritte.Where(s => !s.Parallel).ToList();
                var parallel = schritte.Where(s => s.Parallel).ToList();

                async Task SeriellAsync()
                {
                    foreach (var s in seriell)
                    {
                        AusgewaehltesProgramm = s.Programm;
                        await EinerAsync(s);
                    }
                }

                async Task ParallelAsync()
                {
                    using var bahn = new SemaphoreSlim(3);
                    await Task.WhenAll(parallel.Select(async s =>
                    {
                        await bahn.WaitAsync();
                        try { await EinerAsync(s); }
                        finally { bahn.Release(); }
                    }));
                }

                await Task.WhenAll(SeriellAsync(), ParallelAsync());

                if (phase.Key == Sammelplan.PhaseCli && rest.Any(s => s.Phase > Sammelplan.PhaseCli))
                {
                    cliHinweis = "  ✔ CLI-kritische Updates sind durch – du kannst im CLI weiterarbeiten.";
                    Anzeigen();
                    Diagnose.Ereignis(Schwere.Info, "sammel", "sammel.cli_fertig", KopfStatus, sammelId);
                }
            }

            var fehler = laeufe.Count(l => l.Ergebnis is "Fehlgeschlagen" or "Nicht verifiziert" or "ohne Bericht");
            KopfStatus = "Alle Updates sind durchgelaufen (" + Protokollierung.DauerText(gesamt.Elapsed.TotalSeconds) + ")"
                         + (fehler > 0 ? " – " + fehler + " mit Problemen, siehe rote Karten." : ".")
                         + (cliHinweis.Contains("übersprungen") ? cliHinweis : "");
            vorgang.Beenden("abgeschlossen", KopfStatus);
        }
        finally
        {
            Abschluss(ende);
            LaeuftSammelvorgang = false;
            LaufDetail = "";
            SortierungAnwenden();
        }
    }

    /// <summary>
    /// After the UpdateZentrale updated itself inside "install all", the new instance continues
    /// the stored rest -- once, and only if it is fresh. The user agreed to the whole batch already;
    /// the CLI-critical phase still asks its own question.
    /// </summary>
    private async Task SammelFortsetzenAsync()
    {
        var offen = _einstellungen.SammelFortsetzen;
        if (offen is null) return;
        _einstellungen.SammelFortsetzen = null;
        _einstellungen.Speichern();
        if (DateTime.Now - offen.Zeit > TimeSpan.FromMinutes(30)) return;

        var plan = Sammelplan.Planen(Programme.Where(p => p.HatUpdate && offen.ProgrammIds.Contains(p.Eintrag.Id, StringComparer.OrdinalIgnoreCase)
                                                          && !p.Eintrag.Id.Equals(Sammelplan.SelbstId, StringComparison.OrdinalIgnoreCase)));
        Diagnose.Ereignis(Schwere.Info, "sammel", "sammel.fortsetzung", plan.Count + " Updates nach dem Selbst-Update fortgesetzt.", offen.SammelId);
        if (plan.Count > 0) await SammelAusfuehrenAsync(plan, offen.SammelId, offen.Versatz, offen.PlanGroesse);
    }

    /// <summary>Batch buttons and "reload" are only usable while nothing at all is running.</summary>
    public bool SammelMoeglich => !_koordination.Belegt;

    private void AufKoordinationGeaendert(object? sender, EventArgs e)
    {
        var dispatcher = System.Windows.Application.Current?.Dispatcher;
        if (dispatcher is null || dispatcher.CheckAccess()) OnPropertyChanged(nameof(SammelMoeglich));
        else dispatcher.BeginInvoke(() => OnPropertyChanged(nameof(SammelMoeglich)));
    }

    [RelayCommand]
    private void KatalogNeuLaden()
    {
        // Reloading throws the cards away. A card whose update is still running would vanish
        // mid-run -- its result never shown, and a second run of the same installer possible
        // from the fresh card.
        if (_koordination.Belegt || LaeuftSammelvorgang || Programme.Any(p => p.IstBeschaeftigt))
        {
            KopfStatus = "Neu laden geht erst, wenn alle laufenden Prüfungen und Updates fertig sind.";
            SammelAbgewiesen("katalog-neu-laden");
            return;
        }

        KatalogLaden();
        KopfStatus = "Katalog neu geladen – " + Programme.Count + " Programme.";
    }

    /// <summary>Opens programs.json so a new program can be added without touching the app.</summary>
    [RelayCommand]
    private void KatalogOeffnen()
    {
        try
        {
            using var _ = Process.Start(new ProcessStartInfo { FileName = Pfade.KatalogDatei, UseShellExecute = true });
        }
        catch (Exception ex)
        {
            Diagnose.Ausnahme(ex, "ui", "Katalog öffnen");
            Dialoge.Hinweis("Der Katalog ließ sich nicht öffnen: " + ex.Message);
        }
    }

    /// <summary>One program of a batch as written to sammellaeufe.jsonl.</summary>
    private sealed record SammelLauf(int Position, string Id, int Phase, bool Parallel, DateTime Beginn, DateTime Ende,
                                     double Sekunden, string Ergebnis, double ErwartetSekunden);

    private static void SammelAbgewiesen(string art)
        => Diagnose.Ereignis(Schwere.Warnung, "sammel", "start.abgewiesen", art + " abgewiesen: es läuft bereits ein Vorgang.", art);

    /// <summary>A diagnostics problem must be visible, not only in a file nobody reads.</summary>
    private void AufDiagnoseWarnung(string text)
    {
        var dispatcher = System.Windows.Application.Current?.Dispatcher;
        if (dispatcher is null || dispatcher.CheckAccess()) KopfStatus = "Diagnose-Warnung: " + text;
        else dispatcher.BeginInvoke(() => KopfStatus = "Diagnose-Warnung: " + text);
    }

    /// <summary>Opens the folder with daily logs, structured diagnostics and the update history.</summary>
    [RelayCommand]
    private void DiagnoseOeffnen()
    {
        if (!Protokollierung.OrdnerOeffnen())
            Dialoge.Hinweis("Der Diagnoseordner ließ sich nicht öffnen:\n" + Protokollierung.Ordner
                            + "\n\nDer Grund steht im Diagnoseprotokoll.");
    }

    [ObservableProperty] private bool _exportLaeuft;

    /// <summary>
    /// Builds the masked diagnostics ZIP off the UI thread, then shows it selected in Explorer.
    /// A failure is shown and recorded; it never touches a running update.
    /// </summary>
    [RelayCommand]
    private async Task DiagnoseExportierenAsync()
    {
        if (ExportLaeuft) return;
        ExportLaeuft = true;
        using var vorgang = Diagnose.VorgangBeginnen("diagnose-export", null, "Diagnosepaket erstellen");
        try
        {
            var katalog = Programme.Select(p => p.Eintrag).ToList();
            var laufzeit = Diagnose.Laufzeitinfo();
            var pfad = await Task.Run(() => DiagnoseExport.Erstellen(Protokollierung.Ordner, DiagnoseExport.ExportOrdner,
                DateTime.Now, katalog, laufzeit));
            vorgang.Beenden("erstellt", pfad);
            KopfStatus = "Diagnosepaket erstellt: " + pfad;
            try
            {
                using var _ = Process.Start(new ProcessStartInfo("explorer.exe", "/select,\"" + pfad + "\"") { UseShellExecute = true });
            }
            catch (Exception ex)
            {
                Diagnose.Ausnahme(ex, "ui", "Explorer für das Diagnosepaket", Schwere.Warnung);
                Dialoge.Hinweis("Das Diagnosepaket liegt hier:\n" + pfad);
            }
        }
        catch (Exception ex)
        {
            Diagnose.Ausnahme(ex, "ui", "Diagnosepaket erstellen");
            vorgang.Beenden("Ausnahme", ex.Message, Schwere.Fehler);
            Dialoge.Hinweis("Das Diagnosepaket ließ sich nicht erstellen:\n" + ex.Message);
        }
        finally
        {
            ExportLaeuft = false;
        }
    }

    [RelayCommand]
    private void ZustaendeAuffrischen()
    {
        foreach (var p in Programme) p.ZustandAktualisieren();
        KopfStatus = "Laufende Programme und Autostart wurden neu eingelesen.";
    }
}
