# Android Voice-Assistant-Auslösung + Wake-Word + Mikrofon Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
| 11 | Dauer-Lauschen ohne Akkufresser/Waerme | Zweistufige Pipeline: WebRTC-VAD (`VERY_AGGRESSIVE`!) gate't die ASR; Pre-Roll ~300 ms; Silence-Hangover > ASR-Endpoint (~800 ms); Gate-Statistik-Sonde ins Log | §6 |
