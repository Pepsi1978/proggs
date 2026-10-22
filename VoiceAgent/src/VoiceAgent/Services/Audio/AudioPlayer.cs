using System;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using NAudio.Wave;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Services.Audio
{
    /// <summary>
    /// Spielt MP3-Audio (TTS-Antwort) ueber NAudio ab. Media Foundation decodiert MP3
    /// auf Windows 10/11 nativ — kein zusaetzlicher Codec noetig. Fehler werden geloggt,
    /// nie geworfen (Wiedergabe darf die App nicht crashen).
    /// </summary>
    public sealed class AudioPlayer
    {
        private readonly object _gate = new();
        private WaveOutEvent? _current;

        /// <summary>Spielt die MP3-Bytes ab und kehrt zurueck, wenn die Wiedergabe fertig ist.</summary>
        public async Task PlayAsync(byte[] mp3Audio, CancellationToken ct = default)
        {
            if (mp3Audio == null || mp3Audio.Length == 0) return;

            try
            {
                StopInternal();

                using var ms = new MemoryStream(mp3Audio);
                // Gemini-TTS liefert WAV (RIFF): direkt per WaveFileReader, sonst MP3 ueber Media Foundation.
                bool isWav = mp3Audio.Length > 12 && mp3Audio[0] == (byte)'R' && mp3Audio[1] == (byte)'I' && mp3Audio[2] == (byte)'F' && mp3Audio[3] == (byte)'F';
                using WaveStream reader = isWav ? new WaveFileReader(ms) : new StreamMediaFoundationReader(ms);
                using var output = new WaveOutEvent();

                var tcs = new TaskCompletionSource();
                output.PlaybackStopped += (_, args) =>
                {
                    if (args.Exception != null) Log.Error("AudioPlayer: PlaybackStopped mit Fehler", args.Exception);
                    tcs.TrySetResult();
                };

                lock (_gate) { _current = output; }
                output.Init(reader);
                output.Play();
                Log.Info("AudioPlayer: Wiedergabe gestartet");

                using (ct.Register(() => { try { output.Stop(); } catch { } }))
                {
                    await tcs.Task.ConfigureAwait(false);
                }
            }
            catch (Exception ex)
            {
                Log.Error("AudioPlayer: Wiedergabe fehlgeschlagen", ex);
            }
            finally
            {
                lock (_gate) { _current = null; }
            }
        }

        /// <summary>Stoppt eine laufende Wiedergabe (z.B. wenn Frank dazwischenredet).</summary>
        public void Stop() => StopInternal();

        private void StopInternal()
        {
            lock (_gate)
            {
                try { _current?.Stop(); } catch (Exception ex) { Log.Error("AudioPlayer: Stop fehlgeschlagen", ex); }
            }
        }
    }
}
