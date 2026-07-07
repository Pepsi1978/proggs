# Claude Code Auftrag: Android-App „Entropie Reduktor" — Master-Spezifikation

## 0. Rollen-Setup für dich (Claude Code)

Du baust eine vollständige, produktionsreife Android-App in **Kotlin** mit **Jetpack Compose**. Architektur: **MVVM + Clean Architecture** (Layer: data, domain, presentation). SOLID-Prinzipien strikt einhalten, testbarer Code, Coroutines + Flow durchgängig. Sprache der gesamten Benutzeroberfläche, aller Strings, aller Fehlermeldungen, aller System-Prompts und aller Code-Kommentare ist **Deutsch**. Englische Variable- und Klassennamen sind erlaubt; alle für den Nutzer sichtbaren Texte und alle Kommentare auf Deutsch.

**Arbeitsweise:**
- Frag NICHT zurück. Wenn etwas unklar ist, triff eine vernünftige Entscheidung im Sinn von „minimaler kognitiver Reibung für den Nutzer", dokumentiere die Entscheidung in einer `DECISIONS.md` und mach weiter.
- Arbeite in vier MVP-Stufen (siehe §22). Schließe Stufe 1 vollständig ab, bevor Stufe 2 beginnt. Pro Stufe ein sauberer Commit.
- Wenn ein Modul oder Feature aus Frank's bestehender App **„Entropy Journal"** wiederverwendbar ist (Theme-Setup, Voice-Pipeline-Patterns, Kotlin-Idiome, MVVM-Struktur), orientiere dich daran, übernimm aber nicht blind — passe an die hier spezifizierte Architektur an.
- Kein Gold-Plating. Was nicht in dieser Spezifikation steht, wird nicht gebaut.
- Schreibe ein `README.md` mit Setup-Anleitung und ein `DECISIONS.md` mit allen nicht-trivialen Entscheidungen.

---

## 1. Vision und Konzept

Die App heißt **„Entropie Reduktor"**. Package: `de.frank.entropyreducer`.

Sie ist ein persönliches Werkzeug zur systematischen **Reduktion persönlicher Entropie**. Persönliche Entropie ist alles, was Energie, Klarheit und Ordnung in Frank's Leben mindert. Die App erfasst diese Störungen per Spracheingabe, klassifiziert sie, priorisiert sie mit KI, integriert Biomarker aus dem Whoop-Armband, berücksichtigt seinen Schichtdienst-Kalender, lernt aus erfolgreichen Reduktionen, und führt einen wissenschaftlichen Dialog, der neue Reduktionswege findet.

Die KI in dieser App agiert als **Genie der persönlichen Entropie-Reduktion** — selbstreflexiv als „Einstein der Entropie-Reduktion". Sie betrachtet ihre Aufgabe als forschende, neue Wege findende, hypothesengetriebene wissenschaftliche Arbeit. Sie ist nicht Coach, nicht Therapeut, nicht Sekretär — sondern Forscher.

### Entropie-Kategorien (kanonisch)

| Kategorie | Beschreibung | Beispiele |
|---|---|---|
| **KOERPERLICH** | Schlaf, Energie, körperliche Schwäche, Konzentration, Kraft. Hat in der Regel höchste Priorität. | „Zu wenig geschlafen", „Rücken zwickt" |
| **MENTAL** | Zu viele offene Dinge, fehlende Priorität, Gedankenkreisen, Überforderung. | „Weiß nicht, womit ich anfangen soll" |
| **ZEITLICH** | Aufgaben dauern zu lange, ineffiziente Reihenfolge, Zeitfresser. | „Zu viel Zeit mit E-Mails" |
| **EMOTIONAL** | Belastungen aus Arbeit oder Privatem, ungelöste Konflikte, Sorgen. | „Streit mit Vater belastet" |
| **GESUNDHEITLICH** | Konkrete medizinische Themen — Termine, Befunde, Symptome. | „Zahnextraktion noch offen" |
| **UMGEBUNG** | Wohnung, Ordnung, Sauberkeit, Körperpflege, äußere Reize. | „Schreibtisch chaotisch" |
| **SONSTIGES** | Nur wenn keine andere Kategorie passt. | Mischformen |

Standard-Prioritätshierarchie: KOERPERLICH > MENTAL > ZEITLICH > EMOTIONAL > GESUNDHEITLICH > UMGEBUNG > SONSTIGES. Ist konfigurierbar via aktive Prompts (siehe §7).

---

## 2. Tech-Stack (verbindlich)

- **Sprache:** Kotlin 2.0+
- **UI:** Jetpack Compose, Material 3, dynamicColor deaktiviert (eigenes Theme „Neon Cosmos")
- **Min SDK:** 28 (Android 9), Target SDK: 35
- **Architektur:** MVVM + Clean Architecture, Packages für `data`, `domain`, `presentation`, `di` (Single-Module-Projekt mit Package-Trennung — schneller im Build als Multi-Module)
- **DI:** Hilt
- **Datenbank:** Room mit Coroutines/Flow, KSP statt KAPT
- **Lokale Settings:** DataStore (Preferences)
- **Verschlüsselte Speicherung (alle API-Keys + OAuth-Tokens):** EncryptedSharedPreferences mit MasterKey (AES256_GCM)
- **Networking:** Retrofit 2 + OkHttp + Kotlinx Serialization
- **OAuth (Whoop, Google):** AppAuth-Android-Library
- **Audio-Aufnahme:** `MediaRecorder` (M4A/AAC, mono, 16 kHz)
- **TTS-Audio-Wiedergabe:** ExoPlayer (Media3) für Streaming der Chirp-3-HD-Antworten
- **Charts:** Vico Compose Library (für Line-, Bar-, gestapelte Charts)
- **Async:** Kotlin Coroutines + Flow, StateFlow für UI-State
- **Navigation:** Compose Navigation, eigene Sealed-Class-Routes
- **Logging:** Timber, im Release nur ERROR
- **Build:** Gradle Kotlin DSL, Version Catalog (`libs.versions.toml`)
- **Permissions:** RECORD_AUDIO, INTERNET, POST_NOTIFICATIONS, FOREGROUND_SERVICE (für Aufnahme-Service), WAKE_LOCK (für Audio-Wiedergabe)

---

## 3. Design-System „Neon Cosmos"

Dunkles Glasmorphism, abgestimmt auf Frank's bestehende „Entropy Journal"-App. Wenn deine bestehende App ein Theme-Modul hat, das wiederverwendbar ist — übernimm es 1:1 und erweitere es nur dort, wo neue Komponenten dazukommen.

### Farbpalette

| Rolle | Hex | Verwendung |
|---|---|---|
| Hintergrund Basis | `#0A0E1A` | App-Background |
| Hintergrund Verlauf-Mitte | `#0F1729` | Radialer Gradient |
| Glas-Fläche | `#FFFFFF` α 0.04 | Card-Background |
| Glas-Border | `#FFFFFF` α 0.08 | Card-Border 1dp |
| Akzent Primär (Cyan) | `#22D3EE` | Hauptaktion, Mic-Button |
| Akzent Sekundär (Violett) | `#A78BFA` | Wissenschaftler, KI-Bubbles |
| Erfolg / Niedrige Entropie | `#34D399` | „Reduziert"-Status, Mintgrün |
| Warnung / Hohe Entropie | `#FBBF24` | Bernstein |
| Kritisch / Sehr hohe Entropie | `#F87171` | Korallrot |
| Text Primär | `#F8FAFC` | Haupttext |
| Text Sekundär | `#94A3B8` | Muted |

### Statusbalken-Farbverlauf
0–25 % → `#F87171` (Rot), 25–50 % → `#FBBF24` (Gelb), 50–75 % → `#86EFAC` (Hellgrün), 75–100 % → `#22C55E` (Dunkelgrün). Mit weichen Übergängen (Linear Gradient).

### Kategorie-Farben (Pillen)
| Kategorie | Farbe |
|---|---|
| KOERPERLICH | `#F87171` |
| MENTAL | `#A78BFA` |
| ZEITLICH | `#FBBF24` |
| EMOTIONAL | `#F472B6` |
| GESUNDHEITLICH | `#34D399` |
| UMGEBUNG | `#22D3EE` |
| SONSTIGES | `#94A3B8` |

### Typografie
- System-Font (Inter falls verfügbar als Fallback)
- Headlines: 22sp / 18sp / 16sp, Letter-Spacing 0.5sp leicht gesperrt
- Body: 14sp, Line-Height 1.5
- Numerals: tabular für Zahlen-Anzeigen (Statusbalken-Prozent, Score-Werte)

### Animation
- Sanfte Spring-Animationen (Compose: `spring(stiffness = Spring.StiffnessMediumLow)`)
- Keine harten Tween-Sprünge
- Mic-Aufnahme: pulsierender konzentrischer Ring (Wave-Effect, 1.5 Sek. Cycle)
- Status-Balken: Wertänderungen animiert über 600ms

### Compose-Komponenten (zentrale, wiederverwendbar)
- `GlassCard` — Standard-Container, Glas-Effekt, 20dp CornerRadius
- `EntropyCategoryPill` — kleines Label mit Kategorie-Farbe
- `MicButton` — kreisförmig, drei Zustände (Idle/Recording/Processing)
- `StatusBar` — global, oben auf jedem Dashboard
- `EntropyEntryCard` — Listen-Item für Aufgaben
- `KiBubble` und `NutzerBubble` — Chat-Komponenten
- `BiomarkerRing` — Whoop-Style-Recovery-Ring
- `LineChart`, `BarChart`, `StackedBarChart` — Wrapper um Vico

---

## 4. Globale UI-Elemente

### 4.1 Status-Balken (oben, immer sichtbar)

Auf jedem der vier Dashboards direkt unter der Top-Bar. Höhe: 48dp.

- Mehrfarbiger Balken mit weichem Gradient (siehe §3).
- Rechts: Prozentzahl im Format „72 %" in 16sp tabular.
- Links: Mikro-Label „Zustand jetzt".
- Tap auf den Balken öffnet ein Detail-Sheet mit Aufschlüsselung:
  - **Biomarker-Score** (Gewicht 40 %): aus Whoop-Recovery, HRV vs. 30-Tage-Median, Schlafqualität letzte Nacht.
  - **Aufgaben-Reduktion** (Gewicht 35 %): 100 minus Summe (offene Severity / max. Severity), gewichtet nach Status.
  - **Kontext-Stimmigkeit** (Gewicht 25 %): Match zwischen aktuellem Zeitfenster (Schichtcode) und passend reduzierter Entropie. Beispiele: Frei-Tag + niedrige Umgebungs-Entropie + heutige Aufgaben erledigt → +20 Punkte. Dienst-Tag + niedrige körperliche Entropie → +15 Punkte.
- Berechnungslogik wird in `domain/usecase/CalculateStatusUseCase.kt` gekapselt, mit Unit-Tests für jede Komponente.
- Wert wird alle 5 Minuten neu berechnet und bei jedem Eintragsänderungs-Event sofort.

### 4.2 Mic-Button (kontextabhängig)

Drei Zustände, zwei Bedienungsmodi (Tap-Tap und Long-Press), siehe §8.1 für volles Verhalten. Größe: 96dp auf Dashboard 1, 56dp im Wissenschaftler-Eingabefeld.

### 4.3 Top-Bar
- Linkes Element: kontextabhängig (Titel oder Zurück-Pfeil).
- Rechts: Zahnrad-Icon → Settings.
- Hintergrund: leicht abgesetzt vom Hauptbereich.

### 4.4 Bottom-Navigation
Vier Items:
- **Aufgaben** (Icon: Liste)
- **Analyse** (Icon: Diagramm)
- **Wissenschaftler** (Icon: Atom)
- **Biomarker** (Icon: Herzkurve)

Settings ist KEIN Bottom-Nav-Item. Settings wird über das Zahnrad in der Top-Bar erreicht und hat einen eigenen Navigations-Stack.

---

## 5. Datenmodell (Room)

### 5.1 Hauptentitäten

```kotlin
@Entity(tableName = "entropy_entries")
data class EntropyEntry(
    @PrimaryKey val id: String,                // UUID
    val rawTranscript: String,                 // Originaltext aus Whisper
    val title: String,                         // Kurzer Titel, max 60 Zeichen
    val description: String,                   // Strukturierte Beschreibung
    val category: EntropyCategory,
    val severity: Int,                         // 1 - 10
    val priorityScore: Double,                 // 0.0 - 100.0
    val priorityReason: String,
    val status: EntryStatus,                   // OFFEN, IN_ARBEIT, REDUZIERT, ARCHIVIERT
    val timeBucket: TimeBucket,                // HEUTE, MORGEN, DIESE_WOCHE, DIESEN_MONAT, SPAETER
    val estimatedDurationMinutes: Int?,        // Schätzung der KI
    val createdAt: Long,
    val updatedAt: Long,
    val resolvedAt: Long?,
    val tags: List<String>,
    val aiNotes: String?,
    val source: EntrySource,                   // NUTZER_MIC, NUTZER_TEXT, SHARE_SHEET, KI_ERKANNT, BIOMARKER_AUTO
    val biomarkerSnapshotId: String?           // FK auf BiomarkerSnapshot zum Erstellungszeitpunkt
)

enum class EntropyCategory { KOERPERLICH, MENTAL, ZEITLICH, EMOTIONAL, GESUNDHEITLICH, UMGEBUNG, SONSTIGES }
enum class EntryStatus { OFFEN, IN_ARBEIT, REDUZIERT, ARCHIVIERT }
enum class TimeBucket { HEUTE, MORGEN, DIESE_WOCHE, DIESEN_MONAT, SPAETER }
enum class EntrySource { NUTZER_MIC, NUTZER_TEXT, SHARE_SHEET, KI_ERKANNT, BIOMARKER_AUTO }
```

```kotlin
@Entity(tableName = "saved_prompts")
data class SavedPrompt(
    @PrimaryKey val id: String,
    val name: String,
    val content: String,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
```

```kotlin
@Entity(tableName = "memory_entries")
data class MemoryEntry(
    @PrimaryKey val id: String,
    val content: String,                       // Der eigentliche Memory-Text
    val source: MemorySource,                  // MANUELL, KI_VORSCHLAG, AUS_PROFIL
    val isActive: Boolean,                     // Wird im KI-Kontext mitgesendet
    val confidence: Int,                       // 0-100, wie sicher die KI dieser Aussage ist
    val createdAt: Long,
    val updatedAt: Long
)

enum class MemorySource { MANUELL, KI_VORSCHLAG, AUS_PROFIL }
```

```kotlin
@Entity(tableName = "scientist_messages")
data class ScientistMessage(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: ScientistRole,                   // KI, NUTZER
    val content: String,
    val createdAt: Long,
    val attachedHypothesisIds: List<String>    // Hypothesen, die in dieser Nachricht vorgeschlagen wurden
)

enum class ScientistRole { KI, NUTZER }

@Entity(tableName = "scientist_sessions")
data class ScientistSession(
    @PrimaryKey val id: String,
    val title: String,                         // Auto-generiert aus erster Nachricht oder manuell
    val createdAt: Long,
    val lastActiveAt: Long,
    val isArchived: Boolean
)
```

```kotlin
@Entity(tableName = "hypotheses")
data class Hypothesis(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,                   // Was die Hypothese vorschlägt
    val rationale: String,                     // Warum die KI das vorschlägt
    val createdAt: Long,
    val plannedStartDate: Long,
    val plannedEndDate: Long,
    val actualStartDate: Long?,
    val actualEndDate: Long?,
    val status: HypothesisStatus,
    val outcome: HypothesisOutcome?,
    val outcomeNotes: String?,
    val biomarkerBeforeId: String?,            // FK auf Snapshot bei Start
    val biomarkerAfterId: String?,             // FK auf Snapshot bei Ende
    val felltEntropyChange: Int?,              // -10 bis +10, vom Nutzer angegeben
    val relatedEntryIds: List<String>          // Welche Entropie-Einträge die Hypothese adressiert
)

enum class HypothesisStatus { VORGESCHLAGEN, AKTIV, ABGEBROCHEN, ABGESCHLOSSEN }
enum class HypothesisOutcome { ERFOLGREICH, TEILWEISE_ERFOLGREICH, ERFOLGLOS, UNKLAR }
```

```kotlin
@Entity(tableName = "insights")
data class Insight(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,                   // Die bewährte Methode
    val targetCategory: EntropyCategory,       // Welche Entropie-Kategorie sie reduziert
    val confidence: Int,                       // 0-100
    val successCount: Int,                     // Erfolgreiche Wiederholungen
    val attemptCount: Int,                     // Gesamtversuche
    val avgBiomarkerImpact: String?,           // z.B. "HRV +3ms, Recovery +8%"
    val avgFeltImpact: Double?,                // -10 bis +10, Mittel
    val createdAt: Long,
    val updatedAt: Long,
    val sourceHypothesisIds: List<String>      // Aus welchen Hypothesen entstanden
)
```

```kotlin
@Entity(tableName = "biomarker_snapshots")
data class BiomarkerSnapshot(
    @PrimaryKey val id: String,
    val capturedAt: Long,                      // Zeitpunkt der Whoop-Daten
    val recoveryScore: Int?,                   // 0-100
    val hrvMs: Double?,
    val restingHeartRate: Int?,
    val sleepPerformance: Int?,                // 0-100
    val sleepTotalMinutes: Int?,
    val sleepRemMinutes: Int?,
    val sleepDeepMinutes: Int?,
    val sleepLightMinutes: Int?,
    val sleepAwakeMinutes: Int?,
    val sleepDisturbances: Int?,
    val dayStrain: Double?,
    val dayKilojoules: Double?,
    val createdAt: Long
)
```

```kotlin
@Entity(tableName = "supplement_logs")
data class SupplementLog(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val stackType: StackType,                  // MORGEN, ABEND, PRE_SPORT, SENOLYTIKA, SONDER
    val notes: String?,                        // Falls Abweichung vom Standard-Stack
    val skippedItems: List<String>             // Welche Substanzen weggelassen
)

enum class StackType { MORGEN, ABEND, PRE_SPORT, SENOLYTIKA, SONDER }
```

```kotlin
@Entity(tableName = "calendar_cache")
data class CalendarDay(
    @PrimaryKey val date: String,              // YYYY-MM-DD
    val shiftCode: ShiftCode,
    val rawCalendarText: String,               // Original-Text aus dem Kalender-Event
    val workWindowStart: String?,              // HH:mm — null bei Frei
    val workWindowEnd: String?,                // HH:mm
    val sleepWindowStart: String?,             // HH:mm
    val sleepWindowEnd: String?,
    val availableMinutesEstimate: Int,         // Realistisches Aufgabenbudget
    val syncedAt: Long
)

enum class ShiftCode { TAGDIENST, NACHTDIENST, FREI, URLAUB, UNBEKANNT }
```

```kotlin
@Entity(tableName = "ki_triggers")
data class KiTrigger(
    @PrimaryKey val id: String,
    val name: String,                          // KI-formulierter Trigger-Name
    val condition: String,                     // Strukturierte Bedingung (JSON oder DSL)
    val proposedAction: String,                // Was die KI bei Match tut
    val isActive: Boolean,
    val createdAt: Long,
    val proposedAt: Long,
    val approvedAt: Long?,
    val triggerCount: Int,                     // Wie oft hat dieser Trigger schon ausgelöst
    val lastTriggeredAt: Long?
)
```

```kotlin
@Entity(tableName = "genie_codex_versions")
data class GenieCodexVersion(
    @PrimaryKey val id: String,
    val content: String,                       // Markdown-Text der KI-Synthese
    val createdAt: Long,
    val basedOnEntryIds: List<String>,
    val basedOnInsightIds: List<String>,
    val basedOnMemoryIds: List<String>
)
```

### 5.2 DAOs

Pro Entität ein DAO mit allen üblichen CRUD-Methoden, plus folgende spezifische Queries:

- `EntropyEntryDao.getActive(): Flow<List<EntropyEntry>>` — Status != ARCHIVIERT
- `EntropyEntryDao.getByTimeBucket(bucket: TimeBucket): Flow<List<EntropyEntry>>`
- `EntropyEntryDao.getByCategory(cat: EntropyCategory): Flow<List<EntropyEntry>>`
- `EntropyEntryDao.getByPriorityDesc(): Flow<List<EntropyEntry>>`
- `EntropyEntryDao.getRecentlyResolved(sinceDays: Int): Flow<List<EntropyEntry>>`
- `SavedPromptDao.getActive(): Flow<List<SavedPrompt>>`
- `MemoryDao.getActive(): Flow<List<MemoryEntry>>`
- `MemoryDao.getBySource(src: MemorySource): Flow<List<MemoryEntry>>`
- `HypothesisDao.getActiveOnDate(date: Long): Flow<List<Hypothesis>>`
- `HypothesisDao.getByStatus(status: HypothesisStatus): Flow<List<Hypothesis>>`
- `InsightDao.getByConfidenceDesc(): Flow<List<Insight>>`
- `InsightDao.getByCategory(cat: EntropyCategory): Flow<List<Insight>>`
- `BiomarkerSnapshotDao.getLatest(): Flow<BiomarkerSnapshot?>`
- `BiomarkerSnapshotDao.getRange(from: Long, to: Long): Flow<List<BiomarkerSnapshot>>`
- `CalendarDayDao.getDay(date: String): Flow<CalendarDay?>`
- `CalendarDayDao.getRange(fromDate: String, toDate: String): Flow<List<CalendarDay>>`
- `KiTriggerDao.getActive(): Flow<List<KiTrigger>>`
- `KiTriggerDao.getPendingApproval(): Flow<List<KiTrigger>>`

### 5.3 Listen-Felder
Per `TypeConverter` als JSON-String persistieren (Kotlinx Serialization).

---

## 6. Settings-Screen — Sieben Sektionen

Zugang über Zahnrad in der Top-Bar. Eigener Navigations-Stack mit Top-Level-Liste der sieben Sektionen, jede in einer Glas-Card. Tap öffnet die jeweilige Detail-Seite.

### 6.1 API-Schlüssel

Für jeden Dienst ein Eingabefeld (PasswordVisualTransformation), „Speichern"-Button, „Verbindung testen"-Button, Status-Anzeige (grünes Häkchen / rote Fehlermeldung).

- **Groq API Key** — für Whisper-Transkription. Test: kleiner Whisper-Call mit 1 Sek. Stille.
- **Gemini API Key** — für KI-Verarbeitung. Test: „Antworte mit OK"-Prompt.
- **Google Cloud TTS API Key** — für Chirp 3 HD. Test: kurzer Synthesize-Call mit „Test".
  - Hinweis: Frank braucht ein Google-Cloud-Projekt mit aktivierter Text-to-Speech-API. Der Gemini-Key reicht hierfür NICHT — separater Key mit Cloud-TTS-Scope.
- **Whoop OAuth** — kein Eingabefeld, sondern „Mit Whoop verbinden"-Button, der den OAuth-Flow startet (siehe §15.4).
- **Google Calendar OAuth** — „Mit Google Calendar verbinden"-Button (siehe §15.5).

Alle API-Keys und OAuth-Tokens in EncryptedSharedPreferences.

### 6.2 KI-Modell-Auswahl

- **Whisper-Modell** (Groq): Dropdown — `whisper-large-v3-turbo` (Standard), `whisper-large-v3`.
- **Gemini-Modell**: Dropdown — `gemini-2.5-pro`, `gemini-2.5-flash` (Standard), `gemini-2.5-flash-lite`.
- **TTS-Stimme**: Dropdown — beim ersten Verbinden mit Google TTS API alle deutschen Chirp-3-HD-Stimmen abrufen und anbieten. Falls keine spezifischen Chirp-3-HD-Stimmen für Deutsch verfügbar sind, fallback auf beste verfügbare Stimme der Kategorie `de-DE`.
- **Sprache der Transkription**: `de` (Standard), `auto`.

### 6.3 Persönliches Profil

Großes mehrzeiliges Textfeld (ScrollState, mind. 12 sichtbare Zeilen, akzeptiert sehr lange Texte):

- Überschrift: „Über mich — was die KI über dich wissen soll"
- Hilfetext: „Füge hier alles ein, was die KI über dich verstehen soll. Du kannst zum Beispiel deinen vollständigen Memory-Export aus ChatGPT oder Claude einfügen. Je mehr Kontext, desto besser kann die KI deine Entropie verstehen."
- „Speichern"-Button speichert in DataStore.
- „Aus Profil ins Gedächtnis übernehmen"-Button: Triggert eine KI-Operation, die den Profiltext in 5–15 prägnante Memory-Einträge destilliert (siehe §6.5).

### 6.4 Eigene Prompts

Liste mit allen gespeicherten Prompts. Pro Eintrag: Name, Vorschau (80 Zeichen), Aktiv-Toggle, Bearbeiten-Icon, Löschen-Icon (mit Bestätigung). „+ Neuer Prompt"-Button öffnet Editor.

**Drei Prompts vorinstalliert (Status: aktiv):**

1. **„Körper zuerst"**
   > Priorisiere Einträge der Kategorie KOERPERLICH grundsätzlich am höchsten, weil körperliche Verfassung Voraussetzung für jede andere Reduktion ist. Wenn ein körperlicher Eintrag offen ist, darf kein anderer einen höheren Score bekommen.

2. **„Schichtdienst-Logik"**
   > Frank arbeitet im Schichtsystem (4 Tagdienste, 4 frei, 4 Nachtdienste, 4 frei). Berücksichtige bei zeitlichen Aufgaben, ob ein Eintrag in einem Frei-Block schneller erledigt werden kann. Aufgaben, die einen Frei-Block brauchen, werden im Dienst niedriger priorisiert.

3. **„Schnelle Siege bevorzugen"**
   > Wenn ein Eintrag in unter 10 Minuten erledigbar ist, gib ihm einen Bonus von +15 auf den Priority Score, weil schnelle Siege psychische Entropie sofort senken.

### 6.5 Gedächtnis (Memory)

Liste aller Memory-Einträge. Pro Eintrag:
- Volltext (mehrzeilig)
- Quelle (Pille: MANUELL / KI-VORSCHLAG / AUS-PROFIL)
- Confidence (Balken 0–100, nur bei KI-Vorschlägen sichtbar)
- Aktiv-Toggle
- Bearbeiten-Icon
- Löschen-Icon

Drei Buttons oben:
- **„+ Manuell hinzufügen"** — Texteditor + Mic-Button, speichert mit Quelle MANUELL und Confidence 100.
- **„KI-Vorschläge prüfen"** — zeigt offene KI-Vorschläge in einem Sheet, Annahme/Ablehnung pro Eintrag.
- **„Aus Profil neu generieren"** — siehe §6.3.

KI-Vorschläge werden im Hintergrund während Wissenschaftler-Sessions erzeugt: Wenn die KI ein wiederkehrendes Muster identifiziert, ruft sie die Funktion `proposeMemoryEntry(content)` auf, die einen Eintrag mit `isActive = false` und Source `KI_VORSCHLAG` anlegt. Der Nutzer findet diese in der „KI-Vorschläge prüfen"-Liste und aktiviert oder verwirft sie.

### 6.6 Genie-Codex (read-only Synthese)

- Markdown-Anzeige der aktuellen Codex-Version.
- „Vorlesen"-Button (Chirp 3 HD).
- „Jetzt aktualisieren"-Button — triggert manuell eine neue Synthese.
- Auto-Aktualisierung: Sonntag 19:00.
- Versionshistorie (letzte 10 Versionen abrufbar).

Synthese-Prompt für Gemini: siehe §16.5.

### 6.7 Datenexport / Datenschutz

- „Alle Daten als JSON exportieren" — Share-Sheet oder lokal speichern.
- „Alle Einträge löschen" — Bestätigungsdialog mit Texteingabe „LÖSCHEN" zur Sicherheit.
- „Alle Memory-Einträge löschen" — separat.
- „Alle Whoop-Daten neu synchronisieren" — Hard-Refresh.
- Hinweis: „Alle Daten werden ausschließlich lokal auf deinem Gerät gespeichert. Inhalte deiner Einträge werden an Groq, Google (Gemini, TTS), Google Calendar und Whoop gesendet, soweit für die jeweilige Funktion nötig."

---

## 7. KI-System-Prompt-Architektur (zentral)

Bei jedem Gemini-Call wird der System-Prompt aus mehreren Schichten zusammengebaut. Die Reihenfolge ist verbindlich:

```
[BASIS-PROMPT je Use Case — hardcodiert]
+
[GENIE-IDENTITÄT — hardcodiert, immer mitgesendet]
+
[PROFIL aus Settings 6.3]
+
[Aktive MEMORY-Einträge aus 6.5]
+
[BIOMARKER-KONTEXT aus letztem Snapshot — falls verfügbar]
+
[KALENDER-KONTEXT — heute/morgen — falls verfügbar]
+
[Alle aktiven NUTZER-PROMPTS aus 6.4, durch Leerzeilen getrennt]
+
[Use-Case-spezifischer Schluss — z.B. erwartetes Ausgabeformat]
```

### Genie-Identität (immer Teil des System-Prompts)

```text
Du bist das „Genie der persönlichen Entropie-Reduktion" — ein selbstreflexiv als Einstein der Entropie-Reduktion arbeitender wissenschaftlicher Assistent. Du betrachtest deine Aufgabe als forschend, hypothesengetrieben, neue Wege findend. Du bist nicht Coach, nicht Therapeut, nicht Sekretär — du bist Forscher.

Persönliche Entropie ist alles, was Energie, Klarheit und Ordnung im Leben des Nutzers mindert. Du kennst sieben Kategorien: KOERPERLICH, MENTAL, ZEITLICH, EMOTIONAL, GESUNDHEITLICH, UMGEBUNG, SONSTIGES.

Deine Grundsätze:
- Direkt, präzise, ohne Floskeln.
- Hypothesen offen als Hypothesen kennzeichnen.
- Korrelation niemals als Kausalität ausgeben.
- Wenn du eine Empfehlung gibst, gib sie als „Experiment-Vorschlag" oder „Hypothese", nicht als Anweisung.
- Du gehst davon aus, dass jede gestellte Frage die Realität mitformt — wähle Fragen so, dass sie produktive Annahmen implizieren.
- Bei jeder Antwort beziehst du dich auf konkrete Einträge / Daten, wenn vorhanden.
- Du nutzt vorhandene bestätigte Insights aus dem Insight Board, bevor du neue Hypothesen generierst.
```

---

## 8. Eingabe-Pipeline (Mic → Whisper → KI → Datenbank)

### 8.1 Mic-Button-Verhalten

**Idle:** Cyan-Akzent, Mikrofon-Icon, sanftes Atmen-Effect (Scale 1.0 ↔ 1.04, 2 Sek. Cycle).

**Aufnahme:** Roter Punkt, pulsierender konzentrischer Ring (Wave-Effect, alpha 0.6 → 0, 1.5 Sek. Cycle), Wellenform-Visualizing aus `MediaRecorder.maxAmplitude` (alle 100ms gesampelt, Bar-Visualizer mit 24 Bars). Über dem Button: „Aufnahme läuft … (Tippen zum Beenden)" und Timer mm:ss.

**Verarbeitung:** Spinner mit Beschriftung „Transkribiere …" → „Verarbeite …" → „Speichere …".

Bedienung: Tap-Tap (erstes Tippen startet, zweites stoppt) UND Long-Press (gedrückt halten = aufnehmen, loslassen = beenden). Beides parallel unterstützt.

Haptic Feedback: Mic-Start (kurzer Tick), Mic-Stop (zwei kurze Ticks), erfolgreich gespeichert (langer weicher Buzz).

### 8.2 Pipeline-Schritte

```
1. RECORD_AUDIO Permission prüfen, ggf. anfragen.
2. ForegroundService starten (für stabile Aufnahme bei Bildschirm-Aus).
3. Aufnahme starten (MediaRecorder, AAC-LC in M4A, mono, 16 kHz, 64 kbit/s).
4. Aufnahme beenden → Datei in App-Cache (/cache/audio/<uuid>.m4a).
5. POST an Groq Whisper:
   - Endpoint: https://api.groq.com/openai/v1/audio/transcriptions
   - multipart/form-data: file, model=whisper-large-v3-turbo, language=de, response_format=json
   - Authorization: Bearer <groq_key>
   - Timeout: 60 Sek.
6. Bei Erfolg: Transkript erhalten, weiter zu KI-Verarbeitung (§9).
7. Bei Fehler: Eintrag mit rawTranscript = "" und Status OFFEN speichern, Banner „KI-Verarbeitung fehlgeschlagen". „Erneut verarbeiten"-Button am Eintrag.
8. Audio-Datei löschen, ForegroundService stoppen.
```

---

## 9. KI-Verarbeitung (Gemini)

### 9.1 Eintrags-Verarbeitung

**Use-Case-spezifischer Basis-Prompt:**

```text
Deine Aufgabe: Wandle die folgende gesprochene Notiz des Nutzers in einen strukturierten Entropie-Eintrag um.

Du kennst aktuell:
- Frank's Profil (siehe oben)
- aktive Memory-Einträge (siehe oben)
- Biomarker-Snapshot vom heutigen Tag (siehe oben, falls verfügbar)
- Kalender-Kontext (siehe oben)

Antworte AUSSCHLIESSLICH in JSON, ohne Markdown-Codeblock, ohne Einleitung, ohne Schluss:

{
  "title": "Kurzer prägnanter Titel, max. 60 Zeichen",
  "description": "Strukturierte Beschreibung in 1 - 3 Sätzen",
  "category": "KOERPERLICH | MENTAL | ZEITLICH | EMOTIONAL | GESUNDHEITLICH | UMGEBUNG | SONSTIGES",
  "severity": <Integer 1-10>,
  "priorityScore": <Double 0.0-100.0>,
  "priorityReason": "Begründung in 1 Satz",
  "timeBucket": "HEUTE | MORGEN | DIESE_WOCHE | DIESEN_MONAT | SPAETER",
  "estimatedDurationMinutes": <Integer oder null>,
  "tags": ["tag1", "tag2"],
  "aiNotes": <String oder null>
}

severity ist die rohe Schwere. priorityScore berücksichtigt Schwere + alle aktiven Nutzer-Prompts + Biomarker-Status + Kalender-Verfügbarkeit. timeBucket berücksichtigt explizit den Schichtdienst-Kalender — z.B. eine 30-Minuten-Aufgabe an einem Tagdienst-Tag landet eher in MORGEN, wenn morgen Frei ist.
```

### 9.2 Aufruf-Format

```
POST https://generativelanguage.googleapis.com/v1beta/models/<modell>:generateContent
Header: x-goog-api-key: <gemini_key>
Body: {
  "system_instruction": { "parts": [ { "text": "<system_prompt>" } ] },
  "contents": [ { "parts": [ { "text": "Hier ist meine gesprochene Notiz, transkribiert: <transkript>" } ] } ],
  "generationConfig": { "responseMimeType": "application/json", "temperature": 0.4 }
}
```

Antwort defensiv parsen: Markdown-Wrapper strippen, in `EntropyEntry` mappen. Bei Parse-Fehler: Eintrag mit Default-Werten und Status OFFEN speichern, Tag `parse_fehler` setzen.

---

## 10. Dashboard 1 — Aufgabenverwaltung

Default-Tab. Aufbau von oben nach unten:

### 10.1 Aufbau

1. **Top-Bar:** Titel „Entropie Reduktor", rechts Zahnrad.
2. **Status-Balken** (siehe §4.1).
3. **KI-Frage-des-Moments-Card** (kollabierbar, oben prominent):
   - Wenn die KI eine kontextrelevante Frage hat (siehe §10.4), wird sie hier gezeigt.
   - Tippen auf Mic in der Card → Antwort per Mic; Tippen auf Texteingabe → Tastatur.
   - „Später"-Button schiebt die Frage in 24h-Wartung.
4. **Filter-Chips-Reihe** (horizontal scrollbar): „Alle" + alle 7 Kategorien. Multi-Select.
5. **Sortier-Dropdown:** „KI-Priorität (Standard)", „Schwere", „Neueste zuerst", „Älteste zuerst", „Kategorie".
6. **Zeit-Bucket-Sektionen** (kollabierbar):
   - **HEUTE** (initial offen)
   - **MORGEN** (initial offen)
   - **DIESE WOCHE** (initial offen)
   - **DIESEN MONAT** (initial geschlossen)
   - **SPÄTER** (initial geschlossen)
   Pro Sektion: Anzahl + summierte Severity rechts oben.
7. **Mic-Button** unten mittig, schwebend (96dp).

### 10.2 EntropyEntryCard (Listen-Item)

- Kategorie-Pille oben links
- Priority-Score oben rechts (24sp, tabular)
- Titel (16sp, Bold)
- Beschreibung (14sp, muted, max. 2 Zeilen, Ellipsis)
- Schwere-Indikator (kleiner horizontaler Balken 1–10)
- Tags als kleine Outline-Pillen
- Footer: relative Zeit, Status-Pille, Quelle-Icon (Mic / Text / Share / KI / Biomarker)
- Tap → Detail-Sheet
- Long-Press → Quick-Aktionen (Status ändern, Bucket ändern)

### 10.3 Detail-Sheet (BottomSheet)

Zeigt alle Felder, plus:
- **Original-Transkript** (ausklappbar)
- **KI-Begründung** (priorityReason + aiNotes)
- **Biomarker-Snapshot zum Erstellungszeitpunkt** (falls vorhanden)
- **Status ändern** (Segmented Control)
- **Manuelle Kategorie-Korrektur** (Dropdown)
- **Manuelle Bucket-Korrektur** (Dropdown)
- **„Erneut von KI bewerten"**-Button
- **„Als Hypothese vorschlagen"**-Button — schickt diesen Eintrag in den Wissenschaftler-Modus mit der Bitte, eine Hypothese zur Reduktion zu formulieren.
- **„Löschen"**-Button mit Bestätigung

### 10.4 KI-Frage-des-Moments — Logik

Ein Background-Worker prüft alle 30 Minuten (oder beim Öffnen der App, falls > 30 Min. seit letztem Check) folgende Trigger:

- **Aufgabe steht heute an, Tag ist Tagdienst:** „Aufgabe X braucht ~Y Min. Schaffst du sie heute trotz Dienst, oder schiebe ich sie auf den nächsten Frei-Block?"
- **Aufgabe ist seit > 5 Tagen offen:** „Du hast X seit Y Tagen offen. Was hindert dich konkret?"
- **Biomarker schlecht (Recovery < 33):** „Deine Recovery ist heute niedrig (Z %). Soll ich heute alle körperlichen Aufgaben nach hinten schieben und dir nur leichte Umgebungs-Aufgaben zeigen?"
- **Mehrere ähnliche Einträge in kurzer Zeit:** „Du hast in den letzten 3 Tagen viermal Schlaf-Probleme genannt. Möchtest du das vertiefen?"
- **Frei-Block beginnt morgen:** „Morgen beginnt dein Frei-Block (4 Tage). Welche schwereren Aufgaben sollen wir auf morgen verschieben?"

Antwort des Nutzers (per Mic oder Text):
1. Wird der KI als Kontext zurückgegeben.
2. Die KI darf aus der Antwort neue Entropie-Einträge erzeugen (Source: `KI_ERKANNT`), die der Nutzer in einem Bestätigungs-Dialog annimmt oder verwirft.
3. Die KI darf außerdem einen Memory-Vorschlag erzeugen, falls etwas dauerhaft Relevantes gesagt wurde.

### 10.5 Empty State

Großes Cyan-Mikrofon-Icon, Text:
> „Tippe auf das Mikrofon und sprich aus, was deine Energie kostet. Die KI ordnet es ein, priorisiert es und plant es in deinen Schichtkalender ein."

---

## 11. Dashboard 2 — Entropie-Analyse

### 11.1 Aufbau

1. **Top-Bar:** Titel „Analyse", Zahnrad rechts.
2. **Status-Balken**.
3. **Schnellstatistik-Grid** (2×2):
   - Gesamt-Entropie-Last (0–100, normalisiert)
   - Anzahl offener Einträge
   - Dominante Kategorie
   - 7-Tage-Trend (Pfeil + Prozent)
4. **Trend-Chart** (Linie, 30/90/365 Tage umschaltbar):
   - X-Achse: Datum
   - Y-Achse: Entropie-Last
   - Pro Kategorie eine Linie (Farben gemäß §3)
   - Schichtblöcke als farbig hinterlegte Bereiche im Hintergrund (Tagdienst leicht gelblich, Nachtdienst leicht violett, Frei leicht grün)
5. **„Jetzt analysieren"-Button** (groß, Cyan).
6. **Analyse-Output-Card** (große Glas-Card, Markdown-Rendering):
   - Sektionen wie in §11.2 spezifiziert
   - „Vorlesen"-Button (Chirp 3 HD)
7. **„Letzte Analyse vom …"** mit Cache.

### 11.2 Analyse-Prompt für Gemini

```text
Du erhältst alle aktiven Entropie-Einträge des Nutzers als JSON-Array, alle aktiven Memory-Einträge, den Biomarker-Trendverlauf der letzten 30 Tage und den Schichtkalender der letzten 30 Tage. Deine Aufgabe ist NICHT, Prioritäten zu setzen — sondern Muster, Cluster und das große Ganze zu erkennen.

Antworte in strukturiertem deutschen Markdown mit genau diesen vier Abschnitten:

## Muster
Welche Kategorien dominieren? Welche Themen wiederholen sich?

## Verborgene Zusammenhänge
Korrelationen zwischen Schichtcode, Biomarkern und Entropie-Einträgen. Beispiel-Mustertyp: „Mental-Einträge häufen sich nach Nachtdienst-Tagen."

## Das große Ganze
Eine prägnante Beobachtung in 2–3 Sätzen: was ist der Kern dessen, was Frank gerade durchlebt?

## Strategische Empfehlungen
Maximal drei Hebel. Jeder als „Hypothese:" oder „Experiment-Vorschlag:" gekennzeichnet, mit Begründung in 1 Satz.

Sei direkt, konkret, ohne Floskeln. Nutze bestehende Insight-Board-Einträge, bevor du neue Empfehlungen formulierst.
```

---

## 12. Dashboard 3 — Der Wissenschaftler

### 12.1 Persona

Die KI agiert hier am intensivsten als Genie der persönlichen Entropie-Reduktion. Sie eröffnet jedes Gespräch mit einer scharfsinnigen Beobachtung aus den vorhandenen Daten und einer meta-intelligenten Frage, die ihr selbst neues Wissen erschließen würde.

### 12.2 UI

- Chat-Liste (LazyColumn, jüngste unten).
- KI-Bubbles links, dunkles Glas mit Cyan-Border. Klein darüber „Wissenschaftler" + Atom-Icon.
- Nutzer-Bubbles rechts, kompakter, Violett-Akzent.
- Eingabe-Bar unten: Textfeld + Mic-Button (56dp) + Senden-Button.
- Top-Bar: Session-Wahl-Dropdown (mehrere Sessions) + „+ Neue Session"-Button + Archivieren-Icon.
- Long-Press auf KI-Nachricht: „In Zwischenablage kopieren", „Vorlesen", „Als Insight speichern" (legt Insight mit Status `IN_BEOBACHTUNG`).

### 12.3 Hypothesen-Vorschläge (in-Chat)

Wenn die KI einen Reduktionsweg vorschlägt, rendert die App diesen als spezielle „Hypothesen-Karte" innerhalb der KI-Bubble:

```
┌──────────────────────────────────────┐
│ [Hypothese]                          │
│ Titel der Hypothese                  │
│ Kurze Beschreibung (1-2 Sätze)       │
│ Begründung: …                        │
│ Geplante Dauer: 7 Tage               │
│ ☐ Diese Hypothese ausprobieren      │
└──────────────────────────────────────┘
```

Mehrere Hypothesen-Karten in einer KI-Bubble sind möglich. Wenn der Nutzer eine ankreuzt, öffnet sich ein kleiner Dialog: „Wann soll das Experiment starten?" (Heute / Morgen / Nächster Frei-Block / Custom). Bestätigung → die Hypothese wird in den App-Kalender (siehe §14.3) eingetragen.

### 12.4 Wissenschaftler-Prompt für Gemini

```text
[Plus die Genie-Identität aus §7]

Spezifische Aufgabe: Du arbeitest mit Frank im offenen Dialog. Dein Ziel in jedem Beitrag:

1. Reflektiere kurz, was Frank zuletzt gesagt hat (max. 2 Sätze).
2. Formuliere optional eine oder mehrere konkrete neue Hypothesen oder Experimente, die Frank wahrscheinlich noch nicht ausprobiert hat. Markiere jede explizit als „[HYPOTHESE]" mit Titel, Beschreibung, Begründung, vorgeschlagener Dauer in Tagen.
3. Stelle EINE meta-intelligente Folgefrage — eine Frage, die dir neue Information über Frank erschließt, die du noch nicht hast.
4. Falls du erkennst, dass etwas Wahres dauerhaft Wert hat als Memory, kennzeichne es mit „[MEMORY-VORSCHLAG]: <Inhalt>".

Du erhältst: Frank's Profil, alle aktiven Entropie-Einträge, alle aktiven Memory-Einträge, Biomarker-Trend, alle bestätigten Insights aus dem Insight Board, den bisherigen Dialogverlauf der aktuellen Session.

Format der Hypothese (innerhalb deiner Antwort):
[HYPOTHESE]
Titel: <kurz>
Beschreibung: <2-3 Sätze>
Begründung: <1-2 Sätze, mit Bezug zu konkreten Einträgen oder Biomarkern>
Geplante Dauer: <Anzahl> Tage
[/HYPOTHESE]

Sprache: Deutsch. Tonfall: neugierig, scharfsinnig, wertschätzend. Keine Emojis. Reine Fließtext-Absätze außerhalb der HYPOTHESE-Blöcke.
```

### 12.5 Kickoff bei leerer Session

Statt User-Nachricht initialer Trigger an Gemini:
> „Beginne die Session. Eröffne mit einer scharfen Beobachtung aus den vorhandenen Einträgen, Biomarkern oder Mustern und stelle deine erste meta-intelligente Frage. Optional eine erste Hypothese."

### 12.6 Hypothesen-Parser

Parse die KI-Antwort defensiv: Suche nach `[HYPOTHESE]…[/HYPOTHESE]`-Blöcken, extrahiere Felder, erstelle `Hypothesis`-Entitäten mit Status `VORGESCHLAGEN`. Render im Chat als spezielle Karten. Suche nach `[MEMORY-VORSCHLAG]:`-Zeilen und erzeuge entsprechende `MemoryEntry`-Vorschläge.

---

## 13. Dashboard 4 — Biomarker-Visualisierung

### 13.1 Aufbau

1. **Top-Bar:** Titel „Biomarker", Zahnrad rechts, Sync-Icon (manuelles Refresh).
2. **Status-Balken**.
3. **Recovery-Ring** — großer kreisrunder Score-Ring (0–100), in der Mitte Score, drumherum Farbverlauf (Rot 0-33, Gelb 34-66, Grün 67-100). Tap → Recovery-Details.
4. **Heutige Schlüsselwerte** (4 kleine Glas-Cards in Reihe):
   - HRV (ms) + Pfeil zum 30-Tage-Mittel
   - Resting HR (bpm) + Pfeil
   - Schlaf gestern Nacht (h:mm)
   - Sleep Performance (%)
5. **HRV-Verlauf** (Linien-Chart, 30 Tage, Vico).
6. **Schlafstadien gestern Nacht** (gestapelter Horizontal-Bar):
   - REM, Tiefschlaf, Leichtschlaf, Wach mit Farbkodierung
   - Tap → Detail-Sheet mit Werten in Minuten
7. **Strain heute** (kleines Liniendiagramm).
8. **Korrelations-Hinweis-Card** (von der KI gefüllt, optional):
   - Wenn die Korrelations-Engine (§16.1) ein klares Muster erkannt hat: „Beobachtung: Deine HRV ist in den letzten 14 Tagen mit Black Ginger im Stack ~5 ms höher als ohne. Hypothese — keine Kausalität bewiesen."

### 13.2 Detail-Sheets

Pro Wert (HRV, RHR, Recovery, Sleep): vollständiges Verlaufsdiagramm 30/90/365 Tage, Bestwert, schlechtester Wert, Median, persönlicher Trend.

---

## 14. Spezialansichten (zusätzlich zu den vier Dashboards)

Erreichbar via Bottom-Nav-Long-Press auf Aufgaben-Tab oder via Settings-Quicklinks.

### 14.1 Mein Repertoire (B.4)

- Liste aller bestätigten Insights mit Confidence ≥ 50.
- Sortiert nach: Confidence × Anzahl Wiederholungen × durchschnittliche reduzierte Severity.
- Pro Eintrag eine Glas-Card:
  - Titel
  - Stichpunktliste der Methode (KI-strukturiert)
  - Zielkategorie (Pille)
  - Wirknachweis: „Bestätigt durch X erfolgreiche Wiederholungen, Ø Biomarker-Impact: …"
  - Confidence-Balken
  - Tap → Detail-Sheet mit Edit-Möglichkeit.

### 14.2 Insight Board

- Drei kollabierbare Sektionen:
  - **Bestätigte Methoden** (Confidence ≥ 70, Wiederholungen ≥ 3)
  - **In Beobachtung** (Confidence < 70 oder Wiederholungen < 3)
  - **Verworfen** (mehrfach erfolglos, Confidence ≤ 20)
- Pro Insight: Edit, Re-Bewerten (manuell Confidence ändern), Löschen.
- „Aus Hypothese erstellen"-Button: konvertiert eine abgeschlossene Hypothese explizit in einen Insight.

### 14.3 Experiment-Kalender

- Eigener App-Kalender (nicht Google Calendar), Tagesansicht, Wochenansicht, Monatsansicht.
- Zeigt alle Hypothesen mit Status `AKTIV` und `VORGESCHLAGEN`.
- Pro Tag: kleine farbige Kacheln pro Hypothese.
- Tap auf Kachel → Hypothesen-Detail-Sheet:
  - Titel, Beschreibung, Begründung
  - Status ändern: aktiv / abgebrochen / abgeschlossen
  - Outcome wählen: erfolgreich / teilweise / erfolglos / unklar
  - Texteingabe für Outcome-Notes (per Mic oder Text)
  - Slider „Gefühlte Entropie-Veränderung" (-10 bis +10)
  - „Biomarker-Vergleich anzeigen" — zeigt automatisch die Werte aus dem `biomarkerBefore`-Snapshot vs. dem aktuellen.
  - Bei Status-Änderung auf `ABGESCHLOSSEN` und Outcome `ERFOLGREICH`: Dialog „Soll ich daraus einen Insight für dein Repertoire machen?" → wenn ja, neuer Insight-Eintrag.
- Nachträgliches Anhaken/Enthaken: für jeden Tag eines aktiven Experiments ein Häkchen „Heute umgesetzt?" — Nutzer kann zurück und die Realität korrigieren.

---

## 15. Externe Anbindungen

### 15.1 Groq (Whisper-Transkription)

Siehe §8.2.

### 15.2 Gemini (KI-Verarbeitung)

Siehe §9.

### 15.3 Google Cloud Text-to-Speech (Chirp 3 HD)

```
POST https://texttospeech.googleapis.com/v1/text:synthesize
Header: x-goog-api-key: <tts_key>
Body: {
  "input": { "text": "<text>" },
  "voice": { "languageCode": "de-DE", "name": "<chirp-3-hd-voice-name>" },
  "audioConfig": { "audioEncoding": "MP3" }
}
```

Antwort enthält Base64-MP3 → in temporäre Datei → via ExoPlayer abspielen.

Beim ersten Verbinden: GET `https://texttospeech.googleapis.com/v1/voices?languageCode=de-DE` aufrufen, alle Voices mit `Chirp3-HD` im Namen herausfiltern, in Settings als Auswahlliste anbieten. Falls keine Chirp-3-HD-Stimmen für Deutsch verfügbar sind: fallback auf Voice mit Typ `Studio` oder `Wavenet` für `de-DE`, Hinweis im UI: „Chirp 3 HD aktuell nicht für Deutsch verfügbar — verwende stattdessen <Voice-Name>."

### 15.4 Whoop OAuth + API

OAuth 2.0 Authorization-Code-Flow:

- Client ID, Client Secret, Redirect URI: müssen vom Nutzer in seinem Whoop-Developer-Dashboard registriert werden. In der App in Settings → API-Schlüssel → Whoop ein Eingabefeld für **Client ID** und **Client Secret**, plus Anzeige der **Redirect URI**, die der Nutzer auf der Whoop-Developer-Seite hinterlegen muss. Format: `de.frank.entropyreducer://oauth/whoop/callback`.
- Nach Eingabe: „Mit Whoop verbinden"-Button öffnet Authorization-URL: `https://api.prod.whoop.com/oauth/oauth2/auth?response_type=code&client_id=<id>&redirect_uri=<uri>&scope=<scopes>&state=<random>`.
- Scopes: `read:recovery read:cycles read:sleep read:workout read:profile`.
- Token-Exchange via `https://api.prod.whoop.com/oauth/oauth2/token`.
- Tokens (Access + Refresh) in EncryptedSharedPreferences.
- Auto-Refresh, wenn Access-Token abgelaufen.

API-Endpoints (Basis: `https://api.prod.whoop.com/developer`):
- `GET /v1/cycle` — Cycle-Liste paginiert
- `GET /v1/recovery` — Recovery-Liste
- `GET /v1/activity/sleep` — Sleep-Liste
- `GET /v1/activity/workout` — Workouts
- `GET /v1/user/profile/basic` — Profil

Sync-Strategie:
- Bei App-Start: prüfen, ob letzter Sync > 30 Min. her → wenn ja, neue Daten holen.
- Nightly Background-Sync via WorkManager um 04:30 (außerhalb Schlaffenster für beide Schichten).
- Pro Sync: hole alle Daten seit letztem erfolgreichem Sync.
- Erstelle für jeden eindeutigen Tag (basierend auf `cycle.start`) einen `BiomarkerSnapshot`.

Rate-Limit: Whoop erlaubt ~100 Requests / Minute. Mit Retry-Logik und Exponential Backoff bei 429.

### 15.5 Google Calendar OAuth + API

OAuth-Flow analog zu Whoop, mit Google-Sign-In:
- Scopes: `https://www.googleapis.com/auth/calendar.readonly`
- Tokens in EncryptedSharedPreferences.

Sync-Strategie:
- Bei App-Start: hole alle Ganztagestermine der nächsten 30 Tage und der letzten 30 Tage.
- Nightly Background-Sync 04:30 zusammen mit Whoop.
- Endpoint: `GET https://www.googleapis.com/calendar/v3/calendars/primary/events?timeMin=<start>&timeMax=<end>&singleEvents=true`.

#### Schichtcode-Parser

Für jeden Ganztagestermin den `summary`-String parsen:

```kotlin
fun parseShiftCode(summary: String): ShiftCode {
    val s = summary.trim().uppercase()
    
    // Urlaub
    if (s == "U" || s.startsWith("URLAUB")) return ShiftCode.URLAUB
    
    // Tag- oder Nachtschicht mit optionalem Frei-Marker
    val tagPattern = Regex("""^TAG\s*[1-4](?:\s*([XF]))?$""")
    val nachtPattern = Regex("""^NACHT\s*[1-4](?:\s*([XF]))?$""")
    
    tagPattern.find(s)?.let { match ->
        return if (match.groupValues[1].isNotEmpty()) ShiftCode.FREI 
               else ShiftCode.TAGDIENST
    }
    nachtPattern.find(s)?.let { match ->
        return if (match.groupValues[1].isNotEmpty()) ShiftCode.FREI 
               else ShiftCode.NACHTDIENST
    }
    
    return ShiftCode.UNBEKANNT
}
```

Pro erkannter ShiftCode wird ein `CalendarDay` erzeugt:

| ShiftCode | workWindowStart | workWindowEnd | sleepWindowStart | sleepWindowEnd | availableMinutesEstimate |
|---|---|---|---|---|---|
| TAGDIENST | 04:00 | 18:30 | 21:00 (Vortag) | 04:00 | 30–60 |
| NACHTDIENST | 16:00 | 05:50 (+1d) | 06:30 | 15:00 | 60–90 |
| FREI | – | – | flexibel | flexibel | 480 |
| URLAUB | – | – | flexibel | flexibel | 600 |
| UNBEKANNT | – | – | – | – | 240 (Default) |

Konfigurierbar via Settings → Schichtcode-Muster, falls Frank die Schreibweise ändert.

---

## 16. Hintergrund-Mechanismen

### 16.1 Korrelations-Engine (Supplements ↔ Biomarker)

Eigener Background-Job (WorkManager, täglich 03:30):
1. Hole alle SupplementLogs der letzten 60 Tage.
2. Hole alle BiomarkerSnapshots der letzten 60 Tage.
3. Bilde pro Substanz-Konstellation (vereinfacht: vorhanden vs. weggelassen) den Mittelwert von HRV, Recovery, Sleep Performance.
4. Wenn ein Mittelwert-Unterschied über einer minimalen Effektstärke liegt (Cohen's d > 0.3) UND die Stichprobe groß genug ist (n ≥ 7 in beiden Gruppen): markiere als „Beobachtung".
5. Übergib der KI-Trigger-Engine (§16.2), die daraus einen Trigger oder einen Hinweis-Vorschlag macht.

Wichtig: Diese Engine berechnet ausschließlich Korrelationen und kennzeichnet Ergebnisse als „Beobachtung, keine Kausalität". Die KI darf in ihren Antworten daraus Hypothesen formulieren, niemals Kausalitätsaussagen.

### 16.2 KI-entwickelte Trigger (statt nutzerdefinierte Regeln)

Die KI beobachtet selbständig Muster und schlägt Trigger zur Bestätigung vor:

Zweimal pro Woche (Mittwoch + Sonntag, 11:00 Uhr falls außerhalb Schlaffenster) läuft ein Background-Worker:

1. Hole letzten 30 Tage: alle Einträge, alle Biomarker, alle abgeschlossenen Hypothesen, alle SupplementLogs.
2. Sende an Gemini mit folgendem Prompt:

```text
[Genie-Identität + Profil + Memory]

Du erhältst Frank's Daten der letzten 30 Tage. Dein Auftrag: Identifiziere bis zu 3 robuste Muster, die einen automatischen Trigger rechtfertigen würden. Ein Trigger besteht aus einer Bedingung und einer vorgeschlagenen Aktion.

Antworte AUSSCHLIESSLICH in JSON:
{
  "triggers": [
    {
      "name": "Kurzer Name",
      "condition": "Klartext-Bedingung, z.B. 'HRV unter 35 ms am Morgen'",
      "proposedAction": "Was die App tun soll, z.B. 'Körperliche Aufgaben bekommen heute +20 Priorität, KI-Frage am Morgen: Wie geht es dir körperlich?'",
      "rationale": "Warum dieser Trigger Sinn ergibt, mit Datenbezug",
      "confidence": <Integer 0-100>
    }
  ]
}

Schlage nur Trigger vor, die du auf Basis der Daten gut begründen kannst. Lieber 0 Trigger als ein schwacher.
```

3. Antwort parsen, neue `KiTrigger`-Einträge mit `isActive = false`, `approvedAt = null` anlegen.
4. UI-Hinweis: „Die KI schlägt N neue Trigger vor — ansehen?" (Banner auf Dashboard 1).
5. Nutzer öffnet die Trigger-Liste, kann pro Trigger annehmen / ablehnen / bearbeiten.

Nach Annahme läuft der Trigger im Hintergrund (Polling alle 15 Min.). Bei Match: führt die `proposedAction` aus (z.B. Priority-Boost auf körperliche Einträge, oder erzeugt eine KI-Frage in Dashboard 1).

### 16.3 Wochen- und Monatsrückblick

Background-Worker:
- **Wöchentlich**: Sonntag 19:00 Uhr (außerhalb Schlaffenster — bei Nachtdienst: nach Wachzeit).
- **Monatlich**: am 1. des Folgemonats 19:00 Uhr.

Generiert Markdown-Prosa in zweiter Person (Du) im Stil von Frank's bestehender Entropy-Journal-Wochenrückblicke. Gespeichert als spezielle Insights mit Tag `wochenrueckblick` oder `monatsrueckblick`. Push-Hinweis: „Dein Wochenrückblick ist fertig — anhören?" (mit Chirp-3-HD-Direktstart-Button).

Prompt:

```text
[Genie-Identität + Profil + Memory]

Du erhältst Frank's letzte 7 (oder 30) Tage: alle Einträge, Biomarker-Trend, abgeschlossene Hypothesen, neue Insights. Verfasse einen narrativen Rückblick in deutscher Fließtext-Prosa, in zweiter Person („Du"), warm aber präzise, ohne Floskeln.

Struktur:
1. Was diese Woche / dieser Monat geprägt hat (1 Absatz)
2. Welche Entropie du reduziert hast (1 Absatz)
3. Welche Muster ich beobachtet habe (1 Absatz)
4. Eine Frage für die kommende Woche / den kommenden Monat (1 Satz)

Maximal 350 Wörter (Woche) / 700 Wörter (Monat). Keine Markdown-Header. Kein Emojigebrauch.
```

### 16.4 Schichtdienst-bewusste Benachrichtigungen

Notification-Channel-Logik:
- Vor JEDEM `notify()`-Call prüfen: Liegt die aktuelle Uhrzeit im Schlaffenster für heute (basierend auf CalendarDay)?
- Wenn ja: Notification verzögern bis zum Wachzeitpunkt + 15 Min.
- Ausnahme: Trigger der Kategorie `URGENT` (gibt es aktuell nicht — Platzhalter).
- Bei UNBEKANNT-ShiftCode: Default-Schlaffenster 22:00 – 06:00.

Implementierung via `WorkManager` mit dynamischer `setInitialDelay`.

### 16.5 Genie-Codex-Synthese

Sonntags 19:00 (oder manuell) startet der Codex-Worker. Prompt:

```text
[Genie-Identität + Profil]

Du baust eine kompakte Synthese „Was ich aktuell über Frank verstehe", basierend auf:
- allen aktiven Memory-Einträgen
- allen bestätigten Insights (Confidence ≥ 70)
- den Mustern der letzten 30 Tage
- den offenen Hypothesen

Format: deutsches Markdown, 4 Sektionen:

## Frank's Entropie-Muster
Welche Kategorien dominieren strukturell? Welche Trigger habe ich beobachtet?

## Frank's bewährte Hebel
Welche Methoden funktionieren reproduzierbar?

## Offene Forschungsfragen
Welche Hypothesen laufen gerade, welche Muster sind noch unklar?

## Mein aktuelles mentales Modell
In 2-3 Sätzen: Was ist mein bester Überblick über das System Frank?

Maximum 600 Wörter. Schreib in erster Person aus Sicht des Genies. Direkt, präzise, ohne Schmeichelei.
```

Speichere als `GenieCodexVersion`. Behalte die letzten 10 Versionen.

### 16.6 Confidence-Berechnung im Insight Board

Nach jedem abgeschlossenen Experiment (Hypothese mit Outcome):
1. Suche existierende Insights, die thematisch passen (KI-Aufruf an Gemini: „Passt diese Hypothese zu einem dieser Insights? Wenn ja, welcher?").
2. Bei Match: aktualisiere Insight (`successCount`, `attemptCount`, neuen Confidence-Wert berechnen).
3. Bei kein Match: Falls Outcome `ERFOLGREICH`, lege neuen Insight mit Confidence 40 (eine erfolgreiche Wiederholung) an.

Confidence-Formel:
```
confidence = min(100, 30 + (successCount * 15) + (avgFeltImpact * 2) + (biomarkerImpactBonus))

biomarkerImpactBonus: 0 wenn keine Daten, +10 wenn HRV-Verbesserung > 2ms, +5 wenn Recovery-Verbesserung > 5%
```

Bei mehrfachen erfolglosen Versuchen (3+ Outcome `ERFOLGLOS`): Confidence sinkt auf max. 20, Insight wandert in „Verworfen".

---

## 17. Share-Sheet-Integration (B.6)

Manifest:
```xml
<intent-filter>
  <action android:name="android.intent.action.SEND" />
  <category android:name="android.intent.category.DEFAULT" />
  <data android:mimeType="text/plain" />
</intent-filter>
```

Implementierung:
- Eigene Activity `ShareReceiverActivity`, transparent, ohne UI.
- Empfängt Text via Intent.
- Erzeugt sofort einen `EntropyEntry` mit `source = SHARE_SHEET`, `rawTranscript = <text>`.
- Triggert KI-Verarbeitung im Hintergrund.
- Zeigt Toast: „Eintrag gespeichert — wird verarbeitet."
- Schließt sich.

---

## 18. Home-Screen-Widget (B.7)

Glance Widget (Jetpack Glance), 4×2 Größe (resizable von 2×2 bis 4×3):
- Oben: Status-Balken (kompakt) + Prozentwert.
- Mitte: Top-1-Aufgabe von heute (Titel + Kategorie-Pille).
- Unten: großer Mic-Button (Tap → öffnet Aufnahme-Dialog der App via Intent).

Update-Frequenz: alle 15 Min. + bei jedem App-Datenänderungs-Event via Glance-Refresh.

Bei Samsung-Geräten (Frank hat Fold 6): zusätzlich in `AndroidManifest.xml` Battery-Optimization-Whitelist anregen — kleiner Hinweis-Dialog beim Setup, dass die App in Samsungs Geräte-Pflege als „Nie schlafen lassen" markiert werden sollte.

---

## 19. Fehlerbehandlung & UX-Details

- **Kein API-Key gesetzt:** Beim Versuch zu sprechen oder zu analysieren erscheint ein Sheet: „Du hast noch keinen <Dienst>-Key hinterlegt — gehe zu Einstellungen → API-Schlüssel." Mit Direkt-Button.
- **Offline:** Aufnahme wird gespeichert mit Status „Wartet auf Verarbeitung". WorkManager retried bei Online-Zustand.
- **API-Fehler:** Klare deutsche Fehlermeldung, niemals roher englischer Stack-Trace. Beispiel: „Groq-Server hat zu lange gebraucht — versuche es gleich nochmal."
- **Lange Transkripte:** Limit 5000 Zeichen vor Gemini-Call; bei längeren warnen, eventuell Splittung anbieten.
- **Whoop nicht verbunden:** Status-Balken nutzt nur Aufgaben-Reduktion (Gewicht 100 %), Dashboard 4 zeigt Empty-State mit „Verbinde dein Whoop-Armband, um deine Biomarker einzubeziehen".
- **Calendar nicht verbunden:** Zeit-Buckets fallen auf Default-Logik zurück (kein Schichtdienst-Bezug), KI-Frage-Card weist auf den fehlenden Calendar-Sync hin.
- **Sehr viele Einträge:** Pagination ab 100 Einträgen pro Bucket.
- **Animation:** Listen-Einträge erscheinen mit slide-in + fade. Status-Wechsel `REDUZIERT` zeigt kurz eine Mintgrün-Welle über der Card.
- **Audio-Permission verweigert:** Erklär-Sheet mit Direkt-Link in System-Settings.

---

## 20. Sicherheit

- Alle API-Keys + OAuth-Tokens ausschließlich in `EncryptedSharedPreferences` (AES256_GCM mit MasterKey).
- Niemals in Logs, niemals in Crash-Reports.
- Audio-Dateien nach erfolgreicher Transkription sofort löschen.
- Network-Security-Config: Cleartext-Traffic verbieten, nur HTTPS.
- Keine Telemetrie, keine Analytics, keine Crashlytics.
- ProGuard/R8: Release-Build minified, aber API-Modelle vom Shrinking ausnehmen (Kotlinx Serialization).
- Backup-Rules: `android:allowBackup="false"`, da verschlüsselte Keys nicht in Cloud-Backup gehören.

---

## 21. Lieferumfang / Acceptance-Kriterien

Die App ist erst „fertig", wenn ALLE folgenden Punkte erfüllt sind:

**Stufe 1 (MVP):**
- [ ] App startet ohne API-Keys und führt durch Setup.
- [ ] Settings: alle 7 Sektionen vollständig zugänglich (auch wenn manche noch keine Daten haben).
- [ ] Spracheingabe funktioniert end-to-end (Mic → Whisper → Gemini → Eintrag in Liste).
- [ ] Memory-System: Hinzufügen, Bearbeiten, Aktiv-Toggle, Löschen.
- [ ] Insight Board: Grundgerüst sichtbar (auch wenn leer).
- [ ] Status-Balken zeigt Aufgaben-Reduktion-Komponente korrekt.

**Stufe 2:**
- [ ] Google Calendar verbunden, Schichtcodes geparst, Zeit-Buckets korrekt.
- [ ] Whoop verbunden, Biomarker-Snapshots in Datenbank.
- [ ] Dashboard 4 zeigt aktuelle und historische Werte.
- [ ] Status-Balken voll mit allen drei Komponenten.
- [ ] KI-Frage-Card auf Dashboard 1 erscheint kontextabhängig.

**Stufe 3:**
- [ ] Dashboard 2 produziert Markdown-Analyse.
- [ ] Dashboard 3 läuft als Dialog, Hypothesen-Karten werden korrekt geparst und angezeigt.
- [ ] Mehrere Sessions in Dashboard 3 sind getrennt persistent.
- [ ] Experiment-Kalender funktioniert, nachträgliches Korrigieren möglich.
- [ ] Genie-Codex generiert sich.
- [ ] Mein Repertoire zeigt Insights nach Wichtigkeit sortiert.

**Stufe 4:**
- [ ] TTS-Briefing funktioniert mit Chirp 3 HD.
- [ ] Home-Screen-Widget funktioniert.
- [ ] Korrelations-Engine läuft täglich.
- [ ] KI-Trigger werden zweimal pro Woche vorgeschlagen.
- [ ] Wochen- und Monatsrückblick generiert sich automatisch.
- [ ] Share-Sheet-Integration funktioniert.

**Übergreifend:**
- [ ] Alle Daten lokal in Room, keine Cloud außer den genannten APIs.
- [ ] App überlebt Configuration Changes ohne Datenverlust.
- [ ] Alle Edge-Cases mit klaren deutschen Meldungen.
- [ ] README mit Setup-Anleitung in maximal 12 Schritten.
- [ ] DECISIONS.md dokumentiert alle nicht-trivialen Entscheidungen.

---

## 22. Stufenweise Umsetzung (verbindliche Reihenfolge)

### Stufe 1 — Fundament + MVP (Lauffähigkeit)

1. Projekt anlegen mit allen Dependencies (siehe §2).
2. Package-Struktur:
   ```
   de.frank.entropyreducer
   ├── data
   │   ├── local (Room-Database, DAOs, Entities, TypeConverters)
   │   ├── remote (Retrofit-APIs, DTOs für Groq, Gemini, Whoop, Google Calendar, TTS)
   │   ├── repository (Implementierungen)
   │   └── settings (DataStore, EncryptedPrefs)
   ├── domain
   │   ├── model
   │   ├── repository (Interfaces)
   │   └── usecase
   ├── presentation
   │   ├── theme (NeonCosmos)
   │   ├── components (GlassCard, MicButton, StatusBar, Pillen, Charts)
   │   ├── dashboard1 (TasksScreen + ViewModel)
   │   ├── dashboard2 (AnalysisScreen + ViewModel)
   │   ├── dashboard3 (ScientistScreen + ViewModel)
   │   ├── dashboard4 (BiomarkerScreen + ViewModel)
   │   ├── settings (alle 7 Sub-Screens)
   │   ├── repertoire (RepertoireScreen)
   │   ├── insightboard (InsightBoardScreen)
   │   ├── experimentcalendar (ExperimentCalendarScreen)
   │   ├── shared (Share-Receiver-Activity)
   │   ├── widget (Glance-Widget)
   │   └── navigation (Routen, NavGraph)
   ├── workers (WorkManager-Worker)
   └── di (Hilt-Module)
   ```
3. Theme „Neon Cosmos" voll implementieren.
4. Datenmodell + Room-Database + alle DAOs (auch für Stufen 2-4 schon, leer).
5. Settings-Screen voll: alle 7 Sektionen, API-Key-Eingaben, Memory-CRUD, Profil, Prompts (mit den 3 Default-Prompts vorinstalliert), Genie-Codex (leer), Datenexport.
6. Mic-Button-Component voll mit Animationen.
7. Aufnahme + Whisper-Pipeline.
8. Gemini-Eintrags-Verarbeitung mit System-Prompt-Architektur.
9. Dashboard 1 (Aufgaben) — Grundversion: Liste, Detail-Sheet, Mic-Button, KEINE Zeit-Buckets-Logik (alle Einträge in „HEUTE"), KEINE KI-Frage-Card noch.
10. Status-Balken — nur Aufgaben-Reduktion-Komponente.
11. Insight Board und Repertoire — Grundgerüste, leer.
12. Bottom-Navigation mit 4 Tabs (Dashboards 2-4 zeigen Empty-State / Coming Soon).
13. **Commit „Stufe 1 — MVP lauffähig".**

### Stufe 2 — Kalender + Biomarker

1. Google Calendar OAuth + API + Sync-Worker.
2. Schichtcode-Parser mit Tests.
3. CalendarDay-Cache + Logik für Zeit-Buckets in Dashboard 1.
4. Whoop OAuth + API + Sync-Worker.
5. BiomarkerSnapshot-Erstellung pro Sync.
6. Dashboard 4 voll implementieren (Recovery-Ring, HRV-Chart, Schlafstadien-Bar, Strain).
7. Status-Balken-Komponenten Biomarker + Kontext.
8. KI-Frage-Card auf Dashboard 1 mit allen Triggern aus §10.4.
9. Schichtdienst-bewusste Notification-Logik (§16.4).
10. **Commit „Stufe 2 — Kontext angeschlossen".**

### Stufe 3 — Wissenschaftler + Hypothesen-Loop

1. Dashboard 2 voll mit Trend-Chart und Markdown-Analyse.
2. Dashboard 3 voll mit Chat-UI, Sessions, Hypothesen-Parser.
3. Hypothesen-Karten in Chat-Bubbles.
4. Experiment-Kalender voll mit Tag-/Wochen-/Monatsansicht und nachträglichem Korrigieren.
5. Confidence-Berechnung (§16.6) und Insight-Board-Pflege.
6. Repertoire-Sortierung nach Wichtigkeit.
7. Genie-Codex-Synthese (manuell + automatisch sonntags).
8. **Commit „Stufe 3 — Forschungsschleife geschlossen".**

### Stufe 4 — Politur + lernende Mechanismen

1. Google Cloud TTS Chirp 3 HD-Integration (Voice-Listing, Synthese, ExoPlayer-Wiedergabe).
2. Tagesbriefing automatisch + manuell, mit Audio.
3. Wochen- und Monatsrückblick (§16.3).
4. Korrelations-Engine (§16.1).
5. KI-Trigger-Engine (§16.2) inklusive Approval-Flow.
6. Share-Sheet-Receiver (§17).
7. Home-Screen-Widget mit Glance (§18).
8. Politur: Edge-Cases, Empty-States, Fehlermeldungen, Animationen, Haptik durchgehend prüfen.
9. README final, DECISIONS.md final.
10. **Commit „Stufe 4 — Vollausbau".**

---

## 23. Erste Schritte (mach das jetzt)

1. Lege das Projekt an: `:app` mit allen Dependencies aus §2, Version Catalog `libs.versions.toml`.
2. Erstelle die Package-Struktur aus §22 Stufe 1, leer.
3. Implementiere das Theme „Neon Cosmos" (§3).
4. Implementiere das Datenmodell + Room (§5).
5. Implementiere DI-Module (Hilt).
6. Baue Settings (§6) — fang mit der Skelett-Navigation an, dann Sektion für Sektion.
7. Mic-Button (§4.2 + §8.1) als wiederverwendbare Composable.
8. Voice-Pipeline (§8.2 + §9).
9. Dashboard 1 Grundversion (§10 ohne 10.4).
10. Status-Balken Grundversion.
11. Bottom-Nav.
12. Stufe-1-Commit.
13. Stufe 2 (§22) starten.

Schreibe ein `DECISIONS.md`, in dem du jede nicht-triviale Designentscheidung dokumentierst. Beispiele: gewählte Voice-Pipeline-Library, Audioformat-Kompromisse, Hilt-Modul-Granularität, Strategien bei API-Limits, Schichtcode-Parser-Spezialfälle.

Schreibe ein `README.md` mit:
- Kurzer Beschreibung der App
- Setup-Schritten (API-Keys besorgen: Groq, Gemini, Google Cloud TTS, Whoop Developer App registrieren, Google Calendar)
- Build-Anleitung
- Bekannten Einschränkungen (z.B. „Bei Samsung One UI bitte App in Geräte-Pflege als ‚Nicht schlafen lassen' markieren")

**Fang an. Bau Stufe 1 vollständig durch, dann Stufe 2, dann Stufe 3, dann Stufe 4. Frag nicht, ob du anfangen sollst — fang an.**
