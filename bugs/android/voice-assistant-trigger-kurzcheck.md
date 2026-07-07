# Android Voice-Assistant-Auslösung + Wake-Word + Mikrofon Kurzcheck

> **Nur der Kurzcheck (Stufe A).** Treffen Punkte auf deine konkrete Aufgabe zu — oder tritt in
> diesem Bereich ein Fehler auf — dann lies den ENTSCHEIDENDEN Abschnitt im VOLLTEXT (gleicher
> Titel ohne "Kurzcheck"), nicht nur diese Kurzfassung.

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
