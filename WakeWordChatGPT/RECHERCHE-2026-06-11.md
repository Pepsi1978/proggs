# Recherche: Wake-Word-App startet ChatGPT-Sprachmodus (Android)

**Datum:** 2026-06-11
**Zielgeraet:** Samsung Galaxy S23 Ultra, One UI 6.1.1 / Android 14
**Ziel:** Eine Android-App, die (1) dauerhaft auf ein Weckwort lauscht und (2) bei Erkennung
den ChatGPT-Sprachmodus (Advanced Voice Mode) startet — also exakt das ausloest, was sonst
das lange Druecken der Power-/Home-Taste tut.

Quelle: 7 parallele Researcher (Opus), 2026-06-11.

---

## Kernfazit

Das Ziel ist technisch machbar, aber der entscheidende Schritt — eine App loest die
"Assist-Geste" aus — ist von Android ABSICHTLICH verriegelt (System-only). Es bleiben zwei
realistische Wege, beide mit Nachteilen:

1. **Shizuku + KEYCODE_ASSIST (219)** — elegant, aber auf dem S23 Ultra nicht dauerhaft
   (Shizuku stoppt beim Sperren, Bug #612; nach Reboot manuell neu zu starten).
2. **Accessibility-Service (UI-Automatisierung)** — dauerhaft (ueberlebt Reboot/Sperren),
   aber "ueber die Oberflaeche gemogelt" und damit anfaellig gegen ChatGPT-UI-Aenderungen.

Vor der Architektur-Entscheidung: das Geraet per `adb shell dumpsys package com.openai.chatgpt`
inspizieren — falls ChatGPT versteckte Intent-Filter/Deep-Links/Shortcuts hat, vereinfacht das alles.

---

## 1. Assist-Geste programmatisch ausloesen — NICHT moeglich (normale App)

- `startActivity(Intent.ACTION_ASSIST)`: existiert seit API 16, aber **unzuverlaessig** — kein
  garantiertes Routing an die Default-Assist-App; bei Samsung One UI eingeschraenkt. Kein Ersatz
  fuer den echten Long-Press.
- `SearchManager.launchAssist(...)`: **`@SystemApi`** (hart belegt aus AOSP `SearchManager.java`) —
  nur System-/signierte Apps.
- `VoiceInteractionService.showSession(...)`: nur die AKTUELL aktive Default-Assist-App darf ihre
  EIGENE Session zeigen — keine Drittapp auf einen fremden Assistenten.
- Echte Geste traegt `SHOW_SOURCE_ASSIST_GESTURE` (nur von SystemUI setzbar) + vollen Assist-Kontext.
  Ein App-Intent traegt das nicht.

**Fazit:** Ohne System-Signatur kann eine App die Geste nicht 1:1 nachbilden.

## 2. KEYCODE_ASSIST (219) / Power-Long-Press senden — nur mit Shell-Rechten

- `adb shell input keyevent 219` KANN den Default-Assistant ausloesen (geraeteabhaengig; auf
  Telefonen inkonsistent). `am start -a android.intent.action.ASSIST` ist die robustere Shell-Variante.
- Globale Key-Injection braucht `INJECT_EVENTS` — Schutzlevel **signature|privileged**. Normale Apps
  bekommen das NIE. `Instrumentation` ist auf die eigene UID beschraenkt. Accessibility kann
  Back/Home/Recents, aber KEIN KEYCODE_ASSIST injizieren.
- Einziger No-Root-Weg: ein Prozess mit **shell-UID (2000)** — praktisch **Shizuku**.

## 3. Shizuku — der No-Root-Weg, aber mit S23-Haken

- Eine Shizuku-faehige App kann `input keyevent 219` senden (via `ShizukuBinderWrapper` +
  `IInputManager.injectInputEvent()` oder Shell-Befehl in einer UserService, UID 2000).
- **Nachteile fuer Laien / auf dem Zielgeraet:**
  - Ohne Root **nicht persistent** — nach jedem Reboot per Wireless-Debugging neu starten.
  - **One UI 6.1.1 stoppt Shizuku beim Sperren/Entsperren** (GitHub Issue #612) — Dealbreaker fuer
    eine Dauer-App, die jederzeit reagieren soll.
  - Knox bleibt unberuehrt (kein Trip), aber Alltagstauglichkeit schlecht.
- Bibliothek: `dev.rikka.shizuku:api` / `:provider` (v13.x).

## 4. ChatGPT-Android-App (com.openai.chatgpt)

- **Als Standard-Assistent setzbar** (Einstellungen → Apps → Standard-Apps → Digital-Assistent →
  ChatGPT), seit App-Version v1.2025.070. Die Assist-Geste startet dann **direkt den Advanced
  Voice Mode** ("Connecting" → "Listening").
- **Kein eigenes Hotword** ("Hey ChatGPT" existiert nicht) — nur manueller Trigger ueber die Geste.
- **Kein offiziell bestaetigter `chatgpt://`-Deep-Link** in den Voice-Modus (nur offene Feature-Requests).
- Es gibt ein **Voice-Mode-Homescreen-Widget** und auf manchen Geraeten eine **Schnelleinstellungs-Kachel**.
- Reaktion auf `ACTION_ASSIST` aus App-Code: nicht oeffentlich dokumentiert → am Geraet verifizieren:
  `adb shell dumpsys package com.openai.chatgpt` (Activities, Intent-Filter `ASSIST`/`VOICE_COMMAND`,
  `VoiceInteractionService`, `<data scheme="chatgpt">`); `adb shell cmd shortcut get-shortcuts com.openai.chatgpt`.

## 5. Wake-Word-Engines (On-Device)

| Engine | Custom-Wort | Lizenz | Genauigkeit | Akku 24/7 | Android |
|--------|-------------|--------|-------------|-----------|---------|
| **sherpa-onnx KWS** | Ja (open-vocab, kein Retrain) | Apache-2.0 frei | gut (Modell-abhaengig; EN-Gigaspeech schwach → tunen) | leicht | offizielle Android-APKs |
| Picovoice Porcupine | Ja (Web-Konsole) | Free nur 3 MAU, sonst kostenpflichtig | branchenfuehrend | sehr effizient | offizielles SDK |
| openWakeWord | Ja (~1h Colab-Training) | Apache-2.0 frei | gut (~0.18–1 FA/h) | leicht (+VAD) | fertige Kotlin-Libs |
| Vosk | Wortliste | Apache-2.0 frei | schlecht fuer Wake | hoch | offiziell |
| Snowboy | — | eingestellt | — | — | nicht verwenden |

**Empfehlung:** sherpa-onnx KWS (Wissens-Uebertrag vom Windows-VoiceAgent, frei, offizielle Android-APKs).
Vorher Aufweck-Rate fuers Weckwort live testen + Threshold/Boosting tunen. Plan B: openWakeWord. Beide
mit Silero-VAD vorschalten (Akku + weniger Fehlausloeser).

## 6. Dauer-Mic-Foreground-Service (Android 14 / One UI 6)

Manifest:
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" /> <!-- API 34 Pflicht -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<service android:name=".WakeWordService" android:foregroundServiceType="microphone" android:exported="false" />
```
Laufzeit: RECORD_AUDIO vor Service-Start anfordern; Service AUS DEM VORDERGRUND starten (while-in-use);
`startForeground` mit `FOREGROUND_SERVICE_TYPE_MICROPHONE`; `START_STICKY` + `onTaskRemoved`-Restart +
WorkManager-Watchdog; PARTIAL_WAKE_LOCK; Akku-Optimierung ausnehmen. Dauernde Notification + gruener
Privacy-Punkt sind Pflicht und nicht abschaltbar.

**Samsung One UI 6 (kritisch, sonst Dienst gekillt):** App-Akku auf "Nicht optimieren/Uneingeschraenkt";
aus "Apps im Ruhezustand/Tiefschlaf" entfernen; Adaptiver Akku AUS; "Nicht genutzte Apps schlafen legen" AUS.
In-App "Akku-Setup"-Screen mit Deep-Links anbieten. Reboot: BOOT_COMPLETED darf den Mic-FGS nicht direkt
starten → Notification "Tippen zum Aktivieren".

## 7. Mikrofon-Uebergabe Wake-Word-App → ChatGPT

- Zwei normale Apps NIE gleichzeitig am Mic — eine bekommt Stille (kein Kill). ChatGPT gewinnt als
  Foreground + VOICE_COMMUNICATION praktisch immer; trotzdem aktiv freigeben (Race vermeiden).
- Ablauf: Wake erkannt → `AudioRecord.stop()` → `release()` → **300–500 ms warten** (HAL-Flush, Android 16) →
  Assist/Voice ausloesen → per `AudioRecordingCallback`/Lifecycle erkennen wann ChatGPT fertig → neu lauschen.
- `setPrivacySensitive(true)` auf der Wake-Word-App NICHT setzen (wuerde ChatGPT aussperren).
- AudioFocus loest den Mic-Konflikt NICHT (regelt nur Wiedergabe).

---

## Architektur-Optionen (Entscheidung offen)

**Option A — Shizuku + KEYCODE_ASSIST**
Wake-Word-App (sherpa-onnx) → Mic frei → Shizuku sendet keyevent 219 → ChatGPT (Default-Assistant) startet Voice.
+ sauberste, "echte" Geste. − Shizuku auf S23 nicht dauerhaft (Reboot/Sperren), Laien-Fummelei.

**Option B — Accessibility-Service (UI-Automatisierung)**
Wake-Word-App → Accessibility oeffnet ChatGPT + tippt den Voice-Button (bzw. Voice-Widget).
+ dauerhaft (ueberlebt Reboot/Sperren). − fragil gegen ChatGPT-UI-Aenderungen, mehr Berechtigungen.

**Naechster Schritt:** Geraet per adb inspizieren (dumpsys ChatGPT) → entscheidet, ob ein direkter
Einstiegspunkt existiert, der Option A/B vereinfacht.

---

## Quellen (Auswahl)
- Android Assist/Intents: developer.android.com/develop/devices/assistant/intents
- AOSP SearchManager.launchAssist (@SystemApi): android.googlesource.com/platform/frameworks/base SearchManager.java
- VoiceInteractionService/showSession: developer.android.com/reference/android/service/voice/VoiceInteractionService
- INJECT_EVENTS / shell-UID: developer.android.com/reference/android/view/KeyEvent; doridori.github.io/Android-Security-welcome-to-shell
- Shizuku: github.com/RikkaApps/Shizuku-API; shizuku.rikka.app/guide/setup; Issue #612 (One UI 6.1.1 Sperren)
- ChatGPT als Default-Assistant: howtogeek.com/set-chatgpt-default-voice-assistant-on-android; 9to5google.com/2025/03/14
- sherpa-onnx KWS: k2-fsa.github.io/sherpa/onnx/kws; github.com/k2-fsa/sherpa-onnx
- openWakeWord: github.com/dscripka/openWakeWord; github.com/Re-MENTIA/openwakeword-android-kt
- Foreground Service microphone: developer.android.com/develop/background-work/services/fgs/service-types
- Samsung Akku-Killer: dontkillmyapp.com/samsung
- Mic-Sharing: developer.android.com/media/platform/sharing-audio-input
