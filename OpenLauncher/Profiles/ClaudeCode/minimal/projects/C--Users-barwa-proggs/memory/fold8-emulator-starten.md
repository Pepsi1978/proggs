---
name: fold8-emulator-starten
description: "\"Emulator starten\" heißt: Start-Fold8.ps1 mit -Projekt <aktuelles Projekt>; Größe folgt dem echten Fold"
metadata: 
  node_type: memory
  type: project
  originSessionId: c980f509-916d-45cd-8ff1-927156757c5e
  modified: 2026-08-12T11:56:23.777Z
---

Sagt Frank "starte den Emulator", "zeig das im Emulator", "mach den Emulator auf" oder Ähnliches,
ist immer der Fold-8-Emulator gemeint. Aufruf:

```powershell
& powershell -ExecutionPolicy Bypass -File "C:\Users\barwa\proggs\Werkzeuge\fold8-emulator\Start-Fold8.ps1" -Projekt <Projektname>
```

Das Projekt ist das, an dem in der Sitzung gerade gearbeitet wird — ohne Nachfrage einsetzen, wenn
es eindeutig ist. Das Skript baut, installiert und startet die App.

**Der wichtigste Punkt — das richtige Display.** Frank vergleicht IMMER mit dem Handy in seiner
Hand. Ist es zugeklappt, sieht er das Cover-Display (5,5″, 7,47 × 11,80 cm); aufgeklappt das
Innendisplay (7,6″, 11,63 × 15,37 cm). Das Innendisplay hat 704 dp Breite — Tablet-Fläche, dort
wirkt alles viel größer. Das Skript liest deshalb `wm size` des echten Geräts und wählt die
passende AVD selbst. Nie eigenmächtig auf das Innendisplay wechseln, wenn das Handy zugeklappt ist.

Weitere Erwartungen, die immer gelten:
- **Originalgröße** ist der Standard (1:1 in Zentimetern); größer nur auf Zuruf (`-Zoom`)
- **zentriert** mit Rand ringsum, damit die seitliche Bedienleiste sichtbar bleibt
- **Hochformat**, niemals von selbst quer
- **kein Geräterahmen** — Samsungs Skin ist größer als der Bildschirm

**Was beim Fenster nicht funktioniert (nicht erneut versuchen):** Der Emulator kennt keine
Startgröße — `-scale` ist abgeschafft, `-window-size` gilt nur für Fuchsia, und `emulator-user.ini`
wird beim Beenden überschrieben. Er startet immer bildschirmfüllend; die Korrektur per
`SetWindowPos` erfolgt einmalig, sobald das Fenster existiert. scrcpy kann seine Größe dagegen
beim Start entgegennehmen (`--window-width/--window-height`).

Muster-Erkennung: Weicht das Fensterinnenmaß in der **Breite** vom Soll ab, während die Höhe passt,
steht der Emulator zugeklappt (`emu sensor set hinge-angle0 180`).

Volltext aller Fallen: `bugs/android/emulator-foldable.md` (Punkte 18–20).
Verwandt: [[android-apps-immer-installieren]]
