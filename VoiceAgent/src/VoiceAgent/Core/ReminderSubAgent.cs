using System;
using System.Globalization;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Unteragent fuer Erinnerungen: erkennt "erinnere mich um HH:MM / in N Minuten an X",
    /// parst Zeitpunkt + Text und legt eine Erinnerung an. Zur faelligen Zeit meldet sich der
    /// Agent dann PROAKTIV (Enterprise-Sound + Ansage) — das macht das MainWindow ueber den
    /// ReminderService. Reines In-App-Verhalten, KEIN Computer Use.
    /// </summary>
    public sealed class ReminderSubAgent : ISubAgent
    {
        private readonly ReminderService _service;
        public ReminderSubAgent(ReminderService service) => _service = service;

        public string Name => "Erinnerung";
        public string Description => "Merkt sich zeitgesteuerte Erinnerungen und meldet sich proaktiv, wenn sie faellig sind.";

        private static readonly string[] Triggers =
            { "erinner", "wecke mich", "wecker", "sag mir bescheid", "sag bescheid" };

        public bool CanHandle(string task)
        {
            if (string.IsNullOrWhiteSpace(task)) return false;
            var t = task.ToLowerInvariant();
            foreach (var tr in Triggers)
                if (t.Contains(tr)) return true;
            return false;
        }

        public Task<string> HandleAsync(string task, CancellationToken ct = default)
        {
            var due = ParseDueTime(task, DateTimeOffset.Now);
            if (due == null)
            {
                Probe.That(false, "ReminderSubAgent: keine Zeit erkannt", new { task });
                return Task.FromResult("Ich konnte keine Zeit erkennen. Sag zum Beispiel: erinnere mich um 22 Uhr 15 ans Essen, oder in zehn Minuten.");
            }

            var text = ExtractReminderText(task);
            _service.Add(new Reminder { Text = text, DueUtc = due.Value });

            var culture = new CultureInfo("de-DE");
            var when = due.Value.ToString("HH:mm", culture);
            return Task.FromResult($"Alles klar, ich erinnere dich um {when} Uhr daran: {text}");
        }

        /// <summary>
        /// Liest einen Zeitpunkt aus dem Text. Unterstuetzt "in N Minuten/Stunden" und
        /// "um HH:MM" / "um HH Uhr". Liegt die absolute Zeit heute schon in der Vergangenheit,
        /// wird der naechste Tag genommen. Public static = isoliert testbar. null = nichts erkannt.
        /// </summary>
        public static DateTimeOffset? ParseDueTime(string text, DateTimeOffset now)
        {
            if (string.IsNullOrWhiteSpace(text)) return null;
            var t = text.ToLowerInvariant();

            // relativ: "in 10 minuten", "in 2 stunden"
            var rel = Regex.Match(t, @"in\s+(\d+)\s*(minuten|minute|min|stunden|stunde|std)");
            if (rel.Success)
            {
                int n = int.Parse(rel.Groups[1].Value, CultureInfo.InvariantCulture);
                var unit = rel.Groups[2].Value;
                bool hours = unit.StartsWith("std") || unit.StartsWith("stunde");
                return hours ? now.AddHours(n) : now.AddMinutes(n);
            }

            // absolut mit Minuten: "um 22:15", "um 22.15", "um 22 15"
            var hm = Regex.Match(t, @"um\s+(\d{1,2})[:.\s]\s*(\d{2})(?:\s*uhr)?");
            if (hm.Success)
            {
                int h = int.Parse(hm.Groups[1].Value, CultureInfo.InvariantCulture);
                int m = int.Parse(hm.Groups[2].Value, CultureInfo.InvariantCulture);
                return BuildAbsolute(now, h, m);
            }

            // absolut volle Stunde: "um 22 uhr"
            var hh = Regex.Match(t, @"um\s+(\d{1,2})\s*uhr");
            if (hh.Success)
            {
                int h = int.Parse(hh.Groups[1].Value, CultureInfo.InvariantCulture);
                return BuildAbsolute(now, h, 0);
            }

            return null;
        }

        private static DateTimeOffset? BuildAbsolute(DateTimeOffset now, int h, int m)
        {
            if (h > 23 || m > 59) return null;
            var due = new DateTimeOffset(now.Year, now.Month, now.Day, h, m, 0, now.Offset);
            if (due <= now) due = due.AddDays(1);   // heute schon vorbei -> morgen
            return due;
        }

        /// <summary>Schneidet Trigger- und Zeit-Phrasen weg, sodass die eigentliche Erinnerung bleibt. Testbar.</summary>
        public static string ExtractReminderText(string task)
        {
            if (string.IsNullOrWhiteSpace(task)) return "";
            var s = task;
            s = Regex.Replace(s, @"(?i)\b(erinnere?\s+mich|erinner\s+mich|wecke?\s+mich|sag\s+mir\s+bescheid|sag\s+bescheid|bitte)\b", " ");
            s = Regex.Replace(s, @"(?i)\bin\s+\d+\s*(minuten|minute|min|stunden|stunde|std)\b", " ");
            s = Regex.Replace(s, @"(?i)\bum\s+\d{1,2}([:.\s]\s*\d{2})?\s*(uhr)?\b", " ");
            s = Regex.Replace(s, @"(?i)^\s*(daran[:,]?|dass|an|dran[:,]?)\s+", " ");
            s = Regex.Replace(s, @"\s+", " ").Trim().Trim(',', '.', ':', ' ');
            return s.Length > 0 ? s : task.Trim();
        }
    }
}
