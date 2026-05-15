using System;
using System.IO;
using System.Net;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace TerminalVoiceOverlay.Services
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
        public const string ListenPrefix = "http://127.0.0.1:5723/";

        private readonly Func<bool> _getCurrentState;
        private readonly Action _toggle;
        private HttpListener? _listener;
        private Thread? _thread;
        private volatile bool _stopRequested;

        /// <summary>
        /// <paramref name="getCurrentState"/> liefert "ist Auto-Enter an?".
        /// <paramref name="toggle"/> wird vom Server aufgerufen wenn ein
        /// POST /autoenter/toggle eintrifft — die Implementation muss den
        /// State selbst umschalten (am UI-Thread, z.B. via Dispatcher).
        /// Nach dem Toggle wird <paramref name="getCurrentState"/> erneut
        /// abgefragt, um den neuen Stand als Antwort zu senden.
        /// </summary>
        public AutoEnterStatusServer(Func<bool> getCurrentState, Action toggle)
        {
            _getCurrentState = getCurrentState ?? throw new ArgumentNullException(nameof(getCurrentState));
            _toggle = toggle ?? throw new ArgumentNullException(nameof(toggle));
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

            if (path.Equals("/autoenter/toggle", StringComparison.OrdinalIgnoreCase)
                && ctx.Request.HttpMethod == "POST")
            {
                _toggle(); // synchron oder via Dispatcher.BeginInvoke (Aufrufer-Sache)
                // Kurzes Warten damit der Toggle im UI-Thread durchschlaegt
                // bevor wir den neuen Stand zurueckmelden. 30 ms reichen
                // praktisch immer, ist visuell trotzdem instant.
                Thread.Sleep(30);
                WriteJsonState(ctx, _getCurrentState());
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
