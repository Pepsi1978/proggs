using System.Text.RegularExpressions;

namespace VoiceAgent.Core
{
    /// <summary>Die drei Computer-Use-Stufen (nach Hermes approvals.mode), in den Einstellungen waehlbar.</summary>
    public enum ComputerUseMode
    {
        /// <summary>Aus: der Agent steuert den Rechner gar nicht.</summary>
        Off,
        /// <summary>Sicher: gefaehrliche Befehle werden per gesprochenem "Ja" bestaetigt; harmlose laufen direkt.</summary>
        Safe,
        /// <summary>Vollzugriff: alles laeuft direkt — AUSSER der Hardline-Blocklist (nie umgehbar).</summary>
        Full
    }

    /// <summary>Urteil der Befehlspruefung.</summary>
    public enum CommandVerdict
    {
        /// <summary>Darf direkt ausgefuehrt werden.</summary>
        Approved,
        /// <summary>Gefaehrlich — in der Stufe "Sicher" erst nach gesprochenem "Ja" ausfuehren.</summary>
        NeedsApproval,
        /// <summary>Katastrophal (Hardline) — NIE ausfuehren, egal welche Stufe.</summary>
        BlockedHardline,
        /// <summary>Computer Use ist ausgeschaltet (Stufe "Aus").</summary>
        BlockedDisabled
    }

    /// <summary>
    /// Sicherheits-Kern fuer Computer Use (nachgebaut nach Hermes tools/approval.py): prueft einen
    /// auszufuehrenden Shell-Befehl gegen zwei Tiers — HARDLINE (katastrophal, nie erlaubt, auch
    /// nicht bei Vollzugriff) und DANGEROUS (gefaehrlich-aber-erholbar, in der Stufe "Sicher"
    /// bestaetigungspflichtig). Vor dem Matching wird der Befehl NORMALISIERT (Verschleierung wie
    /// Caret-/Backtick-Escapes, leere Quotes, ANSI), damit triviale Bypaesse nicht durchrutschen.
    ///
    /// Windows-spezifisch (cmd + PowerShell). Public static = isoliert testbar. Das Gate trifft KEINE
    /// Ausfuehrung — es liefert nur das Urteil; der ComputerUseSubAgent setzt es um.
    /// </summary>
    public static class CommandGuard
    {
        // Katastrophal: System unbrauchbar / Daten unwiederbringlich. NIE — auch nicht bei Vollzugriff.
        private static readonly Regex[] Hardline =
        {
            new(@"\bformat\b\s*/?\w*\s*[a-z]:", RegexOptions.Compiled),          // format c:
            new(@"\bformat-volume\b", RegexOptions.Compiled),
            new(@"\b(shutdown|stop-computer|restart-computer|logoff)\b", RegexOptions.Compiled),
            new(@"\bdiskpart\b", RegexOptions.Compiled),
            new(@"\bclear-disk\b", RegexOptions.Compiled),
            new(@"\bcipher\b\s*/w", RegexOptions.Compiled),                       // sicheres Ueberschreiben
            new(@"\bbcdedit\b.*\bdelete", RegexOptions.Compiled),                 // Booteintraege loeschen
            new(@"\b(del|erase|rd|rmdir|remove-item|ri)\b.*(c:\\windows|system32|\$env:windir|%windir%)", RegexOptions.Compiled),
            new(@"\b(rd|rmdir)\b\s*/s\b[^a-z0-9]*c:\\?(\s|$|\*)", RegexOptions.Compiled),  // rmdir /s c:\
            new(@"\bdel\b\s*/[sq]\b.*\bc:\\?(\s|$|\*)", RegexOptions.Compiled),
            new(@"remove-item\b.*-recurse\b.*(c:\\?(\s|$|\*)|\$env:windir|system32)", RegexOptions.Compiled),
            new(@"%0\s*\|\s*%0", RegexOptions.Compiled),                          // cmd fork bomb
            new(@":\s*\(\s*\)\s*\{.*\}\s*;", RegexOptions.Compiled),              // bash-artige fork bomb
            new(@"\bwmic\b.*\bos\b.*\b(delete|call)\b", RegexOptions.Compiled),
            new(@"\breg\b\s+delete\s+hk(lm|ey_local_machine)\\?(\s|$)", RegexOptions.Compiled),  // ganze HKLM-Wurzel
        };

        // Gefaehrlich-aber-erholbar: in "Sicher" bestaetigungspflichtig, bei "Vollzugriff" ohne Nachfrage.
        private static readonly Regex[] Dangerous =
        {
            new(@"\b(del|erase)\b\s*/[sq]", RegexOptions.Compiled),               // rekursives Loeschen
            new(@"\b(rd|rmdir)\b\s*/s", RegexOptions.Compiled),
            new(@"\bremove-item\b.*-recurse", RegexOptions.Compiled),
            new(@"\bremove-item\b.*-force", RegexOptions.Compiled),
            new(@"\bformat\b", RegexOptions.Compiled),
            new(@"\breg\b\s+(delete|add)\b", RegexOptions.Compiled),
            new(@"\bnet\b\s+(user|localgroup)\b", RegexOptions.Compiled),
            new(@"\b(sc|stop-service|set-service|new-service|remove-service)\b", RegexOptions.Compiled),
            new(@"\b(schtasks|register-scheduledtask|unregister-scheduledtask)\b", RegexOptions.Compiled),
            new(@"\b(taskkill|stop-process|kill)\b", RegexOptions.Compiled),
            new(@"\b(icacls|cacls|takeown)\b", RegexOptions.Compiled),
            new(@"\bset-executionpolicy\b", RegexOptions.Compiled),
            new(@"\b(invoke-expression|iex)\b", RegexOptions.Compiled),
            new(@"\b(invoke-webrequest|iwr|curl|wget)\b.*\|\s*(iex|invoke-expression)", RegexOptions.Compiled),
            new(@"\bstart-process\b.*-verb\s+runas", RegexOptions.Compiled),      // Elevation
            new(@"\bgit\b\s+reset\b.*--hard", RegexOptions.Compiled),
            new(@"\bgit\b\s+push\b.*--force", RegexOptions.Compiled),
            new(@"\bgit\b\s+clean\b.*-[a-z]*f", RegexOptions.Compiled),
            new(@"\b(attrib|fsutil)\b", RegexOptions.Compiled),
            new(@"\bdrop\s+(table|database)\b", RegexOptions.Compiled),
            new(@"\bdelete\s+from\b(?!.*\bwhere\b)", RegexOptions.Compiled),       // DELETE ohne WHERE
            new(@"\b(new-item|move-item|copy-item)\b.*(\$env:windir|system32|c:\\windows)", RegexOptions.Compiled),
        };

        /// <summary>Parst den Einstellungs-String ("off"/"safe"/"full") in die Stufe. Unbekannt => Off (sicher).</summary>
        public static ComputerUseMode ParseMode(string? mode) => mode?.Trim().ToLowerInvariant() switch
        {
            "safe" => ComputerUseMode.Safe,
            "full" => ComputerUseMode.Full,
            _ => ComputerUseMode.Off,
        };

        /// <summary>Beurteilt einen Befehl unter der gegebenen Stufe. Trifft KEINE Ausfuehrung.</summary>
        public static CommandVerdict Evaluate(string? command, ComputerUseMode mode)
        {
            if (string.IsNullOrWhiteSpace(command)) return CommandVerdict.BlockedDisabled;

            var norm = Normalize(command);

            // Hardline gilt IMMER zuerst — auch Vollzugriff darf das nie.
            foreach (var p in Hardline)
                if (p.IsMatch(norm)) return CommandVerdict.BlockedHardline;

            if (mode == ComputerUseMode.Off) return CommandVerdict.BlockedDisabled;
            if (mode == ComputerUseMode.Full) return CommandVerdict.Approved;   // alles ausser Hardline

            // Stufe "Sicher": gefaehrlich -> bestaetigen, sonst direkt.
            foreach (var p in Dangerous)
                if (p.IsMatch(norm)) return CommandVerdict.NeedsApproval;
            return CommandVerdict.Approved;
        }

        /// <summary>True, wenn der Befehl in JEDER Stufe blockiert ist (Hardline). Public = testbar.</summary>
        public static bool IsHardline(string? command)
        {
            if (string.IsNullOrWhiteSpace(command)) return false;
            var norm = Normalize(command);
            foreach (var p in Hardline)
                if (p.IsMatch(norm)) return true;
            return false;
        }

        /// <summary>
        /// Normalisiert den Befehl fuers Matching (nicht fuer die Ausfuehrung): entfernt
        /// Verschleierungs-Tricks (cmd-Caret ^, PowerShell-Backtick `, leere Quotes, ANSI, Nullbytes)
        /// und vereinheitlicht Whitespace + Gross-/Kleinschreibung. Public static = testbar.
        /// </summary>
        public static string Normalize(string command)
        {
            var s = command;
            s = Regex.Replace(s, @"\x1b\[[0-9;]*m", "");   // ANSI-Escapes
            s = s.Replace("\0", "");                        // Nullbytes
            s = s.Replace("^", "");                          // cmd-Escape r^m -> rm
            s = s.Replace("`", "");                          // PowerShell-Escape `r`m -> rm
            s = s.Replace("\"", "").Replace("'", "");       // leere/obfuskierende Quotes r""m -> rm
            s = Regex.Replace(s, @"\s+", " ").Trim();
            return s.ToLowerInvariant();
        }
    }
}
