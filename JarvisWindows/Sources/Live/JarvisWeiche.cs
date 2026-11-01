using System.Text.RegularExpressions;

namespace JarvisWindows;

/// <summary>Gemini Live mit „Jarvis direkt“ – 1:1 aus der Handy-App (Jarvis/live/JarvisWeiche.kt) übernommen.
/// Die App, nicht das Sprachmodell, sorgt dafür, dass jeder abgeschlossene Beitrag von Frank an Jarvis geht und Gemini
/// nur Jarvis' Antworten spricht (Gemini 3.8 Live hat Proactive Audio fest an und kennt keinen erzwungenen Werkzeugaufruf).
/// Reine Logik; alle Aufrufe unter einer Sperre, die zurückgegebenen Aktionen führt das Live-Gespräch danach aus.</summary>
public sealed class JarvisWeiche
{
    public abstract record Aktion
    {
        public sealed record NutzerFertig : Aktion;
        public sealed record AnJarvis(string Kennung, string Auftrag, string Hinweis) : Aktion;
        public sealed record Werkzeugantwort(List<string> Ids, string Text) : Aktion;
        public sealed record Einspielen(string Text) : Aktion;
        public sealed record Anzeigen(string Text) : Aktion;
        public sealed record Signal(List<byte[]> Ton, string Text) : Aktion;
        public sealed record Fehler(string Text) : Aktion;
        public sealed record Protokoll(string Text) : Aktion;
    }

    private enum TorArt { Begruessung, Zu, Wartet, Offen }

    public const string Marke = "[JARVIS-ANTWORT] ";
    public const string MitschriftFehlt = "(Keine Mitschrift von Franks Worten; sinngemäß vom Sprachmodell:) ";
    public const long NachlaufMs = 350;
    public const long MitschriftWartenMs = 1_500;
    public const long WartenMaxMs = 1_500;
    public const long StummMaxMs = 10_000;
    public const long JarvisMaxMs = 150_000;
    public const int EchoMax = 2;
    public const int OhneMitschriftMax = 2;
    public static readonly HashSet<string> SignalWorte = new() { "mhm", "mm", "hm", "hmm", "okay", "ok", "oke", "moment", "einen", "kleinen", "augenblick", "sekunde",
        "ich", "schau", "schaue", "seh", "sehe", "mal", "kurz", "nach", "lass", "mich", "nachsehen", "nachschauen", "verstehe", "aha", "ah", "oh", "so" };
    public const int SignalMaxWorte = 6;
    public const int SignalMaxBytes = 144_000;
    public const long SignalNachlaufMs = 400;
    public const long SignalMaxMs = 6_000;

    private readonly long ruheMs;
    private readonly object sperre = new();

    private TorArt tor = TorArt.Begruessung;
    public bool TonFrei { get { lock (sperre) return tor == TorArt.Begruessung || tor == TorArt.Offen; } }

    private sealed record Uebergabe(string Kennung, long SeitMs);
    private sealed record OhneMitschriftEintrag(string Auftrag, long SeitMs, bool Begruessung);
    private sealed class Gepuffert
    {
        public readonly long SeitMs;
        public readonly List<byte[]> Ton = new();
        public int Bytes;
        public string Text = "";
        public long EndeMs;
        public Gepuffert(long s) { SeitMs = s; }
    }

    private string beitrag = "";
    private long letzteMitschriftMs;
    private long letzteSpracheMs;
    private bool abschlussFaellig;
    private string ersatzAuftrag = "";
    private OhneMitschriftEintrag? ohneMitschrift;
    private bool erzeugt;
    private readonly List<string> offeneAufrufe = new();
    private Uebergabe? laufend;
    private string? wartend;
    private string? ungehoert;
    private bool unterbrochenFlag;
    private string letzteAntwort = "";
    private int echos;
    private bool nutzerSeitAbgabe;
    private long abgabeMs;
    private long wartetSeitMs;
    private bool tonSeitOffen;
    private Gepuffert? gepuffert;
    private bool signalGenutzt;
    private string letztesSignal = "";
    private int ohneMitschriftFolge;
    private int nr;

    public JarvisWeiche(long ruheMs) { this.ruheMs = ruheMs; }

    public bool Arbeitet { get { lock (sperre) return laufend != null || wartend != null; } }

    public List<Aktion> NutzerText(string text, long jetzt)
    {
        lock (sperre)
        {
            if (string.IsNullOrWhiteSpace(text)) return Pruefen(jetzt);
            beitrag += text;
            letzteMitschriftMs = jetzt; letzteSpracheMs = jetzt;
            nutzerSeitAbgabe = true;
            ohneMitschriftFolge = 0;
            if (tor == TorArt.Begruessung) tor = TorArt.Zu;
            var aus = SignalVerwerfenWennNeu(jetzt);
            if ((tor == TorArt.Offen || tor == TorArt.Wartet) && !tonSeitOffen)
            {
                tor = TorArt.Zu;
                ungehoert = ungehoert != null ? $"{ungehoert}\n{letzteAntwort}" : letzteAntwort;
            }
            aus.AddRange(Pruefen(jetzt));
            return aus;
        }
    }

    public List<Aktion> NutzerSpricht(long jetzt)
    {
        lock (sperre) { letzteSpracheMs = jetzt; return SignalVerwerfenWennNeu(jetzt); }
    }

    private List<Aktion> SignalVerwerfenWennNeu(long jetzt)
    {
        if (gepuffert == null) return new();
        if (jetzt - gepuffert.SeitMs <= NachlaufMs) return new();
        return SignalVerwerfen("Frank spricht weiter", verbraucht: false);
    }

    private List<Aktion> SignalVerwerfen(string grund, bool verbraucht = true)
    {
        if (gepuffert == null) return new();
        var g = gepuffert;
        gepuffert = null;
        if (verbraucht) signalGenutzt = true;
        return new() { new Aktion.Protokoll($"Weiche: eigene Äußerung von Gemini stumm ({grund}): „{g.Text.Trim().Schnitt(80)}“") };
    }

    public void EigenerTon(byte[] pcm, long jetzt)
    {
        lock (sperre)
        {
            var g = Puffer(jetzt);
            if (g == null || g.EndeMs != 0) return;
            g.Bytes += pcm.Length;
            if (g.Bytes > SignalMaxBytes) g.Ton.Clear(); else g.Ton.Add(pcm);
        }
    }

    public void EigeneMitschrift(string text, long jetzt)
    {
        lock (sperre)
        {
            var g = Puffer(jetzt);
            if (g != null) g.Text += text;
        }
    }

    private Gepuffert? Puffer(long jetzt)
    {
        if (gepuffert != null) return gepuffert;
        if (tor != TorArt.Zu || signalGenutzt || (beitrag.Length == 0 && !(laufend != null || wartend != null))) return null;
        gepuffert = new Gepuffert(jetzt);
        return gepuffert;
    }

    public List<Aktion> ModellAusgabe(long jetzt, bool ton = true)
    {
        lock (sperre)
        {
            erzeugt = true;
            var aus = new List<Aktion>();
            if (ton && tor == TorArt.Offen && !tonSeitOffen)
            {
                tonSeitOffen = true;
                aus.Add(new Aktion.Protokoll($"Weiche: Gemini spricht Jarvis' Antwort, {jetzt - abgabeMs} ms nach der Abgabe"));
            }
            if (tor == TorArt.Zu && beitrag.Length > 0) abschlussFaellig = true;
            aus.AddRange(Pruefen(jetzt));
            return aus;
        }
    }

    public List<Aktion> WerkzeugAufruf(string id, string auftrag, long jetzt)
    {
        lock (sperre)
        {
            erzeugt = false;
            var aus = new List<Aktion>();
            if (gepuffert != null && gepuffert.EndeMs == 0) gepuffert.EndeMs = jetzt;
            if (beitrag.Length > 0)
            {
                offeneAufrufe.Add(id); abschlussFaellig = true; ersatzAuftrag = auftrag;
            }
            else if (laufend != null || wartend != null)
            {
                offeneAufrufe.Add(id);
            }
            else if ((tor == TorArt.Offen || tor == TorArt.Wartet) && !nutzerSeitAbgabe && letzteAntwort.Length > 0)
            {
                echos += 1;
                if (echos <= EchoMax)
                {
                    aus.Add(new Aktion.Protokoll($"Weiche: Gemini fragt Jarvis erneut ohne neuen Beitrag ({echos}), Antwort aus dem Speicher"));
                    aus.Add(new Aktion.Werkzeugantwort(new List<string> { id }, letzteAntwort));
                    tor = TorArt.Offen;
                }
                else
                {
                    aus.Add(new Aktion.Werkzeugantwort(new List<string> { id }, "Das ist bereits Jarvis' Antwort. Sag jetzt nichts mehr."));
                    aus.Add(new Aktion.Anzeigen(letzteAntwort));
                    aus.Add(new Aktion.Fehler("Gemini hat Jarvis' Antwort nicht vorgelesen; sie steht im Chat."));
                    tor = TorArt.Zu;
                }
            }
            else
            {
                offeneAufrufe.Add(id);
                ohneMitschrift = new OhneMitschriftEintrag(auftrag, jetzt, tor == TorArt.Begruessung);
            }
            aus.AddRange(Pruefen(jetzt));
            return aus;
        }
    }

    public void WerkzeugAbgebrochen(IEnumerable<string> ids)
    {
        lock (sperre) { var menge = ids.ToHashSet(); offeneAufrufe.RemoveAll(menge.Contains); }
    }

    public void VerbindungNeu()
    {
        lock (sperre) { offeneAufrufe.Clear(); ohneMitschrift = null; }
    }

    public List<Aktion> ZugEnde(long jetzt)
    {
        lock (sperre)
        {
            erzeugt = false;
            var aus = new List<Aktion>();
            switch (tor)
            {
                case TorArt.Begruessung: tor = TorArt.Zu; break;
                case TorArt.Wartet: tor = TorArt.Offen; abgabeMs = jetzt; break;
                case TorArt.Offen:
                    if (tonSeitOffen) { tor = TorArt.Zu; aus.Add(new Aktion.Protokoll("Weiche: Jarvis' Antwort fertig gesprochen")); }
                    break;
                case TorArt.Zu:
                    if (beitrag.Length > 0) abschlussFaellig = true;
                    if (gepuffert != null && gepuffert.EndeMs == 0) gepuffert.EndeMs = jetzt;
                    break;
            }
            aus.AddRange(Pruefen(jetzt));
            return aus;
        }
    }

    public List<Aktion> Unterbrochen(long jetzt)
    {
        lock (sperre)
        {
            erzeugt = false;
            var aus = SignalVerwerfen("unterbrochen", verbraucht: false);
            switch (tor)
            {
                case TorArt.Begruessung: tor = TorArt.Zu; break;
                case TorArt.Wartet: tor = TorArt.Offen; abgabeMs = jetzt; break;
                case TorArt.Offen: if (tonSeitOffen) { tor = TorArt.Zu; unterbrochenFlag = true; } break;
                case TorArt.Zu: break;
            }
            aus.AddRange(Pruefen(jetzt));
            return aus;
        }
    }

    public void Angetippt()
    {
        lock (sperre) { if (tor == TorArt.Offen && tonSeitOffen) unterbrochenFlag = true; }
    }

    public List<Aktion> JarvisAntwort(string kennung, string text, long jetzt)
    {
        lock (sperre)
        {
            var l = laufend;
            if (l == null || l.Kennung != kennung) return new() { new Aktion.Protokoll($"Weiche: verspätete Antwort zu {kennung} verworfen") };
            laufend = null;
            var t = text.Trim();
            var antwort = t.Length == 0 ? "Jarvis hat keine Antwort geliefert." : t;
            var aus = new List<Aktion> { new Aktion.Protokoll($"Weiche: Antwort von Jarvis nach {jetzt - l.SeitMs} ms, {antwort.Length} Zeichen") };
            if (wartend != null || beitrag.Length > 0)
            {
                ungehoert = ungehoert != null ? $"{ungehoert}\n{antwort}" : antwort;
                aus.Add(new Aktion.Protokoll("Weiche: Frank sprach weiter, Antwort geht mit dem nächsten Beitrag an Jarvis"));
            }
            else
            {
                aus.AddRange(Abgeben(antwort, jetzt));
            }
            aus.AddRange(Pruefen(jetzt));
            return aus;
        }
    }

    /// <summary>Ergänzung der Desktop-Fassung: ein spätes Ergebnis (nach dem Zwischenstand) jetzt sprechen lassen.</summary>
    public List<Aktion> Nachreichen(string text, long jetzt)
    {
        lock (sperre)
        {
            var aus = Abgeben(text, jetzt);
            aus.AddRange(Pruefen(jetzt));
            return aus;
        }
    }

    public List<Aktion> Takt(long jetzt)
    {
        lock (sperre) return Pruefen(jetzt);
    }

    private List<Aktion> Pruefen(long jetzt)
    {
        var aus = new List<Aktion>();
        if (tor == TorArt.Wartet && jetzt - wartetSeitMs >= WartenMaxMs) { tor = TorArt.Offen; abgabeMs = jetzt; }
        if (tor == TorArt.Offen && !tonSeitOffen && abgabeMs > 0 && letzteAntwort.Length > 0 && jetzt - abgabeMs >= StummMaxMs)
        {
            aus.Add(new Aktion.Anzeigen(letzteAntwort));
            aus.Add(new Aktion.Fehler("Gemini hat Jarvis' Antwort nicht vorgelesen; sie steht im Chat."));
            tor = TorArt.Zu;
        }
        if (laufend != null && jetzt - laufend.SeitMs >= JarvisMaxMs)
        {
            aus.AddRange(JarvisAntwort(laufend.Kennung, "Jarvis hat nicht rechtzeitig geantwortet. Ich kann dazu gerade nichts sagen; frag bitte gleich noch einmal.", jetzt));
        }
        if (ohneMitschrift != null)
        {
            var o = ohneMitschrift;
            if (beitrag.Length > 0) { ohneMitschrift = null; abschlussFaellig = true; }
            else if (jetzt - o.SeitMs >= MitschriftWartenMs)
            {
                ohneMitschrift = null;
                if (o.Begruessung || o.Auftrag.Trim().Length == 0 || ohneMitschriftFolge >= OhneMitschriftMax)
                {
                    if (ohneMitschriftFolge >= OhneMitschriftMax && !o.Begruessung)
                    {
                        aus.Add(new Aktion.Fehler("Gemini liefert keine Mitschrift deiner Worte; bitte das Live-Gespräch neu starten."));
                    }
                    aus.Add(new Aktion.Werkzeugantwort(offeneAufrufe.ToList(), "Frank hat noch nichts gesagt. Sag nichts weiter."));
                    offeneAufrufe.Clear();
                }
                else
                {
                    ohneMitschriftFolge += 1;
                    beitrag += o.Auftrag;
                    ersatzAuftrag = MitschriftFehlt;
                    abschlussFaellig = true;
                    letzteMitschriftMs = 0;
                }
            }
        }
        var ruhig = jetzt - Math.Max(letzteMitschriftMs, letzteSpracheMs);
        if (beitrag.Length > 0 && (abschlussFaellig || ruhig >= ruheMs) && jetzt - letzteMitschriftMs >= NachlaufMs)
        {
            var text = beitrag.Trim();
            var ersatz = ersatzAuftrag;
            beitrag = ""; abschlussFaellig = false; ersatzAuftrag = "";
            aus.Add(new Aktion.NutzerFertig());
            aus.Add(new Aktion.Protokoll($"Weiche: Beitrag abgeschlossen ({text.Length} Zeichen)"));
            var worte = ersatz == MitschriftFehlt ? MitschriftFehlt + text : text;
            wartend = wartend != null ? $"{wartend}\n{worte}" : worte;
        }
        if (laufend == null && wartend != null)
        {
            var w = wartend;
            wartend = null;
            aus.Add(Uebergeben(w, jetzt));
        }
        aus.AddRange(SignalPruefen(jetzt));
        return aus;
    }

    private List<Aktion> SignalPruefen(long jetzt)
    {
        var g = gepuffert;
        if (g == null) return new();
        if (g.EndeMs == 0) return jetzt - g.SeitMs >= SignalMaxMs ? SignalVerwerfen("kein Ende") : new();
        if (jetzt - g.EndeMs < SignalNachlaufMs) return new();
        var text = g.Text.Trim();
        string? grund;
        if (g.Bytes > SignalMaxBytes) grund = "zu lang";
        else if (!NeutralesSignal(text)) grund = "kein neutrales Signal";
        else if (SignalSchluessel(text) == letztesSignal) grund = "wiederholt";
        else if (g.Ton.Count == 0) grund = "kein Ton";
        else if (tor != TorArt.Zu || (!(laufend != null || wartend != null) && beitrag.Length == 0)) grund = "nichts zu überbrücken";
        else grund = null;
        if (grund != null) return SignalVerwerfen(grund);
        gepuffert = null;
        signalGenutzt = true;
        letztesSignal = SignalSchluessel(text);
        return new() { new Aktion.Signal(g.Ton, text), new Aktion.Protokoll($"Weiche: neutrales Signal von Gemini hörbar: „{text}“") };
    }

    private Aktion Uebergeben(string worte, long jetzt)
    {
        nr += 1;
        var kennung = $"weiche-{nr}";
        var hinweis = "";
        if (unterbrochenFlag) hinweis += "(Frank hat deine vorige gesprochene Antwort unterbrochen.)";
        if (ungehoert != null)
        {
            hinweis += (hinweis.Length == 0 ? "" : "\n") + $"(Deine vorige Antwort hat Frank nicht gehört, weil er weitersprach: „{ungehoert}“. Beziehe sie ein, soweit sie noch passt, vor allem Bestätigungen erledigter Aktionen.)";
        }
        unterbrochenFlag = false;
        ungehoert = null;
        laufend = new Uebergabe(kennung, jetzt);
        return new Aktion.AnJarvis(kennung, worte, hinweis);
    }

    private List<Aktion> Abgeben(string antwort, long jetzt)
    {
        var aus = SignalVerwerfen("Antwort von Jarvis ist da", verbraucht: false);
        signalGenutzt = false;
        letzteAntwort = antwort;
        echos = 0;
        nutzerSeitAbgabe = false;
        tonSeitOffen = false;
        abgabeMs = jetzt;
        if (offeneAufrufe.Count > 0)
        {
            var ids = offeneAufrufe.ToList();
            offeneAufrufe.Clear();
            tor = TorArt.Offen;
            aus.Add(new Aktion.Werkzeugantwort(ids, antwort));
            return aus;
        }
        if (erzeugt) { tor = TorArt.Wartet; wartetSeitMs = jetzt; } else tor = TorArt.Offen;
        aus.Add(new Aktion.Einspielen(Marke + antwort));
        return aus;
    }

    private static readonly Regex WortMuster = new(@"alles\s+klar|\p{L}+", RegexOptions.Compiled);
    private static readonly HashSet<char> Satzzeichen = new(".,!?…-–—'’\"„“‚‘");

    public static bool NeutralesSignal(string text)
    {
        var t = text.ToLowerInvariant();
        if (t.Any(c => !char.IsLetter(c) && !char.IsWhiteSpace(c) && !Satzzeichen.Contains(c))) return false;
        var worte = WortMuster.Matches(t).Select(m => m.Value).ToList();
        return worte.Count > 0 && worte.Count <= SignalMaxWorte && worte.All(w =>
            SignalWorte.Contains(w) || w.Split((char[]?)null, StringSplitOptions.RemoveEmptyEntries).SequenceEqual(new[] { "alles", "klar" }));
    }

    private static string SignalSchluessel(string text) => new(text.ToLowerInvariant().Where(char.IsLetter).ToArray());
}
