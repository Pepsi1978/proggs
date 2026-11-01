using System.Globalization;

namespace JarvisWindows;

// MARK: - Werkzeuge des Modells (gleiche Parameter, Aktionen und Antworttexte wie ErinnerungenFaehigkeit am Handy)

public static class ErinnerungWerkzeuge
{
    private static AblageAbgleich Abgleich => AblageAbgleich.Shared;
    private static ErinnerungPlaner Planer => ErinnerungPlaner.Shared;
    private const string VorbeiHinweis = " ACHTUNG: Alle Zeitpunkte sind schon vorbei, von selbst meldet sie sich nicht. Sag das Frank und frag nach einer Uhrzeit.";

    private static string S(JSONObjekt a, string k) => a.GetValueOrDefault(k) switch
    {
        null => "",
        string s => s,
        bool b => b ? "true" : "false",
        IFormattable f => f.ToString(null, CultureInfo.InvariantCulture),
        var x => x.ToString() ?? "",
    };
    private static bool Gesetzt(JSONObjekt a, string k) => S(a, k).Trim().Length > 0;

    private static WerkzeugErgebnis Ok(string t) => new(t);
    private static WerkzeugErgebnis Fehler(string t) => new(t, true);

    private static (Erinnerung? E, string? Fehler) Finde(JSONObjekt a)
    {
        var alle = Abgleich.Erinnerungen;
        if (Gesetzt(a, "id"))
        {
            var e = alle.FirstOrDefault(x => x.Id == S(a, "id"));
            return (e, e == null ? "Diese Erinnerung gibt es nicht." : null);
        }
        var wort = S(a, "suche").Trim().ToLowerInvariant();
        if (wort.Length == 0) return (null, "Es fehlt id oder suche.");
        var treffer = alle.Where(x => x.Zeile.ToLowerInvariant().Contains(wort) || x.Kurz.ToLowerInvariant().Contains(wort) || x.Text.ToLowerInvariant().Contains(wort)).ToList();
        if (treffer.Count == 1) return (treffer[0], null);
        if (treffer.Count == 0) return (null, $"Keine Erinnerung passt zu „{wort}“.");
        return (null, "Mehrere passen, bitte genauer: " + string.Join("; ", treffer.Take(8).Select(x => x.Zeile)));
    }

    private static List<int> VorabAus(string t) => t.Split(',', ';').Select(x => int.TryParse(x.Trim(), NumberStyles.Integer, CultureInfo.InvariantCulture, out var m) && m >= 0 ? (int?)m : null).Where(x => x != null).Select(x => x!.Value).Distinct().ToList();

    private static string Kopf(Erinnerung e) => $"{e.DatumAnzeige()}: {e.Zeile} [id {e.Id}]" + (e.Wecker ? " (Wecker)" : "") + (e.Erledigt != 0 ? " (erledigt)" : "");

    private static string StandZeile(Faellig f, long jetzt)
    {
        var q = Planer.QuittungZu(f.Kennung);
        var stand = q != null ? $"bestätigt auf {q.Von}"
            : f.Zeit > jetzt ? "geplant"
            : !ErinnerungPlaner.Meldbar(f, jetzt) ? "verpasst oder beim Einstellen schon vorbei"
            : "fällig, noch nicht bestätigt";
        return $"- {f.Anzeige}: {ErinnerungZeit.ZeitText(f.Zeit)} — {stand}" + (f.Ausloesung != null ? $" [ausloesung_id {f.Ausloesung.Id}]" : "");
    }

    private static string Voll(Erinnerung e)
    {
        var jetzt = ErinnerungZeit.Jetzt;
        var t = $"{Kopf(e)}\nKurzfassung: {e.Kurz}\n\n{e.Text}";
        if (e.Allgemein)
        {
            t += "\n\nAllgemeine Erinnerung ohne Termin.";
            if (e.ZuletztErwaehnt > 0) t += $" Zuletzt erwähnt: {ErinnerungZeit.ZeitText(e.ZuletztErwaehnt)}.";
        }
        var termine = e.Termine();
        if (termine.Count > 0) t += "\n\nZeitpunkte:\n" + string.Join("\n", termine.Select(x => StandZeile(x, jetzt)));
        return t;
    }

    public static WerkzeugErgebnis Lesen(JSONObjekt a)
    {
        if (Gesetzt(a, "id") || Gesetzt(a, "suche"))
        {
            var (e, f) = Finde(a);
            return e != null ? Ok(Voll(e)) : Fehler(f ?? "");
        }
        var art = Gesetzt(a, "art") ? S(a, "art") : "alle";
        var jetzt = ErinnerungZeit.Jetzt;
        var mit = a.Wahr("mit_erledigten") == true || S(a, "mit_erledigten") == "true";
        var datiert = art == "allgemein" ? new List<Erinnerung>() : ErinnerungPlaner.Datiert(jetzt).Where(e =>
            mit || (e.Erledigt == 0 && (e.Termin() ?? 0) >= jetzt) || e.Termine().Any(t => Planer.QuittungZu(t.Kennung) == null && t.Zeit <= jetzt && ErinnerungPlaner.Meldbar(t, jetzt))).ToList();
        var allgemein = art == "datiert" ? new List<Erinnerung>() : ErinnerungPlaner.Allgemeine().Where(e => mit || e.Erledigt == 0).ToList();
        if (datiert.Count == 0 && allgemein.Count == 0) return Ok("Keine Erinnerungen.");
        var teile = new List<string>();
        if (datiert.Count > 0) teile.Add("Mit Datum:\n" + string.Join("\n", datiert.Select(e => "- " + Kopf(e))));
        if (allgemein.Count > 0) teile.Add("Allgemein:\n" + string.Join("\n", allgemein.Select(e => $"- {e.Zeile} [id {e.Id}]" + (e.Erledigt != 0 ? " (erledigt)" : ""))));
        return Ok(string.Join("\n", teile));
    }

    public static async Task<WerkzeugErgebnis> Speichern(JSONObjekt a)
    {
        var aktion = S(a, "aktion");
        if (aktion == "anlegen") return await Anlegen(a);
        var (e, f) = Finde(a);
        if (e == null) return Fehler(f ?? "");
        switch (aktion)
        {
            case "aendern":
            {
                var datum = e.Tag();
                if (Gesetzt(a, "datum"))
                {
                    datum = ErinnerungZeit.DatumAus(S(a, "datum"));
                    if (datum == null) return Fehler($"„{S(a, "datum")}“ ist kein Datum.");
                }
                var uhrzeit = e.Uhrzeit;
                if (Gesetzt(a, "uhrzeit"))
                {
                    if (S(a, "uhrzeit").Trim().ToLowerInvariant() == "keine") uhrzeit = null;
                    else
                    {
                        uhrzeit = ErinnerungZeit.UhrzeitAus(S(a, "uhrzeit"));
                        if (uhrzeit == null) return Fehler($"„{S(a, "uhrzeit")}“ ist keine Uhrzeit.");
                    }
                }
                var form = S(a, "form") is Erinnerung.FormWecker or Erinnerung.FormMeldung ? S(a, "form") : e.Form;
                var neu = e with
                {
                    Text = S(a, "text").Trim() is { Length: > 0 } t ? t : e.Text,
                    Kurz = S(a, "kurz").Trim() is { Length: > 0 } k ? k : e.Kurz,
                    Zeile = S(a, "zeile").Trim() is { Length: > 0 } z ? z : e.Zeile,
                    Datum = datum?.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture), Uhrzeit = uhrzeit, Form = form, Allgemein = datum == null,
                    // Nur beim Wechsel von allgemein zu datiert: Bewusst entfernte Zeitpunkte bleiben entfernt.
                    Ausloesungen = datum != null && e.Allgemein ? new List<Ausloesung> { ErinnerungZeit.NeueAusloesung(0) } : e.Ausloesungen,
                };
                await Abgleich.ErinnerungSpeichern(neu);
                return Ok($"Geändert: {neu.DatumAnzeige()}, {neu.Zeile}, Form {(neu.Wecker ? "Wecker" : "Benachrichtigung")}. Das ist keine Bestätigung." + (neu.Ausloesungen.Count > 0 && !neu.MeldetSichNoch(ErinnerungZeit.Jetzt) ? VorbeiHinweis : ""));
            }
            case "loeschen":
                await Abgleich.ErinnerungLoeschen(e.Id);
                return Ok($"Gelöscht: {e.Zeile}.");
            case "ausloesung_hinzufuegen":
            {
                if (e.Allgemein) return Fehler("Das ist eine allgemeine Erinnerung ohne Termin; dafür gibt es keine Zeitpunkte. Gib ihr erst ein Datum (aendern).");
                string? u = null;
                if (Gesetzt(a, "ausloesung_uhrzeit"))
                {
                    u = ErinnerungZeit.UhrzeitAus(S(a, "ausloesung_uhrzeit"));
                    if (u == null) return Fehler($"Keine Uhrzeit: {S(a, "ausloesung_uhrzeit")}");
                }
                if (a.GetValueOrDefault("minuten") == null) return Fehler("Es fehlt minuten: wie lange vor dem Termin.");
                var neu = ErinnerungZeit.NeueAusloesung((int)Math.Clamp(AblageAbgleich.Zahl(a.GetValueOrDefault("minuten")), 0, int.MaxValue), u);
                if (e.ZeitVon(neu) is not { } zeit) return Fehler("Der Termin ist nicht lesbar.");
                if (zeit <= ErinnerungZeit.Jetzt) return Fehler($"Dieser Zeitpunkt ({ErinnerungZeit.ZeitText(zeit)}) ist schon vorbei, nichts eingestellt.");
                if (e.Doppelt(neu)) return Ok($"Dieser Zeitpunkt ({ErinnerungZeit.ZeitText(zeit)}) ist schon eingestellt.");
                await Abgleich.ErinnerungSpeichern(e with { Ausloesungen = e.Ausloesungen.Append(neu).ToList() });
                return Ok($"Zusätzlich erinnere ich dich {e.VorlaufText(neu)}: {ErinnerungZeit.ZeitText(zeit)}.");
            }
            case "ausloesung_entfernen":
            {
                var id = S(a, "ausloesung_id");
                if (e.Ausloesungen.All(x => x.Id != id)) return Fehler("Diesen Zeitpunkt gibt es nicht.");
                await Abgleich.ErinnerungSpeichern(e with { Ausloesungen = e.Ausloesungen.Where(x => x.Id != id).ToList() });
                return Ok("Zeitpunkt entfernt.");
            }
            case "bestaetigen":
            {
                var jetzt = ErinnerungZeit.Jetzt;
                var offen = e.Termine().Where(t => Planer.QuittungZu(t.Kennung) == null).ToList();
                Faellig? ziel;
                if (Gesetzt(a, "ausloesung_id"))
                {
                    ziel = offen.FirstOrDefault(t => t.Ausloesung?.Id == S(a, "ausloesung_id"));
                    if (ziel == null) return Fehler("Dieser Zeitpunkt ist nicht offen.");
                }
                else
                {
                    // Ohne Angabe nur, was gerade wirklich fällig ist: Nie eine tote oder eine künftige Auslösung statt des klingelnden Weckers.
                    ziel = offen.FirstOrDefault(t => t.Zeit <= jetzt && ErinnerungPlaner.Meldbar(t, jetzt));
                    if (ziel == null) return Ok("Bei dieser Erinnerung ist gerade nichts fällig. Einen künftigen Zeitpunkt bestätigst du nur mit ausloesung_id.");
                }
                Planer.Bestaetige(ziel);
                var rest = offen.Count(t => t.Zeit > jetzt);
                return Ok($"Bestätigt: {ziel.Anzeige}. " + (rest > 0 ? $"{rest} weitere Zeitpunkte bleiben geplant." : "Weitere Zeitpunkte sind nicht geplant."));
            }
            case "erledigt":
                await Abgleich.ErinnerungSpeichern(e with { Erledigt = ErinnerungZeit.Jetzt });
                return Ok($"Als erledigt markiert: {e.Zeile}.");
            case "wieder_offen":
                await Abgleich.ErinnerungSpeichern(e with { Erledigt = 0 });
                return Ok($"Wieder offen: {e.Zeile}. Schon vergangene Zeitpunkte melden sich nicht nachträglich.");
            case "erwaehnt":
                await Abgleich.ErinnerungSpeichern(e with { ZuletztErwaehnt = ErinnerungZeit.Jetzt });
                return Ok("Vermerkt.");
            default:
                return Fehler($"Unbekannte aktion „{aktion}“.");
        }
    }

    private static async Task<WerkzeugErgebnis> Anlegen(JSONObjekt a)
    {
        var text = S(a, "text").Trim();
        if (text.Length == 0) return Fehler("Der Inhalt fehlt.");
        var form = S(a, "form") is Erinnerung.FormWecker or Erinnerung.FormMeldung ? S(a, "form") : null;
        if (form == null) return Fehler("Frag Frank ausdrücklich „Benachrichtigung oder Wecker?“ und übergib seine Auswahl in form.");
        DateOnly? datum = null;
        if (Gesetzt(a, "datum"))
        {
            datum = ErinnerungZeit.DatumAus(S(a, "datum"));
            if (datum == null) return Fehler($"„{S(a, "datum")}“ ist kein Datum.");
        }
        string? uhrzeit = null;
        if (datum != null && Gesetzt(a, "uhrzeit"))
        {
            uhrzeit = ErinnerungZeit.UhrzeitAus(S(a, "uhrzeit"));
            if (uhrzeit == null) return Fehler($"„{S(a, "uhrzeit")}“ ist keine Uhrzeit.");
        }
        var vorab = VorabAus(S(a, "vorab_minuten")).Select(m => ErinnerungZeit.NeueAusloesung(m)).ToList();
        var e = Neu(text, S(a, "zeile"), S(a, "kurz"), datum, uhrzeit, form, vorab);
        await Abgleich.ErinnerungSpeichern(e);
        return Ok(e.Allgemein
            ? $"Als allgemeine Erinnerung gespeichert: {e.Zeile}. Jarvis meldet sich, wenn es passt."
            : $"Gespeichert für {e.DatumAnzeige()}: {e.Zeile}. Form: {(e.Wecker ? "Wecker" : "Benachrichtigung")}. Zeitpunkte: " + string.Join(", ", e.Ausloesungen.Select(e.VorlaufText)) + "." + (e.MeldetSichNoch(ErinnerungZeit.Jetzt) ? "" : VorbeiHinweis));
    }

    /// <summary>Eine neue Erinnerung; leere Kurzfassung und Zeile entstehen aus dem Text, ein Termin ohne Zeitpunkt erinnert zum Termin.</summary>
    public static Erinnerung Neu(string text, string zeile, string kurz, DateOnly? datum, string? uhrzeit, string form, IReadOnlyList<Ausloesung>? vorab = null)
    {
        var sauber = text.Trim();
        var jetzt = ErinnerungZeit.Jetzt;
        return new Erinnerung
        {
            Id = ErinnerungZeit.NeueId(), Allgemein = datum == null, Zeile = zeile.Trim().Length > 0 ? zeile.Trim() : ErinnerungZeit.ZeileAus(sauber), Kurz = kurz.Trim().Length > 0 ? kurz.Trim() : ErinnerungZeit.KurzAus(sauber),
            Text = sauber, Datum = datum?.ToString("yyyy-MM-dd", CultureInfo.InvariantCulture), Uhrzeit = datum == null ? null : uhrzeit, OhneUhrzeitUm = Abgleich.OhneUhrzeitUm, Form = form,
            Ausloesungen = datum == null ? Array.Empty<Ausloesung>() : vorab is { Count: > 0 } ? vorab : new List<Ausloesung> { ErinnerungZeit.NeueAusloesung(0) },
            Quelle = AblageAbgleich.Quelle, Erstellt = jetzt, Geaendert = jetzt,
        };
    }
}
