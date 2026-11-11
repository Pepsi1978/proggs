---
name: jarvis-tablet-zweites-geraet
description: "Galaxy Tab S9 ist „Jarvis Tablet“; Geräteauswahl über geraete.json, Abgleich Handy↔Tablet über Jarvis, Stolperfallen"
metadata:
  node_type: memory
  type: project
  originSessionId: 966a4cf2-d8e6-40cd-8edb-2a66d53ed064
  modified: 2026-11-11T16:42:33.900Z
---

Seit 10.11.2026 läuft Jarvis zusätzlich auf dem Galaxy Tab S9 (SM-X716B, Serie R52W800205N, Tunnel 10.8.0.8) als „Jarvis Tablet“; das Handy ist das Fold 8 (Serie R3GL7073MLM, Tunnel 10.8.0.7). Dieselbe APK, erkannt am Modell (`Jarvis/app/.../data/Geraet.kt`). Doku: Jarvis/README.md Abschnitt „Jarvis Tablet (zweites Gerät) und Geräteabgleich“.

**How to apply:**
- Geräte immer über `Werkzeuge/adb-wlan/geraete.json` wählen (`-Geraet`/`-Ziel tablet`, `--geraet`, `HANDY_GERAET=tablet`); ohne Angabe ist das Handy gemeint. Beide können gleichzeitig verbunden sein.
- JEDE App auf BEIDE Geräte installieren (Franks Vorgabe 11.11.2026: Tablet wie Handy per WLAN/Tunnel, nicht nur über UpdateStation, auch Genialer Wecker = Paket de.frank.genialerwecker, gemeinsamer Key; die Play-Fassung GenialerWeckerAndroid hat einen eigenen Key): `Werkzeuge/adb-wlan/auf-beide.sh <apk…>` (Windows: Git Bash, nie WSL-bash; lehnt Jarvis-APKs ab), Jarvis mit `jarvis-install -Ziel handy` und `-Ziel tablet`.
- `bruecke/GeraeteAbgleich.kt` ist in den fünf Apps byte-gleich (bis auf package): Änderungen in allen fünf nachziehen. Die Inhalts-Prüfsumme (`inhalt()`) nicht ändern, ohne dass danach das Handy einen neuen Stand hochlädt (sonst hält Jarvis den Abgleich der App an, „gesperrt“).
- Nach einem Jarvis-Update per adb startete der Prozess am Handy nicht immer von selbst; `jarvis-install` weckt ihn seit 1.8.606 selbst (`am broadcast -a de.frank.jarvis.AUFWACHEN -p de.frank.jarvis`).
- Abgleich testen ohne Jarvis-Werkzeuge (deren Antwort hängt nach jedem Jarvis-Neustart rund 35.000 Zeichen Kontext an): am Handy `run-as <paket> touch databases/<db>` und dann das Relay-Signal senden.
- Abgleich beobachten: Logbuch-Ereignis „Geräteabgleich <App>“, Zustand `files/geraeteabgleich.json` (run-as); Runde anstoßen über Relay-Signal `{"signal":"geraeteabgleich","geraet":"handy"}` an `/geraet/signal`.
- Franks Entscheidungen vom 10.11.2026: Der Geniale Wecker klingelt nur am Handy und wird nicht abgeglichen; das ChatGPT-Plugin gibt es nur für das Handy (am Tablet ausgeblendet). Jarvis' Gedächtnis und Gespräche gleichen sich ab 1.8.606 ab (`abgleich/GedaechtnisAbgleich.kt`), Programmier-Gespräche bleiben je Gerät.
