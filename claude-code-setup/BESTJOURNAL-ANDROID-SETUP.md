# BestJournalAndroid — Setup-Anleitung fuer neue Plattformen

## Uebersicht

BestJournalAndroid ist die Play Store Version von Best Journal.
Sie nutzt Firebase AI Logic statt direkter Gemini API — kein User-API-Key noetig.

## Was im Repo ist

Alles ausser `google-services.json` — die ist in `.gitignore` weil sie Firebase-Credentials enthaelt.

## Setup auf neuem Rechner

### 0. Git LFS installieren (PFLICHT vor dem ersten Build!)

Die sherpa-onnx AAR-Datei (47 MB) wird ueber Git LFS verwaltet.
Ohne Git LFS ist die Datei nur ein Pointer und der Build schlaegt fehl.

```bash
# macOS:
brew install git-lfs

# Windows:
# Git LFS ist bei Git for Windows bereits dabei

# Beide Plattformen — einmalig initialisieren:
git lfs install
git lfs pull
```

### 1. google-services.json herunterladen

1. Oeffne https://console.firebase.google.com
2. Projekt: **BestJurnal** (Projekt-ID: `bestjurnal-a15b9`)
3. Einstellungen (Zahnrad) → Allgemein → Runterscrollen zu "Meine Apps"
4. Bei "Best Journal" auf **google-services.json herunterladen** klicken
5. Datei nach `BestJournalAndroid/app/google-services.json` kopieren

Die Datei ist in `.gitignore` und wird NICHT ueber Git synchronisiert.
Sie muss auf jedem Rechner einzeln heruntergeladen werden.

### 2. SHA-1 Fingerabdruck hinzufuegen (pro Rechner!)

Jeder Rechner hat einen eigenen Debug-Keystore mit eigenem SHA-1.

```bash
# SHA-1 auslesen:
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android | grep SHA1
```

Diesen SHA-1 in Firebase hinzufuegen:
1. Firebase Console → Einstellungen → Allgemein
2. **WICHTIG: Bei "Best Journal Debug" (com.bestjournal.app.debug) eintragen!**
   NICHT bei "Best Journal" (com.bestjournal.app) — das ist die Release-App.
   Der Fingerabdruck muss beim DEBUG-Paket stehen, sonst schlaegt Google Sign-In fehl.
3. SHA-1 einfuegen → Speichern
4. **DANACH neue google-services.json herunterladen** (die alte enthaelt den neuen Fingerabdruck nicht)

### 3. Google Drive API (bereits aktiviert)

Die Google Drive API ist im Projekt bereits aktiviert.
Falls Probleme: https://console.cloud.google.com/apis/library/drive.googleapis.com?project=bestjurnal-a15b9

### 4. Build

```bash
cd ~/proggs/BestJournalAndroid
chmod +x gradlew   # macOS: Execute-Permission setzen
./gradlew assembleDebug
```

### 5. Install auf Geraet

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 6. Hinweis: Signatur-Inkompatibilitaet zwischen Rechnern

Wenn die App bereits von einem ANDEREN Rechner installiert ist, schlaegt das Update fehl
("signatures do not match"). Loesung: Alte Version deinstallieren, neue installieren.
Daten gehen verloren — vorher Drive-Backup machen!

```bash
adb uninstall com.bestjournal.app.debug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Alternative: Debug-Keystore vom anderen Rechner kopieren (~/.android/debug.keystore).

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
| macOS SHA-1 | 50:DB:D4:95:94:3D:4F:00:83:F3:C9:0B:E0:B6:C4:5E:9D:54:16:AF |

## Google Play Developer Account

| Feld | Wert |
|------|------|
| Entwicklername | Barwandt Digital Labs |
| Kontotyp | Privates Konto |
| Konto-ID | 6096203479331369923 |
| E-Mail | barwandt@gmail.com |
| Support-E-Mail | dev.app.support@gmail.com |
| Registriert am | 2026-03-31 |
| Status | Identitaetspruefung ausstehend (Ausweis hochladen) |

### Noch zu erledigen (Play Console)
1. Identitaet bestaetigen (Ausweis hochladen)
2. Kontakttelefonnummer bestaetigen (geht erst nach Ausweis)
3. App in Play Console anlegen
4. Abo-Produkte erstellen: `bestjournal_ai_monthly` (€4,99) + `bestjournal_ai_yearly` (€39,99)
5. Lizenztester eintragen (barwandt@gmail.com) fuer kostenlose Testkauefe
6. Play Store Listing (Screenshots, Beschreibung, etc.)

## Abo-Preise und Kostenkontrolle (implementiert am 2026-03-31)

| | Preis |
|---|---|
| Monatsabo | €4,99 |
| Jahresabo | €39,99 (33% Ersparnis) |

### Kostenkontrolle (im Code implementiert)
- Dashboard-Limit: 16 pro Tag (still) → Cooldown 30 Min. → ab 20: "morgen wieder"
- Entry-Limit fuer Analyse: 40 Eintraege max (Trial + Abo), 10 fuer Free
- Spam-Schutz: 25 KI-Aufrufe pro Stunde max
- Eintraege (Textverbesserung + Zusammenfassung): Unbegrenzt (kosten fast nichts)
- Details: `docs/superpowers/specs/2026-03-31-token-kalkulation-abo-preise-design.md`

### Fair-Use-Klausel (noch nicht implementiert)
Im Play Store und AGB KEINE konkreten Zahlen nennen. Stattdessen:
- "Umfangreiche KI-Analysen fuer dein Tagebuch" (vage)
- Fair-Use-Klausel in den AGB mit Aenderungsvorbehalt

## Unterschied zu BestJournalFrank

| | BestJournalFrank | BestJournalAndroid |
|---|---|---|
| Package | com.entropyjournal | com.bestjournal.app |
| KI | Gemini API direkt (eigener Key) | Firebase AI Logic |
| Drive Backup | entropy_journal_backup.db | bestjournal_backup.db |
| Groq API | Ja | Nein (nur Offline Whisper) |
| Settings | API-Key Felder | KI-Abo Bereich |
