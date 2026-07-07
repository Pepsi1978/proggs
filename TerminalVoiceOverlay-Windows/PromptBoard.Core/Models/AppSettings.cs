namespace PromptBoard.Core.Models;

/// <summary>
/// Singleton row of global user settings. The repository ensures exactly
/// one row exists and returns it for reads.
/// </summary>
/// <remarks>
/// NOTE: API keys are stored in this entity per spec, but production code
/// should write them into Windows PasswordVault (DPAPI) via the settings
/// service (Phase 7). For the MVP they live here, encrypted-at-rest only
/// by the OS file ACL on the SQLite file.
/// </remarks>
public class AppSettings : BaseEntity
{
    public string? GroqApiKey { get; set; }

    public string? GeminiApiKey { get; set; }

    public string? GoogleOAuthRefreshToken { get; set; }

    /// <summary>
    /// OAuth 2.0 Client ID for the user's own Google Cloud project.
    /// The user creates a Desktop-App OAuth credential at console.cloud.google.com
    /// and pastes both id + secret into the Settings dialog. We persist them here.
    /// </summary>
    public string? GoogleClientId { get; set; }

    /// <summary>OAuth 2.0 Client Secret (paired with <see cref="GoogleClientId"/>).</summary>
    public string? GoogleClientSecret { get; set; }

    /// <summary>Primary e-mail of the connected Google account, cached after successful login.</summary>
    public string? GoogleAccountEmail { get; set; }

    public string GroqModel { get; set; } = "whisper-large-v3-turbo";

    public bool AlwaysOnTop { get; set; } = true;

    public double BarHeight { get; set; } = 140;

    /// <summary>
    /// When true (default), the overlay auto-collapses to just the mic button
    /// after use / when the mouse leaves it, and expands on hover. When false
    /// the overlay stays fully visible at all times (legacy behaviour).
    /// </summary>
    public bool AutoHide { get; set; } = true;

    /// <summary>
    /// Overlay orientation: "vertical" (default, top-to-bottom pill) or
    /// "horizontal" (left-to-right bar at the bottom edge). Toggled via the
    /// settings dialog or the in-overlay ⇄ button.
    /// </summary>
    public string Orientation { get; set; } = "vertical";

    /// <summary>
    /// Separator between chained prompts when IsAlwaysOn prompts are
    /// concatenated with the clicked prompt. Default: blank-line, semicolon,
    /// blank-line — chosen so target AIs treat each segment as a separate task.
    /// </summary>
    public string SeparatorTemplate { get; set; } = "\n\n;\n\n";

    /// <summary>
    /// When true, the overlay position saved via the in-overlay diskette button
    /// survives an app/PC restart (the last saved spot becomes the default).
    /// When false (default), restarts fall back to the canonical default
    /// positions and the saved spots only live for the running session.
    /// Toggled by a checkbox in the settings dialog.
    /// </summary>
    public bool PersistOverlayPosition { get; set; } = false;

    /// <summary>
    /// Last saved overlay position per orientation (expanded-view Left/Top in
    /// WPF DIPs). Null = nothing saved for that orientation. Only applied at
    /// startup when <see cref="PersistOverlayPosition"/> is true; otherwise the
    /// canonical default position is used. Written by the overlay's diskette
    /// button regardless of the flag so the last spot is remembered if the user
    /// enables persistence later.
    /// </summary>
    public double? OverlayVerticalLeft { get; set; }

    public double? OverlayVerticalTop { get; set; }

    public double? OverlayHorizontalLeft { get; set; }

    public double? OverlayHorizontalTop { get; set; }
}
