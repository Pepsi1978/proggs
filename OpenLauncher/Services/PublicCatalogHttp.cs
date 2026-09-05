using System.Diagnostics;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Text.Json;

namespace OpenLauncher.Services;

// Public GET only: never pass authentication or user-supplied URLs to the native fallback.
public static class PublicCatalogHttp
{
    private static readonly HttpClient Http = new(new SocketsHttpHandler
    {
        AutomaticDecompression = DecompressionMethods.All,
        ConnectTimeout = TimeSpan.FromSeconds(5),
        PooledConnectionLifetime = TimeSpan.FromMinutes(3)
    }) { Timeout = TimeSpan.FromSeconds(8) };
    private static readonly object Gate = new();
    private static readonly Dictionary<string, (Task<string> Task, DateTimeOffset At)> Requests = new();

    public static Task<string> GetAsync(string url, CancellationToken ct, bool forceRefresh = false)
    {
        if (url is not ("https://models.dev/api.json" or "https://openrouter.ai/api/v1/models"))
            throw new ArgumentException("Unbekannter öffentlicher Katalog.", nameof(url));
        lock (Gate)
        {
            if (Requests.TryGetValue(url, out var entry) && (!entry.Task.IsCompleted ||
                (!forceRefresh && DateTimeOffset.UtcNow - entry.At <
                    (entry.Task.IsFaulted ? TimeSpan.FromMinutes(1) : TimeSpan.FromMinutes(10)))))
                return entry.Task.WaitAsync(ct);
            // A model switch cancels only its waiter, not the download shared by all models.
            var task = DownloadAsync(url);
            Requests[url] = (task, DateTimeOffset.UtcNow);
            return task.WaitAsync(ct);
        }
    }

    private static async Task<string> DownloadAsync(string url)
    {
        try
        {
            var json = await Http.GetStringAsync(url).ConfigureAwait(false);
            Validate(json);
            return json;
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn(nameof(PublicCatalogHttp), "HttpClient", $"{url}: {ex.GetType().Name}: {ex.Message}; nativer HTTPS-Fallback folgt.");
        }

        using var timeout = new CancellationTokenSource(TimeSpan.FromSeconds(25));
        var start = new ProcessStartInfo(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.System), "curl.exe"))
        {
            UseShellExecute = false, CreateNoWindow = true,
            RedirectStandardOutput = true, RedirectStandardError = true
        };
        foreach (var arg in new[] { "--disable", "--fail", "--silent", "--show-error", "--compressed",
                     "--location", "--proto", "=https", "--proto-redir", "=https",
                     "--connect-timeout", "6", "--max-time", "20", "--max-filesize", "16777216", url })
            start.ArgumentList.Add(arg);
        using var process = Process.Start(start) ?? throw new IOException("HTTPS-Fallback konnte nicht starten.");
        var output = process.StandardOutput.ReadToEndAsync(timeout.Token);
        var errors = process.StandardError.ReadToEndAsync(timeout.Token);
        try
        {
            await process.WaitForExitAsync(timeout.Token).ConfigureAwait(false);
            var json = await output.ConfigureAwait(false);
            var error = await errors.ConfigureAwait(false);
            if (process.ExitCode != 0) throw new HttpRequestException($"Nativer Katalogabruf: Exit {process.ExitCode}: {error.Trim()}");
            Validate(json);
            Logger.Instance.Info(nameof(PublicCatalogHttp), "NativeHttps", $"Katalog über nativen HTTPS-Zugang geladen: {url}");
            return json;
        }
        finally
        {
            if (!process.HasExited) process.Kill(entireProcessTree: true);
        }
    }

    private static void Validate(string json)
    {
        if (json.Length > 16_777_216) throw new JsonException("Katalog zu groß.");
        using var parsed = JsonDocument.Parse(json);
        if (parsed.RootElement.ValueKind != JsonValueKind.Object) throw new JsonException("Katalog ist kein JSON-Objekt.");
    }
}
