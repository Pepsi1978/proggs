using System;
using System.Globalization;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Diagnostics;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Unteragent fuer Erinnerungen. Versteht konkrete Zeiten ("um HH:MM", "in N Minuten"),
    /// grobe Tageszeiten ("vormittag/mittag/nachmittag/abend", optional mit "morgen") UND
    /// Erinnerungen OHNE Uhrzeit ("beim naechsten Start" bzw. gar keine Zeit) — die kommen dann
    /// beim naechsten vollstaendigen App-Start (Sound + Ansage, ueber MainWindow/ReminderService).
    /// Der Agent besteht NICHT mehr auf einer exakten Uhrzeit. Reines In-App-Verhalten, KEIN Computer Use.
    /// </summary>
    public sealed class ReminderSubAgent : ISubAgent
    {
        private readonly ReminderService _service;
        public ReminderSubAgent(ReminderService service) => _service = service;

        public string Name => "Erinnerung";
        public string Description => "Merkt sich Erinnerungen (mit Uhrzeit, grober Tageszeit oder beim naechsten Start) und meldet sich proaktiv.";

        public string HowTo => "Sag z. B. 'erinnere mich um 22:15 ans Essen', 'erinnere mich morgen Vormittag an den Anruf' oder ohne Zeit 'erinnere mich beim naechsten Start an X' — zur faelligen Zeit ertoent ein Signalton, bis der Nutzer reagiert.";

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

        /// <summary>Wie eine Erinnerung terminiert ist.</summary>
        public enum WhenKind { AtTime, NextStart }

        /// <summary>Geplante Terminierung + bereinigter Erinnerungstext (an EINER Stelle entschieden,
        /// damit Rueckfrage und Ausfuehrung garantiert konsistent sind).</summary>
        public readonly record struct Plan(WhenKind When, DateTimeOffset DueUtc, string Text);

        /// <summary>
        /// Entscheidet, WANN erinnert wird: konkrete Zeit/Tageszeit -> AtTime; "beim Start" ODER
        /// GAR KEINE Zeit erkannt -> NextStart (Default — statt auf einer Uhrzeit zu bestehen).
        /// </summary>
        public static Plan BuildPlan(string task, DateTimeOffset now)
        {
            var text = ExtractReminderText(task);
            var due = ParseDueTime(task, now);
            if (due != null) return new Plan(WhenKind.AtTime, due.Value, text);
            // Keine Zeit erkannt (auch "beim Start"): beim naechsten vollstaendigen App-Start melden.
            return new Plan(WhenKind.NextStart, default, text);
        }

        /// <summary>Spezifische Rueckfrage: nennt Zeitpunkt/Tageszeit bzw. "beim naechsten Start".</summary>
        public string ConfirmationQuestion(string task)
        {
            var plan = BuildPlan(task, DateTimeOffset.Now);
            if (plan.When == WhenKind.NextStart)
            {
                var baseQ = string.IsNullOrWhiteSpace(plan.Text)
                    ? "Soll ich dich beim nächsten Programmstart erinnern?"
                    : $"Soll ich dich beim nächsten Programmstart daran erinnern: {plan.Text}?";
                // Hat Frank "beim Start" ausdruecklich gesagt, reicht das. Sonst (gar keine Zeit
                // genannt) zusaetzlich die Alternativen anbieten, statt auf einer Uhrzeit zu bestehen.
                return IsNextStartRequest(task)
                    ? baseQ
                    : baseQ + " Oder sag eine Zeit, zum Beispiel: morgen vormittag, heute abend, oder um neun Uhr.";
            }
            var when = plan.DueUtc.ToString("HH:mm", new CultureInfo("de-DE"));
            return string.IsNullOrWhiteSpace(plan.Text)
                ? $"Soll ich dich um {when} Uhr erinnern?"
                : $"Soll ich dich um {when} Uhr daran erinnern: {plan.Text}?";
        }

        public Task<string> HandleAsync(string task, CancellationToken ct = default)
        {
            var plan = BuildPlan(task, DateTimeOffset.Now);
            Log.Info("Reminder geplant", new
            {
                when = plan.When.ToString(),
                due = plan.When == WhenKind.AtTime ? plan.DueUtc.ToString("u") : "beim-naechsten-start",
                plan.Text
            });

            if (plan.When == WhenKind.NextStart)
            {
                _service.Add(new Reminder { Text = plan.Text, OnNextStart = true });
                return Task.FromResult($"Alles klar, ich erinnere dich beim nächsten Programmstart daran: {plan.Text}");
            }

            _service.Add(new Reminder { Text = plan.Text, DueUtc = plan.DueUtc });
            var when = plan.DueUtc.ToString("HH:mm", new CultureInfo("de-DE"));
            return Task.FromResult($"Alles klar, ich erinnere dich um {when} Uhr daran: {plan.Text}");
        }

        /// <summary>
        /// Erkennt den Wunsch "beim naechsten (App-/Programm-)Start erinnern". Umlaut-tolerant,
        /// weil die Transkription Umlaute liefert (naechsten/laeuft/...), Tests aber ASCII nutzen koennen.
        /// </summary>
        public static bool IsNextStartRequest(string text)
        {
            if (string.IsNullOrWhiteSpace(text)) return false;
            var t = text.ToLowerInvariant();
            return Regex.IsMatch(t,
                @"(beim|zum)\s+(n(ä|ae)chsten\s+)?(programm)?start"
              + @"|(beim|zum)\s+(hochfahren|neustart|hochfaehren)"
              + @"|n(ä|ae)chsten?\s+mal"
              + @"|wenn\s+(du|das programm|die app|der computer|der rechner)\s+(startest|startet|hochf(ä|ae)hrt|l(ä|ae)uft|l(ä|ae)dt|geladen)"
              + @"|wenn ich (wiederkomme|zur(ü|ue)ck|wieder da)");
        }

        /// <summary>
        /// Liest einen Zeitpunkt aus dem Text: "in N Minuten/Stunden", "um HH:MM"/"um HH Uhr",
        /// und grobe Tageszeiten (vormittag/mittag/nachmittag/abend/frueh), optional mit "morgen".
        /// Liegt die Zeit heute schon in der Vergangenheit, wird der naechste Tag genommen.
        /// Public static = isoliert testbar. null = nichts erkannt (-> beim naechsten Start, siehe BuildPlan).
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

            // "morgen" = naechster Tag. NICHT "am morgen"/"morgens" (das ist die Tageszeit "frueh").
            bool tomorrow = Regex.IsMatch(t, @"\bmorgen\b") && !Regex.IsMatch(t, @"\bam\s+morgen\b");

            // grobe Tageszeit (vormittag/mittag/nachmittag/abend/frueh)
            int? hour = TimeOfDayHour(t);
            if (hour != null)
            {
                var due = BuildAbsolute(now, hour.Value, 0);
                if (due != null && tomorrow && due.Value.Date == now.Date)
                    due = due.Value.AddDays(1);   // "morgen vormittag" sicher auf den naechsten Tag
                return due;
            }

            // "morgen" allein (ohne Tageszeit) -> morgen vormittag (grober Default, faellt beim
            // naechsten App-Start nach 9 Uhr auf; vorher fangt der Sekundentakt es ab).
            if (tomorrow)
            {
                var due = BuildAbsolute(now, 9, 0);
                if (due != null && due.Value.Date == now.Date) due = due.Value.AddDays(1);
                return due;
            }

            return null;
        }

        /// <summary>Grobe Tageszeit -> volle Stunde. null = keine Tageszeit erkannt.
        /// Nachmittag VOR Mittag pruefen (sonst matcht "mittag" in "nachmittag").</summary>
        public static int? TimeOfDayHour(string t)
        {
            if (string.IsNullOrWhiteSpace(t)) return null;
            if (Regex.IsMatch(t, @"\bvormittags?\b|\bam vormittag\b")) return 9;
            if (Regex.IsMatch(t, @"\bnachmittags?\b|\bam nachmittag\b")) return 15;
            if (Regex.IsMatch(t, @"\bmittags?\b|\bam mittag\b|\bzu mittag\b")) return 12;
            if (Regex.IsMatch(t, @"\babends?\b|\bam abend\b")) return 19;
            if (Regex.IsMatch(t, @"\b(frü|frue)h\b|\bfrü?hmorgens\b|\bmorgens\b|\bam morgen\b")) return 8;
            return null;
        }

        private static DateTimeOffset? BuildAbsolute(DateTimeOffset now, int h, int m)
        {
            if (h > 23 || m > 59) return null;
            var due = new DateTimeOffset(now.Year, now.Month, now.Day, h, m, 0, now.Offset);
            if (due <= now) due = due.AddDays(1);   // heute schon vorbei -> morgen
            return due;
        }

        /// <summary>Schneidet Trigger-, Zeit-, Tageszeit- und Start-Phrasen weg, sodass die eigentliche
        /// Erinnerung bleibt. Testbar.</summary>
        public static string ExtractReminderText(string task)
        {
            if (string.IsNullOrWhiteSpace(task)) return "";
            var s = task;
            s = Regex.Replace(s, @"(?i)\b(erinnere?\s+mich|erinner\s+mich|wecke?\s+mich|sag\s+mir\s+bescheid|sag\s+bescheid|bitte)\b", " ");
            s = Regex.Replace(s, @"(?i)\bin\s+\d+\s*(minuten|minute|min|stunden|stunde|std)\b", " ");
            s = Regex.Replace(s, @"(?i)\bum\s+\d{1,2}([:.\s]\s*\d{2})?\s*(uhr)?\b", " ");
            // Tageszeit-/Tag-/Start-Phrasen ebenfalls entfernen, damit nur der Inhalt bleibt.
            s = Regex.Replace(s, @"(?i)\b(morgen|heute|vormittags?|nachmittags?|mittags?|abends?|(frü|frue)h|frü?hmorgens|morgens)\b", " ");
            s = Regex.Replace(s, @"(?i)\bam (vormittag|nachmittag|mittag|abend|morgen)\b|\bzu mittag\b", " ");
            s = Regex.Replace(s, @"(?i)\b(beim|zum) (n(ä|ae)chsten )?(programm)?(start|hochfahren|neustart|mal)\b", " ");
            s = Regex.Replace(s, @"(?i)\bwenn (du|das programm|die app|der computer|der rechner) (startest|startet|hochf(ä|ae)hrt|l(ä|ae)uft|l(ä|ae)dt|geladen ist)\b", " ");
            s = Regex.Replace(s, @"(?i)^\s*(daran[:,]?|dass|an|dran[:,]?)\s+", " ");
            s = Regex.Replace(s, @"\s+", " ").Trim().Trim(',', '.', ':', ' ');
            return s.Length > 0 ? s : task.Trim();
        }
    }
}
