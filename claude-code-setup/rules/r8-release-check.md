# R8-Pruefung bei Release-Builds (KRITISCH)

## Regel

Wenn ein Android-Projekt einen Release-Build macht (`isMinifyEnabled = true`),
MUSS eine vollstaendige R8-Kompatibilitaetspruefung durchgefuehrt werden —
sowohl beim ERSTEN Mal als auch NACHDEM R8 schon druebergelaufen ist.

**WICHTIG:** Das ist EIN grosser systematischer Durchgang, nicht stueckweises
Entdecken einzelner Bugs. ALLE Libraries pruefen, ALLE ProGuard-Regeln
schreiben, ALLE Features testen — in EINEM Durchgang.

## Wann diese Pruefung PFLICHT ist

1. **Erster Release-Build** eines Projekts (isMinifyEnabled wird aktiviert)
2. **Nach Hinzufuegen neuer Libraries** die Reflection/JNI/AIDL nutzen
3. **Nach Aktualisierung von Libraries** (neue Versionen koennen neue Klassen haben)
4. **Wenn IRGENDEIN Feature im Release-Build nicht geht** das in Debug funktionierte

## Pflicht-Ablauf (EIN grosser Durchgang)

### Schritt 1: Alle externen Libraries auflisten
```bash
grep -rh "^import " app/src/main/java/ --include="*.kt" | sort -u | grep -v "android\.\|java\.\|javax\.\|kotlin\.\|kotlinx\.\|<projektpaket>"
```

### Schritt 2: Fuer JEDE Library pruefen — braucht sie ProGuard-Regeln?

| Pattern | Warum R8 es kaputt macht | Beispiele |
|---------|-------------------------|-----------|
| **JNI** (native Code) | R8 benennt Java-Felder um, C++ sucht sie per Name | Sherpa-ONNX, OpenCV, TensorFlow Lite |
| **Reflection** | R8 entfernt Klassen die nicht direkt aufgerufen werden | Moshi, Gson, Retrofit, Room Entities |
| **AIDL** | R8 entfernt Interface-Methoden | Google Play Billing, System Services |
| **Credential Manager** | Google-interne Klassen per Reflection geladen | Google Sign-In, OAuth |
| **Serialization** | Felder per Name gelesen/geschrieben | JSON-Parser, Protobuf, XML |
| **Crypto/Security** | Klassen per Reflection instanziiert | EncryptedSharedPreferences, Tink |

### Schritt 3: ALLE ProGuard-Regeln auf einmal schreiben
```
-keep class <package>.** { *; }
```
Fuer JEDE identifizierte Library — nicht eine nach der anderen entdecken,
sondern ALLE vorab in proguard-rules.pro eintragen.

### Schritt 4: Clean Release Build
```bash
./gradlew clean assembleRelease
```

### Schritt 5: JEDEN Feature-Bereich einzeln testen (Checkliste)
- [ ] Google Sign-In / Anmeldung
- [ ] Google Drive Backup (Sichern UND Wiederherstellen)
- [ ] Sprachaufnahme / Transkription (lokal + Cloud)
- [ ] Billing / Abo-Kauf
- [ ] Biometric (Fingerabdruck / PIN)
- [ ] Firebase AI (Dashboard-Analyse, Textverbesserung)
- [ ] Alle API-Aufrufe (Retrofit/OkHttp)
- [ ] Datenbank (Room — Eintraege erstellen, lesen, loeschen)
- [ ] EncryptedSharedPreferences (Einstellungen speichern)
- [ ] Navigation (alle Screens erreichbar)

### Schritt 6: Alle gefundenen Fehler auf einmal fixen
Wenn Fehler gefunden werden: ALLE sammeln, ALLE ProGuard-Regeln auf einmal
hinzufuegen, EIN neuer Build, EIN neuer Testdurchlauf. Nicht: Fix → Build →
Test → naechster Fehler → Fix → Build → Test.

## Was NIEMALS passieren darf

- ❌ Release-Build dem Benutzer geben ohne R8-Pruefung
- ❌ R8-Bugs einzeln entdecken statt systematisch vorab pruefen
- ❌ Annehmen "es geht weil es in Debug ging" — Debug hat KEIN R8
- ❌ Nur den ersten Fehler fixen und hoffen dass es keine weiteren gibt
- ❌ ProGuard-Regeln stueckweise hinzufuegen (1 pro Bug statt alle auf einmal)

## Warum

Am 2026-04-02/03 hat die erste Release-Version von BestJournal 4 R8-Bugs verursacht
die NACHEINANDER entdeckt werden mussten — jeder brauchte einen eigenen Build-Install-
Test-Zyklus (4x je 15-30 Min = 2+ Stunden). Mit einem systematischen Vorab-Check
waeren ALLE 4 Bugs in EINEM Durchgang in 10 Minuten gefunden und gefixt worden.
