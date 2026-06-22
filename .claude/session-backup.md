# Session Handoff — 2026-06-22, später Abend

## Ziel (1-3 Sätze)
Voice-Overlays (TVO + CVO, Windows C# + macOS Swift): Gemini-Korrektur-Prompts voll
editierbar machen + Wörterbuch abschaltbar + alles per Google-Drive-Backup syncen.
"Etappe 2" inkl. Settings-Redesign ist umgesetzt; Windows komplett gebaut+live, Mac code-fertig.

## Laufende/unterbrochene Aufgabe — EXAKTER Wiedereinstiegspunkt
Keine laufende Aufgabe, letzter Stand sauber abgeschlossen und committed (#47087).
Frank hat das Backup ohne ESC-Unterbrechung ausgelöst. Es gibt KEINE eigenen uncommitteten
Overlay-Dateien (git status sauber für die VoiceOverlay-Projekte).

## Aktueller Status
- Erledigt + LIVE auf Windows (gebaut via rebuild-overlay.ps1):
  - Start-Default "kein Profil aktiv / Gemini aus" (#47077)
  - Wörterbuch-EIN/AUS-Schalter, Default aus (#47078; Datei ~/SK/VoiceOverlays/vocabulary-enabled.txt)
  - Etappe 2a Platzhalter-Engine {{TEXT}}/{{WOERTERBUCH}} in allen 4 GeminiClients (#47079)
  - Etappe 2b Windows-Prompt-Editor: GeminiPromptListDialog + GeminiPromptEditDialog,
    Knopf "Gemini-Prompts bearbeiten…" im SettingsDialog (#47080)
  - Settings-Redesign Windows = Karten-Layout (#47081)
  - Etappe 2d Windows Backup-Sync: GeminiPromptDriveSync.cs (Bundle gemini-prompts-bundle.json,
    LWW per Timestamp) (#47084), sichtbare Sync-Bestätigung (#47085),
    editierbare Wörterbuch-Präambel vocabulary-preamble.txt (#47086)
  - Win-Versionen: CVO 2.1.23, TVO 1.4.27
- Erledigt im CODE, Mac NOCH NICHT von mir gebaut (Frank baut per build.sh):
  - Etappe 2c Mac-Prompt-Editor (#47082), Mac-Wörterbuch-Checkbox (#47083),
    Mac-Backup-Sync = enum GeminiPromptSync + uploadGeminiPrompts/downloadGeminiPrompts +
    Mac-Präambel-aus-Datei (#47087). Mac-Versionen: 1.14/1.15 (CFBundleVersion/Short).
- Offen (Folge-Aufgaben, NICHT begonnen):
  - Mac-Apps bauen: `bash build.sh` in beiden Mac-Overlay-Ordnern (Version 1.15)
  - Mac-Settings-Karten-Look (rein kosmetisch, AppKit NSBox-Umbau)
  - Mac-Settings-Editier-FELD für die Präambel (Mac liest sie, editiert wird nur auf Windows)
  - Echter Drive-Roundtrip-Test (Win<->Mac) durch Frank
  - 2 offene Intelligenz-Vorschläge (Frank hat noch nicht geantwortet): (1) Backup-Sync-Fehler
    ins diag.log loggen; (2) Bug-Almanach-Lesemarker übersteht Session-Resets nicht (nervte mehrfach)

## Relevante Dateien
- {Terminal,Claude}VoiceOverlay-Windows/Services/GeminiClient.cs — Engine BuildPrompt, public API (ProfileLabel/EffectivePrompt/SaveProfilePrompt/EffectiveVocabularyPreamble), Wörterbuch-Schalter + Präambel
- {Terminal,Claude}VoiceOverlay-Windows/Services/GeminiPromptDriveSync.cs — Backup-Bundle-Sync (LWW), UploadSucceeded-Event
- {Terminal,Claude}VoiceOverlay-Windows/Views/GeminiPromptListDialog.xaml(.cs) + GeminiPromptEditDialog.xaml(.cs) — Editor-UI (NEU; NICHT mit bestehendem PromptEditDialog verwechseln!)
- {Terminal,Claude}VoiceOverlay-Windows/Views/SettingsDialog.xaml(.cs) — Karten-Layout, Wörterbuch-Checkbox + Präambel-Feld + Prompts-Knopf
- {Terminal,Claude}VoiceOverlay-Windows/Views/OverlayWindow.xaml.cs — geminiEnabled=false/_activeProfile=0 default; GeminiPromptDriveSync.TrySyncFromCloud beim Start + UploadSucceeded-Abo
- *-macOS/.../GeminiClient.swift — buildPrompt, public API, Präambel-aus-Datei
- *-macOS/.../PromptBoardDialogs.swift — GeminiPromptListDialog/EditDialog (AppKit) + PBSettingsDialog (Checkbox + Prompts-Knopf)
- *-macOS/.../GoogleDriveBackupService.swift — uploadGeminiPrompts/downloadGeminiPrompts + enum GeminiPromptSync (am Dateiende)
- *-macOS/.../AppDelegate.swift — GeminiPromptSync.trySyncFromCloud() neben mergeVocabularyFromCloudOnLaunch
- Memory: ~/.claude/projects/C--Users-barwa-proggs/memory/project_voice_overlay_personal_vocabulary.md (voller Etappe-2-Stand)

## Getroffene Entscheidungen
- Editor-Fenster heißen GeminiPromptListDialog/GeminiPromptEditDialog — NICHT PromptEditDialog (existiert schon als PromptBoard-Editor, 14KB; fast überschrieben!)
- Backup = EIN Bundle (gemini-prompts-bundle.json), LWW per savedAt-Timestamp, lokaler Marker .gemini-prompts-synced
- savedAt-Format MUSS identisch Win/Mac sein: "yyyy-MM-ddTHH:mm:ss'Z'" (Win) == ISO8601DateFormatter() (Mac, sekundengenau). NIE auf "o" (7 Nachkommastellen) zurück — bricht cross-platform LWW-Stringvergleich.
- personal-vocabulary.txt NICHT im Prompt-Bundle (hat eigenen verlustfreien Union-Sync PromptVocabularyDriveSync)
- Wörterbuch-Schalter steuert das LADEN (vocabulary-enabled.txt), nicht die Daten — Drive-Union belebt die Wortliste zwar, sie wird aber bei aus nicht benutzt
- Settings-Struktur: Frank wählte Karten-Layout (nicht Reiter)
- Mac baut Frank IMMER selbst per build.sh (build.sh hat EXPLIZITE Dateiliste — neue .swift müssen rein; deshalb neue Klassen in BESTEHENDE Dateien gelegt)

## Fehlgeschlagene Ansätze (WICHTIG)
- Dateiname PromptEditDialog.xaml NICHT verwenden für neuen Editor — existiert bereits (PromptBoard). Eindeutige Namen GeminiPrompt* nehmen.
- Bug-Almanach-BP-Guard (dotnet-csharp / swift-appkit / macos-overlay) feuerte mehrfach mitten in der Session erneut (Session-State-Reset); gecachtes Re-Read zählte NICHT — half nur ein Read mit ANDEREM limit (echtes Read-Event). Wenn das wieder nervt: Read mit frischem limit, dann Edit erneut.

## Nächste Schritte (priorisiert)
1. Falls Frank fragt: auf eine offene Intelligenz-Vorschlags-Antwort warten (Sync-Fehler-Log / Guard-Marker).
2. Mac-Settings-Präambel-Editierfeld + Mac-Settings-Karten-Look (die einzigen offenen Etappe-2-Reste).
3. Optional: Backup-Upload-Fehler ins diag.log schreiben (Vorschlag 1).

## Offene Fragen
- 2 Intelligenz-Vorschläge am Ende des letzten Turns noch unbeantwortet (Sync-Fehler-Log; Guard-Marker-Persistenz).

## Anker
- Branch: main
- Letzte Commits:
80266510e #47087 - Voice Overlays Etappe 2c/2d (Mac): Gemini prompt+switch+preamble Drive backup sync
dc83a5d97 #47086 - Voice Overlays (Windows): editable vocabulary preamble
18dc34df5 #47085 - Voice Overlays (Windows): visible sync confirmation after upload
cada77702 #47084 - Voice Overlays Etappe 2d (Windows): Google Drive backup sync
(davor: #47080-47083 Editor-UI Win+Mac, #47081 Settings-Karten, #47077-47079 Default/Schalter/Engine)
