---
name: windows-fernwartung-zugeklappt
description: "Jarvis-Fernwartung am zugeklappten Windows-Laptop – kein Bild = Anzeige aus, nicht Deckel; Parsec VDD als Notweg"
metadata:
  node_type: memory
  type: project
  originSessionId: 6fadc4dc-e61c-4d94-80ac-cb55e336643d
  modified: 2026-10-28T18:15:03.790Z
---

Stand 28.10.2026:
- „Desktop duplication access denied“ bzw. ddagrab ohne Bilder am zugeklappten Laptop = nach Zeitsperre abgeschaltete Anzeige; SC_MONITORPOWER „an“ (HWND_BROADCAST) bringt das Bild zurück.
- Parsec Virtual Display Adapter installiert (winget Parsec.ParsecVDD), Steuerung per IOCTL in `Jarvis/werkzeuge/windows-fernsteuerung/virtueller_bildschirm.py`.
- NoAutoRebootWithLoggedOnUsers=1 gesetzt.
- Seit 1.8.392: Deckel zu → Desktop hängt an Ausgabe ohne aktiven Monitor (EnumDisplayDevices), ddagrab hängt still ohne Fehler; Dienst erkennt das (`ohne_bildschirm()`) und hängt sofort den virtuellen Bildschirm an (Bild nach ~3 s).
- UAC fragt auf diesem PC nicht (ConsentPromptBehaviorAdmin=0), `Start-Process -Verb RunAs` läuft still.

**Why:** Frank will den Laptop tagelang zugeklappt per Jarvis fernbedienen.
**How to apply:** Bei „kein Bild“ zuerst dienst.log unter %LOCALAPPDATA%\JarvisFernsteuerung lesen. Grenzen: gesperrte Sitzung und Neustart ohne Anmeldung nicht aus der Ferne lösbar. 130-MB-APK über Mobilfunk-Tunnel bricht oft ab → UpdateStation nutzen.
