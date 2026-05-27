# Manual Tests — TerminalVoiceOverlay-macOS Portierung

Diese Datei wird **pro Etappe** abgehakt — Frank testet auf dem Mac, traegt das
Ergebnis ein. So ist bei spaeteren Regressionen klar welche Etappe der Auslöser war.

App-Pfad: `~/proggs/TerminalVoiceOverlay-macOS/build/TerminalVoiceOverlay.app`
Build: `cd ~/proggs/TerminalVoiceOverlay-macOS && bash build.sh && open build/TerminalVoiceOverlay.app`

---

## Test-Matrix

| # | Etappe | Test-Aktion | Erwartet | Ergebnis |
|---|--------|-------------|----------|----------|
| 1a | BeamFade-Helpers | (kein direkter Test — Grundlage) | App startet, vertikale Saeule sichtbar | □ |
| 1b | Horizontales Layout | (kein direkter Test — wird in 2 sichtbar) | — | □ |
| 2  | beamToOrientation | App offen → **Cmd+Shift+O** | Saeule fadet aus → Leiste taucht oben auf → fadet ein | □ |
| 2  | zurueck zu vertical | Erneut **Cmd+Shift+O** | Leiste glidet hoch → fadet aus → Saeule fadet an alter Position ein | □ |
| 3  | Glide-Animation | Wie #2 — Leiste rutscht beim Erscheinen sanft nach unten | Smootherstep, keine Rucker, ~400ms | □ |
| 4  | Collapsed-Mic | **Cmd+Shift+C** in einer der Formen | Form fadet aus → 84×84 Pille mit nur Mic erscheint | □ |
| 4  | Aufklappen | Erneut **Cmd+Shift+C** | Pille fadet aus → vorherige Form an gemerkter Position taucht auf | □ |
| 5a | OrientationToggleButton | Auf den ⇄ Button klicken | Selbes Verhalten wie Cmd+Shift+O | □ |
| 5a | SaveButton (Diskette) | Panel an neue Position rechtsklick-draggen → Diskette klicken | Position wird gespeichert (im Log: `[App] saved ... position: ...`) | □ |
| 9  | AutoEnter HTTP Status | Terminal: `curl http://127.0.0.1:5723/autoenter/status` | `{"on":true}` oder `{"on":false}` | □ |
| 9  | AutoEnter HTTP Toggle | Terminal: `curl -X POST http://127.0.0.1:5723/autoenter/toggle` | Status flippt, JSON-Antwort mit neuem Wert | □ |
| 10 | Letter-Hotkey registriert | **Cmd+Opt+A** druecken, `tail -1 /tmp/tvo-debug.log` | Log-Zeile mit "Cmd+Opt+A — letter-hotkey not yet wired" | □ |

---

## Status-Legende

- `□` = noch nicht getestet
- `✅` = wie erwartet
- `❌` = funktioniert nicht — Notiz dazu
- `⚠️` = funktioniert teilweise — Notiz dazu

---

## Bekannte offene Etappen (NICHT 1:1 portiert)

| # | Etappe | Status / Workaround |
|---|--------|---------------------|
| 5b | Drag-Verhalten Rechtsklick + Threshold | OverlayPanel.swift hat schon `setupDragMonitors()` — Threshold/Profile-Tile-Cancel fehlt noch |
| 6  | Settings-Dialog (620×640) | Aufwendige UI — separater PR mit NSWindow noetig |
| 7  | Confirm/PromptEdit/PromptHistoryEdit/TextInput-Dialoge | Aufwendige UI — eigene PR-Serie |
| 8  | PromptInputWindow + PromptHistoryWindow | Aufwendige UI — eigene PR-Serie |
| 10 | Letter-Hotkey DB-Lookup | `HotkeyLetter`-Spalte fehlt im SQLite-Schema — eigene Migrations-Etappe |
| 12 | ClaudeCodexVoiceOverlay-macOS angleichen | Andere Button-Struktur — eigene Adaptation der TVO-Dateien |

---

## Drive-Restore (separater Punkt)

| Aktion | Erwartet | Ergebnis |
|--------|----------|----------|
| In der App auf Settings → Google Drive verbinden | OAuth-Browser-Fenster → Erfolg | □ |
| App neu starten | `/tmp/tvo-debug.log` sollte `launch-restore applied remote backup from ...` zeigen | □ |
| Prompt-Anzahl checken | `python3 -c "import json; print(len(json.load(open('/Users/frank/Library/Application Support/TerminalVoiceOverlay/history/prompt-history.json'))))"` — sollte >24 sein wenn neue Prompts vom 26.05. da | □ |
