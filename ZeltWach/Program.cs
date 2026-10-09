using System.Reflection;
using System.Runtime.InteropServices;
using System.Security;
using Microsoft.Win32;

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
    bool? freigabe;
    int restMinuten = -1;
    DateTime freigabeBis;

    public TrayKontext()
    {
        freigabeBis = DateTime.UtcNow + Schonfrist;
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

        // Die Richtlinie überlebt Neustart und Ruhezustand: vorher freigeben, damit die Anmeldung
        // im Laptop-Modus den Fingerabdruck anbietet. Nach dem Aufwachen sofort neu bewerten.
        SystemEvents.SessionEnding += (_, _) => Fingerabdruck(true);
        SystemEvents.PowerModeChanged += (_, e) =>
        {
            if (e.Mode == PowerModes.Suspend) Fingerabdruck(true);
            if (e.Mode == PowerModes.Resume) { freigabeBis = DateTime.UtcNow + Schonfrist; zelt = null; Pruefen(); }
        };
        Pruefen();
    }

    void Pruefen()
    {
        bool jetztZelt = GetSystemMetrics(SM_CONVERTIBLESLATEMODE) == 0;
        // Nach Start und Aufwachen bleibt der Fingerabdruck auch im Zelt noch kurz erlaubt:
        // die Anmeldung braucht ihn, die Tastatur (und damit die PIN-Eingabe) ist im Zelt aus.
        TimeSpan rest = freigabeBis - DateTime.UtcNow;
        bool jetztFreigabe = !jetztZelt || rest > TimeSpan.Zero;
        int jetztRest = jetztZelt && rest > TimeSpan.Zero ? (int)Math.Ceiling(rest.TotalMinutes) : -1;
        if (jetztZelt == zelt && jetztFreigabe == freigabe && jetztRest == restMinuten) return;
        bool zeltGewechselt = jetztZelt != zelt;
        zelt = jetztZelt;
        freigabe = jetztFreigabe;
        restMinuten = jetztRest;

        bool schalterOk = Fingerabdruck(jetztFreigabe);
        string hinweis = schalterOk ? "" : " (Fingerabdruck-Schalter: keine Berechtigung, install.ps1 ausführen)";

        if (jetztZelt)
        {
            if (zeltGewechselt) SetThreadExecutionState(ES_CONTINUOUS | ES_DISPLAY_REQUIRED | ES_SYSTEM_REQUIRED);
            icon.Icon = SystemIcons.Shield;
            string anmeldung = jetztFreigabe ? $"Fingerabdruck noch {jetztRest} min" : "PIN";
            icon.Text = Kurz($"ZeltWach {version}: Zelt – an, {anmeldung}{hinweis}");
        }
        else
        {
            if (zeltGewechselt) SetThreadExecutionState(ES_CONTINUOUS);
            icon.Icon = SystemIcons.Application;
            icon.Text = Kurz($"ZeltWach {version}: Laptop – normal{hinweis}");
        }
    }

    // Im Zelt ist die Tastatur samt Fingerabdruck-Sensor unerreichbar: Biometrie-Anmeldung aus,
    // damit der Sperrbildschirm direkt die PIN anbietet. Im Laptop-Modus Richtlinie wieder entfernen.
    static bool Fingerabdruck(bool erlaubt)
    {
        try
        {
            using var key = Registry.LocalMachine.OpenSubKey(BiometrieSchluessel, writable: true);
            if (key == null) return false;
            if (erlaubt) key.DeleteValue("Enabled", false);
            else key.SetValue("Enabled", 0, RegistryValueKind.DWord);
            return true;
        }
        catch (Exception e) when (e is UnauthorizedAccessException or SecurityException)
        {
            return false;
        }
    }

    static readonly TimeSpan Schonfrist = TimeSpan.FromMinutes(5);
    const string BiometrieSchluessel = @"SOFTWARE\Policies\Microsoft\Biometrics\Credential Provider";

    // NotifyIcon.Text erlaubt höchstens 127 Zeichen.
    static string Kurz(string s) => s.Length > 127 ? s[..127] : s;

    void Beenden()
    {
        timer.Stop();
        Fingerabdruck(true);
        SetThreadExecutionState(ES_CONTINUOUS);
        icon.Visible = false;
        ExitThread();
    }
}
