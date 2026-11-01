using System.Windows;
using System.Windows.Controls;
using static JarvisWindows.Bau;

namespace JarvisWindows;

/// <summary>Einstellungen → Kontingente: Die Limits holt das Handy (kontingente_lesen), dort liegen auch die Anmeldungen. Der letzte Stand bleibt, wenn das Handy gerade nicht antwortet.</summary>
public sealed class Kontingente : Beobachtbar
{
    public static Kontingente Shared { get; } = new();
    private const string Speicher = "kontingente.letzterStand";

    public sealed record Limit(string Name, double Genutzt, string ResetSatz, string ResetAm, double ResetSek, string Tempo, bool ZuSchnell);
    public sealed record AnbieterStand(string Id, string Titel, string Konto, string Hinweis, bool Anmelden, string Stand, List<Limit> Limits);

    private List<AnbieterStand> _anbieter = new();
    private string? _fehler;
    private bool _laedt, _ausSpeicher;
    public List<AnbieterStand> Anbieter { get => _anbieter; private set { _anbieter = value; Melde(); } }
    public string? Fehler { get => _fehler; private set => Setze(ref _fehler, value); }
    public bool Laedt { get => _laedt; private set => Setze(ref _laedt, value); }
    /// <summary>Der Stand stammt vom letzten Programmlauf: „heute“ und das Tempo galten nur damals.</summary>
    public bool AusSpeicher { get => _ausSpeicher; private set => Setze(ref _ausSpeicher, value); }

    public Kontingente()
    {
        if (Vorgaben.Text(Speicher) is { } t && Lies(t) is { } alt) { _anbieter = alt; _ausSpeicher = true; }
    }

    public async Task Laden(bool frisch = false)
    {
        // Ein ausdrücklicher Wunsch wartet die laufende Abfrage ab, statt verloren zu gehen.
        if (Laedt && !frisch) return;
        while (Laedt) await Task.Delay(300);
        Laedt = true;
        try
        {
            var argumente = new JSONObjekt { ["json"] = true };
            if (frisch) argumente["frisch"] = true;
            var r = await McpKlient.Shared.Rufe("kontingente_lesen", argumente, 85);
            var t = McpKlient.OhneKontextHinweis(r.Text);
            var neu = Lies(t);
            if (neu == null)
            {
                Fehler = t.Length == 0 ? "Das Handy antwortet gerade nicht." : t;
                Log("fehler", "Kontingente nicht abrufbar", wo: "Kontingente", fehler: Fehler);
                return;
            }
            Fehler = null;
            Anbieter = neu;
            AusSpeicher = false;
            Vorgaben.Setze(Speicher, t);
        }
        finally { Laedt = false; }
    }

    private static List<AnbieterStand>? Lies(string t)
    {
        var j = JSON.Objekt(t);
        if (j == null || j.Liste("anbieter") == null) return null;
        return j.Objekte("anbieter").Select(a => new AnbieterStand(
            a.Text("id") ?? "", a.Text("titel") ?? "", a.Text("konto") ?? "", a.Text("hinweis") ?? "", a.Wahr("anmelden") ?? false, a.Text("stand") ?? "",
            a.Objekte("limits").Select(l => new Limit(l.Text("name") ?? "", l.Zahl("genutzt") ?? 0, l.Text("resetSatz") ?? "", l.Text("resetAm") ?? "", l.Zahl("resetSek") ?? 0,
                l.Text("tempo") ?? "", l.Wahr("zuSchnell") ?? false)).ToList())).ToList();
    }
}

public sealed class KontingenteAnsicht : LebendeAnsicht
{
    private readonly Kontingente k = Kontingente.Shared;

    public KontingenteAnsicht() { Beobachte(k); }

    protected override void Erschienen() { _ = k.Laden(); }

    protected override UIElement Baue()
    {
        var s = V(18, H(10, Kapsel("arrow.clockwise", "Jetzt neu abfragen", async () => await k.Laden(true), hervorgehoben: true), k.Laedt ? Drehkreis() : null, Spacer()));
        if (k.Fehler is { } f) VAnfuegen(s, T(k.Anbieter.Count == 0 ? f : "Alter Stand. " + f, 13, null, Farbe.Warnung), 18);
        else if (k.AusSpeicher) VAnfuegen(s, T("Letzter Stand, die neue Abfrage läuft.", 13, null, Farbe.Warnung), 18);
        foreach (var a in k.Anbieter) VAnfuegen(s, Karte_(a), 18);
        VAnfuegen(s, T("Die Anmeldungen liegen auf dem Handy: Einstellungen → Kontingente.", 12, null, Farbe.Text3), 18);
        return s;
    }

    private static string Zahl(double x) => x.ToString("0.#", Deutsch);

    private UIElement Karte_(Kontingente.AnbieterStand a)
    {
        var s = V(10, Titel("gauge.with.dots.needle.67percent", a.Titel));
        if (a.Konto.Length > 0) VAnfuegen(s, T(a.Konto, 12, null, Farbe.Text3), 10);
        foreach (var l in a.Limits) VAnfuegen(s, Limit_(l), 10);
        if (a.Hinweis.Length > 0) VAnfuegen(s, T(a.Hinweis + (a.Anmelden ? " Anmelden auf dem Handy unter Einstellungen → Kontingente." : ""), 12, null, Farbe.Warnung), 10);
        if (a.Stand.Length > 0) VAnfuegen(s, T(a.Stand, 11, null, Farbe.Text3), 10);
        return Karte(s);
    }

    /// <summary>Aus dem Speicher: festes Datum statt „heute“, kein Tempo; ein inzwischen abgelaufenes Fenster gilt wieder als frei.</summary>
    private UIElement Limit_(Kontingente.Limit l)
    {
        var abgelaufen = l.ResetSek > 0 && l.ResetSek <= DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() / 1000.0;
        var schnell = l.ZuSchnell && !k.AusSpeicher && !abgelaufen;
        var genutzt = Math.Min(100, Math.Max(0, abgelaufen ? 0 : l.Genutzt));
        var reset = abgelaufen ? "Zurückgesetzt, neuer Verbrauch erst beim nächsten Abruf" : (k.AusSpeicher && l.ResetAm.Length > 0 ? "Zurückgesetzt: " + l.ResetAm : l.ResetSatz);
        var s = V(4,
            T(l.Name, 12, FontWeights.SemiBold, Farbe.Text2),
            T($"{Zahl(genutzt)} % verbraucht", 24, FontWeights.Bold, schnell ? Farbe.Rot : Farbe.Haupt),
            Balken(genutzt / 100, schnell ? Farbe.Rot : null),
            T(reset, 13));
        if (l.Tempo.Length > 0 && !k.AusSpeicher && !abgelaufen)
            VAnfuegen(s, T(schnell ? "Zu schnell: " + l.Tempo : l.Tempo, 12, schnell ? FontWeights.SemiBold : FontWeights.Normal, schnell ? Farbe.Rot : Farbe.Text3), 4);
        s.Margin = new Thickness(0, 4, 0, 0);
        return s;
    }
}
