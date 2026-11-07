---
name: fold8-emulator-starten
description: "\"Emulator starten\" heißt: Start-Fold8.ps1 mit -Projekt <aktuelles Projekt>; Größe folgt dem echten Fold"
metadata: 
  node_type: memory
  type: project
  originSessionId: c980f509-916d-45cd-8ff1-927156757c5e
  modified: 2026-08-12T11:56:23.777Z
---

„starte den Emulator“, „zeig das im Emulator“, „mach den Emulator auf“ o. Ä. = immer Fold-8-Emulator (Profilregel 12):

```powershell
& powershell -ExecutionPolicy Bypass -File "C:\Users\barwa\proggs\Werkzeuge\fold8-emulator\Start-Fold8.ps1" -Projekt <Projektname>
```

Projekt = das der Sitzung, ohne Nachfrage, wenn eindeutig. Skript baut, installiert, startet die App.

**Wichtigster Punkt – richtiges Display:** Frank vergleicht IMMER mit dem Handy in der Hand. Zugeklappt = Cover-Display (5,5″, 7,47 × 11,80 cm), aufgeklappt = Innendisplay (7,6″, 11,63 × 15,37 cm; 704 dp breit = Tablet-Fläche, alles wirkt viel größer). Das Skript liest `wm size` des echten Geräts und wählt die AVD selbst. Nie eigenmächtig aufs Innendisplay wechseln, wenn das Handy zugeklappt ist.

Gilt immer:
- **Originalgröße** (1:1 in cm); größer nur auf Zuruf (`-Zoom`)
- **zentriert** mit Rand ringsum (seitliche Bedienleiste sichtbar)
- **Hochformat**, nie von selbst quer
- **kein Geräterahmen** (Samsungs Skin ist größer als der Bildschirm)

**Fenster – nicht erneut versuchen:** Emulator kennt keine Startgröße (`-scale` abgeschafft, `-window-size` nur Fuchsia, `emulator-user.ini` wird beim Beenden überschrieben). Er startet immer bildschirmfüllend; Korrektur einmalig per `SetWindowPos`, sobald das Fenster existiert. scrcpy nimmt die Größe dagegen beim Start (`--window-width/--window-height`).

Muster: Fensterinnenmaß weicht in der **Breite** vom Soll ab, Höhe passt → Emulator steht zugeklappt (`emu sensor set hinge-angle0 180`).

Alle Fallen: `bugs/android/emulator-foldable.md` (Punkte 18–20). Verwandt: [[android-apps-immer-installieren]]
