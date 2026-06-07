using System;

namespace VoiceAgent.Services.Audio
{
    /// <summary>
    /// Reine, seiteneffektfreie Audio-Format-Konvertierung — bewusst als pure Funktion
    /// gebaut, damit sie ohne Mikrofon/Hardware isoliert testbar ist (Best-Practice §7).
    ///
    /// Die Wake-Word-Engine (sherpa-onnx) erwartet 16 kHz mono als float[] im Bereich
    /// [-1, 1]. Das Mikrofon (NAudio WaveInEvent) liefert 16-bit signed PCM, little-endian.
    ///
    /// WICHTIG (Bug-Almanach wake-word #24): Divisor ist 32768 (= 2^15), NICHT 32767 —
    /// sonst mappt der negativste Sample (-32768) knapp unter -1,0 und loest nachgelagertes
    /// Clipping aus.
    /// </summary>
    public static class PcmConverter
    {
        /// <summary>
        /// Wandelt einen 16-bit-PCM-Puffer (little-endian) in normalisierte float-Samples.
        /// </summary>
        /// <param name="buffer">Roh-Puffer mit 16-bit-Samples.</param>
        /// <param name="count">Anzahl gueltiger Bytes in <paramref name="buffer"/> (z.B. NAudios BytesRecorded).</param>
        /// <returns>float-Samples in [-1, 1], Laenge = count/2.</returns>
        public static float[] Pcm16ToFloat(byte[] buffer, int count)
        {
            if (buffer == null) throw new ArgumentNullException(nameof(buffer));
            if (count < 0 || count > buffer.Length) count = buffer.Length;

            int sampleCount = count / 2;                  // 2 Bytes pro Sample
            var samples = new float[sampleCount];
            for (int i = 0, j = 0; j < sampleCount; i += 2, j++)
            {
                short s = (short)(buffer[i] | (buffer[i + 1] << 8));   // little-endian
                samples[j] = s / 32768f;                  // 32768, nicht 32767 (Almanach #24)
            }
            return samples;
        }
    }
}
