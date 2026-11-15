# SwitchBot Cloud-API und Bluetooth (Meter, Meter Pro CO2, Hub Mini)

> Stand: 15.11.2026 19:22. Selbst erlebt mit Hub Mini (klassisch, W0202200, Firmware V6.8), Meter Pro CO2 (W4900010, FW V1.9),
> Outdoor-Meter (WoIOSensor), SwitchBot OpenAPI v1.1, bleak auf macOS 27. Code: `Jarvis/.../faehigkeit/SwitchbotFaehigkeit.kt`,
> `Jarvis/werkzeuge/pc-bruecke/jarvis_switchbot.py`. Best Practices: `best-practices/peripherie/switchbot.md`.

## ⚡ Kurzcheck

| # | Signal | Sofort-Regel |
|---|--------|--------------|
| S1 | Status `statusCode 100`, aber temperature/humidity/CO2/battery alle 0 | Platzhalter, die Cloud hat keine Daten (Gerät nicht über Hub angebunden). Nie als Messwert zeigen |
| S2 | Geräteliste `enableCloudService:false`, `hubDeviceId "000000000000"` | Kein Hub-Weg; per API nicht einschaltbar. Trotzdem Status fragen, Flag kann veraltet sein |
| S3 | App: „Hub offline oder Meter Pro nicht angeschlossen“, obwohl Gerät unter Manage Hubs steht | Hub Mini reicht den Meter Pro CO2 nicht weiter; Rückfall: Werbepakete per Bluetooth mithören |
| S4 | macOS-Scan findet Meter Pro CO2 nur selten | Sendet Messpakete nur alle ~6–30 s: bis 40 s lauschen, beim ersten Treffer aufhören |
| S5 | „Daten in den letzten 20 Tagen nicht synchronisiert“ | Betrifft nur den Verlauf (Handy lädt ihn per BLE), nicht die Live-Werte über den Hub |

## Volltext

### S1 Nullwerte trotz Erfolg
Symptom: GET /v1.1/devices/{id}/status liefert 100 und lauter Nullen. Ursache: kein Hub-Datenweg (Maintainer in
https://github.com/OpenWonderLabs/SwitchBotAPI/issues/371), offene Fälle #448 (Hub 3) und #484 (Hub Mini + Outdoor-Meter).
Fix: 0 °C und 0 % zusammen als „keine Daten“ werten (Feuchte 0 % ist physikalisch unmöglich). Betroffen: API v1.1, Stand 11/2026.

### S2 Cloud-Schalter
Seit App V9.0 gibt es keinen „Cloud Service“-Schalter mehr; Anbindung automatisch über die Nähe zum Hub (≤ 5 m), Verlauf über
„Daten in der Cloud speichern“. Kein API-Endpunkt zum Einschalten (#195 zeigt zudem veraltetes Flag).

### S3 Meter Pro CO2 am klassischen Hub Mini
Offiziell kompatibel (support.switch-bot.com Artikel 26500881438999), in der Praxis hier trotz Zuordnung keine Cloud-Werte.
Rückfall, der sicher geht: Rechner zu Hause hört die Herstellerdaten 0x0969 mit (siehe Best Practices), meldet an eigenen Server.
