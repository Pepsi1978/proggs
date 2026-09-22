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
            ["reposkript"] = new RepoSkriptAktualisierer()
        };

    public HauptViewModel()
    {
        Programme = new ObservableCollection<ProgrammViewModel>();
        Ansicht = CollectionViewSource.GetDefaultView(Programme);
        Ansicht.GroupDescriptions.Add(new PropertyGroupDescription(nameof(ProgrammViewModel.Gruppe)));

        _hellModus = _einstellungen.HellModus;
        Darstellung.Anwenden(_hellModus);

        // The updater is meant to be able to install anything, so it marks itself to always run
        // elevated on the first launch. The switch in the footer turns that off again.
        if (!_einstellungen.AdminStartGesetzt)
        {
            Rechte.ImmerAlsAdminSetzen(true);
            _einstellungen.AdminStartGesetzt = true;
            _einstellungen.Speichern();
        }
        _immerAlsAdmin = Rechte.ImmerAlsAdmin;

        KatalogLaden();
    }

    public ObservableCollection<ProgrammViewModel> Programme { get; }

    public ICollectionView Ansicht { get; }

    [ObservableProperty] private ProgrammViewModel? _ausgewaehltesProgramm;
    [ObservableProperty] private string _kopfStatus = "Bereit.";
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

        try
        {
            var ausgabe = await Terminal.AusfuehrenAsync(befehl);
            TerminalAusgabe += ausgabe;
        }
        catch (Exception ex)
        {
            TerminalAusgabe += "[Fehler] " + ex.Message;
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

    public string UpdateZusammenfassung => AnzahlUpdates switch
    {
        0 => "Keine Updates offen",
        1 => "1 Update verfügbar",
        _ => AnzahlUpdates + " Updates verfügbar"
    };

    private void KatalogLaden()
    {
        foreach (var alt in Programme) alt.PropertyChanged -= AufProgrammGeaendert;
        Programme.Clear();

        var (katalog, fehler) = Katalogdienst.Laden();
        KatalogFehler = fehler;
        var berichte = Protokollierung.LetzteBerichte();

        foreach (var eintrag in katalog.Programme)
        {
            _aktualisierer.TryGetValue(eintrag.Art, out var dienst);
            var vm = new ProgrammViewModel(eintrag, dienst, _einstellungen);
            vm.PropertyChanged += AufProgrammGeaendert;
            vm.Meldung += (_, text) => Dialoge.Hinweis(text);

            // The history survives restarts, so each card can show how its last run went.
            if (berichte.TryGetValue(eintrag.Id, out var bericht)) vm.LetzterBericht = bericht;

            Programme.Add(vm);
        }

        AusgewaehltesProgramm = Programme.FirstOrDefault();
        OnPropertyChanged(nameof(AnzahlUpdates));
        OnPropertyChanged(nameof(UpdateZusammenfassung));
    }

    private void AufProgrammGeaendert(object? sender, PropertyChangedEventArgs e)
    {
        if (e.PropertyName is nameof(ProgrammViewModel.Zustand) or nameof(ProgrammViewModel.HatUpdate))
        {
            OnPropertyChanged(nameof(AnzahlUpdates));
            OnPropertyChanged(nameof(UpdateZusammenfassung));
        }
    }

    /// <summary>
    /// Runs once after the window is up, so the list is meaningful without a first click -- and
    /// so the update buttons show "Aktuell" wherever nothing is pending.
    /// </summary>
    public async Task ErstePruefungAsync()
    {
        await Task.Delay(400);

        // Which programs already start elevated through a scheduled task?
        foreach (var p in Programme) await p.AufgabenZustandLesenAsync();

        // Did a previously staged update arrive in the meantime -- or is it still hanging?
        foreach (var p in Programme) await p.AusstehendesPruefenAsync();

        await AllePruefenAsync();
    }

    [RelayCommand]
    private async Task AllePruefenAsync()
    {
        if (LaeuftSammelvorgang) return;
        LaeuftSammelvorgang = true;
        try
        {
            // Sequential on purpose: winget serialises its source access anyway, and a parallel
            // burst makes the log unreadable.
            // A snapshot: the list must not shift under the loop (reload is blocked meanwhile,
            // this is the second layer).
            var liste = Programme.ToList();
            var gesamt = liste.Count;
            for (var i = 0; i < gesamt; i++)
            {
                var p = liste[i];
                KopfStatus = "Prüft " + p.Name + " (" + (i + 1) + " von " + gesamt + ") …";
                await p.PruefenCommand.ExecuteAsync(null);
            }
            KopfStatus = "Prüfung abgeschlossen – " + UpdateZusammenfassung + ".";
        }
        finally
        {
            LaeuftSammelvorgang = false;
        }
    }

    [RelayCommand]
    private async Task AlleAktualisierenAsync()
    {
        if (LaeuftSammelvorgang) return;

        var offen = Programme.Where(p => p.HatUpdate).ToList();
        if (offen.Count == 0)
        {
            Dialoge.Hinweis("Es ist kein Update offen. Prüfe zuerst, oder aktualisiere einzelne Programme gezielt.");
            return;
        }

        var liste = string.Join("\n", offen.Select(p => "  • " + p.Name));
        if (!Dialoge.Fragen("Diese Programme werden jetzt aktualisiert:\n\n" + liste
                            + "\n\nBei laufenden Programmen wird vorher nachgefragt.", "Alle Updates installieren?"))
        {
            return;
        }

        LaeuftSammelvorgang = true;
        try
        {
            for (var i = 0; i < offen.Count; i++)
            {
                KopfStatus = "Aktualisiert " + offen[i].Name + " (" + (i + 1) + " von " + offen.Count + ") …";
                AusgewaehltesProgramm = offen[i];
                await offen[i].AktualisierenCommand.ExecuteAsync(null);
            }
            KopfStatus = "Alle Updates sind durchgelaufen.";
        }
        finally
        {
            LaeuftSammelvorgang = false;
        }
    }

    [RelayCommand]
    private void KatalogNeuLaden()
    {
        // Reloading throws the cards away. A card whose update is still running would vanish
        // mid-run -- its result never shown, and a second run of the same installer possible
        // from the fresh card.
        if (LaeuftSammelvorgang || Programme.Any(p => p.IstBeschaeftigt))
        {
            KopfStatus = "Neu laden geht erst, wenn alle laufenden Prüfungen und Updates fertig sind.";
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
            Process.Start(new ProcessStartInfo { FileName = Pfade.KatalogDatei, UseShellExecute = true });
        }
        catch (Exception ex)
        {
            Dialoge.Hinweis("Der Katalog ließ sich nicht öffnen: " + ex.Message);
        }
    }

    /// <summary>Opens the log folder; every run is recorded there, day by day.</summary>
    [RelayCommand]
    private void ProtokolleOeffnen() => Protokollierung.OrdnerOeffnen();

    [RelayCommand]
    private void ZustaendeAuffrischen()
    {
        foreach (var p in Programme) p.ZustandAktualisieren();
        KopfStatus = "Laufende Programme und Autostart wurden neu eingelesen.";
    }
}
