# NovaDrehen: Drehsperre pendelt zugeklappt — fremde Fensterereignisse überschreiben „vorne“

## Symptom

Galaxy Fold zugeklappt, Nova Launcher vorne, Handy zur Seite gedreht: Der Bildschirm dreht trotz
Sperre ins Querformat, dann von allein wieder hoch, wieder runter, wieder hoch, solange das Handy
quer gehalten wird. Trat „regelmäßig, aber nicht immer“ auf (Version 1.0.5 und früher).

## Ursache

`DrehService.onAccessibilityEvent` setzte `vorne` auf das Paket **jedes**
`TYPE_WINDOW_STATE_CHANGED`-Ereignisses (außer einer kleinen Ignorierliste). Absender sind aber
nicht nur App-Wechsel, sondern auch Toasts (das Ereignis trägt das Paket der auslösenden App),
Overlays (Edge-Panel, Chat-Köpfe, Routinen-Apps), Popups und Systemdienste. Solche Fenster werden
beim Drehen neu aufgebaut und senden dann erneut Ereignisse.

Rückkopplungsschleife: Overlay-Ereignis → `vorne` = fremdes Paket → Sperre aus → Bildschirm dreht
quer → Nova layoutet neu (Nova-Ereignis) → Sperre an, Hochformat → Overlay baut neu auf
(Ereignis) → Sperre aus → … `notificationTimeout=100` verstärkte das Zufallselement (nur das
letzte Ereignis in 100 ms kommt an).

5-Warum: Pendeln ← Sperre kippt ← `vorne` kippt ← fremde Ereignisse werden als App-Wechsel
gelesen ← **„welche App ist vorne“ wurde aus dem Ereignis-Absender abgeleitet statt aus dem
tatsächlichen Fensterzustand.**

## Fix (Version 1.0.6)

1. **Wahrheit aus der Fensterliste:** `vordergrundAusFenstern()` nimmt das fokussierte, sonst
   aktive, sonst oberste `AccessibilityWindowInfo.TYPE_APPLICATION`-Fenster.
   `TYPE_APPLICATION_OVERLAY`, Toasts, Systemleiste und Tastatur haben andere Typen und fallen
   strukturell heraus. Wird auch bei `TYPE_WINDOWS_CHANGED` und 200 ms nach jedem Ereignis neu
   gelesen (die Liste kann nachhinken).
2. **Ereignis zählt nur als echte Activity:** `istActivity(pkg, klasse)` über PackageManager;
   Paket-Sichtbarkeit per `<queries><intent action=MAIN/></queries>` (keine
   `QUERY_ALL_PACKAGES` nötig). Alles andere wird ignoriert und protokolliert.
3. **Nie „unbekannt = entsperren“:** Lässt sich nichts sicher bestimmen, bleibt `vorne` wie es ist.
4. **Entprellung:** Sperren sofort, Lösen erst nach 500 ms stabilem Wunsch (Frist ab dem ersten
   Wunsch, damit häufige `onDisplayChanged`-Aufrufe sie nicht endlos verschieben). Muss länger
   sein als die 200-ms-Nachprüfung.
5. **ChatGPT-Sprachmodus:** wird nur noch durch einen echten Activity-Wechsel oder das
   Verschwinden des ChatGPT-Fensters beendet, nicht durch Einblendungen.
6. **Diagnose:** `Protokoll` (Ringpuffer, 60 Zeilen) in der App sichtbar, inkl. „Auto-Drehen von
   außen gestellt“ (fremder Schreiber, z. B. Routinen/Modi).

## Verwandte Fehlerquellen geprüft

- Gleiche Klasse: ChatGPT-Sprachmodus wurde von jedem fremden Ereignis beendet → mitgefixt.
- Gleiche Komponente: `zugeklappt()` ist drehungsunabhängig (`min(w,h)`), Schreibreihenfolge
  `USER_ROTATION` vor `ACCELEROMETER_ROTATION` korrekt → unverändert.
- Fremde Schreiber auf `ACCELEROMETER_ROTATION` (Bixby-Routinen, Modi, Good Lock) können
  ebenfalls Pingpong erzeugen → nicht per Code lösbar, wird jetzt protokolliert.
- Offen, bewusst unverändert: Novas Discover-Seite ist ein Fenster der Google-App und hebt die
  Sperre beim Hineinwischen auf (wie bisher).

## Poka-Yoke

Stufe 3 (Eliminierung): Overlay-Fenster können den Vordergrund nicht mehr bestimmen, weil die
Auswahl nach Fenstertyp erfolgt, nicht nach einer Paketliste, die man pflegen müsste.

## Muster-Erkennung

Ein AccessibilityService, der „App vorne“ aus `event.packageName` ableitet, ist anfällig für
Toasts, Overlays und Systemdienste. Immer die Fensterliste (`windows`, `TYPE_APPLICATION`,
`isFocused`) als Quelle nehmen und unbekannte Zustände als „unverändert“ behandeln, nie als
„nichts vorne“. Zustandswechsel, die etwas Sichtbares auslösen (Drehen), entprellen.
