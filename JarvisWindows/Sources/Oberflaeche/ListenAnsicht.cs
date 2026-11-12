using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using static JarvisWindows.Ui;

namespace JarvisWindows;

/// <summary>Ablage → Listen, wie der Listen-Reiter am Handy (ui/ListenBildschirm.kt): Listen als Kacheln, darunter die gewählte Liste mit
/// Abhaken, Unterpunkten, Prioritäten, Tags, Abschnitten, Sortierung und Gruppierung. Alles geht über ListenSpeicher und ist sofort auf Handy und Tablet.</summary>
public sealed class ListenAnsicht : UserControl
{
    private readonly ListenSpeicher speicher = ListenSpeicher.Shared;
    private static long gewaehlt;
    private long unterVon;
    private long bearbeite;
    private string bearbeiteText = "";
    private bool loeschenFrage;
    private readonly AdaptivGitter kacheln = new() { Minimum = 170, Abstand = 10 };
    private readonly Decorator kopf = new();
    private readonly Decorator unterHinweis = new();
    private readonly StackPanel zeilen = new();
    private readonly StackPanel status = new();
    private readonly TextFeld eingabe;
    private readonly Border eingabeRahmen;

    private static readonly Brush[] ListenFarben = { Farbe.Haupt, Farbe.Gruen, Farbe.Gold, Farbe.Kupfer, Farbe.Warnung, Farbe.Rot, Farbe.Text2, Farbe.Haupt };
    private static Brush ListenFarbe(Liste l) => ListenFarben[Math.Abs(l.Farbe) % ListenFarben.Length];
    private static Brush PrioFarbe(int p) => p switch { 3 => Farbe.Rot, 2 => Farbe.Gold, 1 => Farbe.Gruen, _ => Farbe.Text3 };

    public ListenAnsicht()
    {
        eingabe = new TextFeld("Eintrag hinzufügen … (Enter; !hoch, #tag)", 14, absenden: Hinzufuegen);
        eingabeRahmen = Box(H(10, Sym.Bild("plus", 13, Farbe.Haupt), eingabe.Fuell()), Farbe.Glas(0.05), 12, 12, 10, Farbe.Glas(0.08));
        Content = V(16, kacheln, status, kopf, unterHinweis, eingabeRahmen, zeilen);
        // Tippt Frank gerade im Kopf oder in einer Zeile, wartet der Neuaufbau, bis das Feld den Fokus abgibt: Sonst speicherte das verschwindende Feld den halben Text.
        kopf.LostKeyboardFocus += (_, _) => NachholenWennFrei();
        zeilen.LostKeyboardFocus += (_, _) => NachholenWennFrei();
        Loaded += (_, _) => { speicher.PropertyChanged += Geaendert; Bauen(); };
        Unloaded += (_, _) => speicher.PropertyChanged -= Geaendert;
    }

    private void Geaendert(object? s, System.ComponentModel.PropertyChangedEventArgs e)
    {
        if (e.PropertyName is nameof(ListenSpeicher.Aenderung) or nameof(ListenSpeicher.Bereit))
        {
            if (kopf.IsKeyboardFocusWithin || zeilen.IsKeyboardFocusWithin) { aufschub = true; BaueKacheln(); BaueStatus(); return; }
            Bauen();
        }
        else if (e.PropertyName is nameof(ListenSpeicher.Fehler) or nameof(ListenSpeicher.Meldung) or nameof(ListenSpeicher.Ausstehend)) BaueStatus();
    }

    private bool aufschub;

    private void NachholenWennFrei() => Spaeter(() =>
    {
        if (!aufschub || kopf.IsKeyboardFocusWithin || zeilen.IsKeyboardFocusWithin) return;
        aufschub = false;
        Bauen();
    });

    private Liste? Aktuell
    {
        get
        {
            var alle = speicher.Alle;
            var l = alle.FirstOrDefault(x => x.Id == gewaehlt) ?? alle.FirstOrDefault();
            if (l != null) gewaehlt = l.Id;
            return l;
        }
    }

    private void Bauen()
    {
        BaueKacheln();
        BaueStatus();
        BaueKopf();
        BaueZeilen();
    }

    private void BaueStatus()
    {
        status.Children.Clear();
        void Zeile(string t, Brush f) => status.Children.Add(Tx(t, 12, null, f).Rand(0, status.Children.Count > 0 ? 4 : 0, 0, 0));
        if (!speicher.Bereit && speicher.Alle.Count == 0) Zeile("Der Abgleich mit Handy und Tablet läuft …", Farbe.Text2);
        if (speicher.Ausstehend > 0) Zeile($"{speicher.Ausstehend} Änderung(en) gehen noch an Handy und Tablet …", Farbe.Text3);
        if (speicher.Meldung is { } m) Zeile(m, Farbe.Warnung);
        if (speicher.Fehler is { } f) Zeile(f, Farbe.Warnung);
        status.Visibility = status.Children.Count == 0 ? Visibility.Collapsed : Visibility.Visible;
    }

    private void BaueKacheln()
    {
        kacheln.Children.Clear();
        var aktuell = Aktuell;
        foreach (var l in speicher.Alle)
        {
            var aktiv = aktuell?.Id == l.Id;
            var farbe = ListenFarbe(l);
            var bild = new Border { Width = 34, Height = 34, CornerRadius = new CornerRadius(10), Background = Farbe.Mit(farbe, 0.15), Child = Tx(ListenArten.Zeichen(l.Art), 16).Mittig() };
            ((TextBlock)bild.Child).VerticalAlignment = VerticalAlignment.Center;
            var karte = Box(H(10, bild, V(1, Tx(l.Name, 13, FontWeights.SemiBold, null, zeilen: 1), Tx($"{speicher.Offen(l.Id)} offen", 11, null, Farbe.Text3)).Fuell()),
                Farbe.Glas(aktiv ? 0.13 : 0.04), 14, 10, 10, aktiv ? Farbe.Mit(farbe, 0.7) : Farbe.Glas(0.06));
            var id = l.Id;
            kacheln.Children.Add(karte.Klick(() => { if (gewaehlt == id) return; gewaehlt = id; unterVon = 0; bearbeite = 0; loeschenFrage = false; Bauen(); }));
        }
        // Neue Liste: Name eintippen, Enter legt an.
        TextFeld neu = null!;
        neu = new TextFeld("+ Neue Liste", 13, absenden: () =>
        {
            var name = neu.Text.Trim();
            if (name.Length == 0) return;
            var art = ArtAusName(name);
            var l = speicher.ListeAnlegen(name, art);
            gewaehlt = l.Id;
            unterVon = 0; bearbeite = 0; loeschenFrage = false;
            Bauen();
            Log("listen", $"Liste angelegt: {name}", wo: "Ablage/Listen");
        });
        kacheln.Children.Add(Box(neu, Farbe.Glas(0.03), 14, 12, 18, Farbe.Glas(0.08)));
    }

    /// <summary>Wie `Listen.artAusName` am Handy: „Einkaufsliste“ → Einkauf.</summary>
    private static string ArtAusName(string name)
    {
        var g = name.ToLowerInvariant();
        if (new[] { "einkauf", "shopping", "supermarkt", "besorg" }.Any(g.Contains)) return "einkauf";
        if (new[] { "pack", "koffer", "urlaub", "reise" }.Any(g.Contains)) return "packliste";
        if (g.Contains("idee")) return "ideen";
        if (new[] { "aufgabe", "todo", "to do", "erledig" }.Any(g.Contains)) return "aufgaben";
        if (g.Contains("check")) return "checkliste";
        return "sonstiges";
    }

    private void BaueKopf()
    {
        var l = Aktuell;
        eingabeRahmen.Visibility = l == null ? Visibility.Collapsed : Visibility.Visible;
        if (l == null)
        {
            kopf.Child = Tx(speicher.Bereit ? "Noch keine Listen. Leg oben eine an oder sag Jarvis „Setz Milch auf die Einkaufsliste“." : "", 14, null, Farbe.Text2);
            unterHinweis.Child = null;
            return;
        }
        var id = l.Id;
        TextFeld name = null!;
        name = new TextFeld("Name", 20, text: l.Name, absenden: () =>
        {
            var n = name.Text.Trim();
            if (n.Length > 0 && n != l.Name) speicher.ListeAendern(id, o => o["name"] = n);
        });
        name.Box.FontWeight = FontWeights.Bold;
        name.Box.LostKeyboardFocus += (_, _) => { var n = name.Text.Trim(); if (n.Length > 0 && n != l.Name) speicher.ListeAendern(id, o => o["name"] = n); };
        var artWahl = Bau.Auswahl(ListenArten.Alle.Select(a => (a.Kennung, a.Zeichen + " " + a.Anzeige)), () => l.Art, w => speicher.ListeAendern(id, o => o["art"] = w), 150);
        var sortWahl = Bau.Auswahl(ListenArten.Sortierungen.Select(a => (a.Kennung, a.Anzeige)), () => l.Sortierung, w => speicher.ListeAendern(id, o => o["sortierung"] = w), 180);
        var gruppWahl = Bau.Auswahl(ListenArten.Gruppierungen.Select(a => (a.Kennung, "Gruppe: " + a.Anzeige)), () => l.Gruppierung, w => speicher.ListeAendern(id, o => o["gruppierung"] = w), 180);
        var erledigte = new Schalter(() => l.ErledigteZeigen, an => speicher.ListeAendern(id, o => o["erledigte_zeigen"] = an));
        var aufraeumen = new RundKnopf("checkmark.circle.fill", () =>
        {
            var n = speicher.ErledigteLoeschen(id);
            Log("listen", $"{n} erledigte Einträge gelöscht", wo: "Ablage/Listen");
        }, hilfe: "Erledigte Einträge löschen", groesse: 30);
        var loeschen = loeschenFrage
            ? (UIElement)H(6, Tx("Liste wirklich löschen?", 12, FontWeights.SemiBold, Farbe.Rot),
                new KapselKnopf("trash.fill", "Löschen", () => { loeschenFrage = false; speicher.ListeLoeschen(id); gewaehlt = 0; Log("listen", $"Liste gelöscht: {l.Name}", wo: "Ablage/Listen"); }, hervorgehoben: true),
                new KapselKnopf("xmark", "Abbrechen", () => { loeschenFrage = false; BaueKopf(); }))
            : new RundKnopf("trash", () => { loeschenFrage = true; BaueKopf(); }, hilfe: "Liste löschen", groesse: 30);
        var abschnitte = new TextFeld("Abschnitte, mit Komma getrennt (z. B. Obst, Drogerie)", 12, text: string.Join(", ", l.Abschnitte));
        abschnitte.Box.LostKeyboardFocus += (_, _) =>
        {
            var neu = abschnitte.Text.Split(',').Select(a => a.Trim()).Where(a => a.Length > 0).Distinct().ToList();
            if (!neu.SequenceEqual(l.Abschnitte)) speicher.ListeAendern(id, o => o["abschnitte"] = neu.Cast<object?>().ToList());
        };
        kopf.Child = V(10,
            H(12, Tx(ListenArten.Zeichen(l.Art), 22), name.Fuell(), aufraeumen, loeschen),
            H(10, artWahl, sortWahl, gruppWahl, Spacer(), Tx("Erledigte zeigen", 12, null, Farbe.Text2), erledigte),
            l.Gruppierung == "abschnitt" ? Box(abschnitte, Farbe.Glas(0.04), 10, 10, 8) : null);
        BaueUnterHinweis();
    }

    private void BaueUnterHinweis()
    {
        var oben = unterVon != 0 ? speicher.EintraegeVon(gewaehlt).FirstOrDefault(e => e.Id == unterVon) : null;
        if (oben == null) { unterVon = 0; unterHinweis.Child = null; return; }
        unterHinweis.Child = H(8, Sym.Bild("arrow.turn.up.right", 12, Farbe.Haupt), Tx($"Neue Einträge kommen unter „{oben.Titel}“", 12, null, Farbe.Haupt),
            new RundKnopf("xmark", () => { unterVon = 0; BaueUnterHinweis(); }, hilfe: "Wieder auf die oberste Ebene", groesse: 24), Spacer());
    }

    private void Hinzufuegen()
    {
        var l = Aktuell;
        var text = eingabe.Text.Trim();
        if (l == null || text.Length == 0) return;
        var s = ListenArten.Schnell(text);
        var abschnitt = "";
        // In der Abschnittsansicht landet ein neuer Eintrag im letzten Abschnitt, den Frank angetippt hat (wie am Handy der Kopf „+“).
        if (l.Gruppierung == "abschnitt" && unterVon == 0) abschnitt = abschnittWahl;
        speicher.Hinzufuegen(l.Id, s.Titel, unterVon, s.Prio, s.Tags, abschnitt);
        eingabe.Text = "";
        Log("listen", $"Eintrag hinzugefügt: {s.Titel}", wo: "Ablage/Listen");
    }

    private string abschnittWahl = "";

    private void BaueZeilen()
    {
        zeilen.Children.Clear();
        var l = Aktuell;
        if (l == null) return;
        var ansicht = speicher.Ansicht(l);
        if (ansicht.Count == 0) { zeilen.Children.Add(Tx("Die Liste ist leer.", 13, null, Farbe.Text3).Rand(4, 8, 0, 0)); return; }
        foreach (var z in ansicht) zeilen.Children.Add(z.Eintrag == null ? KopfZeile(l, z) : EintragZeile(l, z));
    }

    private UIElement KopfZeile(Liste l, ListenZeile z)
    {
        var name = z.Kopf ?? "";
        var aktiv = l.Gruppierung == "abschnitt" && abschnittWahl == name;
        var t = H(8, Tx(name.ToUpperInvariant(), 11, FontWeights.Bold, aktiv ? Farbe.Haupt : Farbe.Text3), Tx(z.KopfAnzahl.ToString(), 11, null, Farbe.Text3), Spacer());
        t.Margin = new Thickness(4, 14, 0, 4);
        if (l.Gruppierung != "abschnitt" || name == "Erledigt") return t;
        var echt = name == "Ohne Abschnitt" ? "" : name;
        return Box(t, null).Klick(() => { abschnittWahl = abschnittWahl == echt ? "" : echt; BaueZeilen(); }).Hilfe("Antippen: neue Einträge kommen in diesen Abschnitt");
    }

    private UIElement EintragZeile(Liste liste, ListenZeile z)
    {
        var e = z.Eintrag!;
        var id = e.Id;
        var prio = PrioFarbe(e.Prioritaet);
        var haken = new Border
        {
            Width = 20, Height = 20, CornerRadius = new CornerRadius(10), BorderThickness = new Thickness(1.6),
            BorderBrush = e.Erledigt ? Farbe.Haupt : (e.Prioritaet > 0 ? prio : Farbe.Text3), Background = e.Erledigt ? Farbe.Haupt : Farbe.Klar,
            Child = e.Erledigt ? Sym.Bild("checkmark", 10, Farbe.Schwarz) : null, VerticalAlignment = VerticalAlignment.Top, Margin = new Thickness(0, 2, 0, 0),
        }.Klick(() => { speicher.Abhaken(id, !e.Erledigt); Log("listen", (e.Erledigt ? "Wieder geöffnet: " : "Abgehakt: ") + e.Titel, wo: "Ablage/Listen"); }).Hilfe(e.Erledigt ? "Wieder öffnen" : "Abhaken");

        UIElement titel;
        if (bearbeite == id)
        {
            TextFeld feld = null!;
            void Fertig()
            {
                if (bearbeite != id) return;
                bearbeite = 0;
                var t = feld.Text.Trim();
                if (t.Length > 0 && t != e.Titel) speicher.Aendern(id, o => o["titel"] = t); else BaueZeilen();
            }
            feld = new TextFeld("Titel", 14, text: bearbeiteText, absenden: Fertig, schreiben: t => bearbeiteText = t);
            feld.Box.LostKeyboardFocus += (_, _) => Fertig();
            feld.Box.KeyDown += (_, k) => { if (k.Key == Key.Escape) { bearbeite = 0; BaueZeilen(); } };
            Spaeter(() => { feld.Box.Focus(); feld.Box.CaretIndex = feld.Box.Text.Length; });
            titel = feld;
        }
        else
        {
            var t = Tx(e.Titel, 14, e.Angeheftet ? FontWeights.SemiBold : FontWeights.Regular, e.Erledigt ? Farbe.Text3 : Farbe.Text);
            if (e.Erledigt) t.TextDecorations = TextDecorations.Strikethrough;
            t.MouseLeftButtonDown += (_, a) => { if (a.ClickCount == 2) { bearbeite = id; bearbeiteText = e.Titel; BaueZeilen(); a.Handled = true; } };
            t.ToolTip = "Doppelklick: Titel ändern";
            var zusatz = new List<UIElement>();
            if (e.Angeheftet) zusatz.Add(Sym.Bild("bookmark.fill", 10, Farbe.Haupt));
            if (e.Faellig.Length > 0) zusatz.Add(Tx(ListenArten.DatumKurz(e.Faellig), 11, FontWeights.SemiBold, Farbe.Gold));
            foreach (var tag in e.Tags) zusatz.Add(Tx("#" + tag, 11, null, Farbe.Haupt));
            if (z.KinderGesamt > 0) zusatz.Add(Tx($"{z.KinderErledigt}/{z.KinderGesamt}", 11, null, Farbe.Text3));
            if (e.Abschnitt.Length > 0 && liste.Gruppierung != "abschnitt") zusatz.Add(Tx(e.Abschnitt, 11, null, Farbe.Text3));
            var notiz = e.Notiz.Length > 0 ? Tx(e.Notiz, 12, null, Farbe.Text2, zeilen: 2) : null;
            titel = V(3, t, zusatz.Count > 0 ? H(8, zusatz.Append(Spacer()).ToArray()) : null, notiz);
        }

        var werkzeuge = H(4,
            new RundKnopf("plus", () => { unterVon = id; BaueUnterHinweis(); eingabe.Box.Focus(); }, hilfe: "Unterpunkt hinzufügen", groesse: 26),
            new RundKnopf("exclamationmark.circle.fill", () => { var p = (e.Prioritaet + 1) % 4; speicher.Aendern(id, o => o["prioritaet"] = (long)p); }, hilfe: $"Priorität: {ListenArten.PrioName(e.Prioritaet)} (Klick: weiter)", groesse: 26),
            new RundKnopf("bookmark.fill", () => speicher.Aendern(id, o => o["angeheftet"] = !e.Angeheftet), hilfe: e.Angeheftet ? "Lösen" : "Oben anheften", aktiv: e.Angeheftet, groesse: 26),
            e.Eltern != 0 ? new RundKnopf("arrow.uturn.backward", () => speicher.Einhaengen(id, 0), hilfe: "Auf die oberste Ebene", groesse: 26) : null,
            VerschiebenKnopf(e),
            new RundKnopf("trash", () => { speicher.Loeschen(id); Log("listen", "Gelöscht: " + e.Titel, wo: "Ablage/Listen"); }, hilfe: "Löschen (mit Unterpunkten)", groesse: 26));
        werkzeuge.Opacity = 0;
        var zeile = HOben(12, haken, titel.Fuell(), werkzeuge);
        var rahmen = new Border { Child = zeile, Padding = new Thickness(10 + 24 * z.Tiefe, 8, 8, 8), CornerRadius = new CornerRadius(10) };
        rahmen.SchwebeFlaeche(Farbe.Klar, Farbe.Glas(0.05));
        rahmen.Schwebe(an => werkzeuge.Opacity = an ? 1 : 0);
        return rahmen;
    }

    private UIElement? VerschiebenKnopf(ListenEintrag e)
    {
        var andere = speicher.Alle.Where(l => l.Id != e.ListeId).ToList();
        if (andere.Count == 0) return null;
        RundKnopf knopf = null!;
        knopf = new RundKnopf("arrow.right", () =>
        {
            var menue = new ContextMenu();
            foreach (var l in andere)
            {
                var ziel = l.Id;
                var punkt = new MenuItem { Header = ListenArten.Zeichen(l.Art) + "  " + l.Name };
                punkt.Click += (_, _) => { speicher.Verschieben(e.Id, ziel); Log("listen", $"Verschoben nach {l.Name}: {e.Titel}", wo: "Ablage/Listen"); };
                menue.Items.Add(punkt);
            }
            menue.PlacementTarget = knopf;
            menue.IsOpen = true;
        }, hilfe: "In eine andere Liste verschieben", groesse: 26);
        return knopf;
    }
}
