using System;
using System.Net;
using System.Net.Http;
using Google.Apis.Auth.OAuth2;
using Google.Apis.Drive.v3;
using Google.Apis.Http;
using Google.Apis.Services;

namespace TerminalVoiceOverlay.Services
{
    /// <summary>
    /// Eine Fabrik fuer ALLE Drive-Dienste des Overlays (Prompt-Sync, Slots,
    /// Historie, Woerterbuch, Backup).
    ///
    /// WARUM (Vorfall 03.09.2026): Sieben Mal im Diag-Log "cloud sync failed:
    /// The request was canceled due to the configured HttpClient.Timeout of
    /// 100 seconds elapsing" — der Google-Client kam ohne Verbindungs-Timeout
    /// und ohne Keepalive, also mit genau den zwei Luecken, die
    /// <see cref="ResilientHttp"/> fuer Gemini und Groq schon zugenagelt hat.
    /// Jeder haengende Aufruf blockierte 100 s, Backup und Sync scheiterten
    /// still. Hier bekommen alle fuenf Dienste denselben gehaerteten Handler
    /// und einen kurzen Gesamt-Timeout.
    /// </summary>
    internal static class DriveHttp
    {
        /// <summary>
        /// Gesamt-Timeout je Request. Die Nutzdaten sind klein (Prompt-JSON,
        /// Slots, Historie, Woerterbuch, Backup unter 1 MB) und laufen in
        /// Sekunden. 30 s deckt langsame Leitungen ab und laesst einen
        /// haengenden Aufruf nach einer halben Minute statt nach 100 s auffallen.
        /// </summary>
        public static readonly TimeSpan RequestTimeout = TimeSpan.FromSeconds(30);

        public static DriveService CreateService(IConfigurableHttpClientInitializer cred)
        {
            var service = new DriveService(new BaseClientService.Initializer
            {
                HttpClientInitializer = cred,
                ApplicationName = "PromptBoard",
                HttpClientFactory = new ResilientDriveHttpClientFactory(),
            });
            service.HttpClient.Timeout = RequestTimeout;
            return service;
        }

        /// <summary>
        /// Google baut seinen Client ueber diese Fabrik. Wir tauschen nur den
        /// innersten Handler gegen den SocketsHttpHandler aus
        /// <see cref="ResilientHttp"/> (Verbindungs-Timeout + Keepalive) und
        /// behalten Googles Vorgaben: keine eigenen Redirects und Cookies
        /// (ConfigurableMessageHandler macht das selbst), Dekomprimierung nur
        /// wenn Google sie will.
        /// </summary>
        private sealed class ResilientDriveHttpClientFactory : HttpClientFactory
        {
            protected override HttpMessageHandler CreateHandler(CreateHttpClientArgs args)
            {
                var handler = ResilientHttp.CreateHandler(TimeSpan.FromMinutes(10));
                handler.AllowAutoRedirect = false;
                handler.UseCookies = false;
                handler.AutomaticDecompression = args.GZipEnabled
                    ? DecompressionMethods.GZip | DecompressionMethods.Deflate
                    : DecompressionMethods.None;
                return handler;
            }
        }
    }
}
