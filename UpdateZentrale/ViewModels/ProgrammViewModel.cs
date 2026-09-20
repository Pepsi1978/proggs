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

    public string Kuerzel => new string(Eintrag.Name
        .Split(new[] { ' ' }, StringSplitOptions.RemoveEmptyEntries)
        .Take(2)
        .Select(t => char.ToUpperInvariant(t[0]))
        .ToArray());

    public string ArtText => Eintrag.Art switch
    {
        "winget" => "winget",
        "msstore" => "Microsoft Store",
        "cli" => "Selbst-Update",
        "reposkript" => "Eigenes Skript",
        _ => Eintrag.Art
    };

    public string? Hinweis => Eintrag.Hinweis;
    public bool HatHinweis => !string.IsNullOrWhiteSpace(Eintrag.Hinweis);
    public bool KannStarten => !string.IsNullOrWhiteSpace(Eintrag.ExePfad);

    [ObservableProperty] private UpdateZustand _zustand = UpdateZustand.Unbekannt;
    [ObservableProperty] private string _statusText = "Noch nicht geprueft";
    [ObservableProperty] private string _installierteVersion = "";
    [ObservableProperty] private string _verfuegbareVersion = "";
    [ObservableProperty] private bool _istBeschaeftigt;
    [ObservableProperty] private bool _laeuft;
    [ObservableProperty] private bool _imAutostart;
    [ObservableProperty] private bool _adminWarnung;
    [ObservableProperty] private bool _ausgewaehlt = true;

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
                Melde("Administratormodus liess sich nicht setzen - Programmdatei nicht gefunden.");
            }

            AdminWarnung = Systemdienst.AdminBrichtAutostart(Eintrag, _alsAdministrator);
            OnPropertyChanged();
        }
    }

    public string Protokoll => _protokoll.ToString();

    public bool KannAktualisieren => _aktualisierer is not null && !IstBeschaeftigt;

    public string VersionsText => string.IsNullOrWhiteSpace(InstallierteVersion)
        ? "-"
        : string.IsNullOrWhiteSpace(VerfuegbareVersion) || VerfuegbareVersion == InstallierteVersion
            ? InstallierteVersion
            : InstallierteVersion + "   →   " + VerfuegbareVersion;

    public bool HatUpdate => Zustand == UpdateZustand.UpdateVerfuegbar;

    public event EventHandler<string>? Meldung;

    public void ZustandAktualisieren()
    {
        Laeuft = Prozessdienst.Laeuft(Eintrag);
        ImAutostart = Systemdienst.ImAutostart(Eintrag);
        AdminWarnung = Systemdienst.AdminBrichtAutostart(Eintrag, _alsAdministrator);
    }

    [RelayCommand]
    private async Task PruefenAsync()
    {
        if (_aktualisierer is null) return;
        await LaufAsync(async (fortschritt, abbruch) =>
        {
            StatusText = "Wird geprueft …";
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
            var frage = Name + " laeuft gerade (" + laufende + " Prozess(e) inklusive Helferprogramme).\n\n"
                      + "Zum Aktualisieren muss das Programm beendet werden."
                      + (Eintrag.NeuStartenNachUpdate ? " Danach wird es automatisch neu gestartet." : "")
                      + "\n\nJetzt beenden und aktualisieren?";

            if (!Dialoge.Fragen(frage, "Programm beenden?"))
            {
                StatusText = "Abgebrochen - das Programm laeuft weiter.";
                Zustand = UpdateZustand.Abgebrochen;
                return;
            }
        }

        await LaufAsync(async (fortschritt, abbruch) =>
        {
            var liefVorher = Prozessdienst.Laeuft(Eintrag);

            if (Eintrag.BeendenVorUpdate && liefVorher)
            {
                StatusText = "Beende Programm …";
                fortschritt.Report("Beende " + string.Join(", ", Eintrag.AlleProzesse));
                await Prozessdienst.BeendenAsync(Eintrag, abbruch);
            }

            StatusText = "Aktualisiert …";
            var ergebnis = await _aktualisierer.AktualisierenAsync(Eintrag, fortschritt, abbruch);

            if (Eintrag.NeuStartenNachUpdate && liefVorher && ergebnis.Zustand == UpdateZustand.Fertig)
            {
                fortschritt.Report("Starte " + Name + " neu.");
                Prozessdienst.Starten(Eintrag, AlsAdministrator);
            }

            // Re-read the version so the card shows the new state right away.
            if (ergebnis.Zustand == UpdateZustand.Fertig)
            {
                var nachher = await _aktualisierer.PruefenAsync(Eintrag, fortschritt, abbruch);
                return ergebnis with
                {
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
        OnPropertyChanged(nameof(KannAktualisieren));

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
            StatusText = string.IsNullOrWhiteSpace(ergebnis.Meldung) ? Zustand.ToString() : ergebnis.Meldung;
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
            OnPropertyChanged(nameof(KannAktualisieren));
            OnPropertyChanged(nameof(VersionsText));
            OnPropertyChanged(nameof(HatUpdate));
            OnPropertyChanged(nameof(Protokoll));
        }
    }

    private void Melde(string text) => Meldung?.Invoke(this, text);
}
