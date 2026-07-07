using System;
using System.IO;
using System.Net;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace ClaudeVoiceOverlay.Services
{
    /// <summary>
    /// Minimalistischer HTTP-Server fuer die Live-Verbindung zwischen dem
    /// Voice Terminal Overlay und externer Hardware/Plugins (Stream Deck
    /// XL, MacroPads, Companion). Hoert ausschliesslich auf
    /// <c>http://127.0.0.1:5723/</c> — keine externe Erreichbarkeit, kein
    /// netsh urlacl-Eintrag noetig (loopback-only braucht keine ACL).
    ///
    /// Routen:
    ///   GET  /autoenter/status   →  200 application/json  {"on":true|false}
    ///   POST /autoenter/toggle   →  200 application/json  {"on":true|false}  (neuer Stand)
    ///   *                        →  404
    ///
    /// Push-Variante (Server-Sent Events) wurde bewusst weggelassen: das
    /// Stream Deck Plugin polled alle 500 ms, das reicht visuell und ist
    /// robuster (keine Reconnect-Logik noetig wenn der TVO neugestartet
    /// wird).
    ///
    /// Thread-Modell: ein Listener-Thread schluckt eingehende Verbindungen
    /// in einer while-Schleife. Pro Request wird die Antwort synchron im
    /// selben Thread geschrieben — bei einem POST pro Tastendruck und
    /// einem GET alle 500 ms ist das im Microsekunden-Bereich.
    /// </summary>
    public sealed class AutoEnterStatusServer : IDisposable
    {
        // Port 5724 (Terminal-Overlay nutzt 5723) — damit beide Voice-Overlays
        // gleichzeitig laufen koennen und das Stream-Deck-Plugin sie getrennt
        // ansprechen kann, ohne Bind-Konflikt am selben Port.
        public const string ListenPrefix = "http://127.0.0.1:5724/";

        private readonly Func<bool> _getCurrentState;
        private readonly Action _toggle;
        // Liefert "ist das Overlay gerade beschaeftigt?" (Aufnahme/Transkription/BTW).
        // Wird von rebuild-overlay.ps1 ueber GET /recording/status abgefragt, damit ein
        // Rebuild NICHT killt waehrend Frank einspricht (Datenverlust-Schutz).
        private readonly Func<bool>? _getBusyState;
        private HttpListener? _listener;
        private Thread? _thread;
        private volatile bool _stopRequested;

        // Server-Sent-Events-Clients (Stream-Deck-Plugin abonniert hier).
        // SSE wird gebraucht weil das Plugin im Chromium-Webview laeuft und
        // setInterval/setTimeout dort von Chrome auf ~1/Minute gedrosselt
        // werden sobald der Webview nicht im Vordergrund ist (Background-
        // Tab-Throttling). Polling kommt nicht durch — Push schon, weil
        // EventSource-onmessage event-driven ist und NICHT gedrosselt wird.
        private readonly System.Collections.Generic.List<HttpListenerResponse> _sseClients
            = new System.Collections.Generic.List<HttpListenerResponse>();
        private readonly object _sseLock = new object();

        /// <summary>
        /// <paramref name="getCurrentState"/> liefert "ist Auto-Enter an?".
        /// <paramref name="toggle"/> wird vom Server aufgerufen wenn ein
        /// POST /autoenter/toggle eintrifft — die Implementation muss den
        /// State selbst umschalten (am UI-Thread, z.B. via Dispatcher).
        /// Nach dem Toggle wird <paramref name="getCurrentState"/> erneut
        /// abgefragt, um den neuen Stand als Antwort zu senden.
        /// </summary>
        public AutoEnterStatusServer(Func<bool> getCurrentState, Action toggle, Func<bool>? getBusyState = null)
        {
            _getCurrentState = getCurrentState ?? throw new ArgumentNullException(nameof(getCurrentState));
            _toggle = toggle ?? throw new ArgumentNullException(nameof(toggle));
            _getBusyState = getBusyState;
        }

        public void Start()
        {
            if (_listener is not null) return;

            try
            {
                _listener = new HttpListener();
                _listener.Prefixes.Add(ListenPrefix);
                _listener.Start();
            }
            catch (Exception ex)
            {
                // Port 5723 kann theoretisch von einer anderen App belegt sein,
                // oder der HttpListener weigert sich aus Permission-Gruenden zu
                // binden. Wir blocken nicht den App-Start — der Server bleibt
                // einfach aus, das Stream Deck Plugin kriegt dann beim Polling
                // einen Connection-Refused und zeigt seinen "offline"-State.
                LogStartup($"START-FEHLER: {ex.GetType().Name}: {ex.Message}");
                _listener = null;
                return;
            }

            _thread = new Thread(Loop)
            {
                IsBackground = true,
                Name = "AutoEnterStatusServer",
            };
            _thread.Start();
            LogStartup($"START-OK: laeuft auf {ListenPrefix}");
        }

        private static void LogStartup(string msg)
        {
            Console.WriteLine($"AutoEnterStatusServer: {msg}");
            try
            {
                string path = System.IO.Path.Combine(System.IO.Path.GetTempPath(), "TVO-hotkey.log");
                System.IO.File.AppendAllText(path,
                    $"{DateTime.Now:HH:mm:ss.fff} HTTP-SERVER {msg}{Environment.NewLine}");
            }
            catch { /* never block startup */ }
        }

        private void Loop()
        {
            while (!_stopRequested && _listener is { IsListening: true })
            {
                HttpListenerContext ctx;
                try
                {
                    ctx = _listener.GetContext();
                }
                catch (HttpListenerException)
                {
                    // Listener wurde gestoppt — sauberes Ende.
                    break;
                }
                catch (ObjectDisposedException)
                {
                    break;
                }

                try
                {
                    HandleRequest(ctx);
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"AutoEnterStatusServer: Request-Fehler {ex.Message}");
                    try { ctx.Response.StatusCode = 500; ctx.Response.Close(); }
                    catch { /* ignore */ }
                }
            }
        }

        private void HandleRequest(HttpListenerContext ctx)
        {
            // CORS fuer das Stream Deck Plugin (laeuft im Stream-Deck-Software-
            // Webview, der schickt manchmal Preflight). Localhost ist sicher,
            // wir erlauben pauschal.
            ctx.Response.Headers["Access-Control-Allow-Origin"] = "*";
            ctx.Response.Headers["Access-Control-Allow-Methods"] = "GET, POST, OPTIONS";
            ctx.Response.Headers["Access-Control-Allow-Headers"] = "Content-Type";
            // Cache-Buster: das Stream-Deck-Webview ist ein Chrome-Embed und
            // hat einen aggressiven HTTP-Cache. Wenn ein Status-Response auch
            // nur 1 Sekunde gecached wuerde, koennte der Plugin alten State
            // sehen und falsch anzeigen. no-store erzwingt einen frischen
            // Round-Trip bei jedem Poll.
            ctx.Response.Headers["Cache-Control"] = "no-store, no-cache, must-revalidate";
            ctx.Response.Headers["Pragma"] = "no-cache";

            if (ctx.Request.HttpMethod == "OPTIONS")
            {
                ctx.Response.StatusCode = 204;
                ctx.Response.Close();
                return;
            }

            string path = ctx.Request.Url?.AbsolutePath ?? "/";

            if (path.Equals("/autoenter/status", StringComparison.OrdinalIgnoreCase)
                && ctx.Request.HttpMethod == "GET")
            {
                WriteJsonState(ctx, _getCurrentState());
                return;
            }

            // Aufnahme-/Beschaeftigt-Status fuer den Datenverlust-Schutz beim Rebuild:
            // rebuild-overlay.ps1 fragt das VOR dem Kill ab und wartet, solange busy=true
            // (Frank spricht ein / Transkription laeuft). Antwort {"busy":true|false}.
            if (path.Equals("/recording/status", StringComparison.OrdinalIgnoreCase)
                && ctx.Request.HttpMethod == "GET")
            {
                WriteBusyJson(ctx, _getBusyState?.Invoke() ?? false);
                return;
            }

            if (path.Equals("/autoenter/toggle", StringComparison.OrdinalIgnoreCase)
                && ctx.Request.HttpMethod == "POST")
            {
                // Aufrufer-Vertrag: _toggle() ist SYNCHRON. Sprich:
                // OverlayWindow ruft Dispatcher.Invoke (nicht BeginInvoke),
                // sodass nach Rueckkehr aus _toggle() der neue State
                // garantiert sichtbar ist. Kein Sleep mehr noetig.
                bool before = _getCurrentState();
                _toggle();
                bool after = _getCurrentState();
                LogStartup($"TOGGLE before={before} after={after} (changed={before != after})");
                WriteJsonState(ctx, after);
                return;
            }

            if (path.Equals("/autoenter/events", StringComparison.OrdinalIgnoreCase)
                && ctx.Request.HttpMethod == "GET")
            {
                HandleSseSubscribe(ctx);
                return;
            }

            if (path.Equals("/log", StringComparison.OrdinalIgnoreCase)
                && ctx.Request.HttpMethod == "POST")
            {
                // Diagnose-Endpunkt fuer das Stream-Deck-Plugin (keine
                // Filesystem-Rechte im Webview). Plugin POSTet eine Textzeile,
                // wir haengen sie an TVO-hotkey.log an. Body wird einfach
                // 1:1 uebernommen — kein JSON-Parsing.
                string body;
                using (var sr = new StreamReader(ctx.Request.InputStream, ctx.Request.ContentEncoding))
                {
                    body = sr.ReadToEnd();
                }
                try
                {
                    string logPath = Path.Combine(Path.GetTempPath(), "TVO-hotkey.log");
                    File.AppendAllText(logPath, $"{DateTime.Now:HH:mm:ss.fff} PLUGIN {body}{Environment.NewLine}");
                }
                catch { /* ignore */ }
                ctx.Response.StatusCode = 204;
                ctx.Response.Close();
                return;
            }

            ctx.Response.StatusCode = 404;
            ctx.Response.Close();
        }

        private static void WriteJsonState(HttpListenerContext ctx, bool on)
        {
            string json = on ? "{\"on\":true}" : "{\"on\":false}";
            byte[] bytes = Encoding.UTF8.GetBytes(json);
            ctx.Response.StatusCode = 200;
            ctx.Response.ContentType = "application/json; charset=utf-8";
            ctx.Response.ContentLength64 = bytes.Length;
            ctx.Response.OutputStream.Write(bytes, 0, bytes.Length);
            ctx.Response.Close();
        }

        private static void WriteBusyJson(HttpListenerContext ctx, bool busy)
        {
            string json = busy ? "{\"busy\":true}" : "{\"busy\":false}";
            byte[] bytes = Encoding.UTF8.GetBytes(json);
            ctx.Response.StatusCode = 200;
            ctx.Response.ContentType = "application/json; charset=utf-8";
            ctx.Response.ContentLength64 = bytes.Length;
            ctx.Response.OutputStream.Write(bytes, 0, bytes.Length);
            ctx.Response.Close();
        }

        /// <summary>
        /// Server-Sent-Events-Subscription. Plugin verbindet via
        /// <c>new EventSource("http://127.0.0.1:5723/autoenter/events")</c>,
        /// Server haelt die Verbindung offen und schreibt bei jeder State-
        /// Aenderung eine SSE-Zeile in den Stream. Webview-Background-Tab-
        /// Throttling betrifft EventSource-onmessage NICHT — Push kommt
        /// instant durch, auch wenn setInterval im Plugin gedrosselt ist.
        /// </summary>
        private void HandleSseSubscribe(HttpListenerContext ctx)
        {
            var response = ctx.Response;
            response.StatusCode = 200;
            response.ContentType = "text/event-stream; charset=utf-8";
            response.Headers["Cache-Control"] = "no-cache, no-store";
            response.Headers["Connection"] = "keep-alive";
            response.Headers["X-Accel-Buffering"] = "no";
            response.SendChunked = true;

            // Initialer State sofort — damit das Plugin nach Connect den
            // aktuellen Stand kennt und nicht erst auf die naechste
            // State-Aenderung warten muss.
            try
            {
                bool initial = _getCurrentState();
                WriteSseLine(response, initial);
                LogStartup($"SSE client connected (initial={initial}, total={_sseClients.Count + 1})");
            }
            catch (Exception ex)
            {
                LogStartup($"SSE initial write failed: {ex.Message}");
                try { response.Close(); } catch { }
                return;
            }

            // Verbindung offen lassen — der Server-Loop kehrt zur naechsten
            // GetContext()-Iteration zurueck. Die Response bleibt registriert.
            lock (_sseLock)
            {
                _sseClients.Add(response);
            }
        }

        // Sequenzialisierung der SSE-Schreibvorgaenge. response.OutputStream
        // ist nicht thread-safe — gleichzeitige Writes auf denselben Stream
        // wuerden den SSE-Frame-Aufbau ("data: ...\n\n") zerschiessen.
        private readonly object _sseWriteLock = new object();

        // Pre-encoded SSE-Frames. Vorher haben wir bei jedem Broadcast
        // Encoding.UTF8.GetBytes("data: {...}\n\n") aufgerufen — bei vielen
        // schnellen State-Changes summiert sich das zu spuerbarem GC-Druck.
        // Da der Inhalt sich nicht aendert (nur on=true oder false), reichen
        // zwei statische Byte-Arrays.
        private static readonly byte[] _sseFrameOn =
            Encoding.UTF8.GetBytes("data: {\"on\":true}\n\n");
        private static readonly byte[] _sseFrameOff =
            Encoding.UTF8.GetBytes("data: {\"on\":false}\n\n");

        /// <summary>
        /// Wird vom OverlayWindow aufgerufen NACH jeder State-Aenderung
        /// (egal ob UI-Klick oder HTTP-Toggle). Schickt den neuen State an
        /// alle aktiven SSE-Clients. Tote Verbindungen werden anhand von
        /// Write-Exceptions erkannt und sauber entfernt.
        ///
        /// KRITISCH: Diese Methode wird vom UI-Thread aufgerufen (SetAutoEnter).
        /// Wenn die TCP-Sendepuffer eines toten/langsamen Clients voll sind,
        /// blockiert response.OutputStream.Write() bis OS-Default-Timeout (30s+)
        /// — das wuerde den gesamten TVO-UI-Thread einfrieren bei jedem
        /// State-Change. Deshalb laufen die Writes auf dem ThreadPool, nicht
        /// auf dem aufrufenden Thread.
        /// </summary>
        public void NotifyStateChanged(bool newState)
        {
            // Snapshot vom UI-Thread aus (kurzer Lock), dann Writes async.
            System.Collections.Generic.List<HttpListenerResponse> snapshot;
            lock (_sseLock) snapshot = new System.Collections.Generic.List<HttpListenerResponse>(_sseClients);

            if (snapshot.Count == 0) return;

            // Fire-and-forget auf dem ThreadPool. UI-Thread kehrt sofort zurueck.
            Task.Run(() => DoBroadcast(snapshot, newState));
        }

        private void DoBroadcast(System.Collections.Generic.List<HttpListenerResponse> snapshot, bool newState)
        {
            var dead = new System.Collections.Generic.List<HttpListenerResponse>();
            // Lock damit nicht zwei parallele Broadcasts (z.B. von schnellen
            // hintereinander-State-Changes) denselben Stream gleichzeitig
            // beschreiben und SSE-Frames verheddern.
            lock (_sseWriteLock)
            {
                foreach (var r in snapshot)
                {
                    try
                    {
                        WriteSseLine(r, newState);
                    }
                    catch
                    {
                        dead.Add(r);
                    }
                }
            }

            if (dead.Count > 0)
            {
                lock (_sseLock)
                {
                    foreach (var r in dead)
                    {
                        _sseClients.Remove(r);
                        try { r.Close(); } catch { }
                    }
                }
                LogStartup($"SSE removed {dead.Count} dead client(s), active={_sseClients.Count}");
            }
        }

        private static void WriteSseLine(HttpListenerResponse response, bool on)
        {
            // Pre-encoded Frames — keine Allocation bei jedem Broadcast.
            byte[] bytes = on ? _sseFrameOn : _sseFrameOff;
            response.OutputStream.Write(bytes, 0, bytes.Length);
            response.OutputStream.Flush();
        }

        public void Dispose()
        {
            _stopRequested = true;
            try { _listener?.Stop(); } catch { /* ignore */ }
            try { _listener?.Close(); } catch { /* ignore */ }
            _listener = null;
            _thread = null;
        }
    }
}
