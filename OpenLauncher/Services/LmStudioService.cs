using System.Diagnostics;
using System.IO;
using System.Net.Http;
using System.Text.Json;
using OpenLauncher.Models;

namespace OpenLauncher.Services;

/// <summary>
/// Liest die lokal in LM Studio vorhandenen Modelle über den OpenAI-kompatiblen Server
/// (GET http://localhost:1234/v1/models) und startet den Server bei Bedarf über die
/// mitgelieferte lms-CLI. LM Studio lädt ein angefragtes Modell per JIT-Loading selbst,
/// deshalb reicht es, den Server laufen zu haben — OpenCode spricht ihn dann direkt an.
/// </summary>
public sealed class LmStudioService
{
    public const string ProviderId = "lmstudio";
    public const string ProviderName = "LM Studio";
    public const string BaseUrl = "http://localhost:1234/v1";

    /// <summary>
    /// Kleinster Kontext, mit dem OpenCode überhaupt arbeiten kann. Der Systemprompt allein
    /// braucht mit abgeschalteten externen Skills rund 7500 Tokens; darunter bricht die Anfrage mit
    /// exceed_context_size_error ab.
    /// <para>
    /// Reine WARNSCHWELLE und Rückfallwert beim Laden — kein Zwang: ein Modell, das in LM Studio
    /// bereits mit weniger geladen ist, wird nicht angetastet, sondern nur mit einem Hinweis
    /// übernommen.
    /// </para>
    /// </summary>
    public const int MinimumAgentContext = 16_384;

    /// <summary>Wunschgröße beim automatischen Laden. Wird auf den Maximalkontext gedeckelt.</summary>
    public const int PreferredContext = 32_768;

    private static readonly HttpClient Http = new() { Timeout = TimeSpan.FromSeconds(5) };

    private static string LmsPath => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
        ".lmstudio", "bin", "lms.exe");

    /// <summary>Ist die lms-CLI (und damit LM Studio) auf diesem Rechner installiert?</summary>
    public static bool IsInstalled => File.Exists(LmsPath);

    /// <summary>
    /// Startet den lokalen LM-Studio-Server, falls er nicht schon läuft. Idempotent —
    /// "lms server start" meldet bei laufendem Server nur Erfolg.
    /// </summary>
    public static bool EnsureServerRunning()
    {
        if (!IsInstalled) return false;
        try
        {
            using var proc = Process.Start(new ProcessStartInfo
            {
                FileName = LmsPath,
                Arguments = "server start",
                CreateNoWindow = true,
                UseShellExecute = false,
                RedirectStandardOutput = true,
                RedirectStandardError = true
            });
            if (proc == null) return false;
            proc.WaitForExit(20_000);
            Logger.Instance.Info("LmStudioService", "EnsureServerRunning", $"lms server start beendet mit {proc.ExitCode}");
            return proc.ExitCode == 0;
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("LmStudioService", "EnsureServerRunning", $"LM-Studio-Server nicht startbar: {ex.Message}");
            return false;
        }
    }

    /// <summary>
    /// Kontextlaenge, mit der das Modell in LM Studio gerade geladen ist — also exakt die
    /// Einstellung, die der Benutzer dort gewaehlt hat. 0 bedeutet: nicht geladen. OpenCode
    /// rechnet seine Auslastung gegen diesen Wert; steht in der Konfig eine kleinere Zahl,
    /// meldet die Oberflaeche viel zu frueh einen vollen Kontext und komprimiert endlos.
    /// </summary>
    public static int GetLoadedContextLength(string modelId)
    {
        if (!IsInstalled || string.IsNullOrWhiteSpace(modelId)) return 0;
        try
        {
            using var proc = Process.Start(new ProcessStartInfo
            {
                FileName = LmsPath,
                Arguments = "ps --json",
                CreateNoWindow = true,
                UseShellExecute = false,
                RedirectStandardOutput = true,
                RedirectStandardError = true
            });
            if (proc == null) return 0;
            var json = proc.StandardOutput.ReadToEnd();
            proc.WaitForExit(30_000);
            if (string.IsNullOrWhiteSpace(json)) return 0;

            using var doc = JsonDocument.Parse(json);
            if (doc.RootElement.ValueKind != JsonValueKind.Array) return 0;
            foreach (var item in doc.RootElement.EnumerateArray())
            {
                var identifier = item.TryGetProperty("identifier", out var idNode) ? idNode.GetString() : null;
                var modelKey = item.TryGetProperty("modelKey", out var keyNode) ? keyNode.GetString() : null;
                if (!string.Equals(identifier, modelId, StringComparison.OrdinalIgnoreCase) &&
                    !string.Equals(modelKey, modelId, StringComparison.OrdinalIgnoreCase)) continue;
                return item.TryGetProperty("contextLength", out var ctx) && ctx.TryGetInt32(out var value) ? value : 0;
            }
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("LmStudioService", "GetLoadedContextLength", $"lms ps nicht lesbar: {ex.Message}");
        }
        return 0;
    }

    /// <summary>
    /// Ausgabe-Obergrenze. OpenCode reserviert diesen Wert im Kontextbudget und zeigt ihn als
    /// bereits verbraucht an — eine grosszuegige Obergrenze frisst also sichtbar Kontext, ohne
    /// dass ein einziges Token geschrieben waere. 8192 reicht fuer jede realistische Antwort
    /// inklusive Werkzeugaufrufen; bei kleinen Fenstern wird anteilig gedeckelt.
    /// </summary>
    public static int OutputLimitFor(int contextLength) =>
        Math.Clamp(Math.Min(8_192, contextLength / 8), 2_048, 8_192);

    /// <summary>
    /// Holt die Modell-IDs vom laufenden Server. Embedding-Modelle werden aussortiert, sie
    /// taugen nicht als Chat-Modell für OpenCode. Leere Liste = Server aus oder keine Modelle.
    /// </summary>
    public async Task<List<ModelEntry>> GetLocalModelsAsync(CancellationToken ct = default)
    {
        var result = new List<ModelEntry>();
        try
        {
            using var resp = await Http.GetAsync($"{BaseUrl}/models", ct).ConfigureAwait(false);
            if (!resp.IsSuccessStatusCode) return result;

            var json = await resp.Content.ReadAsStringAsync(ct).ConfigureAwait(false);
            using var doc = JsonDocument.Parse(json);
            if (!doc.RootElement.TryGetProperty("data", out var data) || data.ValueKind != JsonValueKind.Array)
                return result;

            foreach (var item in data.EnumerateArray())
            {
                if (!item.TryGetProperty("id", out var idNode)) continue;
                var id = idNode.GetString();
                if (string.IsNullOrWhiteSpace(id)) continue;
                if (id.Contains("embed", StringComparison.OrdinalIgnoreCase)) continue;

                result.Add(new ModelEntry
                {
                    Slug = id,
                    DisplayName = BuildDisplayName(id),
                    ProviderId = ProviderId,
                    ProviderName = ProviderName
                });
            }
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("LmStudioService", "GetLocalModelsAsync", $"LM-Studio-Modelle nicht lesbar: {ex.Message}");
        }
        return result;
    }

    /// <summary>
    /// Startet bei Bedarf den Server und liest danach die Modelle. Der Serverstart passiert nur,
    /// wenn die erste Abfrage ins Leere läuft — so bleibt der Programmstart schnell.
    /// </summary>
    public async Task<List<ModelEntry>> GetLocalModelsWithServerAsync(CancellationToken ct = default)
    {
        var models = await GetLocalModelsAsync(ct).ConfigureAwait(false);
        if (models.Count > 0 || !IsInstalled) return models;

        await Task.Run(() => EnsureServerRunning(), ct).ConfigureAwait(false);

        // "lms server start" kehrt zurück, bevor der HTTP-Endpunkt Anfragen annimmt. Ohne
        // Wiederholversuche liefert die nächste Abfrage deshalb eine leere Liste — der Reiter
        // "LM Studio" bleibt dann leer, obwohl LM Studio läuft.
        for (var attempt = 0; attempt < 4; attempt++)
        {
            models = await GetLocalModelsAsync(ct).ConfigureAwait(false);
            if (models.Count > 0) return models;
            if (attempt < 3) await Task.Delay(1500, ct).ConfigureAwait(false);
        }
        Logger.Instance.Warn("LmStudioService", "GetLocalModelsWithServerAsync",
            "Server gestartet, liefert aber keine Modelle");
        return models;
    }

    /// <summary>"mistralai/devstral-small-2-2512" -> "Devstral Small 2 2512 (lokal)".</summary>
    private static string BuildDisplayName(string id)
    {
        var name = id.Split('/').Last();
        var pretty = string.Join(" ", name
            .Split('-', StringSplitOptions.RemoveEmptyEntries)
            .Select(part => part.Length == 0 ? part : char.ToUpperInvariant(part[0]) + part[1..]));
        return $"{pretty} (lokal)";
    }
}
