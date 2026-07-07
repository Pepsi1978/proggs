# Renderer-Capabilities & technische Limits (KRITISCH)

> **Diese Regel ist die Lookup-Tabelle für jeden Format-Wunsch.**
> Wenn der Benutzer eine optische Anpassung wünscht (Farbe, Linien, Symbole,
> Schriftgröße, Animation, Layout) MUSS hier als ALLERERSTES nachgeschlagen
> werden — bevor irgendwelche Versuche unternommen werden.

---

## VERHALTENS-REGEL (PFLICHT vor jedem Format-Versuch)

Bei JEDEM Wunsch des Benutzers der das visuelle Erscheinungsbild betrifft —
**Farbe, Schriftgröße, Animation, Layout, Linien, Boxen, Symbole, Highlights** —
gilt folgender Ablauf:

1. **ZUERST** in der Tabelle unten nachschlagen ob das gewünschte Feature
   im Claude-Code-Renderer überhaupt möglich ist.
2. **WENN unmöglich:** Sofort in Klartext erklären warum nicht und welche
   Alternativen es gibt. KEIN Versuch, KEIN Probieren, KEINE Iteration.
3. **WENN eingeschränkt möglich:** Die Einschränkung klar benennen, dann
   die beste verfügbare Annäherung anbieten.
4. **WENN voll möglich:** Direkt umsetzen.

**Warum diese Regel existiert:** Am 2026-05-22 hatten wir **fünf Iterationen**
am Linien-Stil weil die Renderer-Grenze (Markdown kann keine Farben für Linien)
erst in der dritten Runde klar ausgesprochen wurde. Drei Iterationen verbrannten
Zeit und Tokens für Versuche die technisch nie funktionieren konnten.

---

## SCHNELL-LOOKUP (häufigste Wünsche)

| Wunsch | Im Claude-Code-Antworttext? | Beste Alternative |
|--------|----------------------------|-------------------|
| Farbige horizontale Linie | ❌ unmöglich | Farb-Emoji-Punkte (🟠/🟢/🟡) als Endmarker ODER reine `━`-Linie ohne Farbe |
| Farbiger Text (orange, grün, …) | ❌ unmöglich | Emoji-Symbole (🟠🟢🟡✅⚠️❗) für Farb-Information |
| Verschiedene Schriftgrößen | ❌ unmöglich | Bold (`**text**`) für Hervorhebung — keine echte Größenänderung |
| Unterstrichener Text | ❌ unmöglich | Bold oder Code-Inline `` `text` `` |
| Animationen, GIFs, Videos | ❌ unmöglich | Statische Beschreibung in Worten |
| Inline-Bilder | ❌ nicht implementiert | Links zu Bildern — werden als `[Image #N]` angezeigt |
| HTML-Tags `<span style=…>` | ❌ als Raw-Text angezeigt | Markdown-Features stattdessen |
| Inline-CSS | ❌ HTML ist geblockt | — |
| ANSI-Codes im Antworttext | ❌ als Raw-Text angezeigt | Nur in Bash-Tool-Output funktionieren ANSI-Codes |
| Verschachtelte Blockquotes | ⚠️ visuell identisch | Einfache Blockquote oder Nummerierung verwenden |
| Headers unterschiedlicher Größe (`#`, `##`, `###`) | ⚠️ ALLE Headers sehen gleich aus | Nur Bold und Listen-Hierarchie nutzen für Struktur |
| Strikethrough `~~text~~` | ❌ wird als `~~text~~` angezeigt | — |
| Task-Listen `- [x]` | ⚠️ Checkbox geht verloren | Bullet mit ✅/⬜ Symbol verwenden |
| Bold `**text**` | ✅ funktioniert | — |
| Italic `*text*` | ✅ funktioniert | — |
| Inline-Code `` `code` `` | ✅ funktioniert | — |
| Fenced Code-Blocks mit Highlighting | ✅ funktioniert (57 Sprachen) | — |
| Tabellen | ✅ funktioniert (ASCII-Grid) | — |
| Bullet-Listen + nummerierte Listen | ✅ funktioniert | — |
| Klickbare Links `[text](url)` | ✅ funktioniert (OSC-8) | Nur http:// und https:// |
| Emoji (farbig) | ✅ funktioniert (terminal-abhängig) | Im Windows Terminal ab 1.19, macOS immer |
| Unicode-Linien `━━━`, `═══`, `───` | ✅ funktioniert | Aber NICHT einfärbbar |
| Box-Drawing `┌┐└┘╔╗╚╝` | ✅ funktioniert | Standard-Fonts können das |

---

## Claude Code Markdown-Renderer (was beim Antworten geht)

Das Claude Code CLI hat seit Ende 2024 / Anfang 2025 einen **eigenen
Terminal-Markdown-Renderer** der Markdown aktiv darstellt. Es ist kein
„raw text" mehr, aber nur eine **Teilmenge von GFM** wird gerendert.

### ✅ Funktioniert zuverlässig

| Feature | Beispiel | Anmerkung |
|---------|----------|-----------|
| Bold | `**fett**` | Via Terminal-Attribut |
| Italic | `*kursiv*` | Via Terminal-Attribut |
| Bold-Italic | `***beides***` | Kombiniert |
| Inline-Code | `` `code` `` | Hervorgehoben |
| Fenced Code-Block | ```` ```python ```` | Mit Shiki-Highlighting für 57 Sprachen |
| Diff-Blocks | ```` ```diff ```` | Farbige +/- Zeilen |
| Tabellen | `\| a \| b \|` | ASCII-Grid mit Rahmen |
| Bullet-Listen | `- punkt` | `-`, `*`, `+` |
| Nummerierte Listen | `1. punkt` | Auto-Nummerierung |
| Einfache Blockquote | `> text` | Linker senkrechter Balken |
| Links | `[text](https://…)` | OSC-8 Hyperlinks |

### ❌ Funktioniert NICHT

| Feature | Verhalten |
|---------|-----------|
| Header-Hierarchie (`#`, `##`, `###`) | ALLE Headers sehen identisch fett aus, keine Größen-Unterscheidung |
| Strikethrough `~~text~~` | Wird als Literal `~~text~~` angezeigt |
| Task-Listen `- [x]` / `- [ ]` | Nur Plain-Bullet, Checkbox geht verloren |
| Verschachtelte Blockquotes `>>` | Alle Ebenen identisch |
| HTML-Entities (`&amp;`, `&copy;`) | Als Raw-Text angezeigt, nicht dekodiert |
| HTML-Tags (`<span>`, `<br>`, `<hr>`) | Aus Security-Gründen geblockt → Raw-Text |
| Inline-CSS | HTML blockiert → nicht möglich |
| Bilder `![alt](url)` | Nicht inline angezeigt — als `[Image #N]` (klickbarer Link) |
| Horizontal Rule `---` | Status unklar — wahrscheinlich nicht gerendert |
| ANSI-Codes im Markdown-Text | Als Raw-Text angezeigt, NICHT als Steuerzeichen |

### Bekannte Bugs / Edge Cases

- ANSI-Codes „leaken" manchmal in den Eingabe-Text nach Code-Blöcken mit JSON
- Auf Windows Terminal: Syntax-Highlighting kann Tokens an falschen Byte-Grenzen
  teilen (UTF-8 vs. Character-Indices), besonders mit Multi-Byte-Zeichen
- Bash-Code-Blocks auf Windows: chaotische Regenbogen-Farben möglich
- Tabellen mit Markdown-Links: zeigen die volle URL statt dem Label-Text
- Emoji in Tabellen (VS Code Extension): manchmal Lücken oder fehlend

### Syntax-Highlighting im Code-Block (57 Sprachen)

Web: `javascript`, `typescript`, `html`, `css`, `scss`, `vue`, `jsx`, `tsx`
Backend: `python`, `java`, `kotlin`, `go`, `rust`, `cpp`, `c`, `csharp`, `php`, `ruby`, `swift`
Scripting: `bash`, `shell`, `dockerfile`, `powershell`
Daten: `json`, `xml`, `yaml`, `sql`, `markdown`
Bei unbekannter Sprache: Fallback auf `text` (kein Highlighting).

---

## Windows PowerShell 7.6 + Windows Terminal

Im Bash-Tool-Output (NICHT im Markdown-Antworttext) und in PowerShell-Skripten
direkt funktionieren diese Features. Relevant wenn der Benutzer ein Skript
gewünscht oder Tool-Output sehen will.

### Farben (ANSI)

| Ebene | Format | Beispiel |
|-------|--------|----------|
| 16 Farben | `` `e[31m...`e[0m `` | `` `e[31mFEHLER`e[0m `` → rote Schrift |
| 256 Farben | `` `e[38;5;NNm `` | `` `e[38;5;220mGoldgelb`e[0m `` |
| 24-bit Truecolor | `$PSStyle.Foreground.FromRgb(R,G,B)` | `$PSStyle.Background.FromRgb(0x1e90ff)` |

Aktivierung: Keine besondere Aktivierung nötig. Windows Terminal ist xterm-kompatibel.
Deaktivieren via `$Env:NO_COLOR=1` oder `$Env:TERM=dumb`.

### Box-Drawing (alle ✅ mit Cascadia Code)

`─ ━ │ ┃ ═ ║` `┌ ┐ └ ┘ ╭ ╮ ╰ ╯` `┏ ┓ ┗ ┛` `╔ ╗ ╚ ╝` `├ ┤ ┬ ┴ ┼`
`█ ▓ ▒ ░ ▄ ▀` — alle funktionieren in Standard-Cascadia-Mono.

### Spinner (Braille-basiert)

```powershell
$spinner = '⠋','⠙','⠹','⠸','⠼','⠴','⠦','⠧','⠇','⠏'
```

### OSC-8 Hyperlinks

Funktionieren seit Windows Terminal 1.4 (2020), stabil:
```powershell
"`e]8;;$Uri`e\$Label`e]8;;`e\"
```

### Was NICHT geht in Windows Terminal

| Feature | Status |
|---------|--------|
| Inline-Bilder | ❌ (kein iTerm2-Protokoll, GitHub Issue #11104) |
| HTML/CSS Rendering | ❌ (text-basiert) |
| Animierte GIFs | ❌ |
| Sixel-Grafiken | ⚠️ Experimentell, nicht stabil |
| Truecolor in alter `conhost.exe` | ⚠️ Inkonsistent (Windows Terminal selbst: zuverlässig) |

---

## macOS Terminal.app + iTerm2

| Feature | Terminal.app | iTerm2 |
|---------|--------------|--------|
| 16 ANSI-Farben | ✅ | ✅ |
| 256 Farben | ✅ | ✅ |
| 24-bit Truecolor | ❌ (fällt still auf 256 zurück!) | ✅ (`COLORTERM=truecolor`) |
| Box-Drawing `─ │ ┌ ┐ └ ┘ ━ ┃ ═ ║` | ✅ | ✅ |
| Block-Zeichen `█ ▓ ▒ ░` | ✅ | ✅ |
| Pfeile `→ ← ↑ ↓ ⇒ ⇐` | ✅ | ✅ |
| Emoji (farbig) | ✅ | ✅ |
| Nerd-Font-Symbole | ✅ wenn Font installiert | ✅ + Built-in Powerline-Renderer |
| OSC-8 Hyperlinks | ✅ ab macOS 13 (Ventura) | ✅ seit 2017 |
| Inline-Bilder (OSC 1337) | ❌ | ✅ (`imgcat`) |
| Tab-Färbung (OSC 6) | ❌ | ✅ |
| Badge-Text | ❌ | ✅ |
| System-Notification (OSC 9) | ❌ | ✅ |
| Dock-Progress-Bar | ❌ | ✅ (OSC 9;4) |
| tmux-Integration | ❌ | ✅ |
| Sixel-Grafiken | ❌ | ❌ (auch iTerm2 nicht!) |

### Truecolor-Falle Terminal.app

**Wichtig:** Terminal.app ignoriert Truecolor-Sequenzen nicht mit Fehler — es fällt
still auf 256-Farben zurück, was zu falschen Farben führt. Vor Truecolor immer prüfen:
```bash
[[ "$COLORTERM" == "truecolor" ]] && echo "iTerm2-OK" || echo "Terminal.app-Fallback"
```

### iTerm2-exklusive Escape-Codes

```bash
# Inline-Bild
printf '\e]1337;File=inline=1:'; base64 < bild.png; printf '\a\n'
# Tab-Farbe
printf '\e]6;1;bg;red;brightness;100\a'
# Notification
printf '\e]9;Build fertig!\a'
# Cursor-Form
printf '\e]1337;CursorShape=1\a'
```

---

## Technische Limits (Zeichen-Anzahl, Performance, Encoding)

### Trennlinien-Länge

| Format | Max sinnvolle Länge | Begründung |
|--------|---------------------|------------|
| Standard-Terminal-Breite | ~80-120 Zeichen | Default-Fenster-Breite |
| Empfehlung für `━`-Linien | **80 Zeichen** | Passt in fast jedes Terminal ohne Umbruch |
| Maximum bevor sicher umgebrochen wird | ~120 Zeichen | Bei breitem Fenster noch in einer Zeile |
| Mehr als 200 Zeichen | ❌ Wird umgebrochen | Wirkt unsauber, geht über mehrere Zeilen |

### Emoji-Reihen

| Format | Zeichen-Limit | Anmerkung |
|--------|---------------|-----------|
| Emoji-Punkte hintereinander (🟠🟢🔵 etc.) | ~40-50 Emoji pro Zeile | Emoji sind 2 char breit, ~40 Emoji = ~80 char visuell |
| Mehr als 50 Emoji | ⚠️ Umbruch im Terminal | Performance kann auf älteren Geräten leiden |

### Markdown-Tabellen

| Aspekt | Limit |
|--------|-------|
| Spaltenbreite | Renderer wraps automatisch, aber wird unleserlich bei >30 Zeichen pro Zelle |
| Spaltenzahl | Praktisches Limit ~6-8 Spalten in Standard-Terminal |
| Zeilenzahl | Keine harte Grenze, aber >50 Zeilen unübersichtlich |

### Antwort-Länge

| Aspekt | Limit |
|--------|-------|
| Claude's max Antwort | Hängt vom Modell ab (typisch 8K-64K Tokens, ~30-200 KB Text) |
| Sinnvolle Anzeige | <500 Zeilen pro Antwort, sonst überfordert den Benutzer |

### Encoding-Fallen

| Problem | Lösung |
|---------|--------|
| Windows: cp1252 statt UTF-8 → kaputte Umlaute | `[Console]::OutputEncoding=[Text.Encoding]::UTF8` |
| Python schreibt JSON mit Emoji → UnicodeEncodeError | `open(path,'w',encoding='utf-8')` immer setzen |
| Git auto.crlf vs LF in Skripten | `.gitattributes` mit `* text=auto eol=lf` |

### Code-Block-Limits

| Aspekt | Limit |
|--------|-------|
| Sprachen für Highlighting | 57 vorgeladen (siehe Liste oben) |
| Unbekannte Sprache | Fallback auf `text` ohne Highlighting |
| Lange Code-Zeilen | Terminal wrappt — sehr lange Zeilen ungünstig |

---

## Was bedeutet das in der Praxis (Mappping Wunsch → Möglichkeit)

| Benutzer-Wunsch (Beispiel) | Antwort die ich GEBEN MUSS |
|----------------------------|----------------------------|
| „Mach die Linie orange" | „Markdown rendert keine Farben für Linien. Ich kann nur farbige Emoji-Punkte (🟠) als Endmarker setzen oder die Linie farblos lassen." |
| „Mach den Text größer" | „Es gibt keine echten Schriftgrößen im Terminal. Ich kann nur **Bold** für Hervorhebung verwenden." |
| „Mach die Box animiert" | „Animationen sind im Markdown-Renderer nicht möglich. Statische Box ist das Maximum." |
| „Roter Text bitte" | „Roter Text geht im Antworttext nicht — ich kann ❗ oder 🔴-Emoji verwenden für 'Achtung/Fehler'." |
| „Mach ein Bild rein" | „Inline-Bilder werden nicht angezeigt, nur als `[Image #N]` mit klickbarem Link zum Öffnen." |
| „Underlined heading" | „Unterstrichener Text geht nicht. Ich kann **bold** oder ein Symbol verwenden." |
| „Ein Spinner während des Builds" | „Im Markdown-Antworttext nicht möglich — nur in Bash-Tool-Output während aktivem Befehl." |
| „Farbiger Hintergrund" | „Nicht möglich im Antworttext. Code-Block hat einen leichten Grau-Hintergrund — das ist die einzige Hintergrund-Option." |

---

## Was IMMER zuerst sagen (Klartext-Sätze)

Wenn der Benutzer einen Format-Wunsch hat, beginne die Antwort mit EINEM dieser Sätze:

- „Markdown rendert keine Farben für [Linien/Text/Hintergrund] — die Optionen sind: …"
- „Echte Schriftgrößen sind im Terminal nicht möglich — Bold ist das Maximum an Hervorhebung."
- „Animationen funktionieren im Markdown-Antworttext nicht — wir können stattdessen [statische Lösung] verwenden."
- „Im CLI gibt es keine Bilder — aber ich kann einen klickbaren Link auf das Bild setzen."
- „[Feature] ist im Claude-Code-Renderer nicht implementiert, [Alternative] kommt am nächsten ran."

---

## Was NIEMALS passieren darf

- ❌ Einen Format-Versuch starten ohne vorher in dieser Tabelle nachzuschlagen
- ❌ Mehr als EINE Iteration für ein Feature versuchen das technisch unmöglich ist
- ❌ Dem Benutzer suggerieren ein unmögliches Feature würde „mit einem Trick gehen"
- ❌ HTML-Tags wie `<span style="color:...">` einbauen — sie werden als Raw-Text angezeigt
- ❌ ANSI-Codes in den Markdown-Text setzen — sie werden als Raw-Text angezeigt
- ❌ Versprechen dass `---` als Horizontal Rule sichtbar erscheint ohne Verifizierung

---

## Zusammenspiel mit anderen Regeln

| Regel | Zusammenspiel |
|-------|--------------|
| `task-completion-summary` | Linien-Stil und Farb-Endmarker werden hier definiert — diese Regel ist die Quelle für die Trennlinien |
| `german-skill-triggers` | Wenn Benutzer eine optische Frage stellt → diese Datei zuerst lesen |
| Direktive #1 Superintelligenz | Wissen über Renderer-Grenzen IST Superintelligenz — verhindert Token-Verschwendung |

---

## Quellen (Stand 2026-05-22)

### Windows / PowerShell

- about_ANSI_Terminals — PowerShell 7.5/7.6 Doku: `learn.microsoft.com/en-us/powershell/module/microsoft.powershell.core/about/about_ansi_terminals`
- PowerShell Clickable Hyperlinks (OSC-8): `lucyllewy.com/powershell-clickable-hyperlinks/`
- Windows Terminal Issue #11104 (keine Inline-Bilder): `github.com/microsoft/terminal/issues/11104`
- Windows Terminal Issue #7246 (kein HTML): `github.com/microsoft/terminal/issues/7246`
- Cascadia Code: `github.com/microsoft/cascadia-code`
- Spectre.Console für PowerShell: `pwshspectreconsole.com`

### macOS / iTerm2

- iTerm2 Proprietary Escape Codes: `iterm2.com/documentation-escape-codes.html`
- iTerm2 Inline Images: `iterm2.com/documentation-images.html`
- OSC-8 Hyperlinks: `gist.github.com/egmontkob/eb114294efbcd5adb1944c9f3cb5feda`
- Truecolor in macOS: `medium.com/@skeough117/the-mac-default-terminal-lacks-true-color-capabilities`
- Nerd Fonts: `nerdfonts.com`

### Claude Code

- GitHub Issue #26390 (Renderer destroys ~40% of GFM): `github.com/anthropics/claude-code/issues/26390`
- GitHub Issue #13600 (Markdown renderer request): `github.com/anthropics/claude-code/issues/13600`
- GitHub Issue #5679 (ANSI nicht gerendert): `github.com/anthropics/claude-code/issues/5679`
- GitHub Issue #22406 (Highlighting auf Windows): `github.com/anthropics/claude-code/issues/22406`
- GitHub Issue #37808 (Links in Tabellen): `github.com/anthropics/claude-code/issues/37808`
- GitHub Issue #42519 (Custom URL-Schemas): `github.com/anthropics/claude-code/issues/42519`
- GitHub Issue #29254 (Inline-Bilder): `github.com/anthropics/claude-code/issues/29254`
- Claude Code Docs Terminal Config: `code.claude.com/docs/en/terminal-config`
