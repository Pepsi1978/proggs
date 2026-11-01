using System.Net.WebSockets;
using System.Text;
using System.Threading.Channels;

namespace JarvisWindows;

/// <summary>Rückmeldungen einer Live-Sitzung an das Live-Gespräch. Kommen aus dem Socket-Faden.</summary>
public interface LiveSitzungsDelegate
{
    void LiveBereit();
    void LiveNutzerText(string text);
    void LiveJarvisText(string text);
    void LiveWerkzeug(string kennung, string auftrag, string hinweis);
    void LiveFehler(string text);
    void LiveEnde(string? grund);
}

/// <summary>Gemeinsame Schnittstelle der Anbieter (GPT-Live, Realtime, Gemini Live).</summary>
public class LiveSitzung
{
    public LiveTon Ton { get; }
    public LiveSitzungsDelegate? Delegate { get; set; }
    private volatile bool bereit;
    /// <summary>Höchstens so viel Mikrofonton wird aufgehoben, solange die Verbindung nicht steht (Start, Gemini-Wechsel).</summary>
    private const double VorlaufSekunden = 5;
    private readonly Queue<byte[]> vorlauf = new();
    private long vorlaufBytes;
    public bool Bereit { get => bereit; set => bereit = value; }
    public virtual bool IstGemini => false;

    public LiveSitzung(LiveTon ton) { Ton = ton; }

    public virtual void Starten() { }
    public virtual void Beenden() { }
    public virtual void Mikro(byte[] pcm) { }
    /// <summary>Sendet den Block oder hebt ihn auf: Frank spricht oft schon beim Öffnen, und beim Verbindungswechsel fielen sonst Worte weg.
    /// Eigene Sperre samt Senden: Der Rest geht immer vor dem neuen Block raus. Senden blockiert nie, das Mikrofon läuft ungestört weiter.</summary>
    protected void SendeOderHebeAuf(byte[] pcm, bool frei, Action<byte[]> senden)
    {
        lock (vorlauf)
        {
            if (!frei)
            {
                vorlauf.Enqueue(pcm);
                vorlaufBytes += pcm.Length;
                var grenze = (long)(Ton.EingabeRate * 2 * VorlaufSekunden);
                while (vorlaufBytes > grenze) vorlaufBytes -= vorlauf.Dequeue().Length;
                return;
            }
            while (vorlauf.Count > 0) senden(vorlauf.Dequeue());
            vorlaufBytes = 0;
            senden(pcm);
        }
    }
    /// <summary>Kann die Sitzung Jarvis' Antwort schon Satz für Satz sprechen, während das Gehirn noch schreibt?</summary>
    public virtual bool KannStroemen => false;
    /// <summary>Ein Satz der laufenden Antwort. false = nicht angekommen.</summary>
    public virtual bool Stueck(string kennung, string text) => false;
    /// <summary>Jarvis' Antwort auf einen Auftrag zurück ans Sprachmodell. false = nicht angekommen.</summary>
    public virtual bool Ergebnis(string kennung, string text) => false;
    /// <summary>Spätes Ergebnis nach einem Zwischenstand jetzt sprechen lassen.</summary>
    public virtual bool Nachreichen(string kennung, string text) => false;
    /// <summary>Regelmäßig (alle 50 ms) aus dem eigenen Takt-Faden des Live-Gesprächs: Zeitgeber der Sitzung. Läuft nicht auf dem Hauptfaden.</summary>
    public virtual void Takt() { }
    /// <summary>Frank tippt zum Unterbrechen.</summary>
    public virtual void Angetippt() => Ton.Leeren();

    /// <summary>Entspricht DispatchQueue.global().asyncAfter.</summary>
    protected static void Nach(double sekunden, Action a) => _ = Task.Run(async () =>
    {
        await Task.Delay(TimeSpan.FromSeconds(sekunden));
        try { a(); } catch (Exception ex) { Log("fehler", "Live: verzögerter Schritt fehlgeschlagen", fehler: ex.Message); }
    });

    protected static byte[]? Base64(string? s)
    {
        if (s == null) return null;
        try { return Convert.FromBase64String(s); } catch { return null; }
    }
}

/// <summary>WebSocket mit Dauerverbindung (Ping alle 20 s), eigener Faden mit hoher Priorität (Live vor Hintergrundarbeit).
/// Wie die OperationQueue der Mac-Fassung läuft jede Rückmeldung aller Sockets nacheinander, nie zwei gleichzeitig.</summary>
public sealed class LiveSocket
{
    private const int MaxNachricht = 32 * 1024 * 1024;
    private static readonly object Warteschlange = new();

    private readonly ClientWebSocket ws = new();
    private readonly Uri adresse;
    private readonly Action beiOffen;
    private readonly Action<string> beiText;
    private readonly Action<int, string> beiZu;
    private readonly CancellationTokenSource abbruch = new();
    /// <summary>null = nächster Tonblock aus tonSchlange. So behält Steuerung ihren Platz, und nur Ton kann bei gestörtem Netz wegfallen.</summary>
    private readonly Channel<byte[]?> ausgang = Channel.CreateUnbounded<byte[]?>(new UnboundedChannelOptions { SingleReader = true });
    private readonly Queue<byte[]> tonSchlange = new();
    private bool tonVerworfenGemeldet;
    /// <summary>So viele Tonblöcke warten höchstens: Bei gestörtem Netz soll der Dienst das Jetzt hören, nicht alles von vor Minuten.</summary>
    public int TonGrenze { get; set; } = 20;
    private readonly object sperre = new();
    private bool zu;
    private bool verbunden;

    public LiveSocket(string adresse, IReadOnlyDictionary<string, string>? kopf, Action beiOffen, Action<string> beiText, Action<int, string> beiZu)
    {
        this.adresse = new Uri(adresse);
        this.beiOffen = beiOffen; this.beiText = beiText; this.beiZu = beiZu;
        ws.Options.KeepAliveInterval = TimeSpan.FromSeconds(20);
        ws.Options.KeepAliveTimeout = TimeSpan.FromSeconds(20);
        ws.Options.CollectHttpResponseDetails = true;
        ws.Options.SetRequestHeader("User-Agent", $"JarvisWindows/{AppInfo.Version}");
        if (kopf != null) foreach (var (k, v) in kopf) ws.Options.SetRequestHeader(k, v);
    }

    /// <summary>Getrennt vom Konstruktor: Der Aufrufer legt den Socket erst ab, dann kommen Rückmeldungen.</summary>
    public void Los()
    {
        new Thread(Lauf) { IsBackground = true, Priority = ThreadPriority.AboveNormal, Name = "jarvis.live.socket" }.Start();
    }

    private static void Nacheinander(Action a)
    {
        lock (Warteschlange)
        {
            try { a(); } catch (Exception ex) { Log("fehler", "Live: Rückmeldung des Sockets fehlgeschlagen", fehler: ex.Message); }
        }
    }

    private void Lauf()
    {
        try
        {
            using (var zeit = CancellationTokenSource.CreateLinkedTokenSource(abbruch.Token))
            {
                zeit.CancelAfter(TimeSpan.FromSeconds(30));
                // Über den geteilten Weg mit IPv4 zuerst: Totes IPv6 ließe Live sonst lange bei „Verbindet“ hängen.
                ws.ConnectAsync(adresse, SprachHttp.Invoker, zeit.Token).GetAwaiter().GetResult();
            }
        }
        catch (Exception ex)
        {
            Schliessen(ws.HttpStatusCode != 0 ? (int)ws.HttpStatusCode : -1, ex.Message);
            return;
        }
        lock (sperre) verbunden = true;
        if (Offen) Nacheinander(beiOffen);
        _ = Task.Run(Schreiben);
        Empfangen();
    }

    private void Empfangen()
    {
        var puffer = new byte[64 * 1024];
        using var nachricht = new MemoryStream();
        try
        {
            while (Offen)
            {
                var r = ws.ReceiveAsync(new ArraySegment<byte>(puffer), abbruch.Token).GetAwaiter().GetResult();
                if (r.MessageType == WebSocketMessageType.Close)
                {
                    Schliessen(ws.CloseStatus is { } c ? (int)c : 1006, ws.CloseStatusDescription ?? "");
                    return;
                }
                nachricht.Write(puffer, 0, r.Count);
                if (nachricht.Length > MaxNachricht)
                {
                    Schliessen(1009, "Nachricht zu groß");
                    return;
                }
                if (!r.EndOfMessage) continue;
                var text = Encoding.UTF8.GetString(nachricht.GetBuffer(), 0, (int)nachricht.Length);
                nachricht.SetLength(0);
                if (!Offen) return;
                Nacheinander(() => beiText(text));
            }
        }
        catch (Exception ex)
        {
            var code = ws.CloseStatus is { } c ? (int)c : ws.HttpStatusCode != 0 ? (int)ws.HttpStatusCode : -1;
            Schliessen(code, ws.CloseStatusDescription ?? ex.Message);
        }
    }

    /// <summary>ClientWebSocket erlaubt nur ein Senden zur Zeit, und der Aufrufer (Mikrofon-Faden) darf nie warten: Alles geht durch eine Warteschlange.</summary>
    private async Task Schreiben()
    {
        try
        {
            await foreach (var eintrag in ausgang.Reader.ReadAllAsync(abbruch.Token))
            {
                var d = eintrag;
                if (d == null) lock (tonSchlange) tonSchlange.TryDequeue(out d);
                if (d != null) await ws.SendAsync(new ArraySegment<byte>(d), WebSocketMessageType.Text, true, abbruch.Token);
            }
            // Von Trennen beendet: sauberer Abschluss.
            await ws.CloseOutputAsync(WebSocketCloseStatus.NormalClosure, "", abbruch.Token);
        }
        catch { }   // Der Empfang bemerkt die tote Verbindung und meldet sie.
    }

    private void Schliessen(int code, string grund)
    {
        lock (sperre)
        {
            if (zu) return;
            zu = true;
        }
        ausgang.Writer.TryComplete();
        // Fehlertexte können die Adresse mit dem Schlüssel enthalten (Gemini: ?key=…).
        var sauber = Geheimnisse.Schwaerzen(grund);
        Nacheinander(() => beiZu(code, sauber));
        Aufraeumen();
    }

    private void Aufraeumen()
    {
        try { abbruch.Cancel(); } catch { }
        try { ws.Abort(); ws.Dispose(); } catch { }
    }

    public bool Offen { get { lock (sperre) return !zu; } }

    public bool Sende(JSONObjekt objekt)
    {
        if (!Offen) return false;
        return ausgang.Writer.TryWrite(JSON.Daten(objekt));
    }

    /// <summary>Mikrofonton: Staut er sich, fällt der älteste Block weg.</summary>
    public bool SendeTon(JSONObjekt objekt)
    {
        if (!Offen) return false;
        var d = JSON.Daten(objekt);
        lock (tonSchlange)
        {
            tonSchlange.Enqueue(d);
            if (tonSchlange.Count > TonGrenze)
            {
                tonSchlange.Dequeue();   // Der Platzhalter im Ausgang gehört jetzt dem neuen Block.
                if (!tonVerworfenGemeldet)
                {
                    tonVerworfenGemeldet = true;
                    Log("live", "Netz zu langsam: alter Mikrofonton wird verworfen");
                }
                return true;
            }
        }
        return ausgang.Writer.TryWrite(null);
    }

    /// <summary>Von uns beendet: kein Fehler mehr an den Aufrufer.</summary>
    public void Trennen()
    {
        bool warVerbunden;
        lock (sperre)
        {
            if (zu) return;
            zu = true;
            warVerbunden = verbunden;
        }
        ausgang.Writer.TryComplete();
        if (!warVerbunden) { Aufraeumen(); return; }
        // Kurz Zeit für den Schlussrahmen lassen, dann hart zu.
        _ = Task.Delay(1000).ContinueWith(_ => Aufraeumen(), TaskScheduler.Default);
    }
}
