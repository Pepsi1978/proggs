---
name: jarvis-tablet-zweites-geraet
description: "Galaxy Tab S9 ist „Jarvis Tablet“; Geräteauswahl über geraete.json, Abgleich Handy↔Tablet über Jarvis, Stolperfallen"
metadata:
  node_type: memory
  type: project
  originSessionId: 966a4cf2-d8e6-40cd-8edb-2a66d53ed064
  modified: 2026-11-10T14:48:10.478Z
---

Seit 10.11.2026 läuft Jarvis zusätzlich auf dem Galaxy Tab S9 (SM-X716B, Serie R52W800205N, Tunnel 10.8.0.8) als „Jarvis Tablet“; das Handy ist das Fold 8 (Serie R3GL7073MLM, Tunnel 10.8.0.7). Dieselbe APK, erkannt am Modell (`Jarvis/app/.../data/Geraet.kt`). Doku: Jarvis/README.md Abschnitt „Jarvis Tablet (zweites Gerät) und Geräteabgleich“.

**How to apply:**
- Geräte immer über `Werkzeuge/adb-wlan/geraete.json` wählen (`-Geraet`/`-Ziel tablet`, `--geraet`, `HANDY_GERAET=tablet`); ohne Angabe ist das Handy gemeint. Beide können gleichzeitig verbunden sein.
- Jarvis und die fünf abgeglichenen Apps (Aufgaben, Ziele, GenialeIdeen, Epigenetik, StackLabor) auf BEIDE Geräte installieren: `Werkzeuge/adb-wlan/auf-beide.sh <apk…>`, Jarvis mit `jarvis-install -Ziel handy` und `-Ziel tablet`.
- `bruecke/GeraeteAbgleich.kt` ist in den fünf Apps byte-gleich (bis auf package): Änderungen in allen fünf nachziehen. Die Inhalts-Prüfsumme (`inhalt()`) nicht ändern, ohne dass danach das Handy einen neuen Stand hochlädt (sonst hält Jarvis den Abgleich der App an, „gesperrt“).
- Nach einem Jarvis-Update per adb startet der Prozess am Handy nicht immer von selbst (am 10.11. beobachtet): danach prüfen, sonst mit `adb shell am broadcast -a de.frank.jarvis.EINSTELLUNGEN_UMZUG -p de.frank.jarvis` wecken und `files/umzug-export.json` per run-as löschen.
- Abgleich testen ohne Jarvis-Werkzeuge (deren Antwort hängt nach jedem Jarvis-Neustart rund 35.000 Zeichen Kontext an): am Handy `run-as <paket> touch databases/<db>` und dann das Relay-Signal senden.
- Abgleich beobachten: Logbuch-Ereignis „Geräteabgleich <App>“, Zustand `files/geraeteabgleich.json` (run-as); Runde anstoßen über Relay-Signal `{"signal":"geraeteabgleich","geraet":"handy"}` an `/geraet/signal`.
- Offen (Stand 10.11.2026): Genialer Wecker wird nicht abgeglichen (Franks Entscheidung fehlt, ob er auf beiden klingeln soll); Entropie Reductor und Best Journal (Debug) liegen auf dem Tablet in alten Fassungen mit unbekanntem Schlüssel (kein Umzug per Rotation möglich, Deinstallation nur nach Franks Ja).
