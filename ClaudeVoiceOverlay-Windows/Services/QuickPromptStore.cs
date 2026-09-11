using System;
using System.Collections.Generic;
using System.IO;
using System.Text;

namespace ClaudeVoiceOverlay.Services;

/// <summary>
/// Die 10 Schnell-Prompts hinter den Zahlen-Kacheln 1-10 (Frank-Wunsch
/// 2026-09-11). Linksklick fuegt den Prompt in die Befehlszeile ein,
/// Rechtsklick -> "Prompt bearbeiten". Liegen als Textdateien im SK-Ordner,
/// damit TVO und CVO auf einem Rechner dieselben Prompts teilen. Dateinamen
/// und Fingerabdruck sind identisch zu macOS (gemeinsames Drive-Bundle).
///
/// Zu jedem Prompt gehoert eine Gemini-Kurzbeschreibung (max. 10 Woerter) fuer
/// den Tooltip. Sie steht mit dem Fingerabdruck des Prompt-Texts in
/// quick-prompt-XX-summary.txt — aendert sich der Prompt, passt der Abdruck
/// nicht mehr und die Beschreibung wird neu erzeugt.
/// </summary>
public static class QuickPromptStore
{
    public const int Count = 10;

    private static string Dir
        => Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
                        "SK", "VoiceOverlays");

    private static string FileName(int slot) => $"quick-prompt-{slot:D2}.txt";
    private static string SummaryFileName(int slot) => $"quick-prompt-{slot:D2}-summary.txt";

    /// <summary>Alle Dateinamen fuer das Drive-Bundle (Prompts + Kurzbeschreibungen).</summary>
    public static IEnumerable<string> FileNames()
    {
        for (int slot = 1; slot <= Count; slot++)
        {
            yield return FileName(slot);
            yield return SummaryFileName(slot);
        }
    }

    public static bool IsQuickPromptFile(string name) =>
        name.StartsWith("quick-prompt-", StringComparison.OrdinalIgnoreCase);

    public static string Load(int slot)
    {
        try
        {
            var p = Path.Combine(Dir, FileName(slot));
            return File.Exists(p) ? File.ReadAllText(p) : string.Empty;
        }
        catch { return string.Empty; }
    }

    public static void Save(int slot, string text)
    {
        Directory.CreateDirectory(Dir);
        File.WriteAllText(Path.Combine(Dir, FileName(slot)), text ?? string.Empty);
    }

    /// <summary>FNV-1a 64 ueber den normalisierten Text — identisch zu macOS.</summary>
    private static string Fingerprint(string text)
    {
        var normalized = (text ?? string.Empty).Replace("\r\n", "\n").Trim();
        ulong hash = 0xcbf29ce484222325;
        foreach (byte b in Encoding.UTF8.GetBytes(normalized))
        {
            hash ^= b;
            hash *= 0x100000001b3;
        }
        return hash.ToString("x16");
    }

    // Erste Zeile der Ueberschriften-Datei: Fingerabdruck des Prompts (KI-
    // Ueberschrift, gilt nur solange der Prompt gleich bleibt) oder "manual"
    // (vom Benutzer vergeben, gilt immer und wird nie von der KI ersetzt).
    private const string ManualMarker = "manual";

    private static string[]? ReadSummaryParts(int slot)
    {
        var p = Path.Combine(Dir, SummaryFileName(slot));
        if (!File.Exists(p)) return null;
        var parts = File.ReadAllText(p).Replace("\r\n", "\n").Split('\n', 2);
        return parts.Length == 2 ? parts : null;
    }

    /// <summary>Gueltige Ueberschrift (eigene oder zum Prompt passende KI-Ueberschrift); sonst null.</summary>
    public static string? LoadSummary(int slot)
    {
        try
        {
            var text = Load(slot);
            if (string.IsNullOrWhiteSpace(text)) return null;
            var parts = ReadSummaryParts(slot);
            if (parts == null) return null;
            var head = parts[0].Trim();
            if (head != ManualMarker && head != Fingerprint(text)) return null;
            var summary = parts[1].Trim();
            return summary.Length == 0 ? null : summary;
        }
        catch { return null; }
    }

    /// <summary>True, wenn der Benutzer die Ueberschrift selbst vergeben hat.</summary>
    public static bool IsManualTitle(int slot)
    {
        try { return ReadSummaryParts(slot)?[0].Trim() == ManualMarker; }
        catch { return false; }
    }

    /// <summary>KI-Ueberschrift speichern (an den Prompt-Text gebunden).</summary>
    public static void SaveSummary(int slot, string sourceText, string summary)
    {
        Directory.CreateDirectory(Dir);
        File.WriteAllText(Path.Combine(Dir, SummaryFileName(slot)),
            Fingerprint(sourceText) + "\n" + (summary ?? string.Empty).Trim());
    }

    /// <summary>Eigene Ueberschrift speichern — bleibt, bis der Benutzer sie aendert oder leert.</summary>
    public static void SaveManualTitle(int slot, string title)
    {
        Directory.CreateDirectory(Dir);
        File.WriteAllText(Path.Combine(Dir, SummaryFileName(slot)),
            ManualMarker + "\n" + (title ?? string.Empty).Trim());
    }

    /// <summary>Ueberschrift verwerfen — die KI vergibt beim naechsten Auffrischen eine neue.
    /// Leere Datei statt Loeschen, damit das Drive-Bundle den Stand mitnimmt.</summary>
    public static void ClearSummary(int slot)
    {
        Directory.CreateDirectory(Dir);
        File.WriteAllText(Path.Combine(Dir, SummaryFileName(slot)), string.Empty);
    }

    /// <summary>Tooltip, solange keine Ueberschrift da ist.</summary>
    public static string Preview(int slot) =>
        string.IsNullOrWhiteSpace(Load(slot))
            ? $"Prompt {slot}: leer — Rechtsklick → Prompt bearbeiten"
            : $"Prompt {slot}";
}
