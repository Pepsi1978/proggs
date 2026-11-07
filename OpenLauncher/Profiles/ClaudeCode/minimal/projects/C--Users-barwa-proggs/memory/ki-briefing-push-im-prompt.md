---
name: ki-briefing-push-im-prompt
description: "Geplante Aufgabe „Tägliches KI Briefing“ (trig_01DUB6nEaZWEAisQv6WuaZqE): Push der Aufgabe kommt nie am Handy an, darum ruft der Prompt selbst PushNotification auf"
metadata:
  node_type: memory
  type: project
  originSessionId: 84bc23d8-b164-4e92-b97e-67f0af0cbc89
  modified: 2026-10-24T08:15:38.432Z
---

Die geplante Aufgabe „Tägliches KI Briefing“ läuft als Cowork-Cloud-Routine (Tags cowork-scheduled) täglich 6:00 und stellt die Datei per SendUserFile zu. Die eingebaute Einstellung „Push-Benachrichtigung“ ist an, trotzdem kam am Handy (com.anthropic.claude, Kanal `routines` „Geplante Aufgaben“, eingeschaltet) nie etwas an; geprüft am 24.10.2026 per `adb shell dumpsys usagestats` (nur Kanal `bogosort` = Code-Sitzungen bekommt Pushes).

**Why:** Anthropic schickt den Push dieser Aufgabe serverseitig nicht ab; am Handy liegt es nicht.
**How to apply:** Seit 24.10.2026 enthält der Prompt Schritt 6 „PUSH-BENACHRICHTIGUNG ANS HANDY“ (PushNotification, sonst SendUserMessage). Prompt nur über claude.ai/scheduled-task/<id> → Menü → Bearbeiten ändern, nicht per RemoteTrigger update (job_config ist ~130 KB groß, inklusive Cowork-Systemprompt). Kommt weiter nichts an: `get_run_log` des letzten Laufs prüfen, ob PushNotification aufgerufen wurde und was es meldete.
