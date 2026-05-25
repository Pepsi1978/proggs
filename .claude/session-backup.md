# Session Handoff — 2026-05-25 (Kontext bei 99%)

## Ziel
Voice Terminal Overlay (TVO, Windows/WPF, `~/proggs/TerminalVoiceOverlay-Windows/`):
Positions-Merk-Feature (Diskette) + flüssiges Gleiten beim Orientierungswechsel
(vertikal↔horizontal) perfektionieren. Zuletzt: das Hochgleiten (vertikale Säule)
ruckelfrei bekommen, ohne die transparente Optik zu verlieren.

## Aktueller Status
- Erledigt (committed+gepusht, #1051–#1075): Diskette-Positions-Feature (merken/löschen pro
  Ausrichtung, grün-Indikator, optionale DB-Persistenz), Enter-Button-Ausrichtung, Bugfix
  collapsed-Mic-Sprung, komplette Glide-Lösung.
- Glide-Lösung steht (HYBRID): Runtergleiten (horizontale Leiste) = Fenster-Move + `DwmFlush()`
  = perfekt, auch über weißer Schrift. Hochgleiten (vertikale Säule) = Content-Transform
  (`TranslateTransform` + `BitmapCache`) in stehendem, vergrößertem Fenster = transparent + flüssig.
- Forcierte `Timeline.DesiredFrameRate` final ENTFERNT (#1074): feste Werte (50/120) kämpfen
  gegen V-Sync → vsync-gebundener WPF-Default ist am glattesten.
- Learning VERIFIZIERT gespeichert: Memory `reference_wpf_overlay_smooth_animation.md` + `~/proggs/LEARNINGS.md` (#1075).

## Relevante Dateien
- `TerminalVoiceOverlay-Windows/Views/OverlayWindow.xaml.cs` — Glide: `AnimateWindowTo` (dispatcht),
  `GlideByWindowMove` (horizontal+DwmFlush), `GlideByContentTransform` (vertikal), `FinalizeContentGlide`,
  `StopGlide`. Diskette: `BtnSavePosition_Click`, `PositionForCurrentOrientation`.
- `TerminalVoiceOverlay-Windows/NativeMethods/Win32.cs` — `DwmFlush` P/Invoke.
- `~/.claude/settings.json` (BOM! mit utf-8-sig lesen) — Hook-Registrierung (Bug unten).
- `~/.claude/hooks/session-backup-nudge.ps1` / `.sh` — falsch verdrahteter Nudge-Hook.

## Getroffene Entscheidungen
- Transparenten Look (`AllowsTransparency=True`) behalten; DWM-Acrylic verworfen (milchiger Look abgelehnt).
- Methode nach FENSTER-FORM: flach→Fenster-Move+DwmFlush, schmal-hoch→Content-Transform.
- Build+Restart immer via `pwsh -File update.ps1` im TVO-Ordner.

## Fehlgeschlagene Ansätze (NICHT wiederholen)
- `DoubleAnimation` auf `Window.Left/Top` → ruckelt.
- Content-Glide für das BREITE horizontale Fenster → riesige transparente Fläche → flackert über Text.
- Tall translucent window per SetWindowPos bewegen → zu teuer → ruckelt (daher Content-Transform vertikal).
- Forcierte DesiredFrameRate 50 (Beating mit 60/120Hz) und 120 (Layered schafft das nicht) → beide schlechter.
- DWM-Umstellung (AllowsTransparency=False + Acrylic) → Look ändert sich → abgelehnt.
- Opake Pille während Glide → Frank will durchsichtig.

## Nächste Schritte (priorisiert)
1. **HOOK-BUG FIXEN (Auslöser dieses Backups):** In `~/.claude/settings.json` ist
   `session-backup-nudge.ps1` unter Event **`StopFailure`** registriert statt **`Stop`** → die
   92%-Backup-Erinnerung feuert NIE bei normalem Antwortende. FIX: das Hook-Objekt
   (`pwsh ... session-backup-nudge.ps1`, timeout 5) aus der `StopFailure`-Gruppe ENTFERNEN und in
   die bestehende `Stop`-Gruppe (neben hyperagent-stop.ps1 + task-ledger-stop.ps1) EINFÜGEN.
   Datei mit utf-8-sig bearbeiten (BOM erhalten!), JSON validieren. Dann 3-Datei-Settings-Sync
   (`claude-code-setup/settings-reference.json` + macOS `settings.json` + `settings.local.json`)
   und `.sh`-Gegenstück prüfen. Commit+Push.
2. ZUSATZ-Befund: Statusline-ctx-% (z.B. 96%) ≠ UI-% (99%) — andere Berechnung/Nenner. Der Hook
   liest den Statusline-Wert (aus `~/.claude/state/ctx-<sid>`). Prüfen ob die 92%-Schwelle zum
   richtigen Maß passt; ggf. Schwelle/Quelle angleichen.
3. TVO Hochgleiten final verifizieren: läuft die vsync-Variante (#1074) für Frank restlos glatt?
   Wenn nein: Monitor-Refreshrate per Win32 auslesen, Animation an sauberen Teiler koppeln.

## Offene Fragen
- Läuft das Hochgleiten nach #1074 (ohne forcierte fps) jetzt restlos glatt? (Frank-Antwort abwarten.)

## Anker
- Branch: main
- Letzte Commits:
```
3c4857d2 #1075 - LEARNINGS: WPF overlay smooth-glide best practice (verified)
7670ba01 #1074 - TVO: drop forced DesiredFrameRate on up-glide (vsync-aligned)
4f1075b9 #1073 - TVO: up-glide DesiredFrameRate 120 -> 50
1bdff7ff #1072 - TVO: 120fps DesiredFrameRate for up-glide
(weiter: #1051–#1071 = Diskette-Feature + Glide-Iterationen)
```
- Frank arbeitet mit 2 parallelen Sessions am selben Repo. Beim Committen nur eigene Dateien
  namentlich stagen, fetch+rebase vor push.
