using System;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using VoiceAgent.Diagnostics;
using VoiceAgent.Services;
using VoiceAgent.Services.Llm;

namespace VoiceAgent.Core
{
    /// <summary>
    /// Unteragent fuer "Computer Use": leitet aus einer Aufgabe einen Windows-Befehl ab, prueft ihn
    /// durch den <see cref="CommandGuard"/> und fuehrt ihn — je nach Stufe — aus. Nachgebaut nach
    /// dem Hermes-Modell (Off / Safe / Full):
    ///   - Off:  tut nichts (informiert, dass Computer Use aus ist).
    ///   - Safe: gefaehrliche Befehle werden ZUERST gezeigt und erst nach gesprochenem "Ja" ausgefuehrt.
    ///   - Full: alles laeuft direkt — AUSSER der Hardline-Blocklist (nie umgehbar).
    ///
    /// Macht seine Bestaetigung SELBST (HandlesOwnConfirmation), um erst den konkreten Befehl
    /// abzuleiten und ihn dann zu zeigen — der Hauptagent leitet die Folge-Antwort ("ja"/"nein")
    /// ueber AwaitingFollowup direkt hierher.
    /// </summary>
    public sealed class ComputerUseSubAgent : ISubAgent
    {
        private readonly ILlmProvider _llm;
        private readonly Func<ComputerUseMode> _mode;
        private readonly Func<string, CancellationToken, Task<ShellResult>> _executor;

        private string? _pendingCommand;   // gefaehrlicher Befehl, der auf Franks "Ja" wartet (Safe-Stufe)

        public ComputerUseSubAgent(
            ILlmProvider llm,
            Func<ComputerUseMode> mode,
            Func<string, CancellationToken, Task<ShellResult>> executor)
        {
            _llm = llm;
            _mode = mode;
            _executor = executor;
        }

        public string Name => "Computer";
        public string Description => "Steuert auf Wunsch den Rechner: oeffnet Programme, fuehrt Befehle aus, erledigt Dinge am PC.";

        public string HowTo => "Sag z. B. 'oeffne den Taschenrechner' oder 'leg einen Ordner Projekte auf dem Desktop an' — der Befehl wird abgeleitet und vor heiklen Aktionen erst laut bestaetigt. Funktioniert nur, wenn Computer Use in den Einstellungen aktiviert ist (Stufe Sicher oder Vollzugriff).";

        private static readonly string[] Triggers =
        {
            "oeffne", "öffne", "starte", "fuehr", "führ", "ausfuehren", "ausführen", "befehl",
            "auf dem computer", "auf dem rechner", "auf meinem computer", "auf meinem rechner",
            "programm", "anwendung", "terminal", "powershell"
        };

        public bool HandlesOwnConfirmation => true;
        public bool AwaitingFollowup => _pendingCommand != null;

        public bool CanHandle(string task)
        {
            if (_pendingCommand != null) return true;                 // wartet auf "ja"/"nein"
            if (_mode() == ComputerUseMode.Off) return false;          // aus -> nicht zustaendig
            if (string.IsNullOrWhiteSpace(task)) return false;
            var t = task.ToLowerInvariant();
            return Triggers.Any(tr => t.Contains(tr));
        }

        public string ConfirmationQuestion(string task) => "Soll ich das auf deinem Rechner machen?";

        public async Task<string> HandleAsync(string task, CancellationToken ct = default)
        {
            // 1) Folge-Antwort auf einen bestaetigungspflichtigen Befehl?
            if (_pendingCommand != null)
            {
                var cmd = _pendingCommand;
                var decision = ConfirmationDetector.Interpret(task);
                Probe.Decision("Computer-Use-Bestaetigung", decision.ToString(), new { command = cmd });
                _pendingCommand = null;
                return decision switch
                {
                    ConfirmationDetector.Decision.Yes => await ExecuteAsync(cmd!, ct).ConfigureAwait(false),
                    ConfirmationDetector.Decision.No => "Alles klar, ich fuehre den Befehl nicht aus.",
                    _ => "Okay, ich habe den Befehl verworfen. Sag nochmal, was ich tun soll."
                };
            }

            var mode = _mode();
            if (mode == ComputerUseMode.Off)
                return "Computer Use ist ausgeschaltet. Wenn ich deinen Rechner steuern soll, stell es in den Einstellungen auf Sicher oder Vollzugriff.";

            // 2) Befehl aus der Aufgabe ableiten.
            string command;
            try
            {
                var msgs = new[]
                {
                    new LlmMessage(LlmRole.System, DerivePrompt),
                    new LlmMessage(LlmRole.User, task)
                };
                var reply = await _llm.ChatAsync(msgs, ct).ConfigureAwait(false);
                command = ExtractCommand(reply);
            }
            catch (Exception ex)
            {
                Log.Error("ComputerUse: Befehl ableiten fehlgeschlagen", ex);
                return "Ich konnte daraus gerade keinen Befehl ableiten. Versuch es nochmal.";
            }

            if (string.IsNullOrWhiteSpace(command))
                return "Ich konnte daraus keinen ausfuehrbaren Befehl ableiten.";

            // 3) Sicherheitspruefung (Guard entscheidet anhand der Stufe).
            var verdict = CommandGuard.Evaluate(command, mode);
            Probe.Decision("Computer-Use-Pruefung", verdict.ToString(), new { command, mode = mode.ToString() });
            switch (verdict)
            {
                case CommandVerdict.BlockedHardline:
                    return "Nein, das mache ich nicht. Der noetige Befehl waere zu gefaehrlich fuer deinen Rechner.";
                case CommandVerdict.BlockedDisabled:
                    return "Computer Use ist ausgeschaltet.";
                case CommandVerdict.NeedsApproval:
                    _pendingCommand = command;
                    return $"Ich wuerde diesen Befehl ausfuehren: {command}. Das ist heikel. Soll ich das wirklich tun? Sag ja oder nein.";
                default: // Approved
                    return await ExecuteAsync(command, ct).ConfigureAwait(false);
            }
        }

        private async Task<string> ExecuteAsync(string command, CancellationToken ct)
        {
            var result = await _executor(command, ct).ConfigureAwait(false);
            Log.Info("ComputerUse: Befehl ausgefuehrt", new { command, ok = result.Ok, exit = result.ExitCode });
            var head = result.Ok ? "Erledigt." : "Der Befehl lief auf einen Fehler.";
            var outp = result.Output.Length > 400 ? result.Output.Substring(0, 400) + " …" : result.Output;
            return string.IsNullOrWhiteSpace(outp) || outp == "(keine Ausgabe)"
                ? head
                : $"{head} {outp}";
        }

        private const string DerivePrompt =
@"Wandle die Aufgabe des Nutzers in GENAU EINEN ausfuehrbaren PowerShell-Befehl fuer Windows um.
Antworte NUR mit dem Befehl — keine Erklaerung, keine Code-Bloecke, keine Anfuehrungszeichen drumherum.
Beispiele:
- ""oeffne den Editor"" -> Start-Process notepad
- ""zeig die Uhrzeit"" -> Get-Date
- ""mach den Rechner auf"" -> Start-Process calc
Wenn sich daraus kein sinnvoller Befehl bilden laesst, antworte mit einem leeren String.";

        /// <summary>
        /// Holt den reinen Befehl aus der LLM-Antwort (entfernt Code-Fences, nimmt die erste
        /// nicht-leere Zeile). Public static = isoliert testbar.
        /// </summary>
        public static string ExtractCommand(string? reply)
        {
            if (string.IsNullOrWhiteSpace(reply)) return string.Empty;
            var s = reply.Trim();
            // Code-Fences entfernen.
            s = s.Replace("```powershell", "").Replace("```pwsh", "").Replace("```cmd", "").Replace("```", "");
            foreach (var raw in s.Split('\n'))
            {
                var line = raw.Trim().Trim('`').Trim();
                if (line.Length > 0) return line;
            }
            return string.Empty;
        }
    }
}
