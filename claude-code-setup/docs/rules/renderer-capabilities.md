# Renderer-Capabilities & technische Limits (KRITISCH)

> **Lookup-Tabelle fuer jeden Format-Wunsch.** Wuenscht der Benutzer etwas Optisches (Farbe, Linien,
> Symbole, Schriftgroesse, Animation, Layout), MUSS hier ZUERST nachgeschlagen werden — bevor
> irgendein Versuch startet.

## Verhaltens-Regel (PFLICHT vor jedem Format-Versuch)

1. **ZUERST** in der Tabelle nachschlagen, ob das Feature im Claude-Code-Renderer geht.
2. **Unmoeglich** → sofort in Klartext erklaeren warum + Alternative nennen. KEIN Versuch, KEINE Iteration.
3. **Eingeschraenkt** → Grenze benennen, beste Annaeherung anbieten.
4. **Voll moeglich** → direkt umsetzen.

Warum: Am 2026-05-22 verbrannten 5 Iterationen am Linien-Stil Zeit/Tokens, weil die Renderer-Grenze
(Markdown kann Linien nicht einfaerben) erst spaet ausgesprochen wurde.

## Schnell-Lookup (haeufigste Wuensche im Antworttext)

| Wunsch | Im Antworttext? | Beste Alternative |
|--------|-----------------|-------------------|
| Farbige horizontale Linie | ❌ | Farb-Emoji-Punkte (🟠🟢🟡) als Endmarker ODER reine `━`-Linie ohne Farbe |
| Farbiger Text (rot, gruen, …) | ❌ | Emoji-Symbole (🟠🟢🟡✅⚠️❗) tragen die Farb-Info |
| Farbiger Hintergrund | ❌ | nur der leichte Grau-Hintergrund von Code-Bloecken |
| Verschiedene Schriftgroessen | ❌ | `**Bold**` zur Hervorhebung — keine echte Groesse |
| Unterstrichener Text | ❌ | Bold oder Inline-Code `` `text` `` |
| Animationen, GIFs, Videos | ❌ | statische Beschreibung in Worten |
| Inline-Bilder `![]()` | ❌ | erscheinen als `[Image #N]` (klickbarer Link) |
| HTML `<span style=…>` / Inline-CSS / ANSI im Antworttext | ❌ (Raw-Text) | Markdown-Features; ANSI nur in Bash-Output/Skripten |
| Header-Hierarchie `#`/`##`/`###` | ⚠️ alle gleich fett | Struktur ueber Bold + Listen |
| Strikethrough `~~text~~` | ❌ (Literal) | — |
| Task-Listen `- [x]` | ⚠️ Checkbox weg | Bullet mit ✅/⬜ |
| Verschachtelte Blockquotes `>>` | ⚠️ alle Ebenen gleich | einfache Blockquote/Nummerierung |
| Horizontal Rule `---` | ⚠️ meist nicht gerendert | `━`-Unicode-Linie |
| Bold / Italic / Inline-Code | ✅ | — |
| Fenced Code-Block (Highlighting 57 Sprachen, Diff) | ✅ | unbekannte Sprache → Fallback `text` |
| Tabellen (ASCII-Grid) | ✅ | Links zeigen ggf. volle URL statt Label |
| Bullet-/nummerierte Listen | ✅ | — |
| Klickbare Links `[text](url)` | ✅ (OSC-8) | nur http(s):// |
| Emoji (farbig) | ✅ | Win Terminal ab 1.19, macOS immer |
| Unicode-Linien `━ ═ ─`, Box-Drawing `┌┐└┘╔╗` | ✅ | NICHT einfaerbbar |

## Terminal-Output vs. Antworttext (wichtige Grenze)

Im **Bash-Tool-Output und in Skripten** funktionieren ANSI-Farben (16/256/Truecolor), Box-Drawing,
Braille-Spinner, OSC-8-Hyperlinks — **im Markdown-Antworttext NICHT**. Fallen: Terminal.app faellt bei
Truecolor still auf 256 Farben zurueck; Inline-Bilder nur in iTerm2 (OSC 1337), nicht Windows
Terminal/Terminal.app; Sixel nirgends stabil.

## Technische Limits (Kern)

- **Trennlinien:** `━` auf **80 Zeichen** halten (>120 bricht um).
- **Tabellen:** ~6-8 Spalten, Zellen <30 Zeichen.
- **Encoding:** Windows Python immer `encoding='utf-8'`; PowerShell `[Console]::OutputEncoding=[Text.Encoding]::UTF8`; Skripte via `.gitattributes` auf LF.

## Klartext-Saetze bei unmoeglichen Wuenschen (Beispiele)

„Markdown faerbt Linien/Text/Hintergrund nicht — die Optionen sind: …" · „Echte Schriftgroessen gibt es
im Terminal nicht — Bold ist das Maximum." · „Animationen sind im Antworttext nicht moeglich —
stattdessen [statische Loesung]." · „Im CLI gibt es keine Inline-Bilder — aber ich kann einen klickbaren
Link setzen." · „Roter Text geht nicht im Text — ich nutze ❗/🔴 fuer 'Achtung'." · „Spinner nur in
Bash-Output, nicht im Antworttext."

## Was NIEMALS passieren darf

- Format-Versuch starten ohne vorheriges Nachschlagen in dieser Tabelle
- Mehr als EINE Iteration an einem technisch unmoeglichen Feature
- Ein unmoegliches Feature als „mit einem Trick machbar" verkaufen
- `<span style="color:…">`-HTML oder ANSI-Codes in den Antworttext setzen (werden Raw-Text)
