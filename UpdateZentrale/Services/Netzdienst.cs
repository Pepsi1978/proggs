using System.IO;
using System.Net;
using System.Net.Http;
using System.Net.Sockets;

namespace UpdateZentrale.Services;

/// <summary>
/// The one HttpClient for version lookups.
///
/// .NET connects to the addresses of a host in DNS order and without a time limit per address.
/// Two kinds of dead addresses have been measured here:
/// - a network that hands out IPv6 addresses but routes none of them (curl -6 never connects),
/// - a filtering resolver on the WLAN (156.154.70.25, UltraDNS) that answers dl.deno.land with
///   sinkhole IPv4 addresses that never accept a connection, while the LAN resolver answers right.
/// Windows picks whichever resolver answers, and an unspecified-family lookup and an IPv4-only
/// lookup can come back from different ones. So the connect is done by hand: addresses from all
/// three lookups, attempts started staggered and run in parallel (the first connection wins), and
/// if none connects, the host is resolved once more over DNS-over-HTTPS (1.1.1.1) as a fallback.
/// </summary>
public static class Netzdienst
{
    private static readonly TimeSpan JeAdresse = TimeSpan.FromSeconds(4);
    private static readonly TimeSpan Staffel = TimeSpan.FromMilliseconds(250);
    /// <summary>Generous: the Windows DNS client itself retries for several seconds on a lossy WLAN.</summary>
    private static readonly TimeSpan DnsFrist = TimeSpan.FromSeconds(8);

    /// <summary>Once one lookup has answered, the others get only this much longer.</summary>
    private static readonly TimeSpan DnsNachzug = TimeSpan.FromMilliseconds(300);

    private const int HoechstensAdressen = 8;

    /// <summary>
    /// Above the worst case of one connect plus TLS and response: DNS 8 s + race ~6 s + DoH 3 s
    /// (both servers in parallel) + race ~6 s = ~23 s. The working path stays at well under a second.
    /// </summary>
    private static readonly TimeSpan Gesamtfrist = TimeSpan.FromSeconds(30);

    public static HttpClient Client { get; } = Erzeugen();

    /// <summary>Talks to an IP literal, so it never needs the system resolver it stands in for.</summary>
    private static readonly HttpClient DohClient = new(new SocketsHttpHandler
    {
        PooledConnectionLifetime = TimeSpan.FromMinutes(5)
    })
    {
        Timeout = TimeSpan.FromSeconds(3)
    };

    private static readonly string[] DohServer = ["1.1.1.1", "1.0.0.1"];

    /// <summary>GitHub's API (Kotlin, uv releases) refuses requests without a User-Agent.</summary>
    private static HttpClient Erzeugen()
    {
        var client = new HttpClient(new SocketsHttpHandler
        {
            ConnectCallback = VerbindenAsync,
            PooledConnectionLifetime = TimeSpan.FromMinutes(5)
        })
        {
            Timeout = Gesamtfrist
        };
        client.DefaultRequestHeaders.UserAgent.ParseAdd("UpdateZentrale/1.0");
        return client;
    }

    private static async ValueTask<Stream> VerbindenAsync(SocketsHttpConnectionContext kontext, CancellationToken abbruch)
    {
        var ziel = kontext.DnsEndPoint;
        var adressen = await AdressenAsync(ziel.Host, abbruch);
        Socket socket;
        try
        {
            socket = await WettlaufAsync(adressen, (a, t) => SocketVerbindenAsync(a, ziel.Port, t), JeAdresse, Staffel, abbruch);
        }
        catch (Exception ex) when (!abbruch.IsCancellationRequested)
        {
            // Every address the system resolver gave is dead (or there were none): ask DoH. Also on
            // "host not found" -- a filtering resolver may block a name that way, too. Only hosts of
            // the catalog's version URLs come through here, and TLS still checks the certificate.
            var ausweich = (await DohAsync(ziel.Host, abbruch)).Except(adressen).Take(HoechstensAdressen).ToList();
            if (ausweich.Count == 0)
                throw new HttpRequestException(KeineVerbindung(ziel.Host, adressen), ex);
            Diagnose.Ereignis(Schwere.Info, "netz", "netz.doh",
                ziel.Host + ": keine Systemadresse erreichbar (" + string.Join(", ", adressen)
                + "), Ausweichweg DNS-over-HTTPS liefert " + string.Join(", ", ausweich) + ".");
            try
            {
                socket = await WettlaufAsync(ausweich, (a, t) => SocketVerbindenAsync(a, ziel.Port, t), JeAdresse, Staffel, abbruch);
            }
            catch (Exception ex2) when (!abbruch.IsCancellationRequested)
            {
                throw new HttpRequestException(KeineVerbindung(ziel.Host, adressen.Concat(ausweich).ToList()), ex2);
            }
        }
        return new NetworkStream(socket, ownsSocket: true);
    }

    private static string KeineVerbindung(string host, IReadOnlyCollection<IPAddress> versucht)
        => versucht.Count == 0
            ? "Der Name " + host + " ließ sich nicht auflösen (System-DNS und DNS-over-HTTPS)."
            : "Keine Verbindung zu " + host + " (versucht: " + string.Join(", ", versucht) + ").";

    private static async Task<Socket> SocketVerbindenAsync(IPAddress adresse, int port, CancellationToken abbruch)
    {
        var socket = new Socket(adresse.AddressFamily, SocketType.Stream, ProtocolType.Tcp) { NoDelay = true };
        try
        {
            await socket.ConnectAsync(new IPEndPoint(adresse, port), abbruch);
            return socket;
        }
        catch
        {
            socket.Dispose();
            throw;
        }
    }

    /// <summary>
    /// Addresses from an unspecified, an IPv4-only and an IPv6-only lookup. They can come from
    /// different resolvers; one failing or hanging lookup must not cost the others.
    /// </summary>
    internal static async Task<List<IPAddress>> AdressenAsync(string host, CancellationToken abbruch)
    {
        if (IPAddress.TryParse(host, out var literal)) return [literal];

        using var frist = CancellationTokenSource.CreateLinkedTokenSource(abbruch);
        frist.CancelAfter(DnsFrist);

        async Task<IPAddress[]> Abfrage(AddressFamily familie)
        {
            try
            {
                return await Dns.GetHostAddressesAsync(host, familie, frist.Token);
            }
            catch (Exception) when (!abbruch.IsCancellationRequested)
            {
                return [];
            }
        }

        var abfragen = new List<Task<IPAddress[]>>
        {
            Abfrage(AddressFamily.InterNetwork), Abfrage(AddressFamily.Unspecified), Abfrage(AddressFamily.InterNetworkV6)
        };

        // Wait for the first lookup with addresses; a dropped AAAA query must not hold up the connect.
        var offen = new List<Task<IPAddress[]>>(abfragen);
        while (offen.Count > 0)
        {
            var fertig = await Task.WhenAny(offen);
            abbruch.ThrowIfCancellationRequested();
            offen.Remove(fertig);
            if (fertig.Result.Length > 0) break;
        }
        if (offen.Count > 0)
            await Task.WhenAny(Task.WhenAll(offen), Task.Delay(DnsNachzug, abbruch));
        abbruch.ThrowIfCancellationRequested();
        frist.Cancel();   // late lookups are no longer awaited

        return Ordnen(abfragen.Where(t => t.Status == TaskStatus.RanToCompletion).SelectMany(t => t.Result));
    }

    /// <summary>IPv4 first (IPv6 is often handed out but not routed), duplicates removed, bounded.</summary>
    internal static List<IPAddress> Ordnen(IEnumerable<IPAddress> adressen)
        => adressen.Distinct()
            .OrderBy(a => a.AddressFamily == AddressFamily.InterNetwork ? 0 : 1)
            .Take(HoechstensAdressen)
            .ToList();

    /// <summary>
    /// Starts one attempt per address, the next one after <paramref name="staffel"/> or as soon as
    /// an attempt fails; the first success wins. Late successes of losers are disposed.
    /// </summary>
    /// <param name="verbinden">Injectable so the race is testable without sockets.</param>
    internal static async Task<T> WettlaufAsync<T>(IReadOnlyList<IPAddress> adressen,
                                                   Func<IPAddress, CancellationToken, Task<T>> verbinden,
                                                   TimeSpan jeAdresse, TimeSpan staffel, CancellationToken abbruch)
        where T : class, IDisposable
    {
        using var rest = CancellationTokenSource.CreateLinkedTokenSource(abbruch);
        var laufend = new List<Task<T>>();
        var fehler = new List<Exception>();
        var naechste = 0;

        async Task<T> Versuch(IPAddress adresse)
        {
            using var frist = CancellationTokenSource.CreateLinkedTokenSource(rest.Token);
            frist.CancelAfter(jeAdresse);
            return await verbinden(adresse, frist.Token);
        }

        // Stays infinite while no further address is waiting; never cancelled itself, the loop ends first.
        Task takt = Task.Delay(Timeout.Infinite, rest.Token);
        void NaechsteStarten()
        {
            laufend.Add(Versuch(adressen[naechste++]));
            takt = naechste < adressen.Count ? Task.Delay(staffel, rest.Token) : Task.Delay(Timeout.Infinite, rest.Token);
        }

        try
        {
            if (adressen.Count > 0) NaechsteStarten();
            while (laufend.Count > 0)
            {
                var fertig = await Task.WhenAny(laufend.Append<Task>(takt));
                abbruch.ThrowIfCancellationRequested();
                if (fertig == takt)
                {
                    NaechsteStarten();
                    continue;
                }

                var versuch = (Task<T>)fertig;
                laufend.Remove(versuch);
                if (versuch.Status == TaskStatus.RanToCompletion) return versuch.Result;
                fehler.Add(versuch.Exception?.GetBaseException()
                           ?? new TimeoutException("Keine Antwort innerhalb von " + jeAdresse.TotalSeconds + " s."));
                if (naechste < adressen.Count) NaechsteStarten();   // a failure starts the next one at once
            }
        }
        finally
        {
            rest.Cancel();
            // Dispose late winners and observe late failures (else they surface as unobserved exceptions).
            foreach (var verlierer in laufend)
                _ = verlierer.ContinueWith(t =>
                {
                    if (t.Status == TaskStatus.RanToCompletion) t.Result.Dispose();
                    else _ = t.Exception;
                }, CancellationToken.None, TaskContinuationOptions.None, TaskScheduler.Default);
        }

        abbruch.ThrowIfCancellationRequested();
        throw new AggregateException(adressen.Count == 0 ? "Keine Adresse." : "Keine Adresse antwortete.", fehler);
    }

    /// <summary>
    /// IPv4 addresses of a host from Cloudflare's DNS-over-HTTPS JSON API, both servers asked in
    /// parallel (the first answer with addresses wins); empty on any failure.
    /// </summary>
    internal static async Task<List<IPAddress>> DohAsync(string host, CancellationToken abbruch)
    {
        using var rest = CancellationTokenSource.CreateLinkedTokenSource(abbruch);

        async Task<List<IPAddress>> Fragen(string server)
        {
            try
            {
                using var anfrage = new HttpRequestMessage(HttpMethod.Get,
                    $"https://{server}/dns-query?name={Uri.EscapeDataString(host)}&type=A");
                anfrage.Headers.Accept.ParseAdd("application/dns-json");
                using var antwort = await DohClient.SendAsync(anfrage, rest.Token);
                return antwort.IsSuccessStatusCode
                    ? DohAuswerten(await antwort.Content.ReadAsStringAsync(rest.Token))
                    : [];
            }
            catch (Exception ex) when (!rest.IsCancellationRequested)
            {
                Diagnose.Gefangen(new HttpRequestException("DNS-over-HTTPS " + server + " für " + host, ex), "netz", Schwere.Warnung);
                return [];
            }
            catch (Exception) when (!abbruch.IsCancellationRequested)
            {
                return [];   // the other server already answered
            }
        }

        var offen = DohServer.Select(Fragen).ToList();
        try
        {
            while (offen.Count > 0)
            {
                var fertig = await Task.WhenAny(offen);
                abbruch.ThrowIfCancellationRequested();
                offen.Remove(fertig);
                if (fertig.Result.Count > 0) return fertig.Result;
            }
            return [];
        }
        finally
        {
            rest.Cancel();
        }
    }

    /// <summary>The A records (type 1) of a DoH JSON answer; CNAME steps are skipped.</summary>
    internal static List<IPAddress> DohAuswerten(string json)
    {
        var ergebnis = new List<IPAddress>();
        try
        {
            using var dokument = System.Text.Json.JsonDocument.Parse(json);
            if (!dokument.RootElement.TryGetProperty("Answer", out var antworten)
                || antworten.ValueKind != System.Text.Json.JsonValueKind.Array)
                return ergebnis;
            foreach (var eintrag in antworten.EnumerateArray())
            {
                if (eintrag.TryGetProperty("type", out var typ) && typ.ValueKind == System.Text.Json.JsonValueKind.Number
                    && typ.GetInt32() == 1
                    && eintrag.TryGetProperty("data", out var daten) && daten.ValueKind == System.Text.Json.JsonValueKind.String
                    && IPAddress.TryParse(daten.GetString(), out var adresse)
                    && adresse.AddressFamily == AddressFamily.InterNetwork)
                    ergebnis.Add(adresse);
            }
        }
        catch (System.Text.Json.JsonException)
        {
            // not a DoH answer -- no fallback addresses
        }
        return ergebnis.Distinct().ToList();
    }

    /// <summary>
    /// One repeat for a GET that failed on the transport: a single dropped connection must not
    /// turn a card red. An HTTP status is an answer and is returned as it is.
    /// </summary>
    public static async Task<HttpResponseMessage> HolenAsync(HttpClient netz, string adresse, CancellationToken abbruch)
    {
        try
        {
            return await netz.GetAsync(adresse, abbruch);
        }
        catch (Exception ex) when (!abbruch.IsCancellationRequested)
        {
            Diagnose.Gefangen(ex, "netz", Schwere.Warnung);
            await Task.Delay(TimeSpan.FromMilliseconds(600), abbruch);
            return await netz.GetAsync(adresse, abbruch);
        }
    }
}
