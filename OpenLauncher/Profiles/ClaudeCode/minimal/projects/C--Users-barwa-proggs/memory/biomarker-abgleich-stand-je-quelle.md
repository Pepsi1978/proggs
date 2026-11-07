---
name: biomarker-abgleich-stand-je-quelle
description: "Biomarker in Entropie Reductor/Jarvis alt oder fehlen – zuerst Diagnose-Protokoll und Abgleich-Stand je Quelle prüfen; Whoop-Refresh-Token geht bei Netzabriss verloren"
metadata:
  node_type: memory
  type: project
  originSessionId: 9beacc4e-bb02-4fe0-8117-a663f08e6b75
  modified: 2026-10-15T14:11:15.688Z
---

Fehlen in Entropie Reductor oder bei Jarvis aktuelle Biodaten: zuerst `adb pull /sdcard/Android/data/de.frank.entropyreducer.debug/files/diagnostics/`, nach `ForegroundSync`, `OAuthService`, `WHOOP`, `OURA` suchen. Abgleich bewertet jede Quelle einzeln (`ApiSyncBericht` in `ForegroundSyncManager`); Jarvis-Brücke liefert Zustand unter `stand` (seit 15.10.2026 auch `stand.whoop`: Schlafende, Vollständigkeit, nächster Abruf).

Seit 15.10.2026 entscheidet allein `WhoopAbrufPlan`, wann Whoop gefragt wird (neuer Schlaf möglich / neuester Schlaf noch nicht SCORED, Tagesbudget, Backoff, 429-Sperre). Jarvis' Tagesauswertung gleicht nicht mehr ab; Brücke `abgleich` mit `max_alter_min` lässt Whoop aus.

**Why:** 09.10. und 15.10.2026: Beim Whoop-Token-Refresh im Hintergrund riss die Verbindung beim Lesen der Antwort ab („Software caused connection abort“); Whoop hatte den Refresh-Token schon getauscht → danach `invalid_request`. 15.10. zudem Worker mitten im Refresh abgebrochen, Wiederholung lief nie. Heilt nur neue Anmeldung (Einstellungen → API-Schlüssel), kein Code.

**How to apply:**
- „Ein Netzfehler beim Refresh, danach dauerhaft OAuth-Fehler“ = Token verbraucht → Frank neu anmelden lassen.
- Echte Refreshes nur an `needsRefresh=true` zählen.
- Protokollzeile „Antwort des Refreshs von … wurde nie gespeichert“ belegt verlorenen Refresh.
- Oura liefert trotz geglücktem Abruf nichts Neues → Ring hat nicht mit der Oura-App abgeglichen.
- Einzelheiten: `bugs/android/retrofit-okhttp-moshi.md`, letzter Abschnitt. Siehe auch [[jarvis-relay-und-chatgpt-plugin]].
