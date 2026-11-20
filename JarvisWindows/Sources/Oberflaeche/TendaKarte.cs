using System.Windows;
using System.Windows.Controls;
using static JarvisWindows.Ui;

namespace JarvisWindows;

/// <summary>Stand der Tenda-Karte, fertig bewertet vom Handy-Jarvis (tenda_lesen, structuredContent) wie TendaDaten am Mac;
/// Schalten (Modus, Connect, Neustart) über dieselben Handy-Werkzeuge.</summary>
public sealed class TendaDaten : Beobachtbar
{
    public static TendaDaten Shared { get; } = new();
    private JSONObjekt? _stand;
    public JSONObjekt? Stand { get => _stand; private set => Setze(ref _stand, value); }
    private bool _laeuft;
    public bool Laeuft { get => _laeuft; private set => Setze(ref _laeuft, value); }
    private string? _meldung;
    public string? Meldung { get => _meldung; private set => Setze(ref _meldung, value); }
    private bool _offen;
    public bool Offen { get => _offen; set => Setze(ref _offen, value); }
    public bool AutomatischGelesen { get; set; }
    /// <summary>Ein Schaltbefehl ist unterwegs: kein zweiter (Doppelklick hieße zwei Neustarts).</summary>
    private bool schaltet;

    public async Task Lesen()
    {
        if (Laeuft) return;
        Laeuft = true;
        try
        {
            var s = await McpKlient.Shared.Struktur("tenda_lesen", new JSONObjekt(), 150);
            if (s != null) { Stand = s; Meldung = null; }
            else Meldung = "Das Handy-Jarvis ist gerade nicht erreichbar oder hat keinen Router-Stand.";
        }
        finally { Laeuft = false; }
    }

    public async Task Schalten(string werkzeug, JSONObjekt args)
    {
        if (schaltet) return;
        schaltet = true;
        try
        {
            Meldung = "Befehl wird gesendet …";
            var e = await McpKlient.Shared.Rufe(werkzeug, args, 120);   // Handy kann bei Mac-Ausfall direkt schalten
            Meldung = McpKlient.OhneKontextHinweis(e.Text);
        }
        finally { schaltet = false; }
    }
}

/// <summary>Startkarte „Tenda Internet“ in der Zentrale wie am Handy und Mac: zugeklappt Modus, Multiplayer, Stand und rechts der Aktualisieren-Knopf;
/// aufgeklappt Verbindung, LTE- und 5G-Werte mit Bewertung, Modus, Connect und Neustart mit Ja/Nein.</summary>
public static class TendaKarte
{
    public static UIElement Bauen()
    {
        var d = TendaDaten.Shared;
        if (!d.AutomatischGelesen) { d.AutomatischGelesen = true; Starte(d.Lesen); }
        return new Reaktiv(() =>
        {
            var s = d.Stand;
            var mp = s?.Obj("multiplayer");
            var modus = s?.Text("modus_aktiv")?.Replace(" (abgeleitet)", "");
            var titel = H(8, new KartenTitel("antenna.radiowaves.left.and.right", "Tenda Internet", Farbe.Haupt), modus != null ? Tx(modus, 12, FontWeights.SemiBold, Farbe.Haupt) : null);
            // Alter beim Anzeigen berechnet, damit „aktuell“ nicht stehen bleibt; ab 90 Minuten veraltet wie am Handy.
            var zeit = s?.Zahl("zeit") is { } sek ? DateTimeOffset.FromUnixTimeSeconds((long)sek).LocalDateTime : (DateTime?)null;
            var veraltet = s?.Text("veraltet_grund") != null || (zeit is { } zz && (DateTime.Now - zz).TotalMinutes > 90);
            var standZeile = zeit is { } zt ? "Stand " + zt.ToString(zt.Date == DateTime.Today ? "HH:mm 'Uhr'" : "dd.MM. HH:mm 'Uhr'") + " · " + (s?.Text("quelle") ?? "") + (veraltet ? " · veraltet" : "") : "";
            var links = V(2, titel,
                Tx(mp != null ? "Multiplayer: " + mp.Text("text") : d.Laeuft ? "Liest den Router …" : "Noch kein Stand", 14, FontWeights.SemiBold, StufenFarbe(mp?.Text("stufe"))),
                Tx(d.Laeuft ? "Frische Abfrage läuft …" : standZeile, 12, null, d.Laeuft ? Farbe.Haupt : veraltet ? Farbe.Warnung : Farbe.Text3, zeilen: 1)).Fuell();
            links.Klick(() => d.Offen = !d.Offen);
            var kopf = H(8, links, d.Laeuft ? new Spinner(16) : null,
                new RundKnopf("arrow.clockwise", () => Starte(d.Lesen), hilfe: "Router frisch abfragen", aktiv: d.Laeuft, groesse: 30),
                new RundKnopf(d.Offen ? "chevron.up" : "chevron.down", () => d.Offen = !d.Offen, hilfe: d.Offen ? "Zuklappen" : "Aufklappen", groesse: 26));
            var v = V(10, kopf);
            if (d.Meldung is { } m) v.Children.Add(Tx(m, 12, null, Farbe.Text2));
            if (d.Offen)
            {
                if (mp?.Text("begruendung") is { } b) v.Children.Add(Tx("Multiplayer-Grundlage: " + b, 12, null, Farbe.Text2));
                foreach (var a in s?.Objekte("abschnitte") ?? new List<JSONObjekt>())
                {
                    var abschnitt = V(4, Tx(a.Text("titel") ?? "", 13, FontWeights.SemiBold, Farbe.Haupt));
                    foreach (var z in a.Objekte("zeilen"))
                    {
                        var wert = z.Text("wert") ?? "";
                        var fehlt = wert.StartsWith("nicht ");
                        var bew = z.Text("bewertung");
                        abschnitt.Children.Add(H(8, Tx(z.Text("name") ?? "", 13, null, Farbe.Text2).Fuell(), Tx(wert, 13, fehlt ? null : FontWeights.SemiBold, fehlt ? Farbe.Text3 : null),
                            bew != null ? Tx(bew, 11, FontWeights.SemiBold, StufenFarbe(bew)) : null));
                    }
                    v.Children.Add(abschnitt);
                }
                v.Children.Add(Tx("Modus eingestellt: " + (s?.Text("modus_eingestellt") ?? "nicht gelesen") + " · aktiv: " + (s?.Text("modus_aktiv") ?? "nicht geliefert"), 12, null, Farbe.Text2));
                v.Children.Add(H(8, Spacer(),
                    new KapselKnopf("antenna.radiowaves.left.and.right", "5G SA bevorzugt", () => Starte(() => d.Schalten("tenda_modus_setzen", new JSONObjekt { ["modus"] = "5g_sa" }))),
                    new KapselKnopf("antenna.radiowaves.left.and.right", "5G NSA", () => Starte(() => d.Schalten("tenda_modus_setzen", new JSONObjekt { ["modus"] = "5g_nsa" }))),
                    new KapselKnopf("antenna.radiowaves.left.and.right", "nur 4G", () => Starte(() => d.Schalten("tenda_modus_setzen", new JSONObjekt { ["modus"] = "4g" })))));
                v.Children.Add(H(8, Spacer(),
                    new KapselKnopf("arrow.clockwise", "Connect", () => Starte(() => d.Schalten("tenda_verbinden", new JSONObjekt()))),
                    new KapselKnopf("power", "Router neu starten", () =>
                    {
                        if (MessageBox.Show("Willst du den Router wirklich neu starten?", "Router neu starten", MessageBoxButton.YesNo, MessageBoxImage.Question) == MessageBoxResult.Yes)
                            Starte(() => d.Schalten("tenda_neustart", new JSONObjekt { ["bestaetigt"] = true }));
                    })));
                v.Children.Add(Tx("Moduswechsel, Connect und Neustart unterbrechen das Internet zu Hause kurz. Jarvis prüft danach den neuen Zustand und löst unklare Befehle nicht erneut aus.", 11, null, Farbe.Text3));
            }
            return new GlasKarte(v);
        }, new[] { nameof(TendaDaten.Stand), nameof(TendaDaten.Laeuft), nameof(TendaDaten.Meldung), nameof(TendaDaten.Offen) }, d);
    }

    private static System.Windows.Media.Brush StufenFarbe(string? s) => s switch
    {
        "exzellent" or "gut" => Farbe.Gruen,
        "mittel" => Farbe.Warnung,
        "schlecht" or "sehr schlecht" => Farbe.Rot,
        _ => Farbe.Text,
    };
}
