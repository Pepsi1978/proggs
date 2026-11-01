using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Media.Effects;
using System.Windows.Threading;

namespace JarvisWindows;

/// <summary>DesignStand.Modus der Mac-Fassung; als eigener Typ, weil C# die Eigenschaft Modus sonst nicht zulässt.</summary>
public enum DesignModus { Hell, Automatik, Dunkel }

public static class DesignModusInfo
{
    extension(DesignModus m)
    {
        public string Titel => m switch { DesignModus.Hell => "Hellmodus", DesignModus.Automatik => "Automatik", _ => "Dunkelmodus" };
        public string Symbol => m switch { DesignModus.Hell => "sun.max.fill", DesignModus.Automatik => "circle.lefthalf.filled", _ => "moon.fill" };
        /// <summary>Hell → Automatik → Dunkel → Hell.</summary>
        public DesignModus Naechster => m switch { DesignModus.Hell => DesignModus.Automatik, DesignModus.Automatik => DesignModus.Dunkel, _ => DesignModus.Hell };
    }
}

/// <summary>Hell, Automatik oder Dunkel. Die Automatik wechselt bei Sonnenaufgang und Sonnenuntergang in Berlin, gerechnet auf dem PC, ohne Netz.</summary>
public sealed class DesignStand : Beobachtbar
{
    // Vor Shared: Der Konstruktor rechnet schon mit der Zeitzone.
    public static readonly TimeZoneInfo Berlin = LadeBerlin();
    public static DesignStand Shared { get; } = new();

    private DesignModus _modus;
    public DesignModus Modus
    {
        get => _modus;
        set
        {
            if (!Setze(ref _modus, value)) return;
            Vorgaben.Setze("darstellung", value.ToString().ToLowerInvariant());
            Log("einstellung", $"Darstellung = {value.ToString().ToLowerInvariant()}", wo: "Seitenleiste");
            Aktualisieren();
            Melde(nameof(Hilfe));
        }
    }
    private bool _dunkel;
    public bool Dunkel { get => _dunkel; private set => Setze(ref _dunkel, value); }

    private DispatcherTimer? uhr;

    private DesignStand()
    {
        _modus = Enum.TryParse<DesignModus>(Vorgaben.Text("darstellung") ?? "", true, out var m) ? m : DesignModus.Automatik;
        _dunkel = IstDunkel(_modus, DateTime.UtcNow);
    }

    public void Weiter() => Modus = Modus.Naechster;

    private string zeitenText = "";
    private int zeitenTag = -1;

    public string Hilfe
    {
        get
        {
            var tag = TimeZoneInfo.ConvertTimeFromUtc(DateTime.UtcNow, Berlin).DayOfYear;
            if (tag != zeitenTag)
            {
                var s = Sonne(DateTime.UtcNow);
                zeitenText = $"Berlin heute: Sonnenaufgang {TimeZoneInfo.ConvertTimeFromUtc(s.Auf, Berlin):HH:mm} Uhr, Sonnenuntergang {TimeZoneInfo.ConvertTimeFromUtc(s.Unter, Berlin):HH:mm} Uhr.";
                zeitenTag = tag;
            }
            return $"{Modus.Titel}. Antippen wechselt zu {Modus.Naechster.Titel}. {zeitenText}";
        }
    }

    /// <summary>Einmal beim Start: prüft in der Automatik jede halbe Minute, ob die Sonne auf- oder untergegangen ist.</summary>
    public void Starten()
    {
        Aktualisieren();
        if (uhr != null) return;
        uhr = new DispatcherTimer { Interval = TimeSpan.FromSeconds(30) };
        uhr.Tick += (_, _) => Aktualisieren();
        uhr.Start();
    }

    private void Aktualisieren() => Dunkel = IstDunkel(Modus, DateTime.UtcNow);

    private static bool IstDunkel(DesignModus m, DateTime jetztUtc)
    {
        switch (m)
        {
            case DesignModus.Hell: return false;
            case DesignModus.Dunkel: return true;
            default:
                var s = Sonne(jetztUtc);
                return jetztUtc < s.Auf || jetztUtc >= s.Unter;
        }
    }

    private static TimeZoneInfo LadeBerlin()
    {
        foreach (var id in new[] { "Europe/Berlin", "W. Europe Standard Time" })
            try { return TimeZoneInfo.FindSystemTimeZoneById(id); } catch { }
        return TimeZoneInfo.Local;
    }

    /// <summary>Sonnenaufgang und -untergang in Berlin (UTC) nach den NOAA-Näherungsformeln (Sonnenrand mit Lichtbrechung: 90,833°).</summary>
    public static (DateTime Auf, DateTime Unter) Sonne(DateTime datumUtc)
    {
        var ort = TimeZoneInfo.ConvertTimeFromUtc(datumUtc, Berlin);
        var tag = ort.DayOfYear;
        var mitternacht = new DateTime(ort.Year, ort.Month, ort.Day, 0, 0, 0, DateTimeKind.Utc);
        var g = 2 * Math.PI / 365 * (tag - 1);
        var gleichung = 229.18 * (0.000075 + 0.001868 * Math.Cos(g) - 0.032077 * Math.Sin(g) - 0.014615 * Math.Cos(2 * g) - 0.040849 * Math.Sin(2 * g));
        var dekl = 0.006918 - 0.399912 * Math.Cos(g) + 0.070257 * Math.Sin(g) - 0.006758 * Math.Cos(2 * g) + 0.000907 * Math.Sin(2 * g) - 0.002697 * Math.Cos(3 * g) + 0.00148 * Math.Sin(3 * g);
        double breite = 52.52 * Math.PI / 180, laenge = 13.405;
        var cosH = Math.Cos(90.833 * Math.PI / 180) / (Math.Cos(breite) * Math.Cos(dekl)) - Math.Tan(breite) * Math.Tan(dekl);
        var h = Math.Acos(Math.Min(1, Math.Max(-1, cosH))) * 180 / Math.PI;
        var auf = 720 - 4 * (laenge + h) - gleichung;
        var unter = 720 - 4 * (laenge - h) - gleichung;
        return (mitternacht.AddMinutes(auf), mitternacht.AddMinutes(unter));
    }
}

/// <summary>Grüner Punkt, solange das Handy-Jarvis erreichbar ist.</summary>
public sealed class LivePunkt : Decorator
{
    public LivePunkt(double groesse = 8)
    {
        Child = new Reaktiv(() =>
        {
            var s = AppSteuerung.Shared;
            var f = s.HandyVerbunden ? Farbe.Gruen : Farbe.Warnung;
            return new Border
            {
                Width = groesse, Height = groesse, CornerRadius = new CornerRadius(groesse / 2), Background = f, VerticalAlignment = VerticalAlignment.Center,
                Effect = new DropShadowEffect { Color = Farbe.Wert(f), BlurRadius = 8, ShadowDepth = 0, Opacity = 1 },
            }.Hilfe(s.HandyVerbunden ? "Mit dem Handy verbunden" : "Handy nicht erreichbar");
        }, new[] { nameof(AppSteuerung.HandyVerbunden) }, AppSteuerung.Shared);
    }
}
