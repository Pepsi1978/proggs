using System.Windows;
using System.Windows.Controls;
using System.Windows.Controls.Primitives;
using System.Windows.Input;
using System.Windows.Media;

namespace JarvisWindows;

/// <summary>Drag &amp; Drop zum Umsortieren, wie das Modul M1.2 am Handy: Karte an beliebiger Stelle greifen und ziehen; die anderen rücken live zur Seite,
/// Loslassen speichert die Reihenfolge. Geht für senkrechte Stapel und Gitter (der Platz unter dem Zeiger zählt). Textfelder und Bildlaufleisten bleiben bedienbar,
/// ein Klick ohne Bewegung bleibt ein Klick. Die Kennung jeder Karte steht in ihrem Tag.</summary>
public static class Ziehen
{
    private const double Schwelle = 8;
    /// <summary>Liegen Zieh-Flächen ineinander (Kacheln in einer Karte), gewinnt die innerste unter dem Zeiger: Das Drücken tunnelt von außen nach innen, die letzte setzt sich.</summary>
    private static Panel? tiefste;

    public static void Ordnen(Panel panel, Action<List<string>> gespeichert)
    {
        UIElement? kandidat = null, gezogen = null;
        Point start = default;
        var vorher = new List<string>();

        panel.PreviewMouseLeftButtonDown += (_, e) =>
        {
            if (e.OriginalSource is DependencyObject q && (IstEingabe(q))) return;
            kandidat = KindUnter(panel, e.OriginalSource as DependencyObject);
            start = e.GetPosition(panel);
            if (kandidat != null) tiefste = panel;
        };
        panel.PreviewMouseMove += (_, e) =>
        {
            if (kandidat == null || e.LeftButton != MouseButtonState.Pressed) { if (gezogen == null) kandidat = null; return; }
            if (gezogen == null && !ReferenceEquals(tiefste, panel)) return;
            var p = e.GetPosition(panel);
            if (gezogen == null)
            {
                if ((p - start).Length < Schwelle) return;
                gezogen = kandidat;
                vorher = Reihe(panel);
                // Die Maus übernimmt das Panel: Ein Knopf unter dem Zeiger verliert sie und löst beim Loslassen nichts aus.
                panel.CaptureMouse();
                gezogen.Opacity = 0.55;
                Panel.SetZIndex(gezogen, 10);
                Mouse.OverrideCursor = Cursors.SizeAll;
            }
            var ziel = KindAnPunkt(panel, p);
            if (ziel == null || ReferenceEquals(ziel, gezogen)) return;
            var von = panel.Children.IndexOf(gezogen);
            var nach = panel.Children.IndexOf(ziel);
            if (von < 0 || nach < 0) return;
            // Erst tauschen, wenn der Zeiger die Mitte des Ziels in Zugrichtung überschritten hat: Sonst läge er nach dem Tausch einer kleinen
            // Karte mit einer großen noch in der großen und tauschte sie beim nächsten Bewegen zurück (Flackern). In einer Zeile zählt die Breite.
            if (ziel is FrameworkElement zf && gezogen is FrameworkElement gf)
            {
                var zr = new Rect(zf.TranslatePoint(new Point(0, 0), panel), new Size(zf.ActualWidth, zf.ActualHeight));
                var gr = new Rect(gf.TranslatePoint(new Point(0, 0), panel), new Size(gf.ActualWidth, gf.ActualHeight));
                var gleicheZeile = Math.Abs(zr.Top - gr.Top) < Math.Min(zr.Height, gr.Height) / 2;
                var mitte = gleicheZeile ? zr.Left + zr.Width / 2 : zr.Top + zr.Height / 2;
                var lage = gleicheZeile ? p.X : p.Y;
                if (nach > von ? lage < mitte : lage > mitte) return;
            }
            panel.Children.RemoveAt(von);
            panel.Children.Insert(nach, gezogen);
            e.Handled = true;
        };
        void Ende()
        {
            kandidat = null;
            if (ReferenceEquals(tiefste, panel) && gezogen == null) tiefste = null;
            if (gezogen == null) return;
            gezogen.Opacity = 1;
            Panel.SetZIndex(gezogen, 0);
            gezogen = null;
            if (ReferenceEquals(tiefste, panel)) tiefste = null;
            Mouse.OverrideCursor = null;
            if (panel.IsMouseCaptured) panel.ReleaseMouseCapture();
            var neu = Reihe(panel);
            if (!neu.SequenceEqual(vorher)) gespeichert(neu);
        }
        panel.PreviewMouseLeftButtonUp += (_, e) => { var lief = gezogen != null; Ende(); if (lief) e.Handled = true; };
        panel.LostMouseCapture += (_, _) => { if (gezogen != null) Ende(); };
    }

    private static List<string> Reihe(Panel panel) => panel.Children.OfType<FrameworkElement>().Select(c => c.Tag as string).Where(t => t != null).Select(t => t!).ToList();

    private static bool IstEingabe(DependencyObject q)
    {
        for (var d = q; d != null; d = VisualTreeHelper.GetParent(d) ?? LogicalTreeHelper.GetParent(d))
            if (d is TextBoxBase or ScrollBar or PasswordBox or ComboBox) return true;
        return false;
    }

    /// <summary>Das direkte Kind des Panels, in dem das Element liegt.</summary>
    private static UIElement? KindUnter(Panel panel, DependencyObject? d)
    {
        while (d != null)
        {
            var eltern = VisualTreeHelper.GetParent(d) ?? LogicalTreeHelper.GetParent(d);
            if (ReferenceEquals(eltern, panel)) return d as FrameworkElement is { Tag: string } f ? f : null;
            d = eltern;
        }
        return null;
    }

    /// <summary>Das Kind, dessen Fläche den Punkt enthält; unter allen Karten das nächste, damit auch Lücken zwischen ihnen zählen.</summary>
    private static UIElement? KindAnPunkt(Panel panel, Point p)
    {
        UIElement? best = null;
        var bestAbstand = double.MaxValue;
        foreach (UIElement c in panel.Children)
        {
            if (c is not FrameworkElement f || f.Tag is not string || f.Visibility != Visibility.Visible || f.ActualHeight <= 0) continue;
            var oben = f.TranslatePoint(new Point(0, 0), panel);
            var r = new Rect(oben, new Size(f.ActualWidth, f.ActualHeight));
            if (r.Contains(p)) return c;
            var dx = Math.Max(0, Math.Max(r.Left - p.X, p.X - r.Right));
            var dy = Math.Max(0, Math.Max(r.Top - p.Y, p.Y - r.Bottom));
            var abstand = dx * dx + dy * dy;
            if (abstand < bestAbstand) { bestAbstand = abstand; best = c; }
        }
        return best;
    }
}
