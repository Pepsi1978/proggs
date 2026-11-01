namespace JarvisWindows;

/// <summary>Gemini Live (BidiGenerateContent per WebSocket) mit „Jarvis direkt“ wie auf dem Handy: Die Weiche gibt jeden
/// abgeschlossenen Beitrag mit Franks erkannten Worten an Jarvis; Gemini spricht nur Jarvis' Antworten und kurze neutrale
/// Signale. Verbindungen enden nach etwa 10 Minuten (goAway) und werden mit der Fortsetzungskennung nahtlos erneuert;
/// bei Abbruch nach 2 s neu, lehnt Google ab („already connected“), noch nach 3 und 6 s, danach als neue Sitzung mit dem
/// bisher Gesagten (letzte 12 Beiträge).</summary>
public sealed class LiveGemini : LiveSitzung
{
    private readonly string schluessel;
    private readonly string modell;
    private readonly string stimme;
    private readonly JarvisWeiche weiche;
    private readonly object sperre = new();
    private LiveSocket? socket;
    private LiveSocket? abgeloest;
    private string apiVersion = "v1beta";
    private string fortsetzKennung = "";
    private bool begruesst;
    private bool beendet;
    private bool wartetAufNeu;
    private int neuVersuche;
    private string nutzerTeile = "";
    private bool antwortLaeuft;
    private readonly Dictionary<string, LiveSocket> aufrufVerbindung = new();
    private readonly List<(bool VonFrank, string Text)> diesesGespraech = new();
    private string jarvisTeile = "";
    public override bool IstGemini => true;

    /// <summary>Werkzeugaufrufe: Extended Thinking nebenbei (NON_BLOCKING), 3.8 Live wartend (BLOCKING), 3.1 Flash ohne Angabe.</summary>
    private bool Nebenbei => modell == "gemini-3.8-live-extended-thinking";

    public LiveGemini(LiveTon ton, string schluessel, string modell, string stimme) : base(ton)
    {
        this.schluessel = schluessel; this.modell = modell; this.stimme = stimme;
        // Pause bis zur Antwort (800 ms) + 400 ms: Franks Vorgabe ist Tempo. Am Handy sind es + 1200 ms; nur der Rückfall, wenn Gemini nicht
        // reagiert, und Sprechen laut vorläufiger Mitschrift verlängert die Zeit. Noch nicht im Gespräch gemessen.
        weiche = new JarvisWeiche(ruheMs: 800 + 400);
    }

    public override void Starten() => Verbinden();

    private void Verbinden()
    {
        var enc = Uri.EscapeDataString(schluessel);
        var adresse = $"wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.{apiVersion}.GenerativeService.BidiGenerateContent?key={enc}";
        Bereit = false;
        LiveSocket? neu = null;
        neu = new LiveSocket(adresse, null,
            beiOffen: () => neu!.Sende(Setup()),
            beiText: t => Ereignis(neu!, t),
            beiZu: (code, grund) => Geschlossen(neu!, code, grund)) { TonGrenze = 2000 / Ton.BlockMs };
        lock (sperre)
        {
            if (socket != null) { abgeloest?.Trennen(); abgeloest = socket; }
            socket = neu;
            wartetAufNeu = false;
        }
        neu.Los();
    }

    private JSONObjekt Setup()
    {
        var gen = new JSONObjekt
        {
            ["responseModalities"] = new List<object?> { "AUDIO" },
            ["speechConfig"] = new JSONObjekt { ["voiceConfig"] = new JSONObjekt { ["prebuiltVoiceConfig"] = new JSONObjekt { ["voiceName"] = stimme } } },
        };
        if (LiveOptionen.GeminiDenkstufe(modell) is { } stufe) gen["thinkingConfig"] = new JSONObjekt { ["thinkingLevel"] = stufe };
        // Neue Sitzung mitten im Gespräch ohne Fortsetzung: das bisher Gesagte dazu.
        List<(bool VonFrank, string Text)> dieses;
        string kennung;
        lock (sperre)
        {
            dieses = begruesst && fortsetzKennung.Length == 0 ? diesesGespraech.ToList() : new();
            kennung = fortsetzKennung;
        }
        var bisher = dieses.Count == 0 ? "" : "\n# Bisheriges Gespräch (zum Anknüpfen)\n" +
            string.Join("\n", dieses.Select(x => (x.VonFrank ? "Frank: " : "Jarvis: ") + x.Text.Schnitt(1200)));
        var funktion = new JSONObjekt
        {
            ["name"] = "jarvis_fragen",
            ["description"] = LiveOptionen.JarvisWerkzeugBeschreibung(),
            ["parameters"] = new JSONObjekt
            {
                ["type"] = "OBJECT",
                ["properties"] = new JSONObjekt { ["auftrag"] = new JSONObjekt { ["type"] = "STRING", ["description"] = "Die vollständige Frage oder der Auftrag" } },
                ["required"] = new List<object?> { "auftrag" },
            },
        };
        if (Nebenbei) funktion["behavior"] = "NON_BLOCKING"; else if (modell == "gemini-3.8-live") funktion["behavior"] = "BLOCKING";
        var erkennung = new JSONObjekt
        {
            ["startOfSpeechSensitivity"] = "START_SENSITIVITY_LOW",
            ["endOfSpeechSensitivity"] = "END_SENSITIVITY_LOW",
            ["prefixPaddingMs"] = 20,
            ["silenceDurationMs"] = 800,
        };
        var fortsetzung = new JSONObjekt();
        if (kennung.Length > 0) fortsetzung["handle"] = kennung;
        var s = new JSONObjekt
        {
            ["model"] = $"models/{modell}",
            ["generationConfig"] = gen,
            ["systemInstruction"] = new JSONObjekt
            {
                ["parts"] = new List<object?>
                {
                    new JSONObjekt { ["text"] = LiveOptionen.Anweisung(gptLive: false, nurStimme: true) + bisher + "\n# Language\nRESPOND IN GERMAN. YOU MUST RESPOND UNMISTAKABLY IN GERMAN." },
                },
            },
            ["tools"] = new List<object?> { new JSONObjekt { ["functionDeclarations"] = new List<object?> { funktion } } },
            ["realtimeInputConfig"] = new JSONObjekt { ["automaticActivityDetection"] = erkennung, ["activityHandling"] = "START_OF_ACTIVITY_INTERRUPTS" },
            ["outputAudioTranscription"] = new JSONObjekt(),
            ["inputAudioTranscription"] = new JSONObjekt(),
            ["contextWindowCompression"] = new JSONObjekt { ["slidingWindow"] = new JSONObjekt(), ["triggerTokens"] = 16000 },
            ["sessionResumption"] = fortsetzung,
        };
        return new JSONObjekt { ["setup"] = s };
    }

    private void Geschlossen(LiveSocket w, int code, string grund)
    {
        bool aktuell, ende, wartet;
        lock (sperre) { aktuell = ReferenceEquals(w, socket); ende = beendet; wartet = wartetAufNeu; }
        if (!aktuell || ende || wartet) return;
        Log("live", $"Gemini schließt: {code} {grund.Schnitt(300)}");
        var g = grund.ToLowerInvariant();
        if (code == 401 || code == 403 || g.Contains("api key") || g.Contains("permission_denied") || g.Contains("unauthenticated"))
        {
            Delegate?.LiveFehler("Der Gemini-Schlüssel wurde abgelehnt."); Delegate?.LiveEnde("Schlüssel"); return;
        }
        if (g.Contains("quota") || g.Contains("resource_exhausted") || code == 429)
        {
            Delegate?.LiveFehler("Gemini-Kontingent erschöpft oder zu viele Anfragen."); Delegate?.LiveEnde("Kontingent"); return;
        }
        var modellFehler = (code == 1007 || code == 1008 || g.Contains("not found") || g.Contains("not supported") || g.Contains("invalid argument"))
            && !g.Contains("already connected") && !g.Contains("existing client");
        if (modellFehler)
        {
            if (!Bereit && apiVersion == "v1beta" && modell.Contains("extended")) { apiVersion = "v1alpha"; Verbinden(); return; }
            Delegate?.LiveFehler($"Gemini hat den Verbindungsaufbau abgelehnt: {grund.Schnitt(160)}"); Delegate?.LiveEnde("Modell"); return;
        }
        // Netz weg, Serverfehler oder abgelehnte Fortsetzung: neu verbinden.
        int versuch;
        lock (sperre) { neuVersuche += 1; wartetAufNeu = true; versuch = neuVersuche; }
        if (versuch > 5)
        {
            Delegate?.LiveFehler("Gemini-Verbindung mehrfach abgebrochen. Bitte neu starten."); Delegate?.LiveEnde("Netz"); return;
        }
        double[] warte = { 2, 3, 6, 2, 2 };
        if (versuch > 3) lock (sperre) fortsetzKennung = "";   // danach als neue Sitzung mit dem bisher Gesagten
        Log("live", $"Gemini: Neuverbindung {versuch} in {warte[versuch - 1]} s");
        weiche.VerbindungNeu();
        Nach(warte[versuch - 1], () =>
        {
            lock (sperre) if (beendet) return;
            Verbinden();
        });
    }

    public override void Beenden()
    {
        LiveSocket? s, a;
        lock (sperre) { beendet = true; s = socket; a = abgeloest; }
        s?.Trennen(); a?.Trennen();
    }

    public override void Mikro(byte[] pcm)
    {
        LiveSocket? s;
        lock (sperre) s = wartetAufNeu ? null : socket;
        // Auch beim Start und beim Wechsel nach goAway: Sonst fehlen Worte mitten im Satz.
        SendeOderHebeAuf(pcm, Bereit && s != null, block => s!.SendeTon(new JSONObjekt
        {
            ["realtimeInput"] = new JSONObjekt { ["audio"] = new JSONObjekt { ["data"] = Convert.ToBase64String(block), ["mimeType"] = "audio/pcm;rate=16000" } },
        }));
    }

    public override void Angetippt()
    {
        weiche.Angetippt();
        Ton.Leeren();
    }

    private void Merken(bool vonFrank, string text)
    {
        lock (sperre)
        {
            diesesGespraech.Add((vonFrank, text));
            if (diesesGespraech.Count > 12) diesesGespraech.RemoveRange(0, diesesGespraech.Count - 12);
        }
    }

    private void NutzerFertig()
    {
        string t;
        lock (sperre) { t = nutzerTeile; nutzerTeile = ""; }
        t = t.Trim();
        if (t.Length > 0) { Merken(true, t); Delegate?.LiveNutzerText(t); }
    }

    private void JarvisFertig()
    {
        string t;
        lock (sperre) { t = jarvisTeile; jarvisTeile = ""; }
        t = t.Trim();
        if (t.Length > 0) { Merken(false, t); Delegate?.LiveJarvisText(t); }
    }

    private void Ereignis(LiveSocket w, string text)
    {
        var e = JSON.Objekt(text);
        if (e == null) return;
        bool istAktuell, istAlt;
        lock (sperre) { istAktuell = ReferenceEquals(w, socket); istAlt = ReferenceEquals(w, abgeloest); }
        if (!istAktuell && !istAlt) return;
        // Von der abgelösten Verbindung nur noch Ton, Mitschrift und Werkzeugaufrufe.
        if (!istAktuell && (e.ContainsKey("setupComplete") || e.ContainsKey("goAway") || e.ContainsKey("sessionResumptionUpdate"))) return;
        var jetzt = LiveTon.JetztMs();

        if (e.ContainsKey("setupComplete"))
        {
            Bereit = true;
            LiveSocket? alt;
            bool warBegruesst;
            lock (sperre)
            {
                neuVersuche = 0;
                alt = abgeloest; abgeloest = null;
                warBegruesst = begruesst; begruesst = true;
            }
            if (alt != null) Nach(1.5, alt.Trennen);
            if (warBegruesst) weiche.VerbindungNeu();
            else w.Sende(new JSONObjekt { ["realtimeInput"] = new JSONObjekt { ["text"] = LiveOptionen.Begruessung } });
            Delegate?.LiveBereit();
            return;
        }
        if (e.Obj("serverContent") is { } c)
        {
            if (c.Obj("interimInputTranscription")?.Text("text") is { Length: > 0 })
                Ausfuehren(weiche.NutzerSpricht(jetzt));
            if (c.Obj("inputTranscription")?.Text("text") is { Length: > 0 } eingabe)
            {
                lock (sperre) nutzerTeile += eingabe;
                Ausfuehren(weiche.NutzerText(eingabe, jetzt));
            }
            var frei = weiche.TonFrei;
            if (c.Wahr("interrupted") == true)
            {
                lock (sperre) antwortLaeuft = false;
                Ton.Leeren();
                JarvisFertig();
                Ausfuehren(weiche.Unterbrochen(jetzt));
            }
            if (c.Obj("outputTranscription")?.Text("text") is { Length: > 0 } ausgabe)
            {
                if (frei) lock (sperre) jarvisTeile += ausgabe;
                else weiche.EigeneMitschrift(ausgabe, jetzt);
            }
            var teile = c.Obj("modelTurn")?.Objekte("parts") ?? new List<JSONObjekt>();
            var toene = new List<byte[]>();
            foreach (var p in teile)
            {
                if (p.Wahr("thought") == true) continue;
                var d = Base64(p.Obj("inlineData")?.Text("data"));
                if (d != null) toene.Add(d);
            }
            if (toene.Count > 0 || c.ContainsKey("outputTranscription")) Ausfuehren(weiche.ModellAusgabe(jetzt, ton: toene.Count > 0));
            if (frei)
            {
                if (toene.Count > 0) lock (sperre) antwortLaeuft = true;
                foreach (var t in toene) Ton.Spiele(t, kappenErlaubt: false);
            }
            else
            {
                foreach (var t in toene) weiche.EigenerTon(t, jetzt);
            }
            if (c.Wahr("turnComplete") == true)
            {
                lock (sperre) antwortLaeuft = false;
                JarvisFertig();
                Ausfuehren(weiche.ZugEnde(jetzt));
            }
            return;
        }
        if (e.Obj("toolCall") is { } tc && tc.Liste("functionCalls") != null)
        {
            foreach (var f in tc.Objekte("functionCalls"))
            {
                if (f.Text("name") != "jarvis_fragen") continue;
                var id = f.Text("id") ?? Guid.NewGuid().ToString().ToUpperInvariant();
                lock (sperre) aufrufVerbindung[id] = w;
                var auftrag = f.Obj("args")?.Text("auftrag") ?? "";
                Ausfuehren(weiche.WerkzeugAufruf(id, auftrag, jetzt));
            }
            return;
        }
        if (e.Obj("toolCallCancellation") is { } tca && tca.Liste("ids") != null)
        {
            var ids = tca.Texte("ids");
            Log("live", $"Gemini bricht Werkzeugaufruf ab: [{string.Join(", ", ids)}]");
            lock (sperre) foreach (var id in ids) aufrufVerbindung.Remove(id);
            weiche.WerkzeugAbgebrochen(ids);
            return;
        }
        if (e.Obj("sessionResumptionUpdate") is { } u)
        {
            if (u.Wahr("resumable") == true && u.Text("newHandle") is { Length: > 0 } h) lock (sperre) fortsetzKennung = h;
            return;
        }
        if (e.ContainsKey("goAway"))
        {
            bool kann;
            lock (sperre) kann = fortsetzKennung.Length > 0 && !beendet;
            Log("live", $"Gemini goAway, Fortsetzung {(kann ? "folgt" : "nicht möglich")}");
            if (kann) Verbinden();
        }
    }

    /// <summary>Führt die Aktionen der Weiche aus.</summary>
    private void Ausfuehren(List<JarvisWeiche.Aktion> aktionen)
    {
        foreach (var a in aktionen)
        {
            switch (a)
            {
                case JarvisWeiche.Aktion.NutzerFertig:
                    NutzerFertig();
                    break;
                case JarvisWeiche.Aktion.AnJarvis j:
                    JarvisFertig();
                    Delegate?.LiveWerkzeug(j.Kennung, j.Auftrag, j.Hinweis);
                    break;
                case JarvisWeiche.Aktion.Werkzeugantwort z:
                {
                    LiveSocket? w;
                    lock (sperre) w = socket;
                    if (w == null) continue;
                    for (var i = 0; i < z.Ids.Count; i++)
                    {
                        var id = z.Ids[i];
                        var r = new JSONObjekt { ["result"] = i == 0 ? z.Text : "Die Antwort von Jarvis steht im ersten Ergebnis." };
                        if (Nebenbei) r["scheduling"] = "INTERRUPT";
                        LiveSocket ziel;
                        lock (sperre)
                        {
                            aufrufVerbindung.Remove(id, out var v);
                            ziel = v != null && (ReferenceEquals(v, socket) || ReferenceEquals(v, abgeloest)) ? v : w;
                        }
                        ziel.Sende(new JSONObjekt
                        {
                            ["toolResponse"] = new JSONObjekt
                            {
                                ["functionResponses"] = new List<object?> { new JSONObjekt { ["id"] = id, ["name"] = "jarvis_fragen", ["response"] = r } },
                            },
                        });
                    }
                    break;
                }
                case JarvisWeiche.Aktion.Einspielen t:
                {
                    LiveSocket? w;
                    lock (sperre) w = socket;
                    if (w == null) continue;
                    // 3.8: clientContent ist während der ganzen Sitzung erlaubt und kommt in Reihenfolge an; 3.1 nur realtimeInput.
                    if (modell.StartsWith("gemini-3.8", StringComparison.Ordinal))
                    {
                        w.Sende(new JSONObjekt
                        {
                            ["clientContent"] = new JSONObjekt
                            {
                                ["turns"] = new List<object?>
                                {
                                    new JSONObjekt { ["role"] = "user", ["parts"] = new List<object?> { new JSONObjekt { ["text"] = t.Text } } },
                                },
                                ["turnComplete"] = true,
                            },
                        });
                    }
                    else
                    {
                        w.Sende(new JSONObjekt { ["realtimeInput"] = new JSONObjekt { ["text"] = t.Text } });
                    }
                    break;
                }
                case JarvisWeiche.Aktion.Anzeigen t:
                    // Jarvis' Antwort steht über den Agenten schon im Gespräch; nur ins Live-Protokoll.
                    Log("live", $"Gemini hat Jarvis' Antwort nicht gesprochen: {t.Text.Schnitt(120)}");
                    break;
                case JarvisWeiche.Aktion.Signal s:
                    foreach (var t in s.Ton) Ton.Spiele(t, kappenErlaubt: false);
                    break;
                case JarvisWeiche.Aktion.Fehler t:
                    Log("live", $"Weiche: {t.Text}");
                    Delegate?.LiveFehler(t.Text);
                    break;
                case JarvisWeiche.Aktion.Protokoll t:
                    Log("live", t.Text);
                    break;
            }
        }
    }

    private bool SocketOffen { get { lock (sperre) return socket?.Offen ?? false; } }

    public override bool Ergebnis(string kennung, string text)
    {
        Ausfuehren(weiche.JarvisAntwort(kennung, text, LiveTon.JetztMs()));
        return SocketOffen;
    }

    public override bool Nachreichen(string kennung, string text)
    {
        if (!SocketOffen) return false;
        Ausfuehren(weiche.Nachreichen(text, LiveTon.JetztMs()));
        return true;
    }

    public override void Takt()
    {
        if (!Bereit) return;
        Ausfuehren(weiche.Takt(LiveTon.JetztMs()));
    }

    /// <summary>Jarvis arbeitet laut Weiche (für die Statusanzeige).</summary>
    public bool WeicheArbeitet => weiche.Arbeitet;
}
