using System.Runtime.InteropServices;
using System.Windows;
using System.Windows.Interop;
using System.Windows.Threading;

namespace JarvisWindows;

/// <summary>Win32-Aufrufe der Oberfläche.</summary>
internal static partial class Nativ
{
    public const int WM_HOTKEY = 0x0312, WM_POWERBROADCAST = 0x0218, PBT_POWERSETTINGCHANGE = 0x8013;
    public const uint MOD_ALT = 1, MOD_CONTROL = 2, MOD_NOREPEAT = 0x4000;
    public const int GWL_EXSTYLE = -20, WS_EX_TOOLWINDOW = 0x80, WS_EX_NOACTIVATE = 0x08000000, WS_EX_TOPMOST = 8;
    public const int SW_RESTORE = 9;
    public const uint WDA_EXCLUDEFROMCAPTURE = 0x11;
    public static readonly IntPtr HWND_BROADCAST = new(0xFFFF);
    public static readonly IntPtr HWND_TOPMOST = new(-1);
    public const uint SWP_NOSIZE = 1, SWP_NOACTIVATE = 0x10, SWP_NOZORDER = 4;
    public const int ASFW_ANY = -1;
    public const int DWMWA_USE_IMMERSIVE_DARK_MODE = 20, DWMWA_WINDOW_CORNER_PREFERENCE = 33;

    [StructLayout(LayoutKind.Sequential)] public struct RECT { public int L, T, R, B; }
    [StructLayout(LayoutKind.Sequential)] public struct MONITORINFO { public int cbSize; public RECT Monitor, Arbeit; public uint Flags; }
    [StructLayout(LayoutKind.Sequential, Pack = 4)] public struct POWERBROADCAST_SETTING { public Guid PowerSetting; public uint DataLength; public byte Data; }

    [LibraryImport("user32.dll", StringMarshalling = StringMarshalling.Utf16)] public static partial uint RegisterWindowMessageW(string name);
    [LibraryImport("user32.dll")][return: MarshalAs(UnmanagedType.Bool)] public static partial bool PostMessageW(IntPtr h, uint m, IntPtr w, IntPtr l);
    [LibraryImport("user32.dll")][return: MarshalAs(UnmanagedType.Bool)] public static partial bool AllowSetForegroundWindow(int pid);
    [LibraryImport("user32.dll", SetLastError = true)][return: MarshalAs(UnmanagedType.Bool)] public static partial bool RegisterHotKey(IntPtr h, int id, uint mods, uint vk);
    [LibraryImport("user32.dll")][return: MarshalAs(UnmanagedType.Bool)] public static partial bool UnregisterHotKey(IntPtr h, int id);
    [LibraryImport("user32.dll")] public static partial IntPtr GetForegroundWindow();
    [LibraryImport("user32.dll")][return: MarshalAs(UnmanagedType.Bool)] public static partial bool SetForegroundWindow(IntPtr h);
    [LibraryImport("user32.dll")][return: MarshalAs(UnmanagedType.Bool)] public static partial bool IsIconic(IntPtr h);
    [LibraryImport("user32.dll")][return: MarshalAs(UnmanagedType.Bool)] public static partial bool ShowWindow(IntPtr h, int cmd);
    [LibraryImport("user32.dll", EntryPoint = "GetWindowLongPtrW")] public static partial IntPtr GetWindowLong(IntPtr h, int i);
    [LibraryImport("user32.dll", EntryPoint = "SetWindowLongPtrW")] public static partial IntPtr SetWindowLong(IntPtr h, int i, IntPtr v);
    [LibraryImport("user32.dll")][return: MarshalAs(UnmanagedType.Bool)] public static partial bool SetWindowDisplayAffinity(IntPtr h, uint a);
    [LibraryImport("user32.dll")][return: MarshalAs(UnmanagedType.Bool)] public static partial bool SetWindowPos(IntPtr h, IntPtr nach, int x, int y, int cx, int cy, uint f);
    [LibraryImport("user32.dll")] public static partial IntPtr MonitorFromWindow(IntPtr h, uint f);
    [LibraryImport("user32.dll", EntryPoint = "GetMonitorInfoW")][return: MarshalAs(UnmanagedType.Bool)] public static partial bool GetMonitorInfo(IntPtr m, ref MONITORINFO i);
    [LibraryImport("shcore.dll")] public static partial int GetDpiForMonitor(IntPtr m, int art, out uint x, out uint y);
    [LibraryImport("dwmapi.dll")] public static partial int DwmSetWindowAttribute(IntPtr h, int a, ref int v, int g);
    [LibraryImport("user32.dll", EntryPoint = "RegisterPowerSettingNotification")] public static partial IntPtr RegisterPowerSettingNotification(IntPtr h, ref Guid g, int f);
    [LibraryImport("user32.dll")][return: MarshalAs(UnmanagedType.Bool)] public static partial bool DestroyIcon(IntPtr h);
}

/// <summary>Einstieg. Einzelinstanz per Mutex; `--hintergrund` startet ohne Fenster (so weckt das Handy die App); ein zweiter Start holt das Fenster nach vorn.</summary>
public static class JarvisApp
{
    private const string MutexName = @"Local\JarvisWindows-Einzelinstanz";
    private static uint nachrichtZeigen;
    private static uint nachrichtBeenden;
    private static bool gesperrt, schlaeft, anzeigeAus;
    private static HwndSource? botschaften;
    private static System.Windows.Forms.NotifyIcon? symbol;
    private static MenueLeiste? menue;
    private static DateTime menueZu = DateTime.MinValue;
    private static bool abschlussLief;
    private static Guid anzeigeStand = new("6FE69556-704A-47A0-8F24-C28D936FDA47"); // GUID_CONSOLE_DISPLAY_STATE

    /// <summary>Griff des unsichtbaren Botschaftsfensters: Empfänger der globalen Kürzel, auch ohne Hauptfenster.</summary>
    public static IntPtr Griff => botschaften?.Handle ?? IntPtr.Zero;
    /// <summary>Das Symbol im Infobereich; Shell.Mitteilung zeigt seine Hinweise darüber.</summary>
    public static System.Windows.Forms.NotifyIcon? Symbol => symbol;
    /// <summary>Gesperrt, im Ruhezustand oder Anzeige aus. Drei getrennte Stände: Eine aufwachende Anzeige hebt eine Sperre nicht auf.</summary>
    public static bool BildschirmAus => gesperrt || schlaeft || anzeigeAus;

    [STAThread]
    public static int Main(string[] args)
    {
        var hintergrund = args.Contains("--hintergrund", StringComparer.OrdinalIgnoreCase);
        // publish.ps1: sauber beenden lassen, damit Einstellungen, Zugang und Gespräche noch auf die Platte kommen.
        var beenden = args.Contains("--beenden", StringComparer.OrdinalIgnoreCase);
        nachrichtZeigen = Nativ.RegisterWindowMessageW("JarvisWindows.FensterZeigen");
        nachrichtBeenden = Nativ.RegisterWindowMessageW("JarvisWindows.Beenden");
        using var einzel = new Mutex(true, MutexName, out var erster);
        if (beenden)
        {
            if (!erster) Nativ.PostMessageW(Nativ.HWND_BROADCAST, nachrichtBeenden, IntPtr.Zero, IntPtr.Zero);
            return 0;
        }
        if (!erster)
        {
            if (!hintergrund)
            {
                Nativ.AllowSetForegroundWindow(Nativ.ASFW_ANY);
                Nativ.PostMessageW(Nativ.HWND_BROADCAST, nachrichtZeigen, IntPtr.Zero, IntPtr.Zero);
            }
            return 0;
        }
        // Zuerst: Haupt.Post fiele ohne Application.Current auf den direkten Aufruf zurück, Meldungen liefen sonst auf Hintergrundfäden.
        var app = new Application { ShutdownMode = ShutdownMode.OnExplicitShutdown };
        ScrollLeiste.Einrichten(app);
        app.DispatcherUnhandledException += (_, e) =>
        {
            Log("fehler", "Unbehandelt in der Oberfläche: " + e.Exception.Message, wo: "App", fehler: e.Exception.ToString());
            e.Handled = true;
        };
        TaskScheduler.UnobservedTaskException += (_, e) => { Log("fehler", "Unbeobachtete Aufgabe: " + e.Exception.GetBaseException().Message, wo: "App"); e.SetObserved(); };
        AppDomain.CurrentDomain.UnhandledException += (_, e) => Log("fehler", "Absturz: " + e.ExceptionObject, wo: "App");
        app.Startup += (_, _) => FertigGestartet(hintergrund);
        app.Exit += (_, _) => Abschluss();
        // Beim Abmelden kann noch abgebrochen werden: Dann nur sichern. Symbol und Abschluss erst, wenn es wirklich endet.
        Microsoft.Win32.SystemEvents.SessionEnding += (_, _) => Sichern();
        Microsoft.Win32.SystemEvents.SessionEnded += (_, _) => Abschluss();
        Microsoft.Win32.SystemEvents.SessionSwitch += (_, e) =>
        {
            if (e.Reason == Microsoft.Win32.SessionSwitchReason.SessionLock) gesperrt = true;
            if (e.Reason == Microsoft.Win32.SessionSwitchReason.SessionUnlock) gesperrt = false;
        };
        Microsoft.Win32.SystemEvents.PowerModeChanged += (_, e) =>
        {
            if (e.Mode == Microsoft.Win32.PowerModes.Suspend) schlaeft = true;
            if (e.Mode == Microsoft.Win32.PowerModes.Resume) schlaeft = false;
        };
        return app.Run();
    }

    private static void FertigGestartet(bool hintergrund)
    {
        Logbuch.Shared.Schreib("system", $"Jarvis {AppInfo.Version} gestartet");
        // Alle Schlüssel gleich beim Start in den Tresor übernehmen und für das Schwärzen bekannt machen, nicht erst bei der ersten Nutzung.
        // Die Ordner unter %APPDATA% sind schon nur für Frank zugänglich; die Rechte-Schritte der Mac-Fassung entfallen.
        _ = Einstellungen.Shared;
        _ = VorleseEinstellungen.Shared;
        _ = GespraechsSpeicher.Shared;
        Ui.Starte(async () => { _ = await CodexAnmeldung.Shared.Angemeldet(); });
        Task.Run(() => AblageLader.Aufraeumen());
        AblageAbgleich.Shared.Hochfahren();
        ErinnerungPlaner.Shared.Starten();
        Ui.Starte(async () => { _ = await McpKlient.Shared.Werkzeuge(frisch: true); });
        BotschaftenAnlegen();
        SymbolAnlegen();
        AppSteuerung.Shared.Hochfahren();
        if (!hintergrund) AppSteuerung.Shared.FensterZeigen();
        else AppSteuerung.Shared.FensterZustandGeaendert();
    }

    private static void Sichern()
    {
        Einstellungen.Shared.ZugangSichern();
        VorleseEinstellungen.Shared.ZugangSichern();
        Vorgaben.Sichern();
        GespraechsSpeicher.Abschliessen();
    }

    private static void Abschluss()
    {
        if (abschlussLief) return;
        abschlussLief = true;
        try { TresorSperre.Shared.Sperren(); } catch { }
        Logbuch.Shared.Schreib("system", "Jarvis beendet");
        Sichern();
        if (symbol != null) { symbol.Visible = false; symbol.Dispose(); symbol = null; }
        Thread.Sleep(100); // Das Logbuch schreibt im Hintergrund: Der letzte Eintrag soll noch auf die Platte.
    }

    public static void Beenden() => Application.Current.Shutdown();

    /// <summary>Unsichtbares Fenster oberster Ebene: Es bekommt WM_HOTKEY, die Nachricht des Zweitstarts und den Anzeigestand, auch solange es kein Hauptfenster gibt.</summary>
    private static void BotschaftenAnlegen()
    {
        botschaften = new HwndSource(new HwndSourceParameters("JarvisWindowsBotschaften") { Width = 0, Height = 0, WindowStyle = 0, ExtendedWindowStyle = Nativ.WS_EX_TOOLWINDOW });
        botschaften.AddHook(Botschaft);
        Nativ.RegisterPowerSettingNotification(botschaften.Handle, ref anzeigeStand, 0);
    }

    private static IntPtr Botschaft(IntPtr h, int msg, IntPtr w, IntPtr l, ref bool erledigt)
    {
        if (msg == nachrichtZeigen) { AppSteuerung.Shared.FensterZeigen(); erledigt = true; return IntPtr.Zero; }
        if (msg == nachrichtBeenden) { Beenden(); erledigt = true; return IntPtr.Zero; }
        switch (msg)
        {
            case Nativ.WM_HOTKEY:
                if (w == 1) AppSteuerung.Shared.KuerzelSprechen(); else if (w == 2) AppSteuerung.Shared.FensterZeigen();
                erledigt = true;
                break;
            case Nativ.WM_POWERBROADCAST when w == Nativ.PBT_POWERSETTINGCHANGE:
                var s = Marshal.PtrToStructure<Nativ.POWERBROADCAST_SETTING>(l);
                if (s.PowerSetting == anzeigeStand) anzeigeAus = s.Data == 0;
                break;
        }
        return IntPtr.Zero;
    }

    /// <summary>Das Symbol im Infobereich wie die Mac-Menüleiste: Linksklick zeigt die Leiste mit Frage und Knöpfen, Rechtsklick das kurze Menü.</summary>
    private static void SymbolAnlegen()
    {
        var kontext = new System.Windows.Forms.ContextMenuStrip();
        kontext.Items.Add("Jarvis öffnen  Strg+Alt+K", null, (_, _) => AppSteuerung.Shared.FensterZeigen());
        kontext.Items.Add("Sprechen  Strg+Alt+J", null, (_, _) => AppSteuerung.Shared.KuerzelSprechen());
        kontext.Items.Add("Live", null, (_, _) => { AppSteuerung.Shared.FensterZeigen(); AppSteuerung.Shared.Live(); });
        kontext.Items.Add(new System.Windows.Forms.ToolStripSeparator());
        kontext.Items.Add("Beenden", null, (_, _) => Beenden());
        symbol = new System.Windows.Forms.NotifyIcon { Text = AppInfo.Name, Icon = SymbolBild(), ContextMenuStrip = kontext, Visible = true };
        Mitteilungen.Symbol = symbol;
        symbol.MouseUp += (_, e) =>
        {
            if (e.Button != System.Windows.Forms.MouseButtons.Left) return;
            if (menue is { IsVisible: true }) { menue.Close(); return; }
            // Der Klick auf das Symbol hat die offene Leiste eben geschlossen: Das heißt zu, nicht wieder auf.
            if ((DateTime.Now - menueZu).TotalMilliseconds < 300) return;
            menue = new MenueLeiste();
            menue.Closed += (_, _) => menueZu = DateTime.Now;
            menue.Show();
            menue.Activate();
        };
    }

    /// <summary>Ohne mitgelieferte Icon-Datei: die Kugel klein gezeichnet.</summary>
    private static System.Drawing.Icon SymbolBild()
    {
        try
        {
            var pfad = Pfade.Ressource("AppIcon.ico");
            if (File.Exists(pfad)) return new System.Drawing.Icon(pfad);
        }
        catch { }
        using var b = new System.Drawing.Bitmap(32, 32);
        using (var g = System.Drawing.Graphics.FromImage(b))
        {
            g.SmoothingMode = System.Drawing.Drawing2D.SmoothingMode.AntiAlias;
            using var p = new System.Drawing.Drawing2D.GraphicsPath();
            p.AddEllipse(2, 2, 28, 28);
            using var v = new System.Drawing.Drawing2D.PathGradientBrush(p) { CenterColor = System.Drawing.Color.FromArgb(255, 255, 236, 200), SurroundColors = new[] { System.Drawing.Color.FromArgb(255, 255, 140, 26) }, CenterPoint = new System.Drawing.PointF(13.5f, 12) };
            g.FillEllipse(v, 2, 2, 28, 28);
        }
        var griff = b.GetHicon();
        var kopie = (System.Drawing.Icon)System.Drawing.Icon.FromHandle(griff).Clone();
        Nativ.DestroyIcon(griff);
        return kopie;
    }
}
