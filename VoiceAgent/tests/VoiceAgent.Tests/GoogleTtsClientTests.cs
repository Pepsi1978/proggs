using System;
using System.Linq;
using VoiceAgent.Services;

namespace VoiceAgent.Tests
{
    public class GoogleTtsClientTests
    {
        [Fact]
        public void BuildRequestJson_UsesVoiceAndMp3()
        {
            var body = GoogleTtsClient.BuildRequestJson("Hallo", "de-DE", "de-DE-Chirp3-HD-Kore");
            Assert.Contains("\"name\":\"de-DE-Chirp3-HD-Kore\"", body);
            Assert.Contains("\"languageCode\":\"de-DE\"", body);
            Assert.Contains("\"audioEncoding\":\"MP3\"", body);
            Assert.Contains("\"text\":\"Hallo\"", body);
        }

        [Fact]
        public void ParseAudio_DecodesBase64()
        {
            var original = new byte[] { 1, 2, 3, 42, 255 };
            var json = "{\"audioContent\":\"" + Convert.ToBase64String(original) + "\"}";
            Assert.Equal(original, GoogleTtsClient.ParseAudio(json));
        }

        [Fact]
        public void Voices_Has30AndContainsDefault()
        {
            Assert.Equal(30, GoogleTtsVoices.All.Count);
            Assert.Contains(GoogleTtsVoices.All, v => v.Name == GoogleTtsVoices.DefaultVoiceName);
            Assert.Equal(14, GoogleTtsVoices.All.Count(v => v.Gender == VoiceGender.Female));
            Assert.Equal(16, GoogleTtsVoices.All.Count(v => v.Gender == VoiceGender.Male));
        }
    }
}
