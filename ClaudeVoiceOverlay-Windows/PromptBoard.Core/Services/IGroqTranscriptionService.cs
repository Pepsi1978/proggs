using System;
using System.Threading;
using System.Threading.Tasks;

namespace PromptBoard.Core.Services;

/// <summary>
/// Sends a WAV audio blob to Groq's Whisper endpoint and returns the
/// transcribed text. The implementation reads the API key and model
/// from the app settings each call so the user can swap them at runtime.
/// </summary>
public interface IGroqTranscriptionService
{
    /// <summary>
    /// Transcribe <paramref name="wavBytes"/> using the configured model.
    /// </summary>
    /// <exception cref="GroqApiKeyMissingException">
    /// Thrown when no API key is configured — the UI should prompt the
    /// user to open Settings.
    /// </exception>
    /// <exception cref="GroqTranscriptionException">
    /// Thrown when Groq returns a non-success status or an unexpected body.
    /// </exception>
    Task<string> TranscribeAsync(byte[] wavBytes, CancellationToken ct = default);
}

/// <summary>Raised when the Groq API key is missing from settings.</summary>
public sealed class GroqApiKeyMissingException : Exception
{
    public GroqApiKeyMissingException()
        : base("Kein Groq-API-Key gesetzt. Bitte im Settings-Dialog eintragen.") { }
}

/// <summary>General transcription failure (HTTP error, malformed response, etc.).</summary>
public sealed class GroqTranscriptionException : Exception
{
    public int? StatusCode { get; }

    public GroqTranscriptionException(string message, int? statusCode = null, Exception? inner = null)
        : base(message, inner)
    {
        StatusCode = statusCode;
    }
}
