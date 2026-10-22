using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Google.Apis.Auth.OAuth2;
using Google.Apis.Auth.OAuth2.Flows;
using Google.Apis.Download;
using Google.Apis.Drive.v3;
using Google.Apis.Http;
using Google.Apis.Services;
using Google.Apis.Util.Store;
using DriveFile = Google.Apis.Drive.v3.Data.File;

namespace ClaudeVoiceOverlay.Services
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
        /// Timeout for the OAuth token refresh. Before, the flow used Google's default
        /// HttpClient (100 s, no connect timeout) even though Drive calls were hardened —
        /// a dead network stalled every sync for 100 s at the refresh step.
        /// </summary>
        public static readonly TimeSpan TokenTimeout = TimeSpan.FromSeconds(20);

        /// <summary>
        /// OAuth flow for all five Drive services, with the same hardened handler as the
        /// Drive calls (connect timeout + keepalive) and a 20 s total timeout.
        /// </summary>
        public static GoogleAuthorizationCodeFlow CreateFlow(string clientId, string clientSecret, params string[] scopes)
        {
            var flow = new GoogleAuthorizationCodeFlow(new GoogleAuthorizationCodeFlow.Initializer
            {
                ClientSecrets = new ClientSecrets { ClientId = clientId, ClientSecret = clientSecret },
                Scopes = scopes,
                DataStore = new NullDataStore(),
                HttpClientFactory = new ResilientDriveHttpClientFactory(),
            });
            flow.HttpClient.Timeout = TokenTimeout;
            return flow;
        }

        /// <summary>
        /// Every non-trashed file with this name in the appDataFolder, newest first, with
        /// the metadata needed for a "did the cloud change since I read it?" check.
        /// </summary>
        public static async Task<List<DriveFile>> ListAppDataFilesAsync(
            DriveService drive, string fileName, CancellationToken ct)
        {
            var list = drive.Files.List();
            list.Spaces = "appDataFolder";
            list.Q = $"name = '{fileName}' and trashed = false";
            list.Fields = "files(id, modifiedTime, md5Checksum, headRevisionId)";
            list.OrderBy = "modifiedTime desc";
            list.PageSize = 100;
            var result = await list.ExecuteAsync(ct).ConfigureAwait(false);
            var files = new List<DriveFile>();
            if (result.Files != null)
            {
                foreach (var f in result.Files)
                    if (!string.IsNullOrEmpty(f.Id)) files.Add(f);
            }
            return files;
        }

        /// <summary>Compact version token of a listing (ids + modification + checksum).</summary>
        public static string VersionOf(IReadOnlyList<DriveFile> files)
        {
            var sb = new StringBuilder();
            foreach (var f in files)
                sb.Append(f.Id).Append('@').Append(f.ModifiedTimeRaw).Append('#')
                  .Append(f.Md5Checksum).Append('#').Append(f.HeadRevisionId).Append(';');
            return sb.ToString();
        }

        /// <summary>Downloads one appDataFolder file as UTF-8 text.</summary>
        public static async Task<string> DownloadTextAsync(DriveService drive, string fileId, CancellationToken ct)
        {
            using var buffer = new MemoryStream();
            var progress = await drive.Files.Get(fileId).DownloadAsync(buffer, ct).ConfigureAwait(false);
            progress.ThrowOnFailure();
            if (progress.Status != DownloadStatus.Completed)
                throw new IOException($"Google Drive download did not complete: {progress.Status}");
            return Encoding.UTF8.GetString(buffer.ToArray());
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
