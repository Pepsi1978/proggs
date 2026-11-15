# SwitchBot – Kurzcheck (Stand 15.11.2026)
- S1 Cloud-Status 100 mit lauter Nullen = keine Daten, nie als Wert zeigen.
- S2 enableCloudService/hubDeviceId 000000000000: per API nicht änderbar; Status trotzdem fragen.
- S3 Meter Pro CO2 am klassischen Hub Mini ohne Cloud-Werte: Hub 10 min stromlos → gelöst (Webhook kam sofort). BLE-Mithören als Rückfall.
- S6 Hub aus, Cloud liefert alte Werte ohne Fehler: frische gemessene Werte vorziehen.
- S4 Meter Pro CO2 sendet selten: bis 40 s lauschen.
- S5 „20 Tage nicht synchronisiert“ = nur Verlauf, nicht Live-Werte.
Volltext: bugs/peripherie/switchbot.md
