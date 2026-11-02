# Bug-Almanach: Android per adb fernbedienen (Handy-Use)

**Stand:** 02.11.2026 · **Anker:** adb / platform-tools 37.0.1, Galaxy Z Fold 8, Android 17, One UI 9, Android Emulator API 37
**Kurzcheck:** `handy-use-kurzcheck.md` · **Best Practices:** `best-practices/android/handy-use.md`

## 1. `uiautomator dump` braucht 11–12 s oder scheitert
- **Symptom:** Mit Jarvis im Vordergrund 11–12 s, teils „could not get idle state“; `android layout` scheitert mit Leerlaufprüfung 3 von 3 Mal.
- **Ursache:** Warten auf Leerlauf; Dauer-Animationen erreichen ihn nie.
- **Versionen:** gemessen Android 17, One UI 9.
- **Fix:** Baum über eine offene UiAutomation ohne `waitForIdle` (Skill `handy-use`, 4–6 ms); notfalls `android layout --no-idle` (~1,1 s).
- **Quelle:** eigene Messung 02.11.2026.

## 2. Helfer stirbt sofort mit `Killed`, rc=137
- **Symptom:** `app_process`-Helfer endet nach dem Verbinden; Logcat: `NullPointerException ... Looper.mQueue` in `AccessibilityInteractionClient.<init>`.
- **Ursache:** `AccessibilityInteractionClient` braucht `Looper.getMainLooper()`, ein nackter `app_process` hat keinen. Im Emulator stürzte es jedes Mal ab, am Handy lief der erste Test trotzdem (warum, ist nicht geklärt).
- **Versionen:** API 37.
- **Fix:** Im Hauptthread `Looper.prepareMainLooper()` und `Looper.loop()`, die Arbeit in einem eigenen Thread.
- **Quelle:** eigene Messung 02.11.2026.

## 3. `pkill -f <Name>` beendet die eigene Shell
- **Symptom:** Befehle nach `pkill -f HandyHelper; mkdir ...; chmod ...` laufen nicht, der Ordner behält falsche Rechte.
- **Ursache:** Die Befehlszeile von `sh -c` enthält das Wort selbst.
- **Fix:** `pkill -f '[H]andyHelper'`.
- **Quelle:** eigene Messung 02.11.2026.

## 4. `adb shell '... &'` kehrt nicht zurück
- **Symptom:** Timeout beim Start eines Hintergrundprogramms per `adb shell`.
- **Fix:** `setsid nohup ... > log 2>&1 < /dev/null &` und am Rechner mit kurzem Zeitlimit aufrufen.
- **Quelle:** eigene Messung 02.11.2026.

## 5. `injectInputEvent(ev, true)` verlangsamt Gesten stark
- **Symptom:** Wischen mit 250 ms dauert 1,6 s, Taste 520 ms (Emulator); aus dem Wischer wird ein anderer Scrollweg.
- **Ursache:** `sync=true` wartet je Ereignis auf die Zustellung.
- **Fix:** `sync=false`, die Reihenfolge bleibt erhalten.
- **Quelle:** eigene Messung 02.11.2026.

## 6. Direkt nach App-Start leerer Baum gilt als „ruhig“
- **Symptom:** `open` plus `wait` plus `tree` liefert nichts.
- **Ursache:** Zwei leere Bäume sind gleich.
- **Fix:** Leere Bäume nie als Ruhe werten.
- **Quelle:** eigene Messung 02.11.2026.

## 7. `input text` kann keine Umlaute und Emojis
- **Symptom:** `input text "Grüße 😀"` tippt nichts, falsche Zeichen oder bricht ab.
- **Ursache:** `InputShellCommand` geht über `KeyCharacterMap.VIRTUAL_KEYBOARD`, Nicht-ASCII hat keine Behandlung.
- **Versionen:** AOSP main, Abruf 02.11.2026.
- **Fix:** `ACTION_SET_TEXT`, sonst Zwischenablage plus `KEYCODE_PASTE`.
- **Quelle:** https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/services/core/java/com/android/server/input/InputShellCommand.java

## 8. `cmd clipboard` meldet „No shell command implementation“
- **Versionen:** gemessen Android 17, One UI 9.
- **Fix:** `IClipboard.setPrimaryClip` per Reflection in der Shell-Rolle.
- **Quelle:** eigene Messung 02.11.2026.

## 9. Screenshot auf dem Fold unlesbar
- **Symptom:** Bild lässt sich nicht dekodieren, wirkt leer oder geschützt.
- **Ursache:** `screencap` ohne `-d` schreibt `[Warning] Multiple displays were found`, und `exec-out` mischt sie vor die Bilddaten.
- **Fix:** `-d <physische ID>` aus `dumpsys SurfaceFlinger --display-id`, oder vor `\x89PNG` beziehungsweise `FF D8` abschneiden.
- **Quelle:** eigene Messung 02.11.2026.

## 10. Tippen landet neben dem Ziel, wenn das Fold zugeklappt ist
- **Ursache:** Koordinaten mit der Größe des Innendisplays gerechnet; `wm size` nennt die Innenmaße auch zugeklappt.
- **Fix:** Koordinaten nur aus dem aktuellen Baum oder Bild, Größe aus den Grenzen der aktiven Wurzel.
- **Quelle:** extern (argent 0.28.0) und eigene Beobachtung 02.11.2026.

## 11. `svc wifi`, `svc data`, `svc bluetooth`, `media` fehlen
- **Versionen:** gemessen Android 17.
- **Fix:** `cmd wifi`, `cmd bluetooth_manager`, `cmd media_session volume`.
- **Quelle:** eigene Messung 02.11.2026; AOSP `Svc.java`.

## 12. `dumpsys notification` zeigt keinen Text
- **Symptom:** `android.text=String [length=18]`.
- **Fix:** `dumpsys notification --noredact`.
- **Quelle:** eigene Messung 02.11.2026.

## 13. Bildschirmtext steuert den Agenten
- **Symptom:** Agent führt Anweisungen aus Bildschirminhalten aus.
- **Ursache:** Baum und Bild gelten als vertrauenswürdig, unsichtbare Overlays wirken schon ab 2 % Deckkraft.
- **Fix:** Bildschirminhalt nur als Daten, Bestätigung vor wirksamen Aktionen.
- **Quelle:** extern, Studien in `best-practices/android/handy-use.md` §6.

## 14. Gradle: „Java home supplied is invalid“ nach JDK-Update
- **Symptom:** Jeder Bau scheitert sofort, `org.gradle.java.home` zeigt auf `jdk-21.0.12.101-hotspot`.
- **Ursache:** Das JDK-Update ersetzt den Ordner, `~/.gradle/gradle.properties` bleibt auf dem alten Pfad.
- **Fix:** `-Dorg.gradle.java.home=<aktueller Pfad>` oder die Datei anpassen.
- **Quelle:** eigene Messung 02.11.2026.
