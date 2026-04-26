using System;
using System.Threading;
using System.Threading.Tasks;

namespace PromptBoard.Core.Services;

/// <summary>
/// Calls the Google Gemini generateContent endpoint to rewrite/improve
/// a user prompt according to a given meta-prompt (system instruction).
/// Model is chosen per call so different AiImprovementPrompts can use
/// different models.
/// </summary>
public interface IGeminiService
{
    /// <summary>
    /// Rewrite <paramref name="userPrompt"/> according to the meta-prompt
    /// (treated as system instruction) using the given Gemini model.
    /// </summary>
    /// <exception cref="GeminiApiKeyMissingException">
    /// Thrown when no API key is configured.
    /// </exception>
    /// <exception cref="GeminiServiceException">
    /// Thrown on non-success HTTP status or a malformed response body.
    /// </exception>
    Task<string> ImproveAsync(
        string userPrompt,
        string metaPrompt,
        string model,
        CancellationToken ct = default);
}

public sealed class GeminiApiKeyMissingException : Exception
{
    public GeminiApiKeyMissingException()
        : base("Kein Gemini-API-Key gesetzt. Bitte im Settings-Dialog eintragen.") { }
}

public sealed class GeminiServiceException : Exception
{
    public int? StatusCode { get; }

    public GeminiServiceException(string message, int? statusCode = null, Exception? inner = null)
        : base(message, inner)
    {
        StatusCode = statusCode;
    }
}
