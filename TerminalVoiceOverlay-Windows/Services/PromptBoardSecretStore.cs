using System;
using System.Collections.Generic;
using System.IO;
using System.Text;

namespace TerminalVoiceOverlay.Services;

/// <summary>
/// File-based secret store for PromptBoard's Google OAuth credentials.
/// Lives at <c>$HOME/SK/PromptBoard/.env</c>, matching the project-wide
/// rule in <c>~/.claude/rules/secrets-in-sk-folder.md</c> — secrets are
/// kept out of the SQLite database (and therefore out of the Google Drive
/// backup JSON) and isolated per machine.
///
/// The .env format is identical to <see cref="Config"/>'s parser so the
/// user can edit the file by hand. Atomic write-to-temp-then-rename
/// avoids corruption mid-write.
/// </summary>
public sealed class PromptBoardSecretStore
{
    public string FilePath { get; }

    public PromptBoardSecretStore()
    {
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        var dir = Path.Combine(home, "SK", "PromptBoard");
        Directory.CreateDirectory(dir);
        FilePath = Path.Combine(dir, ".env");
    }

    public Secrets Load()
    {
        if (!File.Exists(FilePath))
            return Secrets.Empty;

        try
        {
            var env = ParseEnvContents(File.ReadAllText(FilePath, Encoding.UTF8));
            return new Secrets(
                GoogleClientId:           NullIfBlank(Get(env, "GOOGLE_CLIENT_ID")),
                GoogleClientSecret:       NullIfBlank(Get(env, "GOOGLE_CLIENT_SECRET")),
                GoogleOAuthRefreshToken:  NullIfBlank(Get(env, "GOOGLE_OAUTH_REFRESH_TOKEN")),
                GoogleAccountEmail:       NullIfBlank(Get(env, "GOOGLE_ACCOUNT_EMAIL")));
        }
        catch
        {
            // Corrupt or unreadable file — treat as empty, never crash.
            return Secrets.Empty;
        }
    }

    public void Save(Secrets secrets)
    {
        var sb = new StringBuilder();
        sb.AppendLine("# PromptBoard Google OAuth credentials");
        sb.AppendLine("# Auto-managed by TerminalVoiceOverlay-Windows. Edit at your own risk.");
        sb.AppendLine("# Per ~/.claude/rules/secrets-in-sk-folder.md these secrets stay OUT of the DB");
        sb.AppendLine("# and out of the Google Drive backup JSON.");
        sb.AppendLine();
        Append(sb, "GOOGLE_CLIENT_ID",           secrets.GoogleClientId);
        Append(sb, "GOOGLE_CLIENT_SECRET",       secrets.GoogleClientSecret);
        Append(sb, "GOOGLE_OAUTH_REFRESH_TOKEN", secrets.GoogleOAuthRefreshToken);
        Append(sb, "GOOGLE_ACCOUNT_EMAIL",       secrets.GoogleAccountEmail);

        var tmp = FilePath + ".tmp";
        File.WriteAllText(tmp, sb.ToString(), new UTF8Encoding(encoderShouldEmitUTF8Identifier: false));
        // Atomic replace — never leaves a partially-written file behind.
        if (File.Exists(FilePath))
            File.Replace(tmp, FilePath, destinationBackupFileName: null);
        else
            File.Move(tmp, FilePath);
    }

    public bool HasAnyValue()
    {
        var s = Load();
        return !string.IsNullOrEmpty(s.GoogleClientId)
            || !string.IsNullOrEmpty(s.GoogleClientSecret)
            || !string.IsNullOrEmpty(s.GoogleOAuthRefreshToken);
    }

    private static void Append(StringBuilder sb, string key, string? value)
    {
        if (string.IsNullOrEmpty(value))
        {
            sb.Append('#').Append(key).AppendLine("=");
            return;
        }
        sb.Append(key).Append('=').AppendLine(value);
    }

    private static string Get(Dictionary<string, string> env, string key) =>
        env.TryGetValue(key, out var v) ? v : string.Empty;

    private static string? NullIfBlank(string? s) =>
        string.IsNullOrWhiteSpace(s) ? null : s.Trim();

    // Mirrors Config.ParseEnvContents — kept local so this file has
    // no inter-module dependencies and stays trivially testable.
    private static Dictionary<string, string> ParseEnvContents(string contents)
    {
        var result = new Dictionary<string, string>();
        foreach (var rawLine in contents.Split('\n'))
        {
            var line = rawLine.Trim();
            if (string.IsNullOrEmpty(line) || line.StartsWith('#'))
                continue;

            var eqIndex = line.IndexOf('=');
            if (eqIndex <= 0) continue;

            var key = line.Substring(0, eqIndex).Trim();
            var value = line.Substring(eqIndex + 1).Trim();

            if (value.Length >= 2 &&
                ((value.StartsWith('"') && value.EndsWith('"')) ||
                 (value.StartsWith('\'') && value.EndsWith('\''))))
            {
                value = value.Substring(1, value.Length - 2);
            }

            result[key] = value;
        }
        return result;
    }
}

public sealed record Secrets(
    string? GoogleClientId,
    string? GoogleClientSecret,
    string? GoogleOAuthRefreshToken,
    string? GoogleAccountEmail)
{
    public static Secrets Empty { get; } = new(null, null, null, null);
}
