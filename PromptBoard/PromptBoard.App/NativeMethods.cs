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

    [DllImport("user32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool SetForegroundWindow(IntPtr hWnd);

    public static void MakeNoActivateToolWindow(IntPtr hwnd)
    {
        ApplyExStyle(hwnd, current => current | WS_EX_NOACTIVATE | WS_EX_TOOLWINDOW);
    }

    /// <summary>
    /// Temporarily lets the bar accept focus so a modal dialog can receive
    /// keyboard input. Pair with <see cref="ForbidActivation"/> when the
    /// dialog closes. Without this the NoActivate style blocks text input
    /// to child ContentDialogs and typing leaks to the underlying CLI.
    /// </summary>
    public static void AllowActivation(IntPtr hwnd)
    {
        ApplyExStyle(hwnd, current => current & ~WS_EX_NOACTIVATE);
        SetForegroundWindow(hwnd);
    }

    /// <summary>Re-applies WS_EX_NOACTIVATE after a dialog is done.</summary>
    public static void ForbidActivation(IntPtr hwnd)
    {
        ApplyExStyle(hwnd, current => current | WS_EX_NOACTIVATE);
    }

    private static void ApplyExStyle(IntPtr hwnd, Func<long, long> transform)
    {
        if (IntPtr.Size == 8)
        {
            IntPtr current = GetWindowLongPtr64(hwnd, GWL_EXSTYLE);
            IntPtr updated = new(transform(current.ToInt64()));
            SetWindowLongPtr64(hwnd, GWL_EXSTYLE, updated);
        }
        else
        {
            int current = GetWindowLong32(hwnd, GWL_EXSTYLE);
            int updated = (int)transform(current);
            SetWindowLong32(hwnd, GWL_EXSTYLE, updated);
        }
    }
}
