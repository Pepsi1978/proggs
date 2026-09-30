---
name: compose-fussleiste-verdraengt
description: "In Compose darf eine Knopfleiste nie letztes Kind einer Column sein — Insets allein reichen nicht, Box mit BottomCenter nutzen"
metadata: 
  node_type: memory
  type: feedback
  originSessionId: db8d5db1-f570-4d57-a8fc-a29ca646d3fa
  modified: 2026-08-19T11:35:57.347Z
---

Eine Fußleiste (Abbrechen/Übernehmen) als **letztes Kind einer Column** wird aus dem Bild
gedrückt, sobald ein Geschwister mehr Höhe misst als der Bildschirm hergibt — auch wenn ein
`weight(1f)` darüber steht. `statusBarsPadding()`/`navigationBarsPadding()` beheben das
**nicht**, weil das Problem nicht die Systemleisten sind, sondern die Messung.

Konkreter Auslöser im Gedankenspeicher-Tabelleneditor: `Row(Modifier.height(IntrinsicSize.Min))`
innerhalb eines `verticalScroll`-Bereichs. Die Intrinsic-Messung schlägt durch die
`weight`-Kette nach oben durch und beansprucht die volle Inhaltshöhe.

**Why:** Zwei Anläufe mit Insets-Padding haben den Fehler nicht behoben; erst der
Strukturwechsel hat gewirkt. Ohne diese Notiz wird beim nächsten Mal wieder an den Insets
gedreht.

**How to apply:**
1. Vollbild-Blätter mit `Box(fillMaxSize)` bauen, Fußleiste per `align(Alignment.BottomCenter)`
   aus dem Fluss nehmen, ihre Höhe per `onSizeChanged` messen und dem Inhalt als
   `padding(bottom = …)` geben. Dann ist die Leiste strukturell unverdrängbar (Poka-Yoke
   Stufe 3). Im Gedankenspeicher heißt das Gerüst `VollbildBlatt` in
   `ui/verlauf/Anhaenge.kt`.
2. `IntrinsicSize.Min` niemals innerhalb eines scrollbaren Bereichs verwenden — stattdessen
   feste Höhe (dort: `ZEILENHOEHE`).
3. Anzeige und Editor derselben Sache mit derselben Zellhöhe bauen, sonst springt das Layout
   beim Bearbeiten.

Siehe auch [[android-apps-immer-installieren]].
