Add-Type @'
using System;
using System.Text;
using System.Runtime.InteropServices;

public class WinDiag {
    public delegate bool EnumChildProc(IntPtr hwnd, IntPtr lParam);

    [DllImport("user32.dll")] public static extern bool EnumChildWindows(IntPtr parent, EnumChildProc cb, IntPtr lParam);
    [DllImport("user32.dll", CharSet=CharSet.Auto)] public static extern int GetClassName(IntPtr hWnd, StringBuilder sb, int max);
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT r);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr hWnd);

    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int L, T, R, B; }

    public static void Dump(IntPtr parent) {
        var sb = new StringBuilder(256);
        int i = 0;
        EnumChildWindows(parent, (h, _) => {
            sb.Clear();
            GetClassName(h, sb, 256);
            RECT r; GetWindowRect(h, out r);
            bool vis = IsWindowVisible(h);
            Console.WriteLine("  [{0}] hwnd={1} class='{2}' rect=({3},{4},{5},{6}) visible={7}", i++, h, sb, r.L, r.T, r.R, r.B, vis);
            return true;
        }, IntPtr.Zero);
        Console.WriteLine("Total: " + i + " child windows");
    }
}
'@

Write-Host "=== Codex Windows ==="
$codex = Get-Process -Name 'Codex' -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowHandle -ne 0 } | Select-Object -First 1
if ($codex) {
    Write-Host "PID=$($codex.Id) MainWindow=$($codex.MainWindowHandle) Title=$($codex.MainWindowTitle)"
    [WinDiag]::Dump($codex.MainWindowHandle)
} else { Write-Host "Codex not running" }

Write-Host ""
Write-Host "=== Claude Desktop Windows ==="
$claude = Get-Process -Name 'claude' -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowHandle -ne 0 } | Select-Object -First 1
if ($claude) {
    Write-Host "PID=$($claude.Id) MainWindow=$($claude.MainWindowHandle) Title=$($claude.MainWindowTitle)"
    [WinDiag]::Dump($claude.MainWindowHandle)
} else { Write-Host "Claude not running" }
