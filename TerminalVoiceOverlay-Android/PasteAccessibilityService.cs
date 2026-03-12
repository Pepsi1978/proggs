using Android.AccessibilityServices;
using Android.App;
using Android.Content;
using Android.Views.Accessibility;

namespace TerminalVoiceOverlay;

// Minimal AccessibilityService that allows the overlay to paste text into
// the currently focused app (e.g. Termux) and to delete previously pasted text
// by injecting a Ctrl+U line-kill character.
//
// The user must manually enable this service in Android Settings → Accessibility.
[Service(
    Permission = "android.permission.BIND_ACCESSIBILITY_SERVICE",
    Exported = false)]
[IntentFilter(new[] { "android.accessibilityservice.AccessibilityService" })]
[MetaData("android.accessibilityservice", Resource = "@xml/accessibility_config")]
public class PasteAccessibilityService : AccessibilityService
{
    /// <summary>
    /// Singleton instance — set when the service is connected, cleared on unbind.
    /// OverlayService checks this to determine whether auto-paste is available.
    /// </summary>
    public static PasteAccessibilityService? Instance { get; private set; }

    public static bool IsConnected => Instance != null;

    // We don't need to process accessibility events — this service is only
    // used for its ability to perform actions on the focused window.
    public override void OnAccessibilityEvent(AccessibilityEvent? e) { }
    public override void OnInterrupt() { }

    protected override void OnServiceConnected()
    {
        base.OnServiceConnected();
        Instance = this;
    }

    public override bool OnUnbind(Intent? intent)
    {
        Instance = null;
        return base.OnUnbind(intent);
    }

    // ACTION_PASTE constant from the Android SDK (0x8000 = 32768).
    // .NET Android binds this as Android.Views.Accessibility.Action enum.
    private const global::Android.Views.Accessibility.Action ActionPaste =
        (global::Android.Views.Accessibility.Action)0x00008000;

    /// <summary>
    /// Performs ACTION_PASTE on the currently focused input node.
    /// The caller must have already placed text on the clipboard before calling this.
    /// </summary>
    /// <returns>true if the paste action was dispatched successfully.</returns>
    public bool PerformPaste()
    {
        var root = RootInActiveWindow;
        if (root == null) return false;

        try
        {
            // Try the focused input node first (most reliable).
            var focused = root.FindFocus(NodeFocus.Input);
            if (focused != null)
                return focused.PerformAction(ActionPaste);

            // Fallback: try accessibility-focused node.
            var accFocused = root.FindFocus(NodeFocus.Accessibility);
            if (accFocused != null)
                return accFocused.PerformAction(ActionPaste);

            // Last resort: try paste on the root window itself.
            return root.PerformAction(ActionPaste);
        }
        catch
        {
            return false;
        }
        finally
        {
            root.Recycle();
        }
    }
}
