using System;
using System.IO;

namespace ClaudeVoiceOverlay.Services;

/// <summary>
/// Die 10 Schnell-Prompts hinter den Zahlen-Kacheln 1-10 (Frank-Wunsch
/// 2026-09-11). Linksklick fuegt den Prompt in die Befehlszeile ein,
/// Rechtsklick -> "Prompt bearbeiten". Liegen als Textdateien im SK-Ordner,
/// damit TVO und CVO auf einem Rechner dieselben Prompts teilen.
/// </summary>
public static class QuickPromptStore
{
    public const int Count = 10;

    private static string Dir
        => Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
                        "SK", "VoiceOverlays");

    private static string PathFor(int slot) => Path.Combine(Dir, $"quick-prompt-{slot:D2}.txt");

    public static string Load(int slot)
    {
        try
        {
            var p = PathFor(slot);
            return File.Exists(p) ? File.ReadAllText(p) : string.Empty;
        }
        catch { return string.Empty; }
    }

    public static void Save(int slot, string text)
    {
        Directory.CreateDirectory(Dir);
        File.WriteAllText(PathFor(slot), text ?? string.Empty);
    }

    /// <summary>Kurzvorschau fuer den Tooltip der Kachel.</summary>
    public static string Preview(int slot)
    {
        var text = Load(slot).Trim().Replace("\r", " ").Replace("\n", " ");
        if (text.Length == 0) return $"Prompt {slot}: leer — Rechtsklick → Prompt bearbeiten";
        return text.Length > 80 ? $"Prompt {slot}: {text[..80]}…" : $"Prompt {slot}: {text}";
    }
}
