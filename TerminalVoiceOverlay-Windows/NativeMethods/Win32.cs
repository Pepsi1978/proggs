using System;
using System.Runtime.InteropServices;

namespace TerminalVoiceOverlay.NativeMethods
{
    public static class Win32
    {
        // ── Window Styles ──
        public const int GWL_EXSTYLE = -20;
        public const int WS_EX_NOACTIVATE = 0x08000000;
        public const int WS_EX_TOOLWINDOW = 0x00000080;

        // ── WM_MOUSEACTIVATE ──
        public const int WM_MOUSEACTIVATE = 0x0021;
        public const int MA_NOACTIVATE = 3;

        // ── SetWinEventHook ──
        public const uint EVENT_SYSTEM_FOREGROUND = 0x0003;
        public const uint EVENT_OBJECT_LOCATIONCHANGE = 0x800B;
        public const uint WINEVENT_OUTOFCONTEXT = 0x0000;
        public const uint WINEVENT_SKIPOWNPROCESS = 0x0002;

        public delegate void WinEventDelegate(
            IntPtr hWinEventHook, uint eventType, IntPtr hwnd,
            int idObject, int idChild, uint dwEventThread, uint dwmsEventTime);

        [DllImport("user32.dll")]
        public static extern IntPtr SetWinEventHook(
            uint eventMin, uint eventMax, IntPtr hmodWinEventProc,
            WinEventDelegate lpfnWinEventProc, uint idProcess, uint idThread, uint dwFlags);

        [DllImport("user32.dll")]
        public static extern bool UnhookWinEvent(IntPtr hWinEventHook);

        // ── Window Info ──
        [DllImport("user32.dll")]
        public static extern IntPtr GetForegroundWindow();

        [DllImport("user32.dll")]
        public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);

        // Walking the parent chain — used by the prompt-hotkey hook to
        // accept both the inner TermControl and its outer host window
        // as "the active terminal" so Strg+1..9 still fire when a
        // tabbed terminal puts the focused control a level below the
        // HWND that the TerminalWatcher captured.
        [DllImport("user32.dll")]
        public static extern IntPtr GetParent(IntPtr hWnd);

        [DllImport("user32.dll")]
        public static extern int GetWindowLong(IntPtr hWnd, int nIndex);

        [DllImport("user32.dll")]
        public static extern int SetWindowLong(IntPtr hWnd, int nIndex, int dwNewLong);

        // ── Window Rect ──
        [StructLayout(LayoutKind.Sequential)]
        public struct RECT
        {
            public int Left, Top, Right, Bottom;
        }

        [DllImport("user32.dll")]
        public static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);

        // ── Monitor Info ──
        public const int MONITOR_DEFAULTTONEAREST = 2;

        [StructLayout(LayoutKind.Sequential)]
        public struct MONITORINFO
        {
            public int cbSize;
            public RECT rcMonitor;
            public RECT rcWork;
            public int dwFlags;
        }

        [DllImport("user32.dll")]
        public static extern IntPtr MonitorFromWindow(IntPtr hwnd, int dwFlags);

        [DllImport("user32.dll")]
        public static extern bool GetMonitorInfo(IntPtr hMonitor, ref MONITORINFO lpmi);

        // ── SendInput ──
        [StructLayout(LayoutKind.Sequential)]
        public struct INPUT
        {
            public uint type;
            public INPUTUNION u;
        }

        [StructLayout(LayoutKind.Explicit)]
        public struct INPUTUNION
        {
            [FieldOffset(0)] public KEYBDINPUT ki;
        }

        [StructLayout(LayoutKind.Sequential)]
        public struct KEYBDINPUT
        {
            public ushort wVk;
            public ushort wScan;
            public uint dwFlags;
            public uint time;
            public IntPtr dwExtraInfo;
        }

        public const uint INPUT_KEYBOARD = 1;
        public const uint KEYEVENTF_KEYUP = 0x0002;

        // Virtual key codes
        public const ushort VK_CONTROL = 0x11;
        public const ushort VK_SHIFT   = 0x10;
        public const ushort VK_ESCAPE  = 0x1B;
        public const ushort VK_V       = 0x56;
        public const ushort VK_MENU    = 0x12; // Alt
        public const ushort VK_SPACE   = 0x20;
        public const ushort VK_P       = 0x50; // Hotkey: Ctrl+Alt+P = Screenshot
        public const ushort VK_I       = 0x49; // Hotkey: Ctrl+Alt+I = Insert last screenshot
        public const ushort VK_M       = 0x4D; // Hotkey: Shift+Alt+M = PTT (alternative ohne Leertaste, keine Konflikte mit System-Menue)
        public const ushort VK_F9      = 0x78; // Hotkey: Strg+F9 oder Shift+F9 = PTT (kurze Kombi, keine Konflikte in CLIs)
        public const ushort VK_E       = 0x45; // Hotkey: Strg+Shift+Alt+E = Auto-Enter-Toggle (Stream-Deck-tauglich)
        public const ushort VK_F11     = 0x7A; // Hotkey: Alt+F11 = Explorer am Release-Bundle-Pfad oeffnen
        public const ushort VK_F12     = 0x7B; // Hotkey: Alt+F12 = PTT (Alt+Wheel zoomt nicht, daher kein Zoom-Konflikt)
        // Modifier keys for the Win+Alt+<letter> prompt hotkey branch in
        // OverlayWindow's low-level keyboard hook. GetAsyncKeyState honours
        // both left- and right-hand Win/Alt keys via VK_MENU/VK_LWIN/VK_RWIN.
        public const ushort VK_LWIN    = 0x5B;
        public const ushort VK_RWIN    = 0x5C;

        // ── Low-Level Keyboard Hook (fuer Push-to-Talk) ──
        // RegisterHotKey kann nur KeyDown — fuer Push-to-Talk brauchen wir
        // sowohl Down als auch Up. WH_KEYBOARD_LL = 13 liefert beide Events
        // ueber einen low-level Hook der vor allen Apps feuert.
        public const int WH_KEYBOARD_LL = 13;
        public const int WM_KEYDOWN     = 0x0100;
        public const int WM_KEYUP       = 0x0101;
        public const int WM_SYSKEYDOWN  = 0x0104;
        public const int WM_SYSKEYUP    = 0x0105;

        [StructLayout(LayoutKind.Sequential)]
        public struct KBDLLHOOKSTRUCT
        {
            public uint vkCode;
            public uint scanCode;
            public uint flags;
            public uint time;
            public IntPtr dwExtraInfo;
        }

        public delegate IntPtr LowLevelKeyboardProc(int nCode, IntPtr wParam, IntPtr lParam);

        [DllImport("user32.dll", CharSet = CharSet.Auto, SetLastError = true)]
        public static extern IntPtr SetWindowsHookEx(int idHook, LowLevelKeyboardProc lpfn,
            IntPtr hMod, uint dwThreadId);

        [DllImport("user32.dll", CharSet = CharSet.Auto, SetLastError = true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        public static extern bool UnhookWindowsHookEx(IntPtr hhk);

        [DllImport("user32.dll", CharSet = CharSet.Auto, SetLastError = true)]
        public static extern IntPtr CallNextHookEx(IntPtr hhk, int nCode,
            IntPtr wParam, IntPtr lParam);

        [DllImport("user32.dll")]
        public static extern short GetAsyncKeyState(int vKey);

        [DllImport("kernel32.dll", CharSet = CharSet.Auto, SetLastError = true)]
        public static extern IntPtr GetModuleHandle(string lpModuleName);

        [DllImport("user32.dll", SetLastError = true)]
        public static extern uint SendInput(uint nInputs, INPUT[] pInputs, int cbSize);

        // ── Window Activation ──
        [DllImport("user32.dll")]
        public static extern bool SetForegroundWindow(IntPtr hWnd);

        [DllImport("user32.dll")]
        public static extern bool AllowSetForegroundWindow(uint dwProcessId);

        public const uint ASFW_ANY = 0xFFFFFFFF;

        // ── EnumWindows / GetClassName (fuer Alt+F11 Explorer-Fokus) ──
        public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);

        [DllImport("user32.dll")]
        [return: MarshalAs(UnmanagedType.Bool)]
        public static extern bool EnumWindows(EnumWindowsProc lpEnumFunc, IntPtr lParam);

        [DllImport("user32.dll", CharSet = CharSet.Auto, SetLastError = true)]
        public static extern int GetClassName(IntPtr hWnd, System.Text.StringBuilder lpClassName, int nMaxCount);

        // ── Thread Input (for robust foreground switching) ──
        [DllImport("user32.dll")]
        public static extern bool AttachThreadInput(uint idAttach, uint idAttachTo, bool fAttach);

        [DllImport("kernel32.dll")]
        public static extern uint GetCurrentThreadId();

        [DllImport("user32.dll")]
        public static extern bool BringWindowToTop(IntPtr hWnd);

        // ── keybd_event (legacy but works across UIPI) ──
        [DllImport("user32.dll")]
        public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, UIntPtr dwExtraInfo);

        public const uint KEYEVENTF_EXTENDEDKEY = 0x0001;
        // KEYEVENTF_KEYUP = 0x0002 already defined

        // ── MapVirtualKey (VK → scan code) ──
        public const uint MAPVK_VK_TO_VSC = 0;

        [DllImport("user32.dll")]
        public static extern uint MapVirtualKey(uint uCode, uint uMapType);

        // ── Cursor Position ──
        [StructLayout(LayoutKind.Sequential)]
        public struct POINT
        {
            public int X;
            public int Y;
        }

        [DllImport("user32.dll")]
        public static extern bool GetCursorPos(out POINT lpPoint);

        // Liefert das Fenster-HWND unter einem Bildschirmpunkt (Geraetepixel).
        // Erfasst auch Popup-/Menue-HWNDs (z.B. WPF-ContextMenu-Untermenues),
        // die KEINE Application.Windows-Eintraege sind — darum ueber HWND+PID
        // statt ueber die WPF-Fensterliste pruefen, ob der Cursor ueber
        // unserer eigenen UI liegt.
        [DllImport("user32.dll")]
        public static extern IntPtr WindowFromPoint(POINT Point);

        // ── Mouse Capture ──
        [DllImport("user32.dll")]
        public static extern IntPtr SetCapture(IntPtr hWnd);

        [DllImport("user32.dll")]
        public static extern bool ReleaseCapture();

        // ── Mouse Messages ──
        public const int WM_RBUTTONDOWN = 0x0204;
        public const int WM_MOUSEMOVE   = 0x0200;
        public const int WM_RBUTTONUP   = 0x0205;

        // ── DPI ──
        [DllImport("shcore.dll")]
        public static extern int GetDpiForMonitor(IntPtr hmonitor, int dpiType, out uint dpiX, out uint dpiY);

        // ── Window State Checks ──
        [DllImport("user32.dll")]
        public static extern bool IsWindow(IntPtr hWnd);

        [DllImport("user32.dll")]
        public static extern bool IsWindowVisible(IntPtr hWnd);

        [DllImport("user32.dll")]
        public static extern bool IsIconic(IntPtr hWnd);

        // ── Minimize / Restore Events ──
        public const uint EVENT_SYSTEM_MINIMIZESTART = 0x0016;
        public const uint EVENT_SYSTEM_MINIMIZEEND   = 0x0017;

        // ── ShowWindow / SetWindowPos (fuer Alt+F11 Explorer-Shortcut) ──
        public const int SW_SHOWNORMAL = 1;
        public const int SW_RESTORE    = 9;

        public const uint SWP_NOZORDER   = 0x0004;
        public const uint SWP_NOACTIVATE = 0x0010;
        public const uint SWP_NOMOVE     = 0x0002;
        public const uint SWP_NOSIZE     = 0x0001;
        public const uint SWP_SHOWWINDOW = 0x0040;

        // ── Z-Order constants for SetWindowPos ──
        public static readonly IntPtr HWND_TOPMOST    = new IntPtr(-1);
        public static readonly IntPtr HWND_NOTOPMOST  = new IntPtr(-2);
        public static readonly IntPtr HWND_TOP        = new IntPtr(0);

        [DllImport("user32.dll")]
        [return: MarshalAs(UnmanagedType.Bool)]
        public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);

        [DllImport("user32.dll", SetLastError = true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        public static extern bool SetWindowPos(IntPtr hWnd, IntPtr hWndInsertAfter,
            int X, int Y, int cx, int cy, uint uFlags);

        // ── DwmFlush: blockiert bis zur naechsten DWM-Komposition (vsync).
        // Nach einem SetWindowPos-Move eines Layered-Fensters aufgerufen,
        // synchronisiert es unseren Move mit der Darstellung des Fensters
        // darunter (Terminal) → weniger Flackern/Tearing ueber Text. ──
        [DllImport("dwmapi.dll")]
        public static extern int DwmFlush();
    }
}
