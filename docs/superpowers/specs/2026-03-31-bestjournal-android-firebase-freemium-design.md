# Design: BestJournalAndroid — Play Store Version mit Firebase AI & Freemium

**Datum:** 2026-03-31
**Status:** Genehmigt
**Basis:** BestJournalFrank (v0.7.5, Build 56)

---

## 1. Überblick

Neue Play Store Version der Best Journal App. Basiert auf BestJournalFrank,
aber mit fundamentalem Umbau der KI-Infrastruktur:

- **Weg:** Direkte Gemini API mit User-API-Key
- **Hin:** Firebase AI Logic (ehemals Vertex AI in Firebase) — kein API-Key nötig
- **Neu:** Freemium-System mit Nutzungstagen, Google Play Billing Abo

Die persönliche Version (BestJournalFrank) bleibt unverändert.

---

## 2. Zwei getrennte Projekte

| | BestJournalFrank | BestJournalAndroid |
|---|---|---|
| **Zweck** | Persönliche Version | Play Store für alle |
| **Application ID** | `com.entropyjournal` | `com.bestjournal.app` |
| **KI-Backend** | Gemini API direkt (eigener Key) | Firebase AI Logic (kein Key nötig) |
| **Kosten für Nutzer** | Kostenlos (eigener Key) | Freemium + Abo |
| **API-Key Eingabe** | Ja (Settings) | Nein (Firebase) |
| **Groq API** | Ja (Cloud-Transkription) | Nein (nur Offline-Whisper) |
| **Ordner** | `~/proggs/BestJournalFrank/` | `~/proggs/BestJournalAndroid/` |

### Warum getrennte Projekte statt Build Flavors?

- Die Unterschiede sind zu fundamental (komplett anderes KI-Backend, anderes Billing)
- Frank-Version kann frei experimentieren ohne Play Store Nutzer zu beeinflussen
- Eigene `google-services.json`, eigene Signing Keys, eigenes Firebase-Projekt
- Kein Risiko, dass ein versehentlicher Push die Play Store Version kaputt macht

---

## 3. Firebase AI Logic — Technisches Design

### 3.1 Setup

- Firebase-Projekt: Neues Projekt "BestJournal" in Firebase Console
- App registrieren mit Application ID `com.bestjournal.app`
- `google-services.json` herunterladen und ins Projekt legen
- Firebase App Check mit Play Integrity aktivieren

### 3.2 SDK Integration

```kotlin
// build.gradle.kts (app)
dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-ai")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-auth")  // bereits vorhanden
}
```

### 3.3 Neuer AI Service (ersetzt GeminiApi + Retrofit)

```
com.bestjournal.data.remote.ai/
  ├── FirebaseAiService.kt       // Wrapper um Firebase AI Logic SDK
  ├── AiRateLimiter.kt           // Freemium-Logik (Nutzungstage, Limits)
  └── AiUsageTracker.kt          // Zählt Nutzungstage und KI-Aufrufe
```

**FirebaseAiService** ersetzt die bisherige GeminiApi-Interface + Retrofit-Implementierung.
Ein einzelner Service der alle drei KI-Funktionen abdeckt:

- `improveText(text: String): Result<String>`
- `summarizeEntry(text: String): Result<SummaryResult>`
- `analyzeEntropy(entries: String, systemPrompt: String): Result<String>`

### 3.4 Was entfällt

- `GeminiApi.kt` (Retrofit Interface) → wird durch Firebase SDK ersetzt
- `GeminiRequest.kt` / `GeminiResponse.kt` → Firebase SDK hat eigene Typen
- `NetworkModule.kt`: Gemini OkHttpClient + Retrofit Builder entfallen
- Settings-Felder: `PREF_GEMINI_API_KEY`, `PREF_GEMINI_MODEL` (Modellwahl bleibt aber intern)
- Groq API komplett entfernen (nur Offline-Whisper in Play Store Version)

### 3.5 Was bleibt gleich

- Alle Prompts (ImproveTextUseCase, SummarizeEntryUseCase, AdviceRepository)
- Chunking-Logik bei langen Texten
- Alle UI-Screens (Dashboard, Journal, EntryDetail, Settings, Splash, Login)
- Offline-Whisper Spracherkennung (sherpa-onnx)
- Google Drive Backup
- Biometrische Sperre
- Theme-System

---

## 4. Freemium-System

### 4.1 Nutzungstage-Tracking

Ein "Nutzungstag" zählt wenn der User an diesem Kalendertag mindestens einen
Eintrag erstellt (spricht oder schreibt). Nur öffnen ohne Eintrag zählt nicht.

```
AiUsageTracker (in SharedPreferences / Room):
  - firstUsageDate: Long           // Erster Nutzungstag (Timestamp)
  - usageDaysCount: Int            // Anzahl Nutzungstage gesamt
  - usageDaysSet: Set<String>      // Alle Nutzungstage als "YYYY-MM-DD"
  - weeklyAiUsageCount: Int        // KI-Nutzungen diese Woche
  - weeklyResetDate: String        // Wann der Wochen-Counter zurückgesetzt wird
```

### 4.2 Phasen

| Phase | Bedingung | KI-Zugang | UI-Verhalten |
|---|---|---|---|
| **Honeymoon** | Nutzungstag 1–3 | Alles unbegrenzt | Keine Hinweise, App einfach nutzen lassen |
| **Aufklärung** | Nutzungstag 4–7 | Alles unbegrenzt | Sanfter Banner: "KI arbeitet für dich" + Vorteile |
| **Freemium** | Ab Nutzungstag 8 | 3 KI-Einträge/Woche gratis | Zähler sichtbar: "2 von 3 KI-Nutzungen diese Woche" |
| **Abo aktiv** | Abo gekauft | Alles unbegrenzt | Premium-Badge, keine Limits |

### 4.3 Was zählt als "KI-Nutzung"

Jede KI-Aktion zählt als **eine** Nutzung:
- Eintrag mit Textverbesserung + Zusammenfassung = 1 KI-Nutzung
- Dashboard-Analyse aktualisieren = 1 KI-Nutzung

**In den ersten 7 Nutzungstagen:** ALLES unbegrenzt, inkl. Dashboard-Analyse
mit allen Einträgen. Keine Einschränkungen.

**Ab Nutzungstag 8 (ohne Abo):** 3 KI-Nutzungen pro Woche — egal ob
Textverbesserung, Zusammenfassung oder Dashboard-Analyse.

### 4.4 KI-Info-Banner (Nutzungstag 4–7)

Sanfter, nicht-nerviger Banner im Dashboard:
- "Dein Tagebuch wird von KI unterstützt"
- Zeigt 3–4 Vorteile: Textverbesserung, Zusammenfassungen, Lebensratschläge, Muster-Erkennung
- Kann weggeklickt werden, kommt nur 1x pro Tag
- Kein aggressives Upselling in dieser Phase

### 4.5 Abo-Hinweis (ab Nutzungstag 8)

Wenn die 3 freien KI-Nutzungen pro Woche aufgebraucht sind:
- Bottom Sheet: "Deine KI-Nutzungen für diese Woche sind aufgebraucht"
- Zeigt Abo-Optionen (Monat / Jahr)
- "Nein danke" Button — App funktioniert weiter als Tagebuch ohne KI

---

## 5. Google Play Billing

### 5.1 Abo-Produkte

| Produkt | ID | Preis (vorläufig) | Laufzeit |
|---|---|---|---|
| Monatsabo | `bestjournal_ai_monthly` | TBD (nach Kalkulation) | 1 Monat, auto-renew |
| Jahresabo | `bestjournal_ai_yearly` | TBD (ca. 20% günstiger) | 1 Jahr, auto-renew |

Preise werden basierend auf Token-Kosten-Kalkulation festgelegt (separate Aufgabe).

### 5.2 Billing Integration

```
com.bestjournal.billing/
  ├── BillingManager.kt           // Google Play Billing Library Wrapper
  ├── SubscriptionState.kt        // sealed class: Free, Trial, Subscribed, Expired
  └── SubscriptionViewModel.kt    // UI State für Abo-Screens
```

### 5.3 Abo-Status prüfen

Bei App-Start und vor jeder KI-Nutzung:
1. Google Play Billing abfragen: Hat User aktives Abo?
2. Wenn ja → unbegrenzte KI
3. Wenn nein → Nutzungstag-Phase und Wochen-Limit prüfen

---

## 6. Entropie-Analyse: Token-Limit

Die Entropie-Analyse sendet alle Tagebucheinträge an Gemini.
Bei 50+ Einträgen mit je 30–40 Zeilen wird das teuer.

**Lösung:** Nur die letzten N Einträge analysieren.

| Abo-Status | Max. Einträge für Analyse |
|---|---|
| Trial (Tag 1–7) | Alle Einträge (unbegrenzt) |
| Free (ab Tag 8) | Letzte 10 Einträge |
| Abo aktiv | Letzte 30 Einträge |

In den ersten 7 Nutzungstagen gibt es KEINE Einschränkung — der User bekommt
die volle Power mit allen Einträgen. Erst danach greifen die Limits.
Ältere Einträge bleiben gespeichert, werden aber nach der Trial-Phase
nicht mehr in die KI-Analyse einbezogen.
Der User sieht: "Analyse basiert auf deinen letzten 30 Einträgen."

---

## 7. Was entfällt in der Play Store Version

| Feature | Grund |
|---|---|
| Groq API (Cloud-Transkription) | Nutzer müssten eigenen API-Key eintragen |
| API-Key Eingabefelder in Settings | Nicht mehr nötig |
| Gemini-Modell-Auswahl in Settings | Wird intern festgelegt (günstigstes Flash-Modell) |
| Debug-Infos (roter Punkt "Lokales Whisper") | Endnutzer braucht das nicht |

---

## 8. Settings-Screen (Play Store Version)

Vereinfacht — kein API-Key-Bereich mehr:

- Google Drive Backup (Ein/Aus, Account wählen)
- Biometrische Sperre (Ein/Aus)
- Theme (Hell/Dunkel/System/Sonnenstand)
- Aufnahmedauer (Slider)
- **NEU: KI-Abo verwalten** (Status, Abo kaufen/kündigen, Nutzung diese Woche)
- App-Version
- Feedback per E-Mail

---

## 9. Projektstruktur BestJournalAndroid

Identisch zu BestJournalFrank, aber mit diesen Änderungen:

```
BestJournalAndroid/
  app/
    src/main/java/com/bestjournal/
      data/
        remote/
          ai/                          # NEU: Firebase AI Logic
            FirebaseAiService.kt
            AiRateLimiter.kt
            AiUsageTracker.kt
          gemini/                      # ENTFÄLLT (durch Firebase ersetzt)
          groq/                        # ENTFÄLLT (nur Offline-Whisper)
        repository/
          ...                          # Bleiben, nutzen FirebaseAiService
      billing/                         # NEU: Google Play Billing
        BillingManager.kt
        SubscriptionState.kt
        SubscriptionViewModel.kt
      domain/
        usecase/
          ImproveTextUseCase.kt        # Nutzt FirebaseAiService statt GeminiApi
          SummarizeEntryUseCase.kt     # Nutzt FirebaseAiService statt GeminiApi
      ui/
        screens/
          settings/                    # Vereinfacht (kein API-Key)
          subscription/                # NEU: Abo-Screen
          ...                          # Rest bleibt gleich
      di/
        NetworkModule.kt               # Vereinfacht (kein Gemini/Groq Retrofit)
        FirebaseModule.kt              # NEU: Firebase DI
```

---

## 10. Implementierungsreihenfolge

1. **Projekt kopieren und umbenennen** — BestJournalFrank → BestJournalAndroid
2. **Application ID ändern** — `com.entropyjournal` → `com.bestjournal.app`
3. **Firebase-Projekt einrichten** — Console, google-services.json
4. **Firebase AI Logic einbauen** — SDK, FirebaseAiService, App Check
5. **Groq API + Gemini Retrofit entfernen** — durch Firebase ersetzen
6. **Freemium-System bauen** — AiUsageTracker, AiRateLimiter, Phasen-Logik
7. **Settings-Screen vereinfachen** — API-Key-Felder weg, Abo-Bereich rein
8. **Google Play Billing** — Abo-Produkte, BillingManager, Subscription-Screen
9. **KI-Info-Banner** — Aufklärungsphase Tag 4–7
10. **Abo-Paywall** — Bottom Sheet wenn Limit erreicht
11. **Token-Kalkulation** — Echte Kosten durchrechnen, Abo-Preise festlegen
12. **Build & Test** — APK auf echtem Gerät testen
