using System.IO;
using Microsoft.Win32;
using UpdateZentrale.Models;

namespace UpdateZentrale.Services;

/// <summary>
/// Reads and writes the two Windows facts the UI shows per program: "starts elevated" and
/// "is in autostart".
/// </summary>
public static class Systemdienst
{
    private const string LayersSchluessel = @"Software\Microsoft\Windows NT\CurrentVersion\AppCompatFlags\Layers";
    private const string RunSchluessel = @"Software\Microsoft\Windows\CurrentVersion\Run";

    /// <summary>
    /// "Always run as administrator" is the same compatibility flag the Explorer property page
    /// sets. Writing it under HKCU needs no elevation itself.
    /// </summary>
    public static bool AdminModusLesen(ProgrammEintrag eintrag)
    {
        var exe = Pfade.Aufloesen(eintrag.ExePfad);
        if (string.IsNullOrWhiteSpace(exe)) return false;

        using var key = Registry.CurrentUser.OpenSubKey(LayersSchluessel);
        var wert = key?.GetValue(exe) as string;
        return wert is not null && wert.Contains("RUNASADMIN", StringComparison.OrdinalIgnoreCase);
    }

    public static bool AdminModusSetzen(ProgrammEintrag eintrag, bool aktiv)
    {
        var exe = Pfade.Aufloesen(eintrag.ExePfad);
        if (string.IsNullOrWhiteSpace(exe) || !File.Exists(exe)) return false;

        try
        {
            using var key = Registry.CurrentUser.CreateSubKey(LayersSchluessel, writable: true);
            if (key is null) return false;

            if (aktiv)
            {
                // Keep flags that are already there (e.g. HIGHDPIAWARE) instead of overwriting.
                var vorhanden = key.GetValue(exe) as string ?? "~";
                if (!vorhanden.Contains("RUNASADMIN", StringComparison.OrdinalIgnoreCase))
                {
                    vorhanden = (vorhanden.Trim() + " RUNASADMIN").Trim();
                }
                key.SetValue(exe, vorhanden, RegistryValueKind.String);
            }
            else
            {
                var vorhanden = key.GetValue(exe) as string;
                if (vorhanden is null) return true;

                var rest = string.Join(' ', vorhanden
                    .Split(' ', StringSplitOptions.RemoveEmptyEntries)
                    .Where(t => !t.Equals("RUNASADMIN", StringComparison.OrdinalIgnoreCase)));

                if (string.IsNullOrWhiteSpace(rest) || rest == "~") key.DeleteValue(exe, throwOnMissingValue: false);
                else key.SetValue(exe, rest, RegistryValueKind.String);
            }
            return true;
        }
        catch
        {
            return false;
        }
    }

    /// <summary>
    /// True when an HKCU Run entry points at this program. Matching is done on the value, never on
    /// the entry name: the name is free text ("Spracheingabe - Claude Desktop") and would attach
    /// the badge to the wrong program. Wrapper launchers count too -- CVO starts through
    /// wscript.exe with a watcher script inside its repo folder, so the folder is matched as well.
    /// </summary>
    public static bool ImAutostart(ProgrammEintrag eintrag)
    {
        try
        {
            using var key = Registry.CurrentUser.OpenSubKey(RunSchluessel);
            if (key is null) return false;

            var exe = Pfade.Aufloesen(eintrag.ExePfad);
            var ordner = string.IsNullOrWhiteSpace(eintrag.RepoOrdner)
                ? null
                : Path.Combine(Pfade.RepoWurzel, eintrag.RepoOrdner.Replace('/', Path.DirectorySeparatorChar));

            foreach (var name in key.GetValueNames())
            {
                var wert = key.GetValue(name) as string ?? "";
                if (string.IsNullOrWhiteSpace(wert)) continue;

                if (!string.IsNullOrWhiteSpace(exe) && wert.Contains(exe, StringComparison.OrdinalIgnoreCase))
                    return true;
                if (ordner is not null && wert.Contains(ordner, StringComparison.OrdinalIgnoreCase))
                    return true;
            }
        }
        catch
        {
        }
        return false;
    }

    /// <summary>
    /// Elevated programs are skipped by the HKCU Run key -- UAC refuses to auto-start them. The UI
    /// offers to move that autostart into a scheduled task instead of silently breaking it.
    /// </summary>
    public static bool AdminBrichtAutostart(ProgrammEintrag eintrag, bool adminAktiv)
        => adminAktiv && ImAutostart(eintrag);

    /// <summary>Returns the Run entry (name and command) belonging to this program, if any.</summary>
    public static (string Name, string Wert)? RunEintrag(ProgrammEintrag eintrag)
    {
        try
        {
            using var key = Registry.CurrentUser.OpenSubKey(RunSchluessel);
            if (key is null) return null;

            var exe = Pfade.Aufloesen(eintrag.ExePfad);
            var ordner = string.IsNullOrWhiteSpace(eintrag.RepoOrdner)
                ? null
                : Path.Combine(Pfade.RepoWurzel, eintrag.RepoOrdner.Replace('/', Path.DirectorySeparatorChar));

            foreach (var name in key.GetValueNames())
            {
                var wert = key.GetValue(name) as string ?? "";
                if (string.IsNullOrWhiteSpace(wert)) continue;

                if ((!string.IsNullOrWhiteSpace(exe) && wert.Contains(exe, StringComparison.OrdinalIgnoreCase))
                    || (ordner is not null && wert.Contains(ordner, StringComparison.OrdinalIgnoreCase)))
                {
                    return (name, wert);
                }
            }
        }
        catch
        {
        }
        return null;
    }

    public static bool RunEintragEntfernen(string name)
    {
        try
        {
            using var key = Registry.CurrentUser.OpenSubKey(RunSchluessel, writable: true);
            key?.DeleteValue(name, throwOnMissingValue: false);
            return true;
        }
        catch
        {
            return false;
        }
    }

    public static bool RunEintragSchreiben(string name, string wert)
    {
        try
        {
            using var key = Registry.CurrentUser.CreateSubKey(RunSchluessel, writable: true);
            key?.SetValue(name, wert, RegistryValueKind.String);
            return true;
        }
        catch
        {
            return false;
        }
    }
}
