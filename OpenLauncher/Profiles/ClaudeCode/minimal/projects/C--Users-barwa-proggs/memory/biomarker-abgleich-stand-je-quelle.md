---
name: biomarker-abgleich-stand-je-quelle
description: "Biomarker in Entropie Reductor alt oder fehlen – zuerst Diagnose-Protokoll und Abgleich-Stand je Quelle prüfen, Whoop-Refresh-Token geht bei Netzabriss verloren"
metadata:
  node_type: memory
  type: project
  originSessionId: 9beacc4e-bb02-4fe0-8117-a663f08e6b75
  modified: 2026-10-15T14:11:15.688Z
---

Fehlen in Entropie Reductor oder bei Jarvis aktuelle Biodaten: zuerst `adb pull /sdcard/Android/data/de.frank.entropyreducer.debug/files/diagnostics/` und nach `ForegroundSync`, `OAuthService`, `WHOOP`, `OURA` suchen. Der Abgleich bewertet jede Quelle einzeln (`ApiSyncBericht` in `ForegroundSyncManager`), die Jarvis-Brücke liefert den Zustand unter `stand` (seit 15.10.2026 auch `stand.whoop` mit Schlafende, Vollständigkeit, nächstem Abruf).

Seit 15.10.2026 entscheidet `WhoopAbrufPlan` allein, wann Whoop gefragt wird (neuer Schlaf möglich / neuester Schlaf noch nicht SCORED, Tagesbudget, Backoff, 429-Sperre). Jarvis' Tagesauswertung gleicht nicht mehr ab; Brücke `abgleich` mit `max_alter_min` lässt Whoop aus.

**Why:** 09.10. und 15.10.2026: Beim Whoop-Token-Refresh im Hintergrund riss die Verbindung beim Lesen der Antwort ab („Software caused connection abort"); Whoop hatte den Refresh-Token schon getauscht, danach `invalid_request`. Am 15.10. wurde zudem der Worker mitten im Refresh abgebrochen, die Wiederholung lief nie. Das heilt nur eine neue Anmeldung (Einstellungen → API-Schlüssel), kein Code.

**How to apply:** Muster „ein Netzfehler beim Refresh, danach dauerhaft OAuth-Fehler" = Token verbraucht, Frank neu anmelden lassen. Echte Refreshes nur an `needsRefresh=true` zählen. Die Protokollzeile „Antwort des Refreshs von … wurde nie gespeichert" belegt einen verlorenen Refresh. Liefert Oura trotz geglücktem Abruf nichts Neues, hat der Ring nicht mit der Oura-App abgeglichen. Einzelheiten: `bugs/android/retrofit-okhttp-moshi.md`, letzter Abschnitt. Siehe auch [[jarvis-relay-und-chatgpt-plugin]].
