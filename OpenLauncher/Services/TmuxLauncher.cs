using System;
using System.Linq;
using System.Diagnostics;
using System.IO;
using System.Text;

namespace OpenLauncher.Services;

/// <summary>WSL stellt tmux bereit; die CLI bleibt mit ihrem Profil ein Windows-Prozess.</summary>
public static class TmuxLauncher
{
    private static string Wsl => Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.System), "wsl.exe");

    private static string Query(params string[] args)
    {
        var start = new ProcessStartInfo(Wsl)
        {
            UseShellExecute = false, CreateNoWindow = true,
            RedirectStandardOutput = true, RedirectStandardError = true,
            StandardOutputEncoding = Encoding.UTF8, StandardErrorEncoding = Encoding.UTF8
        };
        foreach (var arg in args) start.ArgumentList.Add(arg);
        using var process = Process.Start(start) ?? throw new IOException("WSL konnte nicht gestartet werden.");
        var stdout = process.StandardOutput.ReadToEndAsync();
        var stderr = process.StandardError.ReadToEndAsync();
        if (!process.WaitForExit(15000))
        {
            process.Kill(entireProcessTree: true);
            throw new IOException("WSL antwortet nicht. Bitte die Linux-Distribution prüfen oder Standard-Terminal wählen.");
        }
        var result = stdout.GetAwaiter().GetResult().Trim();
        if (process.ExitCode != 0)
            throw new IOException("tmux-Start nicht möglich: " + stderr.GetAwaiter().GetResult().Trim());
        return result;
    }

    private static string Literal(string text) => "'" + text.Replace("'", "''") + "'";

    public static string BuildStartScript(string script, string workDir, string powerShell)
    {
        if (!File.Exists(script) || !File.Exists(powerShell) || !Directory.Exists(workDir))
            throw new IOException("tmux benötigt ein vorhandenes Startskript, PowerShell und Arbeitsverzeichnis.");
        if (!File.Exists(Wsl))
            throw new IOException("WSL fehlt. Bitte Ubuntu mit tmux einrichten oder Standard-Terminal wählen.");
        // Pin the actual default distribution, so Docker or a later default change cannot silently redirect an attachment.
        var distro = Query("--exec", "printenv", "WSL_DISTRO_NAME");
        if (string.IsNullOrWhiteSpace(distro) || distro.StartsWith("docker-", StringComparison.OrdinalIgnoreCase))
            throw new IOException("Bitte Ubuntu oder eine andere Benutzer-Distribution als WSL-Standard festlegen.");
        var tmux = Query("-d", distro, "--exec", "sh", "-c", "command -v tmux");
        if (string.IsNullOrWhiteSpace(tmux)) throw new IOException("tmux fehlt in " + distro + ". Bitte tmux installieren oder Standard-Terminal wählen.");
        var linuxDir = Query("-d", distro, "--exec", "wslpath", "-u", workDir);
        var linuxShell = Query("-d", distro, "--exec", "wslpath", "-u", powerShell);
        var session = "openlauncher-" + Guid.NewGuid().ToString("N");
        var wrapper = Path.Combine(Path.GetTempPath(), Path.GetFileNameWithoutExtension(script) + "-tmux.ps1");
        // --exec and separate tmux command arguments avoid shell interpretation of paths, quotes and metacharacters.
        var args = new[] { "-d", distro, "--exec", tmux, "-L", "openlauncher", "new-session", "-A", "-s", session,
            "-c", linuxDir, linuxShell, "-NoLogo", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script };
        var content = "# tmux-Sitzung: " + session + " | WSL: " + distro + "\n"
            + "$ErrorActionPreference = 'Stop'\n"
            + "$env:TERM = 'xterm-256color'\n"
            + "& " + Literal(Wsl) + " " + string.Join(" ", args.Select(Literal)) + "\n"
            + "if ($LASTEXITCODE -ne 0) { throw 'tmux-Start fehlgeschlagen. Bitte WSL, tmux und Windows-Interop prüfen.' }\n";
        // Keep the wrapper after detach: the same copied command reattaches to the same session.
        File.WriteAllText(wrapper, content, new UTF8Encoding(false));
        return wrapper;
    }
}
