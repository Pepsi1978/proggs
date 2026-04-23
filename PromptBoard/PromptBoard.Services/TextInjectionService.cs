using System;
using System.Runtime.InteropServices;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;
using PromptBoard.Core.Services;
using Windows.ApplicationModel.DataTransfer;

namespace PromptBoard.Services;

/// <summary>
/// Inserts text into the currently focused window by:
///   1. setting the system clipboard via DataPackage,
///   2. flushing so the clipboard survives after SendInput,
///   3. simulating Ctrl+V via user32!SendInput.
///
/// The PromptBoard bar window is configured with WS_EX_NOACTIVATE, so it
/// never steals keyboard focus; the CLI that was focused before the click
/// receives the Ctrl+V directly.
/// </summary>
public sealed class TextInjectionService : ITextInjectionService
{
    private readonly ILogger<TextInjectionService> _logger;

    public TextInjectionService(ILogger<TextInjectionService> logger)
    {
        _logger = logger;
    }

    public async Task InjectAsync(string text, CancellationToken ct = default)
    {
        if (string.IsNullOrEmpty(text))
        {
            _logger.LogWarning("InjectAsync called with empty text; skipping.");
            return;
        }

        SetClipboard(text);

        // Short pause so clipboard content is definitely visible to the CLI.
        try
        {
            await Task.Delay(30, ct);
        }
        catch (OperationCanceledException)
        {
            return;
        }

        SendCtrlV();
    }

    private void SetClipboard(string text)
    {
        try
        {
            var package = new DataPackage();
            package.SetText(text);
            Clipboard.SetContent(package);
            Clipboard.Flush();
            _logger.LogInformation("Clipboard set: {Length} chars.", text.Length);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to set system clipboard.");
            throw;
        }
    }

    private void SendCtrlV()
    {
        const ushort VK_CONTROL = 0x11;
        const ushort VK_V = 0x56;

        INPUT[] inputs =
        [
            MakeKey(VK_CONTROL, keyUp: false),
            MakeKey(VK_V, keyUp: false),
            MakeKey(VK_V, keyUp: true),
            MakeKey(VK_CONTROL, keyUp: true),
        ];

        uint sent = SendInput((uint)inputs.Length, inputs, Marshal.SizeOf<INPUT>());
        if (sent != inputs.Length)
        {
            int error = Marshal.GetLastWin32Error();
            _logger.LogWarning("SendInput delivered only {Sent}/{Total} events. Win32 error={Err}.", sent, inputs.Length, error);
        }
    }

    private static INPUT MakeKey(ushort virtualKey, bool keyUp) => new()
    {
        type = INPUT_KEYBOARD,
        u = new InputUnion
        {
            ki = new KEYBDINPUT
            {
                wVk = virtualKey,
                wScan = 0,
                dwFlags = keyUp ? KEYEVENTF_KEYUP : 0,
                time = 0,
                dwExtraInfo = nint.Zero,
            },
        },
    };

    // -------- P/Invoke --------

    private const uint INPUT_KEYBOARD = 1;
    private const uint KEYEVENTF_KEYUP = 0x0002;

    [DllImport("user32.dll", SetLastError = true)]
    private static extern uint SendInput(uint nInputs, INPUT[] pInputs, int cbSize);

    [StructLayout(LayoutKind.Sequential)]
    private struct INPUT
    {
        public uint type;
        public InputUnion u;
    }

    [StructLayout(LayoutKind.Explicit)]
    private struct InputUnion
    {
        [FieldOffset(0)] public MOUSEINPUT mi;
        [FieldOffset(0)] public KEYBDINPUT ki;
        [FieldOffset(0)] public HARDWAREINPUT hi;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct MOUSEINPUT
    {
        public int dx;
        public int dy;
        public uint mouseData;
        public uint dwFlags;
        public uint time;
        public nint dwExtraInfo;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct KEYBDINPUT
    {
        public ushort wVk;
        public ushort wScan;
        public uint dwFlags;
        public uint time;
        public nint dwExtraInfo;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct HARDWAREINPUT
    {
        public uint uMsg;
        public ushort wParamL;
        public ushort wParamH;
    }
}
