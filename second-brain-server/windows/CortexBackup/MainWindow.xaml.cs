using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net.Sockets;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using System.Windows;
using Microsoft.Win32;

namespace CortexBackup;

public partial class MainWindow : Window
{
    // Konfig liegt AUSSERHALB des Repos (secrets-in-sk-folder-Regel): %USERPROFILE%\SK\second-brain\backup.env
    private static readonly string SkDir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), "SK", "second-brain");
    private static readonly string EnvFile = Path.Combine(SkDir, "backup.env");

    private Dictionary<string, string> _cfg = new(StringComparer.OrdinalIgnoreCase);
    private bool _busy;

    public MainWindow()
    {
        InitializeComponent();
        LoadConfig();
        RefreshUi();
    }

    // ── Konfiguration laden/speichern ───────────────────────────────────────────────────────────
    private void LoadConfig()
    {
        _cfg = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        try
        {
            if (File.Exists(EnvFile))
                foreach (var line in File.ReadAllLines(EnvFile))
                {
                    var s = line.Trim();
                    if (s.Length == 0 || s.StartsWith('#')) continue;
                    var i = s.IndexOf('=');
                    if (i > 0) _cfg[s[..i].Trim()] = Environment.ExpandEnvironmentVariables(s[(i + 1)..].Trim());
                }
        }
        catch { /* fehlende/kaputte Konfig -> RefreshUi zeigt "nicht eingerichtet" */ }
    }

    private string Get(string key, string fallback = "") =>
        _cfg.TryGetValue(key, out var v) && !string.IsNullOrWhiteSpace(v) ? v : fallback;

    private string DestDir => Get("LOCAL_DEST",
        Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), "CortexBackup"));

    private void SaveConfig()
    {
        Directory.CreateDirectory(SkDir);
        var sb = new StringBuilder();
        sb.AppendLine("# Cortex-Backup-Konfiguration (von der App verwaltet) — liegt ausserhalb des Repos.");
        foreach (var k in new[] { "SERVER_HOST", "SSH_USER", "SSH_PORT", "SSH_KEY", "REMOTE_APP_DIR", "LOCAL_DEST" })
            if (_cfg.TryGetValue(k, out var v) && !string.IsNullOrWhiteSpace(v))
                sb.AppendLine($"{k}={v}");
        File.WriteAllText(EnvFile, sb.ToString(), new UTF8Encoding(false));
    }

    private void RefreshUi()
    {
        var server = Get("SERVER_HOST");
        ServerInfo.Text = string.IsNullOrWhiteSpace(server)
            ? "Server: (noch nicht eingerichtet - auf 'Einrichtung' tippen)"
            : $"Server: {Get("SSH_USER", "root")}@{server}:{Get("SSH_PORT", "22")}";
        DestBox.Text = DestDir;
        LastBackup.Text = "Letztes Backup: " + FindLastBackup();
        bool ready = !string.IsNullOrWhiteSpace(server);
        BackupBtn.IsEnabled = ready && !_busy;
    }

    private string FindLastBackup()
    {
        try
        {
            var dir = DestDir;
            if (!Directory.Exists(dir)) return "—";
            var newest = new DirectoryInfo(dir).GetFiles("cortex-full-*.tar.gz", SearchOption.AllDirectories)
                .OrderByDescending(f => f.LastWriteTime).FirstOrDefault();
            return newest is null ? "—"
                : $"{newest.LastWriteTime:dd.MM.yyyy HH:mm}  ({newest.Length / 1024.0 / 1024.0:0.0} MB)";
        }
        catch { return "—"; }
    }

    // ── Log ─────────────────────────────────────────────────────────────────────────────────────
    private void Log(string msg)
    {
        Dispatcher.Invoke(() =>
        {
            LogBox.AppendText("\n" + msg);
            LogScroll.ScrollToEnd();
        });
    }

    private void SetBusy(bool busy)
    {
        _busy = busy;
        BackupBtn.IsEnabled = !busy && !string.IsNullOrWhiteSpace(Get("SERVER_HOST"));
        RestoreBtn.IsEnabled = !busy;
        SetupBtn.IsEnabled = !busy;
    }

    // ── Ordner auswählen (Durchsuchen…) ─────────────────────────────────────────────────────────
    private void OnBrowse(object sender, RoutedEventArgs e)
    {
        var dlg = new OpenFolderDialog
        {
            Title = "Wohin soll das Backup gespeichert werden?",
            InitialDirectory = Directory.Exists(DestDir) ? DestDir : Environment.GetFolderPath(Environment.SpecialFolder.UserProfile)
        };
        if (dlg.ShowDialog(this) == true)
        {
            _cfg["LOCAL_DEST"] = dlg.FolderName;
            SaveConfig();
            RefreshUi();
            Log($"Backup-Ordner gesetzt: {dlg.FolderName}");
        }
    }

    // ── Backup jetzt ────────────────────────────────────────────────────────────────────────────
    private async void OnBackup(object sender, RoutedEventArgs e)
    {
        if (_busy) return;
        var server = Get("SERVER_HOST");
        if (string.IsNullOrWhiteSpace(server)) { Log("Bitte zuerst die Einrichtung ausfüllen."); return; }

        SetBusy(true);
        LogBox.Text = "=== Backup gestartet ===";
        try
        {
            var user = Get("SSH_USER", "root");
            var port = Get("SSH_PORT", "22");
            var key = ResolveKey(Get("SSH_KEY"));   // Feld leer? -> bekannte Schlüssel automatisch finden
            if (!string.IsNullOrWhiteSpace(key)) FixKeyPermissions(key);  // Windows-„bad permissions" vorbeugen
            var appDir = Get("REMOTE_APP_DIR", "/opt/second-brain");
            var target = $"{user}@{server}";

            if (server == "10.8.0.1")
            {
                Log("Prüfe WireGuard-Tunnel…");
                if (!await TcpReachable(server, int.Parse(port), 2500))
                { Log("FEHLER: WireGuard-Tunnel nicht erreichbar. Erst verbinden (sind Z:/Y: da?)."); return; }
                Log("Tunnel steht.");
            }

            var sshBase = new List<string> { "-p", port, "-o", "StrictHostKeyChecking=accept-new", "-o", "ConnectTimeout=15" };
            var scpBase = new List<string> { "-P", port, "-o", "StrictHostKeyChecking=accept-new", "-o", "ConnectTimeout=15" };
            if (!string.IsNullOrWhiteSpace(key) && File.Exists(key)) { sshBase.AddRange(new[] { "-i", key }); scpBase.AddRange(new[] { "-i", key }); Log("SSH-Schlüssel: " + key); }
            else Log("Hinweis: kein SSH-Schlüssel gefunden — der Server fragt evtl. nach Passwort. Tipp: in Einrichtung einen erzeugen.");

            Log("Stoße Server-Backup an (kann 1-2 Min dauern)…");
            var sshArgs = new List<string>(sshBase) { target, $"bash {appDir}/scripts/full-backup-create.sh" };
            var (code, outp) = await RunProcess("ssh.exe", sshArgs);
            if (code != 0 && string.IsNullOrEmpty(outp)) { Log("FEHLER: SSH-Aufruf fehlgeschlagen. Server/Verbindung prüfen."); return; }

            // Letzte stdout-Zeile, die wie ein absoluter .tar.gz-Pfad aussieht. Robust gegen das
            // \r, das Windows-ssh.exe anhaengt (deshalb pro Zeile Trim statt Regex-Multiline-$).
            var archive = outp.Replace("\r", "").Split('\n')
                .Select(l => l.Trim())
                .LastOrDefault(l => l.StartsWith('/') && l.EndsWith(".tar.gz", StringComparison.Ordinal));
            if (string.IsNullOrWhiteSpace(archive))
            { Log("FEHLER: Server lieferte keinen Archiv-Pfad. Liegen die Skripte auf dem Server (git pull + chmod +x)?"); return; }
            Log("Server-Archiv: " + archive);

            var stamp = DateTime.Now.ToString("yyyy-MM-dd_HHmmss");
            var localDir = Path.Combine(DestDir, stamp);
            Directory.CreateDirectory(localDir);
            var localFile = Path.Combine(localDir, Path.GetFileName(archive));

            Log("Ziehe Archiv auf diesen PC…");
            var scpArgs = new List<string>(scpBase) { $"{target}:{archive}", localFile };
            var (scode, _) = await RunProcess("scp.exe", scpArgs);
            if (!File.Exists(localFile) || new FileInfo(localFile).Length < 2000)
            { Log($"FEHLER: Download fehlgeschlagen (Exit {scode})."); return; }

            var mb = new FileInfo(localFile).Length / 1024.0 / 1024.0;
            Log($"FERTIG ✓  Lokal gespeichert: {localFile}  ({mb:0.0} MB)");
            Log("Mit dieser Datei kann der Server bei Totalausfall komplett neu aufgesetzt werden.");
            RefreshUi();
            try { Process.Start(new ProcessStartInfo("explorer.exe", $"\"{localDir}\"") { UseShellExecute = true }); } catch { }
        }
        catch (Exception ex) { Log("FEHLER: " + ex.Message); }
        finally { SetBusy(false); }
    }

    // ── Wiederherstellen (Anleitung; Restore läuft auf dem Server) ──────────────────────────────
    private void OnRestore(object sender, RoutedEventArgs e)
    {
        var newest = "deinem neuesten cortex-full-….tar.gz";
        try
        {
            var dir = DestDir;
            if (Directory.Exists(dir))
            {
                var f = new DirectoryInfo(dir).GetFiles("cortex-full-*.tar.gz", SearchOption.AllDirectories)
                    .OrderByDescending(x => x.LastWriteTime).FirstOrDefault();
                if (f != null) newest = f.FullName;
            }
        }
        catch { }

        var msg =
            "Wiederherstellen passiert auf dem (neuen) Server — so geht's:\n\n" +
            "1. Frisches Ubuntu, Docker installieren:\n   curl -fsSL https://get.docker.com | sh\n\n" +
            "2. Backup-Archiv auf den Server kopieren (scp), z. B.:\n   " + newest + "\n\n" +
            "3. Restore-Skript ausführen:\n   sudo bash full-restore.sh <archiv.tar.gz>\n   (mit JA bestätigen)\n\n" +
            "4. WireGuard aus der gesicherten wg0.conf einrichten.\n\n" +
            "Die ausführliche Anleitung öffne ich jetzt. Backup-Ordner ebenfalls öffnen?";
        var r = MessageBox.Show(this, msg, "Wiederherstellen", MessageBoxButton.YesNo, MessageBoxImage.Information);

        try
        {
            var readme = Path.Combine(Path.GetDirectoryName(Environment.ProcessPath) ?? ".", "BACKUP-RESTORE-README.md");
            if (File.Exists(readme)) Process.Start(new ProcessStartInfo(readme) { UseShellExecute = true });
        }
        catch { }
        if (r == MessageBoxResult.Yes && Directory.Exists(DestDir))
            try { Process.Start(new ProcessStartInfo("explorer.exe", $"\"{DestDir}\"") { UseShellExecute = true }); } catch { }
    }

    // ── Einrichtung ─────────────────────────────────────────────────────────────────────────────
    private void OnSetup(object sender, RoutedEventArgs e)
    {
        var dlg = new SetupWindow(_cfg) { Owner = this };
        if (dlg.ShowDialog() == true)
        {
            foreach (var kv in dlg.Result) _cfg[kv.Key] = kv.Value;
            SaveConfig();
            LoadConfig();
            RefreshUi();
            Log("Einrichtung gespeichert.");
        }
    }

    // ── Helfer ──────────────────────────────────────────────────────────────────────────────────
    // Konfigurierten Schlüssel nehmen — oder, falls leer/ungültig, bekannte Orte automatisch durchsuchen.
    private static string ResolveKey(string configured)
    {
        if (!string.IsNullOrWhiteSpace(configured) && File.Exists(configured)) return configured;
        var up = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        foreach (var c in new[]
        {
            Path.Combine(up, "SK", "second-brain", "id_ed25519"),
            Path.Combine(up, "SK", "second-brain", "id_cortex"),
            Path.Combine(up, ".ssh", "id_ed25519"),
        })
            if (File.Exists(c)) return c;
        return "";
    }

    // Windows-SSH verlangt strenge Datei-Rechte am privaten Schlüssel. Verwaiste/zu offene ACLs lösen
    // „UNPROTECTED PRIVATE KEY" aus -> hier praeventiv auf „nur aktueller Benutzer" setzen.
    private static void FixKeyPermissions(string key)
    {
        try
        {
            var user = $"{Environment.UserDomainName}\\{Environment.UserName}";
            foreach (var argset in new[]
            {
                new[] { key, "/inheritance:r" },
                new[] { key, "/grant:r", $"{user}:F" },
            })
            {
                var psi = new ProcessStartInfo("icacls.exe")
                { UseShellExecute = false, CreateNoWindow = true, RedirectStandardOutput = true, RedirectStandardError = true };
                foreach (var a in argset) psi.ArgumentList.Add(a);
                using var p = Process.Start(psi);
                p?.WaitForExit(8000);
            }
        }
        catch { /* Reparatur ist Best-Effort; schlimmstenfalls greift die alte Fehlermeldung */ }
    }

    private static async Task<bool> TcpReachable(string host, int port, int timeoutMs)
    {
        try
        {
            using var c = new TcpClient();
            var t = c.ConnectAsync(host, port);
            return await Task.WhenAny(t, Task.Delay(timeoutMs)) == t && c.Connected;
        }
        catch { return false; }
    }

    private async Task<(int code, string output)> RunProcess(string exe, IEnumerable<string> args)
    {
        var psi = new ProcessStartInfo(exe)
        {
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            UseShellExecute = false,
            CreateNoWindow = true,
            StandardOutputEncoding = Encoding.UTF8,
            StandardErrorEncoding = Encoding.UTF8,
        };
        foreach (var a in args) psi.ArgumentList.Add(a);

        var sb = new StringBuilder();
        using var p = new Process { StartInfo = psi, EnableRaisingEvents = true };
        p.OutputDataReceived += (_, ev) => { if (ev.Data != null) { sb.AppendLine(ev.Data); Log("  " + ev.Data); } };
        p.ErrorDataReceived += (_, ev) => { if (!string.IsNullOrWhiteSpace(ev.Data)) Log("  " + ev.Data); };
        try { p.Start(); } catch (Exception ex) { Log("Konnte " + exe + " nicht starten: " + ex.Message); return (-1, ""); }
        p.BeginOutputReadLine();
        p.BeginErrorReadLine();
        await p.WaitForExitAsync();
        return (p.ExitCode, sb.ToString());
    }
}
