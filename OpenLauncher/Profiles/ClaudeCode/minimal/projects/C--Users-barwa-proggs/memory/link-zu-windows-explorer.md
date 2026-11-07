---
name: link-zu-windows-explorer
description: "Fold 8 im Datei-Explorer per „Link zu Windows“ eingerichtet (16.10.2026); nie force-stoppen, Start über StartUpActivity"
metadata:
  node_type: memory
  type: project
  originSessionId: 75028184-0c2a-4a86-82cb-cb78b6c166e6
  modified: 2026-10-16T09:59:06.248Z
---

Das Z Fold 8 hängt seit 16.10.2026 über „Link zu Windows“ (com.microsoft.appmanager) im Windows-Explorer (Mobile Geräte → „Im Datei-Explorer anzeigen“). Dateien laufen direkt übers WLAN (pktmon-Messung: ~600 MB direkt von 192.168.0.155), nicht über Microsoft-Server; ca. 25 MB/s. Heim-WLAN am PC dafür auf „Privat“ gestellt.

**Why:** Die App hat keinen Launcher. Nach `am force-stop` startet sie nicht von selbst neu, und der PC meldet „offline“. Die Anmeldung per E-Mail am Handy hing; gekoppelt wurde per QR-Code aus Smartphone-Link.

**How to apply:** Die App nie per force-stop oder `pm clear` anfassen (pm clear löscht die Kopplung). Bei „offline“ zuerst prüfen, ob der Prozess läuft (`pidof com.microsoft.appmanager`), dann per `am start -n com.microsoft.appmanager/.StartUpActivity` starten. Betrifft adb/WLAN-Installation nicht, siehe [[adb-wlan-selbstheilend]].
