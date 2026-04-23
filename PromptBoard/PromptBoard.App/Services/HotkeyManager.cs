using System;
using System.Runtime.InteropServices;
using Serilog;

namespace PromptBoard.App.Services;

/// <summary>
/// Registers a global Ctrl+Shift+P hotkey against a WinUI 3 window.
/// Uses the Windows subclassing API (comctl32) to observe WM_HOTKEY
/// messages without clobbering the default WinUI window procedure.
/// </summary>
public sealed class HotkeyManager : IDisposable
{
    private const uint MOD_CONTROL = 0x0002;
    private const uint MOD_SHIFT = 0x0004;
    private const int WM_HOTKEY = 0x0312;
    private const uint SUBCLASS_ID = 0xB0AD;

    private readonly IntPtr _hwnd;
    private readonly Action _onPressed;
    private readonly int _hotkeyId;
    private SubclassProc? _subclassProc;
    private bool _subclassed;
    private bool _hotkeyRegistered;

    public HotkeyManager(IntPtr hwnd, Action onPressed)
    {
        _hwnd = hwnd;
        _onPressed = onPressed;
        _hotkeyId = unchecked((int)SUBCLASS_ID);
    }

    /// <summary>
    /// Install the subclass proc and register Ctrl+Shift+P. Safe to call
    /// multiple times — subsequent calls are no-ops.
    /// </summary>
    public void Start()
    {
        if (_subclassed)
        {
            return;
        }

        _subclassProc = OnWindowMessage;
        _subclassed = SetWindowSubclass(_hwnd, _subclassProc, SUBCLASS_ID, IntPtr.Zero);
        if (!_subclassed)
        {
            Log.Warning("HotkeyManager: SetWindowSubclass failed (Win32 {Err}).", Marshal.GetLastWin32Error());
            return;
        }

        _hotkeyRegistered = RegisterHotKey(_hwnd, _hotkeyId, MOD_CONTROL | MOD_SHIFT, (uint)'P');
        if (!_hotkeyRegistered)
        {
            Log.Warning("HotkeyManager: RegisterHotKey failed (Win32 {Err}). Probably claimed by another app.",
                Marshal.GetLastWin32Error());
        }
        else
        {
            Log.Information("HotkeyManager: Ctrl+Shift+P registered for HWND {Hwnd}.", _hwnd);
        }
    }

    private IntPtr OnWindowMessage(IntPtr hwnd, uint msg, IntPtr wParam, IntPtr lParam, UIntPtr subclassId, IntPtr refData)
    {
        if (msg == WM_HOTKEY && wParam.ToInt32() == _hotkeyId)
        {
            try
            {
                _onPressed();
            }
            catch (Exception ex)
            {
                Log.Error(ex, "HotkeyManager: handler threw.");
            }
            return IntPtr.Zero;
        }

        return DefSubclassProc(hwnd, msg, wParam, lParam);
    }

    public void Dispose()
    {
        if (_hotkeyRegistered)
        {
            UnregisterHotKey(_hwnd, _hotkeyId);
            _hotkeyRegistered = false;
        }

        if (_subclassed && _subclassProc is not null)
        {
            RemoveWindowSubclass(_hwnd, _subclassProc, SUBCLASS_ID);
            _subclassed = false;
        }

        _subclassProc = null;
    }

    // -------- P/Invoke --------

    [UnmanagedFunctionPointer(CallingConvention.StdCall)]
    private delegate IntPtr SubclassProc(
        IntPtr hWnd, uint uMsg, IntPtr wParam, IntPtr lParam,
        UIntPtr uIdSubclass, IntPtr dwRefData);

    [DllImport("user32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool RegisterHotKey(IntPtr hWnd, int id, uint fsModifiers, uint vk);

    [DllImport("user32.dll", SetLastError = true)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool UnregisterHotKey(IntPtr hWnd, int id);

    [DllImport("comctl32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool SetWindowSubclass(
        IntPtr hWnd, SubclassProc pfnSubclass, uint uIdSubclass, IntPtr dwRefData);

    [DllImport("comctl32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool RemoveWindowSubclass(
        IntPtr hWnd, SubclassProc pfnSubclass, uint uIdSubclass);

    [DllImport("comctl32.dll", CharSet = CharSet.Unicode)]
    private static extern IntPtr DefSubclassProc(IntPtr hWnd, uint uMsg, IntPtr wParam, IntPtr lParam);
}
