using System.Runtime.InteropServices;

namespace JarvisWindows;

/// <summary>OpenAI: GPT-Live (`gpt-live-1`, wss://api.openai.com/v1/live/sessions, Client-Delegation: Jarvis denkt) und
/// Realtime (`gpt-realtime-*`, wss://api.openai.com/v1/realtime?model=…, Funktion jarvis_fragen). Protokoll wie
/// Jarvis/live/LiveGespraech.kt und best-practices/apis/openai-live-realtime.md §3–§7.</summary>
public sealed class LiveOpenAI : LiveSitzung
{
    private readonly string schluessel;
    private readonly string modell;
    private readonly string stimme;
    private LiveSocket? socket;
    private readonly object sperre = new();
    private bool Live => modell.StartsWith("gpt-live", StringComparison.Ordinal);

    // Mitschrift-Puffer (Fragmente in Lieferreihenfolge, Turns bildet die App selbst, §6)
    private string nutzerTeile = "";
    private string jarvisTeile = "";
    private string seitAuftrag = "";
    private long letzteNutzerMs;
    private long letzteJarvisMs;
    private readonly Dictionary<string, string> kennungArt = new();
    // Unterbrechen per Antippen (GPT-Live hat kein Cancel): Rest der laufenden Antwort verwerfen.
    private long verwerfenSeitMs;
    private long letzteVerworfenMs;
    // Realtime: Kürzen beim Dazwischenreden
    private string antwortItem = "";
    private string verworfenItem = "";
    private int empfangenFrames;
    private bool schliessenWartet;

    public LiveOpenAI(LiveTon ton, string schluessel, string modell, string stimme) : base(ton)
    {
        this.schluessel = schluessel; this.modell = modell; this.stimme = stimme;
    }

    public override void Starten()
    {
        var adresse = Live ? "wss://api.openai.com/v1/live/sessions" : $"wss://api.openai.com/v1/realtime?model={modell}";
        var kopf = new Dictionary<string, string> { ["Authorization"] = $"Bearer {schluessel}" };
        socket = new LiveSocket(adresse, kopf, Geoeffnet, Ereignis, Geschlossen) { TonGrenze = 2000 / Ton.BlockMs };
        socket.Los();
    }

    private void Geoeffnet()
    {
        var s = socket;
        if (s == null) return;
        if (Live)
        {
            s.Sende(LiveStart());
        }
        else
        {
            s.Sende(RealtimeSitzung());
            // Getrennt: Lehnt der Dienst die Mitschrift ab, scheitert nur sie.
            s.Sende(new JSONObjekt
            {
                ["type"] = "session.update",
                ["session"] = new JSONObjekt
                {
                    ["type"] = "realtime",
                    ["audio"] = new JSONObjekt { ["input"] = new JSONObjekt { ["transcription"] = new JSONObjekt { ["model"] = "gpt-transcribe", ["language"] = "de" } } },
                },
            });
            s.Sende(new JSONObjekt { ["type"] = "response.create", ["response"] = new JSONObjekt { ["instructions"] = LiveOptionen.Begruessung } });
            Bereit = true;
            Delegate?.LiveBereit();
        }
    }

    private void Geschlossen(int code, string grund)
    {
        bool warteteAufEnde;
        lock (sperre) warteteAufEnde = schliessenWartet;
        if (warteteAufEnde) return;
        var text = code switch
        {
            401 => "Der OpenAI-Schlüssel wurde abgelehnt.",
            403 => "Der Schlüssel darf dieses Live-Modell nicht nutzen.",
            404 => $"Das Live-Modell „{modell}“ gibt es nicht (mehr).",
            429 => "Kein Guthaben oder zu viele Anfragen bei OpenAI.",
            1000 => "",
            _ => $"Live-Verbindung getrennt ({code}): {grund.Schnitt(200)}",
        };
        if (text.Length > 0)
        {
            Log("live", $"OpenAI schließt: {code} {grund.Schnitt(300)}");
            Delegate?.LiveFehler(text);
        }
        Delegate?.LiveEnde(text.Length == 0 ? null : text);
    }

    public override void Beenden()
    {
        var s = socket;
        if (s == null) return;
        lock (sperre) schliessenWartet = true;
        if (Live && s.Offen)
        {
            // Sauber beenden: session.close, kurz auf session.closed warten (Kosten laufen sonst weiter, §6).
            s.Sende(new JSONObjekt { ["type"] = "session.close" });
            Nach(3, s.Trennen);
        }
        else
        {
            s.Trennen();
        }
    }

    public override void Mikro(byte[] pcm)
    {
        var s = socket;
        if (s == null) return;
        SendeOderHebeAuf(pcm, Bereit, block =>
            s.SendeTon(new JSONObjekt { ["type"] = Live ? "session.input_audio.append" : "input_audio_buffer.append", ["audio"] = Convert.ToBase64String(block) }));
    }

    public override void Angetippt()
    {
        Ton.Leeren();
        var jetzt = LiveTon.JetztMs();
        bool abbrechen;
        lock (sperre)
        {
            if (Live) { verwerfenSeitMs = jetzt; letzteVerworfenMs = jetzt; } else verworfenItem = antwortItem;
            abbrechen = !Live && antwortItem.Length > 0;
        }
        if (abbrechen) socket?.Sende(new JSONObjekt { ["type"] = "response.cancel" });
    }

    // MARK: Sitzungsaufbau

    private static JSONObjekt JarvisWerkzeug() => new()
    {
        ["type"] = "function",
        ["name"] = "jarvis_fragen",
        ["description"] = LiveOptionen.JarvisWerkzeugBeschreibung(),
        ["parameters"] = new JSONObjekt
        {
            ["type"] = "object",
            ["properties"] = new JSONObjekt { ["auftrag"] = new JSONObjekt { ["type"] = "string", ["description"] = "Die vollständige Frage oder der Auftrag" } },
            ["required"] = new List<object?> { "auftrag" },
        },
    };

    private JSONObjekt LiveStart()
    {
        var sitzung = new JSONObjekt
        {
            ["model"] = modell,
            ["instructions"] = LiveOptionen.Anweisung(gptLive: true, nurStimme: false),
            ["audio"] = new JSONObjekt
            {
                ["format"] = new JSONObjekt { ["type"] = "audio/pcm", ["rate"] = 24000 },
                ["output"] = new JSONObjekt { ["voice"] = stimme },
            },
            ["store"] = false,
            ["input"] = new List<object?>
            {
                new JSONObjekt
                {
                    ["type"] = "message",
                    ["role"] = "developer",
                    ["content"] = new List<object?> { new JSONObjekt { ["type"] = "input_text", ["text"] = LiveOptionen.Begruessung } },
                },
            },
            ["delegation"] = new JSONObjekt { ["type"] = "client" },
        };
        return new JSONObjekt { ["type"] = "session.start", ["event_id"] = "start-1", ["session"] = sitzung };
    }

    private JSONObjekt RealtimeSitzung()
    {
        var format = new JSONObjekt { ["type"] = "audio/pcm", ["rate"] = 24000 };
        var erkennung = new JSONObjekt { ["type"] = "semantic_vad", ["eagerness"] = "auto", ["create_response"] = true, ["interrupt_response"] = true };
        var s = new JSONObjekt
        {
            ["type"] = "realtime",
            ["model"] = modell,
            ["output_modalities"] = new List<object?> { "audio" },
            ["instructions"] = LiveOptionen.Anweisung(gptLive: false, nurStimme: false),
            ["audio"] = new JSONObjekt
            {
                ["input"] = new JSONObjekt { ["format"] = format, ["turn_detection"] = erkennung, ["noise_reduction"] = new JSONObjekt { ["type"] = "far_field" } },
                ["output"] = new JSONObjekt { ["format"] = format, ["voice"] = stimme },
            },
            ["tools"] = new List<object?> { JarvisWerkzeug() },
            ["tool_choice"] = "auto",
            ["truncation"] = new JSONObjekt { ["type"] = "retention_ratio", ["retention_ratio"] = 0.8 },
        };
        if (modell is "gpt-realtime-2.1" or "gpt-realtime-2.1-mini" or "gpt-realtime-2") s["reasoning"] = new JSONObjekt { ["effort"] = "low" };
        return new JSONObjekt { ["type"] = "session.update", ["session"] = s };
    }

    // MARK: Ereignisse

    private void Ereignis(string text)
    {
        var e = JSON.Objekt(text);
        if (e == null) return;
        if (Live) LiveEreignis(e); else RealtimeEreignis(e);
    }

    private void NutzerFertig()
    {
        string t;
        lock (sperre) { t = nutzerTeile; nutzerTeile = ""; }
        t = t.Trim();
        if (t.Length > 0) Delegate?.LiveNutzerText(t);
    }

    private void JarvisFertig()
    {
        string t;
        lock (sperre) { t = jarvisTeile; jarvisTeile = ""; }
        t = t.Trim();
        if (t.Length > 0) Delegate?.LiveJarvisText(t);
    }

    private void LiveEreignis(JSONObjekt e)
    {
        var jetzt = LiveTon.JetztMs();
        switch (e.Text("type") ?? "")
        {
            case "session.started":
                Bereit = true;
                Delegate?.LiveBereit();
                break;
            case "session.output_audio.delta":
            {
                var daten = Base64(e.Text("delta"));
                if (daten == null) return;
                bool verwerfen;
                lock (sperre)
                {
                    if (verwerfenSeitMs <= 0) verwerfen = false;
                    else
                    {
                        var hoerbar = Hoerbar(daten);
                        // Rest der abgebrochenen Antwort: bis 1,5 s ohne hörbaren Ton, höchstens 10 s.
                        if (jetzt - verwerfenSeitMs < 10_000 && (!hoerbar || jetzt - letzteVerworfenMs < 1_500))
                        {
                            if (hoerbar) letzteVerworfenMs = jetzt;
                            verwerfen = true;
                        }
                        else
                        {
                            verwerfenSeitMs = 0;
                            verwerfen = false;
                        }
                    }
                }
                if (!verwerfen) Ton.Spiele(daten, kappenErlaubt: true);
                break;
            }
            case "session.input_transcript.delta":
            {
                // Mit echter Echounterdrückung: Kommt Franks Mitschrift, während Jarvis spricht, ist das Dazwischenreden.
                // Mit Echosperre bekommt der Dienst währenddessen nur Stille: Die Mitschrift ist dann die verspätete des vorigen Satzes,
                // die Ausgabe bleibt (§14.2 Nr. 4).
                if (Ton.SprichtHoerbar && !Ton.OhneEchoUnterdrueckung) Ton.Leeren();
                JarvisFertig();
                var d = e.Text("delta") ?? "";
                lock (sperre) { nutzerTeile += d; seitAuftrag += d; letzteNutzerMs = jetzt; verwerfenSeitMs = 0; }
                break;
            }
            case "session.output_transcript.delta":
            {
                NutzerFertig();
                var d = e.Text("delta") ?? "";
                lock (sperre) { jarvisTeile += d; letzteJarvisMs = jetzt; }
                break;
            }
            case "session.delegation.created":
            {
                var d = e.Obj("delegation");
                var id = d?.Text("id");
                if (d == null || id == null) return;
                JarvisFertig();
                if ((d.Text("target") ?? "client") == "client")
                {
                    // Der Auftragstext fehlt im Ereignis (§5.2): aus Franks Mitschrift seit dem letzten Auftrag. Läuft sie dem
                    // Ereignis hinterher, kurz auf sie warten.
                    bool leer;
                    lock (sperre) leer = seitAuftrag.Trim().Length == 0;
                    Nach(leer ? 1.5 : 0, () =>
                    {
                        NutzerFertig();
                        string auftrag;
                        lock (sperre) { auftrag = seitAuftrag; seitAuftrag = ""; kennungArt[id] = "auftrag"; }
                        Delegate?.LiveWerkzeug(id, auftrag.Trim(), "");
                    });
                }
                break;
            }
            case "session.closed":
            {
                NutzerFertig(); JarvisFertig();
                var grund = e.Text("reason") ?? "";
                if (grund == "expired") Delegate?.LiveFehler("Das Live-Gespräch hat die Höchstdauer erreicht.");
                lock (sperre) schliessenWartet = true;
                socket?.Trennen();
                Delegate?.LiveEnde(null);
                break;
            }
            case "error":
            {
                var f = e.Obj("error");
                var teile = new[] { f?.Text("message"), f?.Text("code") is { } c ? $"Code {c}" : null, f?.Text("param") is { } p ? $"Feld {p}" : null };
                var t = string.Join(", ", teile.Where(x => !string.IsNullOrEmpty(x)));
                Log("live", $"GPT-Live-Fehler: {t}");
                Delegate?.LiveFehler("GPT-Live: " + (t.Length == 0 ? "unbekannter Fehler" : t));
                if (!Bereit) { Beenden(); Delegate?.LiveEnde(t); }
                break;
            }
        }
    }

    private void RealtimeEreignis(JSONObjekt e)
    {
        switch (e.Text("type") ?? "")
        {
            case "response.output_audio.delta":
            case "response.audio.delta":
            {
                var item = e.Text("item_id") ?? "";
                var daten = Base64(e.Text("delta"));
                if (daten == null) return;
                bool spielen;
                lock (sperre)
                {
                    if (item.Length > 0 && item == verworfenItem) spielen = false;
                    else
                    {
                        if (item != antwortItem) { antwortItem = item; empfangenFrames = 0; }
                        empfangenFrames += daten.Length / 2;
                        spielen = true;
                    }
                }
                // Realtime grenzt Antworten selbst ab und liefert schubweise: nichts kappen.
                if (spielen) Ton.Spiele(daten, kappenErlaubt: false);
                break;
            }
            case "input_audio_buffer.speech_started":
            {
                string item; long ms;
                lock (sperre)
                {
                    item = antwortItem;
                    ms = Math.Max(0L, (long)empfangenFrames - Ton.Gepuffert) * 1000 / (long)LiveTon.AusgabeRate;
                }
                if (item.Length > 0 && Ton.SprichtHoerbar)
                {
                    // Auf die gehörte Länge kürzen, sonst glaubt das Modell, Frank habe alles gehört (§7).
                    socket?.Sende(new JSONObjekt { ["type"] = "conversation.item.truncate", ["item_id"] = item, ["content_index"] = 0, ["audio_end_ms"] = ms });
                }
                Ton.Leeren();
                break;
            }
            case "conversation.item.input_audio_transcription.completed":
            {
                var t = (e.Text("transcript") ?? "").Trim();
                if (t.Length > 0) Delegate?.LiveNutzerText(t);
                break;
            }
            case "response.output_audio_transcript.done":
            case "response.audio_transcript.done":
            {
                var t = (e.Text("transcript") ?? "").Trim();
                if (t.Length > 0) Delegate?.LiveJarvisText(t);
                break;
            }
            case "response.function_call_arguments.done":
            {
                var name = e.Text("name") ?? "";
                if (name.Length > 0 && name != "jarvis_fragen") return;
                var auftrag = JSON.Objekt(e.Text("arguments") ?? "")?.Text("auftrag") ?? "";
                var callId = e.Text("call_id") ?? "";
                lock (sperre) kennungArt[callId] = "realtime";
                Delegate?.LiveWerkzeug(callId, auftrag, "");
                break;
            }
            case "error":
            {
                var f = e.Obj("error");
                var code = f?.Text("code") ?? "";
                if (code is "response_cancel_not_active" or "conversation_already_has_active_response") return;
                var m = f?.Text("message") ?? "";
                if (m.Contains("transcription", StringComparison.OrdinalIgnoreCase)) return;
                Log("live", $"Realtime-Fehler: {m}");
                Delegate?.LiveFehler("Live: " + (m.Length == 0 ? "unbekannter Fehler" : m));
                break;
            }
        }
    }

    // MARK: Ergebnisse

    public override bool Ergebnis(string kennung, string text)
    {
        var s = socket;
        if (s == null || !s.Offen) return false;
        string? art;
        lock (sperre) kennungArt.Remove(kennung, out art);
        if (art == "auftrag")
        {
            // Höchstens 500 Tokens je Kommentar.
            lock (sperre) kennungArt[kennung] = "auftrag_beantwortet";
            return s.Sende(new JSONObjekt { ["type"] = "session.commentary.append", ["delegation_id"] = kennung, ["content"] = text.Schnitt(1500) });
        }
        s.Sende(new JSONObjekt { ["type"] = "conversation.item.create", ["item"] = new JSONObjekt { ["type"] = "function_call_output", ["call_id"] = kennung, ["output"] = text } });
        return s.Sende(new JSONObjekt { ["type"] = "response.create" });
    }

    /// <summary>GPT-Live setzt mehrere Kommentare derselben Delegation fort (§5.2): Der erste Satz kann gesprochen werden, während Jarvis noch schreibt.</summary>
    public override bool KannStroemen => Live;

    public override bool Stueck(string kennung, string text)
    {
        var s = socket;
        if (!Live || s == null || !s.Offen) return false;
        return s.Sende(new JSONObjekt { ["type"] = "session.commentary.append", ["delegation_id"] = kennung, ["content"] = text.Schnitt(1500) });
    }

    public override bool Nachreichen(string kennung, string text)
    {
        var s = socket;
        if (s == null || !s.Offen) return false;
        if (Live)
        {
            // Mehrere Kommentare mit derselben delegation_id setzen fort (§5.2).
            return s.Sende(new JSONObjekt { ["type"] = "session.commentary.append", ["delegation_id"] = kennung, ["content"] = text.Schnitt(1500) });
        }
        return s.Sende(new JSONObjekt
        {
            ["type"] = "response.create",
            ["response"] = new JSONObjekt { ["instructions"] = $"Sag Frank jetzt auf Deutsch in natürlichen Worten das Ergebnis von Jarvis zu seiner Anfrage von eben, ohne Fakten zu verändern: {text.Schnitt(3000)}" },
        });
    }

    public override void Takt()
    {
        // GPT-Live meldet keine Turn-Grenzen: nach 2 s ohne neue Fragmente gilt der Beitrag als fertig (§6).
        if (!Live) return;
        var jetzt = LiveTon.JetztMs();
        bool n, j;
        lock (sperre)
        {
            n = nutzerTeile.Length > 0 && jetzt - letzteNutzerMs > 2_000;
            j = jarvisTeile.Length > 0 && jetzt - letzteJarvisMs > 2_000;
        }
        if (n) NutzerFertig();
        if (j) JarvisFertig();
    }

    public static bool Hoerbar(byte[] d)
    {
        var s = MemoryMarshal.Cast<byte, short>(d);
        if (s.Length == 0) return false;
        double summe = 0;
        foreach (var x in s) { double v = x; summe += v * v; }
        return Math.Sqrt(summe / s.Length) >= LiveTon.HoerbarEffektiv;
    }
}
