# Codex Desktop normal starten (Autostart im Tray + Desktop-Verknüpfung)

Version 2.0.1 — 08.10.2026, 14:16 Uhr

Startet **Codex Desktop** (MSIX-Paket `OpenAI.Codex`) **ohne Administratorrechte**:
beim Anmelden automatisch im System-Tray und per Desktop-Verknüpfung sichtbar.

## Warum nicht mehr als Administrator

Bis Version 1.2.0 startete der Launcher (`Start-CodexAdmin.ps1`) `app\ChatGPT.exe` direkt aus
dem Paketordner, erhöht und mit `--do-not-de-elevate`. Folge: Codex lief **ohne
Paket-Identität**, und der eingebaute Updater brach ab mit

> Automatic updates are unavailable right now. Updater initialization skipped: missing current
> Windows Package Family.

Seit 2.0.0 startet der Launcher Codex nur noch über die Paket-Aktivierung
`shell:AppsFolder\OpenAI.Codex_2p2nqsd0c76g0!App`: normale Rechte, mit Paket-Identität,
und der Updater funktioniert.

**Regel:** Codex nie direkt über `ChatGPT.exe` starten und nie erhöht (keine Aufgabe mit
„Höchste Rechte“, kein „Als Administrator ausführen“-Bit in der Verknüpfung).

## Dateien

| Datei | Zweck |
|---|---|
| `Start-Codex.ps1` | Der Launcher. Ohne Schalter: sichtbares Fenster. Mit `-Background`: im Tray. Mit `-Neustart`: laufende Instanz beenden und neu starten. |
| `Install-CodexAutostart.ps1` | Richtet die Autostart-Verknüpfung `Codex minimiert.lnk` im Autostart-Ordner (normale Rechte, `-Background`) und die Desktop-Verknüpfung ein. Einmal ausführen (eine UAC-Abfrage, nur um die alte Admin-Aufgabe zu entfernen). |
| `codex.ico` | Symbol für die Desktop-Verknüpfung. |

## Verhalten des Launchers

1. Sucht laufende Codex-Hauptprozesse (Elternprozess ist kein `ChatGPT.exe`; das Paket
   „ChatGPT Classic“ wird ignoriert).
2. Läuft noch eine **erhöhte** Altinstanz → Rückfrage „beenden und normal neu starten?“.
   Sie blockiert sonst per Single-Instance-Sperre jeden normalen Start.
3. Läuft eine alte Fassung nach einem Update → Rückfrage „neu starten?“.
4. Läuft Codex schon → erneute Paket-Aktivierung; die App zeigt ihr Fenster selbst.
5. Sonst: Start über `shell:AppsFolder`. Mit `-Background` wird danach `WM_CLOSE` gepostet,
   Codex geht selbst ins Tray.

Ergebnis jedes Starts: `%LOCALAPPDATA%\CodexAutostart\last-launch.json`.

## Finger weg vom Fenster

Niemals `ShowWindow`/`SetForegroundWindow` auf das Electron-Fenster anwenden: Das Fenster ist
dann zwar zu sehen, zeichnet aber nicht und nimmt keine Klicks an. Fenster zeigen = Paket erneut
aktivieren, ins Tray = `WM_CLOSE`. Win32 darf nur lesen.
