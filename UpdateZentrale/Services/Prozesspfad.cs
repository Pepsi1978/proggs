using System.IO;
using System.Runtime.InteropServices;
using System.Text;

namespace UpdateZentrale.Services;

/// <summary>
/// The executable path of another process. Process.MainModule needs PROCESS_QUERY_INFORMATION and
/// VM_READ and throws for an elevated process seen from a non-elevated one; the limited query
/// right used here is granted across that integrity boundary.
/// </summary>
public static class Prozesspfad
{
    private const uint ProcessQueryLimitedInformation = 0x1000;

    /// <returns>The full path, or "" when it cannot be read -- never a guess.</returns>
    public static string Lesen(int pid)
    {
        var griff = OpenProcess(ProcessQueryLimitedInformation, false, pid);
        if (griff == IntPtr.Zero) return "";
        try
        {
            var puffer = new StringBuilder(1024);
            var laenge = puffer.Capacity;
            return QueryFullProcessImageNameW(griff, 0, puffer, ref laenge) ? puffer.ToString(0, laenge) : "";
        }
        finally
        {
            CloseHandle(griff);
        }
    }

    /// <summary>Same file? Unknown on either side is never "the same".</summary>
    public static bool GleicheDatei(string? a, string? b)
    {
        if (string.IsNullOrWhiteSpace(a) || string.IsNullOrWhiteSpace(b)) return false;
        try
        {
            return string.Equals(Path.GetFullPath(a), Path.GetFullPath(b), StringComparison.OrdinalIgnoreCase);
        }
        catch
        {
            return false;
        }
    }

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern IntPtr OpenProcess(uint zugriff, bool erben, int pid);

    [DllImport("kernel32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    private static extern bool QueryFullProcessImageNameW(IntPtr prozess, uint flags, StringBuilder name, ref int groesse);

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern bool CloseHandle(IntPtr griff);
}
