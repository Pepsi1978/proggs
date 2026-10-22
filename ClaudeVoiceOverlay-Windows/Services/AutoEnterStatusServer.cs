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
    /// in einer while-Schleife. Each request is handled on its own pool task,
    /// so a slow /log body or a blocked UI-thread callback cannot stall the
    /// listener (status polls, SSE, deployment guard keep working).
    ///
    /// CSRF protection: state-changing POSTs (/autoenter/toggle,
    /// /deployment/prepare, /deployment/release, /log) are rejected with 403
    /// when the request carries a web Origin (http:// or https://) or a web
    /// Referer. Native clients (PowerShell, curl) send no Origin; the Stream
    /// Deck plugin page is loaded from file:// and sends "Origin: null".
    /// </summary>
    public sealed class AutoEnterStatusServer : IDisposable
    {
        // Port 5724 (Terminal-Overlay nutzt 5723) — damit beide Voice-Overlays
        // gleichzeitig laufen koennen und das Stream-Deck-Plugin sie getrennt
        // ansprechen kann, ohne Bind-Konflikt am selben Port.
        public const string ListenPrefix = "http://127.0.0.1:5724/";

        /// <summary>Diagnostic log in %TEMP% (hotkeys, HTTP server, Stream Deck plugin lines).</summary>
        // Own file so CVO lines do not mix into the TVO log.
        public const string HotkeyLogFileName = "CVO-hotkey.log";

        // POST /log body cap; plugin lines are a few hundred bytes.
        private const int MaxLogBodyBytes = 8 * 1024;
        // Hotkey log rotates to ".1" above this size (same scheme as DiagLog).
        private const long MaxHotkeyLogBytes = 1024 * 1024;
        // Upper bound for UI-thread callbacks (Dispatcher.Invoke in OverlayWindow).
        private static readonly TimeSpan CallbackTimeout = TimeSpan.FromSeconds(5);
        private static readonly object HotkeyLogLock = new object();

        private readonly Func<bool> _getCurrentState;
        private readonly Action _toggle;
        // Busy-Status plus atomare Deployment-Reservierung schuetzen Aufnahme,
        // Transkription und Paste in allen Build-/Update-Einstiegen.
        private readonly Func<bool>? _getBusyState;
        private readonly Func<bool>? _tryBeginDeployment;
        private readonly Action? _endDeployment;
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
        public AutoEnterStatusServer(
            Func<bool> getCurrentState,
            Action toggle,
            Func<bool>? getBusyState = null,
            Func<bool>? tryBeginDeployment = null,
            Action? endDeployment = null)
        {
            _getCurrentState = getCurrentState ?? throw new ArgumentNullException(nameof(getCurrentState));
            _toggle = toggle ?? throw new ArgumentNullException(nameof(toggle));
            _getBusyState = getBusyState;
            _tryBeginDeployment = tryBeginDeployment;
            _endDeployment = endDeployment;
        }

        public void Start()
        {
            if (_listener is not null) return;

            try
            {
                _listener = new HttpListener();
                _listener.Prefixes.Add(ListenPrefix);
                _listener.Start();
                // Bound slow request bodies (default is 2 minutes).
                try { _listener.TimeoutManager.EntityBody = TimeSpan.FromSeconds(5); }
                catch { /* not supported on this platform -> keep default */ }
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
            AppendHotkeyLog("HTTP-SERVER", msg);
        }

        /// <summary>
        /// Appends one line to %TEMP%\<see cref="HotkeyLogFileName"/> and rotates
        /// the file to ".1" once it exceeds 1 MB. Never throws.
        /// </summary>
        public static void AppendHotkeyLog(string tag, string msg)
        {
            try
            {
                string path = Path.Combine(Path.GetTempPath(), HotkeyLogFileName);
                lock (HotkeyLogLock)
                {
                    try
                    {
                        var fi = new FileInfo(path);
                        if (fi.Exists && fi.Length > MaxHotkeyLogBytes)
                            File.Move(path, path + ".1", overwrite: true);
                    }
                    catch { /* another writer holds the file -> rotate next time */ }
                    File.AppendAllText(path, $"{DateTime.Now:HH:mm:ss.fff} {tag} {msg}{Environment.NewLine}");
                }
            }
            catch { /* logging must never break the server */ }
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

                // One task per request: a slow body or a blocked callback must not
                // stall the accept loop.
                var request = ctx;
                _ = Task.Run(() => HandleRequestSafe(request));
            }
        }

        private void HandleRequestSafe(HttpListenerContext ctx)
        {
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

        private static bool IsMutatingRoute(string path) =>
            path.Equals("/autoenter/toggle", StringComparison.OrdinalIgnoreCase)
            || path.Equals("/deployment/prepare", StringComparison.OrdinalIgnoreCase)
            || path.Equals("/deployment/release", StringComparison.OrdinalIgnoreCase)
            || path.Equals("/log", StringComparison.OrdinalIgnoreCase);

        private static bool IsWebUrl(string? value) =>
            !string.IsNullOrEmpty(value)
            && (value.StartsWith("http://", StringComparison.OrdinalIgnoreCase)
                || value.StartsWith("https://", StringComparison.OrdinalIgnoreCase));

        /// <summary>
        /// Runs a (possibly UI-thread-marshalled) callback with an upper time bound.
        /// Returns false on timeout; the callback keeps running in the background
        /// and its task is handed out so the caller can compensate.
        /// </summary>
        private static bool TryRunWithTimeout<T>(Func<T> callback, out T result, out Task<T> task)
        {
            task = Task.Run(callback);
            bool completed;
            try { completed = task.Wait(CallbackTimeout); }
            catch (AggregateException ae) when (ae.InnerException != null)
            {
                System.Runtime.ExceptionServices.ExceptionDispatchInfo.Capture(ae.InnerException).Throw();
                throw;
            }
            result = completed ? task.Result : default!;
            return completed;
        }

        private void HandleRequest(HttpListenerContext ctx)
        {
            string path = ctx.Request.Url?.AbsolutePath ?? "/";
            string? origin = ctx.Request.Headers["Origin"];
            bool mutating = IsMutatingRoute(path);

            // CORS fuer das Stream Deck Plugin (laeuft im Stream-Deck-Software-
            // Webview, der schickt manchmal Preflight). Read-only routes stay
            // open ("*"). State-changing routes never get "*": web origins are
            // rejected below, the plugin's "null"/file:// origin is echoed back
            // so its fetch can read the response.
            if (!mutating)
                ctx.Response.Headers["Access-Control-Allow-Origin"] = "*";
            else if (!string.IsNullOrEmpty(origin) && !IsWebUrl(origin))
                ctx.Response.Headers["Access-Control-Allow-Origin"] = origin;
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

            // CSRF guard: a web page (any http/https origin) must not toggle,
            // reserve/release deployments or spam the log. Checked before any
            // state change. Referer catches pages that suppress Origin.
            if (mutating && ctx.Request.HttpMethod == "POST")
            {
                string? referer = ctx.Request.Headers["Referer"];
                if (IsWebUrl(origin) || IsWebUrl(referer))
                {
                    string refLog = referer is { Length: > 120 } ? referer.Substring(0, 120) : referer ?? "-";
                    LogStartup($"REJECTED {path} origin={origin ?? "-"} referer={refLog}");
                    ctx.Response.StatusCode = 403;
                    ctx.Response.Close();
                    return;
                }
            }

            if (path.Equals("/autoenter/status", StringComparison.OrdinalIgnoreCase)
                && ctx.Request.HttpMethod == "GET")
            {
                WriteJsonState(ctx, _getCurrentState());
                return;
            }

            // Aufnahme-/Beschaeftigt-Status fuer Diagnose und Legacy-Migration.
            if (path.Equals("/recording/status", StringComparison.OrdinalIgnoreCase)
                && ctx.Request.HttpMethod == "GET")
            {
                bool busy = false;
                if (_getBusyState is { } getBusy && !TryRunWithTimeout(getBusy, out busy, out _))
                {
                    // Timeout -> report busy (the conservative answer for deploy scripts).
                    busy = true;
                    LogStartup("recording/status callback timeout -> busy=true");
                }
                WriteBusyJson(ctx, busy);
                return;
            }

            if (path.Equals("/deployment/prepare", StringComparison.OrdinalIgnoreCase)
                && ctx.Request.HttpMethod == "POST")
            {
                bool ready = false;
                if (_tryBeginDeployment is { } tryBegin && !TryRunWithTimeout(tryBegin, out ready, out var pending))
                {
                    // Answered "not ready"; if the late callback still reserved the
                    // deployment, undo it so recording is not blocked forever.
                    ready = false;
                    LogStartup("deployment/prepare callback timeout -> ready=false");
                    pending.ContinueWith(t =>
                    {
                        if (t.Status == TaskStatus.RanToCompletion && t.Result)
                        {
                            try { _endDeployment?.Invoke(); } catch { /* ignore */ }
                        }
                    }, TaskScheduler.Default);
                }
                WriteDeploymentJson(ctx, ready);
                return;
            }

            if (path.Equals("/deployment/release", StringComparison.OrdinalIgnoreCase)
                && ctx.Request.HttpMethod == "POST")
            {
                if (_endDeployment is { } end && !TryRunWithTimeout(() => { end(); return true; }, out _, out _))
                    LogStartup("deployment/release callback timeout (release continues in background)");
                WriteDeploymentJson(ctx, true);
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
                if (!TryRunWithTimeout(() => { _toggle(); return true; }, out _, out _))
                {
                    LogStartup($"TOGGLE timeout before={before} (UI thread busy)");
                    ctx.Response.StatusCode = 503;
                    ctx.Response.Close();
                    return;
                }
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
                // wir haengen sie an die Hotkey-Logdatei an. Body wird einfach
                // 1:1 uebernommen — kein JSON-Parsing. Capped at 8 KB.
                string? body = ctx.Request.ContentLength64 > MaxLogBodyBytes
                    ? null
                    : ReadBodyCapped(ctx.Request, MaxLogBodyBytes);
                if (body is null)
                {
                    ctx.Response.StatusCode = 413;
                    ctx.Response.Close();
                    return;
                }
                AppendHotkeyLog("PLUGIN", body.Replace('\r', ' ').Replace('\n', ' '));
                ctx.Response.StatusCode = 204;
                ctx.Response.Close();
                return;
            }

            ctx.Response.StatusCode = 404;
            ctx.Response.Close();
        }

        /// <summary>
        /// Reads at most <paramref name="maxBytes"/> of the request body; null if
        /// the body is larger. Slow bodies are bounded by TimeoutManager.EntityBody.
        /// </summary>
        private static string? ReadBodyCapped(HttpListenerRequest request, int maxBytes)
        {
            var buffer = new byte[maxBytes + 1];
            int total = 0;
            var stream = request.InputStream;
            while (total < buffer.Length)
            {
                int n = stream.Read(buffer, total, buffer.Length - total);
                if (n <= 0) break;
                total += n;
            }
            if (total > maxBytes) return null;
            var encoding = request.ContentEncoding ?? Encoding.UTF8;
            return encoding.GetString(buffer, 0, total);
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

        private static void WriteDeploymentJson(HttpListenerContext ctx, bool ready)
        {
            string json = ready ? "{\"ready\":true}" : "{\"ready\":false}";
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
