using System;
using VoiceAgent.Core;

namespace VoiceAgent.Tests
{
    public class ReminderTests
    {
        private static readonly DateTimeOffset Now =
            new DateTimeOffset(2026, 6, 7, 22, 3, 0, TimeSpan.FromHours(2));

        [Fact]
        public void ParseDueTime_AbsoluteWithMinutes()
        {
            var due = ReminderSubAgent.ParseDueTime("erinnere mich um 22:15 ans Essen", Now);
            Assert.NotNull(due);
            Assert.Equal(22, due!.Value.Hour);
            Assert.Equal(15, due.Value.Minute);
        }

        [Fact]
        public void ParseDueTime_FullHour()
        {
            var due = ReminderSubAgent.ParseDueTime("wecke mich um 7 Uhr", Now);
            Assert.NotNull(due);
            Assert.Equal(7, due!.Value.Hour);
            // 7 Uhr ist heute (22:03) schon vorbei -> morgen
            Assert.Equal(Now.Day + 1, due.Value.Day);
        }

        [Fact]
        public void ParseDueTime_Relative()
        {
            var due = ReminderSubAgent.ParseDueTime("erinnere mich in 10 minuten", Now);
            Assert.NotNull(due);
            Assert.Equal(Now.AddMinutes(10), due!.Value);
        }

        [Fact]
        public void ParseDueTime_NoTime_Null()
            => Assert.Null(ReminderSubAgent.ParseDueTime("erinnere mich ans Essen", Now));

        [Fact]
        public void ExtractReminderText_StripsTriggerAndTime()
        {
            var text = ReminderSubAgent.ExtractReminderText("erinnere mich um 22:15 daran, mein Essen zu machen");
            Assert.Contains("Essen", text);
            Assert.DoesNotContain("erinnere", text.ToLowerInvariant());
            Assert.DoesNotContain("22:15", text);
        }

        [Fact]
        public void TakeDue_ReturnsAndMarksDone()
        {
            var path = System.IO.Path.Combine(System.IO.Path.GetTempPath(), $"varem-{Guid.NewGuid():N}.json");
            try
            {
                var svc = new ReminderService(path);
                svc.Add(new Reminder { Text = "Essen", DueUtc = Now.AddMinutes(-1) });   // schon faellig
                svc.Add(new Reminder { Text = "Spaeter", DueUtc = Now.AddHours(1) });     // noch nicht
                var due = svc.TakeDue(Now);
                Assert.Single(due);
                Assert.Equal("Essen", due[0].Text);
                Assert.Empty(svc.TakeDue(Now));   // schon als done markiert
            }
            finally { try { System.IO.File.Delete(path); } catch { } }
        }
    }
}
