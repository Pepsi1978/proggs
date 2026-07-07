using System;
using System.Collections.Concurrent;
using System.Collections.Generic;

namespace ClaudeVoiceOverlay.Services;

/// <summary>
/// Thread-safe in-memory maps that the low-level keyboard hook in
/// OverlayWindow consults when the user presses a registered hotkey.
///
/// Two parallel maps coexist:
///   - <b>Number map</b>: Ctrl+1..9 (legacy, single-digit). Keyed by int 1..9.
///   - <b>Letter map</b>: Win+Alt+A..Z. Keyed by upper-case char 'A'..'Z'.
///
/// The PromptBoardPanel is the writer: it rebuilds both maps every time
/// it re-renders the prompt list (after add / edit / delete / backup-restore /
/// hotkey-context-menu change). Initial population happens on first render
/// after the overlay starts. Until then both maps are empty and the hooks
/// stay inert — that's the desired safe default.
///
/// The hook callback runs on a low-priority hook thread, so lookups MUST
/// be lock-free / cheap — we use ConcurrentDictionary.
/// </summary>
public static class HotkeyRegistry
{
    /// <summary>
    /// Entry value: the prompt id (so we can find the source row again
    /// when conflicts arise) plus the EffectiveText that should be
    /// pasted into the terminal when the hotkey fires.
    /// </summary>
    public readonly record struct Entry(Guid PromptId, string EffectiveText);

    private static readonly ConcurrentDictionary<int, Entry> _numberMap = new();
    private static readonly ConcurrentDictionary<char, Entry> _letterMap = new();

    /// <summary>
    /// Replaces the whole number map (Ctrl+1..9) atomically. Called by
    /// PromptBoardPanel after every render so the registry always matches
    /// the on-disk state. Pass an empty list to clear (e.g. before a
    /// destructive restore).
    /// </summary>
    public static void Replace(IEnumerable<KeyValuePair<int, Entry>> entries)
    {
        _numberMap.Clear();
        if (entries is null) return;
        foreach (var kv in entries)
        {
            if (kv.Key < 1 || kv.Key > 9) continue;
            _numberMap[kv.Key] = kv.Value;
        }
    }

    /// <summary>
    /// Replaces the whole letter map (Win+Alt+A..Z) atomically. Same
    /// semantics as <see cref="Replace"/> but for the letter hotkeys.
    /// Keys are normalised to upper-case so callers can pass either case.
    /// </summary>
    public static void ReplaceLetters(IEnumerable<KeyValuePair<char, Entry>> entries)
    {
        _letterMap.Clear();
        if (entries is null) return;
        foreach (var kv in entries)
        {
            char upper = char.ToUpperInvariant(kv.Key);
            if (upper < 'A' || upper > 'Z') continue;
            _letterMap[upper] = kv.Value;
        }
    }

    /// <summary>
    /// Lookup used by the keyboard hook for the digit hotkey branch.
    /// Returns null when no prompt is bound to the requested number — the
    /// hook then lets the keystroke fall through to the foreground window
    /// (e.g. browser tab switching).
    /// </summary>
    public static Entry? Lookup(int number)
    {
        if (number < 1 || number > 9) return null;
        return _numberMap.TryGetValue(number, out var entry) ? entry : null;
    }

    /// <summary>
    /// Lookup used by the keyboard hook for the letter hotkey branch.
    /// Same null-on-miss semantics as <see cref="Lookup"/>. Case-insensitive.
    /// </summary>
    public static Entry? LookupLetter(char letter)
    {
        char upper = char.ToUpperInvariant(letter);
        if (upper < 'A' || upper > 'Z') return null;
        return _letterMap.TryGetValue(upper, out var entry) ? entry : null;
    }

    /// <summary>True if any number hotkey is currently bound. The hook can
    /// use this as a fast pre-check to skip its Ctrl+1..9 branch entirely
    /// when nothing is registered.</summary>
    public static bool HasAny => !_numberMap.IsEmpty;

    /// <summary>Equivalent to <see cref="HasAny"/> but for the letter map.</summary>
    public static bool HasAnyLetter => !_letterMap.IsEmpty;
}
