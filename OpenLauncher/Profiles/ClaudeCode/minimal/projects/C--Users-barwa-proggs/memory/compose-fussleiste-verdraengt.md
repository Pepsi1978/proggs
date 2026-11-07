---
name: compose-fussleiste-verdraengt
description: "In Compose darf eine Knopfleiste nie letztes Kind einer Column sein — Insets allein reichen nicht, Box mit BottomCenter nutzen"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: db8d5db1-f570-4d57-a8fc-a29ca646d3fa
  modified: 2026-11-07T13:54:26.170Z
---

Eine Fußleiste (Abbrechen/Übernehmen) als **letztes Kind einer Column** wird aus dem Bild gedrückt, sobald ein Geschwister mehr Höhe misst als der Bildschirm hergibt, auch mit `weight(1f)` darüber. `statusBarsPadding()`/`navigationBarsPadding()` helfen **nicht**: Problem ist die Messung, nicht die Systemleisten.

Auslöser im Gedankenspeicher-Tabelleneditor: `Row(Modifier.height(IntrinsicSize.Min))` in einem `verticalScroll`-Bereich; die Intrinsic-Messung schlägt durch die `weight`-Kette nach oben durch und beansprucht die volle Inhaltshöhe.

**Why:** Zwei Anläufe mit Insets-Padding scheiterten, erst der Strukturwechsel wirkte. Ohne Notiz wird wieder an den Insets gedreht.

**How to apply:**
1. Vollbild-Blätter als `Box(fillMaxSize)`, Fußleiste per `align(Alignment.BottomCenter)` aus dem Fluss nehmen, ihre Höhe per `onSizeChanged` messen und dem Inhalt als `padding(bottom = …)` geben → strukturell unverdrängbar (Poka-Yoke Stufe 3). Im Gedankenspeicher: Gerüst `VollbildBlatt` in `ui/verlauf/Anhaenge.kt`.
2. `IntrinsicSize.Min` nie in einem scrollbaren Bereich; stattdessen feste Höhe (dort: `ZEILENHOEHE`).
3. Anzeige und Editor derselben Sache mit derselben Zellhöhe bauen, sonst springt das Layout beim Bearbeiten.

Siehe auch [[android-apps-immer-installieren]].
