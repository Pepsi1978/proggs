# Android Voice-Assistant-Auslösung + Wake-Word + Mikrofon — Best Practices

**Stand:** 2026-06-11 (Best-Practices-Recherchelauf, 7 Researcher, offizielle Quellen zuerst).
**Anlass:** App, die per Weckwort den ChatGPT-Sprachmodus startet (Wake-Word → Assist-Geste).
**Versions-Anker:**
- **Zielgeraet:** Samsung Galaxy S23 Ultra, **One UI 6.1.1 / Android 14 (API 34)**.
- **ChatGPT-App:** ab **v1.2025.070** als Standard-Assistent setzbar (Assist-Geste startet Advanced Voice Mode direkt).
- **Wake-Word:** **sherpa-onnx KWS** (Apache-2.0, gleiche Engine wie der Windows-VoiceAgent + BestJournalAndroid native `.so`), Plan B **openWakeWord**.

> **Zweite Seite der Medaille zum Bug-Almanach** ([`bugs/android/voice-assistant-trigger.md`](../../../bugs/android/voice-assistant-trigger.md)):
> der Almanach sagt *was schiefgeht und wie man es umgeht*, diese Datei sagt *wie man es von vornherein
> richtig macht*. Quellen-Rangordnung: offizielle Android/Google-Quelle (developer.android.com, AOSP) =
> Grundwahrheit (`offiziell`), Community/Blogs = `extern` (sekundaer).
> **Abgrenzung:** Reines Framework/Service/Permission-Verhalten steht zusaetzlich in
> [`best-practices-android-platform.md`](best-practices-android-platform.md). Diese Datei fokussiert das
> SPEZIAL-Thema „fremden Sprachassistenten per Weckwort ausloesen".

---

## ⚡ Kurzcheck (Stufe A — vor der Arbeit lesen)

> **Digest-Modell** (`bugs/SYSTEM.md` §11): Kurzcheck = Stufe-A-Pflichtlektüre (`Read` mit `limit=80`).

| # | Situation | Best Practice (Kurzform) | Volltext |
|---|-----------|--------------------------|----------|
| 1 | Fremden Default-Assistant per App ausloesen | Geht NICHT aus normalem App-Code (System-only). Nur via Shizuku (keyevent 219) ODER Accessibility-UI-Automation | §1 |
| 2 | Persistenz-Anspruch (24/7, ueberlebt Reboot/Sperren) | Accessibility > Shizuku (Shizuku ohne Root nicht persistent; One UI 6.1.1 stoppt es beim Sperren) | §1.3 |
| 3 | Wake-Word-Engine waehlen | sherpa-onnx KWS (frei, open-vocab); Plan B openWakeWord; Silero-VAD davorschalten | §2 |
| 4 | Wake-Rate des eigenen Worts | VOR dem Bau live testen + Threshold/Boosting tunen (EN-Modelle wecken teils schwach auf) | §2.2 |
| 5 | Dauerhaft am Mic lauschen (Android 14) | Foreground-Service `foregroundServiceType="microphone"` + `FOREGROUND_SERVICE_MICROPHONE` | §3.1 |
| 6 | Service starten | AUS DEM VORDERGRUND starten (RECORD_AUDIO ist while-in-use); `START_STICKY` + Watchdog | §3.2 |
| 7 | Samsung One UI killt den Dienst | Akku „Uneingeschraenkt" + aus Tiefschlaf-Liste raus + Adaptiver Akku AUS (In-App-Setup-Screen) | §3.3 |
| 8 | Mikrofon an ChatGPT uebergeben | `stop()` → `release()` → **300–500 ms warten** → DANN Assist ausloesen | §4 |
| 9 | Wake-Word-App `setPrivacySensitive` | NIEMALS `true` (sperrt ChatGPT vom Mic aus) | §4.2 |
| 10 | Fremde Voice-Session WIEDER beenden | `KEYCODE_HEADSETHOOK` an ChatGPTs Media-Session (`AudioManager.dispatchMediaKeyEvent`); Erkennung via `mode == MODE_IN_COMMUNICATION`, nicht via `micSilenced` | §1.4 |

---

## 1. Den fremden Sprachassistenten ausloesen — realistische Wege

**Grundwahrheit (`offiziell`, AOSP):** Die echte Assist-Geste (Long-Press) ist eine privilegierte
SystemUI-Operation. `SearchManager.launchAssist` ist `@SystemApi`; `VoiceInteractionService.showSession`
darf nur die aktive Default-Assist-App auf SICH selbst aufrufen. Eine normal signierte Play-Store-App
kann die Geste **nicht 1:1 nachbilden**. Plane nie damit, „einfach den Assistenten zu starten".

Drei gangbare Wege (mit ehrlichen Trade-offs):

### 1.1 Shizuku + `KEYCODE_ASSIST` (219) — der „echte", aber fragile Weg
- App nutzt die Shizuku-API (`dev.rikka.shizuku:api`), Shizuku laeuft als shell-UID (2000) und darf
  `input keyevent 219` bzw. `IInputManager.injectInputEvent()`. Das loest den eingestellten
  Default-Assistant (z.B. ChatGPT) aus — die sauberste, „echte" Geste.
- **Nur waehlen, wenn das Geraet Shizuku persistent halten kann** (Root, oder Nutzer akzeptiert
  Neustart nach Reboot). Auf One UI 6.1.1 ungeeignet (siehe Almanach #3).

### 1.2 Accessibility-Service (UI-Automatisierung) — der dauerhafte Weg (Default-Empfehlung)
- Der `AccessibilityService` ueberlebt Reboot und Bildschirm-Sperren (sobald einmal aktiviert).
- Ablauf: ChatGPT per Intent oeffnen → auf das UI warten → Voice-Button per `contentDescription`
  finden (`findAccessibilityNodeInfosByText`/`...ByViewId`) → `ACTION_CLICK`. Robust genug fuer eine
  private Single-User-App.
- **Nachteil:** fragil gegen ChatGPT-UI-Aenderungen → Button-Erkennung tolerant (mehrere Label-Varianten:
  „Voice", „Start voice mode", „Sprachmodus") + Fallback-Logging bauen.
- Accessibility kann KEIN `KEYCODE_ASSIST` injizieren — nur App-UI bedienen (Back/Home/Recents + Klicks).

### 1.3 Eigene App ALS Default-Assistant (`VoiceInteractionService`)
- Nur sinnvoll, wenn die App SELBST der Assistent sein soll. Loest das „fremden Assistenten starten"-
  Problem nicht (sie wuerde ChatGPT als Assistent ersetzen). Hier nicht der Weg.

**Persistenz-Faustregel:** Anspruch „jederzeit per Weckwort, ohne Gefummel" → **Accessibility (1.2)**.
Shizuku nur bei Root oder bewusst akzeptierter Reboot-Fummelei.

### 1.4 Eine laufende fremde Voice-Session (ChatGPT) WIEDER beenden — der saubere Weg

Spiegelbild zum Ausloesen. Am Galaxy Fold 6 (One UI 8 / Android 16) verifiziert (2026-06-11).
Reihenfolge der Robustheit:

1. **Bester Weg — Media-Button `KEYCODE_HEADSETHOOK` (kein Sonderrecht, `offiziell`):** ChatGPT-Voice
   meldet eine MediaSession `VoiceModeService` an. Ein Headsethook beendet die Session zuverlaessig
   (Audio → `MODE_NORMAL`). `PAUSE`/`STOP`/`PLAY_PAUSE` ignoriert ChatGPT.
   ```kotlin
   val am = ctx.getSystemService(AudioManager::class.java)
   if (am.mode == AudioManager.MODE_IN_COMMUNICATION) {       // nur wenn ChatGPT-Voice laeuft
       val t = SystemClock.uptimeMillis()
       am.dispatchMediaKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK, 0))
       am.dispatchMediaKeyEvent(KeyEvent(t, t, KeyEvent.ACTION_UP,   KeyEvent.KEYCODE_HEADSETHOOK, 0))
   }
   ```
   Teardown dauert 1–3 s → verzoegert ueber den Audio-Modus verifizieren, sonst zweiten Druck senden.
   (`developer.android.com/reference/android/media/AudioManager#dispatchMediaKeyEvent`)
2. **Erkennung „laeuft noch?":** `AudioManager.mode == MODE_IN_COMMUNICATION` (zuverlaessig). NICHT
   `AudioRecordingCallback.isClientSilenced`/`micSilenced` (flaky). Echtes GSM-Telefonat = `MODE_IN_CALL`
   → durch das `IN_COMMUNICATION`-Gate automatisch ausgenommen.
3. **Trigger „Kugel weggewischt":** `AccessibilityService` beobachtet `TYPE_WINDOW_STATE_CHANGED`/
   `TYPE_WINDOWS_CHANGED`; verschwindet ChatGPTs Fenster, waehrend `IN_COMMUNICATION` noch gilt →
   Headsethook senden. (Manueller Not-Aus-Knopf braucht KEINE Bedienungshilfe — `dispatchMediaKeyEvent`
   reicht.)
4. **Fallback, falls dispatch je OEM-blockiert:** `NotificationListenerService` +
   `MediaSessionManager.getActiveSessions(listener)` → Controller von `com.openai.chatgpt` →
   `controller.dispatchMediaButtonEvent(HEADSETHOOK)` (gezielt an die Session).
5. **Was NICHT zu empfehlen ist:** ChatGPTs UI-Knoepfe klicken (Kugel hat keinen Beenden-Knopf,
   In-App-„Beenden" stoppt die Aufnahme nicht zuverlaessig); Recents-Karte per A11y wegwischen
   (One-UI-Recents ist oft Secure-Window → Karten im A11y-Tree nicht lesbar, fragil); `force-stop`
   via Shizuku (zuverlaessig, aber One UI 8 haelt Shizuku ohne Root nicht persistent, siehe Almanach §3).

## 2. Wake-Word-Engine

### 2.1 Engine-Wahl
- **Primaer sherpa-onnx KWS** (`offiziell` k2-fsa): Apache-2.0, open-vocab (Weckwort ohne Retraining
  ueber Keyword-Datei + Threshold), offizielle vorgebaute Android-APKs, voll offline. Wissens-Uebertrag
  vom Windows-VoiceAgent (gleiche Keyword-Dateien).
- **Plan B openWakeWord**: Apache-2.0, fertige Kotlin-Libs, dokumentierte False-Accept-Rate ~0,18–1/h;
  Custom-Wort braucht ~1 h Colab-Training.
- **Porcupine** nur wenn maximale Genauigkeit > Kosten/Freiheit (Free-Tier nur 3 MAU, sonst Lizenz).
- **Vosk/Snowboy meiden** (schlechte Wake-Genauigkeit / eingestellt).
- **Immer Silero-VAD vorschalten**: spart Akku und senkt Fehlausloeser (nur bei Sprache aufwecken).

### 2.2 Wake-Rate VOR dem Bau verifizieren
Englische Modelle (z.B. sherpa-onnx EN-Gigaspeech) wecken empirisch teils schwach auf (Almanach #7).
Daher: Aufweck-Rate fuers konkrete Weckwort LIVE testen, `keywords_threshold` + `keywords_score`
(Boosting) tunen, ggf. Modell wechseln. Nicht blind auf Default-Threshold 0.5 verlassen.

## 3. Dauer-Mic-Foreground-Service (Android 14 / One UI 6)

### 3.1 Manifest (Pflicht)
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" /> <!-- API 34 Pflicht -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<service android:name=".WakeWordService"
    android:foregroundServiceType="microphone" android:exported="false" />
```
Fehlt `foregroundServiceType` + `FOREGROUND_SERVICE_MICROPHONE` → `MissingForegroundServiceTypeException` (Almanach #4).

### 3.2 Laufzeit
- RECORD_AUDIO zur Laufzeit anfordern und Grant pruefen, BEVOR der Service startet.
- Service AUS DEM VORDERGRUND starten (sichtbare Activity / Notification-Interaktion); Hintergrund-Start
  → `SecurityException` (while-in-use), Almanach #5. Akku-Ausnahme erlaubt zusaetzlich FGS-Start aus dem Hintergrund.
- `startForeground(...)` mit `ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE` binnen ~5 s.
- `onStartCommand` → `START_STICKY`; `onTaskRemoved` → Restart (Receiver/AlarmManager/WorkManager-Watchdog).
- `PARTIAL_WAKE_LOCK` halten. Dauer-Notification + gruener Privacy-Punkt sind Pflicht (nicht abschaltbar).
- Reboot: BOOT_COMPLETED darf den Mic-FGS NICHT direkt starten → Notification „Tippen zum Aktivieren".

### 3.3 Samsung One UI 6 — In-App-Setup-Screen (sonst Dienst gekillt)
Nutzer durch diese Schritte fuehren (Deep-Links anbieten): App-Akku „Uneingeschraenkt"; aus
„Apps im Ruhezustand/Tiefschlaf" entfernen; Adaptiver Akku AUS; „Nicht genutzte Apps schlafen legen" AUS;
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`-Dialog. Quelle: dontkillmyapp.com/samsung.

## 4. Mikrofon-Uebergabe an ChatGPT (ohne Konflikt)

### 4.1 Ablauf (Reihenfolge ist alles)
```
1. Wake-Word erkannt
2. AudioRecord.stop()
3. AudioRecord.release()        // native Mic-Ressource freigeben (Pflicht)
4. 300–500 ms warten            // HAL-Flush ist ab Android 16 NICHT synchron (Almanach #8)
5. Assist/Voice ausloesen (Accessibility-Klick bzw. Shizuku keyevent)
6. Per AudioRecordingCallback/Lifecycle erkennen, wann ChatGPT fertig ist
7. AudioRecord neu erstellen + startRecording() → wieder lauschen
```

### 4.2 Stolperfallen
- `setPrivacySensitive(true)` auf der Wake-Word-App NIEMALS setzen — wuerde ChatGPT vom Mic aussperren.
- `AudioRecordingCallback.onRecordingConfigChanged` + `isClientSilenced()` nutzen, um sauber zu erkennen,
  wann die eigene Aufnahme stillgeschaltet wurde (guter Auto-Stop-Trigger).
- AudioFocus loest den Mic-Konflikt NICHT (regelt nur Wiedergabe) — nicht als Loesung einplanen.

## 5. ChatGPT-spezifisch (Stand 2026-06)
- Kein offizieller `chatgpt://`-Deep-Link in den Voice-Modus, kein eigenes Hotword („Hey ChatGPT" gibt es nicht).
- Es existieren ein Voice-Mode-Homescreen-Widget und teils eine Schnelleinstellungs-Kachel.
- Vor Architektur-Entscheidung am Geraet verifizieren:
  `adb shell dumpsys package com.openai.chatgpt` (Activities, Intent-Filter `ASSIST`/`VOICE_COMMAND`,
  `<data scheme="chatgpt">`), `adb shell cmd shortcut get-shortcuts com.openai.chatgpt`.

---

## 🔗 Bezug zum Bug-Almanach ([`bugs/android/voice-assistant-trigger.md`](../../../bugs/android/voice-assistant-trigger.md))

| Best-Practice (hier) | Verhindert Bug(s) im Almanach |
|----------------------|-------------------------------|
| §1 Realistische Auslöse-Wege | #1 (ACTION_ASSIST unzuverlässig), #2 (INJECT_EVENTS), #3 (Shizuku-Persistenz) |
| §2 Engine-Wahl + Wake-Rate-Test | #7 (schwache Wake-Rate) |
| §3 FGS korrekt + Samsung-Setup | #4 (FGS-Type), #5 (Hintergrund-Start), #6 (Samsung killt) |
| §4 Mic-Übergabe | #8 (HAL-Delay), #9 (setPrivacySensitive) |
| §5 ChatGPT-Einstiegspunkte | #10 (kein Deep-Link/Hotword) |

## Quellen
- Assist/Intents + VoiceInteractionService: developer.android.com/develop/devices/assistant; /reference/android/service/voice/VoiceInteractionService
- AOSP SearchManager.launchAssist (@SystemApi): android.googlesource.com/platform/frameworks/base SearchManager.java
- Shizuku: github.com/RikkaApps/Shizuku-API; shizuku.rikka.app; Issue #612 (One UI 6.1.1)
- sherpa-onnx KWS: k2-fsa.github.io/sherpa/onnx/kws · openWakeWord: github.com/dscripka/openWakeWord
- FGS microphone: developer.android.com/develop/background-work/services/fgs/service-types · Samsung: dontkillmyapp.com/samsung
- Mic-Sharing: developer.android.com/media/platform/sharing-audio-input
- ChatGPT als Default-Assistant: 9to5google.com/2025/03/14; howtogeek.com/set-chatgpt-default-voice-assistant-on-android
