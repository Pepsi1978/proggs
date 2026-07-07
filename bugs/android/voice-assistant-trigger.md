# Bekannte Bugs: Android Voice-Assistant-Auslösung + Wake-Word + Mikrofon

> **PFLICHT-LESEN vor Arbeit an:** App, die einen Sprachassistenten per Weckwort/aus Code
> ausloest, Wake-Word-Daueraufnahme, Foreground-Mic-Service, Mikrofon-Uebergabe zwischen Apps,
> Shizuku/KEYCODE_ASSIST, Default-Assistant-Integration.
>
> **Stand:** 2026-06-12 (Erstrecherche 2026-06-11, 7-Researcher-Schwarm; §13/§14 ergaenzt
> 2026-06-12 nach Low-Power-Recherche + Live-Vorfall VoiceKey 0.6.0).
> **Zielgeraet-Anker:** Samsung Galaxy S23 Ultra, One UI 6.1.1 / Android 14 (API 34).
> ChatGPT-App ab v1.2025.070. Wake-Word: sherpa-onnx KWS / openWakeWord.
>
> **Zweite Seite (Praevention):** [`best-practices/android/voice-assistant-trigger.md`](../../best-practices/android/voice-assistant-trigger.md).
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
| 11 | Fremde ChatGPT-Voice-Session BEENDEN | Kugel hat KEINEN Beenden-Knopf, In-App-„Beenden" stoppt Aufnahme nicht zuverlaessig; `KEYCODE_HEADSETHOOK` an ChatGPTs Media-Session beendet sie | §11 |
| 12 | „Laeuft ChatGPT-Voice noch?" zuverlaessig erkennen | `AudioManager.mode == MODE_IN_COMMUNICATION` (echtes Telefonat = `MODE_IN_CALL`); `isClientSilenced`/`micSilenced` ist FLAKY, nicht als Gate nutzen | §12 |
| 13 | Dauer-Lauschen frisst Akku / Handy wird warm | ASR (Vosk) nie 24/7 nackt laufen lassen — WebRTC-VAD-Gate davor, Mode `VERY_AGGRESSIVE` (NORMAL laesst Raum-Rauschen durch = Gate dauernd offen) | §13 |
| 13b | VAD-Gate da, aber "Wake + beenden" geht waehrend Session nicht mehr | Gate WAEHREND fremder Session aussetzen (`micSilenced`/`MODE_IN_COMMUNICATION`) — gedaempftes Session-Audio wertet das Gate sonst als Stille | §13b |
| 14 | Wake-Word-Engine auswaehlen (2026) | Porcupine-Free-Tier ENDET 30.06.2026; sherpa-KWS hat kein DE-Modell; openWakeWord braucht Training pro Wort; fuer frei waehlbare Woerter: Vosk-Grammatik + VAD-Gate | §14 |

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

## §11 — Fremde ChatGPT-Voice-Session aus der eigenen App BEENDEN (Fold 6 verifiziert 2026-06-11) ⭐
**Symptom:** Die ChatGPT-Voice-Kugel wegwischen oder die eigene App schliessen beendet die
Session NICHT — ChatGPT hoert im Hintergrund weiter zu (Foreground-Dienst). Nur „aus dem
Speicher werfen" (Recents-Karte) stoppt es zuverlaessig.
**Root Cause / Geraete-Befunde (Galaxy Fold 6, One UI 8 / Android 16, live verifiziert):**
- Die Kugel ist `com.openai.chatgpt/com.openai.voice.assistant.AssistantActivity` und hat **nur
  einen `content-desc="In App öffnen"`-Knopf — KEINEN „Beenden"**. Erst die volle App
  (`MainActivity`) zeigt unten rechts `content-desc="Beenden"`.
- Selbst ein synthetischer Tipp auf diesen In-App-„Beenden"-Knopf **stoppte die Aufnahme nicht
  zuverlaessig** (Audio blieb `MODE_IN_COMMUNICATION`), waehrend der Nachbar-Knopf „Mikrofon
  ausschalten" sofort reagierte. UI-Klicken ist also kein verlaesslicher Beenden-Weg.
- Eine normale App **darf** ChatGPT nicht `force-stop`-en (nur Shell-UID 2000 → Shizuku/Root;
  `am force-stop com.openai.chatgpt` wirkt, setzt Audio sofort auf `MODE_NORMAL` — aber Shizuku
  ohne Root ist auf One UI 8 nicht persistent, siehe §3). `killBackgroundProcesses` greift nicht
  (Foreground-Dienst). Audio-Focus stoppt keine fremde Aufnahme.
- **Was zuverlaessig wirkt (der Fix):** ChatGPT meldet eine **MediaSession „VoiceModeService"**
  als Media-Button-Empfaenger an. Ein **`KEYCODE_HEADSETHOOK`** (Code 79) an diese Session
  beendet die Voice-Session sauber (Audio → `MODE_NORMAL`). `KEYCODE_MEDIA_PAUSE`(127)/`STOP`(86)/
  `PLAY_PAUSE`(85) und `media dispatch` werden **ignoriert** bzw. fehlen (`media`-Binary auf One UI
  nicht vorhanden). Der Teardown dauert **1–3 s** — erst danach faellt der Audio-Modus, deshalb mit
  Verzoegerung verifizieren und ggf. einen zweiten Headsethook senden.
**Fix (funktionserhaltend, kein Sonderrecht):**
`AudioManager.dispatchMediaKeyEvent(KeyEvent(ACTION_DOWN, KEYCODE_HEADSETHOOK))` + ACTION_UP —
nur senden, wenn `mode == MODE_IN_COMMUNICATION` (verhindert Start einer neuen Session und schont
echte Telefonate = `MODE_IN_CALL`). Umsetzung: `VoiceKey/.../trigger/AssistantStopper.kt`.
Alternativweg (falls dispatch je OEM-blockiert): MediaController via `NotificationListenerService` →
`MediaSessionManager.getActiveSessions(listener)` → Controller von `com.openai.chatgpt` →
`dispatchMediaButtonEvent(HEADSETHOOK)`.
**Diagnose-Befehle:** `adb shell dumpsys media_session` (zeigt „Media button session is
com.openai.chatgpt/VoiceModeService"), `adb shell dumpsys audio | grep "Actual mode"`,
`adb shell input keyevent 79` (manuell testen). **Quelle:** developer.android.com/reference/android/media/AudioManager#dispatchMediaKeyEvent ; MediaSessionManager-Doku; Geraete-Verifikation.

## §12 — „Laeuft die fremde Voice-Session noch?" — `micSilenced` ist FLAKY, Audio-Modus ist zuverlaessig
**Symptom:** Ein Not-Aus, der am stummgeschalteten eigenen Mic (`AudioRecordingCallback.isClientSilenced`)
erkennt „ChatGPT hat das Mic", greift unzuverlaessig — das Signal kippt hin und her (`silenced:true`
→ kurz darauf `false`), obwohl ChatGPT weiter zuhoert, und blockiert dann den Beenden-Flow
(„keine laufende Session"). Zusaetzlich: ein Guard `if (mode == MODE_IN_COMMUNICATION) abort("Telefonat")`
blockiert den Beenden-Flow GENAU dann, wenn ChatGPT-Voice laeuft (ChatGPT setzt selbst
`MODE_IN_COMMUNICATION`).
**Root Cause:** `isClientSilenced` haengt am eigenen Aufnahmezustand (wird beim Re-Arm/Stop der
eigenen Engine zuruckgesetzt) und an Androids Concurrent-Capture-Policy → kein verlaesslicher
Proxy fuer „fremde Session aktiv". Und `MODE_IN_COMMUNICATION` ist der VoIP-Modus — ChatGPT-Voice,
NICHT ein GSM-Telefonat (das ist `MODE_IN_CALL`).
**Fix:** Session-aktiv-Erkennung ueber `AudioManager.mode == MODE_IN_COMMUNICATION`. Echte
Telefonate ueber `MODE_IN_CALL` ausschliessen (passiert automatisch, da nur auf `IN_COMMUNICATION`
reagiert wird). `micSilenced` nur noch fuer Observability/Logging, nie als Entscheidungs-Gate.
**Versionen:** Android 10+ (Audio-Modi seit jeher); verifiziert One UI 8 / Android 16.
**Quelle:** developer.android.com/reference/android/media/AudioManager (MODE_IN_COMMUNICATION vs MODE_IN_CALL).

## §13 — WebRTC-VAD `Mode.NORMAL` laesst Raum-Rauschen durch → Sprach-Gate wirkungslos ⭐ SELBST ERLEBT
**Symptom:** Trotz VAD-Gate vor der ASR bleibt die CPU-Last bei „Stille" hoch (VoiceKey: 70–78 %
eines Cores, Handy wird warm) — als gaebe es das Gate nicht.
**Root Cause:** Der WebRTC-VAD (GMM) ist bewusst „biased toward speech". Im Modus `NORMAL`
klassifiziert er normales Raum-Rauschen (Luefter, Strasse, Tastatur) dauerhaft als Sprache; ein
Silence-Hangover (noetig fuer die ASR-Finalisierung, z.B. 800 ms) haelt das Gate dann praktisch
permanent offen → die ASR rechnet wie ohne Gate.
**Fix (funktionserhaltend):** `Mode.VERY_AGGRESSIVE` (auch die Empfehlung der Lib-README).
Normal gesprochene Wake-Woerter erkennt der strengste Modus zuverlaessig; False-Positives kosten
nur kurz CPU (dahinter sitzt ja die ASR als zweiter Filter). Live gemessen (Fold 6, 2026-06-12):
70–78 % CPU → **3,5–10,7 %**, Gate nur noch 18–24 % offen bei Buerogeraeuschen.
**Pflicht-Begleiter:** (a) Gate-Statistik-Sonde ins Log (Anteil offener Chunks pro 30 s) — sonst
ist „das Gate wirkt" nicht belegbar; (b) Silence-Hangover MUSS groesser sein als die
Endpoint-Stille der ASR (Vosk ~500 ms → 800 ms), sonst wird die Aeusserung nie finalisiert;
(c) Pre-Roll-Puffer (~300 ms), weil der VAD ~50 ms zum Anschlagen braucht (Wortanfang).
**Versionen:** com.github.gkonovalov.android-vad:webrtc 2.0.10. **Quelle:** github.com/gkonovalov/android-vad
(README: empfohlene Parameter); eigener Vorfall VoiceKey 0.6.0.

## §13b — VAD-Gate VERSCHLUCKT „Wake-Wort + beenden" WAEHREND der fremden Session ⭐ SELBST ERLEBT (Regression)
**Symptom:** „Wake-Wort + beenden" beendet die laufende ChatGPT-Voice-Session NICHT mehr — obwohl
es vor Einbau des VAD-Gates (§13) zuverlaessig ging (am Geraet bestaetigt: alte Logs hatten
„Wake-Wort ignoriert — Session laeuft bereits" + „Beenden-Wort erkannt" WAEHREND der Session).
Nach dem Gate: im Session-Zeitraum (`micSilenced:true`) NULL Wort-Erkennungen im Log.
**Root Cause:** Waehrend ChatGPT-Voice laeuft, haelt ChatGPT das Mic — Androids Concurrent-Capture
daempft/stummschaltet unsere parallele Aufnahme (`isClientSilenced`). Dieses gedaempfte Audio stuft
das `VERY_AGGRESSIVE`-VAD als Stille ein → Gate bleibt ZU → die Vosk-Recognizer werden NICHT mehr
gefuettert → weder Wake- noch Stopp-Wort werden gehoert. Das Gate (gut fuer Akku) hat damit die
Beenden-per-Sprache-Funktion als **Fix-Induced-Failure** gebrochen. WICHTIG: `micSilenced` heisst
NICHT „nur Nullen" — die App bekommt weiterhin (gedaempftes) echtes Audio, die ASR kann es erkennen,
nur das aggressive VAD filtert es weg.
**Fix (funktionserhaltend):** Das VAD-Gate AUSSETZEN, solange eine fremde Session laeuft —
Bypass-Bedingung `micSilenced || mode == MODE_IN_COMMUNICATION`. Dann laufen die Recognizer pro
Chunk durch wie vor dem Gate; im Normalbetrieb (keine Session) bleibt das Gate aktiv und spart Akku.
Beide Features (Akku-Sparen + Beenden-per-Sprache) bleiben erhalten.
**Lehre:** Ein VAD-Gate vor einer ASR, die AUCH waehrend fremder Mic-Sessions hoeren soll, muss
fuer diese Sessions deaktivierbar sein — sonst frisst es genau die Worte, die man dann braucht.
**Versionen:** VoiceKey 0.6.0 (gebrochen) → 0.7.1 (gefixt). **Quelle:** eigener Vorfall + Geraete-Log Fold 6.

## §14 — Wake-Word-Engine-Auswahl 2026: Porcupine-Free-Tier endet, DE-Luecken bei Alternativen
**Symptom:** Suche nach einer energiesparenden Wake-Word-Engine fuer eine private App mit FREI
waehlbaren (auch deutschen) Woertern.
**Befunde (Researcher-Schwarm 2026-06-12):**
- **Picovoice Porcupine:** technisch ideal (<4 % CPU RPi3, ~1 MB RAM, DE-Custom-Words in Sekunden),
  aber der **Free Tier wird am 30.06.2026 abgeschaltet** — bestehende Free-AccessKeys werden
  deaktiviert, SDK-Init schlaegt danach fehl. Kein Non-Commercial-Ersatz geplant. Fuer private
  Apps damit tot. Quelle: community.home-assistant.io/t/1012744; picovoice.ai/docs/faq/general.
- **sherpa-onnx KWS:** Apache-2.0, open-vocabulary, Android-AAR vorhanden — aber **nur ZH/EN-Modelle**,
  keine deutschen Phoneme. Quelle: k2-fsa.github.io/sherpa/onnx/kws.
- **openWakeWord (openwakeword-android-kt):** laeuft nativ (ONNX), braucht aber **Training pro
  Wort** (Colab) — bricht das Feature „Nutzer tippt beliebiges Wort ein". Quelle: github.com/Re-MENTIA/openwakeword-android-kt.
- **Vosk:** laut Hersteller explizit NICHT fuer Always-on gedacht (Akku) — als nackter
  Dauerlauscher falsch, MIT VAD-Gate (§13) aber der einzige Open-Vocab-Weg fuer DE+EN.
  Quelle: alphacephei.com/vosk/android.
- **Das echte „Ok Google"** laeuft auf einem Low-Power-DSP (SoundTrigger HAL); die zugehoerige
  API (`AlwaysOnHotwordDetector`) ist seit Android 12 **@SystemApi nur fuer die
  Default-Assistant-App** — fuer normale Apps unerreichbar. Quelle: source.android.com/docs/whatsnew/android-12-release.
**Fix/Entscheidung:** Fuer frei waehlbare Woerter: zweistufige Pipeline VAD→Vosk-Grammatik (§13).
Nur bei festem Wortschatz lohnt openWakeWord (Training) als sparsamste freie Engine.
**Versionen:** Stand 2026-06-12.


---

<!-- verwandte-almanache (auto, bug-almanac-system) -->
## 🔗 Verwandte Almanache (siehe auch)

Diese Bereiche ueberschneiden sich mit diesem Almanach — bei Arbeit hier oft ebenfalls relevant. Wird einer von ihnen vom bug-almanac-guard getriggert, lohnt sich meist ein Blick (mindestens Kurzcheck) auch hier:

- [groq-transkription](../desktop/groq-transkription.md)
- [voice-pipeline](../desktop/voice-pipeline.md)
- [wake-word](../desktop/wake-word.md)
