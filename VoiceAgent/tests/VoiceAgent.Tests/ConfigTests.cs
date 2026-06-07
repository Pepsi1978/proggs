using System;
using System.IO;
using VoiceAgent.Services;

namespace VoiceAgent.Tests
{
    public class ConfigTests
    {
        [Fact]
        public void SaveThenLoad_PreservesValues()
        {
            var path = Path.Combine(Path.GetTempPath(), "va-test-" + Guid.NewGuid().ToString("N") + ".json");
            try
            {
                var cfg = new AppSettings
                {
                    SystemPrompt = "Test-Prompt",
                    TtsVoiceName = "de-DE-Chirp3-HD-Kore",
                    LlmModel = "gemini-x",
                    MicEnabled = false
                };
                Config.SaveTo(path, cfg);

                var loaded = Config.LoadFrom(path);
                Assert.Equal("Test-Prompt", loaded.SystemPrompt);
                Assert.Equal("de-DE-Chirp3-HD-Kore", loaded.TtsVoiceName);
                Assert.Equal("gemini-x", loaded.LlmModel);
                Assert.False(loaded.MicEnabled);
            }
            finally
            {
                if (File.Exists(path)) File.Delete(path);
            }
        }

        [Fact]
        public void LoadFrom_MissingFile_ReturnsDefaults()
        {
            var path = Path.Combine(Path.GetTempPath(), "va-missing-" + Guid.NewGuid().ToString("N") + ".json");
            var loaded = Config.LoadFrom(path);
            Assert.Equal(AppSettings.DefaultGeminiModel, loaded.LlmModel);
            Assert.Equal(AppSettings.DefaultTtsVoice, loaded.TtsVoiceName);
            Assert.True(loaded.MicEnabled);
        }
    }
}
