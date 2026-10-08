using System.IO;
using System.Net;
using System.Net.Http;
using System.Net.Sockets;

namespace UpdateZentrale.Services;

/// <summary>
/// The one HttpClient for version lookups.
///
/// .NET connects to the addresses of a host in DNS order and without a time limit per address.
/// On a network that hands out IPv6 addresses but routes none of them (measured here: curl -6
/// never connects, curl -4 answers in 0.5 s) the first IPv6 attempt eats the whole request
/// timeout -- every registry lookup failed after exactly 30 s. So the connect is done by hand:
/// IPv4 first, each address bounded, the next one tried on failure.
/// </summary>
public static class Netzdienst
{
    private static readonly TimeSpan JeAdresse = TimeSpan.FromSeconds(4);

    public static HttpClient Client { get; } = new(new SocketsHttpHandler
    {
        ConnectCallback = VerbindenAsync,
        PooledConnectionLifetime = TimeSpan.FromMinutes(5)
    })
    {
        Timeout = TimeSpan.FromSeconds(15)
    };

    private static async ValueTask<Stream> VerbindenAsync(SocketsHttpConnectionContext kontext, CancellationToken abbruch)
    {
        var ziel = kontext.DnsEndPoint;
        var adressen = (await Dns.GetHostAddressesAsync(ziel.Host, abbruch))
            .OrderBy(a => a.AddressFamily == AddressFamily.InterNetwork ? 0 : 1)
            .Take(6)
            .ToList();

        Exception? letzter = null;
        foreach (var adresse in adressen)
        {
            var socket = new Socket(adresse.AddressFamily, SocketType.Stream, ProtocolType.Tcp) { NoDelay = true };
            try
            {
                using var frist = CancellationTokenSource.CreateLinkedTokenSource(abbruch);
                frist.CancelAfter(JeAdresse);
                await socket.ConnectAsync(new IPEndPoint(adresse, ziel.Port), frist.Token);
                return new NetworkStream(socket, ownsSocket: true);
            }
            catch (Exception ex)
            {
                socket.Dispose();
                if (abbruch.IsCancellationRequested) throw;
                letzter = ex;   // this address is unreachable -- the next one may not be
            }
        }

        throw new HttpRequestException(
            "Keine Verbindung zu " + ziel.Host + " (" + adressen.Count + " Adresse(n) versucht).", letzter);
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
