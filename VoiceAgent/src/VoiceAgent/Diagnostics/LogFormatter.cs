using System.Text;
using System.Text.Json;

namespace VoiceAgent.Diagnostics
{
    /// <summary>
    /// Formatiert eine JSON-Lines-Logzeile in eine menschenlesbare Zeile fuer den Log-Viewer.
    /// Robust: bei Parse-Fehler wird die Rohzeile zurueckgegeben (nichts geht verloren).
    /// </summary>
    public static class LogFormatter
    {
        /// <summary>JSON-Zeile -> lesbar. Public static = isoliert testbar.</summary>
        public static string FormatLine(string jsonLine)
        {
            if (string.IsNullOrWhiteSpace(jsonLine)) return string.Empty;
            try
            {
                using var doc = JsonDocument.Parse(jsonLine);
                var r = doc.RootElement;

                string ts = Str(r, "ts");
                string time = ts.Length >= 19 ? ts.Substring(11, 8) : ts;  // HH:mm:ss
                string level = Str(r, "level");
                string module = Str(r, "module");
                string fn = Str(r, "fn");
                string msg = Str(r, "msg");

                var sb = new StringBuilder();
                sb.Append('[').Append(time).Append("] ");
                if (r.TryGetProperty("turn", out var turn) && turn.ValueKind != JsonValueKind.Null)
                    sb.Append("turn ").Append(turn.GetRawText()).Append("  ");
                sb.Append(level.PadRight(5)).Append(' ');
                if (module.Length > 0 || fn.Length > 0)
                    sb.Append(module).Append('.').Append(fn).Append(": ");
                sb.Append(msg);
                if (r.TryGetProperty("ctx", out var ctx) && ctx.ValueKind != JsonValueKind.Null)
                    sb.Append("  ").Append(ctx.GetRawText());
                if (r.TryGetProperty("trace", out var tr) && tr.ValueKind == JsonValueKind.String)
                {
                    var t = tr.GetString();
                    if (!string.IsNullOrEmpty(t))
                        sb.Append("\n    ").Append(t!.TrimEnd().Replace("\n", "\n    "));
                }
                return sb.ToString();
            }
            catch
            {
                return jsonLine;   // unparsbar -> roh anzeigen, nie verschlucken
            }
        }

        private static string Str(JsonElement r, string name)
            => r.TryGetProperty(name, out var v) && v.ValueKind == JsonValueKind.String ? (v.GetString() ?? "") : "";
    }
}
