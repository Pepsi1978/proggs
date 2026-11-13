using System.Globalization;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Controls.Primitives;
using System.Windows.Input;
using System.Windows.Threading;
using static JarvisWindows.Bau;

namespace JarvisWindows;

// MARK: - Liste

/// <summary>Bereich „Erinnerung“ der Ablage: Unterreiter „Mit Datum“ (das Nächste oben, das Älteste unten) und „Allgemein“.</summary>
public sealed class ErinnerungenAnsicht : LebendeAnsicht
{
    private readonly AblageAbgleich abgleich = AblageAbgleich.Shared;
    private string reiter = "datum";

    public ErinnerungenAnsicht()
    {
        Beobachte(abgleich, nameof(abgleich.AufbauStand));
        Beobachte(ErinnerungPlaner.Shared, nameof(ErinnerungPlaner.Stand));
    }

    protected override UIElement Baue()
    {
        var allgemein = reiter == "allgemein";
        var wahl = Segment(new[] { ("datum", "Mit Datum"), ("allgemein", "Allgemein") }, () => reiter, w => { reiter = w; Neu(); }, 240);
        ((FrameworkElement)wahl).Width = 240;
        var s = V(14, H(10, wahl, Spacer(), Kapsel("plus", "Neue Erinnerung", () => ErinnerungEditor.Zeige(null, reiter == "allgemein"), hervorgehoben: true)));
        var jetzt = ErinnerungZeit.Jetzt;
        var liste = allgemein ? ErinnerungPlaner.Allgemeine() : ErinnerungPlaner.Datiert(jetzt);
        if (liste.Count == 0)
            VAnfuegen(s, T(allgemein ? "Keine allgemeinen Erinnerungen. Sag Jarvis: „Erinnere mich später an …“" : "Keine Erinnerungen mit Datum. Klicke auf „Neue Erinnerung“, oder sag Jarvis: „Erinnere mich am 12. August daran, …“", 14, null, Farbe.Text2), 14);
        foreach (var e in liste) VAnfuegen(s, Zeile(e, allgemein, jetzt), 10);
        return s;
    }

    private static UIElement Zeile(Erinnerung e, bool allgemein, long jetzt)
    {
        var vorbei = !allgemein && (e.Erledigt != 0 || (e.Termin() ?? 0) < jetzt);
        var oben = allgemein ? (e.Erledigt != 0 ? "Erledigt" : "Ohne festen Termin") : e.DatumAnzeige() + " · " + (e.Wecker ? "Wecker" : "Erinnerung") + (e.Erledigt != 0 ? " · erledigt" : "");
        return Klick(Karte(V(3,
            T(oben, 12, FontWeights.SemiBold, vorbei ? Farbe.Text3 : Farbe.Haupt, 1),
            T(e.Zeile, 15, FontWeights.Medium, vorbei ? Farbe.Text3 : Farbe.Text, 2)), 14), () => ErinnerungDetail.Zeige(e.Id));
    }
}

// MARK: - Detail

/// <summary>Kurzfassung, Inhalt, jeder Erinnerungszeitpunkt samt Bestätigung, Form, Bearbeiten. Eigenes Fenster ohne Sperre der übrigen: Ein klingelnder Wecker bleibt ausschaltbar.</summary>
public sealed class ErinnerungDetail : LebendeAnsicht
{
    private static readonly Dictionary<string, Window> Offen = new();

    private readonly string id;
    private readonly AblageAbgleich abgleich = AblageAbgleich.Shared;
    private readonly ErinnerungPlaner planer = ErinnerungPlaner.Shared;
    private readonly DispatcherTimer uhr = new() { Interval = TimeSpan.FromSeconds(30) };
    private string fehler = "";
    private bool plusOffen, plusInStunden;
    private string anzahl = "", plusUhr = "";
    private readonly TextBindung anzahlB, plusUhrB;
    private string letzterStand = "";
    private UIElement? letzterBau;

    public static void Zeige(string id)
    {
        if (Offen.TryGetValue(id, out var da)) { da.Activate(); return; }
        var eigner = HauptFenster.Aktuell is { IsVisible: true } h ? h : null;
        var w = new Window
        {
            Content = new ErinnerungDetail(id), Title = "Erinnerung", Width = 720, Height = 720, ResizeMode = ResizeMode.NoResize, WindowStyle = WindowStyle.None, Owner = eigner, ShowInTaskbar = eigner == null,
            WindowStartupLocation = eigner == null ? WindowStartupLocation.CenterScreen : WindowStartupLocation.CenterOwner, Background = B(Farbe.Grund2),
        };
        ErinnerungFenster.NichtHauptfenster(w);
        w.KeyDown += (_, a) => { if (a.Key == Key.Escape) w.Close(); };
        w.Closed += (_, _) => Offen.Remove(id);
        Offen[id] = w;
        w.Show();
        w.Activate();
    }

    private ErinnerungDetail(string id)
    {
        this.id = id;
        anzahlB = new TextBindung(() => anzahl, v => anzahl = new string(v.Where(char.IsAsciiDigit).Take(4).ToArray()));
        plusUhrB = new TextBindung(() => plusUhr, v => plusUhr = v);
        Beobachte(abgleich, nameof(abgleich.AufbauStand));
        Beobachte(planer, nameof(ErinnerungPlaner.Stand));
        uhr.Tick += (_, _) => Neu();
    }

    protected override void Erschienen() => uhr.Start();
    protected override void Verschwunden() => uhr.Stop();

    private Erinnerung? Aktuell => abgleich.Erinnerungen.FirstOrDefault(x => x.Id == id);
    private void Schliessen() => Window.GetWindow(this)?.Close();

    private string StandText(Faellig t, long jetzt)
    {
        var q = planer.QuittungZu(t.Kennung);
        return q != null ? $"bestätigt auf {q.Von}" : t.Zeit > jetzt ? "geplant" : ErinnerungPlaner.Meldbar(t, jetzt) ? "fällig" : "verpasst";
    }

    protected override UIElement Baue()
    {
        if (Aktuell is not { } e)
        {
            Dispatcher.BeginInvoke(Schliessen);
            return new Border();
        }
        var jetzt = ErinnerungZeit.Jetzt;
        var termine = e.Termine();
        // Nur bei echter Änderung neu: Sonst verlöre ein offenes Eingabefeld bei jedem Abgleich den Fokus.
        var stand = JSON.Text(e.AlsJson()) + "|" + plusOffen + plusInStunden + "|" + fehler + "|" + string.Join(",", termine.Select(t => t.Kennung + StandText(t, jetzt)));
        if (stand == letzterStand && letzterBau != null) return letzterBau;
        letzterStand = stand;

        var g = new Grid { Margin = new Thickness(22) };
        g.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        g.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
        g.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });

        var kopf = H(10, Fuell(T(e.Allgemein ? "Ohne festen Termin" : e.DatumAnzeige() + " · " + (e.Wecker ? "Wecker" : "Erinnerung"), 14, FontWeights.SemiBold, Farbe.Haupt, 1)), Rund("xmark", Schliessen, "Schließen", groesse: 30));
        g.Children.Add(kopf);

        var inhalt = V(4,
            T(e.Zeile, 20, FontWeights.Bold),
            Rand(T("Kurzfassung", 12, null, Farbe.Text3), o: 10),
            T(e.Kurz, 14),
            Rand(T("Ausführlich", 12, null, Farbe.Text3), o: 10),
            T(e.Text, 14));
        if (!e.Allgemein)
        {
            VAnfuegen(inhalt, Rand(T("Form", 12, null, Farbe.Text3), o: 12), 4);
            var form = Segment(new[] { (Erinnerung.FormMeldung, "Benachrichtigung"), (Erinnerung.FormWecker, "Wecker") }, () => e.Form, w => Aendere(x => x with { Form = w }), 300);
            ((FrameworkElement)form).Width = 300;
            ((FrameworkElement)form).HorizontalAlignment = HorizontalAlignment.Left;
            VAnfuegen(inhalt, form, 4);
        }
        VAnfuegen(inhalt, Rand(T(e.Allgemein ? "Hinweise" : "Erinnerungszeitpunkte", 12, null, Farbe.Text3), o: 12), 4);
        if (e.Allgemein && e.HinweisUm == 0) VAnfuegen(inhalt, T("Jarvis meldet sich von selbst, wenn es zu Dienst, Schlaf und Tag passt.", 13, null, Farbe.Text2), 4);
        foreach (var t in termine) VAnfuegen(inhalt, ZeitZeile(t, jetzt), 6);
        if (!e.Allgemein)
        {
            if (plusOffen) VAnfuegen(inhalt, PlusWahl(e), 8);
            else VAnfuegen(inhalt, H(0, Rund("plus", () => { fehler = ""; plusOffen = true; plusInStunden = e.Uhrzeit != null; anzahl = ""; plusUhr = ""; Neu(); }, "Erinnerungszeitpunkt hinzufügen"), Spacer()), 8);
        }
        VAnfuegen(inhalt, Rand(T($"Von {(e.Quelle.Trim().Length == 0 ? "Jarvis Handy" : e.Quelle)}{PromptEintrag.ZeitText(e.Erstellt)}", 12, null, Farbe.Text3), o: 10, u: 10), 4);
        var scroll = new ScrollViewer { Content = inhalt, VerticalScrollBarVisibility = ScrollBarVisibility.Auto, HorizontalScrollBarVisibility = ScrollBarVisibility.Disabled, Margin = new Thickness(0, 10, 0, 10) };
        Grid.SetRow(scroll, 1);
        g.Children.Add(scroll);

        var fuss = V(8, fehler.Length > 0 ? T(fehler, 13, null, Farbe.Warnung) : null, H(10,
            Kapsel("trash", "Löschen", Loeschen),
            Spacer(),
            Kapsel("checkmark", e.Erledigt != 0 ? "Wieder offen" : "Erledigt", () => { var neu = e.Erledigt != 0 ? 0 : ErinnerungZeit.Jetzt; Aendere(x => x with { Erledigt = neu }); }),
            Kapsel("square.and.pencil", "Bearbeiten", () => ErinnerungEditor.Zeige(e, e.Allgemein), hervorgehoben: true)));
        Grid.SetRow(fuss, 2);
        g.Children.Add(fuss);
        letzterBau = g;
        return g;
    }

    private UIElement ZeitZeile(Faellig t, long jetzt)
    {
        var q = planer.QuittungZu(t.Kennung);
        return H(8,
            Fuell(V(1, T(t.Anzeige, 14, FontWeights.SemiBold), T($"{ErinnerungZeit.ZeitText(t.Zeit)} · {StandText(t, jetzt)}", 12, null, q != null ? Farbe.Gruen : Farbe.Text3))),
            q == null && t.Zeit <= jetzt ? Kapsel("checkmark", "Bestätigen", () => planer.Bestaetige(t)) : null,
            t.Ausloesung is { } a ? Klein("xmark", "Zeitpunkt entfernen", () => Aendere(x => x with { Ausloesungen = x.Ausloesungen.Where(y => y.Id != a.Id).ToList() })) : null);
    }

    /// <summary>Wie viele Tage oder Stunden vor dem Termin; bei reinem Datum wahlweise mit eigener Uhrzeit. Mehrere möglich.</summary>
    private UIElement PlusWahl(Erinnerung e)
    {
        var vorgaben = (e.Uhrzeit != null ? new[] { ("Zum Termin", 0), ("1 Stunde", 60), ("3 Stunden", 180) } : new[] { ("Am Termintag", 0) })
            .Concat(new[] { ("1 Tag", 1440), ("2 Tage", 2880), ("1 Woche", 10080), ("2 Wochen", 20160), ("1 Monat", 43200) });
        var wahl = new WrapPanel();
        foreach (var (name, m) in vorgaben)
            wahl.Children.Add(Rand((FrameworkElement)Kapsel("clock", name, () => Hinzufuegen(e, m)), r: 6, u: 6));
        var feld = Feld(anzahlB, "Anzahl", 13, breite: 90);
        var teile = new List<UIElement?> { feld };
        if (e.Uhrzeit != null)
        {
            var einheit = Segment(new[] { ("stunden", "Stunden"), ("tage", "Tage") }, () => plusInStunden ? "stunden" : "tage", w => { plusInStunden = w == "stunden"; }, 180);
            ((FrameworkElement)einheit).Width = 180;
            teile.Add(einheit);
        }
        else
        {
            teile.Add(T("Tage", 13, null, Farbe.Text2));
            teile.Add(Feld(plusUhrB, $"Uhrzeit: {e.OhneUhrzeitUm} (Vorgabe)", 13, breite: 200));
        }
        teile.Add(Spacer());
        return Karte(V(10,
            T("Wann erinnern?", 15, FontWeights.Bold),
            wahl,
            H(8, teile.ToArray()),
            H(8, Spacer(), Kapsel("xmark", "Abbrechen", () => { plusOffen = false; fehler = ""; Neu(); }),
                Kapsel("plus", "Hinzufügen", () =>
                {
                    if (!int.TryParse(anzahl, NumberStyles.Integer, CultureInfo.InvariantCulture, out var n) || n <= 0) { fehler = "Bitte eine Anzahl eingeben."; Neu(); return; }
                    Hinzufuegen(e, e.Uhrzeit != null && plusInStunden ? n * 60 : n * 1440);
                }, hervorgehoben: true))), 14);
    }

    private void Hinzufuegen(Erinnerung e, int minuten)
    {
        string? u = null;
        if (e.Uhrzeit == null && plusUhr.Trim().Length > 0)
        {
            u = ErinnerungZeit.UhrzeitAus(plusUhr);
            if (u == null) { fehler = $"„{plusUhr}“ ist keine Uhrzeit."; Neu(); return; }
        }
        var a = ErinnerungZeit.NeueAusloesung(minuten, u);
        if (e.ZeitVon(a) is not { } z || z <= ErinnerungZeit.Jetzt) { fehler = "Dieser Zeitpunkt ist schon vorbei."; Neu(); return; }
        if (e.Doppelt(a)) { fehler = "Dieser Zeitpunkt ist schon eingestellt."; Neu(); return; }
        plusOffen = false;
        Aendere(x => x with { Ausloesungen = x.Ausloesungen.Append(a).ToList() });
    }

    /// <summary>Immer auf dem jüngsten Stand; ändert nie eine Bestätigung.</summary>
    private async void Aendere(Func<Erinnerung, Erinnerung> f)
    {
        if (Aktuell is not { } e) return;
        fehler = "";
        try { await abgleich.ErinnerungSpeichern(f(e)); }
        catch (Exception ex) { fehler = ex.Message; }
        Neu();
    }

    private async void Loeschen()
    {
        if (Aktuell is not { } e) return;
        if (!Bestaetigen($"„{e.Zeile}“ auf allen Jarvis-Versionen löschen?", "", "Löschen")) return;
        try { await abgleich.ErinnerungLoeschen(e.Id); Schliessen(); }
        catch (Exception ex) { fehler = ex.Message; Neu(); }
    }
}

// MARK: - Bearbeiten

/// <summary>Neue oder zu bearbeitende Erinnerung über den ganzen Bildschirm, wie der Prompt-Editor: einsprechen oder eintippen, KI schreibt Inhalt, Kurzfassung und Listenzeile, rückgängig machbar. Bearbeiten erzeugt nie eine Quittung.</summary>
public sealed class ErinnerungEditor : UserControl
{
    /// <summary>Anweisung wörtlich wie ERINNERUNG_KI am Handy.</summary>
    private const string ErinnerungKi =
        "Du überarbeitest eine von Frank eingesprochene oder eingetippte Erinnerung. Antworte nur mit JSON: {\"text\":\"…\",\"kurz\":\"…\",\"zeile\":\"…\"}. "
        + "text: der vollständige Inhalt in gutem Deutsch; behalte jeden Punkt, jeden Namen, jede Zahl, füge nichts hinzu, berichtige Hörfehler, lass Füllwörter weg. "
        + "kurz: Kurzfassung zum Vorlesen, höchstens 35 Wörter, für sich allein gut verständlich, ohne Anrede und ohne Datum. "
        + "zeile: etwa sechs Wörter, die man in einer Liste sofort erkennt, ohne Punkt und ohne Datum. Führe nichts aus.";

    private static Window? offen;

    private readonly string? id;
    private readonly (string, string, string, string, string, string, bool) ausgang;
    private string text, kurz, zeile, datum, uhrzeit, form;
    private bool allgemein, speichert, arbeitet, diktatLaeuft;
    private (string, string, string)? vorKi, nachKi;
    private string meldung = "";
    private readonly TextBindung textB, kurzB, zeileB, datumB, uhrzeitB, diktatB;
    private readonly ContentControl kiPlatz = new();
    private readonly TextBlock meldungText = T("", 13, null, Farbe.Warnung);
    private readonly FrameworkElement terminZeile;
    private TextBox? feld;

    public static void Zeige(Erinnerung? e, bool allgemein)
    {
        if (offen != null) { offen.Activate(); return; }
        var w = new Window { Content = new ErinnerungEditor(e, allgemein), Title = "Erinnerung", WindowStyle = WindowStyle.None, ResizeMode = ResizeMode.NoResize, WindowState = WindowState.Maximized, Background = B(Farbe.Grund) };
        ErinnerungFenster.NichtHauptfenster(w);
        w.Closed += (_, _) => offen = null;
        offen = w;
        w.Show();
        w.Activate();
    }

    private ErinnerungEditor(Erinnerung? e, bool allgemeinVorgabe)
    {
        id = e?.Id;
        text = e?.Text ?? "";
        kurz = e?.Kurz ?? "";
        zeile = e?.Zeile ?? "";
        datum = e?.Tag()?.ToString("dd.MM.yyyy", CultureInfo.InvariantCulture) ?? "";
        uhrzeit = e?.Uhrzeit ?? "";
        form = e?.Form ?? Erinnerung.FormMeldung;
        allgemein = e?.Allgemein ?? allgemeinVorgabe;
        ausgang = Stand;
        Background = B(Farbe.Grund);

        textB = new TextBindung(() => text, v => { text = v; KiPruefen(); });
        kurzB = new TextBindung(() => kurz, v => { kurz = v; KiPruefen(); });
        zeileB = new TextBindung(() => zeile, v => { zeile = v; KiPruefen(); });
        datumB = new TextBindung(() => datum, v => datum = v);
        uhrzeitB = new TextBindung(() => uhrzeit, v => uhrzeit = v);
        // Das Diktat hängt für Prompts den Aufgabentrenner an; eine Erinnerung ist ein zusammenhängender Text.
        diktatB = new TextBindung(() => text, v => textB.Wert = OhneTrenner(v));

        Button? kalender = null;
        kalender = Klick(Flaeche(Sy("calendar", 15, Farbe.Haupt), Farbe.Glas(0.07), 8, 9, 6, Farbe.Glas(0.10)),
            () => MonatsWahl.Oeffne(kalender!, ErinnerungZeit.DatumAus(datum) ?? DateOnly.FromDateTime(DateTime.Today), d => datumB.Wert = d.ToString("dd.MM.yyyy", CultureInfo.InvariantCulture)), "Datum wählen");
        terminZeile = H(8, Feld(datumB, "Datum (TT.MM.JJJJ)", 14, breite: 170), kalender, Feld(uhrzeitB, "Uhrzeit (optional)", 14, breite: 170), Spacer());
        terminZeile.Visibility = allgemein ? Visibility.Collapsed : Visibility.Visible;

        var art = Segment(new[] { ("datum", "Mit Datum"), ("allgemein", "Allgemein") }, () => allgemein ? "allgemein" : "datum", w => { allgemein = w == "allgemein"; terminZeile.Visibility = allgemein ? Visibility.Collapsed : Visibility.Visible; }, 240);
        ((FrameworkElement)art).Width = 240;
        var formWahl = Segment(new[] { (Erinnerung.FormMeldung, "Benachrichtigung"), (Erinnerung.FormWecker, "Wecker") }, () => form, w => form = w, 300);
        ((FrameworkElement)formWahl).Width = 300;

        var kopf = H(12,
            Fuell(T(id != null ? "Erinnerung bearbeiten" : "Neue Erinnerung", 24, FontWeights.Bold)),
            Kapsel("xmark", "Schließen", Versuchen),
            Kapsel("square.and.arrow.down.fill", "Speichern", Speichern, hervorgehoben: true));
        var werkzeuge = H(10, new DiktatKnopf(diktatB, meldeLaeuft: l => diktatLaeuft = l), kiPlatz, Spacer());
        meldungText.Visibility = Visibility.Collapsed;
        var inhaltFeld = Feld(textB, "Woran soll ich dich erinnern? Eintippen oder einsprechen", 17, mehrzeilig: true, fertig: b => feld = b);
        var oben = V(12, kopf, werkzeuge, meldungText, H(16, art, formWahl, Spacer()), terminZeile);
        var rahmen = Rand(Flaeche(inhaltFeld, Farbe.Glas(0.05), 16, 14, 14), o: 14);
        var kurzFeld = Flaeche(Feld(kurzB, "Kurzfassung zum Vorlesen (etwa 35 Wörter)", 14, mehrzeilig: true), Farbe.Glas(0.05), 12, 12, 8);
        kurzFeld.MinHeight = 64;
        kurzFeld.MaxHeight = 110;
        var zeileFeld = Feld(zeileB, "Listenzeile (etwa sechs Wörter)", 14);
        var unten = V(10, Rand(kurzFeld, o: 12), zeileFeld);

        var g = new Grid { Margin = new Thickness(36, 40, 36, 28) };
        g.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        g.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
        g.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
        Grid.SetRow(rahmen, 1);
        Grid.SetRow(unten, 2);
        g.Children.Add(oben); g.Children.Add(rahmen); g.Children.Add(unten);
        Content = g;
        BaueKi();

        PreviewKeyDown += (_, a) =>
        {
            if (a.Key == Key.S && Keyboard.Modifiers == ModifierKeys.Control) { Speichern(); a.Handled = true; }
            if (a.Key == Key.Escape) { Versuchen(); a.Handled = true; }
        };
        Loaded += (_, _) => Dispatcher.BeginInvoke(DispatcherPriority.Input, () => feld?.Focus());
    }

    private (string, string, string, string, string, string, bool) Stand => (text, kurz, zeile, datum, uhrzeit, form, allgemein);

    private string OhneTrenner(string v)
    {
        var alt = text.Trim();
        var r = v;
        if (alt.Length > 0 && r.StartsWith(alt + AufgabenText.Trenner, StringComparison.Ordinal)) r = alt + " " + r[(alt.Length + AufgabenText.Trenner.Length)..];
        if (r.EndsWith(AufgabenText.Trenner, StringComparison.Ordinal)) r = r[..^AufgabenText.Trenner.Length];
        return r;
    }

    private void Zeige(string t)
    {
        meldung = t;
        meldungText.Text = meldung;
        meldungText.Visibility = meldung.Length == 0 ? Visibility.Collapsed : Visibility.Visible;
    }

    private void Schliessen() => Window.GetWindow(this)?.Close();
    private void Versuchen()
    {
        if (Stand == ausgang || Bestaetigen("Deine Änderungen an dieser Erinnerung verwerfen?", "", "Verwerfen")) Schliessen();
    }

    /// <summary>Rückgängig gilt nur für den Stand direkt nach der KI; danach würde es spätere Änderungen löschen.</summary>
    private void KiPruefen()
    {
        if (vorKi != null && (text, kurz, zeile) != nachKi) { vorKi = null; nachKi = null; BaueKi(); }
    }

    private void BaueKi() => kiPlatz.Content = H(10,
        Kapsel("sparkles", arbeitet ? "Jarvis formuliert …" : "Mit KI verbessern", Verbessern),
        arbeitet ? Drehkreis() : null,
        vorKi != null ? Kapsel("arrow.uturn.backward", "Rückgängig", Rueckgaengig) : null);

    private void Rueckgaengig()
    {
        if (vorKi is not { } v) return;
        vorKi = null; nachKi = null;
        textB.Wert = v.Item1; kurzB.Wert = v.Item2; zeileB.Wert = v.Item3;
        BaueKi();
    }

    private async void Verbessern()
    {
        if (arbeitet || text.Trim().Length == 0) return;
        var alt = (text, kurz, zeile);
        arbeitet = true; Zeige(""); BaueKi();
        var roh = await Kurzauftrag.Frage(text, system: ErinnerungKi);
        arbeitet = false;
        JSONObjekt? j = null;
        if (roh != null && roh.IndexOf('{') is >= 0 and var a && roh.LastIndexOf('}') is var b && b > a)
            try { j = JSON.Objekt(roh[a..(b + 1)]); } catch { }
        var neuText = (j?.Text("text") ?? "").Trim();
        if ((text, kurz, zeile) != alt) Zeige("Der Text wurde inzwischen geändert. Die KI-Fassung ist verworfen.");
        else if (neuText.Length < 3) Zeige("Die KI hat nichts Brauchbares geliefert. Der Text bleibt, wie er ist.");
        else
        {
            var neuKurz = (j?.Text("kurz") ?? "").Trim() is { Length: > 0 } k ? k : alt.kurz;
            var neuZeile = (j?.Text("zeile") ?? "").Trim() is { Length: > 0 } z ? z : alt.zeile;
            // Vor dem Setzen: Die Änderungsmeldungen dürfen das Rückgängig nicht löschen.
            nachKi = (neuText, neuKurz, neuZeile);
            vorKi = alt;
            textB.Wert = neuText; kurzB.Wert = neuKurz; zeileB.Wert = neuZeile;
            Log("ablage", "Erinnerung von der KI überarbeitet", wo: "Ablage/Erinnerung");
        }
        BaueKi();
    }

    private async void Speichern()
    {
        if (speichert || arbeitet) return;
        // Das Gesagte käme sonst erst nach dem Speichern an und ginge verloren.
        if (diktatLaeuft) { Zeige("Das Diktat läuft noch. Erst beenden, dann speichern."); return; }
        var sauber = text.Trim();
        if (sauber.Length == 0) { Zeige("Der Inhalt fehlt."); return; }
        DateOnly? tag = null;
        string? uhr = null;
        if (!allgemein)
        {
            if (datum.Trim().Length == 0) { Zeige("Bitte ein Datum wählen."); return; }
            tag = ErinnerungZeit.DatumAus(datum);
            if (tag == null) { Zeige($"„{datum}“ ist kein Datum."); return; }
            if (uhrzeit.Trim().Length > 0)
            {
                uhr = ErinnerungZeit.UhrzeitAus(uhrzeit);
                if (uhr == null) { Zeige($"„{uhrzeit}“ ist keine Uhrzeit."); return; }
            }
        }
        speichert = true; Zeige("");
        try
        {
            var abgleich = AblageAbgleich.Shared;
            Erinnerung e;
            if (id == null) e = ErinnerungWerkzeuge.Neu(sauber, zeile, kurz, tag, uhr, form);
            else
            {
                var alt = abgleich.Erinnerungen.FirstOrDefault(x => x.Id == id) ?? throw AblageFehler.NichtMehrDa;
                e = alt with
                {
                    Text = sauber, Kurz = kurz.Trim().Length > 0 ? kurz.Trim() : ErinnerungZeit.KurzAus(sauber), Zeile = zeile.Trim().Length > 0 ? zeile.Trim() : ErinnerungZeit.ZeileAus(sauber),
                    Datum = tag?.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture), Uhrzeit = uhr, Form = form, Allgemein = allgemein,
                    // Nur beim Wechsel von allgemein zu datiert: Bewusst entfernte Zeitpunkte bleiben entfernt.
                    Ausloesungen = !allgemein && alt.Allgemein ? new List<Ausloesung> { ErinnerungZeit.NeueAusloesung(0) } : alt.Ausloesungen,
                };
            }
            await abgleich.ErinnerungSpeichern(e);
            Schliessen();
        }
        catch (Exception ex) { Zeige(ex.Message); }
        finally { speichert = false; }
    }
}

/// <summary>Kleiner Monatskalender im Design der App: Das WPF-Steuerelement kennt Hell und Dunkel nicht.</summary>
public static class MonatsWahl
{
    public static void Oeffne(FrameworkElement anker, DateOnly start, Action<DateOnly> fertig)
    {
        var p = new Popup { PlacementTarget = anker, Placement = PlacementMode.Bottom, StaysOpen = false, AllowsTransparency = true };
        var monat = new DateOnly(start.Year, start.Month, 1);
        var de = CultureInfo.GetCultureInfo("de-DE");
        void Zeichne()
        {
            var tage = new UniformGrid { Columns = 7 };
            foreach (var w in new[] { "Mo", "Di", "Mi", "Do", "Fr", "Sa", "So" })
            {
                var k = T(w, 11, null, Farbe.Text3);
                k.TextAlignment = TextAlignment.Center;
                tage.Children.Add(k);
            }
            var luecke = ((int)monat.DayOfWeek + 6) % 7;
            for (var i = 0; i < luecke; i++) tage.Children.Add(new Border());
            for (var t = 1; t <= DateTime.DaysInMonth(monat.Year, monat.Month); t++)
            {
                var tag = new DateOnly(monat.Year, monat.Month, t);
                var gewaehlt = tag == start;
                var z = T(t.ToString(CultureInfo.InvariantCulture), 13, gewaehlt ? FontWeights.Bold : FontWeights.Normal, gewaehlt ? Farbe.Haupt : Farbe.Text);
                z.TextAlignment = TextAlignment.Center;
                var f = Flaeche(z, gewaehlt ? Farbe.Glas(0.13) : null, 8, 8, 6);
                Schweben(f, gewaehlt ? Farbe.Glas(0.13) : System.Windows.Media.Brushes.Transparent, Farbe.Glas(0.09));
                tage.Children.Add(Klick(f, () => { p.IsOpen = false; fertig(tag); }));
            }
            var kopf = H(6,
                Klein("chevron.left", "Voriger Monat", () => { monat = monat.AddMonths(-1); Zeichne(); }),
                Fuell(T(monat.ToString("MMMM yyyy", de), 14, FontWeights.SemiBold)),
                Klein("chevron.right", "Nächster Monat", () => { monat = monat.AddMonths(1); Zeichne(); }));
            var inhalt = V(8, kopf, tage);
            inhalt.Width = 300;
            p.Child = Flaeche(inhalt, Farbe.Grund2, 12, 12, 12, Farbe.Glas(0.14));
        }
        Zeichne();
        p.IsOpen = true;
    }
}

// MARK: - Banner

/// <summary>Das Banner fälliger Erinnerungen: unten rechts über allem, auch wenn das Hauptfenster versteckt ist. Bleibt, bis bestätigt oder anderswo erledigt; nimmt beim Erscheinen keinen Fokus, außer ein Wecker klingelt.</summary>
public sealed class ErinnerungFenster : Window
{
    private static ErinnerungFenster? fenster;

    /// <summary>WPF macht das erste Fenster zum Hauptfenster: Startet die App im Hintergrund, wäre es sonst dieses hier.</summary>
    public static void NichtHauptfenster(Window w)
    {
        var app = Application.Current;
        if (app != null && ReferenceEquals(app.MainWindow, w) && HauptFenster.Aktuell is { } h) app.MainWindow = h;
    }

    public static void Abgleichen(IReadOnlyList<Faellig> aktiv)
    {
        if (aktiv.Count == 0)
        {
            fenster?.Close();
            fenster = null;
            return;
        }
        var neu = fenster == null;
        fenster ??= new ErinnerungFenster();
        fenster.Baue(aktiv);
        if (neu) fenster.Show();
        if (aktiv.Any(f => f.Erinnerung.Wecker) && !fenster.wurdeGeholt) { fenster.wurdeGeholt = true; fenster.Activate(); }
        if (!aktiv.Any(f => f.Erinnerung.Wecker)) fenster.wurdeGeholt = false;
    }

    private bool wurdeGeholt;
    private string letzterStand = "";

    private ErinnerungFenster()
    {
        Title = "Erinnerung";
        WindowStyle = WindowStyle.None;
        ResizeMode = ResizeMode.NoResize;
        AllowsTransparency = true;
        Background = System.Windows.Media.Brushes.Transparent;
        ShowInTaskbar = false;
        ShowActivated = false;
        Topmost = true;
        Width = 440;
        SizeToContent = SizeToContent.Height;
        NichtHauptfenster(this);
        SizeChanged += (_, _) => Setzen();
        Loaded += (_, _) => Setzen();
        // Von außen geschlossen (Alt+F4, Werkzeug): Der nächste Lauf baut das Banner neu.
        Closed += (_, _) => { if (ReferenceEquals(fenster, this)) fenster = null; };
    }

    private void Setzen()
    {
        var f = SystemParameters.WorkArea;
        Left = f.Right - Width - 16;
        Top = f.Bottom - ActualHeight - 16;
    }

    private void Baue(IReadOnlyList<Faellig> aktiv)
    {
        var stand = string.Join("\n", aktiv.Select(f => $"{f.Kennung}|{f.Erinnerung.Zeile}|{f.Erinnerung.Kurz}|{f.Erinnerung.Form}|{f.Anzeige}"));
        if (stand == letzterStand) return;
        letzterStand = stand;
        var s = new StackPanel();
        foreach (var f in aktiv) VAnfuegen(s, Karte_(f), 10);
        Content = new ScrollViewer { Content = s, MaxHeight = SystemParameters.WorkArea.Height * 0.7, VerticalScrollBarVisibility = ScrollBarVisibility.Auto, HorizontalScrollBarVisibility = ScrollBarVisibility.Disabled };
    }

    private static UIElement Karte_(Faellig f)
    {
        var e = f.Erinnerung;
        var farbe = e.Wecker ? Farbe.Rot : Farbe.Haupt;
        var kopf = H(8, Sy(e.Wecker ? "speaker.wave.3.fill" : "bell.fill", 14, farbe),
            Fuell(T(e.Allgemein ? f.Anzeige : $"{e.DatumAnzeige()} · {f.Anzeige}", 12, FontWeights.SemiBold, farbe, 1)));
        var text = Klick(V(4, T(e.Zeile, 15, FontWeights.Bold, null, 2), T(e.Kurz, 13, null, Farbe.Text2)), () => ErinnerungDetail.Zeige(e.Id), "Details öffnen");
        var knoepfe = H(8, Spacer(), Kapsel(e.Wecker ? "stop.fill" : "checkmark", e.Wecker ? "Aus" : "Bestätigen", () => ErinnerungPlaner.Shared.Bestaetige(f), hervorgehoben: true));
        return Flaeche(V(8, kopf, text, knoepfe), Farbe.Grund2, 16, 16, 14, farbe, 2);
    }
}
