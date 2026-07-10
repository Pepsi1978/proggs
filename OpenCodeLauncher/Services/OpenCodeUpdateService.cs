using System.Diagnostics;
using System.IO;
using System.Text.Json;

namespace OpenCodeLauncher.Services;

public sealed record OpenCodeUpdateResult(string Status, string Message, int ExitCode);

public sealed class OpenCodeUpdateService
{
    public async Task<OpenCodeUpdateResult> CheckAsync()
    {
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        var script = Path.Combine(home, "proggs", "opencode-setup", "build-install-windows-mousefix.ps1");
        if (!File.Exists(script)) return new("unavailable", "Update-Skript nicht gefunden.", 0);

        var psi = new ProcessStartInfo
        {
            FileName = "pwsh.exe",
            UseShellExecute = false,
            CreateNoWindow = true,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            WorkingDirectory = Path.GetDirectoryName(script)!
        };
        psi.ArgumentList.Add("-NoLogo");
        psi.ArgumentList.Add("-NoProfile");
        psi.ArgumentList.Add("-ExecutionPolicy");
        psi.ArgumentList.Add("Bypass");
        psi.ArgumentList.Add("-File");
        psi.ArgumentList.Add(script);

        try
        {
            using var process = Process.Start(psi) ?? throw new InvalidOperationException("Update-Prozess konnte nicht gestartet werden.");
            var stdoutTask = process.StandardOutput.ReadToEndAsync();
            var stderrTask = process.StandardError.ReadToEndAsync();
            await process.WaitForExitAsync();
            var stdout = (await stdoutTask).Trim();
            var stderr = (await stderrTask).Trim();
            var state = ReadState(home);
            var status = stdout.StartsWith("OPENCODE_UPDATE_STATUS=deferred", StringComparison.Ordinal)
                ? "deferred"
                : stdout.StartsWith("OPENCODE_UPDATE_STATUS=busy", StringComparison.Ordinal)
                    ? "busy"
                    : state.Status ?? (process.ExitCode == 0 ? "completed" : "failed");
            var message = status == "deferred"
                ? "Die tägliche OpenCode-Updateprüfung ist noch nicht fällig."
                : status == "busy"
                    ? "Eine OpenCode-Updateprüfung läuft bereits."
                : state.Message ?? (process.ExitCode == 0 ? stdout : stderr);
            Logger.Instance.Info("OpenCodeUpdateService", "CheckAsync", "Windows-Fix-Update abgeschlossen",
                new { status, message, process.ExitCode, stdout, stderr });
            return new(status, message, process.ExitCode);
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("OpenCodeUpdateService", "CheckAsync", $"Updateprüfung nicht verfügbar: {ex.Message}");
            return new("failed", ex.Message, -1);
        }
    }

    private static (string? Status, string? Message) ReadState(string home)
    {
        var path = Path.Combine(home, ".local", "share", "opencode-mousefix", "update-state.json");
        try
        {
            using var json = JsonDocument.Parse(File.ReadAllText(path));
            var root = json.RootElement;
            return (
                root.TryGetProperty("status", out var status) ? status.GetString() : null,
                root.TryGetProperty("message", out var message) ? message.GetString() : null
            );
        }
        catch (Exception ex)
        {
            Logger.Instance.Warn("OpenCodeUpdateService", "ReadState", $"Update-State konnte nicht gelesen werden: {ex.Message}");
            return (null, null);
        }
    }
}
