# Bekannte Bugs: Android Voice-Assistant-Auslösung + Wake-Word + Mikrofon

> **PFLICHT-LESEN vor Arbeit an:** App, die einen Sprachassistenten per Weckwort/aus Code
> ausloest, Wake-Word-Daueraufnahme, Foreground-Mic-Service, Mikrofon-Uebergabe zwischen Apps,
> Shizuku/KEYCODE_ASSIST, Default-Assistant-Integration.
>
> **Stand:** 2026-06-11 (7-Researcher-Schwarm, offizielle Quellen + AOSP zuerst).
> **Zielgeraet-Anker:** Samsung Galaxy S23 Ultra, One UI 6.1.1 / Android 14 (API 34).
> ChatGPT-App ab v1.2025.070. Wake-Word: sherpa-onnx KWS / openWakeWord.
>
> **Zweite Seite (Praevention):** [`best-practices/projekt-code/android/best-practices-voice-assistant-trigger.md`](../../best-practices/projekt-code/android/best-practices-voice-assistant-trigger.md).
> **Abgrenzung:** Allgemeines Service/Permission/Lifecycle-Verhalten → [`android-platform.md`](android-platform.md).
> Hier nur das Spezial-Thema „fremden Assistenten per Weckwort ausloesen + Mic-Handoff".

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

| # | Signal / Situation | Sofort-Regel | Volltext |
|---|--------------------|--------------|----------|
| 1 | `startActivity(ACTION_ASSIST)` soll Default-Assistant starten | Unzuverlaessig, kein garantiertes Routing, One UI eingeschraenkt — nicht als Hauptweg | §1 |
| 2 | App will `KEYCODE_ASSIST` / Key-Event injizieren | Geht NICHT (INJECT_EVENTS = signature\|privileged). Nur Shell-UID/Shizuku | §2 |
| 3 | Shizuku als No-Root-Weg geplant | One UI 6.1.1 stoppt Shizuku beim Sperren (#612); ohne Root nach Reboot manuell neu starten | §3 |
| 4 | `startForeground` wirft `MissingForegroundServiceTypeException` | `foregroundServiceType="microphone"` + `FOREGROUND_SERVICE_MICROPHONE` (API 34) | §4 |
| 5 | `SecurityException` beim Service-Start | FGS mit Mic nicht aus dem Hintergrund starten — aus Vordergrund/Notification/Akku-Ausnahme | §5 |
| 6 | Dienst stirbt nach Minuten/Stunden (Samsung) | Akku „Uneingeschraenkt" + aus Tiefschlaf-Liste + Adaptiver Akku AUS | §6 |
| 7 | Weckwort wird kaum erkannt | EN-Gigaspeech-Modell schwach — Threshold/Boosting tunen, Modell wechseln, live testen | §7 |
| 8 | Folge-App (ChatGPT) bekommt Mic nicht / Init-Fehler | Nach `release()` 300–500 ms warten (HAL-Flush ab Android 16 async) | §8 |
| 9 | ChatGPT bekommt nur Stille | `setPrivacySensitive(true)` auf Wake-Word-App entfernen | §9 |
| 10 | Suche nach `chatgpt://`-Voice-Deep-Link | Existiert offiziell nicht; kein Hotword — nur Assist-Geste/UI | §10 |

---

## §1 — `ACTION_ASSIST`-Intent aus App ist unzuverlaessig
**Symptom:** `startActivity(new Intent(Intent.ACTION_ASSIST))` startet den eingestellten Assistenten
nicht oder nicht zuverlaessig (besonders Samsung One UI).
**Root Cause:** `ACTION_ASSIST` ist primaer eine Intent-Filter-Aktion, mit der sich Apps ALS Assistent
deklarieren — kein garantierter Ausloese-Mechanismus. Die echte Geste laeuft ueber den privilegierten
SystemUI-/Assist-API-Pfad (`onProvideAssistData`, `SHOW_SOURCE_ASSIST_GESTURE`), den eine App nicht erreicht.
**Fix (funktionserhaltend):** Nicht auf den generischen Intent setzen. Stattdessen Accessibility-UI-Klick
oder Shizuku-keyevent (§2/§3) verwenden; bei bekanntem Ziel ggf. explizite Component der Assistant-App.
**Versionen:** alle; One UI besonders restriktiv. **Quelle:** developer.android.com/develop/devices/assistant.

## §2 — `KEYCODE_ASSIST` / Key-Injection aus App unmoeglich
**Symptom:** `SecurityException: Injecting to another application requires INJECT_EVENTS permission`.
**Root Cause:** Globale Key-Injection braucht `android.permission.INJECT_EVENTS`, Schutzlevel
**`signature|privileged`** — nur Plattform-signierte/privilegierte Apps. `Instrumentation` ist auf die
eigene UID beschraenkt; Accessibility kann nur Back/Home/Recents + UI-Klicks, KEINE beliebigen Keyevents.
**Fix:** Einziger No-Root-Weg ist ein Prozess mit shell-UID (2000) → Shizuku, der `input keyevent 219`
bzw. `IInputManager.injectInputEvent()` ausfuehrt. Sonst Accessibility-UI-Automation.
**Versionen:** alle. **Quelle:** developer.android.com/reference/android/view/KeyEvent; AOSP platform.xml.

## §3 — Shizuku nicht persistent (One UI 6.1.1 stoppt beim Sperren) ⚠️ HAUPTFALLE
**Symptom:** Shizuku-basierte Ausloesung funktioniert kurz, dann nicht mehr — besonders nach
Bildschirm-Sperre oder Reboot.
**Root Cause:** Ohne Root laeuft Shizuku nur, solange der per ADB/Wireless-Debugging gestartete Prozess
lebt. **Auf One UI 6.1.1 wird Shizuku beim Sperren/Entsperren gestoppt** (GitHub Issue #612); nach jedem
Reboot manueller Neustart noetig.
**Fix (funktionserhaltend):** Fuer eine Dauer-App (24/7-Weckwort) Shizuku NICHT als Auslöse-Weg waehlen,
sondern Accessibility-Service (ueberlebt Reboot/Sperren). Shizuku nur bei gerooteten Geraeten oder wenn
der Nutzer die Reboot-Fummelei bewusst akzeptiert. Knox wird durch Shizuku nicht getrippt.
**Versionen:** One UI 6.1.1 / Android 14 (Zielgeraet). **Quelle:** github.com/RikkaApps/Shizuku/issues/612.

## §4 — `MissingForegroundServiceTypeException` (Android 14)
**Symptom:** Crash beim `startForeground` eines Mic-Dienstes ab API 34.
**Root Cause:** Android 14 verlangt `android:foregroundServiceType` im Manifest UND die passende
Typ-Permission.
**Fix:** `android:foregroundServiceType="microphone"` am `<service>` + `<uses-permission
android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE"/>`; `startForeground` mit
`ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE`.
**Versionen:** API 34+. **Quelle:** developer.android.com/about/versions/14/changes/fgs-types-required.

## §5 — `SecurityException`: FGS-Mic aus dem Hintergrund gestartet
**Symptom:** Service-Start schlaegt fehl, wenn aus Hintergrund/BOOT_COMPLETED getriggert.
**Root Cause:** RECORD_AUDIO ist „while-in-use" — ein Mic-FGS darf nicht aus dem Hintergrund starten.
**Fix:** Aus dem Vordergrund starten (sichtbare Activity / Notification-Interaktion / App-Widget), oder
Akku-Optimierung ausnehmen (erlaubt FGS-Start aus Hintergrund). Nach Reboot Notification „Tippen zum
Aktivieren" statt Direktstart.
**Versionen:** Android 12+. **Quelle:** developer.android.com/develop/background-work/services/fgs/restrictions-bg-start.

## §6 — Samsung One UI killt den Hintergrund-Mic-Dienst
**Symptom:** Wake-Word-Dienst stirbt nach Minuten/Stunden oder nach Inaktivitaet, obwohl FGS korrekt.
**Root Cause:** Aggressive One-UI-Akku-Optimierung (Tiefschlaf, „Apps schlafen legen", adaptiver Akku).
**Fix:** In-App-Setup-Screen, der den Nutzer fuehrt: App-Akku „Uneingeschraenkt"; aus „Apps im
Ruhezustand/Tiefschlaf" entfernen; Adaptiver Akku AUS; „Nicht genutzte Apps schlafen legen" AUS;
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. `START_STICKY` + `onTaskRemoved`-Restart + WorkManager-Watchdog.
**Versionen:** One UI (alle). **Quelle:** dontkillmyapp.com/samsung.

## §7 — Wake-Word wird kaum erkannt (schwache Aufweck-Rate)
**Symptom:** Weckwort muss mehrfach/laut gesagt werden; sherpa-onnx weckt selten auf.
**Root Cause:** Das EN-Gigaspeech-KWS-Modell zeigt in mehreren Issues sehr niedrige Wake-Raten (<10 %,
vs. Wenetspeech >90 %). Default-Threshold passt nicht fuer jedes Wort.
**Fix:** `keywords_threshold` senken + `keywords_score` (Boosting) erhoehen; Aufweck-Rate LIVE testen;
ggf. Modell wechseln oder auf openWakeWord (eigenes Training) ausweichen. Silero-VAD vorschalten.
**Versionen:** sherpa-onnx (Modell-abhaengig). **Quelle:** github.com/k2-fsa/sherpa-onnx/issues/2678.

## §8 — Folge-App bekommt Mic nicht (HAL-Flush nicht synchron)
**Symptom:** Nach Wake-Word startet ChatGPT, aber dessen Mic-Init schlaegt fehl / es kommt kein Ton.
**Root Cause:** `AudioRecord.release()` ist am Hardware-Level (ab Android 16) NICHT synchron — die HAL
braucht Zeit, um Buffer zu flushen und den nativen Mic-Lock freizugeben.
**Fix:** Nach `stop()` + `release()` **300–500 ms warten**, BEVOR die andere App das Mic anfordert bzw.
die Geste ausgeloest wird. Reihenfolge strikt sequentiell, nie parallel.
**Versionen:** verschaerft ab Android 16; gute Praxis generell. **Quelle:** Android-16-Mic-Init-Reports;
github.com/Picovoice/porcupine/issues/87.

## §9 — ChatGPT bekommt nur Stille (`setPrivacySensitive`)
**Symptom:** ChatGPT-Voice startet, hoert aber nichts.
**Root Cause:** Die Wake-Word-App hat `AudioRecord.setPrivacySensitive(true)` gesetzt → sperrt andere
Apps aktiv vom Mic aus. Zwei normale Apps koennen ohnehin nie gleichzeitig echtes Mic-Audio bekommen;
privacy-sensitive verschaerft das.
**Fix:** `setPrivacySensitive(true)` NICHT setzen. Mic sauber freigeben (§8). `AudioRecordingCallback`/
`isClientSilenced()` zum Erkennen der eigenen Stummschaltung nutzen.
**Versionen:** Android 10+. **Quelle:** developer.android.com/media/platform/sharing-audio-input.

## §10 — Kein ChatGPT-Voice-Deep-Link / kein Hotword
**Symptom:** Suche nach `chatgpt://...?mode=voice` oder „Hey ChatGPT" bleibt erfolglos.
**Root Cause:** OpenAI bietet (Stand 2026-06) keinen offiziellen Deep-Link in den Voice-Modus und kein
eigenes Hotword. Nur offene Feature-Requests. Externer Trigger = nur die System-Assist-Geste (App als
Default-Assistant) oder das Voice-Widget/Quick-Settings-Tile.
**Fix:** Nicht auf Deep-Link planen. Am Geraet verifizieren:
`adb shell dumpsys package com.openai.chatgpt` (Activities/Intent-Filter/Schemes),
`adb shell cmd shortcut get-shortcuts com.openai.chatgpt`.
**Versionen:** ChatGPT-App ab v1.2025.070. **Quelle:** community.openai.com Feature-Requests; 9to5google.com/2025/03/14.
