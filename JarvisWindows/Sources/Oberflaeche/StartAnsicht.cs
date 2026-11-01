using System.Windows;
using System.Windows.Controls;
using static JarvisWindows.Ui;

namespace JarvisWindows;

/// <summary>Großer Startbildschirm: Kugel und Begrüßung, Schnell-Tags, unten die Blase mit Modell, Kosten und dem Knopf für den Live-Bildschirm.
/// Die Tagesauswertung steht nur in der Zentrale (die Daten werden hier schon vorgeladen); alles Weitere liegt unter Einstellungen.</summary>
public sealed class StartAnsicht : Grid
{
    public static readonly string[] SchnellFragen =
    {
        "Wie war mein Tag?", "Was steht heute an?", "Wie habe ich geschlafen?", "Prüfe meine Updates", "Wie ist das Wetter?", "Was steht morgen an?",
        "Welche Aufgaben sind offen?", "Wie ist meine Erholung?", "Wann ist mein nächster Dienst?", "Welche Termine habe ich?", "Wie ist meine HRV?",
        "Was ist neu in meinen Mails?", "Wie geht es meinem PC?", "Welche Programmieraufträge laufen?", "Wie sieht meine Woche aus?", "Welche Wecker sind gestellt?",
        "Welche Ideen sind offen?", "Wie lange brauche ich zur Arbeit?", "Wie viel Guthaben habe ich?", "Was habe ich gestern gemacht?", "Wie war mein letztes Training?",
        "Was nehme ich heute ein?", "Schau dir meinen Bildschirm an", "Was hast du dir gemerkt?",
    };

    /// <summary>150 Punkte (rund 3,5 cm) Luft zur Eingabezeile; in kleinen Fenstern weniger, damit Kugel und Begrüßung sichtbar bleiben.</summary>
    private static double AbstandZurBlase(double fensterHoehe) => Math.Min(150, Math.Max(12, fensterHoehe - 510));

    public StartAnsicht()
    {
        var daten = ZentraleDaten.Shared;
        var steuerung = AppSteuerung.Shared;
        RowDefinitions.Add(new RowDefinition());
        RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });

        var kugel = new KugelLive(150).Mittig().Klick(steuerung.KuerzelSprechen).Hilfe("Antippen zum Sprechen (Strg+Alt+J überall)");
        kugel.Margin = new Thickness(0, 30, 0, 0);
        var gruss = Tx(ZentraleAnsicht.Gruss, 34, FontWeights.Bold);
        var datum = Tx(ZentraleAnsicht.DatumZeile, 14, FontWeights.Medium, Farbe.Text2);
        datum.TextAlignment = TextAlignment.Center;
        var nichtDa = new Reaktiv(() =>
        {
            if (daten.LetzterFehlschlag == null || daten.Laedt) return null;
            var t = Tx("Das Handy-Jarvis ist gerade nicht erreichbar.", 13, null, Farbe.Warnung);
            t.TextAlignment = TextAlignment.Center;
            return t;
        }, new[] { nameof(ZentraleDaten.LetzterFehlschlag), nameof(ZentraleDaten.Laedt) }, daten);
        var oben = V(22, new ZugangsHinweis(), kugel, V(6, H(10, new LivePunkt(), gruss).Mittig(), datum, nichtDa));
        // 76 geräteunabhängige Punkte entsprechen ungefähr zwei Zentimetern.
        var scroller = new Scroller(new Border { Child = oben, MaxWidth = 884, Padding = new Thickness(64, 76, 64, 20) });
        Children.Add(scroller);

        // Fest über der Blase, damit die Tags ohne Scrollen zu sehen sind.
        var tags = new DreiZeilen { Abstand = 8 };
        foreach (var f in SchnellFragen) tags.Children.Add(new SchnellTag(f, () => { steuerung.Gespraech = null; steuerung.Sende(f); }));
        var tagRand = new Border { Child = tags, MaxWidth = 980, ClipToBounds = true, Padding = new Thickness(64, 10, 64, 150) };
        SetRow(tagRand, 1);
        Children.Add(tagRand);
        SizeChanged += (_, e) => tagRand.Padding = new Thickness(64, 10, 64, AbstandZurBlase(e.NewSize.Height));

        var blase = new EingabeBlase(null);
        SetRow(blase, 2);
        Children.Add(blase);
        Loaded += (_, _) => daten.Vorladen();
    }
}

internal sealed class SchnellTag : Border
{
    public SchnellTag(string titel, Action aktion)
    {
        Child = Tx(titel, 13, FontWeights.Medium, Farbe.Text, zeilen: 1);
        Padding = new Thickness(14, 8, 14, 8);
        this.KapselForm();
        BorderThickness = new Thickness(1);
        BorderBrush = Farbe.Glas(0.10);
        this.SchwebeFlaeche(Farbe.Glas(0.07), Farbe.Glas(0.13));
        this.Schwebe(an => BorderBrush = Farbe.Glas(an ? 0.18 : 0.10));
        this.Klick(aktion);
    }
}

/// <summary>Bricht bei voller Zeile um und zeigt in jeder Fensterbreite genau drei Zeilen; was nicht mehr hineinpasst, bleibt verborgen.</summary>
internal sealed class DreiZeilen : Panel
{
    public double Abstand { get; set; } = 8;
    private const int Zeilen = 3;

    /// <summary>Je Kind die Stelle innerhalb der Fläche, null = passt nicht mehr.</summary>
    private (List<Point?> Stellen, double Hoehe) Anordnung(double breite)
    {
        var stellen = new List<Point?>();
        double x = 0, y = 0, zeilenHoehe = 0;
        var zeile = 0;
        foreach (UIElement k in InternalChildren)
        {
            var g = k.DesiredSize;
            if (x > 0 && x + g.Width > breite)
            {
                zeile += 1;
                if (zeile >= Zeilen) { stellen.Add(null); continue; }
                x = 0;
                y += zeilenHoehe + Abstand;
                zeilenHoehe = 0;
            }
            if (zeile >= Zeilen) { stellen.Add(null); continue; }
            stellen.Add(new Point(x, y));
            x += g.Width + Abstand;
            zeilenHoehe = Math.Max(zeilenHoehe, g.Height);
        }
        return (stellen, y + zeilenHoehe);
    }

    protected override Size MeasureOverride(Size verfuegbar)
    {
        foreach (UIElement k in InternalChildren) k.Measure(new Size(double.PositiveInfinity, double.PositiveInfinity));
        var breite = double.IsInfinity(verfuegbar.Width) ? 700 : verfuegbar.Width;
        return new Size(breite, Anordnung(breite).Hoehe);
    }

    protected override Size ArrangeOverride(Size flaeche)
    {
        var stellen = Anordnung(flaeche.Width).Stellen;
        for (var i = 0; i < InternalChildren.Count; i++)
        {
            var k = InternalChildren[i];
            // Verborgene Kinder liegen weit außerhalb des Fensters.
            var p = stellen[i] ?? new Point(-100_000, -100_000);
            k.Arrange(new Rect(p, k.DesiredSize));
        }
        return flaeche;
    }
}
