using System;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;
using NAudio.Wave;
using PromptBoard.Core.Services;

namespace PromptBoard.Services;

/// <summary>
/// NAudio-based microphone recorder. Captures 16 kHz mono PCM into an
/// in-memory WAV (with RIFF header), suitable for upload to Groq's
/// Whisper endpoint. One session at a time.
/// </summary>
public sealed class AudioRecorder : IAudioRecorder, IDisposable
{
    private readonly ILogger<AudioRecorder> _logger;
    private readonly WaveFormat _format = new(rate: 16000, bits: 16, channels: 1);

    private WaveInEvent? _waveIn;
    private MemoryStream? _buffer;
    private WaveFileWriter? _writer;
    private readonly object _gate = new();

    public AudioRecorder(ILogger<AudioRecorder> logger)
    {
        _logger = logger;
    }

    public bool IsRecording
    {
        get { lock (_gate) { return _waveIn is not null; } }
    }

    public Task StartAsync(CancellationToken ct = default)
    {
        lock (_gate)
        {
            if (_waveIn is not null)
            {
                throw new InvalidOperationException("Eine Aufnahme laeuft bereits.");
            }

            try
            {
                _buffer = new MemoryStream();
                _writer = new WaveFileWriter(new IgnoreDisposeStream(_buffer), _format);

                _waveIn = new WaveInEvent
                {
                    WaveFormat = _format,
                    BufferMilliseconds = 50,
                };
                _waveIn.DataAvailable += OnDataAvailable;
                _waveIn.RecordingStopped += OnRecordingStopped;
                _waveIn.StartRecording();
                _logger.LogInformation("Audio capture started: {Rate} Hz, {Ch} ch.", _format.SampleRate, _format.Channels);
            }
            catch
            {
                CleanupInternal();
                throw;
            }
        }
        return Task.CompletedTask;
    }

    public Task<byte[]> StopAsync(CancellationToken ct = default)
    {
        WaveInEvent? local;
        MemoryStream? bufferToReturn;

        lock (_gate)
        {
            if (_waveIn is null)
            {
                return Task.FromResult(Array.Empty<byte>());
            }

            local = _waveIn;
            bufferToReturn = _buffer;
            _waveIn = null;
        }

        try
        {
            local.StopRecording();
            local.DataAvailable -= OnDataAvailable;
            local.RecordingStopped -= OnRecordingStopped;
            local.Dispose();
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Error while stopping WaveIn.");
        }

        lock (_gate)
        {
            _writer?.Flush();
            _writer?.Dispose();
            _writer = null;
            _buffer = null;
        }

        byte[] result = bufferToReturn?.ToArray() ?? [];
        _logger.LogInformation("Audio capture stopped. Bytes captured: {Bytes}", result.Length);
        return Task.FromResult(result);
    }

    private void OnDataAvailable(object? sender, WaveInEventArgs e)
    {
        lock (_gate)
        {
            if (_writer is null)
            {
                return;
            }
            _writer.Write(e.Buffer, 0, e.BytesRecorded);
        }
    }

    private void OnRecordingStopped(object? sender, StoppedEventArgs e)
    {
        if (e.Exception is not null)
        {
            _logger.LogError(e.Exception, "WaveIn stopped with error.");
        }
    }

    private void CleanupInternal()
    {
        try { _writer?.Dispose(); } catch { /* ignored */ }
        try { _buffer?.Dispose(); } catch { /* ignored */ }
        _writer = null;
        _buffer = null;
        _waveIn = null;
    }

    public void Dispose()
    {
        lock (_gate)
        {
            if (_waveIn is not null)
            {
                try { _waveIn.StopRecording(); } catch { /* ignored */ }
                _waveIn.Dispose();
                _waveIn = null;
            }
            CleanupInternal();
        }
    }

    /// <summary>
    /// Wraps a stream so WaveFileWriter.Dispose does not close the
    /// underlying MemoryStream — we still need to read its bytes after
    /// the writer is flushed.
    /// </summary>
    private sealed class IgnoreDisposeStream : Stream
    {
        private readonly Stream _inner;
        public IgnoreDisposeStream(Stream inner) => _inner = inner;

        public override bool CanRead => _inner.CanRead;
        public override bool CanSeek => _inner.CanSeek;
        public override bool CanWrite => _inner.CanWrite;
        public override long Length => _inner.Length;
        public override long Position { get => _inner.Position; set => _inner.Position = value; }
        public override void Flush() => _inner.Flush();
        public override int Read(byte[] buffer, int offset, int count) => _inner.Read(buffer, offset, count);
        public override long Seek(long offset, SeekOrigin origin) => _inner.Seek(offset, origin);
        public override void SetLength(long value) => _inner.SetLength(value);
        public override void Write(byte[] buffer, int offset, int count) => _inner.Write(buffer, offset, count);
        protected override void Dispose(bool disposing) { /* deliberately swallow */ }
    }
}
