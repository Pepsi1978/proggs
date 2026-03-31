# BestJournalAndroid — Setup-Anleitung fuer neue Plattformen

## Uebersicht

BestJournalAndroid ist die Play Store Version von Best Journal.
Sie nutzt Firebase AI Logic statt direkter Gemini API — kein User-API-Key noetig.

## Was im Repo ist

Alles ausser `google-services.json` — die ist in `.gitignore` weil sie Firebase-Credentials enthaelt.

## Setup auf neuem Rechner (z.B. macOS)

### 1. google-services.json herunterladen

1. Oeffne https://console.firebase.google.com
2. Projekt: **BestJurnal** (Projekt-ID: `bestjurnal-a15b9`)
3. Einstellungen (Zahnrad) → Allgemein → Runterscrollen zu "Meine Apps"
4. Bei "Best Journal" auf **google-services.json herunterladen** klicken
5. Datei nach `BestJournalAndroid/app/google-services.json` kopieren

### 2. SHA-1 Fingerabdruck hinzufuegen (pro Rechner!)

Jeder Rechner hat einen eigenen Debug-Keystore mit eigenem SHA-1.

```bash
# SHA-1 auslesen:
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android | grep SHA1
```

Diesen SHA-1 in Firebase hinzufuegen:
1. Firebase Console → Einstellungen → Allgemein
2. Bei "Best Journal Debug" (com.bestjournal.app.debug) → "Fingerabdruck hinzufuegen"
3. SHA-1 einfuegen → Speichern

### 3. Google Drive API (bereits aktiviert)

Die Google Drive API ist im Projekt bereits aktiviert.
Falls Probleme: https://console.cloud.google.com/apis/library/drive.googleapis.com?project=bestjurnal-a15b9

### 4. Build

```bash
cd ~/proggs/BestJournalAndroid
./gradlew assembleDebug
```

### 5. Install auf Geraet

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Firebase-Projekt Details

| Feld | Wert |
|------|------|
| Projekt-ID | bestjurnal-a15b9 |
| Projekt-Nummer | 314424175660 |
| Web Client ID | 314424175660-12m82j2jgvmc0tcgl61igghp67nnpmn1.apps.googleusercontent.com |
| Plan | Spark (kostenlos) |
| KI | Gemini Developer API (AI Logic) |
| Auth | Google Sign-In |
| Windows SHA-1 | 03:03:2C:A7:C1:ED:5F:C2:E0:6C:51:84:59:08:F2:4D:C3:35:9B:FA |
| macOS SHA-1 | TODO — beim ersten macOS-Build auslesen und in Firebase eintragen |

## Unterschied zu BestJournalFrank

| | BestJournalFrank | BestJournalAndroid |
|---|---|---|
| Package | com.entropyjournal | com.bestjournal.app |
| KI | Gemini API direkt (eigener Key) | Firebase AI Logic |
| Drive Backup | entropy_journal_backup.db | bestjournal_backup.db |
| Groq API | Ja | Nein (nur Offline Whisper) |
| Settings | API-Key Felder | KI-Abo Bereich |
