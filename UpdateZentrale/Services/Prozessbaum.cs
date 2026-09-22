using System.Diagnostics;
using System.Runtime.InteropServices;

namespace UpdateZentrale.Services;

/// <summary>
/// The descendants of a process, captured BEFORE it is killed. Kill(entireProcessTree) only
/// returns once TerminateProcess was requested; it does not wait for the children, and the tree
/// cannot be rebuilt afterwards (the parent links point at a dead PID). Capturing first -- with
/// an open handle per child, which also rules out PID reuse -- lets the caller wait for exactly
/// these processes and nothing else.
/// </summary>
internal static class Prozessbaum
{
    internal readonly record struct Knoten(int Pid, int ElternPid);

    /// <summary>All descendants of the root in the snapshot (breadth first, cycle safe).</summary>
    internal static List<int> Nachfahren(IEnumerable<Knoten> alle, int wurzel)
    {
        var kinderVon = alle.Where(k => k.Pid != k.ElternPid)
            .GroupBy(k => k.ElternPid)
            .ToDictionary(g => g.Key, g => g.Select(k => k.Pid).ToList());

        var ergebnis = new List<int>();
        var gesehen = new HashSet<int> { wurzel };
        var offen = new Queue<int>();
        offen.Enqueue(wurzel);
        while (offen.Count > 0)
        {
            if (!kinderVon.TryGetValue(offen.Dequeue(), out var kinder)) continue;
            foreach (var kind in kinder)
            {
                if (!gesehen.Add(kind)) continue;
                ergebnis.Add(kind);
                offen.Enqueue(kind);
            }
        }
        return ergebnis;
    }

    /// <summary>
    /// Opens every current descendant of the root. Only processes started at or after the root
    /// count: a recycled parent PID from an older process must not pull strangers into the tree.
    /// Processes that cannot be opened (another user, elevated) are skipped -- the tree kill will
    /// report them itself. The caller owns and disposes the returned objects.
    /// </summary>
    internal static List<Process> Erfassen(Process wurzel)
    {
        var erfasst = new List<Process>();
        DateTime wurzelStart;
        try { wurzelStart = wurzel.StartTime; }
        catch { return erfasst; }

        foreach (var pid in Nachfahren(Schnappschuss(), wurzel.Id))
        {
            Process? kind = null;
            try
            {
                kind = Process.GetProcessById(pid);
                _ = kind.Handle;                                   // keep a handle: identity is pinned now
                if (kind.StartTime >= wurzelStart.AddMilliseconds(-50))
                {
                    erfasst.Add(kind);
                    kind = null;
                }
            }
            catch
            {
                // gone meanwhile or not accessible
            }
            finally
            {
                kind?.Dispose();
            }
        }
        return erfasst;
    }

    /// <summary>A Toolhelp32 process snapshot: every process with its parent PID.</summary>
    internal static List<Knoten> Schnappschuss()
    {
        var liste = new List<Knoten>();
        var schnappschuss = CreateToolhelp32Snapshot(Th32csSnapprocess, 0);
        if (schnappschuss == IntPtr.Zero || schnappschuss == new IntPtr(-1)) return liste;
        try
        {
            var eintrag = new ProcessEntry32 { Groesse = (uint)Marshal.SizeOf<ProcessEntry32>() };
            if (!Process32FirstW(schnappschuss, ref eintrag)) return liste;
            do
            {
                liste.Add(new Knoten((int)eintrag.ProzessId, (int)eintrag.ElternId));
            } while (Process32NextW(schnappschuss, ref eintrag));
        }
        finally
        {
            CloseHandle(schnappschuss);
        }
        return liste;
    }

    private const uint Th32csSnapprocess = 0x00000002;

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
    private struct ProcessEntry32
    {
        public uint Groesse;
        public uint Nutzung;
        public uint ProzessId;
        public IntPtr StandardHeap;
        public uint ModulId;
        public uint Threads;
        public uint ElternId;
        public int Prioritaet;
        public uint Flags;
        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 260)] public string Datei;
    }

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern IntPtr CreateToolhelp32Snapshot(uint flags, uint prozessId);

    [DllImport("kernel32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    private static extern bool Process32FirstW(IntPtr schnappschuss, ref ProcessEntry32 eintrag);

    [DllImport("kernel32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    private static extern bool Process32NextW(IntPtr schnappschuss, ref ProcessEntry32 eintrag);

    [DllImport("kernel32.dll", SetLastError = true)]
    private static extern bool CloseHandle(IntPtr griff);
}
