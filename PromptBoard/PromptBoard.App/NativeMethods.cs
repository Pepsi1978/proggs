using System;
using System.Runtime.InteropServices;

namespace PromptBoard.App;

/// <summary>
/// P/Invoke helpers for win32 window style tweaks.
/// Used to apply WS_EX_NOACTIVATE | WS_EX_TOOLWINDOW to the PromptBar,
/// so clicking on it does not steal focus from the underlying CLI.
/// </summary>
internal static class NativeMethods
{
    private const int GWL_EXSTYLE = -20;
    private const int WS_EX_NOACTIVATE = 0x08000000;
    private const int WS_EX_TOOLWINDOW = 0x00000080;

    [DllImport("user32.dll", EntryPoint = "GetWindowLongPtrW", SetLastError = true)]
    private static extern IntPtr GetWindowLongPtr64(IntPtr hWnd, int nIndex);

    [DllImport("user32.dll", EntryPoint = "GetWindowLongW", SetLastError = true)]
    private static extern int GetWindowLong32(IntPtr hWnd, int nIndex);

    [DllImport("user32.dll", EntryPoint = "SetWindowLongPtrW", SetLastError = true)]
    private static extern IntPtr SetWindowLongPtr64(IntPtr hWnd, int nIndex, IntPtr dwNewLong);

    [DllImport("user32.dll", EntryPoint = "SetWindowLongW", SetLastError = true)]
    private static extern int SetWindowLong32(IntPtr hWnd, int nIndex, int dwNewLong);

    public static void MakeNoActivateToolWindow(IntPtr hwnd)
    {
        if (IntPtr.Size == 8)
        {
            IntPtr current = GetWindowLongPtr64(hwnd, GWL_EXSTYLE);
            IntPtr updated = new(current.ToInt64() | WS_EX_NOACTIVATE | WS_EX_TOOLWINDOW);
            SetWindowLongPtr64(hwnd, GWL_EXSTYLE, updated);
        }
        else
        {
            int current = GetWindowLong32(hwnd, GWL_EXSTYLE);
            int updated = current | WS_EX_NOACTIVATE | WS_EX_TOOLWINDOW;
            SetWindowLong32(hwnd, GWL_EXSTYLE, updated);
        }
    }
}
