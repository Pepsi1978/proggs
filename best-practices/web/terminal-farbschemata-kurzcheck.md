# Terminal-Farbschemata (xterm.js, Web-Konsole) Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Bei Fehlern oder vor größerer Arbeit den Volltext
> `terminal-farbschemata.md` lesen. Stand 13.11.2026, xterm.js 5.5.0, Claude Code 2.1.2xx, OpenCode v2.3.0.

## ⚡ Kurzcheck (Stufe A)

1. **Theme-Wechsel:** immer ein neues Objekt zuweisen (`term.options.theme = { ...t }`), nur bei echtem Wechsel (jede Zuweisung leert Caches/Glyph-Atlas). `offiziell`
2. **Helles Theme braucht eigene 16-Farben-Palette:** xterm-Defaults sind dunkel (Tango), Gelb/Weiß/Hellcyan < 3:1 auf Hell. Zusätzlich `minimumContrastRatio: 4.5` (wirkt nur auf Vordergrund, auch Truecolor). `offiziell`
3. **`selectionBackground` immer setzen:** Default ist Weiß mit 30 % Alpha, auf hellem Grund unsichtbar. Bewährt: Akzentfarbe + `4d` (30 %). `offiziell`
4. **ANSI-Schwarz ≠ Hintergrund:** One Half Dark, Gruvbox Dark/Light, Monokai Pro setzen black = bg → unsichtbarer Text. Beim Übernehmen prüfen und ersetzen. `extern`
5. **Gedämpftes Hell statt Weiß:** Gruvbox Light Soft #f2e5bc, Flexoki #f2f0e5/#fffcf0, Catppuccin Latte #eff1f5, Rosé Pine Dawn #faf4ed, Everforest #efebd4, Orange&Grau #e8e6e3. Solarized Light fg #657b83 liegt unter 4,5:1 → #586e75 nehmen. `extern`
6. **Dunkel, aber nicht pechschwarz (tagsüber angenehm):** Nord #2e3440, Catppuccin Frappé #303446, Tokyo Night Storm #24283b, GitHub Dark Dimmed #22272e, Gruvbox #282828. `extern`
7. **Orange-Akzent auf Grau:** Text-Orange und Füll-Orange pro Modus trennen. Hell: #b83e0b auf #e8e6e3 (4,52:1); Dunkel: #e8845f auf #2b2b2b (5,32:1). Keine weiße Schrift auf #d97757 (3,12:1), dunkle Schrift #141413 nehmen. `extern`
8. **Design vor dem ersten Zeichnen setzen** (synchrones Skript im `<head>`), sonst blitzt die Seite im falschen Modus auf.
9. **Claude Code folgt dem Terminal nur mit `dark-ansi`/`light-ansi`** (Slot 7/8 mehrfach belegt, eigene Fallen); `dark`/`light` sind feste RGB. OpenCode: `theme.mode` `system|dark|light`, Luminanz > 0,5 = hell. `offiziell`
10. **OSC 11:** xterm.js im Browser beantwortet OSC 10/11/12/4 aus `options.theme`; `onData` nicht filtern. `COLORFGBG` beim PTY-Start (`0;15` hell, `15;0` dunkel). `extern`
