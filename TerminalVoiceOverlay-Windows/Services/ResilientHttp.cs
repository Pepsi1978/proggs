using System;
using System.IO;
using System.Net.Http;
using System.Net.Sockets;
using System.Threading.Tasks;

namespace TerminalVoiceOverlay.Services
{
    /// <summary>
    /// Gemeinsame Fabrik fuer die HTTP-Verbindungen aller Netz-Dienste des
    /// Overlays (Gemini-Transkription, Gemini-Textkorrektur, Groq).
    ///
    /// WARUM ES DIESE KLASSE GIBT (Vorfall 30.08.2026, 11:12 und 11:19):
    /// Zwei Aufrufe hintereinander blieben haengen, bis der GESAMT-Timeout
    /// zuschlug — 100 s beim Prompt-Abgleich, 120 s bei der Transkription.
    /// Im Diag-Log steht bei beiden kein HTTP-Status, nur
    /// "The request was canceled due to the configured HttpClient.Timeout".
    /// Fuer Frank sah das so aus: Knopf bleibt orange, es passiert zwei
    /// Minuten lang nichts, das Diktat scheint verloren. Dieselbe Aufnahme
    /// lief direkt danach in 3,5 s durch — die Gegenstelle war also gesund,
    /// die VERBINDUNG war es nicht.
    ///
    /// Zwei Stellen im .NET-Standardverhalten machen genau dieses Bild:
    ///
    ///   1. <see cref="SocketsHttpHandler.ConnectTimeout"/> ist ab Werk
    ///      unendlich. Stallt der TCP- oder TLS-Aufbau (halb kaputtes IPv6,
    ///      WLAN-Wechsel, VPN-Aufwachen), wartet der Aufruf bis zum
    ///      Gesamt-Timeout, ohne dass je ein Byte floss.
    ///   2. Ohne TCP-Keepalive merkt weder Client noch Server, wenn ein
    ///      NAT-Gateway die stille Verbindung waehrend der Wartezeit auf die
    ///      Antwort wegwirft (Bug-Almanach apis/api-integration-general §E3,
    ///      dort fuer httpx beschrieben — die Ursache ist stack-unabhaengig).
    ///      Die Antwort kommt dann nie an, obwohl der Anbieter sie erzeugt hat.
    ///
    /// Beides wird hier zugenagelt: harter Verbindungs-Timeout und
    /// Keepalive-Proben auf jedem Socket. Eine haengende Verbindung faellt
    /// damit nach Sekunden auf, nicht nach Minuten — und der Router kann auf
    /// Groq ausweichen, solange Frank noch am Warten ist.
    /// </summary>
    internal static class ResilientHttp
    {
        /// <summary>Verbindungsaufbau (DNS + TCP + TLS) darf so lange dauern.</summary>
        private static readonly TimeSpan ConnectTimeout = TimeSpan.FromSeconds(10);

        /// <summary>Nach so vielen Sekunden Stille beginnen die Keepalive-Proben.</summary>
        private const int KeepAliveIdleSeconds = 20;

        /// <summary>Abstand zwischen zwei Proben.</summary>
        private const int KeepAliveIntervalSeconds = 5;

        /// <summary>So viele Proben ohne Antwort gelten als tote Verbindung (~45 s).</summary>
        private const int KeepAliveRetryCount = 5;

        /// <summary>
        /// Baut den Handler fuer einen langlebigen, geteilten HttpClient.
        /// <paramref name="pooledConnectionLifetime"/> bleibt Sache des
        /// Aufrufers, damit die bisherige DNS-Auffrischung unveraendert bleibt
        /// (Bug-Almanach §E2).
        /// </summary>
        public static SocketsHttpHandler CreateHandler(TimeSpan pooledConnectionLifetime)
        {
            return new SocketsHttpHandler
            {
                PooledConnectionLifetime = pooledConnectionLifetime,
                ConnectTimeout = ConnectTimeout,
                ConnectCallback = ConnectWithKeepAliveAsync,
            };
        }

        private static async ValueTask<Stream> ConnectWithKeepAliveAsync(
            SocketsHttpConnectionContext context, System.Threading.CancellationToken ct)
        {
            var socket = new Socket(SocketType.Stream, ProtocolType.Tcp) { NoDelay = true };
            ApplyKeepAlive(socket);
            try
            {
                await socket.ConnectAsync(context.DnsEndPoint, ct).ConfigureAwait(false);
                return new NetworkStream(socket, ownsSocket: true);
            }
            catch
            {
                socket.Dispose();
                throw;
            }
        }

        /// <summary>
        /// Keepalive ist eine Verbesserung, keine Voraussetzung: sollte eine
        /// der Optionen auf einer Windows-Fassung fehlen, wird ohne sie
        /// verbunden statt die Aufnahme an einer Socket-Option scheitern zu
        /// lassen. Der Verbindungs-Timeout greift dann immer noch.
        /// </summary>
        private static void ApplyKeepAlive(Socket socket)
        {
            try
            {
                socket.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.KeepAlive, true);
                socket.SetSocketOption(SocketOptionLevel.Tcp, SocketOptionName.TcpKeepAliveTime, KeepAliveIdleSeconds);
                socket.SetSocketOption(SocketOptionLevel.Tcp, SocketOptionName.TcpKeepAliveInterval, KeepAliveIntervalSeconds);
                socket.SetSocketOption(SocketOptionLevel.Tcp, SocketOptionName.TcpKeepAliveRetryCount, KeepAliveRetryCount);
            }
            catch (SocketException) { }
            catch (PlatformNotSupportedException) { }
            catch (ObjectDisposedException) { }
        }
    }
}
