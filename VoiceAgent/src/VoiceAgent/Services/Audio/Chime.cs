using System;
using System.IO;
using System.Linq;
using NAudio.Wave;
using NAudio.Wave.SampleProviders;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Services.Audio
{
    /// <summary>
    /// Spielt den Erinnerungs-/Funkspruch-Signalton, wenn der Agent PROAKTIV etwas sagen will.
    /// Bevorzugt die mitgelieferte WAV (assets/message1.wav neben der EXE, von Frank gewaehlt);
    /// fehlt sie, faellt es auf einen generierten NAudio-Zweiton zurueck. Crasht nie (unkritisch).
    /// </summary>
    public sealed class Chime
    {
        private WaveOutEvent? _wo;
        private IDisposable? _reader;

        /// <summary>
        /// Datei relativ zu assets/ neben der EXE. Setzbar, damit der gewaehlte Ton zur Laufzeit
        /// umgestellt werden kann (z.B. "wakeword.wav" oder "sounds/start/07_blip.mp3"). Unterpfade
        /// mit '/' funktionieren ueber Path.Combine auf Windows. AudioFileReader spielt WAV und MP3.
        /// </summary>
        public string FileName { get; set; }

        /// <param name="fileName">Datei relativ zu assets/ neben der EXE (Default: Erinnerungston).</param>
        public Chime(string fileName = "message1.wav") => FileName = fileName;

        private string SoundPath
        {
            get
            {
                var dir = Path.GetDirectoryName(Environment.ProcessPath) ?? AppContext.BaseDirectory;
                return Path.Combine(dir, "assets", FileName);
            }
        }

        public void Play()
        {
            try
            {
                Stop();
                _wo = new WaveOutEvent();
                var path = SoundPath;
                if (File.Exists(path))
                {
                    var afr = new AudioFileReader(path);   // liest die WAV (ISampleProvider + IDisposable)
                    _reader = afr;
                    _wo.Init(afr);
                }
                else
                {
                    Log.Warn("Chime: WAV nicht gefunden — generierter Ton als Fallback", new { path });
                    _wo.Init(GeneratedTone());
                }
                _wo.Play();
            }
            catch (Exception ex)
            {
                Log.Error("Chime: Abspielen fehlgeschlagen (unkritisch)", ex);
            }
        }

        public void Stop()
        {
            try { _wo?.Stop(); _wo?.Dispose(); } catch { /* egal */ }
            try { _reader?.Dispose(); } catch { /* egal */ }
            _wo = null;
            _reader = null;
        }

        // Fallback: zwei kurze aufsteigende Sinustoene (Kommunikator-artig), falls die WAV fehlt.
        private static ISampleProvider GeneratedTone()
        {
            ISampleProvider Tone(double freq, TimeSpan dur)
                => new SignalGenerator(44100, 1) { Type = SignalGeneratorType.Sin, Frequency = freq, Gain = 0.28 }.Take(dur);
            return new ConcatenatingSampleProvider(new[]
            {
                Tone(988.0,  TimeSpan.FromMilliseconds(110)),
                Tone(1318.5, TimeSpan.FromMilliseconds(170)),
            });
        }
    }
}
