---
name: jarvis-rueckfrage-nie-stehen-bleiben
description: "Rückfrage (AskUserQuestion) steht sofort am PC UND in Jarvis, beide 5 Minuten; danach entscheidet die Sitzung selbst – nie offener Dialog"
metadata:
  node_type: memory
  type: feedback
  originSessionId: db1f27e1-357c-4d86-8086-88c4c181fd05
  modified: 2026-11-07T13:41:46.014Z
---

Franks Vorgabe vom 07.11.2026 (ersetzt 23.10.2026): Eine Rückfrage, auch „Zusätzliches Code-Review?“, erscheint sofort im Dialog am Rechner und sofort in Jarvis auf dem Handy; beide bleiben fünf Minuten beantwortbar, wer zuerst antwortet, gewinnt (Antwort am PC schließt sie in Jarvis, Antwort in Jarvis wird in die Sitzung getippt). Ohne Antwort entscheidet die KI nach fünf Minuten selbst, was sinnvoll ist (auch beim Review, nicht mehr pauschal Nein), und arbeitet weiter. Gilt für am PC gestartete Sitzungen und für Jarvis-Aufträge.

**Why:** Früher wartete der Hook `jarvis_hook.py frage` selbst; Claude Code öffnet den Dialog erst danach. Eine Maus-/Tastaturberührung am PC (`am_rechner`, Schonfrist 15 s) schloss die Frage in Jarvis nach 41 s, während Frank am Handy noch überlegte (07.11.2026, Sitzung 8Y64L).

**How to apply:** Windows: Hook antwortet sofort mit `ask` + Frist im Fragetext und startet den abgekoppelten `frage-warter`; der tippt per WriteConsoleInputW ESC + Antwort + Enter. Diagnose im `%LOCALAPPDATA%\JarvisPcBruecke\hook.log` (`Frage-Warter …`). macOS: dasselbe in tmux (OpenLauncherMac „Terminal: tmux“); in Terminal.app ohne tmux, auf dem Server und in OpenCode wartet der Hook selbst, die Frage bleibt dann 5 min nur in Jarvis, danach entscheidet die Sitzung. Der Ablauf darf nie in einem offenen Dialog enden. Almanach: `bugs/claude-tooling/claude-hooks.md` §16.9. Verwandt: [[gestartetes-programm-erbt-agenten-pipe]].
