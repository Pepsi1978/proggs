using System.Diagnostics;
using System.IO;
using System.Security.Principal;
using Microsoft.Win32;

namespace UpdateZentrale.Services;

/// <summary>
/// Everything about the app's own privileges. The manifest stays at asInvoker on purpose: a
/// hard requireAdministrator would elevate every child process too, which makes per-user
/// installers (winget, Squirrel) put their packages in the wrong profile. Instead the app can
/// relaunch itself elevated on demand, or be marked to always start elevated.
/// </summary>
public static class Rechte
{
    private const string LayersSchluessel = @"Software\Microsoft\Windows NT\CurrentVersion\AppCompatFlags\Layers";

    public static string EigeneExe { get; } = Process.GetCurrentProcess().MainModule?.FileName
                                              ?? Environment.ProcessPath
                                              ?? "";

    public static bool IstErhoeht
    {
        get
        {
            try
            {
                using var identitaet = WindowsIdentity.GetCurrent();
                return new WindowsPrincipal(identitaet).IsInRole(WindowsBuiltInRole.Administrator);
            }
            catch
            {
                return false;
            }
        }
    }

    public static bool ImmerAlsAdmin
    {
        get
        {
            try
            {
                using var key = Registry.CurrentUser.OpenSubKey(LayersSchluessel);
                return (key?.GetValue(EigeneExe) as string)?
                    .Contains("RUNASADMIN", StringComparison.OrdinalIgnoreCase) == true;
            }
            catch
            {
                return false;
            }
        }
    }

    public static bool ImmerAlsAdminSetzen(bool aktiv)
    {
        if (string.IsNullOrWhiteSpace(EigeneExe)) return false;

        try
        {
            using var key = Registry.CurrentUser.CreateSubKey(LayersSchluessel, writable: true);
            if (key is null) return false;

            if (aktiv)
            {
                var vorhanden = key.GetValue(EigeneExe) as string ?? "~";
                if (!vorhanden.Contains("RUNASADMIN", StringComparison.OrdinalIgnoreCase))
                {
                    vorhanden = (vorhanden.Trim() + " RUNASADMIN").Trim();
                }
                key.SetValue(EigeneExe, vorhanden, RegistryValueKind.String);
            }
            else
            {
                var vorhanden = key.GetValue(EigeneExe) as string;
                if (vorhanden is null) return true;

                var rest = string.Join(' ', vorhanden
                    .Split(' ', StringSplitOptions.RemoveEmptyEntries)
                    .Where(t => !t.Equals("RUNASADMIN", StringComparison.OrdinalIgnoreCase)));

                if (string.IsNullOrWhiteSpace(rest) || rest == "~") key.DeleteValue(EigeneExe, throwOnMissingValue: false);
                else key.SetValue(EigeneExe, rest, RegistryValueKind.String);
            }
            return true;
        }
        catch
        {
            return false;
        }
    }

    /// <summary>Starts a second, elevated instance and reports whether it came up.</summary>
    public static bool NeuStartenAlsAdmin()
    {
        if (string.IsNullOrWhiteSpace(EigeneExe)) return false;

        try
        {
            Process.Start(new ProcessStartInfo
            {
                FileName = EigeneExe,
                UseShellExecute = true,
                Verb = "runas",
                WorkingDirectory = Path.GetDirectoryName(EigeneExe) ?? ""
            });
            return true;
        }
        catch
        {
            return false;   // UAC prompt dismissed.
        }
    }
}
