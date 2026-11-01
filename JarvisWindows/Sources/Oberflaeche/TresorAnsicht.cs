using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Interop;
using System.Windows.Threading;
using Microsoft.Win32;
using Windows.Security.Credentials.UI;
using static JarvisWindows.Bau;

namespace JarvisWindows;

/// <summary>Sichtbar sind Schlüssel nur nach Windows Hello, höchstens eine Minute. Gesperrt wird außerdem beim Verlassen der Seite,
/// beim Schließen des Fensters oder der App, beim Ruhezustand und beim Bildschirm-Sperren.</summary>
public sealed class TresorSperre : Beobachtbar
{
    public static TresorSperre Shared { get; } = new();
    public const int Sekunden = 60;
    private bool _entsperrt;
    private int _rest;
    private string _meldung = "";
    public bool Entsperrt { get => _entsperrt; private set => Setze(ref _entsperrt, value); }
    public int Rest { get => _rest; private set => Setze(ref _rest, value); }
    public string Meldung { get => _meldung; private set => Setze(ref _meldung, value); }
    private DispatcherTimer? uhr;
    private bool beobachtet;
    private readonly HashSet<Window> fenster = new();

    public void Beobachten()
    {
        if (beobachtet) return;
        beobachtet = true;
        SystemEvents.SessionSwitch += (_, e) =>
        {
            if (e.Reason is SessionSwitchReason.SessionLock or SessionSwitchReason.SessionLogoff or SessionSwitchReason.ConsoleDisconnect or SessionSwitchReason.RemoteDisconnect) Haupt.Post(Sperren);
        };
        SystemEvents.PowerModeChanged += (_, e) => { if (e.Mode == PowerModes.Suspend) Haupt.Post(Sperren); };
        SystemEvents.SessionEnding += (_, _) => Haupt.Post(Sperren);
        if (Application.Current != null) Application.Current.Exit += (_, _) => Sperren();
        HaengeHauptfenster();
    }

    private void HaengeHauptfenster()
    {
        var w = Application.Current?.MainWindow;
        if (w == null || !fenster.Add(w)) return;
        w.Closing += (_, _) => Sperren();
    }

    public async Task Entsperren()
    {
        Beobachten();
        HaengeHauptfenster();
        try
        {
            var verfuegbar = await UserConsentVerifier.CheckAvailabilityAsync();
            if (verfuegbar != UserConsentVerifierAvailability.Available)
            {
                Meldung = "Nicht entsperrt: Windows Hello ist auf diesem Rechner gerade nicht verfügbar (" + verfuegbar + "). Einrichten unter Einstellungen → Konten → Anmeldeoptionen.";
                Log("tresor", "Entsperren nicht möglich", wo: "Einstellungen", fehler: verfuegbar.ToString());
                return;
            }
            var w = AktivesFenster();
            var hwnd = w == null ? IntPtr.Zero : new WindowInteropHelper(w).Handle;
            // Ohne Fensterbezug erscheint die Abfrage in Desktop-Programmen gar nicht oder im Hintergrund.
            var antwort = hwnd != IntPtr.Zero
                ? await UserConsentVerifierInterop.RequestVerificationForWindowAsync(hwnd, "die API-Schlüssel anzuzeigen")
                : await UserConsentVerifier.RequestVerificationAsync("die API-Schlüssel anzuzeigen");
            if (antwort != UserConsentVerificationResult.Verified)
            {
                if (antwort != UserConsentVerificationResult.Canceled)
                {
                    Meldung = "Nicht entsperrt: " + antwort;
                    Log("tresor", "Entsperren abgelehnt", wo: "Einstellungen", fehler: antwort.ToString());
                }
                return;
            }
            Entsperrt = true; Rest = Sekunden; Meldung = "";
            Log("tresor", "Tresor entsperrt", wo: "Einstellungen");
            uhr?.Stop();
            uhr = new DispatcherTimer { Interval = TimeSpan.FromSeconds(1) };
            uhr.Tick += (_, _) =>
            {
                Rest -= 1;
                if (Rest <= 0) Sperren();
            };
            uhr.Start();
        }
        catch (Exception ex)
        {
            Meldung = "Nicht entsperrt: " + ex.Message;
            Log("tresor", "Entsperren abgelehnt", wo: "Einstellungen", fehler: ex.Message);
        }
    }

    public void Sperren()
    {
        uhr?.Stop(); uhr = null;
        // Direkt, nicht über eine neue Aufgabe: Beim Beenden der App liefe die nicht mehr.
        if (Entsperrt) Logbuch.Shared.Schreib("tresor", "Tresor gesperrt", wo: "Einstellungen");
        Entsperrt = false; Rest = 0;
    }
}

public sealed class TresorKopf : LebendeAnsicht
{
    private readonly TresorSperre sperre = TresorSperre.Shared;

    public TresorKopf() { Beobachte(sperre); }

    protected override void Verschwunden() => sperre.Sperren();

    protected override UIElement Baue()
    {
        var offen = sperre.Entsperrt;
        return Rand(V(4,
            H(10,
                Sy(offen ? "lock.open.fill" : "lock.fill", 13, offen ? Farbe.Warnung : Farbe.Gruen),
                T(offen ? $"Tresor offen, sperrt in {sperre.Rest} s" : "Tresor gesperrt", 13, FontWeights.SemiBold),
                Spacer(),
                offen ? Kapsel("lock.fill", "Sperren", sperre.Sperren) : Kapsel("touchid", "Mit Windows Hello anzeigen", async () => await sperre.Entsperren(), hervorgehoben: true)),
            sperre.Meldung.Length > 0 ? T(sperre.Meldung, 11, null, Farbe.Warnung) : null,
            Tresor.Stoerung is { } s ? T(s, 11, null, Farbe.Rot) : null), o: 4, u: 4);
    }
}

/// <summary>Gesperrt: nur Punkte, der Wert selbst erreicht die Oberfläche nicht. Entsperrt: lesbar und änderbar.</summary>
public sealed class TresorFeld : LebendeAnsicht
{
    private readonly string titel;
    private readonly TextBindung wert;
    private readonly TresorSperre sperre = TresorSperre.Shared;

    public TresorFeld(string titel, TextBindung wert)
    {
        this.titel = titel;
        this.wert = wert;
        // Nicht auf Rest: Das Feld verlöre sonst jede Sekunde den Fokus.
        Beobachte(sperre, nameof(TresorSperre.Entsperrt));
    }

    protected override UIElement Baue()
    {
        var leer = wert.Wert.Length == 0;
        var name = T(titel, 13, FontWeights.Medium);
        name.Width = 150;
        var symbol = Sy(leer ? "xmark.circle" : "checkmark.circle.fill", 13, leer ? Farbe.Text3 : Farbe.Gruen);
        wert.Geaendert -= Symbol;
        if (sperre.Entsperrt)
        {
            wert.Geaendert += Symbol;
            this.symbol = symbol;
            return H(8, symbol, name, Fuell((FrameworkElement)Feld(wert, "", 12, schrift: Mono)));
        }
        var punkte = Flaeche(T(leer ? "nicht eingetragen" : new string('•', 16), 13, null, leer ? Farbe.Text3 : Farbe.Text2, 1), Farbe.Glas(0.05), 6, 8, 5);
        punkte.ToolTip = "Antippen und mit Windows Hello entsperren";
        punkte.Cursor = Cursors.Hand;
        punkte.MouseLeftButtonUp += async (_, e) => { e.Handled = true; await sperre.Entsperren(); };
        return H(8, symbol, name, Fuell(punkte));
    }

    private TextBlock? symbol;
    private void Symbol()
    {
        if (symbol == null) return;
        var leer = wert.Wert.Length == 0;
        symbol.Text = Sym.Zeichen(leer ? "xmark.circle" : "checkmark.circle.fill");
        symbol.Foreground = B(leer ? Farbe.Text3 : Farbe.Gruen);
    }
}
