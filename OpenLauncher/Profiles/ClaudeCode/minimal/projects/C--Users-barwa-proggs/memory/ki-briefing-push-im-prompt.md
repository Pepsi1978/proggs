---
name: ki-briefing-push-im-prompt
description: "Geplante Aufgabe „Tägliches KI Briefing“ (trig_01DUB6nEaZWEAisQv6WuaZqE): Push der Aufgabe kommt nie am Handy an, darum ruft der Prompt selbst PushNotification auf"
metadata:
  node_type: memory
  type: project
  originSessionId: 84bc23d8-b164-4e92-b97e-67f0af0cbc89
  modified: 2026-11-07T13:54:38.283Z
---

„Tägliches KI Briefing“ = Cowork-Cloud-Routine (Tags cowork-scheduled), täglich 6:00, stellt die Datei per SendUserFile zu. Eingebaute Einstellung „Push-Benachrichtigung“ ist an, trotzdem kam am Handy (com.anthropic.claude, Kanal `routines` „Geplante Aufgaben“, eingeschaltet) nie etwas an; geprüft 24.10.2026 per `adb shell dumpsys usagestats` (nur Kanal `bogosort` = Code-Sitzungen bekommt Pushes).

**Why:** Anthropic schickt diesen Push serverseitig nicht ab; am Handy liegt es nicht.
**How to apply:** Seit 24.10.2026 enthält der Prompt Schritt 6 „PUSH-BENACHRICHTIGUNG ANS HANDY“ (PushNotification, sonst SendUserMessage). Prompt nur über claude.ai/scheduled-task/<id> → Menü → Bearbeiten ändern, nicht per RemoteTrigger update (job_config ~130 KB inklusive Cowork-Systemprompt). Kommt weiter nichts an: `get_run_log` des letzten Laufs prüfen, ob PushNotification aufgerufen wurde und was es meldete.
