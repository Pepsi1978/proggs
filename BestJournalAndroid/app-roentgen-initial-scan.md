# App-Roentgen Initial-Scan

**Datum:** 2026-06-10
**App-Verzeichnis:** /c/Users/barwa/proggs/BestJournalAndroid
**Manifest:** app/src/main/AndroidManifest.xml
**Build-Gradle:** app/build.gradle.kts
**Strings.xml:** app/src/main/res/values/strings.xml

Dieser Initial-Scan ist die maschinelle Vorarbeit fuer die 7-Schichten-Tiefenanalyse.
Die Detail-Auswertung macht Claude in einem zweiten Schritt.

---

## 0. App-Groesse

| Datei-Typ | Anzahl |
|-----------|--------|
| Kotlin (.kt) | 143 |
| Java (.java) | 0 |
| Resource XML | 36 |
| build.gradle* | 2 |
| Gradle-Module (mit src/main/) | 0
0 |

## Schicht 1 — Manifest-Daten

### 1.1 Permissions

```
android.permission.ACCESS_NETWORK_STATE
android.permission.CAMERA
android.permission.INTERNET
android.permission.POST_NOTIFICATIONS
android.permission.RECEIVE_BOOT_COMPLETED
android.permission.RECORD_AUDIO
```

### 1.2 Activities

```
.MainActivity
```

### 1.3 Services

```
(keine gefunden)
```

### 1.4 Receiver

```
android:name=".util.MonthlyReviewReceiver"
android:name=".util.YearlyReviewReceiver"
```

### 1.5 Provider

```
android:authorities="${applicationId}.fileprovider"
```

### 1.6 Deep-Links und Intent-Filter-Schemes

```
(keine gefunden)
```

### 1.7 Intent-Actions in der App

```
action android:name="android.intent.action.BOOT_COMPLETED"
action android:name="android.intent.action.MAIN"
action android:name="android.intent.action.TIMEZONE_CHANGED"
action android:name="android.intent.action.TIME_SET"
```

### 1.8 Backup-Konfig

```
        android:allowBackup="true"
        android:fullBackupContent="@xml/backup_rules"
        android:dataExtractionRules="@xml/data_extraction_rules"
```

## Schicht 1.5 — Assets-Scan (Legal- und Web-Dokumente)

Assets-Verzeichnis: `app/src/main/assets`

Gefundene Dateien: 82

### 1.5.1 Privacy-Dokumente
  [ar] app/src/main/assets/legal/ar/PRIVACY.html
  [bn] app/src/main/assets/legal/bn/PRIVACY.html
  [de] app/src/main/assets/legal/de/DATENSCHUTZ.html
  [default] app/src/main/assets/legal/zh-TW/PRIVACY.html
  [en] app/src/main/assets/legal/en/PRIVACY.html
  [es] app/src/main/assets/legal/es/PRIVACY.html
  [fr] app/src/main/assets/legal/fr/PRIVACY.html
  [gu] app/src/main/assets/legal/gu/PRIVACY.html
  [hi] app/src/main/assets/legal/hi/PRIVACY.html
  [id] app/src/main/assets/legal/id/PRIVACY.html
  [it] app/src/main/assets/legal/it/PRIVACY.html
  [ja] app/src/main/assets/legal/ja/PRIVACY.html
  [kn] app/src/main/assets/legal/kn/PRIVACY.html
  [ko] app/src/main/assets/legal/ko/PRIVACY.html
  [ml] app/src/main/assets/legal/ml/PRIVACY.html
  [mr] app/src/main/assets/legal/mr/PRIVACY.html
  [nl] app/src/main/assets/legal/nl/PRIVACY.html
  [pl] app/src/main/assets/legal/pl/PRIVACY.html
  [ta] app/src/main/assets/legal/ta/PRIVACY.html
  [te] app/src/main/assets/legal/te/PRIVACY.html
  [th] app/src/main/assets/legal/th/PRIVACY.html
  [tr] app/src/main/assets/legal/tr/PRIVACY.html
  [uk] app/src/main/assets/legal/uk/PRIVACY.html
  [ur] app/src/main/assets/legal/ur/PRIVACY.html

### 1.5.2 Impressum-Dokumente
  [ar] app/src/main/assets/legal/ar/IMPRINT.html
  [bn] app/src/main/assets/legal/bn/IMPRINT.html
  [de] app/src/main/assets/legal/de/IMPRESSUM.html
  [default] app/src/main/assets/legal/zh-TW/IMPRINT.html
  [en] app/src/main/assets/legal/en/IMPRINT.html
  [es] app/src/main/assets/legal/es/IMPRINT.html
  [fr] app/src/main/assets/legal/fr/IMPRINT.html
  [gu] app/src/main/assets/legal/gu/IMPRINT.html
  [hi] app/src/main/assets/legal/hi/IMPRINT.html
  [id] app/src/main/assets/legal/id/IMPRINT.html
  [it] app/src/main/assets/legal/it/IMPRINT.html
  [ja] app/src/main/assets/legal/ja/IMPRINT.html
  [kn] app/src/main/assets/legal/kn/IMPRINT.html
  [ko] app/src/main/assets/legal/ko/IMPRINT.html
  [ml] app/src/main/assets/legal/ml/IMPRINT.html
  [mr] app/src/main/assets/legal/mr/IMPRINT.html
  [nl] app/src/main/assets/legal/nl/IMPRINT.html
  [pl] app/src/main/assets/legal/pl/IMPRINT.html
  [ta] app/src/main/assets/legal/ta/IMPRINT.html
  [te] app/src/main/assets/legal/te/IMPRINT.html
  [th] app/src/main/assets/legal/th/IMPRINT.html
  [tr] app/src/main/assets/legal/tr/IMPRINT.html
  [uk] app/src/main/assets/legal/uk/IMPRINT.html
  [ur] app/src/main/assets/legal/ur/IMPRINT.html

### 1.5.3 Terms/AGB-Dokumente
  [ar] app/src/main/assets/legal/ar/TERMS.html
  [bn] app/src/main/assets/legal/bn/TERMS.html
  [de] app/src/main/assets/legal/de/NUTZUNGSBEDINGUNGEN.html
  [default] app/src/main/assets/legal/zh-TW/TERMS.html
  [en] app/src/main/assets/legal/en/TERMS.html
  [es] app/src/main/assets/legal/es/TERMS.html
  [fr] app/src/main/assets/legal/fr/TERMS.html
  [gu] app/src/main/assets/legal/gu/TERMS.html
  [hi] app/src/main/assets/legal/hi/TERMS.html
  [id] app/src/main/assets/legal/id/TERMS.html
  [it] app/src/main/assets/legal/it/TERMS.html
  [ja] app/src/main/assets/legal/ja/TERMS.html
  [kn] app/src/main/assets/legal/kn/TERMS.html
  [ko] app/src/main/assets/legal/ko/TERMS.html
  [ml] app/src/main/assets/legal/ml/TERMS.html
  [mr] app/src/main/assets/legal/mr/TERMS.html
  [nl] app/src/main/assets/legal/nl/TERMS.html
  [pl] app/src/main/assets/legal/pl/TERMS.html
  [ta] app/src/main/assets/legal/ta/TERMS.html
  [te] app/src/main/assets/legal/te/TERMS.html
  [th] app/src/main/assets/legal/th/TERMS.html
  [tr] app/src/main/assets/legal/tr/TERMS.html
  [uk] app/src/main/assets/legal/uk/TERMS.html
  [ur] app/src/main/assets/legal/ur/TERMS.html

### 1.5.4 Health-Disclaimer-Dokumente
