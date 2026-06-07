using System;
using VoiceAgent.Core;

namespace VoiceAgent.Tests
{
    public class TimeContextTests
    {
        [Fact]
        public void BuildBlock_ContainsTimeAndZone()
        {
            var now = new DateTimeOffset(2026, 6, 7, 22, 3, 0, TimeSpan.FromHours(2));
            var block = TimeContext.BuildBlock("", now);   // "" -> lokale Zone
            Assert.Contains("AKTUELLER ZEITPUNKT", block);
            Assert.Contains("Zeitzone", block);
        }

        [Fact]
        public void ResolveZone_EmptyOrInvalid_FallsBackToLocal()
        {
            Assert.Equal(TimeZoneInfo.Local, TimeContext.ResolveZone(""));
            Assert.Equal(TimeZoneInfo.Local, TimeContext.ResolveZone("Gibt-es-nicht-XYZ"));
        }

        [Fact]
        public void BuildBlock_RespectsExplicitUtc()
        {
            var now = new DateTimeOffset(2026, 6, 7, 20, 3, 0, TimeSpan.Zero);   // 20:03 UTC
            var block = TimeContext.BuildBlock("UTC", now);
            // In UTC bleibt es 20:03 — die Stunde muss im Block stehen.
            Assert.Contains("20:03", block);
        }
    }
}
