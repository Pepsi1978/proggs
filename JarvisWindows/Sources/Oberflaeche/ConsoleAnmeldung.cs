using System.Globalization;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Text.RegularExpressions;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Threading;
using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.Wpf;
using static JarvisWindows.Bau;

namespace JarvisWindows;

/// <summary>Eigene Anmeldung des PCs bei der Claude Console. Das Guthaben gibt es nur mit dem Sitzungs-Cookie, nicht mit einem API-Schlüssel
/// (inoffizielle Schnittstelle, wie am Handy in AnthropicGuthabenFaehigkeit.kt). Das Anmeldefenster arbeitet flüchtig, die Cookies liegen nur im Tresor.</summary>
public static class ConsoleSitzung
{
    public sealed class Fehler : Exception
    {
        public bool IstAbgelaufen { get; }
        private Fehler(string text, bool abgelaufen) : base(text) { IstAbgelaufen = abgelaufen; }
        public static Fehler Abgelaufen => new("Die Anthropic-Konsole lehnt die Anmeldung gerade ab. Sie bleibt gespeichert und Jarvis versucht es weiter; hilft das nicht, bitte neu anmelden.", true);
        public static Fehler Meldung(string t) => new(t, false);
    }

    public sealed class Keks
    {
        public string Name { get; set; } = "";
        public string Wert { get; set; } = "";
        public string Domain { get; set; } = "";
    }

    /// <summary>Satz: derselbe wie das Werkzeug des Handys, für die Punkte auf der Karte.</summary>
    public sealed record Stand_(double Rest, double? Gewaehrt, double? Kurs, string Satz);

    private const string Console = "https://platform.claude.com";
    public static readonly Uri LoginURL = new(Console + "/login");
    /// <summary>Fest für Anmeldung und Abfrage: Die Bot-Abwehr der Console bindet ihr Cookie an die Browserkennung. Passend zur Chromium-Engine von WebView2.</summary>
    public const string BrowserKennung = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36";
    private const string OrgSchluessel = "consoleOrganisation";
    private const string TresorName = "console-sitzung";
    private static (double Wert, string Datum, DateTime Geholt)? kursStand;
    private static readonly HttpClient Netz = new(new SocketsHttpHandler
    {
        UseCookies = false, AllowAutoRedirect = true, AutomaticDecompression = DecompressionMethods.All, PooledConnectionLifetime = TimeSpan.FromMinutes(10),
        ConnectCallback = VerbindeIPv4Zuerst,
    }) { Timeout = TimeSpan.FromSeconds(25) };

    /// <summary>Ein totes IPv6 kostet sonst die ganze Wartezeit (best-practices: HTTP-Timeouts).</summary>
    private static async ValueTask<Stream> VerbindeIPv4Zuerst(SocketsHttpConnectionContext k, CancellationToken ct)
    {
        var adressen = await Dns.GetHostAddressesAsync(k.DnsEndPoint.Host, ct);
        Exception? letzter = null;
        foreach (var a in adressen.OrderBy(x => x.AddressFamily == System.Net.Sockets.AddressFamily.InterNetwork ? 0 : 1))
        {
            var s = new System.Net.Sockets.Socket(a.AddressFamily, System.Net.Sockets.SocketType.Stream, System.Net.Sockets.ProtocolType.Tcp) { NoDelay = true };
            try
            {
                using var frist = CancellationTokenSource.CreateLinkedTokenSource(ct);
                frist.CancelAfter(TimeSpan.FromSeconds(8));
                await s.ConnectAsync(a, k.DnsEndPoint.Port, frist.Token);
                return new System.Net.Sockets.NetworkStream(s, true);
            }
            catch (Exception ex) when (!ct.IsCancellationRequested) { s.Dispose(); letzter = ex; }
        }
        throw letzter ?? new HttpRequestException("Keine Adresse für " + k.DnsEndPoint.Host);
    }

    private static List<Keks> kekse = Starte();
    private static List<Keks> Starte()
    {
        var k = Tresor.Lade<List<Keks>>(TresorName) ?? new List<Keks>();
        GeheimnisseMelden(k);
        return k;
    }
    /// <summary>Zählt jede An- und Abmeldung; ein Abruf, der über eine davon läuft, ist ungültig.</summary>
    public static int Nummer { get; private set; }

    public static bool Angemeldet => kekse.Any(k => k.Name == "sessionKey");

    private static bool GehoertZurConsole(string domain)
    {
        var d = domain.StartsWith('.') ? domain[1..] : domain;
        return d == "claude.com" || d.EndsWith(".claude.com", StringComparison.Ordinal);
    }

    private static void GeheimnisseMelden(List<Keks> k) => Geheimnisse.Setzen(TresorName, k.Select(x => x.Wert));

    private static void Sichern()
    {
        Tresor.Sichere(kekse, TresorName);
        GeheimnisseMelden(kekse);
    }

    public static void Uebernehmen(IEnumerable<Keks> cookies)
    {
        kekse = cookies.Where(c => GehoertZurConsole(c.Domain)).ToList();
        Nummer += 1;
        Vorgaben.Setze(OrgSchluessel, null);   // Das neue Konto kann eine andere Organisation haben.
        Sichern();
        Log("guthaben", "Am PC bei der Console angemeldet", wo: "Guthaben");
    }

    /// <summary>Am Mac holte das eine alte Sitzung aus dem Cookie-Speicher von WebKit. Hier gab es nie einen anderen Ort als den Tresor: immer false.</summary>
    public static Task<bool> AltbestandUebernehmen() => Task.FromResult(false);

    public static void Abmelden()
    {
        kekse = new List<Keks>();
        Nummer += 1;
        Sichern();
        Vorgaben.Setze(OrgSchluessel, null);
    }

    public static async Task<Stand_> Stand()
    {
        var org = await Organisation();
        var c = await Holen($"{Console}/api/organizations/{org}/prepaid/credits") as JSONObjekt;
        if (c == null || (c.Obj("balance") == null && c.Zahl("amount") == null))
            throw Fehler.Meldung("Die Anthropic-Konsole lieferte keine lesbare Antwort (Schnittstelle geändert?).");
        var rest = Betrag(c.Obj("balance"), c.Zahl("amount") ?? 0);
        // Nur gültige Tranchen mit Rest zählen, abgelaufene Vormonate verfälschen sonst „gewährt“ und das Verfallsdatum.
        var gewaehrt = 0.0;
        DateTime? verfaellt = null;
        foreach (var feld in new[] { "tranches", "promo_tranches" })
        {
            foreach (var t in c.Objekte(feld))
            {
                var ende = t.Text("expires_at") is { } s ? Zeitpunkt(s) : null;
                if (ende != null && ende <= DateTime.Now) continue;
                if (Betrag(t.Obj("remaining"), t.Zahl("remaining_amount_minor_units") ?? 0) <= 0) continue;
                gewaehrt += Betrag(t.Obj("granted"), t.Zahl("granted_amount_minor_units") ?? 0);
                if (ende != null && (verfaellt == null || ende < verfaellt)) verfaellt = ende;
            }
        }
        Func<double, string> b = Guthaben.Betrag;
        var k = await Kurs();
        var satz = $"Übrig: {b(rest)} $";
        if (k != null) satz += $" (≈ {b(rest * k.Value.Wert)} €)";
        if (gewaehrt > 0) satz += $" von {b(gewaehrt)} $";
        var details = new List<string>();
        if (gewaehrt > 0) details.Add($"Verbraucht {b(Math.Max(0, gewaehrt - rest))} $");
        if (verfaellt != null) details.Add("verfällt am " + verfaellt.Value.ToString("dd.MM.yyyy", Deutsch));
        details.Add("Stand " + DateTime.Now.ToString("dd.MM., HH:mm", Deutsch) + " Uhr");
        satz += ". " + string.Join(" · ", details) + ".";
        if (k != null) satz += $" Kurs 1 $ = {k.Value.Wert.ToString("N4", Deutsch)} € (EZB {k.Value.Datum}).";
        return new Stand_(rest, gewaehrt > 0 ? gewaehrt : null, k?.Wert, satz);
    }

    /// <summary>Die Organisation mit API-Zugang (nicht die Abo-Organisation von claude.ai).</summary>
    private static async Task<string> Organisation()
    {
        if (Vorgaben.Text(OrgSchluessel) is { } org) return org;
        var liste = (await Holen($"{Console}/api/organizations") as List<object?>)?.OfType<JSONObjekt>() ?? Enumerable.Empty<JSONObjekt>();
        var neu = liste.FirstOrDefault(o => o.Texte("capabilities").Contains("api"))?.Text("uuid");
        if (neu == null) throw Fehler.Meldung("In der Anthropic-Konsole gibt es keine Organisation mit API-Zugang.");
        Vorgaben.Setze(OrgSchluessel, neu);
        return neu;
    }

    private static async Task<object?> Holen(string adresse)
    {
        var nr = Nummer;
        using var req = new HttpRequestMessage(HttpMethod.Get, adresse);
        req.Headers.TryAddWithoutValidation("Cookie", string.Join("; ", kekse.Select(k => $"{k.Name}={k.Wert}")));
        req.Headers.TryAddWithoutValidation("User-Agent", BrowserKennung);
        req.Headers.TryAddWithoutValidation("Accept", "application/json");
        req.Headers.TryAddWithoutValidation("Referer", Console + "/dashboard");
        using var antwort = await Netz.SendAsync(req);
        var daten = await antwort.Content.ReadAsByteArrayAsync();
        if (nr == Nummer) ErneuerteKekse(antwort);
        var code = (int)antwort.StatusCode;
        var json = JSON.Wert(daten);
        // 401 oder ein 403 mit JSON kommt von der Console selbst: Sitzung weg (am Handy so beobachtet). Ein 403 als HTML-Seite ist die Bot-Abwehr.
        if (code == 401 || (code == 403 && json != null) || antwort.RequestMessage?.RequestUri?.AbsolutePath.Contains("/login") == true) throw Fehler.Abgelaufen;
        if (json != null && code is >= 200 and < 300) return json;
        throw code switch
        {
            >= 200 and < 300 => Fehler.Meldung("Die Anthropic-Konsole lieferte keine lesbare Antwort (Schnittstelle geändert?)."),
            403 => Fehler.Meldung("Die Bot-Abwehr der Anthropic-Konsole lässt die Abfrage gerade nicht durch (403)."),
            429 => Fehler.Meldung("Anthropic bremst gerade (429); bitte gleich noch einmal versuchen."),
            _ => Fehler.Meldung($"Die Anthropic-Konsole antwortet mit Fehler {code}."),
        };
    }

    /// <summary>Set-Cookie wie im Browser übernehmen, sonst veralten Sitzung und Bot-Abwehr-Cookie.</summary>
    private static void ErneuerteKekse(HttpResponseMessage http)
    {
        if (!http.Headers.TryGetValues("Set-Cookie", out var felder)) return;
        var host = http.RequestMessage?.RequestUri?.Host ?? "";
        var geaendert = false;
        foreach (var feld in felder)
        {
            var teile = feld.Split(';');
            var gleich = teile[0].IndexOf('=');
            if (gleich <= 0) continue;
            var name = teile[0][..gleich].Trim();
            var wert = teile[0][(gleich + 1)..].Trim();
            var domain = host;
            var weg = wert.Length == 0;
            foreach (var a in teile.Skip(1))
            {
                var p = a.Split('=', 2);
                var schluessel = p[0].Trim().ToLowerInvariant();
                var w = p.Length > 1 ? p[1].Trim() : "";
                if (schluessel == "domain" && w.Length > 0) domain = w;
                else if (schluessel == "max-age" && int.TryParse(w, out var alter) && alter <= 0) weg = true;
                else if (schluessel == "expires" && DateTime.TryParse(w, CultureInfo.InvariantCulture, DateTimeStyles.AdjustToUniversal, out var ablauf) && ablauf <= DateTime.UtcNow) weg = true;
            }
            if (!GehoertZurConsole(domain)) continue;
            // Eine Antwort beendet die Sitzung nie von allein, nur Franks Abmelden. Ein neuer Wert ersetzt den alten auf jeder Domain, sonst gingen zwei mit.
            if (name == "sessionKey")
            {
                if (weg) continue;
                kekse.RemoveAll(k => k.Name == name);
            }
            else kekse.RemoveAll(k => k.Name == name && k.Domain == domain);
            if (!weg) kekse.Add(new Keks { Name = name, Wert = wert, Domain = domain });
            geaendert = true;
        }
        if (geaendert) Sichern();
    }

    /// <summary>Euro je Dollar (EZB, ändert sich einmal am Tag); scheitert der Abruf, gilt der letzte Kurs.</summary>
    private static async Task<(double Wert, string Datum, DateTime Geholt)?> Kurs()
    {
        if (kursStand is { } k && (DateTime.Now - k.Geholt).TotalHours < 6) return k;
        foreach (var adresse in new[] { "https://api.frankfurter.dev/v1/latest?base=USD&symbols=EUR", "https://api.frankfurter.app/latest?from=USD&to=EUR" })
        {
            try
            {
                var j = JSON.Objekt(await Netz.GetByteArrayAsync(adresse));
                var eur = j?.Obj("rates")?.Zahl("EUR") ?? 0;
                if (eur <= 0) continue;
                kursStand = (eur, j!.Text("date") ?? "", DateTime.Now);
                return kursStand;
            }
            catch { }
        }
        return kursStand;
    }

    /// <summary>Betrag aus {credits: {amount_minor, exponent}}, sonst aus dem Cent-Feld.</summary>
    private static double Betrag(JSONObjekt? o, double cent)
    {
        if (o?.Obj("credits") is { } c && c.Zahl("amount_minor") is { } minor) return minor / Math.Pow(10, c.Zahl("exponent") ?? 2);
        return cent / 100;
    }

    /// <summary>ISO-Zeitpunkt, auch mit Mikrosekunden.</summary>
    private static DateTime? Zeitpunkt(string text)
        => DateTimeOffset.TryParse(Regex.Replace(text, @"\.\d+", ""), CultureInfo.InvariantCulture, DateTimeStyles.AssumeUniversal, out var d) ? d.LocalDateTime : null;
}

/// <summary>Fenster mit der Anmeldeseite der Console; schließt sich, sobald die Sitzung steht. Das Guthaben holt danach der Aufrufer (Guthaben.Shared.Laden(frisch)).</summary>
public sealed class ConsoleAnmeldungAnsicht : UserControl
{
    public ConsoleAnmeldungAnsicht()
    {
        var guthaben = Guthaben.Shared;
        var g = new Grid { Width = 560, Height = 740 };
        g.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        g.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
        var kopf = Rand(H(10,
            Fuell(V(2,
                T("Bei der Anthropic-Konsole anmelden", 15, FontWeights.SemiBold),
                T("Am besten per E-Mail-Code. Danach liest Jarvis dein Guthaben direkt hier am PC.", 12, null, Farbe.Text2))),
            Kapsel("xmark", "Schließen", () => guthaben.AnmeldungOffen = false)), 14, 14, 14, 14);
        g.Children.Add(kopf);
        var browser = new ConsoleBrowser(() => guthaben.AnmeldungOffen = false);
        Grid.SetRow(browser, 1);
        g.Children.Add(browser);
        Background = B(Farbe.Grund2);
        Foreground = B(Farbe.Text);
        Content = g;
    }

    private static Window? offen;

    /// <summary>Das Blatt: öffnet genau ein Fenster, solange Guthaben.AnmeldungOffen wahr ist; danach fragt es das Guthaben frisch ab (onDismiss).</summary>
    public static void Zeige()
    {
        if (offen != null) { offen.Activate(); return; }
        var eigner = Bau.AktivesFenster();
        var w = new Window
        {
            Title = "Anthropic-Konsole", Content = new ConsoleAnmeldungAnsicht(), SizeToContent = SizeToContent.WidthAndHeight, ResizeMode = ResizeMode.NoResize, Owner = eigner, ShowInTaskbar = false,
            WindowStartupLocation = eigner == null ? WindowStartupLocation.CenterScreen : WindowStartupLocation.CenterOwner, Background = B(Farbe.Grund2),
        };
        offen = w;
        w.Closed += async (_, _) =>
        {
            offen = null;
            Guthaben.Shared.AnmeldungOffen = false;
            await Guthaben.Shared.Laden(frisch: true);
        };
        w.Show();
    }

    public static void Schliesse() => offen?.Close();
}

/// <summary>WebView2 mit eigenem, nach dem Schließen gelöschtem Datenordner: entspricht WKWebsiteDataStore.nonPersistent().</summary>
internal sealed class ConsoleBrowser : UserControl
{
    private readonly Action fertig;
    private readonly Grid rahmen = new();
    private readonly Grid gesamt = new();
    private readonly List<WebView2> popups = new();
    private readonly ContentControl popupLeiste = new() { Visibility = Visibility.Collapsed };
    private readonly string ordner = Path.Combine(Pfade.Temp, "ConsoleWeb-" + Guid.NewGuid().ToString("N"));
    private readonly DispatcherTimer wache = new() { Interval = TimeSpan.FromSeconds(1) };
    private CoreWebView2Environment? umgebung;
    private WebView2? haupt;
    private bool erledigt, geprueft;

    public ConsoleBrowser(Action fertig)
    {
        this.fertig = fertig;
        gesamt.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        gesamt.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
        // Über einem WebView2 kann WPF nichts zeichnen (Airspace): Der Knopf steht deshalb in einer Leiste darüber statt in der Ecke des Popups.
        popupLeiste.Content = Rand(H(0, Spacer(), Kapsel("xmark", "Popup schließen", () => { if (popups.Count > 0) PopupEntfernen(popups[^1]); })), 8, 4, 8, 4);
        gesamt.Children.Add(popupLeiste);
        Grid.SetRow(rahmen, 1);
        gesamt.Children.Add(rahmen);
        Content = gesamt;
        wache.Tick += async (_, _) => await Pruefen();
        Loaded += async (_, _) => await Starten();
        Unloaded += (_, _) => Aufraeumen();
    }

    private async Task Starten()
    {
        try
        {
            Directory.CreateDirectory(ordner);
            umgebung = await CoreWebView2Environment.CreateAsync(null, ordner);
            var web = await Browser();
            haupt = web;
            web.CoreWebView2.Navigate(ConsoleSitzung.LoginURL.AbsoluteUri);
            // Die Console wechselt nach der Anmeldung per Client-Navigation, und das Cookie kann später kommen als die neue Adresse: regelmäßig nachsehen.
            web.CoreWebView2.NavigationCompleted += async (_, _) => await Pruefen();
            wache.Start();
        }
        catch (Exception ex)
        {
            Log("fehler", "Anmeldefenster der Console nicht startbar", wo: "Guthaben", fehler: ex.Message);
            rahmen.Children.Add(Rand(T("Die Anmeldung braucht die WebView2-Laufzeit: " + ex.Message, 13, null, Farbe.Warnung), 14, 14, 14, 14));
        }
    }

    private async Task<WebView2> Browser()
    {
        var web = new WebView2();
        rahmen.Children.Add(web);
        await web.EnsureCoreWebView2Async(umgebung);
        web.CoreWebView2.Settings.UserAgent = ConsoleSitzung.BrowserKennung;
        web.CoreWebView2.NewWindowRequested += NeuesFenster;
        return web;
    }

    // Die Google-Anmeldung öffnet ein Popup und meldet das Ergebnis an die Console zurück: Es liegt über der Seite, bis es sich schließt oder man es schließt.
    private async void NeuesFenster(object? s, CoreWebView2NewWindowRequestedEventArgs e)
    {
        var aufschub = e.GetDeferral();
        try
        {
            if (haupt != null) haupt.Visibility = Visibility.Hidden;
            var popup = await Browser();
            popups.Add(popup);
            popupLeiste.Visibility = Visibility.Visible;
            popup.CoreWebView2.WindowCloseRequested += (_, _) => PopupEntfernen(popup);
            e.NewWindow = popup.CoreWebView2;
            e.Handled = true;
        }
        finally { aufschub.Complete(); }
    }

    private void PopupEntfernen(WebView2 popup)
    {
        popups.Remove(popup);
        rahmen.Children.Remove(popup);
        try { popup.Dispose(); } catch { }
        if (popups.Count == 0)
        {
            popupLeiste.Visibility = Visibility.Collapsed;
            if (haupt != null) haupt.Visibility = Visibility.Visible;
        }
    }

    private async Task Pruefen()
    {
        if (erledigt || geprueft || haupt?.CoreWebView2 == null) return;
        var u = haupt.CoreWebView2.Source ?? "";
        if (!u.StartsWith("https://platform.claude.com", StringComparison.Ordinal) || u.Contains("/login")) return;
        geprueft = true;
        try
        {
            var cookies = await haupt.CoreWebView2.CookieManager.GetCookiesAsync("");
            if (erledigt || !cookies.Any(c => c.Name == "sessionKey")) return;
            erledigt = true;
            wache.Stop();
            ConsoleSitzung.Uebernehmen(cookies.Select(c => new ConsoleSitzung.Keks { Name = c.Name, Wert = c.Value, Domain = c.Domain }));
            fertig();
        }
        finally { geprueft = false; }
    }

    private void Aufraeumen()
    {
        wache.Stop();
        foreach (var p in popups.ToList()) { try { p.Dispose(); } catch { } }
        popups.Clear();
        try { haupt?.Dispose(); } catch { }
        haupt = null;
        umgebung = null;
        // Die Browserprozesse brauchen einen Moment, bis sie den Ordner freigeben; bleibt er hängen, räumt ihn der nächste Start weg.
        var weg = ordner;
        _ = Task.Run(async () =>
        {
            for (var i = 0; i < 20 && Directory.Exists(weg); i++)
            {
                await Task.Delay(1000);
                try { Directory.Delete(weg, true); } catch { }
            }
        });
    }
}
