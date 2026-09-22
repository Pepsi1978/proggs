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

    /// <summary>
    /// The own exe -- also the registry value name of the RUNASADMIN entry, so it must be the
    /// exact form Windows uses (measured: ProcessPath equals MainModule.FileName).
    /// </summary>
    public static string EigeneExe { get; } = EigeneExeErmitteln();

    private static string EigeneExeErmitteln()
    {
        if (!string.IsNullOrWhiteSpace(Environment.ProcessPath)) return Environment.ProcessPath;
        try
        {
            using var ich = Process.GetCurrentProcess();
            return ich.MainModule?.FileName ?? "";
        }
        catch
        {
            return "";
        }
    }

    private const string AdminToken = "RUNASADMIN";

    /// <summary>The AppCompat value is a list of space-separated tokens; only an exact token counts.</summary>
    internal static bool HatAdminToken(string? wert)
        => wert is not null && wert.Split(' ', StringSplitOptions.RemoveEmptyEntries)
            .Any(t => t.Equals(AdminToken, StringComparison.OrdinalIgnoreCase));

    /// <summary>Adds the token, keeping every other token as it was.</summary>
    internal static string MitAdminToken(string? wert)
    {
        if (HatAdminToken(wert)) return wert!;
        var basis = string.IsNullOrWhiteSpace(wert) ? "~" : wert.Trim();
        return basis + " " + AdminToken;
    }

    /// <returns>The value without the exact token, or null when nothing meaningful is left.</returns>
    internal static string? OhneAdminToken(string? wert)
    {
        if (wert is null) return null;
        var rest = string.Join(' ', wert.Split(' ', StringSplitOptions.RemoveEmptyEntries)
            .Where(t => !t.Equals(AdminToken, StringComparison.OrdinalIgnoreCase)));
        return string.IsNullOrWhiteSpace(rest) || rest == "~" ? null : rest;
    }

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
                return HatAdminToken(key?.GetValue(EigeneExe) as string);
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

            var vorhanden = key.GetValue(EigeneExe) as string;
            if (aktiv)
            {
                key.SetValue(EigeneExe, MitAdminToken(vorhanden), RegistryValueKind.String);
            }
            else
            {
                if (vorhanden is null) return true;
                var rest = OhneAdminToken(vorhanden);
                if (rest is null) key.DeleteValue(EigeneExe, throwOnMissingValue: false);
                else key.SetValue(EigeneExe, rest, RegistryValueKind.String);
            }
            return true;
        }
        catch
        {
            return false;
        }
    }

    /// <summary>
    /// Starts the elevated successor and reports whether it came up. This instance keeps the
    /// single-instance lock during the UAC prompt; the successor gets this PID and waits for
    /// this process to end before it claims the lock (see Uebernahme).
    /// </summary>
    public static bool NeuStartenAlsAdmin()
    {
        if (string.IsNullOrWhiteSpace(EigeneExe)) return false;

        try
        {
            using var _ = Process.Start(new ProcessStartInfo
            {
                FileName = EigeneExe,
                Arguments = Uebernahme.Argument(Environment.ProcessId),
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
