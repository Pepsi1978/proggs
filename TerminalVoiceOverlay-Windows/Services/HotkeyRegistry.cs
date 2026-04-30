using System;
using System.Collections.Concurrent;
using System.Collections.Generic;

namespace TerminalVoiceOverlay.Services;

/// <summary>
/// Thread-safe in-memory map "Strg+N → Prompt-Text" that the low-level
/// keyboard hook in OverlayWindow consults when the user presses Ctrl+1..9.
/// The hook callback runs on a low-priority hook thread, so the lookup
/// MUST be lock-free / cheap — we use ConcurrentDictionary.
///
/// The PromptBoardPanel is the writer: it rebuilds the registry every
/// time it re-renders the prompt list (after add / edit / delete /
/// backup-restore). Initial population happens on first render after
/// the overlay starts. Until then the registry is empty, hotkeys are
/// inert — that's the desired safe default.
/// </summary>
public static class HotkeyRegistry
{
    /// <summary>
    /// Entry value: the prompt id (so we can find the source row again
    /// when conflicts arise) plus the EffectiveText that should be
    /// pasted into the terminal when the hotkey fires.
    /// </summary>
    public readonly record struct Entry(Guid PromptId, string EffectiveText);

    private static readonly ConcurrentDictionary<int, Entry> _map = new();

    /// <summary>
    /// Replaces the whole map atomically. Called by PromptBoardPanel after
    /// every render so the registry always matches the on-disk state.
    /// Pass an empty list to clear (e.g. before a destructive restore).
    /// </summary>
    public static void Replace(IEnumerable<KeyValuePair<int, Entry>> entries)
    {
        _map.Clear();
        if (entries is null) return;
        foreach (var kv in entries)
        {
            if (kv.Key < 1 || kv.Key > 9) continue;
            _map[kv.Key] = kv.Value;
        }
    }

    /// <summary>
    /// Lookup used by the keyboard hook. Returns null when no prompt is
    /// bound to the requested number — the hook then lets the keystroke
    /// fall through to the foreground window (e.g. browser tab switching).
    /// </summary>
    public static Entry? Lookup(int number)
    {
        if (number < 1 || number > 9) return null;
        return _map.TryGetValue(number, out var entry) ? entry : null;
    }

    /// <summary>True if any hotkey is currently bound. The hook can use
    /// this as a fast pre-check to skip its Strg+1..9 branch entirely
    /// when nothing is registered.</summary>
    public static bool HasAny => !_map.IsEmpty;
}
