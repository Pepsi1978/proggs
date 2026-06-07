using System;
using System.Globalization;
using System.Text;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Liefert den AKTUELLEN Zeitpunkt + die Zeitzone als Kontext-Block fuer den System-Prompt.
    ///
    /// Hintergrund (Frank-Vorfall 2026-06-07): ein LLM kennt die Uhrzeit NICHT von sich aus.
    /// Ohne diesen Block raet/halluziniert es (Agent sagte "11 Uhr" obwohl es 22:03 war). Der
    /// Block wird bei JEDEM Turn frisch gebaut, damit die Zeit stimmt.
    /// </summary>
    public static class TimeContext
    {
        /// <summary>Loest die Zeitzone auf: leer/unbekannt -> lokale System-Zeitzone (automatisch). Kein Crash.</summary>
        public static TimeZoneInfo ResolveZone(string? timeZoneId)
        {
            if (string.IsNullOrWhiteSpace(timeZoneId)) return TimeZoneInfo.Local;
            try { return TimeZoneInfo.FindSystemTimeZoneById(timeZoneId); }
            catch { return TimeZoneInfo.Local; }   // unbekannte/ungueltige ID -> lokal
        }

        /// <summary>Baut den Zeit-Block. 'now' wird injiziert (testbar). Standard: DateTimeOffset.Now.</summary>
        public static string BuildBlock(string? timeZoneId, DateTimeOffset now)
        {
            var zone = ResolveZone(timeZoneId);
            var local = TimeZoneInfo.ConvertTime(now, zone);
            var de = new CultureInfo("de-DE");

            var sb = new StringBuilder();
            sb.Append("\n\nAKTUELLER ZEITPUNKT (verlaessliche Echtzeit vom System — nutze GENAU diese Angabe, rate NIE die Uhrzeit oder das Datum):\n");
            sb.Append("- Jetzt: ").Append(local.ToString("dddd, d. MMMM yyyy, HH:mm 'Uhr'", de)).Append('\n');
            sb.Append("- Zeitzone: ").Append(zone.Id).Append(" (UTC").Append(local.ToString("zzz", CultureInfo.InvariantCulture)).Append(')');
            return sb.ToString();
        }

        /// <summary>Der Zeit-Block fuer JETZT (Echtzeit).</summary>
        public static string BuildBlock(string? timeZoneId) => BuildBlock(timeZoneId, DateTimeOffset.Now);
    }
}
