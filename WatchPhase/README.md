# Watch Phase

Nachbau des Zifferblatts der Huawei Watch GT für die Watch 3. Fertige Datei: `dist/WatchPhase.hwt`.

## Auf die Uhr bringen

Die Uhr bleibt am Handy mit Huawei Health gekoppelt. Alle Schritte laufen auf diesem Handy. Die Uhr dort nie entkoppeln und nie löschen, sonst wird der Schlüssel ungültig und Daten gehen verloren.

1. Am PC bei Huawei Cloud anmelden. In den Browser-Entwicklertools unter den Cookies den Wert `userId` kopieren, eine 17-stellige Zahl.
2. Gadgetbridge aus F-Droid installieren und `WatchPhase.hwt` aufs Handy legen.
3. Huawei Health alle Berechtigungen entziehen und die App beenden.
4. In Gadgetbridge nach neuen Geräten suchen, die Uhr lange drücken, „Auth key“ wählen und die Zahl eintragen. Danach verbinden.
5. `WatchPhase.hwt` im Dateimanager mit Gadgetbridge öffnen und die Installation bestätigen.
6. Auf der Uhr das Zifferblatt lange drücken und „Watch Phase“ auswählen.
7. In Gadgetbridge die Verbindung trennen, Huawei Health die Berechtigungen zurückgeben und die App öffnen.

Anleitung von Gadgetbridge: https://gadgetbridge.org/basics/pairing/huawei-honor-pairing/

Der Weg ist laut Gadgetbridge vorgesehen, aber auf einer Watch 3 mit dieser Datei noch nicht getestet. Die Rückkehr zu Huawei Health ist dort nur als „should be able“ beschrieben.
