using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Text;
using SD = System.Drawing;

namespace JarvisWindows;

/// <summary>Die Fensterschnittstellen, die alle Werkzeuge teilen. Die App läuft nur als win-x64: INPUT hat hier die 64-Bit-Anordnung.</summary>
internal static class WinApi
{
    [DllImport("user32.dll")] public static extern uint GetClipboardSequenceNumber();
    public const int INPUT_MOUSE = 0, INPUT_KEYBOARD = 1;
    public const uint MOUSEEVENTF_MOVE = 0x0001, MOUSEEVENTF_LEFTDOWN = 0x0002, MOUSEEVENTF_LEFTUP = 0x0004, MOUSEEVENTF_RIGHTDOWN = 0x0008, MOUSEEVENTF_RIGHTUP = 0x0010,
        MOUSEEVENTF_WHEEL = 0x0800, MOUSEEVENTF_HWHEEL = 0x1000, MOUSEEVENTF_VIRTUALDESK = 0x4000, MOUSEEVENTF_ABSOLUTE = 0x8000;
    public const uint KEYEVENTF_EXTENDEDKEY = 0x0001, KEYEVENTF_KEYUP = 0x0002, KEYEVENTF_UNICODE = 0x0004;
    public const int SM_XVIRTUALSCREEN = 76, SM_YVIRTUALSCREEN = 77, SM_CXVIRTUALSCREEN = 78, SM_CYVIRTUALSCREEN = 79;
    public const int SW_RESTORE = 9, GW_OWNER = 4, GWL_EXSTYLE = -20, GA_ROOTOWNER = 3, DWMWA_CLOAKED = 14;
    public const long WS_EX_TOOLWINDOW = 0x80, WS_EX_APPWINDOW = 0x40000;
    public const uint ASFW_ANY = 0xFFFFFFFF, WM_CLOSE = 0x0010, VK_MENU = 0x12;
    public const uint EVENT_SYSTEM_FOREGROUND = 3, WINEVENT_OUTOFCONTEXT = 0, WINEVENT_SKIPOWNPROCESS = 2;

    [StructLayout(LayoutKind.Sequential)]
    public struct MOUSEINPUT { public int dx, dy; public uint mouseData, dwFlags, time; public IntPtr dwExtraInfo; }
    [StructLayout(LayoutKind.Sequential)]
    public struct KEYBDINPUT { public ushort wVk, wScan; public uint dwFlags, time; public IntPtr dwExtraInfo; }
    [StructLayout(LayoutKind.Explicit, Size = 40)]
    public struct INPUT
    {
        [FieldOffset(0)] public int type;
        [FieldOffset(8)] public MOUSEINPUT mi;
        [FieldOffset(8)] public KEYBDINPUT ki;
    }

    public delegate bool EnumProc(IntPtr h, IntPtr l);
    public delegate void WinEventProc(IntPtr hook, uint ereignis, IntPtr h, int objekt, int kind, uint faden, uint zeit);

    [DllImport("user32.dll", SetLastError = true)] public static extern uint SendInput(uint n, INPUT[] eingaben, int groesse);
    [DllImport("user32.dll")] public static extern int GetSystemMetrics(int i);
    [DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc f, IntPtr l);
    [DllImport("user32.dll")] public static extern bool EnumChildWindows(IntPtr h, EnumProc f, IntPtr l);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
    [DllImport("user32.dll")] public static extern bool IsWindow(IntPtr h);
    [DllImport("user32.dll")] public static extern bool IsIconic(IntPtr h);
    [DllImport("user32.dll")] public static extern bool IsHungAppWindow(IntPtr h);
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
    [DllImport("user32.dll")] public static extern bool BringWindowToTop(IntPtr h);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int cmd);
    [DllImport("user32.dll")] public static extern bool AllowSetForegroundWindow(uint pid);
    [DllImport("user32.dll")] public static extern bool AttachThreadInput(uint von, uint nach, bool an);
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr h, out int pid);
    [DllImport("user32.dll")] public static extern IntPtr GetWindow(IntPtr h, int was);
    [DllImport("user32.dll")] public static extern IntPtr GetAncestor(IntPtr h, int was);
    [DllImport("user32.dll", EntryPoint = "GetWindowLongPtrW")] public static extern long GetWindowLongPtr(IntPtr h, int i);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern int GetWindowTextW(IntPtr h, StringBuilder s, int n);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern int GetClassNameW(IntPtr h, StringBuilder s, int n);
    [DllImport("user32.dll")] public static extern bool PostMessageW(IntPtr h, uint nachricht, IntPtr w, IntPtr l);
    [DllImport("user32.dll")] public static extern IntPtr SetWinEventHook(uint von, uint bis, IntPtr modul, WinEventProc f, uint pid, uint faden, uint schalter);
    [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern short VkKeyScanExW(char c, IntPtr layout);
    [DllImport("user32.dll")] public static extern IntPtr GetKeyboardLayout(uint faden);
    [DllImport("user32.dll")] public static extern uint MapVirtualKeyW(uint code, uint art);
    [DllImport("user32.dll", SetLastError = true)] public static extern IntPtr OpenInputDesktop(uint schalter, bool erben, uint zugriff);
    [DllImport("user32.dll")] public static extern bool CloseDesktop(IntPtr d);
    [DllImport("kernel32.dll")] public static extern uint GetCurrentThreadId();
    [DllImport("user32.dll")] public static extern IntPtr GetDC(IntPtr h);
    [DllImport("user32.dll")] public static extern int ReleaseDC(IntPtr h, IntPtr dc);
    [DllImport("gdi32.dll", SetLastError = true)] public static extern bool BitBlt(IntPtr ziel, int x, int y, int b, int h, IntPtr quelle, int qx, int qy, uint rop);
    public const uint SRCCOPY = 0x00CC0020, CAPTUREBLT = 0x40000000;
    [DllImport("dwmapi.dll")] public static extern int DwmGetWindowAttribute(IntPtr h, int was, out int wert, int groesse);

    public static readonly int InputGroesse = Marshal.SizeOf<INPUT>();

    /// <summary>Sendet die Eingaben geschlossen. false = Windows hat sie nicht angenommen (gesperrter Bildschirm, sicherer Desktop).
    /// Sperrt UIPI ein erhöhtes Fenster, meldet Windows das nicht: Deshalb prüft das Modell am Foto.</summary>
    public static bool Sende(params INPUT[] e) => e.Length == 0 || SendInput((uint)e.Length, e, InputGroesse) == e.Length;

    public static INPUT Maus(uint schalter, int x = 0, int y = 0, uint daten = 0)
        => new() { type = INPUT_MOUSE, mi = new MOUSEINPUT { dx = x, dy = y, mouseData = daten, dwFlags = schalter } };

    public static INPUT Taste(ushort vk, bool hoch)
    {
        var schalter = hoch ? KEYEVENTF_KEYUP : 0;
        if (vk is 0x21 or 0x22 or 0x23 or 0x24 or 0x25 or 0x26 or 0x27 or 0x28 or 0x2D or 0x2E or 0x5B or 0x5C or 0x2C or 0x6F or 0x90) schalter |= KEYEVENTF_EXTENDEDKEY;
        return new INPUT { type = INPUT_KEYBOARD, ki = new KEYBDINPUT { wVk = vk, wScan = (ushort)MapVirtualKeyW(vk, 0), dwFlags = schalter } };
    }

    public static INPUT Zeichen(char c, bool hoch)
        => new() { type = INPUT_KEYBOARD, ki = new KEYBDINPUT { wScan = c, dwFlags = KEYEVENTF_UNICODE | (hoch ? KEYEVENTF_KEYUP : 0) } };

    /// <summary>Anmeldung, Sperre und UAC laufen auf einem eigenen, sicheren Desktop; von dort gibt es weder Fotos noch Eingaben.</summary>
    public static bool SichererDesktop()
    {
        var d = OpenInputDesktop(0, false, 0x0001);
        if (d == IntPtr.Zero) return true;
        CloseDesktop(d);
        return false;
    }
}

public sealed record FensterInfo(IntPtr Hwnd, int Pid, string Prozess, string Titel, string Klasse);

/// <summary>Oberste Fenster finden und wirklich nach vorn holen. Erfolg zählt nur mit Sichtprüfung: Vordergrund und nicht minimiert.</summary>
public static class Fenster
{
    private static string Text(IntPtr h, bool klasse)
    {
        var s = new StringBuilder(512);
        if (klasse) WinApi.GetClassNameW(h, s, s.Capacity); else WinApi.GetWindowTextW(h, s, s.Capacity);
        return s.ToString();
    }

    public static string ProzessName(int pid)
    {
        try { using var p = Process.GetProcessById(pid); return p.ProcessName; } catch { return ""; }
    }

    public static bool Verhuellt(IntPtr h) => WinApi.DwmGetWindowAttribute(h, WinApi.DWMWA_CLOAKED, out var w, sizeof(int)) == 0 && w != 0;

    /// <summary>Store-Apps hängen unter ApplicationFrameHost: Der echte Prozess steckt im Kindfenster.</summary>
    private static int EchterProzess(IntPtr h, int pid, string name)
    {
        if (!name.Equals("ApplicationFrameHost", StringComparison.OrdinalIgnoreCase)) return pid;
        var echt = pid;
        WinApi.EnumChildWindows(h, (k, _) =>
        {
            WinApi.GetWindowThreadProcessId(k, out var kp);
            if (kp == pid) return true;
            echt = kp;
            return false;
        }, IntPtr.Zero);
        return echt;
    }

    public static FensterInfo Info(IntPtr h)
    {
        WinApi.GetWindowThreadProcessId(h, out var pid);
        var name = ProzessName(pid);
        var echt = EchterProzess(h, pid, name);
        return new FensterInfo(h, echt, echt == pid ? name : ProzessName(echt), Text(h, false), Text(h, true));
    }

    /// <summary>Alle sichtbaren obersten Fenster in Z-Reihenfolge, das zuletzt benutzte zuerst, auch Dialoge und Hilfsfenster.</summary>
    public static List<FensterInfo> Sichtbar()
    {
        var griffe = new List<IntPtr>();
        WinApi.EnumWindows((h, _) => { if (WinApi.IsWindowVisible(h) && !Verhuellt(h)) griffe.Add(h); return true; }, IntPtr.Zero);
        return griffe.Select(Info).ToList();
    }

    /// <summary>Die Fenster, die in der Taskleiste stünden: Das entspricht den „normalen“ Programmen der Mac-Fassung.</summary>
    public static List<FensterInfo> Programme() => Sichtbar().Where(f =>
    {
        if (f.Titel.Length == 0) return false;
        var stil = WinApi.GetWindowLongPtr(f.Hwnd, WinApi.GWL_EXSTYLE);
        if ((stil & WinApi.WS_EX_APPWINDOW) != 0) return true;
        return (stil & WinApi.WS_EX_TOOLWINDOW) == 0 && WinApi.GetWindow(f.Hwnd, WinApi.GW_OWNER) == IntPtr.Zero;
    }).ToList();

    /// <summary>Das Fenster eines Programms: genauer Prozessname, dann Teil des Namens, dann Teil des Titels.</summary>
    public static FensterInfo? Finde(string name)
    {
        var n = name.Trim().ToLowerInvariant();
        if (n.Length == 0) return null;
        var alle = Programme();
        return alle.FirstOrDefault(f => f.Prozess.ToLowerInvariant() == n)
            ?? alle.FirstOrDefault(f => f.Prozess.ToLowerInvariant().Contains(n))
            ?? alle.FirstOrDefault(f => f.Titel.ToLowerInvariant().Contains(n));
    }

    /// <summary>Vordergrund UND nicht minimiert; ein minimiertes Fenster kann den Vordergrund halten (Merkzettel vom 20.09.2026). Sein eigener Dialog vorn zählt mit.</summary>
    public static bool IstVorn(IntPtr h)
    {
        if (!WinApi.IsWindow(h) || WinApi.IsIconic(h) || !WinApi.IsWindowVisible(h)) return false;
        var vorn = WinApi.GetForegroundWindow();
        return vorn == h || (vorn != IntPtr.Zero && WinApi.GetAncestor(vorn, WinApi.GA_ROOTOWNER) == h);
    }

    /// <summary>Holt das Fenster nach vorn und gibt nur true zurück, wenn das danach wirklich gemessen wurde.</summary>
    public static Task<bool> NachVorn(IntPtr h) => Task.Run(() =>
    {
        if (!WinApi.IsWindow(h)) return false;
        for (var versuch = 0; versuch < 3; versuch++)
        {
            if (WinApi.IsIconic(h)) WinApi.ShowWindow(h, WinApi.SW_RESTORE);
            WinApi.AllowSetForegroundWindow(WinApi.ASFW_ANY);
            var eigen = WinApi.GetCurrentThreadId();
            var vorn = WinApi.GetForegroundWindow();
            var vornFaden = vorn != IntPtr.Zero ? WinApi.GetWindowThreadProcessId(vorn, out _) : 0;
            var zielFaden = WinApi.GetWindowThreadProcessId(h, out _);
            var anVorn = vornFaden != 0 && vornFaden != eigen && WinApi.AttachThreadInput(eigen, vornFaden, true);
            var anZiel = zielFaden != eigen && zielFaden != vornFaden && WinApi.AttachThreadInput(eigen, zielFaden, true);
            // Ab dem zweiten Versuch mit gehaltener Alt-Taste: Das hebt die Vordergrundsperre auf; losgelassen wird erst nach dem Wechsel, damit kein Menü aufgeht.
            var alt = versuch > 0;
            try
            {
                if (alt) WinApi.Sende(WinApi.Taste((ushort)WinApi.VK_MENU, false));
                WinApi.BringWindowToTop(h);
                WinApi.SetForegroundWindow(h);
            }
            finally
            {
                // Alt allein loslassen öffnete im Zielfenster das Menü, das folgende Strg+V oder Enter landete dort. Eine unbelegte Taste (F24) dazwischen verhindert das.
                if (alt) WinApi.Sende(WinApi.Taste(0x87, false), WinApi.Taste(0x87, true), WinApi.Taste((ushort)WinApi.VK_MENU, true));
                if (anVorn) WinApi.AttachThreadInput(eigen, vornFaden, false);
                if (anZiel) WinApi.AttachThreadInput(eigen, zielFaden, false);
            }
            for (var i = 0; i < 6; i++)
            {
                if (IstVorn(h)) return true;
                Thread.Sleep(100);
            }
        }
        return false;
    });
}

/// <summary>Computer Use: Maus am PC (Tastatur macht windows_tastatur). Koordinaten sind Pixel im letzten Bild aus windows_bildschirm.</summary>
public static class WinBedienung
{
    private static readonly object Sperre = new();
    private static SD.Size bild = SD.Size.Empty;
    private static SD.Rectangle quelle = SD.Rectangle.Empty;

    /// <summary>Der ganze virtuelle Desktop in echten Pixeln (die App ist PerMonitorV2).</summary>
    public static SD.Rectangle VirtuellerDesktop() => new(
        WinApi.GetSystemMetrics(WinApi.SM_XVIRTUALSCREEN), WinApi.GetSystemMetrics(WinApi.SM_YVIRTUALSCREEN),
        WinApi.GetSystemMetrics(WinApi.SM_CXVIRTUALSCREEN), WinApi.GetSystemMetrics(WinApi.SM_CYVIRTUALSCREEN));

    /// <summary>Merkt Bildgröße, Ursprung und Skalierung des letzten Fotos.</summary>
    public static SD.Size MerkeBild(SD.Size groesse, SD.Rectangle bereich)
    {
        lock (Sperre) { bild = groesse; quelle = bereich; }
        return groesse;
    }

    /// <summary>Bild-Pixel auf normierte Koordinaten des virtuellen Desktops; null, solange es kein Bild gibt oder wenn sich die Monitore seither geändert haben.</summary>
    private static (int X, int Y)? Punkt(double x, double y, out string? fehler)
    {
        SD.Size g; SD.Rectangle r;
        lock (Sperre) { g = bild; r = quelle; }
        fehler = null;
        if (g.Width <= 0 || g.Height <= 0)
        {
            fehler = "Hol zuerst mit windows_bildschirm ein Bild; Koordinaten beziehen sich darauf.";
            return null;
        }
        var v = VirtuellerDesktop();
        if (v != r)
        {
            fehler = "Die Monitore haben sich seit dem letzten Bild geändert. Hol mit windows_bildschirm ein neues.";
            return null;
        }
        var px = r.Left + Math.Min(Math.Max(x / g.Width, 0), 0.9999) * r.Width;
        var py = r.Top + Math.Min(Math.Max(y / g.Height, 0), 0.9999) * r.Height;
        return ((int)Math.Round((px - v.Left) * 65535.0 / Math.Max(v.Width - 1, 1)), (int)Math.Round((py - v.Top) * 65535.0 / Math.Max(v.Height - 1, 1)));
    }

    private const uint Bewegen = WinApi.MOUSEEVENTF_MOVE | WinApi.MOUSEEVENTF_ABSOLUTE | WinApi.MOUSEEVENTF_VIRTUALDESK;
    private const string Abgewiesen = "Windows hat die Eingabe nicht angenommen (gesperrter Bildschirm oder eine Sicherheitsabfrage ist offen).";

    private static uint Klick(uint schalter) => schalter | WinApi.MOUSEEVENTF_ABSOLUTE | WinApi.MOUSEEVENTF_VIRTUALDESK;

    /// <summary>Führt die Aktion aus; Rückgabe ist die Fehlermeldung oder null.</summary>
    public static async Task<string?> Ausfuehren(JSONObjekt a)
    {
        var aktion = a.Text("aktion") ?? "";
        if (aktion == "scrollen" && a.Zahl("x") == null) return await Scrollen(a) ? null : Abgewiesen;
        if (a.Zahl("x") is not double x || a.Zahl("y") is not double y) return $"{aktion} braucht x und y.";
        if (Punkt(x, y, out var fehler) is not { } p) return fehler;
        if (!WinApi.Sende(WinApi.Maus(Bewegen, p.X, p.Y))) return Abgewiesen;
        await Task.Delay(30);
        switch (aktion)
        {
            case "bewegen":
                return null;
            case "klick":
                return WinApi.Sende(WinApi.Maus(Klick(WinApi.MOUSEEVENTF_LEFTDOWN), p.X, p.Y), WinApi.Maus(Klick(WinApi.MOUSEEVENTF_LEFTUP), p.X, p.Y)) ? null : Abgewiesen;
            case "doppelklick":
                return WinApi.Sende(WinApi.Maus(Klick(WinApi.MOUSEEVENTF_LEFTDOWN), p.X, p.Y), WinApi.Maus(Klick(WinApi.MOUSEEVENTF_LEFTUP), p.X, p.Y),
                    WinApi.Maus(Klick(WinApi.MOUSEEVENTF_LEFTDOWN), p.X, p.Y), WinApi.Maus(Klick(WinApi.MOUSEEVENTF_LEFTUP), p.X, p.Y)) ? null : Abgewiesen;
            case "rechtsklick":
                return WinApi.Sende(WinApi.Maus(Klick(WinApi.MOUSEEVENTF_RIGHTDOWN), p.X, p.Y), WinApi.Maus(Klick(WinApi.MOUSEEVENTF_RIGHTUP), p.X, p.Y)) ? null : Abgewiesen;
            case "ziehen":
                if (a.Zahl("bis_x") is not double zx || a.Zahl("bis_y") is not double zy) return "ziehen braucht bis_x und bis_y.";
                if (Punkt(zx, zy, out fehler) is not { } ziel) return fehler;
                if (!WinApi.Sende(WinApi.Maus(Klick(WinApi.MOUSEEVENTF_LEFTDOWN), p.X, p.Y))) return Abgewiesen;
                // Die Taste wird in jedem Fall wieder losgelassen, sonst bliebe sie für Frank „gedrückt“.
                try
                {
                    for (var i = 1; i <= 12; i++)
                    {
                        if (Punkt(x + (zx - x) * i / 12, y + (zy - y) * i / 12, out _) is { } q) WinApi.Sende(WinApi.Maus(Bewegen, q.X, q.Y));
                        await Task.Delay(15);
                    }
                }
                finally
                {
                    WinApi.Sende(WinApi.Maus(Bewegen, ziel.X, ziel.Y), WinApi.Maus(Klick(WinApi.MOUSEEVENTF_LEFTUP), ziel.X, ziel.Y));
                }
                return null;
            case "scrollen":
                return await Scrollen(a) ? null : Abgewiesen;
            default:
                return $"Unbekannte Aktion {aktion}.";
        }
    }

    /// <summary>Pixel wie am Mac: Rund 100 Pixel entsprechen einer Raste (120). Positiv = nach unten bzw. rechts; das Mausrad zählt senkrecht umgekehrt.</summary>
    private static async Task<bool> Scrollen(JSONObjekt a)
    {
        static int Rasten(double px) => px == 0 ? 0 : Math.Sign(px) * Math.Max(120, (int)Math.Round(Math.Abs(px) * 1.2));
        async Task<bool> Rad(uint schalter, int menge)
        {
            while (menge != 0)
            {
                var teil = Math.Sign(menge) * Math.Min(Math.Abs(menge), 120);
                if (!WinApi.Sende(WinApi.Maus(schalter, daten: unchecked((uint)teil)))) return false;
                menge -= teil;
                if (menge != 0) await Task.Delay(8);
            }
            return true;
        }
        return await Rad(WinApi.MOUSEEVENTF_WHEEL, -Rasten(a.Zahl("scroll_y") ?? 0)) && await Rad(WinApi.MOUSEEVENTF_HWHEEL, Rasten(a.Zahl("scroll_x") ?? 0));
    }
}
