# Renderer-Capabilities & technische Limits (KRITISCH)

> **Lookup-Tabelle für jeden Format-Wunsch.** Wünscht der Benutzer etwas Optisches
> (Farbe, Linien, Symbole, Schriftgröße, Animation, Layout), MUSS hier ZUERST
> nachgeschlagen werden — bevor irgendein Versuch gestartet wird.

---

## VERHALTENS-REGEL (PFLICHT vor jedem Format-Versuch)

1. **ZUERST** in der Tabelle unten nachschlagen, ob das Feature im Claude-Code-Renderer geht.
2. **Unmöglich** → sofort in Klartext erklären warum + Alternative nennen. KEIN Versuch, KEINE Iteration.
3. **Eingeschränkt** → Grenze benennen, beste Annäherung anbieten.
4. **Voll möglich** → direkt umsetzen.

**Warum:** Am 2026-05-22 verbrannten 5 Iterationen am Linien-Stil Zeit/Tokens, weil die
Renderer-Grenze (Markdown kann Linien nicht einfärben) erst spät ausgesprochen wurde.

---

## SCHNELL-LOOKUP (häufigste Wünsche im Antworttext)

| Wunsch | Im Antworttext? | Beste Alternative |
|--------|-----------------|-------------------|
| Farbige horizontale Linie | ❌ | Farb-Emoji-Punkte (🟠🟢🟡) als Endmarker ODER reine `━`-Linie ohne Farbe |
| Farbiger Text (rot, grün, …) | ❌ | Emoji-Symbole (🟠🟢🟡✅⚠️❗) tragen die Farb-Info |
| Farbiger Hintergrund | ❌ | nur der leichte Grau-Hintergrund von Code-Blöcken |
| Verschiedene Schriftgrößen | ❌ | `**Bold**` zur Hervorhebung — keine echte Größe |
| Unterstrichener Text | ❌ | Bold oder Inline-Code `` `text` `` |
| Animationen, GIFs, Videos | ❌ | statische Beschreibung in Worten |
| Inline-Bilder `![]()` | ❌ | erscheinen als `[Image #N]` (klickbarer Link) |
| HTML-Tags `<span style=…>` / Inline-CSS | ❌ (Raw-Text) | Markdown-Features nutzen |
| ANSI-Codes im Antworttext | ❌ (Raw-Text) | ANSI geht NUR in Bash-Tool-Output/Skripten |
| Header-Hierarchie `#`/`##`/`###` | ⚠️ alle gleich fett | Struktur über Bold + Listen |
| Strikethrough `~~text~~` | ❌ (Literal) | — |
| Task-Listen `- [x]` | ⚠️ Checkbox weg | Bullet mit ✅/⬜ |
| Verschachtelte Blockquotes `>>` | ⚠️ alle Ebenen gleich | einfache Blockquote/Nummerierung |
| Horizontal Rule `---` | ⚠️ meist nicht gerendert | `━`-Unicode-Linie |
| Bold / Italic / Inline-Code | ✅ | — |
| Fenced Code-Block (Highlighting, 57 Sprachen) + Diff-Blocks | ✅ | unbekannte Sprache → Fallback `text` |
| Tabellen (ASCII-Grid) | ✅ | Links in Tabellen zeigen ggf. volle URL statt Label |
| Bullet-/nummerierte Listen | ✅ | — |
| Klickbare Links `[text](url)` | ✅ (OSC-8) | nur http(s):// |
| Emoji (farbig) | ✅ | Win Terminal ab 1.19, macOS immer |
| Unicode-Linien `━ ═ ─`, Box-Drawing `┌┐└┘╔╗` | ✅ | NICHT einfärbbar |

---

## Terminal-Output vs. Antworttext (wichtige Grenze)

Im **Bash-Tool-Output und in Skripten** (PowerShell/bash) funktionieren ANSI-Farben (16/256/
Truecolor), Box-Drawing, Braille-Spinner und OSC-8-Hyperlinks — **im Markdown-Antworttext NICHT**.
Fallen: Terminal.app fällt bei Truecolor still auf 256 Farben zurück (falsche Farben); Inline-Bilder
gehen nur in iTerm2 (OSC 1337), nicht in Windows Terminal/Terminal.app; Sixel nirgends stabil.

---

## Technische Limits (Kern)

- **Trennlinien:** `━`-Linien auf **80 Zeichen** halten (passt in fast jedes Terminal ohne Umbruch; >120 bricht um).
- **Tabellen:** ~6-8 Spalten, Zellen <30 Zeichen, sonst unleserlich.
- **Encoding:** Windows Python IMMER `open(path,'w',encoding='utf-8')`; PowerShell `[Console]::OutputEncoding=[Text.Encoding]::UTF8`; Skripte via `.gitattributes` auf LF.

---

## Was IMMER zuerst sagen (Klartext-Sätze)

- „Markdown rendert keine Farben für [Linien/Text/Hintergrund] — die Optionen sind: …"
- „Echte Schriftgrößen gibt es im Terminal nicht — Bold ist das Maximum."
- „Animationen sind im Antworttext nicht möglich — stattdessen [statische Lösung]."
- „Im CLI gibt es keine Inline-Bilder — aber ich kann einen klickbaren Link setzen."

## Mapping Wunsch → Antwort (Beispiele)

| Wunsch | Antwort die ich gebe |
|--------|----------------------|
| „Linie orange" | „Markdown färbt Linien nicht — nur ein 🟠-Endmarker oder farblose Linie." |
| „Text größer" | „Keine Schriftgrößen im Terminal — nur **Bold**." |
| „Box animiert" | „Animationen gehen nicht — statische Box ist das Maximum." |
| „Roter Text" | „Geht nicht im Text — ich nutze ❗/🔴 für 'Achtung'." |
| „Bild rein" | „Nur als `[Image #N]` mit klickbarem Link." |
| „Spinner beim Build" | „Nur in Bash-Tool-Output, nicht im Antworttext." |

---

## Was NIEMALS passieren darf

- ❌ Format-Versuch starten ohne vorheriges Nachschlagen in dieser Tabelle
- ❌ Mehr als EINE Iteration an einem technisch unmöglichen Feature
- ❌ Ein unmögliches Feature als „mit einem Trick machbar" verkaufen
- ❌ `<span style="color:…">`-HTML oder ANSI-Codes in den Antworttext setzen (werden Raw-Text)

---

## Zusammenspiel

| Regel | Bezug |
|-------|-------|
| `task-completion-summary` | Quelle für Trennlinien-/Marker-Stil |
| `german-skill-triggers` | Bei optischen Fragen zuerst hierher |
| Direktive #1 Superintelligenz | Renderer-Grenzen kennen = Token-Verschwendung vermeiden |
