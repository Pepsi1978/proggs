using System.Text.Json.Serialization;

namespace OpenCodeLauncher.Models;

/// <summary>
/// Ein Provider-Angebot für ein Modell (ein Eintrag aus
/// GET /api/v1/models/{author}/{slug}/endpoints -> data.endpoints[]).
/// Preise liegen als USD/Token (String) vor und werden für die Anzeige in
/// USD / 1M Tokens umgerechnet.
/// </summary>
public sealed class ProviderEntry
{
    public string ProviderName { get; set; } = string.Empty;
    public string Tag { get; set; } = string.Empty;

    /// <summary>OpenRouter-Provider-Slug (lowercase) für provider.order, abgeleitet aus provider_name.</summary>
    public string ProviderSlug { get; set; } = string.Empty;

    public double PromptPerToken { get; set; }
    public double CompletionPerToken { get; set; }
    public double? Discount { get; set; }

    public int ContextLength { get; set; }
    public int? MaxCompletionTokens { get; set; }
    public string Quantization { get; set; } = string.Empty;

    public double? ThroughputLast30m { get; set; }
    public double? UptimeLast5m { get; set; }
    public int Status { get; set; }

    [JsonIgnore]
    public double InputPerMillion => PromptPerToken * 1_000_000.0;

    [JsonIgnore]
    public double OutputPerMillion => CompletionPerToken * 1_000_000.0;

    [JsonIgnore]
    public string StatusText => Status switch
    {
        0 => "ok",
        -2 => "eingeschränkt",
        -5 => "gestört",
        _ => $"Status {Status}"
    };

    [JsonIgnore]
    public bool IsUp => Status >= 0 && !(UptimeLast5m.HasValue && UptimeLast5m < 80.0);
}
