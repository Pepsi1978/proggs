# Session Handoff — 2026-06-08 ~01:45

## Ziel
VoiceAgent (Windows WPF, .NET 10) Wake-Word-Feature "Okay Computer" zum zuverlaessigen Laufen bringen.
Frank-Workflow: Hoeren laeuft immer im Hintergrund; "Okay Computer" -> Erkennungston (Piep) -> ab Piep
SOFORT normal sprechen (wie manueller Mikrofon-Knopf). KEIN gesprochenes "Ja". Mic-Schalter = manueller
Direkt-Sprech-Modus (zuverlaessiger Fallback).

## Aktueller Status
- Wake-Word-Feature komplett gebaut + 4x gefixt. Alle Commits gepusht, EXE neu gebaut.
- ERLEDIGT (Commits): #46610 Feature, #46611 Mic-Schalter-Fallback+Hintergrund-Lauschen,
  #46612 VAD-Vorfilter entfernt (DER Erkennungs-Killer), #46613 kein "Ja"-Greeting + nach Piep
  sofort sprechbereit (ClearBuffer statt _justWoke-Ueberspringen).
- EXE: VoiceAgent/publish/VoiceAgent.exe (79.7 MB, self-contained single-file) NEU gebaut, startet sauber.
- WARTET AUF: Franks Live-Test der NEUESTEN EXE (#46613): "Okay Computer" -> Piep abwarten -> sprechen.
  Beim letzten Test (#46612-Stand) wurde Weckwort ERKANNT, aber die Aussage NACH dem Piep verworfen
  (Bug, in #46613 behoben). #46613 noch NICHT von Frank live bestaetigt.

## Relevante Dateien
- VoiceAgent/src/VoiceAgent/Core/WakeWordController.cs — Sleep/Wake-State, ProcessFrame (KEIN VAD-Filter mehr), ForceAwake
- VoiceAgent/src/VoiceAgent/MainWindow.xaml.cs — OnWokeAsync (nur Ton+ClearBuffer+ResumeMic), HandleUtteranceAsync (kein _justWoke), MicToggle_Click (ForceAwake/ForceSleep)
- VoiceAgent/src/VoiceAgent/Services/Audio/AlwaysOnListener.cs — OnFrame-Event + ClearBuffer()
- VoiceAgent/src/VoiceAgent/Services/Audio/SherpaWakeWordEngine.cs — sherpa-onnx KWS-Wrapper
- VoiceAgent/src/VoiceAgent/assets/wakeword-model/ — int8-Modell + keywords.txt (NUR "▁OKAY ▁COMP U TER", KEIN @-Marker)
- bugs/desktop/wake-word.md — Almanach (33 Bugs); best-practices/projekt-code/desktop/best-practices-wake-word.md
- publish.ps1 — baut die EXE. Test-Kalibrierung: C:/Users/barwa/AppData/Local/Temp/wake-smoke/

## Getroffene Entscheidungen
- sherpa-onnx KWS (gigaspeech-3.3M int8, englisch, Apache-2.0), gebundelt ~5MB, kein LFS.
- Modell ist ENGLISCH -> Wake-Word "Okay Computer" englische Phonetik. Engine erkennt saubere
  englische Aussprache einwandfrei (TTS-Kontrolltest bei jeder Schwelle erkannt).
- Nach jedem Commit baubarer Apps IMMER die EXE neu bauen (Frank-Regel, [[feedback_build_exe_after_every_commit]]).

## Fehlgeschlagene Ansaetze (WICHTIG — NICHT wiederholen)
- VAD-/Energie-Vorfilter VOR der Streaming-KWS-Engine: zerstoert die Erkennung komplett (verworfene
  Bloecke zerstueckeln den Stream). Bug #33. NIE wieder Frames vor einem Streaming-KWS verwerfen.
- @original-Marker in keywords.txt (z.B. "@OKAY COMPUTER"): sherpa 1.13.2 parst es als Tokens -> Init-Fehler. Bug #32.
- _justWoke (erste Aussage nach Wecken ueberspringen): verwarf Franks echte Anfrage. Entfernt in #46613.
- "Ja?"-Greeting + PauseMic nach Wake: verschluckte/verzoegerte Franks Anfrage. Entfernt.
- dotnet new console im Sandbox schlaegt fehl (Template-Engine) -> csproj/Program.cs manuell schreiben.
- curl auf GitHub-Releases blockiert -> gh release download nutzen.

## Wichtige Recherche-Ergebnisse
- Kontroll-Test (datengetrieben): TTS-"Okay Computer" Batch ERKANNT, Streaming-ohne-VAD ERKANNT,
  Streaming-MIT-VAD NICHT erkannt -> VAD war der Killer.
- Live-Pfad (raw PCM -> PcmConverter -> 100ms-Bloecke ohne VAD) erkennt "OKAY COMPUTER" -> Live-Pfad ok.

## Naechste Schritte (priorisiert)
1. Franks Live-Test der EXE #46613 abwarten/auswerten: "Okay Computer" -> Piep -> sprechen ->
   wird die Aussage JETZT verarbeitet? Log live mitlesen:
   Get-Content "$env:LOCALAPPDATA\VoiceAgent\logs\voiceagent.log" -Wait -Tail 20  (auf CHECKPOINT/gehoert/geantwortet achten)
2. Falls Weckwort bei Franks Stimme grenzwertig erkannt wird: keywords.txt Threshold/Boost justieren
   (z.B. "▁OKAY ▁COMP U TER :2.0 #0.15"), EXE neu bauen.
3. Falls ok: Feature als fertig markieren, project_voiceagent_wakeword_feature.md aktualisieren.

## Offene Fragen
- Funktioniert der Live-Flow (#46613) mit Franks echter Stimme? (einziger unbestaetigter Punkt)

## Anker
- Branch: main
- Letzte Commits:
  7e1882d0 #46613 - nach dem Wecken sofort sprechbereit, kein Ja, ClearBuffer
  062c37c3 #46612 - VAD-Vorfilter entfernt (Erkennungs-Killer), Bug #33
  7fd626cd #46611 - Mic-Schalter Direkt-Modus + Hintergrund-Lauschen
  6578d090 #46610 - Wake-Word-Feature (sherpa-onnx)
