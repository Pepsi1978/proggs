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
            ["msstore"] = new MsStoreAktualisierer(),
            ["reposkript"] = new RepoSkriptAktualisierer()
        };

    public HauptViewModel()
    {
        Programme = new ObservableCollection<ProgrammViewModel>();
        Ansicht = CollectionViewSource.GetDefaultView(Programme);
        Ansicht.GroupDescriptions.Add(new PropertyGroupDescription(nameof(ProgrammViewModel.Gruppe)));

        KatalogLaden();
    }

    public ObservableCollection<ProgrammViewModel> Programme { get; }

    public ICollectionView Ansicht { get; }

    [ObservableProperty] private ProgrammViewModel? _ausgewaehltesProgramm;
    [ObservableProperty] private string _kopfStatus = "Bereit.";
    [ObservableProperty] private bool _laeuftSammelvorgang;
    [ObservableProperty] private string? _katalogFehler;

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
        1 => "1 Update verfuegbar",
        _ => AnzahlUpdates + " Updates verfuegbar"
    };

    private void KatalogLaden()
    {
        foreach (var alt in Programme) alt.PropertyChanged -= AufProgrammGeaendert;
        Programme.Clear();

        var (katalog, fehler) = Katalogdienst.Laden();
        KatalogFehler = fehler;

        foreach (var eintrag in katalog.Programme)
        {
            _aktualisierer.TryGetValue(eintrag.Art, out var dienst);
            var vm = new ProgrammViewModel(eintrag, dienst, _einstellungen);
            vm.PropertyChanged += AufProgrammGeaendert;
            vm.Meldung += (_, text) => Dialoge.Hinweis(text);
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

    [RelayCommand]
    private async Task AllePruefenAsync()
    {
        if (LaeuftSammelvorgang) return;
        LaeuftSammelvorgang = true;
        try
        {
            // Sequential on purpose: winget serialises its source access anyway, and a parallel
            // burst makes the log unreadable.
            var gesamt = Programme.Count;
            for (var i = 0; i < gesamt; i++)
            {
                var p = Programme[i];
                KopfStatus = "Pruefe " + p.Name + " (" + (i + 1) + "/" + gesamt + ") …";
                await p.PruefenCommand.ExecuteAsync(null);
            }
            KopfStatus = "Pruefung abgeschlossen – " + UpdateZusammenfassung + ".";
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
            Dialoge.Hinweis("Es ist kein Update offen. Pruefe zuerst, oder aktualisiere einzelne Programme gezielt.");
            return;
        }

        var liste = string.Join("\n", offen.Select(p => "  • " + p.Name));
        if (!Dialoge.Fragen("Diese Programme werden jetzt aktualisiert:\n\n" + liste
                            + "\n\nLaufende Programme werden vorher abgefragt.", "Alle Updates installieren?"))
        {
            return;
        }

        LaeuftSammelvorgang = true;
        try
        {
            for (var i = 0; i < offen.Count; i++)
            {
                KopfStatus = "Aktualisiere " + offen[i].Name + " (" + (i + 1) + "/" + offen.Count + ") …";
                AusgewaehltesProgramm = offen[i];
                await offen[i].AktualisierenCommand.ExecuteAsync(null);
            }
            KopfStatus = "Alle Updates durchgelaufen.";
        }
        finally
        {
            LaeuftSammelvorgang = false;
        }
    }

    [RelayCommand]
    private void KatalogNeuLaden()
    {
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
            Dialoge.Hinweis("Katalog liess sich nicht oeffnen: " + ex.Message);
        }
    }

    [RelayCommand]
    private void ZustaendeAuffrischen()
    {
        foreach (var p in Programme) p.ZustandAktualisieren();
        KopfStatus = "Laufende Programme und Autostart neu eingelesen.";
    }
}
