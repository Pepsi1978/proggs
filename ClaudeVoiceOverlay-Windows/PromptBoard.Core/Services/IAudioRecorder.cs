using System.Threading;
using System.Threading.Tasks;

namespace PromptBoard.Core.Services;

/// <summary>
/// Captures microphone audio as a 16 kHz mono WAV stream. Push-to-talk
/// style: caller invokes StartAsync, later calls StopAsync to retrieve
/// the recorded bytes. Only one recording may run at a time.
/// </summary>
public interface IAudioRecorder
{
    /// <summary>True while a recording session is active.</summary>
    bool IsRecording { get; }

    /// <summary>Begin capturing audio. Throws if already recording.</summary>
    Task StartAsync(CancellationToken ct = default);

    /// <summary>
    /// Stop capturing and return the captured audio as a complete WAV
    /// file (with RIFF header), ready to upload to a transcription API.
    /// Returns an empty array if no recording was active.
    /// </summary>
    Task<byte[]> StopAsync(CancellationToken ct = default);
}
