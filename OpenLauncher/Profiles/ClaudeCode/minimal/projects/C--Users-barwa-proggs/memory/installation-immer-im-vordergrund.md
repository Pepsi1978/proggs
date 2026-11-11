---
name: installation-immer-im-vordergrund
description: "App-Installationen (jarvis-install, adb install, auf-beide) nie mit run_in_background, immer im Vordergrund mit höchstem Zeitlimit"
metadata:
  node_type: memory
  type: feedback
  originSessionId: 0abaeb31-69bd-42f5-988f-2b9be8da3543
  modified: 2026-11-11T18:07:32.989Z
---

Installationen auf Handy oder Tablet immer im Vordergrund starten (Zeitlimit 600000), nie mit `run_in_background`.
Mehrere Geräte nacheinander, jedes in einem eigenen sichtbaren Aufruf.

**Why:** Frank will sehen, ob gerade installiert wird, ob es hängt oder ins Zeitlimit läuft (11.11.2026: „Ich möchte bitte Installation immer im Vordergrund sehen, niemals im Hintergrund“).

**How to apply:** Läuft ein Aufruf trotzdem über 10 Minuten und wird vom Werkzeug verschoben, das sofort melden und den Fortschritt sichtbar prüfen (Dateigröße unter /data/local/tmp). Siehe [[tunnel-install-langsam-heim-wlan]].
