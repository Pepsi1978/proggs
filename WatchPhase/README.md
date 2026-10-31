# Watch Phase und Klassiker

Eigene Zifferblätter für die Huawei Watch 3. Alle vier stehen seit dem 31.10.2026 auf der Uhr.

- Watch Phase: Nachbau des Zifferblatts der Huawei Watch GT, schwarz-weiß mit rotem Sekundenzeiger. `dist/WatchPhase.hwt`, gebaut mit `bauen.py`.
- SLAVA, EIGER SPEED EDITION und BERING: nach Fotos von Franks Uhren. `dist/Slava.hwt`, `dist/EigerSpeedEdition.hwt`, `dist/Bering.hwt`, gebaut mit `klassiker.py`. „21 Jewels“ bei SLAVA ist auf dem Foto teils verdeckt und daher gelesen, nicht sicher.

## Auf die Uhr bringen

Die Uhr bleibt am Handy mit Huawei Health gekoppelt. Sie dort nie entkoppeln und nie löschen, sonst wird der Schlüssel ungültig und Daten gehen verloren.

Den geprüften Ablauf per adb beschreibt `best-practices/peripherie/huawei-watchface.md`, Abschnitt 0. Gadgetbridge liegt auf dem Handy deaktiviert bereit und kennt die Uhr schon. Für eine neue Fassung reicht:

1. Health `BLUETOOTH_CONNECT` und `BLUETOOTH_SCAN` entziehen und beenden.
2. Gadgetbridge aktivieren und verbinden.
3. Die alte Fassung im App-Manager löschen, die neue `.hwt` installieren und aktivieren.
4. Gadgetbridge beenden und deaktivieren, Health die Rechte zurückgeben und dort „Verbinden“ tippen.
