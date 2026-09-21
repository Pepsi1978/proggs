using System.Reflection;
using System.Runtime.InteropServices;

namespace ZeltWach;

// Hält den Bildschirm wach, solange das Display umgeklappt ist (Zelt/Tablet).
// Im Laptop-Modus greift das Tool nicht ein: Bildschirm-Aus und Sperre laufen normal.
static class Program
{
    [STAThread]
    static void Main()
    {
        using var mutex = new Mutex(true, @"Local\ZeltWach", out bool erstesExemplar);
        if (!erstesExemplar) return;

        ApplicationConfiguration.Initialize();
        Application.Run(new TrayKontext());
    }
}

sealed class TrayKontext : ApplicationContext
{
    const int SM_CONVERTIBLESLATEMODE = 0x2003;
    const uint ES_CONTINUOUS = 0x80000000;
    const uint ES_SYSTEM_REQUIRED = 0x00000001;
    const uint ES_DISPLAY_REQUIRED = 0x00000002;

    [DllImport("user32.dll")] static extern int GetSystemMetrics(int index);
    [DllImport("user32.dll")] static extern bool LockWorkStation();
    [DllImport("kernel32.dll")] static extern uint SetThreadExecutionState(uint flags);

    readonly NotifyIcon icon;
    readonly System.Windows.Forms.Timer timer;
    readonly string version;
    bool? zelt;

    public TrayKontext()
    {
        version = Assembly.GetExecutingAssembly().GetName().Version?.ToString(3) ?? "?";

        var menu = new ContextMenuStrip();
        menu.Items.Add("Sperren", null, (_, _) => LockWorkStation());
        menu.Items.Add(new ToolStripSeparator());
        menu.Items.Add($"ZeltWach {version}").Enabled = false;
        menu.Items.Add("Beenden", null, (_, _) => Beenden());

        icon = new NotifyIcon { ContextMenuStrip = menu, Visible = true };

        // Die Ausführungsanforderung gilt pro Thread, deshalb läuft alles im UI-Thread über diesen Timer.
        timer = new System.Windows.Forms.Timer { Interval = 1000 };
        timer.Tick += (_, _) => Pruefen();
        timer.Start();
        Pruefen();
    }

    void Pruefen()
    {
        bool jetztZelt = GetSystemMetrics(SM_CONVERTIBLESLATEMODE) == 0;
        if (jetztZelt == zelt) return;
        zelt = jetztZelt;

        if (jetztZelt)
        {
            SetThreadExecutionState(ES_CONTINUOUS | ES_DISPLAY_REQUIRED | ES_SYSTEM_REQUIRED);
            icon.Icon = SystemIcons.Shield;
            icon.Text = $"ZeltWach {version}: Zelt – Bildschirm bleibt an";
        }
        else
        {
            SetThreadExecutionState(ES_CONTINUOUS);
            icon.Icon = SystemIcons.Application;
            icon.Text = $"ZeltWach {version}: Laptop – normal";
        }
    }

    void Beenden()
    {
        timer.Stop();
        SetThreadExecutionState(ES_CONTINUOUS);
        icon.Visible = false;
        ExitThread();
    }
}
