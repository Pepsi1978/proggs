using System.ComponentModel;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Media.Imaging;
using System.Windows.Threading;
using static JarvisWindows.Ui;

namespace JarvisWindows;

/// <summary>Die Sprechblase unten, auf dem Startbildschirm und im Gespräch gleich und wie am Handy: eingeklappt nur die untere Zeile mit Plus, Modell, Mikrofon und genau einem Knopf
/// (Abbrechen, solange Jarvis arbeitet, der Pfeil, sobald Text oder ein Anhang da ist, sonst Live). Der freie Platz oder das Mikrofon öffnet sie nach oben, ein weiterer Druck
/// verkleinert sie wieder; mit Text bleibt sie offen und zeigt den Papierkorb. Ohne gespraech (Startbildschirm) beginnt das Absenden ein neues Gespräch.</summary>
public sealed class EingabeBlase : Decorator
{
    private readonly Gespraech? gespraech;
    private readonly AppSteuerung steuerung = AppSteuerung.Shared;
    private readonly TextFeld feld;
    private readonly Grid feldZeile = new() { ClipToBounds = true };
    private readonly Border kapsel;
    private readonly Decorator anhangPlatz = new();
    private readonly Decorator diktatPlatz = new();
    private readonly TextBlock fehlerText;
    private bool diktiert;
    private string diktatFehler = "";
    private bool nachDiktatSenden;
    private bool weg;
    private bool angemeldet;
    private bool? kompaktGezeigt;
    private readonly Border frei;

    private bool HatText => steuerung.EingabeText.Trim().Length > 0;
    private bool Kompakt => steuerung.BlaseKompakt && steuerung.EingabeText.Length == 0;

    public EingabeBlase(Gespraech? g)
    {
        gespraech = g;
        feld = new TextFeld("Frag Jarvis oder sag, was er am PC tun soll …  (⏎ senden, ⇧⏎ neue Zeile)", 15, mehrzeilig: true, text: steuerung.EingabeText,
            schreiben: t => { if (steuerung.EingabeText != t) steuerung.EingabeText = t; });
        feld.Box.MaxLines = 10;
        feld.Box.MinLines = 1;
        feld.Box.VerticalScrollBarVisibility = ScrollBarVisibility.Hidden;
        feld.Margin = new Thickness(0, 8, 0, 8);
        feld.Box.PreviewKeyDown += (_, e) =>
        {
            if (e.Key != Key.Return) return;
            if ((Keyboard.Modifiers & ModifierKeys.Shift) != 0) return;
            e.Handled = true;
            Senden();
        };

        // Eingeklappt bleibt das Feld unsichtbar im Fokus: Wer einfach lostippt, öffnet die Blase damit.
        feldZeile.ColumnDefinitions.Add(new ColumnDefinition());
        feldZeile.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        feldZeile.Margin = new Thickness(8, 0, 0, 0);
        feldZeile.Children.Add(feld);
        var leeren = new Reaktiv(LeerenKnopf, new[] { nameof(AppSteuerung.EingabeText), nameof(AppSteuerung.GeleerterText), nameof(Diktat.Aktiv) }, steuerung, Diktat.Shared);
        leeren.Margin = new Thickness(6, 4, 0, 0);
        leeren.VerticalAlignment = VerticalAlignment.Top;
        Grid.SetColumn(leeren, 1);
        feldZeile.Children.Add(leeren);

        // Wie am Handy schaltet der freie Platz zwischen Plus und Modell um.
        frei = new Border { Background = Farbe.Klar, Height = 34 }.Fuell();
        frei.Klick(Umschalten);
        var untereZeile = H(10, PlusMenue(), frei, new ModellKapsel(), DiktatKnopfBauen(),
            new Reaktiv(RechterKnopf, new[] { nameof(AppSteuerung.EingabeText), nameof(AppSteuerung.Anhang), nameof(AppSteuerung.Fragen), nameof(AppSteuerung.GespraechInhalt), nameof(LiveGespraech.Zustand) }, steuerung, LiveGespraech.Shared));

        kapsel = new Border
        {
            CornerRadius = new CornerRadius(24), Background = Farbe.Glas(0.07), BorderThickness = new Thickness(1), BorderBrush = Farbe.Glas(0.1), Padding = new Thickness(10, 6, 8, 6),
            Child = V(0, feldZeile, untereZeile),
        };
        fehlerText = Tx("", 12, null, Farbe.Warnung);
        fehlerText.Visibility = Visibility.Collapsed;
        var inhalt = V(8, anhangPlatz.Sichtbar(false), diktatPlatz.Sichtbar(false), fehlerText, kapsel);
        Child = new Border { Child = inhalt, MaxWidth = 940, Padding = new Thickness(40, 6, 40, 22) };

        feld.Box.GotKeyboardFocus += (_, _) => RandAbgleich();
        feld.Box.LostKeyboardFocus += (_, _) => RandAbgleich();
        SizeChanged += (_, e) => steuerung.BlaseHoehe = e.NewSize.Height;
        Loaded += (_, _) =>
        {
            weg = false;
            if (!angemeldet)
            {
                angemeldet = true;
                steuerung.PropertyChanged += SteuerungGeaendert;
                Diktat.Shared.PropertyChanged += DiktatGeaendert;
                SprachSchleife.Shared.PropertyChanged += DiktatGeaendert;
            }
            KompaktAbgleich(false);
            AnhangZeichnen();
            DiktatAbgleich();
            FokusNehmen();
            // Beim Fensterstart nimmt sonst das erste Textfeld der Leiste den Fokus.
            Starte(async () => { await Task.Delay(150); if (!weg) FokusNehmen(); });
        };
        Unloaded += (_, _) =>
        {
            weg = true;
            angemeldet = false;
            steuerung.PropertyChanged -= SteuerungGeaendert;
            Diktat.Shared.PropertyChanged -= DiktatGeaendert;
            SprachSchleife.Shared.PropertyChanged -= DiktatGeaendert;
        };
    }

    private void SteuerungGeaendert(object? s, PropertyChangedEventArgs e)
    {
        switch (e.PropertyName)
        {
            case nameof(AppSteuerung.EingabeText):
                if (feld.Text != steuerung.EingabeText) { feld.Text = steuerung.EingabeText; feld.Box.CaretIndex = feld.Text.Length; }
                KompaktAbgleich(true);
                break;
            case nameof(AppSteuerung.BlaseKompakt):
                KompaktAbgleich(true);
                break;
            case nameof(AppSteuerung.BlaseFokus):
                FokusNehmen();
                break;
            case nameof(AppSteuerung.Anhang):
                AnhangZeichnen();
                break;
        }
    }

    private void DiktatGeaendert(object? s, PropertyChangedEventArgs e)
    {
        if (e.PropertyName is nameof(Diktat.Aktiv) or nameof(SprachSchleife.Zustand) or null) DiktatAbgleich();
    }

    private void DiktatAbgleich()
    {
        var zeige = Diktat.Shared.Aktiv && (diktiert || SprachSchleife.Shared.Zustand == SprachZustand.Hoert);
        if (zeige == diktatPlatz.IsVisible && diktatPlatz.Child != null) return;
        diktatPlatz.Visibility = zeige ? Visibility.Visible : Visibility.Collapsed;
        diktatPlatz.Child = zeige ? new DiktatStatus(13, 2) : null;
    }

    private void FokusNehmen() => Spaeter(() => { feld.Box.Focus(); Keyboard.Focus(feld.Box); });

    private void RandAbgleich() => kapsel.BorderBrush = Farbe.Glas(feld.Box.IsKeyboardFocused && !Kompakt ? 0.2 : 0.1);

    /// <summary>Klappt die Feldzeile mit 0,18 s ein oder aus (.animation(.easeOut(duration: 0.18), value: kompakt)).</summary>
    private void KompaktAbgleich(bool animiert)
    {
        var k = Kompakt;
        if (kompaktGezeigt == k) return;
        kompaktGezeigt = k;
        feldZeile.IsHitTestVisible = !k;
        feldZeile.Opacity = k ? 0 : 1;
        frei.Hilfe(k ? "Eingabe öffnen" : "Eingabe verkleinern");
        RandAbgleich();
        var kurve = new QuadraticEase { EasingMode = EasingMode.EaseOut };
        if (!animiert) { feldZeile.BeginAnimation(MaxHeightProperty, null); feldZeile.MaxHeight = k ? 0 : double.PositiveInfinity; return; }
        if (k)
        {
            var a = new DoubleAnimation(feldZeile.ActualHeight, 0, new Duration(TimeSpan.FromMilliseconds(180))) { EasingFunction = kurve };
            feldZeile.BeginAnimation(MaxHeightProperty, a);
        }
        else
        {
            var a = new DoubleAnimation(0, 56, new Duration(TimeSpan.FromMilliseconds(180))) { EasingFunction = kurve, FillBehavior = FillBehavior.Stop };
            a.Completed += (_, _) => { if (!Kompakt) feldZeile.MaxHeight = double.PositiveInfinity; };
            feldZeile.MaxHeight = double.PositiveInfinity;
            feldZeile.BeginAnimation(MaxHeightProperty, a);
        }
    }

    private void Umschalten()
    {
        if (Kompakt) { steuerung.BlaseKompakt = false; FokusNehmen(); }
        else if (!HatText) { steuerung.BlaseKompakt = true; steuerung.EingabeText = ""; }
        else FokusNehmen();
    }

    private void AnhangZeichnen()
    {
        if (steuerung.Anhang is not { } a || BildHelfer.Bild(a, 140) is not { } bild) { anhangPlatz.Child = null; anhangPlatz.Visibility = Visibility.Collapsed; return; }
        anhangPlatz.Visibility = Visibility.Visible;
        var x = Sym.Bild("xmark.circle.fill", 15, Farbe.Text2);
        x.IsHitTestVisible = true;
        x.Background = Farbe.Klar;
        x.Klick(() => steuerung.Anhang = null);
        anhangPlatz.Child = H(8, RundBild(bild, 70, 10), x, Spacer());
    }

    /// <summary>Fester Platz wie am Handy, damit die Blase beim ersten Zeichen nicht springt: mit Text der Papierkorb, gleich danach „Rückgängig“.</summary>
    private UIElement LeerenKnopf()
    {
        var platz = new Grid { Width = 30, Height = 30 };
        if (HatText && !Diktat.Shared.Aktiv)
        {
            platz.Children.Add(Sym.Bild("trash", 13, Farbe.Text3));
            platz.Klick(() =>
            {
                steuerung.GeleerterText = steuerung.EingabeText;
                var gemerkt = steuerung.GeleerterText;
                steuerung.EingabeText = "";
                steuerung.GeleerterText = gemerkt;
                FokusNehmen();
            }).Hilfe("Text löschen");
        }
        else if (steuerung.EingabeText.Length == 0 && steuerung.GeleerterText is { } alt)
        {
            platz.Children.Add(Sym.Bild("arrow.uturn.backward", 13, Farbe.Haupt));
            platz.Klick(() => { steuerung.EingabeText = alt; FokusNehmen(); }).Hilfe("Gelöschten Text zurückholen");
        }
        return platz;
    }

    private FrameworkElement PlusMenue()
    {
        var knopf = new Border { Width = 34, Height = 34, CornerRadius = new CornerRadius(17), Background = Farbe.Glas(0.07), Child = Sym.Bild("plus", 15, Farbe.Text) };
        knopf.Klick(() =>
        {
            var e = new List<MenuEintrag>
            {
                new("Bildschirmfoto anhängen", () => Starte(async () => steuerung.Anhang = await Task.Run(Bildschirmfoto))),
                new("Bild auswählen …", BildWaehlen),
                MenuEintrag.Linie,
                new("An den Programmierer übergeben", steuerung.ProgrammiererUmschalten),
            };
            if (steuerung.NachtragMoeglich) e.Add(new MenuEintrag("Nachtrag", steuerung.NachtragUmschalten));
            JarvisMenue.Zeige(knopf, e);
        });
        return knopf;
    }

    private UIElement DiktatKnopfBauen() => new DiktatKnopf(new TextBindung(() => steuerung.EingabeText, t => steuerung.EingabeText = t), vorschau: false, meldeLaeuft: laeuft =>
    {
        if (weg) return;
        diktiert = laeuft;
        if (laeuft) steuerung.BlaseKompakt = false;
        else
        {
            FokusNehmen();
            // Kam nichts an, verkleinert das Stoppen die Blase wieder.
            if (!HatText) { steuerung.BlaseKompakt = true; steuerung.EingabeText = ""; }
        }
        DiktatAbgleich();
        if (!laeuft && nachDiktatSenden) { nachDiktatSenden = false; Senden(); }
    }, meldeFehler: f => { diktatFehler = f; FehlerZeigen(); });

    private void FehlerZeigen()
    {
        fehlerText.Text = diktatFehler;
        fehlerText.Visibility = diktatFehler.Length > 0 ? Visibility.Visible : Visibility.Collapsed;
    }

    private UIElement RechterKnopf()
    {
        if (gespraech != null && steuerung.Abbrechbar(gespraech))
            return new RundKnopf("stop.fill", () => steuerung.Abbrechen(gespraech), hilfe: "Abbrechen", aktiv: true, farbe: Farbe.Haupt);
        if (HatText || steuerung.Anhang != null)
            return new RundKnopf("arrow.up", Senden, hilfe: "Senden", aktiv: true, farbe: Farbe.Haupt);
        var an = LiveGespraech.Shared.Aktiv;
        var farbe = an ? Farbe.Schwarz : Farbe.Text;
        return new Border
        {
            Height = 34, Padding = new Thickness(12, 0, 12, 0), CornerRadius = new CornerRadius(17), Background = an ? Farbe.Gold : Farbe.Glas(0.07), BorderBrush = Farbe.Glas(0.10), BorderThickness = new Thickness(1),
            Child = H(5, Sym.Bild("waveform", 12, farbe), Tx("Live", 12, FontWeights.SemiBold, farbe)),
        }.Klick(() => steuerung.LiveOeffnen(gespraech)).Hilfe("Live-Bildschirm öffnen (Strg+L startet direkt)");
    }

    private void Senden()
    {
        // Läuft das Mikrofon noch, gehört das Gesagte zur Nachricht: erst fertig mitschreiben, dann senden.
        if (diktiert)
        {
            nachDiktatSenden = true;
            Diktat.Shared.Stoppen();
            return;
        }
        var t = steuerung.EingabeText.Trim();
        if (t.EndsWith(';')) t = t[..^1].Trim(' ', '\t');
        if (t.Length == 0 && steuerung.Anhang == null) return;
        diktatFehler = "";
        FehlerZeigen();
        if (ProgrammierVorspann.HatNachtrag(t) && gespraech?.AuftragId is { } id)
        {
            if (steuerung.Anhang != null) { diktatFehler = "Ein Nachtrag geht nur als Text. Bitte den Anhang entfernen."; FehlerZeigen(); return; }
            var rest = ProgrammierVorspann.OhneNachtrag(t).Trim();
            if (rest.Length == 0) { diktatFehler = "Bitte den Nachtrag hinter dem Doppelpunkt ergänzen."; FehlerZeigen(); return; }
            steuerung.NachtragSenden(gespraech, id, rest);
            return;
        }
        var neuerAuftrag = ProgrammierVorspann.HatAuftrag(t);
        if (neuerAuftrag && !ProgrammierVorspann.OhneAuftrag(t).Any(char.IsLetterOrDigit))
        {
            diktatFehler = "Bitte die Aufgabe hinter dem Doppelpunkt ergänzen.";
            FehlerZeigen();
            return;
        }
        var bild = steuerung.Anhang;
        steuerung.EingabeText = "";
        steuerung.Anhang = null;
        steuerung.BlaseKompakt = true;
        // Wie am Handy: Ein neuer Auftrag aus einem Programmiergespräch bekommt ein eigenes Gespräch, sonst vermischt er sich mit der laufenden Sitzung.
        steuerung.Gespraech = neuerAuftrag && gespraech?.IstProgrammierung == true ? null : gespraech;
        steuerung.Sende(t, bild: bild);
    }

    private async void BildWaehlen()
    {
        var d = new Microsoft.Win32.OpenFileDialog { Filter = "Bilder|*.png;*.jpg;*.jpeg;*.gif;*.bmp;*.webp;*.heic;*.tif;*.tiff|Alle Dateien|*.*" };
        if (d.ShowDialog() != true) return;
        var pfad = d.FileName;
        var jpg = await Task.Run(() => { try { return BildHelfer.JpgDaten(File.ReadAllBytes(pfad)); } catch { return null; } });
        if (jpg != null) steuerung.Anhang = jpg;
    }

    /// <summary>Hauptbildschirm als JPEG (höchstens 1440 px, Qualität 60), wie `screencapture -x -m` + `sips` am Mac.</summary>
    private static byte[]? Bildschirmfoto()
    {
        try
        {
            var s = System.Windows.Forms.Screen.PrimaryScreen!.Bounds;
            using var b = new System.Drawing.Bitmap(s.Width, s.Height, System.Drawing.Imaging.PixelFormat.Format32bppArgb);
            using (var g = System.Drawing.Graphics.FromImage(b)) g.CopyFromScreen(s.Left, s.Top, 0, 0, b.Size);
            var griff = b.GetHbitmap();
            try
            {
                var q = System.Windows.Interop.Imaging.CreateBitmapSourceFromHBitmap(griff, IntPtr.Zero, Int32Rect.Empty, BitmapSizeOptions.FromEmptyOptions());
                q.Freeze();
                return BildHelfer.JpgDaten(q, 1440, 60);
            }
            finally { DeleteObject(griff); }
        }
        catch (Exception ex)
        {
            Log("fehler", "Bildschirmfoto für den Anhang gescheitert", wo: "Eingabeblase", fehler: ex.Message);
            return null;
        }
    }

    [System.Runtime.InteropServices.DllImport("gdi32.dll")] private static extern bool DeleteObject(IntPtr h);
}
