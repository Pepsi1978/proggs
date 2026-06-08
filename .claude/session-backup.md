# Session Handoff — 2026-06-08 (Nacht, ~03:30)

## Ziel (1-3 Saetze)
VoiceAgent (Windows-WPF-App, .NET 10, ~/proggs/VoiceAgent) verbessern. Zwei Themen in dieser
Session abgeschlossen: (1) Minimieren-ins-Tray-Feature, (2) Recherche + Almanach + Best-Practices
zur Whisper/Groq-Stille-Halluzination ("Vielen Dank" bei Stille). OFFEN fuer morgen: den
recherchierten Fix tatsaechlich in den VoiceAgent-Code einbauen.

## Aktueller Status
- Erledigt #46622: Minimieren ins Tray (Infobereich) statt Taskleiste, in Einstellungen umschaltbar
  (AppSettings.MinimizeToTray default true, Services/TrayIcon.cs neu = WinForms NotifyIcon,
  MainWindow StateChanged/Restore/Exit, SettingsWindow Checkbox). publish.ps1 gebaut (79,9 MB EXE).
- Erledigt #46623: bug-almanac dotnet-csharp §6.7 (UseWindowsForms+UseWPF+ImplicitUsings -> CS0104,
  Fix via <Using Remove>).
- Erledigt #46624: neuer Almanach bugs/desktop/groq-transkription.md + best-practices Gegenseite.
- Erledigt #46625: best-practices-groq-transkription.md BREIT ausgebaut (Batch-API, Performance,
  .NET-Resilienz, Architektur, Kosten, Audio-Preprocessing, Observability/Fallback) + Almanach-
  Bezugstabelle. Parallele Session hat zusaetzlich den bug-almanac-guard .cs-content-probe fuer
  Groq-STT-Code nachgezogen (auch #46625, commit 6fab37da0).
- In Arbeit: nichts offen im Code — alles committed + gepusht.
- Blockiert: nichts.

## Relevante Dateien
- `VoiceAgent/src/VoiceAgent/Services/GroqWhisperClient.cs` — HIER der Fix: nutzt aktuell
  response_format=text, temperature=0, feste Retries [2,4,8]s, liest retry-after NICHT. Umstellen auf
  verbose_json + Confidence-Gate (no_speech_prob>0.6 AND avg_logprob<-1.0; compression_ratio>2.4).
- `VoiceAgent/src/VoiceAgent/Services/Audio/AlwaysOnListener.cs` — HIER Schicht 1: FinalizeUtterance
  prueft nur Dauer (MinUtteranceMs), NICHT Sprachgehalt. Voiced-Dauer mitzaehlen, sprach-arme Clips
  verwerfen (neue Settings MinVoicedMs/MinVoicedRatio, konservativ).
- `bugs/desktop/groq-transkription.md` — Almanach mit dem konkreten 4-Schicht-Fix-Vorschlag (Abschnitt
  "VoiceAgent — konkreter Fix-Vorschlag").
- `best-practices/projekt-code/desktop/best-practices-groq-transkription.md` — wie man es richtig baut
  (inkl. fertiges STJ-Source-Gen-DTO fuer verbose_json, Polly-Resilienz-Muster).
- `VoiceAgent/publish.ps1` — Pflicht nach JEDER Code-Aenderung ausfuehren (Frank nutzt publish/VoiceAgent.exe).

## Getroffene Entscheidungen
- Tray via WinForms NotifyIcon (nicht eigener Shell_NotifyIcon-Nachbau) — handhabt Explorer-Neustart
  (#6.1) + Kontextmenue (#6.3) selbst. Kollision geloest via <Using Remove System.Drawing/System.Windows.Forms> im csproj.
- MinimizeToTray Default = true (Franks Wunsch). X-Schliessen beendet weiterhin die App.
- Groq-Stille-Halluzination ist Whisper-Modellverhalten (per Design, large-v3 schlimmer) — NICHT
  fixbar im Modell, nur durch funktionserhaltende Abwehr-Kette.
- Confidence-Gate IMMER mit UND (no_speech_prob>0.6 AND avg_logprob<-1.0), nie ODER — schuetzt leise Sprache.

## Fehlgeschlagene Ansaetze (WICHTIGSTER ABSCHNITT)
- temperature=0 allein verhindert Stille-Halluzination NICHT (ist schon gesetzt) — Whisper faehrt
  intern Fallback-Temperatur hoch. NICHT als alleinige Loesung versuchen.
- initial_prompt verhindert Stille-Halluzination NICHT zuverlaessig (nur Stil-Lenkung).
- Floskel-Blocklist ALLEIN reicht nicht (faengt nur bekannte Phrasen) — nur als letzter Filter NACH VAD/Confidence.
- whisper.cpp suppress_non_speech_tokens=true macht es SCHLIMMER (nicht nachbauen).
- Bei Groq KEINE Whisper-internen Parameter (no_speech_threshold, condition_on_previous_text) senden
  -> 400 BadRequest. Filterung MUSS client-seitig ueber verbose_json laufen.
- Audio NICHT vor Whisper denoisen/normalisieren/AGC -> verschlechtert die Genauigkeit messbar.

## Wichtige Recherche-Ergebnisse
- verbose_json kostet bei Groq KEINE Mehrlatenz/Mehrkosten vs. text (nur word-Timestamps kosten extra)
  -> Umstieg ist "gratis", liefert no_speech_prob/avg_logprob/compression_ratio.
- Upload-POST NICHT automatisch retryen (Doppel-Transkription/-Abrechnung) -> DisableForUnsafeHttpMethods;
  retry-after-Header bei 429 auswerten.
- whisper-large-v3-turbo = richtiger Default (DE-Diktat, $0.04/h). Groq Batch API = 50% guenstiger (nur nicht-Live).
- Min-Billing 10s/Clip, free 20 RPM org-weit, 7.200 Audio-Sek/h -> VAD-Vorfilter spart Geld + verhindert Bug.
- HF-Dataset sachaarbonel/whisper-hallucinations (7.890 Floskeln DE+EN) als Blocklist-Quelle.

## Naechste Schritte (priorisiert)
1. Fix-Kette Schicht 1+2 in den VoiceAgent einbauen: (a) AlwaysOnListener Voiced-Dauer-Vorfilter,
   (b) GroqWhisperClient auf verbose_json + Confidence-Gate (UND-Logik) umstellen. KEIN Retry auf POST,
   retry-after auswerten. Danach publish.ps1 bauen. Commit+Push VOR Build.
2. Optional Schicht 3: Floskel-Blocklist (normalisiert, nur bei kurz+Stille-Kontext).
3. Optional: verbose_json per STJ Source-Generator deserialisieren (DTO steht in der best-practices-Datei).
4. Optional: Qualitaetsfelder (no_speech_prob, avg_logprob, duration) als JSON-Lines loggen (Observability).
5. Frank wollte evtl. auch die offenen Tray-Vorschlaege (Schliessen->Tray, Autostart, Tray-Status-Icon).

## Offene Fragen
- Soll der Code-Fix jetzt komplett (alle 4 Schichten) oder erst Schicht 1+2 (groesster Effekt, kleinstes Risiko)?
- Schwellen final: MinVoicedMs ~200ms / MinVoicedRatio ~15% ok? (konservativ, kurze Befehle nicht abschneiden)

## Anker
- Branch: main
- Letzte Commits:
6d5fad148 #46625 - best-practices groq-transkription: broaden beyond bug-prevention (...)
6fab37da0 #46625 - bug-almanac-guard: .cs content-probe routes Groq STT code to groq-transkription.md
20e8f41cf #46624 - new bug-almanac desktop/groq-transkription.md + best-practices
b00adbb9c #46623 - bug-almanac dotnet-csharp §6.7: UseWindowsForms+UseWPF+ImplicitUsings -> CS0104
7c30f390f #46622 - VoiceAgent: minimieren ins Tray (Infobereich) statt Taskleiste, umschaltbar
