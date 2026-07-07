using VoiceAgent.Services.Audio;

namespace VoiceAgent.Tests
{
    public class PcmConverterTests
    {
        [Fact]
        public void Zero_MapsToZero()
        {
            var f = PcmConverter.Pcm16ToFloat(new byte[] { 0x00, 0x00 }, 2);
            Assert.Single(f);
            Assert.Equal(0f, f[0], 5);
        }

        [Fact]
        public void NegativeFullScale_MapsToMinusOne()
        {
            // -32768 (0x8000, little-endian) / 32768 = -1.0 exakt (Almanach #24: Divisor 32768, nicht 32767)
            var f = PcmConverter.Pcm16ToFloat(new byte[] { 0x00, 0x80 }, 2);
            Assert.Equal(-1f, f[0], 5);
        }

        [Fact]
        public void PositiveMax_StaysBelowOne()
        {
            // +32767 (0x7FFF) / 32768 ~= 0.99997 — bleibt unter 1.0
            var f = PcmConverter.Pcm16ToFloat(new byte[] { 0xFF, 0x7F }, 2);
            Assert.True(f[0] > 0.999f && f[0] < 1.0f);
        }

        [Fact]
        public void HalfScale()
        {
            // 16384 (0x4000) / 32768 = 0.5
            var f = PcmConverter.Pcm16ToFloat(new byte[] { 0x00, 0x40 }, 2);
            Assert.Equal(0.5f, f[0], 5);
        }

        [Fact]
        public void LittleEndian_ByteOrder()
        {
            // 0x0102 = 258 -> 258/32768
            var f = PcmConverter.Pcm16ToFloat(new byte[] { 0x02, 0x01 }, 2);
            Assert.Equal(258f / 32768f, f[0], 6);
        }

        [Fact]
        public void Length_IsHalfOfByteCount()
        {
            var f = PcmConverter.Pcm16ToFloat(new byte[8], 8);
            Assert.Equal(4, f.Length);
        }

        [Fact]
        public void RespectsCount_NotBufferLength()
        {
            // NAudio liefert oft einen groesseren Puffer + BytesRecorded — nur count zaehlt.
            var f = PcmConverter.Pcm16ToFloat(new byte[8], 4);
            Assert.Equal(2, f.Length);
        }
    }
}
