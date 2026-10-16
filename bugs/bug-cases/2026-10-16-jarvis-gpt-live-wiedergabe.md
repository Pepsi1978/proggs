# Bug-Fall: Jarvis GPT-Live-Wiedergabe hackt (1.8.100–1.8.108) — Rohbeleg des Live-Tests

- **Datum:** 16.10.2026, gesichert 19:23
- **App:** Jarvis 1.8.108 (versionCode 119), Datei `Jarvis/app/src/main/java/de/frank/jarvis/live/LiveGespraech.kt`
- **Gerät:** Galaxy Z Fold 8 (SM-F971B), Handy-Lautsprecher, Medienweg mit Echosperre, GPT-Live mit Client-Delegation, Handy im Heim-WLAN
- **Auswertung und Regeln:** `best-practices/apis/openai-live-realtime.md` §14 · Bugs F13–F19 in `bugs/apis/openai-live-realtime.md`
- **Subjektiv (Frank):** „deutlich besser als vorher; Klang, Sprachfluss und Aussprache sehr gut" (kein Beleg für Fehlerfreiheit)

## Symptomverlauf und Ursachen (Kurzfassung)

| Version | Symptom | Ursache | Fix |
|---|---|---|---|
| 1.8.100/1.8.105 | Telefonklang | Telefonmodus + SCO | Medienweg (`USAGE_MEDIA`, A2DP, Handy-Mikro + Echosperre) |
| 1.8.101 | BT-Lautsprecher verschluckt Satzanfang | Ausgang geht bei leerem Track in Standby (2 s) | Füll-Stille in Pausen |
| 1.8.103/1.8.106 | „spricht" hängt, Echosperre falsch | Live liefert Stille in Echtzeit, Sperre nach Daten/Zeit | Pegel + Wiedergabeposition, Deckel 3 s |
| 1.8.106 | Späte Mitschrift löscht Antwort | Mitschrift als Barge-in-Signal trotz Echosperre | mit Echosperre nicht leeren, Antippen |
| 1.8.107 | Lücken mitten in der Rede | Füll-Stille nach Zeit statt Füllstand | Füll-Stille nur bei fast leerem Puffer |
| 1.8.108 | Abhacker im ersten Wort | Polster vor dem ersten hörbaren Stück zerriss leisen Wortansatz; 150 ms zu wenig | Polster 250 ms nur hinter Stille in Pausen |
| 1.8.108 | Strom aus dem Mischer | Füllgrenze 40 ms = 1 Mischtakt; leerer Start | Füllgrenze 80/45 ms, Vorlauf vor `play()` |

## Messung 16.10.2026, 19:16:27–19:18:45 (Logcat `JarvisLiveRouting`, `AudioFlinger:W`, `AudioTrack:W`)

- 52 Wiedergabe-Fenster à 2 s: Unterläufe **+0** in allen; Füll-Stille beim Sprechen **60 ms** gesamt (ein Fenster, 19:16:46).
- 65 Empfangs-Fenster: größte Lücke Median 148 ms, Maximum 386 ms, 20 Fenster > 200 ms, 7 > 300 ms (eines während der Rede: 312 ms).
- Satzanfang-Puffer 220–470 ms (14×), zweimal Polster nachgefüllt (von 60 bzw. 180 ms auf 250 ms).
- Echosperre auf 591–1225 ms nach dem letzten hörbaren Stück.
- Delegation 19:18:17,2 → `session.commentary.appended` 19:18:30,7.
- **Offen:** zweimal `restartIfDisabled` beim Start trotz Vorlauf.

### dumpsys media.audio_flinger (ca. 19:22, Auszug)

```
Output thread … name AudioOut_15 … (MIXER): Sample rate: 48000 Hz, HAL frame count: 1920, Output devices: 0x2 (AUDIO_DEVICE_OUT_SPEAKER), Standby delay ns=2000000000
Output thread … name AudioOut_D  … (MIXER): Sample rate: 48000 Hz, HAL frame count: 960,  Output devices: 0x2 (AUDIO_DEVICE_OUT_SPEAKER), Standby delay ns=2000000000
```

### Ereigniszeilen des Tests (ohne die 2-s-Statistik)

```
19:16:27.986 Status: Verbinde …
19:16:30.161 Gesprächsbeginn: Tonweg Medien (kein Telefonmodus), Echosperre=true, Wunsch medien, Ausgänge Hörmuschel, Lautsprecher, Telefonleitung „SM-F971B“
19:16:30.186 Echounterdrückung des Geräts: verfügbar=true, aktiv=true
19:16:30.192 W/AudioFlinger moveEffectChain_ll: effect chain for session 0 not on source thread
19:16:30.273 Wiedergabe: Mindestpuffer 120 ms, Puffer 1000 ms, Startpolster 250 ms, Füllgrenze 80 ms
19:16:30.277 Tatsächlich: Medienton · Echosperre · Ton: Lautsprecher · Mikro: Handy-Mikrofon
19:16:31.431 W/AudioTrack restartIfDisabled: releaseBuffer() track disabled due to previous underrun, restarting
19:16:32.338 Status: Jarvis spricht · Blase antippen zum Unterbrechen
19:16:32.339 Satzanfang: Puffer 380 ms, Stille davor 1792170992339 ms   (Startwert 0, Log-Eigenheit)
19:16:32.529 W/AudioTrack restartIfDisabled: releaseBuffer() track disabled due to previous underrun, restarting
19:16:34.841 Echosperre auf: Kopf 46080, Ton bis 37920, Warteschlange 0, letzter Ton vor 1225 ms
19:16:44.808 Satzanfang: Puffer 240 ms, Stille davor 11192 ms
19:16:46.432 Satzanfang mit Polster direkt davor: Puffer 60 ms, Stille davor 312 ms, 190 ms vorgelegt
19:16:46.442 Wiedergabe in 2004 ms: Puffer 20–450 ms, Füll-Stille 60 ms (beim Sprechen 60 ms), Polster 190 ms, Unterläufe +0
19:16:47.812 GPT-Live-Ausgabe in 2001 ms: 20 Stücke, 2000 ms Ton, davon 19 hörbar, größte Lücke 312 ms
19:17:16.550 Satzanfang mit Polster direkt davor: Puffer 180 ms, Stille davor 395 ms, 70 ms vorgelegt
19:17:38.102 Mitschrift kommt, während Jarvis spricht: Echosperre an, Ausgabe bleibt
19:18:17.225 Status: Jarvis schaut nach …
19:18:30.727 GPT-Live-Ereignis ohne Behandlung: session.commentary.appended
19:18:45.114 GPT-Live-Ausgabe in 2359 ms: 21 Stücke, 2040 ms Ton, davon 0 hörbar, größte Lücke 386 ms
```

## Prävention

Rückfall-Prüfung nach jeder Änderung an `live/`: Best Practice §14.6 (Logcat-Befehl, Auswerte-Einzeiler, Grenzwerte mit dieser Messung als Referenz).
