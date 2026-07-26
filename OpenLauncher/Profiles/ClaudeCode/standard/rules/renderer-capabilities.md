# Renderer-Capabilities & technische Limits (KRITISCH)

> Bei jedem optischen Wunsch (Farbe, Linien, Groesse, Animation, Layout) ZUERST hier nachschlagen.
> **Volle Lookup-Tabelle + Terminal-Grenzen:
> `claude-code-setup/docs/rules/renderer-capabilities.md`.**

## Verhaltens-Regel (vor jedem Format-Versuch)
1. Nachschlagen ob es im Claude-Code-Renderer geht. 2. **Unmoeglich** → sofort in Klartext erklaeren +
Alternative, KEIN Versuch, KEINE Iteration. 3. **Eingeschraenkt** → Grenze benennen, beste Annaeherung.
4. **Voll moeglich** → direkt umsetzen.

## Die haeufigsten Grenzen (im Antworttext)
- ❌ **Farbige Linien/Text/Hintergrund** → Emoji-Symbole (🟠🟢🟡✅⚠️❗) tragen die Farb-Info; farblose `━`-Linie.
- ❌ **Schriftgroessen / Unterstrichen** → `**Bold**` bzw. Inline-Code.
- ❌ **Animationen/GIFs/Videos** → statische Beschreibung. **Inline-Bilder** → `[Image #N]` (klickbarer Link).
- ❌ **HTML `<span style>` / ANSI im Antworttext** → werden Raw-Text; ANSI nur in Bash-Output/Skripten.
- ✅ Bold/Italic/Inline-Code, Tabellen, Listen, Emoji, Code-Bloecke (Highlighting/Diff), Links (http(s)),
  Unicode-Linien `━ ═ ─` (NICHT einfaerbbar). Trennlinien auf **80 Zeichen** halten.

## Was NIEMALS
- Format-Versuch ohne Nachschlagen · >1 Iteration an einem unmoeglichen Feature · Unmoegliches als "mit
  einem Trick machbar" verkaufen · `<span style>`-HTML oder ANSI in den Antworttext setzen.
