using System;
using Microsoft.UI;
using Microsoft.UI.Windowing;
using Microsoft.UI.Xaml;
using Serilog;
using Windows.Graphics;
using WinRT.Interop;

namespace PromptBoard.App;

public sealed partial class MainWindow : Window
{
    private const int DefaultBarHeight = 200;

    /// <summary>
    /// Pixel offset from the top of the work area so the user's Terminal
    /// (Windows Terminal, Claude Code CLI, etc.) still shows its tab strip
    /// above the PromptBoard bar. Roughly two fingers worth of vertical
    /// space at 100% DPI.
    /// </summary>
    private const int TopOffset = 80;

    /// <summary>
    /// HWND of the currently open main window. Needed by components
    /// (Phase 6 file pickers, Phase 7 OAuth) that have to call
    /// InitializeWithWindow.Initialize before opening.
    /// </summary>
    public static IntPtr CurrentHwnd { get; private set; }

    public MainWindow()
    {
        InitializeComponent();
        ConfigureAsPromptBar();
    }

    /// <summary>
    /// Docks the window to the top of the primary screen, makes it borderless,
    /// always-on-top, and sets WS_EX_NOACTIVATE | WS_EX_TOOLWINDOW so clicking
    /// the bar does not steal focus from the underlying CLI.
    /// </summary>
    private void ConfigureAsPromptBar()
    {
        IntPtr hwnd = WindowNative.GetWindowHandle(this);
        CurrentHwnd = hwnd;
        WindowId windowId = Win32Interop.GetWindowIdFromWindow(hwnd);
        AppWindow appWindow = AppWindow.GetFromWindowId(windowId);

        if (appWindow.Presenter is OverlappedPresenter presenter)
        {
            presenter.SetBorderAndTitleBar(false, false);
            presenter.IsAlwaysOnTop = true;
            presenter.IsMaximizable = false;
            presenter.IsMinimizable = false;
            presenter.IsResizable = true;
        }

        DisplayArea? displayArea = DisplayArea.GetFromWindowId(windowId, DisplayAreaFallback.Primary);
        if (displayArea is not null)
        {
            RectInt32 workArea = displayArea.WorkArea;
            appWindow.MoveAndResize(new RectInt32(
                workArea.X,
                workArea.Y + TopOffset,
                workArea.Width,
                DefaultBarHeight));
        }

        try
        {
            NativeMethods.MakeNoActivateToolWindow(hwnd);
            Log.Information("PromptBar configured: borderless, topmost, no-focus-stealing.");
        }
        catch (Exception ex)
        {
            Log.Warning(ex, "Could not apply WS_EX_NOACTIVATE/WS_EX_TOOLWINDOW.");
        }
    }
}
