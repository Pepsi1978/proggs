# Terminal-Farbschemata für Web-Konsolen (xterm.js) — Volltext

Stand: 13.11.2026 · Versions-Anker: xterm.js 5.5.0, OpenCode v2.3.0 (anomalyco/opencode, Branch dev), Claude Code 2.1.2xx ·
Recherche: Sonnet-Schwarm (5 Researcher) · Umgesetzt in `OpenLauncherServer/web/static/designs.js` (20 Designs, 0.1.56).

## 1. xterm.js-Theme richtig setzen `offiziell`
- `ITheme`: background, foreground, cursor, cursorAccent, selectionBackground, selectionInactiveBackground, selectionForeground, 16 ANSI (black … brightWhite), extendedAnsi, scrollbarSlider*.
- Zur Laufzeit: `term.options.theme = { ...theme }` (neues Objekt). Nicht bei jedem Render zuweisen: Jede Zuweisung leert die Kontrast-Caches, im WebGL-Renderer wird der Glyph-Atlas neu gebaut (xterm.js #6291, 6.x-Beta mit Speicherleck).
- Fehlende Felder fallen auf die xterm-Defaults zurück (dunkle Tango-Palette). Für helle Themes deshalb ALLE 16 Farben setzen.
- `minimumContrastRatio` (Default 1): 4,5 = AA, 7 = AAA. Passt nur den Vordergrund an, auch bei Truecolor-Ausgaben von CLIs; gecacht. Auf Hell immer 4,5 setzen.
- `selectionBackground`: Default Weiß 30 % Alpha → auf Hell unsichtbar. Explizit setzen (z. B. Akzent mit Alpha `4d`). WebGL zeichnet die Auswahl bewusst deckend (Discussion #4432).
- Quellen: https://xtermjs.org/docs/api/terminal/interfaces/itheme/ · https://xtermjs.org/docs/api/terminal/interfaces/iterminaloptions/

## 2. Schrift `extern`
- xterm-Default `fontFamily` ist „courier-new, courier, monospace“. Android löst „courier“ auf Cutive Mono auf, eine sehr breite Schreibmaschinenschrift → kaum Text pro Zeile. Eigene Liste setzen: `"Roboto Mono","Noto Sans Mono","Droid Sans Mono","DejaVu Sans Mono","Cascadia Mono",Menlo,Consolas,monospace`.

## 3. Bewährte Paletten (Terminal-bg, fg, Akzent) `extern`
Quelle Terminalwerte: https://github.com/mbadolato/iTerm2-Color-Schemes (ghostty/) · UI-Flächen aus den offiziellen Repos (catppuccin/palette v1.8.0, folke/tokyonight.nvim, rose-pine/palette, sainnhe/everforest, morhetz/gruvbox, stephango.com/flexoki, dracula-theme).

Hell (gedämpft, ohne reines Weiß), Kontrast fg/bg:
| Theme | bg | fg | Kontrast | Akzent |
|---|---|---|---|---|
| Orange & Grau (eigener Entwurf, r4 §4.4) | #e8e6e3 | #1f1e1d | 13,4 | #b83e0b |
| Claude Light | #faf9f5 | #141413 | hoch | #a8461f (Text), Marke #d97757 nur Fläche |
| Flexoki Light | #fffcf0 (bg-2 #f2f0e5) | #100f0f | 18,6 | #bc5215 |
| Gruvbox Light | #fbf1c7 (soft #f2e5bc) | #3c3836 | 10,2 | #af3a03 |
| Solarized Light | #fdf6e3 | #657b83 → besser #586e75 | 4,13 (Original) | #cb4b16 |
| Catppuccin Latte | #eff1f5 | #4c4f69 | 7,06 | #8839ef |
| Rosé Pine Dawn | #faf4ed | #575279 (offiziell neu #464261) | 6,66 | #286983 |
| Everforest Light | #efebd4 (bg_dim) | #5c6a72 | 4,66 | #35a77c |
| Tokyo Night Day | #e1e2e7 | #3760bf | 4,52 | #2e7de9 |
| One Light | #f9f9f9 | #2a2c33 | 13,2 | #4078f2 |

Dunkel (nicht pechschwarz zuerst):
| Theme | bg | fg | Akzent |
|---|---|---|---|
| Orange & Grau | #2b2b2b | #e8e6e3 | #e8845f |
| Monokai Pro Spectrum | #222222 | #f7f1ff | #fd9353 |
| Gruvbox Dark | #282828 | #ebdbb2 | #fe8019 |
| GitHub Dark Dimmed | #22272e | #adbac7 | #539bf5 |
| Nord (seit 2023 ungepflegt) | #2e3440 | #d8dee9 | #88c0d0 |
| Catppuccin Mocha | #1e1e2e | #cdd6f4 | #cba6f7 |
| Tokyo Night Storm | #24283b | #c0caf5 | #7aa2f7 |
| Dracula | #282a36 | #f8f8f2 | #bd93f9 |
| One Dark | #282c34 | #abb2bf | #61afef |

Beliebtheit (VS Code Installs, 13.11.2026): GitHub 20,6 Mio, One Dark Pro 12,9 Mio, Dracula 11,1 Mio, Tokyo Night 3,0 Mio, Catppuccin 1,5 Mio (Trend: Catppuccin vorn). Neovim (dotfyle): Catppuccin, Tokyo Night.

## 4. Kontrastregeln `offiziell`
- WCAG 2.2: Text 4,5:1, großer Text/UI-Komponenten/Fokus 3:1. Gegen die TATSÄCHLICHE Fläche messen (bg UND Panel), nicht runden. https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html
- Auf Hell sind ANSI 3/6/7/11/14/15 (gelb, cyan, weiß) fast immer < 3:1 → nur als Fläche, sonst `minimumContrastRatio`.
- `brightBlack` ist der Dim-Slot (Claude Code zeichnet Sekundärtext damit) → ≥ 4,5:1 wählen.
- Orange: Hell #b83e0b auf #e8e6e3 = 4,52; #c2410c auf #e8e6e3 nur 4,16. Dunkel #e8845f auf #2b2b2b = 5,32; Marke #d97757 auf #333333 nur 4,05. Weiß auf #d97757 = 3,12 → dunkle Schrift #141413 (5,90).
- Text- und Füll-Orange pro Modus als eigene Tokens (Grafana, Warp, Flexoki machen es so).

## 5. CLIs und Hell/Dunkel `extern`
- **OpenCode:** Theme-JSON `{defs, theme:{key:{dark,light}}}`, `tui.json` → `theme.mode` = `system|dark|light`; bei `system` Luminanz des Terminal-Hintergrunds > 0,5 = hell, folgt live. Theme `system` übernimmt die Terminalpalette. https://opencode.ai/docs/themes/
- **Claude Code:** `/config` → Theme. `dark`/`light` = festes RGB (Orange rgb(215,119,87)), `dark-ansi`/`light-ansi` = Terminalpalette (Slot 7/8-Fallen, #40905, #49839). `auto` fragt OSC 11 ab, neu nur bei `CSI ? 997 ; Ps n` (#92389). https://code.claude.com/docs/en/terminal-config
- **Codex CLI:** nur Syntax-Theme `tui.theme` (Drittquellen).
- xterm.js im Browser beantwortet OSC 10/11/12/4 aus `options.theme`. `COLORFGBG` beim PTY-Start setzen (`0;15` hell, `15;0` dunkel).

## 6. Muster aus der Umsetzung (OpenLauncherServer 0.1.56)
- Designs als Daten (`designs.js`: id, name, art, ui-Tokens, term-ITheme), synchron im `<head>` geladen; ein Inline-Skript setzt CSS-Variablen + `data-thema` vor dem ersten Zeichnen.
- Modus `auto|hell|dunkel` + je ein gewähltes helles und dunkles Design in `localStorage`; Auto nach Sonnenauf-/untergang Berlin (NOAA-Gleichung, Berliner Kalendertag per `Intl.DateTimeFormat`).
- Vorschau je Design als kleines Farb-Terminal, weil das gerade nicht aktive Design sonst unsichtbar ist.
