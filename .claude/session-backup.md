# Session Handoff — 2026-06-08 ~22:05

## Ziel
VoiceAgent (Windows-WPF, ~/proggs/VoiceAgent) iterativ verbessern — viele kleine UX-/Logik-Fixes
nacheinander auf Zuruf von Frank. Reine Windows-App, kein Cross-Platform-Gegenstueck.

## Aktueller Status (alles ERLEDIGT, committed, gepusht, gebaut, App laeuft)
- #46636 Einschlafton beim Stille-Timeout (Sound, WakeWordController.Slept gibt reason mit)
- #46637 Settings-UI Sidebar-Navigation (TabControl, 6 Kategorien)
- #46638 Sidebar-Reiter springen nicht mehr (TabPanel->StackPanel) + Agenten-Platzhalter-Reiter
- #46639 Einschlafton gewechselt auf Freesound #422515 (sleep.wav, 0.25s)
- #46640 App-Statuszeile zeigt eingestelltes Weckwort statt hartcodiert "Okay Computer"
- #46641 Statuszeile zeitgenau (OnSpeechStart -> "Hoert zu" ab 1. ms) + Gedankenstriche -> Komma
- #46642 Erinnerungen ohne Uhrzeit (Reminder.OnNextStart, Tageszeiten, "beim Start")
- #46643(mein, kam ueber paralleler Session-Commit #46645) KI-Antwort LIVE mitlesbar
  (RespondAndSpeakAsync: BeginAgentLine, Satz fuer Satz waehrend Streaming statt Append am Ende)
- #46648 Weckwort-Aenderung wirkt jetzt WIRKLICH (Kern-Fix dieser Runde, s.u.)
- #46649 Bug-Almanach wake-word #34 dokumentiert

- In Arbeit: nichts offen. Letzte Aufgabe (Weckwort) ist fertig + verifiziert.
- Offene Vorschlaege (Frank hat noch nicht ja/nein gesagt): Weckwort-Test-Knopf; freie
  Weckwort-Eingabe (echter C#-SentencePiece-Encoder); publish.ps1 selbstheilend; Token-weises
  Live-Tippen; Multi-Session-Build-Schutz.

## Weckwort-Fix Details (#46648, der letzte + wichtigste)
Bug: Weckwort in Einstellungen aendern wirkte NICHT — Erkennung blieb auf "Okay Computer".
Ursache: KWS-Modell (sherpa-onnx gigaspeech) erkennt BPE-TOKENS aus keywords.txt, nicht Klartext.
keywords.txt war fest "▁OKAY ▁COMP U TER". WakeWord-Setting war nur Anzeigetext.
Fix: Neue Datei Core/WakeWords.cs = kuratierte Liste mit VOR-tokenisierten BPE-Keywords (einmal
mit bpe.model via Python sentencepiece erzeugt, verifiziert). WakeWords.EnsureKeywordsFile schreibt
keywords.txt aus dem gewaehlten Wort nach %LOCALAPPDATA%\VoiceAgent\wakeword-keywords.txt.
SherpaWakeWordEngine bekam keywordsFile-Parameter. MainWindow.RebuildWakeController erzeugt sie
aus _settings.WakeWord. SettingsWindow: WakeWordBox jetzt ComboBox (Auswahlliste) statt TextBox.
VERIFIZIERT: wakeword-keywords.txt enthaelt "▁COMP U TER" fuer "Computer", Log bestaetigt.

## Relevante Dateien
- VoiceAgent/src/VoiceAgent/MainWindow.xaml.cs — gross (~1100 Z.); RespondAndSpeakAsync (live
  Antwort), RebuildWakeController (keywords), OnSpeechStart (live status), OnSlept (sleep chime)
- VoiceAgent/src/VoiceAgent/Core/WakeWords.cs — NEU, kuratierte Weckwort-Liste
- VoiceAgent/src/VoiceAgent/Services/Audio/SherpaWakeWordEngine.cs — keywordsFile-Param
- VoiceAgent/src/VoiceAgent/Services/Audio/AlwaysOnListener.cs — OnSpeechStart-Event
- VoiceAgent/src/VoiceAgent/Core/Reminder*.cs — OnNextStart, Tageszeiten
- VoiceAgent/src/VoiceAgent/Views/SettingsWindow.xaml(.cs) — Sidebar + WakeWord-ComboBox
- VoiceAgent/publish.ps1 — Build (self-contained single-file VoiceAgent.exe ~80MB)

## Getroffene Entscheidungen
- Weckwort: kuratierte Auswahlliste statt freier Eingabe (freier Tokenizer waere fragil/schwer).
- VoiceAgent = reine Windows-App -> Status immer "Committed und gepusht." (NICHT plattformuebergreifend)
- Frank nutzt publish\VoiceAgent.exe (~80MB), NICHT bin\Release. Nach JEDER Aenderung publish.ps1.

## Fehlgeschlagene Ansaetze (WICHTIG — nicht wiederholen)
- Microsoft.ML.Tokenizers fuer Laufzeit-SentencePiece: KEINE offensichtliche Stream-Create-API,
  schleppt Google.Protobuf mit. NICHT der Weg. Stattdessen vor-tokenisierte Liste.
- response.Text als string behandeln: FALSCH, es ist IAsyncEnumerable<string> (Token-Stream).
- Browser via cmd start / Start-Process oeffnet oft KEIN Fenster -> chrome.exe direkt mit file-URL.
- publish.ps1 bricht ab wenn VoiceAgent.exe laeuft (EXE gesperrt) -> VORHER Instanz beenden:
  Get-CimInstance Win32_Process | ?{$_.Name -eq 'VoiceAgent.exe'} | %{Stop-Process -Id $_.ProcessId -Force}

## Parallele Sessions (KRITISCH)
Mehrere andere Claude-Sessions arbeiten GLEICHZEITIG am VoiceAgent (Codex-Login/CodexProvider,
SubAgent-Routing/Helfer, Computer-Use). Sie nutzen teils `git add -A` und nehmen fremde
uncommittete Arbeit mit; ihr Code war 2x in nicht-kompilierendem Zwischenzustand und blockierte
den Build. IMMER: nur EIGENE Dateien namentlich stagen, fetch+rebase, Build kann durch parallele
WIP brechen (dann ist es NICHT der eigene Fehler — pruefen welche Datei).

## Naechste Schritte (priorisiert)
1. Auf Frank warten — er gibt die naechste kleine VoiceAgent-Aufgabe per Sprache (oft mit " ; ").
2. Falls Frank einen der offenen Vorschlaege bestaetigt: Weckwort-Test-Knopf ODER freie
   Weckwort-Eingabe ODER publish.ps1 selbstheilend.
3. Nach JEDER Aenderung: commit+push -> publish.ps1 (Instanz vorher beenden!) -> starten -> Log
   pruefen (%LOCALAPPDATA%\VoiceAgent\logs\voiceagent.log).

## Offene Fragen
- Keine blockierenden. Zwei Intelligenz-Vorschlaege stehen unbeantwortet (Frank kann ignorieren).

## Anker
- Branch: main
- Letzte Commits:
db6d1556f #46649 - Bug-Almanach wake-word #34
656d4221b #46648 - VoiceAgent: Weckwort-Aenderung wirkt jetzt wirklich
fabc8cbec #46644 - VoiceAgent Codex Bugfix 2 (parallele Session)
d60f468e7 #46647 - Bug-Almanach bugs/apis/ (parallele Session)
c169514b2 #46646 - VoiceAgent Computer Use Sicherheits-Kern (parallele Session)
