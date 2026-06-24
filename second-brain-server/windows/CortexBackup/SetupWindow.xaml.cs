using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Windows;

namespace CortexBackup;

public partial class SetupWindow : Window
{
    private static readonly string SkDir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), "SK", "second-brain");

    public Dictionary<string, string> Result { get; } = new(StringComparer.OrdinalIgnoreCase);

    public SetupWindow(IReadOnlyDictionary<string, string> current)
    {
        InitializeComponent();
        TbHost.Text = Val(current, "SERVER_HOST", "");
        TbUser.Text = Val(current, "SSH_USER", "root");
        TbPort.Text = Val(current, "SSH_PORT", "22");
        TbAppDir.Text = Val(current, "REMOTE_APP_DIR", "/opt/second-brain");
        TbKey.Text = Val(current, "SSH_KEY", "");
    }

    private static string Val(IReadOnlyDictionary<string, string> d, string k, string fb) =>
        d.TryGetValue(k, out var v) && !string.IsNullOrWhiteSpace(v) ? v : fb;

    // SSH-Schlüssel erzeugen + (optional) zum Server kopieren
    private void OnGenKey(object sender, RoutedEventArgs e)
    {
        try
        {
            Directory.CreateDirectory(SkDir);
            var keyPath = Path.Combine(SkDir, "id_cortex");
            if (!File.Exists(keyPath))
            {
                var p = Process.Start(new ProcessStartInfo("ssh-keygen.exe")
                {
                    UseShellExecute = false,
                    CreateNoWindow = true,
                    ArgumentList = { "-t", "ed25519", "-f", keyPath, "-N", "", "-C", "cortex-backup" }
                });
                p?.WaitForExit();
            }
            if (!File.Exists(keyPath)) { Hint.Text = "Konnte keinen Schlüssel erzeugen (ist OpenSSH-Client installiert?)."; return; }
            TbKey.Text = keyPath;

            var host = TbHost.Text.Trim();
            var user = string.IsNullOrWhiteSpace(TbUser.Text) ? "root" : TbUser.Text.Trim();
            var port = string.IsNullOrWhiteSpace(TbPort.Text) ? "22" : TbPort.Text.Trim();
            if (string.IsNullOrWhiteSpace(host))
            {
                Hint.Text = "Schlüssel erzeugt. Trage erst die Server-Adresse ein, dann nochmal 'Erzeugen' tippen, um ihn zum Server zu kopieren.";
                return;
            }
            // Öffentlichen Schlüssel zum Server kopieren — sichtbares Fenster für die EINMALIGE Passwort-Eingabe.
            var copyCmd = $"type \"{keyPath}.pub\" | ssh -p {port} {user}@{host} \"mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys\" && echo. && echo Schluessel kopiert. Fenster schliessen.";
            Process.Start(new ProcessStartInfo("cmd.exe", $"/k {copyCmd}") { UseShellExecute = true });
            Hint.Text = "Ein Fenster hat sich geöffnet: dort EINMAL dein Server-Passwort eingeben. Danach läuft das Backup ohne Passwort.";
        }
        catch (Exception ex) { Hint.Text = "Fehler: " + ex.Message; }
    }

    private void OnSave(object sender, RoutedEventArgs e)
    {
        if (string.IsNullOrWhiteSpace(TbHost.Text)) { Hint.Text = "Bitte die Server-Adresse eintragen."; return; }
        Result["SERVER_HOST"] = TbHost.Text.Trim();
        Result["SSH_USER"] = string.IsNullOrWhiteSpace(TbUser.Text) ? "root" : TbUser.Text.Trim();
        Result["SSH_PORT"] = string.IsNullOrWhiteSpace(TbPort.Text) ? "22" : TbPort.Text.Trim();
        Result["REMOTE_APP_DIR"] = string.IsNullOrWhiteSpace(TbAppDir.Text) ? "/opt/second-brain" : TbAppDir.Text.Trim();
        if (!string.IsNullOrWhiteSpace(TbKey.Text)) Result["SSH_KEY"] = TbKey.Text.Trim();
        DialogResult = true;
    }

    private void OnCancel(object sender, RoutedEventArgs e) => DialogResult = false;
}
