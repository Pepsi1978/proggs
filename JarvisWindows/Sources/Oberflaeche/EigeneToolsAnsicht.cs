using System.Text.RegularExpressions;
using System.Windows;
using System.Windows.Controls;
using static JarvisWindows.Bau;

namespace JarvisWindows;

/// <summary>„Eigene Tools“: Inhaltsverzeichnis der Helfer dieses PCs mit Detailbereich, oben Pause und Recherche-Wahl.</summary>
public sealed class EigeneToolsAnsicht : LebendeAnsicht
{
    private readonly Einstellungen einst = Einstellungen.Shared;
    private readonly HelferKatalog katalog = HelferKatalog.Shared;
    private string? offen;

    public EigeneToolsAnsicht() { Beobachte(katalog); Beobachte(katalog.Eintraege); }

    protected override UIElement Baue()
    {
        if (offen is { } k && katalog.Finde(k) is { } e && e.Zustand != HelferKatalog.Papierkorb) return Detail(e);
        return Uebersicht();
    }

    private void Loeschfrage(HelferEintrag e)
    {
        var nachricht = "Gelöscht werden Programm, frühere Fassung, eigener Speicher und Ergebnisdateien dieses Helfers."
            + (e.Konten.Count() == 0 ? "" : $" Die Konten {string.Join(", ", e.Konten)} bleiben erhalten.") + " Das lässt sich nicht rückgängig machen.";
        if (Bestaetigen($"{e.Name} endgültig löschen?", nachricht, "Endgültig löschen")) { katalog.EndgueltigLoeschen(e.Kennung); Neu(); }
    }

    private UIElement Uebersicht()
    {
        var kopf = Karte(V(10,
            Titel("wrench.and.screwdriver.fill", "Eigene Tools"),
            T("Helfer, die Jarvis für deine Aufgaben bauen lässt, prüft und selbst benutzt.", 12, null, Farbe.Text2),
            H(10, Fuell(T("Helfer selbst bauen lassen", 14, FontWeights.SemiBold)), new Schalter(() => einst.HelferAutomatik, v => einst.HelferAutomatik = v)),
            H(14,
                T("Recherche", 14, FontWeights.SemiBold),
                Spacer(),
                Auswahl(ModellWahl.FuerRecherche.Select(m => (m.Id, m.Name)), () => einst.RechercheModell, v => einst.RechercheModell = v, 190),
                Auswahl(new[] { ("medium", "Mittel"), ("high", "High"), ("xhigh", "Sehr hoch"), ("max", "Maximal") }, () => einst.RechercheEffort, v => einst.RechercheEffort = v, 120))));

        var inhalt = V(4, Titel("list.bullet", "Inhalt"));
        var sortiert = katalog.Sortiert.ToList();
        if (sortiert.Count == 0) VAnfuegen(inhalt, Rand(T("Noch keine Helfer. Jarvis legt hier an, was deine Aufgaben brauchen.", 13, null, Farbe.Text2), o: 4), 4);
        foreach (var e in sortiert)
        {
            var zeile = H(10,
                Kreis(FarbeVon(e)),
                Fuell(V(1, T(e.Name, 14, FontWeights.SemiBold), T(e.Text("zweck"), 12, null, Farbe.Text2, 1))),
                T(ZustandText(e), 12, null, Farbe.Text2),
                Sy("chevron.right", 11, Farbe.Text3));
            VAnfuegen(inhalt, Rand(Klick(zeile, () => { offen = e.Kennung; Neu(); }), o: 7, u: 7), 4);
        }

        var s = V(18, kopf, Karte(inhalt));
        var korb = katalog.ImPapierkorb.ToList();
        if (korb.Count > 0)
        {
            var k = V(8, Titel("trash", "Papierkorb", Farbe.Text2));
            foreach (var e in korb)
                VAnfuegen(k, H(8,
                    Fuell(T(e.Name, 13, FontWeights.Medium)),
                    Kapsel("arrow.uturn.backward", "Wiederherstellen", () => { _ = katalog.Verwalten(e.Kennung, "wiederherstellen"); Neu(); }),
                    Kapsel("trash", "Löschen", () => Loeschfrage(e))), 8);
            VAnfuegen(s, Karte(k), 18);
        }
        return s;
    }

    private UIElement Detail(HelferEintrag e)
    {
        var d = V(10,
            Titel("wrench.adjustable.fill", e.Name),
            H(8, Kreis(FarbeVon(e)), T(ZustandText(e), 13, FontWeights.SemiBold), T($"Fassung {e.Text("version")} · am PC", 12, null, Farbe.Text2), Spacer()),
            Angabe("Wofür", e.Text("zweck")),
            Angabe("Kann", string.Join(", ", e.Liste("faehigkeiten"))),
            Angabe("Aufruf", $"Jarvis ruft windows_helfer_ausfuehren mit der Kennung {e.Kennung}; vom Handy aus helfer_ausfuehren."),
            Angabe("Braucht", EingabenText(e)),
            Angabe("Liefert", e.Text("ausgabe")),
            Angabe("Voraussetzungen", string.Join(", ", e.Liste("voraussetzungen"))),
            Angabe("Rechte", RechteText(e)),
            Angabe("Grenzen", string.Join(", ", e.Liste("grenzen"))),
            e.Manifest.Wahr("braucht_app_update") == true ? Angabe("Hinweis", "Braucht ein Update von Jarvis.") : null,
            Angabe("Letzte Prüfung", PruefungText(e)));
        VAnfuegen(d, H(8,
            e.Zustand == HelferKatalog.Wartet ? Kapsel("checkmark.seal.fill", "Rechte freigeben", () => { katalog.Freigeben(e.Kennung); Neu(); }, hervorgehoben: true) : null,
            Kapsel(e.Aktiv ? "pause.fill" : "play.fill", e.Aktiv ? "Deaktivieren" : "Aktivieren", () => { _ = katalog.Verwalten(e.Kennung, e.Aktiv ? "deaktivieren" : "aktivieren"); Neu(); }),
            katalog.HatVorherigeFassung(e.Kennung) ? Kapsel("arrow.uturn.backward", "Letzte Fassung", () => { _ = katalog.Verwalten(e.Kennung, "rueckrollen"); Neu(); }) : null,
            Kapsel("arrow.up", "Höher", () => { katalog.Verschieben(e.Kennung, -1); Neu(); }),
            Kapsel("arrow.down", "Tiefer", () => { katalog.Verschieben(e.Kennung, 1); Neu(); }),
            Kapsel("trash", "Papierkorb", () => { _ = katalog.Verwalten(e.Kennung, "papierkorb"); offen = null; Neu(); }),
            Spacer()), 10);
        var zurueck = H(0, Kapsel("chevron.left", "Eigene Tools", () => { offen = null; Neu(); }), Spacer());
        return V(18, zurueck, Karte(d));
    }

    private static UIElement? Angabe(string titel, string text)
    {
        if (text.Length == 0) return null;
        var t = T(titel, 12, FontWeights.SemiBold, Farbe.Text2);
        t.Width = 110;
        // TextBox statt TextBlock: Der Wert bleibt markierbar (.textSelection(.enabled)).
        var wert = new TextBox
        {
            Text = text, IsReadOnly = true, BorderThickness = new Thickness(0), Background = System.Windows.Media.Brushes.Transparent, Foreground = B(Farbe.Text), FontFamily = Schrift, FontSize = 13,
            TextWrapping = TextWrapping.Wrap, Padding = new Thickness(0), SelectionBrush = B(Farbe.Haupt),
        };
        return HOben(10, t, Fuell(wert));
    }

    private static string EingabenText(HelferEintrag e)
    {
        var s = e.Manifest.Obj("eingabe") ?? new JSONObjekt();
        var pflicht = s.Texte("required");
        return string.Join(", ", (s.Obj("properties") ?? new JSONObjekt()).Keys.OrderBy(x => x, StringComparer.Ordinal).Select(k => pflicht.Contains(k) ? $"{k} (nötig)" : k));
    }

    private static string RechteText(HelferEintrag e)
    {
        var t = new List<string>();
        var konten = e.Konten.ToList();
        if (konten.Count > 0) t.Add("Konten: " + string.Join(", ", konten));
        var rechte = e.Rechte;
        var netz = rechte.Texte("netz");
        if (netz.Count > 0) t.Add("Netz: " + string.Join(", ", netz));
        if (rechte.Wahr("extern") == true) t.Add("wirkt nach außen");
        return t.Count == 0 ? "Keine besonderen" : string.Join(" · ", t);
    }

    private static string PruefungText(HelferEintrag e)
    {
        if (e.PruefungZeit <= 0) return e.PruefungErgebnis;
        var d = DateTimeOffset.FromUnixTimeMilliseconds((long)(e.PruefungZeit * 1000)).LocalDateTime;
        return d.ToString("dd.MM.yyyy, HH:mm", Deutsch) + " Uhr: " + e.PruefungErgebnis;
    }

    private static string ZustandText(HelferEintrag e)
    {
        if (!e.Aktiv) return "Deaktiviert";
        if (e.Zustand == HelferKatalog.Bereit) return "Einsatzbereit";
        if (e.Zustand == HelferKatalog.InPruefung) return "In Prüfung";
        if (e.Zustand == HelferKatalog.Wartet) return "Wartet auf Freigabe";
        if (e.Zustand == HelferKatalog.Fehlerhaft) return "Fehlerhaft";
        return "Im Papierkorb";
    }

    private static object FarbeVon(HelferEintrag e)
    {
        if (!e.Aktiv) return Farbe.Text3;
        if (e.Zustand == HelferKatalog.Bereit) return Farbe.Gruen;
        if (e.Zustand == HelferKatalog.Fehlerhaft) return Farbe.Rot;
        return Farbe.Warnung;
    }
}

/// <summary>Alle Schlüssel an einer Stelle, jeder mit zwei kurzen Sätzen. Die Werte bleiben, wo sie sind; nur die Ansicht ist zentral.</summary>
public sealed class KontenListe : LebendeAnsicht
{
    private readonly Einstellungen einst = Einstellungen.Shared;
    private readonly VorleseEinstellungen stimmen = VorleseEinstellungen.Shared;
    private readonly HelferKatalog katalog = HelferKatalog.Shared;
    private readonly TresorSperre sperre = TresorSperre.Shared;
    private string neuTitel = "", neuBeschreibung = "", neuDienst = "";
    private readonly TextBindung titelB, beschreibungB, dienstB;

    public KontenListe()
    {
        titelB = new TextBindung(() => neuTitel, v => neuTitel = v);
        beschreibungB = new TextBindung(() => neuBeschreibung, v => neuBeschreibung = v);
        dienstB = new TextBindung(() => neuDienst, v => neuDienst = v);
        Beobachte(sperre, nameof(TresorSperre.Entsperrt));
    }

    private Zugang Z => einst.Zugang;
    private TextBindung Bindung(Func<Zugang, string> lies, Action<Zugang, string> setze) => new(() => lies(Z), v => { setze(Z, v); einst.ZugangGeaendert(); });

    protected override UIElement Baue()
    {
        var s = V(14,
            Eintrag("anthropic", "Anthropic (Claude)", "Meldet Jarvis bei den Claude-Modellen an. Wird für Chat und Lernen mit Claude verwendet.", "Jarvis' Kopf", Bindung(z => z.Anthropic, (z, v) => z.Anthropic = v)),
            Eintrag("openai", "OpenAI (GPT)", "Meldet Jarvis bei der OpenAI-Plattform an. Wird für GPT ohne ChatGPT-Anmeldung verwendet.", "Jarvis' Kopf", Bindung(z => z.Openai, (z, v) => z.Openai = v)),
            Eintrag("gemini", "Gemini", "Meldet Jarvis bei Google AI Studio an. Wird für die Gemini-Stimmen beim Vorlesen verwendet.", "Vorlesen (Gemini-TTS)", Bindung(z => z.Gemini, (z, v) => z.Gemini = v)),
            Eintrag("groq", "Groq (Whisper)", "Meldet Jarvis bei Groq an. Wird für die Umwandlung von Sprache in Text verwendet.", "Spracheingabe", Bindung(z => z.Groq, (z, v) => z.Groq = v)),
            Eintrag("google", "Google Cloud (Chirp 3 HD)", "Meldet Jarvis bei Google Cloud an. Wird für die Vorlesestimmen Chirp 3 HD verwendet.", "Vorlesen", VorleseBindung(z => z.Google, (z, v) => z.Google = v)),
            Eintrag("gemini_tts_frei", "Gemini TTS kostenlos", "Schlüssel eines Projekts ohne Abrechnung. Wird zuerst für die Gemini-Vorlesestimmen verwendet.", "Vorlesen", VorleseBindung(z => z.GeminiFrei, (z, v) => z.GeminiFrei = v)),
            Eintrag("gemini_tts_bezahlt", "Gemini TTS bezahlt", "Schlüssel eines Projekts mit Abrechnung. Wird für die Gemini-Stimmen verwendet, wenn das Kontingent erschöpft ist.", "Vorlesen", VorleseBindung(z => z.GeminiBezahlt, (z, v) => z.GeminiBezahlt = v)),
            Eintrag("azure", "Azure Speech", "Meldet Jarvis bei Azure Speech an. Wird für die Azure-Vorlesestimmen verwendet.", "Vorlesen", VorleseBindung(z => z.Azure, (z, v) => z.Azure = v)),
            Eintrag("qwen", "Alibaba DashScope", "Meldet Jarvis bei Alibaba an. Wird für die Qwen-Stimmen und die eigene Stimme verwendet.", "Vorlesen", VorleseBindung(z => z.Qwen, (z, v) => z.Qwen = v)));
        foreach (var k in Z.Weitere.Keys.OrderBy(x => x, StringComparer.Ordinal)) VAnfuegen(s, Weiterer(k), 14);
        VAnfuegen(s, sperre.Entsperrt ? NeuForm() : T("Zum Hinzufügen oder Ändern mit Windows Hello entsperren.", 11, null, Farbe.Text3), 14);
        return s;
    }

    private TextBindung VorleseBindung(Func<VorleseZugang, string> lies, Action<VorleseZugang, string> setze)
        => new(() => lies(stimmen.Zugang), v => { setze(stimmen.Zugang, v); stimmen.ZugangGeaendert(); });

    private string Verwendet(string fest, string id)
    {
        var helfer = id.Length == 0 ? new List<string>() : katalog.Nutzer(id).ToList();
        return "Verwendet von: " + string.Join(", ", new[] { fest }.Concat(helfer).Where(x => x.Length > 0));
    }

    private UIElement Eintrag(string id, string titel, string zweiSaetze, string fest, TextBindung wert)
    {
        var gesperrt = Z.GesperrtFuerHelfer.Contains(id);
        return V(3,
            new TresorFeld(titel, wert),
            T(zweiSaetze, 12, null, Farbe.Text2),
            H(8,
                Fuell(T(Verwendet(fest, id), 11, null, Farbe.Text3)),
                id.Length == 0 ? null : TextKnopf(gesperrt ? "Für Helfer entsperren" : "Für Helfer sperren", 11, gesperrt ? Farbe.Warnung : Farbe.Text2, () =>
                {
                    if (gesperrt) Z.GesperrtFuerHelfer.RemoveAll(x => x == id); else Z.GesperrtFuerHelfer.Add(id);
                    einst.ZugangGeaendert();
                    Neu();
                })));
    }

    private UIElement Weiterer(string k)
    {
        var k2 = Z.Weitere.TryGetValue(k, out var w) ? w : new WeiteresKonto();
        var wert = new TextBindung(() => k2.Wert, v => { k2.Wert = v; einst.ZugangGeaendert(); });
        var beschreibung = new TextBindung(() => k2.Beschreibung, v => { k2.Beschreibung = v; einst.ZugangGeaendert(); });
        return V(3,
            new TresorFeld(k2.Titel, wert),
            sperre.Entsperrt ? Feld(beschreibung, "Zwei Sätze: was macht er, wofür?", 12) : T(k2.Beschreibung, 12, null, Farbe.Text2),
            H(8,
                Fuell(T(Verwendet(k2.Dienst, k), 11, null, Farbe.Text3)),
                TextKnopf(k2.Gesperrt ? "Entsperren" : "Sperren", 11, k2.Gesperrt ? Farbe.Warnung : Farbe.Text2, () => { k2.Gesperrt = !k2.Gesperrt; einst.ZugangGeaendert(); Neu(); }),
                sperre.Entsperrt ? TextKnopf("Entfernen", 11, Farbe.Text2, () => Entfernen(k)) : null));
    }

    private void Entfernen(string k)
    {
        var nutzer = katalog.Nutzer(k).ToList();
        var nachricht = nutzer.Count == 0 ? "Kein Helfer benutzt ihn." : $"Diese Helfer funktionieren danach nicht mehr: {string.Join(", ", nutzer)}.";
        if (!Bestaetigen("Schlüssel entfernen?", nachricht, "Entfernen")) return;
        Z.Weitere.Remove(k);
        einst.ZugangGeaendert();
        Neu();
    }

    private UIElement NeuForm() => V(6,
        T("Neuer Schlüssel", 13, FontWeights.SemiBold),
        Feld(titelB, "Name, z. B. YouTube", 12),
        Feld(beschreibungB, "Zwei Sätze: was macht er, wofür?", 12),
        Feld(dienstB, "Dienst, z. B. YouTube Data API", 12),
        H(0, Kapsel("plus", "Hinzufügen", () =>
        {
            var titel = neuTitel.Trim();
            var k = string.Concat(titel.ToLowerInvariant().Select(c => char.IsLetterOrDigit(c) ? c.ToString() : "-"));
            if (k.Length == 0 || Z.Weitere.ContainsKey(k)) return;
            Z.Weitere[k] = new WeiteresKonto { Titel = titel, Beschreibung = neuBeschreibung, Dienst = neuDienst };
            einst.ZugangGeaendert();
            titelB.Wert = ""; beschreibungB.Wert = ""; dienstB.Wert = "";
            Neu();
        }), Spacer()));
}
