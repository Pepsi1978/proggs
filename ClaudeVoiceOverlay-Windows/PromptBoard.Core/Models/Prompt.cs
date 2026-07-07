using System;
using PromptBoard.Core.Enums;

namespace PromptBoard.Core.Models;

/// <summary>
/// A single prompt. Stored together with AiImprovementPrompt in one
/// table using a TPH discriminator.
/// </summary>
public class Prompt : BaseEntity
{
    public Guid CategoryId { get; set; }

    public Category Category { get; set; } = null!;

    /// <summary>
    /// Short label shown on the bar (5–7 words). The spec allows free text
    /// but UI will hint the user to keep it short.
    /// </summary>
    public string ShortLabel { get; set; } = string.Empty;

    /// <summary>
    /// The canonical user-authored or dictated prompt text. Never null and
    /// never overwritten by AI improvement.
    /// </summary>
    public string OriginalText { get; set; } = string.Empty;

    /// <summary>AI-improved version. Null until Gemini has been run at least once.</summary>
    public string? ImprovedText { get; set; }

    /// <summary>Which version gets inserted on click.</summary>
    public PromptVersion ActiveVersion { get; set; } = PromptVersion.Original;

    /// <summary>
    /// If true, this prompt is automatically appended to every insertion
    /// (a "header" that stays pinned to the chain).
    /// </summary>
    public bool IsAlwaysOn { get; set; }

    /// <summary>
    /// When IsAlwaysOn is true, prepend this prompt before the dictated
    /// text. Default true so legacy prompts keep their pre-split semantics.
    /// </summary>
    public bool IsPrePrompt { get; set; } = true;

    /// <summary>
    /// When IsAlwaysOn is true, append this prompt after the dictated text.
    /// Independent of IsPrePrompt — both can be true so the prompt wraps
    /// the dictation on both sides.
    /// </summary>
    public bool IsPostPrompt { get; set; }

    /// <summary>Ordering within the category.</summary>
    public int SortOrder { get; set; }

    /// <summary>
    /// Optional Strg+N hotkey (1-9) that pastes this prompt into the
    /// active terminal. Null when no hotkey is assigned. Globally
    /// unique across all prompts: assigning a number that another
    /// prompt already owns silently transfers it (last-wins).
    /// Local-only state — deliberately not part of the Drive backup
    /// because every machine has its own preferred bindings.
    /// </summary>
    public int? HotkeyNumber { get; set; }

    /// <summary>
    /// Optional Win+Alt+&lt;letter&gt; hotkey (A-Z) that pastes this prompt
    /// into the active terminal. Null when no hotkey is assigned.
    /// Stored as upper-case ASCII char ('A'..'Z'). Same uniqueness rule
    /// as <see cref="HotkeyNumber"/>: assigning a letter that another
    /// prompt already owns silently transfers it (last-wins). Like the
    /// number variant this is local-only and NOT included in the Drive
    /// backup so each machine keeps its own bindings.
    /// </summary>
    public char? HotkeyLetter { get; set; }

    /// <summary>
    /// Which Gemini meta-prompt produced the last ImprovedText. Lets the
    /// user see provenance and re-run against a different meta-prompt.
    /// </summary>
    public Guid? ImprovedByAiPromptId { get; set; }
}
