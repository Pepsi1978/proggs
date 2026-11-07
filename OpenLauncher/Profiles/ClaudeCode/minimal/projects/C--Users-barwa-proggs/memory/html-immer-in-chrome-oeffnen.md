---
name: html-immer-in-chrome-oeffnen
description: "Erzeugte HTML-Dateien immer direkt lokal in Chrome öffnen, nicht nur als Artifact-Link ausgeben"
metadata:
  node_type: memory
  type: feedback
  originSessionId: a34d2da2-fc59-45f6-bb1c-529bcf5f3f8f
  modified: 2026-11-07T13:55:09.292Z
---

Jede erzeugte HTML-Datei (Beispiele, Demos, Berichte, Seiten), die der Nutzer ansehen soll, nach dem Schreiben sofort ohne Rückfrage lokal in Chrome starten: `Start-Process chrome "<Pfad zur .html>"`.

**Why:** 04.10.2026 hatte ich eine Motion-Graphics-Seite nur als claude.ai-Artifact-Link ausgegeben. Nutzer: „immer in chrome html datein starten!“

**How to apply:**
- `<meta charset="utf-8">` an den Dateianfang, sonst können Umlaute lokal kaputtgehen.
- Meine Auslegung: ein Artifact-Link allein reicht nicht; ob er zusätzlich erwünscht ist, ist nicht geklärt.
