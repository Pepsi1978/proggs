# Terminal-Farbschemata (xterm.js, CLI-Themes) — Bug-Almanach Volltext

Stand: 13.11.2026 · Versions-Anker: xterm.js 5.5.0 (OpenLauncherServer), Claude Code 2.1.2xx · Best Practices: `best-practices/web/terminal-farbschemata.md`

## 1. Breite Terminalschrift auf Android/Tablet
- **Symptom:** Wörter im xterm.js-Terminal stark in die Breite gezogen, wenig Text pro Zeile (Jarvis-WebView, Galaxy Tab S9).
- **Ursache:** xterm.js-Default `fontFamily: "courier-new, courier, monospace"`; Android-`fonts.xml` macht „courier“ zum Alias für Cutive Mono (serif-monospace, sehr breit).
- **Versionen:** xterm.js 5.x, Android 14–16 WebView.
- **Fix:** `fontFamily: '"Roboto Mono","Noto Sans Mono","Droid Sans Mono","DejaVu Sans Mono","Cascadia Mono",Menlo,Consolas,monospace'`. Selbst festgestellt 13.11.2026 (OpenLauncherServer 0.1.54).

## 2. Theme-Wechsel greift nicht
- **Symptom:** `term.options.theme` zugewiesen, Farben bleiben.
- **Ursache:** Optionen werden per Referenz verglichen; mutiertes Objekt = keine Änderung.
- **Fix:** jedes Mal neues Objekt (`{ ...theme }`). Quelle: https://xtermjs.org/docs/api/terminal/classes/terminal/

## 3. Auswahl auf hellem Theme unsichtbar
- **Symptom:** Markierter Text nicht zu sehen.
- **Ursache:** Default `selectionBackground` Weiß mit 30 % Alpha; WebGL zeichnet die Auswahl deckend gemischt (by design).
- **Versionen:** xterm.js 5.1–5.5.
- **Fix:** `selectionBackground` (+ `selectionInactiveBackground`) passend zum Theme setzen. https://github.com/xtermjs/xterm.js/discussions/4432

## 4. ANSI-Schwarz gleich Hintergrund
- **Symptom:** Schwarzer Text (ANSI 0) unsichtbar.
- **Ursache:** Paletten One Half Dark (#282c34), Gruvbox Dark (#282828) / Hard (#1d2021), Gruvbox Light (#fbf1c7), Monokai Pro (#2d2a2e) setzen black = bg.
- **Fix:** black auf Fläche (dunkel) bzw. fg (hell) setzen. https://github.com/mbadolato/iTerm2-Color-Schemes

## 5. WebGL: Canvas-Flut/Speicherleck bei Theme-Zuweisung
- **Symptom:** zehntausende Canvases, GB-Speicher, wenn `options.theme` in jedem Render neu gesetzt wird.
- **Ursache:** jedes `onChangeColors` erzeugt einen neuen Glyph-Atlas; Regression durch PR #5929, `dispose()` gibt Backing-Stores nicht frei.
- **Versionen:** @xterm/xterm 6.1.0-beta.336, addon-webgl 0.20.0-beta (nicht 5.5).
- **Fix:** offen; Workaround: Theme nur bei echtem Wechsel zuweisen. https://github.com/xtermjs/xterm.js/issues/6291

## 6. WebGL: schwarze Rechtecke bei Transparenz
- **Symptom:** mit `allowTransparency` deckend schwarze Flächen hinter kursiven/dim-Zellen.
- **Versionen:** @xterm/xterm 6.0.0, addon-webgl 0.19.0. **Fix:** offen (PR #6295); DOM-Renderer oder deckender Hintergrund. https://github.com/xtermjs/xterm.js/issues/6116

## 7. Claude Code `theme: auto` schaltet nach Wechsel nicht um
- **Symptom:** Terminal wechselt hell/dunkel, Claude Code bleibt.
- **Ursache:** Neuabfrage per OSC 11 nur nach `CSI ? 997 ; Ps n` (DEC-Modus 2031); Terminals ohne 2031 senden das nie.
- **Versionen:** Claude Code 2.1.261/2.1.267. **Workaround:** `ESC [ ? 997 ; 2 n` (dunkel) bzw. `; 1 n` (hell) ins PTY einspeisen (nicht selbst getestet). https://github.com/anthropics/claude-code/issues/92389

## 8. Claude Code `auto` wählt das falsche Theme / Syntax bleibt dunkel
- **Symptom:** dunkles Theme auf hellem Terminal (Mosh: Timeout → `dark`); in tmux bleibt der Syntax-Highlighter dunkel (weiß auf weiß).
- **Versionen:** 2.1.132 (tmux 3.5a), 2.1.228, 2.1.259. **Workaround:** `theme` explizit `light`/`dark`; `COLORFGBG` bereitstellen (`set -ag update-environment "COLORFGBG"`). https://github.com/anthropics/claude-code/issues/86048 · https://github.com/anthropics/claude-code/issues/56848

## 9. Claude Code `light-ansi`/`dark-ansi`: unsichtbarer Code, unlesbarer Dim-Text
- **Ursache:** ANSI-Slot 7 gleichzeitig für Code-Token, Balken und Rahmen; Slot 8 (brightBlack) für Dim-Text, in hellen Paletten oft hellgrau.
- **Versionen:** 2.1.87, 2.1.112, 2.1.114. **Fix:** keiner dokumentiert; RGB-Presets nutzen oder Slot 7 auf Mittelgrau, brightBlack ≥ 4,5:1. https://github.com/anthropics/claude-code/issues/40905 · https://github.com/anthropics/claude-code/issues/49839

## 10. Claude-Orange wird zu Lachs
- **Ursache:** Claude Code zeichnet interaktive Elemente mit festem 24-Bit-RGB; in 256-Farben-Terminals wird rgb(215,119,87) auf Index 174 quantisiert.
- **Fix:** Truecolor (`COLORTERM=truecolor`) durchreichen. https://github.com/anthropics/claude-code/issues/39369

## 11. Paletten-Abweichungen zwischen Ports
- Catppuccin: mbadolato, offizielle palette.json und catppuccin/windows-terminal unterscheiden sich (white/brightWhite vertauscht).
- Everforest (mbadolato) nutzt bg_dim (#232a2e dunkel, #efebd4 hell) statt bg0.
- Rosé Pine Dawn: mbadolato fg #575279, offiziell #464261. Solarized Light ANSI 7 dort #bbb5a2, offiziell #eee8d5.
- Für UI-Flächen die offiziellen Paletten nehmen, nicht die Ports.

## 12. Seite blitzt im falschen Modus auf
- **Symptom:** bei Hell kurz dunkle Seite bei jedem Laden.
- **Ursache:** `data-thema` wird erst im `defer`-Skript gesetzt, die CSS-Defaults (dunkel) malen vorher.
- **Fix:** Design-Daten synchron im `<head>` laden und Thema dort setzen. Selbst festgestellt (Code-Review 13.11.2026, OpenLauncherServer 0.1.55).
