using System.Globalization;
using System.Windows;
using System.Windows.Controls;

namespace JarvisWindows;

/// <summary>Vorlese-Einstellungen wie am Handy: Anbieter, Stimme mit Favoriten-Stern und Hörprobe, Tempo je Anbieter,
/// Schlüssel, Gemini-Schlüsselreihenfolge und Rückfallkette.</summary>
public sealed class VorleseEinstellungenAnsicht : UserControl
{
    private readonly VorleseEinstellungen v = VorleseEinstellungen.Shared;
    private readonly Stimme stimme = Stimme.Shared;
    private string? probeVon;

    public VorleseEinstellungenAnsicht()
    {
        Content = Ui.V(18,
            new Reaktiv(AnbieterKarte, new[] { nameof(VorleseEinstellungen.Anbieter) }, v),
            new Reaktiv(StimmenKarte, new[]
            {
                nameof(VorleseEinstellungen.Anbieter), nameof(VorleseEinstellungen.EdgeStimme), nameof(VorleseEinstellungen.GoogleStimme), nameof(VorleseEinstellungen.GeminiFlashStimme),
                nameof(VorleseEinstellungen.GeminiLiteStimme), nameof(VorleseEinstellungen.AzureStimme), nameof(VorleseEinstellungen.QwenStimme), nameof(VorleseEinstellungen.KlonStimmeId),
                nameof(VorleseEinstellungen.KlonStimmen), nameof(VorleseEinstellungen.KlonFehler), nameof(VorleseEinstellungen.Favoriten), nameof(Stimme.Spricht),
            }, v, stimme),
            SchluesselKarte(),
            new Reaktiv(RueckfallKarte, new[] { nameof(VorleseEinstellungen.Kette), nameof(VorleseEinstellungen.Zugang), nameof(VorleseEinstellungen.KlonStimmeId) }, v));
        Loaded += (_, _) =>
        {
            if (v.Zugang.Qwen.Length > 0 && v.KlonStimmen.Count == 0) Ui.Starte(v.KlonStimmenLaden);
        };
    }

    // MARK: Anbieter und Tempo

    private UIElement AnbieterKarte()
    {
        UIElement tempo;
        if (v.Anbieter.KenntTempo)
        {
            var anzeige = Ui.Tx(TempoText(v.Tempo(v.Anbieter)), 12).Breite(50);
            var regler = new Slider
            {
                Minimum = VorleseEinstellungen.TempoMin, Maximum = VorleseEinstellungen.TempoMax, TickFrequency = 0.05, IsSnapToTickEnabled = true,
                Value = v.Tempo(v.Anbieter), Width = 220, VerticalAlignment = VerticalAlignment.Center, Foreground = Farbe.Haupt,
            };
            regler.ValueChanged += (_, e) =>
            {
                v.SetzeTempo(e.NewValue, fuer: v.Anbieter);
                anzeige.Text = TempoText(e.NewValue);
            };
            tempo = Ui.V(12,
                Ui.H(10, Ui.Tx("Tempo", 14, FontWeights.SemiBold), Ui.Spacer(), regler, anzeige),
                Ui.Tx("Jede Stimme behält ihr eigenes Tempo, auch als Rückfallstufe.", 11, null, Farbe.Text3));
        }
        else
        {
            tempo = Ui.Tx("Diese Stimme spricht in ihrem eigenen Tempo.", 12, null, Farbe.Text3);
        }
        return new GlasKarte(Ui.V(12,
            new KartenTitel("speaker.wave.3.fill", "Vorlesen"),
            Ui.H(10,
                Ui.Tx("Anbieter", 14, FontWeights.SemiBold),
                Ui.Spacer(),
                Bau.Auswahl(VorleseAnbieter.AllCases.Select(a => (a, a.Titel)), () => v.Anbieter, a => v.Anbieter = a, 260)),
            Ui.Tx(v.Anbieter.Beschreibung, 12, null, Farbe.Text2),
            tempo,
            Ui.H(10,
                Ui.V(2,
                    Ui.Tx("Immer deutsch vorlesen", 14, FontWeights.SemiBold),
                    Ui.Tx("Mehrsprachige Stimmen sprechen englisch aussehende Wörter sonst englisch aus.", 11, null, Farbe.Text2)).Fuell(),
                new Schalter(() => v.ImmerDeutsch, x => v.ImmerDeutsch = x)),
            Ui.H(10,
                new KapselKnopf("play.fill", "Hörprobe", () => Ui.Starte(() => stimme.Probe(v.Stimme(v.Anbieter))), hervorgehoben: true),
                new KapselKnopf("stop.fill", "Stopp", stimme.Stopp))));
    }

    /// <summary>.frame(maxWidth: .infinity): Die Auswahl nimmt die ganze freie Breite ein.</summary>
    private static FrameworkElement Breit(FrameworkElement auswahl)
    {
        auswahl.HorizontalAlignment = HorizontalAlignment.Stretch;
        if (auswahl is ContentControl c)
        {
            c.HorizontalContentAlignment = HorizontalAlignment.Stretch;
            if (c.Content is FrameworkElement kopf) kopf.HorizontalAlignment = HorizontalAlignment.Stretch;
        }
        return auswahl.Fuell();
    }

    private static string TempoText(double t) => t.ToString("0.00", CultureInfo.InvariantCulture) + "×";

    // MARK: Stimmen mit Favoriten

    private UIElement StimmenKarte()
    {
        var a = v.Anbieter;
        var liste = v.StimmenListe(a);
        UIElement inhalt;
        if (liste.Count == 0)
        {
            inhalt = Ui.Tx(a == VorleseAnbieter.QwenKlon ? "Noch keine geklonte Stimme geladen – Alibaba-Schlüssel eintragen und laden." : "Keine Stimmen.", 13, null, Farbe.Text2);
        }
        else
        {
            var gewaehlt = v.Stimme(a);
            var favorit = v.Favoriten.Contains(gewaehlt);
            // Favoriten stehen oben im Menü.
            var werte = new List<(string, string)>();
            if (!liste.Any(s => s.Id == gewaehlt)) werte.Add((gewaehlt, "Nicht mehr verfügbar, bitte wählen"));
            werte.AddRange(liste.Select(st => (st.Id, $"{(v.Favoriten.Contains(st.Id) ? "★ " : "")}{st.Name} · {(st.Geschlecht == Geschlecht.Weiblich ? "weiblich" : "männlich")}")));
            var probeLaeuft = probeVon == gewaehlt && stimme.Spricht;
            var auswahl = Bau.Auswahl(werte, () => v.Stimme(a), id => v.SetzeStimme(id, fuer: a), double.PositiveInfinity);
            inhalt = Ui.H(10,
                Breit(auswahl),
                new RundKnopf(favorit ? "star.fill" : "star", () => v.FavoritUmschalten(gewaehlt), favorit ? "Favorit entfernen" : "Als Favorit markieren", favorit, Farbe.Gold, 30),
                new RundKnopf(probeLaeuft ? "stop.fill" : "play.fill", () =>
                {
                    if (probeLaeuft) { stimme.Stopp(); probeVon = null; return; }
                    probeVon = gewaehlt;
                    Ui.Starte(async () =>
                    {
                        await stimme.Probe(gewaehlt);
                        if (probeVon == gewaehlt) probeVon = null;
                    });
                }, probeLaeuft ? "Hörprobe beenden" : "Hörprobe", groesse: 30));
        }
        return new GlasKarte(Ui.V(10,
            Ui.H(10,
                new KartenTitel("person.wave.2.fill", $"Stimme · {a.Kurz}", Farbe.Gold),
                Ui.Spacer(),
                a == VorleseAnbieter.QwenKlon ? new KapselKnopf("arrow.clockwise", "Aus dem Alibaba-Konto laden", () => Ui.Starte(v.KlonStimmenLaden)) : null),
            v.KlonFehler is string f && a == VorleseAnbieter.QwenKlon ? Ui.Tx(f, 12, null, Farbe.Rot) : null,
            inhalt));
    }

    // MARK: Schlüssel

    private UIElement SchluesselKarte()
    {
        var region = new TextFeld("westeurope", text: v.Zugang.AzureRegion, schreiben: t =>
        {
            if (v.Zugang.AzureRegion == t) return;
            v.Zugang.AzureRegion = t;
            v.ZugangGeaendert();
        });
        return new GlasKarte(Ui.V(10,
            new KartenTitel("key.fill", "Schlüssel fürs Vorlesen", Farbe.Warnung),
            Ui.Tx("Verschlüsselt im Tresor dieses PCs, Anzeigen und Ändern nur nach Windows Hello. Edge braucht keinen Schlüssel.", 11, null, Farbe.Text2),
            Ui.Tx("Die Schlüssel stehen unter Einstellungen → Konten und APIs.", 12, null, Farbe.Text2),
            Ui.H(0,
                Ui.Tx("Gemini-Reihenfolge", 13, FontWeights.Medium).Breite(150),
                Breit(Bau.Auswahl(new[]
                {
                    (VorleseEinstellungen.GeminiFreiDannBezahlt, "Erst kostenlos, dann bezahlt"),
                    (VorleseEinstellungen.GeminiNurFrei, "Nur kostenlos"),
                    (VorleseEinstellungen.GeminiNurBezahlt, "Nur bezahlt"),
                }, () => v.GeminiWahl, x => v.GeminiWahl = x, double.PositiveInfinity))),
            Ui.H(0,
                Ui.Tx("Azure-Region", 13, FontWeights.Medium).Breite(150),
                Ui.Box(region, Farbe.Glas(0.07), 8, 10, 6, Farbe.Glas(0.10)).Fuell()),
            Ui.Tx("F0 kostenlos 500 000 Zeichen im Monat; HD-Stimmen nur mit S0.", 11, null, Farbe.Text3)));
    }

    // MARK: Rückfallkette

    private UIElement RueckfallKarte()
    {
        var zeilen = Ui.V(10,
            new KartenTitel("arrow.triangle.branch", "Rückfall, wenn die Stimme scheitert", Farbe.Gruen),
            Ui.Tx("Aktive Stufen werden der Reihe nach versucht, jede mit ihrer eigenen Stimme. Zuletzt liest immer Edge, ohne Netz die Systemstimme.", 11, null, Farbe.Text2));
        foreach (var id in v.Kette)
            if (VorleseKatalog.RueckfallStufeZu(id) is { } stufe) zeilen.Children.Add(StufenZeile(stufe, true));
        foreach (var stufe in VorleseKatalog.RueckfallStufen.Where(s => !v.Kette.Contains(s.Id)))
            zeilen.Children.Add(StufenZeile(stufe, false));
        return new GlasKarte(zeilen);
    }

    private UIElement StufenZeile(RueckfallStufe stufe, bool aktiv)
    {
        var bereit = v.Bereit(stufe);
        var nr = v.Kette.IndexOf(stufe.Id);
        return Ui.H(10,
            new Schalter(() => aktiv, _ => v.StufeUmschalten(stufe.Id)),
            aktiv && nr >= 0 ? Ui.Tx($"{nr + 1}.", 12, FontWeights.Bold, Farbe.Haupt).Breite(20) : null,
            Ui.Tx(stufe.Titel, 13, aktiv ? FontWeights.SemiBold : FontWeights.Regular, aktiv ? Farbe.Text : Farbe.Text2),
            !bereit ? Ui.Tx("Schlüssel fehlt", 10, FontWeights.Medium, Farbe.Warnung) : null,
            Ui.Spacer(),
            aktiv ? Sym.Bild("chevron.up", 12).Klick(() => v.StufeVerschieben(stufe.Id, -1)) : null,
            aktiv ? Sym.Bild("chevron.down", 12).Klick(() => v.StufeVerschieben(stufe.Id, 1)) : null).Rand(0, 3);
    }
}
