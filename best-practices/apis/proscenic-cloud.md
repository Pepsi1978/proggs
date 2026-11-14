# Proscenic-Cloud (Saugroboter M7 Pro) — Best Practices (Stand 2026-11-14)

> Gegenstück zu `bugs/apis/proscenic-cloud.md`. Extern (Reverse Engineering). Versions-Anker: Proscenic-App `com.proscenic.robot` 1.5.5 (ProscenicHome-Legacy-Cloud, EU), M7 Pro Modell `811_LDS`, Jarvis 1.8.676 (am Gerät verifiziert 14.11.2026: Status, Positionieren, Tiefenreinigung, Station, Stopp).

## ⚡ Kurzcheck (Stufe A)

| # | Situation | Best Practice | Volltext |
|---|-----------|---------------|----------|
| 1 | Fremd-App steuern | Direkt die Cloud-API nehmen statt UI-Automation: unsichtbar, kein Netzverbot im Hintergrund | Warum Cloud |
| 2 | Anmeldung | `POST /user/login` (JSON, `password`=MD5-hex, `pwd`=Klartext, iOS-Header); Erfolg an `data.token` + `equipcount` >= 1 prüfen, nicht an `code 0` | Ablauf |
| 3 | Token | speichern, nur bei `code 102`/HTTP 401/403 neu einloggen (jeder Login stellt neuen Token aus) | Ablauf |
| 4 | Befehle | Formular-POST `/instructions/...` mit Header `token` | Befehle |
| 5 | Status | TCP-Gateway aus `/appInit/getSockAddr`, 70001-Handshake, AES-128-ECB (Schlüssel = Token), Rahmenende `#\t#`, infoType 20001 | Status |
| 6 | Parallel zur App | Je Konto nur EINE aktive Anmeldung (App-Login macht Fremd-Token ungültig: `code 102` „token过期“, und umgekehrt). Eigenes Zweitkonto nehmen, Gerät in der App per „Gerät teilen“/„Kontenfreigabe“ freigeben (in App 1.5.5 vorhanden: `AddShareForUserActivity`, `/app/cleanRobot/share/...`) | Ablauf |

## Warum Cloud
UI-Automation per Bedienungshilfe braucht die Proscenic-App sichtbar im Vordergrund (Android 15+ sperrt Apps im Hintergrund das Netz, `BLOCKED_REASON_APP_BACKGROUND`, ~5 s nach Wechsel). Eine fremde Activity auf einer eigenen VirtualDisplay starten verbietet Android ab 10. Lokal hat der M7 Pro keine offenen Ports (gemessen 14.11.2026). Bleibt: die Cloud-API.

## Ablauf (Host `https://mobile.proscenic.com.de`)
1. `POST /user/login`, Body JSON `{"state":"欧洲","countryCode":"49","appVer":"1.7.8","type":"2","os":"IOS","password":"<md5>","registrationId":"13165ffa4eb156ac484","language":"EN","username":"<mail>","pwd":"<klartext>"}`, Header `os: i`, `c: 338`, `lan: en`, `v: 1.7.8`, `User-Agent: ProscenicHome/1.7.8 (iPhone; iOS 14.2.1; Scale/3.00)`. Antwort `data.token` (16 Zeichen), `data.equipcount`.
2. `POST /user/getEquips/<mail>` (Form `username`, Header `token`) → `data.content[]`, Roboter mit `typeName == "CleanRobot"` → `sn`.

## Befehle (Form-POST, Header `token`, Query `?username=<mail>`)
| Aktion | Pfad | Feld |
|---|---|---|
| Start | `/instructions/cmd21005/{sn}` | `cleanMode=sweepOnly` |
| Tiefenreinigung | `/instructions/cmd21005_2/{sn}` | `mode=depthTotalClean` |
| Pause / Weiter | `/instructions/{sn}/21017` | `mode=pause` / `pauseOrContinue=continue` |
| Station | `/instructions/{sn}/21012` | `charge=start` |
| Orten | `/instructions/{sn}/21020` | `ctrlCode=3010` (Cloud nimmt an, Code 0) |
| Saugstufe | `/instructions/{sn}/21022` | `setMode=quiet|auto|strong` |
| Staubsammlung | `/instructions/cmd/{sn}` | Rohbody `{"dInfo":{"userId":"<mail>","ts":"<ms>"},"data":{"cmd":"startDustCenter","value":0},"infoType":21024}` |
| Y-Wischen | unbekannt (Hypothese: 21024 `setPathType` value 1) | — |

## Status
`POST /appInit/getSockAddr` (Form `username`, `sn`) → `data.addr_list[0].ip/port`. TCP verbinden, senden `{"data":{"token":"…","sn":"…"},"infoType":70001}#\t#`. Antworten `{"encrypt":1,"data":"<base64>"}` → AES/ECB/PKCS5Padding mit Token-Bytes. infoType 20001: `mode` (charge, fullcharge, sweep, pause, backcharge, DustCenterWorking, idle, fault), `elec` (Akku %), `workNoisy` (quiet/auto/strong). Nach dem ersten 20001 Socket schließen (verdrängt sonst die App).

## Quellen
- https://github.com/Musashi1965/ioBroker.proscenic (0.4.0, am M7 Pro validiert)
- https://github.com/JuliusBlueTek/Proscenic-Home-Assistant (proscenicapis.py)
- https://github.com/Fluepke/proscenic · https://github.com/andker87/Proscenic-M7-PRO
- https://github.com/jonny-samson/ha-lenovo-t1s-vacuum/blob/main/docs/PROTOCOL.md (infoType-/ctrlCode-Tabelle)
- Umsetzung: `Jarvis/app/src/main/java/de/frank/jarvis/staubsauger/ProscenicCloud.kt`
