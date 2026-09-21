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

    private static readonly System.Collections.Concurrent.ConcurrentDictionary<string, string> VerifiedShells = new(StringComparer.OrdinalIgnoreCase);

    /// <summary>
    /// Store-PowerShell liegt in einem geschützten Paketordner, den WSL-Interop nicht starten darf
    /// (execvpe: Permission denied). Die benutzerbezogene App-Ausführungsverknüpfung startet dagegen.
    /// </summary>
    public static bool IsProtectedPackagePath(string path, string programFiles) =>
        Path.GetFullPath(path).StartsWith(Path.Combine(programFiles, "WindowsApps") + Path.DirectorySeparatorChar, StringComparison.OrdinalIgnoreCase);

    public static string[] PowerShellCandidates(string powerShell, string programFiles, string localAppData)
    {
        var candidates = new System.Collections.Generic.List<string>();
        if (!IsProtectedPackagePath(powerShell, programFiles)) candidates.Add(powerShell);
        if (string.Equals(Path.GetFileName(powerShell), "pwsh.exe", StringComparison.OrdinalIgnoreCase))
        {
            candidates.Add(Path.Combine(localAppData, "Microsoft", "WindowsApps", "pwsh.exe"));
            candidates.Add(Path.Combine(programFiles, "PowerShell", "7", "pwsh.exe"));
        }
        return candidates.Distinct(StringComparer.OrdinalIgnoreCase).ToArray();
    }

    private static string ResolveWslPowerShell(string distro, string powerShell)
    {
        var candidates = PowerShellCandidates(powerShell,
            Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles),
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData));
        var errors = new System.Collections.Generic.List<string>();
        foreach (var candidate in candidates.Where(File.Exists))
        {
            if (VerifiedShells.TryGetValue(distro + "|" + candidate, out var known)) return known;
            try
            {
                var linux = Query("-d", distro, "--exec", "wslpath", "-u", candidate);
                // Echter Startbeweis aus derselben Distribution, nicht nur Dateiexistenz.
                Query("-d", distro, "--exec", linux, "-NoLogo", "-NoProfile", "-NonInteractive", "-Command", "exit 0");
                VerifiedShells[distro + "|" + candidate] = linux;
                return linux;
            }
            catch (IOException error) { errors.Add(candidate + ": " + error.Message); } // nächster Kandidat, Grund bleibt erhalten
        }
        throw new IOException("Keine aus WSL startbare PowerShell gefunden (geprüft: " + string.Join(", ", candidates)
            + (errors.Count > 0 ? "; Fehler: " + string.Join(" | ", errors) : "")
            + "). Die Store-PowerShell im Paketordner ist aus WSL gesperrt; bitte die App-Ausführungsverknüpfung für pwsh aktivieren oder Standard-Terminal wählen.");
    }

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
        var linuxShell = ResolveWslPowerShell(distro, powerShell);
        var session = "openlauncher-" + Guid.NewGuid().ToString("N");
        var wrapper = Path.Combine(Path.GetTempPath(), Path.GetFileNameWithoutExtension(script) + "-tmux.ps1");
        var baseArgs = new[] { "-d", distro, "--exec", tmux, "-L", "openlauncher" };
        // --exec and separate tmux command arguments avoid shell interpretation of paths, quotes and metacharacters.
        var args = new[] { "new-session", "-d", "-s", session, "-x", "__W__", "-y", "__H__",
            "-c", linuxDir, linuxShell, "-NoLogo", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script,
            ";", "set-option", "-g", "mouse", "on",
            ";", "bind-key", "-T", "root", "WheelUpPane", "if-shell", "-F", "#{pane_in_mode}",
            "send-keys -M", "copy-mode -e; send-keys -M",
            ";", "bind-key", "-T", "root", "WheelDownPane", "if-shell", "-F", "#{pane_in_mode}",
            "send-keys -M", "",
            ";", "bind-key", "-T", "root", "WheelUpStatus", "if-shell", "-F", "#{pane_in_mode}",
            "send-keys -X -N 5 scroll-up", "copy-mode -e; send-keys -X -N 5 scroll-up",
            ";", "bind-key", "-T", "root", "WheelDownStatus", "if-shell", "-F", "#{pane_in_mode}",
            "send-keys -X -N 5 scroll-down", "" };
        var tmuxCall = "& " + Literal(Wsl) + " " + string.Join(" ", baseArgs.Select(Literal));
        var target = Literal("=" + session);
        // Erst getrennt anlegen und prüfen, dann anhängen: ein sofort sterbender Pane darf nicht als
        // stilles "[exited]" enden. Derselbe Wrapper hängt eine noch laufende Sitzung wieder an.
        var content = "# tmux-Sitzung: " + session + " | WSL: " + distro + "\n"
            + "$ErrorActionPreference = 'Stop'\n"
            + "$env:TERM = 'xterm-256color'\n"
            + tmuxCall + " 'has-session' '-t' " + target + " 2>$null\n"
            + "if ($LASTEXITCODE -ne 0) {\n"
            + "    try { $w = [Math]::Max(80, [Console]::WindowWidth); $h = [Math]::Max(24, [Console]::WindowHeight) } catch { $w = 160; $h = 48 }\n"
            + "    " + tmuxCall + " " + string.Join(" ", args.Select(Literal)).Replace("'__W__'", "$w").Replace("'__H__'", "$h") + "\n"
            + "    if ($LASTEXITCODE -ne 0) { throw 'tmux-Start fehlgeschlagen. Bitte WSL, tmux und Windows-Interop prüfen.' }\n"
            + "    Start-Sleep -Milliseconds 1500\n"
            + "    " + tmuxCall + " 'has-session' '-t' " + target + " 2>$null\n"
            + "    if ($LASTEXITCODE -ne 0) { throw 'Die CLI hat die tmux-Sitzung sofort beendet. Bitte PowerShell-Start aus WSL und das Startskript prüfen.' }\n"
            + "}\n"
            + tmuxCall + " 'attach-session' '-t' " + target + "\n"
            // tmux meldet bei normalem Sitzungsende und bei Detach 0, bei fehlender Sitzung oder Anhängefehler ungleich 0.
            + "if ($LASTEXITCODE -ne 0) { throw 'Anhängen an die tmux-Sitzung fehlgeschlagen oder Sitzung bereits beendet. Bitte WSL, tmux und das Startskript prüfen.' }\n";
        // Keep the wrapper after detach: the same copied command reattaches to the same session.
        File.WriteAllText(wrapper, content, new UTF8Encoding(false));
        return wrapper;
    }
}
