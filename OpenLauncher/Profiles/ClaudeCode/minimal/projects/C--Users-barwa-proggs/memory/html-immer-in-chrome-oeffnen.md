---
name: html-immer-in-chrome-oeffnen
description: "Erzeugte HTML-Dateien immer direkt lokal in Chrome öffnen, nicht nur als Artifact-Link ausgeben"
metadata:
  node_type: memory
  type: feedback
  originSessionId: a34d2da2-fc59-45f6-bb1c-529bcf5f3f8f
  modified: 2026-10-04T20:53:24.091Z
---

Erzeugte HTML-Dateien (Beispiele, Demos, Berichte, Seiten) immer sofort lokal in Chrome starten: `Start-Process chrome "<Pfad zur .html>"`.

**Why:** Am 04.10.2026 hatte ich eine Motion-Graphics-Seite nur als claude.ai-Artifact-Link ausgegeben. Der Nutzer antwortete: „immer in chrome html datein starten!“

**How to apply:** Nach dem Schreiben jeder HTML-Datei, die der Nutzer ansehen soll, ohne Rückfrage in Chrome öffnen. `<meta charset="utf-8">` an den Dateianfang setzen, sonst können Umlaute lokal kaputtgehen. Meine Auslegung: ein Artifact-Link allein reicht nicht; ob er zusätzlich erwünscht ist, ist nicht geklärt.
