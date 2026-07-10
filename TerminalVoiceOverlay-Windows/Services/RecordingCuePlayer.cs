using System;
using System.IO;
using System.Media;

namespace TerminalVoiceOverlay.Services
{
    public sealed class RecordingCuePlayer : IDisposable
    {
        private const int SampleRate = 44100;
        private readonly MemoryStream _startWave = CreateWave((880, 150));
        private readonly MemoryStream _stopWave = CreateWave((660, 120), (440, 120));
        private readonly SoundPlayer _startPlayer;
        private readonly SoundPlayer _stopPlayer;

        public RecordingCuePlayer()
        {
            _startPlayer = new SoundPlayer(_startWave);
            _stopPlayer = new SoundPlayer(_stopWave);

            try
            {
                _startPlayer.Load();
                _stopPlayer.Load();
                DiagLog.Write("AudioCue", "preloaded");
            }
            catch (Exception ex)
            {
                DiagLog.Error("AudioCue", "preload_failed", ex);
            }
        }

        public void PlayStart() => Play(_startPlayer, "start");

        public void PlayStop() => Play(_stopPlayer, "stop");

        private static void Play(SoundPlayer player, string cue)
        {
            try
            {
                player.Play();
                DiagLog.Write("AudioCue", "play_dispatched", ("cue", cue));
            }
            catch (Exception ex)
            {
                DiagLog.Error("AudioCue", "play_failed", ex, ("cue", cue));
                try { SystemSounds.Beep.Play(); }
                catch (Exception fallbackEx)
                {
                    DiagLog.Error("AudioCue", "fallback_failed", fallbackEx, ("cue", cue));
                }
            }
        }

        private static MemoryStream CreateWave(params (int Frequency, int DurationMs)[] tones)
        {
            int sampleCount = 0;
            foreach (var tone in tones)
                sampleCount += SampleRate * tone.DurationMs / 1000;

            var stream = new MemoryStream(44 + sampleCount * sizeof(short));
            using (var writer = new BinaryWriter(stream, System.Text.Encoding.UTF8, leaveOpen: true))
            {
                int dataLength = sampleCount * sizeof(short);
                writer.Write(System.Text.Encoding.ASCII.GetBytes("RIFF"));
                writer.Write(36 + dataLength);
                writer.Write(System.Text.Encoding.ASCII.GetBytes("WAVEfmt "));
                writer.Write(16);
                writer.Write((short)1);
                writer.Write((short)1);
                writer.Write(SampleRate);
                writer.Write(SampleRate * sizeof(short));
                writer.Write((short)sizeof(short));
                writer.Write((short)16);
                writer.Write(System.Text.Encoding.ASCII.GetBytes("data"));
                writer.Write(dataLength);

                foreach (var (frequency, durationMs) in tones)
                {
                    int toneSamples = SampleRate * durationMs / 1000;
                    int fadeSamples = Math.Min(SampleRate / 200, toneSamples / 2);
                    for (int i = 0; i < toneSamples; i++)
                    {
                        double fade = Math.Min(1.0, Math.Min(i + 1, toneSamples - i) / (double)fadeSamples);
                        short sample = (short)(Math.Sin(2 * Math.PI * frequency * i / SampleRate) * short.MaxValue * 0.22 * fade);
                        writer.Write(sample);
                    }
                }
            }

            stream.Position = 0;
            return stream;
        }

        public void Dispose()
        {
            _startPlayer.Dispose();
            _stopPlayer.Dispose();
            _startWave.Dispose();
            _stopWave.Dispose();
        }
    }
}
