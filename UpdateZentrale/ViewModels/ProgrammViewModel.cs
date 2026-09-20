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

    public ProgrammViewModel(ProgrammEintrag eintrag, IAktualisierer? aktualisierer, Einstellungen einstellungen)
    {
        Eintrag = eintrag;
        _aktualisierer = aktualisierer;
        _einstellungen = einstellungen;

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
                                  && Zustand != UpdateZustand.Aktuell;

    public string AktionsText => Zustand switch
    {
        UpdateZustand.Aktuell => "Aktuell",
        UpdateZustand.UpdateVerfuegbar => "Aktualisieren",
        UpdateZustand.Pruefe => "Prüft …",
        UpdateZustand.NichtInstalliert => "Nicht installiert",
        _ => "Aktualisieren"
    };

    /// <summary>Only a real update gets the accent button; everything else stays calm.</summary>
    public bool AktionBetont => Zustand == UpdateZustand.UpdateVerfuegbar;

    public bool KannPruefen => _aktualisierer is not null && !IstBeschaeftigt;

    public string VersionsText => string.IsNullOrWhiteSpace(InstallierteVersion)
        ? "–"
        : string.IsNullOrWhiteSpace(VerfuegbareVersion) || VerfuegbareVersion == InstallierteVersion
            ? InstallierteVersion
            : InstallierteVersion + "   →   " + VerfuegbareVersion;

    public event EventHandler<string>? Meldung;

    [ObservableProperty] private bool _autostartAlsAufgabe;

    /// <summary>
    /// Only worth showing where the conflict actually exists: the program starts elevated and has
    /// (or had) an autostart entry that Windows would now skip.
    /// </summary>
    public bool AutostartUmstellbar => KannStarten && (ImAutostart || AutostartAlsAufgabe) && AlsAdministrator;

    public void ZustandAktualisieren()
    {
        Laeuft = Prozessdienst.Laeuft(Eintrag);
        ImAutostart = Systemdienst.ImAutostart(Eintrag);
        AdminWarnung = Systemdienst.AdminBrichtAutostart(Eintrag, _alsAdministrator) && !AutostartAlsAufgabe;
        OnPropertyChanged(nameof(AutostartUmstellbar));
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
                befehlszeile = "\"" + Pfade.Aufloesen(Eintrag.ExePfad) + "\"";
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
    private async Task PruefenAsync()
    {
        if (_aktualisierer is null) return;
        await LaufAsync(async (fortschritt, abbruch) =>
        {
            StatusText = "Wird geprüft …";
            Zustand = UpdateZustand.Pruefe;
            return await _aktualisierer.PruefenAsync(Eintrag, fortschritt, abbruch);
        });
    }

    [RelayCommand]
    private async Task AktualisierenAsync()
    {
        if (_aktualisierer is null) return;

        // Electron/NSIS installers hang silently while the app is running, so the helper processes
        // go down too -- but only after the user agreed.
        if (Eintrag.BeendenVorUpdate && Prozessdienst.Laeuft(Eintrag))
        {
            var laufende = Prozessdienst.Laufende(Eintrag).Count;
            var frage = Name + " läuft gerade (" + laufende + " Prozess(e) einschließlich Helferprogramme).\n\n"
                      + "Zum Aktualisieren muss das Programm beendet werden."
                      + (Eintrag.NeuStartenNachUpdate ? " Danach wird es automatisch neu gestartet." : "")
                      + "\n\nJetzt beenden und aktualisieren?";

            if (!Dialoge.Fragen(frage, "Programm beenden?"))
            {
                StatusText = "Abgebrochen – das Programm läuft weiter.";
                Zustand = UpdateZustand.Abgebrochen;
                return;
            }
        }

        await LaufAsync(async (fortschritt, abbruch) =>
        {
            var liefVorher = Prozessdienst.Laeuft(Eintrag);

            if (Eintrag.BeendenVorUpdate && liefVorher)
            {
                StatusText = "Beendet das Programm …";
                fortschritt.Report("Beende " + string.Join(", ", Eintrag.AlleProzesse));
                await Prozessdienst.BeendenAsync(Eintrag, abbruch);
            }

            StatusText = "Aktualisiert …";
            var ergebnis = await _aktualisierer.AktualisierenAsync(Eintrag, fortschritt, abbruch);

            if (Eintrag.NeuStartenNachUpdate && liefVorher && ergebnis.Zustand == UpdateZustand.Fertig)
            {
                fortschritt.Report("Startet " + Name + " neu.");
                Prozessdienst.Starten(Eintrag, AlsAdministrator);
            }

            // Re-read the version so the card shows the new state right away -- except when the
            // installer only staged the update; there the old version is still the truth and a
            // re-check would wrongly show "Update verfügbar" again.
            if (ergebnis.Zustand == UpdateZustand.Fertig && !ergebnis.ErstNachNeustart)
            {
                var nachher = await _aktualisierer.PruefenAsync(Eintrag, fortschritt, abbruch);
                return ergebnis with
                {
                    Zustand = nachher.Zustand == UpdateZustand.Aktuell ? UpdateZustand.Aktuell : ergebnis.Zustand,
                    InstallierteVersion = nachher.InstallierteVersion,
                    VerfuegbareVersion = nachher.VerfuegbareVersion
                };
            }

            return ergebnis;
        });
    }

    [RelayCommand]
    private void Starten()
    {
        if (!Prozessdienst.Starten(Eintrag, AlsAdministrator))
        {
            Melde("Konnte nicht gestartet werden: " + Pfade.Aufloesen(Eintrag.ExePfad));
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
