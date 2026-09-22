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
        var berichte = Protokollierung.LetzteBerichte();

        foreach (var eintrag in katalog.Programme)
        {
            _aktualisierer.TryGetValue(eintrag.Art, out var dienst);
            var vm = new ProgrammViewModel(eintrag, dienst, _einstellungen, _koordination);
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

        // One operation for the whole startup pass -- also the scheduled-task queries below, so no
        // external command of the start runs without correlation.
        using var vorgang = Diagnose.VorgangBeginnen("startpruefung", null, "Anfangsprüfung aller Programme");

        // Which programs already start elevated through a scheduled task?
        foreach (var p in Programme) await p.AufgabenZustandLesenAsync();

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

            await AllePruefenMitBesitzAsync(besitz);
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

    private async Task AllePruefenMitBesitzAsync(Laufbesitz besitz)
    {
        // Sequential on purpose: winget serialises its source access anyway, and a parallel
        // burst makes the log unreadable. A snapshot: the list must not shift under the loop
        // (reload is blocked meanwhile, this is the second layer).
        var liste = Programme.ToList();
        var gesamt = liste.Count;
        for (var i = 0; i < gesamt; i++)
        {
            var p = liste[i];
            KopfStatus = "Prüft " + p.Name + " (" + (i + 1) + " von " + gesamt + ") …";
            await p.PruefenImSammelAsync(besitz);
        }
        KopfStatus = "Prüfung abgeschlossen – " + UpdateZusammenfassung + ".";
    }

    [RelayCommand]
    private async Task AlleAktualisierenAsync()
    {
        if (_koordination.Belegt)
        {
            KopfStatus = "Alle Updates gehen erst, wenn der laufende Vorgang fertig ist.";
            SammelAbgewiesen("sammelupdate");
            return;
        }

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

        // Acquired only now: the question above is modal, and whatever started meanwhile wins.
        using var besitz = _koordination.SammelBeginnen();
        if (besitz is null)
        {
            KopfStatus = "Alle Updates gehen erst, wenn der laufende Vorgang fertig ist.";
            SammelAbgewiesen("sammelupdate");
            return;
        }

        LaeuftSammelvorgang = true;
        using var vorgang = Diagnose.VorgangBeginnen("sammelupdate", null, "Alle Updates: " + string.Join(", ", offen.Select(p => p.Eintrag.Id)));
        try
        {
            for (var i = 0; i < offen.Count; i++)
            {
                KopfStatus = "Aktualisiert " + offen[i].Name + " (" + (i + 1) + " von " + offen.Count + ") …";
                AusgewaehltesProgramm = offen[i];
                await offen[i].AktualisierenImSammelAsync(besitz);
            }
            KopfStatus = "Alle Updates sind durchgelaufen.";
            vorgang.Beenden("abgeschlossen", KopfStatus);
        }
        finally
        {
            LaeuftSammelvorgang = false;
        }
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
