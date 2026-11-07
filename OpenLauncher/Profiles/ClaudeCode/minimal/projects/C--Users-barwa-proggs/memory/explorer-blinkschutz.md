---
name: explorer-blinkschutz
description: "Symbole im Explorer/Desktop blinken 2x pro Sekunde - Ursache leerer UserChoice-Schattenschlüssel in der privaten Registry einer Store-App (Lively), Wächter-Aufgabe heilt selbst"
metadata:
  node_type: memory
  type: project
  originSessionId: a5948f05-d18c-4e4d-b670-718e85abc820
  modified: 2026-10-08T20:27:45.940Z
---

Alle Symbole in Explorer und Desktop blinken etwa zweimal pro Sekunde = Sturm von Standard-App-Resets (Ereignis 62441 im Protokoll `Microsoft-Windows-Shell-Core/AppDefaults`). Am 08.10.2026 dreimal. Ursache: leerer Schlüssel `FileExts\.pdf\UserChoice` in Livelys privater Paket-Registry (`%LOCALAPPDATA%\Packages\<Paket>\SystemAppData\Helium\User.dat`), der die echte, gültige Zuordnung verdeckte. Fix: App beenden, `User.dat` beiseitelegen, App starten.

Seit 08.10.2026 läuft die Aufgabe „Explorer-Blinkschutz" (`~/proggs/Werkzeuge/explorer-blinkschutz/`, Log in `%LOCALAPPDATA%\explorer-blinkschutz\`): erkennt den Sturm, beendet das Wirtsprogramm, legt bei Store-Apps die private Registry-Datei beiseite, startet die App neu (Echttest bestanden).

**Why:** Zwei Fixes am selben Tag griffen zu kurz (erst nur Lively beendet, dann die echte `.pdf`-Zuordnung repariert) – Lively sah die echte Zuordnung gar nicht.

**How to apply:** Blinkt es trotzdem: erst Wächter-Log, dann `bugs/desktop/windows-explorer-blinken-assocchanged.md`. Bei Store-Apps die Registry-Sicht aus dem Paket prüfen (`Invoke-CommandInDesktopPackage`), nicht nur die echte Registry. Nie per Skript an `UserChoice` schreiben (UCPD blockt).
