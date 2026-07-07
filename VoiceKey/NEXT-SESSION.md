# VoiceKey — Übergabe & Fortsetzungs-Plan

> Stand: 2026-06-11. App-Idee von Frank: Per frei wählbarem **Wake-Word** den
> **ChatGPT-Voice-Mode** auf dem **Galaxy Fold 6 (Android 16 / One UI 8)** starten —
> exakt so, wie es der Side-Key-Long-Press tut. **Kein Root.** Die App selbst ist nur
> die **Einstellungs-Oberfläche**; das Lauschen läuft im Hintergrund (auch wenn die App
> geschlossen ist).

---

## 1. DER DURCHBRUCH (am echten Gerät verifiziert — das wichtigste Asset)

Auf dem Fold 6 (Gerät `RFCX70KTDFX`, Android 16, One UI 8 Build 80500) ist ChatGPT als
System-Assistent gesetzt (`voice_interaction_service =
com.openai.chatgpt/com.openai.feature.assistant.impl.AssistantVoiceInteractionService`).

**Verifiziert (mit Screenshots belegt):** Eine ganz normale App **ohne Sonderrechte,
ohne Root, ohne Shizuku** kann den ChatGPT-Voice-Mode (die schwirrende Kugel als Overlay)
direkt öffnen mit:

```kotlin
val intent = Intent()
    .setClassName(
        "com.openai.chatgpt",
        "com.openai.voice.assistant.AssistantActivity"
    )
    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
startActivity(intent)
```

Das wurde aus der echten VoiceKey-App-UID getestet → `topResumedActivity =
com.openai.chatgpt/com.openai.voice.assistant.AssistantActivity`, Kugel erschien. **Keine
SecurityException.** Das ist der Trigger, auf dem die ganze App steht.

### Was NICHT funktioniert (alles am Gerät durchgetestet):
- `Intent(ACTION_ASSIST)` ohne Paket → öffnet den App-Auswahl-Dialog (Resolver), nicht den Voice-Mode.
- `Intent(ACTION_ASSIST).setPackage("com.openai.chatgpt")` → öffnet nur die ChatGPT-App, nicht den Voice-Mode.
- `ACTION_ASSIST` an die `AssistantProxyActivity` → Proxy schließt sich sofort (will echten Assist-Kontext).
- `ACTION_VOICE_COMMAND` → kein Voice-Mode.
- `KEYCODE_ASSIST` (input keyevent 219) → öffnet den Voice-Mode, aber nur mit Shell/System-Rechten (eine App darf keine KeyEvents injizieren). War der erste erfolgreiche Test, aber für eine reine App nicht nutzbar — der direkte Component-Start oben ersetzt ihn vollständig.

### Wenn ChatGPT die Activity künftig umbenennt
Falls `com.openai.voice.assistant.AssistantActivity` in einer neuen ChatGPT-Version wegfällt:
auf dem Gerät neu ermitteln, welche Activity der Side-Key öffnet:
`adb shell input keyevent 219` + `adb shell dumpsys activity activities | grep ResumedActivity`.
Den Component-Namen dann in der App konfigurierbar/aktualisierbar halten (siehe „App-Registry" unten).

---

## 2. Geräte-Fakten (Fold 6)
- Android 16, One UI 8 (Build 80500). Gerät-ID adb: `RFCX70KTDFX`.
- Zwei Displays: Cover 968×2376 (state ON beim Test), Innen 1856×2160.
- ChatGPT-Paket: `com.openai.chatgpt`. Voice-Activity: `com.openai.voice.assistant.AssistantActivity`.
- ChatGPT als Default-Digital-Assistant gesetzt (Side-Key startet ChatGPT-Voice).

---

## 3. Die harte Lockscreen-Wahrheit (ehrlich, ohne Root nicht lösbar)
Bei **gesperrtem Gerät mit PIN/Fingerprint** erscheint der ChatGPT-Voice-Mode **nicht
sichtbar über dem Sperrbildschirm** — das ist eine Android-Sicherheitsgrenze, die **sogar
der echte Side-Key hat** (Assistent läuft dann hinter dem Lockscreen). Am Gerät bestätigt:
`input keyevent 219` bei gesperrtem Bildschirm → kein sichtbarer Voice-Mode, Keyguard bleibt.

**Realistischer, machbarer Weg (ohne Root):**
- Bildschirm an & entsperrt → Wake-Word startet Voice **sofort** ✅
- Bildschirm aus → App weckt Bildschirm (Trampolin-Activity mit `showWhenLocked` + `turnScreenOn`), Nutzer entsperrt 1× per Fingerprint, dann Voice ✅
- Komplett gesperrt ohne Nutzer → sichtbarer Voice-Mode nicht möglich (Plattformgrenze) ❌

Das muss so kommuniziert/umgesetzt werden — kein falsches Versprechen.

---

## 4. Wake-Word-Engine: Vosk (Entscheidung)
Frank-Anforderung: frei IN DER APP eintippbare Wake-Wörter, **getrennte Bereiche DE + EN**,
**Favoriten**, gut & kostenlos, offline. Frank nannte „Onyx/Sphinx" — gemeint ist eine
Offline-KWS-Engine. Gewählt: **Vosk** (`com.alphacephei:vosk-android:0.3.47`), weil es als
einzige kostenlose, Android-reife Engine BEIDES erfüllt: deutsches + englisches Modell UND
zur Laufzeit frei definierbare Schlüsselwörter (`KaldiRecognizer` mit Grammatik-JSON / Phrasen-Spotting).

- EN-Modell: `vosk-model-small-en-us-0.15` (~40 MB), DE-Modell: `vosk-model-small-de-0.15` (~45 MB).
- **Modell-Download beim ersten Start** (von alphacephei.com), entpacken in den internen App-Speicher
  (`StorageService.unpack` oder eigener Unzip). NICHT in die APK/ins Git packen (Größe).
- Keyword-Spotting: pro aktivem Wake-Wort eine Phrase ins Grammatik-JSON des `KaldiRecognizer`
  geben; bei Treffer (Confidence-Schwelle) den Trigger feuern. Pro Sprache ein eigener Recognizer/Modell.
- Alternative falls Vosk-DE zu schwach: Picovoice Porcupine (bessere Erkennung, aber Wörter
  müssen vorab in der Cloud-Konsole gebaut werden — widerspricht „frei eintippbar"). Daher Vosk.

---

## 5. Architektur (Soll)
```
de.frank.voicekey/
  VoiceKeyApp.kt              Application (DataStore-Init, Log-Setup)
  ui/
    MainActivity.kt          Compose-Host (NUR Einstellungen)
    SettingsScreen.kt        Hauptbildschirm (siehe Mockup)
    WakeWordViewModel.kt     State: Wörter DE/EN, Favoriten, Dienst an/aus, Permission-Status
    theme/Theme.kt           Material3, Orange-Akzent (#F97316) für Voice
  data/
    WakeWord.kt              Modell: id, text, sprache(DE/EN), favorit:Boolean
    AppTarget.kt             Modell: id, name, paket, voiceActivity, eigene Wake-Wörter (für spätere Apps)
    WakeWordRepository.kt    DataStore-Persistenz (Wörter, Favoriten, Dienst-Status, Modell-Status)
  service/
    WakeWordService.kt       Foreground-Service (microphone), Always-On-Mic, Vosk, bei Treffer -> Trigger
    BootReceiver.kt          BOOT_COMPLETED -> Tipp-Notification (Mic-FGS nicht direkt aus Boot startbar)
  wake/
    VoskWakeEngine.kt        Vosk-Wrapper: Modelle laden, Keyword-Spotting DE+EN parallel
    ModelManager.kt          Modell-Download + Entpacken + Fortschritt
  trigger/
    AssistantLauncher.kt     Baut den verifizierten Intent (oben) und startet ihn
    AssistantLauncherActivity.kt  Trampolin: showWhenLocked + turnScreenOn -> weckt + ruft AssistantLauncher
  obs/
    Obs.kt                   JSON-Lines-Logging + Logik-Sonden (observability-first-Direktive)
```

### Kritische technische Punkte (sonst funktioniert es nicht)
1. **Foreground-Service** `foregroundServiceType="microphone"` + Permissions `RECORD_AUDIO`,
   `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`. `startForeground()` binnen ~5 s.
   Mic-FGS darf NICHT aus `BOOT_COMPLETED` gestartet werden (Android 14+) → nach Boot nur
   eine Tipp-Notification posten, Nutzer öffnet App 1× → Dienst startet aus Vordergrund.
2. **Background-Activity-Launch (BAL)**: Der Hintergrund-Dienst darf ab Android 14/15/16 nur
   eine Activity (ChatGPT-Voice) starten, wenn er eine BAL-Ausnahme hat. Lösung:
   `SYSTEM_ALERT_WINDOW` (Overlay-Permission) — der Dienst hält ein 1px-Overlay, dann ist der
   Activity-Start erlaubt. Alternativ Batterieoptimierung-Ausnahme. Beides in der App anfragen.
3. **Lockscreen/Bildschirm aus**: Trampolin-Activity mit `android:showWhenLocked="true"`
   + `android:turnScreenOn="true"` → weckt Bildschirm; bei sicherem Lockscreen ist 1× Entsperren nötig.
4. **One UI Survival**: App auf „Uneingeschränkter Akku" + „Nie schlafende Apps" setzen lassen
   (sonst killt One UI den Dauer-Dienst). In der App per `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
   anfragen + dem Nutzer erklären.
5. **Mikrofon-Privacy-Punkt** (grüner Punkt) ist bei Dauer-Mic dauerhaft sichtbar — Systemverhalten,
   in der App transparent erklären.

---

## 6. Was schon im Repo ist (Commit-Stand)
- `#46710`: Projekt-Gerüst + Phase-1-Test-Activity (4 Trigger-Buttons), baute & lief.
- Danach (dieser WIP-Stand, evtl. noch nicht committet/baut nicht vollständig):
  - `gradle/libs.versions.toml`: Vosk, DataStore, lifecycle-service, JNA ergänzt.
  - `app/build.gradle.kts`: Dependencies, `versionCode 2 / 0.2.0`, `ndk abiFilters` (arm64/armeabi-v7a), packaging excludes.
  - `app/src/main/AndroidManifest.xml`: alle Permissions, `WakeWordService`, `BootReceiver`,
    `AssistantLauncherActivity` (showWhenLocked), `ui.MainActivity`, `VoiceKeyApp`, `<queries>` ChatGPT.
  - `MainActivity.kt` (im Root-Package) ist noch die TEST-Version mit dem direkten
    AssistantActivity-Start (Beweis-Code) — wird durch `ui/MainActivity.kt` ersetzt.
- **Achtung:** Das Manifest verweist bereits auf Klassen (`VoiceKeyApp`, `ui.MainActivity`,
  Service, Receiver, Trampolin, Themes `Theme.VoiceKey.Transparent`), die noch NICHT existieren.
  Diese sind als nächstes zu erstellen, dann baut es wieder.

## 7. Build/Geräte-Infos
- Gradle 8.11.1 (Wrapper), AGP 8.10.0, Kotlin 2.1.0, JDK 21
  (`C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot` — System-`JAVA_HOME` ist korrekt,
  NICHT überschreiben). compileSdk 36, minSdk 34, targetSdk 36.
- Build: `cd VoiceKey && .\gradlew.bat :app:assembleDebug`
- Install: `adb -s RFCX70KTDFX install -r app\build\outputs\apk\debug\app-debug.apk`
- Start: `adb -s RFCX70KTDFX shell monkey -p de.frank.voicekey.debug -c android.intent.category.LAUNCHER 1`
- Screenshot (Fold, sauber): auf Gerät `screencap -p /sdcard/x.png` dann `adb pull` (NICHT exec-out, sonst „Multiple displays"-Warnung in der Datei).

## 8. Zukunft: weitere Apps (nach ChatGPT Voice)
Datenmodell `AppTarget` (Name, Paket, Voice-Activity, eigene Wake-Wörter) von Anfang an so
bauen, dass später weitere Ziele hinzukommen können. Start: nur ein fester Eintrag
„ChatGPT Voice". Hinzufügen weiterer Apps später (Frank: lieber zentral verwaltet als per +-Button).
```
```

---

## Windows-Followup (2026-06-12): Geteilter Debug-Keystore

Auf dem Mac eingerichtet: `~/SK/VoiceKey/debug-shared.keystore` (Fingerprint SHA-256
`BC:5F:72:42:…:23:05`, identisch mit dem Mac-Default und dem EntropieReductor-SK-Keystore).
`app/build.gradle.kts` signiert Debug-Builds jetzt damit, WENN die Datei existiert
(sonst Maschinen-Default-Fallback).

**Auf der Windows-Maschine einmalig:** Die Datei `debug-shared.keystore` vom Mac nach
`C:\Users\barwa\SK\VoiceKey\debug-shared.keystore` kopieren. Danach signieren beide
Maschinen identisch — kein `INSTALL_FAILED_UPDATE_INCOMPATIBLE` mehr beim Geraete-Wechsel
(Almanach `bugs/android-build/gradle.md` §13; gleicher Vorfall wie EntropieReductor 0.13.0).
