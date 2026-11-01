using System.Collections.Concurrent;
using System.Reflection;
using System.Runtime.InteropServices;
using Microsoft.Win32;
using NAudio.CoreAudioApi;
using NAudio.CoreAudioApi.Interfaces;
using NAudio.Dsp;
using NAudio.Wave;
using NAudio.Wave.SampleProviders;

namespace JarvisWindows;

/// <summary>Ton des Live-Gesprächs unter Windows: Mikrofon über WASAPI in der Kategorie Communications mit dem Echo-Effekt des Geräts, wenn es einen hat,
/// und lückenlose Wiedergabe der 24-kHz-Antworten. Werte für Polster, Kappen und Hörbar-Schwellen aus der Handy-App
/// (best-practices/apis/openai-live-realtime.md §14.3): Polster 250 ms nur hinter Stille, Kappen überschüssiger Stille
/// erst in Pausen ab 600 ms, hörbar ab Effektivwert 120, „ganz still“ unter Spitze 200.
/// Geräteauf- und -abbau laufen nur im eigenen Ton-Faden (MTA, COM); Wiedergabe und Mikrofon haben die Fäden von NAudio.</summary>
public sealed class LiveTon
{
    public const double AusgabeRate = 24_000;
    public const int StartPolsterMs = 250;
    public const long NeuerSatzMs = 300;
    public const long KappenPauseMs = 600;
    public const int KappenUeberMs = 30;
    public const int StilleSpitze = 200;
    public const double HoerbarEffektiv = 120;
    /// <summary>Mikrofonblock: OpenAI verlangt gleichmäßig 100 ms (§11), Gemini bekommt 40 ms für weniger Verzögerung beim Hinhören.</summary>
    public int BlockMs { get; }
    /// <summary>Echosperre: Das Mikrofon bleibt nach dem Ende der Wiedergabe noch so lange zu (Puffer, Hall), §14.3.</summary>
    public const int EchoNachlaufMs = 400;
    /// <summary>Über Bluetooth liegt die Funkstrecke zwischen Puffer und Lautsprecher, §14.5 (Phantom-Mitschrift).</summary>
    public const int BluetoothMinLatenzMs = 1000;
    private const int AusgangLatenzWunschMs = 40;

    public double EingabeRate { get; }
    /// <summary>PCM16 mono in EingabeRate, Blöcke zu BlockMs. Läuft im Mikrofon-Faden.</summary>
    public Action<byte[]>? BeiMikro { get; set; }
    /// <summary>Mikrofon stumm (Stille statt Mikrofon an den Dienst), z. B. während eines späten Vorlesens.</summary>
    public bool MikroStumm { get { lock (schloss) return mikroStumm; } set { lock (schloss) mikroStumm = value; } }
    /// <summary>Der Ton ist nach einem Gerätewechsel endgültig ausgefallen. Läuft im Ton-Faden.</summary>
    public Action<string>? BeiAusfall { get; set; }

    private volatile bool ohneEchoUnterdrueckung;
    /// <summary>Ohne bestätigte Echounterdrückung des Geräts ist das Mikrofon stumm, solange Jarvis hörbar spricht (Echosperre wie am Handy).</summary>
    public bool OhneEchoUnterdrueckung => ohneEchoUnterdrueckung;

    private sealed class Stueck
    {
        public float[] Daten = Array.Empty<float>();
        public float Pegel;
    }

    private readonly object schloss = new();
    private readonly Queue<Stueck> warteschlange = new();
    private int stueckPos;
    private int gepuffertFrames;
    private int latenzMs = AusgangLatenzWunschMs;
    private long letzteHoerbarMs;
    private long hoerbarBisMs;
    private long sperreBisMs;
    private bool letztesStueckStill = true;
    private float pegelEinWert;
    private float pegelAusWert;
    private bool mikroStumm;
    private volatile bool laeuft;
    private volatile bool gestoppt;
    /// <summary>Mikrofon und Lautsprecher laufen.</summary>
    public bool Laeuft => laeuft;

    private readonly TonFaden faden = new();
    private MMDeviceEnumerator? geraete;
    private GeraeteWaechter? waechter;
    private WasapiOut? ausgang;
    private WasapiCapture? eingang;
    private MMDevice? ausgangGeraet;
    private MMDevice? eingangGeraet;
    private readonly Role eingangRolle = Role.Communications;   // auch im Rückfall dasselbe Mikrofon, nur ohne die Kategorie
    private bool sprachverarbeitung;
    private volatile bool stromAusgefallen;
    private int neustartMarke;
    private bool ausgangBluetooth;

    // Nur im Mikrofon-Faden, zwischen zwei Starts frisch.
    private WdlResampler? wandler;
    private byte[] sammel = Array.Empty<byte>();
    private int sammelLaenge;
    private float[] monoPuffer = Array.Empty<float>();
    private float[] wandelPuffer = Array.Empty<float>();
    private WaveFormat? eingangFormat;
    private bool eingangFloat;
    private bool mikroFehlerGemeldet;
    private readonly ManualResetEventSlim erstesPaket = new(false);

    public LiveTon(double eingabeRate, int blockMs = 100) { EingabeRate = eingabeRate; BlockMs = blockMs; }

    public static long JetztMs() => DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

    public float PegelEin { get { lock (schloss) return pegelEinWert; } }
    public float PegelAus { get { lock (schloss) return gepuffertFrames == 0 ? 0 : pegelAusWert; } }
    /// <summary>Hörbarer Ton liegt noch im Puffer oder wird gerade gespielt.</summary>
    public bool SprichtHoerbar { get { lock (schloss) return JetztMs() < hoerbarBisMs; } }
    /// <summary>Noch nicht abgespielte Frames (24 kHz), samt dem, was im Gerätepuffer liegt.</summary>
    public int Gepuffert
    {
        get { lock (schloss) return gepuffertFrames == 0 ? 0 : gepuffertFrames + latenzMs * (int)AusgabeRate / 1000; }
    }

    /// <summary>Darf Windows diese App ans Mikrofon lassen? (Einstellungen → Datenschutz und Sicherheit → Mikrofon.) Gegenstück zur Mikrofon-Abfrage der Mac-Fassung.</summary>
    public static bool MikrofonErlaubt()
    {
        const string zweig = @"SOFTWARE\Microsoft\Windows\CurrentVersion\CapabilityAccessManager\ConsentStore\microphone";
        static bool Verboten(RegistryKey wurzel, string pfad)
        {
            try
            {
                using var k = wurzel.OpenSubKey(pfad);
                return string.Equals(k?.GetValue("Value") as string, "Deny", StringComparison.OrdinalIgnoreCase);
            }
            catch { return false; }
        }
        return !Verboten(Registry.LocalMachine, zweig) && !Verboten(Registry.CurrentUser, zweig) && !Verboten(Registry.CurrentUser, zweig + @"\NonPackaged");
    }

    // MARK: Start / Stopp

    /// <summary>Startet den Ton. Die Kategorie Communications kann scheitern, etwa wenn das Weckwort das Mikrofon gerade erst freigibt oder der Treiber sie nicht mag.
    /// Deshalb wie am Mac: zweiter Versuch nach 400 ms, danach ohne sie mit Echosperre. Kommt trotz Start kein Tonlauf zustande, gleich ohne weiter.
    /// Scheitert auch das, kommt die Ausnahme beim Aufrufer an; es stürzt nichts ab, wenn kein Gerät da ist.</summary>
    public Task Starten()
    {
        var fertig = new TaskCompletionSource(TaskCreationOptions.RunContinuationsAsynchronously);
        faden.Post(() =>
        {
            try { StartSchleife(); fertig.TrySetResult(); }
            catch (Exception ex) { fertig.TrySetException(ex); }
        });
        return fertig.Task;
    }

    private void StartSchleife()
    {
        Exception? letzterFehler = null;
        var versuch = 0;
        while (versuch < 3)
        {
            if (versuch > 0)
            {
                TonAbbauen();
                Thread.Sleep(400);   // Nur der Ton-Faden wartet, nie der Hauptfaden.
            }
            try
            {
                Hochfahren(sprachverarbeitung: versuch < 2);
                laeuft = true;
                SperreAusPuffer();
                WaechterSetzen();
                if (versuch > 0) Log("live", $"Ton im {versuch + 1}. Versuch gestartet{(versuch == 2 ? " – ohne Echounterdrückung, Echosperre aktiv" : "")}");
                return;
            }
            catch (UnauthorizedAccessException) { TonAbbauen(); throw; }   // Windows verbietet das Mikrofon: Ein neuer Versuch ändert daran nichts.
            catch (Exception ex)
            {
                letzterFehler = ex;
                Log("fehler", $"Live: Tonstart Versuch {versuch + 1} fehlgeschlagen: {ex.Message}");
                // Kein Tonlauf: Die Kategorie ist das Problem, also gleich zum Versuch ohne sie, nie darüber hinaus.
                versuch = ex is TonlaufFehler ? Math.Max(versuch + 1, 2) : versuch + 1;
            }
        }
        TonAbbauen();   // Ein Teil kann trotz Fehler laufen, das Mikrofon darf nicht offen bleiben.
        throw letzterFehler ?? new InvalidOperationException("Ton lässt sich nicht starten.");
    }

    private sealed class TonlaufFehler : Exception { public TonlaufFehler(string t) : base(t) { } }

    private void Hochfahren(bool sprachverarbeitung)
    {
        geraete ??= new MMDeviceEnumerator();
        stromAusgefallen = false;
        this.sprachverarbeitung = sprachverarbeitung;
        ohneEchoUnterdrueckung = true;   // Bis der Effekt bestätigt ist: Die Begrüßung kann schon laufen, ihr Echo darf nicht ins Mikrofon.
        AusgangStarten();
        var echoAn = EingangStarten(sprachverarbeitung);
        ohneEchoUnterdrueckung = !echoAn;
        Log("live", echoAn ? "Echounterdrückung des Geräts aktiv" : "Keine Echounterdrückung des Geräts bestätigt, Echosperre aktiv");
    }

    /// <summary>Wiedergabe in der Kategorie Medien, nicht Communications: kein Telefonklang (§14.2 Nr. 1).</summary>
    private void AusgangStarten()
    {
        if (!geraete!.HasDefaultAudioEndpoint(DataFlow.Render, Role.Multimedia)) throw new InvalidOperationException("Kein Lautsprecher verfügbar.");
        var d = geraete.GetDefaultAudioEndpoint(DataFlow.Render, Role.Multimedia);
        ausgangGeraet = d;
        ausgangBluetooth = IstBluetooth(d);
        WaveFormat mix;
        using (var ac = d.AudioClient) mix = ac.MixFormat;
        ISampleProvider quelle = new TonQuelle(this);
        if (mix.SampleRate != (int)AusgabeRate) quelle = new WdlResamplingSampleProvider(quelle, mix.SampleRate);
        var verteiler = new KanalVerteiler(quelle, mix.Channels);
        var w = new WasapiOut(d, AudioClientShareMode.Shared, true, AusgangLatenzWunschMs);
        w.PlaybackStopped += (s, e) =>
        {
            if (!ReferenceEquals(s, ausgang)) return;
            Log("live", "Wiedergabe gestoppt: " + (e.Exception?.Message ?? "ohne Fehler"));
            stromAusgefallen = true;
            faden.Post(NeustartAnfordern);
        };
        DuckingAus(d);
        w.Init(new SampleToWaveProvider(verteiler));
        ausgang = w;
        var client = PrivatesFeld<AudioClient>(w, "audioClient");
        if (client != null)
        {
            try { var ms = (int)(client.StreamLatency / 10_000); lock (schloss) latenzMs = ms > 0 ? ms : AusgangLatenzWunschMs; } catch { }
        }
        // 250 ms Stille vorlegen, damit der Ausgang nicht leer startet (§14.2 Nr. 7). Socket und Ton starten parallel:
        // Liegt schon Ton der Begrüßung bereit, käme die Stille sonst hinter dessen Anfang.
        bool leer;
        lock (schloss) leer = gepuffertFrames == 0;
        if (leer) PlaneStille(StartPolsterMs);
        w.Play();
        SperreAusPuffer();
    }

    /// <summary>Gibt zurück, ob das Gerät wirklich Echounterdrückung macht.</summary>
    private bool EingangStarten(bool sprachverarbeitung)
    {
        if (!geraete!.HasDefaultAudioEndpoint(DataFlow.Capture, eingangRolle)) throw new InvalidOperationException("Kein Mikrofon verfügbar.");
        var d = geraete.GetDefaultAudioEndpoint(DataFlow.Capture, eingangRolle);
        eingangGeraet = d;
        var c = new WasapiCapture(d, true, 100);
        var f = c.WaveFormat;
        var sub = (f as WaveFormatExtensible)?.SubFormat;
        var istFloat = f.Encoding == WaveFormatEncoding.IeeeFloat || sub == new Guid("00000003-0000-0010-8000-00aa00389b71");
        var istPcm = f.Encoding == WaveFormatEncoding.Pcm || sub == new Guid("00000001-0000-0010-8000-00aa00389b71");
        if (f.SampleRate <= 0 || f.Channels <= 0 || !((istFloat && f.BitsPerSample == 32) || (istPcm && f.BitsPerSample is 16 or 24 or 32)))
        {
            c.Dispose();
            throw new InvalidOperationException($"Mikrofonformat wird nicht unterstützt ({f}).");
        }
        eingangFormat = f;
        eingangFloat = istFloat;
        wandler = new WdlResampler();
        wandler.SetMode(true, 2, false);
        wandler.SetFilterParms();
        wandler.SetFeedMode(true);
        wandler.SetRates(f.SampleRate, EingabeRate);
        sammelLaenge = 0;
        mikroFehlerGemeldet = false;
        erstesPaket.Reset();

        var client = PrivatesFeld<AudioClient>(c, "audioClient");
        // Vor dem Initialisieren, das erst StartRecording macht. Options bleibt None: Raw würde gerade die Echounterdrückung abschalten.
        if (sprachverarbeitung) KategorieSetzen(client ?? throw new InvalidOperationException("NAudio-Aufbau unbekannt: audioClient fehlt."), AudioStreamCategory.Communications);

        c.DataAvailable += MikroDaten;
        c.RecordingStopped += (s, e) =>
        {
            if (!ReferenceEquals(s, eingang)) return;
            Log("live", "Mikrofon gestoppt: " + (e.Exception?.Message ?? "ohne Fehler"));
            stromAusgefallen = true;
            faden.Post(NeustartAnfordern);
        };
        eingang = c;
        c.StartRecording();
        var echoAn = sprachverarbeitung && client != null && EchoEinschalten(client, ausgangGeraet?.ID);
        // Kein Paket trotz Start: Der Tonlauf steht, ein zweiter Versuch mit derselben Kategorie hilft nicht (wie „player did not see an IO cycle“ am Mac).
        if (!erstesPaket.Wait(1500)) throw new TonlaufFehler("Mikrofon liefert keinen Ton.");
        return echoAn;
    }

    private void TonAbbauen()
    {
        var w = ausgang; var c = eingang;
        ausgang = null; eingang = null;   // zuerst: Stopp-Meldungen dieser Geräte gelten nicht mehr
        if (c != null) { try { c.DataAvailable -= MikroDaten; c.Dispose(); } catch (Exception ex) { Log("live", "Mikrofon-Abbau: " + ex.Message); } }
        if (w != null) { try { w.Dispose(); } catch (Exception ex) { Log("live", "Wiedergabe-Abbau: " + ex.Message); } }
        try { eingangGeraet?.Dispose(); } catch { }
        try { ausgangGeraet?.Dispose(); } catch { }
        eingangGeraet = null; ausgangGeraet = null;
    }

    private void PufferZuruecksetzen()
    {
        lock (schloss)
        {
            warteschlange.Clear(); stueckPos = 0; gepuffertFrames = 0;
            hoerbarBisMs = 0; sperreBisMs = 0; letzteHoerbarMs = 0; pegelAusWert = 0; letztesStueckStill = true;
        }
    }

    private void WaechterSetzen()
    {
        if (waechter != null) return;
        try
        {
            waechter = new GeraeteWaechter(this);
            geraete!.RegisterEndpointNotificationCallback(waechter);
        }
        catch (Exception ex) { waechter = null; Log("live", "Gerätewechsel nicht beobachtbar: " + ex.Message); }
    }

    private void GeraetGeaendert(DataFlow? fluss, Role? rolle)
    {
        if (fluss == DataFlow.Render && rolle != Role.Multimedia) return;
        if (fluss == DataFlow.Capture && rolle != eingangRolle) return;
        faden.Post(NeustartAnfordern);
    }

    /// <summary>Geräte melden Änderungen oft mehrfach kurz hintereinander: Erst nach 250 ms Ruhe neu verbinden.</summary>
    private void NeustartAnfordern()
    {
        neustartMarke += 1;
        var marke = neustartMarke;
        faden.Nach(250, () => { if (marke == neustartMarke) NeuStarten(); });
    }

    private bool StandardGeaendert()
    {
        try
        {
            if (geraete == null || ausgangGeraet == null || eingangGeraet == null) return true;
            if (!geraete.HasDefaultAudioEndpoint(DataFlow.Render, Role.Multimedia) || !geraete.HasDefaultAudioEndpoint(DataFlow.Capture, eingangRolle)) return true;
            using var a = geraete.GetDefaultAudioEndpoint(DataFlow.Render, Role.Multimedia);
            using var e = geraete.GetDefaultAudioEndpoint(DataFlow.Capture, eingangRolle);
            return a.ID != ausgangGeraet.ID || e.ID != eingangGeraet.ID;
        }
        catch { return true; }
    }

    private void NeuStarten()
    {
        // Läuft alles und blieb das Standardgerät gleich, war es eine Änderung, die uns nicht betrifft: sonst endlose Neustarts.
        if (!laeuft || (!stromAusgefallen && !StandardGeaendert())) return;
        Log("live", "Audiogerät gewechselt, Ton wird neu verbunden");
        PufferZuruecksetzen();
        TonAbbauen();
        try
        {
            Hochfahren(sprachverarbeitung);
            return;
        }
        catch (Exception ex)
        {
            Log("fehler", $"Live: Ton nach Gerätewechsel nicht wieder gestartet: {ex.Message}");
        }
        TonAbbauen();
        PufferZuruecksetzen();
        try
        {
            Hochfahren(false);
            Log("live", "Ton nach Gerätewechsel ohne Echounterdrückung gestartet, Echosperre aktiv");
        }
        catch (Exception ex)
        {
            Log("fehler", $"Live: Ton auch ohne Echounterdrückung nicht wieder gestartet: {ex.Message}");
            TonAbbauen();
            laeuft = false;
            BeiAusfall?.Invoke($"Mikrofon oder Lautsprecher lässt sich nach dem Gerätewechsel nicht starten: {ex.Message}");
        }
    }

    /// <summary>Blockiert den Aufrufer nie: Der Ton-Faden räumt auf.</summary>
    public void Stoppen()
    {
        gestoppt = true;
        laeuft = false;
        PufferZuruecksetzen();
        faden.Post(() =>
        {
            Abbauen();
            faden.Ende();
        });
    }

    private void Abbauen()
    {
        laeuft = false;
        if (waechter != null)
        {
            try { geraete?.UnregisterEndpointNotificationCallback(waechter); } catch { }
            waechter = null;
        }
        PufferZuruecksetzen();
        TonAbbauen();
        BeiMikro = null;   // erst nach dem Entfernen des Abgriffs, der Mikrofon-Faden liest sie ohne Sperre
        BeiAusfall = null;
        try { geraete?.Dispose(); } catch { }
        geraete = null;
    }

    // MARK: Mikrofon

    private void MikroDaten(object? sender, WaveInEventArgs e)
    {
        if (!ReferenceEquals(sender, eingang) && eingang != null) return;
        var f = eingangFormat;
        var w = wandler;
        if (f == null || w == null) return;
        if (!erstesPaket.IsSet)
        {
            try { Thread.CurrentThread.Priority = ThreadPriority.Highest; } catch { }
            erstesPaket.Set();
        }
        if (e.BytesRecorded == 0) return;
        try
        {
            var frames = e.BytesRecorded / f.BlockAlign;
            if (monoPuffer.Length < frames) monoPuffer = new float[frames];
            // Nur den ersten Kanal: Sprachverarbeitung liefert mehrere gleiche Kanäle, ein Mischen brächte nichts.
            for (var i = 0; i < frames; i++) monoPuffer[i] = ErsteProbe(e.Buffer, i * f.BlockAlign, f.BitsPerSample);
            var n = w.ResamplePrepare(frames, 1, out var ein, out var einPos);
            Array.Copy(monoPuffer, 0, ein, einPos, Math.Min(n, frames));
            var kapazitaet = (int)(frames * EingabeRate / f.SampleRate) + 32;
            if (wandelPuffer.Length < kapazitaet) wandelPuffer = new float[kapazitaet];
            var aus = w.ResampleOut(wandelPuffer, 0, Math.Min(n, frames), kapazitaet, 1);
            if (aus <= 0) return;

            double summe = 0;
            var daten = new byte[aus * 2];
            for (var i = 0; i < aus; i++)
            {
                var s = (short)Math.Clamp((int)(wandelPuffer[i] * 32767f), short.MinValue, short.MaxValue);
                summe += (double)s * s;
                daten[2 * i] = (byte)s;
                daten[2 * i + 1] = (byte)(s >> 8);
            }
            var rms = Math.Sqrt(summe / aus);
            bool stumm;
            lock (schloss)
            {
                pegelEinWert = (float)Math.Min(1, rms / 6000);
                stumm = mikroStumm || (ohneEchoUnterdrueckung && JetztMs() < sperreBisMs);
            }
            if (stumm) Array.Clear(daten);
            Sammeln(daten);
        }
        catch (Exception ex)
        {
            if (!mikroFehlerGemeldet)
            {
                mikroFehlerGemeldet = true;
                Log("fehler", $"Live: Mikrofonton nicht wandelbar: {ex.Message}");
            }
        }
    }

    private float ErsteProbe(byte[] b, int pos, int bits)
    {
        if (eingangFloat) return BitConverter.ToSingle(b, pos);
        return bits switch
        {
            16 => BitConverter.ToInt16(b, pos) / 32768f,
            24 => ((b[pos + 2] << 24) | (b[pos + 1] << 16) | (b[pos] << 8)) / 2147483648f,
            _ => BitConverter.ToInt32(b, pos) / 2147483648f,
        };
    }

    private void Sammeln(byte[] daten)
    {
        var blockBytes = (int)EingabeRate * BlockMs / 1000 * 2;
        if (sammel.Length < sammelLaenge + daten.Length) Array.Resize(ref sammel, Math.Max(sammelLaenge + daten.Length, blockBytes * 2));
        Buffer.BlockCopy(daten, 0, sammel, sammelLaenge, daten.Length);
        sammelLaenge += daten.Length;
        var pos = 0;
        while (sammelLaenge - pos >= blockBytes)
        {
            var block = new byte[blockBytes];
            Buffer.BlockCopy(sammel, pos, block, 0, blockBytes);
            pos += blockBytes;
            BeiMikro?.Invoke(block);
        }
        if (pos > 0)
        {
            Buffer.BlockCopy(sammel, pos, sammel, 0, sammelLaenge - pos);
            sammelLaenge -= pos;
        }
    }

    // MARK: Wiedergabe

    /// <summary>Ein Stück Antwort (PCM16 mono 24 kHz). kappenErlaubt: GPT-Live liefert auch in Pausen Stille in Echtzeit; die darf
    /// gekappt werden, wenn sich ein Polster aufgestaut hat.</summary>
    public void Spiele(byte[] pcm, bool kappenErlaubt = true)
    {
        // Auch vor dem Start annehmen: Socket und Ton starten parallel, die Begrüßung darf dabei nicht verloren gehen.
        if (gestoppt || pcm.Length < 2) return;
        var s = MemoryMarshal.Cast<byte, short>(pcm);
        var frames = s.Length;
        double summe = 0;
        var spitze = 0;
        var proben = new float[frames];
        for (var i = 0; i < frames; i++)
        {
            int v = s[i];
            var a = Math.Abs(v);
            if (a > spitze) spitze = a;
            summe += (double)v * v;
            proben[i] = v / 32768f;
        }
        var rms = Math.Sqrt(summe / frames);
        var hoerbar = rms >= HoerbarEffektiv;
        var still = spitze < StilleSpitze;
        var jetzt = JetztMs();

        int polster;
        lock (schloss)
        {
            var gepuffertMs = gepuffertFrames * 1000 / (int)AusgabeRate;
            var vorherStill = letztesStueckStill;
            letztesStueckStill = still;
            if (kappenErlaubt && !hoerbar && still && gepuffertMs > StartPolsterMs + KappenUeberMs && jetzt - letzteHoerbarMs > KappenPauseMs) return;
            // Nur hinter Stille (§14.3): Sonst risse die Polster-Stille einen leisen Wortansatz auf.
            polster = hoerbar && vorherStill && jetzt - letzteHoerbarMs >= NeuerSatzMs && gepuffertMs < StartPolsterMs - 40 ? StartPolsterMs - gepuffertMs : 0;
        }
        if (polster > 0) PlaneStille(polster);
        Plane(proben, (float)Math.Min(1, rms / 5000));
        if (hoerbar)
        {
            lock (schloss)
            {
                letzteHoerbarMs = jetzt;
                var restMs = gepuffertFrames * 1000 / (int)AusgabeRate + latenzMs;
                hoerbarBisMs = jetzt + restMs + 150;
                var nachlauf = EchoNachlaufMs + (ausgangBluetooth ? Math.Max(BluetoothMinLatenzMs, latenzMs) + BlockMs : 0);
                sperreBisMs = jetzt + restMs + nachlauf;
            }
        }
    }

    /// <summary>Ton, der vor dem Gerätestart ankam, läuft erst jetzt los: Sprechanzeige und Echosperre ab jetzt rechnen.</summary>
    private void SperreAusPuffer()
    {
        lock (schloss)
        {
            if (gepuffertFrames == 0 || letzteHoerbarMs == 0) return;
            var jetzt = JetztMs();
            var restMs = gepuffertFrames * 1000 / (int)AusgabeRate + latenzMs;
            hoerbarBisMs = jetzt + restMs + 150;
            sperreBisMs = jetzt + restMs + EchoNachlaufMs + (ausgangBluetooth ? Math.Max(BluetoothMinLatenzMs, latenzMs) + BlockMs : 0);
        }
    }

    private void PlaneStille(int ms) => Plane(new float[(int)AusgabeRate * ms / 1000], 0);

    private void Plane(float[] proben, float pegel)
    {
        lock (schloss)
        {
            warteschlange.Enqueue(new Stueck { Daten = proben, Pegel = pegel });
            gepuffertFrames += proben.Length;
        }
    }

    /// <summary>Holt für den Wiedergabe-Faden. Ist nichts da, kommt Stille: WasapiOut hielte sonst endgültig an, und der Ausgang geht nicht in Bereitschaft (§14.2 Nr. 2).</summary>
    private void Hole(float[] ziel, int pos, int anzahl)
    {
        var offen = anzahl;
        lock (schloss)
        {
            while (offen > 0 && warteschlange.Count > 0)
            {
                var st = warteschlange.Peek();
                var n = Math.Min(offen, st.Daten.Length - stueckPos);
                Array.Copy(st.Daten, stueckPos, ziel, pos, n);
                stueckPos += n; pos += n; offen -= n;
                gepuffertFrames = Math.Max(0, gepuffertFrames - n);
                if (stueckPos >= st.Daten.Length)
                {
                    pegelAusWert = st.Pegel;
                    warteschlange.Dequeue();
                    stueckPos = 0;
                }
            }
        }
        if (offen > 0) Array.Clear(ziel, pos, offen);
    }

    /// <summary>Wiedergabe sofort leeren (Frank unterbricht). Was schon im Gerätepuffer liegt, klingt noch aus (höchstens die Ausgangslatenz).</summary>
    public void Leeren() => PufferZuruecksetzen();

    private sealed class TonQuelle : ISampleProvider
    {
        private readonly LiveTon ton;
        private bool prioritaetGesetzt;
        public TonQuelle(LiveTon t) { ton = t; }
        public WaveFormat WaveFormat { get; } = WaveFormat.CreateIeeeFloatWaveFormat((int)AusgabeRate, 1);
        public int Read(float[] buffer, int offset, int count)
        {
            if (!prioritaetGesetzt)
            {
                prioritaetGesetzt = true;
                try { Thread.CurrentThread.Priority = ThreadPriority.Highest; } catch { }
            }
            ton.Hole(buffer, offset, count);
            return count;
        }
    }

    /// <summary>Mono auf die vorderen zwei Kanäle des Geräts, alle weiteren still.</summary>
    private sealed class KanalVerteiler : ISampleProvider
    {
        private readonly ISampleProvider quelle;
        private readonly int kanaele;
        private float[] mono = Array.Empty<float>();
        public KanalVerteiler(ISampleProvider q, int k)
        {
            quelle = q; kanaele = Math.Max(1, k);
            WaveFormat = WaveFormat.CreateIeeeFloatWaveFormat(q.WaveFormat.SampleRate, kanaele);
        }
        public WaveFormat WaveFormat { get; }
        public int Read(float[] buffer, int offset, int count)
        {
            var frames = count / kanaele;
            if (mono.Length < frames) mono = new float[frames];
            var n = quelle.Read(mono, 0, frames);
            if (n < frames) Array.Clear(mono, n, frames - n);
            for (var i = 0; i < frames; i++)
                for (var k = 0; k < kanaele; k++)
                    buffer[offset + i * kanaele + k] = k < 2 ? mono[i] : 0f;
            return frames * kanaele;
        }
    }

    /// <summary>Rückmeldungen von Windows zu den Audiogeräten. Kommen aus einem fremden Faden und dürfen dort nichts mit Geräten tun.</summary>
    [ComVisible(true)]
    public sealed class GeraeteWaechter : IMMNotificationClient
    {
        private readonly LiveTon ton;
        internal GeraeteWaechter(LiveTon t) { ton = t; }
        public void OnDeviceStateChanged([MarshalAs(UnmanagedType.LPWStr)] string deviceId, [MarshalAs(UnmanagedType.I4)] DeviceState newState) => ton.GeraetGeaendert(null, null);
        public void OnDeviceAdded([MarshalAs(UnmanagedType.LPWStr)] string pwstrDeviceId) { }
        public void OnDeviceRemoved([MarshalAs(UnmanagedType.LPWStr)] string deviceId) => ton.GeraetGeaendert(null, null);
        public void OnDefaultDeviceChanged(DataFlow flow, Role role, [MarshalAs(UnmanagedType.LPWStr)] string defaultDeviceId) => ton.GeraetGeaendert(flow, role);
        public void OnPropertyValueChanged([MarshalAs(UnmanagedType.LPWStr)] string pwstrDeviceId, PropertyKey key) { }
    }

    // MARK: WASAPI-Eigenschaften, die NAudio nicht nach außen gibt (fest auf NAudio 2.2.1 geprüft)

    private static T? PrivatesFeld<T>(object o, string name) where T : class
    {
        try { return o.GetType().GetField(name, BindingFlags.NonPublic | BindingFlags.Instance)?.GetValue(o) as T; }
        catch { return null; }
    }

    private static IAudioClient? Schnittstelle(AudioClient c) => PrivatesFeld<IAudioClient>(c, "audioClientInterface");

    private static void KategorieSetzen(AudioClient client, AudioStreamCategory kategorie)
    {
        if (Schnittstelle(client) is not IAudioClient2Voll c2) throw new InvalidOperationException("IAudioClient2 nicht verfügbar.");
        var eigenschaften = new AudioClientProperties
        {
            cbSize = (uint)Marshal.SizeOf<AudioClientProperties>(),
            bIsOffload = 0,
            eCategory = kategorie,
            Options = AudioClientStreamOptions.None,
        };
        var p = Marshal.AllocHGlobal(Marshal.SizeOf<AudioClientProperties>());
        try
        {
            Marshal.StructureToPtr(eigenschaften, p, false);
            var hr = c2.SetClientProperties(p);
            if (hr < 0) Marshal.ThrowExceptionForHR(hr);
        }
        finally { Marshal.FreeHGlobal(p); }
    }

    /// <summary>IAudioClient2 mit vollständiger Tabelle. Das von NAudio 2.2.1 ist unbrauchbar: Es lässt die zwölf geerbten Methoden aus,
    /// sein SetClientProperties landet deshalb in GetBufferSize und meldet AUDCLNT_E_NOT_INITIALIZED (gemessen).</summary>
    [ComImport, Guid("726778CD-F60A-4EDA-82DE-E47610CD78AA"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface IAudioClient2Voll
    {
        void Initialize_(); void GetBufferSize_(); void GetStreamLatency_(); void GetCurrentPadding_(); void IsFormatSupported_(); void GetMixFormat_();
        void GetDevicePeriod_(); void Start_(); void Stop_(); void Reset_(); void SetEventHandle_(); void GetService_();
        void IsOffloadCapable_();
        [PreserveSig] int SetClientProperties(IntPtr properties);
    }

    private static readonly Guid EffektEcho = new("6f64adbe-8211-11e2-8c70-2c27d7f001fa");

    [ComImport, Guid("4460B3AE-4B44-4527-8676-7548A8ACD260"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface IAudioEffectsManager
    {
        [PreserveSig] int RegisterAudioEffectsChangedNotificationCallback(IntPtr client);
        [PreserveSig] int UnregisterAudioEffectsChangedNotificationCallback(IntPtr client);
        [PreserveSig] int GetAudioEffects(out IntPtr effects, out uint numEffects);
        [PreserveSig] int SetAudioEffectState(Guid effectId, int state);
    }

    [ComImport, Guid("f4ae25b5-aaa3-437d-b6b3-dbbe2d0e9549"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    private interface IAcousticEchoCancellationControl
    {
        [PreserveSig] int SetEchoCancellationRenderEndpoint([MarshalAs(UnmanagedType.LPWStr)] string? endpointId);
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct AudioEffekt
    {
        public Guid Id;
        public int KannSetzen;
        public int Zustand;
    }

    /// <summary>Fragt beim laufenden Strom nach, ob der Echo-Effekt wirklich an ist, und schaltet ihn ein, wenn das geht.
    /// Nur eine bestätigte Antwort zählt: In allen anderen Fällen arbeitet die Echosperre.</summary>
    private static bool EchoEinschalten(AudioClient client, string? ausgangId)
    {
        try
        {
            var c = Schnittstelle(client);
            if (c == null) return false;
            var id = typeof(IAudioEffectsManager).GUID;
            if (c.GetService(id, out var dienst) < 0 || dienst is not IAudioEffectsManager m) return false;

            bool? Lesen(out bool kannSetzen)
            {
                kannSetzen = false;
                if (m.GetAudioEffects(out var liste, out var anzahl) < 0 || liste == IntPtr.Zero) return null;
                try
                {
                    var groesse = Marshal.SizeOf<AudioEffekt>();
                    for (var i = 0; i < anzahl; i++)
                    {
                        var e = Marshal.PtrToStructure<AudioEffekt>(liste + i * groesse);
                        if (e.Id != EffektEcho) continue;
                        kannSetzen = e.KannSetzen != 0;
                        return e.Zustand == 1;
                    }
                    return null;
                }
                finally { Marshal.FreeCoTaskMem(liste); }
            }

            var zustand = Lesen(out var setzbar);
            if (zustand == false && setzbar)
            {
                if (m.SetAudioEffectState(EffektEcho, 1) >= 0) zustand = Lesen(out _);
            }
            if (zustand != true) return false;

            // Bezug fürs Echo ist der Lautsprecher, auf dem Jarvis wirklich spielt. Geht das nicht, nimmt Windows seinen Standard.
            if (ausgangId != null)
            {
                try
                {
                    if (c.GetService(typeof(IAcousticEchoCancellationControl).GUID, out var st) >= 0 && st is IAcousticEchoCancellationControl k)
                        k.SetEchoCancellationRenderEndpoint(ausgangId);
                }
                catch { }
            }
            return true;
        }
        catch (Exception ex)
        {
            Log("live", "Echo-Effekt nicht abfragbar: " + ex.Message);
            return false;
        }
    }

    /// <summary>Das Communications-Mikrofon lässt Windows andere Wiedergaben leiser drücken, bis zu 80 %, auch Jarvis selbst. Diese App nimmt ihre Wiedergabe davon aus.
    /// Vor dem Stromstart über den Sitzungsmanager des Lautsprechers; auf einer Aufnahme-Sitzung lehnt Windows das ab (0x88890003, gemessen).
    /// Fremde Programme werden je nach Windows-Einstellung weiter leiser.</summary>
    private static void DuckingAus(MMDevice d)
    {
        try
        {
            var sitzung = PrivatesFeld<IAudioSessionControl2>(d.AudioSessionManager.AudioSessionControl, "audioSessionControlInterface2");
            if (sitzung == null) { Log("live", "Ducking nicht abschaltbar: Sitzung nicht gefunden"); return; }
            var hr = sitzung.SetDuckingPreference(true);
            if (hr < 0) Log("live", $"Ducking nicht abschaltbar: 0x{hr:X8}");
        }
        catch (Exception ex) { Log("live", "Ducking nicht abschaltbar: " + ex.Message); }
    }

    private static bool IstBluetooth(MMDevice d)
    {
        try
        {
            var name = d.Properties[new PropertyKey(new Guid("A45C254E-DF1C-4EFD-8020-67D146A850E0"), 24)]?.Value as string;
            return name != null && name.StartsWith("BTH", StringComparison.OrdinalIgnoreCase);
        }
        catch { return false; }
    }

    /// <summary>Ein Faden für Geräteauf- und -abbau; entspricht der seriellen DispatchQueue. MTA, damit die COM-Objekte von NAudio frei zwischen den Fäden gehen.</summary>
    private sealed class TonFaden
    {
        private readonly BlockingCollection<Action> arbeit = new();
        public TonFaden()
        {
            var t = new Thread(() =>
            {
                foreach (var a in arbeit.GetConsumingEnumerable())
                {
                    try { a(); }
                    catch (Exception ex) { Log("fehler", "Live: Ton-Faden: " + ex.Message); }
                }
            }) { IsBackground = true, Priority = ThreadPriority.AboveNormal, Name = "jarvis.live.ton" };
            t.SetApartmentState(ApartmentState.MTA);
            t.Start();
        }
        public void Post(Action a)
        {
            try { arbeit.Add(a); } catch (InvalidOperationException) { }   // schon beendet
        }
        public void Nach(int ms, Action a) => _ = Task.Delay(ms).ContinueWith(_ => Post(a), TaskScheduler.Default);
        public void Ende() => arbeit.CompleteAdding();
    }
}
