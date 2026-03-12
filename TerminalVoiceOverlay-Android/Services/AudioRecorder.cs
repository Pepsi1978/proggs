using Android.Media;

namespace TerminalVoiceOverlay.Services;

public sealed class AudioRecorder : IDisposable
{
    private const int SampleRate = 16000;
    private const ChannelIn ChannelConfig = ChannelIn.Mono;
    private const Android.Media.Encoding AudioEncoding = Android.Media.Encoding.Pcm16bit;

    private AudioRecord? _audioRecord;
    private Thread? _recordingThread;
    private string? _tempFile;
    private volatile bool _isRecording;

    public bool IsRecording => _isRecording;

    public void Start()
    {
        if (_isRecording) return;

        var bufferSize = AudioRecord.GetMinBufferSize(SampleRate, ChannelConfig, AudioEncoding);
        if (bufferSize <= 0) bufferSize = SampleRate * 2; // fallback: 1 second of 16-bit audio

        _audioRecord = new AudioRecord(
            AudioSource.Mic,
            SampleRate,
            ChannelConfig,
            AudioEncoding,
            bufferSize);

        if (_audioRecord.State != State.Initialized)
            throw new InvalidOperationException("AudioRecord failed to initialize");

        _tempFile = Path.Combine(Path.GetTempPath(), $"tvo_recording_{Guid.NewGuid():N}.wav");
        _isRecording = true;
        _audioRecord.StartRecording();

        // Record in background thread, writing PCM data to file
        _recordingThread = new Thread(() => WriteAudioData(bufferSize));
        _recordingThread.Start();

        Android.Util.Log.Info("VoiceOverlay", $"AudioRecorder: Recording started ({SampleRate}Hz, mono)");
    }

    private void WriteAudioData(int bufferSize)
    {
        var buffer = new byte[bufferSize];
        using var memStream = new MemoryStream();

        while (_isRecording)
        {
            var bytesRead = _audioRecord?.Read(buffer, 0, buffer.Length) ?? 0;
            if (bytesRead > 0)
                memStream.Write(buffer, 0, bytesRead);
        }

        // Write WAV file with header
        try
        {
            var pcmData = memStream.ToArray();
            using var fileStream = new FileStream(_tempFile!, FileMode.Create);
            WriteWavHeader(fileStream, pcmData.Length, SampleRate, 1, 16);
            fileStream.Write(pcmData, 0, pcmData.Length);
            Android.Util.Log.Info("VoiceOverlay", $"AudioRecorder: WAV written ({pcmData.Length} bytes PCM)");
        }
        catch (Exception ex)
        {
            Android.Util.Log.Error("VoiceOverlay", $"AudioRecorder: Error writing WAV: {ex.Message}");
        }
    }

    private static void WriteWavHeader(System.IO.Stream stream, int pcmDataLength, int sampleRate, int channels, int bitsPerSample)
    {
        var byteRate = sampleRate * channels * bitsPerSample / 8;
        var blockAlign = channels * bitsPerSample / 8;
        var dataChunkSize = pcmDataLength;
        var fileSize = 36 + dataChunkSize;

        using var writer = new BinaryWriter(stream, System.Text.Encoding.UTF8, leaveOpen: true);
        // RIFF header
        writer.Write(System.Text.Encoding.ASCII.GetBytes("RIFF"));
        writer.Write(fileSize);
        writer.Write(System.Text.Encoding.ASCII.GetBytes("WAVE"));
        // fmt chunk
        writer.Write(System.Text.Encoding.ASCII.GetBytes("fmt "));
        writer.Write(16); // chunk size
        writer.Write((short)1); // PCM format
        writer.Write((short)channels);
        writer.Write(sampleRate);
        writer.Write(byteRate);
        writer.Write((short)blockAlign);
        writer.Write((short)bitsPerSample);
        // data chunk
        writer.Write(System.Text.Encoding.ASCII.GetBytes("data"));
        writer.Write(dataChunkSize);
    }

    public string? Stop()
    {
        if (!_isRecording) return null;
        _isRecording = false;

        try
        {
            _recordingThread?.Join(2000); // wait up to 2s for recording thread
            _audioRecord?.Stop();
            _audioRecord?.Release();
            _audioRecord?.Dispose();
            _audioRecord = null;
            _recordingThread = null;

            Android.Util.Log.Info("VoiceOverlay", "AudioRecorder: Recording stopped");
            return _tempFile;
        }
        catch (Exception ex)
        {
            Android.Util.Log.Error("VoiceOverlay", $"AudioRecorder: Error stopping: {ex.Message}");
            return null;
        }
    }

    public void Dispose()
    {
        if (_isRecording) Stop();
        _audioRecord?.Release();
        _audioRecord?.Dispose();
    }
}
