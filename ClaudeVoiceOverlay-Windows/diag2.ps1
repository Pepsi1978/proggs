Add-Type @'
using System;
using System.Text;
using System.Collections.Generic;
using System.Runtime.InteropServices;

public class WinDiag2 {
    public delegate bool EnumWindowsProc(IntPtr hwnd, IntPtr lParam);
    public delegate bool EnumChildProc(IntPtr hwnd, IntPtr lParam);

    [DllImport("user32.dll")] public static extern bool EnumWindows(EnumWindowsProc cb, IntPtr lParam);
    [DllImport("user32.dll")] public static extern bool EnumChildWindows(IntPtr parent, EnumChildProc cb, IntPtr lParam);
    [DllImport("user32.dll", CharSet=CharSet.Auto)] public static extern int GetClassName(IntPtr hWnd, StringBuilder sb, int max);
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT r);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint pid);
    [DllImport("user32.dll", CharSet=CharSet.Auto)] public static extern int GetWindowText(IntPtr hWnd, StringBuilder sb, int max);

    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int L, T, R, B; }

    public static void DumpProcess(uint targetPid) {
        var topWindows = new List<IntPtr>();
        EnumWindows((h, _) => {
            uint pid;
            GetWindowThreadProcessId(h, out pid);
            if (pid == targetPid) topWindows.Add(h);
            return true;
        }, IntPtr.Zero);

        Console.WriteLine("Found " + topWindows.Count + " top-level windows for PID " + targetPid);

        foreach (var tw in topWindows) {
            var sb = new StringBuilder(256);
            GetClassName(tw, sb, 256);
            var title = new StringBuilder(256);
            GetWindowText(tw, title, 256);
            RECT r; GetWindowRect(tw, out r);
            bool vis = IsWindowVisible(tw);
            Console.WriteLine("TopLevel: hwnd=" + tw + " class='" + sb + "' title='" + title + "' rect=(" + r.L + "," + r.T + "," + r.R + "," + r.B + ") visible=" + vis);

            // Enumerate children
            int i = 0;
            EnumChildWindows(tw, (ch, __) => {
                var csb = new StringBuilder(256);
                GetClassName(ch, csb, 256);
                RECT cr; GetWindowRect(ch, out cr);
                bool cv = IsWindowVisible(ch);
                Console.WriteLine("  Child[" + i++ + "] hwnd=" + ch + " class='" + csb + "' rect=(" + cr.L + "," + cr.T + "," + cr.R + "," + cr.B + ") visible=" + cv);
                return true;
            }, IntPtr.Zero);
        }
    }
}
'@

Write-Host "=== ALL Codex Process Windows ==="
$procs = Get-Process -Name 'Codex' -ErrorAction SilentlyContinue
foreach ($p in $procs) {
    Write-Host "--- Codex PID=$($p.Id) ---"
    [WinDiag2]::DumpProcess([uint32]$p.Id)
}

Write-Host ""
Write-Host "=== ALL Claude Process Windows ==="
$procs = Get-Process -Name 'claude' -ErrorAction SilentlyContinue
foreach ($p in $procs) {
    if ($p.MainWindowHandle -ne 0) {
        Write-Host "--- Claude PID=$($p.Id) (has MainWindow) ---"
        [WinDiag2]::DumpProcess([uint32]$p.Id)
    }
}
