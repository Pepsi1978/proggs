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

    public static string BuildStartScript(string script, string workDir, string powerShell, string socket = "openlauncher")
    {
        if (!File.Exists(script) || !File.Exists(powerShell) || !Directory.Exists(workDir))
            throw new IOException("tmux benötigt ein vorhandenes Startskript, PowerShell und Arbeitsverzeichnis.");
        if (!File.Exists(Wsl))
            throw new IOException("WSL fehlt. Bitte Ubuntu mit tmux einrichten oder Standard-Terminal wählen.");
        // Pin the actual default distribution, so Docker or a later default change cannot silently redirect an attachment.
        var distro = Query("--exec", "printenv", "WSL_DISTRO_NAME");
        if (string.IsNullOrWhiteSpace(distro) || distro.StartsWith("docker-", StringComparison.OrdinalIgnoreCase))
            throw new IOException("Bitte Ubuntu oder eine andere Benutzer-Distribution als WSL-Standard festlegen.");
        var tmux = LinuxProgram(distro, "tmux");
        var sleep = LinuxProgram(distro, "sleep");
        // Instanzweiter Interop-Socket von WSL-init: unabhängig von jedem einzelnen wsl.exe-Aufruf.
        try { Query("-d", distro, "--exec", "test", "-e", InstanceInterop); }
        catch (IOException) { throw new IOException("WSL-Interop " + InstanceInterop + " fehlt in " + distro + ". Bitte WSL aktualisieren oder Standard-Terminal wählen."); }
        var linuxDir = Query("-d", distro, "--exec", "wslpath", "-u", workDir);
        var linuxShell = ResolveWslPowerShell(distro, powerShell);
        var session = "openlauncher-" + Guid.NewGuid().ToString("N");
        foreach (var token in new[] { distro, tmux, sleep, socket })
            if (!IsPlainToken(token)) throw new IOException("Unerwartetes Zeichen in WSL-Angabe: " + token);
        var wrapper = Path.Combine(Path.GetTempPath(), Path.GetFileNameWithoutExtension(script) + "-tmux.ps1");
        var mouse = new[] { "set-option", "-g", "mouse", "on",
            ";", "bind-key", "-T", "root", "WheelUpPane", "if-shell", "-F", "#{pane_in_mode}",
            "send-keys -M", "copy-mode -e; send-keys -M",
            ";", "bind-key", "-T", "root", "WheelDownPane", "if-shell", "-F", "#{pane_in_mode}",
            "send-keys -M", "",
            ";", "bind-key", "-T", "root", "WheelUpStatus", "if-shell", "-F", "#{pane_in_mode}",
            "send-keys -X -N 5 scroll-up", "copy-mode -e; send-keys -X -N 5 scroll-up",
            ";", "bind-key", "-T", "root", "WheelDownStatus", "if-shell", "-F", "#{pane_in_mode}",
            "send-keys -X -N 5 scroll-down", "" };
        var cli = new[] { "-c", linuxDir, "-e", "WSL_INTEROP=" + InstanceInterop,
            linuxShell, "-NoLogo", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script };
        File.WriteAllText(wrapper, WrapperScript(session, distro, tmux, socket, sleep, mouse, cli, CliPattern(script)), new UTF8Encoding(false));
        // Keep the wrapper after detach: the same copied command reattaches to the same session.
        return wrapper;
    }

    public const string InstanceInterop = "/run/WSL/1_interop";
    public const string BootstrapWindow = "openlauncher-bootstrap";
    public const string CliWindow = "openlauncher-cli";
    public const int MaxStartAttempts = 3;

    private static string LinuxProgram(string distro, string name)
    {
        try { return Query("-d", distro, "--exec", "which", name); }
        catch (IOException) { throw new IOException(name + " fehlt in " + distro + ". Bitte " + name + " installieren oder Standard-Terminal wählen."); }
    }

    /// <summary>Der Halteprozess wird per Start-Process übergeben; nur Zeichen ohne Quoting-Bedarf sind dort erlaubt.</summary>
    public static bool IsPlainToken(string token) =>
        token.Length > 0 && token.All(c => char.IsAsciiLetterOrDigit(c) || "._/+-=".Contains(c));

    /// <summary>Genau der innere Skriptname als letztes Pfadglied, nicht der Wrapper (*-tmux.ps1) oder ein längerer Name.</summary>
    public static string CliPattern(string script) =>
        @"[\\/]" + System.Text.RegularExpressions.Regex.Escape(Path.GetFileName(script)) + @"(?=[""'\s]|$)";

    /// <summary>
    /// Ablauf je Versuch: ein versteckter wsl.exe-Halteprozess legt die Sitzung mit einem reinen Linux-Bootstrap-Fenster an
    /// und hält WSL bis zur Freigabe aktiv. Die CLI startet in einem eigenen Fenster über den instanzweiten Interop-Socket,
    /// damit ihr Windows-Prozess weder vom Ende eines wsl.exe noch von dessen Konsole abhängt. Erst wenn CIM die echte
    /// CLI-PowerShell über den exakten Skriptnamen bestätigt, verschwindet das Bootstrap-Fenster und der Wrapper hängt an;
    /// sonst wird die Zielsitzung beendet und nach Backoff neu gestartet.
    /// </summary>
    private static string WrapperScript(string session, string distro, string tmux, string socket, string sleep, string[] mouse, string[] cli, string pattern)
    {
        static string List(System.Collections.Generic.IEnumerable<string> items) => "@(" + string.Join(", ", items.Select(Literal)) + ")";
        return $$"""
# tmux-Sitzung: {{session}} | WSL: {{distro}} | Socket: {{socket}}
$ErrorActionPreference = 'Stop'
$env:TERM = 'xterm-256color'
$wsl = {{Literal(Wsl)}}
$tmux = {{List(new[] { "-d", distro, "--exec", tmux, "-L", socket })}}
$session = {{Literal(session)}}
$target = '=' + $session
$bootstrap = $target + ':=' + {{Literal(BootstrapWindow)}}
$cliWindow = $target + ':=' + {{Literal(CliWindow)}}
$mouse = {{List(mouse)}}
$cli = {{List(cli)}}
$cliPattern = {{Literal(pattern)}}
function Invoke-Tmux { & $wsl @tmux @args 2>$null | Out-Null; $LASTEXITCODE }
function Get-CliPid {
    @(Get-CimInstance Win32_Process -Filter "Name='pwsh.exe' OR Name='powershell.exe'" -ErrorAction SilentlyContinue |
        Where-Object { $_.CommandLine -match $cliPattern } | Select-Object -First 1).ProcessId
}
$windows = @(& $wsl @tmux 'list-windows' '-t' $target '-F' '#{window_name}' 2>$null)
if ($LASTEXITCODE -ne 0 -or $windows -contains {{Literal(BootstrapWindow)}}) {
    $confirmed = $false
    for ($attempt = 1; $attempt -le {{MaxStartAttempts}} -and -not $confirmed; $attempt++) {
        if ($attempt -gt 1) {
            Write-Host "tmux: CLI nicht bestätigt, Neustart $attempt/{{MaxStartAttempts}}"
            Start-Sleep -Seconds (2 * ($attempt - 1))
        }
        $null = Invoke-Tmux 'kill-session' '-t' $target
        try { $w = [Math]::Max(80, [Console]::WindowWidth); $h = [Math]::Max(24, [Console]::WindowHeight) } catch { $w = 160; $h = 48 }
        $release = "$session-frei-$attempt"
        $holder = Start-Process -FilePath $wsl -WindowStyle Hidden -PassThru -ArgumentList ($tmux + @('new-session', '-d', '-s', $session, '-x', "$w", '-y', "$h",
            '-n', {{Literal(BootstrapWindow)}}, {{Literal(sleep)}}, 'infinity', ';', 'wait-for', $release))
        try {
            $deadline = (Get-Date).AddSeconds(10)
            while ((Invoke-Tmux 'has-session' '-t' $target) -ne 0) {
                if ($holder.HasExited -or (Get-Date) -gt $deadline) { break }
                Start-Sleep -Milliseconds 200
            }
            if ((Invoke-Tmux 'has-session' '-t' $target) -ne 0) { continue }
            $null = Invoke-Tmux @mouse
            if ((Invoke-Tmux 'new-window' '-t' $target '-n' {{Literal(CliWindow)}} @cli) -ne 0) { continue }
            $deadline = (Get-Date).AddSeconds(15)
            while ((Get-Date) -lt $deadline) {
                $cliPid = Get-CliPid
                if ($cliPid) {
                    # Kurzlebige Treffer (sofort endendes Startskript) zählen nicht als Start.
                    Start-Sleep -Milliseconds 1500
                    if (Get-Process -Id $cliPid -ErrorAction SilentlyContinue) { $confirmed = $true }
                    break
                }
                if ((Invoke-Tmux 'list-panes' '-t' $cliWindow) -ne 0) { break }
                Start-Sleep -Milliseconds 500
            }
        } finally {
            if ($confirmed) { $null = Invoke-Tmux 'kill-window' '-t' $bootstrap }
            else { $null = Invoke-Tmux 'kill-session' '-t' $target }
            $null = Invoke-Tmux 'wait-for' '-S' $release
            if (-not $holder.WaitForExit(5000)) { Stop-Process -Id $holder.Id -Force -ErrorAction SilentlyContinue }
        }
    }
    if (-not $confirmed) { throw 'Die CLI-PowerShell wurde nach {{MaxStartAttempts}} tmux-Startversuchen nicht bestätigt. Bitte WSL-Interop und das Startskript prüfen.' }
}
& $wsl @tmux 'attach-session' '-t' $target
if ($LASTEXITCODE -ne 0) { throw 'Anhängen an die tmux-Sitzung fehlgeschlagen oder Sitzung bereits beendet. Bitte WSL, tmux und das Startskript prüfen.' }

""";
    }
}
