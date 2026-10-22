using System;
using System.Collections.Generic;
using System.Threading;

namespace TerminalVoiceOverlay.Services;

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
/// be lock-free / cheap. Each map is an immutable snapshot: a writer builds a
/// complete new Dictionary and publishes it with one atomic reference swap
/// (Volatile.Write), so a reader never sees a half-cleared/half-filled map.
/// </summary>
public static class HotkeyRegistry
{
    /// <summary>
    /// Entry value: the prompt id (so we can find the source row again
    /// when conflicts arise) plus the EffectiveText that should be
    /// pasted into the terminal when the hotkey fires.
    /// </summary>
    public readonly record struct Entry(Guid PromptId, string EffectiveText);

    // Never mutated after publication; replaced as a whole. Not marked
    // 'volatile' because the fields are passed by ref to Volatile.Read/Write.
    private static Dictionary<int, Entry> _numberMap = new();
    private static Dictionary<char, Entry> _letterMap = new();

    /// <summary>
    /// Replaces the whole number map (Ctrl+1..9) atomically. Called by
    /// PromptBoardPanel after every render so the registry always matches
    /// the on-disk state. Pass an empty list to clear (e.g. before a
    /// destructive restore).
    /// </summary>
    public static void Replace(IEnumerable<KeyValuePair<int, Entry>> entries)
    {
        var map = new Dictionary<int, Entry>();
        if (entries is not null)
        {
            foreach (var kv in entries)
            {
                if (kv.Key < 1 || kv.Key > 9) continue;
                map[kv.Key] = kv.Value; // indexer: later duplicates win (as before)
            }
        }
        Volatile.Write(ref _numberMap, map);
    }

    /// <summary>
    /// Replaces the whole letter map (Win+Alt+A..Z) atomically. Same
    /// semantics as <see cref="Replace"/> but for the letter hotkeys.
    /// Keys are normalised to upper-case so callers can pass either case.
    /// </summary>
    public static void ReplaceLetters(IEnumerable<KeyValuePair<char, Entry>> entries)
    {
        var map = new Dictionary<char, Entry>();
        if (entries is not null)
        {
            foreach (var kv in entries)
            {
                char upper = char.ToUpperInvariant(kv.Key);
                if (upper < 'A' || upper > 'Z') continue;
                map[upper] = kv.Value; // indexer: later duplicates win (as before)
            }
        }
        Volatile.Write(ref _letterMap, map);
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
        return Volatile.Read(ref _numberMap).TryGetValue(number, out var entry) ? entry : null;
    }

    /// <summary>
    /// Lookup used by the keyboard hook for the letter hotkey branch.
    /// Same null-on-miss semantics as <see cref="Lookup"/>. Case-insensitive.
    /// </summary>
    public static Entry? LookupLetter(char letter)
    {
        char upper = char.ToUpperInvariant(letter);
        if (upper < 'A' || upper > 'Z') return null;
        return Volatile.Read(ref _letterMap).TryGetValue(upper, out var entry) ? entry : null;
    }

    /// <summary>True if any number hotkey is currently bound. The hook can
    /// use this as a fast pre-check to skip its Ctrl+1..9 branch entirely
    /// when nothing is registered.</summary>
    public static bool HasAny => Volatile.Read(ref _numberMap).Count > 0;

    /// <summary>Equivalent to <see cref="HasAny"/> but for the letter map.</summary>
    public static bool HasAnyLetter => Volatile.Read(ref _letterMap).Count > 0;
}
