using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Shapes;
using static JarvisWindows.Ui;

namespace JarvisWindows;

/// <summary>Der Biomarker-Bereich am Laptop, nach dem Biomarker-Bereich des Handys (bio/presentation): Abrufstand je Quelle, alle Messgrößen als Karten
/// mit aktuellem Wert, Einordnung gegen die Tage davor, Schnitten 7/30/90 und Verlauf als Balken; Zeitraum 7, 30 oder 90 Tage; Tageswerte eines Tages.
/// Die Karten lassen sich wie am Handy per Drag &amp; Drop umsortieren.</summary>
public sealed class BiomarkerAnsicht : Decorator
{
    private readonly BioDaten daten = BioDaten.Shared;
    private static string tag = "heute";

    public BiomarkerAnsicht()
    {
        var kopf = new BereichsKopf("bolt.heart.fill", "Biomarker", "Spiegel des Handys: Whoop, Oura, Waage und Trainings, ausgewertet wie in Jarvis am Handy.");
        var knoepfe = new Reaktiv(() => H(10,
            new SegmentWahl(new[] { ("7", "7 Tage"), ("30", "30 Tage"), ("90", "90 Tage") }, daten.Tage.ToString(), w => Starte(() => daten.Laden(tage: int.Parse(w)))).Breite(260),
            Spacer(),
            daten.Laedt ? new Spinner(16) : null,
            new RundKnopf("arrow.clockwise", () => Starte(() => daten.Laden(erzwingen: true)), hilfe: "Vom Handy neu lesen", groesse: 32),
            new KapselKnopf(daten.HoltFrisch ? "clock" : "arrow.down.circle.fill", daten.HoltFrisch ? "Holt frische Werte …" : "Frische Werte holen", () => Starte(daten.FrischHolen))
                .Hilfe("Bittet das Handy, jetzt bei Whoop, Oura und Waage abzurufen (bis zu einer Minute). Nur bei Bedarf: Jeder Abruf belastet die Whoop-Anmeldung.")),
            new[] { nameof(BioDaten.Laedt), nameof(BioDaten.HoltFrisch), nameof(BioDaten.Stand) }, daten);
        var status = new Reaktiv(() =>
        {
            var s = V(4);
            if (daten.Zuletzt is { } z) s.Children.Add(Tx($"Gelesen {Relativ(z)} ({z:HH:mm} Uhr)", 12, null, Farbe.Text3));
            foreach (var k in daten.Kopf) s.Children.Add(Tx(k, 12, null, k.StartsWith("Hinweis", StringComparison.Ordinal) ? Farbe.Warnung : Farbe.Text2));
            if (daten.AbrufMeldung is { } a) s.Children.Add(Box(new MarkdownText(a, 12), Farbe.Glas(0.04), 10, 12, 8));
            if (daten.Fehler is { } f) s.Children.Add(Tx(f, 12, FontWeights.SemiBold, Farbe.Warnung));
            if (daten.Werte.Count == 0 && !daten.Laedt && daten.Fehler == null) s.Children.Add(Tx("Noch keine Werte vom Handy.", 13, null, Farbe.Text2));
            return s;
        }, new[] { nameof(BioDaten.Stand), nameof(BioDaten.Fehler), nameof(BioDaten.AbrufMeldung), nameof(BioDaten.Laedt), nameof(BioDaten.Zuletzt) }, daten);

        var gitter = new AdaptivGitter { Minimum = 300, Abstand = 12 };
        void Karten()
        {
            gitter.Children.Clear();
            foreach (var id in BioDaten.Reihenfolge())
            {
                var g = BioDaten.Groessen.First(x => x.Id == id);
                var (wert, verlauf) = daten.Zu(g);
                if (wert == null && (verlauf == null || verlauf.Punkte.Count == 0)) continue;
                var k = new BioKarte(g, wert, verlauf) { Tag = id };
                gitter.Children.Add(k);
            }
        }
        Ziehen.Ordnen(gitter, ids =>
        {
            // Ausgeblendete (ohne Daten) behalten ihren Platz hinten.
            var rest = BioDaten.Reihenfolge().Where(x => !ids.Contains(x));
            BioDaten.ReihenfolgeSpeichern(ids.Concat(rest));
            Log("biomarker", "Kartenreihenfolge geändert", wo: "Biomarker");
        });
        var kartenPlatz = new Reaktiv(() => { Karten(); return null; }, new[] { nameof(BioDaten.Stand) }, daten);

        var tagText = new MarkdownText(daten.TagText ?? "", 13);
        var tagKarte = new KlappKarte("calendar", "Tageswerte", Farbe.Gold, () => V(12,
                new SegmentWahl(new[] { ("heute", "Heute"), ("gestern", "Gestern"), ("vorgestern", "Vorgestern") }, tag, w => { tag = w; Starte(() => daten.TagLaden(w)); }).Breite(320),
                new Reaktiv(() => { tagText.Text = daten.TagText ?? "Lade …"; return null; }, new[] { nameof(BioDaten.TagText) }, daten), tagText),
            untertitel: "Alle Werte eines Tages samt Trainings", offenGeaendert: an => { if (an) Starte(() => daten.TagLaden(tag)); });
        var auswertung = new KlappKarte("doc.text.fill", "Auswertung im Wortlaut", Farbe.Haupt,
            () => new Reaktiv(() => new MarkdownText(daten.AuswertungText ?? "", 13), new[] { nameof(BioDaten.AuswertungText) }, daten), untertitel: "So, wie Jarvis sie am Handy rechnet");

        var v = V(18, kopf, knoepfe, status, gitter, kartenPlatz, tagKarte, auswertung);
        v.Margin = new Thickness(36);
        Child = new Scroller(v);
        Loaded += (_, _) => Starte(() => daten.Laden());
    }
}

/// <summary>Eine Messgröße: Name, Quelle, Wert, Einordnung und der Verlauf als Balken mit Schnittlinie, der jüngste Tag kräftig.</summary>
public sealed class BioKarte : Border
{
    public BioKarte(BioGroesse g, BioWert? w, BioVerlauf? verlauf)
    {
        var einordnungFarbe = w?.Einordnung switch
        {
            { } e when e.Contains("besser") => Farbe.Gruen,
            { } e when e.Contains("schlechter") => Farbe.Rot,
            _ => Farbe.Text2,
        };
        var letzter = verlauf?.Punkte.LastOrDefault();
        var wertText = w?.Wert ?? (letzter is { } l && l.Tag != default ? $"{l.Wert:0.#} {verlauf!.Einheit}" : "–");
        var s = V(8,
            H(8, Tx(g.Titel, 14, FontWeights.SemiBold, null, zeilen: 1).Fuell(), Tx(w?.Quelle ?? verlauf?.Quelle ?? g.Gruppe, 11, null, Farbe.Text3, zeilen: 1)),
            H(10, Tx(wertText, 24, FontWeights.Bold), w?.Datum is { } d ? Tx("vom " + ListenArten.DatumKurz(d), 11, null, Farbe.Warnung) : null, Spacer()),
            w is { Einordnung.Length: > 0 } ? Tx(w.Einordnung, 12, FontWeights.SemiBold, einordnungFarbe) : null,
            w?.SchnittDavor is { } sd ? Tx($"Schnitt davor {sd}" + (w.Schnitte is { } sn ? $" · 7/30/90 Tage: {sn}" : ""), 11, null, Farbe.Text3) : null,
            verlauf is { Punkte.Count: > 1 } ? Balken(verlauf, w?.Richtung ?? "neutral") : null);
        Child = s;
        Padding = new Thickness(16);
        CornerRadius = new CornerRadius(18);
        Background = Farbe.Glas(0.055);
        BorderBrush = Farbe.Glas(0.10);
        BorderThickness = new Thickness(1);
        ToolTip = "Ziehen zum Umsortieren";
    }

    private static UIElement Balken(BioVerlauf v, string richtung)
    {
        const double hoehe = 64;
        var punkte = v.Punkte;
        var min = Math.Min(punkte.Min(p => p.Wert), v.Schnitt ?? double.MaxValue);
        var max = Math.Max(punkte.Max(p => p.Wert), v.Schnitt ?? double.MinValue);
        var unten = min - (max - min) * 0.25;
        var spanne = Math.Max(1e-9, max - unten);
        var flaeche = new Grid { Height = hoehe, ClipToBounds = true };
        var reihe = new UniformGrid1(punkte.Count);
        for (var i = 0; i < punkte.Count; i++)
        {
            var (tag, wert) = punkte[i];
            var besser = v.Schnitt is { } sch && richtung != "neutral" && (richtung == "besser" ? wert >= sch : wert <= sch);
            var farbe = i == punkte.Count - 1 ? Farbe.Haupt : richtung == "neutral" || v.Schnitt == null ? Farbe.Mit(Farbe.Haupt, 0.45) : besser ? Farbe.Mit(Farbe.Gruen, 0.6) : Farbe.Mit(Farbe.Rot, 0.55);
            var b = new Rectangle { Fill = farbe, RadiusX = 2, RadiusY = 2, Height = Math.Max(2, (wert - unten) / spanne * hoehe), VerticalAlignment = VerticalAlignment.Bottom, Margin = new Thickness(1, 0, 1, 0) };
            b.ToolTip = $"{tag:dd.MM.}: {wert:0.##} {v.Einheit}";
            reihe.Children.Add(b);
        }
        flaeche.Children.Add(reihe);
        if (v.Schnitt is { } schnitt)
        {
            var y = hoehe - (schnitt - unten) / spanne * hoehe;
            flaeche.Children.Add(new Rectangle { Height = 1, Fill = Farbe.Text2, VerticalAlignment = VerticalAlignment.Top, Margin = new Thickness(0, Math.Clamp(y, 0, hoehe - 1), 0, 0), Opacity = 0.7, IsHitTestVisible = false });
        }
        var von = punkte[0].Tag.ToString("dd.MM.");
        var bis = punkte[^1].Tag.ToString("dd.MM.");
        return V(4, flaeche, H(6, Tx(von, 10, null, Farbe.Text3), Spacer(), v.Schnitt is { } sc ? Tx($"Schnitt {sc:0.##} {v.Einheit}", 10, null, Farbe.Text3) : null, Spacer(), Tx(bis, 10, null, Farbe.Text3)));
    }
}

/// <summary>Gleich breite Spalten nebeneinander, unten ausgerichtet.</summary>
public sealed class UniformGrid1 : Panel
{
    private readonly int anzahl;
    public UniformGrid1(int anzahl) { this.anzahl = Math.Max(1, anzahl); }
    protected override Size MeasureOverride(Size verfuegbar)
    {
        var b = double.IsInfinity(verfuegbar.Width) ? 300 : verfuegbar.Width;
        var h = double.IsInfinity(verfuegbar.Height) ? 64 : verfuegbar.Height;
        foreach (UIElement c in InternalChildren) c.Measure(new Size(b / anzahl, h));
        return new Size(b, h);
    }
    protected override Size ArrangeOverride(Size endgroesse)
    {
        var w = endgroesse.Width / anzahl;
        var i = 0;
        foreach (UIElement c in InternalChildren) { c.Arrange(new Rect(i * w, 0, w, endgroesse.Height)); i++; }
        return endgroesse;
    }
}

/// <summary>Startkarte „Biomarker“ in der Zentrale, wie BiomarkerStartKarte am Handy: die Kerngrößen mit Einordnung, Klick öffnet den Bereich.</summary>
public static class BiomarkerKarte
{
    public static UIElement Bauen()
    {
        var d = BioDaten.Shared;
        return new Reaktiv(() =>
        {
            var kopf = H(8, new KartenTitel("bolt.heart.fill", "Biomarker", Farbe.Rot).Fuell(), d.Laedt ? new Spinner(16) : null,
                new RundKnopf("arrow.clockwise", () => Starte(() => d.Laden(erzwingen: true)), hilfe: "Vom Handy neu lesen", groesse: 30),
                new RundKnopf("arrow.up.right.square", () => AppSteuerung.Shared.Bereich = Bereich.Biomarker, hilfe: "Biomarker-Bereich öffnen", groesse: 30));
            var s = V(10, kopf);
            if (d.Kopf.FirstOrDefault(k => k.StartsWith("Letzter Abruf", StringComparison.Ordinal)) is { } abruf) s.Children.Add(Tx(abruf, 12, null, Farbe.Text3, zeilen: 2));
            var gitter = new AdaptivGitter { Minimum = 150, Abstand = 8 };
            foreach (var id in BioDaten.Kern)
            {
                var g = BioDaten.Groessen.First(x => x.Id == id);
                var (w, v) = d.Zu(g);
                if (w == null) continue;
                var farbe = w.Einordnung.Contains("besser") ? Farbe.Gruen : w.Einordnung.Contains("schlechter") ? Farbe.Rot : Farbe.Text3;
                gitter.Children.Add(Box(V(2, Tx(g.Titel, 12, null, Farbe.Text2, zeilen: 1), Tx(w.Wert, 17, FontWeights.Bold, null, zeilen: 1), Tx(w.Einordnung, 11, null, farbe, zeilen: 1)),
                    Farbe.Glas(0.04), 12, 12, 10));
            }
            if (gitter.Children.Count > 0) s.Children.Add(gitter);
            else s.Children.Add(Tx(d.Laedt ? "Lade die Biomarker vom Handy …" : d.Fehler ?? "Noch keine Werte vom Handy.", 13, null, Farbe.Text2));
            if (d.Fehler is { } f && gitter.Children.Count > 0) s.Children.Add(Tx(f, 12, null, Farbe.Warnung));
            return new GlasKarte(s);
        }, new[] { nameof(BioDaten.Stand), nameof(BioDaten.Laedt), nameof(BioDaten.Fehler) }, d);
    }
}

/// <summary>Startkarte „Solarbatterie“ in der Zentrale, wie VictronStartKarte am Handy: BMV-712 und Laderegler mit Aktualisieren-Knopf.</summary>
public static class SolarKarte
{
    public static UIElement Bauen()
    {
        var s = SolarDaten.Shared;
        return new Reaktiv(() =>
        {
            var kopf = H(8, V(2, new KartenTitel("sun.max.fill", "Solarbatterie", Farbe.Gold), Tx(s.Laeuft ? "Liest die Anlage …" : s.StandText, 12, null, s.Laeuft ? Farbe.Haupt : Farbe.Text3, zeilen: 1)).Fuell(),
                s.Laeuft ? new Spinner(16) : null,
                new RundKnopf("arrow.clockwise", () => Starte(s.Lesen), hilfe: s.Laeuft ? "Solarbatterie wird gelesen" : "Solarbatterie aktualisieren", aktiv: s.Laeuft, groesse: 30));
            var v = V(10, kopf);
            if (s.Fehler is { } f && !s.Laeuft) v.Children.Add(Tx(f, 12, null, Farbe.Warnung));
            if (s.Geraet("batteriemonitor") is { } b)
            {
                if (b.Zahl("ladezustand_prozent") is { } soc) v.Children.Add(Ladebalken(soc, b.Zahl("strom_a") ?? 0));
                v.Children.Add(Abschnitt("Batterie (BMV-712)", SolarDaten.BatterieZeilen(b)));
            }
            if (s.Geraet("laderegler") is { } l) v.Children.Add(Abschnitt("Laderegler (MPPT)", SolarDaten.ReglerZeilen(l)));
            return new GlasKarte(v);
        }, new[] { nameof(SolarDaten.Stand), nameof(SolarDaten.Laeuft), nameof(SolarDaten.Fehler) }, s);
    }

    private static UIElement Ladebalken(double soc, double strom)
    {
        var farbe = soc >= 60 ? Farbe.Gruen : soc >= 30 ? Farbe.Gold : Farbe.Rot;
        var g = new Grid { Height = 14 };
        g.Children.Add(new Border { CornerRadius = new CornerRadius(7), Background = Farbe.Glas(0.08) });
        var voll = new Border { CornerRadius = new CornerRadius(7), Background = farbe, HorizontalAlignment = HorizontalAlignment.Left };
        g.SizeChanged += (_, _) => voll.Width = Math.Max(14, g.ActualWidth * Math.Clamp(soc, 0, 100) / 100);
        g.Children.Add(voll);
        return V(4, g, Tx($"{soc:0.#} %" + (strom > 0.05 ? " · lädt" : strom < -0.05 ? " · entlädt" : ""), 12, FontWeights.SemiBold, farbe));
    }

    private static UIElement Abschnitt(string titel, List<(string, string)> zeilen)
    {
        var v = V(4, Tx(titel, 13, FontWeights.SemiBold, Farbe.Haupt));
        foreach (var (name, wert) in zeilen) v.Children.Add(H(8, Tx(name, 13, null, Farbe.Text2).Fuell(), Tx(wert, 13, FontWeights.SemiBold)));
        return v;
    }
}
