using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Channels;
using System.Threading.Tasks;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Services.Audio
{
    /// <summary>
    /// Liest einen Strom vollstaendiger Saetze und liest sie ZEITECHT vor: waehrend Satz N
    /// abgespielt wird, synthetisiert ein Producer bereits Satz N+1 (Vorausschau ueber einen
    /// bounded Channel mit Backpressure, BP §1). So startet der erste Ton schon nach der
    /// Synthese des ERSTEN Satzes — nicht erst, wenn die ganze Antwort fertig ist.
    ///
    /// Robust/funktionserhaltend: ohne TTS-Client laeuft der Satzstrom trotzdem durch
    /// (onSentence wird aufgerufen, nur ohne Audio); TTS-Fehler bei einem Satz ueberspringen
    /// nur diesen Satz, der Rest wird weiter vorgelesen.
    /// </summary>
    public sealed class StreamingSpeaker
    {
        private readonly GoogleTtsClient? _tts;
        private readonly AudioPlayer _player;

        public StreamingSpeaker(GoogleTtsClient? tts, AudioPlayer player)
        {
            _tts = tts;
            _player = player;
        }

        /// <summary>
        /// Spricht die Saetze der Reihe nach. onSentence wird fuer JEDEN Satz aufgerufen, sobald
        /// er aus dem Strom kommt (fuer Text-Akkumulation/UI) — auch ohne TTS. Liefert true,
        /// wenn mindestens ein Satz tatsaechlich vorgelesen wurde.
        /// </summary>
        public async Task<bool> SpeakStreamAsync(
            IAsyncEnumerable<string> sentences,
            string languageCode,
            string voiceName,
            Action<string>? onSentence = null,
            CancellationToken ct = default)
        {
            // Kapazitaet 2: bis zu zwei Saetze duerfen vorsynthetisiert sein (Vorausschau ohne Speicherflut).
            var channel = Channel.CreateBounded<byte[]>(new BoundedChannelOptions(2)
            {
                FullMode = BoundedChannelFullMode.Wait,
                SingleReader = true,
                SingleWriter = true
            });

            // Producer: Saetze -> (onSentence) -> TTS -> Channel.
            var producer = Task.Run(async () =>
            {
                try
                {
                    await foreach (var sentence in sentences.WithCancellation(ct).ConfigureAwait(false))
                    {
                        if (string.IsNullOrWhiteSpace(sentence)) continue;
                        try { onSentence?.Invoke(sentence); }
                        catch (Exception ex) { Log.Error("StreamingSpeaker: onSentence-Callback warf (ignoriert)", ex); }

                        if (_tts == null) continue;   // kein TTS -> nur Text, kein Audio

                        byte[] audio;
                        try
                        {
                            audio = await _tts.SynthesizeAsync(sentence, languageCode, voiceName, ct).ConfigureAwait(false);
                        }
                        catch (OperationCanceledException) { throw; }
                        catch (Exception ex)
                        {
                            Log.Error("StreamingSpeaker: TTS fuer einen Satz fehlgeschlagen (uebersprungen)", ex, ctx: new { sentence });
                            continue;   // diesen Satz ueberspringen, Rest weiter vorlesen
                        }
                        await channel.Writer.WriteAsync(audio, ct).ConfigureAwait(false);
                    }
                }
                finally
                {
                    channel.Writer.Complete();   // Consumer weiss: Schluss (BP §1)
                }
            }, ct);

            // Consumer: Audio-Haeppchen SEQUENZIELL abspielen (auf dem aufrufenden Kontext).
            bool spoke = false;
            await foreach (var audio in channel.Reader.ReadAllAsync(ct).ConfigureAwait(false))
            {
                await _player.PlayAsync(audio, ct).ConfigureAwait(false);
                spoke = true;
            }

            await producer.ConfigureAwait(false);   // Producer-Fehler hochreichen (nach Drain)
            return spoke;
        }
    }
}
