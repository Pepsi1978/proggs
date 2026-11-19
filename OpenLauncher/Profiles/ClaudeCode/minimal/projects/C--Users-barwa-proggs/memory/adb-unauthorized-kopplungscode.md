---
name: adb-unauthorized-kopplungscode
description: "Gerät meldet unauthorized ohne Freigabe-Abfrage → sofort Frank um den Kopplungscode bitten, nicht über den Mac herumprobieren"
metadata:
  node_type: memory
  type: feedback
  originSessionId: af28bddf-eb38-4cb2-bebc-3a7d730d1978
  modified: 2026-11-19T14:41:33.474Z
---

Meldet ein Handy/Tablet `unauthorized` und erscheint nach einem Neuverbinden keine Freigabe-Abfrage, sofort Frank um den Kopplungscode bitten
(Entwickleroptionen › Drahtloses Debugging › Gerät mit Kopplungscode koppeln), Port per `adb mdns services`, dann `adb pair`.

**Why:** 19.11.2026 kostete das Herumprobieren über Jarvis Mac OS und wiederholtes Neuverbinden viele Minuten; Frank: „Mit der Nummer hättest du aber auch gleich nennen können … habe ich in drei Sekunden gemacht.“ Jarvis Mac OS gibt Kopplungscodes bewusst nicht weiter.

**How to apply:** Nach einem erfolglosen Neuverbinden direkt fragen; Details in bugs/android/adb-wlan-debugging.md §14. Siehe [[adb-wlan-selbstheilend]], [[jarvis-tablet-zweites-geraet]].
