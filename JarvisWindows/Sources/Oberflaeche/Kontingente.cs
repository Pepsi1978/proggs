using System.Windows;
using System.Windows.Controls;
using static JarvisWindows.Bau;

namespace JarvisWindows;

/// <summary>Einstellungen → Kontingente: Mit eigener Anmeldung (Einstellungen → Konsolen) liest der Laptop die Limits selbst, sonst holt sie das Handy (kontingente_lesen). Der letzte Stand bleibt, wenn eine Quelle gerade nicht antwortet.</summary>
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
    /// <summary>Diese Dienste stehen gerade vom Handy in der Liste.</summary>
    public HashSet<string> VomHandy { get; private set; } = new();

    /// <summary>Einzelabruf aus dem Kopf einer Karte: läuft gerade oder ging so aus.</summary>
    public sealed record Lauf(bool Laeuft, bool Erfolg = false, string Text = "", DateTime Um = default, string Stand = "")
    {
        /// <summary>Am Tag danach darf „heute“ nicht mehr dastehen.</summary>
        public string Anzeige => Erfolg ? "Aktualisiert: " + (Um.Date == DateTime.Today ? "heute, " : Um.ToString("dd.MM., ")) + Um.ToString("HH:mm") + " Uhr" : Text;
    }
    public Dictionary<string, Lauf> Laeufe { get; } = new();
    private int _einzeln;
    private bool _wartet, _handyBelegt;
    /// <summary>Ein ausdrücklicher Gesamtabruf wartet: Weitere Klicks hängen sich nicht zusätzlich an.</summary>
    public bool Wartet { get => _wartet; private set => Setze(ref _wartet, value); }

    /// <summary>Das Ergebnis gilt nur, solange kein neuerer Abruf die Karte geändert hat.</summary>
    public Lauf? LaufVon(AnbieterStand a) => Laeufe.TryGetValue(a.Id, out var l) && (l.Laeuft || (l.Stand == a.Stand && (!l.Erfolg || a.Hinweis.Length == 0))) ? l : null;

    /// <summary>Jeder Anbieter bekommt eine Karte und damit einen Knopf, auch ohne bisherigen Stand.</summary>
    public List<AnbieterStand> Anzeige => KontingentQuellen.Alle.Select(d => _anbieter.FirstOrDefault(a => a.Id == d.Id) ?? new AnbieterStand(d.Id, d.Titel, "", "", false, "", new())).ToList();

    public Kontingente()
    {
        if (Vorgaben.Text(Speicher) is { } t && Lies(t) is { } alt) { _anbieter = alt; _ausSpeicher = true; }
    }

    public async Task Laden(bool frisch = false)
    {
        // Ein ausdrücklicher Wunsch wartet die laufende Abfrage ab, statt verloren zu gehen.
        if ((Laedt || _einzeln > 0) && !frisch) return;
        if (Wartet) return;
        // Auch laufende Einzelabrufe abwarten: Der Gesamtabruf arbeitet auf seiner eigenen Kopie und würde sie sonst überschreiben.
        while (Laedt || _einzeln > 0) { Wartet = true; await Task.Delay(300); }
        Wartet = false;
        Laedt = true;
        // Ein neuer Gesamtabruf macht das Ergebnis eines früheren Einzelabrufs hinfällig.
        if (frisch) Laeufe.Clear();
        try
        {
            var stand = _anbieter.ToDictionary(a => a.Id);
            var hier = new List<(string Id, string Titel)>();
            foreach (var d in KontingentQuellen.Alle) if (await KontingentQuellen.HierMoeglich(d.Id)) hier.Add(d);
            var vomHandy = KontingentQuellen.Alle.Select(d => d.Id).Where(id => hier.All(h => h.Id != id)).ToHashSet();
            // Abgemeldet und vom Handy ohne Zahlen: Der alte Stand darf nicht bleiben.
            foreach (var id in stand.Keys.Where(id => vomHandy.Contains(id) && !VomHandy.Contains(id)).ToList()) stand.Remove(id);
            void Zeige()
            {
                Anbieter = KontingentQuellen.Alle.Where(d => stand.ContainsKey(d.Id)).Select(d => stand[d.Id]).ToList();
                VomHandy = new HashSet<string>(vomHandy);
            }
            // Der Laptop zuerst und nebeneinander: Das Handy kann bis zu 85 Sekunden brauchen und hält hier niemanden auf.
            var handy = vomHandy.Count > 0 ? VomHandyHolen(frisch) : null;
            var laeufe = hier.Select(async d =>
            {
                var a = await KontingentQuellen.Lies(d.Id, d.Titel, frisch);
                stand[d.Id] = a;
                Zeige();
                return a;
            }).ToList();
            var fertig = await Task.WhenAll(laeufe);
            // Abgelehnte Anmeldung am Laptop: dann doch vom Handy, falls dort eine liegt.
            var abgelehnt = fertig.Where(a => a.Anmelden).Select(a => a.Id).ToList();
            if (handy == null && abgelehnt.Count > 0) handy = VomHandyHolen(frisch);
            if (handy != null)
            {
                var (liste, text) = await handy;
                if (liste == null)
                {
                    if (hier.Count == 0) Fehler = text.Length == 0 ? "Das Handy antwortet gerade nicht." : text;
                    Log("fehler", "Kontingente vom Handy nicht abrufbar", wo: "Kontingente", fehler: text);
                }
                else
                {
                    foreach (var a in liste)
                    {
                        if (!vomHandy.Contains(a.Id) && !(abgelehnt.Contains(a.Id) && a.Limits.Count > 0)) continue;
                        stand[a.Id] = a;
                        vomHandy.Add(a.Id);
                    }
                    Fehler = null;
                }
            }
            else Fehler = null;
            Zeige();
            AusSpeicher = false;
            Sichere();
        }
        finally { Laedt = false; }
    }

    /// <summary>Fragt nur diesen Anbieter frisch ab und ändert nur seine Karte. Als Erfolg gilt nur ein frischer Stand ohne Hinweis.</summary>
    public async Task Aktualisieren(string id)
    {
        if (Laeufe.TryGetValue(id, out var lauf) && lauf.Laeuft) return;
        Laeufe[id] = new Lauf(true);
        Melde();
        _einzeln++;
        string? fehler;
        try
        {
            while (Laedt) await Task.Delay(300);
            fehler = await EinzelnHolen(id);
        }
        catch (Exception f)
        {
            Log("fehler", $"Kontingent {id} nicht aktualisiert", wo: "Kontingente", fehler: f.Message);
            fehler = "Aktualisierung fehlgeschlagen: " + f.Message;
        }
        finally { _einzeln--; }
        Laeufe[id] = new Lauf(false, fehler == null, fehler ?? "", DateTime.Now, _anbieter.FirstOrDefault(a => a.Id == id)?.Stand ?? "");
        Melde();
    }

    private async Task<string?> EinzelnHolen(string id)
    {
        AnbieterStand? neu = null;
        string? fehler = null;
        var ueberHandy = !await KontingentQuellen.HierMoeglich(id);
        if (!ueberHandy)
        {
            neu = await KontingentQuellen.Lies(id, KontingentQuellen.Alle.First(d => d.Id == id).Titel, true);
            fehler = Fehlertext(neu);
            // Abgelehnte Anmeldung am Laptop: dann doch vom Handy, falls dort eine liegt.
            ueberHandy = neu.Anmelden;
        }
        var vomHandy = false;
        if (ueberHandy)
        {
            // Das Handy arbeitet Einzelabrufe nacheinander ab: So bekommt jeder seine volle Frist.
            while (_handyBelegt) await Task.Delay(300);
            _handyBelegt = true;
            List<AnbieterStand>? liste;
            string text;
            try { (liste, text) = await VomHandyHolen(true, id); }
            finally { _handyBelegt = false; }
            var a = liste?.FirstOrDefault(x => x.Id == id);
            if (a == null)
            {
                if (neu == null) fehler = "Aktualisierung fehlgeschlagen: " + (liste != null || text.Length == 0 ? "Das Handy antwortet gerade nicht." : text);
                Log("fehler", "Kontingent vom Handy nicht abrufbar", wo: "Kontingente", fehler: text);
            }
            else if (neu == null || a.Limits.Count > 0)
            {
                neu = a;
                vomHandy = true;
                // Nach Fristende schickt das Handy den letzten Stand: Der darf nicht als frisch gelten.
                fehler = JSON.Objekt(text)?.Wahr("aktualisiert") == true ? Fehlertext(a) : "Aktualisierung fehlgeschlagen: Das Handy hat keinen frischen Stand bestätigt.";
                Fehler = null;
            }
        }
        if (neu != null)
        {
            var stand = _anbieter.ToDictionary(a => a.Id);
            stand[id] = neu;
            if (vomHandy) VomHandy.Add(id); else VomHandy.Remove(id);
            Anbieter = KontingentQuellen.Alle.Where(d => stand.ContainsKey(d.Id)).Select(d => stand[d.Id]).ToList();
            Sichere();
        }
        return fehler;
    }

    private static string? Fehlertext(AnbieterStand a)
    {
        if (a.Hinweis.StartsWith("Ein Limit", StringComparison.Ordinal)) return "Nicht vollständig aktualisiert: " + a.Hinweis;
        if (a.Hinweis.Length > 0) return "Aktualisierung fehlgeschlagen: " + a.Hinweis.Replace("Letzter Stand. ", "");
        return a.Limits.Count == 0 ? "Aktualisierung fehlgeschlagen: Der Anbieter nennt keine Limits." : null;
    }

    private void Sichere() => Vorgaben.Setze(Speicher, JSON.Text(new JSONObjekt { ["anbieter"] = Anbieter.Select(a => (object?)new JSONObjekt
    {
        ["id"] = a.Id, ["titel"] = a.Titel, ["konto"] = a.Konto, ["hinweis"] = a.Hinweis, ["anmelden"] = a.Anmelden, ["stand"] = a.Stand,
        ["limits"] = a.Limits.Select(l => (object?)new JSONObjekt { ["name"] = l.Name, ["genutzt"] = l.Genutzt, ["resetSatz"] = l.ResetSatz, ["resetAm"] = l.ResetAm, ["resetSek"] = l.ResetSek, ["tempo"] = l.Tempo, ["zuSchnell"] = l.ZuSchnell }).ToList(),
    }).ToList() }));

    private static async Task<(List<AnbieterStand>? Liste, string Text)> VomHandyHolen(bool frisch, string? ziel = null)
    {
        var argumente = new JSONObjekt { ["json"] = true };
        if (frisch) argumente["frisch"] = true;
        if (ziel != null) argumente["anbieter"] = ziel;
        var r = await McpKlient.Shared.Rufe("kontingente_lesen", argumente, 85);
        var t = McpKlient.OhneKontextHinweis(r.Text);
        return (Lies(t), t);
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
        var s = V(18, H(10, Kapsel("arrow.clockwise", "Jetzt neu abfragen", async () => await k.Laden(true), hervorgehoben: true), k.Laedt || k.Wartet ? Drehkreis() : null, Spacer()));
        if (k.Fehler is { } f) VAnfuegen(s, T(k.Anbieter.Count == 0 ? f : "Alter Stand. " + f, 13, null, Farbe.Warnung), 18);
        else if (k.AusSpeicher) VAnfuegen(s, T("Letzter Stand, die neue Abfrage läuft.", 13, null, Farbe.Warnung), 18);
        foreach (var a in k.Anzeige) VAnfuegen(s, Karte_(a), 18);
        VAnfuegen(s, T("Anmelden unter Einstellungen → Konsolen. Ohne Anmeldung hier am Laptop kommt der Stand vom Handy.", 12, null, Farbe.Text3), 18);
        return s;
    }

    private static string Zahl(double x) => x.ToString("0.#", Deutsch);

    private UIElement Karte_(Kontingente.AnbieterStand a)
    {
        var lauf = k.LaufVon(a);
        var s = V(10, H(10, Titel("gauge.with.dots.needle.67percent", a.Titel), Spacer(), AktualisierenKnopf(a, lauf)));
        if (lauf != null) VAnfuegen(s, lauf.Laeuft ? T("Wird aktualisiert …", 13, null, Farbe.Text2) : T(lauf.Anzeige, 13, FontWeights.SemiBold, lauf.Erfolg ? Farbe.Haupt : Farbe.Rot), 10);
        else if (a.Limits.Count == 0 && a.Hinweis.Length == 0 && !k.Laedt) VAnfuegen(s, T("Noch nicht abgefragt.", 13, null, Farbe.Text3), 10);
        if (a.Konto.Length > 0) VAnfuegen(s, T(a.Konto, 12, null, Farbe.Text3), 10);
        foreach (var l in a.Limits) VAnfuegen(s, Limit_(l), 10);
        // Nach einem gescheiterten Einzelabruf steht der Grund schon oben; der Weg zur Anmeldung bleibt.
        if (a.Hinweis.Length > 0 && (a.Anmelden || lauf == null || lauf.Laeuft || lauf.Erfolg)) VAnfuegen(s, T(a.Hinweis + (a.Anmelden ? (k.VomHandy.Contains(a.Id) ? " Anmelden unter Einstellungen → Konsolen oder auf dem Handy." : " Anmelden unter Einstellungen → Konsolen.") : ""), 12, null, Farbe.Warnung), 10);
        if (a.Stand.Length > 0) VAnfuegen(s, T(a.Stand, 11, null, Farbe.Text3), 10);
        return Karte(s);
    }

    private UIElement AktualisierenKnopf(Kontingente.AnbieterStand a, Kontingente.Lauf? lauf)
    {
        if (lauf?.Laeuft == true)
        {
            var kreis = Drehkreis(14);
            kreis.HorizontalAlignment = HorizontalAlignment.Center;
            return new Border { Width = 26, Height = 24, Child = kreis };
        }
        var symbol = lauf == null ? "arrow.clockwise" : lauf.Erfolg ? "checkmark" : "exclamationmark.triangle.fill";
        return new KleinKnopf(symbol, a.Titel + " aktualisieren", () => _ = k.Aktualisieren(a.Id), lauf == null ? null : lauf.Erfolg ? Farbe.Haupt : Farbe.Rot);
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
