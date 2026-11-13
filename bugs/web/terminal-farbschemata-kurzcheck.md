# Terminal-Farbschemata (xterm.js, CLI-Themes) — Bug-Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Volltext: `terminal-farbschemata.md`. Stand 13.11.2026, xterm.js 5.5.0 / 6.x-Beta, Claude Code 2.1.2xx.

## ⚡ Kurzcheck (Stufe A)
1. **Breite Terminalschrift auf Android** → xterm-Default „courier“ = Cutive Mono. Fix: eigene `fontFamily`-Liste mit `monospace` am Ende.
2. **Theme wechselt nicht** → dieselbe Objektreferenz zugewiesen. Fix: `term.options.theme = { ...t }`.
3. **Auswahl auf Hell unsichtbar** → Default-Selection Weiß 30 %. Fix: `selectionBackground` setzen.
4. **Schwarzer Text unsichtbar** → Palette setzt ANSI black = bg (Gruvbox, One Half Dark, Monokai Pro). Fix: black ersetzen.
5. **Speicher/Canvas-Flut beim Theme-Wechsel (WebGL, xterm 6.1-Beta)** (#6291). Fix: Theme nur bei echtem Wechsel zuweisen.
6. **Claude Code `auto` schaltet nach Hell/Dunkel-Wechsel nicht um** (#92389) → Neuabfrage nur bei `CSI ? 997 ; Ps n`.
7. **Claude Code `light-ansi`: Code weiß auf hell, Dim-Text unlesbar** (#40905, #49839) → Slot 7/8 mehrfach belegt; RGB-Preset `light` nehmen oder brightBlack ≥ 4,5:1.
8. **Seite blitzt im falschen Modus auf** → Thema erst im deferred Skript gesetzt. Fix: synchrones Skript im `<head>`.
9. **Claude-Orange wird zu Lachs** in 256-Farben-Terminals (#39369) → `COLORTERM=truecolor` sicherstellen.
