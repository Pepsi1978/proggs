using System.Globalization;
using System.Text.RegularExpressions;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using static JarvisWindows.Bau;

namespace JarvisWindows;

/// <summary>Guthaben von Anthropic und Google, gelesen über die Werkzeuge des Handys oder die eigene Console-Anmeldung, und was die letzte Aufgabe gekostet hat.</summary>
public sealed class Guthaben : Beobachtbar
{
    public static Guthaben Shared { get; } = new();

    private string? _anthropicText, _anthropicHinweis, _googleText, _googleHinweis;
    private double? _anthropicRest, _anthropicGewaehrt, _kurs, _googleRest, _googleGewaehrt, _kosten;
    private string _googleWaehrung = "€";
    private bool _laedt, _anmeldungOffen;
    private bool _pcAngemeldet = ConsoleSitzung.Angemeldet;
    private DateTime? pcAbgelehntAm;
    private DateTime? geholt;

    public string? AnthropicText { get => _anthropicText; set => Setze(ref _anthropicText, value); }
    /// <summary>Warum die letzte Abfrage keine Zahl lieferte. Dann ist AnthropicRest, falls vorhanden, ein alter Stand.</summary>
    public string? AnthropicHinweis { get => _anthropicHinweis; set { if (Setze(ref _anthropicHinweis, value)) Melde(nameof(KonsoleGetrennt)); } }
    public double? AnthropicRest { get => _anthropicRest; set { if (Setze(ref _anthropicRest, value)) Melde(nameof(KonsoleGetrennt)); } }
    public double? AnthropicGewaehrt { get => _anthropicGewaehrt; set => Setze(ref _anthropicGewaehrt, value); }
    public double? Kurs { get => _kurs; set => Setze(ref _kurs, value); }
    public string? GoogleText { get => _googleText; set => Setze(ref _googleText, value); }
    public string? GoogleHinweis { get => _googleHinweis; set => Setze(ref _googleHinweis, value); }
    public double? GoogleRest { get => _googleRest; set => Setze(ref _googleRest, value); }
    public double? GoogleGewaehrt { get => _googleGewaehrt; set => Setze(ref _googleGewaehrt, value); }
    public string GoogleWaehrung { get => _googleWaehrung; set => Setze(ref _googleWaehrung, value); }
    public bool Laedt { get => _laedt; set { if (Setze(ref _laedt, value)) Melde(nameof(KonsoleGetrennt)); } }
    /// <summary>Der PC hat eine eigene Console-Anmeldung und liest das Guthaben dann selbst statt über das Handy.</summary>
    public bool PcAngemeldet { get => _pcAngemeldet; set { if (Setze(ref _pcAngemeldet, value)) Melde(nameof(KonsoleGetrennt)); } }
    /// <summary>Die Console lehnt die gespeicherte Anmeldung gerade ab. Sie bleibt erhalten und wird weiter versucht, der Link zum Neuanmelden erscheint zusätzlich.</summary>
    public bool PcAbgelaufen => pcAbgelehntAm != null;
    /// <summary>Wahr öffnet das Anmeldefenster (Blatt), falsch schließt es.</summary>
    public bool AnmeldungOffen
    {
        get => _anmeldungOffen;
        set
        {
            if (!Setze(ref _anmeldungOffen, value)) return;
            Haupt.Post(() => { if (value) ConsoleAnmeldungAnsicht.Zeige(); else ConsoleAnmeldungAnsicht.Schliesse(); });
        }
    }
    /// <summary>Nur wenn neu anmelden hilft: Ein Netzfehler oder die Bot-Abwehr zählen nicht, ein alter Stand vom Handy nach dessen Ablehnung schon.</summary>
    public bool KonsoleGetrennt
    {
        get
        {
            if (PcAbgelaufen) return AnthropicRest == null || AnthropicHinweis != null;
            return !PcAngemeldet && ((AnthropicRest == null && !Laedt) || AnthropicHinweis?.Contains("lehnt die Anmeldung") == true);
        }
    }
    /// <summary>Was die letzte Aufgabe mit einem Claude-Modell gekostet hat, in Dollar: aus den Tokens der API, sofort und ohne Abfrage.</summary>
    public double? Kosten { get => _kosten; set => Setze(ref _kosten, value); }

    private void SetzeAbgelehnt(DateTime? d) { pcAbgelehntAm = d; Melde(nameof(PcAbgelaufen)); Melde(nameof(KonsoleGetrennt)); }

    public async Task Laden(bool frisch = false)
    {
        // Ein ausdrücklicher Wunsch wartet die laufende Abfrage ab, statt verloren zu gehen.
        if (Laedt && !frisch) return;
        while (Laedt) await Task.Delay(300);
        Laedt = true;
        try { await Task.WhenAll(AnthropicLaden(frisch), GoogleLaden(frisch)); }
        finally { Laedt = false; }
    }

    private async Task AnthropicLaden(bool frisch)
    {
        PcAngemeldet = ConsoleSitzung.Angemeldet;
        string? pcFehler = null;
        // Nach einer Ablehnung fragt nur jede Stunde oder ein ausdrücklicher Wunsch die Console, sonst gleich das Handy.
        if (PcAngemeldet && !frisch && pcAbgelehntAm is { } am && (DateTime.Now - am).TotalSeconds < 3600)
        {
            pcFehler = ConsoleSitzung.Fehler.Abgelaufen.Message;
        }
        else if (PcAngemeldet)
        {
            if (!frisch && AnthropicRest != null && AnthropicHinweis == null && geholt is { } g && (DateTime.Now - g).TotalSeconds < 900) return;
            // Wer sich während des Abrufs an- oder abmeldet, macht dessen Ergebnis ungültig.
            var nr = ConsoleSitzung.Nummer;
            try
            {
                var s = await ConsoleSitzung.Stand();
                if (nr != ConsoleSitzung.Nummer) return;
                AnthropicHinweis = null;
                SetzeAbgelehnt(null);
                AnthropicText = s.Satz; AnthropicRest = s.Rest; AnthropicGewaehrt = s.Gewaehrt;
                if (s.Kurs is { } k) Kurs = k;
                geholt = DateTime.Now;
                return;
            }
            catch (ConsoleSitzung.Fehler f) when (f.IstAbgelaufen)
            {
                if (nr != ConsoleSitzung.Nummer) return;
                if (pcAbgelehntAm == null) Log("guthaben", "Console lehnt die Anmeldung ab, sie bleibt gespeichert", wo: "Guthaben");
                SetzeAbgelehnt(DateTime.Now);
                pcFehler = f.Message;   // Das Handy kann vielleicht noch antworten.
            }
            catch (Exception ex)
            {
                if (nr != ConsoleSitzung.Nummer) return;
                Log("fehler", "Anthropic-Guthaben nicht abrufbar", wo: "Guthaben", fehler: ex.Message);
                pcFehler = ex.Message;   // Das Handy kann vielleicht noch antworten.
            }
        }
        var r = await McpKlient.Shared.Rufe("anthropic_guthaben", frisch ? new JSONObjekt { ["frisch"] = true } : new JSONObjekt(), 60);
        var t = McpKlient.OhneKontextHinweis(r.Text);
        if (r.Fehler)
        {
            // Der Hinweis des Handys verweist auf dessen Einstellungen; am PC steht der Link direkt darunter. Die Bot-Abwehr-Meldung sagt das Gegenteil und bleibt.
            AnthropicHinweis = pcFehler ?? (t.Contains("anmelden") && !t.Contains("hilft") ? "Für das Guthaben einmal bei der Anthropic-Konsole anmelden." : t);
            return;
        }
        Uebernehmen(t);
    }

    public void Abmelden()
    {
        ConsoleSitzung.Abmelden();
        PcAngemeldet = false;
        SetzeAbgelehnt(null);
        AnthropicRest = null; AnthropicGewaehrt = null; AnthropicText = null; AnthropicHinweis = null; geholt = null;
    }

    private void Uebernehmen(string t)
    {
        var rest = Zahl(t, @"Übrig: ([0-9.,]+) \$");
        if (rest == null) { AnthropicHinweis = t; return; }
        // Das Handy hängt bei einem alten Stand den Grund an: Der Betrag darf dann nicht wie ein aktueller aussehen.
        var alterStand = new[] { "Letzter Stand.", "Neue Abfrage gescheitert" }.Select(x => t.IndexOf(x, StringComparison.Ordinal)).Where(i => i >= 0).Cast<int?>().FirstOrDefault();
        AnthropicHinweis = alterStand is { } a ? t[a..] : null;
        AnthropicText = alterStand is { } b ? t[..b] : t;
        AnthropicRest = rest;
        AnthropicGewaehrt = Zahl(t, @"Übrig: [^\n]*? von ([0-9.,]+) \$");
        Kurs = Zahl(t, @"Kurs 1 \$ = ([0-9.,]+) €");
        if (alterStand == null) geholt = DateTime.Now;
    }

    /// <summary>Die Google-Anmeldung dieses Laptops wurde abgelehnt; der Stand kommt solange vom Handy.</summary>
    public bool GoogleAbgelaufen { get => _googleAbgelaufen; private set => Setze(ref _googleAbgelaufen, value); }
    private bool _googleAbgelaufen;

    private async Task GoogleLaden(bool frisch)
    {
        if (WebSitzungen.Angemeldet("google"))
        {
            try
            {
                GoogleUebernehmen(await GoogleSitzung.Stand(frisch));
                GoogleAbgelaufen = false;
                return;
            }
            catch (GoogleSitzung.Abgelaufen f)
            {
                GoogleAbgelaufen = true;
                Log("guthaben", f.Message, wo: "Konsolen");
            }
            catch (Exception ex)
            {
                Log("fehler", "Google-Guthaben am Laptop nicht abrufbar", wo: "Konsolen", fehler: ex.Message);
            }
        }
        else GoogleAbgelaufen = false;
        var r = await McpKlient.Shared.Rufe("google_guthaben", frisch ? new JSONObjekt { ["frisch"] = true } : new JSONObjekt(), 60);
        var t = McpKlient.OhneKontextHinweis(r.Text);
        if (r.Fehler) { GoogleHinweis = t; return; }
        GoogleUebernehmen(t);
    }

    private void GoogleUebernehmen(string t)
    {
        var rest = Zahl(t, @"übrig ([0-9.,]+) ");
        if (rest == null) { GoogleHinweis = t; return; }
        GoogleHinweis = null;
        GoogleText = t;
        GoogleRest = rest;
        GoogleGewaehrt = Zahl(t, @"übrig [0-9.,]+ \S+ von ([0-9.,]+) ");
        var m = Regex.Match(t, @"übrig [0-9.,]+ (\S+) von");
        if (m.Success) GoogleWaehrung = m.Groups[1].Value;
    }

    // MARK: Text

    public static string Betrag(double x) => x.ToString("N2", Deutsch);

    private static double? Zahl(string text, string muster)
    {
        var m = Regex.Match(text, muster);
        if (!m.Success) return null;
        return double.TryParse(m.Groups[1].Value.Replace(".", "").Replace(',', '.'), NumberStyles.Float, CultureInfo.InvariantCulture, out var x) ? x : null;
    }

    /// <summary>Der Satz des Handys als einzelne Punkte; die große Zahl steht schon darüber.</summary>
    public static List<string> Punkte(string text) =>
        text.Replace("z. B.", "z. B.").Replace(" · ", ". ").Split(". ").Select(x => x.Trim('.', ' ')).Where(x => x.Length > 0).ToList();
}

/// <summary>Modellwahl mit Guthaben und Kosten, in jeder Eingabeblase gleich: „12,34 $ −0,08 $ · Opus 5.5“.</summary>
public sealed class ModellKapsel : LebendeAnsicht
{
    private readonly Einstellungen einst = Einstellungen.Shared;
    private readonly Guthaben guthaben = Guthaben.Shared;

    public ModellKapsel() { Beobachte(einst, nameof(Einstellungen.Modell), nameof(Einstellungen.Effort)); Beobachte(guthaben); }

    private bool IstClaude => einst.ModellWahlAktuell.Anbieter == Anbieter.Anthropic;
    /// <summary>Unter 5 € rot, sonst grün; ohne Kurs bleibt es grün (wie am Handy).</summary>
    private bool Knapp => guthaben.AnthropicRest is { } r && guthaben.Kurs is { } k && r * k < 5;

    /// <summary>Kleine Beträge brauchen mehr Stellen: Eine Haiku-Antwort kostet oft weniger als einen Cent.</summary>
    public static string KostenText(double k) => k >= 0.01 ? Guthaben.Betrag(k) : k.ToString("N4", Deutsch);

    protected override UIElement Baue()
    {
        var teile = new List<UIElement?>();
        if (IstClaude)
        {
            if (guthaben.AnthropicRest is { } r)
                // Ein alter Stand darf nicht wie ein aktueller grün aussehen.
                teile.Add(T($"{Guthaben.Betrag(r)} $", 11, FontWeights.Bold, guthaben.AnthropicHinweis != null ? Farbe.Warnung : (Knapp ? Farbe.Rot : Farbe.Gruen), 1));
            if (guthaben.Kosten is > 0)
                teile.Add(T($"−{KostenText(guthaben.Kosten.Value)} $", 11, FontWeights.Bold, Farbe.Rot, 1));
            if (guthaben.AnthropicRest != null || (guthaben.Kosten ?? 0) > 0)
                teile.Add(T("·", 11, null, Farbe.Text3, 1));
        }
        teile.Add(T(einst.ModellWahlAktuell.Name.Replace("Claude ", ""), 12, FontWeights.SemiBold, Farbe.Text2, 1));
        var kapsel = Flaeche(H(5, teile.ToArray()), Farbe.Glas(0.06), 100, 10, 8).KapselForm();
        Button? k = null;
        k = Klick(kapsel, () => OeffneMenue(k!, Menue()), "Modell, Denkstufe und Guthaben");
        if (!guthaben.KonsoleGetrennt) return k;
        // Ein eigener Knopf: Der Hinweis führt direkt zur Anmeldung, statt erst im Modell-Menü zu enden.
        var warnung = Flaeche(H(4, Sy("exclamationmark.triangle.fill", 11, Farbe.Rot), T("Konsole verbinden", 11, FontWeights.Bold, Farbe.Rot, 1)), Farbe.Glas(0.06), 100, 10, 8).KapselForm();
        var verbinden = Klick(warnung, () => guthaben.AnmeldungOffen = true, "Anthropic-Konsole auf diesem Laptop verbinden. Danach liest Jarvis Guthaben und Kosten direkt, ohne das Handy.");
        return H(6, verbinden, k);
    }

    private List<MenueZeile> Menue()
    {
        var l = new List<MenueZeile>();
        if (guthaben.PcAbgelaufen || guthaben.KonsoleGetrennt) { l.Add(new MenueZeile("Bei der Anthropic-Konsole anmelden …", () => guthaben.AnmeldungOffen = true)); l.Add(MenueZeile.Trenner); }
        foreach (var m in ModellWahl.Alle) l.Add(new MenueZeile(m.Name, () => einst.Modell = m.Id, false, m.Id == einst.Modell));
        l.Add(MenueZeile.Trenner);
        foreach (var e in new[] { "low", "medium", "high", "xhigh", "max" }) l.Add(new MenueZeile($"Denkstufe {e}", () => einst.Effort = e, false, e == einst.Effort));
        l.Add(MenueZeile.Trenner);
        if (guthaben.AnthropicRest is { } r) l.Add(new MenueZeile($"Anthropic: {Guthaben.Betrag(r)} $ übrig"));
        if (guthaben.Kosten is > 0) l.Add(new MenueZeile($"Letzte Aufgabe: {KostenText(guthaben.Kosten.Value)} $ (Listenpreis)"));
        if (guthaben.GoogleRest is { } g) l.Add(new MenueZeile($"Google: {Guthaben.Betrag(g)} {guthaben.GoogleWaehrung} übrig"));
        l.Add(new MenueZeile("Guthaben ansehen …", () => { EinstellungenAnsicht.StartReiter = "guthaben"; AppSteuerung.Shared.Bereich = Bereich.Einstellungen; }));
        return l;
    }
}

/// <summary>Einstellungen → Guthaben: je Anbieter eine Karte wie am Handy.</summary>
public sealed class GuthabenAnsicht : LebendeAnsicht
{
    private readonly Guthaben g = Guthaben.Shared;

    public GuthabenAnsicht() { Beobachte(g); }

    protected override void Erschienen() { _ = g.Laden(); }

    protected override UIElement Baue()
    {
        var s = V(18, T("Geschenktes Guthaben wird immer vor deinem eigenen Geld verbraucht. Hier siehst du, wie viel noch übrig ist und wann es verfällt.", 13, null, Farbe.Text2));
        if (g.KonsoleGetrennt)
        {
            var warn = Flaeche(V(3,
                H(6, Sy("exclamationmark.triangle.fill", 14, Farbe.Rot), T("Anthropic-Konsole nicht verbunden", 14, FontWeights.Bold, Farbe.Rot), Spacer()),
                T("Kosten und Kostenveränderungen sind deshalb nicht sichtbar. Klicken zum Anmelden.", 13)), RotHell(), 12, 12, 12);
            VAnfuegen(s, Klick(warn, () => g.AnmeldungOffen = true, null, true), 18);
        }
        var fussAnthropic = new StackPanel();
        if (g.PcAngemeldet)
            VAnfuegen(fussAnthropic, TextKnopf("Am PC bei der Anthropic-Konsole abmelden", 13, Farbe.Text2, async () =>
            {
                Log("guthaben", "Am PC bei der Console abgemeldet", wo: "Guthaben");
                g.Abmelden();
                await g.Laden(frisch: true);
            }, FontWeights.SemiBold), 10);
        if (!g.PcAngemeldet || g.PcAbgelaufen)
        {
            var link = T("Bei der Anthropic-Konsole anmelden", 13, FontWeights.SemiBold, Farbe.Haupt);
            link.TextDecorations = TextDecorations.Underline;
            VAnfuegen(fussAnthropic, Klick(H(6, Sy("arrow.up.right.square", 13, Farbe.Haupt), link, Spacer()), () => g.AnmeldungOffen = true, null, true), 10);
        }
        VAnfuegen(s, Karte_("Claude-Modelle (Anthropic)", "brain", "Bezahlt Jarvis' Kopf, wenn du ein Claude-Modell gewählt hast.",
            g.AnthropicRest, g.AnthropicGewaehrt, "$", g.Kurs, g.AnthropicText, g.AnthropicHinweis, fussAnthropic), 18);
        VAnfuegen(s, Karte_("Gemini-Stimmen (Google AI Studio)", "waveform", "Bezahlt das Vorlesen mit Gemini-TTS und das Live-Gespräch mit Gemini auf dem bezahlten Schlüssel.",
            g.GoogleRest, g.GoogleGewaehrt, g.GoogleWaehrung, null, g.GoogleText, g.GoogleHinweis, null), 18);
        VAnfuegen(s, H(10, Kapsel("arrow.clockwise", "Jetzt neu abfragen", async () => await g.Laden(frisch: true), hervorgehoben: true), g.Laedt ? Drehkreis() : null, Spacer()), 18);
        if (g.Kosten is > 0)
            VAnfuegen(s, T($"Die letzte Aufgabe mit einem Claude-Modell hat zum Listenpreis {ModellKapsel.KostenText(g.Kosten.Value)} $ gekostet, ohne Websuche.", 13, null, Farbe.Text2), 18);
        VAnfuegen(s, T("Die Anmeldung bei Google liegt auf dem Handy: Einstellungen → Guthaben.", 12, null, Farbe.Text3), 18);
        return s;
    }

    private static object RotHell()
    {
        var k = B(Farbe.Rot).CloneCurrentValue();
        k.Opacity = 0.14;
        k.Freeze();
        return k;
    }

    private UIElement Karte_(string titel, string symbol, string erklaerung, double? rest, double? gewaehrt, string einheit, double? umrechnung, string? text, string? hinweis, UIElement? fuss)
    {
        var s = V(10, Titel(symbol, titel), T(erklaerung, 12, null, Farbe.Text2));
        if (rest is { } r)
        {
            var zahl = T($"{Guthaben.Betrag(r)} {einheit}", 30, FontWeights.Bold, Farbe.Haupt);
            zahl.VerticalAlignment = VerticalAlignment.Bottom;
            var um = umrechnung is { } u ? T($"≈ {Guthaben.Betrag(r * u)} €", 14, null, Farbe.Text2) : null;
            if (um != null) { um.VerticalAlignment = VerticalAlignment.Bottom; um.Margin = new Thickness(0, 0, 0, 6); }
            VAnfuegen(s, H(10, zahl, um, Spacer()), 10);
            if (gewaehrt is { } gw && gw > 0) VAnfuegen(s, Balken(Math.Min(1, Math.Max(0, (gw - r) / gw))), 10);
            if (hinweis != null) VAnfuegen(s, T("Alter Stand. " + hinweis, 13, null, Farbe.Warnung), 10);
            if (text != null) foreach (var p in Guthaben.Punkte(text)) VAnfuegen(s, T("• " + p, 13, null, Farbe.Text2), 10);
        }
        else
        {
            VAnfuegen(s, T(hinweis ?? "Lade …", 13, null, Farbe.Text2), 10);
        }
        if (fuss != null) VAnfuegen(s, fuss, 10);
        return Karte(s);
    }
}
