---
name: heim-wlan-in-sk
description: "Heim-WLAN (SSID + Passwort) liegt in ~/SK/Zuhause/wlan.json; bei Geräte-Einrichtung von dort nehmen, nie Frank fragen"
metadata:
  type: reference
---

Franks Standard-WLAN zu Hause steht in `~/SK/Zuhause/wlan.json` (`ssid`, `passwort`, `band`, `verschluesselung`): „Tenda 2.4 Ghz“, WPA2/WPA3-Personal.

**Why:** Frank am 13.11.2026: Bei neuer Hardware oder falsch eingetragenem WLAN soll Claude Netz und Passwort selbst eintragen, ohne nachzufragen. 2,4 GHz mit Absicht, weil viele Smart-Home-Geräte kein 5 GHz können.

**How to apply:**
- Bei „verwende dieses WLAN“, Smart-Home-, Hub-, Sensor-, Drucker- oder Handy-Einrichtung die Datei lesen und Netz und Passwort eintragen.
- Passwort nie ins Repo, in Commits, Logs oder Gedächtnis schreiben; nur aus SK lesen.
- Am Mac und auf dem Server liegt die Datei nur, wenn Frank sie dort nachgelegt hat (SK wird nicht abgeglichen). Fehlt sie: sagen, nicht raten.
